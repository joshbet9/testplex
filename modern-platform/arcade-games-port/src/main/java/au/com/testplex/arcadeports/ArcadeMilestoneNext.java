package au.com.testplex.arcadeports;

import java.util.Arrays;

/** Read-only milestone track summary; actual completion remains the saved metric. */
final class ArcadeMilestoneNext {
    record Progress(int earnedTiers, int totalTiers, long nextTarget, long remaining, double percent) {
        boolean complete() { return earnedTiers == totalTiers; }
    }

    static Progress of(long recorded, long[] stages) {
        long value = Math.max(0L, recorded);
        long[] milestones = stages == null ? new long[0] : stages;
        int completed = (int) Arrays.stream(milestones).filter(target -> value >= target).count();
        if (completed >= milestones.length) return new Progress(completed, milestones.length, 0, 0, 100.0D);
        long next = milestones[completed];
        return new Progress(completed, milestones.length, next, Math.max(0L, next - value),
                next == 0 ? 100.0D : Math.min(100.0D, value * 100.0D / next));
    }

    private ArcadeMilestoneNext() { }
}
