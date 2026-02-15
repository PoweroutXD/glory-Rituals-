package com.mythicrelics.listener;

import com.mythicrelics.MythicRelicsPlugin;
import com.mythicrelics.item.ItemFactory;
import com.mythicrelics.manager.CooldownManager;
import com.mythicrelics.model.ItemKey;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AbilityListener implements Listener {
    private final MythicRelicsPlugin plugin;
    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;

    private final Map<UUID, Long> dragonRunUntil = new HashMap<>();
    private final Map<UUID, Long> titanUntil = new HashMap<>();
    private final Map<UUID, Long> zeusArmedUntil = new HashMap<>();
    private final Set<UUID> negateFall = new HashSet<>();
    private final Map<UUID, Double> forcedFallDamage = new HashMap<>();
    private final Map<UUID, BukkitTask> luckMaceTimers = new HashMap<>();

    public AbilityListener(MythicRelicsPlugin plugin, ItemFactory itemFactory, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
        startTickers();
    }

    private void startTickers() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID id = player.getUniqueId();
                if (dragonRunUntil.getOrDefault(id, 0L) > now) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    wingVfx(player);
                } else if (dragonRunUntil.containsKey(id)) {
                    dragonRunUntil.remove(id);
                    player.setFlying(false);
                    player.setAllowFlight(false);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
                    player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 60, 0.7, 0.4, 0.7, 0.1);
                }
                if (titanUntil.getOrDefault(id, 0L) > now) {
                    player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 4, 0.4, 0.5, 0.4, 0.05);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.2f, 0.6f);
                } else if (titanUntil.containsKey(id)) {
                    titanUntil.remove(id);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 10, 1));
                    player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 2, 0.3, 0.2, 0.3, 0);
                }
            }
        }, 1L, 10L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemKey key = itemFactory.resolve(hand);
        if (key == null) return;

        switch (key) {
            case VOID_BLADE -> {
                if (player.isSneaking()) dragonBreath(player);
                else dragonRun(player);
            }
            case LUCK_COIN -> {
                if (player.isSneaking()) flipCoin(player);
            }
            case WIND_WAKER -> {
                if (player.isSneaking()) windWaker(player);
                else player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false, true));
            }
            case HERACLES -> {
                if (player.isSneaking()) titanSurge(player);
            }
            case ZEUS -> {
                if (player.isSneaking()) armZeusThrow(player);
            }
            case BAG_OF_WINDS -> {
                if (player.isSneaking()) bagUse(player, hand);
            }
            case CRONOS -> {
                if (player.isSneaking()) cronosDome(player);
            }
            default -> {
            }
        }
    }

    private long cfgCd(String path, long fallback) { return plugin.getConfig().getLong("cooldowns-seconds." + path, fallback); }
    private long cfgDur(String path, long fallback) { return plugin.getConfig().getLong("durations-seconds." + path, fallback); }

    private void dragonRun(Player player) {
        UUID id = player.getUniqueId();
        if (cooldownManager.isCooling(id, "voidblade.dragonrun")) return;
        cooldownManager.start(id, "voidblade.dragonrun", cfgCd("voidblade.dragonrun", 300));
        long dur = cfgDur("voidblade.dragonrun", 33);
        dragonRunUntil.put(id, System.currentTimeMillis() + dur * 1000L);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
    }

    private void wingVfx(Player player) {
        Location base = player.getLocation().add(0, 1.2, 0);
        World world = player.getWorld();
        for (double a = -1.2; a <= 1.2; a += 0.3) {
            world.spawnParticle(Particle.DRAGON_BREATH, base.clone().add(a, Math.abs(a) * 0.2, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.PORTAL, base.clone().add(-a, Math.abs(a) * 0.2, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.clone().add(0, 0, a), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.SMOKE, base.clone().add(0, 0.4, a), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.END_ROD, base.clone().add(a * 0.3, 0.2, a * 0.3), 1, 0, 0, 0, 0);
        }
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
            world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.2f);
        }
    }

    private void dragonBreath(Player player) {
        UUID id = player.getUniqueId();
        if (cooldownManager.isCooling(id, "voidblade.dragonbreath")) return;
        cooldownManager.start(id, "voidblade.dragonbreath", cfgCd("voidblade.dragonbreath", 60));
        Snowball ball = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(1.8));
        ball.getPersistentDataContainer().set(new NamespacedKey(plugin, "dragon_breath"), PersistentDataType.BYTE, (byte) 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
    }

    private void flipCoin(Player player) {
        UUID id = player.getUniqueId();
        if (cooldownManager.isCooling(id, "luckcoin.flip")) return;
        cooldownManager.start(id, "luckcoin.flip", cfgCd("luckcoin.flip", 360));
        boolean good = ThreadLocalRandom.current().nextBoolean();
        if (!good) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 60, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 0));
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 40, 0.6, 0.8, 0.6, 0.02);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 0.7f);
            return;
        }
        player.getWorld().strikeLightningEffect(player.getLocation());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1f);
        ItemStack mace = new ItemStack(Material.MACE);
        mace.addUnsafeEnchantment(Enchantment.WIND_BURST, 1);
        mace.addUnsafeEnchantment(Enchantment.DENSITY, 2);
        player.getInventory().addItem(mace);
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player online = Bukkit.getPlayer(id);
                if (online != null) {
                    removeOneMace(online);
                    online.getInventory().addItem(itemFactory.create(ItemKey.LUCK_COIN));
                }
            }
        }.runTaskLater(plugin, cfgDur("luckcoin.mace", 120) * 20L);
        BukkitTask old = luckMaceTimers.put(id, task);
        if (old != null) old.cancel();
    }

    private void removeOneMace(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == Material.MACE) {
                if (it.getAmount() > 1) it.setAmount(it.getAmount() - 1);
                else inv.setItem(i, null);
                return;
            }
        }
    }

    private void windWaker(Player player) {
        UUID id = player.getUniqueId();
        if (cooldownManager.isCooling(id, "windwaker.gale")) return;
        cooldownManager.start(id, "windwaker.gale", cfgCd("windwaker.gale", 420));
        Location c = player.getLocation();
        World w = player.getWorld();
        for (LivingEntity le : w.getLivingEntities()) {
            if (le == player) continue;
            if (le.getLocation().distanceSquared(c) <= 20 * 20) {
                Vector knock = le.getLocation().toVector().subtract(c.toVector()).normalize().multiply(2.2).setY(0.45);
                le.setVelocity(knock);
            }
        }
        List<Player> nearby = w.getPlayers().stream().filter(p -> p != player && p.getLocation().distanceSquared(c) <= 64).toList();
        if (!nearby.isEmpty()) {
            Player chosen = nearby.get(ThreadLocalRandom.current().nextInt(nearby.size()));
            chosen.setVelocity(new Vector(0, 3.2, 0));
            forcedFallDamage.put(chosen.getUniqueId(), 9.0);
        }
        for (int i = 0; i < 80; i++) {
            double angle = Math.PI * 2 * i / 80.0;
            Location p = c.clone().add(Math.cos(angle) * 3.0, 1.1, Math.sin(angle) * 3.0);
            w.spawnParticle(Particle.CLOUD, p, 1, 0, 0, 0, 0);
            if (i % 2 == 0) w.spawnParticle(Particle.WHITE_ASH, p, 1, 0, 0, 0, 0);
            if (i % 3 == 0) w.spawnParticle(Particle.SWEEP_ATTACK, p, 1, 0, 0, 0, 0);
            if (i % 5 == 0) w.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
            if (i % 7 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0, 0, 0, 0);
        }
        w.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1f, 0.8f);
        w.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 1f, 0.7f);
        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.6f);
    }

    private void titanSurge(Player player) {
        UUID id = player.getUniqueId();
        if (cooldownManager.isCooling(id, "heracles.titansurge")) return;
        cooldownManager.start(id, "heracles.titansurge", cfgCd("heracles.titansurge", 300));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) (cfgDur("heracles.titansurge", 30) * 20), 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (cfgDur("heracles.titansurge", 30) * 20), 1));
        titanUntil.put(id, System.currentTimeMillis() + cfgDur("heracles.titansurge", 30) * 1000L);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 70, 0.8, 1, 0.8, 0.2);
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation(), 20, 0.6, 0.8, 0.6, 0.01);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3, 0.5, 0.3, 0.5, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.2f);
    }

    private void armZeusThrow(Player player) {
        zeusArmedUntil.put(player.getUniqueId(), System.currentTimeMillis() + cfgDur("zeus.armwindow", 10) * 1000L);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
    }

    private void bagUse(Player player, ItemStack bag) {
        UUID id = player.getUniqueId();
        if (cooldownManager.isCooling(id, "bagofwinds.gust")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return;
        }
        cooldownManager.start(id, "bagofwinds.gust", cfgCd("bagofwinds.gust", 34));
        Vector dir = player.getLocation().getDirection().normalize();
        player.setVelocity(new Vector(dir.getX() * 1.8, 1.7, dir.getZ() * 1.8));
        negateFall.add(id);
        World w = player.getWorld();
        w.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.3f);
        w.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.9f, 1.2f);
        w.spawnParticle(Particle.CLOUD, player.getLocation(), 40, 0.7, 0.3, 0.7, 0.1);
        w.spawnParticle(Particle.WHITE_ASH, player.getLocation(), 30, 0.7, 0.4, 0.7, 0.1);

        ItemMeta meta = bag.getItemMeta();
        int uses = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "bag_uses"), PersistentDataType.INTEGER, 4);
        uses--;
        if (uses <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
        } else {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bag_uses"), PersistentDataType.INTEGER, uses);
            bag.setItemMeta(meta);
        }
    }

    private void cronosDome(Player player) {
        UUID id = player.getUniqueId();
        if (cooldownManager.isCooling(id, "cronos.timeprison")) return;
        cooldownManager.start(id, "cronos.timeprison", cfgCd("cronos.timeprison", 180));
        Location center = player.getLocation();
        int radius = 15;
        Set<Location> placed = new HashSet<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist >= radius - 0.8 && dist <= radius + 0.8) {
                        Location l = center.clone().add(x, y, z);
                        if (l.getBlock().getType() == Material.AIR) {
                            l.getBlock().setType(Material.BLACK_CONCRETE);
                            placed.add(l.toBlockLocation());
                        }
                    }
                }
            }
        }
        plugin.getCronosDomeBlocks().addAll(placed);
        player.getWorld().spawnParticle(Particle.EXPLOSION, center, 4, 0.8, 0.8, 0.8, 0);
        player.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.6f);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Location l : placed) {
                    if (l.getBlock().getType() == Material.BLACK_CONCRETE) l.getBlock().setType(Material.AIR);
                }
                plugin.getCronosDomeBlocks().removeAll(placed);
            }
        }.runTaskLater(plugin, cfgDur("cronos.timeprison", 20) * 20L);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;
        ItemKey key = itemFactory.resolve(player.getInventory().getItemInMainHand());
        if (key != ItemKey.ZEUS) return;
        long armedUntil = zeusArmedUntil.getOrDefault(player.getUniqueId(), 0L);
        if (armedUntil < System.currentTimeMillis()) {
            event.setCancelled(true);
            player.sendMessage("§cEmpowered throw not armed. Sneak + right click first.");
            return;
        }
        trident.getPersistentDataContainer().set(new NamespacedKey(plugin, "zeus_empowered"), PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball snowball) {
            if (snowball.getPersistentDataContainer().has(new NamespacedKey(plugin, "dragon_breath"), PersistentDataType.BYTE)) {
                Location hit = snowball.getLocation();
                World w = hit.getWorld();
                w.spawnParticle(Particle.DRAGON_BREATH, hit, 80, 1.2, 0.6, 1.2, 0.05);
                w.spawnParticle(Particle.ASH, hit, 30, 1, 0.4, 1, 0.02);
                w.playSound(hit, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1.1f);
                for (Entity e : w.getNearbyEntities(hit, 2.4, 2.4, 2.4)) {
                    if (e instanceof LivingEntity living && e != snowball.getShooter()) {
                        living.damage(5.0, (Entity) snowball.getShooter());
                        living.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                    }
                }
            }
        }
        if (event.getEntity() instanceof Trident trident && event.getHitEntity() instanceof LivingEntity living && trident.getShooter() instanceof Player player) {
            if (trident.getPersistentDataContainer().has(new NamespacedKey(plugin, "zeus_empowered"), PersistentDataType.BYTE)) {
                player.getWorld().strikeLightningEffect(living.getLocation());
                player.getWorld().playSound(living.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, living.getLocation().add(0,1,0), 50, 0.8, 1, 0.8, 0.1);
                player.getWorld().spawnParticle(Particle.END_ROD, living.getLocation().add(0,1,0), 20, 0.6, 0.6, 0.6, 0.05);
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, living.getLocation().add(0,1,0), 15, 0.3, 0.4, 0.3, 0.01);
                living.damage(10.0, player);
                cooldownManager.start(player.getUniqueId(), "zeus.empoweredthrow", cfgCd("zeus.empoweredthrow", 30));
                zeusArmedUntil.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemKey key = itemFactory.resolve(player.getInventory().getItemInMainHand());
        if (key == ItemKey.VOID_BLADE || key == ItemKey.CRONOS) {
            event.setDamage(event.getDamage() + 6.0);
        }
        if (key == ItemKey.CRONOS && event.getEntity() instanceof LivingEntity living) {
            if (ThreadLocalRandom.current().nextDouble() < 0.25) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 25, 0));
            }
        }
        if (key == ItemKey.ZEUS && event.getEntity() instanceof LivingEntity living) {
            if (ThreadLocalRandom.current().nextDouble() < 0.25) {
                player.getWorld().strikeLightningEffect(living.getLocation());
                living.damage(2.0, player);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (itemFactory.resolve(p.getInventory().getItemInMainHand()) == ItemKey.WIND_WAKER) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false, true));
        }
        if (!plugin.getCronosDomeBlocks().isEmpty()) {
            boolean inside = plugin.getCronosDomeBlocks().stream().anyMatch(l -> l.getWorld().equals(p.getWorld()) && l.distanceSquared(p.getLocation()) <= 15 * 15 + 2);
            if (inside && itemFactory.resolve(p.getInventory().getItemInMainHand()) == ItemKey.CRONOS) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false, true));
            } else if (inside) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, false, true));
            }
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        UUID id = player.getUniqueId();
        if (negateFall.remove(id)) {
            event.setCancelled(true);
            return;
        }
        if (forcedFallDamage.containsKey(id)) {
            event.setDamage(forcedFallDamage.remove(id));
        }
    }

    @EventHandler
    public void onPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack off = player.getInventory().getItemInOffHand();
        if (itemFactory.resolve(off) != ItemKey.GOLDEN_FLEECE) return;
        UUID id = player.getUniqueId();
        if (cooldownManager.isCooling(id, "goldenfleece.resurrect")) {
            event.setCancelled(true);
            return;
        }
        cooldownManager.start(id, "goldenfleece.resurrect", cfgCd("goldenfleece.resurrect", 3600));
        Location popLoc = player.getLocation().clone();
        ItemStack fleeceCopy = off.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getInventory().setItemInOffHand(fleeceCopy);
            Location tp = player.getBedSpawnLocation();
            if (tp == null) tp = player.getWorld().getSpawnLocation();
            for (int i = 0; i <= 8; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(popLoc, item);
                    player.getInventory().setItem(i, null);
                }
            }
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR) player.getWorld().dropItemNaturally(popLoc, armor);
            }
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.teleport(tp);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 3));
        }, 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // Bag should drop on death; avoid keep inventory removing it
        event.setKeepInventory(false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BukkitTask task = luckMaceTimers.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // no-op; cooldowns persisted by manager
    }

    @EventHandler
    public void onDomeBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (plugin.getCronosDomeBlocks().contains(event.getBlock().getLocation().toBlockLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.getCronosDomeBlocks().contains(block.getLocation().toBlockLocation()));
    }
}
