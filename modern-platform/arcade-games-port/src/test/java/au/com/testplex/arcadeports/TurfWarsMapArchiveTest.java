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

/** Verifies every original Turf Wars map archive survives Maven resource packaging. */
final class TurfWarsMapArchiveTest {
    private static final Set<String> EXPECTED_ARCHIVES = Set.of(
            "TF_DoubleForts.zip",
            "TF_FireWater.zip",
            "TF_FrostForts.zip",
            "TurfWars_Agni Kai.zip",
            "TurfWars_Atlantis.zip",
            "TurfWars_Battle Of The Beasts.zip",
            "TurfWars_Battle Of The Heroes.zip",
            "TurfWars_Booksheleves.zip",
            "TurfWars_Candyland.zip",
            "TurfWars_Cathedral.zip",
            "TurfWars_Church.zip",
            "TurfWars_Coastal Cliffs.zip",
            "TurfWars_Cyberspace.zip",
            "TurfWars_Dispiriting War.zip",
            "TurfWars_Domum.zip",
            "TurfWars_Double Forts.zip",
            "TurfWars_Dynasty.zip",
            "TurfWars_Elemental Collision.zip",
            "TurfWars_Fire and Water.zip",
            "TurfWars_First Response.zip",
            "TurfWars_Frost Forts.zip",
            "TurfWars_Gardens Of Arcadia.zip",
            "TurfWars_Hilly.zip",
            "TurfWars_Ice Age.zip",
            "TurfWars_Indiana.zip",
            "TurfWars_Kiasfour.zip",
            "TurfWars_Komorebi.zip",
            "TurfWars_Library.zip",
            "TurfWars_Lush Jungle.zip",
            "TurfWars_Northern Lights.zip",
            "TurfWars_Palace of Harmony.zip",
            "TurfWars_Palace.zip",
            "TurfWars_Pearlescent.zip",
            "TurfWars_Sanctuary Of Ulu.zip",
            "TurfWars_Sinkhole.zip",
            "TurfWars_Stadium.zip",
            "TurfWars_Temple Of The Lost Delta.zip",
            "TurfWars_The Empress.zip",
            "TurfWars_The Greatest Show.zip",
            "TurfWars_Torrid.zip",
            "TurfWars_Vetus.zip",
            "TurfWars_Villa.zip"
    );

    @Test
    void sourceFolderContainsTheExactFortyTwoOriginalArchives() throws Exception {
        Path sourceFolder = findRepositoryRoot().resolve("update/maps/Turf Wars");
        assertTrue(Files.isDirectory(sourceFolder), "The original Turf Wars source map folder must exist");
        assertEquals(EXPECTED_ARCHIVES, zipNames(sourceFolder),
                "Preserve every original filename, including the TF_ aliases and source spelling");
    }

    @Test
    void packagedPluginContainsTheSameFortyTwoArchives() throws Exception {
        URL packagedFolder = getClass().getClassLoader().getResource("source-maps/Turf Wars");
        assertNotNull(packagedFolder,
                "Maven must package Turf Wars archives under source-maps/Turf Wars");
        assertEquals(EXPECTED_ARCHIVES, zipNames(Path.of(packagedFolder.toURI())),
                "The plugin must contain the exact source filename set, not a sampled map list");
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
            if (Files.isDirectory(candidate.resolve("update/maps/Turf Wars"))) return candidate;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate the repository update/maps/Turf Wars folder");
    }
}
