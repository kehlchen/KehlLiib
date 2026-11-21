package dev.kehlchen.lib.api.messaging;

import dev.kehlchen.lib.api.Lifecycle;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/** Adventure-based command sender and player messaging service. */
public interface Messaging extends Lifecycle {
    Component component(String miniMessage);
    Component format(Component template, Map<String, ?> placeholders);
    Component localized(String key, Object... arguments);
    void chat(CommandSender sender, Component message);
    void actionBar(Player player, Component message);
    void title(Player player, Component title, Component subtitle, long fadeInTicks, long stayTicks, long fadeOutTicks);
    void sound(Player player, org.bukkit.Location location, org.bukkit.Sound sound, float volume, float pitch);
    void broadcast(Component message);
    void broadcast(Collection<? extends CommandSender> recipients, Component message);
    /** Shows a bossbar to a player and returns its handle for updates and removal. */
    org.bukkit.boss.BossBar bossBar(Player player, Component title, org.bukkit.boss.BarColor color,
                                    org.bukkit.boss.BarStyle style, float progress);
    /** Sends a particle effect at the player's location. */
    void particles(Player player, org.bukkit.Particle particle, int count);
}
