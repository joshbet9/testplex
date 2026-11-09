package au.com.testplex.arcadeports;

import java.util.List;

/** Projects a runner onto the recorded PB route and interpolates its recorded time. */
final class DragonEscapeDynamicPbTime {
    private DragonEscapeDynamicPbTime() { }

    record Point(long millis, double x, double y, double z) { }

    static long expectedMillis(List<Point> track, double x, double y, double z) {
        if (track == null || track.isEmpty()) return -1L;
        if (track.size() == 1) return Math.max(0L, track.get(0).millis());
        double bestDistance = Double.POSITIVE_INFINITY;
        double bestTime = Math.max(0L, track.get(0).millis());
        for (int i = 0; i < track.size() - 1; i++) {
            Point from = track.get(i);
            Point to = track.get(i + 1);
            double dx = to.x() - from.x();
            double dy = to.y() - from.y();
            double dz = to.z() - from.z();
            double lengthSquared = dx * dx + dy * dy + dz * dz;
            double fraction = lengthSquared < 1.0e-9 ? 0.0D
                    : Math.max(0.0D, Math.min(1.0D,
                            ((x - from.x()) * dx + (y - from.y()) * dy + (z - from.z()) * dz)
                                    / lengthSquared));
            double px = from.x() + dx * fraction - x;
            double py = from.y() + dy * fraction - y;
            double pz = from.z() + dz * fraction - z;
            double distance = px * px + py * py + pz * pz;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTime = from.millis() + (to.millis() - from.millis()) * fraction;
            }
        }
        return Math.max(0L, Math.round(bestTime));
    }
}
