package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** The source Gladiators' small homing Happy Villager trail used to guide arena advances. */
final class GladiatorsParticleTrail {
    private final Player viewer;
    private final Location target;
    private final Location current;
    private final Vector velocity;

    GladiatorsParticleTrail(Player viewer, Location target) {
        this.viewer = viewer;
        this.target = target.clone();
        this.current = viewer.getLocation().clone().add(0.0D, 1.0D, 0.0D);
        this.velocity = viewer.getLocation().getDirection();
        if (velocity.getY() < 0.0D) velocity.setY(0.0D);
        if (velocity.lengthSquared() < 1.0E-8D) velocity.setX(1.0D);
        velocity.normalize();
    }

    /** Advance one source-style steering step; return true once the path reaches its target. */
    boolean tick() {
        if (viewer == null || !viewer.isOnline() || viewer.getWorld() != current.getWorld()
                || target.getWorld() != current.getWorld()) return true;
        Vector towardTarget = target.toVector().subtract(current.toVector());
        towardTarget.normalize().multiply(0.15D);
        velocity.add(towardTarget);
        if (velocity.lengthSquared() < 1.0E-8D) return true;
        velocity.normalize();
        current.add(velocity.clone().multiply(0.5D));
        viewer.spawnParticle(Particle.HAPPY_VILLAGER, current, 3, 0.03D, 0.03D, 0.03D, 0.0D);
        current.getWorld().playSound(current, Sound.BLOCK_FIRE_EXTINGUISH, 0.2F, 3.0F);
        return current.distanceSquared(target) < 16.0D;
    }
}
