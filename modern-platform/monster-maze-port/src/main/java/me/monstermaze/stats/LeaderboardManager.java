package me.monstermaze.stats;

import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.game.MazeMode;
import me.monstermaze.kit.KitType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leaderboards and personal bests.
 *
 * The hosted/public server uses the backend for global records. YAML also
 * retains local per-pattern kit PBs so all-maze kit boards remain available
 * while the backend refreshes or is temporarily unavailable.
 */
public class LeaderboardManager {
    public static final int PATTERN_COUNT = 3;

    private final MonsterMazePlugin plugin;
    private final File file;
    private YamlConfiguration data;
    private final BackendClient backend;

    private volatile boolean remoteReady;
    private volatile Map<String, List<OverallEntry>> remoteOverall = new HashMap<String, List<OverallEntry>>();
    private volatile Map<String, Map<Integer, List<OverallEntry>>> remotePattern = new HashMap<String, Map<Integer, List<OverallEntry>>>();
    private volatile Map<String, Map<String, List<OverallEntry>>> remoteKit = new HashMap<String, Map<String, List<OverallEntry>>>();
    private volatile Map<String, Map<Integer, Map<String, PBInfo>>> remotePB = new HashMap<String, Map<Integer, Map<String, PBInfo>>>();

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{([^{}]*)\\}");
    private static final Pattern JSON_PB = Pattern.compile(
            "\\{\\\"pattern\\\":(\\d+),\\\"kit\\\":\\\"((?:\\\\.|[^\\\"\\\\])*)\\\",\\\"stage\\\":(\\d+),\\\"timeMs\\\":(\\d+)\\}");

    public static class PBInfo {
        public final int stage;
        public final String kit;
        public PBInfo(int stage, String kit) { this.stage = stage; this.kit = kit; }
    }

    public static class Entry {
        public final String name;
        public final int stage;
        public final String kit;
        public Entry(String name, int stage, String kit) { this.name = name; this.stage = stage; this.kit = kit; }
    }

    public static class OverallEntry {
        public final String name;
        public final int stage;
        public final String kit;
        public final int pattern;
        public OverallEntry(String name, int stage, String kit, int pattern) {
            this.name = name; this.stage = stage; this.kit = kit; this.pattern = pattern;
        }
    }

    public LeaderboardManager(final MonsterMazePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
        this.backend = plugin.getBackendClient();
        reload();
        if (backend != null && backend.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(plugin.getHostPlugin(), new Runnable() {
                @Override public void run() { refreshFromBackend(); }
            }, 20L);
            Bukkit.getScheduler().runTaskTimer(plugin.getHostPlugin(), new Runnable() {
                @Override public void run() { refreshFromBackend(); }
            }, 600L, 600L);
        }
    }

    public void reload() { data = YamlConfiguration.loadConfiguration(file); }

    private static String modeKey(MazeMode mode) { return mode.id.toLowerCase(); }
    private static String patternKey(int pattern) { return "pattern" + pattern; }
    public static String patternName(int pattern) {
        return pattern >= 0 && pattern < PATTERN_COUNT ? "Maze " + (pattern + 1) : "All Mazes";
    }

    /** Capture Bukkit state on the main thread, then do all HTTP work asynchronously. */
    public void refreshFromBackend() {
        if (backend == null || !backend.isEnabled()) return;
        Bukkit.getScheduler().runTask(plugin.getHostPlugin(), new Runnable() {
            @Override public void run() {
                final List<UUID> players = new ArrayList<UUID>();
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) players.add(player.getUniqueId());
                Bukkit.getScheduler().runTaskAsynchronously(plugin.getHostPlugin(), new Runnable() {
                    @Override public void run() { refreshFromBackendAsync(players); }
                });
            }
        });
    }

    private void refreshFromBackendAsync(List<UUID> players) {
        try {
            Map<String, List<OverallEntry>> overall = new HashMap<String, List<OverallEntry>>();
            Map<String, Map<Integer, List<OverallEntry>>> patterns = new HashMap<String, Map<Integer, List<OverallEntry>>>();
            Map<String, Map<String, List<OverallEntry>>> kits = new HashMap<String, Map<String, List<OverallEntry>>>();
            Map<String, Map<Integer, Map<String, PBInfo>>> pbs = new HashMap<String, Map<Integer, Map<String, PBInfo>>>();

            for (MazeMode mode : MazeMode.values()) {
                String mk = modeKey(mode);
                overall.put(mk, parseBoard(backend.get("/api/v1/leaderboard/1.21/" + mk + "/overall"), -1));

                Map<Integer, List<OverallEntry>> modePatterns = new HashMap<Integer, List<OverallEntry>>();
                for (int pattern = 0; pattern < PATTERN_COUNT; pattern++) {
                    modePatterns.put(pattern, parseBoard(
                            backend.get("/api/v1/leaderboard/1.21/" + mk + "/pattern/" + pattern), pattern));
                }
                patterns.put(mk, modePatterns);

                Map<String, List<OverallEntry>> modeKits = new HashMap<String, List<OverallEntry>>();
                for (KitType kit : KitType.values()) {
                    modeKits.put(kit.id, aggregateKitRows(parseBoard(
                            backend.get("/api/v1/leaderboard/1.21/" + mk + "/kit/" + kit.id), -1)));
                }
                kits.put(mk, modeKits);

                for (UUID player : players) {
                    String raw = backend.get("/api/v1/pb/1.21/" + mk + "/" + player.toString());
                    pbs.put(mk + "|" + player.toString(), parsePB(raw));
                }
            }

            remoteOverall = overall;
            remotePattern = patterns;
            remoteKit = kits;
            remotePB = pbs;
            remoteReady = true;
            plugin.getLogger().fine("Monster Maze backend leaderboards/PBs refreshed.");
        } catch (Exception e) {
            plugin.getLogger().warning("Backend leaderboard sync failed: " + e.getMessage());
        }
    }

    /** Parse leaderboard rows without depending on JSON field order or an exact field set. */
    static List<OverallEntry> parseBoard(String json, int defaultPattern) {
        List<OverallEntry> out = new ArrayList<OverallEntry>();
        if (json == null) return out;
        Matcher objects = JSON_OBJECT.matcher(json);
        while (objects.find()) {
            String object = objects.group(1);
            String name = jsonStringField(object, "name");
            String kit = jsonStringField(object, "kit");
            Integer stage = jsonIntegerField(object, "stage");
            if (name == null || kit == null || stage == null || stage < 1) continue;
            Integer pattern = jsonIntegerField(object, "pattern");
            out.add(new OverallEntry(name, stage, kit, pattern == null ? defaultPattern : pattern));
        }
        return out;
    }

    /** A kit board represents each player's best result across every maze, once. */
    static List<OverallEntry> aggregateKitRows(List<OverallEntry> rows) {
        Map<String, OverallEntry> bestByPlayer = new HashMap<String, OverallEntry>();
        if (rows != null) for (OverallEntry row : rows) {
            if (row == null || row.name == null || row.name.trim().isEmpty() || row.stage < 1) continue;
            String key = row.name.trim().toLowerCase(Locale.ROOT);
            OverallEntry previous = bestByPlayer.get(key);
            if (previous == null || row.stage > previous.stage
                    || (row.stage == previous.stage && row.pattern >= 0
                    && (previous.pattern < 0 || row.pattern < previous.pattern))) {
                bestByPlayer.put(key, row);
            }
        }
        List<OverallEntry> ranked = new ArrayList<OverallEntry>(bestByPlayer.values());
        Collections.sort(ranked, new Comparator<OverallEntry>() {
            @Override public int compare(OverallEntry a, OverallEntry b) {
                int stageOrder = Integer.compare(b.stage, a.stage);
                return stageOrder != 0 ? stageOrder : a.name.compareToIgnoreCase(b.name);
            }
        });
        return ranked;
    }

    private static String jsonStringField(String object, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(object);
        return matcher.find() ? unescape(matcher.group(1)) : null;
    }

    private static Integer jsonIntegerField(String object, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*(-?\\d+)").matcher(object);
        if (!matcher.find()) return null;
        try { return Integer.valueOf(matcher.group(1)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static Map<Integer, Map<String, PBInfo>> parsePB(String json) {
        Map<Integer, Map<String, PBInfo>> out = new HashMap<Integer, Map<String, PBInfo>>();
        if (json == null) return out;
        Matcher m = JSON_PB.matcher(json);
        while (m.find()) {
            int pattern = Integer.parseInt(m.group(1));
            Map<String, PBInfo> kits = out.get(pattern);
            if (kits == null) { kits = new HashMap<String, PBInfo>(); out.put(pattern, kits); }
            String kit = unescape(m.group(2));
            int stage = Integer.parseInt(m.group(3));
            PBInfo old = kits.get(kit);
            if (old == null || stage > old.stage) kits.put(kit, new PBInfo(stage, kit));
        }
        return out;
    }

    private static String unescape(String value) {
        if (value == null) return "";
        return value.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }

    private Map<Integer, Map<String, PBInfo>> remotePlayer(MazeMode mode, UUID player) {
        return remotePB.get(modeKey(mode) + "|" + player.toString());
    }

    public int getKitPB(MazeMode mode, int pattern, UUID player, String kitId) {
        if (kitId == null) return 0;
        if (remoteReady) {
            Map<Integer, Map<String, PBInfo>> byPattern = remotePlayer(mode, player);
            Map<String, PBInfo> kits = byPattern == null ? null : byPattern.get(pattern);
            PBInfo info = kits == null ? null : kits.get(kitId);
            int remoteStage = info == null ? 0 : info.stage;
            if (info == null && kits != null) for (Map.Entry<String, PBInfo> e : kits.entrySet()) {
                if (e.getKey().equalsIgnoreCase(kitId)) {
                    remoteStage = e.getValue().stage;
                    break;
                }
            }
            return Math.max(remoteStage, localKitPB(mode, pattern, player, kitId));
        }
        return localKitPB(mode, pattern, player, kitId);
    }

    private int localKitPB(MazeMode mode, int pattern, UUID player, String kitId) {
        return storedKitPB(data, mode, pattern, player, kitId);
    }

    static int storedKitPB(YamlConfiguration records, MazeMode mode, int pattern, UUID player, String kitId) {
        ConfigurationSection modeSection = records.getConfigurationSection(modeKey(mode));
        ConfigurationSection patternSection = modeSection == null ? null
                : modeSection.getConfigurationSection(patternKey(pattern));
        ConfigurationSection ps = patternSection == null ? null
                : patternSection.getConfigurationSection(player.toString());
        if (ps == null) return 0;
        if (ps.contains(kitId)) return ps.getInt(kitId, 0);
        for (String storedKit : ps.getKeys(false)) if (storedKit.equalsIgnoreCase(kitId)) return ps.getInt(storedKit, 0);
        return 0;
    }

    static boolean storeKitPB(YamlConfiguration records, MazeMode mode, int pattern,
                              UUID player, String kitId, int stage) {
        if (records == null || mode == null || pattern < 0 || pattern >= PATTERN_COUNT
                || player == null || kitId == null || kitId.isEmpty() || stage < 1
                || stage <= storedKitPB(records, mode, pattern, player, kitId)) return false;
        records.set(modeKey(mode) + "." + patternKey(pattern) + "." + player + "." + kitId, stage);
        return true;
    }

    public int getPB(MazeMode mode, int pattern, UUID player) {
        PBInfo best = getBest(mode, pattern, player);
        return best == null ? 0 : best.stage;
    }

    public PBInfo getBest(MazeMode mode, int pattern, UUID player) {
        if (remoteReady) {
            Map<Integer, Map<String, PBInfo>> byPattern = remotePlayer(mode, player);
            PBInfo best = null;
            Map<String, PBInfo> kits = byPattern == null ? null : byPattern.get(pattern);
            if (kits != null) for (PBInfo info : kits.values()) {
                if (best == null || info.stage > best.stage) best = info;
            }
            ConfigurationSection local = playerSection(mode, pattern, player);
            if (local != null) for (String kit : local.getKeys(false)) {
                int stage = local.getInt(kit, 0);
                if (stage > 0 && (best == null || stage > best.stage)) best = new PBInfo(stage, kit);
            }
            return best;
        }
        ConfigurationSection ps = playerSection(mode, pattern, player);
        if (ps == null) return null;
        String bestKit = null; int bestStage = 0;
        for (String kit : ps.getKeys(false)) {
            int stage = ps.getInt(kit, 0);
            if (stage > bestStage) { bestStage = stage; bestKit = kit; }
        }
        return bestStage < 1 ? null : new PBInfo(bestStage, bestKit);
    }

    public void recordRun(MazeMode mode, int pattern, UUID player, int stage, String kitId) {
        if (mode == null || player == null || pattern < 0 || pattern >= PATTERN_COUNT
                || stage < 1 || kitId == null || kitId.isEmpty()) return;
        // Keep a local durable PB cache even when the backend is active. It lets
        // kit boards retain a just-finished run while the remote leaderboard
        // catches up and preserves it if a remote refresh is temporarily stale.
        if (storeKitPB(data, mode, pattern, player, kitId, stage)) save();
    }

    public void recordRun(MazeMode mode, int pattern, UUID player, int stage, KitType kit) {
        if (kit != null) recordRun(mode, pattern, player, stage, kit.id);
    }

    public List<Entry> getLeaderboard(MazeMode mode, int pattern, int limit) {
        if (remoteReady) {
            Map<Integer, List<OverallEntry>> byPattern = remotePattern.get(modeKey(mode));
            List<OverallEntry> rows = byPattern == null ? null : byPattern.get(pattern);
            return mergePatternRows(rows, localPatternLeaderboard(mode, pattern, Integer.MAX_VALUE), limit);
        }
        return localPatternLeaderboard(mode, pattern, limit);
    }

    /** Rank each player's best for one kit on one maze pattern, independent of other kits. */
    public List<Entry> getKitPatternLeaderboard(MazeMode mode, int pattern, String kitId, int limit) {
        if (mode == null || pattern < 0 || pattern >= PATTERN_COUNT || kitId == null || kitId.isBlank()
                || limit <= 0) return new ArrayList<Entry>();
        List<OverallEntry> remoteRows = null;
        if (remoteReady) {
            Map<Integer, List<OverallEntry>> byPattern = remotePattern.get(modeKey(mode));
            remoteRows = byPattern == null ? null : byPattern.get(pattern);
        }
        return mergeKitPatternRows(remoteRows,
                localKitPatternRows(data, mode, pattern, kitId, this::displayName), kitId, pattern, limit);
    }

    /** Merge durable local PBs into the hosted pattern board while remote submission catches up. */
    static List<Entry> mergePatternRows(List<OverallEntry> remoteRows, List<Entry> localRows, int limit) {
        if (limit <= 0) return new ArrayList<Entry>();
        Map<String, Entry> bestByPlayer = new TreeMap<String, Entry>(String.CASE_INSENSITIVE_ORDER);
        if (remoteRows != null) for (OverallEntry row : remoteRows) {
            if (row != null) mergePatternEntry(bestByPlayer, new Entry(row.name, row.stage, row.kit));
        }
        if (localRows != null) for (Entry row : localRows) mergePatternEntry(bestByPlayer, row);
        List<Entry> ranked = new ArrayList<Entry>(bestByPlayer.values());
        Collections.sort(ranked, new Comparator<Entry>() {
            @Override public int compare(Entry left, Entry right) {
                int stageOrder = Integer.compare(right.stage, left.stage);
                if (stageOrder != 0) return stageOrder;
                String leftName = left.name == null ? "" : left.name;
                String rightName = right.name == null ? "" : right.name;
                return String.CASE_INSENSITIVE_ORDER.compare(leftName, rightName);
            }
        });
        return ranked.size() > limit ? new ArrayList<Entry>(ranked.subList(0, limit)) : ranked;
    }

    /**
     * Rank local stored kit PBs with any matching kit rows available from the public pattern board.
     * The remote pattern endpoint exposes only each player's overall kit, so the durable local
     * per-kit cache is required to retain lower personal bests for the other kits.
     */
    static List<Entry> mergeKitPatternRows(List<OverallEntry> remoteRows, List<Entry> localRows,
            String kitId, int pattern, int limit) {
        if (kitId == null || limit <= 0) return new ArrayList<Entry>();
        Map<String, Entry> bestByPlayer = new TreeMap<String, Entry>(String.CASE_INSENSITIVE_ORDER);
        if (remoteRows != null) for (OverallEntry row : remoteRows) {
            if (row != null && row.pattern == pattern && row.kit != null
                    && row.kit.equalsIgnoreCase(kitId)) {
                mergePatternEntry(bestByPlayer, new Entry(row.name, row.stage, row.kit));
            }
        }
        if (localRows != null) for (Entry row : localRows) {
            if (row != null && row.kit != null && row.kit.equalsIgnoreCase(kitId)) {
                mergePatternEntry(bestByPlayer, row);
            }
        }
        List<Entry> ranked = new ArrayList<Entry>(bestByPlayer.values());
        Collections.sort(ranked, new Comparator<Entry>() {
            @Override public int compare(Entry left, Entry right) {
                int stageOrder = Integer.compare(right.stage, left.stage);
                if (stageOrder != 0) return stageOrder;
                String leftName = left.name == null ? "" : left.name;
                String rightName = right.name == null ? "" : right.name;
                return String.CASE_INSENSITIVE_ORDER.compare(leftName, rightName);
            }
        });
        return ranked.size() > limit ? new ArrayList<Entry>(ranked.subList(0, limit)) : ranked;
    }

    static List<Entry> localKitPatternRows(YamlConfiguration records, MazeMode mode, int pattern,
            String kitId, Function<UUID, String> nameForPlayer) {
        if (records == null || mode == null || pattern < 0 || pattern >= PATTERN_COUNT
                || kitId == null || kitId.isBlank()) return new ArrayList<Entry>();
        ConfigurationSection modeSection = records.getConfigurationSection(modeKey(mode));
        ConfigurationSection patternSection = modeSection == null ? null
                : modeSection.getConfigurationSection(patternKey(pattern));
        if (patternSection == null) return new ArrayList<Entry>();
        List<Entry> rows = new ArrayList<Entry>();
        for (String key : patternSection.getKeys(false)) {
            UUID player;
            try { player = UUID.fromString(key); }
            catch (IllegalArgumentException ignored) { continue; }
            ConfigurationSection playerSection = patternSection.getConfigurationSection(key);
            if (playerSection == null) continue;
            String storedKit = null;
            for (String candidate : playerSection.getKeys(false)) {
                if (candidate.equalsIgnoreCase(kitId)) { storedKit = candidate; break; }
            }
            if (storedKit == null) continue;
            int stage = playerSection.getInt(storedKit, 0);
            String name = nameForPlayer == null ? null : nameForPlayer.apply(player);
            if (stage > 0 && name != null && !name.isBlank()) rows.add(new Entry(name, stage, storedKit));
        }
        return rows;
    }

    private static void mergePatternEntry(Map<String, Entry> bestByPlayer, Entry candidate) {
        if (candidate == null || candidate.name == null || candidate.name.isBlank() || candidate.stage < 1) return;
        Entry previous = bestByPlayer.get(candidate.name);
        if (previous == null || candidate.stage > previous.stage) bestByPlayer.put(candidate.name, candidate);
    }

    private List<Entry> localPatternLeaderboard(MazeMode mode, int pattern, int limit) {
        reload();
        Map<Integer, List<Map.Entry<UUID, String>>> byStage = new TreeMap<Integer, List<Map.Entry<UUID, String>>>(Collections.reverseOrder());
        ConfigurationSection m = data.getConfigurationSection(modeKey(mode));
        if (m != null) {
            ConfigurationSection p = m.getConfigurationSection(patternKey(pattern));
            if (p != null) for (String key : p.getKeys(false)) {
                UUID uuid; try { uuid = UUID.fromString(key); } catch (IllegalArgumentException ignored) { continue; }
                ConfigurationSection ps = p.getConfigurationSection(key); if (ps == null) continue;
                int bestStage = 0; String bestKit = null;
                for (String kit : ps.getKeys(false)) { int stage = ps.getInt(kit, 0); if (stage > bestStage) { bestStage = stage; bestKit = kit; } }
                if (bestStage < 1) continue;
                List<Map.Entry<UUID, String>> bucket = byStage.get(bestStage);
                if (bucket == null) { bucket = new ArrayList<Map.Entry<UUID, String>>(); byStage.put(bestStage, bucket); }
                bucket.add(new AbstractMap.SimpleEntry<UUID, String>(uuid, bestKit));
            }
        }
        List<Entry> out = new ArrayList<Entry>();
        for (Map.Entry<Integer, List<Map.Entry<UUID, String>>> bucket : byStage.entrySet()) for (Map.Entry<UUID, String> idKit : bucket.getValue()) {
            if (out.size() >= limit) return out;
            out.add(new Entry(displayName(idKit.getKey()), bucket.getKey(), idKit.getValue()));
        }
        return out;
    }

    public List<OverallEntry> getModeLeaderboard(MazeMode mode, int limit) {
        if (remoteReady) {
            List<OverallEntry> rows = remoteOverall.get(modeKey(mode));
            return rows == null ? new ArrayList<OverallEntry>()
                    : new ArrayList<OverallEntry>(rows.subList(0, Math.min(limit, rows.size())));
        }
        return localModeLeaderboard(mode, limit, null);
    }

    public List<OverallEntry> getModeAndKitLeaderboard(MazeMode mode, KitType kit, int limit) {
        if (kit == null) return getModeLeaderboard(mode, limit);
        if (remoteReady) {
            Map<String, List<OverallEntry>> modeKits = remoteKit.get(modeKey(mode));
            List<OverallEntry> rows = modeKits == null ? null : modeKits.get(kit.id);
            if (rows == null && modeKits != null) for (Map.Entry<String, List<OverallEntry>> e : modeKits.entrySet()) if (e.getKey().equalsIgnoreCase(kit.id)) { rows = e.getValue(); break; }
            List<OverallEntry> combined = aggregateKitRows(rows);
            combined.addAll(localModeLeaderboard(mode, Integer.MAX_VALUE, kit));
            combined = aggregateKitRows(combined);
            return combined.size() > limit
                    ? new ArrayList<OverallEntry>(combined.subList(0, limit)) : combined;
        }
        return localModeLeaderboard(mode, limit, kit);
    }

    private List<OverallEntry> localModeLeaderboard(MazeMode mode, int limit, KitType filterKit) {
        reload();
        Map<UUID, OverallEntry> bestOf = new HashMap<UUID, OverallEntry>();
        ConfigurationSection m = data.getConfigurationSection(modeKey(mode));
        if (m != null) for (String patKey : m.getKeys(false)) {
            ConfigurationSection pat = m.getConfigurationSection(patKey); Integer pattern = parsePatternKey(patKey); if (pat == null || pattern == null) continue;
            for (String key : pat.getKeys(false)) {
                UUID uuid; try { uuid = UUID.fromString(key); } catch (IllegalArgumentException ignored) { continue; }
                ConfigurationSection ps = pat.getConfigurationSection(key); if (ps == null) continue;
                int bestStage = 0; String bestKit = null;
                for (String k : ps.getKeys(false)) {
                    if (filterKit != null && !(k.equalsIgnoreCase(filterKit.id) || k.equalsIgnoreCase(filterKit.name()))) continue;
                    int stage = ps.getInt(k, 0); if (stage > bestStage) { bestStage = stage; bestKit = k; }
                }
                if (bestStage < 1) continue;
                OverallEntry prev = bestOf.get(uuid);
                if (prev == null || bestStage > prev.stage) bestOf.put(uuid, new OverallEntry(displayName(uuid), bestStage, bestKit, pattern));
            }
        }
        List<OverallEntry> ranked = new ArrayList<OverallEntry>(bestOf.values());
        Collections.sort(ranked, new Comparator<OverallEntry>() { @Override public int compare(OverallEntry a, OverallEntry b) { return b.stage - a.stage; } });
        return ranked.size() > limit ? new ArrayList<OverallEntry>(ranked.subList(0, limit)) : ranked;
    }

    private Integer parsePatternKey(String key) {
        if (key == null || !key.startsWith("pattern")) return null;
        try { return Integer.valueOf(key.substring("pattern".length())); } catch (NumberFormatException ignored) { return null; }
    }

    private ConfigurationSection playerSection(MazeMode mode, int pattern, UUID player) {
        ConfigurationSection m = data.getConfigurationSection(modeKey(mode));
        if (m == null) return null;
        ConfigurationSection p = m.getConfigurationSection(patternKey(pattern));
        return p == null ? null : p.getConfigurationSection(player.toString());
    }

    private String displayName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null && !name.isEmpty() ? name : uuid.toString().substring(0, 6);
    }

    private void save() {
        try { data.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save leaderboards.yml: " + e.getMessage()); }
    }
}
