package dev.kehlchen.lib.api.entity;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/** UUID-safe entity lookup and bounded entity operations. */
public interface EntityService extends Lifecycle {
    Result<Entity> find(UUID uuid);
    Result<Entity> spawn(Location location, EntityType type);
    Result<Boolean> remove(Entity entity);
    Result<Boolean> teleport(Entity entity, Location location);
    boolean isValid(Entity entity);
    Collection<Entity> nearby(Location origin, double radius, Predicate<Entity> filter, int limit);
    Collection<UUID> nearbyIds(Location origin, double radius, Predicate<Entity> filter, int limit);
}
