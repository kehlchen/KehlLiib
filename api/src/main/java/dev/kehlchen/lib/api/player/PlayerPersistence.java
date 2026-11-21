package dev.kehlchen.lib.api.player;

import dev.kehlchen.lib.api.Lifecycle;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/** Optional persistence service; the default in-memory PlayerService is unchanged. */
public interface PlayerPersistence extends Lifecycle {
    Optional<PersistentPlayerData> cached(UUID uniqueId);
    CompletableFuture<PersistentPlayerData> load(UUID uniqueId);
    CompletableFuture<PersistentPlayerData> update(UUID uniqueId, UnaryOperator<PersistentPlayerData> updater);
    CompletableFuture<Void> save(UUID uniqueId);
}
