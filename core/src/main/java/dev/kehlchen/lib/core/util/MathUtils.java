package dev.kehlchen.lib.core.util;

import java.util.concurrent.ThreadLocalRandom;

public final class MathUtils {
    private MathUtils() {}
    public static double lerp(double start, double end, double progress) { return start + (end - start) * progress; }
    public static double inverseLerp(double start, double end, double value) { return (value - start) / (end - start); }
    public static double percent(double value, double total) { return total == 0 ? 0 : value / total * 100; }
    public static int randomInt(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) throw new IllegalArgumentException("min greater than max");
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }
    public static boolean approximately(double a, double b, double epsilon) { return Math.abs(a - b) <= epsilon; }
    public static int floorDiv(int a, int b) { return Math.floorDiv(a, b); }
}
