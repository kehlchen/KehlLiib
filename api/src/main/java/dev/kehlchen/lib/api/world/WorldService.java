package dev.kehlchen.lib.api.world;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.Result;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

/** Thread-safe world lookup and ownership-safe world operations. */
public interface WorldService extends Lifecycle {
    Optional<World> find(String name);
    Optional<World> find(UUID uuid);
    boolean isLoaded(String name);
    Result<World> create(String name, World.Environment environment);
    Result<World> load(String name);
    Result<Void> unload(String name, boolean save);
    Result<Void> setDifficulty(World world, Difficulty difficulty);
    Result<Long> setTime(World world, long time);
    Result<Void> setWeather(World world, boolean storm, boolean thunder, int ticks);
    Result<Location> setSpawn(World world, Location location);
    <T> Result<T> getGameRule(World world, GameRule<T> rule);
    <T> Result<Boolean> setGameRule(World world, GameRule<T> rule, T value);
}
