package dev.kehlchen.lib.api.player;

import dev.kehlchen.lib.api.Lifecycle;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Central player abstraction and lifecycle service. */
public interface PlayerService extends Lifecycle {
    Optional<OnlinePlayer> online(UUID uniqueId);
    Optional<OfflinePlayerView> offline(UUID uniqueId);
    Optional<PlayerProfile> profile(UUID uniqueId);
    CompletableFuture<Optional<OfflinePlayerView>> loadOffline(UUID uniqueId);
    void markActivity(UUID uniqueId);
}
