package au.com.testplex.arcadeports;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.util.Vector;

/** Small, pure rules used by the Turf Wars kit UI and loadout. */
public final class TurfWarsRules {
    public static final double MELEE_KNOCKBACK_DAMPING = 0.5D;
    public static final double MELEE_KNOCKBACK_HORIZONTAL = 0.4D;
    public static final double MELEE_KNOCKBACK_VERTICAL = 0.36D;

    private TurfWarsRules() { }

    /** Materials in the exact left-to-right order used for the kit hotbar. */
    public static List<Material> startingHotbar(String kit) {
        if ("infiltrator".equalsIgnoreCase(kit)) {
            return List.of(Material.IRON_SWORD, Material.BOW);
        }
        return List.of(Material.BOW);
    }

    public static String fortifierPerkDescription(int woolCap) {
        return "Passively receives one team-coloured wool every 4 seconds, up to "
                + Math.max(1, woolCap) + " held (Marksman carries up to 8).";
    }

    /** Recreates the basic 1.8-style melee impulse when custom damage bypasses Bukkit's hit response. */
    public static Vector meleeKnockback(Vector currentVelocity, Vector horizontalDirection) {
        if (currentVelocity == null || horizontalDirection == null
                || !Double.isFinite(currentVelocity.lengthSquared())
                || !Double.isFinite(horizontalDirection.lengthSquared())
                || horizontalDirection.lengthSquared() < 0.001D) return null;
        return currentVelocity.clone().multiply(MELEE_KNOCKBACK_DAMPING)
                .add(horizontalDirection.clone().setY(0.0D).normalize()
                        .multiply(MELEE_KNOCKBACK_HORIZONTAL))
                .setY(Math.max(currentVelocity.getY() * MELEE_KNOCKBACK_DAMPING,
                        MELEE_KNOCKBACK_VERTICAL));
    }
}
