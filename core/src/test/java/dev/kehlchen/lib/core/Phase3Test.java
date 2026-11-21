package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.events.EventPriority;
import dev.kehlchen.lib.api.player.PlayerProfile;
import dev.kehlchen.lib.api.scheduler.SchedulerStats;
import dev.kehlchen.lib.api.scheduler.TaskChain;
import dev.kehlchen.lib.api.scheduler.Scheduler;
import dev.kehlchen.lib.api.scheduler.TaskHandle;
import dev.kehlchen.lib.api.scheduler.TaskStatus;
import dev.kehlchen.lib.api.scheduler.TaskGroup;
import dev.kehlchen.lib.core.messaging.AdventureMessaging;
import dev.kehlchen.lib.core.localization.Localization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase3Test {
    @TempDir Path temp;

    @Test
    void resultAndPlayerProfileKeepExplicitValues() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, new PlayerProfile(uuid, "Steve").uniqueId());
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfile(null, "Steve"));
        assertTrue(Result.success("ok").isSuccess());
        assertTrue(Result.failure(new IllegalStateException("bad")).error().isPresent());
    }

    @Test
    void miniMessageAndLocalizationFormattingUseComponents() {
        Localization localization = new Localization(temp, "en");
        AdventureMessaging messaging = new AdventureMessaging(localization);
        Component component = messaging.component("<green>Hello <bold>world</bold></green>");
        assertEquals("Hello world", PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals("welcome", PlainTextComponentSerializer.plainText().serialize(messaging.localized("welcome")));
    }

    @Test
    void taskChainCannotExecuteAfterCleanup() {
        TaskChain chain = new TaskChain(new TestScheduler(), null);
        chain.then(() -> {});
        chain.close();
        assertThrows(IllegalStateException.class, chain::execute);
    }

    @Test
    void phase3ValueObjectsAndPrioritiesAreStable() {
        assertEquals(TaskStatus.SCHEDULED, TaskStatus.valueOf("SCHEDULED"));
        assertEquals(6, EventPriority.values().length);
        assertEquals(3L, new SchedulerStats(10, 3, 2, 1, 4).completed());
    }

    private static final class TestScheduler implements Scheduler {
        @Override public String name() { return "test"; }
        @Override public String version() { return "1"; }
        @Override public void onEnable() {}
        @Override public void onDisable() {}
        @Override public Result<TaskHandle> runSync(Plugin plugin, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> runAsync(Plugin plugin, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> runSyncLater(Plugin plugin, Duration delay, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> runAsyncLater(Plugin plugin, Duration delay, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> repeatSync(Plugin plugin, Duration initialDelay, Duration period, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> repeatAsync(Plugin plugin, Duration initialDelay, Duration period, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> runForLocation(Plugin plugin, Location location, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> runForLocationLater(Plugin plugin, Location location, Duration delay, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> runForEntity(Plugin plugin, Entity entity, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> runForEntityLater(Plugin plugin, Entity entity, Duration delay, Runnable task) { return Result.failure(new UnsupportedOperationException()); }
        @Override public Result<TaskHandle> teleportAsync(Player player, Location location) { return Result.failure(new UnsupportedOperationException()); }
        @Override public TaskGroup createGroup() { throw new UnsupportedOperationException(); }
        @Override public TaskChain chain(Plugin plugin) { return new TaskChain(this, plugin); }
        @Override public void setExceptionHandler(Consumer<Throwable> handler) {}
        @Override public SchedulerStats statistics() { return new SchedulerStats(0, 0, 0, 0, 0); }
        @Override public boolean isMainThread() { return false; }
    }
}
