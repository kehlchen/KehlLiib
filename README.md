# KehlLib

KehlLib is a modular framework for Paper and Purpur plugins.

## Phase 1 modules

- `api`: public lifecycle, service registry, resource, result, and event contracts
- `core`: thread-safe service registry, resource cleanup, event bus, and module bootstrap
- `plugin`: standalone KehlLib Paper plugin
- `example-plugin`: minimal plugin example

## Phase 2 modules

- Utilities under `dev.kehlchen.lib.core.util`
- Unified logging under `dev.kehlchen.lib.core.logging`
- YAML, JSON, TOML, and properties configuration under `dev.kehlchen.lib.core.config`
- JSON, YAML, and typed serializer helpers under `dev.kehlchen.lib.core.serialization`
- Properties-based localization under `dev.kehlchen.lib.core.localization`

## Phase 3 modules

- Scheduler APIs and a Paper/Folia-compatible implementation under `dev.kehlchen.lib.api.scheduler` and `dev.kehlchen.lib.core.scheduler`
- UUID-safe player, profile, preference, AFK, playtime, and teleport abstractions under `dev.kehlchen.lib.api.player`
- Adventure MiniMessage chat, actionbar, title, bossbar, sound, particle, broadcast, placeholder, and localization messaging
- Lifecycle-managed Bukkit event registrations and the existing internal event bus

### Scheduler example

```java
Scheduler scheduler = runtime.services().getRequired(Scheduler.class);

scheduler.runSyncLater(plugin, Duration.ofSeconds(5), () ->
    Bukkit.broadcast(Component.text("Ready"))
);

scheduler.repeatAsync(plugin, Duration.ZERO, Duration.ofSeconds(30),
    () -> refreshDatabase());
```

Folia users should use location- or entity-bound APIs for world/entity operations:

```java
scheduler.runForEntity(plugin, player, () -> player.getInventory().addItem(item));
scheduler.runForLocation(plugin, location, () -> location.getWorld().strikeLightning(location));
```

Traditional Bukkit servers use the Bukkit scheduler automatically. Folia region/entity operations are never silently moved to the wrong thread.

### Player example

```java
PlayerService players = runtime.services().getRequired(PlayerService.class);
players.online(player.getUniqueId()).ifPresent(view -> {
    players.markActivity(view.uniqueId());
    view.preferences().setString("theme", "dark");
});
```

### Messaging example

```java
Messaging messaging = runtime.services().getRequired(Messaging.class);
messaging.chat(sender, messaging.component("<green>Hello <bold>" + sender.getName() + "</bold></green>"));
messaging.actionBar(player, messaging.localized("welcome", player.getName()));
```

### Event example

```java
BukkitEventRegistry events = runtime.services().getRequired(BukkitEventRegistry.class);
EventRegistration registration = events.register(PlayerJoinEvent.class, this,
    EventPriority.NORMAL, true, join -> join.getPlayer().isOnline(), join -> {
        // handle join
    });
runtime.resources().register(registration);
```

## Development

The project uses Gradle Kotlin DSL and Java 25.

```powershell
.\gradlew.bat test
.\gradlew.bat :plugin:build
```

## Standalone plugin

Install the `plugin` JAR on a Paper or Purpur server. Other plugins can depend on `KehlLib` and integrate through their own plugin APIs. The standalone JAR does not need to be combined with an embedded core copy.
