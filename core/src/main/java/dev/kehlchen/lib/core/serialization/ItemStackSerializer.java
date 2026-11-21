package dev.kehlchen.lib.core.serialization;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public final class ItemStackSerializer implements Serializer<ItemStack> {
    @Override public String serialize(ItemStack value) {
        if (value == null) return "";
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("item", value);
        return yaml.saveToString();
    }
    @Override public ItemStack deserialize(String data) {
        if (data == null || data.isBlank()) return null;
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(data);
            Object value = yaml.get("item");
            return value instanceof ItemStack item ? item : null;
        } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid ItemStack data", e); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid ItemStack data", e); }
    }
}
