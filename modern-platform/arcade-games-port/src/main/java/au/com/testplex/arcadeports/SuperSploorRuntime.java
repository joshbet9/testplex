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
import org.bukkit.block.Block;
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
    public record PlayerMatchStats(String kit, long territoryPainted, long enemyTerritoryReclaimed,
            long neutralTerritoryClaimed, int splats, int deaths, int assists, double squidTravelBlocks,
            double wallClimbBlocks, int specialsUsed, long rollerTerritory,
            int chargerFullHits, long lobberEnemyReclaimed, int dualiesPostRollSplats,
            int bestSpecialTargets) { }
    public record TerritorySummary(int redUnits, int blueUnits, int totalUnits) { }

    private static final class InkMine {
        final UUID ownerId; final int team; final Location location;
        final long armedAt; final long expiresAt;
        final Set<UUID> hitPlayers = new HashSet<>();
        InkMine(UUID ownerId, int team, Location location, long armedAt, long expiresAt) {
            this.ownerId=ownerId; this.team=team; this.location=location.clone();
            this.armedAt=armedAt; this.expiresAt=expiresAt;
        }
    }
    private static final class Sprinkler {
        final UUID ownerId; final int team; final Location location; final long expiresAt;
        final Set<UUID> hitPlayers = new HashSet<>(); long nextPulseAt;
        Sprinkler(UUID ownerId, int team, Location location, long now) {
            this.ownerId=ownerId; this.team=team; this.location=location.clone();
            this.expiresAt=now+6_000L; this.nextPulseAt=now;
        }
    }
    private static final class TrackerFlare {
        final UUID ownerId; final int team; final Location location; final long expiresAt;
        long nextPulseAt;
        TrackerFlare(UUID ownerId, int team, Location location, long now) {
            this.ownerId=ownerId; this.team=team; this.location=location.clone();
            this.expiresAt=now+4_000L; this.nextPulseAt=now;
        }
    }
    private static final class DelayedBlast {
        final UUID ownerId; final int team; final Location location; final long detonatesAt;
        DelayedBlast(UUID ownerId, int team, Location location, long detonatesAt) {
            this.ownerId=ownerId; this.team=team; this.location=location.clone(); this.detonatesAt=detonatesAt;
        }
    }
    private static final class Paintstorm {
        final UUID ownerId; final int team; final Location center; final long expiresAt;
        final Set<UUID> hitPlayers = new HashSet<>(); long nextPulseAt; int pulsesRemaining=4;
        Paintstorm(UUID ownerId, int team, Location center, long now) {
            this.ownerId=ownerId; this.team=team; this.center=center.clone();
            this.expiresAt=now+2_000L; this.nextPulseAt=now;
        }
    }

    private static final class MutableStats {
        long territoryPainted;
        long enemyTerritoryReclaimed;
        long neutralTerritoryClaimed;
        int splats;
        int deaths;
        int assists;
        double squidTravelBlocks;
        double wallClimbBlocks;
        int specialsUsed;
        String kit = "sprayer";
        long rollerTerritory;
        int chargerFullHits;
        long lobberEnemyReclaimed;
        int dualiesPostRollSplats;
        int bestSpecialTargets;

        PlayerMatchStats snapshot() {
            return new PlayerMatchStats(kit, territoryPainted, enemyTerritoryReclaimed,
                    neutralTerritoryClaimed, splats, deaths, assists, squidTravelBlocks,
                    wallClimbBlocks, specialsUsed,
                    rollerTerritory, chargerFullHits, lobberEnemyReclaimed,
                    dualiesPostRollSplats, bestSpecialTargets);
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
    private final Map<UUID, String> kits = new HashMap<>();
    private final Map<UUID, Integer> marks = new HashMap<>();
    private final Map<UUID, Long> lastShotAt = new HashMap<>();
    private final Map<UUID, Long> lastInkRegenAt = new HashMap<>();
    private final Map<UUID, Long> lastInkUseAt = new HashMap<>();
    private final Map<UUID, Long> chargerChargeStartedAt = new HashMap<>();
    private final Map<UUID, Double> specialCharge = new HashMap<>();
    private final Set<UUID> specialChargeSuppressed = new HashSet<>();
    private final Map<UUID, Long> secondaryReadyAt = new HashMap<>();
    private final Map<UUID, Long> stampedeUntil = new HashMap<>();
    private final Map<UUID, Long> stampedeNextPaintAt = new HashMap<>();
    private final Map<UUID, Set<UUID>> stampedeHitPlayers = new HashMap<>();
    private final Map<UUID, Set<UUID>> specialTargetsThisActivation = new HashMap<>();
    private final Map<UUID, Integer> afterimageDashesRemaining = new HashMap<>();
    private final Map<UUID, Long> afterimageNextAt = new HashMap<>();
    private final List<InkMine> mines = new ArrayList<>();
    private final List<Sprinkler> sprinklers = new ArrayList<>();
    private final List<TrackerFlare> trackerFlares = new ArrayList<>();
    private final List<DelayedBlast> delayedBlasts = new ArrayList<>();
    private final List<Paintstorm> paintstorms = new ArrayList<>();
    private final Map<UUID, String> projectileKinds = new HashMap<>();
    private final Map<UUID, UUID> projectileVolleyIds = new HashMap<>();
    private final Map<UUID, Set<UUID>> volleyHitTargets = new HashMap<>();
    private final Map<UUID, Integer> volleyRemaining = new HashMap<>();
    private final Map<UUID, Integer> dodgeRollCharges = new HashMap<>();
    private final Map<UUID, Long> nextDodgeRollRechargeAt = new HashMap<>();
    private final Map<UUID, Long> dualiesRollExpiresAt = new HashMap<>();
    private final Map<UUID, Set<UUID>> dualiesRollTargets = new HashMap<>();
    private final Map<UUID, Set<UUID>> dualiesPostRollUniqueTargets = new HashMap<>();
    private final Map<UUID, Long> lastHitAt = new HashMap<>();
    private final Map<UUID, Long> nextMarkRecoveryAt = new HashMap<>();
    private final Map<UUID, Long> spawnProtectedUntil = new HashMap<>();
    private final Map<UUID, Long> respawnAt = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, MutableStats> playerStats = new HashMap<>();
    private final Map<UUID, Set<UUID>> contributors = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> contributorTimes = new HashMap<>();
    private final Set<UUID> squidMode = new HashSet<>();
    private final Set<UUID> activeProjectiles = new HashSet<>();
    private final Map<UUID, UUID> projectileOwners = new HashMap<>();
    private final Map<UUID, Snowball> projectileEntities = new HashMap<>();
    private final Map<UUID, Integer> projectilePaintRadii = new HashMap<>();
    private final Set<UUID> projectilePaintLines = new HashSet<>();
    private final Set<UUID> fullyChargedProjectiles = new HashSet<>();
    private final Map<UUID, Location> projectileLastLocations = new HashMap<>();

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
            Map<UUID, String> participantKits, IntConsumer finishMatch) {
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
            String kit = normalizeKit(participantKits == null ? null : participantKits.get(id));
            kits.put(id, kit);
            ink.put(id, SuperSploorRules.MAX_INK);
            marks.put(id, 0);
            lastShotAt.put(id, now - 2_000L);
            lastInkRegenAt.put(id, now);
            lastInkUseAt.put(id, now);
            spawnProtectedUntil.put(id, now + SuperSploorRules.SPAWN_PROTECTION_MILLIS);
            kills.put(id, 0);
            MutableStats stats = new MutableStats();
            stats.kit = kit;
            playerStats.put(id, stats);
            specialCharge.put(id, 0.0D);
            secondaryReadyAt.put(id, now);
            specialTargetsThisActivation.put(id, new HashSet<>());
            contributors.put(id, new HashSet<>());
            contributorTimes.put(id, new HashMap<>());
            if ("dualies".equals(kit)) {
                dodgeRollCharges.put(id, 2);
                nextDodgeRollRechargeAt.put(id, Long.MAX_VALUE);
                dualiesRollTargets.put(id, new HashSet<>());
                dualiesPostRollUniqueTargets.put(id, new HashSet<>());
            }
            giveStarterLoadout(player, team, kit);
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
            player.sendMessage(ChatColor.YELLOW + "Right-click your selected primary weapon to paint. Select the Ink Sac on slot 8 while standing on friendly paint to enter Squid Mode.");
        }
        updateScoreboard(now);
    }

    public void tick(long now) {
        if (!running || engine == null) return;
        updateChargerPaintLines();
        rechargeDodgeRolls(now);
        updateTemporaryDevices(now);
        updateStampedes(now);
        updateAfterimages(now);
        for (Map.Entry<UUID, Long> charge : new ArrayList<>(chargerChargeStartedAt.entrySet())) {
            Player chargingPlayer = players.get(charge.getKey());
            if (chargingPlayer != null && chargingPlayer.isOnline()) {
                long percent = Math.min(100L, Math.max(0L, (now - charge.getValue()) * 100L / 900L));
                chargingPlayer.sendActionBar(ChatColor.AQUA + "CHARGER " + percent + "%");
            }
        }
        lastTickAt = now;
        for (UUID id : new ArrayList<>(players.keySet())) {
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            if (!respawnAt.containsKey(id) && player.getLocation().getY() <= world.getMinHeight() + 4.0D) {
                splat(id, player, null);
                continue;
            }
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
                boolean canRegen = SuperSploorRules.canRegenerateInk(now,
                        lastInkUseAt.getOrDefault(id, now), squid, inFriendlyPaint);
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

    private static String normalizeKit(String requested) {
        if (requested == null) return "sprayer";
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return Set.of("sprayer", "roller", "charger", "lobber", "dualies").contains(value) ? value : "sprayer";
    }

    private Material secondaryIcon(String kit) {
        return switch (kit) {
            case "roller" -> Material.STONE_PRESSURE_PLATE;
            case "charger" -> Material.FIREWORK_ROCKET;
            case "lobber" -> Material.LILY_PAD;
            case "dualies" -> Material.MAGMA_CREAM;
            default -> Material.SNOWBALL;
        };
    }

    private String secondaryName(String kit) {
        return switch (kit) {
            case "roller" -> "Ink Mine";
            case "charger" -> "Tracker Flare";
            case "lobber" -> "Sprinkler";
            case "dualies" -> "Suction Bomb";
            default -> "Burst Bomb";
        };
    }

    private String secondaryDescription(String kit) {
        return switch (kit) {
            case "roller" -> "Right-click to place a proximity mine. 30 ink.";
            case "charger" -> "Right-click to reveal nearby enemies to your team. 25 ink.";
            case "lobber" -> "Right-click to launch a temporary area sprinkler. 35 ink.";
            case "dualies" -> "Right-click to launch a delayed suction blast. 30 ink.";
            default -> "Right-click to throw an area paint bomb. 25 ink.";
        };
    }

    private String specialName(String kit) {
        return switch (kit) {
            case "roller" -> "Stampede";
            case "charger" -> "Prism Beam";
            case "lobber" -> "Paintstorm";
            case "dualies" -> "Afterimage Dash";
            default -> "Colourburst";
        };
    }

    private void useSecondary(Player player, UUID id) {
        String kit = kits.getOrDefault(id, "sprayer");
        int cost = switch (kit) {
            case "roller" -> 30;
            case "charger" -> 25;
            case "lobber" -> 35;
            case "dualies" -> 30;
            default -> 25;
        };
        long now = System.currentTimeMillis();
        if (now < secondaryReadyAt.getOrDefault(id, 0L)) {
            player.sendActionBar(ChatColor.YELLOW + "Secondary recharging.");
            return;
        }
        int current = ink.getOrDefault(id, 0);
        if (current < cost) {
            player.sendActionBar(ChatColor.RED + "Not enough ink for " + secondaryName(kit) + ".");
            return;
        }
        ink.put(id, SuperSploorRules.spendInk(current, cost));
        lastInkUseAt.put(id, now);
        secondaryReadyAt.put(id, now + 4_000L);

        if ("roller".equals(kit)) {
            Location mineLocation = player.getLocation().getBlock().getRelative(0, -1, 0)
                    .getLocation().add(0.5D, 1.0D, 0.5D);
            mines.add(new InkMine(id, teams.getOrDefault(id, -1), mineLocation,
                    now + 800L, now + 8_000L));
            world.spawnParticle(Particle.DUST, mineLocation, 12, 0.25D, 0.1D, 0.25D, 0.0D,
                    new Particle.DustOptions(teamColour(teams.get(id)), 1.4F));
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, 0.7F, 1.4F);
            player.sendActionBar(ChatColor.GREEN + "INK MINE PLACED");
        } else {
            String kind = switch (kit) {
                case "charger" -> "tracker-flare";
                case "lobber" -> "sprinkler";
                case "dualies" -> "suction-bomb";
                default -> "burst-bomb";
            };
            launchAbilityProjectile(player, id, kind, 1.45D);
            player.sendActionBar(ChatColor.GREEN + secondaryName(kit).toUpperCase(java.util.Locale.ROOT)
                    + ChatColor.GRAY + " launched");
        }
        updateInkBar(player, id);
    }

    private void launchAbilityProjectile(Player player, UUID ownerId, String kind, double speed) {
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(teams.getOrDefault(ownerId, RED) == RED
                ? Material.RED_DYE : Material.LIGHT_BLUE_DYE));
        projectile.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(speed));
        UUID projectileId = projectile.getUniqueId();
        projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        projectileOwners.put(projectileId, ownerId);
        projectileEntities.put(projectileId, projectile);
        activeProjectiles.add(projectileId);
        projectilePaintRadii.put(projectileId, 0);
        projectileKinds.put(projectileId, kind);
    }

    private void useSpecial(Player player, UUID id) {
        double charge = specialCharge.getOrDefault(id, 0.0D);
        if (!SuperSploorRules.canUseSpecial(charge)) {
            player.sendActionBar(ChatColor.LIGHT_PURPLE + "Special " + (int) Math.floor(charge) + "% — keep painting!");
            return;
        }
        String kit = kits.getOrDefault(id, "sprayer");
        specialCharge.put(id, 0.0D);
        specialChargeSuppressed.add(id);
        specialTargetsThisActivation.computeIfAbsent(id, ignored -> new HashSet<>()).clear();
        MutableStats stats = playerStats.get(id);
        if (stats != null) stats.specialsUsed++;

        switch (kit) {
            case "roller" -> {
                long now = System.currentTimeMillis();
                stampedeUntil.put(id, now + 4_000L);
                stampedeNextPaintAt.put(id, now);
                stampedeHitPlayers.put(id, new HashSet<>());
                player.setWalkSpeed(0.28F);
                player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.85F, 1.45F);
                announceTeam(id, ChatColor.GOLD + player.getName() + " activated STAMPEDE!");
            }
            case "charger" -> firePrismBeam(player, id);
            case "lobber" -> {
                Block target = player.getTargetBlockExact(18);
                Location center = target == null
                        ? player.getEyeLocation().add(player.getLocation().getDirection().multiply(12.0D))
                        : target.getLocation().add(0.5D, 1.0D, 0.5D);
                paintstorms.add(new Paintstorm(id, teams.getOrDefault(id, -1), center,
                        System.currentTimeMillis()));
                world.spawnParticle(Particle.DUST, center, 20, 1.3D, 0.25D, 1.3D, 0.0D,
                        new Particle.DustOptions(teamColour(teams.get(id)), 2.0F));
                world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.85F, 0.8F);
                announceTeam(id, ChatColor.GOLD + player.getName() + " launched PAINTSTORM!");
            }
            case "dualies" -> {
                afterimageDashesRemaining.put(id, 3);
                afterimageNextAt.put(id, System.currentTimeMillis());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.9F, 1.5F);
                announceTeam(id, ChatColor.GOLD + player.getName() + " activated AFTERIMAGE DASH!");
            }
            default -> {
                launchAbilityProjectile(player, id, "colourburst", 1.45D);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.9F, 1.2F);
                player.sendActionBar(ChatColor.LIGHT_PURPLE + "COLOURBURST!");
            }
        }
        updateInkBar(player, id);
    }

    private void announceTeam(UUID ownerId, String message) {
        int team = teams.getOrDefault(ownerId, -1);
        for (Map.Entry<UUID, Player> entry : players.entrySet()) {
            Player recipient = entry.getValue();
            if (recipient != null && recipient.isOnline() && teams.getOrDefault(entry.getKey(), -2) == team) {
                recipient.sendMessage(message);
            }
        }
    }

    private void markSpecialTarget(UUID ownerId, Player target) {
        if (target == null || !target.isOnline() || ownerId == null) return;
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();
        if (!SuperSploorRules.canMarkTarget(
                teams.getOrDefault(ownerId, -1), teams.getOrDefault(targetId, -1),
                respawnAt.containsKey(targetId), now, spawnProtectedUntil.getOrDefault(targetId, 0L))) return;
        Set<UUID> hits = specialTargetsThisActivation.computeIfAbsent(ownerId, ignored -> new HashSet<>());
        if (!hits.add(targetId)) return;
        MutableStats stats = playerStats.get(ownerId);
        if (stats != null) stats.bestSpecialTargets = Math.max(stats.bestSpecialTargets, hits.size());
        applyMark(ownerId, target.getUniqueId(), target);
    }

    private void paintArea(UUID ownerId, Location center, int radius, Set<UUID> alreadyHit,
            boolean special, long now) {
        if (ownerId == null || center == null || center.getWorld() != world) return;
        int team = teams.getOrDefault(ownerId, -1);
        if (team < 0) return;
        recordPaint(ownerId, engine.paint(center, team, radius));
        world.spawnParticle(Particle.DUST, center.clone().add(0.0D, 0.25D, 0.0D),
                22, radius * 0.35D, 0.2D, radius * 0.35D, 0.0D,
                new Particle.DustOptions(teamColour(team), 1.55F));
        for (Player target : new ArrayList<>(players.values())) {
            if (target == null || !target.isOnline() || target.getUniqueId().equals(ownerId)
                    || teams.getOrDefault(target.getUniqueId(), -2) == team
                    || respawnAt.containsKey(target.getUniqueId())) continue;
            if (target.getLocation().distanceSquared(center) > (radius + 0.9D) * (radius + 0.9D)) continue;
            if (alreadyHit != null && !alreadyHit.add(target.getUniqueId())) continue;
            if (special) markSpecialTarget(ownerId, target);
            else applyMark(ownerId, target.getUniqueId(), target);
        }
        world.playSound(center, Sound.BLOCK_WOOL_PLACE, 0.8F, 1.0F);
    }

    private void firePrismBeam(Player player, UUID ownerId) {
        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection().normalize();
        int team = teams.getOrDefault(ownerId, -1);
        Set<UUID> hitPlayers = new HashSet<>();
        for (int step = 1; step <= 56; step++) {
            Location point = origin.clone().add(direction.clone().multiply(step * 0.5D));
            Block block = point.getBlock();
            if (!block.isPassable() && engine.owner(block) == -2) break;
            recordPaint(ownerId, engine.paint(point, team, 0));
            world.spawnParticle(Particle.DUST, point, 2, 0.04D, 0.04D, 0.04D, 0.0D,
                    new Particle.DustOptions(teamColour(team), 1.25F));
            for (Player target : new ArrayList<>(players.values())) {
                if (target == null || !target.isOnline() || target.getUniqueId().equals(ownerId)
                        || teams.getOrDefault(target.getUniqueId(), -2) == team
                        || respawnAt.containsKey(target.getUniqueId())
                        || hitPlayers.contains(target.getUniqueId())) continue;
                if (target.getEyeLocation().distanceSquared(point) <= 1.2D) {
                    hitPlayers.add(target.getUniqueId());
                    markSpecialTarget(ownerId, target);
                }
            }

        }
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.55F, 1.8F);
        announceTeam(ownerId, ChatColor.LIGHT_PURPLE + "PRISM BEAM fired!");
        specialChargeSuppressed.remove(ownerId);
    }

    private void updateStampedes(long now) {
        for (UUID id : new ArrayList<>(stampedeUntil.keySet())) {
            Player player = players.get(id);
            if (player == null || !player.isOnline() || !stampedeUntil.containsKey(id)) {
                stampedeUntil.remove(id); stampedeNextPaintAt.remove(id); stampedeHitPlayers.remove(id);
                continue;
            }
            if (now >= stampedeUntil.get(id)) {
                stampedeUntil.remove(id); stampedeNextPaintAt.remove(id); stampedeHitPlayers.remove(id);
                specialChargeSuppressed.remove(id);
                player.setWalkSpeed(0.2F);
                player.sendActionBar(ChatColor.GRAY + "Stampede ended.");
                continue;
            }
            if (now < stampedeNextPaintAt.getOrDefault(id, 0L) || respawnAt.containsKey(id)) continue;
            stampedeNextPaintAt.put(id, now + 150L);
            Vector direction = player.getLocation().getDirection().setY(0.0D);
            if (direction.lengthSquared() < 1.0E-5D) continue;
            direction.normalize();
            Location origin = player.getLocation();
            Set<UUID> alreadyHit = stampedeHitPlayers.computeIfAbsent(id, ignored -> new HashSet<>());
            for (int step = 0; step <= 3; step++) {
                Location point = origin.clone().add(direction.clone().multiply(step));
                recordRollerPaint(id, engine.paint(point, teams.getOrDefault(id, -1), 2));
            }
            for (Player target : players.values()) {
                if (target == null || !target.isOnline() || target.getUniqueId().equals(id)
                        || teams.getOrDefault(target.getUniqueId(), -2) == teams.getOrDefault(id, -1)
                        || respawnAt.containsKey(target.getUniqueId())
                        || alreadyHit.contains(target.getUniqueId())) continue;
                Vector relative = target.getLocation().toVector().subtract(origin.toVector());
                double along = relative.dot(direction);
                if (along < 0.0D || along > 3.2D) continue;
                double lateral = relative.clone().subtract(direction.clone().multiply(along)).length();
                if (lateral <= 2.0D) {
                    alreadyHit.add(target.getUniqueId());
                    markSpecialTarget(id, target);
                }
            }
        }
    }

    private void resolveAbilityProjectile(UUID ownerId, String kind, Location impact) {
        if (ownerId == null || !players.containsKey(ownerId) || impact == null || impact.getWorld() != world) return;
        int team = teams.getOrDefault(ownerId, -1);
        long now = System.currentTimeMillis();
        switch (kind) {
            case "burst-bomb" -> paintArea(ownerId, impact, 3, new HashSet<>(), false, now);
            case "tracker-flare" -> trackerFlares.add(new TrackerFlare(ownerId, team, impact, now));
            case "sprinkler" -> {
                sprinklers.add(new Sprinkler(ownerId, team, impact, now));
                world.spawnParticle(Particle.DUST, impact, 16, 0.35D, 0.2D, 0.35D, 0.0D,
                        new Particle.DustOptions(teamColour(team), 1.7F));
            }
            case "suction-bomb" -> {
                delayedBlasts.add(new DelayedBlast(ownerId, team, impact, now + 1_400L));
                world.spawnParticle(Particle.DUST, impact, 18, 0.35D, 0.15D, 0.35D, 0.0D,
                        new Particle.DustOptions(teamColour(team), 1.6F));
            }
            case "colourburst" -> {
                paintArea(ownerId, impact, 5, new HashSet<>(), true, now);
                specialChargeSuppressed.remove(ownerId);
            }
            default -> { }
        }
    }

    private void updateAfterimages(long now) {
        for (UUID id : new ArrayList<>(afterimageDashesRemaining.keySet())) {
            Player player = players.get(id);
            int remaining = afterimageDashesRemaining.getOrDefault(id, 0);
            if (player == null || !player.isOnline() || respawnAt.containsKey(id) || remaining <= 0) {
                afterimageDashesRemaining.remove(id);
                afterimageNextAt.remove(id);
                specialChargeSuppressed.remove(id);
                continue;
            }
            if (now < afterimageNextAt.getOrDefault(id, 0L)) continue;
            Vector direction = player.getLocation().getDirection().setY(0.0D);
            if (direction.lengthSquared() < 1.0E-5D) direction = new Vector(0.0D, 0.0D, 1.0D);
            direction.normalize();
            player.setVelocity(direction.clone().multiply(1.2D).setY(0.16D));
            Location origin = player.getLocation().clone();
            int team = teams.getOrDefault(id, -1);
            for (int step = 0; step <= 2; step++) {
                Location point = origin.clone().add(direction.clone().multiply(step * 1.2D));
                recordPaint(id, engine.paint(point, team, 1));
            }
            for (Player target : players.values()) {
                if (target == null || !target.isOnline() || target.getUniqueId().equals(id)
                        || teams.getOrDefault(target.getUniqueId(), -2) == team
                        || respawnAt.containsKey(target.getUniqueId())) continue;
                if (target.getLocation().distanceSquared(origin) <= 4.0D) markSpecialTarget(id, target);
            }
            world.spawnParticle(Particle.DUST, origin, 18, 0.45D, 0.3D, 0.45D, 0.0D,
                    new Particle.DustOptions(teamColour(team), 1.5F));
            world.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.45F, 1.7F);
            afterimageDashesRemaining.put(id, remaining - 1);
            afterimageNextAt.put(id, now + 260L);
        }
    }

    private void updateTemporaryDevices(long now) {
        mines.removeIf(mine -> now >= mine.expiresAt);
        for (InkMine mine : new ArrayList<>(mines)) {
            if (now < mine.armedAt) continue;
            Player target = players.values().stream()
                    .filter(p -> p != null && p.isOnline() && !p.getUniqueId().equals(mine.ownerId)
                            && teams.getOrDefault(p.getUniqueId(), -2) != mine.team
                            && !respawnAt.containsKey(p.getUniqueId())
                            && p.getLocation().distanceSquared(mine.location) <= 2.25D
                            && !mine.hitPlayers.contains(p.getUniqueId()))
                    .findFirst().orElse(null);
            if (target == null) continue;
            paintArea(mine.ownerId, mine.location, 2, mine.hitPlayers, false, now);
            mines.remove(mine);
            break;
        }

        for (Sprinkler sprinkler : new ArrayList<>(sprinklers)) {
            if (now >= sprinkler.expiresAt) { sprinklers.remove(sprinkler); continue; }
            if (now < sprinkler.nextPulseAt) continue;
            sprinkler.nextPulseAt = now + 650L;
            recordPaint(sprinkler.ownerId, engine.paint(sprinkler.location, sprinkler.team, 2));
            for (Player target : players.values()) {
                if (target == null || !target.isOnline() || target.getUniqueId().equals(sprinkler.ownerId)
                        || teams.getOrDefault(target.getUniqueId(), -2) == sprinkler.team
                        || respawnAt.containsKey(target.getUniqueId())
                        || sprinkler.hitPlayers.contains(target.getUniqueId())) continue;
                if (target.getLocation().distanceSquared(sprinkler.location) <= 4.0D) {
                    sprinkler.hitPlayers.add(target.getUniqueId());
                    applyMark(sprinkler.ownerId, target.getUniqueId(), target);
                }
            }
            world.spawnParticle(Particle.DUST, sprinkler.location, 10, 0.8D, 0.15D, 0.8D, 0.0D,
                    new Particle.DustOptions(teamColour(sprinkler.team), 1.1F));
        }

        for (TrackerFlare flare : new ArrayList<>(trackerFlares)) {
            if (now >= flare.expiresAt) { trackerFlares.remove(flare); continue; }
            if (now < flare.nextPulseAt) continue;
            flare.nextPulseAt = now + 400L;
            for (Player viewer : players.values()) {
                if (viewer == null || !viewer.isOnline()
                        || teams.getOrDefault(viewer.getUniqueId(), -2) != flare.team) continue;
                for (Player enemy : players.values()) {
                    if (enemy == null || !enemy.isOnline()
                            || teams.getOrDefault(enemy.getUniqueId(), -2) == flare.team
                            || respawnAt.containsKey(enemy.getUniqueId())
                            || enemy.getLocation().distanceSquared(flare.location) > 324.0D) continue;
                    viewer.spawnParticle(Particle.DUST, enemy.getLocation().add(0.0D, 1.0D, 0.0D),
                            7, 0.22D, 0.35D, 0.22D, 0.0D,
                            new Particle.DustOptions(teamColour(1 - flare.team), 1.4F));
                }
            }
        }

        for (DelayedBlast blast : new ArrayList<>(delayedBlasts)) {
            if (now < blast.detonatesAt) continue;
            paintArea(blast.ownerId, blast.location, 2, new HashSet<>(), false, now);
            delayedBlasts.remove(blast);
        }

        for (Paintstorm storm : new ArrayList<>(paintstorms)) {
            if (now >= storm.expiresAt || storm.pulsesRemaining <= 0) {
                paintstorms.remove(storm);
                if (paintstorms.stream().noneMatch(other -> other.ownerId.equals(storm.ownerId))) {
                    specialChargeSuppressed.remove(storm.ownerId);
                }
                continue;
            }
            if (now < storm.nextPulseAt) continue;
            storm.nextPulseAt = now + 400L;
            storm.pulsesRemaining--;
            recordPaint(storm.ownerId, engine.paint(storm.center, storm.team, 3));
            for (Player target : players.values()) {
                if (target == null || !target.isOnline() || target.getUniqueId().equals(storm.ownerId)
                        || teams.getOrDefault(target.getUniqueId(), -2) == storm.team
                        || respawnAt.containsKey(target.getUniqueId())
                        || target.getLocation().distanceSquared(storm.center) > 16.0D) continue;
                if (!storm.hitPlayers.add(target.getUniqueId())) continue;
                markSpecialTarget(storm.ownerId, target);
            }
            world.spawnParticle(Particle.DUST, storm.center, 24, 1.5D, 0.25D, 1.5D, 0.0D,
                    new Particle.DustOptions(teamColour(storm.team), 1.8F));
            world.playSound(storm.center, Sound.ENTITY_GENERIC_EXPLODE, 0.45F, 1.4F);
        }
    }

    private void giveStarterLoadout(Player player, int team, String kit) {
        player.getInventory().clear();
        String name = switch (kit) {
            case "roller" -> "Paint Roller";
            case "charger" -> "Ink Charger";
            case "lobber" -> "Paint Lobber";
            case "dualies" -> "Twin Blasters";
            default -> "Splattershot";
        };
        Material weapon = switch (kit) {
            case "roller" -> Material.IRON_HOE;
            case "charger" -> Material.CROSSBOW;
            case "lobber" -> Material.SNOWBALL;
            case "dualies" -> Material.FLINT;
            default -> Material.BLAZE_ROD;
        };
        player.getInventory().setItem(0, namedItem(weapon, ChatColor.GOLD + name,
                "Right-click to use your primary • ink cost depends on kit"));
        player.getInventory().setItem(1, namedItem(secondaryIcon(kit),
                ChatColor.GREEN + secondaryName(kit), secondaryDescription(kit)));
        player.getInventory().setItem(2, namedItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + specialName(kit), "Right-click when the Special gauge reaches 100%."));
        if ("dualies".equals(kit)) {
            player.getInventory().setItem(3, namedItem(Material.ENDER_PEARL,
                    ChatColor.AQUA + "Dodge Roll", "Right-click to dodge. Two charges; one recharges every 5 seconds."));
        }
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
        int special = (int) Math.floor(specialCharge.getOrDefault(id, 0.0D));
        player.sendActionBar(ChatColor.AQUA + "INK " + value + "%"
                + (squidMode.contains(id) ? ChatColor.GREEN + "  SQUID" : "")
                + ChatColor.GRAY + "  | " + ChatColor.LIGHT_PURPLE + " SPECIAL " + special + "%"
                + ChatColor.GRAY + "  |  " + ChatColor.RED + engine.redUnits() + ChatColor.GRAY
                + " : " + ChatColor.AQUA + engine.blueUnits());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!running || event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!players.containsKey(id) || !player.isOnline() || respawnAt.containsKey(id)) return;
        event.setCancelled(true);
        ItemStack held = event.getItem();
        if (held == null || !held.hasItemMeta() || held.getItemMeta() == null
                || !held.getItemMeta().hasDisplayName()) return;
        String heldName = ChatColor.stripColor(held.getItemMeta().getDisplayName());
        String kit = kits.getOrDefault(id, "sprayer");
        if (event.getAction().isRightClick() && heldName.equals(secondaryName(kit))) {
            useSecondary(player, id);
            return;
        }
        if (event.getAction().isRightClick() && heldName.equals(specialName(kit))) {
            useSpecial(player, id);
            return;
        }
        if ("dualies".equals(kit) && "Dodge Roll".equals(heldName)
                && event.getAction().isRightClick()) {
            useDodgeRoll(player, id);
            return;
        }

        String expected = switch (kit) {
            case "roller" -> "Paint Roller";
            case "charger" -> "Ink Charger";
            case "lobber" -> "Paint Lobber";
            case "dualies" -> "Twin Blasters";
            default -> "Splattershot";
        };
        if (!expected.equals(heldName)) return;
        if ("charger".equals(kit)) {
            if (event.getAction().isRightClick()) beginChargerCharge(player, id);
            else if (event.getAction().isLeftClick()) releaseChargerCharge(player, id);
            return;
        }
        if (event.getAction().isRightClick()) firePrimary(player, id);
    }

    private void beginChargerCharge(Player player, UUID id) {
        if (chargerChargeStartedAt.containsKey(id)) return;
        long now = System.currentTimeMillis();
        if (ink.getOrDefault(id, 0) < 8) {
            player.sendActionBar(ChatColor.RED + "Not enough ink to charge.");
            return;
        }
        long lastShot = lastShotAt.getOrDefault(id, 0L);
        if (now - lastShot < 1_000L) {
            player.sendActionBar(ChatColor.YELLOW + "Charger recharging.");
            return;
        }
        chargerChargeStartedAt.put(id, now);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.35F, 1.7F);
        player.sendActionBar(ChatColor.AQUA + "CHARGER 0%");
    }

    private void releaseChargerCharge(Player player, UUID id) {
        Long started = chargerChargeStartedAt.remove(id);
        if (started == null) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - started);
        double charge = SuperSploorRules.chargerChargeFraction(elapsed);
        fireChargedCharger(player, id, charge);
    }

    private void fireChargedCharger(Player player, UUID id, double charge) {
        long now = System.currentTimeMillis();
        if (now - lastShotAt.getOrDefault(id, 0L) < 1_000L) return;
        int cost = SuperSploorRules.chargerInkCost(charge);
        int current = ink.getOrDefault(id, 0);
        if (current < cost) {
            player.sendActionBar(ChatColor.RED + "Not enough ink for that charge.");
            return;
        }
        ink.put(id, SuperSploorRules.spendInk(current, cost));
        lastShotAt.put(id, now);
        lastInkUseAt.put(id, now);
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(teams.get(id) == RED ? Material.RED_DYE : Material.LIGHT_BLUE_DYE));
        projectile.setVelocity(player.getEyeLocation().getDirection().normalize()
                .multiply(SuperSploorRules.chargerProjectileSpeed(charge)));
        UUID projectileId = projectile.getUniqueId();
        projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        projectileOwners.put(projectileId, id);
        projectileEntities.put(projectileId, projectile);
        activeProjectiles.add(projectileId);
        projectilePaintRadii.put(projectileId, 0);
        projectilePaintLines.add(projectileId);
        projectileLastLocations.put(projectileId, projectile.getLocation().clone());
        if (charge >= 0.99D) fullyChargedProjectiles.add(projectileId);
        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.9F, 1.9F);
        player.sendActionBar(ChatColor.AQUA + "CHARGER " + Math.round(charge * 100.0D) + "%");
        updateInkBar(player, id);
    }

    private void useDodgeRoll(Player player, UUID id) {
        long now = System.currentTimeMillis();
        int charges = dodgeRollCharges.getOrDefault(id, 0);
        if (charges <= 0) {
            long readyAt = nextDodgeRollRechargeAt.getOrDefault(id, now + 5_000L);
            player.sendActionBar(ChatColor.YELLOW + "Dodge Roll recharging: "
                    + Math.max(0L, (readyAt - now + 999L) / 1_000L) + "s");
            return;
        }
        dodgeRollCharges.put(id, SuperSploorRules.dodgeRollChargesAfterUse(charges));
        if (charges == SuperSploorRules.DODGE_ROLL_MAX_CHARGES) {
            nextDodgeRollRechargeAt.put(id, now + SuperSploorRules.DODGE_ROLL_RECHARGE_MILLIS);
        }
        dualiesRollExpiresAt.put(id, now + 5_000L);
        dualiesRollTargets.computeIfAbsent(id, ignored -> new HashSet<>()).clear();
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-5D) direction = new Vector(0.0D, 0.0D, 1.0D);
        direction.normalize().multiply(1.15D).setY(0.18D);
        player.setVelocity(direction);
        player.setFallDistance(0.0F);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7F, 1.5F);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12,
                0.25D, 0.2D, 0.25D, 0.04D);
        player.sendActionBar(ChatColor.AQUA + "DODGE ROLL  •  " + (charges - 1) + "/2 charges");
    }

    private void rechargeDodgeRolls(long now) {
        for (UUID id : new ArrayList<>(players.keySet())) {
            if (!"dualies".equals(kits.get(id)) || dodgeRollCharges.getOrDefault(id, 0) >= 2) continue;
            long readyAt = nextDodgeRollRechargeAt.getOrDefault(id, Long.MAX_VALUE);
            if (now < readyAt) continue;
            dodgeRollCharges.put(id, SuperSploorRules.nextDodgeRollCharge(
                    dodgeRollCharges.getOrDefault(id, 0)));
            nextDodgeRollRechargeAt.put(id,
                    dodgeRollCharges.getOrDefault(id, 0) < SuperSploorRules.DODGE_ROLL_MAX_CHARGES
                            ? now + SuperSploorRules.DODGE_ROLL_RECHARGE_MILLIS : Long.MAX_VALUE);
        }
    }

    private void firePrimary(Player player, UUID id) {
        long now = System.currentTimeMillis();
        String kit = kits.getOrDefault(id, "sprayer");
        int current = ink.getOrDefault(id, 0);
        long previous = lastShotAt.getOrDefault(id, 0L);
        int cost = switch (kit) {
            case "roller" -> 4;
            case "charger" -> 18;
            case "lobber" -> 14;
            case "dualies" -> 4;
            default -> SuperSploorRules.INK_PER_PRIMARY_SHOT;
        };
        long interval = switch (kit) {
            case "roller" -> 550L;
            case "charger" -> 1_000L;
            case "lobber" -> 1_100L;
            case "dualies" -> 350L;
            default -> SuperSploorRules.PRIMARY_SHOT_INTERVAL_MILLIS;
        };
        if (current < cost || now < previous || now - previous < interval) {
            if (current < cost) player.sendActionBar(ChatColor.RED + "Out of ink! Swim through your team's paint to refill.");
            return;
        }
        ink.put(id, SuperSploorRules.spendInk(current, cost));
        lastShotAt.put(id, now);
        lastInkUseAt.put(id, now);

        if ("roller".equals(kit)) {
            rollPaintPath(player, id);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_HOE_TILL, 0.8F, 0.8F);
            updateInkBar(player, id);
            return;
        }
        if ("dualies".equals(kit)) {
            fireDualiesVolley(player, id);
            updateInkBar(player, id);
            return;
        }

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(teams.get(id) == RED ? Material.RED_DYE : Material.LIGHT_BLUE_DYE));
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double speed = switch (kit) {
            case "charger" -> 3.0D;
            case "lobber" -> 1.15D;
            case "dualies" -> 1.45D;
            default -> 1.85D;
        };
        Vector velocity = direction.multiply(speed);
        if ("lobber".equals(kit)) velocity.setY(velocity.getY() + 0.16D);
        projectile.setVelocity(velocity);
        projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        UUID projectileId = projectile.getUniqueId();
        projectileOwners.put(projectileId, id);
        projectileEntities.put(projectileId, projectile);
        activeProjectiles.add(projectileId);
        projectilePaintRadii.put(projectileId, switch (kit) {
            case "lobber" -> 3;
            case "dualies" -> 1;
            case "charger" -> 0;
            default -> 2;
        });
        if ("charger".equals(kit)) {
            projectilePaintLines.add(projectileId);
            projectileLastLocations.put(projectileId, projectile.getLocation().clone());
        }
        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.7F,
                "charger".equals(kit) ? 1.8F : "lobber".equals(kit) ? 0.75F : 1.45F);
        updateInkBar(player, id);
    }

    private void fireDualiesVolley(Player player, UUID ownerId) {
        UUID volleyId = UUID.randomUUID();
        volleyHitTargets.put(volleyId, new HashSet<>());
        volleyRemaining.put(volleyId, 2);
        Vector forward = player.getEyeLocation().getDirection().normalize();
        int team = teams.getOrDefault(ownerId, RED);
        for (double spread : new double[] {-0.075D, 0.075D}) {
            Snowball projectile = player.launchProjectile(Snowball.class);
            projectile.setItem(new ItemStack(team == RED ? Material.RED_DYE : Material.LIGHT_BLUE_DYE));
            projectile.setVelocity(forward.clone().rotateAroundY(spread).multiply(1.45D));
            UUID projectileId = projectile.getUniqueId();
            projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
            projectileOwners.put(projectileId, ownerId);
            projectileEntities.put(projectileId, projectile);
            activeProjectiles.add(projectileId);
            projectilePaintRadii.put(projectileId, 1);
            projectileVolleyIds.put(projectileId, volleyId);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.7F, 1.65F);
    }

    private boolean registerVolleyHit(UUID projectileId, UUID targetId) {
        UUID volleyId = projectileVolleyIds.get(projectileId);
        if (volleyId == null) return true;
        Set<UUID> hitTargets = volleyHitTargets.get(volleyId);
        return hitTargets != null && hitTargets.add(targetId);
    }

    private void completeVolleyProjectile(UUID projectileId) {
        UUID volleyId = projectileVolleyIds.remove(projectileId);
        if (volleyId == null) return;
        int remaining = volleyRemaining.getOrDefault(volleyId, 1) - 1;
        if (remaining <= 0) {
            volleyRemaining.remove(volleyId);
            volleyHitTargets.remove(volleyId);
        } else {
            volleyRemaining.put(volleyId, remaining);
        }
    }

    private void rollPaintPath(Player player, UUID id) {
        int team = teams.getOrDefault(id, -1);
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-5D) return;
        direction.normalize();
        Location origin = player.getLocation().clone();
        for (int step = 0; step <= 5; step++) {
            Location point = origin.clone().add(direction.clone().multiply(step));
            point.setY(player.getLocation().getY() - 0.35D);
            recordRollerPaint(id, engine.paint(point, team, 1));
        }
        // The Roller pressures close opponents but cannot repeatedly mark one target every tick.
        for (Player target : new ArrayList<>(players.values())) {
            if (target == null || !target.isOnline() || target.getUniqueId().equals(id)
                    || teams.getOrDefault(target.getUniqueId(), -1) == team
                    || respawnAt.containsKey(target.getUniqueId())) continue;
            Vector relative = target.getLocation().toVector().subtract(origin.toVector());
            double along = relative.dot(direction);
            if (along < 0.0D || along > 4.5D) continue;
            double lateral = relative.clone().subtract(direction.clone().multiply(along)).length();
            if (lateral <= 1.25D) {
                applyMark(id, target.getUniqueId(), target);
            }
        }
    }

    private void recordPaint(UUID ownerId, SuperSploorEngine.PaintResult result) {
        MutableStats stats = playerStats.get(ownerId);
        if (stats == null || result == null) return;
        stats.territoryPainted += result.changedUnits();
        stats.enemyTerritoryReclaimed += result.enemyReclaimed();
        stats.neutralTerritoryClaimed += result.neutralClaimed();
        double currentCharge = specialCharge.getOrDefault(ownerId, 0.0D);
        specialCharge.put(ownerId, SuperSploorRules.nextSpecialCharge(
                currentCharge, result.changedUnits(), specialChargeSuppressed.contains(ownerId)));
        if ("lobber".equals(kits.get(ownerId))) stats.lobberEnemyReclaimed += result.enemyReclaimed();
    }

    private void recordRollerPaint(UUID ownerId, SuperSploorEngine.PaintResult result) {
        recordPaint(ownerId, result);
        MutableStats stats = playerStats.get(ownerId);
        if (stats != null && result != null && "roller".equals(kits.get(ownerId))) {
            stats.rollerTerritory += result.changedUnits();
        }
    }

    private void updateChargerPaintLines() {
        if (engine == null || world == null) return;
        for (UUID projectileId : new ArrayList<>(projectilePaintLines)) {
            Snowball projectile = projectileEntities.get(projectileId);
            UUID ownerId = projectileOwners.get(projectileId);
            if (projectile == null || !projectile.isValid() || ownerId == null) {
                projectilePaintLines.remove(projectileId);
                projectileLastLocations.remove(projectileId);
                continue;
            }
            Location current = projectile.getLocation();
            Location previous = projectileLastLocations.put(projectileId, current.clone());
            if (previous == null || previous.getWorld() != current.getWorld()) continue;
            double distance = previous.distance(current);
            int samples = Math.max(1, Math.min(16, (int) Math.ceil(distance / 0.35D)));
            Vector segment = current.toVector().subtract(previous.toVector());
            for (int sample = 1; sample <= samples; sample++) {
                Location point = previous.clone().add(segment.clone().multiply(sample / (double) samples));
                recordPaint(ownerId, engine.paint(point, teams.getOrDefault(ownerId, -1), 0));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!running || !activeProjectiles.remove(event.getEntity().getUniqueId())) return;
        Projectile projectile = event.getEntity();
        UUID projectileId = projectile.getUniqueId();
        UUID ownerId = projectileOwners.remove(projectileId);
        int paintRadius = projectilePaintRadii.getOrDefault(projectileId, 2);
        projectileEntities.remove(projectileId);
        projectilePaintRadii.remove(projectileId);
        projectilePaintLines.remove(projectileId);
        projectileLastLocations.remove(projectileId);
        String abilityKind = projectileKinds.remove(projectileId);
        Location impact = projectile.getLocation().clone();
        if (abilityKind != null) {
            resolveAbilityProjectile(ownerId, abilityKind, impact);
            fullyChargedProjectiles.remove(projectileId);
            projectile.remove();
            return;
        }
        Player owner = ownerId == null ? null : players.get(ownerId);
        if (event.getHitEntity() instanceof Player target && owner != null) {
            boolean validEnemyHit = !target.getUniqueId().equals(ownerId)
                    && teams.getOrDefault(ownerId, -1) != teams.getOrDefault(target.getUniqueId(), -2)
                    && !respawnAt.containsKey(target.getUniqueId())
                    && spawnProtectedUntil.getOrDefault(target.getUniqueId(), 0L) <= System.currentTimeMillis();
            if (fullyChargedProjectiles.remove(projectileId) && validEnemyHit) {
                MutableStats ownerStats = playerStats.get(ownerId);
                if (ownerStats != null) ownerStats.chargerFullHits++;
            }
            if (registerVolleyHit(projectileId, target.getUniqueId())) {
                applyMark(ownerId, target.getUniqueId(), target);
            }
        } else {
            fullyChargedProjectiles.remove(projectileId);
        }
        if (owner != null && owner.isOnline()) {
            int team = teams.getOrDefault(ownerId, -1);
            SuperSploorEngine.PaintResult result = engine.paint(impact, team, paintRadius);
            recordPaint(ownerId, result);
            if (result.changedUnits() > 0) {
                impact.getWorld().spawnParticle(Particle.DUST, impact.clone().add(0.0D, 0.3D, 0.0D),
                        18, 0.55D, 0.35D, 0.55D, 0.0D,
                        new Particle.DustOptions(teamColour(team), 1.4F));
                owner.playSound(impact, Sound.BLOCK_WOOL_PLACE, 0.65F, 1.4F);
            }
        }
        completeVolleyProjectile(projectileId);
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
        // Every real paint hit is resolved by ProjectileHitEvent or the kit's explicit
        // melee-shaped paint action. Vanilla punches and swords must not bypass the kit system.
        event.setCancelled(true);
    }

    private void applyMark(UUID attackerId, UUID targetId, Player target) {
        if (!running || target == null || !target.isOnline()
                || !players.containsKey(targetId) || respawnAt.containsKey(targetId)) return;
        if (spawnProtectedUntil.getOrDefault(targetId, 0L) > System.currentTimeMillis()) return;
        if (attackerId == null) return;
        int attackerTeam = teams.getOrDefault(attackerId, -1);
        int targetTeam = teams.getOrDefault(targetId, -2);
        long now = System.currentTimeMillis();
        if (!SuperSploorRules.canMarkTarget(attackerTeam, targetTeam,
                respawnAt.containsKey(targetId), now, spawnProtectedUntil.getOrDefault(targetId, 0L))) return;
        int count = SuperSploorRules.markCountAfterHit(marks.getOrDefault(targetId, 0));
        marks.put(targetId, count);
        lastHitAt.put(targetId, now);
        nextMarkRecoveryAt.put(targetId, now + SuperSploorRules.MARK_RECOVERY_DELAY_MILLIS);
        paintArmour(target, targetTeam, count);
        target.playEffect(org.bukkit.EntityEffect.HURT);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0D, 1.0D, 0.0D),
                12, 0.35D, 0.5D, 0.35D, 0.0D,
                new Particle.DustOptions(Color.PURPLE, 1.2F));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        contributors.computeIfAbsent(targetId, ignored -> new HashSet<>()).add(attackerId);
        contributorTimes.computeIfAbsent(targetId, ignored -> new HashMap<>()).put(attackerId, now);
        if (count >= SuperSploorRules.MAX_PAINT_MARKS) {
            kills.merge(attackerId, 1, Integer::sum);
            if (!specialChargeSuppressed.contains(attackerId)) {
                specialCharge.put(attackerId,
                        SuperSploorRules.specialChargeAfterSplat(specialCharge.getOrDefault(attackerId, 0.0D)));
            }
            MutableStats attackerStats = playerStats.get(attackerId);
            if (attackerStats != null) {
                attackerStats.splats++;
                if ("dualies".equals(kits.get(attackerId))
                        && dualiesRollExpiresAt.getOrDefault(attackerId, 0L) >= now
                        && dualiesRollTargets.computeIfAbsent(attackerId, ignored -> new HashSet<>()).add(targetId)
                        && dualiesPostRollUniqueTargets.computeIfAbsent(attackerId, ignored -> new HashSet<>()).add(targetId)) {
                    attackerStats.dualiesPostRollSplats++;
                }
            }
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
        chargerChargeStartedAt.remove(targetId);
        stampedeUntil.remove(targetId);
        stampedeNextPaintAt.remove(targetId);
        stampedeHitPlayers.remove(targetId);
        afterimageDashesRemaining.remove(targetId);
        afterimageNextAt.remove(targetId);
        paintstorms.removeIf(storm -> storm.ownerId.equals(targetId));
        List<UUID> cancelledSpecialProjectiles = projectileKinds.entrySet().stream()
                .filter(entry -> entry.getValue().equals("colourburst")
                        && targetId.equals(projectileOwners.get(entry.getKey())))
                .map(Map.Entry::getKey).toList();
        for (UUID projectileId : cancelledSpecialProjectiles) {
            Snowball projectile = projectileEntities.remove(projectileId);
            if (projectile != null && projectile.isValid()) projectile.remove();
            activeProjectiles.remove(projectileId);
            projectileOwners.remove(projectileId);
            projectilePaintRadii.remove(projectileId);
            projectileKinds.remove(projectileId);
        }
        specialChargeSuppressed.remove(targetId);
        target.setInvulnerable(true);
        target.setGameMode(GameMode.SPECTATOR);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_DEATH, 0.9F, 1.1F);
        target.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "SPLATTED",
                ChatColor.GRAY + "Respawning in 2.5 seconds", 0, 30, 5);
        target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation().add(0.0D, 1.0D, 0.0D),
                45, 0.7D, 0.8D, 0.7D, 0.1D);
        Player killer = players.get(killerId);
        announce(ChatColor.GRAY + target.getName()
                + (killer == null ? " was splatted." : " was splatted by " + killer.getName() + "."));
        MutableStats victimStats = playerStats.get(targetId);
        if (victimStats != null) victimStats.deaths++;
        Set<UUID> helpers = contributors.remove(targetId);
        Map<UUID, Long> hitTimes = contributorTimes.remove(targetId);
        if (helpers != null && hitTimes != null) {
            for (UUID helperId : helpers) {
                if (helperId.equals(killerId)) continue;
                long lastHit = hitTimes.getOrDefault(helperId, 0L);
                if (!SuperSploorRules.isAssistEligible(now, lastHit)) continue;
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
        giveStarterLoadout(player, teams.getOrDefault(id, RED), kits.getOrDefault(id, "sprayer"));
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
                ChatColor.LIGHT_PURPLE + "Special: charged in hotbar",
                ChatColor.DARK_GRAY + " ",
                ChatColor.WHITE + "Ink: " + ChatColor.AQUA + "XP bar");
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
        for (Map.Entry<UUID, Player> entry : players.entrySet()) {
            Player player = entry.getValue();
            MutableStats stats = playerStats.get(entry.getKey());
            if (player != null && player.isOnline() && stats != null) {
                player.sendMessage(ChatColor.GOLD + "Your Super Sploor stats: "
                        + ChatColor.WHITE + stats.territoryPainted + " territory | "
                        + ChatColor.AQUA + stats.enemyTerritoryReclaimed + " reclaimed | "
                        + ChatColor.RED + stats.splats + " splats | "
                        + ChatColor.GREEN + stats.assists + " assists.");
            }
        }
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
        projectileEntities.clear();
        fullyChargedProjectiles.clear();
        projectilePaintRadii.clear();
        projectilePaintLines.clear();
        projectileLastLocations.clear();
        projectileKinds.clear();
        projectileVolleyIds.clear();
        volleyHitTargets.clear();
        volleyRemaining.clear();
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
        kits.clear();
        ink.clear();
        marks.clear();
        lastShotAt.clear();
        lastInkRegenAt.clear();
        lastInkUseAt.clear();
        chargerChargeStartedAt.clear();
        dodgeRollCharges.clear();
        nextDodgeRollRechargeAt.clear();
        dualiesRollExpiresAt.clear();
        dualiesRollTargets.clear();
        dualiesPostRollUniqueTargets.clear();
        specialCharge.clear();
        specialChargeSuppressed.clear();
        secondaryReadyAt.clear();
        stampedeUntil.clear();
        stampedeNextPaintAt.clear();
        stampedeHitPlayers.clear();
        specialTargetsThisActivation.clear();
        afterimageDashesRemaining.clear();
        afterimageNextAt.clear();
        mines.clear();
        sprinklers.clear();
        trackerFlares.clear();
        delayedBlasts.clear();
        paintstorms.clear();
        lastHitAt.clear();
        nextMarkRecoveryAt.clear();
        spawnProtectedUntil.clear();
        respawnAt.clear();
        kills.clear();
        playerStats.clear();
        contributors.clear();
        contributorTimes.clear();
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
        kits.remove(id);
        chargerChargeStartedAt.remove(id);
        dodgeRollCharges.remove(id);
        nextDodgeRollRechargeAt.remove(id);
        dualiesRollExpiresAt.remove(id);
        dualiesRollTargets.remove(id);
        dualiesPostRollUniqueTargets.remove(id);
        specialCharge.remove(id);
        specialChargeSuppressed.remove(id);
        secondaryReadyAt.remove(id);
        stampedeUntil.remove(id);
        stampedeNextPaintAt.remove(id);
        stampedeHitPlayers.remove(id);
        specialTargetsThisActivation.remove(id);
        afterimageDashesRemaining.remove(id);
        afterimageNextAt.remove(id);
        mines.removeIf(mine -> mine.ownerId.equals(id));
        sprinklers.removeIf(device -> device.ownerId.equals(id));
        trackerFlares.removeIf(flare -> flare.ownerId.equals(id));
        delayedBlasts.removeIf(blast -> blast.ownerId.equals(id));
        paintstorms.removeIf(storm -> storm.ownerId.equals(id));
        for (UUID projectileId : new ArrayList<>(activeProjectiles)) {
            if (!id.equals(projectileOwners.get(projectileId))) continue;
            Projectile projectile = projectileEntities.remove(projectileId);
            if (projectile != null && projectile.isValid()) projectile.remove();
            activeProjectiles.remove(projectileId);
            projectileOwners.remove(projectileId);
            fullyChargedProjectiles.remove(projectileId);
            projectilePaintRadii.remove(projectileId);
            projectilePaintLines.remove(projectileId);
            projectileLastLocations.remove(projectileId);
            projectileKinds.remove(projectileId);
            completeVolleyProjectile(projectileId);
        }
        ink.remove(id);
        marks.remove(id);
        respawnAt.remove(id);
        spawnProtectedUntil.remove(id);
        lastShotAt.remove(id);
        lastInkRegenAt.remove(id);
        lastInkUseAt.remove(id);
        lastHitAt.remove(id);
        nextMarkRecoveryAt.remove(id);
        contributors.remove(id);
        contributorTimes.remove(id);
        for (Set<UUID> values : contributors.values()) values.remove(id);
        for (Map<UUID, Long> values : contributorTimes.values()) values.remove(id);
        if (players.isEmpty()) finish(-1);
    }
}
