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
```

## Phase 4 modules

- `api.command`: fluent command builder, typed arguments, parsing, cooldowns and tab completion contracts
- `api.permission`: permission checks, temporary permissions and explicit grant/deny explanations
- `api.item`: ItemBuilder, item utilities and namespaced custom-item identifiers
- `api.gui`: GUI definitions, click contexts and lifecycle-managed inventory sessions
- Core implementations: Bukkit/Paper command, permission, item and GUI services

### Command example

```java
CommandService commands = runtime.services().getRequired(CommandService.class);
commands.register(this,
    CommandBuilder.command("heal")
        .permission("example.heal")
        .playerOnly()
        .description("Heals the player")
        .executor(context -> {
            context.player().ifPresent(player -> player.setHealth(player.getMaxHealth()));
            context.reply(Component.text("You have been healed"));
            return CommandResult.SUCCESS;
        }));
```

### Item example

```java
ItemService items = runtime.services().getRequired(ItemService.class);
ItemStack item = items.create(
    ItemBuilder.of(Material.DIAMOND_SWORD)
        .name("<gold>Legendary Sword")
        .lore("<gray>A powerful weapon")
        .unbreakable(true)
        .build());
items.identifyCustom(item, new NamespacedCustomItem(
    new NamespacedKey(this, "legendary_sword")));
```

### GUI example

```java
GuiService guis = runtime.services().getRequired(GuiService.class);
Gui gui = guis.create(GuiBuilder.gui()
    .title("<dark_gray>Example Menu")
    .rows(3)
    .item(13, ItemBuilder.of(Material.DIAMOND).name("<aqua>Click me").build())
    .onClick(13, context -> context.player().sendMessage(Component.text("Clicked")))
    .build());
gui.open(player);
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

## Phase 5 modules

- HikariCP-backed SQLite, MySQL, MariaDB and PostgreSQL JDBC access, prepared queries, batching, transactions and migrations
- JSON, YAML, properties, SQLite-backed and in-memory storage providers with custom serializers
- Async storage and generic storage-backed repositories with filtering and pagination
- Thread-safe bounded TTL caches with LRU/FIFO eviction and coalesced asynchronous loads
- Optional UUID-keyed player persistence; existing in-memory player behavior remains the default

The standalone plugin registers `DatabaseFactory`, `StorageFactory`, and `CacheFactory` in its service registry and tracks created instances for shutdown. Applications embedding `core` should likewise register created resources with their `ResourceManager`.

### Database example

```java
DatabaseFactory databases = runtime.services().getRequired(DatabaseFactory.class);
Database database = databases.create(DatabaseConfig.builder(DatabaseType.SQLITE)
    .file(getDataFolder().toPath().resolve("data.db"))
    .build());

database.updateAsync("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY)")
    .join();
database.updateAsync("INSERT INTO players (uuid) VALUES (?)", playerId.toString());
```

For MySQL, MariaDB, or PostgreSQL, supply the JDBC URL and credentials instead of a file:

```java
DatabaseConfig config = DatabaseConfig.builder(DatabaseType.POSTGRESQL)
    .jdbcUrl("jdbc:postgresql://localhost:5432/game")
    .credentials("game_user", "secret")
    .maximumPoolSize(12)
    .build();
```

SQLite uses a single-connection pool by default. Synchronous database calls run on the calling thread; use the async methods for server-facing code. `DatabaseMigrator` applies ordered `DatabaseMigration`s once and records their versions.

### Storage, repository, and cache examples

```java
StorageFactory storageFactory = runtime.services().getRequired(StorageFactory.class);
Storage<UUID, PlayerProfile> profiles = storageFactory.create(PlayerProfile.class,
    StorageProviders.json(
        getDataFolder().toPath().resolve("players"),
        StorageSerializers.json(UUID.class),
        StorageSerializers.json(PlayerProfile.class)));

profiles.saveAsync(playerId, profile);
Repository<UUID, PlayerProfile> repository = new StorageRepository<>(profiles);
repository.findById(playerId).thenAccept(profile -> { /* ... */ });

Cache<String, PlayerProfile> cache =
    runtime.services().getRequired(CacheFactory.class).create(
        new CacheConfig(5_000, Duration.ofMinutes(10), EvictionPolicy.LRU));
cache.getOrLoadAsync(playerId.toString(),
    key -> repository.findById(UUID.fromString(key)).thenApply(Optional::orElseThrow));
```

Use `StorageProviders.yaml`, `properties`, `memory`, or `sqlite` for the other built-in providers. Custom providers implement `StorageProvider`; custom formats implement `StorageSerializer`.

`PaperPlayerPersistence` is an opt-in lifecycle module backed by `Storage<UUID, PersistentPlayerData>`. It loads asynchronously, saves on disconnect and shutdown, periodically flushes dirty profiles, and retries failed I/O. To persist `PaperPlayerService` preferences and use persisted first-join/playtime values, construct it with the persistence service; the existing constructor continues to use in-memory preferences.

## Phase 6 modules

- World, chunk, block, location and entity services under `dev.kehlchen.lib.api.*`
- Bounded and lifecycle-managed Paper implementations under `dev.kehlchen.lib.core.world`, `chunk`, `block`, `location`, `entity`, `particle`, and `effects`
- Cuboid, sphere and cylinder regions
- In-memory cooldown and combat trackers
- Mathematical particle shape generators

### World and location example

```java
WorldService worlds = runtime.services().getRequired(WorldService.class);
World world = worlds.find("world").orElseThrow();
worlds.setTime(world, 6_000);

LocationService locations = runtime.services().getRequired(LocationService.class);
SafeLocation safe = locations.findSafe(player.getLocation(), 16);
safe.location(); // Only teleport after safe.safe()
```

### Region and particle example

```java
Region region = new CuboidRegion(player.getWorld(), -10, 64, -10, 10, 80, 10);
region.players(100).forEach(player -> {
    // Player is inside this region.
});

List<Location> circle = ParticleShape.circle(player.getLocation(), 3.0, 24);
ParticleService particles = runtime.services().getRequired(ParticleService.class);
circle.forEach(point -> particles.spawn(point, Particle.END_ROD, 1, 0, 0, 0, 0));
```

### Threading and limitations

World, block, entity and particle operations must run with world/region ownership. APIs that would perform unsafe Bukkit access return explicit `Result` failures instead of moving work asynchronously. Chunk APIs avoid unnecessary loading and async loading uses explicit result futures where the server provides it. No full Folia runtime test is claimed; validate Folia behavior on a Folia test server. Repeating effects are lifecycle-managed and close together with the effect service.

## Phase 7 modules

The standalone Paper plugin registers an asynchronous JDK HTTP client, WebSocket service,
webhook sender, plugin-message service, and the integration registry. The API module does
not add network-client dependencies. Optional integrations are separate artifacts:

- `dev.kehlchen:integration-redis:1.0` uses Lettuce
- `dev.kehlchen:integration-vault:1.0` adapts Vault Economy
- `dev.kehlchen:integration-bungee:1.0` uses Bukkit's BungeeCord plugin channel
- `dev.kehlchen:integration-velocity:1.0` is a Velocity-only proxy plugin

Add only the integration artifacts you use. Vault API and Velocity API are provided by
their respective runtime and are not bundled. The Velocity artifact is installed on
Velocity; do not install it as a Paper plugin.

## Phase 8 modules

- Backend-neutral packet listening/sending under `dev.kehlchen.lib.api.packet`
- Virtual entity and hologram state plus optional packet backends under
  `dev.kehlchen.lib.api.virtual`
- Public-Bukkit-API fake block changes under `core.virtual.PaperVirtualBlockService`
- Performance snapshots, thread-safe profiling, opt-in metrics and diagnostics
- Isolated version-adapter registry under `dev.kehlchen.lib.api.nms`

The standalone plugin registers `PacketService`, `VirtualEntityService`,
`VirtualBlockService`, `HologramService`, `Profiler`, `Metrics`,
`PerformanceMonitor`, `Diagnostics`, and `ServerAdapterRegistry`.

Packet interception and modification are not available through Bukkit/Paper alone.
Registering an optional backend is explicit; when no backend is present, packet
operations return a clear `UnsupportedOperationException`. Fake entity and hologram
packet delivery also requires an optional virtual entity backend. Fake block changes
use `Player.sendBlockChange`, do not alter the real world, restore automatically on
expiry and are bounded by a configurable limit.

Packet and virtual backends must not leak internal Minecraft/NMS types through public
API contracts. NMS adapters are only enabled for exact adapters that explicitly
advertise support for the detected Minecraft version.

```java
PacketService packets = runtime.services().getRequired(PacketService.class);
Result<Void> packet = packets.send(player.getUniqueId(),
    PacketDirection.CLIENTBOUND, "example", new MapPacket(Map.of("value", 1)));

Profiler profiler = runtime.services().getRequired(Profiler.class);
profiler.setEnabled(true);
profiler.measure("database-refresh", () -> refreshDatabase());

Metrics metrics = runtime.services().getRequired(Metrics.class);
metrics.counter("example.requests", 1);

PerformanceMonitor monitor =
    runtime.services().getRequired(PerformanceMonitor.class);
PerformanceSnapshot snapshot = monitor.snapshot();
```

TPS/MSPT are returned only when the server exposes them through a supported API.
If unavailable, `OptionalDouble.empty()` is used. CPU process-load values may also
be unavailable and are reported as `-1` rather than guessed. A CPU value from `0`
through `100` is the measured process-load percentage.

Folia: fake block updates are scheduled for each player's entity ownership. Packet
callbacks and packet backend implementations must remain thread-safe and perform
Bukkit entity/world access only with region or entity ownership. Virtual entity and
packet interception compatibility require a tested backend; no Folia compatibility is
claimed by default.

### HTTP and JSON

```java
HttpClientService http = runtime.services().getRequired(HttpClientService.class);
JsonSerialization json = runtime.services().getRequired(JsonSerialization.class);

HttpCall call = http.execute(HttpRequestData.builder(HttpMethod.POST,
        URI.create("https://api.example.net/v1/players"))
    .header("Authorization", "Bearer " + tokenFromConfig)
    .json(json.toJson(playerData))
    .timeout(Duration.ofSeconds(5))
    .retry(new RetryPolicy(3, Duration.ofMillis(200), Duration.ofSeconds(2), Set.of(502, 503, 504)))
    .build());

call.thenAccept(result -> {
    if (result.isSuccess()) {
        HttpResponseData response = result.value().orElseThrow();
        if (response.isSuccessful()) {
            ApiReply reply = json.fromJson(response.text(), ApiReply.class);
        }
    } else {
        logger.log(Level.WARNING, "External API request failed", result.error().orElseThrow());
    }
});
```

`HttpMethod` supports GET, POST, PUT, PATCH, and DELETE. Requests support headers, query
parameters, UTF-8 text/JSON, per-request timeouts, and opt-in bounded retries. Responses
preserve the status, headers, and body; non-2xx statuses are responses, not transport
exceptions. Cancelling the returned `HttpCall` cancels its pending attempt. Defaults cap
request/response bodies at 2 MiB, concurrency at 32 and request rate at 20 per second.
The pending queue is bounded to 128 requests by default.
HTTP redirects are disabled. HTTPS is required by default; plain HTTP and private
destinations require an explicit host allow-list. DNS checks are best-effort and cannot
pin the JDK client's later DNS lookup, so use a narrow `allowedHosts` allow-list for
untrusted endpoint configuration. Never include tokens in logs or source code.

All calls run off-thread. Completion handlers also run asynchronously; schedule Bukkit
work through the right scheduler (and use entity/region scheduling for Folia).

### WebSockets and webhooks

```java
WebSocketService sockets = runtime.services().getRequired(WebSocketService.class);
sockets.connect(URI.create("wss://stream.example.net/events"), WebSocketConfig.defaults(),
    new WebSocketListener() {
        @Override public void onText(WebSocketConnection connection, String text) {
            // Parse on this network callback; schedule Bukkit work separately.
        }
    }).thenAccept(result -> result.value().ifPresent(connection ->
        connection.sendText("{\"type\":\"subscribe\"}")));
```

WebSockets require `wss://`, allow up to 64 concurrent connections and bound messages to 1 MiB by default, send periodic pings,
reconnect a bounded number of times, and are closed by the service during plugin shutdown.
WebSocket listeners run on network threads and must not mutate Bukkit objects directly.

```java
WebhookService webhooks = runtime.services().getRequired(WebhookService.class);
WebhookMessage message = new WebhookMessage(
    "A player joined", "KehlLib", null,
    List.of(new WebhookEmbed("Server", "Online", null, 0x33AA55, List.of(),
        new WebhookEmbed.Footer("Status", null))),
    List.of(), Map.of(), Duration.ofSeconds(5), RetryPolicy.none());
webhooks.send(URI.create(webhookUrlFromConfig), message)
    .thenAccept(result -> { /* inspect result and HTTP status */ });
```

Webhook payload construction is separate from generic HTTP. Discord-compatible embeds,
basic component-row JSON and custom headers are supported; provider-specific constraints
remain the caller's responsibility. The endpoint URL may itself contain a secret—do not
log it.

### Plugin messaging

```java
PluginMessageService messaging = runtime.services().getRequired(PluginMessageService.class);
NamespacedKey channel = new NamespacedKey(this, "sync");
messaging.register(this, channel, (carrier, receivedChannel, bytes) -> {
    String text = new String(bytes, StandardCharsets.UTF_8);
});
Result<Void> sent = messaging.sendString(this, player, channel, "hello");
```

Use `PluginMessagePayload` and `PayloadCodec<T>` for bounded byte/string/custom payload
serialization. Channels are registered and removed with the service lifecycle. Bukkit's
transport needs an online player carrier: this API does not provide server-to-server
transport without a player. Registration and sending are server-thread operations, and
payloads are capped at 32,766 bytes.

### BungeeCord and Velocity

The BungeeCord integration is a Paper-side optional module. Register it with the core
integration registry; requests that query the proxy require an online player as the
message carrier:

```java
ProxyIntegration bungee = new BungeeCordProxyIntegration(this);
runtime.services().getRequired(IntegrationRegistry.class).register(bungee);
bungee.currentServer(player.getUniqueId()).thenAccept(result -> { /* inspect result */ });
bungee.connect(player.getUniqueId(), "lobby");
```

The Velocity module is instead installed on the Velocity proxy and provides direct
connection/server/player operations plus a size-bounded plugin-message channel. BungeeCord
plugin messages provide less information than Velocity's native API; for example, the
Bungee adapter only returns player details for the online carrier. Install one proxy
integration, not both. A Velocity-side plugin can obtain the optional service from the
plugin instance:

```java
ProxyIntegration velocity = proxy.getPluginManager().getPlugin("kehllib-velocity")
    .flatMap(container -> container.getInstance())
    .filter(ProxyIntegration.class::isInstance)
    .map(ProxyIntegration.class::cast)
    .orElseThrow();
velocity.connect(playerId, "lobby");
```

Backend plugins must register the `kehllib:proxy` channel to send or receive Velocity
messages. The Bungee adapter uses Bukkit's main-thread scheduler and does not claim Folia
compatibility.

### Redis and Economy

```java
char[] password = System.getenv("KEHLLIB_REDIS_PASSWORD").toCharArray();
RedisService redis = new LettuceRedisIntegration(new RedisConfig(
    "redis.example.net", 6379, System.getenv("KEHLLIB_REDIS_USER"), password, 0, true,
    Duration.ofSeconds(3), Duration.ofSeconds(2), Duration.ofSeconds(1),
    5, 65_536));
java.util.Arrays.fill(password, '\0');
runtime.services().getRequired(IntegrationRegistry.class).register(redis);
redis.ready().thenAccept(connected -> {
    if (connected.isSuccess()) redis.publish("events", json.toJson(event));
});
```

Redis commands and subscription callbacks are asynchronous. Use `JsonSerialization` to
encode/decode JSON payloads; callbacks run on Lettuce I/O threads. TLS is opt-in and
credentials are passed as configuration, never logged. Subscriptions are `Resource`s and
close with the integration. The integration uses a bounded initial reconnect attempt
policy; test credentials and network settings are deployment-specific.

Economy code depends only on KehlLib's `EconomyProvider`; Vault is an optional adapter:

```java
IntegrationRegistry integrations = runtime.services().getRequired(IntegrationRegistry.class);
integrations.register(new VaultEconomyIntegration(this, runtime.services()));
EconomyProvider economy = runtime.services().getRequired(EconomyProvider.class);
EconomyTransaction deposit = economy.deposit(player.getUniqueId(), new BigDecimal("25.00"));
```

Vault operations must be called on the server thread because providers are not guaranteed
to be thread-safe. The adapter returns explicit failed transaction results when no provider
is present or an operation fails. `setBalance` is performed as one Vault deposit/withdraw
delta; Vault itself does not define an atomic set operation.

### Phase 7 threading, security and validation limits

Network and Redis I/O is asynchronous and lifecycle-managed. WebSocket connections and
HTTP workers are closed on shutdown. Bukkit plugin messaging and Vault calls deliberately
reject unsafe off-thread use rather than silently switching schedulers. Velocity is a
separate proxy runtime; no Bukkit/Folia compatibility is claimed for its plugin.

The HTTP client rejects redirects, bounds bodies/concurrency/rate, requires TLS by
default, and rejects private resolved addresses unless a developer explicitly allow-lists
the host. Do not accept arbitrary request URLs from players. A host allow-list is strongly
recommended for configured URLs because DNS rebinding cannot be completely prevented by
the JDK HTTP client. Configure bounded timeouts and retry counts; retries of POST requests
can repeat side effects.

Unit tests use a local in-process HTTP server and do not contact external services.
Redis, Vault, BungeeCord, Velocity, and Folia behavior still requires deployment-level
integration tests; compiling those modules does not constitute a real server runtime test.

## Development

The project uses Gradle Kotlin DSL and Java 25.

```powershell
.\gradlew.bat test
.\gradlew.bat :plugin:build
.\gradlew.bat build
.\gradlew.bat publishToMavenLocal
```

Before a release, run the complete build and inspect the API, core, plugin, and example
plugin JARs plus the generated Maven publications. Real Paper/Purpur/Folia runtime
behavior, credentials, Redis, Vault, proxy, packet backends, and external network services
must be tested in the target deployment environment.

## Standalone plugin

Install the `plugin` JAR on a Paper or Purpur server. Other plugins can depend on `KehlLib` and integrate through their own plugin APIs. The standalone JAR does not need to be combined with an embedded core copy.
