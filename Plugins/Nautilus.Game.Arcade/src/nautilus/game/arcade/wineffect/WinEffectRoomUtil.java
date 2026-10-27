package nautilus.game.arcade.wineffect;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/** Clears the dedicated win-effect build volume before a themed room is assembled. */
public final class WinEffectRoomUtil
{
    private WinEffectRoomUtil() {}
    public static void clear(Location center, int radius, int below, int above)
    {
        if (center == null || center.getWorld() == null) return;
        World world = center.getWorld();
        int minY = Math.max(1, center.getBlockY() - below);
        int maxY = Math.min(world.getMaxHeight() - 1, center.getBlockY() + above);
        int bx = center.getBlockX(), bz = center.getBlockZ(), radiusSq = radius * radius;
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
            {
                if (x * x + z * z > radiusSq) continue;
                for (int y = minY; y <= maxY; y++)
                    world.getBlockAt(bx + x, y, bz + z).setType(Material.AIR, false);
            }
    }
}
