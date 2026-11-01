package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A Castle Assault round is not safe to start with only a sampled map subset:
 * every selectable legacy map must retain the objective markers consumed by
 * CastleAssaultHeroesOfGwenRuntime.
 */
final class CastleAssaultMapArchiveTest {
    private static final List<String> ORIGINAL_MAPS = List.of(
            "CastleAssault_Autumnal.zip",
            "CastleAssault_Block Fortress.zip",
            "CastleAssault_Castle Siege.zip",
            "CastleAssault_Desolate Valley.zip",
            "CastleAssault_Marley's Castle.zip",
            "CastleAssault_Navajo.zip",
            "CastleAssault_Regrowth.zip",
            "CastleAssault_Subaqueous.zip",
            "CastleAssault_Tailfeathers.zip",
            "CastleAssault_Temple Of Anubis.zip",
            "CastleAssault_Winter Keep.zip"
    );

    @Test
    void everyOriginalMapRetainsItsKingCrystalAndObjectiveTntMarkers() throws Exception {
        assertEquals(11, ORIGINAL_MAPS.size(), "The complete source map set is part of this gate");

        for (String fileName : ORIGINAL_MAPS) {
            String resource = "source-maps/Castle Assault/" + fileName;
            SourceMapCatalog.MapData map = readMap(resource);

            assertNotNull(map, resource + " must include a readable WorldConfig.dat");
            assertFalse(map.mapName().isBlank(), resource + " must retain its source map identity");
            assertFalse(map.dataLocations("RED").isEmpty(), resource + " needs the Red King marker");
            assertFalse(map.dataLocations("BLUE").isEmpty(), resource + " needs the Blue King marker");
            assertTrue(map.dataLocations("PINK").size() >= 2,
                    resource + " needs two Red sentry crystal markers");
            assertTrue(map.dataLocations("LIGHT_BLUE").size() >= 2,
                    resource + " needs two Blue sentry crystal markers");
            assertFalse(map.dataLocations("BLACK").isEmpty(),
                    resource + " needs at least one objective TNT spawn");
        }
    }

    private SourceMapCatalog.MapData readMap(String resource) throws Exception {
        InputStream raw = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(raw, "The original archive must be packaged: " + resource);

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
}
