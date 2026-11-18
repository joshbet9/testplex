package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadeGameDirectoryTest {
    @Test
    void exposesAllThirtyUniqueArcadeGamesInAlphabeticalOrder() {
        List<ArcadeGameDirectory.Game> games = ArcadeGameDirectory.games();
        assertEquals(30, games.size());
        assertEquals(30, games.stream().map(ArcadeGameDirectory.Game::id).collect(Collectors.toSet()).size());
        List<String> names = games.stream().map(ArcadeGameDirectory.Game::name).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), names);
    }

    @Test
    void monsterMazeModesShareOnePlayerFacingEntry() {
        assertEquals(1, ArcadeGameDirectory.games().stream()
                .filter(game -> game.id().startsWith("monster-maze")).count());
        assertTrue(ArcadeGameDirectory.find("monster-maze") != null);
        assertTrue(ArcadeGameDirectory.find("monster-maze-speed") == null);
        assertTrue(ArcadeGameDirectory.find("monster-maze-modern") == null);
    }

    @Test
    void mapEditorHasTheSameRealIconsAsThePlayerFacingGameDirectory() {
        // The Map Editor uses the directory entries, not ArcadePortGame.floor()
        // (that value is a build-material choice rather than a game icon).
        for (ArcadeGameDirectory.Game game : ArcadeGameDirectory.games()) {
            assertTrue(ArcadePortGame.fromId(game.id()) != null, game.id());
            assertTrue(game.icon().isItem(), game.id());
            assertTrue(game.icon() != Material.BARRIER, game.id());
        }
        assertEquals(Material.DRAGON_EGG, ArcadeGameDirectory.find("dragon-escape").icon());
        assertEquals(Material.IRON_SWORD, ArcadeGameDirectory.find("gladiators").icon());
        assertEquals(Material.ENDER_PEARL, ArcadeGameDirectory.find("paintball").icon());
        assertEquals(Material.SPYGLASS, ArcadeGameDirectory.find("camera-shy").icon());
        assertEquals(Material.WITHER_SKELETON_SKULL, ArcadeGameDirectory.find("wither-assault").icon());
    }

    @Test
    void includesStandaloneAndPendingArcadeGames() {
        for (String id : List.of("snake", "speed-builders", "hoops-remastered", "wizards")) {
            assertTrue(ArcadeGameDirectory.find(id) != null, id);
        }
    }
}
