package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArcadeCollisionRulesTest {
    @Test
    void dedicatedRaceCollisionTeamReceivesEveryOnlineRosterEntryOnce() {
        assertEquals(List.of("Runner", "TPXTEST01", "Other"),
                ArcadeCollisionRules.participantEntries(
                        Arrays.asList("Runner", "TPXTEST01", "Runner", null, "", "Other")));
    }

    @Test
    void emptyRaceRosterCreatesNoCollisionEntries() {
        assertEquals(List.of(), ArcadeCollisionRules.participantEntries(null));
        assertEquals(List.of(), ArcadeCollisionRules.participantEntries(List.of()));
    }
}
