package com.mythicrelics.manager;

import com.mythicrelics.item.ItemFactory;
import com.mythicrelics.model.ItemKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MythicManager {
    private final ItemFactory itemFactory;

    public MythicManager(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public boolean canReceiveMythic(Player player, ItemKey incoming) {
        if (!incoming.mythic()) return true;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) continue;
            if (itemFactory.isMythic(stack)) return false;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return offhand == null || !itemFactory.isMythic(offhand);
    }
}
