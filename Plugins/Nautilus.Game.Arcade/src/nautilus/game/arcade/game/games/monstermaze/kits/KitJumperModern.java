package nautilus.game.arcade.game.games.monstermaze.kits;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import mineplex.core.common.util.C;
import mineplex.core.common.util.F;
import mineplex.core.game.kit.GameKit;
import mineplex.core.itemstack.ItemStackFactory;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.game.games.monstermaze.kits.perks.PerkJumpsDisplay;
import nautilus.game.arcade.kit.Kit;
import nautilus.game.arcade.kit.Perk;
import nautilus.game.arcade.kit.perks.PerkDummy;

/** Speed/QOL Jumper: three charges, refreshed by Safe Pads in Monster Maze Modern. */
public class KitJumperModern extends Kit
{
    public KitJumperModern(ArcadeManager manager)
    {
        super(manager, GameKit.MONSTER_MAZE_MODERN_JUMPER,
                new Perk[]
                {
                    new PerkDummy("Jumper", new String[]
                    {
                        C.cGray + "You can jump " + C.cYellow + "3 Times" + C.cGray + ".",
                        C.cGray + "Safe Pads refill your jumps; jumps on pads are free."
                    }),
                    new PerkJumpsDisplay()
                });
    }

    @Override
    public void GiveItems(Player player)
    {
        player.getInventory().setItem(4, ItemStackFactory.Instance.CreateStack(
                Material.COMPASS, (byte) 0, 1, F.item("Safe Pad Locator")));
        player.getInventory().setItem(8, ItemStackFactory.Instance.CreateStack(
                Material.FEATHER, (byte) 0, 3, C.cYellow + C.Bold + "3 Jumps Remaining"));
    }
}
