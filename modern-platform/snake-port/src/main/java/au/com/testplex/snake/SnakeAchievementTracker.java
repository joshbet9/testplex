package au.com.testplex.snake;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Match-local, distinct-event tracking for Snake kit and modern-mechanic achievements. */
final class SnakeAchievementTracker {
    private final String kit;
    private final Set<UUID> speedKills = new HashSet<>();
    private final Set<UUID> reverseKillsThisActivation = new HashSet<>();
    private final Set<UUID> protectedTailSegments = new HashSet<>();
    private final Set<String> rouletteEffects = new HashSet<>();
    private boolean reverseActivationQualified;
    private boolean collectedSlime;
    private boolean touchedMagmaCube;

    SnakeAchievementTracker(String kit) {
        this.kit = kit == null ? "" : kit.toLowerCase(Locale.ROOT);
    }

    void recordKill(UUID victim, boolean speedActive, boolean reverseActive) {
        if (victim == null) return;
        if ("speed".equals(kit) && speedActive) speedKills.add(victim);
        if ("reverse".equals(kit) && reverseActive && reverseKillsThisActivation.add(victim)
                && reverseKillsThisActivation.size() >= 2) {
            reverseActivationQualified = true;
        }
    }

    void beginReverseActivation() {
        reverseKillsThisActivation.clear();
    }

    void recordInvulnerableTailCollision(UUID segment) {
        if ("invulnerable".equals(kit) && segment != null) protectedTailSegments.add(segment);
    }

    void recordRouletteEffect(String effect) {
        if (effect != null && !effect.isBlank()) rouletteEffects.add(effect);
    }

    void recordSlimeCollected() { collectedSlime = true; }
    void recordMagmaCubeContact() { touchedMagmaCube = true; }

    boolean earnedTailwind() { return "speed".equals(kit) && speedKills.size() >= 3; }
    boolean earnedOppositeDay() { return "reverse".equals(kit) && reverseActivationQualified; }
    boolean earnedUntouchasnake() {
        return "invulnerable".equals(kit) && protectedTailSegments.size() >= 3;
    }
    boolean earnedLuckyCoil() { return rouletteEffects.size() >= 4; }
    boolean earnedLastCoilStanding() { return !collectedSlime && !touchedMagmaCube; }
}
