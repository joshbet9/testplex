package me.monstermaze;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Per-viewer mob cosmetics are normal gameplay; only an explicit admin kill switch disables them. */
class MonsterMazePacketDisguiseStartupSafetyTest {
    @Test
    void viewerLocalMobAppearancesAreEnabledByDefault() {
        YamlConfiguration config = new YamlConfiguration();
        assertTrue(MonsterMazePlugin.virtualMobAppearancesEnabled(config));
        assertTrue(MonsterMazePlugin.virtualMobAppearancesEnabled(null));
        config.set("mode", "modern");
        config.set("solo-mode", true);
        assertTrue(MonsterMazePlugin.virtualMobAppearancesEnabled(config));
    }

    @Test
    void explicitEmergencyDisableCanPreventPacketRendering() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("virtual-mob-appearances", false);
        assertFalse(MonsterMazePlugin.virtualMobAppearancesEnabled(config));
        config.set("virtual-mob-appearances", true);
        assertTrue(MonsterMazePlugin.virtualMobAppearancesEnabled(config));
    }
}
