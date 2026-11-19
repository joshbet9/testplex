package me.monstermaze.kit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KitManagerJumperChargeRulesTest {
    @Test
    void doesNotRefillSpentChargesOnOrdinarySafePads() {
        assertEquals(0, KitManager.jumperChargesAfterPad(0, 1));
        assertEquals(1, KitManager.jumperChargesAfterPad(1, 2));
        assertEquals(0, KitManager.jumperChargesAfterPad(0, 3));
        assertEquals(2, KitManager.jumperChargesAfterPad(2, 4));
        assertEquals(1, KitManager.jumperChargesAfterPad(1, 6));
        assertEquals(0, KitManager.jumperChargesAfterPad(0, 9));
    }

    @Test
    void addsExactlyOneChargeAtEachFifthSafePad() {
        assertEquals(1, KitManager.jumperChargesAfterPad(0, 5));
        assertEquals(2, KitManager.jumperChargesAfterPad(1, 5));
        assertEquals(1, KitManager.jumperChargesAfterPad(0, 10));
        assertEquals(1, KitManager.jumperChargesAfterPad(0, 15));
        assertEquals(64, KitManager.jumperChargesAfterPad(64, 5));
        assertEquals(0, KitManager.jumperChargesAfterPad(0, 0));
    }
}
