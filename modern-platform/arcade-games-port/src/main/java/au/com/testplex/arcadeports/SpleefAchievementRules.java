package au.com.testplex.arcadeports;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure requirement checks shared by Spleef achievement detectors and tests. */
public final class SpleefAchievementRules {
    record BlockPosition(int x, int z) { }

    private SpleefAchievementRules() { }

    static boolean sinkhole(int creditedEliminations) {
        return creditedEliminations >= 5;
    }

    static boolean onlySquareAreas(Set<BlockPosition> brokenBlocks) {
        if (brokenBlocks == null || brokenBlocks.isEmpty()) return false;
        Set<BlockPosition> remaining = new HashSet<>(brokenBlocks);
        while (!remaining.isEmpty()) {
            BlockPosition first = remaining.iterator().next();
            Set<BlockPosition> component = new HashSet<>();
            ArrayDeque<BlockPosition> pending = new ArrayDeque<>();
            pending.add(first);
            remaining.remove(first);
            int minX = first.x(), maxX = first.x(), minZ = first.z(), maxZ = first.z();
            while (!pending.isEmpty()) {
                BlockPosition current = pending.removeFirst();
                component.add(current);
                minX = Math.min(minX, current.x());
                maxX = Math.max(maxX, current.x());
                minZ = Math.min(minZ, current.z());
                maxZ = Math.max(maxZ, current.z());
                for (BlockPosition neighbor : List.of(
                        new BlockPosition(current.x() + 1, current.z()),
                        new BlockPosition(current.x() - 1, current.z()),
                        new BlockPosition(current.x(), current.z() + 1),
                        new BlockPosition(current.x(), current.z() - 1))) {
                    if (remaining.remove(neighbor)) pending.addLast(neighbor);
                }
            }
            int width = maxX - minX + 1;
            int depth = maxZ - minZ + 1;
            if (width < 2 || width != depth || component.size() != width * depth) return false;
        }
        return true;
    }

    /** Each boolean represents one successive elimination, newest last. */
    public static boolean finalEliminationsMatch(List<Boolean> qualifyingEliminations, int required) {
        if (qualifyingEliminations == null || required <= 0
                || qualifyingEliminations.size() < required) return false;
        int start = qualifyingEliminations.size() - required;
        for (int index = start; index < qualifyingEliminations.size(); index++) {
            if (!Boolean.TRUE.equals(qualifyingEliminations.get(index))) return false;
        }
        return true;
    }

    public static boolean brawlersGambit(String kit, boolean won, int sameSmashBlocks,
            boolean finalOpponentFellFromSameSmash) {
        return "brawler".equals(kit) && won && sameSmashBlocks >= 5
                && finalOpponentFellFromSameSmash;
    }

    public static boolean thinIce(boolean won, int remainingBreakableFloorBlocks) {
        return won && remainingBreakableFloorBlocks >= 0 && remainingBreakableFloorBlocks <= 5;
    }
}
