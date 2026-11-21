package dev.kehlchen.lib.core.storage;

import dev.kehlchen.lib.api.storage.StorageSerializer;
import dev.kehlchen.lib.core.serialization.JsonSerialization;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class StorageSerializers {
    private StorageSerializers() {}

    public static <T> StorageSerializer<T> json(Class<T> type) {
        JsonSerialization json = new JsonSerialization();
        return new StorageSerializer<>() {
            @Override public String serialize(T value) { return json.toJson(value, type); }
            @Override public T deserialize(String value) { return json.fromJson(value, type); }
        };
    }

    public static <T> StorageSerializer<T> yaml(Class<T> type) {
        JsonSerialization json = new JsonSerialization();
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        return new StorageSerializer<>() {
            @Override public synchronized String serialize(T value) {
                return yaml.dump(json.fromJson(json.toJson(value, type), Object.class));
            }
            @Override public synchronized T deserialize(String value) {
                return json.fromJson(json.toJson(yaml.load(value)), type);
            }
        };
    }

    public static StorageSerializer<String> strings() {
        return new StorageSerializer<>() {
            @Override public String serialize(String value) { return value; }
            @Override public String deserialize(String value) { return value; }
        };
    }
}
