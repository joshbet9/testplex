package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SourceMapCatalogMonsterMazeWorldTest {
    private static final String SOURCE_REVISION = "0123456789abcdef0123456789abcdef01234567";

    @Test
    void acceptsOnlyProvisionedFlyWorldLinksIntoThePackagedMapRoot(@TempDir Path temp) throws IOException {
        Path container = Files.createDirectories(temp.resolve("server"));
        Path assetRoot = Files.createDirectories(temp.resolve("image/MonsterMazeMaps"));
        Path source = createWorld(assetRoot, "mm_void");
        Files.writeString(assetRoot.resolve(".testplex-mm-source-maps"), SOURCE_REVISION);
        Files.createSymbolicLink(container.resolve("mm_void"), source);

        assertEquals(source.toRealPath(),
                SourceMapCatalog.resolveMonsterMazeWorldSource(container, "mm_void", assetRoot));
    }

    @Test
    void rejectsFlyWorldLinksWithMissingOrInvalidProvenance(@TempDir Path temp) throws IOException {
        Path container = Files.createDirectories(temp.resolve("server"));
        Path assetRoot = Files.createDirectories(temp.resolve("image/MonsterMazeMaps"));
        Path source = createWorld(assetRoot, "mm_void");
        Files.createSymbolicLink(container.resolve("mm_void"), source);

        assertNull(SourceMapCatalog.resolveMonsterMazeWorldSource(container, "mm_void", assetRoot));
        Files.writeString(assetRoot.resolve(".testplex-mm-source-maps"), "not-a-revision");
        assertNull(SourceMapCatalog.resolveMonsterMazeWorldSource(container, "mm_void", assetRoot));
    }

    @Test
    void rejectsLinksOutsideTheConfiguredMapRootAndUnsafeWorldNames(@TempDir Path temp) throws IOException {
        Path container = Files.createDirectories(temp.resolve("server"));
        Path assetRoot = Files.createDirectories(temp.resolve("image/MonsterMazeMaps"));
        Path unrelated = Files.createDirectories(temp.resolve("other"));
        Path source = createWorld(unrelated, "mm_void");
        Files.writeString(assetRoot.resolve(".testplex-mm-source-maps"), SOURCE_REVISION);
        Files.createSymbolicLink(container.resolve("mm_void"), source);

        assertNull(SourceMapCatalog.resolveMonsterMazeWorldSource(container, "mm_void", assetRoot));
        assertNull(SourceMapCatalog.resolveMonsterMazeWorldSource(container, "../outside", assetRoot));
        assertNull(SourceMapCatalog.resolveMonsterMazeWorldSource(container, "unknown", assetRoot));
    }

    @Test
    void keepsHyperVRealWorldDirectoriesWorkingWithoutAnAssetRoot(@TempDir Path temp) throws IOException {
        Path container = Files.createDirectories(temp.resolve("server"));
        Path source = createWorld(container, "mm_siberian");

        assertEquals(source.toRealPath(),
                SourceMapCatalog.resolveMonsterMazeWorldSource(container, "mm_siberian", null));
    }

    private static Path createWorld(Path root, String name) throws IOException {
        Path world = Files.createDirectories(root.resolve(name));
        Files.write(world.resolve("level.dat"), new byte[] {1});
        Path region = Files.createDirectories(world.resolve("region"));
        Files.write(region.resolve("r.0.0.mca"), new byte[8192]);
        return world;
    }
}
