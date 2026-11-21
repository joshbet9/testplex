package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class DragonPhaseTargetAccessTest {
    @Test
    void resolvesNativeHandleFromEachWrapperRuntimeClass() throws Exception {
        Object dragonHandle = new Object();
        Object playerHandle = new Object();
        FakeCraftDragon dragon = new FakeCraftDragon(dragonHandle);
        FakeCraftPlayer player = new FakeCraftPlayer(playerHandle);

        assertSame(dragonHandle, DragonPhaseTargetAccess.nativeHandle(dragon));
        assertSame(playerHandle, DragonPhaseTargetAccess.nativeHandle(player));
    }

    public static final class FakeCraftDragon {
        private final Object handle;
        FakeCraftDragon(Object handle) { this.handle = handle; }
        public Object getHandle() { return handle; }
    }

    public static final class FakeCraftPlayer {
        private final Object handle;
        FakeCraftPlayer(Object handle) { this.handle = handle; }
        public Object getHandle() { return handle; }
    }
}
