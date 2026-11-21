package dev.kehlchen.lib.core.logging;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record StructuredLogEntry(Instant timestamp, String category, String message,
                                 Map<String, Object> fields) {
    public StructuredLogEntry {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        category = category == null ? "kehllib" : category;
        fields = fields == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(fields));
    }
    public String render() {
        StringBuilder out = new StringBuilder().append(timestamp).append(" [").append(category).append("] ").append(message);
        if (!fields.isEmpty()) out.append(" ").append(fields);
        return out.toString();
    }
}
