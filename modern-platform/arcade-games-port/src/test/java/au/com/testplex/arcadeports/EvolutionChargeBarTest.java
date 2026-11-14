package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvolutionChargeBarTest {
    @Test
    void centeredSourceBarHasFortySegmentsAndClampsProgress() {
        assertEquals(0, EvolutionChargeBar.filledSegments(-1.0F));
        assertEquals(20, EvolutionChargeBar.filledSegments(0.5F));
        assertEquals(40, EvolutionChargeBar.filledSegments(2.0F));
        assertEquals(40, EvolutionChargeBar.format(0.5F).chars().filter(c -> c == '|').count());
    }

    @Test
    void scoreboardShowsOnlyPlayersThatFit() {
        assertEquals(0, EvolutionChargeBar.visibleRankedPlayers(-2));
        assertEquals(4, EvolutionChargeBar.visibleRankedPlayers(4));
        assertEquals(9, EvolutionChargeBar.visibleRankedPlayers(15));
        assertTrue(EvolutionChargeBar.format(1.0F).contains(ChatColor.GREEN.toString()));
    }
}
