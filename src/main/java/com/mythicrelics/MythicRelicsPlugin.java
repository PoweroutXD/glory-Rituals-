package com.mythicrelics;

import com.mythicrelics.command.MythicCommand;
import com.mythicrelics.item.ItemFactory;
import com.mythicrelics.listener.AbilityListener;
import com.mythicrelics.listener.EnchantBlockListener;
import com.mythicrelics.listener.RitualListener;
import com.mythicrelics.manager.CooldownManager;
import com.mythicrelics.manager.HUDManager;
import com.mythicrelics.manager.MythicManager;
import com.mythicrelics.manager.RitualManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class MythicRelicsPlugin extends JavaPlugin {
    private ItemFactory itemFactory;
    private CooldownManager cooldownManager;
    private HUDManager hudManager;
    private MythicManager mythicManager;
    private RitualManager ritualManager;
    private final Set<Location> cronosDomeBlocks = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        itemFactory = new ItemFactory(this);
        cooldownManager = new CooldownManager(this);
        mythicManager = new MythicManager(itemFactory);
        ritualManager = new RitualManager(this, itemFactory);
        ritualManager.load();
        ritualManager.startTicker();
        hudManager = new HUDManager(this, itemFactory, cooldownManager);
        hudManager.start();

        PluginCommand mythic = getCommand("mythic");
        if (mythic != null) {
            MythicCommand command = new MythicCommand(this);
            mythic.setExecutor(command);
            mythic.setTabCompleter(command);
        } else {
            getLogger().severe("Command /mythic missing in plugin.yml");
        }

        Bukkit.getPluginManager().registerEvents(new AbilityListener(this, itemFactory, cooldownManager), this);
        Bukkit.getPluginManager().registerEvents(new EnchantBlockListener(itemFactory), this);
        Bukkit.getPluginManager().registerEvents(new RitualListener(ritualManager), this);
    }

    @Override
    public void onDisable() {
        if (hudManager != null) hudManager.stop();
        if (cooldownManager != null) cooldownManager.save();
        if (ritualManager != null) {
            ritualManager.save();
            ritualManager.stopTicker();
        }
    }

    public ItemFactory getItemFactory() { return itemFactory; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public MythicManager getMythicManager() { return mythicManager; }
    public RitualManager getRitualManager() { return ritualManager; }
    public Set<Location> getCronosDomeBlocks() { return cronosDomeBlocks; }
}
