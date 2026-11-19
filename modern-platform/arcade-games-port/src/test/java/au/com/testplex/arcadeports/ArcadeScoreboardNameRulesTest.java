package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class ArcadeScoreboardNameRulesTest {
    @Test
    void viewerOwnsGreenNameWithoutChangingOtherPlayersTeamColor() {
        UUID viewer = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();
        assertEquals(ChatColor.GREEN, ArcadeScoreboardNameRules.color(viewer, viewer, ChatColor.AQUA));
        assertEquals(ChatColor.AQUA, ArcadeScoreboardNameRules.color(viewer, teammate, ChatColor.AQUA));
        assertEquals(ChatColor.GREEN + "Viewer", ArcadeScoreboardNameRules.format(
                viewer, viewer, "Viewer", ChatColor.WHITE));
        assertEquals(ChatColor.RED + "Opponent", ArcadeScoreboardNameRules.format(
                viewer, teammate, "Opponent", ChatColor.RED));
    }

    @Test
    void missingViewerDoesNotRecolorAnyName() {
        UUID player = UUID.randomUUID();
        assertEquals(ChatColor.AQUA, ArcadeScoreboardNameRules.color(null, player, ChatColor.AQUA));
        assertEquals(ChatColor.AQUA + "Player", ArcadeScoreboardNameRules.format(
                null, player, "Player", ChatColor.AQUA));
    }
}
