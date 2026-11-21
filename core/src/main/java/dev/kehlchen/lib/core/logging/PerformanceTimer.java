package dev.kehlchen.lib.core.logging;

import java.time.Duration;

public final class PerformanceTimer implements AutoCloseable {
    private final KehlLibLogger logger;
    private final String operation;
    private final long start;

    public PerformanceTimer(KehlLibLogger logger, String operation) {
        this.logger = logger;
        this.operation = operation;
        this.start = System.nanoTime();
    }
    public static PerformanceTimer start(KehlLibLogger logger, String operation) { return new PerformanceTimer(logger, operation); }
    public Duration elapsed() { return Duration.ofNanos(System.nanoTime() - start); }
    @Override public void close() { logger.debug(operation + " completed in " + elapsed().toMillis() + "ms"); }
}
