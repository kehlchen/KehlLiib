package dev.kehlchen.lib.api.database;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

public final class DatabaseConfig {
    private final DatabaseType type;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final Path sqliteFile;
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final Duration connectionTimeout;
    private final Duration idleTimeout;
    private final Duration maximumLifetime;
    private final Duration leakDetectionThreshold;
    private final Duration queryTimeout;
    private final Properties properties;

    private DatabaseConfig(Builder builder) {
        type = Objects.requireNonNull(builder.type, "type");
        jdbcUrl = builder.jdbcUrl;
        username = builder.username;
        password = builder.password;
        sqliteFile = builder.sqliteFile;
        maximumPoolSize = builder.maximumPoolSize;
        minimumIdle = builder.minimumIdle;
        connectionTimeout = builder.connectionTimeout;
        idleTimeout = builder.idleTimeout;
        maximumLifetime = builder.maximumLifetime;
        leakDetectionThreshold = builder.leakDetectionThreshold;
        queryTimeout = builder.queryTimeout;
        properties = new Properties();
        properties.putAll(builder.properties);
        if (maximumPoolSize < 1 || minimumIdle < 0 || minimumIdle > maximumPoolSize) {
            throw new IllegalArgumentException("Invalid connection pool size");
        }
        if (type == DatabaseType.SQLITE && sqliteFile == null && jdbcUrl == null) {
            throw new IllegalArgumentException("SQLite requires a file or JDBC URL");
        }
        if (type != DatabaseType.SQLITE && (jdbcUrl == null || jdbcUrl.isBlank())) {
            throw new IllegalArgumentException("A JDBC URL is required for " + type);
        }
    }

    public static Builder builder(DatabaseType type) { return new Builder(type); }
    public DatabaseType type() { return type; }
    public String jdbcUrl() { return jdbcUrl; }
    public String username() { return username; }
    public String password() { return password; }
    public Path sqliteFile() { return sqliteFile; }
    public int maximumPoolSize() { return maximumPoolSize; }
    public int minimumIdle() { return minimumIdle; }
    public Duration connectionTimeout() { return connectionTimeout; }
    public Duration idleTimeout() { return idleTimeout; }
    public Duration maximumLifetime() { return maximumLifetime; }
    public Duration leakDetectionThreshold() { return leakDetectionThreshold; }
    public Duration queryTimeout() { return queryTimeout; }
    public Properties properties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    public static final class Builder {
        private final DatabaseType type;
        private String jdbcUrl;
        private String username;
        private String password;
        private Path sqliteFile;
        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maximumLifetime = Duration.ofMinutes(30);
        private Duration leakDetectionThreshold = Duration.ZERO;
        private Duration queryTimeout = Duration.ofSeconds(30);
        private final Properties properties = new Properties();

        private Builder(DatabaseType type) { this.type = Objects.requireNonNull(type, "type"); }
        public Builder jdbcUrl(String value) { jdbcUrl = value; return this; }
        public Builder credentials(String user, String secret) { username = user; password = secret; return this; }
        public Builder file(Path value) { sqliteFile = value; return this; }
        public Builder maximumPoolSize(int value) { maximumPoolSize = value; return this; }
        public Builder minimumIdle(int value) { minimumIdle = value; return this; }
        public Builder connectionTimeout(Duration value) { connectionTimeout = positive(value, "connectionTimeout"); return this; }
        public Builder idleTimeout(Duration value) { idleTimeout = nonNegative(value, "idleTimeout"); return this; }
        public Builder maximumLifetime(Duration value) { maximumLifetime = positive(value, "maximumLifetime"); return this; }
        public Builder leakDetectionThreshold(Duration value) { leakDetectionThreshold = nonNegative(value, "leakDetectionThreshold"); return this; }
        public Builder queryTimeout(Duration value) { queryTimeout = positive(value, "queryTimeout"); return this; }
        public Builder property(String name, String value) { properties.setProperty(name, value); return this; }
        public DatabaseConfig build() { return new DatabaseConfig(this); }
        private static Duration positive(Duration value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isZero() || value.isNegative()) throw new IllegalArgumentException(name);
            return value;
        }
        private static Duration nonNegative(Duration value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isNegative()) throw new IllegalArgumentException(name);
            return value;
        }
    }
}
