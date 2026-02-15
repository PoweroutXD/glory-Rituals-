package com.mythicrelics.manager;

import com.mythicrelics.MythicRelicsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final MythicRelicsPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CooldownManager(MythicRelicsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cooldowns.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        for (String uuidStr : yaml.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException ex) { continue; }
            Map<String, Long> map = new HashMap<>();
            for (String ability : yaml.getConfigurationSection(uuidStr).getKeys(false)) {
                map.put(ability, yaml.getLong(uuidStr + "." + ability));
            }
            cooldowns.put(uuid, map);
        }
    }

    public void save() {
        yaml.getKeys(false).forEach(k -> yaml.set(k, null));
        for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
            String base = entry.getKey().toString();
            for (Map.Entry<String, Long> cd : entry.getValue().entrySet()) {
                yaml.set(base + "." + cd.getKey(), cd.getValue());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed saving cooldowns.yml: " + e.getMessage());
        }
    }

    public boolean isCooling(UUID uuid, String ability) {
        return getRemaining(uuid, ability) > 0;
    }

    public long getRemaining(UUID uuid, String ability) {
        long until = cooldowns.getOrDefault(uuid, Map.of()).getOrDefault(ability, 0L);
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public void start(UUID uuid, String ability, long seconds) {
        cooldowns.computeIfAbsent(uuid, u -> new HashMap<>()).put(ability, System.currentTimeMillis() + (seconds * 1000L));
    }

    public void reset(UUID uuid, String ability) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map != null) {
            if ("all".equalsIgnoreCase(ability)) map.clear();
            else map.remove(ability);
        }
    }

    public void resetAll() {
        cooldowns.clear();
    }
}
