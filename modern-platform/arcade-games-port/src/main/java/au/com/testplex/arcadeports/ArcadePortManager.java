package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;
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
    private final Map<UUID, PlayerSnapshot> pendingRestore = new HashMap<>();
    private Session active;
    private BukkitTask pendingStart;
    private ArcadePortGame pendingType;
    private long lastDragonHazard;
    private long lastSheepSpawn;

    ArcadePortManager(ArcadeMechanicsModernPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
        World world = worlds.computeIfAbsent(type.id(), ignored -> createWorld(type));
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "The test world could not be loaded. Check the server log.");
            return;
        }
        buildArena(type, world);
        Session session = new Session(type, world, participants);
        active = session;
        long now = System.currentTimeMillis();
        session.startedAt = now;
        session.endsAt = now + (type == ArcadePortGame.SHEEP_QUEST ? 300_000L : ROUND_MS);
        session.lastBaconHungerAt = now;
        session.lastBaconEscalationAt = now;
        session.baconEscalationIntervalMillis = ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS;
        session.nextDragonEscapeHazardAt = now + ArcadePortRules.DRAGON_ESCAPE_HAZARD_INTERVAL_MILLIS;
        session.microBarrierActive = type == ArcadePortGame.MICRO_BATTLES;
        session.turfCombat = false;
        session.turfPhaseEndsAt = type == ArcadePortGame.TURF_WARS
                ? now + ArcadePortRules.TURF_WARS_BUILD_PHASE_MILLIS : 0L;
        session.turfFrontlineX = -1;
        lastDragonHazard = now + 8_000L;
        lastSheepSpawn = now + 20_000L;
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0D, 0.0D);
        border.setDamageAmount(0.0D);
        border.setSize(type == ArcadePortGame.MICRO_BATTLES ? 42.0D : 80.0D);
        List<Location> spawns = spawnLocations(type, participants.size());
        for (int i = 0; i < participants.size(); i++) {
            Player player = participants.get(i);
            PlayerSnapshot snapshot = pendingRestore.remove(player.getUniqueId());
            if (snapshot == null) snapshot = new PlayerSnapshot(player);
            session.snapshots.put(player.getUniqueId(), snapshot);
            session.players.put(player.getUniqueId(), player);
            session.alive.add(player.getUniqueId());
            session.scores.put(player.getUniqueId(), 0);
            if (type == ArcadePortGame.EVOLUTION) session.evolutionLevels.put(player.getUniqueId(), 0);
            int teamId = type == ArcadePortGame.MICRO_BATTLES ? i % 4 : i % 2;
            session.teams.put(player.getUniqueId(), teamId);
            Location spawn = spawns.get(i % spawns.size()).clone();
            if (type == ArcadePortGame.MICRO_BATTLES) {
                spawn = microTeamSpawn(teamId, i / 4, (participants.size() + 3) / 4);
            } else if (type == ArcadePortGame.TURF_WARS) {
                spawn = turfWarsSpawn(world, session.teams.get(player.getUniqueId()), i / 2,
                        (participants.size() + 1) / 2);
            }
            player.closeInventory();
            player.leaveVehicle();
            player.teleport(spawn);
            player.setGameMode(GameMode.SURVIVAL);
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
                Zombie chaser = session.world.spawn(new Location(session.world, -9.0D, 64.0D, 0.0D), Zombie.class);
                chaser.setCustomName(ChatColor.RED + "Death Tag Test Chaser");
                chaser.setCustomNameVisible(true);
                chaser.setRemoveWhenFarAway(false);
                chaser.setTarget(runner);
                session.entities.add(chaser);
                runner.sendMessage(ChatColor.YELLOW + "Solo test: avoid the chaser; one hit ends the run.");
            }
        }
        if (type == ArcadePortGame.SHEEP_QUEST) spawnSheep(session);
        if (type == ArcadePortGame.MICRO_BATTLES) announce(session, ChatColor.AQUA + "Micro Battles: gather blocks; glass barriers drop after 10 seconds.");
        else announce(session, ChatColor.GOLD + type.displayName() + " started! This is a first-pass mechanics test.");
        if (type == ArcadePortGame.TURF_WARS) {
            announce(session, ChatColor.GREEN + "Build phase: place your team's wool on your own turf for 20 seconds.");
        }
        if (participants.size() == 1) announce(session, ChatColor.GRAY + "Solo test mode: the run will remain available for practice until the timer ends or you reset it.");
        session.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(session), 1L, 1L);
        sender.sendMessage(ChatColor.GREEN + "Started " + type.displayName() + " with " + participants.size() + (participants.size() == 1 ? " player." : " players."));
    }

    private World createWorld(ArcadePortGame type) {
        String worldName = "arcadeport_" + type.id().replace('-', '_');
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT).generateStructures(false);
            world = creator.createWorld();
        }
        if (world == null) {
            plugin.getLogger().severe("Could not create test world " + worldName);
            return null;
        }
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setSpawnLocation(0, 64, 0);
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

    private Location islandSpawn(int index) {
        World world = worlds.get(ArcadePortGame.MICRO_BATTLES.id());
        double angle = 2.0D * Math.PI * Math.floorMod(index, 8) / 8.0D;
        return new Location(world, Math.cos(angle) * 13.0D, 64.0D, Math.sin(angle) * 13.0D, (float)(angle * 180.0D / Math.PI + 90.0F), 0.0F);
    }

    private Location microTeamSpawn(int team, int slot, int slots) {
        Location center = islandSpawn(team * 2);
        if (slots > 1) {
            double angle = 2.0D * Math.PI * slot / slots;
            center.add(Math.cos(angle) * 1.4D, 0.0D, Math.sin(angle) * 1.4D);
        }
        return center;
    }

    private void updateDeathTagChasers(Session session, long now) {
        int requiredChasers = Math.min(session.alive.size(),
                ArcadePortRules.requiredDeathTagChasers(now - session.startedAt));
        long taggedAlive = session.tagged.stream().filter(session.alive::contains).count();
        if (taggedAlive >= requiredChasers) return;

        List<Player> runners = alivePlayers(session).stream()
                .filter(player -> !session.tagged.contains(player.getUniqueId()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        while (taggedAlive < requiredChasers && !runners.isEmpty()) {
            Player promoted = runners.remove(ThreadLocalRandom.current().nextInt(runners.size()));
            session.tagged.add(promoted.getUniqueId());
            announce(session, ChatColor.RED + promoted.getName() + " has been forced to become a Chaser.");
            promoted.sendMessage(ChatColor.YELLOW + "Time is running out! Tag every remaining runner.");
            taggedAlive++;
        }
    }

    private void updateMicroBarrier(Session session, long now) {
        if (!session.microBarrierActive || !ArcadePortRules.shouldRemoveMicroBarrier(now - session.startedAt)) return;
        for (int team = 0; team < 4; team++) {
            Location island = islandSpawn(team * 2);
            int cx = island.getBlockX();
            int cz = island.getBlockZ();
            for (int offset = -4; offset <= 4; offset++) {
                for (int y = 64; y <= 65; y++) {
                    Block[] ring = {
                            session.world.getBlockAt(cx + offset, y, cz - 4),
                            session.world.getBlockAt(cx + offset, y, cz + 4),
                            session.world.getBlockAt(cx - 4, y, cz + offset),
                            session.world.getBlockAt(cx + 4, y, cz + offset)
                    };
                    for (Block block : ring) if (block.getType() == Material.GLASS) block.setType(Material.AIR, false);
                }
            }
        }
        session.microBarrierActive = false;
        announce(session, ChatColor.AQUA + "Glass barriers are down! Fight for your team.");
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
            case BACON_BRAWL:
                player.getInventory().addItem(namedItem(Material.STICK, "Bacon Brawl", "Knock opponents from the platform."));
                player.getInventory().addItem(new ItemStack(Material.COOKED_PORKCHOP, 8));
                break;
            case BOMB_LOBBERS:
                player.getInventory().addItem(namedItem(Material.TNT, "Bomb Lobber", "Throw TNT to blast enemy players away.", 5));
                break;
            case DEATH_TAG:
                player.getInventory().addItem(namedItem(Material.STONE_SWORD, "Tag", "Hit runners to tag them."));
                break;
            case DRAGON_ESCAPE:
                player.getInventory().addItem(namedItem(Material.FEATHER, "Dragon Dash", "Right-click to dash forward.", 3));
                break;
            case DRAGONS:
                player.getInventory().addItem(namedItem(Material.BLAZE_POWDER, "Pyro", "Right-click to launch a fireball.", 5));
                player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
                break;
            case EVOLUTION:
                player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                break;
            case GLADIATORS:
                player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
                player.getInventory().addItem(new ItemStack(Material.SHIELD));
                break;
            case MICRO_BATTLES:
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 16));
                player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 32));
                break;
            case ONE_IN_THE_QUIVER:
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                break;
            case PAINTBALL:
                player.getInventory().addItem(namedItem(Material.SNOWBALL, "Paintball", "Tag opponents with a direct hit.", 32));
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
                break;
            case RUNNER:
                player.getInventory().addItem(namedItem(Material.FEATHER, "Runner Dash", "A short forward burst.", 3));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 12));
                break;
            case SHEEP_QUEST:
                player.getInventory().addItem(namedItem(Material.SADDLE, "Shepherd", "Hold to pick up sheep; carry them to your team's pen."));
                break;
            case SPLEEF:
                player.getInventory().addItem(new ItemStack(Material.IRON_SHOVEL));
                player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 16));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 12));
                break;
            case TURF_WARS:
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
                giveTurfWarsWool(session, player);
                break;
        }
        player.updateInventory();
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
        if (now >= session.endsAt) {
            Player winner = session.type == ArcadePortGame.SHEEP_QUEST
                    ? sheepQuestLeader(session) : topScorerOrLastAlive(session);
            finish(session, winner, true);
            return;
        }
        if (session.type == ArcadePortGame.TURF_WARS) updateTurfWarsPhase(session, now);
        if (session.type == ArcadePortGame.MICRO_BATTLES) updateMicroBarrier(session, now);
        if (session.type == ArcadePortGame.DEATH_TAG && session.participantsAtStart > 1) {
            updateDeathTagChasers(session, now);
        }
        if (session.type == ArcadePortGame.BACON_BRAWL) updateBaconBrawl(session, now);
        if (session.type == ArcadePortGame.RUNNER) processRunnerBlocks(session, now);
        if (session.type == ArcadePortGame.DRAGONS && now >= lastDragonHazard) {
            launchDragonHazard(session);
            lastDragonHazard = now + 4_500L;
        }
        if (session.type == ArcadePortGame.DRAGON_ESCAPE
                && ArcadePortRules.shouldLaunchDragonEscapeHazard(now, session.nextDragonEscapeHazardAt)) {
            launchDragonEscapeHazard(session);
            session.nextDragonEscapeHazardAt = now + ArcadePortRules.DRAGON_ESCAPE_HAZARD_INTERVAL_MILLIS;
        }
        if (session.type == ArcadePortGame.SHEEP_QUEST) {
            updateSheepQuest(session);
            if (now >= lastSheepSpawn && session.sheep.size() < 12) {
                spawnSheep(session);
                lastSheepSpawn = now + 20_000L;
            }
        }

        if (session.type == ArcadePortGame.PAINTBALL && session.ticks % 40 == 0) {
            for (UUID id : session.alive) {
                Player player = session.players.get(id);
                if (player != null && player.isOnline() && player.getInventory().contains(Material.SNOWBALL, 8) == false) {
                    player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 8));
                }
            }
        }
        if (session.type == ArcadePortGame.TURF_WARS && session.turfCombat && session.ticks % 40 == 0) {
            for (UUID id : session.alive) {
                Player player = session.players.get(id);
                if (player != null && player.isOnline() && !player.getInventory().contains(Material.ARROW, 4)) {
                    player.getInventory().addItem(new ItemStack(Material.ARROW, 4));
                }
            }
        }
        if (session.type == ArcadePortGame.DRAGON_ESCAPE && session.ticks % 4 == 0) {
            // The old course crumbles behind the pack to keep forward motion part of the test.
            int farthest = session.players.values().stream().filter(Player::isOnline).mapToInt(p -> p.getLocation().getBlockZ()).max().orElse(-22);
            int crumbleZ = farthest - 7;
            if (crumbleZ > -24 && crumbleZ < 78) for (int x = -3; x <= 3; x++) session.world.getBlockAt(x, 63, crumbleZ).setType(Material.AIR, false);
        }
        updateTitles(session);
        checkRoundEnd(session);
    }

    private void updateBaconBrawl(Session session, long now) {
        if (now - session.lastBaconHungerAt >= 2_000L) {
            session.lastBaconHungerAt = now;
            for (Player player : new ArrayList<>(alivePlayers(session))) {
                if (player.getFoodLevel() > 0) {
                    player.setFoodLevel(Math.max(0, player.getFoodLevel() - 1));
                } else if (player.getHealth() <= 2.0D) {
                    eliminate(session, player, null, "starved");
                    if (active != session) return;
                } else {
                    player.setHealth(player.getHealth() - 2.0D);
                }
            }
        }
        if (active != session || now - session.lastBaconEscalationAt < session.baconEscalationIntervalMillis) return;
        session.lastBaconEscalationAt = now;
        session.baconKnockback = ArcadePortRules.nextBaconKnockback(session.baconKnockback);
        session.baconEscalationIntervalMillis = ArcadePortRules.nextBaconKnockbackInterval(
                session.baconEscalationIntervalMillis);
        announce(session, ChatColor.GREEN + "The ground trembles! Knockback has increased.");
        for (Player player : alivePlayers(session)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 0.7F);
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

    private void launchDragonEscapeHazard(Session session) {
        List<Player> alive = alivePlayers(session);
        if (alive.isEmpty()) return;
        Player target = alive.stream()
                .max(java.util.Comparator.comparingDouble(player -> player.getLocation().getZ()))
                .orElse(null);
        if (target == null) return;

        Location targetLocation = target.getLocation().clone();
        double spawnX = Math.max(-2.5D, Math.min(2.5D,
                targetLocation.getX() + ThreadLocalRandom.current().nextDouble(-1.5D, 1.5D)));
        Location spawn = new Location(session.world, spawnX, targetLocation.getY() + 2.0D,
                Math.max(-23.0D, targetLocation.getZ() - 9.0D));
        Fireball fireball = session.world.spawn(spawn, Fireball.class);
        fireball.setIsIncendiary(false);
        fireball.setYield(0.0F);
        Vector direction = targetLocation.toVector().subtract(spawn.toVector());
        if (direction.lengthSquared() < 0.001D) direction = new Vector(0.0D, 0.0D, 1.0D);
        fireball.setVelocity(direction.normalize().multiply(0.65D));
        session.entities.add(fireball);
    }

    private void handleDragonEscapeHazardHit(Session session, Fireball fireball, Player target) {
        if (active != session || !session.alive.contains(target.getUniqueId())) return;
        Vector recoil = fireball.getLocation().toVector().subtract(target.getLocation().toVector());
        if (recoil.lengthSquared() < 0.001D) recoil = new Vector(0.0D, 0.0D, -1.0D);
        target.setVelocity(recoil.normalize().multiply(0.9D).setY(0.42D));
        target.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.9F, 0.8F);
        target.sendMessage(ChatColor.RED + "The dragon's fireball struck you!");
        if (target.getHealth() <= 4.0D) {
            eliminate(session, target, null, "was caught by the dragon's fireball");
        } else {
            target.setHealth(target.getHealth() - 4.0D);
        }
    }

    private void launchDragonHazard(Session session) {
        List<Player> alive = alivePlayers(session);
        if (alive.isEmpty()) return;
        Player target = alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
        double edgeX = ThreadLocalRandom.current().nextBoolean() ? -18.0D : 18.0D;
        Location spawn = new Location(session.world, edgeX, 68.0D, target.getLocation().getZ());
        Fireball fireball = session.world.spawn(spawn, Fireball.class);
        // Leave the shooter unset: this is a world hazard, not a player-fired attack.
        fireball.setIsIncendiary(false);
        fireball.setYield(0.0F);
        Vector direction = target.getLocation().toVector().subtract(spawn.toVector()).normalize().multiply(0.65D);
        fireball.setVelocity(direction);
        session.entities.add(fireball);
    }

    private Location sheepQuestPenCenter(Session session, int team) {
        return new Location(session.world, team == 0 ? -15.0D : 15.0D, 64.0D, 0.0D);
    }

    private boolean isInsideSheepPen(Location location, int team) {
        if (location == null || team < 0 || team > 1) return false;
        Location center = sheepQuestPenCenter(active, team);
        return location.getWorld() == center.getWorld()
                && Math.abs(location.getX() - center.getX()) <= 2.5D
                && Math.abs(location.getZ() - center.getZ()) <= 3.5D;
    }

    private void buildSheepPens(World world) {
        for (int team = 0; team < 2; team++) {
            int centerX = team == 0 ? -15 : 15;
            Material wool = team == 0 ? Material.RED_WOOL : Material.BLUE_WOOL;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    world.getBlockAt(centerX + dx, 63, dz).setType(wool, false);
                    boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 3;
                    boolean gate = dz == 0 && (team == 0 ? dx == 2 : dx == -2);
                    if (wall && !gate) {
                        for (int y = 64; y <= 65; y++) {
                            world.getBlockAt(centerX + dx, y, dz).setType(wool, false);
                        }
                    }
                }
            }
        }
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
                    sheep.teleport(pen.clone().add(0.5D, 0.0D, 0.5D));
                    sheep.setAI(false);
                }
                continue;
            }

            Vector facing = carrier.getLocation().getDirection().setY(0.0D);
            if (facing.lengthSquared() < 0.001D) facing = new Vector(0.0D, 0.0D, 1.0D);
            Location following = carrier.getLocation().clone().subtract(facing.normalize().multiply(1.35D));
            following.setY(64.0D);
            following.setYaw(carrier.getLocation().getYaw());
            following.setPitch(0.0F);
            sheep.teleport(following);

            int team = session.teams.getOrDefault(carrierId, -1);
            if (team < 0 || team > 1) continue;
            if (horizontalDistanceSquared(carrier.getLocation(), sheepQuestPenCenter(session, team)) > 20.25D) continue;

            int previousOwner = session.sheepOwners.getOrDefault(sheep, -1);
            if (previousOwner == team && isInsideSheepPen(sheep.getLocation(), team)) continue;
            session.sheepOwners.put(sheep, team);
            sheep.setColor(team == 0 ? DyeColor.RED : DyeColor.BLUE);
            sheep.setAI(false);
            Location pen = sheepQuestPenCenter(session, team);
            sheep.teleport(pen.clone().add(ThreadLocalRandom.current().nextDouble(-1.0D, 1.0D),
                    0.0D, ThreadLocalRandom.current().nextDouble(-2.0D, 2.0D)));
            iterator.remove();
            session.scores.merge(carrierId, 1, Integer::sum);
            carrier.playSound(carrier.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 1.0F, 1.5F);
            carrier.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, carrier.getLocation().add(0, 1, 0),
                    12, 0.35D, 0.4D, 0.35D, 0.04D);
            announce(session, (previousOwner < 0 ? ChatColor.GREEN + "Sheep captured by " :
                    ChatColor.YELLOW + "Sheep stolen by ") + carrier.getName() + " for "
                    + (team == 0 ? ChatColor.RED + "Red" : ChatColor.BLUE + "Blue") + ChatColor.WHITE + "!");
        }

        for (Sheep sheep : session.sheep) {
            if (!sheep.isValid() || session.sheepCarriers.containsKey(sheep)) continue;
            int owner = session.sheepOwners.getOrDefault(sheep, -1);
            if (owner >= 0 && !isInsideSheepPen(sheep.getLocation(), owner)) {
                Location pen = sheepQuestPenCenter(session, owner);
                sheep.teleport(pen.clone().add(0.5D, 0.0D, 0.5D));
                sheep.setAI(false);
            }
        }
    }

    private int sheepQuestTeamCount(Session session, int team) {
        int count = 0;
        for (Sheep sheep : session.sheep) {
            if (session.sheepOwners.getOrDefault(sheep, -1) == team && isInsideSheepPen(sheep.getLocation(), team)) count++;
        }
        return count;
    }

    private Player sheepQuestLeader(Session session) {
        int red = sheepQuestTeamCount(session, 0);
        int blue = sheepQuestTeamCount(session, 1);
        if (red == blue) return topScorerOrLastAlive(session);
        int winningTeam = red > blue ? 0 : 1;
        return alivePlayers(session).stream()
                .filter(player -> session.teams.getOrDefault(player.getUniqueId(), -1) == winningTeam)
                .max(java.util.Comparator.comparingInt(player -> session.scores.getOrDefault(player.getUniqueId(), 0)))
                .orElse(null);
    }

    private void spawnSheep(Session session) {
        for (int attempt = 0; attempt < 18; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(-10, 11);
            int z = ThreadLocalRandom.current().nextInt(-HALF_SIZE + 4, HALF_SIZE - 3);
            Location loc = new Location(session.world, x + 0.5D, 64.0D, z + 0.5D);
            if (session.world.getBlockAt(x, 63, z).getType().isAir()) continue;
            Sheep sheep = session.world.spawn(loc, Sheep.class);
            sheep.setColor(DyeColor.WHITE);
            sheep.setCustomName(ChatColor.WHITE + "Sheep");
            sheep.setCustomNameVisible(true);
            sheep.setAI(true);
            sheep.setInvulnerable(true);
            sheep.setRemoveWhenFarAway(false);
            session.sheep.add(sheep);
            session.sheepOwners.put(sheep, -1);
            session.entities.add(sheep);
            return;
        }
    }

    private void updateTitles(Session session) {
        int alive = session.alive.size();
        long seconds = Math.max(0L, (session.endsAt - System.currentTimeMillis()) / 1000L);
        for (UUID id : session.players.keySet()) {
            Player p = session.players.get(id);
            if (!p.isOnline()) continue;
            int score = session.scores.getOrDefault(id, 0);
            p.sendActionBar(ChatColor.GOLD + session.type.displayName() + ChatColor.GRAY + " | " + seconds + "s"
                    + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + "alive " + alive
                    + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + "score " + score);
        }
    }

    private void checkRoundEnd(Session session) {
        if (active != session) return;
        // An empty live set is a real end condition even during a one-player test.
        if (session.alive.isEmpty()) {
            if (!ArcadePortRules.shouldFinishWhenNobodyIsAlive(session.alive.size(), session.respawning.size())) return;
            finish(session, null, true);
            return;
        }
        if (session.type == ArcadePortGame.MICRO_BATTLES && session.participantsAtStart > 1) {
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
            long untagged = session.players.keySet().stream().filter(id -> session.alive.contains(id) && !session.tagged.contains(id)).count();
            // A round starts with one tagged chaser, so "one runner left" is not a win
            // condition. Finish only once the last runner is tagged.
            if (untagged == 0) {
                Player winner = session.players.keySet().stream()
                        .filter(id -> session.alive.contains(id) && session.tagged.contains(id))
                        .map(session.players::get).filter(p -> p != null && p.isOnline())
                        .max(java.util.Comparator.comparingInt(p -> session.scores.getOrDefault(p.getUniqueId(), 0)))
                        .orElse(null);
                finish(session, winner, true);
            }
            return;
        }
        if (session.alive.size() <= 1) finish(session, alivePlayers(session).stream().findFirst().orElse(null), true);
    }

    private void eliminate(Session session, Player player, Player killer, String reason) {
        if (active != session || player == null || !session.alive.remove(player.getUniqueId())) return;
        player.leaveVehicle();
        if (session.type == ArcadePortGame.EVOLUTION && player.isOnline()) {
            if (killer != null && session.alive.contains(killer.getUniqueId())) {
                session.scores.merge(killer.getUniqueId(), 1, Integer::sum);
                int killerLevel = ArcadePortRules.nextEvolutionLevel(
                        session.evolutionLevels.getOrDefault(killer.getUniqueId(), 0), true);
                session.evolutionLevels.put(killer.getUniqueId(), killerLevel);
                giveEvolutionKit(session, killer);
                if (killerLevel >= 5) {
                    finish(session, killer, true);
                    return;
                }
            }
            int victimLevel = ArcadePortRules.nextEvolutionLevel(
                    session.evolutionLevels.getOrDefault(player.getUniqueId(), 0), false);
            session.evolutionLevels.put(player.getUniqueId(), victimLevel);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "You were knocked back to evolution stage " + (victimLevel + 1) + ": " + reason + ".");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (active != session || !player.isOnline()) return;
                Location spawn = spawnFor(session, player);
                player.teleport(spawn);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(20.0F);
                session.alive.add(player.getUniqueId());
                giveEvolutionKit(session, player);
            }, 30L);
            return;
        }
        if (player.isOnline()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "Eliminated: " + reason + ".");
        }
        if (killer != null && session.scores.containsKey(killer.getUniqueId())) session.scores.merge(killer.getUniqueId(), 1, Integer::sum);
        checkRoundEnd(session);
    }

    private void giveEvolutionKit(Session session, Player player) {
        player.getInventory().clear();
        int level = Math.max(0, Math.min(5, session.evolutionLevels.getOrDefault(player.getUniqueId(), 0)));
        Material weapon = level >= 4 ? Material.DIAMOND_SWORD : level >= 2 ? Material.IRON_SWORD
                : level == 1 ? Material.STONE_SWORD : Material.WOODEN_SWORD;
        player.getInventory().addItem(new ItemStack(weapon));
        player.sendMessage(ChatColor.GREEN + "Evolution stage restored: " + (level + 1) + ".");
    }

    private Location spawnFor(Session session, Player player) {
        List<Player> players = new ArrayList<>(session.players.values());
        int index = Math.max(0, players.indexOf(player));
        List<Location> spawns = spawnLocations(session.type, session.players.size());
        return spawns.get(index % spawns.size()).clone();
    }

    private void checkWinnerForScore(Session session, Player player) {
        if (session.type == ArcadePortGame.PAINTBALL && session.scores.getOrDefault(player.getUniqueId(), 0) >= 10) finish(session, player, true);
        if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER
                && ArcadePortRules.isQuiverWinnerScoreReached(session.scores.getOrDefault(player.getUniqueId(), 0))) {
            finish(session, player, true);
        }
    }

    private void handlePlayerHit(Session session, Player attacker, Player target, String source) {
        if (active != session || attacker.equals(target) || !session.alive.contains(attacker.getUniqueId())
                || !session.alive.contains(target.getUniqueId())) return;
        if ((session.type == ArcadePortGame.PAINTBALL || session.type == ArcadePortGame.TURF_WARS
                || session.type == ArcadePortGame.MICRO_BATTLES)
                && session.teams.getOrDefault(attacker.getUniqueId(), -1).equals(session.teams.getOrDefault(target.getUniqueId(), -2))) return;
        if (session.type == ArcadePortGame.TURF_WARS && !session.turfCombat) return;
        if (session.type == ArcadePortGame.SPLEEF) return; // Player combat is disabled; projectiles target the floor.

        switch (session.type) {
            case BACON_BRAWL:
                Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
                if (direction.lengthSquared() < 1.0E-5D) direction = attacker.getLocation().getDirection();
                target.setVelocity(direction.normalize().multiply(session.baconKnockback).setY(0.7D));
                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 10, 0.35D, 0.4D, 0.35D, 0.04D);
                attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + 2));
                session.scores.merge(attacker.getUniqueId(), 1, Integer::sum);
                break;
            case BOMB_LOBBERS:
                // Bomb Lobbers uses thrown TNT; melee attacks do not deal hits.
                break;
            case DEATH_TAG:
                if (session.tagged.contains(attacker.getUniqueId())) {
                    session.tagged.add(target.getUniqueId());
                    target.sendMessage(ChatColor.RED + "Tagged! You are now IT.");
                    attacker.sendMessage(ChatColor.GREEN + "Tag confirmed.");
                } else if (session.tagged.contains(target.getUniqueId())) {
                    session.tagged.remove(attacker.getUniqueId());
                    session.tagged.add(attacker.getUniqueId());
                }
                checkRoundEnd(session);
                break;
            case PAINTBALL:
                session.scores.merge(attacker.getUniqueId(), 1, Integer::sum);
                Vector push = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize().multiply(0.8D);
                target.setVelocity(push.setY(0.25D));
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 14, 0.4D, 0.5D, 0.4D, 0.12D);
                checkWinnerForScore(session, attacker);
                break;
            case TURF_WARS: {
                UUID targetId = target.getUniqueId();
                eliminate(session, target, attacker, "was hit in Turf Wars");
                if (active != session) break;
                advanceTurfWars(session, attacker);
                if (active != session) break;
                session.respawning.add(targetId);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (active != session || !target.isOnline()
                            || !session.players.containsKey(targetId) || !session.respawning.remove(targetId)) return;
                    int playerIndex = new ArrayList<>(session.players.keySet()).indexOf(targetId);
                    int slots = Math.max(1, (session.players.size() + 1) / 2);
                    Location spawn = turfWarsSpawn(session.world, session.teams.getOrDefault(targetId, 0),
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
                break;
            }
            case EVOLUTION:
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
                eliminate(session, target, attacker, "lost a duel");
                break;
            case SHEEP_QUEST:
                // Sheep Quest scoring is handled by capturing sheep, not combat.
                break;
            case ONE_IN_THE_QUIVER:
                scoreQuiverKill(session, attacker, target);
                break;
            default:
                eliminate(session, target, attacker, source);
                break;
        }
    }

    private void advanceTurfWars(Session session, Player attacker) {
        int team = session.teams.getOrDefault(attacker.getUniqueId(), -1);
        if (team != 0 && team != 1) return;
        int oldFrontline = session.turfFrontlineX;
        int nextFrontline = ArcadePortRules.advanceTurfFrontline(oldFrontline, team);
        int paintedX = team == 0 ? nextFrontline : oldFrontline;
        session.turfFrontlineX = nextFrontline;
        Material turf = team == 0 ? Material.RED_TERRACOTTA : Material.BLUE_TERRACOTTA;
        for (int z = -HALF_SIZE; z <= HALF_SIZE; z++) {
            session.world.getBlockAt(paintedX, 63, z).setType(turf, false);
            for (int y = 64; y <= 68; y++) {
                Block structure = session.world.getBlockAt(paintedX, y, z);
                if (structure.getType() == Material.RED_WOOL || structure.getType() == Material.BLUE_WOOL) {
                    structure.setType(Material.AIR, false);
                }
            }
        }
        if (ArcadePortRules.isTurfWarsWinningLine(nextFrontline, team, HALF_SIZE)) {
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

        UUID targetId = target.getUniqueId();
        target.leaveVehicle();
        target.setGameMode(GameMode.SPECTATOR);
        session.alive.remove(targetId);
        session.respawning.add(targetId);
        target.sendMessage(ChatColor.RED + "Eliminated! Respawning shortly.");

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
        }, 40L);
    }

    private void hitProjectile(Session session, Projectile projectile, Player shooter, Player target) {
        if (session.type == ArcadePortGame.BOMB_LOBBERS) {
            handlePlayerHit(session, shooter, target, "bomb hit");
        } else if (session.type == ArcadePortGame.PAINTBALL || session.type == ArcadePortGame.TURF_WARS) {
            handlePlayerHit(session, shooter, target, "projectile hit");
        } else if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER || session.type == ArcadePortGame.MICRO_BATTLES) {
            handlePlayerHit(session, shooter, target, "arrow hit");
            if (session.type == ArcadePortGame.ONE_IN_THE_QUIVER && active == session
                    && shooter.isOnline() && session.alive.contains(shooter.getUniqueId())) {
                shooter.getInventory().setItem(1, new ItemStack(Material.ARROW, 1));
            }
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

    private Player topScorerOrLastAlive(Session session) {
        List<Player> alive = alivePlayers(session);
        if (alive.size() == 1) return alive.get(0);
        UUID best = null;
        int score = Integer.MIN_VALUE;
        for (Map.Entry<UUID, Integer> entry : session.scores.entrySet()) {
            if (entry.getValue() > score) {
                score = entry.getValue();
                best = entry.getKey();
            }
        }
        Player top = best == null ? null : session.players.get(best);
        return top != null && top.isOnline() ? top : alive.stream().findFirst().orElse(null);
    }

    private void finish(Session session, Player winner, boolean announceWinner) {
        if (active != session) return;
        active = null;
        if (session.tickTask != null) session.tickTask.cancel();
        if (announceWinner) {
            announce(session, winner == null ? ChatColor.YELLOW + "Round ended." : ChatColor.GOLD + "" + ChatColor.BOLD + winner.getName() + " won " + session.type.displayName() + "!");
        }
        for (Entity entity : new ArrayList<>(session.entities)) if (entity != null && entity.isValid()) entity.remove();
        session.bombOwners.clear();
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
        if (location.getY() < 58.0D) {
            eliminate(session, player, null, "fell from the arena");
            return;
        }
        if (session.type == ArcadePortGame.MICRO_BATTLES) {
            WorldBorder border = session.world.getWorldBorder();
            double half = border.getSize() / 2.0D;
            if (Math.abs(location.getX()) > half || Math.abs(location.getZ()) > half) eliminate(session, player, null, "caught by the closing border");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Session session = active;
        if (session == null || !isInsideSession(session, event.getTo())) return;
        Player player = event.getPlayer();
        if (!session.players.containsKey(player.getUniqueId())) return;
        if (session.type == ArcadePortGame.MICRO_BATTLES && session.alive.contains(player.getUniqueId())) {
            int team = session.teams.getOrDefault(player.getUniqueId(), -1);
            if (team >= 0 && team < 4) {
                Location ownIsland = islandSpawn(team * 2);
                Location to = event.getTo();
                double ownDistance = horizontalDistanceSquared(to, ownIsland);
                double nearestOther = Double.POSITIVE_INFINITY;
                for (int other = 0; other < 4; other++) {
                    if (other == team) continue;
                    nearestOther = Math.min(nearestOther, horizontalDistanceSquared(to, islandSpawn(other * 2)));
                }
                if (ArcadePortRules.shouldPunishMicroIslandExit(System.currentTimeMillis() - session.startedAt,
                        ownDistance, nearestOther)) {
                    eliminate(session, player, null, "left the starting island before the opening restriction ended");
                    return;
                }
            }
        }
        checkPlayerArena(session, player);
        if (active != session || !session.alive.contains(player.getUniqueId())) return;
        Location previous = session.lastLocations.get(player.getUniqueId());
        Location current = event.getTo();
        if (previous == null || current == null || (previous.getBlockX() == current.getBlockX() && previous.getBlockZ() == current.getBlockZ())) return;
        if (session.type == ArcadePortGame.RUNNER) {
            int x = previous.getBlockX(), z = previous.getBlockZ();
            if (Math.abs(x) < HALF_SIZE && Math.abs(z) < HALF_SIZE) {
                markRunnerBlock(session, session.world.getBlockAt(x, 63, z), System.currentTimeMillis());
            }
        } else if (session.type == ArcadePortGame.DRAGON_ESCAPE) {
            int crumbleZ = current.getBlockZ() - 5;
            if (crumbleZ > -24 && crumbleZ < 78) for (int x = -3; x <= 3; x++) session.world.getBlockAt(x, 63, crumbleZ).setType(Material.AIR, false);
        }
        session.lastLocations.put(player.getUniqueId(), current.clone());
    }

    private void markRunnerBlock(Session session, Block block, long now) {
        if (active != session || block == null || block.getY() != 63 || !block.getType().isSolid()
                || block.getType() == Material.BEDROCK || session.runnerBlocks.containsKey(block)) return;
        block.setType(Material.RED_TERRACOTTA, false);
        session.runnerBlocks.put(block, now);
    }

    private void processRunnerBlocks(Session session, long now) {
        java.util.Iterator<Map.Entry<Block, Long>> iterator = session.runnerBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Block, Long> entry = iterator.next();
            Block block = entry.getKey();
            if (block == null || block.getType() != Material.RED_TERRACOTTA) {
                iterator.remove();
                continue;
            }
            if (!ArcadePortRules.shouldDropRunnerBlock(now - entry.getValue())) continue;
            Location fallingLocation = block.getLocation().add(0.5D, 1.0D, 0.5D);
            block.setType(Material.AIR, false);
            iterator.remove();
            FallingBlock falling = session.world.spawnFallingBlock(fallingLocation, Material.RED_TERRACOTTA.createBlockData());
            falling.setDropItem(false);
            falling.setHurtEntities(true);
            session.entities.add(falling);
        }
    }

    private void handleRunnerFallingBlockHit(Session session, FallingBlock falling, Player target) {
        if (active != session || !session.alive.contains(target.getUniqueId())
                || !session.entities.remove(falling)) return;
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
        if (session == null || session.type != ArcadePortGame.RUNNER
                || !(event.getEntity() instanceof FallingBlock falling)
                || !session.entities.contains(falling)) return;
        event.setCancelled(true);
        session.entities.remove(falling);
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
        if (session.type == ArcadePortGame.SHEEP_QUEST && event.getEntity() instanceof Sheep sheep
                && session.sheep.contains(sheep)) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player target && session.players.containsKey(target.getUniqueId())) {
            event.setCancelled(true);
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

        if (session.type == ArcadePortGame.SPLEEF
                && (event.getEntity() instanceof Snowball || event.getEntity() instanceof Arrow)
                && event.getEntity() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter
                && session.alive.contains(shooter.getUniqueId())) {
            Block hit = event.getHitBlock();
            if (hit != null) {
                boolean arrow = projectile instanceof Arrow;
                double speed = projectile.getVelocity().length();
                int radius = ArcadePortRules.spleefProjectileClearRadius(arrow, speed);
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dz * dz > radius * radius) continue;
                        Block floor = session.world.getBlockAt(hit.getX() + dx, 63, hit.getZ() + dz);
                        if (floor.getType() == Material.SNOW_BLOCK) {
                            floor.setType(Material.AIR, false);
                            session.world.spawnParticle(Particle.CLOUD, floor.getLocation().add(0.5D, 0.5D, 0.5D),
                                    4, 0.2D, 0.2D, 0.2D, 0.02D);
                        }
                    }
                }
                projectile.remove();
            }
            return;
        }

        if (session.type == ArcadePortGame.RUNNER && event.getEntity() instanceof Arrow arrow
                && arrow.getShooter() instanceof Player shooter && session.alive.contains(shooter.getUniqueId())) {
            Block hit = event.getHitBlock();
            if (hit != null) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (dx * dx + dz * dz > 6) continue;
                        markRunnerBlock(session, session.world.getBlockAt(hit.getX() + dx, 63, hit.getZ() + dz),
                                System.currentTimeMillis());
                    }
                }
                arrow.remove();
            }
            return;
        }

        if (session.type == ArcadePortGame.DRAGON_ESCAPE && event.getEntity() instanceof Fireball fireball
                && session.entities.contains(fireball)) {
            if (event.getHitEntity() instanceof Player target && session.alive.contains(target.getUniqueId())) {
                handleDragonEscapeHazardHit(session, fireball, target);
            }
            session.entities.remove(fireball);
            fireball.remove();
            return;
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
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;
        Session session = active;
        if (session == null) return;
        Player player = event.getPlayer();
        if (!session.alive.contains(player.getUniqueId())) return;
        ItemStack heldItem = event.getItem();
        Material item = heldItem == null ? Material.AIR : heldItem.getType();
        if (session.type == ArcadePortGame.BOMB_LOBBERS && item == Material.TNT) {
            event.setCancelled(true);
            consumeHeld(player);
            Location spawn = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.8D));
            TNTPrimed bomb = session.world.spawn(spawn, TNTPrimed.class);
            bomb.setFuseTicks(55);
            Vector velocity = player.getLocation().getDirection().multiply(1.45D);
            velocity.setY(velocity.getY() + 0.28D);
            bomb.setVelocity(velocity);
            session.entities.add(bomb);
            session.bombOwners.put(bomb.getUniqueId(), player.getUniqueId());
            player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 0.9F, 1.0F);
        } else if (session.type == ArcadePortGame.DRAGONS && item == Material.BLAZE_POWDER) {
            event.setCancelled(true);
            consumeHeld(player);
            Location spawn = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.2D));
            Fireball fireball = session.world.spawn(spawn, Fireball.class);
            fireball.setShooter(player);
            fireball.setYield(0.0F);
            fireball.setIsIncendiary(false);
            fireball.setVelocity(player.getLocation().getDirection().multiply(0.8D));
            session.entities.add(fireball);
        } else if ((session.type == ArcadePortGame.RUNNER || session.type == ArcadePortGame.DRAGON_ESCAPE) && item == Material.FEATHER) {
            event.setCancelled(true);
            consumeHeld(player);
            player.setVelocity(player.getLocation().getDirection().multiply(1.0D).setY(0.45D));
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.3D, 0.3D, 0.3D, 0.05D);
        }
    }

    private void consumeHeld(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) return;
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
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
        if (team < 0 || team > 1) return;
        if (owner == team && isInsideSheepPen(sheep.getLocation(), team)) {
            player.sendMessage(ChatColor.YELLOW + "That sheep is already safe in your pen.");
            return;
        }
        if (session.sheepCarriers.containsValue(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "Return the sheep you are carrying first.");
            return;
        }
        session.sheepCarriers.put(sheep, playerId);
        sheep.setAI(false);
        sheep.setColor(team == 0 ? DyeColor.RED : DyeColor.BLUE);
        player.playSound(player.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 1.0F, 2.0F);
        player.sendMessage(ChatColor.GREEN + "Sheep picked up. Carry it to your team's pen!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Session session = active;
        if (session == null || event.getBlock().getWorld() != session.world) return;
        if (session.type == ArcadePortGame.SPLEEF && event.getBlock().getY() == 63 && event.getBlock().getType() == Material.SNOW_BLOCK
                && session.alive.contains(event.getPlayer().getUniqueId())) {
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }
        if (session.type == ArcadePortGame.MICRO_BATTLES && session.alive.contains(event.getPlayer().getUniqueId())
                && event.getBlock().getType() == Material.COBBLESTONE && event.getBlock().getY() >= 64
                && event.getBlock().getY() <= 72) {
            event.setDropItems(false);
            event.setExpToDrop(0);
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
    public void onBlockPlace(BlockPlaceEvent event) {
        Session session = active;
        if (session == null || event.getBlock().getWorld() != session.world) return;
        if (session.type == ArcadePortGame.MICRO_BATTLES) {
            Player player = event.getPlayer();
            boolean allowed = session.alive.contains(player.getUniqueId())
                    && event.getBlockPlaced().getType() == Material.COBBLESTONE
                    && event.getBlockPlaced().getY() >= 64 && event.getBlockPlaced().getY() <= 72
                    && Math.abs(event.getBlockPlaced().getX()) <= HALF_SIZE
                    && Math.abs(event.getBlockPlaced().getZ()) <= HALF_SIZE;
            if (!allowed) event.setCancelled(true);
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
            int x = event.getBlockPlaced().getX();
            if ((team == 0 && x > session.turfFrontlineX) || (team == 1 && x <= session.turfFrontlineX)) {
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Session session = active;
        if (session == null || event.getEntity().getWorld() != session.world) return;
        if (event.getEntity() instanceof TNTPrimed bomb) {
            UUID ownerId = session.bombOwners.remove(bomb.getUniqueId());
            if (ownerId != null && session.type == ArcadePortGame.BOMB_LOBBERS) {
                Location blast = bomb.getLocation().clone();
                Player owner = session.players.get(ownerId);
                event.blockList().clear();
                event.setCancelled(true);
                session.entities.remove(bomb);
                bomb.remove();
                session.world.spawnParticle(Particle.EXPLOSION, blast, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                session.world.playSound(blast, Sound.ENTITY_GENERIC_EXPLODE, 1.4F, 0.9F);
                for (Player target : alivePlayers(session)) {
                    if (owner != null && session.teams.getOrDefault(ownerId, -1)
                            .equals(session.teams.getOrDefault(target.getUniqueId(), -2))) continue;
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
                    if (owner != null && owner.isOnline() && distance <= 3.0D) {
                        owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.45F);
                        owner.sendMessage(ChatColor.YELLOW + "Direct hit!");
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
        final Map<UUID, Integer> evolutionLevels = new HashMap<>();
        final Map<UUID, Integer> teams = new HashMap<>();
        final Set<UUID> tagged = new HashSet<>();
        final Set<UUID> respawning = new HashSet<>();
        final Set<Sheep> sheep = new HashSet<>();
        final Map<Sheep, Integer> sheepOwners = new HashMap<>();
        final Map<Sheep, UUID> sheepCarriers = new HashMap<>();
        final Set<Entity> entities = new HashSet<>();
        final Map<UUID, UUID> bombOwners = new HashMap<>();
        final Map<Block, Long> runnerBlocks = new HashMap<>();
        final Map<UUID, Location> lastLocations = new HashMap<>();
        final int participantsAtStart;
        double baconKnockback = 1.5D;
        long lastBaconHungerAt;
        long lastBaconEscalationAt;
        long baconEscalationIntervalMillis = ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS;
        long startedAt;
        long nextDragonEscapeHazardAt;
        boolean microBarrierActive;
        boolean turfCombat;
        long turfPhaseEndsAt;
        int turfFrontlineX = -1;
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
        final float exp;
        final int level;
        final boolean allowFlight;
        final boolean flying;
        final double health;
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
            exp = p.getExp();
            level = p.getLevel();
            allowFlight = p.getAllowFlight();
            flying = p.isFlying();
            health = p.getHealth();
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
            p.setExp(exp);
            p.setLevel(level);
            p.setFoodLevel(food);
            p.setSaturation(saturation);
            p.setWalkSpeed(walkSpeed);
            p.setAllowFlight(allowFlight);
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
