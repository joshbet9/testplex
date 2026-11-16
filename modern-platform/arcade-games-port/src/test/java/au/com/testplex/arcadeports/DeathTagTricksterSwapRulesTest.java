package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathTagTricksterSwapRulesTest {
    @Test
    void swapCanBeUsedAgainAfterThirtySecondsAndNotBefore() {
        long usedAt = 100_000L;
        long readyAt = usedAt + DeathTagTricksterSwapRules.COOLDOWN_MILLIS;

        assertTrue(DeathTagTricksterSwapRules.isReady(usedAt, usedAt));
        assertFalse(DeathTagTricksterSwapRules.isReady(readyAt - 1L, readyAt));
        assertTrue(DeathTagTricksterSwapRules.isReady(readyAt, readyAt));
    }
}
