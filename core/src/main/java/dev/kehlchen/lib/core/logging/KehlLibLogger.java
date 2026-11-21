package dev.kehlchen.lib.core.logging;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.function.Predicate;

public final class KehlLibLogger {
    private static final Map<String, KehlLibLogger> LOGGERS = new ConcurrentHashMap<>();
    private final Logger delegate;
    private final String module;
    private volatile LogLevel level = LogLevel.INFO;
    private volatile Predicate<String> filter = message -> true;
    private volatile boolean colors = true;

    private KehlLibLogger(String module) {
        this.module = module;
        this.delegate = Logger.getLogger("KehlLib." + module);
        this.delegate.setUseParentHandlers(false);
        this.delegate.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter());
        this.delegate.addHandler(handler);
    }

    public static KehlLibLogger forModule(String module) {
        Objects.requireNonNull(module, "module");
        return LOGGERS.computeIfAbsent(module, KehlLibLogger::new);
    }
    public static KehlLibLogger of(String module) { return forModule(module); }
    public static KehlLibLogger defaultLogger() { return forModule("core"); }
    public String module() { return module; }
    public void setLevel(LogLevel level) {
        this.level = Objects.requireNonNull(level, "level");
        delegate.setLevel(level.javaLevel());
    }
    public LogLevel level() { return level; }
    public void setFilter(Predicate<String> filter) { this.filter = Objects.requireNonNull(filter, "filter"); }
    public void setColored(boolean colored) { this.colors = colored; }
    public void addFile(Path file) {
        try {
            FileHandler handler = new FileHandler(file.toAbsolutePath().toString(), true);
            handler.setFormatter(new SimpleFormatter());
            delegate.addHandler(handler);
        } catch (IOException e) {
            throw new IllegalStateException("Could not open log file: " + file, e);
        }
    }
    public void trace(String message) { log(LogLevel.TRACE, message, null); }
    public void debug(String message) { log(LogLevel.DEBUG, message, null); }
    public void info(String message) { log(LogLevel.INFO, message, null); }
    public void warn(String message) { log(LogLevel.WARN, message, null); }
    public void error(String message) { log(LogLevel.ERROR, message, null); }
    public void debug(String message, Throwable error) { log(LogLevel.DEBUG, message, error); }
    public void info(String message, Throwable error) { log(LogLevel.INFO, message, error); }
    public void warn(String message, Throwable error) { log(LogLevel.WARN, message, error); }
    public void error(String message, Throwable error) { log(LogLevel.ERROR, message, error); }
    public void structured(LogLevel level, StructuredLogEntry entry) {
        log(level, entry == null ? "" : entry.render(), null);
    }
    public void structured(String message, Map<String, Object> fields) {
        info(new StructuredLogEntry(null, module, message, fields).render());
    }
    public PerformanceTimer timed(String operation) { return PerformanceTimer.start(this, operation); }

    private void log(LogLevel requested, String message, Throwable error) {
        if (requested.ordinal() < level.ordinal() || !filter.test(message == null ? "" : message)) return;
        String rendered = (colors ? ansi(requested) : "") + "[" + module + "] " + (message == null ? "" : message)
                + (error == null ? "" : System.lineSeparator() + ExceptionFormatter.format(error))
                + (colors ? "\u001B[0m" : "");
        delegate.log(requested.javaLevel(), rendered);
    }
    private String ansi(LogLevel level) {
        return switch (level) {
            case TRACE, DEBUG -> "\u001B[36m";
            case INFO -> "\u001B[32m";
            case WARN -> "\u001B[33m";
            case ERROR -> "\u001B[31m";
            case OFF -> "";
        };
    }
    public void close() {
        for (Handler handler : delegate.getHandlers()) handler.close();
    }
}
