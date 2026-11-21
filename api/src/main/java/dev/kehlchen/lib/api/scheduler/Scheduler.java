package dev.kehlchen.lib.api.scheduler;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Bukkit and Folia compatible task scheduling abstraction. */
public interface Scheduler extends Lifecycle, Executor {
    /** Runs a task synchronously, choosing the global Folia scheduler when necessary. */
    Result<TaskHandle> runSync(Plugin plugin, Runnable task);

    /** Runs a task asynchronously. */
    Result<TaskHandle> runAsync(Plugin plugin, Runnable task);

    /** Runs a synchronous task after a delay. */
    Result<TaskHandle> runSyncLater(Plugin plugin, Duration delay, Runnable task);

    /** Runs a task asynchronously after a delay. */
    Result<TaskHandle> runAsyncLater(Plugin plugin, Duration delay, Runnable task);

    /** Runs a repeating synchronous task. */
    Result<TaskHandle> repeatSync(Plugin plugin, Duration initialDelay, Duration period, Runnable task);

    /** Runs a repeating asynchronous task. */
    Result<TaskHandle> repeatAsync(Plugin plugin, Duration initialDelay, Duration period, Runnable task);

    /** Runs work on the region owning a location, or returns an unsafe-operation error. */
    Result<TaskHandle> runForLocation(Plugin plugin, Location location, Runnable task);

    /** Runs work on the region owning a location after a delay. */
    Result<TaskHandle> runForLocationLater(Plugin plugin, Location location, Duration delay, Runnable task);

    /** Runs work on the region owning an entity, or returns an unsafe-operation error. */
    Result<TaskHandle> runForEntity(Plugin plugin, Entity entity, Runnable task);

    /** Runs work on the entity scheduler after a delay. */
    Result<TaskHandle> runForEntityLater(Plugin plugin, Entity entity, Duration delay, Runnable task);

    /** Teleports a player asynchronously without unsafe main-thread access. */
    Result<TaskHandle> teleportAsync(Player player, org.bukkit.Location location);

    /** Creates a lifecycle-managed task group. */
    TaskGroup createGroup();

    /** Returns a sequential task chain. */
    TaskChain chain(Plugin plugin);

    /** Sets a handler for uncaught scheduler task failures. */
    void setExceptionHandler(Consumer<Throwable> handler);

    /** Returns scheduler counters. */
    SchedulerStats statistics();

    /** Returns whether code is executing on the server main thread or a compatible global region thread. */
    boolean isMainThread();

    @Override
    default void execute(Runnable command) {
        runSync(null, command);
    }
}
