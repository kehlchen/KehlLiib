package dev.kehlchen.lib.core.serialization;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class LocationSerializer implements Serializer<Location> {
    @Override public String serialize(Location value) {
        if (value == null) return "";
        return (value.getWorld() == null ? "" : value.getWorld().getName()) + ";" + value.getX() + ";" + value.getY() + ";" + value.getZ()
                + ";" + value.getYaw() + ";" + value.getPitch();
    }
    @Override public Location deserialize(String data) {
        try {
            String[] parts = data.split(";");
            if (parts.length != 6) throw new IllegalArgumentException();
            World world = parts[0].isEmpty() ? null : Bukkit.getWorld(parts[0]);
            if (world == null) throw new IllegalArgumentException("World not found: " + parts[0]);
            return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid location: " + data, e); }
    }
}
