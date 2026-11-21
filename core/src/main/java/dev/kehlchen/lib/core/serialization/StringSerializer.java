package dev.kehlchen.lib.core.serialization;

public final class StringSerializer implements Serializer<String> {
    @Override public String serialize(String value) { return value == null ? "" : value; }
    @Override public String deserialize(String data) { return data; }
}
