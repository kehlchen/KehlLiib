package dev.kehlchen.lib.api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.concurrent.CompletableFuture;

public interface Repository<ID, T> {
    CompletableFuture<Optional<T>> findById(ID id);
    CompletableFuture<List<T>> findAll();
    CompletableFuture<Boolean> existsById(ID id);
    CompletableFuture<Void> save(ID id, T entity);
    default CompletableFuture<Optional<T>> update(ID id, UnaryOperator<T> updater) {
        return findById(id).thenCompose(existing -> {
            if (existing.isEmpty()) return CompletableFuture.completedFuture(Optional.empty());
            T updated = java.util.Objects.requireNonNull(updater.apply(existing.get()), "updated entity");
            return save(id, updated).thenApply(ignored -> Optional.of(updated));
        });
    }
    CompletableFuture<Void> saveAll(java.util.Map<ID, T> entities);
    CompletableFuture<Boolean> deleteById(ID id);
    CompletableFuture<Void> deleteAll(Collection<ID> ids);
    CompletableFuture<Long> count();

    default CompletableFuture<Page<T>> findAll(PageRequest request) {
        return findAll(value -> true, null, request);
    }

    default CompletableFuture<Page<T>> findAll(Predicate<? super T> filter,
                                                Comparator<? super T> comparator,
                                                PageRequest request) {
        java.util.Objects.requireNonNull(filter, "filter");
        java.util.Objects.requireNonNull(request, "request");
        return findAll().thenApply(values -> {
            java.util.stream.Stream<T> stream = values.stream().filter(filter);
            if (comparator != null) {
                Comparator<? super T> ordered = request.order() == SortOrder.DESCENDING
                        ? comparator.reversed() : comparator;
                stream = stream.sorted(ordered);
            }
            List<T> selected = stream.toList();
            long start = (long) request.page() * request.size();
            List<T> content = start >= selected.size() ? List.of()
                    : selected.subList((int) start, Math.min(selected.size(), (int) (start + request.size())));
            return new Page<>(content, request.page(), request.size(), selected.size());
        });
    }
}
