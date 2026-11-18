package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.bukkit.ChatColor;

/** Viewer-local, compact Dragon Escape sidebar layout and racer styling. */
final class DragonEscapeRaceScoreboardLayout {
    static final int MAX_ROWS = 12;
    static final int RACER_ROW_COUNT = 6;
    static final int STANDINGS_SEPARATOR_ROW = RACER_ROW_COUNT;
    static final int MAP_PB_ROW = STANDINGS_SEPARATOR_ROW + 1;
    static final int KIT_PB_ROW = MAP_PB_ROW + 1;
    static final int FINISH_SEPARATOR_ROW = KIT_PB_ROW + 1;
    static final int FINISH_TIMER_LABEL_ROW = FINISH_SEPARATOR_ROW + 1;
    static final int FINISH_TIMER_VALUE_ROW = FINISH_TIMER_LABEL_ROW + 1;

    enum RacerStatus { RUNNING, FINISHED, ELIMINATED }

    private DragonEscapeRaceScoreboardLayout() { }

    /**
     * The leading three racers and the viewer's nearest neighbours are always
     * included, in actual placement order. Overlap never produces duplicates.
     */
    static List<Integer> visibleRankPositions(int racerCount, int viewerIndex) {
        if (racerCount <= 0) return List.of();
        TreeSet<Integer> positions = new TreeSet<>();
        for (int index = 0; index < Math.min(3, racerCount); index++) positions.add(index);
        if (viewerIndex >= 0 && viewerIndex < racerCount) {
            if (viewerIndex > 0) positions.add(viewerIndex - 1);
            positions.add(viewerIndex);
            if (viewerIndex + 1 < racerCount) positions.add(viewerIndex + 1);
        }
        return new ArrayList<>(positions);
    }

    static long displayedElapsedMillis(long elapsedMillis, boolean firstLiveFrame) {
        return firstLiveFrame ? 0L : Math.max(0L, elapsedMillis);
    }

    static long remainingFinishWindowMillis(long deadlineMillis, long nowMillis) {
        return deadlineMillis <= 0L ? 0L : Math.max(0L, deadlineMillis - nowMillis);
    }

    static String finishWindowSeconds(long remainingMillis) {
        return String.format(java.util.Locale.ROOT, "%.1f Seconds", Math.max(0L, remainingMillis) / 1000.0D);
    }

    static String kitRecordLabel(String kit, boolean worldRecord) {
        String normalized = ArcadePortRules.normalizeDragonEscapeKit(kit);
        String kitName = switch (normalized == null ? "" : normalized) {
            case "builder" -> "Builder";
            case "digger" -> "Digger";
            case "disruptor" -> "Disruptor";
            case "jumper" -> "Jumper";
            case "warper" -> "Warper";
            case "flutterfly" -> "Flutterfly";
            case "acrobat" -> "Acrobat";
            default -> "Kit";
        };
        return kitName + (worldRecord ? " WR" : " PB");
    }

    static String racerName(String name, RacerStatus status) {
        return color(status).toString() + strikethrough(status) + name;
    }

    static String racerName(String name, RacerStatus status, int placement, boolean ownName) {
        return (ownName ? ChatColor.GREEN : color(status)).toString()
                + strikethrough(status) + placement + ". " + name;
    }

    static String racerTime(String value, RacerStatus status) {
        return racerTime(value, status, false);
    }

    static String racerTime(String value, RacerStatus status, boolean firstPlace) {
        if (status == RacerStatus.ELIMINATED) return "";
        ChatColor timeColor = status == RacerStatus.RUNNING
                ? firstPlace ? ChatColor.GREEN : ChatColor.RED : color(status);
        return timeColor.toString() + " " + value + ChatColor.RESET;
    }

    private static String strikethrough(RacerStatus status) {
        return status == RacerStatus.ELIMINATED ? ChatColor.STRIKETHROUGH.toString() : "";
    }

    private static ChatColor color(RacerStatus status) {
        return switch (status) {
            case RUNNING -> ChatColor.YELLOW;
            case FINISHED -> ChatColor.AQUA;
            case ELIMINATED -> ChatColor.GRAY;
        };
    }
}
