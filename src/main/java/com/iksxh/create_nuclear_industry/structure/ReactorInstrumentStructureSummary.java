package com.iksxh.create_nuclear_industry.structure;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;

/**
 * 仪表端口对外提供的静态结构摘要。
 *
 * <p>摘要只从最近一次有效结构扫描缓存和服务端配置派生，不持有或复制
 * {@code ReactorSnapshot}。有效摘要描述固定 P1 结构的尺寸、列和物理端口数量，并分别
 * 提供冷端、热端的配置容量及二者合计，容量单位均为 mB；无效摘要只提供确定性的不可用
 * 原因，禁止把一组零值解释成有效结构。</p>
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
        long coldInventoryCapacityMb,
        long hotInventoryCapacityMb,
        long totalFluidCapacityMb
) {
    private static final String VALID_KEY = "Valid";
    private static final String WIDTH_KEY = "Width";
    private static final String HEIGHT_KEY = "Height";
    private static final String DEPTH_KEY = "Depth";
    private static final String FUEL_COLUMN_COUNT_KEY = "FuelColumnCount";
    private static final String CONTROL_ROD_COLUMN_COUNT_KEY = "ControlRodColumnCount";
    private static final String COLD_PORT_COUNT_KEY = "ColdPortCount";
    private static final String HOT_PORT_COUNT_KEY = "HotPortCount";
    private static final String COLD_INVENTORY_CAPACITY_MB_KEY = "ColdInventoryCapacityMb";
    private static final String HOT_INVENTORY_CAPACITY_MB_KEY = "HotInventoryCapacityMb";
    private static final String TOTAL_FLUID_CAPACITY_MB_KEY = "TotalFluidCapacityMb";

    public ReactorInstrumentStructureSummary {
        if (unavailableReason == null) {
            throw new IllegalArgumentException("summary availability reason is required");
        }
        if (width < 0 || height < 0 || depth < 0
                || fuelColumnCount < 0 || controlRodColumnCount < 0
                || coldPortCount < 0 || hotPortCount < 0
                || coldInventoryCapacityMb < 0L || hotInventoryCapacityMb < 0L
                || totalFluidCapacityMb < 0L) {
            throw new IllegalArgumentException("structure summary values must be non-negative");
        }
        try {
            if (Math.addExact(coldInventoryCapacityMb, hotInventoryCapacityMb)
                    != totalFluidCapacityMb) {
                throw new IllegalArgumentException(
                        "total fluid capacity must equal cold and hot capacities");
            }
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("cold and hot capacities overflow total capacity", exception);
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
     * 从结构缓存和服务端配置生成摘要。
     *
     * @param scan 最近一次结构扫描结果；不会触发新的世界扫描
     * @param coldInventoryCapacityMb 共享冷端缓冲容量，单位为 mB
     * @param hotInventoryCapacityMb 共享热端缓冲容量，单位为 mB
     * @return 有效结构摘要，或带扫描失败原因的不可用摘要
     */
    public static ReactorInstrumentStructureSummary from(
            ReactorStructureDefinition.ScanResult scan,
            long coldInventoryCapacityMb,
            long hotInventoryCapacityMb
    ) {
        Objects.requireNonNull(scan, "structure scan is required");
        if (coldInventoryCapacityMb < 0L || hotInventoryCapacityMb < 0L) {
            throw new IllegalArgumentException("coolant capacities must be non-negative");
        }
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
        long totalCapacity = Math.addExact(coldInventoryCapacityMb, hotInventoryCapacityMb);

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
                coldInventoryCapacityMb,
                hotInventoryCapacityMb,
                totalCapacity
        );
    }

    /** 返回尚未成型或扫描失败时的明确不可用摘要。 */
    public static ReactorInstrumentStructureSummary unavailable(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("unavailable summary reason is required");
        }
        return new ReactorInstrumentStructureSummary(false, reason, 0, 0, 0,
                0, 0, 0, 0, 0L, 0L, 0L);
    }

    /**
     * 编码供客户端只读显示的摘要字段。
     *
     * <p>该数据只用于方块实体更新标签或网络更新包，不写入持久化结构；客户端不能借此
     * 反向改变服务端结构缓存。无效摘要不携带零值明细，避免被显示层误认为有效结构。</p>
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
        tag.putLong(COLD_INVENTORY_CAPACITY_MB_KEY, coldInventoryCapacityMb);
        tag.putLong(HOT_INVENTORY_CAPACITY_MB_KEY, hotInventoryCapacityMb);
        tag.putLong(TOTAL_FLUID_CAPACITY_MB_KEY, totalFluidCapacityMb);
        return tag;
    }

    /**
     * 解码客户端更新数据中的摘要；缺字段或非法值一律降级为不可用，不能伪造有效结构。
     *
     * @param tag 方块实体更新标签中的摘要子标签，可为空
     * @return 可供客户端只读显示的摘要
     */
    public static ReactorInstrumentStructureSummary readSyncTag(CompoundTag tag) {
        if (tag == null || !tag.getBoolean(VALID_KEY)) {
            return unavailable("structure summary is not synchronized");
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
                    tag.getLong(COLD_INVENTORY_CAPACITY_MB_KEY),
                    tag.getLong(HOT_INVENTORY_CAPACITY_MB_KEY),
                    tag.getLong(TOTAL_FLUID_CAPACITY_MB_KEY)
            );
        } catch (IllegalArgumentException exception) {
            return unavailable("structure summary synchronization data is invalid");
        }
    }

    private static int countColumns(
            Map<com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition,
                    ReactorStructureDefinition.ColumnMapping> columns,
            ReactorStructureDefinition.ColumnType type
    ) {
        return (int) columns.values().stream()
                .filter(mapping -> mapping.type() == type)
                .count();
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
