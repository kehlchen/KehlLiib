package dev.kehlchen.lib.core.messaging;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.messaging.PluginMessageHandler;
import dev.kehlchen.lib.api.messaging.PluginMessagePayload;
import dev.kehlchen.lib.api.messaging.PluginMessageService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Bukkit plugin messages use an online player as the actual transport carrier. */
public final class PaperPluginMessageService implements PluginMessageService {
    public static final int MAX_PAYLOAD_BYTES = PluginMessagePayload.MAX_BYTES;
    private final Map<ChannelKey, Registration> registrations = new ConcurrentHashMap<>();
    private volatile boolean closed;

    @Override public String name() { return "plugin-messaging"; }
    @Override public String version() { return "1.0"; }

    @Override public void register(Plugin owner, NamespacedKey channel, PluginMessageHandler handler) {
        requireServerThread();
        ensureOpen();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");
        ChannelKey key = new ChannelKey(owner, channel);
        Registration registration = new Registration(owner, channel, handler);
        if (registrations.putIfAbsent(key, registration) != null) {
            throw new IllegalArgumentException("Channel is already registered for this plugin: " + channel);
        }
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(owner, channel.toString(), registration);
            Bukkit.getMessenger().registerOutgoingPluginChannel(owner, channel.toString());
        } catch (RuntimeException failure) {
            registrations.remove(key, registration);
            Bukkit.getMessenger().unregisterIncomingPluginChannel(owner, channel.toString(), registration);
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(owner, channel.toString());
            throw failure;
        }
    }

    @Override public boolean unregister(Plugin owner, NamespacedKey channel) {
        requireServerThread();
        Registration registration = registrations.remove(new ChannelKey(owner, channel));
        if (registration == null) return false;
        Bukkit.getMessenger().unregisterIncomingPluginChannel(owner, channel.toString(), registration);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(owner, channel.toString());
        return true;
    }

    @Override public Result<Void> send(Plugin owner, Player carrier, NamespacedKey channel, byte[] payload) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(carrier, "carrier");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(payload, "payload");
        if (!Bukkit.isPrimaryThread()) {
            return Result.failure(new IllegalStateException("Plugin messages must be sent on the server thread"));
        }
        if (closed) return Result.failure(new IllegalStateException("Plugin message service is closed"));
        if (!carrier.isOnline()) return Result.failure(new IllegalStateException("Plugin message carrier is not online"));
        if (payload.length > MAX_PAYLOAD_BYTES) {
            return Result.failure(new IllegalArgumentException("Plugin message exceeds " + MAX_PAYLOAD_BYTES + " bytes"));
        }
        if (!registrations.containsKey(new ChannelKey(owner, channel))) {
            return Result.failure(new IllegalArgumentException("No outgoing channel is registered: " + channel));
        }
        try {
            carrier.sendPluginMessage(owner, channel.toString(), payload.clone());
            return Result.success(null);
        } catch (RuntimeException failure) {
            return Result.failure(failure);
        }
    }

    @Override public void onEnable() {
        requireServerThread();
        ensureOpen();
    }

    @Override public void onDisable() {
        if (closed) return;
        requireServerThread();
        closed = true;
        registrations.values().forEach(registration -> {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(registration.owner,
                    registration.channel.toString(), registration);
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(registration.owner, registration.channel.toString());
        });
        registrations.clear();
    }

    private void requireServerThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Plugin channel registration requires the server thread");
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Plugin message service is closed");
    }

    private record ChannelKey(Plugin owner, NamespacedKey channel) {}

    private static final class Registration implements org.bukkit.plugin.messaging.PluginMessageListener {
        private final Plugin owner;
        private final NamespacedKey channel;
        private final PluginMessageHandler handler;

        private Registration(Plugin owner, NamespacedKey channel, PluginMessageHandler handler) {
            this.owner = owner;
            this.channel = channel;
            this.handler = handler;
        }

        @Override public void onPluginMessageReceived(String incoming, Player carrier, byte[] message) {
            if (!incoming.equals(channel.toString())) return;
            if (message.length > MAX_PAYLOAD_BYTES) {
                owner.getLogger().warning("Dropped oversized plugin message on channel " + channel);
                return;
            }
            try {
                handler.onMessage(carrier, channel, message.clone());
            } catch (RuntimeException failure) {
                owner.getLogger().log(Level.WARNING, "Plugin message handler failed for " + channel, failure);
            }
        }
    }
}
