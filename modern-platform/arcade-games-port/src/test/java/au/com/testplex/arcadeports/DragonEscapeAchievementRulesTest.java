package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DragonEscapeAchievementRulesTest {
    @Test
    void photoFinishRequiresTwoFinishesWithinAQuarterSecond() {
        assertFalse(DragonEscapeAchievementRules.photoFinish(1_000L, 999L));
        assertFalse(DragonEscapeAchievementRules.photoFinish(1_000L, 1_251L));
        assertTrue(DragonEscapeAchievementRules.photoFinish(1_000L, 1_250L));
        assertTrue(DragonEscapeAchievementRules.photoFinish(1_000L, 1_001L));
    }
}
