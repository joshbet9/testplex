package nautilus.game.arcade.game.games.monstermaze;

import java.util.ArrayList;
import java.util.Iterator;

import mineplex.core.common.util.MapUtil;
import mineplex.core.common.util.UtilPlayer;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SafePad implements Listener
{
	private MonsterMaze Host;
	private Maze _maze;
	
	private Location  _loc;
	private byte _surfaceColour = (byte) 5;
	private byte _breakColour = (byte) 5;
	
	private int _decayCount = 11;
	
	private ArrayList<SafePadBlock> _blocks = new ArrayList<SafePadBlock>();
	
	public SafePad(MonsterMaze host, Maze maze, Location loc)
	{
		Host = host;
		_maze = maze;
		
		_loc = loc;
		Bukkit.getPluginManager().registerEvents(this, Host.Manager.getPlugin());

		// Optional cosmetic tint; stock green (data 5) remains the default.
		if (Host.Manager.getPlugin().getConfig().getBoolean("monstermaze.safe-pad-enhancements.surface-colour-enabled", false))
		{
			int configuredColour = Host.Manager.getPlugin().getConfig().getInt("monstermaze.safe-pad-enhancements.surface-colour-data", 5);
			if (configuredColour >= 0 && configuredColour <= 15)
				_surfaceColour = (byte) configuredColour;
		}
		_breakColour = _surfaceColour;
		
		// Walkable safe-pad surface. The centre glass tints the beacon beam per viewer.
		for (int x = -2 ; x < 3 ; x++)
		{
			for (int z = -2 ; z < 3 ; z++)
			{
				if (x == 0 && z == 0)
					_blocks.add(new SafePadBlock(loc.clone().add(0, 0, 0), Material.GLASS, (byte) 0));
				else
					_blocks.add(new SafePadBlock(loc.clone().add(x, 0, z), Material.STAINED_CLAY, _surfaceColour));
			}
		}

		// Beacon sits one block below the walkable glass surface.
		_blocks.add(new SafePadBlock(loc.clone().add(0, -1, 0), Material.BEACON, (byte)0));

		// Its iron base sits another block below the beacon.
		for (int x = -1 ; x < 2 ; x++)
		{
			for (int z = -1 ; z < 2 ; z++)
				_blocks.add(new SafePadBlock(loc.clone().add(x, -2, z), Material.IRON_BLOCK, (byte)0));

			// Stairs
			_blocks.add(new SafePadBlock(loc.clone().add(x, -2, 2), Material.QUARTZ_STAIRS, (byte) 7));
			_blocks.add(new SafePadBlock(loc.clone().add(x, -2, -2), Material.QUARTZ_STAIRS, (byte) 6));
			_blocks.add(new SafePadBlock(loc.clone().add(2, -2, x), Material.QUARTZ_STAIRS, (byte) 5));
			_blocks.add(new SafePadBlock(loc.clone().add(-2, -2, x), Material.QUARTZ_STAIRS, (byte) 4));
		}

		// Corner blocks
		_blocks.add(new SafePadBlock(loc.clone().add(2, -2, 2), Material.QUARTZ_BLOCK, (byte) 1));
		_blocks.add(new SafePadBlock(loc.clone().add(-2, -2, 2), Material.QUARTZ_BLOCK, (byte) 1));
		_blocks.add(new SafePadBlock(loc.clone().add(2, -2, -2), Material.QUARTZ_BLOCK, (byte) 1));
		_blocks.add(new SafePadBlock(loc.clone().add(-2, -2, -2), Material.QUARTZ_BLOCK, (byte) 1));

		// air slabs
		for (int x = -2 ; x < 3 ; x++)
		{
			for (int z = -2 ; z < 3 ; z++)
			{
				_blocks.add(new SafePadBlock(loc.clone().add(x, -3, z), Material.AIR, (byte) 0));
			}
		}
	}
	
	public class SafePadBlock
	{
		private Material _origM;
		private byte _origD;
		
		private Material _tempMat;
		private byte _tempData;
		
		Location _loc;
		
		@SuppressWarnings("deprecation")
		public SafePadBlock(Location loc, Material newMat, byte newData)
		{
			_origM = loc.getBlock().getType();
			_origD = loc.getBlock().getData();
			
			_tempMat = newMat;
			_tempData = newData;
			
			_loc = loc;
		}
		
		@SuppressWarnings("deprecation")
		public void setMaterial(Material m)
		{
			_tempMat = m;
			MapUtil.QuickChangeBlockAt(_loc, _tempMat.getId(), _tempData);
		}
		
		@SuppressWarnings("deprecation")
		public void setData(byte b) // for glass changing
		{
			_tempData = b;
			MapUtil.QuickChangeBlockAt(_loc, _tempMat.getId(), _tempData);
		}
		
		public Material getBlockMaterial()
		{
			return _loc.getBlock().getType();
		}
		
		@SuppressWarnings("deprecation")
		public byte getBlockData()
		{
			return _loc.getBlock().getData();
		}
		
		public Location getLocation()
		{
			return _loc;
		}
		
		@SuppressWarnings("deprecation")
		public void build()
		{
			MapUtil.QuickChangeBlockAt(_loc, _tempMat.getId(), _tempData);
//			_loc.getWorld().playEffect(_loc, Effect.STEP_SOUND, _loc.getBlock().getTypeId());
		}
		
		@SuppressWarnings("deprecation")
		public void restore()
		{
			MapUtil.QuickChangeBlockAt(_loc, _origM.getId(), _origD);
//			_loc.getWorld().playEffect(_loc, Effect.STEP_SOUND, _loc.getBlock().getTypeId());
		}
	}
	
	public void build()
	{
		for (SafePadBlock spb : _blocks)
			spb.build();
		applyViewerCosmeticColours();
	}
	
	private void setBreakData(byte newData)
	{
		_breakColour = newData;
		for (SafePadBlock spb : _blocks)
		{
			if (spb.getBlockMaterial() != Material.STAINED_CLAY)
				continue;
			spb.setData(newData);
		}
		applyViewerCosmeticColours();
	}
	
	/**
	 * Repairs only the center objective beacon. Surrounding stained clay, stairs,
	 * base blocks and the pad's decay state are deliberately left untouched.
	 */
	@SuppressWarnings("deprecation")
	public boolean restoreObjectiveBeaconIfMissing()
	{
		if (_loc == null || _loc.getWorld() == null)
			return false;

		Location surface = _loc.clone();
		Location beacon = _loc.clone().subtract(0, 1, 0);
		boolean changed = false;
		if (surface.getBlock().getType() != Material.GLASS)
		{
			MapUtil.QuickChangeBlockAt(surface, Material.GLASS.getId(), (byte) 0);
			changed = true;
		}
		if (beacon.getBlock().getType() != Material.BEACON)
		{
			MapUtil.QuickChangeBlockAt(beacon, Material.BEACON.getId(), (byte) 0);
			changed = true;
		}
		if (changed)
			applyViewerCosmeticColours();
		return changed;
	}

	public void turnOffBeacon()
	{
		for (SafePadBlock bl : _blocks)
		{
			if (bl.getBlockMaterial() == Material.IRON_BLOCK)
				bl.setMaterial(Material.QUARTZ_BLOCK);
			else if (bl.getBlockMaterial() == Material.BEACON)
				bl.setMaterial(Material.AIR);
			else if (bl.getBlockMaterial() == Material.GLASS)
			{
				bl.setMaterial(Material.STAINED_CLAY);
				bl.setData(_surfaceColour);
			}
		}
		unregisterListener();
		applyViewerCosmeticColours();
	}

	@SuppressWarnings("deprecation")
	private void applyViewerCosmeticColours()
	{
		if (Host == null || _loc == null || _loc.getWorld() == null)
			return;
		for (Player viewer : Host.GetPlayers(false))
			applyViewerCosmeticColours(viewer);
	}

	/** Repaint just this client's pad tint and floor-level beacon glass after Mob Theme changes. */
	@SuppressWarnings("deprecation")
	public void refreshViewerCosmeticColours(Player viewer)
	{
		if (viewer == null || !viewer.isOnline() || Host == null || _loc == null || _loc.getWorld() == null)
			return;
		applyViewerCosmeticColours(viewer);
	}

	@SuppressWarnings("deprecation")
	private void applyViewerCosmeticColours(Player viewer)
	{
		if (viewer == null || !viewer.isOnline() || Host == null || _loc == null || _loc.getWorld() == null)
			return;

		String mobSet = _maze == null ? null : _maze.getMobSetForPlayer(viewer);
		boolean customPack = mobSet != null && ArcadeCosmeticsMenu.isMonsterMazeCustomPackUnlocked(viewer.getUniqueId());
		// The server-side clay still changes through the green/yellow/orange/red warning
		// sequence. Keep the per-viewer cosmetic tint stable over that backing block data;
		// otherwise the selected colour appears to switch off halfway through a stage.
		for (SafePadBlock block : _blocks)
		{
			if (block.getBlockMaterial() != Material.STAINED_CLAY)
				continue;
			byte data = customPack
					? (byte) ArcadeCosmeticsMenu.getMonsterMazeSafePadColourData(viewer.getUniqueId(), mobSet)
					: block.getBlockData();
			viewer.sendBlockChange(block.getLocation(), Material.STAINED_CLAY, data);
		}

		// This clear glass tile sits immediately above the lowered beacon. Change only the viewer's glass colour.
		Location surface = _loc.clone();
		Location beacon = _loc.clone().subtract(0, 1, 0);
		if (customPack
				&& surface.getBlock().getType() == Material.GLASS
				&& beacon.getBlock().getType() == Material.BEACON)
		{
			byte colour = (byte) ArcadeCosmeticsMenu.getMonsterMazeBeaconColourData(viewer.getUniqueId(), mobSet);
			viewer.sendBlockChange(surface, Material.STAINED_GLASS, colour);
		}
		else
		{
			viewer.sendBlockChange(surface, surface.getBlock().getType(), surface.getBlock().getData());
		}

		// Remove any stale client-only overlay left by the earlier native-port build.
		Location legacyOverlay = _loc.clone().add(0, 6, 0);
		viewer.sendBlockChange(legacyOverlay, legacyOverlay.getBlock().getType(), legacyOverlay.getBlock().getData());
	}

	@SuppressWarnings("deprecation")
	private void clearViewerCosmeticColours()
	{
		if (Host == null || _loc == null || _loc.getWorld() == null)
			return;

		for (Player viewer : Host.GetPlayers(false))
		{
			for (SafePadBlock block : _blocks)
			{
				Location location = block.getLocation();
				viewer.sendBlockChange(location, location.getBlock().getType(), location.getBlock().getData());
			}
			Location legacyOverlay = _loc.clone().add(0, 6, 0);
			viewer.sendBlockChange(legacyOverlay, legacyOverlay.getBlock().getType(), legacyOverlay.getBlock().getData());
		}
	}

	private void unregisterListener()
	{
		HandlerList.unregisterAll(this);
	}

	/** Prevent the vanilla Beacon UI opening when a player right-clicks the objective glass or beacon. */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent event)
	{
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || _loc == null)
			return;

		Location clicked = event.getClickedBlock().getLocation();
		if (clicked.getWorld() != _loc.getWorld())
			return;

		boolean objectiveGlass = event.getClickedBlock().getType() == Material.GLASS
				&& clicked.getBlockX() == _loc.getBlockX()
				&& clicked.getBlockY() == _loc.getBlockY()
				&& clicked.getBlockZ() == _loc.getBlockZ();
		boolean objectiveBeacon = event.getClickedBlock().getType() == Material.BEACON
				&& clicked.getBlockX() == _loc.getBlockX()
				&& clicked.getBlockY() == _loc.getBlockY() - 1
				&& clicked.getBlockZ() == _loc.getBlockZ();
		if (objectiveGlass || objectiveBeacon)
			event.setCancelled(true);
	}

	/**
	 * Source Monster Maze repaint fix: right-clicking a pad block makes the client process a
	 * new block update, which can overwrite the custom per-viewer tint. Re-send our cosmetic
	 * block changes after the interaction packet and later client/server block updates settle.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void refreshTintAfterPadInteraction(final PlayerInteractEvent event)
	{
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
				|| _loc == null || _loc.getWorld() == null)
			return;

		final Location clicked = event.getClickedBlock().getLocation();
		if (clicked.getWorld() != _loc.getWorld())
			return;

		boolean belongsToPad = false;
		for (SafePadBlock block : _blocks)
		{
			Location at = block.getLocation();
			if (at.getWorld() == clicked.getWorld()
					&& at.getBlockX() == clicked.getBlockX()
					&& at.getBlockY() == clicked.getBlockY()
					&& at.getBlockZ() == clicked.getBlockZ())
			{
				belongsToPad = true;
				break;
			}
		}
		if (!belongsToPad)
			return;

		final Player viewer = event.getPlayer();
		for (final long delay : new long[] {1L, 3L, 6L, 10L})
		{
			Bukkit.getScheduler().runTaskLater(Host.Manager.getPlugin(), new Runnable()
			{
				@Override
				public void run()
				{
					if (viewer.isOnline() && Host != null && _loc != null && _loc.getWorld() != null
							&& !_blocks.isEmpty())
						applyViewerCosmeticColours(viewer);
				}
			}, delay);
		}
	}

	public boolean decay()
	{
		if (_decayCount == -1)
			return true;

		_decayCount--;

		if(_decayCount == 10)
		{
			setBreakData((byte)5); // green
			setCrackedProgress(1);
		}
		else if(_decayCount == 9)
		{
			setCrackedProgress(2);
		}
		else if(_decayCount == 8)
		{
			setBreakData((byte)4); // yellow
			setCrackedProgress(3);
		}
		else if(_decayCount == 7)
		{
			setCrackedProgress(4);
		}
		else if(_decayCount == 6)
		{
			setBreakData((byte)1); // orange
			setCrackedProgress(5);
		}
		else if(_decayCount == 5)
		{
			setCrackedProgress(6);
		}
		else if(_decayCount == 4)
		{
			setBreakData((byte)14); // red
			setCrackedProgress(7);
		}
		else if(_decayCount == 3)
		{
			setCrackedProgress(8);
		}
		else if(_decayCount == 2)
		{
			setCrackedProgress(9);
		}
		else if(_decayCount == 1)
		{
			_decayCount = -1;
			
			setCrackedProgress(-1);

			destroySurface();
			destroyBase();

			return true;
		}
		return false;
	}

	private void setCrackedProgress(int progress)
	{
		int i = 0;
		Iterator<SafePadBlock> iter = _blocks.iterator();
		ArrayList<Packet> packets = new ArrayList<Packet>();
		while(iter.hasNext())
		{
			SafePadBlock spb = iter.next();
			if (!spb.getLocation().getBlock().getType().equals(Material.STAINED_CLAY)) 
				continue;
						
			i++;
			
			Packet packet = new PacketPlayOutBlockBreakAnimation(i, new BlockPosition(spb.getLocation().getBlockX(), spb.getLocation().getBlockY(), spb.getLocation().getBlockZ()), progress);
			packets.add(packet);
		}
		for(Player p : Host.GetPlayers(false))
		{
			Packet[] pcks = new Packet[packets.size()];
			packets.toArray(pcks);
			UtilPlayer.sendPacket(p, pcks);
		}
		/*for(SafePadBlock spb : _blocks)
		{
			if(!spb.getLocation().getBlock().getType().equals(Material.STAINED_GLASS)) continue;
			
			UtilParticle.PlayParticle(ParticleType.BLOCK_DUST.getParticle(Material.STAINED_GLASS, spb.getBlockData()), spb.getLocation(), 0.5f, 0.5f, 0.5f, 0.05f, 8, ViewDist.NORMAL, Host.GetPlayers(false).toArray(new Player[Host.GetPlayers(false).size()]));
			Packet packet = new PacketPlayOutBlockBreakAnimation(1, spb.getLocation().getBlockX(), spb.getLocation().getBlockY(), spb.getLocation().getBlockZ(), progress);
			for(Player p : Host.GetPlayers(false))
			{
				UtilPlayer.sendPacket(p, packet);
			}
		}*/
	}

	public void destroyBase()
	{
		for (final SafePadBlock bl : _blocks)
		{			
			if (bl.getBlockMaterial() == Material.QUARTZ_BLOCK || bl.getBlockMaterial() == Material.QUARTZ_STAIRS || bl.getBlockMaterial() == Material.AIR)
			{
				bl.restore();
			}
		}
	}
	
	public void destroySurface()
	{
		for (SafePadBlock bl : _blocks)
		{
			if (bl.getBlockMaterial() == Material.STAINED_CLAY || bl.getBlockMaterial() == Material.GLASS)
				bl.restore();
		}
		clearViewerCosmeticColours();
	}

	/**
	 * Restore every block originally snapshotted by this pad. Used during whole-game
	 * cleanup, including pads that were partially decayed at the time the game ended.
	 */
	public void restoreAll()
	{
		for (SafePadBlock block : _blocks)
			block.restore();
		clearViewerCosmeticColours();
		unregisterListener();
	}

	public Location getLocation()
	{
		return _loc;
	}
	
	public boolean isOn(Entity e)
	{
		return e != null && isOn(e.getLocation());
	}

	public boolean isOn(Location entityLocation)
	{
		if (entityLocation == null || entityLocation.getWorld() != _loc.getWorld())
			return false;

		double dx = entityLocation.getX() - _loc.getX();
		double dz = entityLocation.getZ() - _loc.getZ();
		double dy = entityLocation.getY() - _loc.getY();

		// Match the visible 5x5 safe-pad surface symmetrically on both axes.
		// The old bounding-box corners were reversed/asymmetric for Z, so players
		// could stand on one edge of the beacon pad and still be considered off-pad.
		return dx > -2.5 && dx < 2.5
				&& dz > -2.5 && dz < 2.5
				&& dy > 0.0 && dy < 5.0;
	}
}
