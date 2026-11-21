package dev.kehlchen.lib.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtils {
    private static final Pattern VERSION = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");
    private VersionUtils() {}
    public static int[] parse(String version) {
        Matcher matcher = VERSION.matcher(version == null ? "" : version);
        if (!matcher.find()) throw new IllegalArgumentException("Invalid version: " + version);
        return new int[]{group(matcher, 1), group(matcher, 2), group(matcher, 3)};
    }
    public static int compare(String a, String b) {
        int[] left = parse(a), right = parse(b);
        for (int i = 0; i < 3; i++) {
            int result = Integer.compare(left[i], right[i]);
            if (result != 0) return result;
        }
        return 0;
    }
    public static boolean isNewerThan(String version, String other) { return compare(version, other) > 0; }
    private static int group(Matcher matcher, int index) {
        String value = matcher.group(index);
        return value == null ? 0 : Integer.parseInt(value);
    }
}
