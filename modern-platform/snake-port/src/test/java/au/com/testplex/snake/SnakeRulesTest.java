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

    @Test void lengthMakesTheSnakeFasterAndSpeedKitAddsFiftyPercent() {
        double shortSnake = SnakeRules.movementSpeed(1, false);
        double longSnake = SnakeRules.movementSpeed(180, false);
        assertTrue(longSnake > shortSnake);
        assertEquals(shortSnake * 1.5D, SnakeRules.movementSpeed(1, true), 0.000001D);
    }

    @Test void driftTurnRateIsBounded() {
        assertEquals(Math.toRadians(8.0D), SnakeRules.clampTurnRadians(1.2D), 0.000001D);
        assertEquals(-Math.toRadians(8.0D), SnakeRules.clampTurnRadians(-1.2D), 0.000001D);
    }

    @Test void yawAndRadianWrappingKeepAnglesInExpectedRanges() {
        assertEquals(-170.0F, SnakeRules.wrapYaw(190.0F), 0.0001F);
        assertEquals(Math.PI / 2.0D, SnakeRules.wrapRadians(5.0D * Math.PI / 2.0D), 0.000001D);
    }
}
