package nautilus.game.arcade.command;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import mineplex.core.command.CommandBase;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.game.Game;
import nautilus.game.arcade.game.games.monstermaze.MonsterMaze;

/** Native Reborn command to inspect the current player's Monster Maze PBs. */
public class MonsterMazePBCommand extends CommandBase<ArcadeManager>
{
    public MonsterMazePBCommand(ArcadeManager plugin)
    {
        super(plugin, GameCommand.Perm.GAME_COMMAND_DUMMY_PERM, "pb");
    }

    @Override
    public void Execute(Player caller, String[] args)
    {
        if (!Plugin.canPlayerUseGameCmd(caller))
            return;

        Game current = Plugin.GetGame();
        if (!(current instanceof MonsterMaze))
        {
            caller.sendMessage(ChatColor.RED + "This command is available while a Monster Maze game is selected.");
            return;
        }

        MonsterMazePersonalBestManager.show((MonsterMaze) current, caller);
    }
}
