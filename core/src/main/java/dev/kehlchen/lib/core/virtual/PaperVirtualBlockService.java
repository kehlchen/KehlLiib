package dev.kehlchen.lib.core.virtual;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.scheduler.Scheduler;
import dev.kehlchen.lib.api.virtual.VirtualBlockService;
import dev.kehlchen.lib.api.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public final class PaperVirtualBlockService implements VirtualBlockService {
    private final Plugin plugin;
    private final Scheduler scheduler;
    private final int maximumBlocks;
    private final Map<BlockKey, BlockState> blocks = new ConcurrentHashMap<>();
    private TaskHandle expirationTask;
    private volatile boolean enabled;

    public PaperVirtualBlockService(Plugin plugin, Scheduler scheduler, int maximumBlocks) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.maximumBlocks = Math.max(1, maximumBlocks);
    }

    @Override public String name() { return "virtual-block-service"; }
    @Override public String version() { return "1.0"; }
    @Override public synchronized void onEnable() {
        enabled = true;
        expirationTask = scheduler.repeatSync(plugin, Duration.ofSeconds(1), Duration.ofSeconds(1),
                this::restoreExpired).value().orElse(null);
    }

    @Override public synchronized void onDisable() {
        enabled = false;
        if (expirationTask != null) expirationTask.close();
        clear();
    }

    @Override public int maximumBlocks() { return maximumBlocks; }

    @Override
    public Result<Void> set(Location location, BlockData data, UUID... viewers) {
        return set(location, data, Duration.ZERO, viewers);
    }

    @Override
    public Result<Void> set(Location location, BlockData data, Duration expiresAfter, UUID... viewers) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(data, "data");
        if (!enabled) return Result.failure(new IllegalStateException("Virtual block service is disabled"));
        if (blocks.size() >= maximumBlocks && !blocks.containsKey(BlockKey.of(location))) {
            return Result.failure(new IllegalStateException("Virtual block limit reached"));
        }
        Instant expiresAt = expiresAfter == null || expiresAfter.isZero()
                ? null : Instant.now().plus(expiresAfter);
        Set<UUID> viewersSet = viewers == null ? Set.of()
                : Set.copyOf(Arrays.asList(viewers));
        BlockState previous = blocks.put(BlockKey.of(location),
                new BlockState(location.clone(), data.clone(), expiresAt, viewersSet));
        if (previous == null) previous = null;
        for (UUID playerId : (viewers == null ? new UUID[0] : viewers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                scheduler.runForEntity(plugin, player,
                        () -> player.sendBlockChange(location, data)).value();
            }
        }
        return Result.success(null);
    }

    @Override
    public Result<Void> restore(Location location, UUID... viewers) {
        Objects.requireNonNull(location, "location");
        BlockState removed = blocks.remove(BlockKey.of(location));
        UUID[] targets = viewers == null || viewers.length == 0
                ? removed == null ? new UUID[0] : removed.viewers().toArray(UUID[]::new)
                : viewers;
        for (UUID playerId : targets) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                scheduler.runForEntity(plugin, player,
                        () -> player.sendBlockChange(location, location.getBlock().getBlockData())).value();
            }
        }
        return Result.success(null);
    }

    @Override
    public Collection<Location> locations(UUID playerId) {
        return blocks.values().stream()
                .filter(state -> state.viewers().isEmpty() || state.viewers().contains(playerId))
                .map(state -> state.location().clone())
                .toList();
    }

    @Override
    public synchronized void clear() {
        for (BlockState state : blocks.values()) {
            Location location = state.location();
            for (UUID playerId : state.viewers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    scheduler.runForEntity(plugin, player,
                            () -> player.sendBlockChange(location, location.getBlock().getBlockData())).value();
                }
            }
        }
        blocks.clear();
    }

    private void restoreExpired() {
        Instant now = Instant.now();
        for (Map.Entry<BlockKey, BlockState> entry : blocks.entrySet()) {
            if (entry.getValue().expiresAt() != null && entry.getValue().expiresAt().isBefore(now)) {
                restore(entry.getValue().location());
            }
        }
    }

    private record BlockKey(String world, int x, int y, int z) {
        static BlockKey of(Location location) {
            return new BlockKey(location.getWorld().getName(), location.getBlockX(),
                    location.getBlockY(), location.getBlockZ());
        }
    }

    private record BlockState(Location location, BlockData data, Instant expiresAt,
                              Set<UUID> viewers) {}
}
