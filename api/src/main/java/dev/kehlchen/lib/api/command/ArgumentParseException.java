package dev.kehlchen.lib.api.command;

/** Indicates that a command argument could not be parsed or validated. */
public class ArgumentParseException extends Exception {
    public ArgumentParseException(String message) {
        super(message);
    }

    public ArgumentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
