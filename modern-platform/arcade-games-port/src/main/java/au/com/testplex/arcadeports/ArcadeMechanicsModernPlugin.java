package au.com.testplex.arcadeports;

import me.monstermaze.MonsterMazePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArcadeMechanicsModernPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private ArcadePortManager manager;
    private PlexballGame plexball;
    private MonsterMazePlugin monsterMaze;
    private ArcadeMapSelectionMenu mapMenu;
    private CameraShyGame cameraShy;
    private final MonsterMazeRegistrationReadiness monsterMazeReadiness = new MonsterMazeRegistrationReadiness();

    // Keep aligned with ArcadeCatalog's READY_FOR_TEST statuses.
    private static final java.util.List<String> READY_FOR_TEST_GAME_IDS = java.util.List.of(
            "death-tag", "one-in-the-quiver", "bomb-lobbers", "bacon-brawl",
            "gladiators", "micro-battles", "paintball", "plexball", "spleef", "super-sploor", "camera-shy");
    private static final String PLAYABLE_GAMES_REVISION = "shared-acceptance-queue-super-sploor-camera-shy-20261101-v1";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Replace the previous broad acceptance list once, then enforce the reviewed
        // READY_FOR_TEST allow-list on every startup so old VM configs cannot re-enable WIP games.
        if (!PLAYABLE_GAMES_REVISION.equals(getConfig().getString("playable-games-revision", ""))) {
            getConfig().set("enabled-games", new java.util.ArrayList<>(READY_FOR_TEST_GAME_IDS));
            getConfig().set("playable-games-revision", PLAYABLE_GAMES_REVISION);
        }
        java.util.List<String> enabledGames = new java.util.ArrayList<>(getConfig().getStringList("enabled-games"));
        enabledGames.removeIf(gameId -> !READY_FOR_TEST_GAME_IDS.contains(gameId));
        enabledGames = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(enabledGames));
        getConfig().set("enabled-games", enabledGames);

        String selectedLobbyGame = getConfig().getString("lobby-selected-game", "death-tag");
        if (!enabledGames.contains(selectedLobbyGame)) {
            getConfig().set("lobby-selected-game", enabledGames.isEmpty() ? "death-tag" : enabledGames.get(0));
        }
        saveConfig();

        // Bring up Testplex's shared lobby before any optional game module. A failure
        // in Monster Maze must not leave players in the unrelated survival world.
        manager = new ArcadePortManager(this);
        cameraShy = new CameraShyGame(this, manager);
        manager.setCameraShyModule(cameraShy);
        getServer().getPluginManager().registerEvents(cameraShy, this);
        plexball = new PlexballGame(this);
        getServer().getPluginManager().registerEvents(plexball, this);
        mapMenu = new ArcadeMapSelectionMenu(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(mapMenu, this);
        getCommand("stats").setExecutor(this);
        getCommand("arcadeport").setExecutor(this);
        publishArcadeRuntimeReadiness();

        // Monster Maze remains embedded in this Arcade host, but is not currently
        // on the playable allow-list. Keep its startup failure isolated from the
        // games which have been cleared for the shared acceptance test queue.
        try {
            monsterMazeReadiness.initializeAndAttach(
                    () -> {
                        monsterMaze = new MonsterMazePlugin(this);
                        monsterMaze.enable();
                    },
                    () -> monsterMaze != null && monsterMaze.getGameManager() != null,
                    () -> manager.setMonsterMazeModule(monsterMaze),
                    () -> manager.isMonsterMazeModuleRegistered(monsterMaze));
            publishMonsterMazeReadiness();
        } catch (RuntimeException | Error startupFailure) {
            monsterMazeReadiness.markFailed();
            getLogger().log(java.util.logging.Level.SEVERE,
                    "Monster Maze did not initialise inside Testplex Arcade. The shared lobby and cleared Arcade games "
                            + "remain available; Monster Maze stays unavailable until this module fault is fixed.",
                    startupFailure);
            if (monsterMaze != null) {
                try {
                    monsterMaze.shutdown();
                } catch (Throwable shutdownFailure) {
                    getLogger().log(java.util.logging.Level.WARNING,
                            "Monster Maze cleanup after failed startup was incomplete.", shutdownFailure);
                }
                monsterMaze = null;
            }
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTask(this, () -> {
                if (online.isOnline() && manager != null) {
                    boolean restoredCameraShy = cameraShy != null && cameraShy.restorePendingSnapshot(online);
                    if (!restoredCameraShy) manager.returnPlayerToLobby(online);
                    grantConfiguredAdmin(online);
                    manager.restoreMonsterMazeKitPreference(online);
                }
            });
        }
    }

    public MonsterMazePlugin getMonsterMazeModule() { return monsterMaze; }
    public ArcadePortManager getArcadePortManager() { return manager; }
    public PlexballGame getPlexballGame() { return plexball; }
    public void returnPlayerToArcadeLobby(Player player) {
        if (manager != null && player != null && player.isOnline()) {
            manager.returnPlayerToLobby(player);
            // Monster Maze calls this once per participant during its end cleanup.
            // Defer the idle check so its GameManager can finish transitioning to IDLE.
            Bukkit.getScheduler().runTask(this, () -> {
                if (manager != null) manager.cleanupTestPlayersIfNoGameIsRunning();
            });
        }
    }
    public void stopMonsterMazeRound(String reason) {
        if (manager != null) manager.stop(reason == null ? "Monster Maze stopped" : reason);
    }

    public boolean startMonsterMazeTournamentRound(java.util.List<Player> participants) {
        return manager != null && manager.startMonsterMazeTournamentRound(participants);
    }

    @Override
    public void onDisable() {
        try {
            ArcadeRuntimeReadinessSignal.remove(getDataFolder().toPath());
            MonsterMazeRegistrationSignal.remove(getDataFolder().toPath());
        } catch (java.io.IOException exception) {
            getLogger().warning("Could not remove an Arcade runtime readiness record: " + exception.getMessage());
        }
        if (manager != null) manager.shutdown();
        if (plexball != null) plexball.shutdown();
        if (monsterMaze != null) monsterMaze.shutdown();
    }

    private void publishArcadeRuntimeReadiness() {
        try {
            boolean deploymentSignal = ArcadeRuntimeReadinessSignal.publish(
                    getDataFolder().toPath(), System.getenv("INVOCATION_ID"), ProcessHandle.current().pid());
            getLogger().info("TESTPLEX_ARCADE_LOBBY_READY world=" + ModernArcadeLobby.WORLD_NAME
                    + " plane=100x100 floor-y=" + ModernArcadeLobby.FLOOR_Y
                    + " deployment-signal=" + deploymentSignal);
            if (!deploymentSignal && System.getenv("TESTPLEX_DEPLOYED_BUILD_SHA") != null) {
                throw new IllegalStateException("Systemd deployment identity is missing from "
                        + getDataFolder() + "; refusing to report Arcade lobby as deployment-ready.");
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not publish Testplex Arcade lobby readiness.", exception);
        }
    }

    private void publishMonsterMazeReadiness() {
        monsterMazeReadiness.requireReady();
        if (manager == null || !manager.isMonsterMazeModuleRegistered(monsterMaze)) {
            monsterMazeReadiness.markFailed();
            throw new IllegalStateException("Monster Maze readiness cannot be published before Arcade manager attachment.");
        }
        try {
            boolean deploymentSignal = MonsterMazeRegistrationSignal.publish(getDataFolder().toPath(),
                    monsterMazeReadiness, System.getenv("INVOCATION_ID"), ProcessHandle.current().pid());
            getLogger().info("TESTPLEX_ARCADE_MODULE_READY module=monster-maze manager=testplex-arcade "
                    + "game-manager=initialized deployment-signal=" + deploymentSignal);
        } catch (java.io.IOException exception) {
            monsterMazeReadiness.markFailed();
            throw new IllegalStateException("Could not publish Monster Maze post-attachment readiness.", exception);
        }
    }

    /** Public bridge for standalone games whose mechanics live in a separate plugin JAR. */
    public void recordSnakeStats(java.util.UUID playerId, boolean won, int kills, boolean knockedOut,
                                 int lifetimeKills, int lifetimeSlimes, int bestTailLength,
                                 java.util.Set<String> achievements) {
        if (manager != null) {
            manager.recordSnakeStats(playerId, won, kills, knockedOut,
                    lifetimeKills, lifetimeSlimes, bestTailLength, achievements);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline() && manager != null) {
                // Session fake players must remain in the game lifecycle that just admitted them.
                if (manager.isTestPlayer(player)) return;
                // A Camera Shy disconnect must restore the captured pre-match snapshot
                // instead of leaving the temporary spyglass/kit inventory on reconnect.
                boolean restoredCameraShy = cameraShy != null && cameraShy.restorePendingSnapshot(player);
                if (!restoredCameraShy) manager.returnPlayerToLobby(player);
                grantConfiguredAdmin(player);
                manager.restoreMonsterMazeKitPreference(player);
            }
        });
    }

    private boolean isConfiguredOwner(Player player) {
        java.util.List<String> owners = new java.util.ArrayList<>(getConfig().getStringList("owner-usernames"));
        if (owners.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) owners.add("jn_playzmc");
        return owners.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
    }

    private boolean hasAdminAccess(CommandSender sender) {
        if (sender.isOp() || sender.hasPermission("testplex.arcadeports.admin")) return true;
        if (!(sender instanceof Player player)) return false;

        // The sandbox owner must not be blocked by a stale OP/permission cache.
        if (isConfiguredOwner(player)) {
            if (!player.isOp()) player.setOp(true);
            return true;
        }

        java.util.List<String> configured = new java.util.ArrayList<>(getConfig().getStringList("admin-usernames"));
        if (configured.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) configured.add("jn_playzmc");
        boolean match = configured.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        if (match && !player.isOp()) player.setOp(true);
        return match;
    }

    private void grantConfiguredAdmin(Player player) {
        java.util.List<String> configured = new java.util.ArrayList<>(getConfig().getStringList("admin-usernames"));
        configured.addAll(getConfig().getStringList("owner-usernames"));
        if (configured.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) configured.add("jn_playzmc");
        boolean match = configured.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        if (!match) return;
        if (!player.isOp()) {
            player.setOp(true);
            getLogger().info("Granted configured Testplex owner/admin operator access to " + player.getName() + ".");
        }
    }

    /** Resolve supported Mixed Arcade aliases without changing the base game ID. */
    private ArcadePortGame mixedArcadeGameFromId(String requestedId) {
        if (requestedId == null) return null;
        String normalized = requestedId.toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace(' ', '-');
        String baseId = null;
        if (normalized.endsWith("-mixed")) baseId = normalized.substring(0, normalized.length() - "-mixed".length());
        else if (normalized.startsWith("mixed-")) baseId = normalized.substring("mixed-".length());
        if (baseId == null) return null;
        ArcadePortGame game = ArcadePortGame.fromId(baseId);
        return game == ArcadePortGame.TURF_WARS || game == ArcadePortGame.CASTLE_ASSAULT
                || game == ArcadePortGame.HEROES_OF_GWEN ? game : null;
    }

    private boolean isMixedSkyWarsId(String requestedId) {
        if (requestedId == null) return false;
        String normalized = requestedId.toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace(' ', '-');
        return normalized.equals("skywars") || normalized.equals("sky-wars")
                || normalized.equals("skywars-mixed") || normalized.equals("sky-wars-mixed")
                || normalized.equals("mixed-skywars") || normalized.equals("mixed-sky-wars");
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (cameraShy == null) {
            viewer.sendMessage(ChatColor.RED + "Camera Shy progression is unavailable.");
            return true;
        }

        Player subject = viewer;
        int index = 0;
        if (args.length > 0 && !"camera-shy".equalsIgnoreCase(args[0])) {
            subject = Bukkit.getPlayerExact(args[0]);
            if (subject == null) {
                viewer.sendMessage(ChatColor.RED + "That player is not online.");
                viewer.sendMessage(ChatColor.YELLOW + "Usage: /stats [player] [camera-shy] [page]");
                return true;
            }
            index = 1;
        }
        if (args.length > index && "camera-shy".equalsIgnoreCase(args[index])) index++;

        int page = 1;
        if (args.length > index) {
            try {
                page = Integer.parseInt(args[index]);
            } catch (NumberFormatException invalidPage) {
                viewer.sendMessage(ChatColor.YELLOW + "Page must be a number from 1 to "
                        + CameraShyProgressCatalog.pageCount() + ".");
                return true;
            }
        }
        if (args.length > index + 1 || page < 1 || page > CameraShyProgressCatalog.pageCount()) {
            viewer.sendMessage(ChatColor.YELLOW + "Usage: /stats [player] [camera-shy] [page 1-"
                    + CameraShyProgressCatalog.pageCount() + "]");
            return true;
        }
        cameraShy.sendStats(viewer, subject, page);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("stats".equalsIgnoreCase(command.getName())
                || "stats".equalsIgnoreCase(label) || "arcadestats".equalsIgnoreCase(label)) {
            if (CameraShyRules.routesToPlexballStats(args)) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (plexball != null) plexball.openStats(player);
                else sender.sendMessage(ChatColor.RED + "Plexball progression is unavailable.");
                return true;
            }
            return handleStatsCommand(sender, args);
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "join":
            case "play":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport " + sub + " <game-id>");
                    return true;
                }
                ArcadePortGame mixedJoinGame = mixedArcadeGameFromId(args[1]);
                boolean mixedArcadeVariant = mixedJoinGame != null;
                ArcadePortGame requestedGame = mixedArcadeVariant ? mixedJoinGame : ArcadePortGame.fromId(args[1]);
                manager.queue(requestedGame, player, sub.equals("play"), mixedArcadeVariant);
                return true;
            case "leave":
                if (!(sender instanceof Player leavingPlayer)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) {
                    if (cameraShy != null) cameraShy.leaveQueue(leavingPlayer);
                    return true;
                }
                if ("camera-shy".equalsIgnoreCase(args[1])) {
                    if (cameraShy != null) cameraShy.leaveQueue(leavingPlayer);
                    return true;
                }
                ArcadePortGame leavingGame = ArcadePortGame.fromId(args[1]);
                if (leavingGame == null || !manager.leaveQueue(leavingGame, leavingPlayer)) {
                    sender.sendMessage(ChatColor.YELLOW + "You are not queued or spectating that Arcade game.");
                }
                return true;
            case "spectate":
                if (!(sender instanceof Player spectator)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2 || !"plexball".equalsIgnoreCase(args[1])) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport spectate plexball");
                    return true;
                }
                if (plexball != null) plexball.spectate(spectator);
                else sender.sendMessage(ChatColor.RED + "Plexball is unavailable.");
                return true;
            case "lobby":
            case "selectgame":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport lobby <game-id>");
                    return true;
                }
                ArcadePortGame selectedLobbyGame = ArcadePortGame.fromId(args[1]);
                if (selectedLobbyGame == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown game id.");
                    return true;
                }
                manager.selectLobbyGame(selectedLobbyGame);
                sender.sendMessage(ChatColor.GREEN + selectedLobbyGame.displayName()
                        + " selected. Kit and queue datapoints are ready in the lobby.");
                return true;
            case "bot":
            case "bots":
                manager.handleBotCommand(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
                return true;
            case "maps":
            case "map":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport maps <game-id>");
                    return true;
                }
                mapMenu.openMapSelection(player, ArcadePortGame.fromId(args[1]));
                return true;
            case "start":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport start <game-id>");
                    return true;
                }
                if ("plexball".equalsIgnoreCase(args[1])) {
                    if (!hasAdminAccess(sender)) {
                        sender.sendMessage(ChatColor.RED + "Operator permission required to force-start Plexball.");
                        return true;
                    }
                    manager.startQueued(ArcadePortGame.PLEXBALL, null, sender);
                    return true;
                }
                if (plexball != null && plexball.isActive()) {
                    sender.sendMessage(ChatColor.RED + "Plexball is currently using the Arcade test arena.");
                    return true;
                }
                ArcadePortGame mixedStartGame = mixedArcadeGameFromId(args[1]);
                boolean mixedArcadeStart = mixedStartGame != null;
                ArcadePortGame requestedStartGame = mixedArcadeStart ? mixedStartGame : ArcadePortGame.fromId(args[1]);
                manager.startFromLobby(requestedStartGame, sender, mixedArcadeStart);
                return true;
            case "kit":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport kit <kit-id|plexball>");
                    return true;
                }
                if ("plexball".equalsIgnoreCase(args[1])) {
                    if (plexball != null) plexball.openKitMenu(player);
                    else sender.sendMessage(ChatColor.RED + "Plexball is unavailable.");
                    return true;
                }
                if (plexball != null && plexball.selectKit(player, args[1])) return true;
                manager.selectKit(player, args[1]);
                return true;
            case "ghost":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                manager.setDragonEscapeGhost(player, args.length > 1 ? args[1] : "toggle");
                return true;
            case "dragoncolor":
            case "dragoncolour":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport dragoncolor <" + String.join("|", ArcadePortRules.DRAGON_ESCAPE_BEACON_COLORS) + ">");
                    sender.sendMessage(ChatColor.GRAY + "Current beacon colour: " + manager.dragonEscapeBeaconColor(player.getUniqueId()));
                } else manager.setDragonEscapeBeaconColor(player, args[1]);
                return true;
            case "stats":
                if (!(sender instanceof Player statsPlayer)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2 || !"plexball".equalsIgnoreCase(args[1])) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport stats plexball");
                    return true;
                }
                if (plexball != null) plexball.openStats(statsPlayer);
                else sender.sendMessage(ChatColor.RED + "Plexball progression is unavailable.");
                return true;
            case "status":
                sender.sendMessage(ChatColor.GOLD + "Modern Arcade ports: " + ChatColor.WHITE + manager.status());
                if (plexball != null) sender.sendMessage(ChatColor.GOLD + "Plexball: " + ChatColor.WHITE + plexball.status());
                return true;
            case "stop":
                if (!hasAdminAccess(sender)) {
                    sender.sendMessage(ChatColor.RED + "Operator permission required.");
                    return true;
                }
                manager.stop("Stopped by an operator.");
                return true;
            default:
                sender.sendMessage(ChatColor.GOLD + "=== Modern Arcade Mechanics Prototypes ===");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport play <game-id>" + ChatColor.GRAY + " — quick solo test; starts after a short join window");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport play plexball" + ChatColor.GRAY + " — queue for Plexball");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport join plexball" + ChatColor.GRAY + " — join the Plexball queue");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport spectate plexball" + ChatColor.GRAY + " — watch an active Plexball match");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport leave plexball" + ChatColor.GRAY + " — leave the queue or stop spectating");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport kit <slugger|technician|curve-artist|sprinter|skyguard>" + ChatColor.GRAY + " — select a Plexball kit");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport kit plexball" + ChatColor.GRAY + " — open the visual Plexball kit selector");
                sender.sendMessage(ChatColor.YELLOW + "/stats plexball" + ChatColor.GRAY + " — view Plexball achievements, milestones and career stats");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport join <game-id>" + ChatColor.GRAY + " — queue to join a multiplayer test");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport start <game-id>" + ChatColor.GRAY + " — start a queued test now");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport play <game-id>-mixed" + ChatColor.GRAY
                        + " — Mixed Arcade variants: Turf Wars, Castle Assault and Heroes of Gwen");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport lobby camera-shy" + ChatColor.GRAY + " — select Camera Shy and show its five lobby kit choices");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport kit <kit-id>" + ChatColor.GRAY + " — choose a game kit; Camera Shy IDs: camera-shy-smoke-artist, camera-shy-decoy, camera-shy-runner, camera-shy-spotter, camera-shy-understudy");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport leave" + ChatColor.GRAY + " — leave the Camera Shy queue");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport ghost <on|off|toggle>" + ChatColor.GRAY + " — show or hide your Dragon Escape PB ghost");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport dragoncolor <colour>" + ChatColor.GRAY + " — set your Dragon Escape leader beacon colour");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport status" + ChatColor.GRAY + " — active game and queue counts");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport stop" + ChatColor.GRAY + " — operator reset");
                sender.sendMessage(ChatColor.DARK_GRAY + "Cleared game IDs: " + manager.enabledGamesLabel());
                return true;
        }
    }
}
