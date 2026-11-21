package dev.kehlchen.lib.api.gui;

import org.bukkit.entity.Player;

/** Public GUI contract. */
public interface Gui {
    String id();
    void open(Player player);
    void close(Player player);
    void refresh(Player player);
}
