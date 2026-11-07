package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class MasterBuildersPlotRuntimeTest {
    @Test
    void sourceOptionsIncludeAllGroundAndFriendlyParticleChoices() {
        assertEquals(23, MasterBuildersPlotRuntime.groundOptionCount());
        assertEquals(29, MasterBuildersPlotRuntime.particleOptionCount());
        assertEquals(0.1F, MasterBuildersPlotRuntime.flightSpeedForLevel(1), 0.0001F);
        assertEquals(1.0F, MasterBuildersPlotRuntime.flightSpeedForLevel(8), 0.0001F);
        assertThrows(IllegalArgumentException.class, () -> MasterBuildersPlotRuntime.flightSpeedForLevel(0));
        assertThrows(IllegalArgumentException.class, () -> MasterBuildersPlotRuntime.flightSpeedForLevel(9));
    }

    @Test
    void mobShopEnforcesGhastAndSixteenMobLimits() {
        assertTrue(MasterBuildersPlotRuntime.canAddMob(EntityType.PIG, 0));
        assertTrue(MasterBuildersPlotRuntime.canAddMob(EntityType.PIG, 15));
        assertFalse(MasterBuildersPlotRuntime.canAddMob(EntityType.PIG, 16));
        assertFalse(MasterBuildersPlotRuntime.canAddMob(EntityType.GHAST, 0));
        assertFalse(MasterBuildersPlotRuntime.canAddMob(null, 0));
    }

    @Test
    void particleShopEnforcesPlotBoundsAndTwentyFourMarkerLimit() {
        assertTrue(MasterBuildersPlotRuntime.canPlaceParticle(true, 0));
        assertTrue(MasterBuildersPlotRuntime.canPlaceParticle(true, 23));
        assertFalse(MasterBuildersPlotRuntime.canPlaceParticle(true, 24));
        assertFalse(MasterBuildersPlotRuntime.canPlaceParticle(false, 0));
        assertFalse(MasterBuildersPlotRuntime.canPlaceParticle(true, -1));
    }
}
