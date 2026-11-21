package dev.kehlchen.lib.core.util;

import dev.kehlchen.lib.api.Result;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SafeExecution {
    private SafeExecution() {}
    public static <T> Result<T> of(Callable<? extends T> action) {
        Objects.requireNonNull(action, "action");
        try { return Result.success(action.call()); } catch (Throwable error) { return Result.failure(error); }
    }
    public static <T> Optional<T> ofOptional(Callable<? extends T> action) {
        try { return Optional.ofNullable(action.call()); } catch (Throwable error) { return Optional.empty(); }
    }
    public static Result<Void> run(Runnable action) {
        Objects.requireNonNull(action, "action");
        try { action.run(); return Result.success(null); } catch (Throwable error) { return Result.failure(error); }
    }
    public static <T> T orElse(Supplier<T> action, T fallback) {
        try { T value = action.get(); return value == null ? fallback : value; } catch (Throwable error) { return fallback; }
    }
    public static void swallow(Runnable action, Consumer<Throwable> handler) {
        try { action.run(); } catch (Throwable error) { if (handler != null) handler.accept(error); }
    }
}
