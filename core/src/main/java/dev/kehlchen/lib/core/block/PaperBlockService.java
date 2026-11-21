package dev.kehlchen.lib.core.block;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.block.BlockService;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Bounded Paper block operations. */
public final class PaperBlockService implements BlockService {
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private volatile boolean enabled;

    public PaperBlockService(Plugin plugin, PaperScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }
    @Override public String name() { return "block"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { enabled = false; }

    @Override public Result<Block> at(Location location) {
        if (location == null || location.getWorld() == null)
            return Result.failure(new IllegalArgumentException("Location must have a world"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread block access"));
        try { return Result.success(location.getBlock()); } catch (Throwable failure) { return Result.failure(failure); }
    }
    @Override public Result<Block> relative(Block block, int x, int y, int z) {
        if (block == null) return Result.failure(new IllegalArgumentException("Block is required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread block access"));
        try { return Result.success(block.getRelative(x, y, z)); } catch (Throwable failure) { return Result.failure(failure); }
    }
    @Override public Result<BlockState> setType(Location location, Material material) {
        return setType(location, material, true);
    }
    @Override public Result<BlockState> setType(Location location, Material material, boolean applyPhysics) {
        if (location == null || location.getWorld() == null || material == null)
            return Result.failure(new IllegalArgumentException("Location and material are required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread block access"));
        try {
            Block block = location.getBlock();
            block.setType(material, applyPhysics);
            return Result.success(block.getState());
        } catch (Throwable failure) { return Result.failure(failure); }
    }
    @Override public Result<Boolean> breakNaturally(Location location) {
        if (location == null || location.getWorld() == null)
            return Result.failure(new IllegalArgumentException("Location is required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread block access"));
        try { location.getBlock().breakNaturally(); return Result.success(true); }
        catch (Throwable failure) { return Result.failure(failure); }
    }

    @Override public boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) return false;
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);
        return !feet.getType().isSolid() && !head.getType().isSolid()
                && ground.getType().isSolid() && ground.getType().getBlastResistance() < 100;
    }
    @Override public Optional<Location> findSafe(Location origin, int radius) {
        if (origin == null || origin.getWorld() == null) return Optional.empty();
        int r = Math.min(64, Math.max(0, radius));
        for (int y = r; y >= -r; y--) {
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                Location candidate = origin.clone().add(x, y, z);
                if (isSafeLocation(candidate)) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
    @Override public Collection<Block> nearby(Location origin, int radius, Predicate<Block> filter, int limit) {
        if (origin == null || origin.getWorld() == null) return List.of();
        int r = Math.min(64, Math.max(0, radius));
        int maximum = Math.min(100_000, Math.max(0, limit));
        return cuboid(origin.clone().add(-r, -r, -r), origin.clone().add(r, r, r), filter, maximum);
    }
    @Override public Collection<Block> inCuboid(Location first, Location second,
                                                 Predicate<Block> filter, int limit) {
        if (first == null || second == null || first.getWorld() == null
                || !first.getWorld().equals(second.getWorld())) return List.of();
        return cuboid(first, second, filter, limit);
    }
    @Override public long count(Location first, Location second, Predicate<Block> filter, int limit) {
        return inCuboid(first, second, filter, limit).size();
    }
    @Override public List<Block> validate(List<Block> blocks) {
        if (blocks == null) return List.of();
        List<Block> valid = new ArrayList<>();
        for (Block block : blocks) if (block != null && block.getWorld() != null) valid.add(block);
        return valid;
    }

    private Collection<Block> cuboid(Location first, Location second,
                                     Predicate<Block> filter, int limit) {
        int sx = Math.min(first.getBlockX(), second.getBlockX());
        int ex = Math.max(first.getBlockX(), second.getBlockX());
        int sy = Math.min(first.getBlockY(), second.getBlockY());
        int ey = Math.max(first.getBlockY(), second.getBlockY());
        int sz = Math.min(first.getBlockZ(), second.getBlockZ());
        int ez = Math.max(first.getBlockZ(), second.getBlockZ());
        int maximum = Math.min(100_000, Math.max(0, limit));
        List<Block> result = new ArrayList<>();
        outer: for (int x = sx; x <= ex && result.size() < maximum; x++)
            for (int y = sy; y <= ey && result.size() < maximum; y++)
                for (int z = sz; z <= ez && result.size() < maximum; z++) {
                    if (!first.getWorld().isChunkLoaded(x >> 4, z >> 4)) continue;
                    Block block = first.getWorld().getBlockAt(x, y, z);
                    if (filter == null || filter.test(block)) result.add(block);
                }
        return result;
    }
}
