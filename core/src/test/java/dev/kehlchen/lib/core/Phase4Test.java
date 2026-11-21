package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.command.ArgumentParseException;
import dev.kehlchen.lib.api.command.CommandArgument;
import dev.kehlchen.lib.api.command.CommandBuilder;
import dev.kehlchen.lib.api.gui.GuiBuilder;
import dev.kehlchen.lib.api.item.ItemBuilder;
import dev.kehlchen.lib.core.command.PaperCommandService;
import dev.kehlchen.lib.core.permission.BukkitPermissionService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase4Test {
    @Test
    void itemBuilderCreatesMetadata() {
        ItemBuilder builder = ItemBuilder.of(Material.DIAMOND_SWORD)
                .amount(1)
                .name("<gold>Legendary Sword")
                .lore("<gray>A powerful weapon")
                .customModelData(42)
                .unbreakable(true);
        assertNotNull(builder);
    }

    @Test
    void itemBuilderRejectsInvalidAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemBuilder.of(Material.DIAMOND).amount(65));
        assertThrows(IllegalArgumentException.class,
                () -> ItemBuilder.of(Material.AIR));
    }

    @Test
    void commonCommandParsersValidateValues() throws ArgumentParseException {
        PaperCommandService.ArgumentParseRegistry parsers = PaperCommandService.parsers();
        assertEquals(42, parsers.integer().parse("42"));
        assertEquals(42L, parsers.longValue().parse("42"));
        assertEquals(true, parsers.bool().parse("yes"));
        assertEquals(3.5d, parsers.doubleValue().parse("3.5"));
        assertThrows(ArgumentParseException.class, () -> parsers.integer().parse("not-a-number"));
    }

    @Test
    void commandBuilderPreservesArgumentsAndCooldown() {
        CommandBuilder builder = CommandBuilder.command("heal")
                .permission("example.heal")
                .playerOnly()
                .cooldown(Duration.ofSeconds(5))
                .argument(CommandArgument.required("target",
                        PaperCommandService.parsers().string()));

        var definition = builder.build();
        assertEquals("heal", definition.name());
        assertEquals("example.heal", definition.permission());
        assertEquals(CommandBuilder.SenderType.PLAYER, definition.senderType());
        assertEquals(Duration.ofSeconds(5), definition.cooldown());
        assertEquals("target", definition.arguments().get(0).name());
    }

    @Test
    void permissionServiceSupportsDirectWildcardGroupsAndExpiration() throws InterruptedException {
        BukkitPermissionService permissions = new BukkitPermissionService();
        UUID user = UUID.randomUUID();

        permissions.set(user, "world.*", true);
        assertTrue(permissions.check(user, "world.build").granted());

        permissions.setGroupPermission("builders", "plot.build", true);
        permissions.defineGroup("builders", "members");
        permissions.setGroup(user, "builders");
        assertTrue(permissions.check(user, "plot.build").granted());

        permissions.setTemporary(user, "admin.cache", true, Duration.ofMillis(5));
        assertTrue(permissions.check(user, "admin.cache").granted());
        Thread.sleep(15);
        assertFalse(permissions.check(user, "admin.cache").granted());

        permissions.set(user, "*", false);
        assertFalse(permissions.check(user, "any.permission").granted());
    }

    @Test
    void permissionInheritanceCycleCannotLoop() {
        BukkitPermissionService permissions = new BukkitPermissionService();
        permissions.defineGroup("a", "b");
        permissions.defineGroup("b", "a");
        UUID user = UUID.randomUUID();
        permissions.setGroup(user, "a");

        assertFalse(permissions.check(user, "cycle.permission").granted());
    }

    @Test
    void guiBuilderValidatesSizeAndSlots() {
        assertThrows(IllegalArgumentException.class, () -> GuiBuilder.gui().rows(7));
        assertThrows(IllegalArgumentException.class, () -> GuiBuilder.gui().rows(3).item(27, null));

        var definition = GuiBuilder.gui()
                .id("menu")
                .title("<dark_gray>Menu")
                .rows(3)
                .build();
        assertEquals("menu", definition.id());
        assertEquals(3, definition.rows());
        assertTrue(definition.items().isEmpty());
    }
}
