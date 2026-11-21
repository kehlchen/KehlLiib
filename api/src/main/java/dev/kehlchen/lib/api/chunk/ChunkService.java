package dev.kehlchen.lib.api.chunk;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Chunk lookup and load/unload operations with bounded synchronous access. */
public interface ChunkService extends Lifecycle {
    Optional<Chunk> loaded(World world, int x, int z);
    Result<Chunk> get(World world, int x, int z, boolean generate);
    CompletableFuture<Result<Chunk>> loadAsync(World world, int x, int z, boolean generate);
    Result<Boolean> unload(World world, int x, int z, boolean save);
    boolean isLoaded(World world, int x, int z);
    int chunkX(Location location);
    int chunkZ(Location location);
}
