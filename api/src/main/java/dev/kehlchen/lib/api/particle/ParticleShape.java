package dev.kehlchen.lib.api.particle;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/** Pure mathematical particle shape generators for server-side scheduling. */
public final class ParticleShape {
    private ParticleShape() {}

    public static List<Location> circle(Location center, double radius, int points) {
        return ring(center, radius, points, 0);
    }
    public static List<Location> ring(Location center, double radius, int points, double height) {
        List<Location> locations = new ArrayList<>();
        int count = Math.max(1, points);
        for (int i = 0; i < count; i++) {
            double angle = Math.TAU * i / count;
            locations.add(center.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius));
        }
        return locations;
    }
    public static List<Location> sphere(Location center, double radius, int density) {
        List<Location> locations = new ArrayList<>();
        int count = Math.max(1, density);
        if (count == 1) {
            locations.add(center.clone());
            return locations;
        }
        for (int i = 0; i < count; i++) {
            double y = 1.0 - (2.0 * i / (count - 1.0));
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double angle = Math.PI * (3 - Math.sqrt(5)) * i;
            locations.add(center.clone().add(r * Math.cos(angle) * radius, y * radius,
                    r * Math.sin(angle) * radius));
        }
        return locations;
    }
    public static List<Location> line(Location from, Location to, int points) {
        List<Location> locations = new ArrayList<>();
        int count = Math.max(2, points);
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            locations.add(new Location(from.getWorld(),
                    from.getX() + (to.getX() - from.getX()) * t,
                    from.getY() + (to.getY() - from.getY()) * t,
                    from.getZ() + (to.getZ() - from.getZ()) * t));
        }
        return locations;
    }
    public static List<Location> helix(Location center, double radius, double height, int points, int turns) {
        List<Location> locations = new ArrayList<>();
        int count = Math.max(1, points);
        int cycle = Math.max(1, turns);
        for (int i = 0; i < count; i++) {
            double t = (double) i / Math.max(1, count - 1);
            double angle = Math.TAU * cycle * t;
            locations.add(center.clone().add(Math.cos(angle) * radius, height * t,
                    Math.sin(angle) * radius));
        }
        return locations;
    }
}
