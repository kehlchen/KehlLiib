package dev.kehlchen.lib.core.config;

import dev.kehlchen.lib.core.util.FileUtils;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Properties;

public final class PropertiesConfig extends AbstractConfig {
    public PropertiesConfig(Path file) { super(file); }
    @Override public void reload() {
        if (!Files.exists(file)) { data = new LinkedHashMap<>(defaults); save(); return; }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) { properties.load(reader); }
        catch (Exception e) { throw new IllegalStateException("Could not load properties: " + file, e); }
        data = new LinkedHashMap<>();
        properties.forEach((key, value) -> flatSet(String.valueOf(key), value));
        defaults.forEach(this::flatSetIfMissing);
    }
    @Override public void save() {
        Properties properties = new Properties();
        flatten("", data, properties);
        try { FileUtils.ensureDirectory(file.getParent()); try (Writer writer = Files.newBufferedWriter(file)) { properties.store(writer, "KehlLib configuration"); } }
        catch (Exception e) { throw new IllegalStateException("Could not save properties: " + file, e); }
    }
    private void flatten(String prefix, java.util.Map<String,Object> map, Properties out) {
        map.forEach((key, value) -> {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof java.util.Map<?,?> child) flatten(path, (java.util.Map<String,Object>)child, out);
            else out.setProperty(path, String.valueOf(value));
        });
    }
}
