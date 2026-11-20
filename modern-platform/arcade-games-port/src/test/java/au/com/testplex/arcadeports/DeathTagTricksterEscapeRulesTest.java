package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeathTagTricksterEscapeRulesTest {
    @Test void selectedBucketClutchesWithoutLastMomentRightClick() {
        assertTrue(DeathTagTricksterEscapeRules.shouldClutch(10_000L, 0L, 0L, true));
        assertTrue(DeathTagTricksterEscapeRules.shouldClutch(10_000L, 0L, 12_000L, false));
        assertFalse(DeathTagTricksterEscapeRules.shouldClutch(12_000L, 0L, 12_000L, false));
    }
    @Test void cooldownPreventsRepeatedClutchesEvenWhileHolding() {
        assertFalse(DeathTagTricksterEscapeRules.shouldClutch(9_999L, 10_000L, 100_000L, true));
        assertTrue(DeathTagTricksterEscapeRules.shouldClutch(10_000L, 10_000L, 0L, true));
    }
    @Test void temporaryWebRequiresFreeFootSpaceAndSupportedGround() {
        assertTrue(DeathTagTricksterEscapeRules.canPlaceWeb(true, true));
        assertFalse(DeathTagTricksterEscapeRules.canPlaceWeb(false, true));
        assertFalse(DeathTagTricksterEscapeRules.canPlaceWeb(true, false));
    }
}
