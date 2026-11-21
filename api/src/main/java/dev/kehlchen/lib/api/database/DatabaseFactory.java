package dev.kehlchen.lib.api.database;

@FunctionalInterface
public interface DatabaseFactory {
    Database create(DatabaseConfig config);
}
