package dev.kehlchen.lib.api.database;

public interface DatabaseMigration {
    int version();
    String description();
    void migrate(Transaction transaction) throws Exception;
}
