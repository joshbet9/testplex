package au.com.testplex.arcadeports;

import java.util.Locale;

/** Normalizes an optional cosmetic selection while preserving an explicit disabled state. */
final class CosmeticSelectionRules {
    private CosmeticSelectionRules() { }

    static String normalizeOrDisabled(String selected) {
        return selected == null || selected.isBlank() ? null : selected.trim().toLowerCase(Locale.ROOT);
    }
}
