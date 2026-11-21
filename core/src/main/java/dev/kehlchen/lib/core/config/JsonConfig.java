package dev.kehlchen.lib.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.kehlchen.lib.core.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonConfig extends AbstractConfig {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public JsonConfig(Path file) { super(file); }
    @Override public void reload() {
        if (!Files.exists(file)) { data = new LinkedHashMap<>(defaults); save(); return; }
        try {
            Map<String,Object> loaded = gson.fromJson(Files.readString(file), new TypeToken<Map<String,Object>>(){}.getType());
            data = loaded == null ? new LinkedHashMap<>() : new LinkedHashMap<>(loaded);
        } catch (Exception e) { throw new IllegalStateException("Could not load JSON config: " + file, e); }
        defaults.forEach(this::flatSetIfMissing);
    }
    @Override public void save() {
        try { FileUtils.ensureDirectory(file.getParent()); Files.writeString(file, gson.toJson(data)); }
        catch (Exception e) { throw new IllegalStateException("Could not save JSON config: " + file, e); }
    }
}
