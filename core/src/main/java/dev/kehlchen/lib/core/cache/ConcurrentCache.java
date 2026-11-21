package dev.kehlchen.lib.core.cache;

import dev.kehlchen.lib.api.cache.Cache;
import dev.kehlchen.lib.api.cache.CacheConfig;
import dev.kehlchen.lib.api.cache.CacheStats;
import dev.kehlchen.lib.api.cache.EvictionPolicy;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** Thread-safe bounded cache with TTL expiration and coalesced asynchronous loads. */
public final class ConcurrentCache<K, V> implements Cache<K, V> {
    private final CacheConfig config;
    private final long ttlNanos;
    private final Map<K, Entry<V>> values;
    private final ConcurrentHashMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private volatile boolean closed;

    public ConcurrentCache(CacheConfig config) {
        this.config = java.util.Objects.requireNonNull(config, "config");
        Duration ttl = config.timeToLive();
        long ttlValue;
        try {
            ttlValue = ttl.isZero() ? 0 : ttl.toNanos();
        } catch (ArithmeticException overflow) {
            ttlValue = Long.MAX_VALUE;
        }
        ttlNanos = ttlValue;
        boolean accessOrder = config.evictionPolicy() == EvictionPolicy.LRU;
        values = new LinkedHashMap<>(16, 0.75f, accessOrder);
    }

    @Override public synchronized Optional<V> get(K key) {
        ensureOpen();
        Entry<V> entry = values.get(java.util.Objects.requireNonNull(key, "key"));
        if (expired(entry)) {
            values.remove(key);
            evictions.incrementAndGet();
            entry = null;
        }
        if (entry == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        hits.incrementAndGet();
        return Optional.of(entry.value);
    }

    @Override public synchronized void put(K key, V value) {
        ensureOpen();
        java.util.Objects.requireNonNull(key, "key");
        java.util.Objects.requireNonNull(value, "value");
        values.put(key, new Entry<>(value, expirationTime()));
        evictExpired();
        while (values.size() > config.maximumSize()) {
            K eldest = values.keySet().iterator().next();
            values.remove(eldest);
            evictions.incrementAndGet();
        }
    }

    @Override public synchronized Optional<V> remove(K key) {
        ensureOpen();
        CompletableFuture<V> loadingValue = inFlight.remove(key);
        if (loadingValue != null) loadingValue.cancel(false);
        Entry<V> removed = values.remove(key);
        return removed == null || expired(removed) ? Optional.empty() : Optional.of(removed.value);
    }

    @Override public synchronized boolean contains(K key) {
        ensureOpen();
        java.util.Objects.requireNonNull(key, "key");
        Entry<V> entry = values.get(key);
        if (!expired(entry)) return entry != null;
        values.remove(key);
        evictions.incrementAndGet();
        return false;
    }

    @Override public synchronized void clear() {
        ensureOpen();
        values.clear();
        inFlight.values().forEach(future -> future.cancel(false));
        inFlight.clear();
    }

    @Override public void invalidate(K key) { remove(key); }
    @Override public void invalidateAll(Collection<K> keys) {
        synchronized (this) {
            ensureOpen();
            keys.forEach(key -> {
                values.remove(key);
                CompletableFuture<V> loadingValue = inFlight.remove(key);
                if (loadingValue != null) loadingValue.cancel(false);
            });
        }
    }

    @Override public V getOrLoad(K key, Function<? super K, ? extends V> loader) {
        try {
            return getOrLoadAsync(key, current -> CompletableFuture.completedFuture(loader.apply(current))).join();
        } catch (CompletionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw failure;
        }
    }

    @Override public CompletableFuture<V> getOrLoadAsync(
            K key, Function<? super K, ? extends CompletableFuture<V>> loader) {
        java.util.Objects.requireNonNull(key, "key");
        java.util.Objects.requireNonNull(loader, "loader");
        Optional<V> cached = get(key);
        if (cached.isPresent()) return CompletableFuture.completedFuture(cached.get());
        ensureOpen();

        CompletableFuture<V> candidate = new CompletableFuture<>();
        CompletableFuture<V> existing = inFlight.putIfAbsent(key, candidate);
        if (existing != null) return existing;
        try {
            CompletableFuture<V> loading = java.util.Objects.requireNonNull(loader.apply(key), "loader future");
            loading.whenComplete((value, failure) -> {
                try {
                    if (failure != null) candidate.completeExceptionally(failure);
                    else if (candidate.isCancelled()) return;
                    else {
                        candidate.complete(putIfAbsent(key, java.util.Objects.requireNonNull(value, "loaded value")));
                    }
                } catch (Throwable cacheFailure) {
                    candidate.completeExceptionally(cacheFailure);
                } finally {
                    inFlight.remove(key, candidate);
                }
            });
        } catch (Throwable failure) {
            inFlight.remove(key, candidate);
            candidate.completeExceptionally(failure);
        }
        return candidate;
    }

    @Override public synchronized CacheStats statistics() {
        evictExpired();
        return new CacheStats(hits.get(), misses.get(), evictions.get(), values.size());
    }

    @Override public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
            values.clear();
        }
        inFlight.values().forEach(future -> future.cancel(false));
        inFlight.clear();
    }

    private void evictExpired() {
        if (ttlNanos == 0) return;
        var iterator = values.entrySet().iterator();
        while (iterator.hasNext()) {
            if (expired(iterator.next().getValue())) {
                iterator.remove();
                evictions.incrementAndGet();
            }
        }
    }

    private synchronized V putIfAbsent(K key, V value) {
        ensureOpen();
        Entry<V> existing = values.get(key);
        if (!expired(existing)) {
            return existing == null ? putNew(key, value) : existing.value;
        }
        values.remove(key);
        evictions.incrementAndGet();
        return putNew(key, value);
    }

    private V putNew(K key, V value) {
        values.put(key, new Entry<>(value, expirationTime()));
        evictExpired();
        while (values.size() > config.maximumSize()) {
            K eldest = values.keySet().iterator().next();
            values.remove(eldest);
            evictions.incrementAndGet();
        }
        return value;
    }

    private boolean expired(Entry<V> entry) {
        return entry != null && ttlNanos != 0 && System.nanoTime() - entry.expiresAt >= 0;
    }

    private long expirationTime() {
        if (ttlNanos == 0) return Long.MAX_VALUE;
        try {
            return Math.addExact(System.nanoTime(), ttlNanos);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Cache is closed");
    }

    private record Entry<V>(V value, long expiresAt) {}
}
