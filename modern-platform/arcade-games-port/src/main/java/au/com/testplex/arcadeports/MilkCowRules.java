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
    public static final double COW_CHARGE_IMPACT_VERTICAL = 0.45D;
    public static final long HERD_UPDATE_INTERVAL_MILLIS = 1_000L;
    public static final double HERD_FOLLOW_MIN_DISTANCE_SQUARED = 2.25D;
    public static final double HERD_SPAWN_RADIUS_BLOCKS = 2.5D;
    public static final double FARMER_HEAL_PER_MILK_HP = 2.0D;
    public static final int COW_SPRINT_SPEED_DURATION_TICKS = 4;
    public static final int COW_SPRINT_SPEED_AMPLIFIER = 1;
    public static final long COW_BOMB_COOLDOWN_MILLIS = 3_000L;
    public static final double COW_BOMB_LAUNCH_SPEED = 1.35D;
    public static final double COW_BOMB_LAUNCH_VERTICAL_SPEED = 0.28D;
    public static final long COW_BOMB_LIFETIME_MILLIS = 2_500L;
    public static final double COW_BOMB_CONTACT_RADIUS_BLOCKS = 1.1D;
    public static final double COW_BOMB_DAMAGE = 4.0D;
    public static final double COW_BOMB_KNOCKBACK = 2.5D;
    public static final double COW_BOMB_KNOCKBACK_VERTICAL = 0.8D;
    public static final long BODY_SLAM_COOLDOWN_MILLIS = 6_000L;
    public static final long BODY_SLAM_LANDING_DELAY_MILLIS = 1_000L;
    public static final double BODY_SLAM_LAUNCH_SPEED = 0.9D;
    public static final double BODY_SLAM_MIN_LAUNCH_VERTICAL = 0.65D;
    public static final double BODY_SLAM_LAUNCH_VERTICAL_BONUS = 0.35D;
    public static final double BODY_SLAM_RADIUS_BLOCKS = 6.0D;
    public static final double BODY_SLAM_HEIGHT_BLOCKS = 4.0D;
    public static final double BODY_SLAM_DAMAGE_BASE = 0.5D;
    public static final double BODY_SLAM_DAMAGE_AT_CENTER = 4.0D;
    public static final double BODY_SLAM_KNOCKBACK_HORIZONTAL = 1.8D;
    public static final double BODY_SLAM_KNOCKBACK_VERTICAL_BASE = 0.4D;
    public static final long ASSIST_CREDIT_WINDOW_MILLIS = 10_000L;
    public static final int PERFECT_GAME_MAX_FARMER_DEATHS = 0;
    public static final int COMEBACK_LEAD_THRESHOLD = 3;
    public static final int GOT_THE_CHOPS_AIRBORNE_KILLS = 10;
    public static final int SLAUGHTERHOUSE_ASSISTS = 15;
    public static final double FARMER_DOUBLE_JUMP_HORIZONTAL_SPEED = 0.8D;
    public static final double FARMER_DOUBLE_JUMP_VERTICAL_BONUS = 0.45D;
    public static final double FARMER_DOUBLE_JUMP_MIN_VERTICAL = 0.7D;
    public static final double FARMER_DOUBLE_JUMP_MAX_VERTICAL = 0.9D;

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
    public static double cowChargeImpactVertical() { return nonNegativeDouble("COW_CHARGE_IMPACT_VERTICAL", COW_CHARGE_IMPACT_VERTICAL); }

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

    public static long cowBombCooldownMillis() { return nonNegativeLong("COW_BOMB_COOLDOWN_MILLIS", COW_BOMB_COOLDOWN_MILLIS); }
    public static double cowBombLaunchSpeed() { return nonNegativeDouble("COW_BOMB_LAUNCH_SPEED", COW_BOMB_LAUNCH_SPEED); }
    public static double cowBombLaunchVerticalSpeed() { return nonNegativeDouble("COW_BOMB_LAUNCH_VERTICAL_SPEED", COW_BOMB_LAUNCH_VERTICAL_SPEED); }
    public static long cowBombLifetimeMillis() { return Math.max(1L, valueLong("COW_BOMB_LIFETIME_MILLIS", COW_BOMB_LIFETIME_MILLIS)); }
    public static double cowBombContactRadiusBlocks() { return Math.max(0.1D, valueDouble("COW_BOMB_CONTACT_RADIUS_BLOCKS", COW_BOMB_CONTACT_RADIUS_BLOCKS)); }
    public static double cowBombDamage() { return nonNegativeDouble("COW_BOMB_DAMAGE", COW_BOMB_DAMAGE); }
    public static double cowBombKnockback() { return nonNegativeDouble("COW_BOMB_KNOCKBACK", COW_BOMB_KNOCKBACK); }
    public static double cowBombKnockbackVertical() { return nonNegativeDouble("COW_BOMB_KNOCKBACK_VERTICAL", COW_BOMB_KNOCKBACK_VERTICAL); }
    public static long bodySlamCooldownMillis() { return nonNegativeLong("BODY_SLAM_COOLDOWN_MILLIS", BODY_SLAM_COOLDOWN_MILLIS); }
    public static long bodySlamLandingDelayMillis() { return Math.max(0L, valueLong("BODY_SLAM_LANDING_DELAY_MILLIS", BODY_SLAM_LANDING_DELAY_MILLIS)); }
    public static double bodySlamLaunchSpeed() { return nonNegativeDouble("BODY_SLAM_LAUNCH_SPEED", BODY_SLAM_LAUNCH_SPEED); }
    public static double bodySlamMinLaunchVertical() { return nonNegativeDouble("BODY_SLAM_MIN_LAUNCH_VERTICAL", BODY_SLAM_MIN_LAUNCH_VERTICAL); }
    public static double bodySlamLaunchVerticalBonus() { return nonNegativeDouble("BODY_SLAM_LAUNCH_VERTICAL_BONUS", BODY_SLAM_LAUNCH_VERTICAL_BONUS); }
    public static double bodySlamRadiusBlocks() { return Math.max(0.1D, valueDouble("BODY_SLAM_RADIUS_BLOCKS", BODY_SLAM_RADIUS_BLOCKS)); }
    public static double bodySlamHeightBlocks() { return Math.max(0.1D, valueDouble("BODY_SLAM_HEIGHT_BLOCKS", BODY_SLAM_HEIGHT_BLOCKS)); }
    public static double bodySlamDamageBase() { return nonNegativeDouble("BODY_SLAM_DAMAGE_BASE", BODY_SLAM_DAMAGE_BASE); }
    public static double bodySlamDamageAtCenter() { return nonNegativeDouble("BODY_SLAM_DAMAGE_AT_CENTER", BODY_SLAM_DAMAGE_AT_CENTER); }
    public static double bodySlamKnockbackHorizontal() { return nonNegativeDouble("BODY_SLAM_KNOCKBACK_HORIZONTAL", BODY_SLAM_KNOCKBACK_HORIZONTAL); }
    public static double bodySlamKnockbackVerticalBase() { return nonNegativeDouble("BODY_SLAM_KNOCKBACK_VERTICAL_BASE", BODY_SLAM_KNOCKBACK_VERTICAL_BASE); }
    public static long assistCreditWindowMillis() { return Math.max(0L, valueLong("ASSIST_CREDIT_WINDOW_MILLIS", ASSIST_CREDIT_WINDOW_MILLIS)); }
    public static int perfectGameMaxFarmerDeaths() { return Math.max(0, valueInt("PERFECT_GAME_MAX_FARMER_DEATHS", PERFECT_GAME_MAX_FARMER_DEATHS)); }
    public static int comebackLeadThreshold() { return Math.max(1, valueInt("COMEBACK_LEAD_THRESHOLD", COMEBACK_LEAD_THRESHOLD)); }
    public static int gotTheChopsAirborneKills() { return Math.max(1, valueInt("GOT_THE_CHOPS_AIRBORNE_KILLS", GOT_THE_CHOPS_AIRBORNE_KILLS)); }
    public static int slaughterhouseAssists() { return Math.max(1, valueInt("SLAUGHTERHOUSE_ASSISTS", SLAUGHTERHOUSE_ASSISTS)); }
    public static double farmerDoubleJumpHorizontalSpeed() { return nonNegativeDouble("FARMER_DOUBLE_JUMP_HORIZONTAL_SPEED", FARMER_DOUBLE_JUMP_HORIZONTAL_SPEED); }
    public static double farmerDoubleJumpVerticalBonus() { return nonNegativeDouble("FARMER_DOUBLE_JUMP_VERTICAL_BONUS", FARMER_DOUBLE_JUMP_VERTICAL_BONUS); }
    public static double farmerDoubleJumpMinVertical() { return nonNegativeDouble("FARMER_DOUBLE_JUMP_MIN_VERTICAL", FARMER_DOUBLE_JUMP_MIN_VERTICAL); }
    public static double farmerDoubleJumpMaxVertical() { return Math.max(farmerDoubleJumpMinVertical(), valueDouble("FARMER_DOUBLE_JUMP_MAX_VERTICAL", FARMER_DOUBLE_JUMP_MAX_VERTICAL)); }
    public static double bodySlamDamageAtDistance(double distance) {
        return bodySlamDamageAtCenter() * bodySlamScaleAtDistance(distance) + bodySlamDamageBase();
    }
    public static double bodySlamKnockbackHorizontalAtDistance(double distance) {
        return bodySlamKnockbackHorizontal() * bodySlamScaleAtDistance(distance);
    }
    public static double bodySlamKnockbackVerticalAtDistance(double distance) {
        return bodySlamKnockbackVerticalBase() + bodySlamScaleAtDistance(distance);
    }
    private static double bodySlamScaleAtDistance(double distance) {
        return Math.max(0.0D, 1.0D - Math.max(0.0D, distance) / bodySlamRadiusBlocks());
    }

    private static String key(String suffix) { return "MILK_THE_COW_" + suffix; }
    private static long valueLong(String suffix, long fallback) { return GameDebugRuntimeOverrides.longValue(key(suffix), fallback); }
    private static int valueInt(String suffix, int fallback) { return GameDebugRuntimeOverrides.intValue(key(suffix), fallback); }
    private static double valueDouble(String suffix, double fallback) { return GameDebugRuntimeOverrides.doubleValue(key(suffix), fallback); }
    private static long nonNegativeLong(String suffix, long fallback) { return Math.max(0L, valueLong(suffix, fallback)); }
    private static double nonNegativeDouble(String suffix, double fallback) { return Math.max(0.0D, valueDouble(suffix, fallback)); }
}
