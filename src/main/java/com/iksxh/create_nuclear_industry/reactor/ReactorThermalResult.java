package com.iksxh.create_nuclear_industry.reactor;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** 所有燃料列完成裂变热与局部冷却结算后的不可变结果，热量单位为 HU。 */
public record ReactorThermalResult(
        ReactorSnapshot snapshot,
        Map<CoreColumnPosition, FuelColumnThermalResult> columns
) {
    public ReactorThermalResult {
        if (snapshot == null || columns == null) {
            throw new IllegalArgumentException("thermal snapshot and column results are required");
        }
        columns = Collections.unmodifiableMap(new TreeMap<>(columns));
    }
}
