package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WitherAssaultManagerTest {
    @Test
    void controllerClaimsOnlyWitherAssault() {
        WitherAssaultManager wither = new WitherAssaultManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(wither);

        assertSame(wither, registry.get(ArcadePortGame.WITHER_ASSAULT));
        assertNull(registry.get(ArcadePortGame.MILK_THE_COW));
    }

    @Test
    void unpreparedEditorStateCanBeRestoredSafely() {
        WitherAssaultManager wither = new WitherAssaultManager();
        SessionState session = new SessionState(ArcadePortGame.WITHER_ASSAULT, null, List.of(), false);
        wither.restoreEditorBlocks(session);
        wither.restoreEditorBlocks(null);

        assertFalse(wither.isRevivalPotion(null));
        assertFalse(wither.isEditorTunnelPosition(session, null, UUID.randomUUID()));
        assertTrue(session.witherEditorBlockOwners.isEmpty());
    }
}
