package me.monstermaze.game;

import org.bukkit.ChatColor;

/**
 * Game modes. ORIGINAL is a 1:1 recreation of the source.
 * The other modes provide alternate gameplay tuning and mechanics.
 */
public enum MazeMode {
    ORIGINAL("Original", ChatColor.GRAY,
            "The original Monster Maze experience, recreated as faithfully as possible."),
    SPEED("Speed", ChatColor.GOLD,
            "Modern gameplay with the original timer and monster spawning."),
    MODERN("Modern", ChatColor.LIGHT_PURPLE,
            "Enhanced gameplay with faster stages, more monsters, and expanded kit abilities."),
    CLASSIC("Classic", ChatColor.RED,
            "Modern gameplay without the speed boost, keeping the standard player movement speed.");

    public final String id;
    public final ChatColor color;
    public final String description;

    MazeMode(String id, ChatColor color, String description) {
        this.id = id;
        this.color = color;
        this.description = description;
    }

    /** Speed, Modern and Classic use the source QOL kit/interaction rules; Original does not. */
    public boolean qolEnabled() {
        return this != ORIGINAL;
    }

    /** Source stage timer: Modern accelerates to 15 seconds; the other modes retain 60s - 2s/stage. */
    public int stageTimerSeconds(int stage) {
        int safeStage = Math.max(1, stage);
        if (this == MODERN) return Math.max(15, 35 - ((safeStage - 1) * 20 / 9));
        return Math.max(15, 60 - ((safeStage - 1) * 2));
    }

    /** Source MonsterManager: only Modern starts with 225 monsters; other modes start with 150. */
    public int initialMonsterCount() {
        return this == MODERN ? 225 : 150;
    }

    /** Source GameManager: Modern adds 30 monsters each stage; other modes add 15. */
    public int additionalMonstersPerSafePad() {
        return this == MODERN ? 30 : 15;
    }

    public static MazeMode byName(String name) {
        if (name == null) return null;
        for (MazeMode m : values()) {
            if (m.name().equalsIgnoreCase(name) || m.id.equalsIgnoreCase(name)) return m;
        }
        return null;
    }
}
