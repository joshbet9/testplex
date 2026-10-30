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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
    private static final double MOBA_TOWER_RANGE_SQUARED = 100.0D;
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
    private final Set<Entity> entities = new HashSet<>();
    private final Map<Block, BlockData> changedBlocks = new HashMap<>();
    private final NamespacedKey objectiveTntKey;
    private final NamespacedKey thrownTntOwnerKey;
    private final NamespacedKey shopItemKey;
    private final NamespacedKey kingKey;
    private final NamespacedKey towerKey;
    private final Map<Integer, KingState> kings = new HashMap<>();
    private final Map<UUID, Integer> playerGold = new HashMap<>();
    private final Map<UUID, ItemStack> carriedTntOldHelmet = new HashMap<>();
    private final Map<Integer, List<TowerState>> towers = new HashMap<>();
    private final Map<Integer, Wither> withers = new HashMap<>();
    private final Map<Integer, Boolean> witherDamageable = new HashMap<>();
    private final List<Location> objectiveTntSpawns = new ArrayList<>();
    private final List<StoreEntry> storeEntries = new ArrayList<>();
    private final Map<UUID, StoreHolder> openStores = new HashMap<>();

    private Item objectiveItem;
    private UUID objectiveCarrier;
    private long objectiveRespawnAt;
    private long nextGoldAt;
    private long nextStructureUpdateAt;
    private int objectiveKillsEnabled;
    private int castleKillCountRed;
    private int castleKillCountBlue;
    private boolean ready;
    private boolean stopped;

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
        this.kingKey = new NamespacedKey(plugin, "castle-king-team");
        this.towerKey = new NamespacedKey(plugin, "heroes-gwen-tower");
        for (Player player : participants) this.players.put(player.getUniqueId(), player);
        this.teams.putAll(teams);

        boolean prepared = game == ArcadePortGame.CASTLE_ASSAULT
                ? prepareCastleAssault()
                : game == ArcadePortGame.HEROES_OF_GWEN && prepareHeroesOfGwen();
        this.ready = prepared;
        if (prepared) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            long now = System.currentTimeMillis();
            objectiveRespawnAt = now + objectiveDelay();
            nextGoldAt = now + MOBA_PASSIVE_GOLD_INTERVAL;
            nextStructureUpdateAt = now + MOBA_TOWER_UPDATE_INTERVAL;
            plugin.getLogger().info("Prepared " + game.displayName() + (mixedArcade ? " Mixed Arcade" : "")
                    + " source runtime using " + map.mapName() + ".");
        }
    }

    boolean isReady() {
        return ready && !stopped;
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
        return true;
    }

    private void spawnKing(int team, String name, Location location) {
        Zombie king = world.spawn(location, Zombie.class);
        king.setAI(false);
        king.setSilent(true);
        king.setInvulnerable(true);
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
            if (!name.toUpperCase(Locale.ROOT).startsWith("TOWER")) continue;
            int owner = teamFromMarker(name);
            if (owner < 0) continue;
            for (Location location : group.getValue()) {
                if (mixedArcade && isMiddleTowerMarker(name)) continue;
                boolean first = isFirstTowerMarker(name);
                spawnMobaTower(owner, location, first, name);
                found++;
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
        wither.setInvulnerable(true);
        wither.setSilent(true);
        wither.setRemoveWhenFarAway(false);
        wither.setCustomName(teamColour(team) + (team == 0 ? "Red" : "Blue") + " Wither");
        wither.setCustomNameVisible(true);
        wither.getPersistentDataContainer().set(kingKey, PersistentDataType.INTEGER, team);
        entities.add(wither);
        withers.put(team, wither);
        witherDamageable.put(team, false);
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
        addStoreEntry(Material.IRON_SWORD, "Stone Sword", 500, "A dependable melee upgrade.");
        addStoreEntry(Material.GOLDEN_SWORD, "Frostbound Sword", 750, "Slows opponents on hit.");
        addStoreEntry(Material.IRON_HELMET, "Archer's Bane", 150, "Light protective upgrade.");
        addStoreEntry(Material.IRON_CHESTPLATE, "Archer's Bane Chestplate", 200, "Projectile protection.");
        addStoreEntry(Material.IRON_LEGGINGS, "Archer's Bane Leggings", 200, "Movement and defence upgrade.");
        addStoreEntry(Material.IRON_BOOTS, "Archer's Bane Boots", 150, "Movement upgrade.");
        addStoreEntry(Material.GOLDEN_APPLE, "Golden Applegate", 250, "A quick recovery item.");
        addStoreEntry(Material.POTION, "Small Health Potion", 100, "Restores health.");
        addStoreEntry(Material.ENDER_PEARL, "Ender Pearl", 750, "A short-range escape.");
        addStoreEntry(Material.BOW, "Hunter's Bow", 500, "A ranged weapon.");
    }

    private void addStoreEntry(Material material, String name, int cost, String description) {
        storeEntries.add(new StoreEntry(material, name, cost, description));
    }

    private static Location first(List<Location> locations) {
        return locations == null || locations.isEmpty() ? null : locations.get(0).clone();
    }

    private static int teamFromMarker(String marker) {
        String upper = marker.toUpperCase(Locale.ROOT);
        if (upper.matches(".*\\bRED\\b.*")) return 0;
        if (upper.matches(".*\\b(BLUE|AQUA|CYAN)\\b.*")) return 1;
        return -1;
    }

    private static boolean isFirstTowerMarker(String marker) {
        String[] parts = marker.trim().toUpperCase(Locale.ROOT).split("[ _-]+");
        return parts.length > 0 && "1".equals(parts[parts.length - 1]);
    }

    static boolean isMiddleTowerMarker(String marker) {
        String upper = marker == null ? "" : marker.toUpperCase(Locale.ROOT);
        return upper.contains("MIDDLE") || upper.contains("MIDLANE")
                || upper.contains("MID LANE") || upper.contains("CENTER")
                || upper.contains("CENTRE");
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
        if (objectiveItem == null && objectiveCarrier == null && now >= objectiveRespawnAt) {
            spawnObjectiveTnt();
        }
        for (KingState king : kings.values()) {
            if (king.entity != null && king.entity.isValid()) {
                king.entity.teleport(king.location);
                king.entity.setCustomName(teamColour(king.team) + king.name + ChatColor.RED
                        + " ❤ " + Math.max(0, king.health));
            }
        }
        checkTeamDeparture();
    }

    private void tickHeroesOfGwen(long now) {
        if (now >= nextGoldAt) {
            for (Player player : onlinePlayers()) addGold(player, 20, "Passive gold");
            nextGoldAt = now + MOBA_PASSIVE_GOLD_INTERVAL;
        }
        if (now >= nextStructureUpdateAt) {
            updateMobaTowers();
            nextStructureUpdateAt = now + MOBA_TOWER_UPDATE_INTERVAL;
        }
        checkTeamDeparture();
    }

    private void spawnObjectiveTnt() {
        if (objectiveTntSpawns.isEmpty()) return;
        Location spawn = objectiveTntSpawns.get(ThreadLocalRandom.current().nextInt(objectiveTntSpawns.size())).clone();
        Block platform = spawn.getBlock().getRelative(BlockFace.DOWN);
        rememberBlock(platform);
        platform.setType(Material.REDSTONE_BLOCK, false);
        objectiveItem = world.dropItem(spawn, new ItemStack(Material.TNT));
        objectiveItem.setPickupDelay(0);
        objectiveItem.setCustomName(ChatColor.RED + "Objective TNT");
        objectiveItem.setCustomNameVisible(true);
        objectiveItem.getPersistentDataContainer().set(objectiveTntKey, PersistentDataType.BYTE, (byte) 1);
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
        ItemStack helmet = player.getInventory().getHelmet();
        carriedTntOldHelmet.put(player.getUniqueId(), helmet == null ? null : helmet.clone());
        player.getInventory().setHelmet(namedItem(Material.TNT, ChatColor.RED + "Carrying Objective TNT",
                "Right-click to detonate at an enemy objective."));
        objectiveItem.remove();
        objectiveItem = null;
        for (Location location : objectiveTntSpawns) {
            Block block = location.getBlock().getRelative(BlockFace.DOWN);
            if (block.getType() == Material.REDSTONE_BLOCK) {
                rememberBlock(block);
                block.setType(Material.IRON_BLOCK, false);
            }
        }
        announce(ChatColor.GOLD + player.getName() + " picked up Objective TNT! Get to the enemy sentries.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (stopped || event.getHand() == null || event.getHand().name().equals("OFF_HAND")) return;
        Player player = event.getPlayer();
        if (!players.containsKey(player.getUniqueId())) return;
        if (game == ArcadePortGame.CASTLE_ASSAULT) {
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
        } else if (game == ArcadePortGame.HEROES_OF_GWEN
                && event.getAction().isRightClick() && event.getItem() != null
                && event.getItem().getType() == Material.GOLD_INGOT) {
            event.setCancelled(true);
            openStore(player);
        }
    }

    private void detonateObjectiveTnt(Player carrier) {
        restoreCarrierHelmet(carrier);
        objectiveCarrier = null;
        resolveCastleExplosion(carrier.getLocation().clone(), carrier, true, 4.0D);
        objectiveRespawnAt = System.currentTimeMillis() + objectiveDelay();
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
        if (owner != null && owner.isOnline()) resolveCastleExplosion(tnt.getLocation(), owner, false, 4.0D);
        entities.remove(tnt);
        tnt.remove();
    }

    private void resolveCastleExplosion(Location center, Player owner, boolean objective, double radius) {
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.CLOUD, center, 30, 0.65D, 0.65D, 0.65D, 0.08D);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 0.9F);
        int attackingTeam = teams.getOrDefault(owner.getUniqueId(), -1);
        for (Player target : onlinePlayers()) {
            if (target.equals(owner) && objective) continue;
            if (!objective && teams.getOrDefault(target.getUniqueId(), -2) == attackingTeam) continue;
            double distance = target.getLocation().distance(center);
            if (distance > radius) continue;
            double damage = 10.0D * Math.max(0.0D, 1.0D - distance / radius);
            if (damage <= 0.0D) continue;
            if (target.getHealth() <= damage) hooks.eliminate(target, owner, "was caught in a Castle Assault TNT blast");
            else target.setHealth(target.getHealth() - damage);
            Vector direction = target.getLocation().toVector().subtract(center.toVector());
            if (direction.lengthSquared() > 0.001D) target.setVelocity(direction.normalize().multiply(0.8D).setY(0.35D));
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
        attacker.sendMessage(ChatColor.GREEN + "Sentry crystal destroyed!");
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
            announce(ChatColor.GOLD + king.name + " has fallen. " + teamName(1 - king.team) + " wins Castle Assault!");
            hooks.finish(firstPlayerOnTeam(1 - king.team), 1 - king.team, "enemy king defeated");
        }
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
                for (Player teammate : onlinePlayers()) {
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
                int winnerTeam = 1 - entry.getKey();
                announce(ChatColor.GOLD + teamName(winnerTeam) + " destroyed the enemy Wither!");
                hooks.finish(firstPlayerOnTeam(winnerTeam), winnerTeam, "enemy Wither defeated");
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
        for (Player player : onlinePlayers()) {
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
        StoreHolder holder = new StoreHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, ChatColor.DARK_GREEN + "Heroes of Gwen Store");
        for (int i = 0; i < storeEntries.size() && i < 27; i++) {
            StoreEntry entry = storeEntries.get(i);
            int price = ArcadePortRules.heroesOfGwenStoreItemCost(entry.cost, mixedArcade);
            ItemStack item = new ItemStack(entry.material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + entry.name);
            meta.setLore(List.of(ChatColor.GRAY + entry.description,
                    ChatColor.WHITE + "Cost: " + ChatColor.GOLD + price + " gold",
                    ChatColor.YELLOW + "Click to purchase"));
            meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.INTEGER, i);
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }
        openStores.put(player.getUniqueId(), holder);
        player.openInventory(inventory);
        updateGoldActionBar(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onStoreClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !openStores.containsKey(player.getUniqueId())
                || !event.getView().getTopInventory().getHolder().equals(openStores.get(player.getUniqueId()))) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        Integer index = clicked.getItemMeta().getPersistentDataContainer().get(shopItemKey, PersistentDataType.INTEGER);
        if (index == null || index < 0 || index >= storeEntries.size()) return;
        purchaseStoreItem(player, storeEntries.get(index));
    }

    @EventHandler
    public void onStoreClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) openStores.remove(player.getUniqueId());
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
        if (entry.material == Material.GOLDEN_APPLE) {
            player.getInventory().addItem(reward);
        } else if (entry.material == Material.POTION || entry.material == Material.ENDER_PEARL || entry.material == Material.BOW) {
            player.getInventory().addItem(reward);
        } else {
            switch (entry.material) {
                case IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> equip(player, reward);
                default -> player.getInventory().addItem(reward);
            }
        }
        player.sendMessage(ChatColor.GREEN + "Purchased " + entry.name + " for " + cost + " gold.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
        openStore(player);
    }

    private void equip(Player player, ItemStack item) {
        switch (item.getType()) {
            case IRON_HELMET -> player.getInventory().setHelmet(item);
            case IRON_CHESTPLATE -> player.getInventory().setChestplate(item);
            case IRON_LEGGINGS -> player.getInventory().setLeggings(item);
            case IRON_BOOTS -> player.getInventory().setBoots(item);
            default -> player.getInventory().addItem(item);
        }
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
        for (int team = 0; team < 2; team++) {
            boolean hasOnlinePlayer = players.values().stream().anyMatch(player -> player.isOnline()
                    && teams.getOrDefault(player.getUniqueId(), -1) == team);
            if (!hasOnlinePlayer) {
                int winner = 1 - team;
                hooks.finish(firstPlayerOnTeam(winner), winner, "opposing team left the match");
                return;
            }
        }
    }

    private List<Player> onlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (Player player : players.values()) if (player != null && player.isOnline()) result.add(player);
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

    void onPlayerKill(Player killer) {
        if (stopped || game != ArcadePortGame.CASTLE_ASSAULT || killer == null) return;
        if (objectiveKillsEnabled == 0) return;
        int team = teams.getOrDefault(killer.getUniqueId(), -1);
        if (team < 0) return;
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

    void stop() {
        if (stopped) return;
        stopped = true;
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
        final Material material;
        final String name;
        final int cost;
        final String description;
        StoreEntry(Material material, String name, int cost, String description) {
            this.material = material;
            this.name = name;
            this.cost = cost;
            this.description = description;
        }
    }

    private static final class StoreHolder implements InventoryHolder {
        final UUID owner;
        StoreHolder(UUID owner) { this.owner = owner; }
        @Override public Inventory getInventory() { return null; }
    }
}
