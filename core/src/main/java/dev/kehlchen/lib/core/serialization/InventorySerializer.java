package dev.kehlchen.lib.core.serialization;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class InventorySerializer {
    public String serialize(Inventory inventory) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("size", inventory.getSize());
        yaml.set("title", inventory.getType().name());
        List<ItemStack> contents = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) contents.add(item == null ? null : item.clone());
        yaml.set("contents", contents);
        return yaml.saveToString();
    }
    public List<ItemStack> deserializeContents(String data) {
        YamlConfiguration yaml = new YamlConfiguration();
        try { yaml.loadFromString(data); } catch (Exception e) { throw new IllegalArgumentException("Invalid inventory data", e); }
        List<?> values = yaml.getList("contents");
        if (values == null) return List.of();
        List<ItemStack> result = new ArrayList<>();
        for (Object value : values) result.add(value instanceof ItemStack item ? item : null);
        return result;
    }
}
