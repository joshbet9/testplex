package au.com.testplex.arcadeports;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Per-match real block utility and Paper-native player decoys for Death Tag Trickster.
 * Nothing persists beyond a round. Source/flowing water is bounded and restored.
 */
final class DeathTagTricksterMechanics {
    static final long WATER_ARM_MILLIS = 5_000L;
    static final long WATER_COOLDOWN_MILLIS = 30_000L;
    static final long WATER_DURATION_TICKS = 100L;
    static final long WEB_DURATION_TICKS = 160L;
    static final long DECOY_DURATION_TICKS = 200L;
    private static final int MAX_WATER_BLOCKS_PER_SOURCE = 128;

    private final Map<UUID, Long> waterArmedUntil = new HashMap<>();
    private final Map<UUID, Long> waterReadyAt = new HashMap<>();
    private final Map<Block, BlockData> waterOriginals = new LinkedHashMap<>();
    private final Map<Block, Block> waterRoots = new HashMap<>();
    private final Map<Block, Set<Block>> waterGroups = new HashMap<>();
    private final Map<Block, BlockData> webOriginals = new LinkedHashMap<>();
    private final Set<UUID> usedSmoke = new HashSet<>();
    private final Set<UUID> usedDecoy = new HashSet<>();
    private final Map<UUID, Mannequin> decoys = new HashMap<>();
    private final Map<UUID, BukkitTask> decoyTasks = new HashMap<>();
    private boolean closed;

    static boolean canPlaceUtilityBlock(Material type) {
        return type.isAir();
    }

    static boolean armedClutch(long now, long armedUntil, long readyAt) {
        return now < armedUntil && now >= readyAt;
    }

    void useWater(JavaPlugin plugin, Player runner, Block clicked, BlockFace face) {
        if (closed) return;
        UUID owner = runner.getUniqueId();
        long now = System.currentTimeMillis();
        long readyAt = waterReadyAt.getOrDefault(owner, 0L);
        if (now < readyAt) {
            runner.sendActionBar(ChatColor.RED + "Water Bucket ready in "
                    + Math.max(1L, (readyAt - now + 999L) / 1000L) + "s");
            return;
        }
        if (clicked != null && face != null) {
            Block target = clicked.getRelative(face);
            if (placeWater(plugin, target)) {
                waterArmedUntil.remove(owner);
                waterReadyAt.put(owner, now + WATER_COOLDOWN_MILLIS);
                runner.setCooldown(Material.WATER_BUCKET, (int) (WATER_COOLDOWN_MILLIS / 50L));
                runner.playSound(runner.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.8F, 1.0F);
                return;
            }
        }
        // Pre-arming an intentional right-click avoids losing the clutch to late packets.
        waterArmedUntil.put(owner, now + WATER_ARM_MILLIS);
        runner.sendActionBar(ChatColor.AQUA + "Water armed for 5s - real water on landing");
        runner.playSound(runner.getLocation(), Sound.ITEM_BUCKET_FILL, 0.5F, 1.2F);
    }

    boolean catchArmedClutch(JavaPlugin plugin, Player runner) {
        if (closed) return false;
        UUID owner = runner.getUniqueId();
        long now = System.currentTimeMillis();
        if (!armedClutch(now, waterArmedUntil.getOrDefault(owner, 0L),
                waterReadyAt.getOrDefault(owner, 0L))) return false;
        Block feet = runner.getLocation().getBlock();
        if (!feet.getRelative(BlockFace.DOWN).getType().isSolid() || !placeWater(plugin, feet)) {
            return false;
        }
        waterArmedUntil.remove(owner);
        waterReadyAt.put(owner, now + WATER_COOLDOWN_MILLIS);
        runner.setCooldown(Material.WATER_BUCKET, (int) (WATER_COOLDOWN_MILLIS / 50L));
        runner.setFallDistance(0.0F);
        runner.sendActionBar(ChatColor.AQUA + "MLG Water Clutch!");
        runner.playSound(runner.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.9F, 1.2F);
        return true;
    }

    private boolean placeWater(JavaPlugin plugin, Block block) {
        if (closed || block == null || !canPlaceUtilityBlock(block.getType())
                || waterOriginals.containsKey(block) || webOriginals.containsKey(block)) return false;
        waterOriginals.put(block, block.getBlockData());
        waterRoots.put(block, block);
        Set<Block> group = new LinkedHashSet<>();
        group.add(block);
        waterGroups.put(block, group);
        block.setType(Material.WATER, true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> restoreWaterSource(block), WATER_DURATION_TICKS);
        return true;
    }

    void trackWaterFlow(BlockFromToEvent event) {
        if (closed || event.isCancelled()) return;
        Block root = waterRoots.get(event.getBlock());
        if (root == null) return;
        Block to = event.getToBlock();
        Set<Block> group = waterGroups.get(root);
        if (group == null) return;
        if (waterRoots.containsKey(to)) return;
        // Prevent water from damaging map decorations or expanding indefinitely.
        if (!canPlaceUtilityBlock(to.getType()) || group.size() >= MAX_WATER_BLOCKS_PER_SOURCE
                || to.getLocation().distanceSquared(root.getLocation()) > 64.0D) {
            event.setCancelled(true);
            return;
        }
        waterOriginals.put(to, to.getBlockData());
        waterRoots.put(to, root);
        group.add(to);
    }

    private void restoreWaterSource(Block root) {
        Set<Block> group = waterGroups.remove(root);
        if (group == null) return;
        for (Block block : new ArrayList<>(group).reversed()) {
            waterRoots.remove(block);
            BlockData original = waterOriginals.remove(block);
            if (original != null && block.getType() == Material.WATER) block.setBlockData(original, false);
        }
    }

    boolean placeWeb(JavaPlugin plugin, Player runner, Block clicked, BlockFace face) {
        if (closed) return false;
        Block target;
        if (clicked != null && face != null) {
            target = clicked.getRelative(face);
        } else {
            Vector behind = runner.getLocation().getDirection().setY(0.0D);
            if (behind.lengthSquared() < 0.001D) behind.setZ(1.0D);
            target = runner.getLocation().clone().add(behind.normalize().multiply(-1.4D)).getBlock();
        }
        if (!canPlaceUtilityBlock(target.getType()) || waterOriginals.containsKey(target)
                || webOriginals.containsKey(target)) {
            runner.sendActionBar(ChatColor.YELLOW + "No room to place a cobweb");
            return false;
        }
        webOriginals.put(target, target.getBlockData());
        target.setType(Material.COBWEB, false);
        ItemStack held = runner.getInventory().getItemInMainHand();
        if (held.getAmount() == 1) runner.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
        runner.playSound(runner.getLocation(), Sound.BLOCK_COBWEB_PLACE, 0.8F, 1.0F);
        Bukkit.getScheduler().runTaskLater(plugin, () -> restoreWeb(target), WEB_DURATION_TICKS);
        return true;
    }

    private void restoreWeb(Block block) {
        BlockData old = webOriginals.remove(block);
        if (old != null && block.getType() == Material.COBWEB) block.setBlockData(old, false);
    }

    boolean useSmoke(Player runner, Iterable<Player> chasers) {
        if (closed || !usedSmoke.add(runner.getUniqueId())) return false;
        consumeOne(runner);
        int cloakTicks = Math.max(1, (int) (SneakyAssassinsRules.smokeCloakDurationMillis() / 50L));
        int blindTicks = Math.max(1, (int) (SneakyAssassinsRules.smokeBlindnessDurationMillis() / 50L));
        runner.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, cloakTicks,
                0, false, false, false));
        for (Player chaser : chasers) {
            if (chaser != null && chaser.isOnline() && chaser.getWorld() == runner.getWorld()
                    && chaser.getLocation().distanceSquared(runner.getLocation())
                            <= SneakyAssassinsRules.smokeRadiusSquared()) {
                chaser.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                        blindTicks, 0, false, false, true));
            }
        }
        Location smoke = runner.getLocation().add(0.0D, 1.0D, 0.0D);
        runner.getWorld().spawnParticle(Particle.CLOUD, smoke, 38, 1.0D, 0.6D, 1.0D, 0.08D);
        runner.getWorld().spawnParticle(Particle.LARGE_SMOKE, smoke, 26, 0.85D, 0.7D, 0.85D, 0.04D);
        runner.getWorld().playSound(runner.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.2F, 0.65F);
        return true;
    }

    boolean deployDecoy(JavaPlugin plugin, Player runner) {
        if (closed || usedDecoy.contains(runner.getUniqueId())) return false;
        Location origin = runner.getLocation().clone();
        if (!origin.getBlock().isPassable() || !origin.clone().add(0, 1, 0).getBlock().isPassable()) return false;
        Mannequin mannequin = runner.getWorld().spawn(origin, Mannequin.class);
        // Mannequins are actual full-body player models in Paper 26.3, no packet plugin needed.
        mannequin.setProfile(ResolvableProfile.resolvableProfile(runner.getPlayerProfile()));
        mannequin.setDescription(null);
        mannequin.setCustomName(runner.getName());
        mannequin.setCustomNameVisible(true);
        mannequin.setCollidable(false);
        mannequin.setImmovable(true);
        mannequin.setPersistent(false);
        EntityEquipment equipment = mannequin.getEquipment();
        equipment.setHelmet(runner.getInventory().getHelmet() == null ? null : runner.getInventory().getHelmet().clone());
        equipment.setChestplate(runner.getInventory().getChestplate() == null ? null : runner.getInventory().getChestplate().clone());
        equipment.setLeggings(runner.getInventory().getLeggings() == null ? null : runner.getInventory().getLeggings().clone());
        equipment.setBoots(runner.getInventory().getBoots() == null ? null : runner.getInventory().getBoots().clone());
        equipment.setItemInMainHand(runner.getInventory().getItemInMainHand().clone());
        UUID owner = runner.getUniqueId();
        decoys.put(owner, mannequin);
        usedDecoy.add(owner);
        consumeOne(runner);
        Vector forward = origin.getDirection().setY(0.0D);
        if (forward.lengthSquared() < 0.001D) forward.setZ(1.0D);
        forward.normalize().multiply(0.32D);
        final int[] ticks = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (closed || !mannequin.isValid() || !runner.isOnline() || ticks[0] >= DECOY_DURATION_TICKS) {
                removeDecoy(owner);
                return;
            }
            ticks[0] += 2;
            Location next = mannequin.getLocation().add(forward);
            if (next.getBlock().isPassable() && next.clone().add(0, 1, 0).getBlock().isPassable()
                    && next.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                mannequin.teleport(next);
            }
        }, 2L, 2L);
        decoyTasks.put(owner, task);
        runner.getWorld().playSound(origin, Sound.ENTITY_EGG_THROW, 0.8F, 0.9F);
        return true;
    }

    boolean handleDecoyDamage(EntityDamageEvent event, Set<UUID> tagged) {
        if (!(event.getEntity() instanceof Mannequin mannequin) || !decoys.containsValue(mannequin)) return false;
        event.setCancelled(true);
        if (event instanceof EntityDamageByEntityEvent hit && hit.getDamager() instanceof Player attacker
                && tagged.contains(attacker.getUniqueId())) {
            UUID owner = null;
            for (Map.Entry<UUID, Mannequin> entry : decoys.entrySet()) {
                if (entry.getValue().equals(mannequin)) { owner = entry.getKey(); break; }
            }
            if (owner != null) {
                Location location = mannequin.getLocation().add(0, 1, 0);
                mannequin.getWorld().spawnParticle(Particle.LARGE_SMOKE, location, 22, 0.4D, 0.7D, 0.4D, 0.04D);
                mannequin.getWorld().playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.7F, 1.4F);
                removeDecoy(owner);
            }
        }
        return true;
    }

    void removeDecoy(UUID owner) {
        BukkitTask task = decoyTasks.remove(owner);
        if (task != null) task.cancel();
        Mannequin mannequin = decoys.remove(owner);
        if (mannequin != null && mannequin.isValid()) mannequin.remove();
    }

    private static void consumeOne(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
    }

    void close() {
        if (closed) return;
        closed = true;
        for (Block source : new ArrayList<>(waterGroups.keySet())) restoreWaterSource(source);
        for (Block block : new ArrayList<>(webOriginals.keySet())) restoreWeb(block);
        for (UUID owner : new ArrayList<>(decoys.keySet())) removeDecoy(owner);
        waterArmedUntil.clear();
    }
}
