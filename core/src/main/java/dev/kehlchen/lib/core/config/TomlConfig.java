package dev.kehlchen.lib.core.config;

import dev.kehlchen.lib.core.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TomlConfig extends AbstractConfig {
    public TomlConfig(Path file) { super(file); }
    @Override public void reload() {
        if (!Files.exists(file)) { data = new LinkedHashMap<>(defaults); save(); return; }
        data = new LinkedHashMap<>();
        Map<String,Object> table = data;
        try {
            for (String raw : Files.readAllLines(file)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    table = data;
                    for (String part : line.substring(1, line.length()-1).split("\\.")) {
                        Object child = table.get(part);
                        if (!(child instanceof Map)) { child = new LinkedHashMap<String,Object>(); table.put(part, child); }
                        table = (Map<String,Object>)child;
                    }
                } else if (line.contains("=")) {
                    int index = line.indexOf('=');
                    table.put(line.substring(0,index).trim(), parse(line.substring(index+1).trim()));
                }
            }
        } catch (Exception e) { throw new IllegalStateException("Could not load TOML config: " + file, e); }
        defaults.forEach(this::flatSetIfMissing);
    }
    @Override public void save() {
        StringBuilder out = new StringBuilder();
        writeTable("", data, out);
        try { FileUtils.ensureDirectory(file.getParent()); Files.writeString(file, out.toString()); }
        catch (Exception e) { throw new IllegalStateException("Could not save TOML config: " + file, e); }
    }
    private Object parse(String value) {
        if (value.equals("true") || value.equals("false")) return Boolean.valueOf(value);
        if (value.startsWith("\"") && value.endsWith("\"")) return value.substring(1,value.length()-1);
        try { return value.contains(".") ? Double.valueOf(value) : Long.valueOf(value); } catch (NumberFormatException e) { return value; }
    }
    private void writeTable(String prefix, Map<String,Object> table, StringBuilder out) {
        for (Map.Entry<String,Object> entry : table.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?,?> child) {
                String name = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                out.append('\n').append('[').append(name).append("]\n");
                writeTable(name, (Map<String,Object>)child, out);
            } else if (value instanceof String text) out.append(entry.getKey()).append(" = \"").append(text).append("\"\n");
            else out.append(entry.getKey()).append(" = ").append(value).append('\n');
        }
    }
}
