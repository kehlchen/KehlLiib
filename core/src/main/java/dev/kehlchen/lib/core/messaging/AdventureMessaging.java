package dev.kehlchen.lib.core.messaging;

import dev.kehlchen.lib.api.messaging.Messaging;
import dev.kehlchen.lib.core.localization.Localization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/** Adventure MiniMessage implementation with localization and placeholder support. */
public final class AdventureMessaging implements Messaging {
    private final MiniMessage miniMessage;
    private final Localization localization;

    public AdventureMessaging(Localization localization) {
        this.localization = localization;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override public String name() { return "messaging"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() {}
    @Override public void onDisable() {}

    @Override
    public Component component(String miniMessage) {
        return this.miniMessage.deserialize(miniMessage == null ? "" : miniMessage);
    }

    @Override
    public Component format(Component template, Map<String, ?> placeholders) {
        Objects.requireNonNull(template, "template");
        if (placeholders == null || placeholders.isEmpty()) return template;
        TagResolver.Builder resolver = TagResolver.builder();
        placeholders.forEach((key, value) ->
                resolver.resolver(Placeholder.parsed(key, String.valueOf(value))));
        String serialized = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .serialize(template);
        return miniMessage.deserialize(serialized, resolver.build());
    }

    @Override
    public Component localized(String key, Object... arguments) {
        if (localization == null) return component(key);
        return component(localization.tr(key, arguments));
    }

    @Override public void chat(CommandSender sender, Component message) { sender.sendMessage(message); }
    @Override public void actionBar(Player player, Component message) { player.sendActionBar(message); }

    @Override
    public void title(Player player, Component title, Component subtitle,
                      long fadeInTicks, long stayTicks, long fadeOutTicks) {
        player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(fadeInTicks * 50),
                        Duration.ofMillis(stayTicks * 50),
                        Duration.ofMillis(fadeOutTicks * 50))));
    }

    @Override
    public void sound(Player player, Location location, Sound sound, float volume, float pitch) {
        player.playSound(location, sound, volume, pitch);
    }

    @Override
    public void broadcast(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(message);
        Bukkit.getConsoleSender().sendMessage(message);
    }

    @Override
    public void broadcast(Collection<? extends CommandSender> recipients, Component message) {
        for (CommandSender recipient : recipients) recipient.sendMessage(message);
    }

    @Override
    public BossBar bossBar(Player player, Component title, BarColor color, BarStyle style, float progress) {
        BossBar bossBar = Bukkit.createBossBar("", color, style);
        bossBar.setTitle(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .serialize(title));
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        bossBar.addPlayer(player);
        return bossBar;
    }

    @Override
    public void particles(Player player, Particle particle, int count) {
        player.spawnParticle(particle, player.getLocation(), count);
    }
}
