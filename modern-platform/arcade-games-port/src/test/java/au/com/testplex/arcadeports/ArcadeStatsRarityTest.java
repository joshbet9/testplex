package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeStatsRarityTest {
    @Test
    void sortsRarestFirstAndKeepsStableCatalogueOrderForTies() {
        Map<String, Integer> counts = Map.of("common", 8, "hard-one", 1,
                "hard-two", 1, "unearned", 0);
        assertEquals(List.of("unearned", "hard-one", "hard-two", "common"),
                ArcadeStatsRarity.hardestFirst(List.of("common", "hard-one", "hard-two", "unearned"), counts::get));
    }

    @Test
    void neverClaimsZeroPercentWhenNoPlayersAreEligible() {
        assertEquals("N/A (no recorded players)", ArcadeStatsRarity.percent(0, 0));
        assertEquals("12.5% (1/8 players)", ArcadeStatsRarity.percent(1, 8));
        assertEquals("100.0% (8/8 players)", ArcadeStatsRarity.percent(9, 8));
    }
}
