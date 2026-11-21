package dev.kehlchen.lib.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import dev.kehlchen.lib.api.database.Database;
import dev.kehlchen.lib.api.database.DatabaseConfig;
import dev.kehlchen.lib.api.database.DatabaseStats;
import dev.kehlchen.lib.api.database.DatabaseType;
import dev.kehlchen.lib.api.database.RowMapper;
import dev.kehlchen.lib.api.database.Transaction;
import dev.kehlchen.lib.api.database.TransactionWork;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** HikariCP-backed JDBC database with resource-safe query helpers. */
public final class JdbcDatabase implements Database {
    private static final Logger LOGGER = Logger.getLogger(JdbcDatabase.class.getName());
    private final HikariDataSource dataSource;
    private final ExecutorService asyncExecutor;
    private final int queryTimeoutSeconds;
    private final AtomicLong queries = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong totalQueryTime = new AtomicLong();
    private final Set<CompletableFuture<?>> pendingAsync = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public JdbcDatabase(DatabaseConfig config) {
        Objects.requireNonNull(config, "config");
        String jdbcUrl = jdbcUrl(config);
        HikariConfig pool = new HikariConfig();
        pool.setPoolName("KehlLib-" + config.type().name().toLowerCase());
        pool.setJdbcUrl(jdbcUrl);
        pool.setDriverClassName(driver(config.type()));
        pool.setMaximumPoolSize(config.type() == DatabaseType.SQLITE ? 1 : config.maximumPoolSize());
        pool.setMinimumIdle(config.type() == DatabaseType.SQLITE ? 1 : config.minimumIdle());
        pool.setConnectionTimeout(config.connectionTimeout().toMillis());
        if (config.type() != DatabaseType.SQLITE) {
            pool.setIdleTimeout(config.idleTimeout().toMillis());
            pool.setMaxLifetime(config.maximumLifetime().toMillis());
            if (!config.leakDetectionThreshold().isZero()) {
                pool.setLeakDetectionThreshold(config.leakDetectionThreshold().toMillis());
            }
        } else {
            pool.setIdleTimeout(0);
            pool.setMaxLifetime(0);
            pool.setConnectionInitSql("PRAGMA foreign_keys=ON");
        }
        if (config.username() != null) pool.setUsername(config.username());
        if (config.password() != null) pool.setPassword(config.password());
        pool.setDataSourceProperties(config.properties());
        dataSource = new HikariDataSource(pool);
        queryTimeoutSeconds = Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                (config.queryTimeout().toMillis() + 999) / 1000));
        asyncExecutor = Executors.newFixedThreadPool(config.type() == DatabaseType.SQLITE
                ? 1 : config.maximumPoolSize(), runnable -> {
            Thread thread = new Thread(runnable, "kehllib-database");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public int update(String sql, Object... parameters) {
        return measured("update", () -> {
            try (Connection connection = connection();
                 PreparedStatement statement = prepare(connection, sql, parameters)) {
                return statement.executeUpdate();
            } catch (SQLException failure) {
                throw databaseError("update", sql, failure);
            }
        });
    }

    @Override
    public long insert(String sql, Object... parameters) {
        return measured("insert", () -> {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, parameters);
                statement.setQueryTimeout(queryTimeoutSeconds);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : -1L;
                }
            } catch (SQLException failure) {
                throw databaseError("insert", sql, failure);
            }
        });
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
        Objects.requireNonNull(mapper, "mapper");
        return measured("query", () -> {
            try (Connection connection = connection();
                 PreparedStatement statement = prepare(connection, sql, parameters);
                 ResultSet results = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (results.next()) rows.add(mapper.map(results));
                return List.copyOf(rows);
            } catch (SQLException failure) {
                throw databaseError("query", sql, failure);
            }
        });
    }

    @Override
    public int[] batch(String sql, List<Object[]> parameterSets) {
        Objects.requireNonNull(parameterSets, "parameterSets");
        return measured("batch", () -> {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setQueryTimeout(queryTimeoutSeconds);
                for (Object[] parameters : parameterSets) {
                    bind(statement, parameters);
                    statement.addBatch();
                }
                return statement.executeBatch();
            } catch (SQLException failure) {
                throw databaseError("batch", sql, failure);
            }
        });
    }

    @Override
    public <T> T transaction(TransactionWork<T> work) {
        Objects.requireNonNull(work, "work");
        ensureOpen();
        long start = System.nanoTime();
        queries.incrementAndGet();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(new JdbcTransaction(connection));
                connection.commit();
                return result;
            } catch (Throwable failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                failures.incrementAndGet();
                if (failure instanceof Error error) throw error;
                throw new IllegalStateException("Database transaction failed", failure);
            }
        } catch (SQLException failure) {
            failures.incrementAndGet();
            throw databaseError("transaction", "<transaction>", failure);
        } finally {
            long elapsed = System.nanoTime() - start;
            totalQueryTime.addAndGet(elapsed);
            LOGGER.fine(() -> "Database transaction completed in " + TimeUnit.NANOSECONDS.toMillis(elapsed) + " ms");
        }
    }

    @Override
    public CompletableFuture<Integer> updateAsync(String sql, Object... parameters) {
        Object[] copy = parameters.clone();
        return submitAsync(() -> update(sql, copy));
    }

    @Override
    public CompletableFuture<Long> insertAsync(String sql, Object... parameters) {
        Object[] copy = parameters.clone();
        return submitAsync(() -> insert(sql, copy));
    }

    @Override
    public <T> CompletableFuture<List<T>> queryAsync(String sql, RowMapper<T> mapper, Object... parameters) {
        Object[] copy = parameters.clone();
        return submitAsync(() -> query(sql, mapper, copy));
    }

    @Override
    public CompletableFuture<int[]> batchAsync(String sql, List<Object[]> parameterSets) {
        List<Object[]> copy = parameterSets.stream().map(Object[]::clone).toList();
        return submitAsync(() -> batch(sql, copy));
    }

    @Override
    public <T> CompletableFuture<T> transactionAsync(TransactionWork<T> work) {
        return submitAsync(() -> transaction(work));
    }

    @Override
    public boolean isHealthy() {
        if (closed) return false;
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (SQLException failure) {
            return false;
        }
    }

    @Override
    public DatabaseStats statistics() {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        return pool == null
                ? new DatabaseStats(0, 0, 0, 0, queries.get(), failures.get(), totalQueryTime.get())
                : new DatabaseStats(pool.getActiveConnections(), pool.getIdleConnections(),
                        pool.getTotalConnections(), pool.getThreadsAwaitingConnection(),
                        queries.get(), failures.get(), totalQueryTime.get());
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
                failPendingAsync("Database closed before async operation completed");
            }
        } catch (InterruptedException interrupted) {
            asyncExecutor.shutdownNow();
            failPendingAsync("Database shutdown was interrupted");
            Thread.currentThread().interrupt();
        } finally {
            closed = true;
            dataSource.close();
        }
    }

    private Connection connection() throws SQLException {
        ensureOpen();
        return dataSource.getConnection();
    }

    private PreparedStatement prepare(Connection connection, String sql, Object[] parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        bind(statement, parameters);
        statement.setQueryTimeout(queryTimeoutSeconds);
        return statement;
    }

    private static void bind(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }

    private <T> T measured(String operation, SqlSupplier<T> action) {
        ensureOpen();
        long start = System.nanoTime();
        queries.incrementAndGet();
        try {
            return action.get();
        } catch (RuntimeException failure) {
            failures.incrementAndGet();
            throw failure;
        } finally {
            long elapsed = System.nanoTime() - start;
            totalQueryTime.addAndGet(elapsed);
            LOGGER.fine(() -> "Database " + operation + " completed in "
                    + TimeUnit.NANOSECONDS.toMillis(elapsed) + " ms");
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Database is closed");
    }

    private <T> CompletableFuture<T> submitAsync(Supplier<T> operation) {
        ensureOpen();
        CompletableFuture<T> result = new CompletableFuture<>();
        pendingAsync.add(result);
        try {
            asyncExecutor.execute(() -> {
                try {
                    result.complete(operation.get());
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                } finally {
                    pendingAsync.remove(result);
                }
            });
        } catch (RejectedExecutionException failure) {
            pendingAsync.remove(result);
            result.completeExceptionally(new IllegalStateException("Database is shutting down", failure));
        }
        return result;
    }

    private void failPendingAsync(String message) {
        IllegalStateException failure = new IllegalStateException(message);
        pendingAsync.forEach(future -> future.completeExceptionally(failure));
        pendingAsync.clear();
    }

    private static IllegalStateException databaseError(String operation, String sql, Exception cause) {
        return new IllegalStateException("Database " + operation + " failed for SQL: " + sql, cause);
    }

    private static String jdbcUrl(DatabaseConfig config) {
        if (config.jdbcUrl() != null) {
            return config.type() == DatabaseType.SQLITE
                    ? sqliteBusyTimeout(config.jdbcUrl()) : config.jdbcUrl();
        }
        try {
            java.nio.file.Path file = config.sqliteFile().toAbsolutePath();
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            return sqliteBusyTimeout("jdbc:sqlite:" + file);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Could not create SQLite database directory", failure);
        }
    }

    private static String sqliteBusyTimeout(String jdbcUrl) {
        if (jdbcUrl.contains("busy_timeout=")) return jdbcUrl;
        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "busy_timeout=5000";
    }

    private static String driver(DatabaseType type) {
        return switch (type) {
            case SQLITE -> "org.sqlite.JDBC";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case POSTGRESQL -> "org.postgresql.Driver";
        };
    }

    @FunctionalInterface
    private interface SqlSupplier<T> { T get(); }

    private final class JdbcTransaction implements Transaction {
        private final Connection connection;
        private JdbcTransaction(Connection connection) { this.connection = connection; }

        @Override public int update(String sql, Object... parameters) {
            try (PreparedStatement statement = prepare(connection, sql, parameters)) {
                return statement.executeUpdate();
            } catch (SQLException failure) {
                throw databaseError("transaction update", sql, failure);
            }
        }

        @Override public long insert(String sql, Object... parameters) {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, parameters);
                statement.setQueryTimeout(queryTimeoutSeconds);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : -1L;
                }
            } catch (SQLException failure) {
                throw databaseError("transaction insert", sql, failure);
            }
        }

        @Override public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
            try (PreparedStatement statement = prepare(connection, sql, parameters);
                 ResultSet results = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (results.next()) rows.add(mapper.map(results));
                return List.copyOf(rows);
            } catch (SQLException failure) {
                throw databaseError("transaction query", sql, failure);
            }
        }
    }
}
