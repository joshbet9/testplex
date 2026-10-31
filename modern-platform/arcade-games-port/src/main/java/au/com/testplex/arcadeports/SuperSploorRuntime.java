package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

/**
 * First playable Super Sploor core: territory, Sprayer, four-mark splats,
 * respawns and friendly-paint Squid movement. The runtime is hosted by the
 * shared Testplex Arcade manager and never creates its own Bukkit lifecycle.
 */
public final class SuperSploorRuntime implements Listener {
    public record PlayerMatchStats(long territoryPainted, long enemyTerritoryReclaimed,
            int splats, int deaths, int assists, double squidTravelBlocks,
            double wallClimbBlocks, int specialsUsed) { }
    public record TerritorySummary(int redUnits, int blueUnits, int totalUnits) { }

    private static final class MutableStats {
        long territoryPainted;
        long enemyTerritoryReclaimed;
        int splats;
        int deaths;
        int assists;
        double squidTravelBlocks;
        double wallClimbBlocks;
        int specialsUsed;

        PlayerMatchStats snapshot() {
            return new PlayerMatchStats(territoryPainted, enemyTerritoryReclaimed, splats, deaths,
                    assists, squidTravelBlocks, wallClimbBlocks, specialsUsed);
        }
    }

    private static final int RED = 0;
    private static final int BLUE = 1;
    private static final int PRIMARY_SLOT = 0;
    private static final int SQUID_SLOT = 7;

    private final JavaPlugin plugin;
    private final NamespacedKey projectileKey;
    private final Map<UUID, Player> players = new LinkedHashMap<>();
    private final Map<UUID, Integer> teams = new HashMap<>();
    private final Map<UUID, Location> spawns = new HashMap<>();
    private final Map<UUID, Integer> ink = new HashMap<>();
    private final Map<UUID, Integer> marks = new HashMap<>();
    private final Map<UUID, Long> lastShotAt = new HashMap<>();
    private final Map<UUID, Long> lastInkRegenAt = new HashMap<>();
    private final Map<UUID, Long> lastHitAt = new HashMap<>();
    private final Map<UUID, Long> nextMarkRecoveryAt = new HashMap<>();
    private final Map<UUID, Long> spawnProtectedUntil = new HashMap<>();
    private final Map<UUID, Long> respawnAt = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, MutableStats> playerStats = new HashMap<>();
    private final Map<UUID, Set<UUID>> contributors = new HashMap<>();
    private final Set<UUID> squidMode = new HashSet<>();
    private final Set<UUID> activeProjectiles = new HashSet<>();
    private final Map<UUID, UUID> projectileOwners = new HashMap<>();

    private SuperSploorEngine engine;
    private World world;
    private Scoreboard scoreboard;
    private Objective objective;
    private final List<String> sidebarEntries = new ArrayList<>();
    private IntConsumer finishMatch;
    private long matchEndsAt;
    private long overtimeEndsAt;
    private boolean overtime;
    private boolean running;
    private long lastTickAt;

    public SuperSploorRuntime(JavaPlugin plugin) {
        this.plugin = plugin;
        this.projectileKey = new NamespacedKey(plugin, "super-sploor-projectile");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean isRunning() { return running; }

    public Map<UUID, PlayerMatchStats> snapshotStats() {
        Map<UUID, PlayerMatchStats> copy = new HashMap<>();
        for (Map.Entry<UUID, MutableStats> entry : playerStats.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().snapshot());
        }
        return Map.copyOf(copy);
    }

    public TerritorySummary snapshotTerritory() {
        if (engine == null) return new TerritorySummary(0, 0, 0);
        return new TerritorySummary(engine.redUnits(), engine.blueUnits(), engine.totalUnits());
    }

    public int leadingTeam() {
        if (engine == null) return -1;
        return SuperSploorRules.leadingTeam(engine.redUnits(), engine.blueUnits());
    }

    public void start(SuperSploorEngine engine, Map<UUID, Player> participants,
            Map<UUID, Integer> participantTeams, Map<UUID, Location> participantSpawns,
            IntConsumer finishMatch) {
        stop();
        if (engine == null || participants == null || participantTeams == null
                || participantSpawns == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Super Sploor requires a validated map and participants.");
        }
        this.engine = engine;
        this.world = participants.values().iterator().next().getWorld();
        this.finishMatch = finishMatch;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Player> entry : participants.entrySet()) {
            UUID id = entry.getKey();
            Player player = entry.getValue();
            if (player == null || !player.isOnline()) continue;
            int team = participantTeams.getOrDefault(id, -1);
            Location spawn = participantSpawns.get(id);
            if ((team != RED && team != BLUE) || spawn == null || spawn.getWorld() != world) {
                throw new IllegalArgumentException("Super Sploor participant has no valid team or authored spawn: " + id);
            }
            players.put(id, player);
            teams.put(id, team);
            spawns.put(id, spawn.clone());
            ink.put(id, SuperSploorRules.MAX_INK);
            marks.put(id, 0);
            lastShotAt.put(id, now - SuperSploorRules.PRIMARY_SHOT_INTERVAL_MILLIS);
            lastInkRegenAt.put(id, now);
            spawnProtectedUntil.put(id, now + SuperSploorRules.SPAWN_PROTECTION_MILLIS);
            kills.put(id, 0);
            playerStats.put(id, new MutableStats());
            contributors.put(id, new HashSet<>());
            giveStarterLoadout(player, team);
            player.setGameMode(GameMode.ADVENTURE);
            player.setInvulnerable(true);
            updateInkBar(player, id);
        }
        if (players.isEmpty()) throw new IllegalArgumentException("Super Sploor has no online participants.");
        this.matchEndsAt = now + SuperSploorRules.ROUND_MILLIS;
        this.overtimeEndsAt = 0L;
        this.lastTickAt = now;
        this.overtime = false;
        this.running = true;
        setupScoreboard();
        announce(ChatColor.GOLD + "" + ChatColor.BOLD
                + "Super Sploor! " + ChatColor.RED + "Red" + ChatColor.GRAY + " vs "
                + ChatColor.AQUA + "Blue" + ChatColor.GRAY + " — paint the most territory.");
        for (Player player : players.values()) {
            player.sendMessage(ChatColor.YELLOW + "Right-click your Splattershot to paint. Select the Ink Sac on slot 8 while standing on friendly paint to enter Squid Mode.");
        }
        updateScoreboard(now);
    }

    public void tick(long now) {
        if (!running || engine == null) return;
        lastTickAt = now;
        for (UUID id : new ArrayList<>(players.keySet())) {
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            if (respawnAt.containsKey(id)) {
                if (now >= respawnAt.get(id)) respawn(id, now);
                continue;
            }

            int currentInk = ink.getOrDefault(id, SuperSploorRules.MAX_INK);
            boolean inFriendlyPaint = engine.isFriendlySurface(player.getLocation(), teams.getOrDefault(id, -1));
            boolean squid = squidMode.contains(id);
            if (squid && !inFriendlyPaint) {
                exitSquidMode(player, id);
                squid = false;
            }
            long lastRegen = lastInkRegenAt.getOrDefault(id, now);
            if (now - lastRegen >= 1_000L) {
                int seconds = (int) Math.min(10L, (now - lastRegen) / 1_000L);
                boolean canRegen = squid && inFriendlyPaint
                        || now - lastShotAt.getOrDefault(id, 0L) >= 1_000L;
                if (canRegen && seconds > 0) {
                    int updated = SuperSploorRules.nextInkAfterRegeneration(
                            currentInk, seconds, squid && inFriendlyPaint);
                    if (updated != currentInk) {
                        ink.put(id, updated);
                        updateInkBar(player, id);
                    }
                }
                lastInkRegenAt.put(id, lastRegen + seconds * 1_000L);
            }

            long protectedUntil = spawnProtectedUntil.getOrDefault(id, 0L);
            if (protectedUntil > 0L && now >= protectedUntil) {
                spawnProtectedUntil.remove(id);
                player.setInvulnerable(false);
            } else if (protectedUntil > now) {
                player.setInvulnerable(true);
            }

            recoverMarkIfEligible(id, player, now);
            if (squid && engine.isFriendlyWallAdjacent(player.getLocation(), teams.getOrDefault(id, -1))) {
                Vector velocity = player.getVelocity();
                if (velocity.getY() < SuperSploorRules.SQUID_CLIMB_VELOCITY) {
                    velocity.setY(SuperSploorRules.SQUID_CLIMB_VELOCITY);
                    player.setVelocity(velocity);
                }
                world.spawnParticle(Particle.DUST, player.getLocation().add(0.0D, 0.15D, 0.0D),
                        2, 0.2D, 0.1D, 0.2D, 0.0D,
                        new Particle.DustOptions(teamColour(teams.get(id)), 0.8F));
            }
        }

        if (now >= matchEndsAt && !overtime) {
            int leader = leadingTeam();
            if (leader >= 0) {
                finish(leader);
                return;
            }
            overtime = true;
            overtimeEndsAt = now + SuperSploorRules.OVERTIME_MILLIS;
            announce(ChatColor.GOLD + "" + ChatColor.BOLD
                    + "Turf War is tied! First team to lead by 1% wins — 30 seconds of overtime.");
        } else if (overtime) {
            int red = engine.redUnits();
            int blue = engine.blueUnits();
            if (SuperSploorRules.hasDecisiveOvertimeLead(red, blue, engine.totalUnits())) {
                finish(SuperSploorRules.leadingTeam(red, blue));
                return;
            }
            if (now >= overtimeEndsAt) {
                announce(ChatColor.YELLOW + "Super Sploor overtime ended in a draw.");
                finish(-1);
                return;
            }
        }
        if (now - lastScoreboardAt >= 250L) updateScoreboard(now);
    }

    private long lastScoreboardAt;

    private void giveStarterLoadout(Player player, int team) {
        player.getInventory().clear();
        player.getInventory().setItem(0, namedItem(Material.BLAZE_ROD, ChatColor.GOLD + "Splattershot",
                "Right-click to fire paint • 4 ink per shot"));
        player.getInventory().setItem(7, namedItem(Material.INK_SAC, ChatColor.AQUA + "Squid Mode",
                "Select this slot on your team's paint to swim, climb and refill ink"));
        player.getInventory().setHeldItemSlot(PRIMARY_SLOT);
        player.getInventory().setArmorContents(teamArmour(team, 0));
        player.setExp(1.0F);
        player.setLevel(0);
        player.setWalkSpeed(0.2F);
    }

    private ItemStack namedItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(ChatColor.GRAY + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack[] teamArmour(int team, int currentMarks) {
        ItemStack[] armour = new ItemStack[4];
        Material material = Material.LEATHER_BOOTS;
        for (int slot = 0; slot < armour.length; slot++) {
            armour[slot] = new ItemStack(switch (slot) {
                case 0 -> Material.LEATHER_BOOTS;
                case 1 -> Material.LEATHER_LEGGINGS;
                case 2 -> Material.LEATHER_CHESTPLATE;
                default -> Material.LEATHER_HELMET;
            });
            LeatherArmorMeta meta = (LeatherArmorMeta) armour[slot].getItemMeta();
            meta.setColor(slot < currentMarks ? Color.PURPLE : teamColour(team));
            armour[slot].setItemMeta(meta);
        }
        return armour;
    }

    private Color teamColour(int team) {
        return team == RED ? Color.fromRGB(235, 65, 65) : Color.fromRGB(70, 155, 240);
    }

    private void paintArmour(Player player, int team, int markCount) {
        player.getInventory().setArmorContents(teamArmour(team, markCount));
    }

    private void updateInkBar(Player player, UUID id) {
        int value = ink.getOrDefault(id, 0);
        player.setExp(value / 100.0F);
        player.setLevel(value);
        player.sendActionBar(ChatColor.AQUA + "INK " + value + "%"
                + (squidMode.contains(id) ? ChatColor.GREEN + "  SQUID" : "")
                + ChatColor.GRAY + "  |  " + ChatColor.RED + engine.redUnits() + ChatColor.GRAY
                + " : " + ChatColor.AQUA + engine.blueUnits());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!running || event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!players.containsKey(id) || !player.isOnline() || respawnAt.containsKey(id)) return;
        // Map interaction is disabled during the match; only the current primary weapon fires.
        event.setCancelled(true);
        if (!event.getAction().isRightClick()) return;
        ItemStack held = event.getItem();
        if (held == null || held.getType() != Material.BLAZE_ROD
                || !held.hasItemMeta() || held.getItemMeta() == null
                || !ChatColor.stripColor(held.getItemMeta().getDisplayName()).equals("Splattershot")) return;
        event.setCancelled(true);
        firePrimary(player, id);
    }

    private void firePrimary(Player player, UUID id) {
        long now = System.currentTimeMillis();
        int current = ink.getOrDefault(id, 0);
        long previous = lastShotAt.getOrDefault(id, 0L);
        if (!SuperSploorRules.canFire(current, now, previous)) {
            if (current < SuperSploorRules.INK_PER_PRIMARY_SHOT) {
                player.sendActionBar(ChatColor.RED + "Out of ink! Swim through your team's paint to refill.");
            }
            return;
        }
        ink.put(id, SuperSploorRules.spendInk(current, SuperSploorRules.INK_PER_PRIMARY_SHOT));
        lastShotAt.put(id, now);
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(teams.get(id) == RED ? Material.RED_DYE : Material.LIGHT_BLUE_DYE));
        projectile.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.85D));
        projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        projectileOwners.put(projectile.getUniqueId(), id);
        activeProjectiles.add(projectile.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.7F, 1.45F);
        updateInkBar(player, id);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!running || !activeProjectiles.remove(event.getEntity().getUniqueId())) return;
        Projectile projectile = event.getEntity();
        UUID ownerId = projectileOwners.remove(projectile.getUniqueId());
        Player owner = ownerId == null ? null : players.get(ownerId);
        Location impact = projectile.getLocation().clone();
        if (event.getHitEntity() instanceof Player target && owner != null) {
            applyMark(ownerId, target.getUniqueId(), target);
        }
        if (owner != null && owner.isOnline()) {
            int team = teams.getOrDefault(ownerId, -1);
            SuperSploorEngine.PaintResult result = engine.paint(impact, team, 2);
            MutableStats stats = playerStats.get(ownerId);
            if (stats != null) {
                stats.territoryPainted += result.changedUnits();
                stats.enemyTerritoryReclaimed += result.enemyReclaimed();
            }
            if (result.changedUnits() > 0) {
                impact.getWorld().spawnParticle(Particle.DUST, impact.clone().add(0.0D, 0.3D, 0.0D),
                        18, 0.55D, 0.35D, 0.55D, 0.0D,
                        new Particle.DustOptions(teamColour(team), 1.4F));
                owner.playSound(impact, Sound.BLOCK_WOOL_PLACE, 0.65F, 1.4F);
            }
        }
        projectile.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (running && players.containsKey(event.getPlayer().getUniqueId())
                && event.getBlock().getWorld() == world) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (running && players.containsKey(event.getPlayer().getUniqueId())
                && event.getBlock().getWorld() == world) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        if (running && players.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (!running || !(event.getEntity() instanceof Player target)
                || !players.containsKey(target.getUniqueId())) return;
        event.setCancelled(true);
        if (event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Player attacker) {
            UUID attackerId = attacker.getUniqueId();
            if (players.containsKey(attackerId) && teams.getOrDefault(attackerId, -1)
                    != teams.getOrDefault(target.getUniqueId(), -2)) {
                applyMark(attackerId, target.getUniqueId(), target);
            }
        }
    }

    private void applyMark(UUID attackerId, UUID targetId, Player target) {
        if (!running || target == null || !target.isOnline()
                || !players.containsKey(targetId) || respawnAt.containsKey(targetId)) return;
        if (spawnProtectedUntil.getOrDefault(targetId, 0L) > System.currentTimeMillis()) return;
        int attackerTeam = teams.getOrDefault(attackerId, -1);
        int targetTeam = teams.getOrDefault(targetId, -2);
        if (attackerId == null || attackerTeam < 0 || attackerTeam == targetTeam) return;
        long now = System.currentTimeMillis();
        int count = SuperSploorRules.markCountAfterHit(marks.getOrDefault(targetId, 0));
        marks.put(targetId, count);
        lastHitAt.put(targetId, now);
        nextMarkRecoveryAt.put(targetId, now + SuperSploorRules.MARK_RECOVERY_DELAY_MILLIS
                + SuperSploorRules.MARK_RECOVERY_INTERVAL_MILLIS);
        paintArmour(target, targetTeam, count);
        target.playEffect(org.bukkit.EntityEffect.HURT);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0D, 1.0D, 0.0D),
                12, 0.35D, 0.5D, 0.35D, 0.0D,
                new Particle.DustOptions(Color.PURPLE, 1.2F));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        contributors.computeIfAbsent(targetId, ignored -> new HashSet<>()).add(attackerId);
        if (count >= SuperSploorRules.MAX_PAINT_MARKS) {
            kills.merge(attackerId, 1, Integer::sum);
            MutableStats attackerStats = playerStats.get(attackerId);
            if (attackerStats != null) attackerStats.splats++;
            splat(targetId, target, attackerId);
        } else {
            target.sendActionBar(ChatColor.RED + "SPLAT MARK " + count + "/4");
        }
    }

    private void splat(UUID targetId, Player target, UUID killerId) {
        if (!running || respawnAt.containsKey(targetId)) return;
        long now = System.currentTimeMillis();
        respawnAt.put(targetId, now + SuperSploorRules.SPLAT_RESPAWN_MILLIS);
        exitSquidMode(target, targetId);
        target.setInvulnerable(true);
        target.setGameMode(GameMode.SPECTATOR);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_DEATH, 0.9F, 1.1F);
        target.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "SPLATTED",
                ChatColor.GRAY + "Respawning in 2.5 seconds", 0, 30, 5);
        target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation().add(0.0D, 1.0D, 0.0D),
                45, 0.7D, 0.8D, 0.7D, 0.1D);
        announce(ChatColor.GRAY + target.getName() + " was splatted.");
        MutableStats victimStats = playerStats.get(targetId);
        if (victimStats != null) victimStats.deaths++;
        Set<UUID> helpers = contributors.remove(targetId);
        if (helpers != null) {
            for (UUID helperId : helpers) {
                if (helperId.equals(killerId)) continue;
                MutableStats helperStats = playerStats.get(helperId);
                if (helperStats != null) helperStats.assists++;
            }
        }
    }

    private void respawn(UUID id, long now) {
        Player player = players.get(id);
        Location spawn = spawns.get(id);
        if (player == null || !player.isOnline() || spawn == null) {
            respawnAt.remove(id);
            return;
        }
        respawnAt.remove(id);
        marks.put(id, 0);
        lastHitAt.remove(id);
        nextMarkRecoveryAt.remove(id);
        ink.put(id, SuperSploorRules.MAX_INK);
        spawnProtectedUntil.put(id, now + SuperSploorRules.SPAWN_PROTECTION_MILLIS);
        contributors.remove(id);
        player.teleport(spawn.clone());
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setInvulnerable(true);
        giveStarterLoadout(player, teams.getOrDefault(id, RED));
        player.sendMessage(ChatColor.GREEN + "Respawned! Your ink tank is full.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.3F);
    }

    private void recoverMarkIfEligible(UUID id, Player player, long now) {
        int count = marks.getOrDefault(id, 0);
        if (count <= 0) return;
        boolean friendly = engine.isFriendlySurface(player.getLocation(), teams.getOrDefault(id, -1));
        long lastHit = lastHitAt.getOrDefault(id, 0L);
        long nextAt = nextMarkRecoveryAt.getOrDefault(id, Long.MAX_VALUE);
        if (!SuperSploorRules.canRecoverMark(now, lastHit, nextAt, friendly, count)) return;
        int next = count - 1;
        marks.put(id, next);
        nextMarkRecoveryAt.put(id, now + SuperSploorRules.MARK_RECOVERY_INTERVAL_MILLIS);
        paintArmour(player, teams.get(id), next);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.45F, 1.8F);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!running || event instanceof PlayerTeleportEvent || event.getTo() == null) return;
        UUID id = event.getPlayer().getUniqueId();
        if (!players.containsKey(id) || respawnAt.containsKey(id) || !squidMode.contains(id)) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() != world || to.getWorld() != world) return;
        int team = teams.getOrDefault(id, -1);
        MutableStats stats = playerStats.get(id);
        if (stats == null) return;
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (engine.isFriendlySurface(to, team)
                && SuperSploorRules.shouldCountSquidTravel(horizontal, false)) {
            stats.squidTravelBlocks += horizontal;
        }
        double vertical = to.getY() - from.getY();
        if (vertical > 0.0D && vertical <= 1.25D
                && engine.isFriendlyWallAdjacent(from, team)
                && engine.isFriendlyWallAdjacent(to, team)) {
            stats.wallClimbBlocks += vertical;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHeldSlot(PlayerItemHeldEvent event) {
        if (!running) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!players.containsKey(id) || respawnAt.containsKey(id)) return;
        if (event.getNewSlot() == SQUID_SLOT) {
            if (!engine.isFriendlySurface(player.getLocation(), teams.getOrDefault(id, -1))) {
                event.setCancelled(true);
                player.sendActionBar(ChatColor.RED + "You can only enter Squid Mode on your team's paint.");
                return;
            }
            squidMode.add(id);
            player.setWalkSpeed((float) SuperSploorRules.SQUID_WALK_SPEED);
            player.setSneaking(true);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 0.7F, 1.4F);
            player.sendActionBar(ChatColor.GREEN + "SQUID MODE — swim, climb and refill!");
        } else if (squidMode.contains(id)) {
            exitSquidMode(player, id);
        }
    }

    private void exitSquidMode(Player player, UUID id) {
        squidMode.remove(id);
        if (player != null && player.isOnline()) {
            player.setWalkSpeed(0.2F);
            player.setSneaking(false);
        }
    }

    private void setupScoreboard() {
        if (Bukkit.getScoreboardManager() == null) return;
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("sploor", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "SUPER SPLOOR");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (Player player : players.values()) player.setScoreboard(scoreboard);
    }

    private void updateScoreboard(long now) {
        if (objective == null || engine == null) return;
        for (String old : sidebarEntries) scoreboard.resetScores(old);
        sidebarEntries.clear();
        int total = Math.max(1, engine.totalUnits());
        int red = (int) Math.round(engine.redUnits() * 100.0D / total);
        int blue = (int) Math.round(engine.blueUnits() * 100.0D / total);
        int neutral = Math.max(0, 100 - red - blue);
        long remain = overtime ? Math.max(0L, overtimeEndsAt - now) : Math.max(0L, matchEndsAt - now);
        String timer = String.format(java.util.Locale.ROOT, "%d:%02d", remain / 60_000L, (remain / 1_000L) % 60L);
        List<String> lines = List.of(
                ChatColor.WHITE + "Time: " + ChatColor.YELLOW + timer + (overtime ? ChatColor.GOLD + " OT" : ""),
                ChatColor.RED + "Red: " + red + "%",
                ChatColor.AQUA + "Blue: " + blue + "%",
                ChatColor.GRAY + "Neutral: " + neutral + "%",
                ChatColor.DARK_GRAY + " ",
                ChatColor.WHITE + "Your ink: " + ChatColor.AQUA + "shown below");
        int score = lines.size();
        for (String line : lines) {
            sidebarEntries.add(line);
            objective.getScore(line).setScore(score--);
        }
        lastScoreboardAt = now;
        int teamRed = engine.redUnits();
        int teamBlue = engine.blueUnits();
        for (Player player : players.values()) {
            if (!player.isOnline()) continue;
            player.sendActionBar(ChatColor.AQUA + "INK " + ink.getOrDefault(player.getUniqueId(), 0)
                    + "%" + ChatColor.GRAY + " | " + ChatColor.RED + teamRed
                    + ChatColor.GRAY + " : " + ChatColor.AQUA + teamBlue);
        }
    }

    private void announce(String message) {
        for (Player player : players.values()) {
            if (player != null && player.isOnline()) player.sendMessage(message);
        }
    }

    private void finish(int winningTeam) {
        if (!running) return;
        running = false;
        IntConsumer callback = finishMatch;
        if (callback != null) callback.accept(winningTeam);
    }

    public void stop() {
        running = false;
        if (world != null) {
            Set<UUID> projectileIds = new HashSet<>(activeProjectiles);
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (projectileIds.contains(entity.getUniqueId())) entity.remove();
            }
        }
        activeProjectiles.clear();
        projectileOwners.clear();
        for (UUID id : new ArrayList<>(squidMode)) {
            Player player = players.get(id);
            exitSquidMode(player, id);
        }
        if (engine != null) engine.restore();
        if (objective != null) {
            try { objective.unregister(); } catch (IllegalArgumentException ignored) { }
        }
        for (Player player : players.values()) {
            if (player != null && player.isOnline()) {
                player.setInvulnerable(false);
                player.setWalkSpeed(0.2F);
            }
        }
        players.clear();
        teams.clear();
        spawns.clear();
        ink.clear();
        marks.clear();
        lastShotAt.clear();
        lastInkRegenAt.clear();
        lastHitAt.clear();
        nextMarkRecoveryAt.clear();
        spawnProtectedUntil.clear();
        respawnAt.clear();
        kills.clear();
        playerStats.clear();
        contributors.clear();
        squidMode.clear();
        sidebarEntries.clear();
        engine = null;
        world = null;
        scoreboard = null;
        objective = null;
        finishMatch = null;
        matchEndsAt = 0L;
        overtimeEndsAt = 0L;
        overtime = false;
        lastScoreboardAt = 0L;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!players.containsKey(id)) return;
        Player player = players.remove(id);
        if (player != null) exitSquidMode(player, id);
        teams.remove(id);
        spawns.remove(id);
        ink.remove(id);
        marks.remove(id);
        respawnAt.remove(id);
        spawnProtectedUntil.remove(id);
        lastShotAt.remove(id);
        lastInkRegenAt.remove(id);
        lastHitAt.remove(id);
        nextMarkRecoveryAt.remove(id);
        contributors.remove(id);
        for (Set<UUID> values : contributors.values()) values.remove(id);
        if (players.isEmpty()) finish(-1);
    }
}
