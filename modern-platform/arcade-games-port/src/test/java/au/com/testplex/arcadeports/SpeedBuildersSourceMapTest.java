package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class SpeedBuildersSourceMapTest {
    private static final String RESOURCE =
            "/source-maps/Speed Builders/SpeedBuilders_Shroomland_Lake.zip";

    @Test
    void originalMapArchiveContainsTheAuthoredTemplateAndPlayerMarkers() throws Exception {
        try (InputStream resource = getClass().getResourceAsStream(RESOURCE)) {
            assertNotNull(resource, "Maven must package the original Speed Builders map archive.");
            try (ZipInputStream zip = new ZipInputStream(resource)) {
                String worldConfig = null;
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName().replace('\\', '/');
                    if (name.equals("WorldConfig.dat") || name.endsWith("/WorldConfig.dat")) {
                        worldConfig = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                        break;
                    }
                    zip.closeEntry();
                }
                assertNotNull(worldConfig, "The original archive must contain WorldConfig.dat.");
                List<String> lines = worldConfig.lines().toList();
                String dataGroup = "";
                String customGroup = "";
                int yellowMarkers = 0;
                int redMarkers = 0;
                int customTemplates = 0;
                for (String line : lines) {
                    int colon = line.indexOf(':');
                    if (colon <= 0) continue;
                    String key = line.substring(0, colon).trim().toUpperCase(java.util.Locale.ROOT);
                    String value = line.substring(colon + 1).trim();
                    if (key.equals("DATA_NAME")) dataGroup = value.toUpperCase(java.util.Locale.ROOT);
                    else if (key.equals("CUSTOM_NAME")) customGroup = value;
                    else if (key.equals("DATA_LOCS")) {
                        int count = coordinateCount(value);
                        if (dataGroup.equals("YELLOW")) yellowMarkers += count;
                        if (dataGroup.equals("RED")) redMarkers += count;
                    } else if (key.equals("CUSTOM_LOCS") && !value.isBlank()) {
                        customTemplates++;
                    }
                }
                assertTrue(redMarkers >= 1, "The source map needs its central RED display marker.");
                String markerDiagnostics = lines.stream()
                        .filter(line -> line.toUpperCase(java.util.Locale.ROOT).contains("DATA_NAME")
                                || line.toUpperCase(java.util.Locale.ROOT).contains("DATA_LOCS"))
                        .limit(30)
                        .collect(java.util.stream.Collectors.joining(" | "));
                assertTrue(yellowMarkers >= SpeedBuildersRules.MAX_PLAYERS,
                        "The source map needs 16 YELLOW player build plots; parsed=" + yellowMarkers
                                + ", DATA markers: " + markerDiagnostics);
                assertTrue(customTemplates >= 10,
                        "Speed Builders must use the original custom-location build library, not demo templates.");
            }
        }
    }

    private static int coordinateCount(String encoded) {
        int count = 0;
        for (String token : encoded.split(":")) {
            if (token.trim().split(",").length >= 3) count++;
        }
        return count;
    }
}
