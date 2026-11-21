package dev.kehlchen.lib.core.config;

import dev.kehlchen.lib.core.util.FileUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class YamlConfig extends AbstractConfig {
    public YamlConfig(Path file) { super(file); }
    private Yaml yamlLoad() { return new Yaml(); }
    private Yaml yamlSave() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new Yaml(options);
    }
    @Override public void reload() {
        if (!Files.exists(file)) { data = new LinkedHashMap<>(defaults); save(); return; }
        try {
            Object loaded = yamlLoad().load(Files.newInputStream(file));
            data = loaded instanceof Map<?,?> map ? new LinkedHashMap<>((Map<String,Object>)map) : new LinkedHashMap<>();
        } catch (Exception e) { throw new IllegalStateException("Could not load YAML config: " + file, e); }
        defaults.forEach(this::flatSetIfMissing);
    }
    @Override public void save() {
        try { FileUtils.ensureDirectory(file.getParent()); Files.writeString(file, yamlSave().dumpAsMap(data)); }
        catch (Exception e) { throw new IllegalStateException("Could not save YAML config: " + file, e); }
    }
}
