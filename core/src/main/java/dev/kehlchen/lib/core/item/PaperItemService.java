package dev.kehlchen.lib.core.item;

import dev.kehlchen.lib.api.item.ItemBuilder;
import dev.kehlchen.lib.api.item.ItemService;
import dev.kehlchen.lib.api.item.NamespacedCustomItem;
import dev.kehlchen.lib.core.serialization.ItemStackSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/** Bukkit implementation of item utilities and custom item identifiers. */
public final class PaperItemService implements ItemService {
    private final Plugin plugin;
    private final NamespacedKey customIdKey;
    private final ItemStackSerializer serializer = new ItemStackSerializer();

    public PaperItemService(Plugin plugin) {
        this.plugin = plugin;
        this.customIdKey = new NamespacedKey(plugin, "custom_item_id");
    }

    @Override public String name() { return "items"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() {}
    @Override public void onDisable() {}

    @Override public ItemStack create(ItemBuilder builder) {
        if (builder == null) throw new IllegalArgumentException("Item builder is required");
        return builder.build();
    }

    @Override
    public ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }

    @Override
    public boolean deeplyEqual(ItemStack first, ItemStack second) {
        if (first == second) return true;
        if (first == null || second == null) return false;
        return first.equals(second);
    }

    @Override
    public boolean similar(ItemStack first, ItemStack second) {
        if (first == null || second == null) return first == second;
        return first.isSimilar(second);
    }

    @Override
    public int count(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null) return 0;
        int count = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot != null && slot.isSimilar(item)) count += slot.getAmount();
        }
        return count;
    }

    @Override
    public int remove(Inventory inventory, ItemStack item, int amount) {
        if (inventory == null || item == null || amount < 1) return 0;
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack slot = contents[i];
            if (slot == null || !slot.isSimilar(item)) continue;
            int remove = Math.min(slot.getAmount(), remaining);
            slot.setAmount(slot.getAmount() - remove);
            remaining -= remove;
            if (slot.getAmount() == 0) contents[i] = null;
        }
        inventory.setStorageContents(contents);
        return amount - remaining;
    }

    @Override
    public Optional<String> serialize(ItemStack item) {
        if (item == null) return Optional.empty();
        try {
            return Optional.ofNullable(serializer.serialize(item));
        } catch (RuntimeException failure) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ItemStack> deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) return Optional.empty();
        try {
            return Optional.ofNullable(serializer.deserialize(serialized));
        } catch (RuntimeException failure) {
            return Optional.empty();
        }
    }

    @Override
    public void identifyCustom(ItemStack item, NamespacedCustomItem id) {
        if (item == null || id == null) throw new IllegalArgumentException("Item and id are required");
        ItemMeta meta = item.getItemMeta();
        if (meta == null) throw new IllegalArgumentException("Item does not support metadata");
        meta.getPersistentDataContainer().set(customIdKey, PersistentDataType.STRING, id.key().toString());
        item.setItemMeta(meta);
    }

    @Override
    public Optional<String> customId(ItemStack item) {
        if (item == null) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        String id = meta.getPersistentDataContainer().get(customIdKey, PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }
}
