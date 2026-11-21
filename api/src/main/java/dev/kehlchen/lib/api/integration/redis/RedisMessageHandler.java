package dev.kehlchen.lib.api.integration.redis;

@FunctionalInterface
public interface RedisMessageHandler {
    void onMessage(String channel, String payload);
}
