package me.monstermaze.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures only active Monster Maze monster contact suppresses vanilla damage knockback. */
class MonsterEntityListenerKnockbackTest {
    @Test
    void managedMobBumpsDoNotApplyVanillaKnockbackOnTopOfMazeVelocity() {
        assertTrue(MonsterEntityListener.isMazeContactKnockback(true, true, true));
    }

    @Test
    void noKnockbackOverridesInLobbyBeforeStartOrAfterEnd() {
        assertFalse(MonsterEntityListener.isMazeContactKnockback(false, true, true));
    }

    @Test
    void otherPlayersAndSpectatorsAreNotAffected() {
        assertFalse(MonsterEntityListener.isMazeContactKnockback(true, false, true));
    }

    @Test
    void unrelatedMobsAndOtherDamageSourcesKeepVanillaKnockback() {
        assertFalse(MonsterEntityListener.isMazeContactKnockback(true, true, false));
        assertFalse(MonsterEntityListener.isMazeContactKnockback(false, false, false));
    }
}
