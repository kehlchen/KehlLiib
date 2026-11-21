package dev.kehlchen.lib.core.location;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.location.LocationService;
import dev.kehlchen.lib.api.location.SafeLocation;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/** Location parsing, validation and safe teleport integration. */
public final class PaperLocationService implements LocationService {
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private volatile boolean enabled;

    public PaperLocationService(Plugin plugin, PaperScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }
    @Override public String name() { return "location"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { enabled = false; }

    @Override public SafeLocation validate(Location location) {
        if (location == null) return SafeLocation.unsafe(null, "Location is null");
        if (location.getWorld() == null) return SafeLocation.unsafe(location, "Location has no world");
        if (!Double.isFinite(location.getX()) || !Double.isFinite(location.getY()) || !Double.isFinite(location.getZ()))
            return SafeLocation.unsafe(location, "Location coordinates are not finite");
        if (location.getBlockY() < location.getWorld().getMinHeight()
                || location.getBlockY() >= location.getWorld().getMaxHeight())
            return SafeLocation.unsafe(location, "Location is outside world height");
        return SafeLocation.safe(location);
    }

    @Override public SafeLocation findSafe(Location origin, int radius) {
        SafeLocation base = validate(origin);
        if (!base.safe()) return base;
        int r = Math.min(64, Math.max(0, radius));
        for (int y = r; y >= -r; y--)
            for (int x = -r; x <= r; x++)
                for (int z = -r; z <= r; z++) {
                    Location candidate = origin.clone().add(x, y, z);
                    SafeLocation checked = validate(candidate);
                    if (checked.safe()
                            && !candidate.getBlock().isPassable()
                            && candidate.clone().add(0, 1, 0).getBlock().isPassable()
                            && candidate.clone().subtract(0, 1, 0).getBlock().isSolid()) {
                        return SafeLocation.safe(candidate);
                    }
                }
        return SafeLocation.unsafe(origin, "No safe location found within radius");
    }

    @Override public Result<Boolean> teleport(Entity entity, Location location) {
        if (entity == null || location == null) return Result.failure(new IllegalArgumentException("Entity and location are required"));
        if (!entity.isValid()) return Result.failure(new IllegalStateException("Entity is no longer valid"));
        if (!validate(location).safe()) return Result.failure(new IllegalArgumentException("Unsafe location"));
        Result<?> scheduled = scheduler.runForEntity(plugin, entity,
                () -> entity.teleport(location));
        if (!scheduled.isSuccess()) return Result.failure(scheduled.error().orElseThrow());
        return Result.success(entity.isValid());
    }

    @Override public Result<Boolean> teleport(Player player, Location location) {
        if (player == null || !player.isOnline()) return Result.failure(new IllegalStateException("Player is offline"));
        Result<TaskResult> unused = Result.success(null);
        Result<?> scheduled = scheduler.runForEntity(plugin, player, () -> player.teleport(location));
        if (!scheduled.isSuccess()) return Result.failure(scheduled.error().orElseThrow());
        return Result.success(player.isOnline());
    }

    @Override public Location parse(String value) {
        if (value == null) throw new IllegalArgumentException("Location is required");
        String[] parts = value.trim().split(",");
        if (parts.length < 4) throw new IllegalArgumentException("Expected world,x,y,z[,yaw,pitch]");
        String worldName = parts[0].trim();
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) throw new IllegalArgumentException("Unknown or unloaded world: " + worldName);
        double x = Double.parseDouble(parts[1].trim());
        double y = Double.parseDouble(parts[2].trim());
        double z = Double.parseDouble(parts[3].trim());
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0f;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0f;
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Location coordinates must be finite");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private record TaskResult() {}
}
