package dev.kehlchen.lib.api.command;

/** Result of a command execution. */
public enum CommandResult {
    SUCCESS,
    FAILURE,
    CANCELLED,
    NO_PERMISSION,
    INVALID_ARGUMENT,
    COOLDOWN
}
