package dev.kehlchen.lib.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class CollectionUtils {
    private CollectionUtils() {}
    public static boolean isEmpty(Collection<?> value) { return value == null || value.isEmpty(); }
    public static boolean isEmpty(Map<?, ?> value) { return value == null || value.isEmpty(); }
    public static <T> T first(Collection<T> values) { return isEmpty(values) ? null : values.iterator().next(); }
    public static <T> T firstOrDefault(Collection<T> values, T fallback) { T first = first(values); return first == null ? fallback : first; }
    public static <T> List<T> immutableCopy(Collection<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
    public static <T> List<T> filtered(Collection<T> values, Predicate<? super T> predicate) {
        if (values == null) return List.of();
        Objects.requireNonNull(predicate, "predicate");
        List<T> result = new ArrayList<>();
        for (T value : values) if (predicate.test(value)) result.add(value);
        return result;
    }
    public static <T> T random(List<T> values) {
        if (isEmpty(values)) return null;
        return values.get((int) (Math.random() * values.size()));
    }
    public static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
        if (values == null) return Map.of();
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
