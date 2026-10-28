package me.monstermaze.game;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerAudienceTest {
    @Test
    void gameAudienceIsScopedToTheConfiguredArenaWorld() {
        World arena = testWorld();
        World anotherGame = testWorld();

        assertTrue(GameManager.isInArenaWorld(arena, arena));
        assertFalse(GameManager.isInArenaWorld(arena, anotherGame));
        assertFalse(GameManager.isInArenaWorld(null, arena));
        assertFalse(GameManager.isInArenaWorld(arena, null));
    }

    private static World testWorld() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return "MonsterMazeAudienceTestWorld";
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
