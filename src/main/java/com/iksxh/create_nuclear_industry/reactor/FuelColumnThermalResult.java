package com.iksxh.create_nuclear_industry.reactor;

/** 一次模拟步中单根燃料列的热负荷、移热量、量化余数和完整度变化结果，热量单位为 HU。 */
public record FuelColumnThermalResult(
        double generatedHeatHu,
        double removedHeatHu,
        double netHeatLoadHu,
        double quantizedHeatRemainderHu,
        double integrityDamage,
        FuelColumnState nextState
) {
    /** 兼容尚未公开量化余数结果的旧调用方；旧调用默认没有安全余数。 */
    public FuelColumnThermalResult(
            double generatedHeatHu,
            double removedHeatHu,
            double netHeatLoadHu,
            double integrityDamage,
            FuelColumnState nextState
    ) {
        this(generatedHeatHu, removedHeatHu, netHeatLoadHu, 0.0D, integrityDamage, nextState);
    }

    public FuelColumnThermalResult {
        requireFiniteNonNegative("generated heat", generatedHeatHu);
        requireFiniteNonNegative("removed heat", removedHeatHu);
        requireFiniteNonNegative("net heat load", netHeatLoadHu);
        requireFiniteNonNegative("quantized heat remainder", quantizedHeatRemainderHu);
        if (quantizedHeatRemainderHu > netHeatLoadHu + 1.0E-12D) {
            throw new IllegalArgumentException("quantized heat remainder cannot exceed net heat load");
        }
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
