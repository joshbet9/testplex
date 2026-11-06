package me.monstermaze.game;

import au.com.testplex.arcadeports.GameDebugRuntimeOverrides;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SafePadFootprintTest {
    @Test
    void countsOnlyPlayersWithinTheVisibleFiveByFiveFootprintAndHeight() {
        World world = testWorld();
        Location center = new Location(world, 100.5D, 64.0D, -40.5D);

        assertTrue(SafePad.isWithinFootprint(center, 63, new Location(world, 100.5D, 64.0D, -40.5D)));
        assertTrue(SafePad.isWithinFootprint(center, 63, new Location(world, 102.49D, 66.0D, -42.49D)));
        assertFalse(SafePad.isWithinFootprint(center, 63, new Location(world, 103.0D, 64.0D, -40.5D)));
        assertFalse(SafePad.isWithinFootprint(center, 63, new Location(world, 100.5D, 63.0D, -40.5D)));
        assertFalse(SafePad.isWithinFootprint(center, 63, new Location(world, 100.5D, 68.0D, -40.5D)));
    }

    @Test
    void neverTreatsTheSameCoordinatesInAnotherWorldAsSafe() {
        World arenaWorld = testWorld();
        World otherWorld = testWorld();
        Location center = new Location(arenaWorld, 0.5D, 64.0D, 0.5D);
        Location sameCoordinatesElsewhere = new Location(otherWorld, 0.5D, 64.0D, 0.5D);

        assertFalse(SafePad.isWithinFootprint(center, 63, sameCoordinatesElsewhere));
        assertFalse(SafePad.isWithinFootprint(null, 63, sameCoordinatesElsewhere));
        assertFalse(SafePad.isWithinFootprint(center, 63, null));
    }

    @Test
    void debugCaptureRadiusChangesSafePadEligibilityAndClearsBackToFiveByFiveDefault() {
        World world = testWorld();
        Location center = new Location(world, 100.5D, 64.0D, -40.5D);
        Location nearEdge = new Location(world, 102.0D, 64.0D, -40.5D);
        try {
            assertTrue(SafePad.isWithinFootprint(center, 63, nearEdge, MazeMode.ORIGINAL));
            GameDebugRuntimeOverrides.activate(Map.of("MONSTER_MAZE_SAFE_PAD_CAPTURE_RADIUS_BLOCKS", 1.0D));
            assertFalse(SafePad.isWithinFootprint(center, 63, nearEdge, MazeMode.ORIGINAL));
        } finally {
            GameDebugRuntimeOverrides.clear();
        }
        assertTrue(SafePad.isWithinFootprint(center, 63, nearEdge, MazeMode.ORIGINAL));
    }

    private static World testWorld() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return "MonsterMazeSafePadTestWorld";
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
