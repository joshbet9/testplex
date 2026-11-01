package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the complete original Dragon Escape map set survives Maven resource packaging. */
final class DragonEscapeMapArchiveTest {
    private static final Set<String> EXPECTED_ARCHIVES = Set.of(
            "DragonEscape_A Builder's Bedroom.zip",
            "DragonEscape_Acropolis.zip",
            "DragonEscape_Ancient Clash.zip",
            "DragonEscape_Birch Falls.zip",
            "DragonEscape_Blue Orchid.zip",
            "DragonEscape_Crystalline Colony 2.0.zip",
            "DragonEscape_Dark Magic Woods.zip",
            "DragonEscape_Darkened Heights.zip",
            "DragonEscape_Fen.zip",
            "DragonEscape_Frost Run.zip",
            "DragonEscape_Fungi Galore.zip",
            "DragonEscape_Hell.zip",
            "DragonEscape_Into the Jungle.zip",
            "DragonEscape_Journey Reborn.zip",
            "DragonEscape_Kamakoa Blossom.zip",
            "DragonEscape_Lotus Crescent.zip",
            "DragonEscape_Maple Treeway.zip",
            "DragonEscape_Mystical Mines.zip",
            "DragonEscape_Pirate Bay.zip",
            "DragonEscape_Poseidon's Realm.zip",
            "DragonEscape_Quiescent Pines.zip",
            "DragonEscape_Sandstorm.zip",
            "DragonEscape_Skylands.zip",
            "DragonEscape_Steve's Swamp.zip",
            "DragonEscape_Terracola Gorge.zip",
            "DragonEscape_Through Heaven.zip",
            "DragonEscape_Through Hell.zip",
            "DragonEscape_Time Travel.zip",
            "DragonEscape_Utopia.zip",
            "DragonEscape_Zodiac.zip"
    );

    @Test
    void sourceFolderContainsTheExactThirtyOriginalArchives() throws Exception {
        Path sourceFolder = findRepositoryRoot().resolve("update/maps/Dragon Escape");
        assertTrue(Files.isDirectory(sourceFolder), "The original Dragon Escape source map folder must exist");
        assertEquals(EXPECTED_ARCHIVES, zipNames(sourceFolder),
                "Preserve every original filename, including spaces, punctuation and legacy naming");
    }

    @Test
    void packagedPluginContainsTheSameThirtyArchives() throws Exception {
        URL packagedFolder = getClass().getClassLoader().getResource("source-maps/Dragon Escape");
        assertNotNull(packagedFolder,
                "Maven must package Dragon Escape archives under source-maps/Dragon Escape");
        assertEquals(EXPECTED_ARCHIVES, zipNames(Path.of(packagedFolder.toURI())),
                "The plugin must contain the exact source filename set; no sampled or normalised map list");
    }

    private static Set<String> zipNames(Path folder) throws Exception {
        try (Stream<Path> paths = Files.list(folder)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Path findRepositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve("update/maps/Dragon Escape"))) return candidate;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate the repository update/maps/Dragon Escape folder");
    }
}
