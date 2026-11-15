package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

final class SkyWarsChestLootTest {
    @Test
    void unmarkedSourceChestsUseTheNearestAuthoredLootTier() {
        List<Location> spawns = List.of(new Location(null, -80.5D, 64.0D, 0.5D));

        assertEquals(SkyWarsChestLoot.SourceChestTier.ISLAND,
                SkyWarsChestLoot.sourceChestTier(-75.5D, 1.5D, spawns, 0.0D, 0.0D));
        assertEquals(SkyWarsChestLoot.SourceChestTier.MIDDLE,
                SkyWarsChestLoot.sourceChestTier(8.0D, 3.0D, spawns, 0.0D, 0.0D));
        assertEquals(SkyWarsChestLoot.SourceChestTier.CONNECTOR,
                SkyWarsChestLoot.sourceChestTier(48.0D, 0.0D, spawns, 0.0D, 0.0D));
    }
}
