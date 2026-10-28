package mineplex.core.achievement.ui.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import mineplex.core.Managers;
import mineplex.core.account.CoreClientManager;
import mineplex.core.achievement.AchievementCategory;
import mineplex.core.achievement.AchievementManager;
import mineplex.core.achievement.ui.AchievementShop;
import mineplex.core.common.util.C;
import mineplex.core.customdata.CustomDataManager;
import mineplex.core.donation.DonationManager;
import mineplex.core.game.GameDisplay;
import mineplex.core.shop.item.IButton;
import mineplex.core.shop.item.ShopItem;
import mineplex.core.shop.page.ShopPageBase;
import mineplex.core.stats.PlayerStats;
import mineplex.core.stats.StatsManager;

/**
 * Personal Best browser linked from /stats -> Arcade -> Monster Maze.
 * PBs are separated by native mode, original maze pattern and kit.
 */
public class MonsterMazePBPage extends ShopPageBase<AchievementManager, AchievementShop>
{
    private enum View
    {
        MODES,
        PATTERNS,
        KITS
    }

    private final StatsManager _statsManager;
    private final String _targetName;
    private final PlayerStats _targetStats;
    private final View _view;
    private final GameDisplay _display;
    private final int _pattern;

    public MonsterMazePBPage(AchievementManager plugin, StatsManager statsManager, AchievementShop shop,
            CoreClientManager clientManager, DonationManager donationManager, Player player,
            String targetName, PlayerStats targetStats)
    {
        this(plugin, statsManager, shop, clientManager, donationManager, player, targetName, targetStats,
                View.MODES, null, 0);
    }

    private MonsterMazePBPage(AchievementManager plugin, StatsManager statsManager, AchievementShop shop,
            CoreClientManager clientManager, DonationManager donationManager, Player player,
            String targetName, PlayerStats targetStats, View view, GameDisplay display, int pattern)
    {
        super(plugin, shop, clientManager, donationManager, pageTitle(view, display, pattern), player);
        _statsManager = statsManager;
        _targetName = targetName;
        _targetStats = targetStats;
        _view = view;
        _display = display;
        _pattern = pattern;
        buildPage();
    }

    @Override
    protected void buildPage()
    {
        if (_view == View.MODES)
            buildModes();
        else if (_view == View.PATTERNS)
            buildPatterns();
        else
            buildKits();
        addBackButton();
    }

    private void buildModes()
    {
        addButton(20, modeItem(GameDisplay.MonsterMaze, Material.ROTTEN_FLESH),
                new IButton()
                {
                    @Override
                    public void onClick(Player player, ClickType clickType)
                    {
                        openPatterns(player, GameDisplay.MonsterMaze);
                    }
                });
        addButton(22, modeItem(GameDisplay.MonsterMazeSpeed, Material.SUGAR),
                new IButton()
                {
                    @Override
                    public void onClick(Player player, ClickType clickType)
                    {
                        openPatterns(player, GameDisplay.MonsterMazeSpeed);
                    }
                });
        addButton(24, modeItem(GameDisplay.MonsterMazeModern, Material.BEACON),
                new IButton()
                {
                    @Override
                    public void onClick(Player player, ClickType clickType)
                    {
                        openPatterns(player, GameDisplay.MonsterMazeModern);
                    }
                });
        addButton(31, new ShopItem(Material.PAPER, C.cGray + "How Monster Maze PBs work",
                new String[] {
                        ChatColor.GRAY + "PBs are personal to your account.",
                        ChatColor.GRAY + "Each mode, pattern and kit is saved",
                        ChatColor.GRAY + "separately.",
                        ChatColor.YELLOW + "Click for details."
                }, 1, false), new IButton()
        {
            @Override
            public void onClick(Player player, ClickType clickType)
            {
                player.sendMessage(ChatColor.AQUA + "Monster Maze PBs: "
                        + ChatColor.WHITE + "Original, Speed and Modern each store personal bests "
                        + "by maze pattern and kit. Play a run to set or improve a PB.");
                player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
            }
        });
    }

    private ShopItem modeItem(GameDisplay display, Material icon)
    {
        List<String> lore = new ArrayList<String>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Select this mode's personal bests.");
        lore.add(ChatColor.YELLOW + "Click to choose a maze pattern.");
        return new ShopItem(icon, (byte) 0, C.Bold + display.getName(), lore.toArray(new String[0]), 1, false, false);
    }

    private void buildPatterns()
    {
        String[] kits = kitNames(_display);
        for (int pattern = 1; pattern <= 3; pattern++)
        {
            final int selectedPattern = pattern;
            int best = getBestAcrossKits(_display, pattern, kits);
            String pb = best > 0 ? "Stage " + best : "Not set";
            List<String> lore = Arrays.asList(" ",
                    ChatColor.GRAY + "Best across kits: " + ChatColor.WHITE + pb,
                    ChatColor.YELLOW + "Click to view each kit's PB.");
            addButton(19 + (pattern - 1) * 2,
                    new ShopItem(Material.MAP, (byte) 0,
                            C.cAqua + "Maze Pattern " + pattern, lore.toArray(new String[0]), 1, false, false),
                    new IButton()
                    {
                        @Override
                        public void onClick(Player player, ClickType clickType)
                        {
                            openKits(player, _display, selectedPattern);
                        }
                    });
        }
    }

    private void buildKits()
    {
        String[] kits = kitNames(_display);
        int[] slots = kits.length == 4 ? new int[] {20, 22, 24, 31} : new int[] {19, 21, 23, 25, 31};
        for (int i = 0; i < kits.length; i++)
        {
            int best = getKitBest(_display, _pattern, kits[i]);
            String pb = best > 0 ? "Stage " + best : "Not set";
            final String selectedKit = kits[i];
            final String selectedPB = pb;
            addButton(slots[i], new ShopItem(Material.BOOK_AND_QUILL, (byte) 0,
                    C.cAqua + selectedKit,
                    new String[] {" ", ChatColor.GRAY + "Maze Pattern " + _pattern,
                            ChatColor.YELLOW + "Personal Best: " + ChatColor.WHITE + selectedPB,
                            ChatColor.GRAY + "Click to show this PB."},
                    1, false, false), new IButton()
            {
                @Override
                public void onClick(Player player, ClickType clickType)
                {
                    player.sendMessage(ChatColor.AQUA + _display.getName() + " PB — "
                            + ChatColor.WHITE + "Pattern " + _pattern + ", " + selectedKit + ": "
                            + ChatColor.YELLOW + selectedPB);
                    player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
                }
            });
        }
        addButton(40, new ShopItem(Material.PAPER, C.cGray + "Back to Pattern Selection",
                new String[] {ChatColor.WHITE + _display.getName(),
                        ChatColor.GRAY + "Maze Pattern " + _pattern,
                        ChatColor.YELLOW + "Click to choose another pattern."}, 1, false),
                new IButton()
                {
                    @Override
                    public void onClick(Player player, ClickType clickType)
                    {
                        openPatterns(player, _display);
                    }
                });
    }

    private int getBestAcrossKits(GameDisplay display, int pattern, String[] kits)
    {
        int best = 0;
        for (String kit : kits)
            best = Math.max(best, getKitBest(display, pattern, kit));
        return best;
    }

    private int getKitBest(GameDisplay display, int pattern, String kit)
    {
        if (!_targetName.equalsIgnoreCase(getPlayer().getName()))
            return 0;
        CustomDataManager customData = Managers.get(CustomDataManager.class);
        if (customData == null)
            return 0;
        String key = display.getCustomDataKeyName() + "pb.pattern" + pattern + "." + slug(kit);
        return Math.max(0, customData.getData(getPlayer(), key));
    }

    private String[] kitNames(GameDisplay display)
    {
        if (display == GameDisplay.MonsterMaze)
            return new String[] {"Jumper", "Slowballer", "Body Builder", "Repulsor"};
        return new String[] {"Jumper", "Slowballer", "Body Builder", "Repulsor", "Maverick"};
    }

    private String slug(String value)
    {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.+|\\.+$", "");
    }

    private void openPatterns(Player player, GameDisplay display)
    {
        try
        {
            MonsterMazePBPage next = new MonsterMazePBPage(getPlugin(), _statsManager, getShop(),
                    getClientManager(), getDonationManager(), player, _targetName, _targetStats,
                    View.PATTERNS, display, 0);
            getShop().openPageForPlayer(player, next);
            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
        }
        catch (RuntimeException ex)
        {
            org.bukkit.Bukkit.getLogger().warning("[MonsterMaze PB] Could not open pattern list: " + ex.getMessage());
            player.sendMessage(org.bukkit.ChatColor.RED + "Monster Maze PBs could not be opened. Check the server log.");
        }
    }

    private void openKits(Player player, GameDisplay display, int pattern)
    {
        try
        {
            MonsterMazePBPage next = new MonsterMazePBPage(getPlugin(), _statsManager, getShop(),
                    getClientManager(), getDonationManager(), player, _targetName, _targetStats,
                    View.KITS, display, pattern);
            getShop().openPageForPlayer(player, next);
            player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
        }
        catch (RuntimeException ex)
        {
            org.bukkit.Bukkit.getLogger().warning("[MonsterMaze PB] Could not open kit PB list: " + ex.getMessage());
            player.sendMessage(org.bukkit.ChatColor.RED + "Per-kit Monster Maze PBs could not be opened. Check the server log.");
        }
    }

    /**
     * ShopBase identifies pages by inventory title during InventoryCloseEvent. If all three
     * levels share one title, opening the next level makes the close event mistake the new
     * page for the old one and removes the shop registration; subsequent Back and item clicks
     * then appear dead. Use a distinct short title for each navigation level.
     */
    private static String pageTitle(View view, GameDisplay display, int pattern)
    {
        if (view == View.MODES)
            return "MM PB | Modes";
        String mode = display == GameDisplay.MonsterMaze ? "Original"
                : display == GameDisplay.MonsterMazeSpeed ? "Speed" : "Modern";
        return view == View.PATTERNS ? "MM PB | " + mode : "MM PB | " + mode + " P" + pattern;
    }

    private void addBackButton()
    {
        addButton(4, new ShopItem(Material.BED, C.cGray + " \u21FD Go Back", new String[0], 1, false),
                new IButton()
                {
                    @Override
                    public void onClick(Player player, ClickType clickType)
                    {
                        if (_view == View.KITS)
                        {
                            openPatterns(player, _display);
                        }
                        else if (_view == View.PATTERNS)
                        {
                            getShop().openPageForPlayer(player, new MonsterMazePBPage(getPlugin(), _statsManager,
                                    getShop(), getClientManager(), getDonationManager(), player, _targetName,
                                    _targetStats));
                        }
                        else
                        {
                            getShop().openPageForPlayer(player, new AchievementPage(getPlugin(), _statsManager,
                                    AchievementCategory.MONSTER_MAZE, getShop(), getClientManager(),
                                    getDonationManager(), player, _targetName, _targetStats));
                        }
                        player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
                    }
                });
    }
}
