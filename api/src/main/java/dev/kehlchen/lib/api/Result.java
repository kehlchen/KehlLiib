package dev.kehlchen.lib.api;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** An explicit success or failure result. */
public final class Result<T> {
    private final T value;
    private final Throwable error;

    private Result(T value, Throwable error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> failure(Throwable error) {
        return new Result<>(null, Objects.requireNonNull(error, "error"));
    }

    public boolean isSuccess() {
        return error == null;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }

    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (error != null) {
            return Result.failure(error);
        }
        try {
            return Result.success(mapper.apply(value));
        } catch (Throwable failure) {
            return Result.failure(failure);
        }
    }
}
