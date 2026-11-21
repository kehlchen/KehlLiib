package dev.kehlchen.lib.api.messaging;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface PluginMessageService extends Lifecycle {
    void register(Plugin owner, NamespacedKey channel, PluginMessageHandler handler);
    boolean unregister(Plugin owner, NamespacedKey channel);
    Result<Void> send(Plugin owner, Player carrier, NamespacedKey channel, byte[] payload);

    default Result<Void> sendString(Plugin owner, Player carrier, NamespacedKey channel, String message) {
        return send(owner, carrier, channel, message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    default <T> Result<Void> send(Plugin owner, Player carrier, NamespacedKey channel, T value, PayloadCodec<T> codec) {
        try {
            return send(owner, carrier, channel, codec.encode(value));
        } catch (Exception failure) {
            return Result.failure(failure);
        }
    }
}
