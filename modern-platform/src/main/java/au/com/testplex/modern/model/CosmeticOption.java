package au.com.testplex.modern.model;

import java.util.Objects;

/** A game-specific option. Options are deliberately not shared global cosmetic skins. */
public final class CosmeticOption {
    private final String id;
    private final String name;
    private final String description;
    private final String selectionGroup;

    public CosmeticOption(String id, String name, String description) {
        this(id, name, description, null);
    }

    /**
     * Grouped options are independently equipped within one MULTI cosmetic line.
     * Selecting another option in the same group replaces the previous choice,
     * while selections from other groups remain equipped.
     */
    public CosmeticOption(String id, String name, String description, String selectionGroup) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.selectionGroup = selectionGroup == null || selectionGroup.isBlank()
                ? null : selectionGroup.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSelectionGroup() { return selectionGroup; }
}
