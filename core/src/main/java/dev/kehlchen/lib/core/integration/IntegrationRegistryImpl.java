package dev.kehlchen.lib.core.integration;

import dev.kehlchen.lib.api.integration.Integration;
import dev.kehlchen.lib.api.integration.IntegrationRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class IntegrationRegistryImpl implements IntegrationRegistry {
    private final Map<String, Integration> integrations = new LinkedHashMap<>();
    private final java.util.List<Integration> started = new java.util.ArrayList<>();
    private boolean enabled;
    private boolean closed;

    @Override public String name() { return "integrations"; }
    @Override public String version() { return "1.0"; }

    @Override public synchronized void register(Integration integration) {
        ensureOpen();
        if (integrations.containsKey(integration.name())) {
            throw new IllegalArgumentException("Integration already registered: " + integration.name());
        }
        integrations.put(integration.name(), integration);
        if (enabled && integration.isAvailable()) {
            try {
                integration.onEnable();
                started.add(integration);
            } catch (RuntimeException failure) {
                try {
                    integration.onDisable();
                } catch (RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                integrations.remove(integration.name());
                throw new IllegalStateException("Failed to initialize integration: " + integration.name(), failure);
            }
        }
    }

    @Override public synchronized boolean unregister(String name) {
        Integration integration = integrations.remove(name);
        if (integration == null) return false;
        if (started.remove(integration)) integration.onDisable();
        return true;
    }

    @Override public synchronized Optional<Integration> find(String name) {
        return Optional.ofNullable(integrations.get(name));
    }

    @Override public synchronized Collection<Integration> integrations() {
        return java.util.List.copyOf(integrations.values());
    }

    @Override public synchronized void onEnable() {
        ensureOpen();
        if (enabled) return;
        enabled = true;
        try {
            for (Integration integration : integrations.values()) {
                if (!integration.isAvailable()) continue;
                try {
                    integration.onEnable();
                    started.add(integration);
                } catch (RuntimeException moduleFailure) {
                    try {
                        integration.onDisable();
                    } catch (RuntimeException cleanupFailure) {
                        moduleFailure.addSuppressed(cleanupFailure);
                    }
                    throw moduleFailure;
                }
            }
        } catch (RuntimeException failure) {
            for (int i = started.size() - 1; i >= 0; i--) {
                try {
                    started.get(i).onDisable();
                } catch (RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            started.clear();
            enabled = false;
            throw new IllegalStateException("Failed to enable integration registry", failure);
        }
    }

    @Override public synchronized void onDisable() {
        if (closed) return;
        closed = true;
        RuntimeException failure = null;
        for (int i = started.size() - 1; i >= 0; i--) {
            try {
                started.get(i).onDisable();
            } catch (RuntimeException error) {
                if (failure == null) failure = new IllegalStateException("Integration shutdown failed");
                failure.addSuppressed(error);
            }
        }
        integrations.clear();
        started.clear();
        enabled = false;
        if (failure != null) throw failure;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Integration registry is closed");
    }
}
