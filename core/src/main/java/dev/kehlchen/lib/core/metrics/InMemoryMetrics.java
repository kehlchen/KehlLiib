package dev.kehlchen.lib.core.metrics;

import dev.kehlchen.lib.api.metrics.MetricSnapshot;
import dev.kehlchen.lib.api.metrics.Metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

public final class InMemoryMetrics implements Metrics {
    private final Map<String, DoubleAdder> values = new ConcurrentHashMap<>();
    private volatile int maximumSeries = 1000;
    private volatile boolean enabled;

    @Override public String name() { return "metrics"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { enabled = false; clear(); }
    @Override public void setMaximumSeries(int maximumSeries) {
        this.maximumSeries = Math.max(1, maximumSeries);
    }
    @Override public void clear() { values.clear(); }

    @Override public void counter(String name, double increment) {
        if (!enabled) return;
        series(name).add(increment);
    }

    @Override public void gauge(String name, double value) {
        if (!enabled) return;
        series(name).reset();
        series(name).add(value);
    }

    @Override public void timer(String name, Duration duration) {
        if (!enabled) return;
        series(name).add(duration == null ? 0 : duration.toMillis());
    }

    @Override public void histogram(String name, double value) {
        if (!enabled) return;
        series(name).add(value);
    }

    @Override
    public MetricSnapshot snapshot() {
        Map<String, Double> copy = new java.util.HashMap<>();
        values.forEach((name, value) -> copy.put(name, value.sum()));
        return new MetricSnapshot(Instant.now(), copy);
    }

    private DoubleAdder series(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Metric name");
        if (values.size() >= maximumSeries && !values.containsKey(name)) {
            throw new IllegalStateException("Metric cardinality limit reached");
        }
        return values.computeIfAbsent(name, key -> new DoubleAdder());
    }
}
