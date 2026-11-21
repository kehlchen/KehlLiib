package dev.kehlchen.lib.core.network;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.network.websocket.WebSocketConfig;
import dev.kehlchen.lib.api.network.websocket.WebSocketConnection;
import dev.kehlchen.lib.api.network.websocket.WebSocketListener;
import dev.kehlchen.lib.api.network.websocket.WebSocketService;
import dev.kehlchen.lib.api.network.websocket.WebSocketStatus;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Managed JDK WebSockets. Secure WebSocket (wss) is required. */
public final class JdkWebSocketService implements WebSocketService {
    private static final System.Logger LOGGER = System.getLogger(JdkWebSocketService.class.getName());
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "kehllib-websocket");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "kehllib-websocket-timer");
        thread.setDaemon(true);
        return thread;
    });
    private final HttpClient client = HttpClient.newBuilder().executor(executor)
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final Set<ManagedConnection> connections = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    @Override public String name() { return "websocket"; }
    @Override public String version() { return "1.0"; }

    @Override public synchronized CompletableFuture<Result<WebSocketConnection>> connect(
            URI uri, WebSocketConfig config, WebSocketListener listener) {
        CompletableFuture<Result<WebSocketConnection>> result = new CompletableFuture<>();
        if (closed) {
            result.complete(Result.failure(new IllegalStateException("WebSocket service is closed")));
            return result;
        }
        try {
            java.util.Objects.requireNonNull(config, "config");
            validateUri(uri, config);
        } catch (RuntimeException failure) {
            result.complete(Result.failure(failure));
            return result;
        }
        if (connections.size() >= config.maximumConnections()) {
            result.complete(Result.failure(new IllegalStateException("WebSocket connection limit reached")));
            return result;
        }
        try {
            ManagedConnection connection = new ManagedConnection(uri, config, listener, result);
            connections.add(connection);
            result.whenComplete((connected, failure) -> {
                if (result.isCancelled()) connection.close(1001, "connection request cancelled");
            });
            connection.open();
        } catch (RuntimeException failure) {
            result.complete(Result.failure(failure));
        }
        return result;
    }

    private static void validateUri(URI uri, WebSocketConfig config) {
        if (uri == null || !"wss".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("An absolute wss:// URI is required");
        }
        String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
        if (!config.allowedHosts().isEmpty() && !config.allowedHosts().contains(host)) {
            throw new IllegalArgumentException("WebSocket destination host is not allow-listed");
        }
        if (!config.allowedHosts().contains(host)
                && (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local"))) {
            throw new IllegalArgumentException("Local WebSocket destinations are not allowed");
        }
    }

    private static void validateResolvedUri(URI uri, WebSocketConfig config) {
        String host = uri.getHost();
        if (host.startsWith("[") && host.endsWith("]")) host = host.substring(1, host.length() - 1);
        if (config.allowedHosts().contains(uri.getHost().toLowerCase(java.util.Locale.ROOT))) return;
        try {
            for (java.net.InetAddress address : java.net.InetAddress.getAllByName(host)) {
                if (NetworkAddressPolicy.isNonPublic(address)) {
                    throw new IllegalArgumentException("Private or local WebSocket destinations are not allowed");
                }
            }
        } catch (java.io.IOException failure) {
            throw new IllegalArgumentException("Unable to validate WebSocket destination", failure);
        }
    }

    @Override public void onEnable() {
        if (closed) throw new IllegalStateException("WebSocket service is closed");
    }

    @Override public synchronized void onDisable() {
        if (closed) return;
        closed = true;
        RuntimeException failure = null;
        for (ManagedConnection connection : connections) {
            try {
                connection.close(1001, "plugin shutdown");
            } catch (RuntimeException closeFailure) {
                connection.abort();
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }
        }
        try {
            timer.schedule(() -> {
                connections.forEach(ManagedConnection::abort);
                executor.shutdownNow();
            }, 2, TimeUnit.SECONDS);
            timer.shutdown();
            executor.shutdown();
        } catch (RuntimeException shutdownFailure) {
            timer.shutdownNow();
            connections.forEach(ManagedConnection::abort);
            executor.shutdownNow();
            if (failure == null) failure = shutdownFailure;
            else failure.addSuppressed(shutdownFailure);
        }
        if (failure != null) throw new IllegalStateException("One or more WebSocket connections failed to close", failure);
    }

    private final class ManagedConnection implements WebSocketConnection, WebSocket.Listener {
        private final String id = UUID.randomUUID().toString();
        private final URI uri;
        private final WebSocketConfig config;
        private final WebSocketListener listener;
        private final CompletableFuture<Result<WebSocketConnection>> firstConnection;
        private final StringBuilder textFragments = new StringBuilder();
        private int textFragmentBytes;
        private final ByteArrayOutputStream binaryFragments = new ByteArrayOutputStream();
        private volatile WebSocket socket;
        private volatile WebSocketStatus status = WebSocketStatus.CONNECTING;
        private volatile boolean intentionalClose;
        private volatile int reconnects;
        private volatile ScheduledFuture<?> pingTask;
        private volatile ScheduledFuture<?> reconnectTask;
        private volatile boolean reconnectScheduled;

        private ManagedConnection(URI uri, WebSocketConfig config, WebSocketListener listener,
                                  CompletableFuture<Result<WebSocketConnection>> firstConnection) {
            this.uri = uri;
            this.config = java.util.Objects.requireNonNull(config, "config");
            this.listener = java.util.Objects.requireNonNull(listener, "listener");
            this.firstConnection = firstConnection;
        }

        private void open() {
            if (closed || intentionalClose) return;
            status = reconnects == 0 ? WebSocketStatus.CONNECTING : WebSocketStatus.RECONNECTING;
            CompletableFuture.runAsync(() -> validateResolvedUri(uri, config), executor)
                    .orTimeout(config.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .thenCompose(ignored -> {
                        if (closed || intentionalClose) {
                            return CompletableFuture.failedFuture(new IllegalStateException("WebSocket connection cancelled"));
                        }
                        return client.newWebSocketBuilder().connectTimeout(config.connectTimeout()).buildAsync(uri, this);
                    })
                    .whenComplete((connected, failure) -> {
                        if (closed || intentionalClose) return;
                        if (failure != null) {
                            Throwable cause = unwrap(failure);
                            notifyError(cause);
                            reconnectOrFail();
                        }
                    });
        }

        @Override public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            status = WebSocketStatus.CONNECTED;
            reconnects = 0;
            webSocket.request(1);
            if (!firstConnection.isDone()) firstConnection.complete(Result.success(this));
            invoke(() -> listener.onConnected(this));
            Duration interval = config.pingInterval();
            if (!interval.isZero()) {
                pingTask = timer.scheduleAtFixedRate(() -> {
                    WebSocket current = socket;
                    if (current != null && status == WebSocketStatus.CONNECTED) {
                        current.sendPing(ByteBuffer.allocate(0)).exceptionally(failure -> {
                            notifyError(unwrap(failure));
                            return null;
                        });
                    }
                }, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
            }
        }

        @Override public java.util.concurrent.CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String fragment = data.toString();
            int fragmentBytes = fragment.getBytes(StandardCharsets.UTF_8).length;
            if (fragmentBytes > config.maximumMessageBytes() - textFragmentBytes) {
                rejectOversized();
                return CompletableFuture.completedFuture(null);
            }
            textFragments.append(fragment);
            textFragmentBytes += fragmentBytes;
            if (last) {
                String text = textFragments.toString();
                textFragments.setLength(0);
                textFragmentBytes = 0;
                invoke(() -> listener.onText(this, text));
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override public java.util.concurrent.CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (data.remaining() > config.maximumMessageBytes() - binaryFragments.size()) {
                rejectOversized();
                return CompletableFuture.completedFuture(null);
            }
            byte[] fragment = new byte[data.remaining()];
            data.get(fragment);
            if (!append(binaryFragments, fragment)) return CompletableFuture.completedFuture(null);
            if (last) {
                invoke(() -> listener.onBinary(this, ByteBuffer.wrap(binaryFragments.toByteArray()).asReadOnlyBuffer()));
                binaryFragments.reset();
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override public java.util.concurrent.CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override public java.util.concurrent.CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override public java.util.concurrent.CompletionStage<?> onClose(WebSocket webSocket, int code, String reason) {
            socket = null;
            cancelPing();
            invoke(() -> listener.onDisconnected(this, code, reason));
            if (intentionalClose || closed) {
                status = WebSocketStatus.CLOSED;
                connections.remove(this);
            } else {
                reconnectOrFail();
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override public void onError(WebSocket webSocket, Throwable failure) {
            notifyError(failure);
            if (!intentionalClose && status == WebSocketStatus.CONNECTED) reconnectOrFail();
        }

        @Override public String id() { return id; }
        @Override public WebSocketStatus status() { return status; }

        @Override public CompletableFuture<Void> sendText(String message) {
            if (message == null) return CompletableFuture.failedFuture(new IllegalArgumentException("message is required"));
            if (message.getBytes(StandardCharsets.UTF_8).length > config.maximumMessageBytes()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("WebSocket message exceeds configured size limit"));
            }
            WebSocket current = socket;
            if (current == null || status != WebSocketStatus.CONNECTED) {
                return CompletableFuture.failedFuture(new IllegalStateException("WebSocket is not connected"));
            }
            return current.sendText(message, true).thenApply(ignored -> null);
        }

        @Override public CompletableFuture<Void> sendBinary(ByteBuffer message) {
            if (message == null) return CompletableFuture.failedFuture(new IllegalArgumentException("message is required"));
            if (message.remaining() > config.maximumMessageBytes()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("WebSocket message exceeds configured size limit"));
            }
            WebSocket current = socket;
            if (current == null || status != WebSocketStatus.CONNECTED) {
                return CompletableFuture.failedFuture(new IllegalStateException("WebSocket is not connected"));
            }
            return current.sendBinary(message.asReadOnlyBuffer(), true).thenApply(ignored -> null);
        }

        @Override public CompletableFuture<Void> close(int code, String reason) {
            if (code < 1000 || code > 4999 || code == 1004 || code == 1005 || code == 1006 || code == 1015
                    || reason == null || reason.getBytes(StandardCharsets.UTF_8).length > 123) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid WebSocket close code or reason"));
            }
            intentionalClose = true;
            status = WebSocketStatus.CLOSING;
            cancelPing();
            ScheduledFuture<?> retry = reconnectTask;
            if (retry != null) retry.cancel(true);
            reconnectScheduled = false;
            WebSocket current = socket;
            if (current == null) {
                status = WebSocketStatus.CLOSED;
                connections.remove(this);
                if (!firstConnection.isDone()) {
                    firstConnection.complete(Result.failure(
                            new IllegalStateException("WebSocket connection closed before opening")));
                }
                return CompletableFuture.completedFuture(null);
            }
            return current.sendClose(code, reason).thenApply(ignored -> {
                status = WebSocketStatus.CLOSED;
                connections.remove(this);
                return null;
            });
        }

        private void reconnectOrFail() {
            cancelPing();
            if (intentionalClose || closed) {
                status = WebSocketStatus.CLOSED;
                connections.remove(this);
                return;
            }
            if (reconnectScheduled) return;
            if (reconnects >= config.maximumReconnectAttempts()) {
                status = WebSocketStatus.FAILED;
                connections.remove(this);
                if (!firstConnection.isDone()) {
                    firstConnection.complete(Result.failure(new IllegalStateException("WebSocket reconnect limit reached")));
                }
                return;
            }
            reconnects++;
            status = WebSocketStatus.RECONNECTING;
            reconnectScheduled = true;
            reconnectTask = timer.schedule(() -> {
                reconnectScheduled = false;
                open();
            }, config.reconnectDelay().toMillis(), TimeUnit.MILLISECONDS);
        }

        private boolean append(ByteArrayOutputStream target, byte[] bytes) {
            if (bytes.length > config.maximumMessageBytes() - target.size()) {
                target.reset();
                rejectOversized();
                return false;
            }
            target.write(bytes, 0, bytes.length);
            return true;
        }

        private void rejectOversized() {
            textFragments.setLength(0);
            textFragmentBytes = 0;
            binaryFragments.reset();
            notifyError(new IllegalArgumentException("Incoming WebSocket message exceeded configured size limit"));
            close(1009, "message too large");
        }

        private void cancelPing() {
            ScheduledFuture<?> task = pingTask;
            if (task != null) task.cancel(true);
            pingTask = null;
        }

        private void abort() {
            WebSocket current = socket;
            if (current != null) current.abort();
            status = WebSocketStatus.CLOSED;
            connections.remove(this);
        }

        private void notifyError(Throwable failure) {
            try {
                listener.onError(this, failure);
            } catch (RuntimeException callbackFailure) {
                failure.addSuppressed(callbackFailure);
                LOGGER.log(System.Logger.Level.WARNING, "WebSocket error handler failed", failure);
            }
        }

        private void invoke(Runnable callback) {
            try {
                callback.run();
            } catch (RuntimeException failure) {
                notifyError(failure);
            }
        }
    }

    private static Throwable unwrap(Throwable failure) {
        while (failure instanceof CompletionException && failure.getCause() != null) failure = failure.getCause();
        return failure;
    }
}
