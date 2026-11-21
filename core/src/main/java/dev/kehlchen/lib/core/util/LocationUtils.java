package dev.kehlchen.lib.core.util;

import org.bukkit.Location;

import java.util.Locale;
import org.bukkit.Bukkit;

public final class LocationUtils {
    private LocationUtils() {}
    public static Location center(Location location) {
        Location copy = location.clone();
        copy.setX(Math.floor(copy.getX()) + 0.5);
        copy.setZ(Math.floor(copy.getZ()) + 0.5);
        copy.setY(Math.floor(copy.getY()) + 0.5);
        return copy;
    }
    public static double distanceSquared(Location a, Location b) {
        Preconditions.checkNotNull(a, "a");
        Preconditions.checkNotNull(b, "b");
        return a.distanceSquared(b);
    }
    public static String serialize(Location location) {
        if (location == null) return "";
        String world = location.getWorld() == null ? "" : location.getWorld().getName();
        return String.format(Locale.ROOT, "%s,%.4f,%.4f,%.4f,%.4f,%.4f",
                world, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
    public static boolean sameBlock(Location a, Location b) {
        return a != null && b != null && a.getWorld() != null && b.getWorld() != null
                && a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }
    public static int blockX(Location location) { return location == null ? 0 : location.getBlockX(); }
    public static int blockY(Location location) { return location == null ? 0 : location.getBlockY(); }
    public static int blockZ(Location location) { return location == null ? 0 : location.getBlockZ(); }
    public static int chunkX(Location location) { return location == null ? 0 : location.getBlockX() >> 4; }
    public static int chunkZ(Location location) { return location == null ? 0 : location.getBlockZ() >> 4; }
    public static boolean isFinite(Location location) {
        return location != null && Double.isFinite(location.getX())
                && Double.isFinite(location.getY()) && Double.isFinite(location.getZ());
    }
    public static Location parse(String value) {
        if (value == null) throw new IllegalArgumentException("location");
        String[] parts = value.trim().split(",");
        if (parts.length < 4) throw new IllegalArgumentException("Expected world,x,y,z[,yaw,pitch]");
        org.bukkit.World world = Bukkit.getWorld(parts[0].trim());
        if (world == null) throw new IllegalArgumentException("Unknown or unloaded world");
        double x = Double.parseDouble(parts[1].trim());
        double y = Double.parseDouble(parts[2].trim());
        double z = Double.parseDouble(parts[3].trim());
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0f;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0f;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
