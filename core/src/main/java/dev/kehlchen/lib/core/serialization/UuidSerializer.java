package dev.kehlchen.lib.core.serialization;

import java.util.UUID;

public final class UuidSerializer implements Serializer<UUID> {
    @Override public String serialize(UUID value) { return value == null ? "" : value.toString(); }
    @Override public UUID deserialize(String data) {
        if (data == null || data.isBlank()) throw new IllegalArgumentException("UUID data is blank");
        try { return UUID.fromString(data.trim()); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid UUID: " + data, e); }
    }
}
