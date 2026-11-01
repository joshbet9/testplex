package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/** Source-archive integrity, independent of a live Paper world migration. */
final class GladiatorsMapArchiveTest {
    @Test
    void originalCagedInHasRequiredWorldData() throws Exception {
        InputStream archive = getClass().getClassLoader().getResourceAsStream(
                "source-maps/Gladiators/Gladiators_Caged_In.zip");
        assertNotNull(archive, "The original Caged In archive must be packaged");
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(archive)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) names.add(entry.getName().replace('\\', '/'));
                zip.closeEntry();
            }
        }
        assertTrue(names.stream().anyMatch(n -> n.equals("level.dat") || n.endsWith("/level.dat")),
                "World archive must contain level.dat");
        assertTrue(names.stream().anyMatch(n -> n.equals("WorldConfig.dat") || n.endsWith("/WorldConfig.dat")),
                "World archive must contain source WorldConfig.dat");
        assertTrue(names.stream().anyMatch(n -> n.matches("(^|.*/)region/r\\.-?\\d+\\.-?\\d+\\.mca")),
                "World archive must include .mca terrain region files");
    }
}
