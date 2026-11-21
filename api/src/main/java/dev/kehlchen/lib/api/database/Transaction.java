package dev.kehlchen.lib.api.database;

import java.util.List;
import java.util.Optional;

public interface Transaction {
    int update(String sql, Object... parameters);
    long insert(String sql, Object... parameters);
    <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters);
    default <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... parameters) {
        List<T> values = query(sql, mapper, parameters);
        return values.stream().findFirst();
    }
}
