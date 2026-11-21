package dev.kehlchen.lib.api.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.List;

/** Lightweight world-aware region abstraction. */
public interface Region {
    World world();
    boolean contains(Location location);
    boolean contains(double x, double y, double z);
    boolean contains(Block block);
    boolean overlaps(Region other);
    Region expand(double amount);
    Region contract(double amount);
    Region translate(double x, double y, double z);
    double volume();
    Collection<Block> blocks(int limit);
    Collection<Entity> entities(int limit);
    Collection<org.bukkit.entity.Player> players(int limit);
    List<String> serialize();
}
