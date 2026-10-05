package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 只从两端轴和连续转子列推导搭建外观，绝不认领机主或提供流体、动力。
 * 查询只读已加载区块，放拆件时局部刷新；运行资格仍由 TurbineStructure 完整扫描决定。
 */
public final class TurbineAssembly {
    public enum MatchKind { NONE, UNIQUE, AMBIGUOUS }
    /** 区分尚无轴列与多轴列重叠；辅助放置不得把后者当成普通放置。 */
    public record Match(MatchKind kind, Layout layout) {}

    public record Layout(BlockPos front, Direction facing, int length, int diameter) {
        public int radius() { return (diameter - 1) / 2; }
        public BlockPos at(int x, int y, int z) {
            return TurbineStructure.at(front, facing, x, y, z);
        }
        public boolean contains(BlockPos pos) {
            int dx = pos.getX() - front.getX(), dz = pos.getZ() - front.getZ();
            int x = dx * facing.getClockWise().getStepX() + dz * facing.getClockWise().getStepZ();
            int z = dx * facing.getOpposite().getStepX() + dz * facing.getOpposite().getStepZ();
            int y = pos.getY() - front.getY();
            return z >= 0 && z < length && TurbineGeometry.footprint(diameter, x, y);
        }
        public int axial(BlockPos pos) {
            return (pos.getX() - front.getX()) * facing.getOpposite().getStepX()
                    + (pos.getZ() - front.getZ()) * facing.getOpposite().getStepZ();
        }
        public int cross(BlockPos pos) {
            return (pos.getX() - front.getX()) * facing.getClockWise().getStepX()
                    + (pos.getZ() - front.getZ()) * facing.getClockWise().getStepZ();
        }
    }

    private TurbineAssembly() {}

    /** 点击构件所属的唯一轴列；多个相交候选都占该格时不猜测机组。 */
    public static Layout at(Level level, BlockPos pos) {
        return lookup(level, pos).layout();
    }

    public static Match lookup(Level level, BlockPos pos) {
        List<Layout> matches = near(level, pos);
        matches.removeIf(layout -> !layout.contains(pos));
        return matches.isEmpty() ? new Match(MatchKind.NONE, null)
                : matches.size() == 1 ? new Match(MatchKind.UNIQUE, matches.getFirst())
                : new Match(MatchKind.AMBIGUOUS, null);
    }

    private static List<Layout> near(Level level, BlockPos pos) {
        Set<BlockPos> shafts = new HashSet<>();
        for (int dy = -3; dy <= 3; dy++) for (int a = -3; a <= 3; a++)
            for (int b = -3; b <= 3; b++) {
                BlockPos seed = pos.offset(a, dy, b);
                if (!loaded(level, seed)) continue;
                BlockState state = level.getBlockState(seed);
                if (shaft(state)) shafts.add(seed.immutable());
                else if (state.is(TurbineContent.ROTOR.get())) {
                    for (Direction direction : Direction.Plane.HORIZONTAL)
                        for (int distance = 1; distance <= 16; distance++) {
                            BlockPos end = seed.relative(direction, distance);
                            if (!loaded(level, end)) break;
                            BlockState step = level.getBlockState(end);
                            if (shaft(step)) { shafts.add(end.immutable()); break; }
                            if (!step.is(TurbineContent.ROTOR.get())) break;
                        }
                }
            }
        Set<Layout> found = new HashSet<>();
        for (BlockPos shaft : shafts) for (Direction facing : Direction.Plane.HORIZONTAL)
            for (TurbineState.Tier tier : tiers()) {
                if (tier == null || !tier.valid()) continue;
                Layout candidate = new Layout(shaft, facing, tier.length(), tier.diameter());
                if (completeAxis(level, candidate)) found.add(candidate);
            }
        // 两端均可被当成起点时，仅根据现有端轴状态选一个方向；同格相交的不同轴列仍交给上层判歧义。
        List<Layout> result = new ArrayList<>();
        for (Layout layout : found) {
            Layout reverse = new Layout(layout.at(0, 0, layout.length() - 1),
                    layout.facing().getOpposite(), layout.length(), layout.diameter());
            if (!found.contains(reverse) || compareEnds(level, layout, reverse) >= 0)
                result.add(layout);
        }
        result.sort(Comparator.comparingLong((Layout l) -> l.front().asLong())
                .thenComparingInt(l -> l.facing().get2DDataValue()));
        return result;
    }

    private static int compareEnds(Level level, Layout a, Layout b) {
        int first = score(level, a), second = score(level, b);
        if (first != second) return Integer.compare(first, second);
        // 无端位提示时固定北端或西端为前端，避免刷新顺序使形态跳转。
        return a.facing() == Direction.NORTH || a.facing() == Direction.WEST ? 1 : -1;
    }

    private static int score(Level level, Layout layout) {
        BlockState front = level.getBlockState(layout.front());
        BlockState rear = level.getBlockState(layout.at(0, 0, layout.length() - 1));
        int score = front.getValue(TurbinePartBlock.MACHINE_FACING) == layout.facing() ? 2 : 0;
        if (front.getValue(TurbineShaftBlock.END) == TurbineShaftBlock.End.FRONT) score++;
        if (rear.getValue(TurbineShaftBlock.END) == TurbineShaftBlock.End.REAR) score += 2;
        return score;
    }

    private static TurbineState.Tier[] tiers() {
        var settings = TurbineConfig.settings();
        return new TurbineState.Tier[]{settings.shortTier(), settings.mediumTier(), settings.longTier()};
    }

    private static boolean completeAxis(Level level, Layout layout) {
        if (!loaded(level, layout.front()) || !shaft(level.getBlockState(layout.front()))) return false;
        for (int z = 1; z < layout.length() - 1; z++) {
            BlockPos pos = layout.at(0, 0, z);
            if (!loaded(level, pos) || !level.getBlockState(pos).is(TurbineContent.ROTOR.get())) return false;
        }
        BlockPos rear = layout.at(0, 0, layout.length() - 1);
        return loaded(level, rear) && shaft(level.getBlockState(rear));
    }

    private static boolean shaft(BlockState state) { return state.is(TurbineContent.OUTPUT_SHAFT.get()); }
    private static boolean loaded(Level level, BlockPos pos) { return level.hasChunkAt(pos); }

    /** 放件后只重写唯一的局部布局；完整 Form 的形成由控制器自己的 tick 决定。 */
    public static void refreshNear(Level level, BlockPos changed) {
        if (level.isClientSide) return;
        Layout layout = at(level, changed);
        if (layout != null) style(level, layout);
        else clearLostAxis(level, changed);
    }

    /**
     * 断轴后从邻近已定位构件反推轴心，只清理同方向、同直径且连续的残余轴段。
     * 保留每块物理方块和其独立板面，不跨空隙追到相邻机组。
     */
    private static void clearLostAxis(Level level, BlockPos changed) {
        Set<BlockPos> visitedCenters = new HashSet<>();
        for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos partPos = changed.offset(dx, dy, dz);
                if (!loaded(level, partPos)) continue;
                BlockState part = level.getBlockState(partPos);
                if (!part.hasProperty(TurbinePartBlock.LOCATED)
                        || !part.getValue(TurbinePartBlock.LOCATED)) continue;
                Direction facing = part.getValue(TurbinePartBlock.MACHINE_FACING);
                int diameter = part.hasProperty(TurbinePartBlock.DIAMETER)
                        ? part.getValue(TurbinePartBlock.DIAMETER).blocks() : 0;
                BlockPos center = partPos;
                if (part.hasProperty(TurbinePartBlock.PIECE)) {
                    TurbineGeometry.Piece piece = TurbineGeometry.piece(part.getValue(TurbinePartBlock.PIECE));
                    if (piece == null) continue;
                    diameter = piece.diameter();
                    center = partPos.relative(facing.getClockWise(), -piece.x()).below(piece.y());
                }
                if (diameter == 0 || !visitedCenters.add(center)) continue;
                clearAxisSegment(level, center, facing, diameter);
            }
    }

    private static void clearAxisSegment(Level level, BlockPos seed, Direction facing, int diameter) {
        clearCrossSection(level, seed, facing, diameter);
        for (Direction direction : new Direction[]{facing, facing.getOpposite()})
            for (int distance = 1; distance <= 17; distance++) {
                BlockPos center = seed.relative(direction, distance);
                if (!loaded(level, center)) break;
                BlockState core = level.getBlockState(center);
                if (!core.is(TurbineContent.ROTOR.get())
                        && !core.is(TurbineContent.OUTPUT_SHAFT.get())) break;
                clearCrossSection(level, center, facing, diameter);
            }
    }

    private static void clearCrossSection(Level level, BlockPos center, Direction facing, int diameter) {
        if (at(level, center) != null) return;
        int radius = (diameter - 1) / 2;
        for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++) {
            if (!TurbineGeometry.footprint(diameter, x, y)) continue;
            BlockPos pos = center.relative(facing.getClockWise(), x).above(y);
            if (!loaded(level, pos)) continue;
            BlockState before = level.getBlockState(pos);
            if (!before.hasProperty(TurbinePartBlock.LOCATED)
                    || !before.getValue(TurbinePartBlock.LOCATED)
                    || before.getValue(TurbinePartBlock.MACHINE_FACING) != facing) continue;
            BlockState after = before.setValue(TurbinePartBlock.LOCATED, false)
                    .setValue(TurbinePartBlock.FORMED, false);
            if (before.hasProperty(TurbinePartBlock.PIECE)) {
                TurbineGeometry.Piece piece = TurbineGeometry.piece(before.getValue(TurbinePartBlock.PIECE));
                if (piece == null || piece.diameter() != diameter) continue;
                Direction outer = piece.section() == TurbineGeometry.Section.FRONT ? facing
                        : piece.section() == TurbineGeometry.Section.REAR ? facing.getOpposite()
                        : Math.abs(piece.y()) >= Math.abs(piece.x())
                        ? piece.y() >= 0 ? Direction.UP : Direction.DOWN
                        : piece.x() >= 0 ? facing.getClockWise() : facing.getCounterClockWise();
                after = after.setValue(TurbinePartBlock.PIECE,
                        TurbinePartBlock.independentPiece(outer));
            }
            if (before.hasProperty(TurbinePartBlock.DIAMETER))
                after = after.setValue(TurbinePartBlock.DIAMETER, TurbinePartBlock.Diameter.D3);
            if (after != before) level.setBlock(pos, after, 3);
        }
    }

    /** 拆件时延后一 tick，在世界已移除旧方块后刷新相邻存件。 */
    public static void refreshAfterRemoval(Level level, BlockPos removed) {
        if (level.isClientSide) return;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = removed.relative(direction);
            if (!loaded(level, neighbor)) continue;
            Block block = level.getBlockState(neighbor).getBlock();
            if (block instanceof TurbinePartBlock || block instanceof TurbineShaftBlock.Output)
                level.scheduleTick(neighbor, block, 1);
        }
    }

    public static void style(Level level, Layout layout) {
        int radius = layout.radius();
        for (int z = 0; z < layout.length(); z++)
            for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++) {
                if (!TurbineGeometry.footprint(layout.diameter(), x, y)) continue;
                BlockPos pos = layout.at(x, y, z);
                if (!loaded(level, pos)) return;
                BlockState before = level.getBlockState(pos);
                BlockState after = stylePart(before, layout, x, y, z);
                if (after != before) level.setBlock(pos, after, 3);
            }
    }

    private static BlockState stylePart(BlockState before, Layout layout, int x, int y, int z) {
        if (before.is(TurbineContent.CASING.get()) || before.is(TurbineContent.WINDOW.get())) {
            TurbineGeometry.Section section = z == 0 ? TurbineGeometry.Section.FRONT
                    : z == layout.length() - 1 ? TurbineGeometry.Section.REAR : TurbineGeometry.Section.MIDDLE;
            int piece = TurbineGeometry.pieceId(layout.diameter(), section, x, y);
            if (piece == 0 || before.is(TurbineContent.WINDOW.get())
                    && (section != TurbineGeometry.Section.MIDDLE
                    || !TurbineGeometry.windowSlot(layout.diameter(), x, y))) return before;
            return before.setValue(TurbinePartBlock.LOCATED, true)
                    .setValue(TurbinePartBlock.MACHINE_FACING, layout.facing())
                    .setValue(TurbinePartBlock.PIECE, piece);
        }
        if (x == 0 && y == 0 && z > 0 && z < layout.length() - 1
                && before.is(TurbineContent.ROTOR.get()))
            return before.setValue(TurbinePartBlock.LOCATED, true)
                    .setValue(TurbinePartBlock.MACHINE_FACING, layout.facing())
                    .setValue(TurbinePartBlock.DIAMETER,
                            TurbinePartBlock.Diameter.of(layout.diameter()));
        if (x == 0 && y == 0 && (z == 0 || z == layout.length() - 1)
                && before.is(TurbineContent.OUTPUT_SHAFT.get()))
            return before.setValue(TurbinePartBlock.LOCATED, true)
                    .setValue(TurbinePartBlock.MACHINE_FACING, layout.facing())
                    .setValue(TurbineShaftBlock.END,
                            z == 0 ? TurbineShaftBlock.End.FRONT : TurbineShaftBlock.End.REAR);
        if (before.hasProperty(TurbinePartBlock.LOCATED)
                && (before.is(TurbineContent.CONTROLLER.get()) || before.is(TurbineContent.INLET.get())
                || before.is(TurbineContent.EXHAUST.get()))) {
            if (!TurbineGeometry.sideSlot(layout.diameter(), x, y)) return before;
            // 偶数轴长有两排中央位；错误位置的接口保留独立件外观，交给完整扫描报错。
            boolean central = z == layout.length() / 2
                    || layout.length() % 2 == 0 && z == layout.length() / 2 - 1;
            if (before.is(TurbineContent.CONTROLLER.get()) && !central
                    || before.is(TurbineContent.INLET.get()) && !central
                    || before.is(TurbineContent.EXHAUST.get())
                    && z != 1 && z != layout.length() - 2) return before;
            BlockState after = before.setValue(TurbinePartBlock.LOCATED, true)
                    .setValue(TurbinePartBlock.MACHINE_FACING, layout.facing());
            if (before.is(TurbineContent.CONTROLLER.get()))
                return after.setValue(TurbinePartBlock.SIDE, TurbinePartBlock.Side.at(x, y));
            Direction outward = TurbineStructure.outward(layout.facing(), x, y);
            TurbinePartBlock.RingRole role = switch (TurbinePartBlock.Side.at(x, y)) {
                case UP -> TurbinePartBlock.RingRole.TOP;
                case DOWN -> TurbinePartBlock.RingRole.BOTTOM;
                case LEFT -> TurbinePartBlock.RingRole.LEFT;
                case RIGHT -> TurbinePartBlock.RingRole.RIGHT;
            };
            return after.setValue(TurbinePartBlock.RING_ROLE, role)
                    .setValue(TurbinePartBlock.OUTWARD, outward);
        }
        return before;
    }
}
