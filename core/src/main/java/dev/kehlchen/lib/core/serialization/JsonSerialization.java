package dev.kehlchen.lib.core.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;

public final class JsonSerialization {
    private final Gson gson;
    public JsonSerialization() {
        this(new GsonBuilder().serializeNulls()
                .registerTypeAdapter(Instant.class, instantAdapter())
                .create());
    }
    public JsonSerialization(Gson gson) { this.gson = gson; }
    public String toJson(Object value) { return gson.toJson(value); }
    public String toJson(Object value, Type type) { return gson.toJson(value, type); }
    public <T> T fromJson(String json, Class<T> type) {
        try { return gson.fromJson(json, type); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid JSON", e); }
    }
    public <T> T fromJson(String json, Type type) {
        try { return gson.fromJson(json, type); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid JSON", e); }
    }

    private static TypeAdapter<Instant> instantAdapter() {
        return new TypeAdapter<Instant>() {
            @Override public void write(JsonWriter output, Instant value) throws IOException {
                if (value == null) output.nullValue();
                else output.value(value.toString());
            }
            @Override public Instant read(JsonReader input) throws IOException {
                if (input.peek() == com.google.gson.stream.JsonToken.NULL) {
                    input.nextNull();
                    return null;
                }
                return Instant.parse(input.nextString());
            }
        }.nullSafe();
    }
}
