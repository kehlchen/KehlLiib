package dev.kehlchen.lib.api.player;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable persisted player state; updates return a new revision. */
public record PersistentPlayerData(UUID uniqueId, String lastKnownName, boolean firstJoin,
                                   long totalPlaytimeMillis, Instant lastSeen,
                                   Map<String, String> preferences, Map<String, String> customData,
                                   long revision) {
    public PersistentPlayerData {
        Objects.requireNonNull(uniqueId, "uniqueId");
        if (totalPlaytimeMillis < 0 || revision < 0) throw new IllegalArgumentException("Invalid player data counters");
        lastSeen = lastSeen == null ? Instant.EPOCH : lastSeen;
        preferences = Map.copyOf(preferences);
        customData = Map.copyOf(customData);
    }

    public static PersistentPlayerData initial(UUID uniqueId, String name, boolean firstJoin) {
        return new PersistentPlayerData(uniqueId, name, firstJoin, 0, Instant.EPOCH, Map.of(), Map.of(), 0);
    }

    public PersistentPlayerData withName(String name) {
        if (Objects.equals(lastKnownName, name)) return this;
        return copy(name, firstJoin, totalPlaytimeMillis, lastSeen, preferences, customData);
    }

    public PersistentPlayerData withPreference(String key, String value) {
        var updated = new java.util.HashMap<>(preferences);
        if (value == null) updated.remove(key); else updated.put(key, value);
        return copy(lastKnownName, firstJoin, totalPlaytimeMillis, lastSeen, updated, customData);
    }

    public PersistentPlayerData withCustomData(String key, String value) {
        var updated = new java.util.HashMap<>(customData);
        if (value == null) updated.remove(key); else updated.put(key, value);
        return copy(lastKnownName, firstJoin, totalPlaytimeMillis, lastSeen, preferences, updated);
    }

    public PersistentPlayerData finishSession(Duration elapsed, Instant seenAt) {
        long millis = Math.max(0, Objects.requireNonNull(elapsed, "elapsed").toMillis());
        long total = totalPlaytimeMillis > Long.MAX_VALUE - millis ? Long.MAX_VALUE : totalPlaytimeMillis + millis;
        return copy(lastKnownName, firstJoin, total, Objects.requireNonNull(seenAt, "seenAt"),
                preferences, customData);
    }

    private PersistentPlayerData copy(String name, boolean isFirstJoin, long playtime, Instant seen,
                                      Map<String, String> playerPreferences, Map<String, String> playerData) {
        return new PersistentPlayerData(uniqueId, name, isFirstJoin, playtime, seen,
                playerPreferences, playerData, revision + 1);
    }
}
