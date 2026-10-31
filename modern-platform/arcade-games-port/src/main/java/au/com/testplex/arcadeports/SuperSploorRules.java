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
    public static final long CHARGER_FULL_CHARGE_MILLIS = 900L;
    public static final long CHARGER_RECHARGE_MILLIS = 1_000L;
    public static final int CHARGER_MIN_INK_COST = 8;
    public static final int CHARGER_MAX_INK_COST = 18;
    public static final double CHARGER_MIN_SPEED = 1.8D;
    public static final double CHARGER_MAX_SPEED = 3.0D;
    public static final int DODGE_ROLL_MAX_CHARGES = 2;
    public static final long DODGE_ROLL_RECHARGE_MILLIS = 5_000L;
    public static final double SPECIAL_CHARGE_PER_TERRITORY_UNIT = 0.10D;
    public static final double SPECIAL_CHARGE_PER_SPLAT = 5.0D;

    private SuperSploorRules() { }

    public static double chargerChargeFraction(long elapsedMillis) {
        return Math.max(0.2D, Math.min(1.0D,
                Math.max(0L, elapsedMillis) / (double) CHARGER_FULL_CHARGE_MILLIS));
    }

    public static int chargerInkCost(double chargeFraction) {
        double fraction = Double.isFinite(chargeFraction)
                ? Math.max(0.2D, Math.min(1.0D, chargeFraction)) : 0.2D;
        return CHARGER_MIN_INK_COST
                + (int) Math.ceil((CHARGER_MAX_INK_COST - CHARGER_MIN_INK_COST) * fraction);
    }

    public static double chargerProjectileSpeed(double chargeFraction) {
        double fraction = Double.isFinite(chargeFraction)
                ? Math.max(0.2D, Math.min(1.0D, chargeFraction)) : 0.2D;
        return CHARGER_MIN_SPEED + (CHARGER_MAX_SPEED - CHARGER_MIN_SPEED) * fraction;
    }

    public static double nextSpecialCharge(double current, int changedTerritoryUnits,
            boolean suppressedDuringSpecial) {
        double safeCurrent = Double.isFinite(current) ? Math.max(0.0D, Math.min(100.0D, current)) : 0.0D;
        if (suppressedDuringSpecial || changedTerritoryUnits <= 0) return safeCurrent;
        return Math.min(100.0D, safeCurrent
                + changedTerritoryUnits * SPECIAL_CHARGE_PER_TERRITORY_UNIT);
    }

    public static double specialChargeAfterSplat(double current) {
        double safeCurrent = Double.isFinite(current) ? Math.max(0.0D, Math.min(100.0D, current)) : 0.0D;
        return Math.min(100.0D, safeCurrent + SPECIAL_CHARGE_PER_SPLAT);
    }

    public static boolean canUseSpecial(double charge) {
        return Double.isFinite(charge) && charge >= 100.0D;
    }

    public static int dodgeRollChargesAfterUse(int current) {
        return Math.max(0, Math.min(DODGE_ROLL_MAX_CHARGES, current) - 1);
    }

    public static int nextDodgeRollCharge(int current) {
        return Math.min(DODGE_ROLL_MAX_CHARGES, Math.max(0, current) + 1);
    }

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

    /** 0 = no change, 1 = neutral claim, 2 = enemy reclaim. */
    public static int territoryPaintTransition(int currentOwner, int attackingTeam) {
        if (attackingTeam != 0 && attackingTeam != 1) return 0;
        if (currentOwner == attackingTeam) return 0;
        if (currentOwner == -1) return 1;
        return currentOwner == 1 - attackingTeam ? 2 : 0;
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

    public static boolean canRegenerateInk(long nowMillis, long lastInkUseAtMillis,
            boolean squidMode, boolean standingOnFriendlyPaint) {
        if (squidMode && standingOnFriendlyPaint) return true;
        return nowMillis >= lastInkUseAtMillis && nowMillis - lastInkUseAtMillis >= 1_000L;
    }

    public static int overtimeLeadThreshold(int totalTerritoryUnits) {
        long total = Math.max(0L, (long) totalTerritoryUnits);
        return (int) Math.max(1L, (total + 99L) / 100L);
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

    public static boolean isAssistEligible(long nowMillis, long lastContributionAtMillis) {
        return lastContributionAtMillis > 0L && nowMillis >= lastContributionAtMillis
                && nowMillis - lastContributionAtMillis <= 8_000L;
    }

    public static boolean canMarkTarget(int attackerTeam, int targetTeam,
            boolean targetRespawning, long nowMillis, long spawnProtectedUntil) {
        return (attackerTeam == 0 || attackerTeam == 1)
                && (targetTeam == 0 || targetTeam == 1)
                && attackerTeam != targetTeam
                && !targetRespawning
                && nowMillis >= spawnProtectedUntil;
    }

    /** Exclude the authored spawn pad and its immediate room from territory and charge. */
    public static boolean isSpawnSafeTerritoryBlock(int blockX, int blockY, int blockZ,
            int spawnX, int spawnY, int spawnZ) {
        int dx = blockX - spawnX;
        int dz = blockZ - spawnZ;
        int dy = blockY - spawnY;
        return dx * dx + dz * dz <= 9 && dy >= -2 && dy <= 3;
    }
}
