package au.com.testplex.arcadeports;

/** Shared, deterministic rules for the Super Sploor territory shooter. */
public final class SuperSploorRules {
    public static final int MAX_INK = 100;
    public static final int INK_PER_PRIMARY_SHOT = 4;
    public static final long PRIMARY_SHOT_INTERVAL_MILLIS = 200L;
    public static final long SPLAT_RESPAWN_MILLIS = 2_500L;
    public static final long SPAWN_PROTECTION_MILLIS = 2_000L;
    public static final long MARK_RECOVERY_DELAY_MILLIS = 6_000L;
    public static final long MARK_RECOVERY_INTERVAL_MILLIS = 2_000L;
    public static final int MAX_PAINT_MARKS = 4;
    public static final int NORMAL_INK_REGEN_PER_SECOND = 6;
    public static final int SQUID_INK_REGEN_PER_SECOND = 30;
    public static final double SQUID_WALK_SPEED = 0.34D;
    public static final double SQUID_CLIMB_VELOCITY = 0.30D;
    public static final int OVERTIME_MILLIS = 30_000;
    public static final int ROUND_MILLIS = 180_000;

    private SuperSploorRules() { }

    public static int clampInk(int ink) {
        return Math.max(0, Math.min(MAX_INK, ink));
    }

    public static int spendInk(int current, int cost) {
        if (cost < 0 || current < cost) return clampInk(current);
        return clampInk(current - cost);
    }

    public static boolean canFire(int ink, long nowMillis, long lastShotAtMillis) {
        return ink >= INK_PER_PRIMARY_SHOT && nowMillis >= lastShotAtMillis
                && nowMillis - lastShotAtMillis >= PRIMARY_SHOT_INTERVAL_MILLIS;
    }

    public static int markCountAfterHit(int currentMarks) {
        return Math.min(MAX_PAINT_MARKS, Math.max(0, currentMarks) + 1);
    }

    public static boolean isSplatted(int marks) {
        return marks >= MAX_PAINT_MARKS;
    }

    public static boolean canRecoverMark(long nowMillis, long lastHitAtMillis,
            long nextRecoveryAtMillis, boolean standingOnFriendlyPaint, int currentMarks) {
        return standingOnFriendlyPaint && currentMarks > 0
                && nowMillis >= lastHitAtMillis + MARK_RECOVERY_DELAY_MILLIS
                && nowMillis >= nextRecoveryAtMillis;
    }

    public static int nextInkAfterRegeneration(int current, int seconds, boolean squidMode) {
        int rate = squidMode ? SQUID_INK_REGEN_PER_SECOND : NORMAL_INK_REGEN_PER_SECOND;
        return clampInk(current + Math.max(0, seconds) * rate);
    }

    public static int overtimeLeadThreshold(int totalTerritoryUnits) {
        return Math.max(1, Math.max(0, totalTerritoryUnits) / 100);
    }

    /** Returns 0 for Red, 1 for Blue, and -1 for a tied territory score. */
    public static int leadingTeam(int redUnits, int blueUnits) {
        return redUnits == blueUnits ? -1 : redUnits > blueUnits ? 0 : 1;
    }

    public static boolean hasDecisiveOvertimeLead(int redUnits, int blueUnits, int totalTerritoryUnits) {
        return Math.abs(redUnits - blueUnits) >= overtimeLeadThreshold(totalTerritoryUnits);
    }

    /** Reject teleports and implausible corrections when measuring Squid travel. */
    public static boolean shouldCountSquidTravel(double horizontalDistance, boolean teleport) {
        return !teleport && Double.isFinite(horizontalDistance)
                && horizontalDistance > 0.0D && horizontalDistance <= 2.0D;
    }
}
