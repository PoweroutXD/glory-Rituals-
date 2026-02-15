package com.mythicrelics.item;

import com.mythicrelics.MythicRelicsPlugin;
import com.mythicrelics.model.ItemKey;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemFactory {
    private final MythicRelicsPlugin plugin;
    private final NamespacedKey mythicIdKey;
    private final NamespacedKey itemKeyKey;

    public ItemFactory(MythicRelicsPlugin plugin) {
        this.plugin = plugin;
        this.mythicIdKey = new NamespacedKey(plugin, "mythic_id");
        this.itemKeyKey = new NamespacedKey(plugin, "item_key");
    }

    public ItemStack create(ItemKey itemKey) {
        ItemStack stack = new ItemStack(itemKey.material());
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(itemKey.displayName());
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(mythicIdKey, PersistentDataType.STRING, itemKey.mythic() ? "true" : "false");
        pdc.set(itemKeyKey, PersistentDataType.STRING, itemKey.key());
        if (itemKey == ItemKey.BAG_OF_WINDS) {
            pdc.set(new NamespacedKey(plugin, "bag_uses"), PersistentDataType.INTEGER, 4);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isTagged(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer().has(itemKeyKey, PersistentDataType.STRING);
    }

    public boolean isMythic(ItemStack stack) {
        if (!isTagged(stack)) return false;
        String marker = stack.getItemMeta().getPersistentDataContainer().get(mythicIdKey, PersistentDataType.STRING);
        return "true".equals(marker);
    }

    public ItemKey resolve(ItemStack stack) {
        if (!isTagged(stack)) return null;
        String key = stack.getItemMeta().getPersistentDataContainer().get(itemKeyKey, PersistentDataType.STRING);
        return ItemKey.fromInput(key == null ? "" : key).orElse(null);
    }

    public boolean hasForbiddenEnchants(ItemStack stack) {
        if (stack == null) return false;
        for (Enchantment enchantment : stack.getEnchantments().keySet()) {
            if (enchantment != null) return true;
        }
        return false;
    }

    public NamespacedKey getItemKeyKey() {
        return itemKeyKey;
    }
}
