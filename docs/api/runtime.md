# Runtime APIs

## Scheduler

`Scheduler` supports synchronous and asynchronous delayed and repeating tasks, entity/region scheduling, teleport scheduling, task groups, task chains, exception handlers, statistics, and `Executor` integration.

```java
Scheduler scheduler = runtime.services().getRequired(Scheduler.class);

scheduler.runAsync(plugin, () -> refreshDatabase());
scheduler.runSyncLater(plugin, Duration.ofSeconds(5),
    () -> Bukkit.broadcast(Component.text("Ready")));
scheduler.repeatAsync(plugin, Duration.ZERO, Duration.ofMinutes(1),
    () -> refreshDatabase());
```

For Folia, use `runForLocation` or `runForEntity` for world/entity Bukkit access. `runSync` is a global scheduling operation and should not be used as an implicit replacement for a location-owned or entity-owned operation.

Task methods return `Result<TaskHandle>`. Inspect `TaskHandle` and `TaskStatus` for cancellation and state. `SchedulerStats` exposes counters.

## Players

`PlayerService` provides online/offline views, profiles, preferences, playtime, AFK activity, teleport operations, and persistence hooks.

```java
PlayerService players = runtime.services().getRequired(PlayerService.class);
players.online(player.getUniqueId()).ifPresent(view -> {
    view.preferences().setString("theme", "dark");
    players.markActivity(view.uniqueId());
});
```

`OnlinePlayer`, `OfflinePlayerView`, `PlayerProfile`, `CompletablePlayerPreferences`, `PersistentPlayerData`, `PlayerPersistenceConfig`, and `PlayerPersistence` distinguish Bukkit-backed presence from persisted storage.

## Messaging

`Messaging` is the Adventure abstraction for chat, action bars, titles, boss bars, sound, placeholders, broadcasts, localization, and MiniMessage rendering.

```java
Messaging messaging = runtime.services().getRequired(Messaging.class);
Component message = messaging.component("<green>Hello");
messaging.chat(player, message);
```

`AdventureMessaging` returns or formats `Component` values. Never feed untrusted player input into a MiniMessage string without escaping or an application-level validation policy.

## Events

`BukkitEventRegistry` registers Bukkit listeners with `EventPriority`, ignore-cancelled policy, predicates, and handlers.

```java
EventRegistration join = events.register(
    PlayerJoinEvent.class,
    plugin,
    EventPriority.NORMAL,
    true,
    event -> event.getPlayer().isOnline(),
    event -> event.getPlayer().sendMessage(Component.text("Welcome")));
runtime.resources().register(join);
```

`EventBus` and `KehlLibEvent` provide an internal application event channel. `Subscription` is lifecycle-managed.
