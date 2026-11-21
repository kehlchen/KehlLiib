package dev.kehlchen.lib.api.network.websocket;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public interface WebSocketService extends Lifecycle {
    CompletableFuture<Result<WebSocketConnection>> connect(
            URI uri, WebSocketConfig config, WebSocketListener listener);
}
