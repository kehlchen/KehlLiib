package dev.kehlchen.lib.api.permission;

/** Explains why a permission check was granted or denied. */
public record PermissionCheck(boolean granted, String permission, String reason) {
    public static PermissionCheck allow(String permission, String reason) {
        return new PermissionCheck(true, permission, reason);
    }

    public static PermissionCheck deny(String permission, String reason) {
        return new PermissionCheck(false, permission, reason);
    }
}
