package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.bukkit.Color;
import org.bukkit.util.Vector;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * Small deterministic rules used by the mechanics-first Arcade prototypes.
 * Keeping timing and hazard classification here makes them testable without
 * needing a live Paper server.
 */
public final class ArcadePortRules {
    public static final long DEATH_TAG_CHASER_PROMOTION_INTERVAL_MILLIS = 30_000L;
    /** DeathTag inherits the 20-minute timeout from the legacy Arcade Game base class. */
    public static final long DEATH_TAG_GAME_TIMEOUT_MILLIS = 1_200_000L;
    public static final int DEATH_TAG_FORCED_CHASER_REWARD_GEMS = 10;
    public static final int DEATH_TAG_RUNNER_KILL_REWARD_GEMS = 4;
    public static final int DEATH_TAG_FIRST_PLACE_REWARD_GEMS = 15;
    public static final int DEATH_TAG_SECOND_PLACE_REWARD_GEMS = 10;
    public static final int DEATH_TAG_THIRD_PLACE_REWARD_GEMS = 5;
    public static final int DEATH_TAG_PARTICIPATION_REWARD_GEMS = 10;
    public static final long DEATH_TAG_ARCHER_ARROW_INTERVAL_MILLIS = 2_000L;
    public static final int DEATH_TAG_ARCHER_MAX_ARROWS = 2;
    public static final double DEATH_TAG_ARCHER_ARROW_KNOCKBACK_POWER = 3.0D;
    public static final long DEATH_TAG_TRAITOR_KNOCKBACK_COOLDOWN_MILLIS = 400L;
    public static final int EVOLUTION_WIN_LEVEL = 6;
    public static final double DRAGON_ESCAPE_FINISH_X = 0.5D;
    public static final double DRAGON_ESCAPE_FINISH_Y = 64.0D;
    public static final double DRAGON_ESCAPE_FINISH_Z = 77.5D;
    public static final double DRAGON_ESCAPE_CHASE_SPEED = 0.2D;
    public static final double DRAGON_ESCAPE_BLOCK_RADIUS = 10.0D;
    public static final double DRAGON_ESCAPE_FINISH_RADIUS = 3.0D;
    public static final double DRAGON_ESCAPE_RACE_GAP_ESTIMATED_SPEED = 5.0D;
    /** Only discard effectively coincident markers; close BLACK markers are valid route steps. */
    public static final double DRAGON_ESCAPE_DUPLICATE_WAYPOINT_DISTANCE = 0.75D;
    public static final String[] DRAGON_ESCAPE_BEACON_COLORS = {
            "white", "red", "orange", "yellow", "lime", "green", "cyan",
            "light-blue", "blue", "purple", "magenta", "pink"
    };

    public static String dragonEscapeMapStorageKey(String mapResource) {
        if (mapResource == null || mapResource.isBlank()) return "";
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                mapResource.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String formatDragonEscapeTime(long millis) {
        if (millis < 0L) return "--:--.---";
        long minutes = millis / 60_000L;
        long seconds = millis / 1_000L % 60L;
        long ms = millis % 1_000L;
        return String.format(java.util.Locale.ROOT, "%d:%02d.%03d", minutes, seconds, ms);
    }

    public static String formatDragonEscapeDelta(long deltaMillis) {
        if (deltaMillis == 0L) return "0:00.000";
        return (deltaMillis > 0L ? "+" : "-") + formatDragonEscapeTime(Math.abs(deltaMillis));
    }
    public static final long RUNNER_BLOCK_DECAY_MILLIS = 600L;
    public static final long RUNNER_BLOCK_LIFETIME_MILLIS = 1_200L;
    public static final long RUNNER_LEAP_COOLDOWN_MILLIS = 8_000L;
    public static final long RUNNER_QUICKSHOT_COOLDOWN_MILLIS = 6_000L;
    public static final long RUNNER_FROSTBALL_INTERVAL_MILLIS = 500L;
    public static final int RUNNER_MAX_FROSTBALLS = 16;
    public static final int RUNNER_FROSTBALL_SLOW_TICKS = 40;
    public static final int RUNNER_FROSTBALL_SLOW_AMPLIFIER = 2;
    public static final double RUNNER_STEPS_ACHIEVEMENT_BLOCKS = 400.0D;
    public static final int RUNNER_HOP_ACHIEVEMENT_JUMPS = 100;
    public static final long RUNNER_FLASH_ACHIEVEMENT_MAX_WIN_MILLIS = 90_000L;
    private static final java.util.List<String> RUNNER_TRAIL_COLOURS = java.util.List.of(
            "cyan", "white", "orange", "magenta", "light-blue", "yellow", "lime",
            "pink", "gray", "light-gray", "purple", "blue", "brown", "green", "black");
    /** Available non-red Runner trail colours, in deterministic fallback order. */
    public static java.util.List<String> runnerTrailColours() {
        return RUNNER_TRAIL_COLOURS;
    }

    /** Filter teleports and implausible position corrections out of the walk-distance counter. */
    public static boolean shouldCountRunnerWalkSegment(double horizontalBlocks) {
        return Double.isFinite(horizontalBlocks) && horizontalBlocks > 0.0D && horizontalBlocks <= 2.0D;
    }

    /** The source wording is "over 400", so the threshold is strictly greater than 400. */
    public static boolean shouldAwardRunnerSteps(double walkedBlocks) {
        return Double.isFinite(walkedBlocks) && walkedBlocks > RUNNER_STEPS_ACHIEVEMENT_BLOCKS;
    }

    public static boolean shouldAwardRunnerHopToIt(int jumps) {
        return jumps >= RUNNER_HOP_ACHIEVEMENT_JUMPS;
    }

    public static boolean shouldAwardRunnerFlash(long elapsedMillis, boolean won) {
        return won && elapsedMillis >= 0L && elapsedMillis <= RUNNER_FLASH_ACHIEVEMENT_MAX_WIN_MILLIS;
    }

    /**
     * Avoid a trail colour already present on the selected map's exposed palette.
     * Prefer the first unused alternative; if all choices occur, use the least
     * represented alternative (excluding the requested colour).
     */
    public static String resolveRunnerTrailColour(
            String requestedColour, java.util.Map<String, Integer> mapPaletteCounts) {
        String requested = requestedColour == null ? "" : requestedColour.toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace("trail-", "");
        if (!RUNNER_TRAIL_COLOURS.contains(requested)) requested = "cyan";
        if (mapPaletteCounts == null || Math.max(0, mapPaletteCounts.getOrDefault(requested, 0)) == 0) {
            return requested;
        }

        String fallback = null;
        int lowestUse = Integer.MAX_VALUE;
        for (String candidate : RUNNER_TRAIL_COLOURS) {
            if (candidate.equals(requested)) continue;
            int use = Math.max(0, mapPaletteCounts.getOrDefault(candidate, 0));
            if (use < lowestUse) {
                fallback = candidate;
                lowestUse = use;
            }
        }
        return fallback == null ? "cyan" : fallback;
    }

    /** Meter charge comes from the source damage value recorded for each successful hit. */
    public static final int BACON_BLAST_PROJECTILE_STACK_SIZE = 16;
    public static final int BACON_METER_MAX = 100;
    public static final int BACON_METER_MIN_ABILITY = 10;
    public static final double BACON_METER_GAIN_PER_DAMAGE = 5.0D;
    public static final long BACON_TIMED_HIT_SPAM_WINDOW_MILLIS = 250L;
    public static final long BACON_TIMED_HIT_GOOD_MIN_MILLIS = 500L;
    public static final long BACON_TIMED_HIT_GOOD_MAX_MILLIS = 900L;
    public static final long BACON_TIMED_HIT_OK_MIN_MILLIS = 251L;
    public static final long BACON_TIMED_HIT_OK_MAX_MILLIS = 499L;
    public static final long BACON_TIMED_HIT_LATE_MAX_MILLIS = 1_400L;
    public static final long BACON_HIT_FEEDBACK_MILLIS = 1_000L;
    /** The Pig kit's damage meter controls its invincibility window, capped at five seconds. */
    public static final long BACON_PIG_INVINCIBILITY_MIN_MILLIS = 500L;
    public static final long BACON_PIG_INVINCIBILITY_MAX_MILLIS = 5_000L;

    public static boolean isBaconInvincibilityActive(long nowMillis, long invincibleUntilMillis) {
        return invincibleUntilMillis > 0L && nowMillis < invincibleUntilMillis;
    }
    public static final long BACON_FLASH_FRIED_MAX_WIN_MILLIS = 60_000L;
    public static final long BOMB_LOBBERS_ASSIST_WINDOW_MILLIS = 10_000L;
    public static final long BOMB_LOBBERS_POWERUP_FIRST_SPAWN_DELAY_MILLIS = 10_000L;
    public static final double BOMB_LOBBERS_SUPER_TNT_EXPLOSION_RADIUS = 10.0D;
    public static final double BOMB_LOBBERS_SUPER_TNT_MAX_DAMAGE = 65.0D;
    public static final int BOMB_LOBBERS_POWERUP_SPEED_DURATION_TICKS = 240;
    public static final int BOMB_LOBBERS_ULTIMATE_KILLER_KILLS = 5;
    public static final int BOMB_LOBBERS_TWO_BIRDS_KILLS = 2;
    public static final int BOMB_LOBBERS_TEAM_PLAYER_ASSISTS = 5;
    public static final int BOMB_LOBBERS_MASTER_BOMBER_WINS = 30;
    public static final int BACON_THREE_FOR_ONE_BODY_SLAM_KILLS = 3;
    /** Pigs In Blankets is earned by six kills in a single game. */
    public static final int BACON_BRAWL_PIGS_IN_BLANKETS_KILLS = 6;
    /** The updated King of Bacon achievement requires fifteen wins on the isolated test server. */
    public static final int BACON_BRAWL_KING_OF_BACON_WINS = 15;

    public static boolean shouldAwardBaconKingOfBacon(int totalWins) {
        return totalWins >= BACON_BRAWL_KING_OF_BACON_WINS;
    }
    public static int baconMeterGainForDamage(double damage) {
        if (!Double.isFinite(damage) || damage <= 0.0D) return 0;
        return (int) Math.min(BACON_METER_MAX,
                Math.ceil(damage * BACON_METER_GAIN_PER_DAMAGE));
    }

    public static int nextBaconMeter(int current, double damage) {
        int charge = Math.max(0, Math.min(BACON_METER_MAX, current));
        return Math.min(BACON_METER_MAX, charge + baconMeterGainForDamage(damage));
    }

    public static boolean canUseBaconMeterAbility(int percent) {
        return percent >= BACON_METER_MIN_ABILITY;
    }

    /** Lower charge still permits the ability; higher charge increases its knockback. */
    public static double baconBodySlamMeterMultiplier(int percent) {
        int charge = Math.max(0, Math.min(BACON_METER_MAX, percent));
        return 0.5D + charge / 200.0D;
    }

    /** Meter-powered Pig Cloak and Brutus Bacon abilities share a charge-scaled duration. */
    public static long baconAbilityDurationMillis(int percent) {
        int charge = Math.max(0, Math.min(BACON_METER_MAX, percent));
        if (!canUseBaconMeterAbility(charge)) return 0L;
        return BACON_PIG_INVINCIBILITY_MIN_MILLIS
                + Math.round((BACON_PIG_INVINCIBILITY_MAX_MILLIS
                        - BACON_PIG_INVINCIBILITY_MIN_MILLIS) * charge / (double) BACON_METER_MAX);
    }

    /** Retained as a compatibility alias for earlier tests and callers. */
    public static long baconPigInvincibilityDurationMillis(int percent) {
        return baconAbilityDurationMillis(percent);
    }

    public static boolean isBaconTimedCritical(long intervalMillis) {
        return intervalMillis >= BACON_TIMED_HIT_GOOD_MIN_MILLIS
                && intervalMillis <= BACON_TIMED_HIT_GOOD_MAX_MILLIS;
    }

    /** Brutus blocks a frontal hit; a true backstab disables the guard before normal knockback. */
    public static boolean shouldBaconBrutusGuardHit(boolean active, boolean backstab) {
        return active && !backstab;
    }

    /** Mama Piggles spends meter to set Bacon Blast launch and impact power. */
    public static double baconBlastPowerMultiplier(int percent) {
        int charge = Math.max(0, Math.min(BACON_METER_MAX, percent));
        return 0.5D + 1.5D * charge / BACON_METER_MAX;
    }

    /** Spamming within the same short click window weakens hits; well-timed hits are rewarded. */
    public static double baconTimedHitKnockbackMultiplier(long intervalMillis) {
        if (intervalMillis < 0L) return 1.0D;
        if (intervalMillis <= BACON_TIMED_HIT_SPAM_WINDOW_MILLIS) return 0.65D;
        if (intervalMillis >= BACON_TIMED_HIT_GOOD_MIN_MILLIS
                && intervalMillis <= BACON_TIMED_HIT_GOOD_MAX_MILLIS) return 1.6D;
        if (intervalMillis >= BACON_TIMED_HIT_OK_MIN_MILLIS
                && intervalMillis <= BACON_TIMED_HIT_OK_MAX_MILLIS) return 1.1D;
        if (intervalMillis <= BACON_TIMED_HIT_LATE_MAX_MILLIS) return 1.25D;
        return 1.0D;
    }

    public static final double BACON_AVATAR_MAX_ATTACK_DISTANCE_SQUARED = 12.25D;

    /** Packet-hit remapping must still respect the live round, target eligibility, reach and LOS. */
    public static boolean shouldAcceptBaconAvatarPacketHit(boolean activeBaconRound,
            boolean attackerAlive, boolean targetAlive, boolean selfHit,
            double distanceSquared, boolean hasLineOfSight) {
        return activeBaconRound && attackerAlive && targetAlive && !selfHit
                && Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared <= BACON_AVATAR_MAX_ATTACK_DISTANCE_SQUARED
                && hasLineOfSight;
    }

    /** Reborn kit selection is a pre-round choice; live Bacon Brawl changes apply next game. */
    public static boolean shouldDeferBaconBrawlKitChange(boolean activeBaconRound) {
        return activeBaconRound;
    }

    /** Stable test-server key; production achievement storage remains a separate integration. */
    public static String baconBrawlAchievementConfigKey(java.util.UUID playerId, String achievementSlug) {
        if (playerId == null || achievementSlug == null
                || !achievementSlug.matches("[a-z0-9-]+")) return "";
        return "test-achievements.bacon-brawl." + playerId + "." + achievementSlug;
    }

    public static boolean shouldAwardBaconPigsInBlankets(int killsThisGame) {
        return killsThisGame >= BACON_BRAWL_PIGS_IN_BLANKETS_KILLS;
    }

    public static boolean shouldAwardBaconFlashFried(long roundMillis) {
        return roundMillis >= 0L && roundMillis <= BACON_FLASH_FRIED_MAX_WIN_MILLIS;
    }

    public static boolean shouldAwardBaconRawPork(boolean usedAbilityThisGame) {
        return !usedAbilityThisGame;
    }

    public static boolean shouldAwardBaconThreeForOne(int bodySlamKills) {
        return bodySlamKills >= BACON_THREE_FOR_ONE_BODY_SLAM_KILLS;
    }

    public static boolean shouldAwardBombLobbersUltimateKiller(int killsThisGame) {
        return killsThisGame >= BOMB_LOBBERS_ULTIMATE_KILLER_KILLS;
    }
    public static boolean shouldAwardBombLobbersTwoBirdsOneStone(int killsFromOneTnt) {
        return killsFromOneTnt >= BOMB_LOBBERS_TWO_BIRDS_KILLS;
    }
    public static int nextBombLobbersTntKillCount(int currentKills, boolean creditedKill) {
        int current = Math.max(0, currentKills);
        return creditedKill ? Math.min(64, current + 1) : current;
    }
    public static boolean shouldAwardBombLobbersTeamPlayer(int assistsThisGame) {
        return assistsThisGame >= BOMB_LOBBERS_TEAM_PLAYER_ASSISTS;
    }
    public static boolean shouldAwardBombLobbersDoubleJump(boolean won, boolean aliveAtFinish, boolean usedDoubleJump) {
        return won && aliveAtFinish && !usedDoubleJump;
    }
    public static boolean shouldAwardBombLobbersMasterBomber(int winsAfterCurrentGame) {
        return winsAfterCurrentGame >= BOMB_LOBBERS_MASTER_BOMBER_WINS;
    }
    public static String bombLobbersAchievementConfigKey(java.util.UUID playerId, String achievementSlug) {
        if (playerId == null || achievementSlug == null || !achievementSlug.matches("[a-z0-9-]+")) return "";
        return "test-achievements.bomb-lobbers." + playerId + "." + achievementSlug;
    }

    /** Suppress vanilla health damage caused synchronously by our non-block-breaking Bacon Blast impact. */
    public static boolean shouldSuppressBaconBlastHealthDamage(boolean baconBrawl,
            boolean duringTrackedBlastImpact,
            org.bukkit.event.entity.EntityDamageEvent.DamageCause cause) {
        return baconBrawl && duringTrackedBlastImpact
                && (cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                    || cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION);
    }

    /** Bacon Blast calls the source explosion with block damage disabled. */
    public static boolean baconBlastShouldBreakBlock(org.bukkit.Material material, double distanceSquared) {
        return false;
    }

    /** Recent hit credit keeps knock-off kills attached to the attacker who caused the fall. */
    public static final long BACON_FALL_KILL_CREDIT_WINDOW_MILLIS = 5_000L;

    /** Hazards that end a Bacon Brawl knock-off on the embedded arena, including its lava floor. */
    public static boolean isBaconArenaKnockoutReason(String reason) {
        if (reason == null) return false;
        String value = reason.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').trim();
        return value.equals("fell from the arena")
                || value.equals("fall")
                || value.equals("was lost to the water")
                || value.equals("lava")
                || value.equals("fire")
                || value.equals("hot floor")
                || value.equals("magma block")
                || value.equals("drowning")
                || value.equals("void");
    }

    public static boolean shouldCreditBaconFallKill(long nowMillis, long lastHitAtMillis,
            boolean isArenaFallOrWaterKnockout) {
        return isArenaFallOrWaterKnockout && lastHitAtMillis > 0L
                && nowMillis >= lastHitAtMillis
                && nowMillis - lastHitAtMillis <= BACON_FALL_KILL_CREDIT_WINDOW_MILLIS;
    }

    /** Reborn Bacon Brawl has no fixed timer; SoloGame ends when at most one player remains alive. */
    public static final boolean BACON_BRAWL_HAS_ROUND_TIMER = false;
    public static final boolean DRAGON_ESCAPE_HAS_ROUND_TIMER = false;
    public static final long BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS = 50_000L;
    public static final long BACON_KNOCKBACK_INTERVAL_STEP_MILLIS = 5_000L;
    public static final long BACON_MIN_KNOCKBACK_INTERVAL_MILLIS = 5_000L;
    public static final long BACON_SLOW_UPDATE_INTERVAL_MILLIS = 4_000L;
    public static final long BACON_HUNGER_INTERVAL_MILLIS = 4_000L;
    public static final int BACON_HUNGER_DRAIN_PER_INTERVAL = 2;
    public static final float BACON_SATURATION_ON_FOOD_CHANGE = 5.0F;
    public static final double BACON_STARVATION_DAMAGE = 4.0D;
    /** Ordinary Bacon Brawl hits use knockback; health loss is reserved for arena/environment hazards. */
    public static final boolean BACON_BRAWL_HITS_DAMAGE_HEALTH = false;
    /** Body Slam applies a second knockback component in addition to the game-wide knockback. */
    public static final double BACON_BODY_SLAM_KNOCKBACK_MULTIPLIER = 2.0D;

    /** Bacon Blast is a thrown pork item that ends on impact or after five seconds in flight. */
    public static final long BACON_BLAST_MAX_FLIGHT_MILLIS = 5_000L;
    /** Reborn's ProjectileManager AddThrow uses a 0.5-block collision radius. */
    public static final double BACON_BLAST_PLAYER_COLLISION_RADIUS_SQUARED = 0.25D;

    public static boolean isBaconBlastBlockImpact(boolean onGround, boolean inLiquid,
            boolean insideSolidBlock, boolean stationaryAgainstSolidBlock) {
        return onGround || inLiquid || insideSolidBlock || stationaryAgainstSolidBlock;
    }

    public static boolean isBaconBlastNearPlayer(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared <= BACON_BLAST_PLAYER_COLLISION_RADIUS_SQUARED;
    }

    /**
     * Resolve swept Bacon Blast collision order. A player's collision radius may touch the
     * projectile before its centre projection reaches a wall; otherwise the first block wins.
     * blockFraction < 0 means the swept path contains no block collision.
     */
    public static boolean isBaconBlastPlayerBeforeBlock(double playerFraction,
            double blockFraction, double segmentLength) {
        if (!Double.isFinite(playerFraction) || playerFraction < 0.0D || playerFraction > 1.0D
                || !Double.isFinite(segmentLength) || segmentLength < 0.0D) return false;
        if (!Double.isFinite(blockFraction) || blockFraction < 0.0D) return true;
        if (blockFraction > 1.0D || segmentLength <= 1.0E-6D) return false;
        double playerNearEdge = playerFraction
                - Math.sqrt(BACON_BLAST_PLAYER_COLLISION_RADIUS_SQUARED) / segmentLength;
        return playerNearEdge <= blockFraction;
    }

    public static boolean shouldExpireBaconBlast(long nowMillis, long createdAtMillis, boolean touchingBlock) {
        return touchingBlock || createdAtMillis <= 0L || nowMillis < createdAtMillis
                || nowMillis - createdAtMillis >= BACON_BLAST_MAX_FLIGHT_MILLIS;
    }
    /** Reborn's inherited Game.GameTimeout is 20 minutes. */
    public static final long BOMB_LOBBERS_ROUND_MILLIS = 1_200_000L;
    public static final long BOMB_LOBBERS_WALL_CONTACT_DELAY_MILLIS = 3_000L;
    /** GameTeam.GetFormattedName() colours the configured "Red"/"Blue" team name. */
    public static String bombLobbersTeamBadgeName(int teamId) {
        return teamId == 1 ? "Blue" : "Red";
    }

    /** The source spectator-compass menu rate-limits direct teleports for three seconds. */
    public static final long BOMB_LOBBERS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS = 3_000L;

    public static boolean shouldBombLobbersCompassTrackTeam(int spectatorTeam, int targetTeam) {
        return targetTeam >= 0 && (spectatorTeam < 0 || spectatorTeam != targetTeam);
    }

    public static boolean isBombLobbersSpectatorTeleportReady(long nowMillis, long readyAtMillis) {
        return nowMillis >= 0L && readyAtMillis >= 0L && nowMillis >= readyAtMillis;
    }

    public static long nextBombLobbersSpectatorTeleportAt(long nowMillis) {
        if (nowMillis < 0L) return BOMB_LOBBERS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS;
        if (nowMillis > Long.MAX_VALUE - BOMB_LOBBERS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS) {
            return Long.MAX_VALUE;
        }
        return nowMillis + BOMB_LOBBERS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS;
    }

    /** Reborn's Waller perk uses a 100 ms Recharge gate between wall placements. */
    public static final long BOMB_LOBBERS_WALLER_COOLDOWN_MILLIS = 100L;
    public static final long BOMB_LOBBERS_MAX_FLIGHT_MILLIS = 8_000L;
    public static final int BOMB_LOBBERS_FUSE_TICKS = 60;
    public static final int BOMB_LOBBERS_MAX_CARRIED = 3;
    /** Reborn renders two dust particles per 0.2-block segment while TNT is airborne. */
    public static final double BOMB_LOBBERS_TRAIL_SAMPLE_SPACING = 0.2D;
    public static final int BOMB_LOBBERS_TEAM_BADGE_SLOT = 8;
    /** Vanilla 1.8 TNT size parameter; entity damage reaches twice this distance. */
    public static final double BOMB_LOBBERS_EXPLOSION_SIZE = 4.0D;
    public static final double BOMB_LOBBERS_EXPLOSION_RADIUS = BOMB_LOBBERS_EXPLOSION_SIZE * 2.0D;
    public static final double BOMB_LOBBERS_MAX_EXPLOSION_DAMAGE = 50.0D;
    public static final long SPLEEF_HUNGER_DRAIN_INTERVAL_MILLIS = 4_000L;
    public static final int SPLEEF_HUNGER_DRAIN_PER_INTERVAL = 2;
    public static final long SPLEEF_HUNGER_RESTORE_COOLDOWN_MILLIS = 50L;
    public static final long SPLEEF_ARCHER_ARROW_INTERVAL_MILLIS = 2_000L;
    public static final int SPLEEF_ARCHER_MAX_ARROWS = 2;
    public static final int SPLEEF_SNOWBALLER_MAX_SNOWBALLS = 16;
    public static final long SPLEEF_BRAWLER_LEAP_COOLDOWN_MILLIS = 30_000L;
    public static final long SPLEEF_KNOCKBACK_COOLDOWN_MILLIS = 400L;
    public static final double SPLEEF_DEFAULT_KNOCKBACK = 0.3D;
    public static final double SPLEEF_BRAWLER_KNOCKBACK = 0.6D;
    public static final long SPLEEF_SMASHER_COOLDOWN_MILLIS = 250L;

    /** Reborn PerkLeap: forward power 1.2, Y +0.2, max Y 1.2, plus a 0.2 ground boost. */
    public static Vector spleefLeapVelocity(Vector facing, boolean grounded) {
        if (facing == null || !Double.isFinite(facing.getX()) || !Double.isFinite(facing.getY())
                || !Double.isFinite(facing.getZ()) || facing.lengthSquared() <= 1.0E-12D) {
            return new Vector(0.0D, 0.0D, 0.0D);
        }
        Vector velocity = facing.clone().normalize().multiply(1.2D);
        velocity.setY(Math.min(1.2D, velocity.getY() + 0.2D + (grounded ? 0.2D : 0.0D)));
        return velocity;
    }
    public static final long TURF_WARS_GAME_TIMEOUT_MILLIS = 900_000L;
    public static final long TURF_WARS_INITIAL_BUILD_PHASE_MILLIS = 40_000L;
    public static final long TURF_WARS_BUILD_PHASE_MILLIS = 20_000L;
    public static final long TURF_WARS_COMBAT_PHASE_MILLIS = 90_000L;
    public static final long TURF_WARS_CONSTRUCTOR_INTERVAL_MILLIS = 4_000L;
    public static final long TURF_WARS_MARKSMAN_ARROW_INTERVAL_MILLIS = 2_000L;
    public static final long TURF_WARS_INFILTRATOR_ARROW_INTERVAL_MILLIS = 8_000L;
    public static final long TURF_WARS_SHREDDER_ARROW_INTERVAL_MILLIS = 4_000L;
    public static final long TURF_WARS_BARRAGE_FIRST_CHARGE_MILLIS = 1_000L;
    public static final long TURF_WARS_BARRAGE_CHARGE_INTERVAL_MILLIS = 250L;
    public static final int TURF_WARS_BARRAGE_MAX_CHARGE = 5;
    public static final int TURF_WARS_STARTING_WOOL = 6;
    public static final int TURF_WARS_BUILD_PHASE_WOOL = 24;
    public static final int TURF_WARS_MARKSMAN_MAX_WOOL = 8;
    public static final int TURF_WARS_INFILTRATOR_MAX_WOOL = 4;
    public static final int TURF_WARS_SHREDDER_MAX_WOOL = 6;
    public static final int TURF_WARS_MARKSMAN_MAX_ARROWS = 2;
    public static final int TURF_WARS_INFILTRATOR_MAX_ARROWS = 1;
    public static final int TURF_WARS_SHREDDER_MAX_ARROWS = 2;
    public static final long TURF_WARS_RESPAWN_DELAY_TICKS = 80L;
    public static final long TURF_WARS_TERRITORY_KNOCKBACK_COOLDOWN_MILLIS = 2_000L;
    public static final long TURF_WARS_SPAWN_DAMAGE_COOLDOWN_MILLIS = 2_000L;
    public static final long TURF_WARS_INFIlTRATOR_SLOW_INTERVAL_MILLIS = 5_000L;
    public static final long TURF_WARS_INFIlTRATOR_KILL_SLOW_INTERVAL_MILLIS = 2_500L;
    public static final double TURF_WARS_SHREDDER_ARROW_DAMAGE = 9.0D;
    public static final double TURF_WARS_MELEE_DAMAGE = 6.0D;
    public static final double TURF_WARS_IRON_SWORD_DAMAGE = 12.0D;
    public static final long TURF_WARS_MIXED_ARCADE_INITIAL_BUILD_PHASE_MILLIS = 20_000L;
    public static final long TURF_WARS_MIXED_ARCADE_BUILD_PHASE_MILLIS = 10_000L;
    public static final long TURF_WARS_MIXED_ARCADE_COMBAT_PHASE_MILLIS = 45_000L;
    public static final int TURF_WARS_MIXED_ARCADE_TURF_ADVANCE_BLOCKS = 2;
    public static final long MICRO_BATTLES_CHEAT_WINDOW_MILLIS = 4_000L;
    public static final long MICRO_BATTLES_BARRIER_DURATION_MILLIS = 10_000L;
    public static final int MICRO_BATTLES_CRUMBLE_BLOCKS_PER_TICK = 3;
    public static final int MICRO_BATTLES_CRUMBLE_SCAN_INTERVAL_TICKS = 2;
    public static final int DRAGONS_MAX_DRAGONS = 5;
    /** UpdateType.SEC_05: Reborn adds one dragon every five seconds. */
    public static final long DRAGONS_SPAWN_INTERVAL_MILLIS = 5_000L;
    /** UpdateType.FASTEST is the source dragon-target/contact update cadence. */
    public static final long DRAGONS_TARGET_INTERVAL_MILLIS = 125L;
    /** PerkSparkler and PerkFletcher replenish on UpdateType.FAST. */
    public static final long DRAGONS_PERK_UPDATE_INTERVAL_MILLIS = 500L;
    /** GameFlagManager checks FASTER; the per-player water Recharge is 500 ms. */
    public static final long DRAGONS_WATER_DAMAGE_INTERVAL_MILLIS = 500L;
    /** PerkBarrage fires queued arrows once per TICK (approximately 20 per second). */
    public static final long DRAGONS_BARRAGE_FIRE_INTERVAL_MILLIS = 50L;
    public static final double DRAGONS_PARTICIPATION_GEMS = 10.0D;
    public static final double DRAGONS_FLARE_RADIUS = 24.0D;
    public static final double DRAGONS_FLARE_RADIUS_SQUARED = DRAGONS_FLARE_RADIUS * DRAGONS_FLARE_RADIUS;
    public static final double DRAGONS_FLARE_VERTICAL_RANGE = 24.0D;
    public static final long DRAGONS_FLARE_DURATION_MILLIS = 6_000L;
    public static final long DRAGONS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS = 3_000L;
    public static final int DRAGONS_MARKSMAN_START_ARROWS = 1;
    public static final long DRAGONS_CONTACT_COOLDOWN_MILLIS = 2_000L;
    public static final long DRAGONS_SPARKLER_INTERVAL_MILLIS = 25_000L;
    public static final int DRAGONS_MAX_SPARKLERS = 2;
    public static final int DRAGONS_SPARKLER_LIFETIME_TICKS = 100;
    public static final double DRAGONS_SPARKLER_RANGE_SQUARED = 48.0D * 48.0D;
    public static final int DRAGONS_MARKSMAN_MAX_ARROWS = 4;
    public static final long DRAGONS_MARKSMAN_ARROW_INTERVAL_MILLIS = 4_000L;
    public static final int DRAGONS_BARRAGE_MAX_ARROWS = 6;
    public static final long DRAGONS_BARRAGE_INTERVAL_MILLIS = 200L;
    public static final long DRAGONS_BARRAGE_INITIAL_CHARGE_MILLIS = 1_000L;
    public static final long DRAGONS_COWARD_LEAP_COOLDOWN_MILLIS = 8_000L;
    public static final long MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS = 20_000L;
    public static final int MICRO_BATTLES_FLETCHER_MAX_ARROWS = 3;

    private ArcadePortRules() {
    }

    public static String normalizeDragonEscapeKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (value.startsWith("dragon-escape-")) value = value.substring("dragon-escape-".length());
        return switch (value) {
            case "jumper" -> "jumper";
            case "disruptor" -> "disruptor";
            case "warper" -> "warper";
            case "digger" -> "digger";
            default -> null;
        };
    }

    /** A Disruptor mine starts its own recharge window when it is thrown. */
    public static long dragonEscapeNextTntChargeAt(long thrownAtMillis) {
        return thrownAtMillis + DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS;
    }

    public static boolean isDragonEscapeDiggerOpeningLocked(long elapsedMillis) {
        return elapsedMillis < DRAGON_ESCAPE_DIGGER_OPENING_LOCKOUT_MILLIS;
    }

    public static boolean shouldAllowDragonEscapeDigger(long nowMillis, long nextUseAtMillis) {
        return nowMillis >= nextUseAtMillis;
    }

    public static boolean shouldPreventDragonEscapeDigger(double distanceToBlockCenter, double distanceAboveBlock) {
        return (Double.isFinite(distanceToBlockCenter)
                && distanceToBlockCenter >= 0.0D
                && distanceToBlockCenter < DRAGON_ESCAPE_DIGGER_NEARBY_PLAYER_RADIUS)
                || (Double.isFinite(distanceAboveBlock)
                && distanceAboveBlock >= 0.0D
                && distanceAboveBlock < DRAGON_ESCAPE_DIGGER_NEARBY_PLAYER_RADIUS);
    }

    public static boolean isDragonEscapeWarpGraceActive(long nowMillis, long warpedAtMillis) {
        return warpedAtMillis >= 0L && nowMillis >= warpedAtMillis
                && nowMillis - warpedAtMillis < DRAGON_ESCAPE_WARPER_GRACE_MILLIS;
    }

    /**
     * Detect a real three-checkpoint skip but do not punish players near a tight
     * authored switchback where the next sequential checkpoint is also reachable.
     */
    public static boolean shouldRejectDragonEscapeShortcut(int previousNode, int observedNode,
            double distanceToNextWaypoint, double distanceToObservedWaypoint, long nowMillis, long warpedAtMillis) {
        if (observedNode - previousNode < 3
                || !Double.isFinite(distanceToNextWaypoint) || distanceToNextWaypoint < 0.0D
                || !Double.isFinite(distanceToObservedWaypoint) || distanceToObservedWaypoint < 0.0D) return false;
        if (distanceToNextWaypoint <= 3.0D || distanceToObservedWaypoint > 3.0D) return false;
        return !isDragonEscapeWarpGraceActive(nowMillis, warpedAtMillis);
    }

    /** The route list starts with the RED origin; Douglas initially flies toward the first BLACK marker. */
    public static int dragonEscapeInitialTargetIndex(int waypointCount) {
        return waypointCount > 1 ? 1 : -1;
    }

    public static double dragonEscapeSpeedMultiplier(double greenMarkerX) {
        return Double.isFinite(greenMarkerX) ? greenMarkerX / 100.0D : 1.0D;
    }

    public static boolean shouldTriggerDragonEscapeDisruptor(
            int itemAgeTicks, double distanceToTarget, boolean sameOwner) {
        return !sameOwner && itemAgeTicks >= DRAGON_ESCAPE_DISRUPTOR_ARM_TICKS
                && Double.isFinite(distanceToTarget) && distanceToTarget >= 0.0D
                && distanceToTarget <= DRAGON_ESCAPE_DISRUPTOR_TRIGGER_RADIUS;
    }

    public static int dragonEscapeLeapUses(String kit) {
        return switch (normalizeDragonEscapeKit(kit) == null ? "" : normalizeDragonEscapeKit(kit)) {
            case "jumper" -> 4;
            case "disruptor" -> 3;
            case "warper" -> 2;
            default -> 0;
        };
    }

    public static final int DRAGON_ESCAPE_DIGGER_MAX_USES = 6;
    public static final long DRAGON_ESCAPE_DIGGER_OPENING_LOCKOUT_MILLIS = 10_000L;
    public static final long DRAGON_ESCAPE_DIGGER_RECHARGE_MILLIS = 100L;
    public static final int DRAGON_ESCAPE_DIGGER_BLOCK_RESTORE_TICKS = 2_400;
    public static final double DRAGON_ESCAPE_DIGGER_NEARBY_PLAYER_RADIUS = 1.5D;
    public static final long DRAGON_ESCAPE_WARPER_GRACE_MILLIS = 1_000L;
    public static final long DRAGON_ESCAPE_WATER_DAMAGE_INTERVAL_MILLIS = 500L;
    public static final int DRAGON_ESCAPE_DISRUPTOR_MAX_TNT = 2;
    public static final long DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS = 8_000L;
    public static final long DRAGON_ESCAPE_LEAP_RECHARGE_MILLIS = 8_000L;
    public static final long DRAGON_ESCAPE_SCORE_UPDATE_MILLIS = 500L;
    public static final int DRAGON_ESCAPE_DISRUPTOR_ARM_TICKS = 40;

    public static boolean shouldAllowDragonEscapeKitChange(boolean activeRaceParticipant) {
        return !activeRaceParticipant;
    }

    public static boolean shouldRunDragonEscapeScoreUpdate(long nowMillis, long nextUpdateAtMillis) {
        return nowMillis >= nextUpdateAtMillis;
    }

    public static long nextDragonEscapeScoreUpdateAt(long nowMillis) {
        return nowMillis + DRAGON_ESCAPE_SCORE_UPDATE_MILLIS;
    }

    /** Reborn applies the 0.2 lift, caps Y, then adds a further 0.2 when grounded. */
    public static double dragonEscapeLeapVerticalVelocity(double normalizedFacingY, boolean grounded) {
        if (!Double.isFinite(normalizedFacingY)) normalizedFacingY = 0.0D;
        double y = Math.min(1.0D, normalizedFacingY + 0.2D);
        if (grounded) y += 0.2D;
        return y;
    }

    /** Initial rank uses the current closest waypoint; later observations never move backwards. */
    public static int dragonEscapeObservedWaypointBaseline(int previousObserved, int observed, boolean initialized) {
        return initialized ? Math.max(previousObserved, observed) : observed;
    }
    public static final double DRAGON_ESCAPE_DISRUPTOR_TRIGGER_RADIUS = 2.0D;
    public static final double DRAGON_ESCAPE_DISRUPTOR_LAUNCH_Y = 0.5D;

    public static String normalizeDragonsKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (kit.startsWith("dragons-")) kit = kit.substring("dragons-".length());
        return switch (kit) {
            case "coward" -> "coward";
            case "marksman", "markman" -> "marksman";
            case "pyrotechnic", "pyro" -> "pyrotechnic";
            default -> null;
        };
    }

    public static int dragonsFlareCharges(String requestedKit) {
        return switch (normalizeDragonsKit(requestedKit) == null ? "" : normalizeDragonsKit(requestedKit)) {
            case "marksman" -> 3;
            case "pyrotechnic" -> 2;
            case "coward" -> 1;
            default -> 0;
        };
    }

    public static boolean isDragonFlareActive(long nowMillis, long expiresAtMillis) {
        return expiresAtMillis > nowMillis;
    }

    public static boolean shouldCancelDragonsPvp(boolean directPlayerDamager, boolean playerProjectileShooter) {
        return directPlayerDamager || playerProjectileShooter;
    }

    public static boolean shouldAllowDragonWorldItem(Material material) {
        return material == Material.EMERALD;
    }

    /** SoloGame awards one survival gem per ten seconds alive. */
    public static double dragonSurvivalGems(long survivedMillis) {
        return Math.max(0L, survivedMillis) / 10_000.0D;
    }

    /** SoloGame end-of-round placement rewards, one-based. */
    public static double dragonPlacementGemReward(int place) {
        return switch (place) {
            case 1 -> 20.0D;
            case 2 -> 15.0D;
            case 3 -> 10.0D;
            default -> 0.0D;
        };
    }

    /** SoloGame sidebar: 0=all alive/dead names, 1=alive names, 2=alive/dead counts. */
    public static int dragonsSoloScoreboardMode(int totalPlayers, int alivePlayers) {
        if (totalPlayers < 15) return 0;
        if (alivePlayers < 16) return 1;
        return 2;
    }

    /** Mirrors UtilAction.velocity(entity, facing, 1.2, false, 0, 0.2, 1.0, true). */
    public static Vector dragonsLeapVelocity(Vector facing, boolean grounded) {
        if (facing == null || !Double.isFinite(facing.getX()) || !Double.isFinite(facing.getY())
                || !Double.isFinite(facing.getZ()) || facing.lengthSquared() <= 1.0E-12D) {
            return new Vector();
        }
        Vector velocity = facing.clone().normalize().multiply(1.2D);
        velocity.setY(velocity.getY() + 0.2D);
        if (velocity.getY() > 1.0D) velocity.setY(1.0D);
        if (grounded) velocity.setY(velocity.getY() + 0.2D);
        return velocity;
    }

    /** Mirrors UtilAction.velocity(entity, trajectory, 1.0, false, 0, 0.6, 2.0, true). */
    public static Vector dragonsContactKnockback(Vector trajectory, boolean grounded) {
        if (trajectory == null || !Double.isFinite(trajectory.getX()) || !Double.isFinite(trajectory.getY())
                || !Double.isFinite(trajectory.getZ()) || trajectory.lengthSquared() <= 1.0E-12D) {
            return new Vector();
        }
        Vector velocity = trajectory.clone().normalize();
        velocity.setY(velocity.getY() + 0.6D);
        if (velocity.getY() > 2.0D) velocity.setY(2.0D);
        if (grounded) velocity.setY(velocity.getY() + 0.2D);
        return velocity;
    }

    public static boolean shouldDragonSparklerAttract(double y, boolean inLiquid) {
        return y >= 4.0D && !inLiquid;
    }

    public static boolean canUseDragonSpectatorTeleport(long nowMillis, long nextAllowedMillis) {
        return nowMillis >= nextAllowedMillis;
    }

    /** Horizontal radius is circular; the vertical limit prevents one flare protecting an entire tower. */
    public static boolean isDragonFlareLocationProtected(
            double horizontalDistanceSquared, double verticalDistance) {
        return Double.isFinite(horizontalDistanceSquared) && horizontalDistanceSquared >= 0.0D
                && horizontalDistanceSquared <= DRAGONS_FLARE_RADIUS_SQUARED
                && Double.isFinite(verticalDistance)
                && Math.abs(verticalDistance) <= DRAGONS_FLARE_VERTICAL_RANGE;
    }

    public static boolean shouldSpawnTestDragon(int currentCount, long now, long nextSpawnAt) {
        return currentCount < DRAGONS_MAX_DRAGONS && now >= nextSpawnAt;
    }

    public static boolean shouldReplenishDragonSparkler(long now, long nextSpawnAt, int currentCount) {
        return now >= nextSpawnAt && currentCount < DRAGONS_MAX_SPARKLERS;
    }

    public static boolean shouldExpireDragonSparkler(int livedTicks, double y, boolean inLiquid) {
        // PerkSparkler removes at >100 ticks. Dragons skips a low/submerged lure for targeting
        // but it remains alive until the perk's lifetime cleanup.
        return livedTicks > DRAGONS_SPARKLER_LIFETIME_TICKS;
    }

    public static double dragonFallDamageAfterReduction(double damage) {
        return Math.max(0.0D, damage - 1.0D);
    }

    public static String normalizeMicroBattlesKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (kit) {
            case "archer" -> "archer";
            case "worker" -> "worker";
            case "fighter" -> "fighter";
            default -> null;
        };
    }

    public static double microBattlesDamageAfterIronSkin(double damage, boolean fighter) {
        if (!fighter || damage <= 1.0D) return damage;
        return Math.max(0.0D, damage * 0.5D);
    }

    public static boolean shouldReplenishMicroArrow(long nowMillis, long nextArrowAt,
            int namedArrows, int maximum) {
        return nowMillis >= nextArrowAt && namedArrows < Math.max(0, maximum);
    }

    /** Map MAX_Y is exclusive; scanning is complete as soon as the next Y reaches it. */
    public static boolean isMicroCrumbleScanComplete(int nextY, int maxYExclusive) {
        return nextY >= maxYExclusive;
    }

    /** The source Micro Battles glass barrier is two blocks high, starting at the island spawn Y. */
    public static int microBattlesBarrierTopY(int islandSpawnY) {
        return islandSpawnY + 1;
    }

    public static boolean shouldRemoveMicroBarrier(long elapsedMillis) {
        return elapsedMillis >= MICRO_BATTLES_BARRIER_DURATION_MILLIS;
    }

    /** Only the kits retained from the selected source ruleset are exposed. */
    public static String normalizeSpleefKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (value) {
            case "snowballer" -> "snowballer";
            case "brawler" -> "brawler";
            case "archer" -> "archer";
            default -> null;
        };
    }

    /** Reborn protects only bedrock and lava; every other non-air block can be punched away. */
    public static boolean isSpleefProtectedMaterial(Material material) {
        return material == null
                || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR
                || material == Material.BEDROCK || material == Material.LAVA;
    }

    /** Apply the source's green/lime -> yellow -> red -> broken material sequence. */
    public static Material nextSpleefSlowDamageMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();
        String family = name.endsWith("_WOOL") ? "_WOOL"
                : name.endsWith("_TERRACOTTA") ? "_TERRACOTTA" : "";
        if (family.isEmpty()) return null;
        String colour = name.substring(0, name.length() - family.length());
        return switch (colour) {
            case "LIME", "GREEN" -> family.equals("_WOOL") ? Material.YELLOW_WOOL : Material.YELLOW_TERRACOTTA;
            case "YELLOW" -> family.equals("_WOOL") ? Material.RED_WOOL : Material.RED_TERRACOTTA;
            default -> null;
        };
    }

    public static int nextSpleefSlowDamageData(int legacyData) {
        return switch (legacyData) {
            case 5, 13 -> 4;
            case 4 -> 14;
            default -> -1;
        };
    }

    /** A deployed test can expose only games that have passed the mechanical-parity acceptance gate. */
    public static boolean isArcadePortGameEnabled(String gameId, Collection<String> enabledGameIds) {
        if (gameId == null || enabledGameIds == null) return false;
        String target = gameId.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (target.isEmpty()) return false;
        for (String candidate : enabledGameIds) {
            if (candidate == null) continue;
            String normalized = candidate.trim().toLowerCase(java.util.Locale.ROOT)
                    .replace('_', '-').replace(' ', '-');
            if (target.equals(normalized)) return true;
        }
        return false;
    }

    public static String normalizeBombLobbersKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (value) {
            case "jumper" -> "jumper";
            case "armorer", "armourer" -> "armorer";
            case "pitcher" -> "pitcher";
            case "waller" -> "waller";
            default -> null;
        };
    }

    /** Jumper and Waller inherit TeamArmorModule's team-coloured leather set. */
    public static boolean bombLobbersKitUsesTeamArmor(String kit) {
        String normalized = normalizeBombLobbersKit(kit);
        return "jumper".equals(normalized) || "waller".equals(normalized);
    }

    /** Reborn's two Bomb Lobbers sides use red and aqua team colours for their trails/gear. */
    public static Color bombLobbersTeamColor(int teamId) {
        return switch (teamId) {
            case 0 -> Color.RED;
            case 1 -> Color.AQUA;
            default -> Color.WHITE;
        };
    }

    /**
     * Legacy updateParticles uses offsets=(1,1,1), speed=0 for Aqua and
     * offsets=(0,0,0), speed=1 for the other team; count remains 2 in both cases.
     */
    public static double bombLobbersTrailOffset(int teamId) {
        return teamId == 1 ? 1.0D : 0.0D;
    }

    public static double bombLobbersTrailSpeed(int teamId) {
        return teamId == 1 ? 0.0D : 1.0D;
    }

    /** Number of 0.2-block trail samples including the previous position. */
    public static int bombLobbersTrailSampleCount(double segmentLength) {
        if (!Double.isFinite(segmentLength) || segmentLength < 0.0D) return 0;
        return (int) Math.floor(segmentLength / BOMB_LOBBERS_TRAIL_SAMPLE_SPACING) + 1;
    }

    /** True for the metal armour supplied directly by the source Armorer/Pitcher kits.
     * Jumper/Waller instead inherit the separate team-coloured leather armour module.
     */
    public static boolean bombLobbersKitUsesArmor(String kit) {
        String normalized = normalizeBombLobbersKit(kit);
        return "armorer".equals(normalized) || "pitcher".equals(normalized);
    }

    /** Reborn's default DamagePvP flag is enabled for Bomb Lobbers; only live participants may damage another participant. */
    public static boolean shouldProcessBombLobbersPlayerHit(boolean attackerAlive, boolean targetAlive) {
        return attackerAlive && targetAlive;
    }

    /** Bomb Lobbers TNT direct hits are enemy-only; team damage is still possible from the blast. */
    public static boolean areBombLobbersEnemies(int ownerTeam, int targetTeam) {
        return ownerTeam >= 0 && targetTeam >= 0 && ownerTeam != targetTeam;
    }

    /** More players in the match lobby produce more frequent power-up spawns. */
    public static long bombLobbersPowerupSpawnIntervalMillis(int lobbyPlayers) {
        int players = Math.max(1, lobbyPlayers);
        if (players <= 2) return 30_000L;
        if (players <= 4) return 25_000L;
        if (players <= 8) return 20_000L;
        if (players <= 12) return 15_000L;
        return 12_000L;
    }

    /** Keep small matches readable while allowing more simultaneous drops in larger lobbies. */
    public static int bombLobbersPowerupMaxActive(int lobbyPlayers) {
        int players = Math.max(1, lobbyPlayers);
        if (players <= 4) return 1;
        if (players <= 8) return 2;
        if (players <= 12) return 3;
        return 4;
    }

    public static double bombLobbersExplosionRadius(String powerupType) {
        return "super-tnt".equalsIgnoreCase(powerupType)
                ? BOMB_LOBBERS_SUPER_TNT_EXPLOSION_RADIUS : BOMB_LOBBERS_EXPLOSION_RADIUS;
    }

    public static double bombLobbersExplosionMaxDamage(String powerupType) {
        return "super-tnt".equalsIgnoreCase(powerupType)
                ? BOMB_LOBBERS_SUPER_TNT_MAX_DAMAGE : BOMB_LOBBERS_MAX_EXPLOSION_DAMAGE;
    }

    public static double bombLobbersDirectHitDamage(String powerupType) {
        return "super-tnt".equalsIgnoreCase(powerupType) ? 15.0D : 10.0D;
    }

    public static boolean bombLobbersTntIgnites(String powerupType) {
        return "fiery-tnt".equalsIgnoreCase(powerupType);
    }

    /** Returns the opposing Bomb Lobbers side, or -1 when the supplied team is invalid. */
    public static int opposingBombLobbersTeam(int teamId) {
        return teamId == 0 ? 1 : teamId == 1 ? 0 : -1;
    }

    /**
     * Legacy Minecraft armour reduction with zero toughness:
     * effective armour = min(20, max(armour / 5, armour - damage / 2)).
     */
    public static double bombLobbersDamageAfterArmor(double rawDamage, double armorPoints) {
        if (!Double.isFinite(rawDamage) || rawDamage <= 0.0D) return 0.0D;
        double armor = Double.isFinite(armorPoints) ? Math.max(0.0D, Math.min(20.0D, armorPoints)) : 0.0D;
        double effectiveArmor = Math.min(20.0D, Math.max(armor / 5.0D, armor - rawDamage / 2.0D));
        return rawDamage * (1.0D - effectiveArmor / 25.0D);
    }

    public static double bombLobbersPitchVelocity(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 1.75D;
            case 3 -> 2.25D;
            default -> 2.0D;
        };
    }

    /**
     * Reborn keeps a launched bomb tracked while its thrower remains an online participant,
     * even after elimination; the source checks IsPlaying rather than IsAlive here.
     */
    public static boolean shouldKeepBombLobbersBombTracked(boolean throwerStillPlaying) {
        return throwerStillPlaying;
    }

    /** Reborn throws on every live PlayerInteractEvent except a physical click. */
    public static boolean shouldThrowBombLobbersOnAction(boolean physicalAction) {
        return !physicalAction;
    }

    /** Legacy PerkDoubleJump is recharged by Recharge.useForce(player, perkName, 50). */
    public static final long BOMB_LOBBERS_DOUBLE_JUMP_RECHARGE_MILLIS = 50L;

    public static long nextBombLobbersDoubleJumpReadyAt(long nowMillis) {
        if (nowMillis < 0L) return BOMB_LOBBERS_DOUBLE_JUMP_RECHARGE_MILLIS;
        if (nowMillis > Long.MAX_VALUE - BOMB_LOBBERS_DOUBLE_JUMP_RECHARGE_MILLIS) return Long.MAX_VALUE;
        return nowMillis + BOMB_LOBBERS_DOUBLE_JUMP_RECHARGE_MILLIS;
    }

    /** Mirrors UtilAction.velocity for PerkDoubleJump's view vector, Y cap, and optional ground boost. */
    public static org.bukkit.util.Vector bombLobbersDoubleJumpVelocity(
            org.bukkit.util.Vector lookDirection, double power) {
        return bombLobbersDoubleJumpVelocity(lookDirection, power, false);
    }

    public static org.bukkit.util.Vector bombLobbersDoubleJumpVelocity(
            org.bukkit.util.Vector lookDirection, double power, boolean grounded) {
        if (lookDirection == null || !Double.isFinite(power) || power <= 0.0D
                || lookDirection.lengthSquared() <= 1.0E-12D) {
            return new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D);
        }
        org.bukkit.util.Vector velocity = lookDirection.clone();
        velocity.setY(power);
        velocity.normalize().multiply(power);
        if (velocity.getY() > power) velocity.setY(power);
        if (grounded) velocity.setY(velocity.getY() + 0.2D);
        return velocity;
    }

    /** NMS explosion packets add their source-calculated impulse to current player velocity. */
    public static org.bukkit.util.Vector bombLobbersExplosionVelocity(
            org.bukkit.util.Vector currentVelocity, org.bukkit.util.Vector explosionToTarget, double impact) {
        org.bukkit.util.Vector velocity = currentVelocity == null
                ? new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D) : currentVelocity.clone();
        if (explosionToTarget == null || explosionToTarget.lengthSquared() <= 1.0E-12D
                || !Double.isFinite(impact) || impact <= 0.0D) return velocity;
        return velocity.add(explosionToTarget.clone().normalize().multiply(Math.min(1.0D, impact)));
    }

    /** Mirrors UtilAction.velocity for the 2-D direction used by a Bomb Lobbers direct hit. */
    public static org.bukkit.util.Vector bombLobbersDirectHitVelocity(
            org.bukkit.util.Vector targetTrajectory, boolean grounded) {
        if (targetTrajectory == null || targetTrajectory.lengthSquared() <= 1.0E-12D) {
            return new org.bukkit.util.Vector(0.0D, 0.0D, 0.0D);
        }
        org.bukkit.util.Vector velocity = targetTrajectory.clone();
        velocity.normalize().multiply(0.2D);
        velocity.setY(velocity.getY() + 0.2D);
        if (velocity.getY() > 0.4D) velocity.setY(0.4D);
        if (grounded) velocity.setY(velocity.getY() + 0.2D);
        return velocity;
    }

    /** Vanilla 1.8 World#getBlockDensity uses a sample step based on each bounding-box dimension. */
    public static double bombLobbersExposureSampleStep(double dimension) {
        if (!Double.isFinite(dimension) || dimension < 0.0D) return Double.NaN;
        return 1.0D / (dimension * 2.0D + 1.0D);
    }

    /** Vanilla offsets the X/Z sample grid to centre it across the unit interval. */
    public static double bombLobbersExposureSampleOffset(double step) {
        if (!Double.isFinite(step) || step <= 0.0D) return 0.0D;
        return (1.0D - Math.floor(1.0D / step) * step) / 2.0D;
    }

    /** Source Recharge has no entry before first use, so first Craftman grant is immediately ready. */
    public static long bombLobbersFirstCraftReadyAt(long nowMillis) {
        return Math.max(0L, nowMillis);
    }

    public static long bombLobbersCraftmanIntervalMillis(long liveMillis) {
        if (liveMillis <= 20_000L) return 8_000L;
        if (liveMillis <= 40_000L) return 7_000L;
        if (liveMillis <= 60_000L) return 6_000L;
        if (liveMillis <= 620_000L) return 5_000L;
        return 4_000L;
    }

    public static boolean shouldPrimeBombLobbersBomb(long ageMillis, boolean touchingSolidBlock) {
        return ageMillis >= BOMB_LOBBERS_MAX_FLIGHT_MILLIS
                || (ageMillis >= BOMB_LOBBERS_WALL_CONTACT_DELAY_MILLIS && touchingSolidBlock);
    }

    /** Whether a Waller can place a wall at this time, including the source's short Recharge gate. */
    public static boolean isBombLobbersWallerReady(long nowMillis, long readyAtMillis) {
        return nowMillis >= 0L && readyAtMillis >= 0L && nowMillis >= readyAtMillis;
    }

    public static long nextBombLobbersWallerReadyAt(long nowMillis) {
        if (nowMillis < 0L) return BOMB_LOBBERS_WALLER_COOLDOWN_MILLIS;
        if (nowMillis > Long.MAX_VALUE - BOMB_LOBBERS_WALLER_COOLDOWN_MILLIS) return Long.MAX_VALUE;
        return nowMillis + BOMB_LOBBERS_WALLER_COOLDOWN_MILLIS;
    }

    /** Reborn's Waller faces the adjacent block using getFace(playerYaw + 180F). */
    public static int bombLobbersWallFacingIndex(float playerYaw) {
        if (!Float.isFinite(playerYaw)) return 2;
        return Math.round((playerYaw + 180.0F) / 90.0F) & 0x3;
    }

    /**
     * Spigot 1.8.8 casts vanilla explosion damage to int using
     * ((impact^2 + impact) / 2) * (8 * radius) + 1. Mineplex's EXPLOSION
     * condition cancels the original value and replaces it with min(value, 50).
     * Even zero exposure yields the vanilla minimum of 1 for an entity inside or on the radius.
     */
    public static double bombLobbersBlastDamage(double distanceToTarget, double exposure) {
        return bombLobbersBlastDamage(distanceToTarget, exposure,
                BOMB_LOBBERS_EXPLOSION_RADIUS, BOMB_LOBBERS_MAX_EXPLOSION_DAMAGE);
    }

    public static double bombLobbersBlastDamage(double distanceToTarget, double exposure,
            double radius, double maxDamage) {
        if (!Double.isFinite(distanceToTarget) || distanceToTarget < 0.0D
                || !Double.isFinite(radius) || radius <= 0.0D || distanceToTarget > radius
                || !Double.isFinite(maxDamage) || maxDamage <= 0.0D) return 0.0D;
        double visible = Double.isFinite(exposure) ? Math.max(0.0D, Math.min(1.0D, exposure)) : 0.0D;
        double impact = (1.0D - distanceToTarget / radius) * visible;
        double vanillaDamage = ((impact * impact + impact) / 2.0D) * (8.0D * radius) + 1.0D;
        int legacyIntegerDamage = (int) vanillaDamage;
        return Math.min(legacyIntegerDamage, maxDamage);
    }

    public static Material bombLobbersBlockAfterExplosion(Material material) {
        if (material == null) return Material.AIR;
        return switch (material) {
            case STONE -> Material.COBBLESTONE;
            case STONE_BRICKS, MOSSY_STONE_BRICKS, CHISELED_STONE_BRICKS -> Material.CRACKED_STONE_BRICKS;
            default -> Material.AIR;
        };
    }

    public static boolean shouldSuppressBombLobbersSpawnSideExplosion(int throwerTeam, int nearestSpawnTeam) {
        return throwerTeam >= 0 && nearestSpawnTeam >= 0 && throwerTeam == nearestSpawnTeam;
    }

    /** Managed TNT damage is handled once by the custom source-compatible projectile/explosion path. */
    public static boolean shouldSuppressNativeBombLobbersTntDamage(boolean managedBomb) {
        return managedBomb;
    }

    /** The source cancels a TNT detonation if its thrower is unavailable or it is on that team's own side. */
    public static boolean shouldCancelBombLobbersExplosion(
            boolean throwerOnline, int throwerTeam, int explosionSideTeam) {
        return !throwerOnline
                || shouldSuppressBombLobbersSpawnSideExplosion(throwerTeam, explosionSideTeam);
    }

    /** Reborn eliminates a Bomb Lobbers player once they cross into another team's side. */
    public static boolean shouldEliminateBombLobbersForSide(int playerTeam, int nearestSpawnTeam) {
        return playerTeam >= 0 && nearestSpawnTeam >= 0 && playerTeam != nearestSpawnTeam;
    }

    /**
     * Source collision checks entities only up to the first block intersection on the TNT segment.
     * Positive infinity means the ray did not hit a block; invalid entity distances never hit.
     */
    public static boolean shouldResolveBombLobbersDirectHitBeforeBlock(
            double entityHitDistanceSquared, double blockHitDistanceSquared) {
        if (!Double.isFinite(entityHitDistanceSquared) || entityHitDistanceSquared < 0.0D) return false;
        if (blockHitDistanceSquared == Double.POSITIVE_INFINITY) return true;
        return Double.isFinite(blockHitDistanceSquared) && blockHitDistanceSquared >= 0.0D
                && entityHitDistanceSquared <= blockHitDistanceSquared;
    }

    /** Mirrors the projectile wrapper returning after its first direct-hit callback. */
    public static boolean shouldCheckBombLobbersDirectHit(boolean alreadyHitThisFlight) {
        return !alreadyHitThisFlight;
    }

    /** Reborn ray-tests the TNT path against the target bounding box expanded by one block. */
    public static final double BOMB_LOBBERS_DIRECT_HIT_BOX_GROW = 1.0D;

    /**
     * Returns squared distance from the TNT segment start to its first intersection with the
     * source-expanded target box, or positive infinity if that flight segment misses.
     */
    public static double bombLobbersDirectHitDistanceSquared(
            org.bukkit.Location start, org.bukkit.Location end, org.bukkit.util.BoundingBox targetBox) {
        if (start == null || end == null || targetBox == null || start.getWorld() != end.getWorld()) {
            return Double.POSITIVE_INFINITY;
        }
        org.bukkit.util.Vector startVector = start.toVector();
        org.bukkit.util.BoundingBox hitBox = targetBox.clone().expand(
                BOMB_LOBBERS_DIRECT_HIT_BOX_GROW,
                BOMB_LOBBERS_DIRECT_HIT_BOX_GROW,
                BOMB_LOBBERS_DIRECT_HIT_BOX_GROW);
        org.bukkit.util.Vector segment = end.toVector().subtract(startVector);
        double length = segment.length();
        if (!Double.isFinite(length)) return Double.POSITIVE_INFINITY;
        if (length <= 1.0E-9D) {
            return hitBox.contains(startVector) ? 0.0D : Double.POSITIVE_INFINITY;
        }
        org.bukkit.util.RayTraceResult hit = hitBox.rayTrace(startVector,
                segment.multiply(1.0D / length), length);
        return hit == null ? Double.POSITIVE_INFINITY
                : startVector.distanceSquared(hit.getHitPosition());
    }

    /** Pig's source passive is +250% knockback, i.e. 3.5 times the normal amount. */
    public static final double BACON_PIG_BACKSTAB_KNOCKBACK_MULTIPLIER = 2.5D;
    /** PerkBodySlam applies its 2.0 knockback value as a multiplier to both damage events. */
    public static double baconBrawlKnockbackMultiplier(boolean backstab) {
        return backstab ? BACON_PIG_BACKSTAB_KNOCKBACK_MULTIPLIER : 1.0D;
    }

    /** DamageManager uses log10(max(damage, 2)) as the base, then multiplies every knockback modifier. */
    public static double baconBrawlEffectiveKnockback(double damage, double knockbackMultipliers) {
        double safeDamage = Double.isFinite(damage) ? Math.max(2.0D, damage) : 2.0D;
        double safeMultiplier = Double.isFinite(knockbackMultipliers)
                ? Math.max(0.0D, knockbackMultipliers) : 1.0D;
        return Math.log10(safeDamage) * safeMultiplier;
    }

    /** Reproduce DamageManager's velocity strength before UtilAction's ground boost. */
    public static double baconBrawlVelocityStrength(double damage, double knockbackMultipliers,
            boolean projectile) {
        double knockback = baconBrawlEffectiveKnockback(damage, knockbackMultipliers);
        double trajectoryLength = projectile
                ? Math.sqrt(Math.pow(0.37D * knockback, 2.0D) + 0.06D * 0.06D)
                : 0.6D * knockback;
        return 0.2D + trajectoryLength * 0.8D;
    }

    /** DamageManager adds 0.2 * knockback vertically, caps it, then adds 0.2 when grounded. */
    public static double baconBrawlVerticalVelocity(double damage, double knockbackMultipliers,
            boolean projectile, boolean grounded) {
        double knockback = baconBrawlEffectiveKnockback(damage, knockbackMultipliers);
        double trajectoryLength = projectile
                ? Math.sqrt(Math.pow(0.37D * knockback, 2.0D) + 0.06D * 0.06D)
                : 1.0D;
        double directionY = projectile && trajectoryLength > 0.0D
                ? (0.06D / trajectoryLength) * baconBrawlVelocityStrength(damage, knockbackMultipliers, true)
                : 0.0D;
        double vertical = Math.min(directionY + Math.abs(0.2D * knockback),
                0.4D + 0.04D * knockback);
        return vertical + (grounded ? 0.2D : 0.0D);
    }

    /** Match UtilAction.velocity for Body Slam's take-off: 1.2 horizontal speed, capped Y, ground boost. */
    public static double baconBrawlBodySlamLaunchY(double lookDirectionY, boolean grounded) {
        double directionY = Double.isFinite(lookDirectionY) ? lookDirectionY : 0.0D;
        return Math.min(0.8D, directionY * 1.2D + 0.2D) + (grounded ? 0.2D : 0.0D);
    }

    public static boolean isBaconBrawlBackstab(double targetLookX, double targetLookZ,
            double attackerFromTargetX, double attackerFromTargetZ) {
        double lookLength = Math.hypot(targetLookX, targetLookZ);
        double attackerLength = Math.hypot(attackerFromTargetX, attackerFromTargetZ);
        if (!Double.isFinite(lookLength) || !Double.isFinite(attackerLength)
                || lookLength < 1.0E-6D || attackerLength < 1.0E-6D) return false;
        double backX = -targetLookX / lookLength;
        double backZ = -targetLookZ / lookLength;
        double fromX = attackerFromTargetX / attackerLength;
        double fromZ = attackerFromTargetZ / attackerLength;
        return Math.hypot(backX - fromX, backZ - fromZ) < 0.8D;
    }

    /** Brutus considers any attack from the rear half-plane a guard-breaking backstab. */
    public static boolean isBaconBrutusBackstab(double targetLookX, double targetLookZ,
            double attackerFromTargetX, double attackerFromTargetZ) {
        double lookLength = Math.hypot(targetLookX, targetLookZ);
        double attackerLength = Math.hypot(attackerFromTargetX, attackerFromTargetZ);
        if (!Double.isFinite(lookLength) || !Double.isFinite(attackerLength)
                || lookLength < 1.0E-6D || attackerLength < 1.0E-6D) return false;
        double dot = (targetLookX / lookLength) * (attackerFromTargetX / attackerLength)
                + (targetLookZ / lookLength) * (attackerFromTargetZ / attackerLength);
        return dot < -0.25D;
    }

    /** Canonical kit IDs mirror the official names; old test aliases remain accepted. */
    public static String normalizeBaconBrawlKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (value) {
            case "el-muchacho-pigo", "muchacho", "body-slam", "pigo" -> "el-muchacho-pigo";
            case "mama", "mama-pig", "mama-piggles" -> "mama-piggles";
            case "pig", "sneaky-pig", "sheep", "sheep-pig", "pig-cloak", "cloak-pig" -> "pig";
            case "brutus", "brutus-bacon", "bacon-brutus", "brutus-pig" -> "brutus-bacon";
            default -> null;
        };
    }

    /**
     * Modern test controls deliberately accept an axe right-click on any target.
     * Bukkit reports air/block clicks as PlayerInteractEvent and entity clicks as
     * PlayerInteractEntityEvent; callers route both into this same ability gate.
     * Keep the clickedUsableBlock parameter for call-site compatibility, but do not
     * let looking at a chest, player or other surface suppress the ability.
     */
    public static boolean shouldActivateBaconBrawlAbility(boolean rightClick, org.bukkit.Material held,
            boolean clickedUsableBlock, String kit) {
        String normalized = normalizeBaconBrawlKit(kit);
        return rightClick && held == org.bukkit.Material.IRON_AXE && normalized != null;
    }

    /** The retained Sheep Pig kit is the one source kit whose disguise is a pink sheep. */
    public static boolean baconBrawlUsesSheepAvatar(String requestedKit) {
        return "pig".equals(normalizeBaconBrawlKit(requestedKit));
    }

    public static double initialBaconKnockback(int playerCount) {
        return playerCount == 2 ? 1.8D : 1.5D;
    }

    public static double nextBaconKnockback(double current) {
        return current * 1.2D;
    }

    public static boolean shouldDropRunnerBlock(long elapsedMillis) {
        return elapsedMillis >= RUNNER_BLOCK_DECAY_MILLIS;
    }

    public static String normalizeRunnerKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (kit.startsWith("runnerkit-")) kit = kit.substring("runnerkit-".length());
        else if (kit.startsWith("runner-")) kit = kit.substring("runner-".length());
        return switch (kit) {
            case "leaper" -> "leaper";
            case "archer" -> "archer";
            case "frosty" -> "frosty";
            default -> null;
        };
    }

    /** Reborn only destabilises exposed, non-liquid solid blocks once per trail cycle. */
    public static boolean shouldMarkRunnerBlock(boolean solid, boolean bedrock, boolean liquid,
            boolean aboveIsAir, boolean alreadyMarked) {
        return solid && !bedrock && !liquid && aboveIsAir && !alreadyMarked;
    }

    /** Falling blocks only hurt a player when the player is not above the block. */
    public static boolean shouldRunnerFallingBlockHit(double playerY, double fallingBlockY) {
        return playerY <= fallingBlockY + 0.5D;
    }

    public static int spleefProjectileClearRadius(boolean arrow, double velocity) {
        if (!arrow) return 1;
        double safeVelocity = Math.max(0.0D, velocity);
        return Math.max(1, Math.min(3, (int) Math.ceil(0.5D + safeVelocity / 1.6D)));
    }

    public static final long EVOLUTION_RESPAWN_TICKS = 80L;
    public static final long EVOLUTION_FORM_CHANGE_TICKS = 44L;
    public static final long EVOLUTION_TOTAL_ANIMATION_TICKS = 140L;

    public static int nextEvolutionLevel(int currentLevel, boolean wonEvolve) {
        int safeLevel = Math.max(0, Math.min(EVOLUTION_WIN_LEVEL, currentLevel));
        return wonEvolve ? Math.min(EVOLUTION_WIN_LEVEL, safeLevel + 1) : safeLevel;
    }

    public static double evolutionMaxHealth(int level) {
        return switch (Math.max(0, Math.min(5, level))) {
            case 0 -> 40.0D; case 1 -> 36.0D; case 2 -> 32.0D;
            case 3 -> 28.0D; case 4 -> 22.0D; default -> 16.0D;
        };
    }

    public static double evolutionMeleeDamage(int level) {
        return switch (Math.max(0, Math.min(5, level))) {
            case 0 -> 6.0D; case 1, 2 -> 5.0D; case 3, 4 -> 4.0D; default -> 3.0D;
        };
    }

    public static double evolutionChargePerTick(int killsThisLife) {
        int extraKills = Math.max(0, killsThisLife - 1);
        return Math.min(0.022D, 0.012D + 0.003D * extraKills);
    }

    public static boolean isEvolutionWinnerLevel(int level) {
        return level >= EVOLUTION_WIN_LEVEL;
    }

    public static int sheepQuestLeadingTeam(int redSheep, int blueSheep) {
        if (redSheep > blueSheep) return 0;
        if (blueSheep > redSheep) return 1;
        return -1;
    }

    public static int sheepQuestLeadingTeam(int[] sheepCounts) {
        if (sheepCounts == null || sheepCounts.length == 0) return -1;
        int bestTeam = -1;
        int bestCount = Integer.MIN_VALUE;
        boolean tied = false;
        for (int team = 0; team < sheepCounts.length; team++) {
            if (sheepCounts[team] > bestCount) {
                bestCount = sheepCounts[team];
                bestTeam = team;
                tied = false;
            } else if (sheepCounts[team] == bestCount) {
                tied = true;
            }
        }
        return tied ? -1 : bestTeam;
    }

    /** Match the team-index order used by SourceMapCatalog: red, blue, green, yellow. */
    public static String sheepQuestTeamPenMarker(int team) {
        return switch (team) {
            case 0 -> "RED";
            case 1 -> "BLUE";
            case 2 -> "GREEN";
            case 3 -> "YELLOW";
            default -> "UNASSIGNED";
        };
    }

    /** WorldConfig pen markers describe the floor; the sheep's feet block is one block above it. */
    public static boolean sheepQuestInsidePenMarker(int sheepBlockX, int sheepFeetY, int sheepBlockZ,
            int markerX, int markerFloorY, int markerZ) {
        return sheepBlockX == markerX && sheepFeetY == markerFloorY + 1 && sheepBlockZ == markerZ;
    }

    public static final double SHEEP_QUEST_PLAYER_HIT_DAMAGE = 4.0D;
    public static final int SHEEP_QUEST_INITIAL_SHEEP = 4;
    public static final long SHEEP_QUEST_ROUND_MILLIS = 300_000L;
    public static final int SHEEP_QUEST_MAX_CARRY = 3;
    public static final long SHEEP_QUEST_SPAWN_INTERVAL_MILLIS = 20_000L;

    public static boolean canCarryAnotherSheep(int carriedCount) {
        return carriedCount >= 0 && carriedCount < SHEEP_QUEST_MAX_CARRY;
    }

    /** Source Sheep Quest slows carriers progressively, up to the three-sheep limit. */
    public static int sheepQuestCarrySlownessAmplifier(int carriedCount) {
        if (carriedCount <= 0) return -1;
        return Math.min(SHEEP_QUEST_MAX_CARRY - 1, carriedCount - 1);
    }


    public static final long PAINTBALL_WATER_BOMB_COOLDOWN_MILLIS = 250L;
    public static final double PAINTBALL_SAVE_ME_RADIUS = 3.0D;

    /** Reborn's Machine Gun fires on an 80 ms gate; this slower heat gain allows 48 shots per full bar. */
    public static final int PAINTBALL_MACHINE_GUN_SHOTS_PER_HEAT_CYCLE = 48;
    public static final double PAINTBALL_MACHINE_GUN_MAX_HEAT = 0.999D;
    public static final long PAINTBALL_MACHINE_GUN_FIRE_COOLDOWN_MILLIS = 80L;
    public static final long PAINTBALL_MACHINE_GUN_COOL_RECHARGE_MILLIS = 250L;
    public static final double PAINTBALL_MACHINE_GUN_COOL_PER_TICK = 0.020D;

    public static double paintballMachineGunHeatPerShot() {
        return PAINTBALL_MACHINE_GUN_MAX_HEAT / PAINTBALL_MACHINE_GUN_SHOTS_PER_HEAT_CYCLE;
    }

    public static boolean canFirePaintballMachineGun(double currentHeat) {
        if (!Double.isFinite(currentHeat)) return false;
        double heat = Math.max(0.0D, Math.min(PAINTBALL_MACHINE_GUN_MAX_HEAT, currentHeat));
        return heat < PAINTBALL_MACHINE_GUN_MAX_HEAT - paintballMachineGunHeatPerShot() * 0.5D;
    }

    public static double paintballMachineGunHeatAfterShot(double currentHeat) {
        double heat = Double.isFinite(currentHeat)
                ? Math.max(0.0D, currentHeat) : PAINTBALL_MACHINE_GUN_MAX_HEAT;
        return Math.min(PAINTBALL_MACHINE_GUN_MAX_HEAT, heat + paintballMachineGunHeatPerShot());
    }

    public static double coolPaintballMachineGunHeat(double currentHeat) {
        double heat = Double.isFinite(currentHeat) ? Math.max(0.0D, currentHeat) : 0.0D;
        return Math.max(0.0D, heat - PAINTBALL_MACHINE_GUN_COOL_PER_TICK);
    }

    /** The selected held-item model changes for everyone; projectile physics are not affected. */
    public static Material paintballWeaponMaterial(String kit, String skin) {
        String safeKit = kit == null ? "rifle" : kit.toLowerCase(java.util.Locale.ROOT);
        String safeSkin = skin == null ? "chrome-marker" : skin.toLowerCase(java.util.Locale.ROOT);
        if ("splatter-cannon".equals(safeSkin)) return Material.CROSSBOW;
        if ("neon-suppressor".equals(safeSkin)) return Material.BLAZE_ROD;
        return switch (safeKit) {
            case "sniper" -> Material.STONE_HOE;
            case "shotgun" -> Material.GOLDEN_HORSE_ARMOR;
            case "machine-gun" -> Material.DIAMOND_HORSE_ARMOR;
            default -> Material.IRON_HORSE_ARMOR;
        };
    }

    public static boolean shouldPaintballWaterBombAffectPlayer(double distanceSquared, boolean sameTeam) {
        return sameTeam && distanceSquared >= 0.0D
                && distanceSquared <= PAINTBALL_SAVE_ME_RADIUS * PAINTBALL_SAVE_ME_RADIUS;
    }

    public static int paintballPiecesAfterHit(int remainingPieces, int paintAmount) {
        return Math.max(0, Math.max(0, remainingPieces) - Math.max(0, paintAmount));
    }

    public static boolean isPaintballPlayerOut(int remainingPieces) {
        return remainingPieces <= 0;
    }

    public static final int PAINTBALL_WINNER_SCORE = 10;

    public static boolean isPaintballWinnerScoreReached(int score) {
        return score >= PAINTBALL_WINNER_SCORE;
    }

    public static double paintballHealthAfterHit(int remainingPieces) {
        return Math.max(2.0D, Math.max(0, remainingPieces) * 5.0D + 1.0D);
    }

    /** Mirrors Reborn's per-kit armour-paint counts and the Sniper scope damage gauge. */
    public static int paintballPaintAmount(String kit, boolean arrow, double scopeCharge) {
        String safeKit = kit == null ? "rifle" : kit.toLowerCase(java.util.Locale.ROOT);
        if ("rifle".equals(safeKit)) return 3;
        if ("sniper".equals(safeKit) && arrow) return paintballSniperPaintDamage(scopeCharge);
        return 1;
    }

    public static int paintballSniperPaintDamage(double scopeCharge) {
        double charge = Math.max(0.0D, Math.min(0.999D, scopeCharge));
        return 1 + (int) Math.floor(charge * 10.0D / 3.0D);
    }



    public static final long GLADIATOR_ROUND_COUNTDOWN_MILLIS = 5_000L;
    public static final long GLADIATOR_POISON_DELAY_MILLIS = 60_000L;
    public static final long GLADIATOR_POISON_INTERVAL_MILLIS = 1_000L;
    public static final double GLADIATOR_POISON_DAMAGE = 1.0D;

    public static boolean canGladiatorsFight(long nowMillis, long fightStartsAt,
            int attackerPair, int targetPair, boolean pairResolved) {
        return nowMillis >= fightStartsAt && attackerPair >= 0
                && attackerPair == targetPair && !pairResolved;
    }

    public static boolean shouldGladiatorPoison(long nowMillis, long roundStartedAt,
            boolean pairResolved, int playersStillFighting) {
        return nowMillis >= roundStartedAt + GLADIATOR_POISON_DELAY_MILLIS
                && !pairResolved && playersStillFighting >= 2;
    }

    public static int gladiatorsTierForRound(int round) {
        return Math.max(0, Math.min(3, round));
    }

    public static int gladiatorsPairCount(int players) {
        return Math.max(0, (Math.max(0, players) + 1) / 2);
    }

    public static boolean allGladiatorPairsResolved(int pairCount, int resolvedPairs) {
        return pairCount > 0 && resolvedPairs >= pairCount;
    }

    /** Reborn shortens each interval by five seconds; any two-survivor state caps it at 20 seconds. */
    public static final int BACON_BRAWL_MAX_KNOCKBACK_ESCALATIONS =
            (int) (BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS / BACON_KNOCKBACK_INTERVAL_STEP_MILLIS);
    public static final long BACON_BRAWL_ENDGAME_ESCALATION_DELAY_MILLIS = 20_000L;

    public static long nextBaconKnockbackInterval(int completedEscalations, int alivePlayers) {
        if (completedEscalations < 0 || completedEscalations >= BACON_BRAWL_MAX_KNOCKBACK_ESCALATIONS) {
            return 0L;
        }
        long sourceInterval = Math.max(BACON_MIN_KNOCKBACK_INTERVAL_MILLIS,
                BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS
                        - completedEscalations * BACON_KNOCKBACK_INTERVAL_STEP_MILLIS);
        if (alivePlayers <= 2) {
            return Math.min(sourceInterval, BACON_BRAWL_ENDGAME_ESCALATION_DELAY_MILLIS);
        }
        return sourceInterval;
    }

    public static boolean shouldFinishBaconBrawl(int alivePlayers) {
        return alivePlayers <= 1;
    }

    /** Test a player against a world border whose centre may not be at world origin. */
    public static boolean isOutsideWorldBorder(double playerX, double playerZ,
            double centerX, double centerZ, double borderSize) {
        if (!Double.isFinite(playerX) || !Double.isFinite(playerZ)
                || !Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || !Double.isFinite(borderSize) || borderSize <= 0.0D) return true;
        double halfSize = borderSize / 2.0D;
        return Math.abs(playerX - centerX) > halfSize || Math.abs(playerZ - centerZ) > halfSize;
    }

    public static boolean shouldPunishMicroIslandExit(long elapsedMillis, double ownDistanceSquared,
            double nearestOtherDistanceSquared) {
        return elapsedMillis >= 0L && elapsedMillis < MICRO_BATTLES_CHEAT_WINDOW_MILLIS
                && nearestOtherDistanceSquared < ownDistanceSquared;
    }

    public static double microBattlesHitDamage(boolean projectile) {
        return projectile ? 4.0D : 6.0D;
    }

    public static int microBattlesMinimumFoodLevel(int foodLevel) {
        return Math.max(2, Math.min(20, foodLevel));
    }

    public static boolean isWorldDragonHazard(ArcadePortGame game, boolean trackedEntity, boolean playerOwned) {
        return game == ArcadePortGame.DRAGONS && trackedEntity && !playerOwned;
    }

    public static boolean shouldApplyDragonContactHit(long nowMillis, long nextAllowedMillis) {
        return nowMillis >= nextAllowedMillis;
    }

 
    /** Legacy Dragon Escape runs its Hell course at night; other courses keep the normal day start. */
    /**
     * Preserve the original per-game world-water damage settings. Dragon Escape
     * only enables this penalty on its Pirate course.
     */
    public static int sourceWorldWaterDamage(ArcadePortGame game, String mapName) {
        if (game == null) return 0;
        return switch (game) {
            case BOMB_LOBBERS -> 5;
            case DRAGONS, RUNNER, SPLEEF -> 4;
            case DRAGON_ESCAPE -> mapName != null
                    && mapName.toLowerCase(java.util.Locale.ROOT).contains("pirate") ? 2 : 0;
            default -> 0;
        };
    }

    /** Map/game-specific time overrides; all unconfigured source games inherit Game.WorldTimeSet=12000. */
    public static long sourceWorldTime(ArcadePortGame game, String mapName) {
        if (game == null) return 12_000L;
        return switch (game) {
            case BOMB_LOBBERS -> mapName != null && mapName.equalsIgnoreCase("Intergalactic")
                    ? 18_000L : 6_000L;
            case SHEEP_QUEST -> 2_000L;
            case DRAGON_ESCAPE -> mapName != null
                    && mapName.toLowerCase(java.util.Locale.ROOT).contains("hell") ? 16_000L : 12_000L;
            default -> 12_000L;
        };
    }

    public static long dragonEscapeWorldTime(String mapName) {
        return sourceWorldTime(ArcadePortGame.DRAGON_ESCAPE, mapName);
    }

    /** The source game finishes near its final map waypoint, not anywhere on a Z plane. */
    public static boolean isDragonEscapeFinishReached(double x, double y, double z) {
        double dx = x - DRAGON_ESCAPE_FINISH_X;
        double dy = y - DRAGON_ESCAPE_FINISH_Y;
        double dz = z - DRAGON_ESCAPE_FINISH_Z;
        return dx * dx + dy * dy + dz * dz
                < DRAGON_ESCAPE_FINISH_RADIUS * DRAGON_ESCAPE_FINISH_RADIUS;
    }

    /** Reborn kills runners once Douglas's route progress has overtaken theirs. */
    /** Keep route progress monotonic when a course crosses near one of its earlier waypoints. */
    /** For source maps with vertical bounds, let players fall just below the mapped volume before elimination. */
    public static double sourceMapVoidThresholdY(boolean hasVerticalBounds, int mapMinY) {
        return hasVerticalBounds ? Math.max(0.0D, mapMinY - 1.0D) : 58.0D;
    }

    /**
     * Reborn WorldData bounds are inclusive block coordinates: locations at/below
     * MinX/MinZ/MinY or at/above MaxX+1/MaxZ+1/MaxY+1 are outside the map.
     */
    public static boolean isOutsideDragonEscapeMapBounds(
            double x, double y, double z, int minX, int maxX, int minZ, int maxZ,
            boolean hasVerticalBounds, int minY, int maxY) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || x <= minX || x >= maxX + 1.0D
                || z <= minZ || z >= maxZ + 1.0D) return true;
        return hasVerticalBounds && (y <= minY || y >= maxY + 1.0D);
    }

    /** Fall limits below the minimum authored play surface for known low-altitude Dragon Escape maps. */
    public static double dragonEscapeVoidThresholdY(String mapName) {
        String name = mapName == null ? "" : mapName.toLowerCase(java.util.Locale.ROOT);
        if (name.contains("hell")) return 24.0D;
        if (name.contains("pirate")) return 0.0D;
        return 58.0D;
    }

    /** Avoid rechecking the same destruction sphere every server tick. */
    public static boolean isDragonEscapeBlockInsideDestructionRadius(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared <= DRAGON_ESCAPE_BLOCK_RADIUS * DRAGON_ESCAPE_BLOCK_RADIUS;
    }

    public static boolean shouldProcessDragonEscapeDestruction(double distanceSquared, boolean hasPreviousCenter) {
        return !hasPreviousCenter || !Double.isFinite(distanceSquared) || distanceSquared >= 0.25D;
    }

    /** The straight X/Z crumble strip belongs only to the generated fallback course. */
    public static boolean shouldCrumbleDragonEscapeFallbackTrack(boolean hasAuthoredRoute, int playerZ) {
        int crumbleZ = playerZ - 5;
        return !hasAuthoredRoute && crumbleZ > -24 && crumbleZ < 78;
    }

    /** Reborn's rank score: checkpoint index * 10,000 less distance to the next marker. */
    public static double dragonEscapeRankScore(int waypointIndex, double distanceToNextWaypoint) {
        if (waypointIndex < 0 || !Double.isFinite(distanceToNextWaypoint)
                || distanceToNextWaypoint < 0.0D) return Double.NEGATIVE_INFINITY;
        return waypointIndex * 10_000.0D - distanceToNextWaypoint;
    }

    /** Convert a course-distance advantage to an estimated time gap for the live race display. */
    public static long dragonEscapeCourseTimeGapMillis(
            double leaderCourseDistance, double racerCourseDistance, double estimatedSpeed) {
        if (!Double.isFinite(leaderCourseDistance) || !Double.isFinite(racerCourseDistance)
                || !Double.isFinite(estimatedSpeed) || estimatedSpeed <= 0.0D) return 0L;
        double distanceGap = Math.max(0.0D, leaderCourseDistance - racerCourseDistance);
        return Math.max(0L, Math.round(distanceGap / estimatedSpeed * 1_000.0D));
    }

    public static int monotonicDragonEscapeProgress(int previous, int candidate, int waypointCount) {
        if (waypointCount <= 0) return -1;
        return Math.min(waypointCount - 1, Math.max(0, Math.max(previous, candidate)));
    }

    /**
     * Dragon Escape routes contain tight switchbacks. Only advance to the next authored
     * waypoint when it is both within reach and closer than the current waypoint; never
     * jump to a later marker merely because it happens to be spatially nearby.
     */
    public static boolean shouldAdvanceDragonEscapeWaypoint(
            double distanceToCurrent, double distanceToNext, double waypointSpacing) {
        if (!Double.isFinite(distanceToCurrent) || !Double.isFinite(distanceToNext)
                || !Double.isFinite(waypointSpacing)
                || distanceToCurrent < 0.0D || distanceToNext < 0.0D || waypointSpacing < 0.0D) return false;
        // Only compare the next marker in authored order. Reborn scores against
        // the nearest checkpoint; this sequential form preserves switchbacks
        // while allowing a runner to pass a marker between 500ms score updates.
        return distanceToNext < distanceToCurrent;
    }

    public static boolean isDuplicateDragonEscapeWaypoint(double distance) {
        return isDuplicateDragonEscapeWaypoint(distance, DRAGON_ESCAPE_DUPLICATE_WAYPOINT_DISTANCE);
    }

    public static boolean isDuplicateDragonEscapeWaypoint(double distance, double threshold) {
        return Double.isFinite(distance) && Double.isFinite(threshold)
                && distance >= 0.0D && threshold >= 0.0D && distance < threshold;
    }

    /**
     * Split zero is the race start at waypoint zero. Split N is the finish after
     * the final authored waypoint, so a runner at progress N-1 is still on segment N-1.
     */
    public static int dragonEscapePbSegmentStartIndex(int waypointIndex, int waypointCount) {
        if (waypointCount <= 0 || waypointIndex < 0) return -1;
        return Math.min(waypointCount - 1, waypointIndex);
    }

    /** Evenly distribute a legacy PB over N route legs, including the final finish leg. */
    public static long dragonEscapeDefaultSplitMillis(long totalMillis, int splitIndex, int waypointCount) {
        if (totalMillis <= 0L || waypointCount <= 0) return 0L;
        int boundedIndex = Math.max(0, Math.min(waypointCount, splitIndex));
        return Math.round(totalMillis * (boundedIndex / (double) waypointCount));
    }

    /** Interpolate a PB timestamp along one route segment, clamping invalid fractions safely. */
    public static long interpolateDragonEscapeSplitMillis(long fromMillis, long toMillis, double fraction) {
        long from = Math.max(0L, fromMillis);
        long to = Math.max(from, toMillis);
        double progress = Double.isFinite(fraction) ? Math.max(0.0D, Math.min(1.0D, fraction)) : 0.0D;
        return from + Math.round((to - from) * progress);
    }

    /** Interpolate one coordinate on a recorded PB ghost segment. */
    public static double interpolateDragonEscapeCoordinate(double from, double to, double fraction) {
        double progress = Double.isFinite(fraction) ? Math.max(0.0D, Math.min(1.0D, fraction)) : 0.0D;
        return from + (to - from) * progress;
    }

    public static boolean shouldDragonEscapePlayerBeCaught(double dragonProgress, double playerProgress) {
        return dragonProgress > playerProgress;
    }

    public static final long QUIVER_RESPAWN_DELAY_TICKS = 20L;
    public static final long QUIVER_GROUND_POUND_COOLDOWN_MILLIS = 10_000L;
    public static final long QUIVER_NINJA_VANISH_MILLIS = 1_200L;
    /** Practical multi-block target-search range for the source's "nearby enemies" chain arrow. */
    public static final double QUIVER_REBOUND_MAX_DISTANCE_SQUARED = 64.0D;
    public static final int QUIVER_MAX_ARROW_BOUNCES = 2;

    public static boolean isQuiverReboundTargetInRange(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared <= QUIVER_REBOUND_MAX_DISTANCE_SQUARED;
    }

    public static boolean shouldQuiverArrowBounce(int remainingBounces) {
        return remainingBounces > 0;
    }

    public static int quiverArrowBouncesAfterBounce(int remainingBounces) {
        return Math.max(0, remainingBounces - 1);
    }

    public static String normalizeQuiverKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (kit.startsWith("quiver-")) kit = kit.substring("quiver-".length());
        return switch (kit) {
            case "leaper" -> "leaper";
            case "brawler" -> "brawler";
            case "enchanter" -> "enchanter";
            case "slam-shot", "slamshot", "slam-shooter" -> "slam-shot";
            case "ninja" -> "ninja";
            default -> null;
        };
    }

    /** One in the Quiver refunds its single arrow only when the hit secures a kill. */
    public static boolean quiverRegrantsArrowAfterHit(boolean targetEliminated) {
        return targetEliminated;
    }

    /** Reborn's Ground Pound scales horizontal knockback from 0 at range to 0.8 at point-blank. */
    public static double quiverGroundPoundHorizontalMultiplier(double distance) {
        if (!Double.isFinite(distance)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, (8.0D - distance) / 8.0D));
    }

    public static double quiverMeleeDamage(String kit) {
        String value = normalizeQuiverKit(kit);
        if (value == null) value = "leaper";
        return switch (value) {
            case "brawler" -> 7.0D;
            case "slam-shot" -> 5.5D;
            case "ninja" -> 4.0D;
            default -> 5.0D;
        };
    }

    /** Leaper flight can be re-enabled only on landing, never during the launch frame. */
    public static boolean shouldRearmQuiverLeap(boolean onGround, boolean awaitingLanding, double verticalVelocity) {
        if (!onGround) return false;
        if (!awaitingLanding) return true;
        return Double.isFinite(verticalVelocity) && verticalVelocity <= 0.01D;
    }

    public static boolean shouldRemoveQuiverInvisibilityOnHit(boolean melee, boolean ninja) {
        return melee && ninja;
    }

    public static boolean isQuiverWinnerScoreReached(int kills) {
        return kills >= 20;
    }

    public static boolean shouldFinishWhenNobodyIsAlive(int alivePlayers, int respawningPlayers) {
        return alivePlayers <= 0 && respawningPlayers <= 0;
    }

    public static int requiredDeathTagChasers(long elapsedMillis) {
        return elapsedMillis < 0L ? 1
                : 1 + (int) (elapsedMillis / DEATH_TAG_CHASER_PROMOTION_INTERVAL_MILLIS);
    }

    public static boolean shouldSpreadDeathTag(boolean attackerTagged, boolean targetTagged) {
        return attackerTagged && !targetTagged;
    }

    public static boolean deathTagHitIsLethal(double currentHealth, double hitDamage) {
        return hitDamage > 0.0D && currentHealth <= hitDamage;
    }

    /** ComeAtMeBro starts awarding one stat per additional Chaser kill after the second kill. */
    public static boolean shouldAwardDeathTagComeAtMeBro(int chaserKillsByPlayer) {
        return chaserKillsByPlayer >= 2;
    }

    public static double deathTagChaserDamage(boolean alphaChaser) {
        return alphaChaser ? 6.0D : 4.0D;
    }

    public static double deathTagRunnerDamage(String runnerKit, boolean projectile) {
        String kit = runnerKit == null ? "basher" : runnerKit.toLowerCase(java.util.Locale.ROOT);
        if (projectile) return "archer".equals(kit) ? 4.0D : 0.0D;
        // Only Basher and Traitor replace the default unarmed hit with weapon damage.
        return "basher".equals(kit) || "traitor".equals(kit) ? 6.0D : 1.0D;
    }

    /** Iron Skin in Reborn subtracts a flat 2 or 4 damage from attacks greater than 1. */
    public static double deathTagDamageAfterIronSkin(double damage, boolean alphaChaser) {
        if (damage <= 1.0D) return damage;
        return Math.max(0.0D, damage - (alphaChaser ? 4.0D : 2.0D));
    }

    /** The legacy Iron Skin perk does not reduce fire-tick or one-point-and-lower damage. */
    public static double deathTagEnvironmentalDamageAfterIronSkin(
            double damage, boolean alphaChaser, boolean fireTick) {
        return fireTick ? damage : deathTagDamageAfterIronSkin(damage, alphaChaser);
    }

    public static double deathTagKnockbackMultiplier(boolean alphaChaser) {
        return alphaChaser ? 0.5D : 1.0D;
    }

    /** Original Death Tag lights switch between gold blocks and white wool each second. */
    public static Material deathTagNextLightMaterial(Material current) {
        // Original 1.8 marker uses wool data value 15, which maps to BLACK_WOOL.
        return current == Material.GOLD_BLOCK ? Material.BLACK_WOOL : Material.GOLD_BLOCK;
    }

    /** Reborn's runner speed threshold is strictly greater than 25% chasers. */
    public static boolean deathTagRunnersReceiveSpeed(int chasers, int totalPlayers) {
        return totalPlayers > 0 && chasers >= 0 && (double) chasers / (double) totalPlayers > 0.25D;
    }

    /** Reborn's speed threshold is a one-way unlock, even if later disconnects lower the ratio. */
    public static boolean deathTagRunnerSpeedUnlocked(boolean alreadyUnlocked, int chasers, int totalPlayers) {
        return alreadyUnlocked || deathTagRunnersReceiveSpeed(chasers, totalPlayers);
    }

    /** Chat stats show kills divided by deaths; with no deaths, the kill count is the K/D display value. */
    public static double deathTagKillDeathRatio(int kills, int deaths) {
        int safeKills = Math.max(0, kills);
        int safeDeaths = Math.max(0, deaths);
        return safeDeaths == 0 ? safeKills : (double) safeKills / (double) safeDeaths;
    }

    /** Chasers retain their opening Speed I only until the runners earn their own Speed I. */
    public static boolean deathTagChasersReceiveSpeed(int chasers, int totalPlayers) {
        return chasers > 0 && !deathTagRunnersReceiveSpeed(chasers, totalPlayers);
    }

    /** Archer arrows have 300% of normal knockback; Alpha Chasers take half knockback. */
    public static double deathTagArcherArrowKnockbackMultiplier(boolean alphaChaser) {
        return DEATH_TAG_ARCHER_ARROW_KNOCKBACK_POWER * deathTagKnockbackMultiplier(alphaChaser);
    }

    /** Reborn's Traitor knockback perk has a per-target 400 ms recharge. */
    public static boolean deathTagTraitorKnockbackReady(long nowMillis, long lastUseMillis) {
        return lastUseMillis <= 0L || nowMillis - lastUseMillis >= DEATH_TAG_TRAITOR_KNOCKBACK_COOLDOWN_MILLIS;
    }

    /**
     * Select the authored Chaser respawn spawn with the greatest distance to its nearest
     * active player, matching the legacy team-spawn preference to stay away from players.
     * Positive infinity is valid when the arena currently has no active players.
     */
    public static int deathTagSafestRespawnSpawnIndex(java.util.List<Double> nearestPlayerDistances) {
        if (nearestPlayerDistances == null || nearestPlayerDistances.isEmpty()) return -1;
        int bestIndex = -1;
        double bestClearance = -1.0D;
        for (int i = 0; i < nearestPlayerDistances.size(); i++) {
            Double clearance = nearestPlayerDistances.get(i);
            if (clearance == null || Double.isNaN(clearance) || clearance < 0.0D) continue;
            if (bestIndex < 0 || clearance > bestClearance) {
                bestIndex = i;
                bestClearance = clearance;
            }
        }
        return bestIndex;
    }

    /** Source DeathTag.GetKillsGems pays for a Runner kill, never for a Chaser kill. */
    public static int deathTagKillRewardGems(boolean killedRunner) {
        return killedRunner ? DEATH_TAG_RUNNER_KILL_REWARD_GEMS : 0;
    }

    /** The source Death Tag game only awards winning placement to surviving Runners. */
    public static boolean deathTagHasRunnerWinner(int runnersRemaining) {
        return runnersRemaining > 0;
    }

    public static boolean shouldDeathTagRunnerWinTimeout(int runnersRemaining) {
        return deathTagHasRunnerWinner(runnersRemaining);
    }

    /** The legacy DeathTag EndCheck finishes at one or fewer live Runners for every lobby size. */
    public static boolean shouldFinishDeathTagRound(int runnersRemaining, int initialPlayers) {
        return runnersRemaining <= 1;
    }

    /** Generated island-ring removal must never run against an archived source map. */
    public static boolean shouldRemoveGeneratedMicroBarrier(boolean sourceMapLoaded, int trackedBarrierCount) {
        return !sourceMapLoaded && trackedBarrierCount <= 0;
    }

    /** Clamp flying dragon targets inside the authored X/Z map bounds with a one-block safety margin. */
    public static double clampDragonSkyTargetCoordinate(double coordinate, int minInclusive, int maxExclusive) {
        if (!Double.isFinite(coordinate) || maxExclusive <= minInclusive) {
            return (minInclusive + (double) maxExclusive) / 2.0D;
        }
        double low = minInclusive + 1.0D;
        double high = maxExclusive - 1.001D;
        if (high < low) return (minInclusive + (double) maxExclusive) / 2.0D;
        return Math.max(low, Math.min(high, coordinate));
    }

    public static String normalizeTurfWarsKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace(' ', '-');
        return switch (kit) {
            case "marksman", "turfwars-marksman", "turf-wars-marksman" -> "marksman";
            case "infiltrator", "turfwars-infiltrator", "turf-wars-infiltrator" -> "infiltrator";
            case "shredder", "turfwars-shredder", "turf-wars-shredder" -> "shredder";
            default -> null;
        };
    }

    public static int turfWarsConstructorMax(String kit) {
        return switch (normalizeTurfWarsKit(kit) == null ? "marksman" : normalizeTurfWarsKit(kit)) {
            case "infiltrator" -> TURF_WARS_INFILTRATOR_MAX_WOOL;
            case "shredder" -> TURF_WARS_SHREDDER_MAX_WOOL;
            default -> TURF_WARS_MARKSMAN_MAX_WOOL;
        };
    }

    public static int turfWarsArrowMax(String kit) {
        return switch (normalizeTurfWarsKit(kit) == null ? "marksman" : normalizeTurfWarsKit(kit)) {
            case "infiltrator" -> TURF_WARS_INFILTRATOR_MAX_ARROWS;
            case "shredder" -> TURF_WARS_SHREDDER_MAX_ARROWS;
            default -> TURF_WARS_MARKSMAN_MAX_ARROWS;
        };
    }

    public static long turfWarsArrowIntervalMillis(String kit) {
        return switch (normalizeTurfWarsKit(kit) == null ? "marksman" : normalizeTurfWarsKit(kit)) {
            case "infiltrator" -> TURF_WARS_INFILTRATOR_ARROW_INTERVAL_MILLIS;
            case "shredder" -> TURF_WARS_SHREDDER_ARROW_INTERVAL_MILLIS;
            default -> TURF_WARS_MARKSMAN_ARROW_INTERVAL_MILLIS;
        };
    }

    /** Reborn advances one line for the first combat round, two for the second, etc. */
    public static int turfWarsLinesPerKill(int combatRoundsStarted, boolean mixedArcadeVariant) {
        int sourceLines = Math.max(1, combatRoundsStarted);
        return sourceLines * (mixedArcadeVariant ? 2 : 1);
    }

    /** Source axis vector points from the blue marker back toward the red marker. */
    public static int turfWarsMarkerDelta(int teamId, int axisDirection, int lines) {
        int count = Math.max(0, lines);
        if (teamId == 0) return -axisDirection * count;
        if (teamId == 1) return axisDirection * count;
        return 0;
    }

    /** A source turf tile belongs to a captured line if it shares any active map axis. */
    public static boolean turfWarsTileOnMarkerLine(int tileX, int tileZ,
            int markerX, int markerZ, int axisX, int axisZ) {
        return (axisX != 0 && tileX == markerX) || (axisZ != 0 && tileZ == markerZ);
    }

    public static int turfWarsRemainingLines(int frontCoordinate, int baseCoordinate) {
        return Math.abs(baseCoordinate - frontCoordinate);
    }

    /** The team closer to its authored goal has fewer remaining lines. */
    public static int turfWarsLeadingTeam(int redLinesRemaining, int blueLinesRemaining,
            int redKills, int blueKills) {
        if (redLinesRemaining < blueLinesRemaining) return 0;
        if (blueLinesRemaining < redLinesRemaining) return 1;
        if (redKills != blueKills) return redKills > blueKills ? 0 : 1;
        return -1;
    }

    public static double turfWarsHitDamage(boolean projectile, boolean ironSword) {
        return turfWarsHitDamage("marksman", projectile, ironSword);
    }

    public static double turfWarsHitDamage(String kit, boolean projectile, boolean ironSword) {
        if (projectile) {
            return "shredder".equals(normalizeTurfWarsKit(kit))
                    ? TURF_WARS_SHREDDER_ARROW_DAMAGE : 500.0D;
        }
        return ironSword ? TURF_WARS_IRON_SWORD_DAMAGE : TURF_WARS_MELEE_DAMAGE;
    }

    public static long turfWarsInitialBuildPhaseMillis(boolean mixedArcadeVariant) {
        return mixedArcadeVariant ? TURF_WARS_MIXED_ARCADE_INITIAL_BUILD_PHASE_MILLIS
                : TURF_WARS_INITIAL_BUILD_PHASE_MILLIS;
    }

    public static long turfWarsBuildPhaseMillis(boolean mixedArcadeVariant) {
        return mixedArcadeVariant ? TURF_WARS_MIXED_ARCADE_BUILD_PHASE_MILLIS
                : TURF_WARS_BUILD_PHASE_MILLIS;
    }

    public static long turfWarsCombatPhaseMillis(boolean mixedArcadeVariant) {
        return mixedArcadeVariant ? TURF_WARS_MIXED_ARCADE_COMBAT_PHASE_MILLIS
                : TURF_WARS_COMBAT_PHASE_MILLIS;
    }

    public static int advanceTurfFrontline(int currentFrontline, int teamId) {
        return advanceTurfFrontline(currentFrontline, teamId, 1);
    }

    public static int advanceTurfFrontline(int currentFrontline, int teamId, boolean mixedArcadeVariant) {
        return advanceTurfFrontline(currentFrontline, teamId, mixedArcadeVariant
                ? TURF_WARS_MIXED_ARCADE_TURF_ADVANCE_BLOCKS : 1);
    }

    public static int advanceTurfFrontline(int currentFrontline, int teamId, int blocks) {
        int distance = Math.max(0, blocks);
        if (teamId == 0) return currentFrontline + distance;
        if (teamId == 1) return currentFrontline - distance;
        return currentFrontline;
    }

    /**
     * Every newly captured column must be repainted when a kill advances the line.
     * The maximum X coordinate is exclusive, matching the source map bounds.
     */
    public static List<Integer> turfWarsCapturedColumns(int oldFrontline, int newFrontline,
            int teamId, int minX, int maxXExclusive) {
        if ((teamId != 0 && teamId != 1) || maxXExclusive <= minX || oldFrontline == newFrontline) {
            return List.of();
        }
        int first = teamId == 0 ? oldFrontline + 1 : newFrontline + 1;
        int last = teamId == 0 ? newFrontline : oldFrontline;
        first = Math.max(first, minX);
        last = Math.min(last, maxXExclusive - 1);
        if (first > last) return List.of();
        List<Integer> columns = new ArrayList<>(last - first + 1);
        for (int x = first; x <= last; x++) columns.add(x);
        return List.copyOf(columns);
    }

    /**
     * Reborn scales the starting Gladiators arenas to the lobby size: fewer players
     * start farther inward (stronger tier); larger lobbies use outer tiers first.
     * Tier index is GREEN=0, YELLOW=1, ORANGE=2, RED=3.
     */
    public static int gladiatorsStartingTierForPlayers(int players) {
        if (players <= 2) return 3;
        if (players <= 4) return 2;
        if (players <= 8) return 1;
        return 0;
    }

    /** Original Gladiators opens with green outer arenas, then moves inward by tier. */
    public static String gladiatorsArenaMarkerGroup(int round) {
        return switch (Math.max(0, Math.min(3, round))) {
            case 1 -> "YELLOW";
            case 2 -> "ORANGE";
            case 3 -> "RED";
            default -> "GREEN";
        };
    }

    /** A classic-tool challenge is only meaningful in a ruleset that supplies a bow or rod. */
    public static boolean shouldAwardGladiatorsNoBowOrRodWin(
            boolean classicToolsAvailable, boolean usedBow, boolean usedRod) {
        return classicToolsAvailable && !usedBow && !usedRod;
    }

    /** Half a heart is one health point in Bukkit's two-points-per-heart scale. */
    public static boolean shouldAwardGladiatorsHalfHeartWin(double winnerHealth) {
        return Double.isFinite(winnerHealth) && winnerHealth > 0.0D && winnerHealth <= 1.0001D;
    }

    /** A second tournament loss removes a player; the first loss only moves them to the lower bracket. */
    public static boolean shouldEliminateGladiatorAfterLoss(int totalLosses) {
        return totalLosses >= 2;
    }

    public static final double GLADIATORS_LOSER_ARENA_HALF_WIDTH = 3.65D;

    public static boolean isOutsideGladiatorsLoserArena(
            double x, double z, double centerX, double centerZ) {
        return !Double.isFinite(x) || !Double.isFinite(z)
                || !Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || Math.abs(x - centerX) > GLADIATORS_LOSER_ARENA_HALF_WIDTH
                || Math.abs(z - centerZ) > GLADIATORS_LOSER_ARENA_HALF_WIDTH;
    }

    /** WorldConfig min/max coordinates are inclusive block coordinates. */
    public static boolean isOutsideGladiatorsSourceMap(
            double x, double z, int minX, int maxX, int minZ, int maxZ) {
        return !Double.isFinite(x) || !Double.isFinite(z)
                || x <= minX || x >= maxX + 1.0D
                || z <= minZ || z >= maxZ + 1.0D;
    }

    public static String gladiatorsAchievementConfigKey(java.util.UUID playerId, String achievementId) {
        String safeId = achievementId == null ? "" : achievementId.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        return "test-achievements.gladiators." + playerId + "." + safeId;
    }

    public static String gladiatorsMilestoneConfigKey(java.util.UUID playerId) {
        return "test-milestones.gladiators." + playerId + ".silky-seconds";
    }

    /** Turf floor is the block below players' source spawn feet location. */
    public static int turfWarsFloorYFromSpawn(double spawnY) {
        if (!Double.isFinite(spawnY)) return 63;
        return (int) Math.floor(spawnY) - 1;
    }

    /** Initial line sits immediately to the left of the X-axis midpoint (red territory). */
    public static int turfWarsInitialFrontline(int minX, int maxXExclusive) {
        if (maxXExclusive <= minX) return -1;
        return (int) Math.ceil((minX + (double) maxXExclusive) / 2.0D) - 1;
    }

    /** Source-map build bounds use an inclusive minimum and exclusive maximum. */
    public static boolean isTurfWarsBuildWithinBounds(int x, int z,
            int minX, int maxXExclusive, int minZ, int maxZExclusive) {
        return maxXExclusive > minX && maxZExclusive > minZ
                && x >= minX && x < maxXExclusive && z >= minZ && z < maxZExclusive;
    }

    /** Only the half of the arena on the team's side of the current line may be built on. */
    public static boolean canTurfWarsBuildOnTerritory(int teamId, int x, int frontline) {
        if (teamId == 0) return x <= frontline;
        if (teamId == 1) return x > frontline;
        return false;
    }

    /** Source-map bounds are exclusive at the maximum coordinate. */
    public static boolean isTurfWarsWinningLine(int frontline, int teamId, int minX, int maxXExclusive) {
        if (maxXExclusive <= minX) return false;
        if (teamId == 0) return frontline >= maxXExclusive - 1;
        if (teamId == 1) return frontline < minX;
        return false;
    }

    /** Compare territory against the source map's starting line before applying a kill tiebreak. */
    public static int turfWarsTimeoutLeader(int frontline, int initialFrontline, int redKills, int blueKills) {
        if (frontline > initialFrontline) return 0;
        if (frontline < initialFrontline) return 1;
        if (redKills != blueKills) return redKills > blueKills ? 0 : 1;
        return -1;
    }

    public static boolean isTurfWarsWinningLine(int frontline, int teamId, int halfSize) {
        if (halfSize < 1) return false;
        if (teamId == 0) return frontline >= halfSize - 1;
        if (teamId == 1) return frontline < -halfSize;
        return false;
    }

    /**
     * Select a team winner for a timed elimination-game test. Remaining players
     * are the primary standing; aggregate kills break ties. A perfect tie has
     * no objective winner and may fall back to the game's normal score rule.
     */
    public static int teamWithBestTimeoutStanding(int teamZeroAlive, int teamOneAlive,
            int teamZeroKills, int teamOneKills) {
        if (teamZeroAlive != teamOneAlive) return teamZeroAlive > teamOneAlive ? 0 : 1;
        if (teamZeroKills != teamOneKills) return teamZeroKills > teamOneKills ? 0 : 1;
        return -1;
    }

    /**
     * Turf Wars is won by the captured frontline, not by an individual kill
     * count. Only when the line is unchanged do team kills provide a tiebreak.
     */
    public static int turfWarsTimeoutLeader(int frontline, int redKills, int blueKills) {
        if (frontline > -1) return 0;
        if (frontline < -1) return 1;
        if (redKills != blueKills) return redKills > blueKills ? 0 : 1;
        return -1;
    }
}
