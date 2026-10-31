package au.com.testplex.arcadeports;

/** Four long-term Plexball career goals. Distance thresholds are stored in tenths of a metre. */
public enum PlexballMilestone {
    WINNING_LEGEND("winning-legend", "Winning Legend", Metric.WINS, 250),
    WALL_OF_LEATHER("wall-of-leather", "Wall of Leather", Metric.CATCHES, 300),
    HOME_RUN_HERO("home-run-hero", "Home Run Hero", Metric.HOME_RUNS, 100),
    TEN_KILOMETRE_CLUB("ten-kilometre-club", "10 Kilometre Club", Metric.DISTANCE_TENTHS, 100_000);

    public enum Metric {
        WINS("matches won"),
        CATCHES("successful catches"),
        HOME_RUNS("home runs"),
        DISTANCE_TENTHS("total uncaught hit distance");

        private final String displayName;
        Metric(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    private final String slug;
    private final String displayName;
    private final Metric metric;
    private final long target;

    PlexballMilestone(String slug, String displayName, Metric metric, long target) {
        this.slug = slug;
        this.displayName = displayName;
        this.metric = metric;
        this.target = target;
    }

    public String slug() { return slug; }
    public String displayName() { return displayName; }
    public Metric metric() { return metric; }
    public long target() { return target; }

    public String configPath(java.util.UUID playerId) {
        return playerId == null ? "" : "players." + playerId + ".milestones." + slug;
    }

    public String targetLabel() {
        return metric == Metric.DISTANCE_TENTHS
                ? String.format(java.util.Locale.ROOT, "%.1fm", target / 10.0D)
                : Long.toString(target);
    }
}
