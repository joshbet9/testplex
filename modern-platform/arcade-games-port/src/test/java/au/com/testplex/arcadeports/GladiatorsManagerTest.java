package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Proxy;
import org.bukkit.entity.Player;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class GladiatorsManagerTest {
    @Test
    void controllerClaimsOnlyGladiators() {
        GladiatorsManager gladiators = new GladiatorsManager(Logger.getAnonymousLogger());
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(gladiators);

        assertSame(gladiators, registry.get(ArcadePortGame.GLADIATORS));
        assertNull(registry.get(ArcadePortGame.DRAGON_ESCAPE));
    }

    @Test
    void winnerAchievementRulesUseGameStateAndDelegatePersistence() {
        GladiatorsManager gladiators = new GladiatorsManager(Logger.getAnonymousLogger());
        SessionState session = new SessionState(ArcadePortGame.GLADIATORS, null, List.of(), false);
        UUID id = UUID.randomUUID();
        Player winner = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "Gladiators test player";
                    default -> defaultValue(method.getReturnType());
                });
        session.gladiatorArrowsShot.put(id, 3);
        session.gladiatorArrowsHit.put(id, 3);
        List<String> catalogAwards = new ArrayList<>();
        List<String> legacyAwards = new ArrayList<>();
        List<String> stats = new ArrayList<>();
        List<GladiatorsMode> modes = new ArrayList<>();
        GladiatorsManager.AchievementSink sink = new GladiatorsManager.AchievementSink() {
            @Override public void awardCatalog(Player player, String achievementId) {
                assertSame(winner, player);
                catalogAwards.add(achievementId);
            }
            @Override public void awardLegacy(Player player, String achievementId, String displayName) {
                assertEquals(winner, player);
                legacyAwards.add(achievementId);
            }
            @Override public void recordModeWin(Player player, GladiatorsMode mode) {
                modes.add(mode);
            }
            @Override public void incrementLifetimeStat(Player player, String stat, int amount,
                    int achievementThreshold, String achievementId) {
                stats.add(stat + ":" + amount + ":" + achievementThreshold + ":" + achievementId);
            }
        };

        gladiators.recordWinnerAchievements(session, winner, GladiatorsMode.NORMAL, 0, false, sink);

        assertTrue(catalogAwards.contains("man-of-integrity"));
        assertEquals(List.of("are-you-not-entertained"), legacyAwards);
        assertEquals(List.of(GladiatorsMode.NORMAL), modes);
        assertEquals(List.of("precision:1:1:precision"), stats);

        session.gladiatorUsedRod.add(id);
        catalogAwards.clear();
        legacyAwards.clear();
        gladiators.recordWinnerAchievements(session, winner, GladiatorsMode.NORMAL, 0, false, sink);
        assertFalse(catalogAwards.contains("man-of-integrity"));
        assertTrue(legacyAwards.isEmpty());
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return Boolean.FALSE;
        if (type == int.class) return Integer.valueOf(0);
        if (type == long.class) return Long.valueOf(0L);
        if (type == double.class) return Double.valueOf(0.0D);
        if (type == float.class) return Float.valueOf(0.0F);
        if (type == short.class) return Short.valueOf((short) 0);
        if (type == byte.class) return Byte.valueOf((byte) 0);
        if (type == char.class) return Character.valueOf((char) 0);
        return null;
    }

    @Test
    void prePrepareUpdatesAndCleanupAreSafe() {
        GladiatorsManager gladiators = new GladiatorsManager(Logger.getAnonymousLogger());
        SessionState session = new SessionState(ArcadePortGame.GLADIATORS, null, List.of(), false);

        gladiators.updateScoreboard(session, 500L);
        gladiators.cleanupScoreboard(session);
        gladiators.updateScoreboard(null, 500L);

        assertTrue(session.gladiatorScoreboardEntries.isEmpty());
    }
}
