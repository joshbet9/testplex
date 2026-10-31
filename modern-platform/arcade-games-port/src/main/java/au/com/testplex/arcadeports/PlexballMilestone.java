package au.com.testplex.arcadeports;

/** Cumulative Plexball career milestones. Distance thresholds are stored in tenths of a metre. */
public enum PlexballMilestone {
    FIRST_MATCH("first-match", "First Match", Metric.MATCHES, 1),
    REGULAR("regular", "Regular", Metric.MATCHES, 10),
    VETERAN("veteran", "Veteran", Metric.MATCHES, 25),
    LOCAL_LEGEND("local-legend", "Local Legend", Metric.MATCHES, 50),

    FIRST_WIN("first-win", "First Win", Metric.WINS, 1),
    WINNER_CIRCLE("winner-circle", "Winner's Circle", Metric.WINS, 5),
    WINNING_HABIT("winning-habit", "Winning Habit", Metric.WINS, 10),
    DYNASTY("dynasty", "Dynasty", Metric.WINS, 25),

    FIRST_SWING("first-swing", "First Swing", Metric.AT_BATS, 1),
    BATTER_UP("batter-up", "Batter Up", Metric.AT_BATS, 10),
    POWERHOUSE("powerhouse", "Powerhouse", Metric.AT_BATS, 25),
    HOME_PLATE_HERO("home-plate-hero", "Home Plate Hero", Metric.AT_BATS, 50),

    FIRST_CATCH("first-catch", "First Catch", Metric.CATCHES, 1),
    GLOVE_WORK("glove-work", "Glove Work", Metric.CATCHES, 5),
    OUTFIELD_ACE("outfield-ace", "Outfield Ace", Metric.CATCHES, 15),
    WALL_OF_LEATHER("wall-of-leather", "Wall of Leather", Metric.CATCHES, 30),

    FIRST_HOME_RUN("first-home-run", "First Home Run", Metric.HOME_RUNS, 1),
    HOME_RUN_CLUB("home-run-club", "Home Run Club", Metric.HOME_RUNS, 5),
    LONG_BALL_PRO("long-ball-pro", "Long Ball Pro", Metric.HOME_RUNS, 10),
    DERBY_LEGEND("derby-legend", "Derby Legend", Metric.HOME_RUNS, 25),

    HUNDRED_METRES("hundred-metres", "100 Metres", Metric.DISTANCE_TENTHS, 1_000),
    HALF_KILOMETRE("half-kilometre", "Half a Kilometre", Metric.DISTANCE_TENTHS, 5_000),
    ONE_KILOMETRE("one-kilometre", "One Kilometre", Metric.DISTANCE_TENTHS, 10_000),
    FIVE_KILOMETRES("five-kilometres", "Five Kilometres", Metric.DISTANCE_TENTHS, 50_000);

    public enum Metric {
        MATCHES("matches played"),
        WINS("matches won"),
        AT_BATS("at-bats completed"),
        CATCHES("successful catches"),
        HOME_RUNS("home runs"),
        DISTANCE_TENTHS("uncaught hit distance");

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
