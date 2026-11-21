package dev.kehlchen.lib.api.network.websocket;

import dev.kehlchen.lib.api.Resource;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface WebSocketConnection extends Resource {
    String id();
    WebSocketStatus status();
    CompletableFuture<Void> sendText(String message);
    CompletableFuture<Void> sendBinary(ByteBuffer message);
    CompletableFuture<Void> close(int statusCode, String reason);
    @Override default void close() { close(1000, "client shutdown"); }
}
