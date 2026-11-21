package dev.kehlchen.lib.api.nms;

import dev.kehlchen.lib.api.Lifecycle;

import java.util.Optional;

/** Registry for explicitly supported, version-specific optional server adapters. */
public interface ServerAdapterRegistry extends Lifecycle {
    Optional<ServerAdapter> compatibleAdapter();
    void register(ServerAdapter adapter);
    String detectedVersion();
}
