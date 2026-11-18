package au.com.testplex.arcadeports;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * New Dragon Escape movement kits. Keeps control/physics rules independent of
 * the match lifecycle so they can be tested without a running Paper server.
 */
final class DragonEscapeMovementKits {
    // Keep rule helpers independent of Paper's runtime-only sound registry.
    static final String FLUTTER_SOUND_NAME = "ENTITY_SLIME_JUMP";
    static final String WALL_KICK_SOUND_NAME = "ENTITY_FIREWORK_ROCKET_LAUNCH";
    static final double FLUTTER_ACTIVATION_COST = 0.08D;
    // Approximately 34% less charge per tick than the original hover implementation.
    static final double FLUTTER_DRAIN_PER_TICK = 0.0125D;
    static final double FLUTTER_REGEN_PER_TICK = 0.009D;
    // Yoshi-style movement: repeated, smooth rising and falling arcs.
    // The slightly negative baseline prevents unlimited height gain on one hold.
    static final int FLUTTER_WAVE_TICKS = 28;
    static final double FLUTTER_WAVE_BASE_Y = -0.025D;
    static final double FLUTTER_WAVE_AMPLITUDE_Y = 0.22D;
    static final double FLUTTER_HORIZONTAL_MAX = 0.20D;
    static final double WALL_KICK_FORWARD_SPEED = 1.20D;
    static final double WALL_KICK_UPWARD_SPEED = 0.72D;
    static final double WALL_KICK_PITCH_LIFT = 0.60D;
    static final double WALL_KICK_MIN_UPWARD_SPEED = 0.20D;
    static final double WALL_KICK_MAX_UPWARD_SPEED = 1.20D;
    static final double WALL_KICK_MIN_OUTWARD_SPEED = 0.28D;
    static final int WALL_CLING_MAX_TICKS = 10; // 0.5 seconds at 20 TPS.
    static final double WALL_CLING_GRAVITY_COMPENSATION_Y = 0.078D;
    static final double ACROBAT_FALL_DAMAGE_MULTIPLIER = 0.50D;
    private static final double TOUCH_TOLERANCE = 0.055D;
    private static final double MAX_PENETRATION = 0.025D;

    private DragonEscapeMovementKits() { }

    static final class FlutterState {
        double charge = 1.0D;
        boolean fluttering;
        boolean activated;
        boolean usedCurrentHold;
        int airborneTicks;
        int flutterTicks;

        void tick(boolean jumpHeld, boolean grounded) {
            activated = false;
            if (grounded) {
                airborneTicks = 0;
                flutterTicks = 0;
                fluttering = false;
                usedCurrentHold = false;
                charge = Math.min(1.0D, charge + FLUTTER_REGEN_PER_TICK);
                return;
            }
            airborneTicks++;
            if (!jumpHeld) {
                fluttering = false;
                flutterTicks = 0;
                usedCurrentHold = false;
            } else if (!usedCurrentHold && airborneTicks >= 3) {
                // Waiting for the third airborne tick allows players to keep
                // holding Space from their initial, normal jump.
                usedCurrentHold = true;
                if (charge >= FLUTTER_ACTIVATION_COST) {
                    charge -= FLUTTER_ACTIVATION_COST;
                    fluttering = true;
                    flutterTicks = 0;
                    activated = true;
                }
            }
            if (fluttering) {
                flutterTicks++;
                charge = Math.max(0.0D, charge - FLUTTER_DRAIN_PER_TICK);
                if (charge == 0.0D) fluttering = false;
            } else {
                // The XP bar is energy, not a per-jump charge counter. Recover
                // whenever Flutter is inactive, even in midair between bursts.
                charge = Math.min(1.0D, charge + FLUTTER_REGEN_PER_TICK);
            }
        }
    }

    static final class AcrobatState {
        boolean previouslyHeld;
        boolean crouchConsumed;
        int wallClingTicksRemaining;

        /**
         * Wall kicks are chainable with no time-based cooldown. A new press
         * after releasing Space is required for each kick, and the caller
         * checks actual wall contact for every attempted kick.
         */
        boolean pressedForWallKick(boolean jumpHeld) {
            boolean freshPress = jumpHeld && !previouslyHeld;
            previouslyHeld = jumpHeld;
            return freshPress;
        }

        /**
         * Holding crouch against a wall arrests momentum for at most half a
         * second. One cling per crouch hold: it cannot refresh until Shift
         * is released, and walking/jumping away cancels the cling immediately.
         * The caller runs this only while the ability is enabled.
         */
        boolean tickWallCling(boolean crouching, boolean grounded, boolean touchingWall) {
            if (!crouching) {
                crouchConsumed = false;
                wallClingTicksRemaining = 0;
                return false;
            }
            if (grounded || !touchingWall) {
                wallClingTicksRemaining = 0;
                return false;
            }
            if (!crouchConsumed) {
                wallClingTicksRemaining = WALL_CLING_MAX_TICKS;
                crouchConsumed = true;
            }
            if (wallClingTicksRemaining <= 0) return false;
            wallClingTicksRemaining--;
            return true;
        }

        void cancelWallClingForKick() {
            wallClingTicksRemaining = 0;
            crouchConsumed = true;
        }
    }

    /**
     * A held flutter traces repeating up-down-up arcs, rather than applying a
     * one-off hop followed by a fixed downward hover velocity. The wave is
     * phase-locked to each fresh flutter burst, so release/repress begins a
     * new arc; every continuous hold retains its repeating flutter movement.
     */
    static Vector flutterVelocity(Vector current, int flutterTicks) {
        Vector next = current.clone();
        int phaseTicks = Math.max(0, flutterTicks - 1) % FLUTTER_WAVE_TICKS;
        double phase = phaseTicks * 2.0D * Math.PI / FLUTTER_WAVE_TICKS;
        next.setY(FLUTTER_WAVE_BASE_Y + FLUTTER_WAVE_AMPLITUDE_Y * Math.cos(phase));
        double horizontal = Math.hypot(next.getX(), next.getZ());
        if (horizontal > FLUTTER_HORIZONTAL_MAX) {
            double scale = FLUTTER_HORIZONTAL_MAX / horizontal;
            next.setX(next.getX() * scale);
            next.setZ(next.getZ() * scale);
        }
        return next;
    }

    static double acrobatFallDamage(String kit, double vanillaFallDamage) {
        return "acrobat".equals(kit) ? vanillaFallDamage * ACROBAT_FALL_DAMAGE_MULTIPLIER
                : vanillaFallDamage;
    }

    static Vector wallClingVelocity() {
        // Bukkit applies gravity after each velocity update. This slight
        // upward compensation keeps the player nearly stationary for 10 ticks
        // without changing persistent Player gravity or requiring cleanup.
        return new Vector(0.0D, WALL_CLING_GRAVITY_COMPENSATION_Y, 0.0D);
    }

    /**
     * A Champions-inspired kick steered by the player's full look angle.
     * Yaw controls horizontal direction; looking up trades horizontal distance
     * for height, and looking down produces a flatter/lower jump. The outward
     * minimum prevents a valid wall kick from disappearing into the collision
     * surface when the player looks directly at the wall.
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
