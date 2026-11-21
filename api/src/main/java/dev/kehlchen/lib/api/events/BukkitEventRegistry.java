package dev.kehlchen.lib.api.events;

import dev.kehlchen.lib.api.Lifecycle;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Lifecycle-managed registration API for Bukkit events. */
public interface BukkitEventRegistry extends Lifecycle {
    <T extends Event> EventRegistration register(Class<T> eventType, Listener owner,
                                                   Consumer<T> listener);

    <T extends Event> EventRegistration register(Class<T> eventType, Listener owner,
                                                   EventPriority priority,
                                                   boolean ignoreCancelled,
                                                   Predicate<T> filter,
                                                   Consumer<T> listener);
}
