package dev.kehlchen.lib.api.profiling;

import dev.kehlchen.lib.api.Lifecycle;

import java.util.concurrent.Callable;

public interface Profiler extends Lifecycle {
    void setEnabled(boolean enabled);
    boolean enabled();
    void record(String name, long durationNanos);
    <T> T measure(String name, Callable<T> operation) throws Exception;
    void measure(String name, Runnable operation);
    ProfileSnapshot snapshot();
    void clear();
    void setHistoryLimit(int limit);
}
