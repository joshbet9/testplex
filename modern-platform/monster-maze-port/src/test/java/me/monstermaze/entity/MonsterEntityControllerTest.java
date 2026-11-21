package me.monstermaze.entity;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MonsterEntityControllerTest {
    @Test
    void nativeNavigationIsReenabledOnEachControllerUpdate() {
        AtomicBoolean aware = new AtomicBoolean(false);
        AtomicBoolean ai = new AtomicBoolean(false);
        Mob mob = (Mob) Proxy.newProxyInstance(Mob.class.getClassLoader(), new Class<?>[]{Mob.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setAware")) {
                        aware.set((Boolean) args[0]);
                        return null;
                    }
                    if (method.getName().equals("isAware")) return aware.get();
                    if (method.getName().equals("setAI")) {
                        ai.set((Boolean) args[0]);
                        return null;
                    }
                    if (method.getName().equals("hasAI")) return ai.get();
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    return primitiveDefault(method.getReturnType());
                });

        MonsterEntityController.enableNativeNavigation(mob);

        assertTrue(ai.get());
        assertTrue(aware.get(), "tick updates must not disable Paper waypoint navigation");
    }

    @Test
    void forgetRemovesSkinAndStrongEntityReference() {
        UUID id = UUID.randomUUID();
        World world = testWorld();
        LivingEntity entity = testEntity(id, world, false);
        try {
            MonsterEntityController.setSkin(entity, "witch");
            assertEquals("witch", MonsterEntityController.getSkin(id));
            assertTrue(MonsterEntityController.getSkinnedEntities(world).contains(entity));

            MonsterEntityController.forget(entity);

            assertNull(MonsterEntityController.getSkin(id));
            assertFalse(MonsterEntityController.getSkinnedEntities(world).contains(entity));
        } finally {
            MonsterEntityController.forget(entity);
        }
    }

    @Test
    void viewerRefreshPrunesEntitiesThatAreAlreadyDead() {
        UUID id = UUID.randomUUID();
        World world = testWorld();
        LivingEntity deadEntity = testEntity(id, world, true);

        MonsterEntityController.setSkin(deadEntity, "creeper");
        assertEquals("creeper", MonsterEntityController.getSkin(id));

        assertFalse(MonsterEntityController.getSkinnedEntities(world).contains(deadEntity));
        assertNull(MonsterEntityController.getSkin(id));
    }

    private static World testWorld() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return "MonsterMazeDisguiseTestWorld";
                    return primitiveDefault(method.getReturnType());
                });
    }

    private static LivingEntity testEntity(UUID id, World world, boolean dead) {
        return (LivingEntity) Proxy.newProxyInstance(LivingEntity.class.getClassLoader(),
                new Class<?>[]{LivingEntity.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getUniqueId": return id;
                        case "getWorld": return world;
                        case "isDead": return dead;
                        case "isValid": return !dead;
                        case "equals": return proxy == args[0];
                        case "hashCode": return System.identityHashCode(proxy);
                        case "toString": return "MonsterMazeDisguiseTestEntity-" + id;
                        default: return primitiveDefault(method.getReturnType());
                    }
                });
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return (char) 0;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0D;
        return null;
    }
}
