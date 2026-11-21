package dev.kehlchen.lib.api.integration.proxy;

import java.util.UUID;
import java.util.Optional;

@FunctionalInterface
public interface ProxyMessageHandler {
    void onMessage(Optional<UUID> playerId, String channel, byte[] payload);
}
