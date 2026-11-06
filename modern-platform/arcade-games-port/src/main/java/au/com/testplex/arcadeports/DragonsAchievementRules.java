package au.com.testplex.arcadeports;

import java.util.List;

/** Small, deterministic conditions used by Dragons' live achievement detectors. */
final class DragonsAchievementRules {
    static final int FREEFALLIN_HORIZON_TICKS = 60;
    private static final double GRAVITY_PER_TICK = 0.08D;
    private static final double VERTICAL_DRAG = 0.98D;

    private DragonsAchievementRules() { }

    static boolean gliderDistanceReached(double horizontalDistance, boolean landed) {
        return !landed && Double.isFinite(horizontalDistance) && horizontalDistance >= 25.0D;
    }

    static boolean tamerFallElimination(boolean controlledDragonHit, String reason, long elapsedMillis) {
        if (!controlledDragonHit || elapsedMillis < 0L || elapsedMillis > 10_000L || reason == null) return false;
        String normalized = reason.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("fell") || normalized.contains("void");
    }

    /**
     * Forecasts the next three seconds of a falling player's vertical path.
     * A floor sample is the top Y of a solid block under the player's predicted
     * position for that tick, or NaN when the column is open. The forecast uses
     * the same fall-damage reduction as the Dragons runtime.
     */
    static boolean wouldDieFromFallWithinThreeSeconds(double startY, double velocityY,
            double existingFallDistance, double health, double voidDamageY,
            List<Double> floorTopsByTick, double fallDamageReduction) {
        if (!Double.isFinite(startY) || !Double.isFinite(velocityY) || velocityY >= 0.0D
                || !Double.isFinite(existingFallDistance) || !Double.isFinite(health)
                || !Double.isFinite(voidDamageY) || floorTopsByTick == null) return false;

        double y = startY;
        double fallDistance = Math.max(0.0D, existingFallDistance);
        double verticalVelocity = velocityY;
        int ticks = Math.min(FREEFALLIN_HORIZON_TICKS, floorTopsByTick.size());
        for (int tick = 0; tick < ticks; tick++) {
            double previousY = y;
            verticalVelocity = (verticalVelocity - GRAVITY_PER_TICK) * VERTICAL_DRAG;
            y += verticalVelocity;
            if (verticalVelocity < 0.0D) fallDistance += previousY - y;
            if (y <= voidDamageY) return true;

            Double floorTop = floorTopsByTick.get(tick);
            if (floorTop != null && Double.isFinite(floorTop)
                    && previousY > floorTop && y <= floorTop) {
                double rawFallDamage = Math.max(0.0D, fallDistance - 3.0D);
                return Math.max(0.0D, rawFallDamage - Math.max(0.0D, fallDamageReduction)) >= health;
            }
        }
        return false;
    }
}
