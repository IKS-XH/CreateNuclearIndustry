package com.iksxh.create_nuclear_industry.structure;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import net.minecraft.nbt.CompoundTag;

/**
 * 仪表端口对外提供的静态结构摘要。
 *
 * <p>摘要只从最近一次有效结构扫描缓存和服务端配置派生，不持有或复制
 * {@code ReactorSnapshot}。容量由空列与控制棒列的内部主体格共享计算，单位为 mB；
 * 冷态与热态库存属于动态遥测，不在本摘要中伪造为两个独立容量。</p>
 */
public record ReactorInstrumentStructureSummary(
        boolean valid,
        String unavailableReason,
        int width,
        int height,
        int depth,
        int fuelColumnCount,
        int controlRodColumnCount,
        int coldPortCount,
        int hotPortCount,
        int emptyColumnCount,
        int coolantSpaceBlockCount,
        long coolantCapacityPerEmptyBlockMb,
        long coolantCapacityMb
) {
    private static final String VALID_KEY = "Valid";
    private static final String WIDTH_KEY = "Width";
    private static final String HEIGHT_KEY = "Height";
    private static final String DEPTH_KEY = "Depth";
    private static final String FUEL_COLUMN_COUNT_KEY = "FuelColumnCount";
    private static final String CONTROL_ROD_COLUMN_COUNT_KEY = "ControlRodColumnCount";
    private static final String COLD_PORT_COUNT_KEY = "ColdPortCount";
    private static final String HOT_PORT_COUNT_KEY = "HotPortCount";
    private static final String EMPTY_COLUMN_COUNT_KEY = "EmptyColumnCount";
    private static final String COOLANT_SPACE_BLOCK_COUNT_KEY = "CoolantSpaceBlockCount";
    private static final String COOLANT_CAPACITY_PER_EMPTY_BLOCK_MB_KEY =
            "CoolantCapacityPerEmptyBlockMb";
    private static final String COOLANT_CAPACITY_MB_KEY = "CoolantCapacityMb";

    public ReactorInstrumentStructureSummary {
        if (unavailableReason == null) {
            throw new IllegalArgumentException("summary availability reason is required");
        }
        if (width < 0 || height < 0 || depth < 0
                || fuelColumnCount < 0 || controlRodColumnCount < 0
                || coldPortCount < 0 || hotPortCount < 0 || emptyColumnCount < 0
                || coolantSpaceBlockCount < 0 || coolantCapacityPerEmptyBlockMb < 0L
                || coolantCapacityMb < 0L) {
            throw new IllegalArgumentException("structure summary values must be non-negative");
        }
        if (emptyColumnCount > coolantSpaceBlockCount) {
            throw new IllegalArgumentException("empty column count exceeds coolant space count");
        }
        if (coolantCapacityMb != ReactorCoolantCapacity.capacityFor(
                coolantSpaceBlockCount, coolantCapacityPerEmptyBlockMb)) {
            throw new IllegalArgumentException("coolant summary capacity is inconsistent");
        }
        if (valid) {
            if (!unavailableReason.isEmpty()) {
                throw new IllegalArgumentException("valid structure summary cannot have an unavailable reason");
            }
            if (width == 0 || height == 0 || depth == 0
                    || fuelColumnCount == 0 || coldPortCount == 0 || hotPortCount == 0) {
                throw new IllegalArgumentException("valid structure summary must contain structure data");
            }
        } else if (unavailableReason.isBlank()) {
            throw new IllegalArgumentException("invalid structure summary needs an unavailable reason");
        }
    }

    /**
     * 从结构缓存和每格服务端配置生成摘要，不触发新的世界扫描。
     *
     * @param scan 最近一次结构扫描结果
     * @param coolantCapacityPerEmptyBlockMb 每个可计主体格容量，单位为 mB
     * @return 有效结构摘要，或带扫描失败原因的不可用摘要
     */
    public static ReactorInstrumentStructureSummary from(
            ReactorStructureDefinition.ScanResult scan,
            long coolantCapacityPerEmptyBlockMb
    ) {
        Objects.requireNonNull(scan, "structure scan is required");
        ReactorCoolantCapacity.validateCapacityPerEmptyBlockMb(coolantCapacityPerEmptyBlockMb);
        if (!scan.valid()) {
            String reason = scan.failureReason() == null || scan.failureReason().isBlank()
                    ? scan.diagnosticCode().translationKey()
                    : scan.failureReason();
            return unavailable(reason);
        }

        int fuelColumns = countColumns(scan.columns(), ReactorStructureDefinition.ColumnType.FUEL);
        int controlRodColumns = countColumns(
                scan.columns(), ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        int coldPorts = uniqueValidPortCount(scan, ReactorStructureDefinition.PortType.COLD_COOLANT);
        int hotPorts = uniqueValidPortCount(scan, ReactorStructureDefinition.PortType.HOT_COOLANT);
        ReactorCoolantCapacity.Derived capacity = ReactorCoolantCapacity.fromScan(
                scan, coolantCapacityPerEmptyBlockMb);

        return new ReactorInstrumentStructureSummary(
                true,
                "",
                ReactorStructureDefinition.SIZE,
                ReactorStructureDefinition.SIZE,
                ReactorStructureDefinition.SIZE,
                fuelColumns,
                controlRodColumns,
                coldPorts,
                hotPorts,
                capacity.emptyColumnCount(),
                capacity.coolantSpaceBlockCount(),
                capacity.coolantCapacityPerEmptyBlockMb(),
                capacity.coolantCapacityMb()
        );
    }

    /** 返回尚未成型或扫描失败时的明确不可用摘要。 */
    public static ReactorInstrumentStructureSummary unavailable(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("unavailable summary reason is required");
        }
        return new ReactorInstrumentStructureSummary(false, reason, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0L, 0L);
    }

    /**
     * 编码供客户端只读显示的摘要字段。
     *
     * <p>该数据只用于方块实体更新标签，不写入持久化结构；客户端不能借此反向改变服务端
     * 结构缓存，也不能根据同步字段重新扫描或计算容量。</p>
     */
    public CompoundTag writeSyncTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(VALID_KEY, valid);
        if (!valid) {
            return tag;
        }
        tag.putInt(WIDTH_KEY, width);
        tag.putInt(HEIGHT_KEY, height);
        tag.putInt(DEPTH_KEY, depth);
        tag.putInt(FUEL_COLUMN_COUNT_KEY, fuelColumnCount);
        tag.putInt(CONTROL_ROD_COLUMN_COUNT_KEY, controlRodColumnCount);
        tag.putInt(COLD_PORT_COUNT_KEY, coldPortCount);
        tag.putInt(HOT_PORT_COUNT_KEY, hotPortCount);
        tag.putInt(EMPTY_COLUMN_COUNT_KEY, emptyColumnCount);
        tag.putInt(COOLANT_SPACE_BLOCK_COUNT_KEY, coolantSpaceBlockCount);
        tag.putLong(COOLANT_CAPACITY_PER_EMPTY_BLOCK_MB_KEY, coolantCapacityPerEmptyBlockMb);
        tag.putLong(COOLANT_CAPACITY_MB_KEY, coolantCapacityMb);
        return tag;
    }

    /** 解码客户端更新数据中的摘要；缺字段或非法值一律降级为不可用。 */
    public static ReactorInstrumentStructureSummary readSyncTag(CompoundTag tag) {
        if (tag == null || !tag.getBoolean(VALID_KEY)) {
            return unavailable("structure summary is not synchronized");
        }
        if (!hasAllFields(tag)) {
            return unavailable("structure summary synchronization data is incomplete");
        }
        try {
            return new ReactorInstrumentStructureSummary(
                    true,
                    "",
                    tag.getInt(WIDTH_KEY),
                    tag.getInt(HEIGHT_KEY),
                    tag.getInt(DEPTH_KEY),
                    tag.getInt(FUEL_COLUMN_COUNT_KEY),
                    tag.getInt(CONTROL_ROD_COLUMN_COUNT_KEY),
                    tag.getInt(COLD_PORT_COUNT_KEY),
                    tag.getInt(HOT_PORT_COUNT_KEY),
                    tag.getInt(EMPTY_COLUMN_COUNT_KEY),
                    tag.getInt(COOLANT_SPACE_BLOCK_COUNT_KEY),
                    tag.getLong(COOLANT_CAPACITY_PER_EMPTY_BLOCK_MB_KEY),
                    tag.getLong(COOLANT_CAPACITY_MB_KEY)
            );
        } catch (IllegalArgumentException exception) {
            return unavailable("structure summary synchronization data is invalid");
        }
    }

    private static boolean hasAllFields(CompoundTag tag) {
        return tag.contains(WIDTH_KEY) && tag.contains(HEIGHT_KEY) && tag.contains(DEPTH_KEY)
                && tag.contains(FUEL_COLUMN_COUNT_KEY)
                && tag.contains(CONTROL_ROD_COLUMN_COUNT_KEY)
                && tag.contains(COLD_PORT_COUNT_KEY) && tag.contains(HOT_PORT_COUNT_KEY)
                && tag.contains(EMPTY_COLUMN_COUNT_KEY)
                && tag.contains(COOLANT_SPACE_BLOCK_COUNT_KEY)
                && tag.contains(COOLANT_CAPACITY_PER_EMPTY_BLOCK_MB_KEY)
                && tag.contains(COOLANT_CAPACITY_MB_KEY);
    }

    private static int countColumns(
            Map<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> columns,
            ReactorStructureDefinition.ColumnType type
    ) {
        return (int) columns.values().stream().filter(mapping -> mapping.type() == type).count();
    }

    /** 只按物理局部坐标去重；扫描缓存中的每个位置代表一个合法结构端口。 */
    private static int uniqueValidPortCount(
            ReactorStructureDefinition.ScanResult scan,
            ReactorStructureDefinition.PortType type
    ) {
        List<ReactorStructureDefinition.LocalPosition> ports =
                scan.ports().getOrDefault(type, List.of());
        return (int) ports.stream()
                .peek(position -> {
                    if (position == null || !ReactorStructureDefinition.sidePortSlots().contains(position)) {
                        throw new IllegalArgumentException("structure scan contains an invalid port position");
                    }
                })
                .distinct()
                .count();
    }
}
