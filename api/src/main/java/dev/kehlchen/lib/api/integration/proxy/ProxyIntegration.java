package dev.kehlchen.lib.api.integration.proxy;

import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.integration.Integration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProxyIntegration extends Integration {
    CompletableFuture<Result<Void>> connect(UUID playerId, String server);
    CompletableFuture<Result<String>> currentServer(UUID playerId);
    CompletableFuture<Result<ProxyServerInfo>> serverInfo(UUID carrierPlayerId, String server);
    CompletableFuture<Result<List<String>>> servers(UUID carrierPlayerId);
    CompletableFuture<Result<ProxyPlayerInfo>> playerInfo(UUID carrierPlayerId, UUID targetPlayerId);
    Result<Void> sendMessage(UUID playerId, String channel, byte[] payload);
    Resource listen(String channel, ProxyMessageHandler handler);
}
