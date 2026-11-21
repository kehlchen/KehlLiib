package dev.kehlchen.lib.core.virtual;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.virtual.VirtualEntity;
import dev.kehlchen.lib.api.virtual.VirtualEntityBackend;
import dev.kehlchen.lib.api.virtual.VirtualEntityConfig;
import dev.kehlchen.lib.api.virtual.VirtualEntityService;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PaperVirtualEntityService implements VirtualEntityService {
    private final ConcurrentHashMap<UUID, VirtualEntity> entities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, VirtualEntityBackend> backends = new ConcurrentHashMap<>();
    private final int maximumEntities;
    private volatile VirtualEntityBackend active;
    private volatile boolean enabled;

    public PaperVirtualEntityService(int maximumEntities) {
        this.maximumEntities = Math.max(1, maximumEntities);
    }

    @Override public String name() { return "virtual-entity-service"; }
    @Override public String version() { return "1.0"; }
    @Override public synchronized void onEnable() { enabled = true; }
    @Override public synchronized void onDisable() {
        enabled = false;
        for (VirtualEntity entity : entities.values()) {
            if (active != null) active.remove(entity);
        }
        entities.clear();
        for (VirtualEntityBackend backend : backends.values()) backend.onDisable();
        backends.clear();
        active = null;
    }

    @Override public Optional<VirtualEntityBackend> backend() { return Optional.ofNullable(active); }
    @Override public int maximumEntities() { return maximumEntities; }
    @Override public Collection<VirtualEntity> entities() { return entities.values().stream().toList(); }
    @Override public Optional<VirtualEntity> find(UUID id) { return Optional.ofNullable(entities.get(id)); }

    @Override
    public Result<VirtualEntity> create(VirtualEntityConfig config, UUID... viewers) {
        if (!enabled) return Result.failure(new IllegalStateException("Virtual entity service is disabled"));
        if (active == null) return Result.failure(new UnsupportedOperationException(
                "No virtual entity backend is registered"));
        if (entities.size() >= maximumEntities) return Result.failure(
                new IllegalStateException("Virtual entity limit reached"));
        UUID id = UUID.randomUUID();
        VirtualEntity entity = new VirtualEntityImpl(id, config.location(), config.name(),
                config.equipment(), config.visible(), config);
        Result<Void> spawned = active.spawn(entity, viewers);
        if (!spawned.isSuccess()) return Result.failure(spawned.error().orElseThrow());
        entities.put(id, entity);
        return Result.success(entity);
    }

    @Override
    public Result<Void> update(VirtualEntity entity) {
        if (active == null) return Result.failure(new UnsupportedOperationException(
                "No virtual entity backend is registered"));
        entities.put(entity.id(), entity);
        return active.update(entity);
    }

    @Override
    public Result<Void> remove(UUID id) {
        VirtualEntity entity = entities.remove(id);
        if (entity == null) return Result.success(null);
        if (active == null) return Result.success(null);
        return active.remove(entity);
    }

    @Override
    public synchronized void registerBackend(VirtualEntityBackend backend) {
        backends.put(backend.id(), backend);
        active = backend;
        backend.onEnable();
    }

    @Override
    public synchronized void unregisterBackend(String backendId) {
        VirtualEntityBackend removed = backends.remove(backendId);
        if (removed == null) return;
        removed.onDisable();
        if (active == removed) active = backends.values().stream().findFirst().orElse(null);
    }
}
