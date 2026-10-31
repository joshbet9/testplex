package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void rejectsMissingFolderPoolsAndMissingResourceNames() {
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                List.of(), "source-maps/Wizards/Map.zip"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, null));
        assertFalse(SourceMapCatalog.resourcePathBelongsToFolders(
                WIZARDS_FOLDERS, " "));
    }
}
