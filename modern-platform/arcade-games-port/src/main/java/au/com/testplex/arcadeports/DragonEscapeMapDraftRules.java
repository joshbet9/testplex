package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure validation and WorldConfig.dat editing rules used by the Dragon Escape map editor. */
final class DragonEscapeMapDraftRules {
    private static final Pattern SAFE_SLUG = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");

    private DragonEscapeMapDraftRules() {}

    static String draftSlug(String value) {
        if (value == null) return null;
        String slug = value.trim().toLowerCase(Locale.ROOT);
        return SAFE_SLUG.matcher(slug).matches() ? slug : null;
    }

    static String mapSlug(String resource) {
        if (resource == null || !(SourceMapCatalog.resourcePathBelongsToFolders(
                List.of("Dragon Escape"), resource)
                || resource.equals(SourceMapCatalog.resourceFor(ArcadePortGame.DRAGON_ESCAPE)))) return null;
        String name = resource.substring(resource.lastIndexOf('/') + 1);
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) name = name.substring(0, name.length() - 4);
        if (name.regionMatches(true, 0, "DragonEscape_", 0, "DragonEscape_".length())) {
            name = name.substring("DragonEscape_".length());
        }
        return draftSlug(name.replace(' ', '-').replace('.', '-'));
    }

    static String worldName(String mapResource, String draftName) {
        String map = mapSlug(mapResource);
        String draft = draftSlug(draftName);
        if (map == null || draft == null) return null;
        String seed = mapResource + "\n" + draft;
        String hash = java.util.UUID.nameUUIDFromBytes(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString().replace("-", "").substring(0, 12);
        return "arcadeport_source_mapeditor_" + hash;
    }

    static List<String> addPoint(List<String> source, String groupType, String groupName,
            double x, double y, double z) {
        if (!validGroup(groupType) || !validGroupName(groupName)
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Invalid map marker.");
        }
        List<String> lines = new ArrayList<>(source == null ? List.of() : source);
        String nameKey = nameKey(groupType);
        String locationKey = locationKey(groupType);
        lines.add(nameKey + ": " + groupName.trim());
        lines.add(locationKey + ": " + coordinate(x) + "," + coordinate(y) + "," + coordinate(z));
        return List.copyOf(lines);
    }

    /** Remove one one-based marker index from the named group, preserving every other metadata line. */
    static List<String> removePoint(List<String> source, String groupType, String groupName, int oneBasedIndex) {
        if (!validGroup(groupType) || !validGroupName(groupName) || oneBasedIndex < 1) {
            throw new IllegalArgumentException("Invalid map marker index.");
        }
        String desired = groupName.trim().toUpperCase(Locale.ROOT);
        String nameKey = nameKey(groupType);
        String locationKey = locationKey(groupType);
        List<String> result = new ArrayList<>();
        String current = "UNASSIGNED";
        int index = 0;
        boolean removed = false;
        for (String line : source == null ? List.<String>of() : source) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                result.add(line);
                continue;
            }
            String key = line.substring(0, colon).trim().toUpperCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            if (key.equals(nameKey)) current = value.isEmpty() ? "UNASSIGNED" : value.toUpperCase(Locale.ROOT);
            if (!removed && key.equals(locationKey) && current.equals(desired)) {
                List<String> points = new ArrayList<>(List.of(value.split(":")));
                List<String> kept = new ArrayList<>();
                for (String point : points) {
                    if (point.isBlank()) continue;
                    index++;
                    if (index == oneBasedIndex) removed = true;
                    else kept.add(point.trim());
                }
                if (!kept.isEmpty()) result.add(line.substring(0, colon + 1) + " " + String.join(":", kept));
                continue;
            }
            result.add(line);
        }
        if (!removed) throw new IllegalArgumentException("That marker does not exist.");
        return List.copyOf(result);
    }

    static List<String> setBounds(List<String> source, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
            throw new IllegalArgumentException("Map bounds must have a positive size on every axis.");
        }
        List<String> result = new ArrayList<>();
        List<String> keys = List.of("MIN_X", "MAX_X", "MIN_Y", "MAX_Y", "MIN_Z", "MAX_Z");
        for (String line : source == null ? List.<String>of() : source) {
            int colon = line.indexOf(':');
            if (colon <= 0 || !keys.contains(line.substring(0, colon).trim().toUpperCase(Locale.ROOT))) {
                result.add(line);
            }
        }
        result.add("MIN_X: " + minX);
        result.add("MAX_X: " + maxX);
        result.add("MIN_Y: " + minY);
        result.add("MAX_Y: " + maxY);
        result.add("MIN_Z: " + minZ);
        result.add("MAX_Z: " + maxZ);
        return List.copyOf(result);
    }

    private static boolean validGroup(String value) {
        return value != null && List.of("DATA", "TEAM", "CUSTOM").contains(value.toUpperCase(Locale.ROOT));
    }

    private static boolean validGroupName(String value) {
        return value != null && !value.isBlank() && value.length() <= 48
                && value.chars().noneMatch(ch -> ch == ':' || ch == '\n' || ch == '\r');
    }

    private static String nameKey(String type) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "TEAM" -> "TEAM_NAME";
            case "CUSTOM" -> "CUSTOM_NAME";
            default -> "DATA_NAME";
        };
    }

    private static String locationKey(String type) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "TEAM" -> "TEAM_SPAWNS";
            case "CUSTOM" -> "CUSTOM_LOCS";
            default -> "DATA_LOCS";
        };
    }

    private static String coordinate(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return Double.toString(value);
    }
}
