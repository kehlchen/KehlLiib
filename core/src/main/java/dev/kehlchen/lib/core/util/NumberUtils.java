package dev.kehlchen.lib.core.util;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class NumberUtils {
    private NumberUtils() {}
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public static long clamp(long value, long min, long max) { return Math.max(min, Math.min(max, value)); }
    public static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    public static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    public static OptionalInt parseInt(String value) {
        try { return OptionalInt.of(Integer.parseInt(trim(value))); } catch (RuntimeException e) { return OptionalInt.empty(); }
    }
    public static OptionalDouble parseDouble(String value) {
        try { return OptionalDouble.of(Double.parseDouble(trim(value))); } catch (RuntimeException e) { return OptionalDouble.empty(); }
    }
    public static int toInt(String value, int fallback) { return parseInt(value).orElse(fallback); }
    public static double toDouble(String value, double fallback) { return parseDouble(value).orElse(fallback); }
    public static boolean isInteger(String value) { return parseInt(value).isPresent(); }
    public static boolean isFinite(Number value) {
        return value != null && (!(value instanceof Double d) || Double.isFinite(d))
                && (!(value instanceof Float f) || Float.isFinite(f));
    }
    private static String trim(String value) { return value == null ? "" : value.trim(); }
}
