package nautilus.game.arcade.cosmetics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;

import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.types.Gadget;
import mineplex.core.gadget.types.GadgetType;
import mineplex.core.gadget.types.WinEffectGadget;
import mineplex.core.disguise.disguises.DisguisePlayer;

import nautilus.game.arcade.Arcade;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.game.Game;
import nautilus.game.arcade.game.Game.GameState;

/**
 * Testbed-only lobby tools for replaying native Arcade win effects and summoning
 * visual target dummies. It never creates a real Game or claims the dummies are
 * Bukkit Players; player-only game hooks still require a real second client.
 */
public final class ArcadeTestHarness implements CommandExecutor, Listener
{
    private static final String MAIN_TITLE = ChatColor.BLACK + "Arcade Test Bench";
    private static final String WIN_TITLE = ChatColor.BLACK + "Arcade Test Bench | Win Effects";
    private static final String BOT_TITLE = ChatColor.BLACK + "Arcade Test Bench | Game Bots";
    private static final String WORLD_NAME = "arcadetest";
    private static final int MAX_BOTS_PER_OWNER = 8;
    private static final int MAX_BOTS_TOTAL = 32;
    private static final int ARENA_RADIUS = 32;
    private static final int ARENA_CLEAN_HEIGHT = 30;

    private static final GameTarget[] GAMES = {
        new GameTarget("baconbrawl", "Bacon Brawl", Material.PORK, (short) 0),
        new GameTarget("bomblobbers", "Bomb Lobbers", Material.TNT, (short) 0),
        new GameTarget("deathtag", "Death Tag", Material.IRON_SWORD, (short) 0),
        new GameTarget("dragonescape", "Dragon Escape", Material.FEATHER, (short) 0),
        new GameTarget("dragons", "Dragons", Material.BLAZE_ROD, (short) 0),
        new GameTarget("evolution", "Evolution", Material.EGG, (short) 0),
        new GameTarget("gladiators", "Gladiators", Material.IRON_SWORD, (short) 0),
        new GameTarget("microbattles", "Micro Battles", Material.STAINED_GLASS, (short) 3),
        new GameTarget("monstermaze", "Monster Maze", Material.ROTTEN_FLESH, (short) 0),
        new GameTarget("oneinthequiver", "One In The Quiver", Material.BOW, (short) 0),
        new GameTarget("paintball", "Paintball", Material.SNOW_BALL, (short) 0),
        new GameTarget("runner", "Runner", Material.STAINED_CLAY, (short) 14),
        new GameTarget("sheepquest", "Sheep Quest", Material.WOOL, (short) 4),
        new GameTarget("snake", "Snake", Material.SLIME_BALL, (short) 0),
        new GameTarget("spleef", "Spleef", Material.IRON_SPADE, (short) 0),
        new GameTarget("turfwars", "Turf Wars", Material.BOW, (short) 0)
    };

    private final Arcade plugin;
    private final ArcadeManager arcadeManager;
    private final GadgetManager gadgetManager;
    private final List<BotActor> bots = new ArrayList<BotActor>();
    private BukkitTask botMovementTask;
    private BukkitTask winMonitorTask;
    private WinEffectGadget activeEffect;
    private Player activeTester;
    private PlayerSnapshot activeSnapshot;
    private boolean shuttingDown;

    public ArcadeTestHarness(Arcade plugin, ArcadeManager arcadeManager, GadgetManager gadgetManager)
    {
        this.plugin = plugin;
        this.arcadeManager = arcadeManager;
        this.gadgetManager = gadgetManager;

        if (plugin.getCommand("arcadetest") != null)
        {
            plugin.getCommand("arcadetest").setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startBotMovementTask();
        plugin.getLogger().info("Registered the sandbox-only /arcadetest lobby test bench.");
    }

    private boolean isAuthorised(CommandSender sender)
    {
        return arcadeManager != null && arcadeManager.isTestplexSandbox() && sender.isOp();
    }

    private boolean isLobbySafe()
    {
        Game game = arcadeManager == null ? null : arcadeManager.GetGame();
        return game == null || game.inLobby();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!isAuthorised(sender))
        {
            sender.sendMessage(ChatColor.RED + "Arcade Test Bench is available only to operators on the isolated Testplex sandbox.");
            return true;
        }
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0]) || "help".equalsIgnoreCase(args[0]))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage("Use /arcadetest win list, /arcadetest win <effect>, /arcadetest bot <game> [count] [moving], /arcadetest clear or /arcadetest stop.");
                return true;
            }
            openMain((Player) sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if ("win".equals(action))
        {
            if (args.length == 1 || (args.length == 2 && "list".equalsIgnoreCase(args[1])))
            {
                listWinEffects(sender);
                return true;
            }
            if (!(sender instanceof Player))
            {
                sender.sendMessage("Win-effect tests must be started by an in-game operator.");
                return true;
            }
            startWinEffect((Player) sender, join(args, 1));
            return true;
        }
        if ("bot".equals(action))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage("Bots must be summoned by an in-game operator.");
                return true;
            }
            if (args.length < 2)
            {
                openBots((Player) sender);
                return true;
            }
            int count = 3;
            boolean moving = false;
            if (args.length >= 3)
            {
                if (isCount(args[2])) count = parseCount(args[2]);
                else moving = "moving".equalsIgnoreCase(args[2]);
            }
            if (args.length >= 4) moving = "moving".equalsIgnoreCase(args[3]);
            spawnBots((Player) sender, args[1], count, moving);
            return true;
        }
        if ("clear".equals(action))
        {
            if (sender instanceof Player)
            {
                clearBots(((Player) sender).getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Removed your summoned test bots.");
            }
            else
            {
                clearAllBots();
                sender.sendMessage("Removed all summoned test bots.");
            }
            return true;
        }
        if ("stop".equals(action))
        {
            if (activeTester == null)
            {
                sender.sendMessage(ChatColor.YELLOW + "No win-effect test is currently running.");
            }
            else if (sender instanceof Player && !activeTester.getUniqueId().equals(((Player) sender).getUniqueId()))
            {
                sender.sendMessage(ChatColor.RED + "Another operator is running the shared win-effect tester.");
            }
            else
            {
                finishWinTest(true);
            }
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadetest, /arcadetest win list, /arcadetest win <effect name>, /arcadetest bot <game> [1-8] [moving], /arcadetest clear, /arcadetest stop");
        return true;
    }

    private void openMain(Player player)
    {
        Inventory inventory = Bukkit.createInventory(null, 54, MAIN_TITLE);
        put(inventory, 20, item(Material.FIREWORK, ChatColor.GOLD + "Win Effect Tester",
                "Replay a registered Arcade win effect in an isolated miniature test world.",
                "The command is started from this lobby; other players stay here.",
                "BenchAction: win"));
        put(inventory, 24, item(Material.IRON_SWORD, ChatColor.AQUA + "Game Bot Spawner",
                "Summon temporary, player-skinned target dummies near you.",
                "Choose a game to give them its representative item.",
                "Bots are not actual players and cannot trigger every Player-only hook.",
                "BenchAction: bots"));
        put(inventory, 40, item(Material.BARRIER, ChatColor.RED + "Clear My Bots",
                "Remove all of your summoned dummies.", "BenchAction: clear"));
        put(inventory, 49, item(Material.BARRIER, ChatColor.DARK_RED + "Close", "Close this menu.", "BenchAction: close"));
        player.openInventory(inventory);
    }

    private void openWins(Player player)
    {
        Inventory inventory = Bukkit.createInventory(null, 54, WIN_TITLE);
        List<WinEffectGadget> effects = getTestEffects();
        int slot = 10;
        for (WinEffectGadget effect : effects)
        {
            if (slot > 43) break;
            put(inventory, slot++, item(Material.FIREWORK,
                    ChatColor.YELLOW + effect.getName(),
                    "Replay this actual registered win-effect implementation.",
                    "Duration: about " + Math.max(1, effect.getLength() / 1000L) + " seconds.",
                    "TestWin: " + effect.getName()));
        }
        if (effects.isEmpty())
        {
            put(inventory, 22, item(Material.BARRIER, ChatColor.RED + "No Arcade win effects found",
                    "The registered Arcade WIN_EFFECT gadgets could not be found."));
        }
        put(inventory, 45, item(Material.ARROW, ChatColor.YELLOW + "Back", "BenchAction: back"));
        put(inventory, 49, item(Material.BARRIER, ChatColor.RED + "Close", "BenchAction: close"));
        player.openInventory(inventory);
    }

    private void openBots(Player player)
    {
        Inventory inventory = Bukkit.createInventory(null, 54, BOT_TITLE);
        int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};
        for (int i = 0; i < GAMES.length; i++)
        {
            GameTarget game = GAMES[i];
            put(inventory, slots[i], item(game.item, ChatColor.YELLOW + game.name,
                    "Summon 3 " + game.name + " test dummies near you.",
                    "Command option: /arcadetest bot " + game.key + " 3 moving",
                    "Bots are visual target dummies, not real players.",
                    "TestBot: " + game.key));
        }
        put(inventory, 45, item(Material.ARROW, ChatColor.YELLOW + "Back", "BenchAction: back"));
        put(inventory, 49, item(Material.BARRIER, ChatColor.RED + "Close", "BenchAction: close"));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null || !title.startsWith("Arcade Test Bench")) return;

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()
                || clicked.getItemMeta().getLore() == null) return;

        Player player = (Player) event.getWhoClicked();
        String marker = null;
        for (String line : clicked.getItemMeta().getLore())
        {
            String plain = ChatColor.stripColor(line);
            if (plain == null) continue;
            if (plain.startsWith("BenchAction: ") || plain.startsWith("TestWin: ") || plain.startsWith("TestBot: "))
            {
                marker = plain;
                break;
            }
        }
        if (marker == null) return;

        if (marker.startsWith("BenchAction: "))
        {
            String action = marker.substring("BenchAction: ".length());
            if ("win".equals(action)) openWins(player);
            else if ("bots".equals(action)) openBots(player);
            else if ("clear".equals(action))
            {
                clearBots(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Removed your summoned test bots.");
                openMain(player);
            }
            else if ("back".equals(action)) openMain(player);
            else if ("close".equals(action)) player.closeInventory();
            return;
        }
        if (marker.startsWith("TestWin: "))
        {
            player.closeInventory();
            startWinEffect(player, marker.substring("TestWin: ".length()));
            return;
        }
        if (marker.startsWith("TestBot: "))
        {
            player.closeInventory();
            spawnBots(player, marker.substring("TestBot: ".length()), 3, false);
        }
    }

    private List<WinEffectGadget> getTestEffects()
    {
        List<WinEffectGadget> effects = new ArrayList<WinEffectGadget>();
        for (Gadget gadget : gadgetManager.getGadgets(GadgetType.WIN_EFFECT))
        {
            if (gadget instanceof WinEffectGadget && gadget.getName() != null
                    && gadget.getName().startsWith("Arcade "))
            {
                effects.add((WinEffectGadget) gadget);
            }
        }
        Collections.sort(effects, new Comparator<WinEffectGadget>()
        {
            @Override
            public int compare(WinEffectGadget a, WinEffectGadget b)
            {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return effects;
    }

    private void listWinEffects(CommandSender sender)
    {
        List<WinEffectGadget> effects = getTestEffects();
        sender.sendMessage(ChatColor.GOLD + "Registered Arcade win effects (" + effects.size() + "):");
        for (WinEffectGadget effect : effects)
        {
            sender.sendMessage(ChatColor.YELLOW + "- " + effect.getName());
        }
        sender.sendMessage(ChatColor.GRAY + "Run /arcadetest win <effect name> to test one from the lobby.");
    }

    private void startWinEffect(Player player, String requestedName)
    {
        if (!isLobbySafe())
        {
            player.sendMessage(ChatColor.RED + "Win-effect tests can only start from the lobby, not during an active round.");
            return;
        }
        if (activeEffect != null)
        {
            player.sendMessage(ChatColor.RED + "The shared win-effect test arena is already in use.");
            return;
        }

        WinEffectGadget selected = null;
        for (WinEffectGadget effect : getTestEffects())
        {
            if (effect.getName().equalsIgnoreCase(requestedName)
                    || normalise(effect.getName()).equals(normalise(requestedName)))
            {
                selected = effect;
                break;
            }
        }
        if (selected == null)
        {
            player.sendMessage(ChatColor.RED + "No registered Arcade win effect matches '" + requestedName + "'.");
            player.sendMessage(ChatColor.GRAY + "Use /arcadetest win list to list the available effects.");
            return;
        }
        for (WinEffectGadget effect : getTestEffects())
        {
            if (effect.isRunning())
            {
                player.sendMessage(ChatColor.RED + "A win effect is already running. Finish it before starting another test.");
                return;
            }
        }

        World world = getOrCreateTestWorld();
        if (world == null)
        {
            player.sendMessage(ChatColor.RED + "Could not create the isolated win-effect test world. Check the server log.");
            return;
        }

        activeTester = player;
        activeSnapshot = new PlayerSnapshot(player);
        activeEffect = selected;
        try
        {
            resetTestArena(world);
            Location base = new Location(world, 0.5, 4.0, 0.5);
            selected.setupForTest(player, base);
            selected.buildWinnerRoom();

            Location view = base.clone().add(0.0, 3.0, 21.0);
            view.setDirection(base.toVector().subtract(view.toVector()));
            player.closeInventory();
            player.setGameMode(GameMode.ADVENTURE);
            player.setVelocity(new Vector(0, 0, 0));
            player.teleport(view);
            selected.runPlay();

            player.sendMessage(ChatColor.GREEN + "Testing " + selected.getName()
                    + ChatColor.GRAY + " in the isolated win-effect arena. You will return to the lobby when it finishes.");
            startWinMonitor();
        }
        catch (Exception ex)
        {
            plugin.getLogger().severe("Win-effect test failed to start for " + selected.getName() + ": " + ex.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Win-effect test setup exception", ex);
            finishWinTest(false);
            player.sendMessage(ChatColor.RED + "Could not start that win-effect test. Check the Arcade server log.");
        }
    }

    private void startWinMonitor()
    {
        if (winMonitorTask != null) winMonitorTask.cancel();
        winMonitorTask = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (activeEffect == null)
                {
                    cancel();
                    return;
                }
                if (!activeEffect.isRunning())
                {
                    finishWinTest(true);
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private World getOrCreateTestWorld()
    {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null)
        {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            creator.generatorSettings("2;0;1;");
            world = Bukkit.createWorld(creator);
        }
        if (world != null)
        {
            world.setTime(6000L);
            world.setStorm(false);
            world.setThundering(false);
            world.setGameRuleValue("doMobSpawning", "false");
            world.setSpawnLocation(0, 4, 0);
        }
        return world;
    }

    private void resetTestArena(World world)
    {
        for (Entity entity : new ArrayList<Entity>(world.getEntities()))
        {
            if (!(entity instanceof Player)) entity.remove();
        }
        for (int x = -ARENA_RADIUS; x <= ARENA_RADIUS; x++)
        {
            for (int z = -ARENA_RADIUS; z <= ARENA_RADIUS; z++)
            {
                for (int y = 1; y <= ARENA_CLEAN_HEIGHT; y++)
                {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
                world.getBlockAt(x, 1, z).setType(Material.BEDROCK, false);
                world.getBlockAt(x, 2, z).setType(Material.DIRT, false);
                world.getBlockAt(x, 3, z).setType(Material.GRASS, false);
            }
        }
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
    }

    private void finishWinTest(boolean notify)
    {
        WinEffectGadget effect = activeEffect;
        Player player = activeTester;
        PlayerSnapshot snapshot = activeSnapshot;
        activeEffect = null;
        activeTester = null;
        activeSnapshot = null;
        if (winMonitorTask != null)
        {
            winMonitorTask.cancel();
            winMonitorTask = null;
        }

        try
        {
            if (effect != null) effect.runFinishForTest();
        }
        catch (Exception ex)
        {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Error cleaning up win-effect test", ex);
        }
        finally
        {
            if (player != null && player.isOnline() && snapshot != null)
            {
                snapshot.restore(player);
                if (notify) player.sendMessage(ChatColor.GREEN + "Win-effect test finished; you have been returned to the lobby.");
            }
            World world = Bukkit.getWorld(WORLD_NAME);
            if (world != null)
            {
                try
                {
                    resetTestArena(world);
                }
                catch (Exception ex)
                {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Could not fully reset the win-effect test arena", ex);
                }
            }
        }
    }

    private void spawnBots(Player owner, String requestedGame, int count, boolean moving)
    {
        GameTarget game = findGame(requestedGame);
        if (game == null)
        {
            owner.sendMessage(ChatColor.RED + "Unknown game '" + requestedGame + "'. Open /arcadetest bot for the list of supported Arcade games.");
            return;
        }
        count = Math.max(1, Math.min(MAX_BOTS_PER_OWNER, count));
        int ownCount = countBots(owner.getUniqueId());
        if (ownCount + count > MAX_BOTS_PER_OWNER)
        {
            owner.sendMessage(ChatColor.RED + "You already have " + ownCount + " test bots. Clear some first; your limit is " + MAX_BOTS_PER_OWNER + ".");
            return;
        }
        if (bots.size() + count > MAX_BOTS_TOTAL)
        {
            owner.sendMessage(ChatColor.RED + "The test server is at its temporary bot limit (" + MAX_BOTS_TOTAL + "). Clear some bots first.");
            return;
        }

        List<WinEffectGadget> effects = getTestEffects();
        if (effects.isEmpty())
        {
            owner.sendMessage(ChatColor.RED + "No Arcade win-effect actor factory is registered; cannot create player-skinned test dummies.");
            return;
        }

        Vector forward = owner.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 0.001) forward = new Vector(0, 0, 1);
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        Location centre = owner.getLocation().clone().add(forward.clone().multiply(4.0));
        centre.setY(owner.getLocation().getY());
        int firstBotNumber = ownCount + 1;

        try
        {
            for (int i = 0; i < count; i++)
            {
                double side = (i - (count - 1) / 2.0) * 2.0;
                Location at = centre.clone().add(right.clone().multiply(side));
                at.setYaw(owner.getLocation().getYaw());
                at.setPitch(0.0F);
                DisguisePlayer disguise = effects.get(0).getNPC(owner, at, false);
                Entity bukkitEntity = disguise.getEntity() == null ? null : disguise.getEntity().getBukkitEntity();
                if (!(bukkitEntity instanceof ArmorStand))
                {
                    if (bukkitEntity != null) bukkitEntity.remove();
                    arcadeManager.GetDisguise().undisguise(disguise);
                    continue;
                }

                ArmorStand stand = (ArmorStand) bukkitEntity;
                stand.setGravity(false);
                stand.setArms(true);
                stand.setBasePlate(false);
                stand.setSmall(false);
                stand.setCanPickupItems(false);
                stand.setCustomName(ChatColor.GRAY + "Test Bot #" + (firstBotNumber + i) + " - " + game.name);
                stand.setCustomNameVisible(true);
                ItemStack hand = new ItemStack(game.item, 1, game.data);
                stand.getEquipment().setItemInHand(hand);
                bots.add(new BotActor(owner.getUniqueId(), game, stand, disguise, at, moving, i * 0.8));
            }
        }
        catch (Exception ex)
        {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Could not summon all " + game.name + " test bots", ex);
        }

        int spawned = countBots(owner.getUniqueId()) - ownCount;
        owner.sendMessage(ChatColor.GREEN + "Summoned " + spawned + " " + game.name + " test bot(s)"
                + (moving ? ChatColor.AQUA + " with slow movement." : ChatColor.GRAY + " in stationary target mode."));
        owner.sendMessage(ChatColor.GRAY + "These are player-skinned ArmorStand dummies with a representative held item, not real Bukkit Players.");
        owner.sendMessage(ChatColor.GRAY + "They can help test visibility, target practice and some entity interactions; player-only kits, team rules and cosmetics still need a real client.");
    }

    private void startBotMovementTask()
    {
        if (botMovementTask != null) botMovementTask.cancel();
        botMovementTask = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                Iterator<BotActor> iterator = bots.iterator();
                while (iterator.hasNext())
                {
                    BotActor bot = iterator.next();
                    if (bot.stand == null || bot.stand.isDead() || !bot.stand.isValid())
                    {
                        iterator.remove();
                        continue;
                    }
                    if (!bot.moving) continue;

                    bot.phase += 0.10;
                    Location next = bot.anchor.clone().add(Math.cos(bot.phase) * 1.1, 0.0, Math.sin(bot.phase) * 1.1);
                    next.setDirection(bot.anchor.toVector().subtract(next.toVector()));
                    bot.stand.teleport(next);
                }
            }
        }.runTaskTimer(plugin, 2L, 5L);
    }

    private int countBots(UUID owner)
    {
        int count = 0;
        for (BotActor bot : bots)
        {
            if (bot.owner.equals(owner) && bot.stand != null && bot.stand.isValid() && !bot.stand.isDead()) count++;
        }
        return count;
    }

    private void clearBots(UUID owner)
    {
        Iterator<BotActor> iterator = bots.iterator();
        while (iterator.hasNext())
        {
            BotActor bot = iterator.next();
            if (owner == null || bot.owner.equals(owner))
            {
                removeBot(bot);
                iterator.remove();
            }
        }
    }

    private void clearAllBots()
    {
        clearBots(null);
    }

    private void removeBot(BotActor bot)
    {
        try
        {
            if (bot.disguise != null) arcadeManager.GetDisguise().undisguise(bot.disguise);
        }
        catch (Exception ex)
        {
            plugin.getLogger().log(java.util.logging.Level.FINE, "Bot disguise was already removed", ex);
        }
        if (bot.stand != null && !bot.stand.isDead()) bot.stand.remove();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        UUID owner = event.getPlayer().getUniqueId();
        clearBots(owner);
        if (activeTester != null && activeTester.getUniqueId().equals(owner))
        {
            finishWinTest(false);
        }
    }

    public void shutdown()
    {
        if (shuttingDown) return;
        shuttingDown = true;
        if (activeEffect != null) finishWinTest(false);
        clearAllBots();
        if (botMovementTask != null)
        {
            botMovementTask.cancel();
            botMovementTask = null;
        }
    }

    private GameTarget findGame(String requested)
    {
        String key = normalise(requested);
        for (GameTarget game : GAMES)
        {
            if (normalise(game.key).equals(key) || normalise(game.name).equals(key)) return game;
        }
        return null;
    }

    private boolean isCount(String value)
    {
        try
        {
            Integer.parseInt(value);
            return true;
        }
        catch (NumberFormatException ex)
        {
            return false;
        }
    }

    private int parseCount(String value)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex)
        {
            return 3;
        }
    }

    private String join(String[] args, int start)
    {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < args.length; i++)
        {
            if (i > start) result.append(' ');
            result.append(args[i]);
        }
        return result.toString();
    }

    private String normalise(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private ItemStack item(Material material, String name, String... lore)
    {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private void put(Inventory inventory, int slot, ItemStack item)
    {
        inventory.setItem(slot, item);
    }

    private static final class GameTarget
    {
        private final String key;
        private final String name;
        private final Material item;
        private final short data;

        private GameTarget(String key, String name, Material item, short data)
        {
            this.key = key;
            this.name = name;
            this.item = item;
            this.data = data;
        }
    }

    private static final class BotActor
    {
        private final UUID owner;
        private final GameTarget game;
        private final ArmorStand stand;
        private final DisguisePlayer disguise;
        private final Location anchor;
        private final boolean moving;
        private double phase;

        private BotActor(UUID owner, GameTarget game, ArmorStand stand, DisguisePlayer disguise,
                         Location anchor, boolean moving, double phase)
        {
            this.owner = owner;
            this.game = game;
            this.stand = stand;
            this.disguise = disguise;
            this.anchor = anchor.clone();
            this.moving = moving;
            this.phase = phase;
        }
    }

    private static final class PlayerSnapshot
    {
        private final Location location;
        private final GameMode gameMode;
        private final ItemStack[] contents;
        private final ItemStack[] armour;
        private final int heldSlot;
        private final int level;
        private final float experience;
        private final int food;
        private final float saturation;
        private final double health;
        private final boolean allowFlight;
        private final boolean flying;
        private final int fireTicks;
        private final List<PotionEffect> potions;

        private PlayerSnapshot(Player player)
        {
            location = player.getLocation().clone();
            gameMode = player.getGameMode();
            PlayerInventory inventory = player.getInventory();
            contents = cloneItems(inventory.getContents());
            armour = cloneItems(inventory.getArmorContents());
            heldSlot = inventory.getHeldItemSlot();
            level = player.getLevel();
            experience = player.getExp();
            food = player.getFoodLevel();
            saturation = player.getSaturation();
            health = player.getHealth();
            allowFlight = player.getAllowFlight();
            flying = player.isFlying();
            fireTicks = player.getFireTicks();
            potions = new ArrayList<PotionEffect>(player.getActivePotionEffects());
        }

        private void restore(Player player)
        {
            try
            {
                player.setVelocity(new Vector(0, 0, 0));
                player.teleport(location);
                player.setGameMode(gameMode);
                player.setAllowFlight(allowFlight);
                if (allowFlight) player.setFlying(flying);
                PlayerInventory inventory = player.getInventory();
                inventory.setContents(cloneItems(contents));
                inventory.setArmorContents(cloneItems(armour));
                inventory.setHeldItemSlot(Math.max(0, Math.min(8, heldSlot)));
                player.setLevel(level);
                player.setExp(experience);
                player.setFoodLevel(food);
                player.setSaturation(saturation);
                player.setFireTicks(fireTicks);
                for (PotionEffect effect : player.getActivePotionEffects())
                {
                    player.removePotionEffect(effect.getType());
                }
                for (PotionEffect effect : potions)
                {
                    player.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration(),
                            effect.getAmplifier(), effect.isAmbient()));
                }
                double restoredHealth = Math.max(1.0, Math.min(health, player.getMaxHealth()));
                player.setHealth(restoredHealth);
                player.updateInventory();
            }
            catch (Exception ex)
            {
                Bukkit.getLogger().warning("Could not completely restore the win-effect tester's player state: " + ex.getMessage());
            }
        }

        private static ItemStack[] cloneItems(ItemStack[] source)
        {
            if (source == null) return new ItemStack[0];
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
            return copy;
        }
    }
}
