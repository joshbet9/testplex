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
    private final NamespacedKey displayKey;
    private final NamespacedKey panelItemKey;
    private World lobbyWorld;

    public ModernArcadeLobby(JavaPlugin plugin) {
        this.plugin = plugin;
        this.displayKey = new NamespacedKey(plugin, "legacy-lobby-datapoint");
        this.panelItemKey = new NamespacedKey(plugin, "game-panel-item");
    }

    public World ensureWorld() {
        if (lobbyWorld != null) return lobbyWorld;
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.type(WorldType.FLAT);
            creator.generator(new VoidLobbyChunkGenerator());
            creator.generateStructures(false);
            creator.environment(World.Environment.NORMAL);
            world = creator.createWorld();
        }
        if (world == null) {
            plugin.getLogger().severe("Could not create the isolated modern Arcade lobby world.");
            return null;
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

        ensurePlane(world);
        ensureDatapoints(world);
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
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
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

    public void ensurePanelItem(Player player) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isPanelItem(stack)) return;
        }
        int slot = player.getInventory().firstEmpty();
        if (slot < 0) return;
        ItemStack panel = new ItemStack(Material.COMPASS);
        ItemMeta meta = panel.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Game Panel");
        meta.setLore(List.of(
                ChatColor.GRAY + "Open the Testplex Arcade game selector.",
                ChatColor.YELLOW + "Right-click to browse and launch a test game.",
                ChatColor.DARK_GRAY + "Command alternative: /game"));
        meta.getPersistentDataContainer().set(panelItemKey, PersistentDataType.BYTE, (byte) 1);
        panel.setItemMeta(meta);
        player.getInventory().setItem(slot, panel);
    }

    private boolean isPanelItem(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(panelItemKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPanelItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isPanelItem(event.getItem())) return;
        event.setCancelled(true);
        Bukkit.dispatchCommand(event.getPlayer(), "gamepanel");
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
        complete = complete && world.getBlockAt(0, FLOOR_Y, 0).getType() == Material.GRASS_BLOCK
                && world.getBlockAt(PLANE_MIN, FLOOR_Y, PLANE_MIN).getType() == Material.GRASS_BLOCK
                && world.getBlockAt(PLANE_MAX_EXCLUSIVE - 1, FLOOR_Y, PLANE_MAX_EXCLUSIVE - 1).getType() == Material.GRASS_BLOCK;
        if (!complete) {
            plugin.getLogger().info("Building the legacy 100x100 Testplex lobby plane at Y=" + FLOOR_Y + ".");
            for (int x = PLANE_MIN; x < PLANE_MAX_EXCLUSIVE; x++) {
                for (int z = PLANE_MIN; z < PLANE_MAX_EXCLUSIVE; z++) {
                    world.getBlockAt(x, FLOOR_Y - 2, z).setType(Material.BEDROCK, false);
                    world.getBlockAt(x, FLOOR_Y - 1, z).setType(Material.STONE, false);
                    world.getBlockAt(x, FLOOR_Y, z).setType(Material.GRASS_BLOCK, false);
                }
            }
            try {
                Files.writeString(marker, LOBBY_MARKER + System.lineSeparator()
                        + "X/Z -50..49; grass surface Y=100; bedrock void guard at Y=98.",
                        StandardCharsets.UTF_8);
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not mark generated Arcade lobby plane: " + exception.getMessage());
            }
        }
    }

    private void ensureDatapoints(World world) {
        // Preserve the original LegacyGameLobbyManager's kit/team display pads.
        buildDisplayPad(world, (int) KIT_DISPLAY_X, 0, Material.RED_WOOL);
        buildDisplayPad(world, (int) TEAM_DISPLAY_X, 0, Material.BLUE_WOOL);

        // Legacy podium and amplifier datapoint share the original centre at Z=-15.
        for (int x = -1; x <= 1; x++) {
            for (int z = -16; z <= -14; z++) {
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.EMERALD_BLOCK, false);
            }
        }
        world.getBlockAt(0, FLOOR_Y + 1, -15).setType(Material.SEA_LANTERN, false);
        world.getBlockAt(0, FLOOR_Y + 2, -15).setType(Material.AIR, false);

        display(world, "missions", new Location(world, MISSIONS_X, MISSIONS_Y, MISSIONS_Z),
                ChatColor.GOLD + "" + ChatColor.BOLD + "MISSIONS");
        display(world, "game-panel", new Location(world, 0.0D, 106.0D, 3.0D),
                ChatColor.AQUA + "" + ChatColor.BOLD + "GAME PANEL");
        display(world, "game-text", new Location(world, 0.0D, 130.0D, GAME_TEXT_Z),
                ChatColor.AQUA + "" + ChatColor.BOLD + "ARCADE GAME PANEL");
        display(world, "kit-text", new Location(world, KIT_TEXT_X, KIT_TEXT_Y, 0.0D),
                ChatColor.GREEN + "" + ChatColor.BOLD + "KIT SELECT");
        display(world, "team-text", new Location(world, TEAM_TEXT_X, TEAM_TEXT_Y, 0.0D),
                ChatColor.BLUE + "" + ChatColor.BOLD + "TEAM SELECT");
        display(world, "adv-text", new Location(world, 0.0D, ADV_TEXT_Y, ADV_TEXT_Z),
                ChatColor.GOLD + "" + ChatColor.BOLD + "ARCADE INFORMATION");
        display(world, "kit-display", new Location(world, KIT_DISPLAY_X, FLOOR_Y + 2.5D, 0.0D),
                ChatColor.GREEN + "Kit display datapoint");
        display(world, "team-display", new Location(world, TEAM_DISPLAY_X, FLOOR_Y + 2.5D, 0.0D),
                ChatColor.BLUE + "Team display datapoint");
        display(world, "podium", new Location(world, 0.0D, FLOOR_Y + 3.0D, AMP_STAND_Z),
                ChatColor.GOLD + "Podium / amplifier datapoint");
        display(world, "help", new Location(world, 0.0D, 105.5D, 2.0D),
                ChatColor.YELLOW + "Right-click the compass or use /game to open the Game Panel");
    }

    private void buildDisplayPad(World world, int centerX, int centerZ, Material color) {
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
    }

    private void display(World world, String id, Location location, String name) {
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            if (id.equals(stand.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING))) return;
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
            return createChunkData(world);
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
