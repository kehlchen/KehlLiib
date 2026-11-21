package dev.kehlchen.lib.api.scheduler;

import dev.kehlchen.lib.api.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Sequentially executes a list of synchronous tasks. */
public final class TaskChain implements Resource {
    private final Scheduler scheduler;
    private final org.bukkit.plugin.Plugin plugin;
    private final List<Runnable> steps = new ArrayList<>();
    private volatile boolean closed;

    public TaskChain(Scheduler scheduler, org.bukkit.plugin.Plugin plugin) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.plugin = plugin;
    }

    /** Appends a step. */
    public TaskChain then(Runnable step) {
        Objects.requireNonNull(step, "step");
        if (closed) throw new IllegalStateException("Task chain is closed");
        steps.add(step);
        return this;
    }

    /** Executes all steps in order, scheduling each successor after the previous step completes. */
    public void execute() {
        if (closed) throw new IllegalStateException("Task chain is closed");
        executeFrom(0);
    }

    private void executeFrom(int index) {
        if (closed || index >= steps.size()) return;
        scheduler.runSync(plugin, () -> {
            if (closed) return;
            steps.get(index).run();
            executeFrom(index + 1);
        });
    }

    @Override
    public void close() {
        closed = true;
    }
}
