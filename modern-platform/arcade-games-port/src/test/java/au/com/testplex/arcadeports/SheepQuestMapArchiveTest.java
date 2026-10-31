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

/** Verifies the original Sheep Quest archive survives Maven resource packaging unchanged. */
final class SheepQuestMapArchiveTest {
    private static final Set<String> EXPECTED_ARCHIVES = Set.of("Sheep_BiomeSplit.zip");

    @Test
    void sourceFolderContainsTheExactOriginalArchive() throws Exception {
        Path sourceFolder = findRepositoryRoot().resolve("update/maps/Sheep Quest");
        assertTrue(Files.isDirectory(sourceFolder), "The original Sheep Quest source map folder must exist");
        assertEquals(EXPECTED_ARCHIVES, zipNames(sourceFolder),
                "Preserve the original Sheep Quest archive filename");
    }

    @Test
    void packagedPluginContainsTheSameOriginalArchive() throws Exception {
        URL packagedFolder = getClass().getClassLoader().getResource("source-maps/Sheep Quest");
        assertNotNull(packagedFolder,
                "Maven must package Sheep Quest archives under source-maps/Sheep Quest");
        assertEquals(EXPECTED_ARCHIVES, zipNames(Path.of(packagedFolder.toURI())),
                "The plugin must include the exact source archive, not only a convenience bundle");
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
            if (Files.isDirectory(candidate.resolve("update/maps/Sheep Quest"))) return candidate;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate the repository update/maps/Sheep Quest folder");
    }
}
