package dev.kehlchen.lib.core.events;

import dev.kehlchen.lib.api.Subscription;
import dev.kehlchen.lib.api.events.BukkitEventRegistry;
import dev.kehlchen.lib.api.events.EventPriority;
import dev.kehlchen.lib.api.events.EventRegistration;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Lifecycle-managed Bukkit listener registration using dedicated wrapper listeners. */
public final class PaperBukkitEventRegistry implements BukkitEventRegistry {
    private final Plugin plugin;
    private final CopyOnWriteArrayList<Registration> registrations = new CopyOnWriteArrayList<>();
    private volatile boolean enabled;
    private volatile boolean closed;

    public PaperBukkitEventRegistry(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override public String name() { return "events"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { close(); }

    @Override
    public <T extends Event> EventRegistration register(Class<T> eventType, Listener owner,
                                                          Consumer<T> listener) {
        return register(eventType, owner, EventPriority.NORMAL, false, event -> true, listener);
    }

    @Override
    public <T extends Event> EventRegistration register(Class<T> eventType, Listener owner,
                                                         EventPriority priority,
                                                         boolean ignoreCancelled,
                                                         Predicate<T> filter,
                                                         Consumer<T> listener) {
        ensureOpen();
        if (!enabled) throw new IllegalStateException("Event registry is not enabled");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(listener, "listener");

        org.bukkit.event.EventPriority bukkitPriority = switch (priority) {
            case LOWEST -> org.bukkit.event.EventPriority.LOWEST;
            case LOW -> org.bukkit.event.EventPriority.LOW;
            case NORMAL -> org.bukkit.event.EventPriority.NORMAL;
            case HIGH -> org.bukkit.event.EventPriority.HIGH;
            case HIGHEST -> org.bukkit.event.EventPriority.HIGHEST;
            case MONITOR -> org.bukkit.event.EventPriority.MONITOR;
        };

        WrapperListener wrapper = new WrapperListener();
        EventExecutor executor = (Listener ignored, Event event) -> {
            if (!eventType.isInstance(event)) return;
            T typed = eventType.cast(event);
            if (filter.test(typed)) listener.accept(typed);
        };
        Bukkit.getPluginManager().registerEvent(eventType, wrapper, bukkitPriority, executor, plugin, ignoreCancelled);
        Registration registration = new Registration(wrapper);
        registrations.add(registration);
        return registration;
    }

    public void close() {
        if (closed) return;
        closed = true;
        for (Registration registration : registrations) registration.close();
        registrations.clear();
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Event registry is closed");
    }

    private static final class WrapperListener implements Listener {
    }

    private final class Registration implements EventRegistration {
        private final WrapperListener listener;
        private volatile boolean closed;
        private Registration(WrapperListener listener) { this.listener = listener; }

        @Override
        public Subscription subscription() {
            return this::close;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            org.bukkit.event.HandlerList.unregisterAll(listener);
            registrations.remove(this);
        }
    }
}
