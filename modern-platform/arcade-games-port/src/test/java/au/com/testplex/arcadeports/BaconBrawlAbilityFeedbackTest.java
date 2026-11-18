package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaconBrawlAbilityFeedbackTest {
    @Test
    void namesEveryKitAbilityForReadyAndMaximumMeterFeedback() {
        assertEquals(ChatColor.BOLD + "Body Slam Ready", BaconBrawlAbilityFeedback.readyMessage("el-muchacho-pigo"));
        assertEquals(ChatColor.BOLD + "Bacon Blast MAX", BaconBrawlAbilityFeedback.maxMessage("mama-piggles"));
        assertEquals(ChatColor.BOLD + "Crispy Ready", BaconBrawlAbilityFeedback.readyMessage("chris-p-bacon"));
        assertEquals(ChatColor.BOLD + "Cloak Ready", BaconBrawlAbilityFeedback.readyMessage("pig"));
        assertEquals(ChatColor.BOLD + "Brutus MAX", BaconBrawlAbilityFeedback.maxMessage("brutus-bacon"));
    }

    @Test
    void crispyDashUsesRightClickOnBlazePowderAndNotSneaking() {
        assertTrue(BaconBrawlAbilityFeedback.isCrispyDashInput(
                "chris-p-bacon", Material.BLAZE_POWDER, true));
        assertFalse(BaconBrawlAbilityFeedback.isCrispyDashInput(
                "chris-p-bacon", Material.BLAZE_POWDER, false));
        assertFalse(BaconBrawlAbilityFeedback.isCrispyDashInput(
                "chris-p-bacon", Material.IRON_AXE, true));
        assertFalse(BaconBrawlAbilityFeedback.isCrispyDashInput(
                "mama-piggles", Material.BLAZE_POWDER, true));
        assertEquals("Crispy Shot", BaconBrawlAbilityFeedback.primaryItemName("chris-p-bacon"));
        assertEquals("Crispy Shot", BaconBrawlAbilityFeedback.itemName("chris-p-bacon", Material.IRON_AXE));
        assertEquals("Crispy Dash", BaconBrawlAbilityFeedback.itemName("chris-p-bacon", Material.BLAZE_POWDER));
        assertEquals("Body Slam", BaconBrawlAbilityFeedback.itemName("el-muchacho-pigo", Material.IRON_AXE));
    }

    @Test
    void blockedBrutusHitHasDedicatedFeedbackLabel() {
        assertEquals("Shield!", BaconBrawlAbilityFeedback.brutusBlockedHitLabel());
    }
}
