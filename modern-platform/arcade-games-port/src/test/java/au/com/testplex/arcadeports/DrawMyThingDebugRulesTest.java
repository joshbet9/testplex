package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DrawMyThingDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void profileOverridesChangeGuessLimitsScoringAndComebackThreshold() {
        assertEquals(40_000L, ArcadePortRules.drawMyThingRoundMillis(4));
        assertEquals(40_000L, ArcadePortRules.drawMyThingRoundMillis(8));
        assertEquals(35_555L, ArcadePortRules.drawMyThingRoundMillis(9));
        assertEquals(20_000L, ArcadePortRules.drawMyThingRoundMillis(16));
        assertEquals(10, ArcadePortRules.drawMyThingMaxGuesses());
        assertEquals(3, ArcadePortRules.drawMyThingGuesserPoints(true));
        assertEquals(1, ArcadePortRules.drawMyThingGuesserPoints(false));
        assertEquals(2, ArcadePortRules.drawMyThingDrawerFirstGuessPoints());
        assertEquals(10, ArcadePortRules.drawMyThingComebackScoreGap());

        GameDebugRuntimeOverrides.activate(Map.of(
                "DRAW_MY_THING_MAX_GUESSES", 4D,
                "DRAW_MY_THING_FIRST_GUESS_POINTS", 6D,
                "DRAW_MY_THING_LATER_GUESS_POINTS", 2D,
                "DRAW_MY_THING_DRAWER_FIRST_GUESS_POINTS", 5D,
                "DRAW_MY_THING_COMEBACK_SCORE_GAP", 4D));
        assertEquals(4, ArcadePortRules.drawMyThingMaxGuesses());
        assertEquals(6, ArcadePortRules.drawMyThingGuesserPoints(true));
        assertEquals(2, ArcadePortRules.drawMyThingGuesserPoints(false));
        assertEquals(5, ArcadePortRules.drawMyThingDrawerFirstGuessPoints());
        assertEquals(4, ArcadePortRules.drawMyThingComebackScoreGap());

        GameDebugRuntimeOverrides.clear();
        assertEquals(10, ArcadePortRules.drawMyThingMaxGuesses());
        assertEquals(3, ArcadePortRules.drawMyThingGuesserPoints(true));
        assertEquals(1, ArcadePortRules.drawMyThingGuesserPoints(false));
        assertEquals(2, ArcadePortRules.drawMyThingDrawerFirstGuessPoints());
        assertEquals(10, ArcadePortRules.drawMyThingComebackScoreGap());
    }

    @Test
    void profileOverridesChangeHintsDrawingAndRoundFlowAndResetToDefaults() {
        assertEquals(1, ArcadePortRules.drawMyThingBrushRadiusBlocks());
        assertEquals(0L, ArcadePortRules.drawMyThingIntermissionMillis());
        assertEquals(0L, ArcadePortRules.drawMyThingGuessCooldownMillis());
        assertTrue(ArcadePortRules.drawMyThingEndAfterAllGuessed());
        assertEquals(4, ArcadePortRules.drawMyThingHintRevealCount(40_000L, 40_000L, 8));
        assertTrue(ArcadePortRules.drawMyThingCanSubmitGuess(10_000L, 10_000L));

        GameDebugRuntimeOverrides.activate(Map.of(
                "DRAW_MY_THING_BRUSH_RADIUS_BLOCKS", 3D,
                "DRAW_MY_THING_INTERMISSION_MILLIS", 2_000D,
                "DRAW_MY_THING_GUESS_COOLDOWN_MILLIS", 1_000D,
                "DRAW_MY_THING_HINT_REVEAL_DURATION_MULTIPLIER", 1D,
                "DRAW_MY_THING_MAX_HINTED_LETTER_FRACTION", 0.5D,
                "DRAW_MY_THING_END_AFTER_ALL_GUESSED", false));
        assertEquals(3, ArcadePortRules.drawMyThingBrushRadiusBlocks());
        assertEquals(2_000L, ArcadePortRules.drawMyThingIntermissionMillis());
        assertEquals(1_000L, ArcadePortRules.drawMyThingGuessCooldownMillis());
        assertFalse(ArcadePortRules.drawMyThingEndAfterAllGuessed());
        assertEquals(4, ArcadePortRules.drawMyThingHintRevealCount(40_000L, 40_000L, 8));
        assertFalse(ArcadePortRules.drawMyThingCanSubmitGuess(10_000L, 10_500L));
        assertTrue(ArcadePortRules.drawMyThingCanSubmitGuess(10_000L, 11_000L));

        GameDebugRuntimeOverrides.clear();
        assertEquals(1, ArcadePortRules.drawMyThingBrushRadiusBlocks());
        assertEquals(0L, ArcadePortRules.drawMyThingIntermissionMillis());
        assertEquals(0L, ArcadePortRules.drawMyThingGuessCooldownMillis());
        assertTrue(ArcadePortRules.drawMyThingEndAfterAllGuessed());
        assertEquals(4, ArcadePortRules.drawMyThingHintRevealCount(40_000L, 40_000L, 8));
    }

    @Test
    void addedDrawMyThingControlsHavePracticalRangesAndAreRegistered() {
        assertVariable("DRAW_MY_THING_BRUSH_RADIUS_BLOCKS", 0D, 4D);
        assertVariable("DRAW_MY_THING_GUESS_COOLDOWN_MILLIS", 0D, 120_000D);
        assertVariable("DRAW_MY_THING_HINT_REVEAL_DURATION_MULTIPLIER", 0.1D, 10D);
        assertVariable("DRAW_MY_THING_MAX_HINTED_LETTER_FRACTION", 0D, 1D);
        assertVariable("DRAW_MY_THING_INTERMISSION_MILLIS", 0D, 120_000D);
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.DRAW_MY_THING,
                "DRAW_MY_THING_END_AFTER_ALL_GUESSED"));
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.DRAW_MY_THING, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
    }
}
