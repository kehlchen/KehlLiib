package dev.kehlchen.lib.api.network.websocket;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

public record WebSocketConfig(Duration connectTimeout, Duration pingInterval,
                              Duration reconnectDelay, int maximumReconnectAttempts,
                              int maximumMessageBytes, int maximumConnections, Set<String> allowedHosts) {
    public WebSocketConfig {
        if (connectTimeout == null || connectTimeout.compareTo(Duration.ofMillis(1)) < 0
                || connectTimeout.compareTo(Duration.ofMinutes(1)) > 0
                || reconnectDelay == null || reconnectDelay.isNegative()
                || reconnectDelay.compareTo(Duration.ofMinutes(1)) > 0
                || pingInterval == null || pingInterval.isNegative()
                || pingInterval.compareTo(Duration.ofHours(1)) > 0
                || (!pingInterval.isZero() && pingInterval.toMillis() == 0)
                || maximumReconnectAttempts < 0 || maximumReconnectAttempts > 20
                || maximumMessageBytes < 1 || maximumMessageBytes > 16 * 1024 * 1024
                || maximumConnections < 1 || maximumConnections > 1_024) {
            throw new IllegalArgumentException("Invalid WebSocket limits or durations");
        }
        allowedHosts = allowedHosts.stream().map(host -> host.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static WebSocketConfig defaults() {
        return new WebSocketConfig(Duration.ofSeconds(10), Duration.ofSeconds(30),
                Duration.ofSeconds(2), 3, 1024 * 1024, 64, Set.of());
    }
}
