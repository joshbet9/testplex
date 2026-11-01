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
    void smokeArrowsBelongOnlyToAliveHumanArchers() {
        assertTrue(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, "archer", true));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, "archer", false));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(1, "archer", true));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, "medic", true));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, "editor", true));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, null, true));
    }

    @Test
    void smokeArrowsBlindEveryWitherStrictlyInsideTheSixBlockRadius() {
        assertTrue(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(0.0D, 6.0D));
        assertTrue(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(35.999D, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(36.0D, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(36.001D, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(-1.0D, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(Double.NaN, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(1.0D, Double.NaN));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(1.0D, 0.0D));
    }

    @Test
    void editorCannotModifyBlocksBelowTheFourBlockFloorOrUnderAnotherPlayer() {
        assertTrue(WitherAssaultRules.canEditorModifyBlock(72, 76.0D, false));
        assertTrue(WitherAssaultRules.canEditorModifyBlock(80, 76.0D, false));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(71, 76.0D, false));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(76, 76.0D, true));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(76, Double.NaN, false));
    }
}
