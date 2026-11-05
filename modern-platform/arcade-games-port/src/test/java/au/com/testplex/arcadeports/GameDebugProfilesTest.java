package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameDebugProfilesTest {
    @TempDir Path directory;

    @AfterEach
    void clearRuntimeOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test
    void profilesPersistPerTesterAndGameAndSupportLifecycleOperations() {
        Path file = directory.resolve("debug-profiles.yml");
        UUID tester = UUID.randomUUID();
        GameDebugProfiles profiles = new GameDebugProfiles(file.toFile(), ignored -> { });

        assertEquals("default", profiles.selected(tester, ArcadePortGame.BACON_BRAWL));
        profiles.setVariable(tester, ArcadePortGame.BACON_BRAWL, "BACON_1_8_HIT_DELAY_MILLIS", 250.0D);
        profiles.setFeature(tester, ArcadePortGame.BACON_BRAWL, "all-kits", true);
        profiles.setFeature(tester, ArcadePortGame.BACON_BRAWL, "achievement-preview-only", true);
        assertEquals("fast-hits", profiles.duplicate(tester, ArcadePortGame.BACON_BRAWL, "Fast Hits"));
        assertEquals(250.0D, profiles.variable(tester, ArcadePortGame.BACON_BRAWL,
                "BACON_1_8_HIT_DELAY_MILLIS"));

        assertEquals("blank", profiles.create(tester, ArcadePortGame.BACON_BRAWL, "Blank"));
        assertNull(profiles.variable(tester, ArcadePortGame.BACON_BRAWL, "BACON_1_8_HIT_DELAY_MILLIS"));
        assertTrue(profiles.select(tester, ArcadePortGame.BACON_BRAWL, "fast-hits"));
        assertEquals("fast-hits-copy", profiles.duplicate(tester, ArcadePortGame.BACON_BRAWL, "Fast Hits Copy"));
        assertTrue(profiles.renameSelected(tester, ArcadePortGame.BACON_BRAWL, "fast-hits-final") != null);
        assertTrue(profiles.delete(tester, ArcadePortGame.BACON_BRAWL, "fast-hits-final"));
        assertEquals("default", profiles.selected(tester, ArcadePortGame.BACON_BRAWL));

        GameDebugProfiles reloaded = new GameDebugProfiles(file.toFile(), ignored -> { });
        assertEquals(250.0D, reloaded.variable(tester, ArcadePortGame.BACON_BRAWL,
                "BACON_1_8_HIT_DELAY_MILLIS"));
        assertEquals(Boolean.TRUE, reloaded.values(tester, ArcadePortGame.BACON_BRAWL).get("all-kits"));
        assertTrue(reloaded.feature(tester, ArcadePortGame.BACON_BRAWL, "achievement-preview-only", false));
        assertNull(reloaded.variable(tester, ArcadePortGame.CAMERA_SHY, "BACON_1_8_HIT_DELAY_MILLIS"));
    }

    @Test
    void resetRestoresSelectedProfileDefaultsWithoutDeletingOtherProfiles() {
        GameDebugProfiles profiles = new GameDebugProfiles(directory.resolve("profiles.yml").toFile(), ignored -> { });
        UUID tester = UUID.randomUUID();
        ArcadePortGame game = ArcadePortGame.SUPER_SPLOOR;
        profiles.setVariable(tester, game, "SUPER_SPLOOR_MAX_INK", 250.0D);
        profiles.create(tester, game, "experimental");
        profiles.setFeature(tester, game, "unlimited-specials", true);

        profiles.resetSelected(tester, game);

        assertNull(profiles.variable(tester, game, "SUPER_SPLOOR_MAX_INK"));
        assertFalse(profiles.hasFeature(tester, game, "unlimited-specials"));
        assertTrue(profiles.profiles(tester, game).contains("experimental"));
        assertFalse(profiles.delete(tester, game, GameDebugProfiles.DEFAULT_PROFILE));
    }

    @Test
    void resetCanRestoreTheActiveRuntimeToIntendedDefaults() {
        GameDebugProfiles profiles = new GameDebugProfiles(directory.resolve("active-reset.yml").toFile(), ignored -> { });
        UUID tester = UUID.randomUUID();
        profiles.setVariable(tester, ArcadePortGame.BACON_BRAWL, "BACON_1_8_HIT_DELAY_MILLIS", 250.0D);
        GameDebugRuntimeOverrides.activate(profiles.values(tester, ArcadePortGame.BACON_BRAWL));
        assertEquals(250L, GameDebugRuntimeOverrides.longValue("BACON_1_8_HIT_DELAY_MILLIS", 400L));

        profiles.resetSelected(tester, ArcadePortGame.BACON_BRAWL);
        GameDebugRuntimeOverrides.activate(profiles.values(tester, ArcadePortGame.BACON_BRAWL));

        assertEquals(400L, GameDebugRuntimeOverrides.longValue("BACON_1_8_HIT_DELAY_MILLIS", 400L));
    }

    @Test
    void testBotRosterSettingsPersistSeparatelyFromNormalGameConfiguration() {
        Path file = directory.resolve("debug-profiles.yml");
        UUID tester = UUID.randomUUID();
        GameDebugProfiles profiles = new GameDebugProfiles(file.toFile(), ignored -> { });

        profiles.setTestBotCount(tester, ArcadePortGame.MICRO_BATTLES, 8);
        profiles.setTestBotKit(tester, ArcadePortGame.MICRO_BATTLES, 2, "dasher");
        profiles.setTestBotTeam(tester, ArcadePortGame.MICRO_BATTLES, 2, 1);
        profiles.setTestBotCosmetic(tester, ArcadePortGame.MICRO_BATTLES, 2, "start-glass", "violet");

        GameDebugProfiles reloaded = new GameDebugProfiles(file.toFile(), ignored -> { });
        assertEquals(8, reloaded.testBotCount(tester, ArcadePortGame.MICRO_BATTLES, 0));
        assertEquals("dasher", reloaded.testBotKit(tester, ArcadePortGame.MICRO_BATTLES, 2, ""));
        assertEquals(1, reloaded.testBotTeam(tester, ArcadePortGame.MICRO_BATTLES, 2, -1));
        assertEquals("violet", reloaded.testBotCosmetic(
                tester, ArcadePortGame.MICRO_BATTLES, 2, "start-glass", ""));
        assertEquals("", reloaded.testBotKit(tester, ArcadePortGame.BACON_BRAWL, 2, ""));

        reloaded.create(tester, ArcadePortGame.MICRO_BATTLES, "balanced");
        assertEquals(0, reloaded.testBotCount(tester, ArcadePortGame.MICRO_BATTLES, 0));
        reloaded.setTestBotCount(tester, ArcadePortGame.MICRO_BATTLES, 4);
        reloaded.setTestBotKit(tester, ArcadePortGame.MICRO_BATTLES, 3, "dasher");
        reloaded.setTestBotTeam(tester, ArcadePortGame.MICRO_BATTLES, 3, 1);
        reloaded.setTestBotCosmetic(tester, ArcadePortGame.MICRO_BATTLES, 3, "start-glass", "violet");
        assertTrue(reloaded.select(tester, ArcadePortGame.MICRO_BATTLES, "default"));
        assertEquals(8, reloaded.testBotCount(tester, ArcadePortGame.MICRO_BATTLES, 0));
        assertTrue(reloaded.select(tester, ArcadePortGame.MICRO_BATTLES, "balanced"));
        assertEquals("balanced-copy", reloaded.duplicate(tester, ArcadePortGame.MICRO_BATTLES, "Balanced Copy"));
        assertEquals(4, reloaded.testBotCount(tester, ArcadePortGame.MICRO_BATTLES, 0));
        assertEquals("dasher", reloaded.testBotKit(tester, ArcadePortGame.MICRO_BATTLES, 3, ""));
        assertEquals(1, reloaded.testBotTeam(tester, ArcadePortGame.MICRO_BATTLES, 3, -1));
        assertEquals("violet", reloaded.testBotCosmetic(
                tester, ArcadePortGame.MICRO_BATTLES, 3, "start-glass", ""));
        assertTrue(reloaded.select(tester, ArcadePortGame.MICRO_BATTLES, "default"));
        assertEquals(8, reloaded.testBotCount(tester, ArcadePortGame.MICRO_BATTLES, 0));
        assertTrue(reloaded.select(tester, ArcadePortGame.MICRO_BATTLES, "balanced"));
        assertEquals(4, reloaded.testBotCount(tester, ArcadePortGame.MICRO_BATTLES, 0));
    }

    @Test
    void testBotRosterProfilesAreIsolatedForEveryGame() {
        GameDebugProfiles profiles = new GameDebugProfiles(directory.resolve("every-game-rosters.yml").toFile(),
                ignored -> { });
        UUID tester = UUID.randomUUID();

        for (ArcadePortGame game : ArcadePortGame.values()) {
            profiles.setTestBotCount(tester, game, 2);
            profiles.setTestBotKit(tester, game, 1, game.id() + "-kit");
            profiles.setTestBotTeam(tester, game, 1, 1);
            profiles.setTestBotCosmetic(tester, game, 1, "game-line", "game-style");

            assertEquals(2, profiles.testBotCount(tester, game, 0), game.id() + " bot count");
            assertEquals(game.id() + "-kit", profiles.testBotKit(tester, game, 1, ""), game.id() + " bot kit");
            assertEquals(1, profiles.testBotTeam(tester, game, 1, -1), game.id() + " bot team");
            assertEquals("game-style", profiles.testBotCosmetic(tester, game, 1, "game-line", ""),
                    game.id() + " bot cosmetic");

            assertEquals("alternate", profiles.create(tester, game, "alternate"));
            assertEquals(0, profiles.testBotCount(tester, game, 0), game.id() + " profile isolation");
            assertTrue(profiles.select(tester, game, GameDebugProfiles.DEFAULT_PROFILE));
            assertEquals(2, profiles.testBotCount(tester, game, 0), game.id() + " default roster");
        }
    }

    @Test
    void commonFeatureSwitchesPersistAndResetIndependentlyForEveryGame() {
        GameDebugProfiles profiles = new GameDebugProfiles(directory.resolve("all-game-features.yml").toFile(),
                ignored -> { });
        UUID tester = UUID.randomUUID();

        for (ArcadePortGame game : ArcadePortGame.values()) {
            profiles.setFeature(tester, game, "no-fall-damage", true);
            assertTrue(profiles.feature(tester, game, "no-fall-damage", false), game.id());
            profiles.create(tester, game, "fresh");
            assertFalse(profiles.feature(tester, game, "no-fall-damage", false), game.id() + " profile isolation");
            profiles.resetSelected(tester, game);
            assertFalse(profiles.hasFeature(tester, game, "no-fall-damage"), game.id() + " reset");
            assertTrue(profiles.select(tester, game, GameDebugProfiles.DEFAULT_PROFILE));
            assertTrue(profiles.feature(tester, game, "no-fall-damage", false), game.id() + " default retained");
        }
    }

    @Test
    void numericValuesAreRestrictedToKnownGameRulesAndSafeRanges() {
        GameDebugProfiles profiles = new GameDebugProfiles(directory.resolve("bounded.yml").toFile(), ignored -> { });
        UUID tester = UUID.randomUUID();

        profiles.setVariable(tester, ArcadePortGame.BACON_BRAWL, "BACON_1_8_HIT_DELAY_MILLIS", -500.0D);
        profiles.setVariable(tester, ArcadePortGame.BACON_BRAWL, "UNRELATED_SERVER_SETTING", 5.0D);
        profiles.setVariable(tester, ArcadePortGame.BACON_BRAWL, "BACON_BRAWL_HAS_ROUND_TIMER", 1.0D);

        assertEquals(0.0D, profiles.variable(tester, ArcadePortGame.BACON_BRAWL,
                "BACON_1_8_HIT_DELAY_MILLIS"));
        assertNull(profiles.variable(tester, ArcadePortGame.BACON_BRAWL, "UNRELATED_SERVER_SETTING"));
        assertNull(profiles.variable(tester, ArcadePortGame.BACON_BRAWL, "BACON_BRAWL_HAS_ROUND_TIMER"));
        assertFalse(profiles.values(tester, ArcadePortGame.BACON_BRAWL).containsKey("UNRELATED_SERVER_SETTING"));

        profiles.setVariable(tester, ArcadePortGame.SNAKE, "SNAKE_MAX_HAZARDS", 1_000_000.0D);
        assertEquals(64.0D, profiles.variable(tester, ArcadePortGame.SNAKE, "SNAKE_MAX_HAZARDS"));
    }
}
