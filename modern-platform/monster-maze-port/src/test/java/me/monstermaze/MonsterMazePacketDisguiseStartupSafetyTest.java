package me.monstermaze;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards against auto-enabling packet mob disguises after the startup disconnect regression. */
class MonsterMazePacketDisguiseStartupSafetyTest {
    @Test
    void newAndLegacyConfigsLeavePacketRewriteDisabled() {
        YamlConfiguration config = new YamlConfiguration();
        assertFalse(MonsterMazePlugin.packetMobDisguisesEnabled(config));
        assertFalse(MonsterMazePlugin.packetMobDisguisesEnabled(null));
        config.set("mode", "modern");
        config.set("solo-mode", true);
        assertFalse(MonsterMazePlugin.packetMobDisguisesEnabled(config));
    }

    @Test
    void experimentalMobPacketsRequireAnExplicitOptIn() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("experimental-packet-mob-disguises", true);
        assertTrue(MonsterMazePlugin.packetMobDisguisesEnabled(config));
        config.set("experimental-packet-mob-disguises", false);
        assertFalse(MonsterMazePlugin.packetMobDisguisesEnabled(config));
    }
}
