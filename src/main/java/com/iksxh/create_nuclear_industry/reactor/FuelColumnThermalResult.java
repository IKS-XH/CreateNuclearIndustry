package com.iksxh.create_nuclear_industry.reactor;

/** 一次模拟步中单根燃料列的热负荷、移热量和完整度变化结果，热量单位为 HU。 */
public record FuelColumnThermalResult(
        double generatedHeatHu,
        double removedHeatHu,
        double netHeatLoadHu,
        double integrityDamage,
        FuelColumnState nextState
) {
    public FuelColumnThermalResult {
        requireFiniteNonNegative("generated heat", generatedHeatHu);
        requireFiniteNonNegative("removed heat", removedHeatHu);
        requireFiniteNonNegative("net heat load", netHeatLoadHu);
        requireFiniteNonNegative("integrity damage", integrityDamage);
        if (nextState == null) {
            throw new IllegalArgumentException("next fuel column state is required");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
