package au.com.testplex.modern.model;

import java.util.List;
import java.util.Objects;

/** Metadata and independent cosmetic hooks for one Arcade game port. */
public final class GameDefinition {
    public enum PortStatus {
        PLANNED("Not ported yet"),
        MECHANICS_IN_PROGRESS("Mechanics port in progress"),
        READY_FOR_TEST("Ready for in-game testing");

        private final String display;
        PortStatus(String display) { this.display = display; }
        public String getDisplay() { return display; }
    }

    private final String id;
    private final String name;
    private final String description;
    private final String icon;
    private final PortStatus portStatus;
    private final List<CosmeticLineDefinition> cosmeticLines;
    private final List<CosmeticOption> winEffects;
    private final String defaultWinEffectId;

    public GameDefinition(String id, String name, String description, String icon, PortStatus portStatus,
                          List<CosmeticLineDefinition> cosmeticLines, List<CosmeticOption> winEffects,
                          String defaultWinEffectId) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNullElse(description, "");
        this.icon = Objects.requireNonNullElse(icon, "CHEST");
        this.portStatus = Objects.requireNonNull(portStatus);
        this.cosmeticLines = List.copyOf(cosmeticLines);
        this.winEffects = List.copyOf(winEffects);
        if (this.winEffects.isEmpty()) throw new IllegalArgumentException("Every game needs a distinct win-effect line: " + id);
        if (this.winEffects.stream().noneMatch(o -> o.getId().equals(defaultWinEffectId))) {
            throw new IllegalArgumentException("Default win effect must exist for " + id);
        }
        this.defaultWinEffectId = defaultWinEffectId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public PortStatus getPortStatus() { return portStatus; }
    public List<CosmeticLineDefinition> getCosmeticLines() { return cosmeticLines; }
    public List<CosmeticOption> getWinEffects() { return winEffects; }
    public String getDefaultWinEffectId() { return defaultWinEffectId; }
    public boolean isReadyForTest() { return portStatus == PortStatus.READY_FOR_TEST; }
}
