package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** Guard against exposing WDL terrain as a playable source map without Reborn markers. */
final class RecoveredSourceMapMetadataGateTest {
    private static Path repoRoot() {
        Path candidate = Path.of("").toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve("update/maps/Gladiators"))) return candidate;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Original map tree was not checked out");
    }

    private static String config(String relativePath) throws IOException {
        try (ZipFile zip = new ZipFile(repoRoot().resolve(relativePath).toFile())) {
            return new String(zip.getInputStream(zip.getEntry("WorldConfig.dat")).readAllBytes(),
                    StandardCharsets.UTF_8);
        }
    }

    @Test
    void configuredCagedInHasACompleteSourceTournamentBracket() throws IOException {
        String caged = config("update/maps/Gladiators/Gladiators_Caged_In.zip");
        assertTrue(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.GLADIATORS, caged));
        assertFalse(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.EVOLUTION, caged));
        assertFalse(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.GLADIATORS,
                caged.replaceAll("(?m)^DATA_NAME:PINK\\RDATA_LOCS:[^\\r\\n]*", "")));
    }

    @Test
    void matchingTwinPeaksNameDoesNotMakeQuiverAnEvolutionMap() throws IOException {
        String quiver = config("update/maps/One in the Quiver/Quiver_Twin Peaks 2.0.zip");
        assertFalse(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.EVOLUTION, quiver));
        assertFalse(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.GLADIATORS, quiver));
    }

    @Test
    void evolutionNeedsSixteenSpawnsAndTheNearbyThreeColourEvolutionStation() {
        StringBuilder config = new StringBuilder("MAP_NAME:Review Test\n"
                + "MIN_X:-150\nMAX_X:150\nMIN_Y:0\nMAX_Y:100\nMIN_Z:-150\nMAX_Z:150\n"
                + "TEAM_NAME:Green\nTEAM_SPAWNS:");
        for (int i = 0; i < 16; i++) config.append(i * 3).append(",64,0:");
        config.append("\nDATA_NAME:RED\nDATA_LOCS:100,60,100:\n")
              .append("DATA_NAME:GREEN\nDATA_LOCS:104,60,100:\n")
              .append("DATA_NAME:YELLOW\nDATA_LOCS:100,60,104:\n");
        assertTrue(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.EVOLUTION, config.toString()));
        assertFalse(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.EVOLUTION,
                config.toString().replace("100,60,104:", "200,60,204:")));
        assertFalse(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.EVOLUTION,
                config.toString().replace("45,64,0:", "")));
        assertFalse(SourceMapCatalog.hasCompleteGameMarkers(ArcadePortGame.EVOLUTION,
                config.toString().replace("MAP_NAME:Review Test", "MAP_NAME:")));
    }
}
