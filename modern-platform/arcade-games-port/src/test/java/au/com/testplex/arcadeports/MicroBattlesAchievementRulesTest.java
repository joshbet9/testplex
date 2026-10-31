package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MicroBattlesAchievementRulesTest {
    @Test
    void borderBuildRequiresPlacementInsideTheLastTwoBlocksAndThreeBlocksTotal() {
        assertTrue(MicroBattlesAchievementRules.isBorderBuildDistance(0.0D));
        assertTrue(MicroBattlesAchievementRules.isBorderBuildDistance(2.0D));
        assertFalse(MicroBattlesAchievementRules.isBorderBuildDistance(2.01D));
        assertFalse(MicroBattlesAchievementRules.isBorderBuildDistance(-0.1D));
        assertFalse(MicroBattlesAchievementRules.isBorderBuildDistance(Double.NaN));
        assertFalse(MicroBattlesAchievementRules.shouldAwardMicroBattlesKing(2));
    }

    @Test
    void fighterOnlyQualifiesWhenThePerkChangesALethalFinalDuelHitIntoASurvivableOne() {
        assertTrue(MicroBattlesAchievementRules.isCounterfactualFighterSave(4.0D, 3.0D, 4.0D));
        assertFalse(MicroBattlesAchievementRules.isCounterfactualFighterSave(4.0D, 4.0D, 4.0D));
        assertFalse(MicroBattlesAchievementRules.isCounterfactualFighterSave(4.0D, 3.5D, 3.9D));
        assertFalse(MicroBattlesAchievementRules.isCounterfactualFighterSave(0.0D, 0.0D, 2.0D));
    }

    @Test
    void placedBlockCollisionRequiresMovementTowardTheRecentSolidBlock() {
        assertTrue(MicroBattlesAchievementRules.isApproachingPlacedBlock(0.0D, 0.5D,
                0.3D, 0.5D, 1.0D, 0.5D));
        assertFalse(MicroBattlesAchievementRules.isApproachingPlacedBlock(0.0D, 0.5D,
                -0.3D, 0.5D, 1.0D, 0.5D));
        assertFalse(MicroBattlesAchievementRules.isRecent(20_000L, 10_000L, 3_000L));
        assertTrue(MicroBattlesAchievementRules.isRecent(12_500L, 10_000L, 3_000L));
    }

    @Test
    void sniperRequiresRecentHighGroundHitAndAnEightBlockFall() {
        assertTrue(MicroBattlesAchievementRules.isSniperKnockOff(15_000L, 10_000L, 90.0D, 82.0D));
        assertFalse(MicroBattlesAchievementRules.isSniperKnockOff(22_001L, 10_000L, 90.0D, 70.0D));
        assertFalse(MicroBattlesAchievementRules.isSniperKnockOff(15_000L, 10_000L, 90.0D, 83.0D));
        assertTrue(MicroBattlesAchievementRules.shouldAwardMicroBattlesKing(3));
        assertTrue(MicroBattlesAchievementRules.hasKnockedEveryOpposingTeam(3));
        assertFalse(MicroBattlesAchievementRules.hasKnockedEveryOpposingTeam(2));
    }
}
