package dev.kehlchen.lib.api.particle;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Bounded location and player particle effects. */
public interface ParticleService extends Lifecycle {
    Result<Integer> spawn(Location location, Particle particle, int count, double offsetX,
                          double offsetY, double offsetZ, double speed);
    Result<Integer> spawn(Collection<Player> players, Location location, Particle particle, int count,
                          double offsetX, double offsetY, double offsetZ, double speed);
}
