package dev.kehlchen.lib.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigSection {
    private final Config config;
    private final String prefix;

    public ConfigSection(Config config, String prefix) {
        this.config = config;
        this.prefix = prefix == null ? "" : prefix;
    }
    public String getString(String key, String fallback) { return config.getString(join(key), fallback); }
    public int getInt(String key, int fallback) { return config.getInt(join(key), fallback); }
    public boolean getBoolean(String key, boolean fallback) { return config.getBoolean(join(key), fallback); }
    public void set(String key, Object value) { config.set(join(key), value); }
    public ConfigSection section(String key) { return new ConfigSection(config, join(key)); }
    public Object get(String key) { return config.get(join(key)); }
    public boolean contains(String key) { return config.contains(join(key)); }
    private String join(String key) { return prefix.isEmpty() ? key : prefix + "." + key; }
}
