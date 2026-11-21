package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.gameplay.CombatTracker;
import dev.kehlchen.lib.api.gameplay.CooldownManager;
import dev.kehlchen.lib.api.particle.ParticleShape;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase6Test {
    @Test
    void cooldownExpiresAndCanBeCleared() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        UUID player = UUID.randomUUID();
        CooldownManager cooldowns = new CooldownManager(clock);
        cooldowns.start(player, "dash", Duration.ofSeconds(5));
        assertTrue(cooldowns.isActive(player, "dash"));
        assertEquals(Duration.ofSeconds(5), cooldowns.remaining(player, "dash"));
        clock.advance(Duration.ofSeconds(6));
        assertFalse(cooldowns.isActive(player, "dash"));
        assertEquals(Duration.ZERO, cooldowns.remaining(player, "dash"));
    }

    @Test
    void combatTrackerTracksAttackerAndExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        UUID victim = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        CombatTracker combat = new CombatTracker(Duration.ofSeconds(10), clock);
        combat.tag(victim, attacker);
        assertTrue(combat.inCombat(victim));
        assertEquals(attacker, combat.lastAttacker(victim).orElseThrow());
        clock.advance(Duration.ofSeconds(11));
        assertFalse(combat.inCombat(victim));
        assertTrue(combat.lastAttacker(victim).isEmpty());
    }

    @Test
    void particleShapesGenerateBoundedAndFinitePoints() {
        Location center = new Location(null, 10, 20, 30);
        List<Location> circle = ParticleShape.circle(center, 5, 16);
        assertEquals(16, circle.size());
        for (Location point : circle) {
            assertTrue(Double.isFinite(point.getX()));
            assertTrue(Double.isFinite(point.getZ()));
            assertEquals(20.0, point.getY());
        }
        assertEquals(12, ParticleShape.helix(center, 2, 6, 12, 3).size());
        assertEquals(1, ParticleShape.sphere(center, 5, 1).size());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneOffset zone = ZoneOffset.UTC;
        private MutableClock(Instant instant) { this.instant = instant; }
        @Override public ZoneOffset getZone() { return zone; }
        @Override public Clock withZone(java.time.ZoneId zoneId) { return this; }
        @Override public Instant instant() { return instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
    }
}
