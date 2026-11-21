package dev.kehlchen.lib.api.virtual;

import org.bukkit.Location;

import java.util.List;
import java.util.Objects;

public record HologramConfig(Location location, List<String> lines,
                             double lineSpacing, boolean global) {
    public HologramConfig {
        Objects.requireNonNull(location, "location");
        lines = lines == null ? List.of() : List.copyOf(lines);
        lineSpacing = lineSpacing <= 0 ? 0.25 : lineSpacing;
    }

    public static HologramConfig of(Location location, String... lines) {
        return new HologramConfig(location, List.of(lines), 0.25, true);
    }
}
