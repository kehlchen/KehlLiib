package dev.kehlchen.lib.api.packet;

import dev.kehlchen.lib.api.Resource;

/** Lifecycle-managed packet listener registration. */
public interface PacketRegistration extends Resource {
    boolean active();
}
