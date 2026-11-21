package dev.kehlchen.lib.core.profiling;

import dev.kehlchen.lib.api.profiling.ProfileSnapshot;
import dev.kehlchen.lib.api.profiling.ProfileStatistics;
import dev.kehlchen.lib.api.profiling.Profiler;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SimpleProfiler implements Profiler {
    private final Map<String, Section> sections = new ConcurrentHashMap<>();
    private volatile boolean enabled;
    private volatile int historyLimit = 100;

    @Override public String name() { return "profiler"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = false; }
    @Override public void onDisable() { enabled = false; sections.clear(); }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setHistoryLimit(int limit) { historyLimit = Math.max(1, limit); }
    @Override public void clear() { sections.clear(); }

    @Override
    public void record(String name, long durationNanos) {
        if (!enabled || name == null) return;
        if (sections.size() >= historyLimit && !sections.containsKey(name)) return;
        sections.computeIfAbsent(name, key -> new Section()).record(durationNanos);
    }

    @Override
    public <T> T measure(String name, Callable<T> operation) throws Exception {
        if (!enabled) return operation.call();
        long start = System.nanoTime();
        try {
            return operation.call();
        } finally {
            record(name, System.nanoTime() - start);
        }
    }

    @Override
    public void measure(String name, Runnable operation) {
        if (!enabled) { operation.run(); return; }
        long start = System.nanoTime();
        try {
            operation.run();
        } finally {
            record(name, System.nanoTime() - start);
        }
    }

    @Override
    public ProfileSnapshot snapshot() {
        Map<String, ProfileStatistics> values = new java.util.HashMap<>();
        sections.forEach((name, section) -> values.put(name, section.snapshot(name)));
        return new ProfileSnapshot(Instant.now(), values);
    }

    private static final class Section {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong total = new AtomicLong();
        private final AtomicLong minimum = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maximum = new AtomicLong(Long.MIN_VALUE);

        synchronized void record(long value) {
            if (value < 0) value = 0;
            final long duration = value;
            count.incrementAndGet();
            total.addAndGet(duration);
            minimum.updateAndGet(current -> Math.min(current, duration));
            maximum.updateAndGet(current -> Math.max(current, duration));
        }

        ProfileStatistics snapshot(String name) {
            long operations = count.get();
            long totalNanos = total.get();
            return new ProfileStatistics(name, operations,
                    operations == 0 ? 0 : (double) totalNanos / operations,
                    operations == 0 ? 0 : minimum.get(),
                    operations == 0 ? 0 : maximum.get(),
                    totalNanos);
        }
    }
}
