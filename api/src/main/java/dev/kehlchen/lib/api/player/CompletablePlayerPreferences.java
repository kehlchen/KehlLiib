package dev.kehlchen.lib.api.player;

import java.util.concurrent.CompletableFuture;

/** Asynchronous player preference storage. */
public interface CompletablePlayerPreferences {
    CompletableFuture<String> getString(String key, String defaultValue);
    CompletableFuture<Void> setString(String key, String value);
    CompletableFuture<Boolean> getBoolean(String key, boolean defaultValue);
    CompletableFuture<Void> setBoolean(String key, boolean value);
}
