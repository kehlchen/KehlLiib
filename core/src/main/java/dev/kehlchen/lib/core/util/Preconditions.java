package dev.kehlchen.lib.core.util;

import java.util.Objects;

public final class Preconditions {
    private Preconditions() {}
    public static void checkArgument(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
    public static void checkState(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    public static <T> T checkNotNull(T value, String name) { return Objects.requireNonNull(value, name); }
    public static String checkNotBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
    public static int checkRange(int value, int min, int max, String name) {
        if (value < min || value > max) throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        return value;
    }
}
