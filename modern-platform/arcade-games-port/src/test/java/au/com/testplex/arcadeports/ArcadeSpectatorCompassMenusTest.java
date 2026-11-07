package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeSpectatorCompassMenusTest {
    @Test void preservesSharedSpectatorMenuTitleAndNullSafety() {
        assertTrue(ArcadeSpectatorCompassMenus.isBombLobbersCompassMenuTitle(
                ChatColor.DARK_GREEN + "Spectator Menu"));
        assertFalse(ArcadeSpectatorCompassMenus.isBombLobbersCompassMenuTitle("Spectator Menu"));
        assertFalse(ArcadeSpectatorCompassMenus.isQuiverTrackingCompass(null));
        assertFalse(ArcadeSpectatorCompassMenus.isBombTrackingCompass(null));
        assertNull(ArcadeSpectatorCompassMenus.nearestTarget(List.of(), null));
    }
}
