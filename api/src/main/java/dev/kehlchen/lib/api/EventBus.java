package dev.kehlchen.lib.api;

import java.util.function.Consumer;

/** Minimal internal event bus. */
public interface EventBus {
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener);

    <T> void publish(T event);

    void close();
}
