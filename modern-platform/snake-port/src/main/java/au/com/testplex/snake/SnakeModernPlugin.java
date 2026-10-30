package au.com.testplex.snake;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SnakeModernPlugin extends JavaPlugin implements CommandExecutor {
    private SnakeGameManager game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        game = new SnakeGameManager(this);
        if (!game.isAvailable()) {
            getLogger().severe("SnakeModern could not load an original Snake map with valid TEAM_SPAWNS. Disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getCommand("snake") == null) {
            getLogger().severe("Command 'snake' is missing from plugin.yml; disabling SnakeModern.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("snake").setExecutor(this);
        getLogger().info("SnakeModern loaded in isolated test mode; /snake help for commands.");
    }

    /** Optional bridge to the platform's per-player Snake cosmetic line. */
    String selectedCosmeticOption(Player player, String group) {
        if (player == null || group == null) return null;
        org.bukkit.plugin.Plugin platform = getServer().getPluginManager().getPlugin("TestplexModernArcade");
        if (platform == null || !platform.isEnabled()) return null;
        try {
            java.lang.reflect.Method method = platform.getClass().getMethod(
                    "getSelectedCosmeticOption", java.util.UUID.class, String.class, String.class);
            Object value = method.invoke(platform, player.getUniqueId(), "snake", group);
            return value instanceof String text && !text.isBlank() ? text : null;
        } catch (ReflectiveOperationException exception) {
            getLogger().warning("Could not read Snake cosmetic selection from TestplexModernArcade: " + exception.getMessage());
            return null;
        }
    }

    @Override
    public void onDisable() {
        if (game != null) game.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "join":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                game.join(player);
                return true;
            case "start":
                game.start(sender instanceof Player ? (Player) sender : null, sender);
                return true;
            case "leave":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                game.leave(player);
                return true;
            case "kit":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) {
                    game.openKitMenu(player);
                    return true;
                }
                game.setKit(player, args[1]);
                return true;
            case "status":
                sender.sendMessage(ChatColor.GOLD + "Snake: " + ChatColor.WHITE + game.status());
                return true;
            case "maps":
                game.showMaps(sender);
                return true;
            case "map":
                if (!(sender instanceof Player mapPlayer)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) game.showMaps(sender);
                else game.selectMap(mapPlayer, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
                return true;
            case "stats":
                if (!(sender instanceof Player statsPlayer)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                game.showStats(statsPlayer);
                return true;
            case "stop":
                if (!sender.hasPermission("testplex.snake.admin")) {
                    sender.sendMessage(ChatColor.RED + "Operator permission required.");
                    return true;
                }
                game.stop("Snake test stopped by an operator.");
                return true;
            default:
                sender.sendMessage(ChatColor.GOLD + "=== Snake ===");
                sender.sendMessage(ChatColor.YELLOW + "/snake join" + ChatColor.GRAY + " — join the next round");
                sender.sendMessage(ChatColor.YELLOW + "/snake start" + ChatColor.GRAY + " — start queued players (solo allowed for testing)");
                sender.sendMessage(ChatColor.YELLOW + "/snake kit [speed|reverse|invulnerable]" + ChatColor.GRAY + " — choose a kit from the menu or select by name");
                sender.sendMessage(ChatColor.YELLOW + "/snake status" + ChatColor.GRAY + " — inspect the round");
                sender.sendMessage(ChatColor.YELLOW + "/snake maps" + ChatColor.GRAY + " — list the original Snake maps");
                sender.sendMessage(ChatColor.YELLOW + "/snake map <name>" + ChatColor.GRAY + " — select an original map");
                sender.sendMessage(ChatColor.YELLOW + "/snake stats" + ChatColor.GRAY + " — inspect your Snake stat totals and achievements");
                sender.sendMessage(ChatColor.YELLOW + "/snake leave" + ChatColor.GRAY + " — leave and restore your lobby state");
                sender.sendMessage(ChatColor.YELLOW + "/snake stop" + ChatColor.GRAY + " — operator reset");
                return true;
        }
    }
}
