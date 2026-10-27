package nautilus.game.arcade.wineffect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import mineplex.core.common.util.C;
import mineplex.core.common.util.LineFormat;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import mineplex.core.common.util.UtilShapes;
import mineplex.core.common.util.UtilText;
import mineplex.core.disguise.disguises.DisguisePlayer;
import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.types.WinEffectGadget;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;

/**
 * Bomb Lobbers' selected round-end effect.
 * Reuses the generic winner-room schematic and stages three player-disguised
 * targets with a Turtles-inspired prismarine rim. No live player is hit.
 */
public class WinEffectBombLobbers extends WinEffectGadget
{
    private static final int TARGET_COUNT = 3;
    private static final long SHOT_FLIGHT_MS = 600L;
    private static final long SHOT_GAP_MS = 700L;

    private DisguisePlayer _winnerNpc;
    private final List<DisguisePlayer> _targetDisguises = new ArrayList<DisguisePlayer>();
    private final List<ArmorStand> _targets = new ArrayList<ArmorStand>();
    private ArmorStand _bomb;
    private Location _shotStart;
    private Location _shotEnd;
    private long _shotStartedAt;
    private long _nextShotAt;
    private int _nextTarget;
    private boolean _cleaned;

    public WinEffectBombLobbers(GadgetManager manager)
    {
        super(manager, "Arcade Bomb Lobbers Triple Lob",
                UtilText.splitLineToArray(C.cGray
                        + "Lob three TNT shots into three player targets, with a Turtles map-inspired winner-room accent.",
                        LineFormat.LORE),
                -2, Material.TNT, (byte) 0, true);
        setHidden(true);
        _length = TimeUnit.SECONDS.toMillis(7);
        // Keep the default WinRoomPodium schematic.
    }

    @Override
    public void buildWinnerRoom()
    {
        super.buildWinnerRoom();
        Location base = getBaseLocation();
        int floorY = base.getBlockY() - 1;
        int centerX = base.getBlockX();
        int centerZ = base.getBlockZ();
        for (int i = 0; i < 20; i++)
        {
            double angle = Math.PI * 2 * i / 20.0;
            int x = centerX + (int) Math.round(Math.cos(angle) * 4.7);
            int z = centerZ + (int) Math.round(Math.sin(angle) * 4.7);
            org.bukkit.block.Block block = base.getWorld().getBlockAt(x, floorY, z);
            if (i % 5 == 0) block.setType(Material.SEA_LANTERN, false);
            else
            {
                block.setType(Material.PRISMARINE, false);
                block.setData((byte) (i % 3));
            }
        }
    }

    @Override
    public void play()
    {
        _cleaned = false;
        _nextTarget = 0;
        _bomb = null;
        _targets.clear();
        _targetDisguises.clear();
        _winnerNpc = getNPC(_player, getBaseLocation());

        List<Location> positions = UtilShapes.getPointsInCircle(getBaseLocation(), TARGET_COUNT, 3.15);
        List<Player> skins = new ArrayList<Player>();
        if (_other != null)
        {
            for (Player player : _other)
            {
                if (player != null && player.isOnline() && !skins.contains(player)) skins.add(player);
            }
        }
        if (skins.isEmpty()) skins.add(_player);
        for (int i = 0; i < TARGET_COUNT; i++)
        {
            Location loc = positions.get(i).clone();
            loc.setDirection(getBaseLocation().toVector().subtract(loc.toVector()));
            DisguisePlayer disguise = getNPC(skins.get(i % skins.size()), loc, false);
            ArmorStand stand = (ArmorStand) disguise.getEntity().getBukkitEntity();
            stand.setGravity(false);
            stand.setVelocity(new Vector(0, 0, 0));
            _targetDisguises.add(disguise);
            _targets.add(stand);
        }
        _nextShotAt = System.currentTimeMillis() + 550L;
    }

    @EventHandler
    public void animateShots(UpdateEvent event)
    {
        if (event.getType() != UpdateType.TICK || _cleaned || !isRunning()) return;
        long now = System.currentTimeMillis();
        if (_bomb != null)
        {
            double progress = Math.min(1.0, (now - _shotStartedAt) / (double) SHOT_FLIGHT_MS);
            Location current = _shotStart.clone().add(
                    (_shotEnd.getX() - _shotStart.getX()) * progress,
                    (_shotEnd.getY() - _shotStart.getY()) * progress + Math.sin(progress * Math.PI) * 1.1,
                    (_shotEnd.getZ() - _shotStart.getZ()) * progress);
            _bomb.teleport(current);
            UtilParticle.PlayParticle(ParticleType.SMOKE, current, 0.08F, 0.08F, 0.08F, 0.01F, 2, ViewDist.NORMAL);
            if (progress >= 1.0) impactCurrentTarget(now);
            return;
        }
        if (_nextTarget >= _targets.size() || now < _nextShotAt) return;
        startNextShot(now);
    }

    private void startNextShot(long now)
    {
        ArmorStand target = _targets.get(_nextTarget);
        if (target == null || target.isDead() || !target.isValid())
        {
            _nextTarget++;
            _nextShotAt = now + SHOT_GAP_MS;
            return;
        }
        Entity winnerEntity = _winnerNpc == null ? null : _winnerNpc.getEntity().getBukkitEntity();
        if (winnerEntity == null || !winnerEntity.isValid())
        {
            _nextTarget = _targets.size();
            return;
        }
        _shotStart = winnerEntity.getLocation().clone().add(0, 0.9, 0);
        _shotEnd = target.getLocation().clone().add(0, 0.9, 0);
        _shotStartedAt = now;
        _bomb = _player.getWorld().spawn(_shotStart.clone().subtract(0, 0.35, 0), ArmorStand.class);
        _bomb.setVisible(false);
        _bomb.setSmall(true);
        _bomb.setGravity(false);
        _bomb.setBasePlate(false);
        _bomb.setArms(false);
        _bomb.setMarker(true);
        _bomb.setCanPickupItems(false);
        _bomb.setHelmet(new ItemStack(Material.TNT));
        _player.getWorld().playSound(_shotStart, Sound.CLICK, 0.75F, 0.8F);
    }

    private void impactCurrentTarget(long now)
    {
        ArmorStand target = _targets.get(_nextTarget);
        Location impact = target.getLocation().clone().add(0, 0.9, 0);
        if (_bomb != null && !_bomb.isDead()) _bomb.remove();
        _bomb = null;
        Entity winnerEntity = _winnerNpc == null ? null : _winnerNpc.getEntity().getBukkitEntity();
        if (winnerEntity != null && winnerEntity.isValid())
        {
            Location winnerLoc = winnerEntity.getLocation().clone();
            winnerLoc.setDirection(target.getLocation().toVector().subtract(winnerLoc.toVector()));
            winnerEntity.teleport(winnerLoc);
            _winnerNpc.sendHit();
        }
        Vector flight = target.getLocation().toVector().subtract(getBaseLocation().toVector());
        flight.setY(0);
        if (flight.lengthSquared() < 0.01) flight = new Vector(1, 0, 0);
        flight.normalize().multiply(1.25).setY(0.82);
        target.setGravity(true);
        target.setVelocity(flight);
        impact.getWorld().playSound(impact, Sound.EXPLODE, 1.3F, 0.85F);
        UtilParticle.PlayParticle(ParticleType.HUGE_EXPLOSION, impact, 0.1F, 0.1F, 0.1F, 0.01F, 1, ViewDist.NORMAL);
        UtilParticle.PlayParticle(ParticleType.SMOKE, impact, 0.45F, 0.4F, 0.45F, 0.03F, 15, ViewDist.NORMAL);
        _nextTarget++;
        _nextShotAt = now + SHOT_GAP_MS;
    }

    @Override
    public void finish()
    {
        if (_cleaned) return;
        _cleaned = true;
        if (_bomb != null && !_bomb.isDead()) _bomb.remove();
        _bomb = null;
        for (DisguisePlayer disguise : _targetDisguises) removeDisguisedStand(disguise);
        _targetDisguises.clear();
        _targets.clear();
        removeDisguisedStand(_winnerNpc);
        _winnerNpc = null;
    }

    private void removeDisguisedStand(DisguisePlayer disguise)
    {
        if (disguise == null) return;
        Entity entity = disguise.getEntity() == null ? null : disguise.getEntity().getBukkitEntity();
        Manager.getDisguiseManager().undisguise(disguise);
        if (entity != null && !entity.isDead()) entity.remove();
    }
}
