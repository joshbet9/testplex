package me.monstermaze.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** No longer stop the Safe Pad visual guidance after stage two. */
class GameManagerPadFireworksTest {
    @Test
    void fireworksContinueBeyondTheFirstTwoSafePads() {
        for (int stage : new int[] {1, 2, 3, 4, 5, 8, 15, 25}) {
            assertTrue(GameManager.shouldLaunchPadFirework(stage, true), "stage " + stage);
        }
    }

    @Test
    void absentPadOrNotStartedNeverSpawnsFireworks() {
        assertFalse(GameManager.shouldLaunchPadFirework(1, false));
        assertFalse(GameManager.shouldLaunchPadFirework(9, false));
        assertFalse(GameManager.shouldLaunchPadFirework(0, true));
    }
}
