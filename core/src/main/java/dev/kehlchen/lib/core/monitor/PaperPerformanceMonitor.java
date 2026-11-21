package dev.kehlchen.lib.core.monitor;

import dev.kehlchen.lib.api.monitor.PerformanceMonitor;
import dev.kehlchen.lib.api.monitor.PerformanceSnapshot;
import dev.kehlchen.lib.api.scheduler.Scheduler;
import dev.kehlchen.lib.api.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicReference;

public final class PaperPerformanceMonitor implements PerformanceMonitor {
    private final Plugin plugin;
    private final Scheduler scheduler;
    private final ArrayDeque<PerformanceSnapshot> history = new ArrayDeque<>();
    private final ArrayDeque<Double> ticks = new ArrayDeque<>();
    private final AtomicReference<PerformanceSnapshot> current = new AtomicReference<>();
    private volatile Duration interval = Duration.ofSeconds(5);
    private volatile int historyLimit = 240;
    private volatile TaskHandle task;
    private volatile boolean enabled;

    public PaperPerformanceMonitor(Plugin plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        current.set(buildWithoutServerValues());
    }

    @Override public String name() { return "performance-monitor"; }
    @Override public String version() { return "1.0"; }

    @Override
    public synchronized void onEnable() {
        enabled = true;
        startTask();
    }

    @Override
    public synchronized void onDisable() {
        enabled = false;
        if (task != null) task.close();
        history.clear();
        ticks.clear();
    }

    @Override public void setSamplingInterval(Duration interval) {
        this.interval = interval == null || interval.isNegative() || interval.isZero()
                ? Duration.ofSeconds(5) : interval;
        if (enabled) startTask();
    }

    @Override
    public synchronized void setHistoryLimit(int limit) {
        historyLimit = Math.max(1, limit);
        while (history.size() > historyLimit) history.removeFirst();
    }

    @Override
    public PerformanceSnapshot snapshot() {
        if (enabled && scheduler.isMainThread()) capture();
        return current.get();
    }

    @Override
    public synchronized List<PerformanceSnapshot> history() {
        return new ArrayList<>(history);
    }

    private synchronized void startTask() {
        if (task != null) task.close();
        task = scheduler.repeatSync(plugin, interval, interval, this::capture)
                .value().orElse(null);
    }

    private synchronized void capture() {
        PerformanceSnapshot snapshot = buildWithoutServerValues();
        try {
            OptionalDouble tps = serverDouble("getTPS", 0);
            OptionalDouble mspt = serverDouble("getAverageTickTime", -1);
            if (mspt.isPresent()) {
                ticks.addLast(mspt.getAsDouble() * 1_000_000.0);
                while (ticks.size() > 1_000) ticks.removeFirst();
            }
            snapshot = buildSnapshot(tps, mspt);
        } catch (RuntimeException ignored) {
            snapshot = buildWithoutServerValues();
        }
        current.set(snapshot);
        history.addLast(snapshot);
        while (history.size() > historyLimit) history.removeFirst();
    }

    private PerformanceSnapshot buildWithoutServerValues() {
        return buildSnapshot(OptionalDouble.empty(), OptionalDouble.empty());
    }

    private PerformanceSnapshot buildSnapshot(OptionalDouble tps, OptionalDouble mspt) {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        Map<String, Long> gc = new HashMap<>();
        ManagementFactory.getGarbageCollectorMXBeans().forEach(bean ->
                gc.put(bean.getName(), bean.getCollectionCount()));

        double average = 0, minimum = 0, maximum = 0;
        if (!ticks.isEmpty()) {
            minimum = Double.POSITIVE_INFINITY;
            for (double tick : ticks) {
                average += tick;
                minimum = Math.min(minimum, tick);
                maximum = Math.max(maximum, tick);
            }
            average /= ticks.size();
        }

        long chunks = 0, entities = 0;
        Map<String, PerformanceSnapshot.WorldStatistics> worlds = new HashMap<>();
        int online = Bukkit.getServer() == null ? 0 : Bukkit.getOnlinePlayers().size();
        try {
            for (World world : Bukkit.getWorlds()) {
                int worldChunks = world.getLoadedChunks().length;
                int worldEntities = world.getEntities().size();
                chunks += worldChunks;
                entities += worldEntities;
                worlds.put(world.getName(), new PerformanceSnapshot.WorldStatistics(
                        worldChunks, worldEntities, world.getPlayers().size()));
            }
        } catch (RuntimeException ignored) {
            // Server API access is optional and snapshot remains valid.
        }

        int cpuPercent = -1;
        try {
            Object bean = ManagementFactory.getOperatingSystemMXBean();
            Method method = bean.getClass().getMethod("getProcessCpuLoad");
            Object value = method.invoke(bean);
            if (value instanceof Double cpu && cpu >= 0) cpuPercent = (int) (cpu * 100);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            cpuPercent = 0;
        }

        return new PerformanceSnapshot(
                Instant.now(),
                tps,
                mspt,
                new PerformanceSnapshot.TickStatistics(average, minimum == Double.POSITIVE_INFINITY ? 0 : minimum,
                        maximum, ticks.size()),
                new PerformanceSnapshot.MemoryStatistics(heap.getUsed(), heap.getCommitted(), heap.getMax(),
                        nonHeap.getUsed(), cpuPercent),
                gc,
                chunks,
                entities,
                online,
                worlds);
    }

    private OptionalDouble serverDouble(String methodName, int arrayIndex) {
        try {
            Method method = Bukkit.getServer().getClass().getMethod(methodName);
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof double[] values && values.length > arrayIndex && arrayIndex >= 0) {
                return OptionalDouble.of(values[arrayIndex]);
            }
            if (value instanceof Double number) return OptionalDouble.of(number);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.empty();
    }
}
