package dev.kehlchen.lib.api.database;

import dev.kehlchen.lib.api.Resource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Database extends Resource {
    int update(String sql, Object... parameters);
    long insert(String sql, Object... parameters);
    <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters);
    default <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... parameters) {
        return query(sql, mapper, parameters).stream().findFirst();
    }
    int[] batch(String sql, List<Object[]> parameterSets);
    <T> T transaction(TransactionWork<T> work);
    CompletableFuture<Integer> updateAsync(String sql, Object... parameters);
    CompletableFuture<Long> insertAsync(String sql, Object... parameters);
    <T> CompletableFuture<List<T>> queryAsync(String sql, RowMapper<T> mapper, Object... parameters);
    CompletableFuture<int[]> batchAsync(String sql, List<Object[]> parameterSets);
    <T> CompletableFuture<T> transactionAsync(TransactionWork<T> work);
    boolean isHealthy();
    DatabaseStats statistics();
}
