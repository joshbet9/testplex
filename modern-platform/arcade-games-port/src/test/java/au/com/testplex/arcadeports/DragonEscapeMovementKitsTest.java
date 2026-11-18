package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class DragonEscapeMovementKitsTest {
    @Test
    void featherBounceBeginsWithThreeChargesAndRejectsGroundClicks() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        assertEquals(3, DragonEscapeMovementKits.FLUTTER_MAX_CHARGES);
        assertEquals(8_000L, DragonEscapeMovementKits.FLUTTER_CHARGE_RECHARGE_MILLIS);
        assertEquals(3, flutter.charges);
        assertFalse(flutter.useBounce(true, 1_000L));
        assertEquals(3, flutter.charges);
        assertTrue(flutter.useBounce(false, 1_000L));
        assertEquals(2, flutter.charges);
        assertEquals(9_000L, flutter.nextChargeAtMillis);
    }

    @Test
    void featherCanSpendThreeChargesInSameAirbornePeriodWithoutSpace() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        assertTrue(flutter.useBounce(false, 1_000L));
        assertTrue(flutter.useBounce(false, 2_000L));
        assertTrue(flutter.useBounce(false, 3_000L));
        assertEquals(0, flutter.charges);
        assertFalse(flutter.useBounce(false, 3_001L));
        assertEquals(9_000L, flutter.nextChargeAtMillis,
                "Spending more charges must never postpone the earlier recharge");
    }

    @Test
    void featherChargesRestoreOneEveryEightSecondsIncludingInAir() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        for (int i = 0; i < 3; i++) assertTrue(flutter.useBounce(false, 1_000L + i));
        assertFalse(flutter.recharge(8_999L));
        assertTrue(flutter.recharge(9_000L));
        assertEquals(1, flutter.charges);
        assertEquals(17_000L, flutter.nextChargeAtMillis);
        assertTrue(flutter.recharge(17_000L));
        assertEquals(2, flutter.charges);
        assertTrue(flutter.useBounce(false, 17_001L));
        assertEquals(1, flutter.charges);
        assertEquals(25_000L, flutter.nextChargeAtMillis);
        assertTrue(flutter.recharge(25_000L));
        assertTrue(flutter.recharge(33_000L));
        assertEquals(3, flutter.charges);
        assertEquals(0L, flutter.nextChargeAtMillis);
        assertFalse(flutter.recharge(100_000L));
    }

    @Test
    void featherChargeProgressFillsDuringEightSecondRecharge() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        assertEquals(1.0F, flutter.rechargeProgress(1_000L), 0.00001F);
        flutter.useBounce(false, 1_000L);
        assertEquals(0.0F, flutter.rechargeProgress(1_000L), 0.00001F);
        assertEquals(0.5F, flutter.rechargeProgress(5_000L), 0.00001F);
        assertEquals(1.0F, flutter.rechargeProgress(9_000L), 0.00001F);
        flutter.recharge(9_000L);
        assertEquals(1.0F, flutter.rechargeProgress(9_000L), 0.00001F);
    }

    @Test
    void featherRightClickBounceRetainsFlutterHorizontalSpeedAndSteering() {
        double sprint = 1.25D * 0.8D * 5.612D / 20.0D;
        assertEquals(sprint, DragonEscapeMovementKits.FLUTTER_HORIZONTAL_SPEED, 0.00001D);
        Vector north = DragonEscapeMovementKits.flutterBounceVelocity(
                new Vector(0, -0.6, 0), new Vector(0, 0.2, -1),
                true, false, false, false);
        assertEquals(sprint, Math.hypot(north.getX(), north.getZ()), 0.00001D);
        assertEquals(0.60D, north.getY(), 0.00001D);
        assertTrue(north.getZ() < -0.27D);
        Vector diagonal = DragonEscapeMovementKits.flutterBounceVelocity(
                new Vector(), new Vector(0, 0, -1), true, false, false, true);
        assertEquals(sprint, Math.hypot(diagonal.getX(), diagonal.getZ()), 0.00001D);
        Vector momentum = DragonEscapeMovementKits.flutterBounceVelocity(
                new Vector(0.3, 0, 0), new Vector(0, 0, -1),
                false, false, false, false);
        assertTrue(momentum.getX() > 0.27D);
    }

    @Test
    void acrobatGetsTwoNormalLeapChargesInAdditionToWallKick() {
        assertEquals(2, ArcadePortRules.DRAGON_ESCAPE_ACROBAT_LEAP_USES);
        assertEquals(2, ArcadePortRules.dragonEscapeLeapUses("acrobat"));
        assertEquals(0, ArcadePortRules.dragonEscapeLeapUses("flutterfly"));
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
        assertEquals(2, ArcadePortRules.dragonEscapeLeapUses("acrobat"));
        assertEquals(0, ArcadePortRules.dragonEscapeLeapUses("flutterfly"));
    }

    @Test
    void restoredWallKickUsesOriginalBoostedVelocityNotScaledVanillaLeap() {
        Vector facing = new Vector(0, 0, 1);
        Vector normal = new Vector(1, 0, 0);
        Vector kick = DragonEscapeMovementKits.wallKickVelocity(facing, normal);
        assertEquals(1.20D, DragonEscapeMovementKits.WALL_KICK_FORWARD_SPEED, 0.00001D);
        assertEquals(0.72D, DragonEscapeMovementKits.WALL_KICK_UPWARD_SPEED, 0.00001D);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_FORWARD_SPEED,
                kick.getZ(), 0.00001D);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_UPWARD_SPEED,
                kick.getY(), 0.00001D);
        assertTrue(kick.getX() >= DragonEscapeMovementKits.WALL_KICK_MIN_OUTWARD_SPEED);
        Vector vanilla = ArcadePortRules.dragonEscapeLeapVelocity(facing, false);
        assertTrue(kick.getY() > vanilla.getY(),
                "Restore the boosted .72 Y launch, not a weak fraction of ordinary Leap");
        Vector up = DragonEscapeMovementKits.wallKickVelocity(
                new Vector(0, 1, 1), normal);
        Vector down = DragonEscapeMovementKits.wallKickVelocity(
                new Vector(0, -1, 1), normal);
        assertTrue(up.getY() > kick.getY());
        assertTrue(down.getY() < kick.getY());
    }

    @Test
    void wallKickLookingIntoWallKeepsOriginalOutwardCollisionRecovery() {
        Vector normal = new Vector(1, 0, 0);
        Vector intoWall = DragonEscapeMovementKits.wallKickVelocity(
                new Vector(-1, 0, 0), normal);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_MIN_OUTWARD_SPEED,
                intoWall.getX(), 0.00001D);
        assertEquals(DragonEscapeMovementKits.WALL_KICK_UPWARD_SPEED,
                intoWall.getY(), 0.00001D);
        assertEquals(new Vector(), DragonEscapeMovementKits.wallKickVelocity(
                new Vector(), normal));
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
        assertEquals(0, state.wallKicksThisAirborneChain);
        assertTrue(state.tickWallCling(true, false, true, now));
        state.completeWallKick();
        assertEquals(1, state.wallKicksThisAirborneChain);
        assertFalse(state.wallKickCoolingDown(now));
        state.tickWallCling(false, false, false, now + 100L);
        assertTrue(state.tickWallCling(true, false, true, now + 200L));
        state.completeWallKick();
        assertEquals(2, state.wallKicksThisAirborneChain);
        assertFalse(state.wallKickCoolingDown(now + 200L));
        state.tickWallCling(false, false, false, now + 300L);
        assertTrue(state.tickWallCling(true, false, true, now + 400L));
        state.completeWallKick();
        assertEquals(3, state.wallKicksThisAirborneChain,
                "The airborne chain can continue with no cooldown until landing");
        assertFalse(state.wallKickCoolingDown(now + 400L));

        long landing = now + 600L;
        assertFalse(state.tickWallCling(true, true, true, landing));
        assertTrue(state.wallKickCoolingDown(landing));
        assertEquals(5_000L, DragonEscapeMovementKits.WALL_KICK_GROUNDED_COOLDOWN_MILLIS);
        assertEquals(landing + 5_000L, state.cooldownEndsAtMillis);
        assertEquals(0, state.wallKicksThisAirborneChain);
        assertEquals(0, state.wallKicksThisAirborneChain);
        state.tickWallCling(false, false, false, landing + 100L);
        assertFalse(state.tickWallCling(true, false, true, landing + 4_999L));
        assertFalse(state.tickWallCling(true, false, true, landing + 5_000L),
                "Holding Shift through cooldown must not automatically start a cling");
        state.tickWallCling(false, false, true, landing + 5_001L);
        assertTrue(state.tickWallCling(true, false, true, landing + 5_002L),
                "A fresh Shift after five seconds enables the next chain");
    }

    @Test
    void landingWithoutKickingDoesNotStartWallKickCooldown() {
        DragonEscapeMovementKits.AcrobatState state = new DragonEscapeMovementKits.AcrobatState();
        state.tickWallCling(false, true, false, 2_000L);
        state.tickWallCling(true, false, true, 2_200L);
        state.tickWallCling(false, false, false, 2_300L);
        state.tickWallCling(false, true, false, 2_400L);
        assertFalse(state.wallKickCoolingDown(2_400L));
        assertEquals(0, state.wallKicksThisAirborneChain);
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
