package nautilus.game.arcade.cosmetics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple persistent in-game selector for Arcade cosmetic experiments.
 *
 * All prototype choices are open on this private test server. Gameplay adapters
 * consume the selected keys through getEquippedChoice; cosmetic selections are
 * stored per UUID so they survive server restarts and name changes.
 */
public final class ArcadeCosmeticsMenu implements CommandExecutor, Listener {
    private static final String MAIN_TITLE = ChatColor.BLACK + "Arcade Cosmetics";
    private static final String CATEGORY_PREFIX = "Arcade Cosmetics: ";
    private static ArcadeCosmeticsMenu instance;

    private final JavaPlugin plugin;
    private final File dataFile;
    private final FileConfiguration data;
    private final Map<String, List<String>> choices = new LinkedHashMap<String, List<String>>();

    public ArcadeCosmeticsMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        instance = this;

        choices.put("runner", Arrays.asList("white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray"));
        choices.put("baconbrawl", Arrays.asList("crown", "top_hat", "chef", "diamond"));
        choices.put("dragonescape", Arrays.asList("white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"));
        choices.put("evolution", Collections.singletonList("dragon-egg"));
        choices.put("microbattles", Arrays.asList("aqua", "ember", "forest", "royal", "sunset", "solar", "frost", "void"));
        choices.put("snake", Arrays.asList("elemental", "neon", "sunset"));

        if (plugin.getCommand("cosmetics") != null) plugin.getCommand("cosmetics").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Arcade cosmetics menu registered. Open it with /cosmetics.");
    }

    public static String getEquippedChoice(UUID playerId, String game) {
        if (instance == null || playerId == null || game == null) return "";
        String value = instance.data.getString("players." + playerId.toString() + "." + game.toLowerCase(Locale.ROOT), "");
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void setEquippedChoice(UUID playerId, String game, String choice) {
        data.set("players." + playerId.toString() + "." + game, choice == null ? "" : choice);
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save cosmetics.yml: " + ex.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Run /cosmetics in-game to open the cosmetic selector.");
            return true;
        }
        openMain((Player) sender);
        return true;
    }

    private void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, MAIN_TITLE);
        put(inventory, 10, gameIcon(player, "runner", "Runner", Material.STAINED_CLAY, (short) 14));
        put(inventory, 11, gameIcon(player, "baconbrawl", "Bacon Brawl", Material.PORK, (short) 0));
        put(inventory, 12, gameIcon(player, "dragonescape", "Dragon Escape", Material.BEACON, (short) 0));
        put(inventory, 14, gameIcon(player, "evolution", "Evolution", Material.DRAGON_EGG, (short) 0));
        put(inventory, 15, gameIcon(player, "microbattles", "Micro Battles", Material.STAINED_GLASS, (short) 3));
        put(inventory, 16, gameIcon(player, "snake", "Snake", Material.MONSTER_EGG, (short) 91));
        player.openInventory(inventory);
    }

    private ItemStack gameIcon(Player player, String game, String label, Material material, short durability) {
        ItemStack item = new ItemStack(material, 1, durability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + label);
        List<String> lore = new ArrayList<String>();
        String equipped = getEquippedChoice(player.getUniqueId(), game);
        lore.add(ChatColor.GRAY + "Equipped: " + (equipped.isEmpty() ? "Default" : pretty(equipped)));
        lore.add(ChatColor.GRAY + "Click to view choices");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }


    private void openCategory(Player player, String game) {
        List<String> options = choices.get(game);
        if (options == null) return;
        String displayGame = displayGame(game);
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BLACK + CATEGORY_PREFIX + ChatColor.YELLOW + displayGame);
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            put(inventory, i, choiceItem(player, game, option));
        }
        put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to all games"));
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Unequip Cosmetic", ChatColor.GRAY + "Restore the game's default appearance"));
        put(inventory, 53, named(Material.BOOK, ChatColor.AQUA + "How to use", ChatColor.GRAY + "Click a cosmetic to equip it."));
        player.openInventory(inventory);
    }

    private ItemStack choiceItem(Player player, String game, String choice) {
        Material material = choiceMaterial(game, choice);
        short durability = choiceDurability(game, choice);
        ItemStack item = new ItemStack(material, 1, durability);
        ItemMeta meta = item.getItemMeta();
        boolean equipped = choice.equalsIgnoreCase(getEquippedChoice(player.getUniqueId(), game));
        meta.setDisplayName((equipped ? ChatColor.GREEN + "EQUIPPED " : ChatColor.YELLOW.toString()) + pretty(choice));
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + describeChoice(game, choice));
        lore.add(equipped ? ChatColor.GREEN + "Currently equipped" : ChatColor.GRAY + "Click to equip");
        lore.add(ChatColor.DARK_GRAY + "Choice: " + game + ":" + choice);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material choiceMaterial(String game, String choice) {
        if ("runner".equals(game)) return Material.STAINED_CLAY;
        if ("baconbrawl".equals(game)) {
            if ("crown".equals(choice)) return Material.GOLD_HELMET;
            if ("top_hat".equals(choice)) return Material.OBSIDIAN;
            if ("chef".equals(choice)) return Material.CAKE;
            return Material.DIAMOND_BLOCK;
        }
        if ("dragonescape".equals(game) || "microbattles".equals(game)) return Material.STAINED_GLASS;
        if ("evolution".equals(game)) return Material.DRAGON_EGG;
        if ("snake".equals(game)) return Material.WOOL;
        return Material.PAPER;
    }

    private short choiceDurability(String game, String choice) {
        if ("runner".equals(game) || "dragonescape".equals(game)) {
            return (short) colourData(choice);
        }
        if ("microbattles".equals(game)) {
            Map<String, Integer> data = new LinkedHashMap<String, Integer>();
            data.put("aqua", 3); data.put("ember", 14); data.put("forest", 5); data.put("royal", 10);
            data.put("sunset", 6); data.put("solar", 4); data.put("frost", 0); data.put("void", 15);
            return (short) data.get(choice).intValue();
        }
        if ("snake".equals(game)) {
            if ("elemental".equals(choice)) return (short) 11;
            if ("neon".equals(choice)) return (short) 9;
            return (short) 14;
        }
        return 0;
    }

    /** Resolve the visible Minecraft 1.8 stained-glass/clay colour for an equipped cosmetic. */
    public static int getEquippedColourData(UUID playerId, String game) {
        String selected = getEquippedChoice(playerId, game);
        if (selected.isEmpty()) return -1;

        if ("microbattles".equalsIgnoreCase(game)) {
            if ("aqua".equals(selected)) return 3;
            if ("ember".equals(selected)) return 14;
            if ("forest".equals(selected)) return 5;
            if ("royal".equals(selected)) return 10;
            if ("sunset".equals(selected)) return 6;
            if ("solar".equals(selected)) return 4;
            if ("frost".equals(selected)) return 0;
            if ("void".equals(selected)) return 15;
            return -1;
        }

        return colourData(selected);
    }

    public static int colourData(String choice) {
        String[] colours = {"white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        for (int i = 0; i < colours.length; i++) if (colours[i].equals(choice)) return i;
        return -1;
    }

    private String describeChoice(String game, String choice) {
        if ("snake".equals(game)) {
            if ("elemental".equals(choice)) return "Blue Super, yellow Speed, purple Reverse";
            if ("neon".equals(choice)) return "Cyan Super, lime Speed, magenta Reverse";
            return "Red Super, pink Speed, orange Reverse";
        }
        if ("runner".equals(game)) return "Custom colour for your disappearing running trail";
        if ("baconbrawl".equals(game)) return "Themed sparkle trail around your pig disguise, visible to other players";
        if ("dragonescape".equals(game)) return "Custom stained-glass dust trail while running, visible to other players";
        if ("evolution".equals(game)) return "Dragon-egg particle effect when evolving";
        return "Personal stained-glass dust trail, visible to other players";
    }

    private ItemStack named(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(loreLine));
        item.setItemMeta(meta);
        return item;
    }

    private void put(Inventory inventory, int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String rawTitle = event.getView().getTitle();
        String title = ChatColor.stripColor(rawTitle);
        if (title == null || !title.startsWith("Arcade Cosmetics")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String itemName = ChatColor.stripColor(meta.getDisplayName());
        if (itemName == null) return;

        if (title.equals("Arcade Cosmetics")) {
            String game = gameFromDisplayName(itemName);
            if (game != null) openCategory(player, game);
            return;
        }

        String game = gameFromTitle(title);
        if (game == null) return;
        if (itemName.equalsIgnoreCase("Back")) {
            openMain(player);
            return;
        }
        if (itemName.equalsIgnoreCase("Unequip Cosmetic")) {
            setEquippedChoice(player.getUniqueId(), game, "");
            player.sendMessage(ChatColor.GREEN + "Unequipped the " + displayGame(game) + " cosmetic.");
            openCategory(player, game);
            return;
        }

        for (String line : meta.getLore() == null ? Collections.<String>emptyList() : meta.getLore()) {
            String plain = ChatColor.stripColor(line);
            if (plain == null || !plain.startsWith("Choice: " + game + ":")) continue;
            String choice = plain.substring(("Choice: " + game + ":").length());
            if (!choices.get(game).contains(choice)) return;
            setEquippedChoice(player.getUniqueId(), game, choice);
            player.sendMessage(ChatColor.GREEN + "Equipped " + pretty(choice) + " for " + displayGame(game) + ".");
            openCategory(player, game);
            return;
        }
    }

    private String gameFromDisplayName(String label) {
        if ("Runner".equals(label)) return "runner";
        if ("Bacon Brawl".equals(label)) return "baconbrawl";
        if ("Dragon Escape".equals(label)) return "dragonescape";
        if ("Evolution".equals(label)) return "evolution";
        if ("Micro Battles".equals(label)) return "microbattles";
        if ("Snake".equals(label)) return "snake";
        return null;
    }

    private String gameFromTitle(String title) {
        if (!title.startsWith(CATEGORY_PREFIX)) return null;
        String display = title.substring(CATEGORY_PREFIX.length()).trim();
        return gameFromDisplayName(display);
    }

    private String displayGame(String game) {
        if ("baconbrawl".equals(game)) return "Bacon Brawl";
        if ("dragonescape".equals(game)) return "Dragon Escape";
        if ("microbattles".equals(game)) return "Micro Battles";
        return game.substring(0, 1).toUpperCase(Locale.ROOT) + game.substring(1);
    }

    private String pretty(String choice) {
        StringBuilder result = new StringBuilder();
        String[] tokens = choice.replace('-', '_').split("_");
        for (String token : tokens) {
            if (result.length() > 0) result.append(' ');
            if (token.length() == 0) continue;
            result.append(token.substring(0, 1).toUpperCase(Locale.ROOT)).append(token.substring(1));
        }
        return result.toString();
    }
}
