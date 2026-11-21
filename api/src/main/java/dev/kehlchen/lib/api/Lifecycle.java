package dev.kehlchen.lib.api;

import java.util.Objects;

/** Defines the lifecycle contract for a KehlLib module. */
public interface Lifecycle {
    /** Returns the stable module name. */
    String name();

    /** Returns the module version. */
    String version();

    /** Starts this module. Implementations must be safe to initialize once. */
    void onEnable();

    /** Stops this module and releases any resources it owns. */
    void onDisable();

    /** Executes lifecycle callback code with a descriptive module name. */
    default void execute(String action, Runnable callback) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(callback, "callback");
        callback.run();
    }
}
