package com.iksxh.create_nuclear_industry.p0probe.numeric;

import java.util.Map;

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
