package dev.kehlchen.lib.api.cache;

import dev.kehlchen.lib.api.Resource;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface Cache<K, V> extends Resource {
    Optional<V> get(K key);
    void put(K key, V value);
    Optional<V> remove(K key);
    boolean contains(K key);
    void clear();
    void invalidate(K key);
    void invalidateAll(Collection<K> keys);
    V getOrLoad(K key, Function<? super K, ? extends V> loader);
    CompletableFuture<V> getOrLoadAsync(K key, Function<? super K, ? extends CompletableFuture<V>> loader);
    CacheStats statistics();
    @Override default void close() { clear(); }
}
