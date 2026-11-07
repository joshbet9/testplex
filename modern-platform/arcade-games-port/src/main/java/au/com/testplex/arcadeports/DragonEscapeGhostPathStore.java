package au.com.testplex.arcadeports;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Owns the Dragon Escape personal-best ghost file format, frame compaction and playback interpolation.
 * All methods are called on the main thread, as they were in ArcadePortManager. No background
 * tasks, packet listeners or additional work in the per-tick loop are introduced here.
 */
final class DragonEscapeGhostPathStore {
    private static final int FILE_MAGIC = 0x44454748; // "DEGH"
    private static final int FILE_VERSION = 1;
    private static final int MAX_FRAMES = 120_000;

    private final Path dataFolder;
    private final Logger logger;
    private final Map<String, List<DragonEscapeGhostFrame>> frameCache = new HashMap<>();

    DragonEscapeGhostPathStore(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    static void append(List<DragonEscapeGhostFrame> frames, DragonEscapeGhostFrame frame) {
        if (frames == null || frame == null) return;
        if (!frames.isEmpty() && frame.timeMillis() <= frames.get(frames.size() - 1).timeMillis()) return;
        frames.add(frame);
        if (frames.size() > MAX_FRAMES) {
            List<DragonEscapeGhostFrame> compacted = new ArrayList<>(frames.size() / 2 + 1);
            for (int i = 0; i < frames.size(); i += 2) compacted.add(frames.get(i));
            DragonEscapeGhostFrame last = frames.get(frames.size() - 1);
            if (compacted.get(compacted.size() - 1) != last) compacted.add(last);
            frames.clear();
            frames.addAll(compacted);
        }
    }

    private String cacheKey(UUID playerId, String mapResource) {
        String mapKey = ArcadePortRules.dragonEscapeMapStorageKey(mapResource);
        return playerId == null || mapKey.isBlank() ? "" : playerId + ":" + mapKey;
    }

    private Path file(UUID playerId, String mapResource) {
        String mapKey = ArcadePortRules.dragonEscapeMapStorageKey(mapResource);
        if (playerId == null || mapKey.isBlank()) return null;
        String safeMapKey = mapKey.replaceAll("[^A-Za-z0-9._-]", "_");
        return dataFolder.resolve("dragon-escape-ghosts")
                .resolve(playerId.toString()).resolve(safeMapKey + ".ghost.gz");
    }

    void save(UUID playerId, String mapResource, List<DragonEscapeGhostFrame> frames) {
        if (frames == null || frames.isEmpty()) return;
        Path target = file(playerId, mapResource);
        String key = cacheKey(playerId, mapResource);
        if (target == null || key.isEmpty()) return;
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                    new GZIPOutputStream(Files.newOutputStream(temporary))))) {
                output.writeInt(FILE_MAGIC);
                output.writeInt(FILE_VERSION);
                output.writeInt(frames.size());
                for (DragonEscapeGhostFrame frame : frames) {
                    output.writeLong(frame.timeMillis());
                    output.writeDouble(frame.x());
                    output.writeDouble(frame.y());
                    output.writeDouble(frame.z());
                    output.writeFloat(frame.yaw());
                    output.writeFloat(frame.pitch());
                    output.writeBoolean(frame.sneaking());
                    output.writeBoolean(frame.sprinting());
                    output.writeBoolean(frame.onGround());
                }
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            frameCache.put(key, List.copyOf(frames));
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            logger.warning("Could not save Dragon Escape PB ghost path for " + playerId
                    + " on " + mapResource + ": " + exception.getMessage());
        }
    }

    List<DragonEscapeGhostFrame> load(UUID playerId, String mapResource) {
        String key = cacheKey(playerId, mapResource);
        if (key.isEmpty()) return List.of();
        List<DragonEscapeGhostFrame> cached = frameCache.get(key);
        if (cached != null) return cached;
        Path target = file(playerId, mapResource);
        if (target == null || !Files.isRegularFile(target)) return List.of();
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(target))))) {
            if (input.readInt() != FILE_MAGIC || input.readInt() != FILE_VERSION) return List.of();
            int count = input.readInt();
            if (count < 1 || count > MAX_FRAMES) return List.of();
            List<DragonEscapeGhostFrame> frames = new ArrayList<>(count);
            long previousTime = -1L;
            for (int i = 0; i < count; i++) {
                DragonEscapeGhostFrame frame = new DragonEscapeGhostFrame(
                        input.readLong(), input.readDouble(), input.readDouble(), input.readDouble(),
                        input.readFloat(), input.readFloat(),
                        input.readBoolean(), input.readBoolean(), input.readBoolean());
                if (frame.timeMillis() < previousTime
                        || !Double.isFinite(frame.x()) || !Double.isFinite(frame.y())
                        || !Double.isFinite(frame.z()) || !Float.isFinite(frame.yaw())
                        || !Float.isFinite(frame.pitch())) return List.of();
                previousTime = frame.timeMillis();
                frames.add(frame);
            }
            List<DragonEscapeGhostFrame> loaded = List.copyOf(frames);
            frameCache.put(key, loaded);
            return loaded;
        } catch (IOException | RuntimeException exception) {
            logger.warning("Could not read Dragon Escape PB ghost path for " + playerId
                    + " on " + mapResource + ": " + exception.getMessage());
            return List.of();
        }
    }

    static DragonEscapeGhostFrame interpolate(List<DragonEscapeGhostFrame> frames, long elapsed) {
        if (frames == null || frames.isEmpty()) return null;
        if (elapsed <= frames.get(0).timeMillis()) return frames.get(0);
        int lastIndex = frames.size() - 1;
        if (elapsed >= frames.get(lastIndex).timeMillis()) return frames.get(lastIndex);
        int low = 0;
        int high = lastIndex;
        while (low + 1 < high) {
            int middle = (low + high) >>> 1;
            if (frames.get(middle).timeMillis() <= elapsed) low = middle;
            else high = middle;
        }
        DragonEscapeGhostFrame from = frames.get(low);
        DragonEscapeGhostFrame to = frames.get(high);
        long duration = Math.max(1L, to.timeMillis() - from.timeMillis());
        double fraction = Math.max(0.0D, Math.min(1.0D,
                (elapsed - from.timeMillis()) / (double) duration));
        double yawDelta = ((to.yaw() - from.yaw() + 540.0D) % 360.0D) - 180.0D;
        boolean useNextPose = fraction >= 0.5D;
        return new DragonEscapeGhostFrame(elapsed,
                ArcadePortRules.interpolateDragonEscapeCoordinate(from.x(), to.x(), fraction),
                ArcadePortRules.interpolateDragonEscapeCoordinate(from.y(), to.y(), fraction),
                ArcadePortRules.interpolateDragonEscapeCoordinate(from.z(), to.z(), fraction),
                (float) (from.yaw() + yawDelta * fraction),
                (float) (from.pitch() + (to.pitch() - from.pitch()) * fraction),
                useNextPose ? to.sneaking() : from.sneaking(),
                useNextPose ? to.sprinting() : from.sprinting(),
                useNextPose ? to.onGround() : from.onGround());
    }
}

/** On-disk v1 frame layout; its field order and primitive widths must remain stable. */
record DragonEscapeGhostFrame(long timeMillis, double x, double y, double z,
                              float yaw, float pitch, boolean sneaking, boolean sprinting,
                              boolean onGround) { }
