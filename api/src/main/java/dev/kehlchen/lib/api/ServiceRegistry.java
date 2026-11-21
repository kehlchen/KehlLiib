package dev.kehlchen.lib.api;

import java.util.Optional;

/**
 * Thread-safe registry of public service implementations.
 * Registrations are keyed by interface or class and are removed by close.
 */
public interface ServiceRegistry {
    /** Registers an implementation for a type. */
    <T> void register(Class<T> type, T implementation);

    /** Finds a registered implementation. */
    <T> Optional<T> get(Class<T> type);

    /** Finds a required implementation or throws {@link IllegalStateException}. */
    <T> T getRequired(Class<T> type);

    /** Removes and returns a registration. */
    <T> T unregister(Class<T> type);

    void close();
}
