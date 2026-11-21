package dev.kehlchen.lib.api.scheduler;

import dev.kehlchen.lib.api.Resource;

/** Handle for a task managed by {@link Scheduler}. */
public interface TaskHandle extends Resource {
    /** Returns the stable task identifier. */
    long id();

    /** Returns the current task status. */
    TaskStatus status();

    /** Returns whether this task is active. */
    boolean isActive();

    /** Cancels this task. */
    @Override
    void close();
}
