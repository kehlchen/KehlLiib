package dev.kehlchen.lib.core.util;

import org.bukkit.Color;

public final class ColorUtils {
    private ColorUtils() {}
    public static Color rgb(int red, int green, int blue) {
        return Color.fromRGB(NumberUtils.clamp(red, 0, 255),
                NumberUtils.clamp(green, 0, 255), NumberUtils.clamp(blue, 0, 255));
    }
    public static Color fromHex(String hex) {
        String value = hex == null ? "" : hex.trim().replace("#", "");
        if (value.length() != 6) throw new IllegalArgumentException("Hex color must contain six digits");
        return rgb(Integer.parseInt(value.substring(0, 2), 16),
                Integer.parseInt(value.substring(2, 4), 16),
                Integer.parseInt(value.substring(4, 6), 16));
    }
    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
    public static Color blend(Color a, Color b, double ratio) {
        ratio = NumberUtils.clamp(ratio, 0, 1);
        return rgb((int) (a.getRed() + (b.getRed() - a.getRed()) * ratio),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * ratio),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * ratio));
    }
}
