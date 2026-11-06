package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeStatsLayoutTest {
    @Test
    void achievementsAlwaysUseCentredRowsAndNeverOverwriteNavigation() {
        for (int count = 0; count <= 14; count++) {
            List<Integer> slots = ArcadeStatsLayout.achievementSlots(count);
            assertEquals(count, slots.size());
            assertEquals(count, new HashSet<>(slots).size());
            assertTrue(slots.stream().allMatch(slot -> (slot >= 19 && slot <= 25)
                    || (slot >= 28 && slot <= 34)));
            for (int row : List.of(18, 27)) {
                List<Integer> columns = slots.stream().filter(slot -> slot / 9 == row / 9)
                        .map(slot -> slot % 9).toList();
                assertEquals(columns.stream().mapToInt(Integer::intValue).sum(), 4 * columns.size());
            }
        }
        assertEquals(List.of(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34),
                ArcadeStatsLayout.achievementSlots(14));
        assertEquals(List.of(20, 21, 23, 24, 29, 30, 32, 33), ArcadeStatsLayout.achievementSlots(8));
        assertEquals(2, ArcadeStatsLayout.pages(20, ArcadeStatsLayout.ACHIEVEMENTS_PER_PAGE));
    }

    @Test
    void milestoneColumnsAreSymmetricAndEveryTrackCanShowFourTiers() {
        assertEquals(List.of(1, 3, 5, 7), ArcadeStatsLayout.milestoneColumns(4));
        assertEquals(List.of(2, 4, 6), ArcadeStatsLayout.milestoneColumns(3));
        assertEquals(List.of(3, 5), ArcadeStatsLayout.milestoneColumns(2));
        assertEquals(List.of(4), ArcadeStatsLayout.milestoneColumns(1));
        assertEquals(2, ArcadeStatsLayout.pages(7, ArcadeStatsLayout.TRACKS_PER_PAGE));
        assertEquals(1, ArcadeStatsLayout.pages(0, ArcadeStatsLayout.TRACKS_PER_PAGE));
    }

    @Test
    void footerAndPageBoundariesRemainAccessible() {
        assertEquals(List.of(49), ArcadeStatsLayout.footerSlots(1));
        assertEquals(List.of(48, 50), ArcadeStatsLayout.footerSlots(2));
        assertEquals(0, ArcadeStatsLayout.clampPage(-99, 16, 14));
        assertEquals(1, ArcadeStatsLayout.clampPage(99, 16, 14));
        assertArrayEquals(new long[] {25, 63, 125, 250}, ArcadeStatsLayout.fourStages(250));
    }
}
