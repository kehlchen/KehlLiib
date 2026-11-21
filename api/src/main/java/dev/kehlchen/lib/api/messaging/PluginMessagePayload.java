package dev.kehlchen.lib.api.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Immutable, size-bounded plugin-message bytes. Bukkit's plugin messaging limit is 32,767 bytes. */
public final class PluginMessagePayload {
    public static final int MAX_BYTES = 32_766;
    private final byte[] bytes;

    private PluginMessagePayload(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    public static PluginMessagePayload of(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Plugin message exceeds " + MAX_BYTES + " bytes");
        return new PluginMessagePayload(bytes);
    }

    public static PluginMessagePayload utf8(String value) {
        return of(Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8));
    }

    public static <T> PluginMessagePayload encode(T value, PayloadCodec<T> codec) throws Exception {
        return of(Objects.requireNonNull(codec, "codec").encode(value));
    }

    public <T> T decode(PayloadCodec<T> codec) throws Exception {
        return Objects.requireNonNull(codec, "codec").decode(bytes());
    }

    public byte[] bytes() { return bytes.clone(); }
    public int size() { return bytes.length; }
    public String utf8() {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException failure) {
            throw new IllegalArgumentException("Plugin message is not valid UTF-8", failure);
        }
    }
}
