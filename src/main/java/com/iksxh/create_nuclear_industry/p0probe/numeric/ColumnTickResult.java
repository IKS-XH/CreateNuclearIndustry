package com.iksxh.create_nuclear_industry.p0probe.numeric;

public record ColumnTickResult(
        ColumnKey key,
        double generatedHeat,
        double plannedBurn,
        double coolantRemovedHeat,
        double netHeatLoad,
        double propagationHeatReceived,
        double propagationHeatRemoved,
        double integrityDamage,
        ColumnState nextState
) {
}
