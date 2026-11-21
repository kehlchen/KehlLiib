package dev.kehlchen.lib.api.command;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Immutable command metadata produced by {@link CommandBuilder}. */
public record CommandDefinition(
        String name,
        List<String> aliases,
        List<CommandArgument<?>> arguments,
        Map<String, CommandBuilder> subcommands,
        String permission,
        String permissionMessage,
        String description,
        String usage,
        CommandExecutor executor,
        CommandBuilder.SenderType senderType,
        Duration cooldown
) {}
