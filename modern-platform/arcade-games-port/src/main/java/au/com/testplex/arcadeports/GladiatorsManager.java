package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Gladiators' 500 ms scoreboard and per-player double-elimination bracket cues. */
final class GladiatorsManager implements ArcadeMatchController {
    private final Logger logger;

    GladiatorsManager(Logger logger) {
        this.logger = logger;
    }

    @Override public ArcadePortGame game() { return ArcadePortGame.GLADIATORS; }

    interface AchievementSink {
        void awardCatalog(Player player, String achievementId);
        void awardLegacy(Player player, String achievementId, String displayName);
        void recordModeWin(Player player, GladiatorsMode mode);
        void incrementLifetimeStat(Player player, String stat, int amount,
                int achievementThreshold, String achievementId);
    }

    void recordWinnerAchievements(SessionState session, Player winner, GladiatorsMode mode,
            int fullArsenalDuels, boolean fullArsenalIncomplete, AchievementSink sink) {
        if (session == null || winner == null || sink == null) return;
        UUID id = winner.getUniqueId();
        sink.recordModeWin(winner, mode);
        if (GladiatorsAchievementRules.qualifiesFullArsenalTournament(
                mode == GladiatorsMode.NORMAL,
                fullArsenalDuels,
                fullArsenalIncomplete)) {
            sink.awardCatalog(winner, "full-arsenal");
        }
        if (session.gladiatorLosses.getOrDefault(id, 0) > 0) {
            if (session.gladiatorLostRoundOneClose.contains(id)) {
                sink.awardCatalog(winner, "silky-seconds");
            }
            if (session.gladiatorLostRoundOneAfterTenDamage.contains(id)) {
                sink.awardCatalog(winner, "comeback-kid");
            }
        }
        if (mode != null && mode.hasClassicRangedTools() && !session.gladiatorUsedRod.contains(id)) {
            sink.awardCatalog(winner, "man-of-integrity");
        }
        if (ArcadePortRules.shouldAwardGladiatorsNoBowOrRodWin(mode != null && mode.hasClassicRangedTools(),
                session.gladiatorUsedBow.contains(id), session.gladiatorUsedRod.contains(id))) {
            sink.awardLegacy(winner, "are-you-not-entertained", "Are you not entertained?");
        }
        int arrowsShot = session.gladiatorArrowsShot.getOrDefault(id, 0);
        if (arrowsShot >= 3 && arrowsShot == session.gladiatorArrowsHit.getOrDefault(id, 0)) {
            sink.incrementLifetimeStat(winner, "precision", 1, 1, "precision");
        }
    }

    void setupScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("glad", "dummy", ChatColor.GOLD + "GLADIATORS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.gladiatorScoreboard = scoreboard;
        session.gladiatorScoreboardObjective = objective;
        for (int index = 0; index < 15; index++) {
            Team line = scoreboard.registerNewTeam("gladline" + index);
            String entry = ChatColor.values()[index].toString();
            line.addEntry(entry);
            session.gladiatorScoreboardTeams.add(line);
            session.gladiatorScoreboardEntries.add(entry);
        }
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
    }

    void updateScoreboard(SessionState session, long now) {
        if (session == null || session.gladiatorScoreboardObjective == null
                || now < session.gladiatorNextScoreboardAt) return;
        session.gladiatorNextScoreboardAt = now + 500L;
        int activeMatches = 0;
        long nextFightAt = Long.MAX_VALUE;
        long nextPoisonAt = Long.MAX_VALUE;
        for (Integer pair : session.gladiatorPairBrackets.keySet()) {
            if (session.gladiatorResolvedPairs.contains(pair)) continue;
            activeMatches++;
            nextFightAt = Math.min(nextFightAt,
                    session.gladiatorFightStartsAtByPair.getOrDefault(pair, Long.MAX_VALUE));
            nextPoisonAt = Math.min(nextPoisonAt,
                    session.gladiatorNextPoisonAtByPair.getOrDefault(pair, Long.MAX_VALUE));
        }
        String status;
        if (activeMatches == 0) status = session.gladiatorVotingEndsAt > now ? "Vote" : "Waiting";
        else if (now < nextFightAt) status = "Starting in " + Math.max(1, (nextFightAt - now + 999L) / 1_000L) + "s";
        else if (now >= nextPoisonAt) status = "Poison Active";
        else status = "Poison in " + Math.max(0, (nextPoisonAt - now + 500L) / 1_000L) + "s";

        List<Player> players = new ArrayList<>();
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()) players.add(player);
        }
        players.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GREEN + "Status");
        lines.add(ChatColor.WHITE + status);
        lines.add(" ");
        lines.add(ChatColor.AQUA + "Matches Left");
        lines.add(ChatColor.WHITE + Integer.toString(activeMatches));
        lines.add("  ");
        lines.add(ChatColor.YELLOW + "Players");
        if (players.size() > 7) lines.add(ChatColor.WHITE + Integer.toString(players.size()) + " Players");
        else for (Player player : players) lines.add(ChatColor.WHITE + player.getName());
        for (int index = 0; index < session.gladiatorScoreboardTeams.size(); index++) {
            Team line = session.gladiatorScoreboardTeams.get(index);
            String entry = session.gladiatorScoreboardEntries.get(index);
            if (index < lines.size()) {
                line.setPrefix(lines.get(index));
                session.gladiatorScoreboardObjective.getScore(entry).setScore(lines.size() - index);
            } else {
                line.setPrefix("");
                session.gladiatorScoreboard.resetScores(entry);
            }
        }
        updateBracketActionbars(session);
    }

    private void updateBracketActionbars(SessionState session) {
        if (session.gladiatorBracket == null) return;
        List<GladiatorsBracket.MatchView> matches = session.gladiatorBracket.matches();
        for (UUID playerId : session.alive) {
            Player player = session.players.get(playerId);
            if (player == null || !player.isOnline()) continue;
            GladiatorsBracket.MatchView next = matches.stream()
                    .filter(match -> match.status() != GladiatorsBracket.Status.COMPLETED
                            && (playerId.equals(match.first()) || playerId.equals(match.second())))
                    .min(Comparator.comparingInt(GladiatorsBracket.MatchView::round)).orElse(null);
            if (next == null) {
                player.sendActionBar(ChatColor.GOLD + "Bracket "
                        + (session.gladiatorBracket.losses(playerId) == 0 ? "Winners" : "Losers")
                        + " | Losses: " + session.gladiatorBracket.losses(playerId) + " | Waiting for opponent");
                continue;
            }
            String bracketName = switch (next.bracket()) {
                case WINNERS -> "Winners";
                case LOSERS -> "Losers";
                case GRAND_FINAL, GRAND_FINAL_RESET -> "Grand Final";
            };
            UUID opponentId = playerId.equals(next.first()) ? next.second() : next.first();
            Player opponent = opponentId == null ? null : session.players.get(opponentId);
            String opponentName = opponent == null ? "TBD" : opponent.getName();
            String state = next.status() == GladiatorsBracket.Status.FIGHTING ? "Duel active"
                    : next.status() == GladiatorsBracket.Status.READY ? "Ready" : "Waiting";
            player.sendActionBar(ChatColor.GOLD + bracketName + " R" + next.round() + " | Losses: "
                    + session.gladiatorBracket.losses(playerId) + " | vs " + opponentName + " | " + state);
        }
    }

    void cleanupScoreboard(SessionState session) {
        if (session == null || session.gladiatorScoreboard == null) return;
        try {
            if (session.gladiatorScoreboard.getObjective(DisplaySlot.SIDEBAR)
                    == session.gladiatorScoreboardObjective && session.gladiatorPreviousSidebar != null) {
                session.gladiatorPreviousSidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            for (Team team : new ArrayList<>(session.gladiatorScoreboardTeams)) {
                if (session.gladiatorScoreboard.getTeam(team.getName()) != null) team.unregister();
            }
            if (session.gladiatorScoreboardObjective != null) session.gladiatorScoreboardObjective.unregister();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.fine("Gladiators scoreboard was already removed: " + exception.getMessage());
        }
        session.gladiatorScoreboardTeams.clear();
        session.gladiatorScoreboardEntries.clear();
        session.gladiatorScoreboard = null;
        session.gladiatorScoreboardObjective = null;
        session.gladiatorPreviousSidebar = null;
    }
}
