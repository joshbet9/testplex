package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Master Builders' theme, construction timer, review timer and plot-count sidebar. */
final class MasterBuildersManager implements ArcadeMatchController {
    private final Logger logger;

    MasterBuildersManager(Logger logger) {
        this.logger = logger;
    }

    @Override public ArcadePortGame game() { return ArcadePortGame.MASTER_BUILDERS; }

    static long buildMillis(int judgedBuildCount) {
        return ArcadeProgressRules.masterBuildersBuildMillis(ArcadePortRules.masterBuildersTotalMillis(),
                judgedBuildCount, ArcadePortRules.masterBuildersReviewMillis());
    }

    /** Owns build-phase setup; plot geometry and labels remain supplied by the shared world runtime. */
    void startBuild(SessionState session, long now, MasterBuildersPlotRuntime plots,
            IntFunction<Location> plotCenter, Consumer<SessionState> spawnPlotLabels) {
        if (session == null || plots == null || plotCenter == null || spawnPlotLabels == null) return;
        List<String> themes = List.of("Castle", "Dragon", "Treehouse", "Robot", "Pirate Ship", "Underwater City",
                "Volcano", "Space Station", "Farm", "Haunted House", "Rollercoaster", "Zoo", "Airplane",
                "Lighthouse", "Wizard Tower", "Football Stadium");
        session.masterBuilderTheme = themes.get(ArcadePortRules.masterBuildersThemeIndex(
                ThreadLocalRandom.current().nextInt(themes.size()) + 1) - 1);
        int judgedBuildCount = session.masterBuilderReviewOrder.size();
        session.masterBuilderBuildEndsAt = now + buildMillis(judgedBuildCount);
        for (Player player : session.players.values()) {
            if (player == null || !player.isOnline()) continue;
            player.getInventory().clear();
            player.setGameMode(GameMode.CREATIVE);
            Integer plotIndex = session.masterBuilderPlots.get(player.getUniqueId());
            if (plotIndex != null) player.teleport(plotCenter.apply(plotIndex));
            player.sendMessage(ChatColor.YELLOW + "Build: " + ChatColor.GOLD + session.masterBuilderTheme
                    + ChatColor.YELLOW + " | Your plot: " + (plotIndex == null ? "unassigned" : plotIndex + 1));
            player.sendMessage(ChatColor.GRAY + "Use Creative inventory inside your quartz plot border. "
                    + "Build time adjusts to the number of human plots so building plus voting totals five minutes.");
            player.sendTitle(ChatColor.GOLD + "MASTER BUILDERS",
                    ChatColor.YELLOW + "Build: " + session.masterBuilderTheme + ChatColor.GRAY + " | Plot "
                            + (plotIndex == null ? "?" : plotIndex + 1), 5, 50, 10);
        }
        plots.begin(session.players.values());
        spawnPlotLabels.accept(session);
        setupScoreboard(session);
        updateScoreboard(session, System.currentTimeMillis(), true, false);
    }

    boolean recordVote(SessionState session, UUID voterId, int score, boolean reviewPhase, long now) {
        if (session == null || voterId == null || !reviewPhase || session.masterBuilderCurrentTarget == null
                || voterId.equals(session.masterBuilderCurrentTarget) || now >= session.masterBuilderVoteEndsAt
                || score < 1 || score > ArcadePortRules.masterBuildersMaxRating()) return false;
        session.masterBuilderVotes.computeIfAbsent(session.masterBuilderCurrentTarget, ignored -> new HashMap<>())
                .put(voterId, score);
        return true;
    }


    /** Called at the existing beginning-of-build point, not during the shared preparation phase. */
    void setupScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("build", "dummy",
                ChatColor.GOLD + "MASTER BUILDERS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.masterBuildersScoreboard = scoreboard;
        session.masterBuildersScoreboardObjective = objective;
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
    }

    /** Phase booleans come from the match's existing private phase enum; no new phase state. */
    void updateScoreboard(SessionState session, long now, boolean building, boolean review) {
        if (session == null || session.masterBuildersScoreboardObjective == null
                || now < session.masterBuildersNextScoreboardAt) return;
        session.masterBuildersNextScoreboardAt = now + 500L;
        for (String entry : session.masterBuildersScoreboardEntries) {
            session.masterBuildersScoreboard.resetScores(entry);
        }
        session.masterBuildersScoreboardEntries.clear();

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.YELLOW + "Theme: " + ChatColor.WHITE + session.masterBuilderTheme);
        if (building) {
            long remaining = Math.max(0L, session.masterBuilderBuildEndsAt - now);
            lines.add(ChatColor.GREEN + "Build Time");
            lines.add(ChatColor.WHITE + formatTime(remaining));
        } else if (review) {
            long remaining = Math.max(0L, session.masterBuilderVoteEndsAt - now);
            Player builder = session.masterBuilderCurrentTarget == null ? null
                    : session.players.get(session.masterBuilderCurrentTarget);
            String builderName = builder == null ? "Builder" : builder.getName();
            lines.add(ChatColor.GOLD + "Review " + (session.masterBuilderReviewIndex + 1)
                    + "/" + session.masterBuilderReviewOrder.size());
            lines.add(ChatColor.WHITE + builderName);
            lines.add(ChatColor.YELLOW + "Vote Time");
            lines.add(ChatColor.WHITE + formatTime(remaining));
        }
        lines.add(ChatColor.GRAY + "Plots: " + ChatColor.WHITE + session.masterBuilderPlots.size());

        int score = lines.size();
        for (String line : lines) {
            session.masterBuildersScoreboardEntries.add(line);
            session.masterBuildersScoreboardObjective.getScore(line).setScore(score--);
        }
    }

    static String formatTime(long millis) {
        long seconds = (millis + 999L) / 1_000L;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    void cleanupScoreboard(SessionState session) {
        if (session == null || session.masterBuildersScoreboardObjective == null) return;
        try {
            session.masterBuildersScoreboardObjective.unregister();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.fine("Master Builders scoreboard was already removed: " + exception.getMessage());
        }
        session.masterBuildersScoreboard = null;
        session.masterBuildersScoreboardObjective = null;
        session.masterBuildersScoreboardEntries.clear();
    }
}
