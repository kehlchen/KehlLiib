# Persistence, Databases, Storage, and Caches

## Databases

`DatabaseFactory` creates a `Database` from `DatabaseConfig`. Supported `DatabaseType` values are `SQLITE`, `MYSQL`, `MARIADB`, and `POSTGRESQL`.

```java
Database database = runtime.services()
    .getRequired(DatabaseFactory.class)
    .create(DatabaseConfig.builder(DatabaseType.SQLITE)
        .file(plugin.getDataFolder().toPath().resolve("data.db"))
        .maximumPoolSize(1)
        .build());
runtime.resources().register(database);
```

The SQLite pool is normally a single connection. The config defaults are:

| Setting | Default |
|---|---:|
| maximum pool size | 10 |
| minimum idle | 2 |
| connection timeout | 30 seconds |
| idle timeout | 10 minutes |
| maximum lifetime | 30 minutes |
| leak detection | disabled (`Duration.ZERO`) |
| query timeout | 30 seconds |

`Database` exposes synchronous and asynchronous updates, queries, batching, prepared parameters, transactions, statistics, and lifecycle operations. `Transaction` and `TransactionWork` express transactional operations. `RowMapper<T>` maps a result row.

```java
database.updateAsync(
    "CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY)")
    .join();

database.updateAsync(
    "INSERT INTO players(uuid) VALUES (?)",
    player.getUniqueId().toString()).join();
```

Call synchronous database methods off the server thread. Async methods return futures and do not make a server's subsequent Bukkit callback execute on a server thread.

`DatabaseMigrator` applies ordered `DatabaseMigration` implementations once and records migration versions. Verify a migration's SQL and transaction semantics for your target database.

## Storage

`StorageFactory` creates `Storage<K,V>` from a value type and `StorageProvider<K,V>`. `StorageProvider` implementations include JSON, YAML, properties, SQLite, and memory providers in `StorageProviders`.

```java
Storage<UUID, PlayerProfile> profiles =
    storageFactory.create(PlayerProfile.class,
        StorageProviders.json(
            plugin.getDataFolder().toPath().resolve("players"),
            StorageSerializers.json(UUID.class),
            StorageSerializers.json(PlayerProfile.class)));
runtime.resources().register(profiles);
```

Storage operations return futures for async use and expose load/save/delete/list operations depending on the public interface. Custom providers implement `StorageProvider<K,V>` and custom formats implement `StorageSerializer<T>`.

## Repositories

`Repository<K,V>` and `StorageRepository<K,V>` provide generic lookup, filtering, sorting, pagination, and save/delete operations.

```java
Repository<UUID, PlayerProfile> repository =
    new StorageRepository<>(profiles);

Optional<PlayerProfile> profile =
    repository.findById(playerId).join();
Page<PlayerProfile> page =
    repository.findAll(new PageRequest(0, 25, SortOrder.ASC)).join();
```

`Page`, `PageRequest`, and `SortOrder` are public API types. Verify provider filtering support before relying on repository behavior for a custom `StorageProvider`.

## Caches

`Cache<K,V>` is bounded, optionally TTL-based, and lifecycle-managed. `CacheFactory` creates caches from `CacheConfig`.

```java
Cache<String, PlayerProfile> cache =
    cacheFactory.create(new CacheConfig(
        5_000, Duration.ofMinutes(10), EvictionPolicy.LRU));
```

Built-in eviction policies include `LRU` and `FIFO`; inspect `EvictionPolicy` for the exact enum values. `CacheStats` reports hits, misses, evictions, size, and hit rate.
