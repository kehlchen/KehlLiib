package dev.kehlchen.lib.api.packet;

import java.util.Map;

/** Safe, backend-neutral packet data. Internal Minecraft packet objects are never exposed. */
public record MapPacket(Map<String, Object> values) {
    public MapPacket {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
