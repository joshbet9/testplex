package au.com.testplex.snake;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SnakeAchievementTrackerTest {
    @Test void tailwindRequiresThreeDistinctKillsDuringSpeedBoost() {
        SnakeAchievementTracker tracker = new SnakeAchievementTracker("speed");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        tracker.recordKill(first, false, false);
        tracker.recordKill(first, true, false);
        tracker.recordKill(second, true, false);
        assertFalse(tracker.earnedTailwind());
        tracker.recordKill(UUID.randomUUID(), true, false);
        assertTrue(tracker.earnedTailwind());
    }

    @Test void oppositeDayRequiresTwoDistinctKillsWithinOneReverseActivation() {
        SnakeAchievementTracker tracker = new SnakeAchievementTracker("reverse");
        UUID first = UUID.randomUUID();
        tracker.recordKill(first, false, true);
        tracker.recordKill(first, false, true);
        assertFalse(tracker.earnedOppositeDay());
        tracker.recordKill(UUID.randomUUID(), false, true);
        assertTrue(tracker.earnedOppositeDay());
        tracker.beginReverseActivation();
        assertTrue(tracker.earnedOppositeDay());
    }

    @Test void invulnerabilityRequiresThreeDistinctTailSegments() {
        SnakeAchievementTracker tracker = new SnakeAchievementTracker("invulnerable");
        UUID segment = UUID.randomUUID();
        tracker.recordInvulnerableTailCollision(segment);
        tracker.recordInvulnerableTailCollision(segment);
        tracker.recordInvulnerableTailCollision(UUID.randomUUID());
        assertFalse(tracker.earnedUntouchasnake());
        tracker.recordInvulnerableTailCollision(UUID.randomUUID());
        assertTrue(tracker.earnedUntouchasnake());
    }

    @Test void luckyCoilNeedsFourDifferentRouletteEffects() {
        SnakeAchievementTracker tracker = new SnakeAchievementTracker("speed");
        tracker.recordRouletteEffect("DRIFT");
        tracker.recordRouletteEffect("DRIFT");
        tracker.recordRouletteEffect("TURN_LAG");
        tracker.recordRouletteEffect("OVERSHOOT");
        assertFalse(tracker.earnedLuckyCoil());
        tracker.recordRouletteEffect("WEAK_SHARP_TURN");
        assertTrue(tracker.earnedLuckyCoil());
    }

    @Test void lastCoilStandingFailsOnEitherSlimeOrMagmaCubeContact() {
        SnakeAchievementTracker clean = new SnakeAchievementTracker("speed");
        assertTrue(clean.earnedLastCoilStanding());
        clean.recordSlimeCollected();
        assertFalse(clean.earnedLastCoilStanding());

        SnakeAchievementTracker hazard = new SnakeAchievementTracker("reverse");
        hazard.recordMagmaCubeContact();
        assertFalse(hazard.earnedLastCoilStanding());
    }
}
