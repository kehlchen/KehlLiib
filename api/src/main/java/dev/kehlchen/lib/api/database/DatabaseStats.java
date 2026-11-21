package dev.kehlchen.lib.api.database;

public record DatabaseStats(int activeConnections, int idleConnections, int totalConnections,
                            int threadsAwaitingConnection, long queriesExecuted,
                            long failures, long totalQueryTimeNanos) {}
