package nautilus.game.arcade.game.games.monstermaze;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import mineplex.core.Managers;
import mineplex.core.customdata.CustomDataManager;
import mineplex.core.game.GameDisplay;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Personal-best persistence for Monster Maze, using Mineplex's existing account
 * custom-data system rather than an external competitive API.
 *
 * Keys are separated by native GameDisplay, maze pattern, and kit, so Original,
 * Speed, and Modern records cannot overwrite each other.
 */
public final class MonsterMazePersonalBestManager
{
    private static final Object LOCK = new Object();
    private static final Set<String> REGISTERED_KEYS = new HashSet<String>();
    private static final Set<String> IN_FLIGHT = new HashSet<String>();
    private static final Map<String, Integer> PENDING_BESTS = new HashMap<String, Integer>();

    private MonsterMazePersonalBestManager()
    {
    }

    public static void record(final MonsterMaze game, final Player player, final int stage, final int pattern)
    {
        if (game == null || player == null || stage < 1 || pattern < 1 || pattern > 3)
            return;

        final Plugin plugin = game.Manager.getPlugin();
        final CustomDataManager customData = getCustomDataManager(game);
        if (customData == null || plugin == null)
        {
            if (plugin != null)
                plugin.getLogger().warning("[MonsterMaze PB] Cannot record a result because CustomDataManager is unavailable.");
            return;
        }

        String displayPrefix = game.GetType().getDisplay().getCustomDataKeyName();
        String kitName = game.GetKit(player) == null ? "unknown" : game.GetKit(player).GetName();
        final String key = displayPrefix + "pb.pattern" + pattern + "." + slug(kitName);
        final int savedBest = customData.getData(player, key);
        if (stage <= savedBest)
            return;

        final String pendingId = player.getUniqueId().toString() + "|" + key;
        synchronized (LOCK)
        {
            Integer pending = PENDING_BESTS.get(pendingId);
            if (stage <= Math.max(savedBest, pending == null ? -1 : pending.intValue()))
                return;

            PENDING_BESTS.put(pendingId, Integer.valueOf(stage));
            if (!IN_FLIGHT.add(pendingId))
                return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    synchronized (LOCK)
                    {
                        if (!REGISTERED_KEYS.contains(key))
                        {
                            customData.getRepository().registerKey(key);
                            REGISTERED_KEYS.add(key);
                        }
                    }
                }
                catch (Exception ex)
                {
                    plugin.getLogger().warning("Could not register Monster Maze PB key '" + key + "': " + ex.getMessage());
                    synchronized (LOCK)
                    {
                        PENDING_BESTS.remove(pendingId);
                        IN_FLIGHT.remove(pendingId);
                    }
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Integer pendingStage;
                        synchronized (LOCK)
                        {
                            pendingStage = PENDING_BESTS.remove(pendingId);
                            IN_FLIGHT.remove(pendingId);
                        }

                        if (pendingStage == null)
                            return;

                        int bestToSave = pendingStage.intValue();
                        int currentBest = customData.getData(player, key);
                        if (bestToSave <= currentBest)
                            return;

                        customData.setData(player, key, bestToSave);
                        plugin.getLogger().info("[MonsterMaze PB] Recorded " + player.getName()
                                + " — " + game.GetType().getDisplay().getName() + ", pattern " + pattern
                                + ", " + kitName(game, player) + ": stage " + bestToSave + ".");

                        if (player.isOnline())
                        {
                            player.sendMessage(ChatColor.GREEN + "New Monster Maze personal best: Stage "
                                    + bestToSave + ChatColor.GRAY + " (Maze " + pattern + ", " + kitName(game, player) + ").");
                        }
                    }
                });
            }
        });
    }

    private static CustomDataManager getCustomDataManager(MonsterMaze game)
    {
        if (game == null)
            return null;
        CustomDataManager manager = game.Manager == null ? null : game.Manager.getCustomDataManager();
        return manager != null ? manager : Managers.get(CustomDataManager.class);
    }

    /**
     * A player with no recorded PB in any native mode/pattern/kit has not yet made
     * measurable progress on this Testplex Monster Maze installation; show tutorial cues.
     */
    public static boolean hasAnyPersonalBest(MonsterMaze game, Player player)
    {
        if (game == null || player == null)
            return false;
        CustomDataManager customData = getCustomDataManager(game);
        if (customData == null)
            return false;

        GameDisplay[] displays = new GameDisplay[] {
                GameDisplay.MonsterMaze, GameDisplay.MonsterMazeSpeed, GameDisplay.MonsterMazeModern};
        String[] kits = new String[] {"Jumper", "Slowballer", "Body Builder", "Repulsor", "Maverick"};
        for (GameDisplay display : displays)
        {
            String prefix = display.getCustomDataKeyName();
            for (int pattern = 1; pattern <= 3; pattern++)
            {
                for (String kit : kits)
                {
                    int stage = customData.getData(player,
                            prefix + "pb.pattern" + pattern + "." + slug(kit));
                    if (stage > 0)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Best stored result for this pattern regardless of the kit used, matching the
     * original Monster Maze scoreboard's stage-plus-kit PB display.
     */
    public static String getBestForPattern(MonsterMaze game, Player player, int pattern)
    {
        if (game == null || player == null || pattern < 1 || pattern > 3
                || getCustomDataManager(game) == null)
            return "Not set";

        String prefix = game.GetType().getDisplay().getCustomDataKeyName();
        String[] kits = game.isModernVariant()
                ? new String[] {"Jumper", "Slowballer", "Body Builder", "Repulsor", "Maverick"}
                : new String[] {"Jumper", "Slowballer", "Body Builder", "Repulsor"};
        int bestStage = 0;
        String bestKit = null;
        for (String kit : kits)
        {
            String key = prefix + "pb.pattern" + pattern + "." + slug(kit);
            int stage = getCustomDataManager(game).getData(player, key);
            if (stage > bestStage)
            {
                bestStage = stage;
                bestKit = kit;
            }
        }
        return bestStage > 0 ? "Stage " + bestStage + " (" + bestKit + ")" : "Not set";
    }

    /** Read the current kit's PB for a specific pattern without starting a write. */
    public static int getKitBest(MonsterMaze game, Player player, int pattern)
    {
        if (game == null || player == null || pattern < 1 || pattern > 3
                || getCustomDataManager(game) == null || game.GetKit(player) == null)
            return 0;
        String prefix = game.GetType().getDisplay().getCustomDataKeyName();
        String kit = game.GetKit(player).GetName();
        return getCustomDataManager(game).getData(player,
                prefix + "pb.pattern" + pattern + "." + slug(kit));
    }

    /** Show the selected kit's local/native PB for each original maze pattern. */
    public static void show(final MonsterMaze game, final Player player)
    {
        if (game == null || player == null)
            return;

        final CustomDataManager customData = getCustomDataManager(game);
        if (customData == null)
        {
            player.sendMessage(ChatColor.RED + "Monster Maze personal-best storage is unavailable on this server.");
            return;
        }

        String kitName = game.GetKit(player) == null ? "unknown" : game.GetKit(player).GetName();
        if ("unknown".equals(kitName))
        {
            player.sendMessage(ChatColor.YELLOW + "Select a Monster Maze kit first to view its personal bests.");
            return;
        }

        String prefix = game.GetType().getDisplay().getCustomDataKeyName();
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + game.GetType().getDisplay().getName()
                + ChatColor.GRAY + " PBs for " + ChatColor.AQUA + kitName + ChatColor.GRAY + ":");
        for (int pattern = 1; pattern <= 3; pattern++)
        {
            String key = prefix + "pb.pattern" + pattern + "." + slug(kitName);
            int best = customData.getData(player, key);
            String value = best < 1 ? ChatColor.DARK_GRAY + "Not set" : ChatColor.GREEN + "Stage " + best;
            player.sendMessage(ChatColor.GRAY + "Maze " + pattern + ": " + value);
        }
    }

    private static String kitName(MonsterMaze game, Player player)
    {
        return game.GetKit(player) == null ? "Unknown" : game.GetKit(player).GetName();
    }

    private static String slug(String value)
    {
        if (value == null)
            return "unknown";

        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".");
        while (slug.startsWith("."))
            slug = slug.substring(1);
        while (slug.endsWith("."))
            slug = slug.substring(0, slug.length() - 1);
        return slug.isEmpty() ? "unknown" : slug;
    }
}
