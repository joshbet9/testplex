package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvolutionAchievementRulesTest {
    @Test void quickEvolverReducesChargeAndTransformationTimeByTwentyFivePercent() {
        assertEquals(0.012D, ArcadePortRules.evolutionChargePerTick(1), 0.000001D);
        assertEquals(0.016D, ArcadePortRules.evolutionChargePerTick(1, true), 0.000001D);
        assertEquals(44L, ArcadePortRules.evolutionFormChangeTicks(false));
        assertEquals(33L, ArcadePortRules.evolutionFormChangeTicks(true));
        assertEquals(140L, ArcadePortRules.evolutionTotalAnimationTicks(false));
        assertEquals(105L, ArcadePortRules.evolutionTotalAnimationTicks(true));
    }

    @Test void wrongKitChampRequiresTheFinalStandardTransformationInTheQuickEvolverWindow() {
        assertTrue(ArcadePortRules.shouldAwardEvolutionWrongKitChamp(4, false, true, 33));
        assertTrue(ArcadePortRules.shouldAwardEvolutionWrongKitChamp(4, false, true, 43));
        assertFalse(ArcadePortRules.shouldAwardEvolutionWrongKitChamp(4, false, true, 32));
        assertFalse(ArcadePortRules.shouldAwardEvolutionWrongKitChamp(4, false, true, 44));
        assertFalse(ArcadePortRules.shouldAwardEvolutionWrongKitChamp(3, false, true, 40));
        assertFalse(ArcadePortRules.shouldAwardEvolutionWrongKitChamp(4, true, true, 40));
        assertFalse(ArcadePortRules.shouldAwardEvolutionWrongKitChamp(4, false, false, 40));
    }

    @Test void achievementMetadataUsesTheEvolutionProgressionNamespace() {
        assertEquals("wrong-kit-champ", EvolutionAchievement.WRONG_KIT_CHAMP.slug());
        assertEquals("Wrong Kit Champ", EvolutionAchievement.WRONG_KIT_CHAMP.displayName());
    }
}
