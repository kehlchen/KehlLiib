package dev.kehlchen.lib.api.gui;

import dev.kehlchen.lib.api.gui.GuiClickContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Fluent builder for a paged or basic GUI. */
public final class GuiBuilder {
    private String id;
    private Component title;
    private int rows = 3;
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Map<Integer, Consumer<GuiClickContext>> handlers = new HashMap<>();
    private boolean preventInteraction = true;

    public static GuiBuilder gui() {
        return new GuiBuilder();
    }

    public GuiBuilder id(String id) {
        this.id = id;
        return this;
    }

    public GuiBuilder title(String miniMessage) {
        this.title = MiniMessage.miniMessage().deserialize(miniMessage);
        return this;
    }

    public GuiBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public GuiBuilder rows(int rows) {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("GUI rows must be between 1 and 6");
        this.rows = rows;
        return this;
    }

    public GuiBuilder item(int slot, ItemStack item) {
        validateSlot(slot);
        items.put(slot, item);
        return this;
    }

    public GuiBuilder onClick(int slot, Consumer<GuiClickContext> handler) {
        validateSlot(slot);
        handlers.put(slot, Objects.requireNonNull(handler, "handler"));
        return this;
    }

    public GuiBuilder preventInteraction(boolean preventInteraction) {
        this.preventInteraction = preventInteraction;
        return this;
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= rows * 9) throw new IllegalArgumentException("Slot is outside the GUI");
    }

    public GuiDefinition build() {
        if (title == null) title = Component.text("Inventory");
        if (id == null) id = "gui-" + Integer.toHexString(System.identityHashCode(this));
        return new GuiDefinition(id, title, rows, Map.copyOf(items),
                Map.copyOf(handlers), preventInteraction);
    }
}
