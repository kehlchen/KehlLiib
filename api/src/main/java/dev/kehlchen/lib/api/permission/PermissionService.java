package dev.kehlchen.lib.api.permission;

import dev.kehlchen.lib.api.Lifecycle;
import org.bukkit.permissions.Permissible;

import java.time.Duration;
import java.util.UUID;

/** Reusable permission abstraction backed by Bukkit by default. */
public interface PermissionService extends Lifecycle {
    PermissionCheck check(Permissible subject, String permission);
    PermissionCheck check(UUID user, String permission);
    void set(UUID user, String permission, boolean value);
    void setTemporary(UUID user, String permission, boolean value, Duration duration);
    void setGroup(UUID user, String group);
    void clearUser(UUID user);
    void clearCache();
}
