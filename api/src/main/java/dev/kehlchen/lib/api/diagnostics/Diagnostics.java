package dev.kehlchen.lib.api.diagnostics;

import dev.kehlchen.lib.api.Lifecycle;

public interface Diagnostics extends Lifecycle {
    DiagnosticReport report();
    void setDebugLogging(boolean enabled);
}
