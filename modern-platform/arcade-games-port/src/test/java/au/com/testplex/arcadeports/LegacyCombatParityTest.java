package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LegacyCombatParityTest {
    @Test
    void keepsThePreCooldownSwingAndNaturalHealingCadence() {
        assertEquals(1_024.0D, ArcadePortRules.LEGACY_ATTACK_SPEED);
        assertEquals(80, ArcadePortRules.LEGACY_HEALTH_REGEN_TICKS);
        assertEquals(3.0F, ArcadePortRules.LEGACY_REGEN_EXHAUSTION);
    }

    @Test
    void matchesClassicSwordAndToolDamageBeforeArmor() {
        assertDamage(Material.WOODEN_SWORD, 4.0D);
        assertDamage(Material.GOLDEN_SWORD, 4.0D);
        assertDamage(Material.STONE_SWORD, 5.0D);
        assertDamage(Material.IRON_SWORD, 6.0D);
        assertDamage(Material.DIAMOND_SWORD, 7.0D);

        assertDamage(Material.WOODEN_AXE, 3.0D);
        assertDamage(Material.GOLDEN_AXE, 3.0D);
        assertDamage(Material.STONE_AXE, 4.0D);
        assertDamage(Material.IRON_AXE, 5.0D);
        assertDamage(Material.DIAMOND_AXE, 6.0D);

        assertDamage(Material.WOODEN_PICKAXE, 2.0D);
        assertDamage(Material.GOLDEN_PICKAXE, 2.0D);
        assertDamage(Material.STONE_PICKAXE, 3.0D);
        assertDamage(Material.IRON_PICKAXE, 4.0D);
        assertDamage(Material.DIAMOND_PICKAXE, 5.0D);

        assertDamage(Material.WOODEN_SHOVEL, 1.0D);
        assertDamage(Material.GOLDEN_SHOVEL, 1.0D);
        assertDamage(Material.STONE_SHOVEL, 2.0D);
        assertDamage(Material.IRON_SHOVEL, 3.0D);
        assertDamage(Material.DIAMOND_SHOVEL, 4.0D);
    }

    @Test
    void sharpnessUsesTheClassicPerLevelBonus() {
        assertEquals(5.25D, ArcadePortRules.legacyMeleeDamage(Material.IRON_SWORD, 1));
        assertEquals(13.25D, ArcadePortRules.legacyMeleeDamage(Material.DIAMOND_SWORD, 5));
        assertEquals(12.25D, ArcadePortRules.legacyMeleeDamage(Material.DIAMOND_AXE, 5));
        assertEquals(4.0D, ArcadePortRules.legacyMeleeDamage(Material.IRON_SWORD, -1));
    }

    @Test
    void naturalHealingOnlyRunsBelowMaximumHealthWithAtLeastEighteenFood() {
        assertTrue(ArcadePortRules.shouldLegacyNaturalRegen(18, 19.0D, 20.0D));
        assertTrue(ArcadePortRules.shouldLegacyNaturalRegen(20, 1.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldLegacyNaturalRegen(17, 19.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldLegacyNaturalRegen(20, 20.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldLegacyNaturalRegen(20, 0.0D, 20.0D));
    }

    private static void assertDamage(Material weapon, double expected) {
        assertEquals(expected, ArcadePortRules.legacyMeleeDamage(weapon, 0), 0.0001D, weapon.name());
    }
}
