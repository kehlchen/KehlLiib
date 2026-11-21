package dev.kehlchen.lib.api.network.websocket;

import java.nio.ByteBuffer;

public interface WebSocketListener {
    default void onConnected(WebSocketConnection connection) {}
    default void onDisconnected(WebSocketConnection connection, int statusCode, String reason) {}
    default void onText(WebSocketConnection connection, String message) {}
    default void onBinary(WebSocketConnection connection, ByteBuffer message) {}
    default void onError(WebSocketConnection connection, Throwable failure) {}
}
