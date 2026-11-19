package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Slot geometry shared by every 9x6 Arcade achievements/milestones inventory. */
final class ArcadeStatsLayout {
    static final int INVENTORY_SIZE = 54;
    static final int GAME_LOGO = 13; // Second row, centre column.
    static final int BACK = 0;
    static final int PREVIOUS = 45;
    static final int NEXT = 53;
    static final int ACHIEVEMENTS_PER_PAGE = 14;
    static final int KITS_PER_PAGE = 7;
    static final int DRAGON_ESCAPE_MAPS_PER_PAGE = 36;
    static final int TRACKS_PER_PAGE = 4;
    static final int STAGES_PER_PAGE = 4;

    private ArcadeStatsLayout() { }

    static int pages(int count, int perPage) {
        if (perPage < 1) throw new IllegalArgumentException("perPage must be positive");
        return Math.max(1, (Math.max(0, count) + perPage - 1) / perPage);
    }

    static int clampPage(int page, int count, int perPage) {
        return Math.max(0, Math.min(page, pages(count, perPage) - 1));
    }

    /** Symmetric about the centre inventory column, even when the count is even. */
    static List<Integer> columns(int count) {
        if (count < 0 || count > 7) throw new IllegalArgumentException("Expected 0-7 columns");
        return switch (count) {
            case 0 -> List.of();
            case 1 -> List.of(4);
            case 2 -> List.of(3, 5);
            case 3 -> List.of(3, 4, 5);
            case 4 -> List.of(2, 3, 5, 6);
            case 5 -> List.of(2, 3, 4, 5, 6);
            case 6 -> List.of(1, 2, 3, 5, 6, 7);
            default -> List.of(1, 2, 3, 4, 5, 6, 7);
        };
    }

    /** Two balanced achievement rows immediately below the second-row game logo. */
    static List<Integer> achievementSlots(int count) {
        if (count < 0 || count > ACHIEVEMENTS_PER_PAGE)
            throw new IllegalArgumentException("Expected 0-14 achievements");
        int top = count <= 7 ? count : (count + 1) / 2;
        int bottom = count - top;
        List<Integer> slots = new ArrayList<>(count);
        for (int column : columns(top)) slots.add(18 + column);
        for (int column : columns(bottom)) slots.add(27 + column);
        return List.copyOf(slots);
    }

    /** Up to four milestone tracks, aligned with the tier column above each bottom category block. */
    static List<Integer> milestoneColumns(int count) {
        if (count < 0 || count > TRACKS_PER_PAGE)
            throw new IllegalArgumentException("Expected 0-4 milestone tracks");
        return switch (count) {
            case 0 -> List.of();
            case 1 -> List.of(4);
            case 2 -> List.of(3, 5);
            case 3 -> List.of(2, 4, 6);
            default -> List.of(1, 3, 5, 7);
        };
    }

    /** Category blocks sit on the actual bottom row; page navigation uses its outside slots. */
    static int milestoneCategorySlot(int column) {
        if (column < 0 || column > 8)
            throw new IllegalArgumentException("Expected inventory column 0-8");
        return 5 * 9 + column;
    }

    /** Tier 1 is directly above the bottom-row category; later tiers continue upward. */
    static int milestoneTierSlot(int stageOffset, int column) {
        if (stageOffset < 0 || stageOffset >= STAGES_PER_PAGE)
            throw new IllegalArgumentException("Expected milestone tier offset 0-" + (STAGES_PER_PAGE - 1));
        if (column < 0 || column > 8)
            throw new IllegalArgumentException("Expected inventory column 0-8");
        return (STAGES_PER_PAGE - stageOffset) * 9 + column;
    }

    /**
     * Milestone navigation stays on the bottom row. Category tiles and visible page arrows are
     * reserved first, then footer tabs are placed in the most centred remaining columns.
     */
    static List<Integer> milestoneFooterSlots(int count, Set<Integer> reservedSlots) {
        if (count < 0 || count > 3) throw new IllegalArgumentException("Expected 0-3 footer items");
        Set<Integer> reserved = new HashSet<>(reservedSlots);
        List<Integer> available = new ArrayList<>();
        for (int column = 0; column < 9; column++)
            if (!reserved.contains(milestoneCategorySlot(column))) available.add(column);
        if (available.size() < count)
            throw new IllegalArgumentException("Not enough bottom-row slots for milestone footer");
        List<Integer> ideal = switch (count) {
            case 0 -> List.of();
            case 1 -> List.of(4);
            case 2 -> List.of(3, 5);
            default -> List.of(2, 4, 6);
        };
        List<Integer> best = List.of();
        int bestCost = Integer.MAX_VALUE;
        int combinations = 1 << available.size();
        for (int mask = 0; mask < combinations; mask++) {
            if (Integer.bitCount(mask) != count) continue;
            List<Integer> candidate = new ArrayList<>(count);
            for (int index = 0; index < available.size(); index++)
                if ((mask & (1 << index)) != 0) candidate.add(available.get(index));
            int cost = 0;
            for (int index = 0; index < count; index++) {
                int delta = candidate.get(index) - ideal.get(index);
                cost += delta * delta;
            }
            if (cost < bestCost) {
                best = candidate;
                bestCost = cost;
            }
        }
        return best.stream().map(ArcadeStatsLayout::milestoneCategorySlot).toList();
    }

    /** Footer controls have a deliberate centre gap for two items, matching the Mineplex menus. */
    static List<Integer> footerSlots(int count) {
        if (count < 0 || count > 3) throw new IllegalArgumentException("Expected 0-3 footer items");
        return switch (count) {
            case 0 -> List.of();
            case 1 -> List.of(49);
            case 2 -> List.of(48, 50);
            default -> List.of(47, 49, 51);
        };
    }

    static long[] fourStages(long maximum) {
        long safe = Math.max(1L, maximum);
        return new long[] {Math.max(1L, (safe + 9L) / 10L),
                Math.max(1L, (safe + 3L) / 4L),
                Math.max(1L, (safe + 1L) / 2L), safe};
    }
}
