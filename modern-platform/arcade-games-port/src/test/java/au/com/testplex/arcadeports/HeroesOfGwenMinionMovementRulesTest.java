package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroesOfGwenMinionMovementRulesTest {
    @Test
    void waypointToleranceHasNoDeadZoneBetweenAdvancingAndMoving() {
        for (double distanceSquared : new double[] {4.01D, 6.25D, 8.99D}) {
            assertFalse(HeroesOfGwenMinionMovementRules.shouldAdvanceWaypoint(distanceSquared));
            assertTrue(HeroesOfGwenMinionMovementRules.shouldMoveTowardWaypoint(distanceSquared));
        }
        assertTrue(HeroesOfGwenMinionMovementRules.shouldAdvanceWaypoint(3.99D));
        assertFalse(HeroesOfGwenMinionMovementRules.shouldMoveTowardWaypoint(3.99D));
    }

    @Test
    void minionMovesTowardAnIntermediateSourceWaypointTwoAndAHalfBlocksAway() {
        double distanceSquared = 2.5D * 2.5D;

        assertFalse(HeroesOfGwenMinionMovementRules.shouldAdvanceWaypoint(distanceSquared));
        assertTrue(HeroesOfGwenMinionMovementRules.shouldMoveTowardWaypoint(distanceSquared));
    }

    @Test
    void towerStepHitAcceptsMapSlabsNearTheMarkerAndRejectsUnrelatedBlocks() {
        assertTrue(HeroesOfGwenTowerInteractionRules.isStepHitBlock(0.75D));
        assertTrue(HeroesOfGwenTowerInteractionRules.isStepHitBlock(16.0D));
        assertFalse(HeroesOfGwenTowerInteractionRules.isStepHitBlock(16.01D));
        assertFalse(HeroesOfGwenTowerInteractionRules.isStepHitBlock(Double.NaN));
    }
}
