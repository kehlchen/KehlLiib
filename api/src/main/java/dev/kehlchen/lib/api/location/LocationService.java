package dev.kehlchen.lib.api.location;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Location validation and ownership-safe teleport utilities. */
public interface LocationService extends Lifecycle {
    SafeLocation validate(Location location);
    SafeLocation findSafe(Location origin, int radius);
    Result<Boolean> teleport(Entity entity, Location location);
    Result<Boolean> teleport(Player player, Location location);
    Location parse(String value);
}
