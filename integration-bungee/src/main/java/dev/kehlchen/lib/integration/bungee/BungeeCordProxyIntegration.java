package dev.kehlchen.lib.integration.bungee;

import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.integration.proxy.ProxyIntegration;
import dev.kehlchen.lib.api.integration.proxy.ProxyMessageHandler;
import dev.kehlchen.lib.api.integration.proxy.ProxyPlayerInfo;
import dev.kehlchen.lib.api.integration.proxy.ProxyServerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.messaging.PluginMessageListener;

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

/** BungeeCord proxy transport over Bukkit's player-carried plugin messaging channel. Paper only; not Folia-safe. */
public final class BungeeCordProxyIntegration implements ProxyIntegration, PluginMessageListener {
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final int MAX_PAYLOAD_BYTES = 32_766;
    private final Plugin plugin;
    private final Map<RequestKey, CopyOnWriteArrayList<PendingRequest<?>>> pending = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<ListenerRegistration>> listeners = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    public BungeeCordProxyIntegration(Plugin plugin) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
    }

    @Override public String name() { return "bungeecord"; }
    @Override public String version() { return "1.0"; }
    @Override public boolean isAvailable() { return plugin.isEnabled(); }

    @Override public void onEnable() {
        requireServerThread();
        if (enabled) return;
        if (!plugin.isEnabled()) throw new IllegalStateException("Owning plugin is disabled");
        enabled = true;
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
    }

    @Override public CompletableFuture<Result<Void>> connect(UUID playerId, String server) {
        if (!enabled) return completedFailure("BungeeCord integration is not enabled");
        if (server == null || server.isBlank() || server.length() > 128) return completedFailure("Invalid target server");
        Player player = onlinePlayer(playerId);
        if (player == null) return completedFailure("Player is not online");
        try {
            return completed(send(player, data("Connect", out -> out.writeUTF(server))));
        } catch (RuntimeException failure) {
            return completed(Result.failure(failure));
        }
    }

    @Override public CompletableFuture<Result<String>> currentServer(UUID playerId) {
        if (!enabled) return completedFailure("BungeeCord integration is not enabled");
        return request(playerId, "GetServer", "", input -> input.readUTF());
    }

    @Override public CompletableFuture<Result<ProxyServerInfo>> serverInfo(UUID carrierPlayerId, String server) {
        if (!enabled) return completedFailure("BungeeCord integration is not enabled");
        if (!validServer(server)) return completedFailure("Invalid server name");
        return request(carrierPlayerId, "PlayerCount", server,
                input -> new ProxyServerInfo(input.readUTF(), input.readInt(), null));
    }

    @Override public CompletableFuture<Result<List<String>>> servers(UUID carrierPlayerId) {
        if (!enabled) return completedFailure("BungeeCord integration is not enabled");
        return request(carrierPlayerId, "GetServers", "", input -> {
            String value = input.readUTF();
            if (value.isBlank()) return List.of();
            return List.of(value.split(","));
        });
    }

    @Override public CompletableFuture<Result<ProxyPlayerInfo>> playerInfo(UUID carrierPlayerId, UUID targetPlayerId) {
        if (!enabled) return completedFailure("BungeeCord integration is not enabled");
        if (!java.util.Objects.equals(carrierPlayerId, targetPlayerId)) {
            return completedFailure("BungeeCord only exposes current-player information through this carrier");
        }
        Player player = onlinePlayer(carrierPlayerId);
        if (player == null) return completedFailure("Player is not online");
        return currentServer(carrierPlayerId).thenApply(result -> result.isSuccess()
                ? Result.success(new ProxyPlayerInfo(player.getUniqueId(), player.getName(), result.value().orElseThrow()))
                : Result.failure(result.error().orElseThrow()));
    }

    @Override public Result<Void> sendMessage(UUID playerId, String channel, byte[] payload) {
        if (!enabled) return Result.failure(new IllegalStateException("BungeeCord integration is not enabled"));
        if (!validChannel(channel) || payload == null || payload.length > MAX_PAYLOAD_BYTES - 256) {
            return Result.failure(new IllegalArgumentException("Invalid or oversized proxy message"));
        }
        Player player = onlinePlayer(playerId);
        if (player == null) return Result.failure(new IllegalStateException("Player is not online"));
        try {
            byte[] message = data("Forward", out -> {
                out.writeUTF("ALL");
                out.writeUTF(channel);
                out.writeShort(payload.length);
                out.write(payload);
            });
            return send(player, message);
        } catch (RuntimeException failure) {
            return Result.failure(failure);
        }
    }

    @Override public Resource listen(String channel, ProxyMessageHandler handler) {
        requireServerThread();
        ensureEnabled();
        if (!validChannel(channel)) throw new IllegalArgumentException("Invalid plugin message channel");
        ListenerRegistration registration = new ListenerRegistration(channel, handler);
        CopyOnWriteArrayList<ListenerRegistration> channelListeners =
                listeners.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>());
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, registration);
        } catch (RuntimeException failure) {
            if (channelListeners.isEmpty()) listeners.remove(channel, channelListeners);
            throw failure;
        }
        channelListeners.add(registration);
        return registration;
    }

    @Override public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!enabled || !BUNGEE_CHANNEL.equals(channel) || message.length > MAX_PAYLOAD_BYTES) return;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            String subChannel = input.readUTF();
            RequestKey key = new RequestKey(carrier.getUniqueId(), subChannel, "");
            if ("PlayerCount".equals(subChannel)) {
                input.mark(message.length);
                String server = input.readUTF();
                key = new RequestKey(carrier.getUniqueId(), subChannel, server);
                input.reset();
            }
            CopyOnWriteArrayList<PendingRequest<?>> requests = pending.get(key);
            if (requests != null && !requests.isEmpty()) {
                PendingRequest<?> request = requests.remove(0);
                if (requests.isEmpty()) pending.remove(key, requests);
                request.complete(input);
            }
        } catch (IOException failure) {
            plugin.getLogger().warning("Dropped malformed BungeeCord response");
        }
    }

    private <T> CompletableFuture<Result<T>> request(UUID playerId, String subChannel, String parameter,
                                                      Decoder<T> decoder) {
        Player player = onlinePlayer(playerId);
        if (player == null) return completedFailure("Player is not online");
        ensureEnabled();
        RequestKey key = new RequestKey(playerId, subChannel, parameter);
        PendingRequest<T> request = new PendingRequest<>(decoder);
        pending.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(request);
        Result<Void> sent;
        try {
            byte[] message = data(subChannel, out -> {
                if (!parameter.isEmpty()) out.writeUTF(parameter);
            });
            sent = send(player, message);
        } catch (RuntimeException failure) {
            removePending(key, request);
            request.future.complete(Result.failure(failure));
            return request.future;
        }
        if (!sent.isSuccess()) {
            removePending(key, request);
            request.future.complete(Result.failure(sent.error().orElseThrow()));
            return request.future;
        }
        request.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (removePending(key, request)) {
                request.future.complete(Result.failure(new java.util.concurrent.TimeoutException(
                        "BungeeCord did not answer the " + subChannel + " request")));
            }
        }, 100L);
        if (request.future.isDone()) request.cancelTimeout();
        return request.future;
    }

    private Result<Void> send(Player player, byte[] payload) {
        ensureEnabled();
        if (!Bukkit.isPrimaryThread()) return Result.failure(new IllegalStateException("BungeeCord messaging requires the server thread"));
        try {
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, payload);
            return Result.success(null);
        } catch (RuntimeException failure) {
            return Result.failure(failure);
        }
    }

    private Player onlinePlayer(UUID id) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("BungeeCord player operations require the server thread");
        return id == null ? null : Bukkit.getPlayer(id);
    }

    private boolean removePending(RequestKey key, PendingRequest<?> request) {
        CopyOnWriteArrayList<PendingRequest<?>> requests = pending.get(key);
        if (requests == null) return false;
        requests.remove(request);
        if (requests.isEmpty()) pending.remove(key, requests);
        return true;
    }

    private static byte[] data(String subChannel, IoWriter writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeUTF(subChannel);
                writer.write(output);
            }
            if (bytes.size() > MAX_PAYLOAD_BYTES) throw new IllegalArgumentException("Proxy message is too large");
            return bytes.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to encode proxy message", failure);
        }
    }

    private static boolean validChannel(String channel) {
        return channel != null && !channel.isBlank() && channel.length() <= 128
                && channel.getBytes(StandardCharsets.UTF_8).length <= 128
                && channel.indexOf('\0') < 0;
    }

    private static boolean validServer(String server) {
        return server != null && !server.isBlank() && server.length() <= 128
                && server.getBytes(StandardCharsets.UTF_8).length <= 128
                && server.indexOf('\0') < 0;
    }

    private void requireServerThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("BungeeCord integration requires the server thread");
    }

    private void ensureEnabled() {
        if (!enabled) throw new IllegalStateException("BungeeCord integration is not enabled");
    }

    @Override public void onDisable() {
        requireServerThread();
        if (!enabled) return;
        enabled = false;
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        listeners.values().forEach(channelListeners -> channelListeners.forEach(registration ->
                Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, registration.channel, registration)));
        listeners.clear();
        pending.values().forEach(requests -> requests.forEach(request ->
                request.fail(new IllegalStateException("BungeeCord integration shut down"))));
        pending.clear();
    }

    private static <T> CompletableFuture<Result<T>> completedFailure(String message) {
        return CompletableFuture.completedFuture(Result.failure(new IllegalStateException(message)));
    }

    private static CompletableFuture<Result<Void>> completed(Result<Void> result) {
        return CompletableFuture.completedFuture(result);
    }

    @FunctionalInterface private interface IoWriter { void write(DataOutputStream output) throws IOException; }
    @FunctionalInterface private interface Decoder<T> { T decode(DataInputStream input) throws IOException; }

    private record RequestKey(UUID player, String subChannel, String parameter) {}

    private final class PendingRequest<T> {
        private final Decoder<T> decoder;
        private final CompletableFuture<Result<T>> future = new CompletableFuture<>();
        private volatile BukkitTask timeoutTask;
        private PendingRequest(Decoder<T> decoder) { this.decoder = decoder; }
        private void complete(DataInputStream input) {
            cancelTimeout();
            try {
                future.complete(Result.success(decoder.decode(input)));
            } catch (IOException | RuntimeException failure) {
                future.complete(Result.failure(failure));
            }
        }
        private void fail(Throwable failure) {
            cancelTimeout();
            future.complete(Result.failure(failure));
        }
        private void cancelTimeout() {
            BukkitTask task = timeoutTask;
            timeoutTask = null;
            if (task != null) task.cancel();
        }
    }

    private final class ListenerRegistration implements Resource, PluginMessageListener {
        private final String channel;
        private final ProxyMessageHandler handler;
        private volatile boolean closed;

        private ListenerRegistration(String channel, ProxyMessageHandler handler) {
            this.channel = channel;
            this.handler = java.util.Objects.requireNonNull(handler, "handler");
        }

        @Override public void onPluginMessageReceived(String incoming, Player carrier, byte[] payload) {
            if (!closed && channel.equals(incoming) && payload.length <= MAX_PAYLOAD_BYTES) {
                try {
                    handler.onMessage(java.util.Optional.of(carrier.getUniqueId()), channel, payload.clone());
                } catch (RuntimeException failure) {
                    plugin.getLogger().warning("Proxy message handler failed (" + failure.getClass().getName() + ")");
                }
            }
        }

        @Override public void close() {
            if (closed) return;
            requireServerThread();
            closed = true;
            CopyOnWriteArrayList<ListenerRegistration> channelListeners = listeners.get(channel);
            if (channelListeners == null) return;
            channelListeners.remove(this);
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
            if (channelListeners.isEmpty()) listeners.remove(channel, channelListeners);
        }
    }
}
