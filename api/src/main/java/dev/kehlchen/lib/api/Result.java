package dev.kehlchen.lib.api;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents an operation that completed successfully or failed with an associated error.
 * A result does not throw for its documented failure condition; callers inspect
 * {@link #isSuccess()}, {@link #value()}, or {@link #error()}.
 *
 * @param <T> success value type
 */
public final class Result<T> {
    private final T value;
    private final Throwable error;

    private Result(T value, Throwable error) {
        this.value = value;
        this.error = error;
    }

    /** Creates a successful result. The value may be {@code null}. */
    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    /** Creates a failed result.
     * @param error the failure; must not be {@code null}
     */
    public static <T> Result<T> failure(Throwable error) {
        return new Result<>(null, Objects.requireNonNull(error, "error"));
    }

    /** Returns whether this result has no failure. */
    public boolean isSuccess() {
        return error == null;
    }

    /** Returns the success value, or empty for failure or a null success value. */
    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    /** Returns the failure, or empty for success. */
    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Applies a mapper to a successful value and propagates failures unchanged.
     * Exceptions from the mapper are converted into failed results.
     */
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
