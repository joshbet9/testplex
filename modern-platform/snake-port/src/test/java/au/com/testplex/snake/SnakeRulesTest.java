package au.com.testplex.snake;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SnakeRulesTest {
    @Test void rouletteDurationsEscalateAndCap() {
        assertEquals(1, SnakeRules.rouletteDurationSecondsForHit(1));
        assertEquals(3, SnakeRules.rouletteDurationSecondsForHit(2));
        assertEquals(5, SnakeRules.rouletteDurationSecondsForHit(3));
        assertEquals(10, SnakeRules.rouletteDurationSecondsForHit(4));
        assertEquals(30, SnakeRules.rouletteDurationSecondsForHit(99));
    }

    @Test void sourceSpeedCurveSaturatesAt126Segments() {
        assertTrue(SnakeRules.movementSpeed(126, false) > SnakeRules.movementSpeed(1, false));
        assertEquals(SnakeRules.movementSpeed(126, false), SnakeRules.movementSpeed(180, false), 0.000001D);
        assertEquals(SnakeRules.movementSpeed(126, false), SnakeRules.movementSpeed(1000, false), 0.000001D);
        assertEquals(SnakeRules.movementSpeed(1, false) * 1.5D,
                SnakeRules.movementSpeed(1, true), 0.000001D);
        assertEquals(180, SnakeRules.SPEED_REFERENCE_LENGTH);
    }

    @Test void snakeColorsFollowParticipantOrderAndWrap() {
        assertEquals(0, SnakeRules.colorIndexForParticipant(0, 16));
        assertEquals(15, SnakeRules.colorIndexForParticipant(15, 16));
        assertEquals(0, SnakeRules.colorIndexForParticipant(16, 16));
        assertEquals(15, SnakeRules.colorIndexForParticipant(-1, 16));
        assertEquals(0, SnakeRules.colorIndexForParticipant(8, 0));
    }

    @Test void foodAndTailCollisionsUseSourceThreeDimensionalRadii() {
        assertTrue(SnakeRules.isFoodCollected(3.999D));
        assertFalse(SnakeRules.isFoodCollected(4.0D));
        assertFalse(SnakeRules.isFoodCollected(Double.NaN));
        assertTrue(SnakeRules.isTailCollision(1.439D));
        assertFalse(SnakeRules.isTailCollision(1.44D));
        assertFalse(SnakeRules.isTailCollision(Double.POSITIVE_INFINITY));
        assertEquals(2.0D, SnakeRules.FOOD_PICKUP_RADIUS, 0.000001D);
        assertEquals(1.2D, SnakeRules.SNAKE_TAIL_COLLISION_RADIUS, 0.000001D);
    }

    @Test void aNewTailSegmentFollowsTheExistingTailDirection() {
        org.bukkit.util.Vector previous = new org.bukkit.util.Vector(0.0D, 64.0D, 0.0D);
        org.bukkit.util.Vector last = new org.bukkit.util.Vector(1.0D, 64.0D, 0.0D);
        org.bukkit.util.Vector facing = new org.bukkit.util.Vector(0.0D, 0.0D, 1.0D);
        org.bukkit.util.Vector direction = SnakeRules.tailExtensionDirection(previous, last, facing);
        assertEquals(1.0D, direction.getX(), 0.000001D);
        assertEquals(0.0D, direction.getY(), 0.000001D);
        assertEquals(0.0D, direction.getZ(), 0.000001D);

        org.bukkit.util.Vector fallback = SnakeRules.tailExtensionDirection(null, null, facing);
        assertEquals(0.0D, fallback.getX(), 0.000001D);
        assertEquals(0.0D, fallback.getY(), 0.000001D);
        assertEquals(-1.0D, fallback.getZ(), 0.000001D);
    }

    @Test void reverserSeedsPathInTheSameOrderAsTheFlippedSegments() {
        java.util.List<org.bukkit.Location> segments = java.util.List.of(
                new org.bukkit.Location(null, 8.0D, 64.0D, 2.0D),
                new org.bukkit.Location(null, 7.0D, 64.0D, 2.0D),
                new org.bukkit.Location(null, 6.0D, 64.0D, 2.0D));
        java.util.List<org.bukkit.Location> path = SnakeRules.seedReversedBodyPath(segments);
        assertEquals(11, path.size());
        assertEquals(8.0D, path.get(0).getX(), 0.000001D);
        assertEquals(7.0D, path.get(5).getX(), 0.000001D);
        assertEquals(6.0D, path.get(10).getX(), 0.000001D);
        for (int i = 1; i < path.size(); i++) {
            assertTrue(path.get(i - 1).distance(path.get(i)) <= 0.200001D);
        }
        segments.get(0).setX(99.0D);
        assertEquals(8.0D, path.get(0).getX(), 0.000001D);
        assertTrue(SnakeRules.seedReversedBodyPath(null).isEmpty());
    }

    @Test void driftTurnRateIsBounded() {
        assertEquals(Math.toRadians(8.0D), SnakeRules.clampTurnRadians(1.2D), 0.000001D);
        assertEquals(-Math.toRadians(8.0D), SnakeRules.clampTurnRadians(-1.2D), 0.000001D);
    }

    @Test void itemBoxesMatchSourceTimingAndWaitForActiveRouletteToFinish() {
        assertEquals(4, SnakeRules.MAX_HAZARDS);
        assertEquals(10_000L, SnakeRules.FIRST_HAZARD_DELAY_MILLIS);
        assertEquals(15_000L, SnakeRules.HAZARD_INTERVAL_MILLIS);
        assertEquals(4, SnakeRules.HAZARD_STEERING_INTERVAL_TICKS);
        assertEquals(0.12D, SnakeRules.HAZARD_STEERING_SPEED, 0.000001D);
        assertEquals(2.1D, SnakeRules.HAZARD_CONTACT_RADIUS, 0.000001D);
        assertTrue(SnakeRules.shouldConsumeHazardOnContact(false));
        assertFalse(SnakeRules.shouldConsumeHazardOnContact(true));
    }

    @Test void yawAndRadianWrappingKeepAnglesInExpectedRanges() {
        assertEquals(-170.0F, SnakeRules.wrapYaw(190.0F), 0.0001F);
        assertEquals(Math.PI / 2.0D, SnakeRules.wrapRadians(5.0D * Math.PI / 2.0D), 0.000001D);
    }
}
