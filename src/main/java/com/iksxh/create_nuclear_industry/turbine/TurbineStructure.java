package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
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
 * 从前端控制器扫描完整的 3×3×L 水平机组。读取前先检查区块可 tick，绝不为核验加载区块。
 * 返回的坐标均为世界坐标；局部 x 向右、z 从前端向后端、y 以轴心为 1。
 */
public final class TurbineStructure {
    private static final Map<Level, Map<BlockPos, WeakReference<TurbineControllerBlockEntity>>> OWNERS = new WeakHashMap<>();
    public record Form(Direction facing, int rotors, BlockPos rear, List<BlockPos> parts,
                       List<BlockPos> inlets, List<BlockPos> exhausts) {
        public int length() { return rotors + 2; }
        public BlockPos front() { return rear.relative(facing, length() - 1); }
        public boolean contains(BlockPos pos) { return parts.contains(pos); }
    }
    public record Issue(String reason, BlockPos pos) {}
    private record Result(Form form, Issue issue) {}
    private TurbineStructure() {}

    public static BlockPos at(BlockPos front, Direction facing, int x, int y, int z) {
        return front.relative(facing.getClockWise(), x).above(y - 1).relative(facing.getOpposite(), z);
    }

    public static boolean ticking(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return false;
        var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)
                && server.shouldTickBlocksAt(pos) && server.areEntitiesLoaded(ChunkPos.asLong(pos))
                && server.getWorldBorder().isWithinBounds(pos);
    }

    /** 三档由当前配置决定；非配置长度和超容量换档均安全拒绝。 */
    public static Form inspect(Level level, BlockPos front, TurbineState.Settings settings) {
        return diagnose(level, front, settings).form();
    }

    public static Issue issue(Level level, BlockPos front, TurbineState.Settings settings) {
        return diagnose(level, front, settings).issue();
    }

    private static Result diagnose(Level level, BlockPos front, TurbineState.Settings settings) {
        if (!ticking(level, front)) return new Result(null, new Issue("chunk", front));
        BlockState controller = level.getBlockState(front);
        if (!controller.is(TurbineContent.CONTROLLER.get())) return new Result(null, new Issue("controller", front));
        Direction facing = controller.getValue(TurbinePartBlock.MACHINE_FACING);
        Issue first = null;
        for (TurbineState.Tier tier : new TurbineState.Tier[]{settings.shortTier(), settings.mediumTier(), settings.longTier()}) {
            if (tier == null || !tier.valid()) continue;
            Result candidate = scan(level, front, facing, tier.rotorCount());
            if (candidate.form() != null) return candidate;
            if (first == null || "length".equals(first.reason())) first = candidate.issue();
        }
        return new Result(null, first == null ? new Issue("length", front) : first);
    }

    private static Result scan(Level level, BlockPos front, Direction facing, int rotors) {
        int length = rotors + 2;
        List<BlockPos> parts = new ArrayList<>(length * 9);
        List<BlockPos> inlets = new ArrayList<>();
        List<BlockPos> exhausts = new ArrayList<>();
        for (int z = 0; z < length; z++) for (int y = 0; y <= 2; y++) for (int x = -1; x <= 1; x++) {
            BlockPos pos = at(front, facing, x, y, z);
            if (!ticking(level, pos)) return new Result(null, new Issue("chunk", pos));
            BlockState state = level.getBlockState(pos);
            if (x == 0 && y == 1) {
                if (!state.is(z == 0 ? TurbineContent.CONTROLLER.get()
                        : z == length - 1 ? TurbineContent.OUTPUT_SHAFT.get() : TurbineContent.ROTOR.get()))
                    return new Result(null, new Issue(z == length - 1 && state.is(TurbineContent.ROTOR.get())
                            || z > 0 && z < length - 1 && state.is(TurbineContent.OUTPUT_SHAFT.get())
                            ? "length" : "axis", pos));
                if (z == 0 && state.getValue(TurbinePartBlock.MACHINE_FACING) != facing)
                    return new Result(null, new Issue("facing", pos));
            } else if (state.is(TurbineContent.CASING.get())) {
                // 端部及中段的八个环格皆可为壳；端口不能替换角位或底座。
            } else if (z > 0 && z < length - 1 && portSlot(x, y)
                    && (state.is(TurbineContent.INLET.get()) || state.is(TurbineContent.EXHAUST.get()))) {
                // 玩家可从任意合法面放置端口；成型时按实际格位自动写入唯一外法线。
                (state.is(TurbineContent.INLET.get()) ? inlets : exhausts).add(pos.immutable());
            } else return new Result(null, new Issue(z == 0 || z == length - 1 ? "end" : "ring", pos));
            // formed 仅为持久化外观提示；旧控制器拆除后不能把其残留标志当作归属证据。
            parts.add(pos.immutable());
        }
        if (inlets.isEmpty()) return new Result(null, new Issue("inlet", front));
        if (exhausts.isEmpty()) return new Result(null, new Issue("exhaust", front));
        BlockPos rear = at(front, facing, 0, 1, length - 1);
        // 更长的转子串不能以较短档误认：后轴必须确实占据本档末端。
        return new Result(new Form(facing, rotors, rear, List.copyOf(parts), List.copyOf(inlets), List.copyOf(exhausts)), null);
    }

    private static boolean portSlot(int x, int y) { return (y == 1 && x != 0) || (y == 2 && x == 0); }

    public static Direction outward(Direction facing, int x, int y) {
        return y == 2 ? Direction.UP : x < 0 ? facing.getCounterClockWise() : facing.getClockWise();
    }

    /** 仅已加载、仍能重新核验完整结构的 owner 才可占有构件；弱引用不延长世界生命周期。 */
    public static boolean unique(Level level, TurbineControllerBlockEntity proposed, Form form) {
        Map<BlockPos, WeakReference<TurbineControllerBlockEntity>> entries = OWNERS.get(level);
        if (entries == null) return true;
        Set<BlockPos> parts = null;
        entries.entrySet().removeIf(entry -> entry.getValue().get() == null);
        for (WeakReference<TurbineControllerBlockEntity> reference : entries.values()) {
            TurbineControllerBlockEntity other = reference.get();
            if (other == null || other == proposed) continue;
            // 世界中远处机器只看纯坐标包围盒，不触发其结构扫描或区块读取。
            Form old = other.currentForm();
            if (old == null || !boxesIntersect(form, old)) continue;
            Form claimed = other.claimedForm();
            if (claimed == null) continue;
            if (parts == null) parts = new HashSet<>(form.parts());
            for (BlockPos part : claimed.parts()) if (parts.contains(part)) return false;
        }
        return true;
    }

    private static boolean boxesIntersect(Form a, Form b) {
        BlockPos af = a.front(), bf = b.front();
        return Math.max(Math.min(af.getX(), a.rear().getX()) - 1, Math.min(bf.getX(), b.rear().getX()) - 1)
                <= Math.min(Math.max(af.getX(), a.rear().getX()) + 1, Math.max(bf.getX(), b.rear().getX()) + 1)
                && Math.max(af.getY() - 1, bf.getY() - 1) <= Math.min(af.getY() + 1, bf.getY() + 1)
                && Math.max(Math.min(af.getZ(), a.rear().getZ()) - 1, Math.min(bf.getZ(), b.rear().getZ()) - 1)
                <= Math.min(Math.max(af.getZ(), a.rear().getZ()) + 1, Math.max(bf.getZ(), b.rear().getZ()) + 1);
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

    public static TurbinePartBlock.RingRole ringRole(int x, int y) {
        if (y == 2) return x < 0 ? TurbinePartBlock.RingRole.UPPER_LEFT
                : x > 0 ? TurbinePartBlock.RingRole.UPPER_RIGHT : TurbinePartBlock.RingRole.TOP;
        if (y == 0) return x < 0 ? TurbinePartBlock.RingRole.LOWER_LEFT
                : x > 0 ? TurbinePartBlock.RingRole.LOWER_RIGHT : TurbinePartBlock.RingRole.BOTTOM;
        return x < 0 ? TurbinePartBlock.RingRole.LEFT : TurbinePartBlock.RingRole.RIGHT;
    }

    /** 普通构件变化只通知已加载候选控制器；owner 下一 tick 重扫并撤销旧轴源。 */
    public static void invalidateNearby(Level level, BlockPos changed) {
        if (level.isClientSide) return;
        for (Direction facing : Direction.Plane.HORIZONTAL) for (int z = 0; z <= 17; z++)
            for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) {
                BlockPos candidate = changed.relative(facing, z).relative(facing.getClockWise(), -x).above(1 - y);
                if (!ticking(level, candidate)) continue;
                if (level.getBlockEntity(candidate) instanceof TurbineControllerBlockEntity owner)
                    owner.invalidateForm();
            }
        if (level.hasChunkAt(changed)) level.invalidateCapabilities(changed);
    }

    /** 成型口状态提供定位提示，最终仍必须由 owner 核验完整结构与实际口集合。 */
    public static TurbineControllerBlockEntity ownerForPort(Level level, BlockPos port, BlockState state, boolean input) {
        if (!state.getValue(TurbinePartBlock.FORMED) || !ticking(level, port)) return null;
        Direction facing = state.getValue(TurbinePartBlock.MACHINE_FACING);
        TurbinePartBlock.RingRole role = state.getValue(TurbinePartBlock.RING_ROLE);
        int x = role == TurbinePartBlock.RingRole.LEFT ? -1 : role == TurbinePartBlock.RingRole.RIGHT ? 1 : 0;
        int y = role == TurbinePartBlock.RingRole.TOP ? 2 : 1;
        if (!portSlot(x, y) || state.getValue(TurbinePartBlock.OUTWARD) != outward(facing, x, y)) return null;
        BlockPos axis = port.relative(facing.getClockWise(), -x).above(1 - y);
        for (int z = 1; z <= 16; z++) {
            BlockPos front = axis.relative(facing, z);
            if (!ticking(level, front)) continue;
            if (level.getBlockEntity(front) instanceof TurbineControllerBlockEntity owner
                    && owner.validPort(port, input)) return owner;
        }
        return null;
    }
}
