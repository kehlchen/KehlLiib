package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.Resource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Closes registered resources in reverse registration order. */
public final class ResourceManagerImpl implements AutoCloseable {
    private final Deque<Resource> resources = new ArrayDeque<>();
    private volatile boolean closed;

    public void register(Resource resource) {
        if (closed) {
            throw new IllegalStateException("Resource manager is closed");
        }
        resources.push(Objects.requireNonNull(resource, "resource"));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Throwable failure = null;
        while (!resources.isEmpty()) {
            try {
                resources.pop().close();
            } catch (Throwable error) {
                if (failure == null) {
                    failure = error;
                } else {
                    failure.addSuppressed(error);
                }
            }
        }
        if (failure != null) {
            throw new IllegalStateException("One or more resources failed to close", failure);
        }
    }
}
