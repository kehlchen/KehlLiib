package dev.kehlchen.lib.core.entity;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.entity.EntityService;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/** UUID and validity-aware Paper entity operations. */
public final class PaperEntityService implements EntityService {
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private volatile boolean enabled;

    public PaperEntityService(Plugin plugin, PaperScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }
    @Override public String name() { return "entity"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { enabled = false; }

    @Override public Result<Entity> find(UUID uuid) {
        if (uuid == null) return Result.failure(new IllegalArgumentException("UUID is required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Entity lookup must run on a region thread"));
        Entity found = Bukkit.getEntity(uuid);
        return found == null ? Result.failure(new IllegalStateException("Entity not found: " + uuid))
                : Result.success(found);
    }
    @Override public Result<Entity> spawn(Location location, EntityType type) {
        if (!enabled) return Result.failure(new IllegalStateException("Entity service is disabled"));
        if (location == null || location.getWorld() == null || type == null)
            return Result.failure(new IllegalArgumentException("Location and entity type are required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Entity spawning must run on a region thread"));
        try { return Result.success(location.getWorld().spawnEntity(location, type)); }
        catch (Throwable failure) { return Result.failure(failure); }
    }
    @Override public Result<Boolean> remove(Entity entity) {
        if (entity == null) return Result.failure(new IllegalArgumentException("Entity is required"));
        if (!entity.isValid()) return Result.success(false);
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Entity removal must run on a region thread"));
        try { entity.remove(); return Result.success(true); }
        catch (Throwable failure) { return Result.failure(failure); }
    }
    @Override public Result<Boolean> teleport(Entity entity, Location location) {
        if (entity == null || location == null) return Result.failure(new IllegalArgumentException("Entity and location are required"));
        if (!entity.isValid()) return Result.success(false);
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Entity teleportation must run on a region thread"));
        try { return Result.success(entity.teleport(location)); }
        catch (Throwable failure) { return Result.failure(failure); }
    }
    @Override public boolean isValid(Entity entity) {
        return entity != null && entity.isValid() && !entity.isDead();
    }
    @Override public Collection<Entity> nearby(Location origin, double radius,
                                               Predicate<Entity> filter, int limit) {
        if (origin == null || origin.getWorld() == null) return java.util.List.of();
        double r = Math.min(256, Math.max(0, radius));
        int maximum = Math.min(10_000, Math.max(0, limit));
        Collection<Entity> nearby = origin.getWorld().getNearbyEntities(origin, r, r, r,
                entity -> filter == null || filter.test(entity));
        ArrayList<Entity> result = new ArrayList<>(maximum);
        for (Entity entity : nearby) {
            if (result.size() >= maximum) break;
            result.add(entity);
        }
        return result;
    }
    @Override public Collection<UUID> nearbyIds(Location origin, double radius,
                                                Predicate<Entity> filter, int limit) {
        ArrayList<UUID> ids = new ArrayList<>();
        for (Entity entity : nearby(origin, radius, filter, limit)) ids.add(entity.getUniqueId());
        return ids;
    }
}
