package dev.kehlchen.lib.example;

import dev.kehlchen.lib.core.KehlLibBootstrap;
import dev.kehlchen.lib.core.events.PaperBukkitEventRegistry;
import dev.kehlchen.lib.core.localization.Localization;
import dev.kehlchen.lib.core.messaging.AdventureMessaging;
import dev.kehlchen.lib.core.player.PaperPlayerService;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import dev.kehlchen.lib.api.events.BukkitEventRegistry;
import dev.kehlchen.lib.api.messaging.Messaging;
import dev.kehlchen.lib.api.player.PlayerService;
import dev.kehlchen.lib.api.scheduler.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;

/** Minimal example of an independent plugin using KehlLib core classes. */
public final class ExamplePlugin extends JavaPlugin {
    private KehlLibBootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new KehlLibBootstrap();
        Localization localization = new Localization(getDataFolder().toPath(), "en");
        PaperScheduler scheduler = new PaperScheduler(this);
        PaperPlayerService players = new PaperPlayerService(this, scheduler);
        AdventureMessaging messaging = new AdventureMessaging(localization);
        PaperBukkitEventRegistry events = new PaperBukkitEventRegistry(this);
        bootstrap.register(scheduler);
        bootstrap.register(players);
        bootstrap.register(messaging);
        bootstrap.register(events);
        bootstrap.services().register(Scheduler.class, scheduler);
        bootstrap.services().register(PlayerService.class, players);
        bootstrap.services().register(Messaging.class, messaging);
        bootstrap.services().register(BukkitEventRegistry.class, events);
        bootstrap.enableAll();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.close();
            bootstrap = null;
        }
    }
}
