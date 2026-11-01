package me.monstermaze;

import me.monstermaze.maze.MazePathfinder;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MazePathfinderTest {
    @Test
    void distanceUsesWalkableRouteAndTheSafePadFootprint() {
        World world = testWorld();
        List<Location> path = List.of(
                at(world, 0, 64, 0),
                at(world, 1, 64, 0),
                at(world, 2, 64, 0),
                at(world, 3, 64, 0));

        MazePathfinder pathfinder = new MazePathfinder();
        assertFalse(pathfinder.isReady());
        pathfinder.rebuild(path, at(world, 3, 64, 0));

        assertTrue(pathfinder.isReady());
        assertEquals(0.0D, pathfinder.distanceInBlocks(at(world, 3, 64, 0)), 0.00001D);
        assertEquals(0.5D, pathfinder.distanceInBlocks(at(world, 0, 64, 0)), 0.00001D);
    }

    @Test
    void trailFollowsShortestPathTowardTheCurrentSafePad() {
        World world = testWorld();
        List<Location> path = List.of(
                at(world, 0, 64, 0),
                at(world, 1, 64, 0),
                at(world, 2, 64, 0),
                at(world, 3, 64, 0));

        MazePathfinder pathfinder = new MazePathfinder();
        pathfinder.rebuild(path, at(world, 3, 64, 0));

        List<Location> trail = pathfinder.trailTowardTarget(at(world, 0, 64, 0), 20, 1);
        assertFalse(trail.isEmpty());
        assertEquals(1, trail.get(0).getBlockX());
        assertEquals(0, trail.get(0).getBlockZ());
    }

    @Test
    void emptyPathClearsPreviousNavigationState() {
        World world = testWorld();
        MazePathfinder pathfinder = new MazePathfinder();
        pathfinder.rebuild(List.of(at(world, 0, 64, 0), at(world, 1, 64, 0)), at(world, 1, 64, 0));
        assertTrue(pathfinder.isReady());

        pathfinder.rebuild(List.of(), at(world, 1, 64, 0));
        assertFalse(pathfinder.isReady());
        assertTrue(Double.isInfinite(pathfinder.distanceInBlocks(at(world, 0, 64, 0))));
        assertTrue(pathfinder.trailTowardTarget(at(world, 0, 64, 0), 20, 1).isEmpty());
    }

    private static Location at(World world, double x, double y, double z) {
        return new Location(world, x, y, z);
    }

    private static World testWorld() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return "MonsterMazePathfinderTestWorld";
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    if (type == double.class) return 0.0D;
                    if (type == float.class) return 0.0F;
                    if (type == short.class) return (short) 0;
                    if (type == byte.class) return (byte) 0;
                    if (type == char.class) return (char) 0;
                    return null;
                });
    }
}
