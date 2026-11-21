package dev.kehlchen.lib.api.integration.redis;

import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.integration.Integration;

import java.util.concurrent.CompletableFuture;

public interface RedisService extends Integration {
    boolean isConnected();
    CompletableFuture<Result<Void>> ready();
    CompletableFuture<Result<Long>> publish(String channel, String payload);
    CompletableFuture<Result<Resource>> subscribe(String channel, RedisMessageHandler handler);
}
