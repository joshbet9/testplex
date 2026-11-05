package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Legacy-style kit/team lobby pads, cosmetics demo showcase and Bacon Brawl NPC test bots. */
public final class ModernArcadeLobbyRuntime implements Listener {
    private static final String KIT_TITLE = "Kit Select";
    private static final String QUEUE_TITLE = "Team Select | Queue";
    private static final String BOT_MENU_PREFIX = "Bot Cosmetics | ";
    private static final String TEST_BOTS_TITLE = "Test Bots: Setup";
    private static final String TEST_BOT_LOADOUT_TITLE = "Test Bots: Loadout";
    private static final String TEST_BOT_KITS_TITLE = "Test Bots: Kits";
    private static final String TEST_BOT_COSMETICS_TITLE = "Test Bots: Cosmetics";
    private static final String DEBUG_TITLE = "Game Debug Controls";
    private static final String DEBUG_PROFILES_TITLE = "Debug Profiles";
    private static final String DEBUG_FEATURES_TITLE = "Debug Features";
    private static final String DEBUG_VARIABLES_TITLE = "Debug Variables";
    private static final String DEBUG_EVENTS_TITLE = "Debug Events";
    private static final String DEBUG_ACHIEVEMENTS_TITLE = "Debug Achievement Preview";
    private static final List<DebugFeature> COMMON_DEBUG_FEATURES = List.of(
            new DebugFeature("all-kits", "Unlock all kits", Material.NETHER_STAR,
                    "Bypass achievement-kit locks for the selected game.", false),
            new DebugFeature("invulnerable", "Invulnerability", Material.SHIELD,
                    "Ignore damage to this tester in the selected game.", false),
            new DebugFeature("no-fall-damage", "Disable fall damage", Material.FEATHER,
                    "Ignore fall damage to this tester in the selected game.", false),
            new DebugFeature("no-hunger-loss", "Lock hunger", Material.COOKED_BEEF,
                    "Keep this tester's hunger full in the selected game.", false));
    private static final String[] THEMES = {"porky", "inferno", "frost", "slime", "royal", "confetti"};
    private static final String[] BOMB_LOBBERS_THEMES = {
            "classic", "ember", "shockwave", "spark", "confetti", "frost", "slime"
    };
    private static final int[] KIT_MENU_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int KIT_PEDESTAL_X = -17;
    private static final int QUEUE_PEDESTAL_X = 18;

    private final JavaPlugin plugin;
    private final ArcadePortManager manager;
    private final ModernArcadeLobby lobby;
    private final NamespacedKey actionKey, themeKey, botKey, formKey, displayKey, kitNpcKey, teamNpcKey;
    private final Map<String, Bot> bots = new LinkedHashMap<>();
    private final Map<java.util.UUID, TestBotMenuState> testBotMenus = new LinkedHashMap<>();
    private final Map<UUID, DebugPrompt> debugPrompts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> debugVariablePages = new LinkedHashMap<>();
    private final Map<UUID, ArcadePortGame> debugContextGames = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Long> lastBotHitAtByAttacker = new LinkedHashMap<>();
    private final Map<java.util.UUID, Long> lastBotSwingAtByAttacker = new LinkedHashMap<>();
    private final List<Entity> showcase = new ArrayList<>();
    private final List<Entity> kitNpcs = new ArrayList<>();
    private final List<Entity> teamNpcs = new ArrayList<>();
    private ArcadePortGame selectedGame = ArcadePortGame.BACON_BRAWL;
    private World cosmeticPreviewWorld;
    private record TestBotMenuState(TestBotMenuPage page, ArcadePortGame game, int botIndex) {}
    private enum TestBotMenuPage { SETUP, LOADOUT, KITS, COSMETICS }
    private record TestBotCosmetic(String lineId, String lineTitle, String optionId,
            String optionName, String icon) {}
    private record DebugFeature(String key, String title, Material icon, String description, boolean defaultValue) {}
    private record DebugPrompt(ArcadePortGame game, String operation) {}
    private BukkitTask task;

    ModernArcadeLobbyRuntime(JavaPlugin plugin, ArcadePortManager manager, ModernArcadeLobby lobby) {
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-runtime-constructor-start");
        this.plugin = plugin; this.manager = manager; this.lobby = lobby;
        actionKey = new NamespacedKey(plugin, "lobby-runtime-action");
        themeKey = new NamespacedKey(plugin, "lobby-bot-theme");
        botKey = new NamespacedKey(plugin, "test-bot-name");
        formKey = new NamespacedKey(plugin, "test-bot-form");
        displayKey = new NamespacedKey(plugin, "legacy-lobby-datapoint");
        kitNpcKey = new NamespacedKey(plugin, "lobby-kit-npc");
        teamNpcKey = new NamespacedKey(plugin, "lobby-team-npc");
        clearLobbyShowcase();
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-runtime-legacy-bot-cleanup-start");
        clearLegacyBots();
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-runtime-legacy-bot-cleanup-complete");
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-runtime-constructor-complete");
    }

    /**
     * Removes every old Pig/Sheep/Zombie test-bot entity and clears its persisted
     * lobby roster. These entities are not player targets and must never survive
     * a plugin reload or appear in a later game.
     */
    private void clearLegacyBots() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (entity.getPersistentDataContainer().has(botKey, PersistentDataType.STRING)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        boolean hadSavedBots = plugin.getConfig().contains("test-bots");
        plugin.getConfig().set("test-bots", null);
        if (hadSavedBots) plugin.saveConfig();
        bots.clear();
        if (removed > 0 || hadSavedBots) {
            plugin.getLogger().info("Removed " + removed
                    + " legacy lobby NPC test bot(s); persistent global bot storage is disabled.");
        }
    }

    private void clearLobbyShowcase() {
        World world = lobby.ensureWorld();
        if (world == null) return;
        for (Entity entity : new ArrayList<>(world.getEntities())) {
            String tag = entity.getPersistentDataContainer().get(themeKey, PersistentDataType.STRING);
            if (tag != null && (tag.startsWith("showcase:") || "showcase-label".equals(tag))) {
                entity.remove();
            }
        }
    }

    /** Also purge legacy NPC bots from arena chunks that were not loaded at plugin startup. */
    @EventHandler
    public void onLegacyBotChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.getPersistentDataContainer().has(botKey, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    public void setSelectedGame(ArcadePortGame game) {
        if (game == null) return;
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=selected-game-refresh-start game=" + game.id());
        selectedGame = game;
        plugin.getConfig().set("lobby-selected-game", game.id());
        plugin.saveConfig();
        lobby.refreshLobbyScoreboards();
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=selected-game-scoreboards-complete");
        refreshDisplays();
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=selected-game-displays-complete");
        refreshKitAndTeamNpcs();
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=selected-game-kit-team-npcs-complete");
        if (cosmeticPreviewWorld != null) refreshShowcase();
    }

    public void refreshDisplays() {
        World world = lobby.ensureWorld();
        if (world == null) return;
        String name = selectedGame.displayName();
        String map = SourceMapCatalog.selectedResource(plugin, selectedGame);
        String mode = plugin.getConfig().getString("lobby-selected-mode." + selectedGame.id(), "");
        String status = plugin.getConfig().getString("lobby-countdown-status", "Waiting for players");
        display(world, "game-text", ChatColor.AQUA + "" + ChatColor.BOLD + name);
        display(world, "game-map", ChatColor.WHITE + "Map: " + (map == null || map.isBlank() ? "Default" : map));
        display(world, "game-mode", ChatColor.WHITE + "Mode: " + (mode == null || mode.isBlank() ? "Standard" : mode));
        display(world, "game-status", ChatColor.GREEN + status);
        display(world, "kit-text", ChatColor.GREEN + "" + ChatColor.BOLD + "KIT SELECT | " + name);
        String queueLabel = selectedGame.lobbyRolesAssignedAutomatically() ? "ROLE QUEUE | "
                : ArcadePortManager.teamCountForGame(selectedGame) > 0 ? "TEAM SELECT | " : "QUEUE | ";
        display(world, "team-text", ChatColor.BLUE + "" + ChatColor.BOLD + queueLabel + name);
        display(world, "kit-display", ChatColor.GREEN + "Choose a " + name + " kit");
        int teamCount = ArcadePortManager.teamCountForGame(selectedGame);
        if (teamCount > 0) {
            StringBuilder counts = new StringBuilder();
            String[] labels = {"Red", "Blue", "Green", "Yellow"};
            ChatColor[] colours = {ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW};
            for (int team = 0; team < teamCount; team++) {
                if (counts.length() > 0) counts.append(ChatColor.WHITE).append("  |  ");
                counts.append(colours[team]).append(labels[team]).append(" ")
                        .append(manager.queuedCountForTeam(selectedGame, team));
            }
            display(world, "team-display", counts.toString());
        } else if (selectedGame.lobbyRolesAssignedAutomatically()) {
            display(world, "team-display", ChatColor.GOLD + "ROLES ASSIGNED AUTOMATICALLY"
                    + ChatColor.WHITE + " | Queued: " + manager.queuedCount(selectedGame));
        } else {
            String queueDisplayLabel = selectedGame == ArcadePortGame.PLEXBALL ? "INDIVIDUAL QUEUE" : "FREE FOR ALL";
            display(world, "team-display", ChatColor.BLUE + queueDisplayLabel
                    + ChatColor.WHITE + " | Queued: " + manager.queuedCount(selectedGame)
                    + ChatColor.GRAY + " | Test Players: created on Start Game");
        }
    }

    boolean hasKit(ArcadePortGame game, String kitId) {
        if (game == null || kitId == null) return false;
        String normalized = kitId.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
        return kitChoicesFor(game).stream().anyMatch(choice -> choice.id.equals(normalized));
    }

    private void display(World world, String id, String title) {
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            if (id.equals(stand.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING))) {
                stand.setCustomName(title);
            }
        }
    }

    private void refreshKitAndTeamNpcs() {
        World world = lobby.ensureWorld();
        for (Entity entity : new ArrayList<>(kitNpcs)) if (entity != null && entity.isValid()) entity.remove();
        kitNpcs.clear();
        for (Entity entity : new ArrayList<>(teamNpcs)) if (entity != null && entity.isValid()) entity.remove();
        teamNpcs.clear();
        if (world == null) return;
        // The in-memory lists can be empty after reload while tagged entities remain.
        // Remove old kit/team NPC identities before rebuilding the selected game's pads.
        for (Entity entity : new ArrayList<>(world.getEntities())) {
            if (entity.getPersistentDataContainer().has(kitNpcKey, PersistentDataType.STRING)
                    || entity.getPersistentDataContainer().has(teamNpcKey, PersistentDataType.STRING)) {
                entity.remove();
            }
        }

        List<ArcadeLobbyKitCatalogue.KitChoice> choices = kitChoicesFor(selectedGame);
        clearKitAndQueuePedestalAreas(world);
        List<Pedestal> pedestals = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            int centerZ = kitPedestalCenterZ(choices.size(), i);
            pedestals.add(new Pedestal(KIT_PEDESTAL_X, centerZ, choices.get(i).unlock.pedestalColor));
        }
        int teamCount = ArcadePortManager.teamCountForGame(selectedGame);
        int[] teamCentres = teamCount == 4 ? new int[] {9, 3, -3, -9} : new int[] {3, -3};
        Material[] teamWools = {Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL};
        DyeColor[] teamDyes = {DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN, DyeColor.YELLOW};
        String[] teamLabels = {"Red Team", "Blue Team", "Green Team", "Yellow Team"};
        if (teamCount > 0) {
            for (int team = 0; team < teamCount; team++) {
                pedestals.add(new Pedestal(QUEUE_PEDESTAL_X, teamCentres[team], teamWools[team]));
            }
        } else {
            pedestals.add(new Pedestal(QUEUE_PEDESTAL_X, 0, Material.BLUE_WOOL));
        }
        buildLegacyPedestals(world, pedestals);

        for (int i = 0; i < choices.size(); i++) {
            ArcadeLobbyKitCatalogue.KitChoice choice = choices.get(i);
            int centerZ = pedestals.get(i).centerZ;
            Location at = new Location(world, KIT_PEDESTAL_X + 0.5D,
                    ModernArcadeLobby.FLOOR_Y + 2.5D, centerZ + 0.5D);
            at.setYaw(-90.0F);
            kitNpcs.add(spawnKitNpc(world, at, choice));
        }
        if (teamCount > 0) {
            for (int team = 0; team < teamCount; team++) {
                Location at = new Location(world, QUEUE_PEDESTAL_X + 0.5D,
                        ModernArcadeLobby.FLOOR_Y + 2.5D, teamCentres[team] + 0.5D);
                at.setYaw(90.0F);
                Sheep sheep = world.spawn(at, Sheep.class);
                sheep.setColor(teamDyes[team]);
                configureLobbySheep(sheep, ChatColor.BOLD.toString() + teamLabels[team]);
                sheep.getPersistentDataContainer().set(teamNpcKey, PersistentDataType.STRING, "team:" + team);
                teamNpcs.add(sheep);
            }
        } else {
            Location at = new Location(world, QUEUE_PEDESTAL_X + 0.5D,
                    ModernArcadeLobby.FLOOR_Y + 2.5D, 0.5D);
            at.setYaw(90.0F);
            Sheep queue = world.spawn(at, Sheep.class);
            String queueName = selectedGame.lobbyRolesAssignedAutomatically()
                    ? "Auto Roles" : "Free For All";
            configureLobbySheep(queue, ChatColor.AQUA + ChatColor.BOLD.toString() + queueName);
            queue.getPersistentDataContainer().set(teamNpcKey, PersistentDataType.STRING, "queue");
            teamNpcs.add(queue);
        }
    }

    private void configureLobbySheep(Sheep sheep, String label) {
        sheep.setCustomName(label);
        sheep.setCustomNameVisible(true);
        sheep.setInvulnerable(true);
        sheep.setRemoveWhenFarAway(false);
        sheep.setPersistent(false);
        sheep.setSilent(true);
        sheep.setGravity(false);
        sheep.setCollidable(false);
        sheep.setAI(false);
    }

    private LivingEntity spawnKitNpc(World world, Location at, ArcadeLobbyKitCatalogue.KitChoice choice) {
        LivingEntity npc = world.spawn(at, choice.npcType);
        npc.setCustomName(choice.unlock.nameColor + ChatColor.BOLD.toString() + choice.name);
        npc.setCustomNameVisible(true);
        npc.setInvulnerable(true);
        npc.setRemoveWhenFarAway(false);
        npc.setPersistent(false);
        npc.setSilent(true);
        npc.setGravity(false);
        npc.setCollidable(false);
        npc.setCanPickupItems(false);
        npc.setRotation(at.getYaw(), 0.0F);
        if (npc instanceof Mob mob) mob.setAI(false);
        if (npc instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
                    Integer.MAX_VALUE, 0, false, false, false));
        }
        npc.setFireTicks(0);
        npc.setVisualFire(false);
        if (npc instanceof Sheep sheep && choice.entityColor != null) {
            sheep.setColor(choice.entityColor);
        }

        org.bukkit.inventory.EntityEquipment equipment = npc.getEquipment();
        if (equipment != null) {
            equipment.setItemInMainHand(choice.heldItem == null ? null : new ItemStack(choice.heldItem));
            equipment.setItemInMainHandDropChance(0.0F);
            Material[] armourSlots = npcArmorSlots(choice.armour);
            ItemStack[] armour = new ItemStack[armourSlots.length];
            for (int slot = 0; slot < armourSlots.length; slot++) {
                Material material = armourSlots[slot];
                if (material != null) armour[slot] = new ItemStack(material);
            }
            equipment.setArmorContents(armour);
            equipment.setBootsDropChance(0.0F);
            equipment.setLeggingsDropChance(0.0F);
            equipment.setChestplateDropChance(0.0F);
            equipment.setHelmetDropChance(0.0F);
        }

        npc.getPersistentDataContainer().set(kitNpcKey, PersistentDataType.STRING, choice.id);
        return npc;
    }

    static Material[] npcArmorSlots(Material[] kitArmour) {
        Material[] slots = new Material[4];
        if (kitArmour != null) System.arraycopy(kitArmour, 0, slots, 0, Math.min(kitArmour.length, slots.length));
        return slots;
    }

    private void clearKitAndQueuePedestalAreas(World world) {
        clearPedestalArea(world, -22, -13, -20, 20);
        clearPedestalArea(world, 15, 21, -13, 13);
    }

    private void clearPedestalArea(World world, int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getBlockAt(x, ModernArcadeLobby.FLOOR_Y, z).setType(Material.GRASS_BLOCK, false);
                world.getBlockAt(x, ModernArcadeLobby.FLOOR_Y + 1, z).setType(Material.AIR, false);
                world.getBlockAt(x, ModernArcadeLobby.FLOOR_Y + 2, z).setType(Material.AIR, false);
            }
        }
    }

    /**
     * Source-mapped Mineplex kit stations: individual coloured wool pads, a
     * black outline, raised 2x2 wool blocks and stone-slab caps.
     */
    private void buildLegacyPedestals(World world, List<Pedestal> pedestals) {
        for (Pedestal pedestal : pedestals) {
            for (int dx = -3; dx <= 2; dx++) {
                for (int dz = -3; dz <= 2; dz++) {
                    world.getBlockAt(pedestal.centerX + dx, ModernArcadeLobby.FLOOR_Y, pedestal.centerZ + dz)
                            .setType(Material.BLACK_WOOL, false);
                }
            }
        }
        for (Pedestal pedestal : pedestals) {
            for (int dx = -2; dx <= 1; dx++) {
                for (int dz = -2; dz <= 1; dz++) {
                    world.getBlockAt(pedestal.centerX + dx, ModernArcadeLobby.FLOOR_Y, pedestal.centerZ + dz)
                            .setType(pedestal.color, false);
                }
            }
        }
        for (Pedestal pedestal : pedestals) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.getBlockAt(pedestal.centerX + dx, ModernArcadeLobby.FLOOR_Y + 1, pedestal.centerZ + dz)
                            .setType(pedestal.color, false);
                    world.getBlockAt(pedestal.centerX + dx, ModernArcadeLobby.FLOOR_Y + 2, pedestal.centerZ + dz)
                            .setType(Material.STONE_SLAB, false);
                }
            }
        }
    }

    /** Mirrors the legacy four-block kit spacing around the display centre. */
    static int kitPedestalCenterZ(int kitCount, int index) {
        if (kitCount < 1 || index < 0 || index >= kitCount) {
            throw new IllegalArgumentException("Invalid kit pedestal index/count.");
        }
        return (kitCount - 1) * 2 - index * 4;
    }

    private List<ArcadeLobbyKitCatalogue.KitChoice> kitChoicesFor(ArcadePortGame game) {
        List<String> order = plugin.getConfig().getStringList("lobby-kits.order." + game.id());
        // Old server configs hid source kits here. Current kit availability is defined by the
        // shared catalogue and achievement unlocks, so stale hide lists must not remove the menu.
        return ArcadeLobbyKitCatalogue.forGame(game, List.of(), order);
    }

    private static final class Pedestal {
        final int centerX, centerZ;
        final Material color;
        Pedestal(int centerX, int centerZ, Material color) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.color = color;
        }
    }

    private void refreshShowcase() {
        World world = ensureCosmeticPreviewWorld();
        if (world == null) return;
        for (Entity e : new ArrayList<>(showcase)) if (e != null && e.isValid()) e.remove();
        showcase.clear();
        for (Entity e : world.getEntities()) {
            String tag = e.getPersistentDataContainer().get(themeKey, PersistentDataType.STRING);
            if (tag != null && (tag.startsWith("showcase:") || "showcase-label".equals(tag))) e.remove();
        }
        String[] themes = showcaseOptions(selectedGame);
        int columns = Math.min(7, Math.max(3, themes.length));
        int left = columns == 7 ? -9 : -(columns - 1) * 2;
        String titlePrefix = selectedGame == ArcadePortGame.BOMB_LOBBERS
                ? "TNT Impact: " : selectedGame.displayName() + " Preview: ";
        for (int i = 0; i < themes.length; i++) {
            String theme = themes[i];
            int column = i % columns;
            int row = i / columns;
            int x = lobby.spawnLocation().getBlockX() + left + column * 3;
            int z = lobby.spawnLocation().getBlockZ() + 18 + row * 4;
            Location at = new Location(world, x + 0.5D, ModernArcadeLobby.FLOOR_Y + 1.0D, z + 0.5D);

            ArmorStand icon = world.spawn(at.clone().subtract(0.0D, 0.18D, 0.0D), ArmorStand.class);
            icon.setVisible(false);
            icon.setSmall(true);
            icon.setGravity(false);
            icon.setBasePlate(false);
            icon.setArms(false);
            icon.setMarker(true);
            icon.setInvulnerable(true);
            icon.setCanPickupItems(false);
            icon.setHelmet(new ItemStack(showcaseIcon(selectedGame, theme)));
            icon.setCustomName("showcase:" + theme);
            icon.setCustomNameVisible(false);
            icon.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, "showcase:" + theme);
            showcase.add(icon);

            ArmorStand label = world.spawn(at.clone().add(0.0D, 0.65D, 0.0D), ArmorStand.class);
            label.setVisible(false);
            label.setSmall(false);
            label.setGravity(false);
            label.setBasePlate(false);
            label.setArms(false);
            label.setMarker(true);
            label.setInvulnerable(true);
            label.setCanPickupItems(false);
            label.setCustomName(ChatColor.GOLD + titlePrefix + pretty(theme)
                    + ChatColor.GRAY + " | Right-click to preview");
            label.setCustomNameVisible(true);
            label.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, "showcase-label");
            showcase.add(label);
        }
    }

    private World ensureCosmeticPreviewWorld() {
        if (cosmeticPreviewWorld != null) return cosmeticPreviewWorld;
        World world = Bukkit.getWorld("testplex_cosmetic_preview_v1");
        if (world == null) {
            org.bukkit.WorldCreator creator = new org.bukkit.WorldCreator("testplex_cosmetic_preview_v1");
            creator.type(org.bukkit.WorldType.FLAT);
            creator.generateStructures(false);
            world = creator.createWorld();
        }
        if (world == null) {
            plugin.getLogger().severe("Could not create the dedicated game-cosmetic preview world.");
            return null;
        }
        cosmeticPreviewWorld = world;
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        world.getWorldBorder().setCenter(0.0D, 0.0D);
        world.getWorldBorder().setSize(96.0D);
        world.getWorldBorder().setDamageAmount(0.0D);
        for (int x = -20; x <= 20; x++) {
            for (int z = -4; z <= 36; z++) {
                world.getBlockAt(x, ModernArcadeLobby.FLOOR_Y, z).setType(Material.SMOOTH_STONE, false);
            }
        }
        world.setSpawnLocation(0, ModernArcadeLobby.FLOOR_Y + 1, 0);
        return world;
    }

    public void handlePreviewCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.YELLOW + "Cosmetic preview mode is available in-game from the Arcade lobby.");
            return;
        }
        if (args.length > 0 && "exit".equalsIgnoreCase(args[0])) {
            if (cosmeticPreviewWorld != null && player.getWorld() == cosmeticPreviewWorld) {
                lobby.sendToLobby(player);
                player.sendMessage(ChatColor.GREEN + "Returned to the Arcade lobby.");
            }
            return;
        }
        if (cosmeticPreviewWorld != null && player.getWorld() == cosmeticPreviewWorld) {
            lobby.sendToLobby(player);
            player.sendMessage(ChatColor.GREEN + "Returned to the Arcade lobby.");
            return;
        }
        if (!lobby.isLobbyWorld(player.getWorld())) {
            player.sendMessage(ChatColor.YELLOW + "Return to the Arcade lobby before entering cosmetic preview mode.");
            return;
        }
        World world = ensureCosmeticPreviewWorld();
        if (world == null) {
            player.sendMessage(ChatColor.RED + "The cosmetic preview sandbox could not be loaded.");
            return;
        }
        refreshShowcase();
        player.teleport(new Location(world, 0.5D, ModernArcadeLobby.FLOOR_Y + 1.0D, 0.5D));
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setInvulnerable(true);
        lobby.ensurePanelItem(player);
        if (showcaseOptions(selectedGame).length == 0) {
            player.sendMessage(ChatColor.YELLOW + selectedGame.displayName()
                    + " has no game-specific cosmetic line to preview.");
            lobby.sendToLobby(player);
            return;
        }
        player.sendMessage(ChatColor.AQUA + selectedGame.displayName()
                + ChatColor.GRAY + " cosmetic preview sandbox. Right-click a style to preview it. "
                + "Use /arcadeport preview exit to return.");
    }

    /**
     * Keep the sandbox preview specific to the selected game's cosmetic options.
     */
    private String[] showcaseOptions(ArcadePortGame game) {
        String[] registered = registeredCosmeticOptions(game);
        return registered;
    }

    @SuppressWarnings("unchecked")
    private String[] registeredCosmeticOptions(ArcadePortGame game) {
        org.bukkit.plugin.Plugin cosmetics = Bukkit.getPluginManager().getPlugin("TestplexModernArcade");
        if (cosmetics == null || game == null) return new String[0];
        try {
            Object catalog = cosmetics.getClass().getMethod("getCatalog").invoke(cosmetics);
            String gameId = game.isMonsterMaze() ? "monster-maze" : game.id();
            Object definition = catalog.getClass().getMethod("getGame", String.class).invoke(catalog, gameId);
            if (definition == null) return new String[0];
            List<Object> lines = (List<Object>) definition.getClass().getMethod("getCosmeticLines").invoke(definition);
            List<String> options = new ArrayList<>();
            for (Object line : lines) {
                List<Object> choices = (List<Object>) line.getClass().getMethod("getOptions").invoke(line);
                for (Object choice : choices) {
                    String id = (String) choice.getClass().getMethod("getId").invoke(choice);
                    if (!options.contains(id)) options.add(id);
                }
            }
            return options.toArray(String[]::new);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().fine("Could not read cosmetic preview options: " + exception.getMessage());
            return new String[0];
        }
    }

    private Material showcaseIcon(ArcadePortGame game, String option) {
        String value = option == null ? "" : option.toLowerCase(Locale.ROOT);
        if (game == ArcadePortGame.CAMERA_SHY) {
            return switch (value) {
                case "smoke-artist" -> Material.INK_SAC;
                case "decoy" -> Material.FIREWORK_ROCKET;
                case "runner" -> Material.SUGAR;
                case "spotter" -> Material.COMPASS;
                case "understudy" -> Material.ARMOR_STAND;
                default -> Material.SPYGLASS;
            };
        }
        if (game == ArcadePortGame.BOMB_LOBBERS) {
            if (value.contains("tnt") || "classic".equals(value) || "ember".equals(value)) return Material.TNT;
            if ("shockwave".equals(value)) return Material.TARGET;
            if ("spark".equals(value) || "confetti".equals(value)) return Material.FIREWORK_ROCKET;
            if ("frost".equals(value)) return Material.SNOWBALL;
            if ("slime".equals(value)) return Material.SLIME_BALL;
        }
        if (value.contains("lantern") || value.contains("soul")) return Material.SOUL_LANTERN;
        if (value.contains("footprint") || value.contains("bark")) return Material.OAK_LEAVES;
        if (value.contains("dragon-eye")) return Material.ENDER_EYE;
        if (value.contains("compass")) return Material.COMPASS;
        if (value.contains("wing") || value.contains("feather")) return Material.FEATHER;
        if (value.contains("gate") || value.contains("marker")) return Material.BEACON;
        if (value.contains("dragon") || value.contains("flame") || value.contains("ember")) return Material.FIRE_CHARGE;
        if (value.contains("cloud") || value.contains("snow") || value.contains("ice") || value.contains("frost")) return Material.SNOWBALL;
        if (value.contains("shell") || value.contains("evolution") || value.contains("void")) return Material.DRAGON_EGG;
        if (value.contains("nature") || value.contains("bloom")) return Material.OAK_SAPLING;
        if (value.contains("sand") || value.contains("oath") || value.contains("arena")) return Material.IRON_SWORD;
        if (value.contains("prism") || value.contains("glass") || value.contains("shatter")) return Material.LIGHT_BLUE_STAINED_GLASS;
        if (value.contains("arrow") || value.contains("bullseye") || value.contains("fletching") || value.contains("ricochet")) return Material.ARROW;
        if (value.contains("paint") || value.contains("splatter") || value.contains("chrome") || value.contains("neon") || value.contains("colourburst")) return Material.SNOWBALL;
        if (value.contains("ribbon") || value.contains("chalk") || value.contains("red-walk")) return Material.GLOW_INK_SAC;
        if (value.contains("sheep") || value.contains("wool") || value.contains("fleece") || value.contains("shepherd")) return Material.WHITE_WOOL;
        if (value.contains("shovel") || value.contains("shards")) return Material.IRON_SHOVEL;
        if (value.contains("turf") || value.contains("sigil") || value.contains("chevron")) return Material.TARGET;
        return game.floor();
    }

    @EventHandler
    public void onLobbyPadUse(PlayerInteractEvent e) {
        if(e.getHand()!=EquipmentSlot.HAND || (e.getAction()!=Action.RIGHT_CLICK_BLOCK && e.getAction()!=Action.RIGHT_CLICK_AIR)) return;
        Player p=e.getPlayer();
        if(!lobby.isLobbyWorld(p.getWorld()) || e.getAction()==Action.RIGHT_CLICK_AIR || e.getClickedBlock()==null) return;
        int x=e.getClickedBlock().getX(), z=e.getClickedBlock().getZ();
        if(nearPad(x,z,-17,0)){e.setCancelled(true);openKitMenu(p);}
        else if(nearPad(x,z,18,0)){e.setCancelled(true);openQueueMenu(p);}
    }
    private boolean nearPad(int x,int z,int cx,int cz){return Math.abs(x-cx)<=3 && Math.abs(z-cz)<=3;}

    @EventHandler
    public void onLobbyNpcUse(PlayerInteractEntityEvent e) {
        if (handleCosmeticPreviewUse(e)) return;
        Entity entity = e.getRightClicked();
        String kit = entity.getPersistentDataContainer().get(kitNpcKey, PersistentDataType.STRING);
        String queue = entity.getPersistentDataContainer().get(teamNpcKey, PersistentDataType.STRING);
        String bot = entity.getPersistentDataContainer().get(botKey, PersistentDataType.STRING);
        if (kit != null) {
            e.setCancelled(true);
            openKitMenu(e.getPlayer());
            return;
        }
        if (queue != null) {
            e.setCancelled(true);
            Player player = e.getPlayer();
            if (queue.startsWith("team:")) {
                int team = Integer.parseInt(queue.substring("team:".length()));
                manager.selectLobbyTeam(player, selectedGame, team);
                manager.queue(selectedGame, player, false);
                player.sendMessage(ChatColor.GREEN + "Selected " + teamName(team) + " and joined the "
                        + selectedGame.displayName() + " queue.");
                refreshDisplays();
                openQueueMenu(player);
            } else {
                openQueueMenu(player);
            }
            return;
        }
        if (bot != null && hasAdminAccess(e.getPlayer())) {
            e.setCancelled(true);
            Bot b = bots.get(bot);
            if (b != null) openBotCosmetics(e.getPlayer(), b);
        }
    }

    private boolean handleCosmeticPreviewUse(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || cosmeticPreviewWorld == null
                || event.getPlayer().getWorld() != cosmeticPreviewWorld) return false;
        Entity display = event.getRightClicked();
        String tag = display.getPersistentDataContainer().get(themeKey, PersistentDataType.STRING);
        if (tag == null || !tag.startsWith("showcase:") || "showcase-label".equals(tag)) return false;
        event.setCancelled(true);
        String style = tag.substring("showcase:".length());
        playCosmeticPreview(event.getPlayer(), display.getLocation(), style);
        return true;
    }

    private void playCosmeticPreview(Player player, Location source, String style) {
        World world = source.getWorld();
        if (world == null || player.getWorld() != world) return;
        Location center = source.clone().add(0.0D, 1.1D, 0.0D);
        GameAbilityCosmetics.preview(player, center, style);
        player.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.2F);
        player.sendMessage(ChatColor.AQUA + selectedGame.displayName() + ChatColor.GRAY
                + " preview: " + ChatColor.GOLD + pretty(style));
    }

    private static String teamName(int team) {
        return switch (team) {
            case 0 -> "Red Team";
            case 1 -> "Blue Team";
            case 2 -> "Green Team";
            default -> "Yellow Team";
        };
    }

    private void openKitMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, KIT_TITLE);
        List<ArcadeLobbyKitCatalogue.KitChoice> choices = kitChoicesFor(selectedGame);
        if (choices.isEmpty()) {
            inv.setItem(13, item(Material.BARRIER, ChatColor.RED + "No Kit Selection",
                    "This game uses its default loadout.",
                    "The legacy kit datapoint remains available."));
        } else {
            for (int i = 0; i < choices.size() && i < KIT_MENU_SLOTS.length; i++) {
                ArcadeLobbyKitCatalogue.KitChoice choice = choices.get(i);
                ItemStack it;
                if (selectedGame == ArcadePortGame.RUNNER && "runnerkit-switcher".equals(choice.id)) {
                    it = item(choice.icon, ChatColor.YELLOW + choice.name,
                            ChatColor.GRAY + "Requires every Runner achievement.",
                            ChatColor.GRAY + "Randomly swaps with one eligible Runner, once per round.",
                            ChatColor.GREEN + "Click to select");
                } else if (selectedGame == ArcadePortGame.SPLEEF && "spleef-gust".equals(choice.id)) {
                    it = item(choice.icon, ChatColor.YELLOW + choice.name,
                            ChatColor.GRAY + "Unlock with Praying to the Gods: win while falling into the void.",
                            ChatColor.GRAY + "A Wind Charge can launch you or knock opponents back.",
                            ChatColor.GRAY + "Recharges every 20 seconds.",
                            ChatColor.GREEN + "Click to select");
                } else if (selectedGame == ArcadePortGame.TURF_WARS
                        && "turf-wars-fortifier".equals(choice.id)) {
                    boolean unlocked = plugin.getConfig().getBoolean("local-achievements."
                            + p.getUniqueId() + ".turf-wars.genius-mathematician", false);
                    it = item(choice.icon, ChatColor.YELLOW + choice.name,
                            ChatColor.GRAY + "Unlock with Genius Mathematician:",
                            ChatColor.GRAY + "win without leaving your base or being killed.",
                            ChatColor.GRAY + "Carries 12 team-coloured wool.",
                            unlocked ? ChatColor.GREEN + "Click to select" : ChatColor.RED + "Locked");
                } else if (selectedGame == ArcadePortGame.SUPER_SPLOOR) {
                    String selectedKit = plugin.getConfig().getString(
                            "lobby-kit-preferences." + p.getUniqueId() + ".super-sploor", "sprayer");
                    String[] details = switch (choice.id) {
                        case "roller" -> new String[] {"Fast close-range paint path.",
                                "Secondary: place an Ink Mine.", "Special: Stampede through enemy lines."};
                        case "charger" -> new String[] {"Hold right-click, release left-click for a precise ink line.",
                                "Secondary: reveal nearby enemies.", "Special: fire a long Prism Beam."};
                        case "lobber" -> new String[] {"Lob arcing shots with a wide paint splash.",
                                "Secondary: launch a Sprinkler.", "Special: call a pulsing Paintstorm."};
                        case "dualies" -> new String[] {"Rapid paired shots and a two-charge dodge roll.",
                                "Secondary: throw a Suction Bomb.", "Special: dash through a painted afterimage."};
                        default -> new String[] {"Balanced mid-range paint weapon.",
                                "Secondary: throw a Burst Bomb.", "Special: release a close-range Colourburst."};
                    };
                    it = item(choice.icon, ChatColor.YELLOW + choice.name,
                            ChatColor.GRAY + details[0], ChatColor.GRAY + details[1], ChatColor.GRAY + details[2],
                            choice.id.equalsIgnoreCase(selectedKit)
                                    ? ChatColor.GOLD + "Currently selected" : ChatColor.GREEN + "Click to select");
                } else if (selectedGame == ArcadePortGame.CAMERA_SHY) {
                    String selectedSnapKit = plugin.getConfig().getString(
                            "camera-shy-kit-preferences." + p.getUniqueId(), "smoke-artist");
                    boolean selected = choice.id.equals("snap-" + selectedSnapKit);
                    it = item(choice.icon, ChatColor.YELLOW + choice.name,
                            ChatColor.GRAY + "Choose this kit for your next Snap round.",
                            selected ? ChatColor.GOLD + "Currently selected" : ChatColor.GREEN + "Click to select");
                } else if (choice.unlock == ArcadeLobbyKitCatalogue.Unlock.ACHIEVEMENT
                        && ArcadePortManager.achievementKitUnlock(selectedGame, choice.id) != null) {
                    String achievement = ArcadePortManager.achievementKitUnlock(selectedGame, choice.id);
                    boolean unlocked = plugin.getConfig().getBoolean("local-achievements."
                            + p.getUniqueId() + "." + selectedGame.id() + "." + achievement, false);
                    it = item(choice.icon, ChatColor.YELLOW + choice.name,
                            ChatColor.GRAY + "Unlock with " + ArcadePortManager.titleCaseAchievement(achievement) + ".",
                            ChatColor.GRAY + achievementKitSummary(choice.id),
                            unlocked ? ChatColor.GREEN + "Click to select" : ChatColor.RED + "Locked");
                } else {
                    it = item(choice.icon, ChatColor.YELLOW + choice.name,
                            ChatColor.GRAY + "Choose this kit for your next " + selectedGame.displayName() + ".",
                            ChatColor.GREEN + "Click to select");
                }
                mark(it, "kit:" + choice.id, null);
                inv.setItem(KIT_MENU_SLOTS[i], it);
            }
        }
        inv.setItem(49, item(Material.RED_BED, ChatColor.GRAY + "Back / Close"));
        p.openInventory(inv);
    }

    private String achievementKitSummary(String kitId) {
        return switch (kitId) {
            case "runner-turtle" -> "Copper axe hits shell Chasers; every third hit earns a Wind Charge.";
            case "runner-trickster" -> "Swap once with a random Chaser and launch Runner distractions.";
            case "shepherd" -> "Carry unlimited sheep; Slowness increases every two sheep.";
            case "warden" -> "Use an iron sword and shield to protect Humans.";
            default -> "Use the kit's specialised loadout.";
        };
    }
    private void openQueueMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, QUEUE_TITLE);
        int teamCount = ArcadePortManager.teamCountForGame(selectedGame);
        int selectedTeam = manager.lobbyTeamPreference(p, selectedGame, teamCount);
        inv.setItem(4, item(Material.PORKCHOP, ChatColor.GOLD + selectedGame.displayName(),
                ChatColor.GRAY + (teamCount > 0 ? "Choose a team, then join the queue."
                        : selectedGame.lobbyRolesAssignedAutomatically()
                                ? "Your match role is assigned automatically."
                                : "Free-for-all: no team is required."),
                ChatColor.GRAY + "Players queued: " + manager.queuedCount(selectedGame)));
        if (teamCount > 0) {
            Material[] dyes = {Material.RED_DYE, Material.BLUE_DYE, Material.GREEN_DYE, Material.YELLOW_DYE};
            ChatColor[] colours = {ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW};
            for (int team = 0; team < teamCount; team++) {
                ItemStack choice = item(dyes[team], colours[team] + teamName(team),
                        ChatColor.GRAY + "Queued: " + manager.queuedCountForTeam(selectedGame, team),
                        ChatColor.YELLOW + (selectedTeam == team ? "Your current team" : "Click to select and queue"));
                mark(choice, "team:" + team, null);
                inv.setItem(new int[] {10, 12, 14, 16}[team], choice);
            }
        }
        ItemStack join = item(Material.LIME_DYE, ChatColor.GREEN + "Join Queue",
                ChatColor.GRAY + (teamCount > 0 && selectedTeam < 0
                        ? "Choose a team first, or you will be balanced automatically."
                        : "Join the selected game's pre-game queue."));
        mark(join, "queue-join", null);
        inv.setItem(11, join);
        ItemStack leave = item(Material.RED_DYE, ChatColor.RED + "Leave Queue",
                ChatColor.GRAY + "Remove yourself from all waiting queues.");
        mark(leave, "queue-leave", null);
        inv.setItem(15, leave);
        inv.setItem(22, item(Material.RED_BED, ChatColor.GRAY + "Back / Close"));
        p.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e){
        String title=ChatColor.stripColor(e.getView().getTitle());
        if(title==null || !(title.equals(KIT_TITLE)||title.equals(QUEUE_TITLE)||title.startsWith(BOT_MENU_PREFIX)
                || title.equals(TEST_BOTS_TITLE) || title.equals(TEST_BOT_LOADOUT_TITLE)
                || title.equals(TEST_BOT_KITS_TITLE) || title.equals(TEST_BOT_COSMETICS_TITLE)
                || title.equals(DEBUG_TITLE) || title.equals(DEBUG_PROFILES_TITLE)
                || title.equals(DEBUG_FEATURES_TITLE) || title.equals(DEBUG_EVENTS_TITLE)
                || title.equals(DEBUG_ACHIEVEMENTS_TITLE)
                || title.startsWith(DEBUG_VARIABLES_TITLE)))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||e.getClickedInventory()==null||e.getClickedInventory()!=e.getView().getTopInventory())return;
        if (isDebugMenuTitle(title) && !plugin.hasAdminAccess(p)) {
            p.closeInventory();
            p.sendMessage(ChatColor.RED + "Authorised tester access is required for game debug controls.");
            return;
        }
        if ((title.equals(TEST_BOTS_TITLE) || title.equals(TEST_BOT_LOADOUT_TITLE)
                || title.equals(TEST_BOT_KITS_TITLE) || title.equals(TEST_BOT_COSMETICS_TITLE))
                && !plugin.hasAdminAccess(p)) {
            p.closeInventory();
            p.sendMessage(ChatColor.RED + "Authorised tester access is required for bot configuration.");
            return;
        }
        ItemStack it=e.getCurrentItem();if(it==null||it.getType().isAir()||!it.hasItemMeta())return;
        String action=it.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);
        if (isDebugMenuTitle(title)) {
            handleDebugMenuClick(p, title, action, e.getClick());
            return;
        }
        if (title.equals(TEST_BOTS_TITLE) || title.equals(TEST_BOT_LOADOUT_TITLE)
                || title.equals(TEST_BOT_KITS_TITLE) || title.equals(TEST_BOT_COSMETICS_TITLE)) {
            handleTestBotMenuClick(p, title, action);
            return;
        }
        if(KIT_TITLE.equals(title)){
            if(action!=null&&action.startsWith("kit:")){
                String kit=action.substring(4);manager.selectKit(p,kit);
                if (selectedGame == ArcadePortGame.TURF_WARS && "turf-wars-fortifier".equals(kit)
                        && !manager.isDebugOverrideEnabled(p.getUniqueId(), selectedGame, "all-kits")
                        && !plugin.getConfig().getBoolean("local-achievements." + p.getUniqueId()
                                + ".turf-wars.genius-mathematician", false)) {
                    p.closeInventory();
                    return;
                }
                plugin.getConfig().set("lobby-kit-preferences."+p.getUniqueId()+"."+selectedGame.id(),kit);
                plugin.saveConfig();
                lobby.refreshPlayerScoreboard(p);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
                p.sendMessage(ChatColor.GREEN + "Selected " + kit.replace('-', ' ')
                        + " for " + selectedGame.displayName() + ".");
                p.closeInventory();
            }else if(e.getSlot()==31)p.closeInventory();
        } else if (QUEUE_TITLE.equals(title)) {
            if (action != null && action.startsWith("team:")) {
                int team = Integer.parseInt(action.substring("team:".length()));
                manager.selectLobbyTeam(p, selectedGame, team);
                manager.queue(selectedGame, p, false);
                refreshDisplays();
                openQueueMenu(p);
            } else if ("queue-join".equals(action)) {
                manager.queue(selectedGame, p, false);
                refreshDisplays();
                openQueueMenu(p);
            } else if ("queue-leave".equals(action)) {
                if (manager.leaveQueue(selectedGame, p)) {
                    p.sendMessage(ChatColor.YELLOW + "You have left the Arcade queue.");
                } else {
                    p.sendMessage(ChatColor.YELLOW + "You are not queued for " + selectedGame.displayName() + ".");
                }
                refreshDisplays();
                openQueueMenu(p);
            } else if (e.getSlot() == 22) p.closeInventory();
        }else{
            String key=it.getItemMeta().getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
            Bot b=key==null?null:bots.get(key);if(b==null)return;
            if(action!=null&&action.startsWith("theme:")){b.theme=action.substring(6);saveBot(b);refreshBotAppearance(b);openBotCosmetics(p,b);}
            else if(action!=null&&action.startsWith("form:")){b.form=action.substring(5);saveBot(b);replaceBotEntity(b);openBotCosmetics(p,b);}
            else if("bot-close".equals(action))p.closeInventory();
        }
    }

    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of(lore));i.setItemMeta(meta);return i;}
    private void mark(ItemStack i,String action,String target){ItemMeta m=i.getItemMeta();m.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);if(target!=null)m.getPersistentDataContainer().set(botKey,PersistentDataType.STRING,target);i.setItemMeta(m);}

    private void loadSavedBots(){
        var section=plugin.getConfig().getConfigurationSection("test-bots");if(section==null)return;
        for(String key:section.getKeys(false)){
            Bot b=new Bot(key,plugin.getConfig().getString("test-bots."+key+".name",key),
                plugin.getConfig().getString("test-bots."+key+".theme","porky"),
                plugin.getConfig().getString("test-bots."+key+".form","pig"),
                plugin.getConfig().getString("test-bots."+key+".kit","el-muchacho-pigo"));
            bots.put(key,b);spawnBotEntity(b,lobby.spawnLocation().add(bots.size()*1.6D-3.0D,-3.0D,8.0D),false);
        }
    }

    private boolean isConfiguredOwner(Player player) {
        List<String> owners = new ArrayList<>(plugin.getConfig().getStringList("owner-usernames"));
        if (owners.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) owners.add("jn_playzmc");
        return owners.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
    }

    private boolean hasAdminAccess(org.bukkit.command.CommandSender sender) {
        // Treat the configured sandbox owner as authoritative even if Paper's
        // operator/permission cache has not refreshed yet on this login.
        if (sender instanceof Player owner && isConfiguredOwner(owner)) {
            if (!owner.isOp()) owner.setOp(true);
            return true;
        }
        if (sender.isOp() || sender.hasPermission("testplex.arcadeports.admin")) return true;
        if (!(sender instanceof Player player)) return false;
        List<String> admins = new ArrayList<>(plugin.getConfig().getStringList("admin-usernames"));
        if (admins.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) admins.add("jn_playzmc");
        boolean configured = admins.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        if (configured && !player.isOp()) player.setOp(true);
        return configured || player.isOp() || player.hasPermission("testplex.arcadeports.admin");
    }

    public void handleBotCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.YELLOW + "Open Bot Configuration from the Arcade lobby hotbar.");
            return;
        }
        if (!lobby.isLobbyWorld(player.getWorld())) {
            player.sendMessage(ChatColor.YELLOW + "Bot loadouts can be configured from the Arcade lobby before a game starts.");
            return;
        }
        openTestBotSetup(player, selectedGame);
    }

    public void handleDebugCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.YELLOW + "Open Game Debug Controls from the Arcade lobby hotbar.");
            return;
        }
        if (!plugin.hasAdminAccess(player)) {
            player.sendMessage(ChatColor.RED + "Authorised tester access is required for game debug controls.");
            return;
        }
        ArcadePortGame game = lobby.isLobbyWorld(player.getWorld())
                ? selectedGame : manager.activeDebugGame();
        if (game == null) {
            player.sendMessage(ChatColor.YELLOW + "Select a game in the Arcade lobby or join an active game before opening its debug controls.");
            return;
        }
        debugContextGames.put(player.getUniqueId(), game);
        if (args != null && args.length > 0 && "profile".equalsIgnoreCase(args[0])) {
            handleDebugProfileCommand(player, args);
            return;
        }
        openDebugMenu(player);
    }

    private ArcadePortGame debugGame(Player player) {
        return debugContextGames.getOrDefault(player.getUniqueId(), selectedGame);
    }

    private void openDebugMenu(Player player) {
        ArcadePortGame game = debugGame(player);
        manager.beginDebugContext(player, game);
        Inventory inventory = Bukkit.createInventory(null, 54, DEBUG_TITLE);
        String profile = manager.debugProfiles().selected(player.getUniqueId(), game);
        inventory.setItem(4, item(Material.COMPARATOR, ChatColor.RED + game.displayName() + " Debug",
                ChatColor.GRAY + "Selected profile: " + ChatColor.WHITE + profile,
                ChatColor.GRAY + "Controls below are specific to this game."));
        inventory.setItem(19, testBotButton(Material.WRITABLE_BOOK, "Profiles: " + profile,
                "Create, load, duplicate, rename, save or delete configurations.", "debug-open:profiles"));
        inventory.setItem(21, testBotButton(Material.LEVER, "Feature switches",
                "Per-game features and common test overrides.", "debug-open:features"));
        inventory.setItem(23, testBotButton(Material.COMPARATOR, "Gameplay variables",
                "Tune rule constants. Numeric values apply after restarting the round.", "debug-open:variables:0"));
        inventory.setItem(25, testBotButton(Material.IRON_GOLEM_SPAWN_EGG, "Test bot setup",
                "Set up the next round's bot count, team, kit and cosmetics.", "debug-open:bots"));
        inventory.setItem(27, testBotButton(Material.TARGET, "Game event tools",
                "Trigger supported events in the active game through normal runtime handlers.", "debug-open:events"));
        inventory.setItem(29, testBotButton(Material.KNOWLEDGE_BOOK, "Achievement previews",
                "Preview each achievement message; previews never unlock or save progress.", "debug-open:achievements"));
        inventory.setItem(37, testBotButton(Material.CLOCK, "Profile application",
                "Immediate switches take effect now. Rule switches and numeric values apply when the next round starts."));
        inventory.setItem(39, testBotButton(Material.FEATHER, "Advance game phase or timer",
                "Skip this game's start countdown, live phase, turn, or round timer when supported.", "debug-advance-phase"));
        inventory.setItem(41, testBotButton(Material.GOLDEN_APPLE, "Restore tester vitals",
                "In your active round: restore health and hunger, clear fire and potion effects. Does not change stats.",
                "debug-restore-vitals"));
        inventory.setItem(40, testBotButton(Material.BARRIER, "Reset to Defaults",
                "Clear this profile's overrides and restore intended mechanics.", "debug-reset"));
        inventory.setItem(49, testBotButton(Material.BARRIER, "Close", "debug-close"));
        player.openInventory(inventory);
    }

    private boolean isDebugMenuTitle(String title) {
        return DEBUG_TITLE.equals(title) || DEBUG_PROFILES_TITLE.equals(title)
                || DEBUG_FEATURES_TITLE.equals(title) || DEBUG_EVENTS_TITLE.equals(title)
                || DEBUG_ACHIEVEMENTS_TITLE.equals(title)
                || title != null && title.startsWith(DEBUG_VARIABLES_TITLE);
    }

    private void handleDebugMenuClick(Player player, String title, String action, ClickType click) {
        if (action == null) return;
        if ("debug-close".equals(action)) { player.closeInventory(); return; }
        if ("debug-back".equals(action)) { openDebugMenu(player); return; }
        if ("debug-open:profiles".equals(action)) { openDebugProfiles(player); return; }
        if ("debug-open:features".equals(action)) { openDebugFeatures(player); return; }
        if ("debug-open:events".equals(action)) { openDebugEvents(player); return; }
        if ("debug-open:achievements".equals(action)) { openDebugAchievements(player); return; }
        if ("debug-open:variables:0".equals(action)) { openDebugVariables(player, 0); return; }
        if ("debug-open:bots".equals(action)) { openTestBotSetup(player, debugGame(player)); return; }
        ArcadePortGame game = debugGame(player);
        if (action.startsWith("debug-event:")) {
            String eventId = action.substring("debug-event:".length());
            boolean triggered = manager.debugTriggerEvent(player, game, eventId);
            player.sendMessage(triggered ? ChatColor.GREEN + "The game event was triggered."
                    : ChatColor.YELLOW + "That event is unavailable for your current game role or phase.");
            openDebugEvents(player);
            return;
        }
        if (action.startsWith("debug-achievement:")) {
            boolean previewed = manager.previewAchievementForTester(player, game,
                    action.substring("debug-achievement:".length()));
            player.sendMessage(previewed ? ChatColor.AQUA + "Achievement preview shown; progress was not changed."
                    : ChatColor.YELLOW + "That achievement is unavailable for this game.");
            openDebugAchievements(player);
            return;
        }
        if ("debug-advance-phase".equals(action)) {
            boolean advanced = manager.debugAdvanceStartPhase(player, game);
            player.sendMessage(advanced ? ChatColor.GREEN + "The current supported game phase was advanced."
                    : ChatColor.YELLOW + "This game has no supported active phase to advance.");
            openDebugMenu(player);
            return;
        }
        if ("debug-restore-vitals".equals(action)) {
            boolean restored = manager.debugRestoreTesterVitals(player, game);
            player.sendMessage(restored ? ChatColor.GREEN + "Your in-round tester vitals were restored."
                    : ChatColor.YELLOW + "Join a live round of this game before restoring tester vitals.");
            openDebugMenu(player);
            return;
        }
        if ("debug-reset".equals(action)) {
            manager.resetDebugProfile(player.getUniqueId(), game);
            player.sendMessage(ChatColor.GREEN + game.displayName()
                    + " debug profile reset to intended game defaults.");
            openDebugMenu(player);
            return;
        }
        if (action.startsWith("debug-toggle:")) {
            String key = action.substring("debug-toggle:".length());
            boolean fallback = debugFeatures(game).stream().filter(feature -> feature.key().equals(key))
                    .map(DebugFeature::defaultValue).findFirst().orElse(false);
            manager.toggleDebugOverride(player, game, key, fallback);
            openDebugFeatures(player);
            return;
        }
        if (DEBUG_PROFILES_TITLE.equals(title)) { handleDebugProfileMenuClick(player, action); return; }
        if (DEBUG_FEATURES_TITLE.equals(title)) return;
        if (title != null && title.startsWith(DEBUG_VARIABLES_TITLE)) {
            handleDebugVariableClick(player, action, click);
        }
    }

    private void openDebugProfiles(Player player) {
        ArcadePortGame game = debugGame(player);
        Inventory inventory = Bukkit.createInventory(null, 54, DEBUG_PROFILES_TITLE);
        String current = manager.debugProfiles().selected(player.getUniqueId(), game);
        inventory.setItem(4, item(Material.WRITABLE_BOOK, ChatColor.AQUA + game.displayName() + " Profiles",
                ChatColor.GRAY + "Selected: " + ChatColor.WHITE + current,
                ChatColor.GRAY + "Profile changes are saved locally for this tester and game."));
        int slot = 9;
        for (String profile : manager.debugProfiles().profiles(player.getUniqueId(), game)) {
            if (slot > 44) break;
            boolean selected = profile.equals(current);
            ItemStack choice = testBotButton(selected ? Material.ENCHANTED_BOOK : Material.BOOK,
                    (selected ? ChatColor.GREEN : ChatColor.YELLOW) + profile,
                    ChatColor.GRAY + "Left-click to load this profile.", "debug-profile-load:" + profile);
            inventory.setItem(slot++, choice);
        }
        inventory.setItem(45, testBotButton(Material.WRITABLE_BOOK, "Create profile", "Start with default game mechanics and no overrides.", "debug-profile-prompt:create"));
        inventory.setItem(46, testBotButton(Material.COPYING_TABLE, "Duplicate profile", "Copy the selected profile to a new name.", "debug-profile-prompt:duplicate"));
        inventory.setItem(47, testBotButton(Material.NAME_TAG, "Rename profile", "Rename the selected non-default profile.", "debug-profile-prompt:rename"));
        inventory.setItem(48, testBotButton(Material.LAVA_BUCKET, "Delete profile", "Delete the selected non-default profile.", "debug-profile-prompt:delete"));
        inventory.setItem(49, testBotButton(Material.ARROW, "Back", "debug-back"));
        inventory.setItem(51, testBotButton(Material.WRITABLE_BOOK, "Save profile", "Save the selected profile to disk.", "debug-profile-save"));
        player.openInventory(inventory);
    }

    private void openDebugFeatures(Player player) {
        ArcadePortGame game = debugGame(player);
        Inventory inventory = Bukkit.createInventory(null, 54, DEBUG_FEATURES_TITLE);
        inventory.setItem(4, item(Material.LEVER, ChatColor.AQUA + game.displayName() + " Features",
                ChatColor.GRAY + "Settings are isolated to your profile. Each control shows when it applies."));
        List<DebugFeature> features = debugFeatures(game);
        int slot = 9;
        for (DebugFeature feature : features) {
            if (slot > 44) break;
            boolean enabled = manager.debugFeatureEnabled(player.getUniqueId(), game,
                    feature.key(), feature.defaultValue());
            inventory.setItem(slot++, testBotButton(enabled ? Material.LIME_DYE : feature.icon(),
                    feature.title() + ": " + (enabled ? ChatColor.GREEN + "On" : ChatColor.RED + "Off"),
                    feature.description() + " " + (debugFeatureAppliesImmediately(game, feature.key())
                            ? "Applies immediately." : "Applies when the next round starts."),
                    "debug-toggle:" + feature.key()));
        }
        inventory.setItem(49, testBotButton(Material.ARROW, "Back", "debug-back"));
        player.openInventory(inventory);
    }

    private void openDebugEvents(Player player) {
        ArcadePortGame game = debugGame(player);
        Inventory inventory = Bukkit.createInventory(null, 54, DEBUG_EVENTS_TITLE);
        inventory.setItem(4, item(Material.TARGET, ChatColor.AQUA + game.displayName() + " Event Tools",
                ChatColor.GRAY + "Each button invokes a game-specific runtime event.",
                ChatColor.YELLOW + "Only available to authorised testers."));
        List<GameDebugEventCatalog.Event> events = GameDebugEventCatalog.events(game);
        int slot = 9;
        for (GameDebugEventCatalog.Event event : events) {
            if (slot > 44) break;
            inventory.setItem(slot++, testBotButton(event.icon(), event.title(), event.description(),
                    "debug-event:" + event.id()));
        }
        if (events.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, ChatColor.GRAY + "No event tools registered for this game yet"));
        }
        inventory.setItem(49, testBotButton(Material.ARROW, "Back", "debug-back"));
        player.openInventory(inventory);
    }

    private void openDebugAchievements(Player player) {
        ArcadePortGame game = debugGame(player);
        Inventory inventory = Bukkit.createInventory(null, 54, DEBUG_ACHIEVEMENTS_TITLE);
        inventory.setItem(4, item(Material.KNOWLEDGE_BOOK,
                ChatColor.AQUA + game.displayName() + " Achievement Previews",
                ChatColor.GRAY + "Click one to display its message and sound.",
                ChatColor.YELLOW + "No achievement or statistic will be saved."));
        List<ArcadeProgressCatalog.Achievement> achievements = ArcadeProgressCatalog.achievements(game.id());
        int slot = 9;
        for (ArcadeProgressCatalog.Achievement achievement : achievements) {
            if (slot > 44) break;
            inventory.setItem(slot++, testBotButton(Material.PAPER,
                    ChatColor.GOLD + achievement.name(),
                    achievement.requirement(),
                    "debug-achievement:" + achievement.id()));
        }
        if (achievements.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, ChatColor.GRAY + "No achievement entries registered"));
        }
        inventory.setItem(49, testBotButton(Material.ARROW, "Back", "debug-back"));
        player.openInventory(inventory);
    }

    private List<DebugFeature> debugFeatures(ArcadePortGame game) {
        Map<String, DebugFeature> features = new LinkedHashMap<>();
        for (DebugFeature feature : COMMON_DEBUG_FEATURES) features.put(feature.key(), feature);
        for (GameDebugVariableCatalog.Variable variable : manager.debugVariables(game)) {
            if (variable.kind() != GameDebugVariableCatalog.Kind.BOOLEAN) continue;
            String title = variable.key().equals("BOMB_LOBBERS_POWERUP_SPAWNING_ENABLED")
                    ? "Power-up spawns" : variable.label();
            String description = variable.key().equals("BOMB_LOBBERS_POWERUP_SPAWNING_ENABLED")
                    ? "Pause power-up waves and clear loose drops; power-ups already carried can still be used."
                    : "Game rule switch from " + variable.key() + ".";
            features.put(variable.key(), new DebugFeature(variable.key(), title, Material.REDSTONE_TORCH,
                    description, variable.defaultValue() != 0.0D));
        }
        if (game == ArcadePortGame.BOMB_LOBBERS) {
            features.put("double-jump", new DebugFeature("double-jump", "Double jump", Material.FEATHER,
                    "Allow authorised testers to use the normal grounded double jump.", true));
            features.put("pitch-adjustment", new DebugFeature("pitch-adjustment", "TNT pitch control", Material.LEVER,
                    "Allow Pitcher testers to adjust TNT throw power.", true));
            features.put("block-wall", new DebugFeature("block-wall", "Waller block wall", Material.STONE_BRICKS,
                    "Allow Waller testers to place the normal cooldown wall.", true));
        }
        if (game == ArcadePortGame.CASTLE_ASSAULT) {
            features.put("objective-tnt", new DebugFeature("objective-tnt", "Objective TNT", Material.TNT,
                    "Allow the tester to collect new Objective TNT or spawn it for testing; carried TNT resolves normally.", true));
            features.put("crystal-damage", new DebugFeature("crystal-damage", "Sentry crystal damage", Material.END_CRYSTAL,
                    "Allow the tester's kills and Objective TNT to damage sentry crystals.", true));
            features.put("king-damage", new DebugFeature("king-damage", "King damage", Material.ZOMBIE_HEAD,
                    "Allow the tester's attacks and Objective TNT to damage the enemy King.", true));
        }
        if (game == ArcadePortGame.DRAW_MY_THING) {
            features.put("canvas-tools", new DebugFeature("canvas-tools", "Canvas drawing tools", Material.BRUSH,
                    "Allow this tester to paint and use the Pencil, Brush, Line, Square, Circle and Spray tools.", true));
            features.put("palette-selection", new DebugFeature("palette-selection", "Palette selection", Material.PAINTING,
                    "Allow this tester to change their drawing colour from the inventory or map palette.", true));
            features.put("guess-submission", new DebugFeature("guess-submission", "Guess submissions", Material.WRITABLE_BOOK,
                    "Allow this tester to submit guesses through the normal chat answer check.", true));
            features.put("timed-hints", new DebugFeature("timed-hints", "Timed letter hints", Material.PAPER,
                    "Reveal letters on the normal round schedule and play the hint sound.", true));
            features.put("sound-feedback", new DebugFeature("sound-feedback", "Round sound effects", Material.NOTE_BLOCK,
                    "Play the normal drawing, hint and guess feedback sounds.", true));
        }
        if (game == ArcadePortGame.MASTER_BUILDERS) {
            features.put("plot-editing", new DebugFeature("plot-editing", "Plot block editing", Material.OAK_PLANKS,
                    "Allow this tester to place and break blocks inside their assigned plot during Build.", true));
            features.put("review-voting", new DebugFeature("review-voting", "Review voting", Material.LIME_CONCRETE,
                    "Allow this tester to submit ratings through the normal six-level review vote.", true));
        }
        if (game == ArcadePortGame.MILK_THE_COW) {
            features.put("cow-charge", new DebugFeature("cow-charge", "Cow charge", Material.LEATHER_BOOTS,
                    "Allow this tester's sprinting Cow-role charge to damage and knock back Farmers.", true));
            features.put("farmer-double-jump", new DebugFeature("farmer-double-jump", "Farmer double jump", Material.FEATHER,
                    "Allow this tester's one-per-airtime Farmer double jump.", true));
            features.put("herd-spawning", new DebugFeature("herd-spawning", "Cow herd", Material.COW_SPAWN_EGG,
                    "Spawn and move the normal Cow-role herd around the selected Cow.", true));
            features.put("milk-capture", new DebugFeature("milk-capture", "Bucket capture", Material.BUCKET,
                    "Allow this tester to fill a bucket by interacting with a Cow player.", true));
            features.put("milk-scoring", new DebugFeature("milk-scoring", "Milk score", Material.MILK_BUCKET,
                    "Award this tester the normal points when they drink a captured milk bucket.", true));
        }
        if (game == ArcadePortGame.WITHER_ASSAULT) {
            features.put("ability-human-leap", new DebugFeature("ability-human-leap", "Human double jump", Material.FEATHER,
                    "Allow this tester to use the Human team's grounded double jump.", true));
            features.put("human-revival", new DebugFeature("human-revival", "Human revival potions", Material.SPLASH_POTION,
                    "Allow this tester to revive downed Human teammates with a Revival Potion.", true));
            for (String feature : List.of("archer", "medic", "editor", "wither")) {
                String key = "ability-" + feature;
                features.put(key, new DebugFeature(key, "Kit feature: " + humanizeDebugKey(feature), Material.WITHER_SKELETON_SKULL,
                        "Allow this tester's " + humanizeDebugKey(feature) + " kit features and abilities.", true));
            }
        }
        if (game == ArcadePortGame.HOOPS_REMASTERED) {
            features.put("shooting", new DebugFeature("shooting", "Shooting", Material.SNOWBALL,
                    "Allow this tester to release shots, layups and dunks.", true));
            features.put("scoring", new DebugFeature("scoring", "Basket scoring", Material.GOLD_INGOT,
                    "Allow this tester's shots to change the team score when they pass through the hoop.", true));
            features.put("steals", new DebugFeature("steals", "Ball steals", Material.LEATHER_BOOTS,
                    "Allow this tester to steal possession from an opposing ball holder.", true));
        }
        if (game == ArcadePortGame.HEROES_OF_GWEN) {
            for (String hero : List.of("hattori", "bardolf", "devon", "rowena", "anath", "larissa", "dana", "biff", "ivy")) {
                String key = "hero-" + hero + "-abilities";
                features.put(key, new DebugFeature(key, pretty(hero) + " abilities", Material.BLAZE_POWDER,
                        "Allow the tester to activate " + pretty(hero) + " skills; other players and passive effects are unchanged.", true));
            }
        }
        features.put("achievement-preview-only", new DebugFeature("achievement-preview-only",
                "Achievement preview only", Material.KNOWLEDGE_BOOK,
                "Show real achievement condition results without saving unlocks.", false));
        if (game == ArcadePortGame.BACON_BRAWL) {
            features.put("unlimited-abilities", new DebugFeature("unlimited-abilities", "Unlimited abilities",
                    Material.FIREWORK_ROCKET, "Use full-charge Bacon Brawl abilities without consuming meter.", false));
            for (String kit : List.of("mama-piggles", "chris-p-bacon", "pig", "brutus-bacon",
                    "el-muchacho-pigo")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Ability: " + humanizeDebugKey(kit),
                        Material.IRON_AXE, "Allow this kit's ability to activate for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.SUPER_SPLOOR) {
            features.put("unlimited-specials", new DebugFeature("unlimited-specials", "Unlimited Specials",
                    Material.FIREWORK_ROCKET,
                    "Keep the kit special fully charged, except while its active effect is running.", false));
            for (String kit : List.of("sprayer", "roller", "charger", "lobber", "dualies")) {
                String key = "special-" + kit;
                features.put(key, new DebugFeature(key, "Special: " + humanizeDebugKey(kit),
                        Material.FIREWORK_STAR, "Allow this kit's Special to activate for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.DRAGON_ESCAPE) {
            features.put("finish-detection", new DebugFeature("finish-detection", "Course finish detection",
                    Material.DRAGON_EGG, "Allow this tester to finish by reaching the map's authored beacon.", true));
            for (String kit : List.of("jumper", "warper", "digger", "disruptor")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Ability: " + humanizeDebugKey(kit),
                        Material.ENDER_PEARL, "Allow this kit's ability for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.DRAGONS) {
            for (String kit : List.of("coward", "frost", "glider", "tamer", "pyrotechnic", "marksman")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Ability: " + humanizeDebugKey(kit),
                        Material.DRAGON_BREATH, "Allow this kit's ability for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.ONE_IN_THE_QUIVER) {
            for (String kit : List.of("brawler", "enchanter", "slam-shot", "ninja")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Ability: " + humanizeDebugKey(kit),
                        Material.ARROW, "Allow this kit's ability or kill perk for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.PAINTBALL) {
            String key = "ignore-reload";
            features.put(key, new DebugFeature(key, "Ignore reload and heat", Material.SNOWBALL,
                    "Let your weapons fire continuously without reload waits or Machine Gun heat for testing.", false));
            for (String kit : List.of("rifle", "sniper", "shotgun", "machine-gun")) {
                String weaponKey = "weapon-" + kit;
                features.put(weaponKey, new DebugFeature(weaponKey, "Weapon: " + humanizeDebugKey(kit), Material.ENDER_PEARL,
                        "Allow this tester to fire the selected " + humanizeDebugKey(kit) + " weapon.", true));
            }
        }
        if (game == ArcadePortGame.SPLEEF) {
            for (String kit : List.of("snowballer", "archer", "brawler", "gust")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Kit feature: " + humanizeDebugKey(kit),
                        Material.SNOWBALL, "Enable this kit's special supplies and abilities for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.RUNNER) {
            features.put("runner-trail", new DebugFeature("runner-trail", "Runner block trail", Material.LIME_WOOL,
                    "Let this runner paint visited blocks with their trail; trail blocks disappear after the normal delay.", true));
            for (String kit : List.of("leaper", "archer", "frosty", "switcher")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Ability: " + humanizeDebugKey(kit),
                        Material.ENDER_EYE, "Allow this kit's movement, attack or swap ability for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.DEATH_TAG) {
            for (String kit : List.of("basher", "archer", "traitor", "turtle", "trickster")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Kit feature: " + humanizeDebugKey(kit),
                        Material.IRON_AXE,
                        "Allow this kit's combat, movement or passive effect for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.EVOLUTION) {
            for (String form : List.of("golem", "blaze", "creeper", "slime", "chicken", "quick-evolver")) {
                String key = "ability-" + form;
                features.put(key, new DebugFeature(key, "Form feature: " + humanizeDebugKey(form),
                        Material.DRAGON_EGG,
                        "Allow this Evolution form's ability or progression perk for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.SHEEP_QUEST) {
            for (String kit : List.of("beserker", "archer", "shepherd")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Kit feature: " + humanizeDebugKey(kit),
                        Material.SADDLE,
                        "Allow this kit's leap, arrows or carry effects for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.TURF_WARS) {
            features.put("build-editing", new DebugFeature("build-editing", "Turf Wars building", Material.RED_WOOL,
                    "Allow this tester to place and break Turf Wars blocks during the normal build phase.", true));
            features.put("combat-damage", new DebugFeature("combat-damage", "Turf Wars combat damage", Material.BOW,
                    "Allow this tester to deal and receive Turf Wars damage.", true));
            for (String kit : List.of("infiltrator", "shredder", "fortifier")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Kit feature: " + humanizeDebugKey(kit),
                        Material.BOW,
                        "Allow this kit's territory, barrage or fortification feature for authorised testers.", true));
            }
        }
        if (game == ArcadePortGame.MICRO_BATTLES) {
            for (String kit : List.of("archer", "fighter", "dasher")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Kit feature: " + humanizeDebugKey(kit),
                        Material.IRON_SWORD,
                        "Allow this kit's arrows, damage resistance or double jump for authorised testers.", true));
            }
            features.put("blocks-place", new DebugFeature("blocks-place", "Block placing", Material.COBBLESTONE,
                    "Allow authorised testers to place Micro Battles blocks.", true));
            features.put("blocks-break", new DebugFeature("blocks-break", "Block breaking", Material.IRON_PICKAXE,
                    "Allow authorised testers to break Micro Battles blocks.", true));
        }
        if (game == ArcadePortGame.SKY_WARS) {
            for (String kit : List.of("ice", "fire", "air", "metal", "earth")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Kit feature: " + humanizeDebugKey(kit),
                        Material.ENDER_EYE,
                        "Allow this tester's " + humanizeDebugKey(kit) + " kit ability and passive effects.", true));
            }
        }
        if (game == ArcadePortGame.SNAKE) {
            for (String kit : List.of("speed", "reverse", "invulnerable")) {
                String key = "ability-" + kit;
                features.put(key, new DebugFeature(key, "Kit ability: " + humanizeDebugKey(kit),
                        Material.MAGMA_CREAM,
                        "Allow this tester to use the selected Snake kit's normal ability.", true));
            }
            features.put("tail-collisions", new DebugFeature("tail-collisions", "Tail collisions", Material.SHEARS,
                    "Enable normal Snake head-to-tail eliminations for this debug session.", true));
            features.put("magma-hazards", new DebugFeature("magma-hazards", "Magma Cube hazards", Material.MAGMA_CREAM,
                    "Spawn and steer the normal Magma Cube item hazards for this debug session.", true));
            features.put("lava-elimination", new DebugFeature("lava-elimination", "Lava elimination", Material.LAVA_BUCKET,
                    "Eliminate this tester on lava contact using the normal Snake rule.", true));
        }
        if (game == ArcadePortGame.SNEAKY_ASSASSINS) {
            for (String feature : List.of("disguises", "power-ups", "global-reveal", "npc-patrol",
                    "town-npcs", "smoke-bomb", "revealer", "briber", "master-compass")) {
                String key = switch (feature) {
                    case "smoke-bomb", "revealer", "briber" -> "ability-" + feature;
                    default -> feature;
                };
                features.put(key, new DebugFeature(key, "Feature: " + humanizeDebugKey(feature),
                        Material.SKELETON_SKULL,
                        "town-npcs".equals(feature)
                                ? "Spawn the source town NPCs when the next test round starts."
                                : "Enable this Sneaky Assassins runtime feature in the active test session.", true));
            }
        }
        return List.copyOf(features.values());
    }

    private String humanizeDebugKey(String key) {
        String[] words = key.split("-");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(' ');
            if (!word.isEmpty()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private boolean debugFeatureAppliesImmediately(ArcadePortGame game, String key) {
        if (GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(game, key))) return true;
        return "all-kits".equals(key) || "invulnerable".equals(key) || "no-fall-damage".equals(key)
                || "no-hunger-loss".equals(key) || "unlimited-abilities".equals(key)
                || "unlimited-specials".equals(key)
                || (game == ArcadePortGame.BACON_BRAWL && key.startsWith("ability-"))
                || (game == ArcadePortGame.BOMB_LOBBERS
                        && ("double-jump".equals(key) || "pitch-adjustment".equals(key) || "block-wall".equals(key)))
                || (game == ArcadePortGame.CASTLE_ASSAULT
                        && ("objective-tnt".equals(key) || "crystal-damage".equals(key) || "king-damage".equals(key)))
                || (game == ArcadePortGame.HEROES_OF_GWEN && key.startsWith("hero-") && key.endsWith("-abilities"))
                || (game == ArcadePortGame.DRAW_MY_THING
                        && ("canvas-tools".equals(key) || "palette-selection".equals(key)
                                || "guess-submission".equals(key) || "timed-hints".equals(key)
                                || "sound-feedback".equals(key)))
                || (game == ArcadePortGame.MASTER_BUILDERS
                        && ("plot-editing".equals(key) || "review-voting".equals(key)))
                || (game == ArcadePortGame.MILK_THE_COW && Set.of("cow-charge", "farmer-double-jump",
                        "herd-spawning", "milk-capture", "milk-scoring").contains(key))
                || (game == ArcadePortGame.WITHER_ASSAULT
                        && ("ability-human-leap".equals(key) || "human-revival".equals(key) || key.startsWith("ability-")))
                || (game == ArcadePortGame.HOOPS_REMASTERED
                        && ("shooting".equals(key) || "scoring".equals(key) || "steals".equals(key)))
                || (game == ArcadePortGame.DRAGON_ESCAPE && "finish-detection".equals(key))
                || (game == ArcadePortGame.DRAGON_ESCAPE && key.startsWith("ability-"))
                || (game == ArcadePortGame.RUNNER && "runner-trail".equals(key))
                || (game == ArcadePortGame.DRAGONS && key.startsWith("ability-"))
                || (game == ArcadePortGame.ONE_IN_THE_QUIVER && key.startsWith("ability-"))
                || (game == ArcadePortGame.PAINTBALL
                        && ("ignore-reload".equals(key) || key.startsWith("weapon-")))
                || (game == ArcadePortGame.SPLEEF && key.startsWith("ability-"))
                || (game == ArcadePortGame.RUNNER && key.startsWith("ability-"))
                || (game == ArcadePortGame.DEATH_TAG && key.startsWith("ability-"))
                || (game == ArcadePortGame.EVOLUTION && key.startsWith("ability-"))
                || (game == ArcadePortGame.SHEEP_QUEST && key.startsWith("ability-"))
                || (game == ArcadePortGame.TURF_WARS && key.startsWith("ability-"))
                || (game == ArcadePortGame.MICRO_BATTLES
                        && (key.startsWith("ability-") || key.startsWith("blocks-")))
                || (game == ArcadePortGame.SKY_WARS && key.startsWith("ability-"))
                || (game == ArcadePortGame.SNAKE
                        && (key.startsWith("ability-") || "tail-collisions".equals(key)
                                || "magma-hazards".equals(key) || "lava-elimination".equals(key)))
                || (game == ArcadePortGame.SNEAKY_ASSASSINS
                        && (Set.of("disguises", "power-ups", "global-reveal", "npc-patrol", "master-compass")
                                .contains(key) || key.startsWith("ability-")))
                || (game == ArcadePortGame.SUPER_SPLOOR && key.startsWith("special-"))
                || "DRAGONS_SPAWNING_ENABLED".equals(key)
                || "DRAGONS_AUTONOMOUS_MOVEMENT_ENABLED".equals(key)
                || "QUIVER_LEAPER_DOUBLE_JUMP_ENABLED".equals(key)
                || "SPLEEF_HUNGER_DRAIN_ENABLED".equals(key)
                || "BOMB_LOBBERS_POWERUP_SPAWNING_ENABLED".equals(key)
                || "achievement-preview-only".equals(key);
    }

    private void openDebugVariables(Player player, int requestedPage) {
        ArcadePortGame game = debugGame(player);
        List<GameDebugVariableCatalog.Variable> variables = manager.debugVariables(game).stream()
                .filter(variable -> variable.kind() != GameDebugVariableCatalog.Kind.BOOLEAN).toList();
        int pageCount = Math.max(1, (variables.size() + 44) / 45);
        int page = Math.max(0, Math.min(pageCount - 1, requestedPage));
        debugVariablePages.put(player.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(null, 54, DEBUG_VARIABLES_TITLE + " " + (page + 1));
        String profile = manager.debugProfiles().selected(player.getUniqueId(), game);
        inventory.setItem(4, item(Material.COMPARATOR, ChatColor.AQUA + game.displayName() + " Variables",
                ChatColor.GRAY + "Profile: " + ChatColor.WHITE + profile,
                ChatColor.YELLOW + "Rule edits apply at the next round start."));
        int start = page * 45;
        for (int i = start; i < Math.min(variables.size(), start + 45); i++) {
            GameDebugVariableCatalog.Variable variable = variables.get(i);
            Double configured = manager.debugVariable(player.getUniqueId(), game, variable.key());
            double current = configured == null ? variable.defaultValue() : configured;
            inventory.setItem(i - start, debugActionButton(Material.COMPARATOR,
                    ChatColor.YELLOW + variable.label(), "debug-variable:" + variable.key(),
                    ChatColor.GRAY + "Current: " + ChatColor.WHITE + formatDebugValue(current)
                            + ChatColor.GRAY + " | Default: " + ChatColor.WHITE + formatDebugValue(variable.defaultValue()),
                    ChatColor.GRAY + "Left-click: +" + formatDebugValue(variable.step())
                            + " | Right-click: -" + formatDebugValue(variable.step()),
                    ChatColor.GRAY + "Shift-click: reset this variable"));
        }
        if (page > 0) inventory.setItem(45, testBotButton(Material.ARROW, "Previous page", "debug-variable-page:" + (page - 1)));
        inventory.setItem(49, testBotButton(Material.ARROW, "Back", "debug-back"));
        if (page + 1 < pageCount) inventory.setItem(53, testBotButton(Material.ARROW, "Next page", "debug-variable-page:" + (page + 1)));
        player.openInventory(inventory);
    }

    private String formatDebugValue(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.3f", value);
    }

    private ItemStack debugActionButton(Material material, String title, String action, String... lore) {
        ItemStack button = item(material, title, lore);
        mark(button, action, null);
        return button;
    }

    private void handleDebugVariableClick(Player player, String action, ClickType click) {
        ArcadePortGame game = debugGame(player);
        if (action.startsWith("debug-variable-page:")) {
            openDebugVariables(player, Integer.parseInt(action.substring("debug-variable-page:".length())));
            return;
        }
        if (!action.startsWith("debug-variable:")) return;
        String key = action.substring("debug-variable:".length());
        GameDebugVariableCatalog.Variable variable = manager.debugVariables(game).stream()
                .filter(candidate -> candidate.key().equals(key)).findFirst().orElse(null);
        if (variable == null || variable.kind() == GameDebugVariableCatalog.Kind.BOOLEAN) return;
        if (click.isShiftClick()) {
            manager.debugProfiles().setVariable(player.getUniqueId(), game, key, variable.defaultValue());
        } else {
            Double configured = manager.debugVariable(player.getUniqueId(), game, key);
            double current = configured == null ? variable.defaultValue() : configured;
            double direction = click.isRightClick() ? -1.0D : 1.0D;
            double next = Math.max(variable.minimum(), Math.min(variable.maximum(), current + direction * variable.step()));
            manager.setDebugVariable(player.getUniqueId(), game, key, next);
        }
        openDebugVariables(player, debugVariablePages.getOrDefault(player.getUniqueId(), 0));
    }

    private void handleDebugProfileMenuClick(Player player, String action) {
        UUID id = player.getUniqueId();
        ArcadePortGame game = debugGame(player);
        if (action.startsWith("debug-profile-load:")) {
            String profile = action.substring("debug-profile-load:".length());
            if (manager.debugProfiles().select(id, game, profile)) {
                player.sendMessage(ChatColor.GREEN + "Loaded " + game.displayName() + " profile '" + profile + "'.");
                openDebugMenu(player);
            }
        } else if (action.startsWith("debug-profile-prompt:")) {
            String operation = action.substring("debug-profile-prompt:".length());
            debugPrompts.put(id, new DebugPrompt(game, operation));
            player.closeInventory();
            player.sendMessage("delete".equals(operation)
                    ? ChatColor.RED + "Type DELETE to confirm removal of the selected profile, or CANCEL."
                    : ChatColor.YELLOW + "Enter the new profile name in chat. Type CANCEL to stop.");
        } else if ("debug-profile-save".equals(action)) {
            manager.saveDebugProfile(id, game);
            player.sendMessage(ChatColor.GREEN + "Debug profile saved.");
            openDebugProfiles(player);
        } else if ("debug-back".equals(action)) {
            openDebugMenu(player);
        }
    }

    @EventHandler
    public void onDebugProfileChat(AsyncPlayerChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        DebugPrompt prompt = debugPrompts.get(id);
        if (prompt == null) return;
        event.setCancelled(true);
        String input = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            debugPrompts.remove(id);
            Player player = event.getPlayer();
            if (!player.isOnline() || !plugin.hasAdminAccess(player)) return;
            if ("CANCEL".equalsIgnoreCase(input)) {
                player.sendMessage(ChatColor.GRAY + "Profile action cancelled.");
                return;
            }
            String result = switch (prompt.operation()) {
                case "create" -> manager.debugProfiles().create(id, prompt.game(), input);
                case "duplicate" -> manager.debugProfiles().duplicate(id, prompt.game(), input);
                case "rename" -> manager.debugProfiles().renameSelected(id, prompt.game(), input);
                case "delete" -> "DELETE".equalsIgnoreCase(input)
                        && manager.debugProfiles().delete(id, prompt.game(), manager.debugProfiles().selected(id, prompt.game()))
                        ? "deleted" : null;
                default -> null;
            };
            player.sendMessage(result == null ? ChatColor.RED + "Profile action failed; check the name, profile limit or default-profile restriction."
                    : ChatColor.GREEN + "Profile action complete: " + result + ".");
            if (lobby.isLobbyWorld(player.getWorld())) openDebugProfiles(player);
        });
    }

    @EventHandler
    public void onDebugTesterQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        debugContextGames.remove(id);
        debugPrompts.remove(id);
        debugVariablePages.remove(id);
    }

    private void handleDebugProfileCommand(Player player, String[] args) {
        ArcadePortGame game = debugGame(player);
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "/arcadeport debug profile <create|duplicate|rename|delete|load|save|reset> [name]");
            return;
        }
        String operation = args[1].toLowerCase(Locale.ROOT);
        UUID id = player.getUniqueId();
        String name = args.length > 2 ? String.join("-", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";
        boolean ok = switch (operation) {
            case "create" -> manager.debugProfiles().create(id, game, name) != null;
            case "duplicate" -> manager.debugProfiles().duplicate(id, game, name) != null;
            case "rename" -> manager.debugProfiles().renameSelected(id, game, name) != null;
            case "delete" -> manager.debugProfiles().delete(id, game, manager.debugProfiles().selected(id, game));
            case "load" -> manager.debugProfiles().select(id, game, GameDebugProfiles.normalizeProfile(name));
            case "save" -> { manager.saveDebugProfile(id, game); yield true; }
            case "reset" -> { manager.resetDebugProfile(id, game); yield true; }
            default -> false;
        };
        player.sendMessage(ok ? ChatColor.GREEN + "Debug profile updated." : ChatColor.RED + "Profile action failed. Check the name and selected game.");
        openDebugProfiles(player);
    }

    private void openTestBotSetup(Player player, ArcadePortGame game) {
        testBotMenus.put(player.getUniqueId(), new TestBotMenuState(TestBotMenuPage.SETUP, game, -1));
        Inventory inventory = Bukkit.createInventory(null, 54, TEST_BOTS_TITLE);
        int count = manager.configuredTestBotCount(game);
        inventory.setItem(4, item(Material.PLAYER_HEAD, ChatColor.AQUA + game.displayName() + " bots",
                ChatColor.GRAY + "The count is capped by the game's 16-player limit."));
        inventory.setItem(11, testBotButton(Material.RED_CONCRETE, "Fewer bots", "bot-count:-1"));
        inventory.setItem(13, item(Material.IRON_GOLEM_SPAWN_EGG, ChatColor.YELLOW + "Bots to load: " + count,
                ChatColor.GRAY + "Configured for " + game.displayName() + ".",
                ChatColor.DARK_GRAY + "Human players reduce the available slots."));
        inventory.setItem(15, testBotButton(Material.LIME_CONCRETE, "More bots", "bot-count:+1"));
        for (int index = 0; index < ArcadeTestPlayerManager.TARGET_ROSTER_SIZE; index++) {
            String kit = manager.configuredTestBotKit(game, index);
            int team = manager.configuredTestBotTeam(game, index);
            String kitName = kit.isBlank() ? "Default kit" : kit.replace('-', ' ');
            Material icon = configuredKitIcon(game, kit);
            ItemStack bot = item(icon, ChatColor.YELLOW + "Bot " + (index + 1),
                    ChatColor.GRAY + "Kit: " + kitName,
                    ChatColor.GRAY + "Team: " + botTeamName(game, team),
                    ChatColor.GREEN + "Click to configure kit, team and game cosmetics.");
            mark(bot, "bot-slot:" + index, null);
            inventory.setItem(botGridSlot(index), bot);
        }
        inventory.setItem(49, testBotButton(Material.BARRIER, "Close", "bot-close"));
        player.openInventory(inventory);
    }

    private void openTestBotLoadout(Player player, ArcadePortGame game, int botIndex) {
        testBotMenus.put(player.getUniqueId(), new TestBotMenuState(TestBotMenuPage.LOADOUT, game, botIndex));
        Inventory inventory = Bukkit.createInventory(null, 27, TEST_BOT_LOADOUT_TITLE);
        String kit = manager.configuredTestBotKit(game, botIndex);
        inventory.setItem(11, testBotButton(configuredKitIcon(game, kit), "Choose Kit",
                kit.isBlank() ? "Current: game default" : "Current: " + kit.replace('-', ' '), "bot-kits"));
        int team = manager.configuredTestBotTeam(game, botIndex);
        inventory.setItem(15, testBotButton(Material.WHITE_BANNER, "Team: " + botTeamName(game, team),
                "Click to cycle through teams; Auto balances the bot.", "bot-team"));
        inventory.setItem(13, testBotButton(Material.FIREWORK_STAR, "Game Cosmetic Style",
                "Choose this bot's game-specific cosmetic options.", "bot-cosmetics"));
        inventory.setItem(22, testBotButton(Material.ARROW, "Back to Bots", "bot-back"));
        player.openInventory(inventory);
    }

    private void openTestBotKits(Player player, ArcadePortGame game, int botIndex) {
        testBotMenus.put(player.getUniqueId(), new TestBotMenuState(TestBotMenuPage.KITS, game, botIndex));
        Inventory inventory = Bukkit.createInventory(null, 54, TEST_BOT_KITS_TITLE);
        List<ArcadeLobbyKitCatalogue.KitChoice> choices = ArcadeLobbyKitCatalogue.forGame(game, List.of());
        int[] slots = menuSlots();
        for (int index = 0; index < choices.size() && index < slots.length; index++) {
            ArcadeLobbyKitCatalogue.KitChoice choice = choices.get(index);
            ItemStack item = item(choice.icon, ChatColor.YELLOW + choice.name,
                    ChatColor.GRAY + "Assign to Bot " + (botIndex + 1) + " for " + game.displayName() + ".",
                    ChatColor.GREEN + "Click to select.");
            mark(item, "bot-kit:" + choice.id, null);
            inventory.setItem(slots[index], item);
        }
        if (choices.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, ChatColor.RED + "No kit station for this game"));
        }
        inventory.setItem(49, testBotButton(Material.ARROW, "Back", "bot-loadout"));
        player.openInventory(inventory);
    }

    private void openTestBotCosmetics(Player player, ArcadePortGame game, int botIndex) {
        testBotMenus.put(player.getUniqueId(), new TestBotMenuState(TestBotMenuPage.COSMETICS, game, botIndex));
        Inventory inventory = Bukkit.createInventory(null, 54, TEST_BOT_COSMETICS_TITLE);
        List<TestBotCosmetic> choices = testBotCosmetics(game);
        int[] slots = menuSlots();
        for (int index = 0; index < choices.size() && index < slots.length; index++) {
            TestBotCosmetic choice = choices.get(index);
            String selected = manager.configuredTestBotCosmetic(game, botIndex, choice.lineId());
            ItemStack item = item(material(choice.icon()),
                    (choice.optionId().equals(selected) ? ChatColor.GREEN : ChatColor.YELLOW) + choice.optionName(),
                    ChatColor.GRAY + choice.lineTitle(),
                    ChatColor.DARK_GRAY + "Game ability effects only; global cosmetics are unaffected.",
                    choice.optionId().equals(selected) ? ChatColor.GREEN + "Selected" : ChatColor.GREEN + "Click to select.");
            mark(item, "bot-cosmetic:" + choice.lineId() + ":" + choice.optionId(), null);
            inventory.setItem(slots[index], item);
        }
        if (choices.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, ChatColor.RED + "No game cosmetic options are registered"));
        }
        inventory.setItem(49, testBotButton(Material.ARROW, "Back", "bot-loadout"));
        player.openInventory(inventory);
    }

    private void handleTestBotMenuClick(Player player, String title, String action) {
        TestBotMenuState state = testBotMenus.get(player.getUniqueId());
        if (state == null) return;
        ArcadePortGame game = state.game();
        if (game == null) return;
        if ("bot-close".equals(action)) {
            testBotMenus.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        if (state.page() == TestBotMenuPage.SETUP) {
            if ("bot-count:-1".equals(action) || "bot-count:+1".equals(action)) {
                int delta = "bot-count:+1".equals(action) ? 1 : -1;
                manager.setConfiguredTestBotCount(game, manager.configuredTestBotCount(game) + delta);
                openTestBotSetup(player, game);
            } else if (action != null && action.startsWith("bot-slot:")) {
                int index = Integer.parseInt(action.substring("bot-slot:".length()));
                openTestBotLoadout(player, game, index);
            }
            return;
        }
        int botIndex = state.botIndex();
        if (state.page() == TestBotMenuPage.LOADOUT) {
            if ("bot-kits".equals(action)) openTestBotKits(player, game, botIndex);
            else if ("bot-cosmetics".equals(action)) openTestBotCosmetics(player, game, botIndex);
            else if ("bot-team".equals(action)) {
                int teamCount = ArcadePortManager.teamCountForGame(game);
                if (teamCount < 2) {
                    player.sendMessage(ChatColor.YELLOW + game.displayName() + " does not use team assignments.");
                } else {
                    int current = manager.configuredTestBotTeam(game, botIndex);
                    manager.setConfiguredTestBotTeam(game, botIndex, current >= teamCount - 1 ? -1 : current + 1);
                }
                openTestBotLoadout(player, game, botIndex);
            } else if ("bot-back".equals(action)) openTestBotSetup(player, game);
            return;
        }
        if (state.page() == TestBotMenuPage.KITS && action != null && action.startsWith("bot-kit:")) {
            manager.setConfiguredTestBotKit(game, botIndex, action.substring("bot-kit:".length()));
            openTestBotLoadout(player, game, botIndex);
        } else if (state.page() == TestBotMenuPage.COSMETICS && action != null
                && action.startsWith("bot-cosmetic:")) {
            String[] selected = action.substring("bot-cosmetic:".length()).split(":", 2);
            if (selected.length == 2) manager.setConfiguredTestBotCosmetic(game, botIndex, selected[0], selected[1]);
            openTestBotCosmetics(player, game, botIndex);
        } else if ("bot-loadout".equals(action)) {
            openTestBotLoadout(player, game, botIndex);
        }
    }

    private ItemStack testBotButton(Material material, String name, String action) {
        return testBotButton(material, name, "", action);
    }

    private ItemStack testBotButton(Material material, String name, String detail, String action) {
        ItemStack item = detail == null || detail.isBlank()
                ? item(material, ChatColor.YELLOW + name)
                : item(material, ChatColor.YELLOW + name, ChatColor.GRAY + detail);
        mark(item, action, null);
        return item;
    }

    private static int botGridSlot(int index) {
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38};
        return slots[index];
    }

    private static int[] menuSlots() {
        return new int[] {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    }

    private Material configuredKitIcon(ArcadePortGame game, String kitId) {
        for (ArcadeLobbyKitCatalogue.KitChoice choice : ArcadeLobbyKitCatalogue.forGame(game, List.of())) {
            if (choice.id.equalsIgnoreCase(kitId)) return choice.icon;
        }
        return Material.PLAYER_HEAD;
    }

    private String botTeamName(ArcadePortGame game, int selected) {
        int teams = ArcadePortManager.teamCountForGame(game);
        return teams < 2 || selected < 0 || selected >= teams ? "Auto balance" : "Team " + (selected + 1);
    }

    private List<TestBotCosmetic> testBotCosmetics(ArcadePortGame game) {
        org.bukkit.plugin.Plugin cosmetics = Bukkit.getPluginManager().getPlugin("TestplexModernArcade");
        if (cosmetics == null || game == null) return List.of();
        try {
            Object catalog = cosmetics.getClass().getMethod("getCatalog").invoke(cosmetics);
            String gameId = game.isMonsterMaze() ? "monster-maze" : game.id();
            Object definition = catalog.getClass().getMethod("getGame", String.class).invoke(catalog, gameId);
            if (definition == null) return List.of();
            @SuppressWarnings("unchecked")
            List<Object> lines = (List<Object>) definition.getClass().getMethod("getCosmeticLines").invoke(definition);
            List<TestBotCosmetic> choices = new ArrayList<>();
            for (Object line : lines) {
                String lineId = (String) line.getClass().getMethod("getId").invoke(line);
                String lineTitle = (String) line.getClass().getMethod("getTitle").invoke(line);
                String icon = (String) line.getClass().getMethod("getIcon").invoke(line);
                @SuppressWarnings("unchecked")
                List<Object> options = (List<Object>) line.getClass().getMethod("getOptions").invoke(line);
                for (Object option : options) {
                    String optionId = (String) option.getClass().getMethod("getId").invoke(option);
                    String optionName = (String) option.getClass().getMethod("getName").invoke(option);
                    choices.add(new TestBotCosmetic(lineId, lineTitle, optionId, optionName, icon));
                }
            }
            return choices;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().fine("Could not read game cosmetic options for bot setup: " + exception.getMessage());
            return List.of();
        }
    }

    private Material material(String name) {
        Material material = name == null ? null : Material.matchMaterial(name);
        return material == null ? Material.FIREWORK_STAR : material;
    }

    private Bot createBot(String name){String k=key(name);if(k.isBlank()||bots.containsKey(k))return null;Bot b=new Bot(k,name,"porky","pig","el-muchacho-pigo");bots.put(k,b);saveBot(b);spawnBotEntity(b,lobby.spawnLocation().add(bots.size()*1.6D-3.0D,-3.0D,8.0D),false);refreshDisplays();return b;}
    private String key(String n){return n==null?"":n.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_-]","-");}
    private boolean removeBot(String n){Bot b=bots.remove(key(n));if(b==null)return false;if(b.actor!=null&&b.actor.isValid())b.actor.remove();plugin.getConfig().set("test-bots."+b.key,null);plugin.saveConfig();refreshDisplays();return true;}
    private void saveBot(Bot b){String p="test-bots."+b.key+".";plugin.getConfig().set(p+"name",b.name);plugin.getConfig().set(p+"theme",b.theme);plugin.getConfig().set(p+"form",b.form);plugin.getConfig().set(p+"kit",b.kit);plugin.saveConfig();}
    private void replaceBotEntity(Bot b){if(b.actor!=null&&b.actor.isValid())b.actor.remove();Location at=b.actorLocation==null||b.actorLocation.getWorld()!=lobby.ensureWorld()?lobby.spawnLocation().add(0,-3,8):b.actorLocation.clone();spawnBotEntity(b,at,b.inGame&&b.deployedGame!=null&&manager.isActiveGame(at.getWorld(),b.deployedGame));}
    private void spawnBotEntity(Bot b, Location at, boolean inGame) {
        if (at == null || at.getWorld() == null) return;
        LivingEntity actor;
        if (inGame && b.deployedGame == ArcadePortGame.BOMB_LOBBERS) {
            Zombie zombie = at.getWorld().spawn(at, Zombie.class);
            zombie.setBaby(false);
            zombie.setCanPickupItems(false);
            actor = zombie;
            equipBombLobbersBot(zombie, b.team);
        } else if ("pink-sheep".equals(b.form)) {
            Sheep sheep = at.getWorld().spawn(at, Sheep.class);
            sheep.setColor(DyeColor.PINK);
            actor = sheep;
        } else {
            actor = at.getWorld().spawn(at, Pig.class);
        }
        // NPCs stay damageable so Bukkit emits EntityDamageByEntityEvent; this listener
        // cancels vanilla damage and applies the correct game's test-dummy response.
        actor.setInvulnerable(false);
        actor.setRemoveWhenFarAway(false);
        actor.setPersistent(false);
        actor.setSilent(true);
        actor.setCustomNameVisible(true);
        actor.setCollidable(false);
        actor.getPersistentDataContainer().set(botKey, PersistentDataType.STRING, b.key);
        actor.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, b.theme);
        actor.getPersistentDataContainer().set(formKey, PersistentDataType.STRING, b.form);
        if (actor instanceof Mob mob) mob.setAI(false);
        b.actor = actor;
        b.actorLocation = at.clone();
        b.inGame = inGame;
        b.arenaSpawn = at.clone();
        refreshBotAppearance(b);
    }

    private void equipBombLobbersBot(Zombie zombie, int team) {
        var equipment = zombie.getEquipment();
        if (equipment == null) return;
        Color teamColor = team == 1 ? Color.AQUA : Color.RED;
        equipment.setHelmet(new ItemStack(Material.PLAYER_HEAD));
        equipment.setChestplate(dyedBombLobbersArmour(Material.LEATHER_CHESTPLATE, teamColor));
        equipment.setLeggings(dyedBombLobbersArmour(Material.LEATHER_LEGGINGS, teamColor));
        equipment.setBoots(dyedBombLobbersArmour(Material.LEATHER_BOOTS, teamColor));
    }

    private ItemStack dyedBombLobbersArmour(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void refreshBotAppearance(Bot b) {
        if (b.actor == null || !b.actor.isValid()) return;
        if (b.actor instanceof Sheep sheep) sheep.setColor(DyeColor.PINK);
        if (b.actor instanceof Zombie zombie) equipBombLobbersBot(zombie, b.team);
        if (b.actor instanceof LivingEntity living) living.setCollidable(false);
        b.actor.setInvulnerable(false);
        String name = ChatColor.GOLD + b.name + ChatColor.GRAY + " [" + pretty(b.theme) + "]";
        if (b.inGame && b.deployedGame == ArcadePortGame.BOMB_LOBBERS) {
            ChatColor teamColor = b.team == 1 ? ChatColor.AQUA : ChatColor.RED;
            name = teamColor + b.name + ChatColor.GRAY + (b.team == 1 ? " [BLUE BOT]" : " [RED BOT]");
        }
        b.actor.setCustomName(name);
        b.actor.setCustomNameVisible(true);
        b.actor.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, b.theme);
        b.actor.getPersistentDataContainer().set(formKey, PersistentDataType.STRING, b.form);
        b.actor.getPersistentDataContainer().set(botKey, PersistentDataType.STRING, b.key);
    }
    private void openBotCosmetics(Player p,Bot b){
        Inventory inv=Bukkit.createInventory(null,27,BOT_MENU_PREFIX+b.key);int[] slots={10,11,12,13,14,15};
        for(int i=0;i<THEMES.length;i++){String theme=THEMES[i];ItemStack it=item(themeMaterial(theme),(b.theme.equals(theme)?ChatColor.GREEN+"✓ ":ChatColor.WHITE)+pretty(theme),ChatColor.GRAY+"Theme particle preview",b.theme.equals(theme)?ChatColor.GREEN+"Selected":ChatColor.YELLOW+"Click to equip");mark(it,"theme:"+theme,b.key);inv.setItem(slots[i],it);}
        ItemStack form=item(Material.SHEARS,ChatColor.AQUA+"Toggle Bot Form",ChatColor.GRAY+"Current: "+b.form,ChatColor.YELLOW+"Switch Pig / Pink Sheep");mark(form,"form:"+("pig".equals(b.form)?"pink-sheep":"pig"),b.key);inv.setItem(22,form);
        ItemStack close=item(Material.RED_BED,ChatColor.RED+"Close");mark(close,"bot-close",b.key);inv.setItem(26,close);p.openInventory(inv);
    }
    private Material themeMaterial(String theme){return switch(theme){case "inferno"->Material.FIRE_CHARGE;case "frost"->Material.SNOWBALL;case "slime"->Material.SLIME_BALL;case "royal"->Material.GOLDEN_APPLE;case "confetti"->Material.FIREWORK_ROCKET;default->Material.PORKCHOP;};}
    private String pretty(String value){StringBuilder out=new StringBuilder();for(String part:value.split("-")){if(part.isEmpty())continue;if(out.length()>0)out.append(' ');out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));}return out.toString();}

    private void tick(){
        refreshDisplays();
        clearLobbyNpcFire();
        World world = cosmeticPreviewWorld;
        if (world == null || world.getPlayers().isEmpty()) return;
        for (Entity e : new ArrayList<>(showcase)) {
            if (e == null || !e.isValid() || e.getWorld() != world) continue;
            String value = e.getPersistentDataContainer().get(themeKey, PersistentDataType.STRING);
            if (value == null || !value.startsWith("showcase:")) continue;
            String theme = value.substring("showcase:".length());
            e.getWorld().spawnParticle(particle(theme), e.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
        }
        // No persistent or lobby-wide test bots are ticked here. Match players are
        // created by ArcadeTestPlayerManager and destroyed when their round ends.
    }
    private boolean isTaggedLobbyNpc(Entity entity) {
        return entity != null && (entity.getPersistentDataContainer().has(kitNpcKey, PersistentDataType.STRING)
                || entity.getPersistentDataContainer().has(teamNpcKey, PersistentDataType.STRING));
    }

    private void clearLobbyNpcFire() {
        // Search the real world entity set instead of only cached lists. This catches recreated
        // NPCs immediately after a refresh and clears every visual fire tick.
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (!isTaggedLobbyNpc(entity)) continue;
                if (entity instanceof Zombie zombie) zombie.setShouldBurnInDay(false);
                entity.setFireTicks(0);
                entity.setVisualFire(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLobbyNpcCombust(org.bukkit.event.entity.EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (!isTaggedLobbyNpc(entity)) return;
        event.setCancelled(true);
        if (entity instanceof Zombie zombie) zombie.setShouldBurnInDay(false);
        entity.setFireTicks(0);
        entity.setVisualFire(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLobbyNpcEnvironmentalDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getPersistentDataContainer().has(kitNpcKey, PersistentDataType.STRING)
                && !entity.getPersistentDataContainer().has(teamNpcKey, PersistentDataType.STRING)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
            entity.setFireTicks(0);
            entity.setVisualFire(false);
        }
    }

    private void tickBot(Bot b,long now,World lobbyWorld){
        Entity e=b.actor;if(!(e instanceof LivingEntity actor)||!e.isValid()){Location at=b.actorLocation==null?lobby.spawnLocation().add(0,-3,8):b.actorLocation.clone();spawnBotEntity(b,at,b.inGame);return;}
        // Re-assert collisionless, non-AI target state every tick; game logic drives movement/knockback.
        actor.setCollidable(false);
        if (actor instanceof Mob mob) mob.setAI(false);
        if(!b.inGame||b.deployedGame==null||!manager.isActiveGame(e.getWorld(),b.deployedGame)){
            if(e.getWorld()!=lobbyWorld&&b.inGame)returnBotsToLobby();
            return;
        }
        if(b.deployedGame!=ArcadePortGame.BACON_BRAWL){
            e.getWorld().spawnParticle(particle(b.theme),e.getLocation().add(0,1,0),2,0.2,0.2,0.2,0.01);
            return;
        }
        // Let a landed hit's knockback play through instead of having the NPC chase loop
        // overwrite its velocity on the next scheduler tick.
        if (now < b.knockbackUntil) {
            e.getWorld().spawnParticle(particle(b.theme),e.getLocation().add(0,1,0),2,0.2,0.2,0.2,0.01);
            return;
        }
        List<Player> targets=manager.botTargets(e.getWorld(),b.deployedGame);Player nearest=null;double distance=Double.POSITIVE_INFINITY;
        for(Player p:targets){double d=p.getLocation().distanceSquared(e.getLocation());if(d<distance){distance=d;nearest=p;}}
        if(nearest==null)return;
        if(e.getLocation().getY()<e.getWorld().getMinHeight()+8){if(b.arenaSpawn!=null)actor.teleport(b.arenaSpawn);return;}
        if(distance>4){Vector v=nearest.getLocation().toVector().subtract(e.getLocation().toVector()).setY(0);if(v.lengthSquared()>0.01){v.normalize().multiply(0.22);v.setY(actor.isOnGround()?0.15:actor.getVelocity().getY());actor.setVelocity(v);}}
        else if (now >= b.nextAttackAt) {
            boolean landed = manager.applyBaconBrawlBotAttack(e.getLocation(), nearest);
            if (landed) {
                nearest.getWorld().spawnParticle(particle(b.theme),
                        nearest.getLocation().add(0, 1, 0), 8, 0.4, 0.3, 0.4, 0.01);
            }
            b.nextAttackAt = now + 1400L;
        }
        e.getWorld().spawnParticle(particle(b.theme),e.getLocation().add(0,1,0),2,0.2,0.2,0.2,0.01);
    }
    private Particle particle(String theme) {
        String value = theme == null ? "" : theme.toLowerCase(Locale.ROOT);
        if (value.contains("ember") || value.contains("inferno") || value.contains("flame")
                || value.contains("dragon-roar") || value.contains("molten")) return Particle.FLAME;
        if (value.contains("frost") || value.contains("snow") || value.contains("ice")) return Particle.SNOWFLAKE;
        if (value.contains("slime")) return Particle.CRIT;
        if (value.contains("royal") || value.contains("lantern") || value.contains("sky-compass")
                || value.contains("oath")) return Particle.END_ROD;
        if (value.contains("confetti") || value.contains("wool") || value.contains("nature")
                || value.contains("bloom")) return Particle.HAPPY_VILLAGER;
        if (value.contains("spark") || value.contains("wing") || value.contains("fletching")
                || value.contains("starburst")) return Particle.FIREWORK;
        if (value.contains("shockwave") || value.contains("target") || value.contains("bullseye")
                || value.contains("turf-crack")) return Particle.DAMAGE_INDICATOR;
        if (value.contains("soul") || value.contains("void")) return Particle.SOUL;
        if (value.contains("paint") || value.contains("splatter") || value.contains("colourburst")
                || value.contains("stencil")) return Particle.SPLASH;
        if (value.contains("bark") || value.contains("footprint") || value.contains("chalk")
                || value.contains("red-walk") || value.contains("ribbon")) return Particle.COMPOSTER;
        if (value.contains("dragon-eye") || value.contains("ender")) return Particle.PORTAL;
        if (value.contains("cloud")) return Particle.CLOUD;
        if (value.contains("prism") || value.contains("shatter") || value.contains("pixel-crater")) return Particle.CRIT;
        if (value.contains("warning") || value.contains("turtle") || value.contains("pips")) return Particle.ELECTRIC_SPARK;
        if (value.contains("sheep") || value.contains("shepherd")) return Particle.HAPPY_VILLAGER;
        if (value.contains("arena") || value.contains("red-sand")) return Particle.CRIT;
        return Particle.CRIT;
    }

    /** No mob NPCs are valid game targets; all test targets are real Player objects. */
    List<LivingEntity> botTargets(World world, ArcadePortGame game) {
        return List.of();
    }

    /** Compatibility no-op for older call sites; fake-player participants are queued by ArcadePortManager. */
    public void deployBots(ArcadePortGame game, List<Location> spawns) {
        // Intentionally empty. No entity is carried between games.
    }

    public void deployBots(ArcadePortGame game, List<Location> spawns, int bombLobbersTeam) {
        // Intentionally empty. Player-backed test participants use normal session setup.
    }

    public void returnBotsToLobby() {
        // Intentionally empty. ArcadeTestPlayerManager despawns match-scoped players.
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onBotDamage(EntityDamageEvent e){
        Entity entity=e.getEntity();String key=entity.getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
        if(key==null){
            String tag=entity.getPersistentDataContainer().get(themeKey,PersistentDataType.STRING);
            if(entity.getPersistentDataContainer().has(kitNpcKey,PersistentDataType.STRING)
                    ||entity.getPersistentDataContainer().has(teamNpcKey,PersistentDataType.STRING)
                    ||(tag!=null&&(tag.startsWith("showcase:")||"showcase-label".equals(tag))))e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        if(!(entity instanceof LivingEntity actor)||!(e instanceof EntityDamageByEntityEvent be)
                ||!(be.getDamager() instanceof Player attacker))return;
        Bot b=bots.get(key);
        if(b==null||!b.inGame||b.deployedGame==null
                ||!manager.isActiveGame(entity.getWorld(),b.deployedGame))return;
        if (b.deployedGame == ArcadePortGame.BOMB_LOBBERS) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            if (manager.wasRecentBombLobbersThrow(attacker)
                    || (held != null && (held.getType() == Material.TNT || held.getType() == Material.LEVER))) {
                // TNT/kit interactions are handled by the projectile and ability paths.
                return;
            }
            applyBombLobbersBotMeleeHit(b, actor, attacker);
            return;
        }
        if (b.deployedGame == ArcadePortGame.BACON_BRAWL) {
            applyBotHit(b, actor, attacker, e.getDamage());
        } else {
            showBotImpact(b, actor, attacker, ChatColor.YELLOW + "HIT!", ChatColor.GREEN + "Hit bot " + b.name);
        }
    }
    /** Fallback for clients/plugins that suppress the normal entity-damage event for NPC targets. */
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false)
    public void onBotAttackSwing(PlayerAnimationEvent event) {
        if (!"ARM_SWING".equals(event.getAnimationType().name())) return;
        registerBotHitFromSwing(event.getPlayer());
        registerLobbyNpcMenuFromSwing(event.getPlayer());
    }

    /** Left-clicking a kit or queue NPC opens its normal selection menu. */
    private void registerLobbyNpcMenuFromSwing(Player player) {
        if (player == null || !player.isOnline() || !lobby.isLobbyWorld(player.getWorld())) return;
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        var ray = player.getWorld().rayTraceEntities(eye, direction, 4.25D, 0.9D,
                candidate -> candidate instanceof LivingEntity
                        && (candidate.getPersistentDataContainer().has(kitNpcKey, PersistentDataType.STRING)
                        || candidate.getPersistentDataContainer().has(teamNpcKey, PersistentDataType.STRING)));
        if (ray == null || ray.getHitEntity() == null || !player.hasLineOfSight(ray.getHitEntity())) return;
        Entity target = ray.getHitEntity();
        if (target.getPersistentDataContainer().has(kitNpcKey, PersistentDataType.STRING)) {
            openKitMenu(player);
        } else if (target.getPersistentDataContainer().has(teamNpcKey, PersistentDataType.STRING)) {
            openQueueMenu(player);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false)
    public void onBotLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        registerBotHitFromSwing(event.getPlayer());
    }

    private void registerBotHitFromSwing(Player attacker) {
        if (attacker == null || !attacker.isOnline()) return;
        boolean baconMatch = manager.isActiveGame(attacker.getWorld(), ArcadePortGame.BACON_BRAWL);
        boolean bombLobbersMatch = manager.isActiveGame(attacker.getWorld(), ArcadePortGame.BOMB_LOBBERS);
        if (!baconMatch && !bombLobbersMatch) return;
        if (bombLobbersMatch) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            if (held != null && (held.getType() == Material.TNT || held.getType() == Material.LEVER)) return;
        }
        long now = System.currentTimeMillis();
        java.util.UUID id = attacker.getUniqueId();
        if (now - lastBotSwingAtByAttacker.getOrDefault(id, 0L) < 100L
                || now - lastBotHitAtByAttacker.getOrDefault(id, 0L) < 180L) return;

        Location eye = attacker.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        LivingEntity actor = null;
        var ray = attacker.getWorld().rayTraceEntities(eye, direction, 4.25D, 0.9D,
                candidate -> candidate instanceof LivingEntity
                        && candidate.getPersistentDataContainer().has(botKey, PersistentDataType.STRING));
        if (ray != null && ray.getHitEntity() instanceof LivingEntity hit
                && attacker.hasLineOfSight(hit)) actor = hit;

        if (actor == null) {
            double bestAlong = Double.POSITIVE_INFINITY;
            for (Entity candidate : attacker.getWorld().getNearbyEntities(
                    eye, 4.25D, 4.25D, 4.25D,
                    entity -> entity instanceof LivingEntity
                            && entity.getPersistentDataContainer().has(botKey, PersistentDataType.STRING))) {
                if (!(candidate instanceof LivingEntity living) || !attacker.hasLineOfSight(candidate)) continue;
                Vector delta = living.getBoundingBox().getCenter().subtract(eye.toVector());
                double along = delta.dot(direction);
                if (along < 0.0D || along > 4.25D || along >= bestAlong) continue;
                Vector nearest = eye.toVector().add(direction.clone().multiply(along));
                if (living.getBoundingBox().getCenter().distanceSquared(nearest) <= 1.35D * 1.35D) {
                    actor = living;
                    bestAlong = along;
                }
            }
        }
        if (actor == null) return;
        String key = actor.getPersistentDataContainer().get(botKey, PersistentDataType.STRING);
        Bot bot = key == null ? null : bots.get(key);
        if (bot == null || actor.getWorld() != attacker.getWorld()) return;

        lastBotSwingAtByAttacker.put(id, now);
        LivingEntity target = actor;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean stillBaconMatch = baconMatch
                    && manager.isActiveGame(target.getWorld(), ArcadePortGame.BACON_BRAWL);
            boolean stillBombLobbersMatch = bombLobbersMatch
                    && manager.isActiveGame(target.getWorld(), ArcadePortGame.BOMB_LOBBERS);
            if (!attacker.isOnline() || !target.isValid()
                    || target.getWorld() != attacker.getWorld()
                    || (!stillBaconMatch && !stillBombLobbersMatch)
                    || System.currentTimeMillis() - lastBotHitAtByAttacker.getOrDefault(id, 0L) < 250L
                    || attacker.getLocation().distanceSquared(target.getLocation()) > 20.25D
                    || !attacker.hasLineOfSight(target)) return;
            if (bot.deployedGame == ArcadePortGame.BOMB_LOBBERS && stillBombLobbersMatch) {
                applyBombLobbersBotMeleeHit(bot, target, attacker);
            } else if (bot.deployedGame == ArcadePortGame.BACON_BRAWL && stillBaconMatch) {
                applyBotHit(bot, target, attacker, 2.0D);
            }
        }, 1L);
    }

    private void applyBombLobbersBotMeleeHit(Bot bot, LivingEntity actor, Player attacker) {
        if (bot == null || actor == null || attacker == null
                || !manager.isBombLobbersEnemyBot(attacker, actor)) {
            if (attacker != null && attacker.isOnline()) {
                attacker.sendActionBar(ChatColor.RED + "That bot is on your team.");
            }
            return;
        }
        long now = System.currentTimeMillis();
        UUID attackerId = attacker.getUniqueId();
        if (now - lastBotHitAtByAttacker.getOrDefault(attackerId, 0L) < 180L) return;
        Vector away = actor.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0.0D);
        if (away.lengthSquared() < 0.001D) away = attacker.getLocation().getDirection().setY(0.0D).multiply(-1.0D);
        if (away.lengthSquared() < 0.001D) away = new Vector(0.0D, 0.0D, 1.0D);
        actor.setNoDamageTicks(0);
        actor.setVelocity(away.normalize().multiply(0.72D).setY(0.32D));
        bot.knockbackUntil = now + 450L;
        lastBotHitAtByAttacker.put(attackerId, now);
        showBotImpact(bot, actor, attacker, ChatColor.YELLOW + "HIT!",
                ChatColor.GREEN + "Hit opposing Bomb Lobbers bot " + bot.name);
    }

    private void applyBotHit(Bot bot, LivingEntity actor, Player attacker, double rawDamage) {
        long now = System.currentTimeMillis();
        java.util.UUID attackerId = attacker.getUniqueId();
        // Bukkit damage and the arm-swing fallback can describe the same attack packet.
        if (now - lastBotHitAtByAttacker.getOrDefault(attackerId, 0L) < 180L) return;

        actor.setCollidable(false);
        actor.setNoDamageTicks(0);
        bot.knockbackUntil = now + 800L;
        ArcadePortManager.BaconBrawlBotHitResult hit =
                manager.applyBaconBrawlBotHit(attacker, actor, bot.name, bot.kit, rawDamage);
        if (hit.meter() < 0) return;
        lastBotHitAtByAttacker.put(attackerId, now);

        boolean critical = hit.timingMultiplier() >= 1.5D;
        String flashy = ChatColor.GOLD + "✦ " + ChatColor.YELLOW + ChatColor.BOLD
                + "FLASHY CRITICAL!" + ChatColor.GOLD + " ✦";
        if (critical) {
            attacker.sendActionBar(flashy + ChatColor.WHITE + "  Knockback x"
                    + String.format(Locale.ROOT, "%.2f", hit.timingMultiplier()));
            attacker.sendTitle("", flashy, 0, 18, 5);
            actor.getWorld().spawnParticle(Particle.FIREWORK, actor.getLocation().add(0, 1, 0),
                    18, 0.45, 0.5, 0.45, 0.06);
        }
        // Every hit has hurt animation, sound and impact particles; only critical hits get
        // the explicit flashy label, so normal/early/late hits aren't mislabeled as misses.
        showBotImpact(bot, actor, attacker, critical ? flashy : null, null);
    }

    private void showBotImpact(Bot bot, LivingEntity actor, Player attacker, String visibleLabel, String actionbar) {
        actor.setCollidable(false);
        actor.playEffect(EntityEffect.HURT);
        Location impact = actor.getLocation().clone().add(0, 1, 0);
        actor.getWorld().spawnParticle(Particle.CRIT, impact, 8, 0.28, 0.35, 0.28, 0.04);
        actor.getWorld().spawnParticle(particle(bot.theme), impact, 12, 0.4, 0.4, 0.4, 0.02);
        Sound hurt = actor instanceof Pig ? Sound.ENTITY_PIG_HURT
                : actor instanceof Sheep ? Sound.ENTITY_SHEEP_HURT
                : actor instanceof Zombie ? Sound.ENTITY_ZOMBIE_HURT : Sound.ENTITY_PLAYER_HURT;
        actor.getWorld().playSound(actor.getLocation(), hurt, 1.0F, 1.0F);
        if (attacker != null && attacker.isOnline() && actionbar != null && !actionbar.isBlank()) {
            attacker.sendActionBar(actionbar);
        }
        if (visibleLabel != null && !visibleLabel.isBlank()) {
            final ArmorStand label = actor.getWorld().spawn(actor.getLocation().clone().add(0, 1.8D, 0), ArmorStand.class);
            label.setVisible(false); label.setSmall(true); label.setGravity(false); label.setBasePlate(false);
            label.setArms(false); label.setMarker(true); label.setInvulnerable(true); label.setCanPickupItems(false);
            label.setCustomName(visibleLabel); label.setCustomNameVisible(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (label.isValid() && !label.isDead()) label.remove();
            }, 16L);
        }
    }

    public boolean isBombLobbersEnemyBot(LivingEntity target, int attackingTeam) {
        if (target == null || !target.isValid()) return false;
        String botId = target.getPersistentDataContainer().get(botKey, PersistentDataType.STRING);
        Bot bot = botId == null ? null : bots.get(botId);
        return bot != null && bot.inGame && bot.deployedGame == ArcadePortGame.BOMB_LOBBERS
                && bot.team >= 0 && ArcadePortRules.areBombLobbersEnemies(attackingTeam, bot.team);
    }

    /** Called by the TNT sweep when a Bomb Lobbers test bot is struck directly. */
    public void showBombLobbersBotDirectHit(LivingEntity target, Player thrower, Location bombLocation) {
        showBombLobbersBotDirectHit(target, thrower, bombLocation, false);
    }

    public void showBombLobbersBotDirectHit(LivingEntity target, Player thrower,
            Location bombLocation, boolean fiery) {
        if (target == null || !target.isValid()) return;
        String key = target.getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
        Bot bot = key == null ? null : bots.get(key);
        if (bot == null) return;
        Vector away = bombLocation == null ? target.getLocation().toVector()
                .subtract(thrower == null ? target.getLocation().clone().subtract(0,0,1).toVector() : thrower.getLocation().toVector())
                : target.getLocation().toVector().subtract(bombLocation.toVector());
        if (away.lengthSquared()<0.01D) away=new Vector(0,0,1);
        target.setVelocity(away.normalize().multiply(0.8D).setY(0.35D));
        if (fiery) target.setFireTicks(100);
        if (thrower != null && thrower.isOnline()) {
            showBotImpact(bot,target,thrower,ChatColor.GOLD+"DIRECT HIT!",
                    ChatColor.GREEN+"DIRECT HIT!"+ChatColor.GRAY+" | "+bot.name);
        } else {
            showBotImpact(bot,target,null,ChatColor.GOLD+"DIRECT HIT!", "");
        }
    }

    /** Apply a Bomb Lobbers blast impulse and impact feedback to an opposing test bot. */
    public void showBombLobbersBotBlastHit(LivingEntity target, Location blast, double impact) {
        showBombLobbersBotBlastHit(target, blast, impact, false);
    }

    public void showBombLobbersBotBlastHit(LivingEntity target, Location blast, double impact, boolean fiery) {
        if (target == null || !target.isValid() || blast == null
                || target.getWorld() != blast.getWorld() || impact <= 0.0D) return;
        String key = target.getPersistentDataContainer().get(botKey, PersistentDataType.STRING);
        Bot bot = key == null ? null : bots.get(key);
        if (bot == null || !bot.inGame || bot.deployedGame != ArcadePortGame.BOMB_LOBBERS || bot.team < 0) return;
        Vector away = target.getEyeLocation().toVector().subtract(blast.toVector());
        target.setVelocity(ArcadePortRules.bombLobbersExplosionVelocity(target.getVelocity(), away, impact));
        if (fiery) target.setFireTicks(100);
        showBotImpact(bot, target, null, null, null);
    }

    public void shutdown(){if(task!=null)task.cancel();returnBotsToLobby();for(Entity e:new ArrayList<>(showcase))if(e!=null&&e.isValid())e.remove();for(Entity e:new ArrayList<>(kitNpcs))if(e!=null&&e.isValid())e.remove();for (Entity entity : new ArrayList<>(teamNpcs)) if (entity != null && entity.isValid()) entity.remove(); teamNpcs.clear();}

    private final class Bot{
        final String key,name;String theme,form,kit;Entity actor;Location actorLocation,arenaSpawn;boolean inGame;ArcadePortGame deployedGame;int team=-1;long nextAttackAt,knockbackUntil;
        Bot(String key,String name,String theme,String form,String kit){this.key=key;this.name=name;this.theme=List.of(THEMES).contains(theme)?theme:"porky";this.form="pink-sheep".equals(form)?"pink-sheep":"pig";this.kit=ArcadePortRules.normalizeBaconBrawlKit(kit)==null?"el-muchacho-pigo":kit;}
    }
}
