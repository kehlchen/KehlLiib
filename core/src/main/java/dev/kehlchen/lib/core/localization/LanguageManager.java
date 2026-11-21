package dev.kehlchen.lib.core.localization;

import dev.kehlchen.lib.core.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LanguageManager {
    private final Path directory;
    private final String defaultLanguage;
    private volatile String language;
    private final Map<String, Map<String,String>> bundles = new ConcurrentHashMap<>();

    public LanguageManager(Path directory, String defaultLanguage) {
        this.directory = directory.toAbsolutePath();
        this.defaultLanguage = defaultLanguage == null ? "en" : defaultLanguage;
        this.language = this.defaultLanguage;
        FileUtils.ensureDirectory(directory);
        reload();
    }
    public void setLanguage(String language) {
        this.language = language == null || language.isBlank() ? defaultLanguage : language;
    }
    public String get(String key) { return get(language, key); }
    public String get(String language, String key) {
        String value = bundles.getOrDefault(language, Map.of()).get(key);
        if (value == null && !language.equals(defaultLanguage)) value = bundles.getOrDefault(defaultLanguage, Map.of()).get(key);
        return value == null ? key : value;
    }
    public String get(String key, Object... arguments) { return format(get(key), arguments); }
    public String format(String template, Object... arguments) {
        if (arguments == null || arguments.length == 0) return template;
        try { return MessageFormat.format(template, arguments); }
        catch (RuntimeException e) { return template; }
    }
    public void reload() {
        bundles.clear();
        try (java.util.stream.Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".properties")).forEach(this::loadFile);
        } catch (Exception e) { throw new IllegalStateException("Could not reload languages", e); }
    }
    private void loadFile(Path file) {
        String name = file.getFileName().toString();
        String language = name.substring(0, name.length() - ".properties".length()).toLowerCase(Locale.ROOT);
        java.util.Properties properties = new java.util.Properties();
        try (java.io.Reader reader = Files.newBufferedReader(file)) { properties.load(reader); }
        catch (Exception e) { throw new IllegalStateException("Could not load language file: " + file, e); }
        Map<String,String> map = new ConcurrentHashMap<>();
        properties.forEach((key,value) -> map.put(String.valueOf(key), String.valueOf(value)));
        bundles.put(language, map);
    }
    public String language() { return language; }
    public String defaultLanguage() { return defaultLanguage; }
    public boolean hasLanguage(String language) { return bundles.containsKey(language); }
    public Map<String,String> messages(String language) { return Map.copyOf(bundles.getOrDefault(language, Map.of())); }
}
