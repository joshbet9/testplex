package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraShyRulesTest {
    @Test
    void aMissingOrCorruptArenaMarkerForcesTheMazeToBeRebuilt() {
        assertTrue(CameraShyRules.arenaBuildRequired(false, Material.DEEPSLATE_TILES, Material.COARSE_DIRT));
        assertTrue(CameraShyRules.arenaBuildRequired(true, Material.AIR, Material.AIR));
        assertTrue(CameraShyRules.arenaBuildRequired(true, Material.DEEPSLATE_TILES, Material.AIR));
        assertFalse(CameraShyRules.arenaBuildRequired(true, Material.DEEPSLATE_TILES, Material.COARSE_DIRT));
    }

    @Test
    void anUnderstudyOnlyAwardsItsOwnerWhenAnOpposingHumanPhotographsIt() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID opponent = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertTrue(CameraShyRules.shouldAwardUnderstudy(owner, opponent, true, true));
        assertFalse(CameraShyRules.shouldAwardUnderstudy(owner, owner, true, true));
        assertFalse(CameraShyRules.shouldAwardUnderstudy(owner, opponent, true, false));
        assertFalse(CameraShyRules.shouldAwardUnderstudy(owner, opponent, false, true));
        assertFalse(CameraShyRules.shouldAwardUnderstudy(null, opponent, true, true));
    }

    @Test
    void aSmokeEscapeCanOnlyBeCreditedToItsOwnersOwnCloud() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertTrue(CameraShyRules.smokeBelongsTo(owner, owner));
        assertFalse(CameraShyRules.smokeBelongsTo(other, owner));
        assertFalse(CameraShyRules.smokeBelongsTo(null, owner));
        assertTrue(CameraShyRules.smokeBelongsTo(other, null));
    }

    @Test
    void smokeAndMirrorsRequiresTheCloudToObscureThePursuitBeforeItsWindowExpires() {
        assertTrue(CameraShyRules.shouldAwardSmokeEscape(true, 5_000L, 7_000L));
        assertFalse(CameraShyRules.shouldAwardSmokeEscape(false, 5_000L, 7_000L));
        assertFalse(CameraShyRules.shouldAwardSmokeEscape(true, 7_001L, 7_000L));
        assertFalse(CameraShyRules.shouldAwardSmokeEscape(true, 5_000L, 0L));
    }

    @Test
    void timeoutRankingUsesAllMatchMetricsThenAStableUuidTieBreak() {
        UUID lower = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higher = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertTrue(CameraShyRules.compareTimeoutRank(1, 2, 0, 99, 0, 99, lower, higher) > 0,
                "lower exposure wins before any other metric");
        assertTrue(CameraShyRules.compareTimeoutRank(2, 2, 4, 3, 0, 99, lower, higher) > 0,
                "more photographs breaks an exposure tie");
        assertTrue(CameraShyRules.compareTimeoutRank(2, 2, 4, 4, 2, 1, lower, higher) > 0,
                "more eliminations breaks the remaining tie");
        assertTrue(CameraShyRules.compareTimeoutRank(2, 2, 4, 4, 1, 1, higher, lower) > 0,
                "UUID provides a stable final tie-break");
    }

    @Test
    void fiveExposureThresholdIsInclusive() {
        assertFalse(CameraShyRules.eliminatedAt(4));
        assertTrue(CameraShyRules.eliminatedAt(5));
        assertTrue(CameraShyRules.eliminatedAt(6));
    }

    @Test
    void targetProtectionAndCameraRechargeAreIndependent() {
        assertTrue(CameraShyRules.mayPhotograph(10_000L, 9_999L, 9_999L));
        assertFalse(CameraShyRules.mayPhotograph(10_000L, 10_001L, 9_999L));
        assertFalse(CameraShyRules.mayPhotograph(10_000L, 9_999L, 10_001L));
    }

    @Test
    void closeAndLongDistanceBoundariesAreInclusive() {
        assertTrue(CameraShyRules.isClosePhoto(3.0D));
        assertFalse(CameraShyRules.isClosePhoto(3.01D));
        assertTrue(CameraShyRules.isLongDistancePhoto(18.0D));
        assertTrue(CameraShyRules.isLongDistancePhoto(24.0D));
        assertFalse(CameraShyRules.isLongDistancePhoto(24.01D));
    }

    @Test
    void achievementWindowsDoNotAcceptFutureOrExpiredEvents() {
        assertTrue(CameraShyRules.withinWindow(10_000L, 5_000L, 5_000L));
        assertFalse(CameraShyRules.withinWindow(10_001L, 5_000L, 5_000L));
        assertFalse(CameraShyRules.withinWindow(9_000L, 10_000L, 5_000L));
    }

    @Test
    void winningStreakResetsOnlyOnALoss() {
        assertEquals(4, CameraShyRules.consecutiveWinsAfter(3, true));
        assertEquals(0, CameraShyRules.consecutiveWinsAfter(3, false));
    }

    @Test
    void soloTestWinnerMustClearAllAfkTargetsButMultiplayerEndsWithOneHuman() {
        assertFalse(CameraShyRules.shouldEndMatch(16, 1, 1));
        assertFalse(CameraShyRules.shouldEndMatch(2, 1, 1));
        assertTrue(CameraShyRules.shouldEndMatch(1, 1, 1));
        assertTrue(CameraShyRules.shouldEndMatch(10, 0, 1));
        assertTrue(CameraShyRules.shouldEndMatch(10, 1, 4));
        assertFalse(CameraShyRules.shouldEndMatch(14, 2, 4));
    }

    @Test
    void smokeBlocksOnlyWhenItsSphereIntersectsTheCameraRay() {
        assertTrue(CameraShyRules.segmentIntersectsSphere(
                new Vector(0, 0, 0), new Vector(10, 0, 0), new Vector(5, 0, 0), 1.0D));
        assertFalse(CameraShyRules.segmentIntersectsSphere(
                new Vector(0, 0, 0), new Vector(10, 0, 0), new Vector(5, 3, 0), 1.0D));
    }

    @Test
    void groupPhotoRequiresASecondPlayerInsideTheCameraCone() {
        assertTrue(CameraShyRules.insidePhotoCone(new Vector(1, 0, 0), new Vector(5, 0, 0), 0.985D));
        assertFalse(CameraShyRules.insidePhotoCone(new Vector(1, 0, 0), new Vector(0, 0, 5), 0.985D));
    }
}
