package dev.kehlchen.lib.plugin;

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

import java.util.logging.Level;

/** Standalone KehlLib Paper plugin entry point. */
public final class KehlLibPlugin extends JavaPlugin {
    private KehlLibBootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new KehlLibBootstrap();
        try {
            dev.kehlchen.lib.core.util.FileUtils.ensureDirectory(getDataFolder().toPath());
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
            getLogger().info("KehlLib core runtime enabled");
        } catch (RuntimeException failure) {
            getLogger().log(Level.SEVERE, "KehlLib failed to enable", failure);
            bootstrap = null;
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            try {
                bootstrap.close();
            } catch (RuntimeException failure) {
                getLogger().log(Level.SEVERE, "KehlLib failed to shut down cleanly", failure);
            } finally {
                bootstrap = null;
            }
        }
    }

    /** Returns the runtime owned by this plugin, or throws before/after lifecycle transitions. */
    public KehlLibBootstrap runtime() {
        if (bootstrap == null) {
            throw new IllegalStateException("KehlLib runtime is not available");
        }
        return bootstrap;
    }
}
