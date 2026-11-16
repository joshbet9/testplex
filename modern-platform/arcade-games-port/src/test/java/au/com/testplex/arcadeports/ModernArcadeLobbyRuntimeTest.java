package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void lobbyNpcsPersistWhenTheLobbyChunkUnloadsDuringAMatch() {
        AtomicBoolean persistent = new AtomicBoolean();
        AtomicBoolean removeWhenFarAway = new AtomicBoolean(true);
        LivingEntity entity = (LivingEntity) Proxy.newProxyInstance(LivingEntity.class.getClassLoader(),
                new Class<?>[] {LivingEntity.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setPersistent")) persistent.set((boolean) args[0]);
                    if (method.getName().equals("setRemoveWhenFarAway")) {
                        removeWhenFarAway.set((boolean) args[0]);
                    }
                    return null;
                });

        ModernArcadeLobbyRuntime.configureLobbyNpcPersistence(entity);

        assertTrue(persistent.get(), "lobby kit/team NPCs must save with their chunk and reload after a match");
        assertFalse(removeWhenFarAway.get(), "lobby NPCs must not despawn when players are away");
    }

    @Test
    void botSetupShowsOnlyConfiguredRosterAndSupportsZeroBots() {
        assertEquals(0, ModernArcadeLobbyRuntime.visibleBotConfigurationCount(0));
        assertEquals(1, ModernArcadeLobbyRuntime.visibleBotConfigurationCount(1));
        assertEquals(5, ModernArcadeLobbyRuntime.visibleBotConfigurationCount(5));
        assertEquals(ArcadeTestPlayerManager.TARGET_ROSTER_SIZE,
                ModernArcadeLobbyRuntime.visibleBotConfigurationCount(ArcadeTestPlayerManager.TARGET_ROSTER_SIZE + 4));
        assertEquals(0, ModernArcadeLobbyRuntime.visibleBotConfigurationCount(-1));
    }

    @Test
    void onlyUnmanagedNonPlayerLivingEntitiesArePurgedFromTheLobby() {
        assertTrue(ModernArcadeLobbyRuntime.shouldPurgeUnmanagedLobbyMob(true, false, false));
        assertFalse(ModernArcadeLobbyRuntime.shouldPurgeUnmanagedLobbyMob(true, true, false),
                "players must not be purged");
        assertFalse(ModernArcadeLobbyRuntime.shouldPurgeUnmanagedLobbyMob(true, false, true),
                "tagged Arcade NPCs and showcases must be preserved");
        assertFalse(ModernArcadeLobbyRuntime.shouldPurgeUnmanagedLobbyMob(false, false, false),
                "non-living displays must be preserved");
    }
}
