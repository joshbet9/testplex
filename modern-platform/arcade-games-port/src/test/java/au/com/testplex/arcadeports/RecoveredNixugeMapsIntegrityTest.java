package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** The backed-up worlds are authentic assets, not yet configured Reborn game arenas. */
final class RecoveredNixugeMapsIntegrityTest {
    private static Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve("update/maps/Gladiators"))) return candidate;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Cannot locate original 1.8 Testplex map archive tree");
    }

    @Test
    void recoveredWorldsRetainTerrainButAreNotPackagedAsPlayableWithoutWorldConfig() throws Exception {
        Path root = repositoryRoot();
        Path staging = root.resolve("update/map-recovery/nixuge/unconfigured");
        assertEquals(6, validate(staging.resolve("Evolution")));
        assertEquals(22, validate(staging.resolve("Gladiators")));
        Path selectedEvolution = root.resolve("update/maps/Evolution");
        if (Files.exists(selectedEvolution)) {
            try (Stream<Path> zipFiles = Files.list(selectedEvolution)) {
                assertFalse(zipFiles.anyMatch(p -> p.getFileName().toString()
                        .toLowerCase(Locale.ROOT).endsWith(".zip")),
                        "Never expose unconfigured Nixuge worlds as enabled Evolution arenas");
            }
        }
    }

    @Test
    void existingOnePointEightSourcesAreNotDuplicated() {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve(
                "update/maps/One in the Quiver/Quiver_Twin Peaks 2.0.zip")));
        assertTrue(Files.isRegularFile(root.resolve(
                "update/maps/Gladiators/Gladiators_Caged_In.zip")));
        assertFalse(Files.exists(root.resolve(
                "update/map-recovery/nixuge/unconfigured/Gladiators/Gladiators_Caged_In.zip")));
    }

    private static int validate(Path folder) throws IOException {
        assertTrue(Files.isDirectory(folder), "Missing recovered map folder: " + folder);
        List<Path> archives;
        try (Stream<Path> files = Files.list(folder)) {
            archives = files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .toList();
        }
        for (Path archive : archives) {
            try (ZipFile zip = new ZipFile(archive.toFile())) {
                List<String> names = zip.stream()
                        .filter(entry -> !entry.isDirectory())
                        .map(entry -> entry.getName().replace('\\', '/')).toList();
                assertTrue(names.stream().anyMatch(name ->
                        name.equals("level.dat") || name.endsWith("/level.dat")),
                        "Missing original level.dat in " + archive);
                assertTrue(names.stream().anyMatch(name ->
                        name.matches("(^|.*/)region/r\\.-?\\d+\\.-?\\d+\\.mca")),
                        "Missing terrain region in " + archive);
                assertFalse(names.stream().anyMatch(name ->
                        name.equals("WorldConfig.dat") || name.endsWith("/WorldConfig.dat")),
                        "An original WorldConfig.dat has appeared: validate it and promote separately "
                                + archive);
            }
        }
        return archives.size();
    }
}
