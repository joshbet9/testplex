package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class DragonEscapeMovementKitsTest {
    @Test
    void onlyDisruptorTntReceivesAdventurePlacementPermission() {
        assertTrue(DragonEscapeMovementKits.shouldEnableDisruptorAdventurePlacement(
                org.bukkit.Material.TNT, "Disruptor"));
        assertFalse(DragonEscapeMovementKits.shouldEnableDisruptorAdventurePlacement(
                org.bukkit.Material.TNT, "Other TNT"));
        assertFalse(DragonEscapeMovementKits.shouldEnableDisruptorAdventurePlacement(
                org.bukkit.Material.BRICKS, "Disruptor"));
    }

    @Test
    void featherBounceBeginsWithFiveChargesAndRejectsGroundClicks() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        assertEquals(5, DragonEscapeMovementKits.FLUTTER_MAX_CHARGES);
        assertEquals(6_000L, DragonEscapeMovementKits.FLUTTER_CHARGE_RECHARGE_MILLIS);
        assertEquals(5, flutter.charges);
        assertFalse(flutter.useBounce(true, 1_000L));
        assertEquals(5, flutter.charges);
        assertTrue(flutter.useBounce(false, 1_000L));
        assertEquals(4, flutter.charges);
        assertEquals(7_000L, flutter.nextChargeAtMillis);
    }

    @Test
    void featherHeldRightClickOnlySpendsOnceUntilAnInputPause() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        assertEquals(100L, DragonEscapeMovementKits.FLUTTER_RIGHT_CLICK_REARM_GAP_MILLIS);
        assertTrue(flutter.acceptRightClick(1_000L));
        assertTrue(flutter.useBounce(false, 1_000L));
        // A client holding the feather keeps emitting right-click packets.
        for (long time = 1_100L; time <= 2_100L; time += 100L) {
            assertFalse(flutter.acceptRightClick(time));
        }
        assertEquals(4, flutter.charges, "Holding must not use multiple stored charges");
        assertTrue(flutter.acceptRightClick(2_201L), "A fresh press after 100 ms quiet can bounce");
        assertTrue(flutter.useBounce(false, 2_201L));
        assertEquals(3, flutter.charges);
        assertFalse(flutter.acceptRightClick(2_202L), "Same-tick duplicate is ignored");

        // Ground clicks must latch too: keeping the button held during takeoff
        // should never automatically consume a mid-air charge when repeats are frequent.
        var grounded = new DragonEscapeMovementKits.FlutterState();
        assertTrue(grounded.acceptRightClick(3_000L));
        assertFalse(grounded.useBounce(true, 3_000L));
        assertFalse(grounded.acceptRightClick(3_100L));
        assertEquals(5, grounded.charges);
    }

    @Test
    void featherCanChainAfterOneHundredMillisecondsOfQuiet() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        assertTrue(flutter.acceptRightClick(1_000L));
        assertTrue(flutter.useBounce(false, 1_000L));
        assertFalse(flutter.acceptRightClick(1_100L), "100 ms is still inside the re-arm gap");
        assertTrue(flutter.acceptRightClick(1_201L), "101 ms quiet should allow another press");
        assertTrue(flutter.useBounce(false, 1_201L));
        assertFalse(flutter.acceptRightClick(1_301L), "Exactly 100 ms is still inside the gap");
        assertTrue(flutter.acceptRightClick(1_402L));
        assertTrue(flutter.useBounce(false, 1_402L));
        assertEquals(2, flutter.charges);
    }

    @Test
    void featherCanSpendFiveChargesInSameAirbornePeriodWithSeparatePresses() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        for (int i = 0; i < 5; i++) {
            long now = 1_000L + i * 1_000L;
            assertTrue(flutter.acceptRightClick(now));
            assertTrue(flutter.useBounce(false, now));
        }
        assertEquals(0, flutter.charges);
        assertFalse(flutter.useBounce(false, 5_001L));
        assertEquals(7_000L, flutter.nextChargeAtMillis,
                "Additional bounces must never postpone the earliest recharge");
    }

    @Test
    void featherChargesRestoreOneEverySixSecondsIncludingInAir() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        for (int i = 0; i < 5; i++) assertTrue(flutter.useBounce(false, 1_000L + i));
        assertFalse(flutter.recharge(6_999L));
        assertTrue(flutter.recharge(7_000L));
        assertEquals(1, flutter.charges);
        assertEquals(13_000L, flutter.nextChargeAtMillis);
        assertTrue(flutter.recharge(13_000L));
        assertEquals(2, flutter.charges);
        assertTrue(flutter.useBounce(false, 13_001L));
        assertEquals(1, flutter.charges);
        assertEquals(19_000L, flutter.nextChargeAtMillis);
        assertTrue(flutter.recharge(19_000L));
        assertTrue(flutter.recharge(25_000L));
        assertTrue(flutter.recharge(31_000L));
        assertTrue(flutter.recharge(37_000L));
        assertEquals(5, flutter.charges);
        assertEquals(0L, flutter.nextChargeAtMillis);
        assertFalse(flutter.recharge(100_000L));
    }

    @Test
    void featherChargeProgressFillsDuringSixSecondRecharge() {
        var flutter = new DragonEscapeMovementKits.FlutterState();
        assertEquals(1.0F, flutter.rechargeProgress(1_000L), 0.00001F);
        flutter.useBounce(false, 1_000L);
        assertEquals(0.0F, flutter.rechargeProgress(1_000L), 0.00001F);
        assertEquals(0.5F, flutter.rechargeProgress(4_000L), 0.00001F);
        assertEquals(1.0F, flutter.rechargeProgress(7_000L), 0.00001F);
        flutter.recharge(7_000L);
        assertEquals(1.0F, flutter.rechargeProgress(7_000L), 0.00001F);
    }

    @Test
    void featherBounceUsesFacingDirectionAndPreservesHorizontalSpeed() {
        assertEquals(0.384D, DragonEscapeMovementKits.FLUTTER_HORIZONTAL_SPEED, 0.00001D);
        Vector facingNorth = new Vector(0.0D, 0.2D, -1.0D);
        Vector north = DragonEscapeMovementKits.flutterBounceVelocity(
                new Vector(0.0D, -0.6D, 0.0D), facingNorth);
        assertEquals(0.384D, Math.hypot(north.getX(), north.getZ()), 0.00001D);
        assertEquals(0.52D, north.getY(), 0.00001D);
        assertTrue(north.getZ() < -0.31D);
        assertEquals(0.0D, north.getX(), 0.00001D);

        Vector facingEast = new Vector(1.0D, 0.0D, 0.0D);
        Vector sidewaysMomentum = DragonEscapeMovementKits.flutterBounceVelocity(
                new Vector(0.0D, 0.0D, -0.42D), facingEast);
        assertEquals(0.42D, sidewaysMomentum.getX(), 0.00001D,
                "A facing-east bounce must turn the launch east despite northward momentum");
        assertEquals(0.0D, sidewaysMomentum.getZ(), 0.00001D);

        Vector fallback = DragonEscapeMovementKits.flutterTravelDirection(new Vector(0.0D, 1.0D, 0.0D));
        assertEquals(0.0D, fallback.getX(), 0.00001D);
        assertEquals(1.0D, fallback.getZ(), 0.00001D,
                "A vertical look vector falls back to the stable north direction");
    }

    @Test
    void disruptorSelfLaunchRequiresTopFaceUseOnTheSupportBlock() {
        assertTrue(DragonEscapeMovementKits.shouldSelfLaunchDisruptorFromUnderfoot(
                true, true, true, 0.0D));
        assertTrue(DragonEscapeMovementKits.shouldSelfLaunchDisruptorFromUnderfoot(
                true, true, true, 0.12D), "Small server-side position variance does not change the launch");
        assertFalse(DragonEscapeMovementKits.shouldSelfLaunchDisruptorFromUnderfoot(
                false, true, true, 0.0D), "Air clicks do not self-launch");
        assertFalse(DragonEscapeMovementKits.shouldSelfLaunchDisruptorFromUnderfoot(
                true, false, true, 0.0D), "Side-face placement does not self-launch");
        assertFalse(DragonEscapeMovementKits.shouldSelfLaunchDisruptorFromUnderfoot(
                true, true, false, 0.0D), "TNT used on any other block does not self-launch");
        assertFalse(DragonEscapeMovementKits.shouldSelfLaunchDisruptorFromUnderfoot(
                true, true, true, 0.5D), "A block that is not under the racer's feet is rejected");
        assertFalse(DragonEscapeMovementKits.shouldSelfLaunchDisruptorFromUnderfoot(
                true, true, true, Double.NaN));
    }

    @Test
    void repeatedDisruptorUnderfootUsesAddOnePingIndependentVerticalImpulsePerTnt() {
        Vector first = DragonEscapeMovementKits.disruptorUnderfootLaunchVelocity(new Vector());
        Vector second = DragonEscapeMovementKits.disruptorUnderfootLaunchVelocity(first);
        assertEquals(DragonEscapeMovementKits.DISRUPTOR_MINE_BLAST_IMPULSE, first.getY(), 0.00001D);
        assertEquals(2.0D * first.getY(), second.getY(), 0.00001D,
                "Two TNT uses on the support block produce twice the vertical impulse");
        assertEquals(0.35D, DragonEscapeMovementKits.disruptorUnderfootLaunchVelocity(
                new Vector(0.35D, -2.0D, -0.1D)).getX(), 0.00001D,
                "The launch preserves horizontal momentum");
        assertEquals(first.getY(), DragonEscapeMovementKits.disruptorUnderfootLaunchVelocity(
                new Vector(0.0D, -2.0D, 0.0D)).getY(), 0.00001D,
                "A downward fall does not cancel the upward launch");
    }

    @Test
    void disruptorSupportHistoryAcceptsFullAndPartialBlockSurfaces() {
        assertTrue(DragonEscapeMovementKits.hasDisruptorSupportClearance(true, 0.0D),
                "A full block directly underfoot is accepted");
        assertTrue(DragonEscapeMovementKits.hasDisruptorSupportClearance(true, 0.0D),
                "A partial block top surface uses the same zero clearance");
        assertFalse(DragonEscapeMovementKits.hasDisruptorSupportClearance(false, 0.0D));
        assertFalse(DragonEscapeMovementKits.hasDisruptorSupportClearance(true, 0.5D));
    }

    @Test
    void disruptorRecentSupportWindowHandlesLatencyAndStaleEvents() {
        assertEquals(500L, DragonEscapeMovementKits.disruptorSupportHistoryMillis(200));
        assertTrue(DragonEscapeMovementKits.isRecentDisruptorSupport(10_000L, 10_450L, 200));
        assertFalse(DragonEscapeMovementKits.isRecentDisruptorSupport(10_000L, 10_501L, 200));
        assertFalse(DragonEscapeMovementKits.isRecentDisruptorSupport(10_500L, 10_400L, 200));
        assertEquals(1_000L, DragonEscapeMovementKits.disruptorSupportHistoryMillis(900));
    }

    @Test
    void disruptorSelfLaunchImpulseIsHalvedAndRetainsRunningMomentum() {
        Vector current = new Vector(0.4D, 0.0D, 0.2D);
        Vector launched = DragonEscapeMovementKits.disruptorUnderfootLaunchVelocity(current);
        assertEquals(0.825D, launched.getY(), 0.00001D);
        assertEquals(0.4D, launched.getX(), 0.00001D);
        assertEquals(0.2D, launched.getZ(), 0.00001D);
    }

    @Test
    void dragonEscapeVisibilityAndGhostControlsUseSeparateHotbarSlots() {
        assertEquals(4, DragonEscapeMovementKits.DRAGON_ESCAPE_PLAYER_VISIBILITY_SLOT);
        assertEquals(8, DragonEscapeMovementKits.DRAGON_ESCAPE_GHOST_SLOT);
        assertNotEquals(DragonEscapeMovementKits.DRAGON_ESCAPE_PLAYER_VISIBILITY_SLOT,
                DragonEscapeMovementKits.DRAGON_ESCAPE_GHOST_SLOT);
    }

    @Test
    void leapToolIsRemovedWhenTheLastChargeIsSpent() {
        assertTrue(DragonEscapeMovementKits.shouldShowDragonEscapeLeapItem(1));
        assertFalse(DragonEscapeMovementKits.shouldShowDragonEscapeLeapItem(0));
        assertFalse(DragonEscapeMovementKits.shouldShowDragonEscapeLeapItem(-1));
    }

    @Test
    void dragonEscapeVisibilityDefaultsToTheExistingInvisibleStateAndToggles() {
        boolean visible = DragonEscapeMovementKits.dragonEscapePlayerVisibleByDefault();
        assertFalse(visible, "The existing Dragon Escape default is invisible");
        visible = DragonEscapeMovementKits.toggleDragonEscapePlayerVisibility(visible);
        assertTrue(visible);
        visible = DragonEscapeMovementKits.toggleDragonEscapePlayerVisibility(visible);
        assertFalse(visible);
    }

    @Test
    void acrobatExperienceShowsRemainingLeapsAndPreservesWallStickEnergy() {
        assertEquals(2, ArcadePortRules.DRAGON_ESCAPE_ACROBAT_LEAP_USES);
        var full = DragonEscapeMovementKits.acrobatExperience(2, 1.0D);
        assertEquals(2, full.level());
        assertEquals(1.0F, full.progress(), 0.00001F);

        var afterFirstLeap = DragonEscapeMovementKits.acrobatExperience(1, 0.625D);
        assertEquals(1, afterFirstLeap.level());
        assertEquals(0.625F, afterFirstLeap.progress(), 0.00001F,
                "Remaining Leaps must not replace wall-stick energy in the XP bar");

        var afterSecondLeap = DragonEscapeMovementKits.acrobatExperience(0, 0.3D);
        assertEquals(0, afterSecondLeap.level());
        assertEquals(0.3F, afterSecondLeap.progress(), 0.00001F);

        assertEquals(0, DragonEscapeMovementKits.acrobatExperience(-2, 0.0D).level());
        assertEquals(2, DragonEscapeMovementKits.acrobatExperience(50, 1.0D).level());
        assertEquals(0.0F, DragonEscapeMovementKits.acrobatExperience(1, -1.0D).progress());
        assertEquals(1.0F, DragonEscapeMovementKits.acrobatExperience(1, 1.5D).progress());
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
    void acrobatWallClingIgnoresWasdJumpAndSneakMovementButAllowsLooking() {
        Location anchor = new Location(null, 22.50, 74.75, -11.50, 0.0F, 0.0F);
        for (double[] move : new double[][] {
                {23.50, 74.75, -11.50}, // Forward / away from wall
                {21.50, 74.75, -11.50}, // Backward
                {22.50, 74.75, -10.50}, // Strafe
                {22.50, 74.75, -12.50},
                {22.50, 75.75, -11.50}, // Sneak / jump drift
                {22.50, 73.75, -11.50}
        }) {
            Location attempted = new Location(null, move[0], move[1], move[2], 107.0F, -25.0F);
            Location held = DragonEscapeMovementKits.wallClingDestination(attempted, anchor);
            assertEquals(anchor.getX(), held.getX(), 0.00001D);
            assertEquals(anchor.getY(), held.getY(), 0.00001D);
            assertEquals(anchor.getZ(), held.getZ(), 0.00001D);
            assertEquals(107.0F, held.getYaw(), 0.00001F, "Must still turn to aim wall kick");
            assertEquals(-25.0F, held.getPitch(), 0.00001F);
        }
        assertNull(DragonEscapeMovementKits.wallClingDestination(null, anchor));
    }

    @Test
    void acrobatWallClingAnchorMovesOutOfWallWithoutChangingHeightOrAim() {
        Location contact = new Location(null, 10.5D, 72.25D, -4.5D, 38.0F, -12.0F);
        assertEquals(0.04D, DragonEscapeMovementKits.WALL_CLING_WALL_CLEARANCE, 0.00001D);

        for (Vector normal : new Vector[] {
                new Vector(1, 0, 0), new Vector(-1, 0, 0),
                new Vector(0, 0, 1), new Vector(0, 0, -1)
        }) {
            Location anchor = DragonEscapeMovementKits.wallClingAnchor(contact, normal);
            assertEquals(contact.getX() + normal.getX() * 0.04D, anchor.getX(), 0.00001D);
            assertEquals(contact.getY(), anchor.getY(), 0.00001D);
            assertEquals(contact.getZ() + normal.getZ() * 0.04D, anchor.getZ(), 0.00001D);
            assertEquals(contact.getYaw(), anchor.getYaw(), 0.00001F);
            assertEquals(contact.getPitch(), anchor.getPitch(), 0.00001F);
        }
        assertEquals(10.5D, contact.getX(), 0.00001D, "The contact location is not mutated");
        assertEquals(-4.5D, contact.getZ(), 0.00001D);
        assertEquals(contact, DragonEscapeMovementKits.wallClingAnchor(contact, null));
        assertNull(DragonEscapeMovementKits.wallClingAnchor(null, new Vector(1, 0, 0)));
    }

    @Test
    void acrobatWallContactUsesTheLatchedHitboxAfterAttemptedMovement() {
        Location attempted = new Location(null, 23.0D, 75.0D, -10.0D);
        Location anchor = new Location(null, 22.5D, 74.75D, -11.5D);
        BoundingBox movedHitbox = new BoundingBox(22.7D, 75.0D, -10.3D,
                23.3D, 76.8D, -9.7D);

        BoundingBox anchored = DragonEscapeMovementKits.wallClingBoundingBox(
                movedHitbox, attempted, anchor);

        assertEquals(22.2D, anchored.getMinX(), 0.00001D);
        assertEquals(22.8D, anchored.getMaxX(), 0.00001D);
        assertEquals(74.75D, anchored.getMinY(), 0.00001D);
        assertEquals(76.55D, anchored.getMaxY(), 0.00001D);
        assertEquals(-11.8D, anchored.getMinZ(), 0.00001D);
        assertEquals(-11.2D, anchored.getMaxZ(), 0.00001D);
        assertEquals(22.7D, movedHitbox.getMinX(), 0.00001D,
                "The live player hitbox must not be modified while checking anchor contact");
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
    void wallStickRemainsLatchedAfterUncrouchingAndSpaceCanKickNormally() {
        var state = new DragonEscapeMovementKits.AcrobatState();
        assertFalse(state.tickWallCling(false, false, true, 1_000L),
                "Wall contact without crouching does not initiate a stick");
        assertFalse(state.pressedForWallKick(true, false, false),
                "Space without a Shift-activated cling does not kick");
        state.pressedForWallKick(false, false, false);

        assertTrue(state.tickWallCling(true, false, true, 1_050L));
        assertTrue(state.clingActivated);
        assertFalse(state.pressedForWallKick(false, false, true));
        double chargeAfterActivation = state.charge;

        assertTrue(state.tickWallCling(false, false, true, 1_100L),
                "Releasing Shift preserves the active wall-stick");
        assertFalse(state.clingActivated, "Releasing Shift is not a new stick");
        assertTrue(state.charge < chargeAfterActivation,
                "The XP cost must continue while Shift is released");
        assertTrue(state.tickWallCling(false, false, true, 1_150L));
        assertTrue(state.pressedForWallKick(true, false, true),
                "A fresh Space press after releasing Shift performs the kick");

        state.completeWallKick();
        assertFalse(state.clingActive, "The kick consumes the current stick");
        assertFalse(state.tickWallCling(false, false, true, 1_200L),
                "Releasing Shift cannot automatically reattach after a kick");
    }

    @Test
    void wallStickMustHaveWallContactAndDoesNotReactivateWithoutNewCrouch() {
        var state = new DragonEscapeMovementKits.AcrobatState();
        assertFalse(state.tickWallCling(true, false, false, 1_000L));
        assertTrue(state.tickWallCling(true, false, true, 1_050L));
        assertTrue(state.tickWallCling(false, false, true, 1_100L));
        assertFalse(state.tickWallCling(false, false, false, 1_150L));
        assertFalse(state.tickWallCling(false, false, true, 1_200L),
                "Returning to wall with Shift released cannot resume a lost stick");
        assertTrue(state.tickWallCling(true, false, true, 1_250L),
                "A fresh crouch while touching a wall can start a new stick");
        assertFalse(state.tickWallCling(false, true, true, 1_300L),
                "Landing cancels the stick even without holding Shift");
        assertFalse(state.tickWallCling(false, false, true, 1_350L),
                "Jumping off the ground without another crouch cannot stick");
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
        assertTrue(state.tickWallCling(false, false, true, 1_000L),
                "Uncrouching must NOT detach a latched wall-stick");
        assertTrue(state.clingActive);
        assertFalse(state.tickWallCling(false, false, false, 1_000L),
                "A released Shift stick still ends immediately on wall-contact loss");
        assertFalse(state.clingActive);
        assertFalse(state.tickWallCling(false, false, true, 1_000L),
                "Returning to the wall without crouching cannot auto-rearm");
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
