package dev.kehlchen.lib.api.integration;

import dev.kehlchen.lib.api.Lifecycle;

import java.util.Collection;
import java.util.Optional;

public interface IntegrationRegistry extends Lifecycle {
    void register(Integration integration);
    boolean unregister(String name);
    Optional<Integration> find(String name);
    Collection<Integration> integrations();
}
