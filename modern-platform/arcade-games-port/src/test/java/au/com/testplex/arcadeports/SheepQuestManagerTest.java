package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
        assertEquals(Material.WHITE_WOOL, SheepQuestManager.woolIcon(null).getType());
    }

    @Test
    void missingPlayerOrSessionDoesNotChangeCarryState() {
        SheepQuestManager sheep = new SheepQuestManager();
        SessionState session = new SessionState(ArcadePortGame.SHEEP_QUEST, null, List.of(), false);
        sheep.updateCarryUi(session, null, true);
        sheep.updateCarryUi(null, null, false);
        assertTrue(session.sheepQuestMaxCarried.isEmpty());
    }
}
