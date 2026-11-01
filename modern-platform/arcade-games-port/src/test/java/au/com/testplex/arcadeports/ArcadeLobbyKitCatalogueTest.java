package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadeLobbyKitCatalogueTest {
    @Test
    void preservesTheReviewedBombLobbersKitOrder() {
        assertEquals(List.of("jumper", "armorer", "pitcher", "waller"),
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.BOMB_LOBBERS, List.of()).stream()
                        .map(choice -> choice.id).toList());
    }

    @Test
    void hidesOnlyTheConfiguredGameAndKitPair() {
        List<ArcadeLobbyKitCatalogue.KitChoice> monsterMaze =
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.MONSTER_MAZE,
                        List.of("MONSTER-MAZE:JUMPER"));
        assertEquals(List.of("slowball", "body-builder", "repulsor", "maverick"),
                monsterMaze.stream().map(choice -> choice.id).toList());
        assertTrue(ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.DRAGON_ESCAPE,
                List.of("monster-maze:jumper")).stream().anyMatch(choice -> choice.id.equals("jumper")));
    }

    @Test
    void configuredKitOrderMovesKnownEntriesAndKeepsUnlistedCatalogueOrder() {
        assertEquals(List.of("waller", "jumper", "armorer", "pitcher"),
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.BOMB_LOBBERS, List.of(),
                        List.of("waller", "not-a-kit", "jumper")).stream()
                        .map(choice -> choice.id).toList());
    }

    @Test
    void sheepQuestLobbyCatalogueMatchesItsThreeLegacyKitsAndUnlockTiers() {
        List<ArcadeLobbyKitCatalogue.KitChoice> choices =
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.SHEEP_QUEST, List.of());
        assertEquals(List.of("beserker", "archer", "brute"),
                choices.stream().map(choice -> choice.id).toList());
        assertEquals(ArcadeLobbyKitCatalogue.Unlock.FREE, choices.get(0).unlock);
        assertEquals(ArcadeLobbyKitCatalogue.Unlock.GEM, choices.get(1).unlock);
        assertEquals(ArcadeLobbyKitCatalogue.Unlock.GEM, choices.get(2).unlock);
    }

    @Test
    void gamesWithoutLobbyKitStationsExposeNoChoices() {
        assertTrue(ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.MILK_THE_COW, List.of()).isEmpty());
        assertTrue(ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.WITHER_ASSAULT, List.of()).isEmpty());
        assertTrue(ArcadeLobbyKitCatalogue.forGame(null, List.of()).isEmpty());
    }
}
