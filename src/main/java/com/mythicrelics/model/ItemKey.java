package com.mythicrelics.model;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ItemKey {
    VOID_BLADE("voidblade", "§5Void Blade", Material.NETHERITE_SWORD, true),
    LUCK_COIN("luckcoin", "§6Luck Coin", Material.HEART_OF_THE_SEA, true),
    WIND_WAKER("windwaker", "§5Wind Waker", Material.NETHERITE_SWORD, true),
    HERACLES("heracles", "§5Heracles Sword", Material.NETHERITE_SWORD, true),
    ZEUS("zeus", "§5Zeus’s Javelin", Material.TRIDENT, true),
    GOLDEN_FLEECE("goldenfleece", "§6Golden Fleece", Material.WHITE_WOOL, true),
    CRONOS("cronos", "§5Cronos Scythe", Material.NETHERITE_SWORD, true),
    BAG_OF_WINDS("bagofwinds", "§bBag of Winds", Material.BUNDLE, false);

    private final String key;
    private final String displayName;
    private final Material material;
    private final boolean mythic;

    ItemKey(String key, String displayName, Material material, boolean mythic) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.mythic = mythic;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public Material material() { return material; }
    public boolean mythic() { return mythic; }

    public static Optional<ItemKey> fromInput(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).replace("_", "");
        return Arrays.stream(values()).filter(v -> v.key.equals(normalized)).findFirst();
    }
}
