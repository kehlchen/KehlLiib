package dev.kehlchen.lib.api.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

/** Context supplied to inventory click handlers. */
public record GuiClickContext(
        Player player,
        int slot,
        ClickType clickType,
        InventoryAction action,
        ItemStack currentItem
) {}
