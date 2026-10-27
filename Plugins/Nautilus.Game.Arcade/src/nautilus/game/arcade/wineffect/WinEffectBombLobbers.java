package nautilus.game.arcade.wineffect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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
 * Builds a standalone miniature Turtles island with a prismarine shell and ocean moat.
 * Three player-disguised targets are knocked away by animated TNT; no live player is hit.
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
        super(manager, "Arcade Bomb Lobbers - Turtles Demo",
                UtilText.splitLineToArray(C.cGray
                        + "Lob three TNT shots into three player targets, with a Turtles map-inspired winner-room accent.",
                        LineFormat.LORE),
                -2, Material.TNT, (byte) 0, true);
        _length = TimeUnit.SECONDS.toMillis(7);
        // Keep the default WinRoomPodium schematic.
    }

    @Override
    public void buildWinnerRoom()
    {
        Location base = getBaseLocation();
        WinEffectRoomUtil.clear(base, 24, 3, 18);
        World world = base.getWorld();
        int bx = base.getBlockX(), bz = base.getBlockZ(), y = base.getBlockY() - 1;

        // A sandy island surrounded by a stationary-water ocean.
        for (int x = -15; x <= 15; x++)
            for (int z = -15; z <= 15; z++)
            {
                if (x * x + z * z > 225) continue;
                world.getBlockAt(bx + x, y - 1, bz + z).setType(Material.SANDSTONE, false);
                world.getBlockAt(bx + x, y, bz + z).setType(Material.STATIONARY_WATER, false);
                if (x * x + z * z <= 100)
                    world.getBlockAt(bx + x, y, bz + z).setType((x * x + z * z > 80) ? Material.SAND : Material.SANDSTONE, false);
            }

        // Four green legs and a small head frame the central turtle.
        for (int sx : new int[]{-1, 1})
            for (int sz : new int[]{-1, 1})
            {
                int lx = bx + sx * 7, lz = bz + sz * 5;
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                    {
                        org.bukkit.block.Block leg = world.getBlockAt(lx + dx, y + 1, lz + dz);
                        leg.setType(Material.STAINED_CLAY, false);
                        leg.setData((byte) 5);
                    }
            }
        int hx = bx, hz = bz - 9;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -2; dz <= 1; dz++)
            {
                org.bukkit.block.Block head = world.getBlockAt(hx + dx, y + 1, hz + dz);
                head.setType(Material.STAINED_CLAY, false);
                head.setData((byte) 5);
            }
        world.getBlockAt(hx - 1, y + 2, hz - 1).setType(Material.SEA_LANTERN, false);
        world.getBlockAt(hx + 1, y + 2, hz - 1).setType(Material.SEA_LANTERN, false);

        // A domed, segmented prismarine shell.
        for (int dx = -5; dx <= 5; dx++)
            for (int dz = -4; dz <= 4; dz++)
            {
                double norm = (dx * dx / 25.0) + (dz * dz / 16.0);
                if (norm > 1.0) continue;
                int height = norm < 0.18 ? 3 : norm < 0.48 ? 2 : norm < 0.78 ? 1 : 0;
                for (int h = 0; h <= height; h++)
                {
                    org.bukkit.block.Block shell = world.getBlockAt(bx + dx, y + 1 + h, bz + dz);
                    shell.setType((h == height && (dx == 0 || dz == 0)) ? Material.SEA_LANTERN : Material.PRISMARINE, false);
                    if (shell.getType() == Material.PRISMARINE)
                        shell.setData((byte) ((Math.abs(dx) + Math.abs(dz)) % 3));
                }
            }
    }

    @Override
    public void teleport()
    {
        Location view = getBaseLocation().clone().add(0, 3, 20);
        view.setDirection(getBaseLocation().clone().subtract(view).toVector());
        teleport(view);
    }

    @Override
    public void play()
    {
        _cleaned = false;
        _nextTarget = 0;
        _bomb = null;
        _targets.clear();
        _targetDisguises.clear();
        _winnerNpc = getNPC(_player, getBaseLocation().clone().add(0, 0, 8));

        List<Location> positions = UtilShapes.getPointsInCircle(getBaseLocation(), TARGET_COUNT, 7.2);
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
