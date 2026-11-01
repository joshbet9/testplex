package au.com.testplex.arcadeports;

import java.util.Locale;
import java.util.Set;

/** Pure source-contract rules for Wither Assault's Human kit selection and Editor limits. */
public final class WitherAssaultRules {
    private static final Set<String> HUMAN_KITS = Set.of("archer", "medic", "editor");

    private WitherAssaultRules() { }

    /** Normalise the three Reborn Human kits; unknown kit names are rejected, not silently accepted. */
    public static String normalizeHumanKit(String requested) {
        if (requested == null || requested.isBlank()) return null;
        String value = requested.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (value.startsWith("human-")) value = value.substring("human-".length());
        return HUMAN_KITS.contains(value) ? value : null;
    }

    /**
     * The Editor may only modify blocks at or above four blocks below the
     * Wither flight-height marker, and may not break a block directly under
     * another living player.
     */
    public static boolean canEditorModifyBlock(int blockY, double witherSpawnY, boolean underOtherPlayer) {
        if (!Double.isFinite(witherSpawnY) || underOtherPlayer) return false;
        return blockY >= Math.ceil(witherSpawnY - 4.0D);
    }

    /** Fletcher and Smoke Arrow are shared by the source Archer and Editor kits. */
    public static boolean hasFletcher(String kit) {
        return "archer".equals(normalizeHumanKit(kit)) || "editor".equals(normalizeHumanKit(kit));
    }

    /** Only living Humans with a bow kit create tracked Smoke Arrows. */
    public static boolean shouldTrackSmokeArrow(int team, String kit, boolean alive, boolean arrowProjectile) {
        return team == 0 && alive && arrowProjectile && hasFletcher(kit);
    }
}
