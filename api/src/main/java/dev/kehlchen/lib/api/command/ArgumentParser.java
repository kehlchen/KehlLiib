package dev.kehlchen.lib.api.command;

/** Parses raw argument text into a typed value. */
@FunctionalInterface
public interface ArgumentParser<T> {
    T parse(String value) throws ArgumentParseException;
}
