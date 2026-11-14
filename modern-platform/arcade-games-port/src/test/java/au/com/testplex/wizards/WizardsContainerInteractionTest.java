package au.com.testplex.wizards;

import org.bukkit.event.block.Action;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WizardsContainerInteractionTest {
    @Test
    void heldWandDoesNotConsumeRightClickOnAContainer() {
        assertTrue(WizardsGameManager.shouldDeferWandClickToContainer(
                Action.RIGHT_CLICK_BLOCK, true, true));
    }

    @Test
    void wandStillOpensSpellMenuWhenRightClickingAirOrNonContainerBlocks() {
        assertFalse(WizardsGameManager.shouldDeferWandClickToContainer(
                Action.RIGHT_CLICK_AIR, true, false));
        assertFalse(WizardsGameManager.shouldDeferWandClickToContainer(
                Action.RIGHT_CLICK_BLOCK, true, false));
    }

    @Test
    void nonWandItemsAndLeftClicksKeepTheirExistingInteraction() {
        assertFalse(WizardsGameManager.shouldDeferWandClickToContainer(
                Action.RIGHT_CLICK_BLOCK, false, true));
        assertFalse(WizardsGameManager.shouldDeferWandClickToContainer(
                Action.LEFT_CLICK_BLOCK, true, true));
    }
}
