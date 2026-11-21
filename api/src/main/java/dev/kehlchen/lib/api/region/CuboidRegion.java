package dev.kehlchen.lib.api.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Bounded axis-aligned cuboid region. */
public record CuboidRegion(World world, double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ) implements Region {
    public CuboidRegion {
        Objects.requireNonNull(world, "world");
        if (minX > maxX) { double x = minX; minX = maxX; maxX = x; }
        if (minY > maxY) { double y = minY; minY = maxY; maxY = y; }
        if (minZ > maxZ) { double z = minZ; minZ = maxZ; maxZ = z; }
    }

    public static CuboidRegion of(Location a, Location b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        if (a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld()))
            throw new IllegalArgumentException("Both locations must share a loaded world");
        return new CuboidRegion(a.getWorld(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    @Override public boolean contains(Location location) {
        return location != null && location.getWorld() != null && location.getWorld().equals(world)
                && contains(location.getX(), location.getY(), location.getZ());
    }
    @Override public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
    @Override public boolean contains(Block block) {
        return block != null && block.getWorld().equals(world)
                && contains(block.getX(), block.getY(), block.getZ());
    }
    @Override public boolean overlaps(Region other) {
        if (other == null || !world.equals(other.world())) return false;
        if (other instanceof CuboidRegion box) {
            return minX <= box.maxX && maxX >= box.minX
                    && minY <= box.maxY && maxY >= box.minY
                    && minZ <= box.maxZ && maxZ >= box.minZ;
        }
        if (other instanceof SphereRegion sphere) return sphere.overlaps(this);
        if (other instanceof CylinderRegion cylinder) return cylinder.overlaps(this);
        return false;
    }
    @Override public Region expand(double amount) {
        return new CuboidRegion(world, minX - amount, minY - amount, minZ - amount,
                maxX + amount, maxY + amount, maxZ + amount);
    }
    @Override public Region contract(double amount) {
        double expansion = -Math.max(0, amount);
        return expand(expansion);
    }
    @Override public Region translate(double x, double y, double z) {
        return new CuboidRegion(world, minX + x, minY + y, minZ + z,
                maxX + x, maxY + y, maxZ + z);
    }
    @Override public double volume() { return (maxX - minX) * (maxY - minY) * (maxZ - minZ); }
    @Override public Collection<Block> blocks(int limit) {
        List<Block> result = new ArrayList<>();
        int maximum = Math.max(0, limit);
        int sx = (int) Math.floor(minX), ex = (int) Math.floor(maxX);
        int sy = (int) Math.floor(minY), ey = (int) Math.floor(maxY);
        int sz = (int) Math.floor(minZ), ez = (int) Math.floor(maxZ);
        outer: for (int x = sx; x <= ex; x++) for (int y = sy; y <= ey; y++) for (int z = sz; z <= ez; z++) {
            if (result.size() >= maximum) break outer;
            if (world.isChunkLoaded(x >> 4, z >> 4)) result.add(world.getBlockAt(x, y, z));
        }
        return result;
    }
    @Override public Collection<Entity> entities(int limit) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            if (result.size() >= limit) break;
            if (contains(entity.getLocation())) result.add(entity);
        }
        return result;
    }
    @Override public Collection<Player> players(int limit) {
        List<Player> result = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (result.size() >= limit) break;
            if (contains(player.getLocation())) result.add(player);
        }
        return result;
    }
    @Override public List<String> serialize() {
        return List.of(String.format(Locale.ROOT, "cuboid:%s:%.4f,%.4f,%.4f:%.4f,%.4f,%.4f",
                world.getName(), minX, minY, minZ, maxX, maxY, maxZ));
    }
}
