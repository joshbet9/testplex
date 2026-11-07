package au.com.testplex.arcadeports;

/** Deterministic thresholds for the local Micro Battles achievement triggers. */
public final class MicroBattlesAchievementRules {
    public static final double BORDER_BUILD_MAX_EDGE_DISTANCE = 2.0D;
    public static final int BORDER_BUILD_REQUIRED_BLOCKS = 3;
    public static final long PLACED_BLOCK_COLLISION_WINDOW_MILLIS = 3_000L;
    public static final long PLACED_BLOCK_KNOCK_WINDOW_MILLIS = 5_000L;
    public static final double PLACED_BLOCK_COLLISION_RADIUS = 0.85D;
    public static final long SNIPER_KNOCK_WINDOW_MILLIS = 12_000L;
    public static final double SNIPER_MIN_SKYBASE_HEIGHT = 4.0D;
    public static final double SNIPER_MIN_FALL_DISTANCE = 8.0D;
    public static final int SNIPER_REQUIRED_OPPOSING_TEAMS = 3;
    public static final int MICRO_BATTLES_KING_KILLS = 3;
    public static final int LAST_TWO_REQUIRED_TEAM_SWEEP = 1;
    public static final int SHIFT_SUPERVISOR_REQUIRED_BLOCKS = 30;
    public static final double NEEDLE_THREADER_MIN_DISTANCE = 25.0D;
    public static final double AIR_MAIL_MIN_GAP_DISTANCE = 6.0D;
    public static final double LAST_BREATH_MAX_HEALTH = 2.0D;

    private MicroBattlesAchievementRules() { }

    public static boolean isBorderBuildDistance(double edgeDistance) {
        return Double.isFinite(edgeDistance) && edgeDistance >= 0.0D
                && edgeDistance <= BORDER_BUILD_MAX_EDGE_DISTANCE;
    }

    /** The Fighter perk must be the sole reason a player survives this final-duel hit. */
    public static boolean isCounterfactualFighterSave(double health,
            double fighterDamage, double damageWithoutFighterPerk) {
        return Double.isFinite(health) && Double.isFinite(fighterDamage)
                && Double.isFinite(damageWithoutFighterPerk) && health > 0.0D
                && fighterDamage >= 0.0D && damageWithoutFighterPerk >= health
                && fighterDamage < health;
    }

    public static boolean isRecent(long nowMillis, long eventMillis, long windowMillis) {
        return eventMillis > 0L && nowMillis >= eventMillis
                && nowMillis - eventMillis <= windowMillis;
    }

    public static boolean isApproachingPlacedBlock(double fromX, double fromZ,
            double toX, double toZ, double blockCenterX, double blockCenterZ) {
        double beforeX = blockCenterX - fromX;
        double beforeZ = blockCenterZ - fromZ;
        double afterX = blockCenterX - toX;
        double afterZ = blockCenterZ - toZ;
        double beforeDistance = beforeX * beforeX + beforeZ * beforeZ;
        double afterDistance = afterX * afterX + afterZ * afterZ;
        double movementX = toX - fromX;
        double movementZ = toZ - fromZ;
        return beforeDistance > afterDistance + 0.0001D
                && movementX * beforeX + movementZ * beforeZ > 0.0001D
                && afterDistance <= PLACED_BLOCK_COLLISION_RADIUS * PLACED_BLOCK_COLLISION_RADIUS;
    }

    public static boolean isSniperKnockOff(long nowMillis, long hitAtMillis,
            double hitY, double eliminatedY) {
        return isRecent(nowMillis, hitAtMillis, SNIPER_KNOCK_WINDOW_MILLIS)
                && Double.isFinite(hitY) && Double.isFinite(eliminatedY)
                && hitY - eliminatedY >= SNIPER_MIN_FALL_DISTANCE;
    }

    public static boolean shouldAwardMicroBattlesKing(int placedBlockKills) {
        return placedBlockKills >= MICRO_BATTLES_KING_KILLS;
    }

    public static boolean hasKnockedEveryOpposingTeam(int distinctTeamsKnocked) {
        return distinctTeamsKnocked >= SNIPER_REQUIRED_OPPOSING_TEAMS;
    }

    public static boolean shouldAwardPhew(int borderBlocksPlaced, boolean diedToBorder) {
        return !diedToBorder && borderBlocksPlaced >= BORDER_BUILD_REQUIRED_BLOCKS;
    }

    public static boolean shouldAwardMoshPitPlayroom(int assists) {
        return assists >= 8;
    }

    public static boolean shouldAwardLastTwo(int opponentsAtSnapshot, int opponentsPersonallyEliminated,
            boolean winner, boolean survived) {
        return winner && survived && opponentsAtSnapshot >= LAST_TWO_REQUIRED_TEAM_SWEEP
                && opponentsPersonallyEliminated >= opponentsAtSnapshot;
    }

    public static boolean shouldAwardBuiltForThis(boolean wasSavedByFighter, boolean wonFinalDuel) {
        return wasSavedByFighter && wonFinalDuel;
    }

    public static boolean shouldAwardShiftSupervisor(int blocksBroken, int blocksPlaced, boolean wonAsWorker) {
        return wonAsWorker && blocksBroken >= SHIFT_SUPERVISOR_REQUIRED_BLOCKS
                && blocksPlaced >= SHIFT_SUPERVISOR_REQUIRED_BLOCKS;
    }

    public static boolean shouldAwardNoRoomToBuild(int blocksPlaced, boolean won) {
        return won && blocksPlaced == 0;
    }

    public static boolean shouldAwardLastBreath(double health, boolean finalDuelWinner) {
        return finalDuelWinner && Double.isFinite(health) && health > 0.0D
                && health <= LAST_BREATH_MAX_HEALTH;
    }

    public static boolean isNeedleThreaderDistance(double shotDistance) {
        return Double.isFinite(shotDistance) && shotDistance >= NEEDLE_THREADER_MIN_DISTANCE;
    }

    public static boolean isAirMailLanding(double horizontalDistance, int startingTeam, int landingTeam,
            boolean landedOnSolidGround) {
        return Double.isFinite(horizontalDistance) && horizontalDistance >= AIR_MAIL_MIN_GAP_DISTANCE
                && startingTeam >= 0 && landingTeam >= 0 && startingTeam != landingTeam
                && landedOnSolidGround;
    }
}
