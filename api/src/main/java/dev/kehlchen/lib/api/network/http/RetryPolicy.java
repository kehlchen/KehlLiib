package dev.kehlchen.lib.api.network.http;

import java.time.Duration;
import java.util.Set;

public record RetryPolicy(int maxAttempts, Duration initialDelay, Duration maximumDelay,
                          Set<Integer> retryStatusCodes) {
    public RetryPolicy {
        if (maxAttempts < 1 || maxAttempts > 10) throw new IllegalArgumentException("maxAttempts must be between 1 and 10");
        if (initialDelay == null || initialDelay.isNegative() || initialDelay.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("initialDelay must be between zero and five minutes");
        }
        if (maximumDelay == null || maximumDelay.isNegative()
                || maximumDelay.compareTo(initialDelay) < 0 || maximumDelay.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("maximumDelay must be at least initialDelay");
        }
        retryStatusCodes = Set.copyOf(retryStatusCodes);
        if (retryStatusCodes.stream().anyMatch(status -> status < 100 || status > 599)) {
            throw new IllegalArgumentException("retryStatusCodes must be valid HTTP status codes");
        }
    }

    public static RetryPolicy none() {
        return new RetryPolicy(1, Duration.ZERO, Duration.ZERO, Set.of());
    }
}
