package dev.kehlchen.lib.core.virtual;

import dev.kehlchen.lib.api.virtual.VirtualEntity;
import dev.kehlchen.lib.api.virtual.VirtualEntityConfig;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class VirtualEntityImpl implements VirtualEntity {
    private final UUID id;
    private final Location location;
    private final String name;
    private final ItemStack equipment;
    private final boolean visible;
    private final VirtualEntityConfig config;

    public VirtualEntityImpl(UUID id, Location location, String name,
                             ItemStack equipment, boolean visible,
                             VirtualEntityConfig config) {
        this.id = id;
        this.location = location.clone();
        this.name = name;
        this.equipment = equipment == null ? null : equipment.clone();
        this.visible = visible;
        this.config = config;
    }

    @Override public UUID id() { return id; }
    @Override public Location location() { return location.clone(); }
    @Override public String name() { return name; }
    @Override public Optional<ItemStack> equipment() { return Optional.ofNullable(equipment); }
    @Override public boolean visible() { return visible; }
    public VirtualEntityConfig config() { return config; }
}
