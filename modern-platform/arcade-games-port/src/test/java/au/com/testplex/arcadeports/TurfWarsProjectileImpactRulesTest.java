package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurfWarsProjectileImpactRulesTest {
    @Test
    void fallsBackForReportedPlayerHitsFromPlayerProjectiles() {
        // Human players and test bots both arrive here as Bukkit Players; there is no bot-only
        // branch that can cause their arrows to receive different hit behavior.
        assertTrue(TurfWarsProjectileImpactRules.shouldApplyFallback(true, true));
    }

    @Test
    void doesNotFallbackForBlockOrUnownedProjectileImpacts() {
        assertFalse(TurfWarsProjectileImpactRules.shouldApplyFallback(false, true));
        assertFalse(TurfWarsProjectileImpactRules.shouldApplyFallback(true, false));
    }
}
