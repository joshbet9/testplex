package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonEscapeRaceScoreboardLayoutTest {
    @Test
    void reservesRecordAndFinishRowsAfterOrderedRacers() {
        assertEquals(15, DragonEscapeRaceScoreboardLayout.MAX_ROWS);
        assertEquals(9, DragonEscapeRaceScoreboardLayout.RACER_ROW_COUNT);
        assertEquals(9, DragonEscapeRaceScoreboardLayout.MAP_PB_ROW);
        assertEquals(10, DragonEscapeRaceScoreboardLayout.MAP_WR_ROW);
        assertEquals(11, DragonEscapeRaceScoreboardLayout.KIT_PB_ROW);
        assertEquals(12, DragonEscapeRaceScoreboardLayout.KIT_WR_ROW);
        assertEquals(13, DragonEscapeRaceScoreboardLayout.FINISH_TIMER_LABEL_ROW);
        assertEquals(14, DragonEscapeRaceScoreboardLayout.FINISH_TIMER_VALUE_ROW);
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
    void finishedRowsKeepTimerButEliminatedRowsShowOnlyTheirName() {
        assertEquals(ChatColor.AQUA + " 1:23.45" + ChatColor.RESET,
                DragonEscapeRaceScoreboardLayout.racerTime(
                        "1:23.45", DragonEscapeRaceScoreboardLayout.RacerStatus.FINISHED));
        assertEquals(ChatColor.GREEN + " 0:42.10" + ChatColor.RESET,
                DragonEscapeRaceScoreboardLayout.racerTime(
                        "0:42.10", DragonEscapeRaceScoreboardLayout.RacerStatus.RUNNING, true));
        assertEquals(ChatColor.RED + " 0:43.10" + ChatColor.RESET,
                DragonEscapeRaceScoreboardLayout.racerTime(
                        "0:43.10", DragonEscapeRaceScoreboardLayout.RacerStatus.RUNNING, false));
        String eliminated = DragonEscapeRaceScoreboardLayout.racerTime(
                "0:42.10", DragonEscapeRaceScoreboardLayout.RacerStatus.ELIMINATED);
        assertEquals("", eliminated);
    }

    @Test
    void firstLiveFrameIsExactlyZeroWithoutChangingLaterElapsedTime() {
        assertEquals(0L, DragonEscapeRaceScoreboardLayout.displayedElapsedMillis(40L, true));
        assertEquals(0L, DragonEscapeRaceScoreboardLayout.displayedElapsedMillis(-40L, false));
        assertEquals(125L, DragonEscapeRaceScoreboardLayout.displayedElapsedMillis(125L, false));
    }

    @Test
    void firstFinisherCountdownDisplaysTenthsAndStopsAtZero() {
        assertEquals(29_000L, DragonEscapeRaceScoreboardLayout.remainingFinishWindowMillis(30_000L, 1_000L));
        assertEquals("29.0 Seconds", DragonEscapeRaceScoreboardLayout.finishWindowSeconds(29_000L));
        assertEquals("0.0 Seconds", DragonEscapeRaceScoreboardLayout.finishWindowSeconds(0L));
        assertEquals(0L, DragonEscapeRaceScoreboardLayout.remainingFinishWindowMillis(30_000L, 40_000L));
    }

    @Test
    void kitRecordRowsNameTheSelectedKit() {
        assertEquals("Builder PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("builder", false));
        assertEquals("Builder WR", DragonEscapeRaceScoreboardLayout.kitRecordLabel("builder", true));
        assertEquals("Jumper PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("jumper", false));
        assertEquals("Jumper WR", DragonEscapeRaceScoreboardLayout.kitRecordLabel("jumper", true));
        assertEquals("Disruptor PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("disruptor", false));
        assertEquals("Warper WR", DragonEscapeRaceScoreboardLayout.kitRecordLabel("warper", true));
        assertEquals("Digger PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("digger", false));
    }
}
