package dev.kehlchen.lib.api.virtual;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public record VirtualEntityConfig(Location location, EntityType entityType,
                                  String name, ItemStack equipment,
                                  boolean visible, int maxViewers) {
    public VirtualEntityConfig {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(entityType, "entityType");
        name = name == null ? "" : name;
        maxViewers = Math.max(1, maxViewers);
    }

    public static Builder builder(Location location, EntityType entityType) {
        return new Builder(location, entityType);
    }

    public static final class Builder {
        private final Location location;
        private final EntityType entityType;
        private String name = "";
        private ItemStack equipment;
        private boolean visible = true;
        private int maxViewers = 128;

        private Builder(Location location, EntityType entityType) {
            this.location = location;
            this.entityType = entityType;
        }

        public Builder name(String name) { this.name = name; return this; }
        public Builder equipment(ItemStack equipment) { this.equipment = equipment; return this; }
        public Builder visible(boolean visible) { this.visible = visible; return this; }
        public Builder maxViewers(int maxViewers) { this.maxViewers = maxViewers; return this; }
        public VirtualEntityConfig build() {
            return new VirtualEntityConfig(location, entityType, name, equipment, visible, maxViewers);
        }
    }
}
