package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArcadeStatsPlacementTest {
    @Test void topTenHundredAndThousandUseAbsoluteRankColours() {
        assertEquals(ArcadeStatsPlacement.Band.PINK, placementAt(10, 2_000).band());
        assertEquals(ArcadeStatsPlacement.Band.BLUE, placementAt(11, 2_000).band());
        assertEquals(ArcadeStatsPlacement.Band.BLUE, placementAt(100, 2_000).band());
        assertEquals(ArcadeStatsPlacement.Band.GREEN, placementAt(101, 2_000).band());
        assertEquals(ArcadeStatsPlacement.Band.GREEN, placementAt(1_000, 2_000).band());
    }

    @Test void laterRanksUseTopPercentileWithCorrectColourThresholds() {
        assertEquals(ArcadeStatsPlacement.Band.YELLOW, placementAt(1_001, 40_000).band());
        assertEquals("top 2.6%", placementAt(1_001, 40_000).label());
        assertEquals(ArcadeStatsPlacement.Band.YELLOW, placementAt(1_001, 20_020).band());
        assertEquals(ArcadeStatsPlacement.Band.WHITE, placementAt(1_001, 10_000).band());
        assertEquals(ArcadeStatsPlacement.Band.WHITE, placementAt(1_001, 4_004).band());
        assertEquals(ArcadeStatsPlacement.Band.GRAY, placementAt(1_001, 2_000).band());
    }

    @Test void equalScoresShareRankAndFastestTimeWins() {
        assertEquals("#2", ArcadeStatsPlacement.higherIsBetter(5L, List.of(7L, 5L, 5L, 0L)).label());
        assertEquals("#2", ArcadeStatsPlacement.lowerIsBetter(5L, List.of(3L, 5L, 5L, 8L)).label());
        assertEquals("#1", ArcadeStatsPlacement.higherIsBetter(0L, List.of(0L, 0L)).label());
        assertNull(ArcadeStatsPlacement.lowerIsBetter(100L, List.of()));
    }

    private static ArcadeStatsPlacement.Result placementAt(int rank, int population) {
        List<Long> scores = new ArrayList<>(population);
        for (int i = 1; i <= population; i++) scores.add((long) (population - i));
        return ArcadeStatsPlacement.higherIsBetter(population - rank, scores);
    }
}
