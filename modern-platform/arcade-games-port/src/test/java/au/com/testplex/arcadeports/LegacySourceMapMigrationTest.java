package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Validate lossless 1.8-to-Paper-26 storage preparation for problem map archives. */
final class LegacySourceMapMigrationTest {
    @TempDir Path temporary;

    @Test void gladiatorsCagedInRetainsOriginalTerrainDuringPreparation() throws Exception {
        assertLossless("source-maps/Gladiators/Gladiators_Caged_In.zip", "gladiators");
    }
    @Test void throughHellRetainsOriginalTerrainDuringPreparation() throws Exception {
        assertLossless("source-maps/Dragon Escape/DragonEscape_Through Hell.zip", "through-hell");
    }
    @Test void stevesSwampRetainsOriginalTerrainDuringPreparation() throws Exception {
        assertLossless("source-maps/Dragon Escape/DragonEscape_Steve's Swamp.zip", "swamp");
    }

    private void assertLossless(String archive, String name) throws Exception {
        Path extracted = temporary.resolve(name);
        Files.createDirectories(extracted);
        InputStream resource = getClass().getClassLoader().getResourceAsStream(archive);
        assertNotNull(resource, "Bundled original map is missing: " + archive);
        try (ZipInputStream zip = new ZipInputStream(resource)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path output = extracted.resolve(entry.getName().replace('\\', '/')).normalize();
                assertTrue(output.startsWith(extracted), "Unsafe original archive entry: " + entry.getName());
                if (entry.isDirectory()) Files.createDirectories(output);
                else {
                    Files.createDirectories(output.getParent());
                    Files.copy(zip, output);
                }
                zip.closeEntry();
            }
        }

        Path world = SourceMapCatalog.locateMapRoot(extracted);
        assertNotNull(world, "No unambiguous original world metadata for " + archive);
        Path sourceRegion = world.resolve("region");
        String extractedPaths;
        try (var stream = Files.walk(extracted)) {
            extractedPaths = stream.map(p -> extracted.relativize(p).toString())
                    .sorted().limit(30).toList().toString();
        }
        assertTrue(Files.isDirectory(sourceRegion), "Original map has no pre-26.1 terrain regions;"
                + " chosenRoot=" + world + ", rootRegion=" + sourceRegion
                + ", extracted=" + extractedPaths);
        List<String> expected;
        try (var files = Files.list(sourceRegion)) {
            expected = files.filter(p -> p.getFileName().toString().endsWith(".mca"))
                    .map(p -> p.getFileName().toString()).sorted().toList();
        }
        assertTrue(!expected.isEmpty(), "Original terrain is empty for " + archive);

        SourceMapCatalog.preparePaper26WorldStorage(world);
        Path target = world.resolve("dimensions/minecraft/overworld/region");
        List<String> actual;
        try (var files = Files.list(target)) {
            actual = files.filter(p -> p.getFileName().toString().endsWith(".mca"))
                    .map(p -> p.getFileName().toString()).sorted().toList();
        }
        assertEquals(expected, actual, "Terrain region files must be preserved exactly");
        assertTrue(Files.isRegularFile(world.resolve("level.dat")));
        assertTrue(Files.isRegularFile(world.resolve("WorldConfig.dat")));
        assertTrue(!Files.exists(sourceRegion), "Do not leave an obsolete region folder");
        // Restart/install retry must be safe for already converted source worlds.
        SourceMapCatalog.preparePaper26WorldStorage(world);
        assertTrue(Files.isDirectory(target), "Second preparation must preserve converted terrain");
    }
}
