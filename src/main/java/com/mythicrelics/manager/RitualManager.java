package com.mythicrelics.manager;

import com.mythicrelics.MythicRelicsPlugin;
import com.mythicrelics.item.ItemFactory;
import com.mythicrelics.model.ItemKey;
import com.mythicrelics.util.TimeUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RitualManager {
    private final MythicRelicsPlugin plugin;
    private final ItemFactory itemFactory;
    private final File file;
    private final YamlConfiguration yaml;

    private final Map<UUID, RitualData> activeByPlayer = new HashMap<>();
    private BukkitTask task;

    public RitualManager(MythicRelicsPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.file = new File(plugin.getDataFolder(), "rituals.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void startTicker() { task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 10L); }
    public void stopTicker() { if (task != null) task.cancel(); }

    public boolean hasGlobalActive() { return !activeByPlayer.isEmpty(); }
    public RitualData get(UUID uuid) { return activeByPlayer.get(uuid); }
    public Collection<RitualData> getAll() { return activeByPlayer.values(); }

    public String start(Player owner, ItemKey item, Location loc) {
        if (owner.getWorld().getEnvironment() != World.Environment.NORMAL) return "Rituals can only run in the Overworld.";
        if (activeByPlayer.containsKey(owner.getUniqueId())) return "Player already has an active ritual.";
        if (plugin.getConfig().getBoolean("globalSingleRitual", true) && hasGlobalActive()) return "Another ritual is already active globally.";

        long minutes = plugin.getConfig().getLong("ritual-duration-minutes." + item.key(), plugin.getConfig().getLong("ritual-duration-minutes.default", 35));
        long endAt = System.currentTimeMillis() + (minutes * 60_000L);

        RitualData data = new RitualData(owner.getUniqueId(), owner.getName(), item, loc.clone(), endAt);
        spawnDisplay(data);
        data.bar = BossBar.bossBar(Component.text("§6Ritual: " + item.displayName() + " §f" + TimeUtil.formatCooldown(endAt - System.currentTimeMillis())), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        owner.showBossBar(data.bar);
        activeByPlayer.put(owner.getUniqueId(), data);
        save();
        return "Started ritual for " + owner.getName() + " at " + blockString(loc);
    }

    private String blockString(Location loc) {
        return "X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ();
    }

    public void cancel(UUID ownerId) {
        RitualData data = activeByPlayer.remove(ownerId);
        if (data == null) return;
        cleanup(data);
        save();
    }

    private void complete(RitualData data) {
        Player owner = Bukkit.getPlayer(data.owner);
        if (data.displayId != null) {
            Entity e = Bukkit.getEntity(data.displayId);
            if (e != null) e.remove();
        }
        World world = data.location.getWorld();
        if (world != null) {
            Item drop = world.dropItem(data.location.clone().add(0.5, 1.0, 0.5), itemFactory.create(data.item));
            drop.setUnlimitedLifetime(false);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, data.location.clone().add(0.5, 1.2, 0.5), 1);
            world.spawnParticle(Particle.PORTAL, data.location.clone().add(0.5, 1.2, 0.5), 80, 1.2, 1.2, 1.2, 0.2);
            world.spawnParticle(Particle.ENCHANT, data.location.clone().add(0.5, 1.2, 0.5), 80, 1.2, 1.2, 1.2, 0.2);
            world.spawnParticle(Particle.END_ROD, data.location.clone().add(0.5, 1.2, 0.5), 60, 1.2, 1.2, 1.2, 0.04);
            world.spawnParticle(Particle.ELECTRIC_SPARK, data.location.clone().add(0.5, 1.2, 0.5), 40, 1.2, 1.2, 1.2, 0.08);
            world.playSound(data.location, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.1f);
            world.playSound(data.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
        if (owner != null && data.bar != null) owner.hideBossBar(data.bar);
    }

    private void cleanup(RitualData data) {
        Player owner = Bukkit.getPlayer(data.owner);
        if (owner != null && data.bar != null) owner.hideBossBar(data.bar);
        if (data.displayId != null) {
            Entity e = Bukkit.getEntity(data.displayId);
            if (e != null) e.remove();
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<RitualData> it = activeByPlayer.values().iterator(); it.hasNext(); ) {
            RitualData data = it.next();
            Player owner = Bukkit.getPlayer(data.owner);
            long rem = Math.max(0, data.endAt - now);
            if (owner != null && data.bar != null) {
                float progress = Math.max(0f, Math.min(1f, rem / (float) (getDurationMillis(data.item))));
                data.bar.progress(progress);
                data.bar.name(Component.text("§6Ritual " + data.item.displayName() + " §f" + TimeUtil.formatCooldown(rem)));
                owner.sendActionBar(Component.text("§eDrop at: X:" + data.location.getBlockX() + " Y:" + data.location.getBlockY() + " Z:" + data.location.getBlockZ()));
            }
            animateDisplay(data, now);
            spawnAmbient(data);
            if (!data.oneMinuteWarned && rem <= 60_000L && rem > 0) {
                data.oneMinuteWarned = true;
                World world = data.location.getWorld();
                if (world != null) {
                    world.playSound(data.location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.5f);
                    world.playSound(data.location, Sound.BLOCK_BEACON_POWER_SELECT, 0.9f, 1.2f);
                    Location beam = data.location.clone().add(0.5, 0.2, 0.5);
                    for (double y = 0; y < 5; y += 0.25) {
                        world.spawnParticle(Particle.END_ROD, beam.clone().add(0, y, 0), 3, 0.05, 0.05, 0.05, 0);
                        world.spawnParticle(Particle.ELECTRIC_SPARK, beam.clone().add(0, y, 0), 2, 0.05, 0.05, 0.05, 0);
                    }
                }
            }
            if (rem <= 0) {
                complete(data);
                it.remove();
            }
        }
        save();
    }

    private long getDurationMillis(ItemKey item) {
        long minutes = plugin.getConfig().getLong("ritual-duration-minutes." + item.key(), plugin.getConfig().getLong("ritual-duration-minutes.default", 35));
        return minutes * 60_000L;
    }

    private void spawnAmbient(RitualData data) {
        World world = data.location.getWorld();
        if (world == null) return;
        Location center = data.location.clone().add(0.5, 1.2, 0.5);
        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2 * i) / 12.0;
            double x = Math.cos(angle) * 1.2;
            double z = Math.sin(angle) * 1.2;
            Location p = center.clone().add(x, 0.0, z);
            world.spawnParticle(Particle.PORTAL, p, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.ENCHANT, p, 1, 0, 0, 0, 0);
            if (i % 3 == 0) world.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
            if (i % 4 == 0) world.spawnParticle(Particle.SMOKE, p, 1, 0, 0, 0, 0);
            if (i % 5 == 0) world.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private void spawnDisplay(RitualData data) {
        World world = data.location.getWorld();
        if (world == null) return;
        ItemDisplay display = world.spawn(data.location.clone().add(0.5, 1.2, 0.5), ItemDisplay.class, d -> {
            d.setItemStack(itemFactory.create(data.item));
            d.setBillboard(Display.Billboard.VERTICAL);
            d.setPersistent(true);
            d.setBrightness(new Display.Brightness(15, 15));
        });
        data.displayId = display.getUniqueId();
    }

    private void animateDisplay(RitualData data, long now) {
        if (data.displayId == null) return;
        Entity e = Bukkit.getEntity(data.displayId);
        if (!(e instanceof ItemDisplay display)) {
            spawnDisplay(data);
            return;
        }
        float t = (now % 10_000L) / 1000f;
        float bob = (float) (Math.sin(t * 1.2f) * 0.12f);
        float yaw = t;
        display.setTransformation(new Transformation(new Vector3f(0f, bob, 0f), new AxisAngle4f(yaw, 0, 1, 0), new Vector3f(1.0f, 1.0f, 1.0f), new AxisAngle4f()));
        display.setInterpolationDuration(10);
    }

    public void load() {
        activeByPlayer.clear();
        ConfigurationSection section = yaml.getConfigurationSection("rituals");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                String itemRaw = yaml.getString("rituals." + key + ".item", "voidblade");
                ItemKey item = ItemKey.fromInput(itemRaw).orElse(ItemKey.VOID_BLADE);
                World world = Bukkit.getWorld(UUID.fromString(yaml.getString("rituals." + key + ".world")));
                if (world == null) continue;
                Location loc = new Location(world,
                        yaml.getDouble("rituals." + key + ".x"),
                        yaml.getDouble("rituals." + key + ".y"),
                        yaml.getDouble("rituals." + key + ".z"));
                long endAt = yaml.getLong("rituals." + key + ".endAt");
                RitualData data = new RitualData(owner, yaml.getString("rituals." + key + ".ownerName", "Unknown"), item, loc, endAt);
                String displayUuid = yaml.getString("rituals." + key + ".display");
                if (displayUuid != null) {
                    try { data.displayId = UUID.fromString(displayUuid); } catch (IllegalArgumentException ignored) { }
                }
                if (data.displayId == null || Bukkit.getEntity(data.displayId) == null) spawnDisplay(data);
                Player ownerPlayer = Bukkit.getPlayer(owner);
                data.bar = BossBar.bossBar(Component.text("§6Ritual " + item.displayName()), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
                if (ownerPlayer != null) ownerPlayer.showBossBar(data.bar);
                activeByPlayer.put(owner, data);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed loading ritual " + key + ": " + ex.getMessage());
            }
        }
    }

    public void save() {
        yaml.set("rituals", null);
        for (RitualData data : activeByPlayer.values()) {
            String base = "rituals." + data.owner;
            yaml.set(base + ".ownerName", data.ownerName);
            yaml.set(base + ".item", data.item.key());
            yaml.set(base + ".world", data.location.getWorld().getUID().toString());
            yaml.set(base + ".x", data.location.getX());
            yaml.set(base + ".y", data.location.getY());
            yaml.set(base + ".z", data.location.getZ());
            yaml.set(base + ".endAt", data.endAt);
            yaml.set(base + ".display", data.displayId == null ? null : data.displayId.toString());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed saving rituals.yml: " + e.getMessage());
        }
    }

    public static class RitualData {
        public final UUID owner;
        public final String ownerName;
        public final ItemKey item;
        public final Location location;
        public final long endAt;
        public UUID displayId;
        public boolean oneMinuteWarned;
        public BossBar bar;

        public RitualData(UUID owner, String ownerName, ItemKey item, Location location, long endAt) {
            this.owner = owner;
            this.ownerName = ownerName;
            this.item = item;
            this.location = location;
            this.endAt = endAt;
        }
    }
}
