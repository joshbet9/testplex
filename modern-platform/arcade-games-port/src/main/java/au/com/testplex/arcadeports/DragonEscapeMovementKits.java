package au.com.testplex.arcadeports;

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
    static final double FLUTTER_ACTIVATION_COST = 0.08D;
    // 40% faster XP consumption than the prior 0.0125/tick flutter.
    static final double FLUTTER_DRAIN_PER_TICK = 0.0175D;
    static final double FLUTTER_REGEN_PER_TICK = 0.009D;
    // A one-block rise precedes each 28-tick Yoshi flutter arc.
    // Five 0.2-block vertical steps lift the player by approximately one block.
    static final int FLUTTER_LIFT_TICKS = 5;
    static final double FLUTTER_LIFT_VERTICAL_SPEED = 0.20D;
    static final int FLUTTER_WAVE_TICKS = 28;
    static final int FLUTTER_TOTAL_TICKS = FLUTTER_LIFT_TICKS + FLUTTER_WAVE_TICKS;
    static final double FLUTTER_WAVE_BASE_Y = -0.025D;
    static final double FLUTTER_WAVE_AMPLITUDE_Y = 0.22D;
    // 25% faster than the prior 80%-sprint flutter: now about sprint speed.
    static final double FLUTTER_HORIZONTAL_SPEED = 1.25D * 0.8D * 5.612D / 20.0D;
    static final double FLUTTER_END_FORWARD_SPEED = 0.30D; // Brief post-flutter carry.

    // Every wall-jump chain uses fractions of the existing Dragon Escape Leap.
    static final long WALL_KICK_GROUNDED_COOLDOWN_MILLIS = 10_000L;
    static final double WALL_KICK_MIN_OUTWARD_SPEED = 0.28D;
    // No fixed wall-stick duration: active Shift steadily drains its own XP bar.
    static final double WALL_CLING_DRAIN_PER_TICK = 0.0125D;
    static final double WALL_CLING_REGEN_PER_TICK = 0.009D;
    static final double ACROBAT_FALL_DAMAGE_MULTIPLIER = 0.50D;
    private static final double TOUCH_TOLERANCE = 0.055D;
    private static final double MAX_PENETRATION = 0.025D;

    private DragonEscapeMovementKits() { }

    static final class FlutterState {
        double charge = 1.0D;
        boolean fluttering;
        boolean activated;
        boolean ended;
        boolean previouslyHeld;
        boolean releasedWhileAirborne;
        int flutterTicks;

        /**
         * Never activate from a held ground-jump. A release while airborne must
         * arm the next fresh Space press. A press grants exactly ONE 28-tick arc:
         * continuing to hold Space through its end cannot repeat it.
         * The first five ticks are a one-block lift before the 28-tick arc.
         */
        void tick(boolean jumpHeld, boolean grounded) {
            activated = false;
            ended = false;
            boolean freshPress = jumpHeld && !previouslyHeld;
            previouslyHeld = jumpHeld;
            if (grounded) {
                fluttering = false;
                flutterTicks = 0;
                releasedWhileAirborne = false;
                charge = Math.min(1.0D, charge + FLUTTER_REGEN_PER_TICK);
                return;
            }

            if (!jumpHeld) releasedWhileAirborne = true;
            if (fluttering) {
                if (!jumpHeld || flutterTicks >= FLUTTER_TOTAL_TICKS
                        || charge < FLUTTER_DRAIN_PER_TICK) {
                    fluttering = false;
                    ended = true;
                } else {
                    flutterTicks++;
                    charge = Math.max(0.0D, charge - FLUTTER_DRAIN_PER_TICK);
                    return;
                }
            } else if (freshPress && releasedWhileAirborne
                    && charge >= FLUTTER_ACTIVATION_COST + FLUTTER_DRAIN_PER_TICK) {
                charge -= FLUTTER_ACTIVATION_COST + FLUTTER_DRAIN_PER_TICK;
                fluttering = true;
                activated = true;
                releasedWhileAirborne = false;
                flutterTicks = 1;
                return;
            }
            charge = Math.min(1.0D, charge + FLUTTER_REGEN_PER_TICK);
        }
    }

    static final class AcrobatState {
        boolean previouslyHeld;
        boolean crouchConsumed;
        boolean clingActivated;
        boolean clingActive;
        boolean gravityManaged;
        boolean gravityBeforeCling;
        double clingHeight;
        double charge = 1.0D;

        // An airborne chain is unrestricted; cooldown starts ONLY after landing.
        int wallKicksThisAirborneChain;
        boolean awaitingLandingAfterKick;
        long cooldownEndsAtMillis;

        /** Landing after at least one kick starts the full 10-second cooldown. */
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

        /** First/second/third-or-later consecutive airborne wall kicks: 50/75/100%. */
        double nextWallKickStrength() {
            return wallKicksThisAirborneChain == 0 ? 0.50D
                    : wallKicksThisAirborneChain == 1 ? 0.75D : 1.0D;
        }

        /** Called only after a real, successful wall kick. */
        void completeWallKick() {
            wallKicksThisAirborneChain++;
            awaitingLandingAfterKick = true;
            cancelWallClingForKick();
        }

        /**
         * Every wall kick needs a fresh airborne Space press while an already
         * active Shift-stick holds physical wall contact. One stick buys one kick.
         */
        boolean pressedForWallKick(boolean jumpHeld, boolean grounded, boolean sticking) {
            boolean freshPress = jumpHeld && !previouslyHeld;
            previouslyHeld = jumpHeld;
            return freshPress && !grounded && sticking && !clingActivated;
        }

        /**
         * While airborne, Shift sticks to a physically touched wall and drains
         * XP continuously. A landing after a wall kick starts a 10-second
         * lockout; no cooldown ever starts midair, including between chain kicks.
         */
        boolean tickWallCling(boolean crouching, boolean grounded, boolean touchingWall, long nowMillis) {
            tickLanding(grounded, nowMillis);
            clingActivated = false;
            if (!crouching) {
                crouchConsumed = false;
                clingActive = false;
            } else if (wallKickCoolingDown(nowMillis)) {
                clingActive = false;
                crouchConsumed = true; // A held Shift during cooldown cannot auto-arm.
            } else if (grounded || !touchingWall) {
                clingActive = false;
            } else if (!crouchConsumed) {
                crouchConsumed = true;
                if (charge >= WALL_CLING_DRAIN_PER_TICK) {
                    clingActive = true;
                    clingActivated = true;
                }
            }

            boolean sticking = crouching && !grounded && touchingWall && clingActive
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
            crouchConsumed = true;
        }
    }

    /**
     * User input steers each flutter at around 100% of standard ground sprint.
     * If no directional key is held, continue existing travel direction;
     * looking direction is a fallback when starting from standstill.
     */
    static Vector flutterTravelDirection(Vector current, Vector facing,
            boolean forward, boolean backward, boolean left, boolean right) {
        Vector look = facing == null ? new Vector(0, 0, 1) : facing.clone().setY(0.0D);
        if (look.lengthSquared() < 1.0E-8D) look = new Vector(0, 0, 1);
        look.normalize();
        Vector rightVector = new Vector(-look.getZ(), 0.0D, look.getX());
        double forwardInput = (forward ? 1.0D : 0.0D) - (backward ? 1.0D : 0.0D);
        double rightInput = (right ? 1.0D : 0.0D) - (left ? 1.0D : 0.0D);
        Vector steering = look.clone().multiply(forwardInput)
                .add(rightVector.multiply(rightInput));
        if (steering.lengthSquared() > 1.0E-8D) return steering.normalize();
        Vector momentum = current.clone().setY(0.0D);
        return momentum.lengthSquared() > 1.0E-6D ? momentum.normalize() : look;
    }

    /** One-block rise followed by one bounded Yoshi up/down/up arc; never repeats on hold. */
    static Vector flutterVelocity(Vector current, Vector travelDirection, int flutterTicks) {
        Vector next = current.clone();
        if (flutterTicks <= FLUTTER_LIFT_TICKS) {
            next.setY(FLUTTER_LIFT_VERTICAL_SPEED);
        } else {
            int arcTick = Math.min(FLUTTER_WAVE_TICKS, flutterTicks - FLUTTER_LIFT_TICKS);
            double phase = (arcTick - 1) * 2.0D * Math.PI / FLUTTER_WAVE_TICKS;
            next.setY(FLUTTER_WAVE_BASE_Y + FLUTTER_WAVE_AMPLITUDE_Y * Math.cos(phase));
        }
        Vector direction = travelDirection.clone().setY(0.0D);
        if (direction.lengthSquared() > 1.0E-8D) {
            direction.normalize().multiply(FLUTTER_HORIZONTAL_SPEED);
            next.setX(direction.getX());
            next.setZ(direction.getZ());
        }
        return next;
    }

    /** Brief capped forward carry on completion or early release, then vanilla air drag. */
    static Vector flutterEndVelocity(Vector current) {
        Vector next = current.clone();
        Vector horizontal = current.clone().setY(0.0D);
        if (horizontal.lengthSquared() > 1.0E-8D) {
            horizontal.normalize().multiply(FLUTTER_END_FORWARD_SPEED);
            next.setX(horizontal.getX());
            next.setZ(horizontal.getZ());
        }
        return next;
    }

    static double acrobatFallDamage(String kit, double vanillaFallDamage) {
        return "acrobat".equals(kit) ? vanillaFallDamage * ACROBAT_FALL_DAMAGE_MULTIPLIER
                : vanillaFallDamage;
    }

    static Vector wallClingVelocity() {
        // Used with temporary gravity suppression and a fixed vertical anchor.
        // No upward velocity is applied: sticking must never act as wall climbing.
        return new Vector(0.0D, 0.0D, 0.0D);
    }

    /**
     * Original Dragon Escape leap velocity scaled by 50/75/100% for consecutive
     * airborne wall jumps. The minimum outward push is just collision recovery
     * when the player aims into the wall, not an extra launch multiplier.
     */
    static Vector wallKickVelocity(Vector facing, Vector wallNormal, double strength) {
        if (wallNormal == null || !Double.isFinite(strength) || strength <= 0.0D)
            return new Vector();
        Vector launch = ArcadePortRules.dragonEscapeLeapVelocity(facing, false)
                .multiply(strength);
        if (launch.lengthSquared() < 1.0E-8D) return new Vector();
        Vector outward = wallNormal.clone().setY(0.0D);
        if (outward.lengthSquared() > 1.0E-6D) {
            outward.normalize();
            double minOutward = WALL_KICK_MIN_OUTWARD_SPEED * strength;
            double outwardSpeed = launch.dot(outward);
            if (outwardSpeed < minOutward) {
                launch.add(outward.multiply(minOutward - outwardSpeed));
            }
        }
        return launch;
    }

    /**
     * Only physical collision-box contact with a collidable wall side counts.
     * Facing toward, away from or along the wall all qualify. Floor/ceiling
     * contact and merely being near a wall do not.
     */
    static Vector touchingWallNormal(Player player) {
        BoundingBox hitbox = player.getBoundingBox();
        for (int x = (int) Math.floor(hitbox.getMinX() - TOUCH_TOLERANCE);
                x <= Math.floor(hitbox.getMaxX() + TOUCH_TOLERANCE); x++) {
            for (int y = (int) Math.floor(hitbox.getMinY() + 0.15D);
                    y <= Math.floor(hitbox.getMaxY() - 0.15D); y++) {
                for (int z = (int) Math.floor(hitbox.getMinZ() - TOUCH_TOLERANCE);
                        z <= Math.floor(hitbox.getMaxZ() + TOUCH_TOLERANCE); z++) {
                    Block block = player.getWorld().getBlockAt(x, y, z);
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
