package dev.kehlchen.lib.api.virtual;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;

import java.util.Collection;
import java.util.UUID;

public interface HologramService extends Lifecycle {
    Result<Hologram> create(HologramConfig config);
    Result<Void> remove(UUID id);
    Collection<Hologram> holograms();
}
