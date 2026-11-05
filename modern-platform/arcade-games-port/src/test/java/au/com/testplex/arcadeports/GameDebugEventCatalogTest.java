package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameDebugEventCatalogTest {
    @Test
    void eventToolsAreExplicitlyScopedToTheirGame() {
        var witherEvents = GameDebugEventCatalog.events(ArcadePortGame.WITHER_ASSAULT);
        assertEquals(1, witherEvents.size());
        assertEquals("spawn-minions", witherEvents.getFirst().id());
        assertEquals("reveal-letter", GameDebugEventCatalog.events(ArcadePortGame.DRAW_MY_THING).getFirst().id());
        assertEquals("spawn-dragon", GameDebugEventCatalog.events(ArcadePortGame.DRAGONS).getFirst().id());
        assertEquals("advance-dragon", GameDebugEventCatalog.events(ArcadePortGame.DRAGON_ESCAPE).getFirst().id());
        assertEquals("begin-evolution", GameDebugEventCatalog.events(ArcadePortGame.EVOLUTION).getFirst().id());
        assertEquals("shoot-ball", GameDebugEventCatalog.events(ArcadePortGame.HOOPS_REMASTERED).getFirst().id());
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
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.MICRO_BATTLES).stream()
                .anyMatch(event -> event.id().equals("drop-barriers")));
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
    void baconBrawlPerKitAbilitySwitchesAreTesterScopedAndReachActivation() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        for (String kit : List.of("mama-piggles", "chris-p-bacon", "pig", "brutus-bacon",
                "el-muchacho-pigo")) {
            assertTrue(lobby.contains("\"" + kit + "\""), kit + " is configurable in the game-specific menu");
        }
        int activation = manager.indexOf("private void useBaconBrawlAbility(Session session, Player player, long now, boolean crouchDash)");
        int nextMethod = manager.indexOf("private void useCrispyBaconDash(", activation);
        assertTrue(activation >= 0 && nextMethod > activation);
        String ability = manager.substring(activation, nextMethod);
        assertTrue(ability.contains("String abilityFeature = \"ability-\" + kit"));
        assertTrue(ability.contains("plugin.hasAdminAccess(player)"));
        assertTrue(ability.contains("debugFeatureEnabled(id, ArcadePortGame.BACON_BRAWL, abilityFeature, true)"));
        assertTrue(ability.indexOf("debugFeatureEnabled") < ability.indexOf("session.baconAbilitiesUsed.add(id)"));
    }

    @Test
    void hoopsShotEventUsesNormalPossessionPathAndDebugRoundsDoNotPersistCareerOrAchievements() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String runtime = Files.readString(sourceRoot.resolve("HoopsRemasteredRuntime.java"));
        assertTrue(manager.contains("type == ArcadePortGame.HOOPS_REMASTERED && \"shoot-ball\".equals(eventId)"));
        int debugShot = runtime.indexOf("public boolean debugShoot(Player shooter)");
        int nextMethod = runtime.indexOf("@EventHandler", debugShot);
        assertTrue(debugShot >= 0 && nextMethod > debugShot);
        String shot = runtime.substring(debugShot, nextMethod);
        assertTrue(shot.contains("shooter.getUniqueId().equals(current.holderId)"));
        assertTrue(shot.contains("shoot(current, shooter)"));
        int persist = runtime.indexOf("private void persistCareer(");
        int award = runtime.indexOf("private void award(Match current, UUID id, Player player", persist);
        assertTrue(persist >= 0 && award > persist);
        assertTrue(runtime.substring(persist, award).contains("if (GameDebugRuntimeOverrides.isDebugSession()) return;"));
        int awardGuard = runtime.indexOf("if (GameDebugRuntimeOverrides.isDebugSession()) return;", award);
        assertTrue(awardGuard > award);
    }

    @Test
    void speedBuildersPerfectBuildProgressDoesNotPersistInDebugRounds() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String source = Files.readString(sourceRoot.resolve("SpeedBuildersGame.java"));
        int start = source.indexOf("private void markPerfectBuild(PlayerState state)");
        int end = source.indexOf("private void checkPerfectBuild(", start);
        assertTrue(start >= 0 && end > start);
        String method = source.substring(start, end);
        int guard = method.indexOf("if (!GameDebugRuntimeOverrides.isDebugSession())");
        assertTrue(guard >= 0);
        assertTrue(method.indexOf("SpeedBuildersProgressStore.increment") > guard);
        assertTrue(method.indexOf("plugin.saveConfig()") > guard);
        assertTrue(method.indexOf("Bukkit.broadcastMessage") > guard);
    }

    @Test
    void snakeSeparateStatsStoreRejectsEveryProgressWriteDuringDebugRounds() throws IOException {
        Path source = Path.of("..", "snake-port", "src", "main", "java", "au", "com", "testplex",
                "snake", "SnakeStatsStore.java");
        if (!Files.isRegularFile(source)) {
            source = Path.of("modern-platform", "snake-port", "src", "main", "java", "au", "com",
                    "testplex", "snake", "SnakeStatsStore.java");
        }
        String store = Files.readString(source);
        for (String methodName : List.of("recordKill", "recordSlime", "recordTailLength", "awardAchievement",
                "recordGameEnd", "publishToArcadeStats")) {
            int start = store.indexOf(" " + methodName + "(");
            int end = store.indexOf("\n    }", start);
            assertTrue(start >= 0 && end > start, methodName + " exists");
            assertTrue(store.substring(start, end).contains("GameDebugRuntimeOverrides.isDebugSession()"),
                    methodName + " is isolated during debug rounds");
        }
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
    void runnerSwapDebugEventUsesTheNormalEligibleTargetAndGracePath() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        assertTrue(source.contains("type == ArcadePortGame.RUNNER && \"use-position-swap\".equals(eventId)"));
        int swap = source.indexOf("private boolean useRunnerPositionSwap(");
        int nextMethod = source.indexOf("private boolean canRunnerSwapFrom(", swap);
        assertTrue(swap >= 0 && nextMethod > swap);
        String ability = source.substring(swap, nextMethod);
        assertTrue(ability.contains("canRunnerSwapFrom(session, target)"));
        assertTrue(ability.contains("session.runnerSwapUsed.add(id)"));
        assertTrue(ability.contains("protectRunnerSwapLanding(session, player, graceUntil)"));
        assertTrue(ability.contains("protectRunnerSwapLanding(session, target, graceUntil)"));
    }

    @Test
    void bombLobbersPowerupEventUsesNormalSpawnerAndHonoursDisabledFeature() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        assertTrue(source.contains("type == ArcadePortGame.BOMB_LOBBERS && \"spawn-powerups\".equals(eventId)"));
        int updater = source.indexOf("private void updateBombLobbersPowerups(Session session, long now)");
        int featureGate = source.indexOf("if (!ArcadePortRules.bombLobbersPowerupsEnabled())", updater);
        int wave = source.indexOf("String[] choices = {\"wind-charge\"", featureGate);
        assertTrue(updater >= 0 && featureGate > updater && wave > featureGate);
        assertTrue(source.substring(updater, wave).contains("session.bombPowerupDrops.clear()"));
    }

    @Test
    void heroesOfGwenDebugEventStartsTheNormalPairedMinionWave() throws IOException {
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "CastleAssaultHeroesOfGwenRuntime.java");
        if (!Files.isRegularFile(runtimePath)) {
            runtimePath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "CastleAssaultHeroesOfGwenRuntime.java");
        }
        String source = Files.readString(runtimePath);
        int method = source.indexOf("boolean debugStartHeroesOfGwenMinionWave(Player tester)");
        int nextMethod = source.indexOf("private void tickCastleAssault", method);
        assertTrue(method >= 0 && nextMethod > method);
        String debug = source.substring(method, nextMethod);
        assertTrue(debug.contains("activePlayers.contains(tester.getUniqueId())"));
        assertTrue(debug.contains("minionWavePending = true"));
        assertTrue(debug.contains("updateHeroesOfGwenMinions(now)"));
        assertTrue(debug.contains("heroesOfGwenMinionTeams.size() > before"));
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

    @Test
    void booleanRuleSwitchesReachTheRuntimeOverrideAndShowTheirApplyMode() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        assertTrue(manager.contains("debugProfiles.setBooleanVariable(player.getUniqueId(), type, flag, enabled)"));
        assertTrue(manager.contains("GameDebugRuntimeOverrides.setValue(flag, enabled)"));
        assertTrue(lobby.contains("GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(game, key))"));
    }

    @Test
    void microBattlesBarrierEventUsesTheNormalBarrierReleasePath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(manager.contains("\"drop-barriers\".equals(eventId)"));
        assertTrue(manager.contains("releaseMicroBattlesBarrier(session);"));
        assertTrue(manager.contains("private void updateMicroBarrier(Session session, long now)"));
        assertTrue(manager.contains("private void releaseMicroBattlesBarrier(Session session)"));
    }
}
