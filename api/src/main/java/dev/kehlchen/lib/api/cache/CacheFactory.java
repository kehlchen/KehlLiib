package dev.kehlchen.lib.api.cache;

@FunctionalInterface
public interface CacheFactory {
    <K, V> Cache<K, V> create(CacheConfig config);
}
