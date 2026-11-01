package au.com.testplex.hoopssmoke;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Disposable Paper 26.3 acceptance harness. It starts two real 2v2 Arcade
 * sessions with protocol clients, exercises pass/catch/assist/two-point
 * scoring, then steals/three-point shooting/block/rebound, and verifies that
 * the shared Arcade host returns every player to the canonical lobby state.
 */
public final class HoopsPaperSmoke extends JavaPlugin {
    private static final List<String> CLIENT_NAMES =
            List.of("jn_playzmc", "HoopsSmoke2", "HoopsSmoke3", "HoopsSmoke4");
    private static final long MAX_WAIT_TICKS = 1_200L;
    private static final double GRAVITY = 0.045D;

    private final Map<UUID, Baseline> baselines = new HashMap<>();
    private BukkitTask task;
    private Object arcade;
    private Object manager;
    private Object runtime;
    private Player owner;
    private List<Player> players = List.of();
    private Object firstMatch;
    private Object secondMatch;
    private UUID firstDisplayId;
    private UUID secondDisplayId;
    private UUID firstPasserId;
    private UUID passPasserId;
    private UUID passReceiverId;
    private UUID firstShooterId;
    private UUID blockerId;
    private int firstScoringTeam = -1;
    private int stage;
    private long ticks;
    private boolean closing;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        getLogger().info("HOOPS_SMOKE|WAITING|four real Minecraft 26.3 clients");
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 5L);
    }

    private void tick() {
        if (closing) return;
        ticks++;
        try {
            switch (stage) {
                case 0 -> waitForClientsAndStartFirstMatch();
                case 1 -> verifyFirstMatchAndPass();
                case 2 -> waitForPassCatchAndShootTwo();
                case 3 -> waitForTwoPointWinAndStartSecondMatch();
                case 4 -> waitForSecondMatchAndTestStealBlock();
                case 5 -> waitForBlockedShotReboundAndStop();
                case 6 -> verifyFinalRestoration();
                default -> fail("unknown test stage " + stage);
            }
        } catch (Throwable failure) {
            fail(rootCause(failure));
        }
    }

    private void waitForClientsAndStartFirstMatch() throws Exception {
        if (arcade == null) {
            arcade = Bukkit.getPluginManager().getPlugin("ArcadeMechanicsModern");
            require(arcade != null && ((JavaPlugin) arcade).isEnabled(),
                    "ArcadeMechanicsModern is not enabled");
            manager = field(arcade, "manager");
            require(manager != null, "shared Arcade manager was not created");
            runtime = field(manager, "hoops");
            require(runtime != null, "HoopsRemasteredRuntime was not registered");
        }

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                .filter(player -> CLIENT_NAMES.stream().anyMatch(name -> name.equalsIgnoreCase(player.getName())))
                .toList());
        if (online.size() < CLIENT_NAMES.size()) {
            if (ticks >= MAX_WAIT_TICKS) {
                throw new IllegalStateException("timed out waiting for four clients; online="
                        + online.stream().map(Player::getName).toList());
            }
            return;
        }
        Map<String, Player> byName = new HashMap<>();
        online.forEach(player -> byName.put(player.getName().toLowerCase(), player));
        players = CLIENT_NAMES.stream().map(name -> byName.get(name.toLowerCase())).toList();
        require(players.stream().noneMatch(Objects::isNull), "one or more expected test clients are missing");
        owner = players.get(0);
        owner.setOp(true);

        // The host sanitises enabled-games against its production READY_FOR_TEST
        // list during onEnable. Add Hoops only to this disposable server's
        // in-memory config after that guard has run; never persist this override.
        List<String> acceptanceGames = new ArrayList<>(
                ((JavaPlugin) arcade).getConfig().getStringList("enabled-games"));
        if (!acceptanceGames.contains("hoops-remastered")) acceptanceGames.add("hoops-remastered");
        ((JavaPlugin) arcade).getConfig().set("enabled-games", acceptanceGames);

        invoke(arcade, "prepareForHoops", new Class<?>[0]);
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            seedBaselineProfile(player, i);
            baselines.put(player.getUniqueId(), Baseline.capture(player));
        }
        queueAndStart();
        stage = 1;
        ticks = 0L;
        getLogger().info("HOOPS_SMOKE|QUEUED|players=4|format=2v2");
    }

    private void queueAndStart() throws Exception {
        for (Player player : players) {
            require(Bukkit.dispatchCommand(player, "arcadeport join hoops-remastered"),
                    player.getName() + " could not join Hoops queue");
        }

        // The public /start path pads normal acceptance matches to 16 players
        // through FakePlayerPlugin. This disposable smoke fixture needs exactly
        // four real clients, so use the manager's already-validated internal
        // start after proving startup fake-player cleanup completed. Queue
        // membership, snapshots, team assignment, game lifecycle and finish
        // still run through ArcadePortManager.
        Object testPlayers = field(manager, "testPlayers");
        require(testPlayers != null, "shared Arcade test-player manager is unavailable");
        int managedTestPlayers = ((Number) invoke(testPlayers, "activeCount", new Class<?>[0])).intValue();
        require(managedTestPlayers == 0,
                "disposable four-client fixture unexpectedly has " + managedTestPlayers + " managed fake player(s)");
        for (Player online : Bukkit.getOnlinePlayers()) {
            require(!Boolean.TRUE.equals(invoke(testPlayers, "isTestPlayer",
                    new Class<?>[] {Player.class}, online)),
                    "disposable four-client fixture contains an online Testplex fake player: " + online.getName());
        }

        Class<?> gameType = Class.forName("au.com.testplex.arcadeports.ArcadePortGame");
        Class<?> senderType = Class.forName("org.bukkit.command.CommandSender");
        Object hoopsGame = java.util.Arrays.stream(gameType.getEnumConstants())
                .filter(value -> "HOOPS_REMASTERED".equals(String.valueOf(value)))
                .findFirst().orElseThrow(() -> new IllegalStateException("Hoops Remastered is missing from ArcadePortGame"));
        invoke(manager, "startQueuedInternal",
                new Class<?>[] {gameType, Player.class, senderType, boolean.class},
                hoopsGame, owner, owner, false);
    }

    private void waitForMatch() throws Exception {
        Object current = field(runtime, "match");
        if (current != null) {
            if (firstMatch == null) firstMatch = current;
            else secondMatch = current;
            return;
        }
        if (ticks >= MAX_WAIT_TICKS) throw new IllegalStateException("Hoops session did not start");
    }

    private void verifyFirstMatchAndPass() throws Exception {
        waitForMatch();
        if (firstMatch == null) return;
        World world = (World) field(firstMatch, "world");
        require(world != null, "Hoops did not create an isolated arena world");
        require(field(firstMatch, "players") instanceof Map<?, ?> roster && roster.size() == 4,
                "Hoops roster does not contain all four clients");
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> teams = (Map<UUID, Integer>) field(firstMatch, "teams");
        int[] teamCounts = new int[2];
        for (Player player : players) {
            require(player.getWorld().equals(world), player.getName() + " was not sent to the Hoops arena");
            int team = teams.getOrDefault(player.getUniqueId(), -1);
            require(team == 0 || team == 1, "invalid team assignment for " + player.getName());
            teamCounts[team]++;
            require(player.getAllowFlight(), "Hops flight was not enabled for " + player.getName());
            require(player.getScoreboard() == field(firstMatch, "scoreboard"),
                    "match scoreboard was not applied to " + player.getName());
        }
        require(teamCounts[0] == 2 && teamCounts[1] == 2,
                "four-player queue was not balanced 2v2: RED=" + teamCounts[0] + " BLUE=" + teamCounts[1]);
        require(world.getBlockAt(10, 63, 10).getType() == Material.BIRCH_PLANKS,
                "generated hardwood court is missing");
        require(world.getBlockAt(-23, 67, -3).getType() == Material.RED_STAINED_GLASS
                        && world.getBlockAt(23, 67, -3).getType() == Material.LIGHT_BLUE_STAINED_GLASS,
                "both source-coloured stained-glass backboards must be present at x=-23/+23");
        require(((BossBar) field(firstMatch, "bossBar")).isVisible(), "live boss bar is not visible");
        TextDisplay display = (TextDisplay) field(firstMatch, "scoreboardDisplay");
        require(display != null && display.isValid(), "floating centre-court scoreboard was not spawned");
        firstDisplayId = display.getUniqueId();
        int[] score = (int[]) field(firstMatch, "teamScore");
        require(score[0] == 0 && score[1] == 0, "new match did not start at 0-0");

        UUID holderId = (UUID) field(firstMatch, "holderId");
        Player passer = player(holderId);
        require(passer != null, "tip-off did not assign ball possession");
        Player receiver = players.stream()
                .filter(player -> teams.get(player.getUniqueId()).equals(teams.get(passer.getUniqueId())))
                .filter(player -> !player.equals(passer)).findFirst().orElseThrow(
                        () -> new IllegalStateException("the ball carrier has no teammate"));

        // Exercise the actual PlayerToggleFlightEvent handler and its eight-second recharge.
        PlayerToggleFlightEvent flight = new PlayerToggleFlightEvent(passer, false);
        Bukkit.getPluginManager().callEvent(flight);
        require(flight.isCancelled(), "Hops flight event was not intercepted");
        require(!passer.getAllowFlight() && passer.getVelocity().getY() >= 0.7D,
                "Hops did not launch the carrier");
        @SuppressWarnings("unchecked")
        Map<UUID, Long> hops = (Map<UUID, Long>) field(firstMatch, "hopsReadyAt");
        require(hops.getOrDefault(passer.getUniqueId(), 0L) > System.currentTimeMillis(),
                "Hops cooldown was not recorded");

        int team = teams.get(passer.getUniqueId());
        double startX = team == 0 ? -8.0D : 8.0D;
        double receiveX = team == 0 ? -4.0D : 4.0D;
        Location passFrom = new Location(world, startX, 64.0D, 0.0D);
        passFrom.setDirection(new Vector(team == 0 ? 1.0D : -1.0D, 0.0D, 0.0D));
        passer.teleport(passFrom);
        passer.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        receiver.teleport(new Location(world, receiveX, 64.0D, 0.0D));
        receiver.setVelocity(new Vector(0.0D, 0.0D, 0.0D));

        invoke(runtime, "pass", new Class<?>[] {firstMatch.getClass(), Player.class}, firstMatch, passer);
        Object ball = field(firstMatch, "ball");
        require(ball != null && "PASS".equals(String.valueOf(field(ball, "mode"))),
                "left-click pass did not create a flying pass ball");
        passPasserId = passer.getUniqueId();
        passReceiverId = receiver.getUniqueId();
        Vector velocity = ((Vector) field(ball, "velocity")).clone();
        require(velocity.lengthSquared() > 0.25D, "pass did not launch with meaningful velocity");
        require(velocity.getX() * (team == 0 ? 1.0D : -1.0D) > 0.0D,
                "pass trajectory was not directed toward the selected teammate");
        // The real ball tick runs every server tick, while this acceptance
        // driver polls every five ticks. Let it travel first, then park the
        // intended receiver at the live ball position for a deterministic catch.
        
        stage = 2;
        ticks = 0L;
        getLogger().info("HOOPS_SMOKE|PASS_LAUNCHED|from=" + passer.getName()
                + "|to=" + receiver.getName());
    }

    private void waitForPassCatchAndShootTwo() throws Exception {
        UUID holder = (UUID) field(firstMatch, "holderId");
        if (holder == null) {
            if (ticks >= 40L) throw new IllegalStateException("pass did not result in a recoverable catch");
            Object liveBall = field(firstMatch, "ball");
            Player intendedReceiver = player(passReceiverId);
            require(liveBall != null && intendedReceiver != null,
                    "launched pass or intended receiver disappeared before catch");
            Location livePosition = ((Location) field(liveBall, "location")).clone();
            setField(liveBall, "velocity", new Vector(0.0D, 0.0D, 0.0D));
            intendedReceiver.teleport(livePosition.subtract(0.0D, 1.0D, 0.0D));
            intendedReceiver.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            return;
        }
        require(holder.equals(passReceiverId),
                "pass was caught/intercepted by an unintended player: " + player(holder));
        Player shooter = player(holder);
        require(shooter != null, "pass catch holder is not in the match roster");
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> teams = (Map<UUID, Integer>) field(firstMatch, "teams");
        int team = teams.get(shooter.getUniqueId());
        UUID passerId = (UUID) field(firstMatch, "lastPasserId");
        require(passerId != null && passerId.equals(passPasserId)
                        && !passerId.equals(shooter.getUniqueId()),
                "completed teammate pass did not retain the correct assist candidate");

        // Put the carrier eight blocks from the hoop: a normal two-point shot.
        World world = (World) field(firstMatch, "world");
        double hoopX = team == 0 ? 20.0D : -20.0D;
        Location release = new Location(world, team == 0 ? 12.0D : -12.0D, 64.0D, 0.0D);
        release.setDirection(new Vector(hoopX - release.getX(), 66.2D - 65.62D, 0.0D));
        shooter.teleport(release);
        shooter.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        // Clear every non-shooter out of the first controlled shot lane. The
        // match's normal interception/catch logic remains active; this test
        // should validate the scoring flight rather than a defender left at spawn.
        int safeLane = 0;
        for (Player other : players) {
            if (other.equals(shooter)) continue;
            double safeZ = switch (safeLane++) {
                case 0 -> -12.0D;
                case 1 -> 0.0D;
                default -> 12.0D;
            };
            other.teleport(new Location(world, 0.0D, 64.0D, safeZ));
            other.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        }
        int[] scores = (int[]) field(firstMatch, "teamScore");
        scores[team] = 19;
        firstPasserId = passerId;
        firstShooterId = shooter.getUniqueId();
        firstScoringTeam = team;
        invoke(runtime, "shoot", new Class<?>[] {firstMatch.getClass(), Player.class}, firstMatch, shooter);
        Object shot = field(firstMatch, "ball");
        require(shot != null && "SHOT".equals(String.valueOf(field(shot, "mode"))),
                "right-click shooting did not create a shot trajectory");
        getLogger().info("HOOPS_SHOT_DIAGNOSTIC|launch|team=" + team + "|release=" + shooter.getLocation()
                + "|hoopX=" + hoopX + "|releaseDistance=" + field(shot, "releaseDistance")
                + "|points=" + field(shot, "points") + "|dunk=" + field(shot, "dunk")
                + "|aimMiss=" + shooter.getEyeLocation().getDirection().normalize()
                + "|velocity=" + field(shot, "velocity"));
        double recordedReleaseDistance = ((Number) field(shot, "releaseDistance")).doubleValue();
        int recordedPoints = ((Number) field(shot, "points")).intValue();
        getLogger().info("HOOPS_SHOT_DIAGNOSTIC|team=" + team
                + "|shooter=" + shooter.getName()
                + "|location=" + shooter.getLocation()
                + "|releaseDistance=" + recordedReleaseDistance
                + "|points=" + recordedPoints
                + "|velocity=" + shooter.getVelocity()
                + "|onGround=" + shooter.isOnGround());
        require(Math.abs(recordedReleaseDistance - 8.0D) < 0.05D,
                "shot was not released eight blocks from its defending hoop: " + recordedReleaseDistance);
        require(recordedPoints == 2,
                "eight-block release was not scored as two points: distance=" + recordedReleaseDistance
                        + ", points=" + recordedPoints);
        require(((Boolean) field(shot, "reboundEligible")), "missed-shot recovery was not enabled");
        require(passerId.equals(field(shot, "passerId")), "shot lost its pass/assist attribution");
        Location actualEye = shooter.getEyeLocation();
        getLogger().info("HOOPS_SHOT_DIAGNOSTIC|launch|team=" + team + "|defendingTeam="
                + field(shot, "defendingTeam") + "|hoopX=" + hoopX + "|hoopY=66.2"
                + "|eye=" + actualEye + "|direction=" + actualEye.getDirection()
                + "|velocity=" + field(shot, "velocity") + "|origin=" + field(shot, "origin")
                + "|points=" + field(shot, "points") + "|aimMiss="
                + actualEye.getDirection().normalize().getX() + "/" + actualEye.getDirection().normalize().getY());
        stage = 3;
        ticks = 0L;
        getLogger().info("HOOPS_SMOKE|SHOT_LAUNCHED|type=two-point|shooter=" + shooter.getName());
    }

    private void waitForTwoPointWinAndStartSecondMatch() throws Exception {
        int[] scores = (int[]) field(firstMatch, "teamScore");
        if (ticks > 0 && ticks % 20 == 0) {
            Object liveBall = field(firstMatch, "ball");
            getLogger().info("HOOPS_SHOT_DIAGNOSTIC|progress|ticks=" + ticks
                    + "|score=" + scores[0] + "-" + scores[1]
                    + "|holder=" + field(firstMatch, "holderId")
                    + "|ball=" + (liveBall == null ? "null" : field(liveBall, "mode") + "@"
                            + field(liveBall, "location") + "|v=" + field(liveBall, "velocity")
                            + "|bounces=" + field(liveBall, "bounces"))
                    + "|lastTouch=" + (liveBall == null ? "n/a" : field(liveBall, "lastToucher")));
        }
        if (firstScoringTeam >= 0 && scores[firstScoringTeam] >= 21) {
            @SuppressWarnings("unchecked")
            Map<UUID, Object> stats = (Map<UUID, Object>) field(firstMatch, "playerStats");
            require(firstPasserId != null && firstShooterId != null,
                    "first round lost its saved passer/shooter identities");
            require(((Number) field(stats.get(firstShooterId), "points")).intValue() == 2,
                    "two-point basket was not added to the shooter's box score");
            require(((Number) field(stats.get(firstShooterId), "shotsMade")).intValue() == 1,
                    "made shot count was not incremented");
            require(((Number) field(stats.get(firstPasserId), "assists")).intValue() == 1,
                    "passer did not receive exactly one assist");
            TextDisplay display = (TextDisplay) Bukkit.getEntity(firstDisplayId);
            require(display == null || !display.isValid(), "centre scoreboard display survived match cleanup");
            if (field(runtime, "match") != null || !allRestored()) {
                // The shared Arcade end presentation defaults to 10 seconds before snapshots are restored.
                if (ticks >= 300L) throw new IllegalStateException("winner transition did not restore all players after the full end presentation: " + restorationDiagnostics());
                return;
            }
            verifyCareerStats(firstMatch, firstPasserId, firstShooterId, firstScoringTeam);
            getLogger().info("HOOPS_SMOKE|TWO_POINT_WIN|score=" + scores[0] + "-" + scores[1]
                    + "|assist=PASS|restoration=PASS");

            firstMatch = null;
            invoke(arcade, "prepareForHoops", new Class<?>[0]);
            queueAndStart();
            stage = 4;
            ticks = 0L;
            return;
        }
        if (ticks >= 100L) {
            throw new IllegalStateException("two-point trajectory did not score and end the 21-point game; score="
                    + scores[0] + "-" + scores[1]);
        }
    }

    private void waitForSecondMatchAndTestStealBlock() throws Exception {
        if (secondMatch == null) {
            Object current = field(runtime, "match");
            if (current == null) {
                if (ticks >= 100L) throw new IllegalStateException("back-to-back Hoops match did not start");
                return;
            }
            secondMatch = current;
        }
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> teams = (Map<UUID, Integer>) field(secondMatch, "teams");
        int[] scores = (int[]) field(secondMatch, "teamScore");
        require(scores[0] == 0 && scores[1] == 0, "back-to-back game retained the previous score");
        require(teams.size() == 4, "back-to-back game roster did not retain four players");

        UUID initialHolderId = (UUID) field(secondMatch, "holderId");
        Player initialHolder = player(initialHolderId);
        require(initialHolder != null, "second tip-off did not assign possession");
        int initialTeam = teams.get(initialHolder.getUniqueId());
        Player thief = players.stream()
                .filter(player -> teams.get(player.getUniqueId()) != initialTeam)
                .findFirst().orElseThrow(() -> new IllegalStateException("no opposing defender for steal test"));

        World world = (World) field(secondMatch, "world");
        Location holderLoc = new Location(world, 0.0D, 64.0D, 0.0D);
        Location thiefLoc = new Location(world, 1.0D, 64.0D, 0.0D);
        thiefLoc.setDirection(new Vector(-1.0D, -0.62D, 0.0D));
        initialHolder.teleport(holderLoc);
        thief.teleport(thiefLoc);
        initialHolder.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        thief.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        invoke(runtime, "trySteal", new Class<?>[] {secondMatch.getClass(), Player.class, Player.class},
                secondMatch, thief, initialHolder);
        require(thief.getUniqueId().equals(field(secondMatch, "holderId")),
                "facing/range-valid steal did not transfer possession");
        @SuppressWarnings("unchecked")
        Map<UUID, Object> stats = (Map<UUID, Object>) field(secondMatch, "playerStats");
        require(((Number) field(stats.get(thief.getUniqueId()), "steals")).intValue() == 1,
                "successful steal was not tracked");

        // Move every non-participant defender away from the shot lane.
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.equals(thief) || player.equals(initialHolder)) continue;
            player.teleport(new Location(world, i % 2 == 0 ? -18.0D : 18.0D, 64.0D,
                    i < 2 ? -12.0D : 12.0D));
        }
        double hoopX = teams.get(thief.getUniqueId()) == 0 ? 20.0D : -20.0D;
        Location release = new Location(world, 0.0D, 64.0D, 0.0D);
        release.setDirection(new Vector(hoopX, 66.2D - 65.62D, 0.0D));
        thief.teleport(release);
        thief.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        invoke(runtime, "shoot", new Class<?>[] {secondMatch.getClass(), Player.class}, secondMatch, thief);
        Object shot = field(secondMatch, "ball");
        require(((Integer) field(shot, "points")) == 3, "centre-court release was not classified as three points");
        require((Boolean) field(shot, "reboundEligible"), "three-point shot was not reboundable");

        // The defender meets the airborne shot while looking directly at the ball.
        Location ballLocation = ((Location) field(shot, "location")).clone();
        double defenderOffset = teams.get(thief.getUniqueId()) == 0 ? -1.0D : 1.0D;
        Location defenderLocation = ballLocation.clone().add(defenderOffset, -1.0D, 0.0D);
        Location defenderEye = defenderLocation.clone().add(0.0D, 1.62D, 0.0D);
        defenderLocation.setDirection(ballLocation.toVector().subtract(defenderEye.toVector()));
        initialHolder.teleport(defenderLocation);
        initialHolder.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        invoke(runtime, "blockShot", new Class<?>[] {secondMatch.getClass(), Player.class}, secondMatch, initialHolder);
        require("LOOSE".equals(String.valueOf(field(shot, "mode"))),
                "valid facing block did not deflect the shot into a loose ball");
        require((Boolean) field(shot, "reboundEligible"), "blocked shot was not left recoverable");
        require(((Number) field(stats.get(initialHolder.getUniqueId()), "blocks")).intValue() == 1,
                "successful block was not recorded");
        blockerId = initialHolder.getUniqueId();

        Vector nextVelocity = ((Vector) field(shot, "velocity")).clone();
        nextVelocity.setY(nextVelocity.getY() - GRAVITY);
        Location nextBall = ((Location) field(shot, "location")).clone().add(nextVelocity);
        initialHolder.teleport(nextBall.subtract(0.0D, 1.0D, 0.0D));
        initialHolder.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        secondDisplayId = ((TextDisplay) field(secondMatch, "scoreboardDisplay")).getUniqueId();
        stage = 5;
        ticks = 0L;
        getLogger().info("HOOPS_SMOKE|STEAL_BLOCK|steal=PASS|block=PASS|rebound=WAITING");
    }

    private void waitForBlockedShotReboundAndStop() throws Exception {
        UUID holder = (UUID) field(secondMatch, "holderId");
        @SuppressWarnings("unchecked")
        Map<UUID, Object> stats = (Map<UUID, Object>) field(secondMatch, "playerStats");
        Object blockerStats = stats.get(blockerId);
        if (blockerId != null && holder != null && holder.equals(blockerId)
                && ((Number) field(blockerStats, "rebounds")).intValue() >= 1) {
            long shotClock = ((Number) field(secondMatch, "shotClockEndsAt")).longValue();
            require(shotClock > System.currentTimeMillis(), "rebound did not leave a live possession/shot clock");
            TextDisplay display = (TextDisplay) Bukkit.getEntity(secondDisplayId);
            require(display != null && display.isValid(), "second match scoreboard unexpectedly disappeared before stop");
            invoke(arcade, "prepareForHoops", new Class<?>[0]);
            stage = 6;
            ticks = 0L;
            getLogger().info("HOOPS_SMOKE|REBOUND|PASS|defensive-rebound=PASS");
            return;
        }
        if (ticks >= 50L) {
            throw new IllegalStateException("blocked shot was not recovered by the defender; holder=" + holder
                    + ", rebounds=" + (blockerStats == null ? "n/a" : field(blockerStats, "rebounds")));
        }
    }

    private void verifyFinalRestoration() throws Exception {
        if (field(runtime, "match") != null || !allRestored()) {
            if (ticks >= 100L) throw new IllegalStateException("operator stop did not restore every player");
            return;
        }
        TextDisplay display = (TextDisplay) Bukkit.getEntity(secondDisplayId);
        require(display == null || !display.isValid(), "second centre-court display survived stop cleanup");
        require(firstDisplayId != null && secondDisplayId != null, "missing scoreboard cleanup identities");
        finish("PASS", "four clients; 2v2 balance; Hops; pass/catch/assist; two-point win; "
                + "career persistence; back-to-back reset; steal; three-point release; block; defensive rebound; "
                + "operator-stop restoration");
    }

    private void verifyCareerStats(Object completedMatch, UUID passerId, UUID shooterId, int winningTeam)
            throws Exception {
        int[] scores = (int[]) field(completedMatch, "teamScore");
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> teams = (Map<UUID, Integer>) field(completedMatch, "teams");
        for (Player player : players) {
            String path = "local-stats." + player.getUniqueId() + ".hoops-remastered.";
            require(((JavaPlugin) arcade).getConfig().getInt(path + "games-played", 0) == 1,
                    "career games-played was not saved for " + player.getName());
            if (teams.get(player.getUniqueId()) == winningTeam) {
                require(((JavaPlugin) arcade).getConfig().getInt(path + "wins", 0) == 1,
                        "winning career result was not saved for " + player.getName());
            } else {
                require(((JavaPlugin) arcade).getConfig().getInt(path + "losses", 0) == 1,
                        "losing career result was not saved for " + player.getName());
            }
        }
        String passerPath = "local-stats." + passerId + ".hoops-remastered.";
        String shooterPath = "local-stats." + shooterId + ".hoops-remastered.";
        require(((JavaPlugin) arcade).getConfig().getInt(passerPath + "assists", 0) == 1,
                "lifetime assist was not persisted");
        require(((JavaPlugin) arcade).getConfig().getInt(shooterPath + "points", 0) == 2,
                "lifetime points were not persisted");
        require(scores[winningTeam] >= 21, "final score did not reach 21");
    }

    private boolean allRestored() throws Exception {
        Object lobby = field(manager, "lobby");
        Location lobbySpawn = (Location) invoke(lobby, "spawnLocation", new Class<?>[0]);
        if (lobbySpawn == null) return false;
        for (Player player : players) {
            if (!player.isOnline()) return false;
            Baseline baseline = baselines.get(player.getUniqueId());
            if (baseline == null || !baseline.matches(player)) return false;
            Location location = player.getLocation();
            // The lobby teleports every participant to the same spawn. Real players
            // immediately separate under entity collision, so validate the lobby radius,
            // not identical coordinates.
            if (!location.getWorld().equals(lobbySpawn.getWorld())
                    || location.distanceSquared(lobbySpawn) > 144.0D) return false;
        }
        return true;
    }

    private String restorationDiagnostics() {
        StringBuilder details = new StringBuilder();
        try {
            Object snapshots = field(manager, "endingSnapshots");
            Object endAt = field(manager, "postGameEndsAtMillis");
            Object task = field(manager, "postGameReturnTask");
            details.append("endingSnapshots=").append(snapshots instanceof Map<?, ?> map ? map.size() : "unknown")
                    .append(",postGameEndsAtMillis=").append(endAt)
                    .append(",postGameReturnTask=").append(task != null);
        } catch (Exception failure) {
            details.append("managerStateError=").append(failure.getClass().getSimpleName())
                    .append(':').append(failure.getMessage());
        }
        for (Player player : players) {
            Baseline baseline = baselines.get(player.getUniqueId());
            details.append("|").append(player.getName())
                    .append(":online=").append(player.isOnline())
                    .append(",baseline=").append(baseline != null && baseline.matches(player))
                    .append(",world=").append(player.getWorld().getName())
                    .append(",location=").append(player.getLocation())
                    .append(",mode=").append(player.getGameMode())
                    .append(",allowFlight=").append(player.getAllowFlight())
                    .append(",walk=").append(player.getWalkSpeed())
                    .append(",fly=").append(player.getFlySpeed());
        }
        return details.toString();
    }

    private void seedBaselineProfile(Player player, int variant) {
        World world = player.getWorld();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(16.0D);
        player.setFoodLevel(17);
        player.setSaturation(5.0F);
        player.setLevel(7 + variant);
        player.setExp(0.25F);
        player.setWalkSpeed(0.17F);
        player.setFlySpeed(0.08F);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCompassTarget(world.getSpawnLocation().clone().add(variant + 3.0D, 0.0D, variant + 1.0D));
        player.getInventory().clear();
        player.getInventory().setItem(7, namedItem(Material.DIAMOND, 2 + variant, "Baseline " + variant));
        ItemStack[] armour = {
                namedItem(Material.LEATHER_BOOTS, 1, "Baseline Boots"),
                namedItem(Material.LEATHER_LEGGINGS, 1, "Baseline Legs"),
                namedItem(Material.LEATHER_CHESTPLATE, 1, "Baseline Chest"),
                namedItem(Material.LEATHER_HELMET, 1, "Baseline Helmet")
        };
        player.getInventory().setArmorContents(armour);
    }

    private static ItemStack namedItem(Material material, int amount, String name) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Player player(UUID id) {
        if (id == null) return null;
        return players.stream().filter(player -> player.getUniqueId().equals(id)).findFirst().orElse(null);
    }

    private void fail(Throwable failure) {
        if (closing) return;
        String message = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
        getLogger().severe("HOOPS_SMOKE|FAIL|" + message);
        try {
            if (arcade != null) invoke(arcade, "prepareForHoops", new Class<?>[0]);
        } catch (Throwable cleanup) {
            getLogger().warning("Hoops smoke cleanup also failed: " + cleanup.getMessage());
        }
        finish("FAIL", message);
    }

    private void fail(String message) {
        fail(new IllegalStateException(message));
    }

    private void finish(String result, String details) {
        if (closing) return;
        closing = true;
        if (task != null) task.cancel();
        try {
            Files.writeString(getDataFolder().toPath().resolve("results.tsv"),
                    result + "\tHoopsRemastered\t4\t" + stage + "\t" + details.replace('\t', ' ') + "\n");
        } catch (Exception failure) {
            getLogger().severe("Could not write Hoops smoke result: " + failure.getMessage());
            result = "FAIL";
        }
        getLogger().info("HOOPS_SMOKE|" + result + "|" + details);
        Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 20L);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = findField(target.getClass(), name);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.set(target, value);
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static Object invoke(Object target, String methodName,
            Class<?>[] parameterTypes, Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }

    private static Field findField(Class<?> type, String name) throws Exception {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Field field = cursor.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cursor = failure;
        while (cursor.getCause() != null && cursor.getCause() != cursor) cursor = cursor.getCause();
        return cursor;
    }

    private record Baseline(World world, Location location, ItemStack[] inventory, ItemStack[] armour,
                            GameMode gameMode, double health, int food, float saturation,
                            int level, float exp, float walkSpeed, float flySpeed,
                            boolean allowFlight, boolean flying, Location compassTarget,
                            Scoreboard scoreboard) {
        static Baseline capture(Player player) {
            return new Baseline(player.getWorld(), player.getLocation().clone(),
                    cloneItems(player.getInventory().getContents()),
                    cloneItems(player.getInventory().getArmorContents()),
                    player.getGameMode(), player.getHealth(), player.getFoodLevel(),
                    player.getSaturation(), player.getLevel(), player.getExp(),
                    player.getWalkSpeed(), player.getFlySpeed(), player.getAllowFlight(),
                    player.isFlying(), player.getCompassTarget() == null ? null : player.getCompassTarget().clone(),
                    player.getScoreboard());
        }

        boolean matches(Player player) {
            // The shared Arcade manager restores the snapshot and then sends the
            // player through ModernArcadeLobby.sendToLobby. That intentionally
            // sanitises survival state and installs the two canonical lobby items.
            return player.getWorld().equals(world)
                    && player.getGameMode() == GameMode.ADVENTURE
                    && Math.abs(player.getHealth() - player.getMaxHealth()) < 0.05D
                    && player.getFoodLevel() == 20
                    && Math.abs(player.getSaturation() - 20.0F) < 0.05F
                    && hasCanonicalLobbyInventory(player)
                    && empty(player.getInventory().getArmorContents())
                    && (player.getInventory().getItemInOffHand() == null
                        || player.getInventory().getItemInOffHand().getType().isAir())
                    && player.getLevel() == 0
                    && Math.abs(player.getExp()) < 0.001F
                    && Math.abs(player.getWalkSpeed() - 0.2F) < 0.001F
                    && Math.abs(player.getFlySpeed() - flySpeed) < 0.001F
                    && !player.getAllowFlight()
                    && !player.isFlying()
                    && hasCanonicalLobbyScoreboard(player)
                    && ((compassTarget == null && player.getCompassTarget() == null)
                        || (compassTarget != null && player.getCompassTarget() != null
                            && player.getCompassTarget().getWorld().equals(compassTarget.getWorld())
                            && player.getCompassTarget().distanceSquared(compassTarget) < 0.01D));
        }

        private static boolean hasCanonicalLobbyInventory(Player player) {
            ItemStack[] items = player.getInventory().getStorageContents();
            for (int slot = 0; slot < items.length; slot++) {
                ItemStack item = items[slot];
                if (slot == 4) {
                    if (item == null || item.getType() != Material.CHEST) return false;
                } else if (slot == 8) {
                    if (item == null || item.getType() != Material.GLISTERING_MELON_SLICE) return false;
                } else if (item != null && !item.getType().isAir()) {
                    return false;
                }
            }
            return true;
        }

        private static boolean hasCanonicalLobbyScoreboard(Player player) {
            Scoreboard current = player.getScoreboard();
            if (current == null) return false;
            Objective sidebar = current.getObjective(DisplaySlot.SIDEBAR);
            return sidebar != null && "testplex_lobby".equals(sidebar.getName());
        }

        private static boolean empty(ItemStack[] contents) {
            for (ItemStack item : contents) if (item != null && !item.getType().isAir()) return false;
            return true;
        }

        private static ItemStack[] cloneItems(ItemStack[] input) {
            ItemStack[] result = new ItemStack[input.length];
            for (int i = 0; i < input.length; i++) result[i] = input[i] == null ? null : input[i].clone();
            return result;
        }

        private static boolean itemsEqual(ItemStack[] actual, ItemStack[] expected) {
            if (actual.length != expected.length) return false;
            for (int i = 0; i < actual.length; i++) {
                if (!Objects.equals(actual[i], expected[i])) return false;
            }
            return true;
        }
    }
}
