package dev.kehlchen.lib.api.profiling;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProfileSnapshot(Instant capturedAt, Map<String, ProfileStatistics> sections) {
    public ProfileSnapshot {
        sections = Map.copyOf(sections);
    }
}
