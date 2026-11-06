package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HeroesOfGwenDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void storeHealthPotionsUseProfileBackedHealingAndRestoreSourceDefaults() {
        assertEquals(4, ArcadePortRules.heroesOfGwenHealthPotionHealHp(false));
        assertEquals(8, ArcadePortRules.heroesOfGwenHealthPotionHealHp(true));
        assertEquals(20.0D, ArcadePortRules.heroesOfGwenHealthPotionResult(16.0D, 20.0D, true));

        GameDebugRuntimeOverrides.activate(Map.of(
                "HEROES_OF_GWEN_SMALL_HEALTH_POTION_HEAL_HP", 7,
                "HEROES_OF_GWEN_LARGE_HEALTH_POTION_HEAL_HP", 13));
        assertEquals(7, ArcadePortRules.heroesOfGwenHealthPotionHealHp(false));
        assertEquals(20.0D, ArcadePortRules.heroesOfGwenHealthPotionResult(9.0D, 20.0D, true));
        assertEquals(16.0D, ArcadePortRules.heroesOfGwenHealthPotionResult(9.0D, 16.0D, true));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(4, ArcadePortRules.heroesOfGwenHealthPotionHealHp(false));
        assertEquals(8, ArcadePortRules.heroesOfGwenHealthPotionHealHp(true));
    }

    @Test
    void bothPotionHealingValuesAreAvailableWithSafeRangesInTheHeroesDebugMenu() {
        GameDebugVariableCatalog.Variable small = GameDebugVariableCatalog.variable(ArcadePortGame.HEROES_OF_GWEN,
                "HEROES_OF_GWEN_SMALL_HEALTH_POTION_HEAL_HP");
        GameDebugVariableCatalog.Variable large = GameDebugVariableCatalog.variable(ArcadePortGame.HEROES_OF_GWEN,
                "HEROES_OF_GWEN_LARGE_HEALTH_POTION_HEAL_HP");
        assertNotNull(small);
        assertNotNull(large);
        assertEquals(0.0D, small.minimum());
        assertEquals(40.0D, small.maximum());
        assertEquals(0.0D, large.minimum());
        assertEquals(40.0D, large.maximum());
    }
}
