package dev.kehlchen.lib.core.localization;

import java.nio.file.Path;

public final class Localization {
    private final LanguageManager manager;
    public Localization(Path directory, String defaultLanguage) { manager = new LanguageManager(directory, defaultLanguage); }
    public String tr(String key, Object... arguments) { return manager.get(key, arguments); }
    public String get(String key) { return manager.get(key); }
    public String get(String language, String key) { return manager.get(language, key); }
    public String format(String template, Object... arguments) { return manager.format(template, arguments); }
    public void setLanguage(String language) { manager.setLanguage(language); }
    public LanguageManager manager() { return manager; }
}
