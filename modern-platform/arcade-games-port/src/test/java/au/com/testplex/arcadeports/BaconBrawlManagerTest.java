package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
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
}
