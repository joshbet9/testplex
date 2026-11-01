package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MineplexCoreRankBridgeTest {
    @Test
    void mapsMineplexPrimaryGroupsToTheLegacyLobbyRankCategories() {
        assertEquals("owner", MineplexCoreRankBridge.rankIdForGroup("OWNER"));
        assertEquals("admin", MineplexCoreRankBridge.rankIdForGroup("ADMIN"));
        assertEquals("admin", MineplexCoreRankBridge.rankIdForGroup("DEV"));
        assertEquals("admin", MineplexCoreRankBridge.rankIdForGroup("LT"));
        assertEquals("mod", MineplexCoreRankBridge.rankIdForGroup("MOD"));
        assertEquals("mod", MineplexCoreRankBridge.rankIdForGroup("SRMOD"));
        assertEquals("helper", MineplexCoreRankBridge.rankIdForGroup("TRAINEE"));
        assertEquals("helper", MineplexCoreRankBridge.rankIdForGroup("SUPPORT"));
        assertEquals("ultra", MineplexCoreRankBridge.rankIdForGroup("ULTRA"));
        assertEquals("ultra", MineplexCoreRankBridge.rankIdForGroup("LEGEND"));
    }

    @Test
    void preservesTheExactLegacyPermissionGroupPrefixAndTeamName() {
        MineplexCoreRankBridge.RankPresentation owner =
                MineplexCoreRankBridge.presentationForGroup(
                        "OWNER", "Owner", ChatColor.DARK_RED + "" + ChatColor.BOLD + "OWNER");
        assertEquals("OWNER", owner.groupName());
        assertEquals("owner", owner.rankId());
        assertEquals(ChatColor.DARK_RED + "" + ChatColor.BOLD + "OWNER"
                + ChatColor.RESET + " ", owner.prefix());

        MineplexCoreRankBridge.RankPresentation player =
                MineplexCoreRankBridge.presentationForGroup("PLAYER", "", "");
        assertEquals("PLAYER", player.groupName());
        assertEquals("", player.prefix());
    }

    @Test
    void unknownAndNonPrimaryStaffGroupsUseThePlayerTabCategory() {
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("PLAYER"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("BUILDER"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("MAPPER"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup(null));
    }
}
