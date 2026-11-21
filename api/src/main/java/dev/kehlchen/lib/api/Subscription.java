package dev.kehlchen.lib.api;

/** Represents a registered event listener that can be cancelled. */
@FunctionalInterface
public interface Subscription extends AutoCloseable {
    void close();
}
