package au.com.testplex.arcadeports;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
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

    @Test void drawMyThingKeenEyeProgressIsSuppressedForDebugAndFailedRounds() {
        assertTrue(ArcadeProgressRules.shouldRecordDrawMyThingKeenEye(false, false));
        assertFalse(ArcadeProgressRules.shouldRecordDrawMyThingKeenEye(false, true));
        assertFalse(ArcadeProgressRules.shouldRecordDrawMyThingKeenEye(true, false));
        assertFalse(ArcadeProgressRules.shouldRecordDrawMyThingKeenEye(true, true));
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

    @Test void masterBuildersNewAchievementsUseFinalBuildAndVotingEvidence() {
        UUID voter = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersBlockParty(Set.of(
                Material.STONE, Material.DIRT, Material.OAK_PLANKS, Material.GLASS,
                Material.BRICKS, Material.SAND, Material.GRAVEL, Material.COBBLESTONE,
                Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.QUARTZ_BLOCK)));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersBlockParty(Set.of(Material.STONE)));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersMinimalist(32));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersMinimalist(33));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersSecondDraft(30));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersSecondDraft(29));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersTouchingTheSky(true));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersTouchingTheSky(false));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersCriticsDarling(5.0D));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersCriticsDarling(4.999D));

        Set<UUID> eligible = Set.of(voter, second, third);
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersUnanimousOvation(
                Map.of(voter, 6, second, 6, third, 6), eligible));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersUnanimousOvation(
                Map.of(voter, 6, second, 5, third, 6), eligible));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersMixedReviews(Map.of(voter, 1, second, 6)));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersMixedReviews(Map.of(voter, 1, second, 4)));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersKingmaker(500, 499, 499, 6));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersKingmaker(500, 500, 499, 6));
        assertTrue(ArcadeProgressRules.shouldRecordMasterBuildersFairVote(3));
        assertFalse(ArcadeProgressRules.shouldRecordMasterBuildersFairVote(2));

        Map<UUID, Map<UUID, Integer>> votes = Map.of(
                first, Map.of(voter, 6), second, Map.of(voter, 4), third, Map.of(voter, 1));
        Map<UUID, Double> averages = Map.of(first, 5.8D, second, 4.0D, third, 1.5D);
        Map<UUID, Integer> rounded = Map.of(first, 580, second, 400, third, 150);
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersFairCritic(
                voter, Set.of(first, second, third), votes, averages));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersGoldenEye(
                voter, Set.of(first, second, third), votes, rounded));
        assertTrue(ArcadeProgressRules.shouldAwardMasterBuildersGoldenEye(voter,
                Set.of(first, second, third), votes, Map.of(first, 500, second, 500, third, 150)),
                "Standings tied after rounding do not require an invented order.");
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersGoldenEye(voter,
                Set.of(first, second, third), Map.of(first, Map.of(voter, 6), second, Map.of(voter, 4)), rounded),
                "The achievement requires a rating for every other build.");
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersGoldenEye(voter,
                Set.of(first, second, third), Map.of(first, Map.of(voter, 1),
                        second, Map.of(voter, 4), third, Map.of(voter, 6)), rounded));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersFairCritic(voter,
                Set.of(first, second, third), votes, Map.of(first, 3.0D, second, 4.0D, third, 1.5D)));
        assertFalse(ArcadeProgressRules.shouldAwardMasterBuildersFairCritic(voter,
                Set.of(voter), Map.of(), Map.of()), "A solo builder cannot earn a vote-based award without voting.");
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
