package au.com.testplex.arcadeports;

import java.util.UUID;
import org.bukkit.ChatColor;

/** Per-viewer name styling for individual names shown in match sidebars. */
public final class ArcadeScoreboardNameRules {
    private ArcadeScoreboardNameRules() { }

    public static ChatColor color(UUID viewerId, UUID namedPlayerId, ChatColor normalColor) {
        return viewerId != null && viewerId.equals(namedPlayerId) ? ChatColor.GREEN : normalColor;
    }

    public static String format(UUID viewerId, UUID namedPlayerId, String name, ChatColor normalColor) {
        if (name == null) return "";
        return color(viewerId, namedPlayerId, normalColor) + name;
    }
}
