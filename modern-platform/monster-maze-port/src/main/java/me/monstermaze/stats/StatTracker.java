package me.monstermaze.stats;

import me.monstermaze.event.AbilityUseEvent;
import me.monstermaze.event.FirstToSafepadEvent;
import me.monstermaze.event.MonsterBumpPlayerEvent;
import me.monstermaze.event.SafepadBuildEvent;
import me.monstermaze.game.GameManager;
import me.monstermaze.MonsterMazePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StatTracker implements Listener {
    private final GameManager game;

    /** How long after a bump the knock is still "directly knocking the player" toward a pad. */
    private static final long PILOT_WINDOW_MS = 2000L;

    private final Set<UUID> hitByMonster = new HashSet<UUID>();   // Ninja = never hit
    private final Set<UUID> usedAbility = new HashSet<UUID>();    // Hard Mode = never used ability
    private final Map<UUID, Integer> firstPads = new HashMap<UUID, Integer>(); // Speed
    private final Set<UUID> firstPadOnStageOne = new HashSet<UUID>();
    private final Set<UUID> spentJumpsOnStageOne = new HashSet<UUID>();
    private final Set<UUID> pilot = new HashSet<UUID>();          // land on pad within bump knock window
    private final Map<UUID, Long> bumpedAt = new HashMap<UUID, Long>(); // UUID -> bump time (ms)
    private boolean toughGiven = false;

    public StatTracker(GameManager game, MonsterMazePlugin plugin) {
        this.game = game;
        Bukkit.getPluginManager().registerEvents(this, plugin.getHostPlugin());
    }

    public void reset() {
        hitByMonster.clear();
        usedAbility.clear();
        firstPads.clear();
        firstPadOnStageOne.clear();
        spentJumpsOnStageOne.clear();
        pilot.clear();
        bumpedAt.clear();
        toughGiven = false;
    }

    @EventHandler
    public void onBump(MonsterBumpPlayerEvent e) {
        Player player = e.getPlayer();
        UUID id = player.getUniqueId();
        hitByMonster.add(id);

        // Only mark this bump as a "knock in progress" if the player was NOT already standing on a
        // safe pad. We timestamp it: Pilot only counts if they land ON a pad within the short knock
        // window (direct result of the blow), not if they wandered onto a pad later in the round.
        if (!game.isOnAnyPad(player)) {
            bumpedAt.put(id, System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onAbility(AbilityUseEvent e) {
        Player player = e.getPlayer();
        usedAbility.add(player.getUniqueId());
        if (game.getStage() == 1 && game.getKitManager().getKit(player)
                == me.monstermaze.kit.KitType.JUMPER) {
            org.bukkit.inventory.ItemStack jumps = player.getInventory().getItem(8);
            if (jumps == null || jumps.getType() != org.bukkit.Material.FEATHER || jumps.getAmount() <= 0) {
                spentJumpsOnStageOne.add(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onFirst(FirstToSafepadEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        game.noteFirstPad(e.getPlayer());
        Integer n = firstPads.get(id);
        firstPads.put(id, n == null ? 1 : n + 1);
        game.recordFirstSafePad(id);
        if (game.getStage() == 1) firstPadOnStageOne.add(id);
        double closestRival = Double.POSITIVE_INFINITY;
        for (Player other : game.getAlivePlayers()) {
            if (other.equals(e.getPlayer())) continue;
            closestRival = Math.min(closestRival, other.getLocation().distance(e.getPlayer().getLocation()));
        }
        if (Double.isFinite(closestRival) && closestRival > 30.0D) game.recordAchievement(id, "breakaway");
    }

    @EventHandler
    public void onPadBuild(SafepadBuildEvent e) {
        if (toughGiven) return;
        if (game.getStage() > 10) {
            toughGiven = true;
            for (Player p : game.getAlivePlayers()) {
                p.sendMessage(ChatColor.LIGHT_PURPLE + "Stat: Tough Competition (survived past pad 10)");
            }
        }
    }

    public void checkPilotLand(Player player, boolean onPad) {
        if (!onPad) return;
        UUID id = player.getUniqueId();

        // Only award Pilot if the player lands ON a pad while the bump's knock is still in progress
        // (within the short flight window) - i.e. they were knocked directly onto the pad. An old
        // bump from earlier in the round must NOT count.
        Long t = bumpedAt.get(id);
        if (t == null) return;
        bumpedAt.remove(id);
        if (System.currentTimeMillis() - t > PILOT_WINDOW_MS) {
            return;
        }
        if (!pilot.contains(id)) {
            pilot.add(id);
            player.sendMessage(ChatColor.AQUA + "Stat: Pilot (landed on pad after bump)");
        }
    }

    public void clearBump(Player player) {
        bumpedAt.remove(player.getUniqueId());
    }

    public void announceWinners(Player winner) {
        if (winner == null) return;
        UUID id = winner.getUniqueId();
        if (game.getKitManager().getKit(winner) == me.monstermaze.kit.KitType.BODY_BUILDER
                && !firstPads.containsKey(id)) game.recordAchievement(id, "blackout");
        if (firstPads.getOrDefault(id, 0) >= game.getStage()) game.recordAchievement(id, "perfect-run");
        if (firstPadOnStageOne.contains(id) && spentJumpsOnStageOne.contains(id)) {
            game.recordAchievement(id, "jumpman");
        }
        if (!hitByMonster.contains(id))
            winner.sendMessage(ChatColor.GREEN + "Stat: Ninja (never hit by a monster)");
        if (!usedAbility.contains(id))
            winner.sendMessage(ChatColor.GREEN + "Stat: Hard Mode (never used an ability)");
        Integer fp = firstPads.get(id);
        if (fp != null && fp > 0)
            winner.sendMessage(ChatColor.GREEN + "Stat: Speed (first to pad x" + fp + ")");
        if (pilot.contains(id))
            winner.sendMessage(ChatColor.GREEN + "Stat: Pilot");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        hitByMonster.remove(id);
        usedAbility.remove(id);
        bumpedAt.remove(id);
        pilot.remove(id);
    }
}
