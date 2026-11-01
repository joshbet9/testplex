package au.com.testplex.arcadeports;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
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
     *
     * The session also owns a few timer carriers nested inside maps/lists. Those
     * project-local values are traversed, while Bukkit entities and other external
     * objects are deliberately left alone.
     */
    static List<String> shift(Object target, long pausedMillis) {
        if (target == null || pausedMillis <= 0L) return List.of();
        List<String> failures = new ArrayList<>();
        Class<?> ownerClass = target.getClass().getEnclosingClass() == null
                ? target.getClass() : target.getClass().getEnclosingClass();
        shiftObject(target, pausedMillis, ownerClass, failures, new IdentityHashMap<>(), "session");
        return List.copyOf(failures);
    }

    private static void shiftObject(
            Object target,
            long pausedMillis,
            Class<?> ownerClass,
            List<String> failures,
            IdentityHashMap<Object, Boolean> visited,
            String path) {
        if (target == null || visited.put(target, Boolean.TRUE) != null) return;
        Class<?> type = target.getClass();
        if (type.isRecord()) return;

        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            String name = field.getName().toLowerCase(Locale.ROOT);
            String fieldPath = path + "." + field.getName();
            try {
                field.setAccessible(true);
                Object value = field.get(target);
                if (field.getType() == long.class && isTimestampField(name)) {
                    long timestamp = field.getLong(target);
                    field.setLong(target,
                            ArcadeLobbyLifecycleRules.shiftPausedTimestamp(timestamp, pausedMillis));
                } else if (value instanceof Map<?, ?> map) {
                    shiftMap(map, pausedMillis, isTimestampField(name),
                            ownerClass, failures, visited, fieldPath);
                } else if (value instanceof Collection<?> collection) {
                    shiftCollection(collection, pausedMillis, ownerClass, failures, visited, fieldPath);
                } else if (belongsToOwner(value, ownerClass)) {
                    shiftObject(value, pausedMillis, ownerClass, failures, visited, fieldPath);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                failures.add(fieldPath + ": " + exception.getMessage());
            }
        }
    }

    private static boolean isTimestampField(String name) {
        if (EXCLUDED_FIELDS.contains(name)) return false;
        return name.endsWith("at") || name.endsWith("until")
                || name.contains("atby") || name.contains("untilby")
                || name.contains("cooldown") || name.contains("deadline")
                || name.contains("expiresat") || name.contains("expiresatmillis");
    }

    private static void shiftMap(
            Map<?, ?> source,
            long pausedMillis,
            boolean timestampValues,
            Class<?> ownerClass,
            List<String> failures,
            IdentityHashMap<Object, Boolean> visited,
            String path) {
        if (visited.put(source, Boolean.TRUE) != null) return;
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            String entryPath = path + "[" + entry.getKey() + "]";
            try {
                if (value instanceof Long timestamp && timestampValues) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> writable = (Map<Object, Object>) source;
                    writable.put(entry.getKey(),
                            ArcadeLobbyLifecycleRules.shiftPausedTimestamp(timestamp, pausedMillis));
                } else if (value instanceof Map<?, ?> nested) {
                    shiftMap(nested, pausedMillis, timestampValues,
                            ownerClass, failures, visited, entryPath);
                } else if (value instanceof Collection<?> collection) {
                    shiftCollection(collection, pausedMillis, ownerClass, failures, visited, entryPath);
                } else if (isRecordOwnedBy(value, ownerClass)) {
                    Object shifted = shiftRecord(value, pausedMillis, ownerClass, failures, visited, entryPath);
                    if (shifted != value) {
                        @SuppressWarnings("unchecked")
                        Map<Object, Object> writable = (Map<Object, Object>) source;
                        writable.put(entry.getKey(), shifted);
                    }
                } else if (belongsToOwner(value, ownerClass)) {
                    shiftObject(value, pausedMillis, ownerClass, failures, visited, entryPath);
                }
            } catch (RuntimeException exception) {
                failures.add(entryPath + ": " + exception.getMessage());
            }
        }
    }

    private static void shiftCollection(
            Collection<?> source,
            long pausedMillis,
            Class<?> ownerClass,
            List<String> failures,
            IdentityHashMap<Object, Boolean> visited,
            String path) {
        if (visited.put(source, Boolean.TRUE) != null) return;
        if (source instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Object> writable = (List<Object>) list;
            for (int index = 0; index < writable.size(); index++) {
                Object value = writable.get(index);
                String entryPath = path + "[" + index + "]";
                if (isRecordOwnedBy(value, ownerClass)) {
                    Object shifted = shiftRecord(value, pausedMillis, ownerClass, failures, visited, entryPath);
                    if (shifted != value) writable.set(index, shifted);
                } else if (value instanceof Map<?, ?> nested) {
                    shiftMap(nested, pausedMillis, false, ownerClass, failures, visited, entryPath);
                } else if (value instanceof Collection<?> nested) {
                    shiftCollection(nested, pausedMillis, ownerClass, failures, visited, entryPath);
                } else if (belongsToOwner(value, ownerClass)) {
                    shiftObject(value, pausedMillis, ownerClass, failures, visited, entryPath);
                }
            }
        } else {
            for (Object value : source) {
                if (belongsToOwner(value, ownerClass) && !value.getClass().isRecord()) {
                    shiftObject(value, pausedMillis, ownerClass, failures, visited, path);
                }
            }
        }
    }

    private static boolean belongsToOwner(Object value, Class<?> ownerClass) {
        return value != null && ownerClass != null
                && value.getClass().getEnclosingClass() == ownerClass;
    }

    private static boolean isRecordOwnedBy(Object value, Class<?> ownerClass) {
        return value != null && value.getClass().isRecord()
                && belongsToOwner(value, ownerClass);
    }

    private static Object shiftRecord(
            Object record,
            long pausedMillis,
            Class<?> ownerClass,
            List<String> failures,
            IdentityHashMap<Object, Boolean> visited,
            String path) {
        if (visited.put(record, Boolean.TRUE) != null) return record;
        Class<?> type = record.getClass();
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Object[] arguments = new Object[components.length];
        try {
            for (int index = 0; index < components.length; index++) {
                RecordComponent component = components[index];
                parameterTypes[index] = component.getType();
                java.lang.reflect.Method accessor = component.getAccessor();
                accessor.setAccessible(true);
                Object value = accessor.invoke(record);
                String componentName = component.getName().toLowerCase(Locale.ROOT);
                if (component.getType() == long.class && isTimestampField(componentName)) {
                    value = ArcadeLobbyLifecycleRules.shiftPausedTimestamp((long) value, pausedMillis);
                } else if (value instanceof Map<?, ?> nested) {
                    shiftMap(nested, pausedMillis, isTimestampField(componentName),
                            ownerClass, failures, visited, path + "." + component.getName());
                } else if (value instanceof Collection<?> collection) {
                    shiftCollection(collection, pausedMillis, ownerClass, failures, visited,
                            path + "." + component.getName());
                } else if (belongsToOwner(value, ownerClass)) {
                    shiftObject(value, pausedMillis, ownerClass, failures, visited,
                            path + "." + component.getName());
                }
                arguments[index] = value;
            }
            Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            failures.add(path + ": " + exception.getMessage());
            return record;
        }
    }
}
