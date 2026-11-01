package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Isolated source-mechanics runtime for Castle Assault and Heroes of Gwen.
 *
 * It deliberately owns only match-scoped entities, inventory interactions and
 * source objective data. Player lifecycle and snapshots remain owned by the
 * ArcadePortManager through the supplied callbacks.
 */
final class CastleAssaultHeroesOfGwenRuntime implements Listener {
    interface Hooks {
        void eliminate(Player victim, Player killer, String reason);
        void finish(Player winner, int winningTeam, String reason);
    }

    private static final int CASTLE_KING_HEALTH = 600;
    private static final int CASTLE_CRYSTAL_COUNT = 2;
    private static final long CASTLE_NORMAL_TNT_DELAY = 60_000L;
    private static final long MOBA_PASSIVE_GOLD_INTERVAL = ArcadePortRules.HEROES_OF_GWEN_GOLD_INTERVAL_MILLIS;
    private static final int MOBA_BASE_GOLD = ArcadePortRules.HEROES_OF_GWEN_BASE_GOLD_PER_INTERVAL;
    private static final int MOBA_CAPTURE_GOLD = ArcadePortRules.HEROES_OF_GWEN_CAPTURE_POINT_GOLD_PER_INTERVAL;
    private static final int MOBA_CAPTURE_REWARD = ArcadePortRules.HEROES_OF_GWEN_CAPTURE_POINT_REWARD_GOLD;
    private static final long MOBA_MINION_WAVE_INTERVAL = ArcadePortRules.HEROES_OF_GWEN_MINION_WAVE_INTERVAL_MILLIS;
    private static final long MOBA_MINION_UPDATE_INTERVAL = ArcadePortRules.HEROES_OF_GWEN_MINION_TARGET_UPDATE_MILLIS;
    private static final double MOBA_MINION_TARGET_RANGE_SQUARED = ArcadePortRules.HEROES_OF_GWEN_MINION_TARGET_RANGE_SQUARED;
    private static final long MOBA_TOWER_UPDATE_INTERVAL = 1_000L;
    private static final long MOBA_CAPTURE_UPDATE_INTERVAL = 1_000L;
    private static final double MOBA_TOWER_RANGE_SQUARED = 100.0D;
    private static final double MOBA_CAPTURE_POINT_RANGE_SQUARED = 25.0D;
    private static final double MOBA_TOWER_DAMAGE = 3.0D;
    private static final double MOBA_TOWER_HEAL = 2.6D;

    private final JavaPlugin plugin;
    private final ArcadePortGame game;
    private final World world;
    private final SourceMapCatalog.MapData map;
    private final boolean mixedArcade;
    private final Hooks hooks;
    private final Map<UUID, Integer> teams = new HashMap<>();
    private final Map<UUID, Player> players = new HashMap<>();
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Set<Entity> entities = new HashSet<>();
    private final Map<Block, BlockData> changedBlocks = new HashMap<>();
    private final NamespacedKey objectiveTntKey;
    private final NamespacedKey thrownTntOwnerKey;
    private final NamespacedKey shopItemKey;
    private final NamespacedKey shopRoleKey;
    private final NamespacedKey kingKey;
    private final NamespacedKey towerKey;
    private final NamespacedKey minionTeamKey;
    private final NamespacedKey heroSkillKey;
    private final NamespacedKey heroProjectileTypeKey;
    private final NamespacedKey heroProjectileOwnerKey;
    private final NamespacedKey bardolfWolfOwnerKey;
    private final NamespacedKey rowenaBombardmentItemKey;
    private final NamespacedKey biffHorseOwnerKey;
    private final Map<Integer, KingState> kings = new HashMap<>();
    private final Map<UUID, Integer> playerGold = new HashMap<>();
    private final Map<UUID, ItemStack> carriedTntOldHelmet = new HashMap<>();
    private final Map<Integer, List<TowerState>> towers = new HashMap<>();
    private final Map<UUID, Integer> heroesOfGwenMinionTeams = new HashMap<>();
    private final Set<Integer> heroesOfGwenIronMinionTeams = new HashSet<>();
    private final List<CapturePointState> capturePoints = new ArrayList<>();
    private final Map<Integer, Wither> withers = new HashMap<>();
    private final Map<Integer, Boolean> witherDamageable = new HashMap<>();
    private final Map<Integer, Location> witherHomes = new HashMap<>();
    private final Map<Integer, List<Location>> witherBoundaries = new HashMap<>();
    private final Map<Integer, UUID> witherTargets = new HashMap<>();
    private final Map<Integer, Long> witherNextEarthquakeAt = new HashMap<>();
    private final Set<FallingBlock> witherDebris = new HashSet<>();
    private HeroesOfGwenSidequestType heroesOfGwenSidequestType = HeroesOfGwenSidequestType.NONE;
    private List<Location> heroesOfGwenPumpkinBoundary = List.of();
    private WitherSkeleton heroesOfGwenPumpkinKing;
    private UUID heroesOfGwenPumpkinKingTarget;
    private long heroesOfGwenPumpkinKingRespawnAt;
    private long heroesOfGwenPumpkinKingNextAttackAt;
    private long heroesOfGwenSidequestNextStartAt;
    private long heroesOfGwenMonochromeEndsAt;
    private final Set<UUID> heroesOfGwenMonochromeSkeletons = new HashSet<>();
    private final int[] heroesOfGwenMonochromeKills = new int[2];
    private final Map<UUID, Long> heroesOfGwenPumpkinBuffEndsAt = new HashMap<>();
    private final List<Location> objectiveTntSpawns = new ArrayList<>();
    private final List<Block> castleChests = new ArrayList<>();
    private final Map<UUID, Integer> castleKillStreaks = new HashMap<>();
    private final Map<UUID, String> castleAssaultPlayerKits = new HashMap<>();
    private final Map<UUID, Long> castleArcherNextArrowAt = new HashMap<>();
    private final Map<UUID, Long> castleDemolitionistNextTntAt = new HashMap<>();
    private final Map<UUID, Integer> castleKillsByPlayer = new HashMap<>();
    private final Map<UUID, Integer> castleTntHolderKillsByPlayer = new HashMap<>();
    private final Map<UUID, Integer> castleCrystalsDestroyedByPlayer = new HashMap<>();
    private final Set<UUID> castleRodUsed = new HashSet<>();
    private final Set<UUID> castleFinalKingHitByPlayer = new HashSet<>();
    private final Set<UUID> castleCreditedVictims = new HashSet<>();
    private boolean castleResultRecorded;
    private final Map<UUID, String> selectedHeroesOfGwen = new HashMap<>();
    private final Map<UUID, Integer> heroesOfGwenKills = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> heroesOfGwenRoleKills = new HashMap<>();
    private final Set<UUID> heroesOfGwenCounterpartOnly = new HashSet<>();
    private final Set<UUID> heroesOfGwenChangedAfterTowerLoss = new HashSet<>();
    private final Map<UUID, Set<String>> heroesOfGwenFinalObjectiveHits = new HashMap<>();
    private boolean heroesOfGwenResultRecorded;
    private final Map<UUID, Map<String, Long>> heroSkillReadyAt = new HashMap<>();
    private final Map<UUID, Integer> heroTntArrowCharges = new HashMap<>();
    private final Map<UUID, Long> heroAmmoNextAt = new HashMap<>();
    private final Map<UUID, Long> heroEnderBladeEndsAt = new HashMap<>();
    private final Map<UUID, ItemStack> heroEnderBladeOriginalSword = new HashMap<>();
    private final Map<UUID, Long> heroInfinityEndsAt = new HashMap<>();
    private final Map<UUID, Integer> heroInfinityOriginalEnchant = new HashMap<>();
    private final List<AnathMeteorZone> anathMeteorZones = new ArrayList<>();
    private final Map<UUID, Long> anathFlameDashEndsAt = new HashMap<>();
    private final Map<UUID, AnathBurnBeamState> anathBurnBeams = new HashMap<>();
    private final Map<UUID, AnathFlameEmberState> anathFlameEmbers = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> anathBurnBeamNextHitAt = new HashMap<>();
    private final Map<Block, AnathFireTrailState> anathFireTrailBlocks = new HashMap<>();
    private final List<LarissaFountainZone> larissaFountainZones = new ArrayList<>();
    private final Map<UUID, Long> larissaWaterDashEndsAt = new HashMap<>();
    private final Map<UUID, Long> larissaStormBlessingEndsAt = new HashMap<>();
    private final Map<UUID, Long> larissaStormBlessingNextHealAt = new HashMap<>();
    private final Map<UUID, List<Wolf>> bardolfWolves = new HashMap<>();
    private final Map<UUID, UUID> bardolfWolfTargets = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> bardolfLastWolfAttackAt = new HashMap<>();
    private final Map<UUID, BardolfPounceState> bardolfPounces = new HashMap<>();
    private final Map<UUID, Long> bardolfFullMoonEndsAt = new HashMap<>();
    private final Map<UUID, Float> bardolfOriginalWalkSpeeds = new HashMap<>();
    private final Map<UUID, Integer> rowenaLightArrowsRemaining = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> rowenaLightArrowNextHitAt = new HashMap<>();
    private final Map<UUID, Long> rowenaCombatSlideEndsAt = new HashMap<>();
    private final Map<UUID, RowenaBombardmentState> rowenaBombardments = new HashMap<>();
    private final Map<UUID, Long> danaKnockUpEndsAt = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> danaKnockUpNextHitAt = new HashMap<>();
    private final Map<UUID, DanaRallyState> danaRallies = new HashMap<>();
    private final Map<UUID, List<BiffTetherTarget>> biffTetherTargets = new HashMap<>();
    private final Map<UUID, Long> biffBattleLeapStartedAt = new HashMap<>();
    private final Map<UUID, BiffHorseState> biffHorses = new HashMap<>();
    private final Map<UUID, IvyHookState> ivyHooks = new HashMap<>();
    private final Map<UUID, IvyFloralLeapState> ivyFloralLeaps = new HashMap<>();
    private final Map<UUID, IvyThornWallState> ivyThornWalls = new HashMap<>();
    private final List<StoreEntry> storeEntries = new ArrayList<>();
    private final Map<UUID, StoreHolder> openStores = new HashMap<>();

    private Item objectiveItem;
    private Location activeObjectiveSpawn;
    private UUID objectiveCarrier;
    private long objectiveRespawnAt;
    private long objectiveCarryStartedAt;
    private long nextChestRefillAt;
    private long nextGoldAt;
    private long nextStructureUpdateAt;
    private long nextCapturePointUpdateAt;
    private long nextMinionWaveAt;
    private long nextMinionUpdateAt;
    private long nextKingCageUpdateAt;
    private int objectiveKillsEnabled;
    private int castleKillCountRed;
    private int castleKillCountBlue;
    private boolean ready;
    private boolean stopped;
    private boolean listenersRegistered;

    CastleAssaultHeroesOfGwenRuntime(
            JavaPlugin plugin,
            ArcadePortGame game,
            World world,
            SourceMapCatalog.MapData map,
            List<Player> participants,
            Map<UUID, Integer> teams,
            boolean mixedArcade,
            Hooks hooks) {
        this.plugin = plugin;
        this.game = game;
        this.world = world;
        this.map = map;
        this.mixedArcade = mixedArcade;
        this.hooks = hooks;
        this.objectiveTntKey = new NamespacedKey(plugin, "castle-objective-tnt");
        this.thrownTntOwnerKey = new NamespacedKey(plugin, "castle-thrown-tnt-owner");
        this.shopItemKey = new NamespacedKey(plugin, "heroes-gwen-store-item");
        this.shopRoleKey = new NamespacedKey(plugin, "heroes-gwen-store-role");
        this.kingKey = new NamespacedKey(plugin, "castle-king-team");
        this.towerKey = new NamespacedKey(plugin, "heroes-gwen-tower");
        this.minionTeamKey = new NamespacedKey(plugin, "heroes-gwen-minion-team");
        this.heroSkillKey = new NamespacedKey(plugin, "heroes-gwen-skill");
        this.heroProjectileTypeKey = new NamespacedKey(plugin, "heroes-gwen-projectile-type");
        this.heroProjectileOwnerKey = new NamespacedKey(plugin, "heroes-gwen-projectile-owner");
        this.bardolfWolfOwnerKey = new NamespacedKey(plugin, "heroes-gwen-bardolf-owner");
        this.rowenaBombardmentItemKey = new NamespacedKey(plugin, "heroes-gwen-rowena-bombardment-active");
        this.biffHorseOwnerKey = new NamespacedKey(plugin, "heroes-gwen-biff-horse-owner");
        for (Player player : participants) {
            this.players.put(player.getUniqueId(), player);
            this.activePlayers.add(player.getUniqueId());
        }
        this.teams.putAll(teams);
        if (game == ArcadePortGame.HEROES_OF_GWEN) {
            this.heroesOfGwenCounterpartOnly.addAll(this.players.keySet());
        }

        boolean prepared = game == ArcadePortGame.CASTLE_ASSAULT
                ? prepareCastleAssault()
                : game == ArcadePortGame.HEROES_OF_GWEN && prepareHeroesOfGwen();
        this.ready = prepared;
        if (prepared) {
            long now = System.currentTimeMillis();
            objectiveRespawnAt = now + objectiveDelay();
            nextChestRefillAt = now + 120_000L;
            nextGoldAt = now + MOBA_PASSIVE_GOLD_INTERVAL;
            if (game == ArcadePortGame.HEROES_OF_GWEN) {
                nextMinionWaveAt = now + MOBA_MINION_WAVE_INTERVAL;
                nextMinionUpdateAt = now + MOBA_MINION_UPDATE_INTERVAL;
            }
            nextStructureUpdateAt = now + MOBA_TOWER_UPDATE_INTERVAL;
            plugin.getLogger().info("Prepared " + game.displayName() + (mixedArcade ? " Mixed Arcade" : "")
                    + " source runtime using " + map.mapName() + ".");
        }
    }

    /**
     * The mixed-mode runtime owns separate respawn/refill/update clocks from the
     * parent Arcade session. Pause those clocks as well when Prepare completes.
     */
    void shiftPrepareTimers(long pausedMillis) {
        for (String failure : SessionTimestampShifter.shift(this, pausedMillis)) {
            plugin.getLogger().warning("Could not pause a mixed-mode runtime timer: " + failure);
        }
    }

    boolean isReady() {
        return ready && !stopped;
    }

    /** Activate the mixed-mode event handlers only after the parent match enters Live. */
    void startLive() {
        if (!isReady() || listenersRegistered) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        listenersRegistered = true;
        if (game == ArcadePortGame.HEROES_OF_GWEN
                && heroesOfGwenSidequestType != HeroesOfGwenSidequestType.NONE) {
            long firstSidequestAt = System.currentTimeMillis()
                    + ArcadePortRules.HEROES_OF_GWEN_SIDEQUEST_INITIAL_DELAY_MILLIS;
            if (heroesOfGwenSidequestType == HeroesOfGwenSidequestType.PUMPKIN_KING) {
                heroesOfGwenPumpkinKingRespawnAt = firstSidequestAt;
            } else {
                heroesOfGwenSidequestNextStartAt = firstSidequestAt;
            }
        }
    }

    private boolean prepareCastleAssault() {
        List<Location> redKing = map.dataLocations("RED");
        List<Location> blueKing = map.dataLocations("BLUE");
        List<Location> redCrystals = map.dataLocations("PINK");
        List<Location> blueCrystals = map.dataLocations("LIGHT_BLUE");
        List<Location> tnt = map.dataLocations("BLACK");
        if (redKing.isEmpty() || blueKing.isEmpty()
                || redCrystals.size() < CASTLE_CRYSTAL_COUNT
                || blueCrystals.size() < CASTLE_CRYSTAL_COUNT || tnt.isEmpty()) {
            plugin.getLogger().warning("Castle Assault map " + map.mapName()
                    + " lacks RED/BLUE king markers, two PINK/LIGHT_BLUE crystals, or BLACK objective TNT points.");
            return false;
        }
        spawnKing(0, "King Jon", redKing.get(0));
        spawnKing(1, "King Ryan", blueKing.get(0));
        spawnCrystals(0, redCrystals);
        spawnCrystals(1, blueCrystals);
        tnt.forEach(location -> objectiveTntSpawns.add(location.clone()));
        for (Location location : map.dataLocations("BROWN")) {
            Block block = location.getBlock();
            rememberBlock(block);
            block.setType(Material.CHEST, false);
            castleChests.add(block);
            fillCastleChest(block);
        }
        return true;
    }

    private void spawnKing(int team, String name, Location location) {
        Zombie king = world.spawn(location, Zombie.class);
        king.setAI(false);
        king.setSilent(true);
        king.setInvulnerable(false);
        king.setRemoveWhenFarAway(false);
        king.setCustomName(teamColour(team) + name + ChatColor.RED + " ❤ " + CASTLE_KING_HEALTH);
        king.setCustomNameVisible(true);
        king.getPersistentDataContainer().set(kingKey, PersistentDataType.INTEGER, team);
        kings.put(team, new KingState(team, name, location.clone(), king, CASTLE_KING_HEALTH));
        entities.add(king);
    }

    private void spawnCrystals(int team, List<Location> locations) {
        List<EnderCrystal> result = new ArrayList<>(CASTLE_CRYSTAL_COUNT);
        for (int i = 0; i < CASTLE_CRYSTAL_COUNT; i++) {
            EnderCrystal crystal = world.spawn(locations.get(i), EnderCrystal.class);
            Block beaconBase = locations.get(i).getBlock().getRelative(0, -2, 0);
            rememberBlock(beaconBase);
            beaconBase.setType(Material.BEACON, false);
            crystal.setCustomName(teamColour(team) + (team == 0 ? "Red" : "Blue") + " Sentry Crystal");
            crystal.setCustomNameVisible(true);
            crystal.setInvulnerable(true);
            crystal.getPersistentDataContainer().set(kingKey, PersistentDataType.INTEGER, team);
            entities.add(crystal);
            result.add(crystal);
        }
        kings.get(team).crystals.addAll(result);
    }

    private boolean prepareHeroesOfGwen() {
        Location redBase = first(map.dataLocations("RED"));
        Location blueBase = first(map.dataLocations("BLUE"));
        if (redBase == null) redBase = first(map.dataLocations("AQUA"));
        if (blueBase == null) blueBase = first(map.dataLocations("CYAN"));
        if (redBase == null || blueBase == null) {
            plugin.getLogger().warning("Heroes of Gwen map " + map.mapName()
                    + " has no RED/BLUE Wither base markers in WorldConfig.dat.");
            return false;
        }

        Map<String, List<Location>> sourceMapGroups = map.customLocationGroups();
        if (sourceMapGroups.containsKey("HEROES_VALLEY")) {
            heroesOfGwenSidequestType = HeroesOfGwenSidequestType.PUMPKIN_KING;
            List<Location> pumpkinBoundary = map.dataLocations("GRAY").stream()
                    .map(Location::clone).toList();
            if (map.dataLocations("BLACK").isEmpty() || pumpkinBoundary.size() < 3) {
                plugin.getLogger().warning("Heroes Valley map " + map.mapName()
                        + " is missing BLACK Pumpkin King spawn or GRAY boss-boundary markers; sidequest is unavailable.");
                heroesOfGwenSidequestType = HeroesOfGwenSidequestType.NONE;
            } else {
                heroesOfGwenPumpkinBoundary = orderBoundaryPoints(pumpkinBoundary);
            }
        } else if (sourceMapGroups.containsKey("MONOCHROME")) {
            heroesOfGwenSidequestType = HeroesOfGwenSidequestType.MONOCHROME_SKELETONS;
            if (map.dataLocations("BLACK").isEmpty()) {
                plugin.getLogger().warning("Monochrome map " + map.mapName()
                        + " has no BLACK Wither Skeleton challenge spawn markers; sidequest is unavailable.");
                heroesOfGwenSidequestType = HeroesOfGwenSidequestType.NONE;
            }
        } else {
            plugin.getLogger().warning("Heroes of Gwen map " + map.mapName()
                    + " has no recognised HEROES_VALLEY or MONOCHROME source map type; mid-match sidequest is disabled.");
        }

        int found = 0;
        for (Map.Entry<String, List<Location>> group : map.customLocationGroups().entrySet()) {
            String name = group.getKey();
            List<Location> markerLocations = group.getValue();
            if (markerLocations.isEmpty()) continue;
            if (name.toUpperCase(Locale.ROOT).startsWith("TOWER")) {
                int owner = teamFromMarker(name);
                if (owner < 0) continue;
                boolean middleTower = isMiddleTowerMarker(name);
                if (!ArcadePortRules.heroesOfGwenShouldSpawnTower(mixedArcade, middleTower)) continue;
                // Reborn's getLocationStartsWith("TOWER") uses the first custom location per marker.
                Location location = markerLocations.get(0);
                boolean first = isFirstTowerMarker(name);
                spawnMobaTower(owner, location, first, name);
                found++;
            } else if (name.toUpperCase(Locale.ROOT).startsWith("POINT")) {
                String[] parts = name.split("[ _-]+");
                if (parts.length < 3) continue;
                Location center = markerLocations.get(0).clone();
                capturePoints.add(new CapturePointState(parts[1], parts[2], center, capturePointWool(center)));
            }
        }
        if (found == 0) {
            plugin.getLogger().warning("Heroes of Gwen map " + map.mapName()
                    + " contains no usable TOWER team markers.");
            return false;
        }

        spawnMobaWither(0, redBase);
        spawnMobaWither(1, blueBase);
        initialiseStore();
        return true;
    }

    private void spawnMobaTower(int owner, Location location, boolean first, String sourceMarker) {
        ArmorStand guardian = world.spawn(location, ArmorStand.class);
        guardian.setGravity(false);
        guardian.setInvulnerable(true);
        guardian.setCustomName(teamColour(owner) + (first ? "Outer Tower" : "Inner Tower")
                + ChatColor.GRAY + " " + (first ? 500 : 1000));
        guardian.setCustomNameVisible(true);
        guardian.getPersistentDataContainer().set(towerKey, PersistentDataType.STRING, sourceMarker);

        Location base = findTowerCrystalBase(location);
        EnderCrystal crystal = world.spawn(base, EnderCrystal.class);
        crystal.setInvulnerable(true);
        crystal.setCustomName(teamColour(owner) + "Tower");
        crystal.setCustomNameVisible(true);
        crystal.getPersistentDataContainer().set(towerKey, PersistentDataType.STRING, sourceMarker);
        entities.add(guardian);
        entities.add(crystal);
        towers.computeIfAbsent(owner, ignored -> new ArrayList<>())
                .add(new TowerState(owner, location.clone(), sourceMarker, first, first ? 500.0D : 1_000.0D,
                        guardian, crystal));
    }

    private void spawnHeroesOfGwenMinionWave(int team) {
        List<Location> starts = map.teamSpawnLocationsForTeam(team, 2);
        if (starts.isEmpty()) {
            Location home = witherHomes.get(team);
            if (home != null) starts = List.of(home.clone());
        }
        if (starts.isEmpty()) {
            plugin.getLogger().warning("Heroes of Gwen cannot spawn the " + teamName(team)
                    + " minion wave: the source map has no team spawn markers.");
            return;
        }
        EntityType type = team == 0 ? EntityType.ZOMBIE : EntityType.ZOMBIFIED_PIGLIN;
        for (int i = 0; i < ArcadePortRules.HEROES_OF_GWEN_MINIONS_PER_WAVE; i++) {
            Location spawn = starts.get(i % starts.size()).clone();
            spawn.add(ThreadLocalRandom.current().nextDouble(-1.25D, 1.25D), 0.0D,
                    ThreadLocalRandom.current().nextDouble(-1.25D, 1.25D));
            Location enemyHome = witherHomes.get(1 - team);
            if (enemyHome != null) {
                Vector facing = enemyHome.toVector().subtract(spawn.toVector());
                if (facing.lengthSquared() > 0.001D) spawn.setDirection(facing);
            }
            Entity entity = world.spawnEntity(spawn, type);
            if (!(entity instanceof Mob minion)) {
                entity.remove();
                continue;
            }
            if (minion instanceof Ageable ageable) ageable.setBaby();
            minion.setAware(true);
            minion.setRemoveWhenFarAway(false);
            minion.setCanPickupItems(false);
            minion.setTarget(null);
            minion.setPersistent(false);
            minion.getPersistentDataContainer().set(minionTeamKey, PersistentDataType.INTEGER, team);
            heroesOfGwenMinionTeams.put(minion.getUniqueId(), team);
            entities.add(minion);
            if (heroesOfGwenIronMinionTeams.contains(team)) applyHeroesOfGwenIronMinionArmour(minion);
        }
    }

    private void applyHeroesOfGwenIronMinionArmour(Mob minion) {
        var equipment = minion.getEquipment();
        if (equipment == null) return;
        equipment.setHelmet(new ItemStack(Material.IRON_HELMET));
        equipment.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        equipment.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        equipment.setBoots(new ItemStack(Material.IRON_BOOTS));
        equipment.setHelmetDropChance(0.0F);
        equipment.setChestplateDropChance(0.0F);
        equipment.setLeggingsDropChance(0.0F);
        equipment.setBootsDropChance(0.0F);
    }

    private List<Mob> liveHeroesOfGwenMinions() {
        List<Mob> live = new ArrayList<>();
        for (UUID id : new ArrayList<>(heroesOfGwenMinionTeams.keySet())) {
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Mob minion) || !minion.isValid() || minion.isDead()) {
                heroesOfGwenMinionTeams.remove(id);
                if (entity != null) entities.remove(entity);
                continue;
            }
            live.add(minion);
        }
        return live;
    }

    private LivingEntity nextHeroesOfGwenMinionTarget(Mob minion, int team, List<Mob> minions) {
        Mob enemyMinion = minions.stream()
                .filter(other -> other != minion)
                .filter(other -> heroesOfGwenMinionTeams.getOrDefault(other.getUniqueId(), -1) == 1 - team)
                .filter(other -> other.getWorld() == world
                        && other.getLocation().distanceSquared(minion.getLocation())
                                <= MOBA_MINION_TARGET_RANGE_SQUARED)
                .min(java.util.Comparator
                        .comparingDouble((Mob other) -> other.getHealth())
                        .thenComparingDouble(other -> other.getLocation().distanceSquared(minion.getLocation())))
                .orElse(null);
        if (enemyMinion != null) return enemyMinion;
        return towers.getOrDefault(1 - team, List.of()).stream()
                .filter(tower -> !tower.dead && tower.guardian != null && tower.guardian.isValid())
                .sorted(java.util.Comparator.comparingInt(tower -> tower.first ? 0 : 1))
                .map(tower -> (LivingEntity) tower.guardian)
                .findFirst().orElse(null);
    }

    private void updateHeroesOfGwenMinions(long now) {
        if (game != ArcadePortGame.HEROES_OF_GWEN || stopped) return;
        if (now >= nextMinionWaveAt) {
            spawnHeroesOfGwenMinionWave(0);
            spawnHeroesOfGwenMinionWave(1);
            nextMinionWaveAt = now + MOBA_MINION_WAVE_INTERVAL;
        }
        if (now < nextMinionUpdateAt) return;
        nextMinionUpdateAt = now + MOBA_MINION_UPDATE_INTERVAL;
        List<Mob> minions = liveHeroesOfGwenMinions();
        for (Mob minion : minions) {
            int team = heroesOfGwenMinionTeams.getOrDefault(minion.getUniqueId(), -1);
            if (team < 0) continue;
            LivingEntity target = nextHeroesOfGwenMinionTarget(minion, team, minions);
            if (minion.getTarget() != target) minion.setTarget(target);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHeroesOfGwenMinionDamage(EntityDamageByEntityEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Integer victimTeam = heroesOfGwenMinionTeams.get(event.getEntity().getUniqueId());
        if (victimTeam == null) return;
        Integer attackerTeam = heroesOfGwenMinionTeams.get(event.getDamager().getUniqueId());
        Player attackerPlayer = resolveAttacker(event);
        if (attackerPlayer != null) {
            attackerTeam = teams.getOrDefault(attackerPlayer.getUniqueId(), -1);
            if (!activePlayers.contains(attackerPlayer.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
        if (attackerTeam != null && attackerTeam == victimTeam) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroesOfGwenMinionDeath(EntityDeathEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        UUID id = event.getEntity().getUniqueId();
        if (heroesOfGwenMinionTeams.remove(id) == null) return;
        entities.remove(event.getEntity());
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    private void spawnMobaWither(int team, Location location) {
        Wither wither = world.spawn(location, Wither.class);
        var maxHealth = wither.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(275.0D);
        wither.setHealth(275.0D);
        wither.setAI(false);
        wither.setGravity(false);
        wither.setInvulnerable(false);
        wither.setSilent(true);
        wither.setRemoveWhenFarAway(false);
        wither.setCustomName(teamColour(team) + (team == 0 ? "Red" : "Blue") + " Wither");
        wither.setCustomNameVisible(true);
        wither.getPersistentDataContainer().set(kingKey, PersistentDataType.INTEGER, team);
        entities.add(wither);
        withers.put(team, wither);
        witherDamageable.put(team, false);
        witherHomes.put(team, location.clone());
        String boundaryMarker = team == 0 ? "ORANGE" : "LIGHT_BLUE";
        List<Location> boundary = new ArrayList<>(map.dataLocations(boundaryMarker).stream()
                .map(Location::clone).toList());
        if (boundary.size() >= 3) {
            double centerX = boundary.stream().mapToDouble(Location::getX).average().orElse(0.0D);
            double centerZ = boundary.stream().mapToDouble(Location::getZ).average().orElse(0.0D);
            boundary.sort(java.util.Comparator.comparingDouble(point ->
                    Math.atan2(point.getZ() - centerZ, point.getX() - centerX)));
        }
        witherBoundaries.put(team, boundary);
        witherNextEarthquakeAt.put(team, System.currentTimeMillis() + ArcadePortRules.HEROES_OF_GWEN_WITHER_EARTHQUAKE_INTERVAL_MILLIS);
    }

    private void updateMobaWithers(long now) {
        for (int team = 0; team < 2; team++) {
            final int ownerTeam = team;
            Wither wither = withers.get(team);
            Location home = witherHomes.get(team);
            if (wither == null || !wither.isValid() || home == null) continue;

            Entity currentTargetEntity = witherTargets.containsKey(team)
                    ? Bukkit.getEntity(witherTargets.get(team)) : null;
            LivingEntity target = currentTargetEntity instanceof LivingEntity living ? living : null;
            if (!isValidWitherTarget(team, target)) {
                target = world.getEntities().stream()
                        .filter(entity -> entity instanceof LivingEntity)
                        .map(entity -> (LivingEntity) entity)
                        .filter(candidate -> isValidWitherTarget(ownerTeam, candidate))
                        .min(java.util.Comparator.comparingDouble(candidate ->
                                candidate.getLocation().distanceSquared(home)))
                        .orElse(null);
                if (target == null) witherTargets.remove(team);
                else witherTargets.put(team, target.getUniqueId());
            }

            Location goal = target == null ? home : target.getLocation();
            moveWitherDirectly(wither, goal, target == null
                    ? ArcadePortRules.HEROES_OF_GWEN_WITHER_HOME_SPEED : ArcadePortRules.HEROES_OF_GWEN_WITHER_TARGET_SPEED);
            if (target != null) {
                long nextAttack = witherNextEarthquakeAt.getOrDefault(team, now);
                if (now >= nextAttack) {
                    earthquakeWither(team, wither);
                    witherNextEarthquakeAt.put(team, now + ArcadePortRules.HEROES_OF_GWEN_WITHER_EARTHQUAKE_INTERVAL_MILLIS);
                }
            }
        }
    }

    private boolean isValidWitherTarget(int team, LivingEntity target) {
        if (target == null || target.equals(withers.get(team)) || !target.isValid() || target.isDead()
                || !isInsideWitherBoundary(team, target.getLocation())) return false;
        boolean activeTarget = !(target instanceof Player player)
                || (player.isOnline() && activePlayers.contains(player.getUniqueId()));
        return ArcadePortRules.heroesOfGwenWitherCanTarget(
                team, heroesOfGwenEntityTeam(target), activeTarget);
    }

    private boolean isInsideWitherBoundary(int team, Location location) {
        List<Location> points = witherBoundaries.getOrDefault(team, List.of());
        if (points.size() < 3) return false;
        double x = location.getX();
        double z = location.getZ();
        boolean inside = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            double xi = points.get(i).getX();
            double zi = points.get(i).getZ();
            double xj = points.get(j).getX();
            double zj = points.get(j).getZ();
            boolean crosses = (zi > z) != (zj > z)
                    && x < (xj - xi) * (z - zi) / ((zj - zi) == 0.0D ? 0.000001D : zj - zi) + xi;
            if (crosses) inside = !inside;
        }
        return inside;
    }

    private void moveWitherDirectly(Wither wither, Location goal, double sourceSpeed) {
        Location current = wither.getLocation();
        Vector direction = goal.toVector().subtract(current.toVector());
        if (direction.lengthSquared() < 4.0D) return;
        current.setDirection(direction);
        current.add(direction.normalize().multiply(ArcadePortRules.heroesOfGwenWitherStepPerTick(sourceSpeed)));
        wither.teleport(current);
        wither.setVelocity(new Vector());
    }

    private void earthquakeWither(int team, Wither wither) {
        Location center = wither.getLocation();
        for (Entity entity : world.getNearbyEntities(center, ArcadePortRules.HEROES_OF_GWEN_WITHER_EARTHQUAKE_RADIUS,
                ArcadePortRules.HEROES_OF_GWEN_WITHER_EARTHQUAKE_RADIUS, ArcadePortRules.HEROES_OF_GWEN_WITHER_EARTHQUAKE_RADIUS)) {
            if (!(entity instanceof LivingEntity target) || target.equals(wither)
                    || !target.isValid() || target.isDead()) continue;
            boolean activeTarget = !(target instanceof Player player)
                    || (player.isOnline() && activePlayers.contains(player.getUniqueId()));
            if (!ArcadePortRules.heroesOfGwenWitherCanTarget(
                    team, heroesOfGwenEntityTeam(target), activeTarget)) continue;
            double distance = Math.min(center.distance(target.getLocation()),
                    center.distance(target.getEyeLocation()));
            if (distance >= ArcadePortRules.HEROES_OF_GWEN_WITHER_EARTHQUAKE_RADIUS) continue;
            double damage = ArcadePortRules.heroesOfGwenWitherEarthquakeDamage(distance);
            if (target instanceof Player player) {
                if (player.getHealth() <= damage) {
                    hooks.eliminate(player, null, "was struck by the enemy Wither's Earthquake");
                } else {
                    player.setHealth(player.getHealth() - damage);
                }
            } else {
                target.damage(damage, wither);
            }
            Vector knockback = target.getLocation().toVector().subtract(center.toVector());
            if (knockback.lengthSquared() > 0.001D) {
                target.setVelocity(knockback.normalize().setY(1.0D));
            }
        }

        int radius = (int) ArcadePortRules.HEROES_OF_GWEN_WITHER_EARTHQUAKE_RADIUS;
        int minY = Math.max(world.getMinHeight(), center.getBlockY() - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, center.getBlockY() + radius);
        for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isAir() || !block.getRelative(BlockFace.UP).getType().isAir()
                            || block.getLocation().distanceSquared(center) > radius * radius
                            || ThreadLocalRandom.current().nextDouble() > 0.1D) continue;
                    FallingBlock debris = world.spawnFallingBlock(
                            block.getLocation().add(0.5D, 1.0D, 0.5D), block.getBlockData());
                    debris.setHurtEntities(false);
                    debris.setDropItem(false);
                    Vector trajectory = debris.getLocation().toVector().subtract(center.toVector());
                    if (trajectory.lengthSquared() > 0.001D) {
                        debris.setVelocity(trajectory.normalize().multiply(0.25D)
                                .setY(0.7D + ThreadLocalRandom.current().nextDouble()));
                    }
                    witherDebris.add(debris);
                }
            }
        }
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.7F);
        world.spawnParticle(Particle.EXPLOSION, center, 2, 0.4D, 0.3D, 0.4D, 0.02D);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onWitherDebrisLand(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock falling && witherDebris.remove(falling)) {
            event.setCancelled(true);
            falling.remove();
        }
    }

    private Location findTowerCrystalBase(Location location) {
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int y = location.getBlockY();
        int minY = Math.max(world.getMinHeight() + 1, y - 12);
        for (int candidate = y; candidate >= minY; candidate--) {
            Block block = world.getBlockAt(x, candidate, z);
            if (!block.isPassable()) return new Location(world, x + 0.5D, candidate + 1.0D, z + 0.5D);
        }
        return location.clone().add(0.5D, 1.0D, 0.5D);
    }

    private void initialiseStore() {
        addStoreEntry("Assassin", Material.GOLDEN_SWORD, "Sword of Time", 800, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.IRON_SWORD, "Adventurer's Sword", 1000, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.DIAMOND_SWORD, "Pumpkin King's Blade", 1750, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_HELMET, "Leather Cap", 200, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_HELMET, "Ninja's Mask", 500, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_HELMET, "Urchin's Cap", 600, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.CHAINMAIL_HELMET, "Bruiser's Helm", 1000, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_CHESTPLATE, "Leather Chestplate", 250, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_CHESTPLATE, "Ninja's Chestcloth", 750, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_CHESTPLATE, "Urchin's Chestcloth", 850, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.CHAINMAIL_CHESTPLATE, "Bruiser's Chestplate", 1250, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_LEGGINGS, "Leather Leggings", 250, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_LEGGINGS, "Ninja's Leggings", 750, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_LEGGINGS, "Urchin's Leggings", 850, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.CHAINMAIL_LEGGINGS, "Bruiser's Leggings", 1250, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_BOOTS, "Leather Boots", 200, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_BOOTS, "Ninja's Boots", 500, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.LEATHER_BOOTS, "Urchin's Boots", 600, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.CHAINMAIL_BOOTS, "Bruiser's Boots", 1000, "Legacy Assassin store item.");
        addStoreEntry("Warrior", Material.STONE_SWORD, "Stone Sword", 500, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.GOLDEN_SWORD, "Frostbound Sword", 750, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.GOLDEN_SWORD, "Swift Blade", 750, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_SWORD, "Knight's Blade", 1250, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_HELMET, "Archer's Bane", 150, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_HELMET, "Superior Archer's Bane", 350, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_HELMET, "Brawler's Plate", 350, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_HELMET, "Superior Brawler's Plate", 750, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.DIAMOND_HELMET, "Prince's Plate", 1200, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_CHESTPLATE, "Archer's Bane", 200, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_CHESTPLATE, "Superior Archer's Bane", 450, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_CHESTPLATE, "Brawler's Plate", 450, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_CHESTPLATE, "Superior Brawler's Plate", 1000, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.DIAMOND_CHESTPLATE, "Prince's Plate", 1500, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_LEGGINGS, "Archer's Bane", 200, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_LEGGINGS, "Superior Archer's Bane", 450, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_LEGGINGS, "Brawler's Plate", 450, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_LEGGINGS, "Superior Brawler's Plate", 1000, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.DIAMOND_LEGGINGS, "Prince's Plate", 1500, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_BOOTS, "Archer's Bane", 150, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_BOOTS, "Superior Archer's Bane", 350, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_BOOTS, "Brawler's Plate", 350, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.IRON_BOOTS, "Superior Brawler's Plate", 750, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.DIAMOND_BOOTS, "Prince's Plate", 1200, "Legacy Warrior store item.");
        addStoreEntry("Mage", Material.LEATHER_HELMET, "Leather Cap", 200, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_HELMET, "Adept's Cap", 750, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_HELMET, "Helm of Focus", 750, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_HELMET, "Battle Mage Cap", 750, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.GOLDEN_HELMET, "Golden Helmet", 1000, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_CHESTPLATE, "Leather Chestplate", 400, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_CHESTPLATE, "Adept's Chestplate", 1500, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_CHESTPLATE, "Chestplate of Focus", 1250, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_CHESTPLATE, "Battle Mage Chestplate", 1250, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.GOLDEN_CHESTPLATE, "Golden Chestplate", 1250, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_LEGGINGS, "Leather Leggings", 400, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_LEGGINGS, "Adept's Leggings", 1000, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_LEGGINGS, "Leggings of Focus", 1000, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_LEGGINGS, "Battle Mage Leggings", 1000, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.GOLDEN_LEGGINGS, "Golden Leggings", 1000, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_BOOTS, "Leather Boots", 200, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_BOOTS, "Adept's Boots", 750, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_BOOTS, "Boots of Focus", 750, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.LEATHER_BOOTS, "Battle Mage Boots", 750, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.GOLDEN_BOOTS, "Golden Boots", 1000, "Legacy Mage store item.");
        addStoreEntry("Hunter", Material.BOW, "Hunter's Bow", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.BOW, "Elven Bow", 750, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.BOW, "Eagle's Bow", 1000, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.BOW, "Bow of Pursuit", 1200, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.BOW, "Vampiric Bow", 1500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.BOW, "Bow of Renewal", 1750, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.BOW, "Specialist's Bow", 1000, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_HELMET, "Leather Helmet", 200, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_HELMET, "Leather Cap of Holding", 400, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_HELMET, "Leather Cap of Nimble Fingers", 400, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_HELMET, "Focused Cap", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_HELMET, "Vampiric Helmet", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.CHAINMAIL_HELMET, "Chainmail Helmet", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_CHESTPLATE, "Leather Chestplate", 250, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_CHESTPLATE, "Leather Chestplate of Nimble Fingers", 750, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_CHESTPLATE, "Focused Chestplate", 750, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_CHESTPLATE, "Vampiric Chestplate", 750, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.CHAINMAIL_CHESTPLATE, "Chainmail Chestplate", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_LEGGINGS, "Leather Leggings", 250, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_LEGGINGS, "Leather Leggings of Nimble Fingers", 750, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_LEGGINGS, "Focused Leggings", 750, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_LEGGINGS, "Vampiric Leggings", 700, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.CHAINMAIL_LEGGINGS, "Chainmail Leggings", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_BOOTS, "Leather Boots", 250, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_BOOTS, "Leather Boots of Nimble Fingers", 400, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_BOOTS, "Focused Boots", 600, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_BOOTS, "Vampiric Boots", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.LEATHER_BOOTS, "Leather Hiking Boots", 300, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.CHAINMAIL_BOOTS, "Leather Moccasins", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.CHAINMAIL_BOOTS, "Leather Crosstrainers", 800, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.CHAINMAIL_BOOTS, "Chainmail Boots", 500, "Legacy Hunter store item.");
        addStoreEntry("Assassin", Material.POTION, "Small Health Potion", 100, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.POTION, "Large Health Potion", 200, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.POTION, "Power Potion", 1000, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.POTION, "Speed Potion", 500, "Legacy Assassin store item.");
        addStoreEntry("Assassin", Material.ENDER_PEARL, "Ender Pearl", 750, "Legacy Assassin store item.");
        addStoreEntry("Warrior", Material.POTION, "Small Health Potion", 100, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.POTION, "Large Health Potion", 200, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.POTION, "Power Potion", 1000, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.POTION, "Speed Potion", 500, "Legacy Warrior store item.");
        addStoreEntry("Warrior", Material.ENDER_PEARL, "Ender Pearl", 750, "Legacy Warrior store item.");
        addStoreEntry("Mage", Material.POTION, "Small Health Potion", 100, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.POTION, "Large Health Potion", 200, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.POTION, "Power Potion", 1000, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.POTION, "Speed Potion", 500, "Legacy Mage store item.");
        addStoreEntry("Mage", Material.ENDER_PEARL, "Ender Pearl", 750, "Legacy Mage store item.");
        addStoreEntry("Hunter", Material.POTION, "Small Health Potion", 100, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.POTION, "Large Health Potion", 200, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.POTION, "Power Potion", 1000, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.POTION, "Speed Potion", 500, "Legacy Hunter store item.");
        addStoreEntry("Hunter", Material.ENDER_PEARL, "Ender Pearl", 750, "Legacy Hunter store item.");
    }

    private void addStoreEntry(String role, Material material, String name, int cost, String description) {
        storeEntries.add(new StoreEntry(role, material, name, cost, description));
    }

    private static Location first(List<Location> locations) {
        return locations == null || locations.isEmpty() ? null : locations.get(0).clone();
    }

    private static int teamFromMarker(String marker) {
        String upper = marker.toUpperCase(Locale.ROOT);
        String[] parts = upper.split("[ _-]+");
        for (String part : parts) {
            if ("RED".equals(part)) return 0;
            if ("BLUE".equals(part) || "AQUA".equals(part) || "CYAN".equals(part)) return 1;
        }
        return -1;
    }

    private static boolean isFirstTowerMarker(String marker) {
        String[] parts = marker.trim().toUpperCase(Locale.ROOT).split("[ _-]+");
        return parts.length > 0 && "1".equals(parts[parts.length - 1]);
    }

    /**
     * The Reborn Heroes of GWEN archives name the two towers per team
     * "TOWER <team> 1" and "TOWER <team> 2". In the source layouts, marker
     * 1 sits beside the mid-map beacon line; marker 2 is the base-side tower.
     * Mixed Arcade removes only those central marker-1 towers.
     */
    static boolean isMiddleTowerMarker(String marker) {
        if (marker == null) return false;
        String upper = marker.toUpperCase(Locale.ROOT);
        if (upper.contains("MIDDLE") || upper.contains("MIDLANE")
                || upper.contains("MID LANE") || upper.contains("CENTER")
                || upper.contains("CENTRE")) return true;
        String[] parts = upper.trim().split("[ _-]+");
        if (parts.length >= 3 && "TOWER".equals(parts[0]) && "1".equals(parts[parts.length - 1])) {
            return true;
        }
        for (String part : parts) {
            if ("MID".equals(part) || "MIDDLE".equals(part) || "CENTER".equals(part)
                    || "CENTRE".equals(part)) return true;
        }
        return false;
    }

    private List<Block> capturePointWool(Location center) {
        List<Block> result = new ArrayList<>();
        int x = center.getBlockX(), y = center.getBlockY(), z = center.getBlockZ();
        int radius = 5;
        for (int bx = x - radius; bx <= x + radius; bx++) {
            for (int by = y - 2; by <= y + 2; by++) {
                for (int bz = z - radius; bz <= z + radius; bz++) {
                    Block block = world.getBlockAt(bx, by, bz);
                    if (block.getLocation().distanceSquared(center) > MOBA_CAPTURE_POINT_RANGE_SQUARED) continue;
                    String material = block.getType().name();
                    if (material.equals("WOOL") || material.endsWith("_WOOL")) result.add(block);
                }
            }
        }
        return result;
    }

    private long objectiveDelay() {
        return ArcadePortRules.castleAssaultObjectiveTntRespawnMillis(mixedArcade);
    }

    private List<Location> orderBoundaryPoints(List<Location> points) {
        if (points == null || points.size() < 3) return List.of();
        List<Location> ordered = new ArrayList<>(points.stream().map(Location::clone).toList());
        double centerX = ordered.stream().mapToDouble(Location::getX).average().orElse(0.0D);
        double centerZ = ordered.stream().mapToDouble(Location::getZ).average().orElse(0.0D);
        ordered.sort(java.util.Comparator.comparingDouble(point ->
                Math.atan2(point.getZ() - centerZ, point.getX() - centerX)));
        return List.copyOf(ordered);
    }

    private void updateHeroesOfGwenSidequest(long now) {
        heroesOfGwenPumpkinBuffEndsAt.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (heroesOfGwenSidequestType == HeroesOfGwenSidequestType.PUMPKIN_KING) {
            if (heroesOfGwenPumpkinKing == null && heroesOfGwenPumpkinKingRespawnAt > 0L
                    && now >= heroesOfGwenPumpkinKingRespawnAt) {
                spawnHeroesOfGwenPumpkinKing(now);
            }
            if (heroesOfGwenPumpkinKing != null) updateHeroesOfGwenPumpkinKing(now);
        } else if (heroesOfGwenSidequestType == HeroesOfGwenSidequestType.MONOCHROME_SKELETONS) {
            if (heroesOfGwenMonochromeEndsAt > 0L && now >= heroesOfGwenMonochromeEndsAt) {
                finishHeroesOfGwenMonochromeSidequest();
            }
            if (heroesOfGwenMonochromeEndsAt == 0L && heroesOfGwenSidequestNextStartAt > 0L
                    && now >= heroesOfGwenSidequestNextStartAt) {
                startHeroesOfGwenMonochromeSidequest(now);
            }
        }
    }

    private void spawnHeroesOfGwenPumpkinKing(long now) {
        List<Location> spawns = map.dataLocations("BLACK");
        if (spawns.isEmpty() || heroesOfGwenPumpkinBoundary.size() < 3) {
            plugin.getLogger().warning("Cannot spawn the Heroes of Gwen Pumpkin King: source spawn/boundary markers are missing.");
            heroesOfGwenPumpkinKingRespawnAt = 0L;
            return;
        }
        WitherSkeleton king = world.spawn(spawns.get(0).clone(), WitherSkeleton.class);
        king.setAI(false);
        king.setSilent(false);
        king.setRemoveWhenFarAway(false);
        king.setCanPickupItems(false);
        var maximumHealth = king.getAttribute(Attribute.MAX_HEALTH);
        if (maximumHealth != null) maximumHealth.setBaseValue(ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_HEALTH);
        king.setHealth(ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_HEALTH);
        king.getEquipment().setHelmet(new ItemStack(Material.PUMPKIN));
        king.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
        king.getEquipment().setHelmetDropChance(0.0F);
        king.getEquipment().setItemInMainHandDropChance(0.0F);
        king.setCustomName(ChatColor.DARK_RED + "Pumpkin King " + ChatColor.RED + "❤ "
                + ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_HEALTH);
        king.setCustomNameVisible(true);
        heroesOfGwenPumpkinKing = king;
        heroesOfGwenPumpkinKingTarget = null;
        heroesOfGwenPumpkinKingNextAttackAt = now;
        heroesOfGwenPumpkinKingRespawnAt = 0L;
        entities.add(king);
        world.strikeLightningEffect(king.getLocation());
        world.playSound(king.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0F, 0.4F);
        announce(ChatColor.DARK_RED + "The Pumpkin King has awoken! "
                + ChatColor.GRAY + "Defeat him to grant your team a powerful blessing.");
    }

    private boolean isInsideHeroesOfGwenPumpkinBoundary(Location location) {
        List<Location> points = heroesOfGwenPumpkinBoundary;
        if (location == null || points.size() < 3 || location.getWorld() != world) return false;
        double x = location.getX();
        double z = location.getZ();
        boolean inside = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            double xi = points.get(i).getX();
            double zi = points.get(i).getZ();
            double xj = points.get(j).getX();
            double zj = points.get(j).getZ();
            boolean crosses = (zi > z) != (zj > z)
                    && x < (xj - xi) * (z - zi) / ((zj - zi) == 0.0D ? 0.000001D : zj - zi) + xi;
            if (crosses) inside = !inside;
        }
        return inside;
    }

    private boolean isValidHeroesOfGwenPumpkinTarget(LivingEntity target) {
        if (target == null || target.equals(heroesOfGwenPumpkinKing)
                || !target.isValid() || target.isDead()
                || !isInsideHeroesOfGwenPumpkinBoundary(target.getLocation())) return false;
        if (target instanceof Player player) {
            return player.isOnline() && activePlayers.contains(player.getUniqueId());
        }
        return true;
    }

    private LivingEntity findHeroesOfGwenPumpkinTarget() {
        Entity currentEntity = heroesOfGwenPumpkinKingTarget == null
                ? null : Bukkit.getEntity(heroesOfGwenPumpkinKingTarget);
        LivingEntity current = currentEntity instanceof LivingEntity living ? living : null;
        if (isValidHeroesOfGwenPumpkinTarget(current)) return current;
        Location home = heroesOfGwenPumpkinKing.getLocation();
        return world.getEntities().stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(this::isValidHeroesOfGwenPumpkinTarget)
                .min(java.util.Comparator.comparingDouble(candidate ->
                        candidate.getLocation().distanceSquared(home)))
                .orElse(null);
    }

    private void moveHeroesOfGwenPumpkinKing(Location goal, double sourceSpeed) {
        WitherSkeleton king = heroesOfGwenPumpkinKing;
        if (king == null || goal == null) return;
        Location current = king.getLocation();
        Vector direction = goal.toVector().subtract(current.toVector());
        if (direction.lengthSquared() < 4.0D) return;
        current.setDirection(direction);
        current.add(direction.normalize().multiply(ArcadePortRules.heroesOfGwenWitherStepPerTick(sourceSpeed)));
        king.teleport(current);
    }

    private void updateHeroesOfGwenPumpkinKing(long now) {
        WitherSkeleton king = heroesOfGwenPumpkinKing;
        if (king == null || !king.isValid() || king.isDead()) return;
        LivingEntity target = findHeroesOfGwenPumpkinTarget();
        heroesOfGwenPumpkinKingTarget = target == null ? null : target.getUniqueId();
        moveHeroesOfGwenPumpkinKing(target == null ? map.dataLocations("BLACK").get(0) : target.getLocation(),
                target == null ? ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_HOME_SPEED
                        : ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_TARGET_SPEED);
        king.setCustomName(ChatColor.DARK_RED + "Pumpkin King " + ChatColor.RED + "❤ "
                + (int) Math.ceil(king.getHealth()));

        if (now < heroesOfGwenPumpkinKingNextAttackAt) return;
        heroesOfGwenPumpkinKingNextAttackAt = now
                + ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_ATTACK_INTERVAL_MILLIS;
        if (target == null) {
            king.setHealth(Math.min(king.getMaxHealth(),
                    king.getHealth() + ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_OUT_OF_COMBAT_HEAL));
            return;
        }
        if (king.getLocation().distanceSquared(target.getLocation())
                < ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_DIRECT_RANGE_SQUARED) {
            target.damage(ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_DIRECT_DAMAGE, king);
        }
        double radius = ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_AREA_RADIUS;
        for (Entity nearby : world.getNearbyEntities(king.getLocation(), radius, radius, radius)) {
            if (!(nearby instanceof LivingEntity victim) || victim.equals(king)
                    || !victim.isValid() || victim.isDead()
                    || victim.getLocation().distanceSquared(king.getLocation()) > radius * radius) continue;
            if (victim instanceof Player player && !activePlayers.contains(player.getUniqueId())) continue;
            victim.damage(ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_AREA_DAMAGE, king);
            Vector knockback = victim.getLocation().toVector().subtract(king.getLocation().toVector());
            if (knockback.lengthSquared() > 0.001D) {
                victim.setVelocity(knockback.normalize().setY(1.0D));
            }
            world.spawnParticle(Particle.FLAME, victim.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                    5, 0.35D, 0.35D, 0.35D, 0.04D);
        }
    }

    private void startHeroesOfGwenMonochromeSidequest(long now) {
        for (int team = 0; team < heroesOfGwenMonochromeKills.length; team++) {
            heroesOfGwenMonochromeKills[team] = 0;
        }
        int spawned = 0;
        for (Location location : map.dataLocations("BLACK")) {
            WitherSkeleton skeleton = world.spawn(location.clone(), WitherSkeleton.class);
            skeleton.setRemoveWhenFarAway(false);
            skeleton.setCanPickupItems(false);
            skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
            skeleton.getEquipment().setItemInMainHandDropChance(0.0F);
            skeleton.setCustomName(ChatColor.DARK_RED + "Wither Skeleton");
            skeleton.setCustomNameVisible(true);
            heroesOfGwenMonochromeSkeletons.add(skeleton.getUniqueId());
            entities.add(skeleton);
            spawned++;
        }
        if (spawned == 0) {
            heroesOfGwenSidequestNextStartAt = now
                    + ArcadePortRules.HEROES_OF_GWEN_SIDEQUEST_INITIAL_DELAY_MILLIS;
            return;
        }
        heroesOfGwenMonochromeEndsAt = now
                + ArcadePortRules.HEROES_OF_GWEN_MONOCHROME_SKELETONS_DURATION_MILLIS;
        heroesOfGwenSidequestNextStartAt = now
                + ArcadePortRules.HEROES_OF_GWEN_SIDEQUEST_INITIAL_DELAY_MILLIS;
        announce(ChatColor.RED + "Wither Skeletons have spawned! "
                + ChatColor.GRAY + "Your team has 30 seconds to defeat more than the opposing team.");
        for (Player player : onlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8F, 0.5F);
        }
    }

    private void finishHeroesOfGwenMonochromeSidequest() {
        int winner = ArcadePortRules.heroesOfGwenMonochromeWinner(
                heroesOfGwenMonochromeKills[0], heroesOfGwenMonochromeKills[1]);
        for (UUID id : new HashSet<>(heroesOfGwenMonochromeSkeletons)) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) entity.remove();
            if (entity != null) entities.remove(entity);
        }
        heroesOfGwenMonochromeSkeletons.clear();
        heroesOfGwenMonochromeEndsAt = 0L;
        if (winner < 0) {
            announce(ChatColor.GOLD + "The Wither Skeleton challenge ended in a draw. No team received a buff.");
        } else {
            announce(ChatColor.GOLD + teamName(winner) + " defeated the most Wither Skeletons and earned the Pumpkin King's blessing!");
            grantHeroesOfGwenPumpkinKingBuff(winner);
        }
    }

    private void grantHeroesOfGwenPumpkinKingBuff(int team) {
        long expiresAt = System.currentTimeMillis()
                + ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_BUFF_DURATION_MILLIS;
        for (Player player : activeMatchPlayers()) {
            if (teams.getOrDefault(player.getUniqueId(), -1) != team) continue;
            heroesOfGwenPumpkinBuffEndsAt.put(player.getUniqueId(), expiresAt);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                    (int) (ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_BUFF_DURATION_MILLIS / 50L),
                    ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_REGENERATION_AMPLIFIER,
                    false, true, true));
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0F, 0.5F);
            world.spawnParticle(Particle.LAVA, player.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                    10, 0.5D, 0.5D, 0.5D, 0.05D);
            player.sendMessage(ChatColor.GOLD + "Pumpkin King's blessing: "
                    + ChatColor.GREEN + "Regeneration II and 50% increased damage for 60 seconds.");
        }
    }

    void tick(long now) {
        if (stopped || !ready) return;
        if (game == ArcadePortGame.CASTLE_ASSAULT) tickCastleAssault(now);
        else if (game == ArcadePortGame.HEROES_OF_GWEN) tickHeroesOfGwen(now);
    }

    private void tickCastleAssault(long now) {
        refillCastleArcherArrows(now);
        if (objectiveItem != null && objectiveItem.isValid() && activeObjectiveSpawn != null) {
            objectiveItem.teleport(activeObjectiveSpawn);
            objectiveItem.setVelocity(new Vector());
        }
        if (objectiveItem == null && objectiveCarrier == null && now >= objectiveRespawnAt) {
            spawnObjectiveTnt();
        }
        if (objectiveCarrier != null) {
            Player carrier = players.get(objectiveCarrier);
            if (carrier == null || !carrier.isOnline()) {
                objectiveCarrier = null;
                objectiveCarryStartedAt = 0L;
            } else if (now - objectiveCarryStartedAt >= CASTLE_NORMAL_TNT_DELAY
                    || isAtEnemyCrystal(carrier)) {
                detonateObjectiveTnt(carrier);
            }
        }
        if (now >= nextChestRefillAt) {
            for (Block chest : castleChests) fillCastleChest(chest);
            nextChestRefillAt = now + 120_000L;
            announce(ChatColor.GREEN + "Castle Assault chests have refilled!");
        }
        for (KingState king : kings.values()) {
            if (king.entity != null && king.entity.isValid()) {
                king.entity.teleport(king.location);
                king.entity.setCustomName(teamColour(king.team) + king.name + ChatColor.RED
                        + " ❤ " + Math.max(0, king.health));
            }
        }
        if (now >= nextKingCageUpdateAt) {
            for (KingState king : kings.values()) updateKingCage(king, king.activeCrystals() > 0);
            nextKingCageUpdateAt = now + 1_000L;
        }
        // Reborn keeps the objective item at its pad and recreates it if another mechanic removes it.
        if (objectiveItem != null && (!objectiveItem.isValid() || objectiveItem.isDead())
                && objectiveCarrier == null && activeObjectiveSpawn != null) {
            spawnObjectiveItem(activeObjectiveSpawn);
        }
        checkTeamDeparture();
    }

    private void spawnObjectiveItem(Location spawn) {
        if (objectiveItem != null && objectiveItem.isValid()) objectiveItem.remove();
        objectiveItem = world.dropItem(spawn.clone(), new ItemStack(Material.TNT));
        objectiveItem.setPickupDelay(0);
        objectiveItem.setGravity(false);
        objectiveItem.setVelocity(new Vector());
        objectiveItem.setUnlimitedLifetime(true);
        objectiveItem.setCustomName(ChatColor.RED + "Objective TNT");
        objectiveItem.setCustomNameVisible(true);
        objectiveItem.getPersistentDataContainer().set(objectiveTntKey, PersistentDataType.BYTE, (byte) 1);
    }

    /** Match Reborn's 9x9x5 iron cage around each King while at least one crystal survives. */
    private void updateKingCage(KingState king, boolean beaconsAlive) {
        for (int y = 0; y <= 4; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    Block block = king.location.clone().add(x, y, z).getBlock();
                    Material type = block.getType();
                    if (!beaconsAlive) {
                        if (type != Material.AIR) {
                            rememberBlock(block);
                            block.setType(Material.AIR, false);
                        }
                        continue;
                    }
                    if (y == 4) {
                        if (type != Material.IRON_BLOCK) {
                            rememberBlock(block);
                            block.setType(Material.IRON_BLOCK, false);
                        }
                    } else if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                        if (type != Material.IRON_BARS) {
                            rememberBlock(block);
                            block.setType(Material.IRON_BARS, false);
                        }
                    } else if (type != Material.AIR && type != Material.IRON_BLOCK && type != Material.IRON_BARS) {
                        rememberBlock(block);
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void tickHeroesOfGwen(long now) {
        updateMobaWithers(now);
        updateHeroesOfGwenSidequest(now);
        updateHeroesOfGwenAbilities(now);
        updateAnathMeteorZones(now);
        updateHeroesOfGwenMinions(now);
        recordMobaTowerEntries(now);
        if (now >= nextGoldAt) {
            for (Player player : activeMatchPlayers()) addGold(player, MOBA_BASE_GOLD, "Passive gold");
            for (CapturePointState point : capturePoints) {
                if (point.owner < 0) continue;
                for (Player player : activeMatchPlayers()) {
                    if (teams.getOrDefault(player.getUniqueId(), -1) == point.owner) {
                        addGold(player, MOBA_CAPTURE_GOLD, "Captured beacon");
                    }
                }
            }
            nextGoldAt = now + MOBA_PASSIVE_GOLD_INTERVAL;
        }
        if (now >= nextCapturePointUpdateAt) {
            updateCapturePoints();
            nextCapturePointUpdateAt = now + MOBA_CAPTURE_UPDATE_INTERVAL;
        }
        if (now >= nextStructureUpdateAt) {
            updateMobaTowers();
            nextStructureUpdateAt = now + MOBA_TOWER_UPDATE_INTERVAL;
        }
        checkTeamDeparture();
    }

    private boolean isAtEnemyCrystal(Player carrier) {
        int team = teams.getOrDefault(carrier.getUniqueId(), -1);
        KingState enemy = kings.get(1 - team);
        if (team < 0 || enemy == null) return false;
        return enemy.crystals.stream().anyMatch(crystal -> crystal != null && crystal.isValid()
                && crystal.getLocation().distanceSquared(carrier.getLocation()) <= 9.0D);
    }

    private void fillCastleChest(Block block) {
        if (block == null || (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST)) return;
        if (!(block.getState() instanceof Chest chest)) return;
        Inventory inventory = chest.getBlockInventory();
        inventory.clear();
        Set<Integer> slots = new HashSet<>();
        while (slots.size() < 5) slots.add(ThreadLocalRandom.current().nextInt(inventory.getSize()));
        for (int slot : slots) {
            double chance = ThreadLocalRandom.current().nextDouble();
            double subChance = ThreadLocalRandom.current().nextDouble();
            ItemStack loot;
            // Match CastleAssault.fillChest() thresholds: ranged 30%, potions 20%, rods/gaps 10%, misc 40%.
            if (chance <= 0.30D) {
                loot = ThreadLocalRandom.current().nextBoolean()
                        ? new ItemStack(Material.EGG, ThreadLocalRandom.current().nextInt(3, 6))
                        : new ItemStack(Material.ARROW, ThreadLocalRandom.current().nextInt(8, 17));
            } else if (chance <= 0.50D) {
                if (subChance <= 0.45D) {
                    loot = castlePotion(Material.POTION, PotionEffectType.REGENERATION, 900, 0,
                            "Regeneration Potion");
                } else if (ThreadLocalRandom.current().nextBoolean()) {
                    loot = castlePotion(Material.POTION, PotionEffectType.SPEED, 3600, 0, "Speed Potion");
                } else {
                    loot = castlePotion(Material.SPLASH_POTION, PotionEffectType.REGENERATION, 22 * 20, 1,
                            "Splash Regeneration II Potion");
                }
            } else if (chance <= 0.60D) {
                loot = ThreadLocalRandom.current().nextBoolean()
                        ? castleUnbreakableRod()
                        : namedItem(Material.GOLDEN_APPLE, ChatColor.LIGHT_PURPLE + "Golden Applegate",
                                "Castle Assault chest reward.");
            } else {
                int weighted = ThreadLocalRandom.current().nextInt(9);
                loot = weighted < 2 ? new ItemStack(Material.ENDER_PEARL)
                        : weighted < 4 ? new ItemStack(Material.WATER_BUCKET)
                        : weighted < 6 ? castleFlintAndSteel(4, false)
                        : new ItemStack(Material.SNOWBALL, 16);
            }
            inventory.setItem(slot, loot);
        }
        chest.update(true, false);
    }

    private static ItemStack namedItem(Material material, String name, String lore, int amount) {
        ItemStack item = namedItem(material, name, lore);
        item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), amount)));
        return item;
    }

    private ItemStack castlePotion(Material material, PotionEffectType effect, int durationTicks,
            int amplifier, String displayName) {
        ItemStack item = new ItemStack(material);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(effect, durationTicks, amplifier), true);
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + displayName);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack castleUnbreakableRod() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isCastleKitItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        return meta.getLore().stream().map(ChatColor::stripColor).anyMatch(lore ->
                lore != null && ("Kit Item".equalsIgnoreCase(lore.trim())
                        || "Castle Assault kit item".equalsIgnoreCase(lore.trim())));
    }

    private ItemStack castleFlintAndSteel(int remainingUses, boolean kitItem) {
        ItemStack item = new ItemStack(Material.FLINT_AND_STEEL);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int maximum = Material.FLINT_AND_STEEL.getMaxDurability();
            damageable.setDamage(Math.max(0, maximum - Math.max(1, Math.min(maximum, remainingUses))));
        }
        meta.setDisplayName(ChatColor.GOLD + "Flint and Steel");
        meta.setLore(List.of(ChatColor.GRAY + (kitItem ? "Kit Item" : "Limited-use chest item")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack castleEnchantedBook(Enchantment enchantment, int level, String name) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack castleArmour(Material material, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (enchantment != null && level > 0) meta.addEnchant(enchantment, level, true);
        meta.setUnbreakable(true);
        meta.setLore(List.of(ChatColor.GRAY + "Castle Assault kit item"));
        item.setItemMeta(meta);
        return item;
    }

    private void spawnObjectiveTnt() {
        if (objectiveTntSpawns.isEmpty()) return;
        Location spawn = objectiveTntSpawns.get(ThreadLocalRandom.current().nextInt(objectiveTntSpawns.size())).clone();
        Block platform = spawn.getBlock().getRelative(BlockFace.DOWN);
        rememberBlock(platform);
        platform.setType(Material.REDSTONE_BLOCK, false);
        activeObjectiveSpawn = spawn.clone();
        activeObjectiveSpawn = spawn.clone();
        spawnObjectiveItem(spawn);
        world.spawnParticle(Particle.FIREWORK, spawn, 24, 0.3D, 0.3D, 0.3D, 0.03D);
        world.playSound(spawn, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 0.8F);
        objectiveKillsEnabled = 1;
        announce(ChatColor.GOLD + "Objective TNT has spawned! Carry it to an enemy sentry crystal.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onObjectivePickup(EntityPickupItemEvent event) {
        if (stopped || game != ArcadePortGame.CASTLE_ASSAULT || objectiveItem == null
                || !event.getItem().getUniqueId().equals(objectiveItem.getUniqueId())
                || !(event.getEntity() instanceof Player player) || !players.containsKey(player.getUniqueId())) return;
        if (objectiveCarrier != null) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        objectiveCarrier = player.getUniqueId();
        objectiveCarryStartedAt = System.currentTimeMillis();
        objectiveRespawnAt = objectiveCarryStartedAt + objectiveDelay();
        ItemStack helmet = player.getInventory().getHelmet();
        carriedTntOldHelmet.put(player.getUniqueId(), helmet == null ? null : helmet.clone());
        player.getInventory().setHelmet(namedItem(Material.TNT, ChatColor.RED + "Carrying Objective TNT",
                "Right-click to detonate at an enemy objective."));
        objectiveItem.remove();
        objectiveItem = null;
        if (activeObjectiveSpawn != null) {
            Block block = activeObjectiveSpawn.getBlock().getRelative(BlockFace.DOWN);
            if (block.getType() == Material.REDSTONE_BLOCK) {
                rememberBlock(block);
                block.setType(Material.IRON_BLOCK, false);
            }
            activeObjectiveSpawn = null;
        }
        announce(ChatColor.GOLD + player.getName() + " picked up Objective TNT! Get to the enemy sentries.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCastleKitTntDrop(PlayerDropItemEvent event) {
        if (stopped || game != ArcadePortGame.CASTLE_ASSAULT) return;
        Player player = event.getPlayer();
        String kit = castleAssaultPlayerKits.get(player.getUniqueId());
        ItemStack dropped = event.getItemDrop().getItemStack();
        boolean lockedTnt = "demolitionist".equals(kit) && dropped.getType() == Material.TNT;
        boolean lockedArrow = "archer".equals(kit) && isCastleFletchedArrow(dropped);
        boolean lockedKitItem = kit != null && isCastleKitItem(dropped);
        if (!lockedTnt && !lockedArrow && !lockedKitItem) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + (lockedTnt ? "You cannot drop your Throwing TNT."
                : lockedArrow ? "You cannot drop your Fletched Arrows." : "You cannot drop a kit item."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCastleKitInventoryClick(InventoryClickEvent event) {
        if (stopped || game != ArcadePortGame.CASTLE_ASSAULT
                || !(event.getWhoClicked() instanceof Player player)) return;
        String kit = castleAssaultPlayerKits.get(player.getUniqueId());
        boolean demolitionist = "demolitionist".equals(kit);
        boolean archer = "archer".equals(kit);
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        ItemStack hotbarItem = event.getHotbarButton() >= 0
                ? player.getInventory().getItem(event.getHotbarButton()) : null;
        boolean kitItemInChest = event.getView().getTopInventory().getType()
                == org.bukkit.event.inventory.InventoryType.CHEST
                && (isCastleKitItem(current) || isCastleKitItem(cursor) || isCastleKitItem(hotbarItem));
        if (kitItemInChest) {
            event.setCancelled(true);
            return;
        }
        if (!demolitionist && !archer) return;
        boolean movesLockedItem = demolitionist
                && ((current != null && current.getType() == Material.TNT)
                    || (cursor != null && cursor.getType() == Material.TNT));
        movesLockedItem |= archer && (isCastleFletchedArrow(current) || isCastleFletchedArrow(cursor));
        if (!movesLockedItem && hotbarItem != null) {
            movesLockedItem = demolitionist && hotbarItem.getType() == Material.TNT
                    || archer && isCastleFletchedArrow(hotbarItem);
        }
        if (movesLockedItem) event.setCancelled(true);
    }

    private boolean isCastleFletchedArrow(ItemStack item) {
        if (item == null || item.getType() != Material.ARROW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Fletched Arrow");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCastlePlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        if (game == ArcadePortGame.CASTLE_ASSAULT) {
            castleCreditedVictims.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCastleKitDeath(PlayerDeathEvent event) {
        if (stopped || game != ArcadePortGame.CASTLE_ASSAULT) return;
        Player player = event.getEntity();
        Player killer = player.getKiller();
        if (killer != null) recordCastleKill(killer, player, false);
        String kit = castleAssaultPlayerKits.get(player.getUniqueId());
        if ("demolitionist".equals(kit)) {
            event.getDrops().removeIf(item -> item != null && item.getType() == Material.TNT);
        } else if ("archer".equals(kit)) {
            event.getDrops().removeIf(this::isCastleFletchedArrow);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (stopped || event.getHand() == null || event.getHand().name().equals("OFF_HAND")) return;
        Player player = event.getPlayer();
        if (!players.containsKey(player.getUniqueId())) return;
        if (game == ArcadePortGame.CASTLE_ASSAULT) {
            if (event.getItem() != null && event.getItem().getType() == Material.FISHING_ROD) {
                castleRodUsed.add(player.getUniqueId());
            }
            if (event.getAction().isRightClick() && player.getUniqueId().equals(objectiveCarrier)) {
                event.setCancelled(true);
                detonateObjectiveTnt(player);
                return;
            }
            if (event.getAction().isLeftClick() && event.getItem() != null
                    && event.getItem().getType() == Material.TNT) {
                event.setCancelled(true);
                throwPlayerTnt(player);
            }
        } else if (game == ArcadePortGame.HEROES_OF_GWEN) {
            if ("anath".equals(selectedHeroesOfGwen.get(player.getUniqueId()))
                    && player.getInventory().getHeldItemSlot() == 0
                    && (event.getAction().isLeftClick() || event.getAction().isRightClick())
                    && event.getItem() != null && event.getItem().getType() == Material.BLAZE_ROD) {
                event.setCancelled(true);
                fireAnathFlameWand(player);
                return;
            }
            if (isRowenaBombardmentItem(event.getItem())
                    && rowenaBombardments.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
                fireRowenaBombardmentShot(player);
                return;
            }
            if ("larissa".equals(selectedHeroesOfGwen.get(player.getUniqueId()))
                    && player.getInventory().getHeldItemSlot() == 0
                    && (event.getAction().isLeftClick() || event.getAction().isRightClick())
                    && event.getItem() != null && event.getItem().getType() == Material.DIAMOND_HOE) {
                event.setCancelled(true);
                fireLarissaAquaCannon(player);
                return;
            }
            if (event.getItem() != null && heroSkillId(event.getItem()) != null) {
                event.setCancelled(true);
                activateHeroesOfGwenSkill(player, heroSkillId(event.getItem()));
                return;
            }
            if (event.getAction().isRightClick() && event.getItem() != null
                    && event.getItem().getType() == Material.GOLD_INGOT) {
                event.setCancelled(true);
                openStore(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenSkillDrop(PlayerDropItemEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();
        String id = heroSkillId(dropped);
        if (id == null || !Set.of("hattori.ender_blade", "devon.infinity",
                "anath.burn_beam", "anath.meteor_shower", "larissa.storm_blessing",
                "bardolf.full_moon", "rowena.bombardment", "dana.rally", "biff.cavalry_charge",
                "ivy.thorn_wall").contains(id)) return;
        event.setCancelled(true);
        activateHeroesOfGwenSkill(player, id);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenHattoriDoubleJump(PlayerToggleFlightEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!activePlayers.contains(id) || !"hattori".equals(selectedHeroesOfGwen.get(id))
                || player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);
        if (!isHeroSkillReady(player, "hattori.double_jump")) return;
        setHeroSkillCooldown(player, "hattori.double_jump",
                ArcadePortRules.HEROES_OF_GWEN_HATTORI_DOUBLE_JUMP_COOLDOWN_MILLIS);
        Vector velocity = player.getVelocity();
        player.setVelocity(new Vector(velocity.getX() * 0.2D,
                ArcadePortRules.HEROES_OF_GWEN_HATTORI_DOUBLE_JUMP_POWER,
                velocity.getZ() * 0.2D));
        world.spawnParticle(Particle.FLAME, player.getLocation().clone().add(0.0D, 0.8D, 0.0D),
                16, 0.35D, 0.2D, 0.35D, 0.04D);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8F, 1.4F);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenSneak(PlayerToggleSneakEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN || !event.isSneaking()) return;
        Player player = event.getPlayer();
        String hero = selectedHeroesOfGwen.get(player.getUniqueId());
        if ("devon".equals(hero)) {
            ItemStack skill = player.getInventory().getItem(2);
            if ("devon.hunters_boost".equals(heroSkillId(skill))) {
                activateHeroesOfGwenSkill(player, "devon.hunters_boost");
            }
        } else if ("larissa".equals(hero)) {
            ItemStack skill = player.getInventory().getItem(2);
            if ("larissa.water_dash".equals(heroSkillId(skill))) {
                activateHeroesOfGwenSkill(player, "larissa.water_dash");
            }
        } else if ("bardolf".equals(hero)) {
            ItemStack skill = player.getInventory().getItem(2);
            if ("bardolf.wolf_pounce".equals(heroSkillId(skill))) {
                activateHeroesOfGwenSkill(player, "bardolf.wolf_pounce");
            }
        } else if ("rowena".equals(hero)) {
            ItemStack skill = player.getInventory().getItem(2);
            if ("rowena.combat_slide".equals(heroSkillId(skill))) {
                activateHeroesOfGwenSkill(player, "rowena.combat_slide");
            }
        } else if ("dana".equals(hero)) {
            ItemStack skill = player.getInventory().getItem(2);
            if ("dana.knock_up".equals(heroSkillId(skill))) {
                activateHeroesOfGwenSkill(player, "dana.knock_up");
            }
        } else if ("biff".equals(hero)) {
            ItemStack skill = player.getInventory().getItem(2);
            if ("biff.battle_leap".equals(heroSkillId(skill))) {
                activateHeroesOfGwenSkill(player, "biff.battle_leap");
            }
        } else if ("ivy".equals(hero)) {
            ItemStack skill = player.getInventory().getItem(2);
            if ("ivy.floral_leap".equals(heroSkillId(skill))) {
                activateHeroesOfGwenSkill(player, "ivy.floral_leap");
            }
        } else if ("anath".equals(hero)) {
            ItemStack skill = player.getInventory().getItem(2);
            if ("anath.flame_dash".equals(heroSkillId(skill))) {
                activateHeroesOfGwenSkill(player, "anath.flame_dash");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenBowShoot(EntityShootBowEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN
                || !(event.getEntity() instanceof Player player)) return;
        String hero = selectedHeroesOfGwen.get(player.getUniqueId());
        if ("rowena".equals(hero)) {
            int remaining = rowenaLightArrowsRemaining.getOrDefault(player.getUniqueId(), 0);
            if (remaining <= 0 || !(event.getProjectile() instanceof Arrow)) return;
            event.setCancelled(true);
            event.getProjectile().remove();
            int next = remaining - 1;
            if (next <= 0) {
                rowenaLightArrowsRemaining.remove(player.getUniqueId());
                setHeroSkillCooldown(player, "rowena.light_arrows",
                        ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROWS_COOLDOWN_MILLIS);
                giveHeroesOfGwenSkillItem(player, 1, "rowena.light_arrows", Material.GOLD_NUGGET,
                        "Light Arrows", "Infuse your next five arrows. They pass through blocks and deal high damage.");
            } else {
                rowenaLightArrowsRemaining.put(player.getUniqueId(), next);
                ItemStack skillItem = player.getInventory().getItem(1);
                if (skillItem != null && "rowena.light_arrows".equals(heroSkillId(skillItem))) {
                    skillItem.setAmount(next);
                }
            }
            fireRowenaLightArrow(player);
            return;
        }
        if (!"devon".equals(hero)) return;
        if (heroTntArrowCharges.getOrDefault(player.getUniqueId(), 0) > 0
                && event.getProjectile() instanceof Arrow arrow) {
            markHeroProjectile(arrow, player, "devon.tnt_arrow");
            int remaining = heroTntArrowCharges.get(player.getUniqueId()) - 1;
            if (remaining <= 0) {
                heroTntArrowCharges.remove(player.getUniqueId());
                setHeroSkillCooldown(player, "devon.tnt_infusion", 17_000L);
                giveHeroesOfGwenSkillItem(player, 1, "devon.tnt_infusion", Material.TNT,
                        "TNT Infusion", "Infuse your next three arrows with TNT.");
            } else {
                heroTntArrowCharges.put(player.getUniqueId(), remaining);
                ItemStack skill = player.getInventory().getItem(1);
                if (skill != null && heroSkillId(skill) != null) skill.setAmount(remaining);
            }
        }
        if (heroInfinityEndsAt.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()
                && event.getProjectile() instanceof Arrow arrow) {
            markHeroProjectile(arrow, player, "devon.infinity_arrow");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenRowenaBombardmentMove(PlayerMoveEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Player player = event.getPlayer();
        if (!rowenaBombardments.containsKey(player.getUniqueId()) || event.getTo() == null) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            event.setTo(new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(),
                    to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroesOfGwenRoleDamage(EntityDamageByEntityEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN
                || !(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.equals(victim)
                || teams.getOrDefault(attacker.getUniqueId(), -1) == teams.getOrDefault(victim.getUniqueId(), -2)) return;
        String attackerRole = ArcadePortRules.heroesOfGwenHeroRole(
                selectedHeroesOfGwen.getOrDefault(attacker.getUniqueId(), "hattori"));
        String victimRole = ArcadePortRules.heroesOfGwenHeroRole(
                selectedHeroesOfGwen.getOrDefault(victim.getUniqueId(), "hattori"));
        if (attackerRole == null || !attackerRole.equals(victimRole)) {
            heroesOfGwenCounterpartOnly.remove(attacker.getUniqueId());
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenBardolfDamage(EntityDamageByEntityEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        if (event.getEntity() instanceof Wolf wolf) {
            UUID ownerId = bardolfOwnerId(wolf);
            Player attacker = resolveAttacker(event);
            if (ownerId != null && attacker != null
                    && teams.getOrDefault(ownerId, -1) == teams.getOrDefault(attacker.getUniqueId(), -2)) {
                event.setCancelled(true);
                return;
            }
        }

        Player attacker = resolveAttacker(event);
        if (attacker == null || !"bardolf".equals(selectedHeroesOfGwen.get(attacker.getUniqueId()))
                || bardolfFullMoonEndsAt.getOrDefault(attacker.getUniqueId(), 0L) <= System.currentTimeMillis()
                || !(event.getEntity() instanceof LivingEntity)) return;
        int attackerTeam = teams.getOrDefault(attacker.getUniqueId(), -1);
        int victimTeam = heroesOfGwenEntityTeam(event.getEntity());
        if (victimTeam >= 0 && victimTeam == attackerTeam) return;
        int livingWolves = liveBardolfWolves(attacker.getUniqueId()).size();
        if (livingWolves > 0) {
            event.setDamage(event.getDamage()
                    + livingWolves * ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_DAMAGE_PER_WOLF);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroesOfGwenBardolfOwnerAttack(EntityDamageByEntityEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN
                || !(event.getEntity() instanceof LivingEntity target)) return;
        Player attacker = resolveAttacker(event);
        if (attacker == null || !activePlayers.contains(attacker.getUniqueId())
                || !"bardolf".equals(selectedHeroesOfGwen.get(attacker.getUniqueId()))) return;
        int ownerTeam = teams.getOrDefault(attacker.getUniqueId(), -1);
        int targetTeam = heroesOfGwenEntityTeam(target);
        if (targetTeam >= 0 && targetTeam == ownerTeam) return;
        for (Wolf wolf : liveBardolfWolves(attacker.getUniqueId())) {
            bardolfWolfTargets.put(wolf.getUniqueId(), target.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onHeroesOfGwenPumpkinKingDamage(EntityDamageEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN || heroesOfGwenPumpkinKing == null
                || !event.getEntity().getUniqueId().equals(heroesOfGwenPumpkinKing.getUniqueId())) return;
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            event.setCancelled(true);
            return;
        }
        Player attacker = resolveAttacker(byEntity);
        if (attacker == null || !activePlayers.contains(attacker.getUniqueId())
                || !isInsideHeroesOfGwenPumpkinBoundary(attacker.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHeroesOfGwenPumpkinKingBuffDamage(EntityDamageByEntityEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Player attacker = resolveAttacker(event);
        if (attacker == null || !activePlayers.contains(attacker.getUniqueId())) return;
        long expiresAt = heroesOfGwenPumpkinBuffEndsAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (expiresAt > System.currentTimeMillis()) {
            event.setDamage(event.getDamage() * ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_DAMAGE_MULTIPLIER);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHeroesOfGwenSidequestCombust(EntityCombustEvent event) {
        if (game != ArcadePortGame.HEROES_OF_GWEN) return;
        if (heroesOfGwenPumpkinKing != null
                && event.getEntity().getUniqueId().equals(heroesOfGwenPumpkinKing.getUniqueId())) {
            event.setCancelled(true);
        } else if (heroesOfGwenMonochromeSkeletons.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeroesOfGwenSidequestDeath(EntityDeathEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        UUID id = event.getEntity().getUniqueId();
        if (heroesOfGwenPumpkinKing != null && heroesOfGwenPumpkinKing.getUniqueId().equals(id)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            heroesOfGwenPumpkinKing = null;
            heroesOfGwenPumpkinKingTarget = null;
            heroesOfGwenPumpkinKingNextAttackAt = 0L;
            heroesOfGwenPumpkinKingRespawnAt = System.currentTimeMillis()
                    + ArcadePortRules.HEROES_OF_GWEN_PUMPKIN_KING_RESPAWN_DELAY_MILLIS;
            entities.remove(event.getEntity());
            Player killer = event.getEntity().getKiller();
            int team = killer != null && activePlayers.contains(killer.getUniqueId())
                    ? teams.getOrDefault(killer.getUniqueId(), -1) : -1;
            if (team >= 0) {
                announce(teamName(team) + " defeated the Pumpkin King and earned his blessing!");
                grantHeroesOfGwenPumpkinKingBuff(team);
            } else {
                announce(ChatColor.DARK_RED + "The Pumpkin King was defeated, but no team earned the blessing.");
            }
            return;
        }
        if (heroesOfGwenMonochromeSkeletons.remove(id)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            entities.remove(event.getEntity());
            Player killer = event.getEntity().getKiller();
            if (killer != null && activePlayers.contains(killer.getUniqueId())) {
                int team = teams.getOrDefault(killer.getUniqueId(), -1);
                if (team >= 0 && team < heroesOfGwenMonochromeKills.length) {
                    heroesOfGwenMonochromeKills[team]++;
                    killer.sendMessage(ChatColor.GOLD + "Wither Skeleton defeated! "
                            + ChatColor.GRAY + "Your team: " + heroesOfGwenMonochromeKills[team]);
                }
            }
        }
    }

    @EventHandler
    public void onHeroesOfGwenBardolfPlayerDeath(PlayerDeathEvent event) {
        if (game == ArcadePortGame.HEROES_OF_GWEN) {
            removeBiffState(event.getEntity().getUniqueId());
            removeBardolfPack(event.getEntity().getUniqueId());
            removeRowenaState(event.getEntity().getUniqueId());
            removeDanaState(event.getEntity().getUniqueId());
            removeIvyState(event.getEntity().getUniqueId());
            removeAnathState(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onHeroesOfGwenBardolfPlayerQuit(PlayerQuitEvent event) {
        if (game == ArcadePortGame.HEROES_OF_GWEN) {
            removeBiffState(event.getPlayer().getUniqueId());
            removeBardolfPack(event.getPlayer().getUniqueId());
            removeRowenaState(event.getPlayer().getUniqueId());
            removeDanaState(event.getPlayer().getUniqueId());
            removeIvyState(event.getPlayer().getUniqueId());
            removeAnathState(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenBiffHorseDamage(EntityDamageEvent event) {
        if (game != ArcadePortGame.HEROES_OF_GWEN || !(event.getEntity() instanceof Horse horse)) return;
        if (horse.getPersistentDataContainer().has(biffHorseOwnerKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeroesOfGwenBiffHorseDeath(EntityDeathEvent event) {
        if (game != ArcadePortGame.HEROES_OF_GWEN || !(event.getEntity() instanceof Horse horse)) return;
        if (!horse.getPersistentDataContainer().has(biffHorseOwnerKey, PersistentDataType.STRING)) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        String owner = horse.getPersistentDataContainer().get(biffHorseOwnerKey, PersistentDataType.STRING);
        if (owner != null) {
            try { removeBiffHorse(UUID.fromString(owner)); }
            catch (IllegalArgumentException ignored) { entities.remove(horse); }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeroesOfGwenBardolfWolfDeath(EntityDeathEvent event) {
        if (game != ArcadePortGame.HEROES_OF_GWEN || !(event.getEntity() instanceof Wolf wolf)) return;
        UUID ownerId = bardolfOwnerId(wolf);
        if (ownerId == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        List<Wolf> pack = bardolfWolves.get(ownerId);
        if (pack != null) pack.removeIf(existing -> existing == null || existing.getUniqueId().equals(wolf.getUniqueId()));
        bardolfWolfTargets.remove(wolf.getUniqueId());
        entities.remove(wolf);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenAnathEmberPickup(EntityPickupItemEvent event) {
        if (game == ArcadePortGame.HEROES_OF_GWEN
                && anathFlameEmbers.containsKey(event.getItem().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenProjectileDamage(EntityDamageByEntityEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Entity damager = event.getDamager();
        if (damager instanceof Snowball fountain && "larissa.dancing_fountain".equals(
                fountain.getPersistentDataContainer().get(heroProjectileTypeKey, PersistentDataType.STRING))) {
            event.setCancelled(true);
            return;
        }
        if (damager instanceof SmallFireball meteorFireball) {
            String type = meteorFireball.getPersistentDataContainer()
                    .get(heroProjectileTypeKey, PersistentDataType.STRING);
            if (!"anath.meteor_fireball".equals(type)) return;
            Player owner = heroProjectileOwner(meteorFireball);
            if (owner != null && event.getEntity() instanceof Player target
                    && !target.equals(owner)
                    && teams.getOrDefault(owner.getUniqueId(), -1) != teams.getOrDefault(target.getUniqueId(), -2)
                    && activePlayers.contains(target.getUniqueId())) {
                event.setDamage(ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_FIREBALL_DAMAGE);
                target.setFireTicks(Math.max(target.getFireTicks(), 40));
            } else event.setCancelled(true);
            return;
        }
        if (damager instanceof Snowball snowball) {
            String type = snowball.getPersistentDataContainer().get(heroProjectileTypeKey, PersistentDataType.STRING);
            if (!"hattori.shuriken".equals(type)) return;
            Player owner = heroProjectileOwner(snowball);
            if (owner == null || !(event.getEntity() instanceof Player target)
                    || teams.getOrDefault(owner.getUniqueId(), -1) == teams.getOrDefault(target.getUniqueId(), -2)) return;
            event.setDamage(3.0D);
            return;
        }
        if (damager instanceof Arrow arrow) {
            String type = arrow.getPersistentDataContainer().get(heroProjectileTypeKey, PersistentDataType.STRING);
            if (!"devon.infinity_arrow".equals(type)) return;
            Player owner = heroProjectileOwner(arrow);
            if (owner != null && event.getEntity() instanceof Player target
                    && teams.getOrDefault(owner.getUniqueId(), -1) != teams.getOrDefault(target.getUniqueId(), -2)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 3 * 20, 0, true, true, true));
            }
            return;
        }
        if (!(damager instanceof Player attacker)) return;
        if (heroEnderBladeEndsAt.getOrDefault(attacker.getUniqueId(), 0L) <= System.currentTimeMillis()) return;
        if (teams.getOrDefault(attacker.getUniqueId(), -1) == teams.getOrDefault(event.getEntity().getUniqueId(), -2)) return;
        ItemStack held = attacker.getInventory().getItemInMainHand();
        if (held != null && held.getType() == Material.DIAMOND_SWORD
                && held.hasItemMeta() && held.getItemMeta().hasDisplayName()
                && held.getItemMeta().getDisplayName().contains("ENDER BLADE")) {
            event.setDamage(event.getDamage()
                    + ArcadePortRules.HEROES_OF_GWEN_HATTORI_ENDER_BLADE_DAMAGE_BONUS);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroesOfGwenProjectileHit(ProjectileHitEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Projectile projectile = event.getEntity();
        String type = projectile.getPersistentDataContainer().get(heroProjectileTypeKey, PersistentDataType.STRING);
        if ("larissa.dancing_fountain".equals(type)) {
            Player owner = heroProjectileOwner(projectile);
            if (owner != null && activePlayers.contains(owner.getUniqueId())) {
                startLarissaFountain(owner, projectile.getLocation().clone(), System.currentTimeMillis());
            }
            entities.remove(projectile);
            projectile.remove();
            return;
        }
        if ("anath.meteor".equals(type)) {
            Player owner = heroProjectileOwner(projectile);
            if (owner != null && activePlayers.contains(owner.getUniqueId())) {
                Location center = projectile.getLocation().clone();
                startAnathMeteorZone(owner, center, System.currentTimeMillis());
            }
            projectile.remove();
            return;
        }
        if ("anath.meteor_fireball".equals(type)) {
            projectile.remove();
            return;
        }

        if (!"devon.tnt_arrow".equals(type)) return;
        Player owner = heroProjectileOwner(projectile);
        if (owner == null) return;
        Location center = projectile.getLocation().clone();
        world.spawnParticle(Particle.EXPLOSION, center, 3, 0.25D, 0.25D, 0.25D, 0.02D);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.1F);
        ItemStack bow = owner.getInventory().getItem(0);
        int power = bow == null ? 0 : bow.getEnchantmentLevel(Enchantment.POWER);
        double damageAtCenter = ArcadePortRules.heroesOfGwenDevonTntArrowDamage(power);
        int ownerTeam = teams.getOrDefault(owner.getUniqueId(), -1);
        double radius = ArcadePortRules.HEROES_OF_GWEN_DEVON_TNT_EXPLOSION_RADIUS;
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || target.equals(owner)
                    || !target.isValid() || target.isDead()) continue;
            if (target instanceof Player victim && !activePlayers.contains(victim.getUniqueId())) continue;
            double distance = center.distance(target.getLocation());
            if (distance >= radius) continue;
            int targetTeam = heroesOfGwenEntityTeam(target);
            if (targetTeam >= 0 && targetTeam == ownerTeam) continue;
            double falloff = Math.max(0.0D, 1.0D - distance / radius);
            double damage = falloff * damageAtCenter;
            TowerState tower = towerFor(target);
            if (tower != null) {
                if (tower.owner != ownerTeam && canDamageMobaTower(tower, ownerTeam)
                        && owner.getLocation().distanceSquared(tower.location) <= MOBA_TOWER_RANGE_SQUARED) {
                    damageMobaTower(tower, damage, owner);
                }
            } else if (target instanceof Wither wither) {
                Integer enemyTeam = withers.entrySet().stream()
                        .filter(witherEntry -> witherEntry.getValue().equals(wither))
                        .map(Map.Entry::getKey).findFirst().orElse(null);
                if (enemyTeam != null && enemyTeam != ownerTeam
                        && witherDamageable.getOrDefault(enemyTeam, false)) {
                    if (wither.getHealth() <= damage) {
                        finishHeroesOfGwen(ownerTeam, "enemy Wither defeated by Devon's TNT arrow");
                        projectile.remove();
                        return;
                    }
                    wither.setHealth(wither.getHealth() - damage);
                }
            } else {
                target.damage(damage, owner);
            }
            Vector knockback = target.getLocation().toVector().subtract(center.toVector());
            if (knockback.lengthSquared() > 0.001D) {
                target.setVelocity(knockback.normalize().multiply(0.8D * falloff).setY(0.25D));
            }
        }
        projectile.remove();
    }

    private void detonateObjectiveTnt(Player carrier) {
        restoreCarrierHelmet(carrier);
        objectiveCarrier = null;
        objectiveCarryStartedAt = 0L;
        resolveCastleExplosion(carrier.getLocation().clone(), carrier, true, castleTntRadius(carrier));
        carrier.sendMessage(ChatColor.GOLD + "Objective TNT detonated!");
    }

    private void throwPlayerTnt(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.TNT) return;
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
        Location spawn = player.getEyeLocation().add(player.getLocation().getDirection());
        TNTPrimed tnt = world.spawn(spawn, TNTPrimed.class);
        tnt.setFuseTicks(60);
        tnt.setYield(0.0F);
        tnt.setVelocity(player.getLocation().getDirection().normalize().multiply(0.5D).add(new Vector(0, 0.1D, 0)));
        tnt.getPersistentDataContainer().set(thrownTntOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        entities.add(tnt);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onThrownTntExplode(EntityExplodeEvent event) {
        if (stopped || game != ArcadePortGame.CASTLE_ASSAULT
                || !(event.getEntity() instanceof TNTPrimed tnt)) return;
        String ownerValue = tnt.getPersistentDataContainer().get(thrownTntOwnerKey, PersistentDataType.STRING);
        if (ownerValue == null) return;
        event.setCancelled(true);
        event.blockList().clear();
        Player owner = null;
        try {
            owner = Bukkit.getPlayer(UUID.fromString(ownerValue));
        } catch (IllegalArgumentException ignored) {
        }
        if (owner != null && owner.isOnline()) resolveCastleExplosion(tnt.getLocation(), owner, false, castleTntRadius(owner));
        entities.remove(tnt);
        tnt.remove();
    }

    private double castleTntRadius(Player owner) {
        return 4.0D + ("demolitionist".equals(castleAssaultPlayerKits.get(owner.getUniqueId())) ? 3.0D : 0.0D);
    }

    private void refillCastleArcherArrows(long now) {
        refillCastleDemolitionistTnt(now);
        for (UUID id : new ArrayList<>(activePlayers)) {
            if (!"archer".equals(castleAssaultPlayerKits.get(id))) continue;
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            long next = castleArcherNextArrowAt.getOrDefault(id, now);
            if (now < next) continue;
            int amount = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (isCastleFletchedArrow(item)) amount += item.getAmount();
            }
            if (amount < ArcadePortRules.CASTLE_ASSAULT_ARCHER_MAX_ARROWS) {
                player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrow",
                        "Replenished by the Archer kit."));
                player.updateInventory();
            }
            castleArcherNextArrowAt.put(id, now + ArcadePortRules.CASTLE_ASSAULT_ARCHER_REFILL_MILLIS);
        }
    }

    private void refillCastleDemolitionistTnt(long now) {
        for (UUID id : new ArrayList<>(activePlayers)) {
            if (!"demolitionist".equals(castleAssaultPlayerKits.get(id))) continue;
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            long next = castleDemolitionistNextTntAt.getOrDefault(id, now);
            if (now < next) continue;
            int current = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.TNT) current += item.getAmount();
            }
            if (current < ArcadePortRules.CASTLE_ASSAULT_DEMOLITIONIST_MAX_TNT) {
                player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT",
                        "Replenished by the Demolitionist kit."));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
                player.updateInventory();
            }
            castleDemolitionistNextTntAt.put(id, now + ArcadePortRules.CASTLE_ASSAULT_DEMOLITIONIST_TNT_REFILL_MILLIS);
        }
    }

    private int castleBlastProtectionEpf(Player player) {
        int epf = 0;
        for (ItemStack armour : player.getInventory().getArmorContents()) {
            if (armour != null) epf += armour.getEnchantmentLevel(Enchantment.BLAST_PROTECTION) * 2;
        }
        return Math.min(20, epf);
    }

    private int castleHighestBlastProtection(Player player) {
        int highest = 0;
        for (ItemStack armour : player.getInventory().getArmorContents()) {
            if (armour != null) highest = Math.max(highest,
                    armour.getEnchantmentLevel(Enchantment.BLAST_PROTECTION));
        }
        return highest;
    }

    private void resolveCastleExplosion(Location center, Player owner, boolean objective, double radius) {
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.CLOUD, center, 30, 0.65D, 0.65D, 0.65D, 0.08D);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 0.9F);
        int attackingTeam = teams.getOrDefault(owner.getUniqueId(), -1);
        for (Player target : activeMatchPlayers()) {
            if (target.equals(owner) && objective) continue;
            if (!target.equals(owner) && teams.getOrDefault(target.getUniqueId(), -2) == attackingTeam) continue;
            double distance = target.getLocation().distance(center);
            if (distance > radius) continue;
            double damage = 10.0D * Math.max(0.0D, 1.0D - distance / radius);
            damage *= ArcadePortRules.castleAssaultBlastDamageMultiplier(castleBlastProtectionEpf(target));
            if (damage <= 0.0D) continue;
            if (target.getHealth() <= damage) {
                recordCastleKill(owner, target, objective);
                hooks.eliminate(target, owner, "was caught in a Castle Assault TNT blast");
            } else target.setHealth(target.getHealth() - damage);
            Vector direction = target.getLocation().toVector().subtract(center.toVector());
            if (direction.lengthSquared() > 0.001D) {
                double knockback = ArcadePortRules.castleAssaultBlastKnockbackMultiplier(
                        castleHighestBlastProtection(target));
                target.setVelocity(direction.normalize().multiply(0.8D * knockback)
                        .setY(0.35D * knockback));
            }
        }
        if (!objective) return;
        KingState enemyKing = kings.get(1 - attackingTeam);
        if (enemyKing == null) return;
        for (EnderCrystal crystal : new ArrayList<>(enemyKing.crystals)) {
            if (!crystal.isValid() || crystal.getLocation().distance(center) > radius) continue;
            destroyCastleCrystal(enemyKing, crystal, owner);
        }
        if (enemyKing.activeCrystals() == 0 && enemyKing.entity != null
                && enemyKing.location.distance(center) <= radius) damageCastleKing(enemyKing, 50, owner);
    }

    private void destroyCastleCrystal(KingState king, EnderCrystal crystal, Player attacker) {
        if (!king.crystals.remove(crystal)) return;
        entities.remove(crystal);
        crystal.remove();
        castleCrystalsDestroyedByPlayer.merge(attacker.getUniqueId(), 1, Integer::sum);
        attacker.sendMessage(ChatColor.GREEN + "Sentry crystal destroyed!");
        Block beaconBase = crystal.getLocation().getBlock().getRelative(0, -2, 0);
        rememberBlock(beaconBase);
        beaconBase.setType(Material.STONE_BRICKS, false);
        world.playSound(king.location, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0F, 0.8F);
        announce(ChatColor.GOLD + teamName(king.team) + " sentry crystal destroyed. "
                + (king.activeCrystals() == 0 ? "Their King is now vulnerable!" : king.activeCrystals() + " crystal(s) remain."));
    }

    private void damageCastleKing(KingState king, double damage, Player attacker) {
        if (king.activeCrystals() > 0 || king.health <= 0) return;
        king.health = Math.max(0, king.health - (int) Math.ceil(damage));
        if (king.entity != null && king.entity.isValid()) {
            king.entity.setCustomName(teamColour(king.team) + king.name + ChatColor.RED + " ❤ " + king.health);
            king.entity.playEffect(org.bukkit.EntityEffect.HURT);
        }
        if (king.health <= 0) {
            if (attacker != null && castleCrystalsDestroyedByPlayer.getOrDefault(attacker.getUniqueId(), 0) >= 2) {
                castleFinalKingHitByPlayer.add(attacker.getUniqueId());
            }
            int winningTeam = 1 - king.team;
            recordCastleAssaultResult(winningTeam);
            announce(ChatColor.GOLD + king.name + " has fallen. " + teamName(winningTeam) + " wins Castle Assault!");
            hooks.finish(firstPlayerOnTeam(winningTeam), winningTeam, "enemy king defeated");
        }
    }

    private void updateCapturePoints() {
        for (CapturePointState point : capturePoints) {
            int redPlayers = 0;
            int bluePlayers = 0;
            for (Player player : activeMatchPlayers()) {
                if (player.getLocation().distanceSquared(point.center) > MOBA_CAPTURE_POINT_RANGE_SQUARED) continue;
                int team = teams.getOrDefault(player.getUniqueId(), -1);
                if (team == 0) redPlayers++;
                else if (team == 1) bluePlayers++;
            }
            // The original beacon is contested when both teams are present.
            if (redPlayers > 0 && bluePlayers > 0) continue;
            int advancingTeam;
            int playersOnPoint;
            if (redPlayers > 0) {
                advancingTeam = 0;
                playersOnPoint = redPlayers;
            } else if (bluePlayers > 0) {
                advancingTeam = 1;
                playersOnPoint = bluePlayers;
            } else if (point.owner >= 0) {
                // An owned point slowly restores its progress when nobody is standing on it.
                advancingTeam = point.owner;
                playersOnPoint = 1;
            } else {
                continue;
            }

            if (point.side < 0) point.side = advancingTeam;
            if (point.side == advancingTeam) {
                point.progress += playersOnPoint;
                int maximum = point.owner < 0 ? 10 : 5;
                point.progress = Math.min(maximum, point.progress);
                updateCapturePointBlocks(point);
                if (point.progress >= maximum) setCapturePointOwner(point, advancingTeam);
            } else {
                point.progress -= playersOnPoint;
                if (point.progress <= 0) {
                    point.owner = -1;
                    point.side = advancingTeam;
                    point.progress = 0;
                    setCapturePointBeacon(point, -1);
                }
                updateCapturePointBlocks(point);
            }
        }
    }

    private void setCapturePointOwner(CapturePointState point, int owner) {
        if (point.owner == owner) return;
        point.owner = owner;
        point.side = owner;
        point.progress = 5;
        setCapturePointBeacon(point, owner);
        for (Player player : activeMatchPlayers()) {
            if (teams.getOrDefault(player.getUniqueId(), -1) == owner) {
                addGold(player, MOBA_CAPTURE_REWARD, "Capturing " + point.name);
            }
        }
        announce(ChatColor.GOLD + teamName(owner) + " captured the " + point.colour + " " + point.name + " Beacon!");
    }

    private void updateCapturePointBlocks(CapturePointState point) {
        if (point.wool.isEmpty()) return;
        int maximum = point.owner < 0 ? 10 : 5;
        int count = Math.min(point.wool.size(),
                (int) Math.ceil(point.wool.size() / (double) maximum) * point.progress + 1);
        Material colour = teamWool(point.side);
        for (int i = 0; i < point.wool.size(); i++) {
            Block block = point.wool.get(i);
            rememberBlock(block);
            Block above = block.getRelative(BlockFace.UP);
            rememberBlock(above);
            Material target = i < count ? colour : Material.WHITE_WOOL;
            block.setType(target, false);
            above.setType(target, false);
        }
    }

    private void setCapturePointBeacon(CapturePointState point, int owner) {
        Block beacon = point.center.getBlock().getRelative(BlockFace.DOWN);
        rememberBlock(beacon);
        beacon.setType(owner < 0 ? Material.WHITE_WOOL : teamWool(owner), false);
    }

    private Material teamWool(int team) {
        return team == 0 ? Material.RED_WOOL : team == 1 ? Material.LIGHT_BLUE_WOOL : Material.WHITE_WOOL;
    }

    private void recordMobaTowerEntries(long now) {
        List<Mob> minions = liveHeroesOfGwenMinions();
        List<Player> matchPlayers = activeMatchPlayers();
        for (Map.Entry<Integer, List<TowerState>> entry : towers.entrySet()) {
            int owner = entry.getKey();
            for (TowerState tower : entry.getValue()) {
                if (tower.dead) continue;
                Set<UUID> inRange = new HashSet<>();
                for (Mob minion : minions) {
                    if (heroesOfGwenMinionTeams.getOrDefault(minion.getUniqueId(), -1) != 1 - owner
                            || minion.getLocation().distanceSquared(tower.location) > MOBA_TOWER_RANGE_SQUARED) continue;
                    UUID id = minion.getUniqueId();
                    inRange.add(id);
                    tower.targetEnteredAt.putIfAbsent(id, now);
                }
                for (Player player : matchPlayers) {
                    if (teams.getOrDefault(player.getUniqueId(), -1) != 1 - owner
                            || player.getLocation().distanceSquared(tower.location) > MOBA_TOWER_RANGE_SQUARED) continue;
                    UUID id = player.getUniqueId();
                    inRange.add(id);
                    tower.targetEnteredAt.putIfAbsent(id, now);
                }
                tower.targetEnteredAt.keySet().removeIf(id -> !inRange.contains(id));
            }
        }
    }

    private void updateMobaTowers() {
        for (Map.Entry<Integer, List<TowerState>> entry : towers.entrySet()) {
            int owner = entry.getKey();
            for (TowerState tower : entry.getValue()) {
                if (tower.dead) continue;
                Entity target = mobaTowerTarget(tower, owner);
                if (target instanceof Mob minion) {
                    minion.damage(MOBA_TOWER_DAMAGE);
                    minion.getWorld().spawnParticle(Particle.CRIT, minion.getLocation().add(0, 0.7D, 0),
                            4, 0.2D, 0.25D, 0.2D, 0.02D);
                } else if (target instanceof Player player) {
                    if (player.getHealth() <= MOBA_TOWER_DAMAGE) hooks.eliminate(player, null, "was defeated by a tower guardian");
                    else player.setHealth(player.getHealth() - MOBA_TOWER_DAMAGE);
                    player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0),
                            4, 0.2D, 0.3D, 0.2D, 0.02D);
                }
                for (Player teammate : activeMatchPlayers()) {
                    if (teams.getOrDefault(teammate.getUniqueId(), -1) != owner
                            || teammate.getLocation().distanceSquared(tower.location) > MOBA_TOWER_RANGE_SQUARED) continue;
                    teammate.setHealth(Math.min(teammate.getMaxHealth(), teammate.getHealth() + MOBA_TOWER_HEAL));
                }
            }
        }
    }

    private Entity mobaTowerTarget(TowerState tower, int owner) {
        List<Mob> minions = liveHeroesOfGwenMinions().stream()
                .filter(minion -> heroesOfGwenMinionTeams.getOrDefault(minion.getUniqueId(), -1) == 1 - owner)
                .filter(minion -> tower.targetEnteredAt.containsKey(minion.getUniqueId()))
                .toList();
        List<Player> playersInRange = activeMatchPlayers().stream()
                .filter(player -> teams.getOrDefault(player.getUniqueId(), -1) == 1 - owner)
                .filter(player -> tower.targetEnteredAt.containsKey(player.getUniqueId()))
                .toList();
        Mob firstMinion = minions.stream()
                .min(java.util.Comparator.comparingLong(minion ->
                        tower.targetEnteredAt.getOrDefault(minion.getUniqueId(), Long.MAX_VALUE)))
                .orElse(null);
        Player firstPlayer = playersInRange.stream()
                .min(java.util.Comparator.comparingLong(player ->
                        tower.targetEnteredAt.getOrDefault(player.getUniqueId(), Long.MAX_VALUE)))
                .orElse(null);
        if (firstMinion == null) return firstPlayer;
        if (firstPlayer == null) return firstMinion;
        long minionEnteredAt = tower.targetEnteredAt.getOrDefault(firstMinion.getUniqueId(), Long.MAX_VALUE);
        long playerEnteredAt = tower.targetEnteredAt.getOrDefault(firstPlayer.getUniqueId(), Long.MAX_VALUE);
        return ArcadePortRules.heroesOfGwenTowerTargetsPlayer(playerEnteredAt, minionEnteredAt)
                ? firstPlayer : firstMinion;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onStructureDamage(EntityDamageEvent event) {
        if (stopped || game == ArcadePortGame.CASTLE_ASSAULT) {
            if (stopped || game != ArcadePortGame.CASTLE_ASSAULT) return;
            Entity damaged = event.getEntity();
            if (damaged instanceof Zombie zombie && kingState(zombie) != null) {
                event.setCancelled(true);
                if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;
                Player attacker = resolveAttacker(byEntity);
                if (attacker == null) return;
                KingState king = kingState(zombie);
                if (teams.getOrDefault(attacker.getUniqueId(), -1) == king.team) return;
                if (king.activeCrystals() > 0) {
                    attacker.sendMessage(ChatColor.RED + "Destroy both sentry crystals before attacking this King.");
                    return;
                }
                damageCastleKing(king, event.getFinalDamage(), attacker);
            } else if (damaged instanceof EnderCrystal crystal && crystalTeam(crystal) >= 0) {
                event.setCancelled(true);
            }
            return;
        }
        if (game != ArcadePortGame.HEROES_OF_GWEN) return;
        Entity damaged = event.getEntity();
        if (heroesOfGwenPumpkinKing != null
                && damaged.getUniqueId().equals(heroesOfGwenPumpkinKing.getUniqueId())) {
            if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
                event.setCancelled(true);
                return;
            }
            Player attacker = resolveAttacker(byEntity);
            if (attacker == null || !activePlayers.contains(attacker.getUniqueId())
                    || !isInsideHeroesOfGwenPumpkinBoundary(attacker.getLocation())) {
                event.setCancelled(true);
            }
            return;
        }
        TowerState tower = towerFor(damaged);
        if (tower != null) {
            event.setCancelled(true);
            if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;
            Integer minionTeam = heroesOfGwenMinionTeams.get(byEntity.getDamager().getUniqueId());
            if (minionTeam != null) {
                if (minionTeam == tower.owner || !canDamageMobaTower(tower, minionTeam)) return;
                damageMobaTower(tower, event.getFinalDamage(), null);
                return;
            }
            Player attacker = resolveAttacker(byEntity);
            if (attacker == null || teams.getOrDefault(attacker.getUniqueId(), -1) == tower.owner) return;
            if (attacker.getLocation().distanceSquared(tower.location) > MOBA_TOWER_RANGE_SQUARED) {
                attacker.sendMessage(ChatColor.RED + "You must enter the tower's 10-block range to damage it.");
                return;
            }
            if (!canDamageMobaTower(tower, teams.getOrDefault(attacker.getUniqueId(), -1))) {
                attacker.sendMessage(ChatColor.RED + "Destroy the enemy outer tower before attacking this tower.");
                return;
            }
            damageMobaTower(tower, event.getFinalDamage(), attacker);
            return;
        }
        for (Map.Entry<Integer, Wither> entry : withers.entrySet()) {
            if (!entry.getValue().equals(damaged)) continue;
            event.setCancelled(true);
            if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;
            Player attacker = resolveAttacker(byEntity);
            if (attacker == null || teams.getOrDefault(attacker.getUniqueId(), -1) == entry.getKey()) return;
            if (!Boolean.TRUE.equals(witherDamageable.get(entry.getKey()))) {
                attacker.sendMessage(ChatColor.RED + "Destroy every tower protecting this Wither first.");
                return;
            }
            Wither wither = entry.getValue();
            double damage = event.getFinalDamage();
            if (wither.getHealth() <= damage) {
                if (attacker != null) {
                    heroesOfGwenFinalObjectiveHits.computeIfAbsent(attacker.getUniqueId(), ignored -> new HashSet<>())
                            .add("boss:" + entry.getKey());
                }
                int winnerTeam = 1 - entry.getKey();
                announce(ChatColor.GOLD + teamName(winnerTeam) + " destroyed the enemy Wither!");
                finishHeroesOfGwen(winnerTeam, "enemy Wither defeated");
            } else {
                wither.setHealth(wither.getHealth() - damage);
            }
            return;
        }
    }

    private void damageMobaTower(TowerState tower, double damage, Player attacker) {
        tower.health -= damage;
        if (tower.health > 0) {
            if (tower.guardian != null && tower.guardian.isValid()) {
                tower.guardian.setCustomName(teamColour(tower.owner)
                        + (tower.first ? "Outer Tower" : "Inner Tower") + ChatColor.GRAY + " "
                        + (int) Math.ceil(tower.health));
            }
            return;
        }
        tower.dead = true;
        if (attacker != null) {
            heroesOfGwenFinalObjectiveHits.computeIfAbsent(attacker.getUniqueId(), ignored -> new HashSet<>())
                    .add("tower:" + tower.marker);
        }
        if (tower.guardian != null) {
            entities.remove(tower.guardian);
            tower.guardian.remove();
        }
        if (tower.crystal != null) {
            entities.remove(tower.crystal);
            tower.crystal.remove();
        }
        world.spawnParticle(Particle.EXPLOSION, tower.location, 18, 0.6D, 0.6D, 0.6D, 0.05D);
        world.playSound(tower.location, Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.8F);
        int reward = tower.first ? 100 : 250;
        for (Player player : onlinePlayers()) {
            if (teams.getOrDefault(player.getUniqueId(), -1) != tower.owner) addGold(player, reward, "Enemy tower destroyed");
        }
        Wither wither = withers.get(tower.owner);
        if (wither != null && wither.isValid()) {
            double witherDamage = tower.first ? 50.0D : 100.0D;
            if (wither.getHealth() <= witherDamage) {
                if (attacker != null) {
                    heroesOfGwenFinalObjectiveHits.computeIfAbsent(attacker.getUniqueId(), ignored -> new HashSet<>())
                            .add("boss:" + tower.owner);
                }
                int winnerTeam = 1 - tower.owner;
                announce(ChatColor.GOLD + teamName(winnerTeam) + " destroyed the enemy Wither!");
                finishHeroesOfGwen(winnerTeam, "enemy Wither defeated by tower damage");
                return;
            }
            wither.setHealth(wither.getHealth() - witherDamage);
        }
        int remaining = (int) towers.getOrDefault(tower.owner, List.of()).stream().filter(t -> !t.dead).count();
        if (remaining == 0) {
            witherDamageable.put(tower.owner, true);
            int attackingTeam = 1 - tower.owner;
            if (heroesOfGwenIronMinionTeams.add(attackingTeam)) {
                announce(ChatColor.GOLD + teamName(attackingTeam)
                        + " minion waves will now spawn with iron armour!");
            }
            announce(ChatColor.GOLD + teamName(tower.owner) + " towers are destroyed. Their Wither is now vulnerable!");
        }
    }

    private boolean canDamageMobaTower(TowerState tower, int attackerTeam) {
        if (attackerTeam < 0 || attackerTeam == tower.owner || tower.dead) return false;
        if (tower.first) return true;
        boolean outerAlive = towers.getOrDefault(tower.owner, List.of()).stream()
                .anyMatch(other -> other.first && !other.dead);
        return !outerAlive;
    }

    private Player nearestEnemy(Location origin, int owner) {
        Player best = null;
        double distance = MOBA_TOWER_RANGE_SQUARED;
        for (Player player : activeMatchPlayers()) {
            if (teams.getOrDefault(player.getUniqueId(), -1) == owner) continue;
            double candidate = player.getLocation().distanceSquared(origin);
            if (candidate < distance) {
                best = player;
                distance = candidate;
            }
        }
        return best;
    }

    private void openStore(Player player) {
        String hero = selectedHeroesOfGwen.getOrDefault(player.getUniqueId(), "hattori");
        String role = ArcadePortRules.heroesOfGwenHeroRole(hero);
        openStore(player, role == null ? "Assassin" : role);
    }

    private void openStore(Player player, String role) {
        if (role == null) {
            openStore(player);
            return;
        }
        StoreHolder holder = new StoreHolder(player.getUniqueId(), role);
        Inventory inventory = Bukkit.createInventory(holder, 45, ChatColor.DARK_GREEN + "Heroes of Gwen: " + role);
        holder.inventory = inventory;
        int slot = 0;
        for (int index = 0; index < storeEntries.size() && slot < 45; index++) {
            StoreEntry entry = storeEntries.get(index);
            if (!entry.role.equals(role)) continue;
            int price = ArcadePortRules.heroesOfGwenStoreItemCost(entry.cost, mixedArcade);
            ItemStack item = new ItemStack(entry.material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + entry.name);
            meta.setLore(List.of(ChatColor.GRAY + entry.description,
                    ChatColor.WHITE + "Standard cost: " + ChatColor.GRAY + entry.cost,
                    ChatColor.WHITE + "Your cost: " + ChatColor.GOLD + price + " gold",
                    ChatColor.YELLOW + "Click to purchase"));
            meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.INTEGER, index);
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Back to role selection");
        backMeta.setLore(List.of(ChatColor.GRAY + "Choose a different role's store."));
        backMeta.getPersistentDataContainer().set(shopRoleKey, PersistentDataType.STRING, "back");
        back.setItemMeta(backMeta);
        inventory.setItem(44, back);
        openStores.put(player.getUniqueId(), holder);
        player.openInventory(inventory);
        updateGoldActionBar(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onStoreClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !openStores.containsKey(player.getUniqueId())
                || !(event.getView().getTopInventory().getHolder() instanceof StoreHolder holder)
                || !holder.equals(openStores.get(player.getUniqueId()))) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String selectedRole = clicked.getItemMeta().getPersistentDataContainer()
                .get(shopRoleKey, PersistentDataType.STRING);
        if (selectedRole != null) {
            openStore(player, "back".equals(selectedRole) ? null : selectedRole);
            return;
        }
        Integer index = clicked.getItemMeta().getPersistentDataContainer().get(shopItemKey, PersistentDataType.INTEGER);
        if (index == null || index < 0 || index >= storeEntries.size()) return;
        StoreEntry entry = storeEntries.get(index);
        if (!entry.role.equals(holder.role)) return;
        purchaseStoreItem(player, entry);
    }

    @EventHandler
    public void onStoreClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || !(event.getView().getTopInventory().getHolder() instanceof StoreHolder closing)) return;
        // Opening a different store page closes the previous view; do not remove the new holder.
        if (closing.equals(openStores.get(player.getUniqueId()))) openStores.remove(player.getUniqueId());
    }

    private void purchaseStoreItem(Player player, StoreEntry entry) {
        int cost = ArcadePortRules.heroesOfGwenStoreItemCost(entry.cost, mixedArcade);
        int gold = playerGold.getOrDefault(player.getUniqueId(), 0);
        if (gold < cost) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " gold for " + entry.name + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.7F);
            return;
        }
        playerGold.put(player.getUniqueId(), gold - cost);
        ItemStack reward = new ItemStack(entry.material);
        ItemMeta rewardMeta = reward.getItemMeta();
        rewardMeta.setDisplayName(ChatColor.GREEN + entry.name);
        reward.setItemMeta(rewardMeta);
        String materialName = entry.material.name();
        if (materialName.endsWith("_HELMET") || materialName.endsWith("_CHESTPLATE")
                || materialName.endsWith("_LEGGINGS") || materialName.endsWith("_BOOTS")) {
            equip(player, reward);
        } else if (materialName.endsWith("_SWORD")) {
            player.getInventory().setItem(0, reward);
        } else if (entry.material == Material.BOW) {
            player.getInventory().setItem(0, reward);
        } else {
            player.getInventory().addItem(reward);
        }
        player.sendMessage(ChatColor.GREEN + "Purchased " + entry.name + " for " + cost + " gold.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
        updateGoldActionBar(player);
    }

    private void equip(Player player, ItemStack item) {
        String name = item.getType().name();
        if (name.endsWith("_HELMET")) player.getInventory().setHelmet(item);
        else if (name.endsWith("_CHESTPLATE")) player.getInventory().setChestplate(item);
        else if (name.endsWith("_LEGGINGS")) player.getInventory().setLeggings(item);
        else if (name.endsWith("_BOOTS")) player.getInventory().setBoots(item);
        else player.getInventory().addItem(item);
    }

    private void addGold(Player player, int amount, String reason) {
        if (player == null || !player.isOnline()) return;
        UUID id = player.getUniqueId();
        playerGold.put(id, playerGold.getOrDefault(id, 0) + amount);
        updateGoldActionBar(player);
        if (amount >= 100) player.sendMessage(ChatColor.GOLD + "+" + amount + " gold: " + reason);
    }

    private void updateGoldActionBar(Player player) {
        player.sendActionBar(ChatColor.GOLD + "Gold: " + playerGold.getOrDefault(player.getUniqueId(), 0)
                + ChatColor.GRAY + " | Right-click your gold ingot to open the store.");
    }

    private void checkTeamDeparture() {
        if (players.size() <= 1) return;
        for (int team = 0; team < 2; team++) {
            final int currentTeam = team;
            boolean hasOnlinePlayer = players.values().stream().anyMatch(player -> player.isOnline()
                    && teams.getOrDefault(player.getUniqueId(), -1) == currentTeam);
            if (!hasOnlinePlayer) {
                int winner = 1 - team;
                if (game == ArcadePortGame.CASTLE_ASSAULT) recordCastleAssaultResult(winner);
                if (game == ArcadePortGame.HEROES_OF_GWEN) {
                    finishHeroesOfGwen(winner, "opposing team left the match");
                } else {
                    hooks.finish(firstPlayerOnTeam(winner), winner, "opposing team left the match");
                }
                return;
            }
        }
    }

    /** Refresh the UUID-indexed player reference after FakePlayerPlugin respawns a body. */
    void onPlayerRespawn(Player player) {
        if (player == null || !players.containsKey(player.getUniqueId())) return;
        players.put(player.getUniqueId(), player);
    }

    private List<Player> onlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (Player player : players.values()) if (player != null && player.isOnline()) result.add(player);
        return result;
    }

    private List<Player> activeMatchPlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID id : activePlayers) {
            Player player = players.get(id);
            if (player != null && player.isOnline()) result.add(player);
        }
        return result;
    }

    private Player firstPlayerOnTeam(int team) {
        return onlinePlayers().stream().filter(player -> teams.getOrDefault(player.getUniqueId(), -1) == team)
                .findFirst().orElse(null);
    }

    private int teamFromMarker(String marker, boolean ignored) {
        return teamFromMarker(marker);
    }

    private KingState kingState(Zombie entity) {
        if (entity == null) return null;
        Integer team = entity.getPersistentDataContainer().get(kingKey, PersistentDataType.INTEGER);
        return team == null ? null : kings.get(team);
    }

    private int crystalTeam(EnderCrystal entity) {
        Integer team = entity.getPersistentDataContainer().get(kingKey, PersistentDataType.INTEGER);
        return team == null ? -1 : team;
    }

    private TowerState towerFor(Entity entity) {
        if (entity == null) return null;
        String marker = entity.getPersistentDataContainer().get(towerKey, PersistentDataType.STRING);
        if (marker == null) return null;
        for (List<TowerState> teamTowers : towers.values()) {
            for (TowerState tower : teamTowers) {
                if (tower.marker.equals(marker) && (entity.equals(tower.guardian) || entity.equals(tower.crystal))) return tower;
            }
        }
        return null;
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private void restoreCarrierHelmet(Player player) {
        ItemStack old = carriedTntOldHelmet.remove(player.getUniqueId());
        player.getInventory().setHelmet(old == null ? null : old.clone());
    }

    private void rememberBlock(Block block) {
        changedBlocks.putIfAbsent(block, block.getBlockData().clone());
    }

    private void announce(String message) {
        for (Player player : onlinePlayers()) player.sendMessage(message);
    }

    private static String teamName(int team) {
        return team == 0 ? "Red" : "Blue";
    }

    private static ChatColor teamColour(int team) {
        return team == 0 ? ChatColor.RED : ChatColor.AQUA;
    }

    private static ItemStack namedItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.GRAY + lore));
        item.setItemMeta(meta);
        return item;
    }

    void onPlayerEliminated(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        if (game == ArcadePortGame.CASTLE_ASSAULT) {
            castleKillStreaks.remove(id);
            castleArcherNextArrowAt.remove(id);
            castleDemolitionistNextTntAt.remove(id);
        }
        activePlayers.remove(id);
        StoreHolder store = openStores.remove(id);
        if (store != null && player.isOnline()
                && player.getOpenInventory().getTopInventory().getHolder() == store) {
            player.closeInventory();
        }
        if (game == ArcadePortGame.CASTLE_ASSAULT && id.equals(objectiveCarrier)) {
            detonateObjectiveTnt(player);
        }
    }

    void onPlayerRespawned(Player player) {
        if (player != null && player.isOnline() && players.containsKey(player.getUniqueId())) {
            activePlayers.add(player.getUniqueId());
        }
    }

    void onQuit(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        activePlayers.remove(id);
        openStores.remove(id);
        if (game == ArcadePortGame.CASTLE_ASSAULT && id.equals(objectiveCarrier)) {
            detonateObjectiveTnt(player);
        }
    }

    private void recordCastleKill(Player killer, Player victim, boolean objectiveTnt) {
        if (killer == null || victim == null || killer.equals(victim)) return;
        UUID victimId = victim.getUniqueId();
        if (!castleCreditedVictims.add(victimId)) return;
        UUID killerId = killer.getUniqueId();
        if (teams.getOrDefault(killerId, -1) < 0
                || teams.getOrDefault(killerId, -1) == teams.getOrDefault(victimId, -2)) return;
        castleKillsByPlayer.merge(killerId, 1, Integer::sum);
        if (objectiveTnt) castleTntHolderKillsByPlayer.merge(killerId, 1, Integer::sum);
    }

    private void recordCastleAssaultResult(int winningTeam) {
        if (game != ArcadePortGame.CASTLE_ASSAULT || castleResultRecorded || winningTeam < 0) return;
        castleResultRecorded = true;
        KingState winningKing = kings.get(winningTeam);
        String basePath = "local-stats.";
        for (Map.Entry<UUID, Integer> entry : teams.entrySet()) {
            UUID playerId = entry.getKey();
            if (entry.getValue() != winningTeam) {
                plugin.getConfig().set(basePath + playerId + ".castle-assault.win-streak", 0);
                continue;
            }
            String statsPath = basePath + playerId + ".castle-assault.";
            int streak = plugin.getConfig().getInt(statsPath + "win-streak", 0) + 1;
            plugin.getConfig().set(statsPath + "win-streak", streak);
            int kills = castleKillsByPlayer.getOrDefault(playerId, 0);
            int nextHighest = teams.entrySet().stream()
                    .filter(other -> other.getValue() == winningTeam && !other.getKey().equals(playerId))
                    .mapToInt(other -> castleKillsByPlayer.getOrDefault(other.getKey(), 0))
                    .max().orElse(0);
            if (castleFinalKingHitByPlayer.contains(playerId)) {
                awardCastleAssaultAchievement(playerId, "one-man-army");
            }
            if (winningKing != null && winningKing.health < 50) {
                awardCastleAssaultAchievement(playerId, "clutchness");
            }
            if (kills >= 8 && !castleRodUsed.contains(playerId)) {
                awardCastleAssaultAchievement(playerId, "blasphemous");
            }
            if (kills > 0 && kills >= nextHighest * 2) {
                awardCastleAssaultAchievement(playerId, "multiplicative");
            }
            if (castleTntHolderKillsByPlayer.getOrDefault(playerId, 0) >= 5) {
                awardCastleAssaultAchievement(playerId, "moral-exercise");
            }
            if (streak >= 3) awardCastleAssaultAchievement(playerId, "remember-the-days");
        }
        plugin.saveConfig();
    }

    private void awardCastleAssaultAchievement(UUID playerId, String achievementId) {
        if (playerId == null || achievementId == null || achievementId.isBlank()) return;
        String key = "local-achievements." + playerId + ".castle-assault." + achievementId;
        if (plugin.getConfig().getBoolean(key, false)) return;
        plugin.getConfig().set(key, true);
        Player player = players.get(playerId);
        if (player != null && player.isOnline()) {
            String name = ArcadeProgressCatalog.achievements("castle-assault").stream()
                    .filter(achievement -> achievement.id().equals(achievementId))
                    .map(ArcadeProgressCatalog.Achievement::name).findFirst().orElse(achievementId);
            player.sendMessage(ChatColor.GOLD + "Achievement unlocked: " + name);
        }
    }

    private void finishHeroesOfGwen(int winningTeam, String reason) {
        if (game != ArcadePortGame.HEROES_OF_GWEN || heroesOfGwenResultRecorded || winningTeam < 0) return;
        heroesOfGwenResultRecorded = true;
        for (Player player : activeMatchPlayers()) {
            UUID id = player.getUniqueId();
            if (teams.getOrDefault(id, -1) != winningTeam) continue;
            Set<String> finalHits = heroesOfGwenFinalObjectiveHits.getOrDefault(id, Set.of());
            long towerFinalHits = finalHits.stream().filter(value -> value.startsWith("tower:")).distinct().count();
            boolean finalBossHit = finalHits.stream().anyMatch(value -> value.startsWith("boss:"));
            if (towerFinalHits >= 2 && finalBossHit) awardHeroesOfGwenAchievement(player, "mvp");
            if (heroesOfGwenChangedAfterTowerLoss.contains(id)) {
                awardHeroesOfGwenAchievement(player, "tactical-genius");
            }
        }
        hooks.finish(firstPlayerOnTeam(winningTeam), winningTeam, reason == null ? "Heroes of Gwen finished" : reason);
    }

    private void awardHeroesOfGwenAchievement(Player player, String achievementId) {
        if (player == null || achievementId == null || achievementId.isBlank()) return;
        UUID playerId = player.getUniqueId();
        String key = "local-achievements." + playerId + ".heroes-of-gwen." + achievementId;
        if (plugin.getConfig().getBoolean(key, false)) return;
        plugin.getConfig().set(key, true);
        String name = ArcadeProgressCatalog.achievements("heroes-of-gwen").stream()
                .filter(achievement -> achievement.id().equals(achievementId))
                .map(ArcadeProgressCatalog.Achievement::name).findFirst().orElse(achievementId);
        if (player.isOnline()) player.sendMessage(ChatColor.GOLD + "Achievement unlocked: " + name);
        plugin.saveConfig();
    }

    void onPlayerKill(Player killer) {
        if (stopped || killer == null) return;
        if (game == ArcadePortGame.HEROES_OF_GWEN) {
            UUID id = killer.getUniqueId();
            int kills = heroesOfGwenKills.merge(id, 1, Integer::sum);
            String hero = selectedHeroesOfGwen.getOrDefault(id, "hattori");
            String role = ArcadePortRules.heroesOfGwenHeroRole(hero);
            if (role != null) heroesOfGwenRoleKills.computeIfAbsent(id, ignored -> new HashMap<>())
                    .merge(role, 1, Integer::sum);
            if (kills >= 15) awardHeroesOfGwenAchievement(killer, "objective-null");
            return;
        }
        if (game != ArcadePortGame.CASTLE_ASSAULT) return;
        int team = teams.getOrDefault(killer.getUniqueId(), -1);
        if (team < 0) return;
        int streak = castleKillStreaks.merge(killer.getUniqueId(), 1, Integer::sum);
        killer.setLevel(killer.getLevel() + 1);
        killer.getInventory().addItem(namedItem(Material.GOLDEN_APPLE, ChatColor.LIGHT_PURPLE + "Golden Applegate",
                "Kill reward from Castle Assault."));
        giveCastleKillStreakReward(killer, castleAssaultPlayerKits.getOrDefault(killer.getUniqueId(), "fighter"), streak);
        if (streak > 0 && streak % 2 == 0) {
            announce(ChatColor.GREEN + killer.getName() + " is on a " + streak + " player kill streak!");
        }
        if (objectiveKillsEnabled == 0) return;
        int kills = team == 0 ? ++castleKillCountRed : ++castleKillCountBlue;
        if (kills % 20 != 0) return;
        KingState enemy = kings.get(1 - team);
        if (enemy == null || enemy.activeCrystals() == 0) {
            if (enemy != null) damageCastleKing(enemy, 10, killer);
            return;
        }
        List<EnderCrystal> activeCrystals = enemy.crystals.stream().filter(Entity::isValid).toList();
        if (activeCrystals.isEmpty()) return;
        destroyCastleCrystal(enemy, activeCrystals.get(ThreadLocalRandom.current().nextInt(activeCrystals.size())), killer);
    }

    private void giveCastleKillStreakReward(Player player, String kit, int streak) {
        if (streak < 2 || streak > 8 || streak % 2 != 0) return;
        switch (kit) {
            case "alchemist" -> {
                if (streak == 2) player.getInventory().addItem(castlePotion(Material.SPLASH_POTION,
                        PotionEffectType.SLOWNESS, 90 * 20, 0, "Splash Slowness Potion"));
                else if (streak == 4) player.getInventory().addItem(castlePotion(Material.SPLASH_POTION,
                        PotionEffectType.WEAKNESS, 90 * 20, 0, "Splash Weakness Potion"));
                else if (streak == 6) player.getInventory().addItem(castlePotion(Material.SPLASH_POTION,
                        PotionEffectType.INSTANT_DAMAGE, 1, 1, "Splash Harming II Potion"),
                        castlePotion(Material.SPLASH_POTION, PotionEffectType.INSTANT_DAMAGE, 1, 1,
                                "Splash Harming II Potion"),
                        castlePotion(Material.SPLASH_POTION, PotionEffectType.INSTANT_DAMAGE, 1, 1,
                                "Splash Harming II Potion"),
                        castlePotion(Material.SPLASH_POTION, PotionEffectType.INSTANT_DAMAGE, 1, 1,
                                "Splash Harming II Potion"));
                else player.getInventory().addItem(castlePotion(Material.POTION,
                        PotionEffectType.REGENERATION, 10 * 20, 2, "Regeneration III Potion"));
            }
            case "archer" -> {
                if (streak == 2) player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrows", "Kill-streak reward.", 8));
                else if (streak == 4) player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrows", "Kill-streak reward.", 12));
                else if (streak == 6) player.getInventory().addItem(castleEnchantedBook(Enchantment.PUNCH, 1, "Punch I"));
                else {
                    player.getInventory().addItem(namedItem(Material.ARROW, "Fletched Arrows", "Kill-streak reward.", 32),
                            castleEnchantedBook(Enchantment.FLAME, 1, "Flame I"));
                }
            }
            case "demolitionist" -> {
                if (streak == 2) player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT", "Kill-streak reward.", 2));
                else if (streak == 4) player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT", "Kill-streak reward.", 3));
                else if (streak == 6) player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT", "Kill-streak reward.", 4));
                else player.getInventory().addItem(namedItem(Material.TNT, "Throwing TNT", "Kill-streak reward.", 5),
                        castleFlintAndSteel(30, true));
            }
            case "tank" -> {
                if (streak == 2) player.getInventory().addItem(namedItem(Material.GOLDEN_APPLE,
                        ChatColor.LIGHT_PURPLE + "Golden Applegate", "Kill-streak reward."));
                else if (streak == 4) player.getInventory().addItem(castlePotion(Material.POTION,
                        PotionEffectType.REGENERATION, 22 * 20, 1, "Regeneration II Potion"));
                else if (streak == 6) player.getInventory().addItem(castlePotion(Material.POTION,
                        PotionEffectType.RESISTANCE, 90 * 20, 0, "Resistance Potion"));
                else player.getInventory().addItem(castleEnchantedBook(Enchantment.THORNS, 2, "Thorns II"));
            }
            default -> {
                if (streak == 2) player.getInventory().addItem(namedItem(Material.GOLDEN_APPLE,
                        ChatColor.LIGHT_PURPLE + "Golden Applegate", "Kill-streak reward."));
                else if (streak == 4) player.getInventory().addItem(castlePotion(Material.SPLASH_POTION,
                        PotionEffectType.INSTANT_HEALTH, 1, 1, "Splash Healing II Potion"));
                else if (streak == 6) player.getInventory().addItem(castlePotion(Material.POTION,
                        PotionEffectType.SPEED, 90 * 20, 1, "Speed II Potion"));
                else player.getInventory().addItem(castleEnchantedBook(Enchantment.FIRE_ASPECT, 1, "Fire Aspect I"));
            }
        }
    }

    void giveCastleAssaultKit(Player player, String requestedKit) {
        if (stopped || game != ArcadePortGame.CASTLE_ASSAULT || player == null
                || !players.containsKey(player.getUniqueId())) return;
        String kit = ArcadePortRules.normalizeCastleAssaultKit(requestedKit);
        if (kit == null) kit = "fighter";
        castleAssaultPlayerKits.put(player.getUniqueId(), kit);
        long kitAppliedAt = System.currentTimeMillis();
        if ("archer".equals(kit)) {
            castleArcherNextArrowAt.put(player.getUniqueId(),
                    kitAppliedAt + ArcadePortRules.CASTLE_ASSAULT_ARCHER_REFILL_MILLIS);
        } else {
            castleArcherNextArrowAt.remove(player.getUniqueId());
        }
        if ("demolitionist".equals(kit)) {
            castleDemolitionistNextTntAt.put(player.getUniqueId(), kitAppliedAt + ArcadePortRules.CASTLE_ASSAULT_DEMOLITIONIST_TNT_REFILL_MILLIS);
        } else {
            castleDemolitionistNextTntAt.remove(player.getUniqueId());
        }
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 8 * 20, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 8 * 20, 3));

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setUnbreakable(true);
        swordMeta.setLore(List.of(ChatColor.GRAY + "Castle Assault kit item"));
        sword.setItemMeta(swordMeta);
        player.getInventory().setItem(0, sword);

        boolean demolitionist = "demolitionist".equals(kit);
        player.getInventory().setHelmet(castleArmour(Material.DIAMOND_HELMET,
                demolitionist ? Enchantment.BLAST_PROTECTION : null, demolitionist ? 4 : 0));
        player.getInventory().setChestplate(castleArmour(Material.IRON_CHESTPLATE,
                demolitionist ? Enchantment.BLAST_PROTECTION : "tank".equals(kit) ? Enchantment.PROTECTION : null,
                demolitionist ? 4 : "tank".equals(kit) ? 1 : 0));
        player.getInventory().setLeggings(castleArmour(Material.IRON_LEGGINGS,
                demolitionist ? Enchantment.BLAST_PROTECTION : null, demolitionist ? 4 : 0));
        player.getInventory().setBoots(castleArmour(Material.DIAMOND_BOOTS,
                demolitionist ? Enchantment.BLAST_PROTECTION : null, demolitionist ? 4 : 0));

        switch (kit) {
            case "alchemist" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100_000, 0));
                ItemStack speed = castlePotion(Material.POTION, PotionEffectType.SPEED,
                        3 * 60 * 20, 0, "Speed Potion");
                ItemMeta speedMeta = speed.getItemMeta();
                speedMeta.setLore(List.of(ChatColor.GRAY + "Kit Item"));
                speed.setItemMeta(speedMeta);
                player.getInventory().setItem(1, speed);
            }
            case "archer" -> {
                ItemStack bow = new ItemStack(Material.BOW);
                ItemMeta bowMeta = bow.getItemMeta();
                bowMeta.setUnbreakable(true);
                bowMeta.setLore(List.of(ChatColor.GRAY + "Kit Item"));
                bow.setItemMeta(bowMeta);
                player.getInventory().setItem(1, bow);
                player.getInventory().setItem(2, namedItem(Material.ARROW, "Fletched Arrow",
                        "Automatically supplied by the Archer kit.", 10));
            }
            case "demolitionist" -> {
                player.getInventory().setItem(1, castleFlintAndSteel(4, true));
                player.getInventory().setItem(2, namedItem(Material.TNT, "Throwing TNT",
                        "Left-click to throw.", 2));
            }
            case "fighter" -> player.getInventory().setItem(1, namedItem(Material.GOLDEN_APPLE,
                    ChatColor.LIGHT_PURPLE + "Golden Applegate", "Fighter kit item."));
            case "tank" -> { /* The source Tank relies on its armour and kit passives. */ }
            default -> { }
        }
        player.updateInventory();
    }

    void setHeroesOfGwenHero(Player player, String requestedHero) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN || player == null
                || !players.containsKey(player.getUniqueId())) return;
        String hero = ArcadePortRules.normalizeHeroesOfGwenHero(requestedHero);
        if (hero == null) hero = "hattori";
        String previous = selectedHeroesOfGwen.put(player.getUniqueId(), hero);
        if (!hero.equals(previous) && "bardolf".equals(previous)) removeBardolfPack(player.getUniqueId());
        if (!hero.equals(previous) && "rowena".equals(previous)) removeRowenaState(player.getUniqueId());
        if (!hero.equals(previous) && "dana".equals(previous)) removeDanaState(player.getUniqueId());
        if (!hero.equals(previous) && "biff".equals(previous)) removeBiffState(player.getUniqueId());
        if (!hero.equals(previous) && "ivy".equals(previous)) removeIvyState(player.getUniqueId());
        if (!hero.equals(previous) && "anath".equals(previous)) removeAnathState(player.getUniqueId());
        if (!hero.equals(previous) && previous != null) {
            int team = teams.getOrDefault(player.getUniqueId(), -1);
            boolean ownTowerLost = towers.getOrDefault(team, List.of()).stream().anyMatch(tower -> tower.dead);
            if (ownTowerLost) heroesOfGwenChangedAfterTowerLoss.add(player.getUniqueId());
        }
        if (!hero.equals(previous)) giveHeroesOfGwenLoadout(player, hero);
        String role = ArcadePortRules.heroesOfGwenHeroRole(hero);
        player.sendActionBar(ChatColor.GOLD + "Hero: " + ArcadePortRules.heroesOfGwenHeroDisplayName(hero)
                + ChatColor.GRAY + " | " + ChatColor.YELLOW + role + " Gold Upgrades");
    }

    private void giveHeroesOfGwenLoadout(Player player, String hero) {
        UUID id = player.getUniqueId();
        heroTntArrowCharges.remove(id);
        heroAmmoNextAt.remove(id);
        heroEnderBladeEndsAt.remove(id);
        heroEnderBladeOriginalSword.remove(id);
        heroInfinityEndsAt.remove(id);
        heroInfinityOriginalEnchant.remove(id);
        anathBurnBeams.remove(id);
        anathBurnBeamNextHitAt.remove(id);
        anathFlameDashEndsAt.remove(id);
        larissaWaterDashEndsAt.remove(id);
        larissaStormBlessingEndsAt.remove(id);
        larissaStormBlessingNextHealAt.remove(id);
        danaKnockUpEndsAt.remove(id);
        danaKnockUpNextHitAt.remove(id);
        danaRallies.remove(id);
        rowenaLightArrowsRemaining.remove(id);
        rowenaLightArrowNextHitAt.remove(id);
        rowenaCombatSlideEndsAt.remove(id);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItem(8, namedItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "Open Gold Upgrades", "Right-click to open your role shop."));
        switch (hero) {
            case "hattori" -> {
                player.setAllowFlight(player.isOnGround());
                player.getInventory().setItem(0, namedItem(Material.WOODEN_SWORD,
                        ChatColor.GREEN + "Sword", "Basic Hattori weapon; includes Double Jump."));
                giveHeroesOfGwenSkillItem(player, 1, "hattori.shuriken", Material.SNOWBALL,
                        "Shuriken", "Fires three damaging snowballs.");
                giveHeroesOfGwenSkillItem(player, 2, "hattori.ninja_dash", Material.FEATHER,
                        "Ninja Dash", "Dash up to ten blocks and damage enemies in your path.");
                giveHeroesOfGwenSkillItem(player, 3, "hattori.ender_blade", Material.NETHER_STAR,
                        "Ender Blade", "Use the Ender Blade for seven seconds.");
            }
            case "anath" -> {
                player.getInventory().setItem(0, namedItem(Material.BLAZE_ROD,
                        ChatColor.GREEN + "Flame Wand", "Fire 5-damage embers; four charges, replenished every second."));
                giveHeroesOfGwenSkillItem(player, 1, "anath.burn_beam", Material.FIRE_CHARGE,
                        "Burn Beam", "Send a 9-block horizontal beam through blocks and enemies. 11 damage; 9-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 2, "anath.flame_dash", Material.FEATHER,
                        "Flame Dash", "Dash horizontally for 600 ms, leaving fire for 5 seconds. 12-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 3, "anath.meteor_shower", Material.NETHER_STAR,
                        "Meteor Shower", "Throw to mark a 10-block radius for 7 seconds. 60-second cooldown.");
                player.getInventory().setItem(7, namedItem(Material.BLAZE_POWDER,
                        ChatColor.GOLD + "Embers", "Flame Wand ammunition; one charge replenishes every second."));
                player.getInventory().getItem(7).setAmount(ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_AMMO_CAPACITY);
                heroAmmoNextAt.put(id, System.currentTimeMillis()
                        + ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_AMMO_RELOAD_MILLIS);
            }
            case "ivy" -> {
                player.getInventory().setItem(0, namedItem(Material.WOODEN_SWORD,
                        ChatColor.GREEN + "Sword", "Ivy's basic melee weapon."));
                giveHeroesOfGwenSkillItem(player, 1, "ivy.vine_pull", Material.VINE,
                        "Vine Pull", "Fire a nine-block vine beam: damage enemies and pull the first enemy hero toward you. 10-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 2, "ivy.floral_leap", Material.FEATHER,
                        "Floral Leap", "Leap and create a 5-second flower bed; enemies within 5 blocks take 2 damage each second and are slowed. Click or sneak. 11-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 3, "ivy.thorn_wall", Material.NETHER_STAR,
                        "Thorn Wall", "Create a 5-second square thorn ring. Allies regenerate; enemies are rooted or damaged. Drop to activate; 45-second cooldown.");
            }
            case "biff" -> {
                player.getInventory().setItem(0, namedItem(Material.WOODEN_SWORD,
                        ChatColor.GREEN + "Sword", "Biff's basic melee weapon."));
                giveHeroesOfGwenSkillItem(player, 1, "biff.tether", Material.LEAD,
                        "Tether", "Hook nearby enemy heroes within 8 blocks, slow and pull them toward you. 5-second active tether; 12-second cooldown after it ends.");
                giveHeroesOfGwenSkillItem(player, 2, "biff.battle_leap", Material.FEATHER,
                        "Battle Leap", "Leap upward; on landing damage and knock up enemies within 5 blocks. Click or sneak. 11-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 3, "biff.cavalry_charge", Material.NETHER_STAR,
                        "Cavalry Charge", "Mount an armoured horse for 6 seconds; nearby enemies take 4 damage and are rooted. Drop to activate; 45-second cooldown after the initial 5.5-second active window.");
            }
            case "dana" -> {
                player.getInventory().setItem(0, namedItem(Material.WOODEN_SWORD,
                        ChatColor.GREEN + "Sword", "Dana's basic melee weapon."));
                giveHeroesOfGwenSkillItem(player, 1, "dana.pulse_heal", Material.LIME_DYE,
                        "Pulse Heal", "Heal yourself for 2 HP and nearby allies/minions for 4 HP within 7 blocks. 8-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 2, "dana.knock_up", Material.FEATHER,
                        "Knock Up", "Dash horizontally for 600 ms; enemies touched take 12 player / 8 minion damage and are launched. 10-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 3, "dana.rally", Material.NETHER_STAR,
                        "Rally", "Leap, then plant a 7-second banner that heals nearby allies and damages enemies on landing. Drop to activate; 40-second cooldown after the banner.");
            }
            case "rowena" -> {
                ItemStack bow = new ItemStack(Material.BOW);
                ItemMeta bowMeta = bow.getItemMeta();
                bowMeta.setUnbreakable(true);
                bowMeta.setDisplayName(ChatColor.GREEN + "Bow");
                bow.setItemMeta(bowMeta);
                player.getInventory().setItem(0, bow);
                giveHeroesOfGwenSkillItem(player, 1, "rowena.light_arrows", Material.GOLD_NUGGET,
                        "Light Arrows", "Infuse your next five arrows. They pass through blocks and deal high damage.");
                giveHeroesOfGwenSkillItem(player, 2, "rowena.combat_slide", Material.FEATHER,
                        "Combat Slide", "Dash horizontally along the ground for 250 ms. 8-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 3, "rowena.bombardment", Material.NETHER_STAR,
                        "Bombardment", "Ascend 15 blocks, then fire three explosive shots within 10 seconds. 60-second cooldown after the flight.");
                player.getInventory().setItem(7, namedItem(Material.ARROW,
                        ChatColor.YELLOW + "Fletched Arrow", "Replenished every two seconds; maximum two."));
                player.getInventory().getItem(7).setAmount(ArcadePortRules.HEROES_OF_GWEN_ROWENA_FLETCHER_MAX_ARROWS);
                heroAmmoNextAt.put(id, System.currentTimeMillis()
                        + ArcadePortRules.HEROES_OF_GWEN_ROWENA_FLETCHER_RELOAD_MILLIS);
            }
            case "bardolf" -> {
                player.getInventory().setItem(0, namedItem(Material.WOODEN_SWORD,
                        ChatColor.GREEN + "Sword", "Bardolf's basic weapon; attacks direct the wolf pack."));
                giveHeroesOfGwenSkillItem(player, 1, "bardolf.summon_wolf", Material.BONE,
                        "Summon Wolf", "Summon up to five 8-health wolves. Wolves attack enemies you hit. 5-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 2, "bardolf.wolf_pounce", Material.FEATHER,
                        "Wolf Pounce", "Leap into the air; your wolves follow and leap after you. Click or sneak. 7-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 3, "bardolf.full_moon", Material.NETHER_STAR,
                        "Full Moon", "For 10 seconds gain +1 attack damage and +5% movement speed per living wolf; wolves are healed and strengthened, then perish. 60-second cooldown.");
            }
            case "larissa" -> {
                ItemStack cannon = namedItem(Material.DIAMOND_HOE, ChatColor.GREEN + "Aqua Cannon",
                        "Fires a 10-block water beam for 3 damage. Consumes one Water Stone.");
                ItemMeta cannonMeta = cannon.getItemMeta();
                cannonMeta.setUnbreakable(true);
                cannon.setItemMeta(cannonMeta);
                player.getInventory().setItem(0, cannon);
                giveHeroesOfGwenSkillItem(player, 1, "larissa.dancing_fountain", Material.WATER_BUCKET,
                        "Dancing Fountain", "Throw a fountain that heals allies 4 HP/s and damages enemies 2 HP/s for 6 seconds in a 3-block radius. 14-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 2, "larissa.water_dash", Material.FEATHER,
                        "Water Dash", "Dash horizontally for 600 ms. Click or sneak to activate. 10-second cooldown.");
                giveHeroesOfGwenSkillItem(player, 3, "larissa.storm_blessing", Material.NETHER_STAR,
                        "Storm's Blessing", "Heal all teammates by 2 HP/s and grant Speed I for 7 seconds. Drop to activate; 60-second cooldown.");
                player.getInventory().setItem(7, namedItem(Material.LAPIS_LAZULI,
                        ChatColor.AQUA + "Water Stone", "Aqua Cannon ammunition; one charge replenishes every 3 seconds."));
                heroAmmoNextAt.put(id, System.currentTimeMillis()
                        + ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_AMMO_RELOAD_MILLIS);
            }
            case "devon" -> {
                ItemStack bow = new ItemStack(Material.BOW);
                ItemMeta bowMeta = bow.getItemMeta();
                bowMeta.setUnbreakable(true);
                bowMeta.setDisplayName(ChatColor.GREEN + "Bow");
                bow.setItemMeta(bowMeta);
                player.getInventory().setItem(0, bow);
                giveHeroesOfGwenSkillItem(player, 1, "devon.tnt_infusion", Material.TNT,
                        "TNT Infusion", "Your next three arrows explode on impact.");
                giveHeroesOfGwenSkillItem(player, 2, "devon.hunters_boost", Material.FEATHER,
                        "Hunters Boost", "Gain Speed II and Jump Boost I for three seconds.");
                giveHeroesOfGwenSkillItem(player, 3, "devon.infinity", Material.NETHER_STAR,
                        "Infinity", "Seven seconds of homing arrows and Wither.");
                player.getInventory().setItem(7, namedItem(Material.ARROW,
                        ChatColor.YELLOW + "Hunting Arrow", "Replenished every three seconds."));
                player.getInventory().getItem(7).setAmount(3);
                heroAmmoNextAt.put(id, System.currentTimeMillis() + 3_000L);
            }
            default -> {
                String role = ArcadePortRules.heroesOfGwenHeroRole(hero);
                Material primary = "Hunter".equals(role) ? Material.BOW
                        : "Mage".equals(role) ? Material.BLAZE_POWDER : Material.WOODEN_SWORD;
                player.getInventory().setItem(0, namedItem(primary,
                        ChatColor.GREEN + ArcadePortRules.heroesOfGwenHeroDisplayName(hero) + " Weapon",
                        "Hero abilities for this hero are still being ported."));
            }
        }
        player.updateInventory();
    }

    private void giveHeroesOfGwenSkillItem(Player player, int slot, String id, Material material,
            String displayName, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + displayName);
        meta.setLore(List.of(ChatColor.GRAY + description, ChatColor.YELLOW + "Right-click to activate."));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(heroSkillKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        player.getInventory().setItem(slot, item);
    }

    private String heroSkillId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(heroSkillKey, PersistentDataType.STRING);
    }

    private void setHeroSkillCooldown(Player player, String skill, long durationMillis) {
        heroSkillReadyAt.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(skill, System.currentTimeMillis() + durationMillis);
    }

    private boolean isHeroSkillReady(Player player, String skill) {
        return heroSkillReadyAt.getOrDefault(player.getUniqueId(), Map.of())
                .getOrDefault(skill, 0L) <= System.currentTimeMillis();
    }

    private void activateHeroesOfGwenSkill(Player player, String skill) {
        if (player == null || !player.isOnline() || !activePlayers.contains(player.getUniqueId())) return;
        if (!isHeroSkillReady(player, skill)) {
            long remain = heroSkillReadyAt.getOrDefault(player.getUniqueId(), Map.of())
                    .getOrDefault(skill, 0L) - System.currentTimeMillis();
            player.sendActionBar(ChatColor.RED + skill.substring(skill.indexOf('.') + 1).replace('_', ' ')
                    + " ready in " + Math.max(1L, (remain + 999L) / 1000L) + "s");
            return;
        }
        UUID id = player.getUniqueId();
        switch (skill) {
            case "hattori.shuriken" -> {
                setHeroSkillCooldown(player, skill,
                        ArcadePortRules.HEROES_OF_GWEN_HATTORI_SHURIKEN_COOLDOWN_MILLIS);
                for (int i = 0; i < ArcadePortRules.HEROES_OF_GWEN_HATTORI_SHURIKEN_COUNT; i++) {
                    final long delay = i * ArcadePortRules.HEROES_OF_GWEN_HATTORI_SHURIKEN_SPACING_TICKS;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || !activePlayers.contains(id)) return;
                        Snowball snowball = player.launchProjectile(Snowball.class);
                        markHeroProjectile(snowball, player, "hattori.shuriken");
                    }, delay);
                }
                player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0F, 1.3F);
            }
            case "hattori.ninja_dash" -> {
                setHeroSkillCooldown(player, skill,
                        ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_COOLDOWN_MILLIS);
                performHattoriDash(player);
            }
            case "hattori.ender_blade" -> {
                ItemStack sword = player.getInventory().getItem(0);
                heroEnderBladeOriginalSword.put(id, sword == null ? null : sword.clone());
                ItemStack enderBlade = namedItem(Material.DIAMOND_SWORD,
                        ChatColor.GREEN + "ENDER BLADE", "Hattori ultimate weapon.");
                ItemMeta meta = enderBlade.getItemMeta();
                meta.setUnbreakable(true);
                enderBlade.setItemMeta(meta);
                player.getInventory().setItem(0, enderBlade);
                player.getInventory().setHeldItemSlot(0);
                heroEnderBladeEndsAt.put(id, System.currentTimeMillis()
                        + ArcadePortRules.HEROES_OF_GWEN_HATTORI_ENDER_BLADE_ACTIVE_MILLIS);
                setHeroSkillCooldown(player, skill,
                        ArcadePortRules.HEROES_OF_GWEN_HATTORI_ENDER_BLADE_REUSE_DELAY_MILLIS);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Hattori activated Ender Blade!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
            }
            case "devon.tnt_infusion" -> {
                heroTntArrowCharges.put(id, ArcadePortRules.HEROES_OF_GWEN_DEVON_TNT_ARROW_COUNT);
                ItemStack skillItem = player.getInventory().getItem(1);
                if (skillItem != null) skillItem.setAmount(ArcadePortRules.HEROES_OF_GWEN_DEVON_TNT_ARROW_COUNT);
                player.sendMessage(ChatColor.GOLD + "Your next three arrows are infused with TNT.");
                player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.8F, 1.2F);
            }
            case "devon.hunters_boost" -> {
                setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_DEVON_HUNTERS_BOOST_COOLDOWN_MILLIS);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                        (int) (ArcadePortRules.HEROES_OF_GWEN_DEVON_HUNTERS_BOOST_DURATION_MILLIS / 50L),
                        1, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,
                        (int) (ArcadePortRules.HEROES_OF_GWEN_DEVON_HUNTERS_BOOST_DURATION_MILLIS / 50L),
                        0, true, true, true));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.5F);
            }
            case "devon.infinity" -> activateDevonInfinity(player);
            case "anath.burn_beam" -> activateAnathBurnBeam(player);
            case "anath.flame_dash" -> activateAnathFlameDash(player);
            case "anath.meteor_shower" -> launchAnathMeteor(player);
            case "larissa.dancing_fountain" -> launchLarissaFountain(player);
            case "larissa.water_dash" -> activateLarissaWaterDash(player);
            case "larissa.storm_blessing" -> activateLarissaStormBlessing(player);
            case "bardolf.summon_wolf" -> summonBardolfWolf(player);
            case "bardolf.wolf_pounce" -> activateBardolfPounce(player);
            case "bardolf.full_moon" -> activateBardolfFullMoon(player);
            case "rowena.light_arrows" -> activateRowenaLightArrows(player);
            case "rowena.combat_slide" -> activateRowenaCombatSlide(player);
            case "rowena.bombardment" -> beginRowenaBombardment(player);
            case "dana.pulse_heal" -> activateDanaPulseHeal(player);
            case "dana.knock_up" -> activateDanaKnockUp(player);
            case "dana.rally" -> beginDanaRally(player);
            case "biff.tether" -> activateBiffTether(player);
            case "biff.battle_leap" -> activateBiffBattleLeap(player);
            case "biff.cavalry_charge" -> activateBiffCavalryCharge(player);
            case "ivy.vine_pull" -> activateIvyVinePull(player);
            case "ivy.floral_leap" -> activateIvyFloralLeap(player);
            case "ivy.thorn_wall" -> activateIvyThornWall(player);
            default -> player.sendMessage(ChatColor.YELLOW + "That hero ability is not yet available.");
        }
    }


    private void fireAnathFlameWand(Player player) {
        UUID id = player.getUniqueId();
        String skill = "anath.flame_wand";
        if (!isHeroSkillReady(player, skill)) return;
        ItemStack ammo = player.getInventory().getItem(7);
        if (ammo == null || ammo.getType() != Material.BLAZE_POWDER
                || ammo.getAmount() <= 0 || !ammo.hasItemMeta()
                || !ammo.getItemMeta().hasDisplayName()
                || !ammo.getItemMeta().getDisplayName().contains("Embers")) {
            player.sendActionBar(ChatColor.RED + "Flame Wand needs an Ember charge.");
            return;
        }
        if (ammo.getAmount() == 1) player.getInventory().setItem(7, null);
        else ammo.setAmount(ammo.getAmount() - 1);

        Vector direction = player.getLocation().getDirection().normalize();
        Location origin = player.getEyeLocation().clone()
                .add(direction.clone().multiply(0.8D));
        Item ember = world.dropItem(origin, new ItemStack(Material.BLAZE_POWDER));
        ember.setPickupDelay(Integer.MAX_VALUE);
        ember.setGravity(false);
        ember.setVelocity(direction.clone().multiply(
                ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_PROJECTILE_SPEED));
        entities.add(ember);
        anathFlameEmbers.put(ember.getUniqueId(), new AnathFlameEmberState(
                id, ember.getLocation().clone(), direction,
                System.currentTimeMillis() + 3_000L));
        setHeroSkillCooldown(player, skill,
                ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_FIRE_INTERVAL_MILLIS);
        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.65F, 1.4F);
        player.sendActionBar(ChatColor.GOLD + "Embers: "
                + Math.max(0, ammo.getAmount()));
    }

    private void activateAnathBurnBeam(Player player) {
        UUID id = player.getUniqueId();
        String skill = "anath.burn_beam";
        if (!isHeroSkillReady(player, skill)) {
            player.sendActionBar(ChatColor.RED + "Burn Beam is recharging.");
            return;
        }
        Vector direction = player.getLocation().getDirection();
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.001D) direction = new Vector(0.0D, 0.0D, 1.0D);
        direction.normalize();
        Location origin = player.getLocation().clone().add(direction.clone());
        anathBurnBeams.put(id, new AnathBurnBeamState(origin, direction, System.currentTimeMillis()));
        setHeroSkillCooldown(player, skill,
                ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0F, 0.65F);
        player.sendActionBar(ChatColor.GOLD + "Burn Beam!");
    }

    private void activateAnathFlameDash(Player player) {
        UUID id = player.getUniqueId();
        if (!isHeroSkillReady(player, "anath.flame_dash")
                || anathFlameDashEndsAt.containsKey(id)) return;
        long now = System.currentTimeMillis();
        anathFlameDashEndsAt.put(id, now
                + ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_DURATION_MILLIS);
        setHeroSkillCooldown(player, "anath.flame_dash",
                ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_COOLDOWN_MILLIS);
        Vector direction = player.getLocation().getDirection();
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.001D) direction = new Vector(0.0D, 0.0D, 1.0D);
        player.setVelocity(direction.normalize()
                .multiply(ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_SPEED));
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.1F);
    }

    private void updateAnathAbilities(long now) {
        for (Map.Entry<UUID, AnathFlameEmberState> entry
                : new HashMap<>(anathFlameEmbers).entrySet()) {
            UUID entityId = entry.getKey();
            AnathFlameEmberState state = entry.getValue();
            Entity entity = Bukkit.getEntity(entityId);
            if (!(entity instanceof Item ember) || !ember.isValid()
                    || now >= state.expiresAt
                    || ember.getLocation().distanceSquared(state.origin)
                            >= ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_MAX_RANGE
                                    * ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_MAX_RANGE) {
                removeAnathEmber(entityId);
                continue;
            }
            Player owner = players.get(state.ownerId);
            if (owner == null || !owner.isOnline() || !activePlayers.contains(state.ownerId)) {
                removeAnathEmber(entityId);
                continue;
            }
            ember.setVelocity(state.direction.clone()
                    .multiply(ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_PROJECTILE_SPEED));
            Location point = ember.getLocation();
            world.spawnParticle(Particle.FLAME, point, 3, 0.08D, 0.08D, 0.08D, 0.015D);
            world.spawnParticle(Particle.SMOKE, point, 1, 0.03D, 0.03D, 0.03D, 0.005D);
            int ownerTeam = teams.getOrDefault(state.ownerId, -1);
            double radius = ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_HIT_RADIUS;
            boolean hit = false;
            for (Entity target : world.getNearbyEntities(point, radius, radius, radius)) {
                if (target.equals(ember) || target.equals(owner) || !target.isValid()) continue;
                TowerState tower = towerFor(target);
                if (tower != null) {
                    if (tower.owner != ownerTeam && canDamageMobaTower(tower, ownerTeam)
                            && owner.getLocation().distanceSquared(tower.location) <= MOBA_TOWER_RANGE_SQUARED) {
                        damageMobaTower(tower,
                                ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_DAMAGE, owner);
                        hit = true;
                        break;
                    }
                    continue;
                }
                Integer minionTeam = heroesOfGwenMinionTeams.get(target.getUniqueId());
                if (minionTeam != null && target instanceof Mob minion) {
                    if (minionTeam == ownerTeam) continue;
                    minion.damage(ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_DAMAGE, owner);
                    minion.setFireTicks(Math.max(minion.getFireTicks(),
                            ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_FIRE_TICKS));
                    hit = true;
                    break;
                }
                if (target instanceof Player victim && activePlayers.contains(victim.getUniqueId())) {
                    if (teams.getOrDefault(victim.getUniqueId(), -1) == ownerTeam) continue;
                    victim.damage(ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_DAMAGE, owner);
                    victim.setFireTicks(Math.max(victim.getFireTicks(),
                            ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_FIRE_TICKS));
                    hit = true;
                    break;
                }
                if (target instanceof Wither wither) {
                    Integer enemyTeam = withers.entrySet().stream()
                            .filter(witherEntry -> witherEntry.getValue().equals(wither))
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                    if (enemyTeam == null || enemyTeam == ownerTeam
                            || !witherDamageable.getOrDefault(enemyTeam, false)) continue;
                    double damage = ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_DAMAGE;
                    if (wither.getHealth() <= damage) {
                        finishHeroesOfGwen(ownerTeam, "enemy Wither defeated by Anath's Flame Wand");
                        removeAnathEmber(entityId);
                        return;
                    }
                    wither.setHealth(wither.getHealth() - damage);
                    wither.setFireTicks(Math.max(wither.getFireTicks(),
                            ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_FIRE_TICKS));
                    hit = true;
                    break;
                }
            }
            if (hit) removeAnathEmber(entityId);
        }

        for (Map.Entry<UUID, AnathBurnBeamState> entry
                : new HashMap<>(anathBurnBeams).entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = players.get(ownerId);
            AnathBurnBeamState state = entry.getValue();
            if (owner == null || !owner.isOnline() || !activePlayers.contains(ownerId)
                    || !"anath".equals(selectedHeroesOfGwen.get(ownerId))) {
                anathBurnBeams.remove(ownerId);
                continue;
            }
            Map<UUID, Long> targetCooldowns = anathBurnBeamNextHitAt.computeIfAbsent(ownerId,
                    ignored -> new HashMap<>());
            int team = teams.getOrDefault(ownerId, -1);
            boolean finished = false;
            for (int step = 0; step < ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_STEPS_PER_TICK; step++) {
                state.distance += ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_STEP;
                if (state.distance > ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_RANGE) {
                    finished = true;
                    break;
                }
                Location center = state.origin.clone().add(
                        state.direction.clone().multiply(state.distance));
                Location flameColumn = center.clone().add(0.0D, 5.0D, 0.0D);
                world.spawnParticle(Particle.FLAME, flameColumn, 30,
                        0.4D, 5.0D, 0.4D, 0.05D);
                if (Math.random() < 0.1D) {
                    world.playSound(center, Sound.ENTITY_GHAST_SHOOT, 0.45F, 0.7F);
                }
                double radius = ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_HIT_RADIUS;
                for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity target) || target.equals(owner)
                            || !target.isValid() || target.isDead()
                            || center.distanceSquared(target.getLocation()) > radius * radius
                            || targetCooldowns.getOrDefault(target.getUniqueId(), 0L) > now) continue;
                    if (target instanceof Player victim && !activePlayers.contains(victim.getUniqueId())) continue;
                    int targetTeam = heroesOfGwenEntityTeam(target);
                    if (targetTeam >= 0 && targetTeam == team) continue;
                    targetCooldowns.put(target.getUniqueId(),
                            now + ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_TARGET_RECHARGE_MILLIS);
                    TowerState tower = towerFor(target);
                    if (tower != null) {
                        if (tower.owner != team && canDamageMobaTower(tower, team)
                                && owner.getLocation().distanceSquared(tower.location) <= MOBA_TOWER_RANGE_SQUARED) {
                            damageMobaTower(tower, ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_DAMAGE, owner);
                        }
                    } else if (target instanceof Wither wither) {
                        Integer enemyTeam = withers.entrySet().stream()
                                .filter(witherEntry -> witherEntry.getValue().equals(wither))
                                .map(Map.Entry::getKey).findFirst().orElse(null);
                        if (enemyTeam != null && enemyTeam != team
                                && witherDamageable.getOrDefault(enemyTeam, false)) {
                            double damage = ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_DAMAGE;
                            if (wither.getHealth() <= damage) {
                                finishHeroesOfGwen(team, "enemy Wither defeated by Anath's Burn Beam");
                                anathBurnBeams.remove(ownerId);
                                return;
                            }
                            wither.setHealth(wither.getHealth() - damage);
                        }
                    } else {
                        target.damage(ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_DAMAGE, owner);
                    }
                    world.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.35F, 0.8F);
                }
            }
            if (finished) anathBurnBeams.remove(ownerId);
        }

        for (Map.Entry<UUID, Long> entry : new HashMap<>(anathFlameDashEndsAt).entrySet()) {
            UUID ownerId = entry.getKey();
            Player player = players.get(ownerId);
            if (now >= entry.getValue() || player == null || !player.isOnline()
                    || !activePlayers.contains(ownerId)
                    || !"anath".equals(selectedHeroesOfGwen.get(ownerId))) {
                if (player != null && player.isOnline()) player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
                anathFlameDashEndsAt.remove(ownerId);
                continue;
            }
            Vector direction = player.getLocation().getDirection();
            direction.setY(0.0D);
            if (direction.lengthSquared() > 0.001D) {
                player.setVelocity(direction.normalize()
                        .multiply(ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_SPEED));
            }
            Block ground = player.getLocation().getBlock();
            int safety = 0;
            while (!ground.getType().isSolid() && safety++ < 32
                    && ground.getY() > world.getMinHeight()) {
                ground = ground.getRelative(BlockFace.DOWN);
            }
            if (ground.getType().isSolid()) {
                Block fireBlock = ground.getRelative(BlockFace.UP);
                if (fireBlock.isPassable() && !anathFireTrailBlocks.containsKey(fireBlock)) {
                    anathFireTrailBlocks.put(fireBlock, new AnathFireTrailState(
                            fireBlock.getBlockData().clone(),
                            now + ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_FIRE_DELAY_MILLIS));
                }
            }
        }

        for (Map.Entry<Block, AnathFireTrailState> entry
                : new HashMap<>(anathFireTrailBlocks).entrySet()) {
            Block block = entry.getKey();
            AnathFireTrailState state = entry.getValue();
            if (!state.placed && now >= state.placeAt) {
                if (block.isPassable()) {
                    block.setType(Material.FIRE, false);
                    state.placed = true;
                    state.expiresAt = now + ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_FIRE_DURATION_MILLIS;
                } else {
                    anathFireTrailBlocks.remove(block);
                }
            } else if (state.placed && now >= state.expiresAt) {
                if (block.getType() == Material.FIRE) block.setBlockData(state.originalData, false);
                anathFireTrailBlocks.remove(block);
            }
        }

        for (UUID id : new ArrayList<>(activePlayers)) {
            if (!"anath".equals(selectedHeroesOfGwen.get(id))) continue;
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            long nextAmmo = heroAmmoNextAt.getOrDefault(id,
                    now + ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_AMMO_RELOAD_MILLIS);
            if (now >= nextAmmo) {
                ItemStack ammo = player.getInventory().getItem(7);
                int count = ammo != null && ammo.getType() == Material.BLAZE_POWDER
                        && ammo.hasItemMeta() && ammo.getItemMeta().hasDisplayName()
                        && ammo.getItemMeta().getDisplayName().contains("Embers") ? ammo.getAmount() : 0;
                if (count < ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_AMMO_CAPACITY) {
                    if (count <= 0) {
                        player.getInventory().setItem(7, namedItem(Material.BLAZE_POWDER,
                                ChatColor.GOLD + "Embers", "Flame Wand ammunition."));
                    } else {
                        ammo.setAmount(count + 1);
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.45F, 1.4F);
                }
                heroAmmoNextAt.put(id,
                        now + ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_AMMO_RELOAD_MILLIS);
            }
        }
    }

    private void removeAnathEmber(UUID entityId) {
        AnathFlameEmberState state = anathFlameEmbers.remove(entityId);
        Entity entity = Bukkit.getEntity(entityId);
        if (entity != null) {
            entities.remove(entity);
            entity.remove();
        }
    }

    private void removeAnathState(UUID ownerId) {
        anathBurnBeams.remove(ownerId);
        anathBurnBeamNextHitAt.remove(ownerId);
        anathFlameDashEndsAt.remove(ownerId);
        for (Map.Entry<UUID, AnathFlameEmberState> entry
                : new HashMap<>(anathFlameEmbers).entrySet()) {
            if (entry.getValue().ownerId.equals(ownerId)) removeAnathEmber(entry.getKey());
        }
    }

    private void launchAnathMeteor(Player player) {
        UUID id = player.getUniqueId();
        String skill = "anath.meteor_shower";
        if (!isHeroSkillReady(player, skill)) {
            player.sendActionBar(ChatColor.RED + "Meteor Shower is recharging.");
            return;
        }
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(Material.NETHERRACK));
        projectile.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.2D));
        markHeroProjectile(projectile, player, "anath.meteor");
        entities.add(projectile);
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.9F, 0.9F);
        player.sendActionBar(ChatColor.GOLD + "Meteor Shower launched!");
    }

    private void startAnathMeteorZone(Player owner, Location center, long now) {
        if (owner == null || center == null || center.getWorld() != world) return;
        AnathMeteorZone zone = new AnathMeteorZone(owner.getUniqueId(),
                teams.getOrDefault(owner.getUniqueId(), -1), center,
                now + ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_DURATION_MILLIS, now);
        anathMeteorZones.add(zone);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.9F, 0.65F);
        playerMeteorMarker(zone, owner);
    }

    private void playerMeteorMarker(AnathMeteorZone zone, Player owner) {
        Color colour = zone.team == 0 ? Color.fromRGB(255, 65, 65) : Color.fromRGB(75, 175, 255);
        Particle.DustOptions dust = new Particle.DustOptions(colour, 1.3F);
        for (int angle = 0; angle < 360; angle += 6) {
            double radians = Math.toRadians(angle);
            Location point = zone.center.clone().add(
                    Math.cos(radians) * ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_RADIUS,
                    0.15D,
                    Math.sin(radians) * ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_RADIUS);
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust);
        }
        world.spawnParticle(Particle.FLAME, zone.center, 24, 0.8D, 0.15D, 0.8D, 0.02D);
        world.playSound(zone.center, Sound.BLOCK_BEACON_ACTIVATE, 0.7F, 1.1F);
    }

    private void updateAnathMeteorZones(long now) {
        for (AnathMeteorZone zone : new ArrayList<>(anathMeteorZones)) {
            if (now >= zone.expiresAt) {
                anathMeteorZones.remove(zone);
                world.spawnParticle(Particle.SMOKE, zone.center, 28, 2.0D, 0.2D, 2.0D, 0.02D);
                continue;
            }
            Player owner = players.get(zone.ownerId);
            if (owner == null || !owner.isOnline()) {
                anathMeteorZones.remove(zone);
                continue;
            }
            if (now >= zone.nextVisualAt) {
                zone.nextVisualAt = now + 500L;
                playerMeteorMarker(zone, owner);
                for (int i = 0; i < 5; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
                    double radius = ThreadLocalRandom.current().nextDouble(0.0D,
                            ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_RADIUS);
                    Location sky = zone.center.clone().add(Math.cos(angle) * radius, 12.0D,
                            Math.sin(angle) * radius);
                    world.spawnParticle(Particle.FLAME, sky, 2, 0.2D, 0.1D, 0.2D, 0.02D);
                }
            }
            if (now >= zone.nextBurnAt) {
                zone.nextBurnAt = now + ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_BURN_INTERVAL_MILLIS;
                for (Player target : activeMatchPlayers()) {
                    if (teams.getOrDefault(target.getUniqueId(), -1) == zone.team
                            || !ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(
                                    target.getLocation().distance(zone.center))) continue;
                    target.setFireTicks(Math.max(target.getFireTicks(), 30));
                    applyAnathMeteorPlayerDamage(owner, target,
                            ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_BURN_DAMAGE);
                    if (!activePlayers.contains(target.getUniqueId())) continue;
                    if (owner.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                        Vector knockback = target.getLocation().toVector().subtract(zone.center.toVector());
                        if (knockback.lengthSquared() > 0.001D) {
                            target.setVelocity(knockback.normalize().multiply(0.12D).setY(0.05D));
                        }
                    }
                }
                for (Mob minion : liveHeroesOfGwenMinions()) {
                    if (heroesOfGwenMinionTeams.getOrDefault(minion.getUniqueId(), -1) == zone.team
                            || !ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(
                                    minion.getLocation().distance(zone.center))) continue;
                    minion.setFireTicks(Math.max(minion.getFireTicks(), 60));
                }
            }
            if (now >= zone.nextPulseAt) {
                zone.nextPulseAt = now + 1_000L;
                for (Player target : activeMatchPlayers()) {
                    if (teams.getOrDefault(target.getUniqueId(), -1) == zone.team
                            || !ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(
                                    target.getLocation().distance(zone.center))) continue;
                    applyAnathMeteorPlayerDamage(owner, target,
                            ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_PULSE_DAMAGE);
                    if (!activePlayers.contains(target.getUniqueId())) continue;
                }
                for (Map.Entry<Integer, Wither> entry : withers.entrySet()) {
                    if (entry.getKey() == zone.team || entry.getValue() == null || !entry.getValue().isValid()
                            || !ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(
                                    entry.getValue().getLocation().distance(zone.center))) continue;
                    Wither wither = entry.getValue();
                    if (wither.getHealth() <= ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_PULSE_DAMAGE) {
                        finishHeroesOfGwen(zone.team, "enemy Wither defeated by Meteor Shower");
                        anathMeteorZones.remove(zone);
                        return;
                    }
                    wither.setHealth(wither.getHealth() - ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_PULSE_DAMAGE);
                }
                List<Player> candidates = activeMatchPlayers().stream()
                        .filter(target -> teams.getOrDefault(target.getUniqueId(), -1) != zone.team)
                        .filter(target -> ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(
                                target.getLocation().distance(zone.center)))
                        .toList();
                for (Player target : candidates) {
                    Location spawn = target.getLocation().clone().add(
                            ThreadLocalRandom.current().nextDouble(-2.0D, 2.0D), 12.0D,
                            ThreadLocalRandom.current().nextDouble(-2.0D, 2.0D));
                    SmallFireball fireball = world.spawn(spawn, SmallFireball.class);
                    fireball.setShooter(owner);
                    fireball.setIsIncendiary(false);
                    fireball.setYield(0.0F);
                    Vector direction = target.getLocation().toVector().subtract(spawn.toVector());
                    fireball.setVelocity(direction.normalize().multiply(1.3D));
                    markHeroProjectile(fireball, owner, "anath.meteor_fireball");
                    entities.add(fireball);
                }
            }
        }
    }

    private void applyAnathMeteorPlayerDamage(Player owner, Player target, double damage) {
        if (owner == null || !owner.isOnline() || target == null || !target.isOnline()
                || !activePlayers.contains(target.getUniqueId())
                || teams.getOrDefault(owner.getUniqueId(), -1) == teams.getOrDefault(target.getUniqueId(), -2)) return;
        target.damage(damage, owner);
    }


    private void performHattoriDash(Player player) {
        Location origin = player.getEyeLocation().clone();
        Vector direction = player.getLocation().getDirection().normalize();
        Location destination = player.getLocation().clone();
        Set<UUID> hit = new HashSet<>();
        double range = ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_RANGE;
        int team = teams.getOrDefault(player.getUniqueId(), -1);
        for (double distance = 0.4D; distance <= range; distance += 0.4D) {
            Location point = origin.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.CRIT, point, 3, 0.12D, 0.12D, 0.12D, 0.01D);
            double radius = ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_COLLISION_RADIUS;
            for (Entity entity : world.getNearbyEntities(point, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target) || target.equals(player)
                        || !target.isValid() || target.isDead()
                        || point.distanceSquared(target.getLocation()) > radius * radius
                        || !hit.add(target.getUniqueId())) continue;
                if (target instanceof Player victim && !activePlayers.contains(victim.getUniqueId())) continue;
                int targetTeam = heroesOfGwenEntityTeam(target);
                if (targetTeam >= 0 && targetTeam == team) continue;
                TowerState tower = towerFor(target);
                if (tower != null) {
                    if (tower.owner != team && canDamageMobaTower(tower, team)
                            && player.getLocation().distanceSquared(tower.location) <= MOBA_TOWER_RANGE_SQUARED) {
                        damageMobaTower(tower,
                                ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_DAMAGE, player);
                    }
                } else if (target instanceof Wither wither) {
                    Integer enemyTeam = withers.entrySet().stream()
                            .filter(witherEntry -> witherEntry.getValue().equals(wither))
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                    if (enemyTeam != null && enemyTeam != team
                            && witherDamageable.getOrDefault(enemyTeam, false)) {
                        double damage = ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_DAMAGE;
                        if (wither.getHealth() <= damage) {
                            finishHeroesOfGwen(team, "enemy Wither defeated by Hattori's Ninja Dash");
                            return;
                        }
                        wither.setHealth(wither.getHealth() - damage);
                    }
                } else {
                    target.damage(ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_DAMAGE, player);
                }
            }
            if (!point.getBlock().isPassable() && distance > 1.0D) break;
            destination = point.clone().subtract(0.0D, 1.6D, 0.0D);
        }
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());
        if (destination.getBlock().isPassable() && destination.clone().add(0, 1, 0).getBlock().isPassable()) {
            player.teleport(destination);
        } else {
            player.teleport(player.getLocation().clone().add(direction.multiply(1.0D)));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.2F);
    }

    private void activateDevonInfinity(Player player) {
        UUID id = player.getUniqueId();
        ItemStack bow = player.getInventory().getItem(0);
        if (bow == null || bow.getType() != Material.BOW) {
            player.sendMessage(ChatColor.RED + "You need your bow to activate Infinity.");
            return;
        }
        heroInfinityOriginalEnchant.put(id, bow.getEnchantmentLevel(Enchantment.INFINITY));
        if (!bow.containsEnchantment(Enchantment.INFINITY)) bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        ItemStack ammo = player.getInventory().getItem(7);
        if (ammo == null || ammo.getType() != Material.ARROW) {
            player.getInventory().setItem(7, namedItem(Material.ARROW, ChatColor.YELLOW + "Hunting Arrow", "Temporary Infinity ammunition."));
        } else if (ammo.getAmount() < 3) {
            ammo.setAmount(ammo.getAmount() + 1);
        }
        heroInfinityEndsAt.put(id, System.currentTimeMillis()
                + ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_ACTIVE_MILLIS);
        setHeroSkillCooldown(player, "devon.infinity",
                ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_REUSE_DELAY_MILLIS);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Infinity activated: homing arrows and Wither for seven seconds.");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
    }


    private void fireLarissaAquaCannon(Player player) {
        String skill = "larissa.aqua_cannon";
        if (!isHeroSkillReady(player, skill)) return;
        ItemStack ammo = player.getInventory().getItem(7);
        if (ammo == null || ammo.getType() != Material.LAPIS_LAZULI || ammo.getAmount() < 1) {
            player.sendActionBar(ChatColor.RED + "Aqua Cannon needs a Water Stone charge.");
            return;
        }
        if (ammo.getAmount() == 1) player.getInventory().setItem(7, null);
        else ammo.setAmount(ammo.getAmount() - 1);
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_INTERVAL_MILLIS);

        Vector direction = player.getLocation().getDirection().normalize();
        Location start = player.getLocation().clone().add(0.0D, 1.7D, 0.0D);
        Location impact = start.clone();
        int team = teams.getOrDefault(player.getUniqueId(), -1);
        boolean hit = false;
        for (double distance = 0.2D; distance <= ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_RANGE;
                distance += 0.2D) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            impact = point;
            world.spawnParticle(Particle.SPLASH, point, 3, 0.08D, 0.08D, 0.08D, 0.02D);

            for (Entity entity : world.getNearbyEntities(point, 2.0D, 2.0D, 2.0D)) {
                if (point.distanceSquared(entity.getLocation()) > 4.0D) continue;
                TowerState tower = towerFor(entity);
                if (tower != null) {
                    if (tower.owner != team && canDamageMobaTower(tower, team)
                            && player.getLocation().distanceSquared(tower.location)
                                    <= MOBA_TOWER_RANGE_SQUARED) {
                        damageMobaTower(tower, ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_DAMAGE, player);
                        hit = true;
                        break;
                    }
                    continue;
                }
                Integer minionTeam = heroesOfGwenMinionTeams.get(entity.getUniqueId());
                if (minionTeam != null) {
                    if (minionTeam != team && entity instanceof Mob minion) {
                        minion.damage(ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_DAMAGE, player);
                        hit = true;
                        break;
                    }
                    continue;
                }
                if (entity instanceof Player target && !target.equals(player)
                        && activePlayers.contains(target.getUniqueId())) {
                    if (teams.getOrDefault(target.getUniqueId(), -1) == team) continue;
                    target.damage(ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_DAMAGE, player);
                    hit = true;
                    break;
                }
                if (entity instanceof Wither wither) {
                    Integer enemyTeam = withers.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(wither))
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                    if (enemyTeam != null && enemyTeam != team && witherDamageable.getOrDefault(enemyTeam, false)) {
                        if (wither.getHealth() <= ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_DAMAGE) {
                            finishHeroesOfGwen(team, "enemy Wither defeated by Larissa's Aqua Cannon");
                            return;
                        }
                        wither.setHealth(wither.getHealth()
                                - ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_DAMAGE);
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) break;
        }
        world.playSound(impact, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.8F, 1.4F);
        world.spawnParticle(Particle.SPLASH, impact, 10, 0.18D, 0.18D, 0.18D, 0.08D);
    }

    private void launchLarissaFountain(Player player) {
        String skill = "larissa.dancing_fountain";
        if (!isHeroSkillReady(player, skill)) return;
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(Material.WATER_BUCKET));
        projectile.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.15D));
        markHeroProjectile(projectile, player, skill);
        entities.add(projectile);
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 0.8F, 1.0F);
        player.sendActionBar(ChatColor.AQUA + "Dancing Fountain!");
    }

    private void startLarissaFountain(Player owner, Location center, long now) {
        if (owner == null || center == null || center.getWorld() != world) return;
        int team = teams.getOrDefault(owner.getUniqueId(), -1);
        if (team < 0) return;
        larissaFountainZones.add(new LarissaFountainZone(owner.getUniqueId(), team, center,
                now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_DURATION_MILLIS, now));
        world.playSound(center, Sound.BLOCK_WATER_AMBIENT, 0.9F, 1.1F);
        world.spawnParticle(Particle.SPLASH, center.clone().add(0.0D, 0.5D, 0.0D),
                24, 0.6D, 0.25D, 0.6D, 0.12D);
    }

    private void activateLarissaWaterDash(Player player) {
        String skill = "larissa.water_dash";
        if (!isHeroSkillReady(player, skill)) return;
        long now = System.currentTimeMillis();
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_DASH_COOLDOWN_MILLIS);
        larissaWaterDashEndsAt.put(player.getUniqueId(),
                now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_DASH_DURATION_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 0.8F, 1.25F);
    }

    private void activateLarissaStormBlessing(Player player) {
        String skill = "larissa.storm_blessing";
        if (!isHeroSkillReady(player, skill)) return;
        long now = System.currentTimeMillis();
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_REUSE_DELAY_MILLIS);
        long endsAt = now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_DURATION_MILLIS;
        larissaStormBlessingEndsAt.put(player.getUniqueId(), endsAt);
        larissaStormBlessingNextHealAt.put(player.getUniqueId(),
                now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_HEAL_INTERVAL_MILLIS);
        for (Player teammate : activeMatchPlayers()) {
            if (teams.getOrDefault(teammate.getUniqueId(), -1) == teams.getOrDefault(player.getUniqueId(), -2)) {
                teammate.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                        (int) (ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_DURATION_MILLIS / 50L),
                        0, true, true, true));
            }
        }
        player.sendMessage(ChatColor.AQUA + "Larissa has called Storm's Blessing!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.9F, 1.2F);
    }

    private void updateLarissaFountains(long now) {
        java.util.Iterator<LarissaFountainZone> iterator = larissaFountainZones.iterator();
        while (iterator.hasNext()) {
            LarissaFountainZone zone = iterator.next();
            Player owner = players.get(zone.ownerId);
            if (owner == null || !owner.isOnline() || !activePlayers.contains(zone.ownerId)) {
                iterator.remove();
                continue;
            }
            if (now >= zone.expiresAt) {
                world.spawnParticle(Particle.CLOUD, zone.center, 16, 0.8D, 0.25D, 0.8D, 0.02D);
                iterator.remove();
                continue;
            }
            if (now >= zone.nextPulseAt) {
                for (Entity entity : world.getNearbyEntities(zone.center,
                        ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_RADIUS,
                        ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_RADIUS,
                        ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_RADIUS)) {
                    if (zone.center.distanceSquared(entity.getLocation())
                            > ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_RADIUS
                                    * ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_RADIUS) continue;
                    if (entity instanceof Player target && activePlayers.contains(target.getUniqueId())) {
                        if (teams.getOrDefault(target.getUniqueId(), -1) == zone.team) {
                            target.setHealth(Math.min(target.getMaxHealth(), target.getHealth()
                                    + ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_HEAL_PER_SECOND));
                        } else {
                            target.damage(ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_DAMAGE_PER_SECOND,
                                    owner);
                        }
                        continue;
                    }
                    if (entity instanceof Wither wither) {
                        Integer enemyTeam = withers.entrySet().stream()
                                .filter(entry -> entry.getValue().equals(wither))
                                .map(Map.Entry::getKey).findFirst().orElse(null);
                        if (enemyTeam != null && enemyTeam != zone.team
                                && witherDamageable.getOrDefault(enemyTeam, false)) {
                            double damage = ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_DAMAGE_PER_SECOND;
                            if (wither.getHealth() <= damage) {
                                finishHeroesOfGwen(zone.team, "enemy Wither defeated by Larissa's Dancing Fountain");
                                return;
                            }
                            wither.setHealth(wither.getHealth() - damage);
                        }
                        continue;
                    }
                    Integer minionTeam = heroesOfGwenMinionTeams.get(entity.getUniqueId());
                    if (minionTeam != null && entity instanceof Mob minion) {
                        if (minionTeam == zone.team) {
                            var maxHealth = minion.getAttribute(Attribute.MAX_HEALTH);
                            double maximum = maxHealth == null ? 20.0D : maxHealth.getValue();
                            minion.setHealth(Math.min(maximum, minion.getHealth()
                                    + ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_HEAL_PER_SECOND));
                        } else {
                            minion.damage(ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_DAMAGE_PER_SECOND,
                                    owner);
                        }
                    }
                }
                zone.nextPulseAt = now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_PULSE_INTERVAL_MILLIS;
            }
            if (now >= zone.nextVisualAt) {
                Color colour = zone.team == 0 ? Color.fromRGB(255, 70, 70) : Color.fromRGB(70, 165, 255);
                Particle.DustOptions dust = new Particle.DustOptions(colour, 1.15F);
                for (int angle = 0; angle < 360; angle += 15) {
                    double radians = Math.toRadians(angle);
                    Location edge = zone.center.clone().add(
                            Math.cos(radians) * ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_RADIUS,
                            0.12D, Math.sin(radians) * ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_RADIUS);
                    world.spawnParticle(Particle.DUST, edge, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust);
                }
                world.spawnParticle(Particle.SPLASH, zone.center.clone().add(0.0D, 0.45D, 0.0D),
                        5, 0.65D, 0.3D, 0.65D, 0.04D);
                zone.nextVisualAt = now + 250L;
            }
        }
    }

    private void updateLarissaAbilities(long now) {
        for (Map.Entry<UUID, Long> entry : new HashMap<>(larissaWaterDashEndsAt).entrySet()) {
            UUID id = entry.getKey();
            Player player = players.get(id);
            if (now >= entry.getValue()) {
                if (player != null && player.isOnline()) player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
                larissaWaterDashEndsAt.remove(id);
                continue;
            }
            if (player == null || !player.isOnline() || !activePlayers.contains(id)) {
                larissaWaterDashEndsAt.remove(id);
                continue;
            }
            Vector direction = player.getLocation().getDirection();
            direction.setY(0.0D);
            if (direction.lengthSquared() < 0.001D) direction = new Vector(0.0D, 0.0D, 1.0D);
            player.setVelocity(direction.normalize());
            world.spawnParticle(Particle.DRIPPING_WATER, player.getLocation().add(0.0D, 1.0D, 0.0D),
                    4, 0.35D, 0.25D, 0.35D, 0.02D);
        }

        for (Map.Entry<UUID, Long> entry : new HashMap<>(larissaStormBlessingEndsAt).entrySet()) {
            UUID id = entry.getKey();
            if (now >= entry.getValue()) {
                larissaStormBlessingEndsAt.remove(id);
                larissaStormBlessingNextHealAt.remove(id);
                continue;
            }
            Player owner = players.get(id);
            if (owner == null || !owner.isOnline() || !activePlayers.contains(id)) {
                larissaStormBlessingEndsAt.remove(id);
                larissaStormBlessingNextHealAt.remove(id);
                continue;
            }
            long nextHeal = larissaStormBlessingNextHealAt.getOrDefault(id, now + 1_000L);
            if (now >= nextHeal) {
                int team = teams.getOrDefault(id, -1);
                for (Player teammate : activeMatchPlayers()) {
                    if (teams.getOrDefault(teammate.getUniqueId(), -1) != team) continue;
                    teammate.setHealth(Math.min(teammate.getMaxHealth(), teammate.getHealth()
                            + ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_HEAL_PER_SECOND));
                    Location visual = teammate.getLocation().clone().add(0.0D, 2.4D, 0.0D);
                    world.spawnParticle(Particle.CLOUD, visual, 6, 0.35D, 0.2D, 0.35D, 0.01D);
                    world.spawnParticle(Particle.DRIPPING_WATER, visual, 3, 0.3D, 0.15D, 0.3D, 0.03D);
                }
                larissaStormBlessingNextHealAt.put(id,
                        now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_HEAL_INTERVAL_MILLIS);
            }
        }
    }


    private List<Wolf> liveBardolfWolves(UUID ownerId) {
        List<Wolf> pack = bardolfWolves.computeIfAbsent(ownerId, ignored -> new ArrayList<>());
        pack.removeIf(wolf -> {
            boolean stale = wolf == null || !wolf.isValid() || wolf.isDead();
            if (stale && wolf != null) {
                entities.remove(wolf);
                bardolfWolfTargets.remove(wolf.getUniqueId());
            }
            return stale;
        });
        return pack;
    }

    private UUID bardolfOwnerId(Wolf wolf) {
        if (wolf == null) return null;
        String owner = wolf.getPersistentDataContainer().get(bardolfWolfOwnerKey, PersistentDataType.STRING);
        if (owner == null) return null;
        try {
            return UUID.fromString(owner);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private int heroesOfGwenEntityTeam(Entity entity) {
        if (entity instanceof Player player) return teams.getOrDefault(player.getUniqueId(), -1);
        if (entity == null) return -1;
        Integer minionTeam = heroesOfGwenMinionTeams.get(entity.getUniqueId());
        if (minionTeam != null) return minionTeam;
        TowerState tower = towerFor(entity);
        if (tower != null) return tower.owner;
        for (Map.Entry<Integer, Wither> entry : withers.entrySet()) {
            if (entry.getValue().equals(entity)) return entry.getKey();
        }
        if (entity instanceof Horse horse) {
            String owner = horse.getPersistentDataContainer().get(biffHorseOwnerKey, PersistentDataType.STRING);
            if (owner != null) {
                try { return teams.getOrDefault(UUID.fromString(owner), -1); }
                catch (IllegalArgumentException ignored) { return -1; }
            }
        }
        if (entity instanceof Wolf wolf) {
            UUID ownerId = bardolfOwnerId(wolf);
            if (ownerId != null) return teams.getOrDefault(ownerId, -1);
        }
        return -1;
    }

    private void summonBardolfWolf(Player player) {
        UUID ownerId = player.getUniqueId();
        if (bardolfFullMoonEndsAt.getOrDefault(ownerId, 0L) > System.currentTimeMillis()) {
            player.sendActionBar(ChatColor.RED + "You cannot summon wolves during Full Moon.");
            return;
        }
        List<Wolf> pack = liveBardolfWolves(ownerId);
        if (pack.size() >= ArcadePortRules.HEROES_OF_GWEN_BARDOLF_MAX_WOLVES) {
            player.sendActionBar(ChatColor.RED + "Your pack is already at its five-wolf limit.");
            return;
        }
        if (!isHeroSkillReady(player, "bardolf.summon_wolf")) return;

        Wolf wolf = world.spawn(player.getLocation(), Wolf.class);
        var maxHealth = wolf.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_HEALTH);
        wolf.setHealth(ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_HEALTH);
        wolf.setTamed(true);
        wolf.setOwner(player);
        wolf.setCollarColor(teams.getOrDefault(ownerId, 0) == 0 ? DyeColor.RED : DyeColor.BLUE);
        wolf.setRemoveWhenFarAway(false);
        wolf.setCustomName(teamColour(teams.getOrDefault(ownerId, 0)) + player.getName() + "'s Wolf");
        wolf.setCustomNameVisible(true);
        wolf.getPersistentDataContainer().set(bardolfWolfOwnerKey, PersistentDataType.STRING, ownerId.toString());
        pack.add(wolf);
        entities.add(wolf);
        setHeroSkillCooldown(player, "bardolf.summon_wolf",
                ArcadePortRules.HEROES_OF_GWEN_BARDOLF_SUMMON_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.9F, 1.2F);
        player.sendActionBar(ChatColor.GREEN + "Wolf summoned (" + pack.size() + "/"
                + ArcadePortRules.HEROES_OF_GWEN_BARDOLF_MAX_WOLVES + ").");
        updateBardolfSummonItem(player, pack.size());
    }

    private void updateBardolfSummonItem(Player player, int count) {
        ItemStack item = player.getInventory().getItem(1);
        if (item == null || !"bardolf.summon_wolf".equals(heroSkillId(item))) return;
        item.setAmount(Math.max(1, Math.min(ArcadePortRules.HEROES_OF_GWEN_BARDOLF_MAX_WOLVES, count)));
    }

    private void activateBardolfPounce(Player player) {
        String skill = "bardolf.wolf_pounce";
        if (!isHeroSkillReady(player, skill)) return;
        UUID ownerId = player.getUniqueId();
        Vector direction = player.getLocation().getDirection();
        direction.setY(Math.max(ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_MINIMUM_VERTICAL_VELOCITY,
                direction.getY()));
        direction.multiply(ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_VELOCITY_MULTIPLIER);
        Location target = player.getLocation().clone();
        bardolfPounces.put(ownerId, new BardolfPounceState(target, direction,
                System.currentTimeMillis() + ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_OVERRIDE_MILLIS));
        player.setVelocity(direction.clone());
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1.0F, 1.0F);
        world.spawnParticle(Particle.CLOUD, player.getLocation().clone().add(0.0D, 0.6D, 0.0D),
                15, 0.5D, 0.5D, 0.5D, 0.1D);
    }

    private void activateBardolfFullMoon(Player player) {
        UUID ownerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (!isHeroSkillReady(player, "bardolf.full_moon")) return;
        if (bardolfFullMoonEndsAt.containsKey(ownerId)) return;

        List<Wolf> pack = liveBardolfWolves(ownerId);
        float originalSpeed = player.getWalkSpeed();
        bardolfOriginalWalkSpeeds.put(ownerId, originalSpeed);
        double increase = pack.size() * ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_SPEED_PER_WOLF;
        player.setWalkSpeed((float) Math.min(1.0D, originalSpeed + increase));
        bardolfFullMoonEndsAt.put(ownerId,
                now + ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_DURATION_MILLIS);
        setHeroSkillCooldown(player, "bardolf.full_moon",
                ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_REUSE_DELAY_MILLIS);
        world.setTime(18_000L);
        world.strikeLightningEffect(player.getLocation());

        for (Wolf wolf : pack) {
            var maxHealth = wolf.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) maxHealth.setBaseValue(ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_WOLF_HEALTH);
            wolf.setHealth(ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_WOLF_HEALTH);
            wolf.setTamed(false);
            wolf.setAngry(true);
            wolf.getWorld().playSound(wolf.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0F, 1.0F);
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Full Moon! Your living wolves empower your attacks.");
    }

    private void endBardolfFullMoon(UUID ownerId) {
        Long end = bardolfFullMoonEndsAt.remove(ownerId);
        if (end == null) return;
        Player owner = players.get(ownerId);
        Float originalSpeed = bardolfOriginalWalkSpeeds.remove(ownerId);
        if (owner != null && owner.isOnline() && originalSpeed != null) {
            owner.setWalkSpeed(originalSpeed);
            ItemStack summon = owner.getInventory().getItem(1);
            if (summon != null && "bardolf.summon_wolf".equals(heroSkillId(summon))) {
                summon.setAmount(1);
            }
        }
        for (Wolf wolf : new ArrayList<>(bardolfWolves.getOrDefault(ownerId, List.of()))) {
            if (wolf != null && wolf.isValid()) wolf.remove();
            if (wolf != null) {
                entities.remove(wolf);
                bardolfWolfTargets.remove(wolf.getUniqueId());
            }
        }
        bardolfWolves.remove(ownerId);
        bardolfLastWolfAttackAt.remove(ownerId);
        bardolfPounces.remove(ownerId);
        world.setTime(12_000L);
    }

    private void removeBardolfPack(UUID ownerId) {
        if (ownerId == null) return;
        if (bardolfFullMoonEndsAt.containsKey(ownerId)) {
            Player owner = players.get(ownerId);
            Float originalSpeed = bardolfOriginalWalkSpeeds.remove(ownerId);
            if (owner != null && owner.isOnline() && originalSpeed != null) owner.setWalkSpeed(originalSpeed);
            bardolfFullMoonEndsAt.remove(ownerId);
            world.setTime(12_000L);
        } else {
            bardolfOriginalWalkSpeeds.remove(ownerId);
        }
        for (Wolf wolf : new ArrayList<>(bardolfWolves.getOrDefault(ownerId, List.of()))) {
            if (wolf != null && wolf.isValid()) wolf.remove();
            if (wolf != null) {
                entities.remove(wolf);
                bardolfWolfTargets.remove(wolf.getUniqueId());
            }
        }
        bardolfWolves.remove(ownerId);
        bardolfLastWolfAttackAt.remove(ownerId);
        bardolfPounces.remove(ownerId);
    }

    private void updateBardolfWolves(long now) {
        for (UUID ownerId : new ArrayList<>(bardolfWolves.keySet())) {
            Player owner = players.get(ownerId);
            if (owner == null || !owner.isOnline() || !activePlayers.contains(ownerId)
                    || !"bardolf".equals(selectedHeroesOfGwen.get(ownerId))) {
                removeBardolfPack(ownerId);
                continue;
            }
            List<Wolf> pack = liveBardolfWolves(ownerId);
            updateBardolfSummonItem(owner, pack.size());
            if (pack.isEmpty()) {
                bardolfWolfTargets.entrySet().removeIf(entry -> {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    return entity == null || !entity.isValid();
                });
                continue;
            }

            BardolfPounceState pounce = bardolfPounces.get(ownerId);
            if (pounce != null && now >= pounce.expiresAt) {
                bardolfPounces.remove(ownerId);
                pounce = null;
            }
            boolean fullMoon = bardolfFullMoonEndsAt.getOrDefault(ownerId, 0L) > now;
            for (Wolf wolf : pack) {
                UUID wolfId = wolf.getUniqueId();
                if (pounce != null && !pounce.leapedWolfIds.contains(wolfId)) {
                    if (wolf.getLocation().distanceSquared(pounce.target) <=
                            ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_TRIGGER_DISTANCE_SQUARED) {
                        pounce.leapedWolfIds.add(wolfId);
                        wolf.setVelocity(pounce.direction.clone());
                        wolf.getWorld().playSound(wolf.getLocation(), Sound.ENTITY_WOLF_WHINE, 1.0F, 1.5F);
                        continue;
                    }
                    moveBardolfWolf(wolf, pounce.target, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_FOLLOW_SPEED);
                    continue;
                }

                UUID targetId = bardolfWolfTargets.get(wolfId);
                Entity targetEntity = targetId == null ? null : Bukkit.getEntity(targetId);
                LivingEntity target = targetEntity instanceof LivingEntity living ? living : null;
                int ownerTeam = teams.getOrDefault(ownerId, -1);
                if (target == null || !target.isValid() || target.isDead()
                        || (target instanceof Player targetPlayer && !activePlayers.contains(targetPlayer.getUniqueId()))
                        || (heroesOfGwenEntityTeam(target) == ownerTeam)) {
                    bardolfWolfTargets.remove(wolfId);
                    target = null;
                }

                if (target != null) {
                    moveBardolfWolf(wolf, target.getLocation(),
                            fullMoon ? ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_ULTIMATE_SPEED
                                    : ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_FOLLOW_SPEED);
                    if (wolf.getLocation().distanceSquared(target.getLocation())
                            <= ArcadePortRules.HEROES_OF_GWEN_BARDOLF_ATTACK_DISTANCE_SQUARED) {
                        Map<UUID, Long> lastAttacks = bardolfLastWolfAttackAt.computeIfAbsent(ownerId,
                                ignored -> new HashMap<>());
                        long nextAllowed = lastAttacks.getOrDefault(target.getUniqueId(), 0L);
                        if (now >= nextAllowed) {
                            lastAttacks.put(target.getUniqueId(),
                                    now + ArcadePortRules.HEROES_OF_GWEN_BARDOLF_ATTACK_INTERVAL_MILLIS);
                            target.damage(ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_ATTACK_DAMAGE, owner);
                        }
                    }
                    continue;
                }

                double ownerDistance = wolf.getLocation().distanceSquared(owner.getLocation());
                if (ownerDistance > ArcadePortRules.HEROES_OF_GWEN_BARDOLF_OWNER_TELEPORT_DISTANCE_SQUARED) {
                    wolf.teleport(owner.getLocation());
                } else if (ownerDistance > ArcadePortRules.HEROES_OF_GWEN_BARDOLF_OWNER_FOLLOW_MIN_DISTANCE_SQUARED) {
                    moveBardolfWolf(wolf, owner.getLocation(),
                            fullMoon ? ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_ULTIMATE_SPEED
                                    : ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_FOLLOW_SPEED);
                }
            }
        }

        for (UUID ownerId : new ArrayList<>(bardolfFullMoonEndsAt.keySet())) {
            if (now >= bardolfFullMoonEndsAt.getOrDefault(ownerId, Long.MAX_VALUE)) endBardolfFullMoon(ownerId);
        }
    }

    private void moveBardolfWolf(Wolf wolf, Location target, double sourceSpeed) {
        Vector movement = target.toVector().subtract(wolf.getLocation().toVector());
        if (movement.lengthSquared() <= 0.01D) return;
        movement.setY(Math.max(-0.35D, Math.min(0.35D, movement.getY())));
        if (movement.lengthSquared() <= 0.001D) return;
        wolf.setVelocity(movement.normalize().multiply(sourceSpeed / 5.0D));
    }

    private void clearBardolfOwner(UUID ownerId) {
        removeBardolfPack(ownerId);
    }



    private void activateIvyVinePull(Player player) {
        UUID ownerId = player.getUniqueId();
        if (!isHeroSkillReady(player, "ivy.vine_pull") || ivyHooks.containsKey(ownerId)) return;
        Location origin = player.getEyeLocation().clone();
        Vector direction = player.getLocation().getDirection().normalize();
        ivyHooks.put(ownerId, new IvyHookState(origin, direction, System.currentTimeMillis()));
        setHeroSkillCooldown(player, "ivy.vine_pull",
                ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0F, 1.2F);
    }

    private void activateIvyFloralLeap(Player player) {
        String skill = "ivy.floral_leap";
        if (!isHeroSkillReady(player, skill) || ivyFloralLeaps.containsKey(player.getUniqueId())) return;
        Vector direction = player.getLocation().getDirection();
        direction.setY(ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_VERTICAL_VELOCITY);
        player.setVelocity(direction);
        ivyFloralLeaps.put(player.getUniqueId(), new IvyFloralLeapState(System.currentTimeMillis()));
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0F, 1.0F);
        world.spawnParticle(Particle.CLOUD, player.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                20, 0.5D, 0.5D, 0.5D, 0.08D);
    }

    private void activateIvyThornWall(Player player) {
        UUID ownerId = player.getUniqueId();
        if (!isHeroSkillReady(player, "ivy.thorn_wall") || ivyThornWalls.containsKey(ownerId)) return;
        Location floorCenter = player.getLocation().clone().subtract(0.0D, 1.0D, 0.0D);
        int team = teams.getOrDefault(ownerId, 0);
        IvyThornWallState state = new IvyThornWallState(player.getLocation().clone(),
                System.currentTimeMillis() + ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_DURATION_MILLIS);
        Material floorMaterial = team == 0 ? Material.RED_TERRACOTTA : Material.BLUE_TERRACOTTA;
        int radius = ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_BLOCK_RADIUS;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = floorCenter.clone().add(x, 0.0D, z).getBlock();
                if (block.getType().isAir() || block.isPassable()) continue;
                state.originalBlocks.put(block, block.getBlockData().clone());
                state.placedMaterials.put(block, floorMaterial);
                block.setType(floorMaterial, false);
            }
        }
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
                Block flower = floorCenter.clone().add(x, 1.0D, z).getBlock();
                if (!flower.isPassable()) continue;
                state.originalBlocks.put(flower, flower.getBlockData().clone());
                state.placedMaterials.put(flower, Material.POPPY);
                flower.setType(Material.POPPY, false);
            }
        }
        ivyThornWalls.put(ownerId, state);
        setHeroSkillCooldown(player, "ivy.thorn_wall",
                ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_COOLDOWN_MILLIS);
        world.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0F, 0.8F);
        player.sendMessage(ChatColor.GREEN + "Thorn Wall surrounds you!");
    }

    private void updateIvyAbilities(long now) {
        for (Map.Entry<UUID, IvyHookState> entry : new HashMap<>(ivyHooks).entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = players.get(ownerId);
            IvyHookState state = entry.getValue();
            if (owner == null || !owner.isOnline() || !activePlayers.contains(ownerId)
                    || !"ivy".equals(selectedHeroesOfGwen.get(ownerId))
                    || now - state.startedAt >= ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_MAX_PULL_MILLIS) {
                ivyHooks.remove(ownerId);
                continue;
            }
            if (state.targetId != null) {
                Player target = players.get(state.targetId);
                if (target == null || !target.isOnline() || !activePlayers.contains(state.targetId)
                        || target.isDead()
                        || owner.getLocation().distanceSquared(target.getLocation())
                                < ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_STOP_DISTANCE_SQUARED) {
                    if (target != null && target.isOnline()) target.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
                    ivyHooks.remove(ownerId);
                    continue;
                }
                Vector pull = owner.getLocation().toVector().subtract(target.getLocation().toVector());
                pull.setY(state.firstPull ? 0.3D : 0.0D);
                state.firstPull = true;
                if (pull.lengthSquared() > 0.001D) target.setVelocity(pull.normalize());
                Location link = target.getEyeLocation().clone().add(
                        owner.getEyeLocation().toVector().subtract(target.getEyeLocation().toVector()).multiply(0.5D));
                world.spawnParticle(Particle.HAPPY_VILLAGER, link, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                continue;
            }

            for (int step = 0; step < 10; step++) {
                state.distance += ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_BEAM_STEP;
                if (state.distance >= ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_RANGE) {
                    ivyHooks.remove(ownerId);
                    break;
                }
                Location point = state.origin.clone().add(
                        state.direction.clone().multiply(state.distance)).subtract(0.0D, 1.2D, 0.0D);
                world.spawnParticle(Particle.HAPPY_VILLAGER, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                double radius = ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_HIT_RADIUS;
                for (Entity entity : world.getNearbyEntities(point, radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity target) || target.equals(owner)
                            || !target.isValid() || target.isDead()
                            || point.distanceSquared(target.getLocation()) > radius * radius
                            || heroesOfGwenEntityTeam(target) == teams.getOrDefault(ownerId, -1)
                            || (target instanceof Player victim && !activePlayers.contains(victim.getUniqueId()))
                            || !state.hitEntities.add(target.getUniqueId())) continue;
                    if (target instanceof Player victim) {
                        state.targetId = victim.getUniqueId();
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                                ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_SLOW_TICKS,
                                ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_SLOW_AMPLIFIER,
                                false, false, true));
                        victim.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
                        victim.damage(ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_PLAYER_DAMAGE, owner);
                        victim.sendMessage(ChatColor.GREEN + owner.getName() + " has hooked you!");
                        break;
                    }
                    target.damage(ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_MINION_DAMAGE, owner);
                }
                if (state.targetId != null) break;
            }
        }

        for (Map.Entry<UUID, IvyFloralLeapState> entry : new HashMap<>(ivyFloralLeaps).entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = players.get(ownerId);
            IvyFloralLeapState state = entry.getValue();
            if (owner == null || !owner.isOnline() || !activePlayers.contains(ownerId)
                    || !"ivy".equals(selectedHeroesOfGwen.get(ownerId))) {
                restoreIvyBlocks(state.flowerBlocks, state.flowerMaterials);
                ivyFloralLeaps.remove(ownerId);
                continue;
            }
            if (!state.landed) {
                if (now - state.launchedAt < ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_MINIMUM_FLIGHT_MILLIS
                        || !owner.isOnGround()) continue;
                state.landed = true;
                state.center = owner.getLocation().clone();
                state.endsAt = now + ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_BED_DURATION_MILLIS;
                state.nextDamageAt = now + ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_DAMAGE_INTERVAL_MILLIS;
                Material[] flowers = {Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
                        Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
                        Material.WHITE_TULIP, Material.PINK_TULIP};
                int radius = ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_FLOWER_RADIUS;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + z * z > radius * radius) continue;
                        Block flower = state.center.clone().add(x, 0.0D, z).getBlock();
                        Block ground = flower.getRelative(BlockFace.DOWN);
                        if (!flower.isPassable() || ground.getType().isAir() || ground.isPassable()) continue;
                        Material material = flowers[ThreadLocalRandom.current().nextInt(flowers.length)];
                        state.flowerBlocks.put(flower, flower.getBlockData().clone());
                        state.flowerMaterials.put(flower, material);
                        flower.setType(material, false);
                        world.playSound(flower.getLocation(), Sound.BLOCK_GRASS_PLACE, 0.25F, 1.2F);
                    }
                }
                owner.playSound(owner.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0F, 1.0F);
                continue;
            }
            if (now >= state.endsAt) {
                restoreIvyBlocks(state.flowerBlocks, state.flowerMaterials);
                ivyFloralLeaps.remove(ownerId);
                continue;
            }
            if (now >= state.nextDamageAt) {
                double radius = ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_DAMAGE_RADIUS;
                int team = teams.getOrDefault(ownerId, -1);
                for (Entity entity : world.getNearbyEntities(state.center, radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity target) || target.equals(owner)
                            || !target.isValid() || target.isDead()
                            || state.center.distanceSquared(target.getLocation()) > radius * radius
                            || heroesOfGwenEntityTeam(target) == team
                            || (target instanceof Player victim && !activePlayers.contains(victim.getUniqueId()))) continue;
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                            ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_SLOW_TICKS,
                            ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_SLOW_AMPLIFIER,
                            false, false, true));
                    target.damage(ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_DAMAGE, owner);
                }
                state.nextDamageAt = now + ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_DAMAGE_INTERVAL_MILLIS;
            }
            for (int i = 0; i < 8; i++) {
                Location particle = state.center.clone().add(
                        ThreadLocalRandom.current().nextDouble(-0.4D, 0.4D),
                        0.1D + ThreadLocalRandom.current().nextDouble(0.0D, 0.8D),
                        ThreadLocalRandom.current().nextDouble(-0.4D, 0.4D));
                world.spawnParticle(Particle.HAPPY_VILLAGER, particle, 1, 0.0D, 0.0D, 0.0D, 0.01D);
            }
        }

        for (Map.Entry<UUID, IvyThornWallState> entry : new HashMap<>(ivyThornWalls).entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = players.get(ownerId);
            IvyThornWallState state = entry.getValue();
            if (owner == null || !owner.isOnline() || !activePlayers.contains(ownerId)
                    || !"ivy".equals(selectedHeroesOfGwen.get(ownerId)) || now >= state.endsAt) {
                restoreIvyBlocks(state.originalBlocks, state.placedMaterials);
                ivyThornWalls.remove(ownerId);
                continue;
            }
            double radius = ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_BLOCK_RADIUS;
            int team = teams.getOrDefault(ownerId, -1);
            for (Player target : players.values()) {
                if (!target.isOnline() || !activePlayers.contains(target.getUniqueId())
                        || target.getLocation().distanceSquared(state.center) > radius * radius) continue;
                if (teams.getOrDefault(target.getUniqueId(), -2) == team) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                            (int) ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ALLY_REGEN_TICKS,
                            ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ALLY_REGEN_AMPLIFIER,
                            false, false, true));
                    continue;
                }
                long nextRoot = state.nextRootAt.getOrDefault(target.getUniqueId(), 0L);
                if (now >= nextRoot) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                            (int) (ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ENEMY_ROOT_MILLIS / 50L),
                            10, true, true, true));
                    state.nextRootAt.put(target.getUniqueId(),
                            now + ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ENEMY_ROOT_RECHARGE_MILLIS);
                } else {
                    long nextDamage = state.nextDamageAt.getOrDefault(target.getUniqueId(), 0L);
                    if (now >= nextDamage) {
                        target.damage(ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ENEMY_DAMAGE, owner);
                        state.nextDamageAt.put(target.getUniqueId(),
                                now + ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ENEMY_DAMAGE_RECHARGE_MILLIS);
                    }
                }
            }
            for (int angle = 0; angle < 360; angle += 30) {
                double radians = Math.toRadians(angle);
                Location edge = state.center.clone().add(
                        Math.cos(radians) * radius, 0.15D, Math.sin(radians) * radius);
                world.spawnParticle(Particle.CRIT, edge, 2, 0.1D, 0.08D, 0.1D, 0.02D);
            }
        }
    }

    private void restoreIvyBlocks(Map<Block, BlockData> originals, Map<Block, Material> placed) {
        for (Map.Entry<Block, BlockData> entry : originals.entrySet()) {
            Block block = entry.getKey();
            if (block == null || block.getWorld() != world) continue;
            Material expected = placed.get(block);
            if (expected != null && block.getType() == expected) {
                block.setBlockData(entry.getValue(), false);
            }
        }
        originals.clear();
        placed.clear();
    }

    private void removeIvyState(UUID ownerId) {
        ivyHooks.remove(ownerId);
        IvyFloralLeapState floral = ivyFloralLeaps.remove(ownerId);
        if (floral != null) restoreIvyBlocks(floral.flowerBlocks, floral.flowerMaterials);
        IvyThornWallState wall = ivyThornWalls.remove(ownerId);
        if (wall != null) restoreIvyBlocks(wall.originalBlocks, wall.placedMaterials);
    }

    private boolean isRowenaBombardmentItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(rowenaBombardmentItemKey, PersistentDataType.BYTE);
    }

    private ItemStack rowenaBombardmentActiveItem() {
        ItemStack item = namedItem(Material.DIAMOND_HORSE_ARMOR,
                ChatColor.DARK_RED + "Bombardment", "Click to fire an explosive shot. Three shots remain in the flight.");
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(rowenaBombardmentItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void activateRowenaLightArrows(Player player) {
        UUID id = player.getUniqueId();
        if (!isHeroSkillReady(player, "rowena.light_arrows")) return;
        rowenaLightArrowsRemaining.put(id, ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_COUNT);
        ItemStack item = player.getInventory().getItem(1);
        if (item != null && "rowena.light_arrows".equals(heroSkillId(item))) {
            item.setAmount(ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_COUNT);
        }
        player.sendMessage(ChatColor.GOLD + "Your next five arrows are Light Arrows.");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.5F);
    }

    private void activateRowenaCombatSlide(Player player) {
        String skill = "rowena.combat_slide";
        if (!isHeroSkillReady(player, skill)) return;
        long now = System.currentTimeMillis();
        rowenaCombatSlideEndsAt.put(player.getUniqueId(),
                now + ArcadePortRules.HEROES_OF_GWEN_ROWENA_COMBAT_SLIDE_DURATION_MILLIS);
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_ROWENA_COMBAT_SLIDE_COOLDOWN_MILLIS);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0F, 1.0F);
        world.spawnParticle(Particle.CLOUD, player.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                20, 0.5D, 0.5D, 0.5D, 0.001D);
    }

    private void beginRowenaBombardment(Player player) {
        UUID id = player.getUniqueId();
        if (rowenaBombardments.containsKey(id) || !isHeroSkillReady(player, "rowena.bombardment")) return;
        Location flightLocation = player.getLocation().clone()
                .add(0.0D, ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_ASCENT_BLOCKS, 0.0D);
        Block anchorBlock = flightLocation.getBlock().getRelative(BlockFace.DOWN);
        BlockData originalBlock = anchorBlock.getBlockData().clone();
        ItemStack originalBow = player.getInventory().getItem(0) == null
                ? null : player.getInventory().getItem(0).clone();

        if (!player.teleport(flightLocation)) return;
        anchorBlock.setType(Material.BARRIER, false);
        RowenaBombardmentState state = new RowenaBombardmentState(anchorBlock, originalBlock,
                originalBow, System.currentTimeMillis()
                        + ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_DURATION_MILLIS,
                ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_SHOTS);
        rowenaBombardments.put(id, state);
        player.getInventory().setItem(0, rowenaBombardmentActiveItem());
        player.getInventory().setHeldItemSlot(0);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.sendMessage(ChatColor.GOLD + "Bombardment active: fire three explosive shots before landing.");
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 0.8F);
    }

    private void fireRowenaLightArrow(Player player) {
        ItemStack bow = player.getInventory().getItem(0);
        int power = bow == null ? 0 : bow.getEnchantmentLevel(Enchantment.POWER);
        double damage = ArcadePortRules.heroesOfGwenRowenaLightArrowDamage(power);
        Location origin = player.getEyeLocation().clone();
        Vector direction = player.getLocation().getDirection().normalize();
        int team = teams.getOrDefault(player.getUniqueId(), -1);
        Map<UUID, Long> nextHitAt = rowenaLightArrowNextHitAt.computeIfAbsent(player.getUniqueId(),
                ignored -> new HashMap<>());

        for (double distance = ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_STEP;
                distance <= ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_RANGE;
                distance += ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_STEP) {
            Location point = origin.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.FIREWORK, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            for (Entity entity : world.getNearbyEntities(point,
                    ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_HIT_RADIUS,
                    ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_HIT_RADIUS,
                    ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_HIT_RADIUS)) {
                if (!(entity instanceof LivingEntity target) || target.equals(player) || target.isDead()
                        || !target.isValid()) continue;
                int targetTeam = heroesOfGwenEntityTeam(target);
                if (targetTeam >= 0 && targetTeam == team) continue;
                if (target instanceof Player targetPlayer && !activePlayers.contains(targetPlayer.getUniqueId())) continue;
                if (entity instanceof Wolf wolf && teams.getOrDefault(
                        bardolfOwnerId(wolf), -2) == team) continue;
                if (nextHitAt.getOrDefault(target.getUniqueId(), 0L) > System.currentTimeMillis()) continue;
                nextHitAt.put(target.getUniqueId(), System.currentTimeMillis()
                        + ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_TARGET_RECHARGE_MILLIS);

                TowerState tower = towerFor(target);
                if (tower != null) {
                    if (tower.owner != team && canDamageMobaTower(tower, team)
                            && player.getLocation().distanceSquared(tower.location) <= MOBA_TOWER_RANGE_SQUARED) {
                        damageMobaTower(tower, damage, player);
                    }
                    continue;
                }
                if (target instanceof Wither wither) {
                    Integer enemyTeam = withers.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(wither))
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                    if (enemyTeam != null && enemyTeam != team && witherDamageable.getOrDefault(enemyTeam, false)) {
                        if (wither.getHealth() <= damage) {
                            finishHeroesOfGwen(team, "enemy Wither defeated by Rowena's Light Arrow");
                            return;
                        }
                        wither.setHealth(wither.getHealth() - damage);
                    }
                } else {
                    target.damage(damage, player);
                }
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.7F, 1.3F);
            }
        }
    }

    private void fireRowenaBombardmentShot(Player player) {
        UUID id = player.getUniqueId();
        RowenaBombardmentState state = rowenaBombardments.get(id);
        if (state == null || state.shotsRemaining <= 0) return;
        ItemStack bow = state.originalBow;
        int power = bow == null ? 0 : bow.getEnchantmentLevel(Enchantment.POWER);
        double damage = ArcadePortRules.heroesOfGwenRowenaBombardmentDamage(power);
        Location origin = player.getEyeLocation().clone();
        Vector direction = player.getLocation().getDirection().normalize();
        Location destination = origin.clone();
        for (double distance = ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_STEP;
                distance <= ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_RANGE;
                distance += ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_STEP) {
            Location point = origin.clone().add(direction.clone().multiply(distance));
            destination = point;
            if (!point.getBlock().isPassable()) break;
            world.spawnParticle(Particle.FIREWORK, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        world.playSound(destination, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
        world.spawnParticle(Particle.EXPLOSION, destination, 2, 0.2D, 0.2D, 0.2D, 0.02D);
        int team = teams.getOrDefault(id, -1);
        double radius = ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_BLAST_RADIUS;
        for (Entity entity : world.getNearbyEntities(destination, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || target.equals(player) || !target.isValid()
                    || target.isDead() || destination.distanceSquared(target.getLocation()) > radius * radius) continue;
            if (target instanceof Player targetPlayer && !activePlayers.contains(targetPlayer.getUniqueId())) continue;
            int targetTeam = heroesOfGwenEntityTeam(target);
            if (targetTeam >= 0 && targetTeam == team) continue;
            TowerState tower = towerFor(target);
            if (tower != null) {
                if (tower.owner != team && canDamageMobaTower(tower, team)
                        && player.getLocation().distanceSquared(tower.location) <= MOBA_TOWER_RANGE_SQUARED) {
                    damageMobaTower(tower, damage, player);
                }
            } else if (target instanceof Wither wither) {
                Integer enemyTeam = withers.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(wither))
                        .map(Map.Entry::getKey).findFirst().orElse(null);
                if (enemyTeam != null && enemyTeam != team && witherDamageable.getOrDefault(enemyTeam, false)) {
                    if (wither.getHealth() <= damage) {
                        finishHeroesOfGwen(team, "enemy Wither defeated by Rowena's Bombardment");
                        endRowenaBombardment(id, true);
                        return;
                    }
                    wither.setHealth(wither.getHealth() - damage);
                }
            } else {
                target.damage(damage, player);
            }
        }
        state.shotsRemaining--;
        if (state.shotsRemaining <= 0) endRowenaBombardment(id, true);
        else {
            ItemStack active = rowenaBombardmentActiveItem();
            ItemMeta meta = active.getItemMeta();
            meta.setLore(List.of(ChatColor.GRAY + "Shots remaining: " + state.shotsRemaining,
                    ChatColor.GRAY + "Click to fire the next explosive shot."));
            active.setItemMeta(meta);
            player.getInventory().setItem(0, active);
        }
    }

    private void endRowenaBombardment(UUID ownerId, boolean applyCooldown) {
        RowenaBombardmentState state = rowenaBombardments.remove(ownerId);
        if (state == null) return;
        if (state.anchorBlock.getType() == Material.BARRIER) {
            state.anchorBlock.setBlockData(state.originalBlock, false);
        }
        Player player = players.get(ownerId);
        if (player != null && player.isOnline()) {
            if (selectedHeroesOfGwen.getOrDefault(ownerId, "").equals("rowena")) {
                player.getInventory().setItem(0, state.originalBow == null
                        ? namedItem(Material.BOW, ChatColor.GREEN + "Bow", "Rowena's bow.")
                        : state.originalBow.clone());
                player.updateInventory();
                if (applyCooldown) {
                    setHeroSkillCooldown(player, "rowena.bombardment",
                            ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_REUSE_DELAY_MILLIS);
                }
            }
        }
    }

    private void removeRowenaState(UUID ownerId) {
        rowenaLightArrowsRemaining.remove(ownerId);
        rowenaLightArrowNextHitAt.remove(ownerId);
        rowenaCombatSlideEndsAt.remove(ownerId);
        endRowenaBombardment(ownerId, false);
    }

    private void updateRowenaAbilities(long now) {
        for (Map.Entry<UUID, Long> entry : new HashMap<>(rowenaCombatSlideEndsAt).entrySet()) {
            UUID id = entry.getKey();
            Player player = players.get(id);
            if (now >= entry.getValue() || player == null || !player.isOnline()
                    || !activePlayers.contains(id) || !"rowena".equals(selectedHeroesOfGwen.get(id))) {
                if (player != null && player.isOnline()) player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
                rowenaCombatSlideEndsAt.remove(id);
                continue;
            }
            Vector direction = player.getLocation().getDirection();
            direction.setY(0.0D);
            if (direction.lengthSquared() > 0.001D) {
                player.setVelocity(direction.normalize().multiply(
                        ArcadePortRules.HEROES_OF_GWEN_ROWENA_COMBAT_SLIDE_SPEED));
            }
            world.spawnParticle(Particle.CLOUD, player.getLocation().clone().add(0.0D, 0.8D, 0.0D),
                    4, 0.3D, 0.2D, 0.3D, 0.01D);
        }

        for (UUID id : new ArrayList<>(rowenaBombardments.keySet())) {
            Player player = players.get(id);
            RowenaBombardmentState state = rowenaBombardments.get(id);
            if (state == null) continue;
            if (player == null || !player.isOnline() || !activePlayers.contains(id)
                    || !"rowena".equals(selectedHeroesOfGwen.get(id))) {
                endRowenaBombardment(id, false);
                continue;
            }
            player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            if (now >= state.expiresAt) endRowenaBombardment(id, true);
        }
    }


    private void activateDanaPulseHeal(Player player) {
        String skill = "dana.pulse_heal";
        if (!isHeroSkillReady(player, skill)) return;
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_DANA_PULSE_HEAL_COOLDOWN_MILLIS);
        int team = teams.getOrDefault(player.getUniqueId(), -1);
        double radius = ArcadePortRules.HEROES_OF_GWEN_DANA_PULSE_HEAL_RADIUS;
        for (Entity entity : world.getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()
                    || player.getLocation().distanceSquared(target.getLocation()) > radius * radius
                    || heroesOfGwenEntityTeam(target) != team) continue;
            var maximumHealth = target.getAttribute(Attribute.MAX_HEALTH);
            if (maximumHealth == null) continue;
            double amount = target.equals(player)
                    ? ArcadePortRules.HEROES_OF_GWEN_DANA_PULSE_HEAL_SELF
                    : ArcadePortRules.HEROES_OF_GWEN_DANA_PULSE_HEAL_ALLY;
            target.setHealth(Math.min(maximumHealth.getValue(), target.getHealth() + amount));
            world.spawnParticle(Particle.HAPPY_VILLAGER,
                    target.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                    8, 0.45D, 0.5D, 0.45D, 0.06D);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.9F, 1.3F);
        player.sendActionBar(ChatColor.GREEN + "Pulse Heal!");
        for (int angle = 0; angle < 360; angle += 15) {
            double radians = Math.toRadians(angle);
            Location particle = player.getLocation().clone().add(
                    Math.cos(radians) * radius, 0.5D, Math.sin(radians) * radius);
            world.spawnParticle(Particle.HAPPY_VILLAGER, particle, 1, 0.0D, 0.0D, 0.0D, 0.02D);
        }
    }

    private void activateDanaKnockUp(Player player) {
        String skill = "dana.knock_up";
        if (!isHeroSkillReady(player, skill)) return;
        long now = System.currentTimeMillis();
        danaKnockUpEndsAt.put(player.getUniqueId(),
                now + ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_DURATION_MILLIS);
        danaKnockUpNextHitAt.put(player.getUniqueId(), new HashMap<>());
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.5F, 0.4F);
    }

    private void beginDanaRally(Player player) {
        UUID id = player.getUniqueId();
        if (danaRallies.containsKey(id) || !isHeroSkillReady(player, "dana.rally")) return;
        Vector leap = player.getLocation().getDirection();
        leap.setY(ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_JUMP_VERTICAL_VELOCITY);
        player.setVelocity(leap);
        danaRallies.put(id, new DanaRallyState(System.currentTimeMillis()));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        world.spawnParticle(Particle.CLOUD, player.getLocation().clone().add(0.0D, 0.7D, 0.0D),
                20, 0.5D, 0.5D, 0.5D, 0.08D);
    }

    private void landDanaRally(Player player, DanaRallyState state, long now) {
        state.landed = true;
        state.center = player.getLocation().clone();
        state.endsAt = now + ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_BANNER_DURATION_MILLIS;
        state.nextHealAt = now + 1_000L;
        setHeroSkillCooldown(player, "dana.rally", ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_REUSE_DELAY_MILLIS);

        Block bannerBlock = state.center.getBlock();
        state.bannerBlock = bannerBlock;
        state.originalBannerBlockData = bannerBlock.getBlockData().clone();
        if (bannerBlock.isPassable()) {
            bannerBlock.setType(teams.getOrDefault(player.getUniqueId(), 0) == 0
                    ? Material.RED_BANNER : Material.BLUE_BANNER, false);
            state.bannerPlaced = true;
        }

        int team = teams.getOrDefault(player.getUniqueId(), -1);
        double damageRadius = ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_LANDING_DAMAGE_RADIUS;
        for (Entity entity : world.getNearbyEntities(state.center, damageRadius, damageRadius, damageRadius)) {
            if (!(entity instanceof LivingEntity target) || target.equals(player)
                    || !target.isValid() || target.isDead()
                    || state.center.distanceSquared(target.getLocation()) > damageRadius * damageRadius
                    || heroesOfGwenEntityTeam(target) == team) continue;
            if (target instanceof Player targetPlayer && !activePlayers.contains(targetPlayer.getUniqueId())) continue;
            TowerState tower = towerFor(target);
            if (tower != null) {
                if (tower.owner != team && canDamageMobaTower(tower, team)
                        && player.getLocation().distanceSquared(tower.location) <= MOBA_TOWER_RANGE_SQUARED) {
                    damageMobaTower(tower, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_LANDING_DAMAGE, player);
                }
            } else {
                target.damage(ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_LANDING_DAMAGE, player);
            }
        }
        player.sendMessage(ChatColor.GOLD + "Rally banner planted! Allies nearby are healed.");
        player.playSound(state.center, Sound.BLOCK_BELL_USE, 1.0F, 1.2F);
        world.spawnParticle(Particle.HAPPY_VILLAGER, state.center.clone().add(0.0D, 1.0D, 0.0D),
                28, 1.0D, 0.7D, 1.0D, 0.08D);
    }

    private void updateDanaAbilities(long now) {
        for (Map.Entry<UUID, Long> entry : new HashMap<>(danaKnockUpEndsAt).entrySet()) {
            UUID id = entry.getKey();
            Player player = players.get(id);
            if (now >= entry.getValue() || player == null || !player.isOnline()
                    || !activePlayers.contains(id) || !"dana".equals(selectedHeroesOfGwen.get(id))) {
                if (player != null && player.isOnline()) player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
                danaKnockUpEndsAt.remove(id);
                danaKnockUpNextHitAt.remove(id);
                continue;
            }

            Vector direction = player.getLocation().getDirection();
            direction.setY(0.0D);
            if (direction.lengthSquared() > 0.001D) {
                player.setVelocity(direction.normalize()
                        .multiply(ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_SPEED));
            }
            world.spawnParticle(Particle.CLOUD, player.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                    6, 0.5D, 0.35D, 0.5D, 0.04D);

            Map<UUID, Long> nextHitAt = danaKnockUpNextHitAt.computeIfAbsent(id, ignored -> new HashMap<>());
            double radius = ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_COLLISION_RADIUS;
            int attackerTeam = teams.getOrDefault(id, -1);
            for (Entity entity : world.getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target) || target.equals(player)
                        || !target.isValid() || target.isDead()
                        || player.getLocation().distanceSquared(target.getLocation()) > radius * radius
                        || heroesOfGwenEntityTeam(target) == attackerTeam
                        || nextHitAt.getOrDefault(target.getUniqueId(), 0L) > now) continue;
                if (target instanceof Player targetPlayer && !activePlayers.contains(targetPlayer.getUniqueId())) continue;
                nextHitAt.put(target.getUniqueId(),
                        now + ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_TARGET_RECHARGE_MILLIS);
                if (target instanceof Player) {
                    target.damage(ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_PLAYER_DAMAGE, player);
                    target.setVelocity(new Vector(Math.random() / 2.0D - 0.25D,
                            ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_PLAYER_LIFT,
                            Math.random() / 2.0D - 0.25D));
                } else {
                    target.damage(ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_MINION_DAMAGE, player);
                    target.setVelocity(new Vector(Math.random() - 0.5D,
                            ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_MINION_LIFT,
                            Math.random() - 0.5D));
                }
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0F, 0.5F);
            }
        }

        for (Map.Entry<UUID, DanaRallyState> entry : new HashMap<>(danaRallies).entrySet()) {
            UUID id = entry.getKey();
            Player player = players.get(id);
            DanaRallyState state = entry.getValue();
            if (player == null || !player.isOnline() || !activePlayers.contains(id)
                    || !"dana".equals(selectedHeroesOfGwen.get(id))) {
                endDanaRally(id);
                continue;
            }
            if (!state.landed) {
                if (now - state.launchedAt >= ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_MINIMUM_FLIGHT_MILLIS
                        && player.isOnGround()) {
                    landDanaRally(player, state, now);
                }
                continue;
            }
            if (now >= state.endsAt) {
                endDanaRally(id);
                continue;
            }
            if (now >= state.nextHealAt) {
                double radius = ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_HEAL_RADIUS;
                int team = teams.getOrDefault(id, -1);
                for (Player teammate : activeMatchPlayers()) {
                    if (teams.getOrDefault(teammate.getUniqueId(), -2) != team
                            || teammate.getLocation().distanceSquared(state.center) > radius * radius) continue;
                    teammate.setHealth(Math.min(teammate.getMaxHealth(), teammate.getHealth()
                            + ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_HEAL_PER_SECOND));
                    world.spawnParticle(Particle.HAPPY_VILLAGER,
                            teammate.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                            4, 0.3D, 0.35D, 0.3D, 0.04D);
                }
                state.nextHealAt = now + 1_000L;
            }
            for (int angle = 0; angle < 360; angle += 30) {
                double radians = Math.toRadians(angle);
                Location particle = state.center.clone().add(
                        Math.cos(radians) * ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_HEAL_RADIUS,
                        0.35D, Math.sin(radians) * ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_HEAL_RADIUS);
                world.spawnParticle(Particle.HAPPY_VILLAGER, particle, 1, 0.0D, 0.0D, 0.0D, 0.01D);
            }
        }
    }

    private void endDanaRally(UUID ownerId) {
        DanaRallyState state = danaRallies.remove(ownerId);
        if (state == null || !state.bannerPlaced || state.bannerBlock == null) return;
        if (state.bannerBlock.getType() == Material.RED_BANNER || state.bannerBlock.getType() == Material.BLUE_BANNER) {
            state.bannerBlock.setBlockData(state.originalBannerBlockData, false);
        }
    }

    private void removeDanaState(UUID ownerId) {
        danaKnockUpEndsAt.remove(ownerId);
        danaKnockUpNextHitAt.remove(ownerId);
        endDanaRally(ownerId);
    }


    private void activateBiffTether(Player player) {
        UUID ownerId = player.getUniqueId();
        if (!isHeroSkillReady(player, "biff.tether")
                || !biffTetherTargets.getOrDefault(ownerId, List.of()).isEmpty()) return;
        double radius = ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_RANGE;
        int team = teams.getOrDefault(ownerId, -1);
        List<BiffTetherTarget> targets = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof Player target) || !target.isOnline()
                    || !activePlayers.contains(target.getUniqueId())
                    || target.equals(player) || teams.getOrDefault(target.getUniqueId(), -2) == team
                    || player.getLocation().distanceSquared(target.getLocation()) > radius * radius) continue;
            target.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    (int) (ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_SLOW_DURATION_MILLIS / 50L),
                    ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_SLOW_AMPLIFIER, true, true, true));
            target.sendMessage(ChatColor.RED + player.getName() + " has tethered you!");
            target.playSound(target.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.0F, 1.0F);
            targets.add(new BiffTetherTarget(target.getUniqueId(), System.currentTimeMillis()));
        }
        if (targets.isEmpty()) {
            player.sendActionBar(ChatColor.YELLOW + "No enemy heroes are close enough to tether.");
            return;
        }
        biffTetherTargets.put(ownerId, targets);
        setHeroSkillCooldown(player, "biff.tether", ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_REUSE_DELAY_MILLIS);
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.0F, 1.0F);
        player.sendMessage(ChatColor.GOLD + "Tethered " + targets.size() + " enemy hero(es).");
    }

    private void activateBiffBattleLeap(Player player) {
        String skill = "biff.battle_leap";
        if (!isHeroSkillReady(player, skill) || biffBattleLeapStartedAt.containsKey(player.getUniqueId())) return;
        Vector direction = player.getLocation().getDirection();
        direction.setY(ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_VERTICAL_VELOCITY);
        player.setVelocity(direction);
        biffBattleLeapStartedAt.put(player.getUniqueId(), System.currentTimeMillis());
        setHeroSkillCooldown(player, skill, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_COOLDOWN_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        world.spawnParticle(Particle.CLOUD, player.getLocation().clone().add(0.0D, 0.6D, 0.0D),
                15, 0.5D, 0.5D, 0.5D, 0.08D);
    }

    private void activateBiffCavalryCharge(Player player) {
        UUID ownerId = player.getUniqueId();
        if (!isHeroSkillReady(player, "biff.cavalry_charge") || biffHorses.containsKey(ownerId)) return;
        Horse horse = world.spawn(player.getLocation(), Horse.class);
        var maxHealth = horse.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(20.0D);
        horse.setHealth(20.0D);
        horse.setTamed(true);
        horse.setJumpStrength(ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_JUMP_STRENGTH);
        horse.setInvulnerable(true);
        horse.setRemoveWhenFarAway(false);
        horse.getInventory().setArmor(new ItemStack(Material.IRON_HORSE_ARMOR));
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.getPersistentDataContainer().set(biffHorseOwnerKey, PersistentDataType.STRING, ownerId.toString());
        horse.setCustomName(teamColour(teams.getOrDefault(ownerId, 0)) + player.getName() + "'s Cavalry");
        horse.setCustomNameVisible(true);
        entities.add(horse);
        horse.addPassenger(player);
        biffHorses.put(ownerId, new BiffHorseState(horse,
                System.currentTimeMillis() + ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_DURATION_MILLIS));
        setHeroSkillCooldown(player, "biff.cavalry_charge",
                ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_REUSE_DELAY_MILLIS);
        world.strikeLightningEffect(horse.getLocation());
        world.spawnParticle(Particle.CLOUD, horse.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                35, 0.8D, 0.8D, 0.8D, 0.08D);
        world.playSound(horse.getLocation(), Sound.ENTITY_HORSE_DEATH, 1.0F, 1.1F);
        player.sendMessage(ChatColor.GOLD + "Cavalry Charge!");
    }

    private void updateBiffAbilities(long now) {
        for (Map.Entry<UUID, List<BiffTetherTarget>> entry : new HashMap<>(biffTetherTargets).entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = players.get(ownerId);
            List<BiffTetherTarget> list = entry.getValue();
            java.util.Iterator<BiffTetherTarget> iterator = list.iterator();
            while (iterator.hasNext()) {
                BiffTetherTarget tether = iterator.next();
                Player target = players.get(tether.targetId);
                if (owner == null || !owner.isOnline() || !activePlayers.contains(ownerId)
                        || target == null || !target.isOnline() || !activePlayers.contains(tether.targetId)
                        || !"biff".equals(selectedHeroesOfGwen.get(ownerId))
                        || now - tether.startedAt >= ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_ACTIVE_MILLIS
                        || owner.getLocation().distanceSquared(target.getLocation())
                                >= ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_BREAK_DISTANCE_SQUARED
                        || owner.getLocation().distanceSquared(target.getLocation())
                                <= ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_PULL_STOP_DISTANCE_SQUARED) {
                    if (target != null && target.isOnline()) target.removePotionEffect(PotionEffectType.SLOWNESS);
                    iterator.remove();
                    continue;
                }
                Vector pull = owner.getLocation().toVector().subtract(target.getLocation().toVector());
                pull.setY(0.25D);
                if (pull.lengthSquared() > 0.001D) target.setVelocity(pull.normalize().multiply(0.75D));
                Location middle = target.getLocation().clone().add(
                        owner.getLocation().toVector().subtract(target.getLocation().toVector()).multiply(0.5D));
                world.spawnParticle(Particle.CRIT, middle, 1, 0.04D, 0.04D, 0.04D, 0.0D);
            }
            if (list.isEmpty()) biffTetherTargets.remove(ownerId);
        }

        for (Map.Entry<UUID, Long> entry : new HashMap<>(biffBattleLeapStartedAt).entrySet()) {
            UUID id = entry.getKey();
            Player player = players.get(id);
            if (player == null || !player.isOnline() || !activePlayers.contains(id)
                    || !"biff".equals(selectedHeroesOfGwen.get(id))) {
                biffBattleLeapStartedAt.remove(id);
                continue;
            }
            if (now - entry.getValue() < ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_MINIMUM_FLIGHT_MILLIS
                    || !player.isOnGround()) continue;
            double radius = ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_LANDING_RADIUS;
            int team = teams.getOrDefault(id, -1);
            for (Entity entity : world.getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target) || target.equals(player)
                        || !target.isValid() || target.isDead()
                        || player.getLocation().distanceSquared(target.getLocation()) > radius * radius
                        || heroesOfGwenEntityTeam(target) == team) continue;
                if (target instanceof Player targetPlayer && !activePlayers.contains(targetPlayer.getUniqueId())) continue;
                TowerState tower = towerFor(target);
                if (tower != null) {
                    if (tower.owner != team && canDamageMobaTower(tower, team)
                            && player.getLocation().distanceSquared(tower.location) <= MOBA_TOWER_RANGE_SQUARED) {
                        damageMobaTower(tower, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_DAMAGE, player);
                    }
                } else {
                    target.damage(ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_DAMAGE, player);
                }
                target.setVelocity(new Vector(0.0D,
                        ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_MINIMUM_KNOCK_UP
                                + Math.random() * ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_RANDOM_KNOCK_UP,
                        0.0D));
                world.spawnParticle(Particle.CLOUD, target.getLocation().clone().add(0.0D, 0.7D, 0.0D),
                        12, 0.45D, 0.45D, 0.45D, 0.08D);
            }
            biffBattleLeapStartedAt.remove(id);
            world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.7F);
            world.spawnParticle(Particle.EXPLOSION, player.getLocation().clone().add(0.0D, 1.0D, 0.0D),
                    3, 0.4D, 0.4D, 0.4D, 0.04D);
        }

        for (Map.Entry<UUID, BiffHorseState> entry : new HashMap<>(biffHorses).entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = players.get(ownerId);
            BiffHorseState state = entry.getValue();
            Horse horse = state.horse;
            if (owner == null || !owner.isOnline() || !activePlayers.contains(ownerId)
                    || !"biff".equals(selectedHeroesOfGwen.get(ownerId)) || now >= state.expiresAt
                    || !horse.isValid() || horse.isDead()) {
                removeBiffHorse(ownerId);
                continue;
            }
            double radius = ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_RADIUS;
            for (Player target : players.values()) {
                if (!target.isOnline() || !activePlayers.contains(target.getUniqueId())
                        || teams.getOrDefault(target.getUniqueId(), -2) == teams.getOrDefault(ownerId, -1)
                        || horse.getLocation().distanceSquared(target.getLocation()) > radius * radius
                        || state.nextRootAt.getOrDefault(target.getUniqueId(), 0L) > now) continue;
                state.nextRootAt.put(target.getUniqueId(),
                        now + ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_ROOT_RECHARGE_MILLIS);
                target.damage(ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_DAMAGE, owner);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        (int) (ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_ROOT_DURATION_MILLIS / 50L),
                        10, true, true, true));
                owner.sendActionBar(ChatColor.GOLD + "Cavalry hit " + target.getName() + "!");
            }
        }
    }

    private void removeBiffHorse(UUID ownerId) {
        BiffHorseState state = biffHorses.remove(ownerId);
        if (state == null) return;
        if (state.horse != null) {
            state.horse.eject();
            state.horse.remove();
            entities.remove(state.horse);
        }
    }

    private void removeBiffState(UUID ownerId) {
        List<BiffTetherTarget> leashed = biffTetherTargets.remove(ownerId);
        if (leashed != null) {
            for (BiffTetherTarget tether : leashed) {
                Player target = players.get(tether.targetId);
                if (target != null && target.isOnline()) target.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
        biffBattleLeapStartedAt.remove(ownerId);
        removeBiffHorse(ownerId);
    }

    private void markHeroProjectile(Projectile projectile, Player owner, String type) {
        projectile.getPersistentDataContainer().set(heroProjectileTypeKey, PersistentDataType.STRING, type);
        projectile.getPersistentDataContainer().set(heroProjectileOwnerKey, PersistentDataType.STRING,
                owner.getUniqueId().toString());
    }

    private Player heroProjectileOwner(Projectile projectile) {
        String owner = projectile.getPersistentDataContainer().get(heroProjectileOwnerKey, PersistentDataType.STRING);
        if (owner == null) return null;
        try {
            Player player = Bukkit.getPlayer(UUID.fromString(owner));
            return player != null && player.isOnline() ? player : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void updateHeroesOfGwenAbilities(long now) {
        for (Map.Entry<UUID, Long> entry : new HashMap<>(heroEnderBladeEndsAt).entrySet()) {
            if (now < entry.getValue()) continue;
            Player player = players.get(entry.getKey());
            if (player != null && player.isOnline()) {
                ItemStack original = heroEnderBladeOriginalSword.remove(entry.getKey());
                player.getInventory().setItem(0, original == null ? namedItem(Material.WOODEN_SWORD,
                        ChatColor.GREEN + "Sword", "Basic Hattori weapon.") : original);
                player.updateInventory();
            }
            heroEnderBladeEndsAt.remove(entry.getKey());
        }
        for (Map.Entry<UUID, Long> entry : new HashMap<>(heroInfinityEndsAt).entrySet()) {
            if (now < entry.getValue()) continue;
            Player player = players.get(entry.getKey());
            if (player != null && player.isOnline()) {
                ItemStack bow = player.getInventory().getItem(0);
                int original = heroInfinityOriginalEnchant.getOrDefault(entry.getKey(), 0);
                if (bow != null && bow.getType() == Material.BOW) {
                    bow.removeEnchantment(Enchantment.INFINITY);
                    if (original > 0) bow.addUnsafeEnchantment(Enchantment.INFINITY, original);
                }
                player.updateInventory();
            }
            heroInfinityEndsAt.remove(entry.getKey());
            heroInfinityOriginalEnchant.remove(entry.getKey());
        }
        updateAnathAbilities(now);
        updateLarissaAbilities(now);
        updateLarissaFountains(now);
        updateBardolfWolves(now);
        updateDanaAbilities(now);
        updateBiffAbilities(now);
        updateIvyAbilities(now);
        updateRowenaAbilities(now);
        for (Map.Entry<UUID, BardolfPounceState> entry : new HashMap<>(bardolfPounces).entrySet()) {
            if (now >= entry.getValue().expiresAt) bardolfPounces.remove(entry.getKey());
        }
        for (UUID id : new ArrayList<>(activePlayers)) {
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            String hunterHero = selectedHeroesOfGwen.get(id);
            if ("hattori".equals(hunterHero) && player.isOnGround()
                    && player.getGameMode() != org.bukkit.GameMode.CREATIVE
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR
                    && isHeroSkillReady(player, "hattori.double_jump")) {
                player.setAllowFlight(true);
            }
            if ("larissa".equals(hunterHero)) {
                long nextWaterStone = heroAmmoNextAt.getOrDefault(id,
                        now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_AMMO_RELOAD_MILLIS);
                if (now >= nextWaterStone) {
                    ItemStack waterStone = player.getInventory().getItem(7);
                    int amount = waterStone != null && waterStone.getType() == Material.LAPIS_LAZULI
                            && waterStone.hasItemMeta() && waterStone.getItemMeta().hasDisplayName()
                            && waterStone.getItemMeta().getDisplayName().contains("Water Stone")
                            ? waterStone.getAmount() : 0;
                    if (amount < ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_AMMO_CAPACITY) {
                        if (amount <= 0) {
                            player.getInventory().setItem(7, namedItem(Material.LAPIS_LAZULI,
                                    ChatColor.AQUA + "Water Stone", "Aqua Cannon ammunition."));
                        } else waterStone.setAmount(amount + 1);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.45F, 1.5F);
                    }
                    heroAmmoNextAt.put(id, now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_AMMO_RELOAD_MILLIS);
                }
            }
            int fletcherCapacity = ArcadePortRules.heroesOfGwenFletcherMaxArrows(hunterHero);
            long fletcherReload = ArcadePortRules.heroesOfGwenFletcherReloadMillis(hunterHero);
            if (fletcherCapacity > 0 && fletcherReload > 0L) {
                long nextAmmo = heroAmmoNextAt.getOrDefault(id, now + fletcherReload);
                if (now >= nextAmmo) {
                    ItemStack ammo = player.getInventory().getItem(7);
                    int count = ammo != null && ammo.getType() == Material.ARROW ? ammo.getAmount() : 0;
                    if (count < fletcherCapacity) {
                        if (ammo == null || ammo.getType() != Material.ARROW) {
                            player.getInventory().setItem(7, namedItem(Material.ARROW,
                                    ChatColor.YELLOW + "Fletched Arrow", "Replenished by Fletcher."));
                        } else ammo.setAmount(count + 1);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.3F);
                    }
                    heroAmmoNextAt.put(id, now + fletcherReload);
                }
            }
            if (heroInfinityEndsAt.getOrDefault(id, 0L) > now) {
                for (Entity entity : world.getNearbyEntities(player.getLocation(), 32.0D, 32.0D, 32.0D)) {
                    if (!(entity instanceof Arrow arrow)
                            || !"devon.infinity_arrow".equals(arrow.getPersistentDataContainer()
                                    .get(heroProjectileTypeKey, PersistentDataType.STRING))) continue;
                    Player owner = heroProjectileOwner(arrow);
                    if (owner == null || !owner.getUniqueId().equals(id)) continue;
                    Player target = activeMatchPlayers().stream()
                            .filter(candidate -> !candidate.equals(player)
                                    && teams.getOrDefault(candidate.getUniqueId(), -1)
                                            != teams.getOrDefault(id, -2)
                                    && candidate.getLocation().distanceSquared(arrow.getLocation())
                                            <= ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_HOMING_RANGE
                                                    * ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_HOMING_RANGE)
                            .min(java.util.Comparator.comparingDouble(candidate ->
                                    candidate.getLocation().distanceSquared(arrow.getLocation())))
                            .orElse(null);
                    if (target != null) {
                        Vector homing = target.getEyeLocation().toVector().subtract(arrow.getLocation().toVector());
                        if (homing.lengthSquared() > 0.001D) arrow.setVelocity(homing.normalize().multiply(1.5D));
                    }
                }
            }
        }
    }

    void stop() {
        if (stopped) return;
        stopped = true;
        if (listenersRegistered) {
            org.bukkit.event.HandlerList.unregisterAll(this);
            listenersRegistered = false;
        }
        if (objectiveItem != null && objectiveItem.isValid()) objectiveItem.remove();
        objectiveItem = null;
        for (UUID id : new ArrayList<>(carriedTntOldHelmet.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) restoreCarrierHelmet(player);
        }
        for (UUID id : new ArrayList<>(rowenaBombardments.keySet())) endRowenaBombardment(id, false);
        for (UUID id : new ArrayList<>(danaRallies.keySet())) endDanaRally(id);
        for (UUID id : new ArrayList<>(anathFlameEmbers.keySet())) removeAnathEmber(id);
        for (Map.Entry<Block, AnathFireTrailState> entry : new HashMap<>(anathFireTrailBlocks).entrySet()) {
            if (entry.getValue().placed && entry.getKey().getType() == Material.FIRE) {
                entry.getKey().setBlockData(entry.getValue().originalData, false);
            }
        }
        anathFireTrailBlocks.clear();
        for (UUID id : new ArrayList<>(biffHorses.keySet())) removeBiffHorse(id);
        for (UUID id : new ArrayList<>(ivyHooks.keySet())) removeIvyState(id);
        for (UUID id : new ArrayList<>(ivyFloralLeaps.keySet())) removeIvyState(id);
        for (UUID id : new ArrayList<>(ivyThornWalls.keySet())) removeIvyState(id);
        for (Entity entity : new ArrayList<>(entities)) if (entity != null && entity.isValid()) entity.remove();
        for (Map.Entry<Block, BlockData> block : changedBlocks.entrySet()) {
            if (block.getKey() != null && block.getKey().getWorld() == world) block.getKey().setBlockData(block.getValue(), false);
        }
        for (Player player : onlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof StoreHolder) player.closeInventory();
        }
        openStores.clear();
        entities.clear();
        for (FallingBlock debris : new HashSet<>(witherDebris)) {
            if (debris != null && debris.isValid()) debris.remove();
        }
        witherDebris.clear();
        witherTargets.clear();
        heroesOfGwenPumpkinKing = null;
        heroesOfGwenPumpkinKingTarget = null;
        heroesOfGwenPumpkinBoundary = List.of();
        heroesOfGwenMonochromeSkeletons.clear();
        heroesOfGwenPumpkinBuffEndsAt.clear();
        heroesOfGwenMonochromeEndsAt = 0L;
        heroesOfGwenSidequestNextStartAt = 0L;
        heroesOfGwenPumpkinKingRespawnAt = 0L;
        heroesOfGwenMinionTeams.clear();
        heroesOfGwenIronMinionTeams.clear();
        witherHomes.clear();
        witherBoundaries.clear();
        witherNextEarthquakeAt.clear();
        castleKillStreaks.clear();
        heroSkillReadyAt.clear();
        heroTntArrowCharges.clear();
        heroAmmoNextAt.clear();
        heroEnderBladeEndsAt.clear();
        heroEnderBladeOriginalSword.clear();
        heroInfinityEndsAt.clear();
        heroInfinityOriginalEnchant.clear();
        anathMeteorZones.clear();
        anathFlameDashEndsAt.clear();
        anathBurnBeams.clear();
        anathFlameEmbers.clear();
        anathBurnBeamNextHitAt.clear();
        anathFireTrailBlocks.clear();
        larissaFountainZones.clear();
        larissaWaterDashEndsAt.clear();
        larissaStormBlessingEndsAt.clear();
        larissaStormBlessingNextHealAt.clear();
        bardolfWolves.clear();
        bardolfWolfTargets.clear();
        bardolfLastWolfAttackAt.clear();
        bardolfPounces.clear();
        bardolfFullMoonEndsAt.clear();
        bardolfOriginalWalkSpeeds.clear();
        rowenaLightArrowsRemaining.clear();
        rowenaLightArrowNextHitAt.clear();
        rowenaCombatSlideEndsAt.clear();
        rowenaBombardments.clear();
        danaKnockUpEndsAt.clear();
        danaKnockUpNextHitAt.clear();
        danaRallies.clear();
        biffTetherTargets.clear();
        biffBattleLeapStartedAt.clear();
        biffHorses.clear();
        ivyHooks.clear();
        ivyFloralLeaps.clear();
        ivyThornWalls.clear();
        castleAssaultPlayerKits.clear();
        castleArcherNextArrowAt.clear();
        castleDemolitionistNextTntAt.clear();
        selectedHeroesOfGwen.clear();
        heroesOfGwenKills.clear();
        heroesOfGwenRoleKills.clear();
        heroesOfGwenCounterpartOnly.clear();
        heroesOfGwenChangedAfterTowerLoss.clear();
        heroesOfGwenFinalObjectiveHits.clear();
    }

    private enum HeroesOfGwenSidequestType {
        NONE,
        PUMPKIN_KING,
        MONOCHROME_SKELETONS
    }

    private static final class KingState {
        final int team;
        final String name;
        final Location location;
        final Zombie entity;
        final List<EnderCrystal> crystals = new ArrayList<>();
        int health;
        KingState(int team, String name, Location location, Zombie entity, int health) {
            this.team = team;
            this.name = name;
            this.location = location;
            this.entity = entity;
            this.health = health;
        }
        int activeCrystals() {
            return (int) crystals.stream().filter(crystal -> crystal != null && crystal.isValid()).count();
        }
    }

    private static final class CapturePointState {
        final String name;
        final String colour;
        final Location center;
        final List<Block> wool;
        int owner = -1;
        int side = -1;
        int progress;
        CapturePointState(String name, String colour, Location center, List<Block> wool) {
            this.name = name;
            this.colour = colour;
            this.center = center;
            this.wool = wool;
        }
    }

    private static final class IvyHookState {
        final Location origin;
        final Vector direction;
        final long startedAt;
        final Set<UUID> hitEntities = new HashSet<>();
        double distance;
        UUID targetId;
        boolean firstPull;
        IvyHookState(Location origin, Vector direction, long startedAt) {
            this.origin = origin.clone();
            this.direction = direction.clone();
            this.startedAt = startedAt;
        }
    }

    private static final class IvyFloralLeapState {
        final long launchedAt;
        boolean landed;
        Location center;
        long endsAt;
        long nextDamageAt;
        final Map<Block, BlockData> flowerBlocks = new HashMap<>();
        final Map<Block, Material> flowerMaterials = new HashMap<>();
        IvyFloralLeapState(long launchedAt) { this.launchedAt = launchedAt; }
    }

    private static final class IvyThornWallState {
        final Location center;
        final long endsAt;
        final Map<Block, BlockData> originalBlocks = new HashMap<>();
        final Map<Block, Material> placedMaterials = new HashMap<>();
        final Map<UUID, Long> nextRootAt = new HashMap<>();
        final Map<UUID, Long> nextDamageAt = new HashMap<>();
        IvyThornWallState(Location center, long endsAt) {
            this.center = center.clone();
            this.endsAt = endsAt;
        }
    }

    private static final class BiffTetherTarget {
        final UUID targetId;
        final long startedAt;
        BiffTetherTarget(UUID targetId, long startedAt) {
            this.targetId = targetId;
            this.startedAt = startedAt;
        }
    }

    private static final class BiffHorseState {
        final Horse horse;
        final long expiresAt;
        final Map<UUID, Long> nextRootAt = new HashMap<>();
        BiffHorseState(Horse horse, long expiresAt) {
            this.horse = horse;
            this.expiresAt = expiresAt;
        }
    }

    private static final class DanaRallyState {
        final long launchedAt;
        boolean landed;
        Location center;
        long endsAt;
        long nextHealAt;
        Block bannerBlock;
        BlockData originalBannerBlockData;
        boolean bannerPlaced;
        DanaRallyState(long launchedAt) { this.launchedAt = launchedAt; }
    }

    private static final class RowenaBombardmentState {
        final Block anchorBlock;
        final BlockData originalBlock;
        final ItemStack originalBow;
        final long expiresAt;
        int shotsRemaining;
        RowenaBombardmentState(Block anchorBlock, BlockData originalBlock, ItemStack originalBow,
                long expiresAt, int shotsRemaining) {
            this.anchorBlock = anchorBlock;
            this.originalBlock = originalBlock.clone();
            this.originalBow = originalBow == null ? null : originalBow.clone();
            this.expiresAt = expiresAt;
            this.shotsRemaining = shotsRemaining;
        }
    }

    private static final class BardolfPounceState {
        final Location target;
        final Vector direction;
        final long expiresAt;
        final Set<UUID> leapedWolfIds = new HashSet<>();
        BardolfPounceState(Location target, Vector direction, long expiresAt) {
            this.target = target.clone();
            this.direction = direction.clone();
            this.expiresAt = expiresAt;
        }
    }

    private static final class AnathFlameEmberState {
        final UUID ownerId;
        final Location origin;
        final Vector direction;
        final long expiresAt;
        AnathFlameEmberState(UUID ownerId, Location origin, Vector direction, long expiresAt) {
            this.ownerId = ownerId;
            this.origin = origin.clone();
            this.direction = direction.clone();
            this.expiresAt = expiresAt;
        }
    }

    private static final class AnathBurnBeamState {
        final Location origin;
        final Vector direction;
        final long startedAt;
        double distance;
        AnathBurnBeamState(Location origin, Vector direction, long startedAt) {
            this.origin = origin.clone();
            this.direction = direction.clone();
            this.startedAt = startedAt;
        }
    }

    private static final class AnathFireTrailState {
        final BlockData originalData;
        final long placeAt;
        long expiresAt;
        boolean placed;
        AnathFireTrailState(BlockData originalData, long placeAt) {
            this.originalData = originalData.clone();
            this.placeAt = placeAt;
        }
    }

    private static final class LarissaFountainZone {
        final UUID ownerId;
        final int team;
        final Location center;
        final long expiresAt;
        long nextPulseAt;
        long nextVisualAt;
        LarissaFountainZone(UUID ownerId, int team, Location center, long expiresAt, long now) {
            this.ownerId = ownerId;
            this.team = team;
            this.center = center.clone();
            this.expiresAt = expiresAt;
            this.nextPulseAt = now + ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_PULSE_INTERVAL_MILLIS;
            this.nextVisualAt = now;
        }
    }

    private static final class AnathMeteorZone {
        final UUID ownerId;
        final int team;
        final Location center;
        final long expiresAt;
        long nextBurnAt;
        long nextPulseAt;
        long nextVisualAt;
        AnathMeteorZone(UUID ownerId, int team, Location center, long expiresAt, long now) {
            this.ownerId = ownerId;
            this.team = team;
            this.center = center.clone();
            this.expiresAt = expiresAt;
            this.nextBurnAt = now;
            this.nextPulseAt = now;
            this.nextVisualAt = now;
        }
    }

    private static final class TowerState {
        final int owner;
        final Location location;
        final String marker;
        final boolean first;
        final ArmorStand guardian;
        final EnderCrystal crystal;
        final Map<UUID, Long> targetEnteredAt = new HashMap<>();
        double health;
        boolean dead;
        TowerState(int owner, Location location, String marker, boolean first, double health,
                ArmorStand guardian, EnderCrystal crystal) {
            this.owner = owner;
            this.location = location;
            this.marker = marker;
            this.first = first;
            this.health = health;
            this.guardian = guardian;
            this.crystal = crystal;
        }
    }

    private static final class StoreEntry {
        final String role;
        final Material material;
        final String name;
        final int cost;
        final String description;
        StoreEntry(String role, Material material, String name, int cost, String description) {
            this.role = role;
            this.material = material;
            this.name = name;
            this.cost = cost;
            this.description = description;
        }
    }

    private static final class StoreHolder implements InventoryHolder {
        final UUID owner;
        final String role;
        Inventory inventory;
        StoreHolder(UUID owner, String role) {
            this.owner = owner;
            this.role = role;
        }
        @Override public Inventory getInventory() { return inventory; }
    }
}
