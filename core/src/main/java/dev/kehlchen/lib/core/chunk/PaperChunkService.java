package dev.kehlchen.lib.core.chunk;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.chunk.ChunkService;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Chunk operations that avoid retaining stale references. */
public final class PaperChunkService implements ChunkService {
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private volatile boolean enabled;

    public PaperChunkService(Plugin plugin, PaperScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override public String name() { return "chunk"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { enabled = false; }

    @Override public Optional<Chunk> loaded(World world, int x, int z) {
        if (world == null || !world.isChunkLoaded(x, z)) return Optional.empty();
        return Optional.of(world.getChunkAt(x, z));
    }
    @Override public boolean isLoaded(World world, int x, int z) {
        return world != null && world.isChunkLoaded(x, z);
    }
    @Override public int chunkX(Location location) { return location == null ? 0 : location.getBlockX() >> 4; }
    @Override public int chunkZ(Location location) { return location == null ? 0 : location.getBlockZ() >> 4; }

    @Override
    public Result<Chunk> get(World world, int x, int z, boolean generate) {
        if (!enabled) return Result.failure(new IllegalStateException("Chunk service is disabled"));
        if (world == null) return Result.failure(new IllegalArgumentException("World is required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Chunk access must run on a region thread"));
        try {
            Chunk chunk = generate ? world.getChunkAt(x, z) : world.isChunkLoaded(x, z)
                    ? world.getChunkAt(x, z) : null;
            return chunk == null ? Result.failure(new IllegalStateException("Chunk is not loaded"))
                    : Result.success(chunk);
        } catch (Throwable failure) { return Result.failure(failure); }
    }

    @Override
    public CompletableFuture<Result<Chunk>> loadAsync(World world, int x, int z, boolean generate) {
        if (world == null) return CompletableFuture.completedFuture(Result.failure(new IllegalArgumentException("World is required")));
        if (!enabled) return CompletableFuture.completedFuture(Result.failure(new IllegalStateException("Chunk service is disabled")));
        try {
            Method method = world.getClass().getMethod("getChunkAtAsync", int.class, int.class, boolean.class);
            Object result = method.invoke(world, x, z, generate);
            if (result instanceof CompletableFuture<?> future) {
                return future.handle((value, failure) -> failure == null
                        ? value instanceof Chunk chunk ? Result.success(chunk)
                        : Result.failure(new IllegalStateException("Unexpected async chunk result"))
                        : Result.failure(failure));
            }
            return CompletableFuture.completedFuture(Result.failure(
                    new UnsupportedOperationException("Async chunk loading is unsupported")));
        } catch (NoSuchMethodException ignored) {
            return CompletableFuture.completedFuture(Result.failure(
                    new UnsupportedOperationException("Async chunk loading is unavailable")));
        } catch (Throwable failure) {
            return CompletableFuture.completedFuture(Result.failure(failure));
        }
    }

    @Override
    public Result<Boolean> unload(World world, int x, int z, boolean save) {
        if (world == null) return Result.failure(new IllegalArgumentException("World is required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Chunk unload must run on a region thread"));
        try { return Result.success(world.unloadChunk(x, z, save)); }
        catch (Throwable failure) { return Result.failure(failure); }
    }
}
