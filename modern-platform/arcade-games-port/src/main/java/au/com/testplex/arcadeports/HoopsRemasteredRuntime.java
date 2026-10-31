package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

/**
 * Modern Hoops match runtime. The Arcade manager owns the outer session and
 * player snapshots; this class owns basketball possession, ball flight, scoring,
 * the clock, match HUD and Hoops-specific career progression.
 */
public final class HoopsRemasteredRuntime implements Listener {
    public interface Hooks {
        void finish(Player representativeWinner, boolean announceWinner);
    }

    private static final int FLOOR_Y = 63;
    private static final double LEFT_HOOP_X = -20.0D;
    private static final double RIGHT_HOOP_X = 20.0D;
    private static final double HOOP_Y = 66.2D;
    private static final double BALL_GRAVITY = 0.045D;
    private static final String GAME_ID = "hoops-remastered";
    private static final String[] STAT_KEYS = {"points", "assists", "rebounds", "blocks", "steals"};

    private final JavaPlugin plugin;
    private final Hooks hooks;
    private final NamespacedKey ballItemKey;
    private Match match;

    public HoopsRemasteredRuntime(JavaPlugin plugin, Hooks hooks) {
        this.plugin = plugin;
        this.hooks = hooks;
        this.ballItemKey = new NamespacedKey(plugin, "hoops-basketball");
    }

    public static List<Location> spawnLocations(World world, int count) {
        List<Location> result = new ArrayList<>();
        int[] teamCounts = new int[2];
        for (int i = 0; i < Math.max(1, count); i++) {
            int team = HoopsRemasteredRules.teamForSpawnIndex(i);
            int within = teamCounts[team]++;
            double x = HoopsRemasteredRules.spawnX(team);
            float yaw = team == 0 ? -90.0F : 90.0F;
            result.add(new Location(world, x, FLOOR_Y + 1.0D,
                    HoopsRemasteredRules.spawnZ(within), yaw, 0.0F));
        }
        return result;
    }

    /** Build an isolated indoor hardwood court; no production world is touched. */
    public static void buildCourt(World world) {
        for (int x = -24; x <= 24; x++) {
            for (int z = -14; z <= 14; z++) {
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.BIRCH_PLANKS, false);
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 10; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
        // Hardwood boundary and centre line.
        for (int x = -24; x <= 24; x++) {
            world.getBlockAt(x, FLOOR_Y, -14).setType(Material.SMOOTH_QUARTZ, false);
            world.getBlockAt(x, FLOOR_Y, 14).setType(Material.SMOOTH_QUARTZ, false);
        }
        for (int z = -14; z <= 14; z++) {
            world.getBlockAt(-24, FLOOR_Y, z).setType(Material.SMOOTH_QUARTZ, false);
            world.getBlockAt(24, FLOOR_Y, z).setType(Material.SMOOTH_QUARTZ, false);
            world.getBlockAt(0, FLOOR_Y, z).setType(Material.WHITE_CONCRETE, false);
        }
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) world.getBlockAt(x, FLOOR_Y, z).setType(Material.WHITE_CONCRETE, false);
        }
        // Two painted keys and free-throw marks.
        for (int x = -15; x <= -10; x++) {
            for (int z = -5; z <= 5; z++) world.getBlockAt(x, FLOOR_Y, z).setType(Material.RED_TERRACOTTA, false);
        }
        for (int x = 10; x <= 15; x++) {
            for (int z = -5; z <= 5; z++) world.getBlockAt(x, FLOOR_Y, z).setType(Material.LIGHT_BLUE_TERRACOTTA, false);
        }
        world.getBlockAt(-9, FLOOR_Y, 0).setType(Material.WHITE_CONCRETE, false);
        world.getBlockAt(9, FLOOR_Y, 0).setType(Material.WHITE_CONCRETE, false);
        drawThreePointArc(world, LEFT_HOOP_X, true);
        drawThreePointArc(world, RIGHT_HOOP_X, false);
        buildHoop(world, -1);
        buildHoop(world, 1);
        world.setSpawnLocation(0, FLOOR_Y + 1, 0);
    }

    private static void drawThreePointArc(World world, double hoopX, boolean left) {
        double radius = HoopsRemasteredRules.THREE_POINT_DISTANCE;
        for (int step = 0; step <= 64; step++) {
            double angle = -1.22D + 2.44D * step / 64.0D;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            int x = (int) Math.round(hoopX + (left ? dx : -dx));
            int z = (int) Math.round(dz);
            if (Math.abs(x) < 23 && Math.abs(z) < 13) {
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.WHITE_CONCRETE, false);
            }
        }
    }

    private static void buildHoop(World world, int side) {
        int backboardX = side < 0 ? -23 : 23;
        int rimX = side < 0 ? -20 : 20;
        Material board = side < 0 ? Material.RED_STAINED_GLASS : Material.LIGHT_BLUE_STAINED_GLASS;
        for (int y = 67; y <= 70; y++) {
            for (int z = -3; z <= 3; z++) world.getBlockAt(backboardX, y, z).setType(board, false);
        }
        // Open rim; the authoritative scoring cylinder is checked by the runtime.
        // Leave the centre cell open so the ball can pass through the rim.
        world.getBlockAt(rimX - 1, 66, 0).setType(Material.IRON_BARS, false);
        world.getBlockAt(rimX + 1, 66, 0).setType(Material.IRON_BARS, false);
        world.getBlockAt(rimX, 66, -1).setType(Material.IRON_BARS, false);
        world.getBlockAt(rimX, 66, 1).setType(Material.IRON_BARS, false);
        world.getBlockAt(rimX - 1, 65, 0).setType(Material.CHAIN, false);
        world.getBlockAt(rimX + 1, 65, 0).setType(Material.CHAIN, false);
    }

    public boolean isActive(World world) {
        return match != null && match.world == world && !match.ending;
    }

    public void start(World world, List<Player> players, Map<UUID, Integer> teams) {
        if (!HoopsRemasteredRules.supportsPlayerCount(players.size())) {
            throw new IllegalArgumentException("Hoops Remastered requires 2–16 players.");
        }
        stopRuntime();
        Match current = new Match(world, players, teams);
        this.match = current;
        buildCourt(world);
        for (Player player : current.players.values()) {
            if (player == null || !player.isOnline()) continue;
            int team = current.teams.getOrDefault(player.getUniqueId(), 0);
            current.playerStats.put(player.getUniqueId(), new PlayerStats());
            current.lastLocations.put(player.getUniqueId(), player.getLocation().clone());
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setFallDistance(0.0F);
            player.setAllowFlight(true);
            player.setFlying(false);
            player.getInventory().clear();
            player.getInventory().setHeldItemSlot(0);
            player.setWalkSpeed(0.2F);
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "HOOPS REMASTERED");
            player.sendMessage(ChatColor.GRAY + "Left-click " + ChatColor.WHITE + "to pass / steal"
                    + ChatColor.GRAY + "  •  Right-click " + ChatColor.WHITE + "to shoot / contest");
            player.sendMessage(ChatColor.GRAY + "First to 21, win by 2. Three-pointers count from beyond the arc.");
            player.setCompassTarget(team == 0 ? new Location(world, RIGHT_HOOP_X, HOOP_Y, 0)
                    : new Location(world, LEFT_HOOP_X, HOOP_Y, 0));
        }
        current.remainingMillis = HoopsRemasteredRules.REGULATION_MILLIS;
        current.shotClockEndsAt = System.currentTimeMillis() + HoopsRemasteredRules.SHOT_CLOCK_MILLIS;
        current.scoreboard = createScoreboard(current);
        for (Player player : current.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(current.scoreboard);
        }
        List<Player> starters = new ArrayList<>(current.players.values());
        Player first = starters.get(ThreadLocalRandom.current().nextInt(starters.size()));
        givePossession(current, first, false);
        current.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(current), 1L, 1L);
        updateHud(current, true);
        announce(current, ChatColor.GREEN + "Tip-off! " + ChatColor.YELLOW + first.getName() + ChatColor.GREEN + " has the ball.");
    }

    private Scoreboard createScoreboard(Match current) {
        if (Bukkit.getScoreboardManager() == null) return null;
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective sidebar = board.registerNewObjective("hoops", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "HOOPS");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        current.sidebar = sidebar;
        current.scoreboardEntries = List.of(
                ChatColor.RED + "Red Team", ChatColor.RED + "Red Score",
                ChatColor.AQUA + "Blue Team", ChatColor.AQUA + "Blue Score",
                ChatColor.YELLOW + "Clock", ChatColor.GOLD + "Shot clock",
                ChatColor.GRAY + "First to 21");
        Objective below = board.registerNewObjective("hoopspoints", "dummy", ChatColor.GOLD + "PTS");
        below.setDisplaySlot(DisplaySlot.BELOW_NAME);
        current.belowName = below;
        return board;
    }

    private void updateHud(Match current, boolean force) {
        if (current.scoreboard != null && current.sidebar != null) {
            Scoreboard board = current.scoreboard;
            for (String entry : current.scoreboardEntries) board.resetScores(entry);
            current.sidebar.getScore(current.scoreboardEntries.get(0)).setScore(8);
            current.sidebar.getScore(current.scoreboardEntries.get(1)).setScore(current.teamScore[0]);
            current.sidebar.getScore(current.scoreboardEntries.get(2)).setScore(6);
            current.sidebar.getScore(current.scoreboardEntries.get(3)).setScore(current.teamScore[1]);
            current.sidebar.getScore(current.scoreboardEntries.get(4)).setScore(4);
            current.sidebar.getScore(current.scoreboardEntries.get(5)).setScore(
                    (int) Math.ceil(current.remainingMillis / 1000.0D));
            current.sidebar.getScore(current.scoreboardEntries.get(6)).setScore(2);
            String possessionEntry = ChatColor.DARK_GRAY + "Ball: " + possession;
            if (current.possessionEntry != null) board.resetScores(current.possessionEntry);
            current.sidebar.getScore(possessionEntry).setScore(1);
            current.possessionEntry = possessionEntry;
            Objective below = current.belowName;
            if (below != null) {
                for (Player player : current.players.values()) {
                    if (player != null && player.isOnline()) {
                        below.getScore(player.getName()).setScore(
                                current.playerStats.getOrDefault(player.getUniqueId(), new PlayerStats()).points);
                    }
                }
            }
        }
        long now = System.currentTimeMillis();
        long shotRemaining = Math.max(0L, current.shotClockEndsAt - now);
        String clock = current.overtime ? "OVERTIME — NEXT BASKET" :
                HoopsRemasteredRules.formatClock(current.remainingMillis);
        Player currentHolder = current.holderId == null ? null : current.players.get(current.holderId);
        String possession = currentHolder == null ? "LOOSE BALL"
                : currentHolder.getName() + " (" + teamName(current.teams.getOrDefault(current.holderId, 0)) + ")";
        for (Player player : current.players.values()) {
            if (player == null || !player.isOnline()) continue;
            PlayerStats stats = current.playerStats.get(player.getUniqueId());
            player.sendActionBar(ChatColor.RED + "" + current.teamScore[0] + ChatColor.GRAY + " - "
                    + ChatColor.AQUA + current.teamScore[1] + ChatColor.DARK_GRAY + "  |  "
                    + ChatColor.YELLOW + clock + ChatColor.DARK_GRAY + "  |  "
                    + ChatColor.GOLD + "Shot " + Math.max(0L, (shotRemaining + 999L) / 1000L)
                    + ChatColor.GRAY + "  |  " + ChatColor.WHITE + "P " + stats.points
                    + "  A " + stats.assists + "  R " + stats.rebounds
                    + "  B " + stats.blocks + "  S " + stats.steals);
            if (force) {
                int team = current.teams.getOrDefault(player.getUniqueId(), 0);
                player.setCompassTarget(new Location(current.world, team == 0 ? RIGHT_HOOP_X : LEFT_HOOP_X,
                        FLOOR_Y, 0.0D));
            }
        }
    }

    private void tick(Match current) {
        if (match != current || current.ending) return;
        long now = System.currentTimeMillis();
        if (current.holderId != null) {
            Player holder = current.players.get(current.holderId);
            if (holder == null || !holder.isOnline() || !current.world.equals(holder.getWorld())) {
                current.holderId = null;
                if (current.ball == null) spawnLooseBall(current,
                        holder == null ? current.world.getSpawnLocation() : holder.getLocation(), new Vector(0.0D, 0.2D, 0.0D), BallMode.LOOSE, null, null, -1);
            }
        }
        if (!current.overtime) {
            current.remainingMillis = Math.max(0L,
                    HoopsRemasteredRules.REGULATION_MILLIS - (now - current.startedAt));
            if (current.remainingMillis <= 0L) {
                if (HoopsRemasteredRules.isTiedAtTimeLimit(current.teamScore[0], current.teamScore[1])) {
                    current.overtime = true;
                    current.shotClockEndsAt = now + HoopsRemasteredRules.SHOT_CLOCK_MILLIS;
                    announce(current, ChatColor.GOLD + "" + ChatColor.BOLD
                            + "FIVE MINUTES — TIED GAME! NEXT BASKET WINS!");
                    current.world.playSound(new Location(current.world, 0, FLOOR_Y + 1, 0),
                            Sound.BLOCK_BELL_USE, 1.2F, 0.8F);
                } else {
                    complete(current, HoopsRemasteredRules.leadingTeam(
                            current.teamScore[0], current.teamScore[1]));
                    return;
                }
            }
        }
        if (current.holderId != null && now >= current.shotClockEndsAt) {
            UUID expiredHolder = current.holderId;
            Player oldHolder = current.players.get(expiredHolder);
            int nextTeam = 1 - current.teams.getOrDefault(expiredHolder, 0);
            announce(current, ChatColor.RED + "Shot clock violation! Possession changes.");
            if (oldHolder != null) clearBallItem(oldHolder);
            givePossessionToTeam(current, nextTeam, oldHolder == null ? current.world.getSpawnLocation() : oldHolder.getLocation());
        }
        updateHops(current, now);
        tickBall(current, now);
        if (now - current.lastHudAt >= 500L) {
            current.lastHudAt = now;
            updateHud(current, false);
        }
    }

    private void updateHops(Match current, long now) {
        for (Player player : alivePlayers(current)) {
            UUID id = player.getUniqueId();
            long readyAt = current.hopsReadyAt.getOrDefault(id, 0L);
            if (now >= readyAt && player.isOnGround() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
                player.setFlying(false);
            }
        }
    }

    private void tickBall(Match current, long now) {
        Ball ball = current.ball;
        if (ball == null) return;
        if (ball.display == null || !ball.display.isValid()) {
            current.ball = null;
            return;
        }
        Location previous = ball.location.clone();
        ball.velocity.setY(ball.velocity.getY() - BALL_GRAVITY);
        ball.location.add(ball.velocity);
        if (ball.mode == BallMode.SHOT && crossesHoop(ball, previous, ball.location)) {
            scoreBasket(current, ball, now);
            return;
        }
        Block nextBlock = current.world.getBlockAt(ball.location);
        if (!nextBlock.isPassable() && ball.location.getY() > FLOOR_Y + 1.0D) {
            ball.location.subtract(ball.velocity);
            ball.velocity.setX(-ball.velocity.getX() * 0.45D);
            ball.velocity.setZ(-ball.velocity.getZ() * 0.45D);
            ball.velocity.setY(Math.max(0.16D, Math.abs(ball.velocity.getY()) * 0.45D));
            ball.mode = BallMode.LOOSE;
            ball.bounces++;
        }
        if (ball.location.getY() <= FLOOR_Y + 0.22D) {
            ball.location.setY(FLOOR_Y + 0.22D);
            if (Math.abs(ball.velocity.getY()) > 0.14D) {
                ball.velocity.setY(Math.abs(ball.velocity.getY()) * 0.48D);
                ball.velocity.setX(ball.velocity.getX() * 0.82D);
                ball.velocity.setZ(ball.velocity.getZ() * 0.82D);
            } else {
                ball.velocity.setY(0.0D);
                ball.velocity.setX(ball.velocity.getX() * 0.75D);
                ball.velocity.setZ(ball.velocity.getZ() * 0.75D);
            }
            ball.mode = BallMode.LOOSE;
            ball.bounces++;
        }
        if (ball.location.getX() < -24.5D || ball.location.getX() > 24.5D
                || ball.location.getZ() < -14.5D || ball.location.getZ() > 14.5D
                || now - ball.createdAt > 12_000L || ball.bounces > 7) {
            ball.mode = BallMode.LOOSE;
            ball.velocity.multiply(0.65D);
            if (now - ball.createdAt > 15_000L) {
                Player inbound = nearestPlayer(current, ball.location);
                if (inbound != null) givePossession(current, inbound, true);
                else removeBall(current);
                return;
            }
        }
        if (ball.location.getX() < -23.5D || ball.location.getX() > 23.5D
                || ball.location.getZ() < -13.5D || ball.location.getZ() > 13.5D) {
            int lastTeam = ball.lastToucher == null ? -1 : current.teams.getOrDefault(ball.lastToucher, -1);
            Location outLocation = ball.location.clone();
            removeBall(current);
            if (lastTeam >= 0) {
                announce(current, ChatColor.RED + "The ball went out of bounds. Possession changes.");
                givePossessionToTeam(current, 1 - lastTeam, outLocation);
            } else {
                givePossessionToTeam(current, ThreadLocalRandom.current().nextInt(2),
                        new Location(current.world, 0.0D, FLOOR_Y + 1.0D, 0.0D));
            }
            return;
        }
        ball.display.teleport(ball.location);
        for (Player player : alivePlayers(current)) {
            if (player.getLocation().distanceSquared(ball.location) > 1.8D) continue;
            boolean rebound = ball.reboundEligible;
            UUID previousTeam = ball.lastToucher;
            int previousPossessionTeam = rebound && ball.shooterId != null
                    ? current.teams.getOrDefault(ball.shooterId, -1)
                    : previousTeam == null ? -1 : current.teams.getOrDefault(previousTeam, -1);
            int catchingTeam = current.teams.getOrDefault(player.getUniqueId(), -1);
            if (rebound) {
                PlayerStats stats = current.playerStats.get(player.getUniqueId());
                stats.rebounds++;
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.35F);
                player.sendActionBar(ChatColor.GREEN + "REBOUND!");
                current.shotClockEndsAt = now + (previousPossessionTeam == catchingTeam
                        ? HoopsRemasteredRules.OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS
                        : HoopsRemasteredRules.SHOT_CLOCK_MILLIS);
                current.lastPasserId = null;
            } else if (ball.mode == BallMode.PASS && previousPossessionTeam >= 0
                    && previousPossessionTeam != catchingTeam) {
                current.playerStats.get(player.getUniqueId()).steals++;
                current.shotClockEndsAt = now + HoopsRemasteredRules.SHOT_CLOCK_MILLIS;
                current.lastPasserId = null;
                player.sendActionBar(ChatColor.GREEN + "INTERCEPTION!");
            } else if (previousPossessionTeam >= 0 && previousPossessionTeam != catchingTeam) {
                current.shotClockEndsAt = now + HoopsRemasteredRules.SHOT_CLOCK_MILLIS;
                current.lastPasserId = null;
            }
            ball.reboundEligible = false;
            givePossession(current, player, false);
            return;
        }
    }

    private boolean crossesHoop(Ball ball, Location from, Location to) {
        if (from.getY() <= HOOP_Y || to.getY() > HOOP_Y || ball.velocity.getY() >= 0.0D) return false;
        double hoopX = ball.defendingTeam == 0 ? LEFT_HOOP_X : RIGHT_HOOP_X;
        double dx = to.getX() - hoopX;
        double dz = to.getZ();
        return dx * dx + dz * dz <= 1.15D * 1.15D;
    }

    private void scoreBasket(Match current, Ball ball, long now) {
        int scoringTeam = 1 - ball.defendingTeam;
        if (current.teams.getOrDefault(ball.shooterId, -1) != scoringTeam) {
            removeBall(current);
            return;
        }
        Player shooter = current.players.get(ball.shooterId);
        if (shooter == null) {
            removeBall(current);
            return;
        }
        int points = ball.points;
        current.teamScore[scoringTeam] += points;
        PlayerStats shooterStats = current.playerStats.get(ball.shooterId);
        shooterStats.points += points;
        shooterStats.shotsMade++;
        if (ball.passerId != null && !ball.passerId.equals(ball.shooterId)
                && HoopsRemasteredRules.isAssistEligible(now, ball.passedAt)) {
            Player passer = current.players.get(ball.passerId);
            if (passer != null && current.teams.getOrDefault(ball.passerId, -1) == scoringTeam) {
                current.playerStats.get(ball.passerId).assists++;
                if (!current.overtime && current.remainingMillis <= HoopsRemasteredRules.CLUTCH_WINDOW_MILLIS) {
                    current.clutchContributions.merge(ball.passerId, 1, Integer::sum);
                }
            }
        }
        if (!current.overtime && current.remainingMillis <= HoopsRemasteredRules.CLUTCH_WINDOW_MILLIS) {
            current.clutchContributions.merge(ball.shooterId, points, Integer::sum);
        }
        int opponent = 1 - scoringTeam;
        if (HoopsRemasteredRules.isComebackEligible(current.teamScore[opponent], current.teamScore[scoringTeam])) {
            current.comebackTeams.add(opponent);
        }
        boolean buzzer = !current.overtime
                && HoopsRemasteredRules.isBuzzerBeater(current.remainingMillis);
        boolean won = HoopsRemasteredRules.isWinningBasket(
                current.teamScore[0], current.teamScore[1], current.overtime);
        if (buzzer && won) {
            if (ball.passerId != null && current.players.containsKey(ball.passerId)) {
                award(current, ball.passerId, "buzzer-beater");
            }
            award(current, ball.shooterId, "buzzer-beater");
        }
        current.world.spawnParticle(org.bukkit.Particle.FIREWORK, ball.location, 22, 0.5D, 0.35D, 0.5D, 0.08D);
        current.world.playSound(ball.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, points == 3 ? 1.4F : 1.1F);
        announce(current, teamColor(scoringTeam) + "" + ChatColor.BOLD + points + " POINTS! "
                + ChatColor.WHITE + shooter.getName()
                + (points == 3 ? ChatColor.GOLD + " — THREE!" : ChatColor.GRAY + " — TWO!"));
        removeBall(current);
        if (won) {
            complete(current, scoringTeam);
            return;
        }
        // The conceding team inbounds from under its own basket.
        givePossessionToTeam(current, opponent, new Location(current.world,
                opponent == 0 ? -20.0D : 20.0D, FLOOR_Y + 1.0D, 0.0D));
    }

    private void givePossessionToTeam(Match current, int team, Location inbound) {
        List<Player> choices = alivePlayers(current).stream()
                .filter(player -> current.teams.getOrDefault(player.getUniqueId(), -1) == team)
                .sorted(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(inbound)))
                .toList();
        if (choices.isEmpty()) {
            complete(current, 1 - team);
            return;
        }
        Player receiver = choices.get(0);
        receiver.teleport(inbound.clone().add(team == 0 ? 1.5D : -1.5D, 0.0D, 0.0D));
        givePossession(current, receiver, false);
        current.shotClockEndsAt = System.currentTimeMillis() + HoopsRemasteredRules.SHOT_CLOCK_MILLIS;
        current.lastPasserId = null;
        current.world.playSound(receiver.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.9F, 1.1F);
    }

    private void givePossession(Match current, Player player, boolean rebound) {
        if (player == null || !player.isOnline()) return;
        removeBall(current);
        Player previous = current.holderId == null ? null : current.players.get(current.holderId);
        if (previous != null) clearBallItem(previous);
        current.holderId = player.getUniqueId();
        player.getInventory().setItem(0, basketballItem());
        player.getInventory().setHeldItemSlot(0);
        player.setVelocity(player.getVelocity().multiply(0.65D));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.6F);
    }

    private void pass(Match current, Player passer) {
        Player receiver = alivePlayers(current).stream()
                .filter(player -> !player.equals(passer)
                        && current.teams.getOrDefault(player.getUniqueId(), -1)
                                == current.teams.getOrDefault(passer.getUniqueId(), -2))
                .filter(player -> inPassLane(passer, player))
                .min(Comparator.comparingDouble(player -> passer.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
        Vector velocity;
        if (receiver != null) {
            Vector delta = receiver.getEyeLocation().toVector().subtract(passer.getEyeLocation().toVector());
            velocity = delta.normalize().multiply(1.45D).setY(Math.max(0.16D, delta.getY() * 0.08D + 0.24D));
        } else {
            velocity = passer.getEyeLocation().getDirection().normalize().multiply(1.35D).setY(0.22D);
        }
        removePossession(current, passer);
        spawnLooseBall(current, passer.getEyeLocation(), velocity, BallMode.PASS,
                passer.getUniqueId(), current.lastPasserId, -1);
        current.lastPasserId = passer.getUniqueId();
        current.lastPassAt = System.currentTimeMillis();
        passer.playSound(passer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7F, 1.4F);
        passer.sendActionBar(ChatColor.GREEN + "PASS!");
    }

    private boolean inPassLane(Player passer, Player receiver) {
        double distance = passer.getLocation().distance(receiver.getLocation());
        if (distance < 2.0D || distance > 24.0D || !passer.hasLineOfSight(receiver)) return false;
        Vector direction = passer.getEyeLocation().getDirection().normalize();
        Vector toReceiver = receiver.getLocation().toVector().subtract(passer.getEyeLocation().toVector()).normalize();
        return direction.dot(toReceiver) >= 0.45D;
    }

    private void shoot(Match current, Player shooter) {
        Location eye = shooter.getEyeLocation();
        int team = current.teams.getOrDefault(shooter.getUniqueId(), 0);
        int defendingTeam = 1 - team;
        double hoopX = team == 0 ? RIGHT_HOOP_X : LEFT_HOOP_X;
        Location hoop = new Location(current.world, hoopX, HOOP_Y, 0.0D);
        Vector aim = eye.getDirection().normalize();
        Vector toHoop = hoop.toVector().subtract(eye.toVector());
        double projected = toHoop.dot(aim);
        Vector closest = eye.toVector().add(aim.clone().multiply(Math.max(0.0D, projected)));
        boolean aimedAtHoop = projected > 0.0D && closest.distance(hoop.toVector()) <= 4.5D;
        double hoopDistance = Math.sqrt(Math.pow(hoopX - shooter.getLocation().getX(), 2.0D)
                + Math.pow(shooter.getLocation().getZ(), 2.0D));
        boolean layup = hoopDistance <= 6.0D;
        Location target = (aimedAtHoop || layup) ? hoop : eye.clone().add(aim.multiply(18.0D));
        double horizontal = Math.max(1.0D, Math.sqrt(
                Math.pow(target.getX() - eye.getX(), 2.0D) + Math.pow(target.getZ() - eye.getZ(), 2.0D)));
        int flightTicks = (int) Math.max(8L, Math.min(22L, Math.round(horizontal / 1.05D)));
        Vector velocity = new Vector(
                (target.getX() - eye.getX()) / flightTicks,
                (target.getY() - eye.getY() + BALL_GRAVITY * flightTicks * (flightTicks + 1.0D) / 2.0D) / flightTicks,
                (target.getZ() - eye.getZ()) / flightTicks);
        double releaseDistance = Math.sqrt(Math.pow(hoopX - shooter.getLocation().getX(), 2.0D)
                + Math.pow(shooter.getLocation().getZ(), 2.0D));
        int points = HoopsRemasteredRules.scoreValue(releaseDistance);
        PlayerStats stats = current.playerStats.get(shooter.getUniqueId());
        stats.shotsAttempted++;
        removePossession(current, shooter);
        spawnLooseBall(current, eye, velocity, BallMode.SHOT, shooter.getUniqueId(),
                current.lastPasserId, defendingTeam);
        Ball ball = current.ball;
        ball.points = points;
        ball.releaseDistance = releaseDistance;
        ball.passedAt = current.lastPassAt;
        current.lastPasserId = null;
        current.lastPassAt = 0L;
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, points == 3 ? 1.2F : 0.9F);
        shooter.sendActionBar(ChatColor.GOLD + (layup ? "LAYUP" : points == 3 ? "THREE-POINT ATTEMPT" : "SHOT ATTEMPT"));
    }

    private void blockShot(Match current, Player defender) {
        Ball ball = current.ball;
        if (ball == null || ball.mode != BallMode.SHOT
                || current.teams.getOrDefault(ball.shooterId, -1) == current.teams.getOrDefault(defender.getUniqueId(), -2)
                || defender.getLocation().distanceSquared(ball.location) > HoopsRemasteredRules.BLOCK_RADIUS * HoopsRemasteredRules.BLOCK_RADIUS) {
            return;
        }
        PlayerStats stats = current.playerStats.get(defender.getUniqueId());
        if (stats == null) return;
        ball.velocity.add(defender.getLocation().getDirection().normalize().multiply(0.6D).setY(0.35D));
        ball.mode = BallMode.LOOSE;
        ball.lastToucher = defender.getUniqueId();
        stats.blocks++;
        defender.playSound(defender.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.5F);
        defender.sendActionBar(ChatColor.GREEN + "" + ChatColor.BOLD + "BLOCK!");
        current.lastPasserId = null;
    }

    private void trySteal(Match current, Player attacker, Player target) {
        if (target == null || current.holderId == null || !current.holderId.equals(target.getUniqueId())
                || current.teams.getOrDefault(attacker.getUniqueId(), -1)
                        == current.teams.getOrDefault(target.getUniqueId(), -2)) return;
        long now = System.currentTimeMillis();
        long readyAt = current.stealReadyAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (now < readyAt || attacker.getLocation().distanceSquared(target.getLocation())
                > HoopsRemasteredRules.STEAL_RADIUS * HoopsRemasteredRules.STEAL_RADIUS
                || !attacker.hasLineOfSight(target)) return;
        current.stealReadyAt.put(attacker.getUniqueId(), now + 3_000L);
        current.playerStats.get(attacker.getUniqueId()).steals++;
        current.lastPasserId = null;
        current.shotClockEndsAt = now + HoopsRemasteredRules.SHOT_CLOCK_MILLIS;
        givePossession(current, attacker, false);
        attacker.sendActionBar(ChatColor.GREEN + "" + ChatColor.BOLD + "STEAL!");
        target.sendActionBar(ChatColor.RED + "Ball stolen by " + attacker.getName());
        announce(current, ChatColor.YELLOW + attacker.getName() + " stole the ball from " + target.getName() + "!");
    }

    private void spawnLooseBall(Match current, Location location, Vector velocity,
            BallMode mode, UUID shooter, UUID passer, int defendingTeam) {
        removeBall(current);
        ItemDisplay display = current.world.spawn(location, ItemDisplay.class);
        display.setItemStack(ballDisplayItem());
        display.setBillboard(Display.Billboard.CENTER);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setGravity(false);
        display.setInvulnerable(true);
        display.setPersistent(false);
        display.setGlowing(true);
        current.entities.add(display.getUniqueId());
        Ball ball = new Ball(display, location.clone(), velocity.clone(), mode, shooter, passer,
                defendingTeam, System.currentTimeMillis());
        ball.lastToucher = shooter;
        ball.reboundEligible = mode == BallMode.SHOT;
        current.ball = ball;
    }

    private ItemStack basketballItem() {
        ItemStack item = new ItemStack(Material.SLIME_BALL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Basketball");
            meta.setLore(List.of(ChatColor.GRAY + "Left-click: pass", ChatColor.GRAY + "Right-click: shoot"));
            meta.getPersistentDataContainer().set(ballItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack ballDisplayItem() {
        ItemStack item = new ItemStack(Material.SLIME_BALL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Basketball");
            item.setItemMeta(meta);
        }
        return item;
    }

    private void removePossession(Match current, Player holder) {
        if (holder != null) clearBallItem(holder);
        if (current.holderId != null && holder != null && current.holderId.equals(holder.getUniqueId())) {
            current.holderId = null;
        }
    }

    private void clearBallItem(Player player) {
        if (player == null || !player.isOnline()) return;
        ItemStack item = player.getInventory().getItem(0);
        if (item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(ballItemKey, PersistentDataType.BYTE)) {
            player.getInventory().setItem(0, null);
        }
    }

    private void removeBall(Match current) {
        if (current.ball != null && current.ball.display != null && current.ball.display.isValid()) {
            current.ball.display.remove();
        }
        current.ball = null;
    }

    private List<Player> alivePlayers(Match current) {
        return current.players.values().stream()
                .filter(player -> player != null && player.isOnline() && player.getWorld() == current.world)
                .toList();
    }

    private Player nearestPlayer(Match current, Location location) {
        return alivePlayers(current).stream()
                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(location)))
                .orElse(null);
    }

    private void announce(Match current, String message) {
        for (Player player : alivePlayers(current)) player.sendMessage(message);
    }

    private void complete(Match current, int winningTeam) {
        if (match != current || current.ending) return;
        current.ending = true;
        Player representative = current.players.values().stream()
                .filter(player -> player != null && player.isOnline()
                        && current.teams.getOrDefault(player.getUniqueId(), -1) == winningTeam)
                .findFirst().orElseGet(() -> current.players.values().stream()
                        .filter(player -> player != null
                                && current.teams.getOrDefault(player.getUniqueId(), -1) == winningTeam)
                        .findFirst().orElse(null));
        persistCareer(current, winningTeam);
        hooks.finish(representative, true);
    }

    private void persistCareer(Match current, int winningTeam) {
        for (Map.Entry<UUID, Player> entry : current.players.entrySet()) {
            UUID id = entry.getKey();
            Player player = entry.getValue();
            int team = current.teams.getOrDefault(id, -1);
            boolean won = winningTeam >= 0 && team == winningTeam;
            PlayerStats stats = current.playerStats.getOrDefault(id, new PlayerStats());
            String path = statsPath(id);
            increment(path + "games-played", 1);
            if (won) {
                increment(path + "wins", 1);
                increment(path + "win-streak", 1);
                if (plugin.getConfig().getInt(path + "win-streak", 0) >= 4) award(current, id, player, "playoff-sweep");
            } else if (winningTeam < 0) {
                increment(path + "draws", 1);
                plugin.getConfig().set(path + "win-streak", 0);
            } else {
                increment(path + "losses", 1);
                plugin.getConfig().set(path + "win-streak", 0);
            }
            increment(path + "points", stats.points);
            increment(path + "assists", stats.assists);
            increment(path + "rebounds", stats.rebounds);
            increment(path + "blocks", stats.blocks);
            increment(path + "steals", stats.steals);
            increment(path + "shots-attempted", stats.shotsAttempted);
            increment(path + "shots-made", stats.shotsMade);
            int games = Math.max(1, plugin.getConfig().getInt(path + "games-played", 1));
            plugin.getConfig().set(path + "ppg", plugin.getConfig().getInt(path + "points", 0) / (double) games);
            plugin.getConfig().set(path + "apg", plugin.getConfig().getInt(path + "assists", 0) / (double) games);
            plugin.getConfig().set(path + "rpg", plugin.getConfig().getInt(path + "rebounds", 0) / (double) games);
            if (HoopsRemasteredRules.isDoubleDouble(stats.points, stats.assists, stats.rebounds, stats.blocks, stats.steals))
                award(current, id, player, "double-double");
            if (HoopsRemasteredRules.isTripleDouble(stats.points, stats.assists, stats.rebounds, stats.blocks, stats.steals))
                award(current, id, player, "triple-double");
            if (HoopsRemasteredRules.isQuadrupleDouble(stats.points, stats.assists, stats.rebounds, stats.blocks, stats.steals))
                award(current, id, player, "quadruple-double");
            if (HoopsRemasteredRules.isQuintupleDouble(stats.points, stats.assists, stats.rebounds, stats.blocks, stats.steals))
                award(current, id, player, "quintuple-double");
            if (won && stats.shotsAttempted >= 3 && stats.points == 0) award(current, id, player, "brick-by-brick");
            if (won && current.comebackTeams.contains(team)) award(current, id, player, "comeback-kings");
            if (won && current.clutchContributions.getOrDefault(id, 0) >= 6)
                award(current, id, player, "in-the-clutch");
        }
        plugin.saveConfig();
    }

    private String statsPath(UUID id) {
        return "local-stats." + id + "." + GAME_ID + ".";
    }

    private void increment(String key, int amount) {
        if (amount > 0) plugin.getConfig().set(key, plugin.getConfig().getInt(key, 0) + amount);
    }

    private void award(Match current, UUID id, String achievementId) {
        award(current, id, current.players.get(id), achievementId);
    }

    private void award(Match current, UUID id, Player player, String achievementId) {
        String key = "local-achievements." + id + "." + GAME_ID + "." + achievementId;
        if (plugin.getConfig().getBoolean(key, false)) return;
        plugin.getConfig().set(key, true);
        String name = ArcadeProgressCatalog.achievements(GAME_ID).stream()
                .filter(achievement -> achievement.id().equals(achievementId))
                .map(ArcadeProgressCatalog.Achievement::name).findFirst().orElse(achievementId);
        if (player != null && player.isOnline()) player.sendMessage(ChatColor.GOLD + "Achievement unlocked: " + name);
    }

    private void stopRuntime() {
        Match previous = match;
        if (previous == null) return;
        match = null;
        if (previous.task != null) previous.task.cancel();
        removeBall(previous);
        for (UUID entityId : previous.entities) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) entity.remove();
        }
        for (Player player : previous.players.values()) {
            if (player == null || !player.isOnline()) continue;
            clearBallItem(player);
        }
        if (previous.scoreboard != null) {
            try {
                if (previous.sidebar != null && previous.scoreboard.getObjective(previous.sidebar.getName()) != null)
                    previous.sidebar.unregister();
                if (previous.belowName != null && previous.scoreboard.getObjective(previous.belowName.getName()) != null)
                    previous.belowName.unregister();
            } catch (IllegalArgumentException | IllegalStateException ignored) { }
        }
    }

    public void onManagerFinish() {
        stopRuntime();
    }

    public void shutdown() {
        stopRuntime();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Match current = match;
        if (current == null || current.ending || event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!current.players.containsKey(player.getUniqueId()) || player.getWorld() != current.world) return;
        Action action = event.getAction();
        if (!action.isLeftClick() && !action.isRightClick()) return;
        event.setCancelled(true);
        if (current.holderId != null && current.holderId.equals(player.getUniqueId())) {
            if (action.isLeftClick()) pass(current, player);
            else shoot(current, player);
            return;
        }
        if (action.isRightClick()) blockShot(current, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Match current = match;
        Player player = event.getPlayer();
        if (current == null || current.ending || !current.players.containsKey(player.getUniqueId())
                || player.getWorld() != current.world) return;
        event.setCancelled(true);
        player.setFlying(false);
        long now = System.currentTimeMillis();
        long readyAt = current.hopsReadyAt.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            player.sendActionBar(ChatColor.GRAY + "Hops recharging: "
                    + Math.max(1L, (readyAt - now + 999L) / 1_000L) + "s");
            return;
        }
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() > 0.001D) direction.normalize().multiply(0.48D);
        player.setAllowFlight(false);
        player.setVelocity(new Vector(direction.getX(), 0.78D, direction.getZ()));
        current.hopsReadyAt.put(player.getUniqueId(), now + HoopsRemasteredRules.HOPS_COOLDOWN_MILLIS);
        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD,
                player.getLocation().clone().add(0.0D, 0.15D, 0.0D), 12, 0.28D, 0.08D, 0.28D, 0.04D);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7F, 1.5F);
        player.sendActionBar(ChatColor.YELLOW + "HOPS!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Match current = match;
        if (current == null || current.ending || event.getTo() == null) return;
        Player player = event.getPlayer();
        if (!current.players.containsKey(player.getUniqueId()) || player.getWorld() != current.world) return;
        Location to = event.getTo();
        if (to.getX() >= -23.5D && to.getX() <= 23.5D && to.getZ() >= -13.5D && to.getZ() <= 13.5D) return;
        event.setTo(event.getFrom());
        if (current.holderId != null && current.holderId.equals(player.getUniqueId())) {
            int opponent = 1 - current.teams.getOrDefault(player.getUniqueId(), 0);
            current.lastPasserId = null;
            announce(current, ChatColor.RED + player.getName() + " stepped out of bounds. Possession changes.");
            givePossessionToTeam(current, opponent, event.getFrom());
        } else {
            player.sendActionBar(ChatColor.RED + "OUT OF BOUNDS");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        Match current = match;
        if (current == null || current.ending || !(event.getEntity() instanceof Player target)
                || !(event.getDamager() instanceof Player attacker)
                || target.getWorld() != current.world || attacker.getWorld() != current.world
                || !current.players.containsKey(target.getUniqueId())
                || !current.players.containsKey(attacker.getUniqueId())) return;
        event.setCancelled(true);
        trySteal(current, attacker, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onOtherDamage(EntityDamageEvent event) {
        Match current = match;
        if (current == null || event.getEntity().getWorld() != current.world) return;
        if (event.getEntity() instanceof Player player && current.players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Match current = match;
        if (current == null || !current.players.containsKey(event.getPlayer().getUniqueId())) return;
        Player player = event.getPlayer();
        if (current.holderId != null && current.holderId.equals(player.getUniqueId())) {
            current.holderId = null;
            spawnLooseBall(current, player.getLocation(), new Vector(0.0D, 0.18D, 0.0D),
                    BallMode.LOOSE, player.getUniqueId(), null, -1);
        }
        long remaining = current.players.values().stream().filter(p -> p != null && p.isOnline()
                && !p.getUniqueId().equals(player.getUniqueId())).count();
        int[] activeByTeam = new int[2];
        for (Player participant : current.players.values()) {
            if (participant == null || !participant.isOnline() || participant.equals(player)) continue;
            int team = current.teams.getOrDefault(participant.getUniqueId(), -1);
            if (team >= 0 && team < activeByTeam.length) activeByTeam[team]++;
        }
        if (activeByTeam[0] == 0 && activeByTeam[1] > 0) complete(current, 1);
        else if (activeByTeam[1] == 0 && activeByTeam[0] > 0) complete(current, 0);
        else if (activeByTeam[0] == 0 && activeByTeam[1] == 0) {
            current.ending = true;
            hooks.finish(null, false);
        }
    }

    private String teamName(int team) { return team == 0 ? "RED" : "BLUE"; }
    private ChatColor teamColor(int team) { return team == 0 ? ChatColor.RED : ChatColor.AQUA; }

    private static final class Match {
        final World world;
        final Map<UUID, Player> players = new LinkedHashMap<>();
        final Map<UUID, Integer> teams = new HashMap<>();
        final Map<UUID, PlayerStats> playerStats = new HashMap<>();
        final Map<UUID, Location> lastLocations = new HashMap<>();
        final Map<UUID, Long> stealReadyAt = new HashMap<>();
        final Map<UUID, Long> hopsReadyAt = new HashMap<>();
        final Map<UUID, Integer> clutchContributions = new HashMap<>();
        final Set<Integer> comebackTeams = new HashSet<>();
        final Set<UUID> entities = new HashSet<>();
        final int[] teamScore = new int[2];
        final long startedAt = System.currentTimeMillis();
        long remainingMillis = HoopsRemasteredRules.REGULATION_MILLIS;
        long shotClockEndsAt;
        long lastHudAt;
        long lastPassAt;
        UUID holderId;
        UUID lastPasserId;
        boolean overtime;
        boolean ending;
        Ball ball;
        BukkitTask task;
        Scoreboard scoreboard;
        Objective sidebar;
        Objective belowName;
        List<String> scoreboardEntries = List.of();
        String possessionEntry;

        Match(World world, List<Player> participants, Map<UUID, Integer> assignments) {
            this.world = world;
            for (Player player : participants) {
                if (player == null) continue;
                players.put(player.getUniqueId(), player);
                teams.put(player.getUniqueId(), assignments.getOrDefault(player.getUniqueId(), 0));
            }
        }
    }

    private enum BallMode { PASS, SHOT, LOOSE }

    private static final class Ball {
        final ItemDisplay display;
        final Location location;
        final Vector velocity;
        BallMode mode;
        final UUID shooterId;
        final UUID passerId;
        final int defendingTeam;
        final long createdAt;
        UUID lastToucher;
        int points = 2;
        int bounces;
        boolean reboundEligible;
        double releaseDistance;
        long passedAt;

        Ball(ItemDisplay display, Location location, Vector velocity, BallMode mode,
                UUID shooterId, UUID passerId, int defendingTeam, long createdAt) {
            this.display = display;
            this.location = location;
            this.velocity = velocity;
            this.mode = mode;
            this.shooterId = shooterId;
            this.passerId = passerId;
            this.defendingTeam = defendingTeam;
            this.createdAt = createdAt;
            this.lastToucher = shooterId;
        }
    }

    private static final class PlayerStats {
        int points;
        int assists;
        int rebounds;
        int blocks;
        int steals;
        int shotsAttempted;
        int shotsMade;
    }
}
