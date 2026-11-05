package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MicroBattlesDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void openingIslandWindowCanBeAdjustedAndClearingRestoresTheSourceWindow() {
        assertTrue(ArcadePortRules.shouldPunishMicroIslandExit(3_000L, 100.0D, 25.0D));
        GameDebugRuntimeOverrides.activate(Map.of("MICRO_BATTLES_CHEAT_WINDOW_MILLIS", 1_000D));
        assertFalse(ArcadePortRules.shouldPunishMicroIslandExit(3_000L, 100.0D, 25.0D,
                GameDebugRuntimeOverrides.longValue("MICRO_BATTLES_CHEAT_WINDOW_MILLIS",
                        ArcadePortRules.MICRO_BATTLES_CHEAT_WINDOW_MILLIS)));
        GameDebugRuntimeOverrides.clear();
        assertTrue(ArcadePortRules.shouldPunishMicroIslandExit(3_000L, 100.0D, 25.0D,
                GameDebugRuntimeOverrides.longValue("MICRO_BATTLES_CHEAT_WINDOW_MILLIS",
                        ArcadePortRules.MICRO_BATTLES_CHEAT_WINDOW_MILLIS)));
    }

    @Test
    void borderDistanceAndAnnihilationThresholdChangeTheirGameplayPredicates() {
        assertTrue(ArcadePortRules.shouldShowMicroBattlesBorderParticles(47.0D, 0.0D,
                0.0D, 0.0D, 100.0D));
        assertFalse(ArcadePortRules.shouldShowMicroBattlesBorderParticles(47.0D, 0.0D,
                0.0D, 0.0D, 100.0D, 2.0D));
        assertTrue(ArcadePortRules.shouldAwardMicroBattlesAnnihilation(5, 5));
        assertFalse(ArcadePortRules.shouldAwardMicroBattlesAnnihilation(5, 8));
    }

    @Test
    void contributionExpiryAndBorderVerticalPointRangesAreExposed() {
        assertTrue(ArcadePortRules.isMicroBattlesCombatContributionFresh(10_000L, 0L));
        assertFalse(ArcadePortRules.isMicroBattlesCombatContributionFresh(10_000L, 0L, 5_000L));
        assertEquals(1.0D, GameDebugVariableCatalog.variable(ArcadePortGame.MICRO_BATTLES,
                "MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS").minimum());
        assertEquals(8.0D, GameDebugVariableCatalog.variable(ArcadePortGame.MICRO_BATTLES,
                "MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS").maximum());
    }
}
