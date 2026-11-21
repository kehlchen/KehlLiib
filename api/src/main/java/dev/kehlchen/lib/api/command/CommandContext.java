package dev.kehlchen.lib.api.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Execution context passed to command executors. */
public interface CommandContext {
    CommandSender sender();

    default Optional<Player> player() {
        return sender() instanceof Player player ? Optional.of(player) : Optional.empty();
    }

    String label();
    List<String> rawArguments();
    Map<String, Object> arguments();

    default <T> T argument(String name) {
        return (T) arguments().get(name);
    }

    default <T> Optional<T> optionalArgument(String name) {
        return Optional.ofNullable(argument(name));
    }

    void reply(Component message);
}
