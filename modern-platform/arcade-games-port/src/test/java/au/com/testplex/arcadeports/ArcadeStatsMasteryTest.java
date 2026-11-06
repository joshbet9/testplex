package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeStatsMasteryTest {
    @Test void onlyFullAchievementAndMilestoneCompletionUnlocksTheIconGlint() {
        assertFalse(ArcadeStatsMastery.mastered(9, 10, 20, 20));
        assertFalse(ArcadeStatsMastery.mastered(10, 10, 19, 20));
        assertTrue(ArcadeStatsMastery.mastered(10, 10, 20, 20));
    }

    @Test void emptyCataloguesNeverCountAsMastered() {
        assertFalse(ArcadeStatsMastery.mastered(0, 0, 0, 0));
        assertFalse(ArcadeStatsMastery.mastered(0, 0, 20, 20));
        assertFalse(ArcadeStatsMastery.mastered(10, 10, 0, 0));
    }
}
