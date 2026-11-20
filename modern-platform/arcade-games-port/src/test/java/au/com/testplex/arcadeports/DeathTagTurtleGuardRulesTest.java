package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeathTagTurtleGuardRulesTest {
    @Test void guardEndsAtExactExpiry() {
        long triggeredAt = 100_000L;
        long until = triggeredAt + DeathTagTurtleGuardRules.DURATION_MILLIS;
        assertTrue(DeathTagTurtleGuardRules.isActive(triggeredAt, until));
        assertTrue(DeathTagTurtleGuardRules.isActive(until - 1L, until));
        assertFalse(DeathTagTurtleGuardRules.isActive(until, until));
    }

    @Test void guardReducesDamageButDoesNotGrantInvulnerability() {
        assertEquals(3.0D, DeathTagTurtleGuardRules.damageAfterGuard(6.0D, true));
        assertEquals(6.0D, DeathTagTurtleGuardRules.damageAfterGuard(6.0D, false));
        assertEquals(0.0D, DeathTagTurtleGuardRules.damageAfterGuard(-1.0D, true));
    }
}
