package me.monstermaze.kit;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KitManagerNametagTest {
    @Test
    void rendersLiveAbilityStatusDirectlyAfterKitName() {
        assertEquals(" [Jumper 3]", ChatColor.stripColor(KitManager.kitNametagSuffix(KitType.JUMPER, "3")));
        assertEquals(" [Jumper 0]", ChatColor.stripColor(KitManager.kitNametagSuffix(KitType.JUMPER, "0")));
        assertEquals(" [Slowball READY]", ChatColor.stripColor(KitManager.kitNametagSuffix(KitType.SLOWBALL, "READY")));
        assertEquals(" [Slowball 20s]", ChatColor.stripColor(KitManager.kitNametagSuffix(KitType.SLOWBALL, "20s")));
        assertEquals(" [Body 2]", ChatColor.stripColor(KitManager.kitNametagSuffix(KitType.BODY_BUILDER, "2")));
        assertEquals(" [Repulsor 1]", ChatColor.stripColor(KitManager.kitNametagSuffix(KitType.REPULSOR, "1")));
        assertEquals(" [Maverick ∞]", ChatColor.stripColor(KitManager.kitNametagSuffix(KitType.MAVERICK, "∞")));
    }

    @Test
    void safelyHandlesUnavailableKitOrValue() {
        assertEquals("", KitManager.kitNametagSuffix(null, "1"));
        assertEquals(" [Jumper ?]", ChatColor.stripColor(KitManager.kitNametagSuffix(KitType.JUMPER, null)));
    }
}
