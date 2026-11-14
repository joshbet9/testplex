package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;

/** Source-style centered Evolution charge meter. */
final class EvolutionChargeBar {
    static final int SEGMENTS = 40;

    private EvolutionChargeBar() { }

    static int filledSegments(float progress) {
        float clamped = Float.isFinite(progress) ? Math.max(0.0F, Math.min(1.0F, progress)) : 0.0F;
        return Math.min(SEGMENTS, (int) (clamped * SEGMENTS));
    }

    static int visibleRankedPlayers(int rankedPlayers) {
        return Math.min(Math.max(0, rankedPlayers), 9);
    }

    static String format(float progress) {
        int filled = filledSegments(progress);
        StringBuilder bar = new StringBuilder(SEGMENTS * 3);
        for (int index = 0; index < SEGMENTS; index++) {
            bar.append(index < filled ? ChatColor.GREEN : ChatColor.GRAY)
                    .append(ChatColor.BOLD).append('|');
        }
        return bar.toString();
    }
}
