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
        int defaultInitial = this == MODERN ? 35 : MazeDebugRules.STAGE_TIMER_INITIAL_SECONDS;
        double defaultDecrease = this == MODERN ? 20.0D / 9.0D
                : MazeDebugRules.STAGE_TIMER_DECREASE_PER_STAGE_SECONDS;
        int initial = Math.max(1, MazeDebugRules.intValue(this, "STAGE_TIMER_INITIAL_SECONDS", defaultInitial));
        double decrease = Math.max(0.0D, MazeDebugRules.doubleValue(
                this, "STAGE_TIMER_DECREASE_PER_STAGE_SECONDS", defaultDecrease));
        int floor = Math.max(1, MazeDebugRules.intValue(this, "STAGE_TIMER_MIN_SECONDS",
                MazeDebugRules.STAGE_TIMER_MIN_SECONDS));
        return Math.max(floor, initial - (int) Math.floor((safeStage - 1) * decrease));
    }

    /** Source MonsterManager: only Modern starts with 225 monsters; other modes start with 150. */
    public int initialMonsterCount() {
        int fallback = this == MODERN ? 225 : MazeDebugRules.INITIAL_MONSTER_COUNT;
        return Math.max(1, MazeDebugRules.intValue(this, "INITIAL_MONSTER_COUNT", fallback));
    }

    /** Source GameManager: Modern adds 30 monsters each stage; other modes add 15. */
    public int additionalMonstersPerSafePad() {
        int fallback = this == MODERN ? 30 : MazeDebugRules.ADDITIONAL_MONSTERS_PER_SAFE_PAD;
        return Math.max(0, MazeDebugRules.intValue(this, "ADDITIONAL_MONSTERS_PER_SAFE_PAD", fallback));
    }

    /** Speed restores the old 1.8 speeding feel; Modern keeps its boost, Classic opts out. */
    public boolean speedBoostEnabled() {
        return this == SPEED || this == MODERN;
    }

    public static MazeMode byName(String name) {
        if (name == null) return null;
        for (MazeMode m : values()) {
            if (m.name().equalsIgnoreCase(name) || m.id.equalsIgnoreCase(name)) return m;
        }
        return null;
    }
}
