package dev.kehlchen.lib.api.monitor;

import dev.kehlchen.lib.api.Lifecycle;

import java.time.Duration;
import java.util.List;

public interface PerformanceMonitor extends Lifecycle {
    PerformanceSnapshot snapshot();
    List<PerformanceSnapshot> history();
    void setSamplingInterval(Duration interval);
    void setHistoryLimit(int limit);
}
