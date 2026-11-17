package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroesOfGwenFeedbackRegressionTest {
    @Test
    void onlyHattorisSneakSelectedNinjaDashActivatesFromCrouch() {
        assertTrue(CastleAssaultHeroesOfGwenRuntime.shouldActivateHattoriDashOnSneak(
                "hattori", "hattori.ninja_dash"));
        assertFalse(CastleAssaultHeroesOfGwenRuntime.shouldActivateHattoriDashOnSneak(
                "hattori", "hattori.shuriken"));
        assertFalse(CastleAssaultHeroesOfGwenRuntime.shouldActivateHattoriDashOnSneak(
                "devon", "hattori.ninja_dash"));
        assertFalse(CastleAssaultHeroesOfGwenRuntime.shouldActivateHattoriDashOnSneak(
                "hattori", null));
    }

    @Test
    void storeItemsAreGroupedIntoTheOriginalRoleShopCategories() {
        assertEquals("Sword", CastleAssaultHeroesOfGwenRuntime.heroesOfGwenStoreCategory(Material.IRON_SWORD));
        assertEquals("Bow", CastleAssaultHeroesOfGwenRuntime.heroesOfGwenStoreCategory(Material.BOW));
        assertEquals("Helmet", CastleAssaultHeroesOfGwenRuntime.heroesOfGwenStoreCategory(Material.IRON_HELMET));
        assertEquals("Chestplate", CastleAssaultHeroesOfGwenRuntime.heroesOfGwenStoreCategory(Material.LEATHER_CHESTPLATE));
        assertEquals("Leggings", CastleAssaultHeroesOfGwenRuntime.heroesOfGwenStoreCategory(Material.CHAINMAIL_LEGGINGS));
        assertEquals("Boots", CastleAssaultHeroesOfGwenRuntime.heroesOfGwenStoreCategory(Material.DIAMOND_BOOTS));
        assertEquals("Consumables", CastleAssaultHeroesOfGwenRuntime.heroesOfGwenStoreCategory(Material.POTION));
    }

    @Test
    void liveScoreboardIncludesBothBasesBeaconsPersonalGoldAndElapsedTime() {
        List<String> lines = HeroesOfGwenScoreboardRules.lines(0, "red towers, wither 18",
                "blue towers, wither 12", List.of("Beacon A: Red", "Beacon B: Blue"), 350, 125_000L);

        assertEquals(14, lines.size());
        assertTrue(lines.get(0).contains("Your Team"));
        assertTrue(lines.get(1).contains("red towers"));
        assertTrue(lines.get(4).contains("blue towers"));
        assertTrue(lines.get(6).contains("Beacons"));
        assertTrue(lines.get(7).contains("Beacon A: Red"));
        assertEquals("§f350", lines.get(10));
        assertEquals("§f02:05", lines.get(13));
    }

    @Test
    void witherReplacesTowerIconsOnlyAfterEveryTowerHasFallen() {
        String towers = HeroesOfGwenScoreboardRules.baseStatus(
                List.of("§a♛ ", "§7♛ ", "§a♛ "), true, true, 37);
        assertTrue(towers.contains("♛"));
        assertFalse(towers.contains("Wither"));

        assertEquals("§5Wither §f37",
                HeroesOfGwenScoreboardRules.baseStatus(List.of("§7♛ ", "§7♛ "), false, true, 37));
        assertEquals("§7Wither down",
                HeroesOfGwenScoreboardRules.baseStatus(List.of("§7♛ "), false, false, 0));
    }

    @Test
    void neutralCaptureBeaconIsInitializedAsGlassBeforeProgressBegins() throws IOException {
        assertEquals(Material.WHITE_STAINED_GLASS, ArcadePortRules.heroesOfGwenCaptureBeaconMaterial(-1));
        Path source = Path.of("src/main/java/au/com/testplex/arcadeports/CastleAssaultHeroesOfGwenRuntime.java");
        if (!Files.isRegularFile(source)) {
            source = Path.of("modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/"
                    + "CastleAssaultHeroesOfGwenRuntime.java");
        }
        String runtime = Files.readString(source);
        assertTrue(runtime.contains("setCapturePointBeacon(point, -1)"));
    }
}
