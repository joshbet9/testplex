package me.monstermaze.stats;

import org.junit.jupiter.api.Test;
import me.monstermaze.game.MazeMode;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardManagerKitLeaderboardTest {

    @Test
    void parsesKitRowsWithPatternMetadataAndUnorderedFields() {
        List<LeaderboardManager.OverallEntry> rows = LeaderboardManager.parseBoard(
                "[{\"pattern\":2,\"stage\":8,\"kit\":\"jumper\",\"name\":\"Alice\"},"
                        + "{\"name\":\"Bob\",\"kit\":\"jumper\",\"stage\":9}]", -1);

        assertEquals(2, rows.size());
        assertEquals("Alice", rows.get(0).name);
        assertEquals(8, rows.get(0).stage);
        assertEquals(2, rows.get(0).pattern);
        assertEquals(-1, rows.get(1).pattern);
    }

    @Test
    void aggregatesRepeatedPlayerRowsToTheirBestResultAcrossMazes() {
        List<LeaderboardManager.OverallEntry> rows = LeaderboardManager.parseBoard(
                "[{\"name\":\"Alice\",\"kit\":\"jumper\",\"stage\":8,\"pattern\":2},"
                        + "{\"name\":\"alice\",\"kit\":\"jumper\",\"stage\":10,\"pattern\":0},"
                        + "{\"name\":\"Bob\",\"kit\":\"jumper\",\"stage\":9,\"pattern\":1}]", -1);

        List<LeaderboardManager.OverallEntry> ranked = LeaderboardManager.aggregateKitRows(rows);

        assertEquals(2, ranked.size());
        assertEquals("alice", ranked.get(0).name);
        assertEquals(10, ranked.get(0).stage);
        assertEquals(0, ranked.get(0).pattern);
        assertEquals("Bob", ranked.get(1).name);
        assertEquals(9, ranked.get(1).stage);
        assertTrue(LeaderboardManager.patternName(-1).equals("All Mazes"));
    }

    @Test
    void mergesLocalPatternRecordsIntoHostedBoardAndKeepsEachPlayersBestStage() {
        List<LeaderboardManager.OverallEntry> remote = List.of(
                new LeaderboardManager.OverallEntry("Alice", 8, "jumper", 0),
                new LeaderboardManager.OverallEntry("Bob", 9, "frosty", 0));
        List<LeaderboardManager.Entry> local = List.of(
                new LeaderboardManager.Entry("alice", 10, "jumper"),
                new LeaderboardManager.Entry("Bob", 7, "jumper"),
                new LeaderboardManager.Entry("Cara", 6, "repulsor"));

        List<LeaderboardManager.Entry> ranked = LeaderboardManager.mergePatternRows(remote, local, 10);

        assertEquals(3, ranked.size());
        assertEquals("alice", ranked.get(0).name);
        assertEquals(10, ranked.get(0).stage);
        assertEquals("Bob", ranked.get(1).name);
        assertEquals(9, ranked.get(1).stage);
        assertEquals("Cara", ranked.get(2).name);
        assertEquals(6, ranked.get(2).stage);
        assertEquals(1, LeaderboardManager.mergePatternRows(remote, local, 1).size());
    }

    @Test
    void recordsKitBestEvenWhenItIsBelowThePlayersOverallBestOnAnotherKit() {
        YamlConfiguration records = new YamlConfiguration();
        UUID player = UUID.randomUUID();

        assertTrue(LeaderboardManager.storeKitPB(records, MazeMode.MODERN, 0, player, "brawler", 25));
        assertTrue(LeaderboardManager.storeKitPB(records, MazeMode.MODERN, 0, player, "jumper", 12));

        assertEquals(25, LeaderboardManager.storedKitPB(records, MazeMode.MODERN, 0, player, "brawler"));
        assertEquals(12, LeaderboardManager.storedKitPB(records, MazeMode.MODERN, 0, player, "jumper"));
    }

    @Test
    void retainsKitBestPerPatternAndDoesNotLetOneMazeOverwriteAnother() {
        YamlConfiguration records = new YamlConfiguration();
        UUID player = UUID.randomUUID();

        assertTrue(LeaderboardManager.storeKitPB(records, MazeMode.MODERN, 0, player, "jumper", 12));
        assertTrue(LeaderboardManager.storeKitPB(records, MazeMode.MODERN, 2, player, "jumper", 15));
        assertEquals(12, LeaderboardManager.storedKitPB(records, MazeMode.MODERN, 0, player, "jumper"));
        assertEquals(15, LeaderboardManager.storedKitPB(records, MazeMode.MODERN, 2, player, "jumper"));
        assertEquals(0, LeaderboardManager.storedKitPB(records, MazeMode.MODERN, 1, player, "jumper"));
        assertEquals(0, LeaderboardManager.storedKitPB(records, MazeMode.MODERN, 2, player, "frosty"));
        assertFalse(LeaderboardManager.storeKitPB(records, MazeMode.MODERN, 0, player, "jumper", 10));
        assertEquals(12, LeaderboardManager.storedKitPB(records, MazeMode.MODERN, 0, player, "jumper"));
    }

    @Test
    void kitPatternBoardRanksEachKitsRecordEvenWhenAnotherKitIsThePlayersOverallBest() {
        YamlConfiguration records = new YamlConfiguration();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        assertTrue(LeaderboardManager.storeKitPB(records, MazeMode.MODERN, 1, alice, "body-builder", 24));
        assertTrue(LeaderboardManager.storeKitPB(records, MazeMode.MODERN, 1, alice, "jumper", 11));
        assertTrue(LeaderboardManager.storeKitPB(records, MazeMode.MODERN, 1, bob, "jumper", 13));

        List<LeaderboardManager.Entry> local = LeaderboardManager.localKitPatternRows(
                records, MazeMode.MODERN, 1, "jumper",
                player -> player.equals(alice) ? "Alice" : "Bob");
        List<LeaderboardManager.Entry> ranked = LeaderboardManager.mergeKitPatternRows(
                List.of(new LeaderboardManager.OverallEntry("Cara", 12, "jumper", 1),
                        new LeaderboardManager.OverallEntry("Alice", 24, "body-builder", 1)),
                local, "jumper", 1, 10);

        assertEquals(3, ranked.size());
        assertEquals("Bob", ranked.get(0).name);
        assertEquals(13, ranked.get(0).stage);
        assertEquals("Cara", ranked.get(1).name);
        assertEquals(12, ranked.get(1).stage);
        assertEquals("Alice", ranked.get(2).name);
        assertEquals(11, ranked.get(2).stage);
        assertEquals(1, LeaderboardManager.mergeKitPatternRows(null, local, "jumper", 1, 1).size());
    }
}
