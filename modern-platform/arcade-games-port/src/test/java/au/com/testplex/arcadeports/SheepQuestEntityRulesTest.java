package au.com.testplex.arcadeports;

import org.bukkit.entity.Sheep;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

final class SheepQuestEntityRulesTest {
    @Test void hidesSpawnSheepNameTagWithoutChangingOtherEntityState() {
        AtomicReference<String> customName = new AtomicReference<>("Sheep");
        AtomicBoolean visible = new AtomicBoolean(true);
        Sheep sheep = (Sheep) Proxy.newProxyInstance(Sheep.class.getClassLoader(), new Class<?>[] {Sheep.class},
                (proxy, method, args) -> {
                    if ("setCustomName".equals(method.getName())) customName.set((String) args[0]);
                    if ("setCustomNameVisible".equals(method.getName())) visible.set((Boolean) args[0]);
                    return null;
                });

        SheepQuestEntityRules.hideNameTag(sheep);

        assertNull(customName.get());
        assertFalse(visible.get());
    }
}
