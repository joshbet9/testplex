package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeLiveJoinRulesTest {
    @Test void exactlyTheFiveRespawningTeamGamesAdmitMidRoundPlayers() {
        assertEquals(EnumSet.of(ArcadePortGame.CASTLE_ASSAULT, ArcadePortGame.HEROES_OF_GWEN,
                        ArcadePortGame.SUPER_SPLOOR, ArcadePortGame.TURF_WARS, ArcadePortGame.SHEEP_QUEST),
                EnumSet.allOf(ArcadePortGame.class).stream()
                        .filter(ArcadeLiveJoinRules::supports)
                        .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(ArcadePortGame.class))));
    }

    @Test void waitForLiveAndReplaceOnlyManagedTestPlayersWhenFull() {
        assertEquals(16, ArcadeLiveJoinRules.MAX_PARTICIPANTS);
        assertEquals(ArcadeLiveJoinRules.Admission.WAIT_FOR_LIVE, ArcadeLiveJoinRules.admission(false, 0, 1));
        assertEquals(ArcadeLiveJoinRules.Admission.READY, ArcadeLiveJoinRules.admission(true, 15, 0));
        assertEquals(ArcadeLiveJoinRules.Admission.REPLACE_TEST_PLAYER, ArcadeLiveJoinRules.admission(true, 16, 1));
        assertEquals(ArcadeLiveJoinRules.Admission.FULL, ArcadeLiveJoinRules.admission(true, 16, 0));
        assertEquals(ArcadeLiveJoinRules.Admission.FULL, ArcadeLiveJoinRules.admission(true, 17, 0));
    }

    @Test void sheepQuestUsesAllFourActualTeamNamesAndColours() {
        String[] names = {"Red", "Aqua", "Green", "Yellow"};
        ChatColor[] colours = {ChatColor.RED, ChatColor.AQUA, ChatColor.GREEN, ChatColor.YELLOW};
        for (int team = 0; team < 4; team++) {
            assertEquals(names[team], ArcadeLiveJoinRules.teamName(ArcadePortGame.SHEEP_QUEST, team));
            assertEquals(colours[team], ArcadeLiveJoinRules.teamColour(ArcadePortGame.SHEEP_QUEST, team));
        }
        for (ArcadePortGame game : EnumSet.of(ArcadePortGame.CASTLE_ASSAULT,
                ArcadePortGame.HEROES_OF_GWEN, ArcadePortGame.SUPER_SPLOOR, ArcadePortGame.TURF_WARS)) {
            assertEquals("Red", ArcadeLiveJoinRules.teamName(game, 0));
            assertEquals("Blue", ArcadeLiveJoinRules.teamName(game, 1));
        }
    }
}
