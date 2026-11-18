package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaconBrawlCombatRulesTest {
    @Test
    void successfulHitRestoresSourceDamageAmountToReceiver() {
        assertEquals(18.0D, BaconBrawlCombatRules.healthAfterHit(12.0D, 20.0D, 6.0D), 0.00001D);
    }

    @Test
    void successfulHitNeverHealsAboveMaximumHealth() {
        assertEquals(20.0D, BaconBrawlCombatRules.healthAfterHit(19.0D, 20.0D, 6.0D), 0.00001D);
    }

    @Test
    void invalidOrNegativeDamageDoesNotReduceOrOverhealHealth() {
        assertEquals(12.0D, BaconBrawlCombatRules.healthAfterHit(12.0D, 20.0D, -2.0D), 0.00001D);
        assertEquals(12.0D, BaconBrawlCombatRules.healthAfterHit(12.0D, 20.0D, Double.NaN), 0.00001D);
    }
}
