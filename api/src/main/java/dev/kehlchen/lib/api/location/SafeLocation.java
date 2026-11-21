package dev.kehlchen.lib.api.location;

import org.bukkit.Location;

import java.util.Objects;

/** A validated location and the reason when validation fails. */
public record SafeLocation(Location location, boolean safe, String reason) {
    public SafeLocation {
        Objects.requireNonNull(reason, "reason");
    }

    public static SafeLocation safe(Location location) {
        return new SafeLocation(location, true, "safe");
    }

    public static SafeLocation unsafe(Location location, String reason) {
        return new SafeLocation(location, false, reason);
    }
}
