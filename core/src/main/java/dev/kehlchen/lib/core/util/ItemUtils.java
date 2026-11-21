package dev.kehlchen.lib.core.util;

import org.bukkit.inventory.ItemStack;

public final class ItemUtils {
    private ItemUtils() {}
    public static boolean isEmpty(ItemStack item) { return item == null || item.getType().isAir() || item.getAmount() <= 0; }
    public static ItemStack copy(ItemStack item) { return item == null ? null : item.clone(); }
    public static boolean isSimilar(ItemStack a, ItemStack b) {
        return a == null ? b == null : b != null && a.isSimilar(b);
    }
    public static boolean fits(ItemStack[] inventory, ItemStack item) {
        if (isEmpty(item)) return true;
        int remaining = item.getAmount();
        for (ItemStack slot : inventory) {
            if (slot == null || slot.getType().isAir()) remaining -= item.getMaxStackSize();
            else if (slot.isSimilar(item)) remaining -= Math.max(0, item.getMaxStackSize() - slot.getAmount());
            if (remaining <= 0) return true;
        }
        return false;
    }
}
