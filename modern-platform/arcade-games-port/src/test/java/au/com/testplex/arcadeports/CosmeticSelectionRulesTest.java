package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CosmeticSelectionRulesTest {
    @Test
    void disabledCosmeticStaysDisabledInsteadOfFallingBackToAStyle() {
        assertNull(CosmeticSelectionRules.normalizeOrDisabled(null));
        assertNull(CosmeticSelectionRules.normalizeOrDisabled("  "));
    }

    @Test
    void selectedCosmeticIsNormalizedForRuntimeConsumers() {
        assertEquals("golden-thread", CosmeticSelectionRules.normalizeOrDisabled(" Golden-Thread "));
    }

    @Test
    void disabledBeaconColourUsesItsOriginalDefault() {
        assertEquals("white", CosmeticSelectionRules.selectedOrDefault(null, "white"));
        assertEquals("white", CosmeticSelectionRules.selectedOrDefault("", "white"));
        assertEquals("violet", CosmeticSelectionRules.selectedOrDefault(" Violet ", "white"));
    }
}
