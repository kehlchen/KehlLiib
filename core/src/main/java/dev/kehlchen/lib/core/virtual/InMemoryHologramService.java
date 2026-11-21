package dev.kehlchen.lib.core.virtual;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.virtual.Hologram;
import dev.kehlchen.lib.api.virtual.HologramConfig;
import dev.kehlchen.lib.api.virtual.HologramService;
import dev.kehlchen.lib.api.virtual.VirtualEntity;
import dev.kehlchen.lib.api.virtual.VirtualEntityConfig;
import dev.kehlchen.lib.api.virtual.VirtualEntityService;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryHologramService implements HologramService {
    private final VirtualEntityService entities;
    private final Map<UUID, HologramImpl> holograms = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    public InMemoryHologramService(VirtualEntityService entities) {
        this.entities = entities;
    }

    @Override public String name() { return "hologram-service"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public synchronized void onDisable() {
        enabled = false;
        for (Hologram hologram : holograms.values()) hologram.remove();
        holograms.clear();
    }
    @Override public Collection<Hologram> holograms() { return List.copyOf(holograms.values()); }

    @Override
    public Result<Hologram> create(HologramConfig config) {
        if (!enabled) return Result.failure(new IllegalStateException("Hologram service is disabled"));
        if (entities.backend().isEmpty()) return Result.failure(new UnsupportedOperationException(
                "Virtual holograms require a virtual entity packet backend"));
        HologramImpl hologram = new HologramImpl(UUID.randomUUID(), config);
        holograms.put(hologram.id(), hologram);
        Result<Void> redraw = hologram.redraw();
        if (!redraw.isSuccess()) {
            holograms.remove(hologram.id());
            return Result.failure(redraw.error().orElseThrow());
        }
        return Result.success(hologram);
    }

    @Override
    public Result<Void> remove(UUID id) {
        HologramImpl hologram = holograms.remove(id);
        if (hologram == null) return Result.success(null);
        return hologram.removeResult();
    }

    private final class HologramImpl implements Hologram {
        private final UUID id;
        private volatile Location location;
        private volatile List<String> lines;
        private final double spacing;
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        private final Map<Integer, UUID> lineEntities = new ConcurrentHashMap<>();
        private final boolean global;

        private HologramImpl(UUID id, HologramConfig config) {
            this.id = id;
            this.location = config.location().clone();
            this.lines = config.lines();
            this.spacing = config.lineSpacing();
            this.global = config.global();
        }

        @Override public UUID id() { return id; }
        @Override public Location location() { return location.clone(); }
        @Override public List<String> lines() { return List.copyOf(lines); }

        @Override public synchronized void setLines(List<String> lines) {
            this.lines = lines == null ? List.of() : List.copyOf(lines);
            redraw();
        }

        @Override public synchronized void setLocation(Location location) {
            this.location = location.clone();
            redraw();
        }

        @Override public void showTo(UUID playerId) {
            viewers.add(playerId);
            redraw();
        }

        @Override public void hideFrom(UUID playerId) {
            viewers.remove(playerId);
            redraw();
        }

        @Override public void remove() { removeResult(); }

        private Result<Void> removeResult() {
            for (UUID entityId : new ArrayList<>(lineEntities.values())) {
                Result<Void> removed = entities.remove(entityId);
                if (!removed.isSuccess()) return removed;
            }
            lineEntities.clear();
            holograms.remove(id);
            return Result.success(null);
        }

        private Result<Void> redraw() {
            for (UUID entityId : lineEntities.values()) entities.remove(entityId);
            lineEntities.clear();
            UUID[] targets = global ? new UUID[0] : viewers.toArray(UUID[]::new);
            for (int i = 0; i < lines.size(); i++) {
                Location lineLocation = location.clone().add(0, -i * spacing, 0);
                VirtualEntityConfig config = VirtualEntityConfig.builder(lineLocation, EntityType.ARMOR_STAND)
                        .name(lines.get(i)).visible(true).build();
                Result<VirtualEntity> created = entities.create(config, targets);
                if (!created.isSuccess()) return Result.failure(created.error().orElseThrow());
                lineEntities.put(i, created.value().orElseThrow().id());
            }
            return Result.success(null);
        }
    }
}
