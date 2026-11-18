package au.com.testplex.arcadeports;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Hides the right-hand red score values on every player-visible Testplex
 * sidebar, including scoreboards owned by the lobby or separate game plugins.
 * Underlying numeric scores remain untouched so row ordering still works.
 */
final class SidebarNumberFormat {
    private static final NumberFormat BLANK = NumberFormat.blank();

    private SidebarNumberFormat() { }

    static void applyToOnlinePlayers() {
        // Shared team scoreboards should only be inspected once per refresh.
        Set<Scoreboard> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null || !visited.add(board)) continue;
            for (Objective objective : board.getObjectives()) {
                DisplaySlot slot = objective.getDisplaySlot();
                if (slot != null && slot.name().startsWith("SIDEBAR")
                        && !BLANK.equals(objective.numberFormat())) {
                    objective.numberFormat(BLANK);
                }
            }
        }
    }
}
