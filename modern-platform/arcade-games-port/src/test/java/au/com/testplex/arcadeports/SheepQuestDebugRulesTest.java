package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SheepQuestDebugRulesTest {
    @AfterEach void clearOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void jumpThrowAndCaptureProfilesChangeProductionRulesThenReset() {
        assertEquals(0.9D, ArcadePortRules.sheepQuestBerserkerHorizontalSpeed());
        assertEquals(0.65D, ArcadePortRules.sheepQuestBerserkerVerticalSpeed(-0.2D));
        assertEquals(1.4D, ArcadePortRules.sheepQuestBruteThrowHorizontalSpeed());
        assertEquals(0.3D, ArcadePortRules.sheepQuestBruteThrowVerticalSpeed(-0.5D));
        assertEquals(20.25D, ArcadePortRules.sheepQuestCaptureRadiusSquared());
        assertEquals(1.44D, ArcadePortRules.sheepQuestThrowHitRadiusSquared());
        assertTrue(ArcadePortRules.canCarryAnotherSheep(2));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("SHEEP_QUEST_BESERKER_LEAP_HORIZONTAL_SPEED", 1.6D),
                Map.entry("SHEEP_QUEST_BESERKER_LEAP_VERTICAL_MIN", 0.4D),
                Map.entry("SHEEP_QUEST_BESERKER_LEAP_VERTICAL_MAX", 1.8D),
                Map.entry("SHEEP_QUEST_BESERKER_LEAP_VERTICAL_ADD", 0.6D),
                Map.entry("SHEEP_QUEST_BRUTE_THROW_SPEED", 2.2D),
                Map.entry("SHEEP_QUEST_BRUTE_THROW_VERTICAL_MIN", 0.5D),
                Map.entry("SHEEP_QUEST_BRUTE_THROW_VERTICAL_MAX", 1.4D),
                Map.entry("SHEEP_QUEST_BRUTE_THROW_VERTICAL_PITCH_SCALE", 0.5D),
                Map.entry("SHEEP_QUEST_MAX_CARRY", 2),
                Map.entry("SHEEP_QUEST_CAPTURE_RADIUS_BLOCKS", 7.0D),
                Map.entry("SHEEP_QUEST_THROW_HIT_RADIUS", 2.0D)));
        assertEquals(1.6D, ArcadePortRules.sheepQuestBerserkerHorizontalSpeed());
        assertEquals(0.8D, ArcadePortRules.sheepQuestBerserkerVerticalSpeed(0.2D));
        assertEquals(2.2D, ArcadePortRules.sheepQuestBruteThrowHorizontalSpeed());
        assertEquals(0.5D, ArcadePortRules.sheepQuestBruteThrowVerticalSpeed(0.0D));
        assertEquals(49.0D, ArcadePortRules.sheepQuestCaptureRadiusSquared());
        assertEquals(4.0D, ArcadePortRules.sheepQuestThrowHitRadiusSquared());
        assertFalse(ArcadePortRules.canCarryAnotherSheep(2));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(0.9D, ArcadePortRules.sheepQuestBerserkerHorizontalSpeed());
        assertEquals(0.65D, ArcadePortRules.sheepQuestBerserkerVerticalSpeed(-0.2D));
        assertEquals(20.25D, ArcadePortRules.sheepQuestCaptureRadiusSquared());
        assertTrue(ArcadePortRules.canCarryAnotherSheep(2));
    }

    @Test void sheepQuestControlsAreExposedWithBoundedValues() {
        assertVariable("SHEEP_QUEST_BESERKER_LEAP_HORIZONTAL_SPEED", 0.0D, 4.0D);
        assertVariable("SHEEP_QUEST_BESERKER_JUMP_COOLDOWN_MILLIS", 0.0D, 120_000.0D);
        assertVariable("SHEEP_QUEST_BRUTE_THROW_SPEED", 0.0D, 4.0D);
        assertVariable("SHEEP_QUEST_CAPTURE_RADIUS_BLOCKS", 0.0D, 64.0D);
        assertVariable("SHEEP_QUEST_MAX_CARRY", 0.0D, 64.0D);
        assertVariable("SHEEP_QUEST_THROW_BOUNCE_MULTIPLIER", -1.0D, 1.0D);
        assertVariable("SHEEP_QUEST_HOME_SPEED", 0.0D, 4.0D);
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.SHEEP_QUEST, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
    }
}
