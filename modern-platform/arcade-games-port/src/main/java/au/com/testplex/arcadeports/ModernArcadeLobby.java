package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Paper-native port of the isolated 1.8 Testplex legacy lobby layout.
 *
 * The lobby uses its own void world so the test server's normal "world" is not
 * erased. The platform bounds, Y level, spawn, hologram datapoints and the
 * kit/team display coordinates match LegacyGameLobbyManager/TestLobbyPlaneBuilder.
 */
public final class ModernArcadeLobby implements Listener {
    public static final String WORLD_NAME = "testplex_arcade_lobby_v1";
    public static final int PLANE_MIN = -50;
    public static final int PLANE_MAX_EXCLUSIVE = 50;
    public static final int FLOOR_Y = 100;
    public static final int HOTBAR_COSMETICS_SLOT = 4;
    public static final int HOTBAR_GAME_PREFERENCES_SLOT = 6;
    public static final int HOTBAR_GAME_PANEL_SLOT = 8;

    public static final double SPAWN_X = 0.0D;
    public static final double SPAWN_Y = 104.0D;
    public static final double SPAWN_Z = 0.0D;

    public static final double MISSIONS_X = -3.5D;
    public static final double MISSIONS_Y = 102.0D;
    public static final double MISSIONS_Z = 3.5D;
    public static final double GAME_TEXT_Z = 44.0D;
    public static final double KIT_TEXT_X = -40.0D;
    public static final double KIT_TEXT_Y = 120.0D;
    public static final double TEAM_TEXT_X = 40.0D;
    public static final double TEAM_TEXT_Y = 120.0D;
    public static final double ADV_TEXT_Z = -44.0D;
    public static final double ADV_TEXT_Y = 140.0D;
    public static final double AMP_STAND_Z = -15.0D;
    public static final double KIT_DISPLAY_X = -17.0D;
    public static final double TEAM_DISPLAY_X = 18.0D;

    private static final String LOBBY_MARKER = "TESTPLEX_LEGACY_LOBBY_V1";
    private final JavaPlugin plugin;
    private final ModernLobbyScoreboard lobbyScoreboard;
    private final NamespacedKey displayKey;
    private final NamespacedKey hotbarActionKey;
    private final NamespacedKey legacyPanelItemKey;
    private World lobbyWorld;

    public ModernArcadeLobby(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lobbyScoreboard = new ModernLobbyScoreboard(plugin);
        this.displayKey = new NamespacedKey(plugin, "legacy-lobby-datapoint");
        this.hotbarActionKey = new NamespacedKey(plugin, "lobby-hotbar-action");
        this.legacyPanelItemKey = new NamespacedKey(plugin, "game-panel-item");
    }

    public World ensureWorld() {
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-world-ensure-start cached="
                + (lobbyWorld != null) + " loaded=" + (Bukkit.getWorld(WORLD_NAME) != null));
        if (lobbyWorld != null) return lobbyWorld;
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.type(WorldType.FLAT);
            creator.generator(new VoidLobbyChunkGenerator());
            creator.generateStructures(false);
            creator.environment(World.Environment.NORMAL);
            plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-world-create-start name=" + WORLD_NAME);
            world = creator.createWorld();
            plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-world-create-finished success=" + (world != null));
        }
        if (world == null) {
            throw new IllegalStateException("Could not create the isolated modern Arcade lobby world '"
                    + WORLD_NAME + "'. Refusing to start an Arcade server in the survival world.");
        }

        lobbyWorld = world;
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setSpawnLocation((int) SPAWN_X, (int) SPAWN_Y, (int) SPAWN_Z);
        world.getWorldBorder().setCenter(0.0D, 0.0D);
        world.getWorldBorder().setSize(100.0D);
        world.getWorldBorder().setDamageAmount(0.0D);

        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-plane-build-start");
        ensurePlane(world);
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-plane-build-complete");
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-datapoints-build-start");
        ensureDatapoints(world);
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-datapoints-build-complete");
        plugin.getLogger().info("TESTPLEX_ARCADE_LOBBY_READY world=" + WORLD_NAME
                + " plane=100x100 floor-y=" + FLOOR_Y + " spawn=" + SPAWN_X + "," + SPAWN_Y + "," + SPAWN_Z);
        return world;
    }

    public Location spawnLocation() {
        World world = ensureWorld();
        return world == null ? null : new Location(world, SPAWN_X + 0.5D, SPAWN_Y, SPAWN_Z + 0.5D);
    }

    public boolean isLobbyWorld(World world) {
        return world != null && world == ensureWorld();
    }

    /** Teleport a new/rejoining player out of the server's unrelated survival world. */
    public void sendToLobby(Player player) {
        Location spawn = spawnLocation();
        if (spawn == null) {
            player.sendMessage(ChatColor.RED + "The Arcade lobby could not be loaded. Please notify an operator.");
            return;
        }
        player.leaveVehicle();
        player.teleport(spawn);
        // This is an Arcade test lobby, not a continuation of the generated
        // survival world. Do not carry survival inventory, XP, potion effects,
        // creative flight, or modified movement attributes into a test round.
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setWalkSpeed(0.2F);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setExp(0.0F);
        player.setLevel(0);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setAbsorptionAmount(0.0D);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        ensurePanelItem(player);
    }

    /** Ensure a match starts with a snapshot that returns to the Arcade lobby. */
    public void ensurePlayerInLobby(Player player) {
        if (!isLobbyWorld(player.getWorld())) {
            sendToLobby(player);
        } else {
            ensurePanelItem(player);
        }
    }

    /**
     * Port the dedicated Testplex sandbox hotbar from the 1.8 GameHostManager:
     * Stats, the normal cosmetic entry point, and the Game Panel are fixed to
     * slots 1, 5 and 9 respectively (Bukkit slots 0, 4 and 8).
     */
    public void ensurePanelItem(Player player) {
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getStorageContents().length; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isLobbyHotbarItem(item)) inventory.setItem(slot, null);
        }
        // Legacy Testplex sandbox hotbar: Mineplex Core supplied Cosmetics in slot 5;
        // GameHostManager supplied Game Panel in slot 9. /stats remained a command, not a hotbar item.
        inventory.setItem(HOTBAR_COSMETICS_SLOT, createHotbarItem(Material.CHEST,
                ChatColor.LIGHT_PURPLE + "Cosmetics",
                List.of(ChatColor.GRAY + "Open the Mineplex-style Cosmetics menu.",
                        ChatColor.YELLOW + "Right-click to browse.",
                        ChatColor.DARK_GRAY + "Command: /cosmetics"), "cosmetics"));
        // Speckled Melon (1.8) became Glistering Melon Slice in modern Minecraft.
        inventory.setItem(HOTBAR_GAME_PREFERENCES_SLOT, createHotbarItem(Material.WRITABLE_BOOK,
                ChatColor.AQUA + "Game Preferences",
                List.of(ChatColor.GRAY + "Like, dislike or stay neutral on each game.",
                        ChatColor.YELLOW + "Right-click to rate games.",
                        ChatColor.GRAY + "See the live selection likelihood for each game.",
                        ChatColor.DARK_GRAY + "Command: /gamepreferences"), "gamepreferences"));
        inventory.setItem(HOTBAR_GAME_PANEL_SLOT, createHotbarItem(Material.GLISTERING_MELON_SLICE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Game Panel",
                List.of(ChatColor.GRAY + "Open the Testplex Arcade game selector.",
                        ChatColor.YELLOW + "Right-click to choose a game.",
                        ChatColor.DARK_GRAY + "Command: /game"), "gamepanel"));
        lobbyScoreboard.apply(player);
    }

    /** Reapply the live game/kit sidebar to players currently in the Arcade lobby. */
    public void refreshLobbyScoreboards() {
        World world = ensureWorld();
        if (world == null) return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getWorld() == world) lobbyScoreboard.apply(online);
        }
    }

    /** Refresh a single tester after their selected kit changes. */
    public void refreshPlayerScoreboard(Player player) {
        if (player != null && player.isOnline() && isLobbyWorld(player.getWorld())) {
            lobbyScoreboard.apply(player);
        }
    }

    private ItemStack createHotbarItem(Material type, String title, List<String> lore, String action) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(hotbarActionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isLobbyHotbarItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(hotbarActionKey, PersistentDataType.STRING)
                || pdc.has(legacyPanelItemKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPanelItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer()
                .get(hotbarActionKey, PersistentDataType.STRING);
        if (action == null) {
            if (item.getItemMeta().getPersistentDataContainer().has(legacyPanelItemKey, PersistentDataType.BYTE)) {
                action = "gamepanel";
            } else {
                return;
            }
        }
        event.setCancelled(true);
        Bukkit.dispatchCommand(event.getPlayer(), action);
    }

    private void ensurePlane(World world) {
        Path marker = world.getWorldFolder().toPath().resolve("TESTPLEX_LEGACY_LOBBY_V1");
        boolean complete = false;
        if (Files.isRegularFile(marker)) {
            try {
                complete = LOBBY_MARKER.equals(Files.readAllLines(marker, StandardCharsets.UTF_8).stream()
                        .findFirst().orElse(""));
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not read Arcade lobby marker; validating the plane in place.");
            }
        }
        complete = complete && hasExpectedPlane(world);
        if (complete) return;

        plugin.getLogger().info("Generating the legacy 100x100 Testplex lobby plane at Y=" + FLOOR_Y + ".");
        int minChunk = Math.floorDiv(PLANE_MIN, 16);
        int maxChunk = Math.floorDiv(PLANE_MAX_EXCLUSIVE - 1, 16);
        int chunksGenerated = 0;
        for (int chunkX = minChunk; chunkX <= maxChunk; chunkX++) {
            for (int chunkZ = minChunk; chunkZ <= maxChunk; chunkZ++) {
                // New chunks are populated directly by VoidLobbyChunkGenerator.
                world.getChunkAt(chunkX, chunkZ);
                chunksGenerated++;
            }
        }
        plugin.getLogger().info("TESTPLEX_ARCADE_BOOTSTRAP phase=lobby-chunks-ready count=" + chunksGenerated);

        // Repair an older world created by the previous empty generator. New worlds
        // should already be complete from ChunkData, making this fallback unnecessary.
        if (!hasExpectedPlane(world)) {
            plugin.getLogger().warning("Repairing an existing incomplete Arcade lobby plane.");
            for (int x = PLANE_MIN; x < PLANE_MAX_EXCLUSIVE; x++) {
                for (int z = PLANE_MIN; z < PLANE_MAX_EXCLUSIVE; z++) {
                    world.getBlockAt(x, FLOOR_Y - 2, z).setType(Material.BEDROCK, false);
                    world.getBlockAt(x, FLOOR_Y - 1, z).setType(Material.STONE, false);
                    world.getBlockAt(x, FLOOR_Y, z).setType(Material.GRASS_BLOCK, false);
                }
            }
        }
        if (!hasExpectedPlane(world)) {
            throw new IllegalStateException("The dedicated Testplex Arcade world did not generate its 100x100 platform.");
        }
        try {
            Files.writeString(marker, LOBBY_MARKER + System.lineSeparator()
                    + "X/Z -50..49; grass surface Y=100; bedrock void guard at Y=98.",
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not mark generated Arcade lobby plane: " + exception.getMessage());
        }
    }

    private boolean hasExpectedPlane(World world) {
        int max = PLANE_MAX_EXCLUSIVE - 1;
        int[][] samples = {
                {PLANE_MIN, PLANE_MIN}, {PLANE_MIN, max}, {max, PLANE_MIN},
                {max, max}, {0, 0}
        };
        for (int[] sample : samples) {
            int x = sample[0], z = sample[1];
            if (world.getBlockAt(x, FLOOR_Y - 2, z).getType() != Material.BEDROCK
                    || world.getBlockAt(x, FLOOR_Y - 1, z).getType() != Material.STONE
                    || world.getBlockAt(x, FLOOR_Y, z).getType() != Material.GRASS_BLOCK) {
                return false;
            }
        }
        return true;
    }

    static boolean isInsidePlane(int x, int z) {
        return x >= PLANE_MIN && x < PLANE_MAX_EXCLUSIVE
                && z >= PLANE_MIN && z < PLANE_MAX_EXCLUSIVE;
    }

    static Material planeLayerMaterial(int y) {
        if (y == FLOOR_Y - 2) return Material.BEDROCK;
        if (y == FLOOR_Y - 1) return Material.STONE;
        if (y == FLOOR_Y) return Material.GRASS_BLOCK;
        return null;
    }

    private void ensureDatapoints(World world) {
        // Preserve the original LegacyGameLobbyManager's kit/team display pads.
        // Kit and queue stations are built separately by ModernArcadeLobbyRuntime
        // from the selected game's kit list, just as LegacyGameLobbyManager.createKits/createTeams did.

        // Port of LegacyGameLobbyManager.setPodium(new Location(world, 0, 101.5, -15), emerald).
        // The 4x4 emerald floor is surrounded by a 6x6 dark outline; the 2x2 top
        // emerald blocks and slab caps sit one and two blocks above the grass plane.
        for (int x = -2; x <= 1; x++) {
            for (int z = -17; z <= -14; z++) {
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.EMERALD_BLOCK, false);
            }
        }
        for (int x = -3; x <= 2; x++) {
            for (int z = -18; z <= -13; z++) {
                if (x >= -2 && x <= 1 && z >= -17 && z <= -14) continue;
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.BLACK_WOOL, false);
            }
        }
        for (int x = -1; x <= 0; x++) {
            for (int z = -16; z <= -15; z++) {
                world.getBlockAt(x, FLOOR_Y + 1, z).setType(Material.EMERALD_BLOCK, false);
                world.getBlockAt(x, FLOOR_Y + 2, z).setType(Material.STONE_SLAB, false);
            }
        }

        display(world, "missions", new Location(world, MISSIONS_X, MISSIONS_Y, MISSIONS_Z),
                ChatColor.GOLD + "" + ChatColor.BOLD + "MISSIONS");
        display(world, "game-panel", new Location(world, 0.0D, 106.0D, 3.0D),
                ChatColor.AQUA + "" + ChatColor.BOLD + "GAME PANEL");
        display(world, "game-text", new Location(world, 0.0D, 130.0D, GAME_TEXT_Z),
                ChatColor.AQUA + "" + ChatColor.BOLD + "ARCADE");
        display(world, "game-map", new Location(world, 0.0D, 124.0D, GAME_TEXT_Z),
                ChatColor.WHITE + "Map: Default");
        display(world, "game-mode", new Location(world, 0.0D, 118.0D, GAME_TEXT_Z),
                ChatColor.WHITE + "Mode: Standard");
        display(world, "game-status", new Location(world, 0.0D, 112.0D, GAME_TEXT_Z),
                ChatColor.GREEN + "Waiting for players");
        display(world, "kit-text", new Location(world, KIT_TEXT_X, KIT_TEXT_Y, 0.0D),
                ChatColor.GREEN + "" + ChatColor.BOLD + "KIT SELECT");
        display(world, "team-text", new Location(world, TEAM_TEXT_X, TEAM_TEXT_Y, 0.0D),
                ChatColor.BLUE + "" + ChatColor.BOLD + "TEAM SELECT");
        display(world, "adv-text", new Location(world, 0.0D, ADV_TEXT_Y, ADV_TEXT_Z),
                ChatColor.GOLD + "" + ChatColor.BOLD + "ARCADE INFORMATION");
        // Keep station labels above the NPCs instead of at their spawn point.
        // Y=102.5 overlapped the centre kit and queue NPCs.
        display(world, "kit-display", new Location(world, KIT_DISPLAY_X, FLOOR_Y + 5.0D, 0.0D),
                ChatColor.GREEN + "Kit display datapoint");
        display(world, "team-display", new Location(world, TEAM_DISPLAY_X, FLOOR_Y + 5.0D, 0.0D),
                ChatColor.BLUE + "Team display datapoint");
        display(world, "podium", new Location(world, 0.0D, FLOOR_Y + 3.0D, AMP_STAND_Z),
                ChatColor.GOLD + "Podium / amplifier datapoint");
        display(world, "help", new Location(world, 0.0D, 105.5D, 2.0D),
                ChatColor.YELLOW + "Right-click the compass or use /game to open the Game Panel");
    }

    private void buildDisplayPad(World world, int centerX, int centerZ, Material color) {
        // Original 1.8 datapoint: coloured 4x4 floor, dark 6x6 outline,
        // four raised colour blocks and slab caps at the kit/team display.
        for (int dx = -2; dx <= 1; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                world.getBlockAt(centerX + dx, FLOOR_Y, centerZ + dz).setType(color, false);
            }
        }
        for (int dx = -3; dx <= 2; dx++) {
            for (int dz = -3; dz <= 2; dz++) {
                if (dx >= -2 && dx <= 1 && dz >= -2 && dz <= 1) continue;
                world.getBlockAt(centerX + dx, FLOOR_Y, centerZ + dz).setType(Material.BLACK_WOOL, false);
            }
        }
        for (int dx : new int[] {-1, 0}) {
            for (int dz : new int[] {-1, 0}) {
                world.getBlockAt(centerX + dx, FLOOR_Y + 1, centerZ + dz).setType(color, false);
                world.getBlockAt(centerX + dx, FLOOR_Y + 2, centerZ + dz).setType(Material.STONE_SLAB, false);
            }
        }
    }

    private void display(World world, String id, Location location, String name) {
        for (ArmorStand existing : world.getEntitiesByClass(ArmorStand.class)) {
            if (!id.equals(existing.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING))) {
                continue;
            }
            if (!existing.getLocation().equals(location)) existing.teleport(location);
            existing.setCustomName(name);
            existing.setCustomNameVisible(true);
            return;
        }
        ArmorStand stand = world.spawn(location, ArmorStand.class);
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setPersistent(false);
        stand.getPersistentDataContainer().set(displayKey, PersistentDataType.STRING, id);
    }

    /** A genuinely empty world, matching the legacy test plane's void buffer. */
    private static final class VoidLobbyChunkGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            ChunkData data = createChunkData(world);
            for (int localX = 0; localX < 16; localX++) {
                int blockX = x * 16 + localX;
                if (blockX < PLANE_MIN || blockX >= PLANE_MAX_EXCLUSIVE) continue;
                for (int localZ = 0; localZ < 16; localZ++) {
                    int blockZ = z * 16 + localZ;
                    if (!isInsidePlane(blockX, blockZ)) continue;
                    for (int y = FLOOR_Y - 2; y <= FLOOR_Y; y++) {
                        Material layer = planeLayerMaterial(y);
                        if (layer != null) data.setBlock(localX, y, localZ, layer);
                    }
                }
            }
            return data;
        }

        @Override
        public List<BlockPopulator> getDefaultPopulators(World world) {
            return Collections.emptyList();
        }

        @Override
        public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, SPAWN_X + 0.5D, SPAWN_Y, SPAWN_Z + 0.5D);
        }

        @Override
        public boolean canSpawn(World world, int x, int z) {
            return true;
        }
    }
}
