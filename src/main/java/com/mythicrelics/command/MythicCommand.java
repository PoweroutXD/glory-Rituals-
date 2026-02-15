package com.mythicrelics.command;

import com.mythicrelics.MythicRelicsPlugin;
import com.mythicrelics.manager.CooldownManager;
import com.mythicrelics.manager.MythicManager;
import com.mythicrelics.manager.RitualManager;
import com.mythicrelics.model.ItemKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class MythicCommand implements CommandExecutor, TabCompleter {
    private final MythicRelicsPlugin plugin;

    public MythicCommand(MythicRelicsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mythic.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/mythic give|cooldown|ritual|reload");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "cooldown" -> handleCooldown(sender, args);
            case "ritual" -> handleRitual(sender, args);
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage("§aConfig reloaded.");
            }
            default -> sender.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("§cUsage: /mythic give <player> <itemKey>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return; }
        ItemKey key = ItemKey.fromInput(args[2]).orElse(null);
        if (key == null) { sender.sendMessage("§cInvalid item key."); return; }

        MythicManager mythicManager = plugin.getMythicManager();
        if (!mythicManager.canReceiveMythic(target, key)) {
            sender.sendMessage("§cTarget already has a mythic item.");
            return;
        }

        target.getInventory().addItem(plugin.getItemFactory().create(key));
        sender.sendMessage("§aGave " + key.key() + " to " + target.getName());
    }

    private void handleCooldown(CommandSender sender, String[] args) {
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (args.length >= 2 && (args[1].equalsIgnoreCase("resetall") || args[1].equalsIgnoreCase("clearall"))) {
            cooldownManager.resetAll();
            sender.sendMessage("§aAll cooldowns reset.");
            return;
        }
        if (args.length >= 4 && args[1].equalsIgnoreCase("reset")) {
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage("§cPlayer not found."); return; }
            String ability = args[3];
            cooldownManager.reset(target.getUniqueId(), ability);
            sender.sendMessage("§aCooldown reset for " + target.getName() + " -> " + ability);
            return;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("reset")) {
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage("§cPlayer not found."); return; }
            cooldownManager.reset(target.getUniqueId(), "all");
            sender.sendMessage("§aAll cooldowns reset for " + target.getName());
            return;
        }
        sender.sendMessage("§cUsage: /mythic cooldown reset <player> [ability|all], /mythic cooldown resetall");
    }

    private void handleRitual(CommandSender sender, String[] args) {
        RitualManager ritualManager = plugin.getRitualManager();
        if (args.length < 2) { sender.sendMessage("§cUsage: /mythic ritual <start|status|cancel>"); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                if (args.length < 4) { sender.sendMessage("§cUsage: /mythic ritual start <player> <itemKey> [x y z]"); return; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return; }
                ItemKey key = ItemKey.fromInput(args[3]).orElse(null);
                if (key == null) { sender.sendMessage("§cInvalid item key"); return; }
                Location loc = target.getLocation();
                if (args.length >= 7) {
                    try {
                        loc = new Location(target.getWorld(), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]));
                    } catch (NumberFormatException ignored) {
                        sender.sendMessage("§cInvalid coordinates.");
                        return;
                    }
                }
                sender.sendMessage("§a" + ritualManager.start(target, key, loc));
            }
            case "status" -> {
                if (args.length == 3) {
                    Player target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) { sender.sendMessage("§cPlayer not found."); return; }
                    sender.sendMessage(ritualManager.get(target.getUniqueId()) == null ? "§eNo active ritual." : "§aRitual active.");
                } else {
                    sender.sendMessage("§eActive rituals: " + ritualManager.getAll().size());
                }
            }
            case "cancel" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /mythic ritual cancel <player>"); return; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return; }
                ritualManager.cancel(target.getUniqueId());
                sender.sendMessage("§aRitual cancelled.");
            }
            default -> sender.sendMessage("§cUnknown ritual command.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("mythic.admin")) return List.of();
        if (args.length == 1) return List.of("give", "cooldown", "ritual", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("cooldown")) return List.of("reset", "resetall", "clearall");
        if (args.length == 2 && args[0].equalsIgnoreCase("ritual")) return List.of("start", "status", "cancel");
        if (args.length == 3 && List.of("give", "ritual").contains(args[0].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("give") || (args[0].equalsIgnoreCase("ritual") && args[1].equalsIgnoreCase("start")))) {
            return Arrays.stream(ItemKey.values()).map(ItemKey::key).toList();
        }
        return List.of();
    }
}
