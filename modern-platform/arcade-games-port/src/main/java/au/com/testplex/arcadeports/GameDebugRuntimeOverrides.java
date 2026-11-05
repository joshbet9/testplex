package au.com.testplex.arcadeports;

import java.util.LinkedHashMap;
import java.util.Map;

/** Session-scoped overrides for numeric and boolean game rules. */
public final class GameDebugRuntimeOverrides {
    private static volatile Map<String, Object> values = Map.of();
    private static volatile boolean debugSession;

    private GameDebugRuntimeOverrides() { }

    public static void activate(Map<String, Object> overrides) {
        values = overrides == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(overrides));
        debugSession = !values.isEmpty();
    }

    public static void clear() {
        values = Map.of();
        debugSession = false;
    }

    /** Clear active rule changes without allowing this test round to persist progression. */
    public static void resetActiveValues() {
        values = Map.of();
        debugSession = true;
    }

    public static boolean isDebugSession() {
        return debugSession;
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

    private static Number number(String key) {
        Object value = values.get(key);
        return value instanceof Number number && Double.isFinite(number.doubleValue()) ? number : null;
    }
}
