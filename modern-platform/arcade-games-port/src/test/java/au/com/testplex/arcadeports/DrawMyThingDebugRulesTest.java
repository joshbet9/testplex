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
}
