package dev.kehlchen.lib.api.metrics;

import java.time.Instant;
import java.util.Map;

public record MetricSnapshot(Instant capturedAt, Map<String, Double> values) {
    public MetricSnapshot {
        values = Map.copyOf(values);
    }
}
