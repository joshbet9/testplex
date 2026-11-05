package au.com.testplex.arcadeports;

import java.util.Locale;
import java.util.Set;

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
