package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernArcadeLobbyRuntimeTest {
    @Test
    void kitNpcArmorIsAlwaysPaddedOrTruncatedToFourEquipmentSlots() {
        assertArrayEquals(new Material[] {Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS,
                        Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET},
                ModernArcadeLobbyRuntime.npcArmorSlots(new Material[] {Material.LEATHER_BOOTS,
                        Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET,
                        Material.DIAMOND_HELMET}));
        assertArrayEquals(new Material[] {Material.IRON_BOOTS, null, null, null},
                ModernArcadeLobbyRuntime.npcArmorSlots(new Material[] {Material.IRON_BOOTS}));
        assertArrayEquals(new Material[] {null, null, null, null}, ModernArcadeLobbyRuntime.npcArmorSlots(null));
    }

    @Test
    void kitZombieSpawnAlwaysForcesAdultForm() {
        AtomicBoolean adultFormApplied = new AtomicBoolean();
        Zombie zombie = (Zombie) Proxy.newProxyInstance(Zombie.class.getClassLoader(),
                new Class<?>[] {Zombie.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setAdult")) adultFormApplied.set(true);
                    return null;
                });

        ModernArcadeLobbyRuntime.normalizeKitZombieAge(zombie);

        assertTrue(adultFormApplied.get(), "kit NPC zombies must be explicitly grown before display");
    }
}
