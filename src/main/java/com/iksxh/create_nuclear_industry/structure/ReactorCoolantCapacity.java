package com.iksxh.create_nuclear_industry.structure;

import java.util.Objects;

/**
 * 固定 P1 反应堆冷却剂容量的纯派生模型。
 *
 * <p>容量只读取最近一次服务端有效结构扫描中的 {@code bodyPositions}，不持有世界对象，
 * 也不触发扫描。单位为 mB；每个空置或控制棒主体格的配置容量与最终共享容量均必须能由
 * NeoForge 的整数流体 capability 无损表示。</p>
 */
public final class ReactorCoolantCapacity {
    /** 固定 3×3 堆芯在至少一根燃料列存在时最多可计的空气格数量。 */
    public static final int MAX_COOLANT_SPACE_BLOCK_COUNT = 24;
    /** 共享容量必须适配 {@code IFluidHandler} 的 int 容量返回值。 */
    public static final int MAX_CAPACITY_PER_EMPTY_BLOCK_MB =
            Integer.MAX_VALUE / MAX_COOLANT_SPACE_BLOCK_COUNT;
    public static final int DEFAULT_CAPACITY_PER_EMPTY_BLOCK_MB = 1_000;

    private ReactorCoolantCapacity() {
    }

    /**
     * 从有效结构缓存派生静态容量；调用方不得传入未经服务端扫描验证的结构。
     *
     * @param scan 最近一次有效结构扫描缓存
     * @param capacityPerEmptyBlockMb 每个可计主体格的容量，单位为 mB
     * @return 空列统计、空气格统计和共享容量
     */
    public static Derived fromScan(
            ReactorStructureDefinition.ScanResult scan,
            long capacityPerEmptyBlockMb
    ) {
        Objects.requireNonNull(scan, "structure scan is required");
        if (!scan.valid()) {
            throw new IllegalArgumentException("coolant capacity requires a valid structure scan");
        }
        validateCapacityPerEmptyBlockMb(capacityPerEmptyBlockMb);

        int emptyColumnCount = 0;
        int coolantSpaceBlockCount = 0;
        for (ReactorStructureDefinition.ColumnMapping mapping : scan.columns().values()) {
            if (mapping.type() == ReactorStructureDefinition.ColumnType.EMPTY) {
                emptyColumnCount++;
            }
            if (mapping.type() == ReactorStructureDefinition.ColumnType.EMPTY
                    || mapping.type() == ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                try {
                    coolantSpaceBlockCount = Math.addExact(
                            coolantSpaceBlockCount, mapping.bodyPositions().size());
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("coolant space block count overflow", exception);
                }
            }
        }
        long coolantCapacityMb = capacityFor(coolantSpaceBlockCount, capacityPerEmptyBlockMb);
        return new Derived(emptyColumnCount, coolantSpaceBlockCount,
                capacityPerEmptyBlockMb, coolantCapacityMb);
    }

    /** 返回指定可计空气格数量对应的共享容量，单位为 mB。 */
    public static long capacityFor(int coolantSpaceBlockCount, long capacityPerEmptyBlockMb) {
        if (coolantSpaceBlockCount < 0
                || coolantSpaceBlockCount > MAX_COOLANT_SPACE_BLOCK_COUNT) {
            throw new IllegalArgumentException("coolant space block count is outside fixed P1 bounds");
        }
        validateCapacityPerEmptyBlockMb(capacityPerEmptyBlockMb);
        try {
            return Math.multiplyExact(coolantSpaceBlockCount, capacityPerEmptyBlockMb);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("derived coolant capacity overflow", exception);
        }
    }

    /** 校验服务端每格容量，允许零容量但拒绝负数、溢出和超出 capability 安全上限的值。 */
    public static void validateCapacityPerEmptyBlockMb(long capacityPerEmptyBlockMb) {
        if (capacityPerEmptyBlockMb < 0L
                || capacityPerEmptyBlockMb > MAX_CAPACITY_PER_EMPTY_BLOCK_MB) {
            throw new IllegalArgumentException(
                    "capacity per empty block must fit the fixed P1 int capability limit");
        }
    }

    /** 结构派生的静态容量明细；不包含冷/热动态库存。 */
    public record Derived(
            int emptyColumnCount,
            int coolantSpaceBlockCount,
            long coolantCapacityPerEmptyBlockMb,
            long coolantCapacityMb
    ) {
        public Derived {
            if (emptyColumnCount < 0 || coolantSpaceBlockCount < 0
                    || emptyColumnCount > coolantSpaceBlockCount
                    || coolantSpaceBlockCount > MAX_COOLANT_SPACE_BLOCK_COUNT) {
                throw new IllegalArgumentException("derived coolant space counts are invalid");
            }
            validateCapacityPerEmptyBlockMb(coolantCapacityPerEmptyBlockMb);
            if (coolantCapacityMb != capacityFor(
                    coolantSpaceBlockCount, coolantCapacityPerEmptyBlockMb)) {
                throw new IllegalArgumentException("derived coolant capacity is inconsistent");
            }
        }
    }
}
