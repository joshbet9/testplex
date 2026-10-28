package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArcadeMechanicsModernPlugin extends JavaPlugin implements CommandExecutor {
    private ArcadePortManager manager;

    @Override
    public void onEnable() {
        manager = new ArcadePortManager(this);
        if (getCommand("arcadeport") == null) {
            getLogger().severe("Missing /arcadeport command metadata.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("arcadeport").setExecutor(this);
        getLogger().info("ArcadeMechanicsModern loaded. These are first-pass mechanical prototypes, not final parity ports.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
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
            case "start":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport start <game-id>");
                    return true;
                }
                manager.startQueued(ArcadePortGame.fromId(args[1]), sender instanceof Player ? (Player) sender : null, sender);
                return true;
            case "kit":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport kit <jumper|armorer|pitcher|waller|pig|mama-pig|sheep-pig|snowballer|spleef-brawler|micro-archer|worker|fighter|quiver-leaper|quiver-brawler|enchanter|slam-shot|ninja|rifle|sniper|shotgun|machine-gun|runner-basher|runner-archer|runner-traitor|runner-null>");
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
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport kit <kit-id>" + ChatColor.GRAY + " — choose a Bomb Lobbers, Bacon Brawl, Micro Battles, Spleef, One in the Quiver, Paintball or Death Tag runner kit");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport status" + ChatColor.GRAY + " — active game and queue counts");
                sender.sendMessage(ChatColor.YELLOW + "/arcadeport stop" + ChatColor.GRAY + " — operator reset");
                sender.sendMessage(ChatColor.DARK_GRAY + "IDs: bacon-brawl, bomb-lobbers, death-tag, dragon-escape, dragons, evolution, gladiators, micro-battles, one-in-the-quiver, paintball, runner, sheep-quest, spleef, turf-wars");
                return true;
        }
    }
}
