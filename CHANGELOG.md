# Changelog

## Phase 9 - Stabilization and release preparation

### Stabilization

- Standalone plugin enable failures now clean up partially initialized modules and resources before the plugin is disabled.
- Bootstrap, resource, and service lifecycle operations are synchronized and reject late registration after shutdown.
- Repeated resource and service shutdown is safe and does not repeat cleanup.
- WebSocket connections that close before opening now complete their initial connection future with an explicit failure.
- Performance snapshots report unavailable process CPU load as `-1` instead of presenting `0` as a measured value.

### Compatibility and limitations

- Documented support targets Java 25 and Paper/Purpur APIs through the configured Paper API version.
- Runtime compatibility with specific Paper, Purpur, and Folia servers remains deployment-specific and should be smoke-tested on the target server.
- Packet interception/modification and virtual packet delivery require a compatible optional backend.
- Folia support is limited to APIs and implementations that use region/entity ownership; no broad "fully Folia compatible" claim is made.

### Release artifacts

- The `kehllib-api`, `kehllib-core`, standalone `kehllib`, and example plugin artifacts are built and publishable as separate Gradle modules.
- Optional Redis, Vault, BungeeCord, and Velocity integrations remain separate modules and are not forced into unrelated artifacts.
