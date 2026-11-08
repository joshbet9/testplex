package au.com.testplex.arcadeports;

import java.util.Map;
import java.util.UUID;

/** Heroes of Gwen's team-dependent tower health scaling and preservation rules. */
final class HeroesOfGwenManager {
    double towerMaximumHealth(Map<UUID, Integer> teams, int owner, boolean first) {
        long attackers = teams.values().stream().filter(team -> team != null && team == 1 - owner).count();
        return ArcadePortRules.heroesOfGwenTowerMaximumHealth(first, (int) Math.max(1L, attackers));
    }

    double rescaleTowerHealth(double previousHealth, double previousMaximum, double newMaximum) {
        double remainingFraction = previousMaximum <= 0.0D ? 1.0D : previousHealth / previousMaximum;
        return Math.max(1.0D, Math.ceil(newMaximum * remainingFraction));
    }
}
