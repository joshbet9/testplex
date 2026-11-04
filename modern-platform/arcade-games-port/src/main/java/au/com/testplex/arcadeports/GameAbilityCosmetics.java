package au.com.testplex.arcadeports;

import java.util.Locale;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Shared, game-scoped renderer for each game's single ability-effects cosmetic line. */
final class GameAbilityCosmetics {
    private GameAbilityCosmetics() { }

    static void preview(Player viewer, Location center, String optionId) {
        emit(viewer, center, optionId, 1.0D);
    }

    static void hit(Player viewer, Location center, String optionId) {
        emit(viewer, center, optionId, 0.38D);
    }

    static Palette palette(String optionId) {
        String id = normalize(optionId);
        if (id.contains("dragon-eye")) {
            return new Palette(Color.fromRGB(255, 226, 119), Particle.ENCHANT, Particle.END_ROD,
                    Motif.EYE);
        }
        if (id.equals("anaconda")) {
            return new Palette(Color.fromRGB(98, 220, 100), Particle.COMPOSTER, Particle.HAPPY_VILLAGER,
                    Motif.ORBIT);
        }
        if (id.equals("cobra")) {
            return new Palette(Color.fromRGB(255, 204, 69), Particle.CRIT, Particle.ELECTRIC_SPARK,
                    Motif.BURST);
        }
        if (id.equals("neon-viper")) {
            return new Palette(Color.fromRGB(63, 240, 235), Particle.ELECTRIC_SPARK, Particle.END_ROD,
                    Motif.SLASH);
        }
        if (id.contains("wing-flare")) {
            return new Palette(Color.fromRGB(249, 247, 255), Particle.FIREWORK, Particle.DRAGON_BREATH,
                    Motif.SLASH);
        }
        if (id.contains("cloud-surf")) {
            return new Palette(Color.fromRGB(91, 208, 255), Particle.END_ROD, Particle.CLOUD,
                    Motif.SPIRAL);
        }
        if (id.equals("blue-flame") || id.contains("trail-cyan") || id.contains("cloud-surf")
                || id.contains("skyfall") || id.contains("glacier") || id.contains("ice-shard")
                || id.contains("cracked-ice") || id.contains("snowburst") || id.contains("frost")) {
            return new Palette(Color.fromRGB(91, 208, 255), Particle.END_ROD, Particle.SNOWFLAKE,
                    Motif.ARC);
        }
        if (id.equals("trail-red") || id.contains("red-sand") || id.contains("turf-crack")
                || id.contains("painted-chevron") || id.contains("blood")) {
            return new Palette(Color.fromRGB(246, 63, 75), Particle.CRIT, Particle.FIREWORK,
                    Motif.SLASH);
        }
        if (id.equals("trail-chalk") || id.contains("bone-dust") || id.contains("chalk")
                || id.contains("chrome-marker")) {
            return new Palette(Color.fromRGB(235, 241, 247), Particle.END_ROD, Particle.SNOWFLAKE,
                    Motif.GRID);
        }
        if (id.contains("lantern-motes") || id.contains("orange-comet") || id.contains("hoof-quake")
                || id.contains("hay-bloom") || id.contains("arena-challenger") || id.contains("oath-mark")) {
            return new Palette(Color.fromRGB(255, 190, 79), Particle.END_ROD, Particle.COMPOSTER,
                    Motif.ORBIT);
        }
        if (id.contains("shepherd-flare")) {
            return new Palette(Color.fromRGB(255, 190, 79), Particle.HAPPY_VILLAGER, Particle.FIREWORK,
                    Motif.ARC);
        }
        if (id.contains("wool-confetti")) {
            return new Palette(Color.fromRGB(255, 113, 193), Particle.HAPPY_VILLAGER, Particle.FIREWORK,
                    Motif.SPLASH);
        }
        if (id.contains("turtle-shell") || id.contains("charged-shell") || id.contains("sheep-bell")
                || id.contains("nature-bloom")) {
            return new Palette(Color.fromRGB(111, 231, 137), Particle.HAPPY_VILLAGER, Particle.COMPOSTER,
                    Motif.ORBIT);
        }
        if (id.contains("soul") || id.contains("void") || id.contains("ghost") || id.contains("rift")
                || id.contains("wither") || id.contains("arcane") || id.contains("glyph")
                || id.contains("rune") || id.contains("script")) {
            return new Palette(Color.fromRGB(178, 106, 255), Particle.SOUL, Particle.PORTAL,
                    Motif.SPIRAL);
        }
        if (id.contains("fire") || id.contains("flame") || id.contains("ember") || id.contains("molten")
                || id.contains("crispy") || id.contains("inferno") || id.contains("phoenix")
                || id.contains("orange") || id.contains("tnt") || id.contains("siege-spark")) {
            return new Palette(Color.fromRGB(255, 103, 35), Particle.FLAME, Particle.LAVA,
                    Motif.BURST);
        }
        if (id.contains("ink") || id.contains("paint") || id.contains("splat") || id.contains("colour")
                || id.contains("color") || id.contains("stencil") || id.contains("marker")
                || id.contains("dairy-splash")) {
            return new Palette(Color.fromRGB(255, 76, 196), Particle.SPLASH, Particle.FIREWORK,
                    Motif.SPLASH);
        }
        if (id.contains("prism") || id.contains("crystal") || id.contains("shard")
                || id.contains("fracture") || id.contains("grid") || id.contains("seam")
                || id.contains("blueprint") || id.contains("stencil")) {
            return new Palette(Color.fromRGB(123, 193, 255), Particle.ELECTRIC_SPARK, Particle.END_ROD,
                    Motif.GRID);
        }
        if (id.contains("bullseye") || id.contains("target") || id.contains("starburst")
                || id.contains("impact") || id.contains("blast") || id.contains("burst")
                || id.contains("crater") || id.contains("quake") || id.contains("spark")) {
            return new Palette(Color.fromRGB(255, 207, 74), Particle.CRIT, Particle.ELECTRIC_SPARK,
                    Motif.BURST);
        }
        if (id.contains("dragon") || id.contains("wing") || id.contains("flight") || id.contains("comet")) {
            return new Palette(Color.fromRGB(112, 226, 255), Particle.DRAGON_BREATH, Particle.END_ROD,
                    Motif.ARC);
        }
        if (id.contains("smoke") || id.contains("soul-ribbon") || id.contains("mote")
                || id.contains("cloud") || id.contains("dust") || id.contains("confetti")
                || id.contains("ribbon") || id.contains("current") || id.contains("trail")) {
            return new Palette(Color.fromRGB(195, 177, 255), Particle.PORTAL, Particle.END_ROD,
                    Motif.SPIRAL);
        }
        return new Palette(Color.fromRGB(116, 174, 255), Particle.ENCHANT, Particle.END_ROD,
                Motif.RING);
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
            case "speed-builders", "speed-builders-arcade" -> "blueprint-pulse";
            case "monster-maze" -> "maze-pulse";
            default -> "arcade-pulse";
        };
    }

    private static void emit(Player viewer, Location center, String optionId, double scale) {
        if (viewer == null || !viewer.isOnline() || center == null || center.getWorld() == null
                || viewer.getWorld() != center.getWorld()) return;
        World world = center.getWorld();
        Palette palette = palette(optionId);
        Location origin = center.clone().add(0.0D, 0.8D, 0.0D);
        int points = Math.max(10, (int) Math.round(32.0D * scale));
        double radius = 0.72D * scale;
        float dustSize = (float) Math.max(0.6D, 1.1D * scale);
        Particle.DustOptions dust = new Particle.DustOptions(palette.colour(), dustSize);
        switch (palette.motif()) {
            case RING -> emitRing(world, origin, palette, dust, points, radius, scale);
            case ORBIT -> emitOrbit(world, origin, palette, dust, points, radius, scale);
            case SPIRAL -> emitSpiral(world, origin, palette, dust, points, radius, scale);
            case BURST -> emitBurst(world, origin, palette, dust, points, radius, scale);
            case GRID -> emitGrid(world, origin, palette, dust, points, radius, scale);
            case ARC -> emitArc(world, origin, palette, dust, points, radius, scale);
            case SLASH -> emitSlash(world, origin, palette, dust, points, radius, scale);
            case SPLASH -> emitSplash(world, origin, palette, dust, points, radius, scale);
            case EYE -> emitEye(world, origin, palette, dust, points, radius, scale);
        }
    }

    private static void emitRing(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        for (int point = 0; point < points; point++) {
            double angle = Math.PI * 2.0D * point / points;
            spawnDust(world, origin, Math.cos(angle) * radius, Math.sin(angle) * radius,
                    Math.sin(angle * 3.0D) * 0.12D * scale, dust);
        }
        accents(world, origin, palette, scale, points / 3);
    }

    private static void emitSpiral(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        for (int point = 0; point < points; point++) {
            double progress = (double) point / points;
            double angle = Math.PI * 4.0D * progress;
            double r = radius * (0.3D + 0.7D * progress);
            spawnDust(world, origin, Math.cos(angle) * r, Math.sin(angle) * r,
                    progress * 0.9D * scale, dust);
        }
        accents(world, origin, palette, scale, Math.max(2, points / 5));
    }

    private static void emitOrbit(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        int orbitPoints = Math.max(6, points / 2);
        for (int point = 0; point < orbitPoints; point++) {
            double angle = Math.PI * 2.0D * point / orbitPoints;
            spawnDust(world, origin, Math.cos(angle) * radius, Math.sin(angle) * radius,
                    0.18D * scale, dust);
            spawnDust(world, origin, Math.cos(angle + Math.PI / 2.0D) * radius * 0.72D,
                    Math.sin(angle + Math.PI / 2.0D) * radius * 0.72D, -0.16D * scale, dust);
        }
        accents(world, origin, palette, scale, Math.max(2, points / 6));
    }

    private static void emitBurst(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        int rays = Math.max(5, points / 3);
        for (int ray = 0; ray < rays; ray++) {
            double angle = Math.PI * 2.0D * ray / rays;
            for (int step = 1; step <= 3; step++) {
                double r = radius * step / 3.0D;
                spawnDust(world, origin, Math.cos(angle) * r, Math.sin(angle) * r,
                        (step % 2 == 0 ? 0.08D : -0.04D) * scale, dust);
            }
        }
        accents(world, origin, palette, scale, Math.max(3, rays / 2));
    }

    private static void emitGrid(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        int edgePoints = Math.max(3, points / 8);
        for (int edge = 0; edge < 4; edge++) {
            double a1 = Math.PI * 0.25D + edge * Math.PI * 0.5D;
            double a2 = a1 + Math.PI * 0.5D;
            for (int step = 0; step <= edgePoints; step++) {
                double t = (double) step / edgePoints;
                double x = Math.cos(a1) * radius * (1.0D - t) + Math.cos(a2) * radius * t;
                double z = Math.sin(a1) * radius * (1.0D - t) + Math.sin(a2) * radius * t;
                spawnDust(world, origin, x, z, 0.0D, dust);
            }
        }
        accents(world, origin, palette, scale, Math.max(2, points / 8));
    }

    private static void emitArc(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        for (int point = 0; point < points; point++) {
            double progress = (double) point / Math.max(1, points - 1);
            double angle = -Math.PI * 0.85D + Math.PI * 1.7D * progress;
            spawnDust(world, origin, Math.cos(angle) * radius, Math.sin(angle) * radius,
                    Math.sin(progress * Math.PI) * 0.65D * scale, dust);
        }
        accents(world, origin, palette, scale, Math.max(2, points / 6));
    }

    private static void emitSlash(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        for (int point = 0; point < points; point++) {
            double progress = (double) point / Math.max(1, points - 1);
            double x = (progress - 0.5D) * radius * 2.4D;
            double z = (progress - 0.5D) * radius * 1.2D;
            spawnDust(world, origin, x, z, (progress - 0.5D) * 0.7D * scale, dust);
            if (point % Math.max(1, points / 5) == 0) {
                spawnDust(world, origin, x + 0.12D * scale, z, (progress - 0.5D) * 0.7D, dust);
            }
        }
        accents(world, origin, palette, scale, Math.max(2, points / 6));
    }

    private static void emitSplash(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        for (int point = 0; point < points; point++) {
            double angle = Math.PI * 2.0D * point / points;
            double r = radius * (0.35D + ((point % 4) / 4.0D) * 0.8D);
            spawnDust(world, origin, Math.cos(angle) * r, Math.sin(angle) * r,
                    ((point % 3) - 1) * 0.2D * scale, dust);
        }
        accents(world, origin, palette, scale, Math.max(3, points / 4));
    }

    private static void emitEye(World world, Location origin, Palette palette, Particle.DustOptions dust,
            int points, double radius, double scale) {
        for (int ring = 1; ring <= 3; ring++) {
            double r = radius * ring / 3.0D;
            int ringPoints = Math.max(8, points * ring / 3);
            for (int point = 0; point < ringPoints; point++) {
                double angle = Math.PI * 2.0D * point / ringPoints;
                spawnDust(world, origin, Math.cos(angle) * r, Math.sin(angle) * r,
                        Math.cos(angle * 2.0D) * 0.08D * scale, dust);
            }
        }
        world.spawnParticle(palette.primary(), origin, Math.max(2, points / 8),
                0.12D * scale, 0.12D * scale, 0.12D * scale, 0.01D);
        accents(world, origin, palette, scale, Math.max(2, points / 8));
    }

    private static void spawnDust(World world, Location origin, double x, double z, double y,
            Particle.DustOptions dust) {
        world.spawnParticle(Particle.DUST, origin.clone().add(x, y, z), 1,
                0.0D, 0.0D, 0.0D, 0.0D, dust);
    }

    private static void accents(World world, Location origin, Palette palette, double scale, int count) {
        world.spawnParticle(palette.primary(), origin, count, 0.2D * scale, 0.3D * scale,
                0.2D * scale, 0.01D);
        world.spawnParticle(palette.accent(), origin, Math.max(1, count / 2), 0.25D * scale,
                0.35D * scale, 0.25D * scale, 0.01D);
    }

    private static String normalize(String optionId) {
        return optionId == null ? "" : optionId.toLowerCase(Locale.ROOT);
    }

    record Palette(Color colour, Particle primary, Particle accent, Motif motif) { }
    enum Motif { RING, ORBIT, SPIRAL, BURST, GRID, ARC, SLASH, SPLASH, EYE }
}
