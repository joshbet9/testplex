package au.com.testplex.arcadeports;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

/** Paint operations constrained to the authored Draw My Thing canvas. */
final class DrawMyThingCanvas {
    private DrawMyThingCanvas() {}

    static void clear(Set<Block> canvas) {
        for (Block block : canvas) {
            if (block != null) block.setType(Material.WHITE_WOOL, false);
        }
    }

    static void point(Set<Block> canvas, Block block, Material colour, boolean thick) {
        if (!canvas.contains(block)) return;
        block.setType(colour, false);
        if (!thick) return;
        int normal = normalAxis(canvas);
        int[] axes = planeAxes(normal);
        for (int first : new int[] {-1, 1}) {
            for (int second : new int[] {-1, 1}) {
                Block nearby = offset(block, axes[0], first).getRelative(face(axes[1], second));
                if (canvas.contains(nearby)) nearby.setType(colour, false);
            }
            Block nearby = block.getRelative(face(axes[0], first));
            if (canvas.contains(nearby)) nearby.setType(colour, false);
        }
        for (int second : new int[] {-1, 1}) {
            Block nearby = block.getRelative(face(axes[1], second));
            if (canvas.contains(nearby)) nearby.setType(colour, false);
        }
    }

    static void line(Set<Block> canvas, Block from, Block to, Material colour, boolean thick) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(1, Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) * 2);
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            Block point = from.getWorld().getBlockAt(
                    (int) Math.round(from.getX() + dx * ratio),
                    (int) Math.round(from.getY() + dy * ratio),
                    (int) Math.round(from.getZ() + dz * ratio));
            point(canvas, point, colour, thick);
        }
    }

    static void rectangle(Set<Block> canvas, Block from, Block to, Material colour) {
        int normal = normalAxis(canvas);
        int[] axes = planeAxes(normal);
        int minA = Math.min(coordinate(from, axes[0]), coordinate(to, axes[0]));
        int maxA = Math.max(coordinate(from, axes[0]), coordinate(to, axes[0]));
        int minB = Math.min(coordinate(from, axes[1]), coordinate(to, axes[1]));
        int maxB = Math.max(coordinate(from, axes[1]), coordinate(to, axes[1]));
        int fixed = coordinate(from, normal);
        for (int a = minA; a <= maxA; a++) {
            paintCoordinate(canvas, from, normal, fixed, axes[0], a, axes[1], minB, colour);
            paintCoordinate(canvas, from, normal, fixed, axes[0], a, axes[1], maxB, colour);
        }
        for (int b = minB; b <= maxB; b++) {
            paintCoordinate(canvas, from, normal, fixed, axes[0], minA, axes[1], b, colour);
            paintCoordinate(canvas, from, normal, fixed, axes[0], maxA, axes[1], b, colour);
        }
    }

    static void ellipse(Set<Block> canvas, Block from, Block to, Material colour) {
        int normal = normalAxis(canvas);
        int[] axes = planeAxes(normal);
        double centerA = (coordinate(from, axes[0]) + coordinate(to, axes[0])) / 2.0D;
        double centerB = (coordinate(from, axes[1]) + coordinate(to, axes[1])) / 2.0D;
        double radiusA = Math.max(0.5D, Math.abs(coordinate(to, axes[0]) - coordinate(from, axes[0])) / 2.0D);
        double radiusB = Math.max(0.5D, Math.abs(coordinate(to, axes[1]) - coordinate(from, axes[1])) / 2.0D);
        int fixed = coordinate(from, normal);
        int segments = Math.max(24, (int) Math.ceil(Math.PI * 2.0D * Math.max(radiusA, radiusB) * 2.0D));
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            int a = (int) Math.round(centerA + Math.cos(angle) * radiusA);
            int b = (int) Math.round(centerB + Math.sin(angle) * radiusB);
            paintCoordinate(canvas, from, normal, fixed, axes[0], a, axes[1], b, colour);
        }
    }

    static void fill(Set<Block> canvas, Block start, Material colour) {
        if (!canvas.contains(start) || start.getType() == colour) return;
        Material originalType = start.getType();
        BlockData originalData = start.getBlockData().clone();
        ArrayDeque<Block> pending = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        pending.add(start);
        while (!pending.isEmpty()) {
            Block block = pending.removeFirst();
            if (!visited.add(block) || !canvas.contains(block)
                    || block.getType() != originalType || !originalData.matches(block.getBlockData())) continue;
            block.setType(colour, false);
            for (BlockFace face : BlockFace.values()) {
                if (Math.abs(face.getModX()) + Math.abs(face.getModY()) + Math.abs(face.getModZ()) == 1) {
                    Block adjacent = block.getRelative(face);
                    if (canvas.contains(adjacent) && !visited.contains(adjacent)) pending.addLast(adjacent);
                }
            }
        }
    }

    private static void paintCoordinate(Set<Block> canvas, Block anchor, int normal, int fixed,
                                        int first, int firstValue, int second, int secondValue,
                                        Material colour) {
        int[] coordinates = new int[3];
        coordinates[normal] = fixed;
        coordinates[first] = firstValue;
        coordinates[second] = secondValue;
        Block candidate = anchor.getWorld().getBlockAt(coordinates[0], coordinates[1], coordinates[2]);
        if (canvas.contains(candidate)) candidate.setType(colour, false);
    }

    private static int normalAxis(Set<Block> canvas) {
        if (canvas.isEmpty()) return 2;
        int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] max = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (Block block : canvas) {
            int[] value = {block.getX(), block.getY(), block.getZ()};
            for (int axis = 0; axis < 3; axis++) {
                min[axis] = Math.min(min[axis], value[axis]);
                max[axis] = Math.max(max[axis], value[axis]);
            }
        }
        int axis = 0;
        for (int candidate = 1; candidate < 3; candidate++) {
            if (max[candidate] - min[candidate] < max[axis] - min[axis]) axis = candidate;
        }
        return axis;
    }

    private static int[] planeAxes(int normal) {
        int[] axes = new int[2];
        int index = 0;
        for (int axis = 0; axis < 3; axis++) if (axis != normal) axes[index++] = axis;
        return axes;
    }

    private static int coordinate(Block block, int axis) {
        return axis == 0 ? block.getX() : axis == 1 ? block.getY() : block.getZ();
    }

    private static Block offset(Block block, int axis, int amount) {
        return block.getRelative(face(axis, amount));
    }

    private static BlockFace face(int axis, int amount) {
        if (axis == 0) return amount < 0 ? BlockFace.WEST : BlockFace.EAST;
        if (axis == 1) return amount < 0 ? BlockFace.DOWN : BlockFace.UP;
        return amount < 0 ? BlockFace.NORTH : BlockFace.SOUTH;
    }
}
