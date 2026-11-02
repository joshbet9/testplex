package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ArcadePortGameTest {
    @Test void legacyMapCatalogIncludesAllMovedMapFoldersForSupportedGames() {
        assertEquals(java.util.List.of("Bacon Brawl"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.BACON_BRAWL));
        assertEquals(java.util.List.of("Bomb Lobbers"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.BOMB_LOBBERS));
        assertEquals(java.util.List.of("Dragon Escape"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.DRAGON_ESCAPE));
        assertEquals(java.util.List.of("Dragons"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.DRAGONS));
        assertEquals(java.util.List.of("Runner"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.RUNNER));
        assertEquals(java.util.List.of("Paintball", "Super Paintball"),
                SourceMapCatalog.mapFoldersFor(ArcadePortGame.PAINTBALL));
        assertEquals(java.util.List.of("Sploor"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.SUPER_SPLOOR));
        assertEquals(java.util.List.of("Speef", "Super Spleef"),
                SourceMapCatalog.mapFoldersFor(ArcadePortGame.SPLEEF));
        assertEquals(java.util.List.of("Death Tag"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.DEATH_TAG));
        assertEquals(java.util.List.of("One in the Quiver"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.ONE_IN_THE_QUIVER));
        assertEquals(java.util.List.of("Gladiators"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.GLADIATORS));
        assertEquals(java.util.List.of("Micro Battle"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.MICRO_BATTLES));
        assertEquals(java.util.List.of("Turf Wars"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.TURF_WARS));
        assertTrue(SourceMapCatalog.mapFoldersFor(ArcadePortGame.SNEAKY_ASSASSINS).isEmpty(),
                "Sneaky Assassins has no archived map in Mineplex Reborn; do not invent one.");
        assertEquals(java.util.List.of("Castle Assault"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.CASTLE_ASSAULT));
        assertEquals(java.util.List.of("Heroes of GWEN"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.HEROES_OF_GWEN));
        assertTrue(SourceMapCatalog.mapFoldersFor(ArcadePortGame.EVOLUTION).isEmpty(),
                "Evolution has no legacy map archive folder and must not be given an invented one");
        assertEquals(java.util.List.of("Draw My Thing"),
                SourceMapCatalog.mapFoldersFor(ArcadePortGame.DRAW_MY_THING));
        assertTrue(SourceMapCatalog.mapFoldersFor(ArcadePortGame.MASTER_BUILDERS).isEmpty(),
                "Master Builders uses generated plots rather than a fabricated source map");
        assertTrue(SourceMapCatalog.mapFoldersFor(ArcadePortGame.CAMERA_SHY).isEmpty(),
                "Snap uses its dedicated procedural maze, not an invented legacy map archive");
        assertEquals("Snap", ArcadePortGame.CAMERA_SHY.displayName());
        assertEquals(ArcadePortGame.CAMERA_SHY, ArcadePortGame.fromId("snap"));
    }

    @Test void selectedMapArchivePathsAreValidatedAgainstTheirGame() {
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.BACON_BRAWL, "source-maps/Bacon Brawl/BaconBrawl_Castle.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.MILK_THE_COW, "source-maps/Milk the Cow/Milk_BobsFarm.zip"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.WITHER_ASSAULT, "source-maps/Milk the Cow/Milk_BobsFarm.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.PAINTBALL, "source-maps/Super Paintball/SP_Arena.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.RUNNER, "source-maps/Runner/Runner_Clay Plaza.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.RUNNER, "source-maps/Runner/Runner_ClayPlaza.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.DRAGONS, "source-maps/Dragons/Dragons_Salt & Pepper.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.SKY_WARS, "source-maps/Skywars/Skywars_Aquarius.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.BACON_BRAWL, "maps/bacon-brawl.zip"),
                "Pre-map-selector configurations using the legacy default resource remain valid");

        assertFalse(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.SKY_WARS, "source-maps/Bacon Brawl/BaconBrawl_Castle.zip"),
                "SkyWars must not accept an archive from another game's folder");
        assertFalse(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.BACON_BRAWL, "source-maps/Bomb Lobbers/Lobbers_Docks.zip"),
                "A valid archive belonging to another game must not be accepted");
        assertFalse(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.BACON_BRAWL, "source-maps/Bacon Brawl/../../maps/bomb-lobbers.zip"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToGame(
                ArcadePortGame.BACON_BRAWL, "source-maps/Bacon Brawl/not-a-map.txt"));
        assertFalse(SourceMapCatalog.resourcePathBelongsToGame(ArcadePortGame.BACON_BRAWL, null));
    }

    @Test void sourceMapResetTargetsPaper263MigratedDimensionFolder() {
        Path primaryWorld = Path.of(System.getProperty("java.io.tmpdir"), "testplex", "world")
                .toAbsolutePath().normalize();
        Path migrated = SourceMapCatalog.migratedDimensionFolder(primaryWorld, "arcadeport_source_bacon_brawl");
        assertEquals(primaryWorld.resolve("dimensions").resolve("minecraft").resolve("arcadeport_source_bacon_brawl"),
                migrated);
        assertTrue(migrated.startsWith(primaryWorld), "migrated source dimension must stay inside the primary world");
        assertNull(SourceMapCatalog.migratedDimensionFolder(primaryWorld, "world"));
        assertNull(SourceMapCatalog.migratedDimensionFolder(primaryWorld, "../arcadeport_source_bacon_brawl"));
        assertNull(SourceMapCatalog.migratedDimensionFolder(
                primaryWorld.resolveSibling("arcadeport_source_other"), "arcadeport_source_bacon_brawl"));
    }

    @Test void modernLobbyScoreboardMatchesTheOriginalArcadeFieldsAndLifecycleTitle() {
        assertEquals("Waiting for players",
                org.bukkit.ChatColor.stripColor(
                        ModernLobbyScoreboard.scoreboardTitleForStatus("Waiting for players")));
        assertEquals("2/16",
                ModernLobbyScoreboard.formatPlayerCount(2, 16));
        assertEquals("El Muchacho Pigo",
                ModernLobbyScoreboard.formatKitName("el-muchacho-pigo"));
        assertEquals("None",
                ModernLobbyScoreboard.formatKitName("None"));
        assertEquals(0, ArcadePortManager.MANUAL_START_COUNTDOWN_SECONDS,
                "The modern lobby Start Game action should start immediately without a countdown.");
    }

    @Test void modernArcadeLobbyHotbarMatchesLegacyTestplexSandboxItems() {
        assertEquals(4, ModernArcadeLobby.HOTBAR_COSMETICS_SLOT);
        assertEquals(6, ModernArcadeLobby.HOTBAR_GAME_PREFERENCES_SLOT);
        assertEquals(8, ModernArcadeLobby.HOTBAR_GAME_PANEL_SLOT);
    }

    @Test void modernKitPedestalsUseLegacyFourBlockSpacing() {
        assertEquals(4, ModernArcadeLobbyRuntime.kitPedestalCenterZ(3, 0));
        assertEquals(0, ModernArcadeLobbyRuntime.kitPedestalCenterZ(3, 1));
        assertEquals(-4, ModernArcadeLobbyRuntime.kitPedestalCenterZ(3, 2));
        assertEquals(6, ModernArcadeLobbyRuntime.kitPedestalCenterZ(4, 0));
        assertEquals(2, ModernArcadeLobbyRuntime.kitPedestalCenterZ(4, 1));
        assertEquals(-2, ModernArcadeLobbyRuntime.kitPedestalCenterZ(4, 2));
        assertEquals(-6, ModernArcadeLobbyRuntime.kitPedestalCenterZ(4, 3));
        assertThrows(IllegalArgumentException.class, () -> ModernArcadeLobbyRuntime.kitPedestalCenterZ(3, 3));
    }

    @Test void modernArcadeLobbyMatchesTheLegacyTestplexPlaneAndDatapoints() {
        assertEquals("testplex_arcade_lobby_v1", ModernArcadeLobby.WORLD_NAME);
        assertEquals(-50, ModernArcadeLobby.PLANE_MIN);
        assertEquals(50, ModernArcadeLobby.PLANE_MAX_EXCLUSIVE);
        assertEquals(100, ModernArcadeLobby.FLOOR_Y);
        assertTrue(ModernArcadeLobby.isInsidePlane(-50, -50));
        assertTrue(ModernArcadeLobby.isInsidePlane(49, 49));
        assertFalse(ModernArcadeLobby.isInsidePlane(50, 0));
        assertFalse(ModernArcadeLobby.isInsidePlane(0, -51));
        assertEquals(org.bukkit.Material.BEDROCK,
                ModernArcadeLobby.planeLayerMaterial(ModernArcadeLobby.FLOOR_Y - 2));
        assertEquals(org.bukkit.Material.STONE,
                ModernArcadeLobby.planeLayerMaterial(ModernArcadeLobby.FLOOR_Y - 1));
        assertEquals(org.bukkit.Material.GRASS_BLOCK,
                ModernArcadeLobby.planeLayerMaterial(ModernArcadeLobby.FLOOR_Y));
        assertNull(ModernArcadeLobby.planeLayerMaterial(ModernArcadeLobby.FLOOR_Y + 1));
        assertEquals(0.0D, ModernArcadeLobby.SPAWN_X, 0.00001D);
        assertEquals(104.0D, ModernArcadeLobby.SPAWN_Y, 0.00001D);
        assertEquals(0.0D, ModernArcadeLobby.SPAWN_Z, 0.00001D);
        assertEquals(-3.5D, ModernArcadeLobby.MISSIONS_X, 0.00001D);
        assertEquals(44.0D, ModernArcadeLobby.GAME_TEXT_Z, 0.00001D);
        assertEquals(-40.0D, ModernArcadeLobby.KIT_TEXT_X, 0.00001D);
        assertEquals(40.0D, ModernArcadeLobby.TEAM_TEXT_X, 0.00001D);
        assertEquals(-44.0D, ModernArcadeLobby.ADV_TEXT_Z, 0.00001D);
        assertEquals(-17.0D, ModernArcadeLobby.KIT_DISPLAY_X, 0.00001D);
        assertEquals(18.0D, ModernArcadeLobby.TEAM_DISPLAY_X, 0.00001D);
        assertEquals(-15.0D, ModernArcadeLobby.AMP_STAND_Z, 0.00001D);
    }


    @Test void exposesTheMechanicsGamesAndIntegratedModules() {
        assertEquals(29, ArcadePortGame.values().length);
        Set<String> ids = new HashSet<>();
        for (ArcadePortGame game : ArcadePortGame.values()) {
            assertTrue(ids.add(game.id()), "duplicate game id: " + game.id());
            assertNotNull(ArcadePortGame.fromId(game.id()));
            assertNotNull(game.floor());
        }
        assertEquals(ArcadePortGame.MONSTER_MAZE, ArcadePortGame.fromId("monster-maze"));
        assertEquals(ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.fromId("monster-maze-speed"));
        assertEquals(ArcadePortGame.MONSTER_MAZE_MODERN, ArcadePortGame.fromId("monster-maze-modern"));
        assertEquals("Monster Maze (Original)", ArcadePortGame.MONSTER_MAZE.displayName());
        assertEquals("Monster Maze (Speed)", ArcadePortGame.MONSTER_MAZE_SPEED.displayName());
        assertEquals("Monster Maze (Modern)", ArcadePortGame.MONSTER_MAZE_MODERN.displayName());
        assertTrue(ArcadePortGame.MONSTER_MAZE.isMonsterMaze());
        assertTrue(ArcadePortGame.MONSTER_MAZE_SPEED.isMonsterMaze());
        assertTrue(ArcadePortGame.MONSTER_MAZE_MODERN.isMonsterMaze());
        assertEquals(ArcadePortGame.PLEXBALL, ArcadePortGame.fromId("plexball"));
        assertEquals(ArcadePortGame.CAMERA_SHY, ArcadePortGame.fromId("camera-shy"));
        assertEquals(ArcadePortGame.SUPER_SPLOOR, ArcadePortGame.fromId("super-sploor"));
        assertEquals("Plexball", ArcadePortGame.PLEXBALL.displayName());
        java.util.List<String> enabled = java.util.List.of("death-tag", "plexball", "spleef", "super-sploor", "dragons", "runner", "camera-shy");
        assertTrue(ArcadePortRules.isArcadePortGameEnabled(ArcadePortGame.PLEXBALL.id(), enabled),
                "Plexball is promoted into the controlled READY_FOR_TEST queue.");
        assertTrue(ArcadePortRules.isArcadePortGameEnabled(ArcadePortGame.SPLEEF.id(), enabled),
                "Spleef is promoted into the controlled READY_FOR_TEST queue.");
        assertTrue(ArcadePortRules.isArcadePortGameEnabled(ArcadePortGame.DRAGONS.id(), enabled),
                "Dragons is promoted into the controlled READY_FOR_TEST queue.");
        assertTrue(ArcadePortRules.isArcadePortGameEnabled(ArcadePortGame.RUNNER.id(), enabled),
                "Runner is promoted into the controlled READY_FOR_TEST queue.");
        assertTrue(ArcadePortRules.isArcadePortGameEnabled(ArcadePortGame.SUPER_SPLOOR.id(), enabled),
                "Super Sploor is promoted into the controlled READY_FOR_TEST queue.");
        assertTrue(ArcadePortRules.isArcadePortGameEnabled(ArcadePortGame.CAMERA_SHY.id(), enabled),
                "Snap is promoted into the controlled READY_FOR_TEST queue.");
        assertFalse(ArcadePortRules.isArcadePortGameEnabled(ArcadePortGame.CAMERA_SHY.id(),
                java.util.List.of("death-tag", "plexball", "spleef")),
                "Snap must remain gated when it is removed from the reviewed allow-list.");
        assertNull(ArcadePortGame.fromId("snake"));
        assertEquals(ArcadePortGame.DRAW_MY_THING, ArcadePortGame.fromId("Draw My Thing"));
        assertEquals(ArcadePortGame.MASTER_BUILDERS, ArcadePortGame.fromId("master_builders"));
        assertEquals(ArcadePortGame.SNEAKY_ASSASSINS, ArcadePortGame.fromId("sneaky-assassins"));
        assertEquals(ArcadePortGame.SKY_WARS, ArcadePortGame.fromId("skywars"));
        assertEquals(ArcadePortGame.MILK_THE_COW, ArcadePortGame.fromId("milk-the-cow"));
        assertEquals(ArcadePortGame.WITHER_ASSAULT, ArcadePortGame.fromId("wither-assault"));
    }

    @Test void lobbyQueueTeamCountsAreDeclaredByTheSharedGameCatalogue() {
        assertEquals(0, ArcadePortGame.BACON_BRAWL.lobbyTeamCount());
        assertEquals(2, ArcadePortGame.BOMB_LOBBERS.lobbyTeamCount());
        assertEquals(2, ArcadePortGame.PAINTBALL.lobbyTeamCount());
        assertEquals(2, ArcadePortGame.TURF_WARS.lobbyTeamCount());
        assertEquals(2, ArcadePortGame.CASTLE_ASSAULT.lobbyTeamCount());
        assertEquals(2, ArcadePortGame.HEROES_OF_GWEN.lobbyTeamCount());
        assertEquals(4, ArcadePortGame.MICRO_BATTLES.lobbyTeamCount());
        assertEquals(4, ArcadePortGame.SHEEP_QUEST.lobbyTeamCount());
        assertEquals(2, ArcadePortGame.SUPER_SPLOOR.lobbyTeamCount());
        assertEquals(0, ArcadePortGame.PLEXBALL.lobbyTeamCount());
        assertEquals(0, ArcadePortGame.CAMERA_SHY.lobbyTeamCount());
        assertEquals(0, ArcadePortGame.ONE_IN_THE_QUIVER.lobbyTeamCount());
        assertEquals(0, ArcadePortGame.MILK_THE_COW.lobbyTeamCount());
        assertEquals(0, ArcadePortGame.WITHER_ASSAULT.lobbyTeamCount());
        assertTrue(ArcadePortGame.MILK_THE_COW.lobbyRolesAssignedAutomatically());
        assertTrue(ArcadePortGame.WITHER_ASSAULT.lobbyRolesAssignedAutomatically());
        assertFalse(ArcadePortGame.PAINTBALL.lobbyRolesAssignedAutomatically());
        assertFalse(ArcadePortGame.BACON_BRAWL.lobbyRolesAssignedAutomatically());
        assertEquals(ArcadePortGame.MICRO_BATTLES.lobbyTeamCount(),
                ArcadePortManager.teamCountForGame(ArcadePortGame.MICRO_BATTLES));
        assertEquals(ArcadePortGame.BOMB_LOBBERS.lobbyTeamCount(),
                ArcadePortManager.teamCountForGame(ArcadePortGame.BOMB_LOBBERS));
    }

    @Test void lobbyKitCatalogueIdsResolveForActiveAndLobbySelectionPaths() {
        assertEquals("archer", ArcadePortRules.normalizeMicroBattlesKit("micro-archer"));
        assertEquals("worker", ArcadePortRules.normalizeMicroBattlesKit("micro-worker"));
        assertEquals("fighter", ArcadePortRules.normalizeMicroBattlesKit("micro-fighter"));
        assertEquals("snowballer", ArcadePortRules.normalizeSpleefKit("spleef-snowballer"));
        assertEquals("brawler", ArcadePortRules.normalizeSpleefKit("spleef-brawler"));
        assertEquals("archer", ArcadePortRules.normalizeSpleefKit("spleef-archer"));
        assertEquals("basher", ArcadePortRules.normalizeDeathTagRunnerKit("runner-basher"));
        assertEquals("archer", ArcadePortRules.normalizeDeathTagRunnerKit("runner-archer"));
        assertEquals("traitor", ArcadePortRules.normalizeDeathTagRunnerKit("runner-traitor"));
        assertNull(ArcadePortRules.normalizeDeathTagRunnerKit("runner-invalid"));
    }

    @Test void normalizesSpacesAndUnderscoresInTestCommands() {
        assertEquals(ArcadePortGame.ONE_IN_THE_QUIVER, ArcadePortGame.fromId("one_in_the_quiver"));
        assertEquals(ArcadePortGame.BOMB_LOBBERS, ArcadePortGame.fromId("Bomb Lobbers"));
        assertEquals(ArcadePortGame.MICRO_BATTLES, ArcadePortGame.fromId("micro-battles"));
        assertEquals(ArcadePortGame.CASTLE_ASSAULT, ArcadePortGame.fromId("castle-assault"));
        assertEquals(ArcadePortGame.HEROES_OF_GWEN, ArcadePortGame.fromId("Heroes of Gwen"));
    }
    @Test void microBattlesGlassBarrierDropsAfterTenSeconds() {
        assertFalse(ArcadePortRules.shouldRemoveMicroBarrier(9_999L));
        assertTrue(ArcadePortRules.shouldRemoveMicroBarrier(10_000L));
        assertTrue(ArcadePortRules.shouldRemoveMicroBarrier(12_000L));
    }

    @Test void microBattlesBarrierHeightTracksSourceIslandY() {
        assertEquals(65, ArcadePortRules.microBattlesBarrierTopY(64));
        assertEquals(73, ArcadePortRules.microBattlesBarrierTopY(72));
        assertEquals(41, ArcadePortRules.microBattlesBarrierTopY(40));
    }

    @Test void microBattlesBorderChecksRespectItsConfiguredCentre() {
        assertFalse(ArcadePortRules.isOutsideWorldBorder(110.0D, -40.0D, 100.0D, -40.0D, 40.0D));
        assertFalse(ArcadePortRules.isOutsideWorldBorder(120.0D, -60.0D, 100.0D, -40.0D, 40.0D));
        assertTrue(ArcadePortRules.isOutsideWorldBorder(120.01D, -40.0D, 100.0D, -40.0D, 40.0D));
        assertTrue(ArcadePortRules.isOutsideWorldBorder(100.0D, -60.01D, 100.0D, -40.0D, 40.0D));
        assertTrue(ArcadePortRules.isOutsideWorldBorder(Double.NaN, 0.0D, 0.0D, 0.0D, 100.0D));
    }

    @Test void microBattlesBorderCueStartsWithinThreeBlocksAndRespectsOutsideEdge() {
        assertEquals(3.0D, ArcadePortRules.microBattlesDistanceToBorder(7.0D, 0.0D, 0.0D, 0.0D, 20.0D), 0.00001D);
        assertTrue(ArcadePortRules.shouldShowMicroBattlesBorderParticles(7.0D, 0.0D, 0.0D, 0.0D, 20.0D));
        assertTrue(ArcadePortRules.shouldShowMicroBattlesBorderParticles(-9.5D, 0.0D, 0.0D, 0.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldShowMicroBattlesBorderParticles(6.99D, 0.0D, 0.0D, 0.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldShowMicroBattlesBorderParticles(10.01D, 0.0D, 0.0D, 0.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldShowMicroBattlesBorderParticles(Double.NaN, 0.0D, 0.0D, 0.0D, 20.0D));
        assertTrue(Double.isNaN(ArcadePortRules.microBattlesDistanceToBorder(0.0D, 0.0D, 0.0D, 0.0D, 0.0D)));
    }

    @Test void microBattlesPersonalGlassAndSharedBorderColoursResolveTheSameSixteenChoices() {
        for (String colour : java.util.List.of("white", "orange", "magenta", "light-blue", "yellow", "lime",
                "pink", "gray", "light-gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            assertNotNull(ArcadePortRules.microBattlesGlassMaterial(colour), colour);
            assertNotNull(ArcadePortRules.microBattlesParticleColour(colour), colour);
            assertEquals(colour, ArcadePortRules.normalizeMicroBattlesColour(colour));
        }
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS, ArcadePortRules.microBattlesGlassMaterial("unknown"));
        assertEquals("light-blue", ArcadePortRules.normalizeMicroBattlesColour("aqua"));
        assertEquals("light-gray", ArcadePortRules.normalizeMicroBattlesColour("silver"));
        assertNull(ArcadePortRules.normalizeMicroBattlesColour("not-a-colour"));
    }

    @Test void microBattlesTimeoutConsidersAllFourTeamsAndReturnsDraws() {
        assertEquals(2, ArcadePortRules.microBattlesTimeoutTeam(
                new int[] {1, 1, 2, 0}, new int[] {99, 99, 0, 99}));
        assertEquals(3, ArcadePortRules.microBattlesTimeoutTeam(
                new int[] {1, 1, 1, 1}, new int[] {4, 2, 3, 5}));
        assertEquals(1, ArcadePortRules.microBattlesTimeoutTeam(
                new int[] {2, 2, 1, 0}, new int[] {4, 7, 99, 99}));
        assertEquals(-1, ArcadePortRules.microBattlesTimeoutTeam(
                new int[] {1, 1, 0, 0}, new int[] {4, 4, 99, 99}));
        assertEquals(-1, ArcadePortRules.microBattlesTimeoutTeam(
                new int[] {0, 0, 0, 0}, new int[] {0, 0, 0, 0}));
        assertEquals(-1, ArcadePortRules.microBattlesTimeoutTeam(
                new int[] {1, 2}, new int[] {1}));
    }

    @Test void microBattlesAnnihilationMatchesEightKillSourceTracker() {
        assertFalse(ArcadePortRules.shouldAwardMicroBattlesAnnihilation(0));
        assertFalse(ArcadePortRules.shouldAwardMicroBattlesAnnihilation(7));
        assertTrue(ArcadePortRules.shouldAwardMicroBattlesAnnihilation(8));
        assertFalse(ArcadePortRules.shouldAwardMicroBattlesAnnihilation(9));

        java.util.UUID playerId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertEquals("test-achievements.micro-battles." + playerId + ".annihilation",
                MicroBattlesAchievement.ANNIHILATION.configKey(playerId));
        assertEquals("", ArcadePortRules.microBattlesAchievementConfigKey(null, "annihilation"));
        assertEquals("", ArcadePortRules.microBattlesAchievementConfigKey(playerId, "Invalid Slug"));
    }

    @Test void microBattlesLastTwoMissionOnlyTriggersWithExactlyTwoAliveTeams() {
        assertFalse(ArcadePortRules.shouldAwardMicroBattlesLastTwo(0));
        assertFalse(ArcadePortRules.shouldAwardMicroBattlesLastTwo(1));
        assertTrue(ArcadePortRules.shouldAwardMicroBattlesLastTwo(2));
        assertFalse(ArcadePortRules.shouldAwardMicroBattlesLastTwo(3));
        assertFalse(ArcadePortRules.shouldAwardMicroBattlesLastTwo(4));

        java.util.UUID playerId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertEquals("test-achievements.micro-battles." + playerId + ".last-two",
                MicroBattlesAchievement.LAST_TWO.configKey(playerId));
    }

    @Test void microBattlesVerticalMapScanIncludesSourceMaximumY() {
        assertEquals(3.0D, ArcadePortRules.microBattlesDistanceToBorder(7.0D, 0.0D, 0.0D, 0.0D, 20.0D), 0.00001D);
        assertTrue(ArcadePortRules.shouldShowMicroBattlesBorderParticles(7.0D, 0.0D, 0.0D, 0.0D, 20.0D));
        assertTrue(ArcadePortRules.shouldShowMicroBattlesBorderParticles(-9.5D, 0.0D, 0.0D, 0.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldShowMicroBattlesBorderParticles(6.99D, 0.0D, 0.0D, 0.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldShowMicroBattlesBorderParticles(10.01D, 0.0D, 0.0D, 0.0D, 20.0D));
        assertFalse(ArcadePortRules.shouldShowMicroBattlesBorderParticles(Double.NaN, 0.0D, 0.0D, 0.0D, 20.0D));
        assertTrue(Double.isNaN(ArcadePortRules.microBattlesDistanceToBorder(0.0D, 0.0D, 0.0D, 0.0D, 0.0D)));
    }





    @Test void microBattlesCrumbleScanIncludesSourceMaximumY() {
        assertFalse(ArcadePortRules.isMicroCrumbleScanComplete(95, 96));
        assertFalse(ArcadePortRules.isMicroCrumbleScanComplete(96, 96));
        assertTrue(ArcadePortRules.isMicroCrumbleScanComplete(97, 96));
        assertFalse(ArcadePortRules.isMicroCrumbleScanComplete(0, 0));
        assertTrue(ArcadePortRules.isMicroCrumbleScanComplete(1, 0));
        assertTrue(ArcadePortRules.isMicroCrumbleScanComplete(1, 0));
    }

    @Test void microBattlesOpeningRestrictionOnlyPunishesLeavingForAnotherIsland() {
        assertTrue(ArcadePortRules.shouldPunishMicroIslandExit(3_999L, 100.0D, 25.0D));
        assertFalse(ArcadePortRules.shouldPunishMicroIslandExit(4_000L, 100.0D, 25.0D));
        assertFalse(ArcadePortRules.shouldPunishMicroIslandExit(3_999L, 25.0D, 100.0D));
        assertFalse(ArcadePortRules.shouldPunishMicroIslandExit(-1L, 100.0D, 25.0D));
    }

    @Test void worldDragonHazardIsNotConfusedWithPlayerFireball() {
        assertTrue(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, true, false));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, true, true));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, false, false));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.BOMB_LOBBERS, true, false));
    }

    @Test void quiverLeaperOnlyRearmsOnLanding() {
        assertFalse(ArcadePortRules.shouldRearmQuiverLeap(false, true, 0.0D));
        assertFalse(ArcadePortRules.shouldRearmQuiverLeap(true, true, 0.4D));
        assertTrue(ArcadePortRules.shouldRearmQuiverLeap(true, true, 0.0D));
        assertTrue(ArcadePortRules.shouldRearmQuiverLeap(true, true, -0.1D));
        assertTrue(ArcadePortRules.shouldRearmQuiverLeap(true, false, 0.4D));
        assertFalse(ArcadePortRules.shouldRearmQuiverLeap(false, false, 0.0D));
        assertFalse(ArcadePortRules.shouldRearmQuiverLeap(true, true, Double.NaN));
    }

    @Test void quiverRegrantsTheSuperArrowForEveryKillSource() {
        assertTrue(ArcadePortRules.quiverRegrantsArrowAfterKill());
        assertEquals(0, ArcadePortRules.QUIVER_SWORD_SLOT);
        assertEquals(1, ArcadePortRules.QUIVER_BOW_SLOT);
        assertEquals(2, ArcadePortRules.QUIVER_ARROW_SLOT);
    }

    @Test void quiverEnchanterChainsTwiceUsingTheSourceImpactAndIgnoreRules() {
        assertEquals(2, ArcadePortRules.QUIVER_MAX_ARROW_BOUNCES);
        assertTrue(ArcadePortRules.shouldQuiverArrowBounce(2));
        assertTrue(ArcadePortRules.shouldQuiverArrowBounce(1));
        assertFalse(ArcadePortRules.shouldQuiverArrowBounce(0));
        assertEquals(1, ArcadePortRules.quiverArrowBouncesAfterBounce(2));
        assertEquals(0, ArcadePortRules.quiverArrowBouncesAfterBounce(1));
        assertEquals(0, ArcadePortRules.quiverArrowBouncesAfterBounce(0));

        assertTrue(ArcadePortRules.isQuiverReboundImpactInRange(0.0D, 4.0D));
        assertTrue(ArcadePortRules.isQuiverReboundImpactInRange(4.0D, 0.9D));
        assertFalse(ArcadePortRules.isQuiverReboundImpactInRange(1.01D, 1.01D));
        assertFalse(ArcadePortRules.isQuiverReboundImpactInRange(Double.NaN, 2.0D));
        assertEquals(1.0D, ArcadePortRules.QUIVER_REBOUND_MAX_IMPACT_DISTANCE_SQUARED, 0.00001D);
    }

    @Test void quiverSpectatorCompassUsesCooldownOnlyForTeleport() {
        assertTrue(ArcadePortRules.isQuiverSpectatorTeleportReady(1_000L, 1_000L));
        assertFalse(ArcadePortRules.isQuiverSpectatorTeleportReady(999L, 1_000L));
        assertEquals(4_000L, ArcadePortRules.nextQuiverSpectatorTeleportAt(1_000L));
        assertEquals(3_000L, ArcadePortRules.QUIVER_SPECTATOR_TELEPORT_COOLDOWN_MILLIS);
    }

    @Test void quiverOnlyAllowsLegacyStainedGlassAndPanesToBreak() {
        assertTrue(ArcadePortRules.isQuiverBreakableGlassMaterial("WHITE_STAINED_GLASS"));
        assertTrue(ArcadePortRules.isQuiverBreakableGlassMaterial("RED_STAINED_GLASS_PANE"));
        assertTrue(ArcadePortRules.isQuiverBreakableGlassMaterial("BLUE_STAINED_GLASS"));
        assertFalse(ArcadePortRules.isQuiverBreakableGlassMaterial("GLASS"));
        assertFalse(ArcadePortRules.isQuiverBreakableGlassMaterial("GLASS_PANE"));
        assertFalse(ArcadePortRules.isQuiverBreakableGlassMaterial("STONE"));
        assertFalse(ArcadePortRules.isQuiverBreakableGlassMaterial(null));
    }

    @Test void quiverFinishesOnLastLivePlayerImmediatelyAfterKill() {
        assertTrue(ArcadePortRules.shouldFinishQuiverLastPlayer(1, 0, 2));
        assertTrue(ArcadePortRules.shouldFinishQuiverLastPlayer(1, 1, 4));
        assertFalse(ArcadePortRules.shouldFinishQuiverLastPlayer(2, 0, 2));
        assertFalse(ArcadePortRules.shouldFinishQuiverLastPlayer(1, 0, 1));
    }

    @Test void quiverGroundPoundUsesSourceRadialImpulse() {
        assertEquals(1.0D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(0.0D), 0.00001D);
        assertEquals(0.5D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(4.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(8.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(10.0D), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(-1.0D), 0.00001D);
    }

    @Test void bombLobbersTestBotsAreAssignedToTheOpposingTeam() {
        assertEquals(1, ArcadePortRules.opposingBombLobbersTeam(0));
        assertEquals(0, ArcadePortRules.opposingBombLobbersTeam(1));
        assertEquals(-1, ArcadePortRules.opposingBombLobbersTeam(-1));
        assertEquals(-1, ArcadePortRules.opposingBombLobbersTeam(2));
        assertTrue(ArcadePortRules.areBombLobbersEnemies(0, ArcadePortRules.opposingBombLobbersTeam(0)));
        assertTrue(ArcadePortRules.areBombLobbersEnemies(1, ArcadePortRules.opposingBombLobbersTeam(1)));
    }

    @Test void bombLobbersPowerupUsesPairedOneMinuteAverageWaveAndTwoActiveDrops() {
        assertEquals(60_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(1));
        assertEquals(60_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(2));
        assertEquals(60_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(4));
        assertEquals(60_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(8));
        assertEquals(60_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(12));
        assertEquals(60_000L, ArcadePortRules.bombLobbersPowerupSpawnIntervalMillis(16));
        assertEquals(2, ArcadePortRules.bombLobbersPowerupMaxActive(2));
        assertEquals(2, ArcadePortRules.bombLobbersPowerupMaxActive(6));
        assertEquals(2, ArcadePortRules.bombLobbersPowerupMaxActive(10));
        assertEquals(2, ArcadePortRules.bombLobbersPowerupMaxActive(16));
    }

    @Test void bombLobbersSuperAndFieryTntHaveDistinctBlastEffects() {
        assertEquals(8.0D, ArcadePortRules.bombLobbersExplosionRadius("normal"), 0.00001D);
        assertEquals(10.0D, ArcadePortRules.bombLobbersExplosionRadius("super-tnt"), 0.00001D);
        assertEquals(50.0D, ArcadePortRules.bombLobbersExplosionMaxDamage("normal"), 0.00001D);
        assertEquals(65.0D, ArcadePortRules.bombLobbersExplosionMaxDamage("super-tnt"), 0.00001D);
        assertEquals(10.0D, ArcadePortRules.bombLobbersDirectHitDamage("normal"), 0.00001D);
        assertEquals(15.0D, ArcadePortRules.bombLobbersDirectHitDamage("super-tnt"), 0.00001D);
        assertFalse(ArcadePortRules.bombLobbersTntIgnites("normal"));
        assertTrue(ArcadePortRules.bombLobbersTntIgnites("fiery-tnt"));
        assertEquals(65.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 1.0D, 10.0D, 65.0D), 0.00001D);
    }

    @Test void oneInTheQuiverNeedsTwentyKillsToWin() {
        assertFalse(ArcadePortRules.isQuiverWinnerScoreReached(0));
        assertFalse(ArcadePortRules.isQuiverWinnerScoreReached(19));
        assertTrue(ArcadePortRules.isQuiverWinnerScoreReached(20));
        assertTrue(ArcadePortRules.isQuiverWinnerScoreReached(21));
        assertEquals(20L, ArcadePortRules.QUIVER_RESPAWN_DELAY_TICKS);
    }

    @Test void turfWarsUsesFortySecondOpeningBuildThenTwentySecondBuildAndNinetySecondCombat() {
        assertEquals(40_000L, ArcadePortRules.TURF_WARS_INITIAL_BUILD_PHASE_MILLIS);
        assertEquals(20_000L, ArcadePortRules.TURF_WARS_BUILD_PHASE_MILLIS);
        assertEquals(90_000L, ArcadePortRules.TURF_WARS_COMBAT_PHASE_MILLIS);
    }

    @Test void turfWarsKillsMoveFrontlineAndWinAtTheTerritoryEdge() {
        assertEquals(0, ArcadePortRules.advanceTurfFrontline(-1, 0));
        assertEquals(-2, ArcadePortRules.advanceTurfFrontline(-1, 1));
        assertEquals(-1, ArcadePortRules.advanceTurfFrontline(-1, 3));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(18, 0, 20));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(19, 0, 20));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(-20, 1, 20));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(-21, 1, 20));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(20, 2, 20));
    }

    @Test void paintballKitHitsPaintTheCorrectNumberOfArmourPieces() {
        assertEquals(3, ArcadePortRules.paintballPaintAmount("rifle", false, 0.0D));
        assertEquals(3, ArcadePortRules.paintballPaintAmount("rifle", true, 0.999D));
        assertEquals(1, ArcadePortRules.paintballPaintAmount("shotgun", false, 0.999D));
        assertEquals(1, ArcadePortRules.paintballPaintAmount("machine-gun", false, 0.999D));
        assertEquals(1, ArcadePortRules.paintballPaintAmount("sniper", false, 0.999D));
        assertEquals(1, ArcadePortRules.paintballPaintAmount("sniper", true, 0.0D));
        assertEquals(2, ArcadePortRules.paintballPaintAmount("sniper", true, 0.3D));
        assertEquals(3, ArcadePortRules.paintballPaintAmount("sniper", true, 0.6D));
        assertEquals(4, ArcadePortRules.paintballPaintAmount("sniper", true, 0.999D));
        assertEquals(1, ArcadePortRules.paintballSniperPaintDamage(-1.0D));
    }

    @Test void paintballMachineGunNowAllowsFortyEightShotsBeforeOverheating() {
        double heat = 0.0D;
        for (int shot = 1; shot <= 48; shot++) {
            assertTrue(ArcadePortRules.canFirePaintballMachineGun(heat),
                    "Shot " + shot + " should still be allowed");
            heat = ArcadePortRules.paintballMachineGunHeatAfterShot(heat);
        }
        assertFalse(ArcadePortRules.canFirePaintballMachineGun(heat),
                "The next shot after 48 should be blocked until the gun cools");
        assertEquals(48, ArcadePortRules.PAINTBALL_MACHINE_GUN_SHOTS_PER_HEAT_CYCLE);
        assertEquals(80L, ArcadePortRules.PAINTBALL_MACHINE_GUN_FIRE_COOLDOWN_MILLIS);
        assertEquals(250L, ArcadePortRules.PAINTBALL_MACHINE_GUN_COOL_RECHARGE_MILLIS);
        assertEquals(0.0D, ArcadePortRules.coolPaintballMachineGunHeat(0.01D), 0.000001D);
    }

    @Test void paintballWeaponSkinsChangeTheVisibleHeldItemWithoutChangingKitIdentity() {
        assertEquals(Material.IRON_HORSE_ARMOR,
                ArcadePortRules.paintballWeaponMaterial("rifle", "chrome-marker"));
        assertEquals(Material.STONE_HOE,
                ArcadePortRules.paintballWeaponMaterial("sniper", "chrome-marker"));
        assertEquals(Material.GOLDEN_HORSE_ARMOR,
                ArcadePortRules.paintballWeaponMaterial("shotgun", "chrome-marker"));
        assertEquals(Material.DIAMOND_HORSE_ARMOR,
                ArcadePortRules.paintballWeaponMaterial("machine-gun", "chrome-marker"));
        assertEquals(Material.CROSSBOW,
                ArcadePortRules.paintballWeaponMaterial("rifle", "splatter-cannon"));
        assertEquals(Material.CROSSBOW,
                ArcadePortRules.paintballWeaponMaterial("sniper", "splatter-cannon"));
        assertEquals(Material.BLAZE_ROD,
                ArcadePortRules.paintballWeaponMaterial("machine-gun", "neon-suppressor"));
    }

    @Test void respawningPlayersPreventPrematureRoundEnd() {
        assertFalse(ArcadePortRules.shouldFinishWhenNobodyIsAlive(0, 1));
        assertFalse(ArcadePortRules.shouldFinishWhenNobodyIsAlive(1, 0));
        assertTrue(ArcadePortRules.shouldFinishWhenNobodyIsAlive(0, 0));
    }

    @Test void deathTagOnlyAwardsWinningPlacementWhenARunnerSurvives() {
        assertTrue(ArcadePortRules.deathTagHasRunnerWinner(1));
        assertTrue(ArcadePortRules.deathTagHasRunnerWinner(4));
        assertFalse(ArcadePortRules.deathTagHasRunnerWinner(0));
        assertFalse(ArcadePortRules.deathTagHasRunnerWinner(-1));
    }

    @Test void deathTagAddsAChaserEveryThirtySeconds() {
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(0L));
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(29_999L));
        assertEquals(2, ArcadePortRules.requiredDeathTagChasers(30_000L));
        assertEquals(2, ArcadePortRules.requiredDeathTagChasers(59_999L));
        assertEquals(3, ArcadePortRules.requiredDeathTagChasers(60_000L));
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(-1L));
        assertEquals(30_000L, ArcadePortRules.DEATH_TAG_CHASER_PROMOTION_INTERVAL_MILLIS);
    }

    @Test void sourceWorldResetOnlyAcceptsReservedWorldNames() {
        assertTrue(SourceMapCatalog.isReservedSourceWorldName("arcadeport_source_bacon_brawl"));
        assertTrue(SourceMapCatalog.isReservedSourceWorldName("arcadeport_source_micro_battles"));
        assertFalse(SourceMapCatalog.isReservedSourceWorldName("world"));
        assertFalse(SourceMapCatalog.isReservedSourceWorldName("../world"));
        assertFalse(SourceMapCatalog.isReservedSourceWorldName("arcadeport_proto_evolution"));
    }

    @Test void sourceMapInstallerRecognizesAnArchiveWrappedInOneWorldFolder(
            @org.junit.jupiter.api.io.TempDir Path temporaryDirectory) throws Exception {
        Path nestedWorld = temporaryDirectory.resolve("Mighty Lava Arena");
        java.nio.file.Files.createDirectories(nestedWorld);
        java.nio.file.Files.write(nestedWorld.resolve("level.dat"), new byte[] {1, 2, 3});
        java.nio.file.Files.writeString(nestedWorld.resolve("WorldConfig.dat"), "MAP_NAME:Mighty Lava Arena\\n");
        java.nio.file.Files.createDirectories(nestedWorld.resolve("region"));
        java.nio.file.Files.write(nestedWorld.resolve("region/r.0.0.mca"), new byte[] {1});

        assertEquals(nestedWorld, SourceMapCatalog.locateMapRoot(temporaryDirectory));
    }

    @Test void legacySourceMapStorageIsNormalisedForPaper26WithoutOverwritingTerrain(
            @org.junit.jupiter.api.io.TempDir Path temporaryDirectory) throws Exception {
        Path worldRoot = temporaryDirectory.resolve("arcadeport_source_bacon_brawl");
        java.nio.file.Files.createDirectories(worldRoot.resolve("region"));
        java.nio.file.Files.write(worldRoot.resolve("region/r.0.0.mca"), new byte[] {1, 2, 3});
        java.nio.file.Files.createDirectories(worldRoot.resolve("entities"));
        java.nio.file.Files.write(worldRoot.resolve("entities/r.0.0.mca"), new byte[] {4});
        java.nio.file.Files.write(worldRoot.resolve("level.dat"), new byte[] {1});
        java.nio.file.Files.writeString(worldRoot.resolve("WorldConfig.dat"), "MAP_NAME:Mighty Lava Arena\\n");

        SourceMapCatalog.preparePaper26WorldStorage(worldRoot);
        Path modernRoot = worldRoot.resolve("dimensions/minecraft/overworld");
        assertTrue(java.nio.file.Files.isRegularFile(modernRoot.resolve("region/r.0.0.mca")));
        assertTrue(java.nio.file.Files.isRegularFile(modernRoot.resolve("entities/r.0.0.mca")));
        assertFalse(java.nio.file.Files.exists(worldRoot.resolve("region")));
        assertTrue(java.nio.file.Files.isRegularFile(worldRoot.resolve("level.dat")));
        assertTrue(java.nio.file.Files.isRegularFile(worldRoot.resolve("WorldConfig.dat")));

        // The structural conversion is idempotent after a partial retry.
        SourceMapCatalog.preparePaper26WorldStorage(worldRoot);
        assertTrue(java.nio.file.Files.isRegularFile(modernRoot.resolve("region/r.0.0.mca")));
    }

    @Test void authenticBaconBrawlArchiveCanSupplyWorldConfigAfterPaperRemovesLegacyFolder() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("maps/bacon-brawl.zip")) {
            assertNotNull(input, "Bacon Brawl authentic source archive must be packaged");
            String worldConfig = null;
            try (var zip = new java.util.zip.ZipInputStream(input)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().equals("WorldConfig.dat")) {
                        worldConfig = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        break;
                    }
                    zip.closeEntry();
                }
            }
            assertNotNull(worldConfig, "The selected Bacon Brawl archive must carry Reborn map metadata");
            SourceMapCatalog.MapData parsed = SourceMapCatalog.parseMapConfig(
                    null, java.util.Arrays.asList(worldConfig.split("\\R", -1)));
            assertNotNull(parsed);
            assertEquals("Mighty Lava Arena", parsed.mapName());
            assertTrue(parsed.teamSpawnLocationsForName("Green").size() >= 16,
                    "Bacon Brawl needs its original 16 Green free-for-all starts even when Paper deletes the legacy folder");
            assertEquals(4, parsed.minY());
        }
    }

    @Test void paper26SourceMapNormalisationRejectsAmbiguousExistingRegionData(
            @org.junit.jupiter.api.io.TempDir Path temporaryDirectory) throws Exception {
        Path worldRoot = temporaryDirectory.resolve("arcadeport_source_bacon_brawl");
        java.nio.file.Files.createDirectories(worldRoot.resolve("region"));
        java.nio.file.Files.write(worldRoot.resolve("region/r.0.0.mca"), new byte[] {1});
        Path modernRegion = worldRoot.resolve("dimensions/minecraft/overworld/region");
        java.nio.file.Files.createDirectories(modernRegion);
        java.nio.file.Files.write(modernRegion.resolve("r.0.0.mca"), new byte[] {2});

        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                () -> SourceMapCatalog.preparePaper26WorldStorage(worldRoot));
    }

    @Test void sourceMapMetadataLookupFindsLegacyConfigWhenPaperMovesTheDimensionFolder(
            @org.junit.jupiter.api.io.TempDir Path temporaryDirectory) throws Exception {
        Path legacyRoot = temporaryDirectory.resolve("arcadeport_source_bacon_brawl");
        Path migratedWorldFolder = temporaryDirectory.resolve(
                "world/dimensions/minecraft/arcadeport_source_bacon_brawl");
        java.nio.file.Files.createDirectories(legacyRoot);
        java.nio.file.Files.createDirectories(migratedWorldFolder);
        Path legacyConfig = legacyRoot.resolve("WorldConfig.dat");
        java.nio.file.Files.writeString(legacyConfig, "MAP_NAME:Mighty Lava Arena\\n");

        assertEquals(legacyConfig, SourceMapCatalog.resolveWorldConfigPath(
                migratedWorldFolder, temporaryDirectory, "arcadeport_source_bacon_brawl"));
    }

    @Test void dragonEscapeMapMarkerQueriesAreNormalised() {
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAGON_ESCAPE));
        assertEquals("maps/dragon-escape.zip", SourceMapCatalog.resourceFor(ArcadePortGame.DRAGON_ESCAPE));
    }

    @Test void oneInTheQuiverArchiveIsNotAccidentallyTheDeathTagMap() throws Exception {
        try (var quiver = getClass().getClassLoader().getResourceAsStream("maps/one-in-the-quiver.zip");
             var deathTag = getClass().getClassLoader().getResourceAsStream("maps/death-tag.zip")) {
            assertNotNull(quiver, "One in the Quiver needs its own source map archive");
            assertNotNull(deathTag, "Death Tag source map archive should be present");
            assertFalse(java.util.Arrays.equals(quiver.readAllBytes(), deathTag.readAllBytes()),
                    "One in the Quiver must not silently reuse the Death Tag village archive");
        }
    }

    @Test void microBattlesGeneratedBarrierFallbackIsLimitedToPrototypeMaps() {
        assertTrue(ArcadePortRules.shouldRemoveGeneratedMicroBarrier(false, 0));
        assertFalse(ArcadePortRules.shouldRemoveGeneratedMicroBarrier(true, 0));
        assertFalse(ArcadePortRules.shouldRemoveGeneratedMicroBarrier(true, 12));
        assertFalse(ArcadePortRules.shouldRemoveGeneratedMicroBarrier(false, 4));
    }

    @Test void dragonsFlightTargetsStayInsideSourceMapBounds() {
        assertEquals(101.0D, ArcadePortRules.clampDragonSkyTargetCoordinate(50.0D, 100, 140), 0.00001D);
        assertEquals(138.999D, ArcadePortRules.clampDragonSkyTargetCoordinate(200.0D, 100, 140), 0.00001D);
        assertEquals(120.0D, ArcadePortRules.clampDragonSkyTargetCoordinate(Double.NaN, 100, 140), 0.00001D);
        assertEquals(100.5D, ArcadePortRules.clampDragonSkyTargetCoordinate(500.0D, 100, 101), 0.00001D);
        assertEquals(-19.0D, ArcadePortRules.clampDragonSkyTargetCoordinate(-500.0D, -20, 20), 0.00001D);
    }

    @Test void sourceMapSpawnsAreInterleavedAcrossConfiguredTeams() {
        assertEquals(java.util.List.of(0, 1, 2, 0, 1, 0),
                SourceMapCatalog.MapData.roundRobinGroupOrder(java.util.List.of(3, 2, 1)));
        assertEquals(java.util.List.of(0, 1, 2, 3, 0, 1, 2, 3),
                SourceMapCatalog.MapData.roundRobinGroupOrder(java.util.List.of(2, 2, 2, 2)));
        assertEquals(java.util.List.of(),
                SourceMapCatalog.MapData.roundRobinGroupOrder(java.util.List.of(0, -1, 0)));
        assertEquals(java.util.List.of(),
                SourceMapCatalog.MapData.roundRobinGroupOrder(null));
        assertEquals(0, SourceMapCatalog.MapData.teamIndexForGroup("Red", 0, 4));
        assertEquals(1, SourceMapCatalog.MapData.teamIndexForGroup("Blue", 1, 4));
        assertEquals(1, SourceMapCatalog.MapData.teamIndexForGroup("Aqua", 1, 4));
        assertEquals(2, SourceMapCatalog.MapData.teamIndexForGroup("Green", 2, 4));
        assertEquals(3, SourceMapCatalog.MapData.teamIndexForGroup("Yellow", 3, 4));
        assertEquals(3, SourceMapCatalog.MapData.teamIndexForGroup("team-four", 3, 4));
        assertEquals(0, SourceMapCatalog.MapData.teamIndexForGroup("Green", 2, 2));
    }

    @Test void dragonEscapeSourceRouteKeepsAuthoredOrderThroughSwitchbacks() {
        org.bukkit.Location start = new org.bukkit.Location(null, 0.0D, 64.0D, 0.0D);
        java.util.List<org.bukkit.Location> source = java.util.List.of(
                new org.bukkit.Location(null, 0.0D, 64.0D, 5.0D),
                new org.bukkit.Location(null, 0.1D, 64.0D, 5.0D),
                new org.bukkit.Location(null, 64.0D, 64.0D, 5.0D),
                new org.bukkit.Location(null, 64.0D, 64.0D, 1.0D));
        java.util.List<org.bukkit.Location> ordered =
                SourceMapCatalog.MapData.orderedRouteMarkers(start, source, 0.75D);
        assertEquals(4, ordered.size());
        assertEquals(0.0D, ordered.get(0).getZ(), 0.00001D); // RED start is waypoint zero.
        assertEquals(5.0D, ordered.get(1).getZ(), 0.00001D);
        assertEquals(64.0D, ordered.get(2).getX(), 0.00001D);
        assertEquals(1.0D, ordered.get(3).getZ(), 0.00001D);
        assertEquals(1, SourceMapCatalog.MapData.orderedRouteMarkers(start, null, 0.75D).size());
    }

    @Test void originalMapCatalogEmbedsFourteenSourceWorldsAndLeavesUnmappedGamesExplicitlyUnmapped() {
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.BACON_BRAWL));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.CASTLE_ASSAULT));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.HEROES_OF_GWEN));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAW_MY_THING));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.SKY_WARS));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.MILK_THE_COW));
        assertTrue(SourceMapCatalog.mapFoldersFor(ArcadePortGame.WITHER_ASSAULT).isEmpty());
        assertTrue(SourceMapCatalog.mapFoldersFor(ArcadePortGame.MASTER_BUILDERS).isEmpty());
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.BOMB_LOBBERS));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DEATH_TAG));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAGON_ESCAPE));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAGONS));
        assertFalse(SourceMapCatalog.hasMap(ArcadePortGame.EVOLUTION));
        assertFalse(SourceMapCatalog.hasMap(ArcadePortGame.CAMERA_SHY));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.SUPER_SPLOOR));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAGONS));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.RUNNER));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.GLADIATORS));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.MICRO_BATTLES));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.ONE_IN_THE_QUIVER));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.PAINTBALL));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.RUNNER));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.SHEEP_QUEST));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.SPLEEF));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.TURF_WARS));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.MILK_THE_COW));
        assertFalse(SourceMapCatalog.hasMap(ArcadePortGame.WITHER_ASSAULT));
        assertNull(SourceMapCatalog.resourceFor(ArcadePortGame.EVOLUTION));
        assertNull(SourceMapCatalog.resourceFor(ArcadePortGame.WITHER_ASSAULT));
    }

    @Test void oneInTheQuiverSourceKitsAndMeleeDamageAreExplicit() {
        assertEquals("leaper", ArcadePortRules.normalizeQuiverKit("Leaper"));
        assertEquals("brawler", ArcadePortRules.normalizeQuiverKit("quiver-brawler"));
        assertEquals("enchanter", ArcadePortRules.normalizeQuiverKit("Enchanter"));
        assertEquals("slam-shot", ArcadePortRules.normalizeQuiverKit("Slam Shot"));
        assertEquals("ninja", ArcadePortRules.normalizeQuiverKit("Ninja"));
        assertNull(ArcadePortRules.normalizeQuiverKit("unknown"));
        assertEquals(5.0D, ArcadePortRules.quiverMeleeDamage("leaper"), 0.00001D);
        assertEquals(7.0D, ArcadePortRules.quiverMeleeDamage("brawler"), 0.00001D);
        assertEquals(5.0D, ArcadePortRules.quiverMeleeDamage("enchanter"), 0.00001D);
        assertEquals(5.5D, ArcadePortRules.quiverMeleeDamage("slam-shot"), 0.00001D);
        assertEquals(4.0D, ArcadePortRules.quiverMeleeDamage("ninja"), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.quiverMeleeDamage("leaper", false), 0.00001D);
        assertEquals(2.0D, ArcadePortRules.quiverMeleeDamage("brawler", false), 0.00001D);
        assertEquals(0.8D, ArcadePortRules.quiverReboundPower(0.0D), 0.00001D);
        assertEquals(0.9D, ArcadePortRules.quiverReboundPower(3.0D), 0.00001D);
        assertEquals(1.2D, ArcadePortRules.quiverReboundPower(30.0D), 0.00001D);
        assertEquals(0.03D, ArcadePortRules.quiverReboundVerticalArc(3.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.quiverReboundVerticalArc(Double.NaN), 0.00001D);
        assertEquals("bullseye-dash", ArcadePortRules.normalizeQuiverArrowFlightCosmetic(null));
        assertEquals("ricochet-rune", ArcadePortRules.normalizeQuiverArrowFlightCosmetic("RICOCHET-RUNE"));
        assertEquals("bullseye-dash", ArcadePortRules.normalizeQuiverArrowFlightCosmetic("unknown"));
        assertTrue(ArcadePortRules.shouldRemoveQuiverInvisibilityOnHit(true, true));
        assertFalse(ArcadePortRules.shouldRemoveQuiverInvisibilityOnHit(false, true));
        assertEquals(10_000L, ArcadePortRules.QUIVER_GROUND_POUND_COOLDOWN_MILLIS);
        assertEquals(1_200L, ArcadePortRules.QUIVER_NINJA_VANISH_MILLIS);
        assertEquals(20, ArcadePortRules.QUIVER_PERFECT_RUN_KILL_STREAK);
        assertFalse(ArcadePortRules.isQuiverPerfectRun(19));
        assertTrue(ArcadePortRules.isQuiverPerfectRun(20));
        assertEquals("TRIPLE KILL", ArcadePortRules.quiverKillstreakTitle(3));
        assertEquals("PERFECT RUN", ArcadePortRules.quiverKillstreakTitle(20));
        assertNull(ArcadePortRules.quiverKillstreakTitle(4));
        assertTrue(ArcadePortRules.isQuiverKillstreakAnnouncement(13));
        assertFalse(ArcadePortRules.isQuiverKillstreakAnnouncement(14));
        assertFalse(ArcadePortRules.isQuiverSpeedrunnerWin(-1L));
        assertTrue(ArcadePortRules.isQuiverSpeedrunnerWin(89_999L));
        assertFalse(ArcadePortRules.isQuiverSpeedrunnerWin(90_000L));
        assertFalse(ArcadePortRules.shouldAwardQuiverFistPunchKills(2));
        assertTrue(ArcadePortRules.shouldAwardQuiverFistPunchKills(3));
        assertEquals(12.0D, ArcadePortRules.quiverBrawlerHealthAfterKill(10.0D, 20.0D), 0.00001D);
        assertEquals(20.0D, ArcadePortRules.quiverBrawlerHealthAfterKill(19.0D, 20.0D), 0.00001D);
    }

    @Test void quiverAchievementSetMatchesTheUpdatedRequirements() {
        assertEquals(6, QuiverAchievement.values().length);
        assertEquals("The Perfect Game", QuiverAchievement.THE_PERFECT_GAME.displayName());
        assertEquals("Achieve a Perfect Run killstreak in a game.",
                QuiverAchievement.THE_PERFECT_GAME.requirement());
        assertEquals("Win a game by getting 20 kills without using archery.",
                QuiverAchievement.WHATS_A_BOW.requirement());
        assertEquals("Win a game in under 1:30.", QuiverAchievement.SPEEDRUNNER.requirement());
        assertEquals("Get 3 kills with fist/bow punch.",
                QuiverAchievement.THATS_NOT_A_SWORD.requirement());
        assertEquals("Kill a player with a high-arced bow shot.", QuiverAchievement.HEADS.requirement());
        assertEquals("Win with 10 separate two-kill streaks.", QuiverAchievement.RHYTHM_HEAVEN.requirement());
    }

    @Test void dragonsSourceKitsSpawnLoopSparklerAndFallRulesAreExplicit() {
        assertEquals("coward", ArcadePortRules.normalizeDragonsKit("Coward"));
        assertEquals("marksman", ArcadePortRules.normalizeDragonsKit("Marksman"));
        assertEquals("pyrotechnic", ArcadePortRules.normalizeDragonsKit("Pyro"));
        assertNull(ArcadePortRules.normalizeDragonsKit("unknown"));
        assertTrue(ArcadePortRules.shouldSpawnTestDragon(0, 1_000L, 1_000L));
        assertFalse(ArcadePortRules.shouldSpawnTestDragon(5, 1_000L, 1_000L));
        assertFalse(ArcadePortRules.shouldSpawnTestDragon(2, 999L, 1_000L));
        assertTrue(ArcadePortRules.shouldReplenishDragonSparkler(25_000L, 25_000L, 1));
        assertFalse(ArcadePortRules.shouldReplenishDragonSparkler(25_000L, 25_000L, 2));
        assertTrue(ArcadePortRules.shouldExpireDragonSparkler(101, 64.0D, false));
        assertFalse(ArcadePortRules.shouldExpireDragonSparkler(20, 3.9D, false));
        assertFalse(ArcadePortRules.shouldExpireDragonSparkler(20, 64.0D, true));
        assertFalse(ArcadePortRules.shouldDragonSparklerAttract(3.9D, false));
        assertFalse(ArcadePortRules.shouldDragonSparklerAttract(64.0D, true));
        assertTrue(ArcadePortRules.shouldDragonSparklerAttract(64.0D, false));
        assertEquals(3.0D, ArcadePortRules.dragonFallDamageAfterReduction(4.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.dragonFallDamageAfterReduction(0.5D), 0.00001D);
        assertEquals(5_000L, ArcadePortRules.DRAGONS_SPAWN_INTERVAL_MILLIS);
        assertEquals(125L, ArcadePortRules.DRAGONS_TARGET_INTERVAL_MILLIS);
        assertEquals(500L, ArcadePortRules.DRAGONS_PERK_UPDATE_INTERVAL_MILLIS);
        assertEquals(500L, ArcadePortRules.DRAGONS_WATER_DAMAGE_INTERVAL_MILLIS);
        assertEquals(50L, ArcadePortRules.DRAGONS_BARRAGE_FIRE_INTERVAL_MILLIS);
        assertEquals(6, ArcadePortRules.DRAGONS_BARRAGE_MAX_ARROWS);
    }

    @Test void microBattlesUsesTheOriginalSourceMapArchive() {
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.MICRO_BATTLES));
        assertEquals(java.util.List.of("Micro Battle"), SourceMapCatalog.mapFoldersFor(ArcadePortGame.MICRO_BATTLES));
    }

    @Test void microBattlesSourceKitsFletcherAndIronSkinAreExplicit() {
        assertEquals("archer", ArcadePortRules.normalizeMicroBattlesKit("Archer"));
        assertEquals("worker", ArcadePortRules.normalizeMicroBattlesKit("Worker"));
        assertEquals("fighter", ArcadePortRules.normalizeMicroBattlesKit("Fighter"));
        assertNull(ArcadePortRules.normalizeMicroBattlesKit("unknown"));
        assertEquals(20_000L, ArcadePortRules.MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS);
        assertEquals(3, ArcadePortRules.MICRO_BATTLES_FLETCHER_MAX_ARROWS);
        assertEquals(3, ArcadePortRules.MICRO_BATTLES_CRUMBLE_BLOCKS_PER_TICK);
        assertEquals(2, ArcadePortRules.MICRO_BATTLES_CRUMBLE_SCAN_INTERVAL_TICKS);
        assertTrue(ArcadePortRules.shouldReplenishMicroArrow(20_000L, 20_000L, 2, 3));
        assertFalse(ArcadePortRules.shouldReplenishMicroArrow(19_999L, 20_000L, 2, 3));
        assertFalse(ArcadePortRules.shouldReplenishMicroArrow(20_000L, 20_000L, 3, 3));
        assertEquals(3.5D, ArcadePortRules.microBattlesDamageAfterIronSkin(4.0D, true), 0.00001D);
        assertEquals(5.5D, ArcadePortRules.microBattlesDamageAfterIronSkin(6.0D, true), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(1.0D, true), 0.00001D);
        assertEquals(1.5D, ArcadePortRules.microBattlesDamageAfterIronSkin(2.0D, true), 0.00001D);
        assertEquals(4.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(4.0D, false), 0.00001D);
    }

    @Test void spleefBrawlerLeapMatchesLegacyPowerVerticalBoostAndGroundCap() {
        org.bukkit.util.Vector airborne = ArcadePortRules.spleefLeapVelocity(
                new org.bukkit.util.Vector(1.0D, 0.0D, 0.0D), false);
        assertEquals(1.2D, airborne.getX(), 0.00001D);
        assertEquals(0.2D, airborne.getY(), 0.00001D);
        assertEquals(0.0D, airborne.getZ(), 0.00001D);

        org.bukkit.util.Vector grounded = ArcadePortRules.spleefLeapVelocity(
                new org.bukkit.util.Vector(1.0D, 0.0D, 0.0D), true);
        assertEquals(1.2D, grounded.getX(), 0.00001D);
        assertEquals(0.4D, grounded.getY(), 0.00001D);

        org.bukkit.util.Vector lookingDown = ArcadePortRules.spleefLeapVelocity(
                new org.bukkit.util.Vector(0.0D, -1.0D, 0.0D), false);
        assertEquals(-1.0D, lookingDown.getY(), 0.00001D);
        org.bukkit.util.Vector lookingUp = ArcadePortRules.spleefLeapVelocity(
                new org.bukkit.util.Vector(0.0D, 1.0D, 0.0D), true);
        assertEquals(1.2D, lookingUp.getY(), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.spleefLeapVelocity(
                new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D), true).length(), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.spleefLeapVelocity(
                new org.bukkit.util.Vector(Double.NaN, 0.0D, 0.0D), true).length(), 0.00001D);
    }

    @Test void spleefSourceKitSelectionHungerAndPerkTimingAreExplicit() {
        assertEquals("snowballer", ArcadePortRules.normalizeSpleefKit("Snowballer"));
        assertEquals("brawler", ArcadePortRules.normalizeSpleefKit("brawler"));
        assertEquals("archer", ArcadePortRules.normalizeSpleefKit("Archer"));
        assertNull(ArcadePortRules.normalizeSpleefKit("unknown"));
        assertEquals(4_000L, ArcadePortRules.SPLEEF_HUNGER_DRAIN_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.SPLEEF_HUNGER_DRAIN_PER_INTERVAL);
        assertEquals(50L, ArcadePortRules.SPLEEF_HUNGER_RESTORE_COOLDOWN_MILLIS);
        assertEquals(2_000L, ArcadePortRules.SPLEEF_ARCHER_ARROW_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.SPLEEF_ARCHER_MAX_ARROWS);
        assertEquals(16, ArcadePortRules.SPLEEF_SNOWBALLER_MAX_SNOWBALLS);
        assertEquals(30_000L, ArcadePortRules.SPLEEF_BRAWLER_LEAP_COOLDOWN_MILLIS);
        assertEquals(400L, ArcadePortRules.SPLEEF_KNOCKBACK_COOLDOWN_MILLIS);
        assertEquals(0.3D, ArcadePortRules.SPLEEF_DEFAULT_KNOCKBACK, 0.00001D);
        assertEquals(0.6D, ArcadePortRules.SPLEEF_BRAWLER_KNOCKBACK, 0.00001D);
        assertEquals(250L, ArcadePortRules.SPLEEF_SMASHER_COOLDOWN_MILLIS);
        assertEquals(4, ArcadePortRules.nextSpleefSlowDamageData(5));
        assertEquals(4, ArcadePortRules.nextSpleefSlowDamageData(13));
        assertEquals(14, ArcadePortRules.nextSpleefSlowDamageData(4));
        assertEquals(-1, ArcadePortRules.nextSpleefSlowDamageData(14));
    }

    @Test void spleefSlowDamageMatchesLegacyColourDataTransitions() {
        assertEquals(org.bukkit.Material.YELLOW_WOOL,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.LIME_WOOL));
        assertEquals(org.bukkit.Material.YELLOW_WOOL,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.GREEN_WOOL));
        assertEquals(org.bukkit.Material.RED_WOOL,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.YELLOW_WOOL));
        assertNull(ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.RED_WOOL));
        assertNull(ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.ORANGE_WOOL));
        assertNull(ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.BLUE_WOOL));
        assertEquals(org.bukkit.Material.YELLOW_TERRACOTTA,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.LIME_TERRACOTTA));
        assertEquals(org.bukkit.Material.RED_TERRACOTTA,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.YELLOW_TERRACOTTA));
        assertNull(ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.ORANGE_TERRACOTTA));
    }

    @Test void spleefProtectsOnlyBedrockAndLavaLikeReborn() {
        assertFalse(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.SNOW_BLOCK));
        assertFalse(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.LIGHT_BLUE_WOOL));
        assertFalse(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.RED_TERRACOTTA));
        assertFalse(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.STONE));
        assertFalse(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.GLASS));
        assertFalse(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.OBSIDIAN));
        assertTrue(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.AIR));
        assertTrue(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.BEDROCK));
        assertTrue(ArcadePortRules.isSpleefProtectedMaterial(org.bukkit.Material.LAVA));
        assertTrue(ArcadePortRules.isSpleefProtectedMaterial(null));
    }

    @Test void bombLobbersExplosionExposureUsesVanillaBoundingBoxSampleGrid() {
        double stepX = ArcadePortRules.bombLobbersExposureSampleStep(0.6D);
        double stepY = ArcadePortRules.bombLobbersExposureSampleStep(1.8D);
        assertEquals(1.0D / 2.2D, stepX, 0.000001D);
        assertEquals(1.0D / 4.6D, stepY, 0.000001D);
        assertEquals(0.5D * (1.0D - 2.0D * stepX),
                ArcadePortRules.bombLobbersExposureSampleOffset(stepX), 0.000001D);
        assertEquals(0.0D, ArcadePortRules.bombLobbersExposureSampleOffset(Double.NaN), 0.000001D);
        assertTrue(Double.isNaN(ArcadePortRules.bombLobbersExposureSampleStep(-0.1D)));
    }

    @Test void bombLobbersDirectHitUsesLegacyKnockbackAndGroundBoost() {
        var airborne = ArcadePortRules.bombLobbersDirectHitVelocity(
                new org.bukkit.util.Vector(1.0D, 0.0D, 0.0D), false);
        assertEquals(0.2D, airborne.getX(), 0.00001D);
        assertEquals(0.2D, airborne.getY(), 0.00001D);
        assertEquals(0.0D, airborne.getZ(), 0.00001D);

        var grounded = ArcadePortRules.bombLobbersDirectHitVelocity(
                new org.bukkit.util.Vector(0.0D, 0.0D, -2.0D), true);
        assertEquals(0.0D, grounded.getX(), 0.00001D);
        assertEquals(0.4D, grounded.getY(), 0.00001D);
        assertEquals(-0.2D, grounded.getZ(), 0.00001D);

        assertEquals(new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D),
                ArcadePortRules.bombLobbersDirectHitVelocity(
                        new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D), true));
    }

    @Test void bombLobbersDoubleJumpAppliesLegacyGroundBoostAfterVelocityCap() {
        assertEquals(50L, ArcadePortRules.BOMB_LOBBERS_DOUBLE_JUMP_RECHARGE_MILLIS);
        assertEquals(1_050L, ArcadePortRules.nextBombLobbersDoubleJumpReadyAt(1_000L));
        var look = new org.bukkit.util.Vector(1.0D, 0.0D, 0.0D);
        var airborne = ArcadePortRules.bombLobbersDoubleJumpVelocity(look, 1.2D, false);
        var grounded = ArcadePortRules.bombLobbersDoubleJumpVelocity(look, 1.2D, true);
        assertEquals(airborne.getY() + 0.2D, grounded.getY(), 0.00001D);
        var vertical = ArcadePortRules.bombLobbersDoubleJumpVelocity(
                new org.bukkit.util.Vector(0.0D, 1.0D, 0.0D), 1.2D, true);
        assertEquals(1.4D, vertical.getY(), 0.00001D);
    }

    @Test void bombLobbersExplosionAddsLegacyImpulseInsteadOfReplacingVelocity() {
        var impulse = ArcadePortRules.bombLobbersExplosionVelocity(
                new org.bukkit.util.Vector(0.25D, 0.4D, -0.1D),
                new org.bukkit.util.Vector(1.0D, 0.0D, 0.0D), 0.5D);
        assertEquals(0.75D, impulse.getX(), 0.00001D);
        assertEquals(0.4D, impulse.getY(), 0.00001D);
        assertEquals(-0.1D, impulse.getZ(), 0.00001D);
        assertEquals(new org.bukkit.util.Vector(0.25D, 0.4D, -0.1D),
                ArcadePortRules.bombLobbersExplosionVelocity(
                        new org.bukkit.util.Vector(0.25D, 0.4D, -0.1D),
                        new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D), 0.5D));
    }

    @Test void bombLobbersDoubleJumpMatchesLegacyVelocityNormaliseThenScale() {
        var jumper = ArcadePortRules.bombLobbersDoubleJumpVelocity(
                new org.bukkit.util.Vector(1.0D, 0.0D, 0.0D), 1.2D);
        assertEquals(1.2D / Math.sqrt(2.44D), jumper.getX(), 0.00001D);
        assertEquals(1.44D / Math.sqrt(2.44D), jumper.getY(), 0.00001D);
        assertEquals(0.0D, jumper.getZ(), 0.00001D);

        var armorer = ArcadePortRules.bombLobbersDoubleJumpVelocity(
                new org.bukkit.util.Vector(1.0D, 0.0D, 0.0D), 0.9D);
        assertEquals(0.9D / Math.sqrt(1.81D), armorer.getX(), 0.00001D);
        assertEquals(0.81D / Math.sqrt(1.81D), armorer.getY(), 0.00001D);

        assertEquals(new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D),
                ArcadePortRules.bombLobbersDoubleJumpVelocity(
                        new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D), 1.2D));
    }

    @Test void bombLobbersThrowsOnBothMouseButtonsButNotPhysicalActions() {
        // Source listener rejects only Action.PHYSICAL before checking the held TNT.
        assertTrue(ArcadePortRules.shouldThrowBombLobbersOnAction(false)); // left/right, air/block
        assertFalse(ArcadePortRules.shouldThrowBombLobbersOnAction(true)); // pressure-plate / physical
    }

    @Test void bombLobbersOnlyArmorerAndPitcherEquipArmor() {
        assertFalse(ArcadePortRules.bombLobbersKitUsesArmor("jumper"));
        assertTrue(ArcadePortRules.bombLobbersKitUsesArmor("armorer"));
        assertTrue(ArcadePortRules.bombLobbersKitUsesArmor("pitcher"));
        assertFalse(ArcadePortRules.bombLobbersKitUsesArmor("waller"));
        assertFalse(ArcadePortRules.bombLobbersKitUsesArmor("unknown"));
    }

    @Test void bombLobbersUsesItsLegacyFiveDamageWaterPenalty() {
        assertEquals(5, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.BOMB_LOBBERS, "Mighty Lava Arena"));
        assertEquals(0, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.BACON_BRAWL, "Mighty Lava Arena"));
    }

    @Test void bombLobbersSpectatorCompassTargetsOnlyOpponentsAndUsesThreeSecondTeleportCooldown() {
        assertTrue(ArcadePortRules.shouldBombLobbersCompassTrackTeam(0, 1));
        assertFalse(ArcadePortRules.shouldBombLobbersCompassTrackTeam(0, 0));
        assertTrue(ArcadePortRules.shouldBombLobbersCompassTrackTeam(-1, 1));
        assertFalse(ArcadePortRules.shouldBombLobbersCompassTrackTeam(0, -1));
        assertEquals(3_000L, ArcadePortRules.BOMB_LOBBERS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS);
        assertTrue(ArcadePortRules.isBombLobbersSpectatorTeleportReady(1_000L, 1_000L));
        assertFalse(ArcadePortRules.isBombLobbersSpectatorTeleportReady(2_999L, 3_000L));
        assertTrue(ArcadePortRules.isBombLobbersSpectatorTeleportReady(3_000L, 3_000L));
        assertEquals(4_000L, ArcadePortRules.nextBombLobbersSpectatorTeleportAt(1_000L));
    }

    @Test void bombLobbersTntRemainsTrackedAfterThrowerElimination() {
        // IsPlaying remains true for an eliminated spectator; only leaving the server removes tracking.
        assertTrue(ArcadePortRules.shouldKeepBombLobbersBombTracked(true));
        assertFalse(ArcadePortRules.shouldKeepBombLobbersBombTracked(false));
    }

    @Test void bombLobbersTeamBadgeUsesTheSourceTeamNameRatherThanTheParticleColour() {
        assertEquals("Red", ArcadePortRules.bombLobbersTeamBadgeName(0));
        assertEquals("Blue", ArcadePortRules.bombLobbersTeamBadgeName(1));
        assertEquals("Red", ArcadePortRules.bombLobbersTeamBadgeName(-1));
    }

    @Test void bombLobbersJumperAndWallerInheritTheLegacyTeamArmorModule() {
        assertTrue(ArcadePortRules.bombLobbersKitUsesTeamArmor("jumper"));
        assertTrue(ArcadePortRules.bombLobbersKitUsesTeamArmor("waller"));
        assertFalse(ArcadePortRules.bombLobbersKitUsesTeamArmor("armorer"));
        assertFalse(ArcadePortRules.bombLobbersKitUsesTeamArmor("pitcher"));
        assertFalse(ArcadePortRules.bombLobbersKitUsesTeamArmor("unknown"));
        assertEquals(8, ArcadePortRules.BOMB_LOBBERS_TEAM_BADGE_SLOT);
        assertEquals(org.bukkit.Color.RED, ArcadePortRules.bombLobbersTeamColor(0));
        assertEquals(org.bukkit.Color.AQUA, ArcadePortRules.bombLobbersTeamColor(1));
        assertEquals(org.bukkit.Color.WHITE, ArcadePortRules.bombLobbersTeamColor(-1));
    }

    @Test void bombLobbersTrailMatchesLegacyTeamSpreadAndSpeedRatherThanTintingTeams() {
        assertEquals(0.0D, ArcadePortRules.bombLobbersTrailOffset(0), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.bombLobbersTrailSpeed(0), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.bombLobbersTrailOffset(1), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.bombLobbersTrailSpeed(1), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.bombLobbersTrailOffset(-1), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.bombLobbersTrailSpeed(-1), 0.00001D);
    }

    @Test void bombLobbersTntTrailUsesSourceTwoParticlePointSpacing() {
        assertEquals(1, ArcadePortRules.bombLobbersTrailSampleCount(0.0D));
        assertEquals(2, ArcadePortRules.bombLobbersTrailSampleCount(0.2D));
        assertEquals(3, ArcadePortRules.bombLobbersTrailSampleCount(0.4D));
        assertEquals(0, ArcadePortRules.bombLobbersTrailSampleCount(Double.NaN));
        assertEquals(0, ArcadePortRules.bombLobbersTrailSampleCount(-0.2D));
    }

    @Test void bombLobbersKitsPitchLevelsCraftmanAndBombPrimingMatchSourceTiming() {
        assertEquals("jumper", ArcadePortRules.normalizeBombLobbersKit("Jumper"));
        assertEquals("armorer", ArcadePortRules.normalizeBombLobbersKit("armourer"));
        assertEquals("pitcher", ArcadePortRules.normalizeBombLobbersKit("Pitcher"));
        assertEquals("waller", ArcadePortRules.normalizeBombLobbersKit("Waller"));
        assertNull(ArcadePortRules.normalizeBombLobbersKit("unknown"));
        assertEquals(1.75D, ArcadePortRules.bombLobbersPitchVelocity(1), 0.00001D);
        assertEquals(2.0D, ArcadePortRules.bombLobbersPitchVelocity(2), 0.00001D);
        assertEquals(2.25D, ArcadePortRules.bombLobbersPitchVelocity(3), 0.00001D);
        assertEquals(12_345L, ArcadePortRules.bombLobbersFirstCraftReadyAt(12_345L));
        assertEquals(0L, ArcadePortRules.bombLobbersFirstCraftReadyAt(-1L));
        assertEquals(8_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(0L));
        assertEquals(8_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(20_000L));
        assertEquals(7_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(20_001L));
        assertEquals(6_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(40_001L));
        assertEquals(5_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(60_001L));
        assertEquals(4_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(620_001L));
        assertFalse(ArcadePortRules.shouldPrimeBombLobbersBomb(2_999L, true));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(3_000L, true));
        assertFalse(ArcadePortRules.shouldPrimeBombLobbersBomb(7_999L, false));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(8_000L, false));
    }

    @Test void bombLobbersWallerUsesLegacyCardinalFacing() {
        assertEquals(2, ArcadePortRules.bombLobbersWallFacingIndex(0.0F));
        assertEquals(3, ArcadePortRules.bombLobbersWallFacingIndex(90.0F));
        assertEquals(0, ArcadePortRules.bombLobbersWallFacingIndex(180.0F));
        assertEquals(1, ArcadePortRules.bombLobbersWallFacingIndex(270.0F));
        assertEquals(2, ArcadePortRules.bombLobbersWallFacingIndex(Float.NaN));
        assertEquals(2, ArcadePortRules.bombLobbersWallFacingIndex(360.0F));
    }

    @Test void bombLobbersWallerRespectsSourceShortPlacementCooldown() {
        assertEquals(100L, ArcadePortRules.BOMB_LOBBERS_WALLER_COOLDOWN_MILLIS);
        assertTrue(ArcadePortRules.isBombLobbersWallerReady(1_000L, 0L));
        long readyAt = ArcadePortRules.nextBombLobbersWallerReadyAt(1_000L);
        assertEquals(1_100L, readyAt);
        assertFalse(ArcadePortRules.isBombLobbersWallerReady(1_099L, readyAt));
        assertTrue(ArcadePortRules.isBombLobbersWallerReady(1_100L, readyAt));
        assertTrue(ArcadePortRules.isBombLobbersWallerReady(1_101L, readyAt));
    }

    @Test void baconBrawlSourceArchiveContainsTheRequiredGreenSpawnMarkers() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("maps/bacon-brawl.zip")) {
            assertNotNull(input, "Bacon Brawl must use its embedded Reborn source map");
            String worldConfig = null;
            try (var zip = new java.util.zip.ZipInputStream(input)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().endsWith("WorldConfig.dat")) {
                        worldConfig = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        break;
                    }
                    zip.closeEntry();
                }
            }
            assertNotNull(worldConfig, "Source archive must include WorldConfig.dat");
            String teamName = "";
            int greenSpawnCount = 0;
            int mapMinY = Integer.MIN_VALUE;
            boolean greenStartAtY13 = false;
            for (String line : worldConfig.split("\\R")) {
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                if ("MIN_Y".equalsIgnoreCase(key)) {
                    mapMinY = Integer.parseInt(value);
                } else if ("TEAM_NAME".equalsIgnoreCase(key)) {
                    teamName = value;
                } else if ("TEAM_SPAWNS".equalsIgnoreCase(key) && "Green".equalsIgnoreCase(teamName)
                        && !value.isBlank()) {
                    for (String coordinate : value.split(":")) {
                        if (coordinate.isBlank()) continue;
                        greenSpawnCount++;
                        String[] xyz = coordinate.split(",");
                        if (xyz.length >= 2 && Double.parseDouble(xyz[1].trim()) == 13.0D) {
                            greenStartAtY13 = true;
                        }
                    }
                }
            }
            assertTrue(greenSpawnCount >= 16,
                    "Source Bacon Brawl WorldConfig.dat requires at least 16 Green TEAM_SPAWNS; got "
                            + greenSpawnCount);
            assertEquals(4, mapMinY, "Mighty Lava Arena's source volume starts at Y=4");
            assertTrue(greenStartAtY13, "Mighty Lava Arena includes valid Green starts at Y=13");
            assertEquals(3.0D, ArcadePortRules.sourceMapVoidThresholdY(true, mapMinY), 0.00001D,
                    "the arena fall guard must not eliminate players at the source spawn height");
        }
    }

    @Test void baconBrawlExposesAllRestoredKitsAndDoubleHungerRules() {
        assertEquals("pig", ArcadePortRules.normalizeBaconBrawlKit("Pig"));
        assertEquals("pig", ArcadePortRules.normalizeBaconBrawlKit("sheep-pig"));
        assertEquals("mama-piggles", ArcadePortRules.normalizeBaconBrawlKit("mama"));
        assertEquals("mama-piggles", ArcadePortRules.normalizeBaconBrawlKit("Mama Pig"));
        assertEquals("mama-piggles", ArcadePortRules.normalizeBaconBrawlKit("Mama Piggles"));
        assertEquals("el-muchacho-pigo", ArcadePortRules.normalizeBaconBrawlKit("El Muchacho Pigo"));
        assertEquals("el-muchacho-pigo", ArcadePortRules.normalizeBaconBrawlKit("body slam"));
        assertTrue(ArcadePortRules.baconBrawlUsesSheepAvatar("pig"));
        assertTrue(ArcadePortRules.baconBrawlUsesSheepAvatar("sheep-pig"));
        assertTrue(ArcadePortRules.baconBrawlUsesSheepAvatar("Sheep Pig"));
        assertFalse(ArcadePortRules.baconBrawlUsesSheepAvatar("mama-piggles"));
        assertFalse(ArcadePortRules.baconBrawlUsesSheepAvatar("el-muchacho-pigo"));
        assertEquals("chris-p-bacon", ArcadePortRules.normalizeBaconBrawlKit("chris-p-bacon"));
        assertEquals("chris-p-bacon", ArcadePortRules.normalizeBaconBrawlKit("Chris P. Bacon"));
        assertNull(ArcadePortRules.normalizeBaconBrawlKit("unknown"));
        assertEquals(4_000L, ArcadePortRules.BACON_SLOW_UPDATE_INTERVAL_MILLIS);
        assertFalse(ArcadePortRules.BACON_BRAWL_HAS_ROUND_TIMER,
                "Reborn Bacon Brawl ends when one pig remains, not at a fixed timer");
        assertTrue(ArcadePortRules.shouldFinishBaconBrawl(1));
        assertTrue(ArcadePortRules.shouldFinishBaconBrawl(0));
        assertFalse(ArcadePortRules.shouldFinishBaconBrawl(2));
        assertFalse(ArcadePortRules.shouldFinishBaconBrawl(16));
        assertEquals(4_000L, ArcadePortRules.BACON_HUNGER_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.BACON_HUNGER_DRAIN_PER_INTERVAL,
                "the selected balance change doubles Bacon Brawl hunger drain");
        assertEquals(5.0F, ArcadePortRules.BACON_SATURATION_ON_FOOD_CHANGE, 0.00001F);
        assertEquals(4.0D, ArcadePortRules.BACON_STARVATION_DAMAGE, 0.00001D);
        assertFalse(ArcadePortRules.BACON_BRAWL_HITS_DAMAGE_HEALTH,
                "source hits heal back the damage and rely on knockback for arena knock-outs");
        assertEquals(2.0D, ArcadePortRules.BACON_BODY_SLAM_KNOCKBACK_MULTIPLIER, 0.00001D);
        assertEquals(100, ArcadePortRules.BACON_METER_MAX,
                "ability charge is damage-metered rather than time-cooled");
        assertEquals(1.8D, ArcadePortRules.initialBaconKnockback(2), 0.00001D,
                "two-player rounds start with stronger knockback");
        assertEquals(1.5D, ArcadePortRules.initialBaconKnockback(3), 0.00001D);
        assertEquals(1.8D, ArcadePortRules.nextBaconKnockback(1.5D), 0.00001D);
    }

    @Test void baconBlastUsesTheOriginalSixteenPorkProjectileStack() {
        assertEquals(16, ArcadePortRules.BACON_BLAST_PROJECTILE_STACK_SIZE);
    }

    @Test void baconBrawlAbilitiesRequireTheSourceIronAxeAndRightClickButIgnoreTargetSurface() {
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, false, "el-muchacho-pigo"));
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, false, "mama-piggles"));
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, true, "pig"));
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, true, "el-muchacho-pigo"),
                "An interactable block must not suppress the ability.");
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, true, "mama-piggles"),
                "A chest, player or other clicked surface must not suppress the ability.");
        assertFalse(ArcadePortRules.shouldActivateBaconBrawlAbility(false,
                org.bukkit.Material.IRON_AXE, false, "pig"));
        assertFalse(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.STICK, false, "pig"));
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, false, "chris-p-bacon"));
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, true, "Chris P. Bacon"));
    }

    @Test void baconBrutusGuardBlocksFrontHitsButBackstabsBreakItAndPigCloakUsesTimedDuration() {
        assertEquals("pig", ArcadePortRules.normalizeBaconBrawlKit("pig-cloak"));
        assertEquals("brutus-bacon", ArcadePortRules.normalizeBaconBrawlKit("Brutus Bacon"));
        assertTrue(ArcadePortRules.shouldBaconBrutusGuardHit(true, false),
                "A frontal hit is blocked while Brutus Guard is active.");
        assertFalse(ArcadePortRules.shouldBaconBrutusGuardHit(true, true),
                "A backstab breaks the guard so the normal hit/knockback can land.");
        assertFalse(ArcadePortRules.shouldBaconBrutusGuardHit(false, false),
                "Brutus Guard should not protect outside its timer.");
        assertTrue(ArcadePortRules.isBaconBrutusBackstab(0.0D, 1.0D, 0.0D, -1.0D),
                "A hit from behind must break Brutus guard.");
        assertFalse(ArcadePortRules.isBaconBrutusBackstab(0.0D, 1.0D, 0.0D, 1.0D),
                "A hit from the front remains guarded.");
        assertFalse(ArcadePortRules.isBaconBrutusBackstab(0.0D, 1.0D, 1.0D, 0.0D),
                "A side hit does not count as a backstab.");
        assertTrue(ArcadePortRules.isBaconTimedCritical(500L));
        assertTrue(ArcadePortRules.isBaconTimedCritical(900L));
        assertFalse(ArcadePortRules.isBaconTimedCritical(499L));
        assertFalse(ArcadePortRules.isBaconTimedCritical(901L));
        assertEquals(0L, ArcadePortRules.baconAbilityDurationMillis(10));
        assertEquals(2_750L, ArcadePortRules.baconAbilityDurationMillis(50));
        assertEquals(ArcadePortRules.BACON_PIG_INVINCIBILITY_MAX_MILLIS,
                ArcadePortRules.baconAbilityDurationMillis(100));
    }

    @Test void baconPigBackstabAddsTwoHundredAndFiftyPercentKnockback() {
        assertEquals(1.0D, ArcadePortRules.baconBrawlKnockbackMultiplier(false), 0.00001D);
        assertEquals(2.5D, ArcadePortRules.BACON_PIG_BACKSTAB_KNOCKBACK_MULTIPLIER, 0.00001D);
        assertEquals(3.75D, 1.5D * ArcadePortRules.baconBrawlKnockbackMultiplier(true), 0.00001D);
    }

    @Test void baconBrawlKnockbackMatchesDamageManagerLogAndMultiplierFormula() {
        double onePointFive = ArcadePortRules.baconBrawlEffectiveKnockback(1.0D, 1.5D);
        assertEquals(Math.log10(2.0D) * 1.5D, onePointFive, 0.00001D);
        assertEquals(0.2D + 0.6D * onePointFive * 0.8D,
                ArcadePortRules.baconBrawlVelocityStrength(1.0D, 1.5D, false), 0.00001D);
        assertEquals(Math.min(0.2D * onePointFive, 0.4D + 0.04D * onePointFive) + 0.2D,
                ArcadePortRules.baconBrawlVerticalVelocity(1.0D, 1.5D, false, true), 0.00001D);

        double bodySlamTarget = ArcadePortRules.baconBrawlEffectiveKnockback(6.0D, 1.5D * 2.0D);
        double bodySlamRecoil = ArcadePortRules.baconBrawlEffectiveKnockback(2.0D, 1.5D * 2.0D);
        assertEquals(Math.log10(6.0D) * 3.0D, bodySlamTarget, 0.00001D);
        assertEquals(Math.log10(2.0D) * 3.0D, bodySlamRecoil, 0.00001D);
        assertEquals(0.2D + 0.6D * bodySlamTarget * 0.8D,
                ArcadePortRules.baconBrawlVelocityStrength(6.0D, 3.0D, false), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.baconBrawlBodySlamLaunchY(0.5D, true), 0.00001D);
        assertEquals(0.4D, ArcadePortRules.baconBrawlBodySlamLaunchY(0.0D, true), 0.00001D);
        assertEquals(0.2D, ArcadePortRules.baconBrawlBodySlamLaunchY(0.0D, false), 0.00001D);
        assertTrue(ArcadePortRules.baconBrawlVelocityStrength(1.0D, 1.5D, true)
                < ArcadePortRules.baconBrawlVelocityStrength(1.0D, 1.5D, false));
    }

    @Test void sheepPigBackstabBonusOnlyAppliesFromBehind() {
        assertTrue(ArcadePortRules.isBaconBrawlBackstab(0.0D, 1.0D, 0.0D, -1.0D));
        assertTrue(ArcadePortRules.isBaconBrawlBackstab(1.0D, 0.0D, -1.0D, 0.0D));
        assertFalse(ArcadePortRules.isBaconBrawlBackstab(0.0D, 1.0D, 0.0D, 1.0D));
        assertFalse(ArcadePortRules.isBaconBrawlBackstab(1.0D, 0.0D, 1.0D, 0.0D));
        assertFalse(ArcadePortRules.isBaconBrawlBackstab(0.0D, 0.0D, 1.0D, 0.0D));
        assertFalse(ArcadePortRules.isBaconBrawlBackstab(Double.NaN, 1.0D, 0.0D, -1.0D));
    }

    @Test void baconBrawlMetersRewardDamageAndControlAbilityStrength() {
        assertEquals(100, ArcadePortRules.BACON_METER_MAX);
        assertEquals(50, ArcadePortRules.BACON_METER_MIN_ABILITY);
        assertEquals(5, ArcadePortRules.baconMeterGainForHit(false, 16));
        assertEquals(10, ArcadePortRules.baconMeterGainForHit(true, 16));
        assertEquals(10, ArcadePortRules.baconMeterGainForHit(false, 2));
        assertEquals(20, ArcadePortRules.baconMeterGainForHit(true, 2));
        assertEquals(6, ArcadePortRules.baconMeterGainForHit(false, 15));
        assertEquals(11, ArcadePortRules.baconMeterGainForHit(true, 15));
        assertEquals(25, ArcadePortRules.nextBaconMeter(20, false, 16));
        assertEquals(100, ArcadePortRules.nextBaconMeter(95, true, 2));
        assertFalse(ArcadePortRules.canUseBaconMeterAbility(49));
        assertTrue(ArcadePortRules.canUseBaconMeterAbility(50));
        assertEquals(0.75D, ArcadePortRules.baconBodySlamMeterMultiplier(50), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.baconBodySlamMeterMultiplier(100), 0.00001D);
        assertEquals(0L, ArcadePortRules.baconPigInvincibilityDurationMillis(49));
        assertEquals(2_750L, ArcadePortRules.baconPigInvincibilityDurationMillis(50));
        assertEquals(5_000L, ArcadePortRules.baconPigInvincibilityDurationMillis(100));
        assertEquals(1.25D, ArcadePortRules.baconBlastPowerMultiplier(50), 0.00001D);
        assertEquals(2.0D, ArcadePortRules.baconBlastPowerMultiplier(100), 0.00001D);
    }

    @Test void baconBrawlRewardsRhythmAndDiscouragesRapidClicking() {
        assertEquals(0.65D, ArcadePortRules.baconTimedHitKnockbackMultiplier(0L), 0.00001D);
        assertEquals(0.65D, ArcadePortRules.baconTimedHitKnockbackMultiplier(250L), 0.00001D);
        assertEquals(1.1D, ArcadePortRules.baconTimedHitKnockbackMultiplier(251L), 0.00001D);
        assertEquals(1.1D, ArcadePortRules.baconTimedHitKnockbackMultiplier(499L), 0.00001D);
        assertEquals(1.6D, ArcadePortRules.baconTimedHitKnockbackMultiplier(500L), 0.00001D);
        assertEquals(1.6D, ArcadePortRules.baconTimedHitKnockbackMultiplier(900L), 0.00001D);
        assertEquals(1.25D, ArcadePortRules.baconTimedHitKnockbackMultiplier(901L), 0.00001D);
        assertEquals(1.25D, ArcadePortRules.baconTimedHitKnockbackMultiplier(1_400L), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.baconTimedHitKnockbackMultiplier(1_401L), 0.00001D);
        assertEquals(1_000L, ArcadePortRules.baconTimingWindowMillis(500L, 3.0D));
        assertEquals(1_800L, ArcadePortRules.baconTimingWindowMillis(900L, 3.0D));
        assertTrue(ArcadePortRules.isBaconTimedCritical(1_400L, 3.0D));
    }


    @Test void baconPigInvincibilityIsMeterScaledAndHasNoCooldown() {
        assertEquals(500L, ArcadePortRules.BACON_PIG_INVINCIBILITY_MIN_MILLIS);
        assertEquals(5_000L, ArcadePortRules.BACON_PIG_INVINCIBILITY_MAX_MILLIS);
        assertEquals(0L, ArcadePortRules.baconPigInvincibilityDurationMillis(9));
        assertEquals(0L, ArcadePortRules.baconPigInvincibilityDurationMillis(15));
        assertEquals(2_750L, ArcadePortRules.baconPigInvincibilityDurationMillis(50));
        assertEquals(5_000L, ArcadePortRules.baconPigInvincibilityDurationMillis(100));
        assertTrue(ArcadePortRules.isBaconInvincibilityActive(1_000L, 1_001L));
        assertFalse(ArcadePortRules.isBaconInvincibilityActive(1_001L, 1_001L));
        assertFalse(ArcadePortRules.isBaconInvincibilityActive(1_002L, 1_001L));
        assertFalse(ArcadePortRules.isBaconInvincibilityActive(1_000L, 0L));
    }

    @Test void baconBrawlPacketAvatarHitsRequireReachLineOfSightAndLivingPlayers() {
        assertTrue(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, 9.0D, true));
        assertTrue(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, 12.25D, true));
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, 12.26D, true));
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, 4.0D, false));
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, false, true, false, 4.0D, true));
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, false, false, 4.0D, true));
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, true, 4.0D, true));
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(false, true, true, false, 4.0D, true));
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, Double.NaN, true));
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, Double.POSITIVE_INFINITY, true));
    }

    @Test void baconBrawlKitChangesAreDeferredUntilTheNextRound() {
        assertFalse(ArcadePortRules.shouldDeferBaconBrawlKitChange(false));
        assertTrue(ArcadePortRules.shouldDeferBaconBrawlKitChange(true));
    }

    @Test void baconBrawlAchievementKeysAreStableAndNamespaced() {
        java.util.UUID playerId = java.util.UUID.fromString("a3e1b6f0-9950-4de4-a7aa-572e4bd8db12");
        assertEquals("test-achievements.bacon-brawl.a3e1b6f0-9950-4de4-a7aa-572e4bd8db12.flash-fried",
                ArcadePortRules.baconBrawlAchievementConfigKey(playerId, "flash-fried"));
        assertEquals("test-achievements.bacon-brawl.a3e1b6f0-9950-4de4-a7aa-572e4bd8db12.pigs-in-blankets",
                ArcadePortRules.baconBrawlAchievementConfigKey(playerId, "pigs-in-blankets"));
        assertEquals("", ArcadePortRules.baconBrawlAchievementConfigKey(null, "flash-fried"));
        assertEquals("", ArcadePortRules.baconBrawlAchievementConfigKey(playerId, "raw pork"));
        assertEquals("", ArcadePortRules.baconBrawlAchievementConfigKey(playerId, "../flash-fried"));
    }

    @Test void kingOfBaconAchievementUsesUpdatedFifteenWinThreshold() {
        assertEquals(15, ArcadePortRules.BACON_BRAWL_KING_OF_BACON_WINS);
        assertFalse(ArcadePortRules.shouldAwardBaconKingOfBacon(14));
        assertTrue(ArcadePortRules.shouldAwardBaconKingOfBacon(15));
        assertTrue(ArcadePortRules.shouldAwardBaconKingOfBacon(16));
    }

    @Test void baconBrawlStatsMenuHasEveryExpectedAchievementBottle() {
        assertEquals(java.util.List.of("king-of-bacon", "pigs-in-blankets", "flash-fried",
                        "raw-pork", "three-for-one"),
                java.util.Arrays.stream(BaconBrawlAchievement.values())
                        .map(BaconBrawlAchievement::slug).toList());
        assertEquals("King of Bacon", BaconBrawlAchievement.KING_OF_BACON.displayName());
        assertEquals("Win 15 games of Bacon Brawl.", BaconBrawlAchievement.KING_OF_BACON.requirement());
    }

    @Test void baconBrawlUsesTheRequestedThreeAchievements() {
        assertEquals(60_000L, ArcadePortRules.BACON_FLASH_FRIED_MAX_WIN_MILLIS);
        assertTrue(ArcadePortRules.shouldAwardBaconFlashFried(0L));
        assertTrue(ArcadePortRules.shouldAwardBaconFlashFried(60_000L));
        assertFalse(ArcadePortRules.shouldAwardBaconFlashFried(60_001L));
        assertTrue(ArcadePortRules.shouldAwardBaconRawPork(false));
        assertFalse(ArcadePortRules.shouldAwardBaconRawPork(true));
        assertEquals(3, ArcadePortRules.BACON_THREE_FOR_ONE_BODY_SLAM_KILLS);
        assertFalse(ArcadePortRules.shouldAwardBaconThreeForOne(2));
        assertTrue(ArcadePortRules.shouldAwardBaconThreeForOne(3));
        assertTrue(ArcadePortRules.shouldAwardBaconThreeForOne(4));
        assertEquals(6, ArcadePortRules.BACON_BRAWL_PIGS_IN_BLANKETS_KILLS);
        assertFalse(ArcadePortRules.shouldAwardBaconPigsInBlankets(5));
        assertTrue(ArcadePortRules.shouldAwardBaconPigsInBlankets(6));
        assertTrue(ArcadePortRules.shouldAwardBaconPigsInBlankets(7));
    }

    @Test void baconBlastResolvesTheFirstCollisionAlongItsSweptPath() {
        assertTrue(ArcadePortRules.isBaconBlastPlayerBeforeBlock(0.2D, 0.8D, 2.0D));
        assertTrue(ArcadePortRules.isBaconBlastPlayerBeforeBlock(0.6D, 0.8D, 2.0D));
        assertFalse(ArcadePortRules.isBaconBlastPlayerBeforeBlock(0.8D, 0.5D, 2.0D));
        // A half-block segment is within the projectile radius of the player's collision volume.
        assertTrue(ArcadePortRules.isBaconBlastPlayerBeforeBlock(0.8D, 0.1D, 0.5D));
        assertFalse(ArcadePortRules.isBaconBlastPlayerBeforeBlock(0.8D, 0.1D, 2.0D));
        assertTrue(ArcadePortRules.isBaconBlastPlayerBeforeBlock(0.8D, -1.0D, 2.0D));
        assertFalse(ArcadePortRules.isBaconBlastPlayerBeforeBlock(Double.NaN, 0.5D, 2.0D));
        assertFalse(ArcadePortRules.isBaconBlastPlayerBeforeBlock(0.5D, 0.5D, Double.NaN));
    }

    @Test void baconBlastThrownItemUsesSweptHitRadiusAndSafeExpiry() {
        assertTrue(ArcadePortRules.isBaconBlastNearPlayer(0.0D));
        assertTrue(ArcadePortRules.isBaconBlastNearPlayer(0.25D));
        assertFalse(ArcadePortRules.isBaconBlastNearPlayer(0.26D));
        assertFalse(ArcadePortRules.isBaconBlastNearPlayer(Double.NaN));
        assertFalse(ArcadePortRules.isBaconBlastNearPlayer(-1.0D));

        assertTrue(ArcadePortRules.isBaconBlastBlockImpact(true, false, false, false));
        assertTrue(ArcadePortRules.isBaconBlastBlockImpact(false, true, false, false));
        assertTrue(ArcadePortRules.isBaconBlastBlockImpact(false, false, true, false));
        assertTrue(ArcadePortRules.isBaconBlastBlockImpact(false, false, false, true),
                "a stationary pork item touching a side wall must detonate");
        assertFalse(ArcadePortRules.isBaconBlastBlockImpact(false, false, false, false),
                "flying past the arena floor does not itself detonate the item");

        assertFalse(ArcadePortRules.shouldExpireBaconBlast(5_999L, 1_000L, false));
        assertTrue(ArcadePortRules.shouldExpireBaconBlast(6_000L, 1_000L, false));
        assertTrue(ArcadePortRules.shouldExpireBaconBlast(1_100L, 1_000L, true));
        assertTrue(ArcadePortRules.shouldExpireBaconBlast(999L, 1_000L, false));
        assertTrue(ArcadePortRules.shouldExpireBaconBlast(5_000L, 0L, false));
    }

    @Test void baconBlastImpactSuppressesExplosionHealthDamageOnlyDuringItsOwnImpact() {
        var explosion = org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
        var blockExplosion = org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
        var fall = org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL;
        var fire = org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK;
        assertTrue(ArcadePortRules.shouldSuppressBaconBlastHealthDamage(true, true, explosion));
        assertTrue(ArcadePortRules.shouldSuppressBaconBlastHealthDamage(true, true, blockExplosion));
        assertFalse(ArcadePortRules.shouldSuppressBaconBlastHealthDamage(true, false, explosion));
        assertFalse(ArcadePortRules.shouldSuppressBaconBlastHealthDamage(false, true, explosion));
        assertFalse(ArcadePortRules.shouldSuppressBaconBlastHealthDamage(true, true, fall));
        assertFalse(ArcadePortRules.shouldSuppressBaconBlastHealthDamage(true, true, fire));
        assertFalse(ArcadePortRules.shouldSuppressBaconBlastHealthDamage(true, true, null));
    }

    @Test void baconBlastNeverBreaksSourceArenaBlocks() {
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.STONE, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.OAK_PLANKS, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.BEDROCK, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.WATER, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(null, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.STONE, Double.NaN));
    }

    @Test void baconBrawlRecognisesSourceArenaHazardKillReasons() {
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("fell from the arena"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("FALL"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("was lost to the water"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("LAVA"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("hot_floor"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("Magma Block"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("void"));
        assertFalse(ArcadePortRules.isBaconArenaKnockoutReason("starved"));
        assertFalse(ArcadePortRules.isBaconArenaKnockoutReason("melee"));
        assertFalse(ArcadePortRules.isBaconArenaKnockoutReason(null));
    }

    @Test void baconBrawlCreditsRecentKnockbackForFallAndWaterKillsOnly() {
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 5_001L, true));
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 10_000L, true));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(10_001L, 5_000L, true));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 9_000L, false),
                "starvation and Body Slam recoil do not receive opponent kill credit");
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(9_000L, 10_000L, true));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 0L, true));
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 5_001L,
                ArcadePortRules.isBaconArenaKnockoutReason("lava")));
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 5_001L,
                ArcadePortRules.isBaconArenaKnockoutReason("hot_floor")));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 5_001L,
                ArcadePortRules.isBaconArenaKnockoutReason("starved")));
    }

    @Test void baconBrawlSidebarUsesPlayerCountInsteadOfMergingNames() {
        List<String> fifteen = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(index -> "Player" + index).toList();
        List<String> sixteen = java.util.stream.IntStream.rangeClosed(1, 16)
                .mapToObj(index -> "Player" + index).toList();

        assertEquals(fifteen, ArcadePortRules.baconBrawlSidebarLines(fifteen));
        List<String> lines = ArcadePortRules.baconBrawlSidebarLines(sixteen);
        assertEquals(15, lines.size());
        assertEquals("Player14", lines.get(13));
        assertEquals("16 players remaining", lines.get(14));
        assertFalse(lines.contains("Player15, Player16"));
    }

    @Test void baconBrawlKillCreditIncludesTestPlayerParticipantsAndExcludesSelfKnockouts() {
        java.util.UUID testPlayerVictim = java.util.UUID.randomUUID();
        java.util.UUID humanKiller = java.util.UUID.randomUUID();
        assertTrue(ArcadePortRules.shouldCreditBaconKill(testPlayerVictim, humanKiller, true));
        assertFalse(ArcadePortRules.shouldCreditBaconKill(testPlayerVictim, humanKiller, false));
        assertFalse(ArcadePortRules.shouldCreditBaconKill(testPlayerVictim, testPlayerVictim, true));
    }

    @Test void baconBrawlTracksKillsWithoutAddingKnockoutsToStats() {
        assertFalse(ArcadePortRules.shouldRecordKnockoutsStat(ArcadePortGame.BACON_BRAWL));
        assertTrue(ArcadePortRules.shouldRecordKnockoutsStat(ArcadePortGame.BOMB_LOBBERS));
        assertFalse(ArcadePortRules.shouldRecordKnockoutsStat(null));
    }

    @Test void baconBrawlRoundUsesLastPigStandingInsteadOfKillScoreTimeout() {
        assertFalse(ArcadePortRules.BACON_BRAWL_HAS_ROUND_TIMER);
        assertTrue(ArcadePortRules.shouldFinishBaconBrawl(1));
        assertFalse(ArcadePortRules.shouldFinishBaconBrawl(2));
        assertTrue(ArcadePortRules.shouldFinishBaconBrawl(0));
    }

    @Test void baconBrawlKnockbackEscalatesOnShorteningSourceIntervals() {
        assertEquals(50_000L, ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS);
        assertEquals(50_000L, ArcadePortRules.nextBaconKnockbackInterval(0, 8));
        assertEquals(20_000L, ArcadePortRules.nextBaconKnockbackInterval(0, 2),
                "two players at round start use the accelerated escalation cadence");
        assertEquals(45_000L, ArcadePortRules.nextBaconKnockbackInterval(1, 3));
        assertEquals(20_000L, ArcadePortRules.nextBaconKnockbackInterval(1, 2));
        assertEquals(20_000L, ArcadePortRules.nextBaconKnockbackInterval(1, 1));
        assertEquals(40_000L, ArcadePortRules.nextBaconKnockbackInterval(2, 8));
        assertEquals(5_000L, ArcadePortRules.nextBaconKnockbackInterval(9, 8));
        assertEquals(0L, ArcadePortRules.nextBaconKnockbackInterval(10, 8));
        assertEquals(10, ArcadePortRules.BACON_BRAWL_MAX_KNOCKBACK_ESCALATIONS);
        assertEquals(1.8D, ArcadePortRules.nextBaconKnockback(1.5D), 0.00001D);
        assertEquals(2.16D, ArcadePortRules.nextBaconKnockback(1.8D), 0.00001D);
    }

    @Test void runnerSourceKitsHaveDistinctCooldownsAndFrostBallRules() {
        assertEquals("leaper", ArcadePortRules.normalizeRunnerKit("Leaper"));
        assertEquals("archer", ArcadePortRules.normalizeRunnerKit("runner-archer"));
        assertEquals("frosty", ArcadePortRules.normalizeRunnerKit("Frosty"));
        assertNull(ArcadePortRules.normalizeRunnerKit("unknown"));
        assertEquals(8_000L, ArcadePortRules.RUNNER_LEAP_COOLDOWN_MILLIS);
        assertEquals(6_000L, ArcadePortRules.RUNNER_QUICKSHOT_COOLDOWN_MILLIS);
        assertEquals(500L, ArcadePortRules.RUNNER_FROSTBALL_INTERVAL_MILLIS);
        assertEquals(16, ArcadePortRules.RUNNER_MAX_FROSTBALLS);
        assertEquals(40, ArcadePortRules.RUNNER_FROSTBALL_SLOW_TICKS);
        assertEquals(2, ArcadePortRules.RUNNER_FROSTBALL_SLOW_AMPLIFIER);
    }

    @Test void runnerFloorBlocksFallAfterSixHundredMilliseconds() {
        assertEquals(600L, ArcadePortRules.RUNNER_BLOCK_DECAY_MILLIS);
        assertEquals(1_200L, ArcadePortRules.RUNNER_BLOCK_LIFETIME_MILLIS);
        assertFalse(ArcadePortRules.shouldDropRunnerBlock(599L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(600L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(1_200L));
    }

    @Test void runnerOnlyDestabilisesExposedSolidNonLiquidBlocksOnce() {
        assertTrue(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(false, false, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, true, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, true, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, false, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, true, true));
    }

    @Test void runnerFallingBlocksCannotHitPlayersStandingAboveThem() {
        assertTrue(ArcadePortRules.shouldRunnerFallingBlockHit(64.0D, 64.0D));
        assertTrue(ArcadePortRules.shouldRunnerFallingBlockHit(64.5D, 64.0D));
        assertFalse(ArcadePortRules.shouldRunnerFallingBlockHit(64.5001D, 64.0D));
        assertFalse(ArcadePortRules.shouldRunnerFallingBlockHit(70.0D, 64.0D));
    }

    @Test void sourceWaterDamageMatchesTheOriginalArcadeGameSettings() {
        assertEquals(5, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.BOMB_LOBBERS, "Mighty Lava Arena"));
        assertEquals(4, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.DRAGONS, "Dragons"));
        assertEquals(4, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.RUNNER, "Runner"));
        assertEquals(4, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.SPLEEF, "Spleef"));
        assertEquals(2, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.DRAGON_ESCAPE, "Pirate"));
        assertEquals(0, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.DRAGON_ESCAPE, "Hell"));
        assertEquals(0, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.BACON_BRAWL, "Arena"));
    }

    @Test void dragonEscapePirateWaterDamageUsesTheSourceHalfSecondRecharge() {
        assertEquals(500L, ArcadePortRules.DRAGON_ESCAPE_WATER_DAMAGE_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.DRAGON_ESCAPE, "Pirate"));
        assertEquals(0, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.DRAGON_ESCAPE, "Hell"));
    }

    @Test void dragonEscapeHellMapStartsAtNight() {
        assertEquals(16_000L, ArcadePortRules.dragonEscapeWorldTime("Dragon Escape Hell"));
        assertEquals(12_000L, ArcadePortRules.dragonEscapeWorldTime("Dragon Escape Pirate"));
        assertEquals(12_000L, ArcadePortRules.dragonEscapeWorldTime("Standard"));
        assertEquals(12_000L, ArcadePortRules.dragonEscapeWorldTime(null));
    }

    @Test void sourceWorldTimeHonoursPerGameOverridesAndBaseDefault() {
        assertEquals(6_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.BOMB_LOBBERS, "Mighty Lava Arena"));
        assertEquals(18_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.BOMB_LOBBERS, "Intergalactic"));
        assertEquals(2_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.SHEEP_QUEST, "Sheep Quest"));
        assertEquals(16_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.DRAGON_ESCAPE, "Hell"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.DRAGON_ESCAPE, "Pirate"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.BACON_BRAWL, "Bacon Brawl"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.SPLEEF, "Spleef"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.TURF_WARS, "Turf Wars"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(null, "Unknown"));
    }

    @Test void dragonEscapeKitsExposeSourceKitChoicesAndLeapCharges() {
        assertEquals("jumper", ArcadePortRules.normalizeDragonEscapeKit("Jumper"));
        assertEquals("disruptor", ArcadePortRules.normalizeDragonEscapeKit("DISRUPTOR"));
        assertEquals("warper", ArcadePortRules.normalizeDragonEscapeKit("Warper"));
        assertEquals("digger", ArcadePortRules.normalizeDragonEscapeKit("Digger"));
        assertNull(ArcadePortRules.normalizeDragonEscapeKit("speedster"));
        assertEquals(4, ArcadePortRules.dragonEscapeLeapUses("jumper"));
        assertEquals(3, ArcadePortRules.dragonEscapeLeapUses("disruptor"));
        assertEquals(2, ArcadePortRules.dragonEscapeLeapUses("warper"));
        assertEquals(0, ArcadePortRules.dragonEscapeLeapUses("digger"));
        assertEquals(6, ArcadePortRules.DRAGON_ESCAPE_DIGGER_MAX_USES);
        assertEquals(2, ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_MAX_TNT);
        assertEquals(8_000L, ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS);
        assertEquals(8_000L, ArcadePortRules.DRAGON_ESCAPE_LEAP_RECHARGE_MILLIS);
        assertEquals(40, ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_ARM_TICKS);
        assertEquals(2.0D, ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TRIGGER_RADIUS, 0.00001D);
        assertEquals(0.5D, ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_LAUNCH_Y, 0.00001D);
    }

    @Test void dragonEscapeSourceMapBoundsUseInclusiveWorldDataLimits() {
        int minX = -100, maxX = 100, minZ = -80, maxZ = 80, minY = 20, maxY = 130;
        assertTrue(ArcadePortRules.isOutsideDragonEscapeMapBounds(-100.0D, 64.0D, 0.0D,
                minX, maxX, minZ, maxZ, true, minY, maxY));
        assertTrue(ArcadePortRules.isOutsideDragonEscapeMapBounds(101.0D, 64.0D, 0.0D,
                minX, maxX, minZ, maxZ, true, minY, maxY));
        assertTrue(ArcadePortRules.isOutsideDragonEscapeMapBounds(0.0D, 64.0D, -80.0D,
                minX, maxX, minZ, maxZ, true, minY, maxY));
        assertTrue(ArcadePortRules.isOutsideDragonEscapeMapBounds(0.0D, 131.0D, 0.0D,
                minX, maxX, minZ, maxZ, true, minY, maxY));
        assertFalse(ArcadePortRules.isOutsideDragonEscapeMapBounds(-99.999D, 64.0D, 80.999D,
                minX, maxX, minZ, maxZ, true, minY, maxY));
        assertFalse(ArcadePortRules.isOutsideDragonEscapeMapBounds(0.0D, -500.0D, 0.0D,
                minX, maxX, minZ, maxZ, false, minY, maxY));
    }

    @Test void sourceMapFallThresholdUsesVerticalBoundsInsteadOfFixedY58() {
        assertEquals(3.0D, ArcadePortRules.sourceMapVoidThresholdY(true, 4), 0.00001D);
        assertEquals(13.0D, ArcadePortRules.sourceMapVoidThresholdY(true, 14), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.sourceMapVoidThresholdY(true, 0), 0.00001D);
        assertEquals(58.0D, ArcadePortRules.sourceMapVoidThresholdY(false, 4), 0.00001D);
        assertTrue(13.0D > ArcadePortRules.sourceMapVoidThresholdY(true, 4),
                "Mighty Lava Arena Bacon Brawl starts around Y=13 and must not be eliminated on spawn");
    }

    @Test void dragonEscapeVoidThresholdFollowsLowAltitudeSourceMaps() {
        assertEquals(58.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Skylands"), 0.00001D);
        assertEquals(24.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Through Hell"), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Pirate Bay"), 0.00001D);
        assertEquals(58.0D, ArcadePortRules.dragonEscapeVoidThresholdY(null), 0.00001D);
    }

    @Test void dragonEscapeDestructionThrottleChecksNewAreaAfterMovement() {
        assertTrue(ArcadePortRules.shouldProcessDragonEscapeDestruction(0.0D, false));
        assertFalse(ArcadePortRules.shouldProcessDragonEscapeDestruction(0.0D, true));
        assertFalse(ArcadePortRules.shouldProcessDragonEscapeDestruction(0.24D, true));
        assertTrue(ArcadePortRules.shouldProcessDragonEscapeDestruction(0.25D, true));
        assertTrue(ArcadePortRules.shouldProcessDragonEscapeDestruction(Double.NaN, true));
    }

    @Test void dragonEscapeDouglasDestructionUsesBlockCentreWithinTenBlocks() {
        assertTrue(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(0.0D));
        assertTrue(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(100.0D));
        assertFalse(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(100.0001D));
        assertFalse(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(Double.NaN));
        assertFalse(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(-1.0D));
    }

    @Test void dragonEscapeFallbackCrumblesOnlyItsGeneratedCourse() {
        assertTrue(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(false, 0));
        assertTrue(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(false, 82));
        assertFalse(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(false, -19));
        assertFalse(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(false, 83));
        assertFalse(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(true, 0));
        assertFalse(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(true, 82));
    }

    @Test void dragonEscapeRouteProgressNeverMovesBackAtCourseBends() {
        assertEquals(0, ArcadePortRules.monotonicDragonEscapeProgress(0, 0, 4));
        assertEquals(2, ArcadePortRules.monotonicDragonEscapeProgress(2, 1, 4));
        assertEquals(3, ArcadePortRules.monotonicDragonEscapeProgress(2, 99, 4));
        assertEquals(0, ArcadePortRules.monotonicDragonEscapeProgress(-1, -99, 4));
        assertEquals(-1, ArcadePortRules.monotonicDragonEscapeProgress(0, 1, 0));
    }

    @Test void dragonEscapeRaceTimeGapUsesCourseDistanceAndSafeSpeed() {
        assertEquals(2_000L,
                ArcadePortRules.dragonEscapeCourseTimeGapMillis(25.0D, 15.0D, 5.0D));
        assertEquals(0L,
                ArcadePortRules.dragonEscapeCourseTimeGapMillis(10.0D, 15.0D, 5.0D));
        assertEquals(0L,
                ArcadePortRules.dragonEscapeCourseTimeGapMillis(Double.NaN, 15.0D, 5.0D));
        assertEquals(0L,
                ArcadePortRules.dragonEscapeCourseTimeGapMillis(25.0D, 15.0D, 0.0D));
        assertEquals(3.0D, ArcadePortRules.DRAGON_ESCAPE_FINISH_RADIUS, 0.00001D);
        assertEquals(5.0D, ArcadePortRules.DRAGON_ESCAPE_RACE_GAP_ESTIMATED_SPEED, 0.00001D);
    }

    @Test void dragonEscapeRankingUsesCheckpointScoreThenDistanceToNextMarker() {
        assertEquals(-2.0D, ArcadePortRules.dragonEscapeRankScore(0, 2.0D), 0.00001D);
        assertEquals(9_997.0D, ArcadePortRules.dragonEscapeRankScore(1, 3.0D), 0.00001D);
        assertTrue(ArcadePortRules.dragonEscapeRankScore(2, 9_999.0D)
                > ArcadePortRules.dragonEscapeRankScore(1, 0.0D));
        assertEquals(Double.NEGATIVE_INFINITY,
                ArcadePortRules.dragonEscapeRankScore(-1, 0.0D));
        assertEquals(Double.NEGATIVE_INFINITY,
                ArcadePortRules.dragonEscapeRankScore(0, Double.NaN));
    }

    @Test void dragonEscapeCheckpointProgressCannotJumpAheadAtNearbySwitchbacks() {
        assertFalse(ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(0.0D, 1.0D, 1.0D));
        assertFalse(ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(0.5D, 0.5D, 1.0D));
        assertTrue(ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(0.9D, 0.1D, 1.0D));
        assertTrue(ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(5.0D, 2.5D, 10.0D));
        assertTrue(ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(5.0D, 3.5D, 10.0D),
                "Leaper must not get stuck after passing a waypoint between 500ms score samples");
        assertFalse(ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(3.5D, 5.0D, 10.0D));
        assertFalse(ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(3.5D, 3.5D, 10.0D));
        assertFalse(ArcadePortRules.shouldAdvanceDragonEscapeWaypoint(Double.NaN, 0.0D, 1.0D));
    }

    @Test void dragonEscapeRemainsAnElapsedRaceUntilFinishOrNoRacersRemain() {
        assertFalse(ArcadePortRules.DRAGON_ESCAPE_HAS_ROUND_TIMER);
        assertFalse(ArcadePortRules.shouldFinishWhenNobodyIsAlive(1, 0));
        assertTrue(ArcadePortRules.shouldFinishWhenNobodyIsAlive(0, 0));
    }

    @Test void dragonEscapeDiggerHonoursOpeningLockoutProximityAndSourceRestoreWindow() {
        assertTrue(ArcadePortRules.isDragonEscapeDiggerOpeningLocked(0L));
        assertTrue(ArcadePortRules.isDragonEscapeDiggerOpeningLocked(9_999L));
        assertFalse(ArcadePortRules.isDragonEscapeDiggerOpeningLocked(10_000L));
        assertTrue(ArcadePortRules.shouldPreventDragonEscapeDigger(1.49D, 20.0D));
        assertTrue(ArcadePortRules.shouldPreventDragonEscapeDigger(20.0D, 1.49D));
        assertFalse(ArcadePortRules.shouldPreventDragonEscapeDigger(1.5D, 20.0D));
        assertEquals(100L, ArcadePortRules.DRAGON_ESCAPE_DIGGER_RECHARGE_MILLIS);
        assertTrue(ArcadePortRules.shouldAllowDragonEscapeDigger(1_000L, 1_000L));
        assertFalse(ArcadePortRules.shouldAllowDragonEscapeDigger(999L, 1_000L));
        assertEquals(2_400, ArcadePortRules.DRAGON_ESCAPE_DIGGER_BLOCK_RESTORE_TICKS);
    }

    @Test void dragonEscapeWarperUsesOneSecondShortcutGraceWithoutPunishingCloseSwitchbacks() {
        assertFalse(ArcadePortRules.shouldRejectDragonEscapeShortcut(2, 5, 2.0D, 0.0D, 2_000L, -1L));
        assertFalse(ArcadePortRules.shouldRejectDragonEscapeShortcut(2, 5, 10.0D, 0.0D, 1_999L, 1_000L));
        assertTrue(ArcadePortRules.shouldRejectDragonEscapeShortcut(2, 5, 10.0D, 0.0D, 2_000L, 1_000L));
        assertFalse(ArcadePortRules.shouldRejectDragonEscapeShortcut(2, 4, 10.0D, 0.0D, 2_000L, -1L));
        assertTrue(ArcadePortRules.isDragonEscapeWarpGraceActive(1_999L, 1_000L));
        assertFalse(ArcadePortRules.isDragonEscapeWarpGraceActive(2_000L, 1_000L));
    }

    @Test void dragonEscapeKitsCannotBeChangedMidRace() {
        assertFalse(ArcadePortRules.shouldAllowDragonEscapeKitChange(true));
        assertTrue(ArcadePortRules.shouldAllowDragonEscapeKitChange(false));
    }

    @Test void dragonEscapeRacerScoresUseTheSourceFiveHundredMillisecondCadence() {
        assertTrue(ArcadePortRules.shouldRunDragonEscapeScoreUpdate(1_000L, 0L));
        assertFalse(ArcadePortRules.shouldRunDragonEscapeScoreUpdate(1_499L, 1_500L));
        assertTrue(ArcadePortRules.shouldRunDragonEscapeScoreUpdate(1_500L, 1_500L));
        assertEquals(2_000L, ArcadePortRules.nextDragonEscapeScoreUpdateAt(1_500L));
        assertEquals(500L, ArcadePortRules.DRAGON_ESCAPE_SCORE_UPDATE_MILLIS);
    }

    @Test void dragonEscapeLeapUsesSourceVelocityClampBeforeGroundBoostAndSourceItemCharges() {
        assertEquals(0.4D, ArcadePortRules.dragonEscapeLeapVerticalVelocity(0.0D, true), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.dragonEscapeLeapVerticalVelocity(0.9D, false), 0.00001D);
        assertEquals(1.2D, ArcadePortRules.dragonEscapeLeapVerticalVelocity(0.9D, true), 0.00001D);
        assertEquals(-0.8D, ArcadePortRules.dragonEscapeLeapVerticalVelocity(-1.0D, false), 0.00001D);
        assertEquals(4, ArcadePortRules.dragonEscapeLeapUses("jumper"));
        assertEquals(3, ArcadePortRules.dragonEscapeLeapUses("disruptor"));
        assertEquals(2, ArcadePortRules.dragonEscapeLeapUses("warper"));
    }

    @Test void dragonEscapeInitialWaypointRankUsesObservedWaypointWithoutFalseShortcut() {
        assertEquals(4, ArcadePortRules.dragonEscapeObservedWaypointBaseline(0, 4, false));
        assertEquals(5, ArcadePortRules.dragonEscapeObservedWaypointBaseline(4, 5, true));
        assertEquals(4, ArcadePortRules.dragonEscapeObservedWaypointBaseline(4, 2, true));
    }

    @Test void dragonEscapeDisruptorRechargesAtTheSourceEightSecondRate() {
        assertEquals(8_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(0L));
        assertEquals(23_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(15_000L));
        assertEquals(ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS,
                ArcadePortRules.dragonEscapeNextTntChargeAt(-1_000L) - (-1_000L));
    }

    @Test void dragonEscapeDisruptorMineTriggersOnlyOnArmedOpponentPickup() {
        assertFalse(ArcadePortRules.shouldTriggerDragonEscapeDisruptor(39, 0.5D, false));
        assertFalse(ArcadePortRules.shouldTriggerDragonEscapeDisruptor(40, 2.01D, false));
        assertFalse(ArcadePortRules.shouldTriggerDragonEscapeDisruptor(80, 1.0D, true));
        assertFalse(ArcadePortRules.shouldTriggerDragonEscapeDisruptor(80, Double.NaN, false));
        assertTrue(ArcadePortRules.shouldTriggerDragonEscapeDisruptor(40, 2.0D, false));
    }

    @Test void dragonEscapeDouglasStartsByTargetingFirstAuthoredCheckpointAfterRedOrigin() {
        assertEquals(1, ArcadePortRules.dragonEscapeInitialTargetIndex(2));
        assertEquals(1, ArcadePortRules.dragonEscapeInitialTargetIndex(14));
        assertEquals(-1, ArcadePortRules.dragonEscapeInitialTargetIndex(1));
        assertEquals(-1, ArcadePortRules.dragonEscapeInitialTargetIndex(0));
    }

    @Test void dragonEscapeGreenDataMarkerControlsDouglasSpeedMultiplier() {
        assertEquals(1.0D, ArcadePortRules.dragonEscapeSpeedMultiplier(100.0D), 0.00001D);
        assertEquals(1.25D, ArcadePortRules.dragonEscapeSpeedMultiplier(125.0D), 0.00001D);
        assertEquals(0.75D, ArcadePortRules.dragonEscapeSpeedMultiplier(75.0D), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.dragonEscapeSpeedMultiplier(Double.NaN), 0.00001D);
    }

    @Test void dragonEscapePersonalBestKeysAndRaceTimesAreStable() {
        String key = ArcadePortRules.dragonEscapeMapStorageKey("source-maps/Dragon Escape/Map A.zip");
        assertFalse(key.isBlank());
        assertFalse(key.contains("/"));
        assertFalse(key.contains("."));
        assertEquals(key, ArcadePortRules.dragonEscapeMapStorageKey("source-maps/Dragon Escape/Map A.zip"));
        assertEquals("", ArcadePortRules.dragonEscapeMapStorageKey(null));
        assertEquals("0:00.000", ArcadePortRules.formatDragonEscapeTime(0L));
        assertEquals("1:02.345", ArcadePortRules.formatDragonEscapeTime(62_345L));
        assertEquals("--:--.---", ArcadePortRules.formatDragonEscapeTime(-1L));
        assertEquals("+0:03.100", ArcadePortRules.formatDragonEscapeDelta(3_100L));
        assertEquals("-0:01.250", ArcadePortRules.formatDragonEscapeDelta(-1_250L));
        assertEquals("0:00.000", ArcadePortRules.formatDragonEscapeDelta(0L));
    }

    @Test void dragonEscapePersonalBestSplitsFollowEveryAuthoredRouteLeg() {
        assertEquals(0, ArcadePortRules.dragonEscapePbSegmentStartIndex(0, 4));
        assertEquals(1, ArcadePortRules.dragonEscapePbSegmentStartIndex(1, 4));
        assertEquals(2, ArcadePortRules.dragonEscapePbSegmentStartIndex(2, 4));
        assertEquals(3, ArcadePortRules.dragonEscapePbSegmentStartIndex(3, 4),
                "The final checkpoint must use its own finish-radius split");
        assertEquals(0, ArcadePortRules.dragonEscapePbSegmentStartIndex(0, 1));
        assertEquals(-1, ArcadePortRules.dragonEscapePbSegmentStartIndex(0, 0));

        // Four authored waypoints produce four timed legs and five split points:
        // start, three subsequent checkpoint crossings, and finish.
        assertEquals(0L, ArcadePortRules.dragonEscapeDefaultSplitMillis(90_000L, 0, 4));
        assertEquals(22_500L, ArcadePortRules.dragonEscapeDefaultSplitMillis(90_000L, 1, 4));
        assertEquals(45_000L, ArcadePortRules.dragonEscapeDefaultSplitMillis(90_000L, 2, 4));
        assertEquals(67_500L, ArcadePortRules.dragonEscapeDefaultSplitMillis(90_000L, 3, 4));
        assertEquals(90_000L, ArcadePortRules.dragonEscapeDefaultSplitMillis(90_000L, 4, 4));
        assertEquals(0L, ArcadePortRules.dragonEscapeDefaultSplitMillis(90_000L, 0, 0));
        assertEquals(0L, ArcadePortRules.dragonEscapeDefaultSplitMillis(90_000L, 0, 1));
        assertEquals(90_000L, ArcadePortRules.dragonEscapeDefaultSplitMillis(90_000L, 1, 1));

        assertEquals(27_500L, ArcadePortRules.interpolateDragonEscapeSplitMillis(20_000L, 50_000L, 0.25D));
        assertEquals(20_000L, ArcadePortRules.interpolateDragonEscapeSplitMillis(20_000L, 50_000L, -1.0D));
        assertEquals(50_000L, ArcadePortRules.interpolateDragonEscapeSplitMillis(20_000L, 50_000L, 2.0D));
        assertEquals(20_000L, ArcadePortRules.interpolateDragonEscapeSplitMillis(20_000L, 10_000L, 0.5D));
        assertEquals(20_000L, ArcadePortRules.interpolateDragonEscapeSplitMillis(20_000L, 50_000L, Double.NaN));

        assertEquals(5.0D, ArcadePortRules.interpolateDragonEscapeCoordinate(0.0D, 10.0D, 0.5D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.interpolateDragonEscapeCoordinate(0.0D, 10.0D, -1.0D), 0.00001D);
        assertEquals(10.0D, ArcadePortRules.interpolateDragonEscapeCoordinate(0.0D, 10.0D, 2.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.interpolateDragonEscapeCoordinate(0.0D, 10.0D, Double.NaN), 0.00001D);
    }

    @Test void dragonEscapeRouteOnlyDeduplicatesCoincidentWaypoints() {
        assertTrue(ArcadePortRules.isDuplicateDragonEscapeWaypoint(0.0D));
        assertTrue(ArcadePortRules.isDuplicateDragonEscapeWaypoint(0.5D));
        assertFalse(ArcadePortRules.isDuplicateDragonEscapeWaypoint(0.75D));
        assertFalse(ArcadePortRules.isDuplicateDragonEscapeWaypoint(2.0D));
        assertFalse(ArcadePortRules.isDuplicateDragonEscapeWaypoint(Double.NaN));
    }

    @Test void dragonEscapeUsesARealChaseProgressAndCourseFinishProximity() {
        assertEquals(0.2D, ArcadePortRules.DRAGON_ESCAPE_CHASE_SPEED, 0.00001D);
        assertEquals(10.0D, ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS, 0.00001D);
        assertTrue(ArcadePortRules.shouldDragonEscapePlayerBeCaught(25.01D, 25.0D));
        assertFalse(ArcadePortRules.shouldDragonEscapePlayerBeCaught(24.99D, 25.0D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(10.0D, 64.0D, 77.5D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 70.0D, 77.5D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 74.4D));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 75.0D));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 77.5D));
    }

    @Test void spleefProjectileRadiusScalesWithArrowSpeed() {
        assertEquals(1, ArcadePortRules.spleefProjectileClearRadius(false, 8.0D));
        assertEquals(1, ArcadePortRules.spleefProjectileClearRadius(true, 0.0D));
        assertEquals(2, ArcadePortRules.spleefProjectileClearRadius(true, 1.6D));
        assertEquals(3, ArcadePortRules.spleefProjectileClearRadius(true, 4.0D));
        assertEquals(3, ArcadePortRules.spleefProjectileClearRadius(true, 100.0D));
    }

    @Test void evolutionStagesAdvanceThroughFiveFormsAndPersistOnDeath() {
        assertEquals(5, ArcadePortRules.EVOLUTION_WIN_LEVEL);
        assertEquals(1, ArcadePortRules.nextEvolutionLevel(0, true));
        assertEquals(5, ArcadePortRules.nextEvolutionLevel(4, true));
        assertEquals(5, ArcadePortRules.nextEvolutionLevel(5, true));
        assertEquals(2, ArcadePortRules.nextEvolutionLevel(2, false));
        assertEquals(0, ArcadePortRules.nextEvolutionLevel(0, false));
        assertEquals(0, ArcadePortRules.nextEvolutionLevel(-1, false));
        assertFalse(ArcadePortRules.isEvolutionWinnerLevel(4));
        assertTrue(ArcadePortRules.isEvolutionWinnerLevel(5));
        assertEquals(80L, ArcadePortRules.EVOLUTION_RESPAWN_TICKS);
        assertEquals(44L, ArcadePortRules.EVOLUTION_FORM_CHANGE_TICKS);
        assertEquals(140L, ArcadePortRules.EVOLUTION_TOTAL_ANIMATION_TICKS);
    }

    @Test void evolutionUsesFiveSourceFormsAndTheirMeleeDamage() {
        assertEquals(40.0D, ArcadePortRules.evolutionMaxHealth(0));
        assertEquals(36.0D, ArcadePortRules.evolutionMaxHealth(1));
        assertEquals(28.0D, ArcadePortRules.evolutionMaxHealth(2));
        assertEquals(22.0D, ArcadePortRules.evolutionMaxHealth(3));
        assertEquals(16.0D, ArcadePortRules.evolutionMaxHealth(4));
        assertEquals(16.0D, ArcadePortRules.evolutionMaxHealth(5));
        assertEquals(6.0D, ArcadePortRules.evolutionMeleeDamage(0));
        assertEquals(5.0D, ArcadePortRules.evolutionMeleeDamage(1));
        assertEquals(4.0D, ArcadePortRules.evolutionMeleeDamage(2));
        assertEquals(4.0D, ArcadePortRules.evolutionMeleeDamage(3));
        assertEquals(3.0D, ArcadePortRules.evolutionMeleeDamage(4));
        assertEquals(3.0D, ArcadePortRules.evolutionMeleeDamage(5));
    }

    @Test void evolutionFallDamageMatchesTheFiveFormDesign() {
        assertEquals(1.0D, ArcadePortRules.evolutionFallDamageMultiplier(0), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.evolutionFallDamageMultiplier(1), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.evolutionFallDamageMultiplier(2), 0.00001D);
        assertEquals(0.5D, ArcadePortRules.evolutionFallDamageMultiplier(3), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.evolutionFallDamageMultiplier(4), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.evolutionFallDamageMultiplier(99), 0.00001D);
    }

    @Test void evolutionPityClockDecreasesForKillsAndAssistsAndOverkillRequiresTwoKills() {
        assertEquals(90_000L, ArcadePortRules.EVOLUTION_PITY_CLOCK_MILLIS);
        assertEquals(15_000L, ArcadePortRules.EVOLUTION_PITY_KILL_REDUCTION_MILLIS);
        assertEquals(7_500L, ArcadePortRules.EVOLUTION_PITY_ASSIST_REDUCTION_MILLIS);
        assertEquals(10_000L, ArcadePortRules.EVOLUTION_ASSIST_WINDOW_MILLIS);
        assertEquals(7_000L, ArcadePortRules.EVOLUTION_GOLEM_SLAM_COOLDOWN_MILLIS);
        assertEquals(6.0D, ArcadePortRules.EVOLUTION_GOLEM_SLAM_RADIUS);
        assertEquals(16.0D, ArcadePortRules.EVOLUTION_GOLEM_SLAM_DAMAGE);
        assertEquals(5_000L, ArcadePortRules.EVOLUTION_BLAZE_FLAMETHROWER_COOLDOWN_MILLIS);
        assertEquals(1_000L, ArcadePortRules.EVOLUTION_BLAZE_FLAMETHROWER_DURATION_MILLIS);
        assertEquals(50L, ArcadePortRules.EVOLUTION_BLAZE_FLAMETHROWER_TICK_MILLIS);
        assertEquals(1.6D, ArcadePortRules.EVOLUTION_BLAZE_FLAME_SPEED);
        assertEquals(7_000L, ArcadePortRules.EVOLUTION_SLIME_SLAM_COOLDOWN_MILLIS);
        assertEquals(2.0D, ArcadePortRules.EVOLUTION_SLIME_SLAM_RADIUS);
        assertEquals(8.0D, ArcadePortRules.EVOLUTION_SLIME_SLAM_DAMAGE);
        assertEquals(0.6D, ArcadePortRules.EVOLUTION_CHICKEN_DOUBLE_JUMP_HORIZONTAL);
        assertEquals(0.6D, ArcadePortRules.EVOLUTION_CHICKEN_DOUBLE_JUMP_VERTICAL);
        assertEquals(2.0D, ArcadePortRules.EVOLUTION_EGG_DAMAGE);
        assertEquals(75_000L, ArcadePortRules.reduceEvolutionPityDeadline(90_000L, 0L, 15_000L));
        assertEquals(82_500L, ArcadePortRules.reduceEvolutionPityDeadline(90_000L, 0L, 7_500L));
        assertEquals(0L, ArcadePortRules.reduceEvolutionPityDeadline(90_000L, 0L, 100_000L));
        assertEquals(30_000L, ArcadePortRules.evolutionPityRemainingMillis(30_000L, 0L));
        assertEquals(0L, ArcadePortRules.evolutionPityRemainingMillis(0L, 30_000L));
        assertEquals("1:30", ArcadePortRules.formatEvolutionPityRemaining(90_000L));
        assertEquals("0:01", ArcadePortRules.formatEvolutionPityRemaining(1L));
        assertEquals(3_000L, ArcadePortRules.EVOLUTION_SULPHUR_BOMB_COOLDOWN_MILLIS);
        assertEquals(1.2D, ArcadePortRules.EVOLUTION_SULPHUR_BOMB_SPEED);
        assertEquals(2.5D, ArcadePortRules.EVOLUTION_SULPHUR_BOMB_RADIUS);
        assertEquals(4.0D, ArcadePortRules.EVOLUTION_SULPHUR_BOMB_DAMAGE);
        assertTrue(ArcadePortRules.canUseEvolutionSulphurBomb(2, true, true));
        assertFalse(ArcadePortRules.canUseEvolutionSulphurBomb(1, true, true));
        assertFalse(ArcadePortRules.canUseEvolutionSulphurBomb(3, true, true));
        assertFalse(ArcadePortRules.canUseEvolutionSulphurBomb(2, false, true));
        assertFalse(ArcadePortRules.canUseEvolutionSulphurBomb(2, true, false));
        assertTrue(ArcadePortRules.isEvolutionSulphurBombReady(3_000L, 3_000L));
        assertFalse(ArcadePortRules.isEvolutionSulphurBombReady(2_999L, 3_000L));
        assertEquals(6_000L, ArcadePortRules.nextEvolutionSulphurBombReadyAt(3_000L));
        assertTrue(ArcadePortRules.isEvolutionSulphurBombInRange(2.5D));
        assertFalse(ArcadePortRules.isEvolutionSulphurBombInRange(2.51D));
        assertEquals(1, ArcadePortRules.nextEvolutionOverkillKillCount(0));
        assertEquals(2, ArcadePortRules.nextEvolutionOverkillKillCount(1));
        assertEquals(2, ArcadePortRules.nextEvolutionOverkillKillCount(2));
        assertEquals(1, ArcadePortRules.nextEvolutionOverkillKillCount(-1));
        assertFalse(ArcadePortRules.evolutionOverkillRequirementMet(1));
        assertTrue(ArcadePortRules.evolutionOverkillRequirementMet(2));
    }

    @Test void evolutionAchievementSetIncludesOverkillAndWrongKitChamp() {
        assertEquals(2, EvolutionAchievement.values().length);
        assertEquals("Overkill", EvolutionAchievement.OVERKILL.displayName());
        assertEquals("Get 2 kills before each evolution.", EvolutionAchievement.OVERKILL.requirement());
        assertEquals("Wrong Kit Champ", EvolutionAchievement.WRONG_KIT_CHAMP.displayName());
        assertEquals("wrong-kit-champ", EvolutionAchievement.WRONG_KIT_CHAMP.slug());
        assertEquals("local-achievements.00000000-0000-0000-0000-000000000001.evolution.wrong-kit-champ",
                EvolutionAchievement.WRONG_KIT_CHAMP.configKey(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }

    @Test void evolutionChargeAcceleratesWithKillsAndCapsAtTheSourceRate() {
        assertEquals(0.012D, ArcadePortRules.evolutionChargePerTick(0), 0.00001D);
        assertEquals(0.012D, ArcadePortRules.evolutionChargePerTick(1), 0.00001D);
        assertEquals(0.015D, ArcadePortRules.evolutionChargePerTick(2), 0.00001D);
        assertEquals(0.021D, ArcadePortRules.evolutionChargePerTick(4), 0.00001D);
        assertEquals(0.022D, ArcadePortRules.evolutionChargePerTick(5), 0.00001D);
        assertEquals(0.022D, ArcadePortRules.evolutionChargePerTick(50), 0.00001D);
    }

    @Test void sheepQuestLeaderRequiresMorePenSheep() {
        assertEquals(0, ArcadePortRules.sheepQuestLeadingTeam(3, 2));
        assertEquals(1, ArcadePortRules.sheepQuestLeadingTeam(1, 4));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(2, 2));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(0, 0));
    }

    @Test void paintballWaterBombRangeAndCooldownMatchSourceReviveFlow() {
        assertEquals(250L, ArcadePortRules.PAINTBALL_WATER_BOMB_COOLDOWN_MILLIS);
        assertEquals(3.0D, ArcadePortRules.PAINTBALL_SAVE_ME_RADIUS, 0.00001D);
        assertTrue(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(0.0D, true));
        assertTrue(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(9.0D, true));
        assertFalse(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(9.01D, true));
        assertFalse(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(1.0D, false));
    }

    @Test void paintballPlayerIsOutOnlyAfterAllFourArmourPiecesArePainted() {
        int remaining = 4;
        remaining = ArcadePortRules.paintballPiecesAfterHit(remaining, 1);
        assertEquals(3, remaining);
        assertFalse(ArcadePortRules.isPaintballPlayerOut(remaining));
        assertEquals(2, ArcadePortRules.paintballPiecesAfterHit(remaining, 1));
        assertEquals(1, ArcadePortRules.paintballPiecesAfterHit(2, 1));
        assertEquals(0, ArcadePortRules.paintballPiecesAfterHit(1, 1));
        assertTrue(ArcadePortRules.isPaintballPlayerOut(0));
        assertEquals(0, ArcadePortRules.paintballPiecesAfterHit(0, 3));
        assertEquals(4, ArcadePortRules.paintballPiecesAfterHit(4, 0));
    }

    @Test void gladiatorsProgressThroughFourArenaTiers() {
        assertEquals(0, ArcadePortRules.gladiatorsTierForRound(-1));
        assertEquals(0, ArcadePortRules.gladiatorsTierForRound(0));
        assertEquals(1, ArcadePortRules.gladiatorsTierForRound(1));
        assertEquals(2, ArcadePortRules.gladiatorsTierForRound(2));
        assertEquals(3, ArcadePortRules.gladiatorsTierForRound(3));
        assertEquals(3, ArcadePortRules.gladiatorsTierForRound(4));
    }

    @Test void gladiatorsVoteModesExposeTheDefaultAndFourVariants() {
        GladiatorsMode normal = GladiatorsMode.fromId("normal");
        assertEquals(5, GladiatorsMode.values().length);
        assertSame(GladiatorsMode.NORMAL, GladiatorsMode.defaultMode());
        assertSame(GladiatorsMode.NORMAL, normal);
        assertTrue(normal.rodAvailable());
        assertTrue(normal.bowAvailable());
        assertTrue(normal.hasClassicRangedTools());
        assertSame(GladiatorsMode.NORMAL, GladiatorsMode.fromId("classic"));

        assertTrue(GladiatorsMode.fromId("rodless").bowAvailable());
        assertFalse(GladiatorsMode.fromId("rodless").rodAvailable());
        assertTrue(GladiatorsMode.fromId("bowless").rodAvailable());
        assertFalse(GladiatorsMode.fromId("bowless").bowAvailable());
        assertFalse(GladiatorsMode.fromId("modern").hasClassicRangedTools());
        assertFalse(GladiatorsMode.fromId("pot").hasClassicRangedTools());
        assertNull(GladiatorsMode.fromId("not-a-mode"));
    }

    @Test void gladiatorsChallengeAchievementRequiresAvailableClassicToolsAndNoUse() {
        assertTrue(ArcadePortRules.shouldAwardGladiatorsNoBowOrRodWin(true, false, false));
        assertFalse(ArcadePortRules.shouldAwardGladiatorsNoBowOrRodWin(true, true, false));
        assertFalse(ArcadePortRules.shouldAwardGladiatorsNoBowOrRodWin(true, false, true));
        assertFalse(ArcadePortRules.shouldAwardGladiatorsNoBowOrRodWin(false, false, false));
    }

    @Test void gladiatorsFirstLossMovesToLowerBracketAndSecondLossEliminates() {
        assertFalse(ArcadePortRules.shouldEliminateGladiatorAfterLoss(0));
        assertFalse(ArcadePortRules.shouldEliminateGladiatorAfterLoss(1));
        assertTrue(ArcadePortRules.shouldEliminateGladiatorAfterLoss(2));
        assertTrue(ArcadePortRules.shouldEliminateGladiatorAfterLoss(3));
    }

    @Test void gladiatorsLowerBracketStartsOnlyAtAStageBoundaryAndPairsAllWaitingPlayers() {
        assertFalse(ArcadePortRules.canStartGladiatorsLoserStage(false, false, 4));
        assertFalse(ArcadePortRules.canStartGladiatorsLoserStage(true, true, 4));
        assertFalse(ArcadePortRules.canStartGladiatorsLoserStage(true, false, 1));
        assertTrue(ArcadePortRules.canStartGladiatorsLoserStage(true, false, 2));
        assertTrue(ArcadePortRules.canStartGladiatorsLoserStage(true, false, 6));
    }

    @Test void gladiatorsLoserArenaContainsTheDuelFootprint() {
        assertFalse(ArcadePortRules.isOutsideGladiatorsLoserArena(0.5D, 0.5D, 0.5D, 0.5D));
        assertFalse(ArcadePortRules.isOutsideGladiatorsLoserArena(4.1D, -3.1D, 0.5D, 0.5D));
        assertTrue(ArcadePortRules.isOutsideGladiatorsLoserArena(4.151D, 0.5D, 0.5D, 0.5D));
        assertTrue(ArcadePortRules.isOutsideGladiatorsLoserArena(Double.NaN, 0.0D, 0.0D, 0.0D));
    }

    @Test void gladiatorsSourceMapBoundsUseInclusiveWorldConfigBlockCoordinates() {
        assertFalse(ArcadePortRules.isOutsideGladiatorsSourceMap(-0.5D, 10.5D, -1, 20, 0, 30));
        assertFalse(ArcadePortRules.isOutsideGladiatorsSourceMap(20.999D, 30.999D, -1, 20, 0, 30));
        assertTrue(ArcadePortRules.isOutsideGladiatorsSourceMap(-1.0D, 10.5D, -1, 20, 0, 30));
        assertTrue(ArcadePortRules.isOutsideGladiatorsSourceMap(21.0D, 10.5D, -1, 20, 0, 30));
    }

    @Test void gladiatorsHalfHeartAchievementUsesOneHealthPoint() {
        assertTrue(ArcadePortRules.shouldAwardGladiatorsHalfHeartWin(1.0D));
        assertTrue(ArcadePortRules.shouldAwardGladiatorsHalfHeartWin(0.5D));
        assertFalse(ArcadePortRules.shouldAwardGladiatorsHalfHeartWin(1.01D));
        assertFalse(ArcadePortRules.shouldAwardGladiatorsHalfHeartWin(0.0D));
        assertFalse(ArcadePortRules.shouldAwardGladiatorsHalfHeartWin(Double.NaN));
    }

    @Test void gladiatorsAchievementAndLoserBracketMilestoneKeysArePlayerScoped() {
        java.util.UUID player = java.util.UUID.fromString("12345678-1234-1234-1234-123456789abc");
        assertEquals("test-achievements.gladiators." + player + ".are-you-not-entertained",
                ArcadePortRules.gladiatorsAchievementConfigKey(player, "Are You Not Entertained"));
        assertEquals("test-milestones.gladiators." + player + ".silky-seconds",
                ArcadePortRules.gladiatorsMilestoneConfigKey(player));
    }

    

    

    

    

    

    

    

    @Test void gladiatorsWaitFiveSecondsAndKeepCombatInsideTheCurrentDuel() {
        assertEquals(5_000L, ArcadePortRules.GLADIATOR_ROUND_COUNTDOWN_MILLIS);
        assertFalse(ArcadePortRules.canGladiatorsFight(4_999L, 5_000L, 0, 0, false));
        assertTrue(ArcadePortRules.canGladiatorsFight(5_000L, 5_000L, 0, 0, false));
        assertFalse(ArcadePortRules.canGladiatorsFight(5_000L, 5_000L, 0, 1, false));
        assertFalse(ArcadePortRules.canGladiatorsFight(5_000L, 5_000L, 0, 0, true));
        assertFalse(ArcadePortRules.canGladiatorsFight(5_000L, 5_000L, -1, 0, false));
    }

    @Test void gladiatorsPoisonOnlyUnresolvedDuelsAfterSixtySecondsAtOneDamagePerSecond() {
        assertEquals(60_000L, ArcadePortRules.GLADIATOR_POISON_DELAY_MILLIS);
        assertEquals(1_000L, ArcadePortRules.GLADIATOR_POISON_INTERVAL_MILLIS);
        assertEquals(1.0D, ArcadePortRules.GLADIATOR_POISON_DAMAGE, 0.0001D);
        assertFalse(ArcadePortRules.shouldGladiatorPoison(59_999L, 0L, false, 2));
        assertTrue(ArcadePortRules.shouldGladiatorPoison(60_000L, 0L, false, 2));
        assertFalse(ArcadePortRules.shouldGladiatorPoison(60_000L, 0L, true, 2));
        assertFalse(ArcadePortRules.shouldGladiatorPoison(60_000L, 0L, false, 1));
        assertFalse(ArcadePortRules.shouldGladiatorPoison(60_000L, 0L, false, 0));
    }

    @Test void gladiatorsPairPlayersAndOnlyAdvanceAfterEveryMatchResolves() {
        assertEquals(0, ArcadePortRules.gladiatorsPairCount(0));
        assertEquals(1, ArcadePortRules.gladiatorsPairCount(1));
        assertEquals(2, ArcadePortRules.gladiatorsPairCount(3));
        assertEquals(8, ArcadePortRules.gladiatorsPairCount(16));
        assertFalse(ArcadePortRules.allGladiatorPairsResolved(4, 3));
        assertTrue(ArcadePortRules.allGladiatorPairsResolved(4, 4));
        assertTrue(ArcadePortRules.allGladiatorPairsResolved(4, 5));
        assertFalse(ArcadePortRules.allGladiatorPairsResolved(0, 0));
    }

    @Test void dragonsScoreboardMatchesSourceThresholds() {
        assertEquals(0, ArcadePortRules.dragonsScoreboardMode(14, 14));
        assertEquals(0, ArcadePortRules.dragonsScoreboardMode(14, 3));
        assertEquals(1, ArcadePortRules.dragonsScoreboardMode(15, 15));
        assertEquals(1, ArcadePortRules.dragonsScoreboardMode(25, 15));
        assertEquals(2, ArcadePortRules.dragonsScoreboardMode(16, 16));
        assertEquals(2, ArcadePortRules.dragonsScoreboardMode(40, 20));
    }

    @Test void dragonsSurvivalAndPlacementGemRulesMatchSource() {
        assertEquals(0.0D, ArcadePortRules.dragonSurvivalGems(-1L), 0.00001D);
        assertEquals(0.9999D, ArcadePortRules.dragonSurvivalGems(9_999L), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.dragonSurvivalGems(10_000L), 0.00001D);
        assertEquals(12.5D, ArcadePortRules.dragonSurvivalGems(125_000L), 0.00001D);
        assertEquals(20.0D, ArcadePortRules.dragonPlacementGemReward(1), 0.00001D);
        assertEquals(15.0D, ArcadePortRules.dragonPlacementGemReward(2), 0.00001D);
        assertEquals(10.0D, ArcadePortRules.dragonPlacementGemReward(3), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.dragonPlacementGemReward(4), 0.00001D);
        assertEquals(10.0D, ArcadePortRules.DRAGONS_PARTICIPATION_GEMS, 0.00001D);
    }

    

    

    @Test void dragonContactHitsRespectPerPlayerCooldown() {
        assertTrue(ArcadePortRules.shouldApplyDragonContactHit(10_000L, 0L));
        assertFalse(ArcadePortRules.shouldApplyDragonContactHit(10_000L, 11_500L));
        assertTrue(ArcadePortRules.shouldApplyDragonContactHit(11_500L, 11_500L));
    }

    @Test void dragonsLeapMatchesSourceVectorAndGroundBoost() {
        Vector grounded = ArcadePortRules.dragonsLeapVelocity(new Vector(0.0D, 0.0D, 1.0D), true);
        assertEquals(0.0D, grounded.getX(), 0.00001D);
        assertEquals(0.4D, grounded.getY(), 0.00001D);
        assertEquals(1.2D, grounded.getZ(), 0.00001D);
        Vector airborne = ArcadePortRules.dragonsLeapVelocity(new Vector(0.0D, 0.0D, 1.0D), false);
        assertEquals(0.2D, airborne.getY(), 0.00001D);
        Vector lookingUp = ArcadePortRules.dragonsLeapVelocity(new Vector(0.0D, 1.0D, 0.0D), true);
        assertEquals(1.2D, lookingUp.getY(), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.dragonsLeapVelocity(new Vector(), true).length(), 0.00001D);
    }

    @Test void dragonsContactKnockbackMatchesSourceTrajectoryAndGroundBoost() {
        Vector grounded = ArcadePortRules.dragonsContactKnockback(new Vector(0.0D, 0.0D, 1.0D), true);
        assertEquals(0.8D, grounded.getY(), 0.00001D);
        assertEquals(1.0D, grounded.getZ(), 0.00001D);
        Vector airborne = ArcadePortRules.dragonsContactKnockback(new Vector(0.0D, 0.0D, 1.0D), false);
        assertEquals(0.6D, airborne.getY(), 0.00001D);
    }

    @Test void dragonsDisableDirectAndProjectilePvpAndOnlyAllowEmeraldWorldItems() {
        assertTrue(ArcadePortRules.shouldCancelDragonsPvp(true, false));
        assertTrue(ArcadePortRules.shouldCancelDragonsPvp(false, true));
        assertFalse(ArcadePortRules.shouldCancelDragonsPvp(false, false));
        assertTrue(ArcadePortRules.shouldAllowDragonWorldItem(Material.EMERALD));
        assertFalse(ArcadePortRules.shouldAllowDragonWorldItem(Material.ARROW));
        assertFalse(ArcadePortRules.shouldAllowDragonWorldItem(Material.FIREWORK_ROCKET));
    }

    @Test void dragonsHaveSixCommunityKitsAndSourceAbilityRefreshValues() {
        assertEquals("coward", ArcadePortRules.normalizeDragonsKit("coward"));
        assertEquals("marksman", ArcadePortRules.normalizeDragonsKit("dragons marksman"));
        assertEquals("pyrotechnic", ArcadePortRules.normalizeDragonsKit("pyro"));
        assertEquals("frost", ArcadePortRules.normalizeDragonsKit("ice bridge"));
        assertEquals("glider", ArcadePortRules.normalizeDragonsKit("glide"));
        assertEquals("tamer", ArcadePortRules.normalizeDragonsKit("dragon tamer"));
        assertNull(ArcadePortRules.normalizeDragonsKit("unknown"));
        assertEquals(1, ArcadePortRules.DRAGONS_MARKSMAN_START_ARROWS);
        assertEquals(4, ArcadePortRules.DRAGONS_MARKSMAN_MAX_ARROWS);
        assertEquals(4_000L, ArcadePortRules.DRAGONS_MARKSMAN_ARROW_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.DRAGONS_MAX_SPARKLERS);
        assertEquals(25_000L, ArcadePortRules.DRAGONS_SPARKLER_INTERVAL_MILLIS);
        assertEquals(25_000L, ArcadePortRules.DRAGONS_FROST_COOLDOWN_MILLIS);
        assertEquals(16, ArcadePortRules.DRAGONS_FROST_BRIDGE_LENGTH);
        assertEquals(6_000L, ArcadePortRules.DRAGONS_FROST_BRIDGE_LIFETIME_MILLIS);
        assertEquals(15_000L, ArcadePortRules.DRAGONS_GLIDER_COOLDOWN_MILLIS);
        assertEquals(3_500L, ArcadePortRules.DRAGONS_GLIDER_DURATION_MILLIS);
        assertEquals(90_000L, ArcadePortRules.DRAGONS_TAMER_COOLDOWN_MILLIS);
        assertEquals(15_000L, ArcadePortRules.DRAGONS_TAMER_DURATION_MILLIS);
        assertEquals(3_000L, ArcadePortRules.DRAGONS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS);
        assertTrue(ArcadePortRules.canUseDragonSpectatorTeleport(3_000L, 3_000L));
        assertFalse(ArcadePortRules.canUseDragonSpectatorTeleport(2_999L, 3_000L));
    }

    

    

    

    

    

    @Test void dragonEscapeHasAThreeDimensionalFinishWaypoint() {
        assertEquals(77.5D, ArcadePortRules.DRAGON_ESCAPE_FINISH_Z);
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(4.0D, 64.0D, 77.5D));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 77.5D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 80.51D));
    }

    @Test void deathTagChasersConvertRunnersOnlyAfterLethalDamageAndHaveDistinctKits() {
        assertFalse(ArcadePortRules.deathTagHitIsLethal(20.0D, 4.0D));
        assertFalse(ArcadePortRules.deathTagHitIsLethal(4.01D, 4.0D));
        assertTrue(ArcadePortRules.deathTagHitIsLethal(4.0D, 4.0D));
        assertTrue(ArcadePortRules.deathTagHitIsLethal(1.0D, 6.0D));
        assertFalse(ArcadePortRules.shouldAwardDeathTagComeAtMeBro(0));
        assertFalse(ArcadePortRules.shouldAwardDeathTagComeAtMeBro(1));
        assertTrue(ArcadePortRules.shouldAwardDeathTagComeAtMeBro(2));
        assertTrue(ArcadePortRules.shouldAwardDeathTagComeAtMeBro(5));
        assertFalse(ArcadePortRules.deathTagHitIsLethal(1.0D, 0.0D));
        assertEquals(4.0D, ArcadePortRules.deathTagChaserDamage(false));
        assertEquals(6.0D, ArcadePortRules.deathTagChaserDamage(true));
        assertEquals(6.0D, ArcadePortRules.deathTagRunnerDamage("basher", false));
        assertEquals(6.0D, ArcadePortRules.deathTagRunnerDamage("traitor", false));
        assertEquals(4.0D, ArcadePortRules.deathTagRunnerDamage("archer", true));
        assertEquals(1.0D, ArcadePortRules.deathTagRunnerDamage("null", false));
        assertEquals(1.0D, ArcadePortRules.deathTagRunnerDamage("archer", false));
        assertEquals(1.0D, ArcadePortRules.deathTagRunnerDamage("archer", false));
        assertEquals(0.0D, ArcadePortRules.deathTagRunnerDamage("null", true));
        assertEquals(0.0D, ArcadePortRules.deathTagRunnerDamage("basher", true));
        assertEquals(4.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, false));
        assertEquals(2.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, true));
        assertEquals(0.0D, ArcadePortRules.deathTagDamageAfterIronSkin(4.0D, true));
        assertEquals(1.0D, ArcadePortRules.deathTagDamageAfterIronSkin(1.0D, true));
        assertEquals(4.0D, ArcadePortRules.deathTagEnvironmentalDamageAfterIronSkin(6.0D, false, false));
        assertEquals(2.0D, ArcadePortRules.deathTagEnvironmentalDamageAfterIronSkin(6.0D, true, false));
        assertEquals(6.0D, ArcadePortRules.deathTagEnvironmentalDamageAfterIronSkin(6.0D, false, true));
        assertEquals(0.5D, ArcadePortRules.deathTagKnockbackMultiplier(true));
        assertEquals(1.0D, ArcadePortRules.deathTagKnockbackMultiplier(false));
    }

    @Test void deathTagOnlyChasersSpreadTheTagToRunners() {
        assertTrue(ArcadePortRules.shouldSpreadDeathTag(true, false));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(false, true));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(false, false));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(true, true));
    }

    @Test void deathTagRunnersGainSpeedOnlyWhenChasersExceedOneQuarterOfPlayers() {
        assertFalse(ArcadePortRules.deathTagRunnersReceiveSpeed(1, 4));
        assertTrue(ArcadePortRules.deathTagRunnersReceiveSpeed(2, 6));
        assertFalse(ArcadePortRules.deathTagRunnersReceiveSpeed(0, 4));
        assertFalse(ArcadePortRules.deathTagRunnersReceiveSpeed(0, 0));
    }

    @Test void deathTagChasersKeepSpeedUntilRunnersCrossTheStrictQuarterThreshold() {
        assertTrue(ArcadePortRules.deathTagChasersReceiveSpeed(1, 4));
        assertTrue(ArcadePortRules.deathTagChasersReceiveSpeed(1, 5));
        assertFalse(ArcadePortRules.deathTagChasersReceiveSpeed(2, 6));
        assertFalse(ArcadePortRules.deathTagChasersReceiveSpeed(0, 4));
    }

    @Test void deathTagKillDeathRatioMatchesSourceChatStats() {
        assertEquals(0.0D, ArcadePortRules.deathTagKillDeathRatio(0, 0), 0.00001D);
        assertEquals(5.0D, ArcadePortRules.deathTagKillDeathRatio(5, 0), 0.00001D);
        assertEquals(2.0D, ArcadePortRules.deathTagKillDeathRatio(6, 3), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.deathTagKillDeathRatio(-2, 5), 0.00001D);
    }

    @Test void deathTagRunnerSpeedIsAnIrreversibleUnlockAfterTheFirstThresholdCrossing() {
        assertFalse(ArcadePortRules.deathTagRunnerSpeedUnlocked(false, 1, 4));
        assertTrue(ArcadePortRules.deathTagRunnerSpeedUnlocked(false, 2, 6));
        assertTrue(ArcadePortRules.deathTagRunnerSpeedUnlocked(true, 1, 5));
        assertTrue(ArcadePortRules.deathTagRunnerSpeedUnlocked(true, 0, 4));
    }

    @Test void deathTagArcherUsesThreeTimesKnockbackAndAlphaHalvesIt() {
        assertEquals(3.0D, ArcadePortRules.deathTagArcherArrowKnockbackMultiplier(false), 0.00001D);
        assertEquals(1.5D, ArcadePortRules.deathTagArcherArrowKnockbackMultiplier(true), 0.00001D);
        assertEquals(2, ArcadePortRules.DEATH_TAG_ARCHER_MAX_ARROWS);
        assertEquals(2_000L, ArcadePortRules.DEATH_TAG_ARCHER_ARROW_INTERVAL_MILLIS);
    }

    @Test void deathTagTraitorKnockbackUsesTheSourceFourHundredMillisecondRecharge() {
        assertTrue(ArcadePortRules.deathTagTraitorKnockbackReady(1_000L, 0L));
        assertFalse(ArcadePortRules.deathTagTraitorKnockbackReady(1_399L, 1_000L));
        assertTrue(ArcadePortRules.deathTagTraitorKnockbackReady(1_400L, 1_000L));
        assertEquals(400L, ArcadePortRules.DEATH_TAG_TRAITOR_KNOCKBACK_COOLDOWN_MILLIS);
    }

    @Test void deathTagEndsWhenOneRunnerRemainsForEveryLobbySize() {
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(1, 8));
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(1, 2));
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(1, 1));
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(0, 8));
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(0, 2));
        assertFalse(ArcadePortRules.shouldFinishDeathTagRound(2, 8));
        assertFalse(ArcadePortRules.shouldFinishDeathTagRound(2, 2));
    }

    @Test void deathTagUsesTheLegacyTwentyMinuteTimeoutAndGemRewardSchedule() {
        assertEquals(1_200_000L, ArcadePortRules.DEATH_TAG_GAME_TIMEOUT_MILLIS);
        assertEquals(10, ArcadePortRules.DEATH_TAG_FORCED_CHASER_REWARD_GEMS);
        assertEquals(4, ArcadePortRules.DEATH_TAG_RUNNER_KILL_REWARD_GEMS);
        assertEquals(4, ArcadePortRules.deathTagKillRewardGems(true));
        assertEquals(0, ArcadePortRules.deathTagKillRewardGems(false));
        assertEquals(15, ArcadePortRules.DEATH_TAG_FIRST_PLACE_REWARD_GEMS);
        assertEquals(10, ArcadePortRules.DEATH_TAG_SECOND_PLACE_REWARD_GEMS);
        assertEquals(5, ArcadePortRules.DEATH_TAG_THIRD_PLACE_REWARD_GEMS);
        assertEquals(10, ArcadePortRules.DEATH_TAG_PARTICIPATION_REWARD_GEMS);
    }

    @Test void deathTagChaserRespawnsUseTheSpawnFurthestFromTheNearestActivePlayer() {
        assertEquals(1, ArcadePortRules.deathTagSafestRespawnSpawnIndex(
                java.util.List.of(16.0D, 100.0D, 36.0D)));
        assertEquals(0, ArcadePortRules.deathTagSafestRespawnSpawnIndex(
                java.util.List.of(25.0D, 25.0D, 25.0D)));
        assertEquals(0, ArcadePortRules.deathTagSafestRespawnSpawnIndex(
                java.util.List.of(Double.POSITIVE_INFINITY, 64.0D)));
        assertEquals(1, ArcadePortRules.deathTagSafestRespawnSpawnIndex(
                java.util.Arrays.asList(Double.NaN, 4.0D, -1.0D)));
        assertEquals(-1, ArcadePortRules.deathTagSafestRespawnSpawnIndex(java.util.List.of()));
        assertEquals(-1, ArcadePortRules.deathTagSafestRespawnSpawnIndex(null));
    }

    @Test void deployedArcadeAllowListExposesOnlyAcceptedGameIds() {
        java.util.List<String> enabled = java.util.List.of("bacon-brawl");
        assertTrue(ArcadePortRules.isArcadePortGameEnabled("bacon-brawl", enabled));
        assertTrue(ArcadePortRules.isArcadePortGameEnabled("Bacon_Brawl", enabled));
        assertFalse(ArcadePortRules.isArcadePortGameEnabled("bomb-lobbers", enabled));
        assertFalse(ArcadePortRules.isArcadePortGameEnabled("death-tag", enabled));
        assertFalse(ArcadePortRules.isArcadePortGameEnabled(null, enabled));
        assertFalse(ArcadePortRules.isArcadePortGameEnabled("bacon-brawl", java.util.List.of()));
    }

    @Test void bombLobbersManagedTntUsesOnlyCustomDirectAndExplosionDamage() {
        assertTrue(ArcadePortRules.shouldSuppressNativeBombLobbersTntDamage(true));
        assertFalse(ArcadePortRules.shouldSuppressNativeBombLobbersTntDamage(false));
    }

    @Test void bombLobbersUsesLegacyTwentyMinuteTimeout() {
        assertEquals(1_200_000L, ArcadePortRules.BOMB_LOBBERS_ROUND_MILLIS);
    }

    @Test void bombLobbersMeleeDamageRequiresTwoLiveParticipants() {
        assertTrue(ArcadePortRules.shouldProcessBombLobbersPlayerHit(true, true));
        assertFalse(ArcadePortRules.shouldProcessBombLobbersPlayerHit(false, true));
        assertFalse(ArcadePortRules.shouldProcessBombLobbersPlayerHit(true, false));
        assertFalse(ArcadePortRules.shouldProcessBombLobbersPlayerHit(false, false));
    }

    @Test void bombLobbersTwoBirdsCountsDirectHitAndBlastKillsTogether() {
        int kills = 0;
        kills = ArcadePortRules.nextBombLobbersTntKillCount(kills, true);
        assertFalse(ArcadePortRules.shouldAwardBombLobbersTwoBirdsOneStone(kills));
        kills = ArcadePortRules.nextBombLobbersTntKillCount(kills, true);
        assertTrue(ArcadePortRules.shouldAwardBombLobbersTwoBirdsOneStone(kills));
        assertEquals(2, kills);
        assertEquals(2, ArcadePortRules.nextBombLobbersTntKillCount(2, false));
    }

    @Test void bombLobbersAchievementsUseTheRequestedThresholdsAndEligibility() {
        assertFalse(ArcadePortRules.shouldAwardBombLobbersUltimateKiller(4));
        assertTrue(ArcadePortRules.shouldAwardBombLobbersUltimateKiller(5));
        assertFalse(ArcadePortRules.shouldAwardBombLobbersTwoBirdsOneStone(1));
        assertTrue(ArcadePortRules.shouldAwardBombLobbersTwoBirdsOneStone(2));
        assertFalse(ArcadePortRules.shouldAwardBombLobbersTeamPlayer(4));
        assertTrue(ArcadePortRules.shouldAwardBombLobbersTeamPlayer(5));
        assertFalse(ArcadePortRules.shouldAwardBombLobbersDoubleJump(true, true, true));
        assertFalse(ArcadePortRules.shouldAwardBombLobbersDoubleJump(true, false, false));
        assertTrue(ArcadePortRules.shouldAwardBombLobbersDoubleJump(true, true, false));
        assertFalse(ArcadePortRules.shouldAwardBombLobbersMasterBomber(29));
        assertTrue(ArcadePortRules.shouldAwardBombLobbersMasterBomber(30));
        assertEquals("test-achievements.bomb-lobbers.00000000-0000-0000-0000-000000000001.ultimate-killer",
                ArcadePortRules.bombLobbersAchievementConfigKey(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "ultimate-killer"));
        assertEquals("", ArcadePortRules.bombLobbersAchievementConfigKey(null, "ultimate-killer"));
        assertEquals("", ArcadePortRules.bombLobbersAchievementConfigKey(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "../unsafe"));
    }

    @Test void bombLobbersTntIsCancelledForOfflineThrowerOrOwnSide() {
        assertTrue(ArcadePortRules.shouldCancelBombLobbersExplosion(false, 0, 1));
        assertTrue(ArcadePortRules.shouldCancelBombLobbersExplosion(true, 0, 0));
        assertTrue(ArcadePortRules.shouldCancelBombLobbersExplosion(true, 1, 1));
        assertFalse(ArcadePortRules.shouldCancelBombLobbersExplosion(true, 0, 1));
        assertFalse(ArcadePortRules.shouldCancelBombLobbersExplosion(true, -1, 1),
                "Unknown team assignment does not automatically count as own side");
    }

    @Test void bombLobbersTntOnlyDamagesAssignedEnemyTeams() {
        assertFalse(ArcadePortRules.areBombLobbersEnemies(0, 0));
        assertFalse(ArcadePortRules.areBombLobbersEnemies(1, 1));
        assertTrue(ArcadePortRules.areBombLobbersEnemies(0, 1));
        assertTrue(ArcadePortRules.areBombLobbersEnemies(1, 0));
        assertFalse(ArcadePortRules.areBombLobbersEnemies(-1, 1));
        assertFalse(ArcadePortRules.areBombLobbersEnemies(0, -1));
    }

    @Test void bombLobbersBlastDamageMatchesSpigot18IntegerExplosionAndMineplexReplacement() {
        assertEquals(8.0D, ArcadePortRules.BOMB_LOBBERS_EXPLOSION_RADIUS, 0.00001D);
        assertEquals(50.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 1.0D), 0.00001D);
        assertEquals(43.0D, ArcadePortRules.bombLobbersBlastDamage(2.0D, 1.0D), 0.00001D);
        assertEquals(33.0D, ArcadePortRules.bombLobbersBlastDamage(3.0D, 1.0D), 0.00001D);
        assertEquals(25.0D, ArcadePortRules.bombLobbersBlastDamage(4.0D, 1.0D), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.bombLobbersBlastDamage(8.0D, 1.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.bombLobbersBlastDamage(8.01D, 1.0D), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 0.0D), 0.00001D);
        assertEquals(17.0D, ArcadePortRules.bombLobbersBlastDamage(2.0D, 0.5D), 0.00001D);
        assertEquals(50.0D, ArcadePortRules.BOMB_LOBBERS_MAX_EXPLOSION_DAMAGE, 0.00001D);
    }

    @Test void bombLobbersExplosionAppliesOriginalStoneConversions() {
        assertEquals(Material.COBBLESTONE,
                ArcadePortRules.bombLobbersBlockAfterExplosion(Material.STONE));
        assertEquals(Material.AIR,
                ArcadePortRules.bombLobbersBlockAfterExplosion(Material.DIORITE));
        assertEquals(Material.AIR,
                ArcadePortRules.bombLobbersBlockAfterExplosion(Material.POLISHED_GRANITE));
        assertEquals(Material.CRACKED_STONE_BRICKS,
                ArcadePortRules.bombLobbersBlockAfterExplosion(Material.STONE_BRICKS));
        assertEquals(Material.CRACKED_STONE_BRICKS,
                ArcadePortRules.bombLobbersBlockAfterExplosion(Material.MOSSY_STONE_BRICKS));
        assertEquals(Material.CRACKED_STONE_BRICKS,
                ArcadePortRules.bombLobbersBlockAfterExplosion(Material.CHISELED_STONE_BRICKS));
        assertEquals(Material.AIR,
                ArcadePortRules.bombLobbersBlockAfterExplosion(Material.CRACKED_STONE_BRICKS));
        assertEquals(Material.AIR,
                ArcadePortRules.bombLobbersBlockAfterExplosion(Material.GLASS));
    }

    @Test void bombLobbersExplosionIsSuppressedOnTheThrowersOwnSpawnSide() {
        assertTrue(ArcadePortRules.shouldSuppressBombLobbersSpawnSideExplosion(0, 0));
        assertTrue(ArcadePortRules.shouldSuppressBombLobbersSpawnSideExplosion(1, 1));
        assertFalse(ArcadePortRules.shouldSuppressBombLobbersSpawnSideExplosion(0, 1));
        assertFalse(ArcadePortRules.shouldSuppressBombLobbersSpawnSideExplosion(1, 0));
        assertFalse(ArcadePortRules.shouldSuppressBombLobbersSpawnSideExplosion(-1, 0));
        assertTrue(ArcadePortRules.shouldCheckBombLobbersDirectHit(false));
        assertFalse(ArcadePortRules.shouldCheckBombLobbersDirectHit(true));
    }

    @Test void bombLobbersEliminatesPlayersWhoCrossIntoAnotherTeamSide() {
        assertFalse(ArcadePortRules.shouldEliminateBombLobbersForSide(0, 0));
        assertFalse(ArcadePortRules.shouldEliminateBombLobbersForSide(1, 1));
        assertTrue(ArcadePortRules.shouldEliminateBombLobbersForSide(0, 1));
        assertTrue(ArcadePortRules.shouldEliminateBombLobbersForSide(1, 0));
        assertFalse(ArcadePortRules.shouldEliminateBombLobbersForSide(-1, 0));
        assertFalse(ArcadePortRules.shouldEliminateBombLobbersForSide(0, -1));
    }

    @Test void bombLobbersDamageRespectsLegacyDamageDependentArmorReduction() {
        assertEquals(10.0D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 0.0D), 0.00001D);
        assertEquals(9.2D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 7.0D), 0.00001D);
        assertEquals(7.2D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 12.0D), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 15.0D), 0.00001D);
        assertEquals(4.0D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 20.0D), 0.00001D);
        assertEquals(0.42D, ArcadePortRules.bombLobbersDamageAfterArmor(1.0D, 15.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.bombLobbersDamageAfterArmor(-1.0D, 15.0D), 0.00001D);
        assertEquals(10.0D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, -5.0D), 0.00001D);
    }

    @Test void bombLobbersDirectHitCannotPassThroughTheFirstSolidBlock() {
        assertTrue(ArcadePortRules.shouldResolveBombLobbersDirectHitBeforeBlock(4.0D, 9.0D));
        assertTrue(ArcadePortRules.shouldResolveBombLobbersDirectHitBeforeBlock(9.0D, 9.0D));
        assertFalse(ArcadePortRules.shouldResolveBombLobbersDirectHitBeforeBlock(9.01D, 9.0D));
        assertTrue(ArcadePortRules.shouldResolveBombLobbersDirectHitBeforeBlock(
                4.0D, Double.POSITIVE_INFINITY));
        assertFalse(ArcadePortRules.shouldResolveBombLobbersDirectHitBeforeBlock(
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertFalse(ArcadePortRules.shouldResolveBombLobbersDirectHitBeforeBlock(4.0D, Double.NaN));
    }

    @Test void bombLobbersDirectHitUsesSourceExpandedTargetBoxAndSweptPath() {
        assertEquals(1.0D, ArcadePortRules.BOMB_LOBBERS_DIRECT_HIT_BOX_GROW, 0.00001D);
        var targetBox = new org.bukkit.util.BoundingBox(-0.3D, 0.0D, -0.3D, 0.3D, 1.8D, 0.3D);
        var start = new org.bukkit.Location(null, -3.0D, 2.5D, 0.0D);
        var end = new org.bukkit.Location(null, 3.0D, 2.5D, 0.0D);
        double hitDistance = ArcadePortRules.bombLobbersDirectHitDistanceSquared(start, end, targetBox);
        assertTrue(Double.isFinite(hitDistance));
        assertEquals(2.89D, hitDistance, 0.00001D);
        assertTrue(Double.isInfinite(ArcadePortRules.bombLobbersDirectHitDistanceSquared(
                new org.bukkit.Location(null, -3.0D, 3.0D, 0.0D),
                new org.bukkit.Location(null, 3.0D, 3.0D, 0.0D), targetBox)));
        assertTrue(Double.isInfinite(ArcadePortRules.bombLobbersDirectHitDistanceSquared(
                start, end, null)));
    }

    @Test void baconBrawlStartsWithExtraKnockbackForTwoPlayers() {
        assertEquals(1.8D, ArcadePortRules.initialBaconKnockback(2), 0.00001D);
        assertEquals(1.5D, ArcadePortRules.initialBaconKnockback(1), 0.00001D);
        assertEquals(1.5D, ArcadePortRules.initialBaconKnockback(3), 0.00001D);
    }

    @Test void sheepQuestUsesFourInitialSheepAndThreeSheepStacks() {
        assertEquals(4, ArcadePortRules.SHEEP_QUEST_INITIAL_SHEEP);
        assertEquals(300_000L, ArcadePortRules.SHEEP_QUEST_ROUND_MILLIS);
        assertEquals(3, ArcadePortRules.SHEEP_QUEST_MAX_CARRY);
        assertEquals(20_000L, ArcadePortRules.SHEEP_QUEST_SPAWN_INTERVAL_MILLIS);
        assertTrue(ArcadePortRules.canCarryAnotherSheep(0));
        assertTrue(ArcadePortRules.canCarryAnotherSheep(2));
        assertFalse(ArcadePortRules.canCarryAnotherSheep(3));
        assertFalse(ArcadePortRules.canCarryAnotherSheep(-1));
    }

    @Test void sheepQuestKitNamesMatchTheLegacyMenuAndAliases() {
        assertEquals("beserker", ArcadePortRules.normalizeSheepQuestKit("Beserker"));
        assertEquals("beserker", ArcadePortRules.normalizeSheepQuestKit("sheep-berserker"));
        assertEquals("beserker", ArcadePortRules.normalizeSheepQuestKit("double jump"));
        assertEquals("archer", ArcadePortRules.normalizeSheepQuestKit("sheep archer"));
        assertEquals("brute", ArcadePortRules.normalizeSheepQuestKit("thrower"));
        assertNull(ArcadePortRules.normalizeSheepQuestKit("fighter"));
        assertNull(ArcadePortRules.normalizeSheepQuestKit(null));
    }

    @Test void sheepQuestPickupUsesLegacyRangeAndRecharge() {
        assertEquals(2.5D, ArcadePortRules.SHEEP_QUEST_PICKUP_RADIUS, 0.00001D);
        assertEquals(500L, ArcadePortRules.SHEEP_QUEST_PICKUP_COOLDOWN_MILLIS);
        assertTrue(ArcadePortRules.sheepQuestPickupReady(1_500L, 1_500L));
        assertFalse(ArcadePortRules.sheepQuestPickupReady(1_499L, 1_500L));
        assertEquals(8_000L, ArcadePortRules.SHEEP_QUEST_BESERKER_JUMP_COOLDOWN_MILLIS);
    }

    @Test void sheepQuestTiedFiveMinuteResultEntersLeadChangeOvertime() {
        assertTrue(ArcadePortRules.sheepQuestIsTied(new int[]{4, 4, 1, 0}));
        assertTrue(ArcadePortRules.sheepQuestIsTied(new int[]{2, 2, 2, 2}));
        assertTrue(ArcadePortRules.sheepQuestIsTied(new int[]{0, 0, 0, 0}));
        assertFalse(ArcadePortRules.sheepQuestIsTied(new int[]{5, 4, 4, 0}));
        assertTrue(ArcadePortRules.sheepQuestIsTied(null));
        assertEquals(10_000L, ArcadePortRules.SHEEP_QUEST_OVERTIME_ANNOUNCE_INTERVAL_MILLIS);
        assertEquals(6.0D, ArcadePortRules.SHEEP_QUEST_THROWER_DAMAGE, 0.00001D);
    }

    @Test void sheepQuestSlowsMoreAsPlayersCarryAdditionalSheep() {
        assertEquals(-1, ArcadePortRules.sheepQuestCarrySlownessAmplifier(0));
        assertEquals(0, ArcadePortRules.sheepQuestCarrySlownessAmplifier(1));
        assertEquals(1, ArcadePortRules.sheepQuestCarrySlownessAmplifier(2));
        assertEquals(2, ArcadePortRules.sheepQuestCarrySlownessAmplifier(3));
        assertEquals(2, ArcadePortRules.sheepQuestCarrySlownessAmplifier(4));
        assertEquals(-1, ArcadePortRules.sheepQuestCarrySlownessAmplifier(-1));
    }

    @Test void sheepQuestSupportsFourTeamsAndDetectsTies() {
        assertEquals(0, ArcadePortRules.sheepQuestLeadingTeam(new int[]{5, 3, 2, 1}));
        assertEquals(1, ArcadePortRules.sheepQuestLeadingTeam(new int[]{1, 4, 2, 3}));
        assertEquals(2, ArcadePortRules.sheepQuestLeadingTeam(new int[]{1, 2, 6, 3}));
        assertEquals(3, ArcadePortRules.sheepQuestLeadingTeam(new int[]{1, 2, 3, 7}));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(new int[]{3, 3, 1, 0}));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(new int[]{2, 2, 2, 2}));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(null));
    }

    @Test void sheepQuestFourTeamMarkersFollowSourceMapTeamIndexOrder() {
        assertEquals("RED", ArcadePortRules.sheepQuestTeamPenMarker(0));
        assertEquals("BLUE", ArcadePortRules.sheepQuestTeamPenMarker(1));
        assertEquals("GREEN", ArcadePortRules.sheepQuestTeamPenMarker(2));
        assertEquals("YELLOW", ArcadePortRules.sheepQuestTeamPenMarker(3));
        assertEquals("UNASSIGNED", ArcadePortRules.sheepQuestTeamPenMarker(4));
    }

    @Test void sheepQuestPenMarkersMatchTheExactSourceDataLocations() {
        assertTrue(ArcadePortRules.sheepQuestInsidePenMarker(10, 64, -4, 10, 64, -4));
        assertFalse(ArcadePortRules.sheepQuestInsidePenMarker(10, 65, -4, 10, 64, -4));
        assertFalse(ArcadePortRules.sheepQuestInsidePenMarker(11, 64, -4, 10, 64, -4));
        assertFalse(ArcadePortRules.sheepQuestInsidePenMarker(10, 64, -3, 10, 64, -4));
    }

    @Test void sheepQuestPlayerHitsDropSheepAndDealDamage() {
        assertEquals(4.0D, ArcadePortRules.SHEEP_QUEST_PLAYER_HIT_DAMAGE, 0.00001D);
    }

    @Test void turfWarsTerritoryUsesTheSourceSpawnFloorHeight() {
        assertEquals(63, ArcadePortRules.turfWarsFloorYFromSpawn(64.0D));
        assertEquals(69, ArcadePortRules.turfWarsFloorYFromSpawn(70.9D));
        assertEquals(40, ArcadePortRules.turfWarsFloorYFromSpawn(41.0D));
        assertEquals(63, ArcadePortRules.turfWarsFloorYFromSpawn(Double.NaN));
    }

    @Test void turfWarsBuildPlacementStaysInsideSourceBoundsAndTerritory() {
        assertTrue(ArcadePortRules.isTurfWarsBuildWithinBounds(100, -10, 100, 140, -20, 20));
        assertTrue(ArcadePortRules.isTurfWarsBuildWithinBounds(139, 19, 100, 140, -20, 20));
        assertFalse(ArcadePortRules.isTurfWarsBuildWithinBounds(99, 0, 100, 140, -20, 20));
        assertFalse(ArcadePortRules.isTurfWarsBuildWithinBounds(140, 0, 100, 140, -20, 20));
        assertFalse(ArcadePortRules.isTurfWarsBuildWithinBounds(120, -21, 100, 140, -20, 20));
        assertFalse(ArcadePortRules.isTurfWarsBuildWithinBounds(120, 20, 100, 140, -20, 20));

        assertTrue(ArcadePortRules.canTurfWarsBuildOnTerritory(0, 119, 119));
        assertFalse(ArcadePortRules.canTurfWarsBuildOnTerritory(0, 120, 119));
        assertTrue(ArcadePortRules.canTurfWarsBuildOnTerritory(1, 120, 119));
        assertFalse(ArcadePortRules.canTurfWarsBuildOnTerritory(1, 119, 119));
        assertFalse(ArcadePortRules.canTurfWarsBuildOnTerritory(2, 120, 119));
    }

    @Test void turfWarsFrontlineAndVictoryUseTheSourceMapBounds() {
        assertEquals(-1, ArcadePortRules.turfWarsInitialFrontline(-20, 20));
        assertEquals(119, ArcadePortRules.turfWarsInitialFrontline(100, 140));
        assertEquals(120, ArcadePortRules.turfWarsInitialFrontline(100, 141));

        assertFalse(ArcadePortRules.isTurfWarsWinningLine(138, 0, 100, 140));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(139, 0, 100, 140));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(100, 1, 100, 140));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(99, 1, 100, 140));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(120, 2, 100, 140));

        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(120, 119, 0, 50));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(118, 119, 50, 0));
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(119, 119, 4, 3));
        assertEquals(-1, ArcadePortRules.turfWarsTimeoutLeader(119, 119, 4, 4));
    }

    @Test void turfWarsUsesKitSpecificProjectileAndMeleeDamage() {
        assertEquals(500.0D, ArcadePortRules.turfWarsHitDamage("marksman", true, false), 0.00001D);
        assertEquals(500.0D, ArcadePortRules.turfWarsHitDamage("infiltrator", true, true), 0.00001D);
        assertEquals(9.0D, ArcadePortRules.turfWarsHitDamage("shredder", true, false), 0.00001D);
        assertEquals(9.0D, ArcadePortRules.turfWarsHitDamage("shredder", true, true), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.turfWarsHitDamage("marksman", false, false), 0.00001D);
        assertEquals(12.0D, ArcadePortRules.turfWarsHitDamage("infiltrator", false, true), 0.00001D);
    }

    @Test void paintballHealthTracksRemainingArmourPieces() {
        assertEquals(21.0D, ArcadePortRules.paintballHealthAfterHit(4), 0.00001D);
        assertEquals(16.0D, ArcadePortRules.paintballHealthAfterHit(3), 0.00001D);
        assertEquals(11.0D, ArcadePortRules.paintballHealthAfterHit(2), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.paintballHealthAfterHit(1), 0.00001D);
        assertEquals(2.0D, ArcadePortRules.paintballHealthAfterHit(0), 0.00001D);
    }

    @Test void microBattlesDamageMatchesRebornRawDamageIronSkinAndArmour() {
        assertEquals(3.0D, ArcadePortRules.microBattlesMeleeDamage("WOODEN_SWORD"), 0.00001D);
        assertEquals(4.0D, ArcadePortRules.microBattlesMeleeDamage("STONE_SWORD"), 0.00001D);
        assertEquals(7.0D, ArcadePortRules.microBattlesMeleeDamage("DIAMOND_SWORD"), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.microBattlesMeleeDamage("GOLDEN_SWORD"), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.microBattlesMeleeDamage("STONE_SHOVEL"), 0.00001D);
        assertEquals(3.0D, ArcadePortRules.microBattlesArrowDamage(1.0D), 0.00001D);
        assertEquals(4.5D, ArcadePortRules.microBattlesArrowDamage(1.5D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.microBattlesArrowDamage(Double.NaN), 0.00001D);
        assertEquals(2.5D, ArcadePortRules.microBattlesDamageAfterIronSkin(3.0D, true), 0.00001D);
        assertEquals(3.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(3.0D, false), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(1.0D, true), 0.00001D);
        assertEquals(2.16D, ArcadePortRules.microBattlesDamageAfterArmour(3.0D, 7.0D, false), 0.00001D);
        assertEquals(1.8D, ArcadePortRules.microBattlesDamageAfterArmour(2.5D, 7.0D, false), 0.00001D);
        assertEquals(5.0D, ArcadePortRules.microBattlesDamageAfterArmour(5.0D, 7.0D, true), 0.00001D);
        assertEquals(5.0D, ArcadePortRules.microBattlesDamageAfterArmour(5.0D, 0.0D, false), 0.00001D);
        assertTrue(ArcadePortRules.isMicroBattlesCombatContributionFresh(10_000L, 9_999L));
        assertFalse(ArcadePortRules.isMicroBattlesCombatContributionFresh(25_000L, 10_000L));
    }

    @Test void microBattlesDamageRulesRetainSourceScaling() {
        assertEquals(7.0D, ArcadePortRules.microBattlesMeleeDamage("DIAMOND_SWORD"), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.microBattlesMeleeDamage("GOLDEN_SWORD"), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.microBattlesMeleeDamage("STONE_SHOVEL"), 0.00001D);
        assertEquals(3.0D, ArcadePortRules.microBattlesArrowDamage(1.0D), 0.00001D);
        assertEquals(4.5D, ArcadePortRules.microBattlesArrowDamage(1.5D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.microBattlesArrowDamage(Double.NaN), 0.00001D);
        assertEquals(2.5D, ArcadePortRules.microBattlesDamageAfterIronSkin(3.0D, true), 0.00001D);
        assertEquals(3.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(3.0D, false), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(1.0D, true), 0.00001D);
        assertEquals(2.16D, ArcadePortRules.microBattlesDamageAfterArmour(3.0D, 7.0D, false), 0.00001D);
        assertEquals(1.8D, ArcadePortRules.microBattlesDamageAfterArmour(2.5D, 7.0D, false), 0.00001D);
        assertEquals(5.0D, ArcadePortRules.microBattlesDamageAfterArmour(5.0D, 7.0D, true), 0.00001D);
        assertEquals(5.0D, ArcadePortRules.microBattlesDamageAfterArmour(5.0D, 0.0D, false), 0.00001D);
        assertTrue(ArcadePortRules.isMicroBattlesCombatContributionFresh(10_000L, 9_999L));
        assertFalse(ArcadePortRules.isMicroBattlesCombatContributionFresh(25_000L, 10_000L));
    }

    @Test void microBattlesUsesMinimumHunger() {
        assertEquals(2, ArcadePortRules.microBattlesMinimumFoodLevel(0));
        assertEquals(2, ArcadePortRules.microBattlesMinimumFoodLevel(2));
        assertEquals(7, ArcadePortRules.microBattlesMinimumFoodLevel(7));
        assertEquals(20, ArcadePortRules.microBattlesMinimumFoodLevel(25));
    }


    @Test void timedTeamGamesPreferSurvivorsThenAggregateKills() {
        assertEquals(0, ArcadePortRules.teamWithBestTimeoutStanding(3, 2, 1, 99));
        assertEquals(1, ArcadePortRules.teamWithBestTimeoutStanding(1, 2, 99, 0));
        assertEquals(0, ArcadePortRules.teamWithBestTimeoutStanding(2, 2, 7, 6));
        assertEquals(1, ArcadePortRules.teamWithBestTimeoutStanding(2, 2, 3, 4));
        assertEquals(-1, ArcadePortRules.teamWithBestTimeoutStanding(2, 2, 4, 4));
    }

    @Test void deathTagLightsAlternateBetweenGoldAndBlackWool() {
        assertEquals(org.bukkit.Material.BLACK_WOOL,
                ArcadePortRules.deathTagNextLightMaterial(org.bukkit.Material.GOLD_BLOCK));
        assertEquals(org.bukkit.Material.GOLD_BLOCK,
                ArcadePortRules.deathTagNextLightMaterial(org.bukkit.Material.BLACK_WOOL));
        assertEquals(org.bukkit.Material.GOLD_BLOCK,
                ArcadePortRules.deathTagNextLightMaterial(org.bukkit.Material.GLOWSTONE));
    }

    @Test void gladiatorsStartingTierScalesWithLobbySize() {
        assertEquals(3, ArcadePortRules.gladiatorsStartingTierForPlayers(1));
        assertEquals(3, ArcadePortRules.gladiatorsStartingTierForPlayers(2));
        assertEquals(2, ArcadePortRules.gladiatorsStartingTierForPlayers(3));
        assertEquals(2, ArcadePortRules.gladiatorsStartingTierForPlayers(4));
        assertEquals(1, ArcadePortRules.gladiatorsStartingTierForPlayers(5));
        assertEquals(1, ArcadePortRules.gladiatorsStartingTierForPlayers(8));
        assertEquals(0, ArcadePortRules.gladiatorsStartingTierForPlayers(9));
        assertEquals(0, ArcadePortRules.gladiatorsStartingTierForPlayers(16));
    }

    @Test void gladiatorsRoundsMoveFromOuterGreenArenasTowardRedFinals() {
        assertEquals("GREEN", ArcadePortRules.gladiatorsArenaMarkerGroup(0));
        assertEquals("YELLOW", ArcadePortRules.gladiatorsArenaMarkerGroup(1));
        assertEquals("ORANGE", ArcadePortRules.gladiatorsArenaMarkerGroup(2));
        assertEquals("RED", ArcadePortRules.gladiatorsArenaMarkerGroup(3));
        assertEquals("GREEN", ArcadePortRules.gladiatorsArenaMarkerGroup(-1));
        assertEquals("RED", ArcadePortRules.gladiatorsArenaMarkerGroup(4));
    }

    @Test void drawMyThingRoundDurationScalesToTheFiveMinuteBudgetAboveTenPlayers() {
        assertEquals(30_000L, ArcadePortRules.drawMyThingRoundMillis(1));
        assertEquals(30_000L, ArcadePortRules.drawMyThingRoundMillis(10));
        assertEquals(24_545L, ArcadePortRules.drawMyThingRoundMillis(11));
        assertEquals(22_250L, ArcadePortRules.drawMyThingRoundMillis(12));
        assertEquals(15_937L, ArcadePortRules.drawMyThingRoundMillis(16));

        for (int players = 11; players <= 64; players++) {
            long drawingMillis = players * ArcadePortRules.drawMyThingRoundMillis(players);
            long intermissionMillis = (players - 1L) * ArcadePortRules.DRAW_MY_THING_INTERMISSION_MILLIS;
            assertTrue(drawingMillis + intermissionMillis <= ArcadePortRules.DRAW_MY_THING_GAME_MILLIS,
                    "DMT schedule exceeds five minutes for " + players + " players");
        }
    }

    @Test void masterBuildersLimitsLobbySizeAndAllocatesSixteenDistinctPlots() {
        assertFalse(ArcadePortRules.supportsMasterBuildersPlayerCount(0));
        assertTrue(ArcadePortRules.supportsMasterBuildersPlayerCount(1));
        assertTrue(ArcadePortRules.supportsMasterBuildersPlayerCount(16));
        assertFalse(ArcadePortRules.supportsMasterBuildersPlayerCount(17));

        Set<String> plots = new HashSet<>();
        for (int plot = 0; plot < ArcadePortRules.MASTER_BUILDERS_MAX_BUILDERS; plot++) {
            String centre = ArcadePortRules.masterBuildersPlotCenterX(plot) + ":"
                    + ArcadePortRules.masterBuildersPlotCenterZ(plot);
            assertTrue(plots.add(centre), "Master Builders plot centres must be unique");
        }
        assertEquals(ArcadePortRules.masterBuildersPlotCenterX(0),
                ArcadePortRules.masterBuildersPlotCenterX(16), 0.00001D);
        assertEquals(ArcadePortRules.masterBuildersPlotCenterZ(0),
                ArcadePortRules.masterBuildersPlotCenterZ(16), 0.00001D);
    }

    @Test void turfWarsTimeoutUsesTerritoryBeforeKills() {
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(0, 0, 99));
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(8, 0, 99));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(-2, 99, 0));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(-8, 99, 0));
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(-1, 5, 4));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(-1, 4, 5));
        assertEquals(-1, ArcadePortRules.turfWarsTimeoutLeader(-1, 5, 5));
    }
}
