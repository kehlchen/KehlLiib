package dev.kehlchen.lib.api.command;

import dev.kehlchen.lib.api.Lifecycle;
import org.bukkit.plugin.Plugin;

/** Public command registration and lifecycle service. */
public interface CommandService extends Lifecycle {
    CommandRegistration register(Plugin owner, CommandBuilder builder);
    boolean unregister(String name);
}
