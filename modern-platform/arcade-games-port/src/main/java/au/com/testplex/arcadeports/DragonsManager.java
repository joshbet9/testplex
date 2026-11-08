package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Dragons' scoreboard logic runs inside the shared Arcade match lifecycle.
 * Dragon spawning, movement, damage and kit mechanics remain in the coordinator.
 */
final class DragonsManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.DRAGONS; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupDragonsScoreboard(session);
    }

    private void setupDragonsScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("dragons", "dummy",
                ChatColor.GOLD + "DRAGONS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.dragonScoreboard = scoreboard;
        session.dragonScoreboardObjective = objective;
        session.dragonAliveScoreboardTeam = scoreboard.registerNewTeam("drgalive");
        session.dragonAliveScoreboardTeam.setPrefix(ChatColor.GREEN.toString());
        session.dragonDeadScoreboardTeam = scoreboard.registerNewTeam("drgdead");
        session.dragonDeadScoreboardTeam.setPrefix(ChatColor.GRAY.toString());
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateDragonsScoreboard(session, System.currentTimeMillis());
    }

    void updateDragonsScoreboard(SessionState session, long now) {
        if (session == null || session.type != ArcadePortGame.DRAGONS
                || session.dragonScoreboard == null || session.dragonScoreboardObjective == null
                || now < session.dragonNextScoreboardUpdateAt) return;
        session.dragonNextScoreboardUpdateAt = now + 500L; // SoloGame.ScoreboardUpdate uses UpdateType.FAST.
        Scoreboard scoreboard = session.dragonScoreboard;
        Objective objective = session.dragonScoreboardObjective;
        for (String entry : new ArrayList<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
            if (session.dragonAliveScoreboardTeam.hasEntry(entry)) session.dragonAliveScoreboardTeam.removeEntry(entry);
            if (session.dragonDeadScoreboardTeam.hasEntry(entry)) session.dragonDeadScoreboardTeam.removeEntry(entry);
        }

        List<Player> roster = new ArrayList<>(session.players.values());
        int total = roster.size();
        int alive = session.alive.size();
        int mode = ArcadePortRules.dragonsScoreboardMode(total, alive);
        if (mode == 0) {
            int score = total;
            for (Player player : roster) {
                if (player == null) continue;
                String entry = player.getName();
                if (session.alive.contains(player.getUniqueId())) {
                    session.dragonAliveScoreboardTeam.addEntry(entry);
                } else {
                    session.dragonDeadScoreboardTeam.addEntry(entry);
                }
                objective.getScore(entry).setScore(score--);
            }
        } else if (mode == 1) {
            int score = alive;
            for (Player player : roster) {
                if (player == null || !session.alive.contains(player.getUniqueId())) continue;
                String entry = player.getName();
                session.dragonAliveScoreboardTeam.addEntry(entry);
                objective.getScore(entry).setScore(score--);
            }
        } else {
            objective.getScore(ChatColor.GREEN + "Players Alive").setScore(4);
            objective.getScore(ChatColor.WHITE + Integer.toString(alive)).setScore(3);
            objective.getScore(ChatColor.RED + "Players Dead").setScore(2);
            objective.getScore(ChatColor.WHITE + Integer.toString(Math.max(0, total - alive))).setScore(1);
        }
        for (Player player : roster) {
            if (player != null && player.isOnline() && player.getScoreboard() != scoreboard) {
                player.setScoreboard(scoreboard);
            }
        }
    }

}
