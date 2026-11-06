package au.com.testplex.arcadeports;

/** Shared, deterministic rules for the Super Sploor territory shooter. */
public final class SuperSploorRules {
    public static final int MAX_INK = 100;
    public static final int INK_PER_PRIMARY_SHOT = 4;
    public static final long PRIMARY_SHOT_INTERVAL_MILLIS = 200L;
    public static final int ROLLER_PRIMARY_INK_COST = 4;
    public static final long ROLLER_PRIMARY_INTERVAL_MILLIS = 550L;
    public static final int CHARGER_PRIMARY_INK_COST = 18;
    public static final long CHARGER_PRIMARY_INTERVAL_MILLIS = 1_000L;
    public static final int LOBBER_PRIMARY_INK_COST = 14;
    public static final long LOBBER_PRIMARY_INTERVAL_MILLIS = 1_100L;
    public static final int DUALIES_PRIMARY_INK_COST = 4;
    public static final long DUALIES_PRIMARY_INTERVAL_MILLIS = 350L;
    public static final int SPRAYER_SECONDARY_INK_COST = 25;
    public static final int ROLLER_SECONDARY_INK_COST = 30;
    public static final int CHARGER_SECONDARY_INK_COST = 25;
    public static final int LOBBER_SECONDARY_INK_COST = 35;
    public static final int DUALIES_SECONDARY_INK_COST = 30;
    public static final long SECONDARY_COOLDOWN_MILLIS = 4_000L;
    public static final long SPLAT_RESPAWN_MILLIS = 2_500L;
    public static final long SPAWN_PROTECTION_MILLIS = 2_000L;
    public static final long MARK_RECOVERY_DELAY_MILLIS = 6_000L;
    public static final long MARK_RECOVERY_INTERVAL_MILLIS = 2_000L;
    public static final int MAX_PAINT_MARKS = 4;
    public static final int NORMAL_INK_REGEN_PER_SECOND = 6;
    public static final int SQUID_INK_REGEN_PER_SECOND = 30;
    public static final long INK_REGEN_INTERVAL_MILLIS = 1_000L;
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
    public static final double SPECIAL_CHARGE_REQUIRED = 100.0D;
    public static final long ROLLER_MINE_ARM_MILLIS = 800L;
    public static final long ROLLER_MINE_LIFETIME_MILLIS = 8_000L;
    public static final double ROLLER_MINE_TRIGGER_RADIUS_BLOCKS = 1.5D;
    public static final int ROLLER_MINE_PAINT_RADIUS_BLOCKS = 2;
    public static final long ROLLER_STAMPEDE_DURATION_MILLIS = 4_000L;
    public static final long ROLLER_STAMPEDE_PAINT_INTERVAL_MILLIS = 150L;
    public static final double ROLLER_STAMPEDE_SPEED = 0.28D;
    public static final double ROLLER_STAMPEDE_HIT_REACH_BLOCKS = 3.2D;
    public static final double ROLLER_STAMPEDE_HIT_WIDTH_BLOCKS = 2.0D;
    public static final long CHARGER_TRACKER_DURATION_MILLIS = 4_000L;
    public static final long CHARGER_TRACKER_PULSE_MILLIS = 400L;
    public static final double CHARGER_TRACKER_RADIUS_BLOCKS = 18.0D;
    public static final long LOBBER_SPRINKLER_DURATION_MILLIS = 6_000L;
    public static final long LOBBER_SPRINKLER_PULSE_MILLIS = 650L;
    public static final int LOBBER_SPRINKLER_PAINT_RADIUS_BLOCKS = 2;
    public static final double LOBBER_SPRINKLER_HIT_RADIUS_BLOCKS = 2.0D;
    public static final long LOBBER_PAINTSTORM_DURATION_MILLIS = 2_000L;
    public static final long LOBBER_PAINTSTORM_PULSE_MILLIS = 400L;
    public static final int LOBBER_PAINTSTORM_PULSES = 4;
    public static final int LOBBER_PAINTSTORM_PAINT_RADIUS_BLOCKS = 3;
    public static final double LOBBER_PAINTSTORM_HIT_RADIUS_BLOCKS = 4.0D;
    public static final long DUALIES_SUCTION_BOMB_FUSE_MILLIS = 1_400L;
    public static final int DUALIES_SUCTION_BOMB_PAINT_RADIUS_BLOCKS = 2;
    public static final int SPRAYER_BURST_BOMB_PAINT_RADIUS_BLOCKS = 3;
    public static final int SPRAYER_COLOURBURST_PAINT_RADIUS_BLOCKS = 5;
    public static final long DUALIES_AFTERIMAGE_DASH_INTERVAL_MILLIS = 260L;
    public static final int DUALIES_AFTERIMAGE_DASHES = 3;
    public static final double DUALIES_AFTERIMAGE_DISTANCE_BLOCKS = 1.2D;
    public static final double DUALIES_AFTERIMAGE_VERTICAL_VELOCITY = 0.16D;
    public static final int CHARGER_PRISM_BEAM_STEPS = 56;
    public static final double CHARGER_PRISM_BEAM_STEP_BLOCKS = 0.5D;
    public static final double CHARGER_PRISM_BEAM_HIT_RADIUS_BLOCKS = Math.sqrt(1.2D);
    public static final long CHARGER_FULL_SHOT_COOLDOWN_MILLIS = 1_000L;
    public static final double OVERTIME_LEAD_PERCENT = 1.0D;
    public static final boolean SQUID_MODE_ENABLED = true;
    public static final boolean INK_REGEN_ENABLED = true;
    public static final boolean SECONDARIES_ENABLED = true;
    public static final boolean SPECIALS_ENABLED = true;
    public static final double SPRAYER_PRIMARY_PROJECTILE_SPEED = 1.85D;
    public static final double LOBBER_PRIMARY_PROJECTILE_SPEED = 1.15D;
    public static final double LOBBER_PRIMARY_VERTICAL_ARC = 0.16D;
    public static final double DUALIES_PRIMARY_PROJECTILE_SPEED = 1.45D;
    public static final double DUALIES_PRIMARY_SPREAD_RADIANS = 0.075D;
    public static final int DUALIES_PRIMARY_PROJECTILES = 2;
    public static final int SPRAYER_PRIMARY_PAINT_RADIUS_BLOCKS = 2;
    public static final int CHARGER_PRIMARY_PAINT_RADIUS_BLOCKS = 0;
    public static final int LOBBER_PRIMARY_PAINT_RADIUS_BLOCKS = 3;
    public static final int DUALIES_PRIMARY_PAINT_RADIUS_BLOCKS = 1;
    public static final int ROLLER_PRIMARY_PAINT_RADIUS_BLOCKS = 1;
    public static final int ROLLER_PRIMARY_PAINT_STEPS = 5;
    public static final double ROLLER_PRIMARY_HIT_REACH_BLOCKS = 4.5D;
    public static final double ROLLER_PRIMARY_HIT_WIDTH_BLOCKS = 1.25D;

    private SuperSploorRules() { }

    public static int debugInt(String suffix, int fallback) {
        return GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_" + suffix, fallback);
    }

    public static long debugLong(String suffix, long fallback) {
        return GameDebugRuntimeOverrides.longValue("SUPER_SPLOOR_" + suffix, fallback);
    }

    public static double debugDouble(String suffix, double fallback) {
        return GameDebugRuntimeOverrides.doubleValue("SUPER_SPLOOR_" + suffix, fallback);
    }

    public static int primaryInkCost(String kit) {
        return switch (kit == null ? "sprayer" : kit) {
            case "roller" -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_ROLLER_PRIMARY_INK_COST", ROLLER_PRIMARY_INK_COST);
            case "charger" -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_CHARGER_PRIMARY_INK_COST", CHARGER_PRIMARY_INK_COST);
            case "lobber" -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_LOBBER_PRIMARY_INK_COST", LOBBER_PRIMARY_INK_COST);
            case "dualies" -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_DUALIES_PRIMARY_INK_COST", DUALIES_PRIMARY_INK_COST);
            default -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_INK_PER_PRIMARY_SHOT", INK_PER_PRIMARY_SHOT);
        };
    }

    public static long primaryIntervalMillis(String kit) {
        return switch (kit == null ? "sprayer" : kit) {
            case "roller" -> GameDebugRuntimeOverrides.longValue("SUPER_SPLOOR_ROLLER_PRIMARY_INTERVAL_MILLIS", ROLLER_PRIMARY_INTERVAL_MILLIS);
            case "charger" -> GameDebugRuntimeOverrides.longValue("SUPER_SPLOOR_CHARGER_PRIMARY_INTERVAL_MILLIS", CHARGER_PRIMARY_INTERVAL_MILLIS);
            case "lobber" -> GameDebugRuntimeOverrides.longValue("SUPER_SPLOOR_LOBBER_PRIMARY_INTERVAL_MILLIS", LOBBER_PRIMARY_INTERVAL_MILLIS);
            case "dualies" -> GameDebugRuntimeOverrides.longValue("SUPER_SPLOOR_DUALIES_PRIMARY_INTERVAL_MILLIS", DUALIES_PRIMARY_INTERVAL_MILLIS);
            default -> GameDebugRuntimeOverrides.longValue("SUPER_SPLOOR_PRIMARY_SHOT_INTERVAL_MILLIS", PRIMARY_SHOT_INTERVAL_MILLIS);
        };
    }

    public static int secondaryInkCost(String kit) {
        return switch (kit == null ? "sprayer" : kit) {
            case "roller" -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_ROLLER_SECONDARY_INK_COST", ROLLER_SECONDARY_INK_COST);
            case "charger" -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_CHARGER_SECONDARY_INK_COST", CHARGER_SECONDARY_INK_COST);
            case "lobber" -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_LOBBER_SECONDARY_INK_COST", LOBBER_SECONDARY_INK_COST);
            case "dualies" -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_DUALIES_SECONDARY_INK_COST", DUALIES_SECONDARY_INK_COST);
            default -> GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_SPRAYER_SECONDARY_INK_COST", SPRAYER_SECONDARY_INK_COST);
        };
    }

    public static long secondaryCooldownMillis() {
        return GameDebugRuntimeOverrides.longValue("SUPER_SPLOOR_SECONDARY_COOLDOWN_MILLIS", SECONDARY_COOLDOWN_MILLIS);
    }

    public static double chargerChargeFraction(long elapsedMillis) {
        long fullChargeMillis = GameDebugRuntimeOverrides.longValue(
                "SUPER_SPLOOR_CHARGER_FULL_CHARGE_MILLIS", CHARGER_FULL_CHARGE_MILLIS);
        return Math.max(0.2D, Math.min(1.0D,
                Math.max(0L, elapsedMillis) / (double) Math.max(1L, fullChargeMillis)));
    }

    public static int chargerInkCost(double chargeFraction) {
        double fraction = Double.isFinite(chargeFraction)
                ? Math.max(0.2D, Math.min(1.0D, chargeFraction)) : 0.2D;
        int minimum = Math.max(0, GameDebugRuntimeOverrides.intValue(
                "SUPER_SPLOOR_CHARGER_MIN_INK_COST", CHARGER_MIN_INK_COST));
        int maximum = Math.max(minimum, GameDebugRuntimeOverrides.intValue(
                "SUPER_SPLOOR_CHARGER_MAX_INK_COST", CHARGER_MAX_INK_COST));
        return minimum + (int) Math.ceil((maximum - minimum) * fraction);
    }

    public static double chargerProjectileSpeed(double chargeFraction) {
        double fraction = Double.isFinite(chargeFraction)
                ? Math.max(0.2D, Math.min(1.0D, chargeFraction)) : 0.2D;
        double minimum = GameDebugRuntimeOverrides.doubleValue(
                "SUPER_SPLOOR_CHARGER_MIN_SPEED", CHARGER_MIN_SPEED);
        double maximum = Math.max(minimum, GameDebugRuntimeOverrides.doubleValue(
                "SUPER_SPLOOR_CHARGER_MAX_SPEED", CHARGER_MAX_SPEED));
        return minimum + (maximum - minimum) * fraction;
    }

    public static double nextSpecialCharge(double current, int changedTerritoryUnits,
            boolean suppressedDuringSpecial) {
        double safeCurrent = Double.isFinite(current) ? Math.max(0.0D, Math.min(100.0D, current)) : 0.0D;
        if (suppressedDuringSpecial || changedTerritoryUnits <= 0) return safeCurrent;
        double required = specialChargeRequired();
        double perUnit = GameDebugRuntimeOverrides.doubleValue(
                "SUPER_SPLOOR_SPECIAL_CHARGE_PER_TERRITORY_UNIT", SPECIAL_CHARGE_PER_TERRITORY_UNIT);
        return Math.min(required, safeCurrent + changedTerritoryUnits * perUnit);
    }

    public static double specialChargeAfterSplat(double current) {
        double safeCurrent = Double.isFinite(current) ? Math.max(0.0D, Math.min(100.0D, current)) : 0.0D;
        double required = specialChargeRequired();
        return Math.min(required, safeCurrent + GameDebugRuntimeOverrides.doubleValue(
                "SUPER_SPLOOR_SPECIAL_CHARGE_PER_SPLAT", SPECIAL_CHARGE_PER_SPLAT));
    }

    private static double specialChargeRequired() {
        return Math.max(1.0D, GameDebugRuntimeOverrides.doubleValue(
                "SUPER_SPLOOR_SPECIAL_CHARGE_REQUIRED", SPECIAL_CHARGE_REQUIRED));
    }

    public static boolean canUseSpecial(double charge) {
        return Double.isFinite(charge) && charge >= specialChargeRequired();
    }

    public static int dodgeRollChargesAfterUse(int current) {
        return Math.max(0, Math.min(GameDebugRuntimeOverrides.intValue(
                "SUPER_SPLOOR_DODGE_ROLL_MAX_CHARGES", DODGE_ROLL_MAX_CHARGES), current) - 1);
    }

    public static int nextDodgeRollCharge(int current) {
        return Math.min(GameDebugRuntimeOverrides.intValue(
                "SUPER_SPLOOR_DODGE_ROLL_MAX_CHARGES", DODGE_ROLL_MAX_CHARGES), Math.max(0, current) + 1);
    }

    public static int clampInk(int ink) {
        return Math.max(0, Math.min(GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_MAX_INK", MAX_INK), ink));
    }

    public static int spendInk(int current, int cost) {
        if (cost < 0 || current < cost) return clampInk(current);
        return clampInk(current - cost);
    }

    public static boolean canFire(int ink, long nowMillis, long lastShotAtMillis) {
        return ink >= GameDebugRuntimeOverrides.intValue(
                "SUPER_SPLOOR_INK_PER_PRIMARY_SHOT", INK_PER_PRIMARY_SHOT)
                && nowMillis >= lastShotAtMillis
                && nowMillis - lastShotAtMillis >= GameDebugRuntimeOverrides.longValue(
                        "SUPER_SPLOOR_PRIMARY_SHOT_INTERVAL_MILLIS", PRIMARY_SHOT_INTERVAL_MILLIS);
    }

    /** 0 = no change, 1 = neutral claim, 2 = enemy reclaim. */
    public static int territoryPaintTransition(int currentOwner, int attackingTeam) {
        if (attackingTeam != 0 && attackingTeam != 1) return 0;
        if (currentOwner == attackingTeam) return 0;
        if (currentOwner == -1) return 1;
        return currentOwner == 1 - attackingTeam ? 2 : 0;
    }

    public static int markCountAfterHit(int currentMarks) {
        return Math.min(GameDebugRuntimeOverrides.intValue(
                "SUPER_SPLOOR_MAX_PAINT_MARKS", MAX_PAINT_MARKS), Math.max(0, currentMarks) + 1);
    }

    public static boolean isSplatted(int marks) {
        return marks >= GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_MAX_PAINT_MARKS", MAX_PAINT_MARKS);
    }

    public static boolean canRecoverMark(long nowMillis, long lastHitAtMillis,
            long nextRecoveryAtMillis, boolean standingOnFriendlyPaint, int currentMarks) {
        return standingOnFriendlyPaint && currentMarks > 0
                && nowMillis >= lastHitAtMillis + GameDebugRuntimeOverrides.longValue(
                        "SUPER_SPLOOR_MARK_RECOVERY_DELAY_MILLIS", MARK_RECOVERY_DELAY_MILLIS)
                && nowMillis >= nextRecoveryAtMillis;
    }

    public static int nextInkAfterRegeneration(int current, int seconds, boolean squidMode) {
        int rate = squidMode
                ? GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_SQUID_INK_REGEN_PER_SECOND", SQUID_INK_REGEN_PER_SECOND)
                : GameDebugRuntimeOverrides.intValue("SUPER_SPLOOR_NORMAL_INK_REGEN_PER_SECOND", NORMAL_INK_REGEN_PER_SECOND);
        return clampInk(current + Math.max(0, seconds) * rate);
    }

    public static boolean canRegenerateInk(long nowMillis, long lastInkUseAtMillis,
            boolean squidMode, boolean standingOnFriendlyPaint) {
        if (squidMode && standingOnFriendlyPaint) return true;
        return nowMillis >= lastInkUseAtMillis && nowMillis - lastInkUseAtMillis
                >= GameDebugRuntimeOverrides.longValue(
                        "SUPER_SPLOOR_INK_REGEN_INTERVAL_MILLIS", INK_REGEN_INTERVAL_MILLIS);
    }

    public static int overtimeLeadThreshold(int totalTerritoryUnits) {
        long total = Math.max(0L, (long) totalTerritoryUnits);
        double percent = Math.max(0.01D, GameDebugRuntimeOverrides.doubleValue(
                "SUPER_SPLOOR_OVERTIME_LEAD_PERCENT", OVERTIME_LEAD_PERCENT));
        return (int) Math.max(1L, Math.ceil(total * percent / 100.0D));
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
