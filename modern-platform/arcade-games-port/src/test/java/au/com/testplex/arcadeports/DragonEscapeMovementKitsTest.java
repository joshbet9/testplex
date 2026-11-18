package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class DragonEscapeMovementKitsTest {
    @Test
    void flutterStartsAfterNormalHeldJumpAndDrainsXp() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        state.tick(true, true);
        assertFalse(state.fluttering);
        state.tick(true, false);
        state.tick(true, false);
        assertFalse(state.fluttering);
        state.tick(true, false);
        assertTrue(state.fluttering);
        assertTrue(state.activated);
        assertTrue(state.charge < 1.0D);
    }

    @Test
    void releaseAndRepressChainsFlutterWhileRecoveringBetweenBursts() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        for (int tick = 0; tick < 5; tick++) state.tick(true, false);
        double beforeRelease = state.charge;
        state.tick(false, false);
        assertFalse(state.fluttering);
        assertTrue(state.charge > beforeRelease);
        state.tick(true, false);
        assertTrue(state.activated);
        assertTrue(state.fluttering);
    }

    @Test
    void exhaustedFlutterStopsAndRequiresAnotherPressToRestart() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        for (int tick = 0; tick < 100; tick++) state.tick(true, false);
        assertFalse(state.fluttering);
        double before = state.charge;
        for (int tick = 0; tick < 12; tick++) state.tick(true, false);
        assertFalse(state.fluttering);
        assertTrue(state.charge > before);
        state.tick(false, false);
        state.tick(true, false);
        assertTrue(state.fluttering);
    }

    @Test
    void flutterCapsHorizontalSpeedAndSlowsFalling() {
        Vector result = DragonEscapeMovementKits.flutterVelocity(new Vector(0.5D, -0.6D, 0.0D), false);
        assertEquals(DragonEscapeMovementKits.FLUTTER_HORIZONTAL_MAX, result.getX(), 0.0001D);
        assertEquals(DragonEscapeMovementKits.FLUTTER_FALL_SPEED, result.getY(), 0.0001D);
        Vector reactivated = DragonEscapeMovementKits.flutterVelocity(new Vector(0.1D, -0.3D, 0.0D), true);
        assertEquals(DragonEscapeMovementKits.FLUTTER_START_Y, reactivated.getY(), 0.0001D);
    }

    @Test
    void flutterAndWallKickUseTheirRequestedSoundEffects() {
        assertEquals("ENTITY_SLIME_JUMP", DragonEscapeMovementKits.FLUTTER_SOUND_NAME);
        assertEquals("ENTITY_FIREWORK_ROCKET_LAUNCH", DragonEscapeMovementKits.WALL_KICK_SOUND_NAME);
    }

    @Test
    void wallKicksChainOnEveryFreshSpacePressWithoutATimer() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        // On-ground and midair contact are both eligible; the caller checks
        // the player's physical contact with a wall on each activation.
        assertTrue(state.pressedForWallKick(true));
        assertFalse(state.pressedForWallKick(true)); // Holding is not auto-repeat.
        assertFalse(state.pressedForWallKick(false));
        assertTrue(state.pressedForWallKick(true));  // Immediate second kick.
        assertFalse(state.pressedForWallKick(false));
        assertTrue(state.pressedForWallKick(true));  // Immediate third kick.
        assertFalse(state.pressedForWallKick(true));
    }

    @Test
    void failedWallContactStillRequiresReleaseBeforeAnotherKickAttempt() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        // The input edge gets consumed even if the caller detects no wall.
        assertTrue(state.pressedForWallKick(true));
        assertFalse(state.pressedForWallKick(true));
        assertFalse(state.pressedForWallKick(false));
        assertTrue(state.pressedForWallKick(true));
    }

    @Test
    void wallKickFollowsYawAndPitchWithSubstantialMomentum() {
        Vector outward = new Vector(1, 0, 0);
        Vector level = DragonEscapeMovementKits.wallKickVelocity(new Vector(0, 0, 1), outward);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_FORWARD_SPEED, level.getZ(), 0.0001D);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_UPWARD_SPEED, level.getY(), 0.0001D);
        assertTrue(level.getX() >= DragonEscapeMovementKits.WALL_KICK_MIN_OUTWARD_SPEED);

        Vector up = DragonEscapeMovementKits.wallKickVelocity(new Vector(0, 1, 1), outward);
        Vector down = DragonEscapeMovementKits.wallKickVelocity(new Vector(0, -1, 1), outward);
        assertTrue(up.getY() > level.getY(), "Looking up must increase vertical momentum");
        assertTrue(down.getY() < level.getY(), "Looking down must reduce vertical momentum");
        assertTrue(level.getZ() > up.getZ(), "A steep look trades horizontal distance for height");
        assertEquals(0.0D, up.getX() - level.getX(), 0.0001D);

        Vector west = DragonEscapeMovementKits.wallKickVelocity(
                new Vector(-1, 0, 0), new Vector(0, 0, 1));
        assertTrue(west.getX() < -1.0D, "Yaw should steer strongly to the west");
    }

    @Test
    void lookingAtWallStillKicksOutInsteadOfBeingAbsorbedByCollision() {
        Vector normal = new Vector(1, 0, 0);
        Vector intoWall = DragonEscapeMovementKits.wallKickVelocity(new Vector(-1, 0, 0), normal);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_MIN_OUTWARD_SPEED,
                intoWall.getX(), 0.0001D);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_UPWARD_SPEED, intoWall.getY(), 0.0001D);
        Vector vertical = DragonEscapeMovementKits.wallKickVelocity(new Vector(0, 1, 0), normal);
        assertTrue(vertical.getY() > intoWall.getY());
        assertTrue(vertical.getX() >= DragonEscapeMovementKits.WALL_KICK_MIN_OUTWARD_SPEED);
    }

    @Test
    void wallKickRequiresRealSideContactRegardlessOfFacingDirection() {
        BoundingBox player = new BoundingBox(0, 0, 0, 0.6, 1.8, 0.6);
        BoundingBox westWall = new BoundingBox(-1, 0, 0, 0, 2, 0.6);
        // Facing away, into or along the wall must all be equally eligible.
        assertEquals(new Vector(1, 0, 0), DragonEscapeMovementKits.contactNormal(player, westWall));
        assertEquals(new Vector(-1, 0, 0), DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(0.6, 0, 0, 1.6, 2, 0.6)));
        assertEquals(new Vector(0, 0, 1), DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(0, 0, -1, 0.6, 2, 0)));
        assertEquals(new Vector(0, 0, -1), DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(0, 0, 0.6, 0.6, 2, 1.6)));
        // A 0.15-block gap must not count as contact.
        assertNull(DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(-1.15, 0, 0, -0.15, 2, 0.6)));
        // A block only under the player's feet is a floor, not a wall.
        assertNull(DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(-1, -1, 0, 0, 0, 0.6)));
        // A ceiling touching the head is not a wall either.
        assertNull(DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(0, 1.8, 0, 0.6, 2.8, 0.6)));
    }

}
