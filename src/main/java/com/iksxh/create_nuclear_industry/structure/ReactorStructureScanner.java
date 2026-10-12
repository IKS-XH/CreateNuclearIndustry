package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition.LocalPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;

/**
 * 从已加载侧面寻边并验证完整长方体；仪表不需要朝向，四个水平法向各尝试一次。
 * 每个坐标在同次扫描只读取一次，不枚举尺寸与原点组合，也不加载缺失区块。
 */
public final class ReactorStructureScanner {
    /** 四次完整验证最多4×4096，加四面寻边480、穿径60和仪表1格，共16925次。 */
    public static final int MAX_BLOCK_READS = 17_000;
    private static final String INSTRUMENT_ID = id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID);
    private static final String CASING_ID = id(P1ContentIds.REACTOR_CASING_ID);
    private ReactorStructureScanner() { }

    /** 世界读取适配器只用于结构证据；缺区块返回null，空气必须返回真实空气ID。 */
    @FunctionalInterface
    public interface BlockReader { String readLoaded(BlockPos pos); }

    /** 读取SERVER联合尺寸配置后扫描；先检查hasChunkAt，禁止getBlockState隐式加载。 */
    public static WorldScanResult scanInstrumentPort(Level level, BlockPos owner) {
        if (level == null || owner == null) return WorldScanResult.invalid(ReactorStructureDefinition.DiagnosticCode.GENERIC_FAILURE, "level and instrument are required");
        return scanInstrumentPort(owner, pos -> level.hasChunkAt(pos)
                ? BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString() : null,
                P1ServerConfig.VALUES.minDimension.get(), P1ServerConfig.VALUES.maxDimension.get());
    }

    /**
     * 有限表面推断入口，供世界适配与纯扫描断言共用。
     * 邻接结构的棱边通过表面及内侧一格同为墙定位，不以连通表面总体包围盒合并两台设备。
     */
    public static WorldScanResult scanInstrumentPort(BlockPos owner, BlockReader reader, int min, int max) {
        if (!P1ServerConfig.validReactorDimensionRange(min, max))
            return WorldScanResult.invalid(ReactorStructureDefinition.DiagnosticCode.DIMENSIONS, "invalid reactor min/max dimensions");
        var cache = new ReadCache(reader);
        if (!INSTRUMENT_ID.equals(cache.read(owner))) return WorldScanResult.invalid(ReactorStructureDefinition.DiagnosticCode.INSTRUMENT_PORT, "instrument port is not loaded");
        WorldScanResult valid = null;
        WorldScanResult failure = null;
        boolean missing = false;
        for (Direction inward : Direction.Plane.HORIZONTAL) {
            Direction along = inward.getClockWise();
            int left = seekEdge(cache, owner, along.getOpposite(), inward, false);
            int right = seekEdge(cache, owner, along, inward, false);
            int down = seekEdge(cache, owner, Direction.DOWN, inward, true);
            int up = seekEdge(cache, owner, Direction.UP, inward, true);
            if (left < 1 || right < 1 || down < 1 || up < 1) { missing |= cache.missing; continue; }
            int across = left + right + 1, height = down + up + 1;
            if (across < min || across > max || height < min || height > max) continue;
            int thickness = seekOpposite(cache, owner, inward);
            if (thickness < min || thickness > max) { missing |= cache.missing; continue; }
            BlockPos oppositeLow = owner.relative(along.getOpposite(), left).below(down).relative(inward, thickness - 1);
            BlockPos nearLow = owner.relative(along, right).below(down);
            BlockPos origin = new BlockPos(Math.min(nearLow.getX(), oppositeLow.getX()), owner.getY() - down,
                    Math.min(nearLow.getZ(), oppositeLow.getZ()));
            ReactorDimensions dimensions = inward.getAxis() == Direction.Axis.X
                    ? new ReactorDimensions(thickness, height, across) : new ReactorDimensions(across, height, thickness);
            Map<LocalPosition, String> blocks = new HashMap<>();
            boolean unavailable = false;
            for (LocalPosition local : ReactorStructureDefinition.allPositions(dimensions)) {
                String block = cache.read(origin.offset(local.x(), local.y(), local.z()));
                if (block == null) { unavailable = true; break; }
                blocks.put(local, block);
            }
            if (unavailable) { missing = true; continue; }
            var contract = ReactorStructureDefinition.scan(dimensions, blocks);
            WorldScanResult result = new WorldScanResult(origin, contract, cache.reads);
            if (contract.valid()) {
                if (valid != null && !valid.origin.equals(result.origin))
                    return WorldScanResult.invalid(ReactorStructureDefinition.DiagnosticCode.GENERIC_FAILURE, "ambiguous reactor ownership");
                valid = result;
            } else if (failure == null) failure = result;
        }
        if (valid != null) return new WorldScanResult(valid.origin, valid.contract, cache.reads);
        if (missing) return new WorldScanResult(null, new ReactorStructureDefinition.ScanResult(false,
                "reactor waits for loaded chunks", ReactorStructureDefinition.DiagnosticCode.CHUNKS_UNAVAILABLE,
                Map.of(), Map.of()), cache.reads);
        return failure != null ? new WorldScanResult(failure.origin, failure.contract, cache.reads)
                : new WorldScanResult(null, new ReactorStructureDefinition.ScanResult(false,
                "no closed reactor frame in configured dimensions", ReactorStructureDefinition.DiagnosticCode.STRUCTURE_BLOCKS,
                Map.of(), Map.of()), cache.reads);
    }

    /** 找最近的棱边；内部燃料不是墙，因此仪表内侧邻接燃料仍能沿墙正确寻边。 */
    private static int seekEdge(ReadCache cache, BlockPos owner, Direction travel, Direction inward, boolean vertical) {
        for (int step = 1; step < ReactorDimensions.MAX_DIMENSION; step++) {
            BlockPos pos = owner.relative(travel, step);
            String surface = cache.read(pos);
            if (!wall(surface)) return -1;
            String inner = cache.read(pos.relative(inward));
            if (CASING_ID.equals(surface) && (vertical ? cap(inner) : wall(inner))) return step;
        }
        return -1;
    }
    private static int seekOpposite(ReadCache cache, BlockPos owner, Direction inward) {
        for (int step = 1; step < ReactorDimensions.MAX_DIMENSION; step++) {
            String inner = cache.read(owner.relative(inward, step));
            if (inner == null) return -1;
            if (wall(inner)) return step + 1;
            if (!ReactorStructureDefinition.AIR_ID.equals(inner) && !id(P1ContentIds.REACTOR_FUEL_ROD_ID).equals(inner)) return -1;
        }
        return -1;
    }
    private static boolean cap(String block) {
        return CASING_ID.equals(block) || id(P1ContentIds.REACTOR_REFUELING_PORT_ID).equals(block) || id(P1ContentIds.CONTROL_ROD_DRIVE_ID).equals(block);
    }
    private static boolean wall(String block) {
        return CASING_ID.equals(block) || id(P1ContentIds.REACTOR_WINDOW_ID).equals(block)
                || INSTRUMENT_ID.equals(block) || id(P1ContentIds.REACTOR_COLD_PORT_ID).equals(block)
                || id(P1ContentIds.REACTOR_HOT_PORT_ID).equals(block);
    }
    private static String id(String path) { return "create_nuclear_industry:" + path; }

    /** 只扫描索引中的已加载仪表，有限距离过滤后每个所有者更新一次。 */
    public static void rescanAround(Level level, BlockPos changed) {
        for (var instrument : ReactorStructureLifecycle.loadedOwnersNear(level, changed))
            instrument.updateStructureCache(scanInstrumentPort(level, instrument.getBlockPos()));
    }

    private static final class ReadCache {
        private final BlockReader reader;
        private final Map<BlockPos, String> cache = new HashMap<>();
        private int reads;
        private boolean missing;
        private ReadCache(BlockReader reader) { this.reader = reader; }
        private String read(BlockPos pos) {
            if (cache.containsKey(pos)) return cache.get(pos);
            if (++reads > MAX_BLOCK_READS) throw new IllegalStateException("reactor scan read budget exceeded");
            String value = reader.readLoaded(pos);
            cache.put(pos.immutable(), value);
            missing |= value == null;
            return value;
        }
    }

    /** 世界原点、实际结构契约和本次去重读取数；读取数只用于诊断，不参与玩法。 */
    public record WorldScanResult(BlockPos origin, ReactorStructureDefinition.ScanResult contract, int blockReadCount) {
        public WorldScanResult {
            origin = origin == null ? null : origin.immutable();
            if (contract == null) throw new IllegalArgumentException("structure contract is required");
        }
        public WorldScanResult(BlockPos origin, ReactorStructureDefinition.ScanResult contract) { this(origin, contract, 0); }
        public boolean valid() { return contract.valid(); }
        public String failureReason() { return contract.failureReason(); }
        public ReactorStructureDefinition.DiagnosticCode diagnosticCode() { return contract.diagnosticCode(); }
        public static WorldScanResult invalid(ReactorStructureDefinition.DiagnosticCode code, String reason) {
            return new WorldScanResult(null, new ReactorStructureDefinition.ScanResult(false, reason, code, Map.of(), Map.of()));
        }
    }
}
