package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameDebugEventCatalogTest {
    @Test
    void everyGameHasAtLeastOneGameScopedEventTool() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            assertFalse(GameDebugEventCatalog.events(game).isEmpty(),
                    game.id() + " needs at least one meaningful event tool");
        }
    }

    @Test
    void everyGameAchievementFitsInThePreviewMenu() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            assertTrue(ArcadeProgressCatalog.achievements(game.id()).size() <= 36,
                    game.id() + " achievement previews must not be hidden past the menu's last slot");
        }
    }

    @Test
    void eventToolsAreExplicitlyScopedToTheirGame() {
        var witherEvents = GameDebugEventCatalog.events(ArcadePortGame.WITHER_ASSAULT);
        assertEquals(List.of("spawn-minions", "fire-skull", "refresh-kit"), witherEvents.stream()
                .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals("spawn-minions", witherEvents.getFirst().id());
        assertEquals(List.of("spawn-powerups", "throw-held-bomb", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.BOMB_LOBBERS).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals("reveal-letter", GameDebugEventCatalog.events(ArcadePortGame.DRAW_MY_THING).getFirst().id());
        assertEquals(List.of("spawn-dragon", "use-kit-ability", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.DRAGONS).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals(List.of("advance-dragon", "use-kit-ability", "finish-run", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.DRAGON_ESCAPE).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals(List.of("begin-evolution", "make-pity-ready", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.EVOLUTION).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals(List.of("shoot-ball", "try-steal", "drop-rebound-ball"),
                GameDebugEventCatalog.events(ArcadePortGame.HOOPS_REMASTERED).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals("roll-item", GameDebugEventCatalog.events(ArcadePortGame.SNAKE).getFirst().id());
        assertEquals("use-kit-ability", GameDebugEventCatalog.events(ArcadePortGame.SNEAKY_ASSASSINS)
                .getFirst().id());
        assertEquals(List.of("take-photo", "use-kit-ability"),
                GameDebugEventCatalog.events(ArcadePortGame.CAMERA_SHY).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals("reset-own-plot", GameDebugEventCatalog.events(ArcadePortGame.MASTER_BUILDERS).getFirst().id());
        assertEquals(List.of("refill-arrow", "use-kit-ability", "test-bot-kill", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.ONE_IN_THE_QUIVER).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals(List.of("restore-armour", "fire-shot", "throw-water-bomb", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.PAINTBALL).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals(List.of("promote-self", "use-kit-ability", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.DEATH_TAG).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.DEATH_TAG).stream()
                .anyMatch(event -> event.id().equals("refresh-kit")));
        assertEquals("forfeit-duel", GameDebugEventCatalog.events(ArcadePortGame.GLADIATORS)
                .getFirst().id());
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.SPLEEF).stream()
                .anyMatch(event -> event.id().equals("refresh-kit")));
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.SPLEEF).stream()
                .anyMatch(event -> event.id().equals("restore-hunger")));
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.SPLEEF).stream()
                .anyMatch(event -> event.id().equals("use-kit-ability")));
        assertEquals(List.of("charge-barrage", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.TURF_WARS).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertEquals("use-special", GameDebugEventCatalog.events(ArcadePortGame.SUPER_SPLOOR)
                .getFirst().id());
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.MICRO_BATTLES).stream()
                .anyMatch(event -> event.id().equals("drop-barriers")));
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.MICRO_BATTLES).stream()
                .anyMatch(event -> event.id().equals("test-dasher-jump")));
        assertEquals("grant-spells", GameDebugEventCatalog.events(ArcadePortGame.WIZARDS)
                .getFirst().id());
        assertEquals(List.of("fill-meter", "use-ability", "use-crouch-dash", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.BACON_BRAWL).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
    }

    @Test
    void milkTheCowAbilityEventUsesTheRoleAbilityImplementationsAndDebugFeatureGates() throws IOException {
        assertEquals(List.of("spawn-herd", "use-kit-ability"),
                GameDebugEventCatalog.events(ArcadePortGame.MILK_THE_COW).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String runtime = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        assertTrue(manager.contains("launchMilkCowBomb(session, tester, now)"));
        assertTrue(manager.contains("startMilkCowBodySlam(session, tester, now)"));
        assertTrue(manager.contains("\"cow-bomb\", true)"));
        assertTrue(manager.contains("\"body-slam\", true)"));
        assertTrue(manager.contains("MilkCowRules.cowBombDamage()"));
        assertTrue(manager.contains("MilkCowRules.bodySlamDamageAtDistance(distance)"));
        assertTrue(runtime.contains("features.put(\"cow-bomb\""));
        assertTrue(runtime.contains("features.put(\"body-slam\""));
    }

    @Test
    void everyMonsterMazeModeUsesTheSharedMonsterWaveEventBridge() {
        for (ArcadePortGame game : List.of(ArcadePortGame.MONSTER_MAZE,
                ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.MONSTER_MAZE_MODERN)) {
            assertEquals(List.of("spawn-monster-wave"), GameDebugEventCatalog.events(game).stream()
                    .map(GameDebugEventCatalog.Event::id).toList());
        }
    }

    @Test
    void quiverGroundPoundDebugEventUsesTheLiveAbilityAndPreservesItsGuards() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"use-kit-ability\", \"Test Selected Kit Ability\""));
        assertTrue(manager.contains("\"slam-shot\".equals(session.quiverKits"));
        assertTrue(manager.contains("useQuiverGroundPound(session, tester, now)"),
                "the event must execute the same cooldown and knockback path as the live ability");
        assertTrue(manager.contains("useQuiverLeaperDoubleJump(session, tester)"),
                "the selected-kit event must also use the normal Leaper jump path");
        assertTrue(manager.contains("isDebugAbilityAllowed(tester, ArcadePortGame.ONE_IN_THE_QUIVER, kit)"));
        assertTrue(manager.contains("GameDebugRuntimeOverrides.markDebugSession()"));
    }

    @Test
    void quiverKillEventOnlyTargetsBotsAndUsesNormalScoreAndRespawnPath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"test-bot-kill\", \"Test Kill and Respawn\""));
        assertTrue(manager.contains("testPlayers.isTestPlayer(candidate)"),
                "the event must never target a human player");
        assertTrue(manager.contains("scoreQuiverKill(session, tester, target)"),
                "the event must use ordinary scoring, killstreak, and respawn behavior");
        assertTrue(manager.contains("if (target != null)"));
        assertTrue(manager.contains("scoreQuiverKill(session, tester, target)"));
    }

    @Test
    void paintballFireShotEventUsesTheSelectedWeaponsNormalRuntimePath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"fire-shot\", \"Fire Selected Weapon\""));
        assertTrue(manager.contains("session.paintballKits.getOrDefault(testerId, \"rifle\")"));
        assertTrue(manager.contains("firePaintballWeapon(session, tester, kit, System.currentTimeMillis())"),
                "the event must share reload, heat, projectile, and cosmetic behavior with regular firing");
        assertTrue(manager.contains("session.paintballShotOwners.size() > projectilesBefore"));
        assertTrue(manager.contains("if (triggered) GameDebugRuntimeOverrides.markDebugSession()"));
    }

    @Test
    void paintballWaterBombEventUsesNormalProjectilePathAndFeatureGates() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String runtime = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        assertTrue(manager.contains("if (type == ArcadePortGame.PAINTBALL && \"throw-water-bomb\".equals(eventId))"));
        assertTrue(manager.contains("private boolean throwPaintballWaterBomb(Session session, Player player"));
        assertTrue(manager.contains("player.launchProjectile(ThrownPotion.class)"));
        assertTrue(manager.contains("session.paintballWaterBombs.add(potion.getUniqueId())"));
        assertTrue(manager.contains("\"water-bombs\", true)"));
        assertTrue(manager.contains("\"water-bomb-revives\", true)"));
        assertTrue(manager.contains("\"water-bomb-cleanse\", true)"));
        assertTrue(manager.contains("\"save-me-marker\", true)"));
        assertTrue(runtime.contains("features.put(\"water-bombs\""));
        assertTrue(runtime.contains("features.put(\"water-bomb-revives\""));
        assertTrue(runtime.contains("features.put(\"water-bomb-cleanse\""));
    }

    @Test
    void deathTagTricksterDebugEventUsesNormalSwapAndFireworksCooldownPaths() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"use-kit-ability\", \"Test Trickster Ability\""));
        assertTrue(manager.contains("\"trickster\".equals(session.deathTagRunnerKits.get(tester.getUniqueId()))"));
        assertTrue(manager.contains("useDeathTagTricksterSwap(session, tester)"));
        assertTrue(manager.contains("useDeathTagTricksterFireworks(session, tester)"));
        assertTrue(manager.contains("session.deathTagTricksterFireworksReadyAt.getOrDefault("));
        assertTrue(manager.contains("if (triggered) GameDebugRuntimeOverrides.markDebugSession()"));
    }

    @Test
    void dragonsDebugEventRunsSelectedKitThroughItsNormalAbilityAndCooldownMethods() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"use-kit-ability\", \"Test Selected Kit Ability\""));
        assertTrue(manager.contains("debugUseDragonsKitAbility(session, tester)"));
        assertTrue(manager.contains("case \"coward\" -> useDragonsLeap(session, player, now)"));
        assertTrue(manager.contains("case \"frost\" -> activateDragonFrostBridge(session, player, now)"));
        assertTrue(manager.contains("case \"glider\" -> activateDragonGlider(session, player, now)"));
        assertTrue(manager.contains("case \"tamer\" -> activateDragonTamer(session, player, now)"));
        assertTrue(manager.contains("session.dragonGliderReadyAt.getOrDefault(id, 0L) > now"));
        assertTrue(manager.contains("!isDebugAbilityAllowed(player, ArcadePortGame.DRAGONS, kit)"));
    }

    @Test
    void spleefBrawlerDebugEventUsesTheNormalLeapCooldownAndVelocityPath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"use-kit-ability\", \"Test Brawler Leap\""));
        assertTrue(manager.contains("useSpleefLeap(session, tester, now)"));
        assertTrue(manager.contains("session.spleefLeapReadyAt.getOrDefault(tester.getUniqueId(), 0L) > now"));
        assertTrue(manager.contains("isDebugAbilityAllowed(tester, ArcadePortGame.SPLEEF, \"brawler\")"));
    }

    @Test
    void turfWarsDebugEventStartsTheNormalShredderBarrageCharge() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"charge-barrage\", \"Charge Shredder Barrage\""));
        assertTrue(manager.contains("startTurfWarsBarrage(session, tester)"));
        assertTrue(manager.contains("session.turfBarrageCharges.containsKey(tester.getUniqueId())"));
        assertTrue(manager.contains("startTurfWarsBarrage(Session session, Player player)"),
                "the event must retain Shredder's ordinary bow, ammo, and charge checks");
    }

    @Test
    void hoopsStealDebugEventUsesNormalPossessionAndStealValidation() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String runtime = Files.readString(sourceRoot.resolve("HoopsRemasteredRuntime.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"try-steal\", \"Try to Steal Ball\""));
        assertTrue(manager.contains("hoops.debugAttemptSteal(tester)"));
        assertTrue(runtime.contains("trySteal(current, attacker, holder)"));
        assertTrue(runtime.contains("public boolean debugAttemptSteal(Player attacker)"));
    }

    @Test
    void baconBrawlDebugAbilityEventUsesSelectedKitMeterAndLiveAbilityPath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"use-ability\", \"Use Selected Ability\""));
        assertTrue(manager.contains("session.baconKits.getOrDefault(testerId, \"el-muchacho-pigo\")"));
        assertTrue(manager.contains("ArcadePortRules.canUseBaconMeterAbility(charge)"));
        assertTrue(manager.contains("useBaconBrawlAbility(session, tester, System.currentTimeMillis())"),
                "debug use must preserve each kit's standard ability and meter consumption");
    }

    @Test
    void bombLobbersDebugThrowUsesNormalTntPathAndIsolatesMatchProgression() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"throw-held-bomb\", \"Throw Held TNT\""));
        assertTrue(manager.contains("isBombLobbersThrownTnt(tester.getInventory().getItemInMainHand())"));
        assertTrue(manager.contains("throwBombLobbersTnt(session, tester)"));
        assertTrue(manager.contains("GameDebugRuntimeOverrides.markDebugSession();"));
    }

    @Test
    void baconCrispyDashDebugActionRunsItsDistinctCrouchAbilityPath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"use-crouch-dash\", \"Test Crispy Bacon Dash\""));
        assertTrue(manager.contains("useBaconBrawlAbility(session, tester, System.currentTimeMillis(), true)"));
        assertTrue(manager.contains("\"chris-p-bacon\".equals(session.baconKits.getOrDefault("));
        assertTrue(manager.contains("ArcadePortRules.canUseBaconMeterAbility(charge)"));
    }

    @Test
    void witherAssaultDebugSkullUsesNormalTeamAndCooldownChecks() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String eventCatalog = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(eventCatalog.contains("new Event(\"fire-skull\", \"Fire Wither Skull\""));
        assertTrue(manager.contains("launchWitherAssaultSkull(session, tester, now)"));
        assertTrue(manager.contains("session.teams.getOrDefault(tester.getUniqueId(), 0) == 1"));
        assertTrue(manager.contains("session.witherLastAttackAt.getOrDefault(tester.getUniqueId(), 0L) > now"));
    }

    @Test
    void monsterMazeKitDebugSwitchesGateLiveKitAbilitiesAndApplyImmediately() throws IOException {
        Path arcadeRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        Path mazeRoot = Path.of("..", "monster-maze-port", "src", "main", "java", "me", "monstermaze");
        if (!Files.isDirectory(arcadeRoot)) {
            arcadeRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
            mazeRoot = Path.of("modern-platform", "monster-maze-port", "src", "main", "java", "me",
                    "monstermaze");
        }
        String lobby = Files.readString(arcadeRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(arcadeRoot.resolve("ArcadePortManager.java"));
        String rules = Files.readString(mazeRoot.resolve("game").resolve("MazeDebugRules.java"));
        String kits = Files.readString(mazeRoot.resolve("kit").resolve("KitManager.java"));
        String monsters = Files.readString(mazeRoot.resolve("entity").resolve("MonsterManager.java"));
        for (String key : List.of("jumper", "slowball", "body-builder", "repulsor")) {
            assertTrue(lobby.contains("\"" + key + "\""), key + " is exposed in the mode debug menu");
        }
        assertTrue(lobby.contains("if (game.isMonsterMaze() && key.startsWith(\"kit-\")) return true"));
        assertTrue(manager.contains("lobbyRuntime.debugFeatureAppliesImmediately(type, flag)"));
        assertTrue(manager.contains("debugProfiles.featureKeys(tester.getUniqueId(), type)"));
        assertTrue(manager.contains("GameDebugRuntimeOverrides.activate(values, activeDebugProfileOwner)"));
        assertTrue(manager.contains("new HashSet<>(activeDebugFeatureKeys)"),
                "features deleted with an old profile remain known long enough to clear from the live round");
        assertTrue(manager.contains("GameDebugRuntimeOverrides.removeValue(key)"));
        assertTrue(rules.contains("booleanValueFor(player, \"kit-\" + kit, true)"));
        assertTrue(kits.contains("kitEnabled(p.getUniqueId(), \"jumper\")"));
        assertTrue(kits.contains("kitEnabled(p.getUniqueId(), \"slowball\")"));
        assertTrue(kits.contains("kitEnabled(player.getUniqueId(), \"body-builder\")"));
        assertTrue(kits.contains("kitEnabled(player.getUniqueId(), \"repulsor\")"));
        assertTrue(monsters.contains("kitEnabled(player.getUniqueId(), \"maverick\")"));
    }

    @Test
    void profileChangesApplyOnlyImmediateFeaturesAndClearStaleFeatureValuesAcrossGames() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String profiles = Files.readString(sourceRoot.resolve("GameDebugProfiles.java"));
        String overrides = Files.readString(sourceRoot.resolve("GameDebugRuntimeOverrides.java"));
        assertTrue(manager.contains("lobbyRuntime.debugFeatureAppliesImmediately(type, flag)"));
        assertTrue(manager.contains("debugProfiles.featureKeys(tester.getUniqueId(), type)"));
        assertTrue(manager.contains("debugProfiles.selectedFeatureValues("));
        assertTrue(manager.contains("GameDebugRuntimeOverrides.removeValue(key)"));
        assertTrue(lobby.contains("boolean debugFeatureAppliesImmediately(ArcadePortGame game, String key)"));
        assertTrue(lobby.contains("boolean appliesImmediately)"));
        assertTrue(lobby.contains("feature.appliesImmediately()"));
        assertTrue(lobby.contains("debugFeatureAppliesImmediately(game, feature.key())"));
        assertTrue(profiles.contains("Map<String, Boolean> selectedFeatureValues("));
        assertTrue(profiles.contains("Set<String> featureKeys("));
        assertTrue(overrides.contains("public static void removeValue(String key)"));
    }

    @Test
    void speedBuildersOwnBuildResetHasAnAuthorizedRuntimeDispatcher() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String runtime = Files.readString(sourceRoot.resolve("SpeedBuildersGame.java"));
        assertTrue(manager.contains("type == ArcadePortGame.SPEED_BUILDERS && \"reset-own-build\".equals(eventId)"));
        assertTrue(runtime.contains("plugin.hasAdminAccess(tester)"));
        assertTrue(runtime.contains("restorePlot(state.plot)"));
        assertTrue(runtime.contains("pasteTemplate(state.plot, currentTemplate)"));
        assertTrue(runtime.contains("GameDebugRuntimeOverrides.markDebugSession()"));
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
                boolean sharedMazeDispatcher = game.isMonsterMaze() && "spawn-monster-wave".equals(event.id())
                        && source.contains("type.isMonsterMaze() && \"spawn-monster-wave\".equals(eventId)");
                assertTrue(directDispatcher || sharedKitDispatcher || sharedMazeDispatcher,
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
        String cowController = Files.readString(managerPath.resolveSibling("MilkCowManager.java"));
        int dispatcher = source.indexOf("type == ArcadePortGame.MILK_THE_COW && \"spawn-herd\".equals(eventId)");
        int update = source.indexOf("private void updateMilkCowHerd(");
        int nextMethod = source.indexOf("private void ", update + 1);
        assertTrue(dispatcher >= 0);
        assertTrue(update > dispatcher);
        assertTrue(nextMethod > update);
        assertTrue(source.substring(update, nextMethod).contains("milkCowController.updateHerd("));
        assertTrue(cowController.contains("session.teams.getOrDefault(cowPlayer.getUniqueId(), 0) != 1"));
        assertTrue(cowController.contains("MILK_THE_COW_HERD_SIZE"));
        assertTrue(cowController.contains("MILK_THE_COW_HERD_MOVE_SPEED"));
        assertTrue(cowController.contains("session.world.spawn(spawn, Cow.class)"));
    }

    @Test
    void sneakyAssassinsDebugAbilityRunsTheSelectedKitEffectForAuthorisedLivingPlayers() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String mechanics = Files.readString(sourceRoot.resolve("SneakyAssassinsMechanics.java"));
        assertTrue(manager.contains("type == ArcadePortGame.SNEAKY_ASSASSINS && \"use-kit-ability\".equals(eventId)"));
        assertTrue(mechanics.contains("!plugin.hasAdminAccess(tester)"));
        assertTrue(mechanics.contains("!isAlive.test(tester.getUniqueId())"));
        assertTrue(mechanics.contains("escapeArtistSmokeUsed.add(id)"));
        assertTrue(mechanics.contains("useSmokeBomb(tester)"));
        assertTrue(mechanics.contains("throwRevealer(tester)"));
        assertTrue(mechanics.contains("booleanValue(\"ability-smoke-bomb\", true)"));
        assertTrue(mechanics.contains("booleanValue(\"ability-revealer\", true)"));
    }

    @Test
    void plexballFeatureSwitchesGateLiveTesterActionsAndApplyImmediately() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String game = Files.readString(sourceRoot.resolve("PlexballGame.java"));
        for (String key : List.of("batting", "catching", "kit-abilities")) {
            assertTrue(lobby.contains("features.put(\"" + key + "\""), key + " is configurable");
            assertTrue(game.contains("debugFeatureEnabled(player, \"" + key + "\")"),
                    key + " gates the active gameplay action");
        }
        assertTrue(lobby.contains("List.of(\"slugger\", \"technician\", \"curve-artist\", \"sprinter\", \"skyguard\")"));
        assertTrue(lobby.contains("features.put(key, new DebugFeature(key"));
        assertTrue(game.contains("debugFeatureEnabled(player, \"ability-\" + kit.id())"),
                "each selected kit has its own independently gated ability");
        assertTrue(lobby.contains("key.startsWith(\"ability-\")"),
                "ability switches are applied immediately to the selected tester");
        assertTrue(game.contains("serverTick + PlexballRules.introTicks()"));
        assertTrue(game.contains("serverTick + PlexballRules.turnBreakTicks()"));
        assertTrue(game.contains("random.nextInt(maximumCycle - minimumCycle + 1)"));
        assertTrue(game.contains("PlexballRules.kitBattingPower(kit.id(), kit.battingPower)"));
        assertTrue(game.contains("PlexballRules.battingBaseHorizontalSpeed()"));
        assertTrue(game.contains("PlexballRules.technicianTimingBonusTicks()"));
        assertTrue(game.contains("!plugin.hasAdminAccess(player)) return true"),
                "non-authorised players retain normal Plexball rules");
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.PLEXBALL).stream()
                .anyMatch(event -> event.id().equals("use-kit-ability")));
        assertTrue(game.contains("public boolean debugUseSelectedKitAbility(Player tester)"));
        assertTrue(game.contains("phase != Phase.BALL_FLIGHT") && game.contains("useAbility(tester)"));
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
    void sheepQuestBruteDebugEventUsesTheNormalPassengerThrowConsumer() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        int dispatcher = source.indexOf("type == ArcadePortGame.SHEEP_QUEST && \"throw-passenger\".equals(eventId)");
        int thrower = source.indexOf("private void throwSheepQuestPassenger(Session session, Player thrower)");
        assertTrue(dispatcher >= 0);
        assertTrue(thrower > dispatcher);
        assertTrue(source.substring(dispatcher, thrower).contains("throwSheepQuestPassenger(session, tester)"));
        assertTrue(source.substring(thrower, source.indexOf("private void updateSheepQuestKits", thrower))
                .contains("sheepQuestBruteThrowHorizontalSpeed()"));
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
        assertTrue(generator.contains("SkyWarsMixedArcadeRules.tntGeneratorIntervalMillis()"));
    }

    @Test
    void skyWarsKitSwitchesAreListedAndConsumedByTheirLiveKitPaths() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        for (String kit : List.of("ice", "fire", "air", "metal", "earth")) {
            assertTrue(lobby.contains("\"" + kit + "\""), kit + " appears in the SkyWars debug menu");
            assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.SKY_WARS, kit)"),
                    kit + " passive behavior reads its profile switch");
            assertTrue(manager.contains("isDebugAbilityAllowed(player, ArcadePortGame.SKY_WARS, kit)"),
                    kit + " active ability reads its profile switch");
        }
        assertTrue(lobby.contains("game == ArcadePortGame.SKY_WARS && key.startsWith(\"ability-\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.SKY_WARS, \"air\")"));
        assertTrue(manager.contains("player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE)"));
    }

    @Test
    void snakeDebugSwitchesReachItsInProcessGameModule() throws IOException {
        Path arcadeSources = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(arcadeSources)) {
            arcadeSources = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        Path snakeManagerPath = Path.of("..", "snake-port", "src", "main", "java", "au", "com",
                "testplex", "snake", "SnakeGameManager.java");
        if (!Files.isRegularFile(snakeManagerPath)) {
            snakeManagerPath = Path.of("modern-platform", "snake-port", "src", "main", "java", "au",
                    "com", "testplex", "snake", "SnakeGameManager.java");
        }
        String lobby = Files.readString(arcadeSources.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(arcadeSources.resolve("ArcadePortManager.java"));
        String snake = Files.readString(snakeManagerPath);
        for (String control : List.of("speed", "reverse", "invulnerable",
                "tail-collisions", "self-collisions", "opponent-collisions", "magma-hazards", "lava-elimination")) {
            assertTrue(lobby.contains("\"" + control + "\""), control + " appears in Snake's debug menu");
        }
        assertTrue(lobby.contains("String key = \"ability-\" + kit;"),
                "the three kit toggles are generated from their selected kit IDs");
        assertTrue(snake.contains("booleanValue(abilityFeature, true)"));
        for (String control : List.of("tail-collisions", "magma-hazards", "lava-elimination")) {
            assertTrue(snake.contains("booleanValue(\"" + control + "\""),
                    control + " is consumed by Snake gameplay");
        }
        assertTrue(snake.contains("booleanValue(\"self-collisions\", true)"));
        assertTrue(snake.contains("booleanValue(\"opponent-collisions\", true)"));
        assertTrue(manager.contains("GameDebugRuntimeOverrides.setValue(flag, enabled)"));
        assertTrue(snake.contains("plugin.hasAdminAccess(event.getPlayer())"));
        assertTrue(snake.contains("plugin.hasAdminAccess(playerState.player)"));
    }

    @Test
    void sneakyAssassinsDebugSwitchesReachItsLiveMechanics() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String mechanics = Files.readString(sourceRoot.resolve("SneakyAssassinsMechanics.java"));
        for (String key : List.of("disguises", "power-ups", "global-reveal", "npc-patrol", "town-npcs",
                "master-compass")) {
            assertTrue(lobby.contains("\"" + key + "\""), key + " appears in Sneaky Assassins controls");
        }
        assertTrue(lobby.contains("\"ability-\" + feature"));
        for (String kit : List.of("smoke-bomb", "revealer", "briber")) {
            assertTrue(lobby.contains("\"" + kit + "\""));
        }
        for (String key : List.of("disguises", "power-ups", "global-reveal", "npc-patrol", "town-npcs",
                "ability-smoke-bomb", "ability-revealer", "ability-briber", "master-compass")) {
            assertTrue(mechanics.contains("booleanValue(\"" + key + "\""),
                    key + " is read by the live mechanics");
        }
        assertTrue(manager.contains("type == ArcadePortGame.SNEAKY_ASSASSINS"));
        assertTrue(mechanics.contains("removePowerUp(powerUp)"));
        assertTrue(mechanics.contains("removeDisguise(id)"));
    }

    @Test
    void drawMyThingDebugSwitchesControlGuessHintsAndRoundSounds() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String drawController = Files.readString(sourceRoot.resolve("DrawMyThingManager.java"));
        for (String key : List.of("guess-submission", "timed-hints", "sound-feedback")) {
            assertTrue(lobby.contains("\"" + key + "\""), key + " appears in the Draw My Thing panel");
        }
        assertTrue(manager.contains("debugFeatureEnabled(playerId, ArcadePortGame.DRAW_MY_THING, \"guess-submission\", true)"));
        for (String key : List.of("timed-hints", "sound-feedback")) {
            assertTrue(drawController.contains("booleanValue(\"" + key + "\""),
                    key + " is consumed by the active round runtime");
        }
        assertTrue(manager.contains("session.drawRevealedLetters.clear()"));
        assertTrue(manager.contains("type == ArcadePortGame.DRAW_MY_THING"));
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.DRAW_MY_THING).stream()
                .anyMatch(event -> event.id().equals("clear-canvas")));
        assertTrue(manager.contains("\"clear-canvas\".equals(eventId)"));
        assertTrue(manager.contains("tester.getUniqueId().equals(session.drawCurrentDrawer)"));
        assertTrue(manager.contains("resetDrawCanvas(session)"));
        assertTrue(manager.contains("session.drawPreviousPaintPoints.clear()"));
    }

    @Test
    void masterBuildersDebugSwitchesGatePlotEditingAndReviewVotes() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        for (String key : List.of("plot-editing", "review-voting")) {
            assertTrue(lobby.contains("\"" + key + "\""), key + " appears in Master Builders debug menu");
            assertTrue(manager.contains("ArcadePortGame.MASTER_BUILDERS, \"" + key + "\", true"),
                    key + " reaches its gameplay check");
        }
        assertTrue(manager.contains("plugin.hasAdminAccess(builder)"));
        assertTrue(manager.contains("plugin.hasAdminAccess(player)"));
    }

    @Test
    void milkTheCowDebugSwitchesGateRoleAbilitiesAndScoring() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String cowController = Files.readString(sourceRoot.resolve("MilkCowManager.java"));
        for (String key : List.of("cow-charge", "farmer-double-jump", "herd-spawning", "milk-capture", "milk-scoring")) {
            assertTrue(lobby.contains("\"" + key + "\""), key + " appears in Milk the Cow debug menu");
            assertTrue(manager.contains("ArcadePortGame.MILK_THE_COW, \"" + key + "\", true"),
                    key + " reaches its live gameplay check");
        }
        assertTrue(manager.contains("debugFeatureEnabledForActiveSession(ArcadePortGame.MILK_THE_COW, \"herd-spawning\")"));
        assertTrue(cowController.contains("session.milkCowHerd.clear()"));
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
        assertTrue(runtime.substring(persist, award).contains("if (plugin.isTestPlayer(id)) continue;"));
        int awardGuard = runtime.indexOf("if (GameDebugRuntimeOverrides.isDebugSession() || plugin.isTestPlayer(id)) return;", award);
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
        int end = source.indexOf("private void awardAchievement(", start);
        assertTrue(start >= 0 && end > start);
        String method = source.substring(start, end);
        int guard = method.indexOf("!GameDebugRuntimeOverrides.isDebugSession()");
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
            String signature = switch (methodName) {
                case "awardAchievement" -> "public boolean awardAchievement(";
                default -> "public void " + methodName + "(";
            };
            int start = store.indexOf(signature);
            int end = store.indexOf("\n    }", start);
            assertTrue(start >= 0 && end > start, methodName + " exists");
            assertTrue(store.substring(start, end).contains("shouldPersist("),
                    methodName + " uses the shared progression guard");
        }
        assertTrue(store.contains("private boolean shouldPersist(UUID playerId)"));
        assertTrue(store.contains("GameDebugRuntimeOverrides.isDebugSession()"));
        assertTrue(store.contains("getMethod(\"isTestPlayer\", UUID.class)"));
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
        assertTrue(normalize(manager).contains(normalize("GameDebugRuntimeOverrides.markDebugSession();\n"
                + "            return cameraShyModule.debugTakePhoto(tester);")));
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
        assertTrue(normalize(game).contains(normalize("private void award(UUID id, String achievementId) {\n"
                + "        if (GameDebugRuntimeOverrides.isDebugSession()) return;")));
        assertTrue(normalize(game).contains(normalize("private void recordMatchProgress(UUID winnerId) {\n"
                + "        if (GameDebugRuntimeOverrides.isDebugSession()) return;")));
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
            assertTrue(manager.substring(body, Math.min(manager.length(), body + 500)).contains("isDebugAbilityAllowed"),
                    methodName + " consumes its immediate ability switch");
        }
        assertTrue(manager.contains("isDebugAbilityAllowed(player, ArcadePortGame.DRAGONS, \"pyrotechnic\")"));
        assertTrue(manager.contains("isDebugAbilityAllowed(marksman, ArcadePortGame.DRAGONS, \"marksman\")"));
        assertTrue(lobby.contains("game == ArcadePortGame.DRAGONS && key.startsWith(\"ability-\")"));
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
        assertTrue(lobby.contains("List.of(\"leaper\", \"brawler\", \"enchanter\", \"slam-shot\", \"ninja\")"));
        assertTrue(lobby.contains("game == ArcadePortGame.ONE_IN_THE_QUIVER && key.startsWith(\"ability-\")"));
        for (String hook : List.of("ArcadePortGame.ONE_IN_THE_QUIVER, \"leaper\"",
                "ArcadePortGame.ONE_IN_THE_QUIVER, \"slam-shot\"",
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
        int next = manager.indexOf("\n    private ", fire + 1);
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
        assertTrue(manager.contains("boolean debugTriggerEvent(Player tester, ArcadePortGame type, String eventId)"));
        assertTrue(manager.contains("triggered = useRunnerPositionSwap(session, tester, System.currentTimeMillis())"));
        assertEquals(List.of("use-kit-ability", "grant-frostballs", "use-position-swap", "refresh-kit"),
                GameDebugEventCatalog.events(ArcadePortGame.RUNNER).stream()
                        .map(GameDebugEventCatalog.Event::id).toList());
        assertTrue(manager.contains("case \"frosty\" -> triggered = grantRunnerFrostballs(session, tester)"));
        assertTrue(manager.contains("private boolean grantRunnerFrostballs(Session session, Player player)"));
        assertTrue(manager.contains("RUNNER_MAX_FROSTBALLS"));
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
            assertTrue(manager.contains("ArcadePortGame.DEATH_TAG, \"ability-" + kit + "\"")
                            || manager.contains("ArcadePortGame.DEATH_TAG, \"" + kit + "\"")
                            || manager.contains("ArcadePortGame.DEATH_TAG, \"ability-" + kit + "\", true"),
                    kit + " has a runtime consumer");
        }
        assertTrue(normalize(manager).contains(normalize("private void useDeathTagTricksterSwap(Session session, Player runner) {\n"
                + "        if (!isDebugAbilityAllowed(runner, ArcadePortGame.DEATH_TAG, \"trickster\")) return;")));
        assertTrue(normalize(manager).contains(normalize("private void useDeathTagTricksterFireworks(Session session, Player runner) {\n"
                + "        if (!isDebugAbilityAllowed(runner, ArcadePortGame.DEATH_TAG, \"trickster\")) return;")));
        assertTrue(normalize(manager).contains(normalize("plugin.hasAdminAccess(player) && !debugFeatureEnabled(player.getUniqueId(),\n"
                + "                ArcadePortGame.DEATH_TAG, \"ability-archer\", true)")));
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
        assertTrue(normalize(manager).contains(normalize("private void startEvolutionGolemSlam(Session session, Player player, long now) {\n"
                + "        if (!isDebugAbilityAllowed(player, ArcadePortGame.EVOLUTION, \"golem\")) return;")));
        assertTrue(normalize(manager).contains(normalize("private boolean throwEvolutionSulphurBomb(Session session, Player player) {\n"
                + "        if (!isDebugAbilityAllowed(player, ArcadePortGame.EVOLUTION, \"creeper\")) return false;")));
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
        assertTrue(lobby.contains("List.of(\"beserker\", \"archer\", \"brute\", \"shepherd\")"));
        for (String kit : List.of("beserker", "archer", "shepherd")) {
            assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.SHEEP_QUEST, \"" + kit + "\")"),
                    kit + " has a runtime consumer");
        }
        assertTrue(GameDebugEventCatalog.events(ArcadePortGame.SHEEP_QUEST).stream()
                .anyMatch(event -> event.id().equals("throw-passenger")));
        assertTrue(manager.contains("debugKitFeatureEnabled(thrower, ArcadePortGame.SHEEP_QUEST, \"brute\")"));
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
        assertTrue(runtime.contains("specialCharge.put(id, GameDebugRuntimeOverrides.doubleValue("));
        assertTrue(runtime.contains("\"SUPER_SPLOOR_SPECIAL_CHARGE_REQUIRED\", SuperSploorRules.SPECIAL_CHARGE_REQUIRED)"));
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
        String turf = Files.readString(sourceRoot.resolve("TurfWarsManager.java"));
        assertTrue(lobby.contains("features.put(\"build-editing\""));
        assertTrue(lobby.contains("features.put(\"combat-damage\""));
        assertTrue(manager.contains("\"build-editing\", true"));
        assertTrue(manager.contains("\"combat-damage\", true"));
        assertTrue(lobby.contains("List.of(\"infiltrator\", \"shredder\", \"fortifier\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.TURF_WARS, \"infiltrator\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.TURF_WARS, \"shredder\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.TURF_WARS, \"fortifier\")"));
        assertTrue(manager.contains("turfWarsController.startBarrage(session, player,"));
        assertTrue(manager.contains("isDebugAbilityAllowed(player, ArcadePortGame.TURF_WARS, \"shredder\")"));
        assertTrue(turf.contains("if (!abilityAllowed) return;"));
        assertTrue(turf.contains("session.turfBarrageFiring.remove(id)"));
    }

    @Test
    void wizardsDebugSwitchesGateLiveSpellCastingAndChestLooting() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String api = Files.readString(sourceRoot.resolve("ArcadeMechanicsModernPlugin.java"));
        Path wizardManager = Path.of("src", "main", "java", "au", "com", "testplex",
                "wizards", "WizardsGameManager.java");
        if (!Files.isRegularFile(wizardManager)) {
            wizardManager = Path.of("modern-platform", "arcade-games-port", "src", "main", "java",
                    "au", "com", "testplex", "wizards", "WizardsGameManager.java");
        }
        String wizards = Files.readString(wizardManager);
        assertTrue(lobby.contains("features.put(\"spell-casting\""));
        assertTrue(lobby.contains("features.put(\"chest-looting\""));
        assertTrue(api.contains("isArcadeDebugFeatureEnabled(Player player, ArcadePortGame game"));
        assertTrue(wizards.contains("ArcadePortGame.WIZARDS, \"spell-casting\", true"));
        assertTrue(wizards.contains("ArcadePortGame.WIZARDS, \"chest-looting\", true"));
    }

    @Test
    void skyWarsDebugSwitchesGateChestLootIslandCrumblingAndTntGenerator() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("features.put(\"chest-looting\""));
        assertTrue(lobby.contains("features.put(\"island-crumbling\""));
        assertTrue(lobby.contains("features.put(\"tnt-generator\""));
        assertTrue(lobby.contains("features.put(\"guardian-spawns\""));
        assertTrue(lobby.contains("features.put(\"ore-hiding\""));
        assertTrue(normalize(lobby).contains(normalize("new DebugFeature(\"guardian-spawns\", \"Zombie guardians\", Material.ZOMBIE_HEAD,\n                    \"Spawn and run the map-authored Zombie Guardians for the next round.\", false)")));
        assertTrue(normalize(lobby).contains(normalize("new DebugFeature(\"ore-hiding\", \"Hidden island ores\", Material.DIAMOND_ORE,\n                    \"Use the source OreHider behavior for the next round's island ores.\", false)")));
        assertTrue(normalize(manager).contains("ArcadePortGame.SKY_WARS, \"chest-looting\", true"));
        assertTrue(normalize(manager).contains(normalize("debugFeatureEnabledForActiveSession(\n                ArcadePortGame.SKY_WARS, \"island-crumbling\")")));
        assertTrue(manager.contains("debugFeatureEnabledForActiveSession(ArcadePortGame.SKY_WARS, \"tnt-generator\")"));
        assertTrue(manager.contains("debugFeatureEnabledForActiveSession(ArcadePortGame.SKY_WARS, \"guardian-spawns\")"));
        assertTrue(normalize(manager).contains(normalize("debugFeatureEnabledForActiveSession(ArcadePortGame.SKY_WARS, \"ore-hiding\")")));
        int airAbility = manager.indexOf("public void onSkyWarsAirDoubleJump(PlayerToggleFlightEvent event)");
        int airDamage = manager.indexOf("public void onSkyWarsAirFallDamage", airAbility);
        assertTrue(airAbility >= 0 && airDamage > airAbility);
        assertFalse(manager.substring(airAbility, airDamage).contains("GameDebugRuntimeOverrides.isDebugSession()"),
                "The Air kit double jump must remain usable in a debug round.");
    }

    @Test
    void heroesOfGwenTowerSwitchGatesTesterDamageAtTheTowerRuntime() throws IOException {
        Path arcadeSource = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(arcadeSource)) {
            arcadeSource = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(arcadeSource.resolve("ModernArcadeLobbyRuntime.java"));
        String runtime = Files.readString(arcadeSource.resolve("CastleAssaultHeroesOfGwenRuntime.java"));
        assertTrue(lobby.contains("features.put(\"tower-damage\""));
        assertTrue(lobby.contains("ArcadePortGame.HEROES_OF_GWEN && \"tower-damage\".equals(key)"));
        assertTrue(runtime.contains("hooks.debugFeatureEnabled(attacker, \"tower-damage\")"));
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
        assertTrue(lobby.contains("features.put(\"worker-tools\""));
        assertTrue(lobby.contains("features.put(\"border-warning\""));
        assertTrue(lobby.contains("features.put(\"border-particles\""));
        assertTrue(lobby.contains("features.put(\"border-elimination\""));
        assertTrue(lobby.contains("features.put(\"blocks-place\""));
        assertTrue(lobby.contains("features.put(\"blocks-break\""));
        assertTrue(manager.contains("debugKitFeatureEnabled(targetPlayer, ArcadePortGame.MICRO_BATTLES, \"fighter\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.MICRO_BATTLES, \"archer\")"));
        assertTrue(manager.contains("debugKitFeatureEnabled(player, ArcadePortGame.MICRO_BATTLES, \"dasher\")"));
        assertTrue(manager.contains("isMicroWorkerMiningTool(heldMaterial)"));
        assertTrue(normalize(manager).contains("ArcadePortGame.MICRO_BATTLES, \"worker-tools\", true"));
        assertTrue(manager.contains("material == Material.STONE_PICKAXE || material == Material.STONE_SHOVEL"));
        assertTrue(manager.contains("|| material == Material.STONE_AXE"));
        assertTrue(manager.contains("\"border-elimination\", true"));
        assertTrue(manager.contains("\"border-warning\", true"));
        assertTrue(manager.contains("\"border-particles\", true"));
        assertTrue(normalize(manager).contains("ArcadePortGame.MICRO_BATTLES, \"blocks-place\", true"));
        assertTrue(normalize(manager).contains("ArcadePortGame.MICRO_BATTLES, \"blocks-break\", true"));
        assertTrue(normalize(lobby).contains("Set.of(\"worker-tools\", \"border-warning\", \"border-particles\", \"border-elimination\").contains(key)"));
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
        assertTrue(normalize(manager).contains(normalize("GameDebugRuntimeOverrides.markDebugSession();\n"
                + "            tester.setHealth(0.0D)")));
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
        int nextMethod = source.indexOf("\n    private ", swap + 1);
        assertTrue(swap >= 0 && nextMethod > swap);
        String ability = source.substring(swap, nextMethod);
        assertTrue(ability.contains("canRunnerSwapFrom(session, target)"));
        assertTrue(ability.contains("session.runnerSwapUsed.add(id)"));
        assertTrue(ability.contains("protectRunnerSwapLanding(session, player, graceUntil)"));
        assertTrue(ability.contains("protectRunnerSwapLanding(session, target, graceUntil)"));
    }

    private static String normalize(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }

    @Test
    void loadingAProfileDuringItsLiveRoundRefreshesOnlyImmediateRuleValues() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String manager = Files.readString(managerPath);
        int select = manager.indexOf("boolean selectDebugProfile(Player tester, ArcadePortGame type, String profile)");
        int nextMethod = manager.indexOf("\n    private void activateDebugProfile(", select);
        assertTrue(select >= 0 && nextMethod > select);
        String method = normalize(manager.substring(select, nextMethod));
        assertTrue(method.contains("activeDebugProfileGame == type && type == activeDebugGame()"));
        assertTrue(method.contains("GameDebugVariableCatalog.appliesImmediately(variable)"));
        assertTrue(method.contains("GameDebugRuntimeOverrides.setValue(variable.key(), debugProfiles.booleanVariable("));
        assertTrue(method.contains("GameDebugRuntimeOverrides.markDebugSession()"));
        assertFalse(method.contains("GameDebugRuntimeOverrides.activate("),
                "loading a profile mid-round must not apply restart-required numeric values");
        assertTrue(normalize(manager).contains("if (created != null) applySelectedDebugProfileToLiveRound(tester, type);"));
        assertTrue(normalize(manager).contains("if (duplicated != null) applySelectedDebugProfileToLiveRound(tester, type);"));
        assertTrue(normalize(manager).contains("if (renamed != null) applySelectedDebugProfileToLiveRound(tester, type);"));
        assertTrue(normalize(manager).contains("applySelectedDebugProfileToLiveRound(tester, type);"));
        String lobby = Files.readString(managerPath.resolveSibling("ModernArcadeLobbyRuntime.java"));
        assertTrue(lobby.contains("manager.createDebugProfile(player, prompt.game(), input)"));
        assertTrue(lobby.contains("manager.duplicateDebugProfile(player, prompt.game(), input)"));
        assertTrue(lobby.contains("manager.renameDebugProfile(player, prompt.game(), input)"));
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
        assertTrue(source.contains("bombLobbersController.updatePowerups(session, now,"));
        String bombManager = Files.readString(managerPath.resolveSibling("BombLobbersManager.java"));
        int updater = bombManager.indexOf("void updatePowerups(SessionState session, long now,");
        int featureGate = bombManager.indexOf("if (!ArcadePortRules.bombLobbersPowerupsEnabled())", updater);
        int wave = bombManager.indexOf("String[] choices = {\"wind-charge\"", featureGate);
        assertTrue(updater >= 0 && featureGate > updater && wave > featureGate);
        assertTrue(bombManager.substring(updater, wave).contains("session.bombPowerupDrops.clear()"));
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
        assertTrue(runtime.contains("\"constructor-build\", \"Constructor defenses\""));
        assertTrue(manager.contains("\"constructor-build\", true)"));
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
        Path managerPath = runtimePath.resolveSibling("ArcadePortManager.java");
        String manager = Files.readString(managerPath);
        assertTrue(lobby.contains("\"objective-tnt\", \"Objective TNT\""));
        assertTrue(lobby.contains("\"crystal-damage\", \"Sentry crystal damage\""));
        assertTrue(lobby.contains("\"king-damage\", \"King damage\""));
        assertTrue(runtime.contains("!debugFeatureEnabled(tester, \"objective-tnt\")"));
        assertTrue(runtime.contains("!debugFeatureEnabled(attacker, \"crystal-damage\")"));
        assertTrue(runtime.contains("!debugFeatureEnabled(attacker, \"king-damage\")"));
        assertTrue(runtime.contains("!debugFeatureEnabled(player, \"objective-tnt\")"));
        for (String event : List.of("spawn-objective", "destroy-crystal", "damage-king")) {
            assertTrue(GameDebugEventCatalog.events(ArcadePortGame.CASTLE_ASSAULT).stream()
                    .anyMatch(entry -> entry.id().equals(event)), event + " is available in Castle Assault tools");
            assertTrue(manager.contains("\"" + event + "\".equals(eventId)"),
                    event + " dispatches to the live objective runtime");
        }
        assertTrue(runtime.contains("boolean debugDestroyEnemyCrystal(Player tester)"));
        assertTrue(runtime.contains("destroyCastleCrystal(enemyKing, crystal, tester)"));
        assertTrue(runtime.contains("boolean debugDamageEnemyKing(Player tester)"));
        assertTrue(runtime.contains("damageCastleKing(enemyKing, Math.min(50, before), tester)"));
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
        String manager = Files.readString(runtimePath.resolveSibling("ArcadePortManager.java"));
        assertTrue(lobby.contains("List.of(\"hattori\", \"bardolf\", \"devon\", \"rowena\", \"anath\", \"larissa\", \"dana\", \"biff\", \"ivy\")"));
        assertTrue(runtime.contains("\"hero-\" + skill.substring(0, separator) + \"-abilities\""));
        assertTrue(runtime.contains("This hero's abilities are disabled in your debug profile."));
        for (String event : List.of("spawn-minion-wave", "damage-tower", "capture-point")) {
            assertTrue(GameDebugEventCatalog.events(ArcadePortGame.HEROES_OF_GWEN).stream()
                    .anyMatch(entry -> entry.id().equals(event)), event + " is listed in Heroes debug events");
            assertTrue(manager.contains("\"" + event + "\".equals(eventId)"),
                    event + " dispatches to the active Heroes runtime");
        }
        assertTrue(runtime.contains("boolean debugDamageNextHeroesOfGwenTower(Player tester)"));
        assertTrue(runtime.contains("canDamageMobaTower(tower, team)"));
        assertTrue(runtime.contains("damageMobaTower(tower, Math.min(50.0D, before), tester)"));
        assertTrue(runtime.contains("boolean debugCaptureNextHeroesOfGwenPoint(Player tester)"));
        assertTrue(runtime.contains("setCapturePointOwner(point, team)"));
    }

    @Test
    void heroesOfGwenObjectiveAndRoundSystemsHaveLiveProfileSwitches() throws IOException {
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "CastleAssaultHeroesOfGwenRuntime.java");
        Path lobbyPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(runtimePath)) {
            Path base = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au", "com",
                    "testplex", "arcadeports");
            runtimePath = base.resolve("CastleAssaultHeroesOfGwenRuntime.java");
            lobbyPath = base.resolve("ModernArcadeLobbyRuntime.java");
            managerPath = base.resolve("ArcadePortManager.java");
        }
        String runtime = Files.readString(runtimePath);
        String lobby = Files.readString(lobbyPath);
        String manager = Files.readString(managerPath);
        for (String feature : List.of("minion-waves", "minion-combat", "beacon-captures",
                "health-regeneration", "gold-income", "overtime")) {
            assertTrue(lobby.contains("features.put(\"" + feature + "\""), feature + " is available in the Heroes menu");
            assertTrue(runtime.contains("debugFeatureEnabledForActiveSession(\"" + feature + "\")"),
                    feature + " gates its production round system");
        }
        assertTrue(lobby.contains("\"minion-waves\", \"minion-combat\",")
                        && lobby.contains("\"beacon-captures\", \"health-regeneration\", \"gold-income\", \"overtime\").contains(key)"),
                "Heroes round-system switches apply immediately when changed");
        assertTrue(manager.contains("debugFeatureEnabledForActiveSession(type, feature)"),
                "round switches resolve from the active game's selected profile");
    }

    @Test
    void hoopsDebugActionsAndFeatureSwitchesReachPossessionAndReboundGameplay() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        Path testRoot = Path.of("src", "test", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au", "com",
                    "testplex", "arcadeports");
            testRoot = Path.of("modern-platform", "arcade-games-port", "src", "test", "java", "au", "com",
                    "testplex", "arcadeports");
        }
        String runtime = Files.readString(sourceRoot.resolve("HoopsRemasteredRuntime.java"));
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String events = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        String rules = Files.readString(testRoot.resolve("HoopsRemasteredRulesTest.java"));
        for (String feature : List.of("passing", "blocking", "rebounding", "hops")) {
            assertTrue(lobby.contains("features.put(\"" + feature + "\""), feature + " appears in the Hoops menu");
            assertTrue(runtime.contains("debugFeatureEnabled(" + (feature.equals("passing") ? "passer" :
                    feature.equals("blocking") ? "defender" : feature.equals("hops") ? "player" : "player")
                    + ", \"" + feature + "\")"), feature + " gates its production action");
        }
        assertTrue(runtime.contains("spawnLooseBall(current, drop, new Vector(0.0D, 0.12D, 0.0D), BallMode.SHOT,")
                        && runtime.contains("tester.getUniqueId(), null, 1 - current.teams.getOrDefault"),
                "The rebound tool creates a normal shot-mode ball with rebound eligibility.");
        assertTrue(events.contains("new Event(\"drop-rebound-ball\""));
        assertTrue(manager.contains("\"drop-rebound-ball\".equals(eventId)"));
        assertTrue(manager.contains("hoops.debugDropBallForRebound(tester)"));
        assertTrue(rules.contains("offensiveReboundsHaveAShorterShotClockThanDefensiveRebounds"));
    }

    @Test
    void dragonEscapeDebugControlsFinishDetectionAndNormalCompletion() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        Path catalogPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "GameDebugEventCatalog.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
            runtimePath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
            catalogPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "GameDebugEventCatalog.java");
        }
        String manager = Files.readString(managerPath);
        String runtime = Files.readString(runtimePath);
        String catalog = Files.readString(catalogPath);
        assertTrue(catalog.contains("new Event(\"finish-run\", \"Complete the Course\""));
        assertTrue(runtime.contains("\"finish-detection\", \"Course finish detection\""));
        assertTrue(manager.contains("\"finish-run\".equals(eventId)"));
        assertTrue(manager.contains("\"finish-detection\", true"));
        assertTrue(manager.contains("recordDragonEscapeFinish(session, tester"));
    }

    @Test
    void dragonsKitSwitchesGatePerksAsWellAsActivation() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        assertTrue(source.contains("isDebugAbilityAllowed(player, ArcadePortGame.DRAGONS, \"pyrotechnic\")"));
        assertTrue(source.contains("isDebugAbilityAllowed(player, ArcadePortGame.DRAGONS, \"marksman\")"));
        assertTrue(source.contains("isDebugAbilityAllowed(controller, ArcadePortGame.DRAGONS, \"tamer\")"));
    }

    @Test
    void runnerTrailSwitchStopsNewMarksAndRestoresExistingTrail() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        Path lobbyPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
            lobbyPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
        }
        String source = Files.readString(managerPath);
        String runner = Files.readString(managerPath.resolveSibling("RunnerManager.java"));
        String lobby = Files.readString(lobbyPath);
        assertTrue(lobby.contains("\"runner-trail\", \"Runner block trail\""));
        assertTrue(source.contains("debugFeatureEnabled(owner.getUniqueId(), ArcadePortGame.RUNNER, \"runner-trail\", true)"));
        assertTrue(source.contains("runnerController.processBlocks(session, now, this::runnerTrailAllowed)"));
        assertTrue(runner.contains("session.runnerOriginalBlockData.remove(block)"));
        assertTrue(runner.contains("session.runnerBlockOwners.remove(block)"));
    }

    @Test
    void drawMyThingDebugSwitchesGatePaintingAndPaletteSelection() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        Path lobbyPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
            lobbyPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
        }
        String source = Files.readString(managerPath);
        String lobby = Files.readString(lobbyPath);
        assertTrue(lobby.contains("\"canvas-tools\", \"Canvas drawing tools\""));
        assertTrue(lobby.contains("\"palette-selection\", \"Palette selection\""));
        assertTrue(source.contains("ArcadePortGame.DRAW_MY_THING, \"canvas-tools\", true"));
        assertTrue(source.contains("ArcadePortGame.DRAW_MY_THING, \"palette-selection\", true"));
    }

    @Test
    void witherAssaultDebugSwitchesGateHumanAndWitherAbilities() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        Path lobbyPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
            lobbyPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
        }
        String source = Files.readString(managerPath);
        String lobby = Files.readString(lobbyPath);
        assertTrue(lobby.contains("\"ability-human-leap\", \"Human double jump\""));
        assertTrue(lobby.contains("\"human-revival\", \"Human revival potions\""));
        assertTrue(lobby.contains("List.of(\"archer\", \"medic\", \"editor\", \"wither\")"));
        assertTrue(source.contains("debugKitFeatureEnabled(player, ArcadePortGame.WITHER_ASSAULT, \"wither\")"));
        assertTrue(source.contains("debugKitFeatureEnabled(human, ArcadePortGame.WITHER_ASSAULT, \"medic\")"));
        assertTrue(source.contains("debugKitFeatureEnabled(editor, ArcadePortGame.WITHER_ASSAULT, \"editor\")"));
        assertTrue(source.contains("\"ability-human-leap\", true"));
        assertTrue(source.contains("\"human-revival\", true"));
    }

    @Test
    void hoopsDebugSwitchesGateShootingScoringAndSteals() throws IOException {
        Path runtimePath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "HoopsRemasteredRuntime.java");
        Path lobbyPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        if (!Files.isRegularFile(runtimePath)) {
            runtimePath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "HoopsRemasteredRuntime.java");
            lobbyPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
        }
        String runtime = Files.readString(runtimePath);
        String lobby = Files.readString(lobbyPath);
        assertTrue(lobby.contains("\"shooting\", \"Shooting\""));
        assertTrue(lobby.contains("\"scoring\", \"Basket scoring\""));
        assertTrue(lobby.contains("\"steals\", \"Ball steals\""));
        assertTrue(runtime.contains("debugFeatureEnabled(shooter, \"shooting\")"));
        assertTrue(runtime.contains("debugFeatureEnabled(shooter, \"scoring\")"));
        assertTrue(runtime.contains("debugFeatureEnabled(attacker, \"steals\")"));
        assertTrue(runtime.contains("default boolean debugFeatureEnabled(Player player, String feature)"));
    }

    @Test
    void paintballWeaponSwitchesGateActualFiring() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        Path lobbyPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
            lobbyPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java");
        }
        String manager = Files.readString(managerPath);
        String lobby = Files.readString(lobbyPath);
        assertTrue(lobby.contains("List.of(\"rifle\", \"sniper\", \"shotgun\", \"machine-gun\")"));
        assertTrue(manager.contains("ArcadePortGame.PAINTBALL, \"weapon-\" + kit, true"));
        assertTrue(manager.contains("This Paintball weapon is disabled in your debug profile."));
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

    @Test
    void microBattlesDasherDebugEventUsesTheProductionCooldownAndJumpPath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        String events = Files.readString(sourceRoot.resolve("GameDebugEventCatalog.java"));
        assertTrue(events.contains("new Event(\"test-dasher-jump\""));
        assertTrue(manager.contains("\"test-dasher-jump\".equals(eventId)"));
        assertTrue(manager.contains("triggered = useMicroDasherJump(session, tester)"));
        int jump = manager.indexOf("private boolean useMicroDasherJump(Session session, Player player)");
        int end = manager.indexOf("@EventHandler", jump);
        String productionJump = manager.substring(jump, end < 0 ? manager.length() : end);
        assertTrue(productionJump.contains("debugKitFeatureEnabled(player, ArcadePortGame.MICRO_BATTLES, \"dasher\")"));
        assertTrue(productionJump.contains("MICRO_BATTLES_DASHER_JUMP_COOLDOWN_MILLIS"));
        assertTrue(productionJump.contains("player.setVelocity(velocity)"));
        assertTrue(productionJump.contains("player.setFallDistance(0.0F)"));
    }

    @Test
    void spleefDebugSwitchCanSuppressFloorBlockDamageForTheTester() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("features.put(\"block-breaking\""));
        assertTrue(manager.contains("ArcadePortGame.SPLEEF, \"block-breaking\", true"));
    }

    @Test
    void snapOutlineSwitchControlsThePhotoTargetOutlinePath() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String snap = Files.readString(sourceRoot.resolve("CameraShyGame.java"));
        assertTrue(lobby.contains("features.put(\"photo-outlines\""));
        assertTrue(snap.contains("ArcadePortGame.CAMERA_SHY, \"photo-outlines\", true"));
        assertTrue(snap.contains("refreshPhotoOutlines(desiredOutlines)"));
    }

    @Test
    void gladiatorsCombatSwitchGatesDuelDamageForAuthorizedTesters() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("features.put(\"combat-damage\""));
        assertTrue(manager.contains("ArcadePortGame.GLADIATORS, \"combat-damage\", true"));
    }

    @Test
    void dragonEscapeMovementSwitchGatesDouglasCourseMovement() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java"));
        assertTrue(lobby.contains("features.put(\"dragon-movement\""));
        assertTrue(manager.contains("ArcadePortGame.DRAGON_ESCAPE, \"dragon-movement\")"));
        assertTrue(manager.contains("if (dragonMovementEnabled) location = moveDragonEscapeDragon(session, target)"));
    }

    @Test
    void immediateRuntimeFeatureControlsAreMarkedImmediateInTheMenu() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String lobby = Files.readString(sourceRoot.resolve("ModernArcadeLobbyRuntime.java"));
        assertTrue(lobby.contains("Set.of(\"build-editing\", \"combat-damage\").contains(key)"));
        assertTrue(lobby.contains("Set.of(\"spell-casting\", \"chest-looting\").contains(key)"));
    }
}
