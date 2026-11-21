package dev.kehlchen.lib.core.config;

import dev.kehlchen.lib.core.util.FileUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractConfig implements Config {
    protected static final Pattern ENV = Pattern.compile("\\$\\{([^}:]+)(:-([^}]*))?}");
    protected final Path file;
    protected LinkedHashMap<String, Object> data = new LinkedHashMap<>();
    protected final Map<String, String> comments = new LinkedHashMap<>();
    protected final Map<String, Object> defaults = new LinkedHashMap<>();
    protected int version;

    protected AbstractConfig(Path file) {
        this.file = file.toAbsolutePath();
        FileUtils.ensureDirectory(this.file.getParent());
    }

    @Override public Object get(String path) { return resolveEnv(flatGet(path)); }
    @Override public Object get(String path, Object fallback) { Object value = get(path); return value == null ? fallback : value; }
    @Override public String getString(String path) { Object value = get(path); return value == null ? null : String.valueOf(value); }
    @Override public String getString(String path, String fallback) { String value = getString(path); return value == null ? fallback : value; }
    @Override public int getInt(String path, int fallback) { try { return (int) Double.parseDouble(getString(path)); } catch (Exception e) { return fallback; } }
    @Override public long getLong(String path, long fallback) { try { return (long) Double.parseDouble(getString(path)); } catch (Exception e) { return fallback; } }
    @Override public double getDouble(String path, double fallback) { try { return Double.parseDouble(getString(path)); } catch (Exception e) { return fallback; } }
    @Override public boolean getBoolean(String path, boolean fallback) { try { return Boolean.parseBoolean(getString(path)); } catch (Exception e) { return fallback; } }
    @Override public <T> T get(String path, Class<T> type) { return get(path, type, null); }
    @Override public <T> T get(String path, Class<T> type, T fallback) {
        Object value = get(path);
        if (value == null) return fallback;
        if (type.isInstance(value)) return type.cast(value);
        try {
            if (type == String.class) return type.cast(String.valueOf(value));
            if (type == Integer.class) return type.cast(Integer.valueOf(String.valueOf(value)));
            if (type == Long.class) return type.cast(Long.valueOf(String.valueOf(value)));
            if (type == Double.class) return type.cast(Double.valueOf(String.valueOf(value)));
            if (type == Boolean.class) return type.cast(Boolean.valueOf(String.valueOf(value)));
        } catch (RuntimeException ignored) {}
        return fallback;
    }
    @Override public <T> List<T> getList(String path, Class<T> type) {
        Object value = get(path);
        if (!(value instanceof List<?> list)) return List.of();
        List<T> result = new ArrayList<>();
        for (Object item : list) if (type.isInstance(item)) result.add(type.cast(item));
        return result;
    }
    @Override public <K,V> Map<K,V> getMap(String path, Class<K> keyType, Class<V> valueType) {
        Object value = get(path);
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<K,V> result = new LinkedHashMap<>();
        for (Map.Entry<?,?> entry : map.entrySet()) {
            if (keyType.isInstance(entry.getKey()) && valueType.isInstance(entry.getValue()))
                result.put(keyType.cast(entry.getKey()), valueType.cast(entry.getValue()));
        }
        return result;
    }
    @Override public ConfigSection section(String path) { return new ConfigSection(this, path); }
    @Override public void set(String path, Object value) { flatSet(path, value); }
    @Override public boolean contains(String path) { return flatGet(path) != null; }
    @Override public Path path() { return file; }

    protected Object flatGet(String path) {
        String[] parts = path == null ? new String[0] : path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(part)) return null;
            current = map.get(part);
        }
        return current;
    }
    protected void flatSet(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) { next = new LinkedHashMap<String,Object>(); current.put(parts[i], next); }
            current = (Map<String,Object>) next;
        }
        current.put(parts[parts.length - 1], value);
    }
    protected Object resolveEnv(Object value) {
        if (!(value instanceof String text)) return value;
        Matcher matcher = ENV.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String fallback = matcher.group(3);
            String replacement = System.getenv(matcher.group(1));
            if (replacement == null) replacement = fallback == null ? "" : fallback;
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
    public AbstractConfig defaults(Map<String,Object> defaults) { this.defaults.clear(); this.defaults.putAll(defaults); defaults.forEach(this::flatSetIfMissing); return this; }
    protected void flatSetIfMissing(String path, Object value) { if (!contains(path)) flatSet(path, value); }
    public int version() { return version; }
}
