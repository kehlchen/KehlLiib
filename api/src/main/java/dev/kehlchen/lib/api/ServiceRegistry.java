package dev.kehlchen.lib.api;

import java.util.Optional;

/** Thread-safe registry of implementation services. */
public interface ServiceRegistry {
    <T> void register(Class<T> type, T implementation);

    <T> Optional<T> get(Class<T> type);

    <T> T getRequired(Class<T> type);

    <T> T unregister(Class<T> type);

    void close();
}
