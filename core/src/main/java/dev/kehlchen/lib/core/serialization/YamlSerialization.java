package dev.kehlchen.lib.core.serialization;

import org.yaml.snakeyaml.Yaml;

public final class YamlSerialization {
    private final Yaml yaml = new Yaml();
    public String toYaml(Object value) { return yaml.dump(value); }
    public <T> T fromYaml(String data, Class<T> type) {
        try { return yaml.loadAs(data, type); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid YAML", e); }
    }
}
