package me.monstermaze.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameManagerCompletionTaskTest {
    @Test
    void scheduledCompletionOnlyAppliesToItsOwnEndingRound() {
        assertTrue(GameManager.shouldCompleteScheduledRound(GameState.ENDING, 7L, 7L));
        assertFalse(GameManager.shouldCompleteScheduledRound(GameState.ENDING, 8L, 7L));
        assertFalse(GameManager.shouldCompleteScheduledRound(GameState.STARTING, 7L, 7L));
        assertFalse(GameManager.shouldCompleteScheduledRound(GameState.LIVE, 7L, 7L));
        assertFalse(GameManager.shouldCompleteScheduledRound(GameState.IDLE, 7L, 7L));
    }
}
