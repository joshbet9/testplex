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
        assertEquals(List.of("advance-dragon", "use-kit-ability"),
                GameDebugEventCatalog.events(ArcadePortGame.DRAGON_ESCAPE).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals("begin-evolution", GameDebugEventCatalog.events(ArcadePortGame.EVOLUTION).getFirst().id());
        assertEquals("shoot-ball", GameDebugEventCatalog.events(ArcadePortGame.HOOPS_REMASTERED).getFirst().id());
        assertEquals("roll-item", GameDebugEventCatalog.events(ArcadePortGame.SNAKE).getFirst().id());
        assertEquals(List.of("take-photo", "use-kit-ability"),
                GameDebugEventCatalog.events(ArcadePortGame.CAMERA_SHY).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals("reset-own-plot", GameDebugEventCatalog.events(ArcadePortGame.MASTER_BUILDERS).getFirst().id());
        assertEquals("refill-arrow", GameDebugEventCatalog.events(ArcadePortGame.ONE_IN_THE_QUIVER)
                .getFirst().id());
        assertEquals("restore-armour", GameDebugEventCatalog.events(ArcadePortGame.PAINTBALL)
                .getFirst().id());
        assertEquals("promote-self", GameDebugEventCatalog.events(ArcadePortGame.DEATH_TAG)
                .getFirst().id());
        assertEquals("forfeit-duel", GameDebugEventCatalog.events(ArcadePortGame.GLADIATORS)
                .getFirst().id());
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.SPLEEF).stream()
                .anyMatch(event -> event.id().equals("refresh-kit")));
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.SPLEEF).stream()
                .anyMatch(event -> event.id().equals("restore-hunger")));
        assertEquals("use-special", GameDebugEventCatalog.events(ArcadePortGame.SUPER_SPLOOR)
                .getFirst().id());
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
    void cameraShyDebugPhotoUsesTheNormalFlashPathAndIsolatedProgressGuard() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String game = Files.readString(sourceRoot.resolve("CameraShyGame.java"));
        assertTrue(manager.contains("type == ArcadePortGame.CAMERA_SHY && \"take-photo\".equals(eventId)"));
        assertTrue(manager.contains("GameDebugRuntimeOverrides.markDebugSession();\n            return cameraShyModule.debugTakePhoto(tester);"));
        int debugPhoto = game.indexOf("public boolean debugTakePhoto(Player player)");
        int nextMethod = game.indexOf("public int queuedCount()", debugPhoto);
        assertTrue(debugPhoto >= 0 && nextMethod > debugPhoto);
        assertTrue(game.substring(debugPhoto, nextMethod).contains("triggerPhoto(player)"));
        int trigger = game.indexOf("private void triggerPhoto(Player player)");
        int end = game.indexOf("@EventHandler", trigger);
        assertTrue(trigger >= 0 && end > trigger);
        String photo = game.substring(trigger, end);
        assertTrue(photo.contains("emitCameraFlash(player.getLocation())"));
        assertTrue(photo.contains("takePhoto(player)"));
        assertTrue(game.contains("private void award(UUID id, String achievementId) {\n        if (GameDebugRuntimeOverrides.isDebugSession()) return;"));
        assertTrue(game.contains("private void recordMatchProgress(UUID winnerId) {\n        if (GameDebugRuntimeOverrides.isDebugSession()) return;"));
        assertTrue(manager.contains("type == ArcadePortGame.CAMERA_SHY && \"use-kit-ability\".equals(eventId)"));
        int debugAbility = game.indexOf("public boolean debugUseSelectedKitAbility(Player player)");
        int next = game.indexOf("public int queuedCount()", debugAbility);
        assertTrue(debugAbility >= 0 && next > debugAbility);
        assertTrue(game.substring(debugAbility, next).contains("activateAbility(player, kit)"));
    }

    @Test
    void superSploorSpecialSwitchesReachEachKitActivationAndRequireAuthorisedProfileBridge() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String plugin = Files.readString(sourceRoot.resolve("ArcadeMechanicsModernPlugin.java"));
        String runtime = Files.readString(sourceRoot.resolve("SuperSploorRuntime.java"));
        for (String kit : List.of("sprayer", "roller", "charger", "lobber", "dualies")) {
            assertTrue(lobby.contains("\"" + kit + "\""), kit + " Special switch is offered");
        }
        assertTrue(plugin.contains("isArcadeDebugFeatureEnabled(Player player, String feature, boolean fallback)"));
        assertTrue(plugin.contains("hasAdminAccess(player)"));
        assertTrue(runtime.contains("debugSpecialEnabled(player, \"special-\" + kit)"));
        assertTrue(runtime.contains("isArcadeDebugFeatureEnabled"));
    }

    @Test
    void snakeItemRollDebugEventRunsTheRealRouletteForAlivePlayers() throws IOException {
        Path snakeSource = Path.of("..", "snake-port", "src", "main", "java", "au", "com", "testplex",
                "snake", "SnakeGameManager.java");
        if (!Files.isRegularFile(snakeSource)) {
            snakeSource = Path.of("modern-platform", "snake-port", "src", "main", "java", "au", "com",
                    "testplex", "snake", "SnakeGameManager.java");
        }
        Path managerSource = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerSource)) {
            managerSource = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String snake = Files.readString(snakeSource);
        String manager = Files.readString(managerSource);
        int start = snake.indexOf("public boolean debugRollItem(Player player)");
        int end = snake.indexOf("public void showStats", start);
        assertTrue(start >= 0 && end > start);
        String event = snake.substring(start, end);
        assertTrue(event.contains("state.alive"));
        assertTrue(event.contains("state.rouletteActive"));
        assertTrue(event.contains("GameDebugRuntimeOverrides.markDebugSession()"));
        assertTrue(event.contains("beginRoulette(state)"));
        assertTrue(manager.contains("type == ArcadePortGame.SNAKE && \"roll-item\".equals(eventId)"));
    }

    @Test
    void dragonEscapeKitDebugEventInvokesSelectedNormalAbilityWithValidTargets() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        assertTrue(source.contains("type == ArcadePortGame.DRAGON_ESCAPE && \"use-kit-ability\".equals(eventId)"));
        int start = source.indexOf("private boolean debugUseDragonEscapeKitAbility(");
        int end = source.indexOf("boolean debugRestoreTesterVitals(", start);
        assertTrue(start >= 0 && end > start);
        String helper = source.substring(start, end);
        assertTrue(helper.contains("useDragonEscapeLeap(session, tester)"));
        assertTrue(helper.contains("useDragonEscapeWarper(session, tester)"));
        assertTrue(helper.contains("Block target = tester.getTargetBlockExact(6)"));
        assertTrue(helper.contains("if (target == null) return false"));
        assertTrue(helper.contains("updateDragonEscapeResources(session"));
    }

    @Test
    void dragonEscapeAbilitySwitchesArePerKitImmediateAndConsumedByEveryAbilityPath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("game == ArcadePortGame.DRAGON_ESCAPE"));
        assertTrue(lobby.contains("List.of(\"jumper\", \"warper\", \"digger\", \"disruptor\")"));
        assertTrue(lobby.contains("game == ArcadePortGame.DRAGON_ESCAPE && key.startsWith(\"ability-\")"));
        for (String methodName : List.of("useDragonEscapeLeap", "useDragonEscapeWarper", "useDragonEscapeDigger")) {
            int method = manager.indexOf("private void " + methodName + "(");
            int body = manager.indexOf("{", method);
            assertTrue(method >= 0 && body > method, methodName + " exists");
            assertTrue(manager.substring(body, body + 260).contains("isDebugAbilityAllowed"),
                    methodName + " consumes the tester's ability switch");
        }
        assertTrue(manager.contains("private boolean isDebugAbilityAllowed(Player player, ArcadePortGame game, String kit)"));
        assertTrue(manager.contains("throwDragonEscapeMine(session, player)"));
        assertTrue(manager.contains("isDebugAbilityAllowed(player, ArcadePortGame.DRAGON_ESCAPE, kit)"));
    }

    @Test
    void dragonsAbilitySwitchesReachTheSixKitActivationPaths() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"coward\", \"frost\", \"glider\", \"tamer\", \"pyrotechnic\", \"marksman\")"));
        for (String methodName : List.of("useDragonsLeap", "activateDragonFrostBridge", "activateDragonGlider",
                "activateDragonTamer")) {
            int method = manager.indexOf("private void " + methodName + "(");
            int body = manager.indexOf("{", method);
            assertTrue(method >= 0 && body > method, methodName + " exists");
            assertTrue(manager.substring(body, body + 220).contains("isDebugAbilityAllowed"),
                    methodName + " consumes its immediate ability switch");
        }
        assertTrue(manager.contains("isDebugAbilityAllowed(player, ArcadePortGame.DRAGONS, \"pyrotechnic\")"));
        assertTrue(manager.contains("isDebugAbilityAllowed(marksman, ArcadePortGame.DRAGONS, \"marksman\")"));
        assertTrue(manager.contains("game == ArcadePortGame.DRAGONS && key.startsWith(\"ability-\")"));
    }

    @Test
    void quiverKitPerksAreToggleableAndConsumedByTheirRuntimePaths() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"brawler\", \"enchanter\", \"slam-shot\", \"ninja\")"));
        assertTrue(lobby.contains("game == ArcadePortGame.ONE_IN_THE_QUIVER && key.startsWith(\"ability-\")"));
        for (String hook : List.of("ArcadePortGame.ONE_IN_THE_QUIVER, \"slam-shot\"",
                "ArcadePortGame.ONE_IN_THE_QUIVER, \"enchanter\"",
                "ArcadePortGame.ONE_IN_THE_QUIVER, \"brawler\"",
                "ArcadePortGame.ONE_IN_THE_QUIVER, \"ninja\"")) {
            assertTrue(manager.contains("isDebugAbilityAllowed(") && manager.contains(hook),
                    hook + " has a runtime consumer");
        }
    }

    @Test
    void paintballReloadDebugSwitchBypassesCooldownAndMachineGunHeatForTesters() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("\"ignore-reload\""));
        int fire = manager.indexOf("private void firePaintballWeapon(");
        int next = manager.indexOf("private boolean isPaintballWaterBomb(", fire);
        assertTrue(fire >= 0 && next > fire);
        String method = manager.substring(fire, next);
        assertTrue(method.contains("plugin.hasAdminAccess(player)"));
        assertTrue(method.contains("debugFeatureEnabled(playerId, ArcadePortGame.PAINTBALL, \"ignore-reload\", false)"));
        assertTrue(method.contains("if (!ignoreReload && lastShot != null && now - lastShot < cooldown) return;"));
        assertTrue(method.contains("if (!ignoreReload && \"machine-gun\".equals(kit)"));
    }

    @Test
    void spleefKitSwitchesReachSnowballArrowBrawlerAndGustRuntimePaths() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"snowballer\", \"archer\", \"brawler\", \"gust\")"));
        assertTrue(lobby.contains("game == ArcadePortGame.SPLEEF && key.startsWith(\"ability-\")"));
        for (String key : List.of("ability-snowballer", "ability-archer", "ability-brawler", "ability-gust")) {
            assertTrue(manager.contains("ArcadePortGame.SPLEEF, \"" + key + "\""), key + " reaches gameplay");
        }
        assertTrue(manager.contains("public void onSpleefSpecialLaunch(ProjectileLaunchEvent event)"));
        assertTrue(manager.contains("event.setCancelled(true)"));
    }

    @Test
    void runnerKitSwitchesGateLeapQuickshotFrostBallsAndPositionSwap() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"leaper\", \"archer\", \"frosty\", \"switcher\")"));
        for (String key : List.of("leaper", "archer", "frosty", "switcher")) {
            assertTrue(manager.contains("ArcadePortGame.RUNNER, \"ability-" + key + "\"")
                            || manager.contains("ArcadePortGame.RUNNER, \"" + key + "\""),
                    key + " has a runtime consumer");
        }
        assertTrue(manager.contains("public boolean debugTriggerEvent(Player tester, ArcadePortGame type, String eventId)"));
        assertTrue(manager.contains("triggered = useRunnerPositionSwap(session, tester, System.currentTimeMillis())"));
    }

    @Test
    void deathTagKitSwitchesGateCombatArrowsAndTricksterAbilitiesForTesters() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"basher\", \"archer\", \"traitor\", \"turtle\", \"trickster\")"));
        assertTrue(lobby.contains("game == ArcadePortGame.DEATH_TAG && key.startsWith(\"ability-\")"));
        for (String kit : List.of("basher", "archer", "traitor", "turtle", "trickster")) {
            assertTrue(manager.contains("ArcadePortGame.DEATH_TAG, \"ability-" + kit + "\""),
                    kit + " has a runtime consumer");
        }
        assertTrue(manager.contains("private void useDeathTagTricksterSwap(Session session, Player runner) {\n"
                + "        if (!isDebugAbilityAllowed(runner, ArcadePortGame.DEATH_TAG, \"trickster\")) return;"));
        assertTrue(manager.contains("private void useDeathTagTricksterFireworks(Session session, Player runner) {\n"
                + "        if (!isDebugAbilityAllowed(runner, ArcadePortGame.DEATH_TAG, \"trickster\")) return;"));
        assertTrue(manager.contains("plugin.hasAdminAccess(player) && !debugFeatureEnabled(player.getUniqueId(),\n"
                + "                ArcadePortGame.DEATH_TAG, \"ability-archer\", true)"));
    }

    @Test
    void evolutionFormSwitchesGateAbilitiesAndQuickEvolverProgression() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"golem\", \"blaze\", \"creeper\", \"slime\", \"chicken\", \"quick-evolver\")"));
        assertTrue(lobby.contains("game == ArcadePortGame.EVOLUTION && key.startsWith(\"ability-\")"));
        for (String ability : List.of("golem", "blaze", "creeper", "slime", "chicken", "quick-evolver")) {
            assertTrue(manager.contains("ArcadePortGame.EVOLUTION, \"ability-" + ability + "\"")
                            || manager.contains("ArcadePortGame.EVOLUTION, \"" + ability + "\""),
                    ability + " has a runtime consumer");
        }
        assertTrue(manager.contains("private void startEvolutionGolemSlam(Session session, Player player, long now) {\n"
                + "        if (!isDebugAbilityAllowed(player, ArcadePortGame.EVOLUTION, \"golem\")) return;"));
        assertTrue(manager.contains("private boolean throwEvolutionSulphurBomb(Session session, Player player) {\n"
                + "        if (!isDebugAbilityAllowed(player, ArcadePortGame.EVOLUTION, \"creeper\")) return false;"));
    }

    @Test
    void sheepQuestKitSwitchesGateLeapArrowsAndShepherdCarryEffects() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"beserker\", \"archer\", \"shepherd\")"));
        for (String kit : List.of("beserker", "archer", "shepherd")) {
            assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.SHEEP_QUEST, \"" + kit + "\")"),
                    kit + " has a runtime consumer");
        }
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.SHEEP_QUEST, \"shepherd\")"));
        assertTrue(manager.contains("public void onSheepQuestArcherBowShoot(EntityShootBowEvent event)"));
        assertTrue(manager.contains("Archer abilities are disabled in your debug profile."));
        assertTrue(manager.contains("public void onSheepQuestBerserkerJump(PlayerToggleFlightEvent event)"));
    }

    @Test
    void superSploorSpecialDebugEventFillsAndUsesTheNormalSelectedSpecial() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "SuperSploorRuntime.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
            runtimePath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "SuperSploorRuntime.java");
        }
        String manager = Files.readString(managerPath);
        String runtime = Files.readString(runtimePath);
        assertTrue(manager.contains("type == ArcadePortGame.SUPER_SPLOOR && \"use-special\".equals(eventId)"));
        assertTrue(manager.contains("superSploor.debugUseSelectedSpecial(tester)"));
        assertTrue(runtime.contains("public boolean debugUseSelectedSpecial(Player tester)"));
        assertTrue(runtime.contains("specialCharge.put(id, 100.0D)"));
        assertTrue(runtime.contains("useSpecial(tester, id)"));
    }

    @Test
    void turfWarsKitSwitchesGateInfiltratorShredderAndFortifierPerks() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"infiltrator\", \"shredder\", \"fortifier\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.TURF_WARS, \"infiltrator\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.TURF_WARS, \"shredder\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.TURF_WARS, \"fortifier\")"));
        assertTrue(manager.contains("session.turfBarrageFiring.remove(id)"));
        assertTrue(manager.contains("private void startTurfWarsBarrage(Session session, Player player) {\n"
                + "        if (!isDebugAbilityAllowed(player, ArcadePortGame.TURF_WARS, \"shredder\")) return;"));
    }

    @Test
    void microBattlesDebugSwitchesGateBlocksAndKitAbilities() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"archer\", \"fighter\", \"dasher\")"));
        assertTrue(lobby.contains("features.put(\"blocks-place\""));
        assertTrue(lobby.contains("features.put(\"blocks-break\""));
        assertTrue(manager.contains("debugKitFeatureEnabled(targetPlayer, ArcadePortGame.MICRO_BATTLES, \"fighter\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.MICRO_BATTLES, \"archer\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.MICRO_BATTLES, \"dasher\")"));
        assertTrue(manager.contains("ArcadePortGame.MICRO_BATTLES,\n                        \"blocks-place\", true)"));
        assertTrue(manager.contains("ArcadePortGame.MICRO_BATTLES,\n                        \"blocks-break\", true)"));
    }

    @Test
    void gladiatorsDebugForfeitUsesTheNormalDeathAndBracketResolutionPath() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String manager = Files.readString(managerPath);
        assertTrue(manager.contains("type == ArcadePortGame.GLADIATORS && \"forfeit-duel\".equals(eventId)"));
        assertTrue(manager.contains("session.gladiatorPairs.containsKey(tester.getUniqueId())"));
        assertTrue(manager.contains("tester.setHealth(0.0D)"));
        assertTrue(manager.contains("GameDebugRuntimeOverrides.markDebugSession();\n            tester.setHealth(0.0D)"));
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
    void bombLobbersMenuSwitchesGateTheRealKitMechanics() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
            runtimePath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
        }
        String manager = Files.readString(managerPath);
        String runtime = Files.readString(runtimePath);
        assertTrue(runtime.contains("\"double-jump\", \"Double jump\""));
        assertTrue(runtime.contains("\"pitch-adjustment\", \"TNT pitch control\""));
        assertTrue(runtime.contains("\"block-wall\", \"Waller block wall\""));
        assertTrue(manager.contains("debugFeatureEnabled(player.getUniqueId(), ArcadePortGame.BOMB_LOBBERS, \"pitch-adjustment\", true)"));
        assertTrue(manager.contains("debugFeatureEnabled(player.getUniqueId(), ArcadePortGame.BOMB_LOBBERS, \"block-wall\", true)"));
        assertTrue(manager.contains("debugFeatureEnabled(id, ArcadePortGame.BOMB_LOBBERS, \"double-jump\", true)"));
    }

    @Test
    void castleAssaultDebugSwitchesGateObjectiveDamageAndTnt() throws IOException {
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "CastleAssaultHeroesOfGwenRuntime.java");
        Path lobbyPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        if (!Files.isRegularFile(runtimePath)) {
            runtimePath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "CastleAssaultHeroesOfGwenRuntime.java");
            lobbyPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
        }
        String runtime = Files.readString(runtimePath);
        String lobby = Files.readString(lobbyPath);
        assertTrue(lobby.contains("\"objective-tnt\", \"Objective TNT\""));
        assertTrue(lobby.contains("\"crystal-damage\", \"Sentry crystal damage\""));
        assertTrue(lobby.contains("\"king-damage\", \"King damage\""));
        assertTrue(runtime.contains("!debugFeatureEnabled(tester, \"objective-tnt\")"));
        assertTrue(runtime.contains("!debugFeatureEnabled(attacker, \"crystal-damage\")"));
        assertTrue(runtime.contains("!debugFeatureEnabled(attacker, \"king-damage\")"));
        assertTrue(runtime.contains("!debugFeatureEnabled(player, \"objective-tnt\")"));
    }

    @Test
    void heroesOfGwenDebugSwitchesGateEveryHeroSkillSet() throws IOException {
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "CastleAssaultHeroesOfGwenRuntime.java");
        Path lobbyPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        if (!Files.isRegularFile(runtimePath)) {
            runtimePath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "CastleAssaultHeroesOfGwenRuntime.java");
            lobbyPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
        }
        String runtime = Files.readString(runtimePath);
        String lobby = Files.readString(lobbyPath);
        assertTrue(lobby.contains("List.of(\"hattori\", \"bardolf\", \"devon\", \"rowena\", \"anath\", \"larissa\", \"dana\", \"biff\", \"ivy\")"));
        assertTrue(runtime.contains("\"hero-\" + skill.substring(0, separator) + \"-abilities\""));
        assertTrue(runtime.contains("This hero's abilities are disabled in your debug profile."));
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
