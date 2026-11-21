package dev.kehlchen.lib.api.effects;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Location;

import java.time.Duration;

/** Lifecycle-managed delayed and repeating effect utilities. */
public interface EffectService extends Lifecycle {
    Result<EffectHandle> delayed(org.bukkit.plugin.Plugin plugin, Duration delay, Runnable effect);
    Result<EffectHandle> repeating(org.bukkit.plugin.Plugin plugin, Duration initialDelay,
                                   Duration period, Runnable effect);
    Result<EffectHandle> atLocation(org.bukkit.plugin.Plugin plugin, Location location,
                                    Duration delay, Runnable effect);
}
