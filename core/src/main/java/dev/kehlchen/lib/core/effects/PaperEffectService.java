package dev.kehlchen.lib.core.effects;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.effects.EffectHandle;
import dev.kehlchen.lib.api.effects.EffectService;
import dev.kehlchen.lib.api.scheduler.TaskHandle;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Scheduler-backed lifecycle effect handles. */
public final class PaperEffectService implements EffectService {
    private final PaperScheduler scheduler;
    private final List<EffectHandleImpl> handles = new ArrayList<>();
    private volatile boolean enabled;

    public PaperEffectService(PaperScheduler scheduler) {
        this.scheduler = scheduler;
    }
    @Override public String name() { return "effects"; }
    @Override public String version() { return "1.0"; }
    @Override public synchronized void onEnable() { enabled = true; }
    @Override public synchronized void onDisable() {
        enabled = false;
        for (EffectHandleImpl handle : handles) handle.cancel();
        handles.clear();
    }

    @Override
    public synchronized Result<EffectHandle> delayed(Plugin plugin, Duration delay, Runnable effect) {
        if (!enabled) return Result.failure(new IllegalStateException("Effect service is disabled"));
        Result<TaskHandle> result = scheduler.runSyncLater(plugin, normalize(delay), effect);
        if (!result.isSuccess()) return Result.failure(result.error().orElseThrow());
        EffectHandleImpl handle = new EffectHandleImpl(result.value().orElseThrow());
        handles.add(handle);
        return Result.success(handle);
    }

    @Override
    public synchronized Result<EffectHandle> repeating(Plugin plugin, Duration initialDelay,
                                                       Duration period, Runnable effect) {
        if (!enabled) return Result.failure(new IllegalStateException("Effect service is disabled"));
        Duration safePeriod = period == null || period.isZero() ? Duration.ofMillis(50) : period;
        Result<TaskHandle> result = scheduler.repeatSync(plugin, normalize(initialDelay), safePeriod, effect);
        if (!result.isSuccess()) return Result.failure(result.error().orElseThrow());
        EffectHandleImpl handle = new EffectHandleImpl(result.value().orElseThrow());
        handles.add(handle);
        return Result.success(handle);
    }

    @Override
    public synchronized Result<EffectHandle> atLocation(Plugin plugin, Location location,
                                                       Duration delay, Runnable effect) {
        if (!enabled) return Result.failure(new IllegalStateException("Effect service is disabled"));
        if (location == null || location.getWorld() == null)
            return Result.failure(new IllegalArgumentException("Location is required"));
        Result<TaskHandle> result = scheduler.runForLocationLater(plugin, location, normalize(delay), effect);
        if (!result.isSuccess()) return Result.failure(result.error().orElseThrow());
        EffectHandleImpl handle = new EffectHandleImpl(result.value().orElseThrow());
        handles.add(handle);
        return Result.success(handle);
    }

    private Duration normalize(Duration duration) {
        return duration == null || duration.isNegative() ? Duration.ZERO : duration;
    }

    private final class EffectHandleImpl implements EffectHandle {
        private final TaskHandle task;
        private volatile boolean active = true;
        private EffectHandleImpl(TaskHandle task) { this.task = task; }
        @Override public boolean isActive() { return active && task.isActive(); }
        @Override public void cancel() {
            active = false;
            task.close();
            synchronized (PaperEffectService.this) { handles.remove(this); }
        }
        @Override public void close() { cancel(); }
    }
}
