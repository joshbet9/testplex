package au.com.testplex.arcadeports;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Persistent, per-tester profiles for a game's debug controls. */
final class GameDebugProfiles {
    static final String DEFAULT_PROFILE = "default";
    private final File file;
    private final YamlConfiguration config;
    private final Consumer<String> warning;

    GameDebugProfiles(JavaPlugin plugin) {
        this(new File(plugin.getDataFolder(), "debug-profiles.yml"),
                message -> plugin.getLogger().warning(message));
    }

    GameDebugProfiles(File file, Consumer<String> warning) {
        this.file = file;
        this.warning = warning == null ? ignored -> { } : warning;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    List<String> profiles(UUID tester, ArcadePortGame game) {
        if (tester == null || game == null) return List.of(DEFAULT_PROFILE);
        String path = root(tester, game) + ".profiles";
        ConfigurationSection section = config.getConfigurationSection(path);
        List<String> result = section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));
        if (!result.contains(DEFAULT_PROFILE)) result.add(0, DEFAULT_PROFILE);
        result.sort((first, second) -> {
            if (DEFAULT_PROFILE.equals(first)) return -1;
            if (DEFAULT_PROFILE.equals(second)) return 1;
            return first.compareToIgnoreCase(second);
        });
        return List.copyOf(result);
    }

    String selected(UUID tester, ArcadePortGame game) {
        if (tester == null || game == null) return DEFAULT_PROFILE;
        String selected = config.getString(root(tester, game) + ".selected", DEFAULT_PROFILE);
        return profiles(tester, game).contains(selected) ? selected : DEFAULT_PROFILE;
    }

    boolean select(UUID tester, ArcadePortGame game, String profile) {
        if (tester == null || game == null || profile == null || !profiles(tester, game).contains(profile)) return false;
        config.set(root(tester, game) + ".selected", profile);
        save();
        return true;
    }

    String create(UUID tester, ArcadePortGame game, String requestedName) {
        return create(tester, game, requestedName, false);
    }

    String duplicate(UUID tester, ArcadePortGame game, String requestedName) {
        return create(tester, game, requestedName, true);
    }

    private String create(UUID tester, ArcadePortGame game, String requestedName, boolean copySelected) {
        if (tester == null || game == null) return null;
        String name = normalizeProfile(requestedName);
        if (name.isBlank() || name.equals(DEFAULT_PROFILE) || name.length() > 24) return null;
        List<String> existing = profiles(tester, game);
        if (existing.contains(name) || existing.size() >= 12) return null;
        String to = profilePath(tester, game, name);
        if (copySelected) {
            String selected = selected(tester, game);
            String from = profilePath(tester, game, selected);
            ConfigurationSection source = config.getConfigurationSection(from);
            Map<String, Object> copied = source == null ? Map.of() : source.getValues(true);
            for (Map.Entry<String, Object> entry : copied.entrySet()) {
                config.set(to + "." + entry.getKey(), entry.getValue());
            }
        }
        config.set(root(tester, game) + ".selected", name);
        save();
        return name;
    }

    String renameSelected(UUID tester, ArcadePortGame game, String requestedName) {
        if (tester == null || game == null) return null;
        String oldName = selected(tester, game);
        if (DEFAULT_PROFILE.equals(oldName)) return null;
        String newName = normalizeProfile(requestedName);
        if (newName.isBlank() || newName.equals(DEFAULT_PROFILE) || newName.length() > 24
                || profiles(tester, game).contains(newName)) return null;
        String oldPath = profilePath(tester, game, oldName);
        String newPath = profilePath(tester, game, newName);
        ConfigurationSection source = config.getConfigurationSection(oldPath);
        if (source != null) {
            for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
                config.set(newPath + "." + entry.getKey(), entry.getValue());
            }
        }
        config.set(oldPath, null);
        config.set(root(tester, game) + ".selected", newName);
        save();
        return newName;
    }

    boolean delete(UUID tester, ArcadePortGame game, String profile) {
        if (tester == null || game == null || profile == null || DEFAULT_PROFILE.equals(profile)
                || !profiles(tester, game).contains(profile)) return false;
        config.set(profilePath(tester, game, profile), null);
        if (profile.equals(selected(tester, game))) {
            config.set(root(tester, game) + ".selected", DEFAULT_PROFILE);
        }
        save();
        return true;
    }

    boolean feature(UUID tester, ArcadePortGame game, String feature, boolean fallback) {
        return config.getBoolean(profilePath(tester, game, selected(tester, game))
                + ".features." + feature, fallback);
    }

    boolean hasFeature(UUID tester, ArcadePortGame game, String feature) {
        return config.contains(profilePath(tester, game, selected(tester, game))
                + ".features." + feature);
    }

    void setFeature(UUID tester, ArcadePortGame game, String feature, boolean enabled) {
        if (!validKey(feature)) return;
        config.set(profilePath(tester, game, selected(tester, game)) + ".features." + feature, enabled);
        save();
    }

    Double variable(UUID tester, ArcadePortGame game, String variable) {
        GameDebugVariableCatalog.Variable definition = GameDebugVariableCatalog.variable(game, variable);
        if (definition == null || definition.kind() == GameDebugVariableCatalog.Kind.BOOLEAN) return null;
        String path = profilePath(tester, game, selected(tester, game)) + ".variables." + variable;
        if (!config.contains(path)) return null;
        double value = GameDebugVariableCatalog.clamp(definition, config.getDouble(path, Double.NaN));
        return Double.isFinite(value) ? value : null;
    }

    void setVariable(UUID tester, ArcadePortGame game, String variable, double value) {
        if (tester == null || game == null) return;
        GameDebugVariableCatalog.Variable definition = GameDebugVariableCatalog.variable(game, variable);
        if (definition == null || definition.kind() == GameDebugVariableCatalog.Kind.BOOLEAN) return;
        double bounded = GameDebugVariableCatalog.clamp(definition, value);
        if (!Double.isFinite(bounded)) return;
        config.set(profilePath(tester, game, selected(tester, game)) + ".variables." + variable, bounded);
        save();
    }

    boolean booleanVariable(UUID tester, ArcadePortGame game, String variable, boolean fallback) {
        GameDebugVariableCatalog.Variable definition = GameDebugVariableCatalog.variable(game, variable);
        if (definition == null || definition.kind() != GameDebugVariableCatalog.Kind.BOOLEAN) return fallback;
        String path = profilePath(tester, game, selected(tester, game)) + ".variables." + variable;
        return config.getBoolean(path, fallback);
    }

    boolean hasBooleanVariable(UUID tester, ArcadePortGame game, String variable) {
        GameDebugVariableCatalog.Variable definition = GameDebugVariableCatalog.variable(game, variable);
        if (definition == null || definition.kind() != GameDebugVariableCatalog.Kind.BOOLEAN) return false;
        return config.contains(profilePath(tester, game, selected(tester, game)) + ".variables." + variable);
    }

    void setBooleanVariable(UUID tester, ArcadePortGame game, String variable, boolean value) {
        if (tester == null || game == null) return;
        GameDebugVariableCatalog.Variable definition = GameDebugVariableCatalog.variable(game, variable);
        if (definition == null || definition.kind() != GameDebugVariableCatalog.Kind.BOOLEAN) return;
        config.set(profilePath(tester, game, selected(tester, game)) + ".variables." + variable, value);
        save();
    }

    void resetSelected(UUID tester, ArcadePortGame game) {
        String profile = profilePath(tester, game, selected(tester, game));
        config.set(profile + ".features", null);
        config.set(profile + ".variables", null);
        config.set(profile + ".test-bots", null);
        // An explicit zero prevents the default profile from reviving a legacy bot count.
        config.set(profile + ".test-bots.count", 0);
        save();
    }

    void save(UUID tester, ArcadePortGame game) {
        if (tester != null && game != null) save();
    }

    Map<String, Object> values(UUID tester, ArcadePortGame game) {
        ConfigurationSection section = config.getConfigurationSection(
                profilePath(tester, game, selected(tester, game)) + ".variables");
        Map<String, Object> result = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) result.put(key, section.get(key));
        }
        result.entrySet().removeIf(entry -> {
            GameDebugVariableCatalog.Variable definition = GameDebugVariableCatalog.variable(game, entry.getKey());
            if (definition == null) return true;
            if (definition.kind() == GameDebugVariableCatalog.Kind.BOOLEAN) {
                return !(entry.getValue() instanceof Boolean);
            }
            if (!(entry.getValue() instanceof Number number)) return true;
            double bounded = GameDebugVariableCatalog.clamp(definition, number.doubleValue());
            if (!Double.isFinite(bounded)) return true;
            entry.setValue(bounded);
            return false;
        });
        ConfigurationSection features = config.getConfigurationSection(
                profilePath(tester, game, selected(tester, game)) + ".features");
        if (features != null) {
            for (String key : features.getKeys(false)) result.putIfAbsent(key, features.get(key));
        }
        return Map.copyOf(result);
    }

    int testBotCount(UUID tester, ArcadePortGame game, int fallback) {
        if (game == null) return fallback;
        String path = testBotRoot(tester, game) + ".count";
        if (config.contains(path)) return config.getInt(path, fallback);
        if ("default".equals(selected(tester, game))) {
            return config.getInt(legacyTestBotRoot(game) + ".count", fallback);
        }
        return fallback;
    }

    void setTestBotCount(UUID tester, ArcadePortGame game, int count) {
        if (game == null) return;
        config.set(testBotRoot(tester, game) + ".count", Math.max(0, Math.min(16, count)));
        save();
    }

    String testBotKit(UUID tester, ArcadePortGame game, int index, String fallback) {
        if (game == null || index < 0) return fallback;
        String path = testBotRoot(tester, game) + ".players." + index + ".kit";
        if (config.contains(path)) return config.getString(path, fallback);
        return "default".equals(selected(tester, game))
                ? config.getString(legacyTestBotRoot(game) + ".players." + index + ".kit", fallback) : fallback;
    }

    void setTestBotKit(UUID tester, ArcadePortGame game, int index, String kit) {
        if (game == null || index < 0 || kit == null || kit.isBlank()) return;
        config.set(testBotRoot(tester, game) + ".players." + index + ".kit", kit);
        save();
    }

    int testBotTeam(UUID tester, ArcadePortGame game, int index, int fallback) {
        if (game == null || index < 0) return fallback;
        String path = testBotRoot(tester, game) + ".players." + index + ".team";
        if (config.contains(path)) return config.getInt(path, fallback);
        return "default".equals(selected(tester, game))
                ? config.getInt(legacyTestBotRoot(game) + ".players." + index + ".team", fallback) : fallback;
    }

    void setTestBotTeam(UUID tester, ArcadePortGame game, int index, int team) {
        if (game == null || index < 0) return;
        config.set(testBotRoot(tester, game) + ".players." + index + ".team", team);
        save();
    }

    String testBotCosmetic(UUID tester, ArcadePortGame game, int index, String lineId, String fallback) {
        if (game == null || index < 0 || !validKey(lineId)) return fallback;
        String path = testBotRoot(tester, game) + ".players." + index + ".cosmetics." + lineId;
        if (config.contains(path)) return config.getString(path, fallback);
        return "default".equals(selected(tester, game))
                ? config.getString(legacyTestBotRoot(game) + ".players." + index + ".cosmetics." + lineId, fallback)
                : fallback;
    }

    Map<String, String> testBotCosmetics(UUID tester, ArcadePortGame game, int index) {
        if (game == null || index < 0) return Map.of();
        String path = testBotRoot(tester, game) + ".players." + index + ".cosmetics";
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null && "default".equals(selected(tester, game))) {
            section = config.getConfigurationSection(legacyTestBotRoot(game) + ".players." + index + ".cosmetics");
        }
        if (section == null) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (validKey(key) && value != null && !value.isBlank()) result.put(key, value);
        }
        return Map.copyOf(result);
    }

    void setTestBotCosmetic(UUID tester, ArcadePortGame game, int index, String lineId, String optionId) {
        if (game == null || index < 0 || !validKey(lineId) || optionId == null || optionId.isBlank()) return;
        config.set(testBotRoot(tester, game) + ".players." + index + ".cosmetics." + lineId, optionId);
        save();
    }

    private void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                warning.accept("Could not create debug profile directory " + parent);
                return;
            }
            config.save(file);
        } catch (IOException exception) {
            warning.accept("Could not save isolated game debug profiles: " + exception.getMessage());
        }
    }

    private static String root(UUID tester, ArcadePortGame game) {
        return "test-debug-profiles." + tester + "." + game.id();
    }

    private static String profilePath(UUID tester, ArcadePortGame game, String profile) {
        return root(tester, game) + ".profiles." + profile;
    }

    private String testBotRoot(UUID tester, ArcadePortGame game) {
        return profilePath(tester, game, selected(tester, game)) + ".test-bots";
    }

    private static String legacyTestBotRoot(ArcadePortGame game) {
        return "test-bot-loadouts." + game.id();
    }

    static String normalizeProfile(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private static boolean validKey(String key) {
        return key != null && key.matches("[A-Za-z0-9_-]{1,100}");
    }
}
