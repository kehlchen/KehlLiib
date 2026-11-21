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

/** Lightweight spherical region. */
public record SphereRegion(World world, double centerX, double centerY, double centerZ,
                           double radius) implements Region {
    public SphereRegion {
        Objects.requireNonNull(world, "world");
        if (!Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("Radius must be finite and non-negative");
    }
    @Override public boolean contains(Location location) {
        return location != null && world.equals(location.getWorld())
                && contains(location.getX(), location.getY(), location.getZ());
    }
    @Override public boolean contains(double x, double y, double z) {
        double dx = x - centerX, dy = y - centerY, dz = z - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
    @Override public boolean contains(Block block) {
        return block != null && world.equals(block.getWorld())
                && contains(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
    }
    @Override public boolean overlaps(Region other) {
        if (other == null || !world.equals(other.world())) return false;
        if (other instanceof SphereRegion sphere) {
            double dx = centerX-sphere.centerX, dy=centerY-sphere.centerY, dz=centerZ-sphere.centerZ;
            return dx*dx+dy*dy+dz*dz <= (radius+sphere.radius)*(radius+sphere.radius);
        }
        if (other instanceof CuboidRegion box) {
            double dx = Math.max(box.minX()-centerX, 0);
            dx = Math.max(dx, centerX-box.maxX());
            double dy = Math.max(Math.max(box.minY()-centerY, 0), centerY-box.maxY());
            double dz = Math.max(Math.max(box.minZ()-centerZ, 0), centerZ-box.maxZ());
            return dx*dx+dy*dy+dz*dz <= radius*radius;
        }
        return false;
    }
    @Override public Region expand(double amount) { return new SphereRegion(world, centerX, centerY, centerZ, radius + amount); }
    @Override public Region contract(double amount) { return new SphereRegion(world, centerX, centerY, centerZ, Math.max(0, radius - amount)); }
    @Override public Region translate(double x, double y, double z) {
        return new SphereRegion(world, centerX+x, centerY+y, centerZ+z, radius);
    }
    @Override public double volume() { return 4.0 / 3.0 * Math.PI * radius * radius * radius; }
    @Override public Collection<Block> blocks(int limit) {
        List<Block> result = new ArrayList<>();
        int r = (int) Math.ceil(radius), maximum = Math.max(0, limit);
        outer: for (int x=(int)Math.floor(centerX-r); x<=Math.ceil(centerX+r); x++)
            for (int y=(int)Math.floor(centerY-r); y<=Math.ceil(centerY+r); y++)
                for (int z=(int)Math.floor(centerZ-r); z<=Math.ceil(centerZ+r); z++) {
                    if (result.size() >= maximum) break outer;
                    if (contains(x+0.5,y+0.5,z+0.5) && world.isChunkLoaded(x>>4,z>>4))
                        result.add(world.getBlockAt(x,y,z));
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
        return List.of(String.format(Locale.ROOT, "sphere:%s:%.4f,%.4f,%.4f:%.4f",
                world.getName(), centerX, centerY, centerZ, radius));
    }
}
