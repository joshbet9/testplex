package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DragonEscapeDebugRulesTest {
    @AfterEach void clearOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void scoreCadenceAndDisruptorRechargeUseProfileValuesAndReset() {
        assertEquals(2_000L, ArcadePortRules.nextDragonEscapeScoreUpdateAt(1_500L));
        assertEquals(8_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(0L));

        GameDebugRuntimeOverrides.activate(Map.of(
                "DRAGON_ESCAPE_SCORE_UPDATE_MILLIS", 125L,
                "DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS", 2_000L));
        assertEquals(1_625L, ArcadePortRules.nextDragonEscapeScoreUpdateAt(1_500L));
        assertEquals(2_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(0L));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(2_000L, ArcadePortRules.nextDragonEscapeScoreUpdateAt(1_500L));
        assertEquals(8_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(0L));
    }

    @Test void dragonEscapeControlsHavePracticalRanges() {
        assertVariable("DRAGON_ESCAPE_SCORE_UPDATE_MILLIS", 1.0D, 120_000.0D);
        assertVariable("DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS", 0.0D, 120_000.0D);
        assertVariable("DRAGON_ESCAPE_CHASE_SPEED", 0.0D, 2.0D);
        assertVariable("DRAGON_ESCAPE_FINISH_RADIUS", 0.0D, 32.0D);
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.DRAGON_ESCAPE, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
    }
}
