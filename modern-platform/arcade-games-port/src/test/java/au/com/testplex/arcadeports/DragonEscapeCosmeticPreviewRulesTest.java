package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DragonEscapeCosmeticPreviewRulesTest {
    @Test
    void finishBeaconPreviewUsesTheRealStainedGlassColourIncludingSilver() {
        assertEquals(Material.WHITE_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("white"));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("light-blue"));
        assertEquals(Material.LIGHT_GRAY_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("silver"));
        assertEquals(Material.RED_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("red"));
        assertEquals(Material.WHITE_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("unknown"));
    }
}
