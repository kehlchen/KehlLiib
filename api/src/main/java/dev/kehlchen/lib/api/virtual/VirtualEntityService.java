package dev.kehlchen.lib.api.virtual;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface VirtualEntityService extends Lifecycle {
    Optional<VirtualEntityBackend> backend();
    Result<VirtualEntity> create(VirtualEntityConfig config, UUID... viewers);
    Result<Void> update(VirtualEntity entity);
    Result<Void> remove(UUID id);
    Optional<VirtualEntity> find(UUID id);
    Collection<VirtualEntity> entities();
    void registerBackend(VirtualEntityBackend backend);
    void unregisterBackend(String backendId);
    int maximumEntities();
}
