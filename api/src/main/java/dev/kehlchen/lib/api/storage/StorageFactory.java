package dev.kehlchen.lib.api.storage;

@FunctionalInterface
public interface StorageFactory {
    <K, V> Storage<K, V> create(Class<V> valueType, StorageProvider<K, V> provider);
}
