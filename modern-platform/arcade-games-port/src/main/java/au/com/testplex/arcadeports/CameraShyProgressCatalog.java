package au.com.testplex.arcadeports;

import java.util.List;

/** Exact Snap progression contract used by match awards and the /stats menu. */
public final class CameraShyProgressCatalog {
    public record Achievement(String id, String name, String requirement) { }
    public record Milestone(String id, String name, String statKey, int target, String unit) { }

    private static final List<Achievement> ACHIEVEMENTS = List.of(
            a("smoke-and-mirrors", "Smoke and Mirrors", "Escape three encounters using Smoke Artist."),
            a("false-alarm", "False Alarm", "Successfully fool three opponents using Decoy."),
            a("catch-me-if-you-can", "Catch Me If You Can", "Escape a player after using Runner."),
            a("private-investigator", "Private Investigator", "Detect and photograph three different players using Spotter."),
            a("method-actor", "Method Actor", "Successfully fool an opponent using Understudy."),
            a("revenge-is-sweet", "Revenge Is Sweet", "Eliminate three players who previously photographed you in one game."),
            a("double-elimination", "Double Elimination", "Eliminate two opponents within 15 seconds."),
            a("picture-perfect", "Picture Perfect", "Win a match with at least 10 successful photographs taken."),
            a("camera-shy", "Snap", "Win without receiving a photograph."),
            a("stage-fright", "Stage Fright", "Survive 60 seconds with four exposures."),
            a("narrow-escape", "Narrow Escape", "Win while having four exposures."),
            a("untouchable", "Untouchable", "Win three consecutive games."),
            a("out-of-focus", "Out of Focus", "Survive a photograph attempt that nearly catches you, then escape."),
            a("now-you-see-me", "Now You See Me", "Escape three successful photographer encounters in one match."),
            a("paparazzi", "Paparazzi", "Take 5 photographs of different opponents in one match."),
            a("double-take", "Double Take", "Photograph two different players within 5 seconds."),
            a("close-encounter", "Close Encounter", "Photograph someone within 3 blocks."),
            a("long-distance-photography", "Long Distance Photography", "Photograph someone from at least 18 blocks away."),
            a("candid-camera", "Candid Camera", "Photograph someone while they are using their spyglass."),
            a("group-portrait", "Group Portrait", "Photograph two opponents with one Group Photo.")
    );

    private static final List<Milestone> MILESTONES = List.of(
            new Milestone("wins", "Snap Champion", "wins", 250, "wins"),
            new Milestone("eliminations", "Serial Photographer", "eliminations", 500, "eliminations"),
            new Milestone("photos", "Master Photographer", "photos", 1_000, "photos")
    );

    private CameraShyProgressCatalog() { }

    private static final int ACHIEVEMENTS_PER_PAGE = 5;

    public static List<Achievement> achievements() { return ACHIEVEMENTS; }
    public static List<Milestone> milestones() { return MILESTONES; }

    public static int pageCount() {
        return (ACHIEVEMENTS.size() + ACHIEVEMENTS_PER_PAGE - 1) / ACHIEVEMENTS_PER_PAGE;
    }

    /** Page numbers are one-based and clamp safely at either end of the catalogue. */
    public static List<Achievement> page(int requestedPage) {
        int page = Math.max(1, Math.min(requestedPage, pageCount()));
        int start = (page - 1) * ACHIEVEMENTS_PER_PAGE;
        int end = Math.min(start + ACHIEVEMENTS_PER_PAGE, ACHIEVEMENTS.size());
        return ACHIEVEMENTS.subList(start, end);
    }

    public static Achievement achievement(String id) {
        return ACHIEVEMENTS.stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
    }

    private static Achievement a(String id, String name, String requirement) {
        return new Achievement(id, name, requirement);
    }
}
