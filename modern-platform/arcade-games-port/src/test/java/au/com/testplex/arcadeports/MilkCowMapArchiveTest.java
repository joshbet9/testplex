package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies BobsFarm retains the source marker groups consumed by Milk the Cow. */
final class MilkCowMapArchiveTest {
    @Test
    void originalBobsFarmArchiveContainsTheRuntimeAnimalSpawnGroups() throws Exception {
        String resource = "source-maps/Milk the Cow/Milk_BobsFarm.zip";
        InputStream raw = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(raw, "The original Milk the Cow BobsFarm archive must be packaged");

        SourceMapCatalog.MapData map = null;
        try (ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().replace('\\', '/').endsWith("WorldConfig.dat")) {
                    String config = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    map = SourceMapCatalog.parseMapConfig(null, List.of(config.split("\\R", -1)));
                    break;
                }
                zip.closeEntry();
            }
        }

        assertNotNull(map, "BobsFarm must retain its WorldConfig.dat");
        assertFalse(map.mapName().isBlank(), "The source map identity must survive packaging");
        assertFalse(map.dataLocations("WHITE").isEmpty(), "The map must provide its chicken markers");
        assertFalse(map.dataLocations("PINK").isEmpty(), "The map must provide its pig markers");
        assertFalse(map.dataLocations("PURPLE").isEmpty(), "The map must provide its villager markers");
    }
}
