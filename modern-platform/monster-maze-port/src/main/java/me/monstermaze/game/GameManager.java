package me.monstermaze.game;

import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.entity.MonsterManager;
import me.monstermaze.event.FirstToSafepadEvent;
import me.monstermaze.event.SafepadBuildEvent;
import me.monstermaze.kit.KitManager;
import me.monstermaze.maze.MazeGenerator;
import me.monstermaze.maze.MazePathfinder;
import me.monstermaze.stats.StatTracker;
import me.monstermaze.util.GameScoreboard;
import me.monstermaze.util.PlayerFeedback;
import me.monstermaze.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.FireworkEffect;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Full game loop closer to original Mineplex Monster Maze.
 */
public class GameManager implements Listener {

    private final MonsterMazePlugin plugin;
    private final MazeGenerator mazeGenerator;
    private final MazePathfinder safePadNavigation = new MazePathfinder();
    private final MonsterManager monsterManager;
    private final KitManager kitManager;
    private final StatTracker stats;
    private final GameScoreboard scoreboard = new GameScoreboard();
    private final PlayerFeedback feedback;

    private GameState state = GameState.IDLE;
    private Location center;
    private long liveStartMs;

    /** Active game mode (Original / Speed / Modern). */
    private MazeMode mode = MazeMode.ORIGINAL;

    private final Set<UUID> alive = new HashSet<UUID>();
    private final Set<UUID> spectators = new HashSet<UUID>();
    /** UUIDs included at round start; late entrants in this world must remain outside the match. */
    private final Set<UUID> roundParticipants = new HashSet<UUID>();
    private final List<Player> playersOnPad = new ArrayList<Player>();
    /** Players who have landed on this stage's Safe Pad; jumping does not clear this lock-in. */
    private final Set<UUID> padLockIns = new HashSet<UUID>();

    private SafePad safePad;          // active pad
    private SafePad nextSafePad;      // preview pad (spawns at t=2)
    private final LinkedList<SafePad> oldSafePads = new LinkedList<SafePad>();

    private int curSafe = 1;
    private final Set<UUID> firstPadPlayers = new HashSet<UUID>();
    private final Map<UUID, Set<UUID>> outOfCupTargets = new HashMap<UUID, Set<UUID>>();
    private final Set<UUID> photoFinishCandidates = new HashSet<UUID>();
    private int phaseTimer = 60;
    private int phaseTimerStart = 60;
    private int centerSafeZoneDecay = 11;
    private boolean firstClaimedThisPhase;
    private boolean soloMode;

    private BukkitTask mainTask;
    private BukkitTask secondTask;
    private BukkitTask startingTask;   // runs the jump lock during STARTING (pre-glass fall)
    private List<Player> pendingTestplexParticipants;
    private Consumer<Player> testplexReturnToLobby;
    private boolean testplexManagedRound;


    /** PERF: cached per-tick packet targets so we only send what actually changed. */
    private int liveTick;
    private float lastExpPct = -1;
    private int lastLevel = -1;
    private final Map<UUID, Location> lastCompassTargets = new HashMap<UUID, Location>();

    public GameManager(MonsterMazePlugin plugin) {
        this.plugin = plugin;
        this.mazeGenerator = new MazeGenerator(plugin);
        this.monsterManager = new MonsterManager(plugin, this);
        this.kitManager = new KitManager(plugin, this, scoreboard);
        this.stats = new StatTracker(this, plugin);
        this.feedback = new PlayerFeedback(plugin, this);
        Bukkit.getPluginManager().registerEvents(this, plugin.getHostPlugin());
        new ArenaListener(plugin, this);
    }

    public GameState getState() { return state; }
    public boolean isRunning() { return state != GameState.IDLE && state != GameState.ENDING; }
    public boolean isLive() { return state == GameState.LIVE; }
    public int getStage() { return curSafe; }
    public void recordFirstSafePad(UUID playerId) { plugin.recordFirstSafePad(playerId); }
    public void recordAchievement(UUID playerId, String achievementId) {
        plugin.recordAchievement(playerId, achievementId);
    }
    public void noteFirstPad(Player player) {
        if (player != null) firstPadPlayers.add(player.getUniqueId());
    }
    public void noteOutOfCupSlowball(Player shooter, Player target) {
        if (shooter == null || target == null || !firstPadPlayers.contains(shooter.getUniqueId())) return;
        outOfCupTargets.computeIfAbsent(shooter.getUniqueId(), ignored -> new HashSet<UUID>())
                .add(target.getUniqueId());
    }
    public void notePhotoFinish(Player player) {
        if (player != null) photoFinishCandidates.add(player.getUniqueId());
    }
    public void setCenter(Location loc) {
        // A live round's arena world is part of its player/entity isolation boundary.
        if (loc == null || loc.getWorld() == null || isRunning()) return;
        this.center = loc.clone();
        // Testplex owns the only lobby. Store only the arena origin here.
        mazeGenerator.setArenaCenter(center);
    }

    /** Compatibility bridge: all player lobby returns belong to Testplex Arcade. */
    public void sendToLobby(Player player) {
        if (player != null && player.isOnline()) {
            plugin.getHostPlugin().returnPlayerToArcadeLobby(player);
        }
    }
    public Location getCenter() { return center; }
    public MonsterManager getMonsterManager() { return monsterManager; }
    public KitManager getKitManager() { return kitManager; }
    public MazeGenerator getMazeGenerator() { return mazeGenerator; }
    public long getGameLiveTime() { return liveStartMs; }

    /**
     * True when the live player is airborne over a single missing maze-path cell
     * with a real path cell on both sides of that gap.
     */
    public boolean isOverOneBlockGap(Player player) {
        if (player == null || center == null || player.getWorld() != center.getWorld()) return false;
        Location current = player.getLocation();
        if (current.getY() <= center.getY() || current.getY() > center.getY() + 5.0D) return false;
        Location pathCell = new Location(current.getWorld(), current.getX(), center.getY(), current.getZ());
        if (mazeGenerator.isPathRaw(pathCell)) return false;
        boolean north = mazeGenerator.isPathRaw(pathCell.clone().add(0.0D, 0.0D, -1.0D));
        boolean south = mazeGenerator.isPathRaw(pathCell.clone().add(0.0D, 0.0D, 1.0D));
        boolean east = mazeGenerator.isPathRaw(pathCell.clone().add(1.0D, 0.0D, 0.0D));
        boolean west = mazeGenerator.isPathRaw(pathCell.clone().add(-1.0D, 0.0D, 0.0D));
        return isSingleBlockMazeGap(north, south, east, west);
    }

    static boolean isSingleBlockMazeGap(boolean northPath, boolean southPath,
            boolean eastPath, boolean westPath) {
        return (northPath && southPath) || (eastPath && westPath);
    }

    /** Index (0/1/2) of the maze pattern loaded for the current game, or -1 if not live. */
    public int getPatternIndex() {
        return state == GameState.LIVE || state == GameState.STARTING
                ? mazeGenerator.getPatternIndex()
                : -1;
    }

    public MazeMode getMode() {
        return mode;
    }

    /** True for any mode that receives gameplay QOL fixes (everything except Original). */
    public boolean qolEnabled() {
        return mode.qolEnabled();
    }

    // -------------------- Start / Stop --------------------

/** Configure a round whose participants and return path are owned by Testplex Arcade. */
public boolean startTestplexRound(List<Player> participants, Consumer<Player> returnToLobby) {
    if (state != GameState.IDLE || participants == null || participants.isEmpty()) return false;
    List<Player> selected = new ArrayList<>();
    for (Player player : participants) {
        if (player != null && player.isOnline() && !selected.contains(player)) selected.add(player);
    }
    if (selected.isEmpty()) return false;
    this.pendingTestplexParticipants = selected;
    this.testplexReturnToLobby = returnToLobby;
    this.testplexManagedRound = true;
    // Testplex's MM configuration commands set the mode and pattern before
    // queueing; the Arcade manager remains the only way to begin a round.
    startGame(null, plugin.getConfig().getInt("forced-pattern", -1));
    if (state != GameState.STARTING && state != GameState.LIVE) {
        this.pendingTestplexParticipants = null;
        this.testplexReturnToLobby = null;
        this.testplexManagedRound = false;
        return false;
    }
    return true;
}

public void setTestplexReturnToLobby(Consumer<Player> returnToLobby) {
    this.testplexReturnToLobby = returnToLobby;
}

public boolean isTestplexManagedRound() { return testplexManagedRound; }

public void startGame() {
    startGame(null, -1);
}

public void startGame(int requestedPattern) {
    startGame(null, requestedPattern);
}

/** @param preferCenter optional player location when /mm start is used without setcenter */
public void startGame(Location preferCenter) {
    startGame(preferCenter, -1);
}

public void startGame(Location preferCenter, int requestedPattern) {
    if (state != GameState.IDLE) return;

    if (center == null) {
        if (preferCenter != null && preferCenter.getWorld() != null) {
            center = preferCenter.clone();
        } else {
            Location configuredCenter = plugin.getMapManager() == null
                    ? null : plugin.getMapManager().defaultCenter();
            if (configuredCenter == null || configuredCenter.getWorld() == null) {
                broadcast(ChatColor.RED + "[MonsterMaze] No arena center configured. Set a map or use /mm setcenter.");
                return;
            }
            center = configuredCenter.clone();
        }
    }

        // MazeGenerator needs its own center + lobby before generate
        if (mazeGenerator.getCenter() == null) {
            mazeGenerator.setArenaCenter(center);
        }

        cleanupEntities();
        alive.clear();
        spectators.clear();
        playersOnPad.clear();
        padLockIns.clear();
        lastCompassTargets.clear();
        safePadNavigation.clear();
        curSafe = 1;
        phaseTimer = 60;
        phaseTimerStart = 60;
        centerSafeZoneDecay = 11;
        firstClaimedThisPhase = false;
        soloMode = false;
        stats.reset();
        firstPadPlayers.clear();
        outOfCupTargets.clear();
        photoFinishCandidates.clear();

        // Freeze the participant list before generation so late entrants are not reset or counted.
        final List<Player> startingPlayers = testplexManagedRound && pendingTestplexParticipants != null
                ? new ArrayList<>(pendingTestplexParticipants) : getArenaWorldPlayers();
        pendingTestplexParticipants = null;
        roundParticipants.clear();
        for (Player player : startingPlayers) {
            if (player != null && player.isOnline() && player.getGameMode() != GameMode.SPECTATOR) {
                // Keep players in Testplex's shared lobby while the maze is generated.
                // They enter the arena only when the game is ready to start.
                roundParticipants.add(player.getUniqueId());
            }
        }

        // Sync active mode from the plugin's persisted config each game.
        this.mode = plugin.getMode();
        phaseTimer = stageTimer(1);
        phaseTimerStart = phaseTimer;

        state = GameState.STARTING;
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Monster Maze ===");
        broadcast(ChatColor.YELLOW + "Generating maze... (server stays responsive)");

        // Chunked generation — continues when maze is ready
        mazeGenerator.generateMazeAsync(requestedPattern, new Runnable() {
            @Override
            public void run() {
                if (state != GameState.STARTING) return;

                for (Player p : startingPlayers) {
                    if (p == null || !p.isOnline()) {
                        if (p != null) roundParticipants.remove(p.getUniqueId());
                        continue;
                    }
                    UUID playerId = p.getUniqueId();
                    if (!roundParticipants.contains(playerId) || p.getGameMode() == GameMode.SPECTATOR) {
                        roundParticipants.remove(playerId);
                        continue;
                    }
                    if (testplexManagedRound) {
                        p.closeInventory();
                        p.teleport(center.clone().add(0.5D, 1.0D, 0.5D));
                    }
                    if (!isInArenaWorld(center == null ? null : center.getWorld(), p.getWorld())) {
                        roundParticipants.remove(playerId);
                        continue;
                    }
                    alive.add(playerId);
                    preparePlayer(p);
                    p.teleport(center.clone().add(0.5, 1, 0.5));
                }

                // Selected kits were chosen in Testplex's shared Arcade lobby.
                // preparePlayer() applies that selection; no Monster Maze lobby/menu is created.

                if (alive.isEmpty()) {
                    forceStop();
                    return;
                }

                soloMode = alive.size() == 1;
                if (soloMode) {
                    broadcast(ChatColor.AQUA + "Solo mode: survive as long as you can!");
                }

                spawnSafePad();
                safePad = nextSafePad;
                nextSafePad = null;
                rebuildSafePadNavigation();
                launchPadFirework(safePad);

                // Point every alive player's compass at the active safe pad / beacon from spawn.
                updateCompasses();

                // --- FIX: INITIALIZE & APPLY SCOREBOARD UPFRONT (Issue #5) ---
                scoreboard.create();
                List<GameScoreboard.PlayerDistance> distanceStandings = updateDistanceStandings(getAlivePlayers());
                int pattern = getPatternIndex();
                for (Player p : getAlivePlayers()) {
                    String pbText = null;
                    if (pattern >= 0) {
                        me.monstermaze.stats.LeaderboardManager.PBInfo best =
                                plugin.getLeaderboards().getBest(plugin.getMode(), pattern, p.getUniqueId());
                        if (best != null) {
                            String kitName = best.kit != null ? " (" + best.kit + ")" : "";
                            pbText = best.stage + kitName;
                        }
                    }
                    scoreboard.update(p, getAlivePlayers().size(), curSafe, phaseTimer, safePad != null,
                            getMode().color + getMode().id, pbText, distanceStandings);
                }
                scoreboard.apply(getAlivePlayers());
                // -------------------------------------------------------------

                // Spawn maze monsters now (behind the containment glass) so players see them
                // during the countdown; their movement only begins once the game goes LIVE
                // (MonsterManager.move() is gated on live state, and dropContainment() runs
                // at the top of beginLive() so the glass is gone before they start advancing).
                monsterManager.start(mazeGenerator);

                // Lock jumping during the STARTING countdown too (pre-glass-fall): only the
                // Jumper kit can move around the spawn; everyone else is grounded. The LIVE
                // mainTask calls tickJumpLock() once the game starts, so we stop this one then.
                startingTask = Bukkit.getScheduler().runTaskTimer(plugin.getHostPlugin(), new Runnable() {
                    @Override public void run() {
                        if (state != GameState.STARTING) return;
                        kitManager.tickJumpLock();
                    }
                }, 1L, 1L);

                new BukkitRunnable() {
                    int t = 3;
                    @Override public void run() {
                        if (state != GameState.STARTING) { cancel(); return; }
                        if (t <= 0) { beginLive(); cancel(); return; }
                        broadcast(ChatColor.YELLOW + "Starting in " + ChatColor.WHITE + t + "...");
                        titleArenaPlayers("", ChatColor.YELLOW + "" + t, 0, 15, 5);
                        t--;
                    }
                }.runTaskTimer(plugin.getHostPlugin(), 10L, 20L);
            }
        });
    }

    private void beginLive() {
        state = GameState.LIVE;
        if (startingTask != null) { startingTask.cancel(); startingTask = null; }
        liveStartMs = System.currentTimeMillis();
        mazeGenerator.dropContainment();
        for (Player p : getAlivePlayers()) p.closeInventory();

        titleArenaPlayers("", ChatColor.YELLOW + "" + ChatColor.BOLD + "Get to the Safe Pad!", 5, 40, 5);
        broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "GO!");

        secondTask = Bukkit.getScheduler().runTaskTimer(plugin.getHostPlugin(), new Runnable() {
            @Override public void run() {
                if (state != GameState.LIVE) return;
                decrementSafePadTime();
                decrementPhaseTime();
                if (System.currentTimeMillis() - liveStartMs >= 20000L) {
                    deteriorateCenter();
                }
            }
        }, 20L, 20L);

        mainTask = Bukkit.getScheduler().runTaskTimer(plugin.getHostPlugin(), new Runnable() {
            @Override public void run() {
                if (state != GameState.LIVE) return;
                liveTick++;
                List<Player> aliveNow = getAlivePlayers();
                checkPlayersOnSafePad();
                updateCompasses();
                updateExpBars(aliveNow);
                kitManager.tickJumperFlight();
                kitManager.tickJumpLock();

                // PERF: the scoreboard/KB/pad info only changes on stage/phase transitions
                // (and the PB lookup is per player), so update it 2x/s instead of 20x/s.
                if ((liveTick % 10) == 1) {
                    List<GameScoreboard.PlayerDistance> distanceStandings = updateDistanceStandings(aliveNow);
                    if (curSafe == 1) emitStageOneGuidance(aliveNow);
                    int pattern = getPatternIndex();
                    int aliveCount = aliveNow.size();
                    for (Player p : aliveNow) {
                        String pbText = null;
                        if (pattern >= 0) {
                            me.monstermaze.stats.LeaderboardManager.PBInfo best =
                                    plugin.getLeaderboards().getBest(plugin.getMode(), pattern, p.getUniqueId());
                            if (best != null) {
                                String kitName = best.kit != null ? " (" + best.kit + ")" : "";
                                pbText = best.stage + kitName;
                            }
                        }
                        scoreboard.update(p, aliveCount, curSafe, phaseTimer, safePad != null,
                                getMode().color + getMode().id, pbText, distanceStandings);
                    }
                    scoreboard.apply(aliveNow);
                }
            }
        }, 1L, 1L);
    }

    public void forceStop() {
        state = GameState.ENDING;
        if (mainTask != null) { mainTask.cancel(); mainTask = null; }
        if (secondTask != null) { secondTask.cancel(); secondTask = null; }
        if (startingTask != null) { startingTask.cancel(); startingTask = null; }

        monsterManager.stop();
        feedback.clear();
        safePadNavigation.clear();
        padLockIns.clear();
        lastCompassTargets.clear();
        playersOnPad.clear();
        destroyAllPads();
        List<Player> arenaPlayers = getRoundPlayersForCleanup();
        mazeGenerator.cleanup();
        scoreboard.clear(arenaPlayers);

        for (Player p : arenaPlayers) {
            p.setExp(0); p.setLevel(0);
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            kitManager.resetPlayerState(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.setCollidable(true);
            
            // Restore visibility for all players in lobby
            for (Player target : arenaPlayers) {
                p.showPlayer(target);
            }

            if (testplexReturnToLobby != null) testplexReturnToLobby.accept(p);
            else plugin.getHostPlugin().returnPlayerToArcadeLobby(p);
        }
        alive.clear();
        spectators.clear();
        state = GameState.IDLE;
        // Testplex owns the only player lobby. The standalone Monster Maze lobby
        // remains available only for isolated module tests.
        if (testplexManagedRound) {
            for (Player p : arenaPlayers) {
                if (p.isOnline()) p.sendMessage(ChatColor.GREEN + "Monster Maze finished. Returned to the Testplex Arcade lobby.");
            }
        }
        roundParticipants.clear();
        testplexManagedRound = false;
        testplexReturnToLobby = null;
    }

    private void cleanupEntities() {
        monsterManager.stop();
        destroyAllPads();
    }

    // -------------------- Safe pad pipeline (original-style) --------------------

    private List<Location> avoidPadLocations() {
        List<Location> avoid = new ArrayList<Location>();
        if (safePad != null) avoid.add(safePad.getLocation());
        for (SafePad p : oldSafePads) avoid.add(p.getLocation());
        if (nextSafePad != null) avoid.add(nextSafePad.getLocation());
        return avoid;
    }

    /** Build next pad (preview). */
    public void spawnSafePad() {
        Location next = mazeGenerator.randomPadLocation(avoidPadLocations());
        nextSafePad = new SafePad(next, qolEnabled());
        mazeGenerator.disablePadArea(next);

        // Remove mobs standing on the new pad
        monsterManager.removeMonstersOn(nextSafePad);

        Bukkit.getPluginManager().callEvent(new SafepadBuildEvent());
    }

    private void stopSafePad() {
        if (safePad != null) {
            safePad.turnOffBeacon();
            oldSafePads.add(safePad);
            safePad = null;
        }
    }

    private void decrementSafePadTime() {
        Iterator<SafePad> it = oldSafePads.iterator();
        while (it.hasNext()) {
            SafePad pad = it.next();
            if (!pad.decay()) continue;
            mazeGenerator.enablePadArea(pad.getLocation());
            it.remove();
        }
    }

    /** Timer length (seconds) for a given stage, depending on the active mode.
     *  Original: 60 -> 15 floor (minus 2s per prior stage).
     *  Modern:  35 -> 15 floor, reached by stage 10.
     */
    private int stageTimer(int stage) {
        return mode.stageTimerSeconds(stage);
    }

    private void decrementPhaseTime() {
        if (safePad == null) return;
        if (phaseTimer == -1) return;

        phaseTimer--;

        if (phaseTimer == 20 || phaseTimer == 15 || phaseTimer == 10
                || phaseTimer == 5 || phaseTimer == 4) {
            titleArenaPlayers("", ChatColor.GREEN + "" + ChatColor.BOLD + phaseTimer, 5, 40, 5);
        }
        if (phaseTimer == 3) {
            titleArenaPlayers("", ChatColor.YELLOW + "" + ChatColor.BOLD + phaseTimer, 5, 40, 5);
        }
        if (phaseTimer == 2) {
            titleArenaPlayers("", ChatColor.GOLD + "" + ChatColor.BOLD + phaseTimer, 5, 40, 5);
            spawnSafePad(); // next pad appears
        }
        if (phaseTimer == 1) {
            titleArenaPlayers("", ChatColor.RED + "" + ChatColor.BOLD + phaseTimer, 5, 40, 5);
        }

        if (phaseTimer == 0) {
            for (Player p : getAlivePlayers()) {
                boolean onPad = safePad != null && safePad.isOn(p) && p.isOnGround();
                if (onPad) padLockIns.add(p.getUniqueId());
                boolean secured = padLockIns.contains(p.getUniqueId());

                if (secured) {
                    if (!onPad) teleportToSafePadCenter(p);
                    TextUtil.title(p, "", ChatColor.YELLOW + "" + ChatColor.BOLD + "Get to the Next Safe Pad!", 5, 40, 5);
                } else {
                    TextUtil.title(p, "", ChatColor.RED + "" + ChatColor.BOLD + "You weren't on the Safe Pad!", 5, 40, 5);
                    p.sendMessage(ChatColor.RED + "You weren't on the Safe Pad!");
                    eliminate(p, ChatColor.RED + p.getName() + " missed the Safe Pad!");
                }
            }

            monsterManager.spawnMore(getMode().additionalMonstersPerSafePad());
            stopSafePad();
            playersOnPad.clear();
            padLockIns.clear();
            firstClaimedThisPhase = false;

            phaseTimerStart = stageTimer(curSafe + 1);
            phaseTimer = phaseTimerStart;

            if (nextSafePad == null) spawnSafePad();

            monsterManager.removeMonstersOn(nextSafePad);

            curSafe++;
            safePad = nextSafePad;
            nextSafePad = null;
            rebuildSafePadNavigation();
            if (curSafe <= 2) launchPadFirework(safePad);

            checkWin();
        }
    }

    private void checkPlayersOnSafePad() {
        if (safePad == null || state != GameState.LIVE) return;

        boolean allOn = true;
        for (Player p : getAlivePlayers()) {
            UUID id = p.getUniqueId();
            boolean onPad = safePad.isOn(p);
            boolean grounded = p.isOnGround();

            if (onPad && grounded) padLockIns.add(id);
            if (onPad && grounded && phaseTimer <= 1) {
                boolean rivalNearby = false;
                for (Player rival : getAlivePlayers()) {
                    if (!rival.equals(p) && rival.getLocation().distance(p.getLocation()) <= 3.0D) {
                        rivalNearby = true;
                        break;
                    }
                }
                if (rivalNearby) notePhotoFinish(p);
            }
            if (!onPad || !grounded) {
                allOn = false;
                if (!onPad && padLockIns.contains(id) && grounded) {
                    teleportToSafePadCenter(p);
                    continue;
                }
                if (!onPad && grounded && playersOnPad.contains(p) && !padLockIns.contains(id)) {
                    TextUtil.title(p, "", ChatColor.RED + "" + ChatColor.BOLD + "Get back to the Safe Pad!", 0, 5, 0);
                }
                continue;
            }

            // Reaching the pad requires landing. Jumping after that does not clear padLockIns.
            stats.checkPilotLand(p, true);

            if (!playersOnPad.contains(p)) {
                playersOnPad.add(p);
                boolean first = !firstClaimedThisPhase;
                if (first) {
                    firstClaimedThisPhase = true;
                    Bukkit.getPluginManager().callEvent(new FirstToSafepadEvent(p));
                    broadcast(ChatColor.GREEN + p.getName() + ChatColor.YELLOW + " reached the Safe Pad first!");
                    // Keep the existing first-arrival cue and add a softer arena-wide ding.
                    for (Player listener : Bukkit.getOnlinePlayers()) {
                        if (listener.getWorld() == p.getWorld()) {
                            listener.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.45f, 1.8f);
                        }
                    }
                    int decreased = Math.max(6, 16 - (curSafe - 1));
                    phaseTimer = Math.min(decreased, phaseTimer);
                    for (Player other : getAlivePlayers()) {
                        if (other.equals(p)) continue;
                        other.sendMessage(ChatColor.YELLOW + "You have " + ChatColor.WHITE
                                + decreased + " Seconds" + ChatColor.YELLOW + " to make it to the Safe Pad!");
                    }
                } else {
                    p.sendMessage(ChatColor.YELLOW + "You made it to the Safe Pad!");
                }
                kitManager.onReachedPad(p, first);
                feedback.showPadReward(p, first);
            }
        }

        if (allOn && !getAlivePlayers().isEmpty()) {
            phaseTimer = Math.min(4, phaseTimer);
        }
    }

    // -------------------- Center deterioration --------------------

    @SuppressWarnings("deprecation")
    private void deteriorateCenter() {
        List<Location> zone = mazeGenerator.getCenterSafeZone();
        if (zone.isEmpty() || centerSafeZoneDecay == -1) return;

        centerSafeZoneDecay--;
        Material clay = Material.LIME_TERRACOTTA;
        if (centerSafeZoneDecay <= 8 && centerSafeZoneDecay > 6) clay = Material.YELLOW_TERRACOTTA;
        else if (centerSafeZoneDecay <= 6 && centerSafeZoneDecay > 4) clay = Material.ORANGE_TERRACOTTA;
        else if (centerSafeZoneDecay <= 4 && centerSafeZoneDecay > 1) clay = Material.RED_TERRACOTTA;

        Iterator<Location> it = zone.iterator();
        while (it.hasNext()) {
            Location cur = it.next();
            Block floor = cur.getBlock().getRelative(0, -1, 0);
            if (centerSafeZoneDecay == 1) {
                it.remove();
                if (mazeGenerator.getCenterSafeZonePaths().contains(cur)) {
                    // Real path cell: rebuild as the active map's actual maze surface.
                    me.monstermaze.maze.MazeBlockData theme = plugin.getMapManager().activeTheme();
                    floor.setType(theme.top, false);
                    mazeGenerator.enableWaypoint(cur);
                } else {
                    // Decorative/barrier center cell: falls away into the void.
                    floor.setType(Material.AIR, false);
                }
            } else if (floor.getType() == Material.LIME_TERRACOTTA
                    || floor.getType() == Material.YELLOW_TERRACOTTA
                    || floor.getType() == Material.ORANGE_TERRACOTTA
                    || floor.getType() == Material.RED_TERRACOTTA
                    || floor.getType() == Material.QUARTZ_BLOCK) {
                floor.setType(clay, false);
            }
        }

        if (centerSafeZoneDecay == 1) centerSafeZoneDecay = -1;
    }

    // -------------------- Players --------------------

    public boolean isOnAnyPad(Player player) {
        if (safePad != null && safePad.isActive() && safePad.isOn(player)) return true;
        if (nextSafePad != null && nextSafePad.isActive() && nextSafePad.isOn(player)) return true;
        for (SafePad pad : oldSafePads) {
            if (pad.isActive() && pad.isOn(player)) return true;
        }
        return false;
    }

    /** The pad mobs should direct a Maverick player toward (active pad, else the preview pad). */
    public Location getMobKnockTarget() {
        SafePad t = safePad != null ? safePad : nextSafePad;
        return t == null ? null : t.getLocation();
    }

    private void destroyAllPads() {
        if (safePad != null) { safePad.destroy(); safePad = null; }
        if (nextSafePad != null) { nextSafePad.destroy(); nextSafePad = null; }
        for (SafePad pad : oldSafePads) pad.destroy();
        oldSafePads.clear();
    }

    private void preparePlayer(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setCollidable(false);
        kitManager.resetPlayerState(p);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        try { p.setSaturation(20f); } catch (Throwable ignored) {}
        p.setFireTicks(0);
        p.setExp(0.99f);
        p.setLevel(0);
        
        // Ensure all alive players are visible to each other
        for (Player online : getRoundAudiencePlayers()) {
            p.showPlayer(online);
            online.showPlayer(p);
        }

        kitManager.applyKit(p);
    }

    public List<Player> getAlivePlayers() {
        List<Player> list = new ArrayList<Player>();
        for (UUID id : new HashSet<UUID>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) list.add(p);
            else alive.remove(id);
        }
        return list;
    }

    /** Record this player's highest stage reached for the active mode+pattern (persisted). */
    private void recordPB(Player player) {
        int pattern = getPatternIndex();
        if (pattern < 0) return;
        String kit = null;
        me.monstermaze.kit.KitType k = kitManager.getKit(player);
        if (k != null) kit = k.id;
        plugin.getLeaderboards().recordRun(plugin.getMode(), pattern, player.getUniqueId(), curSafe, kit);
        plugin.recordHighestStage(player.getUniqueId(), curSafe);
    }

    private void eliminate(Player player, String message) {
        if (!alive.remove(player.getUniqueId())) return;
        lastCompassTargets.remove(player.getUniqueId());
        padLockIns.remove(player.getUniqueId());
        recordPB(player);
        spectators.add(player.getUniqueId());
        broadcast(message);
        kitManager.resetPlayerState(player);
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);

        // Hide eliminated player from all living players so they don't block screens
        for (UUID aliveId : alive) {
            Player alivePlayer = Bukkit.getPlayer(aliveId);
            if (alivePlayer != null && alivePlayer.isOnline()) {
                alivePlayer.hidePlayer(player);
            }
        }
        
        // Ensure other spectators can still see them
        for (UUID specId : spectators) {
            Player specPlayer = Bukkit.getPlayer(specId);
            if (specPlayer != null && specPlayer.isOnline()) {
                specPlayer.showPlayer(player);
                player.showPlayer(specPlayer);
            }
        }

        // Put eliminated spectators on the maze centre platform (has a floor) so they
        // don't fall into the void; they can fly over to watch the remaining survivors.
        if (center != null) player.teleport(center.clone().add(0, 1, 0));
        checkWin();
    }

    private void checkWin() {
        if (state != GameState.LIVE) return;
        List<Player> remaining = getAlivePlayers();

        // Solo: keep going until the player dies (0 remaining)
        if (soloMode) {
            if (remaining.isEmpty()) {
                state = GameState.ENDING;
                broadcast(ChatColor.GOLD + "Solo run over — reached stage " + curSafe + "!");
                Bukkit.getScheduler().runTaskLater(plugin.getHostPlugin(), new Runnable() {
                    @Override public void run() { forceStop(); }
                }, 80L);
            }
            return;
        }

        // Multiplayer: last player standing wins
        if (remaining.size() <= 1) {
            if (remaining.size() == 1) {
                Player winner = remaining.get(0);
                
                // Record winner's PB BEFORE setting state to ENDING
                recordPB(winner);
                plugin.recordWin(winner.getUniqueId(), curSafe);
                if (photoFinishCandidates.contains(winner.getUniqueId())) {
                    recordAchievement(winner.getUniqueId(), "photo-finish");
                }
                Set<UUID> slowballTargets = outOfCupTargets.getOrDefault(winner.getUniqueId(), Set.of());
                if (slowballTargets.stream().anyMatch(targetId -> roundParticipants.contains(targetId)
                        && !alive.contains(targetId))) {
                    recordAchievement(winner.getUniqueId(), "out-of-the-cup");
                }
                
                broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + winner.getName()
                        + " wins Monster Maze! (Stage " + curSafe + ")");
                TextUtil.title(winner, ChatColor.GOLD + "Victory!", ChatColor.YELLOW + "Stage " + curSafe, 10, 60, 10);
                stats.announceWinners(winner);
                winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            } else {
                broadcast(ChatColor.GOLD + "No winners...");
            }
            
            // Transition state after recording
            state = GameState.ENDING;
            Bukkit.getScheduler().runTaskLater(plugin.getHostPlugin(), new Runnable() {
                @Override public void run() { forceStop(); }
            }, 100L);
        }
    }

    public boolean tryBumpCooldown(Player player) { return true; }

    private void updateCompasses() {
        for (Player player : getAlivePlayers()) {
            // Players who have locked in on this stage's pad are already safe.
            // Once the preview pad appears, point those players at it immediately.
            SafePad target = padLockIns.contains(player.getUniqueId()) && nextSafePad != null
                    ? nextSafePad
                    : (safePad != null ? safePad : nextSafePad);
            if (target == null) continue;

            Location loc = target.getLocation();
            UUID playerId = player.getUniqueId();
            Location last = lastCompassTargets.get(playerId);
            // Each player's target can differ depending on their Safe Pad lock-in.
            if (last != null
                    && last.getWorld() == loc.getWorld()
                    && last.getBlockX() == loc.getBlockX()
                    && last.getBlockY() == loc.getBlockY()
                    && last.getBlockZ() == loc.getBlockZ()) {
                continue;
            }
            player.setCompassTarget(loc);
            lastCompassTargets.put(playerId, loc.clone());
        }
    }

    private void updateExpBars(List<Player> aliveNow) {
        float pct;
        if (safePad == null) {
            pct = 0;
        } else if (playersOnPad.isEmpty()) {
            pct = (float) Math.min(Math.max(phaseTimer * (1.0 / Math.max(1, phaseTimerStart)), 0), 0.999);
        } else {
            pct = (float) Math.min(Math.max(phaseTimer / (double) Math.max(6, 16 - (curSafe - 1)), 0), 0.999);
        }
        int lvl = Math.max(0, phaseTimer);
        // PERF: values change at 1Hz at most; only send the packets when they actually change.
        if (pct == lastExpPct && lvl == lastLevel) return;
        lastExpPct = pct;
        lastLevel = lvl;
        for (Player p : aliveNow) {
            p.setExp(pct);
            p.setLevel(lvl);
        }
    }

    /** Rebuild the exact shortest-path distance field once for each new active Safe Pad. */
    private void rebuildSafePadNavigation() {
        if (safePad == null) {
            safePadNavigation.clear();
            return;
        }
        safePadNavigation.rebuild(mazeGenerator.getPathPoints(), safePad.getLocation());
    }

    private void teleportToSafePadCenter(Player player) {
        if (player == null || safePad == null || !player.isOnline()) return;
        Location from = player.getLocation();
        Location destination = safePad.getLocation().clone();
        destination.setYaw(yawToward(from, destination));
        destination.setPitch(0.0F);
        player.setFallDistance(0.0F);
        player.setVelocity(new Vector(0, 0, 0));
        player.teleport(destination);
    }

    /** Calculate the horizontal Minecraft yaw needed to face from one point toward another. */
    static float yawToward(Location from, Location target) {
        if (from == null || target == null || from.getWorld() == null
                || from.getWorld() != target.getWorld()) return from == null ? 0.0F : from.getYaw();
        double dx = target.getX() - from.getX();
        double dz = target.getZ() - from.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) return from.getYaw();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** The first two active pads get a brief, non-damaging celebration firework. */
    /** First two active Safe Pads get three brighter, harmless fireworks. */
    private void launchPadFirework(SafePad pad) {
        if(pad==null||curSafe>2)return;
        final Location center=pad.getLocation().clone().add(0,1,0);final boolean firstPad=curSafe==1;
        for(int i=0;i<3;i++){
            final int shot=i;
            Bukkit.getScheduler().runTaskLater(plugin.getHostPlugin(),new Runnable(){@Override public void run(){
                Location at=center.clone().add((shot-1)*0.45,0,shot==1?0.35:-0.35);
                try{
                    Firework firework=at.getWorld().spawn(at,Firework.class);FireworkMeta meta=firework.getFireworkMeta();meta.setPower(1);
                    FireworkEffect.Type type=shot==0?FireworkEffect.Type.BALL_LARGE:shot==1?FireworkEffect.Type.STAR:FireworkEffect.Type.BURST;
                    Color primary=firstPad?(shot==0?Color.LIME:shot==1?Color.YELLOW:Color.AQUA):(shot==0?Color.PURPLE:shot==1?Color.FUCHSIA:Color.AQUA);
                    meta.addEffect(FireworkEffect.builder().with(type).withColor(primary,Color.WHITE).withFade(firstPad?Color.YELLOW:Color.LIME).trail(true).flicker(true).build());
                    meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(firstPad?Color.AQUA:Color.YELLOW).trail(true).build());
                    firework.setFireworkMeta(meta);
                }catch(Throwable ignored){
                        for(int n=0;n<8;n++)at.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,at.clone().add((n%3)*0.2,n*0.15,(n%2)*0.2),1,0,0,0,0);
                }
            }},i*3L);
        }
    }

    /** Route particles follow only real maze cells and appear during Stage 1 only. */
    private void emitStageOneGuidance(List<Player> aliveNow) {
        if (safePad == null || !safePadNavigation.isReady()) return;
        for (Player player : aliveNow) {
            if (playersOnPad.contains(player) || safePad.isOn(player)) continue;
            List<Location> markers = safePadNavigation.trailTowardTarget(player.getLocation(), 120, 4);
            for (Location marker : markers) {
                marker.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, marker, 1, 0.05, 0.08, 0.05, 0.0);
            }
        }
    }

    private List<GameScoreboard.PlayerDistance> updateDistanceStandings(List<Player> aliveNow) {
        final List<GameScoreboard.PlayerDistance> lines=new ArrayList<GameScoreboard.PlayerDistance>();
        if(safePad==null||!safePadNavigation.isReady()||aliveNow==null||aliveNow.isEmpty())return lines;
        final List<DistanceEntry> ranking=new ArrayList<DistanceEntry>();
        for(Player player:aliveNow)ranking.add(new DistanceEntry(player,safePadNavigation.distanceInBlocks(player.getLocation())));
        Collections.sort(ranking,new Comparator<DistanceEntry>(){@Override public int compare(DistanceEntry left,DistanceEntry right){
            int distance=Double.compare(left.distance,right.distance);return distance!=0?distance:left.player.getName().compareToIgnoreCase(right.player.getName());
        }});
        for(DistanceEntry entry:ranking) {
            String displayDistance = padLockIns.contains(entry.player.getUniqueId())
                    ? "SAFE" : formatDistance(entry.distance);
            lines.add(new GameScoreboard.PlayerDistance(entry.player.getName(), displayDistance));
        }
        return lines;
    }

    private String formatDistance(double distance) {
        return Double.isInfinite(distance) || Double.isNaN(distance) ? "?" : String.valueOf(Math.round(distance));
    }

    private static final class DistanceEntry {
        final Player player;
        final double distance;
        DistanceEntry(Player player, double distance) { this.player = player; this.distance = distance; }
    }

    /** All online players in this isolated Monster Maze lobby/arena world. */
    private List<Player> getArenaWorldPlayers() {
        List<Player> players = new ArrayList<Player>();
        World arenaWorld = center == null ? null : center.getWorld();
        if (arenaWorld == null) return players;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInArenaWorld(arenaWorld, player.getWorld())) players.add(player);
        }
        return players;
    }

    static boolean isInArenaWorld(World arenaWorld, World playerWorld) {
        return arenaWorld != null && playerWorld != null && arenaWorld == playerWorld;
    }

    static boolean shouldEliminateForWorldChange(GameState state, boolean alive,
            World arenaWorld, World playerWorld) {
        return alive && (state == GameState.STARTING || state == GameState.LIVE)
                && arenaWorld != null && playerWorld != null && arenaWorld != playerWorld;
    }

    static boolean shouldRedirectUnregisteredPlayer(GameState state, boolean registered,
            World arenaWorld, World playerWorld) {
        return !registered && (state == GameState.STARTING || state == GameState.LIVE)
                && isInArenaWorld(arenaWorld, playerWorld);
    }

    static boolean shouldIncludeRoundAudience(boolean registered, boolean online,
            boolean requireArenaWorld, World arenaWorld, World playerWorld) {
        return registered && online
                && (!requireArenaWorld || isInArenaWorld(arenaWorld, playerWorld));
    }

    /** Only players registered when this round started receive its messages and titles. */
    private List<Player> getRoundAudiencePlayers() {
        if (roundParticipants.isEmpty()) {
            return state == GameState.IDLE ? getArenaWorldPlayers() : Collections.<Player>emptyList();
        }
        List<Player> players = new ArrayList<Player>();
        boolean liveAudience = state == GameState.STARTING || state == GameState.LIVE;
        World arenaWorld = center == null ? null : center.getWorld();
        for (UUID id : new HashSet<UUID>(roundParticipants)) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;
            boolean requireArenaWorld = liveAudience
                    && !(testplexManagedRound && state == GameState.STARTING);
            if (!shouldIncludeRoundAudience(true, player.isOnline(), requireArenaWorld,
                    arenaWorld, player.getWorld())) continue;
            players.add(player);
        }
        return players;
    }

    /** Restore only participants captured for this round, wherever they currently are. */
    private List<Player> getRoundPlayersForCleanup() {
        List<Player> players = new ArrayList<Player>();
        for (UUID id : new HashSet<UUID>(roundParticipants)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) players.add(player);
        }
        return players;
    }

    private void titleArenaPlayers(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : getRoundAudiencePlayers()) {
            TextUtil.title(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    private void broadcast(String msg) {
        for (Player player : getRoundAudiencePlayers()) player.sendMessage(msg);
    }

    // -------------------- Events --------------------

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (state != GameState.STARTING && state != GameState.LIVE) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (center == null) return;
        World arenaWorld = center.getWorld();
        if (testplexManagedRound && state == GameState.STARTING && roundParticipants.contains(id)) {
            // Testplex participants intentionally remain in the shared Arcade lobby
            // while the arena is generated; do not treat that as leaving the game.
            return;
        }
        if (shouldRedirectUnregisteredPlayer(state, roundParticipants.contains(id),
                arenaWorld, player.getWorld())) {
            if (testplexReturnToLobby != null) testplexReturnToLobby.accept(player);
            else plugin.getHostPlugin().returnPlayerToArcadeLobby(player);
            return;
        }
        if (!roundParticipants.contains(id)
                || isInArenaWorld(arenaWorld, player.getWorld())) return;
        if (shouldEliminateForWorldChange(state, alive.contains(id), arenaWorld, player.getWorld())) {
            eliminate(player, ChatColor.RED + player.getName() + " left the Monster Maze arena.");
        } else if (state == GameState.STARTING) {
            roundParticipants.remove(id);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        scoreboard.clear(Collections.singletonList(player));
        padLockIns.remove(id);
        lastCompassTargets.remove(id);
        
        for (Player online : getRoundPlayersForCleanup()) {
            online.showPlayer(player);
        }

        boolean wasAlive = alive.remove(id);
        if (wasAlive) recordPB(player);
        spectators.remove(id);
        roundParticipants.remove(id);
        if (wasAlive) {
            broadcast(ChatColor.GRAY + player.getName() + " left.");
            checkWin();
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (state != GameState.LIVE) return;
        Player p = event.getEntity();
        if (!alive.contains(p.getUniqueId())) return;
        event.getDrops().clear();
        event.setDeathMessage(null);
        eliminate(p, ChatColor.RED + p.getName() + " was eliminated!");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!spectators.contains(event.getPlayer().getUniqueId())) return;
        if (center != null) event.setRespawnLocation(center.clone().add(0, 1, 0));
        final Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin.getHostPlugin(), new Runnable() {
            @Override public void run() {
                if (p.isOnline()) p.setGameMode(GameMode.SPECTATOR);
            }
        }, 1L);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event instanceof EntityDamageByEntityEvent
                && ((EntityDamageByEntityEvent) event).getDamager() instanceof Firework) {
            event.setCancelled(true);
            return;
        }
        if (state != GameState.LIVE && state != GameState.STARTING) return;
        Player p = (Player) event.getEntity();
        if (!alive.contains(p.getUniqueId())) { event.setCancelled(true); return; }
        if (state == GameState.STARTING) event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (state != GameState.LIVE) return;
        Player p = event.getPlayer();
        if (!alive.contains(p.getUniqueId())) return;
        if (center == null) return;

        if (p.getLocation().getY() < center.getY() - 3) {
            if (padLockIns.contains(p.getUniqueId()) && safePad != null) {
                teleportToSafePadCenter(p);
                return;
            }
            eliminate(p, ChatColor.RED + p.getName() + " fell off the maze!");
            return;
        }
        // Pilot check: only award when the player actually lands ON a safe pad after a bump.
        // (Previously passed "true" unconditionally, so any grounded landing after a mob hit —
        // even far from a pad — triggered Pilot.)
        if (p.isOnGround()) {
            stats.checkPilotLand(p, isOnAnyPad(p));
        }
    }
}
