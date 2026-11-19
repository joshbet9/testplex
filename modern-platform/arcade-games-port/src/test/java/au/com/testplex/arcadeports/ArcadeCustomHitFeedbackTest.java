package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArcadeCustomHitFeedbackTest {
    @Test
    void paintballAndSpleefReceiveTheSharedHurtFeedbackForAppliedCustomHits() {
        assertTrue(ArcadeCustomHitFeedback.shouldShowLegacyHurtFeedback(ArcadePortGame.PAINTBALL, true));
        assertTrue(ArcadeCustomHitFeedback.shouldShowLegacyHurtFeedback(ArcadePortGame.SPLEEF, true));
    }

    @Test
    void ignoredHitsAndGamesWithTheirOwnDamageFeedbackDoNotUseTheCustomRoute() {
        assertFalse(ArcadeCustomHitFeedback.shouldShowLegacyHurtFeedback(ArcadePortGame.PAINTBALL, false));
        assertFalse(ArcadeCustomHitFeedback.shouldShowLegacyHurtFeedback(ArcadePortGame.SPLEEF, false));
        assertFalse(ArcadeCustomHitFeedback.shouldShowLegacyHurtFeedback(ArcadePortGame.BACON_BRAWL, true));
        assertFalse(ArcadeCustomHitFeedback.shouldShowLegacyHurtFeedback(null, true));
    }
}
