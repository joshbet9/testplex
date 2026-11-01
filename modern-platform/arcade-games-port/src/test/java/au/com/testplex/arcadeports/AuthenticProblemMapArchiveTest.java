package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/** Verify the actual original map payloads, not merely their names in the menu. */
final class AuthenticProblemMapArchiveTest {
    private record Inspection(List<String> names, List<String> configLines) { }

    private Inspection inspect(String resource) throws Exception {
        InputStream raw = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(raw, "Missing bundled authentic source archive: " + resource);
        List<String> names = new ArrayList<>();
        List<String> config = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (!entry.isDirectory()) {
                    names.add(name);
                    if (name.equals("WorldConfig.dat") || name.endsWith("/WorldConfig.dat")) {
                        ByteArrayOutputStream contents = new ByteArrayOutputStream();
                        zip.transferTo(contents);
                        config.addAll(List.of(contents.toString(StandardCharsets.UTF_8).split("\\R")));
                    }
                }
                zip.closeEntry();
            }
        }
        System.out.println("[map-audit] " + resource + " entries=" + names.size()
                + " worldconfig=" + config.size() + " names=" + names.stream().limit(12).toList());
        System.out.println("[map-audit] " + resource + " metadata=" + config.stream()
                .filter(s -> s.startsWith("MAP_NAME:") || s.startsWith("MIN_Y:")
                        || s.startsWith("TEAM_NAME:") || s.startsWith("DATA_NAME:"))
                .limit(16).toList());
        return new Inspection(names, config);
    }

    private void assertAuthenticWorld(String resource) throws Exception {
        Inspection inspection = inspect(resource);
        assertTrue(inspection.names().stream().anyMatch(n -> n.equals("level.dat") || n.endsWith("/level.dat")),
                resource + " lacks level.dat: " + inspection.names().stream().limit(12).toList());
        assertTrue(!inspection.configLines().isEmpty(),
                resource + " lacks WorldConfig.dat: " + inspection.names().stream().limit(12).toList());
        assertTrue(inspection.names().stream().anyMatch(n -> n.toLowerCase(Locale.ROOT)
                .matches("(^|.*/)region/r\\.-?\\d+\\.-?\\d+\\.mca")),
                resource + " lacks an Anvil terrain region: " + inspection.names().stream().limit(12).toList());
    }

    @Test void gladiatorsCagedInHasCompleteWorldFiles() throws Exception {
        assertAuthenticWorld("source-maps/Gladiators/Gladiators_Caged_In.zip");
    }
    @Test void throughHellHasCompleteWorldFiles() throws Exception {
        assertAuthenticWorld("source-maps/Dragon Escape/DragonEscape_Through Hell.zip");
    }
    @Test void stevesSwampHasCompleteWorldFiles() throws Exception {
        assertAuthenticWorld("source-maps/Dragon Escape/DragonEscape_Steve's Swamp.zip");
    }
}
