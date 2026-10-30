package au.com.testplex.wizards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class WizardsGameManager implements Listener {
    private static final String SPELL_MENU_TITLE = ChatColor.DARK_PURPLE + "Wizards Spell Selector";
    private static final int MAX_WAND_SLOTS = WizardsRules.WAND_SLOT_COUNT;
    private static final int MENU_SLOT = 8;
    private static final int ARENA_FLOOR_Y = 63;
    private static final int CHEST_COUNT_FALLBACK = 32;

    private final WizardsModernPlugin plugin;
    private final NamespacedKey spellKey;
    private final NamespacedKey wandKey;
    private final NamespacedKey menuKey;
    private final NamespacedKey soulKey;
    private final NamespacedKey projectileKey;
    private final NamespacedKey projectileDamageKey;
    private final NamespacedKey projectileOwnerKey;
    private final NamespacedKey anvilLevelKey;
    private final LinkedHashMap<UUID, Player> queue = new LinkedHashMap<>();
    private final Map<UUID, WizardsKit> selectedKits = new HashMap<>();
    private final Map<UUID, WizardState> states = new LinkedHashMap<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Map<UUID, PlayerSnapshot> pendingRestore = new HashMap<>();
    private final Map<UUID, BossBar> manaBars = new HashMap<>();
    private final Map<Location, TemporaryBlock> temporaryBlocks = new HashMap<>();
    private final Set<UUID> ownedEntities = new HashSet<>();
    private final Map<UUID, SpectralArrowData> spectralArrows = new HashMap<>();
    private final Map<UUID, SummonedWolfData> summonedWolves = new HashMap<>();
    private final List<Location> chests = new ArrayList<>();
    private final Random random = new Random();

    private final World arena;
    private final BukkitTask tickTask;
    private boolean running;
    private long startedAt;
    private int tick;
    private int startingPlayerCount;
    private int paceStep;

    public WizardsGameManager(WizardsModernPlugin plugin) {
        this.plugin = plugin;
        spellKey = new NamespacedKey(plugin, "spell");
        wandKey = new NamespacedKey(plugin, "wand");
        menuKey = new NamespacedKey(plugin, "spell-menu");
        soulKey = new NamespacedKey(plugin, "soul-star");
        projectileKey = new NamespacedKey(plugin, "spell-projectile");
        projectileDamageKey = new NamespacedKey(plugin, "spell-damage");
        projectileOwnerKey = new NamespacedKey(plugin, "spell-owner");
        anvilLevelKey = new NamespacedKey(plugin, "anvil-level");
        arena = createArena();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private World createArena() {
        String worldName = plugin.getConfig().getString("arena-world", "wizards_arena");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            world = creator.createWorld();
        }
        if (world == null) {
            plugin.getLogger().severe("Could not create the isolated Wizards arena world.");
            return null;
        }

        int radius = Math.max(32, Math.min(96, plugin.getConfig().getInt("arena-radius", 48)));
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0D, 0.0D);
        border.setSize((radius * 2.0D) + 4.0D);
        border.setDamageAmount(0.2D);
        border.setWarningDistance(5);
        world.setSpawnLocation(0, ARENA_FLOOR_Y + 1, 0);

        // No original Wizards archive is present in this repository. Build a
        // predictable mechanics arena rather than silently substituting another game map.
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                world.getBlockAt(x, ARENA_FLOOR_Y, z).setType(Material.GRASS_BLOCK, false);
                if (Math.abs(x) == radius || Math.abs(z) == radius) {
                    for (int y = ARENA_FLOOR_Y + 1; y <= ARENA_FLOOR_Y + 4; y++) {
                        world.getBlockAt(x, y, z).setType(Material.STONE_BRICKS, false);
                    }
                }
            }
        }
        for (int x = -radius + 1; x < radius; x++) {
            for (int z = -radius + 1; z < radius; z++) {
                if (Math.abs(x) > 2 && Math.abs(z) > 2) {
                    world.getBlockAt(x, ARENA_FLOOR_Y - 1, z).setType(Material.DIRT, false);
                }
            }
        }

        int chestCount = Math.max(8, Math.min(64, plugin.getConfig().getInt("chest-count", CHEST_COUNT_FALLBACK)));
        chests.clear();
        for (int i = 0; i < chestCount; i++) {
            double angle = (Math.PI * 2.0D * i) / chestCount;
            double chestRadius = 12.0D + (i % 3) * 9.0D;
            int x = (int) Math.round(Math.cos(angle) * chestRadius);
            int z = (int) Math.round(Math.sin(angle) * chestRadius);
            Block block = world.getBlockAt(x, ARENA_FLOOR_Y + 1, z);
            block.setType(Material.CHEST, false);
            chests.add(block.getLocation());
        }

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) entity.remove();
        }
        return world;
    }

    public void join(Player player) {
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Wizards arena could not be loaded. Check the server log.");
            return;
        }
        if (running) {
            player.sendMessage(ChatColor.RED + "A Wizards round is already running.");
            return;
        }
        if (queue.putIfAbsent(player.getUniqueId(), player) != null) {
            player.sendMessage(ChatColor.YELLOW + "You are already queued for Wizards.");
            return;
        }
        selectedKits.putIfAbsent(player.getUniqueId(), defaultKit());
        player.sendMessage(ChatColor.GREEN + "Queued for Wizards. Choose a kit with /wizards kit <kit>.");
        player.sendMessage(ChatColor.GRAY + "An operator or queued player can start with /wizards start.");
    }

    public void setKit(Player player, WizardsKit kit) {
        if (running) {
            player.sendMessage(ChatColor.RED + "Kits cannot be changed after the round starts.");
            return;
        }
        if (!queue.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Join the Wizards queue first with /wizards join.");
            return;
        }
        selectedKits.put(player.getUniqueId(), kit);
        player.sendMessage(ChatColor.GREEN + "Wizards kit set to " + ChatColor.WHITE + prettyKit(kit) + ChatColor.GREEN + ".");
    }

    private WizardsKit defaultKit() {
        WizardsKit kit = WizardsKit.parse(plugin.getConfig().getString("kits.default", "MAGE"));
        return kit == null ? WizardsKit.MAGE : kit;
    }

    public void start(org.bukkit.command.CommandSender sender) {
        if (running) {
            sender.sendMessage(ChatColor.RED + "Wizards is already running.");
            return;
        }
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Wizards arena failed to load.");
            return;
        }

        List<Player> entrants = new ArrayList<>();
        for (Player player : queue.values()) {
            if (player.isOnline()) entrants.add(player);
        }
        int minimum = Math.max(1, plugin.getConfig().getInt("minimum-players", 2));
        boolean allowSolo = plugin.getConfig().getBoolean("allow-solo-start", true);
        if (entrants.size() < minimum && !(allowSolo && entrants.size() == 1)) {
            sender.sendMessage(ChatColor.RED + "Wizards needs " + minimum + " queued players to start.");
            return;
        }
        if (entrants.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Nobody is queued for Wizards.");
            return;
        }

        refillChests();
        running = true;
        startedAt = System.currentTimeMillis();
        tick = 0;
        paceStep = 0;
        startingPlayerCount = entrants.size();
        states.clear();
        for (int i = 0; i < entrants.size(); i++) {
            Player player = entrants.get(i);
            UUID id = player.getUniqueId();
            snapshots.put(id, PlayerSnapshot.capture(player));
            WizardsKit kit = selectedKits.getOrDefault(id, defaultKit());
            WizardState state = new WizardState(kit);
            states.put(id, state);
            preparePlayer(player, state, i, entrants.size());
        }
        queue.clear();
        sender.sendMessage(ChatColor.GREEN + "Started Wizards with " + entrants.size() + " player(s).");
        for (Player player : entrants) {
            player.sendMessage(ChatColor.GOLD + "Wizards" + ChatColor.DARK_GRAY + " » "
                    + ChatColor.YELLOW + "Find spellbooks and supplies in chests. Last wizard alive wins!");
        }
    }

    private void preparePlayer(Player player, WizardState state, int index, int count) {
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setFoodLevel(20);
        player.setSaturation(5.0F);
        player.setLevel(0);
        player.setExp(0.0F);
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) == null
                ? 20.0D : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHealth, 20.0D));

        double angle = (Math.PI * 2.0D * index) / Math.max(1, count);
        double spawnRadius = Math.max(20.0D, Math.min(35.0D, plugin.getConfig().getInt("arena-radius", 48) - 12.0D));
        Location spawn = new Location(arena, Math.cos(angle) * spawnRadius + 0.5D,
                ARENA_FLOOR_Y + 1.0D, Math.sin(angle) * spawnRadius + 0.5D);
        spawn.setDirection(new Vector(-Math.cos(angle), 0.0D, -Math.sin(angle)));
        player.teleport(spawn);

        for (int slot = 0; slot < MAX_WAND_SLOTS; slot++) {
            if (slot < state.wandCount) {
                player.getInventory().setItem(slot, makeWand(state, slot));
            } else {
                player.getInventory().setItem(slot, makeEmptyWandSlot());
            }
        }
        player.getInventory().setItem(MENU_SLOT, makeSpellMenuBook());
        refreshManaBar(player, state);
        updateWandLabels(player, state);
        player.setCompassTarget(spawn);
    }

    public void leave(Player player) {
        UUID id = player.getUniqueId();
        if (queue.remove(id) != null) {
            selectedKits.remove(id);
            player.sendMessage(ChatColor.YELLOW + "You left the Wizards queue.");
            return;
        }
        WizardState state = states.get(id);
        if (running && state != null && state.alive) {
            state.alive = false;
            removeManaBar(id);
            restoreTemporaryPlayer(player, id);
            player.sendMessage(ChatColor.YELLOW + "You left the Wizards round.");
            Bukkit.getScheduler().runTask(plugin, this::checkWinner);
            return;
        }
        player.sendMessage(ChatColor.GRAY + "You are not queued or alive in a Wizards round.");
    }

    public String status() {
        if (!running) return queue.size() + " queued; no round running";
        long alive = states.values().stream().filter(state -> state.alive).count();
        long seconds = Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L);
        return alive + "/" + states.size() + " alive; " + queue.size() + " queued; " + seconds + "s elapsed";
    }

    public void stop(String reason) {
        if (!running && snapshots.isEmpty()) {
            queue.clear();
            states.clear();
            return;
        }
        finish(null, reason);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        if (running || !snapshots.isEmpty()) finish(null, "Wizards plugin disabled; player state restored.");
        for (Map.Entry<UUID, PlayerSnapshot> entry : pendingRestore.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && !player.isDead()) entry.getValue().restore(player);
        }
        pendingRestore.clear();
    }

    private void tick() {
        if (!running) return;
        tick++;
        tickSpectralArrows();
        tickSummonedWolves();
        long elapsed = System.currentTimeMillis() - startedAt;

        while (paceStep < WizardsRules.gamePaceMinutes.length
                && elapsed >= WizardsRules.gamePaceMinutes[paceStep] * 60_000L) {
            paceStep++;
            float modifier = Math.max(0.5F, 1.0F - paceStep * 0.1F);
            for (WizardState state : states.values()) state.cooldownModifier = modifier;
            for (Player player : alivePlayers()) {
                player.sendMessage(ChatColor.YELLOW + "Power surges through the battlefield! Spell costs and cooldowns are reduced.");
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7F, 1.2F);
            }
        }

        for (Map.Entry<UUID, WizardState> entry : states.entrySet()) {
            WizardState state = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (!state.alive || player == null || !player.isOnline()) continue;

            state.mana = WizardsRules.addMana(state.mana, WizardsRules.manaPerSecond(state.kit, state.soulStars) / 20.0F, state.maximumMana);
            if (tick % 10 == 0) {
                refreshManaBar(player, state);
                updateWandLabels(player, state);
            }
        }
    }

    private List<Player> alivePlayers() {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<UUID, WizardState> entry : states.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (entry.getValue().alive && player != null && player.isOnline()) players.add(player);
        }
        return players;
    }

    private void refreshManaBar(Player player, WizardState state) {
        BossBar bar = manaBars.computeIfAbsent(player.getUniqueId(), ignored ->
                Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_10));
        bar.setTitle(ChatColor.AQUA + "Mana " + ChatColor.WHITE + Math.round(state.mana) + "/" +
                Math.round(state.maximumMana) + ChatColor.DARK_GRAY + "  |  " + ChatColor.LIGHT_PURPLE + "Souls " + state.soulStars);
        bar.setProgress(WizardsRules.manaProgress(state.mana, state.maximumMana));
        if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
        bar.setVisible(true);
    }

    private void removeManaBar(UUID id) {
        BossBar bar = manaBars.remove(id);
        if (bar != null) bar.removeAll();
    }

    private void updateWandLabels(Player player, WizardState state) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < state.wandCount; slot++) {
            if (!isWand(inventory.getItem(slot))) {
                inventory.setItem(slot, makeWand(state, slot));
            } else {
                inventory.setItem(slot, makeWand(state, slot));
            }
        }
    }

    private ItemStack makeWand(WizardState state, int slot) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        WizardsSpell spell = state.wands[slot];
        meta.setDisplayName(ChatColor.WHITE + (spell == null ? "Spell Wand" : spell.displayName() + " Wand"));
        List<String> lore = new ArrayList<>();
        if (spell == null) {
            lore.add(ChatColor.GRAY + "Right-click to choose a spell");
        } else {
            int level = state.known.getOrDefault(spell, 1);
            lore.add(ChatColor.YELLOW + "Mana: " + ChatColor.WHITE + spell.manaCost(level));
            lore.add(ChatColor.YELLOW + "Cooldown: " + ChatColor.WHITE + spell.cooldownSeconds(level, state.cooldownModifier) + "s");
            lore.add(ChatColor.GRAY + "Left-click to cast");
            lore.add(ChatColor.GRAY + "Right-click to change spell");
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "wand");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeEmptyWandSlot() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Empty wand slot");
        meta.setLore(List.of(ChatColor.DARK_GRAY + "Find an extra wand in a chest or from a fallen wizard."));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "empty");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSpellMenuBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Wizard Spells");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to view discovered spells",
                ChatColor.GRAY + "Bind a spell to the wand in your hand."));
        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSpellBook(WizardsSpell spell) {
        ItemStack item = new ItemStack(spell.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(categoryColour(spell) + spell.displayName() + " Spellbook");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click or pick up to learn this spell",
                ChatColor.YELLOW + "Maximum level: " + ChatColor.WHITE + spell.maximumLevel(),
                ChatColor.DARK_GRAY + "Wizards spell: " + spell.id()));
        meta.getPersistentDataContainer().set(spellKey, PersistentDataType.STRING, spell.id());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeWandLoot() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Spell Wand");
        meta.setLore(List.of(ChatColor.GRAY + "Adds another wand slot, up to five."));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "loot");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSoulStar() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Wizard Soul");
        meta.setLore(List.of(ChatColor.GRAY + "Permanently increases mana regeneration this round."));
        meta.getPersistentDataContainer().set(soulKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void openSpellMenu(Player player, WizardState state) {
        Inventory inventory = Bukkit.createInventory(null, 54, SPELL_MENU_TITLE);
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
        int index = 0;
        for (WizardsSpell spell : WizardsSpell.implementedSpells()) {
            int level = state.known.getOrDefault(spell, 0);
            if (level <= 0) continue;
            ItemStack icon = new ItemStack(spell.icon());
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(categoryColour(spell) + spell.displayName());
            meta.setLore(List.of(
                    ChatColor.YELLOW + "Level: " + ChatColor.WHITE + level + "/" + spell.maximumLevel(),
                    ChatColor.YELLOW + "Mana: " + ChatColor.WHITE + spell.manaCost(level),
                    ChatColor.YELLOW + "Cooldown: " + ChatColor.WHITE + spell.cooldownSeconds(level, state.cooldownModifier) + " seconds",
                    "",
                    ChatColor.GREEN + "Left-click: bind to held wand",
                    ChatColor.GREEN + "Right-click: quickcast"));
            meta.getPersistentDataContainer().set(spellKey, PersistentDataType.STRING, spell.id());
            icon.setAmount(Math.max(1, Math.min(64, level)));
            icon.setItemMeta(meta);
            inventory.setItem(slots[index++], icon);
        }
        if (index == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "No spells discovered");
            meta.setLore(List.of(ChatColor.GRAY + "Find spellbooks in arena chests."));
            empty.setItemMeta(meta);
            inventory.setItem(13, empty);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        WizardState state = states.get(player.getUniqueId());
        if (!running || state == null || !state.alive) return;

        Action action = event.getAction();
        ItemStack item = event.getItem();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (isMenuBook(item)) {
                event.setCancelled(true);
                openSpellMenu(player, state);
                return;
            }
            int slot = player.getInventory().getHeldItemSlot();
            if (slot >= 0 && slot < state.wandCount && isWand(item)) {
                event.setCancelled(true);
                openSpellMenu(player, state);
                return;
            }
        }
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            int slot = player.getInventory().getHeldItemSlot();
            if (slot >= 0 && slot < state.wandCount && isWand(item)) {
                event.setCancelled(true);
                WizardsSpell spell = state.wands[slot];
                if (spell != null) castSpell(player, state, spell, null, event.getClickedBlock());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        WizardState state = states.get(player.getUniqueId());
        if (state == null || !state.alive) return;

        if (SPELL_MENU_TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            WizardsSpell spell = spellFromItem(event.getCurrentItem());
            if (spell == null) return;
            int heldSlot = player.getInventory().getHeldItemSlot();
            if (heldSlot < 0 || heldSlot >= state.wandCount) {
                player.sendMessage(ChatColor.RED + "Hold one of your wands before selecting a spell.");
                player.closeInventory();
                return;
            }
            if (event.getClick().isLeftClick()) {
                state.wands[heldSlot] = spell;
                player.sendMessage(ChatColor.BLUE + "Spell on wand set to " + categoryColour(spell) + spell.displayName());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
                updateWandLabels(player, state);
                player.closeInventory();
            } else if (event.getClick().isRightClick()) {
                player.closeInventory();
                castSpell(player, state, spell, null, null);
            }
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() != null && event.getClickedInventory() == top
                && top.getType() == InventoryType.CHEST) {
            ItemStack clicked = event.getCurrentItem();
            if (isSpellBook(clicked) || isLootWand(clicked) || isSoulStar(clicked)) {
                event.setCancelled(true);
                consumeLootItem(player, state, clicked);
                event.getClickedInventory().setItem(event.getSlot(), null);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.0F);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        WizardState state = states.get(player.getUniqueId());
        if (!running || state == null || !state.alive) return;
        ItemStack item = event.getItem().getItemStack();
        if (isSpellBook(item) || isLootWand(item) || isSoulStar(item)) {
            event.setCancelled(true);
            consumeLootItem(player, state, item);
            event.getItem().remove();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.0F);
        }
    }

    private void consumeLootItem(Player player, WizardState state, ItemStack item) {
        WizardsSpell spell = spellFromItem(item);
        if (spell != null) {
            int current = state.known.getOrDefault(spell, 0);
            if (current < spell.maximumLevel()) {
                int next = WizardsRules.nextSpellLevel(current, spell.maximumLevel());
                state.known.put(spell, next);
                player.sendMessage(categoryColour(spell) + spell.displayName() + ChatColor.DARK_GRAY
                        + " » " + ChatColor.GRAY + "Levelled up to " + next + ".");
            } else {
                addMana(state, 50.0F);
                player.sendMessage(ChatColor.GOLD + "Wizards » " + ChatColor.GRAY + "Spellbook converted into 50 mana.");
            }
            refreshManaBar(player, state);
            return;
        }

        if (isLootWand(item)) {
            if (WizardsRules.canGainWand(state.wandCount)) {
                int slot = state.wandCount++;
                player.getInventory().setItem(slot, makeWand(state, slot));
                player.sendMessage(ChatColor.GOLD + "Wizards » " + ChatColor.GRAY + "Extra wand gained.");
            } else {
                addMana(state, WizardsRules.FULL_WAND_CONVERSION);
                player.sendMessage(ChatColor.GOLD + "Wizards » " + ChatColor.GRAY + "Extra wand converted into 100 mana.");
            }
            refreshManaBar(player, state);
            return;
        }

        if (isSoulStar(item)) {
            state.soulStars++;
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Wizard Soul absorbed. Mana regeneration increased.");
            refreshManaBar(player, state);
        }
    }

    private void addMana(WizardState state, float amount) {
        state.mana = WizardsRules.addMana(state.mana, amount, state.maximumMana);
    }

    @EventHandler
    public void onHeldSlotChange(PlayerItemHeldEvent event) {
        WizardState state = states.get(event.getPlayer().getUniqueId());
        if (state == null || !state.alive) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) updateWandLabels(event.getPlayer(), state);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Wolf wolf && summonedWolves.containsKey(wolf.getUniqueId())) {
            SummonedWolfData data = summonedWolves.get(wolf.getUniqueId());
            Player owner = data == null ? null : Bukkit.getPlayer(data.owner);
            WizardState ownerState = owner == null ? null : states.get(owner.getUniqueId());
            if (owner == null || !owner.isOnline() || ownerState == null || !ownerState.alive) {
                event.setCancelled(true);
                wolf.remove();
                summonedWolves.remove(wolf.getUniqueId());
            } else if (event.getEntity() instanceof Player victim
                    && states.containsKey(victim.getUniqueId()) && states.get(victim.getUniqueId()).alive) {
                event.setDamage(event.getDamage() * 0.3D);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getDamager() instanceof Projectile projectile) {
            String projectileSpell = projectile.getPersistentDataContainer().get(projectileKey, PersistentDataType.STRING);
            if (projectileSpell != null && event.getEntity() instanceof Player victim) {
                if ("web-shot".equals(projectileSpell)) {
                    event.setCancelled(true);
                    return;
                }
                if ("spectral-arrow".equals(projectileSpell)) {
                    SpectralArrowData data = spectralArrows.remove(projectile.getUniqueId());
                    if (data != null) {
                        double distance = data.origin.distance(victim.getLocation());
                        event.setDamage(6.0D + distance / Math.max(1.0D, 7.0D - data.level));
                    }
                } else {
                    Double damage = projectile.getPersistentDataContainer().get(projectileDamageKey, PersistentDataType.DOUBLE);
                    if (damage != null) event.setDamage(damage);
                }
                projectile.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.08);
            }
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) return;
        WizardState state = states.get(attacker.getUniqueId());
        if (!running || state == null || !state.alive) return;
        int slot = attacker.getInventory().getHeldItemSlot();
        if (slot >= 0 && slot < state.wandCount && isWand(attacker.getInventory().getItem(slot))) {
            WizardsSpell spell = state.wands[slot];
            if (spell != null) castSpell(attacker, state, spell, event.getEntity(), null);
            event.setDamage(Math.min(event.getDamage(), 0.5D));
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        String spell = projectile.getPersistentDataContainer().get(projectileKey, PersistentDataType.STRING);
        if (spell == null) return;
        Location location = projectile.getLocation();
        if ("web-shot".equals(spell)) {
            Location target = event.getHitEntity() == null
                    ? (event.getHitBlock() == null ? location : event.getHitBlock().getLocation().add(0.5D, 1.0D, 0.5D))
                    : event.getHitEntity().getLocation();
            placeWebAt(target);
            location.getWorld().spawnParticle(Particle.CLOUD, location, 10, 0.25D, 0.25D, 0.25D, 0.03D);
            location.getWorld().playSound(location, Sound.BLOCK_COBWEB_PLACE, 0.6F, 1.1F);
        } else if ("ice-shards".equals(spell)) {
            location.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 18, 0.3D, 0.3D, 0.3D, 0.03D);
            location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 0.7F, 1.4F);
            extinguishNearbyFire(location);
        } else if ("spectral-arrow".equals(spell)) {
            spectralArrows.remove(projectile.getUniqueId());
            location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 14, 0.25D, 0.25D, 0.25D, 0.04D);
        } else if ("fireball".equals(spell)) {
            location.getWorld().spawnParticle(Particle.FLAME, location, 18, 0.4D, 0.4D, 0.4D, 0.02D);
            location.getWorld().playSound(location, Sound.ENTITY_BLAZE_HURT, 0.5F, 1.4F);
        } else if ("anvil-drop".equals(spell)) {
            handleAnvilImpact(projectile);
        } else {
            location.getWorld().spawnParticle(Particle.END_ROD, location, 8, 0.2D, 0.2D, 0.2D, 0.02D);
            location.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5F, 1.8F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnvilLand(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock)) return;
        String spell = entity.getPersistentDataContainer().get(projectileKey, PersistentDataType.STRING);
        if (!"anvil-drop".equals(spell)) return;
        event.setCancelled(true);
        handleAnvilImpact(entity);
    }

    private void handleAnvilImpact(Entity entity) {
        if (entity == null || !entity.isValid()) return;
        int level = entity.getPersistentDataContainer().getOrDefault(anvilLevelKey, PersistentDataType.INTEGER, 1);
        String ownerId = entity.getPersistentDataContainer().get(projectileOwnerKey, PersistentDataType.STRING);
        Player owner = ownerId == null ? null : Bukkit.getPlayer(UUID.fromString(ownerId));
        Location impact = entity.getLocation();
        if (owner != null && owner.isOnline()) {
            double radius = 3.0D + level / 2.0D;
            for (Player victim : alivePlayers()) {
                if (!victim.getWorld().equals(impact.getWorld())) continue;
                double distance = victim.getLocation().distance(impact);
                if (distance <= radius) {
                    victim.damage(3.0D + level * 2.0D, owner);
                }
            }
        }
        impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 2, 0.35D, 0.25D, 0.35D, 0.02D);
        impact.getWorld().spawnParticle(Particle.CLOUD, impact, 18, 0.45D, 0.25D, 0.45D, 0.06D);
        impact.getWorld().playSound(impact, Sound.BLOCK_ANVIL_LAND, 1.2F, 0.8F);
        ownedEntities.remove(entity.getUniqueId());
        entity.remove();
    }

    private void placeWebAt(Location target) {
        if (target == null || target.getWorld() == null) return;
        for (int y = 1; y >= -1; y--) {
            for (int radius = 0; radius <= 1; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location candidate = target.getBlock().getLocation().add(x, y, z);
                        if (candidate.getBlock().isPassable()
                                && candidate.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                            if (setTemporaryBlock(candidate, Material.COBWEB, 100L)) return;
                        }
                    }
                }
            }
        }
    }

    private void extinguishNearbyFire(Location location) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = location.getBlock().getRelative(x, y, z);
                    if (block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void tickSpectralArrows() {
        var iterator = spectralArrows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SpectralArrowData> entry = iterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Arrow arrow) || !arrow.isValid() || arrow.isOnGround()) {
                iterator.remove();
                continue;
            }
            SpectralArrowData data = entry.getValue();
            Location current = arrow.getLocation();
            double distance = Math.max(0.1D, current.distance(data.lastPosition));
            int count = Math.max(1, Math.min(12, (int) Math.ceil(distance / 0.35D)));
            for (int i = 0; i < count; i++) {
                Location particle = data.lastPosition.clone().add(current.clone().subtract(data.lastPosition).multiply((i + 1.0D) / count));
                current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            data.lastPosition = current.clone();
        }
    }

    private void tickSummonedWolves() {
        long now = System.currentTimeMillis();
        var iterator = summonedWolves.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SummonedWolfData> entry = iterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            Player owner = Bukkit.getPlayer(entry.getValue().owner);
            WizardState ownerState = owner == null ? null : states.get(owner.getUniqueId());
            if (!(entity instanceof Wolf wolf) || !wolf.isValid() || owner == null || !owner.isOnline()
                    || ownerState == null || !ownerState.alive || now >= entry.getValue().expiresAt) {
                if (entity != null && entity.isValid()) entity.remove();
                iterator.remove();
                continue;
            }

            Player closest = null;
            double closestDistance = 16.0D;
            for (Player candidate : alivePlayers()) {
                if (candidate.equals(owner) || !candidate.getWorld().equals(wolf.getWorld())) continue;
                double distance = candidate.getLocation().distance(wolf.getLocation());
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
            if (closest != null) {
                wolf.setTarget(closest);
            } else {
                wolf.setTarget(null);
                if (wolf.getLocation().distance(owner.getLocation()) > 16.0D) wolf.teleport(owner.getLocation());
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        WizardState state = states.get(player.getUniqueId());
        if (!running || state == null || !state.alive) return;
        state.alive = false;
        dropDeathLoot(player, state);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
        removeManaBar(player.getUniqueId());
        player.sendMessage(ChatColor.RED + "You have been eliminated from Wizards.");
        Bukkit.getScheduler().runTaskLater(plugin, this::checkWinner, 2L);
    }

    private void dropDeathLoot(Player player, WizardState state) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = MAX_WAND_SLOTS; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) drop(player, item.clone());
        }
        for (ItemStack armour : inventory.getArmorContents()) {
            if (armour != null && !armour.getType().isAir()) drop(player, armour.clone());
        }

        Set<WizardsSpell> droppedSpells = new HashSet<>();
        for (WizardsSpell spell : state.wands) {
            if (spell != null && spell != WizardsSpell.MANA_BOLT) droppedSpells.add(spell);
        }
        for (Map.Entry<WizardsSpell, Integer> entry : state.known.entrySet()) {
            if (entry.getKey() != WizardsSpell.MANA_BOLT && random.nextInt(5) == 0) droppedSpells.add(entry.getKey());
        }
        List<ItemStack> loot = new ArrayList<>();
        for (WizardsSpell spell : droppedSpells) loot.add(makeSpellBook(spell));
        if (state.wandCount > 3 || random.nextBoolean()) loot.add(makeWandLoot());
        loot.add(makeSoulStar());
        for (ItemStack item : loot) drop(player, item);
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
    }

    private void drop(Player player, ItemStack item) {
        org.bukkit.entity.Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), item);
        dropped.setPickupDelay(20);
        ownedEntities.add(dropped.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PlayerSnapshot restore = pendingRestore.get(id);
        if (restore != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = event.getPlayer();
                if (player.isOnline() && !player.isDead()) {
                    pendingRestore.remove(id);
                    restore.restore(player);
                }
            });
            return;
        }
        WizardState state = states.get(id);
        if (running && state != null && !state.alive && arena != null) {
            event.setRespawnLocation(new Location(arena, 0.5D, ARENA_FLOOR_Y + 2.0D, 0.5D));
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = event.getPlayer();
                if (!player.isOnline() || !running || state.alive) return;
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                player.setFlying(true);
                player.sendMessage(ChatColor.GRAY + "You are spectating the remaining wizards.");
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        queue.remove(id);
        WizardState state = states.get(id);
        if (running && state != null && state.alive) {
            state.alive = false;
            dropDeathLoot(event.getPlayer(), state);
            removeManaBar(id);
            Bukkit.getScheduler().runTaskLater(plugin, this::checkWinner, 2L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerSnapshot snapshot = pendingRestore.get(event.getPlayer().getUniqueId());
        if (snapshot == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (player.isOnline() && !player.isDead()) {
                pendingRestore.remove(player.getUniqueId());
                snapshot.restore(player);
            }
        }, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        WizardState state = states.get(event.getPlayer().getUniqueId());
        if (running && state != null && state.alive
                && event.getPlayer().getInventory().getHeldItemSlot() < MAX_WAND_SLOTS
                && isWand(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        temporaryBlocks.remove(event.getBlock().getLocation());
    }

    private void castSpell(Player player, WizardState state, WizardsSpell spell, Entity target, Block clickedBlock) {
        int level = state.known.getOrDefault(spell, 0);
        if (level <= 0) return;
        if (!spell.implemented()) {
            player.sendMessage(ChatColor.RED + spell.displayName() + " is not in the first 26.3 mechanics milestone yet.");
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownUntil = state.cooldowns.getOrDefault(spell, 0L);
        if (cooldownUntil > now) {
            player.sendMessage(ChatColor.BLUE + spell.displayName() + " has not recharged (" +
                    Math.max(1, (cooldownUntil - now + 999) / 1000) + "s).");
            return;
        }
        int manaCost = spell.manaCost(level);
        if (state.mana < manaCost) {
            player.sendMessage(ChatColor.BLUE + "You do not have enough mana for " + spell.displayName() + ".");
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7F, 0.7F);
            return;
        }
        if (!applySpell(player, state, spell, level, target, clickedBlock)) return;

        state.mana -= manaCost;
        int cooldown = spell.cooldownSeconds(level, state.cooldownModifier);
        state.cooldowns.put(spell, now + cooldown * 1000L);
        refreshManaBar(player, state);
        updateWandLabels(player, state);
        player.sendMessage(ChatColor.DARK_AQUA + spell.displayName() + ChatColor.GRAY + " cast • " +
                ChatColor.AQUA + manaCost + " mana" + ChatColor.GRAY + " • " + cooldown + "s cooldown");
    }

    private boolean applySpell(Player caster, WizardState state, WizardsSpell spell, int level,
                               Entity directTarget, Block clickedBlock) {
        Location origin = caster.getEyeLocation();
        Vector direction = origin.getDirection().normalize();
        switch (spell) {
            case ANVIL_DROP: {
                int radius = 4 + level * 2;
                List<Player> targets = new ArrayList<>();
                targets.add(caster);
                for (Player candidate : alivePlayers()) {
                    if (candidate.equals(caster) || !candidate.getWorld().equals(caster.getWorld())) continue;
                    if (Math.abs(candidate.getLocation().getX() - caster.getLocation().getX()) <= radius
                            && Math.abs(candidate.getLocation().getZ() - caster.getLocation().getZ()) <= radius
                            && Math.abs(candidate.getLocation().getY() - caster.getLocation().getY()) <= radius * 3.0D) {
                        targets.add(candidate);
                    }
                }
                int spawned = 0;
                for (Player target : targets) {
                    Location spawn = target.getLocation().clone().add(0.0D, 15.0D + level * 3.0D, 0.0D);
                    spawn.setY(Math.min(spawn.getY(), spawn.getWorld().getMaxHeight() - 2.0D));
                    int lowered = 0;
                    while (lowered < 5 && !spawn.getBlock().isPassable()) {
                        spawn.subtract(0.0D, 1.0D, 0.0D);
                        lowered++;
                    }
                    if (!spawn.getBlock().isPassable()) continue;
                    FallingBlock anvil = spawn.getWorld().spawnFallingBlock(spawn, Material.ANVIL.createBlockData());
                    anvil.setDropItem(false);
                    anvil.setHurtEntities(false);
                    anvil.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "anvil-drop");
                    anvil.getPersistentDataContainer().set(anvilLevelKey, PersistentDataType.INTEGER, level);
                    anvil.getPersistentDataContainer().set(projectileOwnerKey, PersistentDataType.STRING, caster.getUniqueId().toString());
                    ownedEntities.add(anvil.getUniqueId());
                    spawned++;
                }
                if (spawned == 0) {
                    caster.sendMessage(ChatColor.RED + "Anvil Drop could not find clear space overhead.");
                    return false;
                }
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 0.8F);
                return true;
            }
            case ICE_SHARDS: {
                launchIceShard(caster, 4);
                for (int i = 1; i <= level; i++) {
                    final Player shooter = caster;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        WizardState current = states.get(shooter.getUniqueId());
                        if (running && current != null && current.alive && shooter.isOnline()) launchIceShard(shooter, 4);
                    }, i * 5L);
                }
                return true;
            }
            case SPECTRAL_ARROW: {
                Arrow arrow = caster.launchProjectile(Arrow.class);
                arrow.setVelocity(arrow.getVelocity().multiply(2.3D));
                arrow.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, spell.id());
                spectralArrows.put(arrow.getUniqueId(), new SpectralArrowData(caster.getUniqueId(), level,
                        caster.getLocation().clone(), arrow.getLocation().clone()));
                ownedEntities.add(arrow.getUniqueId());
                return true;
            }
            case SUMMON_WOLVES: {
                Location base = clickedBlock == null ? caster.getLocation().getBlock().getLocation()
                        : clickedBlock.getRelative(org.bukkit.block.BlockFace.UP).getLocation();
                if (!base.getBlock().isPassable()) base = caster.getLocation();
                int spawned = 0;
                for (int i = 0; i < level; i++) {
                    Location spawn = base.clone().add((random.nextDouble() - 0.5D) * 1.2D, 0.0D,
                            (random.nextDouble() - 0.5D) * 1.2D);
                    Wolf wolf = spawn.getWorld().spawn(spawn, Wolf.class);
                    wolf.setTamed(true);
                    wolf.setOwner(caster);
                    wolf.setCollarColor(DyeColor.YELLOW);
                    wolf.setCustomName(caster.getName() + "'s Wizard Wolf");
                    wolf.setCustomNameVisible(false);
                    if (wolf.getAttribute(Attribute.MAX_HEALTH) != null) {
                        wolf.getAttribute(Attribute.MAX_HEALTH).setBaseValue(0.5D);
                    }
                    wolf.setHealth(0.5D);
                    summonedWolves.put(wolf.getUniqueId(), new SummonedWolfData(caster.getUniqueId(), System.currentTimeMillis() + 30_000L));
                    ownedEntities.add(wolf.getUniqueId());
                    spawned++;
                }
                caster.getWorld().spawnParticle(Particle.POOF, base.clone().add(0.0D, 0.8D, 0.0D), 18, 0.6D, 0.25D, 0.6D, 0.05D);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.8F, 1.4F);
                return spawned > 0;
            }
            case WEB_SHOT: {
                launchWebShot(caster);
                for (int i = 1; i < level * 2 - 1; i++) {
                    final Player shooter = caster;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        WizardState current = states.get(shooter.getUniqueId());
                        if (running && current != null && current.alive && shooter.isOnline()) launchWebShot(shooter);
                    }, i * 10L);
                }
                return true;
            }
            case MANA_BOLT:
            case FIREBALL: {
                Projectile projectile;
                if (spell == WizardsSpell.FIREBALL) {
                    Fireball fireball = caster.launchProjectile(Fireball.class);
                    fireball.setIsIncendiary(false);
                    fireball.setYield(0.0F);
                    projectile = fireball;
                } else {
                    projectile = caster.launchProjectile(Snowball.class);
                }
                projectile.setVelocity(direction.multiply(spell == WizardsSpell.MANA_BOLT ? 1.8D : 1.35D));
                projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, spell.id());
                double damage = spell == WizardsSpell.MANA_BOLT ? level + 2.0D : level + 3.0D;
                projectile.getPersistentDataContainer().set(projectileDamageKey, PersistentDataType.DOUBLE, damage);
                ownedEntities.add(projectile.getUniqueId());
                caster.getWorld().spawnParticle(spell == WizardsSpell.MANA_BOLT ? Particle.END_ROD : Particle.FLAME,
                        origin.add(direction.clone().multiply(1.2D)), 5, 0.08, 0.08, 0.08, 0.01);
                return true;
            }
            case FLASH: {
                Block target = caster.getTargetBlockExact(20 + (level * 10));
                if (target == null) {
                    caster.sendMessage(ChatColor.RED + "Flash needs a solid block to target.");
                    return false;
                }
                Location destination = findSafeStandingLocation(target.getLocation());
                if (destination == null) {
                    caster.sendMessage(ChatColor.RED + "There is no safe landing space at that target.");
                    return false;
                }
                destination.setYaw(caster.getLocation().getYaw());
                destination.setPitch(caster.getLocation().getPitch());
                caster.teleport(destination);
                caster.getWorld().spawnParticle(Particle.PORTAL, destination, 24, 0.4, 0.5, 0.4, 0.4);
                caster.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7F, 1.1F);
                return true;
            }
            case FROST_BARRIER: {
                Vector forward = caster.getLocation().getDirection().setY(0.0D);
                if (forward.lengthSquared() < 0.001D) forward = new Vector(0, 0, 1);
                forward.normalize();
                Vector side = new Vector(-forward.getZ(), 0, forward.getX());
                Location centre = caster.getLocation().add(forward.multiply(3.0D));
                int halfWidth = 2 + level;
                int height = 1 + level;
                int placed = 0;
                for (int width = -halfWidth; width <= halfWidth; width++) {
                    for (int y = 0; y < height; y++) {
                        Location location = centre.clone().add(side.clone().multiply(width)).add(0, y, 0);
                        if (setTemporaryBlock(location, Material.ICE, 100L)) placed++;
                    }
                }
                if (placed == 0) {
                    caster.sendMessage(ChatColor.RED + "Frost Barrier has no room to form.");
                    return false;
                }
                caster.getWorld().playSound(centre, Sound.BLOCK_GLASS_PLACE, 0.8F, 0.8F);
                return true;
            }
            case GUST: {
                double radius = 10.0D + level * 3.0D;
                int affected = 0;
                for (Entity entity : caster.getWorld().getNearbyEntities(caster.getLocation(), radius, radius, radius)) {
                    if (!(entity instanceof Player target) || target.equals(caster)) continue;
                    Vector push = target.getLocation().toVector().subtract(caster.getLocation().toVector());
                    double distance = Math.max(1.0D, push.length());
                    push.normalize().multiply(Math.max(0.45D, (1.0D + level * 0.25D) * (1.0D - distance / (radius * 1.5D))));
                    push.setY(0.35D + level * 0.08D);
                    target.setVelocity(push);
                    affected++;
                }
                caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation().add(0, 1, 0), 35,
                        radius / 5.0D, 0.5D, radius / 5.0D, 0.12D);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.9F, 1.0F);
                return affected > 0;
            }
            case HEAL: {
                double maximum = caster.getAttribute(Attribute.MAX_HEALTH) == null
                        ? 20.0D : caster.getAttribute(Attribute.MAX_HEALTH).getValue();
                double heal = 2.0D + (level * 0.5D);
                if (caster.getHealth() >= maximum) {
                    caster.sendMessage(ChatColor.YELLOW + "You are already at full health.");
                    return false;
                }
                caster.setHealth(Math.min(maximum, caster.getHealth() + heal));
                caster.getWorld().spawnParticle(Particle.HEART, caster.getLocation().add(0, 1, 0), 12, 0.5, 0.6, 0.5, 0.05);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.6F);
                return true;
            }
            case ICE_PRISON: {
                Player victim = directTarget instanceof Player direct && !direct.equals(caster)
                        ? direct : findTargetPlayer(caster, 20.0D);
                if (victim == null) {
                    caster.sendMessage(ChatColor.RED + "Ice Prison needs an enemy in your line of sight.");
                    return false;
                }
                Location base = victim.getLocation().getBlock().getLocation();
                int radius = 1 + (level / 2);
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
                        for (int y = 0; y <= 2 + level / 2; y++) {
                            setTemporaryBlock(base.clone().add(x, y, z), Material.ICE, 80L);
                        }
                    }
                }
                victim.getWorld().spawnParticle(Particle.SNOWFLAKE, base.clone().add(0, 1, 0), 28, 1, 1, 1, 0.05);
                victim.getWorld().playSound(base, Sound.BLOCK_GLASS_PLACE, 0.9F, 0.7F);
                return true;
            }
            case LIGHTNING_STRIKE: {
                Player victim = directTarget instanceof Player direct && !direct.equals(caster)
                        ? direct : findTargetPlayer(caster, 40.0D);
                Block aimed = clickedBlock != null ? clickedBlock : caster.getTargetBlockExact(40);
                Location strike = victim != null ? victim.getLocation() : (aimed == null ? null : aimed.getLocation());
                if (strike == null) {
                    caster.sendMessage(ChatColor.RED + "Lightning Strike needs a target.");
                    return false;
                }
                caster.getWorld().strikeLightningEffect(strike);
                for (Entity entity : caster.getWorld().getNearbyEntities(strike, 2.8D, 3.0D, 2.8D)) {
                    if (entity instanceof Player target && !target.equals(caster)) {
                        target.damage(2.0D + level * 2.0D, caster);
                    }
                }
                caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strike, 20, 0.6, 1.0, 0.6, 0.1);
                return true;
            }
            case SPEED_BOOST: {
                caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (6 + level * 2) * 20,
                        Math.max(0, level - 1), true, true, true));
                caster.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, caster.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, 0.05);
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.8F, 1.4F);
                return true;
            }
            case WIZARDS_COMPASS: {
                Player target = findTargetPlayer(caster, 100.0D);
                if (target == null) {
                    caster.sendMessage(ChatColor.GRAY + "No other living wizard could be found.");
                    return false;
                }
                caster.setCompassTarget(target.getLocation());
                caster.sendMessage(ChatColor.AQUA + "Nearest wizard: " + ChatColor.WHITE + target.getName()
                        + ChatColor.GRAY + " (" + Math.round(caster.getLocation().distance(target.getLocation())) + " blocks)");
                target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 8, 0.35, 0.5, 0.35, 0.1, caster);
                caster.playSound(caster.getLocation(), Sound.ITEM_COMPASS_LOCK, 0.7F, 1.4F);
                return true;
            }
            default:
                return false;
        }
    }

    private void launchIceShard(Player caster, double damage) {
        Snowball shard = caster.launchProjectile(Snowball.class);
        shard.setVelocity(caster.getEyeLocation().getDirection().normalize().multiply(2.0D));
        shard.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "ice-shards");
        shard.getPersistentDataContainer().set(projectileDamageKey, PersistentDataType.DOUBLE, damage);
        ownedEntities.add(shard.getUniqueId());
    }

    private void launchWebShot(Player caster) {
        Snowball web = caster.launchProjectile(Snowball.class);
        web.setVelocity(caster.getEyeLocation().getDirection().normalize().multiply(1.5D));
        web.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "web-shot");
        ownedEntities.add(web.getUniqueId());
    }

    private Location findSafeStandingLocation(Location blockLocation) {
        World world = blockLocation.getWorld();
        int x = blockLocation.getBlockX();
        int z = blockLocation.getBlockZ();
        for (int y = blockLocation.getBlockY() + 1; y <= blockLocation.getBlockY() + 4; y++) {
            if (world.getBlockAt(x, y, z).isPassable() && world.getBlockAt(x, y + 1, z).isPassable()
                    && world.getBlockAt(x, y - 1, z).getType().isSolid()) {
                return new Location(world, x + 0.5D, y, z + 0.5D);
            }
        }
        return null;
    }

    private Player findTargetPlayer(Player caster, double range) {
        Player best = null;
        double bestDistance = range * range;
        Vector direction = caster.getEyeLocation().getDirection().normalize();
        for (Player candidate : alivePlayers()) {
            if (candidate.equals(caster) || !candidate.getWorld().equals(caster.getWorld())) continue;
            Vector offset = candidate.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector());
            double distanceSquared = offset.lengthSquared();
            if (distanceSquared >= bestDistance || distanceSquared < 0.01D) continue;
            double distance = Math.sqrt(distanceSquared);
            double alignment = direction.dot(offset.clone().normalize());
            if (alignment < 0.82D) continue;
            if (caster.getWorld().rayTraceBlocks(caster.getEyeLocation(), direction, distance,
                    org.bukkit.FluidCollisionMode.NEVER) != null) continue;
            best = candidate;
            bestDistance = distanceSquared;
        }
        return best;
    }

    private boolean setTemporaryBlock(Location location, Material material, long durationTicks) {
        Block block = location.getBlock();
        if (!block.isPassable()) return false;
        Location key = block.getLocation();
        temporaryBlocks.putIfAbsent(key, new TemporaryBlock(block.getBlockData().clone(), material));
        block.setType(material, false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> restoreTemporaryBlock(key), durationTicks);
        return true;
    }

    private void restoreTemporaryBlock(Location key) {
        TemporaryBlock temporary = temporaryBlocks.remove(key);
        if (temporary == null || key.getWorld() == null) return;
        Block block = key.getBlock();
        if (block.getType() == temporary.placedMaterial) {
            block.setBlockData(temporary.originalData, false);
        }
    }

    private void restoreAllTemporaryBlocks() {
        for (Location location : new ArrayList<>(temporaryBlocks.keySet())) restoreTemporaryBlock(location);
        temporaryBlocks.clear();
    }

    private void checkWinner() {
        if (!running) return;
        List<Player> alive = alivePlayers();
        if (alive.size() == 0) {
            finish(null, "No wizard survived the round.");
        } else if (startingPlayerCount > 1 && alive.size() == 1) {
            finish(alive.get(0), alive.get(0).getName() + " is the last wizard alive!");
        }
    }

    private void finish(Player winner, String message) {
        running = false;
        if (message != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.GOLD + "Wizards » " + ChatColor.YELLOW + message);
            }
        }
        for (UUID id : new ArrayList<>(manaBars.keySet())) removeManaBar(id);
        restoreAllTemporaryBlocks();
        spectralArrows.clear();
        summonedWolves.clear();

        for (UUID id : ownedEntities) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) entity.remove();
        }
        ownedEntities.clear();

        for (Map.Entry<UUID, PlayerSnapshot> entry : snapshots.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && !player.isDead()) {
                entry.getValue().restore(player);
            } else {
                pendingRestore.put(entry.getKey(), entry.getValue());
            }
        }
        snapshots.clear();
        states.clear();
        queue.clear();
        selectedKits.clear();
        startingPlayerCount = 0;
        if (arena != null) {
            arena.setTime(6000L);
            arena.setStorm(false);
            arena.setThundering(false);
            WorldBorder border = arena.getWorldBorder();
            border.setCenter(0.0D, 0.0D);
            border.setSize((Math.max(32, Math.min(96, plugin.getConfig().getInt("arena-radius", 48))) * 2.0D) + 4.0D);
        }
    }

    private void restoreTemporaryPlayer(Player player, UUID id) {
        PlayerSnapshot snapshot = snapshots.remove(id);
        if (snapshot != null) snapshot.restore(player);
        states.remove(id);
        removeManaBar(id);
        checkWinner();
    }

    private void refillChests() {
        if (arena == null) return;
        for (Location location : chests) {
            Block block = location.getBlock();
            if (block.getType() != Material.CHEST) block.setType(Material.CHEST, false);
            if (!(block.getState() instanceof Chest chest)) continue;
            Inventory inventory = chest.getBlockInventory();
            inventory.clear();

            int items = 5 + random.nextInt(4);
            int spellSlot = random.nextInt(inventory.getSize());
            inventory.setItem(spellSlot, makeSpellBook(weightedSpell()));
            for (int i = 0; i < items; i++) {
                int slot = random.nextInt(inventory.getSize());
                if (inventory.getItem(slot) != null) continue;
                inventory.setItem(slot, randomLootItem());
            }
            chest.update(true, false);
        }
    }

    private WizardsSpell weightedSpell() {
        List<WizardsSpell> spells = WizardsSpell.implementedSpells();
        int total = spells.stream().mapToInt(WizardsSpell::lootWeight).sum();
        int choice = random.nextInt(Math.max(1, total));
        for (WizardsSpell spell : spells) {
            choice -= spell.lootWeight();
            if (choice < 0) return spell;
        }
        return WizardsSpell.MANA_BOLT;
    }

    private ItemStack randomLootItem() {
        return switch (random.nextInt(12)) {
            case 0 -> new ItemStack(Material.BREAD, 2 + random.nextInt(3));
            case 1 -> new ItemStack(Material.COOKED_BEEF, 1 + random.nextInt(2));
            case 2 -> new ItemStack(Material.COOKED_CHICKEN, 1 + random.nextInt(2));
            case 3 -> new ItemStack(Material.GOLDEN_CARROT, 1 + random.nextInt(2));
            case 4 -> new ItemStack(Material.WOODEN_PLANKS, 4 + random.nextInt(5));
            case 5 -> new ItemStack(Material.GOLD_INGOT, 1 + random.nextInt(2));
            case 6 -> new ItemStack(Material.IRON_INGOT, 1 + random.nextInt(2));
            case 7 -> new ItemStack(Material.DIAMOND);
            case 8 -> new ItemStack(Material.LEATHER_CHESTPLATE);
            case 9 -> new ItemStack(Material.LEATHER_BOOTS);
            case 10 -> new ItemStack(Material.GOLDEN_HELMET);
            default -> random.nextBoolean() ? makeWandLoot() : makeSoulStar();
        };
    }

    private boolean isSpellBook(ItemStack item) {
        return spellFromItem(item) != null;
    }

    private WizardsSpell spellFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(spellKey, PersistentDataType.STRING);
        return WizardsSpell.fromId(id);
    }

    private boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "wand".equals(item.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.STRING));
    }

    private boolean isLootWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "loot".equals(item.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.STRING));
    }

    private boolean isMenuBook(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(menuKey, PersistentDataType.BYTE);
    }

    private boolean isSoulStar(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(soulKey, PersistentDataType.BYTE);
    }

    private ChatColor categoryColour(WizardsSpell spell) {
        return switch (spell.category()) {
            case ATTACK -> ChatColor.RED;
            case SUPPORT -> ChatColor.GREEN;
            case MISC -> ChatColor.AQUA;
        };
    }

    private String prettyKit(WizardsKit kit) {
        return switch (kit) {
            case MAGE -> "Mage";
            case SORCERER -> "Sorcerer";
            case MYSTIC -> "Mystic";
            case WITCH_DOCTOR -> "Witch Doctor";
        };
    }

    private static final class SpectralArrowData {
        private final UUID owner;
        private final int level;
        private final Location origin;
        private Location lastPosition;
        private SpectralArrowData(UUID owner, int level, Location origin, Location lastPosition) {
            this.owner = owner; this.level = level; this.origin = origin; this.lastPosition = lastPosition;
        }
    }

    private static final class SummonedWolfData {
        private final UUID owner;
        private final long expiresAt;
        private SummonedWolfData(UUID owner, long expiresAt) { this.owner = owner; this.expiresAt = expiresAt; }
    }

    private final class WizardState {
        private final WizardsKit kit;
        private final float maximumMana;
        private final EnumMap<WizardsSpell, Integer> known = new EnumMap<>(WizardsSpell.class);
        private final EnumMap<WizardsSpell, Long> cooldowns = new EnumMap<>(WizardsSpell.class);
        private final WizardsSpell[] wands = new WizardsSpell[MAX_WAND_SLOTS];
        private float mana;
        private int wandCount;
        private int soulStars;
        private float cooldownModifier = 1.0F;
        private boolean alive = true;

        private WizardState(WizardsKit kit) {
            this.kit = kit;
            maximumMana = WizardsRules.maximumMana(kit);
            mana = WizardsRules.initialMana();
            wandCount = WizardsRules.startingWands(kit);
            known.put(WizardsSpell.MANA_BOLT, 1);
            known.put(WizardsSpell.WIZARDS_COMPASS, 1);
            // Reborn's Mage kit receives two random starter spell levels. Only
            // spells already implemented in this first modern milestone are rolled.
            if (kit == WizardsKit.MAGE) {
                List<WizardsSpell> candidates = new ArrayList<>(WizardsSpell.implementedSpells());
                candidates.remove(WizardsSpell.MANA_BOLT);
                candidates.remove(WizardsSpell.WIZARDS_COMPASS);
                java.util.Collections.shuffle(candidates, random);
                for (int i = 0; i < Math.min(2, candidates.size()); i++) known.put(candidates.get(i), 1);
            }
        }
    }

    private static final class TemporaryBlock {
        private final org.bukkit.block.data.BlockData originalData;
        private final Material placedMaterial;
        private TemporaryBlock(org.bukkit.block.data.BlockData originalData, Material placedMaterial) {
            this.originalData = originalData;
            this.placedMaterial = placedMaterial;
        }
    }

    private record PlayerSnapshot(Location location, ItemStack[] contents, ItemStack[] armour,
                                  GameMode gameMode, double health, int food, float saturation,
                                  int level, float exp, boolean allowFlight, boolean flying,
                                  float walkSpeed, float flySpeed, int fireTicks,
                                  List<PotionEffect> effects) {
        static PlayerSnapshot capture(Player player) {
            return new PlayerSnapshot(
                    player.getLocation().clone(),
                    cloneItems(player.getInventory().getContents()),
                    cloneItems(player.getInventory().getArmorContents()),
                    player.getGameMode(),
                    player.getHealth(),
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getLevel(),
                    player.getExp(),
                    player.getAllowFlight(),
                    player.isFlying(),
                    player.getWalkSpeed(),
                    player.getFlySpeed(),
                    player.getFireTicks(),
                    new ArrayList<>(player.getActivePotionEffects()));
        }

        void restore(Player player) {
            player.closeInventory();
            player.setGameMode(gameMode);
            player.getInventory().setContents(cloneItems(contents));
            player.getInventory().setArmorContents(cloneItems(armour));
            player.setFoodLevel(food);
            player.setSaturation(saturation);
            player.setLevel(level);
            player.setExp(exp);
            player.setFireTicks(fireTicks);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            for (PotionEffect effect : effects) player.addPotionEffect(effect);
            player.setWalkSpeed(walkSpeed);
            player.setAllowFlight(allowFlight);
            if (allowFlight) player.setFlying(flying);
            else player.setFlying(false);
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) == null
                    ? 20.0D : player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.max(1.0D, Math.min(health, maxHealth)));
            if (location != null && location.getWorld() != null) player.teleport(location);
        }

        private static ItemStack[] cloneItems(ItemStack[] source) {
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
            return copy;
        }
    }
}
