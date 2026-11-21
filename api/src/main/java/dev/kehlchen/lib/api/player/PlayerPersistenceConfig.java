package dev.kehlchen.lib.api.player;

import java.time.Duration;
import java.util.Objects;

public record PlayerPersistenceConfig(Duration saveInterval, int retries, Duration retryDelay,
                                     Duration shutdownTimeout) {
    public PlayerPersistenceConfig {
        Objects.requireNonNull(saveInterval, "saveInterval");
        Objects.requireNonNull(retryDelay, "retryDelay");
        Objects.requireNonNull(shutdownTimeout, "shutdownTimeout");
        if (saveInterval.isZero() || saveInterval.isNegative()
                || retries < 0 || retryDelay.isNegative()
                || shutdownTimeout.isZero() || shutdownTimeout.isNegative()) {
            throw new IllegalArgumentException("Invalid player persistence configuration");
        }
    }

    public static PlayerPersistenceConfig defaults() {
        return new PlayerPersistenceConfig(Duration.ofMinutes(5), 3,
                Duration.ofSeconds(1), Duration.ofSeconds(15));
    }
}
