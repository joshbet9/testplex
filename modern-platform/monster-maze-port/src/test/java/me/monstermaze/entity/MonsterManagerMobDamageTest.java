package me.monstermaze.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MonsterManagerMobDamageTest {
    @Test
    void twoHeartsAreAttributedToTheContactingMob() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Double> amount = new AtomicReference<>();
        AtomicReference<Entity> source = new AtomicReference<>();
        LivingEntity monster = mock(LivingEntity.class, (method, args) -> null);
        Player player = mock(Player.class, (method, args) -> {
            if (method.getName().equals("damage")) {
                calls.incrementAndGet();
                assertEquals(2, args.length, "Must use damage(amount, monster), not anonymous damage(amount)");
                amount.set((Double) args[0]);
                source.set((Entity) args[1]);
            }
            return null;
        });

        MonsterManager.applyMobContactDamage(player, monster, 4.0D);

        assertEquals(1, calls.get());
        assertEquals(4.0D, amount.get());
        assertSame(monster, source.get());
    }

    @Test
    void invalidOrNonPositiveDamageDoesNotApply() {
        AtomicInteger calls = new AtomicInteger();
        LivingEntity monster = mock(LivingEntity.class, (method, args) -> null);
        Player player = mock(Player.class, (method, args) -> {
            if (method.getName().equals("damage")) calls.incrementAndGet();
            return null;
        });
        MonsterManager.applyMobContactDamage(player, monster, 0.0D);
        MonsterManager.applyMobContactDamage(player, monster, -1.0D);
        MonsterManager.applyMobContactDamage(player, monster, Double.NaN);
        MonsterManager.applyMobContactDamage(player, monster, Double.POSITIVE_INFINITY);
        MonsterManager.applyMobContactDamage(null, monster, 4.0D);
        MonsterManager.applyMobContactDamage(player, null, 4.0D);
        assertEquals(0, calls.get());
    }

    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> type, Invocation action) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("toString")) return type.getSimpleName() + "Mock";
                    return action.invoke(method, args);
                });
    }
}
