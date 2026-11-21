package dev.kehlchen.lib.api.command;

/** Executes a command. */
@FunctionalInterface
public interface CommandExecutor {
    CommandResult execute(CommandContext context);
}
