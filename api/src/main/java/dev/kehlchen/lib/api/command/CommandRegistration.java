package dev.kehlchen.lib.api.command;

import java.util.Objects;

/** Handle to a registered command. */
public final class CommandRegistration implements AutoCloseable {
    private final String name;
    private final CommandService service;
    private boolean closed;

    public CommandRegistration(String name, CommandService service) {
        this.name = Objects.requireNonNull(name, "name");
        this.service = Objects.requireNonNull(service, "service");
    }

    public String name() {
        return name;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            service.unregister(name);
        }
    }
}
