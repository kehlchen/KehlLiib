package dev.kehlchen.lib.plugin;

import dev.kehlchen.lib.core.KehlLibBootstrap;
import dev.kehlchen.lib.core.events.PaperBukkitEventRegistry;
import dev.kehlchen.lib.core.localization.Localization;
import dev.kehlchen.lib.core.messaging.AdventureMessaging;
import dev.kehlchen.lib.core.player.PaperPlayerService;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import dev.kehlchen.lib.core.command.PaperCommandService;
import dev.kehlchen.lib.core.permission.BukkitPermissionService;
import dev.kehlchen.lib.core.item.PaperItemService;
import dev.kehlchen.lib.core.gui.BukkitGuiService;
import dev.kehlchen.lib.api.events.BukkitEventRegistry;
import dev.kehlchen.lib.api.messaging.Messaging;
import dev.kehlchen.lib.api.player.PlayerService;
import dev.kehlchen.lib.api.scheduler.Scheduler;
import dev.kehlchen.lib.api.command.CommandService;
import dev.kehlchen.lib.api.permission.PermissionService;
import dev.kehlchen.lib.api.item.ItemService;
import dev.kehlchen.lib.api.gui.GuiService;
import dev.kehlchen.lib.api.database.Database;
import dev.kehlchen.lib.api.database.DatabaseConfig;
import dev.kehlchen.lib.api.database.DatabaseFactory;
import dev.kehlchen.lib.api.storage.Storage;
import dev.kehlchen.lib.api.storage.StorageFactory;
import dev.kehlchen.lib.api.storage.StorageProvider;
import dev.kehlchen.lib.api.cache.Cache;
import dev.kehlchen.lib.api.cache.CacheConfig;
import dev.kehlchen.lib.api.cache.CacheFactory;
import dev.kehlchen.lib.core.database.JdbcDatabaseFactory;
import dev.kehlchen.lib.core.storage.StorageFactoryImpl;
import dev.kehlchen.lib.core.cache.CacheFactoryImpl;
import dev.kehlchen.lib.core.world.PaperWorldService;
import dev.kehlchen.lib.core.chunk.PaperChunkService;
import dev.kehlchen.lib.core.block.PaperBlockService;
import dev.kehlchen.lib.core.location.PaperLocationService;
import dev.kehlchen.lib.core.entity.PaperEntityService;
import dev.kehlchen.lib.core.particle.PaperParticleService;
import dev.kehlchen.lib.core.effects.PaperEffectService;
import dev.kehlchen.lib.core.network.AsyncHttpClient;
import dev.kehlchen.lib.core.network.HttpWebhookService;
import dev.kehlchen.lib.core.network.JdkWebSocketService;
import dev.kehlchen.lib.core.messaging.PaperPluginMessageService;
import dev.kehlchen.lib.core.serialization.JsonSerialization;
import dev.kehlchen.lib.api.network.http.HttpClientConfig;
import dev.kehlchen.lib.api.network.http.HttpClientService;
import dev.kehlchen.lib.api.network.websocket.WebSocketService;
import dev.kehlchen.lib.api.network.webhook.WebhookService;
import dev.kehlchen.lib.api.messaging.PluginMessageService;
import dev.kehlchen.lib.api.integration.IntegrationRegistry;
import dev.kehlchen.lib.core.integration.IntegrationRegistryImpl;
import dev.kehlchen.lib.api.world.WorldService;
import dev.kehlchen.lib.api.chunk.ChunkService;
import dev.kehlchen.lib.api.block.BlockService;
import dev.kehlchen.lib.api.location.LocationService;
import dev.kehlchen.lib.api.entity.EntityService;
import dev.kehlchen.lib.api.particle.ParticleService;
import dev.kehlchen.lib.api.effects.EffectService;
import dev.kehlchen.lib.api.packet.PacketService;
import dev.kehlchen.lib.api.virtual.VirtualEntityService;
import dev.kehlchen.lib.api.virtual.VirtualBlockService;
import dev.kehlchen.lib.api.virtual.HologramService;
import dev.kehlchen.lib.api.profiling.Profiler;
import dev.kehlchen.lib.api.metrics.Metrics;
import dev.kehlchen.lib.api.monitor.PerformanceMonitor;
import dev.kehlchen.lib.api.diagnostics.Diagnostics;
import dev.kehlchen.lib.api.nms.ServerAdapterRegistry;
import dev.kehlchen.lib.core.packet.PacketServiceImpl;
import dev.kehlchen.lib.core.virtual.PaperVirtualEntityService;
import dev.kehlchen.lib.core.virtual.PaperVirtualBlockService;
import dev.kehlchen.lib.core.virtual.InMemoryHologramService;
import dev.kehlchen.lib.core.profiling.SimpleProfiler;
import dev.kehlchen.lib.core.metrics.InMemoryMetrics;
import dev.kehlchen.lib.core.monitor.PaperPerformanceMonitor;
import dev.kehlchen.lib.core.diagnostics.CoreDiagnostics;
import dev.kehlchen.lib.core.nms.CoreServerAdapterRegistry;
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
            PaperCommandService commands = new PaperCommandService();
            BukkitPermissionService permissions = new BukkitPermissionService();
            PaperItemService items = new PaperItemService(this);
            BukkitGuiService guis = new BukkitGuiService(this);
            PaperWorldService worlds = new PaperWorldService(this, scheduler);
            PaperChunkService chunks = new PaperChunkService(this, scheduler);
            PaperBlockService blocks = new PaperBlockService(this, scheduler);
            PaperLocationService locations = new PaperLocationService(this, scheduler);
            PaperEntityService entities = new PaperEntityService(this, scheduler);
            PaperParticleService particles = new PaperParticleService(this, scheduler);
            PaperEffectService effects = new PaperEffectService(scheduler);
            AsyncHttpClient http = new AsyncHttpClient(HttpClientConfig.defaults());
            JdkWebSocketService webSockets = new JdkWebSocketService();
            PaperPluginMessageService pluginMessages = new PaperPluginMessageService();
            IntegrationRegistryImpl integrations = new IntegrationRegistryImpl();
            JsonSerialization json = new JsonSerialization();
            HttpWebhookService webhooks = new HttpWebhookService(http, json);
            PacketServiceImpl packets = new PacketServiceImpl();
            PaperVirtualEntityService virtualEntities = new PaperVirtualEntityService(4096);
            PaperVirtualBlockService virtualBlocks = new PaperVirtualBlockService(this, scheduler, 4096);
            InMemoryHologramService holograms = new InMemoryHologramService(virtualEntities);
            SimpleProfiler profiler = new SimpleProfiler();
            InMemoryMetrics metrics = new InMemoryMetrics();
            PaperPerformanceMonitor performance = new PaperPerformanceMonitor(this, scheduler);
            CoreDiagnostics diagnostics = new CoreDiagnostics(bootstrap.services());
            CoreServerAdapterRegistry adapters = new CoreServerAdapterRegistry();
            bootstrap.register(scheduler);
            bootstrap.register(players);
            bootstrap.register(messaging);
            bootstrap.register(events);
            bootstrap.register(commands);
            bootstrap.register(permissions);
            bootstrap.register(items);
            bootstrap.register(guis);
            bootstrap.register(worlds);
            bootstrap.register(chunks);
            bootstrap.register(blocks);
            bootstrap.register(locations);
            bootstrap.register(entities);
            bootstrap.register(particles);
            bootstrap.register(effects);
            bootstrap.register(http);
            bootstrap.register(webSockets);
            bootstrap.register(pluginMessages);
            bootstrap.register(integrations);
            bootstrap.register(packets);
            bootstrap.register(virtualEntities);
            bootstrap.register(virtualBlocks);
            bootstrap.register(holograms);
            bootstrap.register(profiler);
            bootstrap.register(metrics);
            bootstrap.register(performance);
            bootstrap.register(diagnostics);
            bootstrap.register(adapters);
            bootstrap.services().register(Scheduler.class, scheduler);
            bootstrap.services().register(PlayerService.class, players);
            bootstrap.services().register(Messaging.class, messaging);
            bootstrap.services().register(BukkitEventRegistry.class, events);
            bootstrap.services().register(CommandService.class, commands);
            bootstrap.services().register(PermissionService.class, permissions);
            bootstrap.services().register(ItemService.class, items);
            bootstrap.services().register(GuiService.class, guis);
            bootstrap.services().register(WorldService.class, worlds);
            bootstrap.services().register(ChunkService.class, chunks);
            bootstrap.services().register(BlockService.class, blocks);
            bootstrap.services().register(LocationService.class, locations);
            bootstrap.services().register(EntityService.class, entities);
            bootstrap.services().register(ParticleService.class, particles);
            bootstrap.services().register(EffectService.class, effects);
            bootstrap.services().register(HttpClientService.class, http);
            bootstrap.services().register(WebSocketService.class, webSockets);
            bootstrap.services().register(PluginMessageService.class, pluginMessages);
            bootstrap.services().register(IntegrationRegistry.class, integrations);
            bootstrap.services().register(WebhookService.class, webhooks);
            bootstrap.services().register(JsonSerialization.class, json);
            bootstrap.services().register(PacketService.class, packets);
            bootstrap.services().register(VirtualEntityService.class, virtualEntities);
            bootstrap.services().register(VirtualBlockService.class, virtualBlocks);
            bootstrap.services().register(HologramService.class, holograms);
            bootstrap.services().register(Profiler.class, profiler);
            bootstrap.services().register(Metrics.class, metrics);
            bootstrap.services().register(PerformanceMonitor.class, performance);
            bootstrap.services().register(Diagnostics.class, diagnostics);
            bootstrap.services().register(ServerAdapterRegistry.class, adapters);
            DatabaseFactory databases = config -> {
                Database database = new JdbcDatabaseFactory().create(config);
                bootstrap.resources().register(database);
                return database;
            };
            StorageFactory storages = new StorageFactory() {
                @Override public <K, V> Storage<K, V> create(
                        Class<V> valueType, StorageProvider<K, V> provider) {
                    Storage<K, V> storage = new StorageFactoryImpl().create(valueType, provider);
                    bootstrap.resources().register(storage);
                    return storage;
                }
            };
            CacheFactory caches = new CacheFactory() {
                @Override public <K, V> Cache<K, V> create(CacheConfig config) {
                    Cache<K, V> cache = new CacheFactoryImpl().create(config);
                    bootstrap.resources().register(cache);
                    return cache;
                }
            };
            bootstrap.services().register(DatabaseFactory.class, databases);
            bootstrap.services().register(StorageFactory.class, storages);
            bootstrap.services().register(CacheFactory.class, caches);
            bootstrap.enableAll();
            getLogger().info("KehlLib core runtime enabled");
        } catch (RuntimeException failure) {
            getLogger().log(Level.SEVERE, "KehlLib failed to enable", failure);
            try {
                bootstrap.close();
            } catch (RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
                getLogger().log(Level.SEVERE, "KehlLib failed to clean up after enable failure", closeFailure);
            } finally {
                bootstrap = null;
            }
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
