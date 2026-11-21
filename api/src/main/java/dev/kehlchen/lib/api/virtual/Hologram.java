package dev.kehlchen.lib.api.virtual;

import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public interface Hologram {
    UUID id();
    Location location();
    List<String> lines();
    void setLines(List<String> lines);
    void setLocation(Location location);
    void showTo(UUID playerId);
    void hideFrom(UUID playerId);
    void remove();
}
