package dev.kehlchen.lib.core.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public final class StringUtils {
    private StringUtils() {}

    public static boolean isEmpty(String value) { return value == null || value.isEmpty(); }
    public static boolean isNotEmpty(String value) { return !isEmpty(value); }
    public static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    public static boolean isNotBlank(String value) { return !isBlank(value); }
    public static String defaultIfBlank(String value, String fallback) { return isBlank(value) ? fallback : value; }
    public static String defaultIfEmpty(String value, String fallback) { return isEmpty(value) ? fallback : value; }
    public static String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
    public static String capitalize(String value) {
        if (isEmpty(value)) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
    public static String uncapitalize(String value) {
        if (isEmpty(value)) return value;
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
    public static String truncate(String value, int maxLength) {
        if (value == null || maxLength < 0) return value;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
    public static String truncateEllipsis(String value, int maxLength) {
        if (value == null || maxLength < 0) return value;
        if (value.length() <= maxLength) return value;
        return maxLength == 0 ? "" : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
    public static boolean containsIgnoreCase(String value, String search) {
        return value != null && search != null && value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }
    public static String join(CharSequence delimiter, Iterable<?> values) {
        Objects.requireNonNull(delimiter, "delimiter");
        StringBuilder out = new StringBuilder();
        for (Object value : values) {
            if (!out.isEmpty()) out.append(delimiter);
            out.append(value);
        }
        return out.toString();
    }
    public static String join(CharSequence delimiter, Object... values) { return join(delimiter, Arrays.asList(values)); }
    public static String repeat(String value, int count) { return value == null ? "" : value.repeat(Math.max(0, count)); }
    public static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
