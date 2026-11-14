package au.com.testplex.arcadeports;

import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GladiatorsPotionRulesTest {
    @Test
    void potModeUsesTheVanillaHealingPotionType() {
        assertEquals(PotionType.HEALING, ArcadePortRules.gladiatorsPotionType());
    }
}
