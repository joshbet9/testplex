package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SheepQuestScoreboardRulesTest {
    @Test void rendersAllFourSourceTeamSheepCountsInTeamColours() {
        assertEquals(List.of(
                ChatColor.RED + "Red" + ChatColor.WHITE + ": 4",
                ChatColor.AQUA + "Aqua" + ChatColor.WHITE + ": 2",
                ChatColor.GREEN + "Green" + ChatColor.WHITE + ": 0",
                ChatColor.YELLOW + "Yellow" + ChatColor.WHITE + ": 1"),
                SheepQuestScoreboardRules.teamCountLines(new int[] {4, 2, 0, 1}));
    }

    @Test void rejectsCountsThatDoNotCoverEveryTeam() {
        assertThrows(IllegalArgumentException.class,
                () -> SheepQuestScoreboardRules.teamCountLines(new int[] {1, 2, 3}));
    }
}
