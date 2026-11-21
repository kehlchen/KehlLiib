package dev.kehlchen.lib.core.serialization;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public final class ComponentSerializer implements Serializer<Component> {
    private final GsonComponentSerializer serializer = GsonComponentSerializer.gson();
    @Override public String serialize(Component value) {
        return value == null ? "" : serializer.serialize(value);
    }
    @Override public Component deserialize(String data) {
        if (data == null || data.isBlank()) throw new IllegalArgumentException("Component data is blank");
        try { return serializer.deserialize(data); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid component", e); }
    }
}
