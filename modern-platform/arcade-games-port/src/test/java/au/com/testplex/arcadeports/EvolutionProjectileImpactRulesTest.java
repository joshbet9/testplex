package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EvolutionProjectileImpactRulesTest {
    @Test
    void cancelledTrackedEvolutionAbilityImpactsStillReachTheirCustomResolver() {
        assertTrue(EvolutionProjectileImpactRules.shouldProcessHit(
                ArcadePortGame.EVOLUTION, true, true));
    }

    @Test
    void cancelledUntrackedEvolutionImpactsRemainIgnored() {
        assertFalse(EvolutionProjectileImpactRules.shouldProcessHit(
                ArcadePortGame.EVOLUTION, false, true));
    }

    @Test
    void cancelledImpactsForOtherGamesRemainIgnored() {
        assertFalse(EvolutionProjectileImpactRules.shouldProcessHit(
                ArcadePortGame.GLADIATORS, true, true));
    }

    @Test
    void uncancelledImpactsKeepExistingDispatchBehavior() {
        assertTrue(EvolutionProjectileImpactRules.shouldProcessHit(
                ArcadePortGame.EVOLUTION, false, false));
        assertTrue(EvolutionProjectileImpactRules.shouldProcessHit(
                ArcadePortGame.GLADIATORS, false, false));
    }
}
