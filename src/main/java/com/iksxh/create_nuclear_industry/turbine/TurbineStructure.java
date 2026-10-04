package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 从侧控制器定位三档水平八棱机组。局部 +x 为机器面向的顺时针侧，+y 向上，
 * +z 从前轴到后轴；读取每格前先确认区块可 tick，核验绝不主动加载区块。
 */
public final class TurbineStructure {
    private static final Map<Level, Map<BlockPos, WeakReference<TurbineControllerBlockEntity>>> OWNERS =
            new WeakHashMap<>();

    public record Form(Direction facing, int rotors, int diameter, BlockPos front, BlockPos rear,
                       BlockPos controller, List<BlockPos> parts, List<BlockPos> inlets,
                       List<BlockPos> exhausts, List<BlockPos> chunkSamples) {
        public int length() { return rotors + 2; }
        public boolean contains(BlockPos pos) { return parts.contains(pos); }
    }
    public record Issue(String reason, BlockPos pos) {}
    private record Result(Form form, Issue issue) {}
    private TurbineStructure() {}

    public static BlockPos at(BlockPos front, Direction facing, int x, int y, int z) {
        return front.relative(facing.getClockWise(), x).above(y).relative(facing.getOpposite(), z);
    }

    public static boolean ticking(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return false;
        var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)
                && server.shouldTickBlocksAt(pos) && server.areEntitiesLoaded(ChunkPos.asLong(pos))
                && server.getWorldBorder().isWithinBounds(pos);
    }

    public static Form inspect(Level level, BlockPos controller, TurbineState.Settings settings) {
        return diagnose(level, controller, settings).form();
    }

    public static Issue issue(Level level, BlockPos controller, TurbineState.Settings settings) {
        return diagnose(level, controller, settings).issue();
    }

    private static Result diagnose(Level level, BlockPos controller, TurbineState.Settings settings) {
        if (!ticking(level, controller)) return new Result(null, new Issue("chunk", controller));
        BlockState state = level.getBlockState(controller);
        if (!state.is(TurbineContent.CONTROLLER.get()))
            return new Result(null, new Issue("controller", controller));
        Direction facing = state.getValue(TurbinePartBlock.MACHINE_FACING);
        TurbinePartBlock.Side side = state.getValue(TurbinePartBlock.SIDE);
        Issue first = null;
        for (TurbineState.Tier tier : new TurbineState.Tier[]{settings.shortTier(),
                settings.mediumTier(), settings.longTier()}) {
            if (tier == null || !tier.valid()) continue;
            int length = tier.length(), radius = (tier.diameter() - 1) / 2;
            int x = side.x(radius), y = side.y(radius);
            for (int z : middleRows(length)) {
                BlockPos front = controller.relative(facing.getClockWise(), -x)
                        .below(y).relative(facing, z);
                Result candidate = scan(level, controller, front, facing, tier);
                if (candidate.form() != null) return candidate;
                if (first == null || "length".equals(first.reason())) first = candidate.issue();
            }
        }
        return new Result(null, first == null ? new Issue("length", controller) : first);
    }

    public static int[] middleRows(int length) {
        return length % 2 == 0 ? new int[]{length / 2 - 1, length / 2}
                : new int[]{length / 2};
    }

    private static Result scan(Level level, BlockPos controller, BlockPos front, Direction facing,
                               TurbineState.Tier tier) {
        int length = tier.length(), diameter = tier.diameter();
        int radius = (diameter - 1) / 2;
        List<BlockPos> parts = new ArrayList<>(length * diameter * diameter);
        List<BlockPos> inlets = new ArrayList<>(), exhausts = new ArrayList<>();
        List<BlockPos> chunkSamples = new ArrayList<>();
        Set<Long> seenChunks = new HashSet<>();
        int controllers = 0;
        for (int z = 0; z < length; z++) for (int y = -radius; y <= radius; y++)
            for (int x = -radius; x <= radius; x++) {
                if (!TurbineGeometry.footprint(diameter, x, y)) continue;
                BlockPos pos = at(front, facing, x, y, z);
                if (!ticking(level, pos)) return new Result(null, new Issue("chunk", pos));
                if (seenChunks.add(ChunkPos.asLong(pos))) chunkSamples.add(pos.immutable());
                BlockState state = level.getBlockState(pos);
                if (z == 0 || z == length - 1) {
                    if (x == 0 && y == 0) {
                        if (!state.is(TurbineContent.OUTPUT_SHAFT.get()))
                            return new Result(null, new Issue("axis", pos));
                    } else if (!state.is(TurbineContent.CASING.get()))
                        return new Result(null, new Issue("end", pos));
                } else if (x == 0 && y == 0) {
                    if (!state.is(TurbineContent.ROTOR.get()))
                        return new Result(null, new Issue("axis", pos));
                } else if (TurbineGeometry.airSlot(diameter, x, y)) {
                    if (!state.isAir()) return new Result(null, new Issue("cavity", pos));
                    continue;
                } else if (state.is(TurbineContent.CONTROLLER.get())) {
                    if (!middleRow(length, z) || !TurbineGeometry.sideSlot(diameter, x, y)
                            || !pos.equals(controller)) return new Result(null, new Issue("controller", pos));
                    controllers++;
                } else if (state.is(TurbineContent.INLET.get()) || state.is(TurbineContent.EXHAUST.get())) {
                    if (!TurbineGeometry.sideSlot(diameter, x, y))
                        return new Result(null, new Issue("port", pos));
                    if (state.is(TurbineContent.INLET.get())) {
                        if (!middleRow(length, z)) return new Result(null, new Issue("inlet_position", pos));
                        inlets.add(pos.immutable());
                    } else {
                        if (z != 1 && z != length - 2)
                            return new Result(null, new Issue("exhaust_position", pos));
                        exhausts.add(pos.immutable());
                    }
                } else if (state.is(TurbineContent.WINDOW.get())) {
                    if (!TurbineGeometry.windowSlot(diameter, x, y))
                        return new Result(null, new Issue("window", pos));
                } else if (!state.is(TurbineContent.CASING.get()))
                    return new Result(null, new Issue("ring", pos));
                parts.add(pos.immutable());
            }
        if (controllers != 1) return new Result(null, new Issue("controller", controller));
        if (inlets.isEmpty()) return new Result(null, new Issue("inlet", controller));
        if (exhausts.isEmpty()) return new Result(null, new Issue("exhaust", controller));
        BlockPos rear = at(front, facing, 0, 0, length - 1);
        return new Result(new Form(facing, tier.rotorCount(), diameter, front.immutable(),
                rear.immutable(), controller.immutable(), List.copyOf(parts), List.copyOf(inlets),
                List.copyOf(exhausts), List.copyOf(chunkSamples)), null);
    }

    /**
     * 已成型运行路径只检查少量实际区块票据、轴/口身份与内腔空气，不重试三档全体积扫描。
     * 构件移除/放置由事件使 owner 失效；空腔仍逐 tick 自检以覆盖流体等非玩家变更。
     */
    public static boolean quickLive(Level level, Form form) {
        for (BlockPos sample : form.chunkSamples()) if (!ticking(level, sample)) return false;
        BlockState front = level.getBlockState(form.front());
        BlockState rear = level.getBlockState(form.rear());
        if (!level.getBlockState(form.controller()).is(TurbineContent.CONTROLLER.get())
                || !front.is(TurbineContent.OUTPUT_SHAFT.get())
                || !rear.is(TurbineContent.OUTPUT_SHAFT.get())
                || front.getValue(TurbinePartBlock.MACHINE_FACING) != form.facing()
                || rear.getValue(TurbinePartBlock.MACHINE_FACING) != form.facing()
                || front.getValue(TurbineShaftBlock.END) != TurbineShaftBlock.End.FRONT
                || rear.getValue(TurbineShaftBlock.END) != TurbineShaftBlock.End.REAR) return false;
        for (BlockPos inlet : form.inlets()) if (!level.getBlockState(inlet).is(TurbineContent.INLET.get()))
            return false;
        for (BlockPos exhaust : form.exhausts()) if (!level.getBlockState(exhaust).is(TurbineContent.EXHAUST.get()))
            return false;
        int radius = (form.diameter() - 1) / 2;
        for (int z = 1; z < form.length() - 1; z++)
            for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++) {
                if (!TurbineGeometry.airSlot(form.diameter(), x, y)) continue;
                BlockPos pos = at(form.front(), form.facing(), x, y, z);
                if (!level.getBlockState(pos).isAir()) return false;
            }
        return true;
    }

    private static boolean middleRow(int length, int z) {
        return z == length / 2 || length % 2 == 0 && z == length / 2 - 1;
    }

    public static Direction outward(Direction facing, int x, int y) {
        return y > 0 ? Direction.UP : y < 0 ? Direction.DOWN
                : x < 0 ? facing.getCounterClockWise() : facing.getClockWise();
    }

    /** 重叠核验只比较活机组的坐标集合，不额外扫描世界或加载区块。 */
    public static boolean unique(Level level, TurbineControllerBlockEntity proposed, Form form) {
        Map<BlockPos, WeakReference<TurbineControllerBlockEntity>> entries = OWNERS.get(level);
        if (entries == null) return true;
        Set<BlockPos> parts = null;
        entries.entrySet().removeIf(entry -> entry.getValue().get() == null);
        for (WeakReference<TurbineControllerBlockEntity> reference : entries.values()) {
            TurbineControllerBlockEntity other = reference.get();
            if (other == null || other == proposed) continue;
            Form claimed = other.claimedForm();
            if (claimed == null) continue;
            if (parts == null) parts = new HashSet<>(form.parts());
            for (BlockPos part : claimed.parts()) if (parts.contains(part)) return false;
        }
        return true;
    }

    public static void claim(Level level, TurbineControllerBlockEntity owner) {
        OWNERS.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(owner.getBlockPos().immutable(), new WeakReference<>(owner));
    }

    public static void forget(Level level, TurbineControllerBlockEntity owner) {
        Map<BlockPos, WeakReference<TurbineControllerBlockEntity>> entries = OWNERS.get(level);
        if (entries != null) entries.entrySet().removeIf(entry -> entry.getValue().get() == null
                || entry.getValue().get() == owner);
    }

    public static TurbineControllerBlockEntity ownerForPart(Level level, BlockPos part) {
        if (!ticking(level, part)) return null;
        Map<BlockPos, WeakReference<TurbineControllerBlockEntity>> entries = OWNERS.get(level);
        if (entries == null) return null;
        for (WeakReference<TurbineControllerBlockEntity> reference : entries.values()) {
            TurbineControllerBlockEntity owner = reference.get();
            if (owner != null && owner.currentForm() != null && owner.currentForm().contains(part))
                return owner;
        }
        return null;
    }

    /** 构件或空腔变化通知近邻已加载控制器；上限使用配置允许的最大 18 格轴长。 */
    public static void invalidateNearby(Level level, BlockPos changed) {
        if (level.isClientSide) return;
        Map<BlockPos, WeakReference<TurbineControllerBlockEntity>> entries = OWNERS.get(level);
        if (entries != null) for (WeakReference<TurbineControllerBlockEntity> reference :
                List.copyOf(entries.values())) {
            TurbineControllerBlockEntity owner = reference.get();
            if (owner != null && owner.currentForm() != null
                    && withinBox(owner.currentForm(), changed)) owner.invalidateForm();
        }
        if (level.hasChunkAt(changed)) level.invalidateCapabilities(changed);
    }

    private static boolean withinBox(Form form, BlockPos pos) {
        BlockPos front = form.front();
        Direction facing = form.facing();
        int x = pos.getX() - front.getX(), y = pos.getY() - front.getY(), z = pos.getZ() - front.getZ();
        int localX = x * facing.getClockWise().getStepX() + z * facing.getClockWise().getStepZ();
        int localZ = x * facing.getOpposite().getStepX() + z * facing.getOpposite().getStepZ();
        int radius = (form.diameter() - 1) / 2;
        return Math.abs(localX) <= radius && Math.abs(y) <= radius
                && localZ >= 0 && localZ < form.length();
    }

    public static TurbineControllerBlockEntity ownerForPort(Level level, BlockPos port, BlockState state,
                                                              boolean input) {
        if (!state.getValue(TurbinePartBlock.FORMED)) return null;
        TurbineControllerBlockEntity owner = ownerForPart(level, port);
        return owner != null && owner.validPort(port, input) ? owner : null;
    }
}
