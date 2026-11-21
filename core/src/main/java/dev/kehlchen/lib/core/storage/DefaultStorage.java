package dev.kehlchen.lib.core.storage;

import dev.kehlchen.lib.api.storage.Storage;
import dev.kehlchen.lib.api.storage.StorageProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class DefaultStorage<K, V> implements Storage<K, V> {
    private final StorageProvider<K, V> provider;
    private final ExecutorService executor;
    private final Set<CompletableFuture<?>> pendingAsync = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;
    private volatile boolean closing;

    public DefaultStorage(StorageProvider<K, V> provider) {
        this.provider = java.util.Objects.requireNonNull(provider, "provider");
        executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "kehllib-storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override public synchronized void save(K key, V value) { ensureOpen(); saveInternal(key, value); }
    @Override public synchronized Optional<V> load(K key) { ensureOpen(); return provider.load(key); }
    @Override public synchronized boolean delete(K key) { ensureOpen(); return provider.delete(key); }
    @Override public synchronized boolean exists(K key) { ensureOpen(); return provider.exists(key); }
    @Override public synchronized Set<K> keys() { ensureOpen(); return provider.keys(); }
    @Override public synchronized Optional<V> update(K key, UnaryOperator<V> updater) {
        ensureOpen();
        return updateInternal(key, updater);
    }
    @Override public synchronized Map<K, V> loadAll(Collection<K> keys) {
        ensureOpen();
        Map<K, V> result = new java.util.LinkedHashMap<>();
        for (K key : ListCopy.copy(keys)) provider.load(key).ifPresent(value -> result.put(key, value));
        return Map.copyOf(result);
    }
    @Override public synchronized void saveAll(Map<K, V> values) {
        ensureOpen();
        provider.saveAll(Map.copyOf(values));
    }
    @Override public synchronized void deleteAll(Collection<K> keys) { ensureOpen(); provider.deleteAll(ListCopy.copy(keys)); }
    @Override public CompletableFuture<Void> saveAsync(K key, V value) {
        ensureOpen();
        return submit(() -> { saveInternal(key, value); return null; });
    }
    @Override public CompletableFuture<Optional<V>> loadAsync(K key) {
        ensureOpen();
        return submit(() -> loadInternal(key));
    }
    @Override public CompletableFuture<Boolean> deleteAsync(K key) {
        ensureOpen();
        return submit(() -> deleteInternal(key));
    }
    @Override public CompletableFuture<Set<K>> keysAsync() {
        ensureOpen();
        return submit(this::keysInternal);
    }
    @Override public CompletableFuture<Optional<V>> updateAsync(K key, UnaryOperator<V> updater) {
        ensureOpen();
        return submit(() -> updateInternal(key, updater));
    }
    @Override public CompletableFuture<Map<K, V>> loadAllAsync(Collection<K> keys) {
        Collection<K> copy = ListCopy.copy(keys);
        ensureOpen();
        return submit(() -> loadAllInternal(copy));
    }
    @Override public CompletableFuture<Void> saveAllAsync(Map<K, V> values) {
        Map<K, V> copy = Map.copyOf(values);
        ensureOpen();
        return submit(() -> { saveAllInternal(copy); return null; });
    }
    @Override public CompletableFuture<Void> deleteAllAsync(Collection<K> keys) {
        Collection<K> copy = ListCopy.copy(keys);
        ensureOpen();
        return submit(() -> { deleteAllInternal(copy); return null; });
    }
    @Override public void close() {
        synchronized (this) {
            if (closed || closing) return;
            closing = true;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                failPendingAsync("Storage closed before async operation completed");
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            failPendingAsync("Storage shutdown was interrupted");
            Thread.currentThread().interrupt();
        } finally {
            synchronized (this) {
                closed = true;
                provider.close();
            }
        }
    }
    private void ensureOpen() {
        if (closed || closing) throw new IllegalStateException("Storage is closed or shutting down");
    }

    private synchronized void saveInternal(K key, V value) {
        provider.save(key, value);
    }

    private synchronized Optional<V> updateInternal(K key, UnaryOperator<V> updater) {
        Optional<V> existing = provider.load(key);
        if (existing.isEmpty()) return Optional.empty();
        V updated = java.util.Objects.requireNonNull(updater.apply(existing.get()), "updated value");
        saveInternal(key, updated);
        return Optional.of(updated);
    }

    private synchronized Optional<V> loadInternal(K key) { return provider.load(key); }
    private synchronized boolean deleteInternal(K key) { return provider.delete(key); }
    private synchronized Set<K> keysInternal() { return provider.keys(); }
    private synchronized void saveAllInternal(Map<K, V> values) { provider.saveAll(values); }
    private synchronized void deleteAllInternal(Collection<K> keys) { provider.deleteAll(keys); }

    private synchronized Map<K, V> loadAllInternal(Collection<K> keys) {
        Map<K, V> result = new java.util.LinkedHashMap<>();
        for (K key : keys) provider.load(key).ifPresent(value -> result.put(key, value));
        return Map.copyOf(result);
    }

    private <T> CompletableFuture<T> submit(Supplier<T> operation) {
        CompletableFuture<T> result = new CompletableFuture<>();
        pendingAsync.add(result);
        try {
            executor.execute(() -> {
                try {
                    result.complete(operation.get());
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                } finally {
                    pendingAsync.remove(result);
                }
            });
        } catch (RejectedExecutionException failure) {
            pendingAsync.remove(result);
            result.completeExceptionally(new IllegalStateException("Storage is shutting down", failure));
        }
        return result;
    }

    private void failPendingAsync(String message) {
        IllegalStateException failure = new IllegalStateException(message);
        pendingAsync.forEach(future -> future.completeExceptionally(failure));
        pendingAsync.clear();
    }

    private static final class ListCopy {
        private ListCopy() {}
        private static <T> Collection<T> copy(Collection<T> values) {
            return java.util.List.copyOf(values);
        }
    }
}
