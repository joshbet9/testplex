package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class BaconBrawlManagerTest {
    private final BaconBrawlManager manager = new BaconBrawlManager(
            Logger.getLogger(BaconBrawlManagerTest.class.getName()));

    @Test
    void keepsBaconAbilityNamesForEveryKit() {
        SessionState session = new SessionState(ArcadePortGame.BACON_BRAWL, null, List.of(), false);
        UUID playerId = UUID.randomUUID();

        assertEquals("Body Slam", manager.baconAbilityName(session, playerId));
        session.baconKits.put(playerId, "mama-piggles");
        assertEquals("Bacon Blast", manager.baconAbilityName(session, playerId));
        session.baconKits.put(playerId, "chris-p-bacon");
        assertEquals("Crispy Bacon", manager.baconAbilityName(session, playerId));
        session.baconKits.put(playerId, "pig");
        assertEquals("Cloak", manager.baconAbilityName(session, playerId));
        session.baconKits.put(playerId, "brutus-bacon");
        assertEquals("Brutus Bacon", manager.baconAbilityName(session, playerId));
    }

    @Test
    void retainsChargeSpecificAbilityDescriptions() {
        assertTrue(manager.baconAbilityMeterLore("el-muchacho-pigo", 20).contains("20% charged"));
        assertTrue(manager.baconAbilityMeterLore("el-muchacho-pigo", 50).contains("Body Slam"));
        assertTrue(manager.baconAbilityMeterLore("mama-piggles", 100).contains("Blast force"));
        assertTrue(manager.baconAbilityMeterLore("pig", 100).contains("Cloak lasts"));
        assertTrue(manager.baconAbilityMeterLore("brutus-bacon", 100).contains("Brutus guard"));
        assertTrue(manager.baconAbilityMeterLore("chris-p-bacon", 50).contains("Crispy shots"));
        assertFalse(manager.isBaconMeterItem(null));
    }

    @Test
    void registersAsOneManagedGameWithoutReplacingStandaloneModuleLifecycle() {
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(manager);

        assertSame(manager, registry.get(ArcadePortGame.BACON_BRAWL));
        assertNull(registry.get(ArcadePortGame.EVOLUTION));
    }

    @Test
    void spectatorMenuRecognitionAndEmptyTargetResolutionRemainGameSpecific() {
        SessionState session = new SessionState(ArcadePortGame.BACON_BRAWL, null, List.of(), false);

        assertTrue(manager.isBaconCompassMenuTitle(ChatColor.DARK_GREEN + "Spectator Menu"));
        assertFalse(manager.isBaconCompassMenuTitle("Spectator Menu"));
        assertFalse(manager.isBaconCompassMenuTitle(null));
        assertFalse(manager.isBaconTrackingCompass(null));
        assertNull(manager.nearestAliveBaconTarget(session, null));
    }

    @Test
    void knockbackAndProjectileGeometryHelpersKeepDefensiveBounds() {
        Location source = new Location(null, 0.0D, 65.0D, 0.0D);
        Location target = new Location(null, 2.0D, 65.0D, 0.0D);
        Vector velocity = manager.baconBrawlKnockbackVelocity(source, target,
                2.0D, 1.0D, null, true);

        assertTrue(Double.isFinite(velocity.getX()));
        assertTrue(Double.isFinite(velocity.getY()));
        assertTrue(velocity.getX() > 0.0D);
        assertEquals(-1.0D, manager.baconBlastFirstBlockFraction(null, source, target));
        assertEquals(-1.0D, manager.baconBlastPlayerHitFraction(null, source, target, 2.0D));
    }

    @Test
    void timingHudStillHasAnInitialSwingPrompt() {
        SessionState session = new SessionState(ArcadePortGame.BACON_BRAWL, null, List.of(), false);
        assertTrue(manager.baconTimingStatusText(session, UUID.randomUUID(), 1_000L).contains("swing"));
    }
}
