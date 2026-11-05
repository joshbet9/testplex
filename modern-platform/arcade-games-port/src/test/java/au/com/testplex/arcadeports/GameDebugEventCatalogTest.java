package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameDebugEventCatalogTest {
    @Test
    void eventToolsAreExplicitlyScopedToTheirGame() {
        var witherEvents = GameDebugEventCatalog.events(ArcadePortGame.WITHER_ASSAULT);
        assertEquals(1, witherEvents.size());
        assertEquals("spawn-minions", witherEvents.getFirst().id());
        assertEquals("reveal-letter", GameDebugEventCatalog.events(ArcadePortGame.DRAW_MY_THING).getFirst().id());
        assertEquals("spawn-dragon", GameDebugEventCatalog.events(ArcadePortGame.DRAGONS).getFirst().id());
        assertEquals("refill-arrow", GameDebugEventCatalog.events(ArcadePortGame.ONE_IN_THE_QUIVER)
                .getFirst().id());
        assertEquals("restore-armour", GameDebugEventCatalog.events(ArcadePortGame.PAINTBALL)
                .getFirst().id());
        assertEquals("promote-self", GameDebugEventCatalog.events(ArcadePortGame.DEATH_TAG)
                .getFirst().id());
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.SPLEEF).stream()
                .anyMatch(event -> event.id().equals("refresh-kit")));
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.SPLEEF).stream()
                .anyMatch(event -> event.id().equals("restore-hunger")));
        assertEquals("grant-spells", GameDebugEventCatalog.events(ArcadePortGame.WIZARDS)
                .getFirst().id());
        assertFalse(GameDebugEventCatalog.events(ArcadePortGame.BACON_BRAWL).stream()
                .anyMatch(event -> event.id().equals("spawn-minions")));
    }

    @Test
    void everyListedEventHasAProductionDispatcher() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        for (ArcadePortGame game : ArcadePortGame.values()) {
            for (GameDebugEventCatalog.Event event : GameDebugEventCatalog.events(game)) {
                boolean directDispatcher = source.contains("type == ArcadePortGame." + game.name()
                        + " && \"" + event.id() + "\".equals(eventId)");
                boolean sharedKitDispatcher = "refresh-kit".equals(event.id())
                        && GameDebugEventCatalog.supportsKitRefresh(game)
                        && source.contains("GameDebugEventCatalog.supportsKitRefresh(type)"
                                + " && \"refresh-kit\".equals(eventId)");
                assertTrue(directDispatcher || sharedKitDispatcher,
                        game.id() + " event " + event.id() + " has no gameplay dispatcher");
            }
        }
    }

    @Test
    void milkTheCowHerdEventUsesTheLiveCowRoleAndConfiguredHerdRules() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        int dispatcher = source.indexOf("type == ArcadePortGame.MILK_THE_COW && \"spawn-herd\".equals(eventId)");
        int update = source.indexOf("private void updateMilkCowHerd(");
        int nextMethod = source.indexOf("private void ", update + 1);
        assertTrue(dispatcher >= 0);
        assertTrue(update > dispatcher);
        assertTrue(nextMethod > update);
        String herdUpdater = source.substring(update, nextMethod);
        assertTrue(herdUpdater.contains("session.teams.getOrDefault(cowPlayer.getUniqueId(), 0) != 1"));
        assertTrue(herdUpdater.contains("MILK_THE_COW_HERD_SIZE"));
        assertTrue(herdUpdater.contains("MILK_THE_COW_HERD_MOVE_SPEED"));
        assertTrue(herdUpdater.contains("session.world.spawn(spawn, Cow.class)"));
    }

    @Test
    void sheepQuestSpawnEventUsesNormalConfiguredMapSpawnPath() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        assertTrue(source.contains("type == ArcadePortGame.SHEEP_QUEST && \"spawn-sheep\".equals(eventId)"));
        int spawn = source.indexOf("private void spawnSheep(Session session)");
        int configure = source.indexOf("private void configureQuestSheep", spawn);
        assertTrue(spawn >= 0 && configure > spawn);
        String spawner = source.substring(spawn, configure);
        assertTrue(spawner.contains("mapData.dataLocations(\"WHITE\")"));
        assertTrue(spawner.contains("configureQuestSheep(session"));
    }

    @Test
    void skyWarsGeneratorEventSharesTheNormalTntDropPath() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        assertTrue(source.contains("type == ArcadePortGame.SKY_WARS && \"spawn-tnt\".equals(eventId)"));
        int updater = source.indexOf("private void updateSkyWars(Session session, long now)");
        int helper = source.indexOf("private boolean spawnSkyWarsTnt(Session session, long now)");
        int nextMethod = source.indexOf("private void ", helper + 1);
        assertTrue(updater >= 0 && helper > updater && nextMethod > helper);
        assertTrue(source.substring(updater, helper).contains("spawnSkyWarsTnt(session, now)"));
        String generator = source.substring(helper, nextMethod);
        assertTrue(generator.contains("session.skyWarsTntGenerator"));
        assertTrue(generator.contains("session.world.dropItem"));
        assertTrue(generator.contains("SkyWarsMixedArcadeRules.TNT_GENERATOR_INTERVAL_MILLIS"));
    }

    @Test
    void baconBrawlDebugMeterFillsTheSelectedAbilityAndRefreshesItsHud() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        int dispatch = source.indexOf("type == ArcadePortGame.BACON_BRAWL && \"fill-meter\".equals(eventId)");
        assertTrue(dispatch >= 0);
        int nextEvent = source.indexOf("if (type == ArcadePortGame.WITHER_ASSAULT", dispatch);
        String fill = source.substring(dispatch, nextEvent);
        assertTrue(fill.contains("session.baconMeters.put(tester.getUniqueId(), meter)"));
        assertTrue(fill.contains("updateBaconMeterItem(session, tester)"));
        assertTrue(fill.contains("updateBaconMeterScoreboard(session)"));
        assertTrue(fill.contains("markDebugSession()"));
    }

    @Test
    void castleAssaultObjectiveEventRequiresVacantObjectiveAndUsesNormalSpawner() throws IOException {
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "CastleAssaultHeroesOfGwenRuntime.java");
        if (!Files.isRegularFile(runtimePath)) {
            runtimePath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "CastleAssaultHeroesOfGwenRuntime.java");
        }
        String source = Files.readString(runtimePath);
        int method = source.indexOf("boolean debugSpawnObjectiveTnt(Player tester)");
        int nextMethod = source.indexOf("private void tickCastleAssault", method);
        assertTrue(method >= 0 && nextMethod > method);
        String debug = source.substring(method, nextMethod);
        assertTrue(debug.contains("objectiveItem != null && objectiveItem.isValid()"));
        assertTrue(debug.contains("objectiveCarrier != null"));
        assertTrue(debug.contains("spawnObjectiveTnt()"));
        assertTrue(debug.contains("activePlayers.contains(tester.getUniqueId())"));
    }

    @Test
    void kitRefreshIsOnlyOfferedForGamesWithSharedKitSetup() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            boolean offered = GameDebugEventCatalog.events(game).stream()
                    .anyMatch(event -> event.id().equals("refresh-kit"));
            assertEquals(GameDebugEventCatalog.supportsKitRefresh(game), offered, game.id());
        }
    }

    @Test
    void standaloneSpeedBuildersMatchUsesConfiguredBotRosterAndLoadouts() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String source = Files.readString(sourceRoot.resolve("SpeedBuildersGame.java"));

        assertTrue(source.contains("manager.configuredTestBotCount(ArcadePortGame.SPEED_BUILDERS)"));
        assertTrue(source.contains("testPlayers.spawnForMatch(botsNeeded"));
        assertTrue(source.contains("manager.applyConfiguredTestBotLoadout(ArcadePortGame.SPEED_BUILDERS, index, bot)"));
    }

    @Test
    void speedBuildersProgressWritersSkipDebugRounds() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String source = Files.readString(sourceRoot.resolve("SpeedBuildersGame.java"));
        int matchStart = source.indexOf("private void persistMatchProgress(");
        int playerStart = source.indexOf("private void persistPlayerProgress(", matchStart);
        int scoreStart = source.indexOf("private int score(", playerStart);
        assertTrue(matchStart >= 0 && playerStart > matchStart && scoreStart > playerStart);
        String matchWriter = source.substring(matchStart, playerStart);
        String playerWriter = source.substring(playerStart, scoreStart);

        assertTrue(matchWriter.indexOf("GameDebugRuntimeOverrides.isDebugSession()")
                < matchWriter.indexOf("SpeedBuildersProgressStore.increment"));
        assertTrue(playerWriter.indexOf("GameDebugRuntimeOverrides.isDebugSession()")
                < playerWriter.indexOf("SpeedBuildersProgressStore.increment"));
    }

    @Test
    void achievementPreviewIsAuthorisedGameScopedAndDoesNotWriteProgress() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        int start = manager.indexOf("boolean previewAchievementForTester(");
        int end = manager.indexOf("private void awardBombLobbersAchievement(", start);
        assertTrue(start >= 0 && end > start);
        String preview = manager.substring(start, end);

        assertTrue(lobby.contains("debug-open:achievements"));
        assertTrue(lobby.contains("ArcadeProgressCatalog.achievements(game.id())"));
        assertTrue(preview.contains("!plugin.hasAdminAccess(tester)"));
        assertTrue(preview.contains("!tester.getUniqueId().equals(debugContextOwner)"));
        assertTrue(preview.contains("game != debugContextGame"));
        assertTrue(preview.contains("ArcadeProgressCatalog.achievements(game.id())"));
        assertFalse(preview.contains("plugin.getConfig().set"));
        assertFalse(preview.contains("plugin.saveConfig"));
    }

    @Test
    void debugAndBotMenusAndRuntimeMutationsRemainTesterOnly() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));

        assertTrue(lobby.contains("isDebugMenuTitle(title) && !plugin.hasAdminAccess(p)"));
        assertTrue(lobby.contains("title.equals(TEST_BOTS_TITLE) || title.equals(TEST_BOT_LOADOUT_TITLE)"));
        assertTrue(lobby.contains("&& !plugin.hasAdminAccess(p)"));
        assertTrue(manager.contains("toggleDebugOverride(Player player, ArcadePortGame type, String flag, boolean fallback)"));
        assertTrue(manager.contains("!plugin.hasAdminAccess(player)"));
        assertTrue(manager.contains("debugTriggerEvent(Player tester, ArcadePortGame type, String eventId)"));
        assertTrue(manager.contains("!plugin.hasAdminAccess(tester)"));
        assertTrue(manager.contains("debugAdvanceStartPhase(Player tester, ArcadePortGame type)"));
    }
}
