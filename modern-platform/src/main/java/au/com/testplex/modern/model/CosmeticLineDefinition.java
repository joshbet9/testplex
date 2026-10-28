package au.com.testplex.modern.model;

import java.util.List;
import java.util.Objects;

/**
 * One independent cosmetic line. MULTI lines can equip several items at once
 * (for example enabling multiple Monster Maze mob sets); SINGLE lines equip one.
 */
public final class CosmeticLineDefinition {
    public enum SelectionMode { SINGLE, MULTI }

    private final String id;
    private final String title;
    private final String description;
    private final String icon;
    private final SelectionMode selectionMode;
    private final List<CosmeticOption> options;
    private final String defaultOptionId;

    public CosmeticLineDefinition(String id, String title, String description, String icon,
                                  SelectionMode selectionMode, List<CosmeticOption> options,
                                  String defaultOptionId) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.description = Objects.requireNonNullElse(description, "");
        this.icon = Objects.requireNonNullElse(icon, "CHEST");
        this.selectionMode = Objects.requireNonNull(selectionMode);
        this.options = List.copyOf(options);
        if (this.options.isEmpty()) throw new IllegalArgumentException("A cosmetic line needs options: " + id);
        if (defaultOptionId != null && this.options.stream().noneMatch(o -> o.getId().equals(defaultOptionId))) {
            throw new IllegalArgumentException("Default option must belong to line " + id);
        }
        this.defaultOptionId = defaultOptionId == null ? this.options.get(0).getId() : defaultOptionId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public SelectionMode getSelectionMode() { return selectionMode; }
    public List<CosmeticOption> getOptions() { return options; }
    public String getDefaultOptionId() { return defaultOptionId; }
}
