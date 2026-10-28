package au.com.testplex.snake;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Small deterministic rules shared by the Snake loop and its contract tests. */
public final class SnakeRules {
    private static final int[] ROULETTE_SECONDS = {1, 3, 5, 10, 15, 20, 30};

    public static final int SPEED_REFERENCE_LENGTH = 180;
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
        // Legacy CreatureMoveFast uses multiplier 1.2 + 2 * (0.4 + sourceGrowth).
        // Its effective movement scales at about 0.1 block/tick per multiplier unit.
        double speed = 0.2D + sourceGrowth * 0.2D;
        return speedBoost ? speed * 1.5D : speed;
    }

    /** Match Reborn's per-round player index palette, wrapping safely when more than 16 players queue. */
    public static int colorIndexForParticipant(int participantIndex, int paletteSize) {
        return paletteSize <= 0 ? 0 : Math.floorMod(participantIndex, paletteSize);
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
