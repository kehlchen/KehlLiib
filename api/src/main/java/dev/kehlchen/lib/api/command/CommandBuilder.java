package dev.kehlchen.lib.api.command;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Fluent builder for root and nested commands. */
public final class CommandBuilder {
    private final String name;
    private final List<String> aliases = new ArrayList<>();
    private final List<CommandArgument<?>> arguments = new ArrayList<>();
    private final Map<String, CommandBuilder> subcommands = new LinkedHashMap<>();
    private String permission;
    private String permissionMessage;
    private String description;
    private String usage;
    private CommandExecutor executor;
    private SenderType senderType = SenderType.ANY;
    private Duration cooldown = Duration.ZERO;

    private CommandBuilder(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public static CommandBuilder command(String name) {
        return new CommandBuilder(name);
    }

    public CommandBuilder alias(String... aliases) {
        Collections.addAll(this.aliases, aliases);
        return this;
    }

    public CommandBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    public CommandBuilder permissionMessage(String message) {
        this.permissionMessage = message;
        return this;
    }

    public CommandBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CommandBuilder usage(String usage) {
        this.usage = usage;
        return this;
    }

    public CommandBuilder playerOnly() {
        senderType = SenderType.PLAYER;
        return this;
    }

    public CommandBuilder consoleOnly() {
        senderType = SenderType.CONSOLE;
        return this;
    }

    public CommandBuilder cooldown(Duration cooldown) {
        this.cooldown = cooldown == null ? Duration.ZERO : cooldown;
        return this;
    }

    public CommandBuilder argument(CommandArgument<?> argument) {
        arguments.add(Objects.requireNonNull(argument, "argument"));
        return this;
    }

    public CommandBuilder subcommand(CommandBuilder subcommand) {
        subcommands.put(subcommand.name, subcommand);
        return this;
    }

    public CommandBuilder executor(CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    public CommandDefinition build() {
        return new CommandDefinition(name, List.copyOf(aliases), List.copyOf(arguments),
                Collections.unmodifiableMap(new LinkedHashMap<>(subcommands)), permission,
                permissionMessage, description, usage, executor, senderType, cooldown);
    }

    public enum SenderType {
        ANY, PLAYER, CONSOLE
    }
}
