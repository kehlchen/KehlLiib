package dev.kehlchen.lib.api.integration.proxy;

import java.util.OptionalInt;

public record ProxyServerInfo(String name, int playerCount, Integer maximumPlayers) {
    public ProxyServerInfo {
        if (name == null || name.isBlank() || playerCount < 0
                || maximumPlayers != null && maximumPlayers < playerCount) {
            throw new IllegalArgumentException("Invalid proxy server information");
        }
    }

    public OptionalInt maxPlayers() {
        return maximumPlayers == null ? OptionalInt.empty() : OptionalInt.of(maximumPlayers);
    }
}
