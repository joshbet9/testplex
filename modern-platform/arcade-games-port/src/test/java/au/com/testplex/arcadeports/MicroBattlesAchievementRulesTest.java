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

    @Test
    void borderAwardRequiresThreeNearBorderPlacementsAndNoBorderDeath() {
        assertFalse(MicroBattlesAchievementRules.shouldAwardPhew(2, false));
        assertFalse(MicroBattlesAchievementRules.shouldAwardPhew(3, true));
        assertTrue(MicroBattlesAchievementRules.shouldAwardPhew(3, false));
    }

    @Test
    void lastTwoRequiresPersonalEliminationsOfEveryFinalOpposingPlayerAndVictory() {
        assertFalse(MicroBattlesAchievementRules.shouldAwardLastTwo(3, 2, true, true));
        assertFalse(MicroBattlesAchievementRules.shouldAwardLastTwo(3, 3, false, true));
        assertFalse(MicroBattlesAchievementRules.shouldAwardLastTwo(3, 3, true, false));
        assertTrue(MicroBattlesAchievementRules.shouldAwardLastTwo(3, 3, true, true));
    }

    @Test
    void kitAndWinAchievementsRequireAllTheirGameplayConditions() {
        assertFalse(MicroBattlesAchievementRules.shouldAwardMoshPitPlayroom(7));
        assertTrue(MicroBattlesAchievementRules.shouldAwardMoshPitPlayroom(8));
        assertFalse(MicroBattlesAchievementRules.shouldAwardBuiltForThis(true, false));
        assertTrue(MicroBattlesAchievementRules.shouldAwardBuiltForThis(true, true));
        assertFalse(MicroBattlesAchievementRules.shouldAwardShiftSupervisor(30, 29, true));
        assertFalse(MicroBattlesAchievementRules.shouldAwardShiftSupervisor(30, 30, false));
        assertTrue(MicroBattlesAchievementRules.shouldAwardShiftSupervisor(30, 30, true));
        assertFalse(MicroBattlesAchievementRules.shouldAwardNoRoomToBuild(1, true));
        assertTrue(MicroBattlesAchievementRules.shouldAwardNoRoomToBuild(0, true));
    }

    @Test
    void lastBreathAndLongShotUseBoundedGameplayThresholds() {
        assertTrue(MicroBattlesAchievementRules.shouldAwardLastBreath(2.0D, true));
        assertFalse(MicroBattlesAchievementRules.shouldAwardLastBreath(2.01D, true));
        assertFalse(MicroBattlesAchievementRules.shouldAwardLastBreath(0.0D, true));
        assertFalse(MicroBattlesAchievementRules.shouldAwardLastBreath(1.0D, false));
        assertTrue(MicroBattlesAchievementRules.isNeedleThreaderDistance(25.0D));
        assertFalse(MicroBattlesAchievementRules.isNeedleThreaderDistance(24.99D));
    }

    @Test
    void airMailRequiresARealGapCrossingAndAHostileIslandLanding() {
        assertTrue(MicroBattlesAchievementRules.isAirMailLanding(6.0D, 0, 1, true));
        assertFalse(MicroBattlesAchievementRules.isAirMailLanding(5.99D, 0, 1, true));
        assertFalse(MicroBattlesAchievementRules.isAirMailLanding(8.0D, 0, 0, true));
        assertFalse(MicroBattlesAchievementRules.isAirMailLanding(8.0D, 0, 1, false));
    }
}
