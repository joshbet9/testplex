package au.com.testplex.arcadeports;

import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArcadeProgressRulesTest {
    @Test void drawMyThingPerfectGameRequiresEveryExpectedTurnAndNoMistake() {
        assertTrue(ArcadeProgressRules.isDrawMyThingPixelLinguist(3, 3, false));
        assertFalse(ArcadeProgressRules.isDrawMyThingPixelLinguist(2, 3, false));
        assertFalse(ArcadeProgressRules.isDrawMyThingPixelLinguist(3, 3, true));
        assertFalse(ArcadeProgressRules.isDrawMyThingPixelLinguist(0, 0, false));
    }

    @Test void drawMyThingFirstGuessAndFastGuessThresholdsAreBounded() {
        assertFalse(ArcadeProgressRules.shouldAwardDrawMyThingTripleThreat(2));
        assertTrue(ArcadeProgressRules.shouldAwardDrawMyThingTripleThreat(3));
        assertTrue(ArcadeProgressRules.shouldAwardDrawMyThingFastGuesser(0));
        assertTrue(ArcadeProgressRules.shouldAwardDrawMyThingFastGuesser(5_000));
        assertFalse(ArcadeProgressRules.shouldAwardDrawMyThingFastGuesser(5_001));
        assertFalse(ArcadeProgressRules.shouldAwardDrawMyThingFastGuesser(-1));
    }

    @Test void masterBuildersPuristAndArchitectRequireTheExactConditions() {
        assertTrue(ArcadeProgressRules.isMasterBuildersPurist(Set.of(Material.OAK_PLANKS)));
        assertFalse(ArcadeProgressRules.isMasterBuildersPurist(Set.of(Material.OAK_PLANKS, Material.GLASS)));
        assertFalse(ArcadeProgressRules.isMasterBuildersPurist(Set.of()));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersArchitect(420, 400));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersArchitect(419, 400));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersArchitect(420, Integer.MIN_VALUE));
    }

    @Test void masterBuildersGreaterGoodUsesVoteAdjustedWinnerAndRequiresTheVoterToWin() {
        assertEquals(320, ArcadeProgressRules.masterBuildersScoreIfVoteChangedToOne(21, 5, 6));
        assertEquals(Integer.MIN_VALUE, ArcadeProgressRules.masterBuildersScoreIfVoteChangedToOne(21, 0, 6));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersGreaterGood(430, 420, 400));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersGreaterGood(420, 420, 400));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersGreaterGood(430, 420, 430));
    }

    @Test void milkTheCowAwardsUseLifetimeSafeInRoundConditions() {
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowPerfectGame(0));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowPerfectGame(1));
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowHighClub(true, false));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowHighClub(true, true));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowHighClub(false, false));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowStopStopHeIsAlreadyDead(2));
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowStopStopHeIsAlreadyDead(3));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowGotTheChops(9));
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowGotTheChops(10));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowSlaughterhouse(14));
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowSlaughterhouse(15));
    }


    @Test void witherAssaultAchievementConditionsRequireRealInRoundEvents() {
        assertTrue(ArcadeProgressRules.isRecentWitherHumanActivity(61_000, 1_000, 60_000));
        assertFalse(ArcadeProgressRules.isRecentWitherHumanActivity(61_001, 1_000, 60_000));
        assertFalse(ArcadeProgressRules.isRecentWitherHumanActivity(999, 1_000, 60_000));
        assertTrue(ArcadeProgressRules.shouldAwardWitherSavior(5));
        assertFalse(ArcadeProgressRules.shouldAwardWitherSavior(4));
        assertTrue(ArcadeProgressRules.shouldAwardWitherTormentor(3));
        assertFalse(ArcadeProgressRules.shouldAwardWitherTormentor(2));
        assertTrue(ArcadeProgressRules.shouldAwardWitherWorstWitherEver(5, true));
        assertFalse(ArcadeProgressRules.shouldAwardWitherWorstWitherEver(5, false));
        assertFalse(ArcadeProgressRules.shouldAwardWitherWorstWitherEver(4, true));
        assertTrue(ArcadeProgressRules.shouldAwardWitherLucidDreams(true, false));
        assertFalse(ArcadeProgressRules.shouldAwardWitherLucidDreams(true, true));
        assertFalse(ArcadeProgressRules.shouldAwardWitherLucidDreams(false, false));
    }

}
