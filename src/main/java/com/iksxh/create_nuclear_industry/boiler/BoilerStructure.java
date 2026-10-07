package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 有界的长方体分区验证器。结构变更或区块可用性恢复触发全体积扫描，不加载未知区块。 */
public final class BoilerStructure {
    private static final Map<Level, Map<BlockPos, WeakReference<BoilerControllerBlockEntity>>> LOADED = new WeakHashMap<>();
    /** 明确闭包围盒和隔层；全部成员位置不可变，所有归属与管道刷新复用此快照。 */
    public record Form(BlockPos min, BlockPos max, int partitionY, int waterCells, int steamCells,
            List<BlockPos> sections, List<BlockPos> exchangers, List<BlockPos> waterPorts,
            List<BlockPos> steamPorts, List<BlockPos> hotPorts, List<BlockPos> coldPorts,
            List<BlockPos> windows, BlockPos valve) {
        public int width() { return max.getX() - min.getX() + 1; }
        public int height() { return max.getY() - min.getY() + 1; }
        public int depth() { return max.getZ() - min.getZ() + 1; }
        public boolean contains(BlockPos p) { return p.getX() >= min.getX() && p.getX() <= max.getX()
                && p.getY() >= min.getY() && p.getY() <= max.getY() && p.getZ() >= min.getZ() && p.getZ() <= max.getZ(); }
        public List<BlockPos> ports() {
            var result = new ArrayList<BlockPos>(); result.addAll(waterPorts); result.addAll(steamPorts);
            result.addAll(hotPorts); result.addAll(coldPorts); return List.copyOf(result);
        }
        public Direction outward(BlockPos p) { return p.getX() == min.getX() ? Direction.WEST : p.getX() == max.getX()
                ? Direction.EAST : p.getZ() == min.getZ() ? Direction.NORTH : Direction.SOUTH; }
        public boolean loaded(Level level) {
            for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++)
                for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++)
                    if (!level.hasChunk(x, z)) return false;
            return true;
        }
    }
    public record Issue(String reason, BlockPos pos) {}
    private record Inspection(Form form, Issue issue) {}
    private BoilerStructure() {}
    public static void register(Level level, BoilerControllerBlockEntity owner) {
        if (level != null && !level.isClientSide) {
            LOADED.computeIfAbsent(level, ignored -> new HashMap<>()).put(owner.getBlockPos(), new WeakReference<>(owner));
            retryFailedPeers(level, owner.getBlockPos());
        }
    }
    public static void forget(Level level, BoilerControllerBlockEntity owner) {
        var entries = LOADED.get(level);
        if (entries != null) { entries.remove(owner.getBlockPos()); retryFailedPeers(level, owner.getBlockPos()); }
    }
    private static List<BoilerControllerBlockEntity> controllers(Level level) {
        var entries = LOADED.get(level);
        if (entries == null) return List.of();
        entries.values().removeIf(r -> r.get() == null);
        return entries.values().stream().map(WeakReference::get).filter(java.util.Objects::nonNull).toList();
    }
    /** 只通知已加载且在工程扫描封顶内的控制器，不枚举世界坐标或加载区块。 */
    public static void invalidateNearby(Level level, BlockPos changed) {
        if (level == null || level.isClientSide) return;
        for (var owner : controllers(level)) if (near(owner.getBlockPos(), changed) && owner.affectedBy(changed)) {
            owner.invalidateForm(); retryFailedPeers(level, owner.getBlockPos());
        }
        if (level.hasChunkAt(changed)) level.invalidateCapabilities(changed);
    }
    private static boolean near(BlockPos a, BlockPos b) { return Math.abs(a.getX() - b.getX()) <= 32
            && Math.abs(a.getY() - b.getY()) <= 32 && Math.abs(a.getZ() - b.getZ()) <= 32; }
    /** 两个边长最多32的闭包围盒相交时，任意两控制器轴向距离至多62，不能沿用单炉32格半径。 */
    private static boolean possiblePeers(BlockPos a, BlockPos b) { return Math.abs(a.getX() - b.getX()) <= 62
            && Math.abs(a.getY() - b.getY()) <= 62 && Math.abs(a.getZ() - b.getZ()) <= 62; }
    private static void retryFailedPeers(Level level, BlockPos changedController) {
        for (var owner : controllers(level)) if (possiblePeers(changedController, owner.getBlockPos())) owner.retryFailedForm();
    }
    /** 成型结构占位不可重叠；不会递归调用其他控制器的可变缓存。 */
    static boolean unique(Level level, BlockPos controller, Form form) {
        for (var other : controllers(level)) {
            if (other.getBlockPos().equals(controller) || !possiblePeers(controller, other.getBlockPos())) continue;
            Form f = inspect(level, other.getBlockPos());
            if (f != null && form.min().getX() <= f.max().getX() && form.max().getX() >= f.min().getX()
                    && form.min().getY() <= f.max().getY() && form.max().getY() >= f.min().getY()
                    && form.min().getZ() <= f.max().getZ() && form.max().getZ() >= f.min().getZ()) {
                // 后成型者同样撤销先成型者，避免后者永久保留先前的独占快照；仅实交且已有快照时置脏。
                other.rejectOverlap();
                return false;
            }
        }
        return true;
    }
    /** 按水区内一点六向寻边，再一次验证完整包围盒；支持偶数尺寸和偏心控制器。 */
    public static Form inspect(Level level, BlockPos controller) { return inspectDetailed(level, controller).form(); }
    public static Issue issue(Level level, BlockPos controller) {
        var result = inspectDetailed(level, controller);
        return result.issue() != null ? result.issue() : new Issue("overlap", controller);
    }
    private static Inspection fail(String reason, BlockPos pos) { return new Inspection(null, new Issue(reason, pos)); }
    private static BlockPos wall(Level level, BlockPos start, Direction direction, int limit) {
        for (int i = 1; i <= limit; i++) {
            BlockPos p = start.relative(direction, i);
            if (!level.hasChunkAt(p)) return null;
            if (!level.getBlockState(p).isAir()) return p;
        }
        return null;
    }
    private static Inspection inspectDetailed(Level level, BlockPos controller) {
        var cfg = BoilerConfig.settings();
        if (!cfg.valid()) return fail("invalid", controller);
        if (level == null || !level.hasChunkAt(controller)) return fail("chunk", controller);
        BlockState control = level.getBlockState(controller);
        if (!control.is(BoilerContent.CONTROLLER.get())) return fail("controller", controller);
        BlockPos inside = controller.relative(control.getValue(BoilerPartBlock.FACING).getOpposite());
        if (!level.hasChunkAt(inside) || !level.getBlockState(inside).isAir()) return fail("interior", inside);
        BlockPos west = wall(level, inside, Direction.WEST, cfg.maxDimension());
        BlockPos east = wall(level, inside, Direction.EAST, cfg.maxDimension());
        BlockPos north = wall(level, inside, Direction.NORTH, cfg.maxDimension());
        BlockPos south = wall(level, inside, Direction.SOUTH, cfg.maxDimension());
        BlockPos bottom = wall(level, inside, Direction.DOWN, cfg.maxDimension());
        BlockPos partition = wall(level, inside, Direction.UP, cfg.maxDimension());
        if (west == null || east == null || north == null || south == null || bottom == null || partition == null) return fail("bounds", controller);
        if (!level.hasChunkAt(partition.above()) || !level.getBlockState(partition.above()).isAir()) return fail("partition", partition);
        BlockPos top = wall(level, partition.above(), Direction.UP, cfg.maxDimension());
        if (top == null) return fail("bounds", partition);
        BlockPos min = new BlockPos(west.getX(), bottom.getY(), north.getZ());
        BlockPos max = new BlockPos(east.getX(), top.getY(), south.getZ());
        int w = max.getX() - min.getX() + 1, h = max.getY() - min.getY() + 1, d = max.getZ() - min.getZ() + 1, py = partition.getY();
        for (int size : new int[]{w, h, d}) if (size < cfg.minDimension() || size > cfg.maxDimension()) return fail("bounds", controller);
        if (py < min.getY() + 2 || py > max.getY() - 2) return fail("partition", partition);
        List<BlockPos> sections = new ArrayList<>(), machines = new ArrayList<>(), water = new ArrayList<>(), steam = new ArrayList<>(), hot = new ArrayList<>(), cold = new ArrayList<>(), windows = new ArrayList<>();
        BlockPos valve = null; int controls = 0, wc = 0, sc = 0;
        for (BlockPos mutable : BlockPos.betweenClosed(min, max)) {
            BlockPos p = mutable.immutable();
            if (!level.hasChunkAt(p)) return fail("chunk", p);
            BlockState s = level.getBlockState(p);
            boolean xEdge = p.getX() == min.getX() || p.getX() == max.getX();
            boolean yEdge = p.getY() == min.getY() || p.getY() == max.getY();
            boolean zEdge = p.getZ() == min.getZ() || p.getZ() == max.getZ();
            int edges = (xEdge ? 1 : 0) + (yEdge ? 1 : 0) + (zEdge ? 1 : 0);
            // 底层非角点热液口占用一个水平底边格；只放开这一类部件，并要求其水平接口朝外。
            if (edges == 2 && p.getY() == min.getY() && (xEdge ^ zEdge)
                    && s.is(BoilerContent.HOT_PORT.get())) {
                Direction outward = xEdge ? (p.getX() == min.getX() ? Direction.WEST : Direction.EAST)
                        : (p.getZ() == min.getZ() ? Direction.NORTH : Direction.SOUTH);
                if (!s.hasProperty(BoilerPartBlock.FACING) || s.getValue(BoilerPartBlock.FACING) != outward)
                    return fail("edge", p);
                hot.add(p);
                continue;
            }
            if (edges >= 2) { if (!s.is(BoilerContent.CASING.get())) return fail("edge", p); continue; }
            if (edges == 0) {
                if (p.getY() == py) {
                    if (s.is(BoilerContent.HEAT_SECTION.get())) sections.add(p);
                    else if (!s.is(BoilerContent.CASING.get())) return fail("partition", p);
                } else { if (!s.isAir()) return fail("interior", p); if (p.getY() < py) wc++; else sc++; }
                continue;
            }
            if (yEdge) {
                if (s.is(BoilerContent.CASING.get())) continue;
                if (p.getY() == min.getY() && s.is(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get())) { machines.add(p); continue; }
                if (p.getY() == max.getY() && s.is(BoilerContent.SAFETY_VALVE.get()) && valve == null) { valve = p; continue; }
                return fail("base_shell", p);
            }
            if (s.is(BoilerContent.CASING.get())) continue;
            if (s.is(BoilerContent.WINDOW.get()) && p.getY() != py) { windows.add(p); continue; }
            Direction outward = p.getX() == min.getX() ? Direction.WEST : p.getX() == max.getX() ? Direction.EAST
                    : p.getZ() == min.getZ() ? Direction.NORTH : Direction.SOUTH;
            if (!s.hasProperty(BoilerPartBlock.FACING) || s.getValue(BoilerPartBlock.FACING) != outward) return fail("side", p);
            if (s.is(BoilerContent.CONTROLLER.get()) && p.equals(controller) && p.getY() < py) { controls++; continue; }
            if (s.is(BoilerContent.WATER_PORT.get()) && p.getY() < py) { water.add(p); continue; }
            if (s.is(BoilerContent.STEAM_PORT.get()) && p.getY() > py) { steam.add(p); continue; }
            if (s.is(BoilerContent.COLD_PORT.get()) && p.getY() == py) { cold.add(p); continue; }
            return fail("side", p);
        }
        if (controls != 1 || valve == null || machines.isEmpty() || sections.isEmpty() || water.isEmpty()
                || steam.isEmpty() || hot.isEmpty() || cold.isEmpty()) return fail("missing", controller);
        return new Inspection(new Form(min, max, py, wc, sc, List.copyOf(sections), List.copyOf(machines),
                List.copyOf(water), List.copyOf(steam), List.copyOf(hot), List.copyOf(cold), List.copyOf(windows), valve), null);
    }
    /** 归属只查询已加载控制器的缓存；世界端口及炉内换热器不另造中心坐标。 */
    public static BoilerControllerBlockEntity owner(Level level, BlockPos part, boolean water) { return owner(level, part); }
    public static BoilerControllerBlockEntity ownerOfSection(Level level, BlockPos part) { return owner(level, part); }
    public static BoilerControllerBlockEntity owner(Level level, BlockPos part) {
        if (level == null || level.isClientSide || !level.hasChunkAt(part)) return null;
        for (var owner : controllers(level)) {
            if (!near(owner.getBlockPos(), part)) continue;
            Form f = owner.currentForm();
            if (f != null && f.contains(part)) return owner;
        }
        return null;
    }
}
