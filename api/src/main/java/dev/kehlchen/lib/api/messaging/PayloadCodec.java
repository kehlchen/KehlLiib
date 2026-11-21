package dev.kehlchen.lib.api.messaging;

public interface PayloadCodec<T> {
    byte[] encode(T value) throws Exception;
    T decode(byte[] payload) throws Exception;
}
