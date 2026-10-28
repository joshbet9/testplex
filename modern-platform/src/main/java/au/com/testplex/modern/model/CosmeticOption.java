package au.com.testplex.modern.model;

import java.util.Objects;

/** A game-specific option. Options are deliberately not shared global cosmetic skins. */
public final class CosmeticOption {
    private final String id;
    private final String name;
    private final String description;

    public CosmeticOption(String id, String name, String description) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
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
}
