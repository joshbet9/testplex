package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

class SuperSploorMapArchiveTest {
    @Test void bundledExcavationMapHasBoundsAndEightAuthoredSpawnsPerTeam() throws Exception {
        String resource = "source-maps/Sploor/NanoGames_Excavation.zip";
        InputStream raw = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(raw, "The original 1.8 Excavation map must be packaged at " + resource);
        SourceMapCatalog.MapData map = null;
        try (ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith("WorldConfig.dat")) {
                    String config = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    map = SourceMapCatalog.parseMapConfig(null, List.of(config.split("\\R", -1)));
                    break;
                }
                zip.closeEntry();
            }
        }
        assertNotNull(map, "Excavation archive must include its Reborn WorldConfig.dat");
        assertTrue(map.hasBounds(), "Excavation needs explicit X/Z bounds for territory scoring");
        int redSpawns = map.teamSpawnLocationsForTeam(0, 2).size();
        int blueSpawns = map.teamSpawnLocationsForTeam(1, 2).size();
        assertTrue(redSpawns >= 8, "Red needs eight authored 8v8 spawn markers; found " + redSpawns);
        assertTrue(blueSpawns >= 8, "Blue needs eight authored 8v8 spawn markers; found " + blueSpawns);
    }
}