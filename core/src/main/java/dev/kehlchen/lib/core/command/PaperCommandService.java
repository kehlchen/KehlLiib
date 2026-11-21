package dev.kehlchen.lib.core.command;

import dev.kehlchen.lib.api.command.ArgumentParseException;
import dev.kehlchen.lib.api.command.CommandArgument;
import dev.kehlchen.lib.api.command.CommandBuilder;
import dev.kehlchen.lib.api.command.CommandContext;
import dev.kehlchen.lib.api.command.CommandDefinition;
import dev.kehlchen.lib.api.command.CommandRegistration;
import dev.kehlchen.lib.api.command.CommandResult;
import dev.kehlchen.lib.api.command.CommandService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bukkit-backed command framework with lifecycle cleanup and common typed parsers. */
public final class PaperCommandService implements CommandService {
    private final Map<String, RegisteredCommand> commands = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Instant>> cooldowns = new ConcurrentHashMap<>();

    @Override public String name() { return "commands"; }
    @Override public String version() { return "1.0"; }
    @Override public void onEnable() {}

    @Override
    public void onDisable() {
        for (RegisteredCommand registration : List.copyOf(commands.values())) {
            Bukkit.getCommandMap().getKnownCommands().remove(registration.name());
            for (String alias : registration.definition.aliases()) {
                Bukkit.getCommandMap().getKnownCommands().remove(alias);
            }
        }
        commands.clear();
        cooldowns.clear();
    }

    @Override
    public CommandRegistration register(Plugin owner, CommandBuilder builder) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(builder, "builder");
        CommandDefinition definition = builder.build();
        RegisteredCommand command = new RegisteredCommand(owner, definition);
        boolean ok = Bukkit.getCommandMap().register(owner.getName().toLowerCase(), command);
        if (!ok) throw new IllegalStateException("Command is already registered: " + definition.name());
        commands.put(definition.name().toLowerCase(), command);
        return new CommandRegistration(definition.name(), this);
    }

    @Override
    public boolean unregister(String name) {
        RegisteredCommand removed = commands.remove(name.toLowerCase());
        if (removed == null) return false;
        Bukkit.getCommandMap().getKnownCommands().remove(name.toLowerCase());
        return true;
    }

    private final class RegisteredCommand extends org.bukkit.command.Command {
        private final Plugin owner;
        private final CommandDefinition definition;

        private RegisteredCommand(Plugin owner, CommandDefinition definition) {
            super(definition.name(),
                    definition.description() == null ? "" : definition.description(),
                    definition.usage() == null ? "/" + definition.name() : definition.usage(),
                    definition.aliases());
            this.owner = owner;
            this.definition = definition;
        }

        public String name() {
            return getName();
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            CommandDefinition target = definition;
            List<String> raw = new ArrayList<>(List.of(args));
            if (!raw.isEmpty()) {
                CommandBuilder nested = target.subcommands().get(raw.get(0).toLowerCase());
                if (nested != null) {
                    target = nested.build();
                    raw.remove(0);
                }
            }
            if (target.permission() != null && !sender.hasPermission(target.permission())) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        target.permissionMessage() == null ? "<red>You do not have permission." : target.permissionMessage()));
                return true;
            }
            if (target.senderType() == CommandBuilder.SenderType.PLAYER && !(sender instanceof Player)) {
                sender.sendMessage(Component.text("This command can only be used by a player."));
                return true;
            }
            if (target.senderType() == CommandBuilder.SenderType.CONSOLE && sender instanceof Player) {
                sender.sendMessage(Component.text("This command can only be used from console."));
                return true;
            }

            Map<String, Object> parsed = new HashMap<>();
            for (int i = 0; i < target.arguments().size(); i++) {
                CommandArgument<?> argument = target.arguments().get(i);
                if (i >= raw.size()) {
                    if (argument.required()) {
                        sender.sendMessage(Component.text("Missing required argument: " + argument.name()));
                        return true;
                    }
                    parsed.put(argument.name(), argument.defaultValue());
                    continue;
                }
                String value = i == target.arguments().size() - 1
                        && raw.size() > i
                        && isGreedy(argument)
                        ? String.join(" ", raw.subList(i, raw.size()))
                        : raw.get(i);
                if (argument.validator() != null) {
                    String invalid = argument.validator().apply(value, null);
                    if (invalid != null) {
                        sender.sendMessage(Component.text(invalid));
                        return true;
                    }
                }
                try {
                    parsed.put(argument.name(), argument.parser().parse(value));
                } catch (ArgumentParseException failure) {
                    sender.sendMessage(Component.text("Invalid " + argument.name() + ": " + failure.getMessage()));
                    return true;
                }
            }

            Duration cooldown = target.cooldown();
            if (!cooldown.isZero() && !cooldown.isNegative()) {
                String key = sender instanceof Player player
                        ? player.getUniqueId().toString()
                        : sender.getName();
                Instant ready = cooldowns.computeIfAbsent(key, ignored -> new HashMap<>()).get(target.name());
                if (ready != null && Instant.now().isBefore(ready)) {
                    sender.sendMessage(Component.text("This command is on cooldown."));
                    return true;
                }
                cooldowns.computeIfAbsent(key, ignored -> new HashMap<>()).put(target.name(),
                        Instant.now().plus(cooldown));
            }

            CommandContext context = new PaperCommandContext(sender, label,
                    raw.stream().toList(), parsed);
            if (target.executor() == null) {
                sender.sendMessage(Component.text(target.description() == null
                        ? "Usage: " + (target.usage() == null ? "/" + target.name() : target.usage())
                        : target.description()));
                return true;
            }
            CommandResult result = target.executor().execute(context);
            return result != null && result != CommandResult.CANCELLED;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            CommandDefinition target = definition;
            if (args.length > 1) {
                CommandBuilder nested = target.subcommands().get(args[0].toLowerCase());
                if (nested != null) target = nested.build();
            }
            int index = target == definition ? args.length - 1 : args.length - 2;
            if (index < 0) return new ArrayList<>(target.subcommands().keySet());
            if (!target.subcommands().isEmpty() && args.length == 1) {
                return target.subcommands().keySet().stream()
                        .filter(value -> value.startsWith(args[0].toLowerCase())).toList();
            }
            if (index >= target.arguments().size()) return List.of();
            CommandArgument<?> argument = target.arguments().get(index);
            String prefix = args[args.length - 1].toLowerCase();
            CommandContext context = new PaperCommandContext(sender, alias,
                    List.of(args), Map.of());
            return argument.suggestions().apply(context).stream()
                    .filter(value -> value.toLowerCase().startsWith(prefix)).toList();
        }

        private boolean isGreedy(CommandArgument<?> argument) {
            return argument.name().startsWith("*");
        }
    }

    private record PaperCommandContext(
            CommandSender sender,
            String label,
            List<String> rawArguments,
            Map<String, Object> arguments
    ) implements CommandContext {
        @Override
        public void reply(Component message) {
            sender.sendMessage(message);
        }
    }

    public static ArgumentParseRegistry parsers() {
        return new ArgumentParseRegistry();
    }

    /** Common argument parsers exposed for custom command builders. */
    public static final class ArgumentParseRegistry {
        public dev.kehlchen.lib.api.command.ArgumentParser<String> string() {
            return value -> value;
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<Integer> integer() {
            return value -> parseNumber(value, Integer::parseInt, "integer");
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<Long> longValue() {
            return value -> parseNumber(value, Long::parseLong, "long");
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<Double> doubleValue() {
            return value -> parseNumber(value, Double::parseDouble, "double");
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<Boolean> bool() {
            return value -> switch (value.toLowerCase()) {
                case "true", "yes", "on", "1" -> true;
                case "false", "no", "off", "0" -> false;
                default -> throw new ArgumentParseException("Expected true or false");
            };
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<UUID> uuid() {
            return value -> {
                try { return UUID.fromString(value); }
                catch (IllegalArgumentException failure) {
                    throw new ArgumentParseException("Expected a UUID", failure);
                }
            };
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<Player> player() {
            return value -> {
                Player player = Bukkit.getPlayerExact(value);
                if (player == null) throw new ArgumentParseException("Player not found: " + value);
                return player;
            };
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<OfflinePlayer> offlinePlayer() {
            return Bukkit::getOfflinePlayer;
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<World> world() {
            return value -> {
                World world = Bukkit.getWorld(value);
                if (world == null) throw new ArgumentParseException("World not found: " + value);
                return world;
            };
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<Material> material() {
            return value -> {
                Material material = Material.matchMaterial(value);
                if (material == null) throw new ArgumentParseException("Material not found: " + value);
                return material;
            };
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<EntityType> entityType() {
            return value -> {
                EntityType type = EntityType.fromName(value);
                if (type == null) throw new ArgumentParseException("Entity type not found: " + value);
                return type;
            };
        }
        public dev.kehlchen.lib.api.command.ArgumentParser<NamespacedKey> namespacedKey() {
            return value -> {
                NamespacedKey key = NamespacedKey.fromString(value);
                if (key == null) throw new ArgumentParseException("Invalid namespaced key: " + value);
                return key;
            };
        }

        private <T> T parseNumber(String value, java.util.function.Function<String, T> parser, String name)
                throws ArgumentParseException {
            try { return parser.apply(value); }
            catch (NumberFormatException failure) {
                throw new ArgumentParseException("Expected " + name, failure);
            }
        }
    }
}
