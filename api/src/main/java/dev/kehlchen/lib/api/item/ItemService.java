package dev.kehlchen.lib.api.item;

import dev.kehlchen.lib.api.Lifecycle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Item utilities and custom-item integration. */
public interface ItemService extends Lifecycle {
    ItemStack create(ItemBuilder builder);
    ItemStack clone(ItemStack item);
    boolean deeplyEqual(ItemStack first, ItemStack second);
    boolean similar(ItemStack first, ItemStack second);
    int count(Inventory inventory, ItemStack item);
    int remove(Inventory inventory, ItemStack item, int amount);
    Optional<String> serialize(ItemStack item);
    Optional<ItemStack> deserialize(String serialized);
    void identifyCustom(ItemStack item, NamespacedCustomItem id);
    Optional<String> customId(ItemStack item);
}
