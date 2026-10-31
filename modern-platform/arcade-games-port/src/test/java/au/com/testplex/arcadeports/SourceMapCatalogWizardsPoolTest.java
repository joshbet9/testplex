package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceMapCatalogWizardsPoolTest {
    private static final List<String> WIZARDS_FOLDERS = List.of("Wizards", "Nano Wizards");

    @Test
    void acceptsArchivesFromTheArcadeWizardsAndNanoWizardsCompatibilityPools() {
        assertTrue(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, "source-maps/Wizards/Wizards_Forest.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, "source-maps/Nano Wizards/NanoWizards_Courtyard.zip"));
    }

    @Test
    void rejectsOtherGamesAndPathsOutsideTheWizardsPools() {
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, "source-maps/Dragon Escape/DragonEscape_Course.zip"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, "maps/wizards.zip"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, "source-maps/Wizards/../Death Tag/DeathTag_Map.zip"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, "source-maps/Wizards/Map.zip/"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, "source-maps\\Wizards\\Map.zip"));
    }

    @Test
    void discoversArchivesFromBothExistingServerMapFolders(@TempDir Path mapRoot) throws IOException {
        Path arcadeFolder = Files.createDirectories(mapRoot.resolve("Wizards"));
        Path nanoFolder = Files.createDirectories(mapRoot.resolve("Nano Wizards"));
        Path unrelatedFolder = Files.createDirectories(mapRoot.resolve("Dragon Escape"));
        Files.write(arcadeFolder.resolve("Wizards_Forest.zip"), new byte[] {1, 2, 3});
        Files.write(nanoFolder.resolve("NanoWizards_Courtyard.zip"), new byte[] {4, 5, 6});
        Files.write(unrelatedFolder.resolve("DragonEscape_Course.zip"), new byte[] {7, 8, 9});
        Files.write(arcadeFolder.resolve("readme.txt"), new byte[] {10});

        List<String> resources = SourceMapCatalog.externalMapResources(mapRoot, WIZARDS_FOLDERS);

        assertTrue(resources.contains("source-maps/Wizards/Wizards_Forest.zip"));
        assertTrue(resources.contains("source-maps/Nano Wizards/NanoWizards_Courtyard.zip"));
        assertFalse(resources.stream().anyMatch(resource -> resource.contains("Dragon Escape")));
        assertFalse(resources.stream().anyMatch(resource -> resource.endsWith("readme.txt")));
    }

    @Test
    void resolvesOnlyExistingSafeZipPathsInsideTheConfiguredMapRoot(@TempDir Path mapRoot) throws IOException {
        Path folder = Files.createDirectories(mapRoot.resolve("Wizards"));
        Path archive = folder.resolve("Wizards_Forest.zip");
        Files.write(archive, new byte[] {1});

        assertEquals(archive.toAbsolutePath().normalize(), SourceMapCatalog.resolveExternalArchivePath(
                mapRoot, "source-maps/Wizards/Wizards_Forest.zip"));
        assertNull(SourceMapCatalog.resolveExternalArchivePath(
                mapRoot, "source-maps/Wizards/../Death Tag/DeathTag_Map.zip"));
        assertNull(SourceMapCatalog.resolveExternalArchivePath(
                mapRoot, "source-maps/Wizards/missing.zip"));
        assertNull(SourceMapCatalog.resolveExternalArchivePath(
                mapRoot, "source-maps\\Wizards\\Wizards_Forest.zip"));
    }

    @Test
    void rejectsMissingFolderPoolsAndMissingResourceNames() {
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                List.of(), "source-maps/Wizards/Map.zip"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, null));
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, " "));
    }
}
