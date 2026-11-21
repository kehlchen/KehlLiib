package dev.kehlchen.lib.api.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/** Immutable GUI template. */
public record GuiDefinition(
        String id,
        Component title,
        int rows,
        Map<Integer, ItemStack> items,
        Map<Integer, Consumer<GuiClickContext>> handlers,
        boolean preventInteraction
) {}
