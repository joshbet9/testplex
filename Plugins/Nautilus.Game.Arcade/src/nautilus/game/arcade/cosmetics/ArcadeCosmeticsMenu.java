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

import mineplex.core.achievement.AchievementCategory;

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
import org.bukkit.event.inventory.ClickType;
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
    private static final String[] COLOUR_OPTIONS = {
            "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
            "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
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
                compositeFeature("finish-presentation", "Finish Presentation", Material.BEACON,
                        "Customise the death chat line and the leader beacon independently within one cosmetic line.",
                        setting("death-chat", "Death Chat Lines", Material.NAME_TAG,
                                "Choose a custom line to send when you die. The chat adapter remains prototype work.",
                                null, false, "off", "am-i-cooked", "grok-is-this-true"),
                        setting("leader-beacon", "Leader Beacon Colour", Material.BEACON,
                                "The shared beacon should follow the leading player and use that player's selected colour. Hook not yet connected.",
                                null, false, "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray",
                                "silver", "cyan", "purple", "blue", "brown", "green", "red", "black")));

        registerGame("dragons", "Dragons", Material.FIREWORK, (short) 0,
                "Use a Pyro platform; the winner is hit by a dragon while everyone else falls.",
                feature("pyro-particles", "Pyro Ability Particles", Material.BLAZE_POWDER,
                        "Choose a visual style for Pyro and other ability particles. Presentation only; hook remains prototype work.",
                        null, false, "ember", "frost", "ender", "spark", "aurora"));

        registerGame("evolution", "Evolution", Material.DRAGON_EGG, (short) 0,
                "Fly to the top of a building as a chicken, then evolve at the top.",
                compositeFeature("evolution-style", "Evolution Appearance & Effects", Material.DRAGON_EGG,
                        "Choose the existing evolution effect and customise the egg appearance or evolving particles independently.",
                        setting("evolution-effect", "Evolution Effect", Material.DRAGON_EGG,
                                "The existing dragon-egg evolution effect remains selectable for compatibility with the current gameplay hook.",
                                "evolution", false, "dragon-egg"),
                        setting("egg-appearance", "Dragon Egg Appearance", Material.DRAGON_EGG,
                                "Prototype alternative dragon-egg appearances and evolving particles. Extra visual hooks are not yet connected.",
                                null, false, "classic", "charged", "void", "nature")));

        registerGame("gladiators", "Gladiators", Material.IRON_SWORD, (short) 0,
                "Ironic rod/bow spam at an iconic final-fight location.",
                feature("combat-messages", "Kill & Death Messages", Material.NAME_TAG,
                        "Prototype custom combat message styles. Message replacement hook is not yet connected.",
                        null, false, "classic", "ironic", "champion", "dramatic", "silent"));

        registerGame("microbattles", "Micro Battles", Material.STAINED_GLASS, (short) 3,
                "Three players fall from the pillar, with the winner being the last to fall.",
                compositeFeature("battle-style", "Glass Colour & Border Effects", Material.STAINED_GLASS,
                        "Customise your starting glass colour and border particles independently within one cosmetic line.",
                        setting("glass-colour", "Starting Glass Colour", Material.STAINED_GLASS,
                                "The existing player-specific theme colour selection. It must only recolour that player's starting glass.",
                                "microbattles", false, "aqua", "ember", "forest", "royal", "sunset", "solar", "frost", "void"),
                        setting("border-particles", "Border Particles", Material.FIREWORK,
                                "Prototype matching-colour particles when you are within three blocks of the border.",
                                null, false, "matching", "spark", "soft", "off")));

        registerGame("monstermaze", "Monster Maze", Material.ROTTEN_FLESH, (short) 0,
                "After Repulsor, pilot to the top Safe Pad with three seconds remaining, accompanied by the Safe Pad sound.",
                compositeFeature("mob-line", "Mob Sets & Custom Colours", Material.LEVER,
                        "Enable or disable each supported mob set for the in-game rotation. Right-click a set to configure that set's beacon and Safe Pad colours after Custom Pack is unlocked.",
                        setting("mob-sets", "Mob Sets", Material.MONSTER_EGG,
                                "Toggle individual mob sets for rotation. Right-click a set to set its beacon and Safe Pad colours when Custom Pack is unlocked.",
                                null, true,
                                "creeper", "skeleton", "spider", "zombie", "slime", "zombie_pigman",
                                "enderman", "cave_spider", "silverfish", "blaze", "magma_cube", "bat",
                                "witch", "endermite", "guardian", "pig", "sheep", "cow", "chicken",
                                "squid", "wolf", "mooshroom", "snowman", "ocelot", "horse", "rabbit", "villager")));

        registerGame("oneinthequiver", "One In The Quiver", Material.ARROW, (short) 0,
                "An archery bullseye.",
                feature("death-streak", "Death Location Effects", Material.FIREWORK,
                        "Prototype death-location effects that scale with the killer's current streak. No gameplay outcome is changed.",
                        null, false, "subtle", "burst", "bullseye", "grand-finale"));

        registerGame("paintball", "Paintball", Material.FIREBALL, (short) 0,
                "Clutch or Kick: kill eight players with one shotgun shot.",
                compositeFeature("paintball-style", "Weapon Skins & Impact Effects", Material.IRON_HOE,
                        "Customise weapon appearance and pellet impact effects independently within one cosmetic line.",
                        setting("pellet-impact", "Pellet Impact Effects", Material.FIREWORK,
                                "Research placeholder: confirm whether pellet impacts can be exposed independently before implementing this cosmetic.",
                                null, false, "paint-splash", "spark", "burst", "classic"),
                        setting("weapon-skin", "Weapon Skins", Material.IRON_HOE,
                                "Research placeholder: validate the native 1.8 item/rendering hooks before implementing weapon skins.",
                                null, false, "classic", "chrome", "ember", "frost")));

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
                compositeFeature("snake-style", "Snake Skin & Ability Effects", Material.WOOL,
                        "Choose a snake colour family and independently customise ability visuals. The default random/rainbow palette remains unchanged when no skin is equipped.",
                        setting("skin", "Snake Skin & Ability Colours", Material.WOOL,
                                "Select a snake colour family. With no selection equipped, the existing random/rainbow palette remains unchanged.",
                                "snake", false, "elemental", "neon", "sunset"),
                        setting("ability-visuals", "Ability Visuals", Material.FIREWORK,
                                "Prototype distinct visuals for Super, Speed and Reverse. Skin/ability visuals should remain visible to other players.",
                                null, false, "classic", "colour-coded", "neon", "subtle")));

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
        AchievementCategory statsCategory = statsCategoryFor(key);
        if (statsCategory != null) {
            icon = statsCategory.getIcon();
            durability = (short) statsCategory.getIconData();
        }
        games.put(key, new GameDefinition(key, displayName, icon, durability, winEffect,
                Arrays.asList(features)));
    }

    /** Use the exact icons and durability data shown by /stats for Arcade games. */
    private AchievementCategory statsCategoryFor(String key) {
        switch (key) {
            case "baconbrawl": return AchievementCategory.BACON_BRAWL;
            case "bomblobbers": return AchievementCategory.BOMB_LOBBERS;
            case "deathtag": return AchievementCategory.DEATH_TAG;
            case "dragonescape": return AchievementCategory.DRAGON_ESCAPE;
            case "dragons": return AchievementCategory.DRAGONS;
            case "evolution": return AchievementCategory.EVOLUTION;
            case "gladiators": return AchievementCategory.GLADIATORS;
            case "microbattles": return AchievementCategory.MICRO_BATTLE;
            case "monstermaze": return AchievementCategory.MONSTER_MAZE;
            case "oneinthequiver": return AchievementCategory.ONE_IN_THE_QUIVER;
            case "paintball": return AchievementCategory.SUPER_PAINTBALL;
            case "runner": return AchievementCategory.RUNNER;
            case "sheepquest": return AchievementCategory.SHEEP_QUEST;
            case "snake": return AchievementCategory.SNAKE;
            case "spleef": return AchievementCategory.SPLEEF;
            case "turfwars": return AchievementCategory.TURF_WARS;
            default: return null;
        }
    }

    private FeatureDefinition feature(String key, String displayName, Material icon, String description,
                                      String legacyGameKey, boolean toggleOptions, String... options) {
        return new FeatureDefinition(key, displayName, icon, description, legacyGameKey,
                toggleOptions, Arrays.asList(options));
    }

    private FeatureDefinition compositeFeature(String key, String displayName, Material icon, String description,
                                               SettingDefinition... settings) {
        return new FeatureDefinition(key, displayName, icon, description, null, false,
                Collections.<String>emptyList(), Arrays.asList(settings));
    }

    private SettingDefinition setting(String key, String displayName, Material icon, String description,
                                      String legacyGameKey, boolean toggleOptions, String... options) {
        return new SettingDefinition(key, displayName, icon, description, legacyGameKey,
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
        if (value != null && !value.isEmpty()) return value.toLowerCase(Locale.ROOT);

        GameDefinition definition = instance.games.get(key);
        if (definition != null) {
            SettingDefinition settingDefinition = definition.getSetting(feature);
            FeatureDefinition parent = definition.getFeatureContainingSetting(feature);
            if (settingDefinition != null && parent != null) {
                return instance.getSettingChoice(playerId, definition, parent, settingDefinition);
            }
            FeatureDefinition featureDefinition = definition.getFeature(feature);
            if (featureDefinition != null && featureDefinition.legacyGameKey != null) {
                return getEquippedChoice(playerId, featureDefinition.legacyGameKey);
            }
        }
        return "";
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

    private String settingChoicePath(UUID playerId, GameDefinition game, FeatureDefinition feature,
                                     SettingDefinition setting) {
        return "players." + playerId.toString() + ".arcade." + game.key
                + ".settings." + feature.key + "." + setting.key;
    }

    private String getSettingChoice(UUID playerId, GameDefinition game, FeatureDefinition feature,
                                    SettingDefinition setting) {
        String value = data.getString(settingChoicePath(playerId, game, feature, setting), "");
        if (value == null || value.isEmpty()) {
            value = data.getString(featurePath(playerId, game.key, setting.key), "");
        }
        if ((value == null || value.isEmpty()) && setting.legacyGameKey != null) {
            value = data.getString("players." + playerId.toString() + "." + setting.legacyGameKey, "");
        }
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void setSettingChoice(UUID playerId, GameDefinition game, FeatureDefinition feature,
                                  SettingDefinition setting, String choice) {
        String normalised = choice == null ? "" : choice.toLowerCase(Locale.ROOT);
        data.set(settingChoicePath(playerId, game, feature, setting), normalised.isEmpty() ? null : normalised);
        data.set(featurePath(playerId, game.key, setting.key), normalised.isEmpty() ? null : normalised);
        if (setting.legacyGameKey != null) {
            data.set("players." + playerId.toString() + "." + setting.legacyGameKey,
                    normalised.isEmpty() ? null : normalised);
        }
        save();
    }

    private String settingTogglePath(UUID playerId, GameDefinition game, FeatureDefinition feature,
                                     SettingDefinition setting, String option) {
        return "players." + playerId.toString() + ".arcade." + game.key
                + ".settings-enabled." + feature.key + "." + setting.key + "." + option;
    }

    private boolean isSettingToggleEnabled(UUID playerId, GameDefinition game, FeatureDefinition feature,
                                           SettingDefinition setting, String option) {
        return data.getBoolean(settingTogglePath(playerId, game, feature, setting, option), true);
    }

    private void setSettingToggleEnabled(UUID playerId, GameDefinition game, FeatureDefinition feature,
                                         SettingDefinition setting, String option, boolean enabled) {
        data.set(settingTogglePath(playerId, game, feature, setting, option), enabled);
        save();
    }

    /** Connect this to the actual Custom Pack entitlement when the live unlock adapter is available. */
    public static void setMonsterMazeCustomPackUnlocked(UUID playerId, boolean unlocked) {
        if (instance == null || playerId == null) return;
        instance.data.set("players." + playerId.toString() + ".arcade.monstermaze.custom-pack-unlocked", unlocked);
        instance.save();
    }

    public static boolean isMonsterMazeCustomPackUnlocked(UUID playerId) {
        return instance != null && playerId != null && instance.data.getBoolean(
                "players." + playerId.toString() + ".arcade.monstermaze.custom-pack-unlocked", false);
    }

    public static boolean isMonsterMazeMobSetEnabled(UUID playerId, String mobSet) {
        if (instance == null || playerId == null || mobSet == null) return false;
        GameDefinition game = instance.games.get("monstermaze");
        FeatureDefinition feature = game == null ? null : game.getFeatureContainingSetting("mob-sets");
        SettingDefinition setting = game == null ? null : game.getSetting("mob-sets");
        if (feature == null || setting == null || !setting.options.contains(mobSet.toLowerCase(Locale.ROOT))) return false;
        return instance.isSettingToggleEnabled(playerId, game, feature, setting, mobSet.toLowerCase(Locale.ROOT));
    }

    public static int getMonsterMazeBeaconColourData(UUID playerId, String mobSet) {
        return instance == null || playerId == null || mobSet == null ? colourData("white")
                : colourData(instance.data.getString("players." + playerId + ".arcade.monstermaze.mob-set-colours."
                        + mobSet.toLowerCase(Locale.ROOT) + ".beacon", "white"));
    }

    public static int getMonsterMazeSafePadColourData(UUID playerId, String mobSet) {
        return instance == null || playerId == null || mobSet == null ? colourData("green")
                : colourData(instance.data.getString("players." + playerId + ".arcade.monstermaze.mob-set-colours."
                        + mobSet.toLowerCase(Locale.ROOT) + ".safe-pad", "green"));
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
        put(inventory, WIN_EFFECT_SLOT, namedWithLore(Material.FIREWORK, ChatColor.LIGHT_PURPLE + "Win Effects",
                Arrays.asList(ChatColor.GRAY + "Review the planned win-effect concept for every Arcade game.",
                        ChatColor.YELLOW + "Click to open the Win Effects section.")));
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Close", "Close the Arcade Cosmetics menu."));
        player.openInventory(inventory);
    }

    private ItemStack gameIcon(Player player, GameDefinition game) {
        ItemStack item = new ItemStack(game.icon, 1, game.iconDurability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + game.displayName);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY.toString() + game.features.size() + (game.features.size() == 1 ? " cosmetic line" : " cosmetic lines"));
        lore.add(ChatColor.GRAY + "Win-effect concept tracked in the Win Effects section");
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
        put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back to Games", "Return to the full game list."));
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Close", "Close the Arcade Cosmetics menu."));
        player.openInventory(inventory);
    }

    private void openWinEffects(Player player) {
        openWinEffects(player, null);
    }

    private void openWinEffects(Player player, GameDefinition selectedGame) {
        String title = TITLE_PREFIX + "Win Effects" + (selectedGame == null ? "" : " | " + selectedGame.key);
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BLACK + title);
        if (selectedGame == null) {
            int index = 0;
            for (GameDefinition game : games.values()) {
                put(inventory, GAME_SLOTS[index++], winEffectItem(player, game));
            }
            put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back to Games", "Return to the Arcade Cosmetics game list."));
        } else {
            put(inventory, 22, winEffectItem(player, selectedGame));
            put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back to Game", "Return to " + selectedGame.displayName + " cosmetics."));
        }
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Close", "Close the Arcade Cosmetics menu."));
        player.openInventory(inventory);
    }

    private ItemStack featureIcon(Player player, GameDefinition game, FeatureDefinition feature) {
        ItemStack item = new ItemStack(feature.icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + feature.displayName);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + feature.description);
        if (feature.isComposite()) {
            lore.add(ChatColor.GRAY + feature.settings.size() + " configurable settings");
        } else if (feature.toggleOptions) {
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
        if (feature.isComposite()) {
            int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34};
            for (int i = 0; i < feature.settings.size() && i < slots.length; i++) {
                SettingDefinition setting = feature.settings.get(i);
                put(inventory, slots[i], settingIcon(player, game, feature, setting));
            }
        } else {
            for (int i = 0; i < feature.options.size() && i < 36; i++) {
                String option = feature.options.get(i);
                put(inventory, 10 + i, featureChoiceItem(player, game, feature, option));
            }
        }
        put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back", "Return to " + game.displayName + "."));
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Reset Feature",
                feature.isComposite() ? "Reset each setting in this cosmetic line."
                        : (feature.toggleOptions ? "Enable every toggle." : "Remove this choice and restore the default.")));
        put(inventory, 53, named(Material.BOOK, ChatColor.AQUA + "Prototype Info",
                feature.isComposite()
                        ? "Each setting is saved independently so you can combine unlocked options."
                        : feature.toggleOptions
                                ? "These saved toggles are not yet connected to live gameplay."
                                : "This choice is saved per player; not every option has a gameplay hook yet."));
        player.openInventory(inventory);
    }

    private ItemStack settingIcon(Player player, GameDefinition game, FeatureDefinition feature, SettingDefinition setting) {
        ItemStack item = new ItemStack(setting.icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + setting.displayName);
        List<String> lore = new ArrayList<String>();
        appendWrapped(lore, setting.description, ChatColor.GRAY, 36);
        if (setting.toggleOptions) {
            int enabled = 0;
            for (String option : setting.options) {
                if (isSettingToggleEnabled(player.getUniqueId(), game, feature, setting, option)) enabled++;
            }
            lore.add(ChatColor.GRAY + "Enabled: " + enabled + "/" + setting.options.size());
        } else {
            String equipped = getSettingChoice(player.getUniqueId(), game, feature, setting);
            lore.add(ChatColor.GRAY + "Current: " + (equipped.isEmpty() ? "Default" : pretty(equipped)));
        }
        lore.add(ChatColor.GREEN + "Click to configure");
        lore.add(ChatColor.DARK_GRAY + "Setting: " + setting.key);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openSetting(Player player, GameDefinition game, FeatureDefinition feature, SettingDefinition setting) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BLACK + TITLE_PREFIX
                + "Setting | " + game.key + " | " + feature.key + " | " + setting.key);
        for (int i = 0; i < setting.options.size() && i < 36; i++) {
            put(inventory, 10 + i, settingChoiceItem(player, game, feature, setting, setting.options.get(i)));
        }
        put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back to Settings", "Return to " + feature.displayName + "."));
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Reset Setting",
                setting.toggleOptions ? "Enable all options again." : "Restore the default choice."));
        put(inventory, 53, named(Material.BOOK, ChatColor.AQUA + "Setting Info",
                setting.toggleOptions ? "Right-click Monster Maze mob sets to customise their colours after unlocking Custom Pack."
                        : "This option is saved independently from the other settings in this cosmetic line."));
        player.openInventory(inventory);
    }

    private ItemStack settingChoiceItem(Player player, GameDefinition game, FeatureDefinition feature,
                                        SettingDefinition setting, String option) {
        boolean active = setting.toggleOptions
                ? isSettingToggleEnabled(player.getUniqueId(), game, feature, setting, option)
                : option.equalsIgnoreCase(getSettingChoice(player.getUniqueId(), game, feature, setting));
        Material material;
        short durability = 0;
        if (setting.toggleOptions) {
            if ("monstermaze".equals(game.key) && "mob-sets".equals(setting.key)) {
                material = Material.MONSTER_EGG;
                durability = (short) mobEggData(option);
            } else {
                material = active ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
            }
        } else {
            material = setting.icon;
            if (setting.icon == Material.STAINED_GLASS || setting.icon == Material.STAINED_CLAY
                    || setting.icon == Material.WOOL || setting.icon == Material.STAINED_GLASS_PANE) {
                int colour = settingOptionDurability(game, setting, option);
                if (colour >= 0) durability = (short) colour;
            }
        }
        ItemStack item = new ItemStack(material, 1, durability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((active ? ChatColor.GREEN + (setting.toggleOptions ? "[ON] " : "EQUIPPED ")
                : ChatColor.YELLOW.toString()) + pretty(option));
        List<String> lore = new ArrayList<String>();
        appendWrapped(lore, setting.description, ChatColor.GRAY, 36);
        if (setting.toggleOptions) {
            lore.add(active ? ChatColor.GREEN + "Enabled — click to disable" : ChatColor.RED + "Disabled — click to enable");
            if ("monstermaze".equals(game.key) && "mob-sets".equals(setting.key)) {
                lore.add(ChatColor.AQUA + "Right-click to customise this set's beacon and Safe Pad colours");
                lore.add(ChatColor.GRAY + (isMonsterMazeCustomPackUnlocked(player.getUniqueId())
                        ? "Custom Pack unlocked"
                        : "Custom Pack colours require the Custom Pack unlock"));
            }
            lore.add(ChatColor.DARK_GRAY + "ToggleSetting: " + game.key + ":" + feature.key + ":" + setting.key + ":" + option);
        } else {
            lore.add(active ? ChatColor.GREEN + "Currently equipped" : ChatColor.GRAY + "Click to equip");
            lore.add(ChatColor.DARK_GRAY + "SettingChoice: " + game.key + ":" + feature.key + ":" + setting.key + ":" + option);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int mobEggData(String mobSet) {
        if (mobSet == null) return 97;
        switch (mobSet.toLowerCase(Locale.ROOT)) {
            case "creeper": return 50;
            case "skeleton": return 51;
            case "spider": return 52;
            case "zombie": return 54;
            case "slime": return 55;
            case "zombie_pigman": return 57;
            case "enderman": return 58;
            case "cave_spider": return 59;
            case "silverfish": return 60;
            case "blaze": return 61;
            case "magma_cube": return 62;
            case "bat": return 65;
            case "witch": return 66;
            case "endermite": return 67;
            case "guardian": return 68;
            case "pig": return 90;
            case "sheep": return 91;
            case "cow": return 92;
            case "chicken": return 93;
            case "squid": return 94;
            case "wolf": return 95;
            case "mooshroom": return 96;
            case "snowman": return 97;
            case "ocelot": return 98;
            case "horse": return 100;
            case "rabbit": return 101;
            case "villager": return 120;
            default: return 97;
        }
    }

    private short settingOptionDurability(GameDefinition game, SettingDefinition setting, String option) {
        if ("microbattles".equals(game.key) && "glass-colour".equals(setting.key)) {
            if ("aqua".equals(option)) return 3;
            if ("ember".equals(option)) return 14;
            if ("forest".equals(option)) return 5;
            if ("royal".equals(option)) return 10;
            if ("sunset".equals(option)) return 6;
            if ("solar".equals(option)) return 4;
            if ("frost".equals(option)) return 0;
            if ("void".equals(option)) return 15;
        }
        int colour = colourData(option);
        return (short) (colour < 0 ? 0 : colour);
    }

    private void resetFeature(Player player, GameDefinition game, FeatureDefinition feature) {
        if (!feature.isComposite()) {
            if (feature.toggleOptions) {
                for (String option : feature.options) setToggleEnabled(player.getUniqueId(), game, feature, option, true);
            } else {
                setFeatureChoice(player.getUniqueId(), game, feature, "");
            }
            return;
        }

        UUID id = player.getUniqueId();
        for (SettingDefinition setting : feature.settings) {
            if (setting.toggleOptions) {
                for (String option : setting.options) {
                    data.set(settingTogglePath(id, game, feature, setting, option), true);
                }
            } else {
                data.set(settingChoicePath(id, game, feature, setting), null);
            }
            data.set(featurePath(id, game.key, setting.key), null);
            if (setting.legacyGameKey != null) data.set("players." + id + "." + setting.legacyGameKey, null);
        }
        save();
    }

    private void openMobSetColours(Player player, String mobSet) {
        if (!isMonsterMazeCustomPackUnlocked(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Unlock the Monster Maze Custom Pack to customise this set's colours.");
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BLACK + TITLE_PREFIX + "Mob Set Colours | " + mobSet);
        put(inventory, 4, named(Material.BEACON, ChatColor.AQUA + "Beacon Colour", "Choose the beacon colour for " + pretty(mobSet) + "."));
        put(inventory, 26, named(Material.STAINED_CLAY, ChatColor.GREEN + "Safe Pad Colour", "Choose the Safe Pad surface colour for " + pretty(mobSet) + "."));
        String base = "players." + player.getUniqueId() + ".arcade.monstermaze.mob-set-colours." + mobSet;
        String beaconSelected = data.getString(base + ".beacon", "white");
        String padSelected = data.getString(base + ".safe-pad", "green");
        for (int i = 0; i < COLOUR_OPTIONS.length; i++) {
            String colour = COLOUR_OPTIONS[i];
            ItemStack beacon = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) i);
            ItemMeta beaconMeta = beacon.getItemMeta();
            beaconMeta.setDisplayName((colour.equalsIgnoreCase(beaconSelected) ? ChatColor.GREEN + "[SELECTED] " : ChatColor.YELLOW.toString()) + pretty(colour));
            beaconMeta.setLore(Arrays.asList(ChatColor.GRAY + "Beacon colour for " + pretty(mobSet),
                    ChatColor.DARK_GRAY + "MobColour: beacon:" + mobSet + ":" + colour));
            beacon.setItemMeta(beaconMeta);
            put(inventory, 10 + i, beacon);

            ItemStack pad = new ItemStack(Material.STAINED_CLAY, 1, (short) i);
            ItemMeta padMeta = pad.getItemMeta();
            padMeta.setDisplayName((colour.equalsIgnoreCase(padSelected) ? ChatColor.GREEN + "[SELECTED] " : ChatColor.YELLOW.toString()) + pretty(colour));
            padMeta.setLore(Arrays.asList(ChatColor.GRAY + "Safe Pad surface colour for " + pretty(mobSet),
                    ChatColor.DARK_GRAY + "MobColour: safe-pad:" + mobSet + ":" + colour));
            pad.setItemMeta(padMeta);
            put(inventory, 28 + i, pad);
        }
        put(inventory, 45, named(Material.ARROW, ChatColor.YELLOW + "Back to Mob Sets", "Return to the Monster Maze mob set toggles."));
        put(inventory, 49, named(Material.BARRIER, ChatColor.RED + "Close", "Close Arcade Cosmetics."));
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

    private void handlePrototypeClick(Player player, String title, ItemStack clicked, ItemMeta meta, ClickType clickType) {
        String itemName = ChatColor.stripColor(meta.getDisplayName());
        if (itemName == null) return;

        if (MAIN_TITLE.equals(title)) {
            if ("Close".equalsIgnoreCase(itemName)) {
                player.closeInventory();
                return;
            }
            if ("Win Effects".equalsIgnoreCase(itemName)) {
                openWinEffects(player);
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
            for (String line : meta.getLore() == null ? Collections.<String>emptyList() : meta.getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain == null || !plain.startsWith("Feature: ")) continue;
                FeatureDefinition feature = game.getFeature(plain.substring("Feature: ".length()));
                if (feature != null) openFeature(player, game, feature);
                return;
            }
            return;
        }

        if (titleParts.length == 2 && "Win Effects".equals(titleParts[1])) {
            if ("Back to Games".equalsIgnoreCase(itemName)) {
                openMain(player);
                return;
            }
            if ("Close".equalsIgnoreCase(itemName)) {
                player.closeInventory();
                return;
            }
            GameDefinition effectGame = gameFromWinEffectMeta(meta);
            if (effectGame != null) {
                toggleWinEffectPlaceholder(player, effectGame);
                openWinEffects(player);
            }
            return;
        }

        if (titleParts.length == 3 && "Win Effects".equals(titleParts[1])) {
            GameDefinition selectedGame = games.get(titleParts[2].toLowerCase(Locale.ROOT));
            if (selectedGame == null) return;
            if ("Back to Game".equalsIgnoreCase(itemName)) {
                openGame(player, selectedGame);
                return;
            }
            if ("Back to Games".equalsIgnoreCase(itemName)) {
                openMain(player);
                return;
            }
            if ("Close".equalsIgnoreCase(itemName)) {
                player.closeInventory();
                return;
            }
            GameDefinition effectGame = gameFromWinEffectMeta(meta);
            if (effectGame != null && effectGame.key.equals(selectedGame.key)) {
                toggleWinEffectPlaceholder(player, effectGame);
                openWinEffects(player, selectedGame);
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
                resetFeature(player, game, feature);
                openFeature(player, game, feature);
                return;
            }

            if (feature.isComposite()) {
                for (String line : meta.getLore() == null ? Collections.<String>emptyList() : meta.getLore()) {
                    String plain = ChatColor.stripColor(line);
                    if (plain == null || !plain.startsWith("Setting: ")) continue;
                    SettingDefinition setting = feature.getSetting(plain.substring("Setting: ".length()));
                    if (setting != null) openSetting(player, game, feature, setting);
                    return;
                }
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

        if (titleParts.length == 5 && "Setting".equals(titleParts[1])) {
            GameDefinition game = games.get(titleParts[2].toLowerCase(Locale.ROOT));
            if (game == null) return;
            FeatureDefinition feature = game.getFeature(titleParts[3]);
            SettingDefinition setting = feature == null ? null : feature.getSetting(titleParts[4]);
            if (feature == null || setting == null) return;

            if ("Back to Settings".equalsIgnoreCase(itemName)) {
                openFeature(player, game, feature);
                return;
            }
            if ("Reset Setting".equalsIgnoreCase(itemName)) {
                if (setting.toggleOptions) {
                    for (String option : setting.options) {
                        setSettingToggleEnabled(player.getUniqueId(), game, feature, setting, option, true);
                    }
                } else {
                    setSettingChoice(player.getUniqueId(), game, feature, setting, "");
                }
                openSetting(player, game, feature, setting);
                return;
            }

            String prefix = (setting.toggleOptions ? "ToggleSetting: " : "SettingChoice: ")
                    + game.key + ":" + feature.key + ":" + setting.key + ":";
            for (String line : meta.getLore() == null ? Collections.<String>emptyList() : meta.getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain == null || !plain.startsWith(prefix)) continue;
                String option = plain.substring(prefix.length());
                if (!setting.options.contains(option)) return;

                if (setting.toggleOptions) {
                    if ("monstermaze".equals(game.key) && "mob-sets".equals(setting.key)
                            && clickType == ClickType.RIGHT) {
                        openMobSetColours(player, option);
                        return;
                    }
                    boolean enabled = isSettingToggleEnabled(player.getUniqueId(), game, feature, setting, option);
                    setSettingToggleEnabled(player.getUniqueId(), game, feature, setting, option, !enabled);
                    player.sendMessage(ChatColor.GREEN + pretty(option) + " is now " + (!enabled ? "enabled" : "disabled") + ".");
                } else {
                    setSettingChoice(player.getUniqueId(), game, feature, setting, option);
                    player.sendMessage(ChatColor.GREEN + "Selected " + pretty(option) + " for " + setting.displayName + ".");
                }
                openSetting(player, game, feature, setting);
                return;
            }
            return;
        }

        if (titleParts.length == 3 && "Mob Set Colours".equals(titleParts[1])) {
            String mobSet = titleParts[2].toLowerCase(Locale.ROOT);
            if (!isMonsterMazeCustomPackUnlocked(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Unlock the Monster Maze Custom Pack to customise this set's colours.");
                openSetting(player, games.get("monstermaze"),
                        games.get("monstermaze").getFeature("mob-line"),
                        games.get("monstermaze").getSetting("mob-sets"));
                return;
            }
            if ("Back to Mob Sets".equalsIgnoreCase(itemName)) {
                GameDefinition game = games.get("monstermaze");
                FeatureDefinition feature = game.getFeature("mob-line");
                openSetting(player, game, feature, feature.getSetting("mob-sets"));
                return;
            }
            if ("Close".equalsIgnoreCase(itemName)) {
                player.closeInventory();
                return;
            }
            for (String line : meta.getLore() == null ? Collections.<String>emptyList() : meta.getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain == null || !plain.startsWith("MobColour: ")) continue;
                String[] parts = plain.substring("MobColour: ".length()).split(":");
                if (parts.length != 3 || !parts[1].equals(mobSet) || colourData(parts[2]) < 0) return;
                String path = "players." + player.getUniqueId() + ".arcade.monstermaze.mob-set-colours."
                        + mobSet + "." + parts[0];
                data.set(path, parts[2]);
                save();
                player.sendMessage(ChatColor.GREEN + pretty(mobSet) + " " + parts[0] + " colour set to " + pretty(parts[2]) + ".");
                openMobSetColours(player, mobSet);
                return;
            }
            return;
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
        handlePrototypeClick(player, title, clicked, clicked.getItemMeta(), event.getClick());
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
        for (int i = 0; i < COLOUR_OPTIONS.length; i++) {
            if (COLOUR_OPTIONS[i].equalsIgnoreCase(choice)) return i;
        }
        return -1;
    }

    private GameDefinition gameFromWinEffectMeta(ItemMeta meta) {
        if (meta == null || meta.getLore() == null) return null;
        for (String line : meta.getLore()) {
            String plain = ChatColor.stripColor(line);
            if (plain != null && plain.startsWith("WinEffect: ")) {
                return games.get(plain.substring("WinEffect: ".length()).toLowerCase(Locale.ROOT));
            }
        }
        return null;
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
        if ("snowman".equalsIgnoreCase(choice)) return "Snow Golem";
        if ("lightblue".equalsIgnoreCase(choice)) return "Light Blue";
        if ("zombie_pigman".equalsIgnoreCase(choice)) return "Zombie Pigman";
        if ("magma_cube".equalsIgnoreCase(choice)) return "Magma Cube";
        if ("cave_spider".equalsIgnoreCase(choice)) return "Cave Spider";
        if ("mooshroom".equalsIgnoreCase(choice)) return "Mooshroom";
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

        private SettingDefinition getSetting(String key) {
            if (key == null) return null;
            for (FeatureDefinition feature : features) {
                SettingDefinition setting = feature.getSetting(key);
                if (setting != null) return setting;
            }
            return null;
        }

        private FeatureDefinition getFeatureContainingSetting(String key) {
            if (key == null) return null;
            for (FeatureDefinition feature : features) {
                if (feature.getSetting(key) != null) return feature;
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
        private final List<SettingDefinition> settings;

        private FeatureDefinition(String key, String displayName, Material icon, String description,
                                  String legacyGameKey, boolean toggleOptions, List<String> options) {
            this(key, displayName, icon, description, legacyGameKey, toggleOptions, options,
                    Collections.<SettingDefinition>emptyList());
        }

        private FeatureDefinition(String key, String displayName, Material icon, String description,
                                  String legacyGameKey, boolean toggleOptions, List<String> options,
                                  List<SettingDefinition> settings) {
            this.key = key;
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
            this.legacyGameKey = legacyGameKey;
            this.toggleOptions = toggleOptions;
            this.options = options;
            this.settings = settings;
        }

        private boolean isComposite() {
            return settings != null && !settings.isEmpty();
        }

        private SettingDefinition getSetting(String key) {
            if (key == null) return null;
            for (SettingDefinition setting : settings) {
                if (setting.key.equalsIgnoreCase(key)) return setting;
            }
            return null;
        }
    }

    private static final class SettingDefinition {
        private final String key;
        private final String displayName;
        private final Material icon;
        private final String description;
        private final String legacyGameKey;
        private final boolean toggleOptions;
        private final List<String> options;

        private SettingDefinition(String key, String displayName, Material icon, String description,
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
