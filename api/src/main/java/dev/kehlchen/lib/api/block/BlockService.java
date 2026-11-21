package dev.kehlchen.lib.api.block;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.Material;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Bounded block lookup and modification utilities. */
public interface BlockService extends Lifecycle {
    Result<Block> at(Location location);
    Result<Block> relative(Block block, int x, int y, int z);
    Result<BlockState> setType(Location location, Material material);
    Result<BlockState> setType(Location location, Material material, boolean applyPhysics);
    Result<Boolean> breakNaturally(Location location);
    boolean isSafeLocation(Location location);
    Optional<org.bukkit.Location> findSafe(Location origin, int radius);
    Collection<Block> nearby(Location origin, int radius, Predicate<Block> filter, int limit);
    Collection<Block> inCuboid(Location first, Location second, Predicate<Block> filter, int limit);
    long count(Location first, Location second, Predicate<Block> filter, int limit);
    List<Block> validate(List<Block> blocks);
}
