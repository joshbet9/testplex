package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterBuildersDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void defaultBuildAndReviewScheduleTotalsFiveMinutesAcrossPlayerCounts() {
        long total = ArcadePortRules.masterBuildersTotalMillis();
        long review = ArcadePortRules.masterBuildersReviewMillis();
        for (int players : new int[] {1, 2, 5, 8, 16}) {
            int reviews = players <= 1 ? 0 : players;
            long build = ArcadeProgressRules.masterBuildersBuildMillis(total, reviews, review);
            assertEquals(total, build + reviews * review, "players=" + players);
        }
        assertEquals(300_000L, total);
        assertEquals(8_000L, review);
        assertEquals(8_000L, ArcadePortRules.masterBuildersVoteMillis());
    }

    @Test
    void themeOverrideSelectsARepeatableThemeAndZeroRestoresRandomSelection() {
        assertEquals(11, ArcadePortRules.masterBuildersThemeIndex(11));
        GameDebugRuntimeOverrides.activate(java.util.Map.of("MASTER_BUILDERS_THEME_OVERRIDE", 4));
        assertEquals(4, ArcadePortRules.masterBuildersThemeIndex(11));
        GameDebugRuntimeOverrides.activate(java.util.Map.of("MASTER_BUILDERS_THEME_OVERRIDE", 99));
        assertEquals(16, ArcadePortRules.masterBuildersThemeIndex(11));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(11, ArcadePortRules.masterBuildersThemeIndex(11));
    }

    @Test
    void builderLimitChangesQueueEligibilityWithoutExceedingAuthoredPlotCapacity() {
        assertEquals(16, ArcadePortRules.masterBuildersMaxBuilders());
        assertTrue(ArcadePortRules.supportsMasterBuildersPlayerCount(16));
        GameDebugVariableCatalog.Variable capacity = GameDebugVariableCatalog.variable(
                ArcadePortGame.MASTER_BUILDERS, "MASTER_BUILDERS_MAX_BUILDERS");
        assertNotNull(capacity);
        assertEquals(1.0D, capacity.minimum());
        assertEquals(16.0D, capacity.maximum());
        GameDebugRuntimeOverrides.activate(java.util.Map.of("MASTER_BUILDERS_MAX_BUILDERS", 6));
        assertEquals(6, ArcadePortRules.masterBuildersMaxBuilders());
        assertTrue(ArcadePortRules.supportsMasterBuildersPlayerCount(6));
        assertFalse(ArcadePortRules.supportsMasterBuildersPlayerCount(7));
        assertFalse(ArcadePortRules.supportsMasterBuildersPlayerCount(17));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(16, ArcadePortRules.masterBuildersMaxBuilders());
        assertTrue(ArcadePortRules.supportsMasterBuildersPlayerCount(16));
    }

    @Test
    void reviewAndVoteDurationsAreBoundedAndExposedWithSafeProfileRanges() {
        GameDebugRuntimeOverrides.activate(java.util.Map.ofEntries(
                java.util.Map.entry("MASTER_BUILDERS_REVIEW_MILLIS", 5_000L),
                java.util.Map.entry("MASTER_BUILDERS_VOTE_MILLIS", 9_000L)));
        assertEquals(5_000L, ArcadePortRules.masterBuildersReviewMillis());
        assertEquals(5_000L, ArcadePortRules.masterBuildersVoteMillis(),
                "voting cannot outlast the review phase");

        GameDebugVariableCatalog.Variable theme = GameDebugVariableCatalog.variable(
                ArcadePortGame.MASTER_BUILDERS, "MASTER_BUILDERS_THEME_OVERRIDE");
        assertNotNull(theme);
        assertEquals(0.0D, theme.minimum());
        assertEquals(16.0D, theme.maximum());
    }
}
