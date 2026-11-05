package au.com.testplex.arcadeports;

import java.util.Locale;
import java.util.Set;
import org.bukkit.util.Vector;

/** Pure source-contract rules for Wither Assault's Human kit selection and Editor limits. */
public final class WitherAssaultRules {
    public static final int MINIONS_PER_WAVE = 3;
    public static final long MINION_COOLDOWN_MILLIS = 10_000L;
    public static final long MINION_LIFETIME_MILLIS = 15_000L;
    public static final long SCENT_COOLDOWN_MILLIS = 30_000L;
    public static final long LEAP_COOLDOWN_MILLIS = 6_000L;
    public static final long MEDIC_REFILL_INTERVAL_MILLIS = 45_000L;
    public static final long FLETCHER_ARROW_REFILL_MILLIS = 4_000L;
    public static final long SKULL_ATTACK_COOLDOWN_MILLIS = 2_000L;
    public static final int SMOKE_ARROW_MAX_LIFETIME_TICKS = 120;
    public static final int SMOKE_ARROW_BLINDNESS_TICKS = 80;
    public static final double SMOKE_ARROW_BLIND_RADIUS_BLOCKS = 6.0D;
    public static final int EDITOR_BUILD_DEPTH_BELOW_MARKER = 4;
    public static final double MINION_LAUNCH_SPEED = 1.1D;
    /** Zero preserves the source behavior of targeting any living Human. */
    public static final double MINION_TARGET_RANGE_BLOCKS = 0.0D;
    public static final int MINION_SPEED_AMPLIFIER = 0;
    public static final int MINION_SPEED_DURATION_TICKS = 300;
    public static final double FIRE_STORM_CHARGE_PER_TICK = 0.05D;
    public static final double FIRE_STORM_DRAIN_PER_TICK = 0.05D;
    public static final int FIRE_STORM_ARROWS_PER_TICK = 2;
    public static final double FIRE_STORM_ARROW_SPEED = 2.0D;
    public static final double FIRE_STORM_SPREAD = 0.04D;
    public static final double FLIGHT_MIN_SPEED = 0.01D;
    public static final double FLIGHT_SPAWN_HOVER_SPEED = 0.016D;
    public static final double FLIGHT_MAX_SPEED = 1.0D;
    public static final double FLIGHT_BASE_SPEED = 0.09D;
    public static final double FLIGHT_SPEED_PER_Y_BLOCK = 0.006D;
    public static final double FLIGHT_BLEND_START_BLOCKS = 2.0D;
    public static final double FLIGHT_BLEND_END_BLOCKS = 10.0D;
    public static final double FLIGHT_MAX_SPEED_CHANGE_PER_TICK = 0.0035D;
    public static final double HUMAN_ARROW_DAMAGE_MULTIPLIER = 0.75D;
    public static final double SKULL_BLAST_RADIUS_BLOCKS = 7.0D;
    public static final double SKULL_MIN_DAMAGE = 2.0D;
    public static final double SKULL_MAX_DAMAGE = 12.0D;
    public static final double SKULL_KNOCKBACK_POWER = 1.25D;
    public static final double SKULL_KNOCKBACK_Y_MINIMUM = 0.3D;
    public static final double SKULL_KNOCKBACK_Y_MULTIPLIER = 0.65D;
    public static final int SKULL_TERRAIN_RADIUS_BLOCKS = 4;
    public static final int SKULL_BLOCK_RESTORE_TICKS = 120;
    private static final Set<String> HUMAN_KITS = Set.of("archer", "medic", "editor", "warden");

    private WitherAssaultRules() { }

    /** Normalise the three Reborn Human kits; unknown kit names are rejected, not silently accepted. */
    public static String normalizeHumanKit(String requested) {
        if (requested == null || requested.isBlank()) return null;
        String value = requested.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (value.startsWith("human-")) value = value.substring("human-".length());
        return HUMAN_KITS.contains(value) ? value : null;
    }

    /**
     * The Editor may only modify blocks at or above four blocks below the
     * Wither flight-height marker, and may not break a block directly under
     * another living player.
     */
    public static boolean canEditorModifyBlock(int blockY, double witherSpawnY, boolean underOtherPlayer) {
        if (!Double.isFinite(witherSpawnY) || underOtherPlayer) return false;
        return blockY >= Math.ceil(witherSpawnY - GameDebugRuntimeOverrides.intValue(
                "WITHER_ASSAULT_EDITOR_BUILD_DEPTH_BELOW_MARKER", EDITOR_BUILD_DEPTH_BELOW_MARKER));
    }


    /** Fletcher and Smoke Arrow are shared by the source Archer and Editor kits. */
    public static boolean hasFletcher(String kit) {
        return "archer".equals(normalizeHumanKit(kit)) || "editor".equals(normalizeHumanKit(kit));
    }

    /** Only living Humans with a bow kit create tracked Smoke Arrows. */
    public static boolean shouldTrackSmokeArrow(int team, String kit, boolean alive, boolean arrowProjectile) {
        return team == 0 && alive && arrowProjectile && hasFletcher(kit);
    }
    public static boolean shouldLaunchSkull(String itemType, boolean leftClick) {
        return leftClick && "golden_sword".equals(normalizeItem(itemType));
    }

    public static boolean shouldLaunchMinions(String itemType, boolean leftClick) {
        return leftClick && "diamond_sword".equals(normalizeItem(itemType));
    }

    public static boolean shouldStartFireStorm(String itemType, boolean rightClick) {
        String item = normalizeItem(itemType);
        return rightClick && ("golden_sword".equals(item) || "diamond_sword".equals(item));
    }

    public static int minionCount() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue("WITHER_ASSAULT_MINIONS_PER_WAVE", MINIONS_PER_WAVE));
    }

    public static long minionCooldownMillis() {
        return Math.max(0L, GameDebugRuntimeOverrides.longValue("WITHER_ASSAULT_MINION_COOLDOWN_MILLIS", MINION_COOLDOWN_MILLIS));
    }

    public static long minionLifetimeMillis() {
        return Math.max(1L, GameDebugRuntimeOverrides.longValue("WITHER_ASSAULT_MINION_LIFETIME_MILLIS", MINION_LIFETIME_MILLIS));
    }

    public static long scentCooldownMillis() {
        return Math.max(0L, GameDebugRuntimeOverrides.longValue("WITHER_ASSAULT_SCENT_COOLDOWN_MILLIS", SCENT_COOLDOWN_MILLIS));
    }

    public static long leapCooldownMillis() {
        return Math.max(0L, GameDebugRuntimeOverrides.longValue("WITHER_ASSAULT_LEAP_COOLDOWN_MILLIS", LEAP_COOLDOWN_MILLIS));
    }

    public static long medicRefillIntervalMillis() {
        return Math.max(1L, GameDebugRuntimeOverrides.longValue("WITHER_ASSAULT_MEDIC_REFILL_INTERVAL_MILLIS", MEDIC_REFILL_INTERVAL_MILLIS));
    }

    public static long fletcherArrowRefillMillis() {
        return Math.max(1L, GameDebugRuntimeOverrides.longValue("WITHER_ASSAULT_FLETCHER_ARROW_REFILL_MILLIS", FLETCHER_ARROW_REFILL_MILLIS));
    }

    public static long skullAttackCooldownMillis() {
        return Math.max(0L, GameDebugRuntimeOverrides.longValue("WITHER_ASSAULT_SKULL_ATTACK_COOLDOWN_MILLIS", SKULL_ATTACK_COOLDOWN_MILLIS));
    }

    public static int smokeArrowMaxLifetimeTicks() {
        return Math.max(1, GameDebugRuntimeOverrides.intValue("WITHER_ASSAULT_SMOKE_ARROW_MAX_LIFETIME_TICKS", SMOKE_ARROW_MAX_LIFETIME_TICKS));
    }

    public static int smokeArrowBlindnessTicks() {
        return Math.max(1, GameDebugRuntimeOverrides.intValue("WITHER_ASSAULT_SMOKE_ARROW_BLINDNESS_TICKS", SMOKE_ARROW_BLINDNESS_TICKS));
    }

    public static double smokeArrowBlindRadiusBlocks() {
        double radius = GameDebugRuntimeOverrides.doubleValue("WITHER_ASSAULT_SMOKE_ARROW_BLIND_RADIUS_BLOCKS", SMOKE_ARROW_BLIND_RADIUS_BLOCKS);
        return Double.isFinite(radius) ? Math.max(0.1D, radius) : SMOKE_ARROW_BLIND_RADIUS_BLOCKS;
    }

    public static int minionSpeedAmplifier() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue(
                "WITHER_ASSAULT_MINION_SPEED_AMPLIFIER", MINION_SPEED_AMPLIFIER));
    }

    public static int minionSpeedDurationTicks() {
        return Math.max(1, GameDebugRuntimeOverrides.intValue(
                "WITHER_ASSAULT_MINION_SPEED_DURATION_TICKS", MINION_SPEED_DURATION_TICKS));
    }

    public static double minionLaunchSpeed() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_MINION_LAUNCH_SPEED", MINION_LAUNCH_SPEED), 0.0D, 4.0D, MINION_LAUNCH_SPEED);
    }

    public static double minionTargetRangeBlocks() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_MINION_TARGET_RANGE_BLOCKS", MINION_TARGET_RANGE_BLOCKS), 0.0D, 128.0D,
                MINION_TARGET_RANGE_BLOCKS);
    }

    public static boolean shouldMinionTargetHuman(double distanceSquared, double rangeBlocks) {
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0.0D
                || !Double.isFinite(rangeBlocks) || rangeBlocks < 0.0D) return false;
        return rangeBlocks == 0.0D || distanceSquared <= rangeBlocks * rangeBlocks;
    }

    public static Vector minionLaunchVelocity(Vector direction, double jitterX, double jitterY, double jitterZ) {
        if (direction == null || !Double.isFinite(jitterX) || !Double.isFinite(jitterY)
                || !Double.isFinite(jitterZ) || direction.lengthSquared() < 1.0E-9D) return new Vector();
        return direction.clone().add(new Vector(jitterX, jitterY, jitterZ)).normalize().multiply(minionLaunchSpeed());
    }

    public static float fireStormExperienceAfterTick(float experience, boolean active) {
        double step = active
                ? finiteClamp(GameDebugRuntimeOverrides.doubleValue("WITHER_ASSAULT_FIRE_STORM_DRAIN_PER_TICK",
                        FIRE_STORM_DRAIN_PER_TICK), 0.0D, 0.25D, FIRE_STORM_DRAIN_PER_TICK)
                : finiteClamp(GameDebugRuntimeOverrides.doubleValue("WITHER_ASSAULT_FIRE_STORM_CHARGE_PER_TICK",
                        FIRE_STORM_CHARGE_PER_TICK), 0.0D, 0.25D, FIRE_STORM_CHARGE_PER_TICK);
        double current = Float.isFinite(experience) ? Math.max(0.0D, Math.min(0.999D, experience)) : 0.0D;
        return (float) (active ? Math.max(0.0D, current - step) : Math.min(0.999D, current + step));
    }

    public static int fireStormArrowsPerTick() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue(
                "WITHER_ASSAULT_FIRE_STORM_ARROWS_PER_TICK", FIRE_STORM_ARROWS_PER_TICK));
    }

    public static double fireStormArrowSpeed() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_FIRE_STORM_ARROW_SPEED", FIRE_STORM_ARROW_SPEED), 0.1D, 5.0D,
                FIRE_STORM_ARROW_SPEED);
    }

    public static double fireStormSpread() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_FIRE_STORM_SPREAD", FIRE_STORM_SPREAD), 0.0D, 0.5D, FIRE_STORM_SPREAD);
    }

    public static double flightMinSpeed() { return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
            "WITHER_ASSAULT_FLIGHT_MIN_SPEED", FLIGHT_MIN_SPEED), 0.001D, 0.25D, FLIGHT_MIN_SPEED); }
    public static double flightSpawnHoverSpeed() { return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
            "WITHER_ASSAULT_FLIGHT_SPAWN_HOVER_SPEED", FLIGHT_SPAWN_HOVER_SPEED), flightMinSpeed(), 1.0D,
            FLIGHT_SPAWN_HOVER_SPEED); }
    public static double flightMaxSpeed() { return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
            "WITHER_ASSAULT_FLIGHT_MAX_SPEED", FLIGHT_MAX_SPEED), flightMinSpeed(), 1.0D, FLIGHT_MAX_SPEED); }
    public static double flightBaseSpeed() { return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
            "WITHER_ASSAULT_FLIGHT_BASE_SPEED", FLIGHT_BASE_SPEED), flightMinSpeed(), flightMaxSpeed(),
            FLIGHT_BASE_SPEED); }
    public static double flightSpeedPerYBlock() { return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
            "WITHER_ASSAULT_FLIGHT_SPEED_PER_Y_BLOCK", FLIGHT_SPEED_PER_Y_BLOCK), 0.0D, 0.1D,
            FLIGHT_SPEED_PER_Y_BLOCK); }
    public static double flightBlendStartBlocks() { return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
            "WITHER_ASSAULT_FLIGHT_BLEND_START_BLOCKS", FLIGHT_BLEND_START_BLOCKS), 0.0D, 32.0D,
            FLIGHT_BLEND_START_BLOCKS); }
    public static double flightBlendEndBlocks() {
        double value = finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_FLIGHT_BLEND_END_BLOCKS", FLIGHT_BLEND_END_BLOCKS), 0.1D, 64.0D,
                FLIGHT_BLEND_END_BLOCKS);
        return Math.max(flightBlendStartBlocks() + 0.1D, value);
    }
    public static double flightMaxSpeedChangePerTick() { return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
            "WITHER_ASSAULT_FLIGHT_MAX_SPEED_CHANGE_PER_TICK", FLIGHT_MAX_SPEED_CHANGE_PER_TICK), 0.0001D,
            0.25D, FLIGHT_MAX_SPEED_CHANGE_PER_TICK); }

    public static double humanArrowDamageMultiplier() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_HUMAN_ARROW_DAMAGE_MULTIPLIER", HUMAN_ARROW_DAMAGE_MULTIPLIER), 0.0D, 2.0D,
                HUMAN_ARROW_DAMAGE_MULTIPLIER);
    }

    public static double skullBlastRadiusBlocks() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_SKULL_BLAST_RADIUS_BLOCKS", SKULL_BLAST_RADIUS_BLOCKS), 0.1D, 32.0D,
                SKULL_BLAST_RADIUS_BLOCKS);
    }

    public static double skullDamageAtDistance(double distance, double ageScale) {
        double radius = skullBlastRadiusBlocks();
        if (!Double.isFinite(distance) || distance < 0.0D || distance > radius) return 0.0D;
        double minimum = finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_SKULL_MIN_DAMAGE", SKULL_MIN_DAMAGE), 0.0D, 40.0D, SKULL_MIN_DAMAGE);
        double maximum = finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_SKULL_MAX_DAMAGE", SKULL_MAX_DAMAGE), minimum, 40.0D, SKULL_MAX_DAMAGE);
        double age = finiteClamp(ageScale, 0.0D, 1.0D, 0.0D);
        double scale = Math.max(0.0D, 1.0D - distance / radius);
        return minimum + (maximum - minimum) * scale * age;
    }

    public static double skullKnockbackPower() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_SKULL_KNOCKBACK_POWER", SKULL_KNOCKBACK_POWER), 0.0D, 5.0D,
                SKULL_KNOCKBACK_POWER);
    }

    public static double skullKnockbackYMultiplier() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_SKULL_KNOCKBACK_Y_MULTIPLIER", SKULL_KNOCKBACK_Y_MULTIPLIER), 0.0D, 2.0D,
                SKULL_KNOCKBACK_Y_MULTIPLIER);
    }

    public static double skullKnockbackYMinimum() {
        return finiteClamp(GameDebugRuntimeOverrides.doubleValue(
                "WITHER_ASSAULT_SKULL_KNOCKBACK_Y_MINIMUM", SKULL_KNOCKBACK_Y_MINIMUM), 0.0D, 3.0D,
                SKULL_KNOCKBACK_Y_MINIMUM);
    }

    public static int skullTerrainRadiusBlocks() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue(
                "WITHER_ASSAULT_SKULL_TERRAIN_RADIUS_BLOCKS", SKULL_TERRAIN_RADIUS_BLOCKS));
    }

    public static int skullBlockRestoreTicks() {
        return Math.max(1, GameDebugRuntimeOverrides.intValue(
                "WITHER_ASSAULT_SKULL_BLOCK_RESTORE_TICKS", SKULL_BLOCK_RESTORE_TICKS));
    }

    private static double finiteClamp(double value, double minimum, double maximum, double fallback) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : fallback;
    }

    private static String normalizeItem(String itemType) {
        return itemType == null ? "" : itemType.trim().toLowerCase(Locale.ROOT);
    }

    /** Only a living Human Archer owns the source Smoke Arrow perk. */
    public static boolean isHumanArcherSmokeArrowEligible(int teamId, String kitId, boolean alive) {
        return alive && teamId == 0 && "archer".equals(normalizeHumanKit(kitId));
    }

    /** The source proximity check blinds every Wither strictly inside the six-block radius. */
    public static boolean shouldSmokeArrowBlindAtDistanceSquared(double distanceSquared, double radius) {
        if (!Double.isFinite(distanceSquared) || !Double.isFinite(radius)
                || distanceSquared < 0.0D || radius <= 0.0D) return false;
        return distanceSquared < radius * radius;
    }
}
