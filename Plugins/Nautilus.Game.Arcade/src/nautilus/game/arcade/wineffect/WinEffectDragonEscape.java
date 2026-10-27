package nautilus.game.arcade.wineffect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import mineplex.core.common.util.C;
import mineplex.core.common.util.LineFormat;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import mineplex.core.common.util.UtilText;
import mineplex.core.disguise.disguises.DisguisePlayer;
import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.types.WinEffectGadget;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;

/** A full Skylands-inspired floating-island race, separate from the stock podium room. */
public class WinEffectDragonEscape extends WinEffectGadget
{
    private static final long WINNER_JUMP_MS = 650L;
    private static final long OPPONENT_RUN_MS = 2600L;
    private final List<Location> _platforms = new ArrayList<Location>();
    private DisguisePlayer _winner, _opponentOne, _opponentTwo;
    private Location _winnerJumpStart;
    private long _winnerJumpStarted, _raceStarted;
    private int _winnerJump;
    private boolean _finished, _cleaned;

    public WinEffectDragonEscape(GadgetManager manager)
    {
        super(manager, "Arcade Dragon Escape - Skylands Finale",
                UtilText.splitLineToArray(C.cGray + "Start in third place, leap past two runners, and reach the Skylands finish first.", LineFormat.LORE),
                -2, Material.DRAGON_EGG, (byte) 0, true);
        _length = TimeUnit.SECONDS.toMillis(8);
    }

    @Override
    public void buildWinnerRoom()
    {
        Location base = getBaseLocation();
        WinEffectRoomUtil.clear(base, 25, 3, 20);
        World world = base.getWorld();
        int bx = base.getBlockX(), bz = base.getBlockZ();
        int[] xs = {-14, -8, -2, 4, 10, 16};
        int[] hs = {0, 1, 0, 1, 2, 3};
        _platforms.clear();
        for (int i = 0; i < xs.length; i++)
        {
            int x = bx + xs[i];
            int platformY = base.getBlockY() - 1 + hs[i];
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                {
                    world.getBlockAt(x + dx, platformY, bz + dz).setType(Material.GRASS, false);
                    world.getBlockAt(x + dx, platformY - 1, bz + dz).setType(Material.STONE, false);
                    if (Math.abs(dx) + Math.abs(dz) <= 3)
                        world.getBlockAt(x + dx, platformY - 2, bz + dz).setType(Material.COBBLESTONE, false);
                }
            world.getBlockAt(x, platformY, bz).setType(Material.GLOWSTONE, false);
            _platforms.add(new Location(world, x + 0.5, base.getBlockY() + hs[i], bz + 0.5));
        }

        // Gold-and-quartz finish arch on the final floating island.
        int finishX = bx + 16, floor = base.getBlockY() + 3;
        for (int side : new int[]{-2, 2})
            for (int h = 0; h <= 3; h++)
                world.getBlockAt(finishX + side, floor + h, bz).setType(Material.QUARTZ_BLOCK, false);
        for (int dx = -2; dx <= 2; dx++)
            world.getBlockAt(finishX + dx, floor + 4, bz).setType(Material.GOLD_BLOCK, false);
        for (int dx = -1; dx <= 1; dx++)
        {
            org.bukkit.block.Block goal = world.getBlockAt(finishX + dx, floor + 1, bz);
            goal.setType(Material.STAINED_GLASS, false);
            goal.setData((byte) 3);
        }
    }

    @Override
    public void teleport()
    {
        Location view = getBaseLocation().clone().add(0, 4, 23);
        view.setDirection(getBaseLocation().clone().subtract(view).toVector());
        teleport(view);
    }

    @Override
    public void play()
    {
        _cleaned = false;
        _finished = false;
        _winnerJump = 0;
        _raceStarted = System.currentTimeMillis();
        List<org.bukkit.entity.Player> skins = new ArrayList<org.bukkit.entity.Player>();
        if (_other != null)
            for (org.bukkit.entity.Player player : _other)
                if (player != null && player.isOnline() && !skins.contains(player)) skins.add(player);
        while (skins.size() < 2) skins.add(_player);

        Location third = _platforms.get(2).clone();
        _winner = getNPC(_player, third, false);
        _opponentOne = getNPC(skins.get(0), _platforms.get(3).clone().add(0, 0, 1.35), false);
        _opponentTwo = getNPC(skins.get(1), _platforms.get(4).clone().add(0, 0, -1.35), false);
        _winnerJumpStart = third.clone();
        _winnerJumpStarted = _raceStarted;
    }

    @EventHandler
    public void animateRace(UpdateEvent event)
    {
        if (event.getType() != UpdateType.TICK || !isRunning() || _cleaned) return;
        long now = System.currentTimeMillis();
        animateOpponent(_opponentOne, _platforms.get(3).clone().add(0, 0, 1.35),
                _platforms.get(5).clone().add(0, 0, 1.35), now, 200L);
        animateOpponent(_opponentTwo, _platforms.get(4).clone().add(0, 0, -1.35),
                _platforms.get(5).clone().add(0, 0, -1.35), now, 900L);
        if (_finished || _winner == null) return;
        Entity entity = _winner.getEntity() == null ? null : _winner.getEntity().getBukkitEntity();
        if (entity == null || !entity.isValid()) return;
        Location target = _winnerJump == 0 ? _platforms.get(3).clone() : _platforms.get(5).clone();
        double progress = Math.min(1.0, (now - _winnerJumpStarted) / (double) WINNER_JUMP_MS);
        Location current = _winnerJumpStart.clone().add(
                (target.getX() - _winnerJumpStart.getX()) * progress,
                (target.getY() - _winnerJumpStart.getY()) * progress + Math.sin(progress * Math.PI) * 2.0,
                (target.getZ() - _winnerJumpStart.getZ()) * progress);
        entity.teleport(current);
        UtilParticle.PlayParticle(ParticleType.CLOUD, current, 0.15F, 0.1F, 0.15F, 0.01F, 4, ViewDist.NORMAL);
        if (progress >= 1.0)
        {
            if (_winnerJump == 0)
            {
                _winnerJump = 1;
                _winnerJumpStart = target.clone();
                _winnerJumpStarted = now;
            }
            else
            {
                _finished = true;
                entity.getWorld().playSound(target, Sound.LEVEL_UP, 1.5F, 1.0F);
                entity.getWorld().playSound(target, Sound.FIREWORK_BLAST, 1.0F, 1.2F);
                UtilParticle.PlayParticle(ParticleType.FIREWORKS_SPARK, target, 0.8F, 0.8F, 0.8F, 0.08F, 45, ViewDist.NORMAL);
                UtilParticle.PlayParticle(ParticleType.CLOUD, target, 0.5F, 0.35F, 0.5F, 0.05F, 22, ViewDist.NORMAL);
            }
        }
    }

    private void animateOpponent(DisguisePlayer disguise, Location start, Location end, long now, long delay)
    {
        if (disguise == null || disguise.getEntity() == null) return;
        Entity entity = disguise.getEntity().getBukkitEntity();
        if (entity == null || !entity.isValid()) return;
        double progress = Math.max(0.0, Math.min(1.0, (now - _raceStarted - delay) / (double) OPPONENT_RUN_MS));
        Location current = start.clone().add(
                (end.getX() - start.getX()) * progress,
                (end.getY() - start.getY()) * progress + Math.sin(progress * Math.PI) * 0.8,
                (end.getZ() - start.getZ()) * progress);
        entity.teleport(current);
    }

    @Override
    public void finish()
    {
        if (_cleaned) return;
        _cleaned = true;
        removeNpc(_winner);
        removeNpc(_opponentOne);
        removeNpc(_opponentTwo);
        _winner = null;
        _opponentOne = null;
        _opponentTwo = null;
        _platforms.clear();
    }

    private void removeNpc(DisguisePlayer disguise)
    {
        if (disguise == null) return;
        Entity entity = disguise.getEntity() == null ? null : disguise.getEntity().getBukkitEntity();
        Manager.getDisguiseManager().undisguise(disguise);
        if (entity != null && !entity.isDead()) entity.remove();
    }
}
