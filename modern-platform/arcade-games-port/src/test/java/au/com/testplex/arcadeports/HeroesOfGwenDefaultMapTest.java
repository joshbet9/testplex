package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

final class HeroesOfGwenDefaultMapTest {
    private static final String INFERRED = "source-maps/Heroes of GWEN/HOG_Heroes Valley.zip";
    private static final String AUTHORED = "source-maps/Heroes of GWEN/HOG_Heros_Valley.zip";

    @Test
    void defaultsToAuthoredMapWithoutChangingOtherGamesOrExplicitFallbacks() {
        List<String> gwenMaps = List.of(INFERRED, AUTHORED, "source-maps/Heroes of GWEN/HOG_Tartarus.zip");
        assertEquals(AUTHORED, SourceMapCatalog.defaultResourceFor(ArcadePortGame.HEROES_OF_GWEN, gwenMaps));
        assertEquals(INFERRED, SourceMapCatalog.defaultResourceFor(ArcadePortGame.CASTLE_ASSAULT, gwenMaps));
        assertEquals(INFERRED, SourceMapCatalog.defaultResourceFor(ArcadePortGame.HEROES_OF_GWEN, List.of(INFERRED)));
        assertNull(SourceMapCatalog.defaultResourceFor(ArcadePortGame.HEROES_OF_GWEN, List.of()));
    }

    @Test
    void authoredMapContainsBothTeamObjectiveMarkers() throws Exception {
        InputStream raw = getClass().getClassLoader().getResourceAsStream(AUTHORED);
        assertNotNull(raw, "Original Heroes of Gwen map must be packaged");

        try (ZipInputStream archive = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (entry.getName().replace('\\', '/').endsWith("WorldConfig.dat")) {
                    String config = new String(archive.readAllBytes(), StandardCharsets.UTF_8);
                    SourceMapCatalog.MapData map = SourceMapCatalog.parseMapConfig(null, List.of(config.split("\\R", -1)));
                    assertNotNull(map, "WorldConfig.dat must parse");
                    assertFalse(map.dataLocations("RED").isEmpty(), "Red wither-base marker required");
                    assertFalse(map.dataLocations("BLUE").isEmpty(), "Blue wither-base marker required");
                    return;
                }
                archive.closeEntry();
            }
        }
        fail("WorldConfig.dat is absent from the authored archive");
    }
}
