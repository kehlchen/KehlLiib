package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.cache.Cache;
import dev.kehlchen.lib.api.cache.CacheConfig;
import dev.kehlchen.lib.api.cache.EvictionPolicy;
import dev.kehlchen.lib.api.database.Database;
import dev.kehlchen.lib.api.database.DatabaseConfig;
import dev.kehlchen.lib.api.database.DatabaseMigration;
import dev.kehlchen.lib.api.database.DatabaseType;
import dev.kehlchen.lib.api.player.PlayerProfile;
import dev.kehlchen.lib.api.player.PersistentPlayerData;
import dev.kehlchen.lib.api.repository.PageRequest;
import dev.kehlchen.lib.api.repository.Repository;
import dev.kehlchen.lib.api.storage.Storage;
import dev.kehlchen.lib.api.storage.StorageSerializer;
import dev.kehlchen.lib.core.cache.ConcurrentCache;
import dev.kehlchen.lib.core.database.DatabaseMigrator;
import dev.kehlchen.lib.core.database.JdbcDatabase;
import dev.kehlchen.lib.core.repository.StorageRepository;
import dev.kehlchen.lib.core.storage.DefaultStorage;
import dev.kehlchen.lib.core.storage.StorageProviders;
import dev.kehlchen.lib.core.storage.StorageSerializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Phase5Test {
    @TempDir Path temp;

    @Test
    void sqliteSupportsPreparedQueriesBatchesTransactionsAndMigrations() {
        Database database = new JdbcDatabase(DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(temp.resolve("test.db")).build());
        try (database) {
            database.update("CREATE TABLE sample (id INTEGER PRIMARY KEY AUTOINCREMENT, value TEXT NOT NULL)");
            database.insert("INSERT INTO sample (value) VALUES (?)", "one");
            database.batch("INSERT INTO sample (value) VALUES (?)",
                    List.of(new Object[]{"two"}, new Object[]{"three"}));
            assertEquals(List.of("one", "two", "three"),
                    database.query("SELECT value FROM sample ORDER BY id",
                            result -> result.getString(1)));
            assertTrue(database.queryAsync("SELECT value FROM sample WHERE value=?",
                    result -> result.getString(1), "two").join().contains("two"));

            assertThrows(IllegalStateException.class, () -> database.transaction(transaction -> {
                transaction.update("INSERT INTO sample (value) VALUES (?)", "rolled-back");
                throw new Exception("force rollback");
            }));
            assertEquals(3L, database.queryOne("SELECT COUNT(*) FROM sample", result -> result.getLong(1)).orElseThrow());

            DatabaseMigration first = migration(1, "create migration table",
                    transaction -> {
                        transaction.update("CREATE TABLE migrated (id INTEGER PRIMARY KEY)");
                        return null;
                    });
            DatabaseMigration second = migration(2, "seed migration table",
                    transaction -> {
                        transaction.update("INSERT INTO migrated (id) VALUES (1)");
                        return null;
                    });
            DatabaseMigrator migrator = new DatabaseMigrator(database);
            assertEquals(List.of(1, 2), migrator.migrate(List.of(second, first)));
            assertEquals(List.of(), migrator.migrate(List.of(first, second)));
            assertTrue(database.isHealthy());
            assertTrue(database.statistics().queriesExecuted() > 0);

            UUID uuid = UUID.randomUUID();
            Storage<UUID, PlayerProfile> sqlStorage = new DefaultStorage<>(
                    StorageProviders.sqlite(database, "player_profiles",
                            StorageSerializers.json(UUID.class),
                            StorageSerializers.json(PlayerProfile.class)));
            try (sqlStorage) {
                PlayerProfile profile = new PlayerProfile(uuid, "Steve");
                sqlStorage.save(uuid, profile);
                assertEquals(profile, sqlStorage.load(uuid).orElseThrow());
                assertTrue(sqlStorage.delete(uuid));
            }
        }
    }

    @Test
    void fileAndMemoryStorageWorkWithRepositories() {
        StorageSerializer<String> keys = StorageSerializers.strings();
        Storage<String, String> files = new DefaultStorage<>(
                StorageProviders.json(temp.resolve("records"), keys, StorageSerializers.json(String.class)));
        try (files) {
            files.save("first", "value");
            assertEquals("value", files.load("first").orElseThrow());
            assertEquals(java.util.Set.of("first"), files.keys());
            files.saveAsync("second", "other").join();
            assertTrue(files.exists("second"));
            assertTrue(files.delete("second"));
        }

        for (Storage<String, String> textStorage : List.of(
                new DefaultStorage<>(StorageProviders.yaml(temp.resolve("yaml"), keys, keys)),
                new DefaultStorage<>(StorageProviders.properties(temp.resolve("properties"), keys, keys)))) {
            try (textStorage) {
                textStorage.save("key", "text value");
                assertEquals("text value", textStorage.load("key").orElseThrow());
            }
        }

        Storage<String, String> memory = new DefaultStorage<>(StorageProviders.memory());
        try (memory) {
            Repository<String, String> repository = new StorageRepository<>(memory);
            repository.saveAll(Map.of("b", "Beta", "a", "Alpha")).join();
            assertEquals(2, repository.count().join());
            assertEquals("ALPHA", repository.update("a", value -> value.toUpperCase()).join().orElseThrow());
            assertEquals(Map.of("a", "ALPHA"), memory.loadAll(List.of("a", "missing")));
            assertEquals(List.of("Beta"), repository.findAll(
                    value -> value.startsWith("B"), String::compareTo, PageRequest.of(0, 1))
                    .join().content());
            assertTrue(repository.deleteById("a").join());
            assertFalse(repository.existsById("a").join());
        }
    }

    @Test
    void playerDataRoundTripsInstantAndPreferenceFields() {
        UUID uuid = UUID.randomUUID();
        PersistentPlayerData data = PersistentPlayerData.initial(uuid, "Steve", true)
                .withPreference("theme", "dark")
                .withCustomData("rank", "builder")
                .finishSession(Duration.ofMinutes(3), java.time.Instant.parse("2026-01-02T03:04:05Z"));
        assertEquals(data, StorageSerializers.json(PersistentPlayerData.class)
                .deserialize(StorageSerializers.json(PersistentPlayerData.class).serialize(data)));
        assertEquals(data, StorageSerializers.yaml(PersistentPlayerData.class)
                .deserialize(StorageSerializers.yaml(PersistentPlayerData.class).serialize(data)));
    }

    @Test
    void cacheExpiresEvictsAndCoalescesAsyncLoads() throws Exception {
        Cache<String, String> cache = new ConcurrentCache<>(
                new CacheConfig(1, Duration.ofMillis(30), EvictionPolicy.LRU));
        try (cache) {
            AtomicInteger loads = new AtomicInteger();
            CompletableFuture<String> gate = new CompletableFuture<>();
            CompletableFuture<String> first = cache.getOrLoadAsync("key", ignored -> {
                loads.incrementAndGet();
                return gate;
            });
            CompletableFuture<String> second = cache.getOrLoadAsync("key", ignored -> {
                loads.incrementAndGet();
                return CompletableFuture.completedFuture("unexpected");
            });
            assertSame(first, second);
            assertEquals(1, loads.get());
            gate.complete("loaded");
            assertEquals("loaded", first.join());
            cache.put("other", "value");
            assertFalse(cache.contains("key"));
            Thread.sleep(50);
            assertFalse(cache.contains("other"));
            assertTrue(cache.statistics().evictions() >= 2);
        }
    }

    private static DatabaseMigration migration(int version, String description,
                                               dev.kehlchen.lib.api.database.TransactionWork<Void> work) {
        return new DatabaseMigration() {
            @Override public int version() { return version; }
            @Override public String description() { return description; }
            @Override public void migrate(dev.kehlchen.lib.api.database.Transaction transaction) throws Exception {
                work.execute(transaction);
            }
        };
    }
}
