package dev.kehlchen.lib.core.database;

import dev.kehlchen.lib.api.database.Database;
import dev.kehlchen.lib.api.database.DatabaseConfig;
import dev.kehlchen.lib.api.database.DatabaseFactory;

public final class JdbcDatabaseFactory implements DatabaseFactory {
    @Override public Database create(DatabaseConfig config) { return new JdbcDatabase(config); }
}
