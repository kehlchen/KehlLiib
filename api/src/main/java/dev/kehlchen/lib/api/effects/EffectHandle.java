package dev.kehlchen.lib.api.effects;

import dev.kehlchen.lib.api.Resource;

/** Handle for a cancellable repeating or sequential effect. */
public interface EffectHandle extends Resource {
    boolean isActive();
    void cancel();
}
