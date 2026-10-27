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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Testbed-only, persistent selector for the planned per-game Arcade cosmetics.
 *
 * The normal Mineplex Cosmetic Menu remains owned by Mineplex Core. This class
 * injects one jukebox into that menu and opens the prototype selector with its
 * own command. Planned win effects are catalogue placeholders, not live game
 * sequences. Existing colour/skin getters remain source-compatible for the
 * gameplay adapters that are already implemented.
 */
public final class ArcadeCosmeticsMenu implements CommandExecutor, Listener {
    private static final String MAIN_TITLE = "Arcade Cosmetics | Games";
    private static final String TITLE_PREFIX = "Arcade Cosmetics | ";
    private static final int WIN_EFFECT_SLOT = 31;
    private static final int[] GAME_SLOTS = {
            10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43
    };
    private static ArcadeCosmeticsMenu instance;

    private final JavaPlugin plugin;
    private final File dataFile;
    private final FileConfiguration data;
    private final Map<String, GameDefinition> games = new LinkedHashMap<String, GameDefinition>();

    public ArcadeCosmeticsMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        instance = this;
        registerGames();

        if (plugin.getCommand("arcadecosmetics") != null) {
            plugin.getCommand("arcadecosmetics").setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Registered the 16-game Arcade Cosmetics prototype menu. Open it from the jukebox in the normal Cosmetic Menu or with /arcadecosmetics.");
    }

    private void registerGames() {
        registerGame("baconbrawl", "Bacon Brawl", Material.PORK, (short) 0,
                "Torpedo/body-slam into four players, sending them flying off the edge.",
                feature("accessories", "Pig & Sheep Accessories", Material.GOLD_HELMET,
                        "Accessory concepts for pigs and sheep. Selection is saved; visual attachment hooks remain prototype work.",
                        "baconbrawl", false, "crown", "top_hat", "chef", "diamond"));

        registerGame("bomblobbers", "Bomb Lobbers", Material.TNT, (short) 0,
                "Land three direct lob hits on the map's turtles, with suitable sound effects.",
                feature("tnt-impact", "TNT Impact Effects", Material.TNT,
                        "Prototype TNT explosion and impact presentation. Cosmetic particles must not change damage or explosion rules.",
                        null, false, "classic", "ember", "shockwave", "spark", "confetti"));

        registerGame("deathtag", "Death Tag", Material.SKULL_ITEM, (short) 0,
                "Knock three teammates off a shared landmark, such as Pirate Harbour or the flag area on Halfmoon.",
                feature("runner-trail", "Runner Trails", Material.STAINED_GLASS,
                        "Expanded trail colours for runners. Prototype selection only until all Death Tag trail hooks are connected.",
                        null, false, "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"));

        registerGame("dragonescape", "Dragon Escape", Material.DRAGON_EGG, (short) 0,
                "Two players parkour through the finish; the winner leaps ahead to finish first from third place.",
                feature("running-trail", "Running Trail Colour", Material.STAINED_GLASS,
                        "The existing coloured running trail. This option continues to drive the current Dragon Escape colour hook.",
                        "dragonescape", false, "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"),
                feature("death-chat", "Death Chat Lines", Material.NAME_TAG,
                        "Choose a custom line to send when you die. This is a menu prototype; the chat adapter is not yet connected.",
                        null, false, "off", "am-i-cooked", "grok-is-this-true"),
                feature("leader-beacon", "Leader Beacon Colour", Material.BEACON,
                        "The shared beacon should follow the leading player and use that player's selected colour. Hook not yet connected.",
                        null, false, "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"));

        registerGame("dragons", "Dragons", Material.FIREWORK, (short) 0,
                "Use a Pyro platform; the winner is hit by a dragon while everyone else falls.",
                feature("pyro-particles", "Pyro Ability Particles", Material.BLAZE_POWDER,
                        "Choose a visual style for Pyro and other ability particles. Presentation only; hook remains prototype work.",
                        null, false, "ember", "frost", "ender", "spark", "aurora"));

        registerGame("evolution", "Evolution", Material.DRAGON_EGG, (short) 0,
                "Fly to the top of a building as a chicken, then evolve at the top.",
                feature("evolution-effect", "Evolution Effect", Material.DRAGON_EGG,
                        "The existing dragon-egg evolution effect remains selectable for compatibility with the current gameplay hook.",
                        "evolution", false, "dragon-egg"),
                feature("egg-appearance", "Dragon Egg Appearance", Material.DRAGON_EGG,
                        "Prototype alternative dragon-egg appearances and evolving particles. Extra visual hooks are not yet connected.",
                        null, false, "classic", "charged", "void", "nature"));

        registerGame("gladiators", "Gladiators", Material.IRON_SWORD, (short) 0,
                "Ironic rod/bow spam at an iconic final-fight location.",
                feature("combat-messages", "Kill & Death Messages", Material.NAME_TAG,
                        "Prototype custom combat message styles. Message replacement hook is not yet connected.",
                        null, false, "classic", "ironic", "champion", "dramatic", "silent"));

        registerGame("microbattles", "Micro Battles", Material.STAINED_GLASS, (short) 3,
                "Three players fall from the pillar, with the winner being the last to fall.",
                feature("glass-colour", "Starting Glass Colour", Material.STAINED_GLASS,
                        "The existing player-specific theme colour selection. It must only recolour that player's starting glass.",
                        "microbattles", false, "aqua", "ember", "forest", "royal", "sunset", "solar", "frost", "void"),
                feature("border-particles", "Border Particles", Material.FIREWORK,
                        "Prototype matching-colour particles when you are within three blocks of the border.",
                        null, false, "matching", "spark", "soft", "off"));

        registerGame("monstermaze", "Monster Maze", Material.ROTTEN_FLESH, (short) 0,
                "After Repulsor, pilot to the top Safe Pad with three seconds remaining, accompanied by the Safe Pad sound.",
                toggleFeature("mob-set-toggles", "Mob Set Toggles", Material.LEVER,
                        "Placeholder per-set enable/disable controls. The menu stores these toggles, but the live mob-set rotation adapter is not connected here.",
                        "set-1", "set-2", "set-3", "set-4", "set-5", "set-6"),
                feature("custom-pack-beacon", "Custom Pack Beacon Colour", Material.BEACON,
                        "Choose a beacon colour for the Custom Pack. Values are saved separately and do not overwrite standard sets; live colour application is pending.",
                        null, false, "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"),
                feature("custom-pack-safe-pad", "Custom Pack Safe Pad Colour", Material.STAINED_CLAY,
                        "Choose the Safe Pad surface colour for the Custom Pack. Saved independently from standard sets; live colour application is pending.",
                        null, false, "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"));

        registerGame("oneinthequiver", "One In The Quiver", Material.ARROW, (short) 0,
                "An archery bullseye.",
                feature("death-streak", "Death Location Effects", Material.FIREWORK,
                        "Prototype death-location effects that scale with the killer's current streak. No gameplay outcome is changed.",
                        null, false, "subtle", "burst", "bullseye", "grand-finale"));

        registerGame("paintball", "Paintball", Material.FIREBALL, (short) 0,
                "Clutch or Kick: kill eight players with one shotgun shot.",
                feature("pellet-impact", "Pellet Impact Effects", Material.FIREWORK,
                        "Research placeholder: confirm whether pellet impacts can be exposed independently before implementing this cosmetic.",
                        null, false, "paint-splash", "spark", "burst", "classic"),
                feature("weapon-skin", "Weapon Skins", Material.IRON_HOE,
                        "Research placeholder: validate the native 1.8 item/rendering hooks before implementing weapon skins.",
                        null, false, "classic", "chrome", "ember", "frost"));

        registerGame("runner", "Runner", Material.LEATHER_BOOTS, (short) 0,
                "On Euphoria or Mini Euphoria, the winner runs a red W shape, which then breaks away.",
                feature("trail", "Trail Colour", Material.STAINED_CLAY,
                        "The existing disappearing running-trail colour. The default red trail remains in place when no selection is equipped.",
                        "runner", false, "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"));

        registerGame("sheepquest", "Sheep Quest", Material.WOOL, (short) 4,
                "A game-winning capture with 0.1 seconds remaining.",
                feature("capture-effects", "Sheep Capture Effects", Material.FIREWORK,
                        "Prototype capture particles resolved from the player who captures the sheep. Hook not yet connected.",
                        null, false, "pastel", "spark", "rainbow", "golden"));

        registerGame("snake", "Snake", Material.WOOL, (short) 11,
                "The winner rides around in a zigzag pattern while the song plays.",
                feature("skin", "Snake Skin & Ability Colours", Material.WOOL,
                        "Select a snake colour family. With no selection equipped, the existing random/rainbow palette remains unchanged.",
                        "snake", false, "elemental", "neon", "sunset"),
                feature("ability-visuals", "Ability Visuals", Material.FIREWORK,
                        "Prototype distinct visuals for Super, Speed and Reverse. Skin/ability visuals should remain visible to other players.",
                        null, false, "classic", "colour-coded", "neon", "subtle"));

        registerGame("spleef", "Spleef", Material.IRON_SPADE, (short) 0,
                "Axe-spleef a dense group of players in a small area, then leap back up.",
                feature("block-break-effects", "Block Break Effects", Material.FIREWORK,
                        "Prototype particles when a block is broken. Effect must not change block break or hit rules.",
                        null, false, "snow", "shards", "spark", "burst"));

        registerGame("turfwars", "Turf Wars", Material.BOW, (short) 0,
                "A 360 no-scope shot off a mid-bounce.",
                feature("arrow-impact", "Arrow Floor Impact Effects", Material.FIREWORK,
                        "Prototype floor-impact effects when an arrow lands. Must not alter arrow collision or team visibility.",
                        null, false, "dust", "spark", "burst", "team-colour"));
    }

    private void registerGame(String key, String displayName, Material icon, short durability,
                              String winEffect, FeatureDefinition... features) {
        games.put(key, new GameDefinition(key, displayName, icon, durability, winEffect,
                Arrays.asList(features)));
    }

    private FeatureDefinition feature(String key, String displayName, Material icon, String description,
                                      String legacyGameKey, boolean toggleOptions, String... options) {
        return new FeatureDefinition(key, displayName, icon, description, legacyGameKey,
                toggleOptions, Arrays.asList(options));
    }

    private FeatureDefinition toggleFeature(String key, String displayName, Material icon, String description,
                                            String... options) {
        return feature(key, displayName, icon, description, null, true, options);
    }

    /**
     * Legacy single-choice getter used by existing gameplay adapters.
     * Keep this storage path stable while new choices live under players.<uuid>.arcade.
     */
    public static String getEquippedChoice(UUID playerId, String game) {
        if (instance == null || playerId == null || game == null) return "";
        String value = instance.data.getString("players." + playerId.toString() + "." + game.toLowerCase(Locale.ROOT), "");
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /** Read a new per-feature choice without disturbing the legacy adapter API. */
    public static String getEquippedChoice(UUID playerId, String game, String feature) {
        if (instance == null || playerId == null || game == null || feature == null) return "";
        String key = game.toLowerCase(Locale.ROOT);
        String value = instance.data.getString(featurePath(playerId, key, feature), "");
        if (value == null || value.isEmpty()) {
            GameDefinition definition = instance.games.get(key);
            FeatureDefinition featureDefinition = definition == null ? null : definition.getFeature(feature);
            if (featureDefinition != null && featureDefinition.legacyGameKey != null) {
                return getEquippedChoice(playerId, featureDefinition.legacyGameKey);
            }
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String featurePath(UUID playerId, String game, String feature) {
        return "players." + playerId.toString() + ".arcade." + game.toLowerCase(Locale.ROOT)
                + "." + feature.toLowerCase(Locale.ROOT);
    }

    private void setFeatureChoice(UUID playerId, GameDefinition game, FeatureDefinition feature, String choice) {
        String normalised = choice == null ? "" : choice.toLowerCase(Locale.ROOT);
        data.set(featurePath(playerId, game.key, feature.key), normalised.isEmpty() ? null : normalised);
        if (feature.legacyGameKey != null) {
            data.set("players." + playerId.toString() + "." + feature.legacyGameKey, normalised);
        }
        save();
    }

    private boolean isToggleEnabled(UUID playerId, GameDefinition game, FeatureDefinition feature, String option) {
        return data.getBoolean("players." + playerId.toString() + ".arcade." + game.key + ".enabled."
                + feature.key + "." + option, true);
    }

    private void setToggleEnabled(UUID playerId, GameDefinition game, FeatureDefinition feature,
                                  String option, boolean enabled) {
        data.set("players." + playerId.toString() + ".arcade." + game.key + ".enabled."
                + feature.key + "." + option, enabled);
        save();
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save cosmetics.yml: " + ex.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Run /arcadecosmetics in-game to open the Arcade Cosmetics prototype selector.");
            return true;
        }
        openMain((Player) sender);
        return true;
    }

    private void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BLACK + MAIN_TITLE);
        int index = 0;
        for (GameDefinition game : games.values()) {
            put(inventory, GAME_SLOTS[index++], gameIcon(player, game));
        }
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Close", "Close the Arcade Cosmetics menu."));
        player.openInventory(inventory);
    }

    private ItemStack gameIcon(Player player, GameDefinition game) {
        ItemStack item = new ItemStack(game.icon, 1, game.iconDurability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + game.displayName);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + game.features.size() + (game.features.size() == 1 ? " cosmetic category" : " cosmetic categories"));
        lore.add(ChatColor.GRAY + "Includes a planned win-effect placeholder");
        lore.add(ChatColor.GREEN + "Click to open");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openGame(Player player, GameDefinition game) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                ChatColor.BLACK + TITLE_PREFIX + "Game | " + game.key);
        int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34};
        for (int i = 0; i < game.features.size() && i < slots.length; i++) {
            FeatureDefinition feature = game.features.get(i);
            put(inventory, slots[i], featureIcon(player, game, feature));
        }
        put(inventory, WIN_EFFECT_SLOT, winEffectItem(player, game));
        put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back to Games", "Return to the full game list."));
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Close", "Close the Arcade Cosmetics menu."));
        player.openInventory(inventory);
    }

    private ItemStack featureIcon(Player player, GameDefinition game, FeatureDefinition feature) {
        ItemStack item = new ItemStack(feature.icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + feature.displayName);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + feature.description);
        if (feature.toggleOptions) {
            int enabled = 0;
            for (String option : feature.options) {
                if (isToggleEnabled(player.getUniqueId(), game, feature, option)) enabled++;
            }
            lore.add(ChatColor.GRAY + "Enabled toggles: " + enabled + "/" + feature.options.size());
        } else {
            String equipped = getEquippedChoice(player.getUniqueId(), game.key, feature.key);
            lore.add(ChatColor.GRAY + "Equipped: " + (equipped.isEmpty() ? "Default" : pretty(equipped)));
        }
        lore.add(ChatColor.GREEN + "Click to view options");
        lore.add(ChatColor.DARK_GRAY + "Feature: " + feature.key);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack winEffectItem(Player player, GameDefinition game) {
        ItemStack item = new ItemStack(Material.FIREWORK);
        ItemMeta meta = item.getItemMeta();
        boolean marked = "planned".equalsIgnoreCase(data.getString(
                "players." + player.getUniqueId() + ".arcade." + game.key + ".win-effect", ""));
        meta.setDisplayName((marked ? ChatColor.GREEN : ChatColor.LIGHT_PURPLE) + "Planned Win Effect (Placeholder)");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Planned sequence (not live yet):");
        appendWrapped(lore, game.winEffect, ChatColor.GRAY, 34);
        lore.add(ChatColor.DARK_GRAY + "WinEffect: " + game.key);
        lore.add(marked ? ChatColor.GREEN + "Marked for implementation" : ChatColor.YELLOW + "Click to mark this plan");
        lore.add(ChatColor.RED + "This does not execute a win sequence.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openFeature(Player player, GameDefinition game, FeatureDefinition feature) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                ChatColor.BLACK + TITLE_PREFIX + "Feature | " + game.key + " | " + feature.key);
        for (int i = 0; i < feature.options.size() && i < 36; i++) {
            String option = feature.options.get(i);
            put(inventory, 10 + i, featureChoiceItem(player, game, feature, option));
        }
        put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back", "Return to " + game.displayName + "."));
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Reset Feature",
                feature.toggleOptions ? "Enable every placeholder set toggle." : "Remove this choice and restore the default."));
        put(inventory, 53, named(Material.BOOK, ChatColor.AQUA + "Prototype Info",
                feature.toggleOptions
                        ? "These saved toggles are not yet connected to live gameplay."
                        : "This choice is saved per player; not every option has a gameplay hook yet."));
        player.openInventory(inventory);
    }

    private ItemStack featureChoiceItem(Player player, GameDefinition game, FeatureDefinition feature, String option) {
        boolean active = feature.toggleOptions
                ? isToggleEnabled(player.getUniqueId(), game, feature, option)
                : option.equalsIgnoreCase(getEquippedChoice(player.getUniqueId(), game.key, feature.key));
        Material material = optionMaterial(game, feature, option, active);
        short durability = optionDurability(feature, option);
        ItemStack item = new ItemStack(material, 1, durability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((active ? ChatColor.GREEN + (feature.toggleOptions ? "[ON] " : "EQUIPPED ")
                : ChatColor.YELLOW.toString()) + pretty(option));
        List<String> lore = new ArrayList<String>();
        appendWrapped(lore, feature.description, ChatColor.GRAY, 36);
        if (feature.toggleOptions) {
            lore.add(active ? ChatColor.GREEN + "Enabled — click to disable" : ChatColor.RED + "Disabled — click to enable");
            lore.add(ChatColor.DARK_GRAY + "Toggle: " + game.key + ":" + feature.key + ":" + option);
        } else {
            lore.add(active ? ChatColor.GREEN + "Currently equipped" : ChatColor.GRAY + "Click to equip");
            lore.add(ChatColor.DARK_GRAY + "Choice: " + game.key + ":" + feature.key + ":" + option);
        }
        item.setItemMeta(meta);
        meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material optionMaterial(GameDefinition game, FeatureDefinition feature, String option, boolean active) {
        if (feature.toggleOptions) return active ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        if ("baconbrawl".equals(game.key)) {
            if ("crown".equals(option)) return Material.GOLD_HELMET;
            if ("top_hat".equals(option)) return Material.OBSIDIAN;
            if ("chef".equals(option)) return Material.CAKE;
            if ("diamond".equals(option)) return Material.DIAMOND_BLOCK;
        }
        if ("evolution".equals(game.key) && ("dragon-egg".equals(option) || "charged".equals(option) || "void".equals(option))) return Material.DRAGON_EGG;
        if (feature.icon == Material.STAINED_GLASS || feature.icon == Material.STAINED_CLAY || feature.icon == Material.WOOL) {
            if (colourData(option) >= 0 || "aqua".equals(option) || "ember".equals(option) || "forest".equals(option)
                    || "royal".equals(option) || "sunset".equals(option) || "solar".equals(option)
                    || "frost".equals(option) || "void".equals(option)) return feature.icon;
        }
        return feature.icon;
    }

    private short optionDurability(FeatureDefinition feature, String option) {
        int colour = colourData(option);
        if ("microbattles".equals(feature.legacyGameKey)) {
            if ("aqua".equals(option)) colour = 3;
            else if ("ember".equals(option)) colour = 14;
            else if ("forest".equals(option)) colour = 5;
            else if ("royal".equals(option)) colour = 10;
            else if ("sunset".equals(option)) colour = 6;
            else if ("solar".equals(option)) colour = 4;
            else if ("frost".equals(option)) colour = 0;
            else if ("void".equals(option)) colour = 15;
        }
        return (short) (colour < 0 ? 0 : colour);
    }

    private void handlePrototypeClick(Player player, String title, ItemStack clicked, ItemMeta meta) {
        String itemName = ChatColor.stripColor(meta.getDisplayName());
        if (itemName == null) return;

        if (MAIN_TITLE.equals(title)) {
            if ("Close".equalsIgnoreCase(itemName)) {
                player.closeInventory();
                return;
            }
            GameDefinition game = gameFromDisplayName(itemName);
            if (game != null) openGame(player, game);
            return;
        }

        String[] titleParts = title.split("\\s*\\|\\s*");
        if (titleParts.length == 3 && "Game".equals(titleParts[1])) {
            GameDefinition game = games.get(titleParts[2].toLowerCase(Locale.ROOT));
            if (game == null) return;
            if ("Back to Games".equalsIgnoreCase(itemName)) {
                openMain(player);
                return;
            }
            if ("Close".equalsIgnoreCase(itemName)) {
                player.closeInventory();
                return;
            }
            if (itemName.startsWith("Planned Win Effect")) {
                toggleWinEffectPlaceholder(player, game);
                openGame(player, game);
                return;
            }
            for (String line : meta.getLore() == null ? Collections.<String>emptyList() : meta.getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain == null || !plain.startsWith("Feature: ")) continue;
                FeatureDefinition feature = game.getFeature(plain.substring("Feature: ".length()));
                if (feature != null) openFeature(player, game, feature);
                return;
            }
            return;
        }

        if (titleParts.length == 4 && "Feature".equals(titleParts[1])) {
            GameDefinition game = games.get(titleParts[2].toLowerCase(Locale.ROOT));
            if (game == null) return;
            FeatureDefinition feature = game.getFeature(titleParts[3]);
            if (feature == null) return;
            if ("Back".equalsIgnoreCase(itemName)) {
                openGame(player, game);
                return;
            }
            if ("Reset Feature".equalsIgnoreCase(itemName)) {
                if (feature.toggleOptions) {
                    for (String option : feature.options) setToggleEnabled(player.getUniqueId(), game, feature, option, true);
                } else {
                    setFeatureChoice(player.getUniqueId(), game, feature, "");
                }
                openFeature(player, game, feature);
                return;
            }

            String markerPrefix = (feature.toggleOptions ? "Toggle: " : "Choice: ")
                    + game.key + ":" + feature.key + ":";
            for (String line : meta.getLore() == null ? Collections.<String>emptyList() : meta.getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain == null || !plain.startsWith(markerPrefix)) continue;
                String option = plain.substring(markerPrefix.length());
                if (!feature.options.contains(option)) return;
                if (feature.toggleOptions) {
                    boolean enabled = isToggleEnabled(player.getUniqueId(), game, feature, option);
                    setToggleEnabled(player.getUniqueId(), game, feature, option, !enabled);
                    player.sendMessage(ChatColor.GREEN + pretty(option) + " is now " + (!enabled ? "enabled" : "disabled") + ".");
                } else {
                    setFeatureChoice(player.getUniqueId(), game, feature, option);
                    player.sendMessage(ChatColor.GREEN + "Equipped " + pretty(option) + " for " + game.displayName + ".");
                }
                openFeature(player, game, feature);
                return;
            }
        }
    }

    private void toggleWinEffectPlaceholder(Player player, GameDefinition game) {
        String path = "players." + player.getUniqueId() + ".arcade." + game.key + ".win-effect";
        boolean currentlyMarked = "planned".equalsIgnoreCase(data.getString(path, ""));
        data.set(path, currentlyMarked ? null : "planned");
        save();
        player.sendMessage(ChatColor.LIGHT_PURPLE + game.displayName + " win effect "
                + (currentlyMarked ? "unmarked" : "marked") + ". This is a planning placeholder only; no sequence runs.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void injectJukeboxIntoNormalCosmeticsMenu(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !isNormalCosmeticsMenu(event.getView())) return;
        event.getView().getTopInventory().setItem(52, normalMenuJukebox());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null) return;

        if (isNormalCosmeticsMenu(event.getView()) && event.getRawSlot() == 52 && isArcadeJukebox(event.getCurrentItem())) {
            event.setCancelled(true);
            Bukkit.dispatchCommand(player, "arcadecosmetics");
            return;
        }

        if (!title.startsWith("Arcade Cosmetics |")) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;
        handlePrototypeClick(player, title, clicked, clicked.getItemMeta());
    }

    private boolean isNormalCosmeticsMenu(InventoryView view) {
        if (view == null || view.getTopInventory() == null) return false;
        String title = ChatColor.stripColor(view.getTitle());
        return "Cosmetics".equals(title) && view.getTopInventory().getSize() >= 54;
    }

    private ItemStack normalMenuJukebox() {
        ItemStack item = new ItemStack(Material.JUKEBOX);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Arcade Cosmetics");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Game-specific cosmetic prototypes");
        lore.add(ChatColor.GRAY + "and planned win-effect concepts.");
        lore.add(ChatColor.GREEN + "Click to open");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isArcadeJukebox(ItemStack item) {
        if (item == null || item.getType() != Material.JUKEBOX || !item.hasItemMeta()) return false;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return "Arcade Cosmetics".equals(name);
    }

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

        if ("baconbrawl".equalsIgnoreCase(game)) {
            if ("crown".equals(selected)) return 4;
            if ("top_hat".equals(selected)) return 15;
            if ("chef".equals(selected)) return 8;
            if ("diamond".equals(selected)) return 3;
            return -1;
        }

        return colourData(selected);
    }

    /** Resolve the visible Minecraft 1.8 stained-glass/clay colour for a choice. */
    public static int colourData(String choice) {
        if (choice == null) return -1;
        String[] colours = {"white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        for (int i = 0; i < colours.length; i++) {
            if (colours[i].equalsIgnoreCase(choice)) return i;
        }
        return -1;
    }

    private GameDefinition gameFromDisplayName(String label) {
        String clean = label == null ? "" : ChatColor.stripColor(label);
        if (clean == null) return null;
        for (GameDefinition game : games.values()) {
            if (game.displayName.equalsIgnoreCase(clean)) return game;
        }
        return null;
    }

    private ItemStack named(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + loreLine));
        item.setItemMeta(meta);
        return item;
    }

    private void appendWrapped(List<String> lore, String text, ChatColor colour, int maxLength) {
        if (text == null || text.isEmpty()) return;
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > maxLength) {
                lore.add(colour + line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lore.add(colour + line.toString());
    }

    private ItemStack namedWithLore(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void put(Inventory inventory, int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    private String pretty(String choice) {
        if (choice == null || choice.isEmpty()) return "Default";
        StringBuilder result = new StringBuilder();
        String[] tokens = choice.replace('-', '_').split("_");
        for (String token : tokens) {
            if (result.length() > 0) result.append(' ');
            if (token.length() == 0) continue;
            result.append(token.substring(0, 1).toUpperCase(Locale.ROOT)).append(token.substring(1));
        }
        return result.toString();
    }

    private static final class GameDefinition {
        private final String key;
        private final String displayName;
        private final Material icon;
        private final short iconDurability;
        private final String winEffect;
        private final List<FeatureDefinition> features;

        private GameDefinition(String key, String displayName, Material icon, short iconDurability,
                               String winEffect, List<FeatureDefinition> features) {
            this.key = key;
            this.displayName = displayName;
            this.icon = icon;
            this.iconDurability = iconDurability;
            this.winEffect = winEffect;
            this.features = features;
        }

        private FeatureDefinition getFeature(String key) {
            if (key == null) return null;
            for (FeatureDefinition feature : features) {
                if (feature.key.equalsIgnoreCase(key)) return feature;
            }
            return null;
        }
    }

    private static final class FeatureDefinition {
        private final String key;
        private final String displayName;
        private final Material icon;
        private final String description;
        private final String legacyGameKey;
        private final boolean toggleOptions;
        private final List<String> options;

        private FeatureDefinition(String key, String displayName, Material icon, String description,
                                  String legacyGameKey, boolean toggleOptions, List<String> options) {
            this.key = key;
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
            this.legacyGameKey = legacyGameKey;
            this.toggleOptions = toggleOptions;
            this.options = options;
        }
    }
}
