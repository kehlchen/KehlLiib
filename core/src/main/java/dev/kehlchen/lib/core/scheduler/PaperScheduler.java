package dev.kehlchen.lib.core.scheduler;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.scheduler.Scheduler;
import dev.kehlchen.lib.api.scheduler.SchedulerStats;
import dev.kehlchen.lib.api.scheduler.TaskChain;
import dev.kehlchen.lib.api.scheduler.TaskGroup;
import dev.kehlchen.lib.api.scheduler.TaskHandle;
import dev.kehlchen.lib.api.scheduler.TaskStatus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Paper and Folia scheduler implementation using reflection for optional Folia APIs. */
public final class PaperScheduler implements Scheduler {
    private final Plugin owner;
    private volatile Consumer<Throwable> exceptionHandler = throwable ->
            Bukkit.getLogger().log(java.util.logging.Level.SEVERE, "KehlLib scheduled task failed", throwable);
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong cancelled = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final CopyOnWriteArrayList<TaskHandleImpl> handles = new CopyOnWriteArrayList<>();
    private volatile boolean closed;
    private volatile boolean folia;

    public PaperScheduler(Plugin owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        detectFolia();
    }

    @Override public String name() { return "scheduler"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { detectFolia(); }
    @Override public void onDisable() { close(); }

    @Override
    public Result<TaskHandle> runSync(Plugin plugin, Runnable task) {
        if (folia) {
            return foliaGlobal(pluginOrOwner(plugin), task, null, false);
        }
        return traditional(pluginOrOwner(plugin), task, 0L, 0L);
    }

    @Override
    public Result<TaskHandle> runAsync(Plugin plugin, Runnable task) {
        if (folia) return foliaAsync(pluginOrOwner(plugin), task, null, null);
        return traditional(pluginOrOwner(plugin), task, 0L, 0L, true);
    }

    @Override
    public Result<TaskHandle> runSyncLater(Plugin plugin, Duration delay, Runnable task) {
        if (folia) return foliaGlobal(pluginOrOwner(plugin), task, delay, false);
        return traditional(pluginOrOwner(plugin), task, ticks(delay), 0L);
    }

    @Override
    public Result<TaskHandle> runAsyncLater(Plugin plugin, Duration delay, Runnable task) {
        if (folia) return foliaAsync(pluginOrOwner(plugin), task, delay, null);
        return traditional(pluginOrOwner(plugin), task, ticks(delay), 0L, true);
    }

    @Override
    public Result<TaskHandle> repeatSync(Plugin plugin, Duration initialDelay, Duration period, Runnable task) {
        if (folia) return foliaGlobal(pluginOrOwner(plugin), task, initialDelay, period);
        return traditional(pluginOrOwner(plugin), task, ticks(initialDelay), ticks(period));
    }

    @Override
    public Result<TaskHandle> repeatAsync(Plugin plugin, Duration initialDelay, Duration period, Runnable task) {
        if (folia) return foliaAsync(pluginOrOwner(plugin), task, initialDelay, period);
        return traditional(pluginOrOwner(plugin), task, ticks(initialDelay), ticks(period), true);
    }

    @Override
    public Result<TaskHandle> runForLocation(Plugin plugin, Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        if (!folia) return runSync(plugin, task);
        Object region = invoke(Bukkit.getServer(), "getRegionScheduler");
        if (region == null) return failure(new UnsupportedOperationException("Folia region scheduler is unavailable"));
        return invokeRegion(region, pluginOrOwner(plugin), location, task, null, null);
    }

    @Override
    public Result<TaskHandle> runForLocationLater(Plugin plugin, Location location, Duration delay, Runnable task) {
        Objects.requireNonNull(location, "location");
        if (!folia) return runSyncLater(plugin, delay, task);
        Object region = invoke(Bukkit.getServer(), "getRegionScheduler");
        if (region == null) return failure(new UnsupportedOperationException("Folia region scheduler is unavailable"));
        return invokeRegion(region, pluginOrOwner(plugin), location, task, delay, null);
    }

    @Override
    public Result<TaskHandle> runForEntity(Plugin plugin, Entity entity, Runnable task) {
        Objects.requireNonNull(entity, "entity");
        if (!folia) return runSync(plugin, () -> {
            if (entity.isValid()) task.run();
        });
        Object scheduler = invoke(entity, "getScheduler");
        if (scheduler == null) return failure(new UnsupportedOperationException("Folia entity scheduler is unavailable"));
        return invokeEntity(scheduler, pluginOrOwner(plugin), entity, task, null, null);
    }

    @Override
    public Result<TaskHandle> runForEntityLater(Plugin plugin, Entity entity, Duration delay, Runnable task) {
        Objects.requireNonNull(entity, "entity");
        if (!folia) return runSyncLater(plugin, delay, () -> {
            if (entity.isValid()) task.run();
        });
        Object scheduler = invoke(entity, "getScheduler");
        if (scheduler == null) return failure(new UnsupportedOperationException("Folia entity scheduler is unavailable"));
        return invokeEntity(scheduler, pluginOrOwner(plugin), entity, task, delay, null);
    }

    @Override
    public Result<TaskHandle> teleportAsync(Player player, Location location) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(location, "location");
        if (!player.isOnline()) return failure(new IllegalStateException("Player is no longer online"));
        if (folia) {
            return runForEntity(owner, player, () -> player.teleport(location));
        }
        return runSync(owner, () -> player.teleport(location));
    }

    @Override
    public TaskGroup createGroup() {
        return new PaperTaskGroup();
    }

    @Override
    public TaskChain chain(Plugin plugin) {
        return new TaskChain(this, plugin == null ? owner : plugin);
    }

    @Override
    public void setExceptionHandler(Consumer<Throwable> handler) {
        exceptionHandler = Objects.requireNonNull(handler, "handler");
    }

    @Override
    public SchedulerStats statistics() {
        long active = handles.stream().filter(TaskHandle::isActive).count();
        return new SchedulerStats(submitted.get(), completed.get(), cancelled.get(), failed.get(), active);
    }

    @Override
    public boolean isMainThread() {
        if (Bukkit.isPrimaryThread()) return true;
        if (folia) {
            try {
                Method method = Bukkit.getServer().getClass().getMethod("isOwnedByCurrentRegion");
                return method.invoke(Bukkit.getServer()) instanceof Boolean owned && owned;
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }
        return false;
    }

    private void detectFolia() {
        try {
            Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            folia = true;
        } catch (ReflectiveOperationException ignored) {
            folia = false;
        }
    }

    private Plugin pluginOrOwner(Plugin plugin) {
        return plugin == null ? owner : plugin;
    }

    private long ticks(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return Math.max(0L, duration.toMillis() / 50L);
    }

    private Result<TaskHandle> traditional(Plugin plugin, Runnable task, long delay, long period) {
        return traditional(plugin, task, delay, period, false);
    }

    private Result<TaskHandle> traditional(Plugin plugin, Runnable task, long delay, long period, boolean async) {
        ensureOpen();
        TaskHandleImpl handle = new TaskHandleImpl();
        handles.add(handle);
        submitted.incrementAndGet();
        try {
            if (async) {
                if (period > 0) {
                    handle.delegate = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, wrapped(handle, task, true), delay, period);
                } else if (delay > 0) {
                    handle.delegate = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, wrapped(handle, task), delay);
                } else {
                    handle.delegate = Bukkit.getScheduler().runTaskAsynchronously(plugin, wrapped(handle, task));
                }
            } else if (period > 0) {
                handle.delegate = Bukkit.getScheduler().runTaskTimer(plugin, wrapped(handle, task, true), delay, period);
            } else if (delay > 0) {
                handle.delegate = Bukkit.getScheduler().runTaskLater(plugin, wrapped(handle, task), delay);
            } else {
                handle.delegate = Bukkit.getScheduler().runTask(plugin, wrapped(handle, task));
            }
            handle.status = TaskStatus.SCHEDULED;
            return Result.success(handle);
        } catch (Throwable error) {
            handle.status = TaskStatus.FAILED;
            failed.incrementAndGet();
            return Result.failure(error);
        }
    }

    private Runnable wrapped(TaskHandleImpl handle, Runnable task) {
        return wrapped(handle, task, false);
    }

    private Runnable wrapped(TaskHandleImpl handle, Runnable task, boolean repeating) {
        return () -> {
            if (!handle.isActive()) return;
            handle.status = TaskStatus.RUNNING;
            try {
                task.run();
                if (handle.status != TaskStatus.CANCELLED && !repeating) {
                    handle.status = TaskStatus.COMPLETED;
                    completed.incrementAndGet();
                } else if (repeating && handle.status != TaskStatus.CANCELLED) {
                    handle.status = TaskStatus.SCHEDULED;
                }
            } catch (Throwable error) {
                handle.status = TaskStatus.FAILED;
                failed.incrementAndGet();
                exceptionHandler.accept(error);
            }
        };
    }

    private Result<TaskHandle> foliaGlobal(Plugin plugin, Runnable task, Duration delay, Object period) {
        ensureOpen();
        Object scheduler = invoke(Bukkit.getServer(), "getGlobalRegionScheduler");
        if (scheduler == null) return failure(new UnsupportedOperationException("Folia global region scheduler is unavailable"));
        TaskHandleImpl handle = new TaskHandleImpl();
        handles.add(handle);
        submitted.incrementAndGet();
        try {
            Object result;
            if (period instanceof Duration repeating) {
                result = invokeConsumerMethod(scheduler, "runAtFixedRate", plugin, delay, repeating, task);
            } else if (delay != null) {
                result = invokeConsumerMethod(scheduler, "runDelayed", plugin, delay, null, task);
            } else {
                result = invokeConsumerMethod(scheduler, "run", plugin, null, null, task);
            }
            handle.delegate = result;
            handle.status = TaskStatus.SCHEDULED;
            return Result.success(handle);
        } catch (Throwable error) {
            handle.status = TaskStatus.FAILED;
            failed.incrementAndGet();
            return Result.failure(error);
        }
    }

    private Result<TaskHandle> foliaAsync(Plugin plugin, Runnable task, Duration delay, Duration period) {
        ensureOpen();
        Object scheduler = invoke(Bukkit.getServer(), "getAsyncScheduler");
        if (scheduler == null) return failure(new UnsupportedOperationException("Folia async scheduler is unavailable"));
        TaskHandleImpl handle = new TaskHandleImpl();
        handles.add(handle);
        submitted.incrementAndGet();
        try {
            Object result;
            if (period != null) {
                result = invokeAsyncMethod(scheduler, "runAtFixedRate", plugin, task, delay, period);
            } else if (delay != null) {
                result = invokeAsyncMethod(scheduler, "runDelayed", plugin, task, delay, null);
            } else {
                result = invokeAsyncMethod(scheduler, "runNow", plugin, task, null, null);
            }
            handle.delegate = result;
            handle.status = TaskStatus.SCHEDULED;
            return Result.success(handle);
        } catch (Throwable error) {
            handle.status = TaskStatus.FAILED;
            failed.incrementAndGet();
            return Result.failure(error);
        }
    }

    private Object invokeAsyncMethod(Object target, String name, Plugin plugin, Runnable task,
                                     Duration delay, Duration period) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name)) continue;
            Class<?>[] types = method.getParameterTypes();
            if (types.length >= 2 && types[0].isInstance(plugin) && types[1].getName().equals("java.util.function.Consumer")) {
                Consumer<Object> consumer = ignored -> executeWrapped(task);
                Object[] args = types.length == 4
                        ? new Object[]{plugin, consumer, ticks(delay), ticks(period)}
                        : types.length == 3
                        ? new Object[]{plugin, consumer, ticks(delay)}
                        : new Object[]{plugin, consumer};
                return invoke(target, method, args);
            }
        }
        throw new IllegalStateException("Unsupported Folia async scheduler method: " + name);
    }

    private Object invokeConsumerMethod(Object target, String name, Plugin plugin,
                                        Duration initialDelay, Duration period, Runnable task) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name)) continue;
            Class<?>[] types = method.getParameterTypes();
            if (types.length < 2 || !types[0].isInstance(plugin)
                    || !types[1].getName().equals("java.util.function.Consumer")) continue;
            Consumer<Object> consumer = ignored -> executeWrapped(task);
            Object[] args;
            if (types.length == 4) args = new Object[]{plugin, consumer, ticks(initialDelay), ticks(period)};
            else if (types.length == 3) args = new Object[]{plugin, consumer, ticks(initialDelay)};
            else args = new Object[]{plugin, consumer};
            return invoke(target, method, args);
        }
        throw new IllegalStateException("Unsupported Folia global-region scheduler method: " + name);
    }

    private Result<TaskHandle> invokeRegion(Object region, Plugin plugin, Location location, Runnable task,
                                            Duration delay, Duration period) {
        try {
            String methodName = period != null ? "runAtFixedRate" : delay != null ? "runDelayed" : "run";
            for (Method method : region.getClass().getMethods()) {
                if (!method.getName().equals(methodName)) continue;
                Class<?>[] types = method.getParameterTypes();
                if (types.length < 3 || !types[0].isInstance(plugin)
                        || !types[1].isInstance(location)
                        || !types[2].getName().equals("java.util.function.Consumer")) continue;
                Consumer<Object> consumer = ignored -> executeWrapped(task);
                TaskHandleImpl handle = new TaskHandleImpl();
                handles.add(handle);
                submitted.incrementAndGet();
                Object result;
                if (types.length == 5) result = invoke(region, method,
                        new Object[]{plugin, location, consumer, ticks(delay), ticks(period)});
                else if (types.length == 4) result = invoke(region, method,
                        new Object[]{plugin, location, consumer, ticks(delay)});
                else result = invoke(region, method, new Object[]{plugin, location, consumer});
                handle.delegate = result;
                handle.status = TaskStatus.SCHEDULED;
                return Result.success(handle);
            }
            return failure(new UnsupportedOperationException("Folia region scheduler API shape is unsupported"));
        } catch (Throwable error) {
            return Result.failure(error);
        }
    }

    private Result<TaskHandle> invokeEntity(Object entityScheduler, Plugin plugin, Entity entity, Runnable task,
                                           Duration delay, Duration period) {
        try {
            String methodName = period != null ? "runAtFixedRate" : delay != null ? "runDelayed" : "run";
            for (Method method : entityScheduler.getClass().getMethods()) {
                if (!method.getName().equals(methodName)) continue;
                Class<?>[] types = method.getParameterTypes();
                if (types.length < 2 || !types[0].isInstance(plugin)
                        || (types.length > 1 && !types[1].getName().equals("java.util.function.Consumer"))) continue;
                Consumer<Object> consumer = ignored -> executeWrapped(() -> {
                    if (entity.isValid()) task.run();
                });
                TaskHandleImpl handle = new TaskHandleImpl();
                handles.add(handle);
                submitted.incrementAndGet();
                Object result;
                if (types.length == 4) result = invoke(entityScheduler, method,
                        new Object[]{plugin, consumer, ticks(delay), ticks(period)});
                else if (types.length == 3) result = invoke(entityScheduler, method,
                        new Object[]{plugin, consumer, ticks(delay)});
                else result = invoke(entityScheduler, method, new Object[]{plugin, consumer});
                handle.delegate = result;
                handle.status = TaskStatus.SCHEDULED;
                return Result.success(handle);
            }
            return failure(new UnsupportedOperationException("Folia entity scheduler API shape is unsupported"));
        } catch (Throwable error) {
            return Result.failure(error);
        }
    }

    private void executeWrapped(Runnable task) {
        try {
            task.run();
            completed.incrementAndGet();
        } catch (Throwable error) {
            failed.incrementAndGet();
            exceptionHandler.accept(error);
        }
    }

    private Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private Object invoke(Object target, Method method, Object[] args) {
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not invoke Folia scheduler", error);
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Scheduler is closed");
    }

    private <T> Result<T> failure(Throwable error) {
        return Result.failure(error);
    }

    public void close() {
        if (closed) return;
        closed = true;
        handles.forEach(TaskHandleImpl::close);
    }

    private final class TaskHandleImpl implements TaskHandle {
        private volatile Object delegate;
        private volatile TaskStatus status = TaskStatus.SCHEDULED;

        @Override public long id() { return delegate == null ? 0L : System.identityHashCode(delegate); }
        @Override public TaskStatus status() { return status; }
        @Override public boolean isActive() {
            return status == TaskStatus.SCHEDULED || status == TaskStatus.RUNNING;
        }
        @Override public void close() {
            if (status == TaskStatus.CANCELLED || status == TaskStatus.COMPLETED) return;
            status = TaskStatus.CANCELLED;
            cancelled.incrementAndGet();
            try {
                if (delegate instanceof org.bukkit.scheduler.BukkitTask task) {
                    task.cancel();
                } else if (delegate != null) {
                    for (Method method : delegate.getClass().getMethods()) {
                        if (method.getName().equals("cancel") && method.getParameterCount() == 0) {
                            method.invoke(delegate);
                            break;
                        }
                    }
                }
            } catch (Throwable error) {
                exceptionHandler.accept(error);
            }
        }
    }

    private final class PaperTaskGroup implements TaskGroup {
        private final List<TaskHandle> tasks = new CopyOnWriteArrayList<>();
        @Override public List<TaskHandle> tasks() { return Collections.unmodifiableList(new ArrayList<>(tasks)); }
        @Override public void add(TaskHandle task) { tasks.add(Objects.requireNonNull(task, "task")); }
        @Override public void close() { tasks.forEach(TaskHandle::close); }
    }
}
