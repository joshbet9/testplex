package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

/**
 * Authoritative, per-match territory map for Super Sploor.
 *
 * The map is scanned once at match setup. Only explicitly eligible surfaces are
 * counted, and each eligible block has exactly one owner: neutral (-1), Red (0)
 * or Blue (1). The original BlockData is retained for exact teardown restoration.
 */
public final class SuperSploorEngine {
    public record PaintResult(int neutralClaimed, int enemyReclaimed, int friendlyUnchanged) {
        public int changedUnits() { return neutralClaimed + enemyReclaimed; }
    }

    private static final String[] PAINTABLE_SUFFIXES = {
            "_WOOL", "_STAINED_GLASS", "_STAINED_GLASS_PANE",
            "_CONCRETE", "_TERRACOTTA"
    };

    private final World world;
    private final int redSpawnCount;
    private final int blueSpawnCount;
    private final List<Location> spawnExclusionCenters;
    private final Map<Block, BlockData> originals = new LinkedHashMap<>();
    private final Map<Block, Integer> owners = new HashMap<>();
    private int redUnits;
    private int blueUnits;

    public SuperSploorEngine(World world, SourceMapCatalog.MapData map) {
        if (world == null || map == null || !map.hasBounds()
                || map.maxX() <= map.minX() || map.maxZ() <= map.minZ()) {
            throw new IllegalArgumentException("Super Sploor requires an archived map with valid X/Z bounds.");
        }
        this.world = world;
        List<Location> redSpawns = map.teamSpawnLocationsForTeam(0, 2);
        List<Location> blueSpawns = map.teamSpawnLocationsForTeam(1, 2);
        if (redSpawns.size() < 2 || blueSpawns.size() < 2) {
            throw new IllegalArgumentException("Super Sploor map needs at least two authored spawn points for each team; found Red="
                    + redSpawns.size() + ", Blue=" + blueSpawns.size() + ".");
        }
        this.redSpawnCount = redSpawns.size();
        this.blueSpawnCount = blueSpawns.size();
        this.spawnExclusionCenters = new java.util.ArrayList<>();
        for (Location spawn : redSpawns) this.spawnExclusionCenters.add(spawn.clone());
        for (Location spawn : blueSpawns) this.spawnExclusionCenters.add(spawn.clone());
        int minY = map.hasVerticalBounds() ? map.minY() : Math.max(world.getMinHeight(), 32);
        int maxY = map.hasVerticalBounds() ? map.maxY() : Math.min(world.getMaxHeight() - 1, 128);
        minY = Math.max(world.getMinHeight(), minY);
        maxY = Math.min(world.getMaxHeight() - 1, maxY);
        if (maxY < minY) {
            throw new IllegalArgumentException("Super Sploor map has invalid vertical bounds.");
        }

        long scanVolume = (long) (map.maxX() - map.minX())
                * (long) (map.maxZ() - map.minZ())
                * (long) (maxY - minY + 1);
        if (scanVolume <= 0L || scanVolume > 2_500_000L) {
            throw new IllegalArgumentException("Super Sploor map scan volume is outside the safe limit: " + scanVolume);
        }

        for (int x = map.minX(); x < map.maxX(); x++) {
            for (int z = map.minZ(); z < map.maxZ(); z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!isPaintable(block.getType()) || isSpawnSafeTerritoryBlock(block)) continue;
                    originals.putIfAbsent(block, block.getBlockData().clone());
                    owners.put(block, -1);
                }
            }
        }

        if (owners.size() < 100) {
            throw new IllegalArgumentException("Super Sploor map contains too few eligible paintable blocks: " + owners.size());
        }
    }

    private boolean isSpawnSafeTerritoryBlock(Block block) {
        for (Location spawn : spawnExclusionCenters) {
            if (SuperSploorRules.isSpawnSafeTerritoryBlock(
                    block.getX(), block.getY(), block.getZ(),
                    spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ())) return true;
        }
        return false;
    }

    public static boolean isPaintable(Material material) {
        if (material == null) return false;
        String name = material.name();
        for (String suffix : PAINTABLE_SUFFIXES) {
            if (name.endsWith(suffix)) return true;
        }
        return false;
    }

    public PaintResult paint(Location centre, int team, int radius) {
        if (centre == null || centre.getWorld() != world || (team != 0 && team != 1)) {
            return new PaintResult(0, 0, 0);
        }
        int safeRadius = Math.max(0, Math.min(6, radius));
        int neutral = 0;
        int enemy = 0;
        int friendly = 0;
        int squared = safeRadius * safeRadius;
        int cx = centre.getBlockX();
        int cy = centre.getBlockY();
        int cz = centre.getBlockZ();

        for (int dx = -safeRadius; dx <= safeRadius; dx++) {
            for (int dy = -safeRadius; dy <= safeRadius; dy++) {
                for (int dz = -safeRadius; dz <= safeRadius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > squared) continue;
                    Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    Integer previous = owners.get(block);
                    if (previous == null) continue;
                    if (previous == team) {
                        friendly++;
                        continue;
                    }
                    if (previous == -1) neutral++;
                    else enemy++;
                    owners.put(block, team);
                    if (team == 0) {
                        redUnits++;
                        if (previous == 1) blueUnits--;
                    } else {
                        blueUnits++;
                        if (previous == 0) redUnits--;
                    }
                    Material target = teamMaterial(block.getType(), team);
                    if (target != null && target != block.getType()) block.setType(target, false);
                }
            }
        }
        return new PaintResult(neutral, enemy, friendly);
    }

    private Material teamMaterial(Material current, int team) {
        String source = current.name();
        String suffix = null;
        for (String candidate : PAINTABLE_SUFFIXES) {
            if (source.endsWith(candidate)) {
                suffix = candidate.substring(1);
                break;
            }
        }
        if (suffix == null) return null;
        String colour = team == 0 ? "RED" : "LIGHT_BLUE";
        return Material.matchMaterial(colour + "_" + suffix);
    }

    public int owner(Block block) {
        return owners.getOrDefault(block, -2);
    }

    public boolean isFriendly(Block block, int team) {
        return (team == 0 || team == 1) && owner(block) == team;
    }

    public boolean isFriendlySurface(Location location, int team) {
        if (location == null || location.getWorld() != world || (team != 0 && team != 1)) return false;
        Block feet = location.getBlock();
        Block below = feet.getRelative(0, -1, 0);
        if (isFriendly(feet, team) || isFriendly(below, team)) return true;
        for (org.bukkit.block.BlockFace face : org.bukkit.block.BlockFace.values()) {
            if (face.getModY() == 0 && face != org.bukkit.block.BlockFace.SELF
                    && isFriendly(feet.getRelative(face), team)) return true;
        }
        return false;
    }

    public boolean isFriendlyWallAdjacent(Location location, int team) {
        if (location == null || location.getWorld() != world || (team != 0 && team != 1)) return false;
        Block feet = location.getBlock();
        for (org.bukkit.block.BlockFace face : org.bukkit.block.BlockFace.values()) {
            if (face.getModY() == 0 && face != org.bukkit.block.BlockFace.SELF
                    && isFriendly(feet.getRelative(face), team)) return true;
        }
        return false;
    }

    public int redSpawnCount() { return redSpawnCount; }
    public int blueSpawnCount() { return blueSpawnCount; }

    public int totalUnits() { return owners.size(); }
    public int redUnits() { return redUnits; }
    public int blueUnits() { return blueUnits; }
    public int neutralUnits() { return Math.max(0, owners.size() - redUnits - blueUnits); }
    public Set<Block> paintableBlocks() { return new HashSet<>(owners.keySet()); }

    public void restore() {
        for (Map.Entry<Block, BlockData> entry : originals.entrySet()) {
            Block block = entry.getKey();
            if (block != null && block.getWorld() == world) block.setBlockData(entry.getValue(), false);
        }
        owners.clear();
        originals.clear();
        redUnits = 0;
        blueUnits = 0;
    }
}
