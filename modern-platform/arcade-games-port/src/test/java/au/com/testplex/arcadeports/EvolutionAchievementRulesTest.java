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

    @Test void approvedKitModifiersChangeTheProductionTimersByTheChosenAmounts() {
        assertEquals(72_000L, EvolutionAchievementRules.neanderthalPityClock(90_000L));
        assertEquals(18_750L, EvolutionAchievementRules.neanderthalPityReduction(15_000L));
        assertEquals(9_375L, EvolutionAchievementRules.neanderthalPityReduction(7_500L));
        assertEquals(4_690L, EvolutionAchievementRules.darwinistCooldown(7_000L));
        assertEquals(3_350L, EvolutionAchievementRules.darwinistCooldown(5_000L));
    }

    @Test void achievementPredicatesRequireTheApprovedWinAndGameplayConditions() {
        assertTrue(EvolutionAchievementRules.shouldAwardOverkill(true, true));
        assertFalse(EvolutionAchievementRules.shouldAwardOverkill(false, true));
        assertFalse(EvolutionAchievementRules.shouldAwardOverkill(true, false));

        assertTrue(EvolutionAchievementRules.shouldAwardSecondWind(true, 60.0D));
        assertFalse(EvolutionAchievementRules.shouldAwardSecondWind(true, 59.99D));
        assertFalse(EvolutionAchievementRules.shouldAwardSecondWind(false, 100.0D));

        assertTrue(EvolutionAchievementRules.shouldAwardPityParty(true, 2));
        assertFalse(EvolutionAchievementRules.shouldAwardPityParty(false, 5));
        assertFalse(EvolutionAchievementRules.shouldAwardPityParty(true, 1));

        assertTrue(EvolutionAchievementRules.shouldAwardQuickChangeArtist(true, true, true, 4));
        assertFalse(EvolutionAchievementRules.shouldAwardQuickChangeArtist(true, true, false, 5));
        assertFalse(EvolutionAchievementRules.shouldAwardQuickChangeArtist(true, false, true, 5));
        assertFalse(EvolutionAchievementRules.shouldAwardQuickChangeArtist(true, true, true, 3));

        assertTrue(EvolutionAchievementRules.shouldAwardPityTheAncient(true, true, 5));
        assertFalse(EvolutionAchievementRules.shouldAwardPityTheAncient(false, true, 5));
        assertFalse(EvolutionAchievementRules.shouldAwardPityTheAncient(true, true, 4));
    }

    @Test void abilityAchievementsUseDistinctHitsCorrectKitAndTheSameLife() {
        assertFalse(EvolutionAchievementRules.shouldAwardGroundbreaker(2));
        assertTrue(EvolutionAchievementRules.shouldAwardGroundbreaker(3));
        assertFalse(EvolutionAchievementRules.shouldAwardBounceHouse(2));
        assertTrue(EvolutionAchievementRules.shouldAwardBounceHouse(3));
        assertTrue(EvolutionAchievementRules.qualifiesBloodHarvestKill(5.0D, 20.0D, 20.0D, 3));
        assertFalse(EvolutionAchievementRules.qualifiesBloodHarvestKill(6.0D, 20.0D, 20.0D, 3));
        assertFalse(EvolutionAchievementRules.qualifiesBloodHarvestKill(5.0D, 20.0D, 19.0D, 3));
        assertFalse(EvolutionAchievementRules.qualifiesBloodHarvestKill(5.0D, 20.0D, 20.0D, 2));
        assertTrue(EvolutionAchievementRules.shouldAwardEggspert(true, true, true));
        assertFalse(EvolutionAchievementRules.shouldAwardEggspert(false, true, true));
        assertFalse(EvolutionAchievementRules.shouldAwardEggspert(true, false, true));
        assertFalse(EvolutionAchievementRules.shouldAwardEggspert(true, true, false));
    }

    @Test void wrongKitChampIsLimitedToDarwinistInTheFinalTransformationWindow() {
        assertTrue(EvolutionAchievementRules.shouldAwardWrongKitChamp("darwinist", 4, true, 33));
        assertFalse(EvolutionAchievementRules.shouldAwardWrongKitChamp("quick-evolver", 4, true, 40));
        assertFalse(EvolutionAchievementRules.shouldAwardWrongKitChamp("darwinist", 3, true, 40));
        assertFalse(EvolutionAchievementRules.shouldAwardWrongKitChamp("darwinist", 4, false, 40));
    }
}
