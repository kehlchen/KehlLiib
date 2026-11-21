package dev.kehlchen.lib.api.monitor;

import java.time.Instant;
import java.util.Map;
import java.util.OptionalDouble;

public record PerformanceSnapshot(
        Instant capturedAt,
        OptionalDouble tps,
        OptionalDouble mspt,
        TickStatistics tickStatistics,
        MemoryStatistics memory,
        Map<String, Long> garbageCollections,
        long loadedChunks,
        long loadedEntities,
        int onlinePlayers,
        Map<String, WorldStatistics> worlds) {

    public PerformanceSnapshot {
        garbageCollections = Map.copyOf(garbageCollections);
        worlds = Map.copyOf(worlds);
    }

    public record TickStatistics(double averageNanos, double minimumNanos,
                                 double maximumNanos, int samples) {}

    public record MemoryStatistics(long heapUsed, long heapCommitted, long heapMax,
                               long nonHeapUsed, int currentThreadCpuPercent) {}

    public record WorldStatistics(int chunks, int entities, int players) {}
}
