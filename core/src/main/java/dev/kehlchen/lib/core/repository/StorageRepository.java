package dev.kehlchen.lib.core.repository;

import dev.kehlchen.lib.api.repository.Repository;
import dev.kehlchen.lib.api.storage.Storage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/** Repository implementation over any key-value storage provider. */
public final class StorageRepository<ID, T> implements Repository<ID, T> {
    private final Storage<ID, T> storage;

    public StorageRepository(Storage<ID, T> storage) {
        this.storage = java.util.Objects.requireNonNull(storage, "storage");
    }

    @Override public CompletableFuture<Optional<T>> findById(ID id) { return storage.loadAsync(id); }

    @Override public CompletableFuture<List<T>> findAll() {
        return storage.keysAsync().thenCompose(keys -> {
            List<CompletableFuture<Optional<T>>> loads = keys.stream().map(storage::loadAsync).toList();
            CompletableFuture<?>[] futures = loads.toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures).thenApply(ignored ->
                    loads.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList());
        });
    }

    @Override public CompletableFuture<Boolean> existsById(ID id) {
        return storage.loadAsync(id).thenApply(Optional::isPresent);
    }
    @Override public CompletableFuture<Void> save(ID id, T entity) { return storage.saveAsync(id, entity); }
    @Override public CompletableFuture<Optional<T>> update(ID id, UnaryOperator<T> updater) {
        return storage.updateAsync(id, updater);
    }
    @Override public CompletableFuture<Void> saveAll(Map<ID, T> entities) { return storage.saveAllAsync(entities); }
    @Override public CompletableFuture<Boolean> deleteById(ID id) { return storage.deleteAsync(id); }
    @Override public CompletableFuture<Void> deleteAll(Collection<ID> ids) { return storage.deleteAllAsync(ids); }
    @Override public CompletableFuture<Long> count() {
        return storage.keysAsync().thenApply(keys -> (long) keys.size());
    }
}
