package dev.kehlchen.lib.api.virtual;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/** Identifies a virtual entity; no Bukkit Entity object is created or exposed. */
public interface VirtualEntity {
    UUID id();
    Location location();
    String name();
    Optional<ItemStack> equipment();
    boolean visible();
}
