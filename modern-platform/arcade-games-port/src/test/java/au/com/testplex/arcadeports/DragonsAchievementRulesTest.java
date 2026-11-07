package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class DragonsAchievementRulesTest {
    @Test
    void gliderRequiresTwentyFiveBlocksBeforeLanding() {
        assertTrue(DragonsAchievementRules.gliderDistanceReached(25.0D, false));
        assertFalse(DragonsAchievementRules.gliderDistanceReached(24.99D, false));
        assertFalse(DragonsAchievementRules.gliderDistanceReached(30.0D, true));
        assertFalse(DragonsAchievementRules.gliderDistanceReached(Double.NaN, false));
    }

    @Test
    void tamerCreditRequiresItsControlledDragonAndFallOrVoidEliminationWithinTenSeconds() {
        assertTrue(DragonsAchievementRules.tamerFallElimination(true, "fell into the void", 10_000L));
        assertFalse(DragonsAchievementRules.tamerFallElimination(false, "fell into the void", 1_000L));
        assertFalse(DragonsAchievementRules.tamerFallElimination(true, "fell into the void", 10_001L));
        assertFalse(DragonsAchievementRules.tamerFallElimination(true, "was killed", 1_000L));
    }

    @Test
    void freefallinForecastRequiresADeathWithinTheThreeSecondHorizon() {
        List<Double> openFall = Collections.nCopies(
                DragonsAchievementRules.FREEFALLIN_HORIZON_TICKS, Double.NaN);
        assertTrue(DragonsAchievementRules.wouldDieFromFallWithinThreeSeconds(
                -20.0D, -2.0D, 8.0D, 20.0D, -64.0D, openFall, 1.0D));
        assertFalse(DragonsAchievementRules.wouldDieFromFallWithinThreeSeconds(
                100.0D, -0.05D, 1.0D, 20.0D, -64.0D, openFall, 1.0D));
        assertFalse(DragonsAchievementRules.wouldDieFromFallWithinThreeSeconds(
                20.0D, 0.1D, 0.0D, 20.0D, -64.0D, openFall, 1.0D));
    }

    @Test
    void freefallinForecastDistinguishesSafeAndLethalLanding() {
        List<Double> floor = new ArrayList<>(Collections.nCopies(
                DragonsAchievementRules.FREEFALLIN_HORIZON_TICKS, Double.NaN));
        floor.set(2, 0.0D);
        assertFalse(DragonsAchievementRules.wouldDieFromFallWithinThreeSeconds(
                10.0D, -4.0D, 0.0D, 20.0D, -64.0D, floor, 1.0D));
        assertTrue(DragonsAchievementRules.wouldDieFromFallWithinThreeSeconds(
                10.0D, -4.0D, 30.0D, 10.0D, -64.0D, floor, 1.0D));
    }
}
