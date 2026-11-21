package dev.kehlchen.lib.core.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationUtils {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)\\s*(ms|s|m|h|d)");
    private DurationUtils() {}
    public static Duration parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Duration is blank");
        Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT).replace(" ", ""));
        long millis = 0;
        int matched = 0;
        while (matcher.find()) {
            matched += matcher.group().length();
            long amount = Long.parseLong(matcher.group(1));
            millis += switch (matcher.group(2)) {
                case "ms" -> amount;
                case "s" -> amount * 1000;
                case "m" -> amount * 60_000;
                case "h" -> amount * 3_600_000;
                case "d" -> amount * 86_400_000;
                default -> throw new IllegalArgumentException("Unknown duration unit");
            };
        }
        if (matched == 0 || matched != text.replace(" ", "").length()) throw new IllegalArgumentException("Invalid duration: " + text);
        return Duration.ofMillis(millis);
    }
    public static String format(Duration duration) {
        long millis = duration.toMillis();
        if (millis >= 86_400_000) return (millis / 86_400_000) + "d";
        if (millis >= 3_600_000) return (millis / 3_600_000) + "h";
        if (millis >= 60_000) return (millis / 60_000) + "m";
        if (millis >= 1000) return (millis / 1000) + "s";
        return millis + "ms";
    }
}
