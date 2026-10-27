package nautilus.game.arcade.game.games.baconbrawl;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
/** Shared, visual-only effect palettes for Bacon Brawl abilities. */
public final class BaconBrawlCosmeticEffects
{
    private BaconBrawlCosmeticEffects() {}
    public static String choice(UUID id)
    {
        String value=ArcadeCosmeticsMenu.getEquippedChoice(id,"baconbrawl","ability-effects");
        return value==null||value.isEmpty()?"porky":value.toLowerCase(Locale.ROOT);
    }
    public static void play(Player player,String moment)
    {
        if(player!=null) play(player,player.getLocation().clone().add(0,1,0),moment);
    }
    public static void play(Player player,Location at,String moment)
    {
        if(player!=null) preview(at,choice(player.getUniqueId()),moment);
    }
    public static void preview(Location at,String theme,String moment)
    {
        if(at==null||at.getWorld()==null)return;
        String selected=theme==null?"porky":theme.toLowerCase(Locale.ROOT);
        boolean burst=moment!=null&&(moment.contains("activation")||moment.contains("impact")||moment.contains("hit")||moment.contains("showcase")||moment.contains("cloak"));
        switch(selected)
        {
            case "inferno":
                UtilParticle.PlayParticle(ParticleType.FLAME,at,0.18F,0.24F,0.18F,0.015F,6,ViewDist.NORMAL);
                if(burst)UtilParticle.PlayParticle(ParticleType.DRIP_LAVA,at,0.22F,0.25F,0.22F,0.01F,9,ViewDist.NORMAL); break;
            case "frost":
                UtilParticle.PlayParticle(ParticleType.SNOW_SHOVEL,at,0.20F,0.22F,0.20F,0.02F,7,ViewDist.NORMAL);
                if(burst)UtilParticle.PlayParticle(ParticleType.SNOWBALL_POOF,at,0.26F,0.28F,0.26F,0.01F,8,ViewDist.NORMAL); break;
            case "slime":
                UtilParticle.PlayParticle(ParticleType.SLIME,at,0.22F,0.22F,0.22F,0.015F,6,ViewDist.NORMAL);
                if(burst)UtilParticle.PlayParticle(ParticleType.HAPPY_VILLAGER,at,0.24F,0.24F,0.24F,0.02F,8,ViewDist.NORMAL); break;
            case "royal":
                UtilParticle.PlayParticle(ParticleType.WITCH_MAGIC,at,0.20F,0.22F,0.20F,0.01F,5,ViewDist.NORMAL);
                if(burst)UtilParticle.PlayParticle(ParticleType.MAGIC_CRIT,at,0.25F,0.25F,0.25F,0.03F,8,ViewDist.NORMAL); break;
            case "confetti":
                UtilParticle.PlayParticle(ParticleType.NOTE,at,0.30F,0.30F,0.30F,1.0F,4,ViewDist.NORMAL);
                if(burst)UtilParticle.PlayParticle(ParticleType.FIREWORKS_SPARK,at,0.30F,0.30F,0.30F,0.04F,10,ViewDist.NORMAL); break;
            case "porky":
            default:
                UtilParticle.PlayParticle(ParticleType.BLOCK_DUST.getParticle(Material.STAINED_CLAY,(byte)6),at,0.18F,0.24F,0.18F,0.01F,6,ViewDist.NORMAL);
                if(burst)UtilParticle.PlayParticle(ParticleType.HEART,at,0.28F,0.26F,0.28F,0.02F,5,ViewDist.NORMAL); break;
        }
    }
}
