package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurfWarsTerritoryGeometryTest {
    private static SessionState session() {
        return new SessionState(ArcadePortGame.TURF_WARS, null, List.of(), false);
    }

    @Test void fallbackFrontlineBoundsKeepTheOriginalFortyBlockArena() {
        SessionState match = session();
        assertEquals(-20, match.turfFrontlineMinX);
        assertEquals(20, match.turfFrontlineMaxXExclusive);
        match.turfFrontlineX = -1;
        assertEquals(20, TurfWarsTerritoryGeometry.turfWarsRemainingLines(match, 0));
        assertEquals(19, TurfWarsTerritoryGeometry.turfWarsRemainingLines(match, 1));
        assertEquals(Integer.MAX_VALUE, TurfWarsTerritoryGeometry.turfWarsRemainingLines(match, 2));
    }

    @Test void authoredMarkerDistancesUseXUnlessXFrontsCoincide() {
        SessionState match = session();
        match.turfMarkerGeometry = true;
        match.turfRedFront = new Location(null, 12, 66, -1);
        match.turfRedBase = new Location(null, 5, 66, -12);
        match.turfBlueFront = new Location(null, -8, 66, 14);
        match.turfBlueBase = new Location(null, -4, 66, 6);
        match.turfAxisX = 1;
        assertEquals(7, TurfWarsTerritoryGeometry.turfWarsRemainingLines(match, 0));
        assertEquals(4, TurfWarsTerritoryGeometry.turfWarsRemainingLines(match, 1));
        match.turfAxisX = 0;
        match.turfAxisZ = 1;
        assertEquals(11, TurfWarsTerritoryGeometry.turfWarsRemainingLines(match, 0));
        assertEquals(8, TurfWarsTerritoryGeometry.turfWarsRemainingLines(match, 1));
    }

    @Test void retainsWoolHintsAndOnlyRedOrLightBlueTerracottaAsOwnedTurf() {
        assertTrue(TurfWarsTerritoryGeometry.isTurfWarsWool(Material.RED_WOOL));
        assertFalse(TurfWarsTerritoryGeometry.isTurfWarsWool(Material.RED_TERRACOTTA));
        assertTrue(TurfWarsTerritoryGeometry.isTurfWarsTerracotta(Material.WHITE_TERRACOTTA));
        assertFalse(TurfWarsTerritoryGeometry.isTurfWarsTerracotta(Material.WHITE_WOOL));
        assertEquals(-1, TurfWarsTerritoryGeometry.turfWarsTeamOnBlock(null));
        assertEquals(-1, TurfWarsTerritoryGeometry.turfWarsGroundTeam(null));
        assertEquals(-1, TurfWarsTerritoryGeometry.turfWarsNearestSpawnTeam(session(), null));
    }
}
