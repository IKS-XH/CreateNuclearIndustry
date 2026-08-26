package com.iksxh.create_nuclear_industry.p0probe.numeric;

/** P0 原型单个水平列的不可变上一 tick 状态，不是正式 P1 快照字段。 */
public record ColumnState(
        ColumnKey key,
        ColumnKind kind,
        double fuelRemaining,
        double fuelColumnIntegrity,
        double controlRodColumnIntegrity,
        double controlRodDepth,
        double jammedDepth,
        double cachedHeat,
        boolean jammed
) {
    public ColumnState {
        if (key == null || kind == null) {
            throw new IllegalArgumentException("column key and kind are required");
        }
        requireFiniteNonNegative("fuelRemaining", fuelRemaining);
        requireUnit("fuelColumnIntegrity", fuelColumnIntegrity);
        requireUnit("controlRodColumnIntegrity", controlRodColumnIntegrity);
        requireUnit("controlRodDepth", controlRodDepth);
        requireUnit("jammedDepth", jammedDepth);
        requireFiniteNonNegative("cachedHeat", cachedHeat);
        if (kind != ColumnKind.FUEL && fuelRemaining != 0) {
            throw new IllegalArgumentException("only fuel columns may contain fuel");
        }
        if (kind != ColumnKind.CONTROL_ROD && (controlRodColumnIntegrity != 0 || controlRodDepth != 0
                || jammedDepth != 0 || jammed)) {
            throw new IllegalArgumentException("only control columns may contain control state");
        }
    }

    /** 创建带燃料剩余量、完整度和缓存热量的燃料列。热量单位为 HU。 */
    public static ColumnState fuel(ColumnKey key, double remaining, double integrity, double cachedHeat) {
        return new ColumnState(key, ColumnKind.FUEL, remaining, integrity, 0, 0, 0, cachedHeat, false);
    }

    /** 创建未卡死的 P0 控制棒列，深度单位为 [0,1]。 */
    public static ColumnState controlRod(ColumnKey key, double integrity, double depth) {
        return new ColumnState(key, ColumnKind.CONTROL_ROD, 0, 0, integrity, depth, depth, 0, false);
    }

    /** 创建携带缓存热量的 P0 控制棒列。热量单位为 HU。 */
    public static ColumnState controlRodWithHeat(ColumnKey key, double integrity, double depth, double cachedHeat) {
        return new ColumnState(key, ColumnKind.CONTROL_ROD, 0, 0, integrity, depth, depth, cachedHeat, false);
    }

    /** 创建空列。 */
    public static ColumnState empty(ColumnKey key) {
        return new ColumnState(key, ColumnKind.EMPTY, 0, 0, 0, 0, 0, 0, false);
    }

    /** 判断燃料剩余量和燃料列完整度是否都使其仍可参与反应。 */
    public boolean effectiveFuel() {
        return kind == ColumnKind.FUEL && fuelRemaining > 0 && fuelColumnIntegrity > 0;
    }

    /** 返回考虑卡死阈值后的有效控制深度，范围为 [0,1]。 */
    public double effectiveControlDepth(ReactorParameters parameters) {
        if (kind != ColumnKind.CONTROL_ROD) {
            return 0;
        }
        return jammed || controlRodColumnIntegrity <= parameters.controlRodColumnFailureThreshold()
                ? jammedDepth : controlRodDepth;
    }

    /** 用新燃料状态创建同一列的替换值。 */
    public ColumnState withFuel(double remaining, double integrity, double nextCachedHeat) {
        return fuel(key, remaining, integrity, nextCachedHeat);
    }

    /** 用新控制棒状态创建同一列的替换值。 */
    public ColumnState withControl(double integrity, double depth, double nextCachedHeat, boolean nextJammed,
                                   double nextJammedDepth) {
        return new ColumnState(key, ColumnKind.CONTROL_ROD, 0, 0, integrity, depth, nextJammedDepth,
                nextCachedHeat, nextJammed);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireUnit(String name, double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be in [0, 1]");
        }
    }
}
