package dev.kehlchen.lib.api.player;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** UUID-safe abstraction for an offline or historical player. */
public interface OfflinePlayerView {
    UUID uniqueId();
    Optional<String> name();
    boolean isOnline();
    Optional<Duration> playtime();
    boolean hasPlayedBefore();
}
