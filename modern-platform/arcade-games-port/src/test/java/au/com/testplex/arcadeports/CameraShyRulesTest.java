package au.com.testplex.arcadeports;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraShyRulesTest {
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
