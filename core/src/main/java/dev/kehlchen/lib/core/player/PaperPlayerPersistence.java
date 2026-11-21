package dev.kehlchen.lib.core.player;

import dev.kehlchen.lib.api.player.PersistentPlayerData;
import dev.kehlchen.lib.api.player.PlayerPersistence;
import dev.kehlchen.lib.api.player.PlayerPersistenceConfig;
import dev.kehlchen.lib.api.scheduler.Scheduler;
import dev.kehlchen.lib.api.scheduler.TaskHandle;
import dev.kehlchen.lib.api.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/** Optional event-driven player persistence. Only UUIDs and immutable data leave event threads. */
public final class PaperPlayerPersistence implements PlayerPersistence, Listener {
    private final Plugin plugin;
    private final Scheduler scheduler;
    private final Storage<UUID, PersistentPlayerData> storage;
    private final PlayerPersistenceConfig config;
    private final Map<UUID, PersistentPlayerData> loaded = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<PersistentPlayerData>> loading = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> sessionStarts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dirty = new ConcurrentHashMap<>();
    private final Map<UUID, Long> persistedRevision = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> writeTails = new ConcurrentHashMap<>();
    private volatile boolean enabled;
    private TaskHandle saveTask;

    public PaperPlayerPersistence(Plugin plugin, Scheduler scheduler,
                                  Storage<UUID, PersistentPlayerData> storage,
                                  PlayerPersistenceConfig config) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
        this.scheduler = java.util.Objects.requireNonNull(scheduler, "scheduler");
        this.storage = java.util.Objects.requireNonNull(storage, "storage");
        this.config = java.util.Objects.requireNonNull(config, "config");
    }

    @Override public String name() { return "player-persistence"; }
    @Override public String version() { return "1.0"; }

    @Override public void onEnable() {
        if (enabled) return;
        enabled = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        try {
            var result = scheduler.repeatAsync(plugin, config.saveInterval(), config.saveInterval(), this::saveDirty);
            if (!result.isSuccess()) {
                throw new IllegalStateException("Could not schedule player persistence", result.error().orElseThrow());
            }
            saveTask = result.value().orElseThrow(
                    () -> new IllegalStateException("Scheduler returned no player persistence task"));
        } catch (RuntimeException failure) {
            enabled = false;
            HandlerList.unregisterAll(this);
            throw failure;
        }
    }

    @Override public void onDisable() {
        if (!enabled) return;
        enabled = false;
        HandlerList.unregisterAll(this);
        if (saveTask != null) {
            saveTask.close();
            saveTask = null;
        }

        Instant shutdownAt = Instant.now();
        List<CompletableFuture<Void>> pending = new ArrayList<>();
        for (Map.Entry<UUID, Instant> session : sessionStarts.entrySet()) {
            UUID uuid = session.getKey();
            Instant start = session.getValue();
            sessionStarts.remove(uuid, start);
            pending.add(load(uuid).thenApply(data -> updateLoaded(uuid,
                            value -> value.finishSession(Duration.between(start, shutdownAt), shutdownAt)))
                    .thenCompose(this::enqueueSave));
        }
        dirty.keySet().forEach(uuid -> pending.add(save(uuid)));
        writeTails.values().forEach(pending::add);
        try {
            CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                    .get(config.shutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception failure) {
            throw new IllegalStateException("Player data could not be fully saved during shutdown", failure);
        }
    }

    @Override
    public Optional<PersistentPlayerData> cached(UUID uniqueId) {
        return Optional.ofNullable(loaded.get(java.util.Objects.requireNonNull(uniqueId, "uniqueId")));
    }

    @Override
    public CompletableFuture<PersistentPlayerData> load(UUID uniqueId) {
        java.util.Objects.requireNonNull(uniqueId, "uniqueId");
        PersistentPlayerData cached = loaded.get(uniqueId);
        return cached == null
                ? loadWithDefault(uniqueId, () -> PersistentPlayerData.initial(uniqueId, null, false))
                : CompletableFuture.completedFuture(cached);
    }

    @Override
    public CompletableFuture<PersistentPlayerData> update(
            UUID uniqueId, UnaryOperator<PersistentPlayerData> updater) {
        java.util.Objects.requireNonNull(updater, "updater");
        return load(uniqueId).thenApply(ignored -> updateLoaded(uniqueId, updater));
    }

    @Override
    public CompletableFuture<Void> save(UUID uniqueId) {
        return load(uniqueId).thenCompose(this::enqueueSave);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        boolean firstJoin = !event.getPlayer().hasPlayedBefore();
        sessionStarts.put(uuid, Instant.now());
        loadWithDefault(uuid, () -> PersistentPlayerData.initial(uuid, playerName, firstJoin))
                .thenApply(data -> updateLoaded(uuid, value -> value.withName(playerName)))
                .thenAccept(data -> markDirty(data))
                .exceptionally(failure -> {
                    plugin.getLogger().log(Level.SEVERE, "Could not load player data for " + uuid, unwrap(failure));
                    return null;
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Instant started = sessionStarts.remove(uuid);
        if (started == null) return;
        Instant seenAt = Instant.now();
        load(uuid).thenApply(data -> updateLoaded(uuid,
                        value -> value.finishSession(Duration.between(started, seenAt), seenAt)))
                .thenCompose(this::enqueueSave)
                .whenComplete((ignored, failure) -> {
                    if (failure != null) plugin.getLogger().log(
                            Level.SEVERE, "Could not save player data for " + uuid, unwrap(failure));
                });
    }

    private CompletableFuture<PersistentPlayerData> loadWithDefault(
            UUID uuid, Supplier<PersistentPlayerData> defaultValue) {
        PersistentPlayerData cached = loaded.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        CompletableFuture<PersistentPlayerData> inProgress = loading.get(uuid);
        if (inProgress != null) return inProgress;

        CompletableFuture<PersistentPlayerData> candidate = retry(() -> storage.loadAsync(uuid).thenApply(value -> {
            if (value.isPresent()) {
                persistedRevision.putIfAbsent(uuid, value.get().revision());
                return value.get();
            }
            persistedRevision.putIfAbsent(uuid, -1L);
            return defaultValue.get();
        }), config.retries());
        CompletableFuture<PersistentPlayerData> existing = loading.putIfAbsent(uuid, candidate);
        if (existing != null) return existing;
        candidate.whenComplete((data, failure) -> {
            if (failure == null) {
                loaded.putIfAbsent(uuid, data);
            }
            loading.remove(uuid, candidate);
        });
        return candidate.thenApply(data -> loaded.getOrDefault(uuid, data));
    }

    private PersistentPlayerData updateLoaded(UUID uuid, UnaryOperator<PersistentPlayerData> updater) {
        return loaded.compute(uuid, (key, current) -> {
            if (current == null) throw new IllegalStateException("Player data has not finished loading: " + key);
            PersistentPlayerData updated = java.util.Objects.requireNonNull(updater.apply(current), "updated player data");
            if (!updated.uniqueId().equals(key)
                    || (updated != current && updated.revision() <= current.revision())) {
                throw new IllegalArgumentException("Player data update changed identity or did not advance its revision");
            }
            if (updated != current) dirty.put(key, updated.revision());
            return updated;
        });
    }

    private void markDirty(PersistentPlayerData data) {
        dirty.put(data.uniqueId(), data.revision());
    }

    private void saveDirty() {
        for (UUID uuid : List.copyOf(dirty.keySet())) {
            save(uuid).whenComplete((ignored, failure) -> {
                if (failure != null) plugin.getLogger().log(
                        Level.SEVERE, "Periodic player data save failed for " + uuid, unwrap(failure));
            });
        }
    }

    private CompletableFuture<Void> enqueueSave(PersistentPlayerData snapshot) {
        UUID uuid = snapshot.uniqueId();
        return writeTails.compute(uuid, (key, previous) -> {
            CompletableFuture<Void> ready = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous.handle((ignored, failure) -> null);
            return ready.thenCompose(ignored -> {
                PersistentPlayerData current = loaded.get(uuid);
                long latestRevision = current == null ? snapshot.revision() : current.revision();
                if (snapshot.revision() < latestRevision
                        || persistedRevision.getOrDefault(uuid, -1L) >= snapshot.revision()) {
                    return CompletableFuture.completedFuture(null);
                }
                return retry(() -> storage.saveAsync(uuid, snapshot), config.retries()).thenRun(() -> {
                    persistedRevision.merge(uuid, snapshot.revision(), Math::max);
                    dirty.computeIfPresent(uuid, (id, revision) -> revision <= snapshot.revision() ? null : revision);
                });
            });
        });
    }

    private <T> CompletableFuture<T> retry(Supplier<CompletableFuture<T>> operation, int retriesRemaining) {
        CompletableFuture<T> attempt;
        try {
            attempt = java.util.Objects.requireNonNull(operation.get(), "storage operation future");
        } catch (RuntimeException failure) {
            attempt = CompletableFuture.failedFuture(failure);
        }
        return attempt.handle((value, failure) -> {
            if (failure == null) return CompletableFuture.completedFuture(value);
            if (retriesRemaining == 0) return CompletableFuture.<T>failedFuture(unwrap(failure));
            return CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(
                    config.retryDelay().toMillis(), TimeUnit.MILLISECONDS))
                    .thenCompose(ignored -> retry(operation, retriesRemaining - 1));
        }).thenCompose(future -> future);
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
