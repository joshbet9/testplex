package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Castle Assault's chest/TNT/king scoreboard, owned by the existing variant runtime. */
final class CastleAssaultManager {
    private Scoreboard scoreboard;
    private Objective objective;

    Scoreboard scoreboard() { return scoreboard; }

    boolean hasScoreboard() { return scoreboard != null && objective != null; }

    void createScoreboard(Collection<Player> players) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("castleassault", "dummy",
                ChatColor.DARK_RED + "Castle Assault");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (Player player : players) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
    }

    void updateScoreboard(long now, long nextChestRefillAt, long objectiveRespawnAt,
            boolean objectiveTntSpawned, String redKingStatus, String blueKingStatus) {
        if (!hasScoreboard()) return;
        for (String entry : new ArrayList<>(scoreboard.getEntries())) scoreboard.resetScores(entry);
        int score = 8;
        objective.getScore(ChatColor.GREEN + "Chest Refill").setScore(score--);
        objective.getScore(ChatColor.WHITE + formatCountdown(nextChestRefillAt - now)
                + ChatColor.GREEN).setScore(score--);
        objective.getScore(ChatColor.GREEN + "TNT Spawn").setScore(score--);
        String tntStatus = objectiveTntSpawned ? "Spawned" : formatCountdown(objectiveRespawnAt - now);
        objective.getScore(ChatColor.WHITE + tntStatus + ChatColor.GOLD).setScore(score--);
        objective.getScore(ChatColor.RED + "Red King").setScore(score--);
        objective.getScore(ChatColor.WHITE + redKingStatus + ChatColor.RED).setScore(score--);
        objective.getScore(ChatColor.AQUA + "Blue King").setScore(score--);
        objective.getScore(ChatColor.WHITE + blueKingStatus + ChatColor.AQUA).setScore(score);
    }

    static String formatCountdown(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1_000L);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }
}
