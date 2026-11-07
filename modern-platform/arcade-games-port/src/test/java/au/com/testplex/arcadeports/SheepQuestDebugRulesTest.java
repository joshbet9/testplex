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
        assertVariable("SHEEP_QUEST_SHEPHERD_SLOWNESS_SHEEP_PER_LEVEL", 1.0D, 64.0D);
        assertVariable("SHEEP_QUEST_SHEPHERD_SLOWNESS_MAX_AMPLIFIER", 0.0D, 127.0D);
        assertVariable("SHEEP_QUEST_CAPTURE_SCORE", 0.0D, 100.0D);
        assertVariable("SHEEP_QUEST_ARCHER_ACHIEVEMENT_KILLS_PER_TARGET", 0.0D, 10_000.0D);
    }

    @Test void shepherdStackSlowdownAndCaptureScoringAreProfileBackedAndResettable() {
        assertEquals(-1, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(0));
        assertEquals(0, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(1));
        assertEquals(0, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(2));
        assertEquals(1, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(3));
        assertEquals(1, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(4));
        assertEquals(2, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(5));
        assertEquals(1, ArcadePortRules.sheepQuestCaptureScore());

        GameDebugRuntimeOverrides.activate(Map.of(
                "SHEEP_QUEST_SHEPHERD_SLOWNESS_SHEEP_PER_LEVEL", 1,
                "SHEEP_QUEST_SHEPHERD_SLOWNESS_MAX_AMPLIFIER", 2,
                "SHEEP_QUEST_CAPTURE_SCORE", 4));
        assertEquals(2, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(3));
        assertEquals(2, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(4));
        assertEquals(4, ArcadePortRules.sheepQuestCaptureScore());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(0, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(2));
        assertEquals(1, ArcadePortRules.sheepQuestShepherdSlownessAmplifier(3));
        assertEquals(1, ArcadePortRules.sheepQuestCaptureScore());
    }

    @Test void progressionAndAchievementConditionsUseProfileThresholdsAndResetToSourceValues() {
        assertFalse(ArcadePortRules.sheepQuestSoloCarryQualified(2));
        assertTrue(ArcadePortRules.sheepQuestSoloCarryQualified(3));
        assertFalse(ArcadePortRules.sheepQuestThiefAchievementReached(99));
        assertTrue(ArcadePortRules.sheepQuestThiefAchievementReached(100));
        assertTrue(ArcadePortRules.sheepQuestAnimalRescueAchievementReached(100));
        assertFalse(ArcadePortRules.sheepQuestHungryHungryHippoAchievementReached(19));
        assertTrue(ArcadePortRules.sheepQuestHungryHungryHippoAchievementReached(20));
        assertTrue(ArcadePortRules.sheepQuestSelfishAchievementReached(13));
        assertFalse(ArcadePortRules.sheepQuestSelfishAchievementReached(12));
        assertFalse(ArcadePortRules.sheepQuestArcherAchievementReached(4));
        assertTrue(ArcadePortRules.sheepQuestMovementCreditIsRecent(10_000L, 5_000L));
        assertFalse(ArcadePortRules.sheepQuestMovementCreditIsRecent(10_001L, 5_000L));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("SHEEP_QUEST_SOLO_CARRY_KILL_THRESHOLD", 1),
                Map.entry("SHEEP_QUEST_THIEF_ACHIEVEMENT_COUNT", 2),
                Map.entry("SHEEP_QUEST_ANIMAL_RESCUE_ACHIEVEMENT_COUNT", 3),
                Map.entry("SHEEP_QUEST_HUNGRY_HUNGRY_HIPPO_WINS", 4),
                Map.entry("SHEEP_QUEST_SELFISH_TEAM_SHEEP_THRESHOLD", 5),
                Map.entry("SHEEP_QUEST_ARCHER_ACHIEVEMENT_KILLS_PER_TARGET", 6),
                Map.entry("SHEEP_QUEST_ARCHER_MOVEMENT_CREDIT_WINDOW_MILLIS", 7)));
        assertTrue(ArcadePortRules.sheepQuestSoloCarryQualified(1));
        assertTrue(ArcadePortRules.sheepQuestThiefAchievementReached(2));
        assertTrue(ArcadePortRules.sheepQuestAnimalRescueAchievementReached(3));
        assertTrue(ArcadePortRules.sheepQuestHungryHungryHippoAchievementReached(4));
        assertTrue(ArcadePortRules.sheepQuestSelfishAchievementReached(6));
        assertTrue(ArcadePortRules.sheepQuestArcherAchievementReached(6));
        assertTrue(ArcadePortRules.sheepQuestMovementCreditIsRecent(17L, 10L));
        assertFalse(ArcadePortRules.sheepQuestMovementCreditIsRecent(18L, 10L));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertFalse(ArcadePortRules.sheepQuestSoloCarryQualified(2));
        assertFalse(ArcadePortRules.sheepQuestThiefAchievementReached(99));
        assertFalse(ArcadePortRules.sheepQuestArcherAchievementReached(4));
        assertFalse(ArcadePortRules.sheepQuestMovementCreditIsRecent(10_001L, 5_000L));
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.SHEEP_QUEST, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
    }
}
