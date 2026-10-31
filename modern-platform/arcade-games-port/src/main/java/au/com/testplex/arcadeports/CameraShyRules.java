package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.UUID;

/** Deterministic rules shared by the Camera Shy match controller and its tests. */
public final class CameraShyRules {
    public static final int PHOTOS_TO_ELIMINATE = 5;
    public static final int MAX_PLAYERS = 16;
    public static final int MIN_PLAYERS = 4;
    public static final int MATCH_SECONDS = 180;
    public static final double CAMERA_RANGE_BLOCKS = 24.0D;
    public static final double CLOSE_PHOTO_BLOCKS = 3.0D;
    public static final double LONG_PHOTO_BLOCKS = 18.0D;
    public static final long PHOTO_COOLDOWN_MILLIS = 1_500L;
    public static final long FINAL_SNAPSHOT_COOLDOWN_MILLIS = 1_000L;
    public static final long EXPOSURE_PROTECTION_MILLIS = 1_250L;
    public static final long DOUBLE_TAKE_WINDOW_MILLIS = 5_000L;
    public static final long DOUBLE_ELIMINATION_WINDOW_MILLIS = 15_000L;

    private CameraShyRules() { }

    /** Rebuild a dedicated arena if its persisted build flag or in-world floor marker is missing. */
    public static boolean arenaBuildRequired(boolean buildFlag, Material foundationMarker, Material floorMarker) {
        return !buildFlag || foundationMarker != Material.DEEPSLATE_TILES
                || floorMarker != Material.COARSE_DIRT;
    }

    /** Only a real opposing human may be credited for being fooled by an Understudy. */
    public static boolean shouldAwardUnderstudy(UUID owner, UUID photographer,
                                                 boolean ownerIsHuman, boolean photographerIsHuman) {
        return owner != null && photographer != null && ownerIsHuman && photographerIsHuman
                && !owner.equals(photographer);
    }

    public static boolean eliminatedAt(int exposures) {
        return exposures >= PHOTOS_TO_ELIMINATE;
    }

    public static boolean mayPhotograph(long now, long nextCameraReadyAt, long targetProtectedUntil) {
        return now >= nextCameraReadyAt && now >= targetProtectedUntil;
    }

    public static boolean isClosePhoto(double distance) {
        return distance >= 0.0D && distance <= CLOSE_PHOTO_BLOCKS;
    }

    public static boolean isLongDistancePhoto(double distance) {
        return distance >= LONG_PHOTO_BLOCKS && distance <= CAMERA_RANGE_BLOCKS;
    }

    public static boolean withinWindow(long now, long previous, long windowMillis) {
        return previous >= 0L && now >= previous && now - previous <= windowMillis;
    }

    public static int consecutiveWinsAfter(int currentStreak, boolean won) {
        return won ? Math.max(0, currentStreak) + 1 : 0;
    }

    /**
     * AFK test players are target dummies, not eligible winners. A solo human test
     * continues while any dummies remain, but wins immediately after clearing them.
     */
    public static boolean shouldEndMatch(int aliveParticipants, int livingHumans, int totalHumans) {
        if (aliveParticipants <= 1 || livingHumans <= 0) return true;
        return totalHumans > 1 && livingHumans <= 1;
    }

    /** True when a smoke sphere intersects the finite segment from camera to target. */
    public static boolean segmentIntersectsSphere(Vector start, Vector end, Vector center, double radius) {
        if (start == null || end == null || center == null || radius < 0.0D) return false;
        Vector segment = end.clone().subtract(start);
        double lengthSquared = segment.lengthSquared();
        double t = lengthSquared < 1.0E-9D ? 0.0D
                : Math.max(0.0D, Math.min(1.0D, center.clone().subtract(start).dot(segment) / lengthSquared));
        Vector closest = start.clone().add(segment.multiply(t));
        return closest.distanceSquared(center) <= radius * radius;
    }

    /** A group-photo secondary must be close to the camera's centre line. */
    public static boolean insidePhotoCone(Vector cameraDirection, Vector toTarget, double minimumDot) {
        if (cameraDirection == null || toTarget == null
                || cameraDirection.lengthSquared() < 1.0E-9D || toTarget.lengthSquared() < 1.0E-9D) return false;
        return cameraDirection.clone().normalize().dot(toTarget.clone().normalize()) >= minimumDot;
    }
}
