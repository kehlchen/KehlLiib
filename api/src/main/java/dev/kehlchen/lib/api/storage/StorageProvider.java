package dev.kehlchen.lib.api.storage;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface StorageProvider<K, V> extends AutoCloseable {
    void save(K key, V value);
    Optional<V> load(K key);
    boolean delete(K key);
    boolean exists(K key);
    Set<K> keys();
    default void saveAll(Map<K, V> values) { values.forEach(this::save); }
    default void deleteAll(Collection<K> keys) { keys.forEach(this::delete); }
    @Override default void close() {}
}
