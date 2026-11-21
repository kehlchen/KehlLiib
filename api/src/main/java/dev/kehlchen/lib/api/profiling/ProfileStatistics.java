package dev.kehlchen.lib.api.profiling;

public record ProfileStatistics(String name, long operationCount,
                                double averageNanos, double minimumNanos,
                                double maximumNanos, double totalNanos) {}
