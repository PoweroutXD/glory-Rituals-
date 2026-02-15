package com.mythicrelics.manager;

import com.mythicrelics.MythicRelicsPlugin;
import com.mythicrelics.item.ItemFactory;
import com.mythicrelics.model.ItemKey;
import com.mythicrelics.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class HUDManager {
    private final MythicRelicsPlugin plugin;
    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;
    private BukkitTask task;

    public HUDManager(MythicRelicsPlugin plugin, ItemFactory itemFactory, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack held = player.getInventory().getItemInMainHand();
            ItemKey key = itemFactory.resolve(held);
            if (key == null) continue;
            UUID uuid = player.getUniqueId();
            String message = switch (key) {
                case VOID_BLADE -> dual(uuid, "voidblade.dragonrun", "[DR]", "voidblade.dragonbreath", "[DB]");
                case WIND_WAKER -> single(uuid, "windwaker.gale", "[WW]");
                case BAG_OF_WINDS -> single(uuid, "bagofwinds.gust", "[BW]");
                case LUCK_COIN -> single(uuid, "luckcoin.flip", "[LC]");
                case HERACLES -> single(uuid, "heracles.titansurge", "[TS]");
                case ZEUS -> single(uuid, "zeus.empoweredthrow", "[ZJ]");
                case CRONOS -> single(uuid, "cronos.timeprison", "[TP]");
                case GOLDEN_FLEECE -> single(uuid, "goldenfleece.resurrect", "[GF]");
            };
            player.sendActionBar(Component.text(message));
        }
    }

    private String single(UUID uuid, String ability, String icon) {
        long rem = cooldownManager.getRemaining(uuid, ability);
        return rem == 0 ? "§a" + icon + " Ready" : "§c" + icon + " " + TimeUtil.formatCooldown(rem);
    }

    private String dual(UUID uuid, String a1, String icon1, String a2, String icon2) {
        long r1 = cooldownManager.getRemaining(uuid, a1);
        long r2 = cooldownManager.getRemaining(uuid, a2);
        String s1 = r1 == 0 ? "§a" + icon1 + " Ready" : "§c" + icon1 + " " + TimeUtil.formatCooldown(r1);
        String s2 = r2 == 0 ? "§a" + icon2 + " Ready" : "§c" + icon2 + " " + TimeUtil.formatCooldown(r2);
        return s1 + "  " + s2;
    }
}
