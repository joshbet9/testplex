package au.com.testplex.arcadeports;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadeCollisionRulesTest {

    @Test
    void passThroughDisablesPlayerPushingWithoutChangingPlayerHitboxFlag() {
        AtomicBoolean playerCollisionFlagTouched = new AtomicBoolean(false);
        AtomicBoolean teamRuleDisabled = new AtomicBoolean(false);
        Team team = proxy(Team.class, (method, args) -> {
            if (method.equals("getOption")) return Team.OptionStatus.ALWAYS;
            if (method.equals("setOption") && args[0] == Team.Option.COLLISION_RULE) {
                teamRuleDisabled.set(args[1] == Team.OptionStatus.NEVER);
            }
            return null;
        });
        Scoreboard scoreboard = proxy(Scoreboard.class, (method, args) -> switch (method) {
            case "getTeams" -> Set.of(team);
            case "getEntryTeam" -> team;
            case "getTeam" -> null;
            default -> null;
        });
        Player player = proxy(Player.class, (method, args) -> switch (method) {
            case "isOnline" -> true;
            case "isCollidable" -> {
                playerCollisionFlagTouched.set(true);
                yield false;
            }
            case "setCollidable" -> {
                playerCollisionFlagTouched.set(true);
                yield null;
            }
            case "getScoreboard" -> scoreboard;
            default -> null;
        });

        ArcadeCollisionRules.disable(player);

        assertFalse(playerCollisionFlagTouched.get(),
                "player pass-through must not change the entity hitbox flag used by native hit resolution");
        assertTrue(teamRuleDisabled.get(),
                "scoreboard collision rule must still let players pass through one another");
    }

    @Test
    void assigningReplacementScoreboardImmediatelyDisablesCollisionOnItsTeams() {
        AtomicBoolean assigned = new AtomicBoolean(false);
        AtomicBoolean collisionRuleDisabled = new AtomicBoolean(false);
        Team replacementTeam = proxy(Team.class, (method, args) -> {
            if (method.equals("getOption")) return Team.OptionStatus.ALWAYS;
            if (method.equals("setOption") && args[0] == Team.Option.COLLISION_RULE) {
                collisionRuleDisabled.set(args[1] == Team.OptionStatus.NEVER);
            }
            return null;
        });
        Scoreboard replacement = proxy(Scoreboard.class, (method, args) -> switch (method) {
            case "getTeams" -> Set.of(replacementTeam);
            case "getEntryTeam" -> replacementTeam;
            case "getTeam" -> null;
            default -> null;
        });
        Player player = proxy(Player.class, (method, args) -> switch (method) {
            case "isOnline" -> true;
            case "setScoreboard" -> {
                assigned.set(args[0] == replacement);
                yield null;
            }
            case "getScoreboard" -> replacement;
            default -> null;
        });

        ArcadeCollisionRules.assign(player, replacement);

        assertTrue(assigned.get(), "the replacement game scoreboard must be installed");
        assertTrue(collisionRuleDisabled.get(),
                "all teams on a replacement scoreboard must immediately preserve player pass-through");
    }

    private static <T> T proxy(Class<T> type, ProxyCall call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + " test proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            return call.invoke(method.getName(), args == null ? new Object[0] : args);
        }));
    }

    @FunctionalInterface
    private interface ProxyCall {
        Object invoke(String method, Object[] args);
    }
}
