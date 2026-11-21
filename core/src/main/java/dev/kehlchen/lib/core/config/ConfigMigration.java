package dev.kehlchen.lib.core.config;

@FunctionalInterface
public interface ConfigMigration {
    void migrate(int fromVersion, int toVersion, Config config);
}
