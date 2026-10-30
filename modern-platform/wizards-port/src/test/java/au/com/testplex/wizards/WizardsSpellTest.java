package au.com.testplex.wizards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WizardsSpellTest {
    @Test
    void allTwentyOneHistoricalSpellDefinitionsAreRepresented() {
        assertEquals(21, WizardsSpell.values().length);
        assertNotNull(WizardsSpell.fromId("anvil-drop"));
        assertNotNull(WizardsSpell.fromId("wizards-compass"));
        assertEquals(WizardsSpell.WIZARDS_COMPASS, WizardsSpell.fromId("wizard's compass"));
        assertNull(WizardsSpell.fromId("not-a-spell"));
    }

    @Test
    void firstMilestoneOnlyExposesImplementedSpellEffectsToLoot() {
        assertEquals(10, WizardsSpell.implementedSpells().size());
        assertTrue(WizardsSpell.MANA_BOLT.implemented());
        assertTrue(WizardsSpell.FIREBALL.implemented());
        assertTrue(WizardsSpell.HEAL.implemented());
        assertFalse(WizardsSpell.ANVIL_DROP.implemented());
        assertFalse(WizardsSpell.SUMMON_WOLVES.implemented());
    }

    @Test
    void manaAndCooldownProgressionUseTheHistoricalPerLevelDeltas() {
        assertEquals(30, WizardsSpell.FIREBALL.manaCost(1));
        assertEquals(27, WizardsSpell.FIREBALL.manaCost(2));
        assertEquals(24, WizardsSpell.FIREBALL.manaCost(3));
        assertEquals(15, WizardsSpell.MANA_BOLT.manaCost(1));
        assertEquals(13, WizardsSpell.MANA_BOLT.manaCost(2));
        assertEquals(11, WizardsSpell.MANA_BOLT.manaCost(3));
        assertEquals(15, WizardsSpell.FIREBALL.cooldownSeconds(1, 1.0D));
        assertEquals(13, WizardsSpell.FIREBALL.cooldownSeconds(2, 1.0D));
        assertEquals(11, WizardsSpell.FIREBALL.cooldownSeconds(3, 1.0D));
        assertEquals(8, WizardsSpell.FIREBALL.cooldownSeconds(3, 0.7D));
    }

    @Test
    void manaAndCooldownNeverDropToZero() {
        assertEquals(1, WizardsSpell.NAPALM.manaCost(1));
        assertEquals(1, WizardsSpell.NAPALM.cooldownSeconds(5, 0.5D));
        assertEquals(1, WizardsSpell.RAINBOW_BEAM.cooldownSeconds(1, 0.1D));
    }
}
