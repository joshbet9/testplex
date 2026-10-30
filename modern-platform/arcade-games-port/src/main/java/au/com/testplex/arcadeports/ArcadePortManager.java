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
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
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
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
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
import java.util.concurrent.ConcurrentHashMap;
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
    static final int MANUAL_START_COUNTDOWN_SECONDS = 0;
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
    /** Packet-thread-safe entity-ID lookup for the optional Bacon client hit bridge. */
    private final Map<Integer, UUID> baconAvatarOwnersByEntityId = new ConcurrentHashMap<>();
    private Object baconAvatarPacketBridge;
    private DragonEscapeGhostBridge dragonEscapeGhostBridge;
    private final Map<String, List<DragonEscapeGhostFrame>> dragonEscapeGhostFrameCache = new HashMap<>();
    private Session active;
    private BukkitTask pendingStart;
    private ArcadePortGame pendingType;
    private BukkitTask manualStartCountdown;
    private ArcadePortGame manualStartType;
    private long lastDragonHazard;
    private long lastSheepSpawn;
    private List<Location> baconSourceSpawnOverrides = List.of();

    private final NamespacedKey statsActionKey;
    private final NamespacedKey statsCategoryKey;
    private final NamespacedKey statsGameKey;
    private final NamespacedKey bombPowerupKey;
    private final NamespacedKey microTeamItemKey;
    private final ModernArcadeLobby lobby;
    private final ModernArcadeLobbyRuntime lobbyRuntime;

    ArcadePortManager(ArcadeMechanicsModernPlugin plugin) {
        this.plugin = plugin;
        this.statsActionKey = new NamespacedKey(plugin, "stats-action");
        this.statsCategoryKey = new NamespacedKey(plugin, "stats-category");
        this.statsGameKey = new NamespacedKey(plugin, "stats-game");
        this.bombPowerupKey = new NamespacedKey(plugin, "bomb-lobbers-powerup");
        this.microTeamItemKey = new NamespacedKey(plugin, "micro-team-item");
        this.lobby = new ModernArcadeLobby(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(lobby, plugin);
        lobby.ensureWorld();
        this.lobbyRuntime = new ModernArcadeLobbyRuntime(plugin, this, lobby);
        Bukkit.getPluginManager().registerEvents(lobbyRuntime, plugin);
        lobbyRuntime.setSelectedGame(ArcadePortGame.fromId(plugin.getConfig().getString("lobby-selected-game", "bacon-brawl")));
        registerOptionalBaconPacketBridge();
        registerOptionalDragonEscapeGhostBridge();
    }

    public void selectLobbyGame(ArcadePortGame type) {
        if (type == null) return;
        plugin.getConfig().set("lobby-selected-game", type.id());
        plugin.saveConfig();
        lobbyRuntime.setSelectedGame(type);
    }

    public int queuedCount(ArcadePortGame type) {
        if (type == null) return 0;
        Map<UUID, Player> queued = queues.get(type.id());
        return queued == null ? 0 : (int) queued.values().stream().filter(p -> p != null && p.isOnline()).count();
    }

    public void removeFromQueues(Player player) {
        if (player == null) return;
        for (Map<UUID, Player> queued : queues.values()) queued.remove(player.getUniqueId());
    }

    public boolean isActiveGame(World world, ArcadePortGame type) {
        return active != null && active.type == type && active.world == world;
    }

    public boolean isBombLobbersEnemyBot(Player attacker, LivingEntity target) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS
                || attacker == null || target == null || !attacker.isOnline()
                || attacker.getWorld() != session.world || target.getWorld() != session.world
                || !session.alive.contains(attacker.getUniqueId())) return false;
        int attackerTeam = session.teams.getOrDefault(attacker.getUniqueId(), -1);
        return lobbyRuntime.isBombLobbersEnemyBot(target, attackerTeam);
    }

    public List<Player> botTargets(World world, ArcadePortGame type) {
        if (!isActiveGame(world, type)) return List.of();
        if (type != ArcadePortGame.BACON_BRAWL) return alivePlayers(active);
        // Pig Cloak removes the player from AI targeting just as invisibility should.
        return alivePlayers(active).stream()
                .filter(player -> !active.baconCloakUntil.containsKey(player.getUniqueId()))
                .toList();
    }

    /**
     * Resolve bot attacks using the same Bacon Brawl knockback and Brutus guard rules
     * as a player attack. Test bots have no kill credit, but their hits still count as
     * real incoming combat for fall attribution and defensive-kit testing.
     */
    public boolean applyBaconBrawlBotAttack(Location attackerLocation, Player target) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL
                || attackerLocation == null || attackerLocation.getWorld() != session.world
                || target == null || !target.isOnline() || target.getWorld() != session.world
                || !session.alive.contains(target.getUniqueId())) return false;

        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();
        long brutusUntil = session.baconBrutusUntil.getOrDefault(targetId, 0L);
        boolean brutusActive = brutusUntil > now;
        boolean backstab = isBaconBrutusBackstab(target, attackerLocation);
        if (ArcadePortRules.shouldBaconBrutusGuardHit(brutusActive, backstab)) {
            target.sendActionBar(ChatColor.GOLD + "" + ChatColor.BOLD + "BRUTUS GUARD"
                    + ChatColor.GRAY + " | frontal bot hit absorbed");
            Location guard = target.getLocation().clone().add(0.0D, 1.0D, 0.0D);
            target.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, guard, 10,
                    0.35D, 0.45D, 0.35D, 0.01D);
            target.playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8F, 1.5F);
            return false;
        }
        if (brutusActive && backstab) {
            endBaconBrutus(session, targetId, target);
            target.sendActionBar(ChatColor.RED + "" + ChatColor.BOLD + "BRUTUS GUARD BROKEN!");
            target.playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.9F, 0.8F);
        }

        // Do not credit a previous human attacker for a later bot-caused fall.
        session.baconLastDamager.remove(targetId);
        session.baconLastDamagedAt.put(targetId, now);
        session.baconLastHitSource.put(targetId, "bot");
        Vector velocity = baconBrawlKnockbackVelocity(attackerLocation, target.getLocation(),
                2.0D, session.baconKnockback, null, target.isOnGround());
        target.setVelocity(velocity);
        Location impact = target.getLocation().clone().add(0.0D, 1.0D, 0.0D);
        target.getWorld().spawnParticle(Particle.CLOUD, impact, 10, 0.35D, 0.4D, 0.35D, 0.04D);
        target.playEffect(EntityEffect.HURT);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9F, 0.85F);
        target.sendActionBar(ChatColor.RED + "HIT BY TEST BOT");
        return true;
    }

    public double baconBrawlKnockbackValue(World world) {
        return isActiveGame(world, ArcadePortGame.BACON_BRAWL) ? active.baconKnockback : 1.5D;
    }

    /** Record a test-bot hit using the same meter and timed-hit window as player hits. */
    public BaconBrawlBotHitResult recordBaconBrawlBotHit(Player attacker, String botName, double rawDamage) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL || attacker == null
                || !attacker.isOnline() || attacker.getWorld() != session.world
                || !session.players.containsKey(attacker.getUniqueId())
                || !session.alive.contains(attacker.getUniqueId())) return new BaconBrawlBotHitResult(-1, 1.0D);
        UUID id = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        long previousHit = session.baconLastSwingAt.getOrDefault(id, 0L);
        double timingMultiplier = previousHit > 0L
                ? ArcadePortRules.baconTimedHitKnockbackMultiplier(now - previousHit) : 1.0D;
        session.baconLastSwingAt.put(id, now);
        if (timingMultiplier >= 1.5D) {
            session.baconHitFeedbackUntil.put(id, now + ArcadePortRules.BACON_HIT_FEEDBACK_MILLIS);
        }
        int meter = ArcadePortRules.nextBaconMeter(session.baconMeters.getOrDefault(id, 0), rawDamage);
        session.baconMeters.put(id, meter);
        updateBaconMeterItem(session, attacker);
        updateBaconMeterScoreboard(session);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8F, 1.1F);
        return new BaconBrawlBotHitResult(meter, timingMultiplier);
    }

    /**
     * Resolve a practice-bot hit through the same timed-hit, meter, backstab and knockback
     * calculation as player-versus-player Bacon Brawl combat. The game is knockback-based:
     * as with real players, a successful hit is not ordinary health damage.
     */
    public BaconBrawlBotHitResult applyBaconBrawlBotHit(Player attacker, LivingEntity target,
            String botName, String targetKit, double rawDamage) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL
                || attacker == null || target == null || !target.isValid()
                || target.getWorld() != session.world
                || !session.alive.contains(attacker.getUniqueId())) {
            return new BaconBrawlBotHitResult(-1, 1.0D);
        }

        BaconBrawlBotHitResult hit = recordBaconBrawlBotHit(attacker, botName, rawDamage);
        if (hit.meter() < 0) return hit;

        double multiplier = session.baconKnockback * hit.timingMultiplier();
        String attackerKit = session.baconKits.getOrDefault(attacker.getUniqueId(), "el-muchacho-pigo");
        Vector targetLook = target.getLocation().getDirection().setY(0.0D);
        Vector fromTargetToAttacker = attacker.getLocation().toVector()
                .subtract(target.getLocation().toVector()).setY(0.0D);
        boolean backstab = ArcadePortRules.isBaconBrawlBackstab(targetLook.getX(), targetLook.getZ(),
                fromTargetToAttacker.getX(), fromTargetToAttacker.getZ());
        if ("pig".equals(attackerKit) && backstab) {
            multiplier *= ArcadePortRules.BACON_PIG_BACKSTAB_KNOCKBACK_MULTIPLIER;
        }
        target.setNoDamageTicks(0);
        target.setVelocity(baconBrawlKnockbackVelocity(
                attacker.getLocation(), target.getLocation(), rawDamage, multiplier, null, target.isOnGround()));

        if (session.baconCloakUntil.containsKey(attacker.getUniqueId())) {
            endBaconCloak(session, attacker.getUniqueId(), attacker);
        }
        return hit;
    }

    public static final class BaconBrawlBotHitResult {
        private final int meter;
        private final double timingMultiplier;

        public BaconBrawlBotHitResult(int meter, double timingMultiplier) {
            this.meter = meter;
            this.timingMultiplier = timingMultiplier;
        }

        public int meter() { return meter; }
        public double timingMultiplier() { return timingMultiplier; }
    }

    public void handleBotCommand(org.bukkit.command.CommandSender sender, String[] args) {
        lobbyRuntime.handleBotCommand(sender, args);
    }

    /**
     * PacketEvents is optional: no PacketEvents class is loaded unless that plugin is installed.
     * Bare Paper keeps the previous server-side-avatar hit handling as a fallback.
     */
    private void registerOptionalBaconPacketBridge() {
        org.bukkit.plugin.Plugin packetEvents = Bukkit.getPluginManager().getPlugin("packetevents");
        if (packetEvents == null || !packetEvents.isEnabled()) {
            plugin.getLogger().info("PacketEvents not found; using fallback Bacon Brawl avatar hit handling.");
            return;
        }
        try {
            Class<?> bridgeType = Class.forName(
                    "au.com.testplex.arcadeports.BaconAvatarPacketBridge", true, plugin.getClass().getClassLoader());
            Object bridge = bridgeType
                    .getConstructor(ArcadeMechanicsModernPlugin.class, ArcadePortManager.class)
                    .newInstance(plugin, this);
            bridgeType.getMethod("register").invoke(bridge);
            baconAvatarPacketBridge = bridge;
            plugin.getLogger().info("Bacon Brawl packet hitbox bridge enabled.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("Could not enable Bacon Brawl packet hitbox bridge; using fallback: "
                    + exception.getMessage());
        }
    }

    private void unregisterOptionalBaconPacketBridge() {
        Object bridge = baconAvatarPacketBridge;
        baconAvatarPacketBridge = null;
        if (bridge == null) return;
        try {
            bridge.getClass().getMethod("unregister").invoke(bridge);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("Could not unregister Bacon Brawl packet bridge cleanly: "
                    + exception.getMessage());
        }
    }

    /** Loads the genuine player-model ghost bridge only when PacketEvents is installed. */
    private void registerOptionalDragonEscapeGhostBridge() {
        org.bukkit.plugin.Plugin packetEvents = Bukkit.getPluginManager().getPlugin("packetevents");
        if (packetEvents == null || !packetEvents.isEnabled()) {
            plugin.getLogger().warning("Dragon Escape transparent player ghosts require PacketEvents 2.14.0.");
            return;
        }
        try {
            Class<?> bridgeType = Class.forName(
                    "au.com.testplex.arcadeports.DragonEscapePacketGhostBridge",
                    true, plugin.getClass().getClassLoader());
            Object bridge = bridgeType.getConstructor(org.bukkit.plugin.java.JavaPlugin.class).newInstance(plugin);
            dragonEscapeGhostBridge = (DragonEscapeGhostBridge) bridge;
            plugin.getLogger().info("Dragon Escape translucent player-model ghost bridge enabled.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            dragonEscapeGhostBridge = null;
            plugin.getLogger().warning("Could not enable Dragon Escape player ghost renderer: "
                    + exception.getMessage());
        }
    }

    private void hideDragonEscapeGhost(Player viewer) {
        DragonEscapeGhostBridge bridge = dragonEscapeGhostBridge;
        if (bridge != null && viewer != null) bridge.hide(viewer);
    }

    /** Safe for packet-thread lookups; the owner mapping is published via a concurrent map. */
    public boolean isBaconAvatarEntityId(int entityId) {
        return baconAvatarOwnersByEntityId.containsKey(entityId);
    }

    /** The packet adapter has cancelled the proxy attack packet; apply the hit to the hidden player. */
    public void handleBaconAvatarPacketAttack(Player attacker, int avatarEntityId) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL
                || attacker == null || !attacker.isOnline()) return;
        UUID targetId = baconAvatarOwnersByEntityId.get(avatarEntityId);
        if (targetId == null) return;
        Player target = session.players.get(targetId);
        boolean accepted = ArcadePortRules.shouldAcceptBaconAvatarPacketHit(
                session.type == ArcadePortGame.BACON_BRAWL,
                session.alive.contains(attacker.getUniqueId()),
                target != null && target.isOnline() && session.alive.contains(targetId),
                targetId.equals(attacker.getUniqueId()),
                target == null ? Double.POSITIVE_INFINITY
                        : attacker.getEyeLocation().distanceSquared(target.getEyeLocation()),
                target != null && attacker.hasLineOfSight(target));
        if (!accepted || target == null) return;
        handleBaconBrawlHit(session, attacker, target, "melee");
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
            player.sendMessage(ChatColor.YELLOW + "Bacon Brawl kits: El Muchacho Pigo, Mama Piggles, Pig Cloak, Brutus Bacon. Chris P. Bacon is intentionally omitted.");
            return;
        }
        baconKitPreferences.put(player.getUniqueId(), kit);
        Session session = active;
        boolean activeBaconRound = session != null && session.type == ArcadePortGame.BACON_BRAWL
                && session.players.containsKey(player.getUniqueId()) && session.alive.contains(player.getUniqueId());
        boolean roundLocked = ArcadePortRules.shouldDeferBaconBrawlKitChange(activeBaconRound);
        String displayKit = switch (kit) {
            case "mama-piggles" -> "Mama Piggles";
            case "pig" -> "Pig Cloak";
            case "brutus-bacon" -> "Brutus Bacon";
            default -> "El Muchacho Pigo";
        };
        player.sendMessage(ChatColor.GREEN + "Bacon Brawl kit set to " + displayKit
                + (roundLocked ? " for your next test. Kits are locked once a round starts."
                        : " for your next test."));
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
        Session session = active;
        if (session != null && session.type == ArcadePortGame.DRAGON_ESCAPE
                && session.players.containsKey(id)) {
            player.sendMessage(ChatColor.YELLOW
                    + "Dragon Escape kits are locked once the race starts. Your current kit stays unchanged.");
            return;
        }
        dragonEscapeKitPreferences.put(id, kit);
        player.sendMessage(ChatColor.GREEN + "Dragon Escape kit set to " + kit + " for your next test.");
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
            player.sendMessage(ChatColor.YELLOW + "Death Tag runner kits: basher, archer, traitor, null.");
            return;
        }
        Session session = active;
        if (session != null && session.type == ArcadePortGame.DEATH_TAG
                && session.players.containsKey(player.getUniqueId())) {
            if (session.tagged.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Chasers cannot switch back to a runner kit.");
                return;
            }
            deathTagRunnerKitPreferences.put(player.getUniqueId(), kit);
            session.deathTagRunnerKits.put(player.getUniqueId(), kit);
            giveDeathTagKit(session, player, false, false);
            player.sendMessage(ChatColor.GREEN + "Death Tag runner kit set to " + kit + ".");
        } else {
            deathTagRunnerKitPreferences.put(player.getUniqueId(), kit);
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

    private boolean isGameEnabled(ArcadePortGame type) {
        return type != null && ArcadePortRules.isArcadePortGameEnabled(
                type.id(), plugin.getConfig().getStringList("enabled-games"));
    }

    public String enabledGamesLabel() {
        List<String> enabled = plugin.getConfig().getStringList("enabled-games");
        return enabled.isEmpty() ? "none" : String.join(", ", enabled);
    }

    public void queue(ArcadePortGame type, Player player, boolean quickStart) {
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Unknown game id. Use /arcadeport help for the available IDs.");
            return;
        }
        if (!isGameEnabled(type)) {
            player.sendMessage(ChatColor.RED + type.displayName()
                    + " is not cleared for in-game parity testing yet. Cleared game IDs: " + enabledGamesLabel() + ".");
            return;
        }
        if (active != null && active.type == type) {
            player.sendMessage(ChatColor.RED + "That game is already in progress. Use /arcadeport status or queue the next game.");
            return;
        }
        if (manualStartCountdown != null && manualStartType != type) {
            player.sendMessage(ChatColor.RED + manualStartType.displayName()
                    + " is already counting down. Wait for that round to start before queueing another game.");
            return;
        }
        lobby.ensurePlayerInLobby(player);
        for (LinkedHashMap<UUID, Player> queue : queues.values()) queue.remove(player.getUniqueId());
        queues.computeIfAbsent(type.id(), ignored -> new LinkedHashMap<>()).put(player.getUniqueId(), player);
        String nextAction = manualStartCountdown != null && manualStartType == type
                ? "The existing start countdown is already running."
                : quickStart ? "Starting after a short join window so friends can join too."
                : "An operator/player can use /arcadeport start " + type.id() + ".";
        player.sendMessage(ChatColor.GREEN + "Queued for " + type.displayName() + ". " + nextAction);
        if (quickStart) scheduleQuickStart(type);
    }

    private void scheduleQuickStart(ArcadePortGame type) {
        if (active != null) return;
        if (manualStartCountdown != null) {
            for (Player player : onlineQueuedPlayers(type)) {
                player.sendMessage(ChatColor.YELLOW + manualStartType.displayName()
                        + " already has a start countdown; the queued quick-start will not create a second timer.");
            }
            return;
        }
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

    /**
     * Start as soon as the lobby Start Game action is selected. The modern test
     * harness should not impose the legacy 20-second countdown while mechanics
     * are being tested.
     */
    public void startFromLobby(ArcadePortGame type, org.bukkit.command.CommandSender sender) {
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown game id.");
            return;
        }
        if (!isGameEnabled(type)) {
            sender.sendMessage(ChatColor.RED + type.displayName()
                    + " is not cleared for in-game parity testing yet. Cleared game IDs: " + enabledGamesLabel() + ".");
            return;
        }
        if (active != null) {
            sender.sendMessage(ChatColor.RED + "A modern Arcade test is already running: "
                    + active.type.displayName() + ".");
            return;
        }
        if (manualStartCountdown != null) {
            manualStartCountdown.cancel();
            manualStartCountdown = null;
            manualStartType = null;
            clearLobbyCountdownStatus();
        }
        if (SourceMapCatalog.hasMap(type)) {
            String selectedResource = plugin.getConfig().getString("selected-maps." + type.id(),
                    SourceMapCatalog.resourceFor(type));
            if (!SourceMapCatalog.isInstalledResource(plugin, type, selectedResource)) {
                sender.sendMessage(ChatColor.RED + "The selected map is no longer installed for " + type.displayName()
                        + ". Open Set Game and choose an installed map again.");
                return;
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOnline() && lobby.isLobbyWorld(player.getWorld())) {
                queues.computeIfAbsent(type.id(), ignored -> new LinkedHashMap<>())
                        .put(player.getUniqueId(), player);
            }
        }
        LinkedHashMap<UUID, Player> queue = queues.get(type.id());
        boolean anyQueued = queue != null
                && queue.values().stream().anyMatch(player -> player != null && player.isOnline());
        if (!anyQueued) {
            sender.sendMessage(ChatColor.YELLOW + "No players are in the Arcade lobby to start "
                    + type.displayName() + ". Join the lobby first.");
            return;
        }

        if (pendingStart != null) {
            pendingStart.cancel();
            pendingStart = null;
            pendingType = null;
        }

        Player requester = sender instanceof Player player && lobby.isLobbyWorld(player.getWorld())
                ? player : null;
        List<Player> notifyPlayers = onlineQueuedPlayers(type);
        String hostName = sender instanceof Player player ? player.getName() : "An operator";
        clearLobbyCountdownStatus();
        try {
            startQueued(type, requester, sender);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Unhandled error while starting " + type.displayName() + ".", exception);
            sender.sendMessage(ChatColor.RED + "Could not start " + type.displayName()
                    + ". The error was logged on the server; no successful round start was recorded.");
            return;
        }
        if (active != null && active.type == type) {
            for (Player player : notifyPlayers) {
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + hostName
                            + ChatColor.AQUA + " has started the game.");
                }
            }
        }
    }

    private List<Player> onlineQueuedPlayers(ArcadePortGame type) {
        LinkedHashMap<UUID, Player> queue = queues.get(type.id());
        if (queue == null) return List.of();
        return queue.values().stream().filter(player -> player != null && player.isOnline()).toList();
    }

    private void updateLobbyCountdownStatus(ArcadePortGame type, int secondsRemaining) {
        if (type == ArcadePortGame.BACON_BRAWL) {
            plugin.getConfig().set("lobby-countdown-status", "Starting in " + secondsRemaining + "s");
            lobby.refreshLobbyScoreboards();
        }
    }

    private void clearLobbyCountdownStatus() {
        plugin.getConfig().set("lobby-countdown-status", null);
        lobby.refreshLobbyScoreboards();
    }

    private void cancelManualStartCountdown(ArcadePortGame type, String message) {
        if (manualStartCountdown != null) manualStartCountdown.cancel();
        manualStartCountdown = null;
        manualStartType = null;
        clearLobbyCountdownStatus();
        for (Player player : onlineQueuedPlayers(type)) player.sendMessage(message);
    }

    public void startQueued(ArcadePortGame type, Player requester, org.bukkit.command.CommandSender sender) {
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown game id.");
            return;
        }
        if (!isGameEnabled(type)) {
            sender.sendMessage(ChatColor.RED + type.displayName()
                    + " is not cleared for in-game parity testing yet. Cleared game IDs: " + enabledGamesLabel() + ".");
            return;
        }
        if (SourceMapCatalog.hasMap(type)) {
            String selectedResource = plugin.getConfig().getString("selected-maps." + type.id(),
                    SourceMapCatalog.resourceFor(type));
            if (!SourceMapCatalog.isInstalledResource(plugin, type, selectedResource)) {
                sender.sendMessage(ChatColor.RED + "The selected map is no longer installed for " + type.displayName()
                        + ". Open Set Game and choose an installed map again.");
                return;
            }
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
            if (type == ArcadePortGame.BACON_BRAWL) baconSourceSpawnOverrides = List.of();
            try {
                world = createWorld(type);
            } catch (RuntimeException firstFailure) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Archived map load failed for " + type.displayName()
                                + "; cleaning the isolated source and retrying once.", firstFailure);
                world = null;
                boolean reset = SourceMapCatalog.resetTestWorld(plugin, sourceWorldName);
                sourceMapWorlds.remove(type.id());
                sourceMapData.remove(type.id());
                worlds.remove(type.id());
                if (reset) {
                    try {
                        world = createWorld(type);
                    } catch (RuntimeException retryFailure) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                "Archived map retry failed for " + type.displayName()
                                        + "; refusing to replace the authentic map with a generated arena.", retryFailure);
                        world = null;
                    }
                }

            }
            if (world != null) worlds.put(type.id(), world);
        } else {
            world = worlds.computeIfAbsent(type.id(), ignored -> createWorld(type));
        }
        if (world == null) {
            if (SourceMapCatalog.hasMap(type)) {
                sender.sendMessage(ChatColor.RED + "Could not load the authentic " + type.displayName()
                        + " map after a clean retry. Check the server log for the selected archive and load exception.");
            } else {
                sender.sendMessage(ChatColor.RED + "The test world could not be loaded. Check the server log.");
            }
            return;
        }
        // A mapped game must remain on its authentic legacy terrain. Never overwrite
        // an imported map with a synthetic platform if its metadata or starts are invalid.
        if (SourceMapCatalog.hasMap(type)
                && (!sourceMapWorlds.contains(type.id()) || sourceMapData.get(type.id()) == null)) {
            sender.sendMessage(ChatColor.RED + "The authentic " + type.displayName()
                    + " map or its WorldConfig metadata could not be loaded. No fallback arena was generated; see server logs.");
            return;
        }
        if (type == ArcadePortGame.BACON_BRAWL
                && !prepareBaconSourceSpawns(world, sourceMapData.get(type.id()), participants.size())) {
            SourceMapCatalog.MapData mapData = sourceMapData.get(type.id());
            String mapName = mapData == null ? "unknown map" : mapData.mapName();
            sender.sendMessage(ChatColor.RED + "No safe authored spawn could be resolved on authentic map "
                    + mapName + ". The map was preserved and no generated platform was added; check server logs.");
            return;
        }
        if (!sourceMapWorlds.contains(type.id())) buildArena(type, world);
        Session session = new Session(type, world, participants);
        active = session;
        long now = System.currentTimeMillis();
        session.startedAt = now;
        if (type == ArcadePortGame.BOMB_LOBBERS) {
            session.bombNextPowerupSpawnAt = now
                    + ArcadePortRules.BOMB_LOBBERS_POWERUP_FIRST_SPAWN_DELAY_MILLIS;
        }
        session.endsAt = (type == ArcadePortGame.BACON_BRAWL
                && !ArcadePortRules.BACON_BRAWL_HAS_ROUND_TIMER)
                || (type == ArcadePortGame.DRAGON_ESCAPE && !ArcadePortRules.DRAGON_ESCAPE_HAS_ROUND_TIMER)
                ? 0L : now + (type == ArcadePortGame.SHEEP_QUEST ? ArcadePortRules.SHEEP_QUEST_ROUND_MILLIS
                : type == ArcadePortGame.GLADIATORS ? 600_000L
                : type == ArcadePortGame.BOMB_LOBBERS ? ArcadePortRules.BOMB_LOBBERS_ROUND_MILLIS
                : ROUND_MS);
        session.lastBaconHungerAt = now;
        session.lastBaconSlowUpdateAt = now;
        session.lastWorldWaterDamageAt = now;
        session.lastBaconEscalationAt = now;
        session.lastSpleefHungerAt = now;
        session.dragonNextSpawnAt = now;
        session.dragonSpectatorLocation = dragonSpectatorLocation(session);
        session.baconKnockback = ArcadePortRules.initialBaconKnockback(participants.size());
        session.baconEscalationIntervalMillis = ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS;
        session.microBarrierActive = type == ArcadePortGame.MICRO_BATTLES;
        YamlConfiguration microCosmeticPreferences = type == ArcadePortGame.MICRO_BATTLES
                ? loadMicroBattlesCosmeticPreferences() : null;
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
        if (type == ArcadePortGame.MICRO_BATTLES && session.microBarrierBlocks.isEmpty()) {
            // Source-map fallback: retain the same two-block team-island glass rings as the generated arena.
            for (int team = 0; team < 4; team++) {
                Location island = microSourceMap == null ? islandSpawn(team * 2) : microTeamCenter(session, team);
                int cx = island.getBlockX(), cy = island.getBlockY(), cz = island.getBlockZ();
                for (int offset = -4; offset <= 4; offset++) {
                    for (int y = cy; y <= ArcadePortRules.microBattlesBarrierTopY(cy); y++) {
                        Block[] ring = {
                                session.world.getBlockAt(cx + offset, y, cz - 4),
                                session.world.getBlockAt(cx + offset, y, cz + 4),
                                session.world.getBlockAt(cx - 4, y, cz + offset),
                                session.world.getBlockAt(cx + 4, y, cz + offset)
                        };
                        for (Block barrier : ring) {
                            if (barrier.getType() == Material.GLASS) session.microBarrierBlocks.add(barrier);
                        }
                    }
                }
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
            PlayerSnapshot pendingSnapshot = pendingRestore.remove(player.getUniqueId());
            if (pendingSnapshot != null) pendingSnapshot.restore(player);
            lobby.ensurePlayerInLobby(player);
            // Always snapshot the controlled lobby state, never the server's old survival spawn.
            PlayerSnapshot snapshot = new PlayerSnapshot(player);
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
                session.dragonEscapeRunSplitMillis.put(playerId, new HashMap<>(Map.of(0, 0L)));
                session.dragonEscapeRunSplitLocations.put(playerId, new HashMap<>());
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
                UUID baconId = player.getUniqueId();
                session.baconKits.put(baconId, baconKitPreferences.getOrDefault(baconId, "el-muchacho-pigo"));
                session.baconMeters.put(baconId, 0);
            }
            if (type == ArcadePortGame.BOMB_LOBBERS) {
                session.bombLobbersKits.put(player.getUniqueId(),
                        bombLobbersKitPreferences.getOrDefault(player.getUniqueId(), "jumper"));
                session.bombPitchLevels.put(player.getUniqueId(), 2);
                session.bombDoubleJumpAvailable.add(player.getUniqueId());
            }
            if (type == ArcadePortGame.MICRO_BATTLES) {
                UUID microId = player.getUniqueId();
                session.microKits.put(microId, microKitPreferences.getOrDefault(microId, "archer"));
                session.microNextArrowAt.put(microId,
                        now + ArcadePortRules.MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS);
                session.microGlassColours.put(microId, microBattlesColourSelection(
                        microCosmeticPreferences, microId, "middle-glass-colour"));
                session.microBorderColours.put(microId, microBattlesColourSelection(
                        microCosmeticPreferences, microId, "border-particle-colour"));
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
            if (type == ArcadePortGame.DRAGON_ESCAPE) {
                UUID dragonPlayerId = player.getUniqueId();
                session.dragonEscapeStartLocations.put(dragonPlayerId, spawn.clone());
                session.dragonEscapeRunSplitLocations.computeIfAbsent(
                        dragonPlayerId, ignored -> new HashMap<>()).putIfAbsent(0, spawn.clone());
            }
            if (type == ArcadePortGame.MICRO_BATTLES && !sourceMapWorlds.contains(type.id())) {
                spawn = microTeamSpawn(teamId, i / 4, (participants.size() + 3) / 4);
            } else if (type == ArcadePortGame.TURF_WARS && !sourceMapWorlds.contains(type.id())) {
                spawn = turfWarsSpawn(session, session.teams.get(player.getUniqueId()), i / 2,
                        (participants.size() + 1) / 2);
            }
            player.closeInventory();
            player.leaveVehicle();
            player.teleport(spawn);
            player.setGameMode(type == ArcadePortGame.BACON_BRAWL || type == ArcadePortGame.DEATH_TAG
                    ? GameMode.ADVENTURE : GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setFireTicks(0);
            player.setExp(0.0F);
            player.setLevel(0);
            player.getInventory().clear();
            giveKit(session, player);
            session.lastLocations.put(player.getUniqueId(), spawn.clone());
            if (type == ArcadePortGame.MICRO_BATTLES) {
                UUID microId = player.getUniqueId();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (active == session && player.isOnline() && session.alive.contains(microId)
                            && session.microBarrierActive) applyMicroMiddleGlassCosmetic(session, player);
                }, 2L);
            }
            if (type == ArcadePortGame.DRAGON_ESCAPE) {
                Location initial = player.getLocation().clone();
                session.dragonEscapeRunGhostFrames.put(player.getUniqueId(), new ArrayList<>(List.of(
                        new DragonEscapeGhostFrame(0L, initial.getX(), initial.getY(), initial.getZ(),
                                initial.getYaw(), initial.getPitch(), player.isSneaking(),
                                player.isSprinting(), player.isOnGround()))));
            }
        }
        if (type == ArcadePortGame.BOMB_LOBBERS) {
            cacheBombLobbersTeamSpawnCentres(session);
            updateBombLobbersMvpEligibility(session);
            int playerTeam = participants.isEmpty() ? 0
                    : session.teams.getOrDefault(participants.get(0).getUniqueId(), 0);
            int botTeam = ArcadePortRules.opposingBombLobbersTeam(playerTeam);
            SourceMapCatalog.MapData bombMap = sourceMapData.get(type.id());
            List<Location> botSpawns = bombMap == null
                    ? List.of() : bombMap.teamSpawnLocationsForTeam(botTeam, 2);
            if (botSpawns.isEmpty()) {
                Location humanSpawn = participants.isEmpty() ? spawns.get(0)
                        : session.lastLocations.getOrDefault(participants.get(0).getUniqueId(), spawns.get(0));
                double centerX = bombMap != null && bombMap.hasBounds() ? bombMap.centerX() : 0.0D;
                double centerZ = bombMap != null && bombMap.hasBounds() ? bombMap.centerZ() : 0.0D;
                Location mirrored = humanSpawn.clone();
                mirrored.setX(centerX * 2.0D - humanSpawn.getX());
                mirrored.setZ(centerZ * 2.0D - humanSpawn.getZ());
                mirrored.setYaw(humanSpawn.getYaw() + 180.0F);
                botSpawns = List.of(mirrored);
            }
            lobbyRuntime.deployBots(type, botSpawns, botTeam);
        } else {
            lobbyRuntime.deployBots(type, spawns, -1);
        }
        if (type == ArcadePortGame.BACON_BRAWL) {
            // Player snapshots already hold their pre-match scoreboards at this point.
            // The meter and disguise teams now live only on this match's scoreboard.
            setupBaconDisguiseNameTags(session, participants);
            setupBaconMeterScoreboard(session);
        }
        if (type == ArcadePortGame.DEATH_TAG) {
            if (participants.size() > 1) {
                Player chaser = participants.get(ThreadLocalRandom.current().nextInt(participants.size()));
                session.tagged.add(chaser.getUniqueId());
                session.alphaChasers.add(chaser.getUniqueId());
                chaser.sendMessage(ChatColor.RED + "You are the Alpha Chaser! Tag every runner.");
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
                runner.sendMessage(ChatColor.YELLOW + "Solo practice: avoid the Chaser. Practice hits do not end your run.");
            }
        }
        if (type == ArcadePortGame.DEATH_TAG) {
            for (Player player : participants) {
                giveDeathTagKit(session, player, session.tagged.contains(player.getUniqueId()),
                        session.alphaChasers.contains(player.getUniqueId()));
            }
            setupDeathTagScoreboard(session);
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
        if (type == ArcadePortGame.DRAGON_ESCAPE) {
            setupDragonEscapeRaceScoreboard(session);
        }
        if (participants.size() == 1) announce(session, ChatColor.GRAY + "Solo test mode: the run will remain available for practice until the timer ends or you reset it.");
        session.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(session), 1L, 1L);
        sender.sendMessage(ChatColor.GREEN + "Started " + type.displayName() + " with " + participants.size() + (participants.size() == 1 ? " player." : " players."));
    }

    private World createWorld(ArcadePortGame type) {
        return createWorld(type, true);
    }

    private World createWorld(ArcadePortGame type, boolean allowSourceMap) {
        String sourceWorldName = "arcadeport_source_" + type.id().replace('-', '_');
        World world = allowSourceMap ? Bukkit.getWorld(sourceWorldName) : null;
        if (allowSourceMap && world == null && SourceMapCatalog.hasMap(type)) {
            String selectedResource = SourceMapCatalog.selectedResource(plugin, type);
            if (selectedResource == null || !SourceMapCatalog.install(plugin, type, sourceWorldName)) {
                throw new IllegalStateException("Could not install selected authentic map resource '"
                        + selectedResource + "' for " + type.displayName());
            }
            WorldCreator creator = new WorldCreator(sourceWorldName)
                    .environment(World.Environment.NORMAL).generateStructures(false);
            world = creator.createWorld();
            if (world == null) {
                throw new IllegalStateException("Paper could not load the authentic source map world "
                        + sourceWorldName);
            }
        }
        if (allowSourceMap && world != null && SourceMapCatalog.hasMap(type)
                && !sourceMapWorlds.contains(type.id())) {
            SourceMapCatalog.MapData mapData = SourceMapCatalog.read(plugin, type, world);
            if (mapData == null) {
                Path worldFolder = world.getWorldFolder().toPath();
                Path legacyConfig = SourceMapCatalog.resolveWorldConfigPath(
                        worldFolder, Bukkit.getWorldContainer().toPath(), sourceWorldName);
                throw new IllegalStateException("The selected authentic map has no readable WorldConfig.dat. "
                        + "World folder=" + worldFolder + ", expected metadata=" + legacyConfig
                        + ", selected archive=" + SourceMapCatalog.selectedResource(plugin, type));
            }
            Path worldConfig = SourceMapCatalog.resolveWorldConfigPath(world.getWorldFolder().toPath(),
                    Bukkit.getWorldContainer().toPath(), sourceWorldName);
            if (worldConfig != null && Files.isRegularFile(worldConfig)) {
                plugin.getLogger().info("Loaded " + type.displayName() + " map metadata from " + worldConfig + ".");
            } else {
                plugin.getLogger().fine("The source WorldConfig.dat is not present in the migrated world directory; "
                        + "SourceMapCatalog read it from the embedded selected archive.");
            }
            sourceMapWorlds.add(type.id());
            sourceMapData.put(type.id(), mapData);
            plugin.getLogger().info("Loaded authentic " + type.displayName() + " map: "
                    + mapData.mapName() + " (" + mapData.spawnLocations().size() + " configured spawn locations).");
        }
        if (world == null) {
            if (allowSourceMap && SourceMapCatalog.hasMap(type)) {
                throw new IllegalStateException("Refusing to create a generated world for mapped game "
                        + type.displayName());
            }
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

    /**
     * Resolve each authored Bacon Brawl spawn on the imported map itself. If a 1.8
     * spawn Y is slightly different after conversion, adjust only its vertical
     * coordinate at the same X/Z. The source terrain is never erased or covered.
     */
    private boolean prepareBaconSourceSpawns(
            World world, SourceMapCatalog.MapData sourceMap, int participantCount) {
        if (world == null || sourceMap == null) return false;
        List<Location> sourceSpawns = sourceMap.teamSpawnLocationsForName("Green");
        if (sourceSpawns.isEmpty()) sourceSpawns = sourceMap.dataLocations("GREEN");
        if (sourceSpawns.isEmpty()) sourceSpawns = sourceMap.spawnLocations();
        if (sourceSpawns.isEmpty()) {
            plugin.getLogger().warning("Authentic Bacon Brawl map " + sourceMap.mapName()
                    + " has no authored player spawn locations in WorldConfig.dat.");
            return false;
        }

        List<Location> resolved = new ArrayList<>();
        for (Location source : sourceSpawns) {
            Location safe = findSafeBaconSourceSpawn(world, source);
            if (safe == null) {
                int x = source == null ? 0 : source.getBlockX();
                int y = source == null ? 0 : source.getBlockY();
                int z = source == null ? 0 : source.getBlockZ();
                String blockInfo = source == null ? "null location" : "floor="
                        + world.getBlockAt(x, Math.max(world.getMinHeight(), y - 1), z).getType()
                        + ", feet=" + world.getBlockAt(x, y, z).getType()
                        + ", head=" + world.getBlockAt(x, y + 1, z).getType();
                plugin.getLogger().warning("No safe floor found within 32 vertical blocks of authentic Bacon Brawl spawn "
                        + x + "," + y + "," + z + " in " + sourceMap.mapName() + " (" + blockInfo + ").");
                return false;
            }
            if (Math.abs(safe.getY() - source.getY()) > 0.01D) {
                plugin.getLogger().info("Adjusted converted spawn height on authentic map " + sourceMap.mapName()
                        + " from Y=" + source.getY() + " to Y=" + safe.getY()
                        + " at X/Z " + source.getBlockX() + "/" + source.getBlockZ() + ".");
            }
            resolved.add(safe);
        }
        baconSourceSpawnOverrides = List.copyOf(resolved);
        plugin.getLogger().info("Using " + resolved.size() + " safe authored spawn points on authentic Bacon Brawl map "
                + sourceMap.mapName() + "; queued players=" + Math.max(1, participantCount) + ".");
        return !baconSourceSpawnOverrides.isEmpty();
    }

    private Location findSafeBaconSourceSpawn(World world, Location source) {
        if (source == null || source.getWorld() != world) return null;
        int x = source.getBlockX();
        int baseY = source.getBlockY();
        int z = source.getBlockZ();
        world.getChunkAt(x >> 4, z >> 4).load();
        for (int offset = 0; offset <= 32; offset++) {
            int[] candidates = offset == 0 ? new int[]{baseY}
                    : new int[]{baseY - offset, baseY + offset};
            for (int y : candidates) {
                if (y < world.getMinHeight() + 2 || y >= world.getMaxHeight() - 2) continue;
                Block floor = world.getBlockAt(x, y - 1, z);
                Block feet = world.getBlockAt(x, y, z);
                Block head = world.getBlockAt(x, y + 1, z);
                Material floorType = floor.getType();
                if (floor.isPassable()
                        || floorType == Material.MAGMA_BLOCK
                        || floorType == Material.CAMPFIRE
                        || floorType == Material.SOUL_CAMPFIRE
                        || floorType == Material.FIRE
                        || floorType == Material.SOUL_FIRE
                        || floorType == Material.CACTUS
                        || floorType == Material.POWDER_SNOW
                        || floorType == Material.SWEET_BERRY_BUSH
                        || floorType == Material.WITHER_ROSE
                        || floorType == Material.POINTED_DRIPSTONE
                        || !feet.isPassable()
                        || !head.isPassable()
                        || feet.getType() == Material.LAVA
                        || feet.getType() == Material.WATER
                        || head.getType() == Material.LAVA
                        || head.getType() == Material.WATER) continue;
                Location safe = source.clone();
                safe.setY(y);
                return safe;
            }
        }
        return null;
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
        if (type == ArcadePortGame.BACON_BRAWL && !baconSourceSpawnOverrides.isEmpty()) {
            List<Location> chosen = new ArrayList<>();
            for (int i = 0; i < Math.max(1, count); i++) {
                chosen.add(baconSourceSpawnOverrides.get(i % baconSourceSpawnOverrides.size()).clone());
            }
            return chosen;
        }
        SourceMapCatalog.MapData sourceMap = sourceMapData.get(type.id());
        if (sourceMap != null && type == ArcadePortGame.BACON_BRAWL) {
            // The archived Mighty Lava Arena stores the 16 free-for-all starts in TEAM_NAME:Green /
            // TEAM_SPAWNS, rather than under DATA_LOCS. Prefer that authored spawn group.
            List<Location> authoredSpawns = sourceMap.teamSpawnLocationsForName("Green");
            if (authoredSpawns.isEmpty()) authoredSpawns = sourceMap.dataLocations("GREEN");
            if (!authoredSpawns.isEmpty()) {
                List<Location> chosen = new ArrayList<>();
                for (int i = 0; i < Math.max(1, count); i++) {
                    chosen.add(authoredSpawns.get(i % authoredSpawns.size()).clone());
                }
                return chosen;
            }
        }
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
        player.setGameMode(GameMode.ADVENTURE);
        UUID playerId = player.getUniqueId();
        session.deathTagRunnerTrailColours.putIfAbsent(playerId,
                deathTagCosmeticChoice(playerId, "trail-colour", "white"));
        session.deathTagRunnerTrailStyles.putIfAbsent(playerId,
                deathTagCosmeticChoice(playerId, "trail-style", "classic"));
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        if (chaser) {
            player.getInventory().addItem(namedItem(Material.IRON_AXE,
                    alphaChaser ? "Alpha Chaser" : "Chaser",
                    alphaChaser ? "Deal 6 damage and resist 4 damage per attack." : "Deal 4 damage and resist 2 damage per attack."));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 4, false, false, true));
            // Source Alpha damage is fixed by PerkDamageSet(6); do not stack a modern Strength buff.
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
        syncDeathTagDisguise(session, player);
        player.updateInventory();
    }

    /**
     * Recreates the legacy Skeleton / Wither Skeleton presentation with a server-native
     * non-interactive proxy. The real player remains the combat entity and is hidden.
     */
    private void syncDeathTagDisguise(Session session, Player player) {
        if (session == null || player == null) return;
        UUID id = player.getUniqueId();
        boolean chaser = session.tagged.contains(id) && session.alive.contains(id);
        boolean alpha = chaser && session.alphaChasers.contains(id);
        Mob disguise = session.deathTagDisguises.get(id);

        if (!chaser) {
            removeDeathTagDisguise(session, id);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            return;
        }

        boolean wrongForm = disguise != null && (alpha
                ? !(disguise instanceof WitherSkeleton)
                : !(disguise instanceof Skeleton) || disguise instanceof WitherSkeleton);
        if (disguise == null || !disguise.isValid() || wrongForm) {
            removeDeathTagDisguise(session, id);
            disguise = alpha
                    ? session.world.spawn(player.getLocation(), WitherSkeleton.class)
                    : session.world.spawn(player.getLocation(), Skeleton.class);
            disguise.setAI(false);
            // Keep the proxy damage-event-capable; onDamage forwards hits to the hidden player.
            disguise.setInvulnerable(false);
            disguise.setSilent(true);
            disguise.setPersistent(true);
            disguise.setRemoveWhenFarAway(false);
            disguise.setGravity(false);
            disguise.setCollidable(false);
            disguise.setCanPickupItems(false);
            disguise.setCustomName(ChatColor.RED + player.getName());
            disguise.setCustomNameVisible(true);
            var equipment = disguise.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(null);
                equipment.setChestplate(null);
                equipment.setLeggings(null);
                equipment.setBoots(null);
                equipment.setItemInMainHand(new ItemStack(Material.IRON_AXE));
                equipment.setItemInMainHandDropChance(0.0F);
                equipment.setHelmetDropChance(0.0F);
                equipment.setChestplateDropChance(0.0F);
                equipment.setLeggingsDropChance(0.0F);
                equipment.setBootsDropChance(0.0F);
            }
            session.deathTagDisguises.put(id, disguise);
            session.entities.add(disguise);
        }

        if (disguise.getLocation().distanceSquared(player.getLocation()) > 0.001D
                || disguise.getLocation().getYaw() != player.getLocation().getYaw()) {
            disguise.teleport(player.getLocation());
        }
        disguise.setCustomName(ChatColor.RED + player.getName());
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false, false));
    }

    private void removeDeathTagDisguise(Session session, UUID playerId) {
        if (session == null || playerId == null) return;
        Mob disguise = session.deathTagDisguises.remove(playerId);
        if (disguise != null) {
            session.entities.remove(disguise);
            if (disguise.isValid()) disguise.remove();
        }
    }

    private void updateDeathTagDisguises(Session session) {
        for (Player player : alivePlayers(session)) syncDeathTagDisguise(session, player);
        for (UUID playerId : new ArrayList<>(session.deathTagDisguises.keySet())) {
            if (!session.alive.contains(playerId) || !session.players.containsKey(playerId)) {
                removeDeathTagDisguise(session, playerId);
            }
        }
    }

    private void updateDeathTagCompass(Session session) {
        List<Player> alive = alivePlayers(session);
        for (Player viewer : alive) {
            boolean viewerChaser = session.tagged.contains(viewer.getUniqueId());
            Player target = alive.stream()
                    .filter(candidate -> session.tagged.contains(candidate.getUniqueId()) != viewerChaser)
                    .min(java.util.Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(viewer.getLocation())))
                    .orElse(null);
            if (target == null) continue;

            boolean hasCompass = false;
            for (ItemStack item : viewer.getInventory().getContents()) {
                if (isDeathTagTrackingCompass(item)) {
                    hasCompass = true;
                    break;
                }
            }
            if (!hasCompass) {
                viewer.getInventory().addItem(namedItem(Material.COMPASS, "Tracking Compass",
                        "Tracks the nearest opposing Death Tag player."));
            }
            viewer.setCompassTarget(target.getLocation());
            if (isDeathTagTrackingCompass(viewer.getInventory().getItemInMainHand())) {
                double distance = Math.sqrt(target.getLocation().distanceSquared(viewer.getLocation()));
                double height = target.getLocation().getY() - viewer.getLocation().getY();
                ChatColor targetColor = session.tagged.contains(target.getUniqueId())
                        ? ChatColor.RED : ChatColor.GREEN;
                viewer.sendActionBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Nearest Target: "
                        + targetColor + target.getName() + ChatColor.WHITE + ChatColor.BOLD + " | Distance: "
                        + targetColor + String.format(java.util.Locale.ROOT, "%.1f", distance)
                        + ChatColor.WHITE + ChatColor.BOLD + " | Height: "
                        + targetColor + String.format(java.util.Locale.ROOT, "%.1f", height));
            }
        }
    }

    private boolean isDeathTagTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    private void setupDeathTagScoreboard(Session session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("deathtag", "dummy",
                ChatColor.GOLD + "DEATH TAG");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.deathTagScoreboard = scoreboard;
        session.deathTagScoreboardObjective = objective;

        session.deathTagRunnerScoreboardLine = scoreboard.registerNewTeam("dtrunners");
        session.deathTagRunnerScoreboardEntry = ChatColor.GREEN.toString();
        session.deathTagRunnerScoreboardLine.addEntry(session.deathTagRunnerScoreboardEntry);
        session.deathTagRunnerScoreboardLine.setPrefix(ChatColor.GREEN.toString());
        objective.getScore(session.deathTagRunnerScoreboardEntry).setScore(3);

        Team blankLine = scoreboard.registerNewTeam("dtblank");
        String blankEntry = ChatColor.BLACK.toString();
        blankLine.addEntry(blankEntry);
        blankLine.setPrefix(" ");
        objective.getScore(blankEntry).setScore(2);

        session.deathTagChaserScoreboardLine = scoreboard.registerNewTeam("dtchasers");
        session.deathTagChaserScoreboardEntry = ChatColor.RED.toString();
        session.deathTagChaserScoreboardLine.addEntry(session.deathTagChaserScoreboardEntry);
        session.deathTagChaserScoreboardLine.setPrefix(ChatColor.RED.toString());
        objective.getScore(session.deathTagChaserScoreboardEntry).setScore(1);

        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateDeathTagScoreboard(session);
    }

    private void updateDeathTagScoreboard(Session session) {
        if (session == null || session.deathTagScoreboardObjective == null) return;
        int chasers = (int) session.tagged.stream().filter(session.alive::contains).count();
        int runners = Math.max(0, session.alive.size() - chasers);
        if (session.deathTagRunnerScoreboardLine != null) {
            session.deathTagRunnerScoreboardLine.setSuffix(ChatColor.WHITE + " " + runners + " Runners");
        }
        if (session.deathTagChaserScoreboardLine != null) {
            session.deathTagChaserScoreboardLine.setSuffix(ChatColor.WHITE + " " + chasers + " Chasers");
        }
    }

    private void updateDeathTagLights(Session session, long now) {
        if (session.deathTagOriginalLightBlocks.isEmpty() || now < session.deathTagNextLightAt) return;
        session.deathTagNextLightAt = now + 1_000L;
        for (Block block : session.deathTagOriginalLightBlocks.keySet()) {
            if (block == null || block.getWorld() != session.world) continue;
            block.setType(ArcadePortRules.deathTagNextLightMaterial(block.getType()), false);
        }
    }

    private String deathTagCosmeticChoice(UUID playerId, String setting, String fallback) {
        java.io.File cosmeticsFile = new java.io.File(plugin.getDataFolder(), "cosmetics.yml");
        if (!cosmeticsFile.isFile()) return fallback;
        org.bukkit.configuration.file.YamlConfiguration cosmetics =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(cosmeticsFile);
        String value = cosmetics.getString("players." + playerId + ".arcade.deathtag." + setting, fallback);
        return value == null || value.isBlank() ? fallback : value.toLowerCase(java.util.Locale.ROOT);
    }

    private Material deathTagTrailGlass(String colour) {
        String materialName = switch (colour == null ? "white" : colour.toLowerCase(java.util.Locale.ROOT)) {
            case "orange" -> "ORANGE_STAINED_GLASS";
            case "magenta" -> "MAGENTA_STAINED_GLASS";
            case "lightblue" -> "LIGHT_BLUE_STAINED_GLASS";
            case "yellow" -> "YELLOW_STAINED_GLASS";
            case "lime" -> "LIME_STAINED_GLASS";
            case "pink" -> "PINK_STAINED_GLASS";
            case "gray" -> "GRAY_STAINED_GLASS";
            case "silver" -> "LIGHT_GRAY_STAINED_GLASS";
            case "cyan" -> "CYAN_STAINED_GLASS";
            case "purple" -> "PURPLE_STAINED_GLASS";
            case "blue" -> "BLUE_STAINED_GLASS";
            case "brown" -> "BROWN_STAINED_GLASS";
            case "green" -> "GREEN_STAINED_GLASS";
            case "red" -> "RED_STAINED_GLASS";
            case "black" -> "BLACK_STAINED_GLASS";
            default -> "WHITE_STAINED_GLASS";
        };
        Material material = Material.matchMaterial(materialName);
        return material == null ? Material.WHITE_STAINED_GLASS : material;
    }

    private void updateDeathTagRunnerTrails(Session session) {
        if (session.ticks % 3 != 0) return;
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (session.tagged.contains(id)) continue;
            Location point = player.getLocation().clone().add(0.0D, 0.15D, 0.0D);
            Material trailGlass = deathTagTrailGlass(session.deathTagRunnerTrailColours.get(id));
            session.world.spawnParticle(Particle.BLOCK, point, 2,
                    0.06D, 0.06D, 0.06D, 0.01D, trailGlass.createBlockData());

            String style = session.deathTagRunnerTrailStyles.getOrDefault(id, "classic");
            switch (style) {
                case "soul" -> session.world.spawnParticle(Particle.WITCH, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "sparks" -> session.world.spawnParticle(Particle.FIREWORK, point, 3,
                        0.12D, 0.12D, 0.12D, 0.02D);
                case "smoke" -> session.world.spawnParticle(Particle.SMOKE, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "ender" -> session.world.spawnParticle(Particle.PORTAL, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "hearts" -> session.world.spawnParticle(Particle.HEART, point, 2,
                        0.12D, 0.12D, 0.12D, 0.01D);
                default -> { /* Classic uses only the selected stained-glass trail. */ }
            }
        }
    }

    private void updateDeathTagConditions(Session session, long now) {
        // Reborn's GameFlagManager enforces HungerSet=20 and resets saturation on FAST updates.
        if (session.ticks % 10 == 0) {
            for (Player player : alivePlayers(session)) {
                player.setFoodLevel(20);
                player.setSaturation(1.0F);
            }
        }
        long tagged = session.tagged.stream().filter(session.alive::contains).count();
        boolean hadRunnerSpeed = session.deathTagRunnerSpeedUnlocked;
        session.deathTagRunnerSpeedUnlocked = ArcadePortRules.deathTagRunnerSpeedUnlocked(
                hadRunnerSpeed, (int) tagged, session.alive.size());
        if (!hadRunnerSpeed && session.deathTagRunnerSpeedUnlocked) {
            announce(session, ChatColor.GREEN + "" + ChatColor.BOLD + "Runners receive Speed I");
        }
        boolean runnersHaveSpeed = session.deathTagRunnerSpeedUnlocked;
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
        updateDeathTagDisguises(session);
        updateDeathTagCompass(session);
        updateDeathTagScoreboard(session);
        updateDeathTagRunnerTrails(session);
        for (Player player : alivePlayers(session)) {
            if (!session.tagged.contains(player.getUniqueId())
                    && "archer".equals(session.deathTagRunnerKits.getOrDefault(player.getUniqueId(), "basher"))) {
                Long lastArrow = session.deathTagLastArrowAt.get(player.getUniqueId());
                if (lastArrow == null || now - lastArrow >= ArcadePortRules.DEATH_TAG_ARCHER_ARROW_INTERVAL_MILLIS) {
                    session.deathTagLastArrowAt.put(player.getUniqueId(), now);
                    int arrows = inventoryCount(player, Material.ARROW);
                    if (arrows < ArcadePortRules.DEATH_TAG_ARCHER_MAX_ARROWS) {
                        player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow", "Replenished by the Archer kit."));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6F, 1.3F);
                    }
                }
            }
        }
    }

    private void handleDeathTagSoloPracticeHit(Session session, Player target, Zombie chaser) {
        if (active != session || target == null || !target.isOnline()
                || !session.alive.contains(target.getUniqueId())) return;
        long now = System.currentTimeMillis();
        long readyAt = session.deathTagSoloChaserHitReadyAt.getOrDefault(target.getUniqueId(), 0L);
        if (now < readyAt) return;
        session.deathTagSoloChaserHitReadyAt.put(target.getUniqueId(), now + 1_500L);

        double damage = ArcadePortRules.deathTagChaserDamage(false);
        if (target.getHealth() <= damage) target.setHealth(target.getMaxHealth());
        else target.setHealth(target.getHealth() - damage);

        Vector away = target.getLocation().toVector().subtract(chaser.getLocation().toVector());
        if (away.lengthSquared() > 0.001D) target.setVelocity(away.normalize().multiply(0.45D).setY(0.18D));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 0.85F);
        target.sendActionBar(ChatColor.YELLOW + "PRACTICE HIT " + ChatColor.GRAY + "| Your Death Tag run continues.");
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
        makeDeathTagChaser(session, player, reason, null);
    }

    private void makeDeathTagChaser(Session session, Player player, String reason, Player killer) {
        if (active != session || player == null || !session.alive.contains(player.getUniqueId())) return;

        // A Chaser who is defeated remains a Chaser. Respawn that role rather than
        // sending them through runner-to-Chaser conversion again.
        if (session.tagged.contains(player.getUniqueId())) {
            awardDeathTagKill(session, killer);
            respawnDeathTagChaser(session, player);
            return;
        }

        boolean forcedAlpha = reason != null && reason.contains("forced by the time limit");
        if (!forcedAlpha) session.deathTagDeathsByPlayer.merge(player.getUniqueId(), 1, Integer::sum);
        session.tagged.add(player.getUniqueId());
        if (forcedAlpha) session.alphaChasers.add(player.getUniqueId());
        else session.alphaChasers.remove(player.getUniqueId());
        awardDeathTagKill(session, killer);
        if (player.isOnline()) {
            // Source forced promotions change the player's role in place. Natural conversions
            // likewise remain at the runner's death location, as in DeathTag.RespawnPlayer().
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setFireTicks(0);
            player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            giveDeathTagKit(session, player, true, session.alphaChasers.contains(player.getUniqueId()));
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.0F, 1.0F);
            player.sendMessage(ChatColor.RED + (forcedAlpha ? "You are now an Alpha Chaser!" : "You are now a Chaser!"));
            session.world.spawnParticle(Particle.FLAME, player.getLocation().add(0.0D, 1.0D, 0.0D),
                    18, 0.35D, 0.5D, 0.35D, 0.04D);
        }
        announce(session, ChatColor.RED + player.getName() + ChatColor.GRAY + " became "
                + (forcedAlpha ? "an Alpha Chaser" : "a Chaser") + " (" + reason + ").");
    }

    /** Default Minecraft-style melee knockback for Chasers and the perkless NullKit. */
    private void applyDeathTagDefaultMeleeKnockback(Player attacker, Player target, boolean alphaTarget) {
        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.001D) return;
        double multiplier = ArcadePortRules.deathTagKnockbackMultiplier(alphaTarget);
        Vector velocity = target.getVelocity().multiply(0.5D)
                .add(direction.normalize().multiply(0.4D * multiplier));
        velocity.setY(Math.max(velocity.getY(), 0.36D * multiplier));
        target.setVelocity(velocity);
    }

    private void awardDeathTagKill(Session session, Player killer) {
        if (killer == null || !killer.isOnline() || !session.scores.containsKey(killer.getUniqueId())) return;
        int kills = session.scores.merge(killer.getUniqueId(), 1, Integer::sum);
        killer.sendMessage(ChatColor.GREEN + "Death Tag kill! " + kills + " kills.");
    }

    private void recordDeathTagChaserKill(Session session, Player killer) {
        if (session == null || killer == null || !killer.isOnline()) return;
        UUID id = killer.getUniqueId();
        int chaserKills = session.deathTagChaserKillsByPlayer.merge(id, 1, Integer::sum);
        if (ArcadePortRules.shouldAwardDeathTagComeAtMeBro(chaserKills)) {
            session.deathTagComeAtMeBroAwards.merge(id, 1, Integer::sum);
        }
    }

    private Location deathTagChaserRespawnSpawn(Session session, Player player) {
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null && !mapData.spawnLocations().isEmpty()) {
            List<Location> spawns = mapData.spawnLocations();
            return spawns.get(ThreadLocalRandom.current().nextInt(spawns.size())).clone();
        }
        return spawnFor(session, player);
    }

    private void respawnDeathTagChaser(Session session, Player player) {
        if (active != session || player == null || !player.isOnline()) return;
        session.deathTagDeathsByPlayer.merge(player.getUniqueId(), 1, Integer::sum);
        player.teleport(deathTagChaserRespawnSpawn(session, player));
        player.leaveVehicle();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        giveDeathTagKit(session, player, true, session.alphaChasers.contains(player.getUniqueId()));
        player.sendMessage(ChatColor.RED + "You respawned as a Chaser.");
    }

    private void handleDeathTagArcherArrowHit(Session session, Arrow arrow, Player shooter, Player target) {
        UUID shooterId = shooter.getUniqueId();
        UUID targetId = target.getUniqueId();
        if (active != session || !session.alive.contains(shooterId) || !session.alive.contains(targetId)
                || session.tagged.contains(shooterId)
                || !"archer".equals(session.deathTagRunnerKits.getOrDefault(shooterId, "basher"))
                || !session.tagged.contains(targetId)) return;

        double baseDamage = session.deathTagProjectileDamage.getOrDefault(arrow.getUniqueId(),
                ArcadePortRules.deathTagRunnerDamage("archer", true));
        session.deathTagProjectileDamage.remove(arrow.getUniqueId());
        double damage = ArcadePortRules.deathTagDamageAfterIronSkin(baseDamage,
                session.alphaChasers.contains(targetId));
        if (damage > 0.0D) {
            if (ArcadePortRules.deathTagHitIsLethal(target.getHealth(), damage)) {
                respawnDeathTagChaser(session, target);
                awardDeathTagKill(session, shooter);
                recordDeathTagChaserKill(session, shooter);
                checkRoundEnd(session);
                return;
            }
            target.setHealth(target.getHealth() - damage);
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.05F);
        }
        Vector direction = arrow.getVelocity();
        if (direction.lengthSquared() < 0.001D) {
            direction = target.getLocation().toVector().subtract(shooter.getLocation().toVector());
        }
        if (direction.lengthSquared() > 0.001D) {
            double multiplier = ArcadePortRules.deathTagArcherArrowKnockbackMultiplier(
                    session.alphaChasers.contains(targetId));
            target.setVelocity(direction.normalize().multiply(multiplier));
        }
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
                for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) player.removePotionEffect(effect.getType());
                String kit = session.baconKits.getOrDefault(player.getUniqueId(), "el-muchacho-pigo");
                player.getInventory().setArmorContents(new ItemStack[4]);
                player.getInventory().setItemInOffHand(null);
                player.getInventory().setItem(0, namedItem(Material.IRON_AXE, "Kit Ability",
                        "Right-click to spend your damage meter."));
                if ("mama-piggles".equals(kit)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false, false));
                }
                updateBaconMeterItem(session, player);
                player.sendActionBar(ChatColor.GOLD + "Land hits to charge your kit meter.");
                break;
            }
            case BOMB_LOBBERS: {
                String kit = session.bombLobbersKits.getOrDefault(player.getUniqueId(), "jumper");
                player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT", "Click to throw at enemy players.", 1));
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
                // PerkDoubleJump enables flight-toggle readiness for every kit while grounded.
                player.setAllowFlight(false);
                player.setFlying(false);
                break;
            }
            case DEATH_TAG:
                giveDeathTagKit(session, player, false, false);
                break;
            case DRAGON_ESCAPE: {
                UUID id = player.getUniqueId();
                // Reborn grants invisibility to every runner when their Dragon Escape kit is given.
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                        Integer.MAX_VALUE, 0, false, false, false));
                player.setExp(0.99F);
                String kit = session.dragonEscapeKits.getOrDefault(id, "jumper");
                int leaps = session.dragonEscapeLeapUsesRemaining.getOrDefault(id, ArcadePortRules.dragonEscapeLeapUses(kit));
                if (!"digger".equals(kit)) {
                    player.getInventory().addItem(namedItem(Material.IRON_AXE, "Leap",
                            "Right-click to leap forward (" + leaps + " uses remaining).", Math.max(1, leaps)));
                }
                if ("disruptor".equals(kit)) {
                    player.getInventory().addItem(namedItem(Material.TNT, "Mini Explosive",
                            "Right-click to throw a trip mine. Recharges every 8 seconds.", 1));
                } else if ("warper".equals(kit)) {
                    player.getInventory().addItem(namedItem(Material.ENDER_PEARL, "Warp",
                            "Right-click to warp to the next-ranked racer ahead. One use."));
                } else if ("digger".equals(kit)) {
                    int digs = session.dragonEscapeDiggerUsesRemaining.getOrDefault(id, ArcadePortRules.DRAGON_ESCAPE_DIGGER_MAX_USES);
                    player.getInventory().addItem(namedItem(Material.DIAMOND_PICKAXE, "Digger Pickaxe",
                            "Left-click course blocks to recover and place as shortcuts (" + digs + " digs left).",
                            Math.max(1, digs)));
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
            if (ArcadePortRules.isQuiverReboundTargetInRange(distance)
                    && (nearest == null || distance < nearestDistance)) {
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
        UUID id = player.getUniqueId();
        int team = session.teams.getOrDefault(id, 0);
        Color teamColor = ArcadePortRules.bombLobbersTeamColor(team);
        ChatColor chatColor = team == 1 ? ChatColor.AQUA : ChatColor.RED;
        ItemStack[] armor;

        if ("armorer".equals(kit)) {
            armor = new ItemStack[] {
                    new ItemStack(Material.IRON_BOOTS), new ItemStack(Material.IRON_LEGGINGS),
                    new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_HELMET)
            };
        } else if ("pitcher".equals(kit)) {
            armor = new ItemStack[] {
                    new ItemStack(Material.CHAINMAIL_BOOTS), new ItemStack(Material.CHAINMAIL_LEGGINGS),
                    new ItemStack(Material.CHAINMAIL_CHESTPLATE), new ItemStack(Material.CHAINMAIL_HELMET)
            };
        } else if (ArcadePortRules.bombLobbersKitUsesTeamArmor(kit)) {
            // The source TeamArmorModule runs during PlayerKitGiveEvent for Jumper/Waller.
            armor = new ItemStack[] {
                    bombLobbersTeamArmorPiece(Material.LEATHER_BOOTS, teamColor, chatColor),
                    bombLobbersTeamArmorPiece(Material.LEATHER_LEGGINGS, teamColor, chatColor),
                    bombLobbersTeamArmorPiece(Material.LEATHER_CHESTPLATE, teamColor, chatColor),
                    bombLobbersTeamArmorPiece(Material.LEATHER_HELMET, teamColor, chatColor)
            };
        } else {
            armor = new ItemStack[4];
        }
        player.getInventory().setArmorContents(armor);

        // TeamArmorModule also supplies an unbreakable, coloured team chestplate in hotbar slot 9.
        ItemStack teamBadge = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta badgeMeta = (LeatherArmorMeta) teamBadge.getItemMeta();
        if (badgeMeta != null) {
            badgeMeta.setColor(teamColor);
            badgeMeta.setDisplayName(chatColor + "" + ChatColor.BOLD
                    + ArcadePortRules.bombLobbersTeamBadgeName(team));
            badgeMeta.setUnbreakable(true);
            teamBadge.setItemMeta(badgeMeta);
        }
        player.getInventory().setItem(ArcadePortRules.BOMB_LOBBERS_TEAM_BADGE_SLOT, teamBadge);
    }

    private boolean isBombLobbersTeamLockedItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String name = ChatColor.stripColor(meta.getDisplayName());
        // The source badge's displayed name is the configured map team name (e.g. Red/Blue),
        // whereas all four pieces of actual team armour are named Team Armor.
        return "Team Armor".equals(name)
                || (item.getType() == Material.LEATHER_CHESTPLATE && meta.isUnbreakable());
    }

    private ItemStack bombLobbersTeamArmorPiece(Material material, Color teamColor, ChatColor chatColor) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(teamColor);
            meta.setDisplayName(chatColor + "" + ChatColor.BOLD + "Team Armor");
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private double bombLobbersDamageAfterArmor(Player target, double damage) {
        var armorAttribute = target.getAttribute(Attribute.ARMOR);
        double armorPoints = armorAttribute == null ? 0.0D : armorAttribute.getValue();
        return ArcadePortRules.bombLobbersDamageAfterArmor(damage, armorPoints);
    }

    private void setMicroTeamArmor(Session session, Player player) {
        int team = session.teams.getOrDefault(player.getUniqueId(), 0);
        Color teamColor = microTeamDyeColor(team);
        ChatColor chatColor = microTeamChatColor(team);
        ItemStack[] armor = new ItemStack[] {
                microTeamArmorPiece(Material.LEATHER_BOOTS, teamColor, chatColor),
                microTeamArmorPiece(Material.LEATHER_LEGGINGS, teamColor, chatColor),
                microTeamArmorPiece(Material.LEATHER_CHESTPLATE, teamColor, chatColor),
                microTeamArmorPiece(Material.LEATHER_HELMET, teamColor, chatColor)
        };
        player.getInventory().setArmorContents(armor);
        ItemStack teamBadge = microTeamArmorPiece(Material.LEATHER_CHESTPLATE, teamColor, chatColor);
        ItemMeta badgeMeta = teamBadge.getItemMeta();
        if (badgeMeta != null) {
            badgeMeta.setDisplayName(chatColor + "" + ChatColor.BOLD + "Team " + microTeamName(team));
            badgeMeta.setLore(List.of(ChatColor.GRAY + "Your team colour.", ChatColor.DARK_GRAY + "Cannot be moved or equipped."));
            badgeMeta.getPersistentDataContainer().set(microTeamItemKey, PersistentDataType.STRING, "hotbar");
            teamBadge.setItemMeta(badgeMeta);
        }
        player.getInventory().setItem(8, teamBadge);
    }

    private ItemStack microTeamArmorPiece(Material material, Color colour, ChatColor chatColor) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(colour);
            meta.setDisplayName(chatColor + "" + ChatColor.BOLD + "Team Armor");
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(microTeamItemKey, PersistentDataType.STRING, "armour");
            item.setItemMeta(meta);
        }
        return item;
    }

    private Color microTeamDyeColor(int team) {
        return switch (team) {
            case 0 -> Color.RED;
            case 1 -> Color.AQUA;
            case 2 -> Color.LIME;
            default -> Color.YELLOW;
        };
    }

    private ChatColor microTeamChatColor(int team) {
        return switch (team) {
            case 0 -> ChatColor.RED;
            case 1 -> ChatColor.AQUA;
            case 2 -> ChatColor.GREEN;
            default -> ChatColor.YELLOW;
        };
    }

    private String microTeamName(int team) {
        return switch (team) {
            case 0 -> "Red";
            case 1 -> "Blue";
            case 2 -> "Green";
            default -> "Yellow";
        };
    }

    private boolean isMicroTeamLockedItem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(microTeamItemKey, PersistentDataType.STRING);
    }

    private YamlConfiguration loadMicroBattlesCosmeticPreferences() {
        var preferencesPlugin = Bukkit.getPluginManager().getPlugin("TestplexModernArcade");
        if (preferencesPlugin == null || !preferencesPlugin.isEnabled()) return new YamlConfiguration();
        File preferencesFile = new File(preferencesPlugin.getDataFolder(), "player-preferences.yml");
        return YamlConfiguration.loadConfiguration(preferencesFile);
    }

    private String microBattlesColourSelection(YamlConfiguration preferences, UUID playerId, String lineId) {
        if (preferences == null) return ArcadePortRules.MICRO_BATTLES_DEFAULT_COSMETIC_COLOUR;
        String selected = preferences.getString("players." + playerId + ".games.micro-battles.cosmetics."
                + lineId + ".selected", ArcadePortRules.MICRO_BATTLES_DEFAULT_COSMETIC_COLOUR);
        String normalized = ArcadePortRules.normalizeMicroBattlesColour(selected);
        return normalized == null ? ArcadePortRules.MICRO_BATTLES_DEFAULT_COSMETIC_COLOUR : normalized;
    }

    private int microBarrierTeam(Session session, Block barrier) {
        int nearestTeam = 0;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int team = 0; team < 4; team++) {
            Location center = microTeamCenter(session, team);
            double distance = horizontalDistanceSquared(barrier.getLocation(), center);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTeam = team;
            }
        }
        return nearestTeam;
    }

    /** Sends the chosen glass only to its owner; the server world stays unchanged for everyone else. */
    private void applyMicroMiddleGlassCosmetic(Session session, Player player) {
        UUID playerId = player.getUniqueId();
        int team = session.teams.getOrDefault(playerId, -1);
        if (team < 0 || !session.microBarrierActive || !session.alive.contains(playerId)) return;
        Material glass = ArcadePortRules.microBattlesGlassMaterial(
                session.microGlassColours.getOrDefault(playerId, ArcadePortRules.MICRO_BATTLES_DEFAULT_COSMETIC_COLOUR));
        Set<Block> personalBlocks = new HashSet<>();
        for (Block barrier : session.microBarrierBlocks) {
            if (microBarrierTeam(session, barrier) != team
                    || !session.world.isChunkLoaded(barrier.getX() >> 4, barrier.getZ() >> 4)) continue;
            player.sendBlockChange(barrier.getLocation(), glass.createBlockData());
            personalBlocks.add(barrier);
        }
        if (!personalBlocks.isEmpty()) session.microPersonalGlassBlocks.put(playerId, personalBlocks);
    }

    private void restoreMicroMiddleGlassCosmetic(Session session, Player player) {
        if (session == null || player == null || !player.isOnline()) return;
        Set<Block> personalBlocks = session.microPersonalGlassBlocks.remove(player.getUniqueId());
        if (personalBlocks == null) return;
        for (Block block : personalBlocks) {
            if (block != null && block.getWorld() == session.world
                    && session.world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                player.sendBlockChange(block.getLocation(), block.getBlockData());
            }
        }
    }

    /**
     * Shared floating warning over players who are within the border warning distance.
     * The TextDisplay is a world entity, so nearby players all see the same warning.
     */
    private void updateMicroBorderWarnings(Session session) {
        if (active != session || session.type != ArcadePortGame.MICRO_BATTLES) {
            removeAllMicroBorderWarnings(session);
            return;
        }
        WorldBorder border = session.world.getWorldBorder();
        Location center = border.getCenter();
        Set<UUID> showing = new HashSet<>();
        for (Player player : alivePlayers(session)) {
            Location location = player.getLocation();
            boolean nearBorder = ArcadePortRules.shouldShowMicroBattlesBorderParticles(
                    location.getX(), location.getZ(), center.getX(), center.getZ(), border.getSize());
            if (!nearBorder) continue;

            UUID playerId = player.getUniqueId();
            showing.add(playerId);
            TextDisplay warning = session.microBorderWarnings.get(playerId);
            Location aboveHead = location.clone().add(0.0D, 2.35D, 0.0D);
            if (warning == null || !warning.isValid()) {
                warning = session.world.spawn(aboveHead, TextDisplay.class);
                warning.setText(ChatColor.GOLD + "" + ChatColor.BOLD + "⚠ BORDER");
                warning.setBillboard(Display.Billboard.CENTER);
                warning.setAlignment(TextDisplay.TextAlignment.CENTER);
                warning.setSeeThrough(true);
                warning.setShadowed(true);
                warning.setDefaultBackground(false);
                warning.setLineWidth(120);
                warning.setPersistent(false);
                warning.setGravity(false);
                warning.setInvulnerable(true);
                warning.setSilent(true);
                warning.setViewRange(1.2F);
                session.microBorderWarnings.put(playerId, warning);
            } else {
                // Follow every game tick so the label stays directly above a moving player.
                warning.teleport(aboveHead);
            }
        }

        for (UUID playerId : new HashSet<>(session.microBorderWarnings.keySet())) {
            if (!showing.contains(playerId)) removeMicroBorderWarning(session, playerId);
        }
    }

    private void removeMicroBorderWarning(Session session, UUID playerId) {
        if (session == null || playerId == null) return;
        TextDisplay warning = session.microBorderWarnings.remove(playerId);
        if (warning != null && warning.isValid()) warning.remove();
    }

    private void removeAllMicroBorderWarnings(Session session) {
        if (session == null) return;
        for (UUID playerId : new HashSet<>(session.microBorderWarnings.keySet())) {
            removeMicroBorderWarning(session, playerId);
        }
    }

    /** The source cue is a shared vertical wall marker, positioned at the nearest world-border face. */
    private void updateMicroBattlesBorderParticles(Session session) {
        if (active != session || session.type != ArcadePortGame.MICRO_BATTLES
                || session.ticks % ArcadePortRules.MICRO_BATTLES_BORDER_PARTICLE_INTERVAL_TICKS != 0) return;
        WorldBorder border = session.world.getWorldBorder();
        Location center = border.getCenter();
        double half = border.getSize() / 2.0D;
        for (Player player : alivePlayers(session)) {
            Location location = player.getLocation();
            double x = location.getX(), z = location.getZ();
            if (!ArcadePortRules.shouldShowMicroBattlesBorderParticles(
                    x, z, center.getX(), center.getZ(), border.getSize())) continue;
            double xFaceDistance = half - Math.abs(x - center.getX());
            double zFaceDistance = half - Math.abs(z - center.getZ());
            boolean xFace = xFaceDistance <= zFaceDistance;
            double edgeX = center.getX() + (x >= center.getX() ? half - 0.06D : -half + 0.06D);
            double edgeZ = center.getZ() + (z >= center.getZ() ? half - 0.06D : -half + 0.06D);
            double along = xFace ? Math.max(center.getZ() - half + 0.08D,
                    Math.min(center.getZ() + half - 0.08D, z))
                    : Math.max(center.getX() - half + 0.08D, Math.min(center.getX() + half - 0.08D, x));
            Color colour = ArcadePortRules.microBattlesParticleColour(
                    session.microBorderColours.getOrDefault(player.getUniqueId(),
                            ArcadePortRules.MICRO_BATTLES_DEFAULT_COSMETIC_COLOUR));
            Particle.DustOptions dust = new Particle.DustOptions(colour, 1.2F);
            // Three points make the border obvious across the player's full height, not just on the floor.
            for (double height : new double[] {0.15D, 0.95D, 1.75D}) {
                Location cue = xFace
                        ? new Location(session.world, edgeX, location.getY() + height, along)
                        : new Location(session.world, along, location.getY() + height, edgeZ);
                session.world.spawnParticle(Particle.DUST, cue, 2, 0.015D, 0.025D, 0.015D, 0.0D, dust);
            }
        }
    }

    private boolean isMicroTrackingCompass(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
    }

    private void giveMicroTrackingCompass(Player player) {
        if (player == null || !player.isOnline()) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMicroTrackingCompass(item)) return;
        }
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY + "Left-click to teleport to the nearest opponent.",
                    ChatColor.GRAY + "Right-click to choose an opponent."));
            compass.setItemMeta(meta);
        }
        player.getInventory().addItem(compass);
    }

    private Player nearestMicroSpectateTarget(Session session, Player spectator) {
        int spectatorTeam = session.teams.getOrDefault(spectator.getUniqueId(), -1);
        return alivePlayers(session).stream()
                .filter(target -> !target.getUniqueId().equals(spectator.getUniqueId()))
                .filter(target -> session.teams.getOrDefault(target.getUniqueId(), -1) != spectatorTeam)
                .min(java.util.Comparator.comparingDouble(target -> target.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
    }

    private String microCompassMenuTitle() {
        return ChatColor.DARK_GREEN + "Micro Battles Tracker";
    }

    private boolean isMicroCompassMenuTitle(String title) {
        return microCompassMenuTitle().equals(title);
    }

    private void openMicroCompassMenu(Session session, Player spectator) {
        if (active != session || session.type != ArcadePortGame.MICRO_BATTLES) return;
        List<Player> targets = alivePlayers(session).stream()
                .filter(target -> session.teams.getOrDefault(target.getUniqueId(), -1)
                        != session.teams.getOrDefault(spectator.getUniqueId(), -1))
                .sorted(java.util.Comparator.comparingDouble(
                        target -> target.getLocation().distanceSquared(spectator.getLocation())))
                .toList();
        Inventory inventory = Bukkit.createInventory(null, 54, microCompassMenuTitle());
        Map<Integer, UUID> slots = new HashMap<>();
        int slot = 0;
        for (Player target : targets) {
            if (slot >= 54) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(target);
                int team = session.teams.getOrDefault(target.getUniqueId(), -1);
                meta.setDisplayName(microTeamChatColor(team) + target.getName());
                meta.setLore(List.of(ChatColor.GRAY + "Team " + microTeamName(team),
                        ChatColor.YELLOW + "Left-click: teleport",
                        ChatColor.YELLOW + "Right-click: spectate"));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            slots.put(slot, target.getUniqueId());
            slot++;
        }
        if (targets.isEmpty()) inventory.setItem(22, namedItem(Material.BARRIER,
                "No opponents alive", "There are no opposing players to track."));
        session.microCompassMenuTargets.put(spectator.getUniqueId(), slots);
        spectator.openInventory(inventory);
    }

    private void updateMicroSpectatorCompass(Session session) {
        if (active != session || session.ticks % 4 != 0) return;
        for (Player player : session.players.values()) {
            if (player == null || !player.isOnline() || session.alive.contains(player.getUniqueId())) continue;
            giveMicroTrackingCompass(player);
            Player target = nearestMicroSpectateTarget(session, player);
            if (target != null) {
                player.setCompassTarget(target.getLocation());
                if (isMicroTrackingCompass(player.getInventory().getItemInMainHand())) {
                    double distance = Math.sqrt(player.getLocation().distanceSquared(target.getLocation()));
                    double height = target.getLocation().getY() - player.getLocation().getY();
                    player.sendActionBar(ChatColor.WHITE + "Nearest Target: "
                            + microTeamChatColor(session.teams.getOrDefault(target.getUniqueId(), -1))
                            + target.getName() + ChatColor.WHITE + " | Distance: "
                            + String.format(java.util.Locale.ROOT, "%.1f", distance)
                            + " | Height: " + String.format(java.util.Locale.ROOT, "%+.1f", height));
                }
            }
        }
    }

    /** Local mirror of Reborn Last2Tracker; the live Mineplex mission service is not connected here. */
    private void updateMicroLastTwoMission(Session session) {
        if (active != session || session.microLastTwoAwarded) return;
        Set<Integer> aliveTeams = new HashSet<>();
        for (UUID id : session.alive) aliveTeams.add(session.teams.getOrDefault(id, -1));
        if (!ArcadePortRules.shouldAwardMicroBattlesLastTwo(aliveTeams.size())) return;
        session.microLastTwoAwarded = true;
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            awardMicroBattlesAchievement(player, MicroBattlesAchievement.LAST_TWO.configKey(id),
                    "Last Two — you survived until only two teams remained.");
        }
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
        Material visibleWeapon = ArcadePortRules.paintballWeaponMaterial(
                safeKit, paintballCosmeticOption(player.getUniqueId(), "weapon"));
        player.getInventory().setItem(0, namedItem(visibleWeapon == null ? weapon : visibleWeapon, title, lore));
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
            case "machine-gun" -> ArcadePortRules.PAINTBALL_MACHINE_GUN_FIRE_COOLDOWN_MILLIS;
            default -> 500L;
        };
        Long lastShot = session.paintballLastShotAt.get(playerId);
        if (lastShot != null && now - lastShot < cooldown) return;
        if ("machine-gun".equals(kit) && !ArcadePortRules.canFirePaintballMachineGun(player.getExp())) {
            player.sendMessage(ChatColor.RED + "Machine gun overheated. Release fire and let the gauge cool.");
            return;
        }
        session.paintballLastShotAt.put(playerId, now);
        if ("machine-gun".equals(kit)) {
            player.setExp((float) ArcadePortRules.paintballMachineGunHeatAfterShot(player.getExp()));
        }

        if ("sniper".equals(kit)) {
            int paintAmount = ArcadePortRules.paintballPaintAmount(kit, true, player.getExp());
            Arrow arrow = player.launchProjectile(Arrow.class);
            arrow.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(15.0D));
            session.entities.add(arrow);
            session.paintProjectileDamage.put(arrow.getUniqueId(), paintAmount);
            trackPaintballProjectileCosmetic(session, player, arrow);
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
            trackPaintballProjectileCosmetic(session, player, projectile);
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
                if (player.isSneaking() && "sniper".equals(paintballKitForItem(held))) {
                    player.setExp(Math.min(0.999F, player.getExp() + 0.03332F));
                } else if (player.getExp() > 0.0F && !player.isSneaking()) {
                    player.setExp(0.0F);
                }
            } else if ("machine-gun".equals(kit)) {
                Long lastShot = session.paintballLastShotAt.get(id);
                if (lastShot == null || now - lastShot >= ArcadePortRules.PAINTBALL_MACHINE_GUN_COOL_RECHARGE_MILLIS) {
                    player.setExp((float) ArcadePortRules.coolPaintballMachineGunHeat(player.getExp()));
                }
            } else if (player.getExp() != 0.0F) {
                player.setExp(0.0F);
            }
            if (session.ticks % 10 == 0) updatePaintballWeaponSkin(session, player);
        }
    }

    /**
     * Read the selected option from TestplexModernArcade without a hard dependency
     * on that optional cosmetics/UI plugin. The game falls back to its first
     * catalogue choice when the UI plugin is absent.
     */
    private String paintballCosmeticOption(UUID playerId, String group) {
        String fallback = "weapon".equals(group) ? "chrome-marker" : "colourburst";
        org.bukkit.plugin.Plugin cosmeticsPlugin = Bukkit.getPluginManager().getPlugin("TestplexModernArcade");
        if (cosmeticsPlugin == null || !cosmeticsPlugin.isEnabled()) return fallback;
        try {
            Object selected = cosmeticsPlugin.getClass()
                    .getMethod("getSelectedCosmeticOption", UUID.class, String.class, String.class)
                    .invoke(cosmeticsPlugin, playerId, "paintball", group);
            return selected instanceof String value && !value.isBlank() ? value : fallback;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().fine("Paintball cosmetic preference bridge unavailable: " + exception.getMessage());
            return fallback;
        }
    }

    private void updatePaintballWeaponSkin(Session session, Player player) {
        ItemStack held = player.getInventory().getItem(0);
        String kit = paintballKitForItem(held);
        if (kit == null) return;
        Material desired = ArcadePortRules.paintballWeaponMaterial(
                kit, paintballCosmeticOption(player.getUniqueId(), "weapon"));
        if (desired == null || held.getType() == desired) return;
        ItemStack replacement = held.clone();
        replacement.setType(desired);
        player.getInventory().setItem(0, replacement);
    }

    private void trackPaintballProjectileCosmetic(Session session, Player shooter, Projectile projectile) {
        if (session == null || shooter == null || projectile == null) return;
        int team = session.teams.getOrDefault(shooter.getUniqueId(), 0);
        Color colour = team == 0 ? Color.RED : Color.AQUA;
        String style = paintballCosmeticOption(shooter.getUniqueId(), "pellet");
        session.paintballProjectileCosmetics.put(projectile.getUniqueId(),
                new PaintballProjectileVisual(projectile, style, colour));
    }

    /**
     * Broadcast the selected pellet skin as world particles. World.spawnParticle
     * is intentional: spectators and opponents see the same cosmetic trail as
     * the shooter. It never changes projectile velocity, hitboxes or damage.
     */
    private void updatePaintballPelletCosmetics(Session session) {
        for (Map.Entry<UUID, PaintballProjectileVisual> entry
                : new ArrayList<>(session.paintballProjectileCosmetics.entrySet())) {
            PaintballProjectileVisual visual = entry.getValue();
            Projectile projectile = visual.projectile();
            if (projectile == null || !projectile.isValid() || projectile.getWorld() != session.world) {
                session.paintballProjectileCosmetics.remove(entry.getKey());
                continue;
            }
            Location point = projectile.getLocation();
            Particle.DustOptions dust = new Particle.DustOptions(visual.teamColor(), 1.35F);
            switch (visual.style()) {
                case "paint-drip" -> {
                    session.world.spawnParticle(Particle.DUST, point.clone().add(0.0D, -0.08D, 0.0D),
                            1, 0.008D, 0.008D, 0.008D, 0.0D, dust);
                    session.world.spawnParticle(Particle.DUST, point.clone().add(0.0D, -0.16D, 0.0D),
                            1, 0.008D, 0.008D, 0.008D, 0.0D, dust);
                }
                case "stencil-stamp" -> {
                    for (double dx : new double[] {-0.035D, 0.035D}) {
                        for (double dz : new double[] {-0.035D, 0.035D}) {
                            session.world.spawnParticle(Particle.DUST, point.clone().add(dx, 0.0D, dz),
                                    1, 0.0D, 0.0D, 0.0D, 0.0D, dust);
                        }
                    }
                }
                default -> session.world.spawnParticle(Particle.DUST, point, 4,
                        0.045D, 0.045D, 0.045D, 0.0D, dust);
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
        if (session.endsAt > 0L && now >= session.endsAt) {
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
            updateMicroBorderWarnings(session);
            updateMicroBattlesBorderParticles(session);
            updateMicroSpectatorCompass(session);
            if (session.ticks % 20 == 0) updateMicroLastTwoMission(session);
            enforceMicroBattlesHunger(session);
            updateMicroBattlesArrows(session, now);
            updateMicroCrumble(session, now);
        }
        if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER) updateQuiverKit(session, now);
        if (session.type == ArcadePortGame.DRAGONS) updateTestDragons(session, now);
        if (session.type == ArcadePortGame.DEATH_TAG) {
            if (session.participantsAtStart > 1) updateDeathTagChasers(session, now);
            else updateDeathTagConditions(session, now);
            updateDeathTagLights(session, now);
        }
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
            updateDragonEscapeSafeLocations(session);
            updateDragonEscapeTunnelBlocks(session);
            updateDragonEscapeResources(session, now);
            updateDragonEscapeMines(session);
            updateDragonEscapeChase(session);
            if (active == session) {
                recordDragonEscapeGhostFrames(session, now);
                updateDragonEscapeGhosts(session, now);
            }
        }
        if (session.type == ArcadePortGame.SHEEP_QUEST) {
            updateSheepQuest(session);
            if (now >= lastSheepSpawn && session.sheep.size() < 12) {
                spawnSheep(session);
                lastSheepSpawn = now + ArcadePortRules.SHEEP_QUEST_SPAWN_INTERVAL_MILLIS;
            }
        }

        if (session.type == ArcadePortGame.PAINTBALL) {
            updatePaintballKitState(session, now);
            updatePaintballPelletCosmetics(session);
        }
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
        if (damage <= 0) return;

        if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
            // Reborn's GameFlagManager checks every player on its FASTER cadence
            // and applies an independent 500ms Recharge per player.
            for (Player player : new ArrayList<>(alivePlayers(session))) {
                UUID id = player.getUniqueId();
                long nextCheckAt = session.dragonEscapeWaterDamageNextAt.getOrDefault(id, 0L);
                if (now < nextCheckAt) continue;
                session.dragonEscapeWaterDamageNextAt.put(id,
                        now + ArcadePortRules.DRAGON_ESCAPE_WATER_DAMAGE_INTERVAL_MILLIS);
                Block feet = player.getLocation().getBlock();
                Material lower = feet.getType();
                Material upper = feet.getRelative(BlockFace.UP).getType();
                if (lower != Material.WATER && upper != Material.WATER) continue;
                if (player.getHealth() <= damage) {
                    eliminate(session, player, null, "was lost to the water");
                    if (active != session) return;
                } else {
                    player.setHealth(player.getHealth() - damage);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.8F, 1.0F);
                }
            }
            return;
        }

        if (now - session.lastWorldWaterDamageAt < 1_000L) return;
        session.lastWorldWaterDamageAt = now;
        for (Player player : new ArrayList<>(alivePlayers(session))) {
            if (!player.isOnline()) continue;
            boolean inWater = player.isInWater();
            if (!inWater) continue;
            if (session.type == ArcadePortGame.BOMB_LOBBERS) session.bombPlayersDamaged.add(player.getUniqueId());
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
        // Advance thrown Bacon Blast items each tick; hunger/escalation stay on the slow cadence.
        updateBaconBlastProjectiles(session, now);
        if (active != session) return;
        // Source UpdateType.SLOW runs every 4 seconds. Keep ability contact/expiry checks
        // on every tick, but gate hunger and knockback escalation on that slower cadence.
        updateBaconBrawlAbilities(session, now);
        if (active != session
                || now - session.lastBaconSlowUpdateAt < ArcadePortRules.BACON_SLOW_UPDATE_INTERVAL_MILLIS) return;
        session.lastBaconSlowUpdateAt = now;
        if (now - session.lastBaconHungerAt >= ArcadePortRules.BACON_HUNGER_INTERVAL_MILLIS) {
            session.lastBaconHungerAt = now;
            for (Player player : new ArrayList<>(alivePlayers(session))) {
                if (player.getFoodLevel() > 0) {
                    player.setFoodLevel(Math.max(0, player.getFoodLevel() - ArcadePortRules.BACON_HUNGER_DRAIN_PER_INTERVAL));
                    player.setSaturation(5.0F);
                } else if (player.getHealth() <= ArcadePortRules.BACON_STARVATION_DAMAGE) {
                    eliminate(session, player, null, "starved");
                    if (active != session) return;
                } else player.setHealth(player.getHealth() - ArcadePortRules.BACON_STARVATION_DAMAGE);
            }
        }
        // Re-evaluate the second escalation using the current alive count. A match that
        // drops to two players after the first escalation must accelerate without waiting
        // for a stale interval calculated while three or more players were alive.
        session.baconEscalationIntervalMillis = ArcadePortRules.nextBaconKnockbackInterval(
                session.baconKnockbackEscalations, alivePlayers(session).size());
        if (active != session || session.baconEscalationIntervalMillis <= 0L
                || now - session.lastBaconEscalationAt < session.baconEscalationIntervalMillis) return;
        session.lastBaconEscalationAt = now;
        session.baconKnockback = ArcadePortRules.nextBaconKnockback(session.baconKnockback);
        session.baconKnockbackEscalations++;
        session.baconEscalationIntervalMillis = ArcadePortRules.nextBaconKnockbackInterval(
                session.baconKnockbackEscalations, alivePlayers(session).size());
        String escalationMessage = ChatColor.GREEN
                + "The ground begins to tremble... All pigs now take more knockback!";
        announce(session, escalationMessage);
        for (Player player : alivePlayers(session)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 0.7F);
        }
    }

    /**
     * Keep Bacon Brawl's disguise scoreboard and hidden real-player state confined to this match.
     * Each player's original scoreboard is held in PlayerSnapshot and restored after the round.
     */
    private void setupBaconDisguiseNameTags(Session session, List<Player> participants) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        String teamName = "bb" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        try {
            Team disguiseTeam = scoreboard.registerNewTeam(teamName);
            disguiseTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            // Bacon avatars replace the players visually; prevent the hidden player bodies
            // from pushing against each other while preserving normal hit targeting.
            disguiseTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            session.baconDisguiseScoreboard = scoreboard;
            session.baconDisguiseTeam = disguiseTeam;
            for (Player player : participants) {
                player.setScoreboard(scoreboard);
                String entry = player.getName();
                Team previous = scoreboard.getEntryTeam(entry);
                session.baconPreviousNameTeams.put(player.getUniqueId(), previous == disguiseTeam ? null : previous);
                session.baconNameEntries.put(player.getUniqueId(), entry);
                disguiseTeam.addEntry(entry);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            plugin.getLogger().warning("Could not hide Bacon Brawl player nameplates: " + exception.getMessage());
            cleanupBaconDisguiseNameTags(session);
        }
    }

    private void restoreBaconDisguiseNameTagEntry(Session session, UUID playerId) {
        String entry = session.baconNameEntries.remove(playerId);
        Team disguiseTeam = session.baconDisguiseTeam;
        Scoreboard scoreboard = session.baconDisguiseScoreboard;
        if (entry == null) return;
        if (disguiseTeam != null) {
            try {
                disguiseTeam.removeEntry(entry);
            } catch (IllegalStateException ignored) {
                // Another plugin may already have removed the temporary team.
            }
        }
        Team previous = session.baconPreviousNameTeams.remove(playerId);
        if (previous == null || scoreboard == null) return;
        try {
            Team currentlyRegistered = scoreboard.getTeam(previous.getName());
            if (currentlyRegistered != null && currentlyRegistered != disguiseTeam) {
                currentlyRegistered.addEntry(entry);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            plugin.getLogger().warning("Could not restore scoreboard team for " + entry
                    + " after Bacon Brawl: " + exception.getMessage());
        }
    }

    private void cleanupBaconDisguiseNameTags(Session session) {
        Team disguiseTeam = session.baconDisguiseTeam;
        Scoreboard scoreboard = session.baconDisguiseScoreboard;
        if (disguiseTeam == null || scoreboard == null) return;
        for (UUID playerId : new ArrayList<>(session.baconNameEntries.keySet())) {
            restoreBaconDisguiseNameTagEntry(session, playerId);
        }
        try {
            if (scoreboard.getTeam(disguiseTeam.getName()) != null) disguiseTeam.unregister();
        } catch (IllegalStateException ignored) {
            // Already unregistered by another plugin.
        }
        session.baconDisguiseTeam = null;
        session.baconDisguiseScoreboard = null;
        session.baconPreviousNameTeams.clear();
        session.baconNameEntries.clear();
    }

    private void setupBaconMeterScoreboard(Session session) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = session.baconDisguiseScoreboard;
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            for (Player player : session.players.values()) {
                if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
            }
        }
        session.baconMeterScoreboard = scoreboard;
        session.baconPreviousSidebar = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        String objectiveName = "bbm" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Objective objective = scoreboard.registerNewObjective(objectiveName, "dummy",
                ChatColor.GOLD + "BACON BRAWL");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.baconMeterObjective = objective;
        for (int i = 0; i < 15; i++) {
            Team lineTeam = scoreboard.registerNewTeam("bbmline" + i);
            String entry = ChatColor.values()[i].toString();
            lineTeam.addEntry(entry);
            session.baconMeterTeams.add(lineTeam);
            session.baconMeterEntries.add(entry);
        }
        updateBaconMeterScoreboard(session);
    }

    private void updateBaconMeterScoreboard(Session session) {
        if (session.baconMeterScoreboard == null || session.baconMeterObjective == null
                || session.type != ArcadePortGame.BACON_BRAWL) return;
        List<Player> alive = alivePlayers(session).stream()
                .sorted(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (int i = 0; i < session.baconMeterTeams.size(); i++) {
            Team lineTeam = session.baconMeterTeams.get(i);
            String entry = session.baconMeterEntries.get(i);
            if (i < alive.size()) {
                Player listed = alive.get(i);
                int percent = session.baconMeters.getOrDefault(listed.getUniqueId(), 0);
                lineTeam.setPrefix(ChatColor.WHITE + listed.getName());
                lineTeam.setSuffix(ChatColor.GREEN + " " + percent + "%");
                session.baconMeterObjective.getScore(entry).setScore(15 - i);
            } else {
                lineTeam.setPrefix("");
                lineTeam.setSuffix("");
                session.baconMeterScoreboard.resetScores(entry);
            }
        }
    }

    private void cleanupBaconMeterScoreboard(Session session) {
        Scoreboard scoreboard = session.baconMeterScoreboard;
        Objective objective = session.baconMeterObjective;
        if (scoreboard == null || objective == null) return;
        try {
            if (scoreboard.getObjective(DisplaySlot.SIDEBAR) == objective
                    && session.baconPreviousSidebar != null) {
                session.baconPreviousSidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            for (Team team : new ArrayList<>(session.baconMeterTeams)) {
                if (scoreboard.getTeam(team.getName()) != null) team.unregister();
            }
            objective.unregister();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            plugin.getLogger().fine("Bacon Brawl meter scoreboard was already removed: " + exception.getMessage());
        }
        session.baconMeterTeams.clear();
        session.baconMeterEntries.clear();
        session.baconMeterScoreboard = null;
        session.baconMeterObjective = null;
        session.baconPreviousSidebar = null;
    }

    private String baconMeterBar(int percent) {
        int charge = Math.max(0, Math.min(ArcadePortRules.BACON_METER_MAX, percent));
        int filled = (charge + 9) / 10;
        return ChatColor.GREEN + "█".repeat(filled) + ChatColor.DARK_GRAY + "░".repeat(10 - filled);
    }

    /** The vanilla XP bar is the ability meter during Bacon Brawl. */
    private void updateBaconMeterItem(Session session, Player player) {
        if (session.type != ArcadePortGame.BACON_BRAWL || player == null || !player.isOnline()
                || !session.alive.contains(player.getUniqueId())) return;
        int percent = Math.max(0, Math.min(ArcadePortRules.BACON_METER_MAX,
                session.baconMeters.getOrDefault(player.getUniqueId(), 0)));
        float progress = Math.min(0.999F, percent / (float) ArcadePortRules.BACON_METER_MAX);
        if (Math.abs(player.getExp() - progress) > 0.001F) player.setExp(progress);
        if (player.getLevel() != percent) player.setLevel(percent);

        // Remove the old slot-9 star if a tester is upgrading from an earlier plugin build.
        ItemStack oldMeter = player.getInventory().getItem(8);
        if (isBaconMeterItem(oldMeter)) player.getInventory().setItem(8, null);
    }

    private boolean isBaconMeterItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).startsWith("Ability Meter:");
    }

    private void showBaconHitFeedback(Session session, Player attacker, Player target, double timingMultiplier) {
        // A critical is the only timed-hit state that gets an explicit hit popup.
        // Ordinary/early/late hits remain represented by the live timing meter, not "hit/miss" labels.
        if (timingMultiplier < 1.5D) return;

        long until = System.currentTimeMillis() + ArcadePortRules.BACON_HIT_FEEDBACK_MILLIS;
        session.baconHitFeedbackUntil.put(attacker.getUniqueId(), until);
        session.baconHitFeedbackUntil.put(target.getUniqueId(), until);
        String flashy = ChatColor.GOLD + "✦ " + ChatColor.YELLOW + ChatColor.BOLD
                + "FLASHY CRITICAL!" + ChatColor.GOLD + " ✦";
        attacker.sendActionBar(flashy + ChatColor.WHITE + "  Knockback x"
                + String.format(java.util.Locale.ROOT, "%.2f", timingMultiplier));
        attacker.sendTitle("", flashy, 0, 18, 5);
        target.sendActionBar(ChatColor.RED + "✦ CRITICAL HIT from "
                + attacker.getName() + " ✦");
        target.sendTitle("", flashy, 0, 18, 5);

        org.bukkit.Location impact = target.getLocation().clone().add(0.0D, 1.0D, 0.0D);
        target.getWorld().spawnParticle(Particle.CRIT, impact, 22, 0.45D, 0.45D, 0.45D, 0.15D);
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, impact, 14, 0.35D, 0.45D, 0.35D, 0.08D);
        target.getWorld().spawnParticle(Particle.FIREWORK, impact, 10, 0.25D, 0.35D, 0.25D, 0.04D);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.35F);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9F, 1.1F);
    }

    /**
     * Packet-level Mineplex disguise is approximated with a server-side avatar. Hide the real
     * player from other viewers while the pig/sheep proxy is present, remembering only viewers
     * this session hid so other plugins' pre-existing visibility choices can be preserved.
     */
    private void hideBaconPlayerFromViewers(Session session, Player owner) {
        UUID ownerId = owner.getUniqueId();
        Set<UUID> hiddenViewers = session.baconHiddenViewersByOwner
                .computeIfAbsent(ownerId, ignored -> new HashSet<>());
        // Only match participants need to see the avatar instead of its real player.
        // Hiding from all Bukkit online players also hides this player in unrelated worlds.
        for (Player viewer : session.players.values()) {
            if (viewer == null || viewer.equals(owner) || !viewer.isOnline()) continue;
            if (viewer.canSee(owner)) {
                viewer.hidePlayer(plugin, owner);
                hiddenViewers.add(viewer.getUniqueId());
            }
        }
    }

    private void restoreBaconPlayerVisibilityForOwner(Session session, UUID ownerId, Player owner) {
        Set<UUID> hiddenViewers = session.baconHiddenViewersByOwner.remove(ownerId);
        // showPlayer also clears the viewer's hidden-entity state if the target has just quit;
        // requiring owner.isOnline() here leaks the hide state across a reconnect.
        if (hiddenViewers == null || owner == null) return;
        for (UUID viewerId : hiddenViewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) viewer.showPlayer(plugin, owner);
        }
    }

    private void cleanupBaconPlayerVisibility(Session session) {
        for (UUID ownerId : new ArrayList<>(session.baconHiddenViewersByOwner.keySet())) {
            restoreBaconPlayerVisibilityForOwner(session, ownerId, session.players.get(ownerId));
        }
        session.baconHiddenViewersByOwner.clear();
    }

    /** Keep the real player authoritative while rendering the source kit's pig or pink Sheep Pig avatar. */
    private void updateBaconPlayerPig(Session session, Player owner, long now) {
        UUID ownerId = owner.getUniqueId();
        hideBaconPlayerFromViewers(session, owner);
        UUID avatarId = session.baconPlayerPigs.get(ownerId);
        boolean sheepKit = ArcadePortRules.baconBrawlUsesSheepAvatar(
                session.baconKits.getOrDefault(ownerId, "el-muchacho-pigo"));
        LivingEntity avatar = null;
        if (avatarId != null) {
            for (Entity entity : session.entities) {
                if (entity instanceof LivingEntity living && living.getUniqueId().equals(avatarId) && living.isValid()) {
                    avatar = living;
                    break;
                }
            }
        }
        boolean wrongDisguise = avatar != null && (sheepKit != (avatar instanceof Sheep));
        if (wrongDisguise) {
            UUID oldId = avatar.getUniqueId();
            baconAvatarOwnersByEntityId.remove(avatar.getEntityId());
            avatar.eject();
            avatar.remove();
            session.entities.remove(avatar);
            session.baconPigOwners.remove(oldId);
            session.baconPlayerPigs.remove(ownerId);
            avatar = null;
        }
        if (avatar == null) {
            if (sheepKit) {
                Sheep sheep = session.world.spawn(owner.getLocation(), Sheep.class);
                sheep.setColor(DyeColor.PINK);
                avatar = sheep;
            } else {
                avatar = session.world.spawn(owner.getLocation(), Pig.class);
            }
            if (avatar instanceof Mob mob) mob.setAI(false);
            avatar.setSilent(true);
            avatar.setRemoveWhenFarAway(false);
            avatar.setCollidable(false);
            avatar.setCustomNameVisible(false);
            // The source disguise is visible to other players, not as a second animal in first person.
            owner.hideEntity(plugin, avatar);
            session.entities.add(avatar);
            session.baconPlayerPigs.put(ownerId, avatar.getUniqueId());
            session.baconPigOwners.put(avatar.getUniqueId(), ownerId);
            baconAvatarOwnersByEntityId.put(avatar.getEntityId(), ownerId);
        }
        if (avatar instanceof Sheep sheep) sheep.setColor(DyeColor.PINK);
        if (avatar.getLocation().distanceSquared(owner.getLocation()) > 0.01D) {
            avatar.teleport(owner.getLocation());
        }
        avatar.setRotation(owner.getLocation().getYaw(), 0.0F);
        avatar.setCollidable(false);
        Long cloakUntil = session.baconCloakUntil.get(ownerId);
        boolean cloaked = cloakUntil != null && now < cloakUntil;
        avatar.setInvisible(cloaked);
        Long brutusUntil = session.baconBrutusUntil.get(ownerId);
        boolean brutusGuard = brutusUntil != null && now < brutusUntil;
        avatar.setGlowing(brutusGuard);
        if (brutusGuard && session.ticks % 4 == 0) {
            session.world.spawnParticle(Particle.TOTEM_OF_UNDYING,
                    avatar.getLocation().add(0.0D, 0.5D, 0.0D), 2, 0.25D, 0.25D, 0.25D, 0.01D);
        }
    }

    private void endBaconCloak(Session session, UUID ownerId, Player owner) {
        session.baconCloakUntil.remove(ownerId);
        LivingEntity avatar = baconPlayerAvatar(session, ownerId);
        if (avatar != null) avatar.setInvisible(false);
        if (owner != null && owner.isOnline()) {
            owner.playSound(owner.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.65F, 1.25F);
        }
    }

    private void endBaconBrutus(Session session, UUID ownerId, Player owner) {
        session.baconBrutusUntil.remove(ownerId);
        if (owner != null && owner.isOnline()) {
            owner.removePotionEffect(PotionEffectType.RESISTANCE);
        }
        LivingEntity avatar = baconPlayerAvatar(session, ownerId);
        if (avatar != null) avatar.setGlowing(false);
    }

    private LivingEntity baconPlayerAvatar(Session session, UUID ownerId) {
        UUID avatarId = session.baconPlayerPigs.get(ownerId);
        if (avatarId == null) return null;
        for (Entity entity : session.entities) {
            if (entity instanceof LivingEntity living && living.getUniqueId().equals(avatarId) && living.isValid()) return living;
        }
        return null;
    }

    private void removeBaconPlayerPig(Session session, UUID ownerId) {
        UUID pigId = session.baconPlayerPigs.remove(ownerId);
        if (pigId != null) {
            session.baconPigOwners.remove(pigId);
            for (Entity entity : new ArrayList<>(session.entities)) {
                if (entity.getUniqueId().equals(pigId)) {
                    baconAvatarOwnersByEntityId.remove(entity.getEntityId());
                    entity.remove();
                    session.entities.remove(entity);
                    break;
                }
            }
        }
        Player owner = session.players.get(ownerId);
        if (owner != null && owner.isOnline()) owner.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private void updateBaconBrawlAbilities(Session session, long now) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            String kit = session.baconKits.getOrDefault(id, "el-muchacho-pigo");
            // The real player is hidden behind the pig/sheep disguise; disabling collisions
            // on that hidden body avoids phantom pushes while preserving attack targeting.
            player.setCollidable(false);
            PotionEffect invisible = player.getPotionEffect(PotionEffectType.INVISIBILITY);
            if (invisible == null || invisible.getDuration() < 20) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false, false));
            }
            Long brutusUntil = session.baconBrutusUntil.get(id);
            if (brutusUntil != null && now >= brutusUntil) endBaconBrutus(session, id, player);
            Long cloakUntil = session.baconCloakUntil.get(id);
            if (cloakUntil != null && now >= cloakUntil) endBaconCloak(session, id, player);
            updateBaconPlayerPig(session, player, now);
            updateBaconMeterItem(session, player);
            if ("mama-piggles".equals(kit)) {
                PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
                if (session.ticks % 20 == 0 || speed == null || speed.getDuration() < 20) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, false));
                }
                updateBaconBabyPig(session, player, now);
            } else {
                removeBaconBabyPig(session, id);
            }
        }
        updateBaconSpectatorCompasses(session);
        updateBaconMeterScoreboard(session);

        for (Map.Entry<UUID, Long> entry : new ArrayList<>(session.baconSlams.entrySet())) {
            UUID slammerId = entry.getKey();
            Player slammer = session.players.get(slammerId);
            long expiresAt = entry.getValue();
            if (slammer == null || !slammer.isOnline() || !session.alive.contains(slammerId)
                    || now >= expiresAt || (expiresAt - now < 800L && slammer.isOnGround())) {
                session.baconSlams.remove(slammerId);
                session.baconSlamPowerPercent.remove(slammerId);
                continue;
            }
            if (session.ticks % 2 == 0) {
                Location slamTrail = slammer.getLocation().add(0.0D, 0.9D, 0.0D);
                session.world.spawnParticle(Particle.ELECTRIC_SPARK, slamTrail, 2, 0.12D, 0.12D, 0.12D, 0.01D);
            }
            for (Player target : alivePlayers(session)) {
                if (!target.equals(slammer) && target.getLocation().distanceSquared(slammer.getLocation()) < 4.0D) {
                    handlePlayerHit(session, slammer, target, "body-slam");
                    session.baconSlams.remove(slammerId);
                    session.baconSlamPowerPercent.remove(slammerId);
                    break;
                }
            }
            if (active != session) return;
        }
    }

    /**
     * Mama Piggles' Baby Piggles companion is a client-visible baby pig. It is
     * invulnerable and follows the owner; normal player hitboxes remain authoritative.
     */
    /** Reborn's CompassModule gives eliminated Bacon Brawl players a live nearest-opponent tracker. */
    private void updateBaconSpectatorCompasses(Session session) {
        List<Player> targets = alivePlayers(session);
        if (targets.isEmpty()) return;
        for (Player spectator : session.players.values()) {
            if (spectator == null || !spectator.isOnline()
                    || session.alive.contains(spectator.getUniqueId())) continue;
            boolean hasCompass = false;
            for (ItemStack item : spectator.getInventory().getContents()) {
                if (isBaconTrackingCompass(item)) {
                    hasCompass = true;
                    break;
                }
            }
            if (!hasCompass) {
                spectator.getInventory().addItem(namedItem(Material.COMPASS, "Tracking Compass",
                        "Tracks the nearest surviving pig."));
            }
            Player target = targets.stream()
                    .min(java.util.Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(spectator.getLocation())))
                    .orElse(null);
            if (target != null) spectator.setCompassTarget(target.getLocation());
        }
    }

    private boolean isBaconTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).equals("Tracking Compass");
    }

    private void openBaconCompassMenu(Session session, Player spectator) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Spectator Menu");
        ItemStack header = namedItem(Material.COMPASS, "Tracking Compass", "Choose a surviving pig to follow.");
        inventory.setItem(4, header);

        int[] slots = {
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43,
                46, 47, 48, 49, 50, 51, 52
        };
        Map<Integer, UUID> targetsBySlot = new HashMap<>();
        List<Player> targets = alivePlayers(session).stream()
                .sorted(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (int i = 0; i < targets.size() && i < slots.length; i++) {
            Player target = targets.get(i);
            int slot = slots[i];
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(ChatColor.YELLOW + target.getName());
                double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                String kit = session.baconKits.getOrDefault(target.getUniqueId(), "el-muchacho-pigo");
                meta.setLore(List.of(
                        ChatColor.GRAY + "Kit: " + kit,
                        ChatColor.GRAY + "Distance: " + String.format(java.util.Locale.ROOT, "%.1f", distance),
                        ChatColor.GRAY + "Height difference: " + String.format(java.util.Locale.ROOT, "%.1f", height),
                        ChatColor.YELLOW + "Left-click: Teleport",
                        ChatColor.YELLOW + "Right-click: Spectate"));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            targetsBySlot.put(slot, target.getUniqueId());
        }
        session.baconCompassMenuTargets.put(spectator.getUniqueId(), targetsBySlot);
        spectator.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBaconCompassMenuClick(InventoryClickEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL
                || !isBaconCompassMenuTitle(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player spectator)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        Map<Integer, UUID> targets = session.baconCompassMenuTargets.get(spectator.getUniqueId());
        UUID targetId = targets == null ? null : targets.get(slot);
        if (targetId == null) return;
        Player target = session.players.get(targetId);
        session.baconCompassMenuTargets.remove(spectator.getUniqueId());
        spectator.closeInventory();
        if (target == null || !target.isOnline() || !session.alive.contains(targetId)) {
            spectator.sendMessage(ChatColor.YELLOW + "That pig is no longer alive.");
            return;
        }
        if (event.isRightClick()) {
            spectator.setSpectatorTarget(target);
        } else {
            spectator.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
        }
        spectator.setCompassTarget(target.getLocation());
    }

    private boolean isBaconCompassMenuTitle(String title) {
        return (ChatColor.DARK_GREEN + "Spectator Menu").equals(title);
    }

    private Player nearestAliveBaconTarget(Session session, Player spectator) {
        return alivePlayers(session).stream()
                .min(java.util.Comparator.comparingDouble(candidate ->
                        candidate.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
    }

    private void updateBaconBabyPig(Session session, Player owner, long now) {
        UUID ownerId = owner.getUniqueId();
        UUID pigId = session.baconBabyPigs.get(ownerId);
        Pig baby = null;
        if (pigId != null) {
            for (Entity entity : session.entities) {
                if (entity instanceof Pig pig && pig.getUniqueId().equals(pigId) && pig.isValid()) {
                    baby = pig;
                    break;
                }
            }
        }
        if (baby == null) {
            Location spawn = owner.getLocation().clone().add(0.8D, 0.0D, 0.8D);
            baby = session.world.spawn(spawn, Pig.class);
            baby.setBaby();
            baby.setAgeLock(true);
            baby.setCustomName(ChatColor.LIGHT_PURPLE + "Baby Piggles");
            baby.setCustomNameVisible(false);
            // Source hides the passenger to Mama Piggles while other players see the companion.
            owner.hideEntity(plugin, baby);
            // Reborn transfers damage dealt to Baby Piggles onto Mama Piggles' player.
            baby.setInvulnerable(false);
            baby.setSilent(true);
            baby.setAI(false);
            baby.setRemoveWhenFarAway(false);
            session.entities.add(baby);
            session.baconBabyPigs.put(ownerId, baby.getUniqueId());
            session.baconPigOwners.put(baby.getUniqueId(), ownerId);
            baconAvatarOwnersByEntityId.put(baby.getEntityId(), ownerId);
        }

        LivingEntity avatar = baconPlayerAvatar(session, ownerId);
        if (avatar != null && baby.getVehicle() != avatar) {
            if (baby.getVehicle() != null) baby.leaveVehicle();
            avatar.addPassenger(baby);
        }
        baby.setRotation(owner.getLocation().getYaw(), 0.0F);
        // Reborn mounts Baby Piggles on Mama Piggles' pig form; it is a transferred hitbox, not an attacker.
    }

    private void removeBaconBabyPig(Session session, UUID ownerId) {
        UUID pigId = session.baconBabyPigs.remove(ownerId);
        if (pigId == null) return;
        session.baconPigOwners.remove(pigId);
        for (Entity entity : new ArrayList<>(session.entities)) {
            if (entity.getUniqueId().equals(pigId)) {
                baconAvatarOwnersByEntityId.remove(entity.getEntityId());
                entity.remove();
                session.entities.remove(entity);
                break;
            }
        }
    }

    private boolean baconBlastTouchingAdjacentSolidBlock(Location location) {
        Block center = location.getBlock();
        for (BlockFace face : new BlockFace[] {
                BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN }) {
            Block adjacent = center.getRelative(face);
            if (adjacent.getType().isSolid() && !adjacent.isLiquid()) return true;
        }
        return false;
    }

    private void updateBaconBlastProjectiles(Session session, long now) {
        for (UUID blastId : new ArrayList<>(session.baconBlastItems.keySet())) {
            Item bacon = session.baconBlastItems.get(blastId);
            if (bacon == null || !bacon.isValid() || bacon.getWorld() != session.world) {
                removeBaconBlastItem(session, blastId);
                continue;
            }

            Location current = bacon.getLocation().clone();
            Location previous = session.baconBlastLastLocations.getOrDefault(blastId, current).clone();
            double blastPower = session.baconBlastPowerById.getOrDefault(blastId, 0.5D);
            if (session.ticks % 2 == 0) {
                session.world.spawnParticle(Particle.SOUL_FIRE_FLAME, current, Math.max(1, (int) Math.round(blastPower * 3.0D)),
                        0.05D, 0.05D, 0.05D, 0.005D);
            }
            UUID ownerId = session.baconBlastOwners.get(blastId);
            double segmentLength = Math.sqrt(previous.distanceSquared(current));
            double blockFraction = baconBlastFirstBlockFraction(session, previous, current);
            Player target = null;
            double bestDistanceSquared = Double.POSITIVE_INFINITY;
            double bestTargetFraction = Double.POSITIVE_INFINITY;
            for (Player candidate : alivePlayers(session)) {
                if (candidate.getUniqueId().equals(ownerId)) continue;
                Location targetCenter = candidate.getLocation().clone().add(0.0D, 0.9D, 0.0D);
                double distanceSquared = distanceSquaredToSegment(targetCenter, previous, current);
                if (!ArcadePortRules.isBaconBlastNearPlayer(distanceSquared)) continue;
                double targetFraction = baconBlastSegmentFraction(targetCenter, previous, current);
                if (targetFraction < bestTargetFraction
                        || (Math.abs(targetFraction - bestTargetFraction) < 1.0E-6D
                            && distanceSquared < bestDistanceSquared)) {
                    target = candidate;
                    bestDistanceSquared = distanceSquared;
                    bestTargetFraction = targetFraction;
                }
            }

            boolean playerFirst = target != null && ArcadePortRules.isBaconBlastPlayerBeforeBlock(
                    bestTargetFraction, blockFraction, segmentLength);
            if (blockFraction >= 0.0D && !playerFirst) {
                Location blockImpact = previous.clone().add(
                        current.toVector().subtract(previous.toVector()).multiply(blockFraction));
                spawnBaconBlastImpact(session, blockImpact);
                removeBaconBlastItem(session, blastId);
                continue;
            }

            if (target != null && playerFirst) {
                Player owner = ownerId == null ? null : session.players.get(ownerId);
                Location playerImpact = previous.clone().add(
                        current.toVector().subtract(previous.toVector()).multiply(bestTargetFraction));
                // Reborn resolves the first swept collision, then dispatches custom projectile
                // damage for the target's knockback and kill attribution.
                spawnBaconBlastImpact(session, playerImpact);
                if (owner != null && owner.isOnline() && session.alive.contains(ownerId)
                        && active == session && session.alive.contains(target.getUniqueId())) {
                    handleBaconBrawlHit(session, owner, target, "bacon-blast", 6.0D, null, blastPower);
                }
                removeBaconBlastItem(session, blastId);
                if (active != session) return;
                continue;
            }

            Block currentBlock = current.getBlock();
            boolean stationary = current.distanceSquared(previous) <= 0.0025D
                    && bacon.getVelocity().lengthSquared() <= 0.05D;
            boolean stationaryAgainstBlock = stationary && baconBlastTouchingAdjacentSolidBlock(current);
            boolean touchingBlock = ArcadePortRules.isBaconBlastBlockImpact(
                    bacon.isOnGround(), currentBlock.isLiquid(), currentBlock.getType().isSolid(),
                    stationaryAgainstBlock);
            long createdAt = session.baconBlastCreatedAt.getOrDefault(blastId, 0L);
            if (ArcadePortRules.shouldExpireBaconBlast(now, createdAt, touchingBlock)) {
                spawnBaconBlastImpact(session, current);
                removeBaconBlastItem(session, blastId);
                continue;
            }
            session.baconBlastLastLocations.put(blastId, current);
        }
    }

    private void spawnBaconBlastImpact(Session session, Location impact) {
        // Reborn creates a 0.5-power, non-incendiary, non-block-breaking explosion on impact.
        // Damage events are synchronous; suppress only its explosion health damage while keeping
        // the native explosion impulse and the custom swept-hit knockback.
        session.baconBlastExplosionDepth++;
        try {
            session.world.createExplosion(impact.getX(), impact.getY(), impact.getZ(), 0.5F, false, false);
        } finally {
            session.baconBlastExplosionDepth--;
        }
        session.world.spawnParticle(Particle.CLOUD, impact, 14, 0.25D, 0.25D, 0.25D, 0.04D);
    }

    private void removeBaconBlastItem(Session session, UUID blastId) {
        Item bacon = session.baconBlastItems.remove(blastId);
        if (bacon != null) {
            session.entities.remove(bacon);
            if (bacon.isValid()) bacon.remove();
        }
        session.baconBlastOwners.remove(blastId);
        session.baconBlastLastLocations.remove(blastId);
        session.baconBlastCreatedAt.remove(blastId);
        session.baconBlastPowerById.remove(blastId);
    }

    private void useBaconBrawlAbility(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        String kit = session.baconKits.getOrDefault(id, "el-muchacho-pigo");
        int charge = session.baconMeters.getOrDefault(id, 0);
        if (!ArcadePortRules.canUseBaconMeterAbility(charge)) {
            player.sendMessage(ChatColor.YELLOW + "Your ability meter needs "
                    + ArcadePortRules.BACON_METER_MIN_ABILITY + "% charge.");
            return;
        }
        // Abilities are gated only by earned damage meter, never by a wall-clock cooldown.
        session.baconAbilitiesUsed.add(id);
        session.baconMeters.put(id, 0);
        updateBaconMeterItem(session, player);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9F,
                0.7F + charge / 100.0F);

        switch (kit) {
            case "mama-piggles": {
                double power = ArcadePortRules.baconBlastPowerMultiplier(charge);
                Vector direction = player.getEyeLocation().getDirection().normalize();
                Location launchLocation = player.getEyeLocation().add(direction.clone().multiply(1.5D));
                Item bacon = player.getWorld().dropItem(launchLocation, new ItemStack(Material.PORKCHOP, ArcadePortRules.BACON_BLAST_PROJECTILE_STACK_SIZE));
                bacon.setPickupDelay(Short.MAX_VALUE);
                Vector velocity = direction.clone().multiply(0.6D + power * 0.4D);
                velocity.setY(velocity.getY() + 0.15D + power * 0.15D);
                bacon.setVelocity(velocity);
                UUID blastId = bacon.getUniqueId();
                session.entities.add(bacon);
                session.baconBlastItems.put(blastId, bacon);
                session.baconBlastOwners.put(blastId, id);
                session.baconBlastPowerById.put(blastId, power);
                session.baconBlastCreatedAt.put(blastId, now);
                session.baconBlastLastLocations.put(blastId, bacon.getLocation().clone());
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        player.getEyeLocation(), Math.max(2, charge / 10), 0.2D, 0.2D, 0.2D, 0.01D);
                player.playSound(player.getLocation(), Sound.ENTITY_PIG_AMBIENT, 1.0F, 1.5F);
                player.sendMessage(ChatColor.GREEN + "Bacon Blast! Power: " + Math.round(power * 100) + "%.");
                break;
            }
            case "pig": {
                long duration = ArcadePortRules.baconAbilityDurationMillis(charge);
                session.baconCloakUntil.put(id, now + duration);
                LivingEntity avatar = baconPlayerAvatar(session, id);
                if (avatar != null) avatar.setInvisible(true);
                player.getWorld().spawnParticle(Particle.SMOKE,
                        player.getLocation().add(0.0D, 1.0D, 0.0D),
                        Math.max(8, charge / 4), 0.35D, 0.5D, 0.35D, 0.02D);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.6F);
                player.sendMessage(ChatColor.GREEN + "Pig Cloak active for "
                        + String.format(java.util.Locale.ROOT, "%.1f", duration / 1000.0D)
                        + " seconds. Landing a hit or running out of time reveals you.");
                break;
            }
            case "brutus-bacon": {
                long duration = ArcadePortRules.baconAbilityDurationMillis(charge);
                session.baconBrutusUntil.put(id, now + duration);
                int ticks = (int) Math.max(1L, (duration + 49L) / 50L);
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE, ticks, 4, false, false, true));
                LivingEntity avatar = baconPlayerAvatar(session, id);
                if (avatar != null) avatar.setGlowing(true);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        player.getLocation().add(0.0D, 1.0D, 0.0D),
                        Math.max(8, charge / 3), 0.45D, 0.65D, 0.45D, 0.02D);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.9F, 1.3F);
                player.sendMessage(ChatColor.GREEN + "Brutus Guard active for "
                        + String.format(java.util.Locale.ROOT, "%.1f", duration / 1000.0D)
                        + " seconds. Frontal hits are blocked; a backstab breaks the guard.");
                break;
            }
            default: {
                session.baconSlamPowerPercent.put(id, charge);
                double launchScale = 0.5D + charge / 200.0D;
                Vector leap = player.getLocation().getDirection().normalize().multiply(1.2D * launchScale);
                leap.setY(ArcadePortRules.baconBrawlBodySlamLaunchY(
                        player.getLocation().getDirection().getY(), player.isOnGround()) * launchScale);
                player.setVelocity(leap);
                session.baconSlams.put(id, now + 1_000L);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        player.getLocation(), Math.max(8, charge / 3), 0.3D, 0.25D, 0.3D, 0.05D);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.8F + charge / 150.0F);
                player.sendMessage(ChatColor.GREEN + "Body Slam charged to " + charge + "%!");
                break;
            }
        }
    }

    private void handleBaconBrawlHit(Session session, Player attacker, Player target, String source) {
        double rawDamage = switch (source) {
            case "body-slam", "bacon-blast" -> 6.0D;
            default -> 2.0D;
        };
        handleBaconBrawlHit(session, attacker, target, source, rawDamage, null, 1.0D);
    }

    private void handleBaconBrawlHit(Session session, Player attacker, Player target, String source,
            double rawDamage, Vector projectileVelocity) {
        handleBaconBrawlHit(session, attacker, target, source, rawDamage, projectileVelocity, 1.0D);
    }

    private void handleBaconBrawlHit(Session session, Player attacker, Player target, String source,
            double rawDamage, Vector projectileVelocity, double abilityPowerMultiplier) {
        if (active != session || attacker.equals(target)
                || !session.alive.contains(attacker.getUniqueId())
                || !session.alive.contains(target.getUniqueId())) return;
        long now = System.currentTimeMillis();
        UUID attackerId = attacker.getUniqueId();
        UUID targetId = target.getUniqueId();
        long brutusUntil = session.baconBrutusUntil.getOrDefault(targetId, 0L);
        boolean brutusActive = brutusUntil > now;
        boolean backstab = isBaconBrawlBackstab(target, attacker);
        boolean brutusBackstab = isBaconBrutusBackstab(target, attacker);
        if (ArcadePortRules.shouldBaconBrutusGuardHit(brutusActive, brutusBackstab)) {
            attacker.sendActionBar(ChatColor.RED + "GUARD BLOCKED");
            target.sendActionBar(ChatColor.GOLD + "" + ChatColor.BOLD + "BRUTUS GUARD"
                    + ChatColor.GRAY + " | frontal hit absorbed");
            target.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    target.getLocation().add(0.0D, 1.0D, 0.0D), 10, 0.35D, 0.45D, 0.35D, 0.01D);
            target.getWorld().spawnParticle(Particle.CRIT,
                    target.getLocation().add(0.0D, 1.0D, 0.0D), 8, 0.25D, 0.35D, 0.25D, 0.03D);
            target.playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8F, 1.5F);
            return;
        }
        if (brutusActive && brutusBackstab) {
            endBaconBrutus(session, targetId, target);
            target.sendActionBar(ChatColor.RED + "" + ChatColor.BOLD + "BRUTUS GUARD BROKEN!");
            target.playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.9F, 0.8F);
        }
        if (session.baconCloakUntil.containsKey(attackerId)) {
            endBaconCloak(session, attackerId, attacker);
        }
        session.baconLastDamager.put(targetId, attackerId);
        session.baconLastDamagedAt.put(targetId, now);
        session.baconLastHitSource.put(targetId, source);

        double timingMultiplier = 1.0D;
        boolean normalTimedHit = "melee".equals(source) || "projectile hit".equals(source);
        if (normalTimedHit) {
            long previousHit = session.baconLastSwingAt.getOrDefault(attackerId, 0L);
            if (previousHit > 0L) {
                timingMultiplier = ArcadePortRules.baconTimedHitKnockbackMultiplier(now - previousHit);
            }
            session.baconLastSwingAt.put(attackerId, now);
        }

        String kit = session.baconKits.getOrDefault(attackerId, "el-muchacho-pigo");
        double knockbackMultiplier = session.baconKnockback * timingMultiplier * Math.max(0.0D, abilityPowerMultiplier);
        if ("body-slam".equals(source)) {
            int charge = session.baconSlamPowerPercent.getOrDefault(attackerId, 0);
            knockbackMultiplier *= ArcadePortRules.BACON_BODY_SLAM_KNOCKBACK_MULTIPLIER
                    * ArcadePortRules.baconBodySlamMeterMultiplier(charge);
        }
        if ("pig".equals(kit) && "melee".equals(source) && backstab) {
            knockbackMultiplier *= ArcadePortRules.baconBrawlKnockbackMultiplier(true);
            target.playSound(target.getLocation(), Sound.ENTITY_PIG_DEATH, 1.0F, 2.0F);
        }
        target.setVelocity(baconBrawlKnockbackVelocity(attacker, target, rawDamage,
                knockbackMultiplier, projectileVelocity));
        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0),
                10, 0.35D, 0.4D, 0.35D, 0.04D);

        int meter = ArcadePortRules.nextBaconMeter(session.baconMeters.getOrDefault(attackerId, 0), rawDamage);
        session.baconMeters.put(attackerId, meter);
        updateBaconMeterItem(session, attacker);
        attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + 2));
        attacker.setSaturation(ArcadePortRules.BACON_SATURATION_ON_FOOD_CHANGE);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8F, 1.1F);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9F, 0.85F);
        showBaconHitFeedback(session, attacker, target, timingMultiplier);

        if (active == session && "body-slam".equals(source)
                && session.alive.contains(attackerId)) {
            // Reborn's Body Slam emits reciprocal 2-damage recoil, but only the target receives kill credit.
            session.baconLastDamager.put(attackerId, targetId);
            session.baconLastDamagedAt.put(attackerId, now);
            session.baconLastHitSource.put(attackerId, "body-slam-recoil");
            target.setFoodLevel(Math.min(20, target.getFoodLevel() + 2));
            target.setSaturation(ArcadePortRules.BACON_SATURATION_ON_FOOD_CHANGE);
            int recoilMeter = ArcadePortRules.nextBaconMeter(
                    session.baconMeters.getOrDefault(targetId, 0), 2.0D);
            session.baconMeters.put(targetId, recoilMeter);
            updateBaconMeterItem(session, target);
            attacker.setVelocity(baconBrawlKnockbackVelocity(target, attacker, 2.0D,
                    session.baconKnockback * ArcadePortRules.BACON_BODY_SLAM_KNOCKBACK_MULTIPLIER, null));
        }
    }

    private boolean isBaconBrutusBackstab(Player target, Location attackerLocation) {
        Vector targetLook = target.getLocation().getDirection().setY(0.0D);
        Vector fromTargetToAttacker = attackerLocation.toVector()
                .subtract(target.getLocation().toVector()).setY(0.0D);
        return ArcadePortRules.isBaconBrutusBackstab(targetLook.getX(), targetLook.getZ(),
                fromTargetToAttacker.getX(), fromTargetToAttacker.getZ());
    }

    private boolean isBaconBrutusBackstab(Player target, Player attacker) {
        Vector targetLook = target.getLocation().getDirection().setY(0.0D);
        Vector fromTargetToAttacker = attacker.getLocation().toVector()
                .subtract(target.getLocation().toVector()).setY(0.0D);
        return ArcadePortRules.isBaconBrutusBackstab(targetLook.getX(), targetLook.getZ(),
                fromTargetToAttacker.getX(), fromTargetToAttacker.getZ());
    }

    private boolean isBaconBrawlBackstab(Player target, Player attacker) {
        Vector targetLook = target.getLocation().getDirection().setY(0.0D);
        Vector fromTargetToAttacker = attacker.getLocation().toVector()
                .subtract(target.getLocation().toVector()).setY(0.0D);
        return ArcadePortRules.isBaconBrawlBackstab(targetLook.getX(), targetLook.getZ(),
                fromTargetToAttacker.getX(), fromTargetToAttacker.getZ());
    }

    /** Port DamageManager's logarithmic base and UtilAction's strength, vertical cap and ground boost. */
    private Vector baconBrawlKnockbackVelocity(Player source, Player target, double rawDamage,
            double knockbackMultipliers, Vector projectileVelocity) {
        return baconBrawlKnockbackVelocity(source.getLocation(), target.getLocation(), rawDamage,
                knockbackMultipliers, projectileVelocity, target.isOnGround());
    }

    private Vector baconBrawlKnockbackVelocity(Location source, Location target, double rawDamage,
            double knockbackMultipliers, Vector projectileVelocity, boolean targetOnGround) {
        boolean projectile = projectileVelocity != null;
        Vector direction = projectile
                ? projectileVelocity.clone().setY(0.0D)
                : target.toVector().subtract(source.toVector()).setY(0.0D);
        if (direction.lengthSquared() < 1.0E-5D) direction = source.getDirection().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-5D) direction = new Vector(0.0D, 0.0D, 1.0D);
        double strength = ArcadePortRules.baconBrawlVelocityStrength(rawDamage, knockbackMultipliers, projectile);
        double vertical = ArcadePortRules.baconBrawlVerticalVelocity(
                rawDamage, knockbackMultipliers, projectile, targetOnGround);
        return direction.normalize().multiply(strength).setY(vertical);
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

    /**
     * Reborn's Dragon Escape spectator spawn is the mean live-racer position,
     * raised by ten blocks and centred by half a block.
     */
    private Location dragonEscapeSpectatorLocation(Session session) {
        List<Player> racers = alivePlayers(session);
        if (racers.isEmpty()) return new Location(session.world, 0.5D, 10.0D, 0.5D);
        double x = 0.0D, y = 0.0D, z = 0.0D;
        for (Player racer : racers) {
            Location location = racer.getLocation();
            x += location.getX();
            y += location.getY();
            z += location.getZ();
        }
        double count = racers.size();
        return new Location(session.world, x / count + 0.5D, y / count + 10.0D, z / count + 0.5D);
    }

    private Location randomDragonSkyTarget(Session session) {
        Location center = session.dragonSpectatorLocation == null
                ? session.world.getSpawnLocation() : session.dragonSpectatorLocation;
        double x = center.getX() + ThreadLocalRandom.current().nextDouble(-50.0D, 51.0D);
        double z = center.getZ() + ThreadLocalRandom.current().nextDouble(-50.0D, 51.0D);
        SourceMapCatalog.MapData mapData = sourceMapData.get(session.type.id());
        if (mapData != null && mapData.hasBounds()) {
            x = ArcadePortRules.clampDragonSkyTargetCoordinate(x, mapData.minX(), mapData.maxX());
            z = ArcadePortRules.clampDragonSkyTargetCoordinate(z, mapData.minZ(), mapData.maxZ());
        }
        return new Location(session.world, x,
                center.getY() + ThreadLocalRandom.current().nextDouble(20.0D, 41.0D), z);
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
        session.dragonEscapeMapResource = SourceMapCatalog.selectedResource(plugin, ArcadePortGame.DRAGON_ESCAPE);
        if (mapData != null) {
            session.dragonEscapeMapName = mapData.mapName();
            List<Location> speedMarkers = mapData.dataLocations("GREEN");
            if (!speedMarkers.isEmpty()) {
                session.dragonEscapeSpeedMultiplier =
                        ArcadePortRules.dragonEscapeSpeedMultiplier(speedMarkers.get(0).getX());
            }
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
        session.dragonEscapeStartLocation = spawn.clone();
        EnderDragon dragon = session.world.spawn(spawn, EnderDragon.class);
        dragon.setAI(false);
        dragon.setGravity(false);
        dragon.setInvulnerable(true);
        // Reborn marks Douglas as a ghost: visible and harmless, without player/entity collision.
        dragon.setCollidable(false);
        dragon.setRemoveWhenFarAway(false);
        dragon.setCustomName(ChatColor.YELLOW + "Douglas the Dragon");
        dragon.setCustomNameVisible(true);
        int initialTargetIndex = ArcadePortRules.dragonEscapeInitialTargetIndex(
                session.dragonEscapeWaypoints.size());
        Location firstTarget = initialTargetIndex < 0
                ? spawn.clone().add(0.0D, 0.0D, 8.0D)
                : session.dragonEscapeWaypoints.get(initialTargetIndex).clone();
        Vector initialDirection = firstTarget.toVector().subtract(spawn.toVector()).normalize();
        Location initialLocation = dragon.getLocation();
        initialLocation.setPitch(dragonEscapePitch(initialDirection));
        dragon.teleport(initialLocation);
        session.dragonEscapeDragonLocation = initialLocation.clone();
        session.dragonEscapeDragonVelocity = dragon.getLocation().getDirection().setY(0.0D).normalize();
        session.dragonEscapeDragonPitch = dragonEscapePitch(dragon.getLocation().getDirection());
        session.dragonEscapeDragon = dragon;
        session.entities.add(dragon);
    }

    private void useDragonEscapeLeap(Session session, Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long readyAt = session.dragonEscapeLeapReadyAt.getOrDefault(id, 0L);
        if (now < readyAt) {
            player.sendActionBar(ChatColor.YELLOW + "Leap recharging: "
                    + String.format(java.util.Locale.ROOT, "%.1f", (readyAt - now) / 1000.0D) + "s");
            return;
        }
        int remaining = session.dragonEscapeLeapUsesRemaining.getOrDefault(id, 0);
        if (remaining <= 0) {
            player.sendMessage(ChatColor.YELLOW + "You have used all of your leaps.");
            return;
        }
        remaining--;
        session.dragonEscapeLeapUsesRemaining.put(id, remaining);
        session.dragonEscapeLeapReadyAt.put(id, now + ArcadePortRules.DRAGON_ESCAPE_LEAP_RECHARGE_MILLIS);
        player.setExp(Math.max(0.0F, Math.min(0.99F,
                remaining / (float) Math.max(1, ArcadePortRules.dragonEscapeLeapUses(
                        session.dragonEscapeKits.getOrDefault(id, "jumper"))))));
        if (remaining == 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(namedItem(Material.IRON_AXE, "Leap",
                    "Right-click to leap forward (" + remaining + " uses remaining).", remaining));
        }
        Vector direction = player.getLocation().getDirection().normalize().multiply(1.0D);
        direction.setY(ArcadePortRules.dragonEscapeLeapVerticalVelocity(
                direction.getY(), player.isOnGround()));
        player.setVelocity(direction);
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
        Player ahead = dragonEscapeNextRankedPlayerAhead(session, player);
        if (ahead == null) {
            player.sendMessage(ChatColor.YELLOW + "There is no player ahead of you to warp to.");
            return;
        }
        Location safeLocation = session.dragonEscapeSafeLocations.get(ahead.getUniqueId());
        if (safeLocation == null || safeLocation.getWorld() != session.world) {
            safeLocation = ahead.getLocation();
        }
        Location destination = safeLocation.clone().add(0.0D, 0.5D, 0.0D);
        if (!player.teleport(destination)) {
            player.sendMessage(ChatColor.RED + "The warp failed; your pearl has been kept.");
            return;
        }
        consumeHeld(player);
        session.dragonEscapeWarperUsed.add(id);
        session.dragonEscapeWarpedAt.put(id, System.currentTimeMillis());
        session.lastLocations.put(id, destination.clone());
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setFallDistance(0.0F);
        player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9F, 1.2F);
        player.getWorld().spawnParticle(Particle.PORTAL, destination, 24, 0.3D, 0.5D, 0.3D, 0.2D);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Warped to " + ahead.getName() + "!");
    }

    /** Reborn ranks by checkpoint score and distance, then warps to the next higher-ranked live racer. */
    private Player dragonEscapeNextRankedPlayerAhead(Session session, Player player) {
        List<Player> ranked = new ArrayList<>(alivePlayers(session));
        Map<UUID, Double> scores = session.dragonEscapePlayerRankScores;
        // Reborn's Warper sorts the scores last written on its 500ms FAST update;
        // it does not recalculate ranks at the instant the pearl is clicked.
        ranked.removeIf(candidate -> !session.alive.contains(candidate.getUniqueId())
                || !candidate.isOnline() || !scores.containsKey(candidate.getUniqueId()));
        ranked.sort((left, right) ->
                Double.compare(scores.get(right.getUniqueId()), scores.get(left.getUniqueId())));
        int ownRank = ranked.indexOf(player);
        return ownRank > 0 ? ranked.get(ownRank - 1) : null;
    }

    private int dragonEscapePlayerProgress(Session session, Player player) {
        if (session.dragonEscapeWaypoints.isEmpty()) return player.getLocation().getBlockZ();
        return session.dragonEscapePlayerProgress.getOrDefault(player.getUniqueId(), 0);
    }

    /** Mutates checkpoint/rank state only on Reborn's 500ms FAST score cadence. */
    private int updateDragonEscapePlayerProgress(Session session, Player player, long now) {
        UUID id = player.getUniqueId();
        int previousProgress = session.dragonEscapePlayerProgress.getOrDefault(id, 0);
        int observedIndex = getClosestDragonEscapeWaypointIndex(session, player.getLocation());
        Integer trackedObserved = session.dragonEscapeObservedWaypoint.get(id);
        int previousObserved = ArcadePortRules.dragonEscapeObservedWaypointBaseline(
                trackedObserved == null ? previousProgress : trackedObserved, observedIndex,
                trackedObserved != null);
        int nextExpected = Math.min(session.dragonEscapeWaypoints.size() - 1,
                Math.max(0, previousObserved + 1));
        double distanceToNext = player.getLocation().distance(session.dragonEscapeWaypoints.get(nextExpected));
        double distanceToObserved = player.getLocation().distance(session.dragonEscapeWaypoints.get(observedIndex));
        long warpedAt = session.dragonEscapeWarpedAt.getOrDefault(id, -1L);
        if (ArcadePortRules.shouldRejectDragonEscapeShortcut(previousObserved, observedIndex,
                distanceToNext, distanceToObserved, now, warpedAt)) {
            eliminate(session, player, null, "cheating");
            return previousProgress;
        }
        session.dragonEscapeObservedWaypoint.put(id, Math.max(previousObserved, observedIndex));
        int candidate = getDragonEscapeWaypointIndex(session, player.getLocation(), previousProgress);
        if (ArcadePortRules.isDragonEscapeWarpGraceActive(now, warpedAt)) {
            candidate = Math.max(candidate, observedIndex);
        }
        int progress = ArcadePortRules.monotonicDragonEscapeProgress(
                previousProgress, candidate, session.dragonEscapeWaypoints.size());
        session.dragonEscapePlayerProgress.put(id, progress);
        Map<Integer, Long> runSplits = session.dragonEscapeRunSplitMillis.computeIfAbsent(id, ignored -> new HashMap<>());
        Map<Integer, Location> runSplitLocations = session.dragonEscapeRunSplitLocations.computeIfAbsent(
                id, ignored -> new HashMap<>());
        runSplits.putIfAbsent(0, 0L);
        Location runStart = session.dragonEscapeStartLocations.get(id);
        if (runStart != null) runSplitLocations.putIfAbsent(0, runStart.clone());
        long elapsed = Math.max(0L, now - session.startedAt);
        for (int passed = previousProgress; passed < progress; passed++) {
            runSplits.putIfAbsent(passed + 1, elapsed);
            runSplitLocations.putIfAbsent(passed + 1, player.getLocation().clone());
        }
        return progress;
    }

    private int getClosestDragonEscapeWaypointIndex(Session session, Location location) {
        int bestIndex = 0;
        double bestDistanceSquared = Double.POSITIVE_INFINITY;
        if (location == null || location.getWorld() != session.world) return bestIndex;
        for (int i = 0; i < session.dragonEscapeWaypoints.size(); i++) {
            double distanceSquared = location.distanceSquared(session.dragonEscapeWaypoints.get(i));
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void throwDragonEscapeMine(Session session, Player player) {
        if (inventoryCount(player, Material.TNT) <= 0) {
            player.sendMessage(ChatColor.YELLOW + "Your next mini explosive is still recharging.");
            return;
        }
        consumeHeld(player);
        // Reborn drops an ordinary TNT item at the eye position and leaves it
        // subject to the normal pickup event; it is not a primed explosive.
        Item mine = session.world.dropItem(player.getEyeLocation(), new ItemStack(Material.TNT));
        mine.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        session.entities.add(mine);
        session.dragonEscapeMineOwners.put(mine.getUniqueId(), player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.8F, 1.15F);
    }

    private void updateDragonEscapeSafeLocations(Session session) {
        for (Player player : alivePlayers(session)) {
            if (player.isOnGround()) {
                session.dragonEscapeSafeLocations.put(player.getUniqueId(), player.getLocation().clone());
            }
        }
    }

    /** Reborn hides a Digger's tunnel from every other racer until its 120-second restore. */
    private void updateDragonEscapeTunnelBlocks(Session session) {
        Iterator<Map.Entry<Block, BlockData>> iterator = session.dragonEscapeTunnelBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Block, BlockData> entry = iterator.next();
            Block block = entry.getKey();
            if (block.getType() != Material.AIR) {
                session.dragonEscapeTunnelOwners.remove(block);
                iterator.remove();
                continue;
            }
            UUID owner = session.dragonEscapeTunnelOwners.get(block);
            Location location = block.getLocation();
            for (Player viewer : session.world.getPlayers()) {
                if (owner == null || !viewer.getUniqueId().equals(owner)) {
                    viewer.sendBlockChange(location, entry.getValue());
                }
            }
        }
    }

    private void updateDragonEscapeResources(Session session, long now) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            // Reborn's GameFlagManager continuously enforces HungerSet=20 and
            // restores saturation on the fast update; don't let long runs drain food.
            if (session.ticks % 10 == 0) {
                player.setFoodLevel(20);
                player.setSaturation(1.0F);
            }
            if (!"disruptor".equals(session.dragonEscapeKits.getOrDefault(id, "jumper"))) continue;
            long nextAt = session.dragonEscapeNextTntAt.getOrDefault(id,
                    now + ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS);
            if (now < nextAt) continue;
            if (inventoryCount(player, Material.TNT) < ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_MAX_TNT) {
                player.getInventory().addItem(namedItem(Material.TNT, "Disruptor",
                        "Stationary trip mine; triggers nearby opponents after 2 seconds."));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4F, 1.2F);
            }
            session.dragonEscapeNextTntAt.put(id,
                    ArcadePortRules.dragonEscapeNextTntChargeAt(now));
        }
        updateDragonEscapeSpectatorCompasses(session);
    }

    /** Reborn's CompassModule points every eliminated racer at the nearest living opponent. */
    private void updateDragonEscapeSpectatorCompasses(Session session) {
        List<Player> alive = alivePlayers(session);
        if (alive.isEmpty()) return;
        for (Player spectator : session.players.values()) {
            if (spectator == null || !spectator.isOnline()
                    || session.alive.contains(spectator.getUniqueId())) continue;
            if (inventoryCount(spectator, Material.COMPASS) == 0) {
                spectator.getInventory().addItem(namedItem(Material.COMPASS, "Tracking Compass",
                        "Points toward the nearest living Dragon Escape racer."));
            }
            Player target = alive.stream().min(java.util.Comparator.comparingDouble(
                    candidate -> candidate.getLocation().distanceSquared(spectator.getLocation()))).orElse(null);
            if (target == null) continue;
            spectator.setCompassTarget(target.getLocation());
            ItemStack held = spectator.getInventory().getItemInMainHand();
            if (isDragonEscapeTrackingCompass(held)) {
                double distance = spectator.getLocation().distance(target.getLocation());
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                spectator.sendActionBar(ChatColor.WHITE + "Nearest Target: " + ChatColor.GREEN + target.getName()
                        + ChatColor.WHITE + " | Distance: " + ChatColor.GREEN
                        + String.format(java.util.Locale.ROOT, "%.1f", distance)
                        + ChatColor.WHITE + " | Height: " + ChatColor.GREEN
                        + String.format(java.util.Locale.ROOT, "%.1f", height));
            }
        }
    }

    private boolean isDragonEscapeTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    /** Remove owner tracking when the source-style dropped TNT item despawns or is removed. */
    private void updateDragonEscapeMines(Session session) {
        for (UUID mineId : new ArrayList<>(session.dragonEscapeMineOwners.keySet())) {
            Entity entity = session.world.getEntity(mineId);
            if (entity == null || !entity.isValid()) {
                session.dragonEscapeMineOwners.remove(mineId);
                if (entity != null) session.entities.remove(entity);
            }
        }
    }

    /** Reborn's Disruptor detonates on an opponent's pickup, not by proximity alone. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDragonEscapeDisruptorPickup(EntityPickupItemEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.DRAGON_ESCAPE
                || event.getItem() == null || event.getItem().getWorld() != session.world
                || !(event.getEntity() instanceof Player target)) return;
        Item mine = event.getItem();
        UUID ownerId = session.dragonEscapeMineOwners.get(mine.getUniqueId());
        if (ownerId == null) return;

        // Cancel every pickup attempt so TNT never enters an inventory.
        event.setCancelled(true);
        if (!session.alive.contains(target.getUniqueId())) return;
        if (!ArcadePortRules.shouldTriggerDragonEscapeDisruptor(
                mine.getTicksLived(), target.getLocation().distance(mine.getLocation()),
                target.getUniqueId().equals(ownerId))) return;

        session.dragonEscapeMineOwners.remove(mine.getUniqueId());
        session.entities.remove(mine);
        mine.remove();
        target.setVelocity(new Vector(0.0D, ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_LAUNCH_Y, 0.0D));
        target.setFallDistance(0.0F);
        target.playEffect(EntityEffect.HURT);
        target.setSprinting(false);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 2.0F);
        target.sendActionBar(ChatColor.RED + "A Disruptor mine launched you!");
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
        long elapsed = Math.max(0L, System.currentTimeMillis() - session.startedAt);
        if (ArcadePortRules.isDragonEscapeDiggerOpeningLocked(elapsed)) {
            long seconds = (ArcadePortRules.DRAGON_ESCAPE_DIGGER_OPENING_LOCKOUT_MILLIS - elapsed + 999L) / 1_000L;
            player.sendMessage(ChatColor.YELLOW + "You cannot dig for another " + seconds + " seconds.");
            return;
        }
        if (!isDragonEscapeDiggerBlockAllowed(block.getType())
                || !isDragonEscapeDiggerBuildLocationAllowed(session, block)
                || block.getState() instanceof org.bukkit.block.Container) return;

        Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
        Location above = block.getLocation().add(0.5D, 1.0D, 0.5D);
        for (Player other : alivePlayers(session)) {
            if (other.getUniqueId().equals(id)) continue;
            if (ArcadePortRules.shouldPreventDragonEscapeDigger(
                    center.distance(other.getLocation()), above.distance(other.getLocation()))) {
                player.sendMessage(ChatColor.YELLOW + "You cannot dig while another racer is that close to the block.");
                return;
            }
        }

        long now = System.currentTimeMillis();
        long nextDigAt = session.dragonEscapeDiggerNextUseAt.getOrDefault(id, 0L);
        if (!ArcadePortRules.shouldAllowDragonEscapeDigger(now, nextDigAt)) return;

        BlockState originalState = block.getState();
        BlockData originalBlockData = originalState.getBlockData().clone();
        Material mined = block.getType();
        Location dropLocation = block.getLocation().add(0.5D, 0.5D, 0.5D);
        block.setType(Material.AIR, false);
        session.dragonEscapeTunnelBlocks.put(block, originalBlockData);
        session.dragonEscapeTunnelOwners.put(block, id);
        scheduleDragonEscapeBlockRestore(session, originalState, block.getBlockData().clone());
        session.dragonEscapeDiggerNextUseAt.put(id, now + ArcadePortRules.DRAGON_ESCAPE_DIGGER_RECHARGE_MILLIS);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(mined, 1));
        for (ItemStack leftover : overflow.values()) player.getWorld().dropItemNaturally(dropLocation, leftover);
        remaining--;
        session.dragonEscapeDiggerUsesRemaining.put(id, remaining);
        updateDragonEscapeDiggerPickaxe(player, remaining);
        player.playSound(dropLocation, Sound.BLOCK_STONE_BREAK, 0.75F, 1.2F);
    }

    /** Keep the Digger tool in its inventory slot; Reborn allowed tunnelling with it selected or not. */
    private void updateDragonEscapeDiggerPickaxe(Player player, int remaining) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (existing == null || existing.getType() != Material.DIAMOND_PICKAXE) continue;
            if (remaining <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                player.getInventory().setItem(slot, namedItem(Material.DIAMOND_PICKAXE, "Digger Pickaxe",
                        "Left-click course blocks to recover and place as shortcuts (" + remaining + " digs left).",
                        remaining));
            }
            return;
        }
    }

    private void scheduleDragonEscapeBlockRestore(Session session, BlockState previousState, BlockData expectedCurrent) {
        Block block = previousState.getBlock();
        String key = block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        long token = ++session.dragonEscapeBlockRestoreSequence;
        session.dragonEscapeBlockRestoreTokens.put(key, token);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!session.dragonEscapeBlockRestoreTokens.remove(key, token)) return;
            session.dragonEscapeTunnelBlocks.remove(block);
            session.dragonEscapeTunnelOwners.remove(block);
            World world = block.getWorld();
            if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return;
            Block current = world.getBlockAt(block.getX(), block.getY(), block.getZ());
            if (!current.getBlockData().matches(expectedCurrent)) return;
            previousState.update(true, false);
        }, ArcadePortRules.DRAGON_ESCAPE_DIGGER_BLOCK_RESTORE_TICKS);
    }

    private float dragonEscapePitch(Vector vector) {
        if (vector == null || !Double.isFinite(vector.getX())
                || !Double.isFinite(vector.getY()) || !Double.isFinite(vector.getZ())
                || vector.lengthSquared() < 1.0E-12D) return 0.0F;
        double horizontal = Math.sqrt(vector.getX() * vector.getX() + vector.getZ() * vector.getZ());
        double pitch = Math.toDegrees(Math.atan(horizontal / vector.getY()));
        if (vector.getY() <= 0.0D) pitch += 90.0D;
        else pitch -= 90.0D;
        if (pitch == 180.0D) pitch = 0.0D;
        return (float) pitch;
    }

    private float dragonEscapeYaw(Vector vector) {
        if (vector == null || !Double.isFinite(vector.getX()) || !Double.isFinite(vector.getZ())
                || vector.lengthSquared() < 1.0E-12D) return 0.0F;
        double yaw = Math.toDegrees(Math.atan((-vector.getX()) / vector.getZ()));
        if (vector.getZ() < 0.0D) yaw += 180.0D;
        return (float) yaw;
    }

    private Location moveDragonEscapeDragon(Session session, Location target) {
        EnderDragon dragon = session.dragonEscapeDragon;
        if (dragon == null || !dragon.isValid() || target == null || target.getWorld() != session.world) {
            return dragon == null ? null : dragon.getLocation();
        }
        Location location = session.dragonEscapeDragonLocation == null
                ? dragon.getLocation().clone() : session.dragonEscapeDragonLocation.clone();
        Vector velocity = session.dragonEscapeDragonVelocity == null
                ? location.getDirection().setY(0.0D).normalize()
                : session.dragonEscapeDragonVelocity.clone();
        float pitch = session.dragonEscapeDragonPitch;
        double distanceToTarget = target.distance(location);
        float desiredPitch = distanceToTarget < 1.0E-6D ? pitch
                : dragonEscapePitch(target.toVector().subtract(location.toVector()));
        if (desiredPitch < pitch) pitch -= 0.05F;
        if (desiredPitch > pitch) pitch += 0.05F;
        pitch = Math.max(-0.5F, Math.min(0.5F, pitch));

        Vector desired = target.toVector().subtract(location.toVector()).setY(0.0D);
        if (desired.lengthSquared() > 1.0E-12D) desired.normalize();
        Vector currentFlat = velocity.clone().setY(0.0D);
        if (currentFlat.lengthSquared() > 1.0E-12D) currentFlat.normalize();
        desired.subtract(currentFlat).multiply(0.2D);
        velocity.add(desired);
        if (velocity.lengthSquared() > 1.0E-12D) velocity.normalize();

        double speed = ArcadePortRules.DRAGON_ESCAPE_CHASE_SPEED * session.dragonEscapeSpeedMultiplier;
        location.add(velocity.clone().multiply(speed));
        location.add(0.0D, -pitch, 0.0D);
        location.setPitch(-pitch);
        location.setYaw(180.0F + dragonEscapeYaw(velocity));
        session.dragonEscapeDragonVelocity = velocity;
        session.dragonEscapeDragonPitch = pitch;
        session.dragonEscapeDragonLocation = location.clone();
        dragon.teleport(location);
        return location;
    }

    /** Paper recreation of Reborn's DragonEscapeData.Move plus the radius-10 destruction. */
    private void updateDragonEscapeChase(Session session) {
        EnderDragon dragon = session.dragonEscapeDragon;
        if (active != session || dragon == null || !dragon.isValid() || dragon.isDead()) return;
        // Reborn enforces WorldBoundary on its FAST update even when a player is not moving.
        for (Player racer : new ArrayList<>(alivePlayers(session))) {
            checkPlayerArena(session, racer);
            if (active != session) return;
        }
        Location location = dragon.getLocation();
        if (!session.dragonEscapeWaypoints.isEmpty()) {
            int dragonCandidate = getDragonEscapeWaypointIndex(
                    session, location, session.dragonEscapeDragonProgress);
            int dragonIndex = ArcadePortRules.monotonicDragonEscapeProgress(
                    session.dragonEscapeDragonProgress, dragonCandidate, session.dragonEscapeWaypoints.size());
            session.dragonEscapeDragonProgress = dragonIndex;
            Location target = session.dragonEscapeWaypoints.get(
                    Math.min(session.dragonEscapeWaypoints.size() - 1, dragonIndex + 1));
            location = moveDragonEscapeDragon(session, target);

            // Reborn destroys a radius-10 sphere as the dragon moves. Do not scan the same area
            // every tick: update the sphere after the dragon has moved at least half a block.
            Location lastDestruction = session.dragonEscapeLastDestructionLocation;
            double movedSquared = lastDestruction == null ? 0.0D : location.distanceSquared(lastDestruction);
            if (ArcadePortRules.shouldProcessDragonEscapeDestruction(movedSquared, lastDestruction != null)) {
                // Reborn tests each block centre against the dragon's actual position.
                // Search one extra block in each axis because the centre can still be
                // inside the radius when its integer block offset is radius + 1.
                int radius = (int) Math.ceil(ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS) + 1;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            Block block = location.getBlock().getRelative(dx, dy, dz);
                            Location blockCenter = block.getLocation().add(0.5D, 0.5D, 0.5D);
                            if (!ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(
                                    location.distanceSquared(blockCenter))) continue;
                            if (block.isLiquid() || block.getRelative(BlockFace.UP).isLiquid()) continue;
                            if (session.dragonEscapeMapName.contains("Hell") && block.getY() < 30) continue;
                            if (session.dragonEscapeMapName.contains("Pirate") && block.getY() < 6) continue;
                            if (!block.getType().isAir()) block.setType(Material.AIR, false);
                        }
                    }
                }
                session.dragonEscapeLastDestructionLocation = location.clone();
            }

            long now = System.currentTimeMillis();
            if (ArcadePortRules.shouldRunDragonEscapeScoreUpdate(
                    now, session.dragonEscapeNextScoreUpdateAt)) {
                session.dragonEscapeNextScoreUpdateAt =
                        ArcadePortRules.nextDragonEscapeScoreUpdateAt(now);
                int lastIndex = session.dragonEscapeWaypoints.size() - 1;
                for (Player player : new ArrayList<>(alivePlayers(session))) {
                    int playerIndex = updateDragonEscapePlayerProgress(session, player, now);
                    if (!session.alive.contains(player.getUniqueId())) continue;
                    Location playerNext = session.dragonEscapeWaypoints.get(Math.min(lastIndex, playerIndex + 1));
                    Location dragonNext = session.dragonEscapeWaypoints.get(Math.min(lastIndex, dragonIndex + 1));
                    double playerScore = ArcadePortRules.dragonEscapeRankScore(
                            playerIndex, player.getLocation().distance(playerNext));
                    double dragonScore = ArcadePortRules.dragonEscapeRankScore(
                            dragonIndex, location.distance(dragonNext));
                    session.dragonEscapePlayerRankScores.put(player.getUniqueId(), playerScore);
                    if (playerIndex == lastIndex && player.getLocation().distance(
                            session.dragonEscapeWaypoints.get(lastIndex))
                            < ArcadePortRules.DRAGON_ESCAPE_FINISH_RADIUS) {
                        recordDragonEscapeFinish(session, player, now);
                        finish(session, player, true);
                        return;
                    }
                    if (dragonScore > playerScore) {
                        eliminate(session, player, null, "was caught by Douglas the Dragon");
                        if (active != session) return;
                    }
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
        long now = System.currentTimeMillis();
        if (ArcadePortRules.shouldRunDragonEscapeScoreUpdate(
                now, session.dragonEscapeNextScoreUpdateAt)) {
            session.dragonEscapeNextScoreUpdateAt =
                    ArcadePortRules.nextDragonEscapeScoreUpdateAt(now);
            for (Player racer : alivePlayers(session)) {
                session.dragonEscapePlayerRankScores.put(
                        racer.getUniqueId(), racer.getLocation().getZ());
            }
        }
        double dragonProgress = location.getZ();
        for (Player player : new ArrayList<>(alivePlayers(session))) {
            if (player.getLocation().getZ() >= 78.0D) {
                recordDragonEscapeFinish(session, player, now);
                finish(session, player, true);
                return;
            }
            if (ArcadePortRules.shouldDragonEscapePlayerBeCaught(dragonProgress, player.getLocation().getZ())) {
                eliminate(session, player, null, "was caught by Douglas the Dragon");
                if (active != session) return;
            }
        }
    }

    private void recordDragonEscapeFinish(Session session, Player player, long now) {
        if (session == null || player == null || session.dragonEscapeMapResource == null) return;
        UUID id = player.getUniqueId();
        long elapsed = Math.max(1L, now - session.startedAt);
        Map<Integer, Long> splits = session.dragonEscapeRunSplitMillis.computeIfAbsent(id, ignored -> new HashMap<>());
        splits.putIfAbsent(0, 0L);
        splits.put(session.dragonEscapeWaypoints.size(), elapsed);
        String path = dragonEscapePbPath(id, session.dragonEscapeMapResource);
        if (path.isEmpty()) return;
        long existing = plugin.getConfig().getLong(path + "time-ms", -1L);
        if (existing > 0L && existing <= elapsed) return;
        Location finishLocation = player.getLocation().clone();
        List<DragonEscapeGhostFrame> ghostFrames = session.dragonEscapeRunGhostFrames
                .computeIfAbsent(id, ignored -> new ArrayList<>());
        appendDragonEscapeGhostFrame(ghostFrames, dragonEscapeGhostFrame(player, elapsed, finishLocation));
        saveDragonEscapeGhostFrames(id, session.dragonEscapeMapResource, ghostFrames);
        plugin.getConfig().set(path + "time-ms", elapsed);
        plugin.getConfig().set(path + "map-resource", session.dragonEscapeMapResource);
        plugin.getConfig().set(path + "map-name", session.dragonEscapeMapName);
        // Store each runner's actual GREEN start and exact finish so their PB ghost replays
        // their own lane rather than the shared RED dragon marker.
        Location runStart = session.dragonEscapeStartLocations.get(id);
        if (runStart == null) runStart = session.dragonEscapeStartLocation;
        if (runStart != null) {
            plugin.getConfig().set(path + "start.x", runStart.getX());
            plugin.getConfig().set(path + "start.y", runStart.getY());
            plugin.getConfig().set(path + "start.z", runStart.getZ());
        }
        plugin.getConfig().set(path + "finish.x", finishLocation.getX());
        plugin.getConfig().set(path + "finish.y", finishLocation.getY());
        plugin.getConfig().set(path + "finish.z", finishLocation.getZ());
        Map<Integer, Location> splitLocations = session.dragonEscapeRunSplitLocations.computeIfAbsent(
                id, ignored -> new HashMap<>());
        if (runStart != null) splitLocations.putIfAbsent(0, runStart.clone());
        splitLocations.put(session.dragonEscapeWaypoints.size(), finishLocation);
        plugin.getConfig().set(path + "splits", null);
        for (Map.Entry<Integer, Long> split : splits.entrySet()) {
            plugin.getConfig().set(path + "splits." + split.getKey(), split.getValue());
        }
        plugin.getConfig().set(path + "split-locations", null);
        for (Map.Entry<Integer, Location> split : splitLocations.entrySet()) {
            Location location = split.getValue();
            if (location == null) continue;
            String splitPath = path + "split-locations." + split.getKey();
            plugin.getConfig().set(splitPath + ".x", location.getX());
            plugin.getConfig().set(splitPath + ".y", location.getY());
            plugin.getConfig().set(splitPath + ".z", location.getZ());
        }
        plugin.saveConfig();
        player.sendMessage(ChatColor.GOLD + "New Dragon Escape PB on " + session.dragonEscapeMapName + ": "
                + ChatColor.YELLOW + ArcadePortRules.formatDragonEscapeTime(elapsed) + ChatColor.GRAY + ".");
    }

    private String dragonEscapePbDeltaText(Session session, Player player, long now) {
        if (session.dragonEscapeMapResource == null) return "";
        long pb = dragonEscapePersonalBestMillis(player.getUniqueId(), session.dragonEscapeMapResource);
        if (pb <= 0L) return ChatColor.DARK_GRAY + "PB: not set";
        long elapsed = Math.max(0L, now - session.startedAt);
        long expected = dragonEscapeExpectedPbMillis(session, player, pb);
        long delta = elapsed - expected;
        return ChatColor.GRAY + "PB " + (delta < 0L ? ChatColor.GREEN : delta > 0L ? ChatColor.RED : ChatColor.YELLOW)
                + ArcadePortRules.formatDragonEscapeDelta(delta);
    }

    private Location dragonEscapePersonalBestStartLocation(
            Session session, UUID playerId, String mapResource) {
        String path = dragonEscapePbPath(playerId, mapResource);
        if (!path.isEmpty()
                && plugin.getConfig().contains(path + "start.x")
                && plugin.getConfig().contains(path + "start.y")
                && plugin.getConfig().contains(path + "start.z")) {
            double x = plugin.getConfig().getDouble(path + "start.x");
            double y = plugin.getConfig().getDouble(path + "start.y");
            double z = plugin.getConfig().getDouble(path + "start.z");
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
                return new Location(session.world, x, y, z);
            }
        }
        // Older PB records predate per-player starts. The current GREEN start is a
        // better lane-compatible fallback than the shared RED Douglas spawn.
        Location currentRunStart = session.dragonEscapeStartLocations.get(playerId);
        if (currentRunStart != null) return currentRunStart.clone();
        return session.dragonEscapeStartLocation == null
                ? session.dragonEscapeWaypoints.get(0).clone()
                : session.dragonEscapeStartLocation.clone();
    }

    private Location dragonEscapePersonalBestFinishLocation(
            Session session, UUID playerId, String mapResource) {
        String path = dragonEscapePbPath(playerId, mapResource);
        if (path.isEmpty()
                || !plugin.getConfig().contains(path + "finish.x")
                || !plugin.getConfig().contains(path + "finish.y")
                || !plugin.getConfig().contains(path + "finish.z")) return null;
        double x = plugin.getConfig().getDouble(path + "finish.x");
        double y = plugin.getConfig().getDouble(path + "finish.y");
        double z = plugin.getConfig().getDouble(path + "finish.z");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
        return new Location(session.world, x, y, z);
    }

    private Location dragonEscapePersonalBestSplitLocation(
            Session session, UUID playerId, String mapResource,
            int splitIndex, Location fallback) {
        String path = dragonEscapePbPath(playerId, mapResource)
                + "split-locations." + splitIndex;
        if (!plugin.getConfig().contains(path + ".x")
                || !plugin.getConfig().contains(path + ".y")
                || !plugin.getConfig().contains(path + ".z")) {
            return fallback == null ? null : fallback.clone();
        }
        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return fallback == null ? null : fallback.clone();
        }
        return new Location(session.world, x, y, z);
    }

    /**
     * Split keys mark nearest-waypoint transitions: key 0 is the player's
     * GREEN start, keys 1..N-1 are checkpoint transitions, and key N is finish.
     * New PBs store the exact split positions; older PBs fall back to route midpoints.
     */
    private Location dragonEscapeSplitBoundaryLocation(
            Session session, int splitIndex, Location startLocation, Location finishLocation) {
        List<Location> route = session.dragonEscapeWaypoints;
        int count = route.size();
        if (count == 0) return null;
        if (splitIndex <= 0) {
            return startLocation == null ? route.get(0).clone() : startLocation.clone();
        }
        if (splitIndex >= count) {
            return finishLocation == null ? route.get(count - 1).clone() : finishLocation.clone();
        }
        Location before = route.get(splitIndex - 1);
        Location after = route.get(splitIndex);
        return before.clone().add(
                (after.getX() - before.getX()) * 0.5D,
                (after.getY() - before.getY()) * 0.5D,
                (after.getZ() - before.getZ()) * 0.5D);
    }

    private double dragonEscapeCourseDistance(Session session, Player player) {
        List<Location> route = session.dragonEscapeWaypoints;
        if (route.isEmpty()) return player.getLocation().getZ();
        int count = route.size();
        int progress = Math.max(0, Math.min(count - 1,
                dragonEscapePlayerProgress(session, player)));
        UUID playerId = player.getUniqueId();
        Location runStart = session.dragonEscapeStartLocations.get(playerId);
        if (runStart == null) runStart = session.dragonEscapeStartLocation;
        Map<Integer, Location> liveSplitLocations =
                session.dragonEscapeRunSplitLocations.getOrDefault(playerId, Map.of());
        double distance = 0.0D;
        for (int split = 0; split < progress; split++) {
            Location fallbackFrom = dragonEscapeSplitBoundaryLocation(session, split, runStart, null);
            Location fallbackTo = dragonEscapeSplitBoundaryLocation(session, split + 1, runStart, null);
            Location from = liveSplitLocations.getOrDefault(split, fallbackFrom);
            Location to = liveSplitLocations.getOrDefault(split + 1, fallbackTo);
            distance += from.distance(to);
        }
        Location fallbackFrom = dragonEscapeSplitBoundaryLocation(session, progress, runStart, null);
        Location fallbackTo = dragonEscapeSplitBoundaryLocation(session, progress + 1, runStart, null);
        Location from = liveSplitLocations.getOrDefault(progress, fallbackFrom);
        Location to = liveSplitLocations.getOrDefault(progress + 1, fallbackTo);
        double segmentLength = from.distance(to);
        if (segmentLength > 0.001D) {
            double fraction = Math.max(0.0D, Math.min(1.0D,
                    1.0D - player.getLocation().distance(to) / segmentLength));
            distance += segmentLength * fraction;
        }
        return distance;
    }

    private long dragonEscapeExpectedPbMillis(Session session, Player player, long totalPb) {
        int count = session.dragonEscapeWaypoints.size();
        if (count == 0) {
            double fraction = Math.max(0.0D, Math.min(1.0D, (player.getLocation().getZ() + 24.0D) / 104.0D));
            return Math.round(totalPb * fraction);
        }

        int progress = Math.max(0, Math.min(count - 1, dragonEscapePlayerProgress(session, player)));
        int segment = ArcadePortRules.dragonEscapePbSegmentStartIndex(progress, count);
        if (segment < 0) return 0L;
        Map<Integer, Long> splits = dragonEscapePersonalBestSplits(
                player.getUniqueId(), session.dragonEscapeMapResource);
        long fromTime = splits.getOrDefault(segment,
                ArcadePortRules.dragonEscapeDefaultSplitMillis(totalPb, segment, count));
        long toTime = splits.getOrDefault(segment + 1,
                ArcadePortRules.dragonEscapeDefaultSplitMillis(totalPb, segment + 1, count));
        Location start = dragonEscapePersonalBestStartLocation(
                session, player.getUniqueId(), session.dragonEscapeMapResource);
        Location finish = dragonEscapePersonalBestFinishLocation(
                session, player.getUniqueId(), session.dragonEscapeMapResource);
        Location from = dragonEscapePersonalBestSplitLocation(
                session, player.getUniqueId(), session.dragonEscapeMapResource, segment,
                dragonEscapeSplitBoundaryLocation(session, segment, start, finish));
        Location to = dragonEscapePersonalBestSplitLocation(
                session, player.getUniqueId(), session.dragonEscapeMapResource, segment + 1,
                dragonEscapeSplitBoundaryLocation(session, segment + 1, start, finish));
        double segmentLength = from.distance(to);
        double fraction = segmentLength < 0.001D ? 0.0D
                : 1.0D - player.getLocation().distance(to) / segmentLength;
        return ArcadePortRules.interpolateDragonEscapeSplitMillis(fromTime, toTime, fraction);
    }

    private static final int DRAGON_ESCAPE_GHOST_FILE_MAGIC = 0x44454748; // "DEGH"
    private static final int DRAGON_ESCAPE_GHOST_FILE_VERSION = 1;
    private static final int DRAGON_ESCAPE_GHOST_MAX_FRAMES = 120_000;

    private void recordDragonEscapeGhostFrames(Session session, long now) {
        long elapsed = Math.max(0L, now - session.startedAt);
        for (UUID id : new ArrayList<>(session.alive)) {
            Player player = session.players.get(id);
            if (player == null || !player.isOnline() || player.getWorld() != session.world) continue;
            List<DragonEscapeGhostFrame> frames = session.dragonEscapeRunGhostFrames
                    .computeIfAbsent(id, ignored -> new ArrayList<>());
            appendDragonEscapeGhostFrame(frames, dragonEscapeGhostFrame(player, elapsed, player.getLocation()));
        }
    }

    private DragonEscapeGhostFrame dragonEscapeGhostFrame(Player player, long elapsed, Location location) {
        return new DragonEscapeGhostFrame(Math.max(0L, elapsed),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(),
                player.isSneaking(), player.isSprinting(), player.isOnGround());
    }

    private void appendDragonEscapeGhostFrame(
            List<DragonEscapeGhostFrame> frames, DragonEscapeGhostFrame frame) {
        if (frames == null || frame == null) return;
        if (!frames.isEmpty() && frame.timeMillis() <= frames.get(frames.size() - 1).timeMillis()) return;
        frames.add(frame);
        if (frames.size() > DRAGON_ESCAPE_GHOST_MAX_FRAMES) {
            List<DragonEscapeGhostFrame> compacted = new ArrayList<>(frames.size() / 2 + 1);
            for (int i = 0; i < frames.size(); i += 2) compacted.add(frames.get(i));
            DragonEscapeGhostFrame last = frames.get(frames.size() - 1);
            if (compacted.get(compacted.size() - 1) != last) compacted.add(last);
            frames.clear();
            frames.addAll(compacted);
        }
    }

    private String dragonEscapeGhostCacheKey(UUID playerId, String mapResource) {
        String mapKey = ArcadePortRules.dragonEscapeMapStorageKey(mapResource);
        return playerId == null || mapKey.isBlank() ? "" : playerId + ":" + mapKey;
    }

    private Path dragonEscapeGhostFile(UUID playerId, String mapResource) {
        String mapKey = ArcadePortRules.dragonEscapeMapStorageKey(mapResource);
        if (playerId == null || mapKey.isBlank()) return null;
        String safeMapKey = mapKey.replaceAll("[^A-Za-z0-9._-]", "_");
        return plugin.getDataFolder().toPath().resolve("dragon-escape-ghosts")
                .resolve(playerId.toString()).resolve(safeMapKey + ".ghost.gz");
    }

    private void saveDragonEscapeGhostFrames(
            UUID playerId, String mapResource, List<DragonEscapeGhostFrame> frames) {
        if (frames == null || frames.isEmpty()) return;
        Path target = dragonEscapeGhostFile(playerId, mapResource);
        String cacheKey = dragonEscapeGhostCacheKey(playerId, mapResource);
        if (target == null || cacheKey.isEmpty()) return;
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                    new GZIPOutputStream(Files.newOutputStream(temporary))))) {
                output.writeInt(DRAGON_ESCAPE_GHOST_FILE_MAGIC);
                output.writeInt(DRAGON_ESCAPE_GHOST_FILE_VERSION);
                output.writeInt(frames.size());
                for (DragonEscapeGhostFrame frame : frames) {
                    output.writeLong(frame.timeMillis());
                    output.writeDouble(frame.x());
                    output.writeDouble(frame.y());
                    output.writeDouble(frame.z());
                    output.writeFloat(frame.yaw());
                    output.writeFloat(frame.pitch());
                    output.writeBoolean(frame.sneaking());
                    output.writeBoolean(frame.sprinting());
                    output.writeBoolean(frame.onGround());
                }
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            dragonEscapeGhostFrameCache.put(cacheKey, List.copyOf(frames));
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            plugin.getLogger().warning("Could not save Dragon Escape PB ghost path for " + playerId
                    + " on " + mapResource + ": " + exception.getMessage());
        }
    }

    private List<DragonEscapeGhostFrame> loadDragonEscapeGhostFrames(UUID playerId, String mapResource) {
        String cacheKey = dragonEscapeGhostCacheKey(playerId, mapResource);
        if (cacheKey.isEmpty()) return List.of();
        List<DragonEscapeGhostFrame> cached = dragonEscapeGhostFrameCache.get(cacheKey);
        if (cached != null) return cached;
        Path file = dragonEscapeGhostFile(playerId, mapResource);
        if (file == null || !Files.isRegularFile(file)) return List.of();
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(file))))) {
            if (input.readInt() != DRAGON_ESCAPE_GHOST_FILE_MAGIC
                    || input.readInt() != DRAGON_ESCAPE_GHOST_FILE_VERSION) return List.of();
            int count = input.readInt();
            if (count < 1 || count > DRAGON_ESCAPE_GHOST_MAX_FRAMES) return List.of();
            List<DragonEscapeGhostFrame> frames = new ArrayList<>(count);
            long previousTime = -1L;
            for (int i = 0; i < count; i++) {
                DragonEscapeGhostFrame frame = new DragonEscapeGhostFrame(
                        input.readLong(), input.readDouble(), input.readDouble(), input.readDouble(),
                        input.readFloat(), input.readFloat(),
                        input.readBoolean(), input.readBoolean(), input.readBoolean());
                if (frame.timeMillis() < previousTime
                        || !Double.isFinite(frame.x()) || !Double.isFinite(frame.y())
                        || !Double.isFinite(frame.z()) || !Float.isFinite(frame.yaw())
                        || !Float.isFinite(frame.pitch())) return List.of();
                previousTime = frame.timeMillis();
                frames.add(frame);
            }
            List<DragonEscapeGhostFrame> loaded = List.copyOf(frames);
            dragonEscapeGhostFrameCache.put(cacheKey, loaded);
            return loaded;
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().warning("Could not read Dragon Escape PB ghost path for " + playerId
                    + " on " + mapResource + ": " + exception.getMessage());
            return List.of();
        }
    }

    private DragonEscapeGhostFrame dragonEscapeGhostFrameAt(List<DragonEscapeGhostFrame> frames, long elapsed) {
        if (frames == null || frames.isEmpty()) return null;
        if (elapsed <= frames.get(0).timeMillis()) return frames.get(0);
        int lastIndex = frames.size() - 1;
        if (elapsed >= frames.get(lastIndex).timeMillis()) return frames.get(lastIndex);
        int low = 0;
        int high = lastIndex;
        while (low + 1 < high) {
            int middle = (low + high) >>> 1;
            if (frames.get(middle).timeMillis() <= elapsed) low = middle;
            else high = middle;
        }
        DragonEscapeGhostFrame from = frames.get(low);
        DragonEscapeGhostFrame to = frames.get(high);
        long duration = Math.max(1L, to.timeMillis() - from.timeMillis());
        double fraction = Math.max(0.0D, Math.min(1.0D,
                (elapsed - from.timeMillis()) / (double) duration));
        double yawDelta = ((to.yaw() - from.yaw() + 540.0D) % 360.0D) - 180.0D;
        boolean useNextPose = fraction >= 0.5D;
        return new DragonEscapeGhostFrame(elapsed,
                ArcadePortRules.interpolateDragonEscapeCoordinate(from.x(), to.x(), fraction),
                ArcadePortRules.interpolateDragonEscapeCoordinate(from.y(), to.y(), fraction),
                ArcadePortRules.interpolateDragonEscapeCoordinate(from.z(), to.z(), fraction),
                (float) (from.yaw() + yawDelta * fraction),
                (float) (from.pitch() + (to.pitch() - from.pitch()) * fraction),
                useNextPose ? to.sneaking() : from.sneaking(),
                useNextPose ? to.sprinting() : from.sprinting(),
                useNextPose ? to.onGround() : from.onGround());
    }

    private DragonEscapeGhostFrame dragonEscapeLegacyGhostFrame(
            Session session, UUID playerId, long elapsed, long pb) {
        Location location = dragonEscapeGhostLocation(session, playerId, elapsed);
        if (location == null) return null;
        Location ahead = dragonEscapeGhostLocation(session, playerId, Math.min(pb, elapsed + 100L));
        if (ahead != null) {
            Vector direction = ahead.toVector().subtract(location.toVector());
            if (direction.lengthSquared() > 0.0001D) location.setDirection(direction);
        }
        return new DragonEscapeGhostFrame(elapsed,
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(),
                false, false, false);
    }

    private void updateDragonEscapeGhosts(Session session, long now) {
        DragonEscapeGhostBridge bridge = dragonEscapeGhostBridge;
        for (Player viewer : session.players.values()) {
            if (viewer == null) continue;
            UUID id = viewer.getUniqueId();
            if (bridge == null || !viewer.isOnline() || !session.alive.contains(id)
                    || !isDragonEscapeGhostEnabled(id)) {
                hideDragonEscapeGhost(viewer);
                continue;
            }
            long pb = dragonEscapePersonalBestMillis(id, session.dragonEscapeMapResource);
            long elapsed = Math.max(0L, now - session.startedAt);
            if (pb <= 0L || elapsed >= pb) {
                bridge.hide(viewer);
                continue;
            }
            List<DragonEscapeGhostFrame> recorded = loadDragonEscapeGhostFrames(id, session.dragonEscapeMapResource);
            DragonEscapeGhostFrame frame = recorded.isEmpty()
                    ? dragonEscapeLegacyGhostFrame(session, id, elapsed, pb)
                    : dragonEscapeGhostFrameAt(recorded, elapsed);
            if (frame == null) {
                bridge.hide(viewer);
                continue;
            }
            Location ghostLocation = new Location(session.world,
                    frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch());
            bridge.update(viewer, ghostLocation, frame.sneaking(), frame.sprinting(), frame.onGround());
        }
    }

    private Location dragonEscapeGhostLocation(Session session, UUID playerId, long elapsed) {
        if (session.dragonEscapeMapResource == null || session.dragonEscapeWaypoints.isEmpty()) return null;
        long pb = dragonEscapePersonalBestMillis(playerId, session.dragonEscapeMapResource);
        if (pb <= 0L || elapsed >= pb) return null;
        Map<Integer, Long> splits = dragonEscapePersonalBestSplits(playerId, session.dragonEscapeMapResource);
        int count = session.dragonEscapeWaypoints.size();
        Location start = dragonEscapePersonalBestStartLocation(session, playerId, session.dragonEscapeMapResource);
        Location finish = dragonEscapePersonalBestFinishLocation(session, playerId, session.dragonEscapeMapResource);
        for (int segment = 0; segment < count; segment++) {
            long fromTime = splits.getOrDefault(segment,
                    ArcadePortRules.dragonEscapeDefaultSplitMillis(pb, segment, count));
            long toTime = splits.getOrDefault(segment + 1,
                    ArcadePortRules.dragonEscapeDefaultSplitMillis(pb, segment + 1, count));
            if (toTime <= fromTime) toTime = fromTime + 1L;
            if (elapsed > toTime) continue;
            double fraction = Math.max(0.0D, Math.min(1.0D,
                    (elapsed - fromTime) / (double) (toTime - fromTime)));
            Location from = dragonEscapePersonalBestSplitLocation(
                    session, playerId, session.dragonEscapeMapResource, segment,
                    dragonEscapeSplitBoundaryLocation(session, segment, start, finish));
            Location to = dragonEscapePersonalBestSplitLocation(
                    session, playerId, session.dragonEscapeMapResource, segment + 1,
                    dragonEscapeSplitBoundaryLocation(session, segment + 1, start, finish));
            return new Location(session.world,
                    ArcadePortRules.interpolateDragonEscapeCoordinate(from.getX(), to.getX(), fraction),
                    ArcadePortRules.interpolateDragonEscapeCoordinate(from.getY(), to.getY(), fraction),
                    ArcadePortRules.interpolateDragonEscapeCoordinate(from.getZ(), to.getZ(), fraction));
        }
        return finish == null ? session.dragonEscapeWaypoints.get(count - 1).clone() : finish;
    }

    private Color dragonEscapeBeaconRgb(String color) {
        return switch (color) {
            case "red" -> Color.fromRGB(255, 55, 55);
            case "orange" -> Color.fromRGB(255, 145, 35);
            case "yellow" -> Color.fromRGB(255, 235, 65);
            case "lime" -> Color.fromRGB(150, 255, 50);
            case "green" -> Color.fromRGB(45, 220, 80);
            case "cyan" -> Color.fromRGB(40, 245, 225);
            case "light-blue" -> Color.fromRGB(100, 190, 255);
            case "blue" -> Color.fromRGB(55, 90, 255);
            case "purple" -> Color.fromRGB(160, 75, 255);
            case "magenta" -> Color.fromRGB(255, 60, 225);
            case "pink" -> Color.fromRGB(255, 130, 185);
            default -> Color.fromRGB(245, 245, 245);
        };
    }

    private List<Player> dragonEscapeRaceOrder(Session session) {
        List<Player> racers = new ArrayList<>(alivePlayers(session));
        if (!session.dragonEscapeWaypoints.isEmpty()) {
            racers.sort((left, right) -> {
                double leftScore = session.dragonEscapePlayerRankScores.getOrDefault(
                        left.getUniqueId(), dragonEscapeFallbackRankScore(session, left));
                double rightScore = session.dragonEscapePlayerRankScores.getOrDefault(
                        right.getUniqueId(), dragonEscapeFallbackRankScore(session, right));
                return Double.compare(rightScore, leftScore);
            });
        } else {
            racers.sort((left, right) -> Integer.compare(
                    right.getLocation().getBlockZ(), left.getLocation().getBlockZ()));
        }
        return racers;
    }

    private double dragonEscapeFallbackRankScore(Session session, Player player) {
        int progress = dragonEscapePlayerProgress(session, player);
        int next = Math.min(session.dragonEscapeWaypoints.size() - 1, Math.max(0, progress + 1));
        return ArcadePortRules.dragonEscapeRankScore(
                progress, player.getLocation().distance(session.dragonEscapeWaypoints.get(next)));
    }

    private long dragonEscapeRaceGapMillis(Session session, Player leader, Player racer, long now) {
        if (leader == null || racer == null || !leader.isOnline() || !racer.isOnline()) return 0L;
        if (session.dragonEscapeWaypoints.isEmpty()) {
            double gap = Math.max(0.0D,
                    leader.getLocation().getZ() - racer.getLocation().getZ());
            return Math.round(gap / ArcadePortRules.DRAGON_ESCAPE_RACE_GAP_ESTIMATED_SPEED * 1_000.0D);
        }
        double leaderDistance = dragonEscapeCourseDistance(session, leader);
        double racerDistance = dragonEscapeCourseDistance(session, racer);
        return ArcadePortRules.dragonEscapeCourseTimeGapMillis(
                leaderDistance, racerDistance, ArcadePortRules.DRAGON_ESCAPE_RACE_GAP_ESTIMATED_SPEED);
    }

    private void setupDragonEscapeRaceScoreboard(Session session) {
        if (session.type != ArcadePortGame.DRAGON_ESCAPE || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("dragonrace", "dummy",
                ChatColor.GOLD + "DRAGON ESCAPE");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.dragonEscapeScoreboard = scoreboard;
        session.dragonEscapeScoreboardObjective = objective;
        for (int i = 0; i < 14; i++) {
            Team line = scoreboard.registerNewTeam("derank" + i);
            String entry = ChatColor.values()[i].toString();
            line.addEntry(entry);
            session.dragonEscapeScoreboardTeams.add(line);
            session.dragonEscapeScoreboardEntries.add(entry);
        }
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateDragonEscapeRaceScoreboard(session, System.currentTimeMillis());
    }

    /** Reborn shows the top 14 ranked racers; the modern sidebar also shows live race times. */
    private void updateDragonEscapeRaceScoreboard(Session session, long now) {
        Scoreboard scoreboard = session.dragonEscapeScoreboard;
        Objective objective = session.dragonEscapeScoreboardObjective;
        if (scoreboard == null || objective == null || session.type != ArcadePortGame.DRAGON_ESCAPE) return;
        List<Player> ranked = new ArrayList<>();
        for (Player player : session.players.values()) {
            if (player != null && session.dragonEscapePlayerRankScores.containsKey(player.getUniqueId())) {
                ranked.add(player);
            }
        }
        ranked.sort((left, right) -> Double.compare(
                session.dragonEscapePlayerRankScores.get(right.getUniqueId()),
                session.dragonEscapePlayerRankScores.get(left.getUniqueId())));
        List<Player> liveOrder = dragonEscapeRaceOrder(session);
        Player liveLeader = liveOrder.isEmpty() ? null : liveOrder.get(0);
        long elapsed = Math.max(0L, now - session.startedAt);
        for (int i = 0; i < session.dragonEscapeScoreboardTeams.size(); i++) {
            Team line = session.dragonEscapeScoreboardTeams.get(i);
            String entry = session.dragonEscapeScoreboardEntries.get(i);
            if (i < ranked.size()) {
                Player racer = ranked.get(i);
                boolean alive = session.alive.contains(racer.getUniqueId());
                ChatColor colour = alive ? ChatColor.GREEN : ChatColor.RED;
                line.setPrefix(colour + "#" + (i + 1) + " " + racer.getName());
                if (!alive) {
                    line.setSuffix(ChatColor.DARK_RED + " OUT");
                } else if (liveLeader != null
                        && racer.getUniqueId().equals(liveLeader.getUniqueId())) {
                    line.setSuffix(ChatColor.YELLOW + " " + ArcadePortRules.formatDragonEscapeTime(elapsed));
                } else if (liveLeader != null) {
                    long gap = dragonEscapeRaceGapMillis(session, liveLeader, racer, now);
                    line.setSuffix(ChatColor.RED + " ~+" + ArcadePortRules.formatDragonEscapeTime(gap));
                } else {
                    line.setSuffix("");
                }
                objective.getScore(entry).setScore(14 - i);
            } else {
                line.setPrefix("");
                line.setSuffix("");
                scoreboard.resetScores(entry);
            }
        }
    }

    private void cleanupDragonEscapeRaceScoreboard(Session session) {
        Scoreboard scoreboard = session.dragonEscapeScoreboard;
        Objective objective = session.dragonEscapeScoreboardObjective;
        if (scoreboard == null) return;
        try {
            for (Team team : new ArrayList<>(session.dragonEscapeScoreboardTeams)) {
                if (scoreboard.getTeam(team.getName()) != null) team.unregister();
            }
            if (objective != null && scoreboard.getObjective(objective.getName()) != null) {
                objective.unregister();
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            plugin.getLogger().fine("Dragon Escape scoreboard was already removed: " + exception.getMessage());
        }
        session.dragonEscapeScoreboardTeams.clear();
        session.dragonEscapeScoreboardEntries.clear();
        session.dragonEscapeScoreboardObjective = null;
        session.dragonEscapeScoreboard = null;
    }

    private void updateDragonEscapeRaceUi(Session session, long now) {
        if (session.ticks % 10 == 0) updateDragonEscapeRaceScoreboard(session, now);
        List<Player> order = dragonEscapeRaceOrder(session);
        if (order.isEmpty()) return;
        Player leader = order.get(0);
        UUID leaderId = leader.getUniqueId();
        String leaderColor = dragonEscapeBeaconColor(leaderId);
        if (!leaderId.equals(session.dragonEscapeBeaconLeader)) {
            session.dragonEscapeBeaconLeader = leaderId;
            session.dragonEscapeBeaconColor = leaderColor;
            announce(session, ChatColor.GOLD + leader.getName() + ChatColor.YELLOW
                    + " is now leading! Beacon colour: " + leaderColor + ".");
        } else {
            session.dragonEscapeBeaconColor = leaderColor;
        }
        long elapsed = Math.max(0L, now - session.startedAt);
        for (int i = 0; i < order.size(); i++) {
            Player racer = order.get(i);
            String prefix;
            if (i == 0) {
                prefix = ChatColor.GOLD + "#1 " + ChatColor.WHITE + racer.getName()
                        + ChatColor.YELLOW + " " + ArcadePortRules.formatDragonEscapeTime(elapsed);
            } else {
                long gap = dragonEscapeRaceGapMillis(session, leader, racer, now);
                prefix = ChatColor.GRAY + "#" + (i + 1) + " " + ChatColor.WHITE + racer.getName()
                        + ChatColor.RED + " ~+" + ArcadePortRules.formatDragonEscapeTime(gap);
            }
            String tabName = prefix.length() > 64 ? prefix.substring(0, 64) : prefix;
            if (!tabName.equals(racer.getPlayerListName())) racer.setPlayerListName(tabName);
        }
        if (session.ticks % 4 != 0) return;
        // The PB itself is rendered by the packet-only translucent player model.
        // Keep these particles exclusively for the race-leader beacon.
        Color beaconRgb = dragonEscapeBeaconRgb(session.dragonEscapeBeaconColor);
        Particle.DustOptions beaconDust = new Particle.DustOptions(beaconRgb, 1.7F);
        Location beacon = leader.getLocation().clone().add(0.0D, 1.4D, 0.0D);
        for (Player viewer : session.players.values()) {
            if (viewer == null || !viewer.isOnline() || viewer.getWorld() != session.world) continue;
            for (double y : new double[] {0.0D, 1.8D, 3.6D, 5.4D}) {
                viewer.spawnParticle(Particle.DUST, beacon.clone().add(0.0D, y, 0.0D), 7,
                        0.08D, 0.35D, 0.08D, 0.0D, beaconDust);
            }
        }
    }

    private int getDragonEscapeWaypointIndex(Session session, Location location, int currentProgress) {
        int lastIndex = session.dragonEscapeWaypoints.size() - 1;
        if (lastIndex < 0) return -1;
        int current = Math.max(0, Math.min(lastIndex, currentProgress));
        if (current >= lastIndex || location == null || location.getWorld() != session.world) return current;
        Location currentMarker = session.dragonEscapeWaypoints.get(current);
        Location nextMarker = session.dragonEscapeWaypoints.get(current + 1);
        double distanceToCurrent = location.distance(currentMarker);
        double distanceToNext = location.distance(nextMarker);
        double spacing = currentMarker.distance(nextMarker);
        return ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(
                distanceToCurrent, distanceToNext, spacing) ? current + 1 : current;
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
        long now = System.currentTimeMillis();
        long seconds = Math.max(0L, (session.endsAt - now) / 1000L);
        if (session.type == ArcadePortGame.BACON_BRAWL) updateBaconMeterScoreboard(session);
        if (session.type == ArcadePortGame.DRAGON_ESCAPE) updateDragonEscapeRaceUi(session, now);
        for (UUID id : session.players.keySet()) {
            Player p = session.players.get(id);
            if (!p.isOnline()) continue;
            if (session.type == ArcadePortGame.BACON_BRAWL
                    && !session.alive.contains(id)
                    && isBaconTrackingCompass(p.getInventory().getItemInMainHand())) {
                Player target = nearestAliveBaconTarget(session, p);
                if (target != null) {
                    double distance = Math.sqrt(target.getLocation().distanceSquared(p.getLocation()));
                    double height = target.getLocation().getY() - p.getLocation().getY();
                    p.sendActionBar(ChatColor.WHITE + "Nearest Target: " + ChatColor.YELLOW + target.getName()
                            + ChatColor.WHITE + " | Distance: " + ChatColor.YELLOW
                            + String.format(java.util.Locale.ROOT, "%.1f", distance)
                            + ChatColor.WHITE + " | Height: " + ChatColor.YELLOW
                            + String.format(java.util.Locale.ROOT, "%.1f", height));
                    continue;
                }
            }
            if (session.type == ArcadePortGame.BACON_BRAWL) {
                updateBaconMeterItem(session, p);
                if (now < session.baconHitFeedbackUntil.getOrDefault(id, 0L)) continue;

                // Keep all Bacon Brawl combat information visible in one compact action-bar line.
                // This avoids the normal score field pushing the live timing meter off the end.
                int meter = session.baconMeters.getOrDefault(id, 0);
                String baconTimeText = session.endsAt > 0L ? seconds + "s" : "survive";
                String knockback = String.format(java.util.Locale.ROOT, "%.1f", session.baconKnockback);
                p.sendActionBar(ChatColor.GOLD + "BACON" + ChatColor.GRAY + " | " + baconTimeText
                        + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + "alive " + alive
                        + ChatColor.DARK_GRAY + " | " + ChatColor.AQUA + "meter " + meter + "%"
                        + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + "kb x" + knockback
                        + ChatColor.DARK_GRAY + " | " + baconTimingStatusText(session, id, now));
                continue;
            }
            int score = session.scores.getOrDefault(id, 0);
            String specific = gameStatusText(session, p);
            String timeText = session.type == ArcadePortGame.DRAGON_ESCAPE
                    ? String.format(java.util.Locale.ROOT, "%.1fs",
                            Math.max(0L, now - session.startedAt) / 1_000.0D)
                    : session.endsAt > 0L ? seconds + "s" : "survive";
            if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
                String pbDelta = dragonEscapePbDeltaText(session, p, now);
                if (!pbDelta.isEmpty()) specific += (specific.isEmpty() ? "" : ChatColor.DARK_GRAY + " | ") + pbDelta;
            }
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

    /** Live timing guide: the green section is the 500–900 ms critical window. */
    private String baconTimingStatusText(Session session, UUID id, long now) {
        long lastHit = session.baconLastSwingAt.getOrDefault(id, 0L);
        if (lastHit <= 0L) {
            return ChatColor.GRAY + "timing [" + ChatColor.RED + "■■■■"
                    + ChatColor.GREEN + "■■■■" + ChatColor.YELLOW + "■■■■"
                    + ChatColor.GRAY + "] land first hit";
        }

        long elapsed = Math.max(0L, now - lastHit);
        // 15 ticks × 100 ms gives an exact visual critical segment at 500–900 ms.
        int pointer = (int) Math.min(14L, elapsed / 100L);
        boolean criticalWindow = ArcadePortRules.isBaconTimedCritical(elapsed);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 15; i++) {
            if (i == pointer) {
                bar.append(criticalWindow ? ChatColor.GREEN : ChatColor.WHITE).append("▌");
            } else if (i >= 5 && i <= 8) {
                bar.append(ChatColor.GREEN).append("■");
            } else if (i < 5) {
                bar.append(ChatColor.RED).append("■");
            } else if (i <= 13) {
                bar.append(ChatColor.YELLOW).append("■");
            } else {
                bar.append(ChatColor.GRAY).append("■");
            }
        }
        bar.append(ChatColor.GRAY).append("]");

        String state;
        if (ArcadePortRules.isBaconTimedCritical(elapsed)) {
            state = ChatColor.GREEN + "" + ChatColor.BOLD + "CRITICAL NOW";
        } else if (elapsed < ArcadePortRules.BACON_TIMED_HIT_GOOD_MIN_MILLIS) {
            state = ChatColor.RED + "wait " + (ArcadePortRules.BACON_TIMED_HIT_GOOD_MIN_MILLIS - elapsed) + "ms";
        } else if (elapsed <= ArcadePortRules.BACON_TIMED_HIT_LATE_MAX_MILLIS) {
            state = ChatColor.YELLOW + "late";
        } else {
            state = ChatColor.GRAY + "reset";
        }
        return ChatColor.AQUA + "timing " + bar + " " + state;
    }

    private String gameStatusText(Session session, Player player) {
        UUID id = player.getUniqueId();
        return switch (session.type) {
            case BACON_BRAWL -> "meter " + session.baconMeters.getOrDefault(id, 0) + "% | knockback x"
                    + String.format(java.util.Locale.ROOT, "%.1f", session.baconKnockback)
                    + " | " + baconTimingStatusText(session, id, System.currentTimeMillis());
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
        // Dragon Escape is a course race: one surviving racer is not a round winner.
        // Keep solo practice and last-runner attempts live until the finish is reached.
        if (session.type == ArcadePortGame.DRAGON_ESCAPE) return;
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
        if (session.type == ArcadePortGame.BACON_BRAWL) {
            if (session.participantsAtStart > 1
                    && ArcadePortRules.shouldFinishBaconBrawl(session.alive.size())) {
                finish(session, alivePlayers(session).stream().findFirst().orElse(null), true);
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
                // Source getWinners() only returns the Runners' placements.
                Player winner = runners.size() == 1 ? runners.get(0) : null;
                finish(session, winner, true);
            }
            return;
        }
        if (session.alive.size() <= 1) finish(session, alivePlayers(session).stream().findFirst().orElse(null), true);
    }

    private void eliminate(Session session, Player player, Player killer, String reason) {
        if (active != session || player == null) return;
        if (session.type == ArcadePortGame.BACON_BRAWL
                && session.alive.contains(player.getUniqueId())) {
            UUID playerId = player.getUniqueId();
            if (killer == null && reason != null) {
                boolean fallKnockout = ArcadePortRules.isBaconArenaKnockoutReason(reason);
                long lastHitAt = session.baconLastDamagedAt.getOrDefault(playerId, 0L);
                if (ArcadePortRules.shouldCreditBaconFallKill(
                        System.currentTimeMillis(), lastHitAt, fallKnockout)) {
                    UUID lastAttackerId = session.baconLastDamager.get(playerId);
                    Player lastAttacker = lastAttackerId == null ? null : session.players.get(lastAttackerId);
                    if (lastAttackerId != null && session.scores.containsKey(lastAttackerId)
                            && !lastAttackerId.equals(playerId)) {
                        killer = lastAttacker;
                    }
                }
            }
            String lastHitSource = session.baconLastHitSource.get(playerId);
            if (killer != null && "body-slam".equals(lastHitSource)) {
                UUID killerId = killer.getUniqueId();
                int slamKills = session.baconBodySlamKills.merge(killerId, 1, Integer::sum);
                if (ArcadePortRules.shouldAwardBaconThreeForOne(slamKills)
                        && session.baconThreeForOneAwarded.add(killerId)) {
                    awardBaconAchievement(killer,
                            ArcadePortRules.baconBrawlAchievementConfigKey(killerId, "three-for-one"),
                            "Three for one! — 3 Body Slam kills in one game.");
                }
            }
            session.baconLastDamager.remove(playerId);
            session.baconLastDamagedAt.remove(playerId);
            session.baconLastHitSource.remove(playerId);
            removeBaconBabyPig(session, playerId);
            removeBaconPlayerPig(session, playerId);
            session.baconSlams.remove(playerId);
            session.baconSlamPowerPercent.remove(playerId);
            session.baconBrutusUntil.remove(playerId);
            session.baconCloakUntil.remove(playerId);
            if (!session.alive.remove(playerId)) return;
            if (killer != null && session.scores.containsKey(killer.getUniqueId())) {
                int kills = session.scores.merge(killer.getUniqueId(), 1, Integer::sum);
                killer.sendMessage(ChatColor.GREEN + "Bacon Brawl kill! " + kills + " kills.");
                if (ArcadePortRules.shouldAwardBaconPigsInBlankets(kills)
                        && session.baconPigsInBlanketsAwarded.add(killer.getUniqueId())) {
                    awardBaconAchievement(killer,
                            ArcadePortRules.baconBrawlAchievementConfigKey(killer.getUniqueId(), "pigs-in-blankets"),
                            "Pigs In Blankets — 6 kills in one game.");
                }
            }
            if (player.isOnline()) {
                // Do not leave Brutus' short resistance effect running on an eliminated spectator.
                player.removePotionEffect(PotionEffectType.RESISTANCE);
                player.setGameMode(GameMode.SPECTATOR);
                player.getInventory().clear();
                player.sendMessage(ChatColor.RED + "Knocked out! You are out of Bacon Brawl.");
            }
            // Bacon Brawl is Reborn's last-pig-standing SoloGame: eliminated players do not respawn.
            checkRoundEnd(session);
            return;
        }
        
        if (session.type == ArcadePortGame.BOMB_LOBBERS && session.alive.contains(player.getUniqueId())) {
            UUID victimId = player.getUniqueId();
            if (!session.alive.remove(victimId)) return;
            session.bombDoubleJumpAvailable.remove(victimId);
            player.leaveVehicle();
            if (player.isOnline()) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(ChatColor.RED + "Eliminated: " + reason + ".");
            }
            UUID killerId = killer == null ? null : killer.getUniqueId();
            if (killer != null && session.scores.containsKey(killerId) && !killerId.equals(victimId)) {
                int kills = session.scores.merge(killerId, 1, Integer::sum);
                killer.sendMessage(ChatColor.GREEN + "Bomb Lobbers elimination! " + kills + " kills.");
            }
            recordBombLobbersAssists(session, victimId, killerId, System.currentTimeMillis());
            updateBombLobbersMvpEligibility(session);
            checkRoundEnd(session);
            return;
        }

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
            makeDeathTagChaser(session, player, reason, killer);
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
            if (session.type == ArcadePortGame.MICRO_BATTLES) {
                removeMicroBorderWarning(session, player.getUniqueId());
                restoreMicroMiddleGlassCosmetic(session, player);
                player.getInventory().clear();
                giveMicroTrackingCompass(player);
            }
            player.setGameMode(GameMode.SPECTATOR);
            if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
                player.teleport(dragonEscapeSpectatorLocation(session));
            }
            player.sendMessage(ChatColor.RED + "Eliminated: " + reason + ".");
        }
        if (killer != null && session.scores.containsKey(killer.getUniqueId())) {
            int kills = session.scores.merge(killer.getUniqueId(), 1, Integer::sum);
            if (session.type == ArcadePortGame.MICRO_BATTLES
                    && ArcadePortRules.shouldAwardMicroBattlesAnnihilation(kills)) {
                UUID killerId = killer.getUniqueId();
                awardMicroBattlesAchievement(killer, MicroBattlesAchievement.ANNIHILATION.configKey(killerId),
                        "Annihilation — 8 kills in one Micro Battles game.");
            }
        }
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
                    applyDeathTagDefaultMeleeKnockback(attacker, target, false);
                    double damage = ArcadePortRules.deathTagChaserDamage(
                            session.alphaChasers.contains(attacker.getUniqueId()));
                    if (ArcadePortRules.deathTagHitIsLethal(target.getHealth(), damage)) {
                        makeDeathTagChaser(session, target, "was killed by a chaser", attacker);
                        checkRoundEnd(session);
                    } else {
                        target.setHealth(target.getHealth() - damage);
                        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9F, 0.75F);
                    }
                } else {
                    String kit = session.deathTagRunnerKits.getOrDefault(attacker.getUniqueId(), "basher");
                    if (!"basher".equals(kit) && !"traitor".equals(kit)
                            && !"projectile hit".equals(source)) {
                        applyDeathTagDefaultMeleeKnockback(attacker, target,
                                session.alphaChasers.contains(target.getUniqueId()));
                    }
                    double baseDamage = ArcadePortRules.deathTagRunnerDamage(kit, "projectile hit".equals(source));
                    double damage = ArcadePortRules.deathTagDamageAfterIronSkin(baseDamage,
                            session.alphaChasers.contains(target.getUniqueId()));
                    boolean lethalHit = ArcadePortRules.deathTagHitIsLethal(target.getHealth(), damage);
                    if (damage > 0.0D) {
                        if (lethalHit) {
                            respawnDeathTagChaser(session, target);
                            awardDeathTagKill(session, attacker);
                            recordDeathTagChaserKill(session, attacker);
                        } else {
                            target.setHealth(target.getHealth() - damage);
                            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.05F);
                        }
                    }
                    if (!lethalHit && "basher".equals(kit) && !"projectile hit".equals(source)) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false, true));
                    } else if (!lethalHit && "traitor".equals(kit) && !"projectile hit".equals(source)) {
                        String cooldownKey = attacker.getUniqueId() + ":" + target.getUniqueId();
                        long now = System.currentTimeMillis();
                        long lastUse = session.deathTagTraitorKnockbackAt.getOrDefault(cooldownKey, 0L);
                        if (ArcadePortRules.deathTagTraitorKnockbackReady(now, lastUse)) {
                            session.deathTagTraitorKnockbackAt.put(cooldownKey, now);
                            Vector knockbackDirection = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
                            double multiplier = ArcadePortRules.deathTagKnockbackMultiplier(
                                    session.alphaChasers.contains(target.getUniqueId()));
                            if (knockbackDirection.lengthSquared() > 0.001D) {
                                target.setVelocity(knockbackDirection.normalize().multiply(0.8D * multiplier).setY(0.1D));
                            }
                        }
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
        if (session.type == ArcadePortGame.BACON_BRAWL) {
            if (session.baconHandledProjectiles.remove(projectile.getUniqueId())) return;
            handleBaconBrawlHit(session, shooter, target, "projectile hit",
                    2.0D, projectile.getVelocity());
        } else if (session.type == ArcadePortGame.BOMB_LOBBERS) {
            handlePlayerHit(session, shooter, target, "bomb hit");
        } else if (session.type == ArcadePortGame.PAINTBALL) {
            int paintAmount = session.paintProjectileDamage.getOrDefault(projectile.getUniqueId(), 1);
            session.paintProjectileDamage.remove(projectile.getUniqueId());
            session.paintballProjectileCosmetics.remove(projectile.getUniqueId());
            handlePlayerHit(session, shooter, target, "projectile hit", paintAmount);
        } else if (session.type == ArcadePortGame.DEATH_TAG) {
            if (projectile instanceof Arrow arrow
                    && !session.tagged.contains(shooter.getUniqueId())
                    && session.alive.contains(shooter.getUniqueId())
                    && "archer".equals(session.deathTagRunnerKits.getOrDefault(shooter.getUniqueId(), "basher"))
                    && session.tagged.contains(target.getUniqueId())) {
                handleDeathTagArcherArrowHit(session, arrow, shooter, target);
            } else {
                handlePlayerHit(session, shooter, target, "projectile hit");
            }
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
            if (!ArcadePortRules.deathTagHasRunnerWinner(runners.size())) return null;
            return runners.stream()
                    .max(java.util.Comparator.comparingInt(player ->
                            session.scores.getOrDefault(player.getUniqueId(), 0)))
                    .orElse(null);
        }
        int team = -1;
        if (session.type == ArcadePortGame.TURF_WARS) {
            team = ArcadePortRules.turfWarsTimeoutLeader(session.turfFrontlineX,
                    session.turfInitialFrontlineX, timeoutTeamKills(session, 0), timeoutTeamKills(session, 1));
        } else if (session.type == ArcadePortGame.MICRO_BATTLES) {
            int[] aliveByTeam = new int[4];
            int[] killsByTeam = new int[4];
            for (UUID id : session.alive) {
                int playerTeam = session.teams.getOrDefault(id, -1);
                if (playerTeam >= 0 && playerTeam < aliveByTeam.length) aliveByTeam[playerTeam]++;
            }
            for (int playerTeam = 0; playerTeam < killsByTeam.length; playerTeam++) {
                killsByTeam[playerTeam] = timeoutTeamKills(session, playerTeam);
            }
            team = ArcadePortRules.microBattlesTimeoutTeam(aliveByTeam, killsByTeam);
        } else if (session.type == ArcadePortGame.PAINTBALL
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
        recordLocalStats(session, winner, announceWinner);
        if (session.type == ArcadePortGame.MICRO_BATTLES) {
            removeAllMicroBorderWarnings(session);
            for (Player player : session.players.values()) restoreMicroMiddleGlassCosmetic(session, player);
        }
        if (announceWinner && session.type == ArcadePortGame.BOMB_LOBBERS) {
            recordBombLobbersAchievements(session, winner);
        }
        active = null;
        if (session.tickTask != null) session.tickTask.cancel();
        if (announceWinner && session.type == ArcadePortGame.BACON_BRAWL && winner != null) {
            recordBaconBrawlAchievements(session, winner);
        }
        if (announceWinner) {
            if (session.type == ArcadePortGame.SHEEP_QUEST && winner != null) {
                int winnerTeam = session.teams.getOrDefault(winner.getUniqueId(), -1);
                announce(session, sheepTeamChatColor(winnerTeam) + sheepTeamName(winnerTeam)
                        + ChatColor.GOLD + " team won Sheep Quest!");
            } else if (session.type == ArcadePortGame.BACON_BRAWL) {
                if (winner != null) {
                    int kills = session.scores.getOrDefault(winner.getUniqueId(), 0);
                    announce(session, ChatColor.GOLD + "" + ChatColor.BOLD + winner.getName()
                            + " is the last pig standing!");
                } else {
                    announce(session, ChatColor.YELLOW + "Bacon Brawl ended with no pig left standing.");
                }
            } else {
                announce(session, winner == null ? ChatColor.YELLOW + "Round ended." : ChatColor.GOLD + "" + ChatColor.BOLD + winner.getName() + " won " + session.type.displayName() + "!");
            }
        }
        for (Map.Entry<Block, BlockData> entry : session.deathTagOriginalLightBlocks.entrySet()) {
            Block block = entry.getKey();
            if (block != null && block.getWorld() == session.world) block.setBlockData(entry.getValue(), false);
        }
        session.deathTagOriginalLightBlocks.clear();
        for (UUID playerId : new ArrayList<>(session.deathTagDisguises.keySet())) {
            removeDeathTagDisguise(session, playerId);
        }
        session.deathTagFletchedArrows.clear();
        session.deathTagProjectileDamage.clear();
        session.deathTagTraitorKnockbackAt.clear();
        for (Entity entity : new ArrayList<>(session.entities)) {
            if (entity != null) baconAvatarOwnersByEntityId.remove(entity.getEntityId());
            if (entity != null && entity.isValid()) entity.remove();
        }
        session.bombOwners.clear();
        session.bombPowerTypes.clear();
        session.bombPowerupDrops.clear();
        session.bombLastThrowAt.clear();
        session.bombLastPitchAdjustAt.clear();
        session.bombLastWallerAt.clear();
        session.bombLastLocations.clear();
        session.bombCreatedAt.clear();
        session.bombPrimed.clear();
        session.bombDirectHitResolved.clear();
        session.bombKillsByTnt.clear();
        session.bombTeamSpawnCentres.clear();
        session.bombCompassMenuTargets.clear();
        session.bombSpectateNextTeleportAt.clear();
        session.bombNextCompassUpdateAt = 0L;
        session.bombNativeDamageSuppressed.clear();
        session.bombDamageContributors.clear();
        session.bombAssists.clear();
        session.bombDoubleJumpUsed.clear();
        session.bombMvpPressurePlayers.clear();
        session.bombPlayersDamaged.clear();
        session.dragonContactCooldowns.clear();
        session.paintProjectileDamage.clear();
        session.paintballProjectileCosmetics.clear();
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
        cleanupBaconDisguiseNameTags(session);
        if (session.type == ArcadePortGame.BACON_BRAWL) {
            cleanupBaconPlayerVisibility(session);
            cleanupBaconMeterScoreboard(session);
        } else if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
            for (Player racer : session.players.values()) hideDragonEscapeGhost(racer);
            cleanupDragonEscapeRaceScoreboard(session);
        }
        for (Map.Entry<UUID, Player> entry : session.players.entrySet()) {
            Player player = entry.getValue();
            PlayerSnapshot snapshot = session.snapshots.get(entry.getKey());
            if (snapshot == null) continue;
            if (player != null && player.isOnline()) {
                if (session.type == ArcadePortGame.BACON_BRAWL) {
                    player.removePotionEffect(PotionEffectType.RESISTANCE);
                    session.baconBrutusUntil.remove(entry.getKey());
                }
                if ((session.type == ArcadePortGame.BACON_BRAWL
                        && isBaconCompassMenuTitle(player.getOpenInventory().getTitle()))
                        || (session.type == ArcadePortGame.BOMB_LOBBERS
                        && isBombLobbersCompassMenuTitle(player.getOpenInventory().getTitle()))
                        || (session.type == ArcadePortGame.MICRO_BATTLES
                        && isMicroCompassMenuTitle(player.getOpenInventory().getTitle()))) {
                    player.closeInventory();
                }
                snapshot.restore(player);
                // Ended rounds always return testers to the actual Arcade lobby spawn,
                // even when their restored snapshot is already inside the lobby world.
                lobby.sendToLobby(player);
            } else pendingRestore.put(entry.getKey(), snapshot);
        }
        session.sheep.clear();
        session.sheepOwners.clear();
        session.sheepCarriers.clear();
        session.entities.clear();
        session.baconBabyPigs.clear();
        session.baconBlastItems.clear();
        session.baconBlastOwners.clear();
        session.baconBlastLastLocations.clear();
        session.baconBlastCreatedAt.clear();
        session.baconLastDamager.clear();
        session.baconLastDamagedAt.clear();
        session.baconLastHitSource.clear();
        session.baconMeters.clear();
        session.baconAbilitiesUsed.clear();
        session.baconBrutusUntil.clear();
        session.baconCloakUntil.clear();
        session.baconSlamPowerPercent.clear();
        session.baconBodySlamKills.clear();
        session.baconThreeForOneAwarded.clear();
        session.baconLastSwingAt.clear();
        session.baconHitFeedbackUntil.clear();
        session.baconBlastPowerById.clear();
        session.baconPlayerPigs.clear();
        session.baconPigOwners.clear();
        session.baconCompassMenuTargets.clear();
        session.respawning.clear();
        session.alive.clear();
        session.players.clear();
        session.snapshots.clear();
        lobbyRuntime.returnBotsToLobby();
        if (session.type == ArcadePortGame.BACON_BRAWL) baconSourceSpawnOverrides = List.of();
        lobbyRuntime.refreshDisplays();
    }

    /** Record local Bacon Brawl achievements in isolated test config, never production milestones. */
    private void recordBaconBrawlAchievements(Session session, Player winner) {
        UUID id = winner.getUniqueId();
        int totalWins = plugin.getConfig().getInt(localStatsPath(id, ArcadePortGame.BACON_BRAWL.id()) + "wins", 0);
        if (ArcadePortRules.shouldAwardBaconKingOfBacon(totalWins)) {
            awardBaconAchievement(winner, BaconBrawlAchievement.KING_OF_BACON.configKey(id),
                    "King of Bacon — 15 Bacon Brawl wins.");
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - session.startedAt);
        if (ArcadePortRules.shouldAwardBaconFlashFried(elapsed)) {
            awardBaconAchievement(winner,
                    ArcadePortRules.baconBrawlAchievementConfigKey(id, "flash-fried"),
                    "Flash Fried — won Bacon Brawl in 60 seconds or less.");
        }
        if (ArcadePortRules.shouldAwardBaconRawPork(!session.baconAbilitiesUsed.contains(id))) {
            awardBaconAchievement(winner,
                    ArcadePortRules.baconBrawlAchievementConfigKey(id, "raw-pork"),
                    "Raw Pork — won Bacon Brawl without using an ability.");
        }
    }

    private void recordBombLobbersDamage(Session session, Player target, UUID sourceId, long now) {
        if (session == null || target == null || sourceId == null || sourceId.equals(target.getUniqueId())
                || !session.players.containsKey(sourceId)) return;
        UUID targetId = target.getUniqueId();
        int sourceTeam = session.teams.getOrDefault(sourceId, -1);
        int targetTeam = session.teams.getOrDefault(targetId, -2);
        if (!ArcadePortRules.areBombLobbersEnemies(sourceTeam, targetTeam)) return;
        session.bombDamageContributors.computeIfAbsent(targetId, ignored -> new HashMap<>()).put(sourceId, now);
    }

    private void recordBombLobbersAssists(Session session, UUID victimId, UUID killerId, long now) {
        Map<UUID, Long> contributors = session.bombDamageContributors.remove(victimId);
        if (contributors == null || killerId == null || !session.players.containsKey(killerId)) return;
        int killerTeam = session.teams.getOrDefault(killerId, -1);
        for (Map.Entry<UUID, Long> contribution : contributors.entrySet()) {
            UUID contributorId = contribution.getKey();
            if (contributorId == null || contributorId.equals(victimId) || contributorId.equals(killerId)
                    || now - contribution.getValue() > ArcadePortRules.BOMB_LOBBERS_ASSIST_WINDOW_MILLIS
                    || !session.players.containsKey(contributorId)
                    || session.teams.getOrDefault(contributorId, -2) != killerTeam) continue;
            int assists = session.bombAssists.merge(contributorId, 1, Integer::sum);
            Player contributor = session.players.get(contributorId);
            if (contributor != null && contributor.isOnline()) {
                contributor.sendActionBar(ChatColor.GREEN + "Assist! " + assists + "/5 for Team player!");
            }
        }
    }

    private int bombLobbersAliveTeammates(Session session, UUID playerId) {
        int team = session.teams.getOrDefault(playerId, -1);
        int count = 0;
        for (UUID id : session.alive) if (session.teams.getOrDefault(id, -2) == team) count++;
        return count;
    }

    private int bombLobbersAliveOpponents(Session session, UUID playerId) {
        int team = session.teams.getOrDefault(playerId, -1);
        int count = 0;
        for (UUID id : session.alive) {
            if (ArcadePortRules.areBombLobbersEnemies(team, session.teams.getOrDefault(id, -2))) count++;
        }
        return count;
    }

    private void updateBombLobbersMvpEligibility(Session session) {
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS) return;
        for (UUID id : session.alive) {
            if (bombLobbersAliveTeammates(session, id) == 1
                    && bombLobbersAliveOpponents(session, id) >= 3) {
                session.bombMvpPressurePlayers.add(id);
            }
        }
    }

    private void recordBombLobbersAchievements(Session session, Player winner) {
        for (Map.Entry<UUID, Player> entry : session.players.entrySet()) {
            UUID id = entry.getKey();
            Player player = entry.getValue();
            if (ArcadePortRules.shouldAwardBombLobbersUltimateKiller(session.scores.getOrDefault(id, 0))) {
                awardBombLobbersAchievement(player, BombLobbersAchievement.ULTIMATE_KILLER.configKey(id),
                        "Ultimate Killer — 5 kills in one game.");
            }
            if (ArcadePortRules.shouldAwardBombLobbersTeamPlayer(session.bombAssists.getOrDefault(id, 0))) {
                awardBombLobbersAchievement(player, BombLobbersAchievement.TEAM_PLAYER.configKey(id),
                        "Team player! — 5 assists in one game.");
            }
        }
        if (winner == null) return;
        int winningTeam = session.teams.getOrDefault(winner.getUniqueId(), -1);
        for (UUID winnerId : new HashSet<>(session.alive)) {
            if (session.teams.getOrDefault(winnerId, -2) != winningTeam) continue;
            Player winningPlayer = session.players.get(winnerId);
            if (winningPlayer == null) continue;
            if (ArcadePortRules.shouldAwardBombLobbersDoubleJump(true, true,
                    session.bombDoubleJumpUsed.contains(winnerId))) {
                awardBombLobbersAchievement(winningPlayer,
                        BombLobbersAchievement.WHATS_A_DOUBLE_JUMP.configKey(winnerId),
                        "What's a double jump? — won without double jumping while alive.");
            }
            if (bombLobbersAliveTeammates(session, winnerId) == 1
                    && session.bombMvpPressurePlayers.contains(winnerId)) {
                awardBombLobbersAchievement(winningPlayer, BombLobbersAchievement.THE_MVP.configKey(winnerId),
                        "The MVP — won after surviving alone against at least 3 opponents.");
            }
            if ("armorer".equals(session.bombLobbersKits.getOrDefault(winnerId, "jumper"))) {
                awardBombLobbersAchievement(winningPlayer, BombLobbersAchievement.BLAST_PROOF.configKey(winnerId),
                        "Blast Proof — won as the Armorer.");
            }
            if (!session.bombPlayersDamaged.contains(winnerId)) {
                awardBombLobbersAchievement(winningPlayer, BombLobbersAchievement.JELLY_SKIN.configKey(winnerId),
                        "Jelly Skin — won without taking damage.");
            }
            int wins = plugin.getConfig().getInt(
                    localStatsPath(winnerId, ArcadePortGame.BOMB_LOBBERS.id()) + "wins", 0);
            if (ArcadePortRules.shouldAwardBombLobbersMasterBomber(wins)) {
                awardBombLobbersAchievement(winningPlayer, BombLobbersAchievement.MASTER_BOMBER.configKey(winnerId),
                        "Master Bomber — won 30 Bomb Lobbers games.");
            }
        }
    }

    private void awardBombLobbersAchievement(Player player, String key, String description) {
        if (key == null || key.isBlank() || plugin.getConfig().getBoolean(key, false)) return;
        plugin.getConfig().set(key, true);
        if (player != null && player.isOnline()) player.sendMessage(ChatColor.GOLD + "Achievement unlocked: " + description);
        plugin.saveConfig();
    }

    private void awardMicroBattlesAchievement(Player player, String key, String description) {
        if (key == null || key.isBlank() || plugin.getConfig().getBoolean(key, false)) return;
        plugin.getConfig().set(key, true);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.GOLD + "Achievement unlocked: " + description);
        }
        plugin.saveConfig();
    }

    private void awardBaconAchievement(Player player, String key, String description) {
        if (plugin.getConfig().getBoolean(key, false)) return;
        plugin.getConfig().set(key, true);
        player.sendMessage(ChatColor.GOLD + "Achievement unlocked: " + description);
        plugin.saveConfig();
    }

    private String localStatsPath(UUID playerId, String gameId) {
        return "local-stats." + playerId + "." + gameId + ".";
    }

    /** Store completed local acceptance runs only; operator-aborted sessions aren't counted. */
    private void recordLocalStats(Session session, Player winner, boolean completed) {
        if (!completed) return;
        UUID winnerId = winner == null ? null : winner.getUniqueId();
        int winningTeam = winnerId == null ? -1 : session.teams.getOrDefault(winnerId, -1);
        for (Map.Entry<UUID, Player> entry : session.players.entrySet()) {
            UUID id = entry.getKey();
            String path = localStatsPath(id, session.type.id());
            plugin.getConfig().set(path + "games-played",
                    plugin.getConfig().getInt(path + "games-played", 0) + 1);
            int score = Math.max(0, session.scores.getOrDefault(id, 0));
            plugin.getConfig().set(path + "score",
                    plugin.getConfig().getInt(path + "score", 0) + score);
            if (!session.alive.contains(id)) {
                plugin.getConfig().set(path + "knockouts",
                        plugin.getConfig().getInt(path + "knockouts", 0) + 1);
            }
            boolean won = winnerId != null && (winnerId.equals(id)
                    || ((session.type == ArcadePortGame.BOMB_LOBBERS
                            || session.type == ArcadePortGame.MICRO_BATTLES)
                        && session.alive.contains(id)
                        && session.teams.getOrDefault(id, -2) == winningTeam));
            if (won) {
                plugin.getConfig().set(path + "wins",
                        plugin.getConfig().getInt(path + "wins", 0) + 1);
            }
            if (session.type == ArcadePortGame.DEATH_TAG) {
                int roundKills = Math.max(0, session.scores.getOrDefault(id, 0));
                int roundDeaths = Math.max(0, session.deathTagDeathsByPlayer.getOrDefault(id, 0));
                int previousScore = Math.max(0, plugin.getConfig().getInt(path + "score", 0) - score);
                int totalKills = plugin.getConfig().getInt(path + "kills", previousScore) + roundKills;
                int totalDeaths = plugin.getConfig().getInt(path + "deaths", 0) + roundDeaths;
                plugin.getConfig().set(path + "kills", totalKills);
                plugin.getConfig().set(path + "deaths", totalDeaths);
                plugin.getConfig().set(path + "kill-death-ratio",
                        ArcadePortRules.deathTagKillDeathRatio(totalKills, totalDeaths));

                int comeAtMeBro = session.deathTagComeAtMeBroAwards.getOrDefault(id, 0);
                if (comeAtMeBro > 0) {
                    plugin.getConfig().set(path + "come-at-me-bro",
                            plugin.getConfig().getInt(path + "come-at-me-bro", 0) + comeAtMeBro);
                }
            }
        }
        plugin.saveConfig();
    }

    private enum StatsPage { ROOT, ARCADE, CATEGORY, GAME, DRAGON_ESCAPE_MAPS, DRAGON_ESCAPE_COLOURS }

    private static final class StatsMenuHolder implements InventoryHolder {
        private final StatsPage page;
        private final UUID targetId;
        private final String targetName;
        private final String categoryId;
        private Inventory inventory;

        private StatsMenuHolder(StatsPage page, UUID targetId, String targetName, String categoryId) {
            this.page = page;
            this.targetId = targetId;
            this.targetName = targetName;
            this.categoryId = categoryId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    // AchievementMainPage's exact 5-row OX layout, and the 1.8 non-Arcade categories.
    private static final String[][] ROOT_STATS_CATEGORIES = {
            {"global", "Global", "EMERALD"},
            {"holiday", "Holiday Achievements", "CAKE"},
            {"bridges", "The Bridges", "IRON_PICKAXE"},
            {"survival-games", "Survival Games", "DIAMOND_SWORD"},
            {"skywars", "Skywars", "FEATHER"},
            {"wizards", "Wizards", "BLAZE_ROD"},
            {"castle-assault", "Castle Assault", "DIAMOND_CHESTPLATE"},
            {"castle-siege", "Castle Siege", "DIAMOND_CHESTPLATE"},
            {"bawk-bawk-battles", "Bawk Bawk Battles", "EGG"},
            {"block-hunt", "Block Hunt", "GRASS_BLOCK"},
            {"smash-mobs", "Super Smash Mobs", "WITHER_SKELETON_SKULL"},
            {"mine-strike", "MineStrike", "TNT"},
            {"draw-my-thing", "Draw My Thing", "WRITABLE_BOOK"},
            {"champions", "Champions", "BEACON"},
            {"master-builders", "Master Builders", "OAK_PLANKS"},
            {"speed-builders", "Speed Builders", "QUARTZ_BLOCK"},
            {"skyfall", "Skyfall", "DIAMOND_BOOTS"},
            {"gem-hunters", "Gem Hunters", "EMERALD"},
            {"moba", "Heroes of GWEN", "PRISMARINE_SHARD"},
            {"cake-wars", "Cake Wars", "CAKE"},
            {"nano-games", "Nano Games", "EGG"}
    };

    // Mirrors the legacy ArcadeMainPage order, including Snake and Monster Maze.
    private static final String[][] ARCADE_STATS_GAMES = {
            {"dragons", "Dragons", "ENDER_STONE"},
            {"dragon-escape", "Dragon Escape", "DRAGON_EGG"},
            {"sheep-quest", "Sheep Quest", "WHITE_WOOL"},
            {"one-in-the-quiver", "One in the Quiver", "BOW"},
            {"paintball", "Super Paintball", "ENDER_PEARL"},
            {"turf-wars", "Turf Wars", "RED_TERRACOTTA"},
            {"runner", "Runner", "LEATHER_BOOTS"},
            {"spleef", "Super Spleef", "IRON_SHOVEL"},
            {"death-tag", "Death Tag", "SKELETON_SKULL"},
            {"snake", "Snake", "GRAY_WOOL"},
            {"bacon-brawl", "Bacon Brawl", "PORKCHOP"},
            {"micro-battles", "Micro Battle", "LAVA_BUCKET"},
            {"bomb-lobbers", "Bomb Lobbers", "FIRE_CHARGE"},
            {"evolution", "Evolution", "SLIME_SPAWN_EGG"},
            {"monster-maze", "Monster Maze", "ROTTEN_FLESH"},
            {"gladiators", "Gladiators", "IRON_SWORD"}
    };

    private static final List<String> DRAGON_ESCAPE_BEACON_COLOR_NAMES =
            List.of(ArcadePortRules.DRAGON_ESCAPE_BEACON_COLORS);

    public boolean isDragonEscapeGhostEnabled(UUID playerId) {
        return playerId != null && plugin.getConfig().getBoolean("dragon-escape-ghost-visible." + playerId, false);
    }

    public boolean setDragonEscapeGhost(Player player, String requested) {
        if (player == null || requested == null) return false;
        String value = requested.toLowerCase(java.util.Locale.ROOT);
        boolean enabled;
        if ("on".equals(value) || "true".equals(value) || "enable".equals(value)) enabled = true;
        else if ("off".equals(value) || "false".equals(value) || "disable".equals(value)) enabled = false;
        else if ("toggle".equals(value)) enabled = !isDragonEscapeGhostEnabled(player.getUniqueId());
        else {
            player.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport ghost <on|off|toggle>");
            return isDragonEscapeGhostEnabled(player.getUniqueId());
        }
        plugin.getConfig().set("dragon-escape-ghost-visible." + player.getUniqueId(), enabled);
        plugin.saveConfig();
        if (!enabled) hideDragonEscapeGhost(player);
        if (enabled && dragonEscapeGhostBridge == null) {
            player.sendMessage(ChatColor.RED + "Transparent player ghosts require PacketEvents 2.14.0 on this server.");
        }
        player.sendMessage(ChatColor.AQUA + "Dragon Escape PB ghost " + (enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled")
                + ChatColor.GRAY + ". Your PB delta remains visible above the hotbar either way.");
        return enabled;
    }

    public String dragonEscapeBeaconColor(UUID playerId) {
        if (playerId == null) return "white";
        String value = plugin.getConfig().getString("dragon-escape-beacon-color." + playerId, "white");
        if (value == null) return "white";
        value = value.toLowerCase(java.util.Locale.ROOT);
        return DRAGON_ESCAPE_BEACON_COLOR_NAMES.contains(value) ? value : "white";
    }

    public boolean setDragonEscapeBeaconColor(Player player, String requested) {
        if (player == null || requested == null) return false;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        if (!DRAGON_ESCAPE_BEACON_COLOR_NAMES.contains(value)) {
            player.sendMessage(ChatColor.YELLOW + "Beacon colours: " + String.join(", ", DRAGON_ESCAPE_BEACON_COLOR_NAMES));
            return false;
        }
        plugin.getConfig().set("dragon-escape-beacon-color." + player.getUniqueId(), value);
        plugin.saveConfig();
        player.sendMessage(ChatColor.AQUA + "Dragon Escape leader beacon colour set to " + value + ". It follows the current race leader.");
        return true;
    }

    private String dragonEscapePbPath(UUID playerId, String mapResource) {
        String mapKey = ArcadePortRules.dragonEscapeMapStorageKey(mapResource);
        return playerId == null || mapKey.isEmpty() ? "" : "dragon-escape-pb." + playerId + "." + mapKey + ".";
    }

    private long dragonEscapePersonalBestMillis(UUID playerId, String mapResource) {
        String path = dragonEscapePbPath(playerId, mapResource);
        return path.isEmpty() ? -1L : plugin.getConfig().getLong(path + "time-ms", -1L);
    }

    private Map<Integer, Long> dragonEscapePersonalBestSplits(UUID playerId, String mapResource) {
        Map<Integer, Long> splits = new HashMap<>();
        String path = dragonEscapePbPath(playerId, mapResource);
        if (path.isEmpty()) return splits;
        var section = plugin.getConfig().getConfigurationSection(path + "splits");
        if (section == null) return splits;
        for (String key : section.getKeys(false)) {
            try {
                int index = Integer.parseInt(key);
                long millis = section.getLong(key, -1L);
                if (index >= 0 && millis >= 0L) splits.put(index, millis);
            } catch (NumberFormatException ignored) { }
        }
        return splits;
    }

    public void openStats(Player viewer, String requestedPlayerName) {
        Player target = viewer;
        if (requestedPlayerName != null && !requestedPlayerName.isBlank()
                && !requestedPlayerName.equalsIgnoreCase(viewer.getName())) {
            target = Bukkit.getPlayerExact(requestedPlayerName);
            if (target == null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().equalsIgnoreCase(requestedPlayerName)) {
                        target = online;
                        break;
                    }
                }
            }
            if (target == null) {
                viewer.sendMessage(ChatColor.RED + "Player " + requestedPlayerName
                        + " is not online. Testplex local stats are available for online players only.");
                return;
            }
        }
        openStatsMain(viewer, target.getUniqueId(), target.getName());
        viewer.sendMessage(ChatColor.GRAY + "Showing local Testplex acceptance stats for " + target.getName()
                + ". These remain isolated from production Mineplex stats.");
    }

    private Inventory createStatsInventory(StatsPage page, UUID targetId, String targetName,
                                           String title, String categoryId) {
        StatsMenuHolder holder = new StatsMenuHolder(page, targetId, targetName, categoryId);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inventory;
        return inventory;
    }

    private void openStatsMain(Player viewer, UUID targetId, String targetName) {
        Inventory inventory = createStatsInventory(StatsPage.ROOT, targetId, targetName,
                targetName + "'s Stats", "global");
        int[] rootSlots = {0, 2, 4, 6, 8, 9, 11, 13, 15, 17, 18, 20, 22, 24, 26, 27, 29, 31, 33, 35, 36};
        for (int i = 0; i < ROOT_STATS_CATEGORIES.length; i++) {
            String[] def = ROOT_STATS_CATEGORIES[i];
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            if ("global".equals(def[0])) {
                lore.add(ChatColor.YELLOW + "Games Played: " + ChatColor.WHITE + totalLocalStat(targetId, "games-played"));
                lore.add(ChatColor.YELLOW + "Wins: " + ChatColor.WHITE + totalLocalStat(targetId, "wins"));
                lore.add(ChatColor.YELLOW + "Score / Kills: " + ChatColor.WHITE + totalLocalStat(targetId, "score"));
                lore.add(ChatColor.YELLOW + "Knockouts: " + ChatColor.WHITE + totalLocalStat(targetId, "knockouts"));
            } else {
                lore.add(ChatColor.GRAY + "This category remains in the original 1.8 Stats layout.");
                lore.add(ChatColor.GRAY + "Its production stats backend is not part of this isolated server.");
            }
            lore.add(" ");
            lore.add(ChatColor.RESET + "Click for more details!");
            ItemStack item = statsItem(materialOrChest(def[2]), ChatColor.BOLD + def[1], lore,
                    "open-category", def[0], null);
            inventory.setItem(rootSlots[i], item);
        }
        ItemStack arcade = statsItem(Material.BOW, ChatColor.BOLD + "Arcade Games",
                List.of(" ", ChatColor.RESET + "Click for more!"), "open-arcade", null, null);
        inventory.setItem(38, arcade);
        ItemStack uhc = statsItem(Material.GOLDEN_APPLE, ChatColor.BOLD + "UHC",
                List.of(" ", ChatColor.RESET + "Click for more!"), "open-category", "uhc", null);
        inventory.setItem(40, uhc);
        inventory.setItem(49, statsItem(Material.BARRIER, ChatColor.RED + "Close", List.of(), "close", null, null));
        viewer.openInventory(inventory);
    }

    private void openStatsArcade(Player viewer, UUID targetId, String targetName) {
        Inventory inventory = createStatsInventory(StatsPage.ARCADE, targetId, targetName, "Arcade Games", "arcade");
        int slot = 9;
        for (String[] def : ARCADE_STATS_GAMES) {
            String gameId = def[0];
            ItemStack item = statsItem(materialOrChest(def[2]), ChatColor.BOLD + def[1],
                    gameStatsLore(targetId, gameId, true), "open-game", null, gameId);
            inventory.setItem(slot, item);
            slot += ((slot + 1) % 9 == 0) ? 1 : 2;
        }
        inventory.setItem(4, statsItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back", List.of(),
                "back-main", null, null));
        inventory.setItem(49, statsItem(Material.BARRIER, ChatColor.RED + "Close", List.of(),
                "close", null, null));
        viewer.openInventory(inventory);
    }

    private void openStatsCategory(Player viewer, UUID targetId, String targetName, String categoryId) {
        String categoryName = "global";
        String icon = "EMERALD";
        for (String[] def : ROOT_STATS_CATEGORIES) {
            if (def[0].equals(categoryId)) {
                categoryName = def[1];
                icon = def[2];
                break;
            }
        }
        if ("uhc".equals(categoryId)) {
            categoryName = "Ultra Hardcore";
            icon = "GOLDEN_APPLE";
        }
        Inventory inventory = createStatsInventory(StatsPage.CATEGORY, targetId, targetName, categoryName, categoryId);
        if ("global".equals(categoryId)) {
            int[][] values = {
                    {29, totalLocalStat(targetId, "games-played")},
                    {30, totalLocalStat(targetId, "wins")},
                    {31, totalLocalStat(targetId, "score")},
                    {32, totalLocalStat(targetId, "knockouts")}
            };
            String[] labels = {"Games Played", "Wins", "Score / Kills", "Knockouts"};
            for (int i = 0; i < values.length; i++) {
                int value = values[i][1];
                inventory.setItem(values[i][0], statsItem(value > 0 ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE,
                        (value > 0 ? ChatColor.GREEN : ChatColor.RED) + labels[i],
                        List.of(" ", ChatColor.GRAY + "Recorded by this local test server: " + ChatColor.WHITE + value),
                        "noop", null, null));
            }
            inventory.setItem(22, statsSummaryItem(Material.BOOK, ChatColor.BOLD + "Global Stats",
                    List.of(ChatColor.GRAY + "Total games played: " + totalLocalStat(targetId, "games-played"),
                            ChatColor.GRAY + "Total wins: " + totalLocalStat(targetId, "wins"),
                            ChatColor.GRAY + "Combined score / kills: " + totalLocalStat(targetId, "score"),
                            ChatColor.GRAY + "Total knockouts: " + totalLocalStat(targetId, "knockouts"))));
        } else {
            inventory.setItem(31, statsItem(Material.BARRIER, ChatColor.RED + "No Local Stats",
                    List.of(ChatColor.GRAY + "This category is shown to preserve the 1.8 layout.",
                            ChatColor.GRAY + "Its stats aren't tracked by the isolated Arcade test server."),
                    "noop", null, null));
            inventory.setItem(22, statsSummaryItem(materialOrChest(icon), ChatColor.BOLD + categoryName + " Stats",
                    List.of(ChatColor.GRAY + "No local stats source is configured for this category.")));
        }
        inventory.setItem(4, statsItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back", List.of(),
                "back-main", null, null));
        inventory.setItem(49, statsItem(Material.BARRIER, ChatColor.RED + "Close", List.of(),
                "close", null, null));
        viewer.openInventory(inventory);
    }

    private void openStatsGame(Player viewer, UUID targetId, String targetName, String gameId) {
        if (gameId == null) return;
        String displayName = statsGameName(gameId);
        String path = localStatsPath(targetId, gameId);
        int played = plugin.getConfig().getInt(path + "games-played", 0);
        int wins = plugin.getConfig().getInt(path + "wins", 0);
        int score = plugin.getConfig().getInt(path + "score", 0);
        int knockouts = plugin.getConfig().getInt(path + "knockouts", 0);
        Inventory inventory = createStatsInventory(StatsPage.GAME, targetId, targetName, displayName, gameId);
        if ("bacon-brawl".equals(gameId)) {
            // Match the 1.8 achievement-bottle pattern: unlocked achievements use
            // experience bottles, and unearned achievements use glass bottles.
            int[] achievementSlots = {10, 11, 12, 13, 14};
            BaconBrawlAchievement[] achievements = BaconBrawlAchievement.values();
            for (int i = 0; i < achievements.length; i++) {
                BaconBrawlAchievement achievement = achievements[i];
                boolean unlocked = plugin.getConfig().getBoolean(achievement.configKey(targetId), false);
                List<String> lore = List.of(" ",
                        ChatColor.GRAY + achievement.requirement(),
                        " ",
                        unlocked ? ChatColor.GREEN + "Achievement unlocked"
                                : ChatColor.RED + "Not yet unlocked");
                inventory.setItem(achievementSlots[i],
                        statsItem(unlocked ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE,
                                (unlocked ? ChatColor.GREEN + "✓ " : ChatColor.RED + "○ ")
                                        + achievement.displayName(),
                                lore, "noop", null, null));
            }
        } else if ("dragon-escape".equals(gameId)) {
            String resource = SourceMapCatalog.selectedResource(plugin, ArcadePortGame.DRAGON_ESCAPE);
            long personalBest = dragonEscapePersonalBestMillis(targetId, resource);
            inventory.setItem(30, statsItem(Material.MAP, ChatColor.AQUA + "Personal Bests — Every Map",
                    List.of(" ", ChatColor.GRAY + "View " + SourceMapCatalog.mapsFor(plugin, ArcadePortGame.DRAGON_ESCAPE).size()
                                    + " Dragon Escape maps and your saved personal bests.",
                            ChatColor.YELLOW + "Current map PB: " + ChatColor.WHITE
                                    + (personalBest > 0L ? ArcadePortRules.formatDragonEscapeTime(personalBest) : "Not completed"),
                            " ", ChatColor.RESET + "Click to open the map list."),
                    "open-dragon-escape-pbs", null, gameId));
            boolean canEditSelf = targetId.equals(viewer.getUniqueId());
            inventory.setItem(32, statsItem(Material.BEACON,
                    ChatColor.LIGHT_PURPLE + "Leader Beacon Colour",
                    List.of(" ", ChatColor.GRAY + "Current choice: " + dragonEscapeBeaconColor(targetId),
                            ChatColor.GRAY + "The leading racer supplies the visible beacon colour.",
                            ChatColor.RESET + (canEditSelf ? "Click to select a colour." : "Only your own colour can be changed.")),
                    "open-dragon-escape-colours", null, gameId));
            inventory.setItem(34, statsItem(Material.ENDER_EYE,
                    (isDragonEscapeGhostEnabled(viewer.getUniqueId()) ? ChatColor.GREEN : ChatColor.GRAY)
                            + "PB Ghost: " + (isDragonEscapeGhostEnabled(viewer.getUniqueId()) ? "ON" : "OFF"),
                    List.of(" ", ChatColor.GRAY + "Show your saved best-time ghost while racing.",
                            ChatColor.GRAY + "Your above-hotbar PB delta always remains visible.",
                            ChatColor.RESET + (canEditSelf ? "Click to toggle." : "This controls your own display.")),
                    "toggle-dragon-ghost", null, gameId));
        } else if ("bomb-lobbers".equals(gameId)) {
            int[] achievementSlots = {10, 11, 12, 13, 14, 15, 16, 17};
            BombLobbersAchievement[] achievements = BombLobbersAchievement.values();
            for (int i = 0; i < achievements.length; i++) {
                BombLobbersAchievement achievement = achievements[i];
                boolean unlocked = plugin.getConfig().getBoolean(achievement.configKey(targetId), false);
                List<String> lore = List.of(" ", ChatColor.GRAY + achievement.requirement(), " ",
                        unlocked ? ChatColor.GREEN + "Achievement unlocked" : ChatColor.RED + "Not yet unlocked");
                inventory.setItem(achievementSlots[i], statsItem(
                        unlocked ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE,
                        (unlocked ? ChatColor.GREEN + "✓ " : ChatColor.RED + "○ ") + achievement.displayName(),
                        lore, "noop", null, null));
            }
        } else if ("micro-battles".equals(gameId)) {
            MicroBattlesAchievement[] achievements = MicroBattlesAchievement.values();
            int[] achievementSlots = {12, 13};
            for (int i = 0; i < achievements.length; i++) {
                MicroBattlesAchievement achievement = achievements[i];
                boolean unlocked = plugin.getConfig().getBoolean(achievement.configKey(targetId), false);
                List<String> lore = List.of(" ", ChatColor.GRAY + achievement.requirement(), " ",
                        unlocked ? ChatColor.GREEN + "Achievement unlocked" : ChatColor.RED + "Not yet unlocked",
                        ChatColor.DARK_GRAY + "Local test-server equivalent; not production Mineplex data.");
                inventory.setItem(achievementSlots[i], statsItem(
                        unlocked ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE,
                        (unlocked ? ChatColor.GREEN + "✓ " : ChatColor.RED + "○ ") + achievement.displayName(),
                        lore, "noop", null, null));
            }
        } else {
            int[] slots = {29, 30, 31, 32};
            int[] values = {played, wins, score, knockouts};
            String[] labels = {"Games Played", "Wins", "Score / Kills", "Knockouts"};
            for (int i = 0; i < values.length; i++) {
                int value = values[i];
                List<String> lore = List.of(" ",
                        ChatColor.GRAY + "Recorded by this local test server: " + ChatColor.WHITE + value,
                        ChatColor.DARK_GRAY + "Not production Mineplex achievement data.");
                inventory.setItem(slots[i], statsItem(value > 0 ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE,
                        (value > 0 ? ChatColor.GREEN : ChatColor.RED) + labels[i], lore, "noop", null, null));
            }
        }
        inventory.setItem(22, statsSummaryItem(Material.BOOK, ChatColor.BOLD + displayName + " Stats",
                gameStatsLore(targetId, gameId, false)));
        inventory.setItem(4, statsItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back", List.of(),
                "back-arcade", null, null));
        inventory.setItem(49, statsItem(Material.BARRIER, ChatColor.RED + "Close", List.of(),
                "close", null, null));
        viewer.openInventory(inventory);
    }

    private void openDragonEscapePersonalBests(Player viewer, UUID targetId, String targetName) {
        Inventory inventory = createStatsInventory(StatsPage.DRAGON_ESCAPE_MAPS, targetId, targetName,
                targetName + "'s Dragon Escape PBs", "dragon-escape");
        List<SourceMapCatalog.MapArchive> maps = SourceMapCatalog.mapsFor(plugin, ArcadePortGame.DRAGON_ESCAPE);
        int slot = 9;
        for (SourceMapCatalog.MapArchive map : maps) {
            if (slot > 44) break;
            long pb = dragonEscapePersonalBestMillis(targetId, map.resource());
            Map<Integer, Long> splits = dragonEscapePersonalBestSplits(targetId, map.resource());
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Personal best: " + ChatColor.WHITE
                    + (pb > 0L ? ArcadePortRules.formatDragonEscapeTime(pb) : "Not completed yet"));
            if (pb > 0L) lore.add(ChatColor.GRAY + "Saved checkpoint splits: " + splits.size());
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Map-specific PB saved on this test server.");
            inventory.setItem(slot++, statsItem(Material.CLOCK, ChatColor.BOLD + map.displayName(), lore,
                    "noop", map.resource(), null));
        }
        if (maps.isEmpty()) inventory.setItem(22, statsItem(Material.BARRIER, ChatColor.RED + "No Maps Installed",
                List.of(ChatColor.GRAY + "No Dragon Escape map archives were found."), "noop", null, null));
        inventory.setItem(4, statsItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back", List.of(),
                "back-dragon-escape", null, null));
        inventory.setItem(45, statsItem(Material.BEACON, ChatColor.LIGHT_PURPLE + "Change Leader Beacon Colour",
                List.of(ChatColor.GRAY + "Current: " + dragonEscapeBeaconColor(targetId)),
                "open-dragon-escape-colours", null, "dragon-escape"));
        inventory.setItem(47, statsItem(Material.ENDER_EYE,
                ChatColor.AQUA + "PB Ghost: " + (isDragonEscapeGhostEnabled(viewer.getUniqueId()) ? "ON" : "OFF"),
                List.of(ChatColor.GRAY + "Toggle your visible racing ghost.",
                        ChatColor.GRAY + "PB delta stays visible even with the ghost off."),
                "toggle-dragon-ghost", null, "dragon-escape"));
        inventory.setItem(49, statsItem(Material.BARRIER, ChatColor.RED + "Close", List.of(), "close", null, null));
        viewer.openInventory(inventory);
    }

    private void openDragonEscapeBeaconColours(Player viewer, UUID targetId, String targetName) {
        Inventory inventory = createStatsInventory(StatsPage.DRAGON_ESCAPE_COLOURS, targetId, targetName,
                targetName + "'s Beacon Colours", "dragon-escape");
        String selected = dragonEscapeBeaconColor(targetId);
        Map<String, Material> icons = Map.ofEntries(
                Map.entry("white", Material.WHITE_DYE), Map.entry("red", Material.RED_DYE),
                Map.entry("orange", Material.ORANGE_DYE), Map.entry("yellow", Material.YELLOW_DYE),
                Map.entry("lime", Material.LIME_DYE), Map.entry("green", Material.GREEN_DYE),
                Map.entry("cyan", Material.CYAN_DYE), Map.entry("light-blue", Material.LIGHT_BLUE_DYE),
                Map.entry("blue", Material.BLUE_DYE), Map.entry("purple", Material.PURPLE_DYE),
                Map.entry("magenta", Material.MAGENTA_DYE), Map.entry("pink", Material.PINK_DYE));
        int slot = 10;
        for (String color : DRAGON_ESCAPE_BEACON_COLOR_NAMES) {
            boolean current = color.equals(selected);
            inventory.setItem(slot++, statsItem(icons.getOrDefault(color, Material.WHITE_DYE),
                    (current ? ChatColor.GREEN + "✓ " : ChatColor.WHITE) + color,
                    List.of(" ", ChatColor.GRAY + "The first-place racer uses this colour.",
                            current ? ChatColor.GREEN + "Currently selected" : ChatColor.YELLOW + "Click to equip"),
                    "choose-dragon-color", color, "dragon-escape"));
        }
        inventory.setItem(4, statsItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back", List.of(),
                "back-dragon-escape", null, null));
        inventory.setItem(49, statsItem(Material.BARRIER, ChatColor.RED + "Close", List.of(), "close", null, null));
        viewer.openInventory(inventory);
    }

    private List<String> gameStatsLore(UUID targetId, String gameId, boolean compact) {
        String path = localStatsPath(targetId, gameId);
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Games Played: " + ChatColor.WHITE + plugin.getConfig().getInt(path + "games-played", 0));
        lore.add(ChatColor.GRAY + "Wins: " + ChatColor.WHITE + plugin.getConfig().getInt(path + "wins", 0));
        lore.add(ChatColor.GRAY + "Score / Kills: " + ChatColor.WHITE + plugin.getConfig().getInt(path + "score", 0));
        lore.add(ChatColor.GRAY + "Knockouts: " + ChatColor.WHITE + plugin.getConfig().getInt(path + "knockouts", 0));
        if ("death-tag".equals(gameId)) {
            lore.add(ChatColor.GRAY + "Deaths: " + ChatColor.WHITE + plugin.getConfig().getInt(path + "deaths", 0));
            lore.add(ChatColor.GRAY + "K/D Ratio: " + ChatColor.WHITE
                    + String.format(java.util.Locale.ROOT, "%.2f",
                    plugin.getConfig().getDouble(path + "kill-death-ratio", 0.0D)));
            lore.add(ChatColor.GRAY + "Come At Me Bro: " + ChatColor.WHITE
                    + plugin.getConfig().getInt(path + "come-at-me-bro", 0));
        }
        if ("bacon-brawl".equals(gameId)) {
            long unlocked = java.util.Arrays.stream(BaconBrawlAchievement.values())
                    .filter(achievement -> plugin.getConfig().getBoolean(achievement.configKey(targetId), false))
                    .count();
            lore.add(ChatColor.YELLOW + "Achievements: " + ChatColor.WHITE + unlocked
                    + "/" + BaconBrawlAchievement.values().length);
        } else if ("bomb-lobbers".equals(gameId)) {
            long unlocked = java.util.Arrays.stream(BombLobbersAchievement.values())
                    .filter(achievement -> plugin.getConfig().getBoolean(achievement.configKey(targetId), false))
                    .count();
            lore.add(ChatColor.YELLOW + "Achievements: " + ChatColor.WHITE + unlocked
                    + "/" + BombLobbersAchievement.values().length);
            lore.add(ChatColor.GRAY + "TNT thrown: " + plugin.getConfig().getInt(path + "thrown", 0));
            lore.add(ChatColor.GRAY + "Direct hits: " + plugin.getConfig().getInt(path + "direct-hits", 0));
        }
        if (!compact) lore.add(ChatColor.DARK_GRAY + "Stats recorded by the isolated test server.");
        else lore.add(ChatColor.RESET + "Click for more details!");
        Session current = active;
        if (current != null && current.type != null && current.type.id().equals(gameId)
                && current.players.containsKey(targetId)) {
            lore.add(" ");
            lore.add(ChatColor.GOLD + "Current test: " + (current.alive.contains(targetId) ? "Alive" : "Spectating"));
            lore.add(ChatColor.GRAY + "Current score: " + current.scores.getOrDefault(targetId, 0));
        }
        return lore;
    }

    private int totalLocalStat(UUID targetId, String metric) {
        int total = 0;
        for (ArcadePortGame game : ArcadePortGame.values()) {
            total += plugin.getConfig().getInt(localStatsPath(targetId, game.id()) + metric, 0);
        }
        total += plugin.getConfig().getInt(localStatsPath(targetId, "monster-maze") + metric, 0);
        total += plugin.getConfig().getInt(localStatsPath(targetId, "snake") + metric, 0);
        return total;
    }

    private String statsGameName(String gameId) {
        for (String[] def : ARCADE_STATS_GAMES) if (def[0].equals(gameId)) return def[1];
        ArcadePortGame game = ArcadePortGame.fromId(gameId);
        return game == null ? gameId : game.displayName();
    }

    private Material materialOrChest(String name) {
        Material material = Material.matchMaterial(name);
        return material == null ? Material.CHEST : material;
    }

    private ItemStack statsSummaryItem(Material material, String title, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack statsItem(Material material, String title, List<String> lore,
                                String action, String categoryId, String gameId) {
        ItemStack item = statsSummaryItem(material, title, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(statsActionKey, PersistentDataType.STRING, action);
        if (categoryId != null) meta.getPersistentDataContainer().set(statsCategoryKey, PersistentDataType.STRING, categoryId);
        if (gameId != null) meta.getPersistentDataContainer().set(statsGameKey, PersistentDataType.STRING, gameId);
        item.setItemMeta(meta);
        return item;
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
        Session stopping = active;
        List<Player> participantsToReturn = stopping == null
                ? List.of() : new ArrayList<>(stopping.players.values());
        if (stopping != null) finish(stopping, null, false);
        // Make the explicit Stop Game action authoritative: every participant from
        // the stopped session is put at the lobby spawn after round state is cleaned.
        for (Player player : participantsToReturn) {
            if (player != null && player.isOnline()) lobby.sendToLobby(player);
        }
        queues.clear();
        plugin.getLogger().info("Modern Arcade ports reset: " + reason);
    }

    public String status() {
        String game = active == null ? "idle" : active.type.displayName() + " (" + active.alive.size() + "/" + active.participantsAtStart + " alive)";
        int queued = queues.values().stream().mapToInt(Map::size).sum();
        return game + "; queued=" + queued + "; cleared-games=" + enabledGamesLabel();
    }

    public void shutdown() {
        if (manualStartCountdown != null) manualStartCountdown.cancel();
        manualStartCountdown = null;
        manualStartType = null;
        clearLobbyCountdownStatus();
        stop("plugin shutdown");
        lobbyRuntime.shutdown();
        unregisterOptionalBaconPacketBridge();
        baconAvatarOwnersByEntityId.clear();
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
        SourceMapCatalog.MapData sourceMap = sourceMapData.get(session.type.id());
        double voidThresholdY;
        if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
            voidThresholdY = ArcadePortRules.dragonEscapeVoidThresholdY(session.dragonEscapeMapName);
        } else if (sourceMap != null && sourceMap.hasVerticalBounds()) {
            voidThresholdY = ArcadePortRules.sourceMapVoidThresholdY(true, sourceMap.minY());
        } else {
            voidThresholdY = ArcadePortRules.sourceMapVoidThresholdY(false, 0);
        }
        if (session.type == ArcadePortGame.DRAGON_ESCAPE && sourceMap != null
                && sourceMap.hasBounds()
                && ArcadePortRules.isOutsideDragonEscapeMapBounds(
                        location.getX(), location.getY(), location.getZ(),
                        sourceMap.minX(), sourceMap.maxX(), sourceMap.minZ(), sourceMap.maxZ(),
                        sourceMap.hasVerticalBounds(), sourceMap.minY(), sourceMap.maxY())) {
            eliminate(session, player, null, "left the playable map area");
            return;
        }
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDragonEscapeTrackingCompassDrop(PlayerDropItemEvent event) {
        Session session = active;
        if (session != null && session.type == ArcadePortGame.DRAGON_ESCAPE
                && !session.alive.contains(event.getPlayer().getUniqueId())
                && isDragonEscapeTrackingCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
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

    /**
     * Left-clicking an entity bypasses PlayerInteractEvent. This event path is deliberately
     * cancellation-tolerant because bot hit listeners may cancel damage before we see it.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBombLobbersEntityClick(EntityDamageByEntityEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS
                || event.getEntity().getWorld() != session.world
                || !(event.getDamager() instanceof Player attacker)
                || !session.alive.contains(attacker.getUniqueId())) return;

        long now = System.currentTimeMillis();
        long lastThrow = session.bombLastThrowAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (lastThrow > 0L && now - lastThrow < 180L) {
            event.setCancelled(true);
            return;
        }
        long lastWaller = session.bombLastWallerAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (lastWaller > 0L && now - lastWaller < 180L) {
            // The interact event already built/attempted the Waller ability; do not
            // allow the same click to fall through as a melee attack.
            event.setCancelled(true);
            return;
        }
        ItemStack held = attacker.getInventory().getItemInMainHand();
        if (held != null && held.getType() == Material.TNT) {
            event.setCancelled(true);
            throwBombLobbersTnt(session, attacker);
            return;
        }
        String kit = session.bombLobbersKits.getOrDefault(attacker.getUniqueId(), "jumper");
        if ("pitcher".equals(kit) && held != null && held.getType() == Material.LEVER) {
            event.setCancelled(true);
            adjustBombLobbersPitch(session, attacker, false);
            return;
        }
        if ("waller".equals(kit) && held != null && held.getType() == Material.STONE_SHOVEL) {
            event.setCancelled(true);
            useBombLobbersWaller(session, attacker, attacker.getLocation().getBlock().getRelative(0, -1, 0));
        }
    }

    public boolean wasRecentBombLobbersThrow(Player player) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS || player == null) return false;
        long last = session.bombLastThrowAt.getOrDefault(player.getUniqueId(), 0L);
        return last > 0L && System.currentTimeMillis() - last < 180L;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Session session = active;
        if (session == null || event.getEntity().getWorld() != session.world) return;
        if (session.type == ArcadePortGame.BOMB_LOBBERS
                && event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Player attacker) {
            long lastThrow = session.bombLastThrowAt.getOrDefault(attacker.getUniqueId(), 0L);
            if (lastThrow > 0L && System.currentTimeMillis() - lastThrow < 180L) {
                // The left-click interaction already threw and consumed TNT.
                event.setCancelled(true);
                return;
            }
            ItemStack held = attacker.getInventory().getItemInMainHand();
            if (held != null && held.getType() == Material.TNT) {
                // Left-clicking an entity is delivered as damage, not an interact event.
                event.setCancelled(true);
                throwBombLobbersTnt(session, attacker);
                return;
            }
        }
        if (session.type == ArcadePortGame.BOMB_LOBBERS
                && event.getEntity() instanceof Player bombTarget
                && session.players.containsKey(bombTarget.getUniqueId())) {
            if (event instanceof EntityDamageByEntityEvent byEntity
                    && byEntity.getDamager() instanceof TNTPrimed tnt
                    && ArcadePortRules.shouldSuppressNativeBombLobbersTntDamage(
                            session.bombOwners.containsKey(tnt.getUniqueId())
                                    || session.bombNativeDamageSuppressed.contains(tnt.getUniqueId()))) {
                // Managed TNT direct-hit/blast damage is resolved by the source-compatible custom path.
                // Preserve a one-tick marker because Spigot can dispatch the native explosion damage
                // after EntityExplodeEvent, by which time the TNT's regular owner mapping is removed.
                event.setCancelled(true);
                return;
            }
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                // Native PvP is enabled in Reborn Bomb Lobbers. Cancel the native health
                // mutation and resolve it here so kills, assists, elimination, and win checks
                // stay in sync with TNT damage instead of leaving a dead player marked alive.
                event.setCancelled(true);
                if (!(byEntity.getDamager() instanceof Player attacker)
                        || !session.players.containsKey(attacker.getUniqueId())
                        || !ArcadePortRules.shouldProcessBombLobbersPlayerHit(
                                session.alive.contains(attacker.getUniqueId()),
                                session.alive.contains(bombTarget.getUniqueId()))) return;
                double damage = event.getFinalDamage();
                if (damage <= 0.0D) return;
                UUID targetId = bombTarget.getUniqueId();
                session.bombPlayersDamaged.add(targetId);
                recordBombLobbersDamage(session, bombTarget, attacker.getUniqueId(), System.currentTimeMillis());
                bombTarget.setNoDamageTicks(20);
                Vector push = bombTarget.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0.0D);
                if (push.lengthSquared() > 0.001D) {
                    bombTarget.setVelocity(bombTarget.getVelocity().add(push.normalize().multiply(0.35D).setY(0.18D)));
                }
                if (bombTarget.getHealth() <= damage) {
                    eliminate(session, bombTarget, attacker, "was defeated by " + attacker.getName());
                } else {
                    bombTarget.setHealth(bombTarget.getHealth() - damage);
                    bombTarget.playSound(bombTarget.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
                }
                return;
            }

            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                    || event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                // EntityExplodeEvent applies the authentic radius, exposure, armor and knockback.
                // Let that custom path be the sole source of TNT blast damage.
                event.setCancelled(true);
                return;
            }
            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL
                    && "jumper".equals(session.bombLobbersKits.getOrDefault(bombTarget.getUniqueId(), "jumper"))) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            double damage = event.getFinalDamage();
            if (damage > 0.0D) {
                session.bombPlayersDamaged.add(bombTarget.getUniqueId());
                if (bombTarget.getHealth() <= damage) eliminate(session, bombTarget, null,
                        event.getCause().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
                else bombTarget.setHealth(bombTarget.getHealth() - damage);
            }
            return;
        }
        if (session.type == ArcadePortGame.BACON_BRAWL
                && event.getEntity() instanceof LivingEntity avatar
                && session.baconPigOwners.containsKey(avatar.getUniqueId())) {
            event.setCancelled(true);
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                Entity damager = byEntity.getDamager();
                Player attacker = null;
                String source = "melee";
                if (damager instanceof Player directAttacker) {
                    attacker = directAttacker;
                } else if (damager instanceof Projectile projectile
                        && projectile.getShooter() instanceof Player projectileShooter) {
                    attacker = projectileShooter;
                    source = "projectile hit";
                }
                UUID ownerId = session.baconPigOwners.get(avatar.getUniqueId());
                Player target = ownerId == null ? null : session.players.get(ownerId);
                if (attacker != null && target != null && !attacker.getUniqueId().equals(ownerId)) {
                    if (byEntity.getDamager() instanceof Projectile projectile) {
                        handleBaconBrawlHit(session, attacker, target, source,
                                event.getDamage(), projectile.getVelocity());
                    } else {
                        handleBaconBrawlHit(session, attacker, target, source, event.getDamage(), null);
                    }
                }
            }
            return;
        }
        if (session.type == ArcadePortGame.BACON_BRAWL
                && event.getEntity() instanceof Player baconTarget
                && session.players.containsKey(baconTarget.getUniqueId())) {
            if (ArcadePortRules.shouldSuppressBaconBlastHealthDamage(
                    true, session.baconBlastExplosionDepth > 0, event.getCause())) {
                event.setCancelled(true);
                return;
            }
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                Entity damager = byEntity.getDamager();
                if (damager instanceof Player attacker) {
                    event.setCancelled(true);
                    handleBaconBrawlHit(session, attacker, baconTarget, "melee", event.getDamage(), null);
                    return;
                }
                if (damager instanceof Projectile projectile
                        && projectile.getShooter() instanceof Player shooter) {
                    // Use incoming damage and projectile direction now, then skip the subsequent
                    // projectile callback so the knockback-only hit is resolved exactly once.
                    event.setCancelled(true);
                    session.baconHandledProjectiles.add(projectile.getUniqueId());
                    handleBaconBrawlHit(session, shooter, baconTarget, "projectile hit",
                            event.getDamage(), projectile.getVelocity());
                    return;
                }
            }
            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK) {
                // Fire ticks are ignored by the round's health model and do not end the
                // Pig's independent meter-driven invincibility window.
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
        if (session.type == ArcadePortGame.DEATH_TAG) {
            Player disguiseOwner = deathTagDisguiseOwner(session, event.getEntity());
            if (disguiseOwner != null) {
                double proxyDamage = event.getFinalDamage();
                event.setCancelled(true);
                if (event instanceof EntityDamageByEntityEvent byEntity) {
                    Entity damager = byEntity.getDamager();
                    if (damager instanceof Player attacker) {
                        handlePlayerHit(session, attacker, disguiseOwner, "melee");
                    } else if (damager instanceof Projectile projectile
                            && projectile.getShooter() instanceof Player shooter
                            && session.players.containsKey(shooter.getUniqueId())) {
                        session.deathTagProjectileDamage.put(projectile.getUniqueId(), proxyDamage);
                    }
                }
                return;
            }
        }
        if (event.getEntity() instanceof Player target && session.players.containsKey(target.getUniqueId())) {
            double incomingDamage = event.getFinalDamage();
            if (session.type == ArcadePortGame.DEATH_TAG) {
                event.setCancelled(true);
                handleDeathTagDamage(session, target, event, incomingDamage);
                return;
            }
            event.setCancelled(true);
            if (session.type == ArcadePortGame.DRAGON_ESCAPE
                    && event instanceof EntityDamageByEntityEvent dragonEscapeDamage) {
                Entity damager = dragonEscapeDamage.getDamager();
                boolean directPvp = damager instanceof Player;
                boolean playerProjectile = damager instanceof Projectile projectile
                        && projectile.getShooter() instanceof Player;
                if (directPvp || playerProjectile) return;
            }
            if (session.type == ArcadePortGame.RUNNER && event instanceof EntityDamageByEntityEvent runnerDamage) {
                Entity damager = runnerDamage.getDamager();
                if (damager instanceof Player || damager instanceof Projectile) return;
            }
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                Entity damager = byEntity.getDamager();
                if (session.type == ArcadePortGame.DEATH_TAG && damager instanceof Projectile projectile
                        && projectile.getShooter() instanceof Player shooter
                        && session.players.containsKey(shooter.getUniqueId())) {
                    session.deathTagProjectileDamage.put(projectile.getUniqueId(), incomingDamage);
                }
                if (session.type == ArcadePortGame.RUNNER && damager instanceof FallingBlock falling
                        && session.entities.contains(falling)) {
                    handleRunnerFallingBlockHit(session, falling, target);
                } else if (damager instanceof Player attacker) {
                    handlePlayerHit(session, attacker, target, "melee");
                } else if (session.type == ArcadePortGame.DEATH_TAG && damager instanceof Zombie zombie
                        && session.entities.contains(zombie)) {
                    if (session.participantsAtStart <= 1) {
                        handleDeathTagSoloPracticeHit(session, target, zombie);
                    } else {
                        eliminate(session, target, null, "was tagged by the chaser");
                    }
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

    private Player deathTagDisguiseOwner(Session session, Entity entity) {
        if (session == null || entity == null) return null;
        for (Map.Entry<UUID, Mob> entry : session.deathTagDisguises.entrySet()) {
            Mob disguise = entry.getValue();
            if (disguise == null || !disguise.getUniqueId().equals(entity.getUniqueId())) continue;
            Player owner = session.players.get(entry.getKey());
            return owner != null && owner.isOnline() && session.alive.contains(entry.getKey()) ? owner : null;
        }
        return null;
    }

    private Player deathTagProjectileTarget(Session session, Entity hitEntity) {
        if (hitEntity instanceof Player player) return player;
        return deathTagDisguiseOwner(session, hitEntity);
    }

    private void handleDeathTagDamage(Session session, Player target,
            EntityDamageEvent event, double incomingDamage) {
        if (active != session || target == null || !session.alive.contains(target.getUniqueId())
                || incomingDamage <= 0.0D) return;

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Player attacker) {
                handlePlayerHit(session, attacker, target, "melee");
                return;
            }
            if (damager instanceof Projectile projectile
                    && projectile.getShooter() instanceof Player shooter
                    && session.players.containsKey(shooter.getUniqueId())) {
                // ProjectileHitEvent resolves this once with the source Archer damage and knockback perks.
                session.deathTagProjectileDamage.put(projectile.getUniqueId(), incomingDamage);
                return;
            }
            if (damager instanceof Zombie zombie && session.entities.contains(zombie)
                    && session.participantsAtStart <= 1) {
                handleDeathTagSoloPracticeHit(session, target, zombie);
                return;
            }
        }

        boolean chaser = session.tagged.contains(target.getUniqueId());
        double damage = chaser
                ? ArcadePortRules.deathTagEnvironmentalDamageAfterIronSkin(incomingDamage,
                        session.alphaChasers.contains(target.getUniqueId()),
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)
                : incomingDamage;
        if (damage <= 0.0D) return;

        String cause = event.getCause().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        if (ArcadePortRules.deathTagHitIsLethal(target.getHealth(), damage)) {
            if (chaser) {
                respawnDeathTagChaser(session, target);
            } else {
                makeDeathTagChaser(session, target, "was killed by " + cause);
                checkRoundEnd(session);
            }
        } else {
            target.setHealth(target.getHealth() - damage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeathTagBowShoot(EntityShootBowEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.DEATH_TAG
                || !(event.getEntity() instanceof Player player)
                || player.getWorld() != session.world
                || !session.alive.contains(player.getUniqueId())
                || session.tagged.contains(player.getUniqueId())
                || !"archer".equals(session.deathTagRunnerKits.getOrDefault(player.getUniqueId(), "basher"))
                || !(event.getProjectile() instanceof Arrow arrow)) return;

        // Archer is the only Death Tag kit with a bow; track every shot for remove-on-impact.
        // Do not inspect the inventory here: the final arrow may already have been consumed.
        session.deathTagFletchedArrows.add(arrow.getUniqueId());
        session.entities.add(arrow);
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

        if (session.type == ArcadePortGame.DEATH_TAG
                && event.getEntity() instanceof Arrow deathTagArrow
                && session.deathTagFletchedArrows.remove(deathTagArrow.getUniqueId())) {
            Player hitTarget = deathTagProjectileTarget(session, event.getHitEntity());
            if (hitTarget != null && deathTagArrow.getShooter() instanceof Player shooter) {
                handleDeathTagArcherArrowHit(session, deathTagArrow, shooter, hitTarget);
            }
            session.deathTagProjectileDamage.remove(deathTagArrow.getUniqueId());
            session.entities.remove(deathTagArrow);
            deathTagArrow.remove();
            return;
        }

        if (session.type == ArcadePortGame.BOMB_LOBBERS
                && "wind-projectile".equals(event.getEntity().getPersistentDataContainer()
                        .get(bombPowerupKey, PersistentDataType.STRING))) {
            Projectile windProjectile = event.getEntity();
            Location impact = windProjectile.getLocation().clone();
            Player thrower = windProjectile.getShooter() instanceof Player player ? player : null;
            int throwerTeam = thrower == null ? -1
                    : session.teams.getOrDefault(thrower.getUniqueId(), -1);
            session.entities.remove(windProjectile);
            windProjectile.remove();
            session.world.spawnParticle(Particle.CLOUD, impact, 42, 0.9D, 0.75D, 0.9D, 0.1D);
            session.world.spawnParticle(Particle.ELECTRIC_SPARK, impact, 24, 0.65D, 0.65D, 0.65D, 0.06D);
            session.world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.55F);
            for (Player target : alivePlayers(session)) {
                if (thrower != null && target.getUniqueId().equals(thrower.getUniqueId())) continue;
                if (!ArcadePortRules.areBombLobbersEnemies(throwerTeam,
                        session.teams.getOrDefault(target.getUniqueId(), -1))) continue;
                double distance = target.getLocation().distance(impact);
                if (distance > 5.0D) continue;
                Vector away = target.getEyeLocation().toVector().subtract(impact.toVector());
                if (away.lengthSquared() < 0.001D) away = new Vector(0.0D, 0.3D, 1.0D);
                double strength = 1.25D * (1.0D - distance / 5.0D);
                target.setVelocity(target.getVelocity().add(away.normalize().multiply(strength).setY(0.35D)));
                target.setFallDistance(0.0F);
                target.sendActionBar(ChatColor.AQUA + "WIND BLAST!");
            }
            for (LivingEntity bot : lobbyRuntime.botTargets(session.world, ArcadePortGame.BOMB_LOBBERS)) {
                if (!lobbyRuntime.isBombLobbersEnemyBot(bot, throwerTeam)) continue;
                double distance = bot.getLocation().distance(impact);
                if (distance <= 5.0D) {
                    double strength = 1.25D * (1.0D - distance / 5.0D);
                    lobbyRuntime.showBombLobbersBotBlastHit(bot, impact, strength, false);
                }
            }
            return;
        }

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

         if (session.type == ArcadePortGame.PAINTBALL) {
            // Block impacts and non-player entity impacts also end the visible pellet trail.
            session.paintballProjectileCosmetics.remove(event.getEntity().getUniqueId());
            if (event.getHitEntity() == null) {
                session.paintProjectileDamage.remove(event.getEntity().getUniqueId());
            }
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

        Player projectileTarget = session.type == ArcadePortGame.DEATH_TAG
                ? deathTagProjectileTarget(session, event.getHitEntity())
                : event.getHitEntity() instanceof Player player ? player : null;
        if (projectileTarget != null && event.getEntity() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player shooter) hitProjectile(session, projectile, shooter, projectileTarget);
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
    public void onBaconTrackingCompassDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL) return;
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (!isBaconTrackingCompass(dropped) && !isBaconMeterItem(dropped)) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.YELLOW + "You cannot drop the Bacon Brawl meter or Tracking Compass.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBaconMeterItemMove(InventoryClickEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL) return;
        if (isBaconMeterItem(event.getCurrentItem()) || isBaconMeterItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBaconFoodLevelChange(FoodLevelChangeEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL
                || !(event.getEntity() instanceof Player player)
                || !session.players.containsKey(player.getUniqueId())) return;
        // Match the shared legacy Game listener: food updates reset saturation to five.
        player.setSaturation(ArcadePortRules.BACON_SATURATION_ON_FOOD_CHANGE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Session session = active;
        if (session == null) return;
        if (session.type == ArcadePortGame.DRAGON_ESCAPE
                && event.getAction() != Action.PHYSICAL
                && isDragonEscapeUsableBlock(event.getClickedBlock())) {
            event.setCancelled(true);
            return;
        }
        boolean bombLeftClick = session.type == ArcadePortGame.BOMB_LOBBERS && event.getAction().isLeftClick();
        boolean runnerLeftClick = session.type == ArcadePortGame.RUNNER && event.getAction().isLeftClick();
        boolean baconNonRightClick = session.type == ArcadePortGame.BACON_BRAWL
                && !event.getAction().isRightClick();
        boolean dragonEscapeDisruptorLeftClick = session.type == ArcadePortGame.DRAGON_ESCAPE
                && event.getAction() == Action.LEFT_CLICK_AIR;
        boolean microCompassLeftClick = session.type == ArcadePortGame.MICRO_BATTLES
                && !session.alive.contains(event.getPlayer().getUniqueId())
                && isMicroTrackingCompass(event.getItem()) && event.getAction().isLeftClick();
        if (!event.getAction().isRightClick() && !bombLeftClick && !runnerLeftClick
                && !baconNonRightClick && !dragonEscapeDisruptorLeftClick && !microCompassLeftClick) return;
        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();
        if (session.type == ArcadePortGame.MICRO_BATTLES && isMicroTeamLockedItem(heldItem)
                && event.getAction().isRightClick()) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) player.updateInventory();
            }, 1L);
            return;
        }
        if (session.type == ArcadePortGame.MICRO_BATTLES
                && !session.alive.contains(player.getUniqueId()) && isMicroTrackingCompass(heldItem)) {
            event.setCancelled(true);
            if (event.getAction().isRightClick()) {
                openMicroCompassMenu(session, player);
            } else if (event.getAction().isLeftClick()) {
                long now = System.currentTimeMillis();
                long readyAt = session.microSpectateNextTeleportAt.getOrDefault(player.getUniqueId(), 0L);
                if (now < readyAt) return;
                Player target = nearestMicroSpectateTarget(session, player);
                if (target != null) {
                    player.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
                    player.setCompassTarget(target.getLocation());
                    session.microSpectateNextTeleportAt.put(player.getUniqueId(), now + 3_000L);
                } else player.sendMessage(ChatColor.YELLOW + "There are no living opponents to track.");
            }
            return;
        }
        if (event.isCancelled()) {
            boolean forceBaconAbility = session.type == ArcadePortGame.BACON_BRAWL
                    && event.getAction().isRightClick() && heldItem != null
                    && heldItem.getType() == Material.IRON_AXE
                    && session.alive.contains(player.getUniqueId());
            String bombKit = session.bombLobbersKits.getOrDefault(player.getUniqueId(), "jumper");
            boolean forceBombControlClick = session.type == ArcadePortGame.BOMB_LOBBERS
                    && heldItem != null
                    && (heldItem.getType() == Material.TNT
                        || "wind-charge".equals(bombLobbersPowerupType(heldItem))
                        || ("pitcher".equals(bombKit) && heldItem.getType() == Material.LEVER)
                        || ("waller".equals(bombKit) && heldItem.getType() == Material.STONE_SHOVEL))
                    && (event.getAction().isRightClick() || event.getAction().isLeftClick())
                    && session.alive.contains(player.getUniqueId());
            if (!forceBaconAbility && !forceBombControlClick) return;
        }
        if (session.type == ArcadePortGame.BACON_BRAWL
                && !session.alive.contains(player.getUniqueId())
                && isBaconTrackingCompass(heldItem)) {
            event.setCancelled(true);
            if (event.getAction().isRightClick()) {
                openBaconCompassMenu(session, player);
            } else if (event.getAction().isLeftClick()) {
                Player target = nearestAliveBaconTarget(session, player);
                if (target != null) {
                    player.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
                    player.setCompassTarget(target.getLocation());
                } else {
                    player.sendMessage(ChatColor.YELLOW + "There are no living pigs to track.");
                }
            }
            return;
        }
        if (session.type == ArcadePortGame.DRAGON_ESCAPE
                && !session.alive.contains(player.getUniqueId())
                && isDragonEscapeTrackingCompass(heldItem)) {
            // Keep the tracker in the spectator inventory. Vanilla spectator controls
            // remain available; the compass itself continuously points at the nearest racer.
            event.setCancelled(true);
            return;
        }
        if (session.type == ArcadePortGame.BOMB_LOBBERS
                && !session.alive.contains(player.getUniqueId())
                && isBombTrackingCompass(heldItem)) {
            event.setCancelled(true);
            if (event.getAction().isRightClick()) {
                openBombLobbersCompassMenu(session, player);
            } else if (event.getAction().isLeftClick()) {
                long now = System.currentTimeMillis();
                long readyAt = session.bombSpectateNextTeleportAt.getOrDefault(player.getUniqueId(), 0L);
                if (!ArcadePortRules.isBombLobbersSpectatorTeleportReady(now, readyAt)) return;
                Player target = nearestBombLobbersSpectateTarget(session, player);
                if (target != null) {
                    player.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
                    player.setCompassTarget(target.getLocation());
                    session.bombSpectateNextTeleportAt.put(player.getUniqueId(),
                            ArcadePortRules.nextBombLobbersSpectatorTeleportAt(now));
                } else {
                    player.sendMessage(ChatColor.YELLOW + "There are no living opponents to track.");
                }
            }
            return;
        }
        if (session.type == ArcadePortGame.BOMB_LOBBERS
                && event.getAction().isRightClick() && isBombLobbersTeamLockedItem(heldItem)) {
            // TeamArmorModule prevents the slot-9 chestplate from being equipped as real armour.
            event.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) player.updateInventory();
            }, 1L);
            return;
        }
        if (!session.alive.contains(player.getUniqueId())) return;
        if (session.type == ArcadePortGame.BACON_BRAWL && isBaconMeterItem(heldItem)) {
            event.setCancelled(true);
            return;
        }
        Material item = heldItem == null ? Material.AIR : heldItem.getType();
        if (session.type == ArcadePortGame.BOMB_LOBBERS && event.getAction().isRightClick()
                && "wind-charge".equals(bombLobbersPowerupType(heldItem))) {
            event.setCancelled(true);
            throwBombLobbersWindCharge(session, player);
            return;
        }
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
            if ((event.getAction().isRightClick() || event.getAction() == Action.LEFT_CLICK_AIR)
                    && item == Material.TNT && "disruptor".equals(kit)) {
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
        if (session.type == ArcadePortGame.BACON_BRAWL && event.getAction().isRightClick()) {
            String kit = session.baconKits.getOrDefault(player.getUniqueId(), "el-muchacho-pigo");
            boolean clickedUsableBlock = event.getClickedBlock() != null
                    && event.getClickedBlock().getType().isInteractable();
            if (!ArcadePortRules.shouldActivateBaconBrawlAbility(event.getAction().isRightClick(),
                    item, clickedUsableBlock, kit)) return;
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
        if (session.type == ArcadePortGame.BOMB_LOBBERS && item == Material.TNT) {
            event.setCancelled(true);
            throwBombLobbersTnt(session, player);
            return;
        }
        if (session.type == ArcadePortGame.EVOLUTION && item == Material.TNT
                && session.evolutionLevels.getOrDefault(player.getUniqueId(), 0) == 3
                && event.getAction().isLeftClick()) {
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

    /**
     * Some Bukkit right-click inputs target an entity and never raise PlayerInteractEvent.
     * Route those clicks through the exact same Bacon ability / Bomb Lobbers control paths.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onArcadeEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Session session = active;
        if (session == null) return;
        if (session.type == ArcadePortGame.DEATH_TAG
                && event.getRightClicked() instanceof ItemFrame) {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        if (!session.alive.contains(player.getUniqueId())) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        Material item = held == null ? Material.AIR : held.getType();

        if (session.type == ArcadePortGame.BACON_BRAWL && item == Material.IRON_AXE) {
            String kit = session.baconKits.getOrDefault(player.getUniqueId(), "el-muchacho-pigo");
            if (ArcadePortRules.shouldActivateBaconBrawlAbility(true, item, false, kit)) {
                event.setCancelled(true);
                useBaconBrawlAbility(session, player, System.currentTimeMillis());
            }
            return;
        }

        if (session.type != ArcadePortGame.BOMB_LOBBERS) return;
        if (item == Material.TNT) {
            event.setCancelled(true);
            throwBombLobbersTnt(session, player);
            return;
        }
        String kit = session.bombLobbersKits.getOrDefault(player.getUniqueId(), "jumper");
        if ("pitcher".equals(kit) && item == Material.LEVER) {
            event.setCancelled(true);
            adjustBombLobbersPitch(session, player, true);
            return;
        }
        if ("waller".equals(kit) && item == Material.STONE_SHOVEL) {
            event.setCancelled(true);
            useBombLobbersWaller(session, player, player.getLocation().getBlock().getRelative(0, -1, 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPaintballPearlTeleport(PlayerTeleportEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.PAINTBALL
                || event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || !session.players.containsKey(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    /** Match Reborn's UtilBlock.usable guard so map containers, switches and doors stay inert. */
    private boolean isDragonEscapeUsableBlock(Block block) {
        if (block == null) return false;
        if (block.getState() instanceof org.bukkit.block.Container) return true;
        BlockData data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.Openable
                || data instanceof org.bukkit.block.data.Powerable
                || data instanceof org.bukkit.block.data.type.Switch) return true;
        String material = block.getType().name();
        if (material.endsWith("_BED") || material.endsWith("_DOOR")
                || material.endsWith("_TRAPDOOR") || material.endsWith("_FENCE_GATE")
                || material.endsWith("_BUTTON")) return true;
        return material.equals("DISPENSER") || material.equals("DROPPER")
                || material.equals("HOPPER") || material.equals("CRAFTING_TABLE")
                || material.equals("BOOKSHELF") || material.equals("OAK_FENCE")
                || material.equals("NETHER_BRICK_FENCE")
                || material.equals("ENCHANTING_TABLE") || material.equals("BREWING_STAND")
                || material.equals("ANVIL") || material.equals("CHIPPED_ANVIL")
                || material.equals("DAMAGED_ANVIL") || material.equals("PISTON")
                || material.equals("STICKY_PISTON") || material.equals("REPEATER")
                || material.equals("COMPARATOR");
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
                && event.getPlayer().getInventory().contains(Material.DIAMOND_PICKAXE)) {
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
            if (!allowed) {
                event.setCancelled(true);
            } else {
                BlockState replacedState = event.getBlockReplacedState();
                BlockData placedData = placed.getBlockData().clone();
                scheduleDragonEscapeBlockRestore(session, replacedState, placedData);
            }
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

    /**
     * Bomb Lobbers disables both chest and block inventories in the legacy game.
     * Do not interfere with plugin-owned menus such as the spectator compass menu.
     */
    /** Prevent moving or swapping the source TeamArmorModule's protected gear. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeathTagItemPickup(EntityPickupItemEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.DEATH_TAG
                || !(event.getEntity() instanceof Player player)
                || !session.players.containsKey(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeathTagContainerOpen(InventoryOpenEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.DEATH_TAG
                || !(event.getPlayer() instanceof Player player)
                || !session.players.containsKey(player.getUniqueId())) return;
        String type = event.getInventory().getType().name();
        Object holder = event.getInventory().getHolder();
        boolean blockInventory = holder instanceof org.bukkit.block.BlockState
                || holder instanceof org.bukkit.block.DoubleChest;
        boolean restrictedInventory = switch (type) {
            case "ANVIL", "BEACON", "BREWING", "DISPENSER", "DROPPER", "ENCHANTING",
                    "FURNACE", "HOPPER", "MERCHANT", "ENDER_CHEST", "WORKBENCH", "CHEST" -> true;
            default -> false;
        };
        if (blockInventory || restrictedInventory) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeathTagCraftingClick(InventoryClickEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.DEATH_TAG
                || !session.players.containsKey(event.getWhoClicked().getUniqueId())) return;
        if ("CRAFTING".equals(event.getView().getTopInventory().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBombLobbersTeamArmorMove(InventoryClickEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS
                || !session.players.containsKey(event.getWhoClicked().getUniqueId())) return;
        ItemStack hotbarSwap = null;
        if (event.getWhoClicked() instanceof Player player) {
            int button = event.getHotbarButton();
            if (button >= 0) hotbarSwap = player.getInventory().getItem(button);
        }
        if (isBombLobbersTeamLockedItem(event.getCurrentItem())
                || isBombLobbersTeamLockedItem(event.getCursor())
                || isBombLobbersTeamLockedItem(hotbarSwap)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBombLobbersBlockInventoryOpen(InventoryOpenEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS
                || !(event.getPlayer() instanceof Player player)
                || !session.players.containsKey(player.getUniqueId())) return;
        Object holder = event.getInventory().getHolder();
        if (holder instanceof org.bukkit.block.BlockState
                || holder instanceof org.bukkit.block.DoubleChest) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Session session = active;
        if (session == null || !session.players.containsKey(event.getPlayer().getUniqueId())) return;
        if (session.type == ArcadePortGame.MICRO_BATTLES) {
            ItemStack dropped = event.getItemDrop().getItemStack();
            boolean sword = dropped != null && dropped.getType().name().endsWith("_SWORD");
            if (sword || isMicroTeamLockedItem(dropped) || isMicroTrackingCompass(dropped)) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMicroTeamArmorMove(InventoryClickEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.MICRO_BATTLES
                || !session.players.containsKey(event.getWhoClicked().getUniqueId())) return;
        ItemStack hotbarSwap = null;
        if (event.getWhoClicked() instanceof Player player && event.getHotbarButton() >= 0) {
            hotbarSwap = player.getInventory().getItem(event.getHotbarButton());
        }
        if (isMicroTeamLockedItem(event.getCurrentItem())
                || isMicroTeamLockedItem(event.getCursor())
                || isMicroTeamLockedItem(hotbarSwap)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMicroBlockGrow(BlockGrowEvent event) {
        Session session = active;
        if (session != null && session.type == ArcadePortGame.MICRO_BATTLES
                && event.getBlock().getWorld() == session.world) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMicroBlockFlow(BlockFromToEvent event) {
        Session session = active;
        if (session != null && session.type == ArcadePortGame.MICRO_BATTLES
                && event.getBlock().getWorld() == session.world) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Session activeSession = active;
        if (activeSession != null && activeSession.type == ArcadePortGame.MICRO_BATTLES
                && isMicroCompassMenuTitle(event.getView().getTitle())) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player spectator)) return;
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            Map<Integer, UUID> targets = activeSession.microCompassMenuTargets.get(spectator.getUniqueId());
            UUID targetId = targets == null ? null : targets.get(slot);
            if (targetId == null) return;
            Player target = activeSession.players.get(targetId);
            activeSession.microCompassMenuTargets.remove(spectator.getUniqueId());
            spectator.closeInventory();
            if (target == null || !target.isOnline() || !activeSession.alive.contains(targetId)) {
                spectator.sendMessage(ChatColor.YELLOW + "That opponent is no longer available.");
                return;
            }
            if (event.isRightClick()) {
                spectator.setSpectatorTarget(target);
            } else {
                long now = System.currentTimeMillis();
                long readyAt = activeSession.microSpectateNextTeleportAt.getOrDefault(spectator.getUniqueId(), 0L);
                if (now < readyAt) return;
                spectator.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
                activeSession.microSpectateNextTeleportAt.put(spectator.getUniqueId(), now + 3_000L);
            }
            spectator.setCompassTarget(target.getLocation());
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof StatsMenuHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)
                    || event.getClickedInventory() == null
                    || event.getClickedInventory() != event.getView().getTopInventory()) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
            ItemMeta meta = clicked.getItemMeta();
            String action = meta.getPersistentDataContainer().get(statsActionKey, PersistentDataType.STRING);
            String category = meta.getPersistentDataContainer().get(statsCategoryKey, PersistentDataType.STRING);
            String gameId = meta.getPersistentDataContainer().get(statsGameKey, PersistentDataType.STRING);
            switch (action == null ? "" : action) {
                case "close" -> player.closeInventory();
                case "open-arcade" -> openStatsArcade(player, holder.targetId, holder.targetName);
                case "open-category" -> openStatsCategory(player, holder.targetId, holder.targetName,
                        category == null ? "global" : category);
                case "open-game" -> openStatsGame(player, holder.targetId, holder.targetName, gameId);
                case "open-dragon-escape-pbs" -> openDragonEscapePersonalBests(player, holder.targetId, holder.targetName);
                case "open-dragon-escape-colours" -> openDragonEscapeBeaconColours(player, holder.targetId, holder.targetName);
                case "toggle-dragon-ghost" -> {
                    setDragonEscapeGhost(player, "toggle");
                    if (holder.page == StatsPage.DRAGON_ESCAPE_MAPS) {
                        openDragonEscapePersonalBests(player, holder.targetId, holder.targetName);
                    } else {
                        openStatsGame(player, holder.targetId, holder.targetName, "dragon-escape");
                    }
                }
                case "choose-dragon-color" -> {
                    if (!holder.targetId.equals(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + "You can only change your own Dragon Escape beacon colour.");
                    } else {
                        setDragonEscapeBeaconColor(player, category);
                        openDragonEscapeBeaconColours(player, holder.targetId, holder.targetName);
                    }
                }
                case "back-dragon-escape" -> openStatsGame(player, holder.targetId, holder.targetName, "dragon-escape");
                case "back-main" -> openStatsMain(player, holder.targetId, holder.targetName);
                case "back-arcade" -> openStatsArcade(player, holder.targetId, holder.targetName);
                case "back-category" -> openStatsMain(player, holder.targetId, holder.targetName);
                default -> { }
            }
            return;
        }
        Session session = active;
        if (session != null && event.getWhoClicked() instanceof Player player
                && session.players.containsKey(player.getUniqueId())) {
            if (session.type == ArcadePortGame.MICRO_BATTLES) {
                ItemStack hotbarSwap = event.getHotbarButton() < 0 ? null
                        : player.getInventory().getItem(event.getHotbarButton());
                if (isMicroTeamLockedItem(event.getCurrentItem())
                        || isMicroTeamLockedItem(event.getCursor())
                        || isMicroTeamLockedItem(hotbarSwap)) event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
        }
    }

    private void cacheBombLobbersTeamSpawnCentres(Session session) {
        session.bombTeamSpawnCentres.clear();

        // Reborn calculates each side from every configured team spawn, not just the
        // subset of spawn markers occupied by the current lobby. Keep that boundary stable
        // when the test server has a small party or only one player on either team.
        SourceMapCatalog.MapData sourceMap = sourceMapData.get(session.type.id());
        if (sourceMap != null) {
            boolean hasAllTeamMarkers = true;
            for (int team = 0; team < 2; team++) {
                List<Location> markers = sourceMap.teamSpawnLocationsForTeam(team, 2);
                if (markers.isEmpty()) {
                    hasAllTeamMarkers = false;
                    break;
                }
                double x = 0.0D, y = 0.0D, z = 0.0D;
                for (Location marker : markers) {
                    x += marker.getX();
                    y += marker.getY();
                    z += marker.getZ();
                }
                int count = markers.size();
                session.bombTeamSpawnCentres.put(team, new Location(session.world,
                        x / count, y / count, z / count));
            }
            if (hasAllTeamMarkers) return;
            session.bombTeamSpawnCentres.clear();
        }

        // Fallback for a malformed or incomplete source map: derive centres from occupied spawns.
        Map<Integer, double[]> totals = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        for (UUID id : session.players.keySet()) {
            int team = session.teams.getOrDefault(id, -1);
            Location spawn = session.lastLocations.get(id);
            if (team < 0 || spawn == null || spawn.getWorld() != session.world) continue;
            double[] total = totals.computeIfAbsent(team, ignored -> new double[3]);
            total[0] += spawn.getX();
            total[1] += spawn.getY();
            total[2] += spawn.getZ();
            counts.merge(team, 1, Integer::sum);
        }
        for (Map.Entry<Integer, double[]> entry : totals.entrySet()) {
            int count = counts.getOrDefault(entry.getKey(), 0);
            if (count <= 0) continue;
            double[] total = entry.getValue();
            session.bombTeamSpawnCentres.put(entry.getKey(), new Location(session.world,
                    total[0] / count, total[1] / count, total[2] / count));
        }
    }

    private int nearestBombLobbersSpawnTeam(Session session, Location location) {
        if (session == null || location == null || location.getWorld() != session.world
                || session.bombTeamSpawnCentres.isEmpty()) return -1;
        int nearestTeam = -1;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (Map.Entry<Integer, Location> entry : session.bombTeamSpawnCentres.entrySet()) {
            double distanceSquared = location.distanceSquared(entry.getValue());
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestTeam = entry.getKey();
            }
        }
        return nearestTeam;
    }

    private double bombLobbersDistanceToBoundingBox(Location blast, Player target) {
        org.bukkit.util.BoundingBox box = target.getBoundingBox();
        double dx = blast.getX() < box.getMinX() ? box.getMinX() - blast.getX()
                : blast.getX() > box.getMaxX() ? blast.getX() - box.getMaxX() : 0.0D;
        double dy = blast.getY() < box.getMinY() ? box.getMinY() - blast.getY()
                : blast.getY() > box.getMaxY() ? blast.getY() - box.getMaxY() : 0.0D;
        double dz = blast.getZ() < box.getMinZ() ? box.getMinZ() - blast.getZ()
                : blast.getZ() > box.getMaxZ() ? blast.getZ() - box.getMaxZ() : 0.0D;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Mirrors the legacy CompassModule: spectators track nearest opponents, never their own team. */
    private void updateBombLobbersSpectatorCompasses(Session session) {
        List<Player> targets = alivePlayers(session);
        if (targets.isEmpty()) return;
        for (Player spectator : session.players.values()) {
            if (spectator == null || !spectator.isOnline()
                    || session.alive.contains(spectator.getUniqueId())) continue;
            UUID spectatorId = spectator.getUniqueId();
            int spectatorTeam = session.teams.getOrDefault(spectatorId, -1);
            List<Player> enemies = targets.stream()
                    .filter(target -> ArcadePortRules.shouldBombLobbersCompassTrackTeam(
                            spectatorTeam, session.teams.getOrDefault(target.getUniqueId(), -1)))
                    .toList();
            if (enemies.isEmpty()) continue;

            boolean hasCompass = false;
            for (ItemStack item : spectator.getInventory().getContents()) {
                if (isBombTrackingCompass(item)) {
                    hasCompass = true;
                    break;
                }
            }
            if (!hasCompass) {
                spectator.getInventory().addItem(namedItem(Material.COMPASS, "Tracking Compass",
                        "Tracks the nearest enemy player."));
            }

            Player target = enemies.stream()
                    .min(java.util.Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(spectator.getLocation())))
                    .orElse(null);
            if (target == null) continue;
            spectator.setCompassTarget(target.getLocation());
            if (isBombTrackingCompass(spectator.getInventory().getItemInMainHand())) {
                double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                int targetTeam = session.teams.getOrDefault(target.getUniqueId(), -1);
                ChatColor color = targetTeam == 1 ? ChatColor.AQUA : ChatColor.RED;
                spectator.sendActionBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Nearest Target: "
                        + color + target.getName() + ChatColor.WHITE + ChatColor.BOLD + " | Distance: "
                        + color + String.format(java.util.Locale.ROOT, "%.1f", distance)
                        + ChatColor.WHITE + ChatColor.BOLD + " | Height: "
                        + color + String.format(java.util.Locale.ROOT, "%.1f", height));
            }
        }
    }

    private boolean isBombTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).equals("Tracking Compass");
    }

    /** The source compass needle tracks enemies, but its left-click teleport targets any living player. */
    private Player nearestBombLobbersSpectateTarget(Session session, Player spectator) {
        if (session == null || spectator == null) return null;
        return alivePlayers(session).stream()
                .min(java.util.Comparator.comparingDouble(candidate ->
                        candidate.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
    }

    private void openBombLobbersCompassMenu(Session session, Player spectator) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Spectator Menu");
        inventory.setItem(4, namedItem(Material.COMPASS, "Tracking Compass",
                "Choose an enemy player to follow."));

        int[] slots = {
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43,
                46, 47, 48, 49, 50, 51, 52
        };
        Map<Integer, UUID> targetsBySlot = new HashMap<>();
        // The legacy Spectator Menu lists every living player, even though the needle itself
        // tracks only an opposing team.
        List<Player> targets = alivePlayers(session).stream()
                .sorted(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (int i = 0; i < targets.size() && i < slots.length; i++) {
            Player target = targets.get(i);
            int slot = slots[i];
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(target);
                int targetTeam = session.teams.getOrDefault(target.getUniqueId(), -1);
                ChatColor color = targetTeam == 1 ? ChatColor.AQUA : ChatColor.RED;
                meta.setDisplayName(color + target.getName());
                double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                String kit = session.bombLobbersKits.getOrDefault(target.getUniqueId(), "jumper");
                meta.setLore(List.of(
                        ChatColor.GRAY + "Kit: " + kit,
                        ChatColor.GRAY + "Distance: " + String.format(java.util.Locale.ROOT, "%.1f", distance),
                        ChatColor.GRAY + "Height difference: " + String.format(java.util.Locale.ROOT, "%.1f", height),
                        ChatColor.YELLOW + "Left-click: Teleport",
                        ChatColor.YELLOW + "Right-click: Spectate"));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            targetsBySlot.put(slot, target.getUniqueId());
        }
        session.bombCompassMenuTargets.put(spectator.getUniqueId(), targetsBySlot);
        spectator.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBombLobbersCompassMenuClick(InventoryClickEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS
                || !isBombLobbersCompassMenuTitle(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player spectator)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        Map<Integer, UUID> targets = session.bombCompassMenuTargets.get(spectator.getUniqueId());
        UUID targetId = targets == null ? null : targets.get(slot);
        if (targetId == null) return;
        Player target = session.players.get(targetId);
        session.bombCompassMenuTargets.remove(spectator.getUniqueId());
        spectator.closeInventory();
        if (target == null || !target.isOnline() || !session.alive.contains(targetId)) {
            spectator.sendMessage(ChatColor.YELLOW + "That opponent is no longer available.");
            return;
        }
        if (event.isRightClick()) {
            spectator.setSpectatorTarget(target);
        } else {
            long now = System.currentTimeMillis();
            long readyAt = session.bombSpectateNextTeleportAt.getOrDefault(spectator.getUniqueId(), 0L);
            if (!ArcadePortRules.isBombLobbersSpectatorTeleportReady(now, readyAt)) return;
            spectator.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
            session.bombSpectateNextTeleportAt.put(spectator.getUniqueId(),
                    ArcadePortRules.nextBombLobbersSpectatorTeleportAt(now));
        }
        spectator.setCompassTarget(target.getLocation());
    }

    private boolean isBombLobbersCompassMenuTitle(String title) {
        return (ChatColor.DARK_GREEN + "Spectator Menu").equals(title);
    }

    /** Match vanilla 1.8 World#getBlockDensity sampling for the source explosion's exposure factor. */
    private double bombLobbersBlastExposure(Location blast, Player target) {
        if (blast == null || target == null || blast.getWorld() == null
                || target.getWorld() != blast.getWorld()) return 0.0D;
        org.bukkit.util.BoundingBox box = target.getBoundingBox();
        double xStep = ArcadePortRules.bombLobbersExposureSampleStep((box.getMaxX() - box.getMinX()));
        double yStep = ArcadePortRules.bombLobbersExposureSampleStep(box.getHeight());
        double zStep = ArcadePortRules.bombLobbersExposureSampleStep((box.getMaxZ() - box.getMinZ()));
        if (!Double.isFinite(xStep) || !Double.isFinite(yStep) || !Double.isFinite(zStep)) return 0.0D;
        double xOffset = ArcadePortRules.bombLobbersExposureSampleOffset(xStep);
        double zOffset = ArcadePortRules.bombLobbersExposureSampleOffset(zStep);
        int visible = 0;
        int total = 0;
        for (float fx = 0.0F; fx <= 1.0F; fx = (float) (fx + xStep)) {
            for (float fy = 0.0F; fy <= 1.0F; fy = (float) (fy + yStep)) {
                for (float fz = 0.0F; fz <= 1.0F; fz = (float) (fz + zStep)) {
                    Location sample = new Location(blast.getWorld(),
                            box.getMinX() + (box.getMaxX() - box.getMinX()) * fx + xOffset,
                            box.getMinY() + box.getHeight() * fy,
                            box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * fz + zOffset);
                    Vector ray = sample.toVector().subtract(blast.toVector());
                    double distance = ray.length();
                    total++;
                    if (distance <= 1.0E-6D
                            || blast.getWorld().rayTraceBlocks(blast, ray.normalize(), distance,
                                    org.bukkit.FluidCollisionMode.NEVER, true) == null) visible++;
                }
            }
        }
        return total == 0 ? 0.0D : visible / (double) total;
    }

    private int bombLobbersOnlineMatchPlayers(Session session) {
        if (session == null) return 0;
        return (int) session.players.values().stream()
                .filter(player -> player != null && player.isOnline() && player.getWorld() == session.world)
                .count();
    }

    private void updateBombLobbersPowerups(Session session, long now) {
        if (session == null || active != session) return;
        for (Map.Entry<UUID, Item> entry : new ArrayList<>(session.bombPowerupDrops.entrySet())) {
            Item drop = entry.getValue();
            if (drop == null || !drop.isValid()) {
                session.bombPowerupDrops.remove(entry.getKey());
                if (drop != null) session.entities.remove(drop);
            }
        }
        int lobbyPlayers = bombLobbersOnlineMatchPlayers(session);
        if (lobbyPlayers <= 0) return;
        int maxActive = ArcadePortRules.bombLobbersPowerupMaxActive(lobbyPlayers);
        if (session.bombPowerupDrops.size() >= maxActive) {
            if (now >= session.bombNextPowerupSpawnAt) {
                session.bombNextPowerupSpawnAt = now
                        + Math.min(5_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(lobbyPlayers));
            }
            return;
        }
        if (now < session.bombNextPowerupSpawnAt) return;
        Location spawn = findBombLobbersPowerupSpawn(session);
        if (spawn == null) {
            session.bombNextPowerupSpawnAt = now + 5_000L;
            return;
        }
        String[] choices = {"wind-charge", "super-tnt", "fiery-tnt", "speed-boost"};
        String powerup = choices[ThreadLocalRandom.current().nextInt(choices.length)];
        Item drop = session.world.dropItem(spawn, createBombLobbersPowerupStack(powerup));
        drop.setVelocity(new Vector(0.0D, 0.04D, 0.0D));
        drop.setPickupDelay(10);
        drop.setInvulnerable(true);
        drop.getPersistentDataContainer().set(bombPowerupKey, PersistentDataType.STRING, powerup);
        session.entities.add(drop);
        session.bombPowerupDrops.put(drop.getUniqueId(), drop);
        session.world.spawnParticle(Particle.HAPPY_VILLAGER, spawn.clone().add(0.0D, 0.5D, 0.0D),
                18, 0.45D, 0.55D, 0.45D, 0.03D);
        session.world.playSound(spawn, Sound.BLOCK_NOTE_BLOCK_PLING, 0.75F, 1.55F);
        announce(session, ChatColor.AQUA + "A " + ChatColor.GOLD + bombLobbersPowerupName(powerup)
                + ChatColor.AQUA + " power-up has spawned!");
        session.bombNextPowerupSpawnAt = now
                + ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(lobbyPlayers);
    }

    private Location findBombLobbersPowerupSpawn(Session session) {
        if (session == null || session.world == null) return null;
        SourceMapCatalog.MapData map = sourceMapData.get(ArcadePortGame.BOMB_LOBBERS.id());
        int minX = map != null && map.hasBounds() ? map.minX() + 3 : -18;
        int maxX = map != null && map.hasBounds() ? map.maxX() - 3 : 18;
        int minZ = map != null && map.hasBounds() ? map.minZ() + 3 : -18;
        int maxZ = map != null && map.hasBounds() ? map.maxZ() - 3 : 18;
        if (maxX <= minX || maxZ <= minZ) return null;
        for (int attempt = 0; attempt < 48; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
            int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
            int y = session.world.getHighestBlockYAt(x, z);
            Block ground = session.world.getBlockAt(x, y, z);
            Block feet = ground.getRelative(0, 1, 0);
            Block head = ground.getRelative(0, 2, 0);
            if (!ground.getType().isSolid() || ground.isLiquid()
                    || !feet.isPassable() || !head.isPassable()
                    || feet.isLiquid() || head.isLiquid()) continue;
            Location candidate = new Location(session.world, x + 0.5D, y + 1.1D, z + 0.5D);
            boolean closeToTeamSpawn = session.bombTeamSpawnCentres.values().stream()
                    .anyMatch(center -> center != null && center.distanceSquared(candidate) < 64.0D);
            if (closeToTeamSpawn) continue;
            boolean underPlayer = alivePlayers(session).stream()
                    .anyMatch(player -> player.getLocation().distanceSquared(candidate) < 16.0D);
            if (underPlayer) continue;
            return candidate;
        }
        return null;
    }

    private ItemStack createBombLobbersPowerupStack(String type) {
        Material icon = switch (type) {
            case "wind-charge" -> Material.WIND_CHARGE;
            case "super-tnt" -> Material.TNT;
            case "fiery-tnt" -> Material.FIRE_CHARGE;
            default -> Material.SUGAR;
        };
        String lore = switch (type) {
            case "wind-charge" -> "Right-click to launch a gust that blasts opponents away.";
            case "super-tnt" -> "Throw this TNT for a bigger blast and stronger damage.";
            case "fiery-tnt" -> "Throw this TNT to ignite opponents caught in its blast.";
            default -> "Picked up instantly: Speed II for 12 seconds.";
        };
        ItemStack stack = namedItem(icon, bombLobbersPowerupName(type), lore, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(bombPowerupKey, PersistentDataType.STRING, type);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String bombLobbersPowerupName(String type) {
        return switch (type) {
            case "wind-charge" -> "Wind Charge";
            case "super-tnt" -> "Super Explosive TNT";
            case "fiery-tnt" -> "Fiery TNT";
            case "speed-boost" -> "Speed Boost";
            default -> "Power-up";
        };
    }

    private String bombLobbersPowerupType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(bombPowerupKey, PersistentDataType.STRING);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBombLobbersPowerupPickup(EntityPickupItemEvent event) {
        Session session = active;
        if (session == null || session.type != ArcadePortGame.BOMB_LOBBERS
                || event.getItem() == null || event.getItem().getWorld() != session.world
                || !(event.getEntity() instanceof Player player)
                || !session.alive.contains(player.getUniqueId())) return;
        String powerup = event.getItem().getPersistentDataContainer()
                .get(bombPowerupKey, PersistentDataType.STRING);
        if (powerup == null) powerup = bombLobbersPowerupType(event.getItem().getItemStack());
        if (powerup == null) return;

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9F, 1.35F);
        if ("speed-boost".equals(powerup)) {
            event.setCancelled(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                    ArcadePortRules.BOMB_LOBBERS_POWERUP_SPEED_DURATION_TICKS, 1, false, true));
            player.getWorld().spawnParticle(Particle.CLOUD,
                    player.getLocation().add(0.0D, 1.0D, 0.0D), 16, 0.35D, 0.5D, 0.35D, 0.04D);
            player.sendMessage(ChatColor.GREEN + "Speed Boost! " + ChatColor.WHITE + "Speed II for 12 seconds.");
            Item drop = event.getItem();
            session.bombPowerupDrops.remove(drop.getUniqueId());
            session.entities.remove(drop);
            drop.remove();
        } else {
            player.sendMessage(ChatColor.GOLD + "Power-up collected: "
                    + ChatColor.YELLOW + bombLobbersPowerupName(powerup) + ChatColor.GRAY
                    + ("wind-charge".equals(powerup) ? " — right-click to use." : " — throw with your TNT."));
            Item drop = event.getItem();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!drop.isValid()) {
                    session.bombPowerupDrops.remove(drop.getUniqueId());
                    session.entities.remove(drop);
                }
            });
        }
    }

    private void updateBombLobbers(Session session) {
        long now = System.currentTimeMillis();
        updateBombLobbersPowerups(session, now);
        if (now >= session.bombNextCompassUpdateAt) {
            session.bombNextCompassUpdateAt = now + 250L;
            updateBombLobbersSpectatorCompasses(session);
        }
        if (now >= session.bombNextBoundaryCheckAt) {
            session.bombNextBoundaryCheckAt = now + 1_000L;
            for (Player player : new ArrayList<>(alivePlayers(session))) {
                int playerTeam = session.teams.getOrDefault(player.getUniqueId(), -1);
                int playerSide = nearestBombLobbersSpawnTeam(session, player.getLocation());
                if (ArcadePortRules.shouldEliminateBombLobbersForSide(playerTeam, playerSide)) {
                    eliminate(session, player, null, "cheating");
                    if (active != session) return;
                }
            }
        }
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (!player.isFlying() && player.isOnGround()
                    && now >= session.bombDoubleJumpReadyAt.getOrDefault(id, 0L)) {
                session.bombDoubleJumpAvailable.add(id);
                if (!player.getAllowFlight()) player.setAllowFlight(true);
            }
            long elapsed = Math.max(0L, now - session.startedAt);
            // Recharge.use() returns true immediately when the source cooldown key is absent.
            // The opening Craftman grant therefore arrives on the first live FAST update, then
            // subsequent grants use the game-time-dependent 8/7/6/5/4-second intervals.
            long nextGrant = session.bombNextCraftAt.computeIfAbsent(id,
                    ignored -> ArcadePortRules.bombLobbersFirstCraftReadyAt(now));
            if (now >= nextGrant) {
                if (inventoryCount(player, Material.TNT) < ArcadePortRules.BOMB_LOBBERS_MAX_CARRIED) {
                    player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT",
                            "Click to throw at enemy players.", 1));
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
            // The legacy updater checks IsPlaying, not IsAlive: TNT remains a live projectile
            // after its thrower is eliminated, as long as that player is still in the match.
            if (!ArcadePortRules.shouldKeepBombLobbersBombTracked(owner != null && owner.isOnline())) {
                removeBombTracking(session, bombId);
                session.entities.remove(bomb);
                bomb.remove();
                continue;
            }

            Location current = bomb.getLocation().clone();
            Location previous = session.bombLastLocations.put(bombId, current);
            if (previous == null) previous = current;
            if (!bomb.isOnGround()) renderBombLobbersTrail(session, ownerId, previous, current);

            // Reborn ray-traces blocks first and clips entity collision to that first block hit.
            // Without this, a swept bounding-box test can register direct hits through solid walls.
            Location entitySweepEnd = current;
            double blockHitDistanceSquared = Double.POSITIVE_INFINITY;
            Vector segment = current.toVector().subtract(previous.toVector());
            double segmentLength = segment.length();
            if (Double.isFinite(segmentLength) && segmentLength > 1.0E-6D) {
                org.bukkit.util.RayTraceResult blockHit = session.world.rayTraceBlocks(
                        previous, segment.clone().multiply(1.0D / segmentLength), segmentLength,
                        org.bukkit.FluidCollisionMode.NEVER, true);
                if (blockHit != null && blockHit.getHitPosition() != null) {
                    blockHitDistanceSquared = previous.toVector().distanceSquared(blockHit.getHitPosition());
                    entitySweepEnd = blockHit.getHitPosition().toLocation(session.world);
                }
            }

            // Pick the first enemy intersection before the first block, not the first player iteration.
            Player directTarget = null;
            double directHitDistanceSquared = Double.POSITIVE_INFINITY;
            if (ArcadePortRules.shouldCheckBombLobbersDirectHit(
                    session.bombDirectHitResolved.contains(bombId))) {
                for (Player target : alivePlayers(session)) {
                    UUID targetId = target.getUniqueId();
                    if (targetId.equals(ownerId)) continue;
                    if (!ArcadePortRules.areBombLobbersEnemies(
                            session.teams.getOrDefault(ownerId, -1),
                            session.teams.getOrDefault(targetId, -1))) continue;
                    double hitDistanceSquared = ArcadePortRules.bombLobbersDirectHitDistanceSquared(
                            previous, entitySweepEnd, target.getBoundingBox());
                    if (ArcadePortRules.shouldResolveBombLobbersDirectHitBeforeBlock(
                            hitDistanceSquared, blockHitDistanceSquared)
                            && hitDistanceSquared < directHitDistanceSquared) {
                        directTarget = target;
                        directHitDistanceSquared = hitDistanceSquared;
                    }
                }
            }

            // Test bots are proper target dummies in every game. In Bomb Lobbers a
            // TNT direct hit should give the same visible confirmation but must not
            // eliminate or award stats to an ArmorStand-backed NPC.
            LivingEntity directBotTarget = null;
            double directBotHitDistanceSquared = Double.POSITIVE_INFINITY;
            if (ArcadePortRules.shouldCheckBombLobbersDirectHit(
                    session.bombDirectHitResolved.contains(bombId))) {
                for (LivingEntity candidate : lobbyRuntime.botTargets(session.world, ArcadePortGame.BOMB_LOBBERS)) {
                    if (candidate == null || !candidate.isValid()
                            || !lobbyRuntime.isBombLobbersEnemyBot(candidate,
                                    session.teams.getOrDefault(ownerId, -1))) continue;
                    double hitDistanceSquared = ArcadePortRules.bombLobbersDirectHitDistanceSquared(
                            previous, entitySweepEnd, candidate.getBoundingBox());
                    if (ArcadePortRules.shouldResolveBombLobbersDirectHitBeforeBlock(
                            hitDistanceSquared, blockHitDistanceSquared)
                            && hitDistanceSquared < directBotHitDistanceSquared) {
                        directBotTarget = candidate;
                        directBotHitDistanceSquared = hitDistanceSquared;
                    }
                }
            }
            if (directBotTarget != null
                    && (directTarget == null || directBotHitDistanceSquared < directHitDistanceSquared)) {
                session.bombDirectHitResolved.add(bombId);
                lobbyRuntime.showBombLobbersBotDirectHit(directBotTarget, owner, current,
                        ArcadePortRules.bombLobbersTntIgnites(
                                session.bombPowerTypes.getOrDefault(bombId, "normal")));
                continue;
            }

            if (directTarget != null) {
                Player target = directTarget;
                session.bombDirectHitResolved.add(bombId);
                Vector away = target.getLocation().toVector().subtract(current.toVector());
                away.setY(0.0D);
                target.setFallDistance(0.0F);
                target.setVelocity(ArcadePortRules.bombLobbersDirectHitVelocity(away, target.isOnGround()));
                // Source collision feedback is the normal hurt reaction plus the legacy
                // pickup dings for target (1.7 pitch) and thrower (1.25 pitch). The TNT
                // continues flying; a direct hit is not an explosion or a chat announcement.
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.2F, 1.65F);
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 0.9F);
                target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.7F);
                owner.playSound(owner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.25F);
                String directHitPowerup = session.bombPowerTypes.getOrDefault(bombId, "normal");
                double directHitDamage = bombLobbersDamageAfterArmor(target,
                        ArcadePortRules.bombLobbersDirectHitDamage(directHitPowerup));
                if (ArcadePortRules.bombLobbersTntIgnites(directHitPowerup)) target.setFireTicks(100);
                if (directHitDamage > 0.0D) {
                    session.bombPlayersDamaged.add(target.getUniqueId());
                    recordBombLobbersDamage(session, target, ownerId, now);
                    String directHitsPath = localStatsPath(ownerId, "bomb-lobbers") + "direct-hits";
                    plugin.getConfig().set(directHitsPath, plugin.getConfig().getInt(directHitsPath, 0) + 1);
                }
                if (target.getHealth() <= directHitDamage) {
                    int killsFromThisTnt = session.bombKillsByTnt.merge(bombId, 1, Integer::sum);
                    if (ArcadePortRules.shouldAwardBombLobbersTwoBirdsOneStone(killsFromThisTnt)
                            && owner != null && owner.isOnline() && session.players.containsKey(ownerId)) {
                        awardBombLobbersAchievement(owner, BombLobbersAchievement.TWO_BIRDS_ONE_STONE.configKey(ownerId),
                                "Two birds, one stone — killed 2 players with one TNT.");
                    }
                    eliminate(session, target, owner != null && owner.isOnline() && session.players.containsKey(ownerId)
                            ? owner : null, "was directly hit by TNT");
                } else {
                    target.setHealth(target.getHealth() - directHitDamage);
                }
                checkRoundEnd(session);
                if (active != session) return;
                continue;
            }
            if (active != session) return;

            long createdAt = session.bombCreatedAt.getOrDefault(bombId, now);
            long age = Math.max(0L, now - createdAt);
            boolean touchingSolid = isBombTouchingAnyBlock(bomb);
            if (ArcadePortRules.shouldPrimeBombLobbersBomb(age, touchingSolid)) {
                session.bombPrimed.add(bombId);
                bomb.setFuseTicks(0);
                continue;
            }

            // The original TNT is re-thrown with its existing velocity as its short
            // fuse reaches its last second. It only actually detonates on contact
            // after 3s or unconditionally after 8s.
            if (bomb.getFuseTicks() <= 20) {
                int killsAlreadyCreditedToThisTnt = session.bombKillsByTnt.getOrDefault(bombId, 0);
                String powerupType = session.bombPowerTypes.getOrDefault(bombId, "normal");
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
                session.bombPowerTypes.put(replacementId, powerupType);
                session.bombLastLocations.put(replacementId, current.clone());
                session.bombCreatedAt.put(replacementId, createdAt);
                if (killsAlreadyCreditedToThisTnt > 0) {
                    session.bombKillsByTnt.put(replacementId, killsAlreadyCreditedToThisTnt);
                }
            }
        }
    }

    /**
     * Mirrors BombLobbers.updateParticles: two legacy RED_DUST particles per 0.2-block
     * sample, with the original team-dependent spread and speed. Despite the source's
     * branch on team, its count is 2 (not 0), so the trail uses default red dust rather
     * than RGB-tinted particles.
     */
    private void renderBombLobbersTrail(Session session, UUID ownerId, Location previous, Location current) {
        if (session == null || ownerId == null || previous == null || current == null
                || previous.getWorld() != session.world || current.getWorld() != session.world) return;
        Vector segment = current.toVector().subtract(previous.toVector());
        double distance = segment.length();
        if (!Double.isFinite(distance) || distance <= 1.0E-6D) return;
        int samples = ArcadePortRules.bombLobbersTrailSampleCount(distance);
        if (samples <= 0) return;

        int team = session.teams.getOrDefault(ownerId, 0);
        double spread = ArcadePortRules.bombLobbersTrailOffset(team);
        double speed = ArcadePortRules.bombLobbersTrailSpeed(team);
        Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.0F);
        Vector direction = segment.multiply(1.0D / distance);
        for (int sample = 0; sample < samples; sample++) {
            double offset = sample * ArcadePortRules.BOMB_LOBBERS_TRAIL_SAMPLE_SPACING;
            if (offset > distance) break;
            Location point = previous.clone().add(direction.clone().multiply(offset)).add(0.0D, 0.5D, 0.0D);
            session.world.spawnParticle(Particle.DUST, point, 2,
                    spread, spread, spread, speed, dust);
        }
    }

    private void renderBombLobbersExplosion(Session session, Location blast, Player owner) {
        renderBombLobbersExplosion(session, blast, owner, "normal");
    }

    private void renderBombLobbersExplosion(Session session, Location blast, Player owner, String powerupType) {
        if (session == null || blast == null || blast.getWorld() != session.world) return;
        World world = session.world;

        // The cancelled native explosion normally provides this large emitter. Restore
        // the natural blast first, then layer the player's selected TNT impact cosmetic.
        world.spawnParticle(Particle.EXPLOSION_EMITTER, blast, 1);
        world.spawnParticle(Particle.FLAME, blast, 56, 0.95D, 0.7D, 0.95D, 0.08D);
        world.spawnParticle(Particle.CLOUD, blast, 36, 0.9D, 0.65D, 0.9D, 0.12D);
        world.spawnParticle(Particle.SMOKE, blast, 26, 0.75D, 0.55D, 0.75D, 0.05D);
        if ("super-tnt".equals(powerupType)) {
            world.spawnParticle(Particle.EXPLOSION_EMITTER, blast, 2);
            world.spawnParticle(Particle.FLAME, blast, 80, 1.35D, 0.85D, 1.35D, 0.12D);
            world.spawnParticle(Particle.CLOUD, blast, 52, 1.2D, 0.8D, 1.2D, 0.16D);
        }
        if ("fiery-tnt".equals(powerupType)) {
            world.spawnParticle(Particle.FLAME, blast, 100, 1.05D, 0.75D, 1.05D, 0.12D);
            world.spawnParticle(Particle.LAVA, blast, 20, 0.8D, 0.2D, 0.8D, 0.0D);
        }
        world.playSound(blast, Sound.ENTITY_GENERIC_EXPLODE,
                "super-tnt".equals(powerupType) ? 1.8F : 1.5F,
                "fiery-tnt".equals(powerupType) ? 0.72F : 0.88F);

        String cosmetic = "turtle-shell";
        org.bukkit.plugin.Plugin cosmeticsPlugin = Bukkit.getPluginManager().getPlugin("TestplexModernArcade");
        if (owner != null && cosmeticsPlugin != null && cosmeticsPlugin.isEnabled()) {
            java.io.File preferencesFile = new java.io.File(
                    cosmeticsPlugin.getDataFolder(), "player-preferences.yml");
            if (preferencesFile.isFile()) {
                org.bukkit.configuration.file.YamlConfiguration preferences =
                        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(preferencesFile);
                String path = "players." + owner.getUniqueId()
                        + ".games.bomb-lobbers.cosmetics.tnt-impact.selected";
                cosmetic = preferences.getString(path, "turtle-shell");
            }
        }
        if (cosmetic == null || !List.of("turtle-shell", "starburst-tnt", "pixel-crater").contains(cosmetic)) {
            cosmetic = "turtle-shell";
        }

        if ("turtle-shell".equals(cosmetic)) {
            Particle.DustOptions shellDust = new Particle.DustOptions(Color.fromRGB(75, 190, 135), 1.8F);
            Particle.DustOptions rimDust = new Particle.DustOptions(Color.fromRGB(210, 245, 190), 1.1F);
            for (int i = 0; i < 28; i++) {
                double angle = (Math.PI * 2.0D * i) / 28.0D;
                Location point = blast.clone().add(Math.cos(angle) * 1.55D,
                        0.18D + Math.sin(angle * 3.0D) * 0.12D,
                        Math.sin(angle) * 1.55D);
                world.spawnParticle(Particle.DUST, point, 3, 0.04D, 0.08D, 0.04D, 0.01D, shellDust);
                world.spawnParticle(Particle.DUST, point.clone().add(0.0D, 0.22D, 0.0D),
                        1, 0.02D, 0.02D, 0.02D, 0.01D, rimDust);
                world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.02D, 0.04D, 0.02D, 0.01D);
            }
        } else if ("starburst-tnt".equals(cosmetic)) {
            Particle.DustOptions starDust = new Particle.DustOptions(Color.fromRGB(255, 205, 65), 1.8F);
            for (int ray = 0; ray < 12; ray++) {
                double angle = (Math.PI * 2.0D * ray) / 12.0D;
                for (int step = 1; step <= 4; step++) {
                    Location point = blast.clone().add(Math.cos(angle) * step * 0.48D,
                            0.15D + step * 0.1D, Math.sin(angle) * step * 0.48D);
                    world.spawnParticle(Particle.FIREWORK, point, 3, 0.07D, 0.07D, 0.07D, 0.02D);
                    world.spawnParticle(Particle.DUST, point, 2, 0.05D, 0.05D, 0.05D, 0.01D, starDust);
                }
            }
        } else {
            Particle.DustOptions craterDust = new Particle.DustOptions(Color.fromRGB(255, 95, 35), 1.7F);
            Particle.DustOptions emberDust = new Particle.DustOptions(Color.fromRGB(255, 185, 65), 1.1F);
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != 3) continue;
                    Location point = blast.clone().add(x * 0.52D, 0.12D, z * 0.52D);
                    world.spawnParticle(Particle.DUST, point, 4, 0.03D, 0.04D, 0.03D, 0.01D, craterDust);
                    world.spawnParticle(Particle.FLAME, point, 2, 0.02D, 0.05D, 0.02D, 0.01D);
                }
            }
            world.spawnParticle(Particle.LAVA, blast, 12, 0.65D, 0.1D, 0.65D, 0.0D);
            world.spawnParticle(Particle.DUST, blast, 18, 0.6D, 0.08D, 0.6D, 0.01D, emberDust);
        }
    }

    private void removeBombTracking(Session session, UUID bombId) {
        session.bombOwners.remove(bombId);
        session.bombPowerTypes.remove(bombId);
        session.bombLastLocations.remove(bombId);
        session.bombCreatedAt.remove(bombId);
        session.bombPrimed.remove(bombId);
        session.bombDirectHitResolved.remove(bombId);
        session.bombKillsByTnt.remove(bombId);
    }

    private boolean isBombTouchingAnyBlock(TNTPrimed bomb) {
        Block center = bomb.getLocation().getBlock();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block nearby = center.getRelative(dx, dy, dz);
                    // Reborn primes on any non-air neighbour, including liquids and passable blocks.
                    if (nearby.getType() != Material.AIR) return true;
                }
            }
        }
        return false;
    }

    private void adjustBombLobbersPitch(Session session, Player player, boolean increase) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastAdjust = session.bombLastPitchAdjustAt.getOrDefault(id, 0L);
        if (lastAdjust > 0L && now - lastAdjust < 80L) return;
        session.bombLastPitchAdjustAt.put(id, now);
        int current = session.bombPitchLevels.getOrDefault(id, 2);
        current = increase ? Math.min(3, current + 1) : Math.max(1, current - 1);
        session.bombPitchLevels.put(id, current);
        ItemStack lever = namedItem(Material.LEVER, "Velocity Selector",
                "Throw power " + current + "/3. Right-click +; left-click -.", current);
        player.getInventory().setItem(1, lever);
        player.updateInventory();
        player.sendMessage(ChatColor.YELLOW + "TNT throw power: " + current + "/3.");
    }

    private void throwBombLobbersWindCharge(Session session, Player player) {
        if (session == null || active != session || player == null || !player.isOnline()
                || !session.alive.contains(player.getUniqueId())) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!"wind-charge".equals(bombLobbersPowerupType(held))) return;
        consumeHeld(player);
        Snowball gust = player.launchProjectile(Snowball.class);
        gust.setItem(new ItemStack(Material.WIND_CHARGE));
        gust.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.35D));
        gust.getPersistentDataContainer().set(bombPowerupKey, PersistentDataType.STRING, "wind-projectile");
        session.entities.add(gust);
        player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 0.9F, 0.8F);
        player.sendActionBar(ChatColor.AQUA + "Wind Charge launched!");
    }

    private boolean throwBombLobbersTnt(Session session, Player player) {
        if (active != session || session.type != ArcadePortGame.BOMB_LOBBERS
                || player == null || !player.isOnline()
                || !session.alive.contains(player.getUniqueId())) return false;

        long now = System.currentTimeMillis();
        UUID ownerId = player.getUniqueId();
        long lastThrow = session.bombLastThrowAt.getOrDefault(ownerId, 0L);
        if (lastThrow > 0L && now - lastThrow < 180L) return true;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.TNT) return false;
        String powerupType = bombLobbersPowerupType(held);
        if (powerupType == null) powerupType = "normal";
        session.bombLastThrowAt.put(ownerId, now);
        String throwsPath = localStatsPath(ownerId, "bomb-lobbers") + "thrown";
        plugin.getConfig().set(throwsPath, plugin.getConfig().getInt(throwsPath, 0) + 1);
        consumeHeld(player);

        Location spawn = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.0D));
        TNTPrimed bomb = session.world.spawn(spawn, TNTPrimed.class);
        bomb.setFuseTicks(ArcadePortRules.BOMB_LOBBERS_FUSE_TICKS);
        bomb.setYield("super-tnt".equals(powerupType) ? 6.0F : 4.0F);
        double pitchVelocity = "pitcher".equals(session.bombLobbersKits.get(ownerId))
                ? ArcadePortRules.bombLobbersPitchVelocity(session.bombPitchLevels.getOrDefault(ownerId, 2))
                : 2.0D;
        Vector velocity = player.getLocation().getDirection().normalize().multiply(pitchVelocity);
        velocity.setY(velocity.getY() + 0.1D);
        bomb.setVelocity(velocity);
        session.entities.add(bomb);
        session.bombOwners.put(bomb.getUniqueId(), ownerId);
        session.bombPowerTypes.put(bomb.getUniqueId(), powerupType);
        session.bombLastLocations.put(bomb.getUniqueId(), spawn.clone());
        session.bombCreatedAt.put(bomb.getUniqueId(), now);
        session.bombPrimed.remove(bomb.getUniqueId());
        return true;
    }

    private void useBombLobbersWaller(Session session, Player player, Block clicked) {
        if (session == null || player == null || clicked == null) return;
        long clickAt = System.currentTimeMillis();
        session.bombLastWallerAt.put(player.getUniqueId(), clickAt);
        if (clicked.isLiquid()) {
            player.sendMessage(ChatColor.YELLOW + "You may not place a wall in a liquid!");
            return;
        }
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long readyAt = session.bombWallerReadyAt.getOrDefault(id, 0L);
        if (!ArcadePortRules.isBombLobbersWallerReady(now, readyAt)) return;

        int facingIndex = ArcadePortRules.bombLobbersWallFacingIndex(player.getLocation().getYaw());
        int forwardX = facingIndex == 1 ? 1 : facingIndex == 3 ? -1 : 0;
        int forwardZ = facingIndex == 2 ? 1 : facingIndex == 0 ? -1 : 0;
        int centerX = clicked.getX() + forwardX;
        int centerY = clicked.getY() + 1;
        int centerZ = clicked.getZ() + forwardZ;
        int sideX = forwardZ;
        int sideZ = -forwardX;
        for (int width = -2; width <= 2; width++) {
            for (int height = 0; height < 3; height++) {
                int x = centerX + sideX * width;
                int y = centerY + height;
                int z = centerZ + sideZ * width;
                Block wall = session.world.getBlockAt(x, y, z);
                wall.setType(Material.STONE_BRICKS, false);
                wall.getWorld().spawnParticle(Particle.BLOCK,
                        wall.getLocation().add(0.5D, 0.5D, 0.5D), 5,
                        0.25D, 0.25D, 0.25D, 0.05D, wall.getBlockData());
            }
        }
        session.bombWallerReadyAt.put(id, ArcadePortRules.nextBombLobbersWallerReadyAt(now));
        consumeHeld(player);
    }

    private boolean handleBombLobbersKitInteract(Session session, Player player,
            PlayerInteractEvent event, ItemStack heldItem) {
        if (heldItem == null) return false;
        UUID id = player.getUniqueId();
        String kit = session.bombLobbersKits.getOrDefault(id, "jumper");
        if ("pitcher".equals(kit) && heldItem.getType() == Material.LEVER) {
            event.setCancelled(true);
            adjustBombLobbersPitch(session, player, event.getAction().isRightClick());
            return true;
        }
        if ("waller".equals(kit) && heldItem.getType() == Material.STONE_SHOVEL
                && (event.getAction().isRightClick() || event.getAction().isLeftClick())) {
            event.setCancelled(true);
            Block clicked = event.getClickedBlock();
            // Air and entity clicks have no clickedBlock. Use the block at the player's
            // feet so the wall is still anchored one block ahead at ground level.
            if (clicked == null) clicked = player.getLocation().getBlock().getRelative(0, -1, 0);
            useBombLobbersWaller(session, player, clicked);
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
        UUID id = player.getUniqueId();
        // The source perk only permits the toggle when grounded readiness has recharged.
        if (!session.bombDoubleJumpAvailable.remove(id)) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            return;
        }
        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);
        session.bombDoubleJumpUsed.add(id);
        String kit = session.bombLobbersKits.getOrDefault(id, "jumper");
        double power = "jumper".equals(kit) || "waller".equals(kit) ? 1.2D : 0.9D;
        Vector velocity = ArcadePortRules.bombLobbersDoubleJumpVelocity(
                player.getLocation().getDirection(), power, player.isOnGround());
        player.setFallDistance(0.0F);
        player.setVelocity(velocity);
        session.bombDoubleJumpReadyAt.put(id,
                ArcadePortRules.nextBombLobbersDoubleJumpReadyAt(System.currentTimeMillis()));
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.7F, 1.3F);
    }

    /** Finds the first non-passable/liquid block cell crossed by the projectile's previous-to-current path. */
    private double baconBlastFirstBlockFraction(Session session, Location start, Location end) {
        if (session == null || start == null || end == null
                || start.getWorld() != session.world || end.getWorld() != session.world) return -1.0D;
        Vector segment = end.toVector().subtract(start.toVector());
        double length = segment.length();
        if (!Double.isFinite(length) || length <= 1.0E-6D) return -1.0D;
        int samples = Math.max(1, Math.min(512, (int) Math.ceil(length / 0.1D)));
        for (int i = 1; i <= samples; i++) {
            double fraction = i / (double) samples;
            Location sample = start.clone().add(segment.clone().multiply(fraction));
            Block block = sample.getBlock();
            if (block.isLiquid() || !block.isPassable()) return fraction;
        }
        return -1.0D;
    }

    /** Projection of a point onto the swept projectile segment, clamped to the current tick. */
    private double baconBlastSegmentFraction(Location point, Location start, Location end) {
        Vector segment = end.toVector().subtract(start.toVector());
        double lengthSquared = segment.lengthSquared();
        if (!Double.isFinite(lengthSquared) || lengthSquared < 1.0E-9D) return 0.0D;
        double fraction = point.toVector().subtract(start.toVector()).dot(segment) / lengthSquared;
        return Math.max(0.0D, Math.min(1.0D, fraction));
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
        if (session == null || event.getEntity() == null || event.getEntity().getWorld() != session.world) return;
        if (!(event.getEntity() instanceof TNTPrimed bomb)) {
            event.blockList().clear();
            event.setCancelled(true);
            return;
        }

        UUID bombId = bomb.getUniqueId();
        UUID ownerId = session.bombOwners.get(bombId);
        int priorTntKills = session.bombKillsByTnt.getOrDefault(bombId, 0);
        String bombPowerupType = session.bombPowerTypes.getOrDefault(bombId, "normal");
        boolean bombLobbersBlast = session.type == ArcadePortGame.BOMB_LOBBERS;
        boolean evolutionBlast = session.type == ArcadePortGame.EVOLUTION
                && ownerId != null && session.evolutionLevels.getOrDefault(ownerId, 0) == 3;
        Player owner = ownerId == null ? null : session.players.get(ownerId);
        if (bombLobbersBlast && ArcadePortRules.shouldCancelBombLobbersExplosion(
                owner != null && owner.isOnline(),
                session.teams.getOrDefault(ownerId, -1),
                nearestBombLobbersSpawnTeam(session, bomb.getLocation()))) {
            // Reborn cancels and removes TNT whose thrower left the match, or whose
            // explosion is still on its own side. Never allow an orphaned TNT to blast.
            event.blockList().clear();
            event.setCancelled(true);
            removeBombTracking(session, bombId);
            session.entities.remove(bomb);
            bomb.remove();
            return;
        }

        if (ownerId != null && (bombLobbersBlast || evolutionBlast)) {
            Location blast = bomb.getLocation().clone();
            Map<UUID, Double> blastExposure = new HashMap<>();
            double bombBlastRadius = bombLobbersBlast
                    ? ArcadePortRules.bombLobbersExplosionRadius(bombPowerupType)
                    : ArcadePortRules.BOMB_LOBBERS_EXPLOSION_RADIUS;
            if (bombLobbersBlast) {
                for (Player target : alivePlayers(session)) {
                    // Vanilla 1.8 evaluates explosion intensity at the entity's head position;
                    // block density is still sampled over the complete bounding box.
                    // v1_8_R3 uses Entity.f(x,y,z) (the entity's base position) for
                    // radial damage; the head height is used only for the knockback direction.
                    double distance = target.getLocation().distance(blast);
                    if (distance <= bombBlastRadius) {
                        blastExposure.put(target.getUniqueId(), bombLobbersBlastExposure(blast, target));
                    }
                }
            }
            List<Block> blocksHit = new ArrayList<>(event.blockList());
            event.blockList().clear();
            event.setCancelled(true);
            if (bombLobbersBlast) {
                // Reborn converts stone to cobble, cracks intact stone bricks, and removes other
                // impacted blocks (including stone bricks that were already cracked).
                for (Block block : blocksHit) {
                    if (block == null || block.getWorld() != session.world || block.getType().isAir()) continue;
                    block.setType(ArcadePortRules.bombLobbersBlockAfterExplosion(block.getType()), false);
                }
            }
            if (bombLobbersBlast) {
                session.bombNativeDamageSuppressed.add(bombId);
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> session.bombNativeDamageSuppressed.remove(bombId), 1L);
            }
            removeBombTracking(session, bombId);
            session.entities.remove(bomb);
            bomb.remove();
            if (bombLobbersBlast) {
                renderBombLobbersExplosion(session, blast, owner, bombPowerupType);
            } else {
                session.world.spawnParticle(Particle.EXPLOSION_EMITTER, blast, 1);
                session.world.playSound(blast, Sound.ENTITY_GENERIC_EXPLODE, 1.4F, 0.9F);
            }
            int killsFromThisTnt = priorTntKills;
            for (Player target : alivePlayers(session)) {
                // Reborn applies its nearby-player explosion condition without team filtering.
                double distance = target.getLocation().distance(blast);
                double radius = bombLobbersBlast ? bombBlastRadius : 8.0D;
                if (distance > radius) continue;
                double exposure = bombLobbersBlast
                        ? blastExposure.getOrDefault(target.getUniqueId(), 0.0D) : 1.0D;
                Vector away = target.getEyeLocation().toVector().subtract(blast.toVector());
                if (bombLobbersBlast) {
                    double impact = Math.max(0.0D,
                            1.0D - distance / bombBlastRadius) * exposure;
                    target.setVelocity(ArcadePortRules.bombLobbersExplosionVelocity(
                            target.getVelocity(), away, impact));
                } else {
                    if (away.lengthSquared() < 0.001D) away = new Vector(0.0D, 1.0D, 0.0D);
                    double force = 1.9D * (1.0D - distance / 8.0D);
                    target.setVelocity(away.normalize().multiply(force)
                            .setY(Math.max(0.25D, 0.65D - distance * 0.045D)));
                }
                target.getWorld().spawnParticle(Particle.CLOUD,
                        target.getLocation().add(0.0D, 1.0D, 0.0D),
                        12, 0.35D, 0.45D, 0.35D, 0.05D);
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.2F);
                double blastDamage = bombLobbersBlast
                        ? ArcadePortRules.bombLobbersBlastDamage(distance, exposure, bombBlastRadius,
                                ArcadePortRules.bombLobbersExplosionMaxDamage(bombPowerupType))
                        : Math.max(0.0D, 10.0D * (1.0D - distance / 8.0D));
                if (bombLobbersBlast) {
                    blastDamage = bombLobbersDamageAfterArmor(target, blastDamage);
                    if (ArcadePortRules.bombLobbersTntIgnites(bombPowerupType) && exposure > 0.05D) {
                        target.setFireTicks(100);
                    }
                }
                if (blastDamage > 0.0D) {
                    if (bombLobbersBlast) {
                        session.bombPlayersDamaged.add(target.getUniqueId());
                        recordBombLobbersDamage(session, target, ownerId, System.currentTimeMillis());
                    }
                    if (target.getHealth() <= blastDamage) {
                        if (bombLobbersBlast && owner != null && owner.isOnline()
                                && session.players.containsKey(ownerId) && !target.getUniqueId().equals(ownerId)) {
                            killsFromThisTnt = ArcadePortRules.nextBombLobbersTntKillCount(killsFromThisTnt, true);
                            if (ArcadePortRules.shouldAwardBombLobbersTwoBirdsOneStone(killsFromThisTnt)) {
                                awardBombLobbersAchievement(owner,
                                        BombLobbersAchievement.TWO_BIRDS_ONE_STONE.configKey(ownerId),
                                        "Two birds, one stone — killed 2 players with one TNT.");
                            }
                        }
                        eliminate(session, target, owner != null && owner.isOnline() && session.players.containsKey(ownerId)
                                ? owner : null, "was caught in a TNT blast");
                        if (active != session) return;
                    } else {
                        target.setHealth(target.getHealth() - blastDamage);
                    }
                }
            }
            if (bombLobbersBlast) {
                int ownerTeam = session.teams.getOrDefault(ownerId, -1);
                for (LivingEntity botTarget : lobbyRuntime.botTargets(session.world, ArcadePortGame.BOMB_LOBBERS)) {
                    if (!lobbyRuntime.isBombLobbersEnemyBot(botTarget, ownerTeam)) continue;
                    double distance = botTarget.getLocation().distance(blast);
                    if (distance > bombBlastRadius) continue;
                    double impact = Math.max(0.0D, 1.0D - distance / bombBlastRadius);
                    lobbyRuntime.showBombLobbersBotBlastHit(botTarget, blast, impact,
                            ArcadePortRules.bombLobbersTntIgnites(bombPowerupType));
                }
            }
            return;
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
        if (session.type == ArcadePortGame.MICRO_BATTLES) removeMicroBorderWarning(session, id);
        if (session.type == ArcadePortGame.BOMB_LOBBERS) updateBombLobbersMvpEligibility(session);
        if (session.type == ArcadePortGame.BACON_BRAWL) {
            session.baconBrutusUntil.remove(id);
            session.baconCloakUntil.remove(id);
            removeBaconBabyPig(session, id);
            removeBaconPlayerPig(session, id);
            restoreBaconPlayerVisibilityForOwner(session, id, player);
            restoreBaconDisguiseNameTagEntry(session, id);
            session.baconSlams.remove(id);
            session.baconSlamPowerPercent.remove(id);
            session.baconMeters.remove(id);
            session.baconAbilitiesUsed.remove(id);
            session.baconLastSwingAt.remove(id);
            session.baconHitFeedbackUntil.remove(id);
            session.baconLastHitSource.remove(id);
        }
        PlayerSnapshot snapshot = session.snapshots.get(id);
        if (snapshot != null) pendingRestore.put(id, snapshot);
        if (session.type == ArcadePortGame.DEATH_TAG) {
            session.tagged.remove(id);
            session.alphaChasers.remove(id);
            removeDeathTagDisguise(session, id);
            session.deathTagSoloChaserHitReadyAt.remove(id);
        }
        if (session.type == ArcadePortGame.GLADIATORS) {
            Integer pair = session.gladiatorPairs.get(id);
            if (pair != null) session.gladiatorResolvedPairs.add(pair);
        }
        session.respawning.remove(id);
        session.baconCompassMenuTargets.remove(id);
        session.players.remove(id);
        session.snapshots.remove(id);
        checkRoundEnd(session);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerSnapshot snapshot = pendingRestore.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (snapshot != null) snapshot.restore(player);
            if (!lobby.isLobbyWorld(player.getWorld())) lobby.sendToLobby(player);
            else lobby.ensurePanelItem(player);
        });
    }

    private record PaintballProjectileVisual(Projectile projectile, String style, Color teamColor) {
    }

    private record DragonEscapeGhostFrame(
            long timeMillis, double x, double y, double z,
            float yaw, float pitch, boolean sneaking, boolean sprinting, boolean onGround) {
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
        final Map<UUID, PaintballProjectileVisual> paintballProjectileCosmetics = new HashMap<>();
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
        final Map<UUID, Mob> deathTagDisguises = new HashMap<>();
        boolean deathTagRunnerSpeedUnlocked;
        Scoreboard deathTagScoreboard;
        Objective deathTagScoreboardObjective;
        Team deathTagRunnerScoreboardLine;
        String deathTagRunnerScoreboardEntry;
        Team deathTagChaserScoreboardLine;
        String deathTagChaserScoreboardEntry;
        final Map<UUID, String> deathTagRunnerKits = new HashMap<>();
        final Map<UUID, String> deathTagRunnerTrailColours = new HashMap<>();
        final Map<UUID, String> deathTagRunnerTrailStyles = new HashMap<>();
        final Map<UUID, Integer> deathTagChaserKillsByPlayer = new HashMap<>();
        final Map<UUID, Integer> deathTagComeAtMeBroAwards = new HashMap<>();
        final Map<UUID, Integer> deathTagDeathsByPlayer = new HashMap<>();
        final Map<UUID, Long> deathTagLastArrowAt = new HashMap<>();
        final Map<UUID, Long> deathTagSoloChaserHitReadyAt = new HashMap<>();
        final Set<UUID> deathTagFletchedArrows = new HashSet<>();
        final Map<UUID, Double> deathTagProjectileDamage = new HashMap<>();
        final Map<String, Long> deathTagTraitorKnockbackAt = new HashMap<>();
        final Map<Block, BlockData> deathTagOriginalLightBlocks = new LinkedHashMap<>();
        long deathTagNextLightAt;
        final Map<UUID, String> baconKits = new HashMap<>();
        final Map<UUID, Integer> baconMeters = new HashMap<>();
        final Set<UUID> baconAbilitiesUsed = new HashSet<>();
        final Map<UUID, Integer> baconSlamPowerPercent = new HashMap<>();
        final Map<UUID, Integer> baconBodySlamKills = new HashMap<>();
        final Set<UUID> baconThreeForOneAwarded = new HashSet<>();
        final Map<UUID, Long> baconSlams = new HashMap<>();
        final Map<UUID, Long> baconBrutusUntil = new HashMap<>();
        final Map<UUID, Long> baconCloakUntil = new HashMap<>();
        final Map<UUID, Long> baconLastSwingAt = new HashMap<>();
        final Map<UUID, Long> baconHitFeedbackUntil = new HashMap<>();
        final Map<UUID, String> baconLastHitSource = new HashMap<>();
        final Map<UUID, UUID> baconBabyPigs = new HashMap<>();
        final Map<UUID, Item> baconBlastItems = new HashMap<>();
        final Set<UUID> baconHandledProjectiles = new HashSet<>();
        final Map<UUID, Double> baconBlastPowerById = new HashMap<>();
        final Map<UUID, UUID> baconBlastOwners = new HashMap<>();
        final Map<UUID, Location> baconBlastLastLocations = new HashMap<>();
        final Map<UUID, Long> baconBlastCreatedAt = new HashMap<>();
        final Map<UUID, UUID> baconLastDamager = new HashMap<>();
        final Map<UUID, Long> baconLastDamagedAt = new HashMap<>();
        Scoreboard baconDisguiseScoreboard;
        Team baconDisguiseTeam;
        Scoreboard baconMeterScoreboard;
        Objective baconMeterObjective;
        Objective baconPreviousSidebar;
        final List<Team> baconMeterTeams = new ArrayList<>();
        final List<String> baconMeterEntries = new ArrayList<>();
        final Map<UUID, Team> baconPreviousNameTeams = new HashMap<>();
        final Map<UUID, String> baconNameEntries = new HashMap<>();
        final Map<UUID, Set<UUID>> baconHiddenViewersByOwner = new HashMap<>();
        final Map<UUID, Map<Integer, UUID>> baconCompassMenuTargets = new HashMap<>();
        final Map<UUID, UUID> baconPlayerPigs = new HashMap<>();
        final Map<UUID, UUID> baconPigOwners = new HashMap<>();
        final Set<UUID> baconPigsInBlanketsAwarded = new HashSet<>();
        final Set<UUID> respawning = new HashSet<>();
        final Set<Sheep> sheep = new HashSet<>();
        final Map<Sheep, Integer> sheepOwners = new HashMap<>();
        final Map<Sheep, UUID> sheepCarriers = new LinkedHashMap<>();
        final Set<Entity> entities = new HashSet<>();
        final Map<UUID, UUID> bombOwners = new HashMap<>();
        final Map<UUID, Item> bombPowerupDrops = new HashMap<>();
        final Map<UUID, String> bombPowerTypes = new HashMap<>();
        final Map<UUID, Long> bombLastThrowAt = new HashMap<>();
        long bombNextPowerupSpawnAt;
        final Map<UUID, Long> bombLastPitchAdjustAt = new HashMap<>();
        final Map<UUID, Long> bombLastWallerAt = new HashMap<>();
        final Map<UUID, Integer> bombKillsByTnt = new HashMap<>();
        final Map<UUID, Location> bombLastLocations = new HashMap<>();
        final Map<UUID, Long> bombCreatedAt = new HashMap<>();
        final Set<UUID> bombPrimed = new HashSet<>();
        final Set<UUID> bombDirectHitResolved = new HashSet<>();
        final Map<Integer, Location> bombTeamSpawnCentres = new HashMap<>();
        final Map<UUID, String> bombLobbersKits = new HashMap<>();
        /** One-tick IDs for late native damage events emitted after EntityExplodeEvent. */
        final Set<UUID> bombNativeDamageSuppressed = new HashSet<>();
        final Map<UUID, Integer> bombPitchLevels = new HashMap<>();
        final Set<UUID> bombDoubleJumpAvailable = new HashSet<>();
        final Set<UUID> bombDoubleJumpUsed = new HashSet<>();
        final Set<UUID> bombMvpPressurePlayers = new HashSet<>();
        final Set<UUID> bombPlayersDamaged = new HashSet<>();
        final Map<UUID, Map<UUID, Long>> bombDamageContributors = new HashMap<>();
        final Map<UUID, Integer> bombAssists = new HashMap<>();
        final Map<UUID, Long> bombDoubleJumpReadyAt = new HashMap<>();
        final Map<UUID, Long> bombWallerReadyAt = new HashMap<>();
        final Map<UUID, Long> bombNextCraftAt = new HashMap<>();
        long bombNextBoundaryCheckAt;
        long bombNextCompassUpdateAt;
        final Map<UUID, Map<Integer, UUID>> bombCompassMenuTargets = new HashMap<>();
        final Map<UUID, Long> bombSpectateNextTeleportAt = new HashMap<>();
        final Map<UUID, String> spleefKits = new HashMap<>();
        final Map<UUID, Long> spleefLeapReadyAt = new HashMap<>();
        final Map<UUID, Long> spleefSmasherReadyAt = new HashMap<>();
        final Map<UUID, Long> spleefLastHungerGainAt = new HashMap<>();
        final Map<UUID, Long> spleefNextArrowAt = new HashMap<>();
        final Map<UUID, String> microKits = new HashMap<>();
        final Map<UUID, String> microGlassColours = new HashMap<>();
        final Map<UUID, String> microBorderColours = new HashMap<>();
        final Map<UUID, Set<Block>> microPersonalGlassBlocks = new HashMap<>();
        final Map<UUID, TextDisplay> microBorderWarnings = new HashMap<>();
        boolean microLastTwoAwarded;
        final Map<UUID, Map<Integer, UUID>> microCompassMenuTargets = new HashMap<>();
        final Map<UUID, Long> microSpectateNextTeleportAt = new HashMap<>();
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
        Location dragonEscapeDragonLocation;
        Vector dragonEscapeDragonVelocity = new Vector(0.0D, 0.0D, 1.0D);
        float dragonEscapeDragonPitch;
        double dragonEscapeSpeedMultiplier = 1.0D;
        Location dragonEscapeLastDestructionLocation;
        final List<Location> dragonEscapeWaypoints = new ArrayList<>();
        final Map<UUID, Location> dragonEscapeStartLocations = new HashMap<>();
        final Map<UUID, String> dragonEscapeKits = new HashMap<>();
        final Map<UUID, Integer> dragonEscapeLeapUsesRemaining = new HashMap<>();
        final Map<UUID, Long> dragonEscapeLeapReadyAt = new HashMap<>();
        final Map<UUID, Integer> dragonEscapeDiggerUsesRemaining = new HashMap<>();
        final Map<UUID, Long> dragonEscapeNextTntAt = new HashMap<>();
        final Set<UUID> dragonEscapeWarperUsed = new HashSet<>();
        final Map<UUID, Location> dragonEscapeSafeLocations = new HashMap<>();
        final Map<UUID, Long> dragonEscapeWarpedAt = new HashMap<>();
        final Map<UUID, Long> dragonEscapeWaterDamageNextAt = new HashMap<>();
        final Map<UUID, Integer> dragonEscapeObservedWaypoint = new HashMap<>();
        final Map<UUID, Long> dragonEscapeDiggerNextUseAt = new HashMap<>();
        final Map<Block, BlockData> dragonEscapeTunnelBlocks = new HashMap<>();
        final Map<Block, UUID> dragonEscapeTunnelOwners = new HashMap<>();
        final Map<String, Long> dragonEscapeBlockRestoreTokens = new HashMap<>();
        long dragonEscapeBlockRestoreSequence;
        final Map<UUID, UUID> dragonEscapeMineOwners = new HashMap<>();
        String dragonEscapeMapName = "";
        String dragonEscapeMapResource;
        Location dragonEscapeStartLocation;
        UUID dragonEscapeBeaconLeader;
        String dragonEscapeBeaconColor = "white";
        int dragonEscapeDragonProgress;
        long dragonEscapeNextScoreUpdateAt;
        final Map<UUID, Integer> dragonEscapePlayerProgress = new HashMap<>();
        final Map<UUID, Double> dragonEscapePlayerRankScores = new HashMap<>();
        Scoreboard dragonEscapeScoreboard;
        Objective dragonEscapeScoreboardObjective;
        final List<Team> dragonEscapeScoreboardTeams = new ArrayList<>();
        final List<String> dragonEscapeScoreboardEntries = new ArrayList<>();
        final Map<UUID, Map<Integer, Long>> dragonEscapeRunSplitMillis = new HashMap<>();
        final Map<UUID, Map<Integer, Location>> dragonEscapeRunSplitLocations = new HashMap<>();
        final Map<UUID, List<DragonEscapeGhostFrame>> dragonEscapeRunGhostFrames = new HashMap<>();
        final Map<Block, Long> runnerBlocks = new HashMap<>();
        final Map<Block, Material> runnerBlockMaterials = new HashMap<>();
        final Map<UUID, String> runnerKits = new HashMap<>();
        final Map<UUID, Long> runnerLeapReadyAt = new HashMap<>();
        final Map<UUID, Long> runnerQuickshotReadyAt = new HashMap<>();
        final Map<UUID, Long> runnerNextFrostBallAt = new HashMap<>();
        final Map<UUID, Long> runnerFallingBlockExpiresAt = new HashMap<>();
        final Map<UUID, Location> lastLocations = new HashMap<>();
        final int participantsAtStart;
        int baconBlastExplosionDepth;
        double baconKnockback = 1.5D;
        long lastBaconHungerAt;
        long lastBaconSlowUpdateAt;
        long lastWorldWaterDamageAt;
        long lastBaconEscalationAt;
        long baconEscalationIntervalMillis = ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS;
        int baconKnockbackEscalations;
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
        final boolean collidable;
        final Scoreboard scoreboard;
        final String playerListName;
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
            collidable = p.isCollidable();
            scoreboard = p.getScoreboard();
            playerListName = p.getPlayerListName();
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
            p.setCollidable(collidable);
            p.setScoreboard(scoreboard);
            p.setPlayerListName(playerListName);
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
