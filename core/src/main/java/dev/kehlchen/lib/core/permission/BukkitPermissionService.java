package dev.kehlchen.lib.core.permission;

import dev.kehlchen.lib.api.permission.PermissionCheck;
import dev.kehlchen.lib.api.permission.PermissionService;
import org.bukkit.permissions.Permissible;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe permission cache with Bukkit integration and inheritance. */
public final class BukkitPermissionService implements PermissionService {
    private final Map<UUID, Map<String, Boolean>> direct = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, TemporaryPermission>> temporary = new ConcurrentHashMap<>();
    private final Map<UUID, String> groups = new ConcurrentHashMap<>();
    private final Map<String, String> parentGroups = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Boolean>> groupPermissions = new ConcurrentHashMap<>();
    private final Map<String, PermissionCheck> cache = new ConcurrentHashMap<>();

    @Override public String name() { return "permissions"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() {}
    @Override public void onDisable() { clearCache(); }

    public void defineGroup(String group, String parent) {
        if (parent == null) parentGroups.remove(group);
        else parentGroups.put(group, parent);
        clearCache();
    }

    public void setGroupPermission(String group, String permission, boolean value) {
        if (group == null || group.isBlank() || permission == null || permission.isBlank()) return;
        groupPermissions.computeIfAbsent(group, ignored -> new ConcurrentHashMap<>()).put(permission, value);
        clearCache();
    }

    @Override
    public PermissionCheck check(Permissible subject, String permission) {
        if (subject == null || permission == null || permission.isBlank()) {
            return PermissionCheck.deny(permission == null ? "" : permission, "Missing subject or permission");
        }
        if (subject.hasPermission(permission)) {
            return PermissionCheck.allow(permission, "Granted by Bukkit");
        }
        if (subject instanceof org.bukkit.entity.Player player) {
            return check(player.getUniqueId(), permission);
        }
        return PermissionCheck.deny(permission, "Denied by Bukkit");
    }

    @Override
    public PermissionCheck check(UUID user, String permission) {
        if (user == null || permission == null || permission.isBlank()) {
            return PermissionCheck.deny(permission == null ? "" : permission, "Missing user or permission");
        }
        String cacheKey = user + ":" + permission;
        Instant now = Instant.now();
        Map<String, TemporaryPermission> temporaryForUser = temporary.get(user);
        if (temporaryForUser != null) {
            temporaryForUser.entrySet().removeIf(entry -> {
                TemporaryPermission value = entry.getValue();
                return value.expires().isBefore(now);
            });
        }
        Boolean directValue = lookupDirectAndTemporary(user, permission);
        if (directValue != null) {
            PermissionCheck result = directValue
                    ? PermissionCheck.allow(permission, "User permission")
                    : PermissionCheck.deny(permission, "User denied permission");
            cache.put(cacheKey, result);
            return result;
        }
        String group = groups.get(user);
        if (group != null && evaluateGroup(group, permission, ConcurrentHashMap.newKeySet())) {
            PermissionCheck result = PermissionCheck.allow(permission, "Granted by group " + group);
            cache.put(cacheKey, result);
            return result;
        }
        PermissionCheck result = PermissionCheck.deny(permission, "No matching permission");
        cache.put(cacheKey, result);
        return result;
    }

    private Boolean lookupDirectAndTemporary(UUID user, String permission) {
        Map<String, Boolean> values = direct.get(user);
        if (values != null) {
            Boolean exact = values.get(permission);
            if (exact != null) return exact;
            String mostSpecific = null;
            Boolean wildcardValue = null;
            for (Map.Entry<String, Boolean> entry : values.entrySet()) {
                if (wildcardMatches(entry.getKey(), permission)
                        && (mostSpecific == null || entry.getKey().length() > mostSpecific.length())) {
                    mostSpecific = entry.getKey();
                    wildcardValue = entry.getValue();
                }
            }
            if (wildcardValue != null) return wildcardValue;
        }
        Map<String, TemporaryPermission> temporaryValues = temporary.get(user);
        if (temporaryValues != null) {
            TemporaryPermission exact = temporaryValues.get(permission);
            if (exact != null && !exact.expired()) return exact.value();
            for (TemporaryPermission value : temporaryValues.values()) {
                if (!value.expired() && wildcardMatches(value.permission(), permission)) return value.value();
            }
        }
        return null;
    }

    private boolean evaluateGroup(String group, String permission, java.util.Set<String> visited) {
        if (group == null || !visited.add(group)) return false;
        String parent = parentGroups.get(group);
        if (parent != null && evaluateGroup(parent, permission, visited)) return true;
        Map<String, Boolean> permissions = groupPermissions.get(group);
        if (permissions == null) return false;
        Boolean exact = permissions.get(permission);
        if (exact != null) return exact;
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            if (wildcardMatches(entry.getKey(), permission)) return entry.getValue();
        }
        return false;
    }

    private boolean wildcardMatches(String pattern, String permission) {
        if ("*".equals(pattern)) return true;
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return permission.startsWith(prefix);
        }
        return pattern.equals(permission);
    }

    @Override
    public void set(UUID user, String permission, boolean value) {
        if (user == null || permission == null || permission.isBlank()) return;
        direct.computeIfAbsent(user, ignored -> new ConcurrentHashMap<>()).put(permission, value);
        clearCache();
    }

    @Override
    public void setTemporary(UUID user, String permission, boolean value, Duration duration) {
        if (user == null || permission == null || permission.isBlank()
                || duration == null || duration.isNegative() || duration.isZero()) return;
        temporary.computeIfAbsent(user, ignored -> new ConcurrentHashMap<>())
                .put(permission, new TemporaryPermission(permission, value, Instant.now().plus(duration)));
        clearCache();
    }

    @Override
    public void setGroup(UUID user, String group) {
        if (user == null) return;
        if (group == null || group.isBlank()) groups.remove(user);
        else groups.put(user, group);
        clearCache();
    }

    @Override
    public void clearUser(UUID user) {
        if (user == null) return;
        direct.remove(user);
        temporary.remove(user);
        groups.remove(user);
        clearCache();
    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    private record TemporaryPermission(String permission, boolean value, Instant expires) {
        boolean expired() { return expires.isBefore(Instant.now()); }
    }
}
