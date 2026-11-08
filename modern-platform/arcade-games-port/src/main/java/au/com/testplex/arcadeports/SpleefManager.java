package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/** Spleef floor eligibility, slow-damage colours, and projectile impact geometry. */
final class SpleefManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.SPLEEF; }

    boolean isProtectedBlock(Material material) {
        return ArcadePortRules.isSpleefProtectedMaterial(material);
    }

    boolean isWoolOrTerracotta(Material material) {
        String name = material.name();
        return name.endsWith("_WOOL") || name.endsWith("_TERRACOTTA") || material == Material.TERRACOTTA;
    }

    Material nextColour(Material material) {
        return ArcadePortRules.nextSpleefSlowDamageMaterial(material);
    }

    /** Reborn's 3×3×3 search for a glancing snowball hit on the floor. */
    Block projectileImpactBlock(Location impact) {
        if (impact == null || impact.getWorld() == null) return null;
        Block direct = impact.getBlock();
        if (!direct.getType().isAir()) return direct;
        Block nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block candidate = direct.getRelative(dx, dy, dz);
                    if (candidate.getType().isAir()) continue;
                    double distance = candidate.getLocation().add(0.5D, 0.5D, 0.5D).distanceSquared(impact);
                    if (distance < nearestDistance) {
                        nearest = candidate;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }
}
