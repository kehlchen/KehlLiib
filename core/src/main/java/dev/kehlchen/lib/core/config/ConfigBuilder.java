package dev.kehlchen.lib.core.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfigBuilder {
    private Path path;
    private ConfigFormat format = ConfigFormat.YAML;
    private final Map<String,Object> defaults = new LinkedHashMap<>();
    private int version;

    public ConfigBuilder path(Path path) { this.path = path; return this; }
    public ConfigBuilder file(Path path) { return path(path); }
    public ConfigBuilder format(ConfigFormat format) { this.format = format; return this; }
    public ConfigBuilder defaultValue(String path, Object value) { defaults.put(path, value); return this; }
    public ConfigBuilder version(int version) { this.version = version; return this; }
    public Config build() {
        Objects.requireNonNull(path, "path");
        AbstractConfig config = switch (format) {
            case YAML -> new YamlConfig(path);
            case JSON -> new JsonConfig(path);
            case TOML -> new TomlConfig(path);
            case PROPERTIES -> new PropertiesConfig(path);
        };
        config.defaults(defaults);
        config.version = version;
        config.reload();
        config.defaults(defaults);
        return config;
    }
}
