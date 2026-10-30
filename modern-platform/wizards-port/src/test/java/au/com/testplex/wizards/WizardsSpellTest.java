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
        assertEquals(15, WizardsSpell.implementedSpells().size());
        assertTrue(WizardsSpell.MANA_BOLT.implemented());
        assertTrue(WizardsSpell.FIREBALL.implemented());
        assertTrue(WizardsSpell.HEAL.implemented());
        assertTrue(WizardsSpell.ANVIL_DROP.implemented());
        assertTrue(WizardsSpell.ICE_SHARDS.implemented());
        assertTrue(WizardsSpell.SPECTRAL_ARROW.implemented());
        assertTrue(WizardsSpell.SUMMON_WOLVES.implemented());
        assertTrue(WizardsSpell.WEB_SHOT.implemented());
        assertFalse(WizardsSpell.IMPLODE.implemented());
        assertFalse(WizardsSpell.NAPALM.implemented());
        assertFalse(WizardsSpell.RAINBOW_BEAM.implemented());
        assertFalse(WizardsSpell.RAINBOW_ROAD.implemented());
        assertFalse(WizardsSpell.RUMBLE.implemented());
        assertFalse(WizardsSpell.TRAP_RUNE.implemented());
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
        assertEquals(30, WizardsSpell.FIREBALL.manaCost(1, 1.0D));
        assertEquals(21, WizardsSpell.FIREBALL.manaCost(1, 0.7D));
        assertEquals(30, WizardsSpell.HEAL.cooldownSeconds(1, 0.5D));
    }

    @Test
    void spellFormulasUseSourceMinimumsAndSupportLateGameSurges() {
        assertEquals(60, WizardsSpell.NAPALM.manaCost(1));
        assertEquals(2, WizardsSpell.NAPALM.cooldownSeconds(5, 0.1D));
        assertEquals(0, WizardsSpell.NAPALM.manaCost(1, 0.0D));
        assertEquals(0, WizardsSpell.RAINBOW_BEAM.cooldownSeconds(1, 0.0D));
    }
}
