package dev.kehlchen.lib.api.diagnostics;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DiagnosticReport(
        Instant capturedAt,
        String serverVersion,
        String minecraftVersion,
        String javaVersion,
        boolean folia,
        Map<String, String> dependencies,
        Map<String, String> services,
        List<String> warnings) {
    public DiagnosticReport {
        dependencies = Map.copyOf(dependencies);
        services = Map.copyOf(services);
        warnings = List.copyOf(warnings);
    }
}
