package dev.kehlchen.lib.core.packet;

import dev.kehlchen.lib.api.packet.MapPacket;
import dev.kehlchen.lib.api.packet.PacketBackend;
import dev.kehlchen.lib.api.packet.PacketDirection;
import dev.kehlchen.lib.api.packet.PacketListener;
import dev.kehlchen.lib.api.packet.PacketPriority;
import dev.kehlchen.lib.api.packet.PacketRegistration;
import dev.kehlchen.lib.api.packet.PacketService;
import dev.kehlchen.lib.api.packet.PacketEvent;
import dev.kehlchen.lib.api.Result;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Packet service that degrades honestly when no backend is registered. */
public final class PacketServiceImpl implements PacketService {
    private final CopyOnWriteArrayList<PacketBackend> backends = new CopyOnWriteArrayList<>();
    private volatile PacketBackend active;
    private volatile boolean enabled;

    @Override public String name() { return "packet-service"; }
    @Override public String version() { return "1.0"; }
    @Override public synchronized void onEnable() { enabled = true; }
    @Override public synchronized void onDisable() { close(); }

    @Override
    public Optional<PacketBackend> backend() {
        return Optional.ofNullable(active);
    }

    @Override public boolean interceptionAvailable() {
        return active != null && active.interceptionSupported();
    }

    @Override public boolean modificationAvailable() {
        return active != null && active.modificationSupported();
    }

    @Override
    public Result<Void> send(UUID playerId, PacketDirection direction, String type, MapPacket packet) {
        if (!enabled) return Result.failure(new IllegalStateException("Packet service is disabled"));
        if (active == null) return Result.failure(new UnsupportedOperationException(
                "No packet backend is registered"));
        return active.send(playerId, direction, type, packet);
    }

    @Override
    public Result<PacketRegistration> listen(UUID playerId, PacketDirection direction,
                                              String type, PacketPriority priority,
                                              PacketListener listener) {
        if (!enabled) return Result.failure(new IllegalStateException("Packet service is disabled"));
        if (active == null) return Result.failure(new UnsupportedOperationException(
                "Packet interception requires an optional backend"));
        if (!active.interceptionSupported()) return Result.failure(new UnsupportedOperationException(
                "Active packet backend does not support interception"));
        PacketListener filtered = new FilteredPacketListener(listener);
        return Result.success(active.registerListener(playerId, direction, type, priority, filtered));
    }

    @Override
    public Result<PacketRegistration> listen(PacketDirection direction, String type,
                                              PacketPriority priority,
                                              PacketListener listener) {
        return listen(null, direction, type, priority, listener);
    }

    @Override
    public synchronized void registerBackend(PacketBackend backend) {
        if (backend == null) throw new IllegalArgumentException("backend");
        backends.removeIf(existing -> existing.id().equals(backend.id()));
        backends.add(backend);
        active = backend;
        backend.onEnable();
    }

    @Override
    public synchronized void unregisterBackend(String backendId) {
        PacketBackend removed = backends.stream()
                .filter(backend -> backend.id().equals(backendId))
                .findFirst().orElse(null);
        if (removed != null) {
            removed.onDisable();
            backends.remove(removed);
            if (active == removed) active = backends.isEmpty() ? null : backends.getLast();
        }
    }

    private void close() {
        enabled = false;
        for (PacketBackend backend : backends) backend.onDisable();
        backends.clear();
        active = null;
    }

    private record FilteredPacketListener(PacketListener delegate) implements PacketListener {
        @Override
        public void onPacket(PacketEvent event) {
            if (delegate.filter().test(event)) delegate.onPacket(event);
        }
    }
}
