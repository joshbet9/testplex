package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private static final long MOBA_PASSIVE_GOLD_INTERVAL = 5_000L;
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
    private final NamespacedKey heroSkillKey;
    private final NamespacedKey heroProjectileTypeKey;
    private final NamespacedKey heroProjectileOwnerKey;
    private final Map<Integer, KingState> kings = new HashMap<>();
    private final Map<UUID, Integer> playerGold = new HashMap<>();
    private final Map<UUID, ItemStack> carriedTntOldHelmet = new HashMap<>();
    private final Map<Integer, List<TowerState>> towers = new HashMap<>();
    private final List<CapturePointState> capturePoints = new ArrayList<>();
    private final Map<Integer, Wither> withers = new HashMap<>();
    private final Map<Integer, Boolean> witherDamageable = new HashMap<>();
    private final Map<Integer, Location> witherHomes = new HashMap<>();
    private final Map<Integer, List<Location>> witherBoundaries = new HashMap<>();
    private final Map<Integer, UUID> witherTargets = new HashMap<>();
    private final Map<Integer, Long> witherNextEarthquakeAt = new HashMap<>();
    private final Set<FallingBlock> witherDebris = new HashSet<>();
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
        this.heroSkillKey = new NamespacedKey(plugin, "heroes-gwen-skill");
        this.heroProjectileTypeKey = new NamespacedKey(plugin, "heroes-gwen-projectile-type");
        this.heroProjectileOwnerKey = new NamespacedKey(plugin, "heroes-gwen-projectile-owner");
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

            Player target = witherTargets.containsKey(team) ? players.get(witherTargets.get(team)) : null;
            if (!isValidWitherTarget(team, target)) {
                target = activeMatchPlayers().stream()
                        .filter(player -> isValidWitherTarget(ownerTeam, player))
                        .min(java.util.Comparator.comparingDouble(player ->
                                player.getLocation().distanceSquared(home)))
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

    private boolean isValidWitherTarget(int team, Player target) {
        return target != null && target.isOnline() && activePlayers.contains(target.getUniqueId())
                && teams.getOrDefault(target.getUniqueId(), -1) == 1 - team
                && isInsideWitherBoundary(team, target.getLocation());
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
            if (!(entity instanceof Player player) || !activePlayers.contains(player.getUniqueId())
                    || teams.getOrDefault(player.getUniqueId(), -1) == team) continue;
            double distance = Math.min(center.distance(player.getLocation()),
                    center.distance(player.getEyeLocation()));
            if (distance >= ArcadePortRules.HEROES_OF_GWEN_WITHER_EARTHQUAKE_RADIUS) continue;
            double damage = ArcadePortRules.heroesOfGwenWitherEarthquakeDamage(distance);
            if (player.getHealth() <= damage) {
                hooks.eliminate(player, null, "was struck by the enemy Wither's Earthquake");
            } else {
                player.setHealth(player.getHealth() - damage);
            }
            Vector knockback = player.getLocation().toVector().subtract(center.toVector());
            if (knockback.lengthSquared() > 0.001D) {
                player.setVelocity(knockback.normalize().setY(1.0D));
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
        updateHeroesOfGwenAbilities(now);
        if (now >= nextGoldAt) {
            for (Player player : activeMatchPlayers()) addGold(player, 20, "Passive gold");
            for (CapturePointState point : capturePoints) {
                if (point.owner < 0) continue;
                for (Player player : activeMatchPlayers()) {
                    if (teams.getOrDefault(player.getUniqueId(), -1) == point.owner) {
                        addGold(player, 5, "Captured beacon");
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
        ItemStack skill = player.getInventory().getItem(3);
        String id = heroSkillId(skill);
        if (!"hattori.ender_blade".equals(id) && !"devon.infinity".equals(id)) return;
        event.setCancelled(true);
        activateHeroesOfGwenSkill(player, id);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenSneak(PlayerToggleSneakEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN || !event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!"devon".equals(selectedHeroesOfGwen.get(player.getUniqueId()))) return;
        ItemStack skill = player.getInventory().getItem(2);
        if (!"devon.hunters_boost".equals(heroSkillId(skill))) return;
        activateHeroesOfGwenSkill(player, "devon.hunters_boost");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeroesOfGwenBowShoot(EntityShootBowEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN
                || !(event.getEntity() instanceof Player player)) return;
        String hero = selectedHeroesOfGwen.get(player.getUniqueId());
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
    public void onHeroesOfGwenProjectileDamage(EntityDamageByEntityEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Entity damager = event.getDamager();
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
            event.setDamage(event.getDamage() + 9.0D);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroesOfGwenProjectileHit(ProjectileHitEvent event) {
        if (stopped || game != ArcadePortGame.HEROES_OF_GWEN) return;
        Projectile projectile = event.getEntity();
        String type = projectile.getPersistentDataContainer().get(heroProjectileTypeKey, PersistentDataType.STRING);
        if (!"devon.tnt_arrow".equals(type)) return;
        Player owner = heroProjectileOwner(projectile);
        if (owner == null) return;
        Location center = projectile.getLocation();
        world.spawnParticle(Particle.EXPLOSION, center, 3, 0.25D, 0.25D, 0.25D, 0.02D);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.1F);
        ItemStack bow = owner.getInventory().getItem(0);
        double damageAtCenter = 10.0D + (bow == null ? 0.0D : bow.getEnchantmentLevel(Enchantment.POWER) * 2.5D);
        for (Entity entity : world.getNearbyEntities(center, 5.0D, 5.0D, 5.0D)) {
            if (!(entity instanceof Player target) || target.equals(owner)
                    || !activePlayers.contains(target.getUniqueId())
                    || teams.getOrDefault(target.getUniqueId(), -1) == teams.getOrDefault(owner.getUniqueId(), -2)) continue;
            double distance = Math.min(center.distance(target.getLocation()), center.distance(target.getEyeLocation()));
            if (distance >= 5.0D) continue;
            double falloff = 1.0D - distance / 5.0D;
            double damage = damageAtCenter * falloff;
            if (target.getHealth() <= damage) hooks.eliminate(target, owner, "was hit by a TNT-infused arrow");
            else target.damage(damage, owner);
            Vector knockback = target.getLocation().toVector().subtract(center.toVector());
            if (knockback.lengthSquared() > 0.001D) target.setVelocity(knockback.normalize().multiply(0.8D * falloff).setY(0.25D));
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
                addGold(player, 25, "Capturing " + point.name);
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

    private void updateMobaTowers() {
        for (Map.Entry<Integer, List<TowerState>> entry : towers.entrySet()) {
            int owner = entry.getKey();
            for (TowerState tower : entry.getValue()) {
                if (tower.dead) continue;
                Player target = nearestEnemy(tower.location, owner);
                if (target != null) {
                    if (target.getHealth() <= MOBA_TOWER_DAMAGE) hooks.eliminate(target, null, "was defeated by a tower guardian");
                    else target.setHealth(target.getHealth() - MOBA_TOWER_DAMAGE);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
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
        TowerState tower = towerFor(damaged);
        if (tower != null) {
            event.setCancelled(true);
            if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;
            Player attacker = resolveAttacker(byEntity);
            if (attacker == null || teams.getOrDefault(attacker.getUniqueId(), -1) == tower.owner) return;
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
        heroEnderBladeEndsAt.remove(id);
        heroEnderBladeOriginalSword.remove(id);
        heroInfinityEndsAt.remove(id);
        heroInfinityOriginalEnchant.remove(id);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItem(8, namedItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "Open Gold Upgrades", "Right-click to open your role shop."));
        switch (hero) {
            case "hattori" -> {
                player.getInventory().setItem(0, namedItem(Material.WOODEN_SWORD,
                        ChatColor.GREEN + "Sword", "Basic Hattori weapon."));
                giveHeroesOfGwenSkillItem(player, 1, "hattori.shuriken", Material.SNOWBALL,
                        "Shuriken", "Fires three damaging snowballs.");
                giveHeroesOfGwenSkillItem(player, 2, "hattori.ninja_dash", Material.FEATHER,
                        "Ninja Dash", "Dash up to ten blocks and damage enemies in your path.");
                giveHeroesOfGwenSkillItem(player, 3, "hattori.ender_blade", Material.NETHER_STAR,
                        "Ender Blade", "Use the Ender Blade for seven seconds.");
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
                setHeroSkillCooldown(player, skill, 1_500L);
                for (int i = 0; i < 3; i++) {
                    final long delay = i * 4L;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || !activePlayers.contains(id)) return;
                        Snowball snowball = player.launchProjectile(Snowball.class);
                        markHeroProjectile(snowball, player, "hattori.shuriken");
                    }, delay);
                }
                player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0F, 1.3F);
            }
            case "hattori.ninja_dash" -> {
                setHeroSkillCooldown(player, skill, 7_000L);
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
                heroEnderBladeEndsAt.put(id, System.currentTimeMillis() + 7_000L);
                setHeroSkillCooldown(player, skill, 67_000L);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Hattori activated Ender Blade!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
            }
            case "devon.tnt_infusion" -> {
                heroTntArrowCharges.put(id, 3);
                ItemStack skillItem = player.getInventory().getItem(1);
                if (skillItem != null) skillItem.setAmount(3);
                player.sendMessage(ChatColor.GOLD + "Your next three arrows are infused with TNT.");
                player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.8F, 1.2F);
            }
            case "devon.hunters_boost" -> {
                setHeroSkillCooldown(player, skill, 10_000L);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3 * 20, 1, true, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 3 * 20, 0, true, true, true));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.5F);
            }
            case "devon.infinity" -> activateDevonInfinity(player);
            default -> player.sendMessage(ChatColor.YELLOW + "That hero ability is not yet available.");
        }
    }

    private void performHattoriDash(Player player) {
        Location origin = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().normalize();
        Location destination = player.getLocation().clone();
        Set<UUID> hit = new HashSet<>();
        double range = 10.0D;
        for (double distance = 0.5D; distance <= range; distance += 0.5D) {
            Location point = origin.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.CRIT, point, 3, 0.12D, 0.12D, 0.12D, 0.01D);
            for (Entity entity : world.getNearbyEntities(point, 1.5D, 1.5D, 1.5D)) {
                if (!(entity instanceof Player target) || target.equals(player)
                        || !activePlayers.contains(target.getUniqueId())
                        || teams.getOrDefault(target.getUniqueId(), -1) == teams.getOrDefault(player.getUniqueId(), -2)
                        || !hit.add(target.getUniqueId())) continue;
                target.damage(8.0D, player);
                Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector());
                if (push.lengthSquared() > 0.001D) target.setVelocity(push.normalize().multiply(0.7D).setY(0.25D));
            }
            Block feet = point.getBlock();
            if (!feet.isPassable() && distance > 1.0D) break;
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
        heroInfinityEndsAt.put(id, System.currentTimeMillis() + 7_000L);
        setHeroSkillCooldown(player, "devon.infinity", 67_000L);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Infinity activated: homing arrows and Wither for seven seconds.");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
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
        for (UUID id : new ArrayList<>(activePlayers)) {
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            if ("devon".equals(selectedHeroesOfGwen.get(id))) {
                long nextAmmo = heroAmmoNextAt.getOrDefault(id, now + 3_000L);
                if (now >= nextAmmo) {
                    ItemStack ammo = player.getInventory().getItem(7);
                    int count = ammo != null && ammo.getType() == Material.ARROW ? ammo.getAmount() : 0;
                    if (count < 3) {
                        if (ammo == null || ammo.getType() != Material.ARROW) {
                            player.getInventory().setItem(7, namedItem(Material.ARROW,
                                    ChatColor.YELLOW + "Hunting Arrow", "Replenished by Devon's kit."));
                        } else ammo.setAmount(count + 1);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.3F);
                    }
                    heroAmmoNextAt.put(id, now + 3_000L);
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
                                    && candidate.getLocation().distanceSquared(arrow.getLocation()) <= 36.0D)
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

    private static final class TowerState {
        final int owner;
        final Location location;
        final String marker;
        final boolean first;
        final ArmorStand guardian;
        final EnderCrystal crystal;
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
