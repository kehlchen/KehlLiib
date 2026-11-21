# Practical Examples

## Register and access services

```java
runtime.register(scheduler);
runtime.register(players);
runtime.services().register(Scheduler.class, scheduler);
runtime.services().register(PlayerService.class, players);
runtime.enableAll();

Scheduler scheduling =
    runtime.services().getRequired(Scheduler.class);
```

## Schedule tasks

```java
scheduler.runAsync(plugin, () -> loadExternalData());
scheduler.runSyncLater(plugin, Duration.ofSeconds(5),
    () -> player.sendMessage(Component.text("Ready")));
scheduler.repeatAsync(plugin, Duration.ZERO, Duration.ofMinutes(1),
    () -> refreshCache());
```

Cancel through the `TaskHandle` returned inside the `Result`.

## Register an event

```java
EventRegistration registration = events.register(
    PlayerJoinEvent.class,
    plugin,
    EventPriority.NORMAL,
    true,
    event -> event.getPlayer().isOnline(),
    event -> players.markActivity(event.getPlayer().getUniqueId()));
runtime.resources().register(registration);
```

## Create a command

```java
commands.register(plugin,
    CommandBuilder.command("status")
        .permission("example.status")
        .executor(context -> {
            context.reply(Component.text("Server is running"));
            return CommandResult.SUCCESS;
        }));
```

## Send an Adventure message

```java
Messaging messaging = runtime.services().getRequired(Messaging.class);
messaging.chat(player,
    messaging.component("<green>Welcome <bold>" + player.getName()));
```

## Read configuration

```java
Config config = new ConfigBuilder()
    .path(plugin.getDataFolder().toPath().resolve("config.yml"))
    .format(ConfigFormat.YAML)
    .defaultValue("database.enabled", true)
    .build();

boolean databaseEnabled =
    config.getBoolean("database.enabled", true);
```

## Connect to SQLite

```java
Database database =
    runtime.services().getRequired(DatabaseFactory.class)
        .create(DatabaseConfig.builder(DatabaseType.SQLITE)
            .file(plugin.getDataFolder().toPath().resolve("data.db"))
            .build());
runtime.resources().register(database);
database.updateAsync(
    "CREATE TABLE IF NOT EXISTS homes (uuid TEXT PRIMARY KEY, location TEXT)")
    .join();
```

## Create a storage-backed repository

```java
Storage<UUID, PlayerProfile> storage = storageFactory.create(
    PlayerProfile.class,
    StorageProviders.memory());
Repository<UUID, PlayerProfile> repository =
    new StorageRepository<>(storage);
repository.save(playerId, profile).join();
```

`StorageProviders.memory()` is in-memory and does not persist across restarts.

## Send an HTTP request

```java
http.execute(HttpRequestData.builder(
        HttpMethod.GET, URI.create("https://example.net/status"))
    .timeout(Duration.ofSeconds(5))
    .build())
    .thenAccept(result -> {
        result.value().ifPresent(response -> {
            // Network callback: use Scheduler for Bukkit work.
        });
    });
```

## Check optional economy

```java
runtime.services().get(EconomyProvider.class)
    .ifPresent(economy -> {
        EconomyTransaction tx =
            economy.deposit(playerId, new BigDecimal("25.00"));
        // inspect tx
    });
```
