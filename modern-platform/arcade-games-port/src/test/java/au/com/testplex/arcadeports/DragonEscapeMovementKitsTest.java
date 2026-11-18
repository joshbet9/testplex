package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class DragonEscapeMovementKitsTest {
    @Test
    void holdingGroundJumpDoesNotTriggerFlutterAfterTakeoff() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        state.tick(true, true); // Ordinary on-ground jump.
        for (int tick = 0; tick < 45; tick++) {
            state.tick(true, false); // Same held Space input through takeoff.
            assertFalse(state.fluttering, "A held ground jump must not become a flutter");
            assertFalse(state.activated);
        }
        assertEquals(1.0D, state.charge, 0.0001D);
        state.tick(false, false); // Airborne release explicitly arms the next press.
        state.tick(true, false);
        assertTrue(state.fluttering);
        assertTrue(state.activated);
        assertEquals(1, state.flutterTicks);
    }

    @Test
    void aFreshSpacePressWithoutAnEarlierAirborneReleaseCannotFlutter() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        // Even if the input polling missed the initial grounded tick, do not
        // interpret the first observed held-airborne jump as a second press.
        state.tick(true, false);
        assertFalse(state.fluttering);
        state.tick(false, false);
        state.tick(true, false);
        assertTrue(state.fluttering);
    }

    @Test
    void heldSpaceDoesExactlyOneFlutterArcAndCannotAutoRepeat() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        state.tick(false, true);
        state.tick(false, false);
        state.tick(true, false);
        assertTrue(state.activated);
        for (int tick = 1; tick < DragonEscapeMovementKits.FLUTTER_TOTAL_TICKS; tick++) {
            state.tick(true, false);
            assertTrue(state.fluttering);
        }
        assertEquals(DragonEscapeMovementKits.FLUTTER_TOTAL_TICKS, state.flutterTicks);
        state.tick(true, false);
        assertFalse(state.fluttering);
        assertTrue(state.ended, "Completing one arc should trigger a forward carry");
        for (int tick = 0; tick < 80; tick++) {
            state.tick(true, false);
            assertFalse(state.fluttering, "Holding Space must never repeat a flutter");
            assertFalse(state.activated);
        }
        state.tick(false, false);
        state.tick(true, false);
        assertTrue(state.activated, "Release and press in midair should chain the next flutter");
        assertEquals(1, state.flutterTicks);
    }

    @Test
    void releasingSpaceEndsFlutterAndASecondPressRestartsIt() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        state.tick(false, false);
        state.tick(true, false);
        for (int tick = 0; tick < 7; tick++) state.tick(true, false);
        double chargeBeforeRelease = state.charge;
        state.tick(false, false);
        assertFalse(state.fluttering);
        assertTrue(state.ended);
        assertTrue(state.charge > chargeBeforeRelease);
        state.tick(true, false);
        assertTrue(state.fluttering);
        assertTrue(state.activated);
        assertEquals(1, state.flutterTicks);
    }

    @Test
    void flutterExhaustionCannotRestartUntilFreshPressAndEnoughEnergy() {
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        state.charge = DragonEscapeMovementKits.FLUTTER_ACTIVATION_COST
                + 2.0D * DragonEscapeMovementKits.FLUTTER_DRAIN_PER_TICK;
        state.tick(false, false);
        state.tick(true, false);
        assertTrue(state.fluttering);
        for (int tick = 0; tick < 12; tick++) state.tick(true, false);
        assertFalse(state.fluttering);
        assertFalse(state.activated);
        for (int tick = 0; tick < 30; tick++) {
            state.tick(true, false);
            assertFalse(state.fluttering);
        }
        state.tick(false, false);
        for (int tick = 0; tick < 15; tick++) state.tick(false, false);
        state.tick(true, false);
        assertTrue(state.fluttering);
    }

    @Test
    void flutterRisesOneBlockBeforeItsSingleYoshiArc() {
        Vector current = new Vector(0.4D, -0.6D, 0.0D);
        Vector direction = new Vector(0, 0, 1);
        assertEquals(5, DragonEscapeMovementKits.FLUTTER_LIFT_TICKS);
        assertEquals(0.2D, DragonEscapeMovementKits.FLUTTER_LIFT_VERTICAL_SPEED, 0.00001D);
        double totalInitialLift = 0.0D;
        for (int tick = 1; tick <= DragonEscapeMovementKits.FLUTTER_LIFT_TICKS; tick++) {
            Vector velocity = DragonEscapeMovementKits.flutterVelocity(current, direction, tick);
            totalInitialLift += velocity.getY();
            assertEquals(DragonEscapeMovementKits.FLUTTER_HORIZONTAL_SPEED,
                    velocity.getZ(), 0.00001D);
        }
        assertEquals(1.0D, totalInitialLift, 0.00001D);
        Vector firstRise = DragonEscapeMovementKits.flutterVelocity(
                current, direction, DragonEscapeMovementKits.FLUTTER_LIFT_TICKS + 1);
        Vector dip = DragonEscapeMovementKits.flutterVelocity(
                current, direction, DragonEscapeMovementKits.FLUTTER_LIFT_TICKS + 15);
        Vector finalRise = DragonEscapeMovementKits.flutterVelocity(
                current, direction, DragonEscapeMovementKits.FLUTTER_TOTAL_TICKS);
        assertTrue(firstRise.getY() > 0.15D);
        assertTrue(dip.getY() < -0.15D);
        assertTrue(finalRise.getY() > 0.15D);
        assertTrue(finalRise.getY() > dip.getY());
        assertEquals(DragonEscapeMovementKits.FLUTTER_HORIZONTAL_SPEED,
                firstRise.getZ(), 0.00001D);
    }

    @Test
    void flutterHorizontalSteeringTargetsFullGroundSprint() {
        double expected = 1.25D * 0.8D * 5.612D / 20.0D;
        assertEquals(expected, DragonEscapeMovementKits.FLUTTER_HORIZONTAL_SPEED, 0.00001D);
        Vector facingNorth = new Vector(0, 0.2, -1);
        Vector direction = DragonEscapeMovementKits.flutterTravelDirection(
                new Vector(0.01, 0, 0), facingNorth, true, false, false, false);
        assertEquals(0, direction.getX(), 0.00001D);
        assertTrue(direction.getZ() < -0.99D);
        Vector velocity = DragonEscapeMovementKits.flutterVelocity(
                new Vector(0.01, -0.3, 0), direction, 1);
        assertEquals(expected, Math.hypot(velocity.getX(), velocity.getZ()), 0.00001D);

        Vector right = DragonEscapeMovementKits.flutterTravelDirection(
                new Vector(), facingNorth, false, false, false, true);
        assertTrue(right.getX() > 0.99D);
        Vector diagonal = DragonEscapeMovementKits.flutterTravelDirection(
                new Vector(), facingNorth, true, false, false, true);
        assertEquals(1.0D, diagonal.length(), 0.00001D,
                "Diagonal input should not exceed intended horizontal flutter speed");
        Vector momentumDirection = DragonEscapeMovementKits.flutterTravelDirection(
                new Vector(0.3D, 0, 0), facingNorth, false, false, false, false);
        assertTrue(momentumDirection.getX() > 0.99D,
                "Without new WASD input, retain the airborne travel direction");
    }

    @Test
    void endingFlutterAddsBriefCappedForwardMomentum() {
        Vector active = DragonEscapeMovementKits.flutterVelocity(
                new Vector(), new Vector(0, 0, 1), 28);
        Vector carry = DragonEscapeMovementKits.flutterEndVelocity(active);
        assertEquals(DragonEscapeMovementKits.FLUTTER_END_FORWARD_SPEED,
                Math.hypot(carry.getX(), carry.getZ()), 0.00001D);
        assertTrue(carry.getZ() > active.getZ());
        assertEquals(active.getY(), carry.getY(), 0.00001D);
    }

    @Test
    void flutterKeepsReducedXpDrainAndIdleRegeneration() {
        assertEquals(0.0175D, DragonEscapeMovementKits.FLUTTER_DRAIN_PER_TICK, 0.00001D);
        assertEquals(0.0125D * 1.40D, DragonEscapeMovementKits.FLUTTER_DRAIN_PER_TICK, 0.00001D);
        DragonEscapeMovementKits.FlutterState state = new DragonEscapeMovementKits.FlutterState();
        state.tick(false, false);
        state.tick(true, false);
        for (int tick = 1; tick < DragonEscapeMovementKits.FLUTTER_TOTAL_TICKS; tick++) {
            state.tick(true, false);
        }
        assertTrue(state.charge > 0.25D);
        double before = state.charge;
        state.tick(true, false);
        assertTrue(state.charge > before, "XP should regenerate after the finite arc expires");
    }

    @Test
    void flutterStickAndWallKickKeepDistinctSounds() {
        assertEquals("ENTITY_SLIME_JUMP", DragonEscapeMovementKits.FLUTTER_SOUND_NAME);
        assertEquals("BLOCK_SLIME_BLOCK_PLACE", DragonEscapeMovementKits.WALL_CLING_SOUND_NAME);
        assertEquals("ENTITY_FIREWORK_ROCKET_LAUNCH", DragonEscapeMovementKits.WALL_KICK_SOUND_NAME);
    }

    @Test
    void wallKicksRequireAnAlreadyActiveAirborneWallStick() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        assertFalse(state.tickWallCling(true, true, true, 1_000L));
        assertEquals(1.0D, state.charge, 0.00001D);
        assertFalse(state.pressedForWallKick(true, true, false));
        assertFalse(state.pressedForWallKick(true, false, false));
        assertFalse(state.pressedForWallKick(false, false, false));
        assertFalse(state.pressedForWallKick(true, false, false),
                "An airborne Space press without Shift-stick cannot wall-kick");

        state.pressedForWallKick(false, false, false);
        assertFalse(state.tickWallCling(true, false, false, 1_000L));
        assertTrue(state.tickWallCling(true, false, true, 1_000L));
        assertTrue(state.clingActivated);
        assertFalse(state.pressedForWallKick(true, false, true),
                "A wall kick must not occur on the same tick Shift-stick begins");
        assertFalse(state.pressedForWallKick(false, false, true));
        assertTrue(state.tickWallCling(true, false, true, 1_000L));
        assertFalse(state.clingActivated);
        assertTrue(state.pressedForWallKick(true, false, true));
        state.cancelWallClingForKick();
        assertFalse(state.clingActive);
        assertFalse(state.tickWallCling(true, false, true, 1_000L),
                "Cannot use a single Shift-stick for multiple kicks");
        assertFalse(state.pressedForWallKick(false, false, false));
        assertFalse(state.pressedForWallKick(true, false, false));
    }

    @Test
    void wallStickLastsLongerThanHalfSecondAndDrainsContinuously() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        assertTrue(state.tickWallCling(true, false, true, 1_000L));
        double afterFirst = state.charge;
        assertEquals(1.0D - DragonEscapeMovementKits.WALL_CLING_DRAIN_PER_TICK,
                afterFirst, 0.00001D);
        for (int tick = 0; tick < 30; tick++) {
            assertTrue(state.tickWallCling(true, false, true, 1_000L),
                    "Cling must not expire at 10 ticks / 0.5 seconds");
        }
        assertTrue(state.clingActive);
        assertEquals(1.0D - 31 * DragonEscapeMovementKits.WALL_CLING_DRAIN_PER_TICK,
                state.charge, 0.00001D);
    }

    @Test
    void wallStickStopsOnEnergyExhaustionAndCannotRenewFromSameHold() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        int activeTicks = 0;
        for (int tick = 0; tick < 110; tick++) {
            if (state.tickWallCling(true, false, true, 1_000L)) activeTicks++;
        }
        assertTrue(activeTicks >= 79 && activeTicks <= 81);
        assertFalse(state.clingActive);
        assertFalse(state.tickWallCling(true, false, true, 1_000L));
        assertFalse(state.pressedForWallKick(true, false, false),
                "Out-of-energy clings cannot generate a wall kick");
        // Regenerate while not sticking; continuing to hold Shift must not
        // automatically restart the cling without a new Shift press.
        for (int tick = 0; tick < 105; tick++) {
            assertFalse(state.tickWallCling(true, false, true, 1_000L));
        }
        assertTrue(state.charge > 0.9D);
        state.tickWallCling(false, false, true, 1_000L);
        assertTrue(state.tickWallCling(true, false, true, 1_000L));
    }

    @Test
    void landingReleaseAndPhysicalContactLossCancelWallStick() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        assertFalse(state.tickWallCling(true, true, true, 1_000L));
        assertFalse(state.tickWallCling(true, false, false, 1_000L));
        assertEquals(1.0D, state.charge, 0.00001D);
        assertTrue(state.tickWallCling(true, false, true, 1_000L));
        assertFalse(state.tickWallCling(true, false, false, 1_000L));
        assertFalse(state.tickWallCling(true, false, true, 1_000L),
                "Contact loss cancels the stick until a new Shift press");
        state.tickWallCling(false, false, true, 1_000L);
        assertTrue(state.tickWallCling(true, false, true, 1_000L));
        assertFalse(state.tickWallCling(true, true, true, 1_000L));
        assertFalse(state.tickWallCling(true, false, true, 1_000L),
                "Holding Shift through landing cannot re-arm on takeoff");
        state.tickWallCling(false, false, true, 1_000L);
        assertTrue(state.tickWallCling(true, false, true, 1_000L));
        assertFalse(state.tickWallCling(false, false, true, 1_000L));
        assertFalse(state.clingActive);
    }

    @Test
    void wallStickXpRegeneratesOffWallAndKickNeedsAnotherStick() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        assertTrue(state.tickWallCling(true, false, true, 1_000L));
        for (int tick = 0; tick < 15; tick++) state.tickWallCling(true, false, true, 1_000L);
        double spent = state.charge;
        assertTrue(spent < 1.0D);
        state.cancelWallClingForKick();
        state.tickWallCling(true, false, true, 1_000L);
        assertTrue(state.charge > spent);
        assertFalse(state.clingActive);
        state.tickWallCling(false, false, true, 1_000L);
        assertTrue(state.tickWallCling(true, false, true, 1_000L),
                "A fresh Shift-stick can start immediately without a cooldown");
        assertTrue(state.clingActivated);
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
    void wallJumpStrengthScalesTheActualDragonEscapeLeap() {
        Vector facing = new Vector(0, 0, 1);
        Vector outward = new Vector(0, 0, 1);
        Vector fullLeap = ArcadePortRules.dragonEscapeLeapVelocity(facing, false);
        Vector first = DragonEscapeMovementKits.wallKickVelocity(facing, outward, 0.50D);
        Vector second = DragonEscapeMovementKits.wallKickVelocity(facing, outward, 0.75D);
        Vector third = DragonEscapeMovementKits.wallKickVelocity(facing, outward, 1.00D);
        assertEquals(fullLeap.clone().multiply(0.50D), first);
        assertEquals(fullLeap.clone().multiply(0.75D), second);
        assertEquals(fullLeap, third);
        assertTrue(first.length() < second.length());
        assertTrue(second.length() < third.length());
        Vector up = DragonEscapeMovementKits.wallKickVelocity(
                new Vector(0, 1, 1), outward, 1.0D);
        Vector down = DragonEscapeMovementKits.wallKickVelocity(
                new Vector(0, -1, 1), outward, 1.0D);
        assertTrue(up.getY() > third.getY());
        assertTrue(down.getY() < third.getY());
    }

    @Test
    void lookingIntoWallAlwaysPushesOutByAStrengthScaledMinimum() {
        Vector normal = new Vector(1, 0, 0);
        for (double strength : new double[] {0.50D, 0.75D, 1.0D}) {
            Vector intoWall = DragonEscapeMovementKits.wallKickVelocity(
                    new Vector(-1, 0, 0), normal, strength);
            assertEquals(DragonEscapeMovementKits.WALL_KICK_MIN_OUTWARD_SPEED * strength,
                    intoWall.getX(), 0.00001D);
            assertEquals(ArcadePortRules.dragonEscapeLeapVelocity(
                    new Vector(-1, 0, 0), false).getY() * strength,
                    intoWall.getY(), 0.00001D);
        }
        assertEquals(new Vector(), DragonEscapeMovementKits.wallKickVelocity(
                new Vector(), normal, 0.5D));
    }

    @Test
    void wallClingNeverSuppliesUpwardClimbingVelocity() {
        Vector hold = DragonEscapeMovementKits.wallClingVelocity();
        assertEquals(0.0D, hold.getX(), 0.00001D);
        assertEquals(0.0D, hold.getY(), 0.00001D);
        assertEquals(0.0D, hold.getZ(), 0.00001D);
    }

    @Test
    void cooldownStartsOnlyOnLandingFollowingWallJumpChain() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        long now = 5_000L;
        assertFalse(state.wallKickCoolingDown(now));
        assertEquals(0.5D, state.nextWallKickStrength(), 0.00001D);
        assertTrue(state.tickWallCling(true, false, true, now));
        state.completeWallKick();
        assertEquals(0.75D, state.nextWallKickStrength(), 0.00001D);
        assertFalse(state.wallKickCoolingDown(now));
        state.tickWallCling(false, false, false, now + 100L);
        assertTrue(state.tickWallCling(true, false, true, now + 200L));
        state.completeWallKick();
        assertEquals(1.0D, state.nextWallKickStrength(), 0.00001D);
        assertFalse(state.wallKickCoolingDown(now + 200L));
        state.tickWallCling(false, false, false, now + 300L);
        assertTrue(state.tickWallCling(true, false, true, now + 400L));
        state.completeWallKick();
        assertEquals(1.0D, state.nextWallKickStrength(), 0.00001D,
                "The third and later airborne jumps remain full-strength");
        assertFalse(state.wallKickCoolingDown(now + 400L));

        long landing = now + 600L;
        assertFalse(state.tickWallCling(true, true, true, landing));
        assertTrue(state.wallKickCoolingDown(landing));
        assertEquals(0, state.wallKicksThisAirborneChain);
        assertEquals(0.5D, state.nextWallKickStrength(), 0.00001D);
        state.tickWallCling(false, false, false, landing + 100L);
        assertFalse(state.tickWallCling(true, false, true, landing + 9_999L));
        assertFalse(state.tickWallCling(true, false, true, landing + 10_000L),
                "Holding Shift through cooldown must not automatically start a cling");
        state.tickWallCling(false, false, true, landing + 10_001L);
        assertTrue(state.tickWallCling(true, false, true, landing + 10_002L),
                "A fresh Shift after ten seconds enables the next chain");
    }

    @Test
    void landingWithoutKickingDoesNotStartWallKickCooldown() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        state.tickWallCling(false, true, false, 2_000L);
        state.tickWallCling(true, false, true, 2_200L);
        state.tickWallCling(false, false, false, 2_300L);
        state.tickWallCling(false, true, false, 2_400L);
        assertFalse(state.wallKickCoolingDown(2_400L));
        assertEquals(0.5D, state.nextWallKickStrength(), 0.00001D);
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
