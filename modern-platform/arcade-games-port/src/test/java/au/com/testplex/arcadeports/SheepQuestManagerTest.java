package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SheepQuestManagerTest {
    @Test
    void controllerClaimsOnlySheepQuest() {
        SheepQuestManager sheep = new SheepQuestManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(sheep);

        assertSame(sheep, registry.get(ArcadePortGame.SHEEP_QUEST));
        assertNull(registry.get(ArcadePortGame.MICRO_BATTLES));
    }

    @Test
    void missingSheepDefaultsToWhiteWool() {
        assertEquals(Material.WHITE_WOOL, SheepQuestManager.woolMaterial(null));
    }

    @Test
    void missingPlayerOrSessionDoesNotChangeCarryState() {
        SheepQuestManager sheep = new SheepQuestManager();
        SessionState session = new SessionState(ArcadePortGame.SHEEP_QUEST, null, List.of(), false);
        sheep.updateCarryUi(session, null, true);
        sheep.updateCarryUi(null, null, false);
        assertTrue(session.sheepQuestMaxCarried.isEmpty());
    }

    @Test
    void fourTeamPaletteAndFallbackPenPositionsMatchLegacyLayout() {
        assertEquals(DyeColor.RED, SheepQuestManager.teamDyeColor(0));
        assertEquals(DyeColor.LIGHT_BLUE, SheepQuestManager.teamDyeColor(1));
        assertEquals(DyeColor.LIME, SheepQuestManager.teamDyeColor(2));
        assertEquals(DyeColor.YELLOW, SheepQuestManager.teamDyeColor(3));
        assertEquals(ChatColor.AQUA, SheepQuestManager.teamChatColor(1));
        assertEquals(Material.YELLOW_WOOL, SheepQuestManager.teamWool(3));
        assertEquals("Red", SheepQuestManager.teamName(0));
        assertEquals("BLUE", SheepQuestManager.penMarker(1));
        assertEquals(-14, SheepQuestManager.penCenterX(1));
        assertEquals(14, SheepQuestManager.penCenterX(2));
        assertEquals(-14, SheepQuestManager.penCenterZ(2));
        assertEquals(14, SheepQuestManager.penCenterZ(3));
    }

    @Test
    void sourcePenCentroidUsesBlockCentersWithoutDependingOnLoadedWorld() {
        Location center = SheepQuestManager.penCenter(null,
                List.of(new Location(null, 2, 63, 4), new Location(null, 4, 65, 6)), 0);
        assertEquals(3.5D, center.getX());
        assertEquals(64.0D, center.getY());
        assertEquals(5.5D, center.getZ());
    }
}
