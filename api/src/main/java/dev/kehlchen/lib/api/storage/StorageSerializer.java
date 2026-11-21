package dev.kehlchen.lib.api.storage;

public interface StorageSerializer<T> {
    String serialize(T value);
    T deserialize(String value);
}
