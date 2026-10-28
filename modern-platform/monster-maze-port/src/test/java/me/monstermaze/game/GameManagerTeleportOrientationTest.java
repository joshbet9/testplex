package me.monstermaze.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTeleportOrientationTest {
    @Test
    void yawFacesTheDirectionOfSafePadTravel() {
        World world = testWorld();
        Location from = new Location(world, 0.5D, 64.0D, 0.5D);

        assertEquals(0.0F,
                GameManager.yawToward(from, new Location(world, 0.5D, 64.0D, 10.5D)), 0.001F);
        assertEquals(-90.0F,
                GameManager.yawToward(from, new Location(world, 10.5D, 64.0D, 0.5D)), 0.001F);
        assertEquals(90.0F,
                GameManager.yawToward(from, new Location(world, -9.5D, 64.0D, 0.5D)), 0.001F);
        assertEquals(180.0F,
                Math.abs(GameManager.yawToward(from, new Location(world, 0.5D, 64.0D, -9.5D))), 0.001F);
    }

    @Test
    void preservesCurrentYawWhenThereIsNoHorizontalTravelDirection() {
        World world = testWorld();
        Location from = new Location(world, 12.5D, 64.0D, -4.5D, 37.0F, 25.0F);
        Location sameColumn = new Location(world, 12.5D, 80.0D, -4.5D);

        assertEquals(37.0F, GameManager.yawToward(from, sameColumn), 0.001F);
    }

    @Test
    void fallsBackSafelyForMissingOrDifferentWorldLocations() {
        World first = testWorld();
        World second = testWorld();
        Location from = new Location(first, 0.5D, 64.0D, 0.5D, 23.0F, 10.0F);

        assertEquals(23.0F,
                GameManager.yawToward(from, new Location(second, 10.5D, 64.0D, 0.5D)), 0.001F);
        assertEquals(23.0F, GameManager.yawToward(from, null), 0.001F);
        assertEquals(0.0F, GameManager.yawToward(null, from), 0.001F);
    }

    private static World testWorld() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return "MonsterMazeTeleportOrientationTestWorld";
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
