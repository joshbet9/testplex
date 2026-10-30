package au.com.testplex.snake;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Small deterministic rules shared by the Snake loop and its contract tests. */
public final class SnakeRules {
    private static final int[] ROULETTE_SECONDS = {1, 3, 5, 10, 15, 20, 30};
    private static final DyeColor[] LEGACY_DYE_DATA_ORDER = {
            DyeColor.BLACK, DyeColor.RED, DyeColor.GREEN, DyeColor.BROWN,
            DyeColor.BLUE, DyeColor.PURPLE, DyeColor.CYAN, DyeColor.LIGHT_GRAY,
            DyeColor.GRAY, DyeColor.PINK, DyeColor.LIME, DyeColor.YELLOW,
            DyeColor.LIGHT_BLUE, DyeColor.MAGENTA, DyeColor.ORANGE, DyeColor.WHITE
    };

    public static final int SPEED_REFERENCE_LENGTH = 180;
    public static final long IDLE_TIMEOUT_MILLIS = 2_000L;
    public static final long PREPARE_COUNTDOWN_MILLIS = 9_000L;
    public static final int CANNIBAL_KILLS_REQUIRED = 6;
    public static final int CHOO_CHOO_LENGTH_REQUIRED = 60;
    public static final int SLIMY_SHEEP_REQUIRED = 20;
    public static final double SEGMENT_SPACING = 1.0D;
    public static final double SNAKE_TAIL_COLLISION_RADIUS = 1.2D;
    public static final double FOOD_PICKUP_RADIUS = 2.0D;
    public static final int FOOD_SPAWN_ATTEMPTS = 36;
    public static final double FOOD_SPAWN_RADIUS = 46.0D;

    /** Reborn samples food uniformly by area within a disk around the arena spawn. */
    public static double foodSpawnDistance(double randomUnit) {
        if (!Double.isFinite(randomUnit)) return 0.0D;
        return Math.sqrt(Math.max(0.0D, Math.min(1.0D, randomUnit))) * FOOD_SPAWN_RADIUS;
    }

    public static final int MAX_HAZARDS = 4;
    public static final int HAZARD_SPAWN_ATTEMPTS = 48;
    public static final int HAZARD_SPAWN_BASE_DISTANCE = 12;
    public static final int HAZARD_SPAWN_DISTANCE_STEP = 2;
    public static final int HAZARD_SPAWN_DISTANCE_VARIANTS = 5;
    public static final long FIRST_HAZARD_DELAY_MILLIS = 10_000L;
    public static final long HAZARD_INTERVAL_MILLIS = 15_000L;
    public static final int HAZARD_STEERING_INTERVAL_TICKS = 4;
    public static final double HAZARD_STEERING_SPEED = 0.12D;
    public static final double HAZARD_CONTACT_RADIUS = 2.1D;

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
        return HAZARD_SPAWN_BASE_DISTANCE
                + Math.floorMod(attempt, HAZARD_SPAWN_DISTANCE_VARIANTS) * HAZARD_SPAWN_DISTANCE_STEP;
    }

    /** Legacy Snake leaves an item box available if the player is still rolling an item. */
    public static boolean shouldConsumeHazardOnContact(boolean rouletteActive) {
        return !rouletteActive;
    }

    /** Reborn's Idle rule resets only after the head has travelled more than two blocks. */
    public static boolean hasMovedForIdleCheck(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared > 4.0D;
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
        return idleMillis >= IDLE_TIMEOUT_MILLIS && !hasMovedForIdleCheck(distanceSquared);
    }

    public static boolean shouldAwardCannibal(int killsThisRound) {
        return killsThisRound == CANNIBAL_KILLS_REQUIRED;
    }

    public static boolean shouldAwardChooChoo(int tailLength) {
        return tailLength >= CHOO_CHOO_LENGTH_REQUIRED;
    }

    public static boolean shouldAwardSlimySheep(int slimesEatenThisRound) {
        return slimesEatenThisRound == SLIMY_SHEEP_REQUIRED;
    }

    private SnakeRules() {}

    public static int rouletteDurationSecondsForHit(int hitCount) {
        if (hitCount <= 1) return ROULETTE_SECONDS[0];
        return ROULETTE_SECONDS[Math.min(hitCount, ROULETTE_SECONDS.length) - 1];
    }

    /**
     * Map Reborn's speed curve (0.4 + min(0.7, length / 180)) onto modern
     * block-per-tick movement. The source stops gaining speed at 126 segments;
     * 180 is a reference length, not a maximum tail size.
     */
    public static double movementSpeed(int tailLength, boolean speedBoost) {
        double sourceGrowth = Math.min(0.7D, Math.max(0, tailLength) / (double) SPEED_REFERENCE_LENGTH);
        // Reborn first scales the movement multiplier for Speed, then passes
        // (1.2 + 2 * multiplier) to CreatureMoveFast. The Paper loop uses the
        // equivalent 0.1-block-per-multiplier conversion.
        double sourceMultiplier = 0.4D + sourceGrowth;
        if (speedBoost) sourceMultiplier *= 1.5D;
        return 0.1D * (1.2D + 2.0D * sourceMultiplier);
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
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared < FOOD_PICKUP_RADIUS * FOOD_PICKUP_RADIUS;
    }

    /** Source tail collision tests a strict 1.2-block 3D radius. */
    public static boolean isTailCollision(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared < SNAKE_TAIL_COLLISION_RADIUS * SNAKE_TAIL_COLLISION_RADIUS;
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
        double max = Math.toRadians(8.0D);
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
