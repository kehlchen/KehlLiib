package dev.kehlchen.lib.core.util;

import org.bukkit.Location;
import org.bukkit.block.Block;

public final class BlockUtils {
    private BlockUtils() {}
    public static boolean isSolid(Block block) { return block != null && block.getType().isSolid(); }
    public static boolean isAir(Block block) { return block == null || block.getType().isAir(); }
    public static Location location(Block block) { return block == null ? null : block.getLocation().add(0.5, 0.5, 0.5); }
}
