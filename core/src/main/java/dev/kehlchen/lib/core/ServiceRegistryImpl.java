package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.ServiceRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Concurrent implementation of {@link ServiceRegistry}. */
public final class ServiceRegistryImpl implements ServiceRegistry {
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private boolean closed;

    @Override
    public synchronized <T> void register(Class<T> type, T implementation) {
        ensureOpen();
        services.put(java.util.Objects.requireNonNull(type, "type"),
                java.util.Objects.requireNonNull(implementation, "implementation"));
    }

    @Override
    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(type.cast(services.get(java.util.Objects.requireNonNull(type, "type"))));
    }

    @Override
    public <T> T getRequired(Class<T> type) {
        return get(type).orElseThrow(() ->
                new IllegalStateException("Required service is not registered: " + type.getName()));
    }

    @Override
    public <T> T unregister(Class<T> type) {
        return type.cast(services.remove(java.util.Objects.requireNonNull(type, "type")));
    }

    @Override
    public synchronized void close() {
        closed = true;
        services.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Service registry is closed");
        }
    }
}
