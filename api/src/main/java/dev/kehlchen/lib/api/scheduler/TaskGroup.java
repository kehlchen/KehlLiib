package dev.kehlchen.lib.api.scheduler;

import dev.kehlchen.lib.api.Resource;
import java.util.List;

/** A lifecycle-managed collection of related tasks. */
public interface TaskGroup extends Resource {
    /** Returns an immutable snapshot of tasks in this group. */
    List<TaskHandle> tasks();

    /** Adds a task handle to this group. */
    void add(TaskHandle task);

    /** Cancels every task in this group. */
    @Override
    void close();

    /** Returns whether the group has no active tasks. */
    default boolean isIdle() {
        return tasks().stream().noneMatch(TaskHandle::isActive);
    }
}
