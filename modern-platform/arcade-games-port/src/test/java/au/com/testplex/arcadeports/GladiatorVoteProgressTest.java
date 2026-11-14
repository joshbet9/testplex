package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GladiatorVoteProgressTest {
    @Test
    void exposesElapsedAndRemainingSegmentsAndCeilingSeconds() {
        GladiatorVoteProgress.View view = GladiatorVoteProgress.at(5_000L, 0L, 10_000L);

        assertEquals(12, view.greenSegments());
        assertEquals(12, view.redSegments());
        assertEquals(5L, view.secondsLeft());
        assertEquals(50L, view.remainingTenths());
    }

    @Test
    void clampsBeforeAndAfterTheVoteWindow() {
        GladiatorVoteProgress.View before = GladiatorVoteProgress.at(-1_000L, 0L, 10_000L);
        assertEquals(0, before.greenSegments());
        assertEquals(24, before.redSegments());
        assertEquals(10L, before.secondsLeft());

        GladiatorVoteProgress.View after = GladiatorVoteProgress.at(11_000L, 0L, 10_000L);
        assertEquals(24, after.greenSegments());
        assertEquals(0, after.redSegments());
        assertEquals(0L, after.secondsLeft());
    }
}
