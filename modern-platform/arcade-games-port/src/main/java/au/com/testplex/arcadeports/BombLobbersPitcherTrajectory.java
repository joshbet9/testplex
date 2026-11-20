package au.com.testplex.arcadeports;

import org.bukkit.util.Vector;

/**
 * Solves a small velocity correction for Pitcher TNT using the per-tick drag
 * and gravity used by primed TNT. Direction stays on the selected island lane.
 */
final class BombLobbersPitcherTrajectory {
    private static final double DRAG = 0.98D;
    private static final double GRAVITY = 0.04D;
    private static final double MAX_LAUNCH_SPEED = 3.5D;
    private static final int MAX_FLIGHT_TICKS = 80;

    private BombLobbersPitcherTrajectory() {
    }

    static boolean isDestroyableBlock(boolean solid, float blastResistance) {
        return solid && Float.isFinite(blastResistance) && blastResistance < 13.0F;
    }

    static Vector selectTargetOffset(Vector directlyAimedOffset, Vector fallbackOffset) {
        if (usableTargetOffset(directlyAimedOffset)) return directlyAimedOffset.clone();
        return usableTargetOffset(fallbackOffset) ? fallbackOffset.clone() : null;
    }

    private static boolean usableTargetOffset(Vector offset) {
        if (offset == null || !finite(offset)) return false;
        double horizontalDistanceSquared = offset.getX() * offset.getX() + offset.getZ() * offset.getZ();
        return horizontalDistanceSquared >= 4.0D && horizontalDistanceSquared <= 4096.0D;
    }

    static Vector velocityToTarget(Vector horizontalDirection, Vector normalVelocity,
            double horizontalDistance, double verticalDelta) {
        if (horizontalDirection == null || normalVelocity == null
                || !Double.isFinite(horizontalDistance) || horizontalDistance <= 0.0D
                || !Double.isFinite(verticalDelta)
                || !finite(horizontalDirection) || !finite(normalVelocity)) return null;

        Vector direction = horizontalDirection.clone().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-8D) return null;
        direction.normalize();

        double originalHorizontalSpeed = Math.hypot(normalVelocity.getX(), normalVelocity.getZ());
        double originalVerticalSpeed = normalVelocity.getY();
        double bestScore = Double.POSITIVE_INFINITY;
        Vector best = null;
        for (int ticks = 2; ticks <= MAX_FLIGHT_TICKS; ticks++) {
            double displacementFactor = displacement(1.0D, ticks) - displacement(0.0D, ticks);
            if (!Double.isFinite(displacementFactor) || displacementFactor <= 0.0D) continue;
            double horizontalSpeed = horizontalDistance / displacementFactor;
            double verticalSpeed = (verticalDelta - displacement(0.0D, ticks)) / displacementFactor;
            double totalSpeed = Math.sqrt(horizontalSpeed * horizontalSpeed + verticalSpeed * verticalSpeed);
            if (!Double.isFinite(totalSpeed) || totalSpeed > MAX_LAUNCH_SPEED || horizontalSpeed <= 0.0D) continue;

            double horizontalChange = horizontalSpeed - originalHorizontalSpeed;
            double verticalChange = verticalSpeed - originalVerticalSpeed;
            double score = horizontalChange * horizontalChange + verticalChange * verticalChange;
            if (score >= bestScore) continue;
            bestScore = score;
            best = new Vector(direction.getX() * horizontalSpeed, verticalSpeed,
                    direction.getZ() * horizontalSpeed);
        }
        return best;
    }

    private static double displacement(double initialVelocity, int ticks) {
        double position = 0.0D;
        double velocity = initialVelocity;
        for (int tick = 0; tick < ticks; tick++) {
            position += velocity;
            velocity = velocity * DRAG - GRAVITY;
        }
        return position;
    }

    private static boolean finite(Vector vector) {
        return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY())
                && Double.isFinite(vector.getZ());
    }
}
