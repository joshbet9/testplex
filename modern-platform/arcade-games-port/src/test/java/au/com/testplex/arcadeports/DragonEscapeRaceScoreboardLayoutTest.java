package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonEscapeRaceScoreboardLayoutTest {
    @Test
    void reservesFourDistinctRecordRowsBelowElevenOrderedRacers() {
        assertEquals(15, DragonEscapeRaceScoreboardLayout.MAX_ROWS);
        assertEquals(11, DragonEscapeRaceScoreboardLayout.RACER_ROW_COUNT);
        assertEquals(11, DragonEscapeRaceScoreboardLayout.MAP_PB_ROW);
        assertEquals(12, DragonEscapeRaceScoreboardLayout.MAP_WR_ROW);
        assertEquals(13, DragonEscapeRaceScoreboardLayout.KIT_PB_ROW);
        assertEquals(14, DragonEscapeRaceScoreboardLayout.KIT_WR_ROW);
    }

    @Test
    void racerRowsUsePositionInsteadOfNumberedPrefixesAndStyleTheirState() {
        assertEquals(ChatColor.YELLOW + "Racer", DragonEscapeRaceScoreboardLayout.racerName(
                "Racer", DragonEscapeRaceScoreboardLayout.RacerStatus.RUNNING));
        assertEquals(ChatColor.AQUA + "Racer", DragonEscapeRaceScoreboardLayout.racerName(
                "Racer", DragonEscapeRaceScoreboardLayout.RacerStatus.FINISHED));
        assertTrue(DragonEscapeRaceScoreboardLayout.racerName(
                "Racer", DragonEscapeRaceScoreboardLayout.RacerStatus.ELIMINATED)
                .contains(ChatColor.STRIKETHROUGH.toString()));
    }

    @Test
    void finishedAndEliminatedRowsKeepTheirRecordedTimer() {
        assertEquals(ChatColor.AQUA + " 1:23.45" + ChatColor.RESET,
                DragonEscapeRaceScoreboardLayout.racerTime(
                        "1:23.45", DragonEscapeRaceScoreboardLayout.RacerStatus.FINISHED));
        String eliminated = DragonEscapeRaceScoreboardLayout.racerTime(
                "0:42.10", DragonEscapeRaceScoreboardLayout.RacerStatus.ELIMINATED);
        assertTrue(eliminated.contains("0:42.10"));
        assertTrue(eliminated.contains(ChatColor.STRIKETHROUGH.toString()));
    }
}
