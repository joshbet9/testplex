package au.com.testplex.wizards;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class WizardsModernPlugin extends JavaPlugin implements CommandExecutor {
    private WizardsGameManager game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        game = new WizardsGameManager(this);
        if (getCommand("wizards") == null) {
            getLogger().severe("Command 'wizards' is missing from plugin.yml; disabling WizardsModern.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("wizards").setExecutor(this);
        getLogger().info("WizardsModern loaded without a server resource-pack dependency; /wizards help.");
    }

    @Override
    public void onDisable() {
        if (game != null) game.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                game.join(player);
                return true;
            case "start":
                game.start(sender);
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
                    player.sendMessage(ChatColor.YELLOW + "Usage: /wizards kit <mage|sorcerer|mystic|witchdoctor>");
                    return true;
                }
                WizardsKit kit = WizardsKit.parse(args[1]);
                if (kit == null) {
                    player.sendMessage(ChatColor.RED + "Unknown kit. Choose mage, sorcerer, mystic or witchdoctor.");
                    return true;
                }
                game.setKit(player, kit);
                return true;
            case "status":
                sender.sendMessage(ChatColor.GOLD + "Wizards: " + ChatColor.WHITE + game.status());
                return true;
            case "stop":
                if (!sender.hasPermission("testplex.wizards.admin")) {
                    sender.sendMessage(ChatColor.RED + "Operator permission required.");
                    return true;
                }
                game.stop("Wizards test stopped by an operator.");
                return true;
            default:
                sender.sendMessage(ChatColor.GOLD + "=== Wizards Test ===");
                sender.sendMessage(ChatColor.YELLOW + "/wizards join" + ChatColor.GRAY + " — queue for the next round");
                sender.sendMessage(ChatColor.YELLOW + "/wizards kit <mage|sorcerer|mystic|witchdoctor>" + ChatColor.GRAY + " — choose a kit");
                sender.sendMessage(ChatColor.YELLOW + "/wizards start" + ChatColor.GRAY + " — start queued players");
                sender.sendMessage(ChatColor.YELLOW + "/wizards status" + ChatColor.GRAY + " — inspect the round");
                sender.sendMessage(ChatColor.YELLOW + "/wizards leave" + ChatColor.GRAY + " — leave and restore your lobby state");
                sender.sendMessage(ChatColor.YELLOW + "/wizards stop" + ChatColor.GRAY + " — operator reset");
                return true;
        }
    }
}
