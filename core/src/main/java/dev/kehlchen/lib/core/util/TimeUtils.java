package dev.kehlchen.lib.core.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {
    private TimeUtils() {}
    public static long currentMillis() { return System.currentTimeMillis(); }
    public static long unixTimeSeconds() { return Instant.now().getEpochSecond(); }
    public static String format(long millis, DateTimeFormatter formatter) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(formatter);
    }
    public static String formatIso(long millis) { return Instant.ofEpochMilli(millis).toString(); }
    public static long elapsedMillis(long startMillis) { return System.currentTimeMillis() - startMillis; }
    public static boolean hasElapsed(long startMillis, long durationMillis) { return elapsedMillis(startMillis) >= durationMillis; }
}
