package dev.kehlchen.lib.core;

import dev.kehlchen.lib.core.config.Config;
import dev.kehlchen.lib.core.config.ConfigBuilder;
import dev.kehlchen.lib.core.config.ConfigFormat;
import dev.kehlchen.lib.core.localization.LanguageManager;
import dev.kehlchen.lib.core.serialization.JsonSerialization;
import dev.kehlchen.lib.core.serialization.UuidSerializer;
import dev.kehlchen.lib.core.util.DurationUtils;
import dev.kehlchen.lib.core.util.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2Test {
    @TempDir Path temp;

    @Test
    void utilityAndUuidSerialization() {
        assertTrue(StringUtils.isBlank("  "));
        assertEquals("Hello", StringUtils.capitalize("hello"));
        assertEquals(Duration.ofSeconds(90), DurationUtils.parse("1m30s"));
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, new UuidSerializer().deserialize(uuid.toString()));
    }

    @Test
    void jsonConfigSupportsDefaultsReloadAndNestedValues() throws Exception {
        Path file = temp.resolve("config.json");
        Config config = new ConfigBuilder().path(file).format(ConfigFormat.JSON)
                .defaultValue("server.port", 25565)
                .defaultValue("server.enabled", true)
                .build();

        assertEquals(25565, config.getInt("server.port", 0));
        assertTrue(config.getBoolean("server.enabled", false));
        config.set("server.name", "test");
        config.save();
        config.reload();
        assertEquals("test", config.getString("server.name"));
    }

    @Test
    void jsonGenericSerialization() {
        JsonSerialization json = new JsonSerialization();
        String text = json.toJson(Map.of("answer", 42));
        assertEquals(42.0, json.fromJson(text, Map.class).get("answer"));
    }

    @Test
    void localizationSupportsFallbackAndPlaceholders() throws Exception {
        Files.writeString(temp.resolve("en.properties"), "welcome=Welcome {0}\nmissing=ok");
        Files.writeString(temp.resolve("de.properties"), "missing=ja");
        LanguageManager languages = new LanguageManager(temp, "en");
        languages.setLanguage("de");
        assertEquals("Welcome Steve", languages.format(languages.get("welcome"), "Steve"));
        assertEquals("ja", languages.get("missing"));
        assertFalse(languages.get("unknown").isEmpty());
    }
}
