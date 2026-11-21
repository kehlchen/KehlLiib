package dev.kehlchen.lib.core.config;

import java.util.NavigableMap;
import java.util.TreeMap;

public final class ConfigMigrator {
    private final NavigableMap<Integer, ConfigMigration> migrations = new TreeMap<>();
    public ConfigMigrator register(int fromVersion, ConfigMigration migration) {
        migrations.put(fromVersion, migration);
        return this;
    }
    public void migrate(Config config, int fromVersion, int toVersion) {
        for (int version = fromVersion; version < toVersion; version++) {
            ConfigMigration migration = migrations.get(version);
            if (migration != null) migration.migrate(version, version + 1, config);
        }
        config.save();
    }
}
