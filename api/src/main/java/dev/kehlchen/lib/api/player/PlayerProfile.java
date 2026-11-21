package dev.kehlchen.lib.api.player;

import java.util.UUID;

/** Immutable identification profile for a player. */
public record PlayerProfile(UUID uniqueId, String name) {
    public PlayerProfile {
        if (uniqueId == null) throw new IllegalArgumentException("uniqueId");
    }
}
