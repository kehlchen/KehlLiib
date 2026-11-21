package dev.kehlchen.lib.core.world;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.world.WorldService;
import dev.kehlchen.lib.core.scheduler.PaperScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

/** Paper implementation of bounded world operations. */
public final class PaperWorldService implements WorldService {
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private volatile boolean enabled;

    public PaperWorldService(Plugin plugin, PaperScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override public String name() { return "world"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() { enabled = true; }
    @Override public void onDisable() { enabled = false; }

    @Override public Optional<World> find(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(Bukkit.getWorld(name));
    }
    @Override public Optional<World> find(UUID uuid) {
        return uuid == null ? Optional.empty() : Optional.ofNullable(Bukkit.getWorld(uuid));
    }
    @Override public boolean isLoaded(String name) { return name != null && Bukkit.getWorld(name) != null; }

    @Override
    public Result<World> create(String name, World.Environment environment) {
        if (!enabled) return Result.failure(new IllegalStateException("World service is disabled"));
        if (name == null || name.isBlank()) return Result.failure(new IllegalArgumentException("World name is required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("World creation must run on a global/region thread"));
        try {
            WorldCreator creator = new WorldCreator(name);
            if (environment != null) creator.environment(environment);
            World world = Bukkit.createWorld(creator);
            return world == null ? Result.failure(new IllegalStateException("World could not be created: " + name))
                    : Result.success(world);
        } catch (Throwable failure) {
            return Result.failure(failure);
        }
    }

    @Override
    public Result<World> load(String name) {
        if (name == null || name.isBlank()) return Result.failure(new IllegalArgumentException("World name is required"));
        World existing = Bukkit.getWorld(name);
        if (existing != null) return Result.success(existing);
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("World loading must run on a global/region thread"));
        try {
            World world = Bukkit.createWorld(new WorldCreator(name));
            return world == null ? Result.failure(new IllegalStateException("World could not be loaded: " + name))
                    : Result.success(world);
        } catch (Throwable failure) {
            return Result.failure(failure);
        }
    }

    @Override
    public Result<Void> unload(String name, boolean save) {
        if (name == null) return Result.failure(new IllegalArgumentException("World name is required"));
        World world = Bukkit.getWorld(name);
        if (world == null) return Result.failure(new IllegalStateException("World is not loaded: " + name));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("World unloading must run on a global/region thread"));
        try {
            return Bukkit.unloadWorld(world, save) ? Result.success(null)
                    : Result.failure(new IllegalStateException("World unload was rejected: " + name));
        } catch (Throwable failure) {
            return Result.failure(failure);
        }
    }

    @Override
    public Result<Void> setDifficulty(World world, Difficulty difficulty) {
        if (world == null || difficulty == null) return Result.failure(new IllegalArgumentException("World and difficulty are required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread world access"));
        try { world.setDifficulty(difficulty); return Result.success(null); } catch (Throwable failure) { return Result.failure(failure); }
    }

    @Override
    public Result<Long> setTime(World world, long time) {
        if (world == null) return Result.failure(new IllegalArgumentException("World is required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread world access"));
        try { world.setTime(time); return Result.success(world.getTime()); } catch (Throwable failure) { return Result.failure(failure); }
    }

    @Override
    public Result<Void> setWeather(World world, boolean storm, boolean thunder, int ticks) {
        if (world == null) return Result.failure(new IllegalArgumentException("World is required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread world access"));
        try {
            world.setStorm(storm);
            world.setThundering(thunder);
            world.setWeatherDuration(Math.max(0, ticks));
            return Result.success(null);
        } catch (Throwable failure) { return Result.failure(failure); }
    }

    @Override
    public Result<Location> setSpawn(World world, Location location) {
        if (world == null || location == null || location.getWorld() == null)
            return Result.failure(new IllegalArgumentException("World and location are required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread world access"));
        try {
            world.setSpawnLocation(location);
            return Result.success(world.getSpawnLocation());
        } catch (Throwable failure) { return Result.failure(failure); }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Result<T> getGameRule(World world, GameRule<T> rule) {
        if (world == null || rule == null) return Result.failure(new IllegalArgumentException("World and game rule are required"));
        try { return Result.success(world.getGameRuleValue(rule)); } catch (Throwable failure) { return Result.failure(failure); }
    }

    @Override
    public <T> Result<Boolean> setGameRule(World world, GameRule<T> rule, T value) {
        if (world == null || rule == null) return Result.failure(new IllegalArgumentException("World and game rule are required"));
        if (!scheduler.isMainThread()) return Result.failure(new IllegalStateException("Unsafe cross-thread world access"));
        try { return Result.success(world.setGameRule(rule, value)); } catch (Throwable failure) { return Result.failure(failure); }
    }
}
