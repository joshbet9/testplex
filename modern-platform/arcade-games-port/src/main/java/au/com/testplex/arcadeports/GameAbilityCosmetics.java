package au.com.testplex.arcadeports;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Shared particle language for each game's single ability-effects cosmetic line. */
final class GameAbilityCosmetics {
    private GameAbilityCosmetics() { }

    static void preview(Player viewer, Location center, String optionId) {
        emit(viewer, center, optionId, 1.0D);
    }

    static void hit(Player viewer, Location center, String optionId) {
        emit(viewer, center, optionId, 0.38D);
    }

    static Palette palette(String optionId) {
        String id = optionId == null ? "" : optionId.toLowerCase(Locale.ROOT);
        if (containsAny(id, "ice", "frost", "snow", "glacier", "winter", "crystal")) {
            return new Palette(Color.fromRGB(91, 208, 255), Particle.SNOWFLAKE, Particle.END_ROD);
        }
        if (containsAny(id, "fire", "flame", "ember", "molten", "crispy", "inferno", "phoenix")) {
            return new Palette(Color.fromRGB(255, 103, 35), Particle.FLAME, Particle.LAVA);
        }
        if (containsAny(id, "soul", "void", "wither", "rift", "ghost", "lantern", "death")) {
            return new Palette(Color.fromRGB(178, 106, 255), Particle.SOUL, Particle.PORTAL);
        }
        if (containsAny(id, "nature", "bloom", "sheep", "wool", "hay", "forest", "green")) {
            return new Palette(Color.fromRGB(104, 242, 116), Particle.HAPPY_VILLAGER, Particle.COMPOSTER);
        }
        if (containsAny(id, "ink", "paint", "splat", "colour", "color", "stencil", "marker")) {
            return new Palette(Color.fromRGB(255, 76, 196), Particle.SPLASH, Particle.FIREWORK);
        }
        if (containsAny(id, "crack", "impact", "blast", "shock", "quake", "tnt", "crater", "seam")) {
            return new Palette(Color.fromRGB(255, 207, 74), Particle.CRIT, Particle.ELECTRIC_SPARK);
        }
        if (containsAny(id, "dragon", "wing", "flight", "cloud", "sky", "comet")) {
            return new Palette(Color.fromRGB(112, 226, 255), Particle.DRAGON_BREATH, Particle.END_ROD);
        }
        return new Palette(Color.fromRGB(116, 174, 255), Particle.ENCHANT, Particle.END_ROD);
    }

    static String defaultOption(String gameId) {
        return switch (gameId == null ? "" : gameId) {
            case "bacon-brawl" -> "crispy-comet";
            case "bomb-lobbers" -> "turtle-shell";
            case "death-tag" -> "soul-ribbons";
            case "dragon-escape" -> "dragon-eye";
            case "dragons" -> "phoenix-arc";
            case "evolution" -> "charged-shell";
            case "gladiators" -> "oath-mark";
            case "micro-battles" -> "cracked-ice";
            case "one-in-the-quiver" -> "bullseye-dash";
            case "paintball" -> "chrome-marker";
            case "runner" -> "trail-red";
            case "sheep-quest" -> "sheep-bell";
            case "snake" -> "anaconda";
            case "spleef" -> "snowburst";
            case "draw-my-thing" -> "ink-splash";
            case "master-builders" -> "blueprint-pulse";
            case "castle-assault" -> "crystal-fracture";
            case "heroes-of-gwen" -> "beacon-pulse";
            case "hoops-remastered" -> "court-trail";
            case "wizards" -> "arcane-glyph";
            case "milk-the-cow" -> "dairy-splash";
            case "wither-assault" -> "soul-rift";
            case "skywars" -> "tnt-burst";
            case "turf-wars" -> "team-sigil";
            case "sneaky-assassins" -> "smoke-artist";
            case "camera-shy" -> "flash-frame";
            case "plexball" -> "pitch-flare";
            case "super-sploor" -> "ink-current";
            case "speed-builders" -> "blueprint-pulse";
            case "monster-maze" -> "maze-pulse";
            default -> "arcade-pulse";
        };
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static void emit(Player viewer, Location center, String optionId, double scale) {
        if (viewer == null || !viewer.isOnline() || center == null || center.getWorld() == null
                || viewer.getWorld() != center.getWorld()) return;
        World world = center.getWorld();
        Palette palette = palette(optionId);
        Location origin = center.clone().add(0.0D, 0.8D, 0.0D);
        int points = Math.max(8, (int) Math.round(24.0D * scale));
        double radius = 0.72D * scale;
        float dustSize = (float) Math.max(0.6D, 1.1D * scale);
        Particle.DustOptions dust = new Particle.DustOptions(palette.colour(), dustSize);
        for (int point = 0; point < points; point++) {
            double angle = Math.PI * 2.0D * point / points;
            Location at = origin.clone().add(Math.cos(angle) * radius, Math.sin(angle * 2.0D) * 0.22D,
                    Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST, at, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust);
        }
        int accents = Math.max(2, (int) Math.round(8.0D * scale));
        world.spawnParticle(palette.primary(), origin, accents, 0.22D * scale, 0.35D * scale,
                0.22D * scale, 0.01D);
        world.spawnParticle(palette.accent(), origin, Math.max(1, accents / 2), 0.28D * scale,
                0.38D * scale, 0.28D * scale, 0.01D);
    }

    record Palette(Color colour, Particle primary, Particle accent) { }
}
