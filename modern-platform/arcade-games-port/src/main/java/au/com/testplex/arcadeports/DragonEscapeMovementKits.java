package au.com.testplex.arcadeports;

import java.util.Collection;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * New Dragon Escape movement kits. Keeps control/physics rules independent of
 * the match lifecycle so they can be tested without a running Paper server.
 */
final class DragonEscapeMovementKits {
    static final double FLUTTER_ACTIVATION_COST = 0.08D;
    static final double FLUTTER_DRAIN_PER_TICK = 0.019D;
    static final double FLUTTER_REGEN_PER_TICK = 0.009D;
    static final double FLUTTER_START_Y = 0.16D;
    static final double FLUTTER_FALL_SPEED = -0.09D;
    static final double FLUTTER_HORIZONTAL_MAX = 0.20D;
    static final long WALL_KICK_COOLDOWN_MILLIS = 1_500L;
    static final double WALL_KICK_FORWARD_SPEED = 1.20D;
    static final double WALL_KICK_UPWARD_SPEED = 0.72D;
    private static final double TOUCH_TOLERANCE = 0.055D;
    private static final double MAX_PENETRATION = 0.025D;

    private DragonEscapeMovementKits() { }

    static final class FlutterState {
        double charge = 1.0D;
        boolean fluttering;
        boolean activated;
        boolean usedCurrentHold;
        int airborneTicks;

        void tick(boolean jumpHeld, boolean grounded) {
            activated = false;
            if (grounded) {
                airborneTicks = 0;
                fluttering = false;
                usedCurrentHold = false;
                charge = Math.min(1.0D, charge + FLUTTER_REGEN_PER_TICK);
                return;
            }
            airborneTicks++;
            if (!jumpHeld) {
                fluttering = false;
                usedCurrentHold = false;
            } else if (!usedCurrentHold && airborneTicks >= 3) {
                // Waiting for the third airborne tick allows players to keep
                // holding Space from their initial, normal jump.
                usedCurrentHold = true;
                if (charge >= FLUTTER_ACTIVATION_COST) {
                    charge -= FLUTTER_ACTIVATION_COST;
                    fluttering = true;
                    activated = true;
                }
            }
            if (fluttering) {
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
        boolean releasedSinceJump;
        int airborneTicks;
        long nextWallKickAt;

        boolean pressedForWallKick(boolean jumpHeld, boolean grounded, long now) {
            if (grounded) {
                airborneTicks = 0;
                releasedSinceJump = false;
                previouslyHeld = jumpHeld;
                return false;
            }
            airborneTicks++;
            if (!jumpHeld) releasedSinceJump = true;
            boolean freshPress = jumpHeld && !previouslyHeld && releasedSinceJump
                    && airborneTicks >= 2 && now >= nextWallKickAt;
            previouslyHeld = jumpHeld;
            return freshPress;
        }
    }

    static Vector flutterVelocity(Vector current, boolean activated) {
        Vector next = current.clone();
        next.setY(activated ? Math.max(next.getY(), FLUTTER_START_Y)
                : Math.max(next.getY(), FLUTTER_FALL_SPEED));
        double horizontal = Math.hypot(next.getX(), next.getZ());
        if (horizontal > FLUTTER_HORIZONTAL_MAX) {
            double scale = FLUTTER_HORIZONTAL_MAX / horizontal;
            next.setX(next.getX() * scale);
            next.setZ(next.getZ() * scale);
        }
        return next;
    }

    /** The Champions Assassin Leap wall kick launches along the player's facing. */
    static Vector wallKickVelocity(Vector facing) {
        Vector forward = facing.clone().setY(0.0D);
        if (forward.lengthSquared() < 1.0E-6D) return new Vector();
        forward.normalize().multiply(WALL_KICK_FORWARD_SPEED);
        forward.setY(WALL_KICK_UPWARD_SPEED);
        return forward;
    }

    /**
     * Contact means the player's actual bounding box touches a collidable
     * side-face of the wall (not a proximity check or an adjacent block scan).
     * The player must face away from the wall, as in Champions' back-wall kick.
     * Blocks below the feet alone cannot count as a wall.
     */
    static Vector touchingWallNormal(Player player) {
        BoundingBox hitbox = player.getBoundingBox();
        Vector facing = player.getLocation().getDirection().setY(0.0D);
        if (facing.lengthSquared() < 1.0E-6D) return null;
        facing.normalize();
        Vector winner = null;
        double bestDot = 0.35D;
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
                        Vector normal = contactNormal(hitbox, worldShape, facing);
                        if (normal == null) continue;
                        double dot = normal.dot(facing);
                        if (dot > bestDot) {
                            bestDot = dot;
                            winner = normal;
                        }
                    }
                }
            }
        }
        return winner;
    }

    /** Testable side-contact check, using full collision shapes (including slabs/fences). */
    static Vector contactNormal(BoundingBox player, BoundingBox wall, Vector facing) {
        double yOverlap = Math.min(player.getMaxY() - 0.12D, wall.getMaxY())
                - Math.max(player.getMinY() + 0.15D, wall.getMinY());
        if (yOverlap < 0.25D) return null;
        double xOverlap = Math.min(player.getMaxX(), wall.getMaxX())
                - Math.max(player.getMinX(), wall.getMinX());
        double zOverlap = Math.min(player.getMaxZ(), wall.getMaxZ())
                - Math.max(player.getMinZ(), wall.getMinZ());
        Vector best = null;
        double score = 0.35D;
        if (zOverlap > 0.08D) {
            double westWallGap = player.getMinX() - wall.getMaxX();
            if (westWallGap >= -MAX_PENETRATION && westWallGap <= TOUCH_TOLERANCE
                    && facing.getX() > score) {
                best = new Vector(1, 0, 0);
                score = facing.getX();
            }
            double eastWallGap = wall.getMinX() - player.getMaxX();
            if (eastWallGap >= -MAX_PENETRATION && eastWallGap <= TOUCH_TOLERANCE
                    && -facing.getX() > score) {
                best = new Vector(-1, 0, 0);
                score = -facing.getX();
            }
        }
        if (xOverlap > 0.08D) {
            double northWallGap = player.getMinZ() - wall.getMaxZ();
            if (northWallGap >= -MAX_PENETRATION && northWallGap <= TOUCH_TOLERANCE
                    && facing.getZ() > score) {
                best = new Vector(0, 0, 1);
                score = facing.getZ();
            }
            double southWallGap = wall.getMinZ() - player.getMaxZ();
            if (southWallGap >= -MAX_PENETRATION && southWallGap <= TOUCH_TOLERANCE
                    && -facing.getZ() > score) {
                best = new Vector(0, 0, -1);
            }
        }
        return best;
    }
}
