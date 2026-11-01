package au.com.testplex.arcadeports;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Advances absolute timestamps on a session by the amount of time spent in Prepare.
 * The game loop and player input are paused during that phase, so deadlines and
 * cooldowns must be shifted together when the session enters Live.
 */
final class SessionTimestampShifter {
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "startedat", "endsat", "prepareendsat");

    private SessionTimestampShifter() {
    }

    /**
     * Returns field paths which could not be adjusted. A non-positive value is a
     * sentinel and remains unchanged; zero/negative paused time is a no-op.
     */
    static List<String> shift(Object target, long pausedMillis) {
        if (target == null || pausedMillis <= 0L) return List.of();
        List<String> failures = new ArrayList<>();
        for (Field field : target.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (!isTimestampField(name)) continue;
            try {
                field.setAccessible(true);
                if (field.getType() == long.class) {
                    long value = field.getLong(target);
                    field.setLong(target, ArcadeLobbyLifecycleRules.shiftPausedTimestamp(value, pausedMillis));
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    Object value = field.get(target);
                    if (value instanceof Map<?, ?> timestamps) {
                        shiftTimestampMap(timestamps, pausedMillis, field.getName(), failures);
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                failures.add(field.getName() + ": " + exception.getMessage());
            }
        }
        return List.copyOf(failures);
    }

    private static boolean isTimestampField(String name) {
        if (EXCLUDED_FIELDS.contains(name)) return false;
        return name.endsWith("at") || name.endsWith("until")
                || name.contains("atby") || name.contains("untilby")
                || name.contains("cooldown") || name.contains("deadline");
    }

    @SuppressWarnings("unchecked")
    private static void shiftTimestampMap(
            Map<?, ?> source, long pausedMillis, String path, List<String> failures) {
        Map<Object, Object> timestamps = (Map<Object, Object>) source;
        for (Map.Entry<Object, Object> entry : timestamps.entrySet()) {
            Object value = entry.getValue();
            String entryPath = path + "[" + entry.getKey() + "]";
            try {
                if (value instanceof Long timestamp) {
                    entry.setValue(ArcadeLobbyLifecycleRules.shiftPausedTimestamp(timestamp, pausedMillis));
                } else if (value instanceof Map<?, ?> nested) {
                    shiftTimestampMap(nested, pausedMillis, entryPath, failures);
                }
            } catch (RuntimeException exception) {
                failures.add(entryPath + ": " + exception.getMessage());
            }
        }
    }
}
