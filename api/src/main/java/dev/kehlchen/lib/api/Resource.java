package dev.kehlchen.lib.api;

/** A resource that may be closed deterministically. */
public interface Resource extends AutoCloseable {
    @Override
    void close();
}
