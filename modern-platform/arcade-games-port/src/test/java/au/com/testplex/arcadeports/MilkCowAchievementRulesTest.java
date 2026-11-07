package au.com.testplex.arcadeports;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MilkCowAchievementRulesTest {
    @Test
    void lastDropRequiresTheWinningMilkPointAtOneHeartOrLess() {
        assertTrue(MilkCowAchievementRules.shouldAwardLastDrop(2.0D, 14, 1, 15));
        assertFalse(MilkCowAchievementRules.shouldAwardLastDrop(2.01D, 14, 1, 15));
        assertFalse(MilkCowAchievementRules.shouldAwardLastDrop(2.0D, 13, 1, 15));
        assertFalse(MilkCowAchievementRules.shouldAwardLastDrop(2.0D, 14, 0, 15));
        assertFalse(MilkCowAchievementRules.shouldAwardLastDrop(0.0D, 14, 1, 15));
    }

    @Test
    void wholeHerdRequiresThreeDistinctCowPlayersAndFarmerVictory() {
        assertFalse(MilkCowAchievementRules.shouldAwardWholeHerd(2, true));
        assertFalse(MilkCowAchievementRules.shouldAwardWholeHerd(3, false));
        assertTrue(MilkCowAchievementRules.shouldAwardWholeHerd(3, true));
    }

    @Test
    void areaAbilitiesNeedThreeDifferentFarmersAndAnElimination() {
        assertFalse(MilkCowAchievementRules.shouldAwardAreaAbility(2, 1));
        assertFalse(MilkCowAchievementRules.shouldAwardAreaAbility(3, 0));
        assertTrue(MilkCowAchievementRules.shouldAwardAreaAbility(3, 1));
    }

    @Test
    void fullMooRequiresAllThreeAbilitiesOnTheSameTargetAndACreditedElimination() {
        Set<String> complete = Set.of("cow-bomb", "body-slam", "cow-charge");
        assertTrue(MilkCowAchievementRules.shouldAwardFullMoo(complete, true));
        assertFalse(MilkCowAchievementRules.shouldAwardFullMoo(
                Set.of("cow-bomb", "body-slam"), true));
        assertFalse(MilkCowAchievementRules.shouldAwardFullMoo(complete, false));
        assertFalse(MilkCowAchievementRules.shouldAwardFullMoo(null, true));
    }

    @Test
    void stampedeRequiresFiveDistinctTargetsAndThreeCreditedKillsInOneSequence() {
        assertFalse(MilkCowAchievementRules.shouldAwardStampede(4, 3));
        assertFalse(MilkCowAchievementRules.shouldAwardStampede(5, 2));
        assertTrue(MilkCowAchievementRules.shouldAwardStampede(5, 3));
    }

    @Test
    void spiltMilkRequiresThreeNearVictoryBucketEmptyingsAndAnActualLoss() {
        assertFalse(MilkCowAchievementRules.shouldAwardSpiltMilk(2, true));
        assertFalse(MilkCowAchievementRules.shouldAwardSpiltMilk(3, false));
        assertTrue(MilkCowAchievementRules.shouldAwardSpiltMilk(3, true));
    }
}
