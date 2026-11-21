package dev.kehlchen.lib.api.packet;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;

import java.util.Optional;
import java.util.UUID;

public interface PacketService extends Lifecycle {
    Optional<PacketBackend> backend();
    boolean interceptionAvailable();
    boolean modificationAvailable();
    Result<Void> send(UUID playerId, PacketDirection direction, String type, MapPacket packet);
    Result<PacketRegistration> listen(UUID playerId, PacketDirection direction,
                                      String type, PacketPriority priority,
                                      PacketListener listener);
    Result<PacketRegistration> listen(PacketDirection direction, String type,
                                      PacketPriority priority, PacketListener listener);
    void registerBackend(PacketBackend backend);
    void unregisterBackend(String backendId);
}
