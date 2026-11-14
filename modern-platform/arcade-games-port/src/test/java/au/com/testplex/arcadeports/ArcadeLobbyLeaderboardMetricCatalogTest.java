package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeLobbyLeaderboardMetricCatalogTest {
    @Test
    void includesEveryTrackedNestedAndBaseStatisticWithoutDuplicates() {
        List<ArcadeLobbyLeaderboardMetricCatalog.Metric> metrics =
                ArcadeLobbyLeaderboardMetricCatalog.metrics("runner", List.of(
                        "wins", "games-played", "switcher-catches", "map-wins.water-damage",
                        "kit-wins.builder"));

        List<String> ids = metrics.stream().map(ArcadeLobbyLeaderboardMetricCatalog.Metric::id).toList();
        assertTrue(ids.containsAll(List.of("wins", "games-played", "losses", "test-gems-earned",
                "switcher-catches", "map-wins.water-damage", "kit-wins.builder")));
        assertEquals(ids.size(), ids.stream().distinct().count());
    }

    @Test
    void marksCompletionTimeMetricsAsLowestWinsAndCountsAsHighestWins() {
        var metrics = ArcadeLobbyLeaderboardMetricCatalog.metrics("dragon-escape",
                List.of("best-time", "time-survived", "wins"));

        assertTrue(metrics.stream().filter(metric -> metric.id().equals("pb"))
                .findFirst().orElseThrow().lowerIsBetter());
        assertTrue(metrics.stream().filter(metric -> metric.id().equals("best-time"))
                .findFirst().orElseThrow().lowerIsBetter());
        assertFalse(metrics.stream().filter(metric -> metric.id().equals("time-survived"))
                .findFirst().orElseThrow().lowerIsBetter());
    }

    @Test
    void rejectsInvalidMetricPathsAndMakesReadableLabels() {
        assertFalse(ArcadeLobbyLeaderboardMetricCatalog.isSelectableStatId("../wins"));
        assertFalse(ArcadeLobbyLeaderboardMetricCatalog.isSelectableStatId("wins/../../config"));
        assertFalse(ArcadeLobbyLeaderboardMetricCatalog.isSelectableStatId(" "));
        assertTrue(ArcadeLobbyLeaderboardMetricCatalog.isSelectableStatId("kit-wins.builder"));
        assertEquals("Kit Wins: Builder", ArcadeLobbyLeaderboardMetricCatalog.label("kit-wins.builder"));
    }
}
