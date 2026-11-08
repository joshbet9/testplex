package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

class SkyWarsManagerTest {
    @Test
    void controllerClaimsOnlySkyWars() {
        SkyWarsManager skywars = new SkyWarsManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(skywars);

        assertSame(skywars, registry.get(ArcadePortGame.SKY_WARS));
        assertNull(registry.get(ArcadePortGame.SNEAKY_ASSASSINS));
    }

    @Test
    void kitCooldownAndMissingItemChecksPreserveExistingDefaults() {
        SkyWarsManager skywars = new SkyWarsManager();
        assertEquals(30_000L, SkyWarsManager.cooldownDuration("ice"));
        assertEquals(30_000L, SkyWarsManager.cooldownDuration("fire"));
        assertEquals(15_000L, SkyWarsManager.cooldownDuration("metal"));
        assertEquals(25_000L, SkyWarsManager.cooldownDuration("air"));
        assertEquals(500L, SkyWarsManager.cooldownDuration("earth"));
        assertFalse(skywars.isAbilitySkill(null));
        assertFalse(skywars.isTrackingCompass(null));
        assertFalse(skywars.isSkill(null, "Ice Bridge"));
    }

    @Test
    void spectatorCompassDoesNotAllocateStateForAnEmptyMatch() {
        SkyWarsManager skywars = new SkyWarsManager();
        SessionState session = new SessionState(ArcadePortGame.SKY_WARS, null, List.of(), false);

        skywars.updateCompass(session);

        assertEquals(0, session.skyWarsSpectatorTeleportAt.size());
    }
}
