package dev.kehlchen.lib.api.gameplay;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Lightweight combat tagging without changing Bukkit damage behavior. */
public final class CombatTracker {
    private final Duration duration;
    private final Clock clock;
    private final Map<UUID, Tag> tags = new ConcurrentHashMap<>();

    public CombatTracker(Duration duration) { this(duration, Clock.systemUTC()); }
    public CombatTracker(Duration duration, Clock clock) {
        this.duration = duration == null ? Duration.ofSeconds(15) : duration;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }
    public void tag(UUID victim, UUID attacker) {
        if (victim == null || attacker == null) throw new NullPointerException();
        tags.put(victim, new Tag(attacker, Instant.now(clock).plus(duration)));
    }
    public boolean inCombat(UUID player) {
        Tag tag = tags.get(player);
        if (tag == null) return false;
        if (!Instant.now(clock).isBefore(tag.until)) { tags.remove(player); return false; }
        return true;
    }
    public Optional<UUID> lastAttacker(UUID player) {
        Tag tag = tags.get(player);
        return tag != null && Instant.now(clock).isBefore(tag.until)
                ? Optional.of(tag.attacker) : Optional.empty();
    }
    public void untag(UUID player) { tags.remove(player); }
    public void cleanup() {
        Instant now = Instant.now(clock);
        tags.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().until));
    }
    private record Tag(UUID attacker, Instant until) {}
}
