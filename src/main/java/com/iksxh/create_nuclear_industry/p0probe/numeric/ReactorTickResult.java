package com.iksxh.create_nuclear_industry.p0probe.numeric;

import java.util.Map;

/** P0 单 tick 的总热量、燃耗、冷却、传播覆盖率和列级证据。热量单位为 HU。 */
public record ReactorTickResult(
        ReactorSnapshot next,
        Map<ColumnKey, ColumnTickResult> columns,
        double rawGeneratedHeat,
        double generatedHeat,
        double plannedBurn,
        double convertedCoolant,
        double removedHeat,
        double propagationCoverageFraction,
        boolean meltdownDanger,
        int duplicatePortCount
) {
}
