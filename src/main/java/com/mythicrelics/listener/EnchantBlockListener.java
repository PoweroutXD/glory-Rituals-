package com.mythicrelics.listener;

import com.mythicrelics.item.ItemFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

public class EnchantBlockListener implements Listener {
    private final ItemFactory itemFactory;

    public EnchantBlockListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (itemFactory.isMythic(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        if (itemFactory.isMythic(first) || itemFactory.isMythic(second)) {
            event.setResult(null);
        }
    }
}
