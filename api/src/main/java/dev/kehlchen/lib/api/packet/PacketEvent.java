package dev.kehlchen.lib.api.packet;

import java.util.Map;
import java.util.UUID;

/** Backend-independent packet observation or modification context. */
public interface PacketEvent {
    UUID playerId();
    PacketDirection direction();
    String type();
    Map<String, Object> metadata();
    boolean cancelled();
    void setCancelled(boolean cancelled);
    boolean modificationSupported();
    void setMetadata(String key, Object value);
}
