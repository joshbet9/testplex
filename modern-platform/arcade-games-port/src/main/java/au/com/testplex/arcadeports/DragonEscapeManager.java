package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Dragon Escape's race sidebar; movement and course timing remain in the shared match loop. */
final class DragonEscapeManager implements ArcadeMatchController {
    private final Logger logger;

    DragonEscapeManager(Logger logger) {
        this.logger = logger;
    }

    @Override public ArcadePortGame game() { return ArcadePortGame.DRAGON_ESCAPE; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupScoreboard(session);
    }

    private void setupScoreboard(SessionState session) {
        if (session == null || session.type != ArcadePortGame.DRAGON_ESCAPE
                || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("dragonrace", "dummy",
                ChatColor.GOLD + "DRAGON ESCAPE");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.dragonEscapeScoreboard = scoreboard;
        session.dragonEscapeScoreboardObjective = objective;
        for (int i = 0; i < 14; i++) {
            Team line = scoreboard.registerNewTeam("derank" + i);
            String entry = ChatColor.values()[i].toString();
            line.addEntry(entry);
            session.dragonEscapeScoreboardTeams.add(line);
            session.dragonEscapeScoreboardEntries.add(entry);
        }
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
    }

    /** Reborn shows the top 14 ranked racers; the modern sidebar also shows live race times. */
    void updateScoreboard(SessionState session, long now, List<Player> liveOrder,
            BiFunction<Player, Player, Long> gapMillis) {
        if (session == null) return;
        Scoreboard scoreboard = session.dragonEscapeScoreboard;
        Objective objective = session.dragonEscapeScoreboardObjective;
        if (scoreboard == null || objective == null || session.type != ArcadePortGame.DRAGON_ESCAPE) return;
        List<Player> ranked = new ArrayList<>();
        for (Player player : session.players.values()) {
            if (player != null && session.dragonEscapePlayerRankScores.containsKey(player.getUniqueId())) {
                ranked.add(player);
            }
        }
        ranked.sort((left, right) -> Double.compare(
                session.dragonEscapePlayerRankScores.get(right.getUniqueId()),
                session.dragonEscapePlayerRankScores.get(left.getUniqueId())));
        Player liveLeader = liveOrder.isEmpty() ? null : liveOrder.get(0);
        long elapsed = Math.max(0L, now - session.startedAt);
        for (int i = 0; i < session.dragonEscapeScoreboardTeams.size(); i++) {
            Team line = session.dragonEscapeScoreboardTeams.get(i);
            String entry = session.dragonEscapeScoreboardEntries.get(i);
            if (i < ranked.size()) {
                Player racer = ranked.get(i);
                boolean alive = session.alive.contains(racer.getUniqueId());
                ChatColor colour = alive ? ChatColor.GREEN : ChatColor.RED;
                line.setPrefix(colour + "#" + (i + 1) + " " + racer.getName());
                if (!alive) {
                    line.setSuffix(ChatColor.DARK_RED + " OUT");
                } else if (liveLeader != null
                        && racer.getUniqueId().equals(liveLeader.getUniqueId())) {
                    line.setSuffix(ChatColor.YELLOW + " " + ArcadePortRules.formatDragonEscapeTime(elapsed));
                } else if (liveLeader != null) {
                    long gap = gapMillis.apply(liveLeader, racer);
                    line.setSuffix(ChatColor.RED + " ~+" + ArcadePortRules.formatDragonEscapeTime(gap));
                } else {
                    line.setSuffix("");
                }
                objective.getScore(entry).setScore(14 - i);
            } else {
                line.setPrefix("");
                line.setSuffix("");
                scoreboard.resetScores(entry);
            }
        }
    }

    void cleanupScoreboard(SessionState session) {
        if (session == null) return;
        Scoreboard scoreboard = session.dragonEscapeScoreboard;
        Objective objective = session.dragonEscapeScoreboardObjective;
        if (scoreboard == null) return;
        try {
            for (Team team : new ArrayList<>(session.dragonEscapeScoreboardTeams)) {
                if (scoreboard.getTeam(team.getName()) != null) team.unregister();
            }
            if (objective != null && scoreboard.getObjective(objective.getName()) != null) {
                objective.unregister();
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.fine("Dragon Escape scoreboard was already removed: " + exception.getMessage());
        }
        session.dragonEscapeScoreboardTeams.clear();
        session.dragonEscapeScoreboardEntries.clear();
        session.dragonEscapeScoreboardObjective = null;
        session.dragonEscapeScoreboard = null;
    }
}
