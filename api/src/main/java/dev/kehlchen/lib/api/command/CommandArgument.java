package dev.kehlchen.lib.api.command;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/** A typed command argument. */
public record CommandArgument<T>(
        String name,
        ArgumentParser<T> parser,
        boolean required,
        T defaultValue,
        Function<CommandContext, List<String>> suggestions,
        BiFunction<String, CommandContext, String> validator
) {
    public CommandArgument {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(parser, "parser");
    }

    public static <T> CommandArgument<T> required(String name, ArgumentParser<T> parser) {
        return new CommandArgument<>(name, parser, true, null, context -> List.of(), null);
    }

    public static <T> CommandArgument<T> optional(String name, ArgumentParser<T> parser, T defaultValue) {
        return new CommandArgument<>(name, parser, false, defaultValue, context -> List.of(), null);
    }

    public CommandArgument<T> withSuggestions(Function<CommandContext, List<String>> suggestions) {
        return new CommandArgument<>(name, parser, required, defaultValue, suggestions, validator);
    }

    public CommandArgument<T> validated(BiFunction<String, CommandContext, String> validator) {
        return new CommandArgument<>(name, parser, required, defaultValue, suggestions, validator);
    }
}
