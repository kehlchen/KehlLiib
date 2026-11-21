package dev.kehlchen.lib.api;

/**
 * A resource whose lifetime is managed by the application or KehlLib bootstrap.
 * Close operations must be safe to call as part of plugin shutdown.
 */
public interface Resource extends AutoCloseable {
    @Override
    void close();
}
