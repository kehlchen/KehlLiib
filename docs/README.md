# KehlLib Documentation

KehlLib is a modular Java library for Paper and Purpur server plugins. It provides lifecycle management, a shared service registry, scheduling, player and messaging APIs, commands, permissions, items, GUIs, persistence, world and gameplay helpers, networking, optional proxy and economy integrations, packet abstractions, virtual entities, monitoring, and diagnostics.

KehlLib is published as separate Gradle modules:

| Module | Artifact | Purpose |
|---|---|---|
| `api` | `dev.kehlchen:kehllib-api:1.0` | Public contracts and data types |
| `core` | `dev.kehlchen:kehllib-core:1.0` | Paper implementations and infrastructure |
| `plugin` | `dev.kehlchen:kehllib:1.0` | Standalone bundled Paper/Purpur plugin |
| `integration-redis` | `dev.kehlchen:kehllib-integration-redis:1.0` | Optional Lettuce Redis integration |
| `integration-vault` | `dev.kehlchen:kehllib-integration-vault:1.0` | Optional Vault economy adapter |
| `integration-bungee` | `dev.kehlchen:kehllib-integration-bungee:1.0` | Optional BungeeCord/Paper proxy integration |
| `integration-velocity` | `dev.kehlchen:kehllib-integration-velocity:1.0` | Optional Velocity proxy integration |

## Documentation map

- [Getting Started](getting-started/installation.md)
- [Architecture](fundamentals/architecture.md)
- [Lifecycle and Services](fundamentals/lifecycle-services.md)
- [Configuration and Localization](api/configuration.md)
- [Utilities and Logging](api/utilities-logging.md)
- [Runtime APIs](api/runtime.md)
- [Developer APIs](api/developer.md)
- [Persistence and Storage](api/persistence.md)
- [World and Gameplay](api/world-gameplay.md)
- [Networking and Integrations](api/networking-integrations.md)
- [Advanced Systems](api/advanced.md)
- [Optional Integrations](integrations/optional-integrations.md)
- [Compatibility and Threading](compatibility/platforms.md)
- [Examples](examples/usage.md)
- [Troubleshooting](troubleshooting.md)

## Requirements

- Java 25 toolchain
- Gradle projects using Kotlin DSL
- Paper or Purpur for the Bukkit/Paper runtime
- Velocity 3.4 for the separate Velocity proxy artifact

The repository declares Minecraft/Paper API 26.3. Runtime compatibility with older or newer server builds is not guaranteed.
