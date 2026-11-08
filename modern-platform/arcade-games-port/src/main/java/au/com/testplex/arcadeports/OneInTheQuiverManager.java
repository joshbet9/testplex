package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Quiver's ranking and below-name kill counter, driven by the shared session. */
final class OneInTheQuiverManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.ONE_IN_THE_QUIVER; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupScoreboard(session);
    }

    private void setupScoreboard(SessionState session) {
        if (session.type != ArcadePortGame.ONE_IN_THE_QUIVER || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("quiver", "dummy",
                ChatColor.GOLD + "ONE IN THE QUIVER");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Objective belowName = scoreboard.registerNewObjective("quiverkills", "dummy",
                ChatColor.RED + "Kills");
        belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
        session.quiverScoreboard = scoreboard;
        session.quiverScoreboardObjective = objective;
        session.quiverBelowNameObjective = belowName;
        for (int i = 0; i < 14; i++) {
            Team line = scoreboard.registerNewTeam("qvrank" + i);
            String entry = ChatColor.values()[i].toString();
            line.addEntry(entry);
            session.quiverScoreboardTeams.add(line);
            session.quiverScoreboardEntries.add(entry);
        }
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateScoreboard(session);
    }

    void updateScoreboard(SessionState session) {
        if (session == null) return;
        Scoreboard scoreboard = session.quiverScoreboard;
        Objective objective = session.quiverScoreboardObjective;
        Objective belowName = session.quiverBelowNameObjective;
        if (scoreboard == null || objective == null || session.type != ArcadePortGame.ONE_IN_THE_QUIVER) return;
        List<Player> ranked = new ArrayList<>(session.players.values());
        ranked.removeIf(player -> player == null || !player.isOnline());
        ranked.sort((left, right) -> {
            int byKills = Integer.compare(session.scores.getOrDefault(right.getUniqueId(), 0),
                    session.scores.getOrDefault(left.getUniqueId(), 0));
            return byKills != 0 ? byKills
                    : String.CASE_INSENSITIVE_ORDER.compare(left.getName(), right.getName());
        });
        for (int i = 0; i < session.quiverScoreboardTeams.size(); i++) {
            Team line = session.quiverScoreboardTeams.get(i);
            String entry = session.quiverScoreboardEntries.get(i);
            if (i < ranked.size()) {
                Player rankedPlayer = ranked.get(i);
                boolean alive = session.alive.contains(rankedPlayer.getUniqueId());
                int kills = session.scores.getOrDefault(rankedPlayer.getUniqueId(), 0);
                line.setPrefix((alive ? ChatColor.GREEN : ChatColor.RED) + "#" + (i + 1)
                        + " " + rankedPlayer.getName());
                line.setSuffix(ChatColor.GRAY + " " + kills + "/" + ArcadePortRules.quiverScoreToWin());
                objective.getScore(entry).setScore(session.quiverScoreboardTeams.size() - i);
            } else {
                line.setPrefix("");
                line.setSuffix("");
                scoreboard.resetScores(entry);
            }
        }
        if (belowName != null) {
            for (Player player : ranked) {
                belowName.getScore(player.getName()).setScore(
                        session.scores.getOrDefault(player.getUniqueId(), 0));
            }
        }
    }
}
