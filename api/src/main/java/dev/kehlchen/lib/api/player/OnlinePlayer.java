package dev.kehlchen.lib.api.player;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** UUID-safe abstraction for an online player. */
public interface OnlinePlayer {
    UUID uniqueId();
    Optional<String> name();
    boolean isOnline();
    boolean isFirstJoin();
    Duration playtime();
    Duration afkDuration();
    boolean isAfk();
    Object metadata(String key);
    void metadata(String key, Object value);
    void removeMetadata(String key);
    org.bukkit.Location location();
    boolean teleport(org.bukkit.Location location);
    CompletablePlayerPreferences preferences();
}
