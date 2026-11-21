package dev.kehlchen.lib.core.database;

import dev.kehlchen.lib.api.database.Database;
import dev.kehlchen.lib.api.database.DatabaseMigration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Applies ordered, transactional schema migrations once per database. */
public final class DatabaseMigrator {
    private final Database database;

    public DatabaseMigrator(Database database) {
        this.database = java.util.Objects.requireNonNull(database, "database");
    }

    public List<Integer> migrate(Collection<? extends DatabaseMigration> migrations) {
        List<DatabaseMigration> ordered = new ArrayList<>(migrations);
        ordered.sort(Comparator.comparingInt(DatabaseMigration::version));
        Set<Integer> versions = new HashSet<>();
        for (DatabaseMigration migration : ordered) {
            if (migration.version() < 1 || !versions.add(migration.version())) {
                throw new IllegalArgumentException("Migration versions must be unique positive integers");
            }
        }

        database.update("CREATE TABLE IF NOT EXISTS kehllib_schema_migrations "
                + "(version INTEGER PRIMARY KEY, description VARCHAR(255) NOT NULL, "
                + "applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        int currentVersion = database.queryOne(
                "SELECT version FROM kehllib_schema_migrations ORDER BY version DESC LIMIT 1",
                result -> result.getInt(1)).orElse(0);
        List<Integer> applied = new ArrayList<>();
        for (DatabaseMigration migration : ordered) {
            if (migration.version() <= currentVersion) continue;
            database.transaction(transaction -> {
                migration.migrate(transaction);
                transaction.update("INSERT INTO kehllib_schema_migrations (version, description) VALUES (?, ?)",
                        migration.version(), migration.description());
                return null;
            });
            applied.add(migration.version());
            currentVersion = migration.version();
        }
        return List.copyOf(applied);
    }
}
