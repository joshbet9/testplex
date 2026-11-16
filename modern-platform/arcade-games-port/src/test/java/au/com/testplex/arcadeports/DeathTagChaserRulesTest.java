package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DeathTagChaserRulesTest {
    @Test
    void playerRepresentationsDoNotBurnInDaylight() {
        assertFalse(DeathTagChaserRules.shouldBurnInDaylight());
    }
}
