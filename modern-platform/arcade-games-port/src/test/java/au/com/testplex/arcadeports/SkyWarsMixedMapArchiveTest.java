package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Validates the full original SkyWars map collection and the marker families consumed by the Mixed FFA runtime. */
final class SkyWarsMixedMapArchiveTest {
    private static final int EXPECTED_MAP_COUNT = 50;
    private static final String SOURCE_FOLDER = "Skywars";

    @Test
    void sourceFolderContainsAllFiftyOriginalSkyWarsArchives() throws Exception {
        Path sourceFolder = findRepositoryRoot().resolve("update/maps/" + SOURCE_FOLDER);
        assertTrue(Files.isDirectory(sourceFolder), "The original SkyWars source-map folder must exist");
        assertEquals(EXPECTED_MAP_COUNT, zipNames(sourceFolder).size(),
                "Keep the complete original SkyWars archive collection");
    }

    @Test
    void packagedPluginContainsExactlyTheOriginalSkyWarsArchives() throws Exception {
        Path sourceFolder = findRepositoryRoot().resolve("update/maps/" + SOURCE_FOLDER);
        var packagedFolder = getClass().getClassLoader().getResource("source-maps/" + SOURCE_FOLDER);
        assertNotNull(packagedFolder,
                "Maven must package the original SkyWars maps under source-maps/Skywars");
        assertEquals(zipNames(sourceFolder), zipNames(Path.of(packagedFolder.toURI())),
                "The packaged JAR must preserve every original map filename and include no substituted sample set");
    }

    @Test
    void everyOriginalMapHasTheSpawnGeneratorAndThreeLootMarkerFamilies() throws Exception {
        Path sourceFolder = findRepositoryRoot().resolve("update/maps/" + SOURCE_FOLDER);
        for (String fileName : zipNames(sourceFolder)) {
            String resource = "source-maps/" + SOURCE_FOLDER + "/" + fileName;
            SourceMapCatalog.MapData map = readMap(resource);

            assertNotNull(map, resource + " must include a parseable WorldConfig.dat");
            assertFalse(map.mapName().isBlank(), resource + " must retain its source map identity");
            List<org.bukkit.Location> starts = map.spawnLocations();
            if (starts.isEmpty()) starts = map.dataLocations("GREEN");
            assertFalse(starts.isEmpty(), resource + " needs authored player island spawn points");
            // RED Guardian points are optional in Reborn. Maps without RED spawn no guardians.
            assertFalse(map.dataLocations("LIME").isEmpty(),
                    resource + " needs the original LIME Throwable TNT generator marker");
            assertFalse(map.dataLocations("BROWN").isEmpty(),
                    resource + " needs Island chest locations");
            assertFalse(map.dataLocations("GRAY").isEmpty(),
                    resource + " needs Connector chest locations");
            assertFalse(map.dataLocations("BLACK").isEmpty(),
                    resource + " needs Middle chest locations");
        }
    }

    private SourceMapCatalog.MapData readMap(String resource) throws Exception {
        InputStream raw = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(raw, "Original SkyWars archive was not packaged: " + resource);
        try (ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().replace('\\', '/').endsWith("WorldConfig.dat")) {
                    String config = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return SourceMapCatalog.parseMapConfig(null, List.of(config.split("\\R", -1)));
                }
                zip.closeEntry();
            }
        }
        return null;
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
            if (Files.isDirectory(candidate.resolve("update/maps/" + SOURCE_FOLDER))) return candidate;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate the repository update/maps/Skywars folder");
    }
}
