package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/** Draw My Thing's non-repeating word pool, timed letter hints and action bars. */
final class DrawMyThingManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.DRAW_MY_THING; }

    boolean isPaletteSwatch(SessionState session, Block block, SourceMapCatalog.MapData map) {
        if (session == null || block == null || block.getWorld() != session.world || block.getType().isAir()
                || map == null) return false;
        List<Location> bounds = map.dataLocations("GREEN");
        if (bounds.size() < 2) return false;
        Location first = bounds.get(0);
        Location second = bounds.get(1);
        return block.getX() >= Math.min(first.getBlockX(), second.getBlockX())
                && block.getX() <= Math.max(first.getBlockX(), second.getBlockX())
                && block.getY() >= Math.min(first.getBlockY(), second.getBlockY())
                && block.getY() <= Math.max(first.getBlockY(), second.getBlockY())
                && block.getZ() >= Math.min(first.getBlockZ(), second.getBlockZ())
                && block.getZ() <= Math.max(first.getBlockZ(), second.getBlockZ());
    }

    static String colourName(Material colour) {
        String name = colour.name();
        for (String suffix : List.of("_WOOL", "_STAINED_CLAY", "_TERRACOTTA", "_CONCRETE_POWDER",
                "_CONCRETE", "_STAINED_GLASS_PANE", "_STAINED_GLASS")) {
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }
        return name.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
    }

    void selectColour(SessionState session, Player player, Material colour, String colourName,
            boolean paletteEnabled) {
        if (!paletteEnabled) {
            player.sendActionBar(ChatColor.RED + "Palette selection is disabled in your debug profile.");
            return;
        }
        UUID id = player.getUniqueId();
        session.drawBrushColours.put(id, colour);
        session.drawPreviousPaintPoints.remove(id);
        session.drawShapeStarts.remove(id);
        session.drawShapeTools.remove(id);
        player.sendMessage(ChatColor.GREEN + colourName + " paint selected.");
        player.sendActionBar(Component.text("Paint colour: " + colourName));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0F, 1.0F);
    }

    Block canvasTarget(SessionState session, Player player, Block clicked) {
        if (clicked != null && session.drawCanvasBlocks.contains(clicked)) return clicked;
        Block target = player.getTargetBlockExact(64);
        return target != null && session.drawCanvasBlocks.contains(target) ? target : null;
    }

    void performToolAction(SessionState session, Player player, Block target, String tool,
            boolean canvasToolsEnabled) {
        if (!canvasToolsEnabled) {
            player.sendActionBar(ChatColor.RED + "Canvas drawing tools are disabled in your debug profile.");
            return;
        }
        UUID id = player.getUniqueId();
        Material colour = session.drawBrushColours.getOrDefault(id, Material.BLACK_WOOL);
        Map<Block, Material> beforeTool = snapshotCanvas(session);
        if ("Clear Canvas".equals(tool) && countPaintedBlocks(session) >= 8) {
            session.drawCleanSlateReady = true;
        }
        switch (tool) {
            case "Pencil" -> paintBlock(session, player, target, false);
            case "Paint Brush" -> paintBlock(session, player, target, true);
            case "Line Tool", "Square Tool", "Circle Tool" -> {
                Block start = session.drawShapeStarts.remove(id);
                String previousTool = session.drawShapeTools.remove(id);
                if (start == null || !tool.equals(previousTool)) {
                    session.drawShapeStarts.put(id, target);
                    session.drawShapeTools.put(id, tool);
                    player.sendMessage(ChatColor.YELLOW + "First point selected. Click a second point to finish the "
                            + tool.replace(" Tool", "").toLowerCase(java.util.Locale.ROOT) + ".");
                    return;
                }
                if ("Line Tool".equals(tool)) {
                    DrawMyThingCanvas.line(session.drawCanvasBlocks, start, target, colour, false);
                } else if ("Square Tool".equals(tool)) {
                    DrawMyThingCanvas.rectangle(session.drawCanvasBlocks, start, target, colour);
                } else {
                    DrawMyThingCanvas.ellipse(session.drawCanvasBlocks, start, target, colour);
                }
                markPainted(session);
            }
            case "Spray Can" -> {
                DrawMyThingCanvas.point(session.drawCanvasBlocks, target, colour,
                        ArcadePortRules.drawMyThingBrushRadiusBlocks());
                for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
                        BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) {
                    Block adjacent = target.getRelative(face);
                    if (session.drawCanvasBlocks.contains(adjacent) && ThreadLocalRandom.current().nextBoolean()) {
                        DrawMyThingCanvas.point(session.drawCanvasBlocks, adjacent, colour, false);
                    }
                }
                markPainted(session);
            }
            case "Paint Bucket" -> {
                DrawMyThingCanvas.fill(session.drawCanvasBlocks, target, colour);
                markPainted(session);
                playSound(session, Sound.ENTITY_GENERIC_SPLASH, 0.4F, 1.5F);
            }
            case "Clear Canvas" -> {
                DrawMyThingCanvas.clear(session.drawCanvasBlocks);
                session.drawPreviousPaintPoints.remove(id);
                session.drawShapeStarts.remove(id);
                session.drawShapeTools.remove(id);
                markPainted(session);
                playSound(session, Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.5F);
            }
            default -> { return; }
        }
        recordToolQualification(session, tool, beforeTool);
        if (!"Clear Canvas".equals(tool) && !"Paint Bucket".equals(tool)) {
            playSound(session, Sound.BLOCK_FIRE_EXTINGUISH, 0.2F, 2.0F);
        }
    }

    private void paintBlock(SessionState session, Player player, Block target, boolean thick) {
        UUID id = player.getUniqueId();
        Material colour = session.drawBrushColours.getOrDefault(id, Material.BLACK_WOOL);
        Block previous = session.drawPreviousPaintPoints.put(id, target);
        int radius = thick ? ArcadePortRules.drawMyThingBrushRadiusBlocks() : 0;
        if (previous != null && session.drawCanvasBlocks.contains(previous)) {
            DrawMyThingCanvas.line(session.drawCanvasBlocks, previous, target, colour, radius);
        } else {
            DrawMyThingCanvas.point(session.drawCanvasBlocks, target, colour, radius);
        }
        markPainted(session);
        playSound(session, Sound.BLOCK_FIRE_EXTINGUISH, 0.2F, 2.0F);
    }

    private void playSound(SessionState session, Sound sound, float volume, float pitch) {
        for (Player participant : session.players.values()) {
            if (participant != null && participant.isOnline()) {
                participant.playSound(participant.getLocation(), sound, volume, pitch);
            }
        }
    }

    private void markPainted(SessionState session) { session.drawStartedPainting = true; }

    private Map<Block, Material> snapshotCanvas(SessionState session) {
        Map<Block, Material> snapshot = new HashMap<>();
        for (Block block : session.drawCanvasBlocks) snapshot.put(block, block.getType());
        return snapshot;
    }

    int countPaintedBlocks(SessionState session) {
        int count = 0;
        for (Block block : session.drawCanvasBlocks) {
            if (DrawMyThingAchievementRules.isPaintedBlock(block.getType())) count++;
        }
        return count;
    }

    private void recordToolQualification(SessionState session, String tool, Map<Block, Material> before) {
        Map<Block, Material> after = new HashMap<>();
        for (Block block : session.drawCanvasBlocks) after.put(block, block.getType());
        int changed = DrawMyThingAchievementRules.countChangedBlocks(before, after);
        if (DrawMyThingAchievementRules.qualifiesToolUse(tool, changed)) {
            String achievement = switch (tool) {
                case "Spray Can" -> "spray-and-pray";
                case "Line Tool" -> "line-in-the-sand";
                case "Circle Tool" -> "perfectly-round";
                default -> null;
            };
            if (achievement != null) session.drawToolQualifications.add(achievement);
        }
    }

    String chooseWord(SessionState session) {
        List<String> words = new ArrayList<>(DrawMyThingContent.WORDS);
        words.removeAll(session.drawUsedWords);
        if (words.isEmpty()) {
            session.drawUsedWords.clear();
            words.addAll(DrawMyThingContent.WORDS);
        }
        String word = words.get(ThreadLocalRandom.current().nextInt(words.size()));
        session.drawUsedWords.add(word);
        return word;
    }

    void updateHintUi(SessionState session, Player drawer, long now) {
        if (session == null || session.drawWord == null) return;
        int letters = (int) session.drawWord.chars().filter(ch -> ch != ' ').count();
        boolean timedHints = GameDebugRuntimeOverrides.booleanValue("timed-hints", true);
        if (!timedHints) session.drawRevealedLetters.clear();
        int reveal = !timedHints ? 0 : ArcadePortRules.drawMyThingHintRevealCount(
                now - session.drawRoundStartedAt, session.drawRoundDurationMillis, letters);
        int revealedBefore = session.drawRevealedLetters.size();
        while (session.drawRevealedLetters.size() < reveal) {
            List<Integer> hidden = new ArrayList<>();
            for (int i = 0; i < session.drawWord.length(); i++) {
                if (session.drawWord.charAt(i) != ' ' && !session.drawRevealedLetters.contains(i)) hidden.add(i);
            }
            if (hidden.isEmpty()) break;
            session.drawRevealedLetters.add(hidden.get(ThreadLocalRandom.current().nextInt(hidden.size())));
        }
        if (session.drawRevealedLetters.size() > revealedBefore) {
            for (Player guesser : session.players.values()) {
                if (guesser != null && guesser.isOnline() && !guesser.getUniqueId().equals(session.drawCurrentDrawer)
                        && GameDebugRuntimeOverrides.booleanValue("sound-feedback", true)) {
                    guesser.playSound(guesser.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.5F, 2.0F);
                }
            }
        }
        long seconds = Math.max(0, (session.drawRoundEndsAt - now + 999) / 1000);
        String hint = hintMask(session.drawWord, session.drawRevealedLetters);
        if (drawer != null && drawer.isOnline()) {
            drawer.sendActionBar(Component.text("DRAW: " + session.drawWord + " | " + seconds + "s"));
        }
        for (Player player : session.players.values()) {
            if (player == null || !player.isOnline() || player.getUniqueId().equals(session.drawCurrentDrawer)) continue;
            if (session.drawCorrectGuessers.contains(player.getUniqueId())) {
                player.sendActionBar(Component.text("Correct! Waiting for next round"));
            } else {
                player.sendActionBar(Component.text(hint + " | " + seconds + "s | Guesses: "
                        + Math.max(0, ArcadePortRules.drawMyThingMaxGuesses()
                                - session.drawGuessAttempts.getOrDefault(player.getUniqueId(), 0))
                        + "/" + ArcadePortRules.drawMyThingMaxGuesses()));
            }
        }
    }

    static String hintMask(String word, Set<Integer> revealed) {
        StringBuilder hint = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            hint.append(ch == ' ' ? "  " : revealed.contains(i) ? ch + " " : "_ ");
        }
        return hint.toString();
    }
}
