package dev.kehlchen.lib.api.integration;

import dev.kehlchen.lib.api.Lifecycle;

public interface Integration extends Lifecycle {
    boolean isAvailable();
}
