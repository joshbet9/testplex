package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mechanics-first modern prototypes for the 14 remaining Arcade games.
 * Each game gets its own persistent test world and distinct baseline loop.
 * These implementations are deliberately marked in progress until parity and
 * live gameplay acceptance are done.
 */
public final class ArcadePortManager implements Listener {
    private static final int HALF_SIZE = 20;
    private static final long ROUND_MS = 180_000L;
    private static final long JOIN_WINDOW_TICKS = 100L;
    private final ArcadeMechanicsModernPlugin plugin;
    private final Map<String, LinkedHashMap<UUID, Player>> queues = new HashMap<>();
    private final Map<String, World> worlds = new HashMap<>();
    private final Set<String> sourceMapWorlds = new HashSet<>();
    private final Map<String, SourceMapCatalog.MapData> sourceMapData = new HashMap<>();
    private final Map<UUID, PlayerSnapshot> pendingRestore = new HashMap<>();
    private final Map<UUID, String> paintballKitPreferences = new HashMap<>();
    private final Map<UUID, String> deathTagRunnerKitPreferences = new HashMap<>();
    private final Map<UUID, String> baconKitPreferences = new HashMap<>();
    private final Map<UUID, String> bombLobbersKitPreferences = new HashMap<>();
    private final Map<UUID, String> spleefKitPreferences = new HashMap<>();
    private final Map<UUID, String> microKitPreferences = new HashMap<>();
    private final Map<UUID, String> quiverKitPreferences = new HashMap<>();
    private final Map<UUID, String> runnerKitPreferences = new HashMap<>();
    private final Map<UUID, String> dragonsKitPreferences = new HashMap<>();
    private final Map<UUID, String> dragonEscapeKitPreferences = new HashMap<>();
    private Session active;
    private BukkitTask pendingStart;
    private ArcadePortGame pendingType;
    private long lastDragonHazard;
    private long lastSheepSpawn;

    ArcadePortManager(ArcadeMechanicsModernPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void selectKit(Player player, String requestedKit) {
        if (player == null || requestedKit == null) return;
        String value = requestedKit.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        Session running = active;
        if (running != null && running.players.containsKey(player.getUniqueId())) {
            switch (running.type) {
                case BACON_BRAWL -> { selectBaconBrawlKit(player, value); return; }
                case BOMB_LOBBERS -> { selectBombLobbersKit(player, value); return; }
                case SPLEEF -> { selectSpleefKit(player, value); return; }
                case MICRO_BATTLES -> { selectMicroBattlesKit(player, value); return; }
                case ONE_IN_THE_QUIVER -> { selectQuiverKit(player, value); return; }
                case DRAGONS -> { selectDragonsKit(player, value); return; }
                case DRAGON_ESCAPE -> { selectDragonEscapeKit(player, value); return; }
                case PAINTBALL -> { selectPaintballKit(player, value); return; }
                case DEATH_TAG -> { selectDeathTagRunnerKit(player, value); return; }
                case RUNNER -> { selectRunnerKit(player, value); return; }
                default -> { }
            }
        }
        if (value.startsWith("quiver-")) { selectQuiverKit(player, value); }
        else if (value.startsWith("spleef-")) { selectSpleefKit(player, value.substring("spleef-".length())); }
        else if (value.startsWith("micro-")) { selectMicroBattlesKit(player, value.substring("micro-".length())); }
        else if (ArcadePortRules.normalizeBaconBrawlKit(value) != null) selectBaconBrawlKit(player, value);
        else if (ArcadePortRules.normalizeBombLobbersKit(value) != null) selectBombLobbersKit(player, value);
        else if (ArcadePortRules.normalizeSpleefKit(value) != null) selectSpleefKit(player, value);
        else if (ArcadePortRules.normalizeMicroBattlesKit(value) != null) selectMicroBattlesKit(player, value);
        else if (Set.of("rifle", "sniper", "shotgun", "machine-gun", "machinegun").contains(value)) selectPaintballKit(player, value);
        else if (ArcadePortRules.normalizeQuiverKit(value) != null) selectQuiverKit(player, value);
        else if (ArcadePortRules.normalizeDragonsKit(value) != null) selectDragonsKit(player, value);
        else if (ArcadePortRules.normalizeDragonEscapeKit(value) != null) selectDragonEscapeKit(player, value);
        else if (value.startsWith("runnerkit-")) selectRunnerKit(player, value);
        else selectDeathTagRunnerKit(player, value);
    }

    public void selectBombLobbersKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = ArcadePortRules.normalizeBombLobbersKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Bomb Lobbers kits: jumper, armorer, pitcher, waller.");
            return;
        }
        bombLobbersKitPreferences.put(player.getUniqueId(), kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.BOMB_LOBBERS
                && session.players.containsKey(player.getUniqueId()) && session.alive.contains(player.getUniqueId())) {
            session.bombLobbersKits.put(player.getUniqueId(), kit);
            session.bombPitchLevels.put(player.getUniqueId(), 2);
            giveKit(session, player);
            player.sendMessage(ChatColor.GREEN + "Bomb Lobbers kit changed to " + kit + ".");
        } else player.sendMessage(ChatColor.GREEN + "Bomb Lobbers kit set to " + kit + " for your next test.");
    }

    public void selectBaconBrawlKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = ArcadePortRules.normalizeBaconBrawlKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Bacon Brawl kits: pig, mama-pig, sheep-pig. Chris P. Bacon is intentionally omitted.");
            return;
        }
        baconKitPreferences.put(player.getUniqueId(), kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.BACON_BRAWL
                && session.players.containsKey(player.getUniqueId()) && session.alive.contains(player.getUniqueId())) {
            clearBaconCloak(session, player.getUniqueId());
            session.baconSlams.remove(player.getUniqueId());
            session.baconKits.put(player.getUniqueId(), kit);
            giveKit(session, player);
            player.sendMessage(ChatColor.GREEN + "Bacon Brawl kit changed to " + kit + ".");
        } else player.sendMessage(ChatColor.GREEN + "Bacon Brawl kit set to " + kit + " for your next test.");
    }

    public void selectSpleefKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = ArcadePortRules.normalizeSpleefKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Spleef kits: snowballer, brawler, archer.");
            return;
        }
        spleefKitPreferences.put(player.getUniqueId(), kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.SPLEEF
                && session.players.containsKey(player.getUniqueId()) && session.alive.contains(player.getUniqueId())) {
            session.spleefKits.put(player.getUniqueId(), kit);
            session.spleefLeapReadyAt.remove(player.getUniqueId());
            giveKit(session, player);
            player.sendMessage(ChatColor.GREEN + "Spleef kit changed to " + kit + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "Spleef kit set to " + kit + " for your next test.");
        }
    }

    public void selectMicroBattlesKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = ArcadePortRules.normalizeMicroBattlesKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Micro Battles kits: archer, worker, fighter.");
            return;
        }
        UUID playerId = player.getUniqueId();
        microKitPreferences.put(playerId, kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.MICRO_BATTLES
                && session.players.containsKey(playerId) && session.alive.contains(playerId)) {
            session.microKits.put(playerId, kit);
            session.microNextArrowAt.put(playerId,
                    System.currentTimeMillis() + ArcadePortRules.MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS);
            giveKit(session, player);
            player.sendMessage(ChatColor.GREEN + "Micro Battles kit changed to " + kit + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "Micro Battles kit set to " + kit + " for your next test.");
        }
    }

    public void selectQuiverKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = ArcadePortRules.normalizeQuiverKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "One in the Quiver kits: leaper, brawler, enchanter, slam-shot, ninja.");
            return;
        }
        UUID id = player.getUniqueId();
        quiverKitPreferences.put(id, kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.ONE_IN_THE_QUIVER
                && session.players.containsKey(id) && session.alive.contains(id)) {
            session.quiverKits.put(id, kit);
            session.quiverGroundPoundReadyAt.remove(id);
            if (!"ninja".equals(kit)) clearQuiverCloak(session, id);
            if (!"leaper".equals(kit)) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            giveKit(session, player);
            player.sendMessage(ChatColor.GREEN + "One in the Quiver kit changed to " + kit + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "One in the Quiver kit set to " + kit + " for your next test.");
        }
    }

    public void selectDragonsKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = ArcadePortRules.normalizeDragonsKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Dragons kits: coward, marksman, pyrotechnic.");
            return;
        }
        UUID id = player.getUniqueId();
        dragonsKitPreferences.put(id, kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.DRAGONS
                && session.players.containsKey(id) && session.alive.contains(id)) {
            session.dragonKits.put(id, kit);
            session.dragonNextSparklerAt.put(id, System.currentTimeMillis()
                    + ArcadePortRules.DRAGONS_SPARKLER_INTERVAL_MILLIS);
            session.dragonMarksmanCharge.remove(id);
            session.dragonMarksmanNextChargeAt.remove(id);
            session.dragonMarksmanFiring.remove(id);
            giveKit(session, player);
            player.sendMessage(ChatColor.GREEN + "Dragons kit changed to " + kit + ".");
        } else player.sendMessage(ChatColor.GREEN + "Dragons kit set to " + kit + " for your next test.");
    }

    public void selectDragonEscapeKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = ArcadePortRules.normalizeDragonEscapeKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Dragon Escape kits: jumper, disruptor, warper, digger.");
            return;
        }
        UUID id = player.getUniqueId();
        dragonEscapeKitPreferences.put(id, kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.DRAGON_ESCAPE
                && session.players.containsKey(id) && session.alive.contains(id)) {
            session.dragonEscapeKits.put(id, kit);
            session.dragonEscapeLeapUsesRemaining.put(id, ArcadePortRules.dragonEscapeLeapUses(kit));
            session.dragonEscapeDiggerUsesRemaining.put(id, ArcadePortRules.DRAGON_ESCAPE_DIGGER_MAX_USES);
            session.dragonEscapeNextTntAt.put(id,
                    System.currentTimeMillis() + ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS);
            session.dragonEscapeWarperUsed.remove(id);
            giveKit(session, player);
            player.sendMessage(ChatColor.GREEN + "Dragon Escape kit changed to " + kit + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "Dragon Escape kit set to " + kit + " for your next test.");
        }
    }

    public void selectRunnerKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = ArcadePortRules.normalizeRunnerKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Runner kits: leaper, archer, frosty. Use runnerkit-archer outside a Runner test.");
            return;
        }
        UUID id = player.getUniqueId();
        runnerKitPreferences.put(id, kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.RUNNER
                && session.players.containsKey(id) && session.alive.contains(id)) {
            session.runnerKits.put(id, kit);
            session.runnerLeapReadyAt.remove(id);
            session.runnerQuickshotReadyAt.remove(id);
            session.runnerNextFrostBallAt.put(id,
                    System.currentTimeMillis() + ArcadePortRules.RUNNER_FROSTBALL_INTERVAL_MILLIS);
            giveKit(session, player);
            player.sendMessage(ChatColor.GREEN + "Runner kit changed to " + kit + ".");
        } else player.sendMessage(ChatColor.GREEN + "Runner kit set to " + kit + " for your next test.");
    }

    public void selectDeathTagRunnerKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = requestedKit == null ? "" : requestedKit.toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace(' ', '-');
        if (kit.startsWith("runner-")) kit = kit.substring("runner-".length());
        if (!Set.of("basher", "archer", "traitor", "null").contains(kit)) {
            player.sendMessage(ChatColor.YELLOW + "Kits: rifle, sniper, shotgun, machine-gun, runner-basher, runner-archer, runner-traitor, runner-null.");
            return;
        }
        deathTagRunnerKitPreferences.put(player.getUniqueId(), kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.DEATH_TAG
                && session.players.containsKey(player.getUniqueId())) {
            if (session.tagged.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Chasers cannot switch back to a runner kit.");
                return;
            }
            session.deathTagRunnerKits.put(player.getUniqueId(), kit);
            giveDeathTagKit(session, player, false, false);
            player.sendMessage(ChatColor.GREEN + "Death Tag runner kit set to " + kit + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "Death Tag runner kit set to " + kit + " for your next test.");
        }
    }

    public void selectPaintballKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = requestedKit == null ? "" : requestedKit.toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace(' ', '-');
        if (!Set.of("rifle", "sniper", "shotgun", "machine-gun", "machinegun").contains(kit)) {
            player.sendMessage(ChatColor.YELLOW + "Paintball kits: rifle, sniper, shotgun, machine-gun.");
            return;
        }
        if ("machinegun".equals(kit)) kit = "machine-gun";
        paintballKitPreferences.put(player.getUniqueId(), kit);
        Session session = active;
        if (session != null && session.type == ArcadePortGame.PAINTBALL
                && session.players.containsKey(player.getUniqueId()) && session.alive.contains(player.getUniqueId())) {
            session.paintballKits.put(player.getUniqueId(), kit);
            givePaintballKit(session, player, kit);
            player.sendMessage(ChatColor.GREEN + "Paintball kit changed to " + kit + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "Paintball kit set to " + kit + " for your next Paintball test.");
        }
    }

    public void queue(ArcadePortGame type, Player player, boolean quickStart) {
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Unknown game id. Use /arcadeport help for the available IDs.");
            return;
        }
        if (active != null && active.type == type) {
            player.sendMessage(ChatColor.RED + "That game is already in progress. Use /arcadeport status or queue the next game.");
            return;
        }
        for (LinkedHashMap<UUID, Player> queue : queues.values()) queue.remove(player.getUniqueId());
        queues.computeIfAbsent(type.id(), ignored -> new LinkedHashMap<>()).put(player.getUniqueId(), player);
        player.sendMessage(ChatColor.GREEN + "Queued for " + type.displayName() + ". " +
                (quickStart ? "Starting after a short join window so friends can join too." : "An operator/player can use /arcadeport start " + type.id() + "."));
        if (quickStart) scheduleQuickStart(type);
    }

    private void scheduleQuickStart(ArcadePortGame type) {
        if (active != null) return;
        if (pendingStart != null) {
            if (pendingType == type) return;
            pendingStart.cancel();
        }
        pendingType = type;
        pendingStart = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingStart = null;
            pendingType = null;
            startQueued(type, null, Bukkit.getConsoleSender());
        }, JOIN_WINDOW_TICKS);
    }

    public void startQueued(ArcadePortGame type, Player requester, org.bukkit.command.CommandSender sender) {
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown game id.");
            return;
        }
        if (active != null) {
            sender.sendMessage(ChatColor.RED + "A modern Arcade test is already running: " + active.type.displayName() + ".");
            return;
        }
        LinkedHashMap<UUID, Player> queue = queues.get(type.id());
        List<Player> participants = new ArrayList<>();
        if (queue != null) {
            for (Player player : queue.values()) if (player != null && player.isOnline()) participants.add(player);
        }
        if (requester != null && requester.isOnline() && participants.stream().noneMatch(p -> p.getUniqueId().equals(requester.getUniqueId()))) {
            participants.add(requester);
        }
        if (participants.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No online players queued. Use /arcadeport join " + type.id() + " or /arcadeport play " + type.id() + ".");
            return;
        }
        if (queue != null) queues.remove(type.id());
        if (pendingStart != null && pendingType == type) {
            pendingStart.cancel();
            pendingStart = null;
            pendingType = null;
        }
        World world;
        if (SourceMapCatalog.hasMap(type)) {
            String sourceWorldName = "arcadeport_source_" + type.id().replace('-', '_');
            if (!SourceMapCatalog.resetTestWorld(plugin, sourceWorldName)) {
                sender.sendMessage(ChatColor.RED + "Could not reset the archived source map; refusing to start on a modified arena.");
                return;
            }
            worlds.remove(type.id());
            sourceMapWorlds.remove(type.id());
            sourceMapData.remove(type.id());
            world = createWorld(type);
            if (world != null) worlds.put(type.id(), world);
        } else {
            world = worlds.computeIfAbsent(type.id(), ignored -> createWorld(type));
        }
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "The test world could not be loaded. Check the server log.");
            return;
        }
        if (!sourceMapWorlds.contains(type.id())) buildArena(type, world);
        Session session = new Session(type, world, participants);
        active = session;
        long now = System.currentTimeMillis();
        session.startedAt = now;
        session.endsAt = now + (type == ArcadePortGame.SHEEP_QUEST ? ArcadePortRules.SHEEP_QUEST_ROUND_MILLIS
                : type == ArcadePortGame.GLADIATORS ? 600_000L : ROUND_MS);
        session.lastBaconHungerAt = now;
        session.lastWorldWaterDamageAt = now;
        session.lastBaconEscalationAt = now;
        session.lastSpleefHungerAt = now;
        session.dragonNextSpawnAt = now;
        session.dragonSpectatorLocation = dragonSpectatorLocation(session);
        session.baconKnockback = ArcadePortRules.initialBaconKnockback(participants.size());
        session.baconEscalationIntervalMillis = ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS;
        session.microBarrierActive = type == ArcadePortGame.MICRO_BATTLES;
        SourceMapCatalog.MapData microSourceMap = sourceMapData.get(type.id());
        if (type == ArcadePortGame.MICRO_BATTLES) {
            session.microCrumbleMinX = microSourceMap == null ? -HALF_SIZE : microSourceMap.minX();
            session.microCrumbleMaxX = microSourceMap == null ? HALF_SIZE : microSourceMap.maxX();
            session.microCrumbleMinZ = microSourceMap == null ? -HALF_SIZE : microSourceMap.minZ();
            session.microCrumbleMaxZ = microSourceMap == null ? HALF_SIZE : microSourceMap.maxZ();
            session.microCrumbleMinY = microSourceMap == null ? 0
                    : microSourceMap.hasVerticalBounds() ? microSourceMap.minY() : 0;
            session.microCrumbleMaxY = microSourceMap == null ? 96
                    : microSourceMap.hasVerticalBounds() ? microSourceMap.maxY() : 128;
            session.microCrumbleScanY = session.microCrumbleMinY;
        }
        if (type == ArcadePortGame.MICRO_BATTLES && microSourceMap != null) {
            for (Location marker : microSourceMap.customLocations("20")) {
                Block barrier = marker.getBlock();
                session.microBarrierBlocks.add(barrier);
                barrier.setType(Material.GLASS, false);
            }
        }
        if (type == ArcadePortGame.DEATH_TAG) {
            SourceMapCatalog.MapData deathTagMap = sourceMapData.get(type.id());
            if (deathTagMap != null) {
                for (Location marker : deathTagMap.customLocations("89")) {
                    Block light = marker.getBlock();
                    session.deathTagOriginalLightBlocks.putIfAbsent(light, light.getBlockData().clone());
                    if (ThreadLocalRandom.current().nextBoolean()) light.setType(Material.GOLD_BLOCK, false);
                }
            }
            session.deathTagNextLightAt = now + 1_000L;
        }
        session.turfCombat = false;
        // Reborn doubles the opening build window; later build windows are 20 seconds.
        session.turfPhaseEndsAt = type == ArcadePortGame.TURF_WARS
                ? now + ArcadePortRules.TURF_WARS_INITIAL_BUILD_PHASE_MILLIS : 0L;
        SourceMapCatalog.MapData turfSourceMap = type == ArcadePortGame.TURF_WARS
                ? sourceMapData.get(type.id()) : null;
        if (turfSourceMap != null && turfSourceMap.hasBounds()
                && turfSourceMap.maxX() > turfSourceMap.minX()
                && turfSourceMap.maxZ() > turfSourceMap.minZ()) {
            session.turfFrontlineMinX = turfSourceMap.minX();
            session.turfFrontlineMaxXExclusive = turfSourceMap.maxX();
            session.turfMinZ = turfSourceMap.minZ();
            session.turfMaxZExclusive = turfSourceMap.maxZ();
        }
        if (turfSourceMap != null) {
            List<Location> redTeamSpawns = turfSourceMap.teamSpawnLocationsForTeam(0, 2);
            if (!redTeamSpawns.isEmpty()) {
                session.turfFloorY = ArcadePortRules.turfWarsFloorYFromSpawn(redTeamSpawns.get(0).getY());
            }
        }
        session.turfInitialFrontlineX = ArcadePortRules.turfWarsInitialFrontline(
                session.turfFrontlineMinX, session.turfFrontlineMaxXExclusive);
        session.turfFrontlineX = session.turfInitialFrontlineX;
        lastDragonHazard = now + 8_000L;
        lastSheepSpawn = now + ArcadePortRules.SHEEP_QUEST_SPAWN_INTERVAL_MILLIS;
        if (type == ArcadePortGame.DRAGON_ESCAPE) spawnDragonEscapeDragon(session);
        SourceMapCatalog.MapData sourceMap = sourceMapData.get(type.id());
        String sourceMapName = sourceMap == null ? "" : sourceMap.mapName();
        world.setTime(type == ArcadePortGame.DRAGON_ESCAPE
                ? ArcadePortRules.dragonEscapeWorldTime(sourceMapName)
                : ArcadePortRules.sourceWorldTime(type, sourceMapName));
        world.setStorm(false);
        world.setThundering(false);
        WorldBorder border = world.getWorldBorder();
        if (sourceMap != null && sourceMap.hasBounds()) {
            border.setCenter(sourceMap.centerX(), sourceMap.centerZ());
            border.setSize(Math.max(40.0D, Math.max(sourceMap.width(), sourceMap.depth()) + 8.0D));
        } else {
            border.setCenter(0.0D, 0.0D);
            border.setSize(type == ArcadePortGame.MICRO_BATTLES ? 42.0D : 80.0D);
        }
        border.setDamageAmount(0.0D);
        List<Location> spawns = spawnLocations(type, participants.size());
        for (int i = 0; i < participants.size(); i++) {
            Player player = participants.get(i);
            PlayerSnapshot snapshot = pendingRestore.remove(player.getUniqueId());
            if (snapshot == null) snapshot = new PlayerSnapshot(player);
            session.snapshots.put(player.getUniqueId(), snapshot);
            session.players.put(player.getUniqueId(), player);
            session.alive.add(player.getUniqueId());
            session.scores.put(player.getUniqueId(), 0);
            if (type == ArcadePortGame.PAINTBALL) {
                session.paintPiecesRemaining.put(player.getUniqueId(), 4);
                session.paintballKits.put(player.getUniqueId(),
                        paintballKitPreferences.getOrDefault(player.getUniqueId(), "rifle"));
            }
            if (type == ArcadePortGame.DEATH_TAG) {
                session.deathTagRunnerKits.put(player.getUniqueId(),
                        deathTagRunnerKitPreferences.getOrDefault(player.getUniqueId(), "basher"));
            }
            if (type == ArcadePortGame.SPLEEF) {
                UUID playerId = player.getUniqueId();
                session.spleefKits.put(playerId, spleefKitPreferences.getOrDefault(playerId, "snowballer"));
                session.spleefNextArrowAt.put(playerId, now + ArcadePortRules.SPLEEF_ARCHER_ARROW_INTERVAL_MILLIS);
            }
            if (type == ArcadePortGame.EVOLUTION) {
                session.evolutionLevels.put(player.getUniqueId(), 0);
                session.evolutionLifeKills.put(player.getUniqueId(), 0);
            }
            if (type == ArcadePortGame.DRAGON_ESCAPE) {
                UUID playerId = player.getUniqueId();
                String kit = dragonEscapeKitPreferences.getOrDefault(playerId, "jumper");
                session.dragonEscapeKits.put(playerId, kit);
                session.dragonEscapeLeapUsesRemaining.put(playerId, ArcadePortRules.dragonEscapeLeapUses(kit));
                session.dragonEscapeDiggerUsesRemaining.put(playerId, ArcadePortRules.DRAGON_ESCAPE_DIGGER_MAX_USES);
                session.dragonEscapeNextTntAt.put(playerId,
                        now + ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS);
            }
            int teamCount = type == ArcadePortGame.MICRO_BATTLES || type == ArcadePortGame.SHEEP_QUEST ? 4 : 2;
            boolean teamMode = type == ArcadePortGame.MICRO_BATTLES || type == ArcadePortGame.SHEEP_QUEST
                    || type == ArcadePortGame.BOMB_LOBBERS || type == ArcadePortGame.PAINTBALL
                    || type == ArcadePortGame.TURF_WARS;
            SourceMapCatalog.MapData selectedMap = sourceMapData.get(type.id());
            int teamId = teamMode && selectedMap != null
                    ? selectedMap.teamIndexForSpawn(i, teamCount)
                    : teamMode ? i % teamCount : i % 2;
            session.teams.put(player.getUniqueId(), teamId);
            if (type == ArcadePortGame.BACON_BRAWL) {
                session.baconKits.put(player.getUniqueId(), baconKitPreferences.getOrDefault(player.getUniqueId(), "pig"));
            }
            if (type == ArcadePortGame.BOMB_LOBBERS) {
                session.bombLobbersKits.put(player.getUniqueId(),
                        bombLobbersKitPreferences.getOrDefault(player.getUniqueId(), "jumper"));
                session.bombPitchLevels.put(player.getUniqueId(), 2);
                session.bombDoubleJumpAvailable.add(player.getUniqueId());
            }
            if (type == ArcadePortGame.MICRO_BATTLES) {
                session.microKits.put(player.getUniqueId(), microKitPreferences.getOrDefault(player.getUniqueId(), "archer"));
                session.microNextArrowAt.put(player.getUniqueId(),
                        now + ArcadePortRules.MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS);
            }
            if (type == ArcadePortGame.ONE_IN_THE_QUIVER) {
                session.quiverKits.put(player.getUniqueId(), quiverKitPreferences.getOrDefault(player.getUniqueId(), "leaper"));
            }
            if (type == ArcadePortGame.RUNNER) {
                UUID playerId = player.getUniqueId();
                session.runnerKits.put(playerId, runnerKitPreferences.getOrDefault(playerId, "leaper"));
                session.runnerNextFrostBallAt.put(playerId, now + ArcadePortRules.RUNNER_FROSTBALL_INTERVAL_MILLIS);
            }
            if (type == ArcadePortGame.DRAGONS) {
                UUID playerId = player.getUniqueId();
                session.dragonKits.put(playerId, dragonsKitPreferences.getOrDefault(playerId, "coward"));
                session.dragonNextSparklerAt.put(playerId, now + ArcadePortRules.DRAGONS_SPARKLER_INTERVAL_MILLIS);
                session.dragonNextArrowAt.put(playerId, now + ArcadePortRules.DRAGONS_MARKSMAN_ARROW_INTERVAL_MILLIS);
            }
            Location spawn = spawns.get(i % spawns.size()).clone();
            if (type == ArcadePortGame.MICRO_BATTLES && !sourceMapWorlds.contains(type.id())) {
                spawn = microTeamSpawn(teamId, i / 4, (participants.size() + 3) / 4);
            } else if (type == ArcadePortGame.TURF_WARS && !sourceMapWorlds.contains(type.id())) {
                spawn = turfWarsSpawn(session, session.teams.get(player.getUniqueId()), i / 2,
                        (participants.size() + 1) / 2);
            }
            player.closeInventory();
            player.leaveVehicle();
            player.teleport(spawn);
            player.setGameMode(type == ArcadePortGame.BACON_BRAWL ? GameMode.ADVENTURE : GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setFireTicks(0);
            player.setExp(0.0F);
            player.setLevel(0);
            player.getInventory().clear();
            giveKit(session, player);
            session.lastLocations.put(player.getUniqueId(), spawn.clone());
        }
        if (type == ArcadePortGame.DEATH_TAG) {
            if (participants.size() > 1) {
                Player chaser = participants.get(ThreadLocalRandom.current().nextInt(participants.size()));
                session.tagged.add(chaser.getUniqueId());
                chaser.sendMessage(ChatColor.RED + "You are IT! Tag every runner.");
                for (Player p : participants) if (p != chaser) p.sendMessage(ChatColor.GREEN + "Run! Avoid the tagged player.");
            } else {
                // Solo mechanics test: use an isolated vanilla chaser so Death Tag is still interactive.
                Player runner = participants.get(0);
                Location chaserSpawn = deathTagSoloChaserSpawn(session, runner);
                Zombie chaser = session.world.spawn(chaserSpawn, Zombie.class);
                chaser.setCustomName(ChatColor.RED + "Death Tag Test Chaser");
                chaser.setCustomNameVisible(true);
                chaser.setRemoveWhenFarAway(false);
                chaser.setTarget(runner);
                session.entities.add(chaser);
                runner.sendMessage(ChatColor.YELLOW + "Solo test: avoid the chaser; one hit ends the run.");
            }
        }
        if (type == ArcadePortGame.DEATH_TAG) {
            for (Player player : participants) {
                giveDeathTagKit(session, player, session.tagged.contains(player.getUniqueId()),
                        session.alphaChasers.contains(player.getUniqueId()));
            }
        }
        if (type == ArcadePortGame.SHEEP_QUEST) {
            for (int i = 0; i < ArcadePortRules.SHEEP_QUEST_INITIAL_SHEEP; i++) spawnSheep(session);
        }
        if (type == ArcadePortGame.DRAGONS) spawnTestDragons(session);
        if (type == ArcadePortGame.GLADIATORS && participants.size() > 1) {
            startGladiatorsRound(session, alivePlayers(session), true);
        }
        if (type == ArcadePortGame.MICRO_BATTLES) announce(session, ChatColor.AQUA + "Micro Battles: gather blocks; glass barriers drop after 10 seconds.");
        else announce(session, ChatColor.GOLD + type.displayName() + " started! This is a first-pass mechanics test.");
        if (type == ArcadePortGame.TURF_WARS) {
            announce(session, ChatColor.GREEN + "Opening build phase: place your team's wool on your own turf for 40 seconds.");
        }
        if (participants.size() == 1) announce(session, ChatColor.GRAY + "Solo test mode: the run will remain available for practice until the timer ends or you reset it.");
        session.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(session), 1L, 1L);
        sender.sendMessage(ChatColor.GREEN + "Started " + type.displayName() + " with " + participants.size() + (participants.size() == 1 ? " player." : " players."));
    }

    private World createWorld(ArcadePortGame type) {
        String sourceWorldName = "arcadeport_source_" + type.id().replace('-', '_');
        World world = Bukkit.getWorld(sourceWorldName);
        if (world == null && SourceMapCatalog.hasMap(type)
                && SourceMapCatalog.install(plugin, type, sourceWorldName)) {
            WorldCreator creator = new WorldCreator(sourceWorldName)
                    .environment(World.Environment.NORMAL).generateStructures(false);
            world = creator.createWorld();
        }
        if (world != null && SourceMapCatalog.hasMap(type) && !sourceMapWorlds.contains(type.id())) {
            SourceMapCatalog.MapData mapData = SourceMapCatalog.read(world);
            sourceMapWorlds.add(type.id());
            if (mapData != null) {
                sourceMapData.put(type.id(), mapData);
                plugin.getLogger().info("Loaded original " + type.displayName() + " map: "
                        + mapData.mapName() + " (" + mapData.spawnLocations().size() + " configured spawn locations).");
            } else {
                plugin.getLogger().warning("Loaded original terrain without parsed WorldConfig metadata for "
                        + type.displayName() + "; using fallback spawn locations.");
            }
        }
        if (world == null) {
            String worldName = "arcadeport_proto_" + type.id().replace('-', '_');
            world = Bukkit.getWorld(worldName);
            if (world == null) {
                WorldCreator creator = new WorldCreator(worldName);
                creator.type(WorldType.FLAT).generateStructures(false);
                world = creator.createWorld();
            }
        }
        if (world == null) {
            plugin.getLogger().severe("Could not create world for " + type.displayName());
            return null;
        }
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        if (!sourceMapWorlds.contains(type.id())) world.setSpawnLocation(0, 64, 0);
        return world;
    }

    private void buildArena(ArcadePortGame type, World world) {
        int minX = type == ArcadePortGame.DRAGON_ESCAPE ? -10 : -HALF_SIZE - 2;
        int maxX = type == ArcadePortGame.DRAGON_ESCAPE ? 10 : HALF_SIZE + 2;
        int minZ = type == ArcadePortGame.DRAGON_ESCAPE ? -26 : -HALF_SIZE - 2;
        int maxZ = type == ArcadePortGame.DRAGON_ESCAPE ? 82 : HALF_SIZE + 2;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 62; y <= 67; y++) world.getBlockAt(x, y, z).setType(Material.AIR, false);
            }
        }
        if (type == ArcadePortGame.DRAGON_ESCAPE) {
            for (int z = -24; z <= 78; z += 5) {
                for (int x = -3; x <= 3; x++) {
                    for (int dz = 0; dz <= 2; dz++) world.getBlockAt(x, 63, z + dz).setType(Material.STONE_BRICKS, false);
                }
            }
            return;
        }
        if (type == ArcadePortGame.GLADIATORS) {
            for (int slot = 0; slot < 8; slot++) buildGladiatorArena(world, slot, 0);
            return;
        }
        if (type == ArcadePortGame.MICRO_BATTLES) {
            // Four team islands, each ringed by a two-block glass barrier for the opening countdown.
            for (int team = 0; team < 4; team++) {
                Location island = islandSpawn(team * 2);
                int cx = island.getBlockX();
                int cz = island.getBlockZ();
                for (int x = cx - 3; x <= cx + 3; x++) {
                    for (int z = cz - 3; z <= cz + 3; z++) {
                        world.getBlockAt(x, 63, z).setType(type.floor(), false);
                    }
                }
                for (int offset = -4; offset <= 4; offset++) {
                    for (int y = 64; y <= 65; y++) {
                        world.getBlockAt(cx + offset, y, cz - 4).setType(Material.GLASS, false);
                        world.getBlockAt(cx + offset, y, cz + 4).setType(Material.GLASS, false);
                        world.getBlockAt(cx - 4, y, cz + offset).setType(Material.GLASS, false);
                        world.getBlockAt(cx + 4, y, cz + offset).setType(Material.GLASS, false);
                    }
                }
            }
            return;
        }
        boolean walls = type == ArcadePortGame.BOMB_LOBBERS || type == ArcadePortGame.DEATH_TAG
                || type == ArcadePortGame.DRAGONS || type == ArcadePortGame.GLADIATORS
                || type == ArcadePortGame.PAINTBALL || type == ArcadePortGame.SHEEP_QUEST
                || type == ArcadePortGame.TURF_WARS;
        for (int x = -HALF_SIZE; x <= HALF_SIZE; x++) {
            for (int z = -HALF_SIZE; z <= HALF_SIZE; z++) {
                Material floor = type == ArcadePortGame.TURF_WARS
                        ? (x <= -1 ? Material.RED_TERRACOTTA : Material.BLUE_TERRACOTTA) : type.floor();
                world.getBlockAt(x, 63, z).setType(floor, false);
                if (walls && (Math.abs(x) == HALF_SIZE || Math.abs(z) == HALF_SIZE)) {
                    for (int y = 64; y <= 66; y++) world.getBlockAt(x, y, z).setType(Material.BLACKSTONE, false);
                }
            }
        }
        if (type == ArcadePortGame.SHEEP_QUEST) buildSheepPens(world);
    }

    private List<Location> spawnLocations(ArcadePortGame type, int count) {
        SourceMapCatalog.MapData sourceMap = sourceMapData.get(type.id());
        if (sourceMap != null && type == ArcadePortGame.DRAGON_ESCAPE) {
            // Reborn defines Dragon Escape player starts as GREEN data points, not team spawns.
            List<Location> authoredGreenSpawns = sourceMap.dataLocations("GREEN");
            if (authoredGreenSpawns.isEmpty()) authoredGreenSpawns = sourceMap.spawnLocations();
            if (!authoredGreenSpawns.isEmpty()) {
                List<Location> chosen = new ArrayList<>();
                for (int i = 0; i < Math.max(1, count); i++) {
                    chosen.add(authoredGreenSpawns.get(i % authoredGreenSpawns.size()).clone());
                }
                return chosen;
            }
        }
        if (sourceMap != null && !sourceMap.spawnLocations().isEmpty()) {
            List<Location> configured = sourceMap.spawnLocations();
            List<Location> chosen = new ArrayList<>();
            for (int i = 0; i < Math.max(1, count); i++) chosen.add(configured.get(i % configured.size()).clone());
            return chosen;
        }
        List<Location> locations = new ArrayList<>();
        if (type == ArcadePortGame.DRAGON_ESCAPE) {
            for (int i = 0; i < Math.max(1, count); i++) locations.add(new Location(worlds.get(type.id()), (i % 5) - 2.0D, 64.0D, -22.0D + (i / 5) * 2, 0.0F, 0.0F));
            return locations;
        }
        World world = worlds.get(type.id());
        for (int i = 0; i < Math.max(1, count); i++) {
            double angle = 2.0D * Math.PI * i / Math.max(1, count);
            double radius = type == ArcadePortGame.MICRO_BATTLES ? 13.0D : 7.0D;
            locations.add(new Location(world, Math.cos(angle) * radius, 64.0D, Math.sin(angle) * radius, (float)(angle * 180.0D / Math.PI + 90.0D), 0.0F));
        }
        return locations;
    }

    private Location gladiatorArenaCenter(World world, int arenaSlot) {
        int x = -15 + (Math.floorMod(arenaSlot, 4) * 10);
        int z = arenaSlot < 4 ? -10 : 10;
        return new Location(world, x + 0.5D, 64.0D, z + 0.5D);
    }

    private boolean hasGladiatorSourceArenas(Session session) {
        SourceMapCatalog.MapData data = sourceMapData.get(session.type.id());
        return data != null
                && !data.dataLocations(ArcadePortRules.gladiatorsArenaMarkerGroup(session.gladiatorRound)).isEmpty()
                && data.dataLocations("BLACK").size() >= 2;
    }

    /** Prefer the actual arena centres in the selected Reborn map; generated centres are test fallback only. */
    private List<Location> gladiatorArenaCenters(Session session) {
        if (hasGladiatorSourceArenas(session)) {
            return sourceMapData.get(session.type.id())
                    .dataLocations(ArcadePortRules.gladiatorsArenaMarkerGroup(session.gladiatorRound));
        }
        List<Location> fallback = new ArrayList<>();
        for (int i = 0; i < 8; i++) fallback.add(gladiatorArenaCenter(session.world, i));
        return fallback;
    }

    /** Reborn marks both combat positions with BLACK; each arena uses its two nearest spawn markers. */
    private Location[] gladiatorArenaSpawnPair(Session session, Location center, boolean useSourceMap) {
        if (useSourceMap) {
            List<Location> candidates = new ArrayList<>(sourceMapData.get(session.type.id()).dataLocations("BLACK"));
            candidates.sort((left, right) -> Double.compare(
                    left.distanceSquared(center), right.distanceSquared(center)));
            Location first = candidates.get(0).clone();
            Location second = candidates.get(1).clone();
            first.setDirection(center.toVector().subtract(first.toVector()));
            second.setDirection(center.toVector().subtract(second.toVector()));
            first.setPitch(0.0F);
            second.setPitch(0.0F);
            return new Location[]{first, second};
        }
        Location left = center.clone().add(-2.0D, 0.0D, 0.0D);
        Location right = center.clone().add(2.0D, 0.0D, 0.0D);
        left.setYaw(-90.0F);
        right.setYaw(90.0F);
        left.setPitch(0.0F);
        right.setPitch(0.0F);
        return new Location[]{left, right};
    }

    private void buildGladiatorArena(World world, int arenaSlot, int tier) {
        Location center = gladiatorArenaCenter(world, arenaSlot);
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        Material floor = gladiatorFloor(tier);
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                world.getBlockAt(x, 63, z).setType(floor, false);
                boolean wall = Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4;
                if (wall) for (int y = 64; y <= 66; y++) {
                    world.getBlockAt(x, y, z).setType(Material.BLACKSTONE, false);
                }
            }
        }
    }

    private Material gladiatorFloor(int tier) {
        return switch (Math.max(0, Math.min(3, tier))) {
            case 1 -> Material.YELLOW_CONCRETE;
            case 2 -> Material.ORANGE_CONCRETE;
            case 3 -> Material.RED_CONCRETE;
            default -> Material.GREEN_CONCRETE;
        };
    }

    private void giveGladiatorKit(Session session, Player player) {
        int tier = Math.max(0, Math.min(3, session.gladiatorRound));
        Material sword = switch (tier) {
            case 1 -> Material.STONE_SWORD;
            case 2 -> Material.IRON_SWORD;
            case 3 -> Material.DIAMOND_SWORD;
            default -> Material.WOODEN_SWORD;
        };
        Material[] armor = switch (tier) {
            case 1 -> new Material[]{Material.GOLDEN_BOOTS, Material.GOLDEN_LEGGINGS, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_HELMET};
            case 2 -> new Material[]{Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET};
            case 3 -> new Material[]{Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET};
            default -> new Material[]{Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET};
        };
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(sword), new ItemStack(Material.FISHING_ROD),
                new ItemStack(Material.BOW), new ItemStack(Material.ARROW, new int[]{3, 5, 7, 10}[tier]));
        ItemStack[] armorItems = new ItemStack[4];
        for (int i = 0; i < armorItems.length; i++) {
            armorItems[i] = new ItemStack(armor[i]);
            if (armorItems[i].getItemMeta() instanceof LeatherArmorMeta meta) {
                Color color = switch (tier) {
                    case 1 -> Color.YELLOW;
                    case 2 -> Color.ORANGE;
                    case 3 -> Color.RED;
                    default -> Color.LIME;
                };
                meta.setColor(color);
                armorItems[i].setItemMeta(meta);
            }
        }
        player.getInventory().setArmorContents(armorItems);
        player.updateInventory();
    }

    private void startGladiatorsRound(Session session, List<Player> advancing, boolean initial) {
        if (active != session || advancing.isEmpty()) return;
        if (advancing.size() == 1) {
            finish(session, advancing.get(0), true);
            return;
        }
        session.gladiatorRound = initial
                ? ArcadePortRules.gladiatorsStartingTierForPlayers(advancing.size())
                : ArcadePortRules.gladiatorsTierForRound(session.gladiatorRound + 1);
        session.gladiatorPairs.clear();
        session.gladiatorResolvedPairs.clear();
        session.gladiatorPairCount = ArcadePortRules.gladiatorsPairCount(advancing.size());
        session.nextGladiatorRoundAt = 0L;
        session.gladiatorRoundStartedAt = System.currentTimeMillis();
        session.gladiatorFightStartsAt = session.gladiatorRoundStartedAt + ArcadePortRules.GLADIATOR_ROUND_COUNTDOWN_MILLIS;
        session.lastGladiatorPoisonAt = session.gladiatorRoundStartedAt + ArcadePortRules.GLADIATOR_POISON_DELAY_MILLIS;
        Collections.shuffle(advancing);

        String roundName = switch (session.gladiatorRound) {
            case 1 -> "Yellow arena";
            case 2 -> "Orange arena";
            case 3 -> "Red finals";
            default -> "Green arena";
        };
        announce(session, ChatColor.GOLD + "Gladiators — " + roundName + ". Defeat your opponent to advance!");

        boolean useSourceMapArenas = hasGladiatorSourceArenas(session);
        List<Location> arenaCenters = gladiatorArenaCenters(session);

        for (int i = 0; i < advancing.size(); i++) {
            Player player = advancing.get(i);
            int pair = i / 2;
            int arenaSlot = pair % arenaCenters.size();
            session.gladiatorPairs.put(player.getUniqueId(), pair);
            if (!useSourceMapArenas) buildGladiatorArena(session.world, arenaSlot, session.gladiatorRound);
            Location center = arenaCenters.get(arenaSlot);
            boolean left = i % 2 == 0;
            Location[] combatSpawns = gladiatorArenaSpawnPair(session, center, useSourceMapArenas);
            Location spawn = combatSpawns[left ? 0 : 1].clone();
            player.leaveVehicle();
            player.teleport(spawn);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setFireTicks(0);
            player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            giveGladiatorKit(session, player);
            session.lastLocations.put(player.getUniqueId(), spawn.clone());
        }
        if (advancing.size() % 2 == 1) {
            int byePair = session.gladiatorPairCount - 1;
            session.gladiatorResolvedPairs.add(byePair);
            Player bye = advancing.get(advancing.size() - 1);
            bye.sendMessage(ChatColor.GREEN + "You have a bye and automatically advance to the next round.");
        }
    }

    private void updateGladiators(Session session, long now) {
        if (active != session) return;
        if (now >= session.lastGladiatorPoisonAt) {
            session.lastGladiatorPoisonAt = now + ArcadePortRules.GLADIATOR_POISON_INTERVAL_MILLIS;
            for (Player player : new ArrayList<>(alivePlayers(session))) {
                UUID playerId = player.getUniqueId();
                Integer pair = session.gladiatorPairs.get(playerId);
                if (pair == null || !ArcadePortRules.shouldGladiatorPoison(now,
                        session.gladiatorRoundStartedAt, session.gladiatorResolvedPairs.contains(pair),
                        aliveGladiatorPairMembers(session, pair))) continue;
                if (player.getHealth() <= ArcadePortRules.GLADIATOR_POISON_DAMAGE) {
                    eliminate(session, player, null, "was defeated by arena poison");
                } else {
                    player.setHealth(player.getHealth() - ArcadePortRules.GLADIATOR_POISON_DAMAGE);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7F, 0.7F);
                    player.sendActionBar(ChatColor.RED + "POISON! Keep fighting!");
                }
                if (active != session) return;
            }
        }
        if (session.nextGladiatorRoundAt > 0L && now >= session.nextGladiatorRoundAt) {
            if (session.alive.size() <= 1) {
                finish(session, alivePlayers(session).stream().findFirst().orElse(null), true);
                return;
            }
            startGladiatorsRound(session, alivePlayers(session), false);
        }
    }

    private int aliveGladiatorPairMembers(Session session, int pair) {
        int count = 0;
        for (UUID id : session.alive) {
            if (session.gladiatorPairs.get(id) != null && session.gladiatorPairs.get(id) == pair) count++;
        }
        return count;
    }

    private Location islandSpawn(int index) {
        World world = worlds.get(ArcadePortGame.MICRO_BATTLES.id());
        double angle = 2.0D * Math.PI * Math.floorMod(index, 8) / 8.0D;
        return new Location(world, Math.cos(angle) * 13.0D, 64.0D, Math.sin(angle) * 13.0D, (float)(angle * 180.0D / Math.PI + 90.0F), 0.0F);
    }

    private Location microTeamCenter(Session session, int team) {
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null) {
            List<Location> markers = mapData.teamSpawnLocationsForTeam(team, 4);
            if (!markers.isEmpty()) {
                double x = 0.0D, y = 0.0D, z = 0.0D;
                for (Location marker : markers) {
                    x += marker.getX();
                    y += marker.getY();
                    z += marker.getZ();
                }
                return new Location(session.world, x / markers.size(), y / markers.size(), z / markers.size());
            }
        }
        return islandSpawn(team * 2);
    }

    private Location microTeamSpawn(int team, int slot, int slots) {
        Location center = islandSpawn(team * 2);
        if (slots > 1) {
            double angle = 2.0D * Math.PI * slot / slots;
            center.add(Math.cos(angle) * 1.4D, 0.0D, Math.sin(angle) * 1.4D);
        }
        return center;
    }

    private void giveDeathTagKit(Session session, Player player, boolean chaser, boolean alphaChaser) {
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        if (chaser) {
            player.getInventory().addItem(namedItem(Material.IRON_AXE,
                    alphaChaser ? "Alpha Chaser" : "Chaser",
                    alphaChaser ? "Deal 6 damage and resist 4 damage per attack." : "Deal 4 damage and resist 2 damage per attack."));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 4, false, false, true));
            if (alphaChaser) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 1, false, false, true));
            }
        } else {
            String kit = session.deathTagRunnerKits.getOrDefault(player.getUniqueId(), "basher");
            switch (kit) {
                case "archer" -> {
                    player.getInventory().addItem(namedItem(Material.BOW, "Runner Archer", "Use knockback arrows to keep chasers away."));
                    player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow", "Replenishes over time.", 2));
                }
                case "traitor" -> player.getInventory().addItem(namedItem(Material.IRON_AXE,
                        "Runner Traitor", "Heavy knockback on melee hits."));
                case "null" -> { /* Reborn's NullKit has no kit items. */ }
                default -> player.getInventory().addItem(namedItem(Material.IRON_SWORD,
                        "Runner Basher", "Melee hits cripple chasers with Slowness III."));
            }
        }
        player.updateInventory();
    }

    private void updateDeathTagLights(Session session, long now) {
        if (session.deathTagOriginalLightBlocks.isEmpty() || now < session.deathTagNextLightAt) return;
        session.deathTagNextLightAt = now + 1_000L;
        for (Block block : session.deathTagOriginalLightBlocks.keySet()) {
            if (block == null || block.getWorld() != session.world) continue;
            block.setType(ArcadePortRules.deathTagNextLightMaterial(block.getType()), false);
        }
    }

    private void updateDeathTagConditions(Session session, long now) {
        long tagged = session.tagged.stream().filter(session.alive::contains).count();
        boolean runnersHaveSpeed = ArcadePortRules.deathTagRunnersReceiveSpeed((int) tagged, session.alive.size());
        for (Player player : alivePlayers(session)) {
            if (session.tagged.contains(player.getUniqueId())) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 4, false, false, true));
                if (!runnersHaveSpeed) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, true));
                } else {
                    player.removePotionEffect(PotionEffectType.SPEED);
                }
            } else if (runnersHaveSpeed) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, true));
            } else {
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }
        for (Player player : alivePlayers(session)) {
            if (!session.tagged.contains(player.getUniqueId())
                    && "archer".equals(session.deathTagRunnerKits.getOrDefault(player.getUniqueId(), "basher"))) {
                Long lastArrow = session.deathTagLastArrowAt.get(player.getUniqueId());
                if (lastArrow == null || now - lastArrow >= 2_000L) {
                    session.deathTagLastArrowAt.put(player.getUniqueId(), now);
                    int arrows = inventoryCount(player, Material.ARROW);
                    if (arrows < 2) {
                        player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow", "Replenished by the Archer kit."));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6F, 1.3F);
                    }
                }
            }
        }
    }

    private Location deathTagSoloChaserSpawn(Session session, Player runner) {
        Location runnerLocation = runner.getLocation();
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null) {
            Location otherSpawn = mapData.spawnLocations().stream()
                    .filter(spawn -> horizontalDistanceSquared(spawn, runnerLocation) >= 36.0D)
                    .max(java.util.Comparator.comparingDouble(spawn ->
                            horizontalDistanceSquared(spawn, runnerLocation)))
                    .orElse(null);
            if (otherSpawn != null) return otherSpawn.clone();
        }

        double direction = runnerLocation.getX() <= 0.0D ? 1.0D : -1.0D;
        double x = runnerLocation.getX() + direction * 8.0D;
        double z = runnerLocation.getZ();
        if (mapData != null && mapData.hasBounds()) {
            x = Math.max(mapData.minX() + 1.0D, Math.min(mapData.maxX() - 1.0D, x));
            z = Math.max(mapData.minZ() + 1.0D, Math.min(mapData.maxZ() - 1.0D, z));
        }
        return new Location(session.world, x, runnerLocation.getY(), z,
                runnerLocation.getYaw() + 180.0F, 0.0F);
    }

    private void makeDeathTagChaser(Session session, Player player, String reason) {
        if (active != session || player == null || !session.alive.contains(player.getUniqueId())) return;
        boolean forcedAlpha = reason != null && reason.contains("forced by the time limit");
        boolean newlyTagged = session.tagged.add(player.getUniqueId());
        if (forcedAlpha) session.alphaChasers.add(player.getUniqueId());
        else if (newlyTagged) session.alphaChasers.remove(player.getUniqueId());
        if (player.isOnline()) {
            if (player.getLocation().getY() < 59.0D) player.teleport(spawnFor(session, player));
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setFireTicks(0);
            player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            giveDeathTagKit(session, player, true, session.alphaChasers.contains(player.getUniqueId()));
            if (newlyTagged) {
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.0F, 1.0F);
                player.sendMessage(ChatColor.RED + "You are now a Chaser!");
                session.world.spawnParticle(Particle.FLAME, player.getLocation().add(0.0D, 1.0D, 0.0D),
                        18, 0.35D, 0.5D, 0.35D, 0.04D);
            }
        }
        if (newlyTagged) announce(session, ChatColor.RED + player.getName() + ChatColor.GRAY + " became a Chaser (" + reason + ").");
    }

    private void updateDeathTagChasers(Session session, long now) {
        int requiredChasers = Math.min(session.alive.size(),
                ArcadePortRules.requiredDeathTagChasers(now - session.startedAt));
        long taggedAlive = session.tagged.stream().filter(session.alive::contains).count();
        if (taggedAlive < requiredChasers) {
            List<Player> runners = alivePlayers(session).stream()
                    .filter(player -> !session.tagged.contains(player.getUniqueId()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            while (taggedAlive < requiredChasers && !runners.isEmpty()) {
                Player promoted = runners.remove(ThreadLocalRandom.current().nextInt(runners.size()));
                makeDeathTagChaser(session, promoted, "was forced by the time limit");
                promoted.sendMessage(ChatColor.YELLOW + "Time is running out! Tag every remaining runner.");
                taggedAlive++;
            }
        }
        updateDeathTagConditions(session, now);
    }

    private void updateMicroBarrier(Session session, long now) {
        if (!session.microBarrierActive || !ArcadePortRules.shouldRemoveMicroBarrier(now - session.startedAt)) return;
        if (!session.microBarrierBlocks.isEmpty()) {
            for (Block block : session.microBarrierBlocks) {
                if (block.getType() == Material.GLASS) block.setType(Material.AIR, false);
            }
            session.microBarrierBlocks.clear();
        } else for (int team = 0; team < 4; team++) {
            SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
            Location island = mapData == null ? islandSpawn(team * 2) : microTeamCenter(session, team);
            int cx = island.getBlockX();
            int cy = island.getBlockY();
            int cz = island.getBlockZ();
            for (int offset = -4; offset <= 4; offset++) {
                for (int y = cy; y <= ArcadePortRules.microBattlesBarrierTopY(cy); y++) {
                    Block[] ring = {
                            session.world.getBlockAt(cx + offset, y, cz - 4),
                            session.world.getBlockAt(cx + offset, y, cz + 4),
                            session.world.getBlockAt(cx - 4, y, cz + offset),
                            session.world.getBlockAt(cx + 4, y, cz + offset)
                    };
                    for (Block block : ring) {
                        if (block.getType() == Material.GLASS) block.setType(Material.AIR, false);
                    }
                }
            }
        }
        session.microBarrierActive = false;
        announce(session, ChatColor.AQUA + "Glass barriers are down! Fight for your team.");
    }

    private Location turfWarsSpawn(Session session, int team, int slot, int slots) {
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null) {
            List<Location> teamSpawns = mapData.teamSpawnLocationsForTeam(team, 2);
            if (!teamSpawns.isEmpty()) return teamSpawns.get(Math.floorMod(slot, teamSpawns.size())).clone();
        }
        return turfWarsSpawn(session.world, team, slot, slots);
    }

    private Location turfWarsSpawn(World world, int team, int slot, int slots) {
        double z = Math.max(-14.0D, Math.min(14.0D, (slot - (slots - 1) / 2.0D) * 4.0D));
        boolean red = team == 0;
        return new Location(world, red ? -15.0D : 15.0D, 64.0D, z, red ? -90.0F : 90.0F, 0.0F);
    }

    private void giveTurfWarsWool(Session session, Player player) {
        int team = session.teams.getOrDefault(player.getUniqueId(), -1);
        if (team < 0) return;
        Material wool = team == 0 ? Material.RED_WOOL : Material.BLUE_WOOL;
        player.getInventory().addItem(new ItemStack(wool, 24));
    }

    private void giveKit(Session session, Player player) {
        player.getInventory().clear();
        switch (session.type) {
            case BACON_BRAWL: {
                String kit = session.baconKits.getOrDefault(player.getUniqueId(), "pig");
                switch (kit) {
                    case "mama-pig":
                        player.getInventory().addItem(namedItem(Material.IRON_AXE, "Mama Pig - Bacon Blast", "Right-click to throw a bacon blast (3s cooldown)."));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false, false));
                        break;
                    case "sheep-pig":
                        player.getInventory().addItem(namedItem(Material.IRON_AXE, "Sheep Pig - Cloak", "Right-click to cloak for 5s (10s cooldown). Backstab for bonus knockback."));
                        break;
                    default:
                        player.getInventory().addItem(namedItem(Material.IRON_AXE, "Pig - Body Slam", "Right-click to leap and body-slam nearby opponents (8s cooldown)."));
                        break;
                }
                break;
            }
            case BOMB_LOBBERS: {
                String kit = session.bombLobbersKits.getOrDefault(player.getUniqueId(), "jumper");
                player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT", "Left-click to throw at enemy players.", 1));
                switch (kit) {
                    case "armorer":
                        break;
                    case "pitcher":
                        player.getInventory().setItem(1, namedItem(Material.LEVER, "Velocity Selector",
                                "Right-click to increase throw power; left-click to decrease.", 2));
                        break;
                    case "waller":
                        player.getInventory().setItem(1, namedItem(Material.STONE_SHOVEL, "Wall Builder",
                                "Right-click a block to build a 5x3 stone-brick wall.", 3));
                        break;
                    default:
                        break;
                }
                setBombLobbersArmor(session, player, kit);
                player.setAllowFlight(true);
                player.setFlying(false);
                break;
            }
            case DEATH_TAG:
                giveDeathTagKit(session, player, false, false);
                break;
            case DRAGON_ESCAPE: {
                UUID id = player.getUniqueId();
                String kit = session.dragonEscapeKits.getOrDefault(id, "jumper");
                int leaps = session.dragonEscapeLeapUsesRemaining.getOrDefault(id, ArcadePortRules.dragonEscapeLeapUses(kit));
                if (!"digger".equals(kit)) {
                    player.getInventory().addItem(namedItem(Material.IRON_AXE, "Leap",
                            "Right-click to leap forward (" + leaps + " uses remaining).", 1));
                }
                if ("disruptor".equals(kit)) {
                    player.getInventory().addItem(namedItem(Material.TNT, "Mini Explosive",
                            "Right-click to throw a trip mine. Recharges every 10 seconds.", 1));
                } else if ("warper".equals(kit)) {
                    player.getInventory().addItem(namedItem(Material.ENDER_PEARL, "Warp",
                            "Right-click to teleport to the nearest player ahead. One use."));
                } else if ("digger".equals(kit)) {
                    int digs = session.dragonEscapeDiggerUsesRemaining.getOrDefault(id, ArcadePortRules.DRAGON_ESCAPE_DIGGER_MAX_USES);
                    player.getInventory().addItem(namedItem(Material.DIAMOND_PICKAXE, "Digger Pickaxe",
                            "Left-click course blocks to recover and place as shortcuts (" + digs + " digs left)."));
                }
                break;
            }
            case DRAGONS: {
                String kit = session.dragonKits.getOrDefault(player.getUniqueId(), "coward");
                player.getInventory().setArmorContents(null);
                if ("marksman".equals(kit)) {
                    player.getInventory().addItem(new ItemStack(Material.BOW));
                    player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow",
                            "Replenishes up to four arrows every four seconds.", ArcadePortRules.DRAGONS_MARKSMAN_MAX_ARROWS));
                    player.getInventory().setArmorContents(new ItemStack[] {
                            new ItemStack(Material.CHAINMAIL_BOOTS), new ItemStack(Material.CHAINMAIL_LEGGINGS),
                            new ItemStack(Material.CHAINMAIL_CHESTPLATE), new ItemStack(Material.CHAINMAIL_HELMET) });
                } else if ("pyrotechnic".equals(kit)) {
                    player.getInventory().addItem(namedItem(Material.EMERALD, "Throwing Sparkler",
                            "Right-click to throw a sparkler that attracts nearby dragons.",
                            ArcadePortRules.DRAGONS_MAX_SPARKLERS));
                    player.getInventory().setArmorContents(new ItemStack[] {
                            new ItemStack(Material.GOLDEN_BOOTS), new ItemStack(Material.GOLDEN_LEGGINGS),
                            new ItemStack(Material.GOLDEN_CHESTPLATE), new ItemStack(Material.GOLDEN_HELMET) });
                } else {
                    player.getInventory().addItem(namedItem(Material.IRON_AXE, "Leap",
                            "Right-click to leap forward (8-second cooldown)."));
                    player.getInventory().setArmorContents(new ItemStack[] {
                            new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS),
                            new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET) });
                }
                break;
            }
            case EVOLUTION:
                player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                break;
            case GLADIATORS:
                // The tiered tournament loadout is applied when its arena round starts.
                player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                player.getInventory().addItem(new ItemStack(Material.FISHING_ROD));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 3));
                break;
            case MICRO_BATTLES: {
                String kit = session.microKits.getOrDefault(player.getUniqueId(), "archer");
                switch (kit) {
                    case "worker":
                        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                        player.getInventory().addItem(new ItemStack(Material.STONE_SHOVEL));
                        player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
                        player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
                        player.getInventory().addItem(new ItemStack(Material.APPLE, 4));
                        break;
                    case "fighter":
                        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                        player.getInventory().addItem(new ItemStack(Material.APPLE, 5));
                        break;
                    default:
                        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                        player.getInventory().addItem(new ItemStack(Material.BOW));
                        player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow",
                                "Automatically replenishes up to 3 arrows every 20 seconds.",
                                ArcadePortRules.MICRO_BATTLES_FLETCHER_MAX_ARROWS));
                        player.getInventory().addItem(new ItemStack(Material.APPLE, 3));
                        break;
                }
                setMicroTeamArmor(session, player);
                break;
            }
            case ONE_IN_THE_QUIVER: {
                String kit = session.quiverKits.getOrDefault(player.getUniqueId(), "leaper");
                player.getInventory().setItem(0, new ItemStack(Material.BOW));
                switch (kit) {
                    case "brawler":
                        player.getInventory().setItem(2, new ItemStack(Material.IRON_SWORD));
                        break;
                    case "enchanter": {
                        ItemStack sword = new ItemStack(Material.STONE_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
                        player.getInventory().setItem(2, sword);
                        break;
                    }
                    case "slam-shot":
                        player.getInventory().setItem(2, new ItemStack(Material.DIAMOND_SHOVEL));
                        break;
                    case "ninja": {
                        ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
                        ItemMeta meta = sword.getItemMeta();
                        if (meta != null) {
                            meta.setUnbreakable(true);
                            sword.setItemMeta(meta);
                        }
                        player.getInventory().setItem(2, sword);
                        break;
                    }
                    default:
                        player.getInventory().setItem(2, new ItemStack(Material.STONE_SWORD));
                        break;
                }
                player.getInventory().setItem(1, namedItem(Material.ARROW, "Super Arrow",
                        "One arrow per shot; earn another with each kill."));
                break;
            }
            case PAINTBALL:
                givePaintballKit(session, player,
                        session.paintballKits.getOrDefault(player.getUniqueId(), "rifle"));
                break;
            case RUNNER: {
                String kit = session.runnerKits.getOrDefault(player.getUniqueId(), "leaper");
                switch (kit) {
                    case "archer":
                        player.getInventory().addItem(new ItemStack(Material.BOW));
                        break;
                    case "frosty":
                        // PerkConstructor supplies Frost Balls on its 500 ms recharge cycle.
                        break;
                    default:
                        player.getInventory().addItem(namedItem(Material.IRON_AXE, "Leap",
                                "Right-click to leap (8-second cooldown)."));
                        break;
                }
                break;
            }
            case SHEEP_QUEST:
                player.getInventory().addItem(namedItem(Material.SADDLE, "Shepherd", "Hold to pick up sheep; carry them to your team's pen."));
                player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 8));
                break;
            case SPLEEF: {
                String kit = session.spleefKits.getOrDefault(player.getUniqueId(), "snowballer");
                switch (kit) {
                    case "brawler":
                        player.getInventory().addItem(namedItem(Material.IRON_AXE, "Leaper Axe",
                                "Right-click to leap. Damaging a floor block also breaks its neighbours."));
                        break;
                    case "archer":
                        player.getInventory().addItem(new ItemStack(Material.BOW));
                        player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow",
                                "A replenishing Spleef arrow.", ArcadePortRules.SPLEEF_ARCHER_MAX_ARROWS));
                        break;
                    default:
                        player.getInventory().addItem(namedItem(Material.SNOWBALL, "Snowball",
                                "Break floor tiles at range. Regained snowballs are capped at 16.", 1));
                        break;
                }
                break;
            }
            case TURF_WARS:
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
                giveTurfWarsWool(session, player);
                break;
        }
        player.updateInventory();
    }

    private void updateQuiverKit(Session session, long now) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            String kit = session.quiverKits.getOrDefault(id, "leaper");
            if ("leaper".equals(kit)) {
                boolean awaitingLanding = session.quiverLeapAwaitingLanding.contains(id);
                if (ArcadePortRules.shouldRearmQuiverLeap(
                        player.isOnGround(), awaitingLanding, player.getVelocity().getY())) {
                    session.quiverLeapAwaitingLanding.remove(id);
                    if (!player.getAllowFlight()) player.setAllowFlight(true);
                }
            }
            Long cloakedUntil = session.quiverCloakedUntil.get(id);
            if (cloakedUntil != null && now >= cloakedUntil) clearQuiverCloak(session, id);
        }
    }

    private void clearQuiverCloak(Session session, UUID playerId) {
        session.quiverCloakedUntil.remove(playerId);
        Player player = session.players.get(playerId);
        if (player != null && player.isOnline()) player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private void activateQuiverNinjaVanish(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        if (!"ninja".equals(session.quiverKits.getOrDefault(id, "leaper"))) return;
        session.quiverCloakedUntil.put(id, now + ArcadePortRules.QUIVER_NINJA_VANISH_MILLIS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 24, 0, false, false, true));
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0.0D, 1.0D, 0.0D),
                10, 0.25D, 0.4D, 0.25D, 0.02D);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.6F);
    }

    private void useDragonsLeap(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        long readyAt = session.dragonLeapReadyAt.getOrDefault(id, 0L);
        if (now < readyAt) {
            player.sendMessage(ChatColor.YELLOW + "Leap ready in "
                    + Math.max(1L, (readyAt - now + 999L) / 1000L) + "s.");
            return;
        }
        session.dragonLeapReadyAt.put(id, now + ArcadePortRules.DRAGONS_COWARD_LEAP_COOLDOWN_MILLIS);
        Vector direction = player.getLocation().getDirection().normalize().multiply(1.2D);
        direction.setY(Math.max(0.2D, Math.min(1.0D, direction.getY() + 0.8D)));
        player.setVelocity(direction);
        player.setFallDistance(0.0F);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10,
                0.2D, 0.15D, 0.2D, 0.02D);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8F, 1.1F);
        player.sendMessage(ChatColor.GREEN + "Leap!");
    }

    private void useQuiverGroundPound(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        long readyAt = session.quiverGroundPoundReadyAt.getOrDefault(id, 0L);
        if (now < readyAt) {
            player.sendMessage(ChatColor.YELLOW + "Ground Pound ready in "
                    + Math.max(1L, (readyAt - now + 999L) / 1000L) + "s.");
            return;
        }
        session.quiverGroundPoundReadyAt.put(id, now + ArcadePortRules.QUIVER_GROUND_POUND_COOLDOWN_MILLIS);
        for (Player target : alivePlayers(session)) {
            if (target.equals(player)) continue;
            double distance = target.getLocation().distance(player.getLocation());
            if (distance > 8.0D) continue;
            Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector());
            direction.setY(0.0D); // Reborn uses a 2D trajectory for horizontal knockback.
            if (direction.lengthSquared() < 0.001D) {
                direction = player.getLocation().getDirection();
                direction.setY(0.0D);
            }
            double proximity = ArcadePortRules.quiverGroundPoundHorizontalMultiplier(distance);
            if (direction.lengthSquared() < 0.001D) direction = new Vector(0.0D, 0.0D, 1.0D);
            target.setVelocity(direction.normalize().multiply(0.8D * proximity)
                    .setY(0.8D + 1.0D * proximity));
            target.setFallDistance(0.0F);
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 0.8F);
        }
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1,
                0.15D, 0.1D, 0.15D, 0.0D);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.7F);
        player.sendMessage(ChatColor.GREEN + "Ground Pound!");
    }

    private void handleQuiverMeleeHit(Session session, Player attacker, Player target) {
        String kit = session.quiverKits.getOrDefault(attacker.getUniqueId(), "leaper");
        double damage = ArcadePortRules.quiverMeleeDamage(kit);
        if (ArcadePortRules.shouldRemoveQuiverInvisibilityOnHit(true, "ninja".equals(kit))) {
            clearQuiverCloak(session, attacker.getUniqueId());
        }
        if (target.getHealth() <= damage) {
            scoreQuiverKill(session, attacker, target);
            return;
        }
        target.setHealth(target.getHealth() - damage);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
    }

    private void tryQuiverRebound(Session session, Projectile projectile, Player shooter, Player struckPlayer) {
        Integer bounces = session.quiverArrowBounces.remove(projectile.getUniqueId());
        if (!"enchanter".equals(session.quiverKits.getOrDefault(shooter.getUniqueId(), "leaper"))
                || bounces == null || !ArcadePortRules.shouldQuiverArrowBounce(bounces)
                || active != session || !struckPlayer.isOnline()) return;
        Location origin = struckPlayer.getEyeLocation().clone();
        Player nearest = null;
        double nearestDistance = ArcadePortRules.QUIVER_REBOUND_MAX_DISTANCE_SQUARED;
        for (Player candidate : alivePlayers(session)) {
            if (candidate.equals(shooter) || candidate.equals(struckPlayer)) continue;
            double distance = candidate.getLocation().distanceSquared(origin);
            if (ArcadePortRules.isQuiverReboundTargetInRange(distance) && distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }

        Vector trajectory;
        if (nearest != null) {
            trajectory = nearest.getEyeLocation().toVector().subtract(origin.toVector());
        } else {
            // The source Enchanter chain arrow still bounces when no nearby enemy is available.
            double angle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
            double vertical = ThreadLocalRandom.current().nextDouble(-0.12D, 0.22D);
            trajectory = new Vector(Math.cos(angle), vertical, Math.sin(angle));
        }
        if (trajectory.lengthSquared() < 0.001D) {
            double angle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
            trajectory = new Vector(Math.cos(angle), 0.05D, Math.sin(angle));
        }
        Arrow rebound = session.world.spawnArrow(origin, trajectory.normalize(), 1.2F, 0.0F);
        rebound.setShooter(shooter);
        session.entities.add(rebound);
        session.quiverArrowBounces.put(rebound.getUniqueId(),
                ArcadePortRules.quiverArrowBouncesAfterBounce(bounces));
    }

    private Location microCrumbleSpectatorLocation(Session session, SourceMapCatalog.MapData mapData) {
        if (mapData != null) {
            List<Location> markers = mapData.customLocations("SPECTATOR");
            if (markers.isEmpty()) markers = mapData.dataLocations("SPECTATOR");
            if (!markers.isEmpty()) return markers.get(0).clone();
            if (mapData.hasBounds()) {
                double y = mapData.hasVerticalBounds()
                        ? Math.max(mapData.minY() + 8.0D, mapData.maxY() - 5.0D)
                        : session.world.getSpawnLocation().getY() + 10.0D;
                return new Location(session.world, mapData.centerX(), y, mapData.centerZ());
            }
        }
        return session.world.getSpawnLocation().clone().add(0.0D, 10.0D, 0.0D);
    }

    private void updateMicroCrumble(Session session, long now) {
        if (active != session || session.type != ArcadePortGame.MICRO_BATTLES) return;

        // Source MapCrumbleModule scans the arena vertically in two-tick slices.
        if (!session.microCrumbleScanComplete
                && session.ticks % ArcadePortRules.MICRO_BATTLES_CRUMBLE_SCAN_INTERVAL_TICKS == 0) {
            int y = session.microCrumbleScanY;
            for (int x = session.microCrumbleMinX; x < session.microCrumbleMaxX; x++) {
                for (int z = session.microCrumbleMinZ; z < session.microCrumbleMaxZ; z++) {
                    Block block = session.world.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) session.microCrumbleBlocks.add(block);
                }
            }
            session.microCrumbleScanY++;
            if (ArcadePortRules.isMicroCrumbleScanComplete(session.microCrumbleScanY, session.microCrumbleMaxY)) {
                session.microCrumbleScanComplete = true;
                plugin.getLogger().info("Micro Battles source map scan completed with "
                        + session.microCrumbleBlocks.size() + " crumble candidates.");
            }
        }

        // Reborn starts crumbling after one second and removes three blocks per tick,
        // prioritising blocks furthest from the map's spectator/centre location.
        if (now - session.startedAt < 1_000L || session.microCrumbleBlocks.isEmpty()) return;
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        Location spectatorCenter = microCrumbleSpectatorLocation(session, mapData);
        double centerX = spectatorCenter.getX();
        double centerZ = spectatorCenter.getZ();
        for (int i = 0; i < ArcadePortRules.MICRO_BATTLES_CRUMBLE_BLOCKS_PER_TICK; i++) {
            Block best = null;
            double farthest = -1.0D;
            for (Block candidate : session.microCrumbleBlocks) {
                double dx = candidate.getX() + 0.5D - centerX;
                double dz = candidate.getZ() + 0.5D - centerZ;
                double distanceSquared = dx * dx + dz * dz;
                if (best == null || distanceSquared > farthest) {
                    best = candidate;
                    farthest = distanceSquared;
                }
            }
            if (best == null) break;

            while (best.getY() > session.world.getMinHeight()
                    && !best.getRelative(BlockFace.DOWN).getType().isAir()) {
                best = best.getRelative(BlockFace.DOWN);
            }
            session.microCrumbleBlocks.remove(best);
            if (best.getWorld() != spectatorCenter.getWorld() || best.getType().isAir()) continue;

            Material material = best.getType();
            if (material == Material.OAK_DOOR || material == Material.IRON_DOOR) {
                best.getRelative(BlockFace.UP).setType(Material.AIR, false);
            } else if (ThreadLocalRandom.current().nextDouble() > 0.95D) {
                FallingBlock falling = session.world.spawnFallingBlock(
                        best.getLocation().add(0.5D, 0.5D, 0.5D), best.getBlockData());
                falling.setDropItem(false);
                session.entities.add(falling);
            }
            best.setType(Material.AIR, false);
        }
    }

    private int countNamedItems(Player player, Material material, String expectedName) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material || !item.hasItemMeta()
                    || item.getItemMeta().getDisplayName() == null) continue;
            String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (display != null && display.contains(expectedName)) count += item.getAmount();
        }
        return count;
    }

    private void updateMicroBattlesArrows(Session session, long now) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (!"archer".equals(session.microKits.getOrDefault(id, "archer"))) continue;
            long next = session.microNextArrowAt.getOrDefault(id,
                    now + ArcadePortRules.MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS);
            int count = countNamedItems(player, Material.ARROW, "Fletched Arrow");
            if (now < next) continue;
            if (ArcadePortRules.shouldReplenishMicroArrow(now, next, count,
                    ArcadePortRules.MICRO_BATTLES_FLETCHER_MAX_ARROWS)) {
                player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow",
                        "Automatically replenishes up to 3 arrows every 20 seconds.", 1));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.0F);
            }
            session.microNextArrowAt.put(id, now + ArcadePortRules.MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS);
        }
    }

    private void setBombLobbersArmor(Session session, Player player, String kit) {
        Material boots;
        Material leggings;
        Material chestplate;
        Material helmet;
        if ("armorer".equals(kit)) {
            boots = Material.IRON_BOOTS;
            leggings = Material.IRON_LEGGINGS;
            chestplate = Material.IRON_CHESTPLATE;
            helmet = Material.IRON_HELMET;
        } else if ("pitcher".equals(kit)) {
            boots = Material.CHAINMAIL_BOOTS;
            leggings = Material.CHAINMAIL_LEGGINGS;
            chestplate = Material.CHAINMAIL_CHESTPLATE;
            helmet = Material.CHAINMAIL_HELMET;
        } else {
            boots = Material.LEATHER_BOOTS;
            leggings = Material.LEATHER_LEGGINGS;
            chestplate = Material.LEATHER_CHESTPLATE;
            helmet = Material.LEATHER_HELMET;
        }
        ItemStack[] armor = new ItemStack[] {
                new ItemStack(boots), new ItemStack(leggings),
                new ItemStack(chestplate), new ItemStack(helmet)
        };
        if (!"armorer".equals(kit) && !"pitcher".equals(kit)) {
            int team = session.teams.getOrDefault(player.getUniqueId(), 0);
            Color color = team == 0 ? Color.RED : Color.BLUE;
            for (ItemStack piece : armor) {
                if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
                    meta.setColor(color);
                    piece.setItemMeta(meta);
                }
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    private double bombLobbersDamageAfterArmor(Player target, double damage) {
        var armorAttribute = target.getAttribute(Attribute.ARMOR);
        double armorPoints = armorAttribute == null ? 0.0D : armorAttribute.getValue();
        return ArcadePortRules.bombLobbersDamageAfterArmor(damage, armorPoints);
    }

    private void setMicroTeamArmor(Session session, Player player) {
        Color teamColor = switch (session.teams.getOrDefault(player.getUniqueId(), 0)) {
            case 0 -> Color.RED;
            case 1 -> Color.AQUA;
            case 2 -> Color.LIME;
            default -> Color.YELLOW;
        };
        ItemStack[] armor = new ItemStack[] {
                new ItemStack(Material.LEATHER_BOOTS),
                new ItemStack(Material.LEATHER_LEGGINGS),
                new ItemStack(Material.LEATHER_CHESTPLATE),
                new ItemStack(Material.LEATHER_HELMET)
        };
        for (ItemStack piece : armor) {
            if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
                meta.setColor(teamColor);
                piece.setItemMeta(meta);
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    private void enforceMicroBattlesHunger(Session session) {
        for (Player player : alivePlayers(session)) {
            int minimum = ArcadePortRules.microBattlesMinimumFoodLevel(player.getFoodLevel());
            if (player.getFoodLevel() != minimum) player.setFoodLevel(minimum);
        }
    }

    private void applyMicroBattlesHit(Session session, Player attacker, Player target, String source) {
        if (active != session || !session.alive.contains(attacker.getUniqueId())
                || !session.alive.contains(target.getUniqueId())) return;
        boolean projectile = !"melee".equals(source);
        boolean fighter = "fighter".equals(session.microKits.getOrDefault(target.getUniqueId(), "archer"));
        double damage = ArcadePortRules.microBattlesDamageAfterIronSkin(
                ArcadePortRules.microBattlesHitDamage(projectile), fighter);
        if (projectile) {
            Vector push = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
            if (push.lengthSquared() < 0.0001D) push = attacker.getLocation().getDirection();
            target.setVelocity(push.normalize().multiply(1.6D).setY(0.35D));
        }
        if (target.getHealth() <= damage) {
            eliminate(session, target, attacker, "was eliminated in Micro Battles");
            return;
        }
        target.setHealth(target.getHealth() - damage);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0D, 1.0D, 0.0D),
                5, 0.25D, 0.35D, 0.25D, 0.05D);
    }

    private void givePaintballKit(Session session, Player player, String kit) {
        player.getInventory().clear();
        String safeKit = kit == null ? "rifle" : kit;
        Material weapon;
        String title;
        String lore;
        switch (safeKit) {
            case "sniper" -> {
                weapon = Material.STONE_HOE;
                title = "Paintball Sniper Rifle";
                lore = "Right-click to fire a charged long-range paint arrow. Sneak to scope.";
            }
            case "shotgun" -> {
                weapon = Material.GOLDEN_HORSE_ARMOR;
                title = "Paintball Shotgun";
                lore = "Right-click to fire eight scattered paint pellets.";
            }
            case "machine-gun" -> {
                weapon = Material.DIAMOND_HORSE_ARMOR;
                title = "Paintball Machine Gun";
                lore = "Right-click rapidly; watch the experience bar for overheating.";
            }
            default -> {
                safeKit = "rifle";
                weapon = Material.IRON_HORSE_ARMOR;
                title = "Paintball Rifle";
                lore = "Right-click to fire a fast paint shot that paints three armour pieces.";
            }
        }
        session.paintballKits.put(player.getUniqueId(), safeKit);
        player.getInventory().setItem(0, namedItem(weapon, title, lore));
        int waterBombs = "sniper".equals(safeKit) ? 2 : 3;
        player.getInventory().setItem(1, namedItem(Material.SPLASH_POTION, "Water Bomb",
                "Clean painted teammates or revive an eliminated teammate near the SAVE ME marker.", waterBombs));
        setPaintballArmor(session, player);
        player.setExp(0.0F);
        player.setSneaking(false);
        player.updateInventory();
    }

    private boolean isPaintballWaterBomb(ItemStack item) {
        if (item == null || item.getType() != Material.SPLASH_POTION
                || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return "Water Bomb".equals(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
    }

    private String paintballKitForItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()
                || item.getItemMeta().getDisplayName() == null) return null;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if ("Paintball Rifle".equals(name)) return "rifle";
        if ("Paintball Sniper Rifle".equals(name)) return "sniper";
        if ("Paintball Shotgun".equals(name)) return "shotgun";
        if ("Paintball Machine Gun".equals(name)) return "machine-gun";
        return null;
    }

    private void firePaintballWeapon(Session session, Player player, String kit, long now) {
        UUID playerId = player.getUniqueId();
        if (!kit.equals(session.paintballKits.getOrDefault(playerId, "rifle"))) return;
        long cooldown = switch (kit) {
            case "sniper" -> 1_000L;
            case "shotgun" -> 1_400L;
            case "machine-gun" -> 80L;
            default -> 500L;
        };
        Long lastShot = session.paintballLastShotAt.get(playerId);
        if (lastShot != null && now - lastShot < cooldown) return;
        if ("machine-gun".equals(kit) && player.getExp() >= 0.97F) {
            player.sendMessage(ChatColor.RED + "Machine gun overheated. Release fire and let the gauge cool.");
            return;
        }
        session.paintballLastShotAt.put(playerId, now);
        if ("machine-gun".equals(kit)) player.setExp(Math.min(0.999F, player.getExp() + 0.025F));

        if ("sniper".equals(kit)) {
            int paintAmount = ArcadePortRules.paintballPaintAmount(kit, true, player.getExp());
            Arrow arrow = player.launchProjectile(Arrow.class);
            arrow.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(15.0D));
            session.entities.add(arrow);
            session.paintProjectileDamage.put(arrow.getUniqueId(), paintAmount);
            player.setExp(0.0F);
            player.setSneaking(false);
            player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 0.8F, 1.0F);
            return;
        }

        int shots = "shotgun".equals(kit) ? 8 : 1;
        int paintAmount = ArcadePortRules.paintballPaintAmount(kit, false, 0.0D);
        for (int i = 0; i < shots; i++) {
            Projectile projectile;
            boolean redTeam = session.teams.getOrDefault(playerId, 0) == 0;
            if (redTeam) projectile = player.launchProjectile(EnderPearl.class);
            else projectile = player.launchProjectile(Snowball.class);

            Vector velocity = projectile.getVelocity();
            if ("rifle".equals(kit)) {
                velocity.multiply(2.0D);
            } else if ("shotgun".equals(kit)) {
                velocity.add(new Vector(ThreadLocalRandom.current().nextDouble(-0.2D, 0.2D),
                        ThreadLocalRandom.current().nextDouble(-0.2D, 0.2D),
                        ThreadLocalRandom.current().nextDouble(-0.2D, 0.2D)));
            } else {
                velocity.multiply(1.6D).add(new Vector(ThreadLocalRandom.current().nextDouble(-0.125D, 0.125D),
                        ThreadLocalRandom.current().nextDouble(-0.125D, 0.125D),
                        ThreadLocalRandom.current().nextDouble(-0.125D, 0.125D)));
            }
            projectile.setVelocity(velocity);
            session.entities.add(projectile);
            session.paintProjectileDamage.put(projectile.getUniqueId(), paintAmount);
        }
        float pitch = "shotgun".equals(kit) ? 0.9F : "machine-gun".equals(kit) ? 1.8F : 1.3F;
        player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.2F, pitch);
    }

    private void updatePaintballKitState(Session session, long now) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            String kit = session.paintballKits.getOrDefault(id, "rifle");
            if ("sniper".equals(kit)) {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (player.isSneaking() && held.getType() == Material.STONE_HOE) {
                    player.setExp(Math.min(0.999F, player.getExp() + 0.03332F));
                } else if (player.getExp() > 0.0F && !player.isSneaking()) {
                    player.setExp(0.0F);
                }
            } else if ("machine-gun".equals(kit)) {
                Long lastShot = session.paintballLastShotAt.get(id);
                if (lastShot == null || now - lastShot >= 250L) {
                    player.setExp(Math.max(0.0F, player.getExp() - 0.02F));
                }
            } else if (player.getExp() != 0.0F) {
                player.setExp(0.0F);
            }
        }
    }

    private void setPaintballArmor(Session session, Player player) {
        Color teamColor = session.teams.getOrDefault(player.getUniqueId(), 0) == 0 ? Color.RED : Color.AQUA;
        ItemStack[] armor = new ItemStack[] {
                new ItemStack(Material.LEATHER_BOOTS),
                new ItemStack(Material.LEATHER_LEGGINGS),
                new ItemStack(Material.LEATHER_CHESTPLATE),
                new ItemStack(Material.LEATHER_HELMET)
        };
        for (ItemStack piece : armor) {
            if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
                meta.setColor(teamColor);
                piece.setItemMeta(meta);
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    private ArmorStand spawnPaintballSaveMeMarker(Session session, Player player) {
        Location location = player.getLocation().clone();
        ArmorStand marker = session.world.spawn(location, ArmorStand.class);
        marker.setArms(true);
        marker.setBasePlate(false);
        marker.setGravity(false);
        marker.setInvulnerable(true);
        marker.setSilent(true);
        marker.setCanPickupItems(false);
        marker.setCustomName(ChatColor.RED + "SAVE ME! " + player.getName());
        marker.setCustomNameVisible(true);
        Color teamColor = session.teams.getOrDefault(player.getUniqueId(), 0) == 0 ? Color.RED : Color.AQUA;
        ItemStack[] armor = new ItemStack[] {
                new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS),
                new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET)
        };
        for (ItemStack piece : armor) {
            if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
                meta.setColor(teamColor);
                piece.setItemMeta(meta);
            }
        }
        marker.getEquipment().setBoots(armor[0]);
        marker.getEquipment().setLeggings(armor[1]);
        marker.getEquipment().setChestplate(armor[2]);
        marker.getEquipment().setHelmet(armor[3]);
        session.entities.add(marker);
        session.paintballSaveMeMarkers.put(player.getUniqueId(), marker);
        session.paintballSaveMeLocations.put(player.getUniqueId(), location.clone());
        return marker;
    }

    private void removePaintballSaveMeMarker(Session session, UUID outPlayer) {
        ArmorStand marker = session.paintballSaveMeMarkers.remove(outPlayer);
        session.paintballSaveMeLocations.remove(outPlayer);
        if (marker != null) {
            session.entities.remove(marker);
            if (marker.isValid()) marker.remove();
        }
    }

    private void paintballWaterBombImpact(Session session, Player thrower, Location impact) {
        if (active != session || session.type != ArcadePortGame.PAINTBALL || thrower == null
                || impact == null || !session.alive.contains(thrower.getUniqueId())) return;
        UUID throwerId = thrower.getUniqueId();
        int throwerTeam = session.teams.getOrDefault(throwerId, -1);
        if (throwerTeam < 0) return;

        List<UUID> revive = new ArrayList<>();
        for (Map.Entry<UUID, ArmorStand> entry : new ArrayList<>(session.paintballSaveMeMarkers.entrySet())) {
            UUID targetId = entry.getKey();
            ArmorStand marker = entry.getValue();
            if (marker == null || !marker.isValid()) {
                removePaintballSaveMeMarker(session, targetId);
                continue;
            }
            if (session.teams.getOrDefault(targetId, -2) != throwerTeam) continue;
            if (marker.getLocation().clone().add(0.0D, 1.0D, 0.0D).distanceSquared(impact)
                    <= ArcadePortRules.PAINTBALL_SAVE_ME_RADIUS * ArcadePortRules.PAINTBALL_SAVE_ME_RADIUS) {
                revive.add(targetId);
            }
        }

        for (UUID targetId : revive) {
            if (active != session) return;
            Player target = session.players.get(targetId);
            ArmorStand marker = session.paintballSaveMeMarkers.get(targetId);
            Location respawn = marker == null ? session.paintballSaveMeLocations.get(targetId) : marker.getLocation().clone();
            removePaintballSaveMeMarker(session, targetId);
            if (target == null || !target.isOnline() || respawn == null) continue;
            session.respawning.remove(targetId);
            session.alive.add(targetId);
            target.leaveVehicle();
            target.setSpectatorTarget(null);
            target.teleport(respawn);
            target.setGameMode(GameMode.ADVENTURE);
            target.setAllowFlight(false);
            target.setFlying(false);
            target.setInvulnerable(false);
            target.setFireTicks(0);
            target.setHealth(20.0D);
            target.setFoodLevel(20);
            target.setSaturation(20.0F);
            session.paintPiecesRemaining.put(targetId, 4);
            givePaintballKit(session, target, session.paintballKits.getOrDefault(targetId, "rifle"));
            target.sendMessage(ChatColor.GREEN + "You were revived by " + thrower.getName() + "!");
            thrower.sendMessage(ChatColor.GREEN + "Revived " + target.getName() + "!");
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.2F);
            session.lastLocations.put(targetId, respawn.clone());
        }

        for (Player teammate : alivePlayers(session)) {
            if (session.teams.getOrDefault(teammate.getUniqueId(), -2) != throwerTeam) continue;
            if (!ArcadePortRules.shouldPaintballWaterBombAffectPlayer(
                    teammate.getLocation().distanceSquared(impact), true)) continue;
            UUID teammateId = teammate.getUniqueId();
            session.paintPiecesRemaining.put(teammateId, 4);
            setPaintballArmor(session, teammate);
            teammate.setHealth(20.0D);
            teammate.sendMessage(ChatColor.AQUA + "Water Bomb cleaned your paint!");
            teammate.playSound(teammate.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 0.7F, 1.25F);
        }
        thrower.playSound(impact, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.25F);
        impact.getWorld().spawnParticle(Particle.SPLASH, impact, 24, 0.5D, 0.35D, 0.5D, 0.04D);
        checkRoundEnd(session);
    }

    private int applyPaintballHit(Session session, Player attacker, Player target, int paintAmount) {
        UUID targetId = target.getUniqueId();
        int before = session.paintPiecesRemaining.getOrDefault(targetId, 4);
        int after = ArcadePortRules.paintballPiecesAfterHit(before, paintAmount);
        if (after < before) {
            ItemStack[] armor = target.getInventory().getArmorContents();
            List<Integer> available = new ArrayList<>();
            for (int i = 0; i < armor.length; i++) {
                ItemStack piece = armor[i];
                if (piece == null || !(piece.getItemMeta() instanceof LeatherArmorMeta meta)) continue;
                Color color = meta.getColor();
                if (Color.RED.equals(color) || Color.AQUA.equals(color)) available.add(i);
            }
            int piecesToPaint = Math.min(Math.max(0, paintAmount), available.size());
            for (int i = 0; i < piecesToPaint; i++) {
                int choice = ThreadLocalRandom.current().nextInt(available.size());
                int armorIndex = available.remove(choice);
                ItemStack piece = armor[armorIndex];
                LeatherArmorMeta meta = (LeatherArmorMeta) piece.getItemMeta();
                meta.setColor(Color.PURPLE);
                piece.setItemMeta(meta);
            }
            target.getInventory().setArmorContents(armor);
            after = Math.max(0, before - piecesToPaint);
            session.paintPiecesRemaining.put(targetId, after);
            target.setHealth(ArcadePortRules.paintballHealthAfterHit(after));
        }
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7F, 1.8F);
        target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation().add(0.0D, 1.0D, 0.0D),
                12, 0.35D, 0.4D, 0.35D, 0.03D);
        // Reborn's Paintball hit confirmation is the high-pitched classic orb pickup.
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 3.0F);
        attacker.sendMessage(ChatColor.YELLOW + "Paint hit! " + after + " armour pieces remaining.");
        if (ArcadePortRules.isPaintballPlayerOut(after)) {
            announce(session, ChatColor.RED + target.getName() + ChatColor.GRAY + " was painted out by "
                    + ChatColor.GREEN + attacker.getName() + ChatColor.GRAY + "!");
            // eliminate() records the kill score once and checks the last-team-standing condition.
            eliminate(session, target, attacker, "was completely painted");
        }
        return after;
    }

    private ItemStack namedItem(Material type, String title, String lore) {
        return namedItem(type, title, lore, 1);
    }

    private ItemStack namedItem(Material type, String title, String lore, int amount) {
        ItemStack item = new ItemStack(type, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + title);
            meta.setLore(List.of(ChatColor.GRAY + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void tick(Session session) {
        if (active != session) {
            session.tickTask.cancel();
            return;
        }
        long now = System.currentTimeMillis();
        session.ticks++;
        updateSourceWaterDamage(session, now);
        if (now >= session.endsAt) {
            if (session.type == ArcadePortGame.DEATH_TAG && session.participantsAtStart > 1) {
                // Apply any chaser promotions due on the final timer tick before choosing a winner.
                updateDeathTagChasers(session, now);
                if (active != session) return;
            }
            if (session.type == ArcadePortGame.SHEEP_QUEST) {
                // Most sheep in the pen at five minutes wins. A tied result is a draw, not overtime.
                finish(session, sheepQuestLeader(session), true);
            } else {
                finish(session, timeoutWinner(session), true);
            }
            return;
        }
        if (session.type == ArcadePortGame.TURF_WARS) updateTurfWarsPhase(session, now);
        if (session.type == ArcadePortGame.MICRO_BATTLES) updateMicroBarrier(session, now);
        if (session.type == ArcadePortGame.GLADIATORS) updateGladiators(session, now);
        if (session.type == ArcadePortGame.MICRO_BATTLES) {
            enforceMicroBattlesHunger(session);
            updateMicroBattlesArrows(session, now);
            updateMicroCrumble(session, now);
        }
        if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER) updateQuiverKit(session, now);
        if (session.type == ArcadePortGame.DRAGONS) updateTestDragons(session, now);
        if (session.type == ArcadePortGame.DEATH_TAG && session.participantsAtStart > 1) {
            updateDeathTagChasers(session, now);
        }
        if (session.type == ArcadePortGame.DEATH_TAG) updateDeathTagLights(session, now);
        if (session.type == ArcadePortGame.BACON_BRAWL) updateBaconBrawl(session, now);
        if (session.type == ArcadePortGame.BOMB_LOBBERS) updateBombLobbers(session);
        if (session.type == ArcadePortGame.SPLEEF) updateSpleef(session, now);
        if (session.type == ArcadePortGame.EVOLUTION) {
            updateEvolutionProgress(session, now);
            updateEvolutionWebs(session, now);
        }
        if (session.type == ArcadePortGame.RUNNER) {
            // Reborn marks the whole standing footprint on every game tick, not only on movement events.
            markRunnerFootprints(session, now);
            processRunnerBlocks(session, now);
            updateRunnerKits(session, now);
        }
        if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
            updateDragonEscapeResources(session, now);
            updateDragonEscapeMines(session);
            updateDragonEscapeChase(session);
        }
        if (session.type == ArcadePortGame.SHEEP_QUEST) {
            updateSheepQuest(session);
            if (now >= lastSheepSpawn && session.sheep.size() < 12) {
                spawnSheep(session);
                lastSheepSpawn = now + ArcadePortRules.SHEEP_QUEST_SPAWN_INTERVAL_MILLIS;
            }
        }

        if (session.type == ArcadePortGame.PAINTBALL) updatePaintballKitState(session, now);
        if (session.type == ArcadePortGame.TURF_WARS && session.turfCombat && session.ticks % 40 == 0) {
            for (UUID id : session.alive) {
                Player player = session.players.get(id);
                if (player != null && player.isOnline() && !player.getInventory().contains(Material.ARROW, 4)) {
                    player.getInventory().addItem(new ItemStack(Material.ARROW, 4));
                }
            }
        }
         updateTitles(session);
        checkRoundEnd(session);
    }

    private void updateSourceWaterDamage(Session session, long now) {
        SourceMapCatalog.MapData map = sourceMapData.get(session.type.id());
        int damage = ArcadePortRules.sourceWorldWaterDamage(session.type, map == null ? "" : map.mapName());
        if (damage <= 0 || now - session.lastWorldWaterDamageAt < 1_000L) return;
        session.lastWorldWaterDamageAt = now;
        for (Player player : new ArrayList<>(alivePlayers(session))) {
            if (!player.isOnline()) continue;
            boolean inWater = player.getLocation().getBlock().isLiquid()
                    || player.getEyeLocation().getBlock().isLiquid();
            if (!inWater) continue;
            if (player.getHealth() <= damage) {
                eliminate(session, player, null, "was lost to the water");
                if (active != session) return;
            } else {
                player.setHealth(player.getHealth() - damage);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 0.7F);
            }
        }
    }

    private void updateBaconBrawl(Session session, long now) {
        if (now - session.lastBaconHungerAt >= ArcadePortRules.BACON_HUNGER_INTERVAL_MILLIS) {
            session.lastBaconHungerAt = now;
            for (Player player : new ArrayList<>(alivePlayers(session))) {
                if (player.getFoodLevel() > 0) {
                    player.setFoodLevel(Math.max(0, player.getFoodLevel() - ArcadePortRules.BACON_HUNGER_DRAIN_PER_INTERVAL));
                } else if (player.getHealth() <= ArcadePortRules.BACON_STARVATION_DAMAGE) {
                    eliminate(session, player, null, "starved");
                    if (active != session) return;
                } else player.setHealth(player.getHealth() - ArcadePortRules.BACON_STARVATION_DAMAGE);
            }
        }
        if (active != session) return;
        updateBaconBrawlAbilities(session, now);
        if (active != session || session.baconEscalationIntervalMillis <= 0L
                || now - session.lastBaconEscalationAt < session.baconEscalationIntervalMillis) return;
        session.lastBaconEscalationAt = now;
        session.baconKnockback = ArcadePortRules.nextBaconKnockback(session.baconKnockback);
        session.baconEscalationIntervalMillis = ArcadePortRules.nextBaconKnockbackInterval(session.baconEscalationIntervalMillis);
        String escalationMessage = ChatColor.GREEN
                + "The ground begins to tremble... All pigs now take more knockback!";
        announce(session, escalationMessage);
        for (Player player : alivePlayers(session)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 0.7F);
        }
    }

    private void updateBaconBrawlAbilities(Session session, long now) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
            if ("mama-pig".equals(session.baconKits.getOrDefault(id, "pig"))
                    && (session.ticks % 20 == 0 || speed == null || speed.getDuration() < 20)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, false));
            }
            Long cloakedUntil = session.baconCloakedUntil.get(id);
            if (cloakedUntil != null && now >= cloakedUntil) clearBaconCloak(session, id);
        }
        for (Map.Entry<UUID, Long> entry : new ArrayList<>(session.baconSlams.entrySet())) {
            UUID slammerId = entry.getKey();
            Player slammer = session.players.get(slammerId);
            long expiresAt = entry.getValue();
            if (slammer == null || !slammer.isOnline() || !session.alive.contains(slammerId)
                    || now >= expiresAt || (expiresAt - now < 800L && slammer.isOnGround())) {
                session.baconSlams.remove(slammerId);
                continue;
            }
            for (Player target : alivePlayers(session)) {
                if (!target.equals(slammer) && target.getLocation().distanceSquared(slammer.getLocation()) < 4.0D) {
                    session.baconSlams.remove(slammerId);
                    handlePlayerHit(session, slammer, target, "body-slam");
                    break;
                }
            }
            if (active != session) return;
        }
    }

    private void useBaconBrawlAbility(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        String kit = session.baconKits.getOrDefault(id, "pig");
        long cooldown = switch (kit) {
            case "mama-pig" -> ArcadePortRules.BACON_BLAST_COOLDOWN_MILLIS;
            case "sheep-pig" -> ArcadePortRules.BACON_CLOAK_COOLDOWN_MILLIS;
            default -> ArcadePortRules.BACON_BODY_SLAM_COOLDOWN_MILLIS;
        };
        long nextReady = session.baconAbilityReadyAt.getOrDefault(id, 0L);
        if (now < nextReady) {
            player.sendMessage(ChatColor.YELLOW + "Ability ready in " + Math.max(1L, (nextReady - now + 999L) / 1000L) + "s.");
            return;
        }
        session.baconAbilityReadyAt.put(id, now + cooldown);
        switch (kit) {
            case "mama-pig": {
                Snowball bacon = player.launchProjectile(Snowball.class);
                bacon.setItem(new ItemStack(Material.PORKCHOP));
                Vector velocity = player.getEyeLocation().getDirection().normalize().multiply(1.0D);
                velocity.setY(velocity.getY() + 0.2D);
                bacon.setVelocity(velocity);
                session.entities.add(bacon);
                player.playSound(player.getLocation(), Sound.ENTITY_PIG_AMBIENT, 1.0F, 1.5F);
                player.sendMessage(ChatColor.GREEN + "Bacon Blast!");
                break;
            }
            case "sheep-pig":
                clearBaconCloak(session, id);
                session.baconCloakedUntil.put(id, now + ArcadePortRules.BACON_CLOAK_DURATION_MILLIS);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false, false));
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 14, 0.35D, 0.5D, 0.35D, 0.03D);
                player.sendMessage(ChatColor.GREEN + "Cloak activated for 5 seconds.");
                break;
            default:
                Vector leap = player.getLocation().getDirection().normalize().multiply(1.2D);
                leap.setY(0.2D);
                player.setVelocity(leap);
                session.baconSlams.put(id, now + 1_000L);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.3D, 0.25D, 0.3D, 0.05D);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
                player.sendMessage(ChatColor.GREEN + "Body Slam!");
                break;
        }
    }

    private void clearBaconCloak(Session session, UUID playerId) {
        boolean wasCloaked = session.baconCloakedUntil.remove(playerId) != null;
        Player player = session.players.get(playerId);
        if (wasCloaked && player != null && player.isOnline()) player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private void handleBaconBrawlHit(Session session, Player attacker, Player target, String source) {
        if (active != session || attacker.equals(target)
                || !session.alive.contains(attacker.getUniqueId())
                || !session.alive.contains(target.getUniqueId())) return;
        clearBaconCloak(session, attacker.getUniqueId());
        clearBaconCloak(session, target.getUniqueId());
        String kit = session.baconKits.getOrDefault(attacker.getUniqueId(), "pig");
        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        if (direction.lengthSquared() < 1.0E-5D) direction = attacker.getLocation().getDirection();
        double hitKnockback = session.baconKnockback;
        if ("body-slam".equals(source)) hitKnockback = Math.max(hitKnockback, 2.0D);
        if ("sheep-pig".equals(kit) && "melee".equals(source)) hitKnockback *= 2.5D;
        target.setVelocity(direction.normalize().multiply(hitKnockback).setY(0.7D));
        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 10, 0.35D, 0.4D, 0.35D, 0.04D);
        attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + 2));
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8F, 1.1F);
        double damage = ArcadePortRules.BACON_STANDARD_HIT_DAMAGE;
        if (target.getHealth() <= damage) {
            // eliminate() records the kill score for this game; don't count it twice here.
            eliminate(session, target, attacker, "was knocked out by " + attacker.getName());
        } else {
            target.setHealth(target.getHealth() - damage);
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9F, 0.85F);
        }
        if (active != session || !"body-slam".equals(source) || !session.alive.contains(attacker.getUniqueId())) return;
        double recoil = ArcadePortRules.BACON_BODY_SLAM_RECOIL_DAMAGE;
        if (attacker.getHealth() <= recoil) eliminate(session, attacker, null, "was hurt by their body slam");
        else {
            attacker.setHealth(attacker.getHealth() - recoil);
            Vector recoilDirection = attacker.getLocation().toVector().subtract(target.getLocation().toVector());
            if (recoilDirection.lengthSquared() > 1.0E-5D) attacker.setVelocity(recoilDirection.normalize().multiply(0.6D).setY(0.15D));
        }
    }

    private void updateTurfWarsPhase(Session session, long now) {
        if (now < session.turfPhaseEndsAt) return;
        session.turfCombat = !session.turfCombat;
        if (session.turfCombat) {
            session.turfPhaseEndsAt = now + ArcadePortRules.TURF_WARS_COMBAT_PHASE_MILLIS;
            announce(session, ChatColor.RED + "Combat phase! Kills advance your team's turf.");
        } else {
            session.turfPhaseEndsAt = now + ArcadePortRules.TURF_WARS_BUILD_PHASE_MILLIS;
            for (Player player : alivePlayers(session)) giveTurfWarsWool(session, player);
            announce(session, ChatColor.GREEN + "Build phase! You have 20 seconds to reinforce your team's turf.");
        }
    }

    private void spawnTestDragons(Session session) {
        // Reborn starts its spawn loop during live play and adds at most one
        // dragon every 500 ms until five are active. Do not flood the arena at
        // round start; updateTestDragons() handles the source cadence and RED
        // map spawn markers for every dragon.
        session.dragonNextSpawnAt = System.currentTimeMillis() + ArcadePortRules.DRAGONS_SPAWN_INTERVAL_MILLIS;
        announce(session, ChatColor.DARK_PURPLE
                + "The dragons are hunting! Repel them with your Pyro fireballs.");
    }

    private void updateTestDragons(Session session, long now) {
        if (active != session) return;
        List<Player> alive = alivePlayers(session);

        for (Iterator<Entity> it = session.entities.iterator(); it.hasNext();) {
            Entity entity = it.next();
            if (entity instanceof EnderDragon dragon && (!dragon.isValid() || dragon.isDead())) {
                UUID id = dragon.getUniqueId();
                session.dragonTargetLocations.remove(id);
                session.dragonTargetPlayers.remove(id);
                it.remove();
            }
        }

        long dragonCount = session.entities.stream().filter(e -> e instanceof EnderDragon && e.isValid()).count();
        if (ArcadePortRules.shouldSpawnTestDragon((int) dragonCount, now, session.dragonNextSpawnAt)) {
            SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
            List<Location> spawns = mapData == null ? List.of() : mapData.dataLocations("RED");
            if (spawns.isEmpty() && mapData != null) spawns = mapData.spawnLocations();
            Location spawn;
            if (!spawns.isEmpty()) {
                spawn = spawns.get(ThreadLocalRandom.current().nextInt(spawns.size())).clone();
            } else {
                Location center = session.dragonSpectatorLocation == null
                        ? session.world.getSpawnLocation() : session.dragonSpectatorLocation;
                spawn = center.clone().add(ThreadLocalRandom.current().nextDouble(-18.0D, 19.0D),
                        0.0D, ThreadLocalRandom.current().nextDouble(-18.0D, 19.0D));
            }
            EnderDragon dragon = session.world.spawn(spawn, EnderDragon.class);
            dragon.setAI(false);
            dragon.setGravity(false);
            dragon.setInvulnerable(true);
            dragon.setRemoveWhenFarAway(false);
            dragon.setCustomName(ChatColor.DARK_PURPLE + "Dragon");
            dragon.setCustomNameVisible(true);
            session.entities.add(dragon);
            session.dragonTargetLocations.put(dragon.getUniqueId(), randomDragonSkyTarget(session));
            session.dragonNextSpawnAt = now + ArcadePortRules.DRAGONS_SPAWN_INTERVAL_MILLIS;
            spawn.getWorld().playSound(spawn, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 1.0F);
        }

        for (Player player : alive) {
            UUID id = player.getUniqueId();
            String kit = session.dragonKits.getOrDefault(id, "coward");
            if ("pyrotechnic".equals(kit)) {
                long next = session.dragonNextSparklerAt.getOrDefault(id,
                        now + ArcadePortRules.DRAGONS_SPARKLER_INTERVAL_MILLIS);
                if (now >= next) {
                    int held = countNamedItems(player, Material.EMERALD, "Throwing Sparkler");
                    if (ArcadePortRules.shouldReplenishDragonSparkler(now, next, held)) {
                        player.getInventory().addItem(namedItem(Material.EMERALD, "Throwing Sparkler",
                                "Right-click to throw a sparkler that attracts nearby dragons.", 1));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.0F);
                    }
                    session.dragonNextSparklerAt.put(id, now + ArcadePortRules.DRAGONS_SPARKLER_INTERVAL_MILLIS);
                }
            } else if ("marksman".equals(kit)) {
                long nextArrow = session.dragonNextArrowAt.getOrDefault(id,
                        now + ArcadePortRules.DRAGONS_MARKSMAN_ARROW_INTERVAL_MILLIS);
                if (now >= nextArrow) {
                    if (countNamedItems(player, Material.ARROW, "Fletched Arrow")
                            < ArcadePortRules.DRAGONS_MARKSMAN_MAX_ARROWS) {
                        player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow",
                                "A replenishing Marksman arrow.", 1));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6F, 1.0F);
                    }
                    session.dragonNextArrowAt.put(id, now + ArcadePortRules.DRAGONS_MARKSMAN_ARROW_INTERVAL_MILLIS);
                }
                updateDragonBarrageCharge(session, player, now);
            }
        }

        List<EnderDragon> dragons = session.entities.stream()
                .filter(e -> e instanceof EnderDragon && e.isValid())
                .map(e -> (EnderDragon) e).toList();

        for (EnderDragon dragon : dragons) {
            UUID dragonId = dragon.getUniqueId();
            UUID targetPlayerId = session.dragonTargetPlayers.get(dragonId);
            Location target = session.dragonTargetLocations.get(dragonId);
            if (targetPlayerId != null) {
                Player targetPlayer = session.players.get(targetPlayerId);
                if (targetPlayer != null && targetPlayer.isOnline() && session.alive.contains(targetPlayerId)) {
                    target = targetPlayer.getLocation().clone();
                    session.dragonTargetLocations.put(dragonId, target.clone());
                } else {
                    session.dragonTargetPlayers.remove(dragonId);
                    target = null;
                }
            }

            if (dragon.getLocation().getY() < 10.0D || target == null
                    || dragon.getLocation().distanceSquared(target) <= 36.0D) {
                if (session.dragonSpectatorLocation != null
                        && dragon.getLocation().getY() > session.dragonSpectatorLocation.getY() && !alive.isEmpty()) {
                    Player newTarget = alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
                    session.dragonTargetPlayers.put(dragonId, newTarget.getUniqueId());
                    target = newTarget.getLocation().clone();
                } else {
                    session.dragonTargetPlayers.remove(dragonId);
                    target = randomDragonSkyTarget(session);
                }
                session.dragonTargetLocations.put(dragonId, target.clone());
            }
        }

        for (Iterator<Entity> it = session.entities.iterator(); it.hasNext();) {
            Entity entity = it.next();
            if (!(entity instanceof Item item) || !session.dragonSparklerItems.contains(item.getUniqueId())) continue;
            UUID sparklerId = item.getUniqueId();
            if (!item.isValid() || ArcadePortRules.shouldExpireDragonSparkler(item.getTicksLived(),
                    item.getLocation().getY(), item.getLocation().getBlock().isLiquid())) {
                session.dragonSparklerItems.remove(sparklerId);
                it.remove();
                item.remove();
                continue;
            }
            Location itemLocation = item.getLocation();
            item.getWorld().spawnParticle(Particle.FIREWORK, itemLocation, 1, 0.08D, 0.08D, 0.08D, 0.0D);
            for (EnderDragon dragon : dragons) {
                if (dragon.getLocation().distanceSquared(itemLocation)
                        > ArcadePortRules.DRAGONS_SPARKLER_RANGE_SQUARED) continue;
                session.dragonTargetPlayers.remove(dragon.getUniqueId());
                session.dragonTargetLocations.put(dragon.getUniqueId(), itemLocation.clone());
            }
        }

        for (EnderDragon dragon : dragons) {
            Location target = session.dragonTargetLocations.get(dragon.getUniqueId());
            if (target != null) {
                Vector direction = target.toVector().subtract(dragon.getLocation().toVector());
                if (direction.lengthSquared() > 1.0D) {
                    Vector flight = direction.normalize().multiply(0.32D);
                    flight.setY(Math.max(-0.08D, Math.min(0.16D, flight.getY() + 0.04D)));
                    dragon.setVelocity(flight);
                }
            }
            for (Player player : alive) {
                if (dragon.getLocation().distanceSquared(player.getLocation()) <= 36.0D) {
                    applyDragonContactHit(session, dragon, player, now);
                }
            }
        }
    }

    private Location dragonSpectatorLocation(Session session) {
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null) {
            List<Location> markers = mapData.customLocations("SPECTATOR");
            if (markers.isEmpty()) markers = mapData.dataLocations("SPECTATOR");
            if (!markers.isEmpty()) return markers.get(0).clone();
            double x = mapData.hasBounds() ? mapData.centerX() : session.world.getSpawnLocation().getX();
            double z = mapData.hasBounds() ? mapData.centerZ() : session.world.getSpawnLocation().getZ();
            double y = mapData.hasVerticalBounds()
                    ? Math.max(mapData.minY() + 12.0D, mapData.maxY() - 5.0D) : 90.0D;
            return new Location(session.world, x, y, z);
        }
        return session.world.getSpawnLocation().clone().add(0.0D, 30.0D, 0.0D);
    }

    private Location randomDragonSkyTarget(Session session) {
        Location center = session.dragonSpectatorLocation == null
                ? session.world.getSpawnLocation() : session.dragonSpectatorLocation;
        return center.clone().add(ThreadLocalRandom.current().nextDouble(-50.0D, 51.0D),
                ThreadLocalRandom.current().nextDouble(20.0D, 41.0D),
                ThreadLocalRandom.current().nextDouble(-50.0D, 51.0D));
    }

    private void setDragonTargetSky(Session session, EnderDragon dragon) {
        session.dragonTargetPlayers.remove(dragon.getUniqueId());
        session.dragonTargetLocations.put(dragon.getUniqueId(), randomDragonSkyTarget(session));
    }

    private void updateDragonBarrageCharge(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        if (session.dragonMarksmanFiring.contains(id)) {
            int remaining = session.dragonMarksmanCharge.getOrDefault(id, 0);
            long nextAt = session.dragonMarksmanNextChargeAt.getOrDefault(id, now);
            if (remaining <= 0) {
                session.dragonMarksmanFiring.remove(id);
                session.dragonMarksmanCharge.remove(id);
                session.dragonMarksmanNextChargeAt.remove(id);
                return;
            }
            if (now < nextAt) return;
            Vector jitter = new Vector((ThreadLocalRandom.current().nextDouble() - 0.5D) / 10.0D,
                    (ThreadLocalRandom.current().nextDouble() - 0.5D) / 10.0D,
                    (ThreadLocalRandom.current().nextDouble() - 0.5D) / 10.0D);
            Arrow arrow = player.launchProjectile(Arrow.class);
            arrow.setVelocity(player.getLocation().getDirection().clone().add(jitter).normalize().multiply(3.0D));
            session.entities.add(arrow);
            session.dragonTrackedArrows.add(arrow.getUniqueId());
            session.dragonMarksmanCharge.put(id, remaining - 1);
            session.dragonMarksmanNextChargeAt.put(id, now + ArcadePortRules.DRAGONS_BARRAGE_INTERVAL_MILLIS);
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8F, 1.0F + (0.1F * (remaining - 1)));
            return;
        }
        if (!session.dragonMarksmanCharge.containsKey(id)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.BOW) {
            session.dragonMarksmanCharge.remove(id);
            session.dragonMarksmanNextChargeAt.remove(id);
            return;
        }
        long nextAt = session.dragonMarksmanNextChargeAt.getOrDefault(id,
                now + ArcadePortRules.DRAGONS_BARRAGE_INITIAL_CHARGE_MILLIS);
        int charge = session.dragonMarksmanCharge.getOrDefault(id, 0);
        if (charge >= ArcadePortRules.DRAGONS_BARRAGE_MAX_ARROWS || now < nextAt) return;
        charge++;
        session.dragonMarksmanCharge.put(id, charge);
        session.dragonMarksmanNextChargeAt.put(id, now + ArcadePortRules.DRAGONS_BARRAGE_INTERVAL_MILLIS);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4F, 1.0F + (0.1F * charge));
    }

    private void applyDragonContactHit(Session session, EnderDragon dragon, Player target, long now) {
        if (active != session || !session.alive.contains(target.getUniqueId())) return;
        String key = dragon.getUniqueId() + ":" + target.getUniqueId();
        long nextAllowed = session.dragonContactCooldowns.getOrDefault(key, 0L);
        if (!ArcadePortRules.shouldApplyDragonContactHit(now, nextAllowed)) return;
        session.dragonContactCooldowns.put(key, now + ArcadePortRules.DRAGONS_CONTACT_COOLDOWN_MILLIS);
        Vector recoil = target.getLocation().toVector().subtract(dragon.getLocation().toVector());
        if (recoil.lengthSquared() < 0.001D) recoil = new Vector(0.0D, 1.0D, 0.0D);
        target.setVelocity(recoil.normalize().multiply(1.0D).setY(0.6D));
        target.playEffect(EntityEffect.HURT);
        target.sendMessage(ChatColor.RED + "A dragon struck you!");
    }

    private void handleDragonProjectileHit(Session session, EnderDragon dragon) {
        if (active != session || !dragon.isValid()) return;
        dragon.playEffect(EntityEffect.HURT);
    }

    private void spawnDragonEscapeDragon(Session session) {
        Location spawn = new Location(session.world, 0.5D, 66.5D, -40.0D);
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null) {
            session.dragonEscapeMapName = mapData.mapName();
            List<Location> starts = mapData.dataLocations("RED");
            List<Location> sourceRoute = mapData.dataLocations("BLACK");
            if (!starts.isEmpty() && !sourceRoute.isEmpty()) {
                spawn = starts.get(0).clone();
                // DATA_LOCS is an ordered list in WorldConfig.dat. Preserve authored course order:
                // a nearest-neighbour sort can jump across nearby switchbacks and bends.
                for (Location marker : SourceMapCatalog.MapData.orderedRouteMarkers(
                        spawn, sourceRoute, ArcadePortRules.DRAGON_ESCAPE_DUPLICATE_WAYPOINT_DISTANCE)) {
                    session.dragonEscapeWaypoints.add(marker.subtract(0.0D, 1.0D, 0.0D));
                }
            }
        }
        EnderDragon dragon = session.world.spawn(spawn, EnderDragon.class);
        dragon.setAI(false);
        dragon.setGravity(false);
        dragon.setInvulnerable(true);
        dragon.setRemoveWhenFarAway(false);
        dragon.setCustomName(ChatColor.YELLOW + "Douglas the Dragon");
        dragon.setCustomNameVisible(true);
        dragon.setVelocity(new Vector(0.0D, 0.0D, ArcadePortRules.DRAGON_ESCAPE_CHASE_SPEED));
        session.dragonEscapeDragon = dragon;
        session.entities.add(dragon);
    }

    private void useDragonEscapeLeap(Session session, Player player) {
        UUID id = player.getUniqueId();
        int remaining = session.dragonEscapeLeapUsesRemaining.getOrDefault(id, 0);
        if (remaining <= 0) {
            player.sendMessage(ChatColor.YELLOW + "You have used all of your leaps.");
            return;
        }
        remaining--;
        session.dragonEscapeLeapUsesRemaining.put(id, remaining);
        if (remaining == 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(namedItem(Material.IRON_AXE, "Leap",
                    "Right-click to leap forward (" + remaining + " uses remaining)."));
        }
        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(1.0D).setY(0.45D));
        player.setFallDistance(0.0F);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 16,
                0.35D, 0.3D, 0.35D, 0.05D);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7F, 1.15F);
    }

    private void useDragonEscapeWarper(Session session, Player player) {
        UUID id = player.getUniqueId();
        if (session.dragonEscapeWarperUsed.contains(id)) {
            player.sendMessage(ChatColor.YELLOW + "Your Warper pearl has already been used.");
            return;
        }
        int progress = dragonEscapePlayerProgress(session, player);
        Player ahead = alivePlayers(session).stream()
                .filter(candidate -> !candidate.getUniqueId().equals(id))
                .filter(candidate -> dragonEscapePlayerProgress(session, candidate) > progress)
                .min(java.util.Comparator.comparingInt(candidate -> dragonEscapePlayerProgress(session, candidate)))
                .orElse(null);
        if (ahead == null) {
            player.sendMessage(ChatColor.YELLOW + "There is no player ahead of you to warp to.");
            return;
        }
        Location destination = ahead.getLocation().clone().add(0.0D, 0.15D, 0.0D);
        if (!player.teleport(destination)) {
            player.sendMessage(ChatColor.RED + "The warp failed; your pearl has been kept.");
            return;
        }
        consumeHeld(player);
        session.dragonEscapeWarperUsed.add(id);
        session.lastLocations.put(id, destination.clone());
        player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9F, 1.2F);
        player.getWorld().spawnParticle(Particle.PORTAL, destination, 24, 0.3D, 0.5D, 0.3D, 0.2D);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Warped to " + ahead.getName() + "!");
    }

    private int dragonEscapePlayerProgress(Session session, Player player) {
        if (!session.dragonEscapeWaypoints.isEmpty()) {
            UUID id = player.getUniqueId();
            int candidate = getDragonEscapeWaypointIndex(session, player.getLocation());
            int progress = ArcadePortRules.monotonicDragonEscapeProgress(
                    session.dragonEscapePlayerProgress.getOrDefault(id, 0),
                    candidate, session.dragonEscapeWaypoints.size());
            session.dragonEscapePlayerProgress.put(id, progress);
            return progress;
        }
        return player.getLocation().getBlockZ();
    }

    private void throwDragonEscapeMine(Session session, Player player) {
        if (inventoryCount(player, Material.TNT) <= 0) {
            player.sendMessage(ChatColor.YELLOW + "Your next mini explosive is still recharging.");
            return;
        }
        consumeHeld(player);
        session.dragonEscapeNextTntAt.put(player.getUniqueId(),
                ArcadePortRules.dragonEscapeNextTntChargeAt(System.currentTimeMillis()));
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location spawn = player.getEyeLocation().add(direction.clone().multiply(0.8D));
        TNTPrimed mine = session.world.spawn(spawn, TNTPrimed.class);
        mine.setFuseTicks(12_000);
        mine.setVelocity(direction.multiply(0.8D).setY(direction.getY() * 0.65D + 0.1D));
        session.entities.add(mine);
        session.dragonEscapeMineOwners.put(mine.getUniqueId(), player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.8F, 1.15F);
    }

    private void updateDragonEscapeResources(Session session, long now) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (!"disruptor".equals(session.dragonEscapeKits.getOrDefault(id, "jumper"))) continue;
            long nextAt = session.dragonEscapeNextTntAt.getOrDefault(id,
                    now + ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS);
            if (now < nextAt) continue;
            if (inventoryCount(player, Material.TNT) < ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_MAX_TNT) {
                player.getInventory().addItem(namedItem(Material.TNT, "Mini Explosive",
                        "Right-click to throw a trip mine; recharges every 10 seconds."));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4F, 1.2F);
            }
            session.dragonEscapeNextTntAt.put(id,
                    ArcadePortRules.dragonEscapeNextTntChargeAt(now));
        }
    }

    private void updateDragonEscapeMines(Session session) {
        for (Entity entity : new ArrayList<>(session.entities)) {
            if (!(entity instanceof TNTPrimed mine) || !mine.isValid()) continue;
            UUID ownerId = session.dragonEscapeMineOwners.get(mine.getUniqueId());
            if (ownerId == null) continue;
            for (Player target : alivePlayers(session)) {
                if (target.getUniqueId().equals(ownerId)) continue;
                if (target.getLocation().distanceSquared(mine.getLocation()) <= 1.25D) {
                    mine.setFuseTicks(0);
                    target.sendActionBar(ChatColor.RED + "A mini explosive was triggered!");
                    break;
                }
            }
        }
    }

    private boolean isDragonEscapeDiggerBlockAllowed(Material material) {
        return material != null && material.isBlock() && !material.isAir()
                && material != Material.WATER && material != Material.LAVA && material != Material.BUBBLE_COLUMN
                && material != Material.BEDROCK && material != Material.BARRIER
                && material != Material.END_PORTAL && material != Material.END_PORTAL_FRAME
                && material != Material.COMMAND_BLOCK && material != Material.CHAIN_COMMAND_BLOCK
                && material != Material.REPEATING_COMMAND_BLOCK && material != Material.STRUCTURE_BLOCK
                && material != Material.JIGSAW;
    }

    private boolean isDragonEscapeDiggerBuildLocationAllowed(Session session, Block block) {
        if (block.getY() < 1 || block.getY() >= session.world.getMaxHeight() - 1) return false;
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null && mapData.hasBounds()) {
            return block.getX() >= mapData.minX() && block.getX() < mapData.maxX()
                    && block.getZ() >= mapData.minZ() && block.getZ() < mapData.maxZ();
        }
        return block.getX() >= -4 && block.getX() <= 4
                && block.getZ() >= -24 && block.getZ() <= 80;
    }

    private void useDragonEscapeDigger(Session session, Player player, Block block) {
        UUID id = player.getUniqueId();
        int remaining = session.dragonEscapeDiggerUsesRemaining.getOrDefault(
                id, ArcadePortRules.DRAGON_ESCAPE_DIGGER_MAX_USES);
        if (remaining <= 0) {
            player.sendMessage(ChatColor.YELLOW + "Your Digger pickaxe has no uses remaining.");
            return;
        }
        if (!isDragonEscapeDiggerBlockAllowed(block.getType())
                || !isDragonEscapeDiggerBuildLocationAllowed(session, block)
                || block.getState() instanceof org.bukkit.block.Container) return;
        Material mined = block.getType();
        Location dropLocation = block.getLocation().add(0.5D, 0.5D, 0.5D);
        block.setType(Material.AIR, false);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(mined, 1));
        for (ItemStack leftover : overflow.values()) player.getWorld().dropItemNaturally(dropLocation, leftover);
        remaining--;
        session.dragonEscapeDiggerUsesRemaining.put(id, remaining);
        if (remaining == 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(namedItem(Material.DIAMOND_PICKAXE, "Digger Pickaxe",
                    "Left-click course blocks to recover and place as shortcuts (" + remaining + " digs left)."));
        }
        player.playSound(dropLocation, Sound.BLOCK_STONE_BREAK, 0.75F, 1.2F);
    }

    private void detonateDragonEscapeMine(Session session, EntityExplodeEvent event, TNTPrimed mine) {
        UUID ownerId = session.dragonEscapeMineOwners.remove(mine.getUniqueId());
        event.blockList().clear();
        event.setCancelled(true);
        Location blast = mine.getLocation().clone();
        session.entities.remove(mine);
        mine.remove();
        session.world.spawnParticle(Particle.EXPLOSION, blast, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        session.world.playSound(blast, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.5F);
        for (Player target : alivePlayers(session)) {
            if (ownerId != null && target.getUniqueId().equals(ownerId)) continue;
            double distanceSquared = target.getLocation().distanceSquared(blast);
            if (distanceSquared > 16.0D) continue;
            double distance = Math.sqrt(distanceSquared);
            Vector away = target.getLocation().toVector().subtract(blast.toVector());
            if (away.lengthSquared() < 0.001D) away = new Vector(0.0D, 1.0D, 0.0D);
            double force = 1.4D * Math.max(0.25D, 1.0D - distance / 4.5D);
            target.setVelocity(away.normalize().multiply(force)
                    .setY(Math.max(0.35D, 0.7D - distance * 0.08D)));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, false, false, true));
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 10,
                    0.3D, 0.3D, 0.3D, 0.04D);
        }
    }

    /** Paper-native approximation of Reborn's DragonEscapeData.Move + radius-10 block explosion. */
    private void updateDragonEscapeChase(Session session) {
        EnderDragon dragon = session.dragonEscapeDragon;
        if (active != session || dragon == null || !dragon.isValid() || dragon.isDead()) return;
        Location location = dragon.getLocation();
        if (!session.dragonEscapeWaypoints.isEmpty()) {
            int dragonIndex = ArcadePortRules.monotonicDragonEscapeProgress(
                    session.dragonEscapeDragonProgress,
                    getDragonEscapeWaypointIndex(session, location), session.dragonEscapeWaypoints.size());
            session.dragonEscapeDragonProgress = dragonIndex;
            Location target = session.dragonEscapeWaypoints.get(
                    Math.min(session.dragonEscapeWaypoints.size() - 1, dragonIndex + 1));
            Vector desired = target.toVector().subtract(location.toVector());
            if (desired.lengthSquared() > 1.0E-4D) {
                dragon.setVelocity(desired.normalize().multiply(0.2D));
            } else {
                dragon.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            }

            // Reborn destroys a radius-10 sphere as the dragon moves. Do not scan the same area
            // every tick: update the sphere after the dragon has moved at least half a block.
            Location lastDestruction = session.dragonEscapeLastDestructionLocation;
            double movedSquared = lastDestruction == null ? 0.0D : location.distanceSquared(lastDestruction);
            if (ArcadePortRules.shouldProcessDragonEscapeDestruction(movedSquared, lastDestruction != null)) {
                int radius = (int) Math.ceil(ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS);
                double radiusSquared = ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS
                        * ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (dx * dx + dy * dy + dz * dz > radiusSquared) continue;
                            Block block = location.getBlock().getRelative(dx, dy, dz);
                            if (block.isLiquid() || block.getRelative(BlockFace.UP).isLiquid()) continue;
                            if (session.dragonEscapeMapName.contains("Hell") && block.getY() < 30) continue;
                            if (session.dragonEscapeMapName.contains("Pirate") && block.getY() < 6) continue;
                            if (!block.getType().isAir()) block.setType(Material.AIR, false);
                        }
                    }
                }
                session.dragonEscapeLastDestructionLocation = location.clone();
            }

            int lastIndex = session.dragonEscapeWaypoints.size() - 1;
            for (Player player : new ArrayList<>(alivePlayers(session))) {
                int playerIndex = dragonEscapePlayerProgress(session, player);
                Location playerNext = session.dragonEscapeWaypoints.get(Math.min(lastIndex, playerIndex + 1));
                Location dragonNext = session.dragonEscapeWaypoints.get(Math.min(lastIndex, dragonIndex + 1));
                double playerScore = playerIndex * 10_000.0D - player.getLocation().distance(playerNext);
                double dragonScore = dragonIndex * 10_000.0D - location.distance(dragonNext);
                if (playerIndex == lastIndex && player.getLocation().distance(
                        session.dragonEscapeWaypoints.get(lastIndex)) < 3.0D) {
                    finish(session, player, true);
                    return;
                }
                if (dragonScore > playerScore) {
                    eliminate(session, player, null, "was caught by Douglas the Dragon");
                    if (active != session) return;
                }
            }
            return;
        }

        // Generated fallback course for maps with no authored waypoint markers.
        if (location.getZ() < 80.0D) {
            Vector desired = new Vector((0.5D - location.getX()) * 0.025D,
                    (65.5D - location.getY()) * 0.02D, 1.0D).normalize()
                    .multiply(ArcadePortRules.DRAGON_ESCAPE_CHASE_SPEED);
            dragon.setVelocity(desired);
        } else dragon.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        int minZ = Math.max(-24, (int) Math.floor(location.getZ() - ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS));
        int maxZ = Math.min(80, (int) Math.floor(location.getZ() + ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS));
        double radius = ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS;
        for (int x = -3; x <= 3; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = session.world.getBlockAt(x, 63, z);
                if (block.getType().isAir()) continue;
                double dx = x + 0.5D - location.getX();
                double dy = 63.5D - location.getY();
                double dz = z + 0.5D - location.getZ();
                if (dx * dx + dy * dy + dz * dz <= radius * radius) block.setType(Material.AIR, false);
            }
        }
        double dragonProgress = location.getZ();
        for (Player player : new ArrayList<>(alivePlayers(session))) {
            if (ArcadePortRules.shouldDragonEscapePlayerBeCaught(dragonProgress, player.getLocation().getZ())) {
                eliminate(session, player, null, "was caught by Douglas the Dragon");
                if (active != session) return;
            }
        }
    }

    private int getDragonEscapeWaypointIndex(Session session, Location location) {
        int bestIndex = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < session.dragonEscapeWaypoints.size(); i++) {
            double distance = session.dragonEscapeWaypoints.get(i).distance(location);
            if (distance < bestDistance) { bestDistance = distance; bestIndex = i; }
        }
        return bestIndex;
    }

    private void launchDragonHazard(Session session) {
        List<Player> alive = alivePlayers(session);
        if (alive.isEmpty()) return;
        Player target = alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        boolean fromLeft = ThreadLocalRandom.current().nextBoolean();
        double edgeX;
        double spawnY = Math.max(68.0D, target.getLocation().getY() + 4.0D);
        if (mapData != null && mapData.hasBounds()) {
            edgeX = fromLeft ? mapData.minX() + 1.5D : mapData.maxX() - 1.5D;
            if (mapData.hasVerticalBounds()) spawnY = Math.min(spawnY, mapData.maxY() - 2.0D);
        } else {
            edgeX = fromLeft ? -18.0D : 18.0D;
        }
        Location spawn = new Location(session.world, edgeX, spawnY, target.getLocation().getZ());
        Fireball fireball = session.world.spawn(spawn, Fireball.class);
        // Leave the shooter unset: this is a world hazard, not a player-fired attack.
        fireball.setIsIncendiary(false);
        fireball.setYield(0.0F);
        Vector direction = target.getLocation().toVector().subtract(spawn.toVector()).normalize().multiply(0.65D);
        fireball.setVelocity(direction);
        session.entities.add(fireball);
    }

    private int sheepPenCenterX(int team) {
        return team <= 1 ? -14 : 14;
    }

    private int sheepPenCenterZ(int team) {
        return (team == 0 || team == 2) ? -14 : 14;
    }

    private String sheepPenMarker(int team) {
        return switch (team) {
            case 0 -> "RED";
            case 1 -> "BLUE";
            default -> ArcadePortRules.sheepQuestTeamPenMarker(team);
        };
    }

    private List<Location> sourceSheepPenBlocks(Session session, int team) {
        SourceMapCatalog.MapData data = sourceMapData.get(session.type.id());
        return data == null ? List.of() : data.dataLocations(sheepPenMarker(team));
    }

    private Location sheepQuestPenCenter(Session session, int team) {
        List<Location> pen = sourceSheepPenBlocks(session, team);
        if (!pen.isEmpty()) {
            double x = 0.0D, y = 0.0D, z = 0.0D;
            for (Location block : pen) {
                x += block.getBlockX() + 0.5D;
                y += block.getBlockY() + 1.0D;
                z += block.getBlockZ() + 0.5D;
            }
            return new Location(session.world, x / pen.size(), y / pen.size(), z / pen.size());
        }
        return new Location(session.world, sheepPenCenterX(team) + 0.5D, 64.0D,
                sheepPenCenterZ(team) + 0.5D);
    }

    private DyeColor sheepTeamDyeColor(int team) {
        return switch (team) {
            case 0 -> DyeColor.RED;
            case 1 -> DyeColor.LIGHT_BLUE;
            case 2 -> DyeColor.LIME;
            default -> DyeColor.YELLOW;
        };
    }

    private ChatColor sheepTeamChatColor(int team) {
        return switch (team) {
            case 0 -> ChatColor.RED;
            case 1 -> ChatColor.AQUA;
            case 2 -> ChatColor.GREEN;
            default -> ChatColor.YELLOW;
        };
    }

    private Material sheepTeamWool(int team) {
        return switch (team) {
            case 0 -> Material.RED_WOOL;
            case 1 -> Material.LIGHT_BLUE_WOOL;
            case 2 -> Material.LIME_WOOL;
            default -> Material.YELLOW_WOOL;
        };
    }

    private String sheepTeamName(int team) {
        return switch (team) {
            case 0 -> "Red";
            case 1 -> "Aqua";
            case 2 -> "Green";
            default -> "Yellow";
        };
    }

    private boolean isInsideSheepPen(Session session, Location location, int team) {
        if (session == null || location == null || team < 0 || team > 3
                || location.getWorld() != session.world) return false;
        List<Location> pen = sourceSheepPenBlocks(session, team);
        if (!pen.isEmpty()) {
            Block sheepBlock = location.getBlock();
            for (Location marker : pen) {
                if (ArcadePortRules.sheepQuestInsidePenMarker(sheepBlock.getX(), sheepBlock.getY(),
                        sheepBlock.getZ(), marker.getBlockX(), marker.getBlockY(), marker.getBlockZ())) return true;
            }
            return false;
        }
        Location center = sheepQuestPenCenter(session, team);
        return Math.abs(location.getX() - center.getX()) <= 2.5D
                && Math.abs(location.getZ() - center.getZ()) <= 2.5D;
    }

    private void buildSheepPens(World world) {
        for (int team = 0; team < 4; team++) {
            int centerX = sheepPenCenterX(team);
            int centerZ = sheepPenCenterZ(team);
            Material wool = sheepTeamWool(team);
            int gateX = (team == 0 || team == 1) ? 2 : -2;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    world.getBlockAt(centerX + dx, 63, centerZ + dz).setType(wool, false);
                    boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    boolean gate = dx == gateX && dz == 0;
                    if (wall && !gate) {
                        for (int y = 64; y <= 65; y++) {
                            world.getBlockAt(centerX + dx, y, centerZ + dz).setType(wool, false);
                        }
                    }
                }
            }
        }
    }

    private int sheepCarriedBy(Session session, UUID playerId) {
        int count = 0;
        for (UUID carrier : session.sheepCarriers.values()) if (playerId.equals(carrier)) count++;
        return count;
    }

    private void dropSheepFromCarrier(Session session, Player carrier, String reason) {
        if (carrier == null) return;
        UUID carrierId = carrier.getUniqueId();
        List<Sheep> carried = session.sheepCarriers.entrySet().stream()
                .filter(entry -> carrierId.equals(entry.getValue()))
                .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toList());
        if (carried.isEmpty()) return;
        for (Sheep sheep : carried) {
            session.sheepCarriers.remove(sheep);
            if (sheep == null || !sheep.isValid()) continue;
            int owner = session.sheepOwners.getOrDefault(sheep, -1);
            if (owner >= 0) {
                sheep.setColor(sheepTeamDyeColor(owner));
                Location pen = sheepQuestPenCenter(session, owner);
                sheep.teleport(pen.clone());
                sheep.setAI(false);
            } else {
                sheep.setColor(DyeColor.WHITE);
                sheep.setAI(true);
            }
        }
        carrier.sendMessage(ChatColor.RED + "You dropped your sheep!");
        announce(session, ChatColor.YELLOW + carrier.getName() + " dropped their sheep (" + reason + ").");
    }

    private void updateSheepQuest(Session session) {
        java.util.Iterator<Map.Entry<Sheep, UUID>> iterator = session.sheepCarriers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Sheep, UUID> entry = iterator.next();
            Sheep sheep = entry.getKey();
            UUID carrierId = entry.getValue();
            Player carrier = session.players.get(carrierId);
            if (sheep == null || !sheep.isValid() || sheep.isDead()) {
                iterator.remove();
                session.sheep.remove(sheep);
                session.sheepOwners.remove(sheep);
                continue;
            }
            if (carrier == null || !carrier.isOnline() || !session.alive.contains(carrierId)) {
                iterator.remove();
                sheep.setAI(true);
                int owner = session.sheepOwners.getOrDefault(sheep, -1);
                if (owner >= 0) {
                    Location pen = sheepQuestPenCenter(session, owner);
                    sheep.teleport(pen.clone());
                    sheep.setAI(false);
                }
                continue;
            }

            List<Sheep> carriedStack = session.sheepCarriers.entrySet().stream()
                    .filter(carried -> carrierId.equals(carried.getValue()))
                    .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toList());
            int stackIndex = Math.max(0, carriedStack.indexOf(sheep));
            Vector facing = carrier.getLocation().getDirection().setY(0.0D);
            if (facing.lengthSquared() < 0.001D) facing = new Vector(0.0D, 0.0D, 1.0D);
            Location following = carrier.getLocation().clone()
                    .subtract(facing.normalize().multiply(1.35D + stackIndex * 0.9D));
            following.setY(carrier.getLocation().getY());
            following.setYaw(carrier.getLocation().getYaw());
            following.setPitch(0.0F);
            sheep.teleport(following);

            int team = session.teams.getOrDefault(carrierId, -1);
            if (team < 0 || team > 3) continue;
            if (horizontalDistanceSquared(carrier.getLocation(), sheepQuestPenCenter(session, team)) > 20.25D) continue;

            int previousOwner = session.sheepOwners.getOrDefault(sheep, -1);
            session.sheepOwners.put(sheep, team);
            sheep.setColor(sheepTeamDyeColor(team));
            sheep.setAI(false);
            Location pen = sheepQuestPenCenter(session, team);
            sheep.teleport(pen.clone().add(ThreadLocalRandom.current().nextDouble(-1.0D, 1.0D),
                    0.0D, ThreadLocalRandom.current().nextDouble(-2.0D, 2.0D)));
            iterator.remove();
            if (previousOwner != team) {
                session.scores.merge(carrierId, 1, Integer::sum);
                carrier.playSound(carrier.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 1.0F, 1.5F);
                carrier.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, carrier.getLocation().add(0, 1, 0),
                        12, 0.35D, 0.4D, 0.35D, 0.04D);
                announce(session, (previousOwner < 0 ? ChatColor.GREEN + "Sheep captured by " :
                        ChatColor.YELLOW + "Sheep stolen by ") + carrier.getName() + " for "
                        + sheepTeamChatColor(team) + sheepTeamName(team) + ChatColor.WHITE + "!");
            } else {
                carrier.sendMessage(ChatColor.GREEN + "Sheep returned to your team's pen.");
            }
        }

        for (Sheep sheep : session.sheep) {
            if (!sheep.isValid() || session.sheepCarriers.containsKey(sheep)) continue;
            int owner = session.sheepOwners.getOrDefault(sheep, -1);
            if (owner >= 0 && !isInsideSheepPen(session, sheep.getLocation(), owner)) {
                Location pen = sheepQuestPenCenter(session, owner);
                sheep.teleport(pen.clone());
                sheep.setAI(false);
            }
        }
    }

    private int sheepQuestTeamCount(Session session, int team) {
        int count = 0;
        for (Sheep sheep : session.sheep) {
            if (session.sheepOwners.getOrDefault(sheep, -1) == team
                    && isInsideSheepPen(session, sheep.getLocation(), team)) count++;
        }
        return count;
    }

    private int[] sheepQuestTeamCounts(Session session) {
        int[] counts = new int[4];
        for (int team = 0; team < counts.length; team++) {
            counts[team] = sheepQuestTeamCount(session, team);
        }
        return counts;
    }

    private Player sheepQuestLeader(Session session) {
        int[] counts = sheepQuestTeamCounts(session);
        int winningTeam = ArcadePortRules.sheepQuestLeadingTeam(counts);
        if (winningTeam < 0) return null;
        return session.players.values().stream()
                .filter(player -> player != null && player.isOnline()
                        && session.teams.getOrDefault(player.getUniqueId(), -1) == winningTeam)
                .max(java.util.Comparator.comparingInt(player -> session.scores.getOrDefault(player.getUniqueId(), 0)))
                .orElse(null);
    }

    private void spawnSheep(Session session) {
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null && !mapData.dataLocations("WHITE").isEmpty()) {
            List<Location> sourceSpawns = mapData.dataLocations("WHITE");
            Location sourceSpawn = sourceSpawns.get(ThreadLocalRandom.current().nextInt(sourceSpawns.size())).clone();
            configureQuestSheep(session, session.world.spawn(sourceSpawn, Sheep.class));
            return;
        }
        for (int attempt = 0; attempt < 18; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(-10, 11);
            int z = ThreadLocalRandom.current().nextInt(-HALF_SIZE + 4, HALF_SIZE - 3);
            Location loc = new Location(session.world, x + 0.5D, 64.0D, z + 0.5D);
            if (session.world.getBlockAt(x, 63, z).getType().isAir()) continue;
            Sheep sheep = session.world.spawn(loc, Sheep.class);
            configureQuestSheep(session, sheep);
            return;
        }
    }

    private void configureQuestSheep(Session session, Sheep sheep) {
        sheep.setColor(DyeColor.WHITE);
        sheep.setCustomName(ChatColor.WHITE + "Sheep");
        sheep.setCustomNameVisible(true);
        sheep.setAI(true);
        sheep.setInvulnerable(true);
        sheep.setRemoveWhenFarAway(false);
        session.sheep.add(sheep);
        session.sheepOwners.put(sheep, -1);
        session.entities.add(sheep);
    }

    private void updateTitles(Session session) {
        int alive = session.alive.size();
        long seconds = Math.max(0L, (session.endsAt - System.currentTimeMillis()) / 1000L);
        for (UUID id : session.players.keySet()) {
            Player p = session.players.get(id);
            if (!p.isOnline()) continue;
            int score = session.scores.getOrDefault(id, 0);
            String specific = gameStatusText(session, p);
            String timeText = seconds + "s";
            p.sendActionBar(ChatColor.GOLD + session.type.displayName() + ChatColor.GRAY + " | " + timeText
                    + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + "alive " + alive
                    + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + "score " + score
                    + (specific.isEmpty() ? "" : ChatColor.DARK_GRAY + " | " + specific));
        }
    }

    private String dragonEscapeStatusText(Session session, Player player) {
        if (!session.dragonEscapeWaypoints.isEmpty()) {
            int waypoint = dragonEscapePlayerProgress(session, player);
            return "route " + Math.min(waypoint + 1, session.dragonEscapeWaypoints.size())
                    + "/" + session.dragonEscapeWaypoints.size();
        }
        return "finish " + Math.max(0, player.getLocation().getBlockZ()) + "/"
                + (int) ArcadePortRules.DRAGON_ESCAPE_FINISH_Z;
    }

    private String gameStatusText(Session session, Player player) {
        UUID id = player.getUniqueId();
        return switch (session.type) {
            case BACON_BRAWL -> "knockback x" + String.format(java.util.Locale.ROOT, "%.1f", session.baconKnockback);
            case BOMB_LOBBERS -> "bombs " + inventoryCount(player, Material.TNT);
            case DEATH_TAG -> "IT " + session.tagged.stream().filter(session.alive::contains).count() + " | runners "
                    + Math.max(0, session.alive.size() - session.tagged.stream().filter(session.alive::contains).count());
            case DRAGON_ESCAPE -> dragonEscapeStatusText(session, player);
            case DRAGONS -> "dragons " + session.entities.stream().filter(EnderDragon.class::isInstance).count();
            case EVOLUTION -> "stage " + (session.evolutionLevels.getOrDefault(id, 0) + 1) + "/6";
            case GLADIATORS -> {
                String tier = switch (session.gladiatorRound) {
                    case 1 -> "yellow";
                    case 2 -> "orange";
                    case 3 -> "red";
                    default -> "green";
                };
                yield tier + " | matches " + Math.max(0, session.gladiatorPairCount - session.gladiatorResolvedPairs.size());
            }
            case MICRO_BATTLES -> "team " + (session.teams.getOrDefault(id, 0) + 1) + "/4";
            case ONE_IN_THE_QUIVER -> "kills " + scoreFor(session, id) + "/20";
            case PAINTBALL -> "armour " + session.paintPiecesRemaining.getOrDefault(id, 4) + "/4";
            case RUNNER -> "unstable tiles " + session.runnerBlocks.size();
            case SHEEP_QUEST -> "pens R" + sheepQuestTeamCount(session, 0)
                    + " A" + sheepQuestTeamCount(session, 1) + " G" + sheepQuestTeamCount(session, 2)
                    + " Y" + sheepQuestTeamCount(session, 3);
            case SPLEEF -> "snowballs " + inventoryCount(player, Material.SNOWBALL);
            case TURF_WARS -> (session.turfCombat ? "COMBAT" : "BUILD") + " | turf " + session.turfFrontlineX;
        };
    }

    private int scoreFor(Session session, UUID id) {
        return session.scores.getOrDefault(id, 0);
    }

    private int inventoryCount(Player player, Material material) {
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) amount += item.getAmount();
        }
        return amount;
    }

    private void checkRoundEnd(Session session) {
        if (active != session) return;
        // An empty live set is a real end condition even during a one-player test.
        if (session.alive.isEmpty()) {
            if (!ArcadePortRules.shouldFinishWhenNobodyIsAlive(session.alive.size(), session.respawning.size())) return;
            finish(session, null, true);
            return;
        }
        if (session.type == ArcadePortGame.GLADIATORS && session.participantsAtStart > 1) {
            if (session.alive.size() <= 1) {
                finish(session, alivePlayers(session).stream().findFirst().orElse(null), true);
                return;
            }
            if (ArcadePortRules.allGladiatorPairsResolved(session.gladiatorPairCount, session.gladiatorResolvedPairs.size())
                    && session.nextGladiatorRoundAt == 0L) {
                session.nextGladiatorRoundAt = System.currentTimeMillis() + 2_000L;
                announce(session, ChatColor.GREEN + "All matches are complete. Advancing the winners...");
            }
            return;
        }
        if ((session.type == ArcadePortGame.MICRO_BATTLES || session.type == ArcadePortGame.PAINTBALL
                || session.type == ArcadePortGame.BOMB_LOBBERS)
                && session.participantsAtStart > 1) {
            Set<Integer> remainingTeams = new HashSet<>();
            for (UUID id : session.alive) remainingTeams.add(session.teams.getOrDefault(id, -1));
            if (remainingTeams.size() <= 1) {
                int winnerTeam = remainingTeams.stream().findFirst().orElse(-1);
                Player winner = alivePlayers(session).stream()
                        .filter(p -> session.teams.getOrDefault(p.getUniqueId(), -2) == winnerTeam)
                        .max(java.util.Comparator.comparingInt(p -> session.scores.getOrDefault(p.getUniqueId(), 0)))
                        .orElse(null);
                finish(session, winner, true);
            }
            return;
        }
        if (session.participantsAtStart <= 1) return;
        if (session.type == ArcadePortGame.PAINTBALL || session.type == ArcadePortGame.TURF_WARS
                || session.type == ArcadePortGame.SHEEP_QUEST || session.type == ArcadePortGame.EVOLUTION
                || session.type == ArcadePortGame.ONE_IN_THE_QUIVER) return;
        if (session.type == ArcadePortGame.DEATH_TAG) {
            List<Player> runners = alivePlayers(session).stream()
                    .filter(p -> !session.tagged.contains(p.getUniqueId()))
                    .toList();
            if (ArcadePortRules.shouldFinishDeathTagRound(runners.size(), session.participantsAtStart)) {
                Player winner;
                if (runners.size() == 1) {
                    winner = runners.get(0);
                } else if (ArcadePortRules.deathTagChasersWinRound(runners.size())) {
                    winner = alivePlayers(session).stream()
                            .filter(player -> session.tagged.contains(player.getUniqueId()))
                            .max(java.util.Comparator.comparingInt(player ->
                                    session.scores.getOrDefault(player.getUniqueId(), 0)))
                            .orElse(null);
                } else {
                    winner = null;
                }
                finish(session, winner, true);
            }
            return;
        }
        if (session.alive.size() <= 1) finish(session, alivePlayers(session).stream().findFirst().orElse(null), true);
    }

    private void eliminate(Session session, Player player, Player killer, String reason) {
        if (active != session || player == null) return;
        if (session.type == ArcadePortGame.SHEEP_QUEST && session.alive.contains(player.getUniqueId())) {
            dropSheepFromCarrier(session, player, reason);
            if (!session.alive.remove(player.getUniqueId())) return;
            UUID playerId = player.getUniqueId();
            session.respawning.add(playerId);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "You dropped your sheep and will respawn shortly.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (active != session || !player.isOnline()
                        || !session.players.containsKey(playerId) || !session.respawning.remove(playerId)) return;
                Location spawn = teamSpawnFor(session, player, 4);
                player.teleport(spawn);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(20.0F);
                player.setFireTicks(0);
                session.alive.add(playerId);
                session.lastLocations.put(playerId, spawn.clone());
                giveKit(session, player);
            }, 120L);
            return;
        }
        if (session.type == ArcadePortGame.DEATH_TAG && session.participantsAtStart > 1
                && session.alive.contains(player.getUniqueId())) {
            makeDeathTagChaser(session, player, reason);
            checkRoundEnd(session);
            return;
        }
        if (!session.alive.remove(player.getUniqueId())) return;
        player.leaveVehicle();
        if (session.type == ArcadePortGame.PAINTBALL && player.isOnline()) {
            spawnPaintballSaveMeMarker(session, player);
            player.setGameMode(GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage(ChatColor.RED + "Painted out! A teammate can throw a Water Bomb at your SAVE ME marker.");
            if (killer != null && session.alive.contains(killer.getUniqueId())) {
                session.scores.merge(killer.getUniqueId(), 1, Integer::sum);
                killer.sendMessage(ChatColor.GREEN + "Paintball elimination!");
                checkWinnerForScore(session, killer);
            }
            checkRoundEnd(session);
            return;
        }
        if (session.type == ArcadePortGame.EVOLUTION && player.isOnline()) {
            UUID victimId = player.getUniqueId();
            session.evolutionLifeKills.put(victimId, 0);
            session.evolutionAvailable.remove(victimId);
            session.evolutionCharge.remove(victimId);
            session.evolving.remove(victimId);
            session.evolutionLockedLocations.remove(victimId);
            session.evolutionWaterMessageAt.remove(victimId);
            player.setExp(0.0F);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            if (killer != null && session.alive.contains(killer.getUniqueId())) {
                UUID killerId = killer.getUniqueId();
                session.scores.merge(killerId, 1, Integer::sum);
                int lifeKills = session.evolutionLifeKills.merge(killerId, 1, Integer::sum);
                session.evolutionAvailable.add(killerId);
                session.evolutionCharge.remove(killerId);
                killer.setExp(0.0F);
                killer.removePotionEffect(PotionEffectType.SLOWNESS);
                killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, true));
                killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + killer.getMaxHealth() / 2.0D));
                killer.sendMessage(ChatColor.GREEN + "Kill registered (" + lifeKills
                        + " this life). Hold sneak while grounded to evolve.");
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.1F);
            }
            int victimLevel = session.evolutionLevels.getOrDefault(victimId, 0);
            session.respawning.add(victimId);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "Defeated. Respawning with evolution stage " + (victimLevel + 1) + " retained.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (active != session || !player.isOnline()
                        || !session.respawning.remove(victimId)) return;
                Location spawn = spawnFor(session, player);
                player.teleport(spawn);
                player.setGameMode(GameMode.SURVIVAL);
                player.setFoodLevel(20);
                player.setSaturation(20.0F);
                session.alive.add(victimId);
                session.lastLocations.put(victimId, spawn.clone());
                giveEvolutionKit(session, player);
            }, ArcadePortRules.EVOLUTION_RESPAWN_TICKS);
            return;
        }
        if (player.isOnline()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "Eliminated: " + reason + ".");
        }
        if (killer != null && session.scores.containsKey(killer.getUniqueId())) session.scores.merge(killer.getUniqueId(), 1, Integer::sum);
        if (session.type == ArcadePortGame.GLADIATORS) {
            Integer pair = session.gladiatorPairs.get(player.getUniqueId());
            if (pair != null) {
                session.gladiatorResolvedPairs.add(pair);
                if (killer != null && killer.isOnline()) killer.sendMessage(ChatColor.GREEN + "You won your arena fight and advanced!");
            }
        }
        checkRoundEnd(session);
    }

    private void giveEvolutionKit(Session session, Player player) {
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        int level = Math.max(0, Math.min(5, session.evolutionLevels.getOrDefault(player.getUniqueId(), 0)));
        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(ArcadePortRules.evolutionMaxHealth(level));
        player.setHealth(player.getMaxHealth());
        switch (level) {
            case 0:
                player.getInventory().addItem(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.SHIELD));
                player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1_200, 0, false, false, true));
                break;
            case 1:
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD),
                        namedItem(Material.FIRE_CHARGE, "Blaze Fireball", "Right-click to launch a fireball.", 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1_200, 0, false, false, true));
                break;
            case 2:
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD),
                        namedItem(Material.COBWEB, "Spider Web", "Right-click to place a temporary web.", 4));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1_200, 0, false, false, true));
                break;
            case 3:
                player.getInventory().addItem(new ItemStack(Material.IRON_SWORD),
                        namedItem(Material.TNT, "Creeper Charge", "Right-click to throw an explosive charge.", 3));
                break;
            case 4:
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD),
                        namedItem(Material.SLIME_BALL, "Slime Bounce", "Right-click to leap forward.", 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 1_200, 1, false, false, true));
                break;
            default:
                player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD),
                        namedItem(Material.FEATHER, "Egg Launch", "Right-click to throw an egg; three eggs are carried.", 1),
                        new ItemStack(Material.EGG, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1_200, 1, false, false, true));
                break;
        }
        player.sendMessage(ChatColor.GREEN + "Evolution stage restored: " + (level + 1) + "/6.");
        player.updateInventory();
    }

    private void refreshEvolutionFormPerks(Session session, Player player) {
        int level = Math.max(0, Math.min(5, session.evolutionLevels.getOrDefault(player.getUniqueId(), 0)));
        PotionEffectType type;
        int amplifier;
        switch (level) {
            case 0 -> { type = PotionEffectType.RESISTANCE; amplifier = 0; }
            case 1 -> { type = PotionEffectType.FIRE_RESISTANCE; amplifier = 0; }
            case 2 -> { type = PotionEffectType.SPEED; amplifier = 0; }
            case 4 -> { type = PotionEffectType.JUMP_BOOST; amplifier = 1; }
            case 5 -> { type = PotionEffectType.SPEED; amplifier = 1; }
            default -> { return; }
        }
        PotionEffect current = player.getPotionEffect(type);
        if (current == null || current.getDuration() <= 20 || current.getAmplifier() != amplifier) {
            player.addPotionEffect(new PotionEffect(type, 60, amplifier, false, false, true));
        }
    }

    private void updateEvolutionProgress(Session session, long now) {
        if (active != session) return;
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (session.evolving.contains(id)) continue;
            refreshEvolutionFormPerks(session, player);
            if (!session.evolutionAvailable.contains(id)) {
                if (player.getExp() != 0.0F) player.setExp(0.0F);
                continue;
            }
            if (!player.isSneaking()) {
                cancelEvolutionCharge(session, player);
                if (now % 2_000L < 50L) player.sendActionBar(ChatColor.GREEN + "Kill an opponent, then hold sneak while grounded to evolve.");
                continue;
            }
            if (player.getLocation().getBlock().isLiquid()) {
                cancelEvolutionCharge(session, player);
                long lastNotice = session.evolutionWaterMessageAt.getOrDefault(id, 0L);
                if (now - lastNotice >= 2_000L) {
                    session.evolutionWaterMessageAt.put(id, now);
                    player.sendMessage(ChatColor.YELLOW + "You cannot evolve in water!");
                }
                continue;
            }
            Block below = player.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
            if (!player.isOnGround() || below.getType() == Material.DRAGON_EGG) {
                if (session.evolutionCharge.containsKey(id)) player.sendActionBar(ChatColor.YELLOW + "Land on solid ground to charge your evolution.");
                continue;
            }
            float progress = session.evolutionCharge.getOrDefault(id, 0.0F);
            progress = (float) Math.min(0.9999D, progress
                    + ArcadePortRules.evolutionChargePerTick(session.evolutionLifeKills.getOrDefault(id, 0)));
            session.evolutionCharge.put(id, progress);
            player.setExp(progress);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 4, 1, false, false, true));
            if (session.ticks % 5 == 0) {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        player.getLocation().add(0.0D, 1.0D, 0.0D), 3, 0.35D, 0.45D, 0.35D, 0.0D);
            }
            if (session.ticks % 10 == 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.55F, progress + 0.6F);
            }
            player.sendActionBar(ChatColor.GREEN + "EVOLVING " + ChatColor.WHITE
                    + Math.min(100, (int) (progress * 100.0F)) + "%");
            if (progress >= 0.9999F) beginEvolution(session, player);
        }
    }

    private void cancelEvolutionCharge(Session session, Player player) {
        UUID id = player.getUniqueId();
        session.evolutionCharge.remove(id);
        player.setExp(0.0F);
        // Slowness is charge feedback. Jump Boost belongs to the Slime evolution form,
        // so cancelling a charge must not remove that form's gameplay perk.
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    private void beginEvolution(Session session, Player player) {
        UUID id = player.getUniqueId();
        if (active != session || !session.alive.contains(id)
                || !session.evolutionAvailable.remove(id) || !session.evolving.add(id)) return;
        session.evolutionCharge.remove(id);
        session.evolutionLifeKills.put(id, 0);
        session.evolutionLockedLocations.put(id, player.getLocation().clone());
        player.setExp(0.0F);
        player.setSneaking(false);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setFireTicks(0);
        player.setHealth(player.getMaxHealth());
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8F, 1.0F);
        player.sendTitle(ChatColor.GREEN + "EVOLVING", ChatColor.YELLOW + "Keep still...", 0, 44, 8);
        player.sendMessage(ChatColor.GREEN + "Evolution charge complete. Hold still while your form changes.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active != session || !session.evolving.contains(id)) return;
            if (!player.isOnline() || !session.alive.contains(id)) {
                session.evolving.remove(id);
                session.evolutionLockedLocations.remove(id);
                return;
            }
            int nextLevel = ArcadePortRules.nextEvolutionLevel(
                    session.evolutionLevels.getOrDefault(id, 0), true);
            session.evolutionLevels.put(id, nextLevel);
            if (ArcadePortRules.isEvolutionWinnerLevel(nextLevel)) {
                session.evolving.remove(id);
                session.evolutionLockedLocations.remove(id);
                finish(session, player, true);
                return;
            }
            giveEvolutionKit(session, player);
            player.sendMessage(ChatColor.GREEN + "You evolved into stage " + (nextLevel + 1) + "/6.");
        }, ArcadePortRules.EVOLUTION_FORM_CHANGE_TICKS);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active != session || !session.evolving.remove(id)) return;
            session.evolutionLockedLocations.remove(id);
            if (!player.isOnline() || !session.alive.contains(id)) return;
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFireTicks(0);
            player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0.0D, 1.0D, 0.0D),
                    1, 0.1D, 0.1D, 0.1D, 0.0D);
        }, ArcadePortRules.EVOLUTION_TOTAL_ANIMATION_TICKS);
    }

    private void updateEvolutionWebs(Session session, long now) {
        java.util.Iterator<Map.Entry<Block, Long>> iterator = session.evolutionWebs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Block, Long> entry = iterator.next();
            Block block = entry.getKey();
            if (block == null || block.getType() != Material.COBWEB) {
                iterator.remove();
                continue;
            }
            if (now >= entry.getValue()) {
                block.setType(Material.AIR, false);
                iterator.remove();
            }
        }
    }

    /** Keep respawning team players on their own archived team spawns. */
    private Location teamSpawnFor(Session session, Player player, int teamCount) {
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        int team = session.teams.getOrDefault(player.getUniqueId(), -1);
        if (mapData != null && team >= 0 && team < teamCount) {
            List<Location> teamSpawns = mapData.teamSpawnLocationsForTeam(team, teamCount);
            if (!teamSpawns.isEmpty()) {
                return teamSpawns.get(ThreadLocalRandom.current().nextInt(teamSpawns.size())).clone();
            }
        }
        return spawnFor(session, player);
    }

    private Location spawnFor(Session session, Player player) {
        List<Player> players = new ArrayList<>(session.players.values());
        int index = Math.max(0, players.indexOf(player));
        List<Location> spawns = spawnLocations(session.type, session.players.size());
        return spawns.get(index % spawns.size()).clone();
    }

    private void checkWinnerForScore(Session session, Player player) {
        if (session.type == ArcadePortGame.PAINTBALL
                && ArcadePortRules.isPaintballWinnerScoreReached(
                        session.scores.getOrDefault(player.getUniqueId(), 0))) {
            finish(session, player, true);
        }
        if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER
                && ArcadePortRules.isQuiverWinnerScoreReached(session.scores.getOrDefault(player.getUniqueId(), 0))) {
            finish(session, player, true);
        }
    }

    private void handlePlayerHit(Session session, Player attacker, Player target, String source) {
        handlePlayerHit(session, attacker, target, source, 1);
    }

    private void handlePlayerHit(Session session, Player attacker, Player target, String source, int paintballAmount) {
        if (active != session || attacker.equals(target) || !session.alive.contains(attacker.getUniqueId())
                || !session.alive.contains(target.getUniqueId())) return;
        if (session.type == ArcadePortGame.EVOLUTION) {
            if (session.evolving.contains(attacker.getUniqueId()) || session.evolving.contains(target.getUniqueId())) return;
            cancelEvolutionCharge(session, target);
        }
        if (session.type == ArcadePortGame.GLADIATORS) {
            Integer attackerPair = session.gladiatorPairs.get(attacker.getUniqueId());
            Integer targetPair = session.gladiatorPairs.get(target.getUniqueId());
            if (!ArcadePortRules.canGladiatorsFight(System.currentTimeMillis(), session.gladiatorFightStartsAt,
                    attackerPair == null ? -1 : attackerPair,
                    targetPair == null ? -2 : targetPair,
                    attackerPair != null && session.gladiatorResolvedPairs.contains(attackerPair))) return;
        }
        if ((session.type == ArcadePortGame.PAINTBALL || session.type == ArcadePortGame.TURF_WARS
                || session.type == ArcadePortGame.MICRO_BATTLES || session.type == ArcadePortGame.SHEEP_QUEST)
                && session.teams.getOrDefault(attacker.getUniqueId(), -1).equals(session.teams.getOrDefault(target.getUniqueId(), -2))) return;
        if (session.type == ArcadePortGame.PAINTBALL && "melee".equals(source)) return;
        if (session.type == ArcadePortGame.TURF_WARS && !session.turfCombat) return;
        if (session.type == ArcadePortGame.DRAGONS) return; // PvP is disabled; use Pyro against dragons.
        if (session.type == ArcadePortGame.SPLEEF) return; // Player combat is disabled; projectiles target the floor.

        switch (session.type) {
            case BACON_BRAWL:
                handleBaconBrawlHit(session, attacker, target, source);
                break;
            case BOMB_LOBBERS:
                // Bomb Lobbers uses thrown TNT; melee attacks do not deal hits.
                break;
            case DEATH_TAG: {
                boolean attackerChaser = session.tagged.contains(attacker.getUniqueId());
                boolean targetChaser = session.tagged.contains(target.getUniqueId());
                if (attackerChaser == targetChaser) break;
                if (ArcadePortRules.shouldSpreadDeathTag(attackerChaser, targetChaser)) {
                    double damage = ArcadePortRules.deathTagChaserDamage(
                            session.alphaChasers.contains(attacker.getUniqueId()));
                    if (ArcadePortRules.deathTagHitIsLethal(target.getHealth(), damage)) {
                        makeDeathTagChaser(session, target, "was killed by a chaser");
                        checkRoundEnd(session);
                    } else {
                        target.setHealth(target.getHealth() - damage);
                        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9F, 0.75F);
                    }
                } else {
                    String kit = session.deathTagRunnerKits.getOrDefault(attacker.getUniqueId(), "basher");
                    double baseDamage = ArcadePortRules.deathTagRunnerDamage(kit, "projectile hit".equals(source));
                    double damage = ArcadePortRules.deathTagDamageAfterIronSkin(baseDamage,
                            session.alphaChasers.contains(target.getUniqueId()));
                    if (damage > 0.0D) {
                        if (ArcadePortRules.deathTagHitIsLethal(target.getHealth(), damage)) {
                            makeDeathTagChaser(session, target, "was defeated by a runner");
                        } else {
                            target.setHealth(target.getHealth() - damage);
                            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.05F);
                        }
                    }
                    if ("basher".equals(kit) && !"projectile hit".equals(source)) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false, true));
                    } else if ("traitor".equals(kit) && !"projectile hit".equals(source)) {
                        Vector knockbackDirection = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
                        double multiplier = ArcadePortRules.deathTagKnockbackMultiplier(
                                session.alphaChasers.contains(target.getUniqueId()));
                        if (knockbackDirection.lengthSquared() > 0.001D) target.setVelocity(knockbackDirection.normalize().multiply(0.8D * multiplier).setY(0.1D));
                    }
                }
                break;
            }
            case PAINTBALL:
                applyPaintballHit(session, attacker, target, paintballAmount);
                break;
            case MICRO_BATTLES:
                applyMicroBattlesHit(session, attacker, target, source);
                break;
            case TURF_WARS: {
                ItemStack heldWeapon = attacker.getInventory().getItemInMainHand();
                boolean ironSword = "melee".equals(source) && heldWeapon != null
                        && heldWeapon.getType() == Material.IRON_SWORD;
                double damage = ArcadePortRules.turfWarsHitDamage(!"melee".equals(source), ironSword);
                applyTurfWarsHit(session, attacker, target, damage);
                break;
            }
            case EVOLUTION: {
                int level = Math.max(0, Math.min(5, session.evolutionLevels.getOrDefault(attacker.getUniqueId(), 0)));
                double damage = ArcadePortRules.evolutionMeleeDamage(level);
                if (target.getHealth() <= damage) {
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
                    eliminate(session, target, attacker, "was defeated by " + attacker.getName());
                } else {
                    target.setHealth(target.getHealth() - damage);
                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
                }
                break;
            }
            case SHEEP_QUEST:
                // Hits drop a carrier's stack and still deal damage, as in the original TeamGame.
                dropSheepFromCarrier(session, target, "was hit");
                double sheepQuestDamage = ArcadePortRules.SHEEP_QUEST_PLAYER_HIT_DAMAGE;
                if (target.getHealth() <= sheepQuestDamage) {
                    eliminate(session, target, attacker, "was defeated while carrying sheep");
                } else {
                    target.setHealth(target.getHealth() - sheepQuestDamage);
                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
                }
                break;
            case ONE_IN_THE_QUIVER:
                if ("arrow hit".equals(source)) scoreQuiverKill(session, attacker, target);
                else handleQuiverMeleeHit(session, attacker, target);
                break;
            default:
                eliminate(session, target, attacker, source);
                break;
        }
    }

    private void applyTurfWarsHit(Session session, Player attacker, Player target, double damage) {
        if (active != session || !session.alive.contains(target.getUniqueId())
                || !session.alive.contains(attacker.getUniqueId())) return;
        if (target.getHealth() > damage) {
            target.setHealth(target.getHealth() - damage);
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0D, 1.0D, 0.0D),
                    5, 0.25D, 0.35D, 0.25D, 0.05D);
            return;
        }

        UUID targetId = target.getUniqueId();
        eliminate(session, target, attacker, "was defeated in Turf Wars");
        if (active != session) return;
        advanceTurfWars(session, attacker);
        if (active != session) return;
        session.respawning.add(targetId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active != session || !target.isOnline()
                    || !session.players.containsKey(targetId) || !session.respawning.remove(targetId)) return;
            int playerIndex = new ArrayList<>(session.players.keySet()).indexOf(targetId);
            int slots = Math.max(1, (session.players.size() + 1) / 2);
            Location spawn = turfWarsSpawn(session, session.teams.getOrDefault(targetId, 0),
                    Math.max(0, playerIndex / 2), slots);
            target.teleport(spawn);
            target.setGameMode(GameMode.SURVIVAL);
            target.setHealth(target.getMaxHealth());
            target.setFoodLevel(20);
            target.setSaturation(20.0F);
            target.setFireTicks(0);
            session.alive.add(targetId);
            session.lastLocations.put(targetId, spawn.clone());
            giveKit(session, target);
            target.sendMessage(ChatColor.GREEN + "Respawned. Every kill moves the turf line.");
        }, 80L);
    }

    private void advanceTurfWars(Session session, Player attacker) {
        int team = session.teams.getOrDefault(attacker.getUniqueId(), -1);
        if (team != 0 && team != 1) return;
        int oldFrontline = session.turfFrontlineX;
        int nextFrontline = ArcadePortRules.advanceTurfFrontline(oldFrontline, team);
        int paintedX = team == 0 ? nextFrontline : oldFrontline;
        session.turfFrontlineX = nextFrontline;
        Material turf = team == 0 ? Material.RED_TERRACOTTA : Material.BLUE_TERRACOTTA;
        for (int z = session.turfMinZ; z < session.turfMaxZExclusive; z++) {
            session.world.getBlockAt(paintedX, session.turfFloorY, z).setType(turf, false);
            for (int y = session.turfFloorY + 1; y <= session.turfFloorY + 5; y++) {
                Block structure = session.world.getBlockAt(paintedX, y, z);
                if (structure.getType() == Material.RED_WOOL || structure.getType() == Material.BLUE_WOOL) {
                    structure.setType(Material.AIR, false);
                }
            }
        }
        if (ArcadePortRules.isTurfWarsWinningLine(nextFrontline, team,
                session.turfFrontlineMinX, session.turfFrontlineMaxXExclusive)) {
            finish(session, attacker, true);
            return;
        }
        announce(session, (team == 0 ? ChatColor.RED + "Red" : ChatColor.BLUE + "Blue")
                + ChatColor.WHITE + " advanced the turf line to " + nextFrontline + ".");
    }

    private void scoreQuiverKill(Session session, Player attacker, Player target) {
        if (active != session || !session.alive.contains(attacker.getUniqueId())
                || !session.alive.contains(target.getUniqueId())) return;

        int kills = session.scores.merge(attacker.getUniqueId(), 1, Integer::sum);
        attacker.sendMessage(ChatColor.GREEN + "Quiver kill! " + kills + "/20.");
        checkWinnerForScore(session, attacker);
        if (active != session) return;
        activateQuiverNinjaVanish(session, attacker, System.currentTimeMillis());

        UUID targetId = target.getUniqueId();
        target.leaveVehicle();
        target.setGameMode(GameMode.SPECTATOR);
        session.alive.remove(targetId);
        session.respawning.add(targetId);
        target.sendMessage(ChatColor.RED + "Eliminated! Respawning in one second.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active != session || !target.isOnline()
                    || !session.players.containsKey(targetId) || !session.respawning.remove(targetId)) return;
            Location spawn = spawnFor(session, target);
            target.teleport(spawn);
            target.setGameMode(GameMode.SURVIVAL);
            target.setHealth(target.getMaxHealth());
            target.setFoodLevel(20);
            target.setSaturation(20.0F);
            target.setFireTicks(0);
            target.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            session.alive.add(targetId);
            session.lastLocations.put(targetId, spawn.clone());
            giveKit(session, target);
            target.sendMessage(ChatColor.GREEN + "Respawned. First to 20 kills wins.");
        }, ArcadePortRules.QUIVER_RESPAWN_DELAY_TICKS);
    }

    private void hitProjectile(Session session, Projectile projectile, Player shooter, Player target) {
        if (session.type == ArcadePortGame.BOMB_LOBBERS) {
            handlePlayerHit(session, shooter, target, "bomb hit");
        } else if (session.type == ArcadePortGame.PAINTBALL) {
            int paintAmount = session.paintProjectileDamage.getOrDefault(projectile.getUniqueId(), 1);
            session.paintProjectileDamage.remove(projectile.getUniqueId());
            handlePlayerHit(session, shooter, target, "projectile hit", paintAmount);
        } else if (session.type == ArcadePortGame.DEATH_TAG) {
            if (projectile instanceof Arrow && !session.tagged.contains(shooter.getUniqueId())
                    && session.alive.contains(shooter.getUniqueId())
                    && "archer".equals(session.deathTagRunnerKits.getOrDefault(shooter.getUniqueId(), "basher"))
                    && session.tagged.contains(target.getUniqueId())) {
                Vector direction = projectile.getVelocity();
                double multiplier = ArcadePortRules.deathTagKnockbackMultiplier(
                        session.alphaChasers.contains(target.getUniqueId()));
                if (direction.lengthSquared() > 0.001D) target.setVelocity(direction.normalize().multiply(1.0D * multiplier));
            }
            handlePlayerHit(session, shooter, target, "projectile hit");
        } else if (session.type == ArcadePortGame.TURF_WARS) {
            handlePlayerHit(session, shooter, target, "projectile hit");
        } else if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER) {
            UUID targetId = target.getUniqueId();
            tryQuiverRebound(session, projectile, shooter, target);
            handlePlayerHit(session, shooter, target, "arrow hit");
            projectile.remove();
            boolean targetEliminated = !session.alive.contains(targetId);
            if (active == session && ArcadePortRules.quiverRegrantsArrowAfterHit(targetEliminated)
                    && shooter.isOnline() && session.alive.contains(shooter.getUniqueId())) {
                shooter.getInventory().setItem(1, namedItem(Material.ARROW, "Super Arrow",
                        "One arrow per shot; earn another with each kill."));
            }
        } else if (session.type == ArcadePortGame.MICRO_BATTLES) {
            handlePlayerHit(session, shooter, target, "arrow hit");
        } else {
            handlePlayerHit(session, shooter, target, "projectile hit");
        }
    }

    private List<Player> alivePlayers(Session session) {
        List<Player> players = new ArrayList<>();
        for (UUID id : session.alive) {
            Player p = session.players.get(id);
            if (p != null && p.isOnline()) players.add(p);
        }
        return players;
    }

    /**
     * Resolve time-limit winners using each game's actual objective. In team
     * modes, a personal high score must not announce a losing team as winner.
     */
    private Player timeoutWinner(Session session) {
        if (session.type == ArcadePortGame.DEATH_TAG) {
            List<Player> runners = alivePlayers(session).stream()
                    .filter(player -> !session.tagged.contains(player.getUniqueId()))
                    .toList();
            if (ArcadePortRules.shouldDeathTagRunnerWinTimeout(runners.size())) {
                return runners.stream()
                        .max(java.util.Comparator.comparingInt(player ->
                                session.scores.getOrDefault(player.getUniqueId(), 0)))
                        .orElse(null);
            }
            List<Player> chasers = alivePlayers(session).stream()
                    .filter(player -> session.tagged.contains(player.getUniqueId()))
                    .toList();
            if (!chasers.isEmpty()) {
                return chasers.stream()
                        .max(java.util.Comparator.comparingInt(player ->
                                session.scores.getOrDefault(player.getUniqueId(), 0)))
                        .orElse(null);
            }
            return null;
        }
        int team = -1;
        if (session.type == ArcadePortGame.TURF_WARS) {
            team = ArcadePortRules.turfWarsTimeoutLeader(session.turfFrontlineX,
                    session.turfInitialFrontlineX, timeoutTeamKills(session, 0), timeoutTeamKills(session, 1));
        } else if (session.type == ArcadePortGame.MICRO_BATTLES
                || session.type == ArcadePortGame.PAINTBALL
                || session.type == ArcadePortGame.BOMB_LOBBERS) {
            int teamZeroAlive = (int) session.alive.stream()
                    .filter(id -> session.teams.getOrDefault(id, -1) == 0).count();
            int teamOneAlive = (int) session.alive.stream()
                    .filter(id -> session.teams.getOrDefault(id, -1) == 1).count();
            team = ArcadePortRules.teamWithBestTimeoutStanding(
                    teamZeroAlive, teamOneAlive, timeoutTeamKills(session, 0), timeoutTeamKills(session, 1));
        }
        if (session.type == ArcadePortGame.TURF_WARS
                || session.type == ArcadePortGame.MICRO_BATTLES
                || session.type == ArcadePortGame.PAINTBALL
                || session.type == ArcadePortGame.BOMB_LOBBERS) {
            // These games use team objectives at timeout. If the team standings are tied,
            // don't override the draw with one player's personal score.
            return team < 0 ? null : bestTimeoutPlayerOnTeam(session, team);
        }
        return team < 0 ? topScorerOrLastAlive(session) : bestTimeoutPlayerOnTeam(session, team);
    }

    private int timeoutTeamKills(Session session, int team) {
        int total = 0;
        for (Map.Entry<UUID, Integer> entry : session.scores.entrySet()) {
            if (session.teams.getOrDefault(entry.getKey(), -1) == team) total += entry.getValue();
        }
        return total;
    }

    private Player bestTimeoutPlayerOnTeam(Session session, int team) {
        Player best = null;
        int bestScore = Integer.MIN_VALUE;
        // Prefer a living member of the winning team so the announced winner
        // is still in the round; use a surviving participant only as fallback.
        for (UUID id : session.alive) {
            if (session.teams.getOrDefault(id, -1) != team) continue;
            Player candidate = session.players.get(id);
            if (candidate != null && candidate.isOnline()
                    && session.scores.getOrDefault(id, 0) > bestScore) {
                best = candidate;
                bestScore = session.scores.getOrDefault(id, 0);
            }
        }
        if (best != null) return best;
        for (Map.Entry<UUID, Player> entry : session.players.entrySet()) {
            UUID id = entry.getKey();
            Player candidate = entry.getValue();
            if (session.teams.getOrDefault(id, -1) != team || candidate == null || !candidate.isOnline()) continue;
            int score = session.scores.getOrDefault(id, 0);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private Player topScorerOrLastAlive(Session session) {
        // Eliminated spectators keep their round score for the final scoreboard, but cannot
        // win a timeout tie-break. Restrict candidates to players still alive in this round.
        List<Player> alive = alivePlayers(session);
        if (alive.isEmpty()) return null;
        if (alive.size() == 1) return alive.get(0);
        return alive.stream()
                .max(java.util.Comparator.comparingInt(player ->
                        session.scores.getOrDefault(player.getUniqueId(), 0)))
                .orElse(null);
    }

    private void finish(Session session, Player winner, boolean announceWinner) {
        if (active != session) return;
        active = null;
        if (session.tickTask != null) session.tickTask.cancel();
        if (announceWinner) {
            if (session.type == ArcadePortGame.SHEEP_QUEST && winner != null) {
                int winnerTeam = session.teams.getOrDefault(winner.getUniqueId(), -1);
                announce(session, sheepTeamChatColor(winnerTeam) + sheepTeamName(winnerTeam)
                        + ChatColor.GOLD + " team won Sheep Quest!");
            } else {
                announce(session, winner == null ? ChatColor.YELLOW + "Round ended." : ChatColor.GOLD + "" + ChatColor.BOLD + winner.getName() + " won " + session.type.displayName() + "!");
            }
        }
        for (Map.Entry<Block, BlockData> entry : session.deathTagOriginalLightBlocks.entrySet()) {
            Block block = entry.getKey();
            if (block != null && block.getWorld() == session.world) block.setBlockData(entry.getValue(), false);
        }
        session.deathTagOriginalLightBlocks.clear();
        for (Entity entity : new ArrayList<>(session.entities)) if (entity != null && entity.isValid()) entity.remove();
        session.bombOwners.clear();
        session.bombLastLocations.clear();
        session.dragonContactCooldowns.clear();
        session.paintProjectileDamage.clear();
        session.paintballLastShotAt.clear();
        session.paintballWaterBombLastThrowAt.clear();
        session.paintballWaterBombs.clear();
        for (UUID outPlayer : new ArrayList<>(session.paintballSaveMeMarkers.keySet())) {
            removePaintballSaveMeMarker(session, outPlayer);
        }
        session.paintballSaveMeLocations.clear();
        session.quiverArrowBounces.clear();
        session.quiverCloakedUntil.clear();
        for (Block block : session.evolutionWebs.keySet()) if (block.getType() == Material.COBWEB) block.setType(Material.AIR, false);
        session.evolutionWebs.clear();
        session.runnerBlocks.clear();
        for (Map.Entry<UUID, Player> entry : session.players.entrySet()) {
            Player player = entry.getValue();
            PlayerSnapshot snapshot = session.snapshots.get(entry.getKey());
            if (snapshot == null) continue;
            if (player != null && player.isOnline()) snapshot.restore(player);
            else pendingRestore.put(entry.getKey(), snapshot);
        }
        session.sheep.clear();
        session.sheepOwners.clear();
        session.sheepCarriers.clear();
        session.entities.clear();
        session.respawning.clear();
        session.alive.clear();
        session.players.clear();
        session.snapshots.clear();
    }

    private void announce(Session session, String text) {
        for (Player player : session.players.values()) if (player != null && player.isOnline()) player.sendMessage(text);
    }

    public void stop(String reason) {
        if (pendingStart != null) {
            pendingStart.cancel();
            pendingStart = null;
            pendingType = null;
        }
        if (active != null) finish(active, null, false);
        queues.clear();
        plugin.getLogger().info("Modern Arcade ports reset: " + reason);
    }

    public String status() {
        String game = active == null ? "idle" : active.type.displayName() + " (" + active.alive.size() + "/" + active.participantsAtStart + " alive)";
        int queued = queues.values().stream().mapToInt(Map::size).sum();
        return game + "; queued=" + queued;
    }

    public void shutdown() {
        stop("plugin shutdown");
        for (Map.Entry<UUID, PlayerSnapshot> entry : new ArrayList<>(pendingRestore.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) entry.getValue().restore(player);
        }
        pendingRestore.clear();
    }

    private double horizontalDistanceSquared(Location left, Location right) {
        double dx = left.getX() - right.getX();
        double dz = left.getZ() - right.getZ();
        return dx * dx + dz * dz;
    }

    private boolean isInsideSession(Session session, Location location) {
        return location != null && location.getWorld() == session.world;
    }

    private void checkPlayerArena(Session session, Player player) {
        if (!session.alive.contains(player.getUniqueId())) return;
        Location location = player.getLocation();
        double voidThresholdY = session.type == ArcadePortGame.DRAGON_ESCAPE
                ? ArcadePortRules.dragonEscapeVoidThresholdY(session.dragonEscapeMapName) : 58.0D;
        if (location.getY() < voidThresholdY) {
            eliminate(session, player, null, "fell from the arena");
            return;
        }
        if (session.type == ArcadePortGame.MICRO_BATTLES) {
            WorldBorder border = session.world.getWorldBorder();
            Location center = border.getCenter();
            if (ArcadePortRules.isOutsideWorldBorder(location.getX(), location.getZ(),
                    center.getX(), center.getZ(), border.getSize())) {
                eliminate(session, player, null, "caught by the closing border");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Session session = active;
        if (session == null) return;
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (!session.players.containsKey(playerId) || event.getTo() == null) return;
        if (session.type == ArcadePortGame.EVOLUTION && session.evolving.contains(playerId)) {
            Location locked = session.evolutionLockedLocations.get(playerId);
            if (locked != null) {
                Location destination = locked.clone();
                destination.setYaw(event.getTo().getYaw());
                destination.setPitch(event.getTo().getPitch());
                event.setTo(destination);
            }
            return;
        }
        if (!isInsideSession(session, event.getTo())) return;
        if (session.type == ArcadePortGame.MICRO_BATTLES && session.alive.contains(player.getUniqueId())) {
            int team = session.teams.getOrDefault(player.getUniqueId(), -1);
            if (team >= 0 && team < 4) {
                Location ownIsland = microTeamCenter(session, team);
                Location to = event.getTo();
                double ownDistance = horizontalDistanceSquared(to, ownIsland);
                double nearestOther = Double.POSITIVE_INFINITY;
                for (int other = 0; other < 4; other++) {
                    if (other == team) continue;
                    nearestOther = Math.min(nearestOther,
                            horizontalDistanceSquared(to, microTeamCenter(session, other)));
                }
                if (ArcadePortRules.shouldPunishMicroIslandExit(System.currentTimeMillis() - session.startedAt,
                        ownDistance, nearestOther)) {
                    eliminate(session, player, null, "left the starting island before the opening restriction ended");
                    return;
                }
            }
        }
        if (session.type == ArcadePortGame.DRAGON_ESCAPE && session.dragonEscapeWaypoints.isEmpty()
                && ArcadePortRules.isDragonEscapeFinishReached(
                        event.getTo().getX(), event.getTo().getY(), event.getTo().getZ())) {
            // Fixed finish coordinates are only for the generated fallback course.
            // Original Reborn maps finish at their last authored route waypoint.
            finish(session, player, true);
            return;
        }
        checkPlayerArena(session, player);
        if (active != session || !session.alive.contains(player.getUniqueId())) return;
        Location previous = session.lastLocations.get(player.getUniqueId());
        Location current = event.getTo();
        if (previous == null || current == null || (previous.getBlockX() == current.getBlockX() && previous.getBlockZ() == current.getBlockZ())) return;
        if (session.type == ArcadePortGame.DRAGON_ESCAPE
                && ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(
                        !session.dragonEscapeWaypoints.isEmpty(), current.getBlockZ())) {
            int crumbleZ = current.getBlockZ() - 5;
            for (int x = -3; x <= 3; x++) session.world.getBlockAt(x, 63, crumbleZ).setType(Material.AIR, false);
        }
        session.lastLocations.put(player.getUniqueId(), current.clone());
    }

    private Material runnerTrailMaterial(Player owner) {
        // With no cosmetic selected, Reborn's default trail is red clay.
        return Material.RED_TERRACOTTA;
    }

    private boolean insideSourceMapBounds(Session session, int x, int z) {
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null && mapData.hasBounds()) {
            return x >= mapData.minX() && x < mapData.maxX()
                    && z >= mapData.minZ() && z < mapData.maxZ();
        }
        return Math.abs(x) <= HALF_SIZE && Math.abs(z) <= HALF_SIZE;
    }

    private boolean insideRunnerSourceBounds(Session session, int x, int z) {
        return insideSourceMapBounds(session, x, z);
    }

    private void markRunnerFootprints(Session session, long now) {
        for (UUID id : new HashSet<>(session.alive)) {
            Player player = session.players.get(id);
            if (player == null || !player.isOnline() || player.getWorld() != session.world) continue;

            org.bukkit.util.BoundingBox bounds = player.getBoundingBox();
            int minX = (int) Math.floor(bounds.getMinX());
            int maxX = (int) Math.floor(bounds.getMaxX() - 1.0E-6D);
            int minZ = (int) Math.floor(bounds.getMinZ());
            int maxZ = (int) Math.floor(bounds.getMaxZ() - 1.0E-6D);
            // Matches Reborn's player-location minus half a block: while jumping, the sampled block is air.
            int trailY = (int) Math.floor(player.getLocation().getY() - 0.5D);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!insideRunnerSourceBounds(session, x, z)) continue;
                    markRunnerBlock(session, session.world.getBlockAt(x, trailY, z), player, now);
                }
            }
        }
    }

    private void markRunnerBlock(Session session, Block block, Player owner, long now) {
        if (active != session || block == null || block.getY() < 0
                || block.getY() >= session.world.getMaxHeight()
                || !insideRunnerSourceBounds(session, block.getX(), block.getZ())) return;
        boolean aboveIsAir = block.getRelative(BlockFace.UP).getType().isAir();
        if (!ArcadePortRules.shouldMarkRunnerBlock(block.getType().isSolid(),
                block.getType() == Material.BEDROCK, block.isLiquid(), aboveIsAir,
                session.runnerBlocks.containsKey(block))) return;
        Material trail = runnerTrailMaterial(owner);
        block.setType(trail, false);
        session.runnerBlocks.put(block, now);
        session.runnerBlockMaterials.put(block, trail);
    }

    private void processRunnerBlocks(Session session, long now) {
        java.util.Iterator<Map.Entry<Block, Long>> iterator = session.runnerBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Block, Long> entry = iterator.next();
            Block block = entry.getKey();
            Material trail = session.runnerBlockMaterials.get(block);
            if (block == null || trail == null || block.getType() != trail) {
                iterator.remove();
                session.runnerBlockMaterials.remove(block);
                continue;
            }
            if (!ArcadePortRules.shouldDropRunnerBlock(now - entry.getValue())) continue;
            Location fallingLocation = block.getLocation().add(0.5D, 0.0D, 0.5D);
            block.setType(Material.AIR, false);
            iterator.remove();
            session.runnerBlockMaterials.remove(block);
            FallingBlock falling = session.world.spawnFallingBlock(fallingLocation, trail.createBlockData());
            falling.setDropItem(false);
            falling.setHurtEntities(true);
            // Reborn's projectile helper applies a small upward launch before the block falls.
            falling.setVelocity(new Vector(0.0D, 0.3D, 0.0D));
            session.entities.add(falling);
            session.runnerFallingBlockExpiresAt.put(falling.getUniqueId(),
                    now + ArcadePortRules.RUNNER_BLOCK_LIFETIME_MILLIS);
        }
    }

    private boolean isSpleefProtectedBlock(Material material) {
        return !isSpleefFloorBlock(material);
    }

    private boolean isSpleefFloorBlock(Material material) {
        return ArcadePortRules.isSpleefFloorMaterial(material);
    }

    private void restoreSpleefHunger(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        long last = session.spleefLastHungerGainAt.getOrDefault(id, 0L);
        if (now - last >= ArcadePortRules.SPLEEF_HUNGER_RESTORE_COOLDOWN_MILLIS) {
            session.spleefLastHungerGainAt.put(id, now);
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
        }
    }

    private void giveSpleefSnowball(Session session, Player player) {
        if (!"snowballer".equals(session.spleefKits.getOrDefault(player.getUniqueId(), "snowballer"))) return;
        if (inventoryCount(player, Material.SNOWBALL) < ArcadePortRules.SPLEEF_SNOWBALLER_MAX_SNOWBALLS) {
            player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
        }
    }

    private void handleSpleefBlockHit(Session session, Player player, Block block, long now) {
        if (active != session || session.type != ArcadePortGame.SPLEEF || block == null
                || block.getWorld() != session.world || !session.alive.contains(player.getUniqueId())
                || isSpleefProtectedBlock(block.getType())) return;
        restoreSpleefHunger(session, player, now);
        String kit = session.spleefKits.getOrDefault(player.getUniqueId(), "snowballer");
        if ("brawler".equals(kit)) {
            long readyAt = session.spleefSmasherReadyAt.getOrDefault(player.getUniqueId(), 0L);
            if (now >= readyAt) {
                session.spleefSmasherReadyAt.put(player.getUniqueId(),
                        now + ArcadePortRules.SPLEEF_SMASHER_COOLDOWN_MILLIS);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            fadeSpleefBlock(session, block.getRelative(dx, dy, dz), false);
                        }
                    }
                }
                fadeSpleefBlock(session, block, false);
            } else fadeSpleefBlock(session, block, false);
        } else fadeSpleefBlock(session, block, false);
        giveSpleefSnowball(session, player);
    }

    private void updateSpleef(Session session, long now) {
        if (now - session.lastSpleefHungerAt >= ArcadePortRules.SPLEEF_HUNGER_DRAIN_INTERVAL_MILLIS) {
            session.lastSpleefHungerAt = now;
            for (Player player : new ArrayList<>(alivePlayers(session))) {
                if (player.getFoodLevel() > 0) {
                    player.setFoodLevel(Math.max(0,
                            player.getFoodLevel() - ArcadePortRules.SPLEEF_HUNGER_DRAIN_PER_INTERVAL));
                } else if (player.getHealth() <= 1.0D) {
                    eliminate(session, player, null, "starved");
                    if (active != session) return;
                } else {
                    player.setHealth(player.getHealth() - 1.0D);
                }
            }
        }
        if (active != session) return;
        for (Player player : alivePlayers(session)) {
            if (!"archer".equals(session.spleefKits.getOrDefault(player.getUniqueId(), "snowballer"))) continue;
            long nextAt = session.spleefNextArrowAt.getOrDefault(player.getUniqueId(),
                    now + ArcadePortRules.SPLEEF_ARCHER_ARROW_INTERVAL_MILLIS);
            if (now < nextAt) continue;
            if (inventoryCount(player, Material.ARROW) < ArcadePortRules.SPLEEF_ARCHER_MAX_ARROWS) {
                player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow",
                        "A replenishing Spleef arrow.", 1));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.0F);
            }
            session.spleefNextArrowAt.put(player.getUniqueId(),
                    now + ArcadePortRules.SPLEEF_ARCHER_ARROW_INTERVAL_MILLIS);
        }
    }

    private void useSpleefLeap(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        long readyAt = session.spleefLeapReadyAt.getOrDefault(id, 0L);
        if (now < readyAt) {
            player.sendMessage(ChatColor.YELLOW + "Leap ready in " + Math.max(1L, (readyAt - now + 999L) / 1000L) + "s.");
            return;
        }
        session.spleefLeapReadyAt.put(id, now + ArcadePortRules.SPLEEF_BRAWLER_LEAP_COOLDOWN_MILLIS);
        Vector launch = player.getLocation().getDirection().normalize().multiply(1.2D);
        launch.setY(Math.min(1.2D, Math.max(0.2D, launch.getY() + 0.2D)));
        player.setVelocity(launch);
        player.setFallDistance(0.0F);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 8, 0.25D, 0.15D, 0.25D, 0.02D);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.7F, 1.2F);
    }

    private void fadeSpleefBlock(Session session, Block block, boolean slowDamage) {
        if (active != session || session.type != ArcadePortGame.SPLEEF || block == null
                || block.getWorld() != session.world || isSpleefProtectedBlock(block.getType())) return;
        Material material = block.getType();
        if (!slowDamage) {
            breakSpleefBlock(session, block);
            return;
        }
        Material damaged = null;
        if (isSpleefWoolOrTerracotta(material)) {
            damaged = spleefNextColour(material);
        } else if (material == Material.STONE) {
            damaged = Material.COBBLESTONE;
        } else if (material == Material.STONE_BRICKS || material == Material.MOSSY_STONE_BRICKS) {
            damaged = Material.CRACKED_STONE_BRICKS;
        } else if (material == Material.GRASS_BLOCK) {
            damaged = Material.DIRT;
        } else if (material == Material.SPRUCE_PLANKS) {
            damaged = Material.OAK_PLANKS;
        } else if (material == Material.OAK_PLANKS) {
            damaged = Material.BIRCH_PLANKS;
        }
        if (damaged == null || damaged == material) {
            breakSpleefBlock(session, block);
        } else {
            block.setType(damaged, false);
        }
    }

    private boolean isSpleefWoolOrTerracotta(Material material) {
        String name = material.name();
        return name.endsWith("_WOOL") || name.endsWith("_TERRACOTTA") || material == Material.TERRACOTTA;
    }

    private Material spleefNextColour(Material material) {
        return ArcadePortRules.nextSpleefSlowDamageMaterial(material);
    }

    private void breakSpleefBlock(Session session, Block block) {
        if (isSpleefProtectedBlock(block.getType())) return;
        block.setType(Material.AIR, false);
        session.world.spawnParticle(Particle.CLOUD, block.getLocation().add(0.5D, 0.5D, 0.5D),
                4, 0.2D, 0.2D, 0.2D, 0.02D);
        session.world.playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5F, 1.15F);
    }

    private void updateRunnerKits(Session session, long now) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            String kit = session.runnerKits.getOrDefault(id, "leaper");
            if (!"frosty".equals(kit)) continue;
            long next = session.runnerNextFrostBallAt.getOrDefault(id,
                    now + ArcadePortRules.RUNNER_FROSTBALL_INTERVAL_MILLIS);
            if (now < next) continue;
            if (inventoryCount(player, Material.SNOWBALL) < ArcadePortRules.RUNNER_MAX_FROSTBALLS) {
                player.getInventory().addItem(namedItem(Material.SNOWBALL, "Snowball",
                        "Frost Balls slow opponents.", 1));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7F, 1.0F);
            }
            session.runnerNextFrostBallAt.put(id, now + ArcadePortRules.RUNNER_FROSTBALL_INTERVAL_MILLIS);
        }

        Iterator<Map.Entry<UUID, Long>> expiry = session.runnerFallingBlockExpiresAt.entrySet().iterator();
        while (expiry.hasNext()) {
            Map.Entry<UUID, Long> entry = expiry.next();
            if (now < entry.getValue()) continue;
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity instanceof FallingBlock falling && session.entities.remove(falling)) falling.remove();
            expiry.remove();
        }
    }

    private void useRunnerLeap(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        long readyAt = session.runnerLeapReadyAt.getOrDefault(id, 0L);
        if (now < readyAt) {
            player.sendMessage(ChatColor.YELLOW + "Leap ready in " + Math.max(1L, (readyAt - now + 999L) / 1000L) + "s.");
            return;
        }
        session.runnerLeapReadyAt.put(id, now + ArcadePortRules.RUNNER_LEAP_COOLDOWN_MILLIS);
        Vector velocity = player.getLocation().getDirection().normalize().multiply(1.2D);
        velocity.setY(1.2D);
        player.setVelocity(velocity);
        player.setFallDistance(0.0F);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 8, 0.2D, 0.15D, 0.2D, 0.02D);
        player.sendMessage(ChatColor.GREEN + "Leap!");
    }

    private void useRunnerQuickshot(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        long readyAt = session.runnerQuickshotReadyAt.getOrDefault(id, 0L);
        if (now < readyAt) {
            player.sendMessage(ChatColor.YELLOW + "Quickshot ready in " + Math.max(1L, (readyAt - now + 999L) / 1000L) + "s.");
            return;
        }
        session.runnerQuickshotReadyAt.put(id, now + ArcadePortRules.RUNNER_QUICKSHOT_COOLDOWN_MILLIS);
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(player.getLocation().getDirection().multiply(1.2D));
        session.entities.add(arrow);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.9F, 1.1F);
        player.sendMessage(ChatColor.GREEN + "Quickshot!");
    }

    private void handleRunnerFallingBlockHit(Session session, FallingBlock falling, Player target) {
        if (active != session || !session.alive.contains(target.getUniqueId())
                || !ArcadePortRules.shouldRunnerFallingBlockHit(target.getLocation().getY(), falling.getLocation().getY())
                || !session.entities.remove(falling)) return;
        session.runnerFallingBlockExpiresAt.remove(falling.getUniqueId());
        Location impact = falling.getLocation().clone();
        falling.remove();
        Vector recoil = target.getLocation().toVector().subtract(impact.toVector());
        if (recoil.lengthSquared() < 0.001D) recoil = new Vector(0.0D, 1.0D, 0.0D);
        target.setVelocity(recoil.normalize().multiply(0.7D).setY(0.45D));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9F, 0.8F);
        target.sendMessage(ChatColor.RED + "A falling block hit you!");
        if (target.getHealth() <= 6.0D) {
            eliminate(session, target, null, "was struck by a falling block");
        } else {
            target.setHealth(target.getHealth() - 6.0D);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRunnerFallingBlockLand(EntityChangeBlockEvent event) {
        Session session = active;
        if (session == null || event.getEntity().getWorld() != session.world) return;
        if (session.type == ArcadePortGame.DRAGONS
                && event.getEntity() instanceof EnderDragon dragon && session.entities.contains(dragon)) {
            event.setCancelled(true);
            return;
        }
        if (session.type != ArcadePortGame.RUNNER
                || !(event.getEntity() instanceof FallingBlock falling)
                || !session.entities.contains(falling)) return;
        event.setCancelled(true);
        session.entities.remove(falling);
        session.runnerFallingBlockExpiresAt.remove(falling.getUniqueId());
        falling.remove();
    }

    private void handleDragonHazardHit(Session session, Fireball fireball, Player target) {
        if (active != session || !session.alive.contains(target.getUniqueId())) return;
        Vector recoil = target.getLocation().toVector().subtract(fireball.getLocation().toVector());
        if (recoil.lengthSquared() < 0.001D) recoil = new Vector(0.0D, 1.0D, 0.0D);
        target.setVelocity(recoil.normalize().multiply(0.8D).setY(0.55D));
        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0.0D, 1.0D, 0.0D),
                16, 0.35D, 0.4D, 0.35D, 0.05D);
        target.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 1.4F);
        target.sendMessage(ChatColor.RED + "A dragon fireball knocked you back!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Session session = active;
        if (session == null || event.getEntity().getWorld() != session.world) return;
        if (session.type == ArcadePortGame.BOMB_LOBBERS
                && event.getEntity() instanceof Player bombTarget
                && session.players.containsKey(bombTarget.getUniqueId())
                && !(event instanceof EntityDamageByEntityEvent)) {
            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL
                    && "jumper".equals(session.bombLobbersKits.getOrDefault(bombTarget.getUniqueId(), "jumper"))) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            double damage = event.getFinalDamage();
            if (damage > 0.0D) {
                if (bombTarget.getHealth() <= damage) eliminate(session, bombTarget, null,
                        event.getCause().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
                else bombTarget.setHealth(bombTarget.getHealth() - damage);
            }
            return;
        }
        if (session.type == ArcadePortGame.BACON_BRAWL
                && event.getEntity() instanceof Player baconTarget
                && session.players.containsKey(baconTarget.getUniqueId())) {
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                Entity damager = byEntity.getDamager();
                if (damager instanceof Player attacker) {
                    event.setCancelled(true);
                    handlePlayerHit(session, attacker, baconTarget, "melee");
                    return;
                }
                if (damager instanceof Snowball bacon && session.entities.contains(bacon)
                        && bacon.getShooter() instanceof Player attacker) {
                    event.setCancelled(true);
                    handlePlayerHit(session, attacker, baconTarget, "bacon-blast");
                    return;
                }
            }
            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            double damage = event.getFinalDamage();
            if (damage > 0.0D) {
                if (baconTarget.getHealth() <= damage) eliminate(session, baconTarget, null,
                        event.getCause().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
                else baconTarget.setHealth(baconTarget.getHealth() - damage);
            }
            return;
        }
        if (session.type == ArcadePortGame.SHEEP_QUEST && event.getEntity() instanceof Sheep sheep
                && session.sheep.contains(sheep)) {
            event.setCancelled(true);
            return;
        }
        if (session.type == ArcadePortGame.DRAGONS && event.getEntity() instanceof EnderDragon dragon
                && session.entities.contains(dragon)) {
            event.setCancelled(true);
            if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player) {
                setDragonTargetSky(session, dragon);
                dragon.playEffect(EntityEffect.HURT);
            }
            return;
        }
        if (session.type == ArcadePortGame.DRAGONS && event.getEntity() instanceof Player dragonPlayer
                && session.players.containsKey(dragonPlayer.getUniqueId())
                && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            double rawDamage = event.getFinalDamage();
            event.setCancelled(true);
            double damage = ArcadePortRules.dragonFallDamageAfterReduction(rawDamage);
            if (damage > 0.0D) {
                if (dragonPlayer.getHealth() <= damage) eliminate(session, dragonPlayer, null, "fell");
                else dragonPlayer.setHealth(dragonPlayer.getHealth() - damage);
            }
            return;
        }
        if (event.getEntity() instanceof Player target && session.players.containsKey(target.getUniqueId())) {
            event.setCancelled(true);
            if (session.type == ArcadePortGame.RUNNER && event instanceof EntityDamageByEntityEvent runnerDamage) {
                Entity damager = runnerDamage.getDamager();
                if (damager instanceof Player || damager instanceof Projectile) return;
            }
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                Entity damager = byEntity.getDamager();
                if (session.type == ArcadePortGame.RUNNER && damager instanceof FallingBlock falling
                        && session.entities.contains(falling)) {
                    handleRunnerFallingBlockHit(session, falling, target);
                } else if (damager instanceof Player attacker) {
                    handlePlayerHit(session, attacker, target, "melee");
                } else if (session.type == ArcadePortGame.DEATH_TAG && damager instanceof Zombie zombie
                        && session.entities.contains(zombie)) {
                    eliminate(session, target, null, "was tagged by the chaser");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonsMarksmanShoot(EntityShootBowEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.DRAGONS
                || !(event.getEntity() instanceof Player player)
                || !"marksman".equals(session.dragonKits.getOrDefault(player.getUniqueId(), "coward"))
                || !(event.getProjectile() instanceof Arrow arrow)) return;
        UUID id = player.getUniqueId();
        session.entities.add(arrow);
        session.dragonTrackedArrows.add(arrow.getUniqueId());
        int charge = session.dragonMarksmanCharge.getOrDefault(id, 0);
        if (charge > 0) {
            session.dragonMarksmanFiring.add(id);
            session.dragonMarksmanNextChargeAt.put(id,
                    System.currentTimeMillis() + ArcadePortRules.DRAGONS_BARRAGE_INTERVAL_MILLIS);
        } else {
            session.dragonMarksmanCharge.remove(id);
            session.dragonMarksmanNextChargeAt.remove(id);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuiverBowShoot(EntityShootBowEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.ONE_IN_THE_QUIVER
                || !(event.getEntity() instanceof Player player)
                || !"enchanter".equals(session.quiverKits.getOrDefault(player.getUniqueId(), "leaper"))
                || !(event.getProjectile() instanceof Arrow arrow)) return;
        session.quiverArrowBounces.put(arrow.getUniqueId(), ArcadePortRules.QUIVER_MAX_ARROW_BOUNCES);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMicroBowShoot(EntityShootBowEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.MICRO_BATTLES
                || !(event.getEntity() instanceof Player player)
                || !"archer".equals(session.microKits.getOrDefault(player.getUniqueId(), "archer"))
                || !(event.getProjectile() instanceof Arrow arrow)) return;
        // Archer's loadout only supplies fletched arrows, so each shot gets the source perk's
        // remove-on-impact behaviour even when the last arrow has just left the inventory.
        session.microFletchedArrows.add(arrow.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.TURF_WARS || session.turfCombat
                || !(event.getEntity() instanceof Player player) || event.getEntity().getWorld() != session.world
                || !session.players.containsKey(player.getUniqueId())) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.YELLOW + "You cannot shoot during build time.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Session session = active;
        if (session == null || event.getEntity().getWorld() != session.world) return;

        if (session.type == ArcadePortGame.PAINTBALL
                && event.getEntity() instanceof ThrownPotion potion
                && session.paintballWaterBombs.remove(potion.getUniqueId())) {
            if (potion.getShooter() instanceof Player thrower) {
                paintballWaterBombImpact(session, thrower, potion.getLocation().clone());
            }
            session.entities.remove(potion);
            potion.remove();
            return;
        }

        if (session.type == ArcadePortGame.DRAGONS
                && event.getEntity() instanceof Arrow dragonArrow
                && session.dragonTrackedArrows.remove(dragonArrow.getUniqueId())) {
            if (event.getHitEntity() instanceof EnderDragon dragon) handleDragonProjectileHit(session, dragon);
            session.entities.remove(dragonArrow);
            dragonArrow.remove();
            return;
        }

        if (session.type == ArcadePortGame.MICRO_BATTLES
                && event.getEntity() instanceof Arrow microArrow
                && session.microFletchedArrows.remove(microArrow.getUniqueId())) {
            microArrow.remove();
            return;
        }

        if (session.type == ArcadePortGame.BACON_BRAWL
                && event.getEntity() instanceof Snowball bacon && session.entities.contains(bacon)) {
            Location impact = bacon.getLocation().clone();
            if (event.getHitEntity() instanceof Player target
                    && bacon.getShooter() instanceof Player attacker
                    && session.alive.contains(attacker.getUniqueId())
                    && session.alive.contains(target.getUniqueId())) {
                handlePlayerHit(session, attacker, target, "bacon-blast");
            }
            impact.getWorld().spawnParticle(Particle.CLOUD, impact, 14,
                    0.25D, 0.25D, 0.25D, 0.04D);
            impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.6F);
            session.entities.remove(bacon);
            bacon.remove();
            return;
        }

        if (session.type == ArcadePortGame.SPLEEF
                && (event.getEntity() instanceof Snowball || event.getEntity() instanceof Arrow)
                && event.getEntity() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter
                && session.alive.contains(shooter.getUniqueId())) {
            Block center = event.getHitBlock();
            if (center == null) {
                center = projectile.getLocation().add(projectile.getVelocity().clone().multiply(0.8D)).getBlock();
            }
            long now = System.currentTimeMillis();
            if (center.getWorld() == session.world && !isSpleefProtectedBlock(center.getType())) {
                restoreSpleefHunger(session, shooter, now);
                if (projectile instanceof Arrow arrow) {
                    double arrowSpeed = arrow.getVelocity().length();
                    double radius = Math.min(3.0D, 0.5D + arrowSpeed / 1.6D);
                    int limit = ArcadePortRules.spleefProjectileClearRadius(true, arrowSpeed);
                    fadeSpleefBlock(session, center, false);
                    for (int dx = -limit; dx <= limit; dx++) {
                        for (int dy = -limit; dy <= limit; dy++) {
                            for (int dz = -limit; dz <= limit; dz++) {
                                if ((dx == 0 && dy == 0 && dz == 0)
                                        || dx * dx + dy * dy + dz * dz > radius * radius) continue;
                                fadeSpleefBlock(session, center.getRelative(dx, dy, dz), true);
                            }
                        }
                    }
                } else {
                    fadeSpleefBlock(session, center, false);
                    giveSpleefSnowball(session, shooter);
                }
            }
            projectile.remove();
            return;
        }

        if (session.type == ArcadePortGame.RUNNER && event.getEntity() instanceof Snowball snowball
                && snowball.getShooter() instanceof Player shooter
                && session.alive.contains(shooter.getUniqueId())
                && session.players.containsKey(shooter.getUniqueId())
                && "frosty".equals(session.runnerKits.getOrDefault(shooter.getUniqueId(), "leaper"))) {
            if (event.getHitEntity() instanceof Player target && session.alive.contains(target.getUniqueId())
                    && session.players.containsKey(target.getUniqueId())) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        ArcadePortRules.RUNNER_FROSTBALL_SLOW_TICKS,
                        ArcadePortRules.RUNNER_FROSTBALL_SLOW_AMPLIFIER, false, false, true));
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7F, 0.8F);
                target.sendMessage(ChatColor.AQUA + "Frost Ball slowed you!");
            }
            snowball.remove();
            return;
        }

        if (session.type == ArcadePortGame.RUNNER && event.getEntity() instanceof Arrow arrow
                && arrow.getShooter() instanceof Player shooter && session.alive.contains(shooter.getUniqueId())) {
            Block hit = event.getHitBlock();
            if (hit != null) {
                // Reborn uses a 2.5-block 3D radius, so wall/floor blocks are eligible if exposed above.
                long now = System.currentTimeMillis();
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx * dx + dy * dy + dz * dz > 6.25D) continue;
                            markRunnerBlock(session, session.world.getBlockAt(
                                    hit.getX() + dx, hit.getY() + dy, hit.getZ() + dz), shooter, now);
                        }
                    }
                }
                arrow.remove();
            }
            session.entities.remove(arrow);
            arrow.remove();
            return;
        }

         if (session.type == ArcadePortGame.PAINTBALL && event.getHitEntity() == null) {
            session.paintProjectileDamage.remove(event.getEntity().getUniqueId());
        }

        // Handle Dragons fireballs here rather than relying on damage-event ordering.
        // World hazards knock players back; player-fired fireballs follow normal combat rules.
        if (session.type == ArcadePortGame.DRAGONS && event.getEntity() instanceof Fireball fireball
                && session.entities.contains(fireball)) {
            ProjectileSource source = fireball.getShooter();
            if (event.getHitEntity() instanceof Player target && session.alive.contains(target.getUniqueId())) {
                if (source instanceof Player shooter) {
                    hitProjectile(session, fireball, shooter, target);
                } else if (ArcadePortRules.isWorldDragonHazard(session.type,
                        session.entities.contains(fireball), source instanceof Player)) {
                    handleDragonHazardHit(session, fireball, target);
                }
            } else if (event.getHitEntity() instanceof EnderDragon dragon && source instanceof Player) {
                handleDragonProjectileHit(session, dragon);
            }
            fireball.remove();
            return;
        }

        if (event.getHitEntity() instanceof Player target && event.getEntity() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player shooter) hitProjectile(session, projectile, shooter, target);
        } else if (session.type == ArcadePortGame.TURF_WARS && session.turfCombat && event.getHitBlock() != null
                && event.getEntity() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            Block block = event.getHitBlock();
            if (block.getType() == Material.RED_WOOL || block.getType() == Material.BLUE_WOOL) {
                block.setType(Material.AIR, false);
                shooter.playSound(shooter.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.5F);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Session session = active;
        if (session == null) return;
        boolean bombLeftClick = session.type == ArcadePortGame.BOMB_LOBBERS && event.getAction().isLeftClick();
        boolean runnerLeftClick = session.type == ArcadePortGame.RUNNER && event.getAction().isLeftClick();
        if (!event.getAction().isRightClick() && !bombLeftClick && !runnerLeftClick) return;
        Player player = event.getPlayer();
        if (!session.alive.contains(player.getUniqueId())) return;
        ItemStack heldItem = event.getItem();
        Material item = heldItem == null ? Material.AIR : heldItem.getType();
        if (session.type == ArcadePortGame.RUNNER) {
            String kit = session.runnerKits.getOrDefault(player.getUniqueId(), "leaper");
            if ("leaper".equals(kit) && item == Material.IRON_AXE && event.getAction().isRightClick()) {
                event.setCancelled(true);
                useRunnerLeap(session, player, System.currentTimeMillis());
                return;
            }
            if ("archer".equals(kit) && item == Material.BOW && event.getAction().isLeftClick()) {
                event.setCancelled(true);
                useRunnerQuickshot(session, player, System.currentTimeMillis());
                return;
            }
        }
        if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
            String kit = session.dragonEscapeKits.getOrDefault(player.getUniqueId(), "jumper");
            if (event.getAction().isRightClick() && item == Material.IRON_AXE && !"digger".equals(kit)) {
                event.setCancelled(true);
                useDragonEscapeLeap(session, player);
                return;
            }
            if (event.getAction().isRightClick() && item == Material.ENDER_PEARL && "warper".equals(kit)) {
                event.setCancelled(true);
                useDragonEscapeWarper(session, player);
                return;
            }
            if (event.getAction().isRightClick() && item == Material.TNT && "disruptor".equals(kit)) {
                event.setCancelled(true);
                throwDragonEscapeMine(session, player);
                return;
            }
        }
        if (session.type == ArcadePortGame.DRAGONS) {
            UUID id = player.getUniqueId();
            String kit = session.dragonKits.getOrDefault(id, "coward");
            if ("coward".equals(kit) && item == Material.IRON_AXE) {
                event.setCancelled(true);
                useDragonsLeap(session, player, System.currentTimeMillis());
                return;
            }
            if ("pyrotechnic".equals(kit) && item == Material.EMERALD && heldItem != null
                    && heldItem.hasItemMeta() && heldItem.getItemMeta().hasDisplayName()
                    && "Throwing Sparkler".equals(ChatColor.stripColor(heldItem.getItemMeta().getDisplayName()))) {
                event.setCancelled(true);
                consumeHeld(player);
                Vector direction = player.getLocation().getDirection().normalize();
                Item sparkler = player.getWorld().dropItem(player.getEyeLocation().add(direction.clone()),
                        namedItem(Material.EMERALD, "Throwing Sparkler", "A dragon-attracting sparkler."));
                sparkler.setPickupDelay(2_000);
                sparkler.setVelocity(direction.multiply(0.8D).setY(direction.getY() * 0.8D + 0.1D));
                session.entities.add(sparkler);
                session.dragonSparklerItems.add(sparkler.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 0.8F, 1.2F);
                return;
            }
            if ("marksman".equals(kit) && item == Material.BOW) {
                session.dragonMarksmanCharge.putIfAbsent(id, 0);
                session.dragonMarksmanNextChargeAt.putIfAbsent(id,
                        System.currentTimeMillis() + ArcadePortRules.DRAGONS_BARRAGE_INITIAL_CHARGE_MILLIS);
                return;
            }
        }
        if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER
                && "slam-shot".equals(session.quiverKits.getOrDefault(player.getUniqueId(), "leaper"))
                && item == Material.DIAMOND_SHOVEL && event.getAction().isRightClick()) {
            event.setCancelled(true);
            useQuiverGroundPound(session, player, System.currentTimeMillis());
            return;
        }
        if (session.type == ArcadePortGame.SPLEEF
                && "brawler".equals(session.spleefKits.getOrDefault(player.getUniqueId(), "snowballer"))
                && item == Material.IRON_AXE && event.getAction().isRightClick()) {
            event.setCancelled(true);
            useSpleefLeap(session, player, System.currentTimeMillis());
            return;
        }
        if (session.type == ArcadePortGame.BACON_BRAWL && item == Material.IRON_AXE
                && event.getAction().isRightClick()) {
            event.setCancelled(true);
            useBaconBrawlAbility(session, player, System.currentTimeMillis());
            return;
        }
        if (session.type == ArcadePortGame.BOMB_LOBBERS
                && handleBombLobbersKitInteract(session, player, event, heldItem)) return;
        if (session.type == ArcadePortGame.EVOLUTION
                && (session.evolving.contains(player.getUniqueId())
                || session.evolutionCharge.containsKey(player.getUniqueId()))) {
            event.setCancelled(true);
            return;
        }
        if (session.type == ArcadePortGame.PAINTBALL) {
            if (isPaintballWaterBomb(heldItem)) {
                event.setCancelled(true);
                UUID id = player.getUniqueId();
                long now = System.currentTimeMillis();
                long last = session.paintballWaterBombLastThrowAt.getOrDefault(id, 0L);
                if (now - last < ArcadePortRules.PAINTBALL_WATER_BOMB_COOLDOWN_MILLIS) return;
                session.paintballWaterBombLastThrowAt.put(id, now);
                consumeHeld(player);
                ThrownPotion potion = player.launchProjectile(ThrownPotion.class);
                potion.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(0.8D));
                session.entities.add(potion);
                session.paintballWaterBombs.add(potion.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 0.8F, 1.0F);
                player.sendMessage(ChatColor.AQUA + "Water Bomb thrown!");
                return;
            }
            String kit = paintballKitForItem(heldItem);
            if (kit != null) {
                event.setCancelled(true);
                firePaintballWeapon(session, player, kit, System.currentTimeMillis());
                return;
            }
        }
        if ((session.type == ArcadePortGame.BOMB_LOBBERS || (session.type == ArcadePortGame.EVOLUTION
                && session.evolutionLevels.getOrDefault(player.getUniqueId(), 0) == 3)) && item == Material.TNT) {
            event.setCancelled(true);
            consumeHeld(player);
            Location spawn = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.0D));
            TNTPrimed bomb = session.world.spawn(spawn, TNTPrimed.class);
            bomb.setFuseTicks(ArcadePortRules.BOMB_LOBBERS_FUSE_TICKS);
            double pitchVelocity = "pitcher".equals(session.bombLobbersKits.get(player.getUniqueId()))
                    ? ArcadePortRules.bombLobbersPitchVelocity(session.bombPitchLevels.getOrDefault(player.getUniqueId(), 2))
                    : 2.0D;
            Vector velocity = player.getLocation().getDirection().normalize().multiply(pitchVelocity);
            velocity.setY(velocity.getY() + 0.1D);
            bomb.setVelocity(velocity);
            session.entities.add(bomb);
            session.bombOwners.put(bomb.getUniqueId(), player.getUniqueId());
            session.bombLastLocations.put(bomb.getUniqueId(), spawn.clone());
            session.bombCreatedAt.put(bomb.getUniqueId(), System.currentTimeMillis());
            session.bombPrimed.remove(bomb.getUniqueId());
            player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.9F, 1.0F);
        } else if (session.type == ArcadePortGame.EVOLUTION
                && session.evolutionLevels.getOrDefault(player.getUniqueId(), 0) == 1 && item == Material.FIRE_CHARGE) {
            event.setCancelled(true);
            consumeHeld(player);
            Location spawn = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.0D));
            Fireball fireball = session.world.spawn(spawn, Fireball.class);
            fireball.setShooter(player);
            fireball.setYield(0.0F);
            fireball.setIsIncendiary(false);
            fireball.setVelocity(player.getLocation().getDirection().multiply(0.75D));
            session.entities.add(fireball);
        } else if (session.type == ArcadePortGame.EVOLUTION
                && session.evolutionLevels.getOrDefault(player.getUniqueId(), 0) == 2 && item == Material.COBWEB) {
            event.setCancelled(true);
            Location placement = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(1.5D));
            placement.setY(64.0D);
            Block web = placement.getBlock();
            if (web.getType().isAir()) {
                web.setType(Material.COBWEB, false);
                session.evolutionWebs.put(web, System.currentTimeMillis() + 3_000L);
                consumeHeld(player);
                player.sendMessage(ChatColor.WHITE + "Spider web placed!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "There is no space to place a web.");
            }
        } else if (session.type == ArcadePortGame.EVOLUTION
                && session.evolutionLevels.getOrDefault(player.getUniqueId(), 0) == 4 && item == Material.SLIME_BALL) {
            event.setCancelled(true);
            consumeHeld(player);
            player.setVelocity(player.getLocation().getDirection().multiply(1.15D).setY(0.85D));
            player.getWorld().spawnParticle(Particle.ITEM_SLIME, player.getLocation(), 12, 0.35D, 0.25D, 0.35D, 0.08D);
            player.playSound(player.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0F, 1.25F);
        } else if (session.type == ArcadePortGame.EVOLUTION
                && session.evolutionLevels.getOrDefault(player.getUniqueId(), 0) == 5 && item == Material.FEATHER) {
            event.setCancelled(true);
            if (inventoryCount(player, Material.EGG) <= 0) {
                player.sendMessage(ChatColor.YELLOW + "You need an egg to use Egg Launch.");
                return;
            }
            consumeOne(player, Material.EGG);
            Egg egg = player.launchProjectile(Egg.class);
            egg.setVelocity(player.getEyeLocation().getDirection().multiply(1.35D));
            session.entities.add(egg);
            player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 0.9F, 1.0F);
        } else if (session.type == ArcadePortGame.RUNNER && item == Material.FEATHER) {
            event.setCancelled(true);
            consumeHeld(player);
            player.setVelocity(player.getLocation().getDirection().multiply(1.0D).setY(0.45D));
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.3D, 0.3D, 0.3D, 0.05D);
        }
    }

    private void consumeOne(Player player, Material material) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() != material || item.getAmount() <= 0) continue;
            if (item.getAmount() == 1) player.getInventory().setItem(slot, null);
            else item.setAmount(item.getAmount() - 1);
            return;
        }
    }

    private void consumeHeld(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) return;
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPaintballPearlTeleport(PlayerTeleportEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.PAINTBALL
                || event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || !session.players.containsKey(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractSheep(PlayerInteractEntityEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.SHEEP_QUEST
                || !(event.getRightClicked() instanceof Sheep sheep) || !session.sheep.contains(sheep)
                || !session.alive.contains(event.getPlayer().getUniqueId())) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.SADDLE) return;
        event.setCancelled(true);
        UUID playerId = player.getUniqueId();
        int team = session.teams.getOrDefault(playerId, -1);
        int owner = session.sheepOwners.getOrDefault(sheep, -1);
        if (team < 0 || team > 3) return;
        if (owner == team && isInsideSheepPen(session, sheep.getLocation(), team)) {
            player.sendMessage(ChatColor.YELLOW + "That sheep is already safe in your pen.");
            return;
        }
        int carriedCount = sheepCarriedBy(session, playerId);
        if (!ArcadePortRules.canCarryAnotherSheep(carriedCount)) {
            player.sendMessage(ChatColor.YELLOW + "You can carry up to three sheep at once.");
            return;
        }
        session.sheepCarriers.put(sheep, playerId);
        sheep.setAI(false);
        sheep.setColor(sheepTeamDyeColor(team));
        player.playSound(player.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 1.0F, 2.0F);
        player.sendMessage(ChatColor.GREEN + "Sheep picked up. Carry it to your team's pen!");
    }

    private boolean isQuiverBreakableGlass(Material material) {
        String name = material.name();
        return material == Material.GLASS || material == Material.GLASS_PANE
                || name.endsWith("_STAINED_GLASS") || name.endsWith("_STAINED_GLASS_PANE");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Session session = active;
        if (session == null || event.getBlock().getWorld() != session.world) return;
        if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER
                && session.alive.contains(event.getPlayer().getUniqueId())
                && isQuiverBreakableGlass(event.getBlock().getType())) {
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }
        if (session.type == ArcadePortGame.SPLEEF && !isSpleefProtectedBlock(event.getBlock().getType())
                && session.alive.contains(event.getPlayer().getUniqueId())) {
            event.setDropItems(false);
            event.setExpToDrop(0);
            event.setCancelled(true);
            handleSpleefBlockHit(session, event.getPlayer(), event.getBlock(), System.currentTimeMillis());
            return;
        }
        if (session.type == ArcadePortGame.MICRO_BATTLES
                && session.alive.contains(event.getPlayer().getUniqueId())) {
            if (session.microBarrierBlocks.contains(event.getBlock())) {
                event.setCancelled(true);
                return;
            }
            session.microCrumbleBlocks.remove(event.getBlock());
            return;
        }
        if (session.type == ArcadePortGame.TURF_WARS && session.alive.contains(event.getPlayer().getUniqueId())) {
            int team = session.teams.getOrDefault(event.getPlayer().getUniqueId(), -1);
            Material ownWool = team == 0 ? Material.RED_WOOL : Material.BLUE_WOOL;
            if ((team == 0 || team == 1) && event.getBlock().getType() == ownWool) {
                event.setDropItems(false);
                event.setExpToDrop(0);
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpleefBlockDamage(BlockDamageEvent event) {
        Session session = active;
        if (session == null || event.getBlock().getWorld() != session.world
                || !session.alive.contains(event.getPlayer().getUniqueId())) return;
        if (session.type == ArcadePortGame.DRAGON_ESCAPE
                && "digger".equals(session.dragonEscapeKits.getOrDefault(event.getPlayer().getUniqueId(), "jumper"))
                && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.DIAMOND_PICKAXE) {
            event.setCancelled(true);
            useDragonEscapeDigger(session, event.getPlayer(), event.getBlock());
            return;
        }
        if (session.type != ArcadePortGame.SPLEEF || isSpleefProtectedBlock(event.getBlock().getType())) return;
        event.setCancelled(true);
        handleSpleefBlockHit(session, event.getPlayer(), event.getBlock(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Session session = active;
        if (session == null || event.getBlock().getWorld() != session.world) return;
        if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
            Player player = event.getPlayer();
            Block placed = event.getBlockPlaced();
            boolean allowed = session.alive.contains(player.getUniqueId())
                    && "digger".equals(session.dragonEscapeKits.getOrDefault(player.getUniqueId(), "jumper"))
                    && isDragonEscapeDiggerBlockAllowed(placed.getType())
                    && isDragonEscapeDiggerBuildLocationAllowed(session, placed);
            if (!allowed) event.setCancelled(true);
            return;
        }
        if (session.type == ArcadePortGame.MICRO_BATTLES) {
            Player player = event.getPlayer();
            Block placed = event.getBlockPlaced();
            boolean insideSourceBounds = placed.getX() >= session.microCrumbleMinX
                    && placed.getX() < session.microCrumbleMaxX
                    && placed.getZ() >= session.microCrumbleMinZ
                    && placed.getZ() < session.microCrumbleMaxZ;
            boolean allowed = session.alive.contains(player.getUniqueId()) && insideSourceBounds
                    && placed.getY() > Math.max(0, session.microCrumbleMinY)
                    && placed.getY() < session.world.getMaxHeight();
            if (!allowed) event.setCancelled(true);
            else session.microCrumbleBlocks.add(placed);
            return;
        }
        if (session.type == ArcadePortGame.TURF_WARS) {
            Player player = event.getPlayer();
            int team = session.teams.getOrDefault(player.getUniqueId(), -1);
            Material ownWool = team == 0 ? Material.RED_WOOL : Material.BLUE_WOOL;
            Material ownTurf = team == 0 ? Material.RED_TERRACOTTA : Material.BLUE_TERRACOTTA;
            if (session.turfCombat || !session.alive.contains(player.getUniqueId())
                    || (team != 0 && team != 1) || event.getBlockPlaced().getType() != ownWool) {
                event.setCancelled(true);
                return;
            }
            Block placed = event.getBlockPlaced();
            if (!ArcadePortRules.isTurfWarsBuildWithinBounds(placed.getX(), placed.getZ(),
                    session.turfFrontlineMinX, session.turfFrontlineMaxXExclusive,
                    session.turfMinZ, session.turfMaxZExclusive)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.YELLOW + "You must build inside the original arena.");
                return;
            }
            int x = placed.getX();
            if (!ArcadePortRules.canTurfWarsBuildOnTerritory(team, x, session.turfFrontlineX)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.YELLOW + "You can only build on your team's turf.");
                return;
            }
            boolean aboveOwnTurf = false;
            for (int i = 1; i <= 5; i++) {
                Material below = event.getBlockPlaced().getRelative(0, -i, 0).getType();
                if (below == ownTurf || below == ownWool) {
                    aboveOwnTurf = true;
                    break;
                }
            }
            if (!aboveOwnTurf) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.YELLOW + "Build no more than five blocks above your team's turf.");
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Session session = active;
        if (session != null && session.players.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Session session = active;
        if (session != null && event.getWhoClicked() instanceof Player player && session.players.containsKey(player.getUniqueId())) event.setCancelled(true);
    }

    private void updateBombLobbers(Session session) {
        long now = System.currentTimeMillis();
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (!player.isFlying() && player.isOnGround()) session.bombDoubleJumpAvailable.add(id);
            long elapsed = Math.max(0L, now - session.startedAt);
            long nextGrant = session.bombNextCraftAt.computeIfAbsent(id,
                    ignored -> now + ArcadePortRules.bombLobbersCraftmanIntervalMillis(elapsed));
            if (now >= nextGrant) {
                if (inventoryCount(player, Material.TNT) < ArcadePortRules.BOMB_LOBBERS_MAX_CARRIED) {
                    player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT",
                            "Left-click to throw at enemy players.", 1));
                }
                session.bombNextCraftAt.put(id,
                        now + ArcadePortRules.bombLobbersCraftmanIntervalMillis(elapsed));
            }
        }

        for (Entity entity : new ArrayList<>(session.entities)) {
            if (!(entity instanceof TNTPrimed bomb)) continue;
            UUID bombId = bomb.getUniqueId();
            if (!bomb.isValid()) {
                removeBombTracking(session, bombId);
                continue;
            }
            UUID ownerId = session.bombOwners.get(bombId);
            if (ownerId == null) continue;
            if (session.bombPrimed.contains(bombId)) continue;
            Player owner = session.players.get(ownerId);
            if (owner == null || !owner.isOnline() || !session.alive.contains(ownerId)) {
                removeBombTracking(session, bombId);
                session.entities.remove(bomb);
                bomb.remove();
                continue;
            }

            Location current = bomb.getLocation().clone();
            Location previous = session.bombLastLocations.put(bombId, current);
            if (previous == null) previous = current;

            // Direct-hit detection must run before wall/fuse handling so a TNT that
            // hits a player on the same tick still receives its source-style hit ding.
            boolean directHit = false;
            for (Player target : alivePlayers(session)) {
                UUID targetId = target.getUniqueId();
                if (targetId.equals(ownerId)) continue;
                if (!ArcadePortRules.areBombLobbersEnemies(
                        session.teams.getOrDefault(ownerId, -1),
                        session.teams.getOrDefault(targetId, -1))) continue;
                double distanceSquared = distanceSquaredToSegment(target.getLocation(), previous, current);
                if (!ArcadePortRules.isBombDirectHit(distanceSquared)) continue;

                removeBombTracking(session, bombId);
                session.entities.remove(bomb);
                bomb.remove();
                Vector away = target.getLocation().toVector().subtract(current.toVector());
                away.setY(0.0D);
                if (away.lengthSquared() < 0.001D) away = new Vector(0.0D, 0.0D, 1.0D);
                // Reborn's direct-hit impulse is deliberately light (.2 horizontal, .2 vertical);
                // reserve the large blast impulse for the actual explosion.
                target.setVelocity(away.normalize().multiply(0.2D).setY(0.2D));
                target.getWorld().spawnParticle(Particle.EXPLOSION,
                        target.getLocation().add(0.0D, 1.0D, 0.0D), 1,
                        0.1D, 0.1D, 0.1D, 0.0D);
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 0.9F);
                // Reborn plays the classic note-pling at the struck player on a direct hit.
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.2F, 1.65F);
                owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.45F);
                owner.sendMessage(ChatColor.YELLOW + "Direct hit!");
                double directHitDamage = bombLobbersDamageAfterArmor(target, 10.0D);
                if (target.getHealth() <= directHitDamage) {
                    eliminate(session, target, owner, "was directly hit by TNT");
                } else {
                    target.setHealth(target.getHealth() - directHitDamage);
                }
                checkRoundEnd(session);
                directHit = true;
                break;
            }
            if (directHit || active != session) {
                if (active != session) return;
                continue;
            }

            long createdAt = session.bombCreatedAt.getOrDefault(bombId, now);
            long age = Math.max(0L, now - createdAt);
            boolean touchingSolid = isBombTouchingSolidBlock(bomb);
            if (ArcadePortRules.shouldPrimeBombLobbersBomb(age, touchingSolid)) {
                session.bombPrimed.add(bombId);
                bomb.setFuseTicks(0);
                continue;
            }

            // The original TNT is re-thrown with its existing velocity as its short
            // fuse reaches its last second. It only actually detonates on contact
            // after 3s or unconditionally after 8s.
            if (bomb.getFuseTicks() <= 20) {
                Vector velocity = bomb.getVelocity().clone();
                TNTPrimed replacement = session.world.spawn(current, TNTPrimed.class);
                replacement.setFuseTicks(ArcadePortRules.BOMB_LOBBERS_FUSE_TICKS);
                replacement.setVelocity(velocity);
                removeBombTracking(session, bombId);
                session.entities.remove(bomb);
                bomb.remove();
                UUID replacementId = replacement.getUniqueId();
                session.entities.add(replacement);
                session.bombOwners.put(replacementId, ownerId);
                session.bombLastLocations.put(replacementId, current.clone());
                session.bombCreatedAt.put(replacementId, createdAt);
            }
        }
    }

    private void removeBombTracking(Session session, UUID bombId) {
        session.bombOwners.remove(bombId);
        session.bombLastLocations.remove(bombId);
        session.bombCreatedAt.remove(bombId);
        session.bombPrimed.remove(bombId);
    }

    private boolean isBombTouchingSolidBlock(TNTPrimed bomb) {
        Block center = bomb.getLocation().getBlock();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block nearby = center.getRelative(dx, dy, dz);
                    if (!nearby.isLiquid() && nearby.getType().isSolid()) return true;
                }
            }
        }
        return false;
    }

    private boolean handleBombLobbersKitInteract(Session session, Player player,
            PlayerInteractEvent event, ItemStack heldItem) {
        if (heldItem == null) return false;
        UUID id = player.getUniqueId();
        String kit = session.bombLobbersKits.getOrDefault(id, "jumper");
        if ("pitcher".equals(kit) && heldItem.getType() == Material.LEVER) {
            event.setCancelled(true);
            int current = session.bombPitchLevels.getOrDefault(id, 2);
            if (event.getAction().isRightClick()) current = Math.min(3, current + 1);
            else if (event.getAction().isLeftClick()) current = Math.max(1, current - 1);
            session.bombPitchLevels.put(id, current);
            ItemStack lever = namedItem(Material.LEVER, "Velocity Selector",
                    "Throw power " + current + "/3. Right-click +; left-click -.", current);
            player.getInventory().setItemInMainHand(lever);
            player.sendMessage(ChatColor.YELLOW + "TNT throw power: " + current + "/3.");
            return true;
        }
        if ("waller".equals(kit) && heldItem.getType() == Material.STONE_SHOVEL
                && event.getAction().isRightClick() && event.getClickedBlock() != null) {
            event.setCancelled(true);
            Block clicked = event.getClickedBlock();
            if (clicked.isLiquid()) {
                player.sendMessage(ChatColor.YELLOW + "You may not place a wall in liquid.");
                return true;
            }
            Vector facing = player.getLocation().getDirection().normalize();
            int forwardX = (int) Math.round(facing.getX());
            int forwardZ = (int) Math.round(facing.getZ());
            if (forwardX == 0 && forwardZ == 0) {
                double yaw = Math.toRadians(player.getLocation().getYaw());
                forwardX = (int) Math.round(-Math.sin(yaw));
                forwardZ = (int) Math.round(Math.cos(yaw));
            }
            int centerX = clicked.getX() + forwardX;
            int centerY = clicked.getY() + 1;
            int centerZ = clicked.getZ() + forwardZ;
            int sideX = forwardZ;
            int sideZ = -forwardX;
            List<Block> wallBlocks = new ArrayList<>(15);
            SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
            boolean validFootprint = true;
            for (int width = -2; width <= 2 && validFootprint; width++) {
                for (int height = 0; height < 3; height++) {
                    int x = centerX + sideX * width;
                    int y = centerY + height;
                    int z = centerZ + sideZ * width;
                    if (!insideSourceMapBounds(session, x, z) || y < 1
                            || y >= session.world.getMaxHeight() - 1
                            || (mapData != null && mapData.hasVerticalBounds()
                                && (y < mapData.minY() || y >= mapData.maxY()))) {
                        validFootprint = false;
                        break;
                    }
                    Block wall = session.world.getBlockAt(x, y, z);
                    if (!wall.getType().isAir()) {
                        validFootprint = false;
                        break;
                    }
                    wallBlocks.add(wall);
                }
            }
            if (!validFootprint || wallBlocks.size() != 15) {
                player.sendMessage(ChatColor.YELLOW + "There is not enough clear space to build that wall.");
                return true;
            }
            for (Block wall : wallBlocks) wall.setType(Material.STONE_BRICKS, false);
            consumeHeld(player);
            player.getWorld().playSound(clicked.getLocation(), Sound.BLOCK_STONE_PLACE, 0.8F, 0.9F);
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onQuiverDoubleJump(org.bukkit.event.player.PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        Session session = active;
        if (session == null || session.type != ArcadePortGame.ONE_IN_THE_QUIVER
                || !session.alive.contains(player.getUniqueId())
                || !"leaper".equals(session.quiverKits.getOrDefault(player.getUniqueId(), "leaper"))) return;
        event.setCancelled(true);
        session.quiverLeapAwaitingLanding.add(player.getUniqueId());
        player.setFlying(false);
        player.setAllowFlight(false);
        Vector direction = player.getLocation().getDirection().normalize().multiply(0.9D);
        direction.setY(Math.max(0.55D, Math.min(0.9D, direction.getY() + 0.35D)));
        player.setVelocity(direction);
        player.setFallDistance(0.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8F, 1.2F);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBombDoubleJump(org.bukkit.event.player.PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS
                || !session.alive.contains(player.getUniqueId())) return;
        event.setCancelled(true);
        player.setFlying(false);
        UUID id = player.getUniqueId();
        if (player.isOnGround()) {
            Vector velocity = player.getVelocity();
            player.setVelocity(new Vector(velocity.getX(), 0.42D, velocity.getZ()));
            session.bombDoubleJumpAvailable.add(id);
            return;
        }
        if (!session.bombDoubleJumpAvailable.remove(id)) return;
        String kit = session.bombLobbersKits.getOrDefault(id, "jumper");
        double power = "jumper".equals(kit) || "waller".equals(kit) ? 1.2D : 0.9D;
        Vector velocity = player.getLocation().getDirection().normalize().multiply(power);
        velocity.setY(power);
        player.setVelocity(velocity);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7F, 1.3F);
    }

    private double distanceSquaredToSegment(Location point, Location start, Location end) {
        Vector p = point.toVector();
        Vector a = start.toVector();
        Vector b = end.toVector();
        Vector segment = b.clone().subtract(a);
        double lengthSquared = segment.lengthSquared();
        if (lengthSquared < 1.0E-9D) return p.distanceSquared(a);
        double amount = Math.max(0.0D, Math.min(1.0D, p.clone().subtract(a).dot(segment) / lengthSquared));
        Vector nearest = a.add(segment.multiply(amount));
        return p.distanceSquared(nearest);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Session session = active;
        if (session == null || event.getEntity().getWorld() != session.world) return;
        if (session.type == ArcadePortGame.DRAGON_ESCAPE && event.getEntity() instanceof TNTPrimed mine
                && session.dragonEscapeMineOwners.containsKey(mine.getUniqueId())) {
            detonateDragonEscapeMine(session, event, mine);
            return;
        }
        if (event.getEntity() instanceof TNTPrimed bomb) {
            UUID ownerId = session.bombOwners.remove(bomb.getUniqueId());
            session.bombCreatedAt.remove(bomb.getUniqueId());
            session.bombPrimed.remove(bomb.getUniqueId());
            if (ownerId != null && (session.type == ArcadePortGame.BOMB_LOBBERS
                    || (session.type == ArcadePortGame.EVOLUTION
                    && session.evolutionLevels.getOrDefault(ownerId, 0) == 3))) {
                Location blast = bomb.getLocation().clone();
                Player owner = session.players.get(ownerId);
                List<Block> blocksHit = new ArrayList<>(event.blockList());
                event.blockList().clear();
                event.setCancelled(true);
                if (session.type == ArcadePortGame.BOMB_LOBBERS) {
                    // Keep custom player damage below, but let the explosion carve the map.
                    // Archived source maps are reloaded before the next round.
                    for (Block block : blocksHit) {
                        if (block == null || block.getWorld() != session.world || block.getType().isAir()) continue;
                        block.setType(Material.AIR, false);
                    }
                }
                session.entities.remove(bomb);
                session.bombLastLocations.remove(bomb.getUniqueId());
                bomb.remove();
                session.world.spawnParticle(Particle.EXPLOSION, blast, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                session.world.playSound(blast, Sound.ENTITY_GENERIC_EXPLODE, 1.4F, 0.9F);
                for (Player target : alivePlayers(session)) {
                    if (session.type == ArcadePortGame.BOMB_LOBBERS
                            && !ArcadePortRules.areBombLobbersEnemies(
                                    session.teams.getOrDefault(ownerId, -1),
                                    session.teams.getOrDefault(target.getUniqueId(), -1))) continue;
                    double distanceSquared = target.getLocation().distanceSquared(blast);
                    if (distanceSquared > 64.0D) continue;
                    double distance = Math.sqrt(distanceSquared);
                    Vector away = target.getLocation().toVector().subtract(blast.toVector());
                    if (away.lengthSquared() < 0.001D) away = new Vector(0.0D, 1.0D, 0.0D);
                    double force = 1.9D * (1.0D - distance / 8.0D);
                    target.setVelocity(away.normalize().multiply(force).setY(Math.max(0.25D, 0.65D - distance * 0.045D)));
                    target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0.0D, 1.0D, 0.0D),
                            12, 0.35D, 0.45D, 0.35D, 0.05D);
                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.2F);
                    double blastDamage = Math.max(0.0D, 10.0D * (1.0D - distance / 8.0D));
                    if (session.type == ArcadePortGame.BOMB_LOBBERS) {
                        blastDamage = bombLobbersDamageAfterArmor(target, blastDamage);
                    }
                    if (blastDamage > 0.0D) {
                        if (target.getHealth() <= blastDamage) {
                            eliminate(session, target, owner, "was caught in a TNT blast");
                            if (active != session) return;
                        } else {
                            target.setHealth(target.getHealth() - blastDamage);
                        }
                    }
                }
                return;
            }
        }
        event.blockList().clear();
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        for (Map<UUID, Player> queue : queues.values()) queue.remove(id);
        Session session = active;
        if (session == null || !session.players.containsKey(id)) return;
        Player player = event.getPlayer();
        session.alive.remove(id);
        PlayerSnapshot snapshot = session.snapshots.get(id);
        if (snapshot != null) pendingRestore.put(id, snapshot);
        if (session.type == ArcadePortGame.DEATH_TAG) session.tagged.remove(id);
        if (session.type == ArcadePortGame.GLADIATORS) {
            Integer pair = session.gladiatorPairs.get(id);
            if (pair != null) session.gladiatorResolvedPairs.add(pair);
        }
        session.respawning.remove(id);
        session.players.remove(id);
        session.snapshots.remove(id);
        checkRoundEnd(session);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerSnapshot snapshot = pendingRestore.remove(event.getPlayer().getUniqueId());
        if (snapshot != null) Bukkit.getScheduler().runTask(plugin, () -> { if (event.getPlayer().isOnline()) snapshot.restore(event.getPlayer()); });
    }

    private static final class Session {
        final ArcadePortGame type;
        final World world;
        final Map<UUID, Player> players = new LinkedHashMap<>();
        final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
        final Set<UUID> alive = new HashSet<>();
        final Map<UUID, Integer> scores = new HashMap<>();
        final Map<UUID, Integer> paintPiecesRemaining = new HashMap<>();
        final Map<UUID, String> paintballKits = new HashMap<>();
        final Map<UUID, Long> paintballLastShotAt = new HashMap<>();
        final Map<UUID, Long> paintballWaterBombLastThrowAt = new HashMap<>();
        final Set<UUID> paintballWaterBombs = new HashSet<>();
        final Map<UUID, ArmorStand> paintballSaveMeMarkers = new HashMap<>();
        final Map<UUID, Location> paintballSaveMeLocations = new HashMap<>();
        final Map<UUID, Integer> paintProjectileDamage = new HashMap<>();
        final Map<UUID, Integer> evolutionLevels = new HashMap<>();
        final Map<UUID, Integer> evolutionLifeKills = new HashMap<>();
        final Set<UUID> evolutionAvailable = new HashSet<>();
        final Map<UUID, Float> evolutionCharge = new HashMap<>();
        final Set<UUID> evolving = new HashSet<>();
        final Map<UUID, Location> evolutionLockedLocations = new HashMap<>();
        final Map<UUID, Long> evolutionWaterMessageAt = new HashMap<>();
        final Map<Block, Long> evolutionWebs = new HashMap<>();
        final Map<UUID, Integer> gladiatorPairs = new HashMap<>();
        final Set<Integer> gladiatorResolvedPairs = new HashSet<>();
        int gladiatorRound = -1;
        int gladiatorPairCount;
        long gladiatorRoundStartedAt;
        long gladiatorFightStartsAt;
        long lastGladiatorPoisonAt;
        long nextGladiatorRoundAt;
        final Map<UUID, Integer> teams = new HashMap<>();
        final Set<UUID> tagged = new HashSet<>();
        final Set<UUID> alphaChasers = new HashSet<>();
        final Map<UUID, String> deathTagRunnerKits = new HashMap<>();
        final Map<UUID, Long> deathTagLastArrowAt = new HashMap<>();
        final Map<Block, BlockData> deathTagOriginalLightBlocks = new LinkedHashMap<>();
        long deathTagNextLightAt;
        final Map<UUID, String> baconKits = new HashMap<>();
        final Map<UUID, Long> baconAbilityReadyAt = new HashMap<>();
        final Map<UUID, Long> baconSlams = new HashMap<>();
        final Map<UUID, Long> baconCloakedUntil = new HashMap<>();
        final Set<UUID> respawning = new HashSet<>();
        final Set<Sheep> sheep = new HashSet<>();
        final Map<Sheep, Integer> sheepOwners = new HashMap<>();
        final Map<Sheep, UUID> sheepCarriers = new LinkedHashMap<>();
        final Set<Entity> entities = new HashSet<>();
        final Map<UUID, UUID> bombOwners = new HashMap<>();
        final Map<UUID, Location> bombLastLocations = new HashMap<>();
        final Map<UUID, Long> bombCreatedAt = new HashMap<>();
        final Set<UUID> bombPrimed = new HashSet<>();
        final Map<UUID, String> bombLobbersKits = new HashMap<>();
        final Map<UUID, Integer> bombPitchLevels = new HashMap<>();
        final Set<UUID> bombDoubleJumpAvailable = new HashSet<>();
        final Map<UUID, Long> bombNextCraftAt = new HashMap<>();
        final Map<UUID, String> spleefKits = new HashMap<>();
        final Map<UUID, Long> spleefLeapReadyAt = new HashMap<>();
        final Map<UUID, Long> spleefSmasherReadyAt = new HashMap<>();
        final Map<UUID, Long> spleefLastHungerGainAt = new HashMap<>();
        final Map<UUID, Long> spleefNextArrowAt = new HashMap<>();
        final Map<UUID, String> microKits = new HashMap<>();
        final Map<UUID, Long> microNextArrowAt = new HashMap<>();
        final Set<UUID> microFletchedArrows = new HashSet<>();
        final Set<Block> microCrumbleBlocks = new HashSet<>();
        int microCrumbleMinX = -20, microCrumbleMaxX = 20, microCrumbleMinZ = -20, microCrumbleMaxZ = 20;
        int microCrumbleMinY, microCrumbleMaxY = 96, microCrumbleScanY;
        boolean microCrumbleScanComplete;
        final Map<UUID, String> quiverKits = new HashMap<>();
        final Map<UUID, Integer> quiverArrowBounces = new HashMap<>();
        final Set<UUID> quiverLeapAwaitingLanding = new HashSet<>();
        final Map<UUID, Long> quiverCloakedUntil = new HashMap<>();
        final Map<UUID, Long> quiverGroundPoundReadyAt = new HashMap<>();
        long lastSpleefHungerAt;
        final Set<Block> microBarrierBlocks = new HashSet<>();
        final Map<String, Long> dragonContactCooldowns = new HashMap<>();
        final Map<UUID, String> dragonKits = new HashMap<>();
        final Map<UUID, Long> dragonNextSparklerAt = new HashMap<>();
        final Map<UUID, Long> dragonNextArrowAt = new HashMap<>();
        final Map<UUID, Integer> dragonMarksmanCharge = new HashMap<>();
        final Map<UUID, Long> dragonMarksmanNextChargeAt = new HashMap<>();
        final Set<UUID> dragonMarksmanFiring = new HashSet<>();
        final Set<UUID> dragonTrackedArrows = new HashSet<>();
        final Set<UUID> dragonSparklerItems = new HashSet<>();
        final Map<UUID, Location> dragonTargetLocations = new HashMap<>();
        final Map<UUID, UUID> dragonTargetPlayers = new HashMap<>();
        Location dragonSpectatorLocation;
        long dragonNextSpawnAt;
        final Map<UUID, Long> dragonLeapReadyAt = new HashMap<>();
        EnderDragon dragonEscapeDragon;
        Location dragonEscapeLastDestructionLocation;
        final List<Location> dragonEscapeWaypoints = new ArrayList<>();
        final Map<UUID, String> dragonEscapeKits = new HashMap<>();
        final Map<UUID, Integer> dragonEscapeLeapUsesRemaining = new HashMap<>();
        final Map<UUID, Integer> dragonEscapeDiggerUsesRemaining = new HashMap<>();
        final Map<UUID, Long> dragonEscapeNextTntAt = new HashMap<>();
        final Set<UUID> dragonEscapeWarperUsed = new HashSet<>();
        final Map<UUID, UUID> dragonEscapeMineOwners = new HashMap<>();
        String dragonEscapeMapName = "";
        int dragonEscapeDragonProgress;
        final Map<UUID, Integer> dragonEscapePlayerProgress = new HashMap<>();
        final Map<Block, Long> runnerBlocks = new HashMap<>();
        final Map<Block, Material> runnerBlockMaterials = new HashMap<>();
        final Map<UUID, String> runnerKits = new HashMap<>();
        final Map<UUID, Long> runnerLeapReadyAt = new HashMap<>();
        final Map<UUID, Long> runnerQuickshotReadyAt = new HashMap<>();
        final Map<UUID, Long> runnerNextFrostBallAt = new HashMap<>();
        final Map<UUID, Long> runnerFallingBlockExpiresAt = new HashMap<>();
        final Map<UUID, Location> lastLocations = new HashMap<>();
        final int participantsAtStart;
        double baconKnockback = 1.5D;
        long lastBaconHungerAt;
        long lastWorldWaterDamageAt;
        long lastBaconEscalationAt;
        long baconEscalationIntervalMillis = ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS;
        long startedAt;
         boolean microBarrierActive;
        boolean turfCombat;
        long turfPhaseEndsAt;
        int turfFrontlineX = -1;
        int turfInitialFrontlineX = -1;
        int turfFrontlineMinX = -HALF_SIZE;
        int turfFrontlineMaxXExclusive = HALF_SIZE;
        int turfFloorY = 63;
        int turfMinZ = -HALF_SIZE;
        int turfMaxZExclusive = HALF_SIZE + 1;
        long endsAt;
        int ticks;
        BukkitTask tickTask;
        Session(ArcadePortGame type, World world, List<Player> participants) {
            this.type = type;
            this.world = world;
            this.participantsAtStart = participants.size();
        }
    }

    private static final class PlayerSnapshot {
        final Location location;
        final ItemStack[] inventory;
        final ItemStack[] armor;
        final ItemStack offhand;
        final GameMode mode;
        final List<PotionEffect> potionEffects;
        final float exp;
        final int level;
        final boolean allowFlight;
        final boolean flying;
        final double health;
        final double maxHealthBase;
        final int food;
        final float saturation;
        final float walkSpeed;
        PlayerSnapshot(Player p) {
            location = p.getLocation().clone();
            inventory = cloneItems(p.getInventory().getContents());
            armor = cloneItems(p.getInventory().getArmorContents());
            ItemStack off = p.getInventory().getItemInOffHand();
            offhand = off == null ? null : off.clone();
            mode = p.getGameMode();
            potionEffects = new ArrayList<>(p.getActivePotionEffects());
            exp = p.getExp();
            level = p.getLevel();
            allowFlight = p.getAllowFlight();
            flying = p.isFlying();
            health = p.getHealth();
            var maxHealthAttribute = p.getAttribute(Attribute.MAX_HEALTH);
            maxHealthBase = maxHealthAttribute == null ? p.getMaxHealth() : maxHealthAttribute.getBaseValue();
            food = p.getFoodLevel();
            saturation = p.getSaturation();
            walkSpeed = p.getWalkSpeed();
        }
        void restore(Player p) {
            if (p == null || !p.isOnline()) return;
            p.leaveVehicle();
            if (location != null && location.getWorld() != null) p.teleport(location);
            p.getInventory().clear();
            p.getInventory().setContents(cloneItems(inventory));
            p.getInventory().setArmorContents(cloneItems(armor));
            p.getInventory().setItemInOffHand(offhand == null ? null : offhand.clone());
            p.setGameMode(mode);
            for (PotionEffect effect : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(effect.getType());
            for (PotionEffect effect : potionEffects) p.addPotionEffect(effect);
            p.setExp(exp);
            p.setLevel(level);
            p.setFoodLevel(food);
            p.setSaturation(saturation);
            p.setWalkSpeed(walkSpeed);
            p.setAllowFlight(allowFlight);
            var maxHealthAttribute = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null) maxHealthAttribute.setBaseValue(maxHealthBase);
            double maxHealth = p.getMaxHealth();
            p.setHealth(Math.max(1.0D, Math.min(health, maxHealth)));
            if (allowFlight) p.setFlying(flying);
        }
        private static ItemStack[] cloneItems(ItemStack[] items) {
            ItemStack[] copy = new ItemStack[items.length];
            for (int i=0;i<items.length;i++) copy[i] = items[i] == null ? null : items[i].clone();
            return copy;
        }
    }
}
