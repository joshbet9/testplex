package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GladiatorsScoreboardStatusTest {
    @Test
    void activePoisonTakesPrecedenceOverAnotherDuelStarting() {
        assertEquals("Active", GladiatorsScoreboardStatus.display(
                false, 0L, 2, 10_000L, 10_500L, 9_000L));
    }

    @Test
    void nextDuelCountdownIsShownUntilPoisonStarts() {
        assertEquals("Starting in 1s", GladiatorsScoreboardStatus.display(
                false, 0L, 2, 10_000L, 10_500L, 12_000L));
        assertEquals("Poison in 2s", GladiatorsScoreboardStatus.display(
                false, 0L, 1, 10_000L, 9_000L, 12_000L));
    }

    @Test
    void waitingAndVotingRemainUnchanged() {
        assertEquals("Waiting", GladiatorsScoreboardStatus.display(
                false, 0L, 0, 10_000L, Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals("Voting 1 second left", GladiatorsScoreboardStatus.display(
                true, 1L, 0, 10_000L, Long.MAX_VALUE, Long.MAX_VALUE));
    }
}
