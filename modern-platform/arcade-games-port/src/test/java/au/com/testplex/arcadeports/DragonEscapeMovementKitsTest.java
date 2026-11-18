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
    void flutterTracesRepeatedYoshiStyleUpDownUpArcsWithoutHovering() {
        Vector current = new Vector(0.5D, -0.6D, 0.0D);
        Vector firstRise = DragonEscapeMovementKits.flutterVelocity(current, 1);
        Vector descending = DragonEscapeMovementKits.flutterVelocity(current, 15);
        Vector renewedRise = DragonEscapeMovementKits.flutterVelocity(current, 29);
        assertTrue(firstRise.getY() > 0.15D, "First phase should actually climb");
        assertTrue(descending.getY() < -0.15D, "Halfway into the wave should descend");
        assertEquals(firstRise.getY(), renewedRise.getY(), 0.00001D,
                "Held flutter should rise again within the same burst");
        assertEquals(DragonEscapeMovementKits.FLUTTER_HORIZONTAL_MAX, firstRise.getX(), 0.0001D);
        assertTrue(firstRise.getY() > descending.getY());
    }

    @Test
    void flutterUsesOneThirdLessEnergyAndLastsLongerThanOldHover() {
        assertEquals(0.0125D, DragonEscapeMovementKits.FLUTTER_DRAIN_PER_TICK, 0.00001D);
        assertTrue(DragonEscapeMovementKits.FLUTTER_DRAIN_PER_TICK <= 0.019D * 0.67D);
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        for (int tick = 0; tick < 60; tick++) state.tick(true, false);
        assertTrue(state.fluttering, "One continuous hold should last at least three seconds");
        assertTrue(state.flutterTicks >= 55);
        assertTrue(state.charge > 0.1D);
        for (int tick = 0; tick < 50; tick++) state.tick(true, false);
        assertFalse(state.fluttering, "Exhaustion should still stop sustained flight");
    }

    @Test
    void flutterReleaseAndRepressResetsTheArcToTheFirstRisingPhase() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        for (int tick = 0; tick < 21; tick++) state.tick(true, false);
        assertTrue(state.flutterTicks >= 19);
        state.tick(false, false);
        assertEquals(0, state.flutterTicks);
        state.tick(true, false);
        assertTrue(state.activated);
        assertEquals(1, state.flutterTicks);
        assertTrue(DragonEscapeMovementKits.flutterVelocity(new Vector(), state.flutterTicks).getY() > 0.15D);
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
    void crouchWallClingLastsHalfASecondAndDoesNotAutomaticallyRepeat() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        for (int tick = 0; tick < DragonEscapeMovementKits.WALL_CLING_MAX_TICKS; tick++) {
            assertTrue(state.tickWallCling(true, false, true));
        }
        assertEquals(0, state.wallClingTicksRemaining);
        assertFalse(state.tickWallCling(true, false, true));
        assertFalse(state.tickWallCling(true, false, true));
        assertFalse(state.tickWallCling(false, false, true));
        assertTrue(state.tickWallCling(true, false, true), "Release/repress Shift permits a new cling");
    }

    @Test
    void crouchWallClingRequiresAirborneWallContactAndCancelsOnKick() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        assertFalse(state.tickWallCling(true, true, true), "Ground crouching should remain normal crouching");
        assertFalse(state.tickWallCling(true, false, false), "No wall contact means no cling");
        assertTrue(state.tickWallCling(true, false, true));
        assertTrue(state.wallClingTicksRemaining > 0);
        state.cancelWallClingForKick();
        assertEquals(0, state.wallClingTicksRemaining);
        assertFalse(state.tickWallCling(true, false, true), "A successful kick cannot instantly re-cling");
        assertFalse(state.tickWallCling(false, false, true));
        assertTrue(state.tickWallCling(true, false, true));
        assertFalse(state.tickWallCling(true, false, false), "Losing wall contact cancels the hold");
        Vector hold = DragonEscapeMovementKits.wallClingVelocity();
        assertEquals(0.0D, hold.getX(), 0.00001D);
        assertTrue(hold.getY() > 0.0D && hold.getY() < 0.1D);
    }

    @Test
    void acrobatReducesOnlyItsOwnFallDamageByHalf() {
        assertEquals(4.0D, DragonEscapeMovementKits.acrobatFallDamage("acrobat", 8.0D), 0.0001D);
        assertEquals(8.0D, DragonEscapeMovementKits.acrobatFallDamage("jumper", 8.0D), 0.0001D);
        assertEquals(0.0D, DragonEscapeMovementKits.acrobatFallDamage("acrobat", 0.0D), 0.0001D);
        assertEquals(1, ArcadePortRules.dragonEscapeLeapUses("acrobat"));
        assertEquals(0, ArcadePortRules.dragonEscapeLeapUses("flutterfly"));
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
