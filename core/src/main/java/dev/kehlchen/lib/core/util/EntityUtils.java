package dev.kehlchen.lib.core.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public final class EntityUtils {
    private EntityUtils() {}
    public static boolean isAlive(Entity entity) { return entity != null && entity.isValid() && !entity.isDead(); }
    public static boolean isLiving(Entity entity) { return entity instanceof LivingEntity living && living.isValid(); }
    public static void heal(Entity entity) {
        if (entity instanceof LivingEntity living) living.setHealth(living.getMaxHealth());
    }
    public static double distance(Entity a, Entity b) {
        if (a == null || b == null || a.getWorld() != b.getWorld()) return Double.POSITIVE_INFINITY;
        return a.getLocation().distance(b.getLocation());
    }
}
