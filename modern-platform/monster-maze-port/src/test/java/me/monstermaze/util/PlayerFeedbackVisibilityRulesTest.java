package me.monstermaze.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerFeedbackVisibilityRulesTest {
    @Test void onlySendEntityVisibilityWhenViewerStateChanges() {
        assertTrue(PlayerFeedback.shouldUpdateVisibility(true, false));
        assertTrue(PlayerFeedback.shouldUpdateVisibility(false, true));
        assertFalse(PlayerFeedback.shouldUpdateVisibility(true, true));
        assertFalse(PlayerFeedback.shouldUpdateVisibility(false, false));
    }
}
