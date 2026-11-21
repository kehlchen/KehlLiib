package dev.kehlchen.lib.core.util;

import org.bukkit.Location;

import java.util.Locale;

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
}
