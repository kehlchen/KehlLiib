package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.packet.MapPacket;
import dev.kehlchen.lib.api.packet.PacketBackend;
import dev.kehlchen.lib.api.packet.PacketDirection;
import dev.kehlchen.lib.api.packet.PacketEvent;
import dev.kehlchen.lib.api.packet.PacketListener;
import dev.kehlchen.lib.api.packet.PacketPriority;
import dev.kehlchen.lib.api.packet.PacketRegistration;
import dev.kehlchen.lib.core.metrics.InMemoryMetrics;
import dev.kehlchen.lib.core.packet.PacketServiceImpl;
import dev.kehlchen.lib.core.profiling.SimpleProfiler;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Phase8AdvancedSystemsTest {
    @Test
    void packetServiceReportsUnavailableBackendAndCanRegisterOptionalBackend() {
        PacketServiceImpl packets = new PacketServiceImpl();
        packets.onEnable();
        assertFalse(packets.interceptionAvailable());
        assertFalse(packets.send(UUID.randomUUID(), PacketDirection.CLIENTBOUND,
                "test", new MapPacket(Map.of())).isSuccess());

        TestBackend backend = new TestBackend();
        packets.registerBackend(backend);
        assertTrue(packets.interceptionAvailable());
        assertTrue(packets.modificationAvailable());

        AtomicInteger received = new AtomicInteger();
        Result<PacketRegistration> listener = packets.listen(
                PacketDirection.CLIENTBOUND, "test", PacketPriority.NORMAL,
                event -> received.incrementAndGet());
        assertTrue(listener.isSuccess());
        listener.value().orElseThrow().close();
        packets.unregisterBackend("test");
        packets.onDisable();
    }

    @Test
    void profilerCalculatesAggregateStatisticsAndCanBeDisabled() {
        SimpleProfiler profiler = new SimpleProfiler();
        profiler.onEnable();
        profiler.setEnabled(true);
        profiler.record("work", 100);
        profiler.record("work", 300);

        var statistics = profiler.snapshot().sections().get("work");
        assertEquals(2, statistics.operationCount());
        assertEquals(200, statistics.averageNanos());
        assertEquals(100, statistics.minimumNanos());
        assertEquals(300, statistics.maximumNanos());
        assertEquals(400, statistics.totalNanos());

        profiler.setEnabled(false);
        profiler.record("ignored", 900);
        assertFalse(profiler.snapshot().sections().containsKey("ignored"));
        profiler.onDisable();
    }

    @Test
    void metricsAreEnableableAndCardinalityIsBounded() {
        InMemoryMetrics metrics = new InMemoryMetrics();
        metrics.onEnable();
        metrics.setMaximumSeries(2);
        metrics.counter("requests", 2);
        metrics.gauge("players", 3);
        assertThrows(IllegalStateException.class, () -> metrics.histogram("third", 1));
        assertEquals(2, metrics.snapshot().values().get("requests"));
        assertEquals(3, metrics.snapshot().values().get("players"));

        metrics.onDisable();
    }

    private static final class TestBackend implements PacketBackend {
        @Override public String id() { return "test"; }
        @Override public String name() { return "test-packet-backend"; }
        @Override public String version() { return "1.0"; }
        @Override public boolean interceptionSupported() { return true; }
        @Override public boolean modificationSupported() { return true; }
        @Override public void onEnable() {}
        @Override public void onDisable() {}
        @Override public void close() {}

        @Override
        public Result<Void> send(UUID playerId, PacketDirection direction,
                                 String type, MapPacket packet) {
            return Result.success(null);
        }

        @Override
        public PacketRegistration registerListener(UUID playerId, PacketDirection direction,
                                                   String type, PacketPriority priority,
                                                   PacketListener listener) {
            return new PacketRegistration() {
                private boolean active = true;
                @Override public boolean active() { return active; }
                @Override public void close() { active = false; }
            };
        }
    }
}
