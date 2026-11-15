package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class EvolutionViewingLocationTest {
    @Test
    void authoredGreenMarkersTakePriorityOverInferredTestLocations() {
        SourceMapCatalog.MapData data = SourceMapCatalog.parseMapConfig(null, List.of(
                "DATA_NAME: GREEN", "DATA_LOCS: 10,64,20",
                "DATA_NAME: INFERRED_EVOLUTION_VIEWING", "DATA_LOCS: 30,70,40"));

        assertEquals(1, data.evolutionViewingLocations().size());
        assertEquals(10.0D, data.evolutionViewingLocations().get(0).getX());
        assertEquals(64.0D, data.evolutionViewingLocations().get(0).getY());
        assertEquals(20.0D, data.evolutionViewingLocations().get(0).getZ());
    }

    @Test
    void terrainOnlyMapsUseExplicitInferredEvolutionViewingLocations() {
        SourceMapCatalog.MapData data = SourceMapCatalog.parseMapConfig(null, List.of(
                "DATA_NAME: INFERRED_EVOLUTION_VIEWING", "DATA_LOCS: 30,70,40"));

        assertEquals(1, data.evolutionViewingLocations().size());
        assertEquals(30.0D, data.evolutionViewingLocations().get(0).getX());
    }

    @Test
    void playerSelectsAStableValidViewingLocationAndReceivesAClone() {
        Location first = new Location(null, 1, 64, 2);
        Location second = new Location(null, 3, 70, 4);
        UUID playerId = UUID.fromString("12345678-1234-5678-9abc-def012345678");

        Location selected = ArcadePortManager.selectEvolutionViewingLocation(List.of(first, second), playerId);

        assertEquals(ArcadePortManager.selectEvolutionViewingLocation(List.of(first, second), playerId), selected);
        assertNotSame(first, selected);
        assertNotSame(second, selected);
        assertNull(ArcadePortManager.selectEvolutionViewingLocation(List.of(), playerId));
        assertNull(ArcadePortManager.selectEvolutionViewingLocation(List.of(first), null));
    }
}
