package nautilus.game.arcade.game.games.monstermaze.kits;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import mineplex.core.common.util.C;
import mineplex.core.common.util.F;
import mineplex.core.game.kit.GameKit;
import mineplex.core.itemstack.ItemStackFactory;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.kit.Kit;
import nautilus.game.arcade.kit.Perk;
import nautilus.game.arcade.kit.perks.PerkDummy;

/** Passive kit: monster knockback is directed toward the next Safe Pad. */
public class KitMaverick extends Kit
{
    public KitMaverick(ArcadeManager manager)
    {
        super(manager, GameKit.MONSTER_MAZE_MAVERICK,
                new Perk[]
                {
                    new PerkDummy("Maverick", new String[]
                    {
                        C.cGray + "Monster hits launch you " + C.cGold + "toward the next Safe Pad" + C.cGray + "."
                    })
                });
    }

    @Override
    public void GiveItems(Player player)
    {
        player.getInventory().setItem(4, ItemStackFactory.Instance.CreateStack(
                Material.COMPASS, (byte) 0, 1, F.item("Safe Pad Locator")));
    }
}
