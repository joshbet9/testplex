package nautilus.game.arcade.game.games.monstermaze;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import mineplex.core.customdata.CustomDataManager;

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

        final CustomDataManager customData = game.Manager.getCustomDataManager();
        final Plugin plugin = game.Manager.getPlugin();
        if (customData == null || plugin == null)
            return;

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

    /** Read the current kit's PB for a specific pattern without starting a write. */
    public static int getKitBest(MonsterMaze game, Player player, int pattern)
    {
        if (game == null || player == null || pattern < 1 || pattern > 3
                || game.Manager.getCustomDataManager() == null || game.GetKit(player) == null)
            return 0;
        String prefix = game.GetType().getDisplay().getCustomDataKeyName();
        String kit = game.GetKit(player).GetName();
        return game.Manager.getCustomDataManager().getData(player,
                prefix + "pb.pattern" + pattern + "." + slug(kit));
    }

    /** Show the selected kit's local/native PB for each original maze pattern. */
    public static void show(final MonsterMaze game, final Player player)
    {
        if (game == null || player == null)
            return;

        final CustomDataManager customData = game.Manager.getCustomDataManager();
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
