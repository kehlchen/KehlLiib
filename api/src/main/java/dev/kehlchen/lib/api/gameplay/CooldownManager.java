package dev.kehlchen.lib.api.gameplay;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory player cooldown tracker with automatic expiration. */
public final class CooldownManager {
    private final Clock clock;
    private final Map<String, Instant> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager() { this(Clock.systemUTC()); }
    public CooldownManager(Clock clock) { this.clock = clock == null ? Clock.systemUTC() : clock; }

    public void start(UUID player, String key, Duration duration) {
        if (player == null || key == null || duration == null) throw new NullPointerException();
        cooldowns.put(player + ":" + key, Instant.now(clock).plus(duration));
    }
    public boolean isActive(UUID player, String key) {
        Instant until = cooldowns.get(player + ":" + key);
        if (until == null) return false;
        if (!Instant.now(clock).isBefore(until)) {
            cooldowns.remove(player + ":" + key);
            return false;
        }
        return true;
    }
    public Duration remaining(UUID player, String key) {
        Instant until = cooldowns.get(player + ":" + key);
        if (until == null) return Duration.ZERO;
        Duration remaining = Duration.between(Instant.now(clock), until);
        if (remaining.isNegative()) { cooldowns.remove(player + ":" + key); return Duration.ZERO; }
        return remaining;
    }
    public void clear(UUID player, String key) { cooldowns.remove(player + ":" + key); }
    public void clear(UUID player) {
        String prefix = player + ":";
        cooldowns.keySet().removeIf(id -> id.startsWith(prefix));
    }
    public void cleanup() {
        Instant now = Instant.now(clock);
        cooldowns.entrySet().removeIf(entry -> !now.isBefore(entry.getValue()));
    }
}
