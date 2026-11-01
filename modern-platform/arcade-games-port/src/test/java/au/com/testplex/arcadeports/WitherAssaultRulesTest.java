package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class WitherAssaultRulesTest {
    @Test
    void onlyTheThreeSourceHumanKitsAreSelectable() {
        assertEquals("archer", WitherAssaultRules.normalizeHumanKit("Archer"));
        assertEquals("medic", WitherAssaultRules.normalizeHumanKit("human-medic"));
        assertEquals("editor", WitherAssaultRules.normalizeHumanKit("Human Editor"));
        assertNull(WitherAssaultRules.normalizeHumanKit(null));
        assertNull(WitherAssaultRules.normalizeHumanKit(""));
        assertNull(WitherAssaultRules.normalizeHumanKit("miner"));
        assertNull(WitherAssaultRules.normalizeHumanKit("wither"));
    }

    @Test
    void lobbyCatalogueExposesTheSameSourceHumanKitSet() {
        assertEquals(List.of("archer", "medic", "editor"),
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.WITHER_ASSAULT, List.of())
                        .stream().map(choice -> choice.id).toList());
    }

    @Test
    void editorCannotModifyBlocksBelowTheFourBlockFloorOrUnderAnotherPlayer() {
        assertTrue(WitherAssaultRules.canEditorModifyBlock(72, 76.0D, false));
        assertTrue(WitherAssaultRules.canEditorModifyBlock(80, 76.0D, false));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(71, 76.0D, false));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(76, 76.0D, true));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(76, Double.NaN, false));
    }

    @Test
    void witherSwordClicksMatchTheSourceAbilityLayout() {
        assertTrue(WitherAssaultRules.shouldLaunchSkull("golden_sword", true));
        assertFalse(WitherAssaultRules.shouldLaunchSkull("golden_sword", false));
        assertTrue(WitherAssaultRules.shouldLaunchMinions("diamond_sword", true));
        assertFalse(WitherAssaultRules.shouldLaunchMinions("diamond_sword", false));
        assertTrue(WitherAssaultRules.shouldStartFireStorm("golden_sword", true));
        assertTrue(WitherAssaultRules.shouldStartFireStorm("diamond_sword", true));
        assertFalse(WitherAssaultRules.shouldStartFireStorm("golden_sword", false));
        assertFalse(WitherAssaultRules.shouldStartFireStorm("shield", true));
        assertFalse(WitherAssaultRules.shouldStartFireStorm("compass", true));
    }

    @Test
    void archerAndEditorShareFletcherAndSmokeArrowButMedicDoesNot() {
        assertTrue(WitherAssaultRules.hasFletcher("archer"));
        assertTrue(WitherAssaultRules.hasFletcher("editor"));
        assertFalse(WitherAssaultRules.hasFletcher("medic"));
        assertTrue(WitherAssaultRules.shouldTrackSmokeArrow(0, "archer", true, true));
        assertTrue(WitherAssaultRules.shouldTrackSmokeArrow(0, "editor", true, true));
        assertFalse(WitherAssaultRules.shouldTrackSmokeArrow(0, "medic", true, true));
        assertFalse(WitherAssaultRules.shouldTrackSmokeArrow(1, "archer", true, true));
        assertFalse(WitherAssaultRules.shouldTrackSmokeArrow(0, "editor", false, true));
        assertFalse(WitherAssaultRules.shouldTrackSmokeArrow(0, "archer", true, false));
    }
}
