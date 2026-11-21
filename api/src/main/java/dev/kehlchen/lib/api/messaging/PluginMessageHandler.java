package dev.kehlchen.lib.api.messaging;

import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;

@FunctionalInterface
public interface PluginMessageHandler {
    void onMessage(Player carrier, NamespacedKey channel, byte[] payload);
}
