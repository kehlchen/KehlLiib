package dev.kehlchen.lib.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public final class UUIDUtils {
    private UUIDUtils() {}
    public static UUID random() { return UUID.randomUUID(); }
    public static UUID parse(String value) { return UUID.fromString(value); }
    public static UUID parseOrNull(String value) {
        if (value == null) return null;
        try { return UUID.fromString(value.trim()); } catch (IllegalArgumentException e) { return null; }
    }
    public static boolean isValid(String value) { return parseOrNull(value) != null; }
    public static UUID v5(String namespace, String name) {
        UUID ns = parse(namespace);
        return newUUIDv5(ns, name);
    }
    public static UUID namespaced(UUID namespace, String name) { return newUUIDv5(namespace, name); }
    private static UUID newUUIDv5(UUID namespace, String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(toBytes(namespace));
            md.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest();
            hash[6] &= 0x0f; hash[6] |= 0x50;
            hash[8] &= 0x3f; hash[8] |= 0x80;
            long msb = 0, lsb = 0;
            for (int i = 0; i < 8; i++) msb = (msb << 8) | (hash[i] & 0xff);
            for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (hash[i] & 0xff);
            return new UUID(msb, lsb);
        } catch (Exception e) { throw new IllegalStateException("SHA-1 unavailable", e); }
    }
    private static byte[] toBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits(), lsb = uuid.getLeastSignificantBits();
        byte[] b = new byte[16];
        for (int i = 0; i < 8; i++) b[i] = (byte) (msb >>> (8 * (7 - i)));
        for (int i = 8; i < 16; i++) b[i] = (byte) (lsb >>> (8 * (15 - i)));
        return b;
    }
}
