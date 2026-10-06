package com.iksxh.create_nuclear_industry.reactor;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * 服务端一次模拟步所有燃料列裂变计算的不可变聚合结果，热量单位为 HU。
 *
 * <p>{@code rawHeatHu} 是限幅与取整前的诊断热量；计算器返回的 {@code generatedHeatHu}
 * 是先限幅、再全堆向上取整一次的权威新生热总量，供冷却与遥测共用。列热按原比例分配，
 * 最后一个正产热列承接浮点尾差；缓存热不属于本结果，燃耗仍按原始公式计算。</p>
 */
public record ReactorFissionResult(
        Map<CoreColumnPosition, FuelColumnFissionResult> columns,
        double rawHeatHu,
        double generatedHeatHu,
        double plannedFuelBurnUnits
) {
    public ReactorFissionResult {
        if (columns == null) {
            throw new IllegalArgumentException("fission result columns are required");
        }
        columns = Collections.unmodifiableMap(new TreeMap<>(columns));
        requireFiniteNonNegative("raw heat", rawHeatHu);
        requireFiniteNonNegative("generated heat", generatedHeatHu);
        requireFiniteNonNegative("planned fuel burn", plannedFuelBurnUnits);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
