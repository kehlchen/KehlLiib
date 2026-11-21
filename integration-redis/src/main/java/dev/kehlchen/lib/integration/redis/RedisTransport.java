package dev.kehlchen.lib.integration.redis;

import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.integration.redis.RedisMessageHandler;

import java.util.concurrent.CompletableFuture;

interface RedisTransport extends AutoCloseable {
    CompletableFuture<Void> connect();
    boolean isConnected();
    CompletableFuture<Long> publish(String channel, String payload);
    CompletableFuture<Resource> subscribe(String channel, RedisMessageHandler handler);
    @Override void close();
}
