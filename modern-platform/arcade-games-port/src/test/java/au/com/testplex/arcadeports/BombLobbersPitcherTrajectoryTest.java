package au.com.testplex.arcadeports;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BombLobbersPitcherTrajectoryTest {
    @Test
    void correctedThrowReachesTheSelectedOpposingSurfaceWithABoundedVelocity() {
        Vector result = BombLobbersPitcherTrajectory.velocityToTarget(
                new Vector(1.0D, 0.0D, 0.0D),
                new Vector(1.95D, 0.1D, 0.0D),
                20.0D, 1.0D);

        assertNotNull(result);
        assertTrue(result.length() <= 3.5D);
        Vector impact = simulate(result, 60);
        assertEquals(20.0D, impact.getX(), 0.01D);
        assertEquals(1.0D, impact.getY(), 0.01D);
        assertEquals(0.0D, impact.getZ(), 0.01D);
    }

    @Test
    void invalidAimOrDistanceDoesNotCreateAnUnsafeVelocity() {
        Vector normal = new Vector(2.0D, 0.1D, 0.0D);
        assertNull(BombLobbersPitcherTrajectory.velocityToTarget(
                new Vector(), normal, 20.0D, 1.0D));
        assertNull(BombLobbersPitcherTrajectory.velocityToTarget(
                new Vector(1.0D, 0.0D, 0.0D), normal, Double.NaN, 1.0D));
        assertNull(BombLobbersPitcherTrajectory.velocityToTarget(
                new Vector(1.0D, 0.0D, 0.0D), normal, 20.0D, 1.0E6D));
    }

    private static Vector simulate(Vector initial, int maxTicks) {
        Vector position = new Vector();
        Vector velocity = initial.clone();
        for (int tick = 0; tick < maxTicks; tick++) {
            position.add(velocity);
            velocity.multiply(0.98D);
            velocity.setY(velocity.getY() - 0.04D);
            if (position.getX() >= 20.0D) break;
        }
        return position;
    }
}
