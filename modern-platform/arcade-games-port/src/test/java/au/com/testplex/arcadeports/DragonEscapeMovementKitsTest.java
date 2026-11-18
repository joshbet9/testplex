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
    void wallKickMustBeASecondPressInMidairAndRespectsCooldown() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        assertFalse(state.pressedForWallKick(true, true, 1L));
        assertFalse(state.pressedForWallKick(true, false, 2L));
        assertFalse(state.pressedForWallKick(false, false, 3L));
        assertTrue(state.pressedForWallKick(true, false, 4L));
        state.nextWallKickAt = 1_504L;
        state.pressedForWallKick(false, false, 5L);
        assertFalse(state.pressedForWallKick(true, false, 10L));
        state.pressedForWallKick(false, false, 1_505L);
        assertTrue(state.pressedForWallKick(true, false, 1_506L));
    }

    @Test
    void championsInspiredKickHasStrongForwardAndUpwardMomentum() {
        Vector next = DragonEscapeMovementKits.wallKickVelocity(new Vector(0, -0.2, 1));
        assertEquals(DragonEscapeMovementKits.WALL_KICK_FORWARD_SPEED, next.getZ(), 0.0001);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_UPWARD_SPEED, next.getY(), 0.0001);
    }

    @Test
    void wallKickRejectsProximityAndFloorOnlyContact() {
        BoundingBox player = new BoundingBox(0, 0, 0, 0.6, 1.8, 0.6);
        Vector facingAway = new Vector(1, 0, 0);
        // Solid west wall with its east face exactly against the player.
        assertEquals(new Vector(1, 0, 0), DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(-1, 0, 0, 0, 2, 0.6), facingAway));
        // A 0.15-block gap is NOT physical contact.
        assertNull(DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(-1.15, 0, 0, -0.15, 2, 0.6), facingAway));
        // A block touching only the player's boots is a floor, not a wall.
        assertNull(DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(-1, -1, 0, 0, 0, 0.6), facingAway));
        // Must have the wall behind you, as in Champions' Assassin Leap.
        assertNull(DragonEscapeMovementKits.contactNormal(
                player, new BoundingBox(-1, 0, 0, 0, 2, 0.6), new Vector(-1, 0, 0)));
    }
}
