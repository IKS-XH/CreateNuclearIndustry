package com.iksxh.create_nuclear_industry.p0probe.numeric;

/** P0 单 tick 单列的热量、冷却、传播、损伤和下一状态证据。热量单位为 HU。 */
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
