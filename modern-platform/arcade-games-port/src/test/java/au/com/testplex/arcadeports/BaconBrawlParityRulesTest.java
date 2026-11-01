package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BaconBrawlParityRulesTest {
    @Test
    void chrisPBaconResolvesToTheRestoredSourceKit() {
        assertEquals("chris-p-bacon", ArcadePortRules.normalizeBaconBrawlKit("Chris P. Bacon"));
        assertEquals("chris-p-bacon", ArcadePortRules.normalizeBaconBrawlKit("crispy_bacon"));
        assertEquals("chris-p-bacon", ArcadePortRules.normalizeBaconBrawlKit("chris"));
    }

    @Test
    void onlyOrdinaryWeaponHitsChargeTheBaconMeter() {
        assertTrue(ArcadePortRules.shouldChargeBaconMeter("melee"));
        assertTrue(ArcadePortRules.shouldChargeBaconMeter("projectile hit"));
        assertFalse(ArcadePortRules.shouldChargeBaconMeter("body-slam"));
        assertFalse(ArcadePortRules.shouldChargeBaconMeter("bacon-blast"));
        assertFalse(ArcadePortRules.shouldChargeBaconMeter("crispy-bacon"));
        assertFalse(ArcadePortRules.shouldChargeBaconMeter("body-slam-recoil"));
    }

    @Test
    void bombLobbersPowerupWavesAverageOneMinuteAndKeepOneDropPerSide() {
        assertEquals(60_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(2));
        assertEquals(60_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(16));
        assertEquals(2, ArcadePortRules.bombLobbersPowerupMaxActive(2));
        assertEquals(2, ArcadePortRules.bombLobbersPowerupMaxActive(16));
    }
}
