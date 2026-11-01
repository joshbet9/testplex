package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MineplexCoreRankBridgeTest {
    @Test
    void mapsEveryMineplexPrimaryGroupToItsLegacyLobbyRankCategory() {
        assertEquals("owner", MineplexCoreRankBridge.rankIdForGroup("OWNER"));

        assertEquals("admin", MineplexCoreRankBridge.rankIdForGroup("ADMIN"));
        assertEquals("admin", MineplexCoreRankBridge.rankIdForGroup("DEV"));
        assertEquals("admin", MineplexCoreRankBridge.rankIdForGroup("LT"));

        assertEquals("mod", MineplexCoreRankBridge.rankIdForGroup("MOD"));
        assertEquals("mod", MineplexCoreRankBridge.rankIdForGroup("SRMOD"));
        assertEquals("helper", MineplexCoreRankBridge.rankIdForGroup("TRAINEE"));
        assertEquals("helper", MineplexCoreRankBridge.rankIdForGroup("SUPPORT"));

        assertEquals("ultra", MineplexCoreRankBridge.rankIdForGroup("ULTRA"));
        assertEquals("ultra", MineplexCoreRankBridge.rankIdForGroup("HERO"));
        assertEquals("ultra", MineplexCoreRankBridge.rankIdForGroup("LEGEND"));
        assertEquals("ultra", MineplexCoreRankBridge.rankIdForGroup("TITAN"));
        assertEquals("ultra", MineplexCoreRankBridge.rankIdForGroup("ETERNAL"));

        // These remain distinguishable by their exact PermissionGroup suffix in the
        // tab-team key even though they share the ordinary player sort category.
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("PLAYER"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("TWITCH"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("YT"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("YOUTUBE"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("BUILDER"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("MAPPER"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("MAPLEAD"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup("UNKNOWN"));
        assertEquals("player", MineplexCoreRankBridge.rankIdForGroup(null));
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
