package dev.kehlchen.lib.integration.redis;

import dev.kehlchen.lib.api.integration.redis.RedisConfig;
import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.integration.redis.RedisMessageHandler;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {
    @Test
    void copiesCredentialsAndNeverIncludesThemInItsStringRepresentation() {
        char[] secret = "private-token".toCharArray();
        RedisConfig config = new RedisConfig("redis.example", 6379, "user", secret, 0,
                true, Duration.ofSeconds(2), Duration.ofSeconds(3), Duration.ofSeconds(1),
                5, 65_536);
        secret[0] = 'X';
        char[] read = config.password();
        assertEquals('p', read[0]);
        read[0] = 'Y';
        assertEquals('p', config.password()[0]);
        assertFalse(config.toString().contains("private-token"));
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> new RedisConfig("localhost", 6379,
                null, null, 0, false, Duration.ofSeconds(1), Duration.ofSeconds(1),
                Duration.ofSeconds(1), 11, 1024));
    }

    @Test
    void managesMockedPubSubSubscriptionsAndLifecycleWithoutRedis() throws Exception {
        FakeTransport transport = new FakeTransport();
        LettuceRedisIntegration redis = new LettuceRedisIntegration(config(1024), () -> transport);
        redis.onEnable();
        assertTrue(redis.ready().get(1, TimeUnit.SECONDS).isSuccess());
        assertTrue(redis.isConnected());

        AtomicInteger received = new AtomicInteger();
        Result<Resource> subscription = redis.subscribe("events", (channel, payload) -> {
            assertEquals("events", channel);
            assertEquals("{\"type\":\"join\"}", payload);
            received.incrementAndGet();
        }).get(1, TimeUnit.SECONDS);
        assertTrue(subscription.isSuccess());
        assertTrue(redis.publish("events", "{\"type\":\"join\"}").get(1, TimeUnit.SECONDS).isSuccess());
        assertEquals(1, received.get());

        subscription.value().orElseThrow().close();
        redis.publish("events", "ignored").get(1, TimeUnit.SECONDS);
        assertEquals(1, received.get());
        redis.onDisable();
        assertTrue(transport.closed);
        assertFalse(redis.isConnected());
    }

    @Test
    void rejectsOversizedMessagesAndReportsBoundedConnectionFailure() throws Exception {
        FakeTransport transport = new FakeTransport();
        LettuceRedisIntegration redis = new LettuceRedisIntegration(config(4), () -> transport);
        redis.onEnable();
        assertFalse(redis.publish("events", "too long").get(1, TimeUnit.SECONDS).isSuccess());
        redis.onDisable();

        AtomicInteger attempts = new AtomicInteger();
        FakeTransport failing = new FakeTransport() {
            @Override public CompletableFuture<Void> connect() {
                attempts.incrementAndGet();
                return CompletableFuture.failedFuture(new IllegalStateException("unavailable"));
            }
        };
        LettuceRedisIntegration unavailable = new LettuceRedisIntegration(config(128, 2), () -> failing);
        unavailable.onEnable();
        assertFalse(unavailable.ready().get(1, TimeUnit.SECONDS).isSuccess());
        assertEquals(2, attempts.get());
        unavailable.onDisable();
    }

    private RedisConfig config(int maximumMessageBytes) {
        return config(maximumMessageBytes, 3);
    }

    private RedisConfig config(int maximumMessageBytes, int maximumReconnectAttempts) {
        return new RedisConfig("redis.invalid", 6379, null, null, 0,
                true, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofMillis(1),
                maximumReconnectAttempts, maximumMessageBytes);
    }

    private static class FakeTransport implements RedisTransport {
        private final Map<String, CopyOnWriteArrayList<RedisMessageHandler>> listeners = new java.util.concurrent.ConcurrentHashMap<>();
        private volatile boolean closed;

        @Override public CompletableFuture<Void> connect() { return CompletableFuture.completedFuture(null); }
        @Override public boolean isConnected() { return !closed; }

        @Override public CompletableFuture<Long> publish(String channel, String payload) {
            CopyOnWriteArrayList<RedisMessageHandler> handlers = listeners.get(channel);
            if (handlers != null) handlers.forEach(handler -> handler.onMessage(channel, payload));
            return CompletableFuture.completedFuture(handlers == null ? 0L : (long) handlers.size());
        }

        @Override public CompletableFuture<Resource> subscribe(String channel, RedisMessageHandler handler) {
            CopyOnWriteArrayList<RedisMessageHandler> handlers =
                    listeners.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>());
            handlers.add(handler);
            return CompletableFuture.completedFuture(() -> handlers.remove(handler));
        }

        @Override public void close() {
            closed = true;
            listeners.clear();
        }
    }
}
