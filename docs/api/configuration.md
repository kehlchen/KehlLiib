# Configuration, Serialization, and Localization

## Configuration

The core module provides `Config`, `AbstractConfig`, `ConfigBuilder`, and format implementations:

- `YamlConfig`
- `JsonConfig`
- `TomlConfig`
- `PropertiesConfig`

The format implementations support `reload()`, `save()`, path lookup, typed fallback reads, sections, environment substitution, defaults, and migration metadata.

```java
Config config = new ConfigBuilder()
    .path(plugin.getDataFolder().toPath().resolve("config.yml"))
    .format(ConfigFormat.YAML)
    .defaultValue("server.enabled", true)
    .version(1)
    .build();

boolean enabled = config.getBoolean("server.enabled", true);
config.set("server.name", "Lobby");
config.save();
```

`ConfigFormat` values are `YAML`, `JSON`, `TOML`, and `PROPERTIES`. `ConfigSection` provides nested reads and writes.

Environment substitution is implemented in `AbstractConfig.resolveEnv(...)`. Configuration keys are application-defined; KehlLib core does not define a required plugin `config.yml`.

## Serialization

The following public/core serialization types are available:

- `JsonSerialization`
- `YamlSerialization`
- `SerializationRegistry`
- `Serializer<T>`
- `StringSerializer`
- `UuidSerializer`
- `VersionedSerialization`
- `ItemStackSerializer`
- `InventorySerializer`
- `LocationSerializer`
- `ComponentSerializer`

```java
JsonSerialization json = new JsonSerialization();
String text = json.toJson(playerProfile);
PlayerProfile copy = json.fromJson(text, PlayerProfile.class);
```

Serialization exceptions are implementation-specific and should be handled by the caller. Do not assume arbitrary Bukkit objects can be serialized unless they are supported by a registered or built-in serializer.

## Localization

`Localization` loads properties bundles below a directory and defaults to a language:

```java
Localization localization =
    new Localization(plugin.getDataFolder().toPath(), "en");

String welcome = localization.tr("welcome", player.getName());
messaging.chat(player, messaging.component(welcome));
```

Key methods include `get(...)`, `tr(String, Object...)`, `format(...)`, `setLanguage(...)`, `manager()`, and language bundle lookup through `LanguageManager`.
