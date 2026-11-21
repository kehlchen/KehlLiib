package dev.kehlchen.lib.integration.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.integration.proxy.ProxyIntegration;
import dev.kehlchen.lib.api.integration.proxy.ProxyMessageHandler;
import dev.kehlchen.lib.api.integration.proxy.ProxyPlayerInfo;
import dev.kehlchen.lib.api.integration.proxy.ProxyServerInfo;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Velocity-native KehlLib proxy plugin. It is not a Paper plugin and must be installed on Velocity only. */
@Plugin(id = "kehllib-velocity", name = "KehlLib Velocity", version = "1.0")
public final class KehlLibVelocityPlugin implements ProxyIntegration {
    private static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.create("kehllib", "proxy");
    private static final int MAX_PAYLOAD_BYTES = 32_766;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();
    private volatile boolean enabled;
    private volatile boolean closed;

    @Inject
    public KehlLibVelocityPlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Override public String name() { return "velocity"; }
    @Override public String version() { return "1.0"; }
    @Override public boolean isAvailable() { return enabled; }

    @Subscribe public void onProxyInitialize(ProxyInitializeEvent event) { onEnable(); }
    @Subscribe public void onProxyShutdown(ProxyShutdownEvent event) { onDisable(); }

    @Override public synchronized void onEnable() {
        if (enabled) return;
        if (closed) throw new IllegalStateException("Velocity integration cannot restart after shutdown");
        proxy.getChannelRegistrar().register(CHANNEL);
        enabled = true;
    }

    @Override public CompletableFuture<Result<Void>> connect(UUID playerId, String serverName) {
        if (!enabled) return completedFailure("Velocity integration is not enabled");
        if (playerId == null) return completedFailure("Player ID is required");
        if (serverName == null || serverName.isBlank()) return completedFailure("Target server is required");
        Player player = proxy.getPlayer(playerId).orElse(null);
        RegisteredServer server = proxy.getServer(serverName).orElse(null);
        if (player == null) return completedFailure("Player is not online");
        if (server == null) return completedFailure("Target server is not registered");
        return player.createConnectionRequest(server).connect().thenApply(result ->
                result.isSuccessful()
                        ? Result.success(null)
                        : Result.failure(new IllegalStateException(
                                "Velocity connection request failed: " + result.getStatus())));
    }

    @Override public CompletableFuture<Result<String>> currentServer(UUID playerId) {
        if (playerId == null) return completedFailure("Player ID is required");
        Player player = proxy.getPlayer(playerId).orElse(null);
        if (!enabled || player == null) return completedFailure("Player is not online or integration is disabled");
        return CompletableFuture.completedFuture(player.getCurrentServer()
                .<Result<String>>map(connection -> Result.success(connection.getServerInfo().getName()))
                .orElseGet(() -> Result.failure(new IllegalStateException("Player is not connected to a server"))));
    }

    @Override public CompletableFuture<Result<ProxyServerInfo>> serverInfo(UUID carrierPlayerId, String serverName) {
        if (serverName == null || serverName.isBlank()) return completedFailure("Server name is required");
        RegisteredServer server = proxy.getServer(serverName).orElse(null);
        if (!enabled || server == null) return completedFailure("Server is not registered or integration is disabled");
        return CompletableFuture.completedFuture(Result.success(new ProxyServerInfo(
                server.getServerInfo().getName(), server.getPlayersConnected().size(), null)));
    }

    @Override public CompletableFuture<Result<List<String>>> servers(UUID carrierPlayerId) {
        if (!enabled) return completedFailure("Velocity integration is not enabled");
        return CompletableFuture.completedFuture(Result.success(proxy.getAllServers().stream()
                .map(server -> server.getServerInfo().getName()).sorted().toList()));
    }

    @Override public CompletableFuture<Result<ProxyPlayerInfo>> playerInfo(UUID carrierPlayerId, UUID targetPlayerId) {
        if (targetPlayerId == null) return completedFailure("Player ID is required");
        Player player = proxy.getPlayer(targetPlayerId).orElse(null);
        if (!enabled || player == null) return completedFailure("Player is not online or integration is disabled");
        String currentServer = player.getCurrentServer().map(connection -> connection.getServerInfo().getName()).orElse(null);
        return CompletableFuture.completedFuture(Result.success(
                new ProxyPlayerInfo(player.getUniqueId(), player.getUsername(), currentServer)));
    }

    @Override public Result<Void> sendMessage(UUID playerId, String subChannel, byte[] payload) {
        if (!enabled) return Result.failure(new IllegalStateException("Velocity integration is not enabled"));
        if (playerId == null) return Result.failure(new IllegalArgumentException("Player ID is required"));
        if (!validMessage(subChannel, payload)) return Result.failure(new IllegalArgumentException("Invalid or oversized proxy message"));
        Player player = proxy.getPlayer(playerId).orElse(null);
        if (player == null) return Result.failure(new IllegalStateException("Player is not online"));
        ServerConnection connection = player.getCurrentServer().orElse(null);
        if (connection == null) return Result.failure(new IllegalStateException("Player is not connected to a server"));
        return connection.getServer().sendPluginMessage(CHANNEL, encode(subChannel, payload))
                ? Result.success(null) : Result.failure(new IllegalStateException("Velocity could not deliver the plugin message"));
    }

    @Override public Resource listen(String subChannel, ProxyMessageHandler handler) {
        if (!enabled) throw new IllegalStateException("Velocity integration is not enabled");
        if (!validChannel(subChannel)) throw new IllegalArgumentException("Invalid proxy message channel");
        MessageListener listener = new MessageListener(subChannel, handler);
        listeners.computeIfAbsent(subChannel, ignored -> new CopyOnWriteArrayList<>()).add(listener);
        return listener;
    }

    @Subscribe public void onPluginMessage(PluginMessageEvent event) {
        if (!enabled || !CHANNEL.equals(event.getIdentifier())) return;
        byte[] data = event.getData();
        if (data.length > MAX_PAYLOAD_BYTES) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());
            return;
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            String subChannel = input.readUTF();
            int length = input.readUnsignedShort();
            if (!validChannel(subChannel) || length > MAX_PAYLOAD_BYTES || length != input.available()) {
                event.setResult(PluginMessageEvent.ForwardResult.handled());
                return;
            }
            byte[] payload = input.readNBytes(length);
            UUID sourcePlayer = null;
            if (event.getSource() instanceof Player player) sourcePlayer = player.getUniqueId();
            else if (event.getSource() instanceof ServerConnection connection) {
                sourcePlayer = connection.getPlayer().getUniqueId();
            }
            for (MessageListener listener : listeners.getOrDefault(subChannel, new CopyOnWriteArrayList<>())) {
                if (!listener.closed) {
                    try {
                        listener.handler.onMessage(java.util.Optional.ofNullable(sourcePlayer), subChannel, payload.clone());
                    } catch (RuntimeException failure) {
                        logger.warn("Proxy message handler failed for {} ({})", subChannel, failure.getClass().getName());
                    }
                }
            }
            event.setResult(PluginMessageEvent.ForwardResult.handled());
        } catch (IOException failure) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());
            logger.warn("Dropped malformed KehlLib proxy message");
        }
    }

    private static byte[] encode(String subChannel, byte[] payload) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeUTF(subChannel);
                output.writeShort(payload.length);
                output.write(payload);
            }
            return bytes.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to encode proxy message", failure);
        }
    }

    private static boolean validMessage(String channel, byte[] payload) {
        return validChannel(channel) && payload != null && payload.length <= MAX_PAYLOAD_BYTES - 256;
    }

    private static boolean validChannel(String channel) {
        return channel != null && !channel.isBlank() && channel.length() <= 128
                && channel.getBytes(StandardCharsets.UTF_8).length <= 128;
    }

    @Override public synchronized void onDisable() {
        if (!enabled) return;
        enabled = false;
        closed = true;
        proxy.getChannelRegistrar().unregister(CHANNEL);
        listeners.values().forEach(handlers -> handlers.forEach(listener -> listener.closed = true));
        listeners.clear();
    }

    private static <T> CompletableFuture<Result<T>> completedFailure(String message) {
        return CompletableFuture.completedFuture(Result.failure(new IllegalStateException(message)));
    }

    private final class MessageListener implements Resource {
        private final String subChannel;
        private final ProxyMessageHandler handler;
        private volatile boolean closed;

        private MessageListener(String subChannel, ProxyMessageHandler handler) {
            this.subChannel = subChannel;
            this.handler = java.util.Objects.requireNonNull(handler, "handler");
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            CopyOnWriteArrayList<MessageListener> handlers = listeners.get(subChannel);
            if (handlers != null) {
                handlers.remove(this);
                if (handlers.isEmpty()) listeners.remove(subChannel, handlers);
            }
        }
    }
}
