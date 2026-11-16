package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathTagTurtleChargeProgressTest {
    @Test
    void everyThreeSuccessfulHitsAwardsOneChargeAndResetsProgress() {
        assertEquals(new DeathTagTurtleChargeProgress.HitResult(1, false),
                DeathTagTurtleChargeProgress.recordHit(0, 3, 0));
        assertEquals(new DeathTagTurtleChargeProgress.HitResult(2, false),
                DeathTagTurtleChargeProgress.recordHit(1, 3, 0));
        assertEquals(new DeathTagTurtleChargeProgress.HitResult(0, true),
                DeathTagTurtleChargeProgress.recordHit(2, 3, 0));
    }

    @Test
    void holdingThreeChargesStopsAdditionalAwardsAndProgress() {
        DeathTagTurtleChargeProgress.HitResult hit =
                DeathTagTurtleChargeProgress.recordHit(2, 3, 3);
        assertFalse(hit.awardsCharge());
        assertEquals(0, hit.hitProgress());
        assertEquals(0.0F,
                DeathTagTurtleChargeProgress.experienceProgress(2, 3, 3), 0.0001F);
    }

    @Test
    void experienceBarShowsFractionalProgressToTheNextCharge() {
        assertEquals(2.0F / 3.0F,
                DeathTagTurtleChargeProgress.experienceProgress(2, 3, 1), 0.0001F);
        assertTrue(DeathTagTurtleChargeProgress.recordHit(2, 3, 2).awardsCharge());
    }
}
