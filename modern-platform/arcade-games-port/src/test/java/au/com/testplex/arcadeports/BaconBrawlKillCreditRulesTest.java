package au.com.testplex.arcadeports;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaconBrawlKillCreditRulesTest {
    private static final UUID VICTIM = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ATTACKER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void resolvesRecentArenaKnockoutKillerBeforeMessage() {
        assertEquals(ATTACKER, BaconBrawlKillCreditRules.resolveForDeathMessage(
                null, false, VICTIM, ATTACKER, true, 20_000L, 5_000L, true));
    }

    @Test
    void keepsExplicitValidKillerAndRejectsStaleOrInvalidFallback() {
        assertEquals(OTHER, BaconBrawlKillCreditRules.resolveForDeathMessage(
                OTHER, true, VICTIM, ATTACKER, true, 20_000L, 5_000L, true));
        assertNull(BaconBrawlKillCreditRules.resolveForDeathMessage(
                null, false, VICTIM, ATTACKER, true, 40_001L, 5_000L, true));
        assertNull(BaconBrawlKillCreditRules.resolveForDeathMessage(
                null, false, VICTIM, VICTIM, true, 20_000L, 5_000L, true));
        assertNull(BaconBrawlKillCreditRules.resolveForDeathMessage(
                null, false, VICTIM, ATTACKER, false, 20_000L, 5_000L, true));
    }

    @Test
    void doesNotCreditArenaHitterForNonKnockoutDeath() {
        assertNull(BaconBrawlKillCreditRules.resolveForDeathMessage(
                null, false, VICTIM, ATTACKER, true, 20_000L, 5_000L, false));
    }
}
