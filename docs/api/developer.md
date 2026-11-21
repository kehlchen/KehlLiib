# Developer APIs

## Commands

`CommandService` executes `CommandDefinition` values built with `CommandBuilder`.

```java
CommandService commands =
    runtime.services().getRequired(CommandService.class);

commands.register(plugin,
    CommandBuilder.command("heal")
        .description("Heals a player")
        .permission("example.heal")
        .playerOnly()
        .executor(context -> {
            context.player().ifPresent(player ->
                player.setHealth(player.getMaxHealth()));
            context.reply(Component.text("Healed"));
            return CommandResult.SUCCESS;
        }));
```

Typed arguments use `CommandArgument`, `ArgumentParser`, `ArgumentParseException`, suggestions, and validators. `CommandContext`, `CommandResult`, and `CommandRegistration` cover execution and lifecycle.

## Permissions

`PermissionService` supports permission checks and temporary grant/deny state. `PermissionCheck` carries explanation data and `PermissionService` can answer `hasPermission`, `grantTemporary`, `denyTemporary`, and cache-clearing operations.

Check the implementation signature in the IDE or source before assuming every Vault permission feature is present; Vault is a separate optional economy integration, not the permission API.

## Items

`ItemService` creates `ItemStack` values and identifies namespaced custom items.

```java
ItemService items = runtime.services().getRequired(ItemService.class);
ItemStack sword = items.create(
    ItemBuilder.of(Material.DIAMOND_SWORD)
        .name("<gold>Legendary Sword")
        .lore("<gray>A powerful weapon")
        .unbreakable(true)
        .build());
```

`NamespacedCustomItem` carries a `NamespacedKey`.

## GUIs

`GuiBuilder`, `GuiDefinition`, `Gui`, `GuiClickContext`, and `GuiService` create and manage inventory UI sessions.

```java
GuiService guis = runtime.services().getRequired(GuiService.class);
Gui gui = guis.create(GuiBuilder.gui()
    .title("<dark_gray>Menu")
    .rows(3)
    .item(13, ItemBuilder.of(Material.DIAMOND).name("<aqua>Click").build())
    .onClick(13, context ->
        context.player().sendMessage(Component.text("Clicked")))
    .build());

gui.open(player);
runtime.resources().register(gui);
```

Inventory and item operations must occur on the appropriate server thread. GUI callbacks should not mutate world state from an unsafe asynchronous thread.
