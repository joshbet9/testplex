package au.com.testplex.arcadeports;

/** Shared source constants and score/role calculations for Milk the Cow. */
public final class MilkCowRules {
    public record ChargeKnockback(double horizontal, double vertical) { }
    public static final int SCORE_TO_WIN = 15;
    public static final int SCORE_LOST_ON_DEATH = 2;
    public static final int SCORE_PER_MILK = 1;
    public static final int SCORE_PER_COW_KILL = 1;
    public static final double COW_TEAM_RATIO = 0.20D;
    public static final int HERD_SIZE = 5;
    public static final double HERD_MOVE_SPEED = 1.4D;
    public static final long COW_CHARGE_HUNGER_INTERVAL_MILLIS = 250L;
    public static final double COW_CHARGE_TARGET_RADIUS_BLOCKS = 2.0D;
    public static final double COW_CHARGE_TARGET_HEIGHT_BLOCKS = 1.6D;
    public static final long COW_CHARGE_TARGET_COOLDOWN_MILLIS = 500L;
    public static final double COW_CHARGE_DAMAGE = 3.0D;
    public static final double COW_CHARGE_KNOCKBACK_BASE = 1.0D;
    public static final double COW_CHARGE_KNOCKBACK_BONUS = 0.8D;
    public static final double COW_CHARGE_VERTICAL_BASE = 0.8D;
    public static final double COW_CHARGE_VERTICAL_BONUS = 0.4D;
    public static final long HERD_UPDATE_INTERVAL_MILLIS = 1_000L;
    public static final double HERD_FOLLOW_MIN_DISTANCE_SQUARED = 2.25D;
    public static final double HERD_SPAWN_RADIUS_BLOCKS = 2.5D;
    public static final double FARMER_HEAL_PER_MILK_HP = 2.0D;
    public static final int COW_SPRINT_SPEED_DURATION_TICKS = 4;
    public static final int COW_SPRINT_SPEED_AMPLIFIER = 1;

    private MilkCowRules() { }

    /** Keep at least one player on each side when the lobby has two or more players. */
    public static int cowCountForPlayers(int playerCount) {
        if (playerCount <= 1) return 0;
        return Math.max(1, Math.min(playerCount - 1,
                (int) Math.round(playerCount * GameDebugRuntimeOverrides.doubleValue(
                        "MILK_THE_COW_COW_TEAM_RATIO", COW_TEAM_RATIO))));
    }

    public static int scoreAfterChange(int currentScore, int change) {
        return Math.max(0, currentScore + change);
    }

    public static boolean hasWon(int score) {
        return score >= GameDebugRuntimeOverrides.intValue("MILK_THE_COW_SCORE_TO_WIN", SCORE_TO_WIN);
    }

    public static long cowChargeHungerIntervalMillis() {
        return Math.max(1L, GameDebugRuntimeOverrides.longValue(
                "MILK_THE_COW_COW_CHARGE_HUNGER_INTERVAL_MILLIS", COW_CHARGE_HUNGER_INTERVAL_MILLIS));
    }

    public static double cowChargeTargetRadiusBlocks() {
        return Math.max(0.1D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_COW_CHARGE_TARGET_RADIUS_BLOCKS", COW_CHARGE_TARGET_RADIUS_BLOCKS));
    }

    public static double cowChargeTargetHeightBlocks() {
        return Math.max(0.1D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_COW_CHARGE_TARGET_HEIGHT_BLOCKS", COW_CHARGE_TARGET_HEIGHT_BLOCKS));
    }

    public static long cowChargeTargetCooldownMillis() {
        return Math.max(0L, GameDebugRuntimeOverrides.longValue(
                "MILK_THE_COW_COW_CHARGE_TARGET_COOLDOWN_MILLIS", COW_CHARGE_TARGET_COOLDOWN_MILLIS));
    }

    public static double cowChargeDamage() {
        return Math.max(0.0D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_COW_CHARGE_DAMAGE", COW_CHARGE_DAMAGE));
    }

    public static double cowChargeKnockbackBase() {
        return Math.max(0.0D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_COW_CHARGE_KNOCKBACK_BASE", COW_CHARGE_KNOCKBACK_BASE));
    }

    public static double cowChargeKnockbackBonus() {
        return Math.max(0.0D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_COW_CHARGE_KNOCKBACK_BONUS", COW_CHARGE_KNOCKBACK_BONUS));
    }

    public static double cowChargeVerticalBase() {
        return Math.max(0.0D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_COW_CHARGE_VERTICAL_BASE", COW_CHARGE_VERTICAL_BASE));
    }

    public static double cowChargeVerticalBonus() {
        return Math.max(0.0D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_COW_CHARGE_VERTICAL_BONUS", COW_CHARGE_VERTICAL_BONUS));
    }

    public static ChargeKnockback cowChargeKnockbackAtDistance(double distanceFromForwardCenter) {
        double closeness = Math.max(0.0D, Math.min(1.0D,
                1.0D - Math.max(0.0D, distanceFromForwardCenter) / cowChargeTargetRadiusBlocks()));
        return new ChargeKnockback(cowChargeKnockbackBase() + cowChargeKnockbackBonus() * closeness,
                cowChargeVerticalBase() + cowChargeVerticalBonus() * closeness);
    }

    public static long herdUpdateIntervalMillis() {
        return Math.max(1L, GameDebugRuntimeOverrides.longValue(
                "MILK_THE_COW_HERD_UPDATE_INTERVAL_MILLIS", HERD_UPDATE_INTERVAL_MILLIS));
    }

    public static double herdFollowMinDistanceSquared() {
        return Math.max(0.0D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_HERD_FOLLOW_MIN_DISTANCE_SQUARED", HERD_FOLLOW_MIN_DISTANCE_SQUARED));
    }

    public static double herdSpawnRadiusBlocks() {
        return Math.max(0.0D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_HERD_SPAWN_RADIUS_BLOCKS", HERD_SPAWN_RADIUS_BLOCKS));
    }

    public static double farmerHealPerMilkHp() {
        return Math.max(0.0D, GameDebugRuntimeOverrides.doubleValue(
                "MILK_THE_COW_FARMER_HEAL_PER_MILK_HP", FARMER_HEAL_PER_MILK_HP));
    }

    public static int scorePerCowKill() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue(
                "MILK_THE_COW_SCORE_PER_COW_KILL", SCORE_PER_COW_KILL));
    }

    public static int cowSprintSpeedDurationTicks() {
        return Math.max(1, GameDebugRuntimeOverrides.intValue(
                "MILK_THE_COW_COW_SPRINT_SPEED_DURATION_TICKS", COW_SPRINT_SPEED_DURATION_TICKS));
    }

    public static int cowSprintSpeedAmplifier() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue(
                "MILK_THE_COW_COW_SPRINT_SPEED_AMPLIFIER", COW_SPRINT_SPEED_AMPLIFIER));
    }
}
