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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Mineplex Reborn Sneaky Assassins mechanics adapted for the isolated Paper 26.3 harness.
 *
 * The upstream repository does not include a Sneaky Assassins map archive, so this port
 * builds a clearly isolated town test arena and uses authored test spawn/power-up points.
 */
final class SneakyAssassinsMechanics {
    static final int SOURCE_NPC_COUNT = 200;
    static final long GLOBAL_REVEAL_INTERVAL_MILLIS = 60_000L;
    static final long GLOBAL_REVEAL_DURATION_MILLIS = 4_000L;
    static final long REVEALER_DURATION_MILLIS = 5_000L;
    static final long SMOKE_CLOAK_DURATION_MILLIS = 1_200L;
    static final long BRIBE_COOLDOWN_MILLIS = 8_000L;
    static final long FIRST_POWERUP_DELAY_MILLIS = SneakyAssassinsRules.firstPowerUpDelayMillis();
    static final long NEXT_POWERUP_DELAY_MILLIS = SneakyAssassinsRules.nextPowerUpDelayMillis();
    static final int MAX_ACTIVE_POWERUPS = SneakyAssassinsRules.maxActivePowerUps();
    static final double REVEALER_RADIUS = 12.0D;
    static final double SMOKE_RADIUS = 5.0D;

    @FunctionalInterface
    interface Eliminator {
        void eliminate(Player victim, Player killer, String reason);
    }

    private final ArcadeMechanicsModernPlugin plugin;
    private final Predicate<UUID> isTestPlayer;
    private final NamespacedKey itemKey;
    private final Map<UUID, String> kitPreferences = new HashMap<>();

    private World world;
    private final Map<UUID, Player> players = new LinkedHashMap<>();
    private Predicate<UUID> isAlive = ignored -> false;
    private Eliminator eliminator = (victim, killer, reason) -> { };
    private final Set<UUID> npcIds = new HashSet<>();
    private final Map<UUID, Villager> disguises = new HashMap<>();
    private final Map<UUID, Set<UUID>> hiddenViewers = new HashMap<>();
    private final Map<UUID, Long> revealUntil = new HashMap<>();
    private final Map<UUID, Long> cloakUntil = new HashMap<>();
    private final Map<UUID, Long> bribeReadyAt = new HashMap<>();
    private final Map<UUID, Bribe> bribes = new HashMap<>();
    private final Map<UUID, Long> npcNextAttackAt = new HashMap<>();
    private final List<Location> npcPatrolAnchors = new ArrayList<>();
    private final Map<UUID, Location> npcPatrolTargets = new HashMap<>();
    private final Map<UUID, Location> npcLastMovementLocations = new HashMap<>();
    private final Map<UUID, Long> npcLastMovementAt = new HashMap<>();
    private final Map<UUID, Item> revealerProjectiles = new HashMap<>();
    private final Map<UUID, UUID> revealerOwners = new HashMap<>();
    private final Map<UUID, Location> revealerLastLocations = new HashMap<>();
    private final Map<UUID, Double> pendingProjectileDamage = new HashMap<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final Map<UUID, Integer> powerUpCounts = new HashMap<>();
    private final Map<UUID, Long> masterCompassUpdateAt = new HashMap<>();
    private final Set<UUID> masterAssassins = new HashSet<>();
    private final Map<UUID, UUID> pendingCompassKills = new HashMap<>();
    private final Set<UUID> rangedAssassinMeleeDamage = new HashSet<>();
    private final Set<UUID> escapeArtistSmokeUsed = new HashSet<>();
    private final Map<UUID, Set<UUID>> revealerTargets = new HashMap<>();
    private final Map<UUID, Long> lastPowerUpCollectedAt = new HashMap<>();
    private long nextPowerUpAt;
    private long nextGlobalRevealAt;
    private long globalRevealEndsAt;
    private long lastHudUpdateAt;
    private int globalRevealWarning = -1;
    private Scoreboard scoreboard;
    private Objective scoreboardObjective;
    private final Map<UUID, Team> playerScoreboardTeams = new HashMap<>();
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
    private Location lastPowerUpLocation;
    private int tick;

    SneakyAssassinsMechanics(ArcadeMechanicsModernPlugin plugin, Predicate<UUID> isTestPlayer) {
        this.plugin = plugin;
        this.isTestPlayer = isTestPlayer == null ? ignored -> false : isTestPlayer;
        this.itemKey = new NamespacedKey(plugin, "sneaky-assassins-item");
    }

    static String normalizeKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace(' ', '-');
        while (kit.contains("--")) kit = kit.replace("--", "-");
        return switch (kit) {
            case "escape", "escape-artist", "escapeartist" -> "escape-artist";
            case "assassin", "archer" -> "assassin";
            case "briber", "bribe" -> "briber";
            case "revealer", "reveal" -> "revealer";
            default -> null;
        };
    }

    void selectKit(Player player, String requestedKit) {
        if (player == null) return;
        String kit = normalizeKit(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Sneaky Assassins kits: escape-artist, assassin, briber, revealer.");
            return;
        }
        if (players.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Sneaky Assassins kits are locked once the round starts.");
            return;
        }
        kitPreferences.put(player.getUniqueId(), kit);
        player.sendMessage(ChatColor.GREEN + "Sneaky Assassins kit set to " + kit.replace('-', ' ') + ".");
    }

    String selectedKit(UUID playerId) {
        return kitPreferences.getOrDefault(playerId, "escape-artist");
    }

    String statusText(UUID playerId) {
        int count = powerUpCounts.getOrDefault(playerId, 0);
        long now = System.currentTimeMillis();
        int seconds = globalRevealEndsAt > now ? 0
                : (int) Math.max(0L, (nextGlobalRevealAt - now + 999L) / 1_000L);
        return "power-ups " + count + "/5 | "
                + (masterAssassins.contains(playerId) ? "MASTER" : "reveal " + seconds + "s");
    }

    void buildArena(World world) {
        for (int x = -22; x <= 22; x++) {
            for (int z = -22; z <= 22; z++) {
                world.getBlockAt(x, 63, z).setType(Material.STONE_BRICKS, false);
                if (Math.abs(x) == 22 || Math.abs(z) == 22) {
                    for (int y = 64; y <= 67; y++) {
                        world.getBlockAt(x, y, z).setType(Material.DARK_OAK_FENCE, false);
                    }
                }
                for (int y = 64; y <= 69; y++) {
                    if (Math.abs(x) != 22 && Math.abs(z) != 22) {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
        }
        // A compact market-town layout: buildings frame the lanes but leave clear sightlines.
        buildHouse(world, -12, -12, Material.OAK_PLANKS, Material.BRICKS);
        buildHouse(world, 12, -12, Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS);
        buildHouse(world, -12, 12, Material.BIRCH_PLANKS, Material.STONE_BRICKS);
        buildHouse(world, 12, 12, Material.DARK_OAK_PLANKS, Material.DEEPSLATE_BRICKS);
        for (int x = -21; x <= 21; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(x, 63, z).setType(Material.POLISHED_ANDESITE, false);
            }
        }
        for (int z = -21; z <= 21; z++) {
            for (int x = -2; x <= 2; x++) {
                world.getBlockAt(x, 63, z).setType(Material.POLISHED_ANDESITE, false);
            }
        }
        for (int x = -18; x <= 18; x += 6) {
            for (int z = -18; z <= 18; z += 6) {
                if (Math.abs(x) < 8 && Math.abs(z) < 8) continue;
                world.getBlockAt(x, 64, z).setType(Material.LANTERN, false);
            }
        }
    }

    private void buildHouse(World world, int cx, int cz, Material wall, Material roof) {
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                world.getBlockAt(x, 63, z).setType(Material.SPRUCE_PLANKS, false);
                for (int y = 64; y <= 67; y++) {
                    boolean edge = Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4;
                    world.getBlockAt(x, y, z).setType(edge ? wall : Material.AIR, false);
                }
                world.getBlockAt(x, 68, z).setType(roof, false);
            }
        }
        world.getBlockAt(cx, 64, cz - 4).setType(Material.AIR, false);
        world.getBlockAt(cx, 65, cz - 4).setType(Material.AIR, false);
        world.getBlockAt(cx - 2, 65, cz - 4).setType(Material.GLASS_PANE, false);
        world.getBlockAt(cx + 2, 65, cz - 4).setType(Material.GLASS_PANE, false);
    }

    List<Location> spawnLocations(World world, int count) {
        List<Location> locations = new ArrayList<>();
        int amount = Math.max(1, count);
        for (int i = 0; i < amount; i++) {
            double angle = 2.0D * Math.PI * i / amount;
            double radius = 16.0D + (i % 2) * 1.5D;
            Location spawn = new Location(world, Math.cos(angle) * radius, 64.0D,
                    Math.sin(angle) * radius, (float) (angle * 180.0D / Math.PI + 90.0D), 0.0F);
            locations.add(spawn);
        }
        return locations;
    }

    void giveKit(Player player) {
        if (player == null) return;
        String kit = selectedKit(player.getUniqueId());
        player.getInventory().clear();
        player.getInventory().setItemInOffHand(null);
        player.getInventory().setArmorContents(new ItemStack[]{
                new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS),
                new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET)
        });
        player.getInventory().setItem(0, new ItemStack(Material.WOODEN_SWORD));
        player.getInventory().addItem(createSmokeBomb());
        switch (kit) {
            case "assassin" -> {
                player.getInventory().addItem(new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 32));
                player.sendMessage(ChatColor.GRAY + "Assassin: bow, 32 arrows and your smoke bomb.");
            }
            case "briber" -> {
                player.getInventory().addItem(new ItemStack(Material.EMERALD, 4));
                player.sendMessage(ChatColor.GRAY + "Briber: right-click a villager with an emerald to make it attack your enemies.");
            }
            case "revealer" -> {
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
                player.sendMessage(ChatColor.GRAY + "Revealer: throw a diamond to reveal nearby players for 5 seconds.");
            }
            default -> {
                player.getInventory().addItem(createSmokeBomb(2));
                player.sendMessage(ChatColor.GRAY + "Escape Artist: three smoke bombs total.");
            }
        }
        player.updateInventory();
    }

    private ItemStack createSmokeBomb() {
        return createSmokeBomb(1);
    }

    private ItemStack createSmokeBomb(int amount) {
        ItemStack item = new ItemStack(Material.INK_SAC, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Smoke Bomb");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to cloak yourself for 1.2 seconds.",
                ChatColor.GRAY + "Blinds nearby living entities for 3 seconds."));
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "smoke-bomb");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMasterCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Master Assassin Compass");
        meta.setLore(List.of(ChatColor.GRAY + "Tracks the nearest living assassin."));
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "master-compass");
        item.setItemMeta(meta);
        return item;
    }

    void start(World world, List<Player> participants, Predicate<UUID> alive, Eliminator eliminator) {
        stop();
        this.world = world;
        this.isAlive = alive;
        this.eliminator = eliminator;
        for (Player player : participants) {
            if (player == null || !player.isOnline()) continue;
            players.put(player.getUniqueId(), player);
            powerUpCounts.put(player.getUniqueId(), 0);
            setDisguised(player);
        }
        prepareNpcPatrolAnchors();
        for (int i = 0; i < SOURCE_NPC_COUNT; i++) spawnNpc();
        long now = System.currentTimeMillis();
        nextPowerUpAt = now + FIRST_POWERUP_DELAY_MILLIS;
        nextGlobalRevealAt = now + GLOBAL_REVEAL_INTERVAL_MILLIS;
        setupScoreboard();
        announce(ChatColor.GOLD + "Sneaky Assassins: stay disguised, collect power-ups and be the last assassin alive!");
        plugin.getLogger().info("Sneaky Assassins started with " + players.size()
                + " players and " + npcIds.size() + " town NPCs.");
    }

    void tick() {
        if (world == null) return;
        tick++;
        long now = System.currentTimeMillis();
        updateDisguises(now);
        updateRevealCycle(now);
        updateNpcMovement(now);
        updatePowerUps(now);
        updateMasterCompasses(now);
        if (now >= lastHudUpdateAt) {
            lastHudUpdateAt = now + 1_000L;
            updateHud(now);
        }
    }

    private void updateHud(long now) {
        long revealRemaining = Math.max(0L, nextGlobalRevealAt - now);
        int seconds = globalRevealEndsAt > now
                ? 0 : (int) ((revealRemaining + 999L) / 1_000L);
        updateScoreboard(seconds);
        for (UUID id : players.keySet()) {
            Player player = players.get(id);
            if (player == null || !player.isOnline() || !isAlive.test(id)) continue;
            int count = powerUpCounts.getOrDefault(id, 0);
            player.sendActionBar(ChatColor.GOLD + "Power-ups: " + count + "/5"
                    + ChatColor.DARK_GRAY + "  |  " + ChatColor.YELLOW
                    + (masterAssassins.contains(id) ? "MASTER" : "Reveal: " + seconds + "s"));
        }
    }

    private void setupScoreboard() {
        if (Bukkit.getScoreboardManager() == null) return;
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboardObjective = scoreboard.registerNewObjective("sneaky", "dummy", ChatColor.GOLD + "Sneaky Assassins");
        scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (Player player : players.values()) {
            if (player != null && player.isOnline()) {
                previousScoreboards.put(player.getUniqueId(), player.getScoreboard());
                player.setScoreboard(scoreboard);
            }
        }
    }

    private void updateScoreboard(int revealSeconds) {
        if (scoreboard == null || scoreboardObjective == null) return;
        for (Team team : new ArrayList<>(playerScoreboardTeams.values())) {
            for (String entry : new HashSet<>(team.getEntries())) team.removeEntry(entry);
            team.unregister();
        }
        playerScoreboardTeams.clear();
        for (String entry : new HashSet<>(scoreboard.getEntries())) scoreboard.resetScores(entry);
        List<Player> all = players.values().stream().filter(p -> p != null && p.isOnline()).toList();
        List<Player> alive = all.stream().filter(p -> isAlive.test(p.getUniqueId())).toList();
        List<Player> dead = all.stream().filter(p -> !isAlive.test(p.getUniqueId())).toList();
        int line = 15;
        if (all.size() < 13 || (alive.size() < 13 && alive.size() > 0)) {
            List<Player> shown = all.size() < 13 ? all : alive;
            int teamIndex = 0;
            for (Player player : shown) {
                UUID id = player.getUniqueId();
                String name = player.getName();
                Team team = scoreboard.registerNewTeam("sa" + teamIndex++);
                team.setPrefix(isAlive.test(id) ? ChatColor.GREEN.toString() : ChatColor.GRAY.toString());
                team.setSuffix(ChatColor.WHITE + "  " + powerUpCounts.getOrDefault(id, 0));
                team.addEntry(name);
                playerScoreboardTeams.put(id, team);
                scoreboardObjective.getScore(name).setScore(line--);
            }
        } else {
            addScoreboardLine(ChatColor.GREEN + "Players Alive", alive.size(), line--);
            addScoreboardLine(ChatColor.RED + "Players Dead", dead.size(), line--);
        }
        addScoreboardLine(ChatColor.YELLOW + "Player Reveal", Math.max(0, revealSeconds), line--);
    }

    private void addScoreboardLine(String label, int value, int score) {
        String entry = label;
        scoreboardObjective.getScore(entry).setScore(score);
        String valueEntry = ChatColor.WHITE + String.valueOf(value);
        // Preserve uniqueness when a player name happens to match a display line.
        if (scoreboard.getEntries().contains(valueEntry)) valueEntry += ChatColor.RESET;
        scoreboardObjective.getScore(valueEntry).setScore(score - 1);
    }

    private void updateRevealCycle(long now) {
        if (globalRevealEndsAt > 0L) {
            if (now >= globalRevealEndsAt) {
                globalRevealEndsAt = 0L;
                for (UUID id : players.keySet()) {
                    if (isAlive.test(id) && revealUntil.getOrDefault(id, 0L) <= now) {
                        restoreDisguise(id);
                        Player player = players.get(id);
                        if (player != null) player.removePotionEffect(PotionEffectType.WITHER);
                    }
                }
                nextGlobalRevealAt = now + GLOBAL_REVEAL_INTERVAL_MILLIS;
                globalRevealWarning = -1;
            }
            return;
        }
        long remaining = nextGlobalRevealAt - now;
        int warning = (int) Math.ceil(remaining / 1_000.0D);
        if (warning >= 1 && warning <= 3 && warning != globalRevealWarning) {
            globalRevealWarning = warning;
            announce(ChatColor.RED + "Players revealed in " + warning + "...");
        }
        if (remaining <= 0L) {
            announce(ChatColor.RED + "Players Revealed!");
            for (UUID id : players.keySet()) if (isAlive.test(id)) revealPlayer(id, now + GLOBAL_REVEAL_DURATION_MILLIS);
            globalRevealEndsAt = now + GLOBAL_REVEAL_DURATION_MILLIS;
        }
    }

    private void updateDisguises(long now) {
        for (UUID id : new ArrayList<>(players.keySet())) {
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            if (!isAlive.test(id)) {
                removeDisguise(id);
                continue;
            }
            Villager proxy = disguises.get(id);
            if (proxy != null && proxy.isValid()) {
                Location target = player.getLocation().clone();
                if (proxy.getWorld() != target.getWorld()
                        || proxy.getLocation().distanceSquared(target) > 0.04D
                        || Math.abs(proxy.getLocation().getYaw() - target.getYaw()) > 3.0F) {
                    proxy.teleport(target);
                }
                proxy.setInvisible(cloakUntil.getOrDefault(id, 0L) > now);
            }
            Long expiry = revealUntil.get(id);
            if (expiry != null && expiry <= now && globalRevealEndsAt <= now) {
                revealUntil.remove(id);
                restoreDisguise(id);
                player.removePotionEffect(PotionEffectType.WITHER);
            }
            if (cloakUntil.getOrDefault(id, 0L) <= now) cloakUntil.remove(id);
        }
    }

    private void revealPlayer(UUID id, long until) {
        Player player = players.get(id);
        if (player == null || !player.isOnline()) return;
        removeDisguise(id);
        revealUntil.put(id, until);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,
                Math.max(1, (int) ((until - System.currentTimeMillis()) / 50L)), 0, false, false, true));
    }

    private void setDisguised(Player player) {
        if (world == null || player == null || !player.isOnline() || !isAlive.test(player.getUniqueId())
                || revealUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()
                || globalRevealEndsAt > System.currentTimeMillis()) return;
        UUID id = player.getUniqueId();
        removeDisguise(id);
        Villager proxy = world.spawn(player.getLocation(), Villager.class);
        proxy.setAI(false);
        proxy.setSilent(true);
        proxy.setPersistent(true);
        proxy.setRemoveWhenFarAway(false);
        proxy.setCollidable(true);
        proxy.setInvulnerable(false);
        proxy.setCanPickupItems(false);
        proxy.setGravity(false);
        proxy.setCustomNameVisible(false);
        disguises.put(id, proxy);
        Set<UUID> viewers = hiddenViewers.computeIfAbsent(id, ignored -> new HashSet<>());
        for (Player viewer : players.values()) {
            if (viewer == null || !viewer.isOnline() || viewer.equals(player)) continue;
            if (viewer.canSee(player)) {
                viewer.hidePlayer(plugin, player);
                viewers.add(viewer.getUniqueId());
            }
        }
    }

    private void restoreDisguise(UUID id) {
        Player player = players.get(id);
        if (player == null || !player.isOnline() || !isAlive.test(id)) return;
        if (revealUntil.getOrDefault(id, 0L) > System.currentTimeMillis()
                || globalRevealEndsAt > System.currentTimeMillis()) return;
        removeDisguise(id);
        setDisguised(player);
    }

    private void removeDisguise(UUID id) {
        Villager proxy = disguises.remove(id);
        if (proxy != null && proxy.isValid()) proxy.remove();
        Set<UUID> viewers = hiddenViewers.remove(id);
        if (viewers == null) return;
        Player owner = players.get(id);
        if (owner == null) return;
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) viewer.showPlayer(plugin, owner);
        }
    }

    Player disguiseOwner(Entity entity) {
        if (entity == null) return null;
        UUID id = entity.getUniqueId();
        for (Map.Entry<UUID, Villager> entry : disguises.entrySet()) {
            Villager disguise = entry.getValue();
            if (disguise != null && disguise.getUniqueId().equals(id)) {
                Player owner = players.get(entry.getKey());
                return owner != null && owner.isOnline() && isAlive.test(entry.getKey()) ? owner : null;
            }
        }
        return null;
    }

    private void spawnNpc() {
        if (world == null) return;
        Location location = randomSafeTownLocation();
        if (location == null) return;
        Villager npc = world.spawn(location, Villager.class);
        npc.setAI(false);
        npc.setSilent(true);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);
        npc.setCanPickupItems(false);
        npc.setCollidable(true);
        npcIds.add(npc.getUniqueId());
    }

    /**
     * The source reads nearby locations from the town's team-zero spawn group.
     * The real archive is absent in this port, so build deterministic waypoints
     * from clear, supported cells in the isolated procedural town.
     */
    private void prepareNpcPatrolAnchors() {
        npcPatrolAnchors.clear();
        for (int x = -20; x <= 20; x += 2) {
            for (int z = -20; z <= 20; z += 2) {
                // Match the four procedural house footprints; NPC patrol points stay on the lanes.
                if ((Math.abs(x) >= 8 && Math.abs(x) <= 16)
                        && (Math.abs(z) >= 8 && Math.abs(z) <= 16)) continue;
                Location candidate = new Location(world, x + 0.5D, 64.0D, z + 0.5D);
                if (isSafeNpcPatrolDestination(candidate)) npcPatrolAnchors.add(candidate);
            }
        }
    }

    private boolean isSafeNpcPatrolDestination(Location location) {
        if (world == null || location == null || location.getWorld() != world) return false;
        Block floor = location.getBlock().getRelative(0, -1, 0);
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        return SneakyAssassinsRules.isSafeNpcPatrolDestination(
                floor.getType().isSolid(), floor.isLiquid(), feet.isPassable(), head.isPassable());
    }

    /** Reborn picks one nearby town spawn, then tries at most five random 5x5 offsets. */
    private Location nextNpcPatrolTarget(Location npcLocation) {
        List<Location> nearby = npcPatrolAnchors.stream()
                .filter(anchor -> SneakyAssassinsRules.isNpcPatrolAnchorNearby(
                        npcLocation.getX(), npcLocation.getZ(), anchor.getX(), anchor.getZ()))
                .toList();
        if (nearby.isEmpty()) return null;

        Location anchor = nearby.get(ThreadLocalRandom.current().nextInt(nearby.size()));
        for (int attempt = 0; attempt < 5; attempt++) {
            int dx = ThreadLocalRandom.current().nextInt(5) - 2;
            int dz = ThreadLocalRandom.current().nextInt(5) - 2;
            Location candidate = anchor.clone().add(dx, 0.0D, dz);
            if (isSafeNpcPatrolDestination(candidate)) return candidate;
        }
        return anchor.clone();
    }

    private Location randomSafeTownLocation() {
        for (int attempt = 0; attempt < 80; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(-20, 21);
            int z = ThreadLocalRandom.current().nextInt(-20, 21);
            if ((Math.abs(x) >= 8 && Math.abs(x) <= 16) && (Math.abs(z) >= 8 && Math.abs(z) <= 16)) continue;
            Block floor = world.getHighestBlockAt(x, z);
            if (!floor.getType().isSolid()) continue;
            Location feet = floor.getLocation().add(0.5D, 1.0D, 0.5D);
            if (feet.getBlock().isPassable() && feet.clone().add(0.0D, 1.0D, 0.0D).getBlock().isPassable()) return feet;
        }
        return new Location(world, 0.5D, 64.0D, 8.5D);
    }

    private void updateNpcMovement(long now) {
        for (UUID id : new ArrayList<>(npcIds)) {
            Entity entity = Bukkit.getEntity(id);
            boolean validVillager = entity instanceof Villager npc && npc.isValid();
            if (SneakyAssassinsRules.shouldPruneTownNpc(entity != null, validVillager)) {
                npcIds.remove(id);
                bribes.remove(id);
                npcNextAttackAt.remove(id);
                npcPatrolTargets.remove(id);
                npcLastMovementLocations.remove(id);
                npcLastMovementAt.remove(id);
                continue;
            }

            Villager npc = (Villager) entity;
            Bribe bribe = bribes.get(id);
            if (bribe != null) {
                Player target = nearestEnemy(npc.getLocation(), bribe.owner);
                if (target == null) {
                    npcPatrolTargets.remove(id);
                    continue;
                }

                Vector direction = target.getLocation().toVector().subtract(npc.getLocation().toVector());
                double distance = direction.length();
                if (distance <= 1.5D) {
                    long ready = npcNextAttackAt.getOrDefault(id, 0L);
                    if (now >= ready) {
                        npcNextAttackAt.put(id, now + 1_000L);
                        applyDamage(target, 1.0D, bribe.owner, "was attacked by a bribed villager");
                        npc.getWorld().playSound(npc.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 0.8F);
                    }
                } else if (distance > 0.001D) {
                    moveNpcTowards(npc, target.getLocation(), 0.8D, now, true);
                }
                continue;
            }

            Location patrolTarget = npcPatrolTargets.get(id);
            if (patrolTarget == null || npc.getLocation().distanceSquared(patrolTarget) < 1.0D
                    || SneakyAssassinsRules.shouldIssueNpcPatrolOrder(
                            ThreadLocalRandom.current().nextInt(40))) {
                patrolTarget = nextNpcPatrolTarget(npc.getLocation());
                if (patrolTarget == null) {
                    npcPatrolTargets.remove(id);
                    continue;
                }
                npcPatrolTargets.put(id, patrolTarget);
            }
            moveNpcTowards(npc, patrolTarget, 0.7D, now, false);
        }
    }

    /**
     * A velocity-steered path approximation for the generated test town. The
     * source uses the NMS Creature navigation API after its goal selectors have
     * been cleared; the modern harness has no equivalent archived patrol points
     * or NMS navigation shim, so it steers toward the selected safe waypoint.
     */
    private void moveNpcTowards(Villager npc, Location target, double speed, long now, boolean recoverIfStuck) {
        UUID id = npc.getUniqueId();
        Location current = npc.getLocation();
        if (recoverIfStuck) {
            Location last = npcLastMovementLocations.get(id);
            long lastMovedAt = npcLastMovementAt.getOrDefault(id, now);
            if (last == null || current.distanceSquared(last) > 2.25D) {
                npcLastMovementLocations.put(id, current.clone());
                npcLastMovementAt.put(id, now);
            } else if (SneakyAssassinsRules.shouldRecoverBribedNpc(now, lastMovedAt)) {
                Vector recovery = target.toVector().subtract(current.toVector());
                recovery.setY(0.0D);
                if (recovery.lengthSquared() < 0.001D) recovery = new Vector(1.0D, 0.0D, 0.0D);
                recovery.normalize().multiply(0.3D).setY(0.3D);
                npc.setVelocity(recovery);
                npcLastMovementLocations.put(id, current.clone());
                npcLastMovementAt.put(id, now);
                return;
            }
        }

        Vector direction = target.toVector().subtract(current.toVector());
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.001D) return;
        Vector motion = direction.normalize().multiply(speed * 0.18D);
        motion.setY(Math.max(-0.05D, Math.min(0.12D, npc.getVelocity().getY())));
        npc.setVelocity(motion);
    }

    private Player nearestEnemy(Location location, Player owner) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player candidate : players.values()) {
            if (candidate == null || !candidate.isOnline() || !isAlive.test(candidate.getUniqueId())
                    || candidate.equals(owner) || cloakUntil.getOrDefault(candidate.getUniqueId(), 0L) > System.currentTimeMillis()) continue;
            double distance = candidate.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    void handlePlayerInteract(PlayerInteractEvent event) {
        if (world == null || event.getPlayer().getWorld() != world
                || !players.containsKey(event.getPlayer().getUniqueId())
                || !isAlive.test(event.getPlayer().getUniqueId())
                || !event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        ItemStack held = event.getItem();
        if (held == null) return;
        String itemType = held.getItemMeta() == null ? null
                : held.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if ("smoke-bomb".equals(itemType)) {
            event.setCancelled(true);
            consumeHeld(player);
            useSmokeBomb(player);
        } else if (held.getType() == Material.DIAMOND && "revealer".equals(selectedKit(player.getUniqueId()))) {
            event.setCancelled(true);
            consumeHeld(player);
            throwRevealer(player);
        }
    }

    void handleEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (world == null || player.getWorld() != world || !players.containsKey(player.getUniqueId())) return;
        Entity clicked = event.getRightClicked();
        if (clicked instanceof Hanging) {
            event.setCancelled(true);
            return;
        }
        if (!(clicked instanceof Villager villager)) return;
        // Source blocks villager trading/interactions even when the player is not bribing.
        event.setCancelled(true);
        if (!isAlive.test(player.getUniqueId()) || !npcIds.contains(villager.getUniqueId())
                || !"briber".equals(selectedKit(player.getUniqueId()))
                || player.getInventory().getItemInMainHand().getType() != Material.EMERALD) return;
        long now = System.currentTimeMillis();
        long ready = bribeReadyAt.getOrDefault(player.getUniqueId(), 0L);
        if (now < ready) {
            player.sendMessage(ChatColor.YELLOW + "Bribe ready in "
                    + ((ready - now + 999L) / 1_000L) + "s.");
            return;
        }
        bribeReadyAt.put(player.getUniqueId(), now + BRIBE_COOLDOWN_MILLIS);
        consumeHeld(player);
        bribes.put(villager.getUniqueId(), new Bribe(player));
        villager.setCustomName(ChatColor.GOLD + "Bribed");
        villager.setCustomNameVisible(true);
        villager.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, villager.getLocation().add(0, 1, 0),
                12, 0.35D, 0.35D, 0.35D, 0.02D);
        player.sendMessage(ChatColor.GREEN + "Villager bribed! It will attack your nearest enemy until it is removed.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0F, 1.1F);
    }

    private void useSmokeBomb(Player player) {
        if ("escape-artist".equals(selectedKit(player.getUniqueId()))) {
            escapeArtistSmokeUsed.add(player.getUniqueId());
        }
        long now = System.currentTimeMillis();
        cloakUntil.put(player.getUniqueId(), now + SMOKE_CLOAK_DURATION_MILLIS);
        Villager proxy = disguises.get(player.getUniqueId());
        if (proxy != null && proxy.isValid()) proxy.setInvisible(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 24, 0, false, false, false));
        for (Entity nearby : player.getNearbyEntities(SMOKE_RADIUS, SMOKE_RADIUS, SMOKE_RADIUS)) {
            if (!(nearby instanceof LivingEntity living) || nearby.equals(player)) continue;
            if (nearby instanceof Player target && !isAlive.test(target.getUniqueId())) continue;
            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false, true));
        }
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0),
                38, 1.0D, 0.6D, 1.0D, 0.08D);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0),
                26, 0.85D, 0.7D, 0.85D, 0.04D);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.2F, 0.65F);
        player.sendMessage(ChatColor.GREEN + "Smoke Bomb used!");
    }

    private void throwRevealer(Player player) {
        Item diamond = world.dropItem(player.getEyeLocation(), new ItemStack(Material.DIAMOND));
        diamond.setPickupDelay(32_767);
        diamond.setVelocity(player.getLocation().getDirection().normalize().multiply(1.2D)
                .add(new Vector(0.0D, 0.2D, 0.0D)));
        revealerProjectiles.put(diamond.getUniqueId(), diamond);
        revealerOwners.put(diamond.getUniqueId(), player.getUniqueId());
        revealerLastLocations.put(diamond.getUniqueId(), diamond.getLocation().clone());
        player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 0.9F, 0.8F);
    }

    private void updatePowerUps(long now) {
        if (powerUps.size() < MAX_ACTIVE_POWERUPS && now >= nextPowerUpAt) {
            if (spawnPowerUp(now)) nextPowerUpAt = now + NEXT_POWERUP_DELAY_MILLIS;
        }
        for (PowerUp powerUp : new ArrayList<>(powerUps)) {
            if (powerUp.item != null && (!powerUp.item.isValid() || powerUp.item.isDead())) {
                removePowerUp(powerUp);
                continue;
            }
            if (powerUp.item == null) {
                long age = now - powerUp.spawnedAt;
                double y = Math.max(65.0D, 250.0D - age / 25.0D);
                Location effect = powerUp.location.clone();
                effect.setY(y);
                if (tick % 2 == 0) {
                    world.spawnParticle(Particle.END_ROD, effect, 4, 0.12D, 0.12D, 0.12D, 0.01D);
                    world.spawnParticle(Particle.FIREWORK, effect, 2, 0.1D, 0.1D, 0.1D, 0.01D);
                }
                if (y <= 65.0D) {
                    powerUp.item = world.spawn(powerUp.location.clone().add(0.5D, 0.5D, 0.5D), Skeleton.class);
                    powerUp.item.setAI(false);
                    powerUp.item.setSilent(true);
                    powerUp.item.setPersistent(true);
                    powerUp.item.setRemoveWhenFarAway(false);
                    powerUp.item.setCollidable(false);
                    powerUp.item.setInvulnerable(true);
                    powerUp.item.setInvisible(true);
                    // The source replaces the centre beacon block with glass once the power-up lands.
                    world.getBlockAt(powerUp.location.getBlockX(), 63, powerUp.location.getBlockZ())
                            .setType(Material.GLASS, false);
                    EntityEquipment equipment = powerUp.item.getEquipment();
                    if (equipment != null) {
                        equipment.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                        equipment.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
                        equipment.setChestplateDropChance(0.0F);
                        equipment.setItemInMainHandDropChance(0.0F);
                    }
                }
                continue;
            }
            Location orbit = powerUp.item.getLocation().clone();
            double phase = powerUp.item.getTicksLived() / 4.0D;
            orbit.add(Math.sin(phase), Math.cos(powerUp.item.getTicksLived() / 7.0D) + 1.0D,
                    Math.cos(phase));
            world.spawnParticle(Particle.HAPPY_VILLAGER, orbit, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            for (Player player : players.values()) {
                if (player == null || !player.isOnline() || !isAlive.test(player.getUniqueId())) continue;
                if (player.getLocation().distanceSquared(powerUp.item.getLocation()) <= 1.0D) {
                    collectPowerUp(player);
                    removePowerUp(powerUp);
                    nextPowerUpAt = now + NEXT_POWERUP_DELAY_MILLIS;
                    break;
                }
            }
        }
        for (Map.Entry<UUID, Item> entry : new ArrayList<>(revealerProjectiles.entrySet())) {
            Item item = entry.getValue();
            UUID id = entry.getKey();
            if (item == null || !item.isValid()) {
                removeRevealerProjectile(id);
                continue;
            }
            Location previous = revealerLastLocations.get(id);
            Location current = item.getLocation();
            Vector travel = previous == null ? item.getVelocity() : current.toVector().subtract(previous.toVector());
            if (previous != null && travel.lengthSquared() > 0.0001D) {
                RayTraceResult hit = world.rayTraceBlocks(previous, travel.clone().normalize(),
                        Math.max(0.2D, travel.length() + 0.2D), org.bukkit.FluidCollisionMode.NEVER, true);
                if (hit != null || item.isOnGround()) {
                    detonateRevealer(id, current);
                    continue;
                }
            } else if (item.isOnGround() || item.getTicksLived() > 120) {
                detonateRevealer(id, current);
                continue;
            }
            revealerLastLocations.put(id, current.clone());
        }
    }

    private boolean spawnPowerUp(long now) {
        Location[] points = {
                new Location(world, -17.5D, 64.0D, -17.5D),
                new Location(world, 17.5D, 64.0D, -17.5D),
                new Location(world, -17.5D, 64.0D, 17.5D),
                new Location(world, 17.5D, 64.0D, 17.5D)
        };
        List<Location> available = new ArrayList<>();
        for (Location point : points) {
            boolean used = powerUps.stream().anyMatch(p -> p.location.distanceSquared(point) < 1.0D);
            if (!used) available.add(point);
        }
        if (available.size() > 1 && lastPowerUpLocation != null) {
            available.removeIf(point -> point.distanceSquared(lastPowerUpLocation) < 1.0D);
        }
        if (available.isEmpty()) return false;
        Location location = available.get(ThreadLocalRandom.current().nextInt(available.size()));
        lastPowerUpLocation = location.clone();
        PowerUp powerUp = new PowerUp(location, now);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    Block block = world.getBlockAt(location.getBlockX() + dx, 62 + dy,
                            location.getBlockZ() + dz);
                    powerUp.originalBlocks.put(block, block.getBlockData().clone());
                    Material material = dy == 0 ? Material.IRON_BLOCK
                            : dx == 0 && dz == 0 ? Material.BEACON : Material.YELLOW_STAINED_GLASS;
                    block.setType(material, false);
                }
            }
        }
        powerUps.add(powerUp);
        announce(ChatColor.YELLOW + "Power-up spawning...");
        return true;
    }

    private void collectPowerUp(Player player) {
        UUID id = player.getUniqueId();
        int count = powerUpCounts.getOrDefault(id, 0);
        lastPowerUpCollectedAt.put(id, System.currentTimeMillis());
        incrementLocalStat(id, "powerups");
        addSmokeBomb(player);
        if (count >= 5) {
            var attribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (attribute != null) {
                attribute.setBaseValue(Math.min(40.0D, attribute.getBaseValue() + 2.0D));
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2.0D));
            }
            upgradeWeapon(player);
            upgradeArmor(player);
            player.sendMessage(ChatColor.GREEN + "Bonus power-up! +1 heart and a Smoke Bomb.");
            announce(ChatColor.YELLOW + player.getName() + " collected a bonus power-up!");
        } else if (count == 4) {
            masterAssassins.add(id);
            incrementLocalStat(id, "master-assassin");
            powerUpCounts.put(id, 5);
            player.getInventory().addItem(createMasterCompass());
            player.sendMessage(ChatColor.GOLD + "You are now a Master Assassin! Your compass tracks the nearest player.");
            announce(ChatColor.LIGHT_PURPLE + player.getName() + " has become a Master Assassin!");
            world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8F, 1.35F);
        } else {
            upgradeWeapon(player);
            upgradeArmor(player);
            powerUpCounts.put(id, count + 1);
            player.sendMessage(ChatColor.GREEN + "Power-up collected! (" + (count + 1) + "/5)");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.4F);
            announce(ChatColor.YELLOW + player.getName() + " collected a power-up!");
        }
        updateHud(System.currentTimeMillis());
    }

    private void addSmokeBomb(Player player) {
        player.getInventory().addItem(createSmokeBomb());
    }

    private void upgradeWeapon(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null) continue;
            Material next = switch (item.getType()) {
                case WOODEN_SWORD -> Material.STONE_SWORD;
                case STONE_SWORD -> Material.GOLDEN_SWORD;
                case GOLDEN_SWORD -> Material.IRON_SWORD;
                case IRON_SWORD -> Material.DIAMOND_SWORD;
                default -> null;
            };
            if (next != null) {
                player.getInventory().setItem(slot, new ItemStack(next));
                return;
            }
        }
    }

    private void upgradeArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        int lowest = 99;
        List<Integer> choices = new ArrayList<>();
        for (int i = 0; i < armor.length; i++) {
            int tier = armorTier(armor[i] == null ? Material.AIR : armor[i].getType());
            if (tier < 0 || tier >= 2) continue;
            if (tier < lowest) {
                lowest = tier;
                choices.clear();
            }
            if (tier == lowest) choices.add(i);
        }
        if (choices.isEmpty()) return;
        int slot = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        Material next = switch (armor[slot].getType()) {
            case LEATHER_BOOTS -> Material.CHAINMAIL_BOOTS;
            case LEATHER_LEGGINGS -> Material.CHAINMAIL_LEGGINGS;
            case LEATHER_CHESTPLATE -> Material.CHAINMAIL_CHESTPLATE;
            case LEATHER_HELMET -> Material.CHAINMAIL_HELMET;
            case CHAINMAIL_BOOTS -> Material.IRON_BOOTS;
            case CHAINMAIL_LEGGINGS -> Material.IRON_LEGGINGS;
            case CHAINMAIL_CHESTPLATE -> Material.IRON_CHESTPLATE;
            case CHAINMAIL_HELMET -> Material.IRON_HELMET;
            default -> null;
        };
        if (next != null) armor[slot] = new ItemStack(next);
        player.getInventory().setArmorContents(armor);
    }

    private int armorTier(Material material) {
        String name = material.name();
        if (name.startsWith("LEATHER_")) return 0;
        if (name.startsWith("CHAINMAIL_")) return 1;
        if (name.startsWith("IRON_")) return 2;
        return -1;
    }

    private void updateMasterCompasses(long now) {
        for (UUID id : new ArrayList<>(masterAssassins)) {
            if (!isAlive.test(id)) continue;
            long ready = masterCompassUpdateAt.getOrDefault(id, 0L);
            if (now < ready) continue;
            masterCompassUpdateAt.put(id, now + 1_000L);
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            Player target = nearestEnemy(player.getLocation(), player);
            if (target != null) player.setCompassTarget(target.getLocation());
        }
    }

    private void detonateRevealer(UUID itemId, Location impact) {
        UUID ownerId = revealerOwners.get(itemId);
        Player thrower = ownerId == null ? null : players.get(ownerId);
        if (impact.getWorld() != null) {
            world.spawnParticle(Particle.FIREWORK, impact, 38, 0.8D, 0.7D, 0.8D, 0.08D);
            world.spawnParticle(Particle.END_ROD, impact, 22, 0.6D, 0.6D, 0.6D, 0.05D);
            world.playSound(impact, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 0.7F);
        }
        for (UUID id : players.keySet()) {
            if (ownerId != null && ownerId.equals(id)) continue;
            Player target = players.get(id);
            if (target == null || !target.isOnline() || !isAlive.test(id)
                    || target.getWorld() != impact.getWorld()
                    || target.getLocation().distanceSquared(impact) > REVEALER_RADIUS * REVEALER_RADIUS) continue;
            long now = System.currentTimeMillis();
            if (revealUntil.getOrDefault(id, 0L) <= now && ownerId != null) {
                incrementLocalStat(ownerId, "i-see-you");
            }
            if (ownerId != null) {
                Set<UUID> distinctTargets = revealerTargets.computeIfAbsent(ownerId, ignored -> new HashSet<>());
                if (distinctTargets.add(id) && SneakyAssassinsRules.shouldAwardProsecutor(distinctTargets.size())
                    && thrower != null) {
                    awardAchievement(thrower, "prosecutor", "Prosecutor");
                }
            }
            revealPlayer(id, now + REVEALER_DURATION_MILLIS);
            target.sendMessage(ChatColor.AQUA + "You have been revealed!");
            if (thrower != null) thrower.sendMessage(ChatColor.AQUA + "Revealed " + target.getName() + "!");
        }
        removeRevealerProjectile(itemId);
    }

    private void removeRevealerProjectile(UUID id) {
        Item item = revealerProjectiles.remove(id);
        if (item != null && item.isValid()) item.remove();
        revealerOwners.remove(id);
        revealerLastLocations.remove(id);
    }

    void handleDamage(EntityDamageEvent event) {
        if (world == null || event.getEntity().getWorld() != world) return;
        Entity entity = event.getEntity();
        if (entity instanceof Skeleton skeleton && powerUps.stream().anyMatch(p -> p.item == skeleton)) {
            event.setCancelled(true);
            return;
        }
        Player target = entity instanceof Player player ? player : disguiseOwner(entity);
        if (target == null || !players.containsKey(target.getUniqueId())) return;
        event.setCancelled(true);
        if (!isAlive.test(target.getUniqueId()) || event.getCause() == EntityDamageEvent.DamageCause.WITHER) return;
        Player attacker = null;
        double damage = Math.max(0.0D, event.getFinalDamage());
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof org.bukkit.entity.Projectile projectile
                    && projectile.getShooter() instanceof Player) {
                pendingProjectileDamage.put(projectile.getUniqueId(), Math.max(0.0D, event.getFinalDamage()));
                return;
            }
            if (damager instanceof Player direct) {
                attacker = direct;
                if (!direct.equals(target) && direct.getInventory().getItemInMainHand() != null) {
                    double sword = SneakyAssassinsRules.swordDamage(direct.getInventory().getItemInMainHand().getType());
                    if (sword > 0.0D) {
                        event.setDamage(sword);
                        damage = Math.max(0.0D, event.getFinalDamage());
                    }
                }
            } else if (damager instanceof org.bukkit.entity.Projectile projectile) {
                ProjectileSource source = projectile.getShooter();
                if (source instanceof Player shooter) attacker = shooter;
            } else if (damager instanceof Villager npc && npcIds.contains(npc.getUniqueId())) {
                Bribe bribe = bribes.get(npc.getUniqueId());
                if (bribe != null) {
                    attacker = bribe.owner;
                    damage = 1.0D;
                }
            }
        }
        if (attacker != null && (!players.containsKey(attacker.getUniqueId())
                || !isAlive.test(attacker.getUniqueId()) || attacker.equals(target))) attacker = null;
        boolean directPlayerAttack = event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Player direct
                && attacker != null && direct.getUniqueId().equals(attacker.getUniqueId());
        if (directPlayerAttack && damage > 0.0D
                && "assassin".equals(selectedKit(attacker.getUniqueId()))) {
            rangedAssassinMeleeDamage.add(attacker.getUniqueId());
        }
        boolean lethalMasterCompassHit = directPlayerAttack && attacker != null
                && SneakyAssassinsRules.shouldAwardYouNeedDirections(
                        masterAssassins.contains(attacker.getUniqueId()), true,
                        isMasterCompass(attacker.getInventory().getItemInMainHand()),
                        target.getHealth() <= damage);
        if (lethalMasterCompassHit) pendingCompassKills.put(target.getUniqueId(), attacker.getUniqueId());
        if (attacker != null) {
            cloakUntil.remove(attacker.getUniqueId());
            Villager proxy = disguises.get(attacker.getUniqueId());
            if (proxy != null && proxy.isValid()) proxy.setInvisible(false);
            attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if (damage <= 0.0D) return;
        applyDamage(target, damage, attacker, "was defeated");
    }

    void handleNpcDeath(EntityDeathEvent event) {
        if (world == null || event.getEntity().getWorld() != world) return;
        UUID id = event.getEntity().getUniqueId();
        if (!npcIds.remove(id)) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        Player killer = event.getEntity().getKiller();
        if (killer != null && players.containsKey(killer.getUniqueId())) {
            incrementLocalStat(killer.getUniqueId(), "incompetence");
        }
        bribes.remove(id);
        npcNextAttackAt.remove(id);
    }

    void handleCombust(EntityCombustEvent event) {
        if (world == null || event.getEntity().getWorld() != world) return;
        event.setCancelled(true);
    }

    void projectileMissed(org.bukkit.entity.Projectile projectile) {
        if (projectile != null) pendingProjectileDamage.remove(projectile.getUniqueId());
    }

    void handleProjectileHit(org.bukkit.entity.Projectile projectile, Player shooter, Player target) {
        if (projectile == null || shooter == null || target == null
                || !players.containsKey(shooter.getUniqueId())
                || !players.containsKey(target.getUniqueId())
                || !isAlive.test(shooter.getUniqueId()) || !isAlive.test(target.getUniqueId())) return;
        Double recordedDamage = pendingProjectileDamage.remove(projectile.getUniqueId());
        double damage = recordedDamage == null || recordedDamage <= 0.0D
                ? Math.max(2.0D, projectile.getVelocity().length() * 2.0D)
                : recordedDamage;
        cloakUntil.remove(shooter.getUniqueId());
        Villager proxy = disguises.get(shooter.getUniqueId());
        if (proxy != null && proxy.isValid()) proxy.setInvisible(false);
        shooter.removePotionEffect(PotionEffectType.INVISIBILITY);
        applyDamage(target, damage, shooter, "was defeated by " + shooter.getName());
        projectile.remove();
    }

    private void applyDamage(Player target, double damage, Player attacker, String reason) {
        if (target == null || !target.isOnline() || !isAlive.test(target.getUniqueId()) || damage <= 0.0D) return;
        if (target.getHealth() <= damage) {
            eliminator.eliminate(target, attacker, reason);
        } else {
            target.setNoDamageTicks(0);
            target.setHealth(Math.max(0.5D, target.getHealth() - damage));
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        }
    }

    void onPlayerRespawn(Player player) {
        if (player == null || !players.containsKey(player.getUniqueId())) return;
        players.put(player.getUniqueId(), player);
        if (scoreboard != null && player.isOnline()) player.setScoreboard(scoreboard);
    }

    void onKill(Player killer, Player victim) {
        if (killer == null || !killer.isOnline() || !players.containsKey(killer.getUniqueId())) return;
        UUID killerId = killer.getUniqueId();
        UUID compassKiller = victim == null ? null : pendingCompassKills.remove(victim.getUniqueId());
        if (killerId.equals(compassKiller)) {
            awardAchievement(killer, "you-need-directions", "You Need Directions?");
        }
        if (victim != null) {
            Long collectedAt = lastPowerUpCollectedAt.get(victim.getUniqueId());
            long now = System.currentTimeMillis();
            if (collectedAt != null && SneakyAssassinsRules.isPowerUpKillWindow(now, collectedAt)) {
                awardAchievement(killer, "disappointment-is-immeasurable", "Disappointment is Immeasurable");
            }
        }
        if (victim != null && powerUpCounts.getOrDefault(killer.getUniqueId(), 0) == 0
                && powerUpCounts.getOrDefault(victim.getUniqueId(), 0) >= 4) {
            incrementLocalStat(killer.getUniqueId(), "the-masters-master");
            unlockTheMastersMaster(killer);
        }
        var attribute = killer.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(attribute.getBaseValue() + 2.0D);
            killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + 2.0D));
        }
        addSmokeBomb(killer);
        killer.sendMessage(ChatColor.GREEN + "Kill bonus: +1 heart and a Smoke Bomb.");
    }

    void finish(Player winner) {
        if (winner == null || !players.containsKey(winner.getUniqueId())) return;
        UUID winnerId = winner.getUniqueId();
        String kit = selectedKit(winnerId);
        if (SneakyAssassinsRules.shouldAwardNotSoSneaky(
                kit, rangedAssassinMeleeDamage.contains(winnerId))) {
            awardAchievement(winner, "not-so-sneaky", "Not So Sneaky");
        }
        if (SneakyAssassinsRules.shouldAwardIdiot(kit, escapeArtistSmokeUsed.contains(winnerId))) {
            awardAchievement(winner, "idiot", "Idiot");
        }
        long otherMasters = masterAssassins.stream().filter(id -> !id.equals(winnerId)).count();
        if (SneakyAssassinsRules.shouldAwardTheDuelsDuelsDuel(
                masterAssassins.contains(winnerId), (int) Math.min(Integer.MAX_VALUE, otherMasters))) {
            awardAchievement(winner, "the-duels-duels-duel", "The Duel’s Duel’s Duel");
        }
    }

    private void awardAchievement(Player player, String id, String displayName) {
        if (player == null || !player.isOnline() || isTestPlayer.test(player.getUniqueId()) || id == null) return;
        String path = "local-achievements." + player.getUniqueId() + ".sneaky-assassins." + id;
        if (plugin.getConfig().getBoolean(path, false)) return;
        plugin.getConfig().set(path, true);
        plugin.saveConfig();
        player.sendMessage(ChatColor.GOLD + "Achievement unlocked: " + ChatColor.YELLOW + displayName);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.15F);
    }

    private boolean isMasterCompass(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS && item.hasItemMeta()
                && "master-compass".equals(item.getItemMeta().getPersistentDataContainer()
                        .get(itemKey, PersistentDataType.STRING));
    }

    void onDrop(PlayerDropItemEvent event) {
        if (world == null || event.getPlayer().getWorld() != world
                || !players.containsKey(event.getPlayer().getUniqueId())) return;
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getItemMeta() == null) return;
        String kind = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if ("master-compass".equals(kind)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.YELLOW + "You cannot drop your Master Assassin compass.");
        }
    }

    private void consumeHeld(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) return;
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
    }

    private String localStatPath(UUID playerId, String stat) {
        return "local-stats." + playerId + ".sneaky-assassins." + stat;
    }

    private void incrementLocalStat(UUID playerId, String stat) {
        if (playerId == null || isTestPlayer.test(playerId)) return;
        String path = localStatPath(playerId, stat);
        plugin.getConfig().set(path, plugin.getConfig().getInt(path, 0) + 1);
        plugin.saveConfig();
    }

    private void unlockTheMastersMaster(Player player) {
        if (player == null || isTestPlayer.test(player.getUniqueId())) return;
        String path = "test-achievements.sneaky-assassins." + player.getUniqueId() + ".the-masters-master";
        if (plugin.getConfig().getBoolean(path, false)) return;
        plugin.getConfig().set(path, true);
        plugin.saveConfig();
        player.sendMessage(ChatColor.GOLD + "Achievement unlocked: The Master's Master — defeat an assassin with 4+ power-ups while you have none.");
    }

    private void announce(String message) {
        for (Player player : players.values()) if (player != null && player.isOnline()) player.sendMessage(message);
    }

    void onQuit(Player player) {
        if (player != null) {
            UUID id = player.getUniqueId();
            removeDisguise(id);
            players.remove(id);
            powerUpCounts.remove(id);
            revealUntil.remove(id);
            cloakUntil.remove(id);
            masterAssassins.remove(id);
            masterCompassUpdateAt.remove(id);
            previousScoreboards.remove(id);
            if (scoreboard != null) {
                Team team = playerScoreboardTeams.remove(id);
                if (team != null) {
                    team.removeEntry(player.getName());
                    team.unregister();
                }
                scoreboard.resetScores(player.getName());
            }
        }
    }

    void stop() {
        for (UUID id : new ArrayList<>(disguises.keySet())) removeDisguise(id);
        for (UUID id : new ArrayList<>(players.keySet())) {
            Player player = players.get(id);
            if (player != null) {
                player.removePotionEffect(PotionEffectType.WITHER);
                Scoreboard previous = previousScoreboards.get(id);
                if (player.isOnline() && previous != null) player.setScoreboard(previous);
            }
        }
        for (PowerUp powerUp : new ArrayList<>(powerUps)) removePowerUp(powerUp);
        for (UUID id : new ArrayList<>(npcIds)) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) entity.remove();
        }
        for (UUID id : new ArrayList<>(revealerProjectiles.keySet())) removeRevealerProjectile(id);
        pendingProjectileDamage.clear();
        pendingCompassKills.clear();
        rangedAssassinMeleeDamage.clear();
        escapeArtistSmokeUsed.clear();
        revealerTargets.clear();
        lastPowerUpCollectedAt.clear();
        npcIds.clear();
        bribes.clear();
        npcNextAttackAt.clear();
        npcPatrolAnchors.clear();
        npcPatrolTargets.clear();
        npcLastMovementLocations.clear();
        npcLastMovementAt.clear();
        players.clear();
        hiddenViewers.clear();
        revealUntil.clear();
        cloakUntil.clear();
        bribeReadyAt.clear();
        powerUpCounts.clear();
        masterAssassins.clear();
        masterCompassUpdateAt.clear();
        scoreboard = null;
        scoreboardObjective = null;
        playerScoreboardTeams.clear();
        previousScoreboards.clear();
        lastPowerUpLocation = null;
        world = null;
        isAlive = ignored -> false;
        eliminator = (victim, killer, reason) -> { };
        nextPowerUpAt = nextGlobalRevealAt = globalRevealEndsAt = 0L;
    }

    private void removePowerUp(PowerUp powerUp) {
        if (powerUp.item != null && powerUp.item.isValid()) powerUp.item.remove();
        for (Map.Entry<Block, BlockData> block : powerUp.originalBlocks.entrySet()) {
            if (block.getKey().getWorld() == world) block.getKey().setBlockData(block.getValue(), false);
        }
        powerUps.remove(powerUp);
    }

    private static final class Bribe {
        final Player owner;
        Bribe(Player owner) { this.owner = owner; }
    }

    private static final class PowerUp {
        final Location location;
        final long spawnedAt;
        final Map<Block, BlockData> originalBlocks = new LinkedHashMap<>();
        Skeleton item;
        PowerUp(Location location, long spawnedAt) {
            this.location = location.clone();
            this.spawnedAt = spawnedAt;
        }
    }
}
