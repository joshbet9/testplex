package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeMilestoneNextTest {
    @Test void nextUncompletedTierIncludesRemainingDistance() {
        var next = ArcadeMilestoneNext.of(183, new long[]{25, 63, 125, 250});
        assertEquals(3, next.earnedTiers());
        assertEquals(250, next.nextTarget());
        assertEquals(67, next.remaining());
        assertEquals(73.2, next.percent(), 0.001);
        assertFalse(next.complete());
    }

    @Test void completedTracksAndEmptyTracksAreSafe() {
        assertTrue(ArcadeMilestoneNext.of(250, new long[]{25, 63, 125, 250}).complete());
        assertEquals(100.0, ArcadeMilestoneNext.of(0, new long[0]).percent());
    }
}
