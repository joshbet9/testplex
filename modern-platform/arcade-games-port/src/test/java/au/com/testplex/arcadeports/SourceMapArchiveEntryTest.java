package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class SourceMapArchiveEntryTest {
    private static final Path STAGING = Path.of("test-world.installing").toAbsolutePath().normalize();

    @Test
    void supportsWindowsArchiveSeparatorsWithoutChangingWorldStructure() throws IOException {
        assertEquals(STAGING.resolve("region/r.0.0.mca"),
                SourceMapCatalog.resolveArchiveEntryPath(STAGING, "region\\r.0.0.mca"));
        assertEquals(STAGING.resolve("region/r.-1.0.mca"),
                SourceMapCatalog.resolveArchiveEntryPath(STAGING, "region/r.-1.0.mca"));
        assertEquals(STAGING.resolve("WorldConfig.dat"),
                SourceMapCatalog.resolveArchiveEntryPath(STAGING, "WorldConfig.dat"));
    }

    @Test
    void rejectsTraversalAndAbsoluteEntriesEvenWithBackslashes() {
        for (String path : new String[]{"../world/level.dat", "..\\world\\level.dat",
                "region/../../world/level.dat", "/tmp/world/level.dat",
                "C:\\world\\level.dat", "\\\\server\\share\\level.dat", "."}) {
            assertThrows(IOException.class, () -> SourceMapCatalog.resolveArchiveEntryPath(STAGING, path),
                    () -> "Unexpectedly accepted unsafe map entry " + path);
        }
    }

    @Test
    void bundledGladiatorsArchiveRegionFilesResolveToAnvilRegionDirectory() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("maps/gladiators.zip")) {
            assertNotNull(resource, "Original Gladiators archive must be bundled");
            int regionFiles = 0;
            try (ZipInputStream zip = new ZipInputStream(resource)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    Path resolved = SourceMapCatalog.resolveArchiveEntryPath(STAGING, entry.getName());
                    if (entry.getName().endsWith(".mca")) {
                        assertEquals("region", resolved.getParent().getFileName().toString());
                        regionFiles++;
                    }
                    zip.closeEntry();
                }
            }
            assertTrue(regionFiles >= 4, "Expected the archived Gladiators Anvil region files");
        }
    }
}
