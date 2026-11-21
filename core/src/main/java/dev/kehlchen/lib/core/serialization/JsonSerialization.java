package dev.kehlchen.lib.core.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public final class JsonSerialization {
    private final Gson gson;
    public JsonSerialization() { this(new GsonBuilder().serializeNulls().create()); }
    public JsonSerialization(Gson gson) { this.gson = gson; }
    public String toJson(Object value) { return gson.toJson(value); }
    public String toJson(Object value, Type type) { return gson.toJson(value, type); }
    public <T> T fromJson(String json, Class<T> type) {
        try { return gson.fromJson(json, type); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid JSON", e); }
    }
    public <T> T fromJson(String json, Type type) {
        try { return gson.fromJson(json, type); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid JSON", e); }
    }
}
