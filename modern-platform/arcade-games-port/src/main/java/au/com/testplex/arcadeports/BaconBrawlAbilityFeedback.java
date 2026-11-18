package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/** Names and input routing for Bacon Brawl's charge-meter ability feedback. */
final class BaconBrawlAbilityFeedback {
    private BaconBrawlAbilityFeedback() { }

    static String abilityName(String kit) {
        if (kit == null) return "Body Slam";
        return switch (kit) {
            case "mama-piggles" -> "Bacon Blast";
            case "chris-p-bacon" -> "Crispy";
            case "pig" -> "Cloak";
            case "brutus-bacon" -> "Brutus";
            default -> "Body Slam";
        };
    }

    static String readyMessage(String kit) {
        return ChatColor.BOLD + abilityName(kit) + " Ready";
    }

    static String maxMessage(String kit) {
        return ChatColor.BOLD + abilityName(kit) + " MAX";
    }

    static String primaryItemName(String kit) {
        return "chris-p-bacon".equals(kit) ? "Crispy Shot" : abilityName(kit);
    }

    static String itemName(String kit, Material item) {
        if ("chris-p-bacon".equals(kit) && item == Material.BLAZE_POWDER) return "Crispy Dash";
        if (item == Material.IRON_AXE) return primaryItemName(kit);
        return "";
    }

    static boolean isCrispyDashInput(String kit, Material item, boolean rightClick) {
        return "chris-p-bacon".equals(kit) && item == Material.BLAZE_POWDER && rightClick;
    }

    static String brutusBlockedHitLabel() {
        return "Shield!";
    }
}
