package dev.kehlchen.lib.core.nms;

import dev.kehlchen.lib.api.nms.ServerAdapter;
import dev.kehlchen.lib.api.nms.ServerAdapterRegistry;
import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CoreServerAdapterRegistry implements ServerAdapterRegistry {
    private final ConcurrentHashMap<String, ServerAdapter> adapters = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    @Override public String name() { return "server-adapter-registry"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public synchronized void onDisable() {
        enabled = false;
        for (ServerAdapter adapter : adapters.values()) adapter.onDisable();
        adapters.clear();
    }

    @Override
    public Optional<ServerAdapter> compatibleAdapter() {
        if (!enabled) return Optional.empty();
        return adapters.values().stream()
                .filter(adapter -> adapter.supports(detectedVersion()))
                .findFirst();
    }

    @Override
    public synchronized void register(ServerAdapter adapter) {
        adapters.put(adapter.id(), adapter);
        if (enabled && adapter.supports(detectedVersion())) adapter.onEnable();
    }

    @Override
    public String detectedVersion() {
        try {
            return Bukkit.getMinecraftVersion();
        } catch (RuntimeException ignored) {
            return "unknown";
        }
    }
}
