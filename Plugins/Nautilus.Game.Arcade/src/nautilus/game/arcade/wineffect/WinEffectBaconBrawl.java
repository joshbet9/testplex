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
import org.bukkit.util.Vector;

import mineplex.core.common.util.C;
import mineplex.core.common.util.LineFormat;
import mineplex.core.common.util.UtilShapes;
import mineplex.core.common.util.UtilText;
import mineplex.core.disguise.disguises.DisguisePlayer;
import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.types.WinEffectGadget;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;

/**
 * Bacon Brawl's game-specific winner celebration.
 *
 * It runs inside the normal Reborn winner room, using player-disguised armour
 * stands as visual stand-ins. The targets are not live players, so the effect
 * cannot damage, kill, or move real players.
 */
public class WinEffectBaconBrawl extends WinEffectGadget
{
    private static final int TARGET_COUNT = 4;
    private static final long FIRST_IMPACT_DELAY_MS = 800L;
    private static final long IMPACT_INTERVAL_MS = 850L;

    private DisguisePlayer _winnerNpc;
    private final List<DisguisePlayer> _targetDisguises = new ArrayList<>();
    private final List<ArmorStand> _targets = new ArrayList<>();
    private int _nextTarget;
    private long _nextImpact;
    private boolean _cleaned;

    public WinEffectBaconBrawl(GadgetManager manager)
    {
        super(manager, "Arcade Bacon Brawl Body Slam",
                UtilText.splitLineToArray(C.cGray + "Body-slam four opponents off a bare-dirt arena inspired by Mighty Lava Arena.",
                        LineFormat.LORE),
                -2, Material.IRON_AXE, (byte) 0, true);

        // Keep WinEffectGadget's generic WinRoomPodium schematic. Bacon Brawl
        // decorates its floor with a small, bare-dirt Mighty Lava Arena-inspired
        // fighting area; it does not replace the generic winner room.
        _length = TimeUnit.SECONDS.toMillis(8);
    }

    @Override
    public void buildWinnerRoom()
    {
        super.buildWinnerRoom();

        Location base = getBaseLocation();
        int centerX = base.getBlockX();
        int centerZ = base.getBlockZ();
        int floorY = base.getBlockY() - 1;

        // Mighty Lava Arena's visual language for this effect is deliberately
        // just dirt blocks: no grass layer, fences, lava, or extra decoration.
        for (int x = -6; x <= 6; x++)
        {
            for (int z = -6; z <= 6; z++)
            {
                base.getWorld().getBlockAt(centerX + x, floorY, centerZ + z).setType(Material.DIRT, false);
            }
        }
    }

    @Override
    public void play()
    {
        _cleaned = false;
        _nextTarget = 0;
        _targets.clear();
        _targetDisguises.clear();

        _winnerNpc = getNPC(_player, getBaseLocation());

        List<Location> positions = UtilShapes.getPointsInCircle(getBaseLocation(), TARGET_COUNT, 3.1);
        List<Player> skins = new ArrayList<>();
        if (_other != null)
        {
            for (Player player : _other)
            {
                if (player != null && player.isOnline() && !skins.contains(player))
                {
                    skins.add(player);
                }
            }
        }
        if (skins.isEmpty())
        {
            skins.add(_player);
        }

        for (int i = 0; i < TARGET_COUNT; i++)
        {
            Location loc = positions.get(i).clone();
            loc.setDirection(getBaseLocation().toVector().subtract(loc.toVector()));

            DisguisePlayer disguise = getNPC(skins.get(i % skins.size()), loc, true);
            ArmorStand stand = (ArmorStand) disguise.getEntity().getBukkitEntity();
            stand.setGravity(true);
            stand.setVelocity(new Vector(0, 0, 0));
            _targetDisguises.add(disguise);
            _targets.add(stand);
        }

        _nextImpact = System.currentTimeMillis() + FIRST_IMPACT_DELAY_MS;
    }

    @EventHandler
    public void onUpdate(UpdateEvent event)
    {
        if (event.getType() != UpdateType.TICK || _cleaned || !isRunning())
        {
            return;
        }

        if (_nextTarget >= _targets.size() || System.currentTimeMillis() < _nextImpact)
        {
            return;
        }

        ArmorStand target = _targets.get(_nextTarget);
        if (target == null || target.isDead() || !target.isValid())
        {
            _nextTarget++;
            _nextImpact = System.currentTimeMillis() + IMPACT_INTERVAL_MS;
            return;
        }

        Entity winnerEntity = _winnerNpc == null ? null : _winnerNpc.getEntity().getBukkitEntity();
        if (winnerEntity != null && winnerEntity.isValid())
        {
            Location winnerLoc = target.getLocation().clone();
            Vector chargeDirection = target.getLocation().toVector().subtract(getBaseLocation().toVector());
            if (chargeDirection.lengthSquared() < 0.01)
            {
                chargeDirection = new Vector(1, 0, 0);
            }
            chargeDirection.setY(0).normalize();
            winnerLoc.subtract(chargeDirection.clone().multiply(1.0));
            winnerLoc.setDirection(target.getLocation().toVector().subtract(winnerLoc.toVector()));
            winnerEntity.teleport(winnerLoc);
            _winnerNpc.sendHit();
        }

        Vector flight = target.getLocation().toVector().subtract(getBaseLocation().toVector());
        flight.setY(0);
        if (flight.lengthSquared() < 0.01)
        {
            flight = new Vector(1, 0, 0);
        }
        flight.normalize().multiply(1.55).setY(0.82);
        target.setVelocity(flight);
        target.getWorld().playSound(target.getLocation(), Sound.VILLAGER_HIT, 1.6F, 0.8F);
        target.getWorld().playSound(target.getLocation(), Sound.EXPLODE, 0.7F, 1.5F);

        _nextTarget++;
        _nextImpact = System.currentTimeMillis() + IMPACT_INTERVAL_MS;
    }

    @Override
    public void finish()
    {
        if (_cleaned)
        {
            return;
        }
        _cleaned = true;

        for (DisguisePlayer disguise : _targetDisguises)
        {
            removeDisguisedStand(disguise);
        }
        _targetDisguises.clear();
        _targets.clear();

        removeDisguisedStand(_winnerNpc);
        _winnerNpc = null;
    }

    private void removeDisguisedStand(DisguisePlayer disguise)
    {
        if (disguise == null)
        {
            return;
        }

        Entity entity = disguise.getEntity() == null ? null : disguise.getEntity().getBukkitEntity();
        Manager.getDisguiseManager().undisguise(disguise);
        if (entity != null && !entity.isDead())
        {
            entity.remove();
        }
    }
}
