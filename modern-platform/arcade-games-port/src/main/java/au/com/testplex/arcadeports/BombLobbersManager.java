package au.com.testplex.arcadeports;

import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/** Game-specific scoreboard only; the shared Arcade runtime still owns each match. */
final class BombLobbersManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.BOMB_LOBBERS; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupScoreboard(session);
    }

    private void setupScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("bomblob", "dummy",
                ChatColor.GOLD + "BOMB LOBBERS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.bombLobbersScoreboard = scoreboard;
        session.bombLobbersScoreboardObjective = objective;

        session.bombRedScoreboardLine = scoreboard.registerNewTeam("bombred");
        session.bombRedScoreboardEntry = ChatColor.RED.toString();
        session.bombRedScoreboardLine.addEntry(session.bombRedScoreboardEntry);
        session.bombRedScoreboardLine.setPrefix(ChatColor.RED + "Red Team: ");
        objective.getScore(session.bombRedScoreboardEntry).setScore(3);

        Team blank = scoreboard.registerNewTeam("bombblank");
        String blankEntry = ChatColor.DARK_GRAY.toString();
        blank.addEntry(blankEntry);
        blank.setPrefix(" ");
        objective.getScore(blankEntry).setScore(2);

        session.bombBlueScoreboardLine = scoreboard.registerNewTeam("bombblue");
        session.bombBlueScoreboardEntry = ChatColor.BLUE.toString();
        session.bombBlueScoreboardLine.addEntry(session.bombBlueScoreboardEntry);
        session.bombBlueScoreboardLine.setPrefix(ChatColor.BLUE + "Blue Team: ");
        objective.getScore(session.bombBlueScoreboardEntry).setScore(1);

        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateScoreboard(session);
    }

    void updateScoreboard(SessionState session) {
        if (session == null || session.bombLobbersScoreboardObjective == null) return;
        int red = countAliveTeammates(session, 0);
        int blue = countAliveTeammates(session, 1);
        if (session.bombRedScoreboardLine != null) {
            session.bombRedScoreboardLine.setSuffix(powerupScoreboardText(session, 0)
                    + ChatColor.WHITE + Integer.toString(red));
        }
        if (session.bombBlueScoreboardLine != null) {
            session.bombBlueScoreboardLine.setSuffix(powerupScoreboardText(session, 1)
                    + ChatColor.WHITE + Integer.toString(blue));
        }
    }

    /** Sample line-of-sight like vanilla 1.8 World#getBlockDensity. */
    double blastExposure(Location blast, Player target) {
        if (blast == null || target == null || blast.getWorld() == null
                || target.getWorld() != blast.getWorld()) return 0.0D;
        BoundingBox box = target.getBoundingBox();
        double xStep = ArcadePortRules.bombLobbersExposureSampleStep((box.getMaxX() - box.getMinX()));
        double yStep = ArcadePortRules.bombLobbersExposureSampleStep(box.getHeight());
        double zStep = ArcadePortRules.bombLobbersExposureSampleStep((box.getMaxZ() - box.getMinZ()));
        if (!Double.isFinite(xStep) || !Double.isFinite(yStep) || !Double.isFinite(zStep)) return 0.0D;
        double xOffset = ArcadePortRules.bombLobbersExposureSampleOffset(xStep);
        double zOffset = ArcadePortRules.bombLobbersExposureSampleOffset(zStep);
        int visible = 0;
        int total = 0;
        for (float fx = 0.0F; fx <= 1.0F; fx = (float) (fx + xStep)) {
            for (float fy = 0.0F; fy <= 1.0F; fy = (float) (fy + yStep)) {
                for (float fz = 0.0F; fz <= 1.0F; fz = (float) (fz + zStep)) {
                    Location sample = new Location(blast.getWorld(),
                            box.getMinX() + (box.getMaxX() - box.getMinX()) * fx + xOffset,
                            box.getMinY() + box.getHeight() * fy,
                            box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * fz + zOffset);
                    Vector ray = sample.toVector().subtract(blast.toVector());
                    double distance = ray.length();
                    total++;
                    if (distance <= 1.0E-6D
                            || blast.getWorld().rayTraceBlocks(blast, ray.normalize(), distance,
                                    org.bukkit.FluidCollisionMode.NEVER, true) == null) visible++;
                }
            }
        }
        return total == 0 ? 0.0D : visible / (double) total;
    }

    void renderTrail(SessionState session, UUID ownerId, Location previous, Location current) {
        if (session == null || ownerId == null || previous == null || current == null
                || previous.getWorld() != session.world || current.getWorld() != session.world) return;
        Vector segment = current.toVector().subtract(previous.toVector());
        double distance = segment.length();
        if (!Double.isFinite(distance) || distance <= 1.0E-6D) return;
        int samples = ArcadePortRules.bombLobbersTrailSampleCount(distance);
        if (samples <= 0) return;

        int team = session.teams.getOrDefault(ownerId, 0);
        double spread = ArcadePortRules.bombLobbersTrailOffset(team);
        double speed = ArcadePortRules.bombLobbersTrailSpeed(team);
        Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.0F);
        Vector direction = segment.multiply(1.0D / distance);
        for (int sample = 0; sample < samples; sample++) {
            double offset = sample * GameDebugRuntimeOverrides.doubleValue("BOMB_LOBBERS_TRAIL_SAMPLE_SPACING", ArcadePortRules.BOMB_LOBBERS_TRAIL_SAMPLE_SPACING);
            if (offset > distance) break;
            Location point = previous.clone().add(direction.clone().multiply(offset)).add(0.0D, 0.5D, 0.0D);
            session.world.spawnParticle(Particle.DUST, point, 2,
                    spread, spread, spread, speed, dust);
        }
    }

    private int countAliveTeammates(SessionState session, int team) {
        int count = 0;
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()
                    && session.teams.getOrDefault(id, -1) == team) count++;
        }
        return count;
    }

    private String powerupScoreboardText(SessionState session, int team) {
        BombLobbersPowerupStatus status = session.bombPowerupStatusByTeam.get(team);
        if (status == null) return "";
        ChatColor color = switch (status) {
            case SPAWNED -> ChatColor.DARK_GRAY;
            case OBTAINED -> ChatColor.YELLOW;
            case ACTIVATED -> ChatColor.GREEN;
        };
        return color + " Powerup " + ChatColor.RESET;
    }
}
