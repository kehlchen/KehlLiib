package dev.kehlchen.lib.core.logging;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LoggerManager {
    private static final ConcurrentHashMap<String, LogLevel> LEVELS = new ConcurrentHashMap<>();
    private LoggerManager() {}
    public static KehlLibLogger module(String name) { return KehlLibLogger.forModule(name); }
    public static void setLevel(String module, LogLevel level) {
        LEVELS.put(module, level);
        module(module).setLevel(level);
    }
    public static LogLevel level(String module) { return LEVELS.getOrDefault(module, LogLevel.INFO); }
    public static void enableFileLogging(String module, Path file) { module(module).addFile(file); }
    public static void setColored(String module, boolean colored) { module(module).setColored(colored); }
}
