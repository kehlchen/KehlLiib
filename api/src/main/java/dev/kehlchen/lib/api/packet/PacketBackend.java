package dev.kehlchen.lib.api.packet;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;

import java.util.UUID;

/** Optional implementation boundary for packet send/interception backends. */
public interface PacketBackend extends Lifecycle {
    String id();
    boolean interceptionSupported();
    boolean modificationSupported();
    Result<Void> send(UUID playerId, PacketDirection direction, String type,
                       MapPacket packet);
    PacketRegistration registerListener(UUID playerId, PacketDirection direction,
                                        String type, PacketPriority priority,
                                        PacketListener listener);
    void close();
}
