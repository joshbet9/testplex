package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/**
 * Spatial geometry for Turf Wars source maps and territory lookups.
 * Uses the live session state directly; no per-tick copies, allocations or new scheduling.
 */
final class TurfWarsTerritoryGeometry {
    private TurfWarsTerritoryGeometry() { }

    static void initializeTurfWarsMap(SessionState session, SourceMapCatalog.MapData map, Logger logger) {
        if (map == null) return;
        List<Location> turf = map.dataLocations("YELLOW");
        List<Location> red = map.dataLocations("RED");
        List<Location> redBase = map.dataLocations("PINK");
        List<Location> blue = map.dataLocations("BLUE");
        List<Location> blueBase = map.dataLocations("LIGHT_BLUE");
        if (turf.isEmpty() || red.isEmpty() || redBase.isEmpty() || blue.isEmpty() || blueBase.isEmpty()) {
            logger.warning("Turf Wars map " + map.mapName()
                    + " is missing its YELLOW/RED/PINK/BLUE/LIGHT_BLUE objective markers; using the bounded fallback objective.");
            return;
        }
        session.turfSurfaceLocations.clear();
        for (Location location : turf) session.turfSurfaceLocations.add(location.clone());
        session.turfRedFront = red.get(0).clone();
        session.turfRedBase = redBase.get(0).clone();
        session.turfBlueFront = blue.get(0).clone();
        session.turfBlueBase = blueBase.get(0).clone();
        session.turfAxisX = Integer.compare(session.turfRedFront.getBlockX(), session.turfBlueFront.getBlockX());
        session.turfAxisZ = Integer.compare(session.turfRedFront.getBlockZ(), session.turfBlueFront.getBlockZ());
        if (session.turfAxisX == 0 && session.turfAxisZ == 0) {
            session.turfSurfaceLocations.clear();
            logger.warning("Turf Wars map " + map.mapName()
                    + " has overlapping RED/BLUE frontline markers; using the bounded fallback objective.");
            return;
        }
        session.turfMarkerGeometry = true;
        for (Location surface : session.turfSurfaceLocations) {
            // Source ParseData colours ties blue: only a strictly closer red marker gets red turf.
            boolean redSide = surface.distanceSquared(session.turfRedFront)
                    < surface.distanceSquared(session.turfBlueFront);
            surface.getBlock().setType(redSide ? Material.RED_TERRACOTTA : Material.LIGHT_BLUE_TERRACOTTA, false);
        }
        session.turfFrontlineX = session.turfRedFront.getBlockX();
        session.turfInitialFrontlineX = session.turfFrontlineX;
        logger.info("Loaded source Turf Wars objective markers for " + map.mapName()
                + " (" + turf.size() + " turf blocks; axis " + session.turfAxisX + "/" + session.turfAxisZ + ").");
    }

    static int turfWarsRemainingLines(SessionState session, int team) {
        if (session.turfMarkerGeometry) {
            // Source getLines() uses X when the fronts differ on X, otherwise it uses Z.
            if (session.turfAxisX != 0) {
                return team == 0
                        ? ArcadePortRules.turfWarsRemainingLines(session.turfRedFront.getBlockX(), session.turfRedBase.getBlockX())
                        : ArcadePortRules.turfWarsRemainingLines(session.turfBlueFront.getBlockX(), session.turfBlueBase.getBlockX());
            }
            return team == 0
                    ? ArcadePortRules.turfWarsRemainingLines(session.turfRedFront.getBlockZ(), session.turfRedBase.getBlockZ())
                    : ArcadePortRules.turfWarsRemainingLines(session.turfBlueFront.getBlockZ(), session.turfBlueBase.getBlockZ());
        }
        if (team == 0) return Math.max(0, session.turfFrontlineMaxXExclusive - 1 - session.turfFrontlineX);
        if (team == 1) return Math.max(0, session.turfFrontlineX - session.turfFrontlineMinX);
        return Integer.MAX_VALUE;
    }

    static boolean isTurfWarsWool(Material material) {
        return material != null && material.name().endsWith("_WOOL");
    }

    static boolean isTurfWarsTerracotta(Material material) {
        return material != null && material.name().endsWith("_TERRACOTTA");
    }

    static int turfWarsTeamOnBlock(Block block) {
        if (block == null) return -1;
        Material type = block.getType();
        if (type == Material.RED_TERRACOTTA) return 0;
        if (type == Material.LIGHT_BLUE_TERRACOTTA) return 1;
        return -1;
    }

    /**
     * Mirrors TurfForts.Territory: start beneath the feet, look at adjacent clay/Wool
     * when grounded over a gap, then descend through non-clay blocks until the first
     * stained-clay surface. Wool is a footprint hint, never the territory colour itself.
     */
    static int turfWarsGroundTeam(Player player) {
        if (player == null || player.getWorld() == null) return -1;
        Block ground = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (ground.isEmpty() && player.isOnGround()) {
            Block adjacent = null;
            double nearestDistance = Double.POSITIVE_INFINITY;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block candidate = ground.getRelative(dx, 0, dz);
                    if (!isTurfWarsTerracotta(candidate.getType()) && !isTurfWarsWool(candidate.getType())) continue;
                    double distance = dx * dx + dz * dz;
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        adjacent = candidate;
                    }
                }
            }
            if (adjacent != null) ground = adjacent;
        }

        int minY = Math.max(0, player.getWorld().getMinHeight());
        while (!isTurfWarsTerracotta(ground.getType()) && ground.getY() > minY) {
            ground = ground.getRelative(BlockFace.DOWN);
        }
        if (!isTurfWarsTerracotta(ground.getType())) return -1;
        return turfWarsTeamOnBlock(ground);
    }

    static int turfWarsNearestSpawnTeam(SessionState session, Location location) {
        if (session == null || location == null) return -1;
        int nearest = -1;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Map.Entry<Integer, Location> entry : session.turfTeamSpawnCentres.entrySet()) {
            Location centre = entry.getValue();
            if (centre == null || centre.getWorld() != session.world) continue;
            double distance = centre.distanceSquared(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = entry.getKey();
            }
        }
        return nearest;
    }

}
