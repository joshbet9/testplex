package au.com.testplex.snake;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import au.com.testplex.arcadeports.GameDebugRuntimeOverrides;

import java.util.ArrayList;
import java.util.List;

/** Small deterministic rules shared by the Snake loop and its contract tests. */
public final class SnakeRules {
    public static long debugLiveAt(boolean running, boolean live, long nowMillis, long currentLiveAtMillis) {
        return running && !live ? Math.min(currentLiveAtMillis, nowMillis) : currentLiveAtMillis;
    }
    private static final DyeColor[] LEGACY_DYE_DATA_ORDER = {
            DyeColor.BLACK, DyeColor.RED, DyeColor.GREEN, DyeColor.BROWN,
            DyeColor.BLUE, DyeColor.PURPLE, DyeColor.CYAN, DyeColor.LIGHT_GRAY,
            DyeColor.GRAY, DyeColor.PINK, DyeColor.LIME, DyeColor.YELLOW,
            DyeColor.LIGHT_BLUE, DyeColor.MAGENTA, DyeColor.ORANGE, DyeColor.WHITE
    };

    public static final int SPEED_REFERENCE_LENGTH = 180;
    public static final long IDLE_TIMEOUT_MILLIS = 2_000L;
    public static final double IDLE_MOVEMENT_DISTANCE_SQUARED = 4.0D;
    public static final long PREPARE_COUNTDOWN_MILLIS = 9_000L;
    public static final double START_SPAWN_RADIUS_BLOCKS = 1.5D;
    public static final int CANNIBAL_KILLS_REQUIRED = 6;
    public static final int CHOO_CHOO_LENGTH_REQUIRED = 60;
    public static final int SLIMY_SHEEP_REQUIRED = 20;
    public static final double SEGMENT_SPACING = 1.0D;
    public static final double BODY_FOLLOW_SPACING = 1.4D;
    public static final int SELF_TAIL_SAFE_SEGMENTS = 3;
    public static final double TAIL_COLLISION_RADIUS = 1.2D;
    public static final double FOOD_PICKUP_RADIUS = 2.0D;
    public static final int FOOD_SPAWN_ATTEMPTS = 36;
    public static final double FOOD_SPAWN_RADIUS = 46.0D;
    public static final long FOOD_SPAWN_INTERVAL_MILLIS = 1_000L;
    public static final long GROWTH_INTERVAL_MILLIS = 1_000L;
    public static final int PASSIVE_GROWTH_SEGMENTS = 1;
    public static final int FOOD_GROWTH_SEGMENTS = 2;
    public static final double MOVEMENT_SOURCE_BASE_MULTIPLIER = 0.4D;
    public static final double MOVEMENT_GROWTH_CAP = 0.7D;
    public static final double MOVEMENT_SPEED_BOOST_MULTIPLIER = 1.5D;
    public static final double MOVEMENT_CONVERSION_SCALE = 0.1D;
    public static final double MOVEMENT_CONVERSION_BASE = 1.2D;
    public static final double TURN_LAG_DEGREES_PER_TICK = 6.0D;
    public static final double OVERSHOOT_TURN_MULTIPLIER = 0.35D;
    public static final double WEAK_TURN_THRESHOLD_DEGREES = 15.0D;
    public static final double WEAK_TURN_DEGREES_PER_TICK = 4.0D;
    public static final double DRIFT_MAX_TURN_DEGREES = 8.0D;
    public static final int ROULETTE_HIT_1_SECONDS = 1;
    public static final int ROULETTE_HIT_2_SECONDS = 3;
    public static final int ROULETTE_HIT_3_SECONDS = 5;
    public static final int ROULETTE_HIT_4_SECONDS = 10;
    public static final int ROULETTE_HIT_5_SECONDS = 15;
    public static final int ROULETTE_HIT_6_SECONDS = 20;
    public static final int ROULETTE_HIT_7_PLUS_SECONDS = 30;
    public static final int ROULETTE_ANIMATION_STEPS = 12;
    public static final long ROULETTE_ANIMATION_INTERVAL_TICKS = 2L;
    public static final int BUSTED_STREAK_REQUIRED = 3;
    public static final int EDGE_STAY_MARGIN_BLOCKS = 10;
    public static final long SPEED_ABILITY_DURATION_MILLIS = 2_000L;
    public static final long REVERSE_ABILITY_DURATION_MILLIS = 2_000L;
    public static final long INVULNERABLE_ABILITY_DURATION_MILLIS = 2_000L;
    public static final long KIT_ABILITY_COOLDOWN_MILLIS = 1_000L;
    public static final int SPEED_ABILITY_CHARGES = 5;
    public static final int REVERSE_ABILITY_CHARGES = 3;
    public static final int INVULNERABLE_ABILITY_CHARGES = 2;

    /** Reborn samples food uniformly by area within a disk around the arena spawn. */
    public static double foodSpawnDistance(double randomUnit) {
        if (!Double.isFinite(randomUnit)) return 0.0D;
        return Math.sqrt(Math.max(0.0D, Math.min(1.0D, randomUnit))) * foodSpawnRadius();
    }

    public static final int MAX_HAZARDS = 4;
    public static final int HAZARD_SPAWN_ATTEMPTS = 48;
    public static final int HAZARD_SPAWN_BASE_DISTANCE = 12;
    public static final int HAZARD_SPAWN_DISTANCE_STEP = 2;
    public static final int HAZARD_SPAWN_DISTANCE_VARIANTS = 5;
    public static final int HAZARD_MAX_CUBE_SIZE = 4;
    public static final int HAZARD_CLEARANCE_BLOCKS = 4;
    public static final long HAZARD_RETRY_MILLIS = 5_000L;
    public static final long FIRST_HAZARD_DELAY_MILLIS = 10_000L;
    public static final long HAZARD_INTERVAL_MILLIS = 15_000L;
    public static final int HAZARD_STEERING_INTERVAL_TICKS = 4;
    public static final double HAZARD_STEERING_SPEED = 0.12D;
    public static final double HAZARD_CONTACT_RADIUS = 2.1D;

    public static int speedReferenceLength() { return intRule("SPEED_REFERENCE_LENGTH", SPEED_REFERENCE_LENGTH, 1, 10_000); }
    public static long idleTimeoutMillis() { return longRule("IDLE_TIMEOUT_MILLIS", IDLE_TIMEOUT_MILLIS, 1L, 600_000L); }
    public static double idleMovementDistanceSquared() { return doubleRule("IDLE_MOVEMENT_DISTANCE_SQUARED", IDLE_MOVEMENT_DISTANCE_SQUARED, 0.0D, 256.0D); }
    public static long prepareCountdownMillis() { return longRule("PREPARE_COUNTDOWN_MILLIS", PREPARE_COUNTDOWN_MILLIS, 1L, 600_000L); }
    public static double startSpawnRadiusBlocks() { return doubleRule("START_SPAWN_RADIUS_BLOCKS", START_SPAWN_RADIUS_BLOCKS, 0.0D, 16.0D); }
    public static int cannibalKillsRequired() { return intRule("CANNIBAL_KILLS_REQUIRED", CANNIBAL_KILLS_REQUIRED, 1, 1_000); }
    public static int chooChooLengthRequired() { return intRule("CHOO_CHOO_LENGTH_REQUIRED", CHOO_CHOO_LENGTH_REQUIRED, 1, 10_000); }
    public static int slimySheepRequired() { return intRule("SLIMY_SHEEP_REQUIRED", SLIMY_SHEEP_REQUIRED, 1, 10_000); }
    public static double segmentSpacing() { return doubleRule("SEGMENT_SPACING", SEGMENT_SPACING, 0.1D, 8.0D); }
    public static double bodyFollowSpacing() { return doubleRule("BODY_FOLLOW_SPACING", BODY_FOLLOW_SPACING, 0.1D, 8.0D); }
    public static int selfTailSafeSegments() { return intRule("SELF_TAIL_SAFE_SEGMENTS", SELF_TAIL_SAFE_SEGMENTS, 0, 32); }
    public static double snakeTailCollisionRadius() { return doubleRule("TAIL_COLLISION_RADIUS", TAIL_COLLISION_RADIUS, 0.0D, 16.0D); }
    public static double foodPickupRadius() { return doubleRule("FOOD_PICKUP_RADIUS", FOOD_PICKUP_RADIUS, 0.0D, 16.0D); }
    public static int foodSpawnAttempts() { return intRule("FOOD_SPAWN_ATTEMPTS", FOOD_SPAWN_ATTEMPTS, 1, 1_000); }
    public static double foodSpawnRadius() { return doubleRule("FOOD_SPAWN_RADIUS", FOOD_SPAWN_RADIUS, 0.0D, 256.0D); }
    public static long foodSpawnIntervalMillis() { return longRule("FOOD_SPAWN_INTERVAL_MILLIS", FOOD_SPAWN_INTERVAL_MILLIS, 1L, 600_000L); }
    public static long growthIntervalMillis() { return longRule("GROWTH_INTERVAL_MILLIS", GROWTH_INTERVAL_MILLIS, 1L, 600_000L); }
    public static int passiveGrowthSegments() { return intRule("PASSIVE_GROWTH_SEGMENTS", PASSIVE_GROWTH_SEGMENTS, 0, 64); }
    public static int foodGrowthSegments() { return intRule("FOOD_GROWTH_SEGMENTS", FOOD_GROWTH_SEGMENTS, 0, 64); }
    public static double movementSourceBaseMultiplier() { return doubleRule("MOVEMENT_SOURCE_BASE_MULTIPLIER", MOVEMENT_SOURCE_BASE_MULTIPLIER, 0.0D, 8.0D); }
    public static double movementGrowthCap() { return doubleRule("MOVEMENT_GROWTH_CAP", MOVEMENT_GROWTH_CAP, 0.0D, 8.0D); }
    public static double movementSpeedBoostMultiplier() { return doubleRule("MOVEMENT_SPEED_BOOST_MULTIPLIER", MOVEMENT_SPEED_BOOST_MULTIPLIER, 0.0D, 8.0D); }
    public static double movementConversionScale() { return doubleRule("MOVEMENT_CONVERSION_SCALE", MOVEMENT_CONVERSION_SCALE, 0.0D, 2.0D); }
    public static double movementConversionBase() { return doubleRule("MOVEMENT_CONVERSION_BASE", MOVEMENT_CONVERSION_BASE, 0.0D, 8.0D); }
    public static double turnLagDegreesPerTick() { return doubleRule("TURN_LAG_DEGREES_PER_TICK", TURN_LAG_DEGREES_PER_TICK, 0.0D, 180.0D); }
    public static double overshootTurnMultiplier() { return doubleRule("OVERSHOOT_TURN_MULTIPLIER", OVERSHOOT_TURN_MULTIPLIER, 0.0D, 4.0D); }
    public static double weakTurnThresholdDegrees() { return doubleRule("WEAK_TURN_THRESHOLD_DEGREES", WEAK_TURN_THRESHOLD_DEGREES, 0.0D, 180.0D); }
    public static double weakTurnDegreesPerTick() { return doubleRule("WEAK_TURN_DEGREES_PER_TICK", WEAK_TURN_DEGREES_PER_TICK, 0.0D, 180.0D); }
    public static double driftMaxTurnDegrees() { return doubleRule("DRIFT_MAX_TURN_DEGREES", DRIFT_MAX_TURN_DEGREES, 0.0D, 180.0D); }
    public static int rouletteAnimationSteps() { return intRule("ROULETTE_ANIMATION_STEPS", ROULETTE_ANIMATION_STEPS, 0, 100); }
    public static long rouletteAnimationIntervalTicks() { return longRule("ROULETTE_ANIMATION_INTERVAL_TICKS", ROULETTE_ANIMATION_INTERVAL_TICKS, 1L, 100L); }
    public static int bustedStreakRequired() { return intRule("BUSTED_STREAK_REQUIRED", BUSTED_STREAK_REQUIRED, 1, 100); }
    public static int edgeStayMarginBlocks() { return intRule("EDGE_STAY_MARGIN_BLOCKS", EDGE_STAY_MARGIN_BLOCKS, 0, 128); }
    public static long speedAbilityDurationMillis() { return longRule("SPEED_ABILITY_DURATION_MILLIS", SPEED_ABILITY_DURATION_MILLIS, 1L, 600_000L); }
    public static long reverseAbilityDurationMillis() { return longRule("REVERSE_ABILITY_DURATION_MILLIS", REVERSE_ABILITY_DURATION_MILLIS, 1L, 600_000L); }
    public static long invulnerableAbilityDurationMillis() { return longRule("INVULNERABLE_ABILITY_DURATION_MILLIS", INVULNERABLE_ABILITY_DURATION_MILLIS, 1L, 600_000L); }
    public static long kitAbilityCooldownMillis() { return longRule("KIT_ABILITY_COOLDOWN_MILLIS", KIT_ABILITY_COOLDOWN_MILLIS, 0L, 600_000L); }
    public static int speedAbilityCharges() { return intRule("SPEED_ABILITY_CHARGES", SPEED_ABILITY_CHARGES, 0, 64); }
    public static int reverseAbilityCharges() { return intRule("REVERSE_ABILITY_CHARGES", REVERSE_ABILITY_CHARGES, 0, 64); }
    public static int invulnerableAbilityCharges() { return intRule("INVULNERABLE_ABILITY_CHARGES", INVULNERABLE_ABILITY_CHARGES, 0, 64); }
    public static int maxHazards() { return intRule("MAX_HAZARDS", MAX_HAZARDS, 0, 64); }
    public static int hazardSpawnAttempts() { return intRule("HAZARD_SPAWN_ATTEMPTS", HAZARD_SPAWN_ATTEMPTS, 1, 1_000); }
    public static int hazardSpawnBaseDistance() { return intRule("HAZARD_SPAWN_BASE_DISTANCE", HAZARD_SPAWN_BASE_DISTANCE, 0, 128); }
    public static int hazardSpawnDistanceStep() { return intRule("HAZARD_SPAWN_DISTANCE_STEP", HAZARD_SPAWN_DISTANCE_STEP, 0, 128); }
    public static int hazardSpawnDistanceVariants() { return intRule("HAZARD_SPAWN_DISTANCE_VARIANTS", HAZARD_SPAWN_DISTANCE_VARIANTS, 1, 128); }
    public static int hazardMaxCubeSize() { return intRule("HAZARD_MAX_CUBE_SIZE", HAZARD_MAX_CUBE_SIZE, 1, 16); }
    public static int hazardClearanceBlocks() { return intRule("HAZARD_CLEARANCE_BLOCKS", HAZARD_CLEARANCE_BLOCKS, 0, 32); }
    public static long hazardRetryMillis() { return longRule("HAZARD_RETRY_MILLIS", HAZARD_RETRY_MILLIS, 1L, 600_000L); }
    public static long firstHazardDelayMillis() { return longRule("FIRST_HAZARD_DELAY_MILLIS", FIRST_HAZARD_DELAY_MILLIS, 1L, 600_000L); }
    public static long hazardIntervalMillis() { return longRule("HAZARD_INTERVAL_MILLIS", HAZARD_INTERVAL_MILLIS, 1L, 600_000L); }
    public static int hazardSteeringIntervalTicks() { return intRule("HAZARD_STEERING_INTERVAL_TICKS", HAZARD_STEERING_INTERVAL_TICKS, 1, 1_000); }
    public static double hazardSteeringSpeed() { return doubleRule("HAZARD_STEERING_SPEED", HAZARD_STEERING_SPEED, 0.0D, 8.0D); }
    public static double hazardContactRadius() { return doubleRule("HAZARD_CONTACT_RADIUS", HAZARD_CONTACT_RADIUS, 0.0D, 32.0D); }

    private static int intRule(String key, int fallback, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, GameDebugRuntimeOverrides.intValue("SNAKE_" + key, fallback)));
    }

    private static long longRule(String key, long fallback, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, GameDebugRuntimeOverrides.longValue("SNAKE_" + key, fallback)));
    }

    private static double doubleRule(String key, double fallback, double minimum, double maximum) {
        double value = GameDebugRuntimeOverrides.doubleValue("SNAKE_" + key, fallback);
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : fallback;
    }

    /** Source ReSit remounts the player on the live snake head every tick. */
    public static boolean shouldResitPlayer(boolean hasHead, boolean ridingHead) {
        return hasHead && !ridingHead;
    }

    /** The source cancels normal damage but immediately eliminates a player hit by lava. */
    public static boolean shouldEliminateFromDamageCause(String causeName) {
        return causeName != null && "LAVA".equalsIgnoreCase(causeName);
    }

    /** Match Reborn's golden-angle search distances for finding a clear magma-cube spawn. */
    public static double hazardSpawnDistance(int attempt) {
        return hazardSpawnBaseDistance()
                + Math.floorMod(attempt, hazardSpawnDistanceVariants()) * hazardSpawnDistanceStep();
    }

    /** Legacy Snake leaves an item box available if the player is still rolling an item. */
    public static boolean shouldConsumeHazardOnContact(boolean rouletteActive) {
        return !rouletteActive;
    }

    /** Reborn's Idle rule resets only after the head has travelled more than two blocks. */
    public static boolean hasMovedForIdleCheck(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared > idleMovementDistanceSquared();
    }

    /** Reborn leaves the mounted head collidable and ghosts all appended tail segments. */
    public static boolean shouldGhostTailSegment(int segmentIndex) {
        return segmentIndex > 0;
    }

    public static boolean isAbilityItemInteraction(String actionName) {
        return actionName != null && !"PHYSICAL".equalsIgnoreCase(actionName);
    }

    public static boolean isKitAbilityItem(String kit, String materialName) {
        if (kit == null || materialName == null) return false;
        return switch (kit.toLowerCase(java.util.Locale.ROOT)) {
            case "speed" -> "FEATHER".equalsIgnoreCase(materialName);
            case "reverse" -> "COOKIE".equalsIgnoreCase(materialName);
            case "invulnerable" -> "NETHER_STAR".equalsIgnoreCase(materialName);
            default -> false;
        };
    }

    public static boolean isPreparing(long nowMillis, long liveAtMillis) {
        return nowMillis < liveAtMillis;
    }

    public static boolean shouldEliminateForIdle(double distanceSquared, long idleMillis) {
        return idleMillis >= idleTimeoutMillis() && !hasMovedForIdleCheck(distanceSquared);
    }

    public static boolean shouldAwardCannibal(int killsThisRound) {
        return killsThisRound == cannibalKillsRequired();
    }

    public static boolean shouldAwardChooChoo(int tailLength) {
        return tailLength >= chooChooLengthRequired();
    }

    public static boolean shouldAwardSlimySheep(int slimesEatenThisRound) {
        return slimesEatenThisRound == slimySheepRequired();
    }


    public static int nextSameRouletteEffectCount(boolean sameAsPrevious, int previousCount) {
        return sameAsPrevious ? Math.max(0, previousCount) + 1 : 1;
    }

    public static boolean shouldAwardBusted(int consecutiveSameEffects) {
        return consecutiveSameEffects >= bustedStreakRequired();
    }

    public static boolean isTenSecondRouletteHit(int durationSeconds) {
        return durationSeconds == rouletteDurationSecondsForHit(4);
    }

    public static boolean isWithinTenBlocksOfMapEdge(double x, double z, double clampedX, double clampedZ) {
        if (!Double.isFinite(x) || !Double.isFinite(z)
                || !Double.isFinite(clampedX) || !Double.isFinite(clampedZ)) return false;
        return Math.abs(x - clampedX) > 0.001D || Math.abs(z - clampedZ) > 0.001D;
    }

    public static boolean isAbilityActive(long now, long speedUntil, long reverseUntil, long invulnerableUntil) {
        return speedUntil > now || reverseUntil > now || invulnerableUntil > now;
    }

    public static boolean shouldAwardFlawless(int kills, boolean allKillsDuringAbility) {
        return kills > 0 && allKillsDuringAbility;
    }

    private SnakeRules() {}

    public static int rouletteDurationSecondsForHit(int hitCount) {
        return switch (Math.max(1, Math.min(hitCount, 7))) {
            case 1 -> intRule("ROULETTE_HIT_1_SECONDS", ROULETTE_HIT_1_SECONDS, 1, 600);
            case 2 -> intRule("ROULETTE_HIT_2_SECONDS", ROULETTE_HIT_2_SECONDS, 1, 600);
            case 3 -> intRule("ROULETTE_HIT_3_SECONDS", ROULETTE_HIT_3_SECONDS, 1, 600);
            case 4 -> intRule("ROULETTE_HIT_4_SECONDS", ROULETTE_HIT_4_SECONDS, 1, 600);
            case 5 -> intRule("ROULETTE_HIT_5_SECONDS", ROULETTE_HIT_5_SECONDS, 1, 600);
            case 6 -> intRule("ROULETTE_HIT_6_SECONDS", ROULETTE_HIT_6_SECONDS, 1, 600);
            default -> intRule("ROULETTE_HIT_7_PLUS_SECONDS", ROULETTE_HIT_7_PLUS_SECONDS, 1, 600);
        };
    }

    /**
     * Map Reborn's speed curve (0.4 + min(0.7, length / 180)) onto modern
     * block-per-tick movement. The source stops gaining speed at 126 segments;
     * 180 is a reference length, not a maximum tail size.
     */
    public static double movementSpeed(int tailLength, boolean speedBoost) {
        double sourceGrowth = Math.min(movementGrowthCap(), Math.max(0, tailLength) / (double) speedReferenceLength());
        // Reborn first scales the movement multiplier for Speed, then passes
        // (1.2 + 2 * multiplier) to CreatureMoveFast. The Paper loop uses the
        // equivalent 0.1-block-per-multiplier conversion.
        double sourceMultiplier = movementSourceBaseMultiplier() + sourceGrowth;
        if (speedBoost) sourceMultiplier *= movementSpeedBoostMultiplier();
        return movementConversionScale() * (movementConversionBase() + 2.0D * sourceMultiplier);
    }

    /** Match Reborn's per-round player index palette, wrapping safely when more than 16 players queue. */
    public static int colorIndexForParticipant(int participantIndex, int paletteSize) {
        return paletteSize <= 0 ? 0 : Math.floorMod(participantIndex, paletteSize);
    }

    /** Reproduce Bukkit 1.8 DyeColor.getByDyeData(i), not the modern enum order. */
    public static DyeColor colorForLegacyDyeData(int dyeData) {
        return LEGACY_DYE_DATA_ORDER[Math.floorMod(dyeData, LEGACY_DYE_DATA_ORDER.length)];
    }

    /** Source food collection uses a strict 2-block 3D radius. */
    public static boolean isFoodCollected(double distanceSquared) {
        double radius = foodPickupRadius();
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared < radius * radius;
    }

    /** Source tail collision tests a strict 1.2-block 3D radius. */
    public static boolean isTailCollision(double distanceSquared) {
        double radius = snakeTailCollisionRadius();
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared < radius * radius;
    }

    /**
     * Continue a growing tail along its existing final segment. For a one-segment
     * snake, extend behind the player's facing direction as the source does.
     */
    public static Vector tailExtensionDirection(Vector previousSegment, Vector lastSegment, Vector playerFacing) {
        if (previousSegment != null && lastSegment != null) {
            Vector tailDirection = lastSegment.clone().subtract(previousSegment).setY(0.0D);
            if (tailDirection.lengthSquared() > 1.0E-6D) return tailDirection.normalize();
        }
        Vector facing = playerFacing == null ? new Vector(0.0D, 0.0D, 1.0D)
                : playerFacing.clone().setY(0.0D);
        if (facing.lengthSquared() < 1.0E-6D) facing = new Vector(0.0D, 0.0D, 1.0D);
        return facing.normalize().multiply(-1.0D);
    }

    /**
     * Seed the head-first path deque from the segment positions after Reverser flips
     * the segment list. Keeping the existing geometry prevents all segments collapsing
     * onto the old head when normal path-following resumes.
     */
    public static List<Location> seedReversedBodyPath(List<Location> reversedSegments) {
        List<Location> path = new ArrayList<>();
        if (reversedSegments == null || reversedSegments.isEmpty()) return path;
        Location previous = null;
        for (Location location : reversedSegments) {
            if (location == null) continue;
            if (previous == null) {
                path.add(location.clone());
                previous = location;
                continue;
            }
            double dx = location.getX() - previous.getX();
            double dy = location.getY() - previous.getY();
            double dz = location.getZ() - previous.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int steps = Math.max(1, (int) Math.ceil(distance / 0.2D));
            for (int step = 1; step <= steps; step++) {
                double amount = step / (double) steps;
                path.add(previous.clone().add(dx * amount, dy * amount, dz * amount));
            }
            previous = location;
        }
        return path;
    }

    public static double clampTurnRadians(double requestedRadians) {
        double max = Math.toRadians(driftMaxTurnDegrees());
        return Math.max(-max, Math.min(max, requestedRadians));
    }

    public static float wrapYaw(float angle) {
        while (angle <= -180.0F) angle += 360.0F;
        while (angle > 180.0F) angle -= 360.0F;
        return angle;
    }

    public static double wrapRadians(double angle) {
        while (angle <= -Math.PI) angle += 2.0D * Math.PI;
        while (angle > Math.PI) angle -= 2.0D * Math.PI;
        return angle;
    }
}
