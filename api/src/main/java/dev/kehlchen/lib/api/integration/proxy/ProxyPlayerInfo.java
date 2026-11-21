package dev.kehlchen.lib.api.integration.proxy;

import java.util.Optional;
import java.util.UUID;

public record ProxyPlayerInfo(UUID uniqueId, String name, String server) {
    public ProxyPlayerInfo {
        java.util.Objects.requireNonNull(uniqueId, "uniqueId");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Player name is required");
    }

    public Optional<String> currentServer() { return Optional.ofNullable(server); }
}
