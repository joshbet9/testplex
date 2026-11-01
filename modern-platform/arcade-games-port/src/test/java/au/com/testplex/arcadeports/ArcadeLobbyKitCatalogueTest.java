package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadeLobbyKitCatalogueTest {
    @Test
    void preservesTheReviewedBombLobbersKitOrder() {
        assertEquals(List.of("jumper", "armorer", "pitcher", "waller"),
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.BOMB_LOBBERS, List.of()).stream()
                        .map(choice -> choice.id).toList());
    }

    @Test
    void baconBrawlKeepsChrisPBaconRemovedFromTheSelectableCatalogue() {
        assertFalse(ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.BACON_BRAWL, List.of()).stream()
                .anyMatch(choice -> choice.id.equals("chris-p-bacon")));
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
    void snapUsesItsNewNameAndKitIdsWhileRetainingItsLegacyGameId() {
        List<ArcadeLobbyKitCatalogue.KitChoice> choices =
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.CAMERA_SHY, List.of());
        assertEquals(List.of("snap-smoke-artist", "snap-decoy", "snap-runner", "snap-spotter", "snap-understudy"),
                choices.stream().map(choice -> choice.id).toList());
        assertEquals("Snap", ArcadePortGame.fromId("snap").displayName());
        assertEquals(ArcadePortGame.CAMERA_SHY, ArcadePortGame.fromId("camera-shy"));
    }

    @Test
    void dragonsCommunityKitsAreAvailableFromTheSharedKitCatalogue() {
        List<ArcadeLobbyKitCatalogue.KitChoice> choices =
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.DRAGONS, List.of());
        assertEquals(List.of("coward", "marksman", "pyrotechnic", "frost", "glider", "tamer"),
                choices.stream().map(choice -> choice.id).toList());
        assertEquals(ArcadeLobbyKitCatalogue.Unlock.FREE, choices.get(0).unlock);
        assertTrue(choices.subList(1, choices.size()).stream()
                .allMatch(choice -> choice.unlock == ArcadeLobbyKitCatalogue.Unlock.GEM));
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
        assertTrue(ArcadeLobbyKitCatalogue.forGame(null, List.of()).isEmpty());
    }

    @Test
    void witherAssaultExposesItsThreeSourceHumanKits() {
        assertEquals(List.of("archer", "medic", "editor"),
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.WITHER_ASSAULT, List.of())
                        .stream().map(choice -> choice.id).toList());
    }
}
