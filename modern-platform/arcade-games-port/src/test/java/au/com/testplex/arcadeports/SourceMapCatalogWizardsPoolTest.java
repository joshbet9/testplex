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
    void locatesAWorldDownloaderSnapshotWithoutWorldConfigDat(@TempDir Path extracted) throws IOException {
        Path world = Files.createDirectories(extracted.resolve("downloaded-world"));
        Files.write(world.resolve("level.dat"), new byte[] {1});
        Path region = Files.createDirectories(world.resolve("region"));
        Files.write(region.resolve("r.0.0.mca"), new byte[8192]);

        assertEquals(world.toAbsolutePath().normalize(), SourceMapCatalog.locateWizardsMapRoot(extracted));
    }

    @Test
    void rejectsAmbiguousWizardsWorldRoots(@TempDir Path extracted) throws IOException {
        for (String name : List.of("first", "second")) {
            Path world = Files.createDirectories(extracted.resolve(name));
            Files.write(world.resolve("level.dat"), new byte[] {1});
            Path region = Files.createDirectories(world.resolve("region"));
            Files.write(region.resolve("r.0.0.mca"), new byte[8192]);
        }

        assertNull(SourceMapCatalog.locateWizardsMapRoot(extracted));
    }

    @Test
    void derivesExactNegativeAndPositiveChunkBoundsFromAnvilLocationTable() {
        byte[] header = new byte[8192];
        setChunkLocation(header, 0, 2, 1);
        setChunkLocation(header, 33, 3, 2);

        SourceMapCatalog.ChunkBounds bounds =
                SourceMapCatalog.chunkBoundsFromRegionHeader(1, -2, header);

        assertEquals(new SourceMapCatalog.ChunkBounds(32, 33, -64, -63, 2), bounds);
    }

    @Test
    void ignoresEmptyAnvilRegionHeaders() {
        assertNull(SourceMapCatalog.chunkBoundsFromRegionHeader(0, 0, new byte[8192]));
        assertNull(SourceMapCatalog.chunkBoundsFromRegionHeader(0, 0, new byte[100]));
    }

    private static void setChunkLocation(byte[] header, int slot, int sectorOffset, int sectorCount) {
        int index = slot * 4;
        header[index] = (byte) ((sectorOffset >>> 16) & 0xff);
        header[index + 1] = (byte) ((sectorOffset >>> 8) & 0xff);
        header[index + 2] = (byte) (sectorOffset & 0xff);
        header[index + 3] = (byte) (sectorCount & 0xff);
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
