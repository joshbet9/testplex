package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraShyRulesTest {
    @AfterEach
    void clearDebugOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void photoEliminationDistanceAndLensSettingsChangeLiveRulesAndRestore() {
        assertFalse(CameraShyRules.eliminatedAt(4));
        assertTrue(CameraShyRules.eliminatedAt(5));
        assertTrue(CameraShyRules.isClosePhoto(3.0D));
        assertTrue(CameraShyRules.isLongDistancePhoto(18.0D));
        assertTrue(CameraShyRules.insidePhotoCone(new Vector(1.0D, 0.0D, 0.0D),
                new Vector(1.0D, 0.35D, 0.0D), CameraShyRules.photoLensMinDot()));

        GameDebugRuntimeOverrides.activate(Map.of(
                "CAMERA_SHY_PHOTOS_TO_ELIMINATE", 3,
                "CAMERA_SHY_CLOSE_PHOTO_BLOCKS", 5.0D,
                "CAMERA_SHY_LONG_PHOTO_BLOCKS", 12.0D,
                "CAMERA_SHY_CAMERA_RANGE_BLOCKS", 16.0D,
                "CAMERA_SHY_PHOTO_LENS_MIN_DOT", 0.98D));
        assertTrue(CameraShyRules.eliminatedAt(3));
        assertTrue(CameraShyRules.isClosePhoto(5.0D));
        assertTrue(CameraShyRules.isLongDistancePhoto(12.0D));
        assertFalse(CameraShyRules.isLongDistancePhoto(18.0D));
        assertFalse(CameraShyRules.insidePhotoCone(new Vector(1.0D, 0.0D, 0.0D),
                new Vector(1.0D, 0.35D, 0.0D), CameraShyRules.photoLensMinDot()));

        GameDebugRuntimeOverrides.clear();
        assertFalse(CameraShyRules.eliminatedAt(4));
        assertTrue(CameraShyRules.eliminatedAt(5));
        assertFalse(CameraShyRules.isClosePhoto(3.01D));
        assertTrue(CameraShyRules.isLongDistancePhoto(24.0D));
        assertEquals(0.94D, CameraShyRules.photoLensMinDot());
    }

    @Test
    void debugRoundAdvanceUsesTheNormalTimeoutDeadlineOnlyDuringLivePlay() {
        assertEquals(9_000L, CameraShyRules.debugRoundDeadline(false, 1_000L, 9_000L));
        assertEquals(1_000L, CameraShyRules.debugRoundDeadline(true, 1_000L, 9_000L));
        assertEquals(500L, CameraShyRules.debugRoundDeadline(true, 1_000L, 500L));
    }

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
    void bareStatsUsesSharedRootAndPlexballRequiresExplicitSelection() {
        assertFalse(CameraShyRules.routesToPlexballStats(null));
        assertFalse(CameraShyRules.routesToPlexballStats(new String[0]));
        assertTrue(CameraShyRules.routesToPlexballStats(new String[] {"plexball"}));
        assertTrue(CameraShyRules.routesToPlexballStats(new String[] {"PLEXBALL"}));
        assertFalse(CameraShyRules.routesToPlexballStats(new String[] {"snap"}));
        assertFalse(CameraShyRules.routesToPlexballStats(new String[] {"camera-shy"}));
        assertFalse(CameraShyRules.routesToPlexballStats(new String[] {"snap", "2"}));
        assertFalse(CameraShyRules.routesToPlexballStats(new String[] {"camera-shy", "2"}));
        assertFalse(CameraShyRules.routesToPlexballStats(new String[] {"tester", "camera-shy", "3"}));
        assertFalse(CameraShyRules.routesToPlexballStats(new String[] {"tester"}));
    }

    @Test
    void cameraShyQueueAndStartRequireExplicitReadyAllowListMembership() {
        assertTrue(ArcadePortRules.isArcadePortGameEnabled("camera-shy", java.util.List.of("camera-shy")));
        assertFalse(ArcadePortRules.isArcadePortGameEnabled("camera-shy", java.util.List.of("plexball", "spleef")));
        assertFalse(ArcadePortRules.isArcadePortGameEnabled("camera-shy", java.util.List.of()));
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
    void snapDebugProfilesTuneKitEffectsPursuitAndProgressionThenRestoreDefaults() {
        assertEquals(55_000L, CameraShyRules.longSetting("SPOTLIGHT_INTERVAL_MILLIS",
                CameraShyRules.SPOTLIGHT_INTERVAL_MILLIS));
        assertEquals(25_000L, CameraShyRules.longSetting("DECOY_ABILITY_COOLDOWN_MILLIS",
                CameraShyRules.DECOY_ABILITY_COOLDOWN_MILLIS));
        assertEquals(18.0D, CameraShyRules.doubleSetting("SPOTTER_RANGE_BLOCKS",
                CameraShyRules.SPOTTER_RANGE_BLOCKS));
        assertEquals(3, CameraShyRules.intSetting("FALSE_ALARM_TARGETS_REQUIRED",
                CameraShyRules.FALSE_ALARM_TARGETS_REQUIRED));
        assertTrue(CameraShyRules.insidePhotoCone(new Vector(1, 0, 0), new Vector(1, 0.35D, 0), 0.94D));

        GameDebugRuntimeOverrides.activate(Map.of(
                "CAMERA_SHY_SPOTLIGHT_INTERVAL_MILLIS", 10_000L,
                "CAMERA_SHY_DECOY_ABILITY_COOLDOWN_MILLIS", 5_000L,
                "CAMERA_SHY_SPOTTER_RANGE_BLOCKS", 4.0D,
                "CAMERA_SHY_DECOY_LOOK_MIN_DOT", 0.99D,
                "CAMERA_SHY_PURSUIT_ESCAPE_MILLIS", 500L,
                "CAMERA_SHY_RUNNER_ABILITY_DURATION_MILLIS", 7_000L,
                "CAMERA_SHY_FALSE_ALARM_TARGETS_REQUIRED", 1));
        assertEquals(10_000L, CameraShyRules.longSetting("SPOTLIGHT_INTERVAL_MILLIS",
                CameraShyRules.SPOTLIGHT_INTERVAL_MILLIS));
        assertEquals(5_000L, CameraShyRules.longSetting("DECOY_ABILITY_COOLDOWN_MILLIS",
                CameraShyRules.DECOY_ABILITY_COOLDOWN_MILLIS));
        assertEquals(4.0D, CameraShyRules.doubleSetting("SPOTTER_RANGE_BLOCKS",
                CameraShyRules.SPOTTER_RANGE_BLOCKS));
        assertEquals(500L, CameraShyRules.longSetting("PURSUIT_ESCAPE_MILLIS",
                CameraShyRules.PURSUIT_ESCAPE_MILLIS));
        assertEquals(7_000L, CameraShyRules.longSetting("RUNNER_ABILITY_DURATION_MILLIS",
                CameraShyRules.RUNNER_ABILITY_DURATION_MILLIS));
        assertEquals(1, CameraShyRules.intSetting("FALSE_ALARM_TARGETS_REQUIRED",
                CameraShyRules.FALSE_ALARM_TARGETS_REQUIRED));
        assertFalse(CameraShyRules.insidePhotoCone(new Vector(1, 0, 0), new Vector(1, 0.35D, 0),
                CameraShyRules.doubleSetting("DECOY_LOOK_MIN_DOT", CameraShyRules.DECOY_LOOK_MIN_DOT)));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(55_000L, CameraShyRules.longSetting("SPOTLIGHT_INTERVAL_MILLIS",
                CameraShyRules.SPOTLIGHT_INTERVAL_MILLIS));
        assertEquals(25_000L, CameraShyRules.longSetting("DECOY_ABILITY_COOLDOWN_MILLIS",
                CameraShyRules.DECOY_ABILITY_COOLDOWN_MILLIS));
        assertEquals(18.0D, CameraShyRules.doubleSetting("SPOTTER_RANGE_BLOCKS",
                CameraShyRules.SPOTTER_RANGE_BLOCKS));
        assertEquals(3, CameraShyRules.intSetting("FALSE_ALARM_TARGETS_REQUIRED",
                CameraShyRules.FALSE_ALARM_TARGETS_REQUIRED));
    }

    @Test
    void groupPhotoRequiresASecondPlayerInsideTheCameraCone() {
        assertTrue(CameraShyRules.insidePhotoCone(new Vector(1, 0, 0), new Vector(5, 0, 0), 0.985D));
        assertFalse(CameraShyRules.insidePhotoCone(new Vector(1, 0, 0), new Vector(0, 0, 5), 0.985D));
    }
}
