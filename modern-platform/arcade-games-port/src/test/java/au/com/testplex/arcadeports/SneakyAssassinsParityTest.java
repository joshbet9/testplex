package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SneakyAssassinsParityTest {
    @Test void registersAndNormalizesTheFourSourceKits() {
        assertEquals("escape-artist", SneakyAssassinsMechanics.normalizeKit("Escape Artist"));
        assertEquals("escape-artist", SneakyAssassinsMechanics.normalizeKit("escape_artist"));
        assertEquals("assassin", SneakyAssassinsMechanics.normalizeKit("assassin"));
        assertEquals("briber", SneakyAssassinsMechanics.normalizeKit("BRIBER"));
        assertEquals("revealer", SneakyAssassinsMechanics.normalizeKit("revealer"));
        assertNull(SneakyAssassinsMechanics.normalizeKit("master-assassin"));
        assertNull(SneakyAssassinsMechanics.normalizeKit(null));
    }

    @Test void appliesSourceSwordDamageAcrossThePowerupProgression() {
        assertEquals(4.0D, SneakyAssassinsRules.swordDamage(Material.WOODEN_SWORD), 0.00001D);
        assertEquals(4.5D, SneakyAssassinsRules.swordDamage(Material.STONE_SWORD), 0.00001D);
        assertEquals(5.0D, SneakyAssassinsRules.swordDamage(Material.GOLDEN_SWORD), 0.00001D);
        assertEquals(6.0D, SneakyAssassinsRules.swordDamage(Material.IRON_SWORD), 0.00001D);
        assertEquals(7.0D, SneakyAssassinsRules.swordDamage(Material.DIAMOND_SWORD), 0.00001D);
        assertEquals(0.0D, SneakyAssassinsRules.swordDamage(Material.BOW), 0.00001D);
    }

    @Test void upgradesWeaponsInTheSourceOrderAndStopsAtDiamond() {
        assertEquals(Material.STONE_SWORD, SneakyAssassinsRules.nextWeapon(Material.WOODEN_SWORD));
        assertEquals(Material.GOLDEN_SWORD, SneakyAssassinsRules.nextWeapon(Material.STONE_SWORD));
        assertEquals(Material.IRON_SWORD, SneakyAssassinsRules.nextWeapon(Material.GOLDEN_SWORD));
        assertEquals(Material.DIAMOND_SWORD, SneakyAssassinsRules.nextWeapon(Material.IRON_SWORD));
        assertNull(SneakyAssassinsRules.nextWeapon(Material.DIAMOND_SWORD));
    }

    @Test void armourUpgradesLeatherToChainmailThenIron() {
        assertEquals(0, SneakyAssassinsRules.armorTier(Material.LEATHER_HELMET));
        assertEquals(1, SneakyAssassinsRules.armorTier(Material.CHAINMAIL_CHESTPLATE));
        assertEquals(2, SneakyAssassinsRules.armorTier(Material.IRON_BOOTS));
        assertEquals(-1, SneakyAssassinsRules.armorTier(Material.DIAMOND_BOOTS));
        assertEquals(Material.CHAINMAIL_BOOTS, SneakyAssassinsRules.nextArmor(Material.LEATHER_BOOTS));
        assertEquals(Material.IRON_LEGGINGS, SneakyAssassinsRules.nextArmor(Material.CHAINMAIL_LEGGINGS));
        assertNull(SneakyAssassinsRules.nextArmor(Material.IRON_HELMET));
    }

    @Test void fifthPowerupCreatesMasterAssassinAndPowerupCountIsBounded() {
        assertFalse(SneakyAssassinsRules.becomesMasterAssassin(3));
        assertTrue(SneakyAssassinsRules.becomesMasterAssassin(4));
        assertEquals(1, SneakyAssassinsRules.nextPowerUpCount(0));
        assertEquals(5, SneakyAssassinsRules.nextPowerUpCount(4));
        assertEquals(5, SneakyAssassinsRules.nextPowerUpCount(5));
    }

    @Test void matchesSourceTimingAndEffectRadii() {
        assertEquals(200, SneakyAssassinsRules.sourceNpcCount());
        assertEquals(60_000L, SneakyAssassinsRules.globalRevealCycleMillis());
        assertEquals(5_000L, SneakyAssassinsRules.revealerDurationMillis());
        assertEquals(1_200L, SneakyAssassinsRules.smokeCloakDurationMillis());
        assertEquals(8_000L, SneakyAssassinsRules.bribeCooldownMillis());
        assertEquals(144.0D, SneakyAssassinsRules.revealRadiusSquared(), 0.00001D);
        assertEquals(25.0D, SneakyAssassinsRules.smokeRadiusSquared(), 0.00001D);
    }

    @Test void sneakAssassinAwardConditionsRejectFalsePositiveKills() {
        assertTrue(SneakyAssassinsRules.shouldAwardYouNeedDirections(true, true, true, true));
        assertFalse(SneakyAssassinsRules.shouldAwardYouNeedDirections(false, true, true, true));
        assertFalse(SneakyAssassinsRules.shouldAwardYouNeedDirections(true, false, true, true));
        assertFalse(SneakyAssassinsRules.shouldAwardYouNeedDirections(true, true, false, true));
        assertFalse(SneakyAssassinsRules.shouldAwardYouNeedDirections(true, true, true, false));
        assertTrue(SneakyAssassinsRules.shouldAwardNotSoSneaky("assassin", false));
        assertFalse(SneakyAssassinsRules.shouldAwardNotSoSneaky("assassin", true));
        assertFalse(SneakyAssassinsRules.shouldAwardNotSoSneaky("briber", false));
        assertTrue(SneakyAssassinsRules.shouldAwardIdiot("escape-artist", false));
        assertFalse(SneakyAssassinsRules.shouldAwardIdiot("escape-artist", true));
        assertTrue(SneakyAssassinsRules.shouldAwardTheDuelsDuelsDuel(true, 2));
        assertFalse(SneakyAssassinsRules.shouldAwardTheDuelsDuelsDuel(true, 1));
        assertFalse(SneakyAssassinsRules.shouldAwardTheDuelsDuelsDuel(false, 3));
        assertFalse(SneakyAssassinsRules.shouldAwardProsecutor(4));
        assertTrue(SneakyAssassinsRules.shouldAwardProsecutor(5));
        assertTrue(SneakyAssassinsRules.isPowerUpKillWindow(1_000, 1));
        assertFalse(SneakyAssassinsRules.isPowerUpKillWindow(1_002, 1));
        assertFalse(SneakyAssassinsRules.isPowerUpKillWindow(0, 1));
        assertEquals(12_000L, SneakyAssassinsRules.firstPowerUpDelayMillis());
        assertEquals(7_000L, SneakyAssassinsRules.nextPowerUpDelayMillis());
        assertEquals(2, SneakyAssassinsRules.maxActivePowerUps());
    }

}
