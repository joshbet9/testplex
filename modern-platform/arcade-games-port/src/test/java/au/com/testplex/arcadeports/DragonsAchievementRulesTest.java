package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class DragonsAchievementRulesTest {
    @Test
    void dragonOwnerGetsFallCreditOnlyForTheirMostRecentRecentContact() {
        java.util.UUID victim = java.util.UUID.randomUUID();
        java.util.UUID sparkler = java.util.UUID.randomUUID();
        java.util.UUID tamer = java.util.UUID.randomUUID();
        long now = 20_000L;
        assertEquals(tamer, DragonsAchievementRules.resolveDragonFallKiller(victim, null,
                sparkler, 15_000L, tamer, 18_000L, now, "fell into the void"));
        assertEquals(sparkler, DragonsAchievementRules.resolveDragonFallKiller(victim, null,
                sparkler, 19_000L, tamer, 18_000L, now, "fell from a high place"));
        assertEquals(null, DragonsAchievementRules.resolveDragonFallKiller(victim, null,
                sparkler, 9_999L, tamer, 0L, now, "fell into the void"));
        assertEquals(null, DragonsAchievementRules.resolveDragonFallKiller(victim, null,
                victim, now, tamer, 0L, now, "fell into the void"));
        assertEquals(null, DragonsAchievementRules.resolveDragonFallKiller(victim, null,
                sparkler, now, null, 0L, now, "was slain"));
    }

    @Test
    void directDragonDeathAttributionTakesPrecedenceOverFallCredit() {
        java.util.UUID victim = java.util.UUID.randomUUID();
        java.util.UUID direct = java.util.UUID.randomUUID();
        java.util.UUID tamer = java.util.UUID.randomUUID();
        assertEquals(direct, DragonsAchievementRules.resolveDragonFallKiller(victim, direct,
                null, 0L, tamer, 19_000L, 20_000L, "fell into the void"));
    }

    @Test
    void gliderRequiresTwentyFiveBlocksBeforeLanding() {
        assertTrue(DragonsAchievementRules.gliderDistanceReached(25.0D, false));
        assertFalse(DragonsAchievementRules.gliderDistanceReached(24.99D, false));
        assertFalse(DragonsAchievementRules.gliderDistanceReached(30.0D, true));
        assertFalse(DragonsAchievementRules.gliderDistanceReached(Double.NaN, false));
    }

    @Test
    void snowPatrolRequiresASeparateLivingPlayerToRecoverFromAFallOnFrostIce() {
        java.util.UUID frost = java.util.UUID.randomUUID();
        java.util.UUID rescuer = java.util.UUID.randomUUID();
        assertFalse(DragonsAchievementRules.snowPatrolRescue(frost, frost, 8.0D, true));
        assertFalse(DragonsAchievementRules.snowPatrolRescue(frost, rescuer, 2.99D, true));
        assertFalse(DragonsAchievementRules.snowPatrolRescue(frost, rescuer, 5.0D, false));
        assertTrue(DragonsAchievementRules.snowPatrolRescue(frost, rescuer, 3.0D, true));
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
