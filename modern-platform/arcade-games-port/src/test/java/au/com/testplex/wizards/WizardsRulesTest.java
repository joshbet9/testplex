package au.com.testplex.wizards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WizardsRulesTest {
    @Test
    void capsEveryDirectMeleeHitAtTheOriginalHalfHeartValue() {
        assertEquals(0.5D, WizardsRules.capMeleeDamage(4.0D), 0.000001D);
        assertEquals(0.5D, WizardsRules.capMeleeDamage(0.5D), 0.000001D);
        assertEquals(0.25D, WizardsRules.capMeleeDamage(0.25D), 0.000001D);
        assertEquals(0.0D, WizardsRules.capMeleeDamage(0.0D), 0.000001D);
    }

    @Test
    void rejectsNegativeOrNonFiniteDamageInputs() {
        assertEquals(0.0D, WizardsRules.capMeleeDamage(-1.0D), 0.000001D);
        assertEquals(0.0D, WizardsRules.capMeleeDamage(Double.NaN), 0.000001D);
        assertEquals(0.0D, WizardsRules.capMeleeDamage(Double.POSITIVE_INFINITY), 0.000001D);
    }
}
