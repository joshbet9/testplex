package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DragonsCommunityRulesTest {
    @Test void allSixDragonsKitsHaveStableIdsAndAliases() {
        assertEquals("coward", ArcadePortRules.normalizeDragonsKit("Coward"));
        assertEquals("marksman", ArcadePortRules.normalizeDragonsKit("dragons-markman"));
        assertEquals("pyrotechnic", ArcadePortRules.normalizeDragonsKit("pyro"));
        assertEquals("frost", ArcadePortRules.normalizeDragonsKit("Ice Bridge"));
        assertEquals("glider", ArcadePortRules.normalizeDragonsKit("Glider"));
        assertEquals("tamer", ArcadePortRules.normalizeDragonsKit("Dragon Tamer"));
        assertNull(ArcadePortRules.normalizeDragonsKit("flare"));
    }

    @Test void requestedCooldownsAndDurationsAreExact() {
        assertEquals(25_000L, ArcadePortRules.DRAGONS_FROST_COOLDOWN_MILLIS);
        assertEquals(15_000L, ArcadePortRules.DRAGONS_GLIDER_COOLDOWN_MILLIS);
        assertEquals(3_500L, ArcadePortRules.DRAGONS_GLIDER_DURATION_MILLIS);
        assertEquals(90_000L, ArcadePortRules.DRAGONS_TAMER_COOLDOWN_MILLIS);
        assertEquals(15_000L, ArcadePortRules.DRAGONS_TAMER_DURATION_MILLIS);
    }

    @Test void survivalAchievementStartsAtExactlyFiveLiveMinutes() {
        assertFalse(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(299_999L));
        assertTrue(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(300_000L));
        assertTrue(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(300_001L));
    }

    @Test void threeWinStreakIncrementsAndAnyCompletedLossResets() {
        assertEquals(1, ArcadePortRules.nextDragonsWinStreak(0, true));
        assertEquals(2, ArcadePortRules.nextDragonsWinStreak(1, true));
        assertEquals(3, ArcadePortRules.nextDragonsWinStreak(2, true));
        assertEquals(0, ArcadePortRules.nextDragonsWinStreak(2, false));
        assertTrue(ArcadePortRules.shouldAwardDragonsWinStreak(3));
        assertFalse(ArcadePortRules.shouldAwardDragonsWinStreak(2));
    }

    @Test void noDamageAchievementRequiresWinningWithoutAnyDamage() {
        assertTrue(ArcadePortRules.shouldAwardDragonsNoDamageWin(true, false));
        assertFalse(ArcadePortRules.shouldAwardDragonsNoDamageWin(true, true));
        assertFalse(ArcadePortRules.shouldAwardDragonsNoDamageWin(false, false));
    }
}
