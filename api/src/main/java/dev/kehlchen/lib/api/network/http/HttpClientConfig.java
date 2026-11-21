package dev.kehlchen.lib.api.network.http;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

public record HttpClientConfig(Duration connectTimeout, int workerThreads, int maximumRequestBytes, int maximumResponseBytes,
                               int maximumConcurrentRequests, int maximumQueuedRequests, int requestsPerSecond,
                               boolean allowPlainHttp, Set<String> allowedHosts) {
    public HttpClientConfig {
        if (connectTimeout == null || connectTimeout.compareTo(Duration.ofMillis(1)) < 0
                || connectTimeout.compareTo(Duration.ofMinutes(1)) > 0) {
            throw new IllegalArgumentException("connectTimeout must be between 1 ms and 1 minute");
        }
        if (workerThreads < 1 || workerThreads > 64
                || maximumRequestBytes < 1 || maximumRequestBytes > 64 * 1024 * 1024
                || maximumResponseBytes < 1 || maximumResponseBytes > 64 * 1024 * 1024
                || maximumConcurrentRequests < 1 || maximumConcurrentRequests > 1_024
                || maximumQueuedRequests < 0 || maximumQueuedRequests > 10_000
                || requestsPerSecond < 1 || requestsPerSecond > 100_000
                || (long) maximumConcurrentRequests + maximumQueuedRequests > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("HTTP limits exceed supported bounds");
        }
        allowedHosts = allowedHosts.stream().map(host -> host.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static HttpClientConfig defaults() {
        return new HttpClientConfig(Duration.ofSeconds(10), 4, 2 * 1024 * 1024,
                2 * 1024 * 1024, 32, 128, 20, false, Set.of());
    }
}
