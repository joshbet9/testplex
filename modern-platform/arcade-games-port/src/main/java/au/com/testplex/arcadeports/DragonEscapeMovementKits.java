package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Dragon Escape movement kits. Keep input rules, cooldowns and vectors
 * testable independently of a live Paper server.
 */
final class DragonEscapeMovementKits {
    // Keep rule helpers independent of Paper's runtime-only sound registry.
    static final String FLUTTER_SOUND_NAME = "ENTITY_SLIME_JUMP";
    static final String WALL_KICK_SOUND_NAME = "ENTITY_FIREWORK_ROCKET_LAUNCH";
    static final String WALL_CLING_SOUND_NAME = "BLOCK_SLIME_BLOCK_PLACE";
    // A single right-click launches a bounce. XP no longer powers Flutterfly.
    static final int FLUTTER_MAX_CHARGES = 5;
    // Bukkit has no use-button release event for ordinary feathers. A 100 ms
    // quiet interval distinguishes a new Feather click from held-button repeats.
    static final long FLUTTER_RIGHT_CLICK_REARM_GAP_MILLIS = 100L;
    static final int DRAGON_ESCAPE_PLAYER_VISIBILITY_SLOT = 4;
    static final int DRAGON_ESCAPE_GHOST_SLOT = 8;
    // A six-second interval regenerates Flutterfly charges 33.3% faster.
    static final long FLUTTER_CHARGE_RECHARGE_MILLIS = 6_000L;
    // Raise the tested Flutterfly bounce's minimum horizontal impulse by 20%.
    static final double FLUTTER_HORIZONTAL_SPEED = 0.384D;
    // Lower arc than the previous bounce, without changing regular jumps.
    static final double FLUTTER_BOUNCE_UPWARD_SPEED = 0.52D;

    // Restore the pre-#738 boosted wall kick (no weak scaled vanilla Leap).
    static final double WALL_KICK_FORWARD_SPEED = 1.20D;
    static final double WALL_KICK_UPWARD_SPEED = 0.72D;
    static final double WALL_KICK_PITCH_LIFT = 0.60D;
    static final double WALL_KICK_MIN_UPWARD_SPEED = 0.20D;
    static final double WALL_KICK_MAX_UPWARD_SPEED = 1.20D;
    static final long WALL_KICK_GROUNDED_COOLDOWN_MILLIS = 5_000L;
    static final double WALL_KICK_MIN_OUTWARD_SPEED = 0.28D;
    // No fixed wall-stick duration: active Shift steadily drains its own XP bar.
    static final double WALL_CLING_DRAIN_PER_TICK = 0.0125D;
    static final double WALL_CLING_REGEN_PER_TICK = 0.009D;
    static final double ACROBAT_FALL_DAMAGE_MULTIPLIER = 0.50D;
    private static final double TOUCH_TOLERANCE = 0.055D;
    private static final double MAX_PENETRATION = 0.025D;

    private DragonEscapeMovementKits() { }

    static final class FlutterState {
        int charges = FLUTTER_MAX_CHARGES;
        long nextChargeAtMillis;
        long lastRightClickAtMillis = Long.MIN_VALUE;

        /**
         * Ordinary feathers can produce repeated PlayerInteractEvents during one
         * sustained right-click. Refresh the latch even for ignored repeats, so
         * one hold spends only one charge, without imposing a recharge cooldown.
         */
        boolean acceptRightClick(long nowMillis) {
            boolean newPress = lastRightClickAtMillis == Long.MIN_VALUE
                    || nowMillis - lastRightClickAtMillis > FLUTTER_RIGHT_CLICK_REARM_GAP_MILLIS;
            lastRightClickAtMillis = nowMillis;
            return newPress;
        }

        /**
         * Charges replenish individually every six seconds, including in air.
         * Spending another charge cannot delay an already-scheduled recharge.
         */
        boolean recharge(long nowMillis) {
            if (charges >= FLUTTER_MAX_CHARGES || nextChargeAtMillis <= 0L) return false;
            boolean changed = false;
            while (charges < FLUTTER_MAX_CHARGES && nowMillis >= nextChargeAtMillis) {
                charges++;
                changed = true;
                nextChargeAtMillis = charges == FLUTTER_MAX_CHARGES
                        ? 0L : nextChargeAtMillis + FLUTTER_CHARGE_RECHARGE_MILLIS;
            }
            return changed;
        }

        /** Consume one stored feather bounce only while actually airborne. */
        boolean useBounce(boolean grounded, long nowMillis) {
            recharge(nowMillis);
            if (grounded || charges <= 0) return false;
            charges--;
            if (nextChargeAtMillis == 0L) {
                nextChargeAtMillis = nowMillis + FLUTTER_CHARGE_RECHARGE_MILLIS;
            }
            return true;
        }

        /** Fraction of the current six-second recharge interval completed. */
        float rechargeProgress(long nowMillis) {
            if (charges >= FLUTTER_MAX_CHARGES || nextChargeAtMillis <= 0L) return 1.0F;
            long remaining = Math.max(0L, nextChargeAtMillis - nowMillis);
            return (float) (1.0D - Math.min(1.0D,
                    (double) remaining / FLUTTER_CHARGE_RECHARGE_MILLIS));
        }
    }

    /** The XP level is the remaining number of Leaps; the bar is cling energy. */
    record AcrobatExperience(int level, float progress) { }

    static AcrobatExperience acrobatExperience(int remainingLeaps, double wallStickCharge) {
        int level = Math.max(0, Math.min(ArcadePortRules.DRAGON_ESCAPE_ACROBAT_LEAP_USES, remainingLeaps));
        float progress = (float) Math.max(0.0D, Math.min(1.0D, wallStickCharge));
        return new AcrobatExperience(level, progress);
    }

    static final class AcrobatState {
        boolean previouslyHeld;
        boolean crouchConsumed;
        boolean clingActivated;
        boolean clingActive;
        boolean gravityManaged;
        boolean gravityBeforeCling;
        double clingHeight;
        Location clingAnchor;
        double charge = 1.0D;

        // An airborne chain is unrestricted; cooldown starts ONLY after landing.
        int wallKicksThisAirborneChain;
        boolean awaitingLandingAfterKick;
        long cooldownEndsAtMillis;

        /** Landing after at least one kick starts the five-second cooldown. */
        void tickLanding(boolean grounded, long nowMillis) {
            if (grounded && awaitingLandingAfterKick) {
                cooldownEndsAtMillis = Math.max(cooldownEndsAtMillis,
                        nowMillis + WALL_KICK_GROUNDED_COOLDOWN_MILLIS);
                awaitingLandingAfterKick = false;
                wallKicksThisAirborneChain = 0;
                clingActive = false;
            }
        }

        boolean wallKickCoolingDown(long nowMillis) {
            return nowMillis < cooldownEndsAtMillis;
        }

        /** Called only after a real, successful wall kick. */
        void completeWallKick() {
            wallKicksThisAirborneChain++;
            awaitingLandingAfterKick = true;
            cancelWallClingForKick();
        }

        /**
         * Every wall kick needs a fresh airborne Space press after a crouch
         * activated wall-stick. Shift can be released before jumping. One stick
         * buys one kick as long as physical wall contact is maintained.
         */
        boolean pressedForWallKick(boolean jumpHeld, boolean grounded, boolean sticking) {
            boolean freshPress = jumpHeld && !previouslyHeld;
            previouslyHeld = jumpHeld;
            return freshPress && !grounded && sticking && !clingActivated;
        }

        /**
         * Crouch activates an airborne wall-stick. The stick remains latched
         * after releasing Shift so the player can press Space normally.
         * Charge drains throughout the cling, which ends on a kick, contact
         * loss, landing, cooldown, or XP exhaustion. A new stick needs a
         * fresh crouch after any of those endings.
         */
        boolean tickWallCling(boolean crouching, boolean grounded, boolean touchingWall, long nowMillis) {
            tickLanding(grounded, nowMillis);
            clingActivated = false;
            // Releasing Shift re-arms the next activation without detaching now.
            if (!crouching) crouchConsumed = false;
            if (wallKickCoolingDown(nowMillis)) {
                clingActive = false;
                if (crouching) crouchConsumed = true; // Holding Shift cannot auto-arm after cooldown.
            } else if (grounded || !touchingWall) {
                clingActive = false;
            } else if (crouching && !crouchConsumed) {
                crouchConsumed = true;
                if (!clingActive && charge >= WALL_CLING_DRAIN_PER_TICK) {
                    clingActive = true;
                    clingActivated = true;
                }
            }

            boolean sticking = !grounded && touchingWall && clingActive
                    && !wallKickCoolingDown(nowMillis);
            if (sticking) {
                if (charge >= WALL_CLING_DRAIN_PER_TICK) {
                    charge = Math.max(0.0D, charge - WALL_CLING_DRAIN_PER_TICK);
                } else {
                    clingActive = false;
                    sticking = false;
                }
            }
            if (!sticking) charge = Math.min(1.0D, charge + WALL_CLING_REGEN_PER_TICK);
            return sticking;
        }

        void cancelWallClingForKick() {
            clingActive = false;
            clingAnchor = null;
            crouchConsumed = true;
        }
    }

    /** Flutterfly launches in the horizontal direction the player is facing. */
    static Vector flutterTravelDirection(Vector facing) {
        Vector look = facing == null ? new Vector(0.0D, 0.0D, 1.0D) : facing.clone().setY(0.0D);
        if (look.lengthSquared() < 1.0E-8D) look = new Vector(0.0D, 0.0D, 1.0D);
        return look.normalize();
    }

    /**
     * A Feather bounce preserves the player's horizontal speed while steering
     * the launch along the direction they are facing.
     */
    static Vector flutterBounceVelocity(Vector current, Vector facing) {
        Vector direction = flutterTravelDirection(facing);
        double horizontalSpeed = current == null ? 0.0D
                : Math.hypot(current.getX(), current.getZ());
        return direction.multiply(Math.max(FLUTTER_HORIZONTAL_SPEED, horizontalSpeed))
                .setY(FLUTTER_BOUNCE_UPWARD_SPEED);
    }

    static boolean shouldShowDragonEscapeLeapItem(int remainingUses) {
        return remainingUses > 0;
    }

    static boolean dragonEscapePlayerVisibleByDefault() {
        return false;
    }

    static boolean toggleDragonEscapePlayerVisibility(boolean currentlyVisible) {
        return !currentlyVisible;
    }

    static double acrobatFallDamage(String kit, double vanillaFallDamage) {
        return "acrobat".equals(kit) ? vanillaFallDamage * ACROBAT_FALL_DAMAGE_MULTIPLIER
                : vanillaFallDamage;
    }

    /**
     * Shift the latched position slightly out of the contacted block. Paper can
     * report the player a fraction inside a wall after a collision; pinning that
     * exact location leaves the client fighting collision correction every move.
     * The clearance stays inside TOUCH_TOLERANCE so the wall remains eligible
     * for a kick while the player box no longer overlaps it.
     */
    static final double WALL_CLING_WALL_CLEARANCE = 0.04D;

    static Location wallClingAnchor(Location contact, Vector wallNormal) {
        if (contact == null) return null;
        Location anchor = contact.clone();
        if (wallNormal == null || !Double.isFinite(wallNormal.getX())
                || !Double.isFinite(wallNormal.getY()) || !Double.isFinite(wallNormal.getZ())) return anchor;
        Vector outward = wallNormal.clone().setY(0.0D);
        if (outward.lengthSquared() < 1.0E-8D) return anchor;
        anchor.add(outward.normalize().multiply(WALL_CLING_WALL_CLEARANCE));
        return anchor;
    }

    /**
     * Cling is a full position latch, not just a fixed Y coordinate. Movement
     * keys must never walk the player off a wall; only mouse look changes while
     * latched. A fresh Space press is handled separately by wallKickVelocity.
     */
    static Location wallClingDestination(Location attempted, Location anchor) {
        if (attempted == null || anchor == null || attempted.getWorld() != anchor.getWorld())
            return attempted;
        Location pinned = anchor.clone();
        pinned.setYaw(attempted.getYaw());
        pinned.setPitch(attempted.getPitch());
        return pinned;
    }

    static Vector wallClingVelocity() {
        // Used with temporary gravity suppression and a fixed vertical anchor.
        // No upward velocity is applied: sticking must never act as wall climbing.
        return new Vector(0.0D, 0.0D, 0.0D);
    }

    /**
     * Restore the unscaled pre-#738 Champions-inspired boosted wall kick.
     * Wall contact is still mandatory, and look yaw/pitch steer the launch.
     * The minimum outward component keeps a launch from being swallowed by
     * the collision surface when aiming at or along the wall.
     */
    static Vector wallKickVelocity(Vector facing, Vector wallNormal) {
        if (facing == null || wallNormal == null
                || !Double.isFinite(facing.getX()) || !Double.isFinite(facing.getY())
                || !Double.isFinite(facing.getZ()) || facing.lengthSquared() < 1.0E-6D) {
            return new Vector();
        }
        Vector aim = facing.clone().normalize();
        Vector horizontal = aim.clone().setY(0.0D).multiply(WALL_KICK_FORWARD_SPEED);
        Vector outward = wallNormal.clone().setY(0.0D);
        if (outward.lengthSquared() > 1.0E-6D) {
            outward.normalize();
            double outwardSpeed = horizontal.dot(outward);
            if (outwardSpeed < WALL_KICK_MIN_OUTWARD_SPEED) {
                horizontal.add(outward.multiply(WALL_KICK_MIN_OUTWARD_SPEED - outwardSpeed));
            }
        }
        double upward = Math.max(WALL_KICK_MIN_UPWARD_SPEED,
                Math.min(WALL_KICK_MAX_UPWARD_SPEED,
                        WALL_KICK_UPWARD_SPEED + aim.getY() * WALL_KICK_PITCH_LIFT));
        horizontal.setY(upward);
        return horizontal;
    }

    /**
     * Only physical collision-box contact with a collidable wall side counts.
     * Facing toward, away from or along the wall all qualify. Floor/ceiling
     * contact and merely being near a wall do not.
     */
    static Vector touchingWallNormal(Player player) {
        return touchingWallNormal(player.getWorld(), player.getBoundingBox());
    }

    /**
     * Keep wall contact tied to the latched position, not a transient attempted
     * movement position. Otherwise a held movement key can move the live hitbox
     * off the wall before the movement event pins it back to the cling anchor.
     */
    static Vector touchingWallNormal(Player player, Location anchor) {
        if (player == null || anchor == null || anchor.getWorld() != player.getWorld()) return null;
        BoundingBox hitbox = wallClingBoundingBox(
                player.getBoundingBox(), player.getLocation(), anchor);
        return touchingWallNormal(player.getWorld(), hitbox);
    }

    static BoundingBox wallClingBoundingBox(BoundingBox current, Location currentLocation, Location anchor) {
        if (current == null || currentLocation == null || anchor == null
                || currentLocation.getWorld() != anchor.getWorld()) return current;
        return current.clone().shift(anchor.getX() - currentLocation.getX(),
                anchor.getY() - currentLocation.getY(), anchor.getZ() - currentLocation.getZ());
    }

    private static Vector touchingWallNormal(org.bukkit.World world, BoundingBox hitbox) {
        for (int x = (int) Math.floor(hitbox.getMinX() - TOUCH_TOLERANCE);
                x <= Math.floor(hitbox.getMaxX() + TOUCH_TOLERANCE); x++) {
            for (int y = (int) Math.floor(hitbox.getMinY() + 0.15D);
                    y <= Math.floor(hitbox.getMaxY() - 0.15D); y++) {
                for (int z = (int) Math.floor(hitbox.getMinZ() - TOUCH_TOLERANCE);
                        z <= Math.floor(hitbox.getMaxZ() + TOUCH_TOLERANCE); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.isEmpty()) continue;
                    for (BoundingBox shape : block.getCollisionShape().getBoundingBoxes()) {
                        BoundingBox worldShape = shape.clone().shift(x, y, z);
                        Vector normal = contactNormal(hitbox, worldShape);
                        if (normal != null) return normal;
                    }
                }
            }
        }
        return null;
    }

    /** Testable side-contact check, using full collision shapes (including slabs/fences). */
    static Vector contactNormal(BoundingBox player, BoundingBox wall) {
        double yOverlap = Math.min(player.getMaxY() - 0.12D, wall.getMaxY())
                - Math.max(player.getMinY() + 0.15D, wall.getMinY());
        if (yOverlap < 0.25D) return null;
        double xOverlap = Math.min(player.getMaxX(), wall.getMaxX())
                - Math.max(player.getMinX(), wall.getMinX());
        double zOverlap = Math.min(player.getMaxZ(), wall.getMaxZ())
                - Math.max(player.getMinZ(), wall.getMinZ());
        Vector best = null;
        double nearestGap = Double.POSITIVE_INFINITY;
        if (zOverlap > 0.08D) {
            double westGap = player.getMinX() - wall.getMaxX();
            if (westGap >= -MAX_PENETRATION && westGap <= TOUCH_TOLERANCE) {
                best = new Vector(1, 0, 0);
                nearestGap = Math.abs(westGap);
            }
            double eastGap = wall.getMinX() - player.getMaxX();
            if (eastGap >= -MAX_PENETRATION && eastGap <= TOUCH_TOLERANCE
                    && Math.abs(eastGap) < nearestGap) {
                best = new Vector(-1, 0, 0);
                nearestGap = Math.abs(eastGap);
            }
        }
        if (xOverlap > 0.08D) {
            double northGap = player.getMinZ() - wall.getMaxZ();
            if (northGap >= -MAX_PENETRATION && northGap <= TOUCH_TOLERANCE
                    && Math.abs(northGap) < nearestGap) {
                best = new Vector(0, 0, 1);
                nearestGap = Math.abs(northGap);
            }
            double southGap = wall.getMinZ() - player.getMaxZ();
            if (southGap >= -MAX_PENETRATION && southGap <= TOUCH_TOLERANCE
                    && Math.abs(southGap) < nearestGap) {
                best = new Vector(0, 0, -1);
            }
        }
        return best;
    }
}
