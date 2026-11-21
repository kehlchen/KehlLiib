package dev.kehlchen.lib.api.storage;

import dev.kehlchen.lib.api.Resource;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface Storage<K, V> extends Resource {
    void save(K key, V value);
    Optional<V> load(K key);
    default V load(K key, V defaultValue) { return load(key).orElse(defaultValue); }
    boolean delete(K key);
    boolean exists(K key);
    Set<K> keys();
    Optional<V> update(K key, UnaryOperator<V> updater);
    Map<K, V> loadAll(Collection<K> keys);
    void saveAll(Map<K, V> values);
    void deleteAll(Collection<K> keys);
    CompletableFuture<Void> saveAsync(K key, V value);
    CompletableFuture<Optional<V>> loadAsync(K key);
    CompletableFuture<Boolean> deleteAsync(K key);
    CompletableFuture<Set<K>> keysAsync();
    CompletableFuture<Optional<V>> updateAsync(K key, UnaryOperator<V> updater);
    CompletableFuture<Map<K, V>> loadAllAsync(Collection<K> keys);
    CompletableFuture<Void> saveAllAsync(Map<K, V> values);
    CompletableFuture<Void> deleteAllAsync(Collection<K> keys);
    @Override void close();
}
