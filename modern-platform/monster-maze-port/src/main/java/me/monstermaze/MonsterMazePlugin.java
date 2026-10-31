package me.monstermaze;

import au.com.testplex.arcadeports.ArcadeMechanicsModernPlugin;
import me.monstermaze.command.MMCommand;
import me.monstermaze.command.MMUtilityCommandListener;
import me.monstermaze.command.PauseCommand;
import me.monstermaze.command.DebugSoloModeListener;
import me.monstermaze.entity.MonsterEntityListener;
import me.monstermaze.game.BuildBypassListener;
import me.monstermaze.game.GameManager;
import me.monstermaze.game.MazeMode;
import me.monstermaze.stats.BackendClient;
import me.monstermaze.stats.ChallengeManager;
import me.monstermaze.stats.CompetitiveMatchTracker;
import me.monstermaze.stats.RunRecorder;
import me.monstermaze.stats.StatusHeartbeatReporter;
import me.monstermaze.stats.SoloRunCompletionListener;
import me.monstermaze.world.MapCommandListener;
import me.monstermaze.world.MapManager;
import me.monstermaze.world.MapThemeApplier;
import me.monstermaze.world.MapWorldSafetyListener;
import me.monstermaze.world.SoloPBCommandListener;
import me.monstermaze.util.MobThemeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Objects;

/**
 * Monster Maze's Testplex game-module facade.
 *
 * This is deliberately not a JavaPlugin. Testplex's ArcadeMechanicsModernPlugin
 * owns Bukkit's plugin lifecycle, player join flow, commands, queues and game
 * selection. Monster Maze keeps its gameplay managers and private configuration
 * namespace, but it is loaded and started by the Testplex Arcade manager.
 */
public final class MonsterMazePlugin {
    private static MonsterMazePlugin instance;
    private final ArcadeMechanicsModernPlugin host;
    private final File dataFolder;
    private final File configFile;
    private FileConfiguration config;

    private GameManager gameManager;
    private MobThemeManager mobThemeManager;
    private MapManager mapManager;
    private MapThemeApplier mapThemeApplier;
    private MapWorldSafetyListener mapWorldSafetyListener;
    private MazeMode mode = MazeMode.ORIGINAL;
    private me.monstermaze.stats.LeaderboardManager leaderboards;
    private boolean soloMode;
    private boolean debug;
    private boolean recordRuns;
    private BackendClient backendClient;
    private RunRecorder runRecorder;
    private SoloRunCompletionListener soloRunCompletionListener;
    private MonsterEntityListener monsterEntityListener;
    private Object monsterDisguiseListener;
    private java.lang.reflect.Method refreshMonsterDisguisesMethod;
    private java.lang.reflect.Method shutdownMonsterDisguisesMethod;
    private ChallengeManager challengeManager;
    private CompetitiveMatchTracker competitiveMatchTracker;
    private StatusHeartbeatReporter statusHeartbeatReporter;

    public MonsterMazePlugin(ArcadeMechanicsModernPlugin host) {
        this.host = Objects.requireNonNull(host, "host");
        this.dataFolder = new File(host.getDataFolder(), "monster-maze");
        this.configFile = new File(dataFolder, "config.yml");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            host.getLogger().warning("Could not create Monster Maze module data directory: " + dataFolder);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        instance = this;
    }

    /** Initialise Monster Maze managers without taking ownership of player joins or the server lobby. */
    public void enable() {
        instance = this;
        saveDefaultConfig();
        this.backendClient = new BackendClient(this);
        this.mapManager = new MapManager(this);
        this.mapThemeApplier = new MapThemeApplier(this);
        this.mapWorldSafetyListener = new MapWorldSafetyListener(this);
        this.leaderboards = new me.monstermaze.stats.LeaderboardManager(this);
        this.runRecorder = new RunRecorder(this);
        this.challengeManager = new ChallengeManager(this);

        FileConfiguration cfg = getConfig();
        MazeMode stored = MazeMode.byName(cfg.getString("mode", "Original"));
        if (stored == null) stored = MazeMode.ORIGINAL;
        this.mode = stored;
        this.soloMode = cfg.getBoolean("solo-mode", false);
        this.debug = cfg.getBoolean("debug", false);
        this.recordRuns = cfg.getBoolean("record-runs", true);
        if (!cfg.contains("forced-pattern")) {
            cfg.set("forced-pattern", -1);
            saveConfig();
        }

        for (MazeMode m : MazeMode.values()) {
            try {
                String resource = "modes/" + m.id + ".txt";
                if (getResource(resource) != null) {
                    File dest = new File(dataFolder, resource);
                    if (!dest.exists()) {
                        File parent = dest.getParentFile();
                        if (parent != null) parent.mkdirs();
                        saveResource(resource, false);
                    }
                }
            } catch (Exception exception) {
                getLogger().log(Level.WARNING, "Could not prepare Monster Maze mode data.", exception);
            }
        }

        this.mobThemeManager = new MobThemeManager(this);
        Location mapCenter = mapManager.defaultCenter();
        if (mapCenter == null) {
            getLogger().warning("No active Monster Maze source map is available yet; the module will remain idle until a map can be loaded.");
        }
        this.gameManager = new GameManager(this);
        if (mapCenter != null) this.gameManager.setCenter(mapCenter);
        this.gameManager.getMonsterManager().setMobType(mapManager.activeMob());
        this.monsterEntityListener = new MonsterEntityListener(this, gameManager);
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            try {
                Class<?> adapterType = Class.forName("me.monstermaze.entity.MonsterDisguiseListener");
                Object adapter = adapterType.getConstructor(MonsterMazePlugin.class).newInstance(this);
                java.lang.reflect.Method refresh = adapterType.getMethod("refreshPlayer", Player.class);
                java.lang.reflect.Method shutdown = adapterType.getMethod("shutdown");
                this.monsterDisguiseListener = adapter;
                this.refreshMonsterDisguisesMethod = refresh;
                this.shutdownMonsterDisguisesMethod = shutdown;
                getLogger().info("ProtocolLib detected; Monster Maze ghost-mob disguises enabled.");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING,
                        "ProtocolLib is present but ghost-mob disguises could not be enabled; base mob rendering will remain active.", t);
            }
        } else {
            getLogger().warning("ProtocolLib not installed; Monster Maze mobs will render as Snow Golems.");
        }
        this.mapThemeApplier.start();
        this.soloRunCompletionListener = new SoloRunCompletionListener(this);
        this.competitiveMatchTracker = new CompetitiveMatchTracker(this);
        this.statusHeartbeatReporter = new StatusHeartbeatReporter(this);

        // Intentionally do not bootstrap a Monster Maze lobby or register the old
        // LobbyListener. Testplex's shared Arcade lobby is the only player lobby.
        new MapCommandListener(this);
        new SoloPBCommandListener(this);
        new BuildBypassListener(this);
        new MMUtilityCommandListener(this);
        Bukkit.getPluginManager().registerEvents(new DebugSoloModeListener(this), host);

        PluginCommand mm = getCommand("mm");
        if (mm == null) throw new IllegalStateException("Testplex Arcade plugin.yml is missing the /mm command.");
        mm.setExecutor(new MMCommand(this));
        PluginCommand pause = getCommand("pause");
        if (pause == null) throw new IllegalStateException("Testplex Arcade plugin.yml is missing the /pause command.");
        pause.setExecutor(new PauseCommand(this));

        statusHeartbeatReporter.start();
        getLogger().info("Monster Maze gameplay initialization completed; awaiting Testplex Arcade manager attachment.");
        getLogger().info("Active map: " + mapManager.getActiveMap());
        getLogger().info("Mode: " + mode.id);
        getLogger().info("Run recording: " + recordRuns);
    }

    public void shutdown() {
        if (competitiveMatchTracker != null) competitiveMatchTracker.shutdown();
        if (soloRunCompletionListener != null) soloRunCompletionListener.shutdown();
        if (gameManager != null) gameManager.forceStop();
        if (monsterDisguiseListener != null && shutdownMonsterDisguisesMethod != null) {
            try {
                shutdownMonsterDisguisesMethod.invoke(monsterDisguiseListener);
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Could not shut down optional mob disguise adapter.", t);
            }
        }
        if (mobThemeManager != null) mobThemeManager.shutdown();
        saveConfig();
    }

    public void setReturnToTestplex(java.util.function.Consumer<Player> callback) {
        if (gameManager != null) gameManager.setTestplexReturnToLobby(callback);
    }
    public boolean startTestplexGame(java.util.List<Player> participants) {
        return gameManager != null && gameManager.startTestplexRound(participants,
                player -> host.returnPlayerToArcadeLobby(player));
    }
    public boolean isGameRunning() { return gameManager != null && gameManager.isRunning(); }

    public ArcadeMechanicsModernPlugin getHostPlugin() { return host; }

    public void recordFirstSafePad(UUID playerId) {
        host.recordMonsterMazeFirstSafePad(playerId);
    }

    public void recordHighestStage(UUID playerId, int stage) {
        host.recordMonsterMazeHighestStage(playerId, stage);
    }

    public void recordWin(UUID playerId, int stage) {
        host.recordMonsterMazeWin(playerId, stage);
    }

    public void recordAchievement(UUID playerId, String achievementId) {
        host.recordMonsterMazeAchievement(playerId, achievementId);
    }
    public static MonsterMazePlugin getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public MobThemeManager getMobThemeManager() { return mobThemeManager; }
    public void refreshPlayerMobThemes(Player player) {
        if (monsterDisguiseListener == null || refreshMonsterDisguisesMethod == null || player == null) return;
        try { refreshMonsterDisguisesMethod.invoke(monsterDisguiseListener, player); }
        catch (Throwable ignored) { }
    }
    public boolean hasPerPlayerMobDisguises() {
        return monsterDisguiseListener != null && refreshMonsterDisguisesMethod != null;
    }
    public MazeMode getMode() { return mode; }
    public MazeMode setMode(MazeMode newMode) {
        if (newMode == null) newMode = MazeMode.ORIGINAL;
        this.mode = newMode;
        getConfig().set("mode", newMode.id);
        saveConfig();
        return newMode;
    }
    public MapManager getMapManager() { return mapManager; }
    public MapThemeApplier getMapThemeApplier() { return mapThemeApplier; }
    public me.monstermaze.stats.LeaderboardManager getLeaderboards() { return leaderboards; }
    public ChallengeManager getChallengeManager() { return challengeManager; }
    public boolean isSoloMode() { return soloMode; }
    public void setSoloMode(boolean enabled) { this.soloMode = enabled; getConfig().set("solo-mode", enabled); saveConfig(); }
    public boolean isDebug() { return debug; }
    public boolean isRecordRuns() { return recordRuns; }
    public BackendClient getBackendClient() { return backendClient; }
    public RunRecorder getRunRecorder() { return runRecorder; }

    // Bukkit services delegated to Testplex's owning plugin.
    public ArcadeMechanicsModernPlugin getPlugin() { return host; }
    public Server getServer() { return host.getServer(); }
    public java.util.logging.Logger getLogger() { return host.getLogger(); }
    public PluginDescriptionFile getDescription() { return host.getDescription(); }
    public PluginCommand getCommand(String name) { return host.getCommand(name); }
    public File getDataFolder() { return dataFolder; }
    public InputStream getResource(String name) { return host.getResource("monster-maze/" + name); }
    public FileConfiguration getConfig() { return config; }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                getLogger().warning("Could not create Monster Maze module data directory: " + dataFolder);
                return;
            }
            config.save(configFile);
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Could not save Monster Maze module configuration.", exception);
        }
    }

    public void saveResource(String resourcePath, boolean replace) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        if (resourcePath.isBlank() || resourcePath.startsWith("/") || resourcePath.contains("..")) {
            throw new IllegalArgumentException("Invalid Monster Maze module resource path: " + resourcePath);
        }
        File destination = new File(dataFolder, resourcePath.replace('/', File.separatorChar));
        if (destination.exists() && !replace) return;
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create resource directory: " + parent);
        }
        try (InputStream input = getResource(resourcePath)) {
            if (input == null) throw new IllegalArgumentException("Missing bundled Monster Maze resource: " + resourcePath);
            if (replace) {
                Files.copy(input, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(input, destination.toPath());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save Monster Maze resource: " + resourcePath, exception);
        }
    }
}
