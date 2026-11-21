package dev.kehlchen.lib.api.scheduler;

/** Point-in-time scheduler statistics. */
public record SchedulerStats(long submitted, long completed, long cancelled, long failed, long active) {
}
