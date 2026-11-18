package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceMapPublishedOverrideTest {
    @Test
    void resolvesOnlyExistingZipFilesWithinThePluginPublishedMapFolder(@TempDir Path pluginData) throws IOException {
        Path archive = pluginData.resolve("map-editor/published/zodiac/repaired.zip");
        Files.createDirectories(archive.getParent());
        Files.write(archive, new byte[] {1, 2, 3});

        assertEquals(archive.toAbsolutePath().normalize(), SourceMapCatalog.publishedOverridePath(
                pluginData, "map-editor/published/zodiac/repaired.zip"));
        assertNull(SourceMapCatalog.publishedOverridePath(pluginData, "map-editor/published/../../outside.zip"));
        assertNull(SourceMapCatalog.publishedOverridePath(pluginData, "map-editor/exports/zodiac.zip"));
        assertNull(SourceMapCatalog.publishedOverridePath(pluginData, "C:/outside.zip"));
        assertNull(SourceMapCatalog.publishedOverridePath(pluginData, "map-editor/published/missing.zip"));
    }

    @Test
    void refusesSymbolicLinkEscapes(@TempDir Path pluginData, @TempDir Path outside) throws IOException {
        Path published = Files.createDirectories(pluginData.resolve("map-editor/published"));
        Path outsideZip = outside.resolve("outside.zip");
        Files.write(outsideZip, new byte[] {1});
        try {
            Files.createSymbolicLink(published.resolve("escape.zip"), outsideZip);
        } catch (UnsupportedOperationException | SecurityException | IOException exception) {
            return;
        }
        assertNull(SourceMapCatalog.publishedOverridePath(pluginData, "map-editor/published/escape.zip"));
    }

    @Test
    void encodesMapResourcesIntoDistinctSafeConfigurationKeys() {
        String first = SourceMapCatalog.publishedOverrideConfigKey(
                "source-maps/Dragon Escape/DragonEscape_Zodiac.zip");
        String second = SourceMapCatalog.publishedOverrideConfigKey(
                "source-maps/Dragon Escape/DragonEscape_Through_Hell.zip");
        assertTrue(first.startsWith("map-editor.published.current."));
        assertTrue(first.matches("[A-Za-z0-9._-]+"));
        assertNotEquals(first, second);
        assertNull(SourceMapCatalog.publishedOverrideConfigKey(" "));
    }
}
