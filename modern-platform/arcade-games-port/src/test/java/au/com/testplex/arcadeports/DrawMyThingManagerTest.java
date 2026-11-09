package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class DrawMyThingManagerTest {
    @Test
    void controllerClaimsOnlyDrawMyThing() {
        DrawMyThingManager draw = new DrawMyThingManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(draw);

        assertSame(draw, registry.get(ArcadePortGame.DRAW_MY_THING));
        assertNull(registry.get(ArcadePortGame.MASTER_BUILDERS));
    }

    @Test
    void wordSelectionRecordsUsedWords() {
        DrawMyThingManager draw = new DrawMyThingManager();
        SessionState session = new SessionState(ArcadePortGame.DRAW_MY_THING, null, List.of(), false);

        String selected = draw.chooseWord(session);

        assertTrue(DrawMyThingContent.WORDS.contains(selected));
        assertTrue(session.drawUsedWords.contains(selected));
    }

    @Test
    void hintMaskKeepsSpacesAndOnlyRevealsSelectedLetters() {
        assertEquals("C _ _   D _ _ ", DrawMyThingManager.hintMask("CAT DOG", Set.of(0, 4)));
        assertEquals("_ _ _ ", DrawMyThingManager.hintMask("CAT", Set.of()));
    }

    @Test
    void paletteLabelsMatchLegacyMaterialNames() {
        assertEquals("light blue", DrawMyThingManager.colourName(Material.LIGHT_BLUE_WOOL));
        assertEquals("red", DrawMyThingManager.colourName(Material.RED_TERRACOTTA));
        assertEquals("black", DrawMyThingManager.colourName(Material.BLACK_CONCRETE_POWDER));
    }

    @Test
    void disabledPaletteAndCanvasToolsDoNotChangeDrawingState() {
        DrawMyThingManager draw = new DrawMyThingManager();
        SessionState session = new SessionState(ArcadePortGame.DRAW_MY_THING, null, List.of(), false);
        UUID playerId = UUID.randomUUID();
        session.drawBrushColours.put(playerId, Material.BLUE_WOOL);
        Player player = playerStub();

        draw.selectColour(session, player, Material.RED_WOOL, "red", false);
        draw.performToolAction(session, player, null, "Pencil", false);

        assertEquals(Material.BLUE_WOOL, session.drawBrushColours.get(playerId));
        assertFalse(session.drawStartedPainting);
    }

    private static Player playerStub() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "Draw My Thing test player";
                    default -> method.getReturnType() == boolean.class ? false
                            : method.getReturnType() == int.class ? 0
                            : method.getReturnType() == long.class ? 0L
                            : method.getReturnType() == double.class ? 0.0D
                            : method.getReturnType() == float.class ? 0.0F
                            : method.getReturnType() == short.class ? (short) 0
                            : method.getReturnType() == byte.class ? (byte) 0
                            : method.getReturnType() == char.class ? '\0' : null;
                });
    }

    @Test
    void canvasMethodsAreSafeBeforeMapPreparation() {
        DrawMyThingManager draw = new DrawMyThingManager();
        SessionState session = new SessionState(ArcadePortGame.DRAW_MY_THING, null, List.of(), false);
        assertFalse(draw.isPaletteSwatch(session, null, null));
        assertEquals(0, draw.countPaintedBlocks(session));
    }
}
