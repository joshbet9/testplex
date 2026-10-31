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

/** Verifies the one original Draw My Thing map is retained in the Paper 26.3 plugin. */
final class DrawMyThingMapArchiveTest {
    private static final Set<String> EXPECTED_ARCHIVES = Set.of("Draw_Stadium.zip");

    @Test
    void sourceFolderContainsTheExactOriginalArchive() throws Exception {
        Path sourceFolder = findRepositoryRoot().resolve("update/maps/Draw My Thing");
        assertTrue(Files.isDirectory(sourceFolder), "The original Draw My Thing source map folder must exist");
        assertEquals(EXPECTED_ARCHIVES, zipNames(sourceFolder),
                "Keep the original Draw Stadium archive; do not replace it with a generated canvas");
    }

    @Test
    void packagedPluginContainsTheSameOriginalArchive() throws Exception {
        URL packagedFolder = getClass().getClassLoader().getResource("source-maps/Draw My Thing");
        assertNotNull(packagedFolder,
                "Maven must package Draw My Thing under source-maps/Draw My Thing");
        assertEquals(EXPECTED_ARCHIVES, zipNames(Path.of(packagedFolder.toURI())),
                "The packaged plugin must preserve the original source map filename");
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
            if (Files.isDirectory(candidate.resolve("update/maps/Draw My Thing"))) return candidate;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate update/maps/Draw My Thing");
    }
}
