package dev.kehlchen.lib.core.player;

import dev.kehlchen.lib.api.player.CompletablePlayerPreferences;
import dev.kehlchen.lib.api.player.OfflinePlayerView;
import dev.kehlchen.lib.api.player.OnlinePlayer;
import dev.kehlchen.lib.api.player.PlayerProfile;
import dev.kehlchen.lib.api.player.PlayerPersistence;
import dev.kehlchen.lib.api.player.PlayerService;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** UUID-safe Paper player service with session and preference abstractions. */
public final class PaperPlayerService implements PlayerService, Listener {
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private final PlayerPersistence persistence;
    private final Executor io = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "kehllib-player-data");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> preferences = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> firstJoin = new ConcurrentHashMap<>();
    private volatile boolean enabled;
    private volatile Duration afkThreshold = Duration.ofMinutes(5);

    public PaperPlayerService(Plugin plugin, PaperScheduler scheduler) {
        this(plugin, scheduler, null);
    }

    public PaperPlayerService(Plugin plugin, PaperScheduler scheduler, PlayerPersistence persistence) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.persistence = persistence;
    }

    /** Sets the AFK threshold. */
    public void setAfkThreshold(Duration threshold) {
        afkThreshold = threshold == null ? Duration.ofMinutes(5) : threshold;
    }

    @Override public String name() { return "player"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() {
        enabled = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    @Override public void onDisable() {
        enabled = false;
        sessions.clear();
        ((java.util.concurrent.ThreadPoolExecutor) io).shutdownNow();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean first = !player.hasPlayedBefore();
        firstJoin.put(uuid, first);
        sessions.put(uuid, new Session(player.getName(), Instant.now(), Instant.now()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Session session = sessions.remove(uuid);
        if (session != null) session.lastActivity = Instant.now();
    }

    @Override
    public Optional<OnlinePlayer> online(UUID uniqueId) {
        if (!enabled || uniqueId == null) return Optional.empty();
        Player player = Bukkit.getPlayer(uniqueId);
        if (player == null || !player.isOnline()) return Optional.empty();
        return Optional.of(new PaperOnlinePlayer(uniqueId));
    }

    @Override
    public Optional<OfflinePlayerView> offline(UUID uniqueId) {
        if (uniqueId == null) return Optional.empty();
        org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(uniqueId);
        return Optional.of(new PaperOfflinePlayer(player));
    }

    @Override
    public Optional<PlayerProfile> profile(UUID uniqueId) {
        if (uniqueId == null) return Optional.empty();
        org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(uniqueId);
        if (player.getName() == null) return Optional.empty();
        return Optional.of(new PlayerProfile(uniqueId, player.getName()));
    }

    @Override
    public CompletableFuture<Optional<OfflinePlayerView>> loadOffline(UUID uniqueId) {
        if (uniqueId == null) return CompletableFuture.failedFuture(new NullPointerException("uniqueId"));
        return CompletableFuture.supplyAsync(() -> offline(uniqueId), io);
    }

    @Override
    public void markActivity(UUID uniqueId) {
        Session session = sessions.get(uniqueId);
        if (session != null) session.lastActivity = Instant.now();
    }

    private final class PaperOnlinePlayer implements OnlinePlayer {
        private final UUID uuid;
        private final Map<String, Object> metadata = new ConcurrentHashMap<>();

        private PaperOnlinePlayer(UUID uuid) { this.uuid = uuid; }

        @Override public UUID uniqueId() { return uuid; }
        @Override public Optional<String> name() {
            Player player = Bukkit.getPlayer(uuid);
            return player == null ? Optional.empty() : Optional.ofNullable(player.getName());
        }
        @Override public boolean isOnline() {
            Player player = Bukkit.getPlayer(uuid);
            return player != null && player.isOnline();
        }
        @Override public boolean isFirstJoin() {
            return persistence == null
                    ? firstJoin.getOrDefault(uuid, false)
                    : persistence.cached(uuid).map(data -> data.firstJoin())
                            .orElseGet(() -> firstJoin.getOrDefault(uuid, false));
        }
        @Override public Duration playtime() {
            Session session = sessions.get(uuid);
            if (persistence != null) {
                Optional<dev.kehlchen.lib.api.player.PersistentPlayerData> data = persistence.cached(uuid);
                if (data.isPresent()) {
                    Duration total = Duration.ofMillis(data.get().totalPlaytimeMillis());
                    return session == null ? total : total.plus(Duration.between(session.joined, Instant.now()));
                }
            }
            if (session == null) {
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                return Duration.ofMillis(Math.max(0L, offline.getLastPlayed() - offline.getFirstPlayed()));
            }
            return Duration.between(session.joined, Instant.now());
        }
        @Override public Duration afkDuration() {
            Session session = sessions.get(uuid);
            return session == null ? Duration.ZERO :
                    Duration.between(session.lastActivity, Instant.now()).abs();
        }
        @Override public boolean isAfk() { return afkDuration().compareTo(afkThreshold) >= 0; }
        @Override public Object metadata(String key) { return metadata.get(key); }
        @Override public void metadata(String key, Object value) {
            if (value == null) metadata.remove(key); else metadata.put(key, value);
        }
        @Override public void removeMetadata(String key) { metadata.remove(key); }
        @Override public Location location() {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) throw new IllegalStateException("Player is offline");
            return player.getLocation();
        }
        @Override public boolean teleport(Location location) {
            if (!Bukkit.isPrimaryThread()) {
                throw new IllegalStateException("Teleport must be executed on the region thread");
            }
            Player player = Bukkit.getPlayer(uuid);
            return player != null && player.isOnline() && player.teleport(location);
        }
        @Override public CompletablePlayerPreferences preferences() {
            return new PaperPreferences(uuid);
        }
    }

    private final class PaperPreferences implements CompletablePlayerPreferences {
        private final UUID uuid;
        private PaperPreferences(UUID uuid) { this.uuid = uuid; }
        @Override public CompletableFuture<String> getString(String key, String defaultValue) {
            if (persistence != null) {
                return persistence.load(uuid)
                        .thenApply(data -> data.preferences().getOrDefault(key, defaultValue));
            }
            return CompletableFuture.supplyAsync(() ->
                    preferences.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>())
                            .getOrDefault(key, defaultValue), io);
        }
        @Override public CompletableFuture<Void> setString(String key, String value) {
            if (persistence != null) {
                return persistence.update(uuid, data -> data.withPreference(key, value)).thenApply(ignored -> null);
            }
            return CompletableFuture.runAsync(() ->
                    preferences.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>()).put(key, value), io);
        }
        @Override public CompletableFuture<Boolean> getBoolean(String key, boolean defaultValue) {
            return getString(key, Boolean.toString(defaultValue)).thenApply(Boolean::parseBoolean);
        }
        @Override public CompletableFuture<Void> setBoolean(String key, boolean value) {
            return setString(key, Boolean.toString(value));
        }
    }

    private record OfflineData(String name, Duration playtime, boolean playedBefore) {}

    private final class PaperOfflinePlayer implements OfflinePlayerView {
        private final org.bukkit.OfflinePlayer delegate;
        private PaperOfflinePlayer(org.bukkit.OfflinePlayer delegate) { this.delegate = delegate; }
        @Override public UUID uniqueId() { return delegate.getUniqueId(); }
        @Override public Optional<String> name() { return Optional.ofNullable(delegate.getName()); }
        @Override public boolean isOnline() { return delegate.isOnline(); }
        @Override public Optional<Duration> playtime() {
            long millis = delegate.getLastPlayed() - delegate.getFirstPlayed();
            return millis >= 0 ? Optional.of(Duration.ofMillis(millis)) : Optional.empty();
        }
        @Override public boolean hasPlayedBefore() { return delegate.hasPlayedBefore(); }
    }

    private static final class Session {
        private final String name;
        private final Instant joined;
        private volatile Instant lastActivity;
        private Session(String name, Instant joined, Instant lastActivity) {
            this.name = name;
            this.joined = joined;
            this.lastActivity = lastActivity;
        }
    }
}
