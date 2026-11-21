# Advanced Systems

## Packets

`PacketService` is a backend-neutral abstraction for packet events and sending. `PacketBackend` implementations can be registered explicitly.

Paper/Bukkit alone cannot intercept or modify packets. If no backend is registered, packet operations fail with a clear unsupported operation result or exception according to the operation. Compile-time availability is not runtime packet support.

## Virtual entities and holograms

`VirtualEntityService`, `VirtualBlockService`, and `HologramService` manage virtual state. `VirtualEntityConfig`, `VirtualEntityBackend`, `VirtualBlockService`, and `HologramConfig` describe limits and behavior.

Fake entity/hologram delivery requires an optional tested virtual entity backend. The public API does not expose internal Minecraft/NMS types.

Fake block changes use `Player.sendBlockChange`, are bounded, do not change the real world, and restore automatically or expire according to service configuration.

```java
VirtualEntityService entities =
    runtime.services().getRequired(VirtualEntityService.class);
HologramService holograms =
    runtime.services().getRequired(HologramService.class);
```

## Performance monitoring

`PerformanceMonitor` returns `PerformanceSnapshot`, including TPS/MSPT and process load when exposed by the server. TPS/MSPT are `OptionalDouble`; unavailable values remain empty.

CPU process load is `-1` when unavailable and otherwise in the `0` to `100` range.

## Profiling

`Profiler` is an opt-in, lifecycle-managed recorder.

```java
Profiler profiler = runtime.services().getRequired(Profiler.class);
profiler.setEnabled(true);
profiler.measure("database-refresh", this::refreshDatabase);
ProfileSnapshot snapshot = profiler.snapshot();
```

## Metrics and diagnostics

`Metrics` records counters, gauges, histograms, or timers using `MetricType` and returns `MetricSnapshot`.

`Diagnostics` creates `DiagnosticReport` output. `ServerAdapterRegistry` and `ServerAdapter` isolate exact-version server adapter support.

An adapter is enabled only when it advertises support for the detected server version. Do not assume NMS types are safe public API.
