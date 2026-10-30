package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WitherAssaultMovementRulesTest {
    @Test void spawnHeightUsesTheOriginalHoverSpeed() {
        assertEquals(WitherAssaultMovementRules.SPAWN_HOVER_SPEED,
                WitherAssaultMovementRules.targetFlySpeed(76.0D, 76.0D, 76.0D), 0.000001D);
    }

    @Test void transitionIsContinuousAndEasedAcrossTheSpawnBand() {
        double atStart = WitherAssaultMovementRules.targetFlySpeed(78.0001D, 76.0D, 76.0D);
        double justAfterStart = WitherAssaultMovementRules.targetFlySpeed(78.01D, 76.0D, 76.0D);
        double beforeEnd = WitherAssaultMovementRules.targetFlySpeed(85.99D, 76.0D, 76.0D);
        double atEnd = WitherAssaultMovementRules.targetFlySpeed(86.0D, 76.0D, 76.0D);
        assertTrue(Math.abs(justAfterStart - atStart) < 0.0001D);
        assertTrue(Math.abs(atEnd - beforeEnd) < 0.0001D);
    }

    @Test void altitudeCurveIsMonotonicAboveSpawnAndAlwaysBounded() {
        double previous = WitherAssaultMovementRules.targetFlySpeed(76.0D, 76.0D, 76.0D);
        for (double y = 77.0D; y <= 300.0D; y += 1.0D) {
            double speed = WitherAssaultMovementRules.targetFlySpeed(y, 76.0D, 76.0D);
            assertTrue(speed >= previous - 0.000001D, "speed decreased at Y=" + y);
            assertTrue(speed >= WitherAssaultMovementRules.MIN_FLY_SPEED);
            assertTrue(speed <= WitherAssaultMovementRules.MAX_FLY_SPEED);
            previous = speed;
        }
        assertEquals(WitherAssaultMovementRules.MIN_FLY_SPEED,
                WitherAssaultMovementRules.targetFlySpeed(-1_000.0D, 76.0D, 76.0D), 0.000001D);
        assertEquals(WitherAssaultMovementRules.MAX_FLY_SPEED,
                WitherAssaultMovementRules.targetFlySpeed(10_000.0D, 76.0D, 76.0D), 0.000001D);
    }

    @Test void liveSpeedApproachesTargetAtALimitedRate() {
        double current = 0.016D;
        double next = WitherAssaultMovementRules.approachSpeed(current, 0.8D);
        assertEquals(WitherAssaultMovementRules.MAX_SPEED_CHANGE_PER_TICK,
                next - current, 0.000001D);
        assertEquals(WitherAssaultMovementRules.MIN_FLY_SPEED,
                WitherAssaultMovementRules.approachSpeed(0.01D, 0.0D), 0.000001D);
        assertEquals(WitherAssaultMovementRules.MAX_FLY_SPEED,
                WitherAssaultMovementRules.approachSpeed(1.0D, 1.0D), 0.000001D);
    }
}
