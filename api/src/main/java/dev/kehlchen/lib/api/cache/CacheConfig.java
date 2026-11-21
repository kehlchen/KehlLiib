package dev.kehlchen.lib.api.cache;

import java.time.Duration;
import java.util.Objects;

public record CacheConfig(int maximumSize, Duration timeToLive, EvictionPolicy evictionPolicy) {
    public CacheConfig {
        if (maximumSize < 1) throw new IllegalArgumentException("maximumSize must be positive");
        Objects.requireNonNull(timeToLive, "timeToLive");
        if (timeToLive.isNegative()) throw new IllegalArgumentException("timeToLive cannot be negative");
        Objects.requireNonNull(evictionPolicy, "evictionPolicy");
    }

    public static CacheConfig defaults() {
        return new CacheConfig(10_000, Duration.ZERO, EvictionPolicy.LRU);
    }
}
