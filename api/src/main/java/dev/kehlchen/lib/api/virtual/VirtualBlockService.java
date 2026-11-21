package dev.kehlchen.lib.api.virtual;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

public interface VirtualBlockService extends Lifecycle {
    Result<Void> set(Location location, BlockData data, UUID... viewers);
    Result<Void> set(Location location, BlockData data, Duration expiresAfter, UUID... viewers);
    Result<Void> restore(Location location, UUID... viewers);
    Collection<Location> locations(UUID playerId);
    int maximumBlocks();
    void clear();
}
