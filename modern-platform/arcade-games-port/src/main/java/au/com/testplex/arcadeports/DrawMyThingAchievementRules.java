package au.com.testplex.arcadeports;

import java.util.Map;
import org.bukkit.Material;

/** Small, deterministic qualification rules for Draw My Thing achievements. */
final class DrawMyThingAchievementRules {
    private DrawMyThingAchievementRules() {}

    static boolean minimalistMasterpiece(int paintedBlocks, int humanGuessers) {
        return paintedBlocks >= 0 && paintedBlocks <= 12 && humanGuessers >= 2;
    }

    static boolean isPaintedBlock(Material material) {
        return material != null && material != Material.AIR && material != Material.WHITE_WOOL;
    }

    static <K> int countChangedBlocks(Map<K, Material> before, Map<K, Material> after) {
        if (before == null || after == null) return 0;
        int changed = 0;
        for (Map.Entry<K, Material> entry : after.entrySet()) {
            if (!java.util.Objects.equals(before.getOrDefault(entry.getKey(), Material.AIR), entry.getValue())) {
                changed++;
            }
        }
        return changed;
    }

    static boolean qualifiesToolUse(String tool, int distinctBlocksChanged) {
        if (tool == null || distinctBlocksChanged < 0) return false;
        return switch (tool) {
            case "Spray Can" -> distinctBlocksChanged >= 20;
            case "Line Tool" -> distinctBlocksChanged >= 12;
            case "Circle Tool" -> distinctBlocksChanged >= 16;
            default -> false;
        };
    }

    static boolean cleanSlate(int paintedBeforeClear, int paintedWhenGuessed) {
        return paintedBeforeClear >= 8 && paintedWhenGuessed >= 8;
    }

    static boolean lastSecondSketch(long millisRemaining) {
        return millisRemaining >= 0 && millisRemaining <= 3_000;
    }

    static boolean twoHats(int drawerPoints, int guesserPoints) {
        return drawerPoints >= 2 && guesserPoints >= 6;
    }

    static boolean readBetweenTheLines(boolean firstCorrectGuess, int revealedLetters) {
        return firstCorrectGuess && revealedLetters == 1;
    }

    static boolean thirdTimesTheCharm(int acceptedGuessNumber, int revealedLetters, int totalLetters) {
        if (acceptedGuessNumber != 3 || revealedLetters < 0 || totalLetters <= 0) return false;
        return revealedLetters * 4 <= totalLetters;
    }
}
