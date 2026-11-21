package dev.kehlchen.lib.core.storage;

import dev.kehlchen.lib.api.storage.Storage;
import dev.kehlchen.lib.api.storage.StorageFactory;
import dev.kehlchen.lib.api.storage.StorageProvider;

public final class StorageFactoryImpl implements StorageFactory {
    @Override public <K, V> Storage<K, V> create(Class<V> valueType, StorageProvider<K, V> provider) {
        java.util.Objects.requireNonNull(valueType, "valueType");
        return new DefaultStorage<>(provider);
    }
}
