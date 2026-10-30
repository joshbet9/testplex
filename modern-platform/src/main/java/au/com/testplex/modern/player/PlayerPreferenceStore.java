package au.com.testplex.modern.player;

import au.com.testplex.modern.model.CosmeticLineDefinition;
import au.com.testplex.modern.model.GameDefinition;
import au.com.testplex.modern.model.CosmeticOption;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Player preferences are scoped by UUID, game and line—not by global cosmetics. */
public final class PlayerPreferenceStore {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public PlayerPreferenceStore(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create " + plugin.getDataFolder());
        }
        this.file = new File(plugin.getDataFolder(), "player-preferences.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public Set<String> getSelected(UUID player, GameDefinition game, CosmeticLineDefinition line) {
        String path = linePath(player, game, line);
        if (line.getSelectionMode() == CosmeticLineDefinition.SelectionMode.SINGLE) {
            return Set.of(data.getString(path, line.getDefaultOptionId()));
        }
        if (!data.contains(path)) {
            // A grouped MULTI line starts with the first option in each group equipped.
            // This lets a single menu line provide independent weapon/pellet selections.
            if (line.getOptions().stream().anyMatch(option -> option.getSelectionGroup() != null)) {
                Set<String> defaults = new LinkedHashSet<>();
                Set<String> groups = new LinkedHashSet<>();
                for (var option : line.getOptions()) {
                    String group = option.getSelectionGroup();
                    if (group == null) continue;
                    if (groups.add(group)) defaults.add(option.getId());
                }
                if (!defaults.isEmpty()) return defaults;
            }
            return Set.of(line.getDefaultOptionId());
        }
        return new LinkedHashSet<>(data.getStringList(path));
    }

    public boolean isSelected(UUID player, GameDefinition game, CosmeticLineDefinition line, String optionId) {
        return getSelected(player, game, line).contains(optionId);
    }

    public void chooseOption(UUID player, GameDefinition game, CosmeticLineDefinition line, String optionId) {
        boolean belongs = line.getOptions().stream().anyMatch(option -> option.getId().equals(optionId));
        if (!belongs) throw new IllegalArgumentException("Option does not belong to " + line.getId());

        String path = linePath(player, game, line);
        if (line.getSelectionMode() == CosmeticLineDefinition.SelectionMode.SINGLE) {
            data.set(path, optionId);
        } else {
            Set<String> selected = new LinkedHashSet<>(getSelected(player, game, line));
            CosmeticOption selectedOption = line.getOptions().stream()
                    .filter(option -> option.getId().equals(optionId)).findFirst().orElseThrow();
            String group = selectedOption.getSelectionGroup();
            if (group != null) {
                // Equip exactly one choice within this group without disturbing
                // the user's selections in the other groups on this same line.
                selected.removeIf(existingId -> line.getOptions().stream()
                        .anyMatch(option -> option.getId().equals(existingId)
                                && group.equals(option.getSelectionGroup())));
                selected.add(optionId);
            } else if (!selected.add(optionId)) {
                selected.remove(optionId);
            }
            data.set(path, new ArrayList<>(selected));
        }
        save();
    }

    public String getWinEffect(UUID player, GameDefinition game) {
        return data.getString(winPath(player, game), game.getDefaultWinEffectId());
    }

    public void chooseWinEffect(UUID player, GameDefinition game, String optionId) {
        boolean belongs = game.getWinEffects().stream().anyMatch(option -> option.getId().equals(optionId));
        if (!belongs) throw new IllegalArgumentException("Win effect does not belong to " + game.getId());
        data.set(winPath(player, game), optionId);
        save();
    }

    private String linePath(UUID player, GameDefinition game, CosmeticLineDefinition line) {
        return "players." + player + ".games." + game.getId() + ".cosmetics." + line.getId() + ".selected";
    }

    private String winPath(UUID player, GameDefinition game) {
        return "players." + player + ".games." + game.getId() + ".win-effect";
    }

    public void flush() { save(); }

    private void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save player preferences: " + exception.getMessage());
        }
    }
}
