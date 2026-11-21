package dev.kehlchen.lib.api.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Fluent ItemStack builder using Bukkit metadata and Adventure components. */
public final class ItemBuilder {
    private final Material material;
    private int amount = 1;
    private Component name;
    private final List<Component> lore = new ArrayList<>();
    private final Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
    private final List<ItemFlag> flags = new ArrayList<>();
    private Integer customModelData;
    private Boolean unbreakable;
    private Integer damage;
    private final List<AttributeModifierEntry> attributes = new ArrayList<>();

    private ItemBuilder(Material material) {
        this.material = Objects.requireNonNull(material, "material");
        if (material == Material.AIR) throw new IllegalArgumentException("Item material cannot be AIR");
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public ItemBuilder amount(int amount) {
        if (amount < 1 || amount > 64) {
            throw new IllegalArgumentException("Invalid item amount: " + amount);
        }
        this.amount = amount;
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        this.name = MiniMessage.miniMessage().deserialize(miniMessage == null ? "" : miniMessage);
        return this;
    }

    public ItemBuilder name(Component name) {
        this.name = name;
        return this;
    }

    public ItemBuilder lore(String... miniMessageLines) {
        lore.clear();
        for (String line : miniMessageLines) lore.add(MiniMessage.miniMessage().deserialize(line));
        return this;
    }

    public ItemBuilder lore(Component... lines) {
        lore.clear();
        lore.addAll(List.of(lines));
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        enchantments.put(Objects.requireNonNull(enchantment, "enchantment"), level);
        return this;
    }

    public ItemBuilder flag(ItemFlag... flags) {
        this.flags.addAll(List.of(flags));
        return this;
    }

    public ItemBuilder customModelData(int modelData) {
        this.customModelData = modelData;
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public ItemBuilder damage(int damage) {
        this.damage = damage;
        return this;
    }

    public ItemBuilder attribute(Attribute attribute, AttributeModifier modifier) {
        attributes.add(new AttributeModifierEntry(attribute, modifier));
        return this;
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (name != null) meta.displayName(name);
        if (!lore.isEmpty()) meta.lore(lore);
        enchantments.forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
        for (ItemFlag flag : flags) meta.addItemFlags(flag);
        if (customModelData != null) meta.setCustomModelData(customModelData);
        if (unbreakable != null) meta.setUnbreakable(unbreakable);
        if (damage != null && meta instanceof Damageable damageMeta) damageMeta.setDamage(damage);
        for (AttributeModifierEntry entry : attributes) {
            meta.addAttributeModifier(entry.attribute(), entry.modifier());
        }
        item.setItemMeta(meta);
        return item;
    }

    private record AttributeModifierEntry(Attribute attribute, AttributeModifier modifier) {}
}
