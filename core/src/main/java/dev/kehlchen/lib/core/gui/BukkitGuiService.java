package dev.kehlchen.lib.core.gui;

import dev.kehlchen.lib.api.gui.Gui;
import dev.kehlchen.lib.api.gui.GuiClickContext;
import dev.kehlchen.lib.api.gui.GuiDefinition;
import dev.kehlchen.lib.api.gui.GuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Bukkit inventory GUI service with per-player sessions and lifecycle cleanup. */
public final class BukkitGuiService implements GuiService, Listener {
    private final Plugin plugin;
    private final Map<String, GuiImpl> guis = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();

    public BukkitGuiService(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "guis"; }
    @Override public String version() { return "1.0"; }
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (openInventories.containsKey(player.getUniqueId())) player.closeInventory();
        }
        openInventories.clear();
        guis.clear();
        InventoryCloseEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryDragEvent.getHandlerList().unregister(this);
    }

    @Override
    public Gui create(GuiDefinition definition) {
        GuiImpl gui = new GuiImpl(plugin, definition);
        guis.put(definition.id(), gui);
        return gui;
    }

    @Override
    public void unregister(Gui gui) {
        if (gui == null) return;
        guis.remove(gui.id());
        for (Player player : Bukkit.getOnlinePlayers()) gui.close(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;
        GuiImpl gui = guis.get(holder.guiId());
        if (gui == null) return;
        if (gui.definition.preventInteraction()) event.setCancelled(true);
        Consumer<GuiClickContext> handler = gui.definition.handlers().get(event.getSlot());
        if (handler != null) {
            handler.accept(new GuiClickContext(player, event.getSlot(), event.getClick(),
                    event.getAction(), event.getCurrentItem()));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openInventories.remove(player.getUniqueId());
        }
    }

    private record GuiHolder(String guiId) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final class GuiImpl implements Gui {
        private final Plugin plugin;
        private final GuiDefinition definition;
        private final Map<UUID, Inventory> sessions = new ConcurrentHashMap<>();

        private GuiImpl(Plugin plugin, GuiDefinition definition) {
            this.plugin = plugin;
            this.definition = definition;
        }

        @Override public String id() { return definition.id(); }

        @Override
        public void open(Player player) {
            if (player == null || !player.isOnline()) throw new IllegalArgumentException("Player must be online");
            Inventory inventory = Bukkit.createInventory(new GuiHolder(definition.id()),
                    definition.rows() * 9, definition.title());
            definition.items().forEach((slot, item) ->
                    inventory.setItem(slot, item == null ? null : item.clone()));
            sessions.put(player.getUniqueId(), inventory);
            openInventories.put(player.getUniqueId(), inventory);
            player.openInventory(inventory);
        }

        @Override
        public void close(Player player) {
            if (player == null) return;
            Inventory inventory = sessions.remove(player.getUniqueId());
            openInventories.remove(player.getUniqueId());
            if (inventory != null && player.getOpenInventory().getTopInventory().equals(inventory)) {
                player.closeInventory();
            }
        }

        @Override
        public void refresh(Player player) {
            if (player == null) return;
            Inventory inventory = sessions.get(player.getUniqueId());
            if (inventory == null) return;
            inventory.clear();
            definition.items().forEach((slot, item) ->
                    inventory.setItem(slot, item == null ? null : item.clone()));
        }
    }
}
