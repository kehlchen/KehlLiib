package dev.kehlchen.lib.api.scheduler;

/** Runtime state of a scheduled task. */
public enum TaskStatus {
    SCHEDULED,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}
