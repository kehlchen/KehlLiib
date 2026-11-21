package dev.kehlchen.lib.api.events;

import dev.kehlchen.lib.api.Resource;
import dev.kehlchen.lib.api.Subscription;

/** Registration handle for a Bukkit or internal event listener. */
public interface EventRegistration extends Resource {
    Subscription subscription();
    @Override void close();
}
