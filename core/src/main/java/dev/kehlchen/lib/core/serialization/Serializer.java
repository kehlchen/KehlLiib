package dev.kehlchen.lib.core.serialization;

public interface Serializer<T> {
    String serialize(T value);
    T deserialize(String data);
}
