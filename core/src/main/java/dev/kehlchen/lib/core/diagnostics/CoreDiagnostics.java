package dev.kehlchen.lib.core.diagnostics;

import dev.kehlchen.lib.api.ServiceRegistry;
import dev.kehlchen.lib.api.diagnostics.DiagnosticReport;
import dev.kehlchen.lib.api.diagnostics.Diagnostics;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class CoreDiagnostics implements Diagnostics {
    private final ServiceRegistry services;
    private volatile boolean debugLogging;

    public CoreDiagnostics(ServiceRegistry services) {
        this.services = services;
    }

    @Override public String name() { return "diagnostics"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() {}
    @Override public void onDisable() { debugLogging = false; }
    @Override public void setDebugLogging(boolean enabled) { debugLogging = enabled; }

    @Override
    public DiagnosticReport report() {
        Map<String, String> dependencies = new HashMap<>();
        for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            dependencies.put(plugin.getName(), plugin.getPluginMeta().getVersion());
        }

        Map<String, String> serviceMap = new HashMap<>();
        serviceMap.put("KehlLib ServiceRegistry", "available");

        ArrayList<String> warnings = new ArrayList<>();
        if (System.getProperty("java.version", "").startsWith("1.")) {
            warnings.add("Legacy Java version format detected");
        }
        boolean folia = false;
        try {
            Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            folia = true;
            warnings.add("Folia detected: verify region ownership for every Bukkit/world operation");
        } catch (ReflectiveOperationException ignored) {
            // Traditional Bukkit/Paper server.
        }

        return new DiagnosticReport(
                Instant.now(),
                Bukkit.getBukkitVersion(),
                Bukkit.getMinecraftVersion(),
                System.getProperty("java.version"),
                folia,
                dependencies,
                serviceMap,
                warnings);
    }
}
