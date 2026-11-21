package dev.kehlchen.lib.core.logging;

import java.util.logging.Level;

public enum LogLevel {
    TRACE(Level.FINEST), DEBUG(Level.FINE), INFO(Level.INFO), WARN(Level.WARNING), ERROR(Level.SEVERE), OFF(Level.OFF);

    private final Level javaLevel;
    LogLevel(Level javaLevel) { this.javaLevel = javaLevel; }
    public Level javaLevel() { return javaLevel; }
    public static LogLevel from(Level level) {
        if (level == Level.SEVERE) return ERROR;
        if (level == Level.WARNING) return WARN;
        if (level == Level.INFO) return INFO;
        if (level.intValue() <= Level.FINEST.intValue()) return TRACE;
        if (level.intValue() <= Level.FINE.intValue()) return DEBUG;
        return INFO;
    }
}
