package dev.kehlchen.lib.api.packet;

import java.util.function.Predicate;

@FunctionalInterface
public interface PacketListener {
    void onPacket(PacketEvent event);

    default Predicate<PacketEvent> filter() {
        return event -> true;
    }
}
