package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CastleAssaultManagerTest {
    @Test
    void countdownRoundsUpAndClampsExpiredTimers() {
        assertEquals("0:00", CastleAssaultManager.formatCountdown(0));
        assertEquals("0:01", CastleAssaultManager.formatCountdown(1));
        assertEquals("1:01", CastleAssaultManager.formatCountdown(60_001));
        assertEquals("0:00", CastleAssaultManager.formatCountdown(-5_000));
    }

    @Test
    void scoreboardUpdateIsSafeBeforeCastlePreparation() {
        CastleAssaultManager castle = new CastleAssaultManager();
        assertFalse(castle.hasScoreboard());
        assertNull(castle.scoreboard());
        castle.updateScoreboard(100L, 500L, 800L, false, "Unavailable", "Unavailable");
    }
}
