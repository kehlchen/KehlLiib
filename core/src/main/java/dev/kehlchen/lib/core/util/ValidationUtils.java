package dev.kehlchen.lib.core.util;

import java.util.Collection;

public final class ValidationUtils {
    private ValidationUtils() {}
    public static boolean isValidEmail(String value) {
        return value != null && value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
    public static boolean inRange(int value, int min, int max) { return value >= min && value <= max; }
    public static boolean inRange(double value, double min, double max) { return value >= min && value <= max; }
    public static boolean notEmpty(Collection<?> value) { return value != null && !value.isEmpty(); }
}
