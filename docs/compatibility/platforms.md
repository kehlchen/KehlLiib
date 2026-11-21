# Compatibility and Threading

## Supported platforms

- Paper: primary platform for the standalone plugin and core implementations.
- Purpur: supported through Paper API compatibility.
- Folia: limited scheduling support is implemented, but complete runtime compatibility is not claimed.
- Velocity: separate proxy plugin artifact.

The repository declares Paper API/Minecraft `26.3`. An older or newer server is not guaranteed to work, and an API compile success does not prove runtime compatibility.

## Bukkit thread safety

Do not access Bukkit `World`, `Chunk`, `Entity`, `Player`, `Location`, `Block`, or `Inventory` objects from:

- HTTP completion handlers
- WebSocket callbacks
- Redis callbacks
- arbitrary async tasks
- database callbacks

Use `Scheduler.runSync`, `runForLocation`, or `runForEntity` where an ownership requirement exists.

## Scheduler behavior

The `PaperScheduler` detects Folia where possible:

- `runSync` uses a compatible global context.
- `runAsync` executes off-server.
- `repeatSync` executes periodically in the compatible sync context.
- `runForLocation` and `runForEntity` use region/entity schedulers when available.
- Traditional Bukkit schedulers remain the fallback for non-Folia servers.

`isMainThread()` means the server main thread or a compatible global region thread. It does not prove that a region/entity-owned Bukkit object may be accessed. Region ownership is explicit.

## Folia limitations

The repository has scheduler and fake-block/entity scheduling design for Folia, but no complete Folia runtime test suite is claimed. Verify:

- region-owned world and block work
- entity-owned player operations
- teleport behavior
- repeating effects
- packet callbacks
- virtual entity backends
- plugin messaging

The BungeeCord adapter does not claim Folia compatibility.

## Packet and virtual backends

Packet interception and modification require a separately installed compatible backend. Fake block changes use only the public Paper API and do not modify the real world. Fake entity delivery and holograms require a compatible virtual entity packet backend.
