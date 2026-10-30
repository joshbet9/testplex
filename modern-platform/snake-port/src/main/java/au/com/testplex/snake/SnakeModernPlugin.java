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
                    player.sendMessage(ChatColor.YELLOW + "Usage: /snake kit <speed|reverse|invulnerable>");
                    return true;
                }
                game.setKit(player, args[1]);
                return true;
            case "status":
                sender.sendMessage(ChatColor.GOLD + "Snake: " + ChatColor.WHITE + game.status());
                return true;
            case "stop":
                if (!sender.hasPermission("testplex.snake.admin")) {
                    sender.sendMessage(ChatColor.RED + "Operator permission required.");
                    return true;
                }
                game.stop("Snake test stopped by an operator.");
                return true;
            default:
                sender.sendMessage(ChatColor.GOLD + "=== Snake Test ===");
                sender.sendMessage(ChatColor.YELLOW + "/snake join" + ChatColor.GRAY + " — join the next round");
                sender.sendMessage(ChatColor.YELLOW + "/snake start" + ChatColor.GRAY + " — start queued players (solo allowed for testing)");
                sender.sendMessage(ChatColor.YELLOW + "/snake kit <speed|reverse|invulnerable>" + ChatColor.GRAY + " — select a kit");
                sender.sendMessage(ChatColor.YELLOW + "/snake status" + ChatColor.GRAY + " — inspect the round");
                sender.sendMessage(ChatColor.YELLOW + "/snake leave" + ChatColor.GRAY + " — leave and restore your lobby state");
                sender.sendMessage(ChatColor.YELLOW + "/snake stop" + ChatColor.GRAY + " — operator reset");
                return true;
        }
    }
}
