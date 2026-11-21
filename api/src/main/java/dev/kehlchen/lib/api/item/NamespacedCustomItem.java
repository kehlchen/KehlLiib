package dev.kehlchen.lib.api.item;

import org.bukkit.NamespacedKey;

import java.util.Objects;

/** Stable identifier for a custom item. */
public record NamespacedCustomItem(NamespacedKey key) {
    public NamespacedCustomItem {
        Objects.requireNonNull(key, "key");
    }

    @Override
    public String toString() {
        return key.toString();
    }
}
