package dev.kehlchen.lib.core.particle;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.particle.ParticleService;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

/** Location-owned, bounded particle spawning. */
public final class PaperParticleService implements ParticleService {
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private volatile boolean enabled;

    public PaperParticleService(Plugin plugin, PaperScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }
    @Override public String name() { return "particle"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { enabled = false; }

    @Override
    public Result<Integer> spawn(Location location, Particle particle, int count,
                                 double offsetX, double offsetY, double offsetZ, double speed) {
        if (!enabled) return Result.failure(new IllegalStateException("Particle service is disabled"));
        if (location == null || location.getWorld() == null || particle == null)
            return Result.failure(new IllegalArgumentException("Location and particle are required"));
        int bounded = Math.min(10_000, Math.max(0, count));
        if (!scheduler.isMainThread())
            return Result.failure(new IllegalStateException("Particle spawning must run on a region thread"));
        try {
            location.getWorld().spawnParticle(particle, location, bounded, offsetX, offsetY, offsetZ, speed);
            return Result.success(bounded);
        } catch (Throwable failure) { return Result.failure(failure); }
    }

    @Override
    public Result<Integer> spawn(Collection<Player> players, Location location, Particle particle,
                                 int count, double offsetX, double offsetY,
                                 double offsetZ, double speed) {
        if (!enabled) return Result.failure(new IllegalStateException("Particle service is disabled"));
        if (players == null || location == null || location.getWorld() == null || particle == null)
            return Result.failure(new IllegalArgumentException("Players, location and particle are required"));
        int bounded = Math.min(10_000, Math.max(0, count));
        if (!scheduler.isMainThread())
            return Result.failure(new IllegalStateException("Particle spawning must run on a region thread"));
        try {
            for (Player player : players) {
                if (player != null && player.isOnline()) {
                    player.spawnParticle(particle, location, bounded,
                            offsetX, offsetY, offsetZ, speed);
                }
            }
            return Result.success(bounded);
        } catch (Throwable failure) { return Result.failure(failure); }
    }
}
