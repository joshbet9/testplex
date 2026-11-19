package me.monstermaze.game;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerLiveDamageStateTest {
    @Test
    void startingLiveDropsLobbyInvulnerabilityAndStaleHurtTicks() {
        AtomicBoolean protectedFromDamage = new AtomicBoolean(true);
        AtomicInteger noDamageTicks = new AtomicInteger(40);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[]{Player.class}, (instance, method, args) -> {
                    switch (method.getName()) {
                        case "setInvulnerable" -> protectedFromDamage.set((Boolean) args[0]);
                        case "setNoDamageTicks" -> noDamageTicks.set((Integer) args[0]);
                        default -> { }
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });

        GameManager.enableLiveMobDamage(player);

        assertFalse(protectedFromDamage.get(),
                "An invulnerable lobby player cannot take the intended two hearts of mob damage");
        assertEquals(0, noDamageTicks.get(),
                "Stale immunity from the previous match must not suppress the first monster hit");
    }

    @Test
    void nullPlayerIsSafe() {
        assertDoesNotThrow(() -> GameManager.enableLiveMobDamage(null));
    }
}
