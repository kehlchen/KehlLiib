package dev.kehlchen.lib.core.cache;

import dev.kehlchen.lib.api.cache.Cache;
import dev.kehlchen.lib.api.cache.CacheConfig;
import dev.kehlchen.lib.api.cache.CacheFactory;

public final class CacheFactoryImpl implements CacheFactory {
    @Override public <K, V> Cache<K, V> create(CacheConfig config) {
        return new ConcurrentCache<>(config);
    }
}
