# Optional Integrations

Do not register an integration unless the artifact is deployed and the external service/plugin is available. Absence should be handled through `Optional` service lookup or `IntegrationRegistry`.

## Redis

Artifact: `dev.kehlchen:kehllib-integration-redis:1.0`

`LettuceRedisIntegration` implements `RedisService` and uses `RedisConfig`.

```kotlin
implementation("dev.kehlchen:kehllib-integration-redis:1.0")
```

```java
char[] password = System.getenv("KEHLLIB_REDIS_PASSWORD").toCharArray();
RedisService redis = new LettuceRedisIntegration(new RedisConfig(
    "redis.example.net", 6379,
    System.getenv("KEHLLIB_REDIS_USER"),
    password, 0, true,
    Duration.ofSeconds(3), Duration.ofSeconds(2),
    Duration.ofSeconds(1), 5, 65_536));
runtime.services().getRequired(IntegrationRegistry.class).register(redis);
Arrays.fill(password, '\0');
```

Redis commands and callbacks are asynchronous. Subscription callbacks run on Lettuce I/O threads. TLS is opt-in. Credentials must not be logged. Redis runtime behavior must be tested against the target Redis deployment.

## Vault

Artifact: `dev.kehlchen:kehllib-integration-vault:1.0`

```kotlin
implementation("dev.kehlchen:kehllib-integration-vault:1.0")
```

The integration is compile-time optional and requires Vault API and a Vault-compatible economy plugin at runtime.

```java
IntegrationRegistry integrations =
    runtime.services().getRequired(IntegrationRegistry.class);
integrations.register(new VaultEconomyIntegration(plugin,
    runtime.services()));
```

Call economy operations on the server thread. The integration returns explicit failed transactions when no provider or operation is available. `setBalance` is implemented as a deposit/withdraw delta because Vault does not define an atomic set operation.

## BungeeCord

Artifact: `dev.kehlchen:kehllib-integration-bungee:1.0`

Install on Paper/Purpur, register `BungeeCordProxyIntegration`, and provide an online player carrier for proxy queries.

```java
ProxyIntegration bungee = new BungeeCordProxyIntegration(plugin);
runtime.services().getRequired(IntegrationRegistry.class).register(bungee);
bungee.currentServer(playerId).thenAccept(result -> { /* inspect */ });
bungee.connect(playerId, "lobby");
```

The adapter uses Bukkit scheduling and no Folia compatibility is claimed. It provides less proxy detail than the Velocity native API. It cannot query a proxy without a valid online player carrier.

## Velocity

Artifact: `dev.kehlchen:kehllib-integration-velocity:1.0`

Install only on a Velocity proxy. This module does not belong on a Paper server.

The plugin exposes `KehlLibVelocityPlugin` and `ProxyIntegration`. Backend plugins should register the `kehllib:proxy` plugin-message channel. Direct proxy operations include connection lookup, server lookup, player lookup, and connect operations according to `ProxyIntegration`.

## Packet backends

No packet backend is bundled in the standalone plugin. A compatible optional implementation must register a `PacketBackend`. Without one:

- packet interception is unavailable
- packet modification is unavailable
- virtual entity delivery has no packet transport
- packet calls must be treated as unsupported
