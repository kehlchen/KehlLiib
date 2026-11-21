package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.EventBus;
import dev.kehlchen.lib.api.Subscription;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Thread-safe internal event bus. */
public final class EventBusImpl implements EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    private volatile boolean closed;

    @Override
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
        ensureOpen();
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        CopyOnWriteArrayList<Consumer<?>> list =
                listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>());
        list.add(listener);
        return () -> list.remove(listener);
    }

    @Override
    public <T> void publish(T event) {
        ensureOpen();
        Objects.requireNonNull(event, "event");
        for (Consumer<?> listener : listeners
                .getOrDefault(event.getClass(), new CopyOnWriteArrayList<>())) {
            @SuppressWarnings("unchecked")
            Consumer<T> typed = (Consumer<T>) listener;
            typed.accept(event);
        }
    }

    @Override
    public void close() {
        closed = true;
        listeners.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Event bus is closed");
        }
    }
}
