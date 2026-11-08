package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/** Runner's palette-aware trail rules; shared Arcade still owns timers and block decay. */
final class RunnerManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.RUNNER; }

    Map<String, Integer> scanMapPalette(SessionState session, SourceMapCatalog.MapData mapData, int halfSize) {
        Map<String, Integer> palette = new HashMap<>();
        int minX = mapData != null && mapData.hasBounds() ? mapData.minX() : -halfSize;
        int maxX = mapData != null && mapData.hasBounds() ? mapData.maxX() : halfSize + 1;
        int minZ = mapData != null && mapData.hasBounds() ? mapData.minZ() : -halfSize;
        int maxZ = mapData != null && mapData.hasBounds() ? mapData.maxZ() : halfSize + 1;
        int minY = mapData != null && mapData.hasVerticalBounds()
                ? mapData.minY() : Math.max(session.world.getMinHeight(), 0);
        int maxY = mapData != null && mapData.hasVerticalBounds()
                ? mapData.maxY() : Math.min(session.world.getMaxHeight() - 1, 128);
        if (mapData != null && !mapData.hasVerticalBounds()) {
            List<Location> spawns = mapData.spawnLocations();
            if (!spawns.isEmpty()) {
                double lowestSpawn = spawns.stream().mapToDouble(Location::getY).min().orElse(minY);
                double highestSpawn = spawns.stream().mapToDouble(Location::getY).max().orElse(maxY);
                minY = Math.max(session.world.getMinHeight(), (int) Math.floor(lowestSpawn) - 16);
                maxY = Math.min(session.world.getMaxHeight() - 1, (int) Math.ceil(highestSpawn) + 32);
            }
        }
        minY = Math.max(minY, session.world.getMinHeight());
        maxY = Math.min(maxY, session.world.getMaxHeight() - 1);
        if (maxX <= minX || maxZ <= minZ || maxY < minY) return palette;

        long area = (long) (maxX - minX) * (long) (maxZ - minZ);
        int columnStride = Math.max(1, (int) Math.ceil(Math.sqrt(area / 12_000.0D)));
        for (int x = minX; x < maxX; x += columnStride) {
            for (int z = minZ; z < maxZ; z += columnStride) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = session.world.getBlockAt(x, y, z);
                    if (!block.getType().isSolid() || block.isLiquid()
                            || !block.getRelative(BlockFace.UP).getType().isAir()) continue;
                    String colour = paletteColour(block.getType());
                    if (colour != null) palette.merge(colour, 1, Integer::sum);
                }
            }
        }
        return palette;
    }

    String paletteColour(Material material) {
        String name = material.name();
        for (String suffix : List.of("_CONCRETE_POWDER", "_STAINED_GLASS_PANE", "_STAINED_GLASS",
                "_CONCRETE", "_GLAZED_TERRACOTTA", "_TERRACOTTA", "_WOOL")) {
            if (!name.endsWith(suffix)) continue;
            String colour = name.substring(0, name.length() - suffix.length())
                    .toLowerCase(Locale.ROOT).replace('_', '-');
            if (ArcadePortRules.runnerTrailColours().contains(colour)) return colour;
        }
        return null;
    }

    Material trailMaterial(SessionState session, Player owner) {
        String selected = session.runnerTrailOptions.getOrDefault(owner.getUniqueId(), "trail-cyan");
        String colour = selected.startsWith("trail-") ? selected.substring("trail-".length()) : selected;
        Material material = Material.matchMaterial(
                colour.toUpperCase(Locale.ROOT).replace('-', '_') + "_TERRACOTTA");
        return material == null ? Material.CYAN_TERRACOTTA : material;
    }
}
