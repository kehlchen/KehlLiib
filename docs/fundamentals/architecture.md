# Architecture

## Modules

The `api` module contains public interfaces, records, enums, and result types. It has no implementation dependencies beyond compile-only Paper API types.

The `core` module implements lifecycle, registries, configuration, serialization, logging, localization, scheduler, player, command, permission, item, GUI, database, storage, cache, world, network, and advanced systems.

The `plugin` module bundles `api` and `core` into a standalone Paper/Purpur plugin and registers the core services.

Optional integration modules implement public integration contracts for Redis, Vault, BungeeCord, and Velocity.

## Bootstrap

`KehlLibBootstrap` owns:

- lifecycle modules
- a thread-safe `ServiceRegistry`
- a `ResourceManager`
- an internal `EventBus`

Call `register(Lifecycle)` for modules, call `enableAll()`, and call `close()` during plugin shutdown. If `enableAll()` fails, the bootstrap disables enabled modules and exposes the failure as `IllegalStateException`.

`close()` is idempotent and is also responsible for reverse lifecycle shutdown, the event bus, service registry, and resource manager.

## Services

Use `ServiceRegistry.register(Class<T>, T)` to expose an implementation through a public interface. `get(Class<T>)` returns `Optional<T>` and `getRequired(Class<T>)` throws `IllegalStateException` when a service is absent.

The standalone plugin registers the major APIs. An embedded bootstrap must register each implementation it wants to expose.

## Resources

`Resource` extends `AutoCloseable` but has `close()` as the no-checked-exception contract. A database, cache, storage, WebSocket service, integration, packet backend, or custom resource can be registered for deterministic shutdown.

## Results

`Result<T>` separates success values from failures. It provides `isSuccess()`, `value()`, `error()`, and `map(...)`. The HTTP, scheduler, packet, location, and database APIs use results for expected failures. Exceptions still remain for programming-contract violations, lifecycle misuse, and unrecoverable JVM or implementation failures.

## Public API compatibility

Do not depend on implementation-only classes from `core` if a public API contract exists. The implementation packages may change. Public Bukkit classes, however, are intentionally visible where the API is Paper/Bukkit-specific.
