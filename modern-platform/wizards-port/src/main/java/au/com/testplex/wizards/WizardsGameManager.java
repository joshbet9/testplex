package au.com.testplex.wizards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Fireball;
import org.bukkit.block.TileState;
import org.bukkit.entity.Firework;
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
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class WizardsGameManager implements Listener {
    private static final String SPELL_MENU_TITLE = ChatColor.DARK_PURPLE + "Wizards Spell Selector";
    private static final int MAX_WAND_SLOTS = WizardsRules.WAND_SLOT_COUNT;
    private static final int MENU_SLOT = 8;
    private static final int ARENA_FLOOR_Y = 63;
    private static final int CHEST_COUNT_FALLBACK = 32;

    private final WizardsModernPlugin plugin;
    private final NamespacedKey spellKey;
    private final NamespacedKey wandKey;
    private final NamespacedKey menuKey;
    private final NamespacedKey soulKey;
    private final NamespacedKey projectileKey;
    private final NamespacedKey projectileDamageKey;
    private final NamespacedKey projectileOwnerKey;
    private final NamespacedKey anvilLevelKey;
    private final NamespacedKey meteorKey;
    private final LinkedHashMap<UUID, Player> queue = new LinkedHashMap<>();
    private final Map<UUID, WizardsKit> selectedKits = new HashMap<>();
    private final Map<UUID, WizardState> states = new LinkedHashMap<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Map<UUID, PlayerSnapshot> pendingRestore = new HashMap<>();
    private final Map<UUID, BossBar> manaBars = new HashMap<>();
    private final Map<Location, TemporaryBlock> temporaryBlocks = new HashMap<>();
    private final Set<UUID> ownedEntities = new HashSet<>();
    private final Map<UUID, SpectralArrowData> spectralArrows = new HashMap<>();
    private final Map<UUID, SummonedWolfData> summonedWolves = new HashMap<>();
    private final Map<UUID, ManaBoltData> manaBolts = new HashMap<>();
    private final Map<UUID, Long> fallingProtection = new HashMap<>();
    private final List<TrapRuneData> trapRunes = new ArrayList<>();
    private final List<BukkitTask> spellTasks = new ArrayList<>();
    private final Map<UUID, MeteorData> endgameMeteors = new HashMap<>();
    private final List<Location> chests = new ArrayList<>();
    private final Random random = new Random();

    private final World arena;
    private final BukkitTask tickTask;
    private boolean running;
    private long startedAt;
    private int tick;
    private int startingPlayerCount;
    private int paceStep;
    private boolean endgameStarted;
    private boolean suddenDeathTriggered;
    private int endgameType;
    private int endgameMessageCounter;
    private int nextEndgameStrikeMillis = WizardsRules.ENDGAME_INITIAL_STRIKE_MILLIS;
    private long lastEndgameStrikeAt;
    private long lastEndgameMoanAt;
    private double endgameAccuracy;
    private float endgameSize = 1.5F;
    private float meteorSpeed = 0.05F;

    public WizardsGameManager(WizardsModernPlugin plugin) {
        this.plugin = plugin;
        spellKey = new NamespacedKey(plugin, "spell");
        wandKey = new NamespacedKey(plugin, "wand");
        menuKey = new NamespacedKey(plugin, "spell-menu");
        soulKey = new NamespacedKey(plugin, "soul-star");
        projectileKey = new NamespacedKey(plugin, "spell-projectile");
        projectileDamageKey = new NamespacedKey(plugin, "spell-damage");
        projectileOwnerKey = new NamespacedKey(plugin, "spell-owner");
        anvilLevelKey = new NamespacedKey(plugin, "anvil-level");
        meteorKey = new NamespacedKey(plugin, "endgame-meteor-size");
        arena = createArena();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private World createArena() {
        String worldName = plugin.getConfig().getString("arena-world", "wizards_arena");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            world = creator.createWorld();
        }
        if (world == null) {
            plugin.getLogger().severe("Could not create the isolated Wizards arena world.");
            return null;
        }

        int radius = Math.max(32, Math.min(96, plugin.getConfig().getInt("arena-radius", 48)));
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0D, 0.0D);
        border.setSize((radius * 2.0D) + 4.0D);
        border.setDamageAmount(0.2D);
        border.setWarningDistance(5);
        world.setSpawnLocation(0, ARENA_FLOOR_Y + 1, 0);

        // No original Wizards archive is present in this repository. Build a
        // predictable mechanics arena rather than silently substituting another game map.
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                world.getBlockAt(x, ARENA_FLOOR_Y, z).setType(Material.GRASS_BLOCK, false);
                if (Math.abs(x) == radius || Math.abs(z) == radius) {
                    for (int y = ARENA_FLOOR_Y + 1; y <= ARENA_FLOOR_Y + 4; y++) {
                        world.getBlockAt(x, y, z).setType(Material.STONE_BRICKS, false);
                    }
                }
            }
        }
        for (int x = -radius + 1; x < radius; x++) {
            for (int z = -radius + 1; z < radius; z++) {
                if (Math.abs(x) > 2 && Math.abs(z) > 2) {
                    world.getBlockAt(x, ARENA_FLOOR_Y - 1, z).setType(Material.DIRT, false);
                }
            }
        }

        int chestCount = Math.max(8, Math.min(64, plugin.getConfig().getInt("chest-count", CHEST_COUNT_FALLBACK)));
        chests.clear();
        for (int i = 0; i < chestCount; i++) {
            double angle = (Math.PI * 2.0D * i) / chestCount;
            double chestRadius = 12.0D + (i % 3) * 9.0D;
            int x = (int) Math.round(Math.cos(angle) * chestRadius);
            int z = (int) Math.round(Math.sin(angle) * chestRadius);
            Block block = world.getBlockAt(x, ARENA_FLOOR_Y + 1, z);
            block.setType(Material.CHEST, false);
            chests.add(block.getLocation());
        }

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) entity.remove();
        }
        return world;
    }

    public void join(Player player) {
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Wizards arena could not be loaded. Check the server log.");
            return;
        }
        if (running) {
            player.sendMessage(ChatColor.RED + "A Wizards round is already running.");
            return;
        }
        if (queue.putIfAbsent(player.getUniqueId(), player) != null) {
            player.sendMessage(ChatColor.YELLOW + "You are already queued for Wizards.");
            return;
        }
        selectedKits.putIfAbsent(player.getUniqueId(), defaultKit());
        player.sendMessage(ChatColor.GREEN + "Queued for Wizards. Choose a kit with /wizards kit <kit>.");
        player.sendMessage(ChatColor.GRAY + "An operator or queued player can start with /wizards start.");
    }

    public void setKit(Player player, WizardsKit kit) {
        if (running) {
            player.sendMessage(ChatColor.RED + "Kits cannot be changed after the round starts.");
            return;
        }
        if (!queue.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Join the Wizards queue first with /wizards join.");
            return;
        }
        selectedKits.put(player.getUniqueId(), kit);
        player.sendMessage(ChatColor.GREEN + "Wizards kit set to " + ChatColor.WHITE + prettyKit(kit) + ChatColor.GREEN + ".");
    }

    private WizardsKit defaultKit() {
        WizardsKit kit = WizardsKit.parse(plugin.getConfig().getString("kits.default", "MAGE"));
        return kit == null ? WizardsKit.MAGE : kit;
    }

    public void start(org.bukkit.command.CommandSender sender) {
        if (running) {
            sender.sendMessage(ChatColor.RED + "Wizards is already running.");
            return;
        }
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Wizards arena failed to load.");
            return;
        }

        List<Player> entrants = new ArrayList<>();
        for (Player player : queue.values()) {
            if (player.isOnline()) entrants.add(player);
        }
        int minimum = Math.max(1, plugin.getConfig().getInt("minimum-players", 2));
        boolean allowSolo = plugin.getConfig().getBoolean("allow-solo-start", true);
        if (entrants.size() < minimum && !(allowSolo && entrants.size() == 1)) {
            sender.sendMessage(ChatColor.RED + "Wizards needs " + minimum + " queued players to start.");
            return;
        }
        if (entrants.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Nobody is queued for Wizards.");
            return;
        }

        refillChests();
        running = true;
        startedAt = System.currentTimeMillis();
        tick = 0;
        paceStep = 0;
        startingPlayerCount = entrants.size();
        endgameStarted = false;
        suddenDeathTriggered = false;
        endgameType = random.nextInt(2);
        endgameMessageCounter = 0;
        nextEndgameStrikeMillis = WizardsRules.ENDGAME_INITIAL_STRIKE_MILLIS;
        lastEndgameStrikeAt = 0L;
        lastEndgameMoanAt = 0L;
        endgameAccuracy = 0.0D;
        endgameSize = 1.5F;
        meteorSpeed = 0.05F;
        endgameMeteors.clear();
        states.clear();
        for (int i = 0; i < entrants.size(); i++) {
            Player player = entrants.get(i);
            UUID id = player.getUniqueId();
            snapshots.put(id, PlayerSnapshot.capture(player));
            WizardsKit kit = selectedKits.getOrDefault(id, defaultKit());
            WizardState state = new WizardState(kit);
            states.put(id, state);
            preparePlayer(player, state, i, entrants.size());
        }
        queue.clear();
        sender.sendMessage(ChatColor.GREEN + "Started Wizards with " + entrants.size() + " player(s).");
        for (Player player : entrants) {
            player.sendMessage(ChatColor.GOLD + "Wizards" + ChatColor.DARK_GRAY + " » "
                    + ChatColor.YELLOW + "Find spellbooks and supplies in chests. Last wizard alive wins!");
        }
    }

    private void preparePlayer(Player player, WizardState state, int index, int count) {
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setFoodLevel(20);
        player.setSaturation(5.0F);
        player.setLevel(0);
        player.setExp(0.0F);
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) == null
                ? 20.0D : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHealth, 20.0D));

        double angle = (Math.PI * 2.0D * index) / Math.max(1, count);
        double spawnRadius = Math.max(20.0D, Math.min(35.0D, plugin.getConfig().getInt("arena-radius", 48) - 12.0D));
        Location spawn = new Location(arena, Math.cos(angle) * spawnRadius + 0.5D,
                ARENA_FLOOR_Y + 1.0D, Math.sin(angle) * spawnRadius + 0.5D);
        spawn.setDirection(new Vector(-Math.cos(angle), 0.0D, -Math.sin(angle)));
        player.teleport(spawn);

        for (int slot = 0; slot < MAX_WAND_SLOTS; slot++) {
            if (slot < state.wandCount) {
                player.getInventory().setItem(slot, makeWand(state, slot));
            } else {
                player.getInventory().setItem(slot, makeEmptyWandSlot());
            }
        }
        player.getInventory().setItem(MENU_SLOT, makeSpellMenuBook());
        refreshManaBar(player, state);
        updateWandLabels(player, state);
        player.setCompassTarget(spawn);
    }

    public void leave(Player player) {
        UUID id = player.getUniqueId();
        if (queue.remove(id) != null) {
            selectedKits.remove(id);
            player.sendMessage(ChatColor.YELLOW + "You left the Wizards queue.");
            return;
        }
        WizardState state = states.get(id);
        if (running && state != null && state.alive) {
            state.alive = false;
            removeManaBar(id);
            restoreTemporaryPlayer(player, id);
            player.sendMessage(ChatColor.YELLOW + "You left the Wizards round.");
            Bukkit.getScheduler().runTask(plugin, this::checkWinner);
            return;
        }
        player.sendMessage(ChatColor.GRAY + "You are not queued or alive in a Wizards round.");
    }

    public String status() {
        if (!running) return queue.size() + " queued; no round running";
        long alive = states.values().stream().filter(state -> state.alive).count();
        long seconds = Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L);
        return alive + "/" + states.size() + " alive; " + queue.size() + " queued; " + seconds + "s elapsed";
    }

    public void stop(String reason) {
        if (!running && snapshots.isEmpty()) {
            queue.clear();
            states.clear();
            return;
        }
        finish(null, reason);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        if (running || !snapshots.isEmpty()) finish(null, "Wizards plugin disabled; player state restored.");
        for (Map.Entry<UUID, PlayerSnapshot> entry : pendingRestore.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && !player.isDead()) entry.getValue().restore(player);
        }
        pendingRestore.clear();
    }

    private void tick() {
        if (!running) return;
        tick++;
        tickSpectralArrows();
        tickSummonedWolves();
        tickManaBolts();
        tickTrapRunes();
        tickFallingProtection();
        tickEndgame();
        tickEndgameMeteors();
        expireTemporaryBlocks();
        long elapsed = System.currentTimeMillis() - startedAt;

        while (paceStep < WizardsRules.GAME_PACE_MINUTES.length
                && elapsed >= WizardsRules.GAME_PACE_MINUTES[paceStep] * 60_000L) {
            paceStep++;
            float modifier = Math.max(0.5F, 1.0F - paceStep * 0.1F);
            for (WizardState state : states.values()) state.cooldownModifier = modifier;
            for (Player player : alivePlayers()) {
                player.sendMessage(ChatColor.YELLOW + "Power surges through the battlefield! Spell costs and cooldowns are reduced.");
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7F, 1.2F);
            }
        }

        for (Map.Entry<UUID, WizardState> entry : states.entrySet()) {
            WizardState state = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (!state.alive || player == null || !player.isOnline()) continue;

            state.mana = WizardsRules.addMana(state.mana, WizardsRules.manaPerSecond(state.kit, state.soulStars) / 20.0F, state.maximumMana);
            if (tick % 10 == 0) {
                refreshManaBar(player, state);
                updateWandLabels(player, state);
            }
        }
    }

    private void tickEndgame() {
        if (!running || startedAt <= 0L) return;
        long now = System.currentTimeMillis();
        long elapsed = now - startedAt;

        if (!endgameStarted && elapsed >= WizardsRules.ENDGAME_START_MINUTES * 60_000L) {
            endgameStarted = true;
            endgameMessageCounter = 0;
            nextEndgameStrikeMillis = WizardsRules.ENDGAME_INITIAL_STRIKE_MILLIS;
            lastEndgameStrikeAt = 0L;
            lastEndgameMoanAt = now;
        }

        if (endgameStarted) {
            long phaseElapsed = elapsed - WizardsRules.ENDGAME_START_MINUTES * 60_000L;
            if (endgameMessageCounter <= 6) {
                long warningSecond = phaseElapsed / 1000L;
                if (warningSecond >= endgameMessageCounter) {
                    announceEndgameWarning(endgameMessageCounter);
                    if (endgameMessageCounter == 6 && arena != null) arena.setTime(15_000L);
                    endgameMessageCounter++;
                }
            } else {
                nextEndgameStrikeMillis = WizardsRules.nextEndgameStrikeInterval(nextEndgameStrikeMillis);
                if (lastEndgameStrikeAt == 0L || now - lastEndgameStrikeAt >= nextEndgameStrikeMillis) {
                    lastEndgameStrikeAt = now;
                    if (endgameType == 0) makeEndgameMeteor();
                    else makeEndgameLightning();
                }
                if (now - lastEndgameMoanAt >= 5_000L + random.nextInt(8_000)) {
                    Sound sound = random.nextBoolean() ? Sound.ENTITY_GHAST_AMBIENT : Sound.ENTITY_GHAST_SCREAM;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), sound, 0.7F, random.nextFloat() / 10.0F);
                    }
                    lastEndgameMoanAt = now;
                }
            }
        }

        if (!suddenDeathTriggered && elapsed >= WizardsRules.SUDDEN_DEATH_MINUTES * 60_000L) {
            suddenDeathTriggered = true;
            applySuddenDeath();
        }
    }

    private void announceEndgameWarning(int index) {
        String message;
        if (endgameType == 0) {
            message = switch (index) {
                case 0 -> "Broken is the cage, the skies scream with rage!";
                case 2 -> "The ground trembles with fear, your doom is here!";
                case 4 -> "Where the wizards stand, meteors strike the land!";
                case 6 -> "Fight to the death! Fight with your dying breath!";
                default -> null;
            };
        } else {
            message = switch (index) {
                case 0 -> "Storm rumbles through the sky, birds fly high!";
                case 2 -> "Lightning strikes the earth, terror given birth!";
                case 4 -> "Lightning flickering through the air, doom is here!";
                case 6 -> "Fight to the death! Fight with your dying breath!";
                default -> null;
            };
        }
        if (message != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.YELLOW.toString() + ChatColor.BOLD + message);
            }
        }
        if (index == 6 && arena != null) {
            arena.setStorm(false);
            arena.setThundering(endgameType == 1);
        }
    }

    private Location getEndgameLocation() {
        List<Player> alive = alivePlayers();
        if (alive.isEmpty() || arena == null) return null;

        int chance = random.nextInt(50) + 3;
        int accuracy = Math.max((int) (chance - endgameAccuracy * chance), 1);
        endgameAccuracy += 0.0001D;
        for (int attempt = 0; attempt < 50; attempt++) {
            Player target = alive.get(random.nextInt(alive.size()));
            Location targetLocation = target.getLocation();
            int x = targetLocation.getBlockX() + random.nextInt(accuracy * 2) - accuracy;
            int z = targetLocation.getBlockZ() + random.nextInt(accuracy * 2) - accuracy;
            Block highest = arena.getHighestBlockAt(x, z);
            Location location = highest.getLocation().add(0.5D, 0.0D, 0.5D);
            if (location.getBlockY() > 0 && location.getBlock().getType().isSolid()) return location;
        }
        return null;
    }

    private void makeEndgameLightning() {
        Location location = getEndgameLocation();
        if (location == null) return;
        World world = location.getWorld();
        world.strikeLightningEffect(location);
        world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.5F,
                0.8F + random.nextFloat() * 0.5F);
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.2F,
                0.9F + random.nextFloat() / 3.0F);
        world.spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(0.0D, 1.0D, 0.0D),
                45, 1.3D, 1.0D, 1.3D, 0.12D);
        world.spawnParticle(Particle.FLASH, location, 2, 0.3D, 0.2D, 0.3D, 0.0D);

        double radius = 4.0D * endgameSize;
        double baseDamage = 6.0D * endgameSize;
        for (Player victim : alivePlayers()) {
            if (!victim.getWorld().equals(world)) continue;
            double distance = victim.getLocation().distance(location);
            if (distance >= radius) continue;
            double damage = baseDamage * (1.0D - distance / radius);
            if (damage > 0.0D) victim.damage(damage);
        }
        if (plugin.getConfig().getBoolean("destructive-endgame-blocks", false)) {
            destroyEndgameBlocks(location, 3.0D * endgameSize, 20);
        }
    }

    private void makeEndgameMeteor() {
        Location target = getEndgameLocation();
        if (target == null) return;

        meteorSpeed += 0.002F;
        endgameSize = Math.min(10.0F, endgameSize + 0.04F);
        Vector fromTarget = new Vector(random.nextDouble() - 0.5D, 0.8D,
                random.nextDouble() - 0.5D).normalize();
        Location spawn = target.clone()
                .add((random.nextDouble() - 0.5D) * 7.0D, 0.0D,
                        (random.nextDouble() - 0.5D) * 7.0D)
                .add(fromTarget.clone().multiply(40.0D));

        Fireball meteor = spawn.getWorld().spawn(spawn, Fireball.class);
        meteor.setIsIncendiary(false);
        meteor.setYield(0.0F);
        meteor.setBounce(false);
        meteor.setVelocity(fromTarget.multiply(-(0.04D + ((meteorSpeed - 0.05D) / 2.0D))));
        meteor.getPersistentDataContainer().set(meteorKey, PersistentDataType.DOUBLE, (double) endgameSize);
        endgameMeteors.put(meteor.getUniqueId(), new MeteorData((double) endgameSize));
        ownedEntities.add(meteor.getUniqueId());
        spawn.getWorld().playSound(spawn, Sound.ENTITY_GHAST_SHOOT, 1.2F, 0.6F);
    }

    private void tickEndgameMeteors() {
        if (endgameMeteors.isEmpty()) return;
        for (Map.Entry<UUID, MeteorData> entry : new ArrayList<>(endgameMeteors.entrySet())) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Fireball meteor) || !meteor.isValid()) {
                endgameMeteors.remove(entry.getKey());
                ownedEntities.remove(entry.getKey());
                continue;
            }
            MeteorData data = entry.getValue();
            meteor.getWorld().spawnParticle(Particle.FLAME, meteor.getLocation(), 3,
                    0.18D, 0.18D, 0.18D, 0.02D);
            meteor.getWorld().spawnParticle(Particle.SMOKE, meteor.getLocation(), 2,
                    0.13D, 0.13D, 0.13D, 0.01D);
            if (++data.ticks % 6 == 0) {
                meteor.getWorld().playSound(meteor.getLocation(), Sound.ENTITY_CAT_HISS, 1.0F, 0.6F);
            }
        }
    }

    private void handleEndgameMeteorImpact(Fireball meteor) {
        MeteorData data = endgameMeteors.remove(meteor.getUniqueId());
        double size = data == null
                ? meteor.getPersistentDataContainer().getOrDefault(meteorKey, PersistentDataType.DOUBLE, 1.5D)
                : data.size;
        Location impact = meteor.getLocation();
        World world = impact.getWorld();
        if (world == null) return;

        double radius = 4.0D * size;
        double baseDamage = 6.0D * size;
        world.spawnParticle(Particle.EXPLOSION, impact, 3, 0.7D, 0.5D, 0.7D, 0.02D);
        world.spawnParticle(Particle.FLAME, impact, 42, 1.0D, 0.7D, 1.0D, 0.06D);
        world.spawnParticle(Particle.CLOUD, impact, 35, 0.9D, 0.5D, 0.9D, 0.08D);
        world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.7F);
        for (Player victim : alivePlayers()) {
            if (!victim.getWorld().equals(world)) continue;
            double distance = victim.getLocation().distance(impact);
            if (distance >= radius) continue;
            double damage = baseDamage * (1.0D - distance / radius);
            if (damage > 0.0D) victim.damage(damage);
        }
        if (plugin.getConfig().getBoolean("destructive-endgame-blocks", false)) {
            destroyEndgameBlocks(impact, 1.4D * size, 20);
        }
        ownedEntities.remove(meteor.getUniqueId());
        meteor.remove();
        Bukkit.getScheduler().runTaskLater(plugin, this::checkWinner, 2L);
    }

    private void destroyEndgameBlocks(Location centre, double radius, int maximumBlocks) {
        World world = centre.getWorld();
        if (world == null) return;
        List<Block> candidates = new ArrayList<>();
        int extent = Math.max(1, (int) Math.ceil(radius));
        for (int x = -extent; x <= extent; x++) {
            for (int y = -extent; y <= extent; y++) {
                for (int z = -extent; z <= extent; z++) {
                    Block block = world.getBlockAt(centre.getBlockX() + x,
                            centre.getBlockY() + y, centre.getBlockZ() + z);
                    if (block.getType().isAir() || block.getType() == Material.BEDROCK
                            || block.getState() instanceof InventoryHolder
                            || block.getState() instanceof TileState) continue;
                    if (block.getLocation().add(0.5D, 0.5D, 0.5D).distance(centre) <= radius) candidates.add(block);
                }
            }
        }
        java.util.Collections.shuffle(candidates, random);
        int removed = 0;
        for (Block block : candidates) {
            if (removed >= maximumBlocks) break;
            world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5D, 0.5D, 0.5D),
                    6, 0.25D, 0.25D, 0.25D, 0.05D, block.getBlockData());
            block.setType(Material.AIR, false);
            removed++;
        }
    }

    private void applySuddenDeath() {
        List<Player> players = alivePlayers();
        if (players.size() <= 1) return;
        players.sort(Comparator.comparingDouble(Player::getHealth).reversed());
        for (int i = 0; i < players.size() - 1; i++) {
            Player player = players.get(i);
            player.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "The final magic consumes all but one wizard!");
            player.damage(9999.0D);
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::checkWinner, 2L);
    }

    private List<Player> alivePlayers() {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<UUID, WizardState> entry : states.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (entry.getValue().alive && player != null && player.isOnline()) players.add(player);
        }
        return players;
    }

    private void refreshManaBar(Player player, WizardState state) {
        BossBar bar = manaBars.computeIfAbsent(player.getUniqueId(), ignored ->
                Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_10));
        bar.setTitle(ChatColor.AQUA + "Mana " + ChatColor.WHITE + Math.round(state.mana) + "/" +
                Math.round(state.maximumMana) + ChatColor.DARK_GRAY + "  |  " + ChatColor.LIGHT_PURPLE + "Souls " + state.soulStars);
        bar.setProgress(WizardsRules.manaProgress(state.mana, state.maximumMana));
        if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
        bar.setVisible(true);
    }

    private void removeManaBar(UUID id) {
        BossBar bar = manaBars.remove(id);
        if (bar != null) bar.removeAll();
    }

    private void updateWandLabels(Player player, WizardState state) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < state.wandCount; slot++) {
            if (!isWand(inventory.getItem(slot))) {
                inventory.setItem(slot, makeWand(state, slot));
            } else {
                inventory.setItem(slot, makeWand(state, slot));
            }
        }
    }

    private ItemStack makeWand(WizardState state, int slot) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        WizardsSpell spell = state.wands[slot];
        meta.setDisplayName(ChatColor.WHITE + (spell == null ? "Spell Wand" : spell.displayName() + " Wand"));
        List<String> lore = new ArrayList<>();
        if (spell == null) {
            lore.add(ChatColor.GRAY + "Right-click to choose a spell");
        } else {
            int level = state.known.getOrDefault(spell, 1);
            lore.add(ChatColor.YELLOW + "Mana: " + ChatColor.WHITE + spell.manaCost(level));
            lore.add(ChatColor.YELLOW + "Cooldown: " + ChatColor.WHITE + spell.cooldownSeconds(level, state.cooldownModifier) + "s");
            lore.add(ChatColor.GRAY + "Left-click to cast");
            lore.add(ChatColor.GRAY + "Right-click to change spell");
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "wand");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeEmptyWandSlot() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Empty wand slot");
        meta.setLore(List.of(ChatColor.DARK_GRAY + "Find an extra wand in a chest or from a fallen wizard."));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "empty");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSpellMenuBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Wizard Spells");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to view discovered spells",
                ChatColor.GRAY + "Bind a spell to the wand in your hand."));
        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSpellBook(WizardsSpell spell) {
        ItemStack item = new ItemStack(spell.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(categoryColour(spell) + spell.displayName() + " Spellbook");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click or pick up to learn this spell",
                ChatColor.YELLOW + "Maximum level: " + ChatColor.WHITE + spell.maximumLevel(),
                ChatColor.DARK_GRAY + "Wizards spell: " + spell.id()));
        meta.getPersistentDataContainer().set(spellKey, PersistentDataType.STRING, spell.id());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeWandLoot() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Spell Wand");
        meta.setLore(List.of(ChatColor.GRAY + "Adds another wand slot, up to five."));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "loot");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSoulStar() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Wizard Soul");
        meta.setLore(List.of(ChatColor.GRAY + "Permanently increases mana regeneration this round."));
        meta.getPersistentDataContainer().set(soulKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void openSpellMenu(Player player, WizardState state) {
        Inventory inventory = Bukkit.createInventory(null, 54, SPELL_MENU_TITLE);
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
        int index = 0;
        for (WizardsSpell spell : WizardsSpell.implementedSpells()) {
            int level = state.known.getOrDefault(spell, 0);
            if (level <= 0) continue;
            ItemStack icon = new ItemStack(spell.icon());
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(categoryColour(spell) + spell.displayName());
            meta.setLore(List.of(
                    ChatColor.YELLOW + "Level: " + ChatColor.WHITE + level + "/" + spell.maximumLevel(),
                    ChatColor.YELLOW + "Mana: " + ChatColor.WHITE + spell.manaCost(level),
                    ChatColor.YELLOW + "Cooldown: " + ChatColor.WHITE + spell.cooldownSeconds(level, state.cooldownModifier) + " seconds",
                    "",
                    ChatColor.GREEN + "Left-click: bind to held wand",
                    ChatColor.GREEN + "Right-click: quickcast"));
            meta.getPersistentDataContainer().set(spellKey, PersistentDataType.STRING, spell.id());
            icon.setAmount(Math.max(1, Math.min(64, level)));
            icon.setItemMeta(meta);
            inventory.setItem(slots[index++], icon);
        }
        if (index == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "No spells discovered");
            meta.setLore(List.of(ChatColor.GRAY + "Find spellbooks in arena chests."));
            empty.setItemMeta(meta);
            inventory.setItem(13, empty);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        WizardState state = states.get(player.getUniqueId());
        if (!running || state == null || !state.alive) return;

        Action action = event.getAction();
        ItemStack item = event.getItem();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (isMenuBook(item)) {
                event.setCancelled(true);
                openSpellMenu(player, state);
                return;
            }
            int slot = player.getInventory().getHeldItemSlot();
            if (slot >= 0 && slot < state.wandCount && isWand(item)) {
                event.setCancelled(true);
                openSpellMenu(player, state);
                return;
            }
        }
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            int slot = player.getInventory().getHeldItemSlot();
            if (slot >= 0 && slot < state.wandCount && isWand(item)) {
                event.setCancelled(true);
                WizardsSpell spell = state.wands[slot];
                if (spell != null) castSpell(player, state, spell, null, event.getClickedBlock());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        WizardState state = states.get(player.getUniqueId());
        if (state == null || !state.alive) return;

        if (SPELL_MENU_TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            WizardsSpell spell = spellFromItem(event.getCurrentItem());
            if (spell == null) return;
            int heldSlot = player.getInventory().getHeldItemSlot();
            if (heldSlot < 0 || heldSlot >= state.wandCount) {
                player.sendMessage(ChatColor.RED + "Hold one of your wands before selecting a spell.");
                player.closeInventory();
                return;
            }
            if (event.getClick().isLeftClick()) {
                state.wands[heldSlot] = spell;
                player.sendMessage(ChatColor.BLUE + "Spell on wand set to " + categoryColour(spell) + spell.displayName());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
                updateWandLabels(player, state);
                player.closeInventory();
            } else if (event.getClick().isRightClick()) {
                player.closeInventory();
                castSpell(player, state, spell, null, null);
            }
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() != null && event.getClickedInventory() == top
                && top.getType() == InventoryType.CHEST) {
            ItemStack clicked = event.getCurrentItem();
            if (isSpellBook(clicked) || isLootWand(clicked) || isSoulStar(clicked)) {
                event.setCancelled(true);
                consumeLootItem(player, state, clicked);
                event.getClickedInventory().setItem(event.getSlot(), null);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.0F);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        WizardState state = states.get(player.getUniqueId());
        if (!running || state == null || !state.alive) return;
        ItemStack item = event.getItem().getItemStack();
        if (isSpellBook(item) || isLootWand(item) || isSoulStar(item)) {
            event.setCancelled(true);
            consumeLootItem(player, state, item);
            event.getItem().remove();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.0F);
        }
    }

    private void consumeLootItem(Player player, WizardState state, ItemStack item) {
        WizardsSpell spell = spellFromItem(item);
        if (spell != null) {
            int current = state.known.getOrDefault(spell, 0);
            if (current < spell.maximumLevel()) {
                int next = WizardsRules.nextSpellLevel(current, spell.maximumLevel());
                state.known.put(spell, next);
                player.sendMessage(categoryColour(spell) + spell.displayName() + ChatColor.DARK_GRAY
                        + " » " + ChatColor.GRAY + "Levelled up to " + next + ".");
            } else {
                addMana(state, 50.0F);
                player.sendMessage(ChatColor.GOLD + "Wizards » " + ChatColor.GRAY + "Spellbook converted into 50 mana.");
            }
            refreshManaBar(player, state);
            return;
        }

        if (isLootWand(item)) {
            if (WizardsRules.canGainWand(state.wandCount)) {
                int slot = state.wandCount++;
                player.getInventory().setItem(slot, makeWand(state, slot));
                player.sendMessage(ChatColor.GOLD + "Wizards » " + ChatColor.GRAY + "Extra wand gained.");
            } else {
                addMana(state, WizardsRules.FULL_WAND_CONVERSION);
                player.sendMessage(ChatColor.GOLD + "Wizards » " + ChatColor.GRAY + "Extra wand converted into 100 mana.");
            }
            refreshManaBar(player, state);
            return;
        }

        if (isSoulStar(item)) {
            state.soulStars++;
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Wizard Soul absorbed. Mana regeneration increased.");
            refreshManaBar(player, state);
        }
    }

    private void addMana(WizardState state, float amount) {
        state.mana = WizardsRules.addMana(state.mana, amount, state.maximumMana);
    }

    @EventHandler
    public void onHeldSlotChange(PlayerItemHeldEvent event) {
        WizardState state = states.get(event.getPlayer().getUniqueId());
        if (state == null || !state.alive) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) updateWandLabels(event.getPlayer(), state);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Wolf wolf && summonedWolves.containsKey(wolf.getUniqueId())) {
            SummonedWolfData data = summonedWolves.get(wolf.getUniqueId());
            Player owner = data == null ? null : Bukkit.getPlayer(data.owner);
            WizardState ownerState = owner == null ? null : states.get(owner.getUniqueId());
            if (owner == null || !owner.isOnline() || ownerState == null || !ownerState.alive) {
                event.setCancelled(true);
                wolf.remove();
                summonedWolves.remove(wolf.getUniqueId());
            } else if (event.getEntity() instanceof Player victim
                    && states.containsKey(victim.getUniqueId()) && states.get(victim.getUniqueId()).alive) {
                event.setDamage(event.getDamage() * 0.3D);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getDamager() instanceof Projectile projectile) {
            String projectileSpell = projectile.getPersistentDataContainer().get(projectileKey, PersistentDataType.STRING);
            if (projectileSpell != null && event.getEntity() instanceof Player victim) {
                String ownerId = projectile.getPersistentDataContainer().get(projectileOwnerKey, PersistentDataType.STRING);
                if (ownerId != null && ownerId.equals(victim.getUniqueId().toString())) {
                    event.setCancelled(true);
                    return;
                }
                if ("web-shot".equals(projectileSpell) || "ice-prison".equals(projectileSpell)) {
                    event.setCancelled(true);
                    return;
                }
                if ("spectral-arrow".equals(projectileSpell)) {
                    SpectralArrowData data = spectralArrows.remove(projectile.getUniqueId());
                    if (data != null) {
                        double distance = data.origin.distance(victim.getLocation());
                        event.setDamage(WizardsRules.spectralArrowDamage(data.level, distance));
                    }
                } else {
                    Double damage = projectile.getPersistentDataContainer().get(projectileDamageKey, PersistentDataType.DOUBLE);
                    if (damage != null) event.setDamage(damage);
                }
                projectile.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.08);
            }
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) return;
        WizardState state = states.get(attacker.getUniqueId());
        if (!running || state == null || !state.alive) return;
        int slot = attacker.getInventory().getHeldItemSlot();
        if (slot >= 0 && slot < state.wandCount && isWand(attacker.getInventory().getItem(slot))) {
            WizardsSpell spell = state.wands[slot];
            if (spell != null) castSpell(attacker, state, spell, event.getEntity(), null);
            event.setDamage(Math.min(event.getDamage(), 0.5D));
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile instanceof Fireball meteor
                && meteor.getPersistentDataContainer().has(meteorKey, PersistentDataType.DOUBLE)) {
            handleEndgameMeteorImpact(meteor);
            return;
        }
        String spell = projectile.getPersistentDataContainer().get(projectileKey, PersistentDataType.STRING);
        if (spell == null) return;
        Location location = projectile.getLocation();
        if ("web-shot".equals(spell)) {
            Location target = event.getHitEntity() == null
                    ? (event.getHitBlock() == null ? location : event.getHitBlock().getLocation().add(0.5D, 1.0D, 0.5D))
                    : event.getHitEntity().getLocation();
            placeWebAt(target);
            location.getWorld().spawnParticle(Particle.CLOUD, location, 10, 0.25D, 0.25D, 0.25D, 0.03D);
            location.getWorld().playSound(location, Sound.BLOCK_COBWEB_PLACE, 0.6F, 1.1F);
        } else if ("ice-prison".equals(spell)) {
            int strength = projectile.getPersistentDataContainer().getOrDefault(
                    anvilLevelKey, PersistentDataType.INTEGER, 4);
            createIcePrison(location, strength);
            ownedEntities.remove(projectile.getUniqueId());
            projectile.remove();
        } else if ("ice-shards".equals(spell)) {
            location.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 18, 0.3D, 0.3D, 0.3D, 0.03D);
            location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 0.7F, 1.4F);
            extinguishNearbyFire(location);
        } else if ("spectral-arrow".equals(spell)) {
            spectralArrows.remove(projectile.getUniqueId());
            location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 14, 0.25D, 0.25D, 0.25D, 0.04D);
        } else if ("mana-bolt".equals(spell)) {
            manaBolts.remove(projectile.getUniqueId());
            location.getWorld().spawnParticle(Particle.END_ROD, location, 24, 0.45D, 0.45D, 0.45D, 0.12D);
            location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 0.7F);
        } else if ("fireball".equals(spell)) {
            String ownerId = projectile.getPersistentDataContainer().get(projectileOwnerKey, PersistentDataType.STRING);
            Player owner = ownerId == null ? null : Bukkit.getPlayer(UUID.fromString(ownerId));
            Double damage = projectile.getPersistentDataContainer().get(projectileDamageKey, PersistentDataType.DOUBLE);
            double explosionRadius = 4.0D + (damage == null ? 3.0D : Math.max(0.0D, (damage - 6.0D) * 0.3D));
            for (Player victim : alivePlayers()) {
                if (owner == null || victim.equals(owner) || !victim.getWorld().equals(location.getWorld())) continue;
                double distance = victim.getLocation().distance(location);
                if (distance <= explosionRadius) {
                    victim.damage(damage == null ? 6.0D : damage, owner);
                    victim.setFireTicks(Math.max(victim.getFireTicks(), 80));
                }
            }
            location.getWorld().spawnParticle(Particle.FLAME, location, 32, 0.6D, 0.6D, 0.6D, 0.04D);
            location.getWorld().spawnParticle(Particle.LAVA, location, 8, 0.35D, 0.35D, 0.35D, 0.0D);
            location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.1F);
        } else if ("anvil-drop".equals(spell)) {
            handleAnvilImpact(projectile);
        } else {
            location.getWorld().spawnParticle(Particle.END_ROD, location, 8, 0.2D, 0.2D, 0.2D, 0.02D);
            location.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5F, 1.8F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnvilLand(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock)) return;
        String spell = entity.getPersistentDataContainer().get(projectileKey, PersistentDataType.STRING);
        if (!"anvil-drop".equals(spell)) return;
        event.setCancelled(true);
        handleAnvilImpact(entity);
    }

    private void handleAnvilImpact(Entity entity) {
        if (entity == null || !entity.isValid()) return;
        int level = entity.getPersistentDataContainer().getOrDefault(anvilLevelKey, PersistentDataType.INTEGER, 1);
        String ownerId = entity.getPersistentDataContainer().get(projectileOwnerKey, PersistentDataType.STRING);
        Player owner = ownerId == null ? null : Bukkit.getPlayer(UUID.fromString(ownerId));
        Location impact = entity.getLocation();
        if (owner != null && owner.isOnline()) {
            double radius = 3.0D + level / 2.0D;
            for (Player victim : alivePlayers()) {
                if (!victim.getWorld().equals(impact.getWorld())) continue;
                double distance = victim.getLocation().distance(impact);
                if (distance <= radius) {
                    victim.damage(3.0D + level * 2.0D, owner);
                }
            }
        }
        impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 2, 0.35D, 0.25D, 0.35D, 0.02D);
        impact.getWorld().spawnParticle(Particle.CLOUD, impact, 18, 0.45D, 0.25D, 0.45D, 0.06D);
        impact.getWorld().playSound(impact, Sound.BLOCK_ANVIL_LAND, 1.2F, 0.8F);
        ownedEntities.remove(entity.getUniqueId());
        entity.remove();
    }

    private void createIcePrison(Location centre, int strength) {
        if (centre == null || centre.getWorld() == null) return;
        int radius = Math.max(1, strength);
        long duration = (20L + random.nextInt(10)) * 20L;
        int placed = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if ((x * x) + (y * y) + (z * z) > radius * radius) continue;
                    Location block = centre.getBlock().getLocation().add(x, y, z);
                    if (setTemporaryBlock(block, Material.ICE, duration, false)) placed++;
                }
            }
        }
        if (placed > 0) {
            centre.getWorld().spawnParticle(Particle.SNOWFLAKE, centre, Math.min(80, 20 + placed),
                    radius / 2.0D, radius / 2.0D, radius / 2.0D, 0.04D);
            centre.getWorld().playSound(centre, Sound.ENTITY_SILVERFISH_HURT, 2.0F, 1.0F);
        }
    }

    private void tickFallingProtection() {
        long now = System.currentTimeMillis();
        var iterator = fallingProtection.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || now >= entry.getValue()) {
                iterator.remove();
            } else {
                player.setFallDistance(0.0F);
            }
        }
    }

    private void placeWebAt(Location target) {
        if (target == null || target.getWorld() == null) return;
        for (int y = 1; y >= -1; y--) {
            for (int radius = 0; radius <= 1; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location candidate = target.getBlock().getLocation().add(x, y, z);
                        if (candidate.getBlock().isPassable()
                                && candidate.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                            if (setTemporaryBlock(candidate, Material.COBWEB, 100L)) return;
                        }
                    }
                }
            }
        }
    }

    private void extinguishNearbyFire(Location location) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = location.getBlock().getRelative(x, y, z);
                    if (block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void tickSpectralArrows() {
        var iterator = spectralArrows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SpectralArrowData> entry = iterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Arrow arrow) || !arrow.isValid() || arrow.isOnGround()) {
                iterator.remove();
                continue;
            }
            SpectralArrowData data = entry.getValue();
            Location current = arrow.getLocation();
            double distance = Math.max(0.1D, current.distance(data.lastPosition));
            int count = Math.max(1, Math.min(12, (int) Math.ceil(distance / 0.35D)));
            for (int i = 0; i < count; i++) {
                Location particle = data.lastPosition.clone().add(current.clone().subtract(data.lastPosition).multiply((i + 1.0D) / count));
                current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            data.lastPosition = current.clone();
        }
    }

    private void tickSummonedWolves() {
        long now = System.currentTimeMillis();
        var iterator = summonedWolves.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SummonedWolfData> entry = iterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            Player owner = Bukkit.getPlayer(entry.getValue().owner);
            WizardState ownerState = owner == null ? null : states.get(owner.getUniqueId());
            if (!(entity instanceof Wolf wolf) || !wolf.isValid() || owner == null || !owner.isOnline()
                    || ownerState == null || !ownerState.alive || now >= entry.getValue().expiresAt) {
                if (entity != null && entity.isValid()) entity.remove();
                iterator.remove();
                continue;
            }

            Player closest = null;
            double closestDistance = 16.0D;
            for (Player candidate : alivePlayers()) {
                if (candidate.equals(owner) || !candidate.getWorld().equals(wolf.getWorld())) continue;
                double distance = candidate.getLocation().distance(wolf.getLocation());
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
            if (closest != null) {
                wolf.setTarget(closest);
            } else {
                wolf.setTarget(null);
                if (wolf.getLocation().distance(owner.getLocation()) > 16.0D) wolf.teleport(owner.getLocation());
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        WizardState state = states.get(player.getUniqueId());
        if (!running || state == null || !state.alive) return;
        state.alive = false;
        dropDeathLoot(player, state);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
        removeManaBar(player.getUniqueId());
        player.sendMessage(ChatColor.RED + "You have been eliminated from Wizards.");
        Bukkit.getScheduler().runTaskLater(plugin, this::checkWinner, 2L);
    }

    private void dropDeathLoot(Player player, WizardState state) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = MAX_WAND_SLOTS; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) drop(player, item.clone());
        }
        for (ItemStack armour : inventory.getArmorContents()) {
            if (armour != null && !armour.getType().isAir()) drop(player, armour.clone());
        }

        Set<WizardsSpell> droppedSpells = new HashSet<>();
        for (WizardsSpell spell : state.wands) {
            if (spell != null && spell != WizardsSpell.MANA_BOLT) droppedSpells.add(spell);
        }
        for (Map.Entry<WizardsSpell, Integer> entry : state.known.entrySet()) {
            if (entry.getKey() != WizardsSpell.MANA_BOLT && random.nextInt(5) == 0) droppedSpells.add(entry.getKey());
        }
        List<ItemStack> loot = new ArrayList<>();
        for (WizardsSpell spell : droppedSpells) loot.add(makeSpellBook(spell));
        if (state.wandCount > 3 || random.nextBoolean()) loot.add(makeWandLoot());
        loot.add(makeSoulStar());
        for (ItemStack item : loot) drop(player, item);
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
    }

    private void drop(Player player, ItemStack item) {
        org.bukkit.entity.Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), item);
        dropped.setPickupDelay(20);
        ownedEntities.add(dropped.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PlayerSnapshot restore = pendingRestore.get(id);
        if (restore != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = event.getPlayer();
                if (player.isOnline() && !player.isDead()) {
                    pendingRestore.remove(id);
                    restore.restore(player);
                }
            });
            return;
        }
        WizardState state = states.get(id);
        if (running && state != null && !state.alive && arena != null) {
            event.setRespawnLocation(new Location(arena, 0.5D, ARENA_FLOOR_Y + 2.0D, 0.5D));
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = event.getPlayer();
                if (!player.isOnline() || !running || state.alive) return;
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                player.setFlying(true);
                player.sendMessage(ChatColor.GRAY + "You are spectating the remaining wizards.");
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        queue.remove(id);
        WizardState state = states.get(id);
        if (running && state != null && state.alive) {
            state.alive = false;
            dropDeathLoot(event.getPlayer(), state);
            removeManaBar(id);
            Bukkit.getScheduler().runTaskLater(plugin, this::checkWinner, 2L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerSnapshot snapshot = pendingRestore.get(event.getPlayer().getUniqueId());
        if (snapshot == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (player.isOnline() && !player.isDead()) {
                pendingRestore.remove(player.getUniqueId());
                snapshot.restore(player);
            }
        }, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        WizardState state = states.get(event.getPlayer().getUniqueId());
        if (running && state != null && state.alive
                && event.getPlayer().getInventory().getHeldItemSlot() < MAX_WAND_SLOTS
                && isWand(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        temporaryBlocks.remove(event.getBlock().getLocation());
    }

    private void castSpell(Player player, WizardState state, WizardsSpell spell, Entity target, Block clickedBlock) {
        int level = state.known.getOrDefault(spell, 0);
        if (level <= 0) return;
        if (!spell.implemented()) {
            player.sendMessage(ChatColor.RED + spell.displayName() + " is not in the first 26.3 mechanics milestone yet.");
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownUntil = state.cooldowns.getOrDefault(spell, 0L);
        if (cooldownUntil > now) {
            player.sendMessage(ChatColor.BLUE + spell.displayName() + " has not recharged (" +
                    Math.max(1, (cooldownUntil - now + 999) / 1000) + "s).");
            return;
        }
        int manaCost = spell.manaCost(level);
        if (state.mana < manaCost) {
            player.sendMessage(ChatColor.BLUE + "You do not have enough mana for " + spell.displayName() + ".");
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7F, 0.7F);
            return;
        }
        if (!applySpell(player, state, spell, level, target, clickedBlock)) return;

        state.mana -= manaCost;
        int cooldown = spell.cooldownSeconds(level, state.cooldownModifier);
        state.cooldowns.put(spell, now + cooldown * 1000L);
        refreshManaBar(player, state);
        updateWandLabels(player, state);
        player.sendMessage(ChatColor.DARK_AQUA + spell.displayName() + ChatColor.GRAY + " cast • " +
                ChatColor.AQUA + manaCost + " mana" + ChatColor.GRAY + " • " + cooldown + "s cooldown");
    }

    private boolean applySpell(Player caster, WizardState state, WizardsSpell spell, int level,
                               Entity directTarget, Block clickedBlock) {
        Location origin = caster.getEyeLocation();
        Vector direction = origin.getDirection().normalize();
        switch (spell) {
            case IMPLODE: {
                Block target = caster.getTargetBlockExact(50);
                if (target == null) {
                    caster.sendMessage(ChatColor.RED + "Implode needs a block to target within 50 blocks.");
                    return false;
                }
                Location centre = target.getLocation().add(0.5D, 0.5D, 0.5D);
                int size = (int) (1.5F + level * 0.7F);
                int radius = size * 2;
                List<Block> affected = new ArrayList<>();
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Block block = target.getRelative(x, y, z);
                            if (block.getType().isAir() || block.getType() == Material.BEDROCK
                                    || block.getState() instanceof InventoryHolder || block.getState() instanceof TileState) continue;
                            Location blockCentre = block.getLocation().add(0.5D, 0.5D, 0.5D);
                            if (centre.distance(blockCentre) + Math.abs(y / 4.0D) <= radius + 0.5D) affected.add(block);
                        }
                    }
                }
                if (affected.isEmpty()) {
                    caster.sendMessage(ChatColor.GRAY + "There is nothing for Implode to gather at that location.");
                    return false;
                }
                java.util.Collections.shuffle(affected, random);
                for (int i = 0; i < affected.size(); i++) {
                    Block block = affected.get(i);
                    if (i % 3 == 0) {
                        block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5D, 0.5D, 0.5D),
                                5, 0.22D, 0.22D, 0.22D, 0.04D, block.getBlockData());
                    }
                }
                caster.getWorld().playSound(centre, Sound.BLOCK_BEACON_DEACTIVATE, 1.2F, 0.5F);
                for (Player victim : alivePlayers()) {
                    if (victim.equals(caster) || !victim.getWorld().equals(centre.getWorld())) continue;
                    double distance = victim.getLocation().distance(centre);
                    if (distance <= radius + 2.0D) {
                        Vector pull = centre.toVector().subtract(victim.getLocation().toVector());
                        pull.normalize().multiply(Math.max(0.25D, 1.15D - distance / (radius + 2.0D)));
                        pull.setY(Math.max(0.15D, pull.getY() + 0.25D));
                        victim.setVelocity(pull);
                    }
                }
                scheduleSpellLater(() -> {
                    if (!running) return;
                    if (plugin.getConfig().getBoolean("destructive-spells", false)) {
                        for (Block block : affected) {
                            if (!block.getType().isAir() && block.getType() != Material.BEDROCK
                                    && !(block.getState() instanceof InventoryHolder) && !(block.getState() instanceof TileState)) {
                                block.setType(Material.AIR, false);
                            }
                        }
                    }
                    centre.getWorld().spawnParticle(Particle.EXPLOSION, centre, 2 + level, 0.8D, 0.8D, 0.8D, 0.08D);
                    centre.getWorld().spawnParticle(Particle.CLOUD, centre, 40, radius / 3.0D, 1.0D, radius / 3.0D, 0.12D);
                    centre.getWorld().playSound(centre, Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 0.7F);
                    for (Player victim : alivePlayers()) {
                        if (victim.equals(caster) || !victim.getWorld().equals(centre.getWorld())) continue;
                        double distance = victim.getLocation().distance(centre);
                        if (distance <= radius + 2.0D) {
                            victim.damage(Math.max(1.0D, (2.0D + level * 2.0D) * (1.0D - distance / (radius + 3.0D))), caster);
                            Vector push = victim.getLocation().toVector().subtract(centre.toVector());
                            if (push.lengthSquared() > 0.001D) victim.setVelocity(push.normalize().multiply(0.65D).setY(0.35D));
                        }
                    }
                }, 20L);
                return true;
            }
            case NAPALM: {
                launchNapalm(caster, level);
                return true;
            }
            case RAINBOW_BEAM: {
                Player victim = directTarget instanceof Player direct && !direct.equals(caster)
                        ? direct : findTargetPlayer(caster, 80.0D);
                Block aimed = caster.getTargetBlockExact(80);
                Location end;
                if (victim != null) {
                    end = victim.getEyeLocation();
                    double distance = caster.getEyeLocation().distance(end);
                    double damage = (level * 2.0D) + 2.0D;
                    if (distance > 16.0D) damage = Math.max(1.0D, damage * (1.0D - (distance - 16.0D) / 64.0D));
                    victim.damage(damage, caster);
                    victim.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, victim.getLocation().add(0.0D, 1.0D, 0.0D),
                            18, 0.35D, 0.45D, 0.35D, 0.08D);
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
                } else if (aimed != null) {
                    end = aimed.getLocation().add(0.5D, 0.5D, 0.5D);
                } else {
                    caster.sendMessage(ChatColor.RED + "Rainbow Beam needs a target within 80 blocks.");
                    return false;
                }
                drawRainbowBeam(caster.getEyeLocation().clone().subtract(0.0D, 0.1D, 0.0D), end);
                caster.playSound(caster.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.0F);
                return true;
            }
            case RAINBOW_ROAD: {
                launchRainbowRoad(caster, level);
                return true;
            }
            case RUMBLE: {
                launchRumble(caster, level);
                return true;
            }
            case TRAP_RUNE: {
                return placeTrapRune(caster, level, clickedBlock);
            }
            case ANVIL_DROP: {
                int radius = 4 + level * 2;
                List<Player> targets = new ArrayList<>();
                targets.add(caster);
                for (Player candidate : alivePlayers()) {
                    if (candidate.equals(caster) || !candidate.getWorld().equals(caster.getWorld())) continue;
                    if (Math.abs(candidate.getLocation().getX() - caster.getLocation().getX()) <= radius
                            && Math.abs(candidate.getLocation().getZ() - caster.getLocation().getZ()) <= radius
                            && Math.abs(candidate.getLocation().getY() - caster.getLocation().getY()) <= radius * 3.0D) {
                        targets.add(candidate);
                    }
                }
                int spawned = 0;
                for (Player target : targets) {
                    Location spawn = target.getLocation().clone().add(0.0D, 15.0D + level * 3.0D, 0.0D);
                    spawn.setY(Math.min(spawn.getY(), spawn.getWorld().getMaxHeight() - 2.0D));
                    int lowered = 0;
                    while (lowered < 5 && !spawn.getBlock().isPassable()) {
                        spawn.subtract(0.0D, 1.0D, 0.0D);
                        lowered++;
                    }
                    if (!spawn.getBlock().isPassable()) continue;
                    FallingBlock anvil = spawn.getWorld().spawnFallingBlock(spawn, Material.ANVIL.createBlockData());
                    anvil.setDropItem(false);
                    anvil.setHurtEntities(false);
                    anvil.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "anvil-drop");
                    anvil.getPersistentDataContainer().set(anvilLevelKey, PersistentDataType.INTEGER, level);
                    anvil.getPersistentDataContainer().set(projectileOwnerKey, PersistentDataType.STRING, caster.getUniqueId().toString());
                    ownedEntities.add(anvil.getUniqueId());
                    spawned++;
                }
                if (spawned == 0) {
                    caster.sendMessage(ChatColor.RED + "Anvil Drop could not find clear space overhead.");
                    return false;
                }
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 0.8F);
                return true;
            }
            case ICE_SHARDS: {
                launchIceShard(caster, 2.0D);
                for (int i = 1; i <= level; i++) {
                    final Player shooter = caster;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        WizardState current = states.get(shooter.getUniqueId());
                        if (running && current != null && current.alive && shooter.isOnline()) launchIceShard(shooter, 2.0D);
                    }, i * 5L);
                }
                return true;
            }
            case SPECTRAL_ARROW: {
                Arrow arrow = caster.launchProjectile(Arrow.class);
                arrow.setVelocity(arrow.getVelocity().multiply(2.3D));
                arrow.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, spell.id());
                arrow.getPersistentDataContainer().set(projectileOwnerKey, PersistentDataType.STRING, caster.getUniqueId().toString());
                spectralArrows.put(arrow.getUniqueId(), new SpectralArrowData(caster.getUniqueId(), level,
                        caster.getLocation().clone(), arrow.getLocation().clone()));
                ownedEntities.add(arrow.getUniqueId());
                return true;
            }
            case SUMMON_WOLVES: {
                Location base = clickedBlock == null ? caster.getLocation().getBlock().getLocation()
                        : clickedBlock.getRelative(org.bukkit.block.BlockFace.UP).getLocation();
                if (!base.getBlock().isPassable()) base = caster.getLocation();
                int spawned = 0;
                for (int i = 0; i < WizardsRules.summonedWolfCount(level); i++) {
                    Location spawn = base.clone().add((random.nextDouble() - 0.5D) * 1.2D, 0.0D,
                            (random.nextDouble() - 0.5D) * 1.2D);
                    Wolf wolf = spawn.getWorld().spawn(spawn, Wolf.class);
                    wolf.setTamed(true);
                    wolf.setOwner(caster);
                    wolf.setCollarColor(DyeColor.YELLOW);
                    wolf.setCustomName(caster.getName() + "'s Wizard Wolf");
                    wolf.setCustomNameVisible(false);
                    if (wolf.getAttribute(Attribute.MAX_HEALTH) != null) {
                        wolf.getAttribute(Attribute.MAX_HEALTH).setBaseValue(0.5D);
                    }
                    wolf.setHealth(0.5D);
                    summonedWolves.put(wolf.getUniqueId(), new SummonedWolfData(caster.getUniqueId(), System.currentTimeMillis() + 30_000L));
                    ownedEntities.add(wolf.getUniqueId());
                    spawned++;
                }
                caster.getWorld().spawnParticle(Particle.POOF, base.clone().add(0.0D, 0.8D, 0.0D), 18, 0.6D, 0.25D, 0.6D, 0.05D);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.8F, 1.4F);
                return spawned > 0;
            }
            case WEB_SHOT: {
                launchWebShot(caster);
                for (int i = 1; i < WizardsRules.webShotCount(level); i++) {
                    final Player shooter = caster;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        WizardState current = states.get(shooter.getUniqueId());
                        if (running && current != null && current.alive && shooter.isOnline()) launchWebShot(shooter);
                    }, i * 10L);
                }
                return true;
            }
            case MANA_BOLT:
            case FIREBALL: {
                Projectile projectile;
                if (spell == WizardsSpell.FIREBALL) {
                    Fireball fireball = caster.launchProjectile(Fireball.class);
                    fireball.setIsIncendiary(false);
                    fireball.setYield(0.0F);
                    projectile = fireball;
                } else {
                    projectile = caster.launchProjectile(Snowball.class);
                }
                projectile.setVelocity(direction.multiply(spell == WizardsSpell.MANA_BOLT
                        ? WizardsRules.manaBoltVelocity() : WizardsRules.fireballVelocity()));
                projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, spell.id());
                projectile.getPersistentDataContainer().set(projectileOwnerKey, PersistentDataType.STRING, caster.getUniqueId().toString());
                projectile.getPersistentDataContainer().set(anvilLevelKey, PersistentDataType.INTEGER, level);
                double damage = spell == WizardsSpell.MANA_BOLT ? 4.0D + level * 2.0D : level + 6.0D;
                projectile.getPersistentDataContainer().set(projectileDamageKey, PersistentDataType.DOUBLE, damage);
                if (spell == WizardsSpell.MANA_BOLT) {
                    manaBolts.put(projectile.getUniqueId(), new ManaBoltData(caster.getUniqueId(), level, caster.getLocation().clone()));
                }
                ownedEntities.add(projectile.getUniqueId());
                caster.getWorld().spawnParticle(spell == WizardsSpell.MANA_BOLT ? Particle.END_ROD : Particle.FLAME,
                        origin.add(direction.clone().multiply(1.2D)), 5, 0.08, 0.08, 0.08, 0.01);
                return true;
            }
            case FLASH: {
                Block target = caster.getTargetBlockExact(20 + (level * 10));
                if (target == null) {
                    caster.sendMessage(ChatColor.RED + "Flash needs a solid block to target.");
                    return false;
                }
                Location destination = findSafeStandingLocation(target.getLocation());
                if (destination == null) {
                    caster.sendMessage(ChatColor.RED + "There is no safe landing space at that target.");
                    return false;
                }
                destination.setYaw(caster.getLocation().getYaw());
                destination.setPitch(caster.getLocation().getPitch());
                caster.teleport(destination);
                caster.getWorld().spawnParticle(Particle.PORTAL, destination, 24, 0.4, 0.5, 0.4, 0.4);
                caster.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7F, 1.1F);
                return true;
            }
            case FROST_BARRIER: {
                Vector forward = caster.getLocation().getDirection().setY(0.0D);
                if (forward.lengthSquared() < 0.001D) forward = new Vector(0, 0, 1);
                forward.normalize();
                Vector side = new Vector(-forward.getZ(), 0, forward.getX());
                Location centre = caster.getLocation().add(forward.multiply(3.0D));
                int halfWidth = 2 + level;
                int height = 1 + level;
                int placed = 0;
                for (int width = -halfWidth; width <= halfWidth; width++) {
                    for (int y = 0; y < height; y++) {
                        Location location = centre.clone().add(side.clone().multiply(width)).add(0, y, 0);
                        if (setTemporaryBlock(location, Material.ICE, 100L)) placed++;
                    }
                }
                if (placed == 0) {
                    caster.sendMessage(ChatColor.RED + "Frost Barrier has no room to form.");
                    return false;
                }
                caster.getWorld().playSound(centre, Sound.BLOCK_GLASS_PLACE, 0.8F, 0.8F);
                return true;
            }
            case GUST: {
                Vector facing = caster.getLocation().getDirection().setY(0.0D);
                if (facing.lengthSquared() < 0.001D) facing.setZ(1.0D);
                facing.normalize();
                Vector gustVelocity = facing.clone().multiply(1.5D).setY(0.3D)
                        .multiply(1.2D + level * 0.4D);
                double gustSize = level * 3.0D + 10.0D;
                List<Map.Entry<Player, Double>> affected = new ArrayList<>();
                for (Player target : alivePlayers()) {
                    if (target.equals(caster) || !target.getWorld().equals(caster.getWorld())
                            || !target.canSee(caster)) continue;
                    Vector offset = target.getLocation().toVector().subtract(caster.getLocation().toVector());
                    double forward = offset.dot(facing);
                    if (forward <= 0.0D || forward > 45.0D) continue;
                    Vector lateral = offset.clone().subtract(facing.clone().multiply(forward));
                    double widthAtDistance = gustSize * (forward / 45.0D);
                    if (lateral.length() > widthAtDistance) continue;
                    double strength = Math.max(0.2D, 1.0D - forward / 45.0D);
                    affected.add(Map.entry(target, strength));
                }
                if (affected.isEmpty()) {
                    caster.sendMessage(ChatColor.GRAY + "Gust needs an enemy in the forward cone.");
                    return false;
                }
                for (Map.Entry<Player, Double> entry : affected) {
                    Player target = entry.getKey();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        WizardState current = states.get(target.getUniqueId());
                        if (!running || current == null || !current.alive || !target.isOnline()) return;
                        target.setVelocity(gustVelocity.clone().multiply(Math.max(0.2D, entry.getValue())));
                        fallingProtection.put(target.getUniqueId(), System.currentTimeMillis() + 2_000L);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0F, 0.7F);
                    });
                }
                caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation().add(0, 1, 0), 35,
                        gustSize / 5.0D, 0.5D, gustSize / 5.0D, 0.12D);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0F, 0.7F);
                return true;
            }
            case HEAL: {
                double maximum = caster.getAttribute(Attribute.MAX_HEALTH) == null
                        ? 20.0D : caster.getAttribute(Attribute.MAX_HEALTH).getValue();
                double heal = 3.0D + level;
                if (caster.getHealth() >= maximum) {
                    caster.sendMessage(ChatColor.YELLOW + "You are already at full health.");
                    return false;
                }
                caster.setHealth(Math.min(maximum, caster.getHealth() + heal));
                caster.getWorld().spawnParticle(Particle.HEART, caster.getLocation().add(0, 1, 0), 12, 0.5, 0.6, 0.5, 0.05);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.6F);
                return true;
            }
            case ICE_PRISON: {
                Snowball projectile = caster.launchProjectile(Snowball.class);
                projectile.setVelocity(direction.clone().multiply(1.7D));
                projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "ice-prison");
                projectile.getPersistentDataContainer().set(projectileOwnerKey, PersistentDataType.STRING,
                        caster.getUniqueId().toString());
                projectile.getPersistentDataContainer().set(anvilLevelKey, PersistentDataType.INTEGER, 3 + level);
                ownedEntities.add(projectile.getUniqueId());
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 0.7F, 0.8F);
                return true;
            }
            case LIGHTNING_STRIKE: {
                Player victim = directTarget instanceof Player direct && !direct.equals(caster)
                        ? direct : findTargetPlayer(caster, 40.0D);
                Block aimed = clickedBlock != null ? clickedBlock : caster.getTargetBlockExact(40);
                Location strike = victim != null ? victim.getLocation() : (aimed == null ? null : aimed.getLocation());
                if (strike == null) {
                    caster.sendMessage(ChatColor.RED + "Lightning Strike needs a target.");
                    return false;
                }
                caster.getWorld().strikeLightningEffect(strike);
                for (Entity entity : caster.getWorld().getNearbyEntities(strike, 2.8D, 3.0D, 2.8D)) {
                    if (entity instanceof Player target && !target.equals(caster)) {
                        target.damage(2.0D + level * 4.0D, caster);
                    }
                }
                caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strike, 20, 0.6, 1.0, 0.6, 0.1);
                return true;
            }
            case SPEED_BOOST: {
                caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20,
                        level, true, true, true));
                caster.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, caster.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, 0.05);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.8F, 1.4F);
                return true;
            }
            case WIZARDS_COMPASS: {
                Player target = findTargetPlayer(caster, 100.0D);
                if (target == null) {
                    caster.sendMessage(ChatColor.GRAY + "No other living wizard could be found.");
                    return false;
                }
                caster.setCompassTarget(target.getLocation());
                caster.sendMessage(ChatColor.AQUA + "Nearest wizard: " + ChatColor.WHITE + target.getName()
                        + ChatColor.GRAY + " (" + Math.round(caster.getLocation().distance(target.getLocation())) + " blocks)");
                target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 8, 0.35, 0.5, 0.35, 0.1, caster);
                caster.playSound(caster.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.4F);
                return true;
            }
            default:
                return false;
        }
    }

    private void scheduleSpellLater(Runnable action, long delayTicks) {
        spellTasks.add(Bukkit.getScheduler().runTaskLater(plugin, action, delayTicks));
    }

    private void scheduleSpellTask(BukkitRunnable runnable, long delayTicks, long periodTicks) {
        spellTasks.add(runnable.runTaskTimer(plugin, delayTicks, periodTicks));
    }

    private void launchNapalm(Player caster, int level) {
        if (states.get(caster.getUniqueId()) == null) return;
        final Location napalm = caster.getEyeLocation().add(0.0D, 0.5D, 0.0D)
                .add(caster.getEyeLocation().getDirection().normalize().multiply(2.0D));
        final Vector step = caster.getEyeLocation().getDirection().normalize().multiply(0.15D);
        final int maximumLength = 5 + 10 * level;
        scheduleSpellTask(new BukkitRunnable() {
            private int ticks;
            private double travelled;
            private double size = 1.0D;

            @Override public void run() {
                WizardState state = states.get(caster.getUniqueId());
                if (!running || !caster.isOnline() || state == null || !state.alive || travelled >= maximumLength) {
                    cancel();
                    return;
                }
                napalm.add(step);
                travelled += 0.15D;
                ticks++;
                size = Math.min(8.0D, size + 0.06D);
                if (!napalm.getBlock().isPassable()) {
                    cancel();
                    return;
                }

                napalm.getWorld().spawnParticle(Particle.FLAME, napalm, Math.min(35, 8 + (int) (size * 3)),
                        size / 5.0D, size / 5.0D, size / 5.0D, 0.025D);
                napalm.getWorld().spawnParticle(Particle.SMOKE, napalm, 4 + (int) size,
                        size / 7.0D, size / 7.0D, size / 7.0D, 0.01D);

                double heatRadius = size * 1.1D;
                for (Player victim : alivePlayers()) {
                    if (victim.equals(caster) || !victim.getWorld().equals(napalm.getWorld())) continue;
                    double heat = heatRadius - victim.getLocation().distance(napalm);
                    if (heat <= 0.0D) continue;
                    victim.setFireTicks(Math.max(victim.getFireTicks(), (int) (heat * 40.0D)));
                    if (ticks % 10 == 0) victim.damage(Math.max(0.5D, heat / 1.5D), caster);
                }

                // Legacy Napalm can destroy blocks; modern test arenas instead get reversible flame.
                if (ticks % 3 == 0 && plugin.getConfig().getBoolean("temporary-spell-fire", true)) {
                    int radius = Math.max(1, (int) Math.ceil(size * 0.75D));
                    for (int attempts = 0; attempts < 3; attempts++) {
                        int x = random.nextInt(radius * 2 + 1) - radius;
                        int y = random.nextInt(radius * 2 + 1) - radius;
                        int z = random.nextInt(radius * 2 + 1) - radius;
                        Block fire = napalm.clone().add(x, y, z).getBlock();
                        if (fire.getType().isAir() && fire.getRelative(org.bukkit.block.BlockFace.DOWN).getType().isSolid()) {
                            setTemporaryBlock(fire.getLocation(), Material.FIRE, 60L);
                        }
                    }
                }
                if (ticks % 8 == 0) napalm.getWorld().playSound(napalm, Sound.ENTITY_BLAZE_SHOOT,
                        0.65F, 0.7F + (float) size / 12.0F);
            }
        }, 0L, 1L);
    }

    private void drawRainbowBeam(Location start, Location end) {
        Vector delta = end.toVector().subtract(start.toVector());
        double distance = delta.length();
        int points = Math.max(1, (int) Math.ceil(distance / 0.3D));
        Vector step = delta.multiply(1.0D / points);
        for (int i = 0; i <= points; i++) {
            Location point = start.clone().add(step.clone().multiply(i));
            point.getWorld().spawnParticle(i % 2 == 0 ? Particle.END_ROD : Particle.HAPPY_VILLAGER,
                    point, 1, 0.015D, 0.015D, 0.015D, 0.0D);
        }
    }

    private void launchRainbowRoad(Player caster, int level) {
        final Vector look = caster.getLocation().getDirection();
        final Vector horizontal = look.clone().setY(0.0D);
        if (horizontal.lengthSquared() < 0.001D) horizontal.setZ(1.0D);
        horizontal.normalize();
        final Vector side = new Vector(-horizontal.getZ(), 0.0D, horizontal.getX());
        final double verticalStep = Math.max(-1.0D, Math.min(1.0D, -caster.getLocation().getPitch() / 30.0D));
        final Location cursor = caster.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getLocation()
                .add(0.5D, 0.0D, 0.5D);
        final Material[] rainbow = {
                Material.RED_STAINED_GLASS, Material.ORANGE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS,
                Material.LIME_STAINED_GLASS, Material.GREEN_STAINED_GLASS, Material.CYAN_STAINED_GLASS,
                Material.LIGHT_BLUE_STAINED_GLASS, Material.BLUE_STAINED_GLASS, Material.PURPLE_STAINED_GLASS,
                Material.MAGENTA_STAINED_GLASS, Material.PINK_STAINED_GLASS
        };
        final int maximumSteps = 3 + 10 * level;
        scheduleSpellTask(new BukkitRunnable() {
            private int steps;
            private int colourIndex;

            @Override public void run() {
                WizardState state = states.get(caster.getUniqueId());
                if (!running || !caster.isOnline() || state == null || !state.alive || steps++ >= maximumSteps) {
                    cancel();
                    return;
                }
                for (int width = -1; width <= 1; width++) {
                    Location tile = cursor.clone().add(side.clone().multiply(width));
                    Material colour = rainbow[colourIndex++ % rainbow.length];
                    if (setTemporaryBlock(tile, colour, 280L + random.nextInt(120), true)) {
                        tile.getWorld().spawnParticle(Particle.END_ROD, tile.clone().add(0.5D, 1.0D, 0.5D),
                                3, 0.18D, 0.08D, 0.18D, 0.015D);
                        tile.getWorld().playSound(tile, Sound.BLOCK_GLASS_PLACE, 0.25F,
                                1.4F + (colourIndex % 5) * 0.1F);
                    }
                }
                cursor.add(horizontal).add(0.0D, verticalStep, 0.0D);
            }
        }, 5L, 5L);
    }

    private void launchRumble(Player caster, int level) {
        final Vector direction = caster.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() < 0.001D) direction.setZ(1.0D);
        direction.normalize();
        Block start = caster.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
        if (!start.getType().isSolid()) start = start.getRelative(org.bukkit.block.BlockFace.DOWN);
        final Block initial = start;
        scheduleSpellTask(new BukkitRunnable() {
            private Block current = initial;
            private int travelled;
            private final Set<UUID> hit = new HashSet<>();

            @Override public void run() {
                WizardState state = states.get(caster.getUniqueId());
                if (!running || !caster.isOnline() || state == null || !state.alive || travelled >= 10 * level) {
                    finishRumble(caster, current.getLocation().add(0.5D, 0.5D, 0.5D), level);
                    cancel();
                    return;
                }
                Block next = findRumbleGround(current, direction);
                if (next == null) {
                    finishRumble(caster, current.getLocation().add(0.5D, 0.5D, 0.5D), level);
                    cancel();
                    return;
                }
                current = next;
                travelled++;
                Location wave = current.getLocation().add(0.5D, 0.75D, 0.5D);
                wave.getWorld().spawnParticle(Particle.BLOCK, wave, 10, 0.35D, 0.12D, 0.35D, 0.04D, current.getBlockData());
                wave.getWorld().spawnParticle(Particle.CLOUD, wave, 8, 0.35D, 0.08D, 0.35D, 0.025D);
                wave.getWorld().playSound(wave, Sound.BLOCK_STONE_BREAK, 0.35F, 0.6F + random.nextFloat() * 0.3F);

                for (Player victim : alivePlayers()) {
                    if (victim.equals(caster) || hit.contains(victim.getUniqueId())
                            || !victim.getWorld().equals(wave.getWorld())) continue;
                    Location feet = victim.getLocation();
                    if (feet.getBlockX() == current.getX() && feet.getBlockZ() == current.getZ()
                            && feet.getY() >= current.getY() && feet.getY() <= current.getY() + 3.0D) {
                        hit.add(victim.getUniqueId());
                        victim.damage(4.0D + level * 2.0D, caster);
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60,
                                Math.max(0, level - 1), true, true, true));
                        victim.setVelocity(direction.clone().multiply(0.45D).setY(0.28D));
                    }
                }
            }
        }, 5L, 1L);
    }

    private Block findRumbleGround(Block current, Vector direction) {
        int x = (int) Math.floor(current.getX() + direction.getX() + 0.5D);
        int z = (int) Math.floor(current.getZ() + direction.getZ() + 0.5D);
        for (int y : new int[]{0, 1, -1, -2}) {
            Block candidate = current.getWorld().getBlockAt(x, current.getY() + y, z);
            if (candidate.getY() <= 0) continue;
            if (candidate.getType().isSolid() && candidate.getRelative(org.bukkit.block.BlockFace.UP).isPassable()) return candidate;
        }
        return null;
    }

    private void finishRumble(Player caster, Location centre, int level) {
        if (centre.getWorld() == null) return;
        double radius = 2.0D + level * 0.5D;
        centre.getWorld().spawnParticle(Particle.EXPLOSION, centre, 1, 0.3D, 0.2D, 0.3D, 0.0D);
        centre.getWorld().spawnParticle(Particle.CLOUD, centre, 28, radius / 2.0D, 0.25D, radius / 2.0D, 0.08D);
        centre.getWorld().playSound(centre, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 0.65F);
        for (Player victim : alivePlayers()) {
            if (victim.equals(caster) || !victim.getWorld().equals(centre.getWorld())) continue;
            double distance = victim.getLocation().distance(centre);
            if (distance >= 2.0D) continue;
            double damage = (1.0D + level / 5.0D) * (2.0D - distance);
            victim.damage(damage, caster);
            Vector push = victim.getLocation().toVector().subtract(centre.toVector());
            if (push.lengthSquared() > 0.001D) victim.setVelocity(push.normalize().multiply(0.8D).setY(0.38D));
        }
    }

    private boolean placeTrapRune(Player caster, int level, Block clickedBlock) {
        Block target = clickedBlock != null ? clickedBlock : caster.getTargetBlockExact(WizardsRules.trapRuneRange(level));
        if (target == null || !target.getType().isSolid()) {
            caster.sendMessage(ChatColor.RED + "Trap Rune needs a solid floor within " + (level * 4 + 4) + " blocks.");
            return false;
        }
        Location centre = target.getLocation().add(0.5D, 1.0D, 0.5D);
        if (!centre.getBlock().isPassable()) {
            caster.sendMessage(ChatColor.RED + "Trap Rune cannot be drawn on that surface.");
            return false;
        }
        double size = WizardsRules.trapRuneSize(level);
        trapRunes.add(new TrapRuneData(caster.getUniqueId(), centre, level, size, System.currentTimeMillis()));
        caster.getWorld().playSound(centre, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 0.55F);
        caster.sendMessage(ChatColor.LIGHT_PURPLE + "Trap Rune drawn. It arms after five seconds.");
        drawTrapRune(centre, size, false);
        return true;
    }

    private void tickTrapRunes() {
        long now = System.currentTimeMillis();
        var iterator = trapRunes.iterator();
        while (iterator.hasNext()) {
            TrapRuneData rune = iterator.next();
            Player owner = Bukkit.getPlayer(rune.owner);
            WizardState ownerState = owner == null ? null : states.get(rune.owner);
            if (owner == null || !owner.isOnline() || ownerState == null || !ownerState.alive
                    || now - rune.createdAt >= 100_000L || rune.centre.getWorld() == null
                    || !rune.centre.getBlock().isPassable()
                    || !rune.centre.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType().isSolid()) {
                iterator.remove();
                continue;
            }
            long ageTicks = (now - rune.createdAt) / 50L;
            boolean armed = ageTicks >= 100L;
            if (tick % 5 == 0) drawTrapRune(rune.centre, rune.size, armed);
            if (!armed) continue;

            Player trigger = null;
            for (Player victim : alivePlayers()) {
                if (victim.equals(owner) || !victim.getWorld().equals(rune.centre.getWorld())) continue;
                Location loc = victim.getLocation();
                if (Math.abs(loc.getX() - rune.centre.getX()) <= rune.size
                        && Math.abs(loc.getZ() - rune.centre.getZ()) <= rune.size
                        && loc.getY() >= rune.centre.getY() - 0.1D && loc.getY() <= rune.centre.getY() + 0.9D) {
                    trigger = victim;
                    break;
                }
            }
            if (trigger != null) {
                detonateTrapRune(rune, owner);
                iterator.remove();
            }
        }
    }

    private void drawTrapRune(Location centre, double size, boolean armed) {
        Particle particle = armed ? Particle.FLAME : Particle.WITCH;
        for (int i = 0; i < 4; i++) {
            double t = i / 3.0D;
            Location[] points = {
                    centre.clone().add(-size + size * 2.0D * t, 0.04D, -size),
                    centre.clone().add(size, 0.04D, -size + size * 2.0D * t),
                    centre.clone().add(size - size * 2.0D * t, 0.04D, size),
                    centre.clone().add(-size, 0.04D, size - size * 2.0D * t)
            };
            for (Location point : points) point.getWorld().spawnParticle(particle, point, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
        if (armed) centre.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centre, 3,
                size / 2.0D, 0.02D, size / 2.0D, 0.0D);
    }

    private void detonateTrapRune(TrapRuneData rune, Player owner) {
        Location centre = rune.centre;
        double radius = rune.size * 1.2D;
        double maxDamage = WizardsRules.trapRuneDamage(rune.level);
        centre.getWorld().spawnParticle(Particle.EXPLOSION, centre, 2, 0.3D, 0.15D, 0.3D, 0.0D);
        centre.getWorld().spawnParticle(Particle.SMOKE, centre, 36, radius / 1.5D, 0.25D, radius / 1.5D, 0.08D);
        centre.getWorld().playSound(centre, Sound.ENTITY_WITHER_SHOOT, 1.0F, 0.9F);
        for (Player victim : alivePlayers()) {
            if (!victim.getWorld().equals(centre.getWorld())) continue;
            double distance = victim.getLocation().distance(centre);
            if (distance > radius + 1.0D) continue;
            double damage = Math.max(1.0D, maxDamage * Math.max(0.15D, 1.0D - distance / (radius + 1.0D)));
            victim.damage(damage, owner);
            Vector push = victim.getLocation().toVector().subtract(centre.toVector());
            if (push.lengthSquared() > 0.001D) victim.setVelocity(push.normalize().multiply(1.0D).setY(0.45D));
        }
    }

    private void tickManaBolts() {
        var iterator = manaBolts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ManaBoltData> entry = iterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            ManaBoltData data = entry.getValue();
            Player owner = Bukkit.getPlayer(data.owner);
            WizardState ownerState = owner == null ? null : states.get(data.owner);
            if (!(entity instanceof Snowball bolt) || !bolt.isValid() || owner == null || !owner.isOnline()
                    || ownerState == null || !ownerState.alive
                    || bolt.getLocation().distance(data.origin) >= 20.0D + data.level * 10.0D) {
                if (entity != null && entity.isValid()) entity.remove();
                ownedEntities.remove(entry.getKey());
                iterator.remove();
                continue;
            }
            Player nearest = null;
            double nearestDistance = 20.0D + data.level * 10.0D + 10.0D;
            Vector currentDirection = bolt.getVelocity().clone().normalize();
            for (Player candidate : alivePlayers()) {
                if (candidate.equals(owner) || !candidate.getWorld().equals(bolt.getWorld())) continue;
                Vector offset = candidate.getLocation().clone().add(0.0D, 1.0D, 0.0D).toVector()
                        .subtract(bolt.getLocation().toVector());
                double distance = offset.length();
                if (distance >= nearestDistance || distance < 0.1D
                        || currentDirection.dot(offset.clone().normalize()) < 0.0D) continue;
                nearest = candidate;
                nearestDistance = distance;
            }
            if (nearest != null) {
                Vector steer = nearest.getLocation().clone().add(0.0D, 1.0D, 0.0D).toVector()
                        .subtract(bolt.getLocation().toVector()).normalize();
                bolt.setVelocity(currentDirection.add(steer.multiply(0.01D)).normalize().multiply(WizardsRules.manaBoltVelocity()));
            }
            bolt.getWorld().spawnParticle(Particle.END_ROD, bolt.getLocation(), 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private void expireTemporaryBlocks() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Location, TemporaryBlock> entry : new ArrayList<>(temporaryBlocks.entrySet())) {
            if (now >= entry.getValue().expiresAtMillis) restoreTemporaryBlock(entry.getKey());
        }
    }

    private void launchIceShard(Player caster, double damage) {
        Snowball shard = caster.launchProjectile(Snowball.class);
        shard.setVelocity(caster.getEyeLocation().getDirection().normalize().multiply(2.0D));
        shard.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "ice-shards");
        shard.getPersistentDataContainer().set(projectileDamageKey, PersistentDataType.DOUBLE, damage);
        ownedEntities.add(shard.getUniqueId());
    }

    private void launchWebShot(Player caster) {
        Snowball web = caster.launchProjectile(Snowball.class);
        web.setVelocity(caster.getEyeLocation().getDirection().normalize().multiply(1.5D));
        web.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "web-shot");
        ownedEntities.add(web.getUniqueId());
    }

    private Location findSafeStandingLocation(Location blockLocation) {
        World world = blockLocation.getWorld();
        int x = blockLocation.getBlockX();
        int z = blockLocation.getBlockZ();
        for (int y = blockLocation.getBlockY() + 1; y <= blockLocation.getBlockY() + 4; y++) {
            if (world.getBlockAt(x, y, z).isPassable() && world.getBlockAt(x, y + 1, z).isPassable()
                    && world.getBlockAt(x, y - 1, z).getType().isSolid()) {
                return new Location(world, x + 0.5D, y, z + 0.5D);
            }
        }
        return null;
    }

    private Player findTargetPlayer(Player caster, double range) {
        Player best = null;
        double bestDistance = range * range;
        Vector direction = caster.getEyeLocation().getDirection().normalize();
        for (Player candidate : alivePlayers()) {
            if (candidate.equals(caster) || !candidate.getWorld().equals(caster.getWorld())) continue;
            Vector offset = candidate.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector());
            double distanceSquared = offset.lengthSquared();
            if (distanceSquared >= bestDistance || distanceSquared < 0.01D) continue;
            double distance = Math.sqrt(distanceSquared);
            double alignment = direction.dot(offset.clone().normalize());
            if (alignment < 0.82D) continue;
            if (caster.getWorld().rayTraceBlocks(caster.getEyeLocation(), direction, distance,
                    org.bukkit.FluidCollisionMode.NEVER) != null) continue;
            best = candidate;
            bestDistance = distanceSquared;
        }
        return best;
    }

    private boolean setTemporaryBlock(Location location, Material material, long durationTicks) {
        return setTemporaryBlock(location, material, durationTicks, false);
    }

    private boolean setTemporaryBlock(Location location, Material material, long durationTicks, boolean replaceSolid) {
        Block block = location.getBlock();
        Location key = block.getLocation();
        TemporaryBlock temporary = temporaryBlocks.get(key);
        if (!replaceSolid && !block.isPassable() && temporary == null) return false;
        if (block.getType() == Material.BEDROCK || block.getState() instanceof InventoryHolder
                || block.getState() instanceof TileState) return false;
        if (temporary == null) {
            temporary = new TemporaryBlock(block.getBlockData().clone(), material,
                    System.currentTimeMillis() + Math.max(1L, durationTicks) * 50L);
            temporaryBlocks.put(key, temporary);
        } else {
            temporary.placedMaterial = material;
            temporary.expiresAtMillis = System.currentTimeMillis() + Math.max(1L, durationTicks) * 50L;
        }
        block.setType(material, false);
        return true;
    }

    private void restoreTemporaryBlock(Location key) {
        TemporaryBlock temporary = temporaryBlocks.remove(key);
        if (temporary == null || key.getWorld() == null) return;
        Block block = key.getBlock();
        if (block.getType() == temporary.placedMaterial) {
            block.setBlockData(temporary.originalData, false);
        }
    }

    private void restoreAllTemporaryBlocks() {
        for (Location location : new ArrayList<>(temporaryBlocks.keySet())) restoreTemporaryBlock(location);
        temporaryBlocks.clear();
    }

    private void checkWinner() {
        if (!running) return;
        List<Player> alive = alivePlayers();
        if (alive.size() == 0) {
            finish(null, "No wizard survived the round.");
        } else if (startingPlayerCount > 1 && alive.size() == 1) {
            finish(alive.get(0), alive.get(0).getName() + " is the last wizard alive!");
        }
    }

    private void finish(Player winner, String message) {
        running = false;
        if (message != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.GOLD + "Wizards » " + ChatColor.YELLOW + message);
            }
        }
        for (UUID id : new ArrayList<>(manaBars.keySet())) removeManaBar(id);
        restoreAllTemporaryBlocks();
        fallingProtection.clear();
        endgameMeteors.clear();
        for (BukkitTask task : spellTasks) if (task != null && !task.isCancelled()) task.cancel();
        spellTasks.clear();
        spectralArrows.clear();
        summonedWolves.clear();
        manaBolts.clear();
        trapRunes.clear();

        for (UUID id : ownedEntities) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) entity.remove();
        }
        ownedEntities.clear();

        for (Map.Entry<UUID, PlayerSnapshot> entry : snapshots.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && !player.isDead()) {
                entry.getValue().restore(player);
            } else {
                pendingRestore.put(entry.getKey(), entry.getValue());
            }
        }
        snapshots.clear();
        states.clear();
        queue.clear();
        selectedKits.clear();
        startingPlayerCount = 0;
        if (arena != null) {
            arena.setTime(6000L);
            arena.setStorm(false);
            arena.setThundering(false);
            WorldBorder border = arena.getWorldBorder();
            border.setCenter(0.0D, 0.0D);
            border.setSize((Math.max(32, Math.min(96, plugin.getConfig().getInt("arena-radius", 48))) * 2.0D) + 4.0D);
        }
    }

    private void restoreTemporaryPlayer(Player player, UUID id) {
        PlayerSnapshot snapshot = snapshots.remove(id);
        if (snapshot != null) snapshot.restore(player);
        states.remove(id);
        removeManaBar(id);
        checkWinner();
    }

    private void refillChests() {
        if (arena == null) return;
        for (Location location : chests) {
            Block block = location.getBlock();
            if (block.getType() != Material.CHEST) block.setType(Material.CHEST, false);
            if (!(block.getState() instanceof Chest chest)) continue;
            Inventory inventory = chest.getBlockInventory();
            inventory.clear();

            int items = 5 + random.nextInt(4);
            int spellSlot = random.nextInt(inventory.getSize());
            inventory.setItem(spellSlot, makeSpellBook(weightedSpell()));
            for (int i = 0; i < items; i++) {
                int slot = random.nextInt(inventory.getSize());
                if (inventory.getItem(slot) != null) continue;
                inventory.setItem(slot, randomLootItem());
            }
            chest.update(true, false);
        }
    }

    private WizardsSpell weightedSpell() {
        List<WizardsSpell> spells = WizardsSpell.implementedSpells();
        int total = spells.stream().mapToInt(WizardsSpell::lootWeight).sum();
        int choice = random.nextInt(Math.max(1, total));
        for (WizardsSpell spell : spells) {
            choice -= spell.lootWeight();
            if (choice < 0) return spell;
        }
        return WizardsSpell.MANA_BOLT;
    }

    private ItemStack randomLootItem() {
        return switch (random.nextInt(12)) {
            case 0 -> new ItemStack(Material.BREAD, 2 + random.nextInt(3));
            case 1 -> new ItemStack(Material.COOKED_BEEF, 1 + random.nextInt(2));
            case 2 -> new ItemStack(Material.COOKED_CHICKEN, 1 + random.nextInt(2));
            case 3 -> new ItemStack(Material.GOLDEN_CARROT, 1 + random.nextInt(2));
            case 4 -> new ItemStack(Material.OAK_PLANKS, 4 + random.nextInt(5));
            case 5 -> new ItemStack(Material.GOLD_INGOT, 1 + random.nextInt(2));
            case 6 -> new ItemStack(Material.IRON_INGOT, 1 + random.nextInt(2));
            case 7 -> new ItemStack(Material.DIAMOND);
            case 8 -> new ItemStack(Material.LEATHER_CHESTPLATE);
            case 9 -> new ItemStack(Material.LEATHER_BOOTS);
            case 10 -> new ItemStack(Material.GOLDEN_HELMET);
            default -> random.nextBoolean() ? makeWandLoot() : makeSoulStar();
        };
    }

    private boolean isSpellBook(ItemStack item) {
        return spellFromItem(item) != null;
    }

    private WizardsSpell spellFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(spellKey, PersistentDataType.STRING);
        return WizardsSpell.fromId(id);
    }

    private boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "wand".equals(item.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.STRING));
    }

    private boolean isLootWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "loot".equals(item.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.STRING));
    }

    private boolean isMenuBook(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(menuKey, PersistentDataType.BYTE);
    }

    private boolean isSoulStar(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(soulKey, PersistentDataType.BYTE);
    }

    private ChatColor categoryColour(WizardsSpell spell) {
        return switch (spell.category()) {
            case ATTACK -> ChatColor.RED;
            case SUPPORT -> ChatColor.GREEN;
            case MISC -> ChatColor.AQUA;
        };
    }

    private String prettyKit(WizardsKit kit) {
        return switch (kit) {
            case MAGE -> "Mage";
            case SORCERER -> "Sorcerer";
            case MYSTIC -> "Mystic";
            case WITCH_DOCTOR -> "Witch Doctor";
        };
    }

    private static final class SpectralArrowData {
        private final UUID owner;
        private final int level;
        private final Location origin;
        private Location lastPosition;
        private SpectralArrowData(UUID owner, int level, Location origin, Location lastPosition) {
            this.owner = owner; this.level = level; this.origin = origin; this.lastPosition = lastPosition;
        }
    }

    private static final class SummonedWolfData {
        private final UUID owner;
        private final long expiresAt;
        private SummonedWolfData(UUID owner, long expiresAt) { this.owner = owner; this.expiresAt = expiresAt; }
    }

    private final class WizardState {
        private final WizardsKit kit;
        private final float maximumMana;
        private final EnumMap<WizardsSpell, Integer> known = new EnumMap<>(WizardsSpell.class);
        private final EnumMap<WizardsSpell, Long> cooldowns = new EnumMap<>(WizardsSpell.class);
        private final WizardsSpell[] wands = new WizardsSpell[MAX_WAND_SLOTS];
        private float mana;
        private int wandCount;
        private int soulStars;
        private float cooldownModifier = 1.0F;
        private boolean alive = true;

        private WizardState(WizardsKit kit) {
            this.kit = kit;
            maximumMana = WizardsRules.maximumMana(kit);
            mana = WizardsRules.initialMana();
            wandCount = WizardsRules.startingWands(kit);
            for (WizardsSpell spell : WizardsRules.initialKnownSpells()) known.put(spell, 1);
        }
    }

    private static final class TemporaryBlock {
        private final org.bukkit.block.data.BlockData originalData;
        private Material placedMaterial;
        private long expiresAtMillis;
        private TemporaryBlock(org.bukkit.block.data.BlockData originalData, Material placedMaterial, long expiresAtMillis) {
            this.originalData = originalData;
            this.placedMaterial = placedMaterial;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private static final class ManaBoltData {
        private final UUID owner;
        private final int level;
        private final Location origin;
        private ManaBoltData(UUID owner, int level, Location origin) {
            this.owner = owner;
            this.level = level;
            this.origin = origin;
        }
    }

    private static final class TrapRuneData {
        private final UUID owner;
        private final Location centre;
        private final int level;
        private final double size;
        private final long createdAt;
        private TrapRuneData(UUID owner, Location centre, int level, double size, long createdAt) {
            this.owner = owner;
            this.centre = centre;
            this.level = level;
            this.size = size;
            this.createdAt = createdAt;
        }
    }

    private static final class MeteorData {
        private final double size;
        private int ticks;
        private MeteorData(double size) { this.size = size; }
    }

    private record PlayerSnapshot(Location location, ItemStack[] contents, ItemStack[] armour,
                                  GameMode gameMode, double health, int food, float saturation,
                                  int level, float exp, boolean allowFlight, boolean flying,
                                  float walkSpeed, float flySpeed, int fireTicks,
                                  List<PotionEffect> effects) {
        static PlayerSnapshot capture(Player player) {
            return new PlayerSnapshot(
                    player.getLocation().clone(),
                    cloneItems(player.getInventory().getContents()),
                    cloneItems(player.getInventory().getArmorContents()),
                    player.getGameMode(),
                    player.getHealth(),
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getLevel(),
                    player.getExp(),
                    player.getAllowFlight(),
                    player.isFlying(),
                    player.getWalkSpeed(),
                    player.getFlySpeed(),
                    player.getFireTicks(),
                    new ArrayList<>(player.getActivePotionEffects()));
        }

        void restore(Player player) {
            player.closeInventory();
            player.setGameMode(gameMode);
            player.getInventory().setContents(cloneItems(contents));
            player.getInventory().setArmorContents(cloneItems(armour));
            player.setFoodLevel(food);
            player.setSaturation(saturation);
            player.setLevel(level);
            player.setExp(exp);
            player.setFireTicks(fireTicks);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            for (PotionEffect effect : effects) player.addPotionEffect(effect);
            player.setWalkSpeed(walkSpeed);
            player.setAllowFlight(allowFlight);
            if (allowFlight) player.setFlying(flying);
            else player.setFlying(false);
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) == null
                    ? 20.0D : player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.max(1.0D, Math.min(health, maxHealth)));
            if (location != null && location.getWorld() != null) player.teleport(location);
        }

        private static ItemStack[] cloneItems(ItemStack[] source) {
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
            return copy;
        }
    }
}
