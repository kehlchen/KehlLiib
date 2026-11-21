package dev.kehlchen.lib.core.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface Config {
    Object get(String path);
    Object get(String path, Object fallback);
    String getString(String path);
    String getString(String path, String fallback);
    int getInt(String path, int fallback);
    long getLong(String path, long fallback);
    double getDouble(String path, double fallback);
    boolean getBoolean(String path, boolean fallback);
    <T> T get(String path, Class<T> type);
    <T> T get(String path, Class<T> type, T fallback);
    <T> List<T> getList(String path, Class<T> type);
    <K, V> Map<K, V> getMap(String path, Class<K> keyType, Class<V> valueType);
    ConfigSection section(String path);
    void set(String path, Object value);
    boolean contains(String path);
    void reload();
    void save();
    Path path();
}
