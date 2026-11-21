# Troubleshooting

## `getRequired(...)` throws `IllegalStateException`

Likely causes:

- the service was not registered
- the standalone plugin is unavailable
- embedded bootstrap initialization completed only partially
- an optional integration was not registered

Check `onEnable()` ordering, plugin dependencies, and `IntegrationRegistry`. Use `get(...)` for optional services.

## Plugin startup fails

The standalone plugin calls `bootstrap.close()` and disables itself after a runtime exception during enable. Inspect the server log for:

- missing Paper API
- lifecycle module enable failure
- invalid module registration
- resource construction failure

Fix the first exception in the log rather than retrying without changing the configuration.

## Database connection fails

Verify:

- `DatabaseType`
- JDBC URL
- credentials
- SQLite file path
- driver availability
- pool minimum and maximum values
- network firewall for remote databases

Use async database calls from server-facing code. Do not log passwords or JDBC URLs containing credentials.

## HTTP request fails

Check timeout, retry count, status handling, allowed hosts, DNS, private-address policy, TLS, and endpoint response body limits. A non-2xx status is a response. A `null` or failed `Result` means inspect `error()`.

## Integration unavailable

Redis, Vault, BungeeCord, and Velocity do not become available merely because their APIs are compiled.

Verify:

- integration artifact deployment
- external Redis/Vault/plugin availability
- integration registration
- correct runtime: Paper for Bukkit/Vault/Bungee, Velocity for Velocity
- `IntegrationRegistry` state

## Packet operations are unsupported

Packet interception requires a compatible backend. No backend is bundled. Verify backend registration and backend thread safety. UnsupportedPacket operations must be handled explicitly.

## Bukkit objects used on wrong thread

Likely causes include HTTP or WebSocket callbacks, Redis callbacks, database futures, or background tasks directly touching `World`, `Entity`, `Player`, or `Inventory`.

Return to the server through `Scheduler.runSync`, `runForLocation`, or `runForEntity`.

## Gradle dependency problems

Confirm:

- Java toolchain 25
- `mavenLocal()` for locally published modules
- the artifact coordinate and `1.0` version
- Paper repository
- JitPack repository for Vault

Run `.\gradlew.bat :api:dependencies`, `:core:dependencies`, or the equivalent consuming-project dependency report.

## Resource cleanup

Close every bootstrap in plugin `onDisable()`. Register databases, storages, caches, WebSockets, integrations, subscriptions, and packet/virtual resources that are not already owned by the bootstrap. Resolve close failures from server logs rather than suppressing them.
