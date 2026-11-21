package dev.kehlchen.lib.api.metrics;

import dev.kehlchen.lib.api.Lifecycle;

import java.time.Duration;

public interface Metrics extends Lifecycle {
    void counter(String name, double increment);
    void gauge(String name, double value);
    void timer(String name, Duration duration);
    void histogram(String name, double value);
    MetricSnapshot snapshot();
    void clear();
    void setMaximumSeries(int maximumSeries);
}
