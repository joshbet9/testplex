package au.com.testplex.arcadeports;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Session-scoped overrides for numeric and boolean game rules. */
public final class GameDebugRuntimeOverrides {
    private static volatile Map<String, Object> values = Map.of();
    private static volatile boolean debugSession;
    private static volatile UUID debugOwner;

    private GameDebugRuntimeOverrides() { }

    public static void activate(Map<String, Object> overrides) {
        activate(overrides, null);
    }

    /** Activate profile values for one authorised tester while other players keep defaults. */
    public static void activate(Map<String, Object> overrides, UUID owner) {
        values = overrides == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(overrides));
        debugSession = !values.isEmpty();
        debugOwner = owner;
    }

    /** Update one explicitly immediate profile switch in the active test round. */
    public static void setValue(String key, Object value) {
        if (key == null || key.isBlank() || value == null) return;
        Map<String, Object> updated = new LinkedHashMap<>(values);
        updated.put(key, value);
        values = Map.copyOf(updated);
    }

    /** Remove an explicitly immediate value so the consumer returns to its intended default. */
    public static void removeValue(String key) {
        if (key == null || key.isBlank() || !values.containsKey(key)) return;
        Map<String, Object> updated = new LinkedHashMap<>(values);
        updated.remove(key);
        values = Map.copyOf(updated);
    }

    public static void clear() {
        values = Map.of();
        debugSession = false;
        debugOwner = null;
    }

    /** Clear active rule changes without allowing this test round to persist progression. */
    public static void resetActiveValues() {
        values = Map.of();
        debugSession = true;
    }

    public static boolean isDebugSession() {
        return debugSession;
    }

    /** Mark an already-running round as a test after an authorized live debug action. */
    public static void markDebugSession() {
        debugSession = true;
    }

    public static int intValue(String key, int fallback) {
        Number number = number(key);
        if (number == null) return fallback;
        double value = number.doubleValue();
        if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.round(value);
    }

    public static long longValue(String key, long fallback) {
        Number number = number(key);
        return number == null ? fallback : Math.round(number.doubleValue());
    }

    public static double doubleValue(String key, double fallback) {
        Number number = number(key);
        return number == null ? fallback : number.doubleValue();
    }

    public static float floatValue(String key, float fallback) {
        Number number = number(key);
        return number == null ? fallback : number.floatValue();
    }

    public static boolean booleanValue(String key, boolean fallback) {
        Object value = values.get(key);
        return value instanceof Boolean enabled ? enabled : fallback;
    }

    public static boolean booleanValueFor(UUID player, String key, boolean fallback) {
        return player != null && player.equals(debugOwner) ? booleanValue(key, fallback) : fallback;
    }

    private static Number number(String key) {
        Object value = values.get(key);
        return value instanceof Number number && Double.isFinite(number.doubleValue()) ? number : null;
    }
}
