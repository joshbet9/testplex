package au.com.testplex.arcadeports;

import java.util.List;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonEscapeRaceScoreboardLayoutTest {
    @Test
    void reservesSixRacersTwoPbRowsAndSeparateSpacersForRecordsAndCountdown() {
        assertEquals(12, DragonEscapeRaceScoreboardLayout.MAX_ROWS);
        assertEquals(6, DragonEscapeRaceScoreboardLayout.RACER_ROW_COUNT);
        assertEquals(6, DragonEscapeRaceScoreboardLayout.STANDINGS_SEPARATOR_ROW);
        assertEquals(7, DragonEscapeRaceScoreboardLayout.MAP_PB_ROW);
        assertEquals(8, DragonEscapeRaceScoreboardLayout.KIT_PB_ROW);
        assertEquals(9, DragonEscapeRaceScoreboardLayout.FINISH_SEPARATOR_ROW);
        assertEquals(10, DragonEscapeRaceScoreboardLayout.FINISH_TIMER_LABEL_ROW);
        assertEquals(11, DragonEscapeRaceScoreboardLayout.FINISH_TIMER_VALUE_ROW);
    }

    @Test
    void showsTopThreeAndImmediateNeighboursOfViewerWithoutDuplication() {
        assertEquals(List.of(0, 1, 2, 5, 6, 7),
                DragonEscapeRaceScoreboardLayout.visibleRankPositions(16, 6));
        assertEquals(List.of(0, 1, 2),
                DragonEscapeRaceScoreboardLayout.visibleRankPositions(16, 1));
        assertEquals(List.of(0, 1, 2, 3, 4),
                DragonEscapeRaceScoreboardLayout.visibleRankPositions(16, 3));
        assertEquals(List.of(0, 1, 2, 14, 15),
                DragonEscapeRaceScoreboardLayout.visibleRankPositions(16, 15));
        assertEquals(List.of(0, 1, 2),
                DragonEscapeRaceScoreboardLayout.visibleRankPositions(3, 2));
        assertEquals(List.of(0, 1, 2),
                DragonEscapeRaceScoreboardLayout.visibleRankPositions(16, -1));
        assertEquals(List.of(),
                DragonEscapeRaceScoreboardLayout.visibleRankPositions(0, -1));
    }

    @Test
    void placementPrefixShowsRealRankAndOnlyViewerNameIsBrightGreen() {
        assertEquals(ChatColor.YELLOW + "1. Racer",
                DragonEscapeRaceScoreboardLayout.racerName(
                        "Racer", DragonEscapeRaceScoreboardLayout.RacerStatus.RUNNING, 1, false));
        assertEquals(ChatColor.GREEN + "7. JN_PlayzMC",
                DragonEscapeRaceScoreboardLayout.racerName(
                        "JN_PlayzMC", DragonEscapeRaceScoreboardLayout.RacerStatus.RUNNING, 7, true));
        assertEquals(ChatColor.AQUA + "2. Racer",
                DragonEscapeRaceScoreboardLayout.racerName(
                        "Racer", DragonEscapeRaceScoreboardLayout.RacerStatus.FINISHED, 2, false));
        assertTrue(DragonEscapeRaceScoreboardLayout.racerName(
                "Racer", DragonEscapeRaceScoreboardLayout.RacerStatus.ELIMINATED, 13, true)
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
        assertEquals("",
                DragonEscapeRaceScoreboardLayout.racerTime(
                        "0:42.10", DragonEscapeRaceScoreboardLayout.RacerStatus.ELIMINATED));
    }

    @Test
    void firstLiveFrameIsExactlyZeroWithoutChangingLaterElapsedTime() {
        assertEquals(0L, DragonEscapeRaceScoreboardLayout.displayedElapsedMillis(40L, true));
        assertEquals(0L, DragonEscapeRaceScoreboardLayout.displayedElapsedMillis(-40L, false));
        assertEquals(125L, DragonEscapeRaceScoreboardLayout.displayedElapsedMillis(125L, false));
    }

    @Test
    void finishWindowRetainsTenthsAndStopsAtZero() {
        assertEquals(29_000L, DragonEscapeRaceScoreboardLayout.remainingFinishWindowMillis(30_000L, 1_000L));
        assertEquals("29.0 Seconds", DragonEscapeRaceScoreboardLayout.finishWindowSeconds(29_000L));
        assertEquals("0.0 Seconds", DragonEscapeRaceScoreboardLayout.finishWindowSeconds(0L));
        assertEquals(0L, DragonEscapeRaceScoreboardLayout.remainingFinishWindowMillis(30_000L, 40_000L));
    }

    @Test
    void kitRecordRowsNameTheSelectedKit() {
        assertEquals("Builder PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("builder", false));
        assertEquals("Jumper PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("jumper", false));
        assertEquals("Disruptor PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("disruptor", false));
        assertEquals("Warper PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("warper", false));
        assertEquals("Digger PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("digger", false));
        assertEquals("Acrobat PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("acrobat", false));
        assertEquals("Flutterfly PB", DragonEscapeRaceScoreboardLayout.kitRecordLabel("flutterfly", false));
    }
}
