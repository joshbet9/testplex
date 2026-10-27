package nautilus.game.arcade.wineffect;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import mineplex.core.common.util.UtilEnt;
import mineplex.core.common.util.UtilServer;
import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.gadgets.wineffect.WinEffectPodium;
import mineplex.core.gadget.types.Gadget;
import mineplex.core.gadget.types.GadgetType;
import mineplex.core.gadget.types.WinEffectGadget;

import nautilus.game.arcade.GameType;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import nautilus.game.arcade.game.Game;

public class WinEffectManager
{

    private Game _game;
	private Player _winner;
	private List<Player> _team;
	private List<Player> _nonTeam;
	private WinEffectBaconBrawl _baconBrawlEffect;
	private WinEffectBombLobbers _bombLobbersEffect;
	private WinEffectGadget _activeEffect;
	
	public void prePlay(Game game, Player winner, List<Player> team, List<Player> nonTeam) 
	{
		_winner = winner;
		_game = game;
		_team = team;
		_nonTeam = nonTeam;
	}
	
	public void playWinEffect(Location loc) 
	{
		buildWinnerRoom(loc);
		UtilEnt.getAllInRadius(loc, 20).keySet().stream().filter(e-> !(e instanceof Player)).forEach(Entity::remove);
		_game.CreatureAllowOverride = true;
		for(Player p : UtilServer.getPlayers())
		{
			Gadget g = _game.getArcadeManager().getCosmeticManager().getGadgetManager().getActive(p, GadgetType.ITEM);
			if(g != null) g.disable(p);
		}
		playEffect();
	}
	
	protected void buildWinnerRoom(Location loc)
	{
		_activeEffect = getWinEffect();

		if (_activeEffect == null)
		{
			return;
		}

		_game.WorldTimeSet = _activeEffect.getGameTime();
		_activeEffect.setup(_winner, _team, _nonTeam, loc);
		_activeEffect.buildWinnerRoom();
	}
	
	protected void playEffect()
    {
		if (_game == null)
		{
			return;
		}

        if (_activeEffect == null)
        {
            _activeEffect = getWinEffect();
        }
        if (_activeEffect == null)
        {
            return;
        }

		_game.getArcadeManager().getCosmeticManager().getGadgetManager().setHideParticles(true);
		_activeEffect.teleport();
		_activeEffect.runPlay();
	}

	public void end()
    {
    	if (_game == null)
		{
			return;
		}

        WinEffectGadget effect = _activeEffect == null ? getWinEffect() : _activeEffect;
        try
        {
            if (effect != null)
            {
                effect.runFinish();
            }
        }
        finally
        {
            _activeEffect = null;
            _game.getArcadeManager().getCosmeticManager().getGadgetManager().setHideParticles(false);
            _game.CreatureAllowOverride = false;
        }
	}
	
	public WinEffectGadget getWinEffect()
	{
		if (_game == null)
		{
			return null;
		}

        // This opt-in Arcade effect is separate from the player's normal Mineplex
        // win-effect selection. Every other game still uses its equipped gadget.
        if (_winner != null && _game.GetType() == GameType.BaconBrawl
                && "body-slam".equals(ArcadeCosmeticsMenu.getEquippedWinEffect(
                        _winner.getUniqueId(), "baconbrawl")))
        {
            if (_baconBrawlEffect == null)
            {
                GadgetManager arcadeGadgets = _game.getArcadeManager().getCosmeticManager().getGadgetManager();
                _baconBrawlEffect = new WinEffectBaconBrawl(arcadeGadgets);
            }
            return _baconBrawlEffect;
        }

        if (_winner != null && _game.GetType() == GameType.Lobbers
                && "tnt-triple-hit".equals(ArcadeCosmeticsMenu.getEquippedWinEffect(
                        _winner.getUniqueId(), "bomblobbers")))
        {
            if (_bombLobbersEffect == null)
            {
                GadgetManager arcadeGadgets = _game.getArcadeManager().getCosmeticManager().getGadgetManager();
                _bombLobbersEffect = new WinEffectBombLobbers(arcadeGadgets);
            }
            return _bombLobbersEffect;
        }

		GadgetManager manager = _game.getArcadeManager().getCosmeticManager().getGadgetManager();
		Gadget winEffect = manager.getGadget(WinEffectPodium.class);

		if (_winner != null)
		{
			for (Gadget gadget : manager.getGadgets(GadgetType.WIN_EFFECT))
			{
				if (gadget.isActive(_winner))
				{
					winEffect = gadget;
				}
			}
		}

		return (WinEffectGadget) winEffect;
	}

}
