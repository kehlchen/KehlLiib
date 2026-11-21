package dev.kehlchen.lib.api.virtual;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;

import java.util.UUID;

/** Optional backend for packet-provided virtual entities. */
public interface VirtualEntityBackend extends Lifecycle {
    String id();
    Result<Void> spawn(VirtualEntity entity, UUID... viewers);
    Result<Void> update(VirtualEntity entity, UUID... viewers);
    Result<Void> remove(VirtualEntity entity, UUID... viewers);
}
