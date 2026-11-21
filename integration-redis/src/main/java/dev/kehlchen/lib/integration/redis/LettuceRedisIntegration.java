package dev.kehlchen.lib.integration.redis;

import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.integration.redis.RedisConfig;
import dev.kehlchen.lib.api.integration.redis.RedisMessageHandler;
import dev.kehlchen.lib.api.integration.redis.RedisService;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Optional Redis module; callbacks run on Redis I/O threads, never Bukkit's main thread. */
public final class LettuceRedisIntegration implements RedisService {
    private static final System.Logger LOGGER = System.getLogger(LettuceRedisIntegration.class.getName());
    private final RedisConfig config;
    private final Supplier<RedisTransport> transportFactory;
    private final ScheduledExecutorService retries = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "kehllib-redis-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private volatile RedisTransport transport;
    private volatile CompletableFuture<Result<Void>> ready = new CompletableFuture<>();
    private volatile boolean enabled;
    private volatile boolean closed;

    public LettuceRedisIntegration(RedisConfig config) {
        this(config, () -> new LettuceRedisTransport(config));
    }

    LettuceRedisIntegration(RedisConfig config, Supplier<RedisTransport> transportFactory) {
        this.config = java.util.Objects.requireNonNull(config, "config");
        this.transportFactory = java.util.Objects.requireNonNull(transportFactory, "transportFactory");
    }

    @Override public String name() { return "redis"; }
    @Override public String version() { return "1.0"; }
    @Override public boolean isAvailable() { return true; }

    @Override public synchronized void onEnable() {
        if (enabled) return;
        if (closed) throw new IllegalStateException("Redis integration cannot restart after shutdown");
        transport = java.util.Objects.requireNonNull(transportFactory.get(), "transport");
        ready = new CompletableFuture<>();
        enabled = true;
        connect(1);
    }

    private void connect(int attempt) {
        RedisTransport current = transport;
        if (!enabled || current == null) return;
        try {
            current.connect().whenComplete((ignored, failure) -> {
                if (!enabled) return;
                if (failure == null) {
                    ready.complete(Result.success(null));
                    return;
                }
                retryConnection(attempt, failure);
            });
        } catch (RuntimeException failure) {
            retryConnection(attempt, failure);
        }
    }

    private void retryConnection(int attempt, Throwable failure) {
        if (attempt >= config.maximumReconnectAttempts()) {
            LOGGER.log(System.Logger.Level.ERROR, "Redis connection failed after bounded retries: {0}",
                    failure.getClass().getName());
            ready.complete(Result.failure(new IllegalStateException("Redis connection failed after bounded retries")));
            return;
        }
        try {
            retries.schedule(() -> connect(attempt + 1), config.reconnectDelay().toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            if (enabled) ready.complete(Result.failure(rejected));
        }
    }

    @Override public boolean isConnected() {
        RedisTransport current = transport;
        return enabled && current != null && current.isConnected();
    }

    @Override public CompletableFuture<Result<Void>> ready() { return ready.copy(); }

    @Override public CompletableFuture<Result<Long>> publish(String channel, String payload) {
        if (!validChannel(channel) || payload == null
                || payload.getBytes(StandardCharsets.UTF_8).length > config.maximumMessageBytes()) {
            return CompletableFuture.completedFuture(Result.failure(
                    new IllegalArgumentException("Invalid or oversized Redis channel or payload")));
        }
        RedisTransport current = transport;
        if (!isConnected() || current == null) {
            return CompletableFuture.completedFuture(Result.failure(new IllegalStateException("Redis is not connected")));
        }
        try {
            return current.publish(channel, payload)
                    .handle((count, failure) -> failure == null ? Result.success(count) : Result.failure(failure));
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(Result.failure(failure));
        }
    }

    @Override public CompletableFuture<Result<Resource>> subscribe(String channel, RedisMessageHandler handler) {
        java.util.Objects.requireNonNull(handler, "handler");
        if (!validChannel(channel)) {
            return CompletableFuture.completedFuture(Result.failure(new IllegalArgumentException("Invalid Redis channel")));
        }
        RedisTransport current = transport;
        if (!isConnected() || current == null) {
            return CompletableFuture.completedFuture(Result.failure(new IllegalStateException("Redis Pub/Sub is not connected")));
        }
        try {
            return current.subscribe(channel, handler)
                    .handle((subscription, failure) -> failure == null
                            ? Result.success(subscription) : Result.failure(failure));
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(Result.failure(failure));
        }
    }

    private static boolean validChannel(String channel) {
        return channel != null && !channel.isBlank() && channel.getBytes(StandardCharsets.UTF_8).length <= 256;
    }

    @Override public synchronized void onDisable() {
        if (closed) return;
        enabled = false;
        closed = true;
        ready.complete(Result.failure(new IllegalStateException("Redis integration shut down")));
        RuntimeException failure = null;
        RedisTransport current = transport;
        transport = null;
        if (current != null) {
            try {
                current.close();
            } catch (RuntimeException closeFailure) {
                failure = closeFailure;
            }
        }
        retries.shutdownNow();
        if (failure != null) throw new IllegalStateException("Redis shutdown failed", failure);
    }
}
